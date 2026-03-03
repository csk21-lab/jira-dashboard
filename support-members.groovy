import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.user.ApplicationUser

import com.onresolve.scriptrunner.runner.customisers.WithPlugin
import com.onresolve.scriptrunner.runner.customisers.PluginModule

import com.riadalabs.jira.plugins.insight.channel.external.api.facade.ObjectFacade
import com.riadalabs.jira.plugins.insight.channel.external.api.facade.ObjectTypeAttributeFacade
import com.riadalabs.jira.plugins.insight.services.model.ObjectBean
import com.riadalabs.jira.plugins.insight.services.model.ObjectTypeAttributeBean
import com.riadalabs.jira.plugins.insight.services.model.ObjectAttributeBean

@WithPlugin("com.riadalabs.jira.plugins.insight")
class Dummy {} // required for @WithPlugin in some contexts

// ====== Managers ======
def issueManager       = ComponentAccessor.issueManager
def customFieldManager = ComponentAccessor.customFieldManager
def userManager        = ComponentAccessor.userManager
def authContext        = ComponentAccessor.jiraAuthenticationContext

// In Script Console you might hard‑code; in listener/post‑function use the injected "issue"
def issue = issueManager.getIssueObject("ABC-123")

// ====== CFAAN handling ======
def cfaan        = customFieldManager.getCustomFieldObjectByName("CFAAN")
def cfaanValue   = issue.getCustomFieldValue(cfaan)      // could be Assets object, label, etc.
def aan          = cfaanValue?.label ?: cfaanValue?.toString()
log.warn "CFAAN / aan: ${aan}"

// ====== Custom field IDs ======
Long sourceFieldId       = 27605L   // Source Assets field (user selection)
Long serviceComponentId  = 31201L   // Service Component Assets field
// Targets are Assets fields (by *name* below), so IDs not required for HAPI
String supportMembersName = "Support Members"   // Multi user picker field name

// Attribute names on the asset object
String attrNameO      = "O"
String attrNameOT     = "OT"
String attrNameOF     = "OF"
String attrNameSTeam  = "STeam"  // attribute that points to STeam object
String attrNameTeam   = "Team"   // user attribute on STeam object

// ====== Load Insight facades ======
@PluginModule ObjectFacade objectFacade
@PluginModule ObjectTypeAttributeFacade objectTypeAttributeFacade

// ====== Helpers ======

// Get first object from an Assets custom field
def getFirstAssetObjectFromCF = { def iss, Long cfId ->
    def cf   = customFieldManager.getCustomFieldObject(cfId)
    def list = iss.getCustomFieldValue(cf) as List<ObjectBean>
    list ? list.first() : null
}

// Get referenced object key from an attribute on an ObjectBean (object reference attribute).[web:13][web:21]
def getReferencedObjectKey = { ObjectBean obj, String attrName ->
    if (!obj) return null
    ObjectTypeAttributeBean attrBean =
            objectTypeAttributeFacade.loadObjectTypeAttributeBean(obj.objectTypeId, attrName)
    if (!attrBean) return null
    ObjectAttributeBean objAttrBean =
            objectFacade.loadObjectAttributeBean(obj.id, attrBean.id)
    def refObjId = objAttrBean?.objectAttributeValueBeans?.first()?.referencedObjectBeanId
    if (!refObjId) return null
    ObjectBean refObj = objectFacade.loadObjectBean(refObjId)
    refObj?.objectKey
}

// Get Jira users from a user attribute on a given asset (e.g. STeam.Team).[web:12][web:23]
def getUserAttributeAsApplicationUsers = { String objKey, String attrName ->
    if (!objKey) return [] as List<ApplicationUser>

    ObjectBean objBean = objectFacade.loadObjectBean(objKey)
    if (!objBean) return [] as List<ApplicationUser>

    ObjectTypeAttributeBean attrBean =
            objectTypeAttributeFacade.loadObjectTypeAttributeBean(objBean.objectTypeId, attrName)
    if (!attrBean) return [] as List<ApplicationUser>

    ObjectAttributeBean objAttrBean =
            objectFacade.loadObjectAttributeBean(objBean.id, attrBean.id)
    def valueBeans = objAttrBean?.objectAttributeValueBeans ?: []

    // For user attributes, .user returns ApplicationUser (DC).[web:12][web:23]
    def users = valueBeans.collect { it?.user }.findAll { it != null }
    users as List<ApplicationUser>
}

// ====== Get driving objects ======
def selectedObject        = getFirstAssetObjectFromCF(issue, sourceFieldId)
def serviceComponentObject = getFirstAssetObjectFromCF(issue, serviceComponentId)

if (!selectedObject && !serviceComponentObject) {
    log.warn "No object selected in source or service component for issue ${issue.key}"
    return
}

// Which asset drives the mapping?
ObjectBean drivingAsset = (aan == "XYZ") ? serviceComponentObject : selectedObject
log.warn "Driving asset key: ${drivingAsset?.objectKey}"

// ====== Resolve referenced assets from driving asset ======
def refOKey     = getReferencedObjectKey(drivingAsset, attrNameO)     as String
def refOTKey    = getReferencedObjectKey(drivingAsset, attrNameOT)    as String
def refOFKey    = getReferencedObjectKey(drivingAsset, attrNameOF)    as String
def sTeamKey    = getReferencedObjectKey(drivingAsset, attrNameSTeam) as String

log.warn "O: ${refOKey}, OT: ${refOTKey}, OF: ${refOFKey}, STeam: ${sTeamKey}"

// ====== From STeam object, get Team (user attribute) and map to Support Members ======
def sTeamUsers = getUserAttributeAsApplicationUsers(sTeamKey, attrNameTeam)
log.warn "sTeamUsers: ${sTeamUsers*.username}"

// ====== Update issue fields (Assets + multi‑user picker) ======

// Using HAPI-style update; assumes field names match exactly.[web:5][web:27]
issue.update {
    // Assets fields by name, set by object key
    if (refOKey) {
        setCustomFieldValue("O") { set(refOKey) }
    }
    if (refOTKey) {
        setCustomFieldValue("OT") { set(refOTKey) }
    }
    if (refOFKey) {
        setCustomFieldValue("OF") { set(refOFKey) }
    }
    if (sTeamKey) {
        setCustomFieldValue("STeam") { set(sTeamKey) }
    }

    // Multi user picker: Support Members
    if (sTeamUsers) {
        setCustomFieldValue(supportMembersName) {
            set(sTeamUsers)   // List<ApplicationUser>
        }
    }
}

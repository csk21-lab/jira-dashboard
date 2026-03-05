import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.issue.Issue
import com.atlassian.jira.user.ApplicationUser

def log = org.apache.log4j.Logger.getLogger("assets.simple")

// =====================
// CONFIG
// =====================
def issue = ComponentAccessor.issueManager.getIssueObject("ABC-123")   // Script Console test
// In post-function/listener: comment above and use provided `issue`

Long sourceFieldId      = 27605L
Long serviceComponentId = 31201L

// Target Assets fields (NAMES as used in issue.update below)
String targetOName     = "O"
String targetOTName    = "OT"
String targetOFName    = "OF"
String targetSTeamName = "STeam"

// Multi User Picker field (NAME)
String multiUserFieldName = "Support Members"   // <<< change to your multi-user field name

// Object attributes on source object
String attrO     = "O"
String attrOT    = "OT"
String attrOF    = "OF"
String attrSTeam = "STeam"

// On Support Team object (User attribute)
String teamMembersAttr = "Team Members"

// CFAAN
String cfaFieldName = "CFAAN"
String specialAanValue = "XYZ"

// =====================
// Load Assets facades
// =====================
def pluginAccessor = ComponentAccessor.pluginAccessor
def objectFacadeClass = pluginAccessor.classLoader.findClass("com.riadalabs.jira.plugins.insight.channel.external.api.facade.ObjectFacade")
def objectTypeAttributeFacadeClass = pluginAccessor.classLoader.findClass("com.riadalabs.jira.plugins.insight.channel.external.api.facade.ObjectTypeAttributeFacade")
def objectFacade = ComponentAccessor.getOSGiComponentInstanceOfType(objectFacadeClass)
def objectTypeAttributeFacade = ComponentAccessor.getOSGiComponentInstanceOfType(objectTypeAttributeFacadeClass)

def cfMgr = ComponentAccessor.customFieldManager

// =====================
// Small helpers
// =====================

// Get first Assets object from CF id (single or multi)
def firstAssets = { Issue iss, Long cfId ->
  def cf = cfMgr.getCustomFieldObject(cfId)
  def v = cf ? iss.getCustomFieldValue(cf) : null
  (v instanceof Collection) ? (v ? v.first() : null) : v
}

// Get referenced object (Object reference attribute)
def refObj = { obj, String attrName ->
  if (!obj) return null
  def attrBean = objectTypeAttributeFacade.loadObjectTypeAttributeBean(obj.objectTypeId, attrName)
  if (!attrBean) return null
  def objAttrBean = objectFacade.loadObjectAttributeBean(obj.id, attrBean.id)
  def refId = objAttrBean?.objectAttributeValueBeans?.find { it?.referencedObjectBeanId }?.referencedObjectBeanId
  refId ? objectFacade.loadObjectBean(refId) : null
}

// Get User attribute values (Team Members)
def userAttr = { obj, String attrName ->
  if (!obj) return [] as List<ApplicationUser>
  def attrBean = objectTypeAttributeFacade.loadObjectTypeAttributeBean(obj.objectTypeId, attrName)
  if (!attrBean) return [] as List<ApplicationUser>
  def objAttrBean = objectFacade.loadObjectAttributeBean(obj.id, attrBean.id)
  def vals = (objAttrBean?.objectAttributeValueBeans ?: []).collect { it?.value }.findAll { it instanceof ApplicationUser }
  (vals as List<ApplicationUser>).unique { it.key }
}

// =====================
// CFAAN logic
// =====================
def cfaCf = cfMgr.getCustomFieldObjectsByName(cfaFieldName)?.first()
def cfaVal = cfaCf ? issue.getCustomFieldValue(cfaCf) : null
def aan = (cfaVal instanceof Collection) ? cfaVal?.first()?.label : cfaVal?.label
log.warn("aan=${aan}")

def selectedObject = firstAssets(issue, sourceFieldId)
def serviceComponentObject = firstAssets(issue, serviceComponentId)

def baseObj = (aan == specialAanValue) ? serviceComponentObject : selectedObject
if (!baseObj) {
  log.warn("No base Assets object found for issue ${issue.key}")
  return
}

// =====================
// Pull referenced objects
// =====================
def oObj     = refObj(baseObj, attrO)
def otObj    = refObj(baseObj, attrOT)
def ofObj    = refObj(baseObj, attrOF)
def sTeamObj = refObj(baseObj, attrSTeam)

def members = userAttr(sTeamObj, teamMembersAttr)

log.warn("O=${oObj?.objectKey}, OT=${otObj?.objectKey}, OF=${ofObj?.objectKey}, STeam=${sTeamObj?.objectKey}")
log.warn("Team Members=${members*.name}")

// =====================
// Update issue (simple)
// =====================
issue.update {
  setCustomFieldValue(targetOName)     { set(oObj) }
  setCustomFieldValue(targetOTName)    { set(otObj) }
  setCustomFieldValue(targetOFName)    { set(ofObj) }
  setCustomFieldValue(targetSTeamName) { set(sTeamObj) }

  // Multi User Picker field
  setCustomFieldValue(multiUserFieldName) { set(members) }   // replace
}

import com.atlassian.jira.component.ComponentAccessor
import com.riadalabs.jira.plugins.insight.services.model.ObjectBean
import com.riadalabs.jira.plugins.insight.services.model.IQL
import com.riadalabs.jira.plugins.insight.channel.external.api.facade.IQLFacade
import com.riadalabs.jira.plugins.insight.channel.external.api.facade.ObjectFacade

// Inputs
def issueKey = "ABC-123"
def assetCfId = "customfield_12345"  // Asset custom field

def issueManager = ComponentAccessor.issueManager
def customFieldManager = ComponentAccessor.customFieldManager
def iqlFacade = ComponentAccessor.getOSGiComponentInstanceOfType(IQLFacade)
def objectFacade = ComponentAccessor.getOSGiComponentInstanceOfType(ObjectFacade)

def issue = issueManager.getIssueObject(issueKey)
def locationCF = customFieldManager.getCustomFieldObject(assetCfId)
def locationObject = issue.getCustomFieldValue(locationCF) as ObjectBean

if (!locationObject) {
    log.warn("No value set for asset field on issue ${issueKey}")
    return
}

// Build AQL to count objects with same attribute
def aql = "objectType = \"Servers\" AND Location = ${locationObject.id}"
def results = iqlFacade.findObjects(IQL.parse(aql))

log.warn("Found ${results?.size()} objects in 'Servers' with Location = ${locationObject.label}")

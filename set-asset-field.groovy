import com.atlassian.jira.component.ComponentAccessor
import com.riadalabs.jira.plugins.insight.channel.external.api.facade.IQLFacade
import com.atlassian.jira.issue.MutableIssue
import com.atlassian.jira.issue.fields.CustomField
import com.atlassian.jira.issue.util.DefaultIssueChangeHolder
import com.atlassian.jira.issue.ModifiedValue
import com.atlassian.jira.event.type.EventDispatchOption
import org.apache.log4j.Logger

def log = Logger.getLogger("com.acme.SetAssetFieldSpecificIssue")

// Configure these for your environment
String targetIssueKey = "ABC-212"
String assetsCustomFieldName = "ON"
String assetObjectKey = "ABC-912536"
String objectTypeName = "YourObjectTypeName" // <-- Replace with your actual type name

def issueManager = ComponentAccessor.getIssueManager()
def customFieldManager = ComponentAccessor.getCustomFieldManager()
def iqlFacade = ComponentAccessor.getOSGiComponentInstanceOfType(IQLFacade)
def authContext = ComponentAccessor.getJiraAuthenticationContext()
def currentUser = authContext.getLoggedInUser()

if (!currentUser) {
    log.warn("No logged in user found. Script must run with a user context.")
}

def issue = issueManager.getIssueByCurrentKey(targetIssueKey) as MutableIssue
if (!issue) {
    log.warn("Issue ${targetIssueKey} not found")
    return
}

def assetsCustomField = customFieldManager.getCustomFieldObjectByName(assetsCustomFieldName)
if (!assetsCustomField) {
    log.warn("Custom field '${assetsCustomFieldName}' not found")
    return
}

// Build IQL and find the asset object bean
def iql = 'objectType = "' + objectTypeName + '" AND Key = "' + assetObjectKey + '"'
def objectBeans = iqlFacade.findObjectsByIQL(iql)
def assetObjectBean = (objectBeans && objectBeans.size() > 0) ? objectBeans[0] : null

if (!assetObjectBean) {
    log.warn("Asset object not found with IQL: ${iql}")
    return
}

// Prepare new value depending on whether the CF is multi-valued
def oldValue = issue.getCustomFieldValue(assetsCustomField)
def newValue = assetsCustomField.isMultiple() || assetsCustomField.getCustomFieldType().isList() ? [assetObjectBean] : assetObjectBean

try {
    // Update the custom field value on the issue (in-memory)
    def changeHolder = new DefaultIssueChangeHolder()
    assetsCustomField.updateValue(null, issue, new ModifiedValue(oldValue, newValue), changeHolder)

    // Persist the issue change and fire events so Jira updates indices and listeners
    issueManager.updateIssue(currentUser, issue, EventDispatchOption.ISSUE_UPDATED, false)

    log.warn("Set asset '${assetObjectBean.name}' on issue '${issue.key}' (oldValue=${oldValue})")
} catch (Exception e) {
    log.warn("FAILED to set asset field: ${e.message}", e)
}

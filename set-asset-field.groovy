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

// Prepare new value (single-value holder)
def oldValue = issue.getCustomFieldValue(assetsCustomField)
def newValue = assetObjectBean

// Debug logging: types and contents so we can see what's actually being used
try {
    log.warn("DEBUG: assetObjectBean class=${assetObjectBean?.getClass()?.name} id=${assetObjectBean?.id} name=${assetObjectBean?.name} objectKey=${assetObjectBean?.objectKey}")
} catch (Exception ignored) {}
try {
    log.warn("DEBUG: oldValue class=${oldValue?.getClass()?.name} value=${oldValue}")
} catch (Exception ignored) {}

boolean updated = false

// Try the usual updateValue approach first
try {
    def changeHolder = new DefaultIssueChangeHolder()
    assetsCustomField.updateValue(null, issue, new ModifiedValue(oldValue, newValue), changeHolder)
    issueManager.updateIssue(currentUser, issue, EventDispatchOption.ISSUE_UPDATED, false)
    log.warn("SUCCESS: set asset (via updateValue) '${assetObjectBean?.name}' on issue '${issue.key}' (oldValue=${oldValue})")
    updated = true
} catch (Exception e) {
    log.warn("updateValue failed: ${e.class.name}: ${e.message}", e)
}

// If that failed, try setting the field directly on the issue as a fallback
if (!updated) {
    try {
        issue.setCustomFieldValue(assetsCustomField, newValue)
        issueManager.updateIssue(currentUser, issue, EventDispatchOption.ISSUE_UPDATED, false)
        log.warn("SUCCESS: set asset (via setCustomFieldValue fallback) '${assetObjectBean?.name}' on issue '${issue.key}' (oldValue=${oldValue})")
        updated = true
    } catch (Exception e2) {
        log.warn("Fallback setCustomFieldValue also failed: ${e2.class.name}: ${e2.message}", e2)
    }
}

if (!updated) {
    log.warn("FAILED to set asset field after both primary and fallback attempts. See previous logs for exceptions.")
}

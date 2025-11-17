import com.atlassian.jira.component.ComponentAccessor
import org.apache.log4j.Logger
import com.atlassian.jira.issue.MutableIssue
import com.atlassian.jira.issue.util.DefaultIssueChangeHolder
import com.atlassian.jira.issue.ModifiedValue
import com.atlassian.jira.event.type.EventDispatchOption

def log = Logger.getLogger("com.acme.SetAssetFieldSpecificIssue")

// Configure these for your environment
String targetIssueKey = "ABC-212"           // used when 'issue' is not in the binding
String assetsCustomFieldName = "ON"         // the Assets/Insight custom field name
String assetObjectKey = "ABC-912536"       // the asset object Key you want to set (single-value)

def issueManager = ComponentAccessor.getIssueManager()
def customFieldManager = ComponentAccessor.getCustomFieldManager()
def authContext = ComponentAccessor.getJiraAuthenticationContext()
def currentUser = authContext.getLoggedInUser()

// Resolve the issue: use binding 'issue' if available, otherwise load by key (no autowiring logs)
MutableIssue theIssue
if (this.binding?.hasVariable('issue')) {
    theIssue = binding.getVariable('issue') as MutableIssue
} else {
    theIssue = issueManager.getIssueByCurrentKey(targetIssueKey) as MutableIssue
    if (!theIssue) {
        log.warn("Issue ${targetIssueKey} not found")
        return
    }
}

try {
    // Try ScriptRunner DSL first (sets by asset object Key)
    theIssue.update {
        setCustomFieldValue(assetsCustomFieldName) {
            set(assetObjectKey)   // single-value: the object Key string
        }
    }
    log.info("Set asset ${assetObjectKey} on ${theIssue.key} via issue.update DSL")
    return
} catch (MissingMethodException mme) {
    // DSL not available here — fall through to explicit API
} catch (Exception e) {
    log.warn("Failed using issue.update DSL: ${e.class.name}: ${e.message}", e)
    // Attempt explicit API fallback below
}

// Explicit API fallback: set by object Key (single-value)
def cf = customFieldManager.getCustomFieldObjectByName(assetsCustomFieldName)
if (!cf) {
    log.warn("Custom field '${assetsCustomFieldName}' not found")
    return
}

def oldValue = theIssue.getCustomFieldValue(cf)
def newValue = assetObjectKey // set by Key string (single value)

try {
    cf.updateValue(null, theIssue, new ModifiedValue(oldValue, newValue), new DefaultIssueChangeHolder())
    issueManager.updateIssue(currentUser, theIssue, EventDispatchOption.ISSUE_UPDATED, false)
    log.info("Set asset ${assetObjectKey} on ${theIssue.key} via explicit API")
} catch (Exception e) {
    // Final fallback: set directly on issue and persist
    try {
        theIssue.setCustomFieldValue(cf, newValue)
        issueManager.updateIssue(currentUser, theIssue, EventDispatchOption.ISSUE_UPDATED, false)
        log.info("Set asset ${assetObjectKey} on ${theIssue.key} via direct setCustomFieldValue fallback")
    } catch (Exception e2) {
        log.warn("Failed to set asset field by any method: ${e2.class.name}: ${e2.message}", e2)
    }
}

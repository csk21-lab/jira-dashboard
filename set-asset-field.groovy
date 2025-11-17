import com.atlassian.jira.component.ComponentAccessor
import org.apache.log4j.Logger
import com.atlassian.jira.issue.MutableIssue

def log = Logger.getLogger("com.acme.SetAssetFieldSpecificIssue")

// Configure these for your environment
String targetIssueKey = "ABC-212"           // used when 'issue' is not in the binding
String assetsCustomFieldName = "ON"         // the Assets/Insight custom field name
String assetObjectKey = "ABC-912536"       // the asset object Key you want to set (single-value)

def issueManager = ComponentAccessor.getIssueManager()

// Resolve the issue: use binding 'issue' if available (ScriptRunner), otherwise load by key
MutableIssue theIssue
if (this.binding?.hasVariable('issue')) {
    theIssue = binding.getVariable('issue') as MutableIssue
    log.warn("Using issue from binding: ${theIssue?.key}")
} else {
    theIssue = issueManager.getIssueByCurrentKey(targetIssueKey) as MutableIssue
    if (!theIssue) {
        log.warn("Issue ${targetIssueKey} not found")
        return
    }
    log.warn("Loaded issue: ${theIssue.key}")
}

try {
    // ScriptRunner DSL: set by asset object Key (first-time set, no old value needed)
    theIssue.update {
        setCustomFieldValue(assetsCustomFieldName) {
            set(assetObjectKey)   // single-value: the object Key string
        }
    }
    log.warn("SUCCESS: set asset ${assetObjectKey} on ${theIssue.key} via issue.update DSL")
} catch (MissingMethodException mme) {
    // DSL not available in this context — log and instruct fallback
    log.warn("ScriptRunner issue.update DSL not available here (MissingMethodException). Use explicit API fallback.", mme)
} catch (Exception e) {
    log.warn("Failed to set asset using issue.update DSL: ${e.class.name}: ${e.message}", e)
}

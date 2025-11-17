import com.atlassian.jira.component.ComponentAccessor
import org.apache.log4j.Logger
import org.apache.log4j.Level
import com.atlassian.jira.issue.MutableIssue
import com.atlassian.jira.issue.util.DefaultIssueChangeHolder
import com.atlassian.jira.issue.ModifiedValue
import com.atlassian.jira.event.type.EventDispatchOption
// MissingPropertyException is in Groovy runtime; no explicit import required

def log = Logger.getLogger("com.acme.SetAssetFieldSpecificIssue")

// Disable noisy debug logs from migrationsystem and migrationutils
// Set to WARN to suppress DEBUG-level output
Logger.getLogger("migrationsystem").setLevel(Level.WARN)
Logger.getLogger("migrationutils").setLevel(Level.WARN)

// Configure these for your environment
// If you want the script to run "for current issue" in ScriptRunner contexts, leave targetIssueKey as null.
// Otherwise set a fallback issue key (e.g. "ABC-212") to operate when no 'issue' is available in the binding.
String targetIssueKey = null            // used only when 'issue' is not in the binding
String assetsCustomFieldName = "ON"     // the Assets/Insight custom field name
String assetObjectKey = "ABC-912536"    // the asset object Key you want to set (single-value)

// Additional fields you asked to update
String extraField1Name = "Extra Field 1"
def extraField1Value = "Some value for field 1"

String extraField2Name = "Extra Field 2"
def extraField2Value = "Some value for field 2"

def issueManager = ComponentAccessor.getIssueManager()
def customFieldManager = ComponentAccessor.getCustomFieldManager()
def authContext = ComponentAccessor.getJiraAuthenticationContext()
def currentUser = authContext.getLoggedInUser()

// Resolve the issue: prefer the 'issue' variable from the binding (current issue).
MutableIssue theIssue = null
def scriptBinding = this.binding
if (scriptBinding?.hasVariable('issue') && scriptBinding.issue) {
    try {
        theIssue = scriptBinding.issue as MutableIssue
    } catch (Exception e) {
        log.warn("Found 'issue' in binding but failed to coerce to MutableIssue: ${e.class.name}: ${e.message}", e)
    }
}

if (!theIssue) {
    if (targetIssueKey) {
        theIssue = issueManager.getIssueByCurrentKey(targetIssueKey) as MutableIssue
        if (!theIssue) {
            log.warn("Fallback issue ${targetIssueKey} not found; aborting")
            return
        } else {
            log.info("Using fallback issue ${targetIssueKey}")
        }
    } else {
        log.warn("No 'issue' in binding and no targetIssueKey configured; script is configured to run for the current issue only. Aborting.")
        return
    }
}

// Helper to update a single custom field via explicit API (with direct fallback)
def updateFieldExplicitly = { String fieldName, def newValue ->
    def cf = customFieldManager.getCustomFieldObjectByName(fieldName)
    if (!cf) {
        log.warn("Custom field '${fieldName}' not found; skipping")
        return
    }

    def oldValue = theIssue.getCustomFieldValue(cf)
    try {
        cf.updateValue(null, theIssue, new ModifiedValue(oldValue, newValue), new DefaultIssueChangeHolder())
        log.info("Updated '${fieldName}' on ${theIssue.key} via cf.updateValue")
    } catch (Exception e) {
        log.warn("cf.updateValue failed for '${fieldName}': ${e.class.name}: ${e.message}; attempting direct set on issue", e)
        try {
            theIssue.setCustomFieldValue(cf, newValue)
            log.info("Set '${fieldName}' on ${theIssue.key} via direct setCustomFieldValue fallback")
        } catch (Exception e2) {
            log.warn("Failed to set '${fieldName}' by any method: ${e2.class.name}: ${e2.message}", e2)
        }
    }
}

try {
    // Try ScriptRunner DSL first: set all three fields in a single update closure (works when running in contexts that provide DSL)
    theIssue.update {
        // Assets field often requires a set(...) block with the object Key
        setCustomFieldValue(assetsCustomFieldName) {
            set(assetObjectKey)
        }
        // For the two additional fields, use a straightforward set (works for simple values)
        setCustomFieldValue(extraField1Name) {
            set(extraField1Value)
        }
        setCustomFieldValue(extraField2Name) {
            set(extraField2Value)
        }
    }
    log.info("Set asset and extra fields on ${theIssue.key} via issue.update DSL")
    return
} catch (MissingMethodException mme) {
    // DSL not available here — fall through to explicit API
} catch (Exception e) {
    log.warn("Failed using issue.update DSL: ${e.class.name}: ${e.message}", e)
    // Attempt explicit API fallback below
}

// Explicit API fallback: update each field individually
updateFieldExplicitly(assetsCustomFieldName, assetObjectKey)
updateFieldExplicitly(extraField1Name, extraField1Value)
updateFieldExplicitly(extraField2Name, extraField2Value)

// Persist the issue if any direct setCustomFieldValue occurred in fallback path
try {
    issueManager.updateIssue(currentUser, theIssue, EventDispatchOption.ISSUE_UPDATED, false)
    log.info("Persisted updates to ${theIssue.key}")
} catch (Exception e) {
    log.warn("Failed to persist updates for ${theIssue.key}: ${e.class.name}: ${e.message}", e)
}

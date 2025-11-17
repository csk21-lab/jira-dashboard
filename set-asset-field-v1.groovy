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
String targetIssueKey = "ABC-212"            // used when 'issue' is not in the binding
String assetsCustomFieldName = "ON"          // the Assets/Insight custom field name
String assetObjectKey = "ABC-912536"         // the asset object Key you want to set (single-value)

// Additional fields you asked to update
// Adjust names and values to your needs (could be Strings, numbers, maps, lists depending on field type)
String extraField1Name = "Extra Field 1"
def extraField1Value = "Some value for field 1"

String extraField2Name = "Extra Field 2"
def extraField2Value = "Some value for field 2"

def issueManager = ComponentAccessor.getIssueManager()
def customFieldManager = ComponentAccessor.getCustomFieldManager()
def authContext = ComponentAccessor.getJiraAuthenticationContext()
def currentUser = authContext.getLoggedInUser()

// Resolve the issue: try to reference the 'issue' variable directly so ScriptRunner will attempt autowiring
MutableIssue theIssue
try {
    // Accessing 'issue' directly (instead of checking binding.hasVariable('issue')) allows ScriptRunner
    // to attempt autowiring and therefore emit its autowiring logs if enabled.
    theIssue = issue as MutableIssue
} catch (MissingPropertyException mpe) {
    // 'issue' wasn't provided in the binding — fall back to loading by key (no autowiring logs)
    theIssue = issueManager.getIssueByCurrentKey(targetIssueKey) as MutableIssue
    if (!theIssue) {
        log.warn("Issue ${targetIssueKey} not found")
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
    // Try ScriptRunner DSL first: set all three fields in a single update closure
    theIssue.update {
        // Assets field often requires a set(...) block with the object Key
        setCustomFieldValue(assetsCustomFieldName) {
            set(assetObjectKey)
        }
        // For the two additional fields, use a straightforward set (works for simple values)
        // If these require special handling (objects/option objects), adjust accordingly.
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
// Assets field: set by object Key (single-value)
updateFieldExplicitly(assetsCustomFieldName, assetObjectKey)
// Extra fields
updateFieldExplicitly(extraField1Name, extraField1Value)
updateFieldExplicitly(extraField2Name, extraField2Value)

// Persist the issue if any direct setCustomFieldValue occurred in fallback path
try {
    issueManager.updateIssue(currentUser, theIssue, EventDispatchOption.ISSUE_UPDATED, false)
    log.info("Persisted updates to ${theIssue.key}")
} catch (Exception e) {
    log.warn("Failed to persist updates for ${theIssue.key}: ${e.class.name}: ${e.message}", e)
}

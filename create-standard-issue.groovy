import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.bc.issue.IssueService
import com.atlassian.jira.issue.IssueInputParameters
import com.atlassian.jira.user.ApplicationUser

// =====================
// CONFIGURE THESE
// =====================
final String SOURCE_ISSUE_KEY      = "ABC-123"      // The issue with the Confluence table in Description
final String TARGET_PROJECT_KEY    = "NEW"          // Where to create the new issues
final String TARGET_ISSUE_TYPE_ID  = "10001"        // Standard issue type id (e.g. Task)
// Optional: link type to relate back. Set to null to skip linking.
final String ISSUE_LINK_TYPE_NAME  = "Relates"      // E.g. "Relates" (must exist in Jira or set to null)
// Header text we are looking for (case-insensitive, trims whitespace)
final String USER_ID_HEADER_TEXT   = "USER ID"

// =====================
// SETUP
// =====================
def issueManager      = ComponentAccessor.issueManager
def customFieldMgr    = ComponentAccessor.customFieldManager
def issueService      = ComponentAccessor.issueService
def linkManager       = ComponentAccessor.issueLinkManager
ApplicationUser user  = ComponentAccessor.jiraAuthenticationContext.loggedInUser

def sourceIssue = issueManager.getIssueByCurrentKey(SOURCE_ISSUE_KEY)
assert sourceIssue : "Source issue not found: $SOURCE_ISSUE_KEY"

def description = sourceIssue.getDescription()
assert description : "Source issue has no Description."

// =====================
// PARSE CONFLUENCE TABLE (WIKI MARKUP)
// =====================
// Accepts either:
// || USER ID || Name || ... ||
// | user1 | Alice | ... |
// | user2 | Bob   | ... |

def lines = description.readLines().findAll { it.trim() }
def tableLines = lines.findAll { it.startsWith('|') || it.startsWith('||') }
assert tableLines && tableLines.size() >= 2 : "Table must have a header row and at least one data row."

// Parse header row (starts with '||')
def headerLine = tableLines.find { it.startsWith('||') }
assert headerLine : "No table header found (row starting with '||')."
def headerCells = headerLine.replaceAll(/^(\|\|)+/, '').split(/\|\|/)*.trim()
int userIdColIdx = headerCells.findIndexOf { it.equalsIgnoreCase(USER_ID_HEADER_TEXT) }
assert userIdColIdx >= 0 : "Could not find a header named '${USER_ID_HEADER_TEXT}' in the table header row."

log.warn("USER ID column index detected as: ${userIdColIdx}")

// Parse data rows (start with single '|', not '||')
def dataRows = tableLines.findAll { it.startsWith('|') && !it.startsWith('||') }
assert dataRows : "No data rows found in table."

// =====================
// CREATE ISSUES
// =====================
def createdKeys = []
dataRows.eachWithIndex { row, r ->
    def dataCells = row.replaceAll(/^\|+/, '').split(/\|/)*.trim()
    if (dataCells.size() <= userIdColIdx) {
        log.warn("Row ${r + 1} has no USER ID column or is malformed; skipping.")
        return
    }
    def userIdValue = dataCells[userIdColIdx]
    if (!userIdValue) {
        log.warn("Row ${r + 1}: USER ID is blank; skipping.")
        return
    }

    def newSummary = "Task for USER ID: ${userIdValue}"
    def newDescription = """\
Auto-created from ${SOURCE_ISSUE_KEY}
Table row ${r + 1}, USER ID: ${userIdValue}
""".stripIndent()

    IssueInputParameters params = issueService.newIssueInputParameters()
    params.setProjectKey(TARGET_PROJECT_KEY)
          .setIssueTypeId(TARGET_ISSUE_TYPE_ID)
          .setSummary(newSummary)
          .setDescription(newDescription)
          .setReporterId(user.key) // Use user.accountId if on Jira Cloud

    def validate = issueService.validateCreate(user, params)
    if (!validate.isValid()) {
        log.error("Validation failed for row ${r+1} (USER ID: ${userIdValue}): ${validate.errorCollection}")
        return
    }

    def create = issueService.create(user, validate)
    if (!create.isValid()) {
        log.error("Create failed for row ${r+1} (USER ID: ${userIdValue}): ${create.errorCollection}")
        return
    }

    def newIssue = create.issue
    createdKeys << newIssue.key
    log.warn("Created ${newIssue.key} for USER ID ${userIdValue}")

    // Optional: link back to source (if link type is specified)
    if (ISSUE_LINK_TYPE_NAME) {
        try {
            def linkType = ComponentAccessor.issueLinkTypeManager.issueLinkTypes.find { it.name == ISSUE_LINK_TYPE_NAME }
            if (linkType) {
                linkManager.createIssueLink(sourceIssue.id, newIssue.id, linkType.id, 0L, user)
                log.warn("Linked ${newIssue.key} to ${SOURCE_ISSUE_KEY} via '${ISSUE_LINK_TYPE_NAME}'")
            } else {
                log.warn("Link type '${ISSUE_LINK_TYPE_NAME}' not found; skipping link.")
            }
        } catch (Throwable t) {
            log.error("Failed to create link to ${newIssue.key}: ${t.message}", t)
        }
    }
}

log.warn("Done. Created issues: ${createdKeys}")
return createdKeys

import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.bc.issue.IssueService
import com.atlassian.jira.issue.IssueInputParameters
import com.atlassian.jira.issue.MutableIssue
import com.atlassian.jira.issue.link.IssueLinkManager
import com.atlassian.jira.user.ApplicationUser
import org.jsoup.Jsoup

// =====================
// CONFIGURE THESE
// =====================
final String SOURCE_ISSUE_KEY     = "ABC-123"  // issue containing the table in Description
final String TARGET_PROJECT_KEY   = "NEW"      // project where new issues will be created
final String TARGET_ISSUE_TYPE_ID = "10001"    // e.g., Task issue type ID (Admin → Issues → Issue types)
final String USER_ID_HEADER_TEXT  = "USER ID"  // header name (case-insensitive)

// Optional link back to source (set to null to skip)
final String ISSUE_LINK_TYPE_NAME = "Relates"

// =====================
// SETUP
// =====================
def issueManager   = ComponentAccessor.issueManager
def issueService   = ComponentAccessor.issueService
def linkManager    = ComponentAccessor.getComponent(IssueLinkManager)
ApplicationUser me = ComponentAccessor.jiraAuthenticationContext.loggedInUser

MutableIssue source = issueManager.getIssueByCurrentKey(SOURCE_ISSUE_KEY)
assert source : "Source issue not found: $SOURCE_ISSUE_KEY"

def description = source.description
assert description : "Source issue has no Description."

// Parse description HTML and locate first table
def doc   = Jsoup.parse(description)
def table = doc.select("table").first()
assert table : "No <table> found in Description of ${SOURCE_ISSUE_KEY}."

def rows = table.select("tr")
assert rows && rows.size() >= 2 : "Table must have a header row and at least one data row."

// Find the USER ID column index by header text
def headerCells = rows.get(0).select("th, td")
int userIdColIdx = -1
headerCells.eachWithIndex { h, idx ->
    if ((h.text() ?: "").trim().equalsIgnoreCase(USER_ID_HEADER_TEXT)) {
        userIdColIdx = idx
    }
}
assert userIdColIdx >= 0 : "Could not find a header named '${USER_ID_HEADER_TEXT}' in the first row."

log.warn("USER ID column index: ${userIdColIdx}")

// Create standard issues in target project
def created = []
for (int r = 1; r < rows.size(); r++) { // skip header
    def cells = rows.get(r).select("td")
    if (cells.isEmpty() || userIdColIdx >= cells.size()) {
        log.warn("Row ${r}: malformed or missing USER ID column. Skipping.")
        continue
    }

    def userId = (cells.get(userIdColIdx).text() ?: "").trim()
    if (!userId) {
        log.warn("Row ${r}: USER ID is blank. Skipping.")
        continue
    }

    def summary     = "Task for USER ID: ${userId}"
    def newDesc     = """\
Auto-created from ${SOURCE_ISSUE_KEY}
Table row ${r}, USER ID: ${userId}
""".stripIndent()

    IssueInputParameters params = issueService.newIssueInputParameters()
    params.setProjectKey(TARGET_PROJECT_KEY)
          .setIssueTypeId(TARGET_ISSUE_TYPE_ID)
          .setSummary(summary)
          .setDescription(newDesc)
          .setReporterId(me.key)

    def validate = issueService.validateCreate(me, params)
    if (!validate.isValid()) {
        log.error("Validation failed (row ${r}, USER ID ${userId}): ${validate.errorCollection}")
        continue
    }

    def createdResult = issueService.create(me, validate)
    if (!createdResult.isValid()) {
        log.error("Create failed (row ${r}, USER ID ${userId}): ${createdResult.errorCollection}")
        continue
    }

    def newIssue = createdResult.issue
    created << newIssue.key
    log.warn("Created ${newIssue.key} for USER ID ${userId}")

    // Optional link back to source
    if (ISSUE_LINK_TYPE_NAME) {
        def linkType = ComponentAccessor.issueLinkTypeManager.issueLinkTypes.find { it.name == ISSUE_LINK_TYPE_NAME }
        if (linkType) {
            try {
                linkManager.createIssueLink(source.id, newIssue.id, linkType.id, 0L, me)
                log.warn("Linked ${newIssue.key} to ${SOURCE_ISSUE_KEY} via '${ISSUE_LINK_TYPE_NAME}'")
            } catch (Throwable t) {
                log.error("Linking failed for ${newIssue.key}: ${t.message}", t)
            }
        } else {
            log.warn("Issue link type '${ISSUE_LINK_TYPE_NAME}' not found; skipping link.")
        }
    }
}

log.warn("Done. Created issues: ${created}")
return created

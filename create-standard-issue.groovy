import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.issue.MutableIssue
import com.atlassian.jira.user.ApplicationUser

// =====================
// CONFIGURE THESE
// =====================
final String SOURCE_ISSUE_KEY      = "ABC-123"      // The issue with the table in Description
final String TARGET_PROJECT_KEY    = "NEW"          // Where to create the new issues
final String TARGET_ISSUE_TYPE_ID  = "10001"        // Standard issue type id (e.g. Task)
final String ISSUE_LINK_TYPE_NAME  = "Relates"      // E.g. "Relates" (must exist in Jira or set to null)
final String USER_ID_HEADER_TEXT   = "USER ID"      // Flexible match: "USER ID", "USER_ID", "UserId"

// =====================
// SETUP
// =====================
def issueManager      = ComponentAccessor.issueManager
def customFieldMgr    = ComponentAccessor.customFieldManager
def projectManager    = ComponentAccessor.projectManager
def issueFactory      = ComponentAccessor.issueFactory
def linkManager       = ComponentAccessor.issueLinkManager
def userManager       = ComponentAccessor.userManager
ApplicationUser loggedInUser = ComponentAccessor.jiraAuthenticationContext.loggedInUser

def sourceIssue = issueManager.getIssueByCurrentKey(SOURCE_ISSUE_KEY)
assert sourceIssue : "Source issue not found: $SOURCE_ISSUE_KEY"

def description = sourceIssue.getDescription()
assert description : "Source issue has no Description."

// =====================
// PARSE TABLE (Flexible for single or double pipe headers)
// =====================
// Accepts:
// || USER ID || Name || Role ||
// | USER_ID | Name | Role |
// | USER ID | Name | Role |
// (single or double pipe, spaces/underscores/case-insensitive)

def lines = description.readLines().findAll { it.trim() }
def tableLines = lines.findAll { it.startsWith('|') || it.startsWith('||') }
assert tableLines && tableLines.size() >= 2 : "Table must have a header row and at least one data row."

// Find header row: first table line (|| or |)
def headerLine = tableLines.find { it.startsWith('||') } ?: tableLines[0]
def headerCells = headerLine.replaceAll(/^\|+/, '').replaceAll(/\|+$/, '').split(/\|{2,}|\|/)*.trim()

// Flexible header matching
String normalizedHeader = USER_ID_HEADER_TEXT.replaceAll(/[\s_]/, '').toLowerCase()
int userIdColIdx = headerCells.findIndexOf { cell ->
    cell.replaceAll(/[\s_]/, '').toLowerCase() == normalizedHeader
}
assert userIdColIdx >= 0 : "Could not find a header named '${USER_ID_HEADER_TEXT}' in the table header row."

log.warn("USER ID column index detected as: ${userIdColIdx}")

// Data rows: exclude header
def dataRows = tableLines.findAll { it != headerLine }

// =====================
// CREATE ISSUES (Direct, flexible)
// =====================
def targetProject = projectManager.getProjectObjByKey(TARGET_PROJECT_KEY)
assert targetProject : "Target project not found: $TARGET_PROJECT_KEY"

def createdKeys = []
dataRows.eachWithIndex { row, r ->
    def dataCells = row.replaceAll(/^\|+/, '').replaceAll(/\|+$/, '').split(/\|{2,}|\|/)*.trim()
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

    // Direct issue creation
    MutableIssue issue = issueFactory.issue
    issue.projectObject = targetProject
    issue.summary = newSummary
    issue.issueTypeId = TARGET_ISSUE_TYPE_ID
    issue.assignee = loggedInUser
    issue.reporter = loggedInUser
    issue.description = newDescription

    // (Optional) Set custom fields by name here if needed, e.g.:
    // def customField = customFieldMgr.getCustomFieldObjectByName("Your Custom Field")
    // if (customField) issue.setCustomFieldValue(customField, someValue)

    try {
        def createdIssue = issueManager.createIssueObject(loggedInUser, issue)
        createdKeys << createdIssue.key
        log.warn("Created ${createdIssue.key} for USER ID ${userIdValue}")

        // Optional: link back to source (if link type is specified)
        if (ISSUE_LINK_TYPE_NAME) {
            def linkType = ComponentAccessor.issueLinkTypeManager.issueLinkTypes.find { it.name == ISSUE_LINK_TYPE_NAME }
            if (linkType) {
                linkManager.createIssueLink(sourceIssue.id, createdIssue.id, linkType.id, 0L, loggedInUser)
                log.warn("Linked ${createdIssue.key} to ${SOURCE_ISSUE_KEY} via '${ISSUE_LINK_TYPE_NAME}'")
            } else {
                log.warn("Link type '${ISSUE_LINK_TYPE_NAME}' not found; skipping link.")
            }
        }
    } catch (Exception e) {
        log.error("Error creating issue for USER ID ${userIdValue}: ${e.message}")
    }
}

log.warn("Done. Created issues: ${createdKeys}")
return createdKeys

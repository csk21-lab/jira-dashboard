import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.issue.IssueService
import com.atlassian.jira.issue.IssueInputParameters

def issueManager = ComponentAccessor.issueManager
def customFieldManager = ComponentAccessor.customFieldManager
def issueService = ComponentAccessor.getIssueService()
def user = ComponentAccessor.jiraAuthenticationContext.loggedInUser

// Set your JQL to find relevant parent issues
def jqlSearch = "project = YOUR_PROJECT AND issuetype != Sub-task"
// Use Jira's SearchService to get issues
def searchService = ComponentAccessor.getComponent(com.atlassian.jira.issue.search.SearchService)
def query = searchService.parseQuery(user, jqlSearch).query
def results = searchService.search(user, query, com.atlassian.jira.web.bean.PagerFilter.getUnlimitedFilter())
def issues = results.issues

issues.each { issue ->
    def scheduleType = issue.getCustomFieldValue(customFieldManager.getCustomFieldObjectByName("Schedule Type"))?.toString()
    def startDate = issue.getCustomFieldValue(customFieldManager.getCustomFieldObjectByName("Start Date"))
    def endDate = issue.getCustomFieldValue(customFieldManager.getCustomFieldObjectByName("End Date"))
    def today = new Date().clearTime()

    // --- Only process tickets within start/end date ---
    if (!startDate || !endDate) {
        return // skip if missing dates
    }
    // Convert startDate/endDate to java.util.Date if needed
    def sDate = startDate instanceof java.sql.Timestamp ? new Date(startDate.time) : startDate
    def eDate = endDate instanceof java.sql.Timestamp ? new Date(endDate.time) : endDate

    if (today.before(sDate) || today.after(eDate)) {
        return // skip if today not in range
    }

    // --- Only process Weekly/Monthly schedule types ---
    if (!(scheduleType in ["Weekly", "Monthly"])) {
        return
    }

    // --- Ensure no open subtask exists ---
    def hasOpenSubtask = issue.getSubTaskObjects().any { subtask ->
        subtask.status.name == "Open"
    }
    if (hasOpenSubtask) {
        return // skip if there is already an open subtask
    }

    // --- Prepare subtask summary and description ---
    def subtaskSummary = "Scheduled ${scheduleType} Subtask"
    def subtaskDesc = "Automatically created based on Schedule Type (${scheduleType}).\nDuration: ${sDate.format('yyyy-MM-dd')} - ${eDate.format('yyyy-MM-dd')}"

    // --- Create subtask ---
    def issueInputParameters = issueService.newIssueInputParameters()
    issueInputParameters
        .setProjectId(issue.projectObject.id)
        .setIssueTypeId(getSubtaskIssueTypeId(issue.projectObject))
        .setSummary(subtaskSummary)
        .setDescription(subtaskDesc)
        .setReporterId(user.key)
        .setParentIssueId(issue.id)
        .addCustomFieldValue(customFieldManager.getCustomFieldObjectByName("Start Date").id, sDate.format('yyyy-MM-dd'))
        .addCustomFieldValue(customFieldManager.getCustomFieldObjectByName("End Date").id, eDate.format('yyyy-MM-dd'))
        .addCustomFieldValue(customFieldManager.getCustomFieldObjectByName("Schedule Type").id, scheduleType)

    def createValidationResult = issueService.validateSubTaskCreate(user, issue.id, issueInputParameters)
    if (createValidationResult.isValid()) {
        def createResult = issueService.create(user, createValidationResult)
        if (!createResult.isValid()) {
            log.warn("Failed to create subtask for issue ${issue.key}: ${createResult.errorCollection}")
        }
    } else {
        log.warn("Subtask create validation failed for issue ${issue.key}: ${createValidationResult.errorCollection}")
    }
}

// Helper to get the subtask issue type ID for the project
def getSubtaskIssueTypeId(project) {
    def issueTypeSchemeManager = ComponentAccessor.getIssueTypeSchemeManager()
    def subtaskTypes = issueTypeSchemeManager.getSubTaskIssueTypes()
    return subtaskTypes ? subtaskTypes[0].id : null // choose first subtask type
}

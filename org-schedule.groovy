import com.atlassian.jira.component.ComponentAccessor

def issueManager = ComponentAccessor.getIssueManager()
def customFieldManager = ComponentAccessor.getCustomFieldManager()
def user = ComponentAccessor.getJiraAuthenticationContext().getLoggedInUser()
def issueFactory = ComponentAccessor.getIssueFactory()

// Set your test issue key
def specificIssueKey = "YOUR-123" // Change to your specific issue key

// Get the specific issue
def issue = issueManager.getIssueByCurrentKey(specificIssueKey)
if (!issue) {
    log.warn("Issue ${specificIssueKey} not found.")
    return
}

// Get custom fields
def scheduleType = issue.getCustomFieldValue(customFieldManager.getCustomFieldObjectByName("Schedule Type"))?.toString()
def startDate = issue.getCustomFieldValue(customFieldManager.getCustomFieldObjectByName("Start Date"))
def endDate = issue.getCustomFieldValue(customFieldManager.getCustomFieldObjectByName("End Date"))
def today = new Date().clearTime()

// Only proceed if startDate and endDate exist
if (!startDate || !endDate) {
    log.warn("Start Date or End Date is missing for issue ${specificIssueKey}.")
    return
}

// Convert to java.util.Date if needed
def sDate = startDate instanceof java.sql.Timestamp ? new Date(startDate.time) : startDate
def eDate = endDate instanceof java.sql.Timestamp ? new Date(endDate.time) : endDate

// Check if today is within range
if (today.before(sDate) || today.after(eDate)) {
    log.warn("Today (${today}) is not within Start Date (${sDate}) and End Date (${eDate}) for issue ${specificIssueKey}.")
    return
}

// Only process Weekly/Monthly schedule types
if (!(scheduleType in ["Weekly", "Monthly"])) {
    log.warn("Schedule Type is not Weekly or Monthly for issue ${specificIssueKey}.")
    return
}

// Ensure no open subtask exists
def hasOpenSubtask = issue.getSubTaskObjects().any { subtask ->
    subtask.status.name == "Open"
}
if (hasOpenSubtask) {
    log.warn("Issue ${specificIssueKey} already has an open subtask.")
    return
}

// Prepare subtask
def subTaskIssueType = ComponentAccessor.getConstantsManager().getAllIssueTypeObjects().find { it.isSubTask() }
if (!subTaskIssueType) {
    log.warn("No subtask issue type found.")
    return
}
def subTaskSummary = "Scheduled ${scheduleType} Subtask"
def subTaskDescription = "Automatically created based on Schedule Type (${scheduleType}).\nDuration: ${sDate.format('yyyy-MM-dd')} - ${eDate.format('yyyy-MM-dd')}"

def newSubTask = issueFactory.getIssue()
newSubTask.setSummary(subTaskSummary)
newSubTask.setDescription(subTaskDescription)
newSubTask.setProjectObject(issue.getProjectObject())
newSubTask.setIssueTypeId(subTaskIssueType.id)
newSubTask.setReporter(user)
newSubTask.setParentObject(issue)

// Set custom fields on subtask
newSubTask.setCustomFieldValue(customFieldManager.getCustomFieldObjectByName("Start Date"), sDate)
newSubTask.setCustomFieldValue(customFieldManager.getCustomFieldObjectByName("End Date"), eDate)
newSubTask.setCustomFieldValue(customFieldManager.getCustomFieldObjectByName("Schedule Type"), scheduleType)

// Create subtask
def createdIssue = issueManager.createIssueObject(user, newSubTask)
if (createdIssue) {
    log.info("Subtask created for issue ${specificIssueKey}: ${createdIssue.key}")
} else {
    log.warn("Failed to create subtask for issue ${specificIssueKey}.")
}

import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.user.ApplicationUser
import com.atlassian.jira.event.type.EventDispatchOption

// Configuration
def issueKey = "PROJECT-123" // Replace with your issue key
def customFieldName = "Multiline Custom Field Name"
def newValuesToAdd = ["abc-123", "def-2543", "cdef-245"] // Replace with your new values

// Get necessary managers and services
def customFieldManager = ComponentAccessor.customFieldManager
def issueManager = ComponentAccessor.issueManager
def loggedInUser = ComponentAccessor.jiraAuthenticationContext.loggedInUser as ApplicationUser

// Retrieve the issue
def issue = issueManager.getIssueByCurrentKey(issueKey) as MutableIssue
if (!issue) {
    log.error("Issue with key ${issueKey} not found.")
    return
}

// Retrieve the custom field
def customField = customFieldManager.getCustomFieldObjectByName(customFieldName)
if (!customField) {
    log.error("Custom field ${customFieldName} not found.")
    return
}

// Retrieve the current value of the custom field
def existingValues = issue.getCustomFieldValue(customField)?.toString() ?: ""

// Function to add unique values
def addUniqueValues(existingValues, newValues) {
    def existingSet = existingValues.split(",").collect { it.trim() }.toSet()
    def newSet = newValues.toSet()
    def combinedSet = existingSet + newSet
    return combinedSet.join(", ")
}

// Add new unique values
def updatedValues = addUniqueValues(existingValues, newValuesToAdd)
log.warn("Updated Values: ${updatedValues}")

// Update the custom field
issue.setCustomFieldValue(customField, updatedValues)
issueManager.updateIssue(loggedInUser, issue, EventDispatchOption.DO_NOT_DISPATCH, false)

log.warn("Custom field ${customFieldName} updated successfully for issue ${issueKey}")

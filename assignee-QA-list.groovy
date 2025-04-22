import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.issue.MutableIssue

// Get the issue object (for the context of this behavior)
def issue = underlyingIssue
log.debug("Issue retrieved: ${issue.key}")

// Get the current assignee of the issue
def assignee = issue?.assignee
log.debug("Current assignee: ${assignee?.username ?: 'No assignee'}")

// Get the "QA Analyst" field
def qaAnalystField = getFieldByName("QA Analyst")  // Replace with the exact name of the field if needed
log.debug("Retrieved QA Analyst field: ${qaAnalystField?.getFieldId()}")

// Check if the "QA Analyst" field and assignee are not null
if (qaAnalystField && assignee) {
    // Get the current value (user key) of the "QA Analyst" field
    def currentValue = qaAnalystField.getValue()
    log.debug("Current value in 'QA Analyst' field: ${currentValue}")

    // Check if the current value of the QA Analyst field is the same as the assignee's username
    if (currentValue == assignee.key) {
        log.debug("Assignee is the same as selected in 'QA Analyst'. Showing error on screen.")
        
        // Show an error message on the screen if the assignee is selected in 'QA Analyst'
        qaAnalystField.setErrorMessage("The assignee cannot be selected as the QA Analyst.")
        
        // Optionally, clear the value of the "QA Analyst" field
        qaAnalystField.setFieldValue(null)  // Clear the value if the assignee is selected
    } else {
        // If the assignee is not selected, remove the assignee from the options
        def users = qaAnalystField.getFieldOptions()
        log.debug("Total available users in QA Analyst: ${users.size()}")
        
        def filteredUsers = users.findAll { user ->
            user.key != assignee.key  // Remove the assignee from the options by comparing the user key
        }
        log.debug("Filtered users (Assignee removed): ${filteredUsers.size()} remaining options.")

        // Update the user picker options
        qaAnalystField.setFieldOptions(filteredUsers)
        log.debug("QA Analyst field options updated.")
    }
} else {
    log.warn("QA Analyst field or assignee is null. Assignee: ${assignee?.username}, QA Analyst Field: ${qaAnalystField?.getFieldId()}")
}

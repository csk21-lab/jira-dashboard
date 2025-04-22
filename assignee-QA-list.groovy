import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.issue.MutableIssue

// Get the issue object (for the context of this behavior)
def issue = underlyingIssue

// Get the current assignee of the issue
def assignee = issue?.assignee

// Get the "QA Analyst" field
def qaAnalystField = getFieldByName("QA Analyst")  // Replace with the exact name of the field if needed

// Check if the "QA Analyst" field and assignee are not null
if (qaAnalystField && assignee) {
    // Get the current value (user key) of the "QA Analyst" field
    def currentValue = qaAnalystField.getValue()

    // Check if the current value of the QA Analyst field is the same as the assignee's username
    if (currentValue == assignee.key) {
        // If so, remove the assignee from the "QA Analyst" field options
        qaAnalystField.setFieldValue(null) // Clear the value if the assignee is selected
    }

    // Ensure the assignee is not available in the user picker options
    def users = qaAnalystField.getFieldOptions()
    def filteredUsers = users.findAll { user ->
        user.key != assignee.key  // Remove the assignee from the options by comparing the user key
    }

    // Update the user picker options
    qaAnalystField.setFieldOptions(filteredUsers)
}

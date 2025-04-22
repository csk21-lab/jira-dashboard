import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.issue.MutableIssue
import com.atlassian.jira.user.ApplicationUser

// Get the issue object
def issue = getIssueContext()

// Get the assignee of the issue
def assignee = issue?.assignee

// Get the custom field for the QA Analyst user picker
def qaAnalystField = getFieldByName("QA Analyst") // Replace with the exact name of the field

if (qaAnalystField && assignee) {
    // Get the user manager
    def userManager = ComponentAccessor.getUserManager()
    
    // Get all users in the user picker options for "QA Analyst"
    def userPickerOptions = qaAnalystField.getField().getCustomFieldType().getConfigurationSchemes()

    // Remove the assignee from the options list
    def updatedOptions = userPickerOptions.findAll { user ->
        return user.username != assignee.username // Remove the assignee from the list
    }
    
    // Set the updated options
    qaAnalystField.setFieldValue(updatedOptions)
} else {
    log.debug("Assignee or QA Analyst field is null. Assignee: ${assignee}, QA Analyst Field: ${qaAnalystField}")
}

// Get the issue object
def issue = getIssueContext()

// Get the assignee of the issue
def assignee = issue?.assignee

// Get the custom field for the QA Analyst user picker
def qaAnalystField = getFieldByName("QA Analyst") // Replace with the exact name of the field

if (qaAnalystField) {
    // Get the current list of users in the QA Analyst field
    def availableUsers = qaAnalystField.getFieldValue()

    if (availableUsers && assignee) {
        // Remove the assignee from the options
        def updatedUsers = availableUsers.findAll { it != assignee }
        qaAnalystField.setFieldValue(updatedUsers)
    } else {
        log.debug("Assignee or available users are null. Assignee: ${assignee}, Available Users: ${availableUsers}")
    }
} else {
    log.debug("QA Analyst field not found.")
}

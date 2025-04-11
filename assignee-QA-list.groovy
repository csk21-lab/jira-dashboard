// Get the issue object
def issue = getIssueContext()

// Get the assignee of the issue
def assignee = issue.assignee

// Get the custom field for the QA Analyst user picker
def qaAnalystField = getFieldByName("QA Analyst") // Replace with the exact name of the field

// Get the current list of users in the QA Analyst field
def availableUsers = qaAnalystField.getFieldValue()

// If assignee is in the QA Analyst field, remove them from the options
if (assignee) {
    def updatedUsers = availableUsers.findAll { it != assignee }
    qaAnalystField.setFieldValue(updatedUsers)
}

//Script to create subtask based on element connect field 

import com.atlassian.jira.component.ComponentAccessor

def issue = issue // Current issue context

// Replace with your Elements Connect custom field ID
def customFieldId = "customfield_12345"
def customField = ComponentAccessor.customFieldManager.getCustomFieldObject(customFieldId)
def selectedValues = issue.getCustomFieldValue(customField)

if (selectedValues) {
    def user = ComponentAccessor.jiraAuthenticationContext.loggedInUser
    def issueService = ComponentAccessor.getIssueService()
    selectedValues.each { value ->
        def issueInputParameters = issueService.newIssueInputParameters()
        issueInputParameters
            .setProjectId(issue.projectObject.id)
            .setIssueTypeId("5") // Replace with your sub-task issue type ID
            .setSummary(value.toString()) // Set summary to the selected value
            .setReporterId(user.name)
            .setParentIssueId(issue.id)

        def validationResult = issueService.validateSubTaskCreate(user, issue.id, issueInputParameters)
        if (validationResult.isValid()) {
            issueService.create(user, validationResult)
        }
    }
}

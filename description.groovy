import com.atlassian.jira.bc.issue.IssueService;
import com.atlassian.jira.bc.issue.IssueService.UpdateValidationResult;
import com.atlassian.jira.bc.issue.IssueService.IssueResult;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.issue.IssueInputParameters;
import com.atlassian.jira.user.ApplicationUser;

public class UpdateIssueDescription {
    public void updateIssueDescription(ApplicationUser user, Long issueId) {
        IssueService issueService = ComponentAccessor.getIssueService();
        IssueInputParameters issueInputParameters = issueService.newIssueInputParameters(); 
        issueInputParameters.setDescription("I am a new description"); 
        UpdateValidationResult updateValidationResult = issueService.validateUpdate(user, issueId, issueInputParameters); 
        if (updateValidationResult.isValid()) { 
            IssueResult updateResult = issueService.update(user, updateValidationResult); 
            if (!updateResult.isValid()) { 
                // Do something 
            } 
        }
    }
}


import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.issue.Issue
import com.atlassian.jira.issue.customfields.manager.CustomFieldManager
import com.atlassian.jira.issue.fields.CustomField

// Get the current issue
Issue issue = ComponentAccessor.getIssueManager().getIssueObject("ISSUE-KEY") // Replace with your issue key

// Get the custom field manager
CustomFieldManager customFieldManager = ComponentAccessor.getCustomFieldManager()

// Define your custom fields
CustomField customField1 = customFieldManager.getCustomFieldObjectByName("Custom Field 1")
CustomField customField2 = customFieldManager.getCustomFieldObjectByName("Custom Field 2")
CustomField customField3 = customFieldManager.getCustomFieldObjectByName("Custom Field 3")

// Get the values of the custom fields
def customFieldValue1 = issue.getCustomFieldValue(customField1)
def customFieldValue2 = issue.getCustomFieldValue(customField2)
def customFieldValue3 = issue.getCustomFieldValue(customField3)

// Create the table in Markdown format
def table = """
| Field Name   | Description        |
|--------------|--------------------|
| Custom Field 1 | ${customFieldValue1 ?: 'N/A'} |
| Custom Field 2 | ${customFieldValue2 ?: 'N/A'} |
| Custom Field 3 | ${customFieldValue3 ?: 'N/A'} |
"""

// Update the issue description
issue.setDescription(issue.getDescription() + "\n\n" + table)

// Save the changes
ComponentAccessor.getIssueManager().updateIssue(ComponentAccessor.getJiraAuthenticationContext().getLoggedInUser(), issue, com.atlassian.jira.event.type.EventDispatchOption.DO_NOT_DISPATCH, false)

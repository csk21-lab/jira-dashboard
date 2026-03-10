/*
To create subtasks based on a multi-select field in Jira Data Center using ScriptRunner,
you can use a Post Function on the "Create" or "Update" transition. 
The script will iterate through the selected values and create a subtask for each, with special logic to handle an "ALL" option.
*/

import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.issue.Issue
import com.atlassian.jira.issue.MutableIssue

def issueManager = ComponentAccessor.issueManager
def customFieldManager = ComponentAccessor.customFieldManager
def constantManager = ComponentAccessor.constantManager
def user = ComponentAccessor.jiraAuthenticationContext.loggedInUser

// Replace with your actual Multi-Select Custom Field ID
def multiSelectField = customFieldManager.getCustomFieldObject("customfield_10100")
def selectedOptions = (issue.getCustomFieldValue(multiSelectField) as List)?.collect { it.toString() }

if (!selectedOptions) return

def optionsToProcess = []
if (selectedOptions.contains("ALL")) {
    // Get all possible options for the field except "ALL"
    def fieldConfig = multiSelectField.getRelevantConfig(issue)
    optionsToProcess = ComponentAccessor.optionsManager.getOptions(fieldConfig).collect { it.value }
    optionsToProcess.remove("ALL")
} else {
    optionsToProcess = selectedOptions
}

// Create a subtask for each identified option
optionsToProcess.each { optionValue ->
    MutableIssue newSubtask = issueManager.getIssueObject(issue.id).with { parent ->
        def subtask = issueManager.getIssueFactory().getIssue()
        subtask.setSummary("Subtask for: ${optionValue}")
        subtask.setParentObject(parent)
        subtask.setProjectObject(parent.projectObject)
        subtask.setIssueTypeId(constantManager.allIssueTypeObjects.find { it.subTask }.id)
        return subtask
    }
    
    issueManager.createIssueObject(user, newSubtask)
    ComponentAccessor.subTaskManager.createSubTaskIssueLink(issue, newSubtask, user)
}

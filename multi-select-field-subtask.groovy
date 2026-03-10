import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.issue.MutableIssue

def issueManager = ComponentAccessor.issueManager
def customFieldManager = ComponentAccessor.customFieldManager
def constantsManager = ComponentAccessor.constantsManager
def user = ComponentAccessor.jiraAuthenticationContext.loggedInUser
def subTaskManager = ComponentAccessor.subTaskManager
def optionsManager = ComponentAccessor.optionsManager

// 1️⃣ Get the Multi-Select Custom Field (Update ID as needed)
def multiSelectField = customFieldManager.getCustomFieldObject("customfield_10100")
def selectedOptions = (issue.getCustomFieldValue(multiSelectField) as List)
if (!selectedOptions) return

def optionsToProcess = []

// 2️⃣ Handle "ALL" logic
if (selectedOptions.any { it.toString() == "ALL" }) {
    def fieldConfig = multiSelectField.getRelevantConfig(issue)
    optionsToProcess = optionsManager.getOptions(fieldConfig).collect { it.value }
    optionsToProcess.remove("ALL")
} else {
    optionsToProcess = selectedOptions.collect { it.toString() }
}

// 3️⃣ Create a subtask for each option
optionsToProcess.each { optionValue ->
    MutableIssue newSubtask = ComponentAccessor.issueFactory.getIssue()
    newSubtask.setSummary("Subtask for: ${optionValue}")
    newSubtask.setParentObject(issue)
    newSubtask.setProjectObject(issue.projectObject)
    
    // Find the subtask issue type using the corrected constantsManager
    def subtaskType = constantsManager.allIssueTypeObjects.find { it.subTask }
    if (subtaskType) {
        newSubtask.setIssueTypeId(subtaskType.id)
        
        // 4️⃣ Persist and Link the subtask
        def subtaskObject = issueManager.createIssueObject(user, newSubtask)
        subTaskManager.createSubTaskIssueLink(issue, subtaskObject, user)
    }
}

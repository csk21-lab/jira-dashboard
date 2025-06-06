import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.issue.ModifiedValue
import com.atlassian.jira.issue.util.DefaultIssueChangeHolder
import com.atlassian.jira.issue.label.LabelManager
import com.atlassian.jira.user.ApplicationUser

def issueKey = "ABCD-4679"
def issueManager = ComponentAccessor.issueManager
def issue = issueManager.getIssueObject(issueKey)
def currentUser = ComponentAccessor.jiraAuthenticationContext.getLoggedInUser()

def customFieldManager = ComponentAccessor.customFieldManager
def appField = customFieldManager.getCustomFieldObject("customfield_16") // Asset field ID
def selectedObjects = issue.getCustomFieldValue(appField) as List

if (!selectedObjects || selectedObjects.isEmpty()) {
    log.warn("No objects selected in the asset field.")
    return
}

def firstObject = selectedObjects[0]
def departmentIds = firstObject?.getAttributeValues("Department ID")*.textValue?.findAll { it }
def departmentIdValue = departmentIds ? departmentIds[0] : null

if (!departmentIdValue) {
    log.warn("No Department ID found in asset.")
    return
}

log.warn("Setting Label to Department ID: ${departmentIdValue}")

// Set label using LabelManager
LabelManager labelManager = ComponentAccessor.getComponent(LabelManager)
Set<String> labels = [departmentIdValue] as Set

labelManager.setLabels(currentUser, issue.id, labels, false, false)
log.warn("Label field updated with: ${labels}")

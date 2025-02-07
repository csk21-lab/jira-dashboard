def text = "DEV - Development"

// Use a regex pattern that captures everything before the dash (with optional spaces around it)
def pattern = /^(.*?)\s*-\s*/
def matcher = text =~ pattern

if (matcher.find()) {
    def extracted = matcher.group(1)
    println "Extracted text: ${extracted}"  // Should print "DEV"
}


def text = "DEV - Development"

// Split the string on the dash and trim the result to remove extra whitespace
def extracted = text.split('-')[0].trim()
println "Extracted text: ${extracted}"
log.info("Extracted text: ${extracted}")


import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.issue.MutableIssue
import com.atlassian.jira.issue.fields.CustomField

// Define the issue key for which you want to update the custom field value
def issueKey = "PROJ-123"  // Replace with your actual issue key

// Define the new value you want to set
def newValue = "New Value" // Replace with the desired new value

// Retrieve managers
def issueManager = ComponentAccessor.getIssueManager()
def customFieldManager = ComponentAccessor.getCustomFieldManager()

// Get the issue as a MutableIssue so that it can be updated
MutableIssue issue = issueManager.getIssueObject(issueKey) as MutableIssue

// Retrieve the custom field object by its name (or you can use getCustomFieldObject(customFieldId))
def myCustomField = customFieldManager.getCustomFieldObjectByName("Custom Field Name") // Replace with your custom field name

if (myCustomField) {
    // Set the new value for the custom field on the issue
    issue.setCustomFieldValue(myCustomField, newValue)
    
    // Retrieve the current logged-in user for the update
    def user = ComponentAccessor.getJiraAuthenticationContext().getLoggedInUser()
    
    // Update the issue without dispatching events (adjust EventDispatchOption if needed)
    issueManager.updateIssue(user, issue, com.atlassian.jira.event.type.EventDispatchOption.DO_NOT_DISPATCH, false)
    
    println "Custom field value updated for issue ${issue.key}."
    log.info("Custom field value updated for issue ${issue.key}.")
} else {
    println "Custom field not found."
    log.error("Custom field 'Custom Field Name' not found.")
}

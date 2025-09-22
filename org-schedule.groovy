import com.atlassian.jira.component.ComponentAccessor

def issueManager = ComponentAccessor.issueManager
def customFieldManager = ComponentAccessor.customFieldManager

// Your JQL to find relevant issues
def issues = // ...get issues with your JQL...

issues.each { issue ->
    def startDate = issue.getCustomFieldValue(customFieldManager.getCustomFieldObjectByName("Start Date"))
    def endDate = issue.getCustomFieldValue(customFieldManager.getCustomFieldObjectByName("End Date"))
    def today = new Date().clearTime()

    if (startDate && startDate.equals(today)) {
        // === RUN ADD USER SCRIPT ===
        // (Use your original script: assign users to assets)
    }
    if (endDate && endDate.equals(today)) {
        // === RUN REMOVE USER SCRIPT ===
        // (Use the "remove user" script from earlier)
    }
}

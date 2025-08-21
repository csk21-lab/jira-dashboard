import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.issue.Issue
import com.atlassian.jira.user.ApplicationUser

// Define the issue key for testing
String issueKeyForTesting = "YOURPROJECT-123" // <<-- Replace with the actual issue key you want to test

Class activeDirectoryAttributeManagerClass = ComponentAccessor.getPluginAccessor().getClassLoader().findClass("com.intenso.jira.plugins.admanager.util.ActiveDirectoryAttributeManager")
def activeDirectoryAttributeManager = ComponentAccessor.getOSGiComponentInstanceOfType(activeDirectoryAttributeManagerClass)

def issueManager = ComponentAccessor.getIssueManager() // Get the IssueManager
def issue = issueManager.getIssueObject(issueKeyForTesting) // Get the issue object by its key


if (issue) {
    def reporter = issue.getReporter() as ApplicationUser // Get the reporter of the issue

    if (reporter) {
        def userAttributeMap = activeDirectoryAttributeManager.getUserAttributes(reporter, "myActiveDirectory") // Get attributes for the reporter using the configured AD connection

        def managersName = userAttributeMap.get("manager") // Get the "manager" attribute from the map (assuming this is your AD attribute for manager)

        if (managersName) {
            log.debug "Reporter's Manager for issue ${issue.getKey()}: ${managersName}"
            // You can now use the 'managersName' variable as needed (e.g., set a custom field with it)
        } else {
            log.warn "Could not find manager for reporter ${reporter.getDisplayName()} in AD connection 'myActiveDirectory' for issue ${issue.getKey()}"
        }
    } else {
        log.warn "Issue ${issue.getKey()} has no reporter."
    }
} else {
    log.error "Could not find issue with key: ${issueKeyForTesting}"
}

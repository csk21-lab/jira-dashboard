import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.issue.MutableIssue
import com.atlassian.jira.user.ApplicationUser

// Load required OSGi components
def iqlFacadeClass = ComponentAccessor.getPluginAccessor().getClassLoader().findClass("com.riadalabs.jira.plugins.insight.channel.external.api.facade.IQLFacade")
def iqlFacade = ComponentAccessor.getOSGiComponentInstanceOfType(iqlFacadeClass)
def objectFacade = ComponentAccessor.getOSGiComponentInstanceOfType(
    ComponentAccessor.getPluginAccessor().getClassLoader().findClass("com.riadalabs.jira.plugins.insight.channel.external.api.facade.ObjectFacade")
)

// Configuration
def jiraProjectId = 11403 // Replace with your project ID
def issueTypeId = "17000" // Replace with your issue type ID
def vulCategoryField = ComponentAccessor.customFieldManager.getCustomFieldObjectByName("Vulnerability Details")

// Define applicationName and categoryName
def applicationName = "JIRA" // Replace with actual application name
def categoryName = "7-Zip" // Replace with actual category name

// Fetch matching Insight objects based on applicationName and categoryName
def iqlQuery = """
    "Applications" = "${applicationName}" AND "Category" = "${categoryName}"
"""
def matchingObjects = iqlFacade.findObjects(iqlQuery)

log.warn("Matching objects: ${matchingObjects*.getLabel()}")

if (!matchingObjects) {
    log.warn("No matching objects found for Applications: ${applicationName} and Category: ${categoryName}")
    return
}

// Construct JQL to check if a ticket already exists
def jqlQuery = """
    project = JRAA AND statusCategory not in (Done) 
    AND "Vulnerability Details" in aqlFunction("Applications = ${applicationName} and Category = ${categoryName}")
"""

// Search for existing issues
def searchService = ComponentAccessor.getComponentOfType(com.atlassian.jira.bc.issue.search.SearchService)
def loggedInUser = ComponentAccessor.jiraAuthenticationContext.loggedInUser as ApplicationUser

try {
    // Parse and validate the JQL query
    def parseResult = searchService.parseQuery(loggedInUser, jqlQuery)
    if (!parseResult.isValid()) {
        log.error("Invalid JQL query: ${jqlQuery}")
        return
    }

    // Extract the Query object and perform the search
    def query = parseResult.getQuery()
    def searchResults = searchService.search(loggedInUser, query, com.atlassian.jira.web.bean.PagerFilter.getUnlimitedFilter())

    if (searchResults.getResults()) {
        log.warn("Issue already exists for Applications: ${applicationName}, Category: ${categoryName}")
        return
    }
} catch (Exception e) {
    log.error("Error while checking existing issues: ${e.message}")
    return
}

// Create a new Jira issue (if no existing ticket found)
try {
    def userManager = ComponentAccessor.userManager
    def reporter = userManager.getUserByKey("JIRA_Ticket") ?: loggedInUser
    def project = ComponentAccessor.projectManager.getProjectObj(jiraProjectId)

    MutableIssue issue = ComponentAccessor.issueFactory.issue
    issue.projectObject = project
    issue.summary = "New Vulnerability Issue for Applications: ${applicationName}, Category: ${categoryName}"
    issue.issueTypeId = issueTypeId
    issue.assignee = loggedInUser
    issue.reporter = reporter
    issue.description = """
        Vulnerability details:
        Application: ${applicationName}
        Category: ${categoryName}
    """

    // Set the custom field value
    if (vulCategoryField) {
        issue.setCustomFieldValue(vulCategoryField, "${applicationName}, ${categoryName}")
    }

    // Save the issue
    def createdIssue = ComponentAccessor.issueManager.createIssueObject(loggedInUser, issue)
    log.warn("Issue created successfully: ${createdIssue.key}")
} catch (Exception e) {
    log.error("Error creating issue: ${e.message}")
}

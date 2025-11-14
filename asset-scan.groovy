import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.issue.IssueInputParametersImpl
import com.atlassian.jira.user.ApplicationUsers

// Configuration
def projectKey = "ABCD"                // Your Jira project key
def issueTypeId = "10000"              // Replace with your issue type ID (number, not name)
def reporterUsername = "at"            // Username for reporter
def aqlString = 'objectType = "Sample"'// AQL statement for your Assets search

// Set up reporter
def reporterUser = ComponentAccessor.userManager.getUserByName(reporterUsername)

// This is your convenience abstraction, assumed as a custom Assets class/integration available in your Jira
def tableRows = []
tableRows << "| SCValue | SCLINeValue | STValue |"
tableRows << "| --- | --- | --- |"

def objects = Assets.search(aqlString) // No schemaId needed in your abstraction

objects.each { obj ->
    // Adjust these attribute names as needed!
    def scValue = obj['SCValue'] ?: ""
    def sclValue = obj['SCLINeValue'] ?: ""
    def stValue = obj['STValue'] ?: ""
    tableRows << "| ${scValue} | ${sclValue} | ${stValue} |"
}

def description = tableRows.join("\n")
def project = ComponentAccessor.projectManager.getProjectByCurrentKey(projectKey)

def issueInputParameters = new IssueInputParametersImpl()
    .setProjectId(project.id)
    .setIssueTypeId(issueTypeId)
    .setSummary("Sample Issue with Object Data Table")
    .setDescription(description)
    .setReporterId(reporterUser.key)

def issueService = ComponentAccessor.issueService
def validationResult = issueService.create(ApplicationUsers.toDirectoryUser(reporterUser), issueInputParameters)
if (validationResult.isValid()) {
    def createResult = issueService.create(ApplicationUsers.toDirectoryUser(reporterUser), validationResult)
    if (createResult.isValid()) {
        log.warn("Ticket created: ${createResult.issue.key}")
    }
} else {
    log.error("Issue create failed: ${validationResult.errorCollection}")
}

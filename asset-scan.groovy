import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.issue.IssueInputParametersImpl
import com.atlassian.jira.user.ApplicationUsers
import com.riadalabs.jira.plugins.insight.channel.external.api.facade.ObjectFacade
import com.riadalabs.jira.plugins.insight.channel.external.api.facade.IQLFacade

def objectFacade = ComponentAccessor.getOSGiComponentInstanceOfType(ObjectFacade)
def iqlFacade = ComponentAccessor.getOSGiComponentInstanceOfType(IQLFacade)
def issueService = ComponentAccessor.issueService

// -- PARAMETERS TO EDIT --
def projectKey = "PROJ"
def issueTypeName = "Task" // or your desired issue type
def summary = "AQL Results Table"
def aqlQuery = 'ObjectType = "YourObjectType"'
def attr1 = "Name"
def attr2 = "Owner"
def attr3 = "Status"
def reporterUsername = "at" // Username or key for your target reporter
// -------------------------

// Find Jira user for reporter
def reporterUser = ComponentAccessor.userManager.getUserByName(reporterUsername)

// Run AQL and fetch objects
def objectTypeId = objectFacade.findObjectTypeBeans().find { it.name == "YourObjectType" }?.id
def results = iqlFacade.findObjects(objectTypeId, aqlQuery, 0, Integer.MAX_VALUE)

def tableRows = []
tableRows << "| ${attr1} | ${attr2} | ${attr3} |"
tableRows << "| --- | --- | --- |"
results.each { obj ->
    def n1 = obj.getAttribute(attr1)?.objectAttributeValueBeans?.first()?.value ?: ""
    def n2 = obj.getAttribute(attr2)?.objectAttributeValueBeans?.first()?.value ?: ""
    def n3 = obj.getAttribute(attr3)?.objectAttributeValueBeans?.first()?.value ?: ""
    tableRows << "| ${n1} | ${n2} | ${n3} |"
}
def description = tableRows.join("\n")

def project = ComponentAccessor.projectManager.getProjectByCurrentKey(projectKey)
def issueType = ComponentAccessor.constantsManager.allIssueTypeObjects.find { it.name == issueTypeName }

def issueInputParameters = new IssueInputParametersImpl()
    .setProjectId(project.id)
    .setIssueTypeId(issueType.id)
    .setSummary(summary)
    .setDescription(description)
    .setReporterId(reporterUser.key) // set specific user as reporter

def validationResult = issueService.create(ApplicationUsers.toDirectoryUser(reporterUser), issueInputParameters)
if (validationResult.isValid()) {
    def createResult = issueService.create(ApplicationUsers.toDirectoryUser(reporterUser), validationResult)
    if (createResult.isValid()) {
        log.debug("Issue created: ${createResult.issue.key} with reporter ${reporterUser.name}")
    }
} else {
    log.warn("Issue create failed: ${validationResult.errorCollection}")
}

import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.issue.IssueInputParametersImpl
import com.atlassian.jira.user.ApplicationUsers
import com.riadalabs.jira.plugins.insight.channel.external.api.facade.ObjectFacade
import com.riadalabs.jira.plugins.insight.services.model.ObjectBean

def objectFacade = ComponentAccessor.getOSGiComponentInstanceOfType(ObjectFacade)
def issueService = ComponentAccessor.issueService

// -- PARAMETERS TO EDIT --
def projectKey = "PROJ"
def issueTypeName = "Task"
def summary = "AQL Results Table"
def schemaId = 1 // <-- YOUR schema ID!
def aqlQuery = 'ObjectType = "YourObjectType"' // Your AQL query
def attr1 = "Name"
def attr2 = "Owner"
def attr3 = "Status"
def reporterUsername = "at"
// -------------------------

// Reporter user
def reporterUser = ComponentAccessor.userManager.getUserByName(reporterUsername)

// Get all matching objects using the facade
List<ObjectBean> results = objectFacade.findByIQL(schemaId, aqlQuery)

def tableRows = []
tableRows << "| ${attr1} | ${attr2} | ${attr3} |"
tableRows << "| --- | --- | --- |"

results.each { obj ->
    def n1 = obj.getObjectAttributeBeans().find { it.objectTypeAttribute.name == attr1 }?.objectAttributeValueBeans?.first()?.value ?: ""
    def n2 = obj.getObjectAttributeBeans().find { it.objectTypeAttribute.name == attr2 }?.objectAttributeValueBeans?.first()?.value ?: ""
    def n3 = obj.getObjectAttributeBeans().find { it.objectTypeAttribute.name == attr3 }?.objectAttributeValueBeans?.first()?.value ?: ""
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
    .setReporterId(reporterUser.key)

def validationResult = issueService.create(ApplicationUsers.toDirectoryUser(reporterUser), issueInputParameters)
if (validationResult.isValid()) {
    def createResult = issueService.create(ApplicationUsers.toDirectoryUser(reporterUser), validationResult)
    if (createResult.isValid()) {
        log.debug("Issue created: ${createResult.issue.key} with reporter ${reporterUser.name}")
    }
} else {
    log.warn("Issue create failed: ${validationResult.errorCollection}")
}

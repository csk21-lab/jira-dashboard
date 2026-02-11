import com.atlassian.jira.component.ComponentAccessor
import groovy.json.JsonSlurper

def issueManager = ComponentAccessor.issueManager
def issue = issueManager.getIssueByCurrentKey("ABC-189") // Replace with your test issue key
def customFieldId = "customfield_10000"
def customField = ComponentAccessor.customFieldManager.getCustomFieldObject(customFieldId)
def selectedValuesRaw = issue.getCustomFieldValue(customField)
log.warn("selectedValuesRaw : ${selectedValuesRaw}")
def issueid = issue.id
log.warn("issueid : ${issueid}")

if (selectedValuesRaw) {
    // Parse the JSON string from Elements Connect field
    def json = new JsonSlurper().parseText(selectedValuesRaw.toString())
    def selectedValues = json.keys // This is now a list: ["ABC_TEST", "XYZ_123", "ABCD_REST"]

    def user = ComponentAccessor.jiraAuthenticationContext.loggedInUser
    def issueService = ComponentAccessor.getIssueService()
    def issueLinkManager = ComponentAccessor.getIssueLinkManager()
    def linkTypeName = "relates" // Change to your desired link type name

    // Get the link type object by name
    def linkTypeManager = ComponentAccessor.getComponent(com.atlassian.jira.issue.link.IssueLinkTypeManager)
    def linkType = linkTypeManager.getIssueLinkTypesByName(linkTypeName)?.find { it }

    selectedValues.each { value ->
        def issueInputParameters = issueService.newIssueInputParameters()
        issueInputParameters
            .setProjectId(issue.projectObject.id)
            .setIssueTypeId("10300") // Replace with your sub-task issue type ID
            .setSummary(value.toString())
            .setReporterId(user.name)

        // Pass the parent issue's ID as the second parameter
        def validationResult = issueService.validateSubTaskCreate(user, issue.id, issueInputParameters)
        if (validationResult.isValid()) {
            def result = issueService.create(user, validationResult)
            if (result.isValid() && linkType) {
                def subTaskIssue = result.issue
                // Link the sub-task to the parent with the specified link type
                issueLinkManager.createIssueLink(
                    issue.id, // source (parent)
                    subTaskIssue.id, // destination (sub-task)
                    linkType.id as Long,
                    1L,
                    user
                )
            }
        }
    }
}

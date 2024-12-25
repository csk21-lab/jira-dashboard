import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.issue.IssueManager
import com.atlassian.jira.issue.MutableIssue
import com.atlassian.jira.bc.issue.IssueService
import com.atlassian.jira.issue.customfields.manager.CustomFieldManager
import com.atlassian.jira.issue.customfields.option.Options
import com.atlassian.jira.issue.fields.CustomField
import com.atlassian.jira.issue.fields.config.FieldConfig
import com.atlassian.jira.issue.fields.config.manager.FieldConfigSchemeManager
import com.atlassian.jira.issue.fields.config.manager.OptionsManager
import com.onresolve.scriptrunner.runner.customisers.WithPlugin

@WithPlugin("com.riadalabs.jira.plugins.insight")
import com.riadalabs.jira.plugins.insight.services.model.ObjectAttributeBean
import com.riadalabs.jira.plugins.insight.services.model.ObjectBean
import com.riadalabs.jira.plugins.insight.services.model.ObjectTypeAttributeBean
import com.riadalabs.jira.plugins.insight.services.model.ObjectTypeAttributeBean.AttributeType
import com.riadalabs.jira.plugins.insight.services.IInsightObjectFacade
import com.riadalabs.jira.plugins.insight.services.ObjectTypeAttributeService
import com.riadalabs.jira.plugins.insight.util.InsightObjectAttributeBeanUtil

def issueKey = "PROJECT-123"  // Replace with your issue key
def customFieldId = "customfield_10000"  // Replace with your custom field ID

def issueManager = ComponentAccessor.issueManager
def customFieldManager = ComponentAccessor.customFieldManager
def issueService = ComponentAccessor.issueService
def user = ComponentAccessor.jiraAuthenticationContext.loggedInUser
def insightObjectFacade = ComponentAccessor.getOSGiComponentInstanceOfType(IInsightObjectFacade)
def objectTypeAttributeService = ComponentAccessor.getOSGiComponentInstanceOfType(ObjectTypeAttributeService)

def issue = issueManager.getIssueObject(issueKey)
if (!issue) {
    log.error("Issue not found: $issueKey")
    return
}

def customField = customFieldManager.getCustomFieldObject(customFieldId)
if (!customField) {
    log.error("Custom field not found: $customFieldId")
    return
}

// Get the value of the Assets custom field
def assets = issue.getCustomFieldValue(customField) as List<ObjectBean>
if (!assets) {
    log.error("No assets found in custom field: $customFieldId")
    return
}

// Extract attribute names and values
def headers = [] as Set
def rows = []

assets.each { asset ->
    def row = [:]
    asset.objectAttributeBeans.each { attrib ->
        def attribName = attrib.objectTypeAttribute.name
        def attribValue = InsightObjectAttributeBeanUtil.getObjectAttributeValue(attrib)
        headers << attribName
        row[attribName] = attribValue
    }
    rows << row
}

// Create table headers
def tableHeaders = headers.join(" || ")

// Create table rows
def tableRows = rows.collect { row ->
    headers.collect { header -> row[header] ?: "" }.join(" | ")
}.join("\n| ")

// Create the table in Jira's wiki markup
def table = """
|| $tableHeaders ||
| $tableRows |
"""

def newDescription = (issue.description ?: "") + "\n\n" + table

// Update the issue description
def issueInputParameters = issueService.newIssueInputParameters()
issueInputParameters.setDescription(newDescription)

def updateValidationResult = issueService.validateUpdate(user, issue.id, issueInputParameters)
if (updateValidationResult.isValid()) {
    def updateResult = issueService.update(user, updateValidationResult)
    if (!updateResult.isValid()) {
        log.error("Failed to update issue: $updateResult.errorCollection")
    } else {
        log.info("Issue updated successfully")
    }
} else {
    log.error("Validation failed: $updateValidationResult.errorCollection")
}

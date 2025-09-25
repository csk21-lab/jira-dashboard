import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.issue.Issue
import com.atlassian.jira.user.ApplicationUser
import com.riadalabs.jira.plugins.insight.channel.external.api.facade.ObjectFacade
import com.riadalabs.jira.plugins.insight.channel.external.api.facade.ObjectSchemaFacade

// --- CONFIGURE THESE ---
def computerFieldName = "Computer"
def assignedToAttributeName = "Assigned to"
def computerObjectTypeName = "Computer"
def outputTextFieldName = "Reporter’s Computers"
def schemaName = "yourSchemaName" // <-- set your schema name

// --- SET YOUR ISSUE KEY HERE ---
def issueKey = "PROJ-123" // <-- set your issue key

def issueManager = ComponentAccessor.getIssueManager()
Issue issue = issueManager.getIssueObject(issueKey)
if (!issue) {
    throw new IllegalArgumentException("Issue with key '${issueKey}' not found!")
}

ApplicationUser reporter = issue.getReporter()
if (!reporter) {
    throw new IllegalArgumentException("Issue '${issueKey}' has no reporter!")
}

def objectFacade = ComponentAccessor.getOSGiComponentInstanceOfType(ObjectFacade)
def objectSchemaFacade = ComponentAccessor.getOSGiComponentInstanceOfType(ObjectSchemaFacade)
def customFieldManager = ComponentAccessor.getCustomFieldManager()

// THIS IS THE PROPER WAY:
def schemas = objectSchemaFacade.findAll()
if (!schemas || schemas.isEmpty()) {
    throw new IllegalArgumentException("No Insight/Assets schemas found!")
}
def targetSchema = schemas.find { it.name == schemaName }
if (!targetSchema) {
    throw new IllegalArgumentException("Schema '${schemaName}' not found in Insight/Assets! Available schemas: " +
        schemas.collect{ it.name }.join(', '))
}

def objectTypeBeans = objectFacade.findObjectTypeBeans(targetSchema.id)
def computerObjectTypeBean = objectTypeBeans.find { it.name == computerObjectTypeName }
if (!computerObjectTypeBean) {
    throw new IllegalArgumentException("Object type '${computerObjectTypeName}' not found in schema '${schemaName}'!")
}
def computerObjectTypeId = computerObjectTypeBean.id

def computerAssets = objectFacade.findObjectBeansByObjectType(computerObjectTypeId)

def reporterAssets = computerAssets.findAll { asset ->
    def assignedToAttr = asset.objectAttributeBeans.find { it.objectTypeAttributeBean.name == assignedToAttributeName }
    def assignedToValue = assignedToAttr?.getObjectAttributeValueBeans()?.first()?.value
    assignedToValue?.toString() == reporter?.key
}

def assetListText = reporterAssets.collect { asset ->
    "Name: ${asset.name}, Key: ${asset.objectKey}"
}.join("\n")

if (!assetListText) {
    assetListText = "No computers assigned to reporter."
}

def outputTextField = customFieldManager.getCustomFieldObjectByName(outputTextFieldName)
if (outputTextField) {
    issue.setCustomFieldValue(outputTextField, assetListText)
    return "Custom field '${outputTextFieldName}' updated for issue ${issueKey}.\n\n${assetListText}"
} else {
    throw new IllegalArgumentException("Custom field '${outputTextFieldName}' not found!")
}

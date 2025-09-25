import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.issue.Issue
import com.atlassian.jira.user.ApplicationUser
import com.riadalabs.jira.plugins.insight.channel.external.api.facade.ObjectFacade

// --- CONFIGURE THESE ---
def schemaId = 123 // <-- CHANGE THIS to your schema ID!
def computerObjectTypeName = "Computer" // <-- Ensure this matches your object type name
def assignedToAttributeName = "Assigned to" // <-- Ensure this matches your attribute name
def outputTextFieldName = "Reporter’s Computers" // <-- Ensure this matches your custom field name
def issueKey = "PROJ-123" // <-- CHANGE THIS to your issue key

def issueManager = ComponentAccessor.getIssueManager()
Issue issue = issueManager.getIssueObject(issueKey)
if (!issue) throw new IllegalArgumentException("Issue with key '${issueKey}' not found!")

ApplicationUser reporter = issue.getReporter()
if (!reporter) throw new IllegalArgumentException("Issue '${issueKey}' has no reporter!")

def objectFacade = ComponentAccessor.getOSGiComponentInstanceOfType(ObjectFacade)
def customFieldManager = ComponentAccessor.getCustomFieldManager()

// Get object types for the schema by ID
def objectTypeBeans = objectFacade.findObjectTypeBeans(schemaId)
if (!objectTypeBeans || objectTypeBeans.isEmpty()) {
    throw new IllegalArgumentException("No object types found for schema ID '${schemaId}'!")
}
def computerObjectTypeBean = objectTypeBeans.find { it.name == computerObjectTypeName }
if (!computerObjectTypeBean) {
    throw new IllegalArgumentException("Object type '${computerObjectTypeName}' not found in schema with ID '${schemaId}'! Available types: " + objectTypeBeans.collect{it.name})
}
def computerObjectTypeId = computerObjectTypeBean.id

def computerAssets = objectFacade.findObjectBeansByObjectType(computerObjectTypeId)
if (!computerAssets || computerAssets.isEmpty()) {
    log.warn("No Computer assets found for object type ID ${computerObjectTypeId}")
}

def reporterAssets = computerAssets.findAll { asset ->
    def assignedToAttr = asset.objectAttributeBeans.find { 
        // Use correct getter for attribute name, varies by Insight version
        it.objectTypeAttributeBean?.name == assignedToAttributeName 
    }
    def assignedToValue = assignedToAttr?.objectAttributeValueBeans?.first()?.value
    assignedToValue?.toString() == reporter?.key
}

def assetListText = reporterAssets.collect { asset ->
    "Name: ${asset.name}, Key: ${asset.objectKey}"
}.join("\n")

if (!assetListText) assetListText = "No computers assigned to reporter."

def outputTextField = customFieldManager.getCustomFieldObjectByName(outputTextFieldName)
if (outputTextField) {
    issue.setCustomFieldValue(outputTextField, assetListText)
    return "Custom field '${outputTextFieldName}' updated for issue ${issueKey}.\n\n${assetListText}"
} else {
    throw new IllegalArgumentException("Custom field '${outputTextFieldName}' not found!")
}

import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.issue.Issue
import com.atlassian.jira.user.ApplicationUser
import com.riadalabs.jira.plugins.insight.channel.external.api.facade.ObjectFacade

// --- CONFIGURE THESE ---
def computerFieldName = "Computer" // Asset field on the ticket
def assignedToAttributeName = "Assigned to"
def computerObjectTypeName = "Computer" // Asset schema/object type
def outputTextFieldName = "Reporter’s Computers" // Name of the custom text field for output

// --- SET YOUR ISSUE KEY HERE ---
def issueKey = "PROJ-123" // <--- CHANGE THIS TO YOUR ISSUE KEY

// Get the specific issue for testing
def issueManager = ComponentAccessor.getIssueManager()
Issue issue = issueManager.getIssueObject(issueKey)
if (!issue) {
    throw new IllegalArgumentException("Issue with key '${issueKey}' not found!")
}

// Get reporter
ApplicationUser reporter = issue.getReporter()
if (!reporter) {
    throw new IllegalArgumentException("Issue '${issueKey}' has no reporter!")
}

// Assets/Insight API access
def objectFacade = ComponentAccessor.getOSGiComponentInstanceOfType(ObjectFacade)
def customFieldManager = ComponentAccessor.getCustomFieldManager()

// Get all Computer assets
def computerObjectTypeBeans = objectFacade.findObjectTypeBeansByName(computerObjectTypeName)
if (!computerObjectTypeBeans || computerObjectTypeBeans.isEmpty()) {
    throw new IllegalArgumentException("Object type '${computerObjectTypeName}' not found in Insight/Assets!")
}
def computerObjectTypeId = computerObjectTypeBeans[0].getId()
def computerAssets = objectFacade.findObjectBeansByObjectType(computerObjectTypeId)

// Filter assets assigned to reporter (using key; adjust if you want email/username matching)
def reporterAssets = computerAssets.findAll { asset ->
    def assignedToAttr = asset.objectAttributeBeans.find { it.objectTypeAttributeBean.name == assignedToAttributeName }
    def assignedToValue = assignedToAttr?.getObjectAttributeValueBeans()?.first()?.value
    assignedToValue?.toString() == reporter?.key
}

// Create list text
def assetListText = reporterAssets.collect { asset ->
    "Name: ${asset.name}, Key: ${asset.objectKey}"
}.join("\n")

if (!assetListText) {
    assetListText = "No computers assigned to reporter."
}

// Set the value to custom text field
def outputTextField = customFieldManager.getCustomFieldObjectByName(outputTextFieldName)
if (outputTextField) {
    issue.setCustomFieldValue(outputTextField, assetListText)
    return "Custom field '${outputTextFieldName}' updated for issue ${issueKey}.\n\n${assetListText}"
} else {
    throw new IllegalArgumentException("Custom field '${outputTextFieldName}' not found!")
}

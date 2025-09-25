import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.issue.Issue
import com.atlassian.jira.user.ApplicationUser
import com.riadalabs.jira.plugins.insight.channel.external.api.facade.ObjectFacade

// --- CONFIGURE THESE ---
def computerFieldName = "Computer" // Asset field on the ticket
def assignedToAttributeName = "Assigned to"
def computerObjectTypeName = "Computer" // Asset schema/object type
def outputTextFieldName = "Reporter’s Computers" // Name of the custom text field for output

// Get current issue (for postfunction/automation)
Issue issue = issue

// Get reporter
ApplicationUser reporter = issue.getReporter()

// Insight/Assets API access
def objectFacade = ComponentAccessor.getOSGiComponentInstanceOfType(ObjectFacade)
def customFieldManager = ComponentAccessor.getCustomFieldManager()

// Get all Computer assets
def computerObjectTypeId = objectFacade.findObjectTypeBeansByName(computerObjectTypeName)[0].getId()
def computerAssets = objectFacade.findObjectBeansByObjectType(computerObjectTypeId)

// Filter assets assigned to reporter
def reporterAssets = computerAssets.findAll { asset ->
    def assignedToAttr = asset.objectAttributeBeans.find { it.objectTypeAttributeBean.name == assignedToAttributeName }
    def assignedToValue = assignedToAttr?.getObjectAttributeValueBeans()?.first()?.value
    assignedToValue?.toString() == reporter?.key // Use key/email/username as per config
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
}

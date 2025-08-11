import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.issue.CustomFieldManager

// Get the issue object
def issueManager = ComponentAccessor.issueManager
def issue = issueManager.getIssueObject("ABC-1111")

// Get 'org Location' value from the issue
def customFieldManager = ComponentAccessor.customFieldManager
def orgLocationField = customFieldManager.getCustomFieldObjectByName("org Location")
def orgLocationValue = issue.getCustomFieldValue(orgLocationField)[0].label as String

// Get Computer asset object from issue custom field (e.g., 'Computer' Insight field)
def computerField = customFieldManager.getCustomFieldObject("customfield_16008")
def computerObject = issue.getCustomFieldValue(computerField)[0] // Insight Object Bean

// Get Insight API classes
def pluginAccessor = ComponentAccessor.pluginAccessor
def objectFacadeClass = pluginAccessor.getClassLoader().findClass("com.riadalabs.jira.plugins.insight.channel.external.api.facade.ObjectFacade")
def objectFacade = ComponentAccessor.getOSGiComponentInstanceOfType(objectFacadeClass)
def objectTypeAttributeFacadeClass = pluginAccessor.getClassLoader().findClass("com.riadalabs.jira.plugins.insight.channel.external.api.facade.ObjectTypeAttributeFacade")
def objectTypeAttributeFacade = ComponentAccessor.getOSGiComponentInstanceOfType(objectTypeAttributeFacadeClass)
def objectAttributeBeanFactoryClass = pluginAccessor.getClassLoader().findClass("com.riadalabs.jira.plugins.insight.services.model.factory.ObjectAttributeBeanFactory")
def objectAttributeBeanFactory = ComponentAccessor.getOSGiComponentInstanceOfType(objectAttributeBeanFactoryClass)

// Get the Computer asset's object bean (ID from computerObject)
def computerObjectId = computerObject.id
def computerObjectBean = objectFacade.loadObjectBean(computerObjectId)

// Get the attribute bean for 'Location' (replace 23726 with real attribute ID)
def locationAttributeBean = objectTypeAttributeFacade.loadObjectTypeAttributeBean(23726)

// Create an updated attribute bean with the new location value
def updatedLocationAttributeBean = objectAttributeBeanFactory.createObjectAttributeBeanForObject(computerObjectBean, locationAttributeBean, orgLocationValue)

// Apply the updated attribute and store the object
def updatedAttributes = []
updatedAttributes.add(updatedLocationAttributeBean)
computerObjectBean.setObjectAttributeBeans(updatedAttributes)

try {
    objectFacade.storeObjectBean(computerObjectBean)
    log.warn("Computer asset updated with new Location: ${orgLocationValue}")
} catch (Exception e) {
    log.warn("Failed to update Computer asset: " + e.getMessage())
}

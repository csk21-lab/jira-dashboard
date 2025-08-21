import com.atlassian.jira.component.ComponentAccessor

//== Get the issue object ==
def issueManager = ComponentAccessor.issueManager
def issue = issueManager.getIssueObject("RED-1")

//== Get custom field managers ==
def customFieldManager = ComponentAccessor.customFieldManager

//== Get OLocation asset field and value ==
def orgLocationField = customFieldManager.getCustomFieldObjectByName("OLocation")
def orgLocationObjects = issue.getCustomFieldValue(orgLocationField) // Insight objects list

//== Prepare Insight API access ==
def pluginAccessor = ComponentAccessor.pluginAccessor
def objectFacadeClass = pluginAccessor.getClassLoader().findClass("com.riadalabs.jira.plugins.insight.channel.external.api.facade.ObjectFacade")
def objectFacade = ComponentAccessor.getOSGiComponentInstanceOfType(objectFacadeClass)
def objectTypeAttributeFacadeClass = pluginAccessor.getClassLoader().findClass("com.riadalabs.jira.plugins.insight.channel.external.api.facade.ObjectTypeAttributeFacade")
def objectTypeAttributeFacade = ComponentAccessor.getOSGiComponentInstanceOfType(objectTypeAttributeFacadeClass)
def objectAttributeBeanFactoryClass = pluginAccessor.getClassLoader().findClass("com.riadalabs.jira.plugins.insight.services.model.factory.ObjectAttributeBeanFactory")
def objectAttributeBeanFactory = ComponentAccessor.getOSGiComponentInstanceOfType(objectAttributeBeanFactoryClass)

//== Get Computer asset object from issue custom field ==
def computerField = customFieldManager.getCustomFieldObject("customfield_16008")
def computerObjects = issue.getCustomFieldValue(computerField)
def computerObject = computerObjects ? computerObjects[0] : null

//== Process OLocation asset and extract label and City ==
def orgLocationLabel = null
def cityValue = null

if (orgLocationObjects && orgLocationObjects.size() > 0) {
    def orgLocationObject = orgLocationObjects[0]
    def orgLocationObjectBean = objectFacade.loadObjectBean(orgLocationObject.id)
    orgLocationLabel = orgLocationObjectBean.label

    // Find "City" attribute (replace name or use ID if you know it)
    def cityAttributeBean = orgLocationObjectBean.objectAttributeBeans.find { it.objectTypeAttributeBean.name == "City" }
    if (cityAttributeBean && cityAttributeBean.objectAttributeValueBeans) {
        cityValue = cityAttributeBean.objectAttributeValueBeans[0]?.value
    }
    log.warn("OLocation label: ${orgLocationLabel}")
    log.warn("City attribute value: ${cityValue}")
} else {
    log.warn("No OLocation asset found on issue.")
}

//== Update Computer asset's Location attribute ==
if (computerObject && orgLocationLabel) {
    def computerObjectBean = objectFacade.loadObjectBean(computerObject.id)
    // Replace with your real attribute IDs for Location and City
    def locationAttributeBean = objectTypeAttributeFacade.loadObjectTypeAttributeBean(23726)
    def cityAttributeBean = objectTypeAttributeFacade.loadObjectTypeAttributeBean(23725)

    // Create attribute beans for update
    def updatedLocationAttributeBean = objectAttributeBeanFactory.createObjectAttributeBeanForObject(computerObjectBean, locationAttributeBean, orgLocationLabel)
    def updatedCityAttributeBean = null
    if (cityValue) {
        updatedCityAttributeBean = objectAttributeBeanFactory.createObjectAttributeBeanForObject(computerObjectBean, cityAttributeBean, cityValue)
    }

    // Store updated attributes
    try {
        objectFacade.storeObjectAttributeBean(updatedLocationAttributeBean)
        if (updatedCityAttributeBean) {
            objectFacade.storeObjectAttributeBean(updatedCityAttributeBean)
            log.warn("Computer asset updated with new Location: ${orgLocationLabel} and City: ${cityValue}")
        } else {
            log.warn("Computer asset updated with new Location: ${orgLocationLabel}")
        }
    } catch (Exception e) {
        log.warn("Failed to update Computer asset attribute: " + e.getMessage())
    }
} else {
    log.warn("Computer asset or Location value is missing, update skipped.")
}




/*
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

// ... previous code to get computerObjectBean, locationAttributeBean, orgLocationValue ...

def updatedLocationAttributeBean = objectAttributeBeanFactory.createObjectAttributeBeanForObject(computerObjectBean, locationAttributeBean, orgLocationValue)

// Store the attribute bean (this updates the attribute value)
try {
    objectFacade.storeObjectAttributeBean(updatedLocationAttributeBean)
    log.warn("Computer asset updated with new Location: ${orgLocationValue}")
} catch (Exception e) {
    log.warn("Failed to update Computer asset attribute: " + e.getMessage())
}

*/

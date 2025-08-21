import com.atlassian.jira.component.ComponentAccessor

//=== Get the issue object ===
def issueManager = ComponentAccessor.issueManager
def issue = issueManager.getIssueObject("RED-1")

//=== Get custom field managers ===
def customFieldManager = ComponentAccessor.customFieldManager

//=== Get OLocation asset field and value ===
def orgLocationField = customFieldManager.getCustomFieldObjectByName("OLocation")
def orgLocationObjects = issue.getCustomFieldValue(orgLocationField) // Insight objects list

//=== Prepare Insight API access ===
def pluginAccessor = ComponentAccessor.pluginAccessor
def objectFacadeClass = pluginAccessor.getClassLoader().findClass("com.riadalabs.jira.plugins.insight.channel.external.api.facade.ObjectFacade")
def objectFacade = ComponentAccessor.getOSGiComponentInstanceOfType(objectFacadeClass)
def objectTypeAttributeFacadeClass = pluginAccessor.getClassLoader().findClass("com.riadalabs.jira.plugins.insight.channel.external.api.facade.ObjectTypeAttributeFacade")
def objectTypeAttributeFacade = ComponentAccessor.getOSGiComponentInstanceOfType(objectTypeAttributeFacadeClass)
def objectAttributeBeanFactoryClass = pluginAccessor.getClassLoader().findClass("com.riadalabs.jira.plugins.insight.services.model.factory.ObjectAttributeBeanFactory")
def objectAttributeBeanFactory = ComponentAccessor.getOSGiComponentInstanceOfType(objectAttributeBeanFactoryClass)

//=== Get Computer asset object from issue custom field ===
def computerField = customFieldManager.getCustomFieldObject("customfield_16008")
def computerObjects = issue.getCustomFieldValue(computerField)
def computerObject = computerObjects ? computerObjects[0] : null

//=== Process OLocation asset and extract label and City ===
def orgLocationLabel = null
def cityValue = null

if (orgLocationObjects && orgLocationObjects.size() > 0) {
    def orgLocationObject = orgLocationObjects[0]
    def orgLocationObjectBean = objectFacade.loadObjectBean(orgLocationObject.id)
    orgLocationLabel = orgLocationObjectBean.label

    // Get all attribute beans for OLocation object
    def attributeBeans = orgLocationObjectBean.getObjectAttributeBeans()

    // Find the "City" attribute (by name)
    def cityAttributeBean = attributeBeans.find {
        def objectTypeAttributeId = it.getObjectTypeAttributeId()
        def objectTypeAttributeBean = objectTypeAttributeFacade.loadObjectTypeAttributeBean(objectTypeAttributeId)
        objectTypeAttributeBean.getName() == "City"
    }
    cityValue = cityAttributeBean?.getObjectAttributeValueBeans()?.getAt(0)?.getValue()

    log.warn("OLocation label: ${orgLocationLabel}")
    log.warn("City attribute value: ${cityValue}")
} else {
    log.warn("No OLocation asset found on issue.")
}

//=== Update Computer asset's Location and City attributes ===
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

import com.atlassian.jira.component.ComponentAccessor

//=== Get the issue object ===
def issueManager = ComponentAccessor.issueManager
def issue = issueManager.getIssueObject("ABC-123")
//def issue = issue

//=== Get custom field managers ===
def customFieldManager = ComponentAccessor.customFieldManager

//=== Get OLocation asset field and value ===
def orgLocationField = customFieldManager.getCustomFieldObjectByName("Adobe Location")

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
def streetValue = null
def stateValue = null
def departmentValue = null
def officeValue = null

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

    
    // Find the "Street" attribute
    def streetAttributeBean = attributeBeans.find {
        def objectTypeAttributeId = it.getObjectTypeAttributeId()
        def objectTypeAttributeBean = objectTypeAttributeFacade.loadObjectTypeAttributeBean(objectTypeAttributeId)
        objectTypeAttributeBean.getName() == "Street"
    }
    streetValue = streetAttributeBean?.getObjectAttributeValueBeans()?.getAt(0)?.getValue()

    // Find the "State" attribute
    def stateAttributeBean = attributeBeans.find {
        def objectTypeAttributeId = it.getObjectTypeAttributeId()
        def objectTypeAttributeBean = objectTypeAttributeFacade.loadObjectTypeAttributeBean(objectTypeAttributeId)
        objectTypeAttributeBean.getName() == "State"
    }
    stateValue = stateAttributeBean?.getObjectAttributeValueBeans()?.getAt(0)?.getValue()
    
    log.warn("OLocation label: ${orgLocationLabel}")
    log.warn("City attribute value: ${cityValue}")
    log.warn("street attribute value: ${streetValue}")
    log.warn("state attribute value: ${stateValue}")


} else {
    log.warn("No OLocation asset found on issue.")
}


//Reporter Details 
    def reporterDetailsField = customFieldManager.getCustomFieldObjectByName("Reporter Details")


    // Get the value of the Reporter Details field
    def reporterDetailsValue = issue.getCustomFieldValue(reporterDetailsField)

   // if (reporterDetailsValue) {
        // Print the entire reporterDetailsValue structure for debugging
        println "Reporter Details Value: ${reporterDetailsValue}"

        // Assuming the structure is as follows:
        // userAttr is nested within valueMap, which is indexed by some key (like 1)
        def valueMap = reporterDetailsValue.valueMap
        if (valueMap) {
            valueMap.each { key, value ->
                println "Key: ${key}, Value: ${value}"
                // Check if userAttr exists and print it
                def userAttr = value.userAttr
                if (userAttr) {
                    println "User Attributes: ${userAttr}"
                    officeValue = userAttr.get("Office") // Access the "Office" attribute
                    log.warn("Office: ${officeValue}")
                    def jobRoleValue = userAttr.get("Job title") // Access the "Office" attribute
                    log.warn("Job title: ${jobRoleValue}")
                    departmentValue = userAttr.get("Department") // Access the "Office" attribute
                    log.warn("department: ${departmentValue}")
                } else {
                    println "No userAttr found for key: ${key}"
                }
            }
        } else {
            println "No valueMap found in Reporter Details."
        } 
   // }// else {
      //  println "No Reporter Details found for this issue."
   // }

//=== Update Computer asset's Location and City attributes ===
if (computerObject && orgLocationLabel) {
    def computerObjectBean = objectFacade.loadObjectBean(computerObject.id)
    // Replace with your real attribute IDs for Location and City
    def locationAttributeBean = objectTypeAttributeFacade.loadObjectTypeAttributeBean(23726)
    def cityAttributeBean = objectTypeAttributeFacade.loadObjectTypeAttributeBean(23725)
    def streetAttributeBean = objectTypeAttributeFacade.loadObjectTypeAttributeBean(24127)
    def stateAttributeBean = objectTypeAttributeFacade.loadObjectTypeAttributeBean(24128)
    def departmentAttributeBean = objectTypeAttributeFacade.loadObjectTypeAttributeBean(23723)
    def officeAttributeBean = objectTypeAttributeFacade.loadObjectTypeAttributeBean(23722)
 



    // Create attribute beans for update
    def updatedLocationAttributeBean = objectAttributeBeanFactory.createObjectAttributeBeanForObject(computerObjectBean, locationAttributeBean, orgLocationLabel)
    def updatedCityAttributeBean = null
    if (cityValue) {
        updatedCityAttributeBean = objectAttributeBeanFactory.createObjectAttributeBeanForObject(computerObjectBean, cityAttributeBean, cityValue)
    }
    def updatedStreetAttributeBean = objectAttributeBeanFactory.createObjectAttributeBeanForObject(computerObjectBean, streetAttributeBean, streetValue)
    def updatedStateAttributeBean = objectAttributeBeanFactory.createObjectAttributeBeanForObject(computerObjectBean, stateAttributeBean, stateValue)
    def updatedDepartmentAttributeBean = objectAttributeBeanFactory.createObjectAttributeBeanForObject(computerObjectBean, departmentAttributeBean, departmentValue)
    def updatedOfficeAttributeBean = objectAttributeBeanFactory.createObjectAttributeBeanForObject(computerObjectBean, officeAttributeBean, officeValue)





    // Store updated attributes
    try {
        objectFacade.storeObjectAttributeBean(updatedLocationAttributeBean)
        objectFacade.storeObjectAttributeBean(updatedStreetAttributeBean)
        objectFacade.storeObjectAttributeBean(updatedStateAttributeBean)
        objectFacade.storeObjectAttributeBean(updatedDepartmentAttributeBean)
        objectFacade.storeObjectAttributeBean(updatedOfficeAttributeBean)


        if (updatedCityAttributeBean) {
            objectFacade.storeObjectAttributeBean(updatedCityAttributeBean)
            log.warn("Computer asset updated with new Location: ${orgLocationLabel} - City: ${cityValue} - Street : ${streetValue} - State : ${stateValue} - Department : ${departmentValue} - Office : ${officeValue}")
        } else {
            log.warn("Computer asset updated with new Location: ${orgLocationLabel}")
        }
    } catch (Exception e) {
        log.warn("Failed to update Computer asset attribute: " + e.getMessage())
    }
} else {
    log.warn("Computer asset or Location value is missing, update skipped.")
}

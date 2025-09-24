import com.atlassian.jira.component.ComponentAccessor

//=== Get the issue object ===
def issueManager = ComponentAccessor.issueManager
def issue = issueManager.getIssueObject("ABC-123") // Change to your issue key or pass dynamically

//=== Get custom field managers ===
def customFieldManager = ComponentAccessor.customFieldManager

//=== Asset fields, attribute names, and corresponding User Picker fields ===
def assetFieldNames = [
    "Computer Asset", 
    "Mobile Asset", 
    "License Asset"
]
def userPickerFieldNames = [
    "User Picker 1",
    "User Picker 2",
    "User Picker 3"
]
def attributeNames = [
    "Owner",
    "Responsible User",
    "Assigned To"
]

//=== Prepare Insight API access ===
def pluginAccessor = ComponentAccessor.pluginAccessor
def objectFacadeClass = pluginAccessor.getClassLoader().findClass("com.riadalabs.jira.plugins.insight.channel.external.api.facade.ObjectFacade")
def objectFacade = ComponentAccessor.getOSGiComponentInstanceOfType(objectFacadeClass)
def objectTypeAttributeFacadeClass = pluginAccessor.getClassLoader().findClass("com.riadalabs.jira.plugins.insight.channel.external.api.facade.ObjectTypeAttributeFacade")
def objectTypeAttributeFacade = ComponentAccessor.getOSGiComponentInstanceOfType(objectTypeAttributeFacadeClass)
def objectAttributeBeanFactoryClass = pluginAccessor.getClassLoader().findClass("com.riadalabs.jira.plugins.insight.services.model.factory.ObjectAttributeBeanFactory")
def objectAttributeBeanFactory = ComponentAccessor.getOSGiComponentInstanceOfType(objectAttributeBeanFactoryClass)

//=== Main logic: Only update attributes for non-empty user picker fields ===
assetFieldNames.eachWithIndex { assetFieldName, idx ->
    def assetField = customFieldManager.getCustomFieldObjectByName(assetFieldName)
    def userPickerField = customFieldManager.getCustomFieldObjectByName(userPickerFieldNames[idx])
    def attributeName = attributeNames[idx]

    def assetObjects = issue.getCustomFieldValue(assetField)
    def assetObject = (assetObjects instanceof List) ? (assetObjects ? assetObjects[0] : null) : assetObjects

    def userValue = issue.getCustomFieldValue(userPickerField)
    // Support both single and multi-user picker fields
    def userKeys = []
    if (userValue instanceof com.atlassian.jira.user.ApplicationUser) {
        userKeys << userValue.key
    } else if (userValue instanceof List && userValue && userValue[0] instanceof com.atlassian.jira.user.ApplicationUser) {
        userKeys.addAll(userValue.collect { it.key })
    }

    // Only proceed if userKeys is not empty and assetObject is present
    if (assetObject && userKeys && userKeys.size() > 0) {
        def assetObjectBean = objectFacade.loadObjectBean(assetObject.id)
        // Find the attribute by name
        def attributeBeans = assetObjectBean.getObjectAttributeBeans()
        def attrBean = attributeBeans.find { attr ->
            def objectTypeAttributeId = attr.getObjectTypeAttributeId()
            def objectTypeAttributeBean = objectTypeAttributeFacade.loadObjectTypeAttributeBean(objectTypeAttributeId)
            objectTypeAttributeBean.getName() == attributeName
        }
        if (attrBean) {
            def objectTypeAttributeBean = objectTypeAttributeFacade.loadObjectTypeAttributeBean(attrBean.getObjectTypeAttributeId())
            // Remove attribute if any of the current values match any of the user keys
            def currentValues = attrBean.getObjectAttributeValueBeans()?.collect { it.value }
            def removableUserKeys = currentValues.intersect(userKeys)
            if (removableUserKeys && removableUserKeys.size() > 0) {
                try {
                    objectFacade.removeObjectAttributeBean(attrBean)
                    log.warn("Removed ${attributeName} for ${assetFieldName} (was user(s) from ${userPickerFieldNames[idx]}: ${removableUserKeys.join(',')})")
                } catch (Exception e) {
                    log.warn("Failed to remove ${attributeName} for ${assetFieldName}: " + e.getMessage())
                }
            } else {
                log.warn("Attribute ${attributeName} for ${assetFieldName} does not match any user from ${userPickerFieldNames[idx]}: ${userKeys.join(',')}, not removed")
            }
        } else {
            log.warn("No attribute named '${attributeName}' found on asset object ${assetFieldName}")
        }
    } else {
        log.warn("Asset field ${assetFieldName} or user picker field ${userPickerFieldNames[idx]} value is missing or not set; skipping.")
    }
}

// ... rest of your script if any ...

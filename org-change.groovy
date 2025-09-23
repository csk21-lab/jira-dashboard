import com.atlassian.jira.component.ComponentAccessor

//=== Get the issue object ===
def issueManager = ComponentAccessor.issueManager
def issue = issueManager.getIssueObject("ABC-123")
//def issue = issue

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

// Collect all user picker values and their corresponding info
def userPickerInfos = userPickerFieldNames.collectWithIndex { pickerName, idx ->
    def userPickerField = customFieldManager.getCustomFieldObjectByName(pickerName)
    def userValue = issue.getCustomFieldValue(userPickerField)
    def userKey = null
    if (userValue instanceof com.atlassian.jira.user.ApplicationUser) {
        userKey = userValue.key
    } else if (userValue instanceof List && userValue && userValue[0] instanceof com.atlassian.jira.user.ApplicationUser) {
        userKey = userValue[0].key
    }
    [
        idx: idx,
        pickerName: pickerName,
        userKey: userKey
    ]
}

// Only update attributes for fields that have values
userPickerInfos.each { info ->
    if (info.userKey) {
        def assetFieldName = assetFieldNames[info.idx]
        def attributeName = attributeNames[info.idx]

        def assetField = customFieldManager.getCustomFieldObjectByName(assetFieldName)
        def assetObjects = issue.getCustomFieldValue(assetField)
        def assetObject = (assetObjects instanceof List) ? (assetObjects ? assetObjects[0] : null) : assetObjects

        if (assetObject) {
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
                def updatedAttrBean = objectAttributeBeanFactory.createObjectAttributeBeanForObject(assetObjectBean, objectTypeAttributeBean, info.userKey)
                try {
                    objectFacade.storeObjectAttributeBean(updatedAttrBean)
                    log.warn("Updated ${attributeName} for ${assetFieldName} with user from ${info.pickerName}: ${info.userKey}")
                } catch (Exception e) {
                    log.warn("Failed to update ${attributeName} for ${assetFieldName}: " + e.getMessage())
                }
            } else {
                log.warn("No attribute named '${attributeName}' found on asset object ${assetFieldName}")
            }
        } else {
            log.warn("Asset field ${assetFieldName} value is missing")
        }
    }
}

// ... rest of your original script ...

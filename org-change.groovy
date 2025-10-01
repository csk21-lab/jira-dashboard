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

// Correct Groovy way to collect user picker values and their corresponding info
def userPickerInfos = userPickerFieldNames.withIndex().collect { pickerName, idx ->
    def userPickerField = customFieldManager.getCustomFieldObjectByName(pickerName)
    def userValue = issue.getCustomFieldValue(userPickerField)
    def userKeys = []
    if (userValue instanceof com.atlassian.jira.user.ApplicationUser) {
        userKeys << userValue.key
    } else if (userValue instanceof List && userValue && userValue[0] instanceof com.atlassian.jira.user.ApplicationUser) {
        userKeys.addAll(userValue.collect { it.key })
    }
    [
        idx: idx,
        pickerName: pickerName,
        userKeys: userKeys
    ]
}

// Only update attributes for fields that have values
userPickerInfos.each { info ->
    if (info.userKeys && info.userKeys.size() > 0) {
        def assetFieldName = assetFieldNames[info.idx]
        def attributeName = attributeNames[info.idx]
        def assetField = customFieldManager.getCustomFieldObjectByName(assetFieldName)
        def assetObjects = issue.getCustomFieldValue(assetField)
        def assetObject = (assetObjects instanceof List) ? (assetObjects ? assetObjects[0] : null) : assetObjects

        if (assetObject) {
            // --- HAPI-style update ---
            assetObject.update {
                setAttribute(attributeName) {
                    add(*info.userKeys)
                }
            }
            log.warn("HAPI: Appended user keys to ${attributeName} for ${assetFieldName} from ${info.pickerName}: ${info.userKeys}")
        } else {
            log.warn("Asset field ${assetFieldName} value is missing")
        }
    }
}

// ... rest of your original script ...

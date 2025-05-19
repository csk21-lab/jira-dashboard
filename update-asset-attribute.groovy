import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.issue.Issue
import com.atlassian.jira.issue.customfields.manager.CustomFieldManager
import com.atlassian.jira.issue.customfields.option.Option
import com.onresolve.scriptrunner.runner.customisers.PluginModule
import com.riadalabs.jira.plugins.insight.services.model.ObjectBean
import com.riadalabs.jira.plugins.insight.services.model.ObjectFieldBean
import com.riadalabs.jira.plugins.insight.services.InsightService

// Get necessary components
def issue = event.issue // The issue object (available in listener or post-function)
def customFieldManager = ComponentAccessor.getCustomFieldManager()
def insightService = ComponentAccessor.getOSGiComponentInstanceOfType(InsightService)

def customField = customFieldManager.getCustomFieldObjectByName("Your Custom Field Name") // Replace with actual custom field name
def customFieldValue = issue.getCustomFieldValue(customField)

// Assuming custom field holds the Issue Key, find the related asset
if (customFieldValue) {
    // Search for the asset object based on the custom field value (issue key or another identifier)
    def objectBeanList = insightService.getObjectsByKey(customFieldValue.toString()) // Change based on how you search in Insight
    
    if (objectBeanList) {
        def assetObject = objectBeanList[0] // Get the first object found (assuming single object)
        
        // Set a custom field value on the asset object (modify attribute)
        def objectFieldBean = assetObject.getObjectFieldByName("Your Attribute Name") // Replace with actual attribute name
        if (objectFieldBean) {
            // Update the asset object attribute with the issue key
            objectFieldBean.setValue(issue.key) // You can set issue key or any other value you need
            insightService.updateObjectField(assetObject.getId(), objectFieldBean)
            log.info("Asset object updated with issue key: ${issue.key}")
        }
    }
}

import com.atlassian.jira.component.ComponentAccessor
import com.riadalabs.jira.plugins.insight.services.model.ObjectBean

def issueKey = "ABC-124"
def customFieldId = "customfield_00100"  // Replace with your actual Insight field ID

def issueManager = ComponentAccessor.issueManager
def customFieldManager = ComponentAccessor.customFieldManager

def issue = issueManager.getIssueObject(issueKey)
def assetField = customFieldManager.getCustomFieldObject(customFieldId)

def selectedAssets = issue.getCustomFieldValue(assetField)

int totalCount = 0

if (selectedAssets instanceof List) {
    totalCount = selectedAssets.size()
} else if (selectedAssets instanceof ObjectBean) {
    totalCount = 1  // Single selected asset
} else {
    totalCount = 0  // Field is empty
}

log.warn("Total number of asset objects in field '${assetField.name}': ${totalCount}")

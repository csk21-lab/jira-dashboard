import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.issue.MutableIssue
import com.atlassian.jira.issue.IssueManager
import com.atlassian.jira.issue.fields.CustomField
import com.riadalabs.jira.plugins.insight.channel.external.api.facade.ObjectFacade
import org.apache.log4j.Logger

def log = Logger.getLogger("com.acme.SetAssetFieldSpecificIssue")

// --- Configuration ---
String targetIssueKey = "ABC-212"
String assetsCustomFieldName = "ON"           // Your Assets custom field name
String assetObjectKey = "XYZ-912536"          // Your Insight Object Key
Long objectTypeId = 101                       // <-- REPLACE with your Insight Object Type ID

def issueManager = ComponentAccessor.getIssueManager()
def customFieldManager = ComponentAccessor.getCustomFieldManager()
def objectFacade = ComponentAccessor.getOSGiComponentInstanceOfType(ObjectFacade)

// Fetch the MutableIssue object from the issue key
def issue = issueManager.getIssueByCurrentKey(targetIssueKey)
def assetsCustomField = customFieldManager.getCustomFieldObjectByName(assetsCustomFieldName)

// Load the ObjectBean by key
def assetObjectBean = objectFacade.loadObjectBeanByKey(objectTypeId, assetObjectKey)

// Set the custom field value if everything is found
if (issue && assetsCustomField && assetObjectBean) {
    issue.setCustomFieldValue(assetsCustomField, assetObjectBean) // For single-value field
    // For multi-value field:
    // issue.setCustomFieldValue(assetsCustomField, [assetObjectBean])
    log.warn("Set asset '${assetObjectBean.name}' on issue '${issue.key}'")
} else {
    log.warn("FAILED: issue=${issue!=null}, field=${assetsCustomField!=null}, bean=${assetObjectBean!=null}")
}




/*
import com.atlassian.jira.component.ComponentAccessor
import com.riadalabs.jira.plugins.insight.channel.external.api.facade.ObjectFacade

def customFieldManager = ComponentAccessor.getCustomFieldManager()
def objectFacade = ComponentAccessor.getOSGiComponentInstanceOfType(ObjectFacade)

// Update these values for your assets field and object key
def assetsCustomFieldName = "My asset field"
def assetObjectKey = "XXX-123"

def assetsCustomField = customFieldManager.getCustomFieldObjectByName(assetsCustomFieldName)
def assetObjectBean = objectFacade.loadObjectBean(assetObjectKey)

// For single-value fields, use assetObjectBean. For multi-value fields, use a list: [assetObjectBean]
if (assetsCustomField && assetObjectBean) {
    issue.setCustomFieldValue(assetsCustomField, [assetObjectBean])
}

*/

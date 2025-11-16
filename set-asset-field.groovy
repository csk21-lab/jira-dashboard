import com.atlassian.jira.component.ComponentAccessor
import com.riadalabs.jira.plugins.insight.channel.external.api.facade.IQLFacade
import com.atlassian.jira.issue.MutableIssue
import com.atlassian.jira.issue.fields.CustomField
import org.apache.log4j.Logger

def log = Logger.getLogger("com.acme.SetAssetFieldSpecificIssue")

String targetIssueKey = "ABC-212"
String assetsCustomFieldName = "ON"  
String assetObjectKey = "ABC-912536"
String objectTypeName = "YourObjectTypeName" // <-- Replace with your actual type name

def issueManager = ComponentAccessor.getIssueManager()
def customFieldManager = ComponentAccessor.getCustomFieldManager()
def iqlFacade = ComponentAccessor.getOSGiComponentInstanceOfType(IQLFacade)

def issue = issueManager.getIssueByCurrentKey(targetIssueKey)
def assetsCustomField = customFieldManager.getCustomFieldObjectByName(assetsCustomFieldName)
def iql = 'objectType = "' + objectTypeName + '" AND Key = "' + assetObjectKey + '"'

def objectBeans = iqlFacade.findObjectsByIQL(iql)
def assetObjectBean = objectBeans ? objectBeans[0] : null

if (issue && assetsCustomField && assetObjectBean) {
    issue.setCustomFieldValue(assetsCustomField, assetObjectBean) // [assetObjectBean] for multi-value fields
    log.warn("Set asset '${assetObjectBean.name}' on issue '${issue.key}'")
} else {
    log.warn("FAILED: issue=${issue != null}, field=${assetsCustomField != null}, bean=${assetObjectBean != null}, IQL='$iql'")
}

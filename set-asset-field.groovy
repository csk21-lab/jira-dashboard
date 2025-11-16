import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.issue.MutableIssue
import com.riadalabs.jira.plugins.insight.channel.external.api.facade.IQLFacade
import com.atlassian.jira.issue.fields.CustomField
import org.apache.log4j.Logger

def log = Logger.getLogger("com.acme.SetAssetFieldSpecificIssue")

String targetIssueKey = "ABC-212"
String assetsCustomFieldName = "ON"  
String assetObjectKey = "ABC-912536"
Long objectTypeId = 103

def issueManager = ComponentAccessor.getIssueManager()
def customFieldManager = ComponentAccessor.getCustomFieldManager()
def iqlFacade = ComponentAccessor.getOSGiComponentInstanceOfType(IQLFacade)

def issue = issueManager.getIssueByCurrentKey(targetIssueKey)
def assetsCustomField = customFieldManager.getCustomFieldObjectByName(assetsCustomFieldName)
def objectBeans = iqlFacade.findObjectsByIQL(objectTypeId, "Key = \"$assetObjectKey\"")
def assetObjectBean = objectBeans ? objectBeans[0] : null

if (issue && assetsCustomField && assetObjectBean) {
    issue.setCustomFieldValue(assetsCustomField, assetObjectBean) // For multi-value, use [assetObjectBean]
    log.warn("Set asset '${assetObjectBean.name}' on issue '${issue.key}'")
} else {
    log.warn("FAILED: issue=${issue!=null}, field=${assetsCustomField!=null}, bean=${assetObjectBean!=null}")
}

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

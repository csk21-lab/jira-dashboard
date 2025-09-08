import com.atlassian.jira.component.ComponentAccessor
import com.riadalabs.jira.plugins.insight.channel.external.api.facade.ObjectFacade
import com.riadalabs.jira.plugins.insight.services.model.ObjectBean

// Set your parameters here
def objectKey = "ASSET-123"            // The key of your asset object
def attributeName = "Serial Number"    // The attribute name you want to fetch
def objectTypeAttributeId = null       // Set to a specific ID if you know it, else leave as null

// Get the ObjectFacade service
def objectFacade = ComponentAccessor.getOSGiComponentInstanceOfType(ObjectFacade)

// Load the asset object by key
ObjectBean assetObject = objectFacade.loadObjectBean(objectKey)
if (!assetObject) {
    log.warn("No asset found for key: $objectKey")
    return null
}

def attributeBean = null

if (objectTypeAttributeId) {
    // Preferred: Find the attribute by ID
    attributeBean = assetObject.objectAttributeBeans.find { attr ->
        attr.objectTypeAttributeId == objectTypeAttributeId
    }
} else {
    // Fallback: Find the attribute by name (less robust)
    attributeBean = assetObject.objectAttributeBeans.find { attr ->
        attr.objectTypeAttribute?.name == attributeName
    }
    if (attributeBean) {
        objectTypeAttributeId = attributeBean.objectTypeAttributeId
    }
}

if (!attributeBean) {
    log.warn("Attribute '${objectTypeAttributeId ?: attributeName}' not found on object $objectKey")
    return null
}

// Get the value(s) of the attribute
def values = attributeBean.objectAttributeValueBeans*.value

// If attribute is single-valued, just return the first (or null)
def value = values.size() == 1 ? values[0] : values

log.info("Value of '${objectTypeAttributeId ?: attributeName}' on $objectKey: $value")
return value

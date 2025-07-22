import com.atlassian.jira.component.ComponentAccessor

def objectFacade = ComponentAccessor.getOSGiComponentInstanceOfType(
    ComponentAccessor.pluginAccessor.classLoader.findClass("com.riadalabs.jira.plugins.insight.channel.external.api.facade.ObjectFacade")
)
def objectTypeAttributeFacade = ComponentAccessor.getOSGiComponentInstanceOfType(
    ComponentAccessor.pluginAccessor.classLoader.findClass("com.riadalabs.jira.plugins.insight.channel.external.api.facade.ObjectTypeAttributeFacade")
)
def objectAttributeBeanFactory = ComponentAccessor.getOSGiComponentInstanceOfType(
    ComponentAccessor.pluginAccessor.classLoader.findClass("com.riadalabs.jira.plugins.insight.services.model.factory.ObjectAttributeBeanFactory")
)

// Load the Insight object (replace with actual object ID)
def objectBean = objectFacade.loadObjectBean(12345) // 👈 Replace with actual object ID

// Source attribute IDs
int attrId1 = 2001  // e.g., CVSS BaseScore
int attrId2 = 2002  // e.g., Impact Score

// Target attribute ID
int targetAttrId = 2003  // e.g., Total Score

// Utility to get integer value from attribute
def getIntValue = { attrId ->
    def attrBean = objectFacade.loadObjectAttributeBean(objectBean.id, attrId)
    def val = attrBean?.objectAttributeValueBeans?.first()?.value
    return val ? val.toString().toInteger() : 0
}

def value1 = getIntValue(attrId1)
def value2 = getIntValue(attrId2)
def sum = value1 + value2
log.warn("Adding $value1 + $value2 = $sum")

// Prepare target attribute bean
def targetAttrType = objectTypeAttributeFacade.loadObjectTypeAttributeBean(targetAttrId).createMutable()
def newAttrBean = objectAttributeBeanFactory.createObjectAttributeBeanForObject(objectBean, targetAttrType, sum.toString())

// Reuse existing attribute ID if present
def existingAttr = objectFacade.loadObjectAttributeBean(objectBean.id, targetAttrId)
if (existingAttr) {
    newAttrBean.setId(existingAttr.id)
}

try {
    objectFacade.storeObjectAttributeBean(newAttrBean)
    log.warn("Updated attribute ID $targetAttrId with value: $sum")
} catch (Exception e) {
    log.error("Failed to update attribute: ${e.message}")
}

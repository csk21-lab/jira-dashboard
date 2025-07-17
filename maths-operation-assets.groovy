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

// IDs of attributes to sum
int attr1Id = 1001
int attr2Id = 1002
int attr3Id = 1003
int attr4Id = 1004

// Target attribute to store result
int resultAttrId = 1005

// Replace with the actual object being updated
def object = <YOUR_OBJECT_BEAN>  // e.g., from insight custom field, or context

// Load source attribute values
def attr1 = objectFacade.loadObjectAttributeBean(object.id, attr1Id)
def attr2 = objectFacade.loadObjectAttributeBean(object.id, attr2Id)
def attr3 = objectFacade.loadObjectAttributeBean(object.id, attr3Id)
def attr4 = objectFacade.loadObjectAttributeBean(object.id, attr4Id)

if ([attr1, attr2, attr3, attr4].any { it == null }) {
    log.warn("One or more source attributes are missing.")
    return
}

def val1 = attr1.objectAttributeValueBeans[0]?.value as Integer ?: 0
def val2 = attr2.objectAttributeValueBeans[0]?.value as Integer ?: 0
def val3 = attr3.objectAttributeValueBeans[0]?.value as Integer ?: 0
def val4 = attr4.objectAttributeValueBeans[0]?.value as Integer ?: 0

def result = val1 + val2 + val3 + val4

// Load target attribute bean definition
def resultAttrBean = objectTypeAttributeFacade.loadObjectTypeAttributeBean(resultAttrId).createMutable()

// Create new attribute value bean
def newAttrBean = objectAttributeBeanFactory.createObjectAttributeBeanForObject(object, resultAttrBean, result.toString())

// Check if attribute already exists to update instead of create
def existingAttr = objectFacade.loadObjectAttributeBean(object.id, resultAttrId)
if (existingAttr) {
    newAttrBean.setId(existingAttr.id)
}

// Store the updated attribute
try {
    objectFacade.storeObjectAttributeBean(newAttrBean)
    log.warn("Updated attribute ID $resultAttrId with value: $result")
} catch (Exception e) {
    log.warn("Failed to update attribute: " + e.message)
}

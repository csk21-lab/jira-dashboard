import com.atlassian.jira.component.ComponentAccessor
import com.riadalabs.jira.plugins.insight.channel.external.api.facade.ObjectFacade
import com.riadalabs.jira.plugins.insight.services.model.ObjectBean
import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.user.ApplicationUser
// Set your parameters here
def objectKey = "abc-123"//"PSI-1887884"            // The key of your asset object
def attributeName = "Team"    // The attribute name you want to fetch
def objectTypeAttributeId = 121       // Set to a specific ID if you know it, else leave as null
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
//return value

def usernames = value
def outputString = usernames.join(", ") // Joins elements with ", "

//if (value){
  // The user key you want to resolve
String userKey = outputString

// Get the UserManager component
def userManager = ComponentAccessor.userManager

// Get the user object using the user key
ApplicationUser user = userManager.getUserByKey(userKey)

// Check if a user was found before trying to get properties
if (user) {
    // Get the username (internal name)
    String username = user.name

    // Get the display name (full name)
    String displayName = user.displayName

    // Log or return the results
    log.info("Found user with key: ${userKey}")
    log.info("Username: ${username}")
    log.info("Display Name: ${displayName}")
    
    // Example: Return the username for a scripted field
    return username
} else {
    log.warn("Could not find a user with the key: ${userKey}")
    return "User not found"
}

//}

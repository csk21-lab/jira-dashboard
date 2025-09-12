import com.atlassian.jira.component.ComponentAccessor
import com.riadalabs.jira.plugins.insight.channel.external.api.facade.ObjectFacade
import com.riadalabs.jira.plugins.insight.services.model.ObjectBean
import com.atlassian.jira.user.ApplicationUser

// Set your parameters here
def objectKey = "abc-123" // The key of your asset object
def attributeName = "Team" // The attribute name you want to fetch
def objectTypeAttributeId = 121 // Set to a specific ID if you know it, else leave as null

def objectFacade = ComponentAccessor.getOSGiComponentInstanceOfType(ObjectFacade)
ObjectBean assetObject = objectFacade.loadObjectBean(objectKey)
if (!assetObject) {
    log.warn("No asset found for key: $objectKey")
    return null
}

def attributeBean = null
if (objectTypeAttributeId) {
    attributeBean = assetObject.objectAttributeBeans.find { attr ->
        attr.objectTypeAttributeId == objectTypeAttributeId
    }
} else {
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
def userKeys = attributeBean.objectAttributeValueBeans*.value

def userManager = ComponentAccessor.userManager

// Map each user key to username if valid
def usernames = userKeys.collect { userKey ->
    ApplicationUser user = userManager.getUserByKey(userKey as String)
    if (user) {
        // Switch to user.displayName instead of user.name if you prefer display names
        user.name
    } else {
        log.warn("Could not find a user with the key: ${userKey}")
        null
    }
}.findAll { it != null } // Remove nulls (invalid users)

if (usernames) {
    log.info("Usernames found: ${usernames}")
    return usernames.join(", ") // Or return usernames if you want a list
} else {
    return "No valid users found"
}

import com.adaptavist.hapi.jira.issues.Issues
import com.atlassian.jira.component.ComponentAccessor

def issue = Issues.getByKey(issue.key)

// 1️⃣ Get single Asset object
def asset = issue.getCustomFieldValue('Asset Picker')
if (!asset) return

// 2️⃣ Get single referenced object
def referencedObject = asset.getAttributeValue('Application')
if (!referencedObject) return

// 3️⃣ Get multi-user attribute from referenced object
def userKeys = referencedObject.getAttributeValues('Approvers')
if (!userKeys) return

// 4️⃣ Convert user keys to ApplicationUser
def userManager = ComponentAccessor.userManager
def users = userKeys.collect { userManager.getUserByKey(it) }

log.warn("Approvers: ${users*.displayName}")

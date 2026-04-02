import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.issue.Issue

def currentUser = ComponentAccessor.jiraAuthenticationContext.loggedInUser
Issue child = issue  // current issue in transition

// 1. Get parent or linked parent
Issue parent = child.parentObject   // for sub-tasks
// If you use links instead of parent/child, comment above and use something like:
// def issueLinkManager = ComponentAccessor.issueLinkManager
// def links = issueLinkManager.getOutwardLinks(child.id)
// Issue parent = links.find { it.issueLinkType.name == "Parent-Child" }?.destinationObject

if (!parent) {
    // No parent found, require normal approval on child
    return true
}

// 2. Check parent status
def requiredParentStatusName = "Approved"   // adjust
if (parent.status.name != requiredParentStatusName) {
    // Parent not in required status, do NOT skip child approval
    return true
}

// 3. Check if same approver did parent approval
def customFieldManager = ComponentAccessor.customFieldManager
def approverCf = customFieldManager.getCustomFieldObjectByName("Approver")  // adjust to your field
def parentApprover = parent.getCustomFieldValue(approverCf)   // ApplicationUser for single user picker

if (!parentApprover) {
    // No approver on parent, do NOT skip child approval
    return true
}

// 4. Compare parent approver with current user
if (parentApprover.key == currentUser.key) {
    // Same person approved parent in required status:
    // return false if you want to HIDE this transition (i.e. "skipping" manual approval)
    // or keep true but add an auto-transition post-function instead.
    return false
}

// Different user -> require child approval
return true

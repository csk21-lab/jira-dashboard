import org.apache.log4j.Category
def Category log = Category.getInstance("com.onresolve.jira.groovy")
import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.issue.MutableIssue
import com.atlassian.jira.event.type.EventDispatchOption

def issueManager = ComponentAccessor.getIssueManager()
Object plugin = ComponentAccessor.getPluginAccessor().getPlugin("com.valiantys.jira.plugins.SQLFeed")
Class serviceClass = plugin.getClassLoader().loadClass("com.valiantys.nfeed.api.IFieldDisplayService")
Object fieldDisplayService = ComponentAccessor.getOSGiComponentInstanceOfType(serviceClass)

// COMMENT OUT THE NEXT TWO LINES FOR POST FUNCTION
// only use for debugging in console
def issue = issueManager.getIssueByCurrentKey("ABC-111")

// Get the issue's custom fields
def cfRequestParticipants = ComponentAccessor.getCustomFieldManager().getCustomFieldObjectByName("Request participants")
log.debug "Request participants Object Field: " + cfRequestParticipants
def members = issue.getCustomFieldValue(cfRequestParticipants)
log.debug "Request participants field value: " + members

// Hardcoded support group value
def supportGroup = "Hardcoded_Support_Group_Name"
log.debug "Support Group: " + supportGroup

def groupManager = ComponentAccessor.getGroupManager()
members = groupManager.getUsersInGroup(supportGroup)
log.debug "Members of Request participants: " + members

MutableIssue mutableIssue = issue
mutableIssue.setCustomFieldValue(cfRequestParticipants, members)
issueManager.updateIssue(ComponentAccessor.getJiraAuthenticationContext().getLoggedInUser(), mutableIssue, EventDispatchOption.ISSUE_UPDATED, false)

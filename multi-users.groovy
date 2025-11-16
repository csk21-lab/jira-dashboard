import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.issue.ModifiedValue
import com.atlassian.jira.issue.util.DefaultIssueChangeHolder

def customFieldManager = ComponentAccessor.getCustomFieldManager()
def issueManager = ComponentAccessor.getIssueManager()
def userManager = ComponentAccessor.getUserManager()
def changeHolder = new DefaultIssueChangeHolder()
def searchService = ComponentAccessor.getComponent(com.atlassian.jira.bc.issue.search.SearchService)
def authenticationContext = ComponentAccessor.jiraAuthenticationContext
def user = authenticationContext.loggedInUser

// JQL query to filter issues
def jqlQuery = "project = ABC AND status = Open" // replace with your JQL
def parseResult = searchService.parseQuery(user, jqlQuery)
if (!parseResult.isValid()) {
    return "Invalid JQL query"
}

def query = parseResult.getQuery()
def searchResult = searchService.search(user, query, com.atlassian.jira.web.bean.PagerFilter.getUnlimitedFilter())

def multiUserField = customFieldManager.getCustomFieldObjectByName("Multi User Picker Field Name")
def newUser = userManager.getUserByKey("usernameToAdd")  // User to append

searchResult.getIssues().each { issue ->
    def existingUsers = issue.getCustomFieldValue(multiUserField) ?: []
    def updatedUsers = existingUsers.toSet() + [newUser]
    multiUserField.updateValue(null, issue, new ModifiedValue(existingUsers, updatedUsers.toList()), changeHolder)
}

import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.issue.ModifiedValue
import com.atlassian.jira.issue.util.DefaultIssueChangeHolder
import com.atlassian.jira.bc.issue.search.SearchService
import com.atlassian.jira.web.bean.PagerFilter

// Parameters
def userKeyToAppend = "userkey"  // User key of the user you want to add
def multiUserFieldName = "Multi User Picker Field Name"  // Update with your custom field name
def jqlQuery = "project = ABC AND status = Open"  // Update with your JQL

// Components
def customFieldManager = ComponentAccessor.getCustomFieldManager()
def userManager = ComponentAccessor.getUserManager()
def searchService = ComponentAccessor.getComponent(SearchService)
def authenticationContext = ComponentAccessor.jiraAuthenticationContext
def runningUser = authenticationContext.loggedInUser
def changeHolder = new DefaultIssueChangeHolder()

// Validate JQL
def parseResult = searchService.parseQuery(runningUser, jqlQuery)
if (!parseResult.isValid()) {
    def errs = parseResult.errors ? parseResult.errors.join('; ') : 'unknown parse error'
    throw new IllegalArgumentException("Invalid JQL query: ${errs}")
}

// Search issues
def query = parseResult.getQuery()
// use an unlimited pager if you truly want every issue
def searchResults = searchService.search(runningUser, query, PagerFilter.getUnlimitedFilter())

// IMPORTANT: use getResults() (or .results) — SearchResults does not have getIssues()
def issues = searchResults.getResults()

def multiUserField = customFieldManager.getCustomFieldObjectByName(multiUserFieldName)
assert multiUserField : "Custom field not found: ${multiUserFieldName}"

def userToAppend = userManager.getUserByKey(userKeyToAppend)
assert userToAppend : "User not found: ${userKeyToAppend}"

issues.each { issue ->
    // ensure existingUsers is a List (could be null)
    def existingUsers = (issue.getCustomFieldValue(multiUserField) ?: []) as List
    // merge de-duplicated
    def updatedUsers = (existingUsers.toSet() + userToAppend).toList()
    // pass runningUser (or a real ApplicationUser) as context instead of null
    multiUserField.updateValue(runningUser, issue, new ModifiedValue(existingUsers, updatedUsers), changeHolder)
}

return "Appended user ${userKeyToAppend} to ${issues.size()} issues matching JQL: ${jqlQuery}"

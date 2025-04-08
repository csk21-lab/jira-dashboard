import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.issue.CustomFieldManager
import com.atlassian.jira.issue.search.SearchProvider
import com.atlassian.jira.bc.issue.search.SearchService
import com.atlassian.jira.util.QueryFactory
import com.atlassian.jira.web.bean.PagerFilter
import com.atlassian.jira.user.ApplicationUsers

def user = ComponentAccessor.jiraAuthenticationContext.loggedInUser
def searchService = ComponentAccessor.getComponent(SearchService)
def customFieldManager = ComponentAccessor.customFieldManager
def issueManager = ComponentAccessor.issueManager

// Update to your Assets field name and JQL query
def assetField = customFieldManager.getCustomFieldObjectByName("Assets")
def jqlQuery = 'project = ABC AND "Assets" IS NOT EMPTY'

def parseResult = searchService.parseQuery(user, jqlQuery)
if (!parseResult.isValid()) {
    log.error("Invalid JQL: ${parseResult.errors}")
    return
}

def results = searchService.search(user, parseResult.query, PagerFilter.unlimitedFilter)
def issues = results.results

issues.each { issue ->
    def value = issue.getCustomFieldValue(assetField)
    def count = 0

    if (value instanceof List) {
        count = value.size()
    } else if (value != null) {
        count = 1
    }

    log.warn("${issue.key} has ${count} asset(s) selected.")
}

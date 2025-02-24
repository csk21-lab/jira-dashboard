import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.bc.issue.search.SearchService
import com.atlassian.jira.issue.IssueManager
import com.atlassian.jira.issue.MutableIssue
import com.atlassian.jira.user.ApplicationUser
import com.atlassian.jira.web.bean.PagerFilter
import com.onresolve.scriptrunner.runner.util.UserMessageUtil

def issueManager = ComponentAccessor.getIssueManager()
def searchService = ComponentAccessor.getComponent(SearchService)
def loggedInUser = ComponentAccessor.jiraAuthenticationContext.loggedInUser as ApplicationUser

def jqlQuery = "issuekey in (ABC-6592, ABC-1540)"

try {
    def parseResult = searchService.parseQuery(loggedInUser, jqlQuery)
    if (!parseResult.isValid()) {
        log.error("Invalid JQL query: ${jqlQuery}")
        return
    }

    def query = parseResult.getQuery()
    def searchResults = searchService.search(loggedInUser, query, PagerFilter.getUnlimitedFilter())
    def issues = searchResults.getIssues()

    if (issues.size() > 0) {
        issues.each { issue ->
            def issueDescription = issue.getDescription()
            if (issueDescription.contains("ABC-338")) {
                issueDescription = issueDescription.replace("ABC-338", "ABC-343")
                log.warn("IF Block : ${issueDescription}")

                // Update the issue description using ScriptRunner's updateIssue method
                def mutableIssue = issueManager.getIssueObject(issue.id) as MutableIssue
                mutableIssue.setDescription(issueDescription)
                ComponentAccessor.getIssueManager().updateIssue(loggedInUser, mutableIssue, EventDispatchOption.ISSUE_UPDATED, false)
                UserMessageUtil.success("Successfully updated the issue with key ${issue.key}")
            }
        }
    } else {
        log.warn("No issues found for the JQL query: ${jqlQuery}")
    }
} catch (Exception e) {
    log.error("An error occurred: ${e.message}")
}

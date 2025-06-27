import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.issue.search.SearchProvider
import com.atlassian.jira.web.bean.PagerFilter
import com.atlassian.query.Query
import com.atlassian.jira.jql.parser.JqlQueryParser
import com.riadalabs.jira.plugins.insight.services.model.ObjectBean

def jql = 'project = ABC AND issuetype = Bug'  // Replace with your JQL
def customFieldId = "customfield_00100"        // Replace with your Insight field ID

def searchService = ComponentAccessor.getComponent(SearchProvider)
def user = ComponentAccessor.jiraAuthenticationContext.loggedInUser
def jqlParser = ComponentAccessor.getComponent(JqlQueryParser)
def customFieldManager = ComponentAccessor.customFieldManager

def assetField = customFieldManager.getCustomFieldObject(customFieldId)
Query query = jqlParser.parseQuery(jql)
def results = searchService.search(query, user, PagerFilter.getUnlimitedFilter())

results.issues.each { issue ->
    def assetValue = issue.getCustomFieldValue(assetField)
    int count = 0

    if (assetValue instanceof List) {
        count = assetValue.size()
    } else if (assetValue instanceof ObjectBean) {
        count = 1
    }

    log.warn("Issue ${issue.key} has ${count} asset object(s) in field '${assetField.name}'")
}

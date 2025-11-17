import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.bc.issue.search.SearchService
import com.atlassian.jira.web.bean.PagerFilter
import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.issue.IssueInputParameters

/*
  Corrected script: uses SearchResults.getResults() and IssueService to perform updates
  - Update the three parameters below before running:
      userIdentifier: user key (Server/DC) or accountId (Cloud) or username (older Jira)
      multiUserFieldName: friendly name of the multi-user picker custom field (e.g. "Reviewers")
      jqlQuery: JQL that selects the issues to update
  - This approach avoids calling concrete CustomField implementation.updateValue(...) signatures,
    and works across Jira versions (Server/DC and many Cloud setups), handling accountId vs userKey where available.
*/

def userIdentifier = "userkey_or_accountId_or_username"    // <-- change me
def multiUserFieldName = "Multi User Picker Field Name"    // <-- change me
def jqlQuery = "project = ABC AND status = Open"           // <-- change me

def customFieldManager = ComponentAccessor.getCustomFieldManager()
def userManager = ComponentAccessor.getUserManager()
def searchService = ComponentAccessor.getComponent(SearchService)
def authenticationContext = ComponentAccessor.jiraAuthenticationContext
def runningUser = authenticationContext.loggedInUser
def issueService = ComponentAccessor.issueService

if (!runningUser) {
    throw new IllegalStateException("No logged in user (runningUser). Script must be executed by a user.")
}

// resolve the custom field
def multiUserField = customFieldManager.getCustomFieldObjectByName(multiUserFieldName)
assert multiUserField : "Custom field not found: ${multiUserFieldName}"

// Try to resolve the user to append. Try key, then name.
def userToAppend = userManager.getUserByKey(userIdentifier) ?: userManager.getUserByName(userIdentifier)
if (!userToAppend) {
    // In some environments you might only have accountId (Cloud); we can still proceed without resolving to ApplicationUser,
    // but this script expects a resolved ApplicationUser if possible to determine which identifier to use.
    throw new IllegalArgumentException("User not found by key or name: ${userIdentifier}. If you are on Cloud, supply the accountId and ensure user lookup works.")
}

// Determine which identifier we'll write into the custom field (accountId preferred if present)
def idToAdd = (userToAppend.metaClass.respondsTo(userToAppend, "getAccountId") && userToAppend.accountId) ? userToAppend.accountId :
              (userToAppend.key ?: userToAppend.name)

// Validate JQL
def parseResult = searchService.parseQuery(runningUser, jqlQuery)
if (!parseResult.isValid()) {
    def errs = parseResult.errors ? parseResult.errors.join('; ') : 'unknown parse error'
    throw new IllegalArgumentException("Invalid JQL query: ${errs}")
}

def query = parseResult.getQuery()
// Use PagerFilter.getUnlimitedFilter() with caution if you expect very many issues
def searchResults = searchService.search(runningUser, query, PagerFilter.getUnlimitedFilter())

// IMPORTANT: use getResults() (SearchResults does not have getIssues())
def issues = searchResults.getResults()

def updatedCount = 0
def failedUpdates = []

issues.each { issue ->
    try {
        // Read existing custom field value — may be null, a List<ApplicationUser>, or a List<String> depending on Jira/version
        def existingValues = issue.getCustomFieldValue(multiUserField)
        def existingIds = []

        if (existingValues instanceof Collection) {
            existingIds = existingValues.collect { v ->
                // if the stored values are ApplicationUser-like objects, extract accountId or key
                if (v == null) {
                    return null
                } else if (v.metaClass.respondsTo(v, "getAccountId") && v.accountId) {
                    return v.accountId
                } else if (v.metaClass.respondsTo(v, "getKey") && v.key) {
                    return v.key
                } else {
                    // might already be a String (accountId or key)
                    return v.toString()
                }
            }.findAll { it } as List
        }

        // Merge and de-duplicate
        def mergedIds = (existingIds.toSet() + idToAdd).toList()

        // Build IssueInputParameters with the desired values for the custom field.
        // Use the custom field id string (e.g. "customfield_10010")
        IssueInputParameters params = issueService.newIssueInputParameters()
        // Clear any existing values for this field by not adding them except the merged ones.
        mergedIds.each { val ->
            params.addCustomFieldValue(multiUserField.getId(), val)
        }

        // Validate and perform update
        def validateUpdate = issueService.validateUpdate(runningUser, issue.id as Long, params)
        if (!validateUpdate.isValid()) {
            failedUpdates << [issue: issue.key, reason: validateUpdate.errorCollection?.toString()]
            return
        }

        def updateResult = issueService.update(runningUser, validateUpdate)
        if (!updateResult.isValid()) {
            failedUpdates << [issue: issue.key, reason: updateResult.errorCollection?.toString()]
            return
        }

        updatedCount++
    } catch (Exception e) {
        failedUpdates << [issue: issue?.key ?: 'unknown', reason: e.toString()]
    }
}

def summary = "Attempted to append user '${userIdentifier}' to ${issues.size()} issues matching JQL: ${jqlQuery}\n" +
              "Successfully updated: ${updatedCount}\n" +
              "Failures: ${failedUpdates.size()}\n" +
              (failedUpdates ? "Failed details: ${failedUpdates}" : "")

// Print summary to console and return
println summary
return summary

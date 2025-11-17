import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.bc.issue.search.SearchService
import com.atlassian.jira.web.bean.PagerFilter
import com.atlassian.jira.issue.IssueInputParameters

/*
 Script: Append a user to a Multi User Picker custom field across issues matched by JQL.
 - Update the three variables below before running:
    userIdentifier: user key (Server/DC) or accountId (Cloud) or username (older Jira)
    multiUserFieldName: the display name of your multi-user picker custom field
    jqlQuery: the JQL selecting issues to update
 - This script merges existing field values + the new user and writes the merged set back.
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

// Resolve custom field
def multiUserField = customFieldManager.getCustomFieldObjectByName(multiUserFieldName)
assert multiUserField : "Custom field not found: ${multiUserFieldName}"

// Resolve user to append (try key then name). If on Cloud and you only have accountId,
 // you can pass that as userIdentifier but getUserByKey/getUserByName may not resolve it.
def userToAppend = userManager.getUserByKey(userIdentifier) ?: userManager.getUserByName(userIdentifier)
if (!userToAppend) {
    // If userToAppend is not resolved (e.g. only accountId available in Cloud), we'll still use the provided identifier directly.
    // But prefer resolving to ApplicationUser when possible.
    log.warn("Could not resolve an ApplicationUser for '${userIdentifier}'. Proceeding with identifier string as-is.")
}

// Decide which ID string we will persist in the custom field (accountId preferred)
def idToAdd = userToAppend ? ((userToAppend.metaClass.respondsTo(userToAppend, "getAccountId") && userToAppend.accountId) ? userToAppend.accountId : (userToAppend.key ?: userToAppend.name)) : userIdentifier

// Validate JQL
def parseResult = searchService.parseQuery(runningUser, jqlQuery)
if (!parseResult.isValid()) {
    def errs = parseResult.errors ? parseResult.errors.join('; ') : 'unknown parse error'
    throw new IllegalArgumentException("Invalid JQL query: ${errs}")
}

def query = parseResult.getQuery()
def searchResults = searchService.search(runningUser, query, PagerFilter.getUnlimitedFilter())
// Use getResults() (SearchResults does not have getIssues())
def issues = searchResults.getResults()

def updatedCount = 0
def failedUpdates = []

issues.each { issue ->
    try {
        // Read existing custom field values (could be null, Collection<ApplicationUser>, or Collection<String>)
        def existingValues = issue.getCustomFieldValue(multiUserField)
        def existingIds = []

        if (existingValues instanceof Collection) {
            existingIds = existingValues.collect { v ->
                if (v == null) {
                    return null
                } else if (v.metaClass.respondsTo(v, "getAccountId") && v.accountId) {
                    return v.accountId
                } else if (v.metaClass.respondsTo(v, "getKey") && v.key) {
                    return v.key
                } else {
                    // might already be a String identifier
                    return v.toString()
                }
            }.findAll { it } as List
        }

        // If the id to add is already present, skip update for this issue
        if (existingIds.contains(idToAdd)) {
            // nothing to append
            return
        }

        // Merge (existing + new), preserving uniqueness
        def mergedIds = (existingIds.toSet() + idToAdd).toList()

        // Build IssueInputParameters and set the custom field values to the merged list.
        IssueInputParameters params = issueService.newIssueInputParameters()
        // addCustomFieldValue expects the custom field id string like "customfield_10010"
        def cfId = multiUserField.getId()
        mergedIds.each { val ->
            params.addCustomFieldValue(cfId, val)
        }

        // Validate and update
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

def summary = "Attempted to append '${idToAdd}' to ${issues.size()} issues matching JQL: ${jqlQuery}\n" +
              "Successfully updated: ${updatedCount}\n" +
              "Failures: ${failedUpdates.size()}\n" +
              (failedUpdates ? "Failed details: ${failedUpdates}" : "")

println summary
return summary

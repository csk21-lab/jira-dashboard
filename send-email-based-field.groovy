import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.issue.Issue
import com.atlassian.jira.bc.issue.search.SearchService
import com.atlassian.jira.web.bean.PagerFilter
import com.atlassian.jira.issue.fields.CustomField
import com.onresolve.scriptrunner.runner.util.UserMessageUtil
import com.onresolve.scriptrunner.runner.util.Mail

// Script config
String jqlString = 'project = ABC AND status = "Open"'
String impactCfName = 'Impact'

// Impact -> list of email recipients
Map<String, List<String>> impactToEmails = [
    'ABC': ['rama@gmail.com'],
    'XYZ': ['rama@gmail.com', 'steeve@gmail.com'],
]

// --- JQL search ---
def user = ComponentAccessor.jiraAuthenticationContext.loggedInUser
SearchService searchService = ComponentAccessor.getComponent(SearchService)

def parseResult = searchService.parseQuery(user, jqlString)
if (!parseResult.isValid()) {
    log.warn "Invalid JQL: ${parseResult.errors}"
    UserMessageUtil.error("Invalid JQL: ${parseResult.errors}")
    return
}

def results = searchService.search(user, parseResult.query, PagerFilter.unlimitedFilter)
List<Issue> issues = results.issues
log.warn "Found ${issues.size()} issues for JQL: ${jqlString}"

// --- get Impact custom field ---
def customFieldManager = ComponentAccessor.customFieldManager
CustomField impactCf = customFieldManager.getCustomFieldObjectByName(impactCfName)
if (!impactCf) {
    log.warn "Custom field '${impactCfName}' not found"
    UserMessageUtil.error("Custom field '${impactCfName}' not found")
    return
}

// --- group issues by impact value ---
Map<String, List<Issue>> impactToIssues = [:].withDefault { [] }

issues.each { Issue issue ->
    def option = issue.getCustomFieldValue(impactCf)
    String impactValue = option?.value as String

    log.warn "Issue ${issue.key} has Impact='${impactValue}'"

    if (impactValue) {
        impactToIssues[impactValue] << issue
    } else {
        log.warn "Issue ${issue.key} has no Impact value, skipping"
    }
}

// --- send emails per impact value ---
impactToIssues.each { String impactValue, List<Issue> impactIssues ->

    List<String> recipients = impactToEmails[impactValue]
    log.warn "For Impact='${impactValue}' found ${impactIssues.size()} issues: ${impactIssues*.key}"
    log.warn "Recipients for Impact='${impactValue}': ${recipients}"

    if (!recipients) {
        log.warn "No email mapping defined for impact '${impactValue}', skipping"
        return
    }

    String subject = "Issues with Impact '${impactValue}' found by JQL"
    String body = """Hello,

The following issues have Impact = ${impactValue}:

${impactIssues.collect { "- ${it.key}: ${it.summary}" }.join("\n")}

JQL used:
${jqlString}

Regards,
Jira Notification
"""

    log.warn "Sending email for Impact='${impactValue}' to ${recipients} with subject='${subject}'"

    Mail.send {
        setTo(*recipients as String[])
        setSubject(subject)
        setBody(body)
        setMimeType("text/plain")
    }

    log.warn "Email sent for Impact='${impactValue}' to ${recipients}"
}

UserMessageUtil.success("Processed ${issues.size()} issues and sent emails for impacts: ${impactToIssues.keySet().join(', ')}")

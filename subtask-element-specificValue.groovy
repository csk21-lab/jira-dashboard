import com.atlassian.jira.component.ComponentAccessor
import groovy.json.JsonSlurper

def issue = ComponentAccessor.issueManager.getIssueByCurrentKey("ABC-189")

def cf = ComponentAccessor.customFieldManager.getCustomFieldObject("customfield_10000")
def issueService = ComponentAccessor.issueService
def user = ComponentAccessor.jiraAuthenticationContext.loggedInUser

def triggerValue = "ABC_TEST"

def rawValue = issue.getCustomFieldValue(cf)
log.warn("Raw value: ${rawValue}")

if(!rawValue) return

def json = new JsonSlurper().parseText(rawValue.toString())
def values = json.keys

log.warn("Values: ${values}")

// only proceed if trigger value exists
if(!values.contains(triggerValue)){
    log.warn("Trigger value not found")
    return
}

values.each{ val ->

    if(val == triggerValue) return   // skip trigger value

    def params = issueService.newIssueInputParameters()
            .setProjectId(issue.projectObject.id)
            .setIssueTypeId("10300")  // subtask type
            .setSummary(val)
            .setReporterId(user.name)

    def validation = issueService.validateSubTaskCreate(user, issue.id, params)

    if(validation.isValid()){
        def result = issueService.create(user, validation)
        log.warn("Subtask created: ${result.issue.key}")
    }
}

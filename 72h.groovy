import com.atlassian.jira.component.ComponentAccessor
import com.opensymphony.workflow.InvalidInputException
import com.atlassian.jira.user.preferences.PreferenceKeys

import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

def customFieldManager = ComponentAccessor.customFieldManager
def userPreferencesManager = ComponentAccessor.userPreferencesManager
def authenticationContext = ComponentAccessor.jiraAuthenticationContext

// ---- CONFIG: update these IDs / names ----
def startCf    = customFieldManager.getCustomFieldObject("customfield_12345") // Start
def endCf      = customFieldManager.getCustomFieldObject("customfield_12346") // End
def severityCf = customFieldManager.getCustomFieldObject("customfield_12347") // Severity
def severityToCheck = "No"   // option text to trigger validation
// ------------------------------------------

// Get Severity value (single select)
def severityOption = issue.getCustomFieldValue(severityCf)   // com.atlassian.jira.issue.customfields.option.Option
def severityValue  = severityOption ? severityOption.value as String : null

// If Severity is not "No", skip all Start/End checks
if (severityValue != severityToCheck) {
    return true
}

// From here down is the same Start/End validation as before
def startVal = issue.getCustomFieldValue(startCf) as Date
def endVal   = issue.getCustomFieldValue(endCf)   as Date

if (!startVal || !endVal) {
    throw new InvalidInputException("Both Start and End window must be entered.")
}

// Resolve user timezone (fallback)
def currentUser = authenticationContext.loggedInUser
def tzIdStr = userPreferencesManager
        .getExtendedPreferences(currentUser)
        .getString(PreferenceKeys.USER_TIMEZONE)

ZoneId userZoneId
try {
    userZoneId = tzIdStr ? ZoneId.of(tzIdStr) : ZoneId.systemDefault()
} catch (Exception e) {
    userZoneId = ZoneId.systemDefault()
}

def cstZoneId = ZoneId.of("America/Chicago")

def startUser = ZonedDateTime.ofInstant(startVal.toInstant(), userZoneId)
def endUser   = ZonedDateTime.ofInstant(endVal.toInstant(),   userZoneId)

def startCst = startUser.withZoneSameInstant(cstZoneId)
def endCst   = endUser.withZoneSameInstant(cstZoneId)

def nowUser = ZonedDateTime.now(userZoneId)
def minStartUser = nowUser.plusHours(72)

def fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm z")

if (startUser.isBefore(minStartUser)) {
    def minStartUserStr = minStartUser.format(fmt)
    def minStartCstStr  = minStartUser.withZoneSameInstant(cstZoneId).format(fmt)

    throw new InvalidInputException(
        "Enter a Start time at least 72 hours from now. " +
        "Earliest allowed Start in your timezone: ${minStartUserStr}; " +
        "equivalent CST: ${minStartCstStr}."
    )
}

if (!endUser.isAfter(startUser)) {
    def startUserStr = startUser.format(fmt)
    def startCstStr  = startCst.format(fmt)

    throw new InvalidInputException(
        "End must be after Start. Current Start is ${startUserStr} " +
        "(CST: ${startCstStr})."
    )
}

return true

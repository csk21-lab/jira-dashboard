import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.issue.CustomFieldManager
import com.atlassian.jira.issue.MutableIssue
import com.atlassian.jira.issue.util.DefaultIssueChangeHolder
import com.riadalabs.jira.plugins.insight.services.model.ObjectTypeAttributeBean

def issue = event.issue as MutableIssue

// Get the necessary managers
def customFieldManager = ComponentAccessor.getCustomFieldManager()
def issueManager = ComponentAccessor.getIssueManager()
log.warn("Issue: ${issue}")

def exploitableOrder = ["Yes", "No"]

// Function to get the highest severity value from asset objects
def getHighestSeverity(assetObjects) {
    def highestSeverity = "No" // Default to the lowest severity
    log.warn("Starting exploitable evaluation... : ${assetObjects} ")
    def severityList = []
    assetObjects.each { asset ->
        def vulAsset = Assets.getByKey(asset?.objectKey)
        def severityValue = vulAsset.getString('Exploitable')
        log.warn("vulAsset : ${vulAsset}-${severityValue}")
        severityList.add(severityValue)
    }
    if (severityList.contains('Yes')) {
        highestSeverity = 'Yes'
    } else if (severityList.contains('No')) {
        highestSeverity = 'No'
    }
    log.warn("Final highest Exp : ${highestSeverity}")
    return highestSeverity
}

// Get the custom field objects
def assetField = customFieldManager.getCustomFieldObjectsByName("Vulnerability Details")
if (issue.issueType.name == 'Vulnerability Asset') {
    if (assetField) {
        log.warn("Custom fields found: 'V asset' and 'Severity'")
        def assetObjects = issue.getCustomFieldValue(assetField)
        log.warn("assetObjects: ${assetObjects}")

        if (assetObjects) {
            log.warn("Asset objects found: ${assetObjects.size()}")
            // Get the highest severity value from the asset objects
            def highestSeverity = getHighestSeverity(assetObjects)
            def issueSummary = issue.summary
            def issueDescription = issue.description

            if (highestSeverity == "Yes" && !issueSummary.contains("EXPT")) {
                issueSummary = "EXPT - ${issueSummary}"
                issueDescription = issueDescription + " EXP"
                log.warn("IF Block highestSeverity: ${highestSeverity} - ${issueSummary}")
            } else if (highestSeverity == "No" && issueSummary.contains("EXPT")) {
                issueSummary = issueSummary.replace("EXPT - ", "")
                issueDescription = issueDescription.replace(" EXP", "")
                log.warn("ELSE IF Block highestSeverity: ${highestSeverity} - ${issueSummary}")
            }

            issue.update {
                setCustomFieldValue('Exploitable', highestSeverity)
                setSummary(issueSummary)
                setDescription(issueDescription)
            }
            log.warn("Exploitable field updated to ${highestSeverity} for issue ${issue.key}")
        } else {
            log.warn("No asset objects found in 'V asset' field for issue ${issue.key}")
        }
    } else {
        log.error("Custom fields 'V asset' or 'Exploitable' not found")
    }
}

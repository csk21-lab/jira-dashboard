import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.issue.Issue
import com.atlassian.jira.issue.CustomFieldManager
import com.atlassian.jira.issue.fields.CustomField
import com.atlassian.jira.issue.ModifiedValue
import com.atlassian.jira.issue.util.DefaultIssueChangeHolder

// Get the necessary managers
def customFieldManager = ComponentAccessor.getCustomFieldManager()
def issueManager = ComponentAccessor.getIssueManager()

// Define the severity order
def severityOrder = ["Critical", "High", "Medium", "Low"]

// Function to get the highest severity value from asset objects
def getHighestSeverity(assetObjects) {
    def highestSeverity = "Low" // Default to the lowest severity
    assetObjects.each { asset ->
        def severity = asset.get("severity")
        if (severity && severityOrder.indexOf(severity) < severityOrder.indexOf(highestSeverity)) {
            highestSeverity = severity
        }
    }
    return highestSeverity
}

// Get the current issue (use the appropriate method to get the issue in your context)
def issue = issueManager.getIssueObject("ISSUE-KEY") // Replace with the actual issue key or method to get the issue

// Get the custom field objects
def assetField = customFieldManager.getCustomFieldObjectByName("V asset")
def severityField = customFieldManager.getCustomFieldObjectByName("Severity")

if (assetField && severityField) {
    // Get the asset objects from the "V asset" field
    def assetObjects = issue.getCustomFieldValue(assetField)

    // Get the highest severity value from the asset objects
    def highestSeverity = getHighestSeverity(assetObjects)

    // Set the "Severity" custom field value based on the highest severity found
    def changeHolder = new DefaultIssueChangeHolder()
    severityField.updateValue(null, issue, new ModifiedValue(issue.getCustomFieldValue(severityField), highestSeverity), changeHolder)
    
    log.info("Severity field updated to ${highestSeverity} for issue ${issue.key}")
} else {
    log.error("Custom fields 'V asset' or 'Severity' not found")
}

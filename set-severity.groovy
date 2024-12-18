import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.issue.CustomFieldManager
import com.atlassian.jira.issue.ModifiedValue
import com.atlassian.jira.issue.util.DefaultIssueChangeHolder
import com.riadalabs.jira.plugins.insight.services.model.ObjectAttributeBean
import com.riadalabs.jira.plugins.insight.services.model.ObjectTypeAttributeBean

// Get the necessary managers
def customFieldManager = ComponentAccessor.getCustomFieldManager()
def issueManager = ComponentAccessor.getIssueManager()

// Define the severity order
def severityOrder = ["Critical", "High", "Medium", "Low"]

// Function to get the highest severity value from asset objects
def getHighestSeverity(assetObjects) {
    def highestSeverity = "Low" // Default to the lowest severity
    log.warn("Starting severity evaluation...")
    assetObjects.each { asset ->
        // Get the 'Severity' attribute values
        def severityAttributes = asset.getObjectAttributeBeans().findAll { it.objectTypeAttributeId == "Severity" }
        severityAttributes.each { attribute ->
            def severity = attribute.objectAttributeValueBeans*.value[0]
            log.warn("Evaluating severity: ${severity}")
            if (severity && severityOrder.indexOf(severity) < severityOrder.indexOf(highestSeverity)) {
                highestSeverity = severity
                log.warn("Updated highest severity to: ${highestSeverity}")
            }
        }
    }
    log.warn("Final highest severity: ${highestSeverity}")
    return highestSeverity
}

// Get the current issue (use the appropriate method to get the issue in your context)
def issue = issueManager.getIssueObject("ISSUE-KEY") // Replace with the actual issue key or method to get the issue
log.warn("Processing issue: ${issue.key}")

// Get the custom field objects
def assetField = customFieldManager.getCustomFieldObjectByName("V asset")
def severityField = customFieldManager.getCustomFieldObjectByName("Severity")

if (assetField && severityField) {
    log.warn("Custom fields found: 'V asset' and 'Severity'")
    // Get the asset objects from the "V asset" field
    def assetObjects = issue.getCustomFieldValue(assetField)

    if (assetObjects) {
        log.warn("Asset objects found: ${assetObjects.size()}")
        // Get the highest severity value from the asset objects
        def highestSeverity = getHighestSeverity(assetObjects)

        // Set the "Severity" custom field value based on the highest severity found
        def changeHolder = new DefaultIssueChangeHolder()
        severityField.updateValue(null, issue, new ModifiedValue(issue.getCustomFieldValue(severityField), highestSeverity), changeHolder)

        log.warn("Severity field updated to ${highestSeverity} for issue ${issue.key}")
    } else {
        log.warn("No asset objects found in 'V asset' field for issue ${issue.key}")
    }
} else {
    log.error("Custom fields 'V asset' or 'Severity' not found")
}

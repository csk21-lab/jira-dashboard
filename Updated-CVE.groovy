import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.issue.CustomFieldManager
import com.atlassian.jira.issue.Issue
import com.atlassian.jira.issue.MutableIssue
import com.atlassian.jira.issue.fields.CustomField
import com.atlassian.jira.event.type.EventDispatchOption

// Get the necessary managers
def customFieldManager = ComponentAccessor.getCustomFieldManager()
def issueManager = ComponentAccessor.getIssueManager()

// Define the extended CVE Score Order (from 1.0 to 10.0 with a step of 0.1)
def cveScoreOrder = (1..10).collect { (it / 10.0).toString().padRight(3, '0') }
cveScoreOrder.add('10.0')  // Ensure that 10.0 is included at the end
log.warn("Extended CVE Score Order: ${cveScoreOrder}")

// Convert cveScoreOrder values into BigDecimal for proper numeric comparison
def numericCveScoreOrder = cveScoreOrder.collect { new BigDecimal(it) }

// Function to get the highest CVE score from asset objects
def getHighestCveScore(assetObjects) {
    def highestCveScore = new BigDecimal('1.0')  // Default to the lowest severity (1.0)
    log.warn("Starting CVE Score evaluation... : ${assetObjects}")
    
    def severityList = []
    assetObjects.each { asset ->
        def vulAsset = Assets.getByKey(asset?.objectKey)
        if (vulAsset) {
            def severityValue = vulAsset.getAttributeValues('CVSS Version3 BaseScore')
            if (severityValue) {
                // Log severity value for each asset
                log.warn("vulAsset: ${vulAsset} - CVE Score: ${severityValue}")
                severityList.add(new BigDecimal(severityValue))
            }
        }
    }

    // Find the highest severity score by comparing to numericCveScoreOrder
    severityList.each { severity ->
        def highestScoreIndex = numericCveScoreOrder.indexOf(highestCveScore)
        def currentScoreIndex = numericCveScoreOrder.indexOf(severity)
        
        // Compare numerically
        if (currentScoreIndex > highestScoreIndex) {
            highestCveScore = severity
        }
    }

    log.warn("Final highest CVE Score: ${highestCveScore}")
    return highestCveScore.toString()
}

// Fetch the issue (replace with the correct issue key)
def issue = issueManager.getIssueObject("XYZ-28")  // Replace with actual issue key

if (issue && issue.issueType.name == 'VA') {
    // Get the custom field for "Vulnerability Details" (this should match the actual field name)
    def assetField = customFieldManager.getCustomFieldObjectsByName("Vulnerability Details").find { it.name == "Vulnerability Details" }

    if (assetField) {
        log.warn("Custom field 'Vulnerability Details' found")

        // Retrieve the asset objects from the custom field value
        def assetObjects = issue.getCustomFieldValue(assetField) as List

        if (assetObjects) {
            log.warn("Asset objects found: ${assetObjects.size()}")
            
            // Get the highest severity value from the asset objects
            def highestSeverity = getHighestCveScore(assetObjects)
            
            // Get the custom field for "CVE Score" (replace with your actual CVE score custom field ID or name)
            def cveScoreField = customFieldManager.getCustomFieldObjectByName("CVE Score")

            if (cveScoreField) {
                // Set the new value for the CVE Score field
                issue.setCustomFieldValue(cveScoreField, highestSeverity)

                // Save the updated issue
                ComponentAccessor.getIssueManager().updateIssue(
                    ComponentAccessor.getJiraAuthenticationContext().getLoggedInUser(),
                    issue,
                    EventDispatchOption.DO_NOT_DISPATCH,
                    false
                )

                log.warn("CVE Score field updated to ${highestSeverity} for issue ${issue.key}")
            } else {
                log.error("Custom field 'CVE Score' not found")
            }
        } else {
            log.warn("No asset objects found in 'Vulnerability Details' field for issue ${issue.key}")
        }
    } else {
        log.error("Custom field 'Vulnerability Details' not found")
    }
} else {
    log.error("Issue not found or issue type is not 'VA'")
}

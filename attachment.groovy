import com.atlassian.jira.bc.issue.search.SearchService
import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.config.util.JiraHome
import com.atlassian.jira.issue.Issue
import com.atlassian.jira.issue.attachment.CreateAttachmentParamsBean
import com.atlassian.jira.issue.fields.CustomField
import com.atlassian.jira.issue.fields.CustomFieldManager
import com.atlassian.jira.web.bean.PagerFilter
import org.joda.time.DateTime

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.PrintWriter

def today = new DateTime()
def reportPath = ComponentAccessor.getComponent(JiraHome).getHome().toString() + "/export/slaexports/"
def reportFilename = "asset_export_" + today.toString('MM-dd-yyyy-HHmm') + ".csv"
def reportFilePath = reportPath + reportFilename
def reportDirectory = new File(reportPath)

// Create the directory if it doesn't exist
if (!reportDirectory.exists()) {
    log.warn "${reportDirectory} does not exist."
    try {
        log.warn "Creating ${reportDirectory}."
        reportDirectory.mkdirs()
    } catch (Exception e) {
        log.error "Unable to create directory. ${e}"
        throw e
    }
}

// Define the custom field name and issue key
def customFieldName = "Asset Custom Field"
def issueKey = "PROJECT-123"  // Replace with your specific issue key

// Get custom field manager, issue manager, and attachment manager
def customFieldManager = ComponentAccessor.getCustomFieldManager()
def issueManager = ComponentAccessor.getIssueManager()
def attachmentManager = ComponentAccessor.getAttachmentManager()
def user = ComponentAccessor.getJiraAuthenticationContext().getLoggedInUser()

// Get the custom field object
def customField = customFieldManager.getCustomFieldObjectByName(customFieldName)
if (!customField) {
    throw new IllegalArgumentException("Custom field with name '${customFieldName}' not found")
}

// Get the issue
def issue = issueManager.getIssueByCurrentKey(issueKey)
if (!issue) {
    throw new IllegalArgumentException("Issue with key '${issueKey}' not found")
}

// Get the asset objects from the custom field
def assetObjects = issue.getCustomFieldValue(customField)
if (!assetObjects) {
    throw new IllegalArgumentException("No asset objects found in the custom field for issue '${issueKey}'")
}

// Create a ByteArrayOutputStream to hold the CSV data
ByteArrayOutputStream baos = new ByteArrayOutputStream()
PrintWriter writer = new PrintWriter(baos)

// Write CSV headers
writer.println("Issue Key,Asset Object")

// Write CSV data
for (def assetObject : assetObjects) {
    writer.println("${issue.getKey()},${assetObject.toString()}")
}

// Close the writer
writer.close()

// Create an attachment file
def fileName = "${issue.key}_asset_export.csv"
def fileData = baos.toByteArray()

// Attach the file to the issue using CreateAttachmentParamsBean
try {
    def attachmentParams = new CreateAttachmentParamsBean.Builder(
        new ByteArrayInputStream(fileData),
        fileName,
        "text/csv",
        user,
        issue
    ).build()

    attachmentManager.createAttachment(attachmentParams)
    log.info("Asset objects exported and attached to issue ${issueKey} successfully.")
    return "Asset objects exported and attached to issue ${issueKey} successfully."
} catch (Exception e) {
    log.error("Failed to attach file to issue: ${issueKey}", e)
    return "Failed to attach file to issue ${issueKey}: ${e.message}"
}

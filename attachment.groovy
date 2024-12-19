import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.issue.Issue
import com.atlassian.jira.issue.fields.CustomField
import com.atlassian.jira.issue.fields.CustomFieldManager
import com.atlassian.jira.issue.search.SearchProvider
import com.atlassian.jira.web.bean.PagerFilter
import com.atlassian.query.Query
import com.atlassian.query.QueryImpl
import com.atlassian.query.clause.TerminalClauseImpl
import com.atlassian.query.operand.SingleValueOperand
import com.atlassian.query.operator.Operator

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.PrintWriter

// Define the custom field name and issue key
def customFieldName = "Asset Custom Field"
def issueKey = "PROJECT-123"  // Replace with your specific issue key

// Get custom field manager and issue manager
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

// Attach the file to the issue
attachmentManager.createAttachment(new ByteArrayInputStream(fileData), fileName, "text/csv", user, issue)

return "Asset objects exported and attached to issue ${issueKey}."

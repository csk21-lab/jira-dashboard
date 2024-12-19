import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.issue.Issue
import com.atlassian.jira.issue.fields.CustomField
import com.atlassian.jira.issue.fields.CustomFieldManager
import com.atlassian.jira.issue.search.SearchProvider
import com.atlassian.jira.issue.search.SearchRequest
import com.atlassian.jira.issue.search.SearchResults
import com.atlassian.jira.util.csv.CsvWriter
import com.atlassian.query.Query
import com.atlassian.query.QueryImpl
import com.atlassian.query.operator.Operator
import com.atlassian.query.clause.TerminalClauseImpl
import com.atlassian.query.operand.SingleValueOperand

import java.io.ByteArrayOutputStream
import java.io.IOException

// Define the custom field ID or name
def customFieldName = "Asset Custom Field"

// Get custom field manager, issue manager, and search provider
CustomFieldManager customFieldManager = ComponentAccessor.getCustomFieldManager()
def issueManager = ComponentAccessor.getIssueManager()
def searchProvider = ComponentAccessor.getComponent(SearchProvider)

// Get the custom field object
CustomField customField = customFieldManager.getCustomFieldObjectByName(customFieldName)
if (!customField) {
    throw new IllegalArgumentException("Custom field with name '${customFieldName}' not found")
}

// Create a query to search for issues with the custom field populated
Query query = new QueryImpl(new TerminalClauseImpl(customField.getIdAsLong().toString(), Operator.IS_NOT, new SingleValueOperand("")))
SearchRequest searchRequest = new SearchRequest(query)

// Perform the search
SearchResults searchResults = searchProvider.search(searchRequest, ComponentAccessor.getJiraAuthenticationContext().getLoggedInUser(), PagerFilter.getUnlimitedFilter())

// Create an attachment manager
def attachmentManager = ComponentAccessor.getAttachmentManager()
def user = ComponentAccessor.getJiraAuthenticationContext().getLoggedInUser()

// Iterate through each issue and create a CSV file with the asset objects
for (Issue issue : searchResults.getIssues()) {
    def assetObjects = issue.getCustomFieldValue(customField)
    if (assetObjects) {
        // Create a CSV writer
        ByteArrayOutputStream baos = new ByteArrayOutputStream()
        CsvWriter csvWriter = new CsvWriter(baos, ',')

        // Write CSV headers
        csvWriter.writeField("Issue Key")
        csvWriter.writeField("Asset Object")
        csvWriter.endRecord()

        // Write CSV data
        for (def assetObject : assetObjects) {
            csvWriter.writeField(issue.getKey())
            csvWriter.writeField(assetObject.toString())
            csvWriter.endRecord()
        }

        // Close the CSV writer
        csvWriter.close()

        // Create an attachment file
        def fileName = "${issue.key}_asset_export.csv"
        def fileData = baos.toByteArray()

        // Attach the file to the specified issue
        attachmentManager.createAttachment(new ByteArrayInputStream(fileData), fileName, "text/csv", user, issue)
    }
}

return "Asset objects exported and attached to each issue."

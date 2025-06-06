import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.issue.AttachmentManager
import com.atlassian.jira.issue.Issue
import com.atlassian.jira.issue.IssueManager
import com.atlassian.jira.user.ApplicationUser

import java.nio.file.Files

// Config
def issueKey = "TEST-123" // Or use `issue` if inside post-function
def fileName = "ticket-summary.txt"

// Load issue and user
IssueManager issueManager = ComponentAccessor.issueManager
Issue issue = issueManager.getIssueObject(issueKey)
ApplicationUser user = ComponentAccessor.jiraAuthenticationContext.getLoggedInUser()

// Prepare file content
def content = """
Jira Ticket: ${issue.key}
Summary: ${issue.summary}
Description: ${issue.description ?: 'No description'}
Created: ${issue.created}
"""

// Write to temp file
def tempFile = File.createTempFile("jira-${issue.key}-", ".txt")
tempFile.write(content)
log.warn("Temp file created: ${tempFile.absolutePath}")

// Attach the file
AttachmentManager attachmentManager = ComponentAccessor.attachmentManager
def fileInputStream = new FileInputStream(tempFile)

attachmentManager.createAttachment(
    fileInputStream,
    tempFile.name,
    "text/plain",
    user,
    issue
)

fileInputStream.close()
tempFile.delete() // Clean up temp file

log.warn("Attachment successfully added to issue ${issue.key}")

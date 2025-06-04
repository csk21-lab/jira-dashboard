import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.issue.attachment.Attachment
import com.atlassian.jira.issue.attachment.AttachmentManager

// Set issueKey to null to use 'issue' if available (e.g., in post-function). Otherwise, set it to your issue key.
def issueKey = null // e.g. "PROJ-123" or null for current issue context

def issue
if (issueKey) {
    issue = ComponentAccessor.issueManager.getIssueByCurrentKey(issueKey)
    if (!issue) return "Issue with key ${issueKey} not found."
} else {
    // Use the 'issue' variable provided by the ScriptRunner context
    if (!binding.hasVariable('issue')) return "No 'issue' variable found in context. Please set issueKey."
    issue = issue
}

def attachmentManager = ComponentAccessor.getComponent(AttachmentManager)
def attachmentPathManager = ComponentAccessor.attachmentPathManager
def delegator = ComponentAccessor.getOfBizDelegator()

def renamedCount = 0

attachmentManager.getAttachments(issue).each { Attachment attachment ->
    if (attachment.filename?.toLowerCase()?.endsWith('.pdf') && !attachment.filename.toLowerCase().endsWith('_sample.pdf')) {
        def baseName = attachment.filename[0..-5] // strip '.pdf'
        def newFileName = baseName + '_sample.pdf'
        def file = attachmentPathManager.getAttachmentFile(attachment)
        if (!file?.exists()) {
            log.warn("File not found: ${attachment.filename}")
            return
        }
        def newFile = new File(file.parent, newFileName)
        if (!file.renameTo(newFile)) {
            log.warn("Could not rename file: ${file.absolutePath} to ${newFile.absolutePath}")
            return
        }
        // Update DB record
        attachment.filename = newFileName
        def record = delegator.findById("FileAttachment", attachment.id)
        record.set("filename", newFileName)
        delegator.store(record)
        renamedCount++
        log.info("Renamed attachment ${attachment.id} to ${newFileName}")
    }
}

return "Renamed ${renamedCount} PDF attachment(s) on issue ${issue.key}."

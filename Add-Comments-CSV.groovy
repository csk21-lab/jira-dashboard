import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.issue.IssueManager
import com.atlassian.jira.bc.issue.comment.CommentService
import com.atlassian.jira.user.ApplicationUser
import java.nio.file.Files
import java.nio.file.Paths

def commentService = ComponentAccessor.getComponent(CommentService)
def issueManager = ComponentAccessor.getIssueManager()
def userManager = ComponentAccessor.getUserManager()

// Path to your CSV file
def csvPath = "/path/to/your/file.csv"
def lines = Files.readAllLines(Paths.get(csvPath))

// Assuming CSV columns: ticketNumber,comment,author (with header row)
lines.drop(1).each { line ->
    def (ticketNumber, commentText, authorName) = line.split(',', 3)
    ticketNumber = ticketNumber?.trim()
    commentText = commentText?.trim()
    authorName = authorName?.trim()
    
    def issue = issueManager.getIssueByCurrentKey(ticketNumber)
    if (issue) {
        ApplicationUser author = userManager.getUserByName(authorName)
        if (author) {
            def commentParams = CommentService.CommentParameters.builder()
                .author(author)
                .body(commentText)
                .issue(issue)
                .build()
                
            // Validate and add the comment
            def validateResult = commentService.validateCommentCreate(author, commentParams)
            if (validateResult.isValid()) {
                commentService.create(author, validateResult, true)
            } else {
                log.warn "Validation failed for issue ${ticketNumber}: ${validateResult.errorCollection}"
            }
        } else {
            log.warn "Author '${authorName}' not found for issue ${ticketNumber}"
        }
    } else {
        log.warn "Issue with key '${ticketNumber}' not found."
    }
}

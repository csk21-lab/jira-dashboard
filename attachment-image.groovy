import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.mail.Email
import com.atlassian.mail.server.SMTPMailServer

def issue = issue
def mailServerManager = ComponentAccessor.getMailServerManager()
def mailServer = mailServerManager.getDefaultSMTPMailServer()

if (!mailServer) {
    log.warn("No SMTP mail server configured")
    return
}

// Set recipient(s)
def toAddress = "recipient@example.com" // Replace with your recipient

// Email subject
def subject = "Notification for issue ${issue.key}"

// Email body with image via public URL
def body = """
<html>
<body>
<p>Hello,</p>
<p>This is a notification for issue <b>${issue.key}</b>.</p>
<img src="https://yourserver.com/path/to/static-image.png" alt="Static Image" />
</body>
</html>
"""

def email = new Email(toAddress)
email.setSubject(subject)
email.setMimeType("text/html")
email.setBody(body)

mailServer.send(email)

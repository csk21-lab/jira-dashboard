import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.bc.issue.search.SearchService
import com.atlassian.jira.web.bean.PagerFilter
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.util.Base64
import org.apache.http.client.methods.HttpPost
import org.apache.http.entity.mime.MultipartEntityBuilder
import org.apache.http.impl.client.HttpClients
import org.apache.http.util.EntityUtils
import org.apache.http.entity.ContentType

// === CONFIGURATION ===
def issueKey = "ABC-123" // 🔁 Specific Jira issue key
def exportDirPath = "/var/atlassian/application-data/jira/exported-attachments" // 🔁 Existing directory path
def pageId = "123456" // 🔁 Confluence page ID
def confluenceBaseUrl = "https://confluence.example.com" // 🔁 Your Confluence base URL (no trailing slash)
def confluenceUser = "your-username" // 🔁 Confluence user
def confluenceToken = "your-api-token" // 🔁 API token (or password)

// === SETUP ===
def exportDir = Paths.get(exportDirPath)
if (!Files.exists(exportDir)) {
    Files.createDirectories(exportDir)
}

def authHeader = "Basic " + Base64.encoder.encodeToString("${confluenceUser}:${confluenceToken}".bytes)
def searchService = ComponentAccessor.getComponent(SearchService)
def jiraUser = ComponentAccessor.jiraAuthenticationContext.loggedInUser
def query = searchService.parseQuery(jiraUser, "issuekey = ${issueKey}").query
def results = searchService.search(jiraUser, query, PagerFilter.getUnlimitedFilter())
def attachmentManager = ComponentAccessor.getAttachmentManager()
def client = HttpClients.createDefault()

results.results.each { issue ->
    def attachments = attachmentManager.getAttachments(issue)
    attachments.each { att ->
        def inputStream = att.getContents()
        def fileName = "${issue.key}-${att.filename}"
        def tempFile = exportDir.resolve(fileName)

        Files.copy(inputStream, tempFile, StandardCopyOption.REPLACE_EXISTING)
        inputStream.close()

        // === Upload to Confluence ===
        def post = new HttpPost("${confluenceBaseUrl}/wiki/rest/api/content/${pageId}/child/attachment")
        post.setHeader("Authorization", authHeader)

        def builder = MultipartEntityBuilder.create()
        builder.addBinaryBody("file", tempFile.toFile(), ContentType.APPLICATION_OCTET_STREAM, tempFile.fileName.toString())
        builder.addTextBody("minorEdit", "true")
        builder.addTextBody("comment", "Uploaded from Jira issue ${issue.key}")

        post.setEntity(builder.build())

        def response = client.execute(post)
        def statusCode = response.statusLine.statusCode
        def responseText = EntityUtils.toString(response.entity)

        if (statusCode != 200 && statusCode != 201) {
            log.warn("❌ Upload failed: ${fileName} → $statusCode - $responseText")
        } else {
            log.warn("✅ Uploaded: ${fileName}")
        }

        response.close()
    }
}

log.warn("✔️ Export & upload complete. Files saved at: ${exportDir.toAbsolutePath()}")

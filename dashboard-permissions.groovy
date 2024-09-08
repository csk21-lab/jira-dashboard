import com.atlassian.jira.component.ComponentAccessor
import groovy.json.JsonSlurper
import groovy.json.JsonOutput
import com.atlassian.sal.api.net.Request
import com.atlassian.sal.api.net.RequestFactory

// Replace with your dashboard ID and group names
def dashboardId = 10001L // The ID of the dashboard to which permissions will be added
def groups = ["jira-software-users", "jira-administrators"] // List of groups to grant permissions to

def baseUrl = ComponentAccessor.getApplicationProperties().getString("jira.baseurl")
def restApiUrl = "${baseUrl}/rest/api/3/dashboard/${dashboardId}/permissions"

def requestFactory = ComponentAccessor.getComponent(RequestFactory)
def httpClient = requestFactory.createRequest(Request.Method.GET, restApiUrl)

// Set up authentication
def authHeader = "Basic " + "YOUR_ENCODED_CREDENTIALS" // Replace with your base64-encoded credentials
httpClient.addHeader("Authorization", authHeader)
httpClient.addHeader("Content-Type", "application/json")

// Fetch current permissions
def response = httpClient.execute()
if (response.status >= 200 && response.status < 300) {
    def currentPermissions = new JsonSlurper().parseText(response.getResponseBodyAsString())

    // Add new group permissions
    def newPermissions = currentPermissions.findAll { it.type != "group" } // Exclude existing group permissions
    groups.each { groupName ->
        def sharePermission = [
            type: "group",
            group: groupName,
            permissions: ["VIEW"]
        ]
        newPermissions.add(sharePermission)
    }

    // Update permissions via REST API
    def updateResponse = requestFactory.createRequest(Request.Method.PUT, restApiUrl)
    updateResponse.addHeader("Authorization", authHeader)
    updateResponse.addHeader("Content-Type", "application/json")
    updateResponse.setRequestBody(JsonOutput.toJson(newPermissions))

    def updateResult = updateResponse.execute()
    if (updateResult.status >= 200 && updateResult.status < 300) {
        log.warn("Successfully updated permissions for dashboard ID ${dashboardId}.")
    } else {
        log.warn("Failed to update permissions for dashboard ID ${dashboardId}. Status: ${updateResult.status}")
    }
} else {
    log.warn("Failed to fetch current permissions. Status: ${response.status}")
}

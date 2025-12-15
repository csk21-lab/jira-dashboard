import groovy.json.JsonBuilder
import groovyx.net.http.RESTClient

def jiraUrl = 'http://<your-jira-url>'
def groupName = 'your-group-name'
def userName = 'user-to-add'
def authString = 'admin:admin_password'.bytes.encodeBase64().toString()

def client = new RESTClient(jiraUrl)
client.headers['Authorization'] = "Basic ${authString}"
client.headers['Content-Type'] = 'application/json'

def payload = new JsonBuilder([name: userName]).toString()
def response = client.post(
    path: "/rest/api/2/group/user?groupname=${groupName}",
    body: payload
)

println response.status

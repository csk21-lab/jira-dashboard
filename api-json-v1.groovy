import groovy.json.JsonSlurper
import groovy.json.JsonOutput

def uniqueField = 'id' // Define the field to dedupe on
def apiOutput = []

(1..25).each { page ->
    def json = fetchPage(page)

    if (json.Devices instanceof List) {
        apiOutput.addAll(json.Devices)
    } else if (json.Devices instanceof Map) {
        apiOutput.add(json.Devices)
    }
}

// Remove duplicates based on uniqueField
apiOutput = apiOutput.unique { it[uniqueField] }  

// Write the result to a file
new File('/tmp/api-output.json').text = JsonOutput.toJson(apiOutput)

def fetchPage(int page) {
    // Simulating an API call that returns a JSON response
    // Replace this with your actual logic to fetch the page
    return new JsonSlurper().parseText('{ "Devices": [{ "id": "1" }, { "id": "2" }, { "id": "2" }] }')
}
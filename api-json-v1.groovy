import groovy.json.JsonSlurper
import groovy.json.JsonBuilder

import java.net.URL
import java.net.HttpURLConnection

def apiBaseUrl = "https://abc.com/your/api/search?page="
def apiKey = "1234"
def basicAuth = "Basic abcdefgh"
def outputFile = new File("/tmp/api-output.json") // Change path as needed

def allResults = []

for (int page = 1; page <= 25; page++) {
    def apiUrl = "${apiBaseUrl}${page}"
    def connection = (HttpURLConnection) new URL(apiUrl).openConnection()
    connection.setRequestMethod("GET")
    connection.setRequestProperty("Authorization", basicAuth)
    connection.setRequestProperty("Accept", "application/json")
    connection.setRequestProperty("abc-tenant-code", apiKey)
    connection.connect()

    def responseCode = connection.responseCode
    def responseText

    if (responseCode == 200) {
        responseText = connection.inputStream.text
        if (responseText && responseText.trim().startsWith('{')) {
            try {
                def json = new JsonSlurper().parseText(responseText)

                // Adjust this depending on your API's JSON structure
                if (json.results) {
                    allResults.addAll(json.results)
                } else {
                    allResults.add(json)
                }
            } catch (Exception e) {
                println "Failed to parse JSON on page $page: ${e.message}"
            }
        } else {
            println "Response on page $page is not valid JSON:\n$responseText"
        }
    } else {
        responseText = connection.errorStream?.text
        println "HTTP error on page $page: $responseCode"
        println "Response: $responseText"
    }
}

// --- DEDUPLICATION STEP ---
// Remove duplicates based on a unique key (e.g. 'id').
// Change 'id' to whatever field uniquely identifies your records.
def dedupedResults = allResults.unique { it.id }

// If you need to base uniqueness on multiple fields, use:
// def dedupedResults = allResults.unique { "${it.id}::${it.name}" }

// Or to dedupe by full map equality, simply:
// def dedupedResults = allResults.unique()

// Save all combined, de-duplicated results to a single file
outputFile.text = new JsonBuilder(dedupedResults).toPrettyString()
println "Combined JSON output saved to: ${outputFile.absolutePath}"
println "Total records before de-dup: ${allResults.size()}"
println "Total records after de-dup: ${dedupedResults.size()}"

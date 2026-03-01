import groovy.json.JsonSlurper
import groovy.json.JsonBuilder
import java.net.URL
import java.net.HttpURLConnection

def apiBaseUrl = "https://abc.com/your/api/search?page="
def apiKey = "1234"
def basicAuth = "Basic abcdefgh"

def outputFile = new File("/tmp/api-output.json")
def stateFile  = new File("/tmp/api-output.state.json")   // stores lastProcessedPage

def maxPage = 25

// ---------- load state ----------
def slurper = new JsonSlurper()
def state = [ lastProcessedPage: 0 ]
if (stateFile.exists() && stateFile.text.trim()) {
    try {
        state = slurper.parseText(stateFile.text) as Map
    } catch (ignored) {
        // if state file is corrupted, start over
        state = [ lastProcessedPage: 0 ]
    }
}

int startPage = (state.lastProcessedPage ?: 0) as int
startPage = startPage + 1

if (startPage > maxPage) {
    println "Nothing to do. lastProcessedPage=${state.lastProcessedPage}, maxPage=${maxPage}"
    return
}

// ---------- load existing output so we append (not overwrite) ----------
def allResults = []
if (outputFile.exists() && outputFile.text.trim()) {
    try {
        def existing = slurper.parseText(outputFile.text)
        if (existing instanceof List) allResults.addAll(existing)
        else allResults.add(existing)
    } catch (e) {
        println "Warning: could not parse existing output file, starting fresh: ${e.message}"
    }
}

// OPTIONAL: if you have a stable unique key, dedupe by it (best)
// Change 'id' to whatever unique field you have (e.g., recordId, uuid, etc.)
def uniqueKey = "id"
def seenIds = new HashSet()
allResults.each { r ->
    if (r instanceof Map && r[uniqueKey] != null) seenIds.add(r[uniqueKey].toString())
}

// ---------- fetch only new pages ----------
for (int page = startPage; page <= maxPage; page++) {
    def apiUrl = "${apiBaseUrl}${page}"
    def connection = (HttpURLConnection) new URL(apiUrl).openConnection()
    connection.setRequestMethod("GET")
    connection.setRequestProperty("Authorization", basicAuth)
    connection.setRequestProperty("Accept", "application/json")
    connection.setRequestProperty("abc-tenant-code", apiKey)
    connection.connect()

    def responseCode = connection.responseCode

    if (responseCode == 200) {
        def responseText = connection.inputStream.text
        if (responseText && responseText.trim().startsWith('{')) {
            try {
                def json = slurper.parseText(responseText)

                def pageResults = []
                if (json.results instanceof List) pageResults = json.results
                else if (json.results != null) pageResults = [json.results]
                else pageResults = [json]

                // append with dedupe (if possible)
                pageResults.each { rec ->
                    if (rec instanceof Map && rec[uniqueKey] != null) {
                        def id = rec[uniqueKey].toString()
                        if (!seenIds.contains(id)) {
                            allResults.add(rec)
                            seenIds.add(id)
                        }
                    } else {
                        // no key to dedupe; just append
                        allResults.add(rec)
                    }
                }

                // save progress after each successful page
                state.lastProcessedPage = page
                stateFile.text = new JsonBuilder(state).toPrettyString()

                // write output incrementally too (safer)
                outputFile.text = new JsonBuilder(allResults).toPrettyString()

                println "Fetched page ${page}, total records now: ${allResults.size()}"
            } catch (Exception e) {
                println "Failed to parse JSON on page $page: ${e.message}"
                break
            }
        } else {
            println "Response on page $page is not valid JSON:\n$responseText"
            break
        }
    } else {
        def errorText = connection.errorStream?.text
        println "HTTP error on page $page: $responseCode"
        println "Response: $errorText"
        break
    }
}

println "Combined JSON output saved to: ${outputFile.absolutePath}"
println "State saved to: ${stateFile.absolutePath}"

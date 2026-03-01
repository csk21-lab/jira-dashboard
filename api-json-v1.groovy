import groovy.json.JsonSlurper
import groovy.json.JsonBuilder
import java.net.URL
import java.net.HttpURLConnection

def apiBaseUrl = "https://abc.com/your/api/search?page="
def apiKey = "1234"
def basicAuth = "Basic abcdefgh"

def outputFile = new File("/tmp/api-output.json")
def pagesFile  = new File("/tmp/api-output.pages.json")  // stores processed pages

def maxPage = 25
def slurper = new JsonSlurper()

// 1) Load previously processed pages
def processedPages = new HashSet<Integer>()
if (pagesFile.exists() && pagesFile.text.trim()) {
    try {
        def arr = slurper.parseText(pagesFile.text)
        if (arr instanceof List) arr.each { processedPages.add((it as Number).intValue()) }
    } catch (e) {
        println "Warning: couldn't parse pages file, starting with none processed: ${e.message}"
    }
}

// 2) Load existing output so we append (not overwrite)
def allResults = []
if (outputFile.exists() && outputFile.text.trim()) {
    try {
        def existing = slurper.parseText(outputFile.text)
        if (existing instanceof List) allResults.addAll(existing)
        else allResults.add(existing)
    } catch (e) {
        println "Warning: couldn't parse existing output file; starting fresh: ${e.message}"
    }
}

// 3) Process only pages not yet processed
for (int page = 1; page <= maxPage; page++) {
    if (processedPages.contains(page)) {
        println "Skipping page ${page} (already written earlier)"
        continue
    }

    def apiUrl = "${apiBaseUrl}${page}"
    def connection = (HttpURLConnection) new URL(apiUrl).openConnection()
    connection.setRequestMethod("GET")
    connection.setRequestProperty("Authorization", basicAuth)
    connection.setRequestProperty("Accept", "application/json")
    connection.setRequestProperty("abc-tenant-code", apiKey)
    connection.connect()

    if (connection.responseCode != 200) {
        println "HTTP error on page $page: ${connection.responseCode}"
        println "Response: ${connection.errorStream?.text}"
        break
    }

    def responseText = connection.inputStream.text
    def json = slurper.parseText(responseText)

    def pageResults = []
    if (json.results instanceof List) pageResults = json.results
    else if (json.results != null) pageResults = [json.results]
    else pageResults = [json]

    // append the page output once
    allResults.addAll(pageResults)

    // mark page as processed only after success
    processedPages.add(page)

    // persist after each page (safer if the script is interrupted)
    outputFile.text = new JsonBuilder(allResults).toPrettyString()
    pagesFile.text  = new JsonBuilder((processedPages as List).sort()).toPrettyString()

    println "Wrote page ${page} (added ${pageResults.size()} records). Total records: ${allResults.size()}"
}

println "Saved combined output: ${outputFile.absolutePath}"
println "Saved processed pages: ${pagesFile.absolutePath}"

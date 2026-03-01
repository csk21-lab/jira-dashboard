import groovy.json.JsonSlurper
import groovy.json.JsonBuilder
import groovy.json.JsonOutput

import java.net.URL
import java.net.HttpURLConnection
import java.security.MessageDigest

def apiBaseUrl = "https://abc.com/your/api/search?page="   // if your base URL already has '?', use &page= (see notes below)
def apiKey = "1234"
def basicAuth = "Basic abcdefgh"
def outputFile = new File("/tmp/api-output.json") // Change path as needed

// If you have a true unique id field, set it here (recommended).
// Example: "id", "recordId", "uuid".
// Leave as null to use fingerprint-based dedupe.
def uniqueField = null  // e.g. "id"

// ---------------- helpers ----------------
String sha256(String s) {
    def md = MessageDigest.getInstance("SHA-256")
    md.digest(s.getBytes("UTF-8")).collect { String.format("%02x", it) }.join()
}

String stableJson(def obj) {
    // Stable JSON with sorted Map keys so hashing is consistent
    if (obj == null) return "null"

    if (obj instanceof Map) {
        def keys = (obj as Map).keySet().collect { it.toString() }.sort()
        def parts = keys.collect { k ->
            def v = (obj as Map)[k]
            "\"${k.replace('\\','\\\\').replace('"','\\"')}\":${stableJson(v)}"
        }
        return "{${parts.join(',')}}"
    }
    if (obj instanceof List) {
        return "[" + (obj as List).collect { stableJson(it) }.join(",") + "]"
    }
    if (obj instanceof Number || obj instanceof Boolean) return obj.toString()
    def s = obj.toString()
    s = s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t")
    return "\"${s}\""
}

String fingerprint(def rec) {
    return sha256(stableJson(rec))
}

// ---------------- main ----------------
def slurper = new JsonSlurper()
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
    if (responseCode == 200) {
        def responseText = connection.inputStream.text

        if (responseText && responseText.trim().startsWith('{')) {
            try {
                def json = slurper.parseText(responseText)

                // Adjust based on your API response shape
                if (json.results instanceof List) {
                    allResults.addAll(json.results)
                } else if (json.results != null) {
                    allResults.add(json.results)
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
        def err = connection.errorStream?.text
        println "HTTP error on page $page: $responseCode"
        println "Response: $err"
    }
}

// ---------------- dedupe ----------------
def deduped = []
def seen = new HashSet<String>()

allResults.each { rec ->
    String key

    if (uniqueField != null && (rec instanceof Map) && rec[uniqueField] != null) {
        key = rec[uniqueField].toString()
    } else {
        // fallback to content fingerprint
        key = fingerprint(rec)
    }

    if (seen.add(key)) {
        deduped.add(rec)
    }
}

println "Fetched records: ${allResults.size()}"
println "After dedupe:    ${deduped.size()}"

// Save deduped results
outputFile.text = new JsonBuilder(deduped).toPrettyString()
println "Deduped JSON output saved to: ${outputFile.absolutePath}"

/*
NOTES (important if you still see duplicates 25 times):
- If the API returns the same data for every page, dedupe will reduce duplicates,
  but you still won't get real paging. Make sure the endpoint truly supports `page=`.
- If your base URL already contains '?', do NOT use '?page=' again. Use '&page='.
  Example:
    apiBaseUrl = "https://abc.com/your/api/search?query=foo&page="
  or build URLs like:
    sep = apiBaseUrl.contains('?') ? '&' : '?'
    apiUrl = "${apiBaseUrl}${sep}page=${page}"
*/

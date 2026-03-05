import groovy.json.JsonBuilder
import groovy.json.JsonSlurper
import java.net.URL
import java.net.HttpURLConnection

def apiBaseUrl = "https://abc.com/your/api/search?"   // <-- your base
def apiKey     = "1234"
def basicAuth  = "Basic abcdefgh"

def maxPages  = 25
def pageSize  = 500   // your response shows PageSize=500; include if the API supports it

// Unique: DeviceFriendlyName -> device
def uniqueDevices = new LinkedHashMap<String, Map>()

// helper: safely append query params
String buildUrl(String base, Map params) {
    def sep = base.contains("?") ? (base.endsWith("?") || base.endsWith("&") ? "" : "&") : "?"
    def qs = params.collect { k, v -> "${URLEncoder.encode(k.toString(), 'UTF-8')}=${URLEncoder.encode(v.toString(), 'UTF-8')}" }
                   .join("&")
    return base + sep + qs
}

for (int page = 1; page <= maxPages; page++) {

    def urlStr = buildUrl(apiBaseUrl, [page: page, pageSize: pageSize])  // if pageSize is not supported, remove it
    def url = new URL(urlStr)

    HttpURLConnection conn = (HttpURLConnection) url.openConnection()

    try {
        conn.setRequestMethod("GET")
        conn.setConnectTimeout(15000)
        conn.setReadTimeout(60000)

        conn.setRequestProperty("Authorization", basicAuth)
        conn.setRequestProperty("Accept", "application/json")
        conn.setRequestProperty("abc-tenant-code", apiKey)

        int code = conn.responseCode
        def body = (code >= 200 && code < 300) ?
                conn.inputStream?.getText("UTF-8") :
                conn.errorStream?.getText("UTF-8")

        log.warn("Fetching page=${page} url=${url} HTTP=${code}")

        if (!(code >= 200 && code < 300)) {
            log.warn("HTTP error on page=${page}. Body: ${body?.take(800)}")
            break
        }

        def json = new JsonSlurper().parseText(body)
        def devices = (json?.Devices ?: []) as List

        if (devices.isEmpty()) {
            log.warn("No devices returned on page=${page}. Stopping early.")
            break
        }

        int before = uniqueDevices.size()
        int dupOnPage = 0
        int missingKey = 0

        devices.each { Map d ->
            def key = d?.DeviceFriendlyName?.toString()?.trim()
            if (!key) { missingKey++; return }

            if (uniqueDevices.containsKey(key)) {
                dupOnPage++
                // keep first occurrence; to keep latest instead:
                // uniqueDevices[key] = d
            } else {
                uniqueDevices[key] = d
            }
        }

        int after = uniqueDevices.size()
        log.warn("Page=${page} devices=${devices.size()} addedUnique=${after - before} dupOnPage=${dupOnPage} missingKey=${missingKey} uniqueTotal=${after}")

        // sanity check: confirms API is actually returning requested page
        if (json?.Page && (json.Page as int) != page) {
            log.warn("WARNING: API returned Page=${json.Page} but requested page=${page}. Your paging params might be different.")
        }

    } finally {
        conn?.disconnect()
    }
}

log.warn("DONE. Unique devices total=${uniqueDevices.size()}")

def output = [Count: uniqueDevices.size(), Devices: uniqueDevices.values().toList()]
def pretty = new JsonBuilder(output).toPrettyString()

// Optional: write to node-local file
// new File("/tmp/devices-unique.json").text = pretty

return pretty

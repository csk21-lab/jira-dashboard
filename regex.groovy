def text = "DEV - Development"

// Use a regex pattern that captures everything before the dash (with optional spaces around it)
def pattern = /^(.*?)\s*-\s*/
def matcher = text =~ pattern

if (matcher.find()) {
    def extracted = matcher.group(1)
    println "Extracted text: ${extracted}"  // Should print "DEV"
}


def text = "DEV - Development"

// Split the string on the dash and trim the result to remove extra whitespace
def extracted = text.split('-')[0].trim()
println "Extracted text: ${extracted}"
log.info("Extracted text: ${extracted}")

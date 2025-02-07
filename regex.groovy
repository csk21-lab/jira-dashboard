def text = "DEV - Development"

// Use a regex pattern that captures everything before the dash (with optional spaces around it)
def pattern = /^(.*?)\s*-\s*/
def matcher = text =~ pattern

if (matcher.find()) {
    def extracted = matcher.group(1)
    println "Extracted text: ${extracted}"  // Should print "DEV"
}

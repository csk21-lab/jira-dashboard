/**
 * Checks if a string value needs to be quoted for CSV output.
 * Returns true if the string contains special CSV characters.
 */
boolean needsQuotes(String s) {
    s != null && (s.contains(',') || s.contains('"') || s.contains('\n') || s.contains('\r'))
}

/**
 * Converts any value to a CSV-safe string.
 * If quoting is needed, wraps the value in quotes and escapes inner quotes.
 */
String csvCell(Object v) {
    if (v == null) return ''
    String s = v.toString()
    if (needsQuotes(s)) {
        // Escape inner quotes by doubling them, per CSV rules
        return '"' + s.replace('"','""') + '"'
    }
    return s
}

// Example usage for a summary field from a Jira issue:
def summary = issue.summary  // Replace with your source of summary
def csvSummary = csvCell(summary)
// Use csvSummary in your CSV output

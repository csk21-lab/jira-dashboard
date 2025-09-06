boolean needsQuotes(String s) {
    s != null && (s.indexOf(',') >= 0 || s.indexOf('"') >= 0 || s.indexOf('\n') >= 0 || s.indexOf('\r') >= 0)
}

String csvCell(Object v) {
    if (v == null) return ''
    String s = v.toString()
    if (needsQuotes(s)) {
        return '"' + s.replace('"','""') + '"'   // wrap in quotes
    }
    return s
}

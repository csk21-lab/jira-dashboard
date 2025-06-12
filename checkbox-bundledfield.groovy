import com.atlassian.jira.component.ComponentAccessor
import groovy.json.JsonSlurper
import org.apache.log4j.Logger
import org.apache.log4j.Level

def log = Logger.getLogger("com.onresolve.scriptrunner.runner.ScriptRunnerImpl")
log.setLevel(Level.DEBUG)

// Configurable section
String issueKey = 'WORK-27'
String bundledFieldId = "customfield_10300"
String fieldName = "checkbox field" // <-- Change this to your checkbox field name
Integer row = 0

def issueManager = ComponentAccessor.getIssueManager()
def customFieldManager = ComponentAccessor.getCustomFieldManager()
def cfBundledFields = customFieldManager.getCustomFieldObject(bundledFieldId)
def issue = issueManager.getIssueObject(issueKey)

String cfValue = issue.getCustomFieldValue(cfBundledFields)
Object jsonValue = new JsonSlurper().parseText(cfValue)

List fields = getFieldsForRow(row, jsonValue)
if (!fields) {
    log.warn("No fields found for row $row")
    return null
}
Map field = getFieldByName(fieldName, fields)
if (!field) {
    log.warn("No field named '$fieldName' found in row $row")
    return null
}
def value = getFieldValue(field)
log.info("Checkbox field '${fieldName}' value is '${value}'")
return value

// --- Helper Methods ---

def String getFieldValue(Map field) {
    if (!field) return null
    def type = field.type
    if (type == 'checkbox') {
        return getCheckboxValues(field)
    } else if (type == 'select') {
        return getOptionValue(field)
    }
    return field.value
}

def String getCheckboxValues(Map field) {
    // field.value: comma-separated option IDs, e.g. "10100,10101"
    // field.options: list of option maps, each with id and name
    if (!field.value) return ""
    List<Map> allOptions = field.options instanceof List ? field.options : []
    List<String> selectedIds = field.value.split(',').collect { it.trim() }
    List<String> selectedNames = allOptions.findAll { opt -> selectedIds.contains(opt.id) }
                                        .collect { it.name }
    return selectedNames.join(', ')
}

def String getOptionValue(Map field) {
    // For select fields
    List<Map> allOptions = (field.options instanceof List) ? (List<Map>) field.options : []
    String val = field.value ?: ""
    String[] selectedIds = val.split(',').collect { it.trim() }
    List<Map> selectedOptions = allOptions.findAll { opt -> selectedIds.contains(opt.id) }
    return selectedOptions.collect { it.name }.join(', ')
}

def Map getFieldByName(String fieldName, List<Map> fields) {
    if (!fields) return null
    return fields.find { it.name == fieldName }
}

def List getFieldsForRow(Integer row, Object json) {
    if (!json) return null
    // Handle both List and Map structures for bundled fields
    if (json instanceof List) {
        if (row < json.size() && json[row] instanceof Map && json[row].containsKey("fields")) {
            return json[row].fields
        }
    } else if (json instanceof Map) {
        int i = 0
        for (entry in json) {
            def v = entry.value
            if (i == row && v instanceof Map && v.containsKey("fields")) {
                return v.fields
            }
            i++
        }
    }
    return null
}

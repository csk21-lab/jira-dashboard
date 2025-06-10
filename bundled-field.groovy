import com.atlassian.jira.component.ComponentAccessor
import groovy.json.JsonSlurper
import org.apache.log4j.Logger
import org.apache.log4j.Level

def log = Logger.getLogger("com.onresolve.scriptrunner.runner.ScriptRunnerImpl")
log.setLevel(Level.DEBUG)

// Key of issue where the bundledfields value is
String issueKey = 'WORK-27'
// ID of bundledfield where we are looking for value
String bundledFieldId = "customfield_10300"
// Field column in bundledfield
String fieldName = "select field"
// Row in bundledfield
Integer row = 0

def issueManager = ComponentAccessor.getIssueManager()
def customFieldManager = ComponentAccessor.getCustomFieldManager()
def cfBundledFields = customFieldManager.getCustomFieldObject(bundledFieldId)
def issue = issueManager.getIssueObject(issueKey)

String cfValue = issue.getCustomFieldValue(cfBundledFields)
Object jsonValue = new JsonSlurper().parseText(cfValue)

// Retrieve a value of requested row and field name
List fields = getFieldsForRow(row, (Map) jsonValue)
Map field = getFieldByName(fieldName, fields)
def value = getFieldValue(field)
log.info("my subField ${fieldName} value is ${value}")
value

def String getFieldValue(Map field) {
    def type = field.type
    if (type == 'select' || type == 'checkbox') {
        return getOptionValue(field)
    }
    return field.value
}

def String getOptionValue(Map field) {
    List<Map> allOptions = (List<Map>) field.options
    String val = field.value
    String[] selectedIds = val.split(',')
    List<Map> selectedOptions = allOptions.findAll { it.id in selectedIds }
    return selectedOptions.name.join(', ')
}

def Map getFieldByName(fieldName, List<Map> fields) {
    return fields.find { it.name == fieldName }
}

def List getFieldsForRow(row, Map json) {
    List fields = null
    int i = 0
    json.each { k, v ->
        if (i == row) {
            fields = ((Map) v).get("fields")
        }
        i++
    }
    return fields
}

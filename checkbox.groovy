import com.onresolve.jira.groovy.user.FieldBehaviours
import static com.atlassian.jira.issue.IssueFieldConstants.*
import groovy.transform.BaseScript

@BaseScript FieldBehaviours fieldBehaviours

def checkboxField = getFieldById(fieldChanged)

def selectedOptions = checkboxField.value as List

if (selectedOptions && selectedOptions.size() > 1) {
    checkboxField.setError("Please select only one option.")
} else {
    checkboxField.clearError()
}

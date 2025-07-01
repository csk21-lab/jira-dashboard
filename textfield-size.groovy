import com.atlassian.jira.component.ComponentAccessor

// Get the custom field manager
def customFieldManager = ComponentAccessor.getCustomFieldManager()

// Replace with your custom field ID
def textFieldId = "customfield_XXXXX"  // Replace with your field ID

// Get the custom field object
def textField = customFieldManager.getCustomFieldObject(textFieldId)

// Get the value of the text field from the issue
def fieldValue = issue.getCustomFieldValue(textField)

if (fieldValue && fieldValue.toString().length() >= 200) {
    // If the field has at least 200 characters, validation passes
    return true
} else {
    // If the field has fewer than 200 characters, validation fails
    return false
}

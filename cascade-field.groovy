// Get the custom field object
def cascadingField = getFieldByName("CustomFieldName")

// Get the value (as a Map: ["parent": ..., "child": ...])
def cascadingValue = cascadingField.getValue()

// Extract parent and child
def parentValue = cascadingValue?.get("parent")
def childValue = cascadingValue?.get("child")

// Example usage: log values
log.info("Parent Value: ${parentValue}")
log.info("Child Value: ${childValue}")

// You can also use them in your behaviour logic, e.g. conditionally set other fields
if (parentValue == "SomeParent" && childValue == "SomeChild") {
    // Do something
}

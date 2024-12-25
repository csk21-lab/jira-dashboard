if(issueCount > 0 ){
    searchResults.issues.each { issue ->
        def existingValues = issue.getCustomFieldValue(ComponentAccessor.customFieldManager.getCustomFieldObjectByName('Multiline Custom Field Name'))?.toString() ?: ""
        def newValues = addUniqueValues(existingValues, objectKeyCVEID)
        issue.setCustomFieldValue(ComponentAccessor.customFieldManager.getCustomFieldObjectByName('Multiline Custom Field Name'), newValues)
        ComponentAccessor.issueManager.updateIssue(loggedInUser, issue, EventDispatchOption.ISSUE_UPDATED, false)
    }    
}

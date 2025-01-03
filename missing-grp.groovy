// Ensure that all names are in the same case for comparison
def jiraGroupsNormalized = groupManager.getAllGroups().collect { it.getName().toLowerCase() }
log.warn("Normalized JIRA Groups: ${jiraGroupsNormalized.join(', ')}")

def groupsExistInJira = missingGroups.findAll { missingGroup ->
    // Normalize the missing group for a case-insensitive comparison
    def exists = jiraGroupsNormalized.contains(missingGroup.toLowerCase())
    log.warn("Checking if missing group '${missingGroup}' exists in JIRA groups: ${exists}")
    exists
}

// Output the results of the check
if (!groupsExistInJira.isEmpty()) {
    log.warn("Groups existing in JIRA that are missing correct mappings: ${groupsExistInJira.join(', ')}")
} else {
    log.warn("No common groups in JIRA")
}

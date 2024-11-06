import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.user.ApplicationUser

def FILE_PATH = "/Downloads/issues.csv"
File file = new File(FILE_PATH)

if (!file.exists()) {
    log.warn "File does not exist at $FILE_PATH"
    return false
}

def userManager = ComponentAccessor.userManager
def groupManager = ComponentAccessor.groupManager

file.eachLine { eachLine ->
    List<String> usersAndGroups = eachLine?.split(";")
    
    if (usersAndGroups?.size() < 2) {
        log.warn "Invalid line format: expected user and groups, but got: $eachLine"
        return
    }
    
    String user = usersAndGroups[0]?.trim()
    String groupsString = usersAndGroups[1]?.replaceAll("\\[", "")?.replaceAll("]", "")?.trim()
    
    if (user && groupsString) {
        ApplicationUser applicationUser = userManager.getUserByName(user)
        
        // Skip adding groups if the user's directory ID is 1
        if (applicationUser && applicationUser.isActive()) {
            if (applicationUser.directoryId == 1) {
                log.warn "Skipping group addition for user '$user' in directory ID 1"
                return
            }
            
            List<String> groupNames = groupsString.split(",").collect { it.trim() }
            if (!groupNames.isEmpty()) {
                groupNames.each { groupName ->
                    // Skip group addition if the group name is "jira-developer"
                    if (groupName != "jira-developer") {
                        def group = groupManager.getGroup(groupName)
                        if (group) {
                            def isUserExistsInGroup = groupManager.isUserInGroup(applicationUser, group)
                            if (!isUserExistsInGroup) {
                                groupManager.addUserToGroup(applicationUser, group)
                            }
                        } else {
                            log.warn "Group '$groupName' not found for user '$user'"
                        }
                    } else {
                        log.warn "Skipping addition to group 'jira-developer' for user '$user'"
                    }
                }
            } else {
                log.warn "No valid groups found in the file for user '$user'"
            }
        } else {
            log.warn "User '$user' is either not found or inactive"
        }
    } else {
        log.warn "User or groups information missing in line: $eachLine"
    }
}

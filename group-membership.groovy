import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.user.ApplicationUser

def FILE_PATH = "/Downloads/issues.csv"

File file = new File(FILE_PATH)

if(!file.exists()){
    log.warn "File not exists"
    return false
}
def userManager = ComponentAccessor.userManager
def groupManager = ComponentAccessor.groupManager

file.eachLine { eachLine ->

    List<String> usersAndGroups = eachLine?.split(";")
    String user = null
    String groupsString = null
    if(!usersAndGroups?.isEmpty()){
        user = usersAndGroups?.get(0)?.trim()
    }

    if(!usersAndGroups?.isEmpty() && usersAndGroups.size() > 0){
        groupsString = usersAndGroups?.get(1)?.replaceAll("\\[", "")?.replaceAll("]", "")
    }

    if(user && !groupsString?.trim()?.isEmpty()){
        ApplicationUser applicationUser = userManager.getUserByName(user)
        if(applicationUser && applicationUser.isActive()){
           List<String> groupNames =  groupsString?.split(",")
            if(!groupNames.isEmpty()){
                groupNames.each { groupName ->
                    def group = groupManager.getGroup(groupName)
                    if(group){
                        def isUserExistsInGroup = groupManager.isUserInGroup(applicationUser, groupName)
                        if(!isUserExistsInGroup){
                            groupManager.addUserToGroup(applicationUser, group)
                        }
                    } else {
                        log.warn "$groupName not found"
                    }
                }
            } else {
                log.warn "No Groups found in the file"
            }
        } else {
            log.warn "$user is not found"
        }
    }
}

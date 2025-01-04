import com.atlassian.jira.security.roles.ProjectRoleActor
import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.project.ProjectManager
import com.atlassian.jira.security.roles.ProjectRoleManager
import com.atlassian.jira.user.ApplicationUser

def projectManager = ComponentAccessor.getProjectManager()
def projectRoleManager = ComponentAccessor.getComponent(ProjectRoleManager)
def groupManager = ComponentAccessor.getGroupManager()

// Define the desired project key and a list of role names
def projectKey = "AAA"  // Replace with your project key
def roleNames = ["Developers", "Administrators", "Project Manager"]  // Replace with actual role names
def specialgroups = ["jira-administrators", "jira-software-users","jira-system-administrators"]

// Get the project object by key
def project = projectManager.getProjectObjByKey(projectKey)
if (!project) {
    log.error("Project not found: $projectKey")
    return
}

def users = [] //as Set<ApplicationUser>  // Use a set to avoid duplicates

roleNames.each { roleName ->
    log.info("Processing role: $roleName")
    // Get the project role by name 
        def role = projectRoleManager.getProjectRole(roleName)
        if (!role){
            log.error("Role not found: ${roleName}")
            
        }else{
            def projectRoleActors = projectRoleManager.getProjectRoleActors(role, project) 
            projectRoleActors.getRoleActorsByType(ProjectRoleActor.USER_ROLE_ACTOR_TYPE).each { userInRole ->
                // Add individual users
                def member = userInRole.getUser()
                log.warn("member : ${member}")
                if (users.add(member.username)){
                    // log.warn("user : ${member.displayName} (username : ${member.username}, Role : ${role}")

                }
            }
            // Add users from groups
            projectRoleActors.getRoleActorsByType(ProjectRoleActor.GROUP_ROLE_ACTOR_TYPE).each { groupRoleActor ->
               // log.warn("groupRoleActor : ${groupRoleActor}")
                def groupName = groupRoleActor.getGroup().getName()
                if(!specialgroups.contains(groupName)){
                    def groupUsers = groupManager.getUsersInGroup(groupName)
                    log.warn("Role : ${roleName} - GroupName : ${groupName} - users : ${groupUsers*.username}")
                    groupUsers.each { member ->
                        if (users.add(member.username)){
                           // log.warn("user : ${member.displayName} Username : ${member.username}, Role : ${role} via Group : ${groupName}")
                        }
                    }
                }
            }

        }        
}
return ("users : ${users.join(' ,')}")


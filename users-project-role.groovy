import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.project.ProjectManager
import com.atlassian.jira.security.roles.ProjectRoleManager
import com.atlassian.jira.user.ApplicationUser

def projectManager = ComponentAccessor.getProjectManager()
def projectRoleManager = ComponentAccessor.getComponent(ProjectRoleManager)
def groupManager = ComponentAccessor.getGroupManager()

// Define the desired project key and role name
def projectKey = "PROJECT_KEY" // Replace with your project key
def roleName = "ROLE_NAME" // Replace with your role name

// Get the project object by key
def project = projectManager.getProjectObjByKey(projectKey)
if (!project) {
    log.error("Project not found: ${projectKey}")
    return
}

// Get the project role by name
def role = projectRoleManager.getProjectRole(roleName)
if (!role) {
    log.error("Role not found: ${roleName}")
    return
}

// Get users mapped to the role
def projectRoleActors = projectRoleManager.getProjectRoleActors(role, project)
def users = [] as Set<ApplicationUser> // Use a set to avoid duplicates

// Add individual users
users.addAll(projectRoleActors.getRoleActorsByType(ProjectRoleActor.USER_ROLE_ACTOR_TYPE).collect { it.getUser() })

// Add users from groups
projectRoleActors.getRoleActorsByType(ProjectRoleActor.GROUP_ROLE_ACTOR_TYPE).each { groupRoleActor ->
    def groupName = groupRoleActor.getGroup().getName()
    def groupUsers = groupManager.getUsersInGroup(groupName)
    users.addAll(groupUsers)
}

// Print the list of users
users.each { user ->
    log.info("User: ${user.displayName} (Username: ${user.username})")
}

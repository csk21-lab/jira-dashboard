import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.security.roles.ProjectRoleManager
import com.atlassian.jira.security.roles.ProjectRoleActor
import com.atlassian.jira.security.groups.GroupManager
import com.atlassian.jira.user.util.UserManager
import com.atlassian.jira.project.Project  // Import Project class

String groupnamesinRole
String allusersinRole
String result = ""
String finalResult = ""
def userList = []
def rolesMissingGroup = []  // List to store roles with missing groups

// Specify the project keys you want to check
def projectkeys = ["ABC", "DEF"]
projectkeys.each { projectKey ->
    // Get the project using the Project Manager
    Project project = ComponentAccessor.getProjectManager().getProjectByKey(projectKey)
    ProjectRoleManager projectRoleManager = ComponentAccessor.getComponent(ProjectRoleManager)
    GroupManager groupManager = ComponentAccessor.getGroupManager()
    UserManager userManager = ComponentAccessor.getUserManager() as UserManager
    def groupRoles = projectRoleManager.getProjectRoles(project)

    result += "Project Name: " + project.name + "\r"
    result += "Project Key: " + project.key + "\r\n"

    groupRoles.each { role ->
        result += "<b>" + role.toString() + "</b>\r"
        def projectRoleMembers = projectRoleManager.getProjectRoleActors(role, project)

        // Check if any group is associated with the role
        def groupsinRole = projectRoleManager.getProjectRoleActors(role, project).findAll { it instanceof ProjectRoleActor.GroupRoleActor }
        
        if (groupsinRole.isEmpty()) {
            // If no group is associated with this role, add it to the missing group list
            rolesMissingGroup << role.toString()
        }

        // Skipping the part for displaying user information if not needed
        // projectRoleMembers.each { projectRoleActor ->
        //     def user = projectRoleActor.getUser()
        //     result += "User: " + user.getUsername() + "\r"
        // }
    }

    // For roles that are missing groups, display the roles
    def groupsinRole = []
    if (groupsinRole.size() > 0) {
        result += "Groups in this role:\r"
        groupsinRole.each { group ->
            result += "Group: " + group.descriptor + "\r"
        }
    }

    finalResult += result
    finalResult += "-------------------------\r\n"
    result = ""
}

// If any roles are missing corresponding groups, display the missing roles
if (rolesMissingGroup.size() > 0) {
    finalResult += "The following roles are missing corresponding groups:\r"
    rolesMissingGroup.each { role ->
        finalResult += role + "\r"
    }
}

"<pre>$finalResult</pre>"

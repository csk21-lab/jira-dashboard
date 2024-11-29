import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.security.roles.ProjectRoleManager
import com.atlassian.jira.security.roles.ProjectRoleActor
import com.atlassian.jira.security.roles.GroupRoleActor
import com.atlassian.jira.security.groups.GroupManager
import com.atlassian.jira.user.util.UserManager
import com.atlassian.jira.project.Project

String groupnamesinRole
String allusersinRole
String result = ""
String finalResult = ""
def userList = []
def rolesMissingGroup = []  // List to store roles with missing groups

// Specify the project key you want to check (for one project)
def projectKey = "ABC"  // Replace with your project key
def project = ComponentAccessor.getProjectManager().getProjectByKey(projectKey)

ProjectRoleManager projectRoleManager = ComponentAccessor.getComponent(ProjectRoleManager)
GroupManager groupManager = ComponentAccessor.getGroupManager()
UserManager userManager = ComponentAccessor.getUserManager() as UserManager
def groupRoles = projectRoleManager.getProjectRoles(project)

result += "Project Name: " + project.name + "\r"
result += "Project Key: " + project.key + "\r\n"

groupRoles.each { role ->
    result += "<b>" + role.toString() + "</b>\r"
    def projectRoleMembers = projectRoleManager.getProjectRoleActors(role, project)

    // Check if a group is associated with the role using HAPI method
    def groupRoleActors = projectRoleManager.getProjectRoleActorsByRoleType(role, project, ProjectRoleActor.GroupRoleActor)
    if (groupRoleActors.isEmpty()) {
        // If no group is associated with this role, add it to the missing group list
        rolesMissingGroup << role.toString()
    } else {
        groupRoleActors.each { groupRoleActor ->
            result += "Group: " + groupRoleActor.getGroup().getName() + "\r"
        }
    }

    // Skipping the part for displaying user information if not needed
    projectRoleMembers.each { projectRoleActor ->
        def user = projectRoleActor.getUser()
        result += "User: " + user.getUsername() + "\r"

        // Using HAPI to check if the user is a member of a specific role in the project
        def isMember = user.isMemberOfRole(role, project)
        if (isMember) {
            result += "User is a member of the ${role} role in project ${project.name}\r"
        } else {
            result += "User is NOT a member of the ${role} role in project ${project.name}\r"
        }
    }
}

finalResult += result
finalResult += "-------------------------\r\n"

// If any roles are missing corresponding groups, display the missing roles
if (rolesMissingGroup.size() > 0) {
    finalResult += "The following roles are missing corresponding groups:\r"
    rolesMissingGroup.each { role ->
        finalResult += role + "\r"
    }
}

"<pre>$finalResult</pre>"

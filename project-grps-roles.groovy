import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.security.roles.ProjectRoleManager
import com.atlassian.jira.security.roles.ProjectRoleActor
import com.atlassian.jira.security.roles.GroupRoleActor
import com.atlassian.jira.security.groups.GroupManager
import com.atlassian.jira.user.util.UserManager
import com.atlassian.jira.project.Project

String result = ""
String finalResult = ""
def rolesMissingGroup = []  // List to store roles with missing groups

// Specify the project key you want to check (for one project)
def projectKey = "ABC"  // Replace with your project key
def project = ComponentAccessor.getProjectManager().getProjectByKey(projectKey)  // Get the Project object using the project key

ProjectRoleManager projectRoleManager = ComponentAccessor.getComponent(ProjectRoleManager)
GroupManager groupManager = ComponentAccessor.getGroupManager()
UserManager userManager = ComponentAccessor.getUserManager() as UserManager

// Fetch project roles for the given project
def groupRoles = projectRoleManager.getProjectRoles(project)

result += "Project Name: " + project.name + "\r"
result += "Project Key: " + project.key + "\r\n"

groupRoles.each { role ->
    result += "<b>" + role.toString() + "</b>\r"
    
    // Retrieve all project role actors (users and groups)
    def projectRoleMembers = projectRoleManager.getProjectRoleActors(role, project)
    
    // To check for group-role actors, filter them
    def groupRoleActors = []
    projectRoleMembers.each { projectRoleActor ->
        if (projectRoleActor instanceof GroupRoleActor) {
            groupRoleActors << projectRoleActor
        }
    }

    // If no group is associated with this role, add it to the missing group list
    if (groupRoleActors.isEmpty()) {
        rolesMissingGroup << role.toString()
    } else {
        // List groups associated with this role
        groupRoleActors.each { groupRoleActor ->
            result += "Group: " + groupRoleActor.getGroup().getName() + "\r"
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

import com.atlassian.jira.project.Project
import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.project.ProjectManager
import com.atlassian.jira.security.roles.ProjectRoleManager
import com.atlassian.jira.security.roles.ProjectRoleActor
import com.atlassian.jira.security.groups.GroupManager

def projectManager = ComponentAccessor.getProjectManager()
def projectRoleManager = ComponentAccessor.getComponent(ProjectRoleManager)
def groupManager = ComponentAccessor.getGroupManager()
def projects = projectManager.getProjects()
def loggedUser = ComponentAccessor.jiraAuthenticationContext.getLoggedInUser()

def issues = []
def projectKeys = ["DEF","ABC"]
projectKeys.each { projectKey ->
    Project projectObject = projectManager.getProjectObjByKey(projectKey)
    def projectRoles = projectRoleManager.getProjectRoles()
    projectRoles.each { role ->
        //log.warn("role : ${role}")

        def projectRoleIdentifier = "${projectObject.getName()}_${role.getName()}"
        def groups = projectRoleManager.getProjectRoleActors(role, projectObject).getRoleActorsByType(ProjectRoleActor.GROUP_ROLE_ACTOR_TYPE)
        def groupNames = groups.collect { it.getGroup().getName() }
        def expectedGroupName = "${projectObject.getKey()}_${role.getName()}"
        //log.warn("expectedGroupName : ${expectedGroupName}")
        // Check if no group or group name does not match exactly
        if (groupNames.isEmpty()) {
            issues.add("Project: ${projectObject.getName()}, Role: ${role.getName()}, Expected Group: ${expectedGroupName}, Actual Groups: ${groupNames.join(', ')}")
        }else{
            groupNames.each{ groupName ->
                    //    log.warn("groupName : ${groupName}")


                 if(groupName != expectedGroupName ){
                        issues.add("Project: ${projectObject.getName()}, Role: ${role.getName()}, Expected Group: ${expectedGroupName}, Actual Groups: ${groupName}")
 
                 }
                
            }
        }
    }
}

if (!issues.isEmpty()) {
    issues.each { issue ->
        log.warn(issue)
    }
} else {
    log.info("All roles have correct group mappings and naming conventions or are correctly ungrouped.")
}

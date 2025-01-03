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
def projectKeys = ["AAA","ABC"]
projectKeys.each { projectKey ->
    Project projectObject = projectManager.getProjectObjByKey(projectKey)
    def projectRoles = projectRoleManager.getProjectRoles()
    projectRoles.each { role ->
        //log.warn("role : ${role}")
        def isGroupMapped = false

        def projectRoleIdentifier = "${projectObject.getName()}_${role.getName()}"
        def groups = projectRoleManager.getProjectRoleActors(role, projectObject).getRoleActorsByType(ProjectRoleActor.GROUP_ROLE_ACTOR_TYPE)
        def groupNames = groups.collect { it.getGroup().getName() }
        def expectedGroupName = "${projectObject.getKey()}_${role.getName()}"
        // Check if no group or group name does not match exactly
        if (!groupNames.isEmpty()) {
             groupNames.each{ groupName ->
                if(groupName == expectedGroupName ){
                    isGroupMapped = true
                 }
            }
        }
        if(!isGroupMapped){
            issues.add("Project: ${projectObject.getName()}, Role: ${role.getName()}, Missing Group: ${expectedGroupName}")
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

def jiraGroups = groupManager.getAllGroups().collect { it.getName() }
def groupsExistInJira = issues.findAll { jiraGroups.contains(it) }

if (!groupsExistInJira.isEmpty()) {
    log.warn("Groups existing in JIRA that are missing correct mappings: ${groupsExistInJira.join(', ')}")
} else {
    log.warn("No common groups in JIRA")
}

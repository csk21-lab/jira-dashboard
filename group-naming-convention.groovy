import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.project.ProjectManager
import com.atlassian.jira.security.roles.ProjectRoleManager
import com.atlassian.jira.security.roles.ProjectRoleActor
import com.atlassian.jira.security.groups.GroupManager

def projectManager = ComponentAccessor.getProjectManager()
def projectRoleManager = ComponentAccessor.getComponent(ProjectRoleManager)
def groupManager = ComponentAccessor.getGroupManager()
def projects = projectManager.getProjects()

def issues = []

projects.each { project ->
    def projectRoles = projectRoleManager.getProjectRoles(project)
    projectRoles.each { role ->
        def projectRoleIdentifier = "${project.getName()}_${role.getName()}"
        def groups = projectRoleManager.getProjectRoleActors(role, project).getRoleActorsByType(ProjectRoleActor.GROUP_ROLE_ACTOR_TYPE)
        def groupNames = groups.collect { it.getGroup().getName() }
        def expectedGroupName = "${project.getKey()}_${role.getName()}"

        // Check if no group or group name does not match exactly
        if (groupNames.isEmpty() || !groupNames.contains(expectedGroupName)) {
            issues.add("Project: ${project.getName()}, Role: ${role.getName()}, Expected Group: ${expectedGroupName}, Actual Groups: ${groupNames.join(', ')}")
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

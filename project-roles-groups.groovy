import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.project.ProjectManager
import com.atlassian.jira.security.roles.ProjectRoleManager
import com.atlassian.jira.security.roles.ProjectRoleActor

def projectManager = ComponentAccessor.getProjectManager()
def projectRoleManager = ComponentAccessor.getComponent(ProjectRoleManager)
def projects = projectManager.getProjects()
def requiredRoles = ["Administrators", "Developers", "Project Manager"]

projects.each { project ->
    def projectRoles = projectRoleManager.getProjectRoles(project)
    def existingRoleNames = projectRoles.collect { it.getName() }
    def missingRoles = requiredRoles.findAll { !existingRoleNames.contains(it) }
    String missingRolesInfo = missingRoles.isEmpty() ? "" : " Missing Roles: ${missingRoles.join(', ')}"

    if (projectRoles.isEmpty()) {
        log.warn("${project.getName()} - No roles defined${missingRolesInfo}")
    } else {
        projectRoles.each { role ->
            def groups = projectRoleManager.getProjectRoleActors(role, project).getRoleActorsByType(ProjectRoleActor.GROUP_ROLE_ACTOR_TYPE)
            String groupInfo = groups.isEmpty() ? "No groups" : "Groups exist"
            log.warn("${project.getName()} - ${role.getName()} : ${groupInfo}${missingRolesInfo}")
        }
    }
}

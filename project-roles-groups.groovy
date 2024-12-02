import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.project.ProjectManager
import com.atlassian.jira.security.roles.ProjectRoleManager
import com.atlassian.jira.security.roles.ProjectRoleActor

def projectManager = ComponentAccessor.getProjectManager()
def projectRoleManager = ComponentAccessor.getComponent(ProjectRoleManager)
def projects = projectManager.getProjects()
def expectedGroups = ["Group1", "Group2", "Group3"] // Define expected groups here

def projectsWithMissingGroups = []

projects.each { project ->
    def projectRoles = projectRoleManager.getProjectRoles(project)
    boolean missingGroups = false

    projectRoles.each { role ->
        def groups = projectRoleManager.getProjectRoleActors(role, project).getRoleActorsByType(ProjectRoleActor.GROUP_ROLE_ACTOR_TYPE)
        def groupNames = groups.collect { it.getGroup().getName() }
        def missingInRole = expectedGroups.findAll { !groupNames.contains(it) }

        if (!missingInRole.isEmpty()) {
            missingGroups = true
        }
    }

    if (missingGroups) {
        projectsWithMissingGroups.add(project.getName())
    }
}

if (!projectsWithMissingGroups.isEmpty()) {
    log.warn("Projects with missing groups: ${projectsWithMissingGroups.join(', ')}")
} else {
    log.warn("All projects have the necessary groups assigned to roles.")
}

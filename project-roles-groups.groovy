import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.project.ProjectManager
import com.atlassian.jira.security.roles.ProjectRoleManager
import com.atlassian.jira.security.roles.ProjectRoleActor

def projectManager = ComponentAccessor.getProjectManager()
def projectRoleManager = ComponentAccessor.getComponent(ProjectRoleManager)
def projects = projectManager.getProjects()

// Initialize a map to store dynamic expected groups
def dynamicExpectedGroups = [:]

projects.each { project ->
    def projectRoles = projectRoleManager.getProjectRoles(project)
    projectRoles.each { role ->
        // Assuming every role should have at least one group, populate expected groups
        dynamicExpectedGroups["${project.getName()}_${role.getName()}"] = []
    }
}

// Check each project and role for group associations
def missingGroupDetails = []

projects.each { project ->
    def projectRoles = projectRoleManager.getProjectRoles(project)
    projectRoles.each { role ->
        def projectRoleIdentifier = "${project.getName()}_${role.getName()}"
        def groups = projectRoleManager.getProjectRoleActors(role, project).getRoleActorsByType(ProjectRoleActor.GROUP_ROLE_ACTOR_TYPE)
        def groupNames = groups.collect { it.getGroup().getName() }

        // Check if the expected group (by role) is empty (no groups found)
        if (groups.isEmpty()) {
            missingGroupDetails.add(projectRoleIdentifier)
        } else {
            dynamicExpectedGroups[projectRoleIdentifier] = groupNames
        }
    }
}

if (!missingGroupDetails.isEmpty()) {
    log.warn("Projects and roles with missing groups: ${missingGroupDetails.join(', ')}")
} else {
    log.warn("All specified roles in all projects have associated groups.")
}

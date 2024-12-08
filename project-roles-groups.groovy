import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.project.ProjectManager
import com.atlassian.jira.security.roles.ProjectRoleManager
import com.atlassian.jira.security.roles.ProjectRoleActor
import com.atlassian.jira.security.groups.GroupManager

def projectManager = ComponentAccessor.getProjectManager()
def projectRoleManager = ComponentAccessor.getComponent(ProjectRoleManager)
def groupManager = ComponentAccessor.getGroupManager()
def projects = projectManager.getProjects()

def requiredRoles = ["admin", "team"]
def groupsOfRoles = [:]
def missingGroupDetails = []

projects.each { project ->
    requiredRoles.each { role ->
        groupsOfRoles[project.getKey() + "_" + role] = []
    }

    project.getProjectRoles().each { role ->
        def projectRoleIdentifier = "${project.getKey()}_${role.getName()}"
        def groups = projectRoleManager.getProjectRoleActors(role, project).getRoleActorsByType(ProjectRoleActor.GROUP_ROLE_ACTOR_TYPE)
        def groupNames = groups.collect { it.getGroup().getName() }

        if (groupNames.isEmpty()) {
            missingGroupDetails.add(projectRoleIdentifier)
        } else {
            groupNames.each { groupName ->
                if (groupName != projectRoleIdentifier) {
                    missingGroupDetails.add(projectRoleIdentifier)
                }
            }
        }
    }
}

if (!missingGroupDetails.isEmpty()) {
    missingGroupDetails.each { groupMissing ->
        log.warn("${groupMissing} is missing the correct group mapping.")
    }
}

def jiraGroups = groupManager.getAllGroups().collect { it.getName() }
def groupsExistInJira = missingGroupDetails.findAll { jiraGroups.contains(it) }

if (!groupsExistInJira.isEmpty()) {
    log.warn("Groups existing in JIRA that are missing correct mappings: ${groupsExistInJira.join(', ')}")
} else {
    log.warn("No common groups in JIRA")
}

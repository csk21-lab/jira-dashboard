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
def projectKeys = ["AAA"]
projectKeys.each { projectKey ->
    Project projectObject = projectManager.getProjectObjByKey(projectKey)
    def projectRoles = projectRoleManager.getProjectRoles()
    def validGroupMapping = [:]
    Map<String, List> inValidGroupMapping = [:]

    projectRoles.each { role ->
        def projectRoleIdentifier = "${projectObject.getName()}_${role.getName()}"
        def roleActors = projectRoleManager.getProjectRoleActors(role, projectObject)
        def groupActors = roleActors.getRoleActorsByType(ProjectRoleActor.GROUP_ROLE_ACTOR_TYPE)
        def userActors = roleActors.getRoleActorsByType(ProjectRoleActor.USER_ROLE_ACTOR_TYPE)

        def groupNames = groupActors.collect { it.getGroup().getName() }
        def userNames = userActors.collect { it.getUserKey() }
        def expectedGroupName = "${projectObject.getKey()}_${role.getName()}"

        if (groupNames.isEmpty() && userNames.isEmpty()) {
            issues.add("Project: ${projectObject.getName()}, Role: ${role.getName()}, Expected Group: ${expectedGroupName}, Actual Groups: ${groupNames.join(', ')}, Actual Users: ${userNames.join(', ')}")
        } else {
            groupNames.each { groupName ->
                if (groupName != expectedGroupName) {
                    issues.add("Project: ${projectObject.getName()}, Role: ${role.getName()}, Expected Group: ${expectedGroupName}, Actual Groups: ${groupName}")
                    if (inValidGroupMapping.containsKey(role.getName())) {
                        List groupList = inValidGroupMapping.get(role.getName())
                        groupList.add(groupName)
                        inValidGroupMapping.put(role.getName(), groupList)
                    } else {
                        List roleGroups = []
                        roleGroups.add(groupName)
                        inValidGroupMapping.put(role.getName(), roleGroups)
                    }
                } else {
                    validGroupMapping.put(role.getName(), groupName)
                }
            }
        }
    }

    log.warn("inValidGroupMapping : ${inValidGroupMapping}")
    log.warn("validGroupMapping : ${validGroupMapping}")

    projectRoles.each { role ->
        def roleName = role.getName()
        String validGroupName = validGroupMapping.get(roleName)
        
        log.warn("Role: ${roleName}")
        // Log individual users directly assigned to the role
        def roleActors = projectRoleManager.getProjectRoleActors(role, projectObject)
        def userActors = roleActors.getRoleActorsByType(ProjectRoleActor.USER_ROLE_ACTOR_TYPE)
        def directUserNames = userActors.collect { it.getUserKey() }
        
        log.warn("Direct Users: ${directUserNames.join(', ')}")

        if (validGroupName) {
            log.warn("Valid Group: ${validGroupName}")
            List usersList = groupManager.getUserNamesInGroup(validGroupName)
            log.warn("Users in Valid Group: ${usersList.join(', ')}")

            def groupsList = inValidGroupMapping.get(roleName)
            if (groupsList) {
                groupsList.each { groupName ->
                    List invalidGroupUserList = groupManager.getUserNamesInGroup(groupName)
                    log.warn("Users in Invalid Group (${groupName}): ${invalidGroupUserList.join(', ')}")
                }
            }

            // Compare direct users to users in valid group
            directUserNames.each { userName ->
                if (usersList.contains(userName)) {
                    log.warn("Direct userName Matching with Group User: ${userName}")
                } else {
                    log.warn("Direct userName MIS-Match with Group User: ${userName}")
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

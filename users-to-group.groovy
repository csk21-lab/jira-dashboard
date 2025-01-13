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
    Map<String,List> inValidGroupMapping = [:] 
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
                        if(inValidGroupMapping.containsKey(role.getName())){
                            List groupList = inValidGroupMapping.get(role.getName())
                            groupList.add(groupName)
                            inValidGroupMapping.put(role.getName(), groupList)

                        }else{
                            List roleGroups = []
                            roleGroups.add(groupName)
                            inValidGroupMapping.put(role.getName(), roleGroups)

                        }
 
                 }else{
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
        if(validGroupName){
            log.warn("validGroupName : ${validGroupName}")
            List usersList = groupManager.getUserNamesInGroup(validGroupName)
            log.warn("usersList : ${usersList}")
            def groupsList = inValidGroupMapping.get(roleName)
            groupsList.each { groupName -> 
                List invalidGroupUserList = groupManager.getUserNamesInGroup(groupName)
                invalidGroupUserList?.each { userName ->
                    if(usersList.contains(userName)){
                        log.warn("userName Matching : ${userName}")

                    }else{
                        log.warn("userName MIS-Match : ${userName}")

                    }
                 }





             }

        }else{

        }




    }



}

if (!issues.isEmpty()) {
    issues.each { issue ->
      //  log.warn(issue)
    }
} else {
   // log.info("All roles have correct group mappings and naming conventions or are correctly ungrouped.")
}

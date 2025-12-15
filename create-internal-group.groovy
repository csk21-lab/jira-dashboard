import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.security.groups.GroupManager
 
// Name of the new group to create
String groupName = "new-internal-group"

// Get the GroupManager instance
GroupManager groupManager = ComponentAccessor.getGroupManager()

if (!groupManager.groupExists(groupName)) {
    // Create the group
    groupManager.createGroup(groupName)
    log.info "Group '${groupName}' created successfully!"
} else {
    log.info "Group '${groupName}' already exists."
}

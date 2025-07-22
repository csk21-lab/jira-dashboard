import com.atlassian.jira.security.login.LoginManager
import com.atlassian.crowd.manager.directory.DirectoryManager
import com.atlassian.jira.bc.user.UserService
import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.user.ApplicationUser
import com.atlassian.jira.issue.search.SearchProvider
import com.atlassian.jira.jql.parser.JqlQueryParser
import com.atlassian.jira.web.bean.PagerFilter

def userManager = ComponentAccessor.userManager
def directoryManager = ComponentAccessor.getComponent(DirectoryManager)
def loginManager = ComponentAccessor.getComponent(LoginManager)
def groupManager = ComponentAccessor.getGroupManager()
def userService = ComponentAccessor.getComponent(UserService)
def loggedInUser = ComponentAccessor.jiraAuthenticationContext.loggedInUser
def runLog = []

def internalDirectory = 10001
def neverLoggedInUsers = []  // List to store never logged-in users
def today = new Date()

// Iterate over all users
userManager.allApplicationUsers.each { user ->
    def directory = directoryManager.findDirectoryById(user.directoryId)
    if (directory.id == internalDirectory) {
        // Check if the user is part of the 'jira-software-users' group
        def groups = groupManager.getGroupsForUser(user)
        if (groups.any { it.name == 'jira-software-users' }) {
            // Get the login information for the user
            def loginInfo = loginManager.getLoginInfo(user.username)
            if (loginInfo) {
                if (loginInfo.lastLoginTime == null) {
                    // User has never logged in
                    neverLoggedInUsers << user.username
                }
            }
        }
    }
}

// Display the results
def neverLoggedInList = neverLoggedInUsers.join(", ")
def totalNeverLoggedInUsers = neverLoggedInUsers.size()

log.warn("Total number of users who have never logged in and are in 'jira-software-users' group: ${totalNeverLoggedInUsers}")
log.warn("List of users who have never logged in: \n${neverLoggedInList}")

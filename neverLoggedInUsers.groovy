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
def daysOfInactivity = 350  // Define the threshold for inactivity
def neverLoggedInUsers = []  // List to store users who have never logged in, have application access, and are not inactive
def today = new Date()
def cutoffDate = today - daysOfInactivity  // Calculate the cutoff date for inactivity

// Iterate over all users
userManager.allApplicationUsers.each { user ->
    def directory = directoryManager.findDirectoryById(user.directoryId)
    if (directory.id == internalDirectory) {
        // Check if the user has never logged in
        def loginInfo = loginManager.getLoginInfo(user.username)
        if (loginInfo && loginInfo.lastLoginTime == null) {
            // Check if the user has Jira application access
            def hasApplicationAccess = userService.isUserInApplication(user, "jira-software")  // Check for Jira Software application access
            if (hasApplicationAccess) {
                // Check if the user is inactive (i.e., hasn't logged in recently based on cutoff date)
                def lastLogin = loginInfo.lastLoginTime ? new Date(loginInfo.lastLoginTime) : null
                if (lastLogin == null || lastLogin.after(cutoffDate)) {
                    // User has never logged in and is not inactive
                    neverLoggedInUsers << user.username
                }
            }
        }
    }
}

// Display the results
def neverLoggedInList = neverLoggedInUsers.join

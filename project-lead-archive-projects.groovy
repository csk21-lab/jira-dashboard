import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.project.Project
import org.apache.log4j.Level
import org.apache.log4j.Logger

// Set up the logger
def log = Logger.getLogger("com.example.archivedProjectsLogger")
log.setLevel(Level.DEBUG)

def projectManager = ComponentAccessor.getProjectManager()

// Log the start of the script
log.info("Starting to retrieve archived project details...")

// Get all projects and filter for archived ones
def archivedProjects = projectManager.getProjects().findAll { it.isArchived() }
log.info("Total number of archived projects found: ${archivedProjects.size()}")

archivedProjects.each { project ->
    log.info("Processing archived project: ${project.getName()} (Key: ${project.key})")

    // Log project lead information
    def projectLead = project.getProjectLead()
    if (projectLead) {
        log.debug("Project Lead Display Name: ${projectLead.displayName}")
        log.debug("Project Lead Username/Key: ${projectLead.getKey()}")
        log.debug("Project Lead Email Address: ${projectLead.getEmailAddress() ?: 'No email address available'}")
    } else {
        log.warn("Archived Project: ${project.getName()} has no assigned project lead.")
    }

    // Log additional project information
    log.debug("Project Category: ${project.getProjectCategoryObject()?.name ?: 'No category assigned'}")
    log.debug("Project Components: ${project.getComponents().collect { it.name }.join(', ') ?: 'No components available'}")
    log.debug("Project Versions: ${project.getVersions().collect { it.name }.join(', ') ?: 'No versions available'}")
    log.debug("Project Issue Types: ${project.getIssueTypes().collect { it.name }.join(', ') ?: 'No issue types available'}")
}

// Log the completion of the script
log.info("Completed processing archived project details.")

import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.project.Project

def projectManager = ComponentAccessor.getProjectManager()

// Get all archived projects and log the total count
def archivedProjects = projectManager.getProjectObjects().findAll { it.isArchived() }
log.warn("Total Archived Projects Found: ${archivedProjects.size()}")

archivedProjects.each { project ->
    def projectName = project.getName()
    def projectLead = project.getProjectLead()

    // Log project name to ensure it's being accessed
    log.warn("Checking Project: ${projectName}")

    if (projectLead) {
        // Log project lead details
        log.warn("Archived Project: ${projectName}, Project Lead Display Name: ${projectLead.displayName}, Username: ${projectLead.getKey()}")
    } else {
        log.warn("Archived Project: ${projectName} has no assigned project lead.")
    }
}

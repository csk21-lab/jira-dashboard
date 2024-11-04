import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.project.Project

def projectManager = ComponentAccessor.getProjectManager()

// Get all archived projects
def archivedProjects = projectManager.getProjectObjects().findAll { it.isArchived() }

archivedProjects.each { project ->
    def projectLead = project.getProjectLead()
    def projectName = project.getName()

    if (projectLead) {
        // Use getKey() to retrieve the username
        log.warn("Archived Project: ${projectName}, Project Lead: ${projectLead.displayName} (Username: ${projectLead.getKey()})")
    } else {
        log.warn("Archived Project: ${projectName} has no assigned project lead.")
    }
}

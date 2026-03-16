import com.atlassian.confluence.pages.Page
import com.atlassian.confluence.pages.PageManager
import com.atlassian.confluence.spaces.Space
import com.atlassian.confluence.spaces.SpaceManager
import com.atlassian.sal.api.component.ComponentLocator

// ---------- CONFIG ----------
def SPACE_KEY    = "ITSM"                 // target space
def PARENT_PAGE_TITLE = "Jira Requirements"  // parent page title (optional)
def PROJECT_NAME = "Sample Project"
def ISSUE_TYPE   = "Service Request"
def PORTAL_NAME  = "Access Request"
// -----------------------------

def spaceManager = ComponentLocator.getComponent(SpaceManager)
def pageManager  = ComponentLocator.getComponent(PageManager)

Space space = spaceManager.getSpace(SPACE_KEY)
assert space : "Space ${SPACE_KEY} not found"

// find parent page by title in space (or just use space home page)
Page parentPage = pageManager.getPage(space, PARENT_PAGE_TITLE)
if (!parentPage) {
    parentPage = space.getHomePage()
}

def pageTitle = "Jira Project / Portal Requirements - ${PROJECT_NAME}"

// body in Confluence storage format (XHTML)
def body = """
<h1>Jira Project / Portal Requirements</h1>

<p><strong>Project name:</strong> ${PROJECT_NAME}</p>
<p><strong>Issue type:</strong> ${ISSUE_TYPE}</p>
<p><strong>Portal request form name:</strong> ${PORTAL_NAME}</p>

<h2>1. Context</h2>
<table class="wrapped">
  <tr>
    <th>Project key</th>
    <th>Project type</th>
    <th>Requester</th>
    <th>Target go-live</th>
  </tr>
  <tr>
    <td></td><td></td><td></td><td></td>
  </tr>
</table>

<h2>2. Issue types</h2>
<table class="wrapped">
  <tr>
    <th>Issue type</th>
    <th>Scheme</th>
    <th>Used in portal?</th>
    <th>Notes</th>
  </tr>
  <tr>
    <td>${ISSUE_TYPE}</td>
    <td></td>
    <td>Yes</td>
    <td></td>
  </tr>
</table>

<h2>3. Portal request forms</h2>
<table class="wrapped">
  <tr>
    <th>Request type name</th>
    <th>Issue type</th>
    <th>Portal group</th>
    <th>Summary pattern</th>
  </tr>
  <tr>
    <td>${PORTAL_NAME}</td>
    <td>${ISSUE_TYPE}</td>
    <td></td>
    <td>[${PROJECT_NAME}] - </td>
  </tr>
</table>

<h2>4. Fields</h2>
<table class="wrapped">
  <tr>
    <th>Field name</th>
    <th>Jira field</th>
    <th>Required?</th>
    <th>Type</th>
    <th>Default / Options</th>
    <th>Validation / Notes</th>
  </tr>
  <tr>
    <td>Summary</td>
    <td>summary</td>
    <td>Required</td>
    <td>Text</td>
    <td></td>
    <td></td>
  </tr>
</table>

<h2>5. Workflow & approvals</h2>
<table class="wrapped">
  <tr>
    <th>Step</th>
    <th>Status</th>
    <th>Approver source</th>
    <th>Transition rule</th>
  </tr>
  <tr>
    <td>Manager approval</td>
    <td>Pending approval</td>
    <td>Manager field</td>
    <td>Auto-approve if ...</td>
  </tr>
</table>

<h2>6. SLAs / Notifications / Automation</h2>
<ul>
  <li><strong>SLAs:</strong> Name, start/stop events, goal, calendar.</li>
  <li><strong>Notifications:</strong> Events and recipients.</li>
  <li><strong>Automation:</strong> Auto-assign, set fields, create subtasks, sync with Assets, etc.</li>
</ul>

<h2>7. Permissions & access</h2>
<ul>
  <li>Who can raise this request?</li>
  <li>Who can work on these issues?</li>
</ul>
"""

Page page = new Page()
page.setSpace(space)
page.setParentPage(parentPage)
page.setTitle(pageTitle)
page.setBodyAsString(body)

parentPage.addChild(page)
pageManager.saveContentEntity(page, null, null)

log.warn "Created page '${page.title}' in space ${SPACE_KEY}, parent='${parentPage.title}'"
return page.id

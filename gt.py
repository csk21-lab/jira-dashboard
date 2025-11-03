import gitlab
import json
import os

# GitLab configuration
GITLAB_URL = "https://gitlab.com"
PRIVATE_TOKEN = "YOUR_GITLAB_TOKEN"

# Specify the local download path (update this path as you like)
output_path = "./exports/gitlab_metadata.json"

# Create directory if it doesn't exist
os.makedirs(os.path.dirname(output_path), exist_ok=True)

# Connect to GitLab instance
gl = gitlab.Gitlab(GITLAB_URL, private_token=PRIVATE_TOKEN)

output = []

# List all groups the token has access to
groups = gl.groups.list(all=True)
for group in groups:
    group_data = {
        "id": group.id,
        "name": group.name,
        "full_name": group.full_name,
        "web_url": group.web_url,
        "projects": []
    }
    # List all projects in the group
    projects = group.projects.list(all=True)
    for project in projects:
        project_full = gl.projects.get(project.id)
        project_data = {
            "id": project.id,
            "name": project.name,
            "description": project.description,
            "web_url": project.web_url,
            "created_at": project.created_at,
            "last_activity_at": project.last_activity_at,
            "visibility": project.visibility,
            "default_branch": project.default_branch,
            "branches": [],
            "commits": []
        }
        # Get all branches
        branches = project_full.branches.list(all=True)
        for branch in branches:
            project_data["branches"].append({
                "name": branch.name,
                "protected": branch.protected,
                "commit_id": branch.commit['id']
            })
            # Get recent commits for each branch (e.g., last 5 to avoid overloading)
            commits = project_full.commits.list(ref_name=branch.name, per_page=5)
            for commit in commits:
                project_data["commits"].append({
                    "id": commit.id,
                    "short_id": commit.short_id,
                    "title": commit.title,
                    "author_name": commit.author_name,
                    "created_at": commit.created_at
                })
        group_data["projects"].append(project_data)
    output.append(group_data)

# Export all collected metadata to the specified local path
with open(output_path, "w", encoding="utf-8") as f:
    json.dump(output, f, indent=2)

print(f"Export complete. Check {output_path}.")

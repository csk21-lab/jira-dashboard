import os
import requests
from requests.auth import HTTPBasicAuth

# === CONFIGURATION ===

# Jira
JIRA_URL = "https://jira.example.com"
JIRA_USER = "your-jira-username"
JIRA_API_TOKEN = "your-jira-api-token-or-password"
JIRA_ISSUE_KEY = "ABC-123"

# Confluence
CONFLUENCE_URL = "https://confluence.example.com"
CONFLUENCE_USER = "your-confluence-username"
CONFLUENCE_API_TOKEN = "your-confluence-api-token-or-password"
CONFLUENCE_PAGE_ID = "123456"

# Local directory to store files temporarily
DOWNLOAD_DIR = "attachments"

# === SETUP ===
os.makedirs(DOWNLOAD_DIR, exist_ok=True)

# === Step 1: Get Attachments from Jira Issue ===

def get_attachments_from_jira():
    url = f"{JIRA_URL}/rest/api/2/issue/{JIRA_ISSUE_KEY}"
    auth = HTTPBasicAuth(JIRA_USER, JIRA_API_TOKEN)
    response = requests.get(url, auth=auth)
    response.raise_for_status()

    issue = response.json()
    attachments = issue.get("fields", {}).get("attachment", [])
    files = []

    for att in attachments:
        filename = att["filename"]
        content_url = att["content"]
        file_path = os.path.join(DOWNLOAD_DIR, f"{JIRA_ISSUE_KEY}-{filename}")

        print(f"⬇️ Downloading: {filename}")
        file_resp = requests.get(content_url, auth=auth, stream=True)
        file_resp.raise_for_status()
        with open(file_path, "wb") as f:
            for chunk in file_resp.iter_content(chunk_size=8192):
                f.write(chunk)

        files.append(file_path)

    return files

# === Step 2: Upload Files to Confluence Page ===

def upload_to_confluence(files):
    url = f"{CONFLUENCE_URL}/wiki/rest/api/content/{CONFLUENCE_PAGE_ID}/child/attachment"
    auth = HTTPBasicAuth(CONFLUENCE_USER, CONFLUENCE_API_TOKEN)

    for path in files:
        filename = os.path.basename(path)
        print(f"⬆️ Uploading to Confluence: {filename}")

        with open(path, "rb") as file_data:
            response = requests.post(
                url,
                auth=auth,
                files={"file": (filename, file_data)},
                data={"minorEdit": "true", "comment": f"Uploaded from Jira issue {JIRA_ISSUE_KEY}"}
            )
            if response.status_code not in (200, 201):
                print(f"❌ Failed to upload {filename}: {response.status_code} - {response.text}")
            else:
                print(f"✅ Uploaded: {filename}")

# === MAIN EXECUTION ===
if __name__ == "__main__":
    downloaded_files = get_attachments_from_jira()
    if downloaded_files:
        upload_to_confluence(downloaded_files)
    else:
        print("⚠️ No attachments found in Jira issue.")

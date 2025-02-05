#!/bin/bash

# Remote server details
REMOTE_USER="your_username"
REMOTE_HOST="remote_server_ip_or_hostname"
REMOTE_COMMAND="curl http://example.com/api/endpoint"

# Execute the curl command remotely using SSH
ssh ${REMOTE_USER}@${REMOTE_HOST} "${REMOTE_COMMAND}"

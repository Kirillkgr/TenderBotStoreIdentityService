#!/bin/bash

# Update and upgrade the system
# need using command sudo apt -V install gnupg2 pass

# Check if docker is installed
echo "Checking if docker is installed..."
if ! command -v docker &> /dev/null; then
  echo "Updating and upgrading the system..."
    sudo apt update && sudo apt full-upgrade -y
    echo "docker is not installed. Installing..."
    apt install -y docker.io
fi

# Check if docker-compose is installed
echo "Checking if docker-compose is installed..."
if ! command -v docker-compose &> /dev/null; then
    echo "docker-compose is not installed. Installing..."
    apt install -y docker-compose
fi

# Start the Docker service
echo "Starting the Docker service..."
systemctl start docker

# Login to Yandex Container Registry using Service Account JSON key
echo "Login to Yandex Container Registry (via service account JSON key)..."
if [ -z "$YC_SA_JSON_CREDENTIALS" ]; then
  echo "ERROR: YC_SA_JSON_CREDENTIALS is not set. Export YC_SA_JSON_CREDENTIALS with the JSON key before running this script."
  exit 1
fi
echo "$YC_SA_JSON_CREDENTIALS" | docker login --username json_key --password-stdin cr.yandex

# Change directory
echo "Change directory..."
cd /home/user

# Stop container
echo "Stop container..."
docker-compose down

# Clear images
echo "Clear images..."
docker system prune -af

# Container up
echo "Container up..."

# Validate Google OAuth2 environment presence (injected by CI)
if [ -z "$GOOGLE_CLIENT_ID" ] || [ -z "$GOOGLE_CLIENT_SECRET" ] || [ -z "$GOOGLE_REDIRECT_URI" ]; then
  echo "ERROR: Google OAuth2 env vars are missing. Expected GOOGLE_CLIENT_ID, GOOGLE_CLIENT_SECRET, GOOGLE_REDIRECT_URI to be set."
  exit 1
fi

echo "OAuth2 env present: GOOGLE_CLIENT_ID=${GOOGLE_CLIENT_ID:+***set***} GOOGLE_REDIRECT_URI=$GOOGLE_REDIRECT_URI"

docker-compose up -d

# Remove script file
echo "Remove script file..."
rm -rf starts3.sh

# Print success message
echo "Starting s3 server successfully..."
exit
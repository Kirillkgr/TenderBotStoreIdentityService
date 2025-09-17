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

# OAuth to docker yandex registry (expects OAUTH_TOKEN env variable)
echo "OAuth to docker yandex registry..."
if [ -z "$OAUTH_TOKEN" ]; then
  echo "ERROR: OAUTH_TOKEN is not set. Export OAUTH_TOKEN before running this script."
  exit 1
fi
echo "$OAUTH_TOKEN" | docker login --username oauth --password-stdin cr.yandex

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
docker-compose up -d

# Remove script file
echo "Remove script file..."
rm -rf starts3.sh

# Print success message
echo "Starting s3 server successfully..."
exit
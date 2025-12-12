#!/usr/bin/env bash
set -euo pipefail

# Required envs (export before run or pass inline):
#   CERT_EMAIL       - email for Let's Encrypt (required)
#   DOMAIN           - primary domain (default: none)
#   NGINX_CERT_DIR   - host dir mounted into nginx as /etc/ssl (default: /home/user/certs/ssl)
#   COMPOSE_FILE_PATH- absolute path to docker-compose.yml (optional, for restarting frontend)
# Optional:
#   FRONTEND_CONTAINER_NAME=frontend
#
# This script:
# 1) removes previous local work ssl dir
# 2) runs certbot via docker-compose (standalone HTTP-01 on port 80)
# 3) copies fresh certs to Nginx cert dir
# 4) restarts/reloads frontend (nginx) container

WORK_DIR="$(cd "$(dirname "$0")" && pwd)"
FRONTEND_CONTAINER_NAME=${FRONTEND_CONTAINER_NAME:-frontend}
DOMAIN=${DOMAIN}
NGINX_CERT_DIR=${NGINX_CERT_DIR:-/home/user/certs/ssl}

# detect docker compose command (v2 vs v1)
dcmd() {
  if command -v docker &>/dev/null; then
    if docker compose version &>/dev/null; then
      echo "docker compose"
      return
    fi
  fi
  if command -v docker-compose &>/dev/null; then
    echo "docker-compose"
    return
  fi
  echo "docker compose" # best effort
}
DCMD=$(dcmd)

if [[ -z "${CERT_EMAIL:-}" ]]; then
  echo "CERT_EMAIL must be set" >&2
  exit 1
fi

echo "[1/6] Cleanup local work ssl dir"
rm -rf "${WORK_DIR}/ssl"
mkdir -p "${WORK_DIR}/ssl"

echo "[2/6] Stop frontend (if running and may occupy :80)"
if docker ps --format '{{.Names}}' | grep -q "^${FRONTEND_CONTAINER_NAME}$"; then
  docker stop "${FRONTEND_CONTAINER_NAME}" || true
fi

echo "[3/6] Run certbot (HTTP-01, port 80) for domain: ${DOMAIN}"
CERT_EMAIL=${CERT_EMAIL} DOMAIN=${DOMAIN} \
  ${DCMD} -f "${WORK_DIR}/docker-compose-create-cert.yml" up --abort-on-container-exit

LIVE_SRC_DIR="${WORK_DIR}/ssl/live/${DOMAIN}"
if [[ ! -d "${LIVE_SRC_DIR}" ]]; then
  echo "Live dir not found: ${LIVE_SRC_DIR}" >&2
  exit 2
fi

echo "[4/6] Prepare target certs dir: ${NGINX_CERT_DIR}"
mkdir -p "${NGINX_CERT_DIR}/live/${DOMAIN}"

echo "[5/6] Copy certs to Nginx dir"
# Copy live and archive (for compatibility)
rsync -a --delete "${WORK_DIR}/ssl/live/${DOMAIN}/" "${NGINX_CERT_DIR}/live/${DOMAIN}/"
mkdir -p "${NGINX_CERT_DIR}/archive/${DOMAIN}" || true
if [[ -d "${WORK_DIR}/ssl/archive/${DOMAIN}" ]]; then
  rsync -a --delete "${WORK_DIR}/ssl/archive/${DOMAIN}/" "${NGINX_CERT_DIR}/archive/${DOMAIN}/"
fi

# Optionally create combined PEM if needed by other services
cat "${NGINX_CERT_DIR}/live/${DOMAIN}/fullchain.pem" \
    "${NGINX_CERT_DIR}/live/${DOMAIN}/privkey.pem" > "${NGINX_CERT_DIR}/live/${DOMAIN}/combined.pem"
chmod 600 "${NGINX_CERT_DIR}/live/${DOMAIN}/"*.pem || true

# Ensure correct ownership if needed (uncomment and adjust)
# chown -R nginx:nginx "${NGINX_CERT_DIR}"

echo "[6/6] Start frontend and reload Nginx"
# Start frontend back
if docker ps -a --format '{{.Names}}' | grep -q "^${FRONTEND_CONTAINER_NAME}$"; then
  docker start "${FRONTEND_CONTAINER_NAME}" || true
fi
# Try graceful reload, fallback to restart
if docker ps --format '{{.Names}}' | grep -q "^${FRONTEND_CONTAINER_NAME}$"; then
  if docker exec "${FRONTEND_CONTAINER_NAME}" nginx -t >/dev/null 2>&1; then
    docker exec "${FRONTEND_CONTAINER_NAME}" nginx -s reload || docker restart "${FRONTEND_CONTAINER_NAME}"
  else
    docker restart "${FRONTEND_CONTAINER_NAME}"
  fi
elif [[ -n "${COMPOSE_FILE_PATH:-}" && -f "${COMPOSE_FILE_PATH}" ]]; then
  ${DCMD} -f "${COMPOSE_FILE_PATH}" restart frontend || true
else
  echo "Frontend container not found; please reload Nginx manually." >&2
fi

echo "Done. Certificates updated for ${DOMAIN}."

# Cleanup working files to allow fresh copy next run
echo "Cleanup working files in ${WORK_DIR}"
rm -rf "${WORK_DIR}/ssl" || true
rm -f  "${WORK_DIR}/docker-compose-create-cert.yml" || true
rm -f  "${WORK_DIR}/renew-and-deploy.sh" || true

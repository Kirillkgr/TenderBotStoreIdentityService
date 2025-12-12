#!/usr/bin/env bash
set -euo pipefail

# init-all.sh - Initialize Terraform backends for dashboard and tbs_kuber with Yandex Object Storage (S3-compatible)
# Usage:
#   ./init-all.sh                     # init BOTH modules (no apply)
#   ./init-all.sh -d                  # init ONLY dashboard (no apply)
#   ./init-all.sh -t                  # init ONLY tbs_kuber (no apply)
#   ./init-all.sh -d -t               # init BOTH modules (no apply)
#   ./init-all.sh --apply             # init BOTH modules and apply
#   ./init-all.sh -d --apply          # init+apply dashboard only
#   ./init-all.sh -t --apply          # init+apply tbs_kuber only
#   BUCKET_INFRA_NAME=... ACCESS_KEY_INFRA_ID=... SECRET_KEY_INFRA=... ./init-all.sh
#   ./init-all.sh --bucket tbs-infra --access-key ... --secret-key ... [--region ru-central1]

ROOT_DIR="$(cd "$(dirname "$0")"/.. && pwd)"
TF_DIR="$(cd "$(dirname "$0")" && pwd)"


REGION="ru-central1"

# Parse args
DO_APPLY=false
DO_DASHBOARD=false
DO_TBS=false
while [[ $# -gt 0 ]]; do
  case "$1" in
    --apply)
      DO_APPLY=true; shift 1;;
    --bucket)
      BUCKET_NAME="$2"; shift 2;;
    --access-key)
      ACCESS_KEY="$2"; shift 2;;
    --secret-key)
      SECRET_KEY="$2"; shift 2;;
    --region)
      REGION="$2"; shift 2;;
    -d|--dashboard)
      DO_DASHBOARD=true; shift 1;;
    -t|--tbs|--tbs-only|-tbs|--tbs-only|--tbs)
      DO_TBS=true; shift 1;;
    -h|--help)
      echo "Usage: $0 [-d|--dashboard] [-t|--tbs] [--apply] [--bucket <name>] [--access-key <id>] [--secret-key <key>] [--region <ru-central1>]";
      echo "  Default: init BOTH modules (no apply).";
      echo "  -d/--dashboard: init dashboard (can be combined with -t).";
      echo "  -t/--tbs: init tbs_kuber (can be combined with -d).";
      echo "  --apply: after init, run terraform apply for selected modules.";
      exit 0;;
    *)
      echo "Unknown arg: $1"; exit 1;;
  esac
done

# If no module flags were provided, default to BOTH
if [[ "$DO_DASHBOARD" == "false" && "$DO_TBS" == "false" ]]; then
  DO_DASHBOARD=true
  DO_TBS=true
fi

# Load .env from repo root if present
if [[ -f "$ROOT_DIR/.env" ]]; then
  set -a
  # shellcheck disable=SC1090
  . "$ROOT_DIR/.env"
  set +a
fi

# Fallback to .env vars if not provided via args
if [[ -n "${BUCKET_INFRA_NAME:-}" ]]; then BUCKET_NAME="$BUCKET_INFRA_NAME"; fi
if [[ -n "${ACCESS_KEY_INFRA_ID:-}" ]]; then ACCESS_KEY="$ACCESS_KEY_INFRA_ID"; fi
if [[ -n "${SECRET_KEY_INFRA:-}" ]]; then SECRET_KEY="$SECRET_KEY_INFRA"; fi
if [[ -n "${BUCKET_INFRA_REGION:-}" ]]; then REGION="$BUCKET_INFRA_REGION"; fi

if [[ -z "$BUCKET_NAME" || -z "$ACCESS_KEY" || -z "$SECRET_KEY" ]]; then
  echo "Error: Missing credentials. Provide --bucket/--access-key/--secret-key or set BUCKET_INFRA_NAME, ACCESS_KEY_INFRA_ID, SECRET_KEY_INFRA in .env"
  exit 1
fi

export AWS_ACCESS_KEY_ID="$ACCESS_KEY"
export AWS_SECRET_ACCESS_KEY="$SECRET_KEY"
export AWS_DEFAULT_REGION="$REGION"
export AWS_EC2_METADATA_DISABLED=true

command_exists() {
  command -v "$1" >/dev/null 2>&1
}

ensure_bucket_exists() {
  echo "Checking S3 bucket '${BUCKET_NAME}' in Yandex Cloud..."
  if command_exists s3cmd; then
    if s3cmd info "s3://${BUCKET_NAME}" >/dev/null 2>&1; then
      echo "Bucket exists: ${BUCKET_NAME} (via s3cmd)"
    else
      echo "Bucket not found. Creating via s3cmd..."
      s3cmd mb "s3://${BUCKET_NAME}"
      echo "Enabling versioning on bucket..."
      s3cmd versioning "s3://${BUCKET_NAME}" --enable
      echo "Bucket created and versioning enabled."
    fi
    return
  fi

  if command_exists aws; then
    if aws --endpoint-url=https://storage.yandexcloud.net s3api head-bucket --bucket "${BUCKET_NAME}" >/dev/null 2>&1; then
      echo "Bucket exists: ${BUCKET_NAME} (via aws cli)"
    else
      echo "Bucket not found. Creating via aws cli..."
      aws --endpoint-url=https://storage.yandexcloud.net s3api create-bucket --bucket "${BUCKET_NAME}"
      echo "Enabling versioning on bucket..."
      aws --endpoint-url=https://storage.yandexcloud.net s3api put-bucket-versioning \
        --bucket "${BUCKET_NAME}" \
        --versioning-configuration Status=Enabled
      echo "Bucket created and versioning enabled."
    fi
    return
  fi

  echo "Warning: Neither s3cmd nor aws CLI found. Skipping bucket bootstrap. Assuming bucket exists (${BUCKET_NAME})." >&2
  return 0
}

create_backend_file() {
  local module_key="$1"
  local tmpfile
  tmpfile="$(mktemp)"
  cat > "$tmpfile" <<EOF
bucket         = "${BUCKET_NAME}"
key            = "${module_key}"
region         = "${REGION}"
endpoints = {
  s3 = "https://storage.yandexcloud.net"
}
use_path_style = true
skip_credentials_validation = true
skip_region_validation = true
skip_requesting_account_id = true
skip_metadata_api_check = true
access_key = "${ACCESS_KEY}"
secret_key = "${SECRET_KEY}"
EOF
  echo "$tmpfile"
}

init_module() {
  local module_dir="$1"
  local state_key="$2"
  echo "Initializing Terraform in: ${module_dir} (state key: ${state_key})"
  local bcfg
  bcfg="$(create_backend_file "$state_key")"
  # Use -chdir to avoid changing working directory
  echo "Using backend: bucket='${BUCKET_NAME}', key='${state_key}', region='${REGION}', endpoint='https://storage.yandexcloud.net'"
  # Decide whether to migrate local state or just reconfigure
  local init_flags=("-backend-config=$bcfg")
  if [[ -f "$module_dir/terraform.tfstate" && -s "$module_dir/terraform.tfstate" ]]; then
    # Local state exists: migrate it to the configured backend
    init_flags=("-migrate-state" "${init_flags[@]}")
  else
    # First-time init or no local state: just (re)configure backend
    init_flags=("-reconfigure" "${init_flags[@]}")
  fi
  terraform -chdir="$module_dir" init "${init_flags[@]}"
  rm -f "$bcfg"
}

# Modules and their state keys inside the bucket
DASHBOARD_DIR="$TF_DIR/dashboard"
KUBER_DIR="$TF_DIR/tbs_kuber"

if [[ ! -d "$DASHBOARD_DIR" ]]; then
  echo "Error: Directory not found: $DASHBOARD_DIR"; exit 1
fi
if [[ ! -d "$KUBER_DIR" ]]; then
  echo "Error: Directory not found: $KUBER_DIR"; exit 1
fi

# Ensure backend bucket exists before initializing modules
ensure_bucket_exists

if [[ "$DO_DASHBOARD" == "true" ]]; then
  init_module "$DASHBOARD_DIR" "dashboard/terraform.tfstate"
fi
if [[ "$DO_TBS" == "true" ]]; then
  init_module "$KUBER_DIR" "tbs_kuber/terraform.tfstate"
fi

echo "Backends initialized in bucket '${BUCKET_NAME}'."

if [[ "$DO_APPLY" != "true" ]]; then
  echo "Done. Init only (no apply)."
  exit 0
fi

echo "Starting terraform apply for selected modules..."

set -x
if [[ "$DO_DASHBOARD" == "true" ]]; then
  terraform -chdir="$DASHBOARD_DIR" apply -auto-approve
fi
if [[ "$DO_TBS" == "true" ]]; then
  terraform -chdir="$KUBER_DIR" apply -auto-approve
fi
set +x

echo "Done. Init and apply completed."

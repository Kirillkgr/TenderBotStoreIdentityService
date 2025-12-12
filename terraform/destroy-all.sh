#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")"/.. && pwd)"
TF_DIR="$(cd "$(dirname "$0")" && pwd)"

# Parse args
DESTROY_TARGET="all"  # all|dashboard|tbs
while [[ $# -gt 0 ]]; do
  case "$1" in
    --bucket)
      BUCKET_NAME="$2"; shift 2;;
    --access-key)
      ACCESS_KEY="$2"; shift 2;;
    --secret-key)
      SECRET_KEY="$2"; shift 2;;
    --region)
      REGION="$2"; shift 2;;
    -d|--dashboard)
      DESTROY_TARGET="dashboard"; shift 1;;
    -t|--tbs|--tbs-only)
      DESTROY_TARGET="tbs"; shift 1;;
    -h|--help)
      echo "Usage: $0 [-d|--dashboard] [-t|--tbs] [--bucket <name>] [--access-key <id>] [--secret-key <key>] [--region <ru-central1>]";
      echo "  Default: destroy BOTH modules (dashboard and tbs_kuber).";
      echo "  -d/--dashboard: only dashboard.";
      echo "  -t/--tbs: only tbs_kuber.";
      exit 0;;
    *)
      echo "Unknown arg: $1"; exit 1;;
  esac
done

# Defaults (can be overridden by args or .env)
BUCKET_NAME="${BUCKET_NAME:-}" 
ACCESS_KEY="${ACCESS_KEY:-}"
SECRET_KEY="${SECRET_KEY:-}"
REGION="${REGION:-ru-central1}"

# Load .env from repo root if present
if [[ -f "$ROOT_DIR/.env" ]]; then
  set -a
  # shellcheck disable=SC1090
  . "$ROOT_DIR/.env"
  set +a
fi

# Prefer infra bucket credentials for terraform backend
if [[ -n "${BUCKET_INFRA_NAME:-}" ]]; then BUCKET_NAME="$BUCKET_INFRA_NAME"; fi
if [[ -n "${ACCESS_KEY_INFRA_ID:-}" ]]; then ACCESS_KEY="$ACCESS_KEY_INFRA_ID"; fi
if [[ -n "${SECRET_KEY_INFRA:-}" ]]; then SECRET_KEY="$SECRET_KEY_INFRA"; fi
if [[ -n "${BUCKET_INFRA_REGION:-}" ]]; then REGION="$BUCKET_INFRA_REGION"; fi

if [[ -z "${BUCKET_NAME:-}" || -z "${ACCESS_KEY:-}" || -z "${SECRET_KEY:-}" ]]; then
  echo "Error: Missing credentials. Provide --bucket/--access-key/--secret-key or set BUCKET_INFRA_NAME, ACCESS_KEY_INFRA_ID, SECRET_KEY_INFRA in .env"
  exit 1
fi

export AWS_ACCESS_KEY_ID="$ACCESS_KEY"
export AWS_SECRET_ACCESS_KEY="$SECRET_KEY"
export AWS_DEFAULT_REGION="$REGION"
export AWS_EC2_METADATA_DISABLED=true

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
  local bcfg
  bcfg="$(create_backend_file "$state_key")"
  local init_flags=("-backend-config=$bcfg")
  if [[ -f "$module_dir/terraform.tfstate" && -s "$module_dir/terraform.tfstate" ]]; then
    init_flags=("-migrate-state" "${init_flags[@]}")
  else
    init_flags=("-reconfigure" "${init_flags[@]}")
  fi
  terraform -chdir="$module_dir" init "${init_flags[@]}"
  rm -f "$bcfg"
}

DASHBOARD_DIR="$TF_DIR/dashboard"
KUBER_DIR="$TF_DIR/tbs_kuber"

if [[ ! -d "$DASHBOARD_DIR" ]]; then
  echo "Error: Directory not found: $DASHBOARD_DIR"; exit 1
fi
if [[ ! -d "$KUBER_DIR" ]]; then
  echo "Error: Directory not found: $KUBER_DIR"; exit 1
fi

init_module "$DASHBOARD_DIR" "dashboard/terraform.tfstate"
init_module "$KUBER_DIR" "tbs_kuber/terraform.tfstate"

echo "Backends initialized in bucket '${BUCKET_NAME}'. Starting terraform destroy for target: ${DESTROY_TARGET}..."

set -x
case "$DESTROY_TARGET" in
  dashboard)
    terraform -chdir="$DASHBOARD_DIR" destroy -auto-approve -input=false
    ;;
  tbs)
    terraform -chdir="$KUBER_DIR" destroy -auto-approve -input=false
    ;;
  all)
    terraform -chdir="$KUBER_DIR" destroy -auto-approve -input=false
    terraform -chdir="$DASHBOARD_DIR" destroy -auto-approve -input=false
    ;;
esac
set +x

echo "Done. Destroy completed for target: ${DESTROY_TARGET}."

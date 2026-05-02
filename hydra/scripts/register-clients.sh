#!/usr/bin/env bash
# =============================================================================
# register-clients.sh
# Registers dev OAuth2 clients defined in hydra/clients/dev-clients.json
# with the local Hydra admin API.
#
# Run this after start-dev.sh has Hydra up and healthy.
# Safe to run multiple times — existing clients are updated, not duplicated.
# =============================================================================

set -euo pipefail

HYDRA_ADMIN_URL="http://localhost:4445"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
CLIENTS_FILE="${SCRIPT_DIR}/../clients/dev-clients.json"

# -----------------------------------------------------------------------------
# Check dependencies
# -----------------------------------------------------------------------------
if ! command -v curl &>/dev/null; then
  echo "ERROR: curl is required. Please install curl and retry."
  exit 1
fi

if ! command -v jq &>/dev/null; then
  echo "ERROR: jq is required. Please install jq and retry."
  echo "  macOS:  brew install jq"
  echo "  RHEL:   sudo dnf install jq"
  echo "  Windows (Git Bash): download from https://stedolan.github.io/jq/"
  exit 1
fi

# -----------------------------------------------------------------------------
# Wait for Hydra admin API to be ready
# -----------------------------------------------------------------------------
echo "Waiting for Hydra admin API at ${HYDRA_ADMIN_URL}..."
MAX_RETRIES=15
RETRY=0
until curl -sf "${HYDRA_ADMIN_URL}/health/ready" > /dev/null 2>&1; do
  RETRY=$((RETRY + 1))
  if [ "${RETRY}" -ge "${MAX_RETRIES}" ]; then
    echo "ERROR: Hydra admin API did not become ready after ${MAX_RETRIES} attempts."
    echo "Make sure start-dev.sh is running first."
    exit 1
  fi
  echo "  Not ready yet, retrying in 2s... (${RETRY}/${MAX_RETRIES})"
  sleep 2
done
echo "Hydra is ready."
echo ""

# -----------------------------------------------------------------------------
# Register each client
# -----------------------------------------------------------------------------
CLIENT_COUNT=$(jq '. | length' "${CLIENTS_FILE}")
echo "Registering ${CLIENT_COUNT} client(s) from ${CLIENTS_FILE}..."
echo ""

for i in $(seq 0 $((CLIENT_COUNT - 1))); do
  CLIENT=$(jq ".[$i]" "${CLIENTS_FILE}")
  CLIENT_ID=$(echo "${CLIENT}" | jq -r '.client_id')
  CLIENT_NAME=$(echo "${CLIENT}" | jq -r '.client_name')

  echo "Processing client: ${CLIENT_ID} (${CLIENT_NAME})"

  # Check if client already exists
  HTTP_STATUS=$(curl -s -o /dev/null -w "%{http_code}" \
    "${HYDRA_ADMIN_URL}/admin/clients/${CLIENT_ID}")

  if [ "${HTTP_STATUS}" = "200" ]; then
    echo "  Client exists — updating..."
    RESPONSE=$(curl -s -w "\n%{http_code}" -X PUT \
      "${HYDRA_ADMIN_URL}/admin/clients/${CLIENT_ID}" \
      -H "Content-Type: application/json" \
      -d "${CLIENT}")
    BODY=$(echo "${RESPONSE}" | head -n -1)
    STATUS=$(echo "${RESPONSE}" | tail -n 1)
  else
    echo "  Client not found — creating..."
    RESPONSE=$(curl -s -w "\n%{http_code}" -X POST \
      "${HYDRA_ADMIN_URL}/admin/clients" \
      -H "Content-Type: application/json" \
      -d "${CLIENT}")
    BODY=$(echo "${RESPONSE}" | head -n -1)
    STATUS=$(echo "${RESPONSE}" | tail -n 1)
  fi

  if [ "${STATUS}" = "200" ] || [ "${STATUS}" = "201" ]; then
    echo "  OK (HTTP ${STATUS})"
  else
    echo "  ERROR (HTTP ${STATUS}): ${BODY}"
    exit 1
  fi
  echo ""
done

echo "============================================================"
echo "  All clients registered successfully."
echo ""
echo "  Test client:        test-saas-app"
echo "  Admin console:      idm-admin-console"
echo ""
echo "  Start an auth flow:"
echo "  http://localhost:4444/oauth2/auth?response_type=code"
echo "    &client_id=test-saas-app"
echo "    &redirect_uri=http://localhost:9000/callback"
echo "    &scope=openid+offline_access"
echo "    &state=random-state"
echo "    &code_challenge=<pkce-challenge>"
echo "    &code_challenge_method=S256"
echo "============================================================"

#!/usr/bin/env bash
# =============================================================================
# start-dev.sh
# Downloads the correct Hydra v26.2.0 binary for the current OS/arch if not
# already present, then starts Hydra in dev mode using SQLite storage.
#
# Supported platforms:
#   - macOS (Intel x86_64 and Apple Silicon arm64)
#   - Linux x86_64 (RHEL and others)
#   - Windows x86_64 via Git Bash / WSL
# =============================================================================

set -euo pipefail

HYDRA_VERSION="26.2.0"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"
HYDRA_BIN_DIR="${REPO_ROOT}/hydra/bin"
CONFIG_FILE="${REPO_ROOT}/hydra/config/hydra.dev.yml"

mkdir -p "${HYDRA_BIN_DIR}"

# -----------------------------------------------------------------------------
# Detect OS and architecture
# -----------------------------------------------------------------------------
OS="$(uname -s)"
ARCH="$(uname -m)"

case "${OS}" in
  Darwin)
    case "${ARCH}" in
      arm64)
        PLATFORM="macOS_sqlite_arm64"
        ;;
      x86_64)
        PLATFORM="macOS_sqlite_64bit"
        ;;
      *)
        echo "ERROR: Unsupported macOS architecture: ${ARCH}"
        exit 1
        ;;
    esac
    BINARY_NAME="hydra"
    ;;
  Linux)
    case "${ARCH}" in
      x86_64)
        PLATFORM="linux_sqlite_64bit"
        ;;
      aarch64)
        PLATFORM="linux_sqlite_arm64"
        ;;
      *)
        echo "ERROR: Unsupported Linux architecture: ${ARCH}"
        exit 1
        ;;
    esac
    BINARY_NAME="hydra"
    ;;
  MINGW*|MSYS*|CYGWIN*)
    # Git Bash on Windows — note: Windows build does not include SQLite
    # Use the PowerShell script (start-dev.ps1) for Windows instead,
    # which handles the .zip format. Git Bash users can also use it via:
    #   powershell.exe -ExecutionPolicy Bypass -File hydra/scripts/start-dev.ps1
    echo "WARNING: Running on Windows via Git Bash."
    echo "The recommended approach on Windows is to use the PowerShell script:"
    echo "  powershell.exe -ExecutionPolicy Bypass -File hydra/scripts/start-dev.ps1"
    echo ""
    echo "Attempting Git Bash download anyway..."
    PLATFORM="windows_64bit"
    BINARY_NAME="hydra.exe"
    ;;
  *)
    echo "ERROR: Unsupported OS: ${OS}"
    echo "Supported: macOS, Linux (x86_64/arm64), Windows (via Git Bash)"
    exit 1
    ;;
esac

HYDRA_BINARY="${HYDRA_BIN_DIR}/${BINARY_NAME}"

# Windows releases are .zip; all others are .tar.gz
if [[ "${OS}" == MINGW* ]] || [[ "${OS}" == MSYS* ]] || [[ "${OS}" == CYGWIN* ]]; then
  ARCHIVE_EXT="zip"
else
  ARCHIVE_EXT="tar.gz"
fi

ARCHIVE_NAME="hydra_${HYDRA_VERSION}-${PLATFORM}.${ARCHIVE_EXT}"
DOWNLOAD_URL="https://github.com/ory/hydra/releases/download/v${HYDRA_VERSION}/${ARCHIVE_NAME}"

# -----------------------------------------------------------------------------
# Download binary if not already present
# -----------------------------------------------------------------------------
if [ ! -f "${HYDRA_BINARY}" ]; then
  echo "Hydra binary not found. Downloading v${HYDRA_VERSION} for ${PLATFORM}..."
  echo "URL: ${DOWNLOAD_URL}"

  TMP_DIR="$(mktemp -d)"
  trap 'rm -rf "${TMP_DIR}"' EXIT

  if command -v curl &>/dev/null; then
    curl -fsSL "${DOWNLOAD_URL}" -o "${TMP_DIR}/${ARCHIVE_NAME}"
  elif command -v wget &>/dev/null; then
    wget -q "${DOWNLOAD_URL}" -O "${TMP_DIR}/${ARCHIVE_NAME}"
  else
    echo "ERROR: Neither curl nor wget found. Please install one and retry."
    exit 1
  fi

  echo "Extracting..."
  if [ "${ARCHIVE_EXT}" = "zip" ]; then
    if command -v unzip &>/dev/null; then
      unzip -q "${TMP_DIR}/${ARCHIVE_NAME}" -d "${TMP_DIR}"
    else
      echo "ERROR: unzip not found. Please install unzip or use start-dev.ps1 on Windows."
      exit 1
    fi
  else
    tar -xzf "${TMP_DIR}/${ARCHIVE_NAME}" -C "${TMP_DIR}"
  fi

  # The binary is at the root of the archive
  cp "${TMP_DIR}/${BINARY_NAME}" "${HYDRA_BINARY}"
  chmod +x "${HYDRA_BINARY}"

  echo "Hydra binary installed at: ${HYDRA_BINARY}"
else
  echo "Hydra binary already present at: ${HYDRA_BINARY}"
fi

# Verify the binary works
echo ""
echo "Hydra version:"
"${HYDRA_BINARY}" version

# -----------------------------------------------------------------------------
# Run database migration (idempotent for SQLite)
# -----------------------------------------------------------------------------
echo ""
echo "Running database migration..."
"${HYDRA_BINARY}" migrate sql --yes --config "${CONFIG_FILE}"

# -----------------------------------------------------------------------------
# Start Hydra
# -----------------------------------------------------------------------------
echo ""
echo "============================================================"
echo "  Starting Hydra v${HYDRA_VERSION} (dev mode)"
echo "  Public:  http://localhost:4444"
echo "  Admin:   http://localhost:4445"
echo "  Config:  ${CONFIG_FILE}"
echo "============================================================"
echo ""

"${HYDRA_BINARY}" serve all --config "${CONFIG_FILE}"

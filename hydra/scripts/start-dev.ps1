# =============================================================================
# start-dev.ps1
# Windows PowerShell equivalent of start-dev.sh
# Downloads Hydra v26.2.0 for Windows x86_64 if not present, then starts it.
#
# Run from PowerShell (not CMD):
#   .\hydra\scripts\start-dev.ps1
# =============================================================================

$ErrorActionPreference = "Stop"

$HYDRA_VERSION = "26.2.0"
$SCRIPT_DIR   = Split-Path -Parent $MyInvocation.MyCommand.Path
$REPO_ROOT    = Resolve-Path (Join-Path $SCRIPT_DIR "../../")
$HYDRA_BIN_DIR = Join-Path $REPO_ROOT "hydra\bin"
$CONFIG_FILE   = Join-Path $REPO_ROOT "hydra\config\hydra.dev.yml"
$BINARY_NAME   = "hydra.exe"
$HYDRA_BINARY  = Join-Path $HYDRA_BIN_DIR $BINARY_NAME
$PLATFORM      = "windows_64bit"
$ARCHIVE_NAME  = "hydra_${HYDRA_VERSION}-${PLATFORM}.zip"
$DOWNLOAD_URL  = "https://github.com/ory/hydra/releases/download/v${HYDRA_VERSION}/${ARCHIVE_NAME}"

# Create bin dir if needed
if (-not (Test-Path $HYDRA_BIN_DIR)) {
    New-Item -ItemType Directory -Path $HYDRA_BIN_DIR | Out-Null
}

# -----------------------------------------------------------------------------
# Download binary if not already present
# -----------------------------------------------------------------------------
if (-not (Test-Path $HYDRA_BINARY)) {
    Write-Host "Hydra binary not found. Downloading v${HYDRA_VERSION} for Windows x86_64..."
    Write-Host "URL: $DOWNLOAD_URL"

    $TMP_DIR = Join-Path $env:TEMP "hydra-download"
    New-Item -ItemType Directory -Force -Path $TMP_DIR | Out-Null

    $ARCHIVE_PATH = Join-Path $TMP_DIR $ARCHIVE_NAME
    Invoke-WebRequest -Uri $DOWNLOAD_URL -OutFile $ARCHIVE_PATH

    Write-Host "Extracting..."
    Expand-Archive -Path $ARCHIVE_PATH -DestinationPath $TMP_DIR -Force

    Copy-Item (Join-Path $TMP_DIR $BINARY_NAME) $HYDRA_BINARY
    Remove-Item -Recurse -Force $TMP_DIR

    Write-Host "Hydra binary installed at: $HYDRA_BINARY"
} else {
    Write-Host "Hydra binary already present at: $HYDRA_BINARY"
}

# Verify
Write-Host ""
Write-Host "Hydra version:"
& $HYDRA_BINARY version

# -----------------------------------------------------------------------------
# Run database migration
# NOTE: The standard Windows Hydra binary does not include SQLite support.
# For local dev on Windows, override the DSN to use a local PostgreSQL or
# set HYDRA_DSN environment variable before running this script.
# Simplest option: install PostgreSQL locally or use the WSL bash script.
# -----------------------------------------------------------------------------
Write-Host ""
Write-Host "Running database migration..."
Write-Host "NOTE: If you see a 'sqlite not supported' error, see README for Windows DB setup."
& $HYDRA_BINARY migrate sql --yes --config $CONFIG_FILE

# -----------------------------------------------------------------------------
# Start Hydra
# -----------------------------------------------------------------------------
Write-Host ""
Write-Host "============================================================"
Write-Host "  Starting Hydra v${HYDRA_VERSION} (dev mode)"
Write-Host "  Public:  http://localhost:4444"
Write-Host "  Admin:   http://localhost:4445"
Write-Host "  Config:  $CONFIG_FILE"
Write-Host "============================================================"
Write-Host ""

& $HYDRA_BINARY serve all --config $CONFIG_FILE

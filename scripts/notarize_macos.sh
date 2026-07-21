#!/usr/bin/env bash
# =============================================================================
# notarize_macos.sh — Sign, DMG, notarize & staple the Alcedo Studio .app
# =============================================================================
# Prerequisites (CI or local):
#   - A valid Apple Developer certificate installed in the keychain
#   - An app-specific password stored in the keychain as
#     "ALCEDO_NOTARIZE_PASSWORD" (or set via env)
#   - The Apple ID and Team ID set via environment or command-line flags
#
# Usage:
#   ./scripts/notarize_macos.sh \
#     --app-path build/install/AlcedoStudio.app \
#     --identity "Developer ID Application: Your Name (TEAMID)" \
#     --apple-id dev@example.com \
#     --team-id TEAMID \
#     --password @keychain:ALCEDO_NOTARIZE_PASSWORD \
#     [--entitlements packaging/macOS/AlcedoStudio.entitlements] \
#     [--dmg-output build/AlcedoStudio.dmg] \
#     [--skip-dmg] \
#     [--skip-staple]
# =============================================================================
set -euo pipefail

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
repo_root="$(cd "${script_dir}/.." && pwd)"

# ── Defaults ──────────────────────────────────────────────────────────────────
app_path=""
signing_identity=""
entitlements="${repo_root}/packaging/macOS/AlcedoStudio.entitlements"
apple_id=""
team_id=""
password=""
keychain_profile=""
dmg_output=""
skip_dmg=0
skip_staple=0
verbose=0

# ── CLI ───────────────────────────────────────────────────────────────────────
usage() {
  cat <<USAGE
Usage: $0 [options]

Required:
  --app-path PATH         Path to the .app bundle to sign and notarize
  --identity ID           codesign signing identity (e.g. "Developer ID Application: ...")

Notarization credentials (at least one set required):
  --apple-id EMAIL        Apple ID for notarization
  --team-id TEAM          Team ID for notarization
  --password PWD          App-specific password (or @keychain: item)
  --keychain-profile NAME Keychain profile name (xcrun notarytool keychain-profile)

Optional:
  --entitlements PATH     Entitlements .plist (default: packaging/macOS/AlcedoStudio.entitlements)
  --dmg-output PATH       Output DMG path (default: <app_dir>/../AlcedoStudio.dmg)
  --skip-dmg              Skip DMG creation (notarize the .app directly)
  --skip-staple           Skip stapling the notarization ticket
  --verbose               Enable verbose output
  -h, --help              Show this help
USAGE
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --app-path)       app_path="$2"; shift 2 ;;
    --identity)       signing_identity="$2"; shift 2 ;;
    --entitlements)   entitlements="$2"; shift 2 ;;
    --apple-id)       apple_id="$2"; shift 2 ;;
    --team-id)        team_id="$2"; shift 2 ;;
    --password)       password="$2"; shift 2 ;;
    --keychain-profile) keychain_profile="$2"; shift 2 ;;
    --dmg-output)     dmg_output="$2"; shift 2 ;;
    --skip-dmg)       skip_dmg=1; shift ;;
    --skip-staple)    skip_staple=1; shift ;;
    --verbose)        verbose=1; shift ;;
    -h|--help)        usage; exit 0 ;;
    *)                echo "Unknown argument: $1" >&2; usage >&2; exit 2 ;;
  esac
done

# ── Validation ────────────────────────────────────────────────────────────────
if [[ -z "${app_path}" ]]; then
  echo "ERROR: --app-path is required" >&2; exit 1
fi
if [[ ! -d "${app_path}" ]]; then
  echo "ERROR: App bundle not found: ${app_path}" >&2; exit 1
fi
if [[ -z "${signing_identity}" ]]; then
  echo "ERROR: --identity is required" >&2; exit 1
fi

# Resolve absolute path
app_path="$(cd "$(dirname "${app_path}")" && pwd)/$(basename "${app_path}")"
bundle_name="$(basename "${app_path}" .app)"

if [[ -z "${dmg_output}" ]]; then
  dmg_output="$(dirname "${app_path}")/${bundle_name}.dmg"
fi

# ── Helper ────────────────────────────────────────────────────────────────────
log()  { echo "==> $*"; }
vlog() { (( verbose )) && echo "  [verbose] $*"; }
die()  { echo "FATAL: $*" >&2; exit 1; }

# ── Step 1: Sign the app bundle ───────────────────────────────────────────────
log "Signing ${bundle_name}.app with identity '${signing_identity}'"

# Sign all embedded frameworks, dylibs, and helpers first (depth-first).
# codesign requires signing nested code before the containing bundle.
sign_target() {
  local target="$1"
  local sign_args=(
    --force
    --sign "${signing_identity}"
    --options runtime
    --timestamp
  )
  if [[ -f "${entitlements}" ]]; then
    sign_args+=(--entitlements "${entitlements}")
  fi
  vlog "codesign ${sign_args[*]} ${target}"
  codesign "${sign_args[@]}" "${target}"
}

# Collect and sign all Mach-O binaries, frameworks, and plugins inside the bundle.
# Order matters: sign deeply-nested code first, then work outward.
log "  Signing nested code (frameworks, dylibs, helpers)..."
find "${app_path}" \( -name '*.framework' -o -name '*.dylib' -o -name '*.so' \) -print0 \
  | sort -z -r \
  | while IFS= read -r -d '' nested; do
      sign_target "${nested}"
    done

# Sign helper apps (e.g. app extensions, XPC services)
find "${app_path}" \( -name '*.app' -o -name '*.xpc' \) -print0 \
  | sort -z -r \
  | while IFS= read -r -d '' nested; do
      sign_target "${nested}"
    done

# Finally, sign the main bundle
log "  Signing main bundle..."
sign_target "${app_path}"

# Verify the signature
log "  Verifying signature..."
codesign --verify --deep --strict --verbose=2 "${app_path}" 2>&1 || die "Code signature verification failed"
spctl --assess --type execute --verbose=2 "${app_path}" 2>&1 || true
log "  Signature OK"

# ── Step 2: Create DMG ───────────────────────────────────────────────────────
if (( skip_dmg )); then
  log "Skipping DMG creation (--skip-dmg)"
else
  log "Creating DMG: ${dmg_output}"

  dmg_staging="$(mktemp -d)/dmg_staging"
  mkdir -p "${dmg_staging}"

  # Symlink the .app into the staging directory
  ln -sf "$(dirname "${app_path}")" "${dmg_staging}/${bundle_name}.app"

  # Create a symlink to /Applications for drag-and-drop install
  ln -sf /Applications "${dmg_staging}/Applications"

  # Remove any existing DMG
  rm -f "${dmg_output}"

  hdiutil create \
    -volname "${bundle_name}" \
    -srcfolder "${dmg_staging}" \
    -ov \
    -format UDZO \
    "${dmg_output}"

  rm -rf "${dmg_staging}"
  log "  DMG created: ${dmg_output}"

  # Sign the DMG itself
  log "  Signing DMG..."
  codesign --force --sign "${signing_identity}" --timestamp "${dmg_output}"
  log "  DMG signed OK"
fi

# ── Step 3: Submit for notarization ───────────────────────────────────────────
notarize_path="${app_path}"
if (( ! skip_dmg )) && [[ -f "${dmg_output}" ]]; then
  notarize_path="${dmg_output}"
fi

log "Submitting for notarization: ${notarize_path}"

notary_args=()
if [[ -n "${keychain_profile}" ]]; then
  notary_args+=(--keychain-profile "${keychain_profile}")
else
  if [[ -z "${apple_id}" || -z "${team_id}" ]]; then
    die "Notarization requires --apple-id and --team-id (or --keychain-profile)"
  fi
  notary_args+=(
    --apple-id "${apple_id}"
    --team-id "${team_id}"
    --password "${password:-@env:NOTARIZE_PASSWORD}"
  )
fi

# Submit and capture the submission ID
log "  Uploading to Apple notarization service..."
submit_output="$(xcrun notarytool submit "${notarize_path}" "${notary_args[@]}" --wait --format json 2>&1)" || {
  echo "notarytool submit failed:" >&2
  echo "${submit_output}" >&2
  exit 1
}

# Extract the submission ID from the output
submission_id="$(echo "${submit_output}" | grep -oE '"id":"[a-f0-9-]+"' | head -1 | cut -d'"' -f4 || true)"
if [[ -n "${submission_id}" ]]; then
  log "  Submission ID: ${submission_id}"
fi

# --wait makes notarytool block until the result is available.
# Check the result from the output.
if echo "${submit_output}" | grep -q '"status":"Accepted"'; then
  log "  Notarization ACCEPTED"
else
  log "  Fetching notarization log for details..."
  if [[ -n "${submission_id}" ]]; then
    xcrun notarytool log "${submission_id}" "${notary_args[@]}" 2>&1 || true
  fi
  die "Notarization FAILED. See log above for details."
fi

# ── Step 4: Staple the notarization ticket ────────────────────────────────────
if (( skip_staple )); then
  log "Skipping staple (--skip-staple)"
else
  log "Stapling notarization ticket to ${notarize_path}..."
  xcrun stapler staple "${notarize_path}" 2>&1 || die "Stapling failed"
  log "  Staple OK"

  # Verify the staple
  spctl --assess --type execute --verbose=2 "${app_path}" 2>&1 || true
fi

# ── Step 5: Final verification ────────────────────────────────────────────────
log "Final verification..."
codesign --verify --deep --strict "${app_path}" 2>&1 || die "Final signature verification failed"

if (( ! skip_dmg )) && [[ -f "${dmg_output}" ]]; then
  codesign --verify --deep --strict "${dmg_output}" 2>&1 || die "Final DMG signature verification failed"
fi

log "========================================"
log "  Notarization Complete"
log "========================================"
log "  App:  ${app_path}"
if (( ! skip_dmg )); then
  log "  DMG:  ${dmg_output}"
fi
log "  Identity: ${signing_identity}"

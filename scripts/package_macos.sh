#!/usr/bin/env bash
set -euo pipefail

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
repo_root="$(cd "${script_dir}/.." && pwd)"

preset="macos_release"
build_dir="${repo_root}/build/macos-release"
install_dir="${repo_root}/build/install"
package_out_dir="${repo_root}/build/macos-release/package"
bundle_name="AlcedoStudio"
jobs="8"
qt_prefix=""
require_metal_assets=1
codesign_identity="-"
onnxruntime_dir="${ALCEDO_ORT_DIR:-}"
entitlements_file="${ALCEDO_ENTITLEMENTS:-}"
codesign_options=""
codesign_options_set=0
codesign_timestamp="OFF"
codesign_timestamp_set=0

usage() {
  cat <<USAGE
Usage: $0 [options]

Build and package the macOS Alcedo Studio .app, DMG, and ZIP.

Options:
  --preset NAME              CMake configure/build preset (default: macos_release)
  --build-dir PATH           Build directory (default: build/macos-release)
  --install-dir PATH         CMake install prefix (default: build/install)
  --package-out-dir PATH     CPack output directory (default: build/macos-release/package)
  --bundle-name NAME         App bundle/executable name (default: AlcedoStudio)
  --qt-prefix PATH           Qt prefix containing bin/, lib/cmake/Qt6, plugins/, qml/
  --onnxruntime-dir PATH     ONNX Runtime root directory (containing lib/libonnxruntime*.dylib)
  --entitlements PATH        Entitlements plist for codesign (required for ORT JIT)
  --codesign-identity ID     macOS signing identity (default: '-' for ad-hoc; empty disables signing)
  --codesign-options VALUE   Semicolon-separated codesign options (default: empty for ad-hoc)
  --codesign-timestamp       Request a trusted timestamp when signing
  --no-codesign              Disable bundle signing
  --jobs N                   Parallel build jobs (default: 8)
  --skip-metal-asset-check   Do not require Metal metallib assets in verification
  -h, --help                 Show this help
USAGE
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --preset)
      preset="$2"
      shift 2
      ;;
    --build-dir)
      build_dir="$2"
      shift 2
      ;;
    --install-dir)
      install_dir="$2"
      shift 2
      ;;
    --package-out-dir)
      package_out_dir="$2"
      shift 2
      ;;
    --bundle-name)
      bundle_name="$2"
      shift 2
      ;;
    --qt-prefix)
      qt_prefix="$2"
      shift 2
      ;;
    --onnxruntime-dir)
      onnxruntime_dir="$2"
      shift 2
      ;;
    --entitlements)
      entitlements_file="$2"
      shift 2
      ;;
    --codesign-identity)
      codesign_identity="$2"
      shift 2
      ;;
    --codesign-options)
      codesign_options="$2"
      codesign_options_set=1
      shift 2
      ;;
    --codesign-timestamp)
      codesign_timestamp="ON"
      codesign_timestamp_set=1
      shift
      ;;
    --no-codesign)
      codesign_identity=""
      shift
      ;;
    --jobs)
      jobs="$2"
      shift 2
      ;;
    --skip-metal-asset-check)
      require_metal_assets=0
      shift
      ;;
    --version)
      ALCEDO_VERSION="$2"
      shift 2
      ;;
    -h|--help)
      usage
      exit 0
      ;;
    *)
      echo "Unknown argument: $1" >&2
      usage >&2
      exit 2
      ;;
  esac
done

if [[ -n "$codesign_identity" && "$codesign_identity" != "-" ]]; then
  if [[ "$codesign_options_set" -eq 0 ]]; then
    codesign_options="--options;runtime"
  fi
  if [[ "$codesign_timestamp_set" -eq 0 ]]; then
    codesign_timestamp="ON"
  fi
fi

duckdb_extensions_dir="${build_dir}/duckdb_extensions"

resolve_duckdb_extension() {
  local extension_name="$1"
  local env_name="$2"
  local extension_file_name="$3"
  local configured_path="${!env_name:-}"

  if [[ -n "$configured_path" ]]; then
    if [[ ! -f "$configured_path" ]]; then
      echo "${env_name} points to a missing file: ${configured_path}" >&2
      exit 1
    fi
    printf '%s' "$configured_path"
    return
  fi

  if ! command -v duckdb >/dev/null 2>&1; then
    echo "duckdb CLI is required to prepare ${extension_name}; install Homebrew duckdb or set ${env_name}." >&2
    exit 1
  fi

  mkdir -p "$duckdb_extensions_dir"
  duckdb -c "SET extension_directory='${duckdb_extensions_dir}'; INSTALL ${extension_name};" >/dev/null

  local resolved_path
  resolved_path="$(find "$duckdb_extensions_dir" -name "${extension_file_name}" -type f | head -n1)"
  if [[ -z "$resolved_path" ]]; then
    echo "Failed to locate installed ${extension_file_name} under ${duckdb_extensions_dir}" >&2
    find "$duckdb_extensions_dir" -type f >&2 || true
    exit 1
  fi

  printf '%s' "$resolved_path"
}

alcedo_duckdb_vss_extension="$(resolve_duckdb_extension vss ALCEDO_DUCKDB_VSS_EXTENSION vss.duckdb_extension)"
alcedo_duckdb_fts_extension="$(resolve_duckdb_extension fts ALCEDO_DUCKDB_FTS_EXTENSION fts.duckdb_extension)"
export ALCEDO_DUCKDB_VSS_EXTENSION="$alcedo_duckdb_vss_extension"
export ALCEDO_DUCKDB_FTS_EXTENSION="$alcedo_duckdb_fts_extension"

echo "========================================"
echo "  Alcedo Studio macOS Packager"
echo "========================================"
echo
echo "DuckDB VSS extension: ${ALCEDO_DUCKDB_VSS_EXTENSION}"
echo "DuckDB FTS extension: ${ALCEDO_DUCKDB_FTS_EXTENSION}"
echo

configure_args=(
  --preset "$preset"
  -B "$build_dir"
  "-DCMAKE_INSTALL_PREFIX=${install_dir}"
  "-DALCEDO_MACOS_BUNDLE=ON"
  "-DALCEDO_MACOS_BUNDLE_NAME=${bundle_name}"
  "-DALCEDO_MACOS_CODESIGN_IDENTITY=${codesign_identity}"
  "-DALCEDO_MACOS_CODESIGN_OPTIONS=${codesign_options}"
  "-DALCEDO_MACOS_CODESIGN_TIMESTAMP=${codesign_timestamp}"
  "-DALCEDO_DUCKDB_VSS_EXTENSION=${ALCEDO_DUCKDB_VSS_EXTENSION}"
  "-DALCEDO_DUCKDB_FTS_EXTENSION=${ALCEDO_DUCKDB_FTS_EXTENSION}"
)
if [[ -n "$qt_prefix" ]]; then
  configure_args+=("-DALCEDO_QT_PREFIX=${qt_prefix}")
fi
# Add CMAKE_PREFIX_PATH from environment (for OpenCV etc.)
if [[ -n "${CMAKE_PREFIX_PATH:-}" ]]; then
  configure_args+=("-DCMAKE_PREFIX_PATH=${CMAKE_PREFIX_PATH}")
fi

echo "Configuring CMake with preset '${preset}' ..."
printf '> cmake'
printf ' %q' "${configure_args[@]}"
printf '\n'
cmake "${configure_args[@]}"

echo
echo "Building install target ..."
echo "Note: Qt deployment uses macdeployqt and can take 10+ minutes on release builds."
build_args=(--build "$build_dir" --target install --parallel "$jobs")
printf '> cmake'
printf ' %q' "${build_args[@]}"
printf '\n'
cmake "${build_args[@]}"

echo
echo "Verifying install tree ..."
verify_args=(
  --install-dir "$install_dir"
  --bundle-name "$bundle_name"
)
if [[ "$require_metal_assets" -eq 0 ]]; then
  verify_args+=(--skip-metal-asset-check)
fi
"${script_dir}/verify_macos_install_tree.sh" "${verify_args[@]}"

# --- Deploy ONNX Runtime dylib into the framework bundle ---
app_bundle="${install_dir}/${bundle_name}.app"
frameworks_dir="${app_bundle}/Contents/Frameworks"
macos_dir="${app_bundle}/Contents/MacOS"

if [[ -n "$onnxruntime_dir" && -d "$onnxruntime_dir" && -d "$app_bundle" ]]; then
  echo
  echo "Deploying ONNX Runtime into framework bundle ..."
  mkdir -p "$frameworks_dir"

  # Copy ORT dylibs
  for ort_dylib in "${onnxruntime_dir}/lib/"libonnxruntime*.dylib; do
    [[ -f "$ort_dylib" ]] || continue
    ort_basename="$(basename "$ort_dylib")"
    cp -fL "$ort_dylib" "${frameworks_dir}/${ort_basename}"
    chmod u+w "${frameworks_dir}/${ort_basename}"

    # Update install_name to use @rpath for bundling
    /usr/bin/install_name_tool -id "@rpath/${ort_basename}" "${frameworks_dir}/${ort_basename}" 2>/dev/null || true

    echo "  Deployed and patched: ${ort_basename}"
  done

  # Sign the ORT dylibs (ad-hoc or with configured identity)
  if [[ -n "$codesign_identity" ]]; then
    echo "Signing ONNX Runtime dylibs ..."
    for ort_dylib in "${frameworks_dir}/"libonnxruntime*.dylib; do
      [[ -f "$ort_dylib" ]] || continue
      sign_args=(--force --sign "$codesign_identity")
      if [[ -n "$entitlements_file" && -f "$entitlements_file" ]]; then
        sign_args+=(--entitlements "$entitlements_file")
      fi
      if [[ "$codesign_timestamp" == "ON" ]]; then
        sign_args+=(--timestamp)
      else
        sign_args+=(--timestamp=none)
      fi
      /usr/bin/codesign "${sign_args[@]}" "$ort_dylib" || {
        echo "WARNING: codesign failed for $(basename "$ort_dylib")" >&2
      }
    done
  fi
fi

# --- Deploy model files into the framework bundle ---
models_src="${repo_root}/alcedo_studio/src/config/models"
models_dest="${macos_dir}/models"
if [[ -d "$models_src" && -d "$macos_dir" ]]; then
  echo "Deploying model files into framework bundle ..."
  rm -rf "$models_dest"
  cp -r "$models_src" "$models_dest"
  echo "  Model files deployed to ${models_dest}"
fi

# --- Ensure entitlements cover ONNX Runtime JIT ---
if [[ -n "$entitlements_file" && -f "$entitlements_file" ]]; then
  echo "Using entitlements from: ${entitlements_file}"
elif [[ -n "$codesign_identity" && "$codesign_identity" != "-" ]]; then
  # Auto-generate entitlements with JIT support for ORT
  echo "NOTE: No entitlements file specified. ORT JIT (dynamic library loading) may require"
  echo "  com.apple.security.cs.allow-unsigned-executable-memory and"
  echo "  com.apple.security.cs.allow-jit entitlements for notarized distribution."
  echo "  Pass --entitlements path/to/entitlements.plist to include them."
fi

echo
echo "Running CPack ..."
mkdir -p "$package_out_dir"
cpack_args=(--config "${build_dir}/CPackConfig.cmake" -B "$package_out_dir")
printf '> cpack'
printf ' %q' "${cpack_args[@]}"
printf '\n'
cpack "${cpack_args[@]}"

staging_root="${package_out_dir}/_CPack_Packages"
if [[ -d "$staging_root" ]]; then
  echo
  echo "Verifying CPack staging apps ..."
  while IFS= read -r -d '' staged_app; do
    staged_install_dir="$(dirname "$staged_app")"
    staged_verify_args=(
      --install-dir "$staged_install_dir"
      --bundle-name "$bundle_name"
    )
    if [[ "$require_metal_assets" -eq 0 ]]; then
      staged_verify_args+=(--skip-metal-asset-check)
    fi
    "${script_dir}/verify_macos_install_tree.sh" "${staged_verify_args[@]}"
  done < <(find "$staging_root" -name "${bundle_name}.app" -type d -print0)
fi

echo
echo "========================================"
echo "  Packaging Complete"
echo "========================================"
find "$package_out_dir" -maxdepth 1 \( -name '*.dmg' -o -name '*.zip' \) -print | sort
echo

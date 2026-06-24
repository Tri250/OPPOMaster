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

echo "========================================"
echo "  Alcedo Studio macOS Packager"
echo "========================================"
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
)
if [[ -n "$qt_prefix" ]]; then
  configure_args+=("-DALCEDO_QT_PREFIX=${qt_prefix}")
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

#!/usr/bin/env bash
# package_linux.sh — Build AppImage and .deb for AlcedoStudio
# Usage: ./scripts/package_linux.sh [--appimage|--deb] [--build-dir <dir>]
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
BUILD_DIR="${PROJECT_ROOT}/build-package"
BUILD_TYPE="Release"
MAKE_APPIMAGE=true
MAKE_DEB=true
VCPKG_ROOT="${VCPKG_ROOT:-${PROJECT_ROOT}/vcpkg}"

# Parse arguments
while [[ $# -gt 0 ]]; do
  case "$1" in
    --appimage) MAKE_DEB=false; shift ;;
    --deb)      MAKE_APPIMAGE=false; shift ;;
    --build-dir) BUILD_DIR="$2"; shift 2 ;;
    --vcpkg-root) VCPKG_ROOT="$2"; shift 2 ;;
    --help)
      echo "Usage: $0 [--appimage|--deb] [--build-dir <dir>] [--vcpkg-root <dir>]"
      exit 0 ;;
    *) echo "Unknown option: $1" >&2; exit 1 ;;
  esac
done

APP_NAME="AlcedoStudio"
APP_NAME_LOWER="alcedo-studio"

# --- Dynamic version detection ---
detect_version() {
  # 1. From environment variable
  if [ -n "${ALCEDO_VERSION:-}" ]; then
    echo "${ALCEDO_VERSION}"
    return
  fi
  # 2. From git tag (e.g. v0.2.7 -> 0.2.7)
  if git -C "${PROJECT_ROOT}" describe --tags --exact-match 2>/dev/null | sed 's/^v//' ; then
    return
  fi
  # 3. From CMakeLists.txt project() command
  local cmake_ver
  cmake_ver="$(grep -m1 'project(' "${PROJECT_ROOT}/CMakeLists.txt" 2>/dev/null \
    | sed -n 's/.*VERSION[[:space:]]\+\([0-9]\+\.[0-9]\+\.[0-9]\+\).*/\1/p')"
  if [ -n "${cmake_ver}" ]; then
    echo "${cmake_ver}"
    return
  fi
  # 4. Fallback
  echo "0.0.0"
}
VERSION="$(detect_version)"
ARCH="$(dpkg-architecture -qDEB_HOST_ARCH 2>/dev/null || echo amd64)"

# --- ONNX Runtime configuration ---
ORT_VERSION="${ORT_VERSION:-1.20.1}"
ORT_BASE_URL="https://github.com/microsoft/onnxruntime/releases/download/v${ORT_VERSION}"
ORT_DIR="${PROJECT_ROOT}/third_party/onnxruntime-linux"

download_onnxruntime() {
  if [ -d "${ORT_DIR}" ]; then
    echo "ONNX Runtime already downloaded at ${ORT_DIR}"
    return
  fi
  echo ">>> Downloading ONNX Runtime v${ORT_VERSION}..."
  mkdir -p "${ORT_DIR}"
  local ort_pkg="onnxruntime-linux-x64-${ORT_VERSION}.tgz"
  local ort_url="${ORT_BASE_URL}/${ort_pkg}"
  local tmpdir
  tmpdir="$(mktemp -d)"
  curl -fSL -o "${tmpdir}/${ort_pkg}" "${ort_url}" || {
    echo "ERROR: Failed to download ONNX Runtime from ${ort_url}" >&2
    rm -rf "${ORT_DIR}" "${tmpdir}"
    exit 1
  }
  tar xzf "${tmpdir}/${ort_pkg}" -C "${tmpdir}"
  # The archive extracts as onnxruntime-linux-x64-VERSION/
  local extracted_dir="${tmpdir}/onnxruntime-linux-x64-${ORT_VERSION}"
  if [ -d "${extracted_dir}" ]; then
    cp -r "${extracted_dir}/." "${ORT_DIR}/"
  else
    echo "WARNING: Unexpected ONNX Runtime archive layout; copying as-is."
    cp -r "${tmpdir}/"* "${ORT_DIR}/"
  fi
  rm -rf "${tmpdir}"
  echo "ONNX Runtime extracted to ${ORT_DIR}"
}

echo "========================================"
echo "Packaging ${APP_NAME} v${VERSION} (${ARCH})"
echo "Build dir: ${BUILD_DIR}"
echo "========================================"

# --- Download ONNX Runtime ---
download_onnxruntime

# --- Build ---
mkdir -p "${BUILD_DIR}"
cd "${BUILD_DIR}"

if [ ! -f "${BUILD_DIR}/alcedo_studio/src/${APP_NAME_LOWER}" ]; then
  echo ">>> Configuring..."
  cmake "${PROJECT_ROOT}" \
    -DCMAKE_BUILD_TYPE="${BUILD_TYPE}" \
    -DVCPKG_ROOT="${VCPKG_ROOT}" \
    -DCMAKE_INSTALL_PREFIX=/usr \
    -DALCEDO_VERSION="${VERSION}" \
    -Donnxruntime_ROOT="${ORT_DIR}" \
    -DCMAKE_PREFIX_PATH="${ORT_DIR}/lib/cmake;${CMAKE_PREFIX_PATH:-}"

  echo ">>> Building..."
  cmake --build . --parallel "$(nproc)"
fi

# --- Install to staging directory ---
STAGING_DIR="${BUILD_DIR}/staging"
rm -rf "${STAGING_DIR}"
mkdir -p "${STAGING_DIR}"

echo ">>> Installing to staging..."
cmake --install . --prefix "${STAGING_DIR}/usr"

# Copy desktop file and icon
mkdir -p "${STAGING_DIR}/usr/share/applications"
cp "${PROJECT_ROOT}/packaging/linux/AlcedoStudio.desktop" \
   "${STAGING_DIR}/usr/share/applications/"

# Copy or generate icon
mkdir -p "${STAGING_DIR}/usr/share/icons/hicolor/scalable/apps"
if [ -f "${PROJECT_ROOT}/packaging/linux/alcedo-studio.svg" ]; then
  cp "${PROJECT_ROOT}/packaging/linux/alcedo-studio.svg" \
     "${STAGING_DIR}/usr/share/icons/hicolor/scalable/apps/"
elif [ -f "${PROJECT_ROOT}/resources/icons/alcedo-studio.svg" ]; then
  cp "${PROJECT_ROOT}/resources/icons/alcedo-studio.svg" \
     "${STAGING_DIR}/usr/share/icons/hicolor/scalable/apps/"
else
  # Generate a simple SVG placeholder
  cat > "${STAGING_DIR}/usr/share/icons/hicolor/scalable/apps/alcedo-studio.svg" << 'SVGEOF'
<?xml version="1.0" encoding="UTF-8"?>
<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 128 128">
  <rect width="128" height="128" rx="16" fill="#2196F3"/>
  <text x="64" y="80" font-family="sans-serif" font-size="64" fill="white" text-anchor="middle">A</text>
</svg>
SVGEOF
fi

# --- AppImage ---
if [ "${MAKE_APPIMAGE}" = true ]; then
  echo ""
  echo ">>> Building AppImage..."

  APPDIR="${BUILD_DIR}/AppDir"
  rm -rf "${APPDIR}"
  mkdir -p "${APPDIR}"

  # Copy staging into AppDir
  cp -r "${STAGING_DIR}/usr" "${APPDIR}/"

  # Create AppRun
  cat > "${APPDIR}/AppRun" << 'APPRUN'
#!/bin/bash
SELF="$(readlink -f "$0")"
HERE="$(dirname "${SELF}")"
export PATH="${HERE}/usr/bin:${PATH}"
export LD_LIBRARY_PATH="${HERE}/usr/lib:${LD_LIBRARY_PATH}"
export XDG_DATA_DIRS="${HERE}/usr/share:${XDG_DATA_DIRS:-/usr/local/share:/usr/share}"
# Wayland compatibility: prefer Wayland if available, fall back to X11.
# Qt6 auto-detects the platform but the WAYLAND_DISPLAY variable helps
# when running under XWayland without native Wayland support.
if [ -n "${WAYLAND_DISPLAY}" ]; then
  export QT_QPA_PLATFORM=wayland;xcb
else
  export QT_QPA_PLATFORM=xcb
fi
exec "${HERE}/usr/bin/alcedo-studio" "$@"
APPRUN
  chmod +x "${APPDIR}/AppRun"

  # Copy desktop file to AppDir root
  cp "${STAGING_DIR}/usr/share/applications/AlcedoStudio.desktop" "${APPDIR}/"
  cp "${STAGING_DIR}/usr/share/icons/hicolor/scalable/apps/alcedo-studio.svg" \
     "${APPDIR}/alcedo-studio.svg" 2>/dev/null || true
  cp "${STAGING_DIR}/usr/share/icons/hicolor/scalable/apps/alcedo-studio.svg" \
     "${APPDIR}/.DirIcon" 2>/dev/null || true

  # Bundle vcpkg dependencies
  echo ">>> Bundling shared libraries..."
  local_bin="${APPDIR}/usr/bin"
  if [ -d "${VCPKG_ROOT}/installed" ]; then
    for lib in "${VCPKG_ROOT}/installed/${ARCH}-rel/lib/"lib*.so*; do
      [ -f "$lib" ] || continue
      cp "$lib" "${APPDIR}/usr/lib/" 2>/dev/null || true
    done
  fi

  # Bundle ONNX Runtime shared library
  echo ">>> Bundling ONNX Runtime..."
  if [ -f "${ORT_DIR}/lib/libonnxruntime.so.${ORT_VERSION}" ]; then
    cp "${ORT_DIR}/lib/libonnxruntime.so."* "${APPDIR}/usr/lib/" 2>/dev/null || true
  elif [ -f "${ORT_DIR}/lib/libonnxruntime.so" ]; then
    cp "${ORT_DIR}/lib/libonnxruntime.so"* "${APPDIR}/usr/lib/" 2>/dev/null || true
  else
    echo "WARNING: libonnxruntime.so not found in ${ORT_DIR}/lib"
  fi

  # Bundle model files directory
  echo ">>> Bundling model files..."
  MODEL_DIR="${PROJECT_ROOT}/alcedo_studio/src/config/models"
  if [ -d "${MODEL_DIR}" ]; then
    mkdir -p "${APPDIR}/usr/share/${APP_NAME_LOWER}/models"
    cp -r "${MODEL_DIR}/"* "${APPDIR}/usr/share/${APP_NAME_LOWER}/models/" 2>/dev/null || true
  fi

  # Use linuxdeployqt if available, otherwise manual approach
  if command -v linuxdeployqt &>/dev/null; then
    linuxdeployqt "${APPDIR}/usr/share/applications/AlcedoStudio.desktop" \
      -verbose=1 -bundle-non-qt-libs
  elif command -v linuxdeploy &>/dev/null; then
    linuxdeploy --appdir "${APPDIR}" \
      --plugin qt \
      --output appimage
  else
    echo "WARNING: linuxdeployqt/linuxdeploy not found. Creating manual AppImage."
    # Manual library bundling with ldd
    for bin in "${APPDIR}/usr/bin/"*; do
      [ -f "$bin" ] || continue
      ldd "$bin" 2>/dev/null | grep "=> /" | awk '{print $3}' | while read -r lib; do
        case "$lib" in
          /lib/*|/lib64/*|/usr/lib/*) continue ;;  # Skip system libs
        esac
        cp -n "$lib" "${APPDIR}/usr/lib/" 2>/dev/null || true
      done
    done
  fi

  # Create AppImage using appimagetool if available
  OUTPUT_NAME="${APP_NAME}-${VERSION}-${ARCH}.AppImage"
  if command -v appimagetool &>/dev/null; then
    appimagetool "${APPDIR}" "${BUILD_DIR}/${OUTPUT_NAME}"
  elif command -v mksquashfs &>/dev/null; then
    # Manual AppImage creation
    mksquashfs "${APPDIR}" "/tmp/appimage_${$}.squashfs" -comp xz -root-owned -noappend
    # Prepend runtime
    if [ -f /usr/lib/appimage/runtime ]; then
      cat /usr/lib/appimage/runtime "/tmp/appimage_${$}.squashfs" > "${BUILD_DIR}/${OUTPUT_NAME}"
      chmod +x "${BUILD_DIR}/${OUTPUT_NAME}"
    else
      echo "WARNING: No AppImage runtime found. Creating squashfs archive instead."
      mv "/tmp/appimage_${$}.squashfs" "${BUILD_DIR}/${OUTPUT_NAME}.squashfs"
    fi
    rm -f "/tmp/appimage_${$}.squashfs"
  else
    echo "WARNING: No AppImage tool found. Creating tar archive instead."
    tar czf "${BUILD_DIR}/${OUTPUT_NAME}.tar.gz" -C "${APPDIR}" .
  fi

  echo "AppImage output: ${BUILD_DIR}/${OUTPUT_NAME}"
fi

# --- .deb package ---
if [ "${MAKE_DEB}" = true ]; then
  echo ""
  echo ">>> Building .deb package..."

  DEB_DIR="${BUILD_DIR}/deb-build"
  rm -rf "${DEB_DIR}"
  mkdir -p "${DEB_DIR}/DEBIAN"

  # Copy staging files
  cp -r "${STAGING_DIR}/usr" "${DEB_DIR}/"

  # Install model files into the deb package
  MODEL_DIR="${PROJECT_ROOT}/alcedo_studio/src/config/models"
  if [ -d "${MODEL_DIR}" ]; then
    mkdir -p "${DEB_DIR}/usr/share/${APP_NAME_LOWER}/models"
    cp -r "${MODEL_DIR}/"* "${DEB_DIR}/usr/share/${APP_NAME_LOWER}/models/" 2>/dev/null || true
  fi

  # Generate control file
  cat > "${DEB_DIR}/DEBIAN/control" << CONTROLEOF
Package: ${APP_NAME_LOWER}
Version: ${VERSION}
Section: graphics
Priority: optional
Architecture: ${ARCH}
Depends: libc6 (>= 2.31), libgcc-s1 (>= 10), libstdc++6 (>= 10), libgl1, libx11-6, libxcb1, libxkbcommon0, libdbus-1-3, libfontconfig1, libfreetype6, libglib2.0-0, libpng16-16, libjpeg-turbo8 | libjpeg8, libtiff5 | libtiff6, libopenexr25 | libopenexr-3-1-30, liblcms2-2, libwayland-client0, libonnxruntime (>= 1.17) | onnxruntime (>= 1.17)
Maintainer: AlcedoStudio Team <dev@alcedo.studio>
Description: Professional RAW image editor with AI-powered tools
 AlcedoStudio is a professional RAW image editor featuring
 AI-powered labeling and content analysis, comprehensive
 adjustment tools, and support for a wide range of camera formats.
Homepage: https://github.com/alcedo-studio/alcedo
Vcs-Git: https://github.com/alcedo-studio/alcedo.git
CONTROLEOF

  # Post-install script: update desktop database and icon cache
  cat > "${DEB_DIR}/DEBIAN/postinst" << POSTINST
#!/bin/bash
set -e
if command -v update-desktop-database &>/dev/null; then
  update-desktop-database -q /usr/share/applications 2>/dev/null || true
fi
if command -v gtk-update-icon-cache &>/dev/null; then
  gtk-update-icon-cache -q /usr/share/icons/hicolor 2>/dev/null || true
fi
POSTINST
  chmod 755 "${DEB_DIR}/DEBIAN/postinst"

  # Post-remove script
  cat > "${DEB_DIR}/DEBIAN/postrm" << POSTRM
#!/bin/bash
set -e
if command -v update-desktop-database &>/dev/null; then
  update-desktop-database -q /usr/share/applications 2>/dev/null || true
fi
POSTRM
  chmod 755 "${DEB_DIR}/DEBIAN/postrm"

  # Build the .deb
  DEB_NAME="${APP_NAME}_${VERSION}-${ARCH}.deb"
  dpkg-deb --build "${DEB_DIR}" "${BUILD_DIR}/${DEB_NAME}"
  echo ".deb output: ${BUILD_DIR}/${DEB_NAME}"
fi

echo ""
echo "========================================"
echo "Packaging complete!"
echo "========================================"

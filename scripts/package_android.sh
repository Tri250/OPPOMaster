#!/usr/bin/env bash
# package_android.sh — Build and package AlcedoStudio for Android (APK)
#
# Usage:
#   ./scripts/package_android.sh [options]
#
# Options:
#   --qt-dir <path>          Qt for Android installation prefix
#   --android-ndk <path>     Android NDK path (default: ANDROID_NDK env or auto-detect)
#   --sdk-dir <path>         Android SDK path (default: ANDROID_HOME env or auto-detect)
#   --keystore <path>        Keystore file for APK signing
#   --keystore-password <pw> Password for the keystore
#   --keystore-alias <name>  Key alias in the keystore
#   --build-type <type>      Build type: Release (default) or Debug
#   --abi <abi>              Target ABI (default: arm64-v8a)
#   --model-dir <path>       Directory containing ONNX model files to bundle
#   --opencv-libs <path>     Directory containing OpenCV Android .so files
#   --help                   Show this help message
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
BUILD_DIR="${PROJECT_ROOT}/build/android-package"
BUILD_TYPE="Release"
ABI="arm64-v8a"
KEYSTORE=""
KEYSTORE_PASSWORD=""
KEYSTORE_ALIAS=""
MODEL_DIR=""
OPENCV_LIBS=""

QT_DIR="${QT_DIR:-}"
ANDROID_NDK="${ANDROID_NDK:-}"
SDK_DIR="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-}}"

# Parse arguments
while [[ $# -gt 0 ]]; do
  case "$1" in
    --qt-dir)          QT_DIR="$2"; shift 2 ;;
    --android-ndk)     ANDROID_NDK="$2"; shift 2 ;;
    --sdk-dir)         SDK_DIR="$2"; shift 2 ;;
    --keystore)        KEYSTORE="$2"; shift 2 ;;
    --keystore-password) KEYSTORE_PASSWORD="$2"; shift 2 ;;
    --keystore-alias)  KEYSTORE_ALIAS="$2"; shift 2 ;;
    --build-type)      BUILD_TYPE="$2"; shift 2 ;;
    --abi)             ABI="$2"; shift 2 ;;
    --model-dir)       MODEL_DIR="$2"; shift 2 ;;
    --opencv-libs)     OPENCV_LIBS="$2"; shift 2 ;;
    --version)         ALCEDO_VERSION="$2"; shift 2 ;;
    --help)
      echo "Usage: $0 [options]"
      echo ""
      echo "Build and package AlcedoStudio for Android (APK)."
      echo ""
      echo "Required environment or options:"
      echo "  --qt-dir <path>        Qt for Android installation prefix"
      echo "  --android-ndk <path>   Android NDK path"
      echo "  --sdk-dir <path>       Android SDK path"
      echo ""
      echo "Signing options (omit for debug signing):"
      echo "  --keystore <path>        Keystore file"
      echo "  --keystore-password <pw> Keystore password"
      echo "  --keystore-alias <name>  Key alias"
      echo ""
      echo "Other options:"
      echo "  --build-type <type>    Release (default) or Debug"
      echo "  --abi <abi>            Target ABI (default: arm64-v8a)"
      echo "  --model-dir <path>     ONNX model directory to bundle"
      echo "  --opencv-libs <path>   OpenCV Android native libraries"
      exit 0 ;;
    *) echo "Unknown option: $1" >&2; exit 1 ;;
  esac
done

# Validate required paths
if [[ -z "${QT_DIR}" ]]; then
  echo "ERROR: --qt-dir is required (or set QT_DIR env variable)." >&2
  exit 1
fi
if [[ ! -d "${QT_DIR}" ]]; then
  echo "ERROR: Qt directory does not exist: ${QT_DIR}" >&2
  exit 1
fi

# Auto-detect NDK from SDK
if [[ -z "${ANDROID_NDK}" && -n "${SDK_DIR}" ]]; then
  # Find the latest NDK in the SDK
  ANDROID_NDK="$(ls -d "${SDK_DIR}"/ndk/* 2>/dev/null | sort -V | tail -1 || true)"
  if [[ -n "${ANDROID_NDK}" ]]; then
    echo "Auto-detected Android NDK: ${ANDROID_NDK}"
  fi
fi
if [[ -z "${ANDROID_NDK}" ]]; then
  echo "ERROR: --android-ndk is required (or set ANDROID_NDK env variable)." >&2
  exit 1
fi
if [[ ! -d "${ANDROID_NDK}" ]]; then
  echo "ERROR: Android NDK directory does not exist: ${ANDROID_NDK}" >&2
  exit 1
fi

if [[ -z "${SDK_DIR}" ]]; then
  echo "ERROR: --sdk-dir is required (or set ANDROID_HOME env variable)." >&2
  exit 1
fi

# Detect version from CMakeLists.txt
detect_version() {
  if [ -n "${ALCEDO_VERSION:-}" ]; then
    echo "${ALCEDO_VERSION}"
    return
  fi
  local cmake_ver
  cmake_ver="$(grep -m1 'project(' "${PROJECT_ROOT}/CMakeLists.txt" 2>/dev/null \
    | sed -n 's/.*VERSION[[:space:]]\+\([0-9]\+\.[0-9]\+\.[0-9]\+\).*/\1/p')"
  if [ -n "${cmake_ver}" ]; then
    echo "${cmake_ver}"
    return
  fi
  echo "0.0.0"
}
VERSION="$(detect_version)"

# Locate Qt Android toolchain
QT_ANDROID_TOOLCHAIN="${QT_DIR}/lib/cmake/Qt6/qt.toolchain.cmake"
if [[ ! -f "${QT_ANDROID_TOOLCHAIN}" ]]; then
  echo "ERROR: Qt Android toolchain not found at: ${QT_ANDROID_TOOLCHAIN}" >&2
  echo "Make sure Qt for Android is installed (e.g. Qt/6.x.x/android_arm64_v8a)" >&2
  exit 1
fi

# Locate androiddeployqt
ANDROIDDEPLOYQT="${QT_DIR}/bin/androiddeployqt"
if [[ ! -x "${ANDROIDDEPLOYQT}" ]]; then
  echo "ERROR: androiddeployqt not found at: ${ANDROIDDEPLOYQT}" >&2
  exit 1
fi

# Locate apksigner
APKSIGNER="${SDK_DIR}/build-tools/$(ls "${SDK_DIR}/build-tools/" 2>/dev/null | sort -V | tail -1)/apksigner"
if [[ ! -x "${APKSIGNER}" ]]; then
  echo "WARNING: apksigner not found at expected location; will try PATH."
  APKSIGNER="$(command -v apksigner 2>/dev/null || true)"
fi

echo "========================================"
echo "AlcedoStudio Android Packaging"
echo "Version: ${VERSION}"
echo "ABI: ${ABI}"
echo "Build type: ${BUILD_TYPE}"
echo "Qt prefix: ${QT_DIR}"
echo "Android NDK: ${ANDROID_NDK}"
echo "Android SDK: ${SDK_DIR}"
echo "Build dir: ${BUILD_DIR}"
echo "========================================"

# ── Step 1: Configure CMake ──────────────────────────────────────
echo ""
echo ">>> Configuring CMake for Android..."

mkdir -p "${BUILD_DIR}"
cd "${BUILD_DIR}"

cmake "${PROJECT_ROOT}" \
  -G Ninja \
  -DCMAKE_TOOLCHAIN_FILE="${QT_ANDROID_TOOLCHAIN}" \
  -DQT_HOST_PATH="${QT_DIR}/../gcc_64" \
  -DCMAKE_BUILD_TYPE="${BUILD_TYPE}" \
  -DANDROID_ABI="${ABI}" \
  -DCMAKE_SYSTEM_NAME=Android \
  -DCMAKE_SYSTEM_VERSION=24 \
  -DCMAKE_ANDROID_ARCH_ABI="${ABI}" \
  -DANDROID_NDK="${ANDROID_NDK}" \
  -DANDROID_SDK="${SDK_DIR}" \
  -DALCEDO_ENABLE_CUDA=OFF \
  -DALCEDO_ENABLE_METAL=OFF \
  -DALCEDO_ENABLE_OPENCL=OFF \
  -DALCEDO_ENABLE_OPENGL_EDITOR=OFF \
  -DALCEDO_ENABLE_ANDROID=ON \
  -DALCEDO_BUILD_TESTS=OFF \
  -DALCEDO_BUILD_SEMANTIC_SIDECAR=OFF

# ── Step 2: Build ────────────────────────────────────────────────
echo ""
echo ">>> Building..."

cmake --build . --parallel "$(nproc 2>/dev/null || sysctl -n hw.ncpu 2>/dev/null || echo 4)"

# ── Step 3: Prepare Android package directory ────────────────────
echo ""
echo ">>> Preparing Android package directory..."

PACKAGE_DIR="${BUILD_DIR}/android-build"
mkdir -p "${PACKAGE_DIR}"

# Copy the built library
LIB_NAME="libalcedo_main.so"
if [[ -f "${BUILD_DIR}/alcedo_studio/src/ui/alcedo_main/${LIB_NAME}" ]]; then
  mkdir -p "${PACKAGE_DIR}/libs/${ABI}"
  cp "${BUILD_DIR}/alcedo_studio/src/ui/alcedo_main/${LIB_NAME}" \
     "${PACKAGE_DIR}/libs/${ABI}/"
else
  echo "WARNING: ${LIB_NAME} not found at expected location; androiddeployqt will handle deployment."
fi

# ── Step 4: Bundle model files as assets ─────────────────────────
echo ""
echo ">>> Bundling model files..."

ASSETS_DIR="${PACKAGE_DIR}/assets"
mkdir -p "${ASSETS_DIR}/models"

if [[ -n "${MODEL_DIR}" && -d "${MODEL_DIR}" ]]; then
  cp -r "${MODEL_DIR}/"* "${ASSETS_DIR}/models/" 2>/dev/null || true
  echo "Bundled model files from: ${MODEL_DIR}"
elif [[ -d "${PROJECT_ROOT}/alcedo_studio/src/config/models" ]]; then
  cp -r "${PROJECT_ROOT}/alcedo_studio/src/config/models/"* \
        "${ASSETS_DIR}/models/" 2>/dev/null || true
  echo "Bundled model files from project config."
else
  echo "WARNING: No model directory found; models will need to be downloaded at runtime."
fi

# Copy config assets
CONFIG_DIR="${PROJECT_ROOT}/alcedo_studio/src/config"
if [[ -d "${CONFIG_DIR}" ]]; then
  mkdir -p "${ASSETS_DIR}/config"
  for dir in fonts icc lens_calib nikon_lens; do
    if [[ -d "${CONFIG_DIR}/${dir}" ]]; then
      cp -r "${CONFIG_DIR}/${dir}" "${ASSETS_DIR}/config/" 2>/dev/null || true
    fi
  done
  # Copy LUTs
  if [[ -d "${CONFIG_DIR}/LUTs" ]]; then
    cp -r "${CONFIG_DIR}/LUTs" "${ASSETS_DIR}/config/" 2>/dev/null || true
  fi
fi

# ── Step 5: Bundle OpenCV native libraries ───────────────────────
echo ""
echo ">>> Bundling OpenCV native libraries..."

if [[ -n "${OPENCV_LIBS}" && -d "${OPENCV_LIBS}" ]]; then
  mkdir -p "${PACKAGE_DIR}/libs/${ABI}"
  cp "${OPENCV_LIBS}"/lib*.so "${PACKAGE_DIR}/libs/${ABI}/" 2>/dev/null || true
  echo "Bundled OpenCV native libraries from: ${OPENCV_LIBS}"
elif [[ -d "${PROJECT_ROOT}/alcedo_studio/third_party/opencv-android/sdk/native/libs/${ABI}" ]]; then
  mkdir -p "${PACKAGE_DIR}/libs/${ABI}"
  cp "${PROJECT_ROOT}/alcedo_studio/third_party/opencv-android/sdk/native/libs/${ABI}"/lib*.so \
     "${PACKAGE_DIR}/libs/${ABI}/" 2>/dev/null || true
  echo "Bundled OpenCV native libraries from third_party."
else
  echo "WARNING: No OpenCV Android native libraries found; they must be bundled separately."
fi

# ── Step 6: Run androiddeployqt ──────────────────────────────────
echo ""
echo ">>> Running androiddeployqt..."

# Find the android-deployment-settings.json generated by CMake
DEPLOY_SETTINGS="${BUILD_DIR}/android-alcedo_main-deployment-settings.json"
if [[ ! -f "${DEPLOY_SETTINGS}" ]]; then
  # Search for it in the build tree
  DEPLOY_SETTINGS="$(find "${BUILD_DIR}" -name "*android*deployment*settings*.json" -type f | head -1 || true)"
fi

if [[ -z "${DEPLOY_SETTINGS}" || ! -f "${DEPLOY_SETTINGS}" ]]; then
  echo "ERROR: Android deployment settings JSON not found." >&2
  echo "CMake should have generated this file during the build step." >&2
  echo "Looked in: ${BUILD_DIR}" >&2
  exit 1
fi

ANDROIDDEPLOYQT_ARGS=(
  --output "${PACKAGE_DIR}"
  --input "${DEPLOY_SETTINGS}"
  --android-platform android-34
  --jdk "${JAVA_HOME:-/usr/lib/jvm/java-17-openjdk-amd64}"
)

# Add signing arguments for release builds
if [[ "${BUILD_TYPE}" == "Release" && -n "${KEYSTORE}" ]]; then
  if [[ -z "${KEYSTORE_PASSWORD}" || -z "${KEYSTORE_ALIAS}" ]]; then
    echo "ERROR: --keystore-password and --keystore-alias are required when --keystore is provided." >&2
    exit 1
  fi
  ANDROIDDEPLOYQT_ARGS+=(
    --release
    --sign "${KEYSTORE}" "${KEYSTORE_ALIAS}" --storepass "${KEYSTORE_PASSWORD}"
  )
  echo "APK will be signed with release key."
elif [[ "${BUILD_TYPE}" == "Release" ]]; then
  ANDROIDDEPLOYQT_ARGS+=(--release)
  echo "WARNING: Release build without explicit keystore; debug key will be used."
fi

"${ANDROIDDEPLOYQT}" "${ANDROIDDEPLOYQT_ARGS[@]}"

# ── Step 7: Sign APK if needed ───────────────────────────────────
echo ""
echo ">>> Signing APK..."

# Find the generated APK
APK_FILE=""
for apk_candidate in \
  "${PACKAGE_DIR}/build/outputs/apk/release/alcedo_main-release-signed.apk" \
  "${PACKAGE_DIR}/build/outputs/apk/release/alcedo_main-release-unsigned.apk" \
  "${PACKAGE_DIR}/build/outputs/apk/release/*.apk" \
  "${PACKAGE_DIR}/build/outputs/apk/debug/alcedo_main-debug.apk" \
  "${PACKAGE_DIR}/build/outputs/apk/debug/*.apk" \
  "${PACKAGE_DIR}/*.apk"; do
  for f in ${apk_candidate}; do
    if [[ -f "$f" ]]; then
      APK_FILE="$f"
      break 2
    fi
  done
done

if [[ -z "${APK_FILE}" ]]; then
  echo "ERROR: No APK file found after androiddeployqt." >&2
  exit 1
fi

echo "Found APK: ${APK_FILE}"

# Zipalign and sign with apksigner if we have the tools and the APK is unsigned
if [[ -n "${APKSIGNER}" && -x "${APKSIGNER}" ]]; then
  # Determine if signing is needed
  NEEDS_SIGNING=true
  case "${APK_FILE}" in
    *-signed.apk|*-signed.*.apk) NEEDS_SIGNING=false ;;
  esac

  if [[ "${NEEDS_SIGNING}" == "true" ]]; then
    # Find zipalign
    ZIPALIGN="${SDK_DIR}/build-tools/$(ls "${SDK_DIR}/build-tools/" 2>/dev/null | sort -V | tail -1)/zipalign"

    ALIGNED_APK="${APK_FILE%.apk}-aligned.apk"
    SIGNED_APK="${APK_FILE%.apk}-signed.apk"

    if [[ -x "${ZIPALIGN}" ]]; then
      echo "Running zipalign..."
      "${ZIPALIGN}" -f 4 "${APK_FILE}" "${ALIGNED_APK}"
      APK_FILE="${ALIGNED_APK}"
    fi

    if [[ -n "${KEYSTORE}" && -n "${KEYSTORE_PASSWORD}" && -n "${KEYSTORE_ALIAS}" ]]; then
      echo "Signing APK with apksigner (release)..."
      "${APKSIGNER}" sign \
        --ks "${KEYSTORE}" \
        --ks-pass "pass:${KEYSTORE_PASSWORD}" \
        --ks-key-alias "${KEYSTORE_ALIAS}" \
        --out "${SIGNED_APK}" \
        "${APK_FILE}"
      APK_FILE="${SIGNED_APK}"
    else
      echo "Signing APK with debug key..."
      DEBUG_KEYSTORE="${HOME}/.android/debug.keystore"
      if [[ ! -f "${DEBUG_KEYSTORE}" ]]; then
        keytool -genkey -v -keystore "${DEBUG_KEYSTORE}" \
          -alias androiddebugkey -storepass android -keypass android \
          -keyalg RSA -keysize 2048 -validity 10000 \
          -dname "CN=Android Debug,O=Android,C=US"
      fi
      "${APKSIGNER}" sign \
        --ks "${DEBUG_KEYSTORE}" \
        --ks-pass "pass:android" \
        --ks-key-alias androiddebugkey \
        --out "${SIGNED_APK}" \
        "${APK_FILE}"
      APK_FILE="${SIGNED_APK}"
    fi
    echo "Signed APK: ${APK_FILE}"
  fi
else
  echo "WARNING: apksigner not found; APK may be unsigned."
fi

# ── Step 8: Copy final APK to output directory ───────────────────
echo ""
echo ">>> Copying APK to output directory..."

OUTPUT_DIR="${BUILD_DIR}/output"
mkdir -p "${OUTPUT_DIR}"
OUTPUT_NAME="AlcedoStudio-${VERSION}-android-${ABI}.apk"
cp "${APK_FILE}" "${OUTPUT_DIR}/${OUTPUT_NAME}"

echo ""
echo "========================================"
echo "Android packaging complete!"
echo "APK: ${OUTPUT_DIR}/${OUTPUT_NAME}"
echo "========================================"

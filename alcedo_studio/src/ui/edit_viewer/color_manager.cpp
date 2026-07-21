//  Copyright 2026 Yurun Zi
//  SPDX-License-Identifier: GPL-3.0-only
//  Additional permission under GPLv3 section 7 applies; see the LICENSE file.

#include "ui/edit_viewer/color_manager.hpp"

#if defined(_WIN32)
#ifndef NOMINMAX
#define NOMINMAX
#endif
#include <Windows.h>
#include <icm.h>
#include <dwmapi.h>
#include <dxgi1_6.h>
#pragma comment(lib, "icmui.lib")
#pragma comment(lib, "dwmapi.lib")
#pragma comment(lib, "user32.lib")
#endif

#include <QtCore/qlogging.h>
#include <QDebug>
#include <QString>

#include <algorithm>
#include <cmath>
#include <cstring>
#include <filesystem>
#include <fstream>
#include <unordered_map>

namespace alcedo {

// Static member definitions
std::mutex ColorManager::cache_mutex_;
std::unordered_map<ColorTransformCacheKey,
                   ColorTransformHandle,
                   ColorTransformCacheKeyHash> ColorManager::transform_cache_;
MonitorIccProfile ColorManager::cached_display_profile_;
ColorManager::DisplayProfileChangeCallback ColorManager::profile_change_callback_;

#if defined(_WIN32)
namespace {

auto ComputeProfileHash(const std::vector<uint8_t>& bytes) -> std::string {
  // Simple hash based on size and first/last bytes for fast cache invalidation.
  // A full MD5 is overkill for display profile change detection.
  if (bytes.empty()) {
    return "";
  }
  const size_t sz = bytes.size();
  const uint32_t h1 = static_cast<uint32_t>(sz);
  const uint32_t h2 = static_cast<uint32_t>(bytes[0]) |
                       (static_cast<uint32_t>(bytes[sz > 1 ? sz - 1 : 0]) << 8);
  const uint32_t h3 = static_cast<uint32_t>(bytes[sz > 4 ? 4 : 0]) |
                       (static_cast<uint32_t>(bytes[sz > 8 ? 8 : 0]) << 8);
  return std::to_string(h1) + "_" + std::to_string(h2) + "_" + std::to_string(h3);
}

auto GetMonitorDeviceName(HMONITOR monitor) -> std::wstring {
  if (!monitor) {
    return {};
  }

  // Get the monitor device name from the MONITORINFOEX structure
  MONITORINFOEXW mi;
  mi.cbSize = sizeof(mi);
  if (!GetMonitorInfoW(monitor, &mi)) {
    return {};
  }

  // mi.szDevice contains the device name like "\\.\DISPLAY1"
  return std::wstring(mi.szDevice);
}

auto GetMonitorProfilePath(HMONITOR monitor) -> std::wstring {
  if (!monitor) {
    return {};
  }

  // Get the monitor device name (e.g. "\\.\DISPLAY1")
  std::wstring device_name = GetMonitorDeviceName(monitor);
  if (device_name.empty()) {
    return {};
  }

  // First, get the required buffer size for the profile path
  DWORD profile_size = 0;
  BOOL result = WcsGetDefaultColorProfileSize(
      WCS_PROFILE_MANAGEMENT_SCOPE_CURRENT_USER,
      device_name.c_str(),
      COLOR_PROFILE_DEFAULT,
      CPST_RGB_WORKING_SPACE,
      0,
      &profile_size);

  if (!result || profile_size == 0) {
    // Try system-wide scope
    result = WcsGetDefaultColorProfileSize(
        WCS_PROFILE_MANAGEMENT_SCOPE_SYSTEM_WIDE,
        device_name.c_str(),
        COLOR_PROFILE_DEFAULT,
        CPST_RGB_WORKING_SPACE,
        0,
        &profile_size);
  }

  if (!result || profile_size == 0) {
    return {};
  }

  // Allocate and retrieve the profile path
  std::vector<WCHAR> profile_path(profile_size, L'\0');
  result = WcsGetDefaultColorProfile(
      WCS_PROFILE_MANAGEMENT_SCOPE_CURRENT_USER,
      device_name.c_str(),
      COLOR_PROFILE_DEFAULT,
      CPST_RGB_WORKING_SPACE,
      0,
      profile_size,
      profile_path.data());

  if (!result || profile_path[0] == L'\0') {
    // Fallback: try the system-level scope
    std::fill(profile_path.begin(), profile_path.end(), L'\0');
    result = WcsGetDefaultColorProfile(
        WCS_PROFILE_MANAGEMENT_SCOPE_SYSTEM_WIDE,
        device_name.c_str(),
        COLOR_PROFILE_DEFAULT,
        CPST_RGB_WORKING_SPACE,
        0,
        profile_size,
        profile_path.data());
  }

  if (!result || profile_path[0] == L'\0') {
    return {};
  }

  return std::wstring(profile_path.data());
}

struct MonitorEnumContext {
  HWND   target_window;
  HMONITOR found_monitor;
};

BOOL CALLBACK MonitorEnumProc(HMONITOR monitor, HDC /*hdc*/, LPRECT rect, LPARAM data) {
  auto* ctx = reinterpret_cast<MonitorEnumContext*>(data);
  if (!ctx || !ctx->target_window) {
    return TRUE;
  }

  // Check if the target window is within this monitor's rect
  RECT win_rect;
  if (GetWindowRect(ctx->target_window, &win_rect)) {
    const long center_x = (win_rect.left + win_rect.right) / 2;
    const long center_y = (win_rect.top + win_rect.bottom) / 2;
    if (center_x >= rect->left && center_x <= rect->right &&
        center_y >= rect->top && center_y <= rect->bottom) {
      ctx->found_monitor = monitor;
      return FALSE;  // Found it, stop enumerating
    }
  }
  return TRUE;
}

auto GetMonitorForWindow(HWND window) -> HMONITOR {
  if (!window) {
    return nullptr;
  }

  // Prefer MonitorFromWindow as it's simpler and more reliable
  HMONITOR monitor = MonitorFromWindow(window, MONITOR_DEFAULTTONEAREST);
  return monitor;
}

auto ReadIccFile(const std::wstring& path) -> std::vector<uint8_t> {
  if (path.empty()) {
    return {};
  }

  std::ifstream file(path, std::ios::binary | std::ios::ate);
  if (!file.is_open()) {
    return {};
  }

  const auto size = file.tellg();
  file.seekg(0, std::ios::beg);

  std::vector<uint8_t> bytes(static_cast<size_t>(size));
  if (!file.read(reinterpret_cast<char*>(bytes.data()), size)) {
    return {};
  }

  return bytes;
}

// Map our ColorSpace enum to Windows COLORPROFILETYPE + profile data.
// We use the ICC profile files bundled with the app.
auto GetSourceIccProfileBytes(ColorUtils::ColorSpace space, ColorUtils::EOTF eotf)
    -> std::vector<uint8_t> {
  // Build the expected ICC filename from the space/eotf combo
  const char* icc_filename = nullptr;

  switch (space) {
    case ColorUtils::ColorSpace::REC709:
      switch (eotf) {
        case ColorUtils::EOTF::BT1886:    icc_filename = "rec709_bt1886.icc"; break;
        case ColorUtils::EOTF::GAMMA_2_2: icc_filename = "rec709_gamma22.icc"; break;
        default:                          icc_filename = "rec709_gamma22.icc"; break;
      }
      break;
    case ColorUtils::ColorSpace::P3_D65:
      switch (eotf) {
        case ColorUtils::EOTF::GAMMA_2_2: icc_filename = "p3_d65_gamma22.icc"; break;
        case ColorUtils::EOTF::ST2084:     icc_filename = "p3_d65_pq.icc"; break;
        default:                           icc_filename = "p3_d65_gamma22.icc"; break;
      }
      break;
    case ColorUtils::ColorSpace::P3_D60:
      icc_filename = "p3_d60_gamma26.icc"; break;
    case ColorUtils::ColorSpace::P3_DCI:
      icc_filename = "p3_dci_gamma26.icc"; break;
    case ColorUtils::ColorSpace::REC2020:
      switch (eotf) {
        case ColorUtils::EOTF::ST2084: icc_filename = "rec2020_pq.icc"; break;
        case ColorUtils::EOTF::HLG:    icc_filename = "rec2020_hlg.icc"; break;
        default:                        icc_filename = "rec2020_pq.icc"; break;
      }
      break;
    case ColorUtils::ColorSpace::XYZ:
      icc_filename = "xyz_gamma26.icc"; break;
    default:
      icc_filename = "rec709_gamma22.icc"; break;
  }

  if (!icc_filename) {
    return {};
  }

  // Search for the ICC file relative to the executable
  WCHAR exe_path[MAX_PATH] = {};
  GetModuleFileNameW(nullptr, exe_path, MAX_PATH);
  std::filesystem::path exe_dir = std::filesystem::path(exe_path).parent_path();

  std::vector<std::filesystem::path> search_roots = {
      exe_dir / "config" / "icc",
      exe_dir / "icc",
  };

  for (const auto& root : search_roots) {
    auto candidate = root / icc_filename;
    std::ifstream f(candidate, std::ios::binary | std::ios::ate);
    if (f.is_open()) {
      const auto sz = f.tellg();
      f.seekg(0, std::ios::beg);
      std::vector<uint8_t> bytes(static_cast<size_t>(sz));
      if (f.read(reinterpret_cast<char*>(bytes.data()), sz)) {
        return bytes;
      }
    }
  }

  qWarning("ColorManager: could not find source ICC profile: %s", icc_filename);
  return {};
}

auto CreateIccProfileFromBytes(const std::vector<uint8_t>& bytes) -> HPROFILE {
  if (bytes.empty()) {
    return nullptr;
  }

  PROFILE profile;
  profile.cbDataSize = static_cast<DWORD>(bytes.size());
  profile.pProfileData = const_cast<void*>(static_cast<const void*>(bytes.data()));
  profile.dwType = PROFILE_FILENAME;

  // Try as file-based profile first; if the bytes are an in-memory ICC profile,
  // use PROFILE_MEMBUFFER
  PROFILE mem_profile;
  mem_profile.cbDataSize = static_cast<DWORD>(bytes.size());
  mem_profile.pProfileData = const_cast<void*>(static_cast<const void*>(bytes.data()));
  mem_profile.dwType = PROFILE_MEMBUFFER;

  HPROFILE hProfile = OpenColorProfile(
      &mem_profile,
      PROFILE_READ,
      FILE_SHARE_READ,
      OPEN_EXISTING);

  if (!hProfile) {
    // Fallback: try as filename profile
    hProfile = OpenColorProfile(
        &profile,
        PROFILE_READ,
        FILE_SHARE_READ,
        OPEN_EXISTING);
  }

  return hProfile;
}

auto CreateTransformBetweenProfiles(HPROFILE src_profile, HPROFILE dst_profile) -> HTRANSFORM {
  if (!src_profile || !dst_profile) {
    return nullptr;
  }

  // Create a multi-profile transform that converts from the source ICC
  // profile to the destination (display) ICC profile.
  // CreateMultiProfileTransform takes an array of profile handles and builds
  // a processing transform between them.
  HPROFILE profiles[] = {src_profile, dst_profile};
  DWORD    intents[]  = {LCS_RELATIVE_COLORIMETRIC, LCS_RELATIVE_COLORIMETRIC};

  HTRANSFORM hTransform = CreateMultiProfileTransform(
      profiles,
      2,                // number of profiles
      intents,
      0,                // index of the intent to use (0-based)
      BEST_MODE,        // flags for quality/speed
      INDEX_DONT_CARE); // profile index for proofing

  return hTransform;
}

}  // namespace
#endif  // _WIN32

// ============================================================================
// ColorManager Implementation - Windows (color_manager.cpp)
// ============================================================================

auto ColorManager::ApplyWindowColorSpace(void*                      native_view_or_window,
                                         const ViewerDisplayConfig& config) -> bool {
#if defined(_WIN32)
  if (!native_view_or_window) {
    return false;
  }

  HWND window = static_cast<HWND>(native_view_or_window);
  if (!IsWindow(window)) {
    return false;
  }

  // Check and apply Windows 11+ DWM color management for HDR/Advanced Color
  // This enables proper ICC profile handling for the swap chain.
  // On Windows 11 22H2+, DWM supports ICC profiles for SDR content when
  // Auto Color Management (ACM) is enabled.

  // Detect the monitor ICC profile and cache it for preview rendering
  auto profile = DetectMonitorIccProfile(native_view_or_window);
  if (profile.has_value()) {
    std::lock_guard<std::mutex> lock(cache_mutex_);
    if (cached_display_profile_.profile_path != profile->profile_path ||
        cached_display_profile_.profile_hash != profile->profile_hash) {
      qInfo() << "ColorManager: display ICC profile detected:"
              << QString::fromWCharArray(profile->profile_path.c_str());
      cached_display_profile_ = std::move(*profile);
    }
  }

  // For HDR content (ST2084/HLG), try to enable DXGI swap chain HDR metadata
  const bool is_hdr = config.encoding_eotf == ColorUtils::EOTF::ST2084 ||
                      config.encoding_eotf == ColorUtils::EOTF::HLG;

  if (is_hdr) {
    // Enable DWM's advanced color for the window.
    // This is required for HDR output on Windows 10 1703+ / Windows 11.
    // We use DwmSetWindowAttribute with DWMWA_USE_IMMERSIVE_DARK_MODE
    // and rely on the swap chain's DXGISwapChain4::SetHDRMetaData.
    // The actual HDR setup is handled in the HDRManager.
    qInfo("ColorManager: HDR output requested (EOTF=%s), HDR metadata will be set by HDRManager.",
          ColorUtils::EOTFToString(config.encoding_eotf).c_str());
  }

  // For SDR content, if the display has a non-sRGB ICC profile, we need to
  // apply the color transform to the preview pixels before presenting.
  // The DWM does NOT apply ICC profiles to swap chain content automatically
  // (unlike macOS which applies the display profile via the window server).
  // Therefore, we must transform pixels ourselves.

  return true;

#else  // Non-Windows: stub
  (void)native_view_or_window;
  (void)config;
  return false;
#endif
}

auto ColorManager::DetectMonitorIccProfile(void* native_window) -> std::optional<MonitorIccProfile> {
#if defined(_WIN32)
  if (!native_window) {
    return std::nullopt;
  }

  HWND window = static_cast<HWND>(native_window);
  HMONITOR monitor = GetMonitorForWindow(window);
  if (!monitor) {
    return std::nullopt;
  }

  std::wstring profile_path = GetMonitorProfilePath(monitor);
  if (profile_path.empty()) {
    // No custom ICC profile — the display uses the default sRGB profile
    return std::nullopt;
  }

  MonitorIccProfile result;
  result.profile_path = profile_path;
  result.profile_bytes = ReadIccFile(profile_path);
  result.profile_hash = ComputeProfileHash(result.profile_bytes);

  return result;
#else
  (void)native_window;
  return std::nullopt;
#endif
}

auto ColorManager::GetOrCreateColorTransform(const ViewerDisplayConfig& source_config,
                                             const MonitorIccProfile&  display_profile)
    -> ColorTransformHandle {
#if defined(_WIN32)
  std::lock_guard<std::mutex> lock(cache_mutex_);

  ColorTransformCacheKey key;
  key.source_space = source_config.encoding_space;
  key.source_eotf = source_config.encoding_eotf;
  key.display_profile_hash = display_profile.profile_hash;

  auto it = transform_cache_.find(key);
  if (it != transform_cache_.end()) {
    return it->second;
  }

  // Create new transform
  auto src_bytes = GetSourceIccProfileBytes(source_config.encoding_space,
                                             source_config.encoding_eotf);
  if (src_bytes.empty()) {
    qWarning("ColorManager: failed to load source ICC profile for space=%s eotf=%s",
             ColorUtils::ColorSpaceToString(source_config.encoding_space).c_str(),
             ColorUtils::EOTFToString(source_config.encoding_eotf).c_str());
    return {};
  }

  HPROFILE src_profile = CreateIccProfileFromBytes(src_bytes);
  HPROFILE dst_profile = CreateIccProfileFromBytes(display_profile.profile_bytes);

  if (!src_profile) {
    qWarning("ColorManager: failed to create source ICC profile handle.");
    return {};
  }
  if (!dst_profile) {
    // If the display profile cannot be opened, fall back to sRGB
    auto srgb_bytes = GetSourceIccProfileBytes(ColorUtils::ColorSpace::REC709,
                                                ColorUtils::EOTF::GAMMA_2_2);
    dst_profile = CreateIccProfileFromBytes(srgb_bytes);
    if (!dst_profile) {
      CloseColorProfile(src_profile);
      qWarning("ColorManager: failed to create display ICC profile handle (sRGB fallback).");
      return {};
    }
    qWarning("ColorManager: display profile open failed, falling back to sRGB.");
  }

  HTRANSFORM hTransform = CreateTransformBetweenProfiles(src_profile, dst_profile);
  if (!hTransform) {
    CloseColorProfile(src_profile);
    CloseColorProfile(dst_profile);
    qWarning("ColorManager: CreateColorTransform failed (error=%lu).", GetLastError());
    return {};
  }

  ColorTransformHandle handle;
  handle.transform = hTransform;
  handle.src_profile = src_profile;
  handle.dst_profile = dst_profile;

  transform_cache_[key] = handle;
  return handle;
#else
  (void)source_config;
  (void)display_profile;
  return {};
#endif
}

auto ColorManager::ApplyTransformToBuffer(const ColorTransformHandle& handle,
                                          float* pixel_data, int width, int height) -> bool {
#if defined(_WIN32)
  if (!handle.transform || !pixel_data || width <= 0 || height <= 0) {
    return false;
  }

  // Windows ICM TranslateBitmapBits works with BMP/BGR order.
  // Our RGBA32F data needs to be converted to a format ICM understands,
  // transformed, then converted back.
  //
  // We use BM_FORMAT_32B_SCARGB (premultiplied ARGB, 32-bit floating-point)
  // which is supported by WCS (Windows Color System) on Windows 10+.

  const size_t pixel_count = static_cast<size_t>(width) * static_cast<size_t>(height);

  // TranslateBitmapBits expects a packed ARGB layout.
  // Our data is already RGBA32F, so we need to swap R<->A channels.
  // Actually, BM_FORMAT_32B_SCARGB uses ARGB order; our data is RGBA.
  // We'll use BM_FORMAT_32B_SCRGB which is RGBA order.

  // Prepare a BMFORMAT that matches our RGBA32F layout
  PVOID src_bits = static_cast<PVOID>(pixel_data);

  // We need a destination buffer because TranslateBitmapBits writes to a
  // separate output. We'll allocate, transform, then copy back.
  std::vector<float> dst_buffer(pixel_count * 4);

  BOOL result = TranslateBitmapBits(
      static_cast<HTRANSFORM>(handle.transform),
      src_bits,
      BM_FORMAT_32B_SCRGB,          // 32-bit scRGB (RGBA float)
      static_cast<DWORD>(width),
      static_cast<DWORD>(height),
      0,                             // input stride (0 = packed)
      dst_buffer.data(),
      BM_FORMAT_32B_SCRGB,          // output format
      0,                             // output stride (0 = packed)
      nullptr,                       // palette
      CMS_FORWARD);

  if (!result) {
    qWarning("ColorManager: TranslateBitmapBits failed (error=%lu).", GetLastError());
    return false;
  }

  // Copy transformed data back
  std::memcpy(pixel_data, dst_buffer.data(), pixel_count * 4 * sizeof(float));
  return true;
#else
  (void)handle;
  (void)pixel_data;
  (void)width;
  (void)height;
  return false;
#endif
}

void ColorManager::ReleaseColorTransform(ColorTransformHandle& handle) {
#if defined(_WIN32)
  if (handle.transform) {
    DeleteColorTransform(static_cast<HTRANSFORM>(handle.transform));
    handle.transform = nullptr;
  }
  if (handle.src_profile) {
    CloseColorProfile(static_cast<HPROFILE>(handle.src_profile));
    handle.src_profile = nullptr;
  }
  if (handle.dst_profile) {
    CloseColorProfile(static_cast<HPROFILE>(handle.dst_profile));
    handle.dst_profile = nullptr;
  }
#else
  (void)handle;
#endif
}

void ColorManager::SetDisplayProfileChangeCallback(DisplayProfileChangeCallback callback) {
  std::lock_guard<std::mutex> lock(cache_mutex_);
  profile_change_callback_ = std::move(callback);
}

auto ColorManager::CheckAndInvalidateDisplayProfile(void* native_window) -> bool {
#if defined(_WIN32)
  auto new_profile = DetectMonitorIccProfile(native_window);
  if (!new_profile.has_value()) {
    return false;
  }

  std::lock_guard<std::mutex> lock(cache_mutex_);
  if (cached_display_profile_.profile_hash == new_profile->profile_hash) {
    return false;  // No change
  }

  qInfo() << "ColorManager: display ICC profile changed from"
          << QString::fromWCharArray(cached_display_profile_.profile_path.c_str())
          << "to"
          << QString::fromWCharArray(new_profile->profile_path.c_str());

  // Invalidate all cached transforms since the display profile changed
  for (auto& [key, handle] : transform_cache_) {
    ReleaseColorTransform(handle);
  }
  transform_cache_.clear();

  cached_display_profile_ = std::move(*new_profile);

  if (profile_change_callback_) {
    profile_change_callback_(cached_display_profile_.profile_path);
  }

  return true;
#else
  (void)native_window;
  return false;
#endif
}

void ColorManager::ClearCache() {
  std::lock_guard<std::mutex> lock(cache_mutex_);
  for (auto& [key, handle] : transform_cache_) {
    ReleaseColorTransform(handle);
  }
  transform_cache_.clear();
  cached_display_profile_ = {};
}

}  // namespace alcedo

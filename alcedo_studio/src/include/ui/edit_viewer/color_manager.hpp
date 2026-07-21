//  Copyright 2026 Yurun Zi
//  SPDX-License-Identifier: GPL-3.0-only
//  Additional permission under GPLv3 section 7 applies; see the LICENSE file.

#pragma once

#include <cstdint>
#include <functional>
#include <mutex>
#include <optional>
#include <string>
#include <unordered_map>
#include <vector>

#include "ui/edit_viewer/frame_sink.hpp"

namespace alcedo {

struct MonitorIccProfile {
  std::wstring          profile_path;
  std::vector<uint8_t>  profile_bytes;
  std::string           profile_hash;   // MD5 or size+mtime for cache invalidation
};

struct ColorTransformCacheKey {
  ColorUtils::ColorSpace source_space;
  ColorUtils::EOTF       source_eotf;
  std::string            display_profile_hash;

  auto operator==(const ColorTransformCacheKey& other) const -> bool {
    return source_space == other.source_space && source_eotf == other.source_eotf &&
           display_profile_hash == other.display_profile_hash;
  }
};

struct ColorTransformCacheKeyHash {
  auto operator()(const ColorTransformCacheKey& key) const -> std::size_t {
    auto h = static_cast<std::size_t>(key.source_space);
    h = h * 31 + static_cast<std::size_t>(key.source_eotf);
    h = h * 31 + std::hash<std::string>{}(key.display_profile_hash);
    return h;
  }
};

// Opaque handle for a compiled color transform. Platform-specific internals
// are hidden behind void*; lifetime is managed by ColorManager.
struct ColorTransformHandle {
  void* transform = nullptr;   // HTRANSFORM on Windows, unused on macOS
  void* src_profile = nullptr; // HPROFILE on Windows
  void* dst_profile = nullptr; // HPROFILE on Windows
};

class ColorManager {
 public:
  // Apply the appropriate color space to a native window/view for preview
  // rendering. On macOS this sets the CAMetalLayer colorspace + EDR metadata.
  // On Windows this detects the monitor ICC profile and configures DXGI/DWm
  // color management for the swap chain.
  static auto ApplyWindowColorSpace(void* native_view_or_window,
                                    const ViewerDisplayConfig& config) -> bool;

  // ---- Windows Color Management API ----

  // Detect the ICC profile for the monitor that contains the given window.
  // Returns nullopt on non-Windows platforms or on failure.
  static auto DetectMonitorIccProfile(void* native_window) -> std::optional<MonitorIccProfile>;

  // Create a color transform from the working color space (source) to the
  // display's ICC profile (destination). The returned handle is cached
  // internally and must be released via ReleaseColorTransform.
  static auto GetOrCreateColorTransform(const ViewerDisplayConfig& source_config,
                                         const MonitorIccProfile& display_profile)
      -> ColorTransformHandle;

  // Apply a color transform to an RGBA32F host buffer in-place.
  // width * height * 4 floats expected in `pixel_data`.
  static auto ApplyTransformToBuffer(const ColorTransformHandle& handle,
                                     float* pixel_data, int width, int height) -> bool;

  // Release a previously created color transform and its associated resources.
  static void ReleaseColorTransform(ColorTransformHandle& handle);

  // Register a callback that fires when the display profile changes (e.g.
  // user changes monitor ICC in Windows settings). The callback receives the
  // new profile path.
  using DisplayProfileChangeCallback = std::function<void(const std::wstring& new_profile_path)>;
  static void SetDisplayProfileChangeCallback(DisplayProfileChangeCallback callback);

  // Check if the display profile has changed since the last call, and
  // invalidate cached transforms if so. Returns true if the profile changed.
  static auto CheckAndInvalidateDisplayProfile(void* native_window) -> bool;

  // Clear all cached transforms and profiles. Call on shutdown.
  static void ClearCache();

 private:
  static std::mutex                                             cache_mutex_;
  static std::unordered_map<ColorTransformCacheKey,
                             ColorTransformHandle,
                             ColorTransformCacheKeyHash>        transform_cache_;
  static MonitorIccProfile                                      cached_display_profile_;
  static DisplayProfileChangeCallback                           profile_change_callback_;
};

}  // namespace alcedo

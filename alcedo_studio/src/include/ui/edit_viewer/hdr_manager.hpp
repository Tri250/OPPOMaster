//  Copyright 2026 Yurun Zi
//  SPDX-License-Identifier: GPL-3.0-only
//  Additional permission under GPLv3 section 7 applies; see the LICENSE file.

#pragma once

#include <cstdint>
#include <functional>
#include <optional>
#include <string>

#include "ui/edit_viewer/frame_sink.hpp"

namespace alcedo {

// Describes the HDR capabilities and current state of a display.
struct HDRDisplayInfo {
  bool        is_hdr_capable = false;      // Display supports HDR output
  bool        is_hdr_enabled = false;       // HDR is currently active on the display
  float       max_luminance = 0.0f;        // Maximum full-frame luminance (nits)
  float       max_full_frame_luminance = 0.0f;  // Max full-frame (nits)
  float       min_luminance = 0.0f;         // Minimum luminance (nits)
  float       max_content_light_level = 0.0f;  // Content light level (nits)
  float       max_frame_average_light_level = 0.0f;  // Frame average (nits)
  uint32_t    red_primary_x = 0;            // CIE 1931 red primary x * 50000
  uint32_t    red_primary_y = 0;            // CIE 1931 red primary y * 50000
  uint32_t    green_primary_x = 0;          // CIE 1931 green primary x * 50000
  uint32_t    green_primary_y = 0;          // CIE 1931 green primary y * 50000
  uint32_t    blue_primary_x = 0;           // CIE 1931 blue primary x * 50000
  uint32_t    blue_primary_y = 0;           // CIE 1931 blue primary y * 50000
  uint32_t    white_point_x = 0;            // CIE 1931 white point x * 50000
  uint32_t    white_point_y = 0;            // CIE 1931 white point y * 50000
};

struct HDRToneMappingParams {
  float scene_max_nits = 1000.0f;     // Maximum luminance in scene-referred data
  float display_max_nits = 0.0f;      // Max display nits (0 = auto-detect)
  float display_min_nits = 0.005f;    // Min display nits
  float paper_white_nits = 203.0f;    // SDR reference white level in nits
  bool  hdr_preview_enabled = false;   // User toggle for HDR preview
};

// Manages Windows HDR display detection, swap chain HDR metadata, and
// tone mapping from scene-referred to display-referred for HDR displays.
class HDRManager {
 public:
  // Detect HDR capabilities of the monitor that contains the given window.
  // Uses IDXGIOutput6::GetDesc1 on Windows to query DXGI_COLOR_SPACE_TYPE
  // and display capabilities.
  static auto DetectHDRDisplay(void* native_window) -> HDRDisplayInfo;

  // Set up HDR metadata on the swap chain associated with the given window.
  // This calls IDXGISwapChain4::SetHDRMetaData with the appropriate
  // DXGI_HDR_METADATA_HDR10 structure.
  static auto SetSwapChainHDRMetadata(void* swap_chain, const HDRDisplayInfo& display_info,
                                      const HDRToneMappingParams& params) -> bool;

  // Apply tone mapping from scene-referred linear data to display-referred.
  // This is needed when the display is HDR-capable and HDR preview is enabled.
  // pixel_data: RGBA32F buffer (linear, scene-referred)
  // width/height: dimensions
  // params: tone mapping parameters
  // Returns true on success.
  static auto ApplyToneMapping(float* pixel_data, int width, int height,
                               const HDRToneMappingParams& params) -> bool;

  // Apply inverse tone mapping (display-referred to scene-referred) for
  // round-trip correctness when editing HDR content.
  static auto ApplyInverseToneMapping(float* pixel_data, int width, int height,
                                      const HDRToneMappingParams& params) -> bool;

  // Check if the window's display has switched between SDR/HDR and update
  // the internal state. Returns true if a change was detected.
  static auto CheckDisplayModeChange(void* native_window) -> bool;

  // Get/set the HDR preview enable/disable setting.
  static auto IsHDRPreviewEnabled() -> bool;
  static void SetHDRPreviewEnabled(bool enabled);

  // Get the cached display info (updated by DetectHDRDisplay / CheckDisplayModeChange)
  static auto GetCachedDisplayInfo() -> const HDRDisplayInfo&;

  // Register callback for HDR display mode changes
  using HDRDisplayChangeCallback = std::function<void(bool is_now_hdr)>;
  static void SetHDRDisplayChangeCallback(HDRDisplayChangeCallback callback);

  // Clear cached state on shutdown
  static void ClearCache();

 private:
  static HDRDisplayInfo           cached_display_info_;
  static bool                     hdr_preview_enabled_;
  static HDRDisplayChangeCallback display_change_callback_;
};

}  // namespace alcedo

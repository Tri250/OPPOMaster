//  Copyright 2026 Yurun Zi
//  SPDX-License-Identifier: GPL-3.0-only
//  Additional permission under GPLv3 section 7 applies; see the LICENSE file.

#include "ui/edit_viewer/hdr_manager.hpp"

#if defined(_WIN32)
#ifndef NOMINMAX
#define NOMINMAX
#endif
#include <Windows.h>
#include <dxgi1_6.h>
#pragma comment(lib, "dxgi.lib")
#pragma comment(lib, "user32.lib")
#endif

#include <QtCore/qlogging.h>
#include <QDebug>

#include <algorithm>
#include <cmath>
#include <cstring>

namespace alcedo {

// Static member definitions
HDRDisplayInfo           HDRManager::cached_display_info_;
bool                     HDRManager::hdr_preview_enabled_ = false;
HDRManager::HDRDisplayChangeCallback HDRManager::display_change_callback_;

#if defined(_WIN32)
namespace {

auto GetDXGIOutputForWindow(HWND window) -> IDXGIOutput6* {
  if (!window) {
    return nullptr;
  }

  HMONITOR monitor = MonitorFromWindow(window, MONITOR_DEFAULTTONEAREST);
  if (!monitor) {
    return nullptr;
  }

  // Enumerate DXGI adapters and outputs to find the one matching our monitor
  IDXGIAdapter1* adapter = nullptr;
  IDXGIFactory6* factory = nullptr;

  HRESULT hr = CreateDXGIFactory1(IID_PPV_ARGS(&factory));
  if (FAILED(hr) || !factory) {
    qWarning("HDRManager: CreateDXGIFactory1 failed (hr=0x%08X).", hr);
    return nullptr;
  }

  IDXGIOutput6* result_output = nullptr;

  for (UINT adapter_idx = 0;
       factory->EnumAdapterByGpuPreference(adapter_idx, DXGI_GPU_PREFERENCE_HIGH_PERFORMANCE,
                                           IID_PPV_ARGS(&adapter)) != DXGI_ERROR_NOT_FOUND;
       ++adapter_idx) {
    if (!adapter) {
      continue;
    }

    IDXGIOutput* output = nullptr;
    for (UINT output_idx = 0;
         adapter->EnumOutputs(output_idx, &output) != DXGI_ERROR_NOT_FOUND;
         ++output_idx) {
      if (!output) {
        continue;
      }

      DXGI_OUTPUT_DESC output_desc;
      hr = output->GetDesc(&output_desc);
      if (SUCCEEDED(hr) && output_desc.Monitor == monitor) {
        // Found the matching output — query for IDXGIOutput6
        IDXGIOutput6* output6 = nullptr;
        hr = output->QueryInterface(IID_PPV_ARGS(&output6));
        output->Release();
        if (SUCCEEDED(hr) && output6) {
          result_output = output6;
          adapter->Release();
          factory->Release();
          return result_output;
        }
        // QI failed, continue searching
      } else {
        output->Release();
      }
    }

    adapter->Release();
  }

  factory->Release();
  return nullptr;
}

}  // namespace
#endif  // _WIN32

// ============================================================================
// HDRManager Implementation
// ============================================================================

auto HDRManager::DetectHDRDisplay(void* native_window) -> HDRDisplayInfo {
  HDRDisplayInfo info;

#if defined(_WIN32)
  if (!native_window) {
    return info;
  }

  HWND window = static_cast<HWND>(native_window);
  IDXGIOutput6* output6 = GetDXGIOutputForWindow(window);
  if (!output6) {
    qWarning("HDRManager: could not get IDXGIOutput6 for window.");
    return info;
  }

  DXGI_OUTPUT_DESC1 desc1;
  HRESULT hr = output6->GetDesc1(&desc1);
  output6->Release();

  if (FAILED(hr)) {
    qWarning("HDRManager: IDXGIOutput6::GetDesc1 failed (hr=0x%08X).", hr);
    return info;
  }

  // Check if the display supports HDR
  info.is_hdr_capable = (desc1.ColorSpace == DXGI_COLOR_SPACE_RGB_FULLSCREEN_G2084_NONE_P2020);

  // Check if HDR is currently enabled on the display
  // On Windows 10 1703+, this is indicated by the color space being the HDR space
  // AND the display being in advanced color mode
  info.is_hdr_enabled = info.is_hdr_capable && (desc1.BitsPerColor > 8);

  // Fill in display capabilities from DXGI_OUTPUT_DESC1
  info.max_full_frame_luminance = static_cast<float>(desc1.MaxFullFrameLuminance);
  info.max_luminance = static_cast<float>(desc1.MaxLuminance);
  info.min_luminance = static_cast<float>(desc1.MinLuminance);

  // Default HDR10 metadata for a typical HDR display
  // These will be refined by SetSwapChainHDRMetadata
  if (info.is_hdr_capable) {
    info.max_content_light_level = info.max_luminance > 0 ? info.max_luminance : 1000.0f;
    info.max_frame_average_light_level =
        info.max_full_frame_luminance > 0 ? info.max_full_frame_luminance : 400.0f;

    // Standard Rec.2020 primaries (for most HDR displays)
    // x,y * 50000 per HDR10 spec
    info.red_primary_x = 34000;     // 0.680
    info.red_primary_y = 16000;     // 0.320
    info.green_primary_x = 13250;   // 0.265
    info.green_primary_y = 34500;   // 0.690
    info.blue_primary_x = 7500;     // 0.150
    info.blue_primary_y = 3000;     // 0.060
    info.white_point_x = 15635;     // 0.3127
    info.white_point_y = 16450;     // 0.3290
  }

  // Update cache
  cached_display_info_ = info;

  qInfo("HDRManager: HDR display detected: capable=%s enabled=%s "
        "max_luminance=%.1f min_luminance=%.4f",
        info.is_hdr_capable ? "yes" : "no",
        info.is_hdr_enabled ? "yes" : "no",
        info.max_luminance, info.min_luminance);

#else
  (void)native_window;
#endif

  return info;
}

auto HDRManager::SetSwapChainHDRMetadata(void*                       swap_chain,
                                         const HDRDisplayInfo&       display_info,
                                         const HDRToneMappingParams& params) -> bool {
#if defined(_WIN32)
  if (!swap_chain) {
    return false;
  }

  IDXGISwapChain4* swap_chain4 = nullptr;
  HRESULT hr = static_cast<IUnknown*>(swap_chain)->QueryInterface(
      IID_PPV_ARGS(&swap_chain4));
  if (FAILED(hr) || !swap_chain4) {
    qWarning("HDRManager: failed to get IDXGISwapChain4 (hr=0x%08X).", hr);
    return false;
  }

  if (!display_info.is_hdr_capable) {
    // Not an HDR display — set empty metadata to disable HDR
    hr = swap_chain4->SetHDRMetaData(DXGI_HDR_METADATA_TYPE_NONE, 0, nullptr);
    swap_chain4->Release();
    return SUCCEEDED(hr);
  }

  // Build DXGI_HDR_METADATA_HDR10
  DXGI_HDR_METADATA_HDR10 hdr10 = {};
  hdr10.RedPrimary[0] = display_info.red_primary_x;
  hdr10.RedPrimary[1] = display_info.red_primary_y;
  hdr10.GreenPrimary[0] = display_info.green_primary_x;
  hdr10.GreenPrimary[1] = display_info.green_primary_y;
  hdr10.BluePrimary[0] = display_info.blue_primary_x;
  hdr10.BluePrimary[1] = display_info.blue_primary_y;
  hdr10.WhitePoint[0] = display_info.white_point_x;
  hdr10.WhitePoint[1] = display_info.white_point_y;

  // Max/min luminance from the display
  const float max_nits = display_info.max_luminance > 0 ? display_info.max_luminance : 1000.0f;
  const float min_nits = display_info.min_luminance > 0 ? display_info.min_luminance : 0.005f;

  // Convert nits to the HDR10 format: nits * 10000
  hdr10.MaxMasteringLuminance = static_cast<UINT>(max_nits * 10000.0f);
  hdr10.MinMasteringLuminance = static_cast<UINT>(min_nits * 10000.0f);

  // Content light level
  const float cll = params.scene_max_nits > 0 ? params.scene_max_nits : max_nits;
  const float fall = cll * 0.4f;  // Conservative frame average
  hdr10.MaxContentLightLevel = static_cast<UINT>(cll);
  hdr10.MaxFrameAverageLightLevel = static_cast<UINT>(fall);

  hr = swap_chain4->SetHDRMetaData(
      DXGI_HDR_METADATA_TYPE_HDR10,
      sizeof(DXGI_HDR_METADATA_HDR10),
      &hdr10);

  swap_chain4->Release();

  if (FAILED(hr)) {
    qWarning("HDRManager: SetHDRMetaData failed (hr=0x%08X).", hr);
    return false;
  }

  qInfo("HDRManager: SetHDRMetaData succeeded (max_nits=%.1f, CLL=%.1f).",
        max_nits, cll);
  return true;

#else
  (void)swap_chain;
  (void)display_info;
  (void)params;
  return false;
#endif
}

auto HDRManager::ApplyToneMapping(float*                      pixel_data,
                                  int                          width,
                                  int                          height,
                                  const HDRToneMappingParams&  params) -> bool {
  if (!pixel_data || width <= 0 || height <= 0) {
    return false;
  }

  const size_t pixel_count = static_cast<size_t>(width) * static_cast<size_t>(height);

  if (params.hdr_preview_enabled) {
    // HDR preview: scale scene-referred values to the display's HDR range.
    // The pixel data is in linear, scene-referred (nits).
    // We need to apply the PQ (SMPTE ST 2084) transfer function so the display
    // can interpret the values correctly.

    const float max_display_nits = params.display_max_nits > 0
                                       ? params.display_max_nits
                                       : params.scene_max_nits;
    const float scale = 1.0f / max_display_nits;

    // PQ constants (SMPTE ST 2084)
    constexpr float m1 = 2610.0f / 16384.0f;      // 0.15930175781
    constexpr float m2 = 2523.0f / 32.0f;          // 78.84375
    constexpr float c1 = 3424.0f / 4096.0f;        // 0.8359375
    constexpr float c2 = 2413.0f / 128.0f;          // 18.8515625
    constexpr float c3 = 2392.0f / 128.0f;          // 18.6875

    for (size_t i = 0; i < pixel_count; ++i) {
      float* px = pixel_data + i * 4;

      for (int ch = 0; ch < 3; ++ch) {
        // Normalize to [0, 1] based on display max
        float normalized = std::clamp(px[ch] * scale, 0.0f, 1.0f);

        // Apply PQ (ST 2084) transfer function:
        // V_pq = ((c1 + c2 * L^n) / (1 + c3 * L^n))^m
        float L_m1 = std::pow(normalized, m1);
        float num = c1 + c2 * L_m1;
        float den = 1.0f + c3 * L_m1;
        float V = std::pow(num / den, m2);

        // Scale to [0, 1] for 10-bit PQ encoding (values > 1 are "above paper white")
        px[ch] = std::clamp(V, 0.0f, 1.0f);
      }
      // Alpha channel remains unchanged
    }
  } else {
    // SDR preview: apply tone mapping from scene-referred to display-referred.
    // Uses a simplified Reinhard-style tone mapping with knee adaptation.

    const float display_white = params.paper_white_nits;
    const float scene_white = params.scene_max_nits;

    if (scene_white <= 0.0f || display_white <= 0.0f) {
      return false;
    }

    // Calculate tone mapping parameters
    const float exposure = display_white / scene_white;
    const float shoulder = scene_white * 0.9f;  // Knee point at 90% of scene max

    for (size_t i = 0; i < pixel_count; ++i) {
      float* px = pixel_data + i * 4;

      for (int ch = 0; ch < 3; ++ch) {
        float linear = px[ch];

        // Apply exposure
        float mapped = linear * exposure;

        // Apply soft shoulder for highlights above the knee
        if (mapped > 1.0f) {
          // Reinhard-style soft clamp: y = 1 + (x - 1) / (1 + (x - 1) * shoulder_slope)
          const float overshoot = mapped - 1.0f;
          const float shoulder_slope = 0.5f;
          mapped = 1.0f + overshoot / (1.0f + overshoot * shoulder_slope);
        }

        // Apply sRGB OETF (gamma) for display
        if (mapped <= 0.0031308f) {
          mapped = 12.92f * mapped;
        } else {
          mapped = 1.055f * std::pow(mapped, 1.0f / 2.4f) - 0.055f;
        }

        px[ch] = std::clamp(mapped, 0.0f, 1.0f);
      }
    }
  }

  return true;
}

auto HDRManager::ApplyInverseToneMapping(float*                      pixel_data,
                                         int                          width,
                                         int                          height,
                                         const HDRToneMappingParams&  params) -> bool {
  if (!pixel_data || width <= 0 || height <= 0) {
    return false;
  }

  const size_t pixel_count = static_cast<size_t>(width) * static_cast<size_t>(height);

  if (params.hdr_preview_enabled) {
    // Inverse PQ: convert PQ-encoded values back to linear scene-referred
    const float max_display_nits = params.display_max_nits > 0
                                       ? params.display_max_nits
                                       : params.scene_max_nits;

    constexpr float m1 = 2610.0f / 16384.0f;
    constexpr float m2 = 2523.0f / 32.0f;
    constexpr float c1 = 3424.0f / 4096.0f;
    constexpr float c2 = 2413.0f / 128.0f;
    constexpr float c3 = 2392.0f / 128.0f;

    for (size_t i = 0; i < pixel_count; ++i) {
      float* px = pixel_data + i * 4;

      for (int ch = 0; ch < 3; ++ch) {
        float V = std::clamp(px[ch], 0.0f, 1.0f);

        // Inverse PQ: L = ((max(V^(1/m2) - c1, 0) / (c2 - c3 * V^(1/m2))) ^ (1/m1))
        float V_m2_inv = std::pow(V, 1.0f / m2);
        float num = std::max(V_m2_inv - c1, 0.0f);
        float den = c2 - c3 * V_m2_inv;

        if (den <= 0.0f) {
          px[ch] = max_display_nits;
          continue;
        }

        float L = std::pow(num / den, 1.0f / m1);
        px[ch] = std::clamp(L * max_display_nits, 0.0f, max_display_nits);
      }
    }
  } else {
    // Inverse sRGB OETF: convert display-referred back to linear
    const float display_white = params.paper_white_nits;
    const float scene_white = params.scene_max_nits;

    for (size_t i = 0; i < pixel_count; ++i) {
      float* px = pixel_data + i * 4;

      for (int ch = 0; ch < 3; ++ch) {
        float srgb = std::clamp(px[ch], 0.0f, 1.0f);

        // Inverse sRGB EOTF
        float linear;
        if (srgb <= 0.04045f) {
          linear = srgb / 12.92f;
        } else {
          linear = std::pow((srgb + 0.055f) / 1.055f, 2.4f);
        }

        // Scale back to scene-referred nits
        if (scene_white > 0.0f && display_white > 0.0f) {
          px[ch] = linear * scene_white / display_white;
        } else {
          px[ch] = linear;
        }
      }
    }
  }

  return true;
}

auto HDRManager::CheckDisplayModeChange(void* native_window) -> bool {
  auto new_info = DetectHDRDisplay(native_window);

  const bool was_hdr = cached_display_info_.is_hdr_enabled;
  const bool is_hdr = new_info.is_hdr_enabled;

  cached_display_info_ = new_info;

  if (was_hdr != is_hdr) {
    qInfo("HDRManager: display mode changed: %s -> %s",
          was_hdr ? "HDR" : "SDR", is_hdr ? "HDR" : "SDR");

    if (display_change_callback_) {
      display_change_callback_(is_hdr);
    }
    return true;
  }

  return false;
}

auto HDRManager::IsHDRPreviewEnabled() -> bool {
  return hdr_preview_enabled_;
}

void HDRManager::SetHDRPreviewEnabled(bool enabled) {
  hdr_preview_enabled_ = enabled;
  qInfo("HDRManager: HDR preview %s.", enabled ? "enabled" : "disabled");
}

auto HDRManager::GetCachedDisplayInfo() -> const HDRDisplayInfo& {
  return cached_display_info_;
}

void HDRManager::SetHDRDisplayChangeCallback(HDRDisplayChangeCallback callback) {
  display_change_callback_ = std::move(callback);
}

void HDRManager::ClearCache() {
  cached_display_info_ = {};
  hdr_preview_enabled_ = false;
  display_change_callback_ = nullptr;
}

}  // namespace alcedo

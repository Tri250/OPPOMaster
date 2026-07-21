//  Copyright 2026 Yurun Zi
//  SPDX-License-Identifier: GPL-3.0-only
//  Additional permission under GPLv3 section 7 applies; see the LICENSE file.

#import <AppKit/AppKit.h>
#include <CoreGraphics/CGColorSpace.h>
#import <CoreGraphics/CoreGraphics.h>
#import <QuartzCore/CAMetalLayer.h>
#import <QuartzCore/QuartzCore.h>
#import <ColorSync/ColorSync.h>
#include <CoreServices/CoreServices.h>

#include "ui/edit_viewer/color_manager.hpp"

#include <QtCore/qlogging.h>
#include <QDebug>
#include <QString>
#include <algorithm>
#include <cmath>
#include <limits>

namespace alcedo {

// Static member definitions (shared by all platforms)
std::mutex ColorManager::cache_mutex_;
std::unordered_map<ColorTransformCacheKey,
                   ColorTransformHandle,
                   ColorTransformCacheKeyHash> ColorManager::transform_cache_;
MonitorIccProfile ColorManager::cached_display_profile_;
ColorManager::DisplayProfileChangeCallback ColorManager::profile_change_callback_;

namespace {

auto FindMetalLayerInLayerHierarchy(CALayer* layer) -> CAMetalLayer* {
  if (!layer) {
    return nil;
  }
  if ([layer isKindOfClass:[CAMetalLayer class]]) {
    return (CAMetalLayer*)layer;
  }
  for (CALayer* sublayer in layer.sublayers) {
    if (CAMetalLayer* metal_layer = FindMetalLayerInLayerHierarchy(sublayer)) {
      return metal_layer;
    }
  }
  return nil;
}

auto FindMetalLayerInViewHierarchy(NSView* view) -> CAMetalLayer* {
  if (!view) {
    return nil;
  }
  if (CAMetalLayer* metal_layer = FindMetalLayerInLayerHierarchy(view.layer)) {
    return metal_layer;
  }
  for (NSView* subview in view.subviews) {
    if (CAMetalLayer* metal_layer = FindMetalLayerInViewHierarchy(subview)) {
      return metal_layer;
    }
  }
  return nil;
}

auto ResolveLinearColorSpace(ColorUtils::ColorSpace encoding_space, bool* exact_match)
    -> CFStringRef {
  if (exact_match) {
    *exact_match = true;
  }

  switch (encoding_space) {
    case ColorUtils::ColorSpace::REC709:
      return kCGColorSpaceLinearSRGB;
    case ColorUtils::ColorSpace::P3_D65:
      return kCGColorSpaceExtendedLinearDisplayP3;
    case ColorUtils::ColorSpace::P3_D60:
    case ColorUtils::ColorSpace::P3_DCI:
      if (exact_match) {
        *exact_match = false;
      }
      return kCGColorSpaceExtendedLinearDisplayP3;
    case ColorUtils::ColorSpace::REC2020:
      return kCGColorSpaceExtendedLinearITUR_2020;
    case ColorUtils::ColorSpace::XYZ:
      if (exact_match) {
        *exact_match = false;
      }
      return kCGColorSpaceGenericXYZ;
    default:
      if (exact_match) {
        *exact_match = false;
      }
      return kCGColorSpaceExtendedLinearSRGB;
  }
}

auto ResolveNamedColorSpace(const ViewerDisplayConfig& config, bool* exact_match) -> CFStringRef {
  if (config.encoding_eotf == ColorUtils::EOTF::LINEAR) {
    return ResolveLinearColorSpace(config.encoding_space, exact_match);
  }

  if (exact_match) {
    *exact_match = true;
  }

  switch (config.encoding_space) {
    case ColorUtils::ColorSpace::REC709:
      switch (config.encoding_eotf) {
        case ColorUtils::EOTF::ST2084:
          return kCGColorSpaceITUR_709_PQ;
        case ColorUtils::EOTF::HLG:
          return kCGColorSpaceITUR_709_HLG;
        case ColorUtils::EOTF::BT1886:
        case ColorUtils::EOTF::GAMMA_2_2:
          return kCGColorSpaceSRGB;
        default:
          if (exact_match) {
            *exact_match = false;
          }
          return kCGColorSpaceSRGB;
      }
    case ColorUtils::ColorSpace::P3_D65:
      switch (config.encoding_eotf) {
        case ColorUtils::EOTF::ST2084:
          return kCGColorSpaceDisplayP3_PQ;
        case ColorUtils::EOTF::HLG:
          return kCGColorSpaceDisplayP3_HLG;
        case ColorUtils::EOTF::GAMMA_2_2:
          return kCGColorSpaceDisplayP3;
        default:
          if (exact_match) {
            *exact_match = false;
          }
          return kCGColorSpaceDisplayP3;
      }
    case ColorUtils::ColorSpace::P3_D60:
      if (exact_match) {
        *exact_match = false;
      }
      return kCGColorSpaceDisplayP3;
    case ColorUtils::ColorSpace::P3_DCI:
      if (config.encoding_eotf != ColorUtils::EOTF::GAMMA_2_6 && exact_match) {
        *exact_match = false;
      }
      return kCGColorSpaceDCIP3;
    case ColorUtils::ColorSpace::REC2020:
      switch (config.encoding_eotf) {
        case ColorUtils::EOTF::ST2084:
          return kCGColorSpaceITUR_2100_PQ;
        case ColorUtils::EOTF::HLG:
          return kCGColorSpaceITUR_2100_HLG;
        default:
          return kCGColorSpaceITUR_2020;
      }
    case ColorUtils::ColorSpace::XYZ:
      return kCGColorSpaceGenericXYZ;
    default:
      if (exact_match) {
        *exact_match = false;
      }
      return kCGColorSpaceSRGB;
  }
}

void LogFallbackColorSpace(const ViewerDisplayConfig& config, CFStringRef resolved_name) {
  QString resolved = QStringLiteral("<null>");
  if (resolved_name) {
    char buffer[256] = {};
    if (CFStringGetCString(resolved_name, buffer, sizeof(buffer), kCFStringEncodingUTF8)) {
      resolved = QString::fromUtf8(buffer);
    }
  }
  qWarning().noquote()
      << QStringLiteral("ColorManager: falling back to %1 for encoding_space=%2 encoding_eotf=%3")
             .arg(resolved,
                  QString::fromStdString(ColorUtils::ColorSpaceToString(config.encoding_space)),
                  QString::fromStdString(ColorUtils::EOTFToString(config.encoding_eotf)));
}

auto ClampHdrPeakLuminance(float peak_luminance) -> float {
  if (!std::isfinite(peak_luminance)) {
    return 100.0f;
  }
  return std::clamp(peak_luminance, 100.0f, 10000.0f);
}

auto ResolveEDRMetadata(const ViewerDisplayConfig& config) -> CAEDRMetadata* {
  if (@available(macOS 10.15, *)) {
    if (config.encoding_eotf == ColorUtils::EOTF::ST2084) {
      const float peak_luminance = ClampHdrPeakLuminance(config.peak_luminance);
      return [CAEDRMetadata HDR10MetadataWithMinLuminance:0.0001f
                                             maxLuminance:peak_luminance
                                       opticalOutputScale:10000.0f];
    }
    if (config.encoding_eotf == ColorUtils::EOTF::HLG) {
      return [CAEDRMetadata HLGMetadata];
    }
  }
  return nil;
}

}  // namespace

auto ColorManager::ApplyWindowColorSpace(void*                      native_view_or_window,
                                         const ViewerDisplayConfig& config) -> bool {
  if (!native_view_or_window) {
    return false;
  }

  id        object = (__bridge id)native_view_or_window;

  NSWindow* window = nil;
  NSView*   view   = nil;
  if ([object isKindOfClass:[NSWindow class]]) {
    window = (NSWindow*)object;
    view   = window.contentView;
  } else if ([object isKindOfClass:[NSView class]]) {
    view   = (NSView*)object;
    window = view.window;
  } else {
    return false;
  }

  CAMetalLayer* metal_layer = FindMetalLayerInViewHierarchy(view);
  if (!metal_layer && window) {
    metal_layer = FindMetalLayerInViewHierarchy(window.contentView);
  }
  if (!metal_layer && window.contentView.superview) {
    metal_layer = FindMetalLayerInViewHierarchy(window.contentView.superview);
  }
  if (!metal_layer) {
    return false;
  }

  bool        exact_match = true;
  CFStringRef color_name  = ResolveNamedColorSpace(config, &exact_match);
  if (!color_name) {
    return false;
  }

  const bool uses_hdr_transfer = config.encoding_eotf == ColorUtils::EOTF::ST2084 ||
                                 config.encoding_eotf == ColorUtils::EOTF::HLG;
  metal_layer.wantsExtendedDynamicRangeContent = uses_hdr_transfer;
  if (@available(macOS 10.15, *)) {
    metal_layer.EDRMetadata = uses_hdr_transfer ? ResolveEDRMetadata(config) : nil;
  }

  CGColorSpaceRef color_space = CGColorSpaceCreateWithName(color_name);
  if (!color_space) {
    return false;
  }

  metal_layer.colorspace = color_space;

  CGColorSpaceRelease(color_space);

  if (!exact_match) {
    LogFallbackColorSpace(config, color_name);
  }
  return true;
}

// ---- macOS ColorSync ICC profile detection ----

auto ColorManager::DetectMonitorIccProfile(void* native_window) -> std::optional<MonitorIccProfile> {
  if (!native_window) {
    return std::nullopt;
  }

  id object = (__bridge id)native_window;
  NSScreen* screen = nil;

  if ([object isKindOfClass:[NSWindow class]]) {
    screen = [(NSWindow*)object screen];
  } else if ([object isKindOfClass:[NSView class]]) {
    NSWindow* window = [(NSView*)object window];
    if (window) {
      screen = window.screen;
    }
  }

  if (!screen) {
    return std::nullopt;
  }

  // Get the ColorSync profile UUID for the screen
  CGDirectDisplayID display_id = 0;
  if (@available(macOS 10.15, *)) {
    NSNumber* screen_num = screen.deviceDescription[@"NSScreenNumber"];
    if (screen_num) {
      display_id = (CGDirectDisplayID)[screen_num unsignedIntValue];
    }
  }

  if (display_id == 0) {
    return std::nullopt;
  }

  // Retrieve the ColorSync profile URL for the display
  CFDictionaryRef uuid_info = nullptr;
  if (@available(macOS 10.11, *)) {
    uuid_info = ColorSyncDeviceGetDeviceInfo(
        kColorSyncDisplayDeviceClass,
        (__bridge CFStringRef)[NSString stringWithFormat:@"%u", display_id],
        false,
        nullptr);
  }

  MonitorIccProfile result;

  if (uuid_info) {
    CFStringRef profile_url_str = (CFStringRef)CFDictionaryGetValue(
        uuid_info, kColorSyncDeviceProfileURL);
    if (profile_url_str) {
      char buffer[1024] = {};
      if (CFStringGetCString(profile_url_str, buffer, sizeof(buffer),
                             kCFStringEncodingUTF8)) {
        result.profile_path = std::wstring(buffer, buffer + strlen(buffer));
      }
    }
    CFRelease(uuid_info);
  }

  // Attempt to read the ICC profile data from the display
  CMProfileRef cm_profile = nullptr;
  if (@available(macOS 10.13, *)) {
    CMError err = CMGetProfileByAVID(std::numeric_limits<UInt32>::max(), &cm_profile);
    if (err != noErr) {
      // Fallback: try getting the profile from the display ID
      err = CMGetProfileByAVID(display_id, &cm_profile);
    }
  }

  if (cm_profile) {
    CFDataRef data = nullptr;
    CMProfileCopyDescriptionData(cm_profile, &data);
    if (data) {
      const UInt8* bytes = CFDataGetBytePtr(data);
      CFIndex length = CFDataGetLength(data);
      if (bytes && length > 0) {
        result.profile_bytes.assign(bytes, bytes + length);
      }
      CFRelease(data);
    }
    CMCloseProfile(cm_profile);
  }

  if (result.profile_bytes.empty()) {
    return std::nullopt;
  }

  // Compute a simple hash for cache invalidation
  if (!result.profile_bytes.empty()) {
    const size_t sz = result.profile_bytes.size();
    const uint32_t h1 = static_cast<uint32_t>(sz);
    const uint32_t h2 = static_cast<uint32_t>(result.profile_bytes[0]) |
                         (static_cast<uint32_t>(result.profile_bytes[sz > 1 ? sz - 1 : 0]) << 8);
    result.profile_hash = std::to_string(h1) + "_" + std::to_string(h2);
  }

  return result;
}

auto ColorManager::GetOrCreateColorTransform(const ViewerDisplayConfig& source_config,
                                             const MonitorIccProfile&  display_profile)
    -> ColorTransformHandle {
  (void)source_config;
  (void)display_profile;
  return {};
}

auto ColorManager::ApplyTransformToBuffer(const ColorTransformHandle& handle,
                                          float* pixel_data, int width, int height) -> bool {
  (void)handle;
  (void)pixel_data;
  (void)width;
  (void)height;
  return false;
}

void ColorManager::ReleaseColorTransform(ColorTransformHandle& handle) {
  (void)handle;
}

void ColorManager::SetDisplayProfileChangeCallback(DisplayProfileChangeCallback callback) {
  std::lock_guard<std::mutex> lock(cache_mutex_);
  profile_change_callback_ = std::move(callback);
}

auto ColorManager::CheckAndInvalidateDisplayProfile(void* native_window) -> bool {
  (void)native_window;
  return false;
}

void ColorManager::ClearCache() {
  std::lock_guard<std::mutex> lock(cache_mutex_);
  transform_cache_.clear();
  cached_display_profile_ = {};
}

}  // namespace alcedo

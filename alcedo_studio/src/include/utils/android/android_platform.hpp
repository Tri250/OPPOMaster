//  Copyright 2026 Yurun Zi
//  SPDX-License-Identifier: GPL-3.0-only
//  Additional permission under GPLv3 section 7 applies; see the LICENSE file.

#pragma once

#include <cstdint>
#include <string>

struct ANativeWindow;

namespace alcedo::android {

/// Display information for the current Android device.
struct DisplayInfo {
  float density_dpi       = 0.0f;   // Screen density in DPI
  float refresh_rate_hz   = 0.0f;   // Screen refresh rate in Hz
  int   width_pixels      = 0;      // Screen width in pixels
  int   height_pixels     = 0;      // Screen height in pixels
  float density_scale     = 1.0f;   // Density scale factor (density_dpi / 160)
};

/// AndroidPlatform provides Android-specific platform services via JNI.
/// All methods use QJniObject (Qt6 Android JNI) internally.
class AndroidPlatform {
 public:
  static auto Instance() -> AndroidPlatform&;

  /// Get the ANativeWindow from the current Qt surface.
  /// Returns nullptr if the window cannot be obtained.
  auto GetNativeWindow() -> ANativeWindow*;

  /// Get the app's internal (private) storage path.
  /// Maps to Context.getFilesDir() on Android.
  auto GetInternalStoragePath() -> std::string;

  /// Get the external storage path for shared photo storage.
  /// Maps to Environment.getExternalStorageDirectory() on Android.
  auto GetExternalStoragePath() -> std::string;

  /// Get the app's cache directory.
  /// Maps to Context.getCacheDir() on Android.
  auto GetCachePath() -> std::string;

  /// Request runtime storage permission (READ_EXTERNAL_STORAGE /
  /// READ_MEDIA_IMAGES for API 33+).
  /// Returns true if the permission was already granted or was just granted.
  auto RequestStoragePermission() -> bool;

  /// Get display information for the default display.
  auto GetDisplayInfo() -> DisplayInfo;

  /// Check if the display supports HDR (HDR-capable display mode).
  auto IsHdrSupported() -> bool;

  /// Trigger haptic feedback with the given duration in milliseconds.
  void Vibrate(int duration_ms);

  /// Share an image file via Android's share sheet.
  /// @param file_path Absolute path to the image file.
  void ShareImage(const std::string& file_path);

  /// Open an image in the system gallery app.
  /// @param file_path Absolute path to the image file.
  void OpenInGallery(const std::string& file_path);

 private:
  AndroidPlatform() = default;
};

}  // namespace alcedo::android

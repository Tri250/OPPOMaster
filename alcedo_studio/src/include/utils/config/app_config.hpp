//  Copyright 2026 Yurun Zi
//  SPDX-License-Identifier: GPL-3.0-only
//  Additional permission under GPLv3 section 7 applies; see the LICENSE file.

#pragma once

#include <cstdint>
#include <filesystem>
#include <functional>
#include <mutex>
#include <optional>
#include <string>
#include <unordered_map>

#include <nlohmann/json.hpp>

namespace alcedo {

// Central application configuration. Loads from a JSON config file and
// provides typed access to all configurable parameters with per-platform
// defaults and validation.
//
// Thread-safe: all access methods are guarded by an internal mutex.
class AppConfig {
 public:
  // Get the singleton instance.
  static auto Instance() -> AppConfig&;

  // Load configuration from the given JSON file. If the file doesn't exist,
  // a default config is created at that path.
  void LoadFromFile(const std::filesystem::path& config_path);

  // Save the current configuration to the file it was loaded from.
  void SaveToFile() const;

  // Reload the configuration from disk (watches for external changes).
  void Reload();

  // Get the path the config was loaded from.
  auto ConfigFilePath() const -> std::filesystem::path;

  // ---- Typed accessors with defaults ----

  // Rendering / preview
  int    FastPreviewMaxLongEdge() const;
  int    QualityBasePreviewMaxLongEdge() const;
  int    HsReferenceMaskMaxLongEdge() const;
  int    FullResPreviewMaxLongEdge() const;

  // Export
  int    DefaultJpegQuality() const;
  int    DefaultWebpQuality() const;
  int    DefaultPngCompression() const;
  int    DefaultTiffCompression() const;
  float  UltraHdrSdrWhiteNits() const;
  float  UltraHdrHlgPeakNits() const;
  float  UltraHdrPqPeakNits() const;
  int    UltraHdrGainMapScaleFactor() const;

  // Batch processing
  int    DefaultBatchSize() const;
  int    MaxBatchSize() const;

  // GPU / compute
  int    GpuThreadCount() const;
  int    CpuThreadCount() const;
  int    OpenCLWorkGroupSize() const;
  int    CUDABlockSize() const;

  // Cache
  int    ThumbnailCacheJpegQuality() const;
  int    ThumbnailCacheMaxSizeMB() const;
  uint32_t ThumbnailCacheSchemaVersion() const;

  // Timeouts (milliseconds)
  int    RawProcessingTimeoutMs() const;
  int    GpuInitializationTimeoutMs() const;
  int    ExportTimeoutMs() const;

  // Memory management
  float  MemorySafetyMarginFraction() const;
  int    MemoryBudgetRefreshIntervalMs() const;

  // NN / AI
  int    DemosaicNetBatchSize() const;
  float  DemosaicNetGammaEncode() const;

  // Logging
  std::string LogLevel() const;
  int         LogMaxFileSizeKB() const;
  int         LogMaxFileCount() const;
  std::string LogFilePath() const;

  // ---- Setters (for UI-driven config changes) ----
  void SetFastPreviewMaxLongEdge(int value);
  void SetQualityBasePreviewMaxLongEdge(int value);
  void SetHsReferenceMaskMaxLongEdge(int value);
  void SetFullResPreviewMaxLongEdge(int value);
  void SetDefaultJpegQuality(int value);
  void SetDefaultBatchSize(int value);
  void SetLogLevel(const std::string& level);
  void SetMemorySafetyMarginFraction(float fraction);

  // Generic typed access for extension.
  template <typename T>
  auto Get(const std::string& key, const T& default_value) const -> T {
    std::lock_guard<std::mutex> lock(mutex_);
    if (config_.contains(key) && !config_.at(key).is_null()) {
      try {
        return config_.at(key).get<T>();
      } catch (...) {
        return default_value;
      }
    }
    return default_value;
  }

  template <typename T>
  void Set(const std::string& key, const T& value) {
    std::lock_guard<std::mutex> lock(mutex_);
    config_[key] = value;
  }

  // Set a callback to be invoked when the config changes.
  using ConfigChangeCallback = std::function<void(const std::string& key)>;
  void SetConfigChangeCallback(ConfigChangeCallback callback);

 private:
  AppConfig();

  auto GetDefaultConfig() const -> nlohmann::json;
  void Validate();
  void MergeWithDefaults();
  void NotifyChange(const std::string& key);

  mutable std::mutex mutex_;
  nlohmann::json     config_;
  std::filesystem::path config_path_;
  ConfigChangeCallback change_callback_;
};

}  // namespace alcedo

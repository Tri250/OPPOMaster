//  Copyright 2026 Yurun Zi
//  SPDX-License-Identifier: GPL-3.0-only
//  Additional permission under GPLv3 section 7 applies; see the LICENSE file.

#include "utils/config/app_config.hpp"

#include <algorithm>
#include <cmath>
#include <fstream>
#include <limits>
#include <mutex>
#include <stdexcept>
#include <string>
#include <utility>

#include "utils/diagnostics/app_logging.hpp"

namespace alcedo {

auto AppConfig::Instance() -> AppConfig& {
  static AppConfig instance;
  return instance;
}

AppConfig::AppConfig() {
  config_ = GetDefaultConfig();
}

auto AppConfig::GetDefaultConfig() const -> nlohmann::json {
  nlohmann::json defaults;

  // ---- Rendering / preview ----
  defaults["fast_preview_max_long_edge"]         = 2560;
  defaults["quality_base_preview_max_long_edge"]  = 4096;
  defaults["hs_reference_mask_max_long_edge"]     = 2048;
  defaults["full_res_preview_max_long_edge"]      = 8192;

  // ---- Export ----
  defaults["default_jpeg_quality"]       = 95;
  defaults["default_webp_quality"]       = 90;
  defaults["default_png_compression"]    = 5;
  defaults["default_tiff_compression"]   = 1;  // LZW
  defaults["ultra_hdr_sdr_white_nits"]   = 203.0f;
  defaults["ultra_hdr_hlg_peak_nits"]    = 1000.0f;
  defaults["ultra_hdr_pq_peak_nits"]     = 10000.0f;
  defaults["ultra_hdr_gain_map_scale"]   = 1;

  // ---- Batch processing ----
  defaults["default_batch_size"]         = 4;
  defaults["max_batch_size"]             = 64;

  // ---- GPU / compute ----
#ifdef __APPLE__
  defaults["gpu_thread_count"]           = 1;
  defaults["cpu_thread_count"]           = static_cast<int>(std::thread::hardware_concurrency());
#else
  defaults["gpu_thread_count"]           = 2;
  defaults["cpu_thread_count"]           = static_cast<int>(std::thread::hardware_concurrency());
#endif
  defaults["opencl_work_group_size"]     = 256;
  defaults["cuda_block_size"]            = 256;

  // ---- Cache ----
  defaults["thumbnail_cache_jpeg_quality"] = 85;
  defaults["thumbnail_cache_max_size_mb"]  = 500;
  defaults["thumbnail_cache_schema_version"] = 1;

  // ---- Timeouts (ms) ----
  defaults["raw_processing_timeout_ms"]  = 30000;
  defaults["gpu_init_timeout_ms"]        = 15000;
  defaults["export_timeout_ms"]          = 60000;

  // ---- Memory management ----
  defaults["memory_safety_margin_fraction"] = 0.2f;
  defaults["memory_budget_refresh_ms"]      = 5000;

  // ---- NN / AI ----
  defaults["demosaicnet_batch_size"]     = 2;
  defaults["demosaicnet_gamma_encode"]   = 1.0f / 2.2f;

  // ---- Logging ----
  defaults["log_level"]           = "info";
  defaults["log_max_file_size_kb"] = 10240;
  defaults["log_max_file_count"]   = 5;
  defaults["log_file_path"]       = "";

  return defaults;
}

void AppConfig::LoadFromFile(const std::filesystem::path& config_path) {
  std::lock_guard<std::mutex> lock(mutex_);
  config_path_ = config_path;

  if (!std::filesystem::exists(config_path)) {
    // Create default config file.
    config_ = GetDefaultConfig();
    try {
      std::ofstream out(config_path);
      out << config_.dump(2);
      out.close();
      APP_LOG_INFO_DEFAULT("AppConfig: Created default config at %s", config_path.string().c_str());
    } catch (const std::exception& e) {
      APP_LOG_WARN_DEFAULT("AppConfig: Failed to create default config file: %s", e.what());
    }
    return;
  }

  try {
    std::ifstream in(config_path);
    auto loaded = nlohmann::json::parse(in, nullptr, false);
    if (loaded.is_discarded()) {
      APP_LOG_WARN_DEFAULT("AppConfig: Failed to parse config file, using defaults");
      config_ = GetDefaultConfig();
      return;
    }
    config_ = loaded;
    MergeWithDefaults();
    Validate();
    APP_LOG_INFO_DEFAULT("AppConfig: Loaded config from %s", config_path.string().c_str());
  } catch (const std::exception& e) {
    APP_LOG_WARN_DEFAULT("AppConfig: Error loading config: %s", e.what());
    config_ = GetDefaultConfig();
  }
}

void AppConfig::SaveToFile() const {
  std::lock_guard<std::mutex> lock(mutex_);
  if (config_path_.empty()) return;

  try {
    std::ofstream out(config_path_);
    out << config_.dump(2);
    out.close();
    APP_LOG_DEBUG_DEFAULT("AppConfig: Saved config to %s", config_path_.string().c_str());
  } catch (const std::exception& e) {
    APP_LOG_WARN_DEFAULT("AppConfig: Failed to save config: %s", e.what());
  }
}

void AppConfig::Reload() {
  if (config_path_.empty()) return;
  LoadFromFile(config_path_);
}

auto AppConfig::ConfigFilePath() const -> std::filesystem::path {
  std::lock_guard<std::mutex> lock(mutex_);
  return config_path_;
}

void AppConfig::MergeWithDefaults() {
  auto defaults = GetDefaultConfig();
  // Add any missing keys from defaults.
  for (auto it = defaults.begin(); it != defaults.end(); ++it) {
    if (!config_.contains(it.key())) {
      config_[it.key()] = it.value();
    }
  }
}

void AppConfig::Validate() {
  // Clamp numeric values to reasonable ranges.
  if (config_.contains("fast_preview_max_long_edge")) {
    auto& v = config_["fast_preview_max_long_edge"];
    if (v.is_number()) {
      v = std::clamp(v.get<int>(), 512, 16384);
    }
  }
  if (config_.contains("default_jpeg_quality")) {
    auto& v = config_["default_jpeg_quality"];
    if (v.is_number()) {
      v = std::clamp(v.get<int>(), 1, 100);
    }
  }
  if (config_.contains("memory_safety_margin_fraction")) {
    auto& v = config_["memory_safety_margin_fraction"];
    if (v.is_number()) {
      v = std::clamp(v.get<float>(), 0.0f, 0.9f);
    }
  }
  if (config_.contains("log_level")) {
    auto& v = config_["log_level"];
    if (v.is_string()) {
      const auto level = v.get<std::string>();
      if (level != "trace" && level != "debug" && level != "info" &&
          level != "warn" && level != "error" && level != "critical" && level != "off") {
        v = "info";
      }
    }
  }
}

void AppConfig::NotifyChange(const std::string& key) {
  if (change_callback_) {
    change_callback_(key);
  }
}

// ---- Typed accessors ----

int AppConfig::FastPreviewMaxLongEdge() const {
  return Get<int>("fast_preview_max_long_edge", 2560);
}
int AppConfig::QualityBasePreviewMaxLongEdge() const {
  return Get<int>("quality_base_preview_max_long_edge", 4096);
}
int AppConfig::HsReferenceMaskMaxLongEdge() const {
  return Get<int>("hs_reference_mask_max_long_edge", 2048);
}
int AppConfig::FullResPreviewMaxLongEdge() const {
  return Get<int>("full_res_preview_max_long_edge", 8192);
}

int AppConfig::DefaultJpegQuality() const {
  return Get<int>("default_jpeg_quality", 95);
}
int AppConfig::DefaultWebpQuality() const {
  return Get<int>("default_webp_quality", 90);
}
int AppConfig::DefaultPngCompression() const {
  return Get<int>("default_png_compression", 5);
}
int AppConfig::DefaultTiffCompression() const {
  return Get<int>("default_tiff_compression", 1);
}
float AppConfig::UltraHdrSdrWhiteNits() const {
  return Get<float>("ultra_hdr_sdr_white_nits", 203.0f);
}
float AppConfig::UltraHdrHlgPeakNits() const {
  return Get<float>("ultra_hdr_hlg_peak_nits", 1000.0f);
}
float AppConfig::UltraHdrPqPeakNits() const {
  return Get<float>("ultra_hdr_pq_peak_nits", 10000.0f);
}
int AppConfig::UltraHdrGainMapScaleFactor() const {
  return Get<int>("ultra_hdr_gain_map_scale", 1);
}

int AppConfig::DefaultBatchSize() const {
  return Get<int>("default_batch_size", 4);
}
int AppConfig::MaxBatchSize() const {
  return Get<int>("max_batch_size", 64);
}

int AppConfig::GpuThreadCount() const {
  return Get<int>("gpu_thread_count", 2);
}
int AppConfig::CpuThreadCount() const {
  return Get<int>("cpu_thread_count", static_cast<int>(std::thread::hardware_concurrency()));
}
int AppConfig::OpenCLWorkGroupSize() const {
  return Get<int>("opencl_work_group_size", 256);
}
int AppConfig::CUDABlockSize() const {
  return Get<int>("cuda_block_size", 256);
}

int AppConfig::ThumbnailCacheJpegQuality() const {
  return Get<int>("thumbnail_cache_jpeg_quality", 85);
}
int AppConfig::ThumbnailCacheMaxSizeMB() const {
  return Get<int>("thumbnail_cache_max_size_mb", 500);
}
uint32_t AppConfig::ThumbnailCacheSchemaVersion() const {
  return static_cast<uint32_t>(Get<int>("thumbnail_cache_schema_version", 1));
}

int AppConfig::RawProcessingTimeoutMs() const {
  return Get<int>("raw_processing_timeout_ms", 30000);
}
int AppConfig::GpuInitializationTimeoutMs() const {
  return Get<int>("gpu_init_timeout_ms", 15000);
}
int AppConfig::ExportTimeoutMs() const {
  return Get<int>("export_timeout_ms", 60000);
}

float AppConfig::MemorySafetyMarginFraction() const {
  return Get<float>("memory_safety_margin_fraction", 0.2f);
}
int AppConfig::MemoryBudgetRefreshIntervalMs() const {
  return Get<int>("memory_budget_refresh_ms", 5000);
}

int AppConfig::DemosaicNetBatchSize() const {
  return Get<int>("demosaicnet_batch_size", 2);
}
float AppConfig::DemosaicNetGammaEncode() const {
  return Get<float>("demosaicnet_gamma_encode", 1.0f / 2.2f);
}

std::string AppConfig::LogLevel() const {
  return Get<std::string>("log_level", "info");
}
int AppConfig::LogMaxFileSizeKB() const {
  return Get<int>("log_max_file_size_kb", 10240);
}
int AppConfig::LogMaxFileCount() const {
  return Get<int>("log_max_file_count", 5);
}
std::string AppConfig::LogFilePath() const {
  return Get<std::string>("log_file_path", "");
}

// ---- Setters ----

void AppConfig::SetFastPreviewMaxLongEdge(int value) {
  Set("fast_preview_max_long_edge", std::clamp(value, 512, 16384));
  NotifyChange("fast_preview_max_long_edge");
}
void AppConfig::SetQualityBasePreviewMaxLongEdge(int value) {
  Set("quality_base_preview_max_long_edge", std::clamp(value, 1024, 16384));
  NotifyChange("quality_base_preview_max_long_edge");
}
void AppConfig::SetHsReferenceMaskMaxLongEdge(int value) {
  Set("hs_reference_mask_max_long_edge", std::clamp(value, 512, 8192));
  NotifyChange("hs_reference_mask_max_long_edge");
}
void AppConfig::SetFullResPreviewMaxLongEdge(int value) {
  Set("full_res_preview_max_long_edge", std::clamp(value, 2048, 32768));
  NotifyChange("full_res_preview_max_long_edge");
}
void AppConfig::SetDefaultJpegQuality(int value) {
  Set("default_jpeg_quality", std::clamp(value, 1, 100));
  NotifyChange("default_jpeg_quality");
}
void AppConfig::SetDefaultBatchSize(int value) {
  Set("default_batch_size", std::clamp(value, 1, 64));
  NotifyChange("default_batch_size");
}
void AppConfig::SetLogLevel(const std::string& level) {
  Set("log_level", level);
  NotifyChange("log_level");
}
void AppConfig::SetMemorySafetyMarginFraction(float fraction) {
  Set("memory_safety_margin_fraction", std::clamp(fraction, 0.0f, 0.9f));
  NotifyChange("memory_safety_margin_fraction");
}

void AppConfig::SetConfigChangeCallback(ConfigChangeCallback callback) {
  std::lock_guard<std::mutex> lock(mutex_);
  change_callback_ = std::move(callback);
}

}  // namespace alcedo

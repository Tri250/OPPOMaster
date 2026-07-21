//  Copyright 2026 Yurun Zi
//  SPDX-License-Identifier: GPL-3.0-only
//  Additional permission under GPLv3 section 7 applies; see the LICENSE file.

#pragma once

#include <cstddef>
#include <cstdint>
#include <functional>
#include <optional>
#include <string>
#include <vector>

#include "image/gpu_backend.hpp"

namespace alcedo {

// Describes the memory budget and current usage for the rendering pipeline.
struct MemoryBudget {
  size_t total_vram_bytes = 0;        // Total GPU VRAM available
  size_t used_vram_bytes = 0;         // Currently used GPU VRAM
  size_t available_vram_bytes = 0;    // total_vram_bytes - used_vram_bytes
  size_t total_system_memory_bytes = 0;  // Total system RAM
  size_t available_system_memory_bytes = 0;  // Available system RAM
};

// Configuration for large image tile-based processing.
struct TileProcessingConfig {
  int     tile_size = 512;                    // Default tile size in pixels
  int     min_tile_size = 128;                // Minimum tile size (never go below this)
  int     max_tile_size = 2048;               // Maximum tile size (never go above this)
  int     large_image_threshold_pixels = 20000000;  // 20 MP — above this, tile-based processing
  size_t  large_image_threshold_bytes = 800000000;   // ~800 MB — VRAM budget threshold
  float   vram_usage_target = 0.7f;           // Target VRAM utilization (70% of available)
  float   vram_oom_safety_margin = 0.15f;     // Safety margin before OOM (15% of total)
  bool    enable_adaptive_quality = true;      // Reduce quality under memory pressure
  bool    enable_gpu_fallback_on_oom = true;   // Fall back to CPU if GPU OOM occurs
};

// Describes the quality level for adaptive rendering under memory pressure.
enum class AdaptiveQualityLevel {
  Full,           // No reduction — full resolution, full quality
  Reduced,        // Reduced resolution preview, full quality operators
  Minimal,        // Minimal resolution, simplified operators
  CPUFallback,    // GPU OOM — fall back to CPU pipeline
};

// Information about a tile in the tile grid.
struct TileInfo {
  int x = 0;               // X offset in pixels
  int y = 0;               // Y offset in pixels
  int width = 0;           // Tile width in pixels
  int height = 0;          // Tile height in pixels
  int tile_index = 0;      // Index in the tile grid
  int grid_col = 0;        // Column index in the grid
  int grid_row = 0;        // Row index in the grid
};

// Result of tile size calculation.
struct TileSizeResult {
  int                     recommended_tile_size = 512;
  int                     tiles_per_row = 0;
  int                     tiles_per_col = 0;
  int                     total_tiles = 0;
  size_t                  estimated_vram_per_tile = 0;  // bytes
  bool                    requires_tiling = false;       // image exceeds threshold
  AdaptiveQualityLevel    quality_level = AdaptiveQualityLevel::Full;
  std::string             reasoning;                     // Human-readable explanation
};

// Manages VRAM-aware tile size calculation, memory pressure monitoring,
// adaptive quality reduction, and GPU OOM graceful degradation for
// large image stability (60MP+ images requiring 8GB+ VRAM).
class LargeImageManager {
 public:
  // ---- VRAM Detection ----

  // Query available VRAM on the GPU. Returns nullopt if the info is not
  // available (e.g. no GPU, or platform not supported).
  static auto QueryVRAM(GpuBackendKind backend) -> std::optional<MemoryBudget>;

  // Query system memory information.
  static auto QuerySystemMemory() -> MemoryBudget;

  // ---- Tile Size Calculation ----

  // Calculate the optimal tile size for an image of the given dimensions,
  // considering VRAM availability and the configured thresholds.
  static auto CalculateTileSize(int image_width, int image_height, int channels = 4,
                                int bytes_per_channel = 4,
                                const TileProcessingConfig& config = TileProcessingConfig{})
      -> TileSizeResult;

  // Generate the list of tiles for a given image and tile configuration.
  static auto GenerateTileGrid(int image_width, int image_height, int tile_size)
      -> std::vector<TileInfo>;

  // ---- Memory Pressure Monitoring ----

  // Check current memory pressure and return the recommended adaptive quality level.
  // This should be called periodically (e.g. before each render) to detect
  // pressure changes.
  static auto CheckMemoryPressure(GpuBackendKind backend,
                                  const TileProcessingConfig& config = TileProcessingConfig{})
      -> AdaptiveQualityLevel;

  // Register a callback for memory pressure level changes.
  using MemoryPressureCallback =
      std::function<void(AdaptiveQualityLevel old_level, AdaptiveQualityLevel new_level)>;
  static void SetMemoryPressureCallback(MemoryPressureCallback callback);

  // ---- GPU OOM Handling ----

  // Check if a GPU OOM error has occurred (detects CUDA out-of-memory, etc.)
  // and returns true if fallback to CPU is needed.
  static auto DetectGPUOOM(GpuBackendKind backend) -> bool;

  // Attempt to recover from GPU OOM by releasing GPU resources and
  // optionally falling back to the CPU pipeline. Returns true if
  // recovery was successful.
  static auto RecoverFromGPUOOM(GpuBackendKind backend) -> bool;

  // ---- Progressive/Lazy Loading ----

  // Check if an image of the given dimensions should use progressive loading
  // (i.e. decode at a lower resolution first, then refine).
  static auto ShouldUseProgressiveLoading(int image_width, int image_height,
                                          const TileProcessingConfig& config = TileProcessingConfig{})
      -> bool;

  // Calculate the recommended initial decode resolution for progressive loading.
  // Returns the long-edge pixel count for the initial low-res decode.
  static auto GetProgressiveInitialDecodeRes(int image_width, int image_height,
                                             const TileProcessingConfig& config = TileProcessingConfig{})
      -> int;

  // ---- Configuration ----

  static auto GetConfig() -> const TileProcessingConfig&;
  static void SetConfig(const TileProcessingConfig& config);

  // Clear cached state.
  static void ClearCache();

  // Convert AdaptiveQualityLevel to string for logging.
  static auto AdaptiveQualityLevelToString(AdaptiveQualityLevel level) -> std::string;

 private:
  static TileProcessingConfig    config_;
  static MemoryBudget            cached_vram_budget_;
  static AdaptiveQualityLevel    current_quality_level_;
  static MemoryPressureCallback  memory_pressure_callback_;
};

}  // namespace alcedo

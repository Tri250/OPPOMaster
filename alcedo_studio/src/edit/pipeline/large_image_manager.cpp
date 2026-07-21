//  Copyright 2026 Yurun Zi
//  SPDX-License-Identifier: GPL-3.0-only
//  Additional permission under GPLv3 section 7 applies; see the LICENSE file.

#include "edit/pipeline/large_image_manager.hpp"

#if defined(_WIN32)
#ifndef NOMINMAX
#define NOMINMAX
#endif
#include <Windows.h>
#include <dxgi.h>
#include <dxgitype.h>
#pragma comment(lib, "dxgi.lib")
#elif defined(__APPLE__)
#include <mach/mach.h>
#include <sys/sysctl.h>
#else
#include <sys/sysinfo.h>
#endif

// CUDA VRAM query (if compiled with CUDA support)
#if defined(ALCEDO_HAS_CUDA)
#include <cuda_runtime.h>
#endif

#include <QtCore/qlogging.h>
#include <QDebug>

#include <algorithm>
#include <cmath>
#include <cstring>

namespace alcedo {

// Static member definitions
TileProcessingConfig             LargeImageManager::config_;
MemoryBudget                     LargeImageManager::cached_vram_budget_;
AdaptiveQualityLevel             LargeImageManager::current_quality_level_ =
    AdaptiveQualityLevel::Full;
LargeImageManager::MemoryPressureCallback LargeImageManager::memory_pressure_callback_;

// ============================================================================
// VRAM Detection
// ============================================================================

auto LargeImageManager::QueryVRAM(GpuBackendKind backend) -> std::optional<MemoryBudget> {
  MemoryBudget budget;

#if defined(ALCEDO_HAS_CUDA)
  if (backend == GpuBackendKind::CUDA) {
    int device_count = 0;
    cudaError_t err = cudaGetDeviceCount(&device_count);
    if (err != cudaSuccess || device_count == 0) {
      qWarning("LargeImageManager: CUDA device not available.");
      return std::nullopt;
    }

    cudaDeviceProp prop;
    err = cudaGetDeviceProperties(&prop, 0);
    if (err != cudaSuccess) {
      return std::nullopt;
    }

    size_t free_mem = 0, total_mem = 0;
    err = cudaMemGetInfo(&free_mem, &total_mem);
    if (err != cudaSuccess) {
      // Fall back to device properties
      budget.total_vram_bytes = static_cast<size_t>(prop.totalGlobalMem);
      budget.available_vram_bytes = budget.total_vram_bytes / 2;  // conservative estimate
      budget.used_vram_bytes = budget.total_vram_bytes - budget.available_vram_bytes;
    } else {
      budget.total_vram_bytes = total_mem;
      budget.available_vram_bytes = free_mem;
      budget.used_vram_bytes = total_mem - free_mem;
    }

    cached_vram_budget_ = budget;
    return budget;
  }
#endif

#if defined(_WIN32)
  // Use DXGI to query GPU memory on Windows (works without CUDA)
  if (backend == GpuBackendKind::None || backend == GpuBackendKind::OpenCL) {
    IDXGIFactory4* factory = nullptr;
    HRESULT hr = CreateDXGIFactory2(0, IID_PPV_ARGS(&factory));
    if (FAILED(hr) || !factory) {
      return std::nullopt;
    }

    IDXGIAdapter3* adapter3 = nullptr;
    IDXGIAdapter1* adapter1 = nullptr;

    for (UINT i = 0;
         factory->EnumAdapters1(i, &adapter1) != DXGI_ERROR_NOT_FOUND;
         ++i) {
      hr = adapter1->QueryInterface(IID_PPV_ARGS(&adapter3));
      adapter1->Release();
      if (SUCCEEDED(hr) && adapter3) {
        DXGI_QUERY_VIDEO_MEMORY_INFO info;
        hr = adapter3->QueryVideoMemoryInfo(0, DXGI_MEMORY_SEGMENT_GROUP_LOCAL, &info);
        if (SUCCEEDED(hr)) {
          budget.total_vram_bytes = info.Budget;
          budget.available_vram_bytes = info.Budget - info.CurrentUsage;
          budget.used_vram_bytes = info.CurrentUsage;

          adapter3->Release();
          factory->Release();

          cached_vram_budget_ = budget;
          return budget;
        }
        adapter3->Release();
      }
    }

    factory->Release();
    return std::nullopt;
  }
#endif

#if defined(__APPLE__)
  // On macOS with Metal, VRAM is shared memory. Query total system memory.
  if (backend == GpuBackendKind::Metal) {
    uint64_t mem_size = 0;
    size_t size = sizeof(mem_size);
    if (sysctlbyname("hw.memsize", &mem_size, &size, nullptr, 0) == 0) {
      // On Apple Silicon, GPU and CPU share memory.
      // Reserve some for system use.
      budget.total_vram_bytes = static_cast<size_t>(mem_size);
      budget.available_vram_bytes = budget.total_vram_bytes / 2;
      budget.used_vram_bytes = budget.total_vram_bytes - budget.available_vram_bytes;

      cached_vram_budget_ = budget;
      return budget;
    }
  }
#endif

  (void)backend;
  return std::nullopt;
}

auto LargeImageManager::QuerySystemMemory() -> MemoryBudget {
  MemoryBudget budget;

#if defined(_WIN32)
  MEMORYSTATUSEX status;
  status.dwLength = sizeof(status);
  if (GlobalMemoryStatusEx(&status)) {
    budget.total_system_memory_bytes = static_cast<size_t>(status.ullTotalPhys);
    budget.available_system_memory_bytes = static_cast<size_t>(status.ullAvailPhys);
  }
#elif defined(__APPLE__)
  uint64_t mem_size = 0;
  size_t size = sizeof(mem_size);
  if (sysctlbyname("hw.memsize", &mem_size, &size, nullptr, 0) == 0) {
    budget.total_system_memory_bytes = static_cast<size_t>(mem_size);
  }
  vm_statistics64_data_t vm_stat;
  mach_msg_type_number_t count = HOST_VM_INFO64_COUNT;
  if (host_statistics64(mach_host_self(), HOST_VM_INFO64,
                        reinterpret_cast<host_info64_t>(&vm_stat), &count) == KERN_SUCCESS) {
    const uint64_t page_size = static_cast<uint64_t>(vm_kernel_page_size);
    budget.available_system_memory_bytes =
        static_cast<size_t>(vm_stat.free_count + vm_stat.inactive_count) * page_size;
  }
#else
  struct sysinfo info;
  if (sysinfo(&info) == 0) {
    budget.total_system_memory_bytes =
        static_cast<size_t>(info.totalram) * static_cast<size_t>(info.mem_unit);
    budget.available_system_memory_bytes =
        static_cast<size_t>(info.freeram + info.bufferram) * static_cast<size_t>(info.mem_unit);
  }
#endif

  return budget;
}

// ============================================================================
// Tile Size Calculation
// ============================================================================

auto LargeImageManager::CalculateTileSize(int image_width, int image_height, int channels,
                                          int bytes_per_channel,
                                          const TileProcessingConfig& config) -> TileSizeResult {
  TileSizeResult result;

  if (image_width <= 0 || image_height <= 0 || channels <= 0 || bytes_per_channel <= 0) {
    result.reasoning = "Invalid image dimensions or channel parameters.";
    return result;
  }

  const size_t total_pixels = static_cast<size_t>(image_width) * static_cast<size_t>(image_height);
  const size_t bytes_per_pixel = static_cast<size_t>(channels) * static_cast<size_t>(bytes_per_channel);
  const size_t total_image_bytes = total_pixels * bytes_per_pixel;

  // Estimate VRAM needed for the full image:
  // - Source texture:  total_image_bytes
  // - Output texture:  total_image_bytes
  // - Intermediate scratch (merge, LUT, etc.): ~50% of total_image_bytes
  // Total estimate: ~2.5x the raw pixel data
  const size_t estimated_full_vram = static_cast<size_t>(total_image_bytes * 2.5);

  // Determine if tiling is required
  result.requires_tiling =
      total_pixels > static_cast<size_t>(config.large_image_threshold_pixels) ||
      estimated_full_vram > config.large_image_threshold_bytes;

  if (!result.requires_tiling) {
    // Small image — no tiling needed
    result.recommended_tile_size = std::max(image_width, image_height);
    result.tiles_per_row = 1;
    result.tiles_per_col = 1;
    result.total_tiles = 1;
    result.estimated_vram_per_tile = estimated_full_vram;
    result.quality_level = AdaptiveQualityLevel::Full;
    result.reasoning = "Image is within budget, no tiling required.";
    return result;
  }

  // Query available VRAM to inform tile size calculation
  auto vram = QueryVRAM(GpuBackendKind::CUDA);
  size_t available_vram = 0;

  if (vram.has_value()) {
    available_vram = vram->available_vram_bytes;
  } else {
    // No VRAM info available — use a conservative default
    // Assume 4 GB available (common minimum for modern GPUs)
    available_vram = 4000000000ULL;
  }

  // Apply the VRAM usage target — we don't want to use 100% of available VRAM
  const size_t target_vram_per_frame = static_cast<size_t>(
      static_cast<double>(available_vram) * static_cast<double>(config.vram_usage_target));

  // Calculate how many tiles we need to fit within the target VRAM budget
  // Each tile uses ~2.5x its raw pixel data in VRAM
  const size_t target_bytes_per_tile = static_cast<size_t>(
      static_cast<double>(target_vram_per_frame));

  // Work out the maximum tile dimension that fits within target_bytes_per_tile
  // tile_bytes = tile_pixels * bytes_per_pixel * 2.5
  // tile_pixels = target_bytes_per_tile / (bytes_per_pixel * 2.5)
  const double max_tile_pixels = static_cast<double>(target_bytes_per_tile) /
                                 (static_cast<double>(bytes_per_pixel) * 2.5);
  const int max_tile_dim = static_cast<int>(std::sqrt(max_tile_pixels));

  int tile_size = std::clamp(max_tile_dim, config.min_tile_size, config.max_tile_size);

  // Round down to a multiple of 64 for better GPU alignment
  tile_size = (tile_size / 64) * 64;
  tile_size = std::max(tile_size, config.min_tile_size);

  // Calculate the tile grid
  const int tiles_per_row = static_cast<int>(
      std::ceil(static_cast<double>(image_width) / tile_size));
  const int tiles_per_col = static_cast<int>(
      std::ceil(static_cast<double>(image_height) / tile_size));

  result.recommended_tile_size = tile_size;
  result.tiles_per_row = tiles_per_row;
  result.tiles_per_col = tiles_per_col;
  result.total_tiles = tiles_per_row * tiles_per_col;

  // Estimate VRAM per tile
  const size_t tile_pixels = static_cast<size_t>(tile_size) * static_cast<size_t>(tile_size);
  result.estimated_vram_per_tile = static_cast<size_t>(tile_pixels * bytes_per_pixel * 2.5);

  // Determine quality level based on available VRAM
  if (available_vram < config.large_image_threshold_bytes) {
    if (available_vram < config.large_image_threshold_bytes / 4) {
      result.quality_level = AdaptiveQualityLevel::CPUFallback;
      result.reasoning = "VRAM critically low (<25% of threshold), falling back to CPU.";
    } else {
      result.quality_level = AdaptiveQualityLevel::Minimal;
      result.reasoning = "VRAM is very limited, using minimal quality.";
    }
  } else if (result.estimated_vram_per_tile > available_vram * (1.0 - config.vram_oom_safety_margin)) {
    result.quality_level = AdaptiveQualityLevel::Reduced;
    result.reasoning = "Tile VRAM usage is close to limit, reducing quality.";
  } else {
    result.quality_level = AdaptiveQualityLevel::Full;
    result.reasoning = "VRAM budget is sufficient for full quality tiling.";
  }

  qInfo("LargeImageManager: %s (image=%dx%d, tile=%d, grid=%dx%d, vram_est=%zu MB, avail=%zu MB)",
        result.reasoning.c_str(), image_width, image_height, tile_size,
        tiles_per_row, tiles_per_col,
        result.estimated_vram_per_tile / (1024 * 1024),
        available_vram / (1024 * 1024));

  return result;
}

// ============================================================================
// Tile Grid Generation
// ============================================================================

auto LargeImageManager::GenerateTileGrid(int image_width, int image_height, int tile_size)
    -> std::vector<TileInfo> {
  std::vector<TileInfo> tiles;

  if (image_width <= 0 || image_height <= 0 || tile_size <= 0) {
    return tiles;
  }

  const int cols = static_cast<int>(std::ceil(static_cast<double>(image_width) / tile_size));
  const int rows = static_cast<int>(std::ceil(static_cast<double>(image_height) / tile_size));

  tiles.reserve(static_cast<size_t>(cols) * static_cast<size_t>(rows));

  int tile_index = 0;
  for (int row = 0; row < rows; ++row) {
    for (int col = 0; col < cols; ++col) {
      TileInfo tile;
      tile.x = col * tile_size;
      tile.y = row * tile_size;
      tile.width = std::min(tile_size, image_width - tile.x);
      tile.height = std::min(tile_size, image_height - tile.y);
      tile.tile_index = tile_index;
      tile.grid_col = col;
      tile.grid_row = row;
      tiles.push_back(tile);
      ++tile_index;
    }
  }

  return tiles;
}

// ============================================================================
// Memory Pressure Monitoring
// ============================================================================

auto LargeImageManager::CheckMemoryPressure(GpuBackendKind backend,
                                            const TileProcessingConfig& config)
    -> AdaptiveQualityLevel {
  auto vram = QueryVRAM(backend);
  AdaptiveQualityLevel new_level = AdaptiveQualityLevel::Full;

  if (vram.has_value() && vram->total_vram_bytes > 0) {
    const float usage_ratio = static_cast<float>(vram->used_vram_bytes) /
                              static_cast<float>(vram->total_vram_bytes);

    if (usage_ratio > (1.0f - config.vram_oom_safety_margin)) {
      // Using > 85% of VRAM — critical
      new_level = AdaptiveQualityLevel::CPUFallback;
    } else if (usage_ratio > (1.0f - config.vram_oom_safety_margin * 2)) {
      // Using > 70% of VRAM — reduce quality
      new_level = AdaptiveQualityLevel::Reduced;
    } else if (usage_ratio > config.vram_usage_target) {
      // Above target — minimal reduction
      new_level = AdaptiveQualityLevel::Minimal;
    }
  }

  // Notify if level changed
  if (new_level != current_quality_level_) {
    qInfo("LargeImageManager: quality level changed: %s -> %s",
          AdaptiveQualityLevelToString(current_quality_level_).c_str(),
          AdaptiveQualityLevelToString(new_level).c_str());

    if (memory_pressure_callback_) {
      memory_pressure_callback_(current_quality_level_, new_level);
    }
    current_quality_level_ = new_level;
  }

  return new_level;
}

void LargeImageManager::SetMemoryPressureCallback(MemoryPressureCallback callback) {
  memory_pressure_callback_ = std::move(callback);
}

// ============================================================================
// GPU OOM Handling
// ============================================================================

auto LargeImageManager::DetectGPUOOM(GpuBackendKind backend) -> bool {
#if defined(ALCEDO_HAS_CUDA)
  if (backend == GpuBackendKind::CUDA) {
    cudaError_t err = cudaGetLastError();
    if (err == cudaErrorMemoryAllocation || err == cudaErrorOutOfMemory) {
      qWarning("LargeImageManager: CUDA out-of-memory detected (error=%d).",
               static_cast<int>(err));
      // Clear the error so subsequent CUDA calls can succeed
      cudaGetLastError();
      return true;
    }
  }
#else
  (void)backend;
#endif

  // Also check via memory pressure
  auto vram = QueryVRAM(backend);
  if (vram.has_value() && vram->total_vram_bytes > 0) {
    const float usage = static_cast<float>(vram->used_vram_bytes) /
                        static_cast<float>(vram->total_vram_bytes);
    if (usage > 0.95f) {
      qWarning("LargeImageManager: VRAM usage at %.0f%%, likely OOM.", usage * 100.0f);
      return true;
    }
  }

  return false;
}

auto LargeImageManager::RecoverFromGPUOOM(GpuBackendKind backend) -> bool {
  qInfo("LargeImageManager: attempting GPU OOM recovery...");

#if defined(ALCEDO_HAS_CUDA)
  if (backend == GpuBackendKind::CUDA) {
    // Reset the CUDA device to free all allocated memory
    cudaError_t err = cudaDeviceReset();
    if (err != cudaSuccess) {
      qWarning("LargeImageManager: cudaDeviceReset failed (error=%d).",
               static_cast<int>(err));
      return false;
    }
    qInfo("LargeImageManager: CUDA device reset successful.");
  }
#else
  (void)backend;
#endif

  // Update the quality level to CPU fallback
  if (current_quality_level_ != AdaptiveQualityLevel::CPUFallback) {
    auto old_level = current_quality_level_;
    current_quality_level_ = AdaptiveQualityLevel::CPUFallback;
    if (memory_pressure_callback_) {
      memory_pressure_callback_(old_level, current_quality_level_);
    }
  }

  return true;
}

// ============================================================================
// Progressive/Lazy Loading
// ============================================================================

auto LargeImageManager::ShouldUseProgressiveLoading(int image_width, int image_height,
                                                    const TileProcessingConfig& config) -> bool {
  const size_t total_pixels = static_cast<size_t>(image_width) * static_cast<size_t>(image_height);

  // Use progressive loading for images larger than the threshold
  if (total_pixels > static_cast<size_t>(config.large_image_threshold_pixels)) {
    return true;
  }

  // Also check VRAM — if we can't fit even a half-res preview, use progressive
  auto vram = QueryVRAM(GpuBackendKind::CUDA);
  if (vram.has_value()) {
    const size_t half_res_bytes = (total_pixels / 4) * 4 * 4 * 2;  // RGBA32F, 2.5x overhead
    if (half_res_bytes > vram->available_vram_bytes) {
      return true;
    }
  }

  return false;
}

auto LargeImageManager::GetProgressiveInitialDecodeRes(int image_width, int image_height,
                                                       const TileProcessingConfig& config)
    -> int {
  const size_t total_pixels = static_cast<size_t>(image_width) * static_cast<size_t>(image_height);
  const auto long_edge = static_cast<int>(std::max(image_width, image_height));

  if (total_pixels <= static_cast<size_t>(config.large_image_threshold_pixels)) {
    return long_edge;  // No progressive loading needed
  }

  // Start with 1/4 resolution for the initial decode (1/16 of total pixels)
  int initial_res = long_edge / 4;

  // Ensure we have a reasonable minimum for the initial preview
  initial_res = std::max(initial_res, 1024);

  // Check if even the 1/4 resolution fits in VRAM
  auto vram = QueryVRAM(GpuBackendKind::CUDA);
  if (vram.has_value()) {
    const size_t initial_pixels = static_cast<size_t>(initial_res) * static_cast<size_t>(initial_res);
    const size_t initial_bytes = initial_pixels * 4 * 4 * 2;  // RGBA32F, 2x overhead

    if (initial_bytes > vram->available_vram_bytes) {
      // Drop to 1/8 resolution
      initial_res = long_edge / 8;
      initial_res = std::max(initial_res, 512);
    }
  }

  qInfo("LargeImageManager: progressive loading initial decode res = %d (from %d long edge).",
        initial_res, long_edge);

  return initial_res;
}

// ============================================================================
// Configuration
// ============================================================================

auto LargeImageManager::GetConfig() -> const TileProcessingConfig& {
  return config_;
}

void LargeImageManager::SetConfig(const TileProcessingConfig& config) {
  config_ = config;
  qInfo("LargeImageManager: config updated (tile_size=%d, threshold=%d MP, vram_target=%.0f%%).",
        config.tile_size, config.large_image_threshold_pixels / 1000000,
        config.vram_usage_target * 100.0f);
}

void LargeImageManager::ClearCache() {
  cached_vram_budget_ = {};
  current_quality_level_ = AdaptiveQualityLevel::Full;
  memory_pressure_callback_ = nullptr;
}

auto LargeImageManager::AdaptiveQualityLevelToString(AdaptiveQualityLevel level) -> std::string {
  switch (level) {
    case AdaptiveQualityLevel::Full:       return "Full";
    case AdaptiveQualityLevel::Reduced:    return "Reduced";
    case AdaptiveQualityLevel::Minimal:    return "Minimal";
    case AdaptiveQualityLevel::CPUFallback: return "CPUFallback";
  }
  return "Unknown";
}

}  // namespace alcedo

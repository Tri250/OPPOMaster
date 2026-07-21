//  Copyright 2026 Yurun Zi
//  SPDX-License-Identifier: GPL-3.0-only
//  Additional permission under GPLv3 section 7 applies; see the LICENSE file.

#include "renderer/memory_budget_manager.hpp"

#include <algorithm>
#include <cstddef>
#include <cstdint>
#include <cstdio>
#include <cstdlib>
#include <cstring>
#include <fstream>
#include <limits>
#include <mutex>
#include <sstream>
#include <string>
#include <utility>

#ifdef _WIN32
#include <windows.h>
#else
#include <sys/sysinfo.h>
#include <unistd.h>
#endif

#ifdef HAVE_CUDA
#include <cuda_runtime.h>
#endif

#include "utils/diagnostics/app_logging.hpp"

namespace alcedo {

// ---- Singleton ----

auto MemoryBudgetManager::Instance() -> MemoryBudgetManager& {
  static MemoryBudgetManager instance;
  return instance;
}

MemoryBudgetManager::MemoryBudgetManager() {
  RefreshMemoryStatus();
}

// ---- System memory queries ----

auto MemoryBudgetManager::QuerySystemMemory() const -> size_t {
#ifdef _WIN32
  MEMORYSTATUSEX status;
  status.dwLength = sizeof(status);
  if (GlobalMemoryStatusEx(&status)) {
    return static_cast<size_t>(status.ullAvailPhys);
  }
  return 0;
#else
  struct sysinfo info;
  if (sysinfo(&info) == 0) {
    return static_cast<size_t>(info.freeram) * static_cast<size_t>(info.mem_unit);
  }
  return 0;
#endif
}

auto MemoryBudgetManager::QueryGpuMemory() const -> size_t {
#ifdef HAVE_CUDA
  cudaDeviceProp prop{};
  if (cudaGetDeviceProperties(&prop, 0) != cudaSuccess) {
    return 0;
  }
  size_t free_memory = 0;
  size_t total_memory = 0;
  if (cudaMemGetInfo(&free_memory, &total_memory) == cudaSuccess) {
    return free_memory;
  }
#endif
  return 0;
}

auto MemoryBudgetManager::IsUnifiedMemoryArchitecture() const -> bool {
  if (unified_memory_cached_) {
    return unified_memory_result_;
  }

  bool is_unified = false;

#ifdef HAVE_CUDA
  cudaDeviceProp prop{};
  if (cudaGetDeviceProperties(&prop, 0) == cudaSuccess) {
    // NVIDIA integrated GPUs (Tegra) and devices where host and device memory
    // are shared report non-zero integrated field.
    is_unified = (prop.integrated != 0);
  }
#endif

#ifdef __APPLE__
  // Apple Silicon always uses unified memory.
  is_unified = true;
#endif

  // Check for integrated GPU indicators on Linux.
#ifndef _WIN32
#ifndef __APPLE__
#ifndef HAVE_CUDA
  // Read from /proc/meminfo as a fallback for integrated GPUs on Linux.
  // If there's no discrete GPU memory info available, assume unified.
  std::ifstream meminfo("/proc/meminfo");
  if (meminfo.is_open()) {
    // If we can't detect any discrete GPU memory, assume unified.
    is_unified = true;  // Conservative: assume unified if no GPU detected
  }
#endif
#endif
#endif

  unified_memory_result_ = is_unified;
  unified_memory_cached_ = true;
  return is_unified;
}

// ---- Core operations ----

void MemoryBudgetManager::RefreshMemoryStatus() {
  std::lock_guard<std::mutex> lock(mutex_);

  const size_t sys_avail = QuerySystemMemory();
  const size_t gpu_avail = QueryGpuMemory();

#ifdef _WIN32
  MEMORYSTATUSEX status;
  status.dwLength = sizeof(status);
  if (GlobalMemoryStatusEx(&status)) {
    status_.system_total_bytes_ = static_cast<size_t>(status.ullTotalPhys);
  }
#else
  struct sysinfo info;
  if (sysinfo(&info) == 0) {
    status_.system_total_bytes_ = static_cast<size_t>(info.totalram) *
                                  static_cast<size_t>(info.mem_unit);
  }
#endif

#ifdef HAVE_CUDA
  {
    size_t free_mem = 0;
    size_t total_mem = 0;
    if (cudaMemGetInfo(&free_mem, &total_mem) == cudaSuccess) {
      status_.gpu_total_bytes_ = total_mem;
    }
  }
#endif

  status_.system_available_bytes_ = sys_avail;
  status_.gpu_available_bytes_    = gpu_avail;
  status_.unified_memory_         = IsUnifiedMemoryArchitecture();

  if (status_.system_total_bytes_ > 0) {
    status_.system_usage_fraction_ = 1.0f - static_cast<float>(
        static_cast<double>(sys_avail) / static_cast<double>(status_.system_total_bytes_));
  }
  if (status_.gpu_total_bytes_ > 0) {
    status_.gpu_usage_fraction_ = 1.0f - static_cast<float>(
        static_cast<double>(gpu_avail) / static_cast<double>(status_.gpu_total_bytes_));
  }

  // Notify callback if registered.
  if (status_callback_) {
    status_callback_(status_);
  }
}

auto MemoryBudgetManager::GetMemoryStatus() const -> MemoryStatus {
  std::lock_guard<std::mutex> lock(mutex_);
  return status_;
}

auto MemoryBudgetManager::RequestBudget(const std::string& operation_id,
                                         size_t requested_bytes) -> MemoryBudget {
  std::lock_guard<std::mutex> lock(mutex_);

  // Calculate available memory considering the safety margin.
  const size_t available = status_.unified_memory_
      ? status_.system_available_bytes_
      : (status_.system_available_bytes_ + status_.gpu_available_bytes_) / 2;

  const size_t safe_available = static_cast<size_t>(
      static_cast<double>(available) * (1.0 - static_cast<double>(safety_margin_fraction_)));

  // Subtract already-budgeted amounts.
  const size_t already_budgeted = TotalBudgetedBytesLocked();
  const size_t remaining = (safe_available > already_budgeted)
      ? safe_available - already_budgeted
      : 0;

  MemoryBudget budget;
  budget.budget_bytes_ = std::min(requested_bytes, remaining);
  budget.allocated_bytes_ = 0;
  budget.peak_bytes_ = 0;

  budgets_[operation_id] = budget;
  APP_LOG_DEBUG_DEFAULT("MemoryBudgetManager: Budget allocated for '%s': %zu / %zu bytes "
                "(remaining: %zu)", operation_id.c_str(), budget.budget_bytes_, requested_bytes, remaining);
  return budget;
}

void MemoryBudgetManager::ReleaseBudget(const std::string& operation_id) {
  std::lock_guard<std::mutex> lock(mutex_);
  auto it = budgets_.find(operation_id);
  if (it != budgets_.end()) {
    APP_LOG_DEBUG_DEFAULT("MemoryBudgetManager: Budget released for '%s' (peak: %zu bytes)",
                  operation_id.c_str(), it->second.peak_bytes_);
    budgets_.erase(it);
  }
}

void MemoryBudgetManager::UpdateAllocation(const std::string& operation_id,
                                            size_t actual_bytes) {
  std::lock_guard<std::mutex> lock(mutex_);
  auto it = budgets_.find(operation_id);
  if (it != budgets_.end()) {
    it->second.allocated_bytes_ = actual_bytes;
    if (actual_bytes > it->second.peak_bytes_) {
      it->second.peak_bytes_ = actual_bytes;
    }
  }
}

auto MemoryBudgetManager::CanAllocate(size_t bytes) const -> bool {
  std::lock_guard<std::mutex> lock(mutex_);

  const size_t available = status_.unified_memory_
      ? status_.system_available_bytes_
      : std::min(status_.system_available_bytes_, status_.gpu_available_bytes_);

  const size_t safe_available = static_cast<size_t>(
      static_cast<double>(available) * (1.0 - static_cast<double>(safety_margin_fraction_)));

  const size_t already_budgeted = TotalBudgetedBytesLocked();
  return (bytes + already_budgeted) <= safe_available;
}

auto MemoryBudgetManager::RecommendedBatchSize(size_t bytes_per_image, size_t min_batch,
                                                size_t max_batch) const -> size_t {
  std::lock_guard<std::mutex> lock(mutex_);

  const size_t available = status_.unified_memory_
      ? status_.system_available_bytes_
      : std::min(status_.system_available_bytes_, status_.gpu_available_bytes_);

  const size_t safe_available = static_cast<size_t>(
      static_cast<double>(available) * (1.0 - static_cast<double>(safety_margin_fraction_)));

  const size_t already_budgeted = TotalBudgetedBytesLocked();
  const size_t remaining = (safe_available > already_budgeted)
      ? safe_available - already_budgeted
      : 0;

  if (bytes_per_image == 0) {
    return min_batch;
  }

  const size_t batch = remaining / bytes_per_image;
  return std::clamp(batch, min_batch, max_batch);
}

void MemoryBudgetManager::SetMemoryStatusCallback(MemoryStatusCallback callback) {
  std::lock_guard<std::mutex> lock(mutex_);
  status_callback_ = std::move(callback);
}

void MemoryBudgetManager::SetSafetyMarginFraction(float fraction) {
  std::lock_guard<std::mutex> lock(mutex_);
  safety_margin_fraction_ = std::clamp(fraction, 0.0f, 0.9f);
}

auto MemoryBudgetManager::TotalBudgetedBytes() const -> size_t {
  std::lock_guard<std::mutex> lock(mutex_);
  return TotalBudgetedBytesLocked();
}

auto MemoryBudgetManager::TotalAllocatedBytes() const -> size_t {
  std::lock_guard<std::mutex> lock(mutex_);
  size_t total = 0;
  for (const auto& [id, budget] : budgets_) {
    total += budget.allocated_bytes_;
  }
  return total;
}

// Private: must be called with mutex_ held.
auto MemoryBudgetManager::TotalBudgetedBytesLocked() const -> size_t {
  size_t total = 0;
  for (const auto& [id, budget] : budgets_) {
    total += budget.budget_bytes_;
  }
  return total;
}

}  // namespace alcedo

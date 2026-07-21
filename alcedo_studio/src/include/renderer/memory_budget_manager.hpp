//  Copyright 2026 Yurun Zi
//  SPDX-License-Identifier: GPL-3.0-only
//  Additional permission under GPLv3 section 7 applies; see the LICENSE file.

#pragma once

#include <atomic>
#include <cstddef>
#include <cstdint>
#include <functional>
#include <mutex>
#include <string>
#include <unordered_map>

namespace alcedo {

// Memory budget information for a single operation.
struct MemoryBudget {
  size_t allocated_bytes_   = 0;  // Bytes currently allocated.
  size_t budget_bytes_      = 0;  // Maximum bytes this operation may use.
  size_t peak_bytes_        = 0;  // Peak allocation observed.
};

// System and GPU memory status.
struct MemoryStatus {
  size_t system_total_bytes_      = 0;
  size_t system_available_bytes_  = 0;
  size_t gpu_total_bytes_         = 0;
  size_t gpu_available_bytes_     = 0;
  float  system_usage_fraction_   = 0.0f;  // 0.0 - 1.0
  float  gpu_usage_fraction_      = 0.0f;  // 0.0 - 1.0
  bool   unified_memory_          = false;  // Apple Silicon / integrated GPU.
};

// Manages memory budgets for batch rendering operations. Monitors available
// system and GPU memory, allocates processing budgets per operation, and
// implements back-pressure when memory is low.
//
// Supports both discrete GPU architectures (NVIDIA/AMD) and unified memory
// architectures (Apple Silicon) where system and GPU memory are shared.
class MemoryBudgetManager {
 public:
  // Callback type for memory status updates.
  using MemoryStatusCallback = std::function<void(const MemoryStatus&)>;

  // Get the singleton instance.
  static auto Instance() -> MemoryBudgetManager&;

  // Refresh memory status from the system. Call periodically or before
  // allocating large buffers.
  void RefreshMemoryStatus();

  // Get the current memory status (may be stale until RefreshMemoryStatus).
  auto GetMemoryStatus() const -> MemoryStatus;

  // Request a memory budget for an operation. Returns the allocated budget
  // which may be less than requested if system memory is constrained.
  // The operation_id should be a unique identifier for the operation.
  auto RequestBudget(const std::string& operation_id, size_t requested_bytes) -> MemoryBudget;

  // Release a previously acquired budget.
  void ReleaseBudget(const std::string& operation_id);

  // Update the allocated amount for an operation (e.g., after actual allocation).
  void UpdateAllocation(const std::string& operation_id, size_t actual_bytes);

  // Check if a given allocation request would exceed the available budget.
  // Returns true if the allocation is safe, false if it would cause memory
  // pressure (back-pressure signal).
  auto CanAllocate(size_t bytes) const -> bool;

  // Get the recommended batch size for operations that process images in
  // batches. Considers available memory and the per-image memory requirement.
  auto RecommendedBatchSize(size_t bytes_per_image, size_t min_batch = 1,
                            size_t max_batch = 64) const -> size_t;

  // Set a callback to be invoked when memory status changes significantly
  // (e.g., crossing a pressure threshold).
  void SetMemoryStatusCallback(MemoryStatusCallback callback);

  // Set the safety margin fraction (0.0 - 1.0). The manager will not
  // allocate more than (available - safety_margin * available) bytes.
  // Default: 0.2 (keep 20% of memory free).
  void SetSafetyMarginFraction(float fraction);

  // Get total bytes currently budgeted across all operations.
  auto TotalBudgetedBytes() const -> size_t;

  // Get total bytes currently allocated across all operations.
  auto TotalAllocatedBytes() const -> size_t;

 private:
  MemoryBudgetManager();

  auto QuerySystemMemory() const -> size_t;
  auto QueryGpuMemory() const -> size_t;
  auto IsUnifiedMemoryArchitecture() const -> bool;
  auto TotalBudgetedBytesLocked() const -> size_t;

  mutable std::mutex mutex_;
  MemoryStatus       status_;
  std::unordered_map<std::string, MemoryBudget> budgets_;
  float              safety_margin_fraction_ = 0.2f;
  MemoryStatusCallback status_callback_;

  // Cached values for unified memory architecture detection.
  bool               unified_memory_cached_ = false;
  bool               unified_memory_result_ = false;
};

}  // namespace alcedo

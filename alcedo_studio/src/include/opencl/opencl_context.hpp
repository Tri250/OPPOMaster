//  Copyright 2026 Yurun Zi
//  SPDX-License-Identifier: GPL-3.0-only
//  Additional permission under GPLv3 section 7 applies; see the LICENSE file.

#pragma once

#ifdef HAVE_OPENCL

#include <array>
#include <mutex>
#include <optional>
#include <string>
#include <string_view>

#ifndef CL_TARGET_OPENCL_VERSION
#define CL_TARGET_OPENCL_VERSION 120
#endif
#include <CL/cl.h>

namespace alcedo {

struct OpenClDeviceCapabilities {
  std::string           name;
  std::string           vendor;
  std::string           driver_version;
  std::string           device_version;
  std::string           opencl_c_version;
  cl_device_type        device_type                 = 0;
  cl_uint               compute_units               = 0;
  cl_uint               max_clock_frequency_mhz     = 0;
  cl_ulong              global_memory_bytes         = 0;
  cl_ulong              max_single_allocation_bytes = 0;
  cl_ulong              local_memory_bytes          = 0;
  size_t                max_work_group_size         = 0;
  cl_uint               max_work_item_dimensions    = 0;
  std::array<size_t, 3> max_work_item_sizes         = {};
  bool                  image_support               = false;
  bool                  available                   = false;
  bool                  compiler_available          = false;
};

struct OpenClInitializationOptions {
  // Case-insensitive substring matched against "<vendor> <device name>".
  // Useful for user preferences such as "nvidia" or "intel arc". If omitted,
  // Initialize() also checks ALCEDO_OPENCL_DEVICE as a lightweight user override.
  std::optional<std::string> preferred_device;

  // Optional ID3D11Device* on Windows. When set, OpenCL initialization selects a
  // device that supports cl_khr_d3d11_sharing with that D3D11 device and creates
  // the context with the matching D3D11 sharing properties.
  void*                      d3d11_device = nullptr;
};

class OpenClContext {
 private:
  cl_platform_id           platform_ = nullptr;
  cl_device_id             device_   = nullptr;
  cl_context               context_  = nullptr;
  cl_command_queue         queue_    = nullptr;
  OpenClDeviceCapabilities capabilities_;
  bool                     initialized_              = false;
  bool                     initialization_attempted_ = false;
  bool                     d3d11_sharing_enabled_    = false;
  std::string              last_initialization_error_;
  mutable std::mutex       mutex_;

  OpenClContext() = default;

  ~OpenClContext();

 public:
  OpenClContext(const OpenClContext&)                      = delete;
  auto operator=(const OpenClContext&) -> OpenClContext&   = delete;
  OpenClContext(OpenClContext&&)                           = delete;
  auto        operator=(OpenClContext&&) -> OpenClContext& = delete;

  static auto Instance() -> OpenClContext&;

  // Selects the best usable GPU by default. If options.preferred_device is set,
  // it takes precedence when a matching usable device exists.
  void        Initialize(const OpenClInitializationOptions& options = {});

  // Non-throwing path for optional fallback logic. If OpenCL cannot be
  // initialized, callers can continue with the CPU path instead.
  auto        TryInitialize(const OpenClInitializationOptions& options = {}) -> bool;

  auto        IsInitialized() const -> bool;
  auto        InitializationAttempted() const -> bool;
  auto        LastInitializationError() const -> std::string;

  auto        Platform() const -> cl_platform_id;
  auto        Device() const -> cl_device_id;
  auto        Context() const -> cl_context;
  auto        Queue() const -> cl_command_queue;
  auto        D3D11SharingEnabled() const -> bool;
  auto        Capabilities() const -> const OpenClDeviceCapabilities&;
};

}  // namespace alcedo

#endif

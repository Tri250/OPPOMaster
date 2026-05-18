//  Copyright 2026 Yurun Zi
//  SPDX-License-Identifier: GPL-3.0-only
//  Additional permission under GPLv3 section 7 applies; see the LICENSE file.

#ifdef HAVE_OPENCL

#define CL_USE_DEPRECATED_OPENCL_1_2_APIS
#include "opencl/opencl_context.hpp"

#include <algorithm>
#include <cctype>
#include <cstdlib>
#include <sstream>
#include <stdexcept>
#include <utility>
#include <vector>

namespace alcedo {
namespace {

struct OpenClDeviceCandidate {
  cl_platform_id           platform = nullptr;
  cl_device_id             device   = nullptr;
  OpenClDeviceCapabilities capabilities;
};

auto ToLower(std::string value) -> std::string {
  std::transform(value.begin(), value.end(), value.begin(),
                 [](unsigned char ch) { return static_cast<char>(std::tolower(ch)); });
  return value;
}

auto DeviceTypeRank(cl_device_type type) -> int {
  if ((type & CL_DEVICE_TYPE_GPU) != 0) {
    return 3;
  }
  if ((type & CL_DEVICE_TYPE_ACCELERATOR) != 0) {
    return 2;
  }
  if ((type & CL_DEVICE_TYPE_CPU) != 0) {
    return 1;
  }
  return 0;
}

template <typename T>
auto GetDeviceInfoValue(cl_device_id device, cl_device_info field) -> T {
  T      value = {};
  cl_int error = clGetDeviceInfo(device, field, sizeof(T), &value, nullptr);
  if (error != CL_SUCCESS) {
    throw std::runtime_error("[FATAL] OpenClContext: clGetDeviceInfo failed.");
  }
  return value;
}

auto GetDeviceInfoString(cl_device_id device, cl_device_info field) -> std::string {
  size_t size  = 0;
  cl_int error = clGetDeviceInfo(device, field, 0, nullptr, &size);
  if (error != CL_SUCCESS) {
    throw std::runtime_error("[FATAL] OpenClContext: failed to query string size.");
  }

  std::string value(size, '\0');
  error = clGetDeviceInfo(device, field, size, value.data(), nullptr);
  if (error != CL_SUCCESS) {
    throw std::runtime_error("[FATAL] OpenClContext: failed to query string value.");
  }
  if (!value.empty() && value.back() == '\0') {
    value.pop_back();
  }
  return value;
}

auto GetMaxWorkItemSizes(cl_device_id device, cl_uint dimensions) -> std::array<size_t, 3> {
  std::array<size_t, 3> result = {};
  const auto            clamped_dimensions =
      static_cast<size_t>(std::min<cl_uint>(dimensions, static_cast<cl_uint>(result.size())));
  if (clamped_dimensions == 0) {
    return result;
  }

  std::vector<size_t> queried_sizes(static_cast<size_t>(dimensions), 0);
  cl_int              error =
      clGetDeviceInfo(device, CL_DEVICE_MAX_WORK_ITEM_SIZES, queried_sizes.size() * sizeof(size_t),
                      queried_sizes.data(), nullptr);
  if (error != CL_SUCCESS) {
    throw std::runtime_error("[FATAL] OpenClContext: failed to query work-item sizes.");
  }

  std::copy_n(queried_sizes.begin(), clamped_dimensions, result.begin());
  return result;
}

auto QueryCapabilities(cl_device_id device) -> OpenClDeviceCapabilities {
  OpenClDeviceCapabilities capabilities;
  capabilities.name             = GetDeviceInfoString(device, CL_DEVICE_NAME);
  capabilities.vendor           = GetDeviceInfoString(device, CL_DEVICE_VENDOR);
  capabilities.driver_version   = GetDeviceInfoString(device, CL_DRIVER_VERSION);
  capabilities.device_version   = GetDeviceInfoString(device, CL_DEVICE_VERSION);
  capabilities.opencl_c_version = GetDeviceInfoString(device, CL_DEVICE_OPENCL_C_VERSION);
  capabilities.device_type      = GetDeviceInfoValue<cl_device_type>(device, CL_DEVICE_TYPE);
  capabilities.compute_units    = GetDeviceInfoValue<cl_uint>(device, CL_DEVICE_MAX_COMPUTE_UNITS);
  capabilities.max_clock_frequency_mhz =
      GetDeviceInfoValue<cl_uint>(device, CL_DEVICE_MAX_CLOCK_FREQUENCY);
  capabilities.global_memory_bytes =
      GetDeviceInfoValue<cl_ulong>(device, CL_DEVICE_GLOBAL_MEM_SIZE);
  capabilities.max_single_allocation_bytes =
      GetDeviceInfoValue<cl_ulong>(device, CL_DEVICE_MAX_MEM_ALLOC_SIZE);
  capabilities.local_memory_bytes = GetDeviceInfoValue<cl_ulong>(device, CL_DEVICE_LOCAL_MEM_SIZE);
  capabilities.max_work_group_size =
      GetDeviceInfoValue<size_t>(device, CL_DEVICE_MAX_WORK_GROUP_SIZE);
  capabilities.max_work_item_dimensions =
      GetDeviceInfoValue<cl_uint>(device, CL_DEVICE_MAX_WORK_ITEM_DIMENSIONS);
  capabilities.max_work_item_sizes =
      GetMaxWorkItemSizes(device, capabilities.max_work_item_dimensions);
  capabilities.image_support =
      GetDeviceInfoValue<cl_bool>(device, CL_DEVICE_IMAGE_SUPPORT) == CL_TRUE;
  capabilities.available = GetDeviceInfoValue<cl_bool>(device, CL_DEVICE_AVAILABLE) == CL_TRUE;
  capabilities.compiler_available =
      GetDeviceInfoValue<cl_bool>(device, CL_DEVICE_COMPILER_AVAILABLE) == CL_TRUE;
  return capabilities;
}

auto IsUsable(const OpenClDeviceCandidate& candidate) -> bool {
  return candidate.capabilities.available && candidate.capabilities.compiler_available;
}

auto IsGpu(const OpenClDeviceCandidate& candidate) -> bool {
  return (candidate.capabilities.device_type & CL_DEVICE_TYPE_GPU) != 0;
}

auto IsPreferredDevice(const OpenClDeviceCandidate& candidate, std::string_view preferred_device)
    -> bool {
  const auto haystack = ToLower(candidate.capabilities.vendor + " " + candidate.capabilities.name);
  const auto needle   = ToLower(std::string(preferred_device));
  return !needle.empty() && haystack.find(needle) != std::string::npos;
}

auto PreferCandidate(const OpenClDeviceCandidate& lhs, const OpenClDeviceCandidate& rhs) -> bool {
  const auto lhs_rank = DeviceTypeRank(lhs.capabilities.device_type);
  const auto rhs_rank = DeviceTypeRank(rhs.capabilities.device_type);
  if (lhs_rank != rhs_rank) {
    return lhs_rank > rhs_rank;
  }
  if (lhs.capabilities.global_memory_bytes != rhs.capabilities.global_memory_bytes) {
    return lhs.capabilities.global_memory_bytes > rhs.capabilities.global_memory_bytes;
  }
  return lhs.capabilities.compute_units > rhs.capabilities.compute_units;
}

auto EnumerateCandidates() -> std::vector<OpenClDeviceCandidate> {
  cl_uint platform_count = 0;
  cl_int  error          = clGetPlatformIDs(0, nullptr, &platform_count);
  if (error != CL_SUCCESS || platform_count == 0) {
    throw std::runtime_error("[FATAL] OpenClContext: no OpenCL platform is available.");
  }

  std::vector<cl_platform_id> platforms(platform_count, nullptr);
  error = clGetPlatformIDs(platform_count, platforms.data(), nullptr);
  if (error != CL_SUCCESS) {
    throw std::runtime_error("[FATAL] OpenClContext: failed to enumerate OpenCL platforms.");
  }

  std::vector<OpenClDeviceCandidate> candidates;
  for (const auto platform : platforms) {
    cl_uint device_count = 0;
    error                = clGetDeviceIDs(platform, CL_DEVICE_TYPE_ALL, 0, nullptr, &device_count);
    if (error == CL_DEVICE_NOT_FOUND || device_count == 0) {
      continue;
    }
    if (error != CL_SUCCESS) {
      throw std::runtime_error("[FATAL] OpenClContext: failed to enumerate platform devices.");
    }

    std::vector<cl_device_id> devices(device_count, nullptr);
    error = clGetDeviceIDs(platform, CL_DEVICE_TYPE_ALL, device_count, devices.data(), nullptr);
    if (error != CL_SUCCESS) {
      throw std::runtime_error("[FATAL] OpenClContext: failed to read platform devices.");
    }

    for (const auto device : devices) {
      OpenClDeviceCandidate candidate;
      candidate.platform     = platform;
      candidate.device       = device;
      candidate.capabilities = QueryCapabilities(device);
      candidates.push_back(std::move(candidate));
    }
  }

  if (candidates.empty()) {
    throw std::runtime_error("[FATAL] OpenClContext: no OpenCL device was found.");
  }
  return candidates;
}

auto DescribeCandidates(const std::vector<OpenClDeviceCandidate>& candidates) -> std::string {
  std::ostringstream stream;
  for (const auto& candidate : candidates) {
    stream << "\n  - " << candidate.capabilities.vendor << " " << candidate.capabilities.name;
    if (!IsUsable(candidate)) {
      stream << " (unusable)";
    }
  }
  return stream.str();
}

auto ResolvePreferredDevice(const OpenClInitializationOptions& options)
    -> std::optional<std::string> {
  if (options.preferred_device.has_value() && !options.preferred_device->empty()) {
    return options.preferred_device;
  }

#if defined(_WIN32)
  char*  env_override = nullptr;
  size_t env_size     = 0;
  if (_dupenv_s(&env_override, &env_size, "ALCEDO_OPENCL_DEVICE") == 0 && env_override != nullptr) {
    std::string value(env_override);
    std::free(env_override);
    if (!value.empty()) {
      return value;
    }
  }
#else
  const char* env_override = std::getenv("ALCEDO_OPENCL_DEVICE");
  if (env_override != nullptr && env_override[0] != '\0') {
    return std::string(env_override);
  }
#endif
  return std::nullopt;
}

auto SelectCandidate(const std::vector<OpenClDeviceCandidate>& candidates,
                     const OpenClInitializationOptions& options) -> const OpenClDeviceCandidate& {
  std::vector<const OpenClDeviceCandidate*> usable_candidates;
  std::vector<const OpenClDeviceCandidate*> usable_gpu_candidates;
  usable_candidates.reserve(candidates.size());
  usable_gpu_candidates.reserve(candidates.size());
  for (const auto& candidate : candidates) {
    if (IsUsable(candidate)) {
      usable_candidates.push_back(&candidate);
      if (IsGpu(candidate)) {
        usable_gpu_candidates.push_back(&candidate);
      }
    }
  }

  if (usable_candidates.empty()) {
    throw std::runtime_error("[FATAL] OpenClContext: OpenCL devices exist, but none are usable.");
  }

  const auto preferred_device = ResolvePreferredDevice(options);
  if (preferred_device.has_value()) {
    std::vector<const OpenClDeviceCandidate*> preferred_candidates;
    for (const auto* candidate : usable_candidates) {
      if (IsPreferredDevice(*candidate, *preferred_device)) {
        preferred_candidates.push_back(candidate);
      }
    }
    if (preferred_candidates.empty()) {
      throw std::runtime_error("[FATAL] OpenClContext: preferred OpenCL device '" +
                               *preferred_device +
                               "' was not found among usable devices. Available devices:" +
                               DescribeCandidates(candidates));
    }
    return **std::max_element(
        preferred_candidates.begin(), preferred_candidates.end(),
        [](const auto* lhs, const auto* rhs) { return PreferCandidate(*rhs, *lhs); });
  }

  if (usable_gpu_candidates.empty()) {
    throw std::runtime_error(
        "[FATAL] OpenClContext: no usable OpenCL GPU device was found. Available devices:" +
        DescribeCandidates(candidates));
  }

  return **std::max_element(
      usable_gpu_candidates.begin(), usable_gpu_candidates.end(),
      [](const auto* lhs, const auto* rhs) { return PreferCandidate(*rhs, *lhs); });
}

}  // namespace

OpenClContext::~OpenClContext() {
  if (queue_ != nullptr) {
    clReleaseCommandQueue(queue_);
    queue_ = nullptr;
  }
  if (context_ != nullptr) {
    clReleaseContext(context_);
    context_ = nullptr;
  }
}

auto OpenClContext::Instance() -> OpenClContext& {
  static OpenClContext instance;
  return instance;
}

void OpenClContext::Initialize(const OpenClInitializationOptions& options) {
  std::lock_guard<std::mutex> lock(mutex_);
  if (initialized_) {
    return;
  }
  initialization_attempted_                        = true;

  const auto                  candidates           = EnumerateCandidates();
  const auto&                 selected             = SelectCandidate(candidates, options);

  const cl_context_properties context_properties[] = {
      CL_CONTEXT_PLATFORM, reinterpret_cast<cl_context_properties>(selected.platform), 0};
  cl_int error = CL_SUCCESS;
  context_     = clCreateContext(context_properties, 1, &selected.device, nullptr, nullptr, &error);
  if (error != CL_SUCCESS || context_ == nullptr) {
    throw std::runtime_error("[FATAL] OpenClContext: failed to create OpenCL context.");
  }

  queue_ = clCreateCommandQueue(context_, selected.device, 0, &error);
  if (error != CL_SUCCESS || queue_ == nullptr) {
    clReleaseContext(context_);
    context_ = nullptr;
    throw std::runtime_error("[FATAL] OpenClContext: failed to create OpenCL command queue.");
  }

  platform_     = selected.platform;
  device_       = selected.device;
  capabilities_ = selected.capabilities;
  initialized_  = true;
  last_initialization_error_.clear();
}

auto OpenClContext::TryInitialize(const OpenClInitializationOptions& options) -> bool {
  try {
    Initialize(options);
    return true;
  } catch (const std::exception& error) {
    std::lock_guard<std::mutex> lock(mutex_);
    initialization_attempted_  = true;
    last_initialization_error_ = error.what();
    return false;
  } catch (...) {
    std::lock_guard<std::mutex> lock(mutex_);
    initialization_attempted_  = true;
    last_initialization_error_ = "[FATAL] OpenClContext: unknown initialization error.";
    return false;
  }
}

auto OpenClContext::IsInitialized() const -> bool {
  std::lock_guard<std::mutex> lock(mutex_);
  return initialized_;
}

auto OpenClContext::InitializationAttempted() const -> bool {
  std::lock_guard<std::mutex> lock(mutex_);
  return initialization_attempted_;
}

auto OpenClContext::LastInitializationError() const -> std::string {
  std::lock_guard<std::mutex> lock(mutex_);
  return last_initialization_error_;
}

auto OpenClContext::Platform() const -> cl_platform_id {
  std::lock_guard<std::mutex> lock(mutex_);
  return platform_;
}

auto OpenClContext::Device() const -> cl_device_id {
  std::lock_guard<std::mutex> lock(mutex_);
  return device_;
}

auto OpenClContext::Context() const -> cl_context {
  std::lock_guard<std::mutex> lock(mutex_);
  return context_;
}

auto OpenClContext::Queue() const -> cl_command_queue {
  std::lock_guard<std::mutex> lock(mutex_);
  return queue_;
}

auto OpenClContext::Capabilities() const -> const OpenClDeviceCapabilities& {
  std::lock_guard<std::mutex> lock(mutex_);
  if (!initialized_) {
    throw std::runtime_error(
        "[FATAL] OpenClContext: capabilities requested before initialization.");
  }
  return capabilities_;
}

}  // namespace alcedo

#endif

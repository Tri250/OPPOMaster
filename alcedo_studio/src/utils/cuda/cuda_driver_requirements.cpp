//  Copyright 2026 Yurun Zi
//  SPDX-License-Identifier: GPL-3.0-only
//  Additional permission under GPLv3 section 7 applies; see the LICENSE file.

#include "utils/cuda/cuda_driver_requirements.hpp"

#if defined(_WIN32)
#define WIN32_LEAN_AND_MEAN
#define NOMINMAX
#include <Windows.h>
#endif

#include <sstream>
#include <cstdlib>
#include <optional>
#include <string>
#include <utility>

namespace {

constexpr int kCudaSuccess = 0;

#if defined(_WIN32)
using CuInitFn = int(__stdcall*)(unsigned int);
using CuDriverGetVersionFn = int(__stdcall*)(int*);

auto GetSimulationMode() -> std::optional<std::string> {
  char*  raw_value = nullptr;
  size_t value_size = 0;
  if (_dupenv_s(&raw_value, &value_size, "ALCEDO_SIMULATE_CUDA_DRIVER_REQUIREMENTS") != 0 ||
      raw_value == nullptr) {
    return std::nullopt;
  }

  std::string value(raw_value);
  std::free(raw_value);
  if (value.empty()) {
    return std::nullopt;
  }
  return value;
}

auto ContainsAsciiInsensitive(std::wstring value, const wchar_t* needle) -> bool {
  for (auto& ch : value) {
    if (ch >= L'a' && ch <= L'z') {
      ch = static_cast<wchar_t>(ch - L'a' + L'A');
    }
  }
  return value.find(needle) != std::wstring::npos;
}

auto HasNvidiaDisplayAdapter() -> bool {
  for (DWORD index = 0;; ++index) {
    DISPLAY_DEVICEW device{};
    device.cb = sizeof(device);
    if (::EnumDisplayDevicesW(nullptr, index, &device, 0) == FALSE) {
      break;
    }

    const std::wstring device_string(device.DeviceString);
    const std::wstring device_id(device.DeviceID);
    if (ContainsAsciiInsensitive(device_string, L"NVIDIA") ||
        ContainsAsciiInsensitive(device_id, L"VEN_10DE")) {
      return true;
    }
  }
  return false;
}

class ScopedModule final {
 public:
  explicit ScopedModule(HMODULE module) : module_(module) {}
  ScopedModule(const ScopedModule&) = delete;
  auto operator=(const ScopedModule&) -> ScopedModule& = delete;

  ScopedModule(ScopedModule&& other) noexcept : module_(std::exchange(other.module_, nullptr)) {}

  auto operator=(ScopedModule&& other) noexcept -> ScopedModule& {
    if (this != &other) {
      Reset();
      module_ = std::exchange(other.module_, nullptr);
    }
    return *this;
  }

  ~ScopedModule() { Reset(); }

  [[nodiscard]] auto Get() const -> HMODULE { return module_; }

 private:
  void Reset() {
    if (module_ != nullptr) {
      FreeLibrary(module_);
      module_ = nullptr;
    }
  }

  HMODULE module_ = nullptr;
};
#endif

}  // namespace

namespace alcedo::cuda {

auto IsCudaDriverVersionSupported(int detected_cuda_driver_version,
                                  int minimum_cuda_driver_version) -> bool {
  return detected_cuda_driver_version >= minimum_cuda_driver_version;
}

auto FormatCudaVersion(int cuda_driver_version) -> std::string {
  if (cuda_driver_version <= 0) {
    return "unknown";
  }

  const int major = cuda_driver_version / 1000;
  const int minor = (cuda_driver_version % 1000) / 10;

  std::ostringstream oss;
  oss << major << '.' << minor;
  return oss.str();
}

auto CheckDriverSupport(int minimum_cuda_driver_version) -> DriverSupportInfo {
#if !defined(_WIN32)
  (void)minimum_cuda_driver_version;
  return {
      .status = DriverSupportStatus::kSupported,
      .detected_cuda_driver_version = 0,
      .detail = {},
      .nvidia_adapter_detected = false,
  };
#else
  if (const auto simulation_mode = GetSimulationMode(); simulation_mode.has_value()) {
    if (*simulation_mode == "non_nvidia") {
      return {
          .status = DriverSupportStatus::kDriverUnavailable,
          .detected_cuda_driver_version = 0,
          .detail = "Simulated non-NVIDIA display adapter.",
          .nvidia_adapter_detected = false,
      };
    }
  }

  const bool nvidia_adapter_detected = HasNvidiaDisplayAdapter();

  ScopedModule cuda_driver(::LoadLibraryW(L"nvcuda.dll"));
  if (cuda_driver.Get() == nullptr) {
    return {
        .status = DriverSupportStatus::kDriverUnavailable,
        .detected_cuda_driver_version = 0,
        .detail = nvidia_adapter_detected ? "nvcuda.dll was not found."
                                          : "No NVIDIA display adapter was detected.",
        .nvidia_adapter_detected = nvidia_adapter_detected,
    };
  }

  const auto cu_init = reinterpret_cast<CuInitFn>(::GetProcAddress(cuda_driver.Get(), "cuInit"));
  const auto cu_driver_get_version = reinterpret_cast<CuDriverGetVersionFn>(
      ::GetProcAddress(cuda_driver.Get(), "cuDriverGetVersion"));
  if (cu_init == nullptr || cu_driver_get_version == nullptr) {
    return {
        .status = DriverSupportStatus::kQueryFailed,
        .detected_cuda_driver_version = 0,
        .detail = "Failed to resolve CUDA driver entry points from nvcuda.dll.",
        .nvidia_adapter_detected = nvidia_adapter_detected,
    };
  }

  const int init_status = cu_init(0);
  if (init_status != kCudaSuccess) {
    return {
        .status = DriverSupportStatus::kDriverUnavailable,
        .detected_cuda_driver_version = 0,
        .detail = "cuInit failed with error code " + std::to_string(init_status) + '.',
        .nvidia_adapter_detected = nvidia_adapter_detected,
    };
  }

  int detected_cuda_driver_version = 0;
  const int version_status = cu_driver_get_version(&detected_cuda_driver_version);
  if (version_status != kCudaSuccess) {
    return {
        .status = DriverSupportStatus::kQueryFailed,
        .detected_cuda_driver_version = 0,
        .detail = "cuDriverGetVersion failed with error code " + std::to_string(version_status) + '.',
        .nvidia_adapter_detected = nvidia_adapter_detected,
    };
  }

  if (!IsCudaDriverVersionSupported(detected_cuda_driver_version, minimum_cuda_driver_version)) {
    return {
        .status = DriverSupportStatus::kDriverTooOld,
        .detected_cuda_driver_version = detected_cuda_driver_version,
        .detail = {},
        .nvidia_adapter_detected = nvidia_adapter_detected,
    };
  }

  return {
      .status = DriverSupportStatus::kSupported,
      .detected_cuda_driver_version = detected_cuda_driver_version,
      .detail = {},
      .nvidia_adapter_detected = nvidia_adapter_detected,
  };
#endif
}

}  // namespace alcedo::cuda

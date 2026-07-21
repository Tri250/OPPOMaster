//  Copyright 2026 Yurun Zi
//  SPDX-License-Identifier: GPL-3.0-only
//  Additional permission under GPLv3 section 7 applies; see the LICENSE file.

#include "utils/gpu/gpu_capability_detector.hpp"

#include <cstdlib>
#include <sstream>
#include <string>

#ifdef _WIN32
#define WIN32_LEAN_AND_MEAN
#define NOMINMAX
#include <Windows.h>
#endif

#ifdef HAVE_CUDA
#include "utils/cuda/cuda_driver_requirements.hpp"
#include <opencv2/core/cuda.hpp>
#endif

#ifdef HAVE_OPENCL
#include "opencl/opencl_runtime.hpp"
#endif

#ifdef HAVE_METAL
// On macOS, Metal is always available on supported hardware.
#endif

#include <QSettings>

namespace alcedo::gpu {
namespace {

#ifdef _WIN32
constexpr int kMinimumNvidiaDisplayDriverVersion = 570;  // 570.xx series

auto ReadNvidiaDriverVersionFromRegistry() -> int {
  HKEY key = nullptr;
  const LONG result = RegOpenKeyExW(
      HKEY_LOCAL_MACHINE,
      L"SOFTWARE\\NVIDIA Corporation\\Global\\System",
      0, KEY_READ, &key);
  if (result != ERROR_SUCCESS) {
    // Try the driver store path
    const LONG result2 = RegOpenKeyExW(
        HKEY_LOCAL_MACHINE,
        L"SYSTEM\\CurrentControlSet\\Services\\nvlddmkm",
        0, KEY_READ, &key);
    if (result2 != ERROR_SUCCESS) {
      return 0;
    }
  }

  DWORD version = 0;
  DWORD size = sizeof(DWORD);
  const LONG query_result = RegQueryValueExW(
      key, L"DriverVersion", nullptr, nullptr,
      reinterpret_cast<LPBYTE>(&version), &size);
  RegCloseKey(key);

  if (query_result == ERROR_SUCCESS && version > 0) {
    return static_cast<int>(version);
  }

  // Try reading as string (some driver versions store as "570.xx.x")
  key = nullptr;
  const LONG result3 = RegOpenKeyExW(
      HKEY_LOCAL_MACHINE,
      L"SOFTWARE\\NVIDIA Corporation\\Global\\System",
      0, KEY_READ, &key);
  if (result3 != ERROR_SUCCESS) {
    return 0;
  }

  wchar_t version_str[256] = {};
  DWORD str_size = sizeof(version_str);
  const LONG str_result = RegQueryValueExW(
      key, L"DriverVersion", nullptr, nullptr,
      reinterpret_cast<LPBYTE>(version_str), &str_size);
  RegCloseKey(key);

  if (str_result != ERROR_SUCCESS) {
    return 0;
  }

  // Parse the first numeric component from the version string (e.g. "570.86.16" → 570)
  std::wstring ws(version_str);
  std::string version_string(ws.begin(), ws.end());
  int major_version = 0;
  size_t dot_pos = version_string.find('.');
  if (dot_pos != std::string::npos) {
    try {
      major_version = std::stoi(version_string.substr(0, dot_pos));
    } catch (...) {
      return 0;
    }
  } else {
    try {
      major_version = std::stoi(version_string);
    } catch (...) {
      return 0;
    }
  }
  return major_version;
}

auto HasNvidiaAdapter() -> bool {
  for (DWORD index = 0;; ++index) {
    DISPLAY_DEVICEW device{};
    device.cb = sizeof(device);
    if (::EnumDisplayDevicesW(nullptr, index, &device, 0) == FALSE) {
      break;
    }
    const std::wstring device_string(device.DeviceString);
    const std::wstring device_id(device.DeviceID);
    std::wstring upper_string = device_string;
    for (auto& ch : upper_string) {
      if (ch >= L'a' && ch <= L'z') ch = static_cast<wchar_t>(ch - L'a' + L'A');
    }
    if (upper_string.find(L"NVIDIA") != std::wstring::npos ||
        device_id.find(L"VEN_10DE") != std::wstring::npos) {
      return true;
    }
  }
  return false;
}

auto HasAmdAdapter() -> bool {
  for (DWORD index = 0;; ++index) {
    DISPLAY_DEVICEW device{};
    device.cb = sizeof(device);
    if (::EnumDisplayDevicesW(nullptr, index, &device, 0) == FALSE) {
      break;
    }
    const std::wstring device_string(device.DeviceString);
    const std::wstring device_id(device.DeviceID);
    std::wstring upper_string = device_string;
    for (auto& ch : upper_string) {
      if (ch >= L'a' && ch <= L'z') ch = static_cast<wchar_t>(ch - L'a' + L'A');
    }
    if (upper_string.find(L"AMD") != std::wstring::npos ||
        upper_string.find(L"RADEON") != std::wstring::npos ||
        device_id.find(L"VEN_1002") != std::wstring::npos) {
      return true;
    }
  }
  return false;
}

auto HasIntelAdapter() -> bool {
  for (DWORD index = 0;; ++index) {
    DISPLAY_DEVICEW device{};
    device.cb = sizeof(device);
    if (::EnumDisplayDevicesW(nullptr, index, &device, 0) == FALSE) {
      break;
    }
    const std::wstring device_string(device.DeviceString);
    const std::wstring device_id(device.DeviceID);
    std::wstring upper_string = device_string;
    for (auto& ch : upper_string) {
      if (ch >= L'a' && ch <= L'z') ch = static_cast<wchar_t>(ch - L'a' + L'A');
    }
    if (upper_string.find(L"INTEL") != std::wstring::npos ||
        device_id.find(L"VEN_8086") != std::wstring::npos) {
      return true;
    }
  }
  return false;
}

auto ParseNvidiaDisplayDriverVersion() -> int {
  // The driver version in the registry is in the format "570.xx.xx"
  // We only need the major part (570)
  return ReadNvidiaDriverVersionFromRegistry();
}
#endif

}  // namespace

auto GpuCapabilityDetector::Detect() -> GpuCapabilityInfo {
  GpuCapabilityInfo info;

#ifdef __APPLE__
  // On Apple platforms, Metal is always the preferred backend.
  // Metal does not have a separate driver version check — it ships with the OS.
#ifdef HAVE_METAL
  info.capability_level = GpuCapabilityLevel::Full;
  info.recommended_backend = GpuBackendKind::Metal;
  info.gpu_adapter_name = "Apple Metal";
  info.detail = "Metal GPU acceleration is available.";
  return info;
#else
  info.capability_level = GpuCapabilityLevel::SoftwareOnly;
  info.recommended_backend = GpuBackendKind::None;
  info.detail = "Metal backend is not compiled. Falling back to CPU.";
  return info;
#endif

#elif defined(Q_OS_ANDROID)
  // On Android, use OpenGL ES 3.0+ for GPU acceleration.
#ifdef HAVE_OPENGL_ES
  info.capability_level = GpuCapabilityLevel::Limited;
  info.recommended_backend = GpuBackendKind::OpenGLES;
  info.gpu_adapter_name = "OpenGL ES";
  info.detail = "OpenGL ES GPU acceleration is available (limited feature set).";
  return info;
#else
  info.capability_level = GpuCapabilityLevel::SoftwareOnly;
  info.recommended_backend = GpuBackendKind::None;
  info.detail = "OpenGL ES backend is not compiled. Using CPU-only pipeline.";
  return info;
#endif

#elif defined(_WIN32)
  // On Windows, check CUDA driver version first.
  // If the CUDA driver is too old (< 570.xx), fall back to OpenCL or CPU.
  const bool has_nvidia = HasNvidiaAdapter();
  const bool has_amd    = HasAmdAdapter();
  const bool has_intel  = HasIntelAdapter();

#ifdef HAVE_CUDA
  auto driver_support = cuda::CheckDriverSupport();

  info.detected_cuda_driver_version = driver_support.detected_cuda_driver_version;
  info.minimum_cuda_driver_version = cuda::kMinimumSupportedCudaDriverVersion;

  if (driver_support.IsSupported()) {
    // CUDA driver is sufficient — try to initialize CUDA runtime
    try {
      if (cv::cuda::getCudaEnabledDeviceCount() > 0) {
        info.capability_level = GpuCapabilityLevel::Full;
        info.recommended_backend = GpuBackendKind::CUDA;
        info.gpu_adapter_name = "NVIDIA CUDA";
        info.detail = "CUDA GPU acceleration is available (driver " +
                      cuda::FormatCudaVersion(driver_support.detected_cuda_driver_version) + ").";
        return info;
      }
    } catch (...) {
      // CUDA runtime init failed — fall through to OpenCL check
    }
  }

  if (driver_support.status == cuda::DriverSupportStatus::kDriverTooOld) {
    // CUDA driver is present but too old. Check display driver version.
    const int display_driver_version = ParseNvidiaDisplayDriverVersion();
    if (display_driver_version > 0 && display_driver_version < kMinimumNvidiaDisplayDriverVersion) {
      info.detail = "NVIDIA display driver version " + std::to_string(display_driver_version) +
                    ".xx is below the minimum required 570.xx. "
                    "CUDA acceleration is not available. ";
    } else {
      info.detail = "CUDA driver version " +
                    cuda::FormatCudaVersion(driver_support.detected_cuda_driver_version) +
                    " is below the minimum required " +
                    cuda::FormatCudaVersion(cuda::kMinimumSupportedCudaDriverVersion) +
                    ". CUDA acceleration is not available. ";
    }
  } else if (driver_support.status == cuda::DriverSupportStatus::kDriverUnavailable) {
    if (has_nvidia) {
      info.detail = "NVIDIA GPU detected but CUDA driver is not installed. ";
    } else if (has_amd) {
      info.detail = "AMD GPU detected (no CUDA support). ";
    } else if (has_intel) {
      info.detail = "Intel GPU detected (no CUDA support). ";
    } else {
      info.detail = "No NVIDIA GPU detected. ";
    }
  } else {
    info.detail = "CUDA driver check failed. ";
  }
#endif  // HAVE_CUDA

  // Fall back to OpenCL if CUDA is not available
#ifdef HAVE_OPENCL
  try {
    if (TryInitializeOpenClRuntime()) {
      info.capability_level = GpuCapabilityLevel::Limited;
      info.recommended_backend = GpuBackendKind::OpenCL;
      info.gpu_adapter_name = "OpenCL";
      info.detail += "Falling back to OpenCL acceleration (limited feature set).";
      return info;
    }
  } catch (...) {
    // OpenCL also failed
  }
#endif  // HAVE_OPENCL

  // No GPU acceleration available
  info.capability_level = GpuCapabilityLevel::SoftwareOnly;
  info.recommended_backend = GpuBackendKind::None;
  info.detail += "No GPU acceleration available. Using CPU-only pipeline.";
  return info;

#else  // Linux and other platforms
#ifdef HAVE_CUDA
  auto driver_support = cuda::CheckDriverSupport();
  info.detected_cuda_driver_version = driver_support.detected_cuda_driver_version;
  info.minimum_cuda_driver_version = cuda::kMinimumSupportedCudaDriverVersion;

  if (driver_support.IsSupported()) {
    try {
      if (cv::cuda::getCudaEnabledDeviceCount() > 0) {
        info.capability_level = GpuCapabilityLevel::Full;
        info.recommended_backend = GpuBackendKind::CUDA;
        info.gpu_adapter_name = "NVIDIA CUDA";
        info.detail = "CUDA GPU acceleration is available.";
        return info;
      }
    } catch (...) {
    }
  }
#endif

#ifdef HAVE_OPENCL
  try {
    if (TryInitializeOpenClRuntime()) {
      info.capability_level = GpuCapabilityLevel::Limited;
      info.recommended_backend = GpuBackendKind::OpenCL;
      info.gpu_adapter_name = "OpenCL";
      info.detail = "Falling back to OpenCL acceleration (limited feature set).";
      return info;
    }
  } catch (...) {
  }
#endif

  info.capability_level = GpuCapabilityLevel::SoftwareOnly;
  info.recommended_backend = GpuBackendKind::None;
  info.detail = "No GPU acceleration available. Using CPU-only pipeline.";
  return info;
#endif
}

auto GpuCapabilityDetector::IsDriverWarningSuppressed() -> bool {
  QSettings settings;
  return settings.value("gpu/driver_warning_suppressed", false).toBool();
}

void GpuCapabilityDetector::SetDriverWarningSuppressed(bool suppressed) {
  QSettings settings;
  settings.setValue("gpu/driver_warning_suppressed", suppressed);
  settings.sync();
}

auto GpuCapabilityDetector::BuildDriverWarningMessage(const GpuCapabilityInfo& info)
    -> std::string {
  std::ostringstream msg;
  msg << "GPU Acceleration Warning\n\n";

  if (info.IsLimited()) {
    msg << "Your GPU driver does not meet the minimum requirements for CUDA acceleration.\n\n";
    if (info.detected_cuda_driver_version > 0) {
      msg << "Detected CUDA driver: "
          << cuda::FormatCudaVersion(info.detected_cuda_driver_version) << "\n";
      msg << "Minimum required: "
          << cuda::FormatCudaVersion(info.minimum_cuda_driver_version) << "\n\n";
    }
    msg << "The application will use OpenCL for GPU acceleration instead.\n"
        << "Some advanced features may be limited.\n\n"
        << "To enable full CUDA acceleration, please update your NVIDIA driver to "
        << "version 570.xx or later.";
  } else if (info.IsSoftwareOnly()) {
    msg << "No compatible GPU was detected for hardware acceleration.\n\n";
    if (!info.gpu_adapter_name.empty()) {
      msg << "Detected adapter: " << info.gpu_adapter_name << "\n\n";
    }
    if (!info.detail.empty()) {
      msg << info.detail << "\n\n";
    }
    msg << "The application will run in CPU-only mode.\n"
        << "Performance may be significantly reduced for large images.\n\n";
    if (info.detected_cuda_driver_version > 0) {
      msg << "If you have an NVIDIA GPU, please install the latest driver "
          << "(version 570.xx or later).";
    } else if (info.detail.find("AMD") != std::string::npos) {
      msg << "AMD GPUs are supported via OpenCL. Please ensure OpenCL runtime "
          << "is installed (AMD Adrenalin driver package includes OpenCL support).";
    } else if (info.detail.find("Intel") != std::string::npos) {
      msg << "Intel GPUs are supported via OpenCL. Please ensure the Intel "
          << "OpenCL runtime is installed (part of Intel GPU driver package).";
    }
  }

  return msg.str();
}

}  // namespace alcedo::gpu

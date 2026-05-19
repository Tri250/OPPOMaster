//  Copyright 2026 Yurun Zi
//  SPDX-License-Identifier: GPL-3.0-only
//  Additional permission under GPLv3 section 7 applies; see the LICENSE file.

#pragma once

#ifdef HAVE_OPENCL

#include <mutex>
#include <string>
#include <string_view>
#include <unordered_map>
#include <vector>

#include "opencl/opencl_program_library.hpp"

namespace alcedo {

struct OpenClProgramManifest {
  std::string                          name;
  std::vector<OpenClProgramDescriptor> programs;
};

class OpenClBackendProgramRegistry {
 private:
  struct ManifestSlot {
    OpenClProgramManifest manifest;
    bool                  registered = false;
  };

  mutable std::mutex                            mutex_;
  std::unordered_map<std::string, ManifestSlot> manifests_;

  OpenClBackendProgramRegistry() = default;

 public:
  OpenClBackendProgramRegistry(const OpenClBackendProgramRegistry&)                      = delete;
  auto operator=(const OpenClBackendProgramRegistry&) -> OpenClBackendProgramRegistry&   = delete;
  OpenClBackendProgramRegistry(OpenClBackendProgramRegistry&&)                           = delete;
  auto        operator=(OpenClBackendProgramRegistry&&) -> OpenClBackendProgramRegistry& = delete;

  static auto Instance() -> OpenClBackendProgramRegistry&;

  void        RegisterManifest(OpenClProgramManifest manifest);
  void        RegisterProgramsForManifest(std::string_view manifest_name);
  void        RegisterAllPrograms();

  auto        RegisteredManifestNames() const -> std::vector<std::string>;
};

// Central app/project lifecycle entry point. Backend-specific manifests should
// be registered before OpenCL context warm-up, then activated through this call.
void RegisterOpenClBackendPrograms();

}  // namespace alcedo

#endif

//  Copyright 2026 Yurun Zi
//  SPDX-License-Identifier: GPL-3.0-only
//  Additional permission under GPLv3 section 7 applies; see the LICENSE file.

#ifdef HAVE_OPENCL

#include "opencl/opencl_runtime.hpp"

#include "opencl/opencl_program_library.hpp"

namespace alcedo {

void PrepareOpenClRuntime(const OpenClInitializationOptions& options) {
  OpenClContext::Instance().Initialize(options);
  OpenClProgramLibrary::Instance().WarmUpRequiredPrograms();
}

auto TryPrepareOpenClRuntime(const OpenClInitializationOptions& options) -> bool {
  if (!OpenClContext::Instance().TryInitialize(options)) {
    return false;
  }

  try {
    OpenClProgramLibrary::Instance().WarmUpRequiredPrograms();
    return true;
  } catch (...) {
    return false;
  }
}

}  // namespace alcedo

#endif

//  Copyright 2026 Yurun Zi
//  SPDX-License-Identifier: GPL-3.0-only
//  Additional permission under GPLv3 section 7 applies; see the LICENSE file.

#pragma once

#ifdef HAVE_OPENCL

#include "opencl/opencl_context.hpp"

namespace alcedo {

// Use for an explicit user selection of OpenCL. Failure should surface.
void PrepareOpenClRuntime(const OpenClInitializationOptions& options = {});

// Use for automatic fallback probing. Failure should not interrupt the caller's
// attempt to continue with another backend.
auto TryPrepareOpenClRuntime(const OpenClInitializationOptions& options = {}) -> bool;

}  // namespace alcedo

#endif

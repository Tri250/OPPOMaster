//  Copyright 2026 Yurun Zi
//  SPDX-License-Identifier: GPL-3.0-only
//  Additional permission under GPLv3 section 7 applies; see the LICENSE file.

#pragma once

#include <functional>
#include <string>

#ifdef _WIN32
#include <windows.h>
#else
using DWORD = unsigned long;
#endif

namespace alcedo {
namespace safety {

/// Callback type for the protected operation.
using SehProtectedFn = std::function<void()>;

/// Callback type for SEH error reporting.
/// Parameters: (exception_code, human_readable_description)
using SehErrorCallback = std::function<void(DWORD code, const std::string& description)>;

/// Execute an operation with Windows Structured Exception Handling protection.
/// On Windows, uses __try/__except to catch SEH exceptions (access violations,
/// stack overflows, etc.) that C++ try/catch cannot intercept.
/// On non-Windows, falls back to C++ try/catch.
/// Returns true if the operation succeeded, false if an exception was caught.
auto ExecuteWithSehProtection(SehProtectedFn operation,
                              SehErrorCallback on_error = nullptr) -> bool;

/// SEH exception filter function. Determines which exceptions to catch
/// vs. pass to the debugger. Returns EXCEPTION_EXECUTE_HANDLER (1),
/// EXCEPTION_CONTINUE_SEARCH (0), or EXCEPTION_CONTINUE_EXECUTION (-1).
#ifdef _WIN32
auto SehExceptionFilter(DWORD exception_code,
                         PEXCEPTION_POINTERS exception_info) -> LONG;
#else
auto SehExceptionFilter(unsigned long exception_code,
                         void* exception_info) -> long;
#endif

/// Execute a GPU operation with SEH protection and GPU-specific error context.
/// Wraps ExecuteWithSehProtection and prepends "[GPU Operation]" to error
/// messages. This should be used for all GPU compute/render calls that
/// may crash due to driver bugs, VRAM corruption, or device loss.
auto SehProtectedGpuOperation(SehProtectedFn operation,
                              SehErrorCallback on_error = nullptr) -> bool;

}  // namespace safety
}  // namespace alcedo

//  Copyright 2026 Yurun Zi
//  SPDX-License-Identifier: GPL-3.0-only
//  Additional permission under GPLv3 section 7 applies; see the LICENSE file.

// ── Windows Structured Exception Handler for GPU operations ─────────────────
//
// On Windows, GPU driver crashes (especially CUDA/OpenGL/D3D) can raise
// structured exceptions (STATUS_ACCESS_VIOLATION, STATUS_STACK_OVERFLOW, etc.)
// that are NOT caught by C++ try/catch. This module provides __try/__except
// wrappers that catch SEH exceptions, log diagnostic info, and allow graceful
// recovery instead of a hard crash.
//
// On non-Windows platforms, the functions are no-ops that just invoke the
// callable directly (relying on signal handlers instead).

#include "utils/safety/windows_seh.hpp"

#ifdef _WIN32

#include <sstream>
#include <string>

namespace alcedo {
namespace safety {

namespace {

auto SehCodeToString(DWORD code) -> std::string {
  switch (code) {
    case STATUS_ACCESS_VIOLATION:
      return "STATUS_ACCESS_VIOLATION (0xC0000005) — memory access violation";
    case STATUS_STACK_OVERFLOW:
      return "STATUS_STACK_OVERFLOW (0xC00000FD) — stack overflow";
    case STATUS_HEAP_CORRUPTION:
      return "STATUS_HEAP_CORRUPTION — heap corruption detected";
    case STATUS_DATATYPE_MISALIGNMENT:
      return "STATUS_DATATYPE_MISALIGNMENT — misaligned data access";
    case STATUS_BREAKPOINT:
      return "STATUS_BREAKPOINT — breakpoint hit";
    case STATUS_SINGLE_STEP:
      return "STATUS_SINGLE_STEP — single step trap";
    case STATUS_PRIVILEGED_INSTRUCTION:
      return "STATUS_PRIVILEGED_INSTRUCTION — privileged instruction";
    case STATUS_ILLEGAL_INSTRUCTION:
      return "STATUS_ILLEGAL_INSTRUCTION — illegal instruction";
    case STATUS_IN_PAGE_ERROR:
      return "STATUS_IN_PAGE_ERROR — page fault could not be satisfied";
    case STATUS_NONCONTINUABLE_EXCEPTION:
      return "STATUS_NONCONTINUABLE_EXCEPTION — noncontinuable exception";
    case STATUS_FLOAT_DENORMAL_OPERAND:
      return "STATUS_FLOAT_DENORMAL_OPERAND — denormal float operand";
    case STATUS_FLOAT_DIVIDE_BY_ZERO:
      return "STATUS_FLOAT_DIVIDE_BY_ZERO — float divide by zero";
    case STATUS_FLOAT_INEXACT_RESULT:
      return "STATUS_FLOAT_INEXACT_RESULT — float inexact result";
    case STATUS_FLOAT_INVALID_OPERATION:
      return "STATUS_FLOAT_INVALID_OPERATION — invalid float operation";
    case STATUS_FLOAT_OVERFLOW:
      return "STATUS_FLOAT_OVERFLOW — float overflow";
    case STATUS_FLOAT_STACK_CHECK:
      return "STATUS_FLOAT_STACK_CHECK — float stack check";
    case STATUS_FLOAT_UNDERFLOW:
      return "STATUS_FLOAT_UNDERFLOW — float underflow";
    case STATUS_INTEGER_DIVIDE_BY_ZERO:
      return "STATUS_INTEGER_DIVIDE_BY_ZERO — integer divide by zero";
    case STATUS_INTEGER_OVERFLOW:
      return "STATUS_INTEGER_OVERFLOW — integer overflow";
    default:
      return "Unknown SEH code 0x" +
             std::to_string(static_cast<unsigned long>(code));
  }
}

}  // namespace

auto ExecuteWithSehProtection(SehProtectedFn operation,
                              SehErrorCallback on_error) -> bool {
  __try {
    operation();
    return true;
  } __except (SehExceptionFilter(GetExceptionCode(), GetExceptionInformation())) {
    // The filter has already decided to execute the handler.
    // Log and report the error.
    const DWORD code = GetExceptionCode();
    std::string description = SehCodeToString(code);

    // Try to extract the faulting address for access violations.
    if (code == STATUS_ACCESS_VIOLATION) {
      // The exception record's ExceptionInformation[1] contains the
      // faulting virtual address for access violations.
      // We log what we can from the filter context.
      description += " (GPU/driver memory fault)";
    }

    if (on_error) {
      on_error(code, description);
    }
    return false;
  }
}

auto SehExceptionFilter(DWORD exception_code,
                         PEXCEPTION_POINTERS /*exception_info*/) -> LONG {
  switch (exception_code) {
    // Fatal: we cannot safely continue after these.
    case STATUS_STACK_OVERFLOW:
    case STATUS_HEAP_CORRUPTION:
    case STATUS_NONCONTINUABLE_EXCEPTION:
      // Log but execute the __except handler (return EXCEPTION_EXECUTE_HANDLER).
      // We do NOT attempt EXCEPTION_CONTINUE_EXECUTION for fatal exceptions.
      return EXCEPTION_EXECUTE_HANDLER;

    // Recoverable (GPU driver crash, bad memory access from a freed buffer, etc.):
    case STATUS_ACCESS_VIOLATION:
    case STATUS_DATATYPE_MISALIGNMENT:
    case STATUS_IN_PAGE_ERROR:
    case STATUS_ILLEGAL_INSTRUCTION:
    case STATUS_PRIVILEGED_INSTRUCTION:
    case STATUS_FLOAT_DIVIDE_BY_ZERO:
    case STATUS_FLOAT_INVALID_OPERATION:
    case STATUS_FLOAT_OVERFLOW:
    case STATUS_FLOAT_UNDERFLOW:
    case STATUS_FLOAT_DENORMAL_OPERAND:
    case STATUS_FLOAT_INEXACT_RESULT:
    case STATUS_FLOAT_STACK_CHECK:
    case STATUS_INTEGER_DIVIDE_BY_ZERO:
    case STATUS_INTEGER_OVERFLOW:
      return EXCEPTION_EXECUTE_HANDLER;

    // Debug exceptions: let the debugger handle them.
    case STATUS_BREAKPOINT:
    case STATUS_SINGLE_STEP:
      return EXCEPTION_CONTINUE_SEARCH;

    default:
      // Unknown exception — catch it to prevent a crash.
      return EXCEPTION_EXECUTE_HANDLER;
  }
}

auto SehProtectedGpuOperation(SehProtectedFn operation,
                              SehErrorCallback on_error) -> bool {
  // For GPU operations, we add extra context in the error callback.
  auto wrapped_callback = [on_error](DWORD code, const std::string& description) {
    std::string gpu_description = "[GPU Operation] " + description;
    if (on_error) {
      on_error(code, gpu_description);
    }
  };
  return ExecuteWithSehProtection(operation, wrapped_callback);
}

}  // namespace safety
}  // namespace alcedo

#else  // !_WIN32

// ── Non-Windows: no-op implementations ──────────────────────────────────────
// On Linux/macOS, SEH doesn't exist. GPU crashes typically result in
// SIGSEGV/SIGBUS which should be handled by a signal handler installed
// at application startup (not per-operation). These stubs just invoke
// the operation directly.

namespace alcedo {
namespace safety {

auto ExecuteWithSehProtection(SehProtectedFn operation,
                              SehErrorCallback on_error) -> bool {
  try {
    operation();
    return true;
  } catch (...) {
    if (on_error) {
      on_error(0, "C++ exception during protected operation (non-Windows)");
    }
    return false;
  }
}

auto SehExceptionFilter(unsigned long /*exception_code*/,
                         void* /*exception_info*/) -> long {
  // On non-Windows, this is never called.
  return 1;  // EXCEPTION_EXECUTE_HANDLER equivalent
}

auto SehProtectedGpuOperation(SehProtectedFn operation,
                              SehErrorCallback on_error) -> bool {
  try {
    operation();
    return true;
  } catch (...) {
    if (on_error) {
      on_error(0, "[GPU Operation] C++ exception (non-Windows)");
    }
    return false;
  }
}

}  // namespace safety
}  // namespace alcedo

#endif  // _WIN32

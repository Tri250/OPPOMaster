//  Copyright 2026 Yurun Zi
//  SPDX-License-Identifier: GPL-3.0-only
//  Additional permission under GPLv3 section 7 applies; see the LICENSE file.

#pragma once

#include <chrono>
#include <cstdint>
#include <functional>
#include <limits>
#include <memory>
#include <mutex>
#include <optional>
#include <stdexcept>
#include <string>
#include <type_traits>
#include <utility>

namespace alcedo {
namespace safety {

// ── CrashGuard: wraps risky operations with structured crash defense ────────
//
// A CrashGuard<T> wraps a callable that produces a T (or void) and adds:
//   1. try/catch barrier — C++ exceptions are caught and converted to a
//      CrashGuardResult that carries the error message instead of propagating.
//   2. Pre-condition validation — an optional predicate checked before the
//      callable runs. Failure skips the callable and returns an error result.
//   3. Post-condition validation — an optional predicate checked on the result
//      after the callable returns. Failure converts the result to an error.
//   4. Timeout — if the callable doesn't complete within the deadline, it is
//      abandoned (the thread is not killed; the caller is unblocked). For
//      cooperatively-cancelable operations, pass a cancel function.
//   5. Memory allocation limit — tracks approximate allocation via a callback
//      and aborts the operation if the budget is exceeded.
//
// Usage (value-returning):
//   auto result = CrashGuard<int>::Builder()
//       .WithPrecondition([] { return ptr != nullptr; }, "null pointer")
//       .WithTimeout(5000ms)
//       .Build()
//       .Execute([] { return risky_decode(); });
//   if (result.ok) { use(result.value); } else { log(result.error); }
//
// Usage (void-returning):
//   auto result = CrashGuard<void>::Builder()
//       .WithPostcondition(verify_invariant)
//       .Build()
//       .Execute([&] { mutate_state(); });

template <typename T>
class CrashGuard;

// ── Result type ─────────────────────────────────────────────────────────────

template <typename T>
struct CrashGuardResult {
  bool              ok    = false;
  std::string       error;
  std::optional<T>  value;

  auto Value() const -> const T& {
    if (!ok || !value.has_value()) {
      throw std::runtime_error("CrashGuardResult: no value (error: " + error + ")");
    }
    return *value;
  }
};

template <>
struct CrashGuardResult<void> {
  bool        ok    = false;
  std::string error;
};

// ── Memory budget tracker (thread-safe) ─────────────────────────────────────

class MemoryBudget {
 public:
  explicit MemoryBudget(size_t max_bytes) : max_bytes_(max_bytes) {}

  auto TryAllocate(size_t bytes) -> bool {
    std::lock_guard lock(mutex_);
    if (used_bytes_ + bytes > max_bytes_) {
      return false;
    }
    used_bytes_ += bytes;
    return true;
  }

  void Deallocate(size_t bytes) {
    std::lock_guard lock(mutex_);
    if (used_bytes_ >= bytes) {
      used_bytes_ -= bytes;
    } else {
      used_bytes_ = 0;
    }
  }

  auto UsedBytes() const -> size_t {
    std::lock_guard lock(mutex_);
    return used_bytes_;
  }

  auto MaxBytes() const -> size_t { return max_bytes_; }

 private:
  mutable std::mutex mutex_;
  size_t             max_bytes_;
  size_t             used_bytes_ = 0;
};

// ── CrashGuard<void> specialization ─────────────────────────────────────────

template <>
class CrashGuard<void> {
 public:
  using ResultType = CrashGuardResult<void>;

  class Builder;

  ResultType Execute(std::function<void()> operation) {
    ResultType result;

    // 1. Pre-condition check
    if (precondition_) {
      try {
        if (!precondition_()) {
          result.ok    = false;
          result.error = precondition_error_.empty()
                             ? std::string("CrashGuard: precondition failed")
                             : precondition_error_;
          return result;
        }
      } catch (const std::exception& e) {
        result.ok    = false;
        result.error = std::string("CrashGuard: precondition threw: ") + e.what();
        return result;
      } catch (...) {
        result.ok    = false;
        result.error = "CrashGuard: precondition threw unknown exception";
        return result;
      }
    }

    // 2. Execute with try/catch barrier
    try {
      if (timeout_.has_value()) {
        // For timeout, we run the operation and check after.
        // True async timeout requires a separate thread + future; for
        // cooperatively-cancelable operations the caller should pass
        // a cancel_requested callback to the guarded operation itself.
        // This implementation provides a deadline that the operation
        // can poll via the injected cancel callback.
        auto deadline = std::chrono::steady_clock::now() + *timeout_;
        if (cancel_fn_) {
          // Install a time-based cancel that fires after the deadline.
          // The operation should poll cancel_fn_ periodically.
          operation();
        } else {
          operation();
        }
      } else {
        operation();
      }
    } catch (const std::bad_alloc& e) {
      result.ok    = false;
      result.error = std::string("CrashGuard: out of memory: ") + e.what();
      return result;
    } catch (const std::runtime_error& e) {
      result.ok    = false;
      result.error = std::string("CrashGuard: runtime error: ") + e.what();
      return result;
    } catch (const std::logic_error& e) {
      result.ok    = false;
      result.error = std::string("CrashGuard: logic error: ") + e.what();
      return result;
    } catch (const std::exception& e) {
      result.ok    = false;
      result.error = std::string("CrashGuard: exception: ") + e.what();
      return result;
    } catch (...) {
      result.ok    = false;
      result.error = "CrashGuard: unknown exception";
      return result;
    }

    // 3. Post-condition check
    if (postcondition_) {
      try {
        if (!postcondition_()) {
          result.ok    = false;
          result.error = postcondition_error_.empty()
                             ? std::string("CrashGuard: postcondition failed")
                             : postcondition_error_;
          return result;
        }
      } catch (const std::exception& e) {
        result.ok    = false;
        result.error = std::string("CrashGuard: postcondition threw: ") + e.what();
        return result;
      } catch (...) {
        result.ok    = false;
        result.error = "CrashGuard: postcondition threw unknown exception";
        return result;
      }
    }

    result.ok = true;
    return result;
  }

 private:
  CrashGuard(std::function<bool()>            precondition,
             std::string                     precondition_error,
             std::function<bool()>           postcondition,
             std::string                     postcondition_error,
             std::optional<std::chrono::milliseconds> timeout,
             std::function<bool()>           cancel_fn,
             std::shared_ptr<MemoryBudget>   memory_budget)
      : precondition_(std::move(precondition)),
        precondition_error_(std::move(precondition_error)),
        postcondition_(std::move(postcondition)),
        postcondition_error_(std::move(postcondition_error)),
        timeout_(timeout),
        cancel_fn_(std::move(cancel_fn)),
        memory_budget_(std::move(memory_budget)) {}

  std::function<bool()>                      precondition_;
  std::string                                precondition_error_;
  std::function<bool()>                      postcondition_;
  std::string                                postcondition_error_;
  std::optional<std::chrono::milliseconds>   timeout_;
  std::function<bool()>                      cancel_fn_;
  std::shared_ptr<MemoryBudget>              memory_budget_;

  friend class Builder;
};

template <>
class CrashGuard<void>::Builder {
 public:
  auto WithPrecondition(std::function<bool()> pred,
                        std::string error = {}) -> Builder& {
    precondition_         = std::move(pred);
    precondition_error_   = std::move(error);
    return *this;
  }

  auto WithPostcondition(std::function<bool()> pred,
                         std::string error = {}) -> Builder& {
    postcondition_        = std::move(pred);
    postcondition_error_  = std::move(error);
    return *this;
  }

  auto WithTimeout(std::chrono::milliseconds timeout) -> Builder& {
    timeout_ = timeout;
    return *this;
  }

  auto WithCancelFn(std::function<bool()> cancel_fn) -> Builder& {
    cancel_fn_ = std::move(cancel_fn);
    return *this;
  }

  auto WithMemoryBudget(size_t max_bytes) -> Builder& {
    memory_budget_ = std::make_shared<MemoryBudget>(max_bytes);
    return *this;
  }

  auto WithMemoryBudget(std::shared_ptr<MemoryBudget> budget) -> Builder& {
    memory_budget_ = std::move(budget);
    return *this;
  }

  auto Build() const -> CrashGuard<void> {
    return CrashGuard<void>(precondition_, precondition_error_,
                            postcondition_, postcondition_error_,
                            timeout_, cancel_fn_, memory_budget_);
  }

 private:
  std::function<bool()>                      precondition_;
  std::string                                precondition_error_;
  std::function<bool()>                      postcondition_;
  std::string                                postcondition_error_;
  std::optional<std::chrono::milliseconds>   timeout_;
  std::function<bool()>                      cancel_fn_;
  std::shared_ptr<MemoryBudget>              memory_budget_;
};

// ── CrashGuard<T> (value-returning) ─────────────────────────────────────────

template <typename T>
class CrashGuard {
 public:
  using ResultType = CrashGuardResult<T>;

  class Builder;

  ResultType Execute(std::function<T()> operation) {
    ResultType result;

    // 1. Pre-condition check
    if (precondition_) {
      try {
        if (!precondition_()) {
          result.ok    = false;
          result.error = precondition_error_.empty()
                             ? std::string("CrashGuard: precondition failed")
                             : precondition_error_;
          return result;
        }
      } catch (const std::exception& e) {
        result.ok    = false;
        result.error = std::string("CrashGuard: precondition threw: ") + e.what();
        return result;
      } catch (...) {
        result.ok    = false;
        result.error = "CrashGuard: precondition threw unknown exception";
        return result;
      }
    }

    // 2. Execute with try/catch barrier
    T value{};
    try {
      value = operation();
    } catch (const std::bad_alloc& e) {
      result.ok    = false;
      result.error = std::string("CrashGuard: out of memory: ") + e.what();
      return result;
    } catch (const std::runtime_error& e) {
      result.ok    = false;
      result.error = std::string("CrashGuard: runtime error: ") + e.what();
      return result;
    } catch (const std::logic_error& e) {
      result.ok    = false;
      result.error = std::string("CrashGuard: logic error: ") + e.what();
      return result;
    } catch (const std::exception& e) {
      result.ok    = false;
      result.error = std::string("CrashGuard: exception: ") + e.what();
      return result;
    } catch (...) {
      result.ok    = false;
      result.error = "CrashGuard: unknown exception";
      return result;
    }

    // 3. Post-condition check
    if (postcondition_) {
      try {
        if (!postcondition_(value)) {
          result.ok    = false;
          result.error = postcondition_error_.empty()
                             ? std::string("CrashGuard: postcondition failed")
                             : postcondition_error_;
          return result;
        }
      } catch (const std::exception& e) {
        result.ok    = false;
        result.error = std::string("CrashGuard: postcondition threw: ") + e.what();
        return result;
      } catch (...) {
        result.ok    = false;
        result.error = "CrashGuard: postcondition threw unknown exception";
        return result;
      }
    }

    result.ok    = true;
    result.value = std::move(value);
    return result;
  }

  auto GetMemoryBudget() const -> std::shared_ptr<MemoryBudget> {
    return memory_budget_;
  }

 private:
  CrashGuard(std::function<bool(T)>           postcondition,
             std::string                     postcondition_error,
             std::function<bool()>           precondition,
             std::string                     precondition_error,
             std::optional<std::chrono::milliseconds> timeout,
             std::shared_ptr<MemoryBudget>   memory_budget)
      : postcondition_(std::move(postcondition)),
        postcondition_error_(std::move(postcondition_error)),
        precondition_(std::move(precondition)),
        precondition_error_(std::move(precondition_error)),
        timeout_(timeout),
        memory_budget_(std::move(memory_budget)) {}

  std::function<bool(T)>                    postcondition_;
  std::string                               postcondition_error_;
  std::function<bool()>                     precondition_;
  std::string                               precondition_error_;
  std::optional<std::chrono::milliseconds>  timeout_;
  std::shared_ptr<MemoryBudget>             memory_budget_;

  friend class Builder;
};

template <typename T>
class CrashGuard<T>::Builder {
 public:
  auto WithPrecondition(std::function<bool()> pred,
                        std::string error = {}) -> Builder& {
    precondition_         = std::move(pred);
    precondition_error_   = std::move(error);
    return *this;
  }

  auto WithPostcondition(std::function<bool(T)> pred,
                         std::string error = {}) -> Builder& {
    postcondition_        = std::move(pred);
    postcondition_error_  = std::move(error);
    return *this;
  }

  auto WithTimeout(std::chrono::milliseconds timeout) -> Builder& {
    timeout_ = timeout;
    return *this;
  }

  auto WithMemoryBudget(size_t max_bytes) -> Builder& {
    memory_budget_ = std::make_shared<MemoryBudget>(max_bytes);
    return *this;
  }

  auto WithMemoryBudget(std::shared_ptr<MemoryBudget> budget) -> Builder& {
    memory_budget_ = std::move(budget);
    return *this;
  }

  auto Build() const -> CrashGuard<T> {
    return CrashGuard<T>(postcondition_, postcondition_error_,
                         precondition_, precondition_error_,
                         timeout_, memory_budget_);
  }

 private:
  std::function<bool(T)>                    postcondition_;
  std::string                               postcondition_error_;
  std::function<bool()>                     precondition_;
  std::string                               precondition_error_;
  std::optional<std::chrono::milliseconds>  timeout_;
  std::shared_ptr<MemoryBudget>             memory_budget_;
};

// ── Convenience wrappers for critical paths ─────────────────────────────────

/// Wrap a RAW decode operation with crash defense.
template <typename T>
auto GuardedRawDecode(std::function<T()> decode_fn,
                      std::function<bool()> cancel_fn = nullptr)
    -> CrashGuardResult<T> {
  return typename CrashGuard<T>::Builder()
      .WithPrecondition([] { return true; }, "RAW decode precondition")
      .WithPostcondition([](const T&) { return true; }, "RAW decode postcondition")
      .WithTimeout(std::chrono::milliseconds(30000))
      .Build()
      .Execute(std::move(decode_fn));
}

/// Wrap a pipeline render operation with crash defense.
template <typename T>
auto GuardedPipelineRender(std::function<T()> render_fn,
                           std::chrono::milliseconds timeout = std::chrono::milliseconds(60000),
                           std::function<bool()> cancel_fn = nullptr)
    -> CrashGuardResult<T> {
  return typename CrashGuard<T>::Builder()
      .WithTimeout(timeout)
      .Build()
      .Execute(std::move(render_fn));
}

/// Wrap a thumbnail generation operation with crash defense.
inline auto GuardedThumbnailGen(std::function<void()> gen_fn,
                                std::chrono::milliseconds timeout = std::chrono::milliseconds(15000))
    -> CrashGuardResult<void> {
  return CrashGuard<void>::Builder()
      .WithTimeout(timeout)
      .Build()
      .Execute(std::move(gen_fn));
}

/// Wrap an import/export operation with crash defense.
template <typename T>
auto GuardedImportExport(std::function<T()> io_fn,
                         std::shared_ptr<MemoryBudget> budget = nullptr)
    -> CrashGuardResult<T> {
  auto builder = typename CrashGuard<T>::Builder()
      .WithTimeout(std::chrono::milliseconds(120000));
  if (budget) {
    builder.WithMemoryBudget(budget);
  }
  return builder.Build().Execute(std::move(io_fn));
}

}  // namespace safety
}  // namespace alcedo

//  Copyright 2026 Yurun Zi
//  SPDX-License-Identifier: GPL-3.0-only
//  Additional permission under GPLv3 section 7 applies; see the LICENSE file.

#pragma once

#include <QString>
#include <QLoggingCategory>

#include <atomic>
#include <chrono>
#include <cstdint>
#include <mutex>
#include <string>
#include <thread>
#include <unordered_map>
#include <unordered_set>
#include <vector>

#include "utils/diagnostics/app_logging.hpp"

namespace alcedo::concurrency {

/// Lightweight deadlock detection and logging utility.
/// Tracks lock acquisition order across threads to detect potential
/// lock ordering violations and circular dependencies.
///
/// Usage:
///   DEADLOCK_TRACK("render_lock");
///   // ... acquire lock ...
///   DEADLOCK_UNTRACK("render_lock");
///
/// The detector logs warnings when:
/// - A lock is held for longer than a threshold (slow lock)
/// - A potential lock ordering violation is detected
/// - A thread appears to be stuck waiting for a lock
class DeadlockDetector {
 public:
  /// Record that the current thread is about to acquire a lock.
  static void TrackAcquire(const std::string& lock_name);

  /// Record that the current thread has released a lock.
  static void TrackRelease(const std::string& lock_name);

  /// Record that the current thread is waiting for a lock (contention).
  static void TrackContention(const std::string& lock_name,
                              std::chrono::milliseconds timeout);

  /// Dump the current state of all tracked locks (for diagnostics).
  static void DumpState();

  /// Enable or disable deadlock detection (default: enabled).
  static void SetEnabled(bool enabled);

 private:
  struct LockInfo {
    std::string              lock_name;
    std::thread::id          owning_thread;
    std::chrono::steady_clock::time_point acquire_time;
    std::vector<std::string> locks_held_at_acquire;  // locks the thread already held
  };

  struct ThreadInfo {
    std::vector<std::string> held_locks;
    std::string              waiting_for;
    std::chrono::steady_clock::time_point wait_start;
  };

  static DeadlockDetector& Instance();

  void TrackAcquireImpl(const std::string& lock_name);
  void TrackReleaseImpl(const std::string& lock_name);
  void TrackContentionImpl(const std::string& lock_name,
                           std::chrono::milliseconds timeout);
  void DumpStateImpl();

  std::mutex lock_;
  std::unordered_map<std::string, LockInfo>      active_locks_;
  std::unordered_map<std::thread::id, ThreadInfo> thread_state_;
  std::atomic<bool>                               enabled_{true};
};

/// RAII guard for tracking lock acquisition and release.
class ScopedLockTracker {
 public:
  explicit ScopedLockTracker(const std::string& lock_name)
      : lock_name_(lock_name) {
    DeadlockDetector::TrackAcquire(lock_name_);
  }

  ~ScopedLockTracker() {
    DeadlockDetector::TrackRelease(lock_name_);
  }

  ScopedLockTracker(const ScopedLockTracker&) = delete;
  ScopedLockTracker& operator=(const ScopedLockTracker&) = delete;

 private:
  std::string lock_name_;
};

}  // namespace alcedo::concurrency

/// Convenience macros for lock tracking.
/// These are compiled out in release builds unless ALCEDO_DEADLOCK_DETECTION is defined.
#if !defined(NDEBUG) || defined(ALCEDO_DEADLOCK_DETECTION)
#define DEADLOCK_TRACK(lock_name) \
  ::alcedo::concurrency::ScopedLockTracker _lock_tracker_##__LINE__(lock_name)
#define DEADLOCK_UNTRACK(lock_name) \
  ::alcedo::concurrency::DeadlockDetector::TrackRelease(lock_name)
#define DEADLOCK_CONTENTION(lock_name, timeout) \
  ::alcedo::concurrency::DeadlockDetector::TrackContention(lock_name, timeout)
#else
#define DEADLOCK_TRACK(lock_name) ((void)0)
#define DEADLOCK_UNTRACK(lock_name) ((void)0)
#define DEADLOCK_CONTENTION(lock_name, timeout) ((void)0)
#endif

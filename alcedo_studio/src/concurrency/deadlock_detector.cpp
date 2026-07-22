//  Copyright 2026 Yurun Zi
//  SPDX-License-Identifier: GPL-3.0-only
//  Additional permission under GPLv3 section 7 applies; see the LICENSE file.

#include "concurrency/deadlock_detector.hpp"

#include <QDebug>
#include <QThread>

#include <algorithm>
#include <sstream>
#include <chrono>

namespace {

// Portable thread-id to integer conversion (std::thread::id is not
// reinterpret_cast-able on all platforms, e.g. macOS libc++)
quintptr ThreadIdToUintptr(std::thread::id tid) {
  return static_cast<quintptr>(std::hash<std::thread::id>{}(tid));
}

} // namespace

namespace alcedo::concurrency {

DeadlockDetector& DeadlockDetector::Instance() {
  static DeadlockDetector instance;
  return instance;
}

void DeadlockDetector::TrackAcquire(const std::string& lock_name) {
  if (Instance().enabled_.load(std::memory_order_relaxed)) {
    Instance().TrackAcquireImpl(lock_name);
  }
}

void DeadlockDetector::TrackRelease(const std::string& lock_name) {
  if (Instance().enabled_.load(std::memory_order_relaxed)) {
    Instance().TrackReleaseImpl(lock_name);
  }
}

void DeadlockDetector::TrackContention(const std::string& lock_name,
                                        std::chrono::milliseconds timeout) {
  if (Instance().enabled_.load(std::memory_order_relaxed)) {
    Instance().TrackContentionImpl(lock_name, timeout);
  }
}

void DeadlockDetector::DumpState() {
  Instance().DumpStateImpl();
}

void DeadlockDetector::SetEnabled(bool enabled) {
  Instance().enabled_.store(enabled, std::memory_order_relaxed);
}

void DeadlockDetector::TrackAcquireImpl(const std::string& lock_name) {
  std::lock_guard<std::mutex> guard(lock_);

  auto tid = std::this_thread::get_id();

  // Record the locks currently held by this thread (for ordering analysis)
  ThreadInfo& thread = thread_state_[tid];

  // Check for potential ordering violations:
  // If this thread already holds locks, record the ordering.
  if (!thread.held_locks.empty()) {
    for (const auto& held_lock : thread.held_locks) {
      // Check if any other thread holds lock_name while waiting for held_lock
      auto it = active_locks_.find(lock_name);
      if (it != active_locks_.end() && it->second.owning_thread != tid) {
        // The other thread holds lock_name. Check if it's also waiting
        // for one of our held locks (circular dependency).
        auto other_thread = thread_state_.find(it->second.owning_thread);
        if (other_thread != thread_state_.end() &&
            !other_thread->second.waiting_for.empty()) {
          for (const auto& our_lock : thread.held_locks) {
            if (other_thread->second.waiting_for == our_lock) {
              qCCritical(diag::pipelineLog).noquote()
                  << QStringLiteral("pipeline.deadlock.circular_dependency "
                                    "POTENTIAL DEADLOCK DETECTED: "
                                    "Thread A holds '%1' and waits for '%2', "
                                    "Thread B holds '%2' and waits for '%3'")
                         .arg(QString::fromStdString(our_lock),
                              QString::fromStdString(lock_name),
                              QString::fromStdString(other_thread->second.waiting_for));
            }
          }
        }
      }
    }
  }

  // Record the lock acquisition
  LockInfo info;
  info.lock_name = lock_name;
  info.owning_thread = tid;
  info.acquire_time = std::chrono::steady_clock::now();
  info.locks_held_at_acquire = thread.held_locks;
  active_locks_[lock_name] = std::move(info);

  thread.held_locks.push_back(lock_name);
  thread.waiting_for.clear();
}

void DeadlockDetector::TrackReleaseImpl(const std::string& lock_name) {
  std::lock_guard<std::mutex> guard(lock_);

  auto tid = std::this_thread::get_id();

  // Check how long the lock was held
  auto it = active_locks_.find(lock_name);
  if (it != active_locks_.end()) {
    auto held_duration = std::chrono::duration_cast<std::chrono::milliseconds>(
        std::chrono::steady_clock::now() - it->second.acquire_time);
    if (held_duration.count() > 5000) {
      qCWarning(diag::pipelineLog).noquote()
          << QStringLiteral("pipeline.deadlock.slow_release "
                            "Lock '%1' held for %2 ms by thread %3. "
                            "Long lock hold times may indicate performance issues.")
                 .arg(QString::fromStdString(lock_name),
                      QString::number(held_duration.count()),
                      QString::number(ThreadIdToUintptr(tid), 16));
    }
    active_locks_.erase(it);
  }

  // Remove from thread's held locks
  auto thread_it = thread_state_.find(tid);
  if (thread_it != thread_state_.end()) {
    auto& held = thread_it->second.held_locks;
    held.erase(std::remove(held.begin(), held.end(), lock_name), held.end());
  }
}

void DeadlockDetector::TrackContentionImpl(const std::string& lock_name,
                                            std::chrono::milliseconds timeout) {
  std::lock_guard<std::mutex> guard(lock_);

  auto tid = std::this_thread::get_id();
  ThreadInfo& thread = thread_state_[tid];
  thread.waiting_for = lock_name;
  thread.wait_start = std::chrono::steady_clock::now();

  qCInfo(diag::pipelineLog).noquote()
      << QStringLiteral("pipeline.deadlock.contention "
                        "Thread %1 waiting for lock '%2' (timeout=%3 ms)")
             .arg(QString::number(ThreadIdToUintptr(tid), 16),
                  QString::fromStdString(lock_name),
                  QString::number(static_cast<qint64>(timeout.count())));
}

void DeadlockDetector::DumpStateImpl() {
  std::lock_guard<std::mutex> guard(lock_);

  qCInfo(diag::pipelineLog).noquote()
      << QStringLiteral("pipeline.deadlock.dump === Deadlock Detector State ===");

  qCInfo(diag::pipelineLog).noquote()
      << QStringLiteral("pipeline.deadlock.dump Active locks: %1")
             .arg(static_cast<int>(active_locks_.size()));

  for (const auto& [name, info] : active_locks_) {
    auto held_ms = std::chrono::duration_cast<std::chrono::milliseconds>(
        std::chrono::steady_clock::now() - info.acquire_time);
    qCInfo(diag::pipelineLog).noquote()
        << QStringLiteral("pipeline.deadlock.dump   Lock '%1' held by thread %2 for %3 ms")
               .arg(QString::fromStdString(name),
                    QString::number(ThreadIdToUintptr(info.owning_thread), 16),
                    QString::number(held_ms.count()));
  }

  qCInfo(diag::pipelineLog).noquote()
      << QStringLiteral("pipeline.deadlock.dump Thread states: %1")
             .arg(static_cast<int>(thread_state_.size()));

  for (const auto& [tid, info] : thread_state_) {
    std::ostringstream locks;
    for (size_t i = 0; i < info.held_locks.size(); ++i) {
      if (i > 0) locks << ", ";
      locks << info.held_locks[i];
    }
    qCInfo(diag::pipelineLog).noquote()
        << QStringLiteral("pipeline.deadlock.dump   Thread %1: held=[%2] waiting='%3'")
               .arg(QString::number(ThreadIdToUintptr(tid), 16),
                    QString::fromStdString(locks.str()),
                    QString::fromStdString(info.waiting_for));
  }
}

}  // namespace alcedo::concurrency

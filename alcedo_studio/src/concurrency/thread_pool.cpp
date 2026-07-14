//  Copyright 2025 Yurun Zi
//  SPDX-License-Identifier: GPL-3.0-only
//  Additional permission under GPLv3 section 7 applies; see the LICENSE file.

#include "concurrency/thread_pool.hpp"

#include <algorithm>
#include <future>
#include <memory>
#include <mutex>
#include <optional>
#include <vector>

#ifdef HAVE_METAL
#include <alcedo/metal/Metal.hpp>
#endif

namespace alcedo {
ThreadPool::ThreadPool(size_t thread_count)
    : tasks_(kQueueCapacity), stop_(false) {
  for (size_t i = 0; i < thread_count; ++i) {
    workers_.emplace_back(&ThreadPool::WorkerThread, this);
  }
  ALCEDO_LOG_DEBUG("ThreadPool: started with {} worker threads (MPMS queue capacity={})",
                   thread_count, kQueueCapacity);
}

ThreadPool::~ThreadPool() {
  stop_.store(true, std::memory_order_release);
  wake_cv_.notify_all();
  for (std::thread& worker : workers_) {
    // Guard with joinable(): owners that called Shutdown() first have already
    // joined these workers (making them non-joinable). An unguarded join() on a
    // non-joinable thread throws std::system_error(_Not_joinable); since this
    // destructor is implicitly noexcept, that throw terminates the process.
    if (worker.joinable()) {
      worker.join();
    }
  }
}

void ThreadPool::Shutdown() {
  stop_.store(true, std::memory_order_release);

  // Drain the MPMC queue, discarding any tasks that have not started yet.
  // They capture state that may be destroyed before this pool's owner; running
  // them during teardown is the use-after-free we are avoiding. In-flight
  // tasks (already popped by a worker) are unaffected and finish on their own.
  while (tasks_.pop().has_value()) {
    // discard
  }

  wake_cv_.notify_all();
  for (std::thread& worker : workers_) {
    if (worker.joinable()) {
      worker.join();
    }
  }
  ALCEDO_LOG_DEBUG("ThreadPool: shutdown complete, all workers joined");
}

void ThreadPool::Submit(std::function<void()> task) {
  if (stop_.load(std::memory_order_acquire)) {
    ALCEDO_LOG_WARN("ThreadPool::Submit: task submitted after stop, discarding");
    return;
  }
  tasks_.push(std::move(task));
  wake_cv_.notify_one();
}

void ThreadPool::WorkerThread() {
  while (true) {
    // Try a lock-free pop first — this is the fast path when the queue is
    // non-empty and avoids mutex contention entirely.
    std::optional<std::function<void()>> opt_task = tasks_.pop();
    if (opt_task.has_value()) {
#ifdef HAVE_METAL
      auto autorelease_pool = NS::TransferPtr(NS::AutoreleasePool::alloc()->init());
#endif
      opt_task.value()();
      continue;
    }

    // Queue was empty — wait for a signal (new task or stop).
    {
      std::unique_lock<std::mutex> lock(wake_mtx_);
      wake_cv_.wait(lock, [this] {
        return stop_.load(std::memory_order_acquire) || !tasks_.empty();
      });
    }

    if (stop_.load(std::memory_order_acquire)) {
      // Drain remaining tasks before exiting only if this is NOT a Shutdown()
      // path. During Shutdown(), the caller already drained the queue. During
      // normal destructor path, we run remaining tasks for safety.
      // However, both paths set stop_ and join, so workers should just exit.
      return;
    }

    // We were woken up because a task was pushed; loop back to pop it.
  }
}

};  // namespace alcedo

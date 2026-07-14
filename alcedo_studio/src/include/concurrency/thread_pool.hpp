//  Copyright 2025 Yurun Zi
//  SPDX-License-Identifier: GPL-3.0-only
//  Additional permission under GPLv3 section 7 applies; see the LICENSE file.

#include <atomic>
#include <cstddef>
#include <cstdint>
#include <functional>
#include <future>
#include <memory>
#include <type_traits>
#include <vector>

#include "type/type.hpp"
#include "utils/diagnostics/app_logging.hpp"
#include "utils/queue/queue.hpp"

#pragma once

namespace alcedo {
class ThreadPool {
 public:
  ThreadPool(size_t thread_count);
  ~ThreadPool();

  void Submit(std::function<void()> task);

  // Stop the pool, discarding any queued tasks that have not started yet, and
  // join all worker threads. In-flight tasks run to completion. Use this when
  // owned state the queued tasks capture is about to be destroyed: the default
  // destructor drains the queue (runs every queued task), which is unsafe if
  // the captured state is destroyed before the pool (member destruction is
  // reverse-declaration order). Calling Shutdown() first empties the queue
  // under the lock and joins the workers, so the subsequent destructor is a
  // safe no-op — the destructor guards each join with joinable() and therefore
  // tolerates workers that Shutdown() already joined.
  void Shutdown();

  template <typename F>
  void Submit(F&& task) {
    using TaskT = std::decay_t<F>;
    static_assert(std::is_copy_constructible_v<TaskT>,
                  "ThreadPool::Submit requires a copy-constructible task when using std::function."
                  " Wrap move-only state in std::shared_ptr or provide a copyable callable.");
    Submit(std::function<void()>(std::forward<F>(task)));
  }

 private:
  static constexpr size_t kQueueCapacity = 4096;

  // MPMS ring-buffer queue — producers push lock-free, consumers pop lock-free.
  // A separate condition variable + mutex are used only for worker wake-up and
  // stop-signal coordination, not for queue access itself.
  LockFreeMPMCQueue<std::function<void()>> tasks_;

  std::mutex                              wake_mtx_;
  std::condition_variable                 wake_cv_;
  std::vector<std::thread>                workers_;

  std::atomic<bool>                       stop_;

  void                                    WorkerThread();
};
};  // namespace alcedo

//  Copyright 2025 Yurun Zi
//  SPDX-License-Identifier: GPL-3.0-only
//  Additional permission under GPLv3 section 7 applies; see the LICENSE file.

// TODO: Change tasks to MPMS Queue to improve efficiency

#include <cstddef>
#include <cstdint>
#include <functional>
#include <future>
#include <memory>
#include <queue>
#include <type_traits>
#include <vector>

#include "type/type.hpp"
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
  // under the lock so the subsequent destructor drain is a no-op.
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
  std::queue<std::function<void()>> tasks_;
  std::mutex                        mtx_;
  std::condition_variable           condition_;
  std::vector<std::thread>          workers_;

  bool                              stop_;

  void                              WorkerThread();
};
};  // namespace alcedo

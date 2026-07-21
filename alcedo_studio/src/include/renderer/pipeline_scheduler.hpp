//  Copyright 2025 Yurun Zi
//  SPDX-License-Identifier: GPL-3.0-only
//  Additional permission under GPLv3 section 7 applies; see the LICENSE file.

#pragma once

#include <cstdint>
#include <mutex>
#include <vector>

#include "concurrency/thread_pool.hpp"
#include "pipeline_task.hpp"
#include "utils/id/id_generator.hpp"

namespace alcedo {
class PipelineScheduler {
 private:
  IncrID::IDGenerator<uint32_t> id_generator_{0};

  std::mutex                    scheduler_lock_;
  ThreadPool thread_pool_;  // use thred pool for now, can be changed to task scheduler later

  // ── Integration-6: Pending tasks queue for back-pressure ──
  // When MemoryBudgetManager reports insufficient memory, tasks are
  // queued here instead of being submitted to the thread pool. They
  // can be flushed later when memory becomes available.
  std::vector<PipelineTask> pending_tasks_;

  
 public:
  explicit PipelineScheduler();
  explicit PipelineScheduler(size_t thread_count);

  /**
   * @brief Schedule a pipeline task
   *
   * @param task
   */
  void ScheduleTask(PipelineTask&& task);

  /**
   * @brief Attempt to submit queued tasks that were held back due to
   *        memory back-pressure. Called when budget is released.
   */
  void TryFlushPendingTasks();
};
};  // namespace alcedo
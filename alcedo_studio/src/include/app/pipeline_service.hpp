//  Copyright 2026 Yurun Zi
//  SPDX-License-Identifier: GPL-3.0-only
//  Additional permission under GPLv3 section 7 applies; see the LICENSE file.

#pragma once

#include <functional>
#include <memory>
#include <span>
#include <unordered_map>

#include "app/image_pool_service.hpp"
#include "edit/pipeline/pipeline_accelerator.hpp"
#include "edit/pipeline/pipeline.hpp"
#include "json.hpp"
#include "renderer/pipeline_scheduler.hpp"
#include "sleeve/storage_service.hpp"
#include "type/type.hpp"
#include "utils/cache/lru_cache.hpp"

namespace alcedo {

struct PipelineGuard {
  std::shared_ptr<CPUPipelineExecutor> pipeline_;
  sl_element_id_t                      id_;
  bool                                 dirty_  = false;
  bool                                 pinned_ = false;
  size_t                               pin_count_ = 0;
};

// Phase 3: a read-only clone of a pipeline's params captured into an independent
// executor, used for background analysis rendering. The snapshot never pins the
// live PipelineGuard, never writes storage, and never clears the live guard's
// dirty state. Rendering on `executor_` does not affect the live pipeline.
struct PipelineSnapshot {
  sl_element_id_t                      element_id_ = 0;
  image_id_t                           image_id_   = 0;
  nlohmann::json                       pipeline_params_;
  std::shared_ptr<CPUPipelineExecutor> executor_;
};

class PipelineMgmtService final {
 private:
  std::shared_ptr<StorageService>                                     storage_service_;

  LRUCache<sl_element_id_t, sl_element_id_t>                          pipeline_cache_;

  std::unordered_map<sl_element_id_t, std::shared_ptr<PipelineGuard>> loaded_pipelines_;

  std::mutex                                                          lock_;

  static constexpr size_t                                             default_cache_capacity_ = 16;

  AcceleratorBackendPreference accelerator_preference_ = AcceleratorBackendPreference::Auto;

  void HandleEviction(sl_element_id_t evicted_id);

 public:
  PipelineMgmtService() = delete;
  explicit PipelineMgmtService(std::shared_ptr<StorageService> storage_service)
      : storage_service_(storage_service),
        pipeline_cache_(default_cache_capacity_),
        loaded_pipelines_() {}

  void SavePipeline(std::shared_ptr<PipelineGuard> pipeline);

  auto LoadPipeline(sl_element_id_t id) -> std::shared_ptr<PipelineGuard>;

  void DeletePipeline(sl_element_id_t id);
  void DeletePipelines(std::span<const sl_element_id_t> ids);

  void SetAcceleratorBackendPreference(AcceleratorBackendPreference preference);
  [[nodiscard]] auto GetAcceleratorBackendPreference() const -> AcceleratorBackendPreference {
    return accelerator_preference_;
  }

  void Sync();

  // Phase 3: capture a read-only snapshot of the current pipeline state without
  // pinning the live guard, forcing it to disk, or touching dirty state. The
  // returned executor is an independent clone; rendering on it does not affect the
  // live pipeline. May briefly block on the live executor's render lock
  // (serializes with an in-flight editor render on the same executor). Returns
  // nullptr and writes *error on failure.
  auto LoadPipelineSnapshot(sl_element_id_t id, image_id_t image_id,
                           std::string* error) -> std::shared_ptr<PipelineSnapshot>;

  // Release the snapshot executor's intermediate buffers (mirrors SavePipeline's
  // last-pin cleanup). Safe to call from the snapshot's task callback; the
  // shared_ptr then drops naturally. Not a storage write. No-op if null.
  void ReleasePipelineSnapshot(std::shared_ptr<PipelineSnapshot> snapshot);
};
}  // namespace alcedo

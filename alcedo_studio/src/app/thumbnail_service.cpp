//  Copyright 2026 Yurun Zi
//  SPDX-License-Identifier: GPL-3.0-only
//  Additional permission under GPLv3 section 7 applies; see the LICENSE file.

#include "app/thumbnail_service.hpp"

#include <algorithm>
#include <atomic>
#include <cstdint>
#include <format>
#include <iterator>
#include <memory>
#include <mutex>
#include <optional>
#include <stdexcept>
#include <unordered_map>
#include <vector>

#include "app/pipeline_service.hpp"
#include "app/render_service.hpp"
#include "json.hpp"
#include "renderer/pipeline_task.hpp"

namespace alcedo {
namespace {

// Map ThumbnailResolution to the max_edge value for SetRenderRes.
constexpr uint32_t ResolutionToMaxEdge(ThumbnailResolution res) {
  return static_cast<uint32_t>(res);
}

// Map ThumbnailResolution to the appropriate DecodeRes for RAW decoding.
constexpr DecodeRes ResolutionToDecodeRes(ThumbnailResolution res) {
  switch (res) {
    case ThumbnailResolution::k256:  return DecodeRes::EIGHTH;
    case ThumbnailResolution::k512:  return DecodeRes::QUARTER;
    case ThumbnailResolution::k1024: return DecodeRes::QUARTER;
    case ThumbnailResolution::k2048: return DecodeRes::HALF;
  }
  return DecodeRes::QUARTER;
}

void DispatchThumbnailCallback(const ThumbnailCallback&           callback,
                               const CallbackDispatcher&          dispatcher,
                               const std::shared_ptr<ThumbnailGuard>& guard) {
  if (!callback) {
    return;
  }

  try {
    if (dispatcher) {
      dispatcher([callback, guard]() { callback(guard); });
    } else {
      callback(guard);
    }
  } catch (...) {
  }
}

auto IsRenderableThumbnailResult(const ImageBuffer& result_buffer) -> bool {
  return result_buffer.buffer_valid_ || result_buffer.cpu_data_valid_ || result_buffer.gpu_data_valid_;
}

auto ReadColorTempOperatorParams(const std::shared_ptr<PipelineGuard>& pipeline)
    -> std::optional<nlohmann::json> {
  if (!pipeline || !pipeline->pipeline_) {
    return std::nullopt;
  }

  auto& to_ws_stage = pipeline->pipeline_->GetStage(PipelineStageName::To_WorkingSpace);
  auto  color_temp_entry = to_ws_stage.GetOperator(OperatorType::COLOR_TEMP);
  if (!color_temp_entry.has_value() || !color_temp_entry.value() || !color_temp_entry.value()->op_) {
    return std::nullopt;
  }

  return color_temp_entry.value()->op_->GetParams();
}
}  // namespace

struct ThumbnailService::State {
  static constexpr size_t                    default_cache_size_ = 64;

  struct PendingCallback {
    ThumbnailCallback  callback_{};
    CallbackDispatcher dispatcher_{};
  };

  std::shared_ptr<SleeveServiceImpl>         sleeve_service_     = nullptr;
  std::shared_ptr<ImagePoolService>          image_pool_service_ = nullptr;
  std::shared_ptr<PipelineMgmtService>       pipeline_service_   = nullptr;

  std::mutex                                 cache_lock_;

  // LRU keyed by composite {element_id, resolution_tier}.
  LRUCache<ThumbnailCacheKey, ThumbnailCacheKey> thumbnail_cache_;
  std::unordered_map<ThumbnailCacheKey, std::shared_ptr<ThumbnailGuard>> thumbnail_cache_data_{};
  std::unordered_map<ThumbnailCacheKey, std::vector<PendingCallback>>    pending_{};

  // Generation tokens for Strategy A (pre-flight cancellation).
  // Each element has a shared atomic that queued tasks check before executing.
  // Incrementing the token invalidates all queued tasks for that element.
  std::unordered_map<sl_element_id_t, std::shared_ptr<std::atomic<uint64_t>>> generation_tokens_{};

  // Pipeline scheduler (global/shared), must outlive tasks.
  std::shared_ptr<PipelineScheduler> pipeline_scheduler_ = nullptr;

  State(std::shared_ptr<SleeveServiceImpl> sleeve_service,
        std::shared_ptr<ImagePoolService> image_pool_service,
        std::shared_ptr<PipelineMgmtService> pipeline_service)
      : sleeve_service_(std::move(sleeve_service)),
        image_pool_service_(std::move(image_pool_service)),
        pipeline_service_(std::move(pipeline_service)),
        thumbnail_cache_(default_cache_size_) {
    pipeline_scheduler_ = RenderService::GetThumbnailOrExportScheduler();
  }

  // Get or create a generation token for the given element.
  auto GetOrCreateGenerationToken(sl_element_id_t element_id)
      -> std::shared_ptr<std::atomic<uint64_t>> {
    auto it = generation_tokens_.find(element_id);
    if (it != generation_tokens_.end() && it->second) {
      return it->second;
    }
    auto token = std::make_shared<std::atomic<uint64_t>>(0);
    generation_tokens_[element_id] = token;
    return token;
  }
};

ThumbnailService::ThumbnailService(std::shared_ptr<SleeveServiceImpl>   sleeve_service,
                                   std::shared_ptr<ImagePoolService>    image_pool_service,
                                   std::shared_ptr<PipelineMgmtService> pipeline_service)
    : state_(std::make_shared<State>(std::move(sleeve_service),
                                     std::move(image_pool_service),
                                     std::move(pipeline_service))) {}

void ThumbnailService::GetThumbnail(sl_element_id_t id, image_id_t image_id,
                                    ThumbnailCallback callback, bool pin_if_found,
                                    CallbackDispatcher dispatcher,
                                    ThumbnailResolution resolution) {
  auto st = state_;
  if (!st || !st->image_pool_service_ || !st->pipeline_service_ || !st->pipeline_scheduler_) {
    throw std::runtime_error("[ERROR] ThumbnailService: Services not initialized.");
  }

  const ThumbnailCacheKey cache_key{id, resolution};

  std::shared_ptr<ThumbnailGuard> guard;
  {
    std::unique_lock lock(st->cache_lock_);
    if (st->thumbnail_cache_.Contains(cache_key)) {
      auto guard_it = st->thumbnail_cache_data_.find(cache_key);
      if (guard_it != st->thumbnail_cache_data_.end() && guard_it->second) {
        guard = guard_it->second;
        if (pin_if_found) {
          guard->pin_count_++;
        }
      } else {
        st->thumbnail_cache_.RemoveRecord(cache_key);
      }
    }
  }

  if (guard) {
    DispatchThumbnailCallback(callback, dispatcher, guard);
    return;
  }

  std::shared_ptr<std::atomic<uint64_t>> gen_token;
  uint64_t                               expected_gen = 0;
  {
    std::unique_lock lock(st->cache_lock_);
    auto it = st->pending_.find(cache_key);
    if (it != st->pending_.end()) {
      State::PendingCallback pending_cb{};
      pending_cb.callback_   = std::move(callback);
      pending_cb.dispatcher_ = std::move(dispatcher);
      it->second.push_back(std::move(pending_cb));
      return;
    }
    State::PendingCallback pending_cb{};
    pending_cb.callback_   = std::move(callback);
    pending_cb.dispatcher_ = std::move(dispatcher);
    std::vector<State::PendingCallback> pending_callbacks;
    pending_callbacks.push_back(std::move(pending_cb));
    st->pending_.emplace(cache_key, std::move(pending_callbacks));

    gen_token    = st->GetOrCreateGenerationToken(id);
    expected_gen = gen_token->load();
  }

  auto fail_pending_request = [&](const std::string&               message,
                                  const std::shared_ptr<PipelineGuard>& pipeline) -> void {
    std::vector<State::PendingCallback> callbacks;
    {
      std::unique_lock lock(st->cache_lock_);
      auto             it = st->pending_.find(cache_key);
      if (it != st->pending_.end()) {
        callbacks = std::move(it->second);
        st->pending_.erase(it);
      }
      st->thumbnail_cache_.RemoveRecord(cache_key);
      st->thumbnail_cache_data_.erase(cache_key);
    }

    if (pipeline) {
      try {
        st->pipeline_service_->SavePipeline(pipeline);
      } catch (...) {
      }
    }

    for (const auto& pending_cb : callbacks) {
      DispatchThumbnailCallback(pending_cb.callback_, pending_cb.dispatcher_, nullptr);
    }

    throw std::runtime_error(message);
  };

  std::shared_ptr<PipelineGuard> pipeline;
  try {
    pipeline = st->pipeline_service_->LoadPipeline(id);
  } catch (const std::exception& e) {
    fail_pending_request(
        std::format("[ERROR] ThumbnailService: Failed to load pipeline for file ID {}: {}", id,
                    e.what()),
        nullptr);
  } catch (...) {
    fail_pending_request(
        std::format(
            "[ERROR] ThumbnailService: Failed to load pipeline for file ID {}: unknown error.", id),
        nullptr);
  }

  if (!pipeline || !pipeline->pipeline_) {
    fail_pending_request(
        std::format("[ERROR] ThumbnailService: Pipeline for file ID {} not available.", id),
        nullptr);
  }

  pipeline->pipeline_->SetForceCPUOutput(true);

  std::shared_ptr<Image> img_result;
  try {
    img_result = st->image_pool_service_->Read<std::shared_ptr<Image>>(
        image_id, [](const std::shared_ptr<Image>& img) { return img; });
  } catch (const std::exception& e) {
    fail_pending_request(
        std::format("[ERROR] ThumbnailService: Failed to load image ID {} for element {}: {}",
                    image_id, id, e.what()),
        pipeline);
  } catch (...) {
    fail_pending_request(
        std::format(
            "[ERROR] ThumbnailService: Failed to load image ID {} for element {}: unknown error.",
            image_id, id),
        pipeline);
  }

  if (!img_result) {
    fail_pending_request(
        std::format("[ERROR] ThumbnailService: Image with ID {} not found in pool.", image_id),
        pipeline);
  }

  const uint32_t max_edge  = ResolutionToMaxEdge(resolution);
  const DecodeRes decode_res = ResolutionToDecodeRes(resolution);

  PipelineTask thumb_task;
  thumb_task.pipeline_executor_                 = pipeline->pipeline_;
  thumb_task.input_desc_                        = std::move(img_result);
  thumb_task.options_.render_desc_.render_type_ = RenderType::THUMBNAIL;
  thumb_task.options_.render_desc_.max_edge_    = max_edge;
  thumb_task.options_.render_desc_.decode_res_  = decode_res;
  thumb_task.options_.is_blocking_              = false;
  thumb_task.options_.is_callback_              = true;
  thumb_task.options_.is_seq_callback_          = false;
  thumb_task.cancel_requested_ = [gen_token, expected_gen]() {
    return gen_token && gen_token->load() != expected_gen;
  };

  const auto pre_render_color_temp_params = ReadColorTempOperatorParams(pipeline);

  thumb_task.callback_ = [st, id, cache_key, pipeline, pre_render_color_temp_params,
                          gen_token, expected_gen](ImageBuffer& result_buffer) {
    // Strategy A: stale tasks must not touch pending_ because a newer request
    // for the same element/resolution may already have claimed that slot.
    if (gen_token && gen_token->load() != expected_gen) {
      return;
    }

    std::shared_ptr<ThumbnailGuard>      guard;
    std::vector<State::PendingCallback> callbacks;

    {
      std::unique_lock lock(st->cache_lock_);

      if (gen_token && gen_token->load() != expected_gen) {
        return;
      }

      auto             pending_it = st->pending_.find(cache_key);
      const bool       request_active = (pending_it != st->pending_.end());
      if (request_active) {
        callbacks = std::move(pending_it->second);
        st->pending_.erase(pending_it);
      }

      const bool valid_result = IsRenderableThumbnailResult(result_buffer);
      if (request_active && valid_result) {
        guard                   = std::make_shared<ThumbnailGuard>();
        guard->thumbnail_buffer_ = std::make_unique<ImageBuffer>(std::move(result_buffer));
        guard->pin_count_        = 1;

        auto evicted = st->thumbnail_cache_.RecordAccess_WithEvict(cache_key, cache_key);
        HandleEvict(*st, evicted);
        st->thumbnail_cache_data_[cache_key] = guard;
      } else {
        st->thumbnail_cache_.RemoveRecord(cache_key);
        st->thumbnail_cache_data_.erase(cache_key);
      }
    }

    // Strategy A: re-check token after pipeline work (before callbacks).
    // If cancelled mid-render, remove only the guard inserted by this task.
    if (gen_token && gen_token->load() != expected_gen) {
      if (guard) {
        std::unique_lock lock(st->cache_lock_);
        auto guard_it = st->thumbnail_cache_data_.find(cache_key);
        if (guard_it != st->thumbnail_cache_data_.end() && guard_it->second == guard) {
          st->thumbnail_cache_.RemoveRecord(cache_key);
          st->thumbnail_cache_data_.erase(guard_it);
        }
      }
      for (const auto& pending_cb : callbacks) {
        DispatchThumbnailCallback(pending_cb.callback_, pending_cb.dispatcher_, nullptr);
      }
      return;
    }

    const auto post_render_color_temp_params = ReadColorTempOperatorParams(pipeline);
    if (post_render_color_temp_params != pre_render_color_temp_params) {
      pipeline->dirty_ = true;
    }

    try {
      st->pipeline_service_->SavePipeline(pipeline);
    } catch (...) {
    }

    for (const auto& pending_cb : callbacks) {
      DispatchThumbnailCallback(pending_cb.callback_, pending_cb.dispatcher_, guard);
    }
  };

  try {
    st->pipeline_scheduler_->ScheduleTask(std::move(thumb_task));
  } catch (const std::exception& e) {
    fail_pending_request(
        std::format("[ERROR] ThumbnailService: Failed to schedule thumbnail for element {}: {}", id,
                    e.what()),
        pipeline);
  } catch (...) {
    fail_pending_request(
        std::format(
            "[ERROR] ThumbnailService: Failed to schedule thumbnail for element {}: unknown error.",
            id),
        pipeline);
  }
}

void ThumbnailService::CancelPending(sl_element_id_t sleeve_element_id) {
  auto st = state_;
  if (!st) {
    return;
  }

  std::vector<State::PendingCallback> callbacks_to_dispatch;
  {
    std::unique_lock lock(st->cache_lock_);

    // Increment the generation token — all queued tasks for this element
    // will see the mismatch and skip execution (Strategy A).
    auto token_it = st->generation_tokens_.find(sleeve_element_id);
    if (token_it != st->generation_tokens_.end() && token_it->second) {
      token_it->second->fetch_add(1);
    } else {
      auto token = std::make_shared<std::atomic<uint64_t>>(1);  // start at 1 so 0 != 1
      st->generation_tokens_[sleeve_element_id] = token;
    }

    // Remove all pending callbacks that existed for this element at cancel time.
    for (auto res : {ThumbnailResolution::k256, ThumbnailResolution::k512,
                     ThumbnailResolution::k1024, ThumbnailResolution::k2048}) {
      ThumbnailCacheKey key{sleeve_element_id, res};
      auto it = st->pending_.find(key);
      if (it != st->pending_.end()) {
        auto callbacks = std::move(it->second);
        st->pending_.erase(it);
        callbacks_to_dispatch.insert(callbacks_to_dispatch.end(),
                                     std::make_move_iterator(callbacks.begin()),
                                     std::make_move_iterator(callbacks.end()));
      }
    }
  }

  for (const auto& cb : callbacks_to_dispatch) {
    DispatchThumbnailCallback(cb.callback_, cb.dispatcher_, nullptr);
  }
}

void ThumbnailService::ReleaseThumbnail(sl_element_id_t sleeve_element_id) {
  auto st = state_;
  if (!st) {
    return;
  }

  // Increment generation token to invalidate queued tasks (Strategy A).
  CancelPending(sleeve_element_id);

  std::unique_lock lock(st->cache_lock_);

  // Release pins for all resolution tiers of this element.
  for (auto res : {ThumbnailResolution::k256, ThumbnailResolution::k512,
                   ThumbnailResolution::k1024, ThumbnailResolution::k2048}) {
    ThumbnailCacheKey key{sleeve_element_id, res};
    auto it = st->thumbnail_cache_data_.find(key);
    if (it == st->thumbnail_cache_data_.end() || !it->second) {
      st->thumbnail_cache_.RemoveRecord(key);
      continue;
    }

    auto guard = it->second;
    if (guard->pin_count_ > 0) {
      guard->pin_count_--;
    }

    if (guard->pin_count_ == 0) {
      st->thumbnail_cache_.RemoveRecord(key);
      st->thumbnail_cache_data_.erase(it);
    }
  }
}

void ThumbnailService::InvalidateThumbnail(sl_element_id_t sleeve_element_id) {
  auto st = state_;
  if (!st) {
    return;
  }

  std::unique_lock lock(st->cache_lock_);

  // Invalidate all resolution tiers for this element.
  for (auto res : {ThumbnailResolution::k256, ThumbnailResolution::k512,
                   ThumbnailResolution::k1024, ThumbnailResolution::k2048}) {
    ThumbnailCacheKey key{sleeve_element_id, res};
    st->pending_.erase(key);
    st->thumbnail_cache_.RemoveRecord(key);
    st->thumbnail_cache_data_.erase(key);
  }
}

void ThumbnailService::ResizeCache(uint32_t desired_capacity) {
  auto st = state_;
  if (!st) {
    return;
  }

  std::unique_lock lock(st->cache_lock_);

  // Clamp to reasonable bounds.
  constexpr uint32_t kMinCacheSize = 32;
  constexpr uint32_t kMaxCacheSize = 1024;
  const uint32_t capacity = std::clamp(desired_capacity, kMinCacheSize, kMaxCacheSize);

  // Only shrink if all currently-cached entries are unpinned.
  // If pinned items would exceed capacity, keep current size.
  uint32_t pinned_count = 0;
  for (const auto& [key, guard] : st->thumbnail_cache_data_) {
    if (guard && guard->pin_count_ > 0) {
      pinned_count++;
    }
  }

  const uint32_t effective_capacity = std::max(capacity, pinned_count);
  st->thumbnail_cache_.Resize(effective_capacity);
}

void ThumbnailService::HandleEvict(State& st, std::optional<ThumbnailCacheKey> evicted_key) {
  if (evicted_key.has_value()) {
    const auto& key = evicted_key.value();
    auto it = st.thumbnail_cache_data_.find(key);
    if (it != st.thumbnail_cache_data_.end() && it->second) {
      auto guard = it->second;
      if (guard->pin_count_ <= 0) {
        st.thumbnail_cache_data_.erase(it);
      } else {
        // Re-insert into cache since it's still pinned.
        // Boost the cache size to avoid immediate eviction.
        st.thumbnail_cache_.Resize(static_cast<uint32_t>(st.thumbnail_cache_data_.size() + 5));
        st.thumbnail_cache_.RecordAccess(key, key);
      }
    }
  } else {
    // No eviction happened, check cache size.
    if (st.thumbnail_cache_data_.size() > State::default_cache_size_) {
      st.thumbnail_cache_.Resize(static_cast<uint32_t>(st.thumbnail_cache_data_.size() - 1));
    }
  }
}
};  // namespace alcedo

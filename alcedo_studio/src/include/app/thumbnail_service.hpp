//  Copyright 2026 Yurun Zi
//  SPDX-License-Identifier: GPL-3.0-only
//  Additional permission under GPLv3 section 7 applies; see the LICENSE file.

#pragma once

#include <cstdint>
#include <functional>
#include <memory>
#include <optional>

#include "app/image_pool_service.hpp"
#include "app/pipeline_service.hpp"
#include "app/sleeve_service.hpp"
#include "image/image_buffer.hpp"
#include "type/type.hpp"

namespace alcedo {

// Resolution tiers for thumbnail requests. Values are the max-edge pixel size.
// These are fixed tiers to simplify cache management and memory alignment.
enum class ThumbnailResolution : uint32_t {
  k256  = 256,
  k512  = 512,
  k1024 = 1024,
  k2048 = 2048,
};

// Composite cache key: element + resolution tier.
// Different resolutions of the same element are independent cache entries.
struct ThumbnailCacheKey {
  sl_element_id_t    element_id     = 0;
  ThumbnailResolution resolution     = ThumbnailResolution::k1024;

  bool operator==(const ThumbnailCacheKey& other) const = default;
};

}  // namespace alcedo

// std::hash specialization for ThumbnailCacheKey
template <>
struct std::hash<alcedo::ThumbnailCacheKey> {
  size_t operator()(const alcedo::ThumbnailCacheKey& key) const noexcept {
    // Combine element_id (32-bit via macro sl_element_id_t) and resolution (32-bit).
    const auto h1 = std::hash<std::uint32_t>{}(key.element_id);
    const auto h2 = std::hash<std::uint32_t>{}(static_cast<std::uint32_t>(key.resolution));
    return h1 ^ (h2 + 0x9e3779b9 + (h1 << 6) + (h1 >> 2));
  }
};

namespace alcedo {

struct ThumbnailGuard {
  std::unique_ptr<ImageBuffer> thumbnail_buffer_ = nullptr;
  int                          pin_count_        = 0;

  ThumbnailGuard()  = default;
  ~ThumbnailGuard() = default;

  // Non-copyable
  ThumbnailGuard(const ThumbnailGuard&)            = delete;
  ThumbnailGuard& operator=(const ThumbnailGuard&) = delete;

  // Movable
  ThumbnailGuard(ThumbnailGuard&&)            = default;
  ThumbnailGuard& operator=(ThumbnailGuard&&) = default;
};

enum class ThumbnailRequestStatus {
  kReady,
  kCanceled,
  kError,
};

struct ThumbnailRequestResult {
  std::shared_ptr<ThumbnailGuard> guard{};
  ThumbnailRequestStatus          status = ThumbnailRequestStatus::kError;
  std::string                     message{};
  ThumbnailCacheKey               key{};
};

using ThumbnailCallback  = std::function<void(std::shared_ptr<ThumbnailGuard>)>;
using ThumbnailResultCallback = std::function<void(ThumbnailRequestResult)>;
using CallbackDispatcher = std::function<void(std::function<void()>)>;

class ThumbnailService {
 private:
  struct State;
  std::shared_ptr<State> state_;

  static void            HandleEvict(State& st, std::optional<ThumbnailCacheKey> evicted_key);

 public:
  ThumbnailService() = delete;
  ThumbnailService(std::shared_ptr<SleeveServiceImpl>   sleeve_service,
                   std::shared_ptr<ImagePoolService>    image_pool_service,
                   std::shared_ptr<PipelineMgmtService> pipeline_service);
  ~ThumbnailService() = default;

  // Request a thumbnail for the given element/image pair.
  // resolution selects the desired fixed tier (256, 512, 1024, 2048).
  void GetThumbnail(sl_element_id_t id, image_id_t image_id,
                    ThumbnailCallback callback, bool pin_if_found = true,
                    CallbackDispatcher dispatcher = nullptr,
                    ThumbnailResolution resolution = ThumbnailResolution::k1024);

  // Request a thumbnail and receive a detailed result. This distinguishes
  // cancellation from render/load failures, while GetThumbnail preserves the
  // legacy guard/null callback contract.
  void GetThumbnailDetailed(sl_element_id_t id, image_id_t image_id,
                            ThumbnailResultCallback callback, bool pin_if_found = true,
                            CallbackDispatcher dispatcher = nullptr,
                            ThumbnailResolution resolution = ThumbnailResolution::k1024);

  // Cancel a pending thumbnail request for one element/resolution key.
  // Also increments the key generation token so queued tasks skip execution.
  void CancelPending(const ThumbnailCacheKey& key);

  // Cancel all pending thumbnail requests for the given element at all resolutions.
  // Use this only for content-level invalidation, deletion, or full element teardown.
  void CancelPending(sl_element_id_t sleeve_element_id);

  // Force the cached thumbnail for this sleeve element to be discarded.
  // Next GetThumbnail() will re-render via pipeline.
  void InvalidateThumbnail(sl_element_id_t sleeve_element_id);

  // Release a cached/pending thumbnail for one element/resolution key.
  void ReleaseThumbnail(const ThumbnailCacheKey& key);

  // Release all resolution tiers for an element.
  // Use this only for full element teardown or legacy callers.
  void ReleaseThumbnail(sl_element_id_t sleeve_element_id);

  // Proactively resize the cache to the desired capacity.
  // Useful when zoom level changes: growing avoids eviction churn,
  // shrinking reduces wasted memory.
  void ResizeCache(uint32_t desired_capacity);
};
};  // namespace alcedo

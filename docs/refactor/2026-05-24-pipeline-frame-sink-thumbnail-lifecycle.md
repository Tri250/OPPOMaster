# Pipeline Frame Sink and Thumbnail Lifecycle Refactor

Date: 2026-05-24

## Background

Commit `cf659da` fixed a crash observed around
`CPUPipelineExecutor::SetNextFramePresentationMode()` by detaching the editor frame sink when
`EditorFrameManager` is destroyed. This addresses one concrete dangling-pointer path, but the
underlying ownership model is still awkward:

- `CPUPipelineExecutor` instances are cached by `PipelineMgmtService` per sleeve element.
- `CPUPipelineExecutor` also stores a raw `IFrameSink*`.
- The editor frame sink is only valid while an editor session is open.
- Thumbnail and export tasks may reuse the same cached or pinned pipeline executor.

That means a cached image pipeline can retain UI output state from an editor session, and thumbnail
rendering can accidentally interact with an editor-owned sink. This is inconsistent with the
intended architecture.

Separately, thumbnail zoom-tier behavior currently mixes two different identities:

- Cache and pending entries are keyed by `(element_id, resolution)`.
- Cancellation, active flags, pin tracking, release, and invalidation are mostly keyed only by
  `element_id`.

This coarse cancellation model can cancel or invalidate work for a thumbnail request that is still
visible after a zoom-level change.

## Target Semantics

The desired album behavior is:

- The editor preview output target is session-scoped, not a durable part of a per-image pipeline.
- Thumbnail rendering is independent from editor frame presentation.
- Thumbnail cache entries are keyed by the actual thumbnail request identity.
- Zoom-level changes may release old tiers and request new tiers, but must not cancel the currently
  visible request.
- Cache pressure should evict unpinned thumbnail entries first.
- Pinned visible entries must survive cache resizing and LRU eviction.
- Content changes, import/delete, or edit commits may invalidate all tiers for an element.
- View recycling or zoom changes should only cancel/release the specific stale request.

## Phase 1: Decouple Frame Sink From Cached Pipelines

### Problem

`CPUPipelineExecutor` currently owns a raw `frame_sink_` pointer and forwards presentation metadata
through it. Since executors are cached by `PipelineMgmtService`, this makes a UI object lifetime
depend on a cached per-image pipeline lifetime.

This is especially fragile when:

- An editor opens and attaches a frame sink to a pipeline.
- The same element's pipeline remains pinned or cached.
- A thumbnail task loads or reuses that pipeline.
- The thumbnail path calls `SetNextFramePresentationMode()` or preview metadata setters.
- The editor frame sink has already been destroyed or replaced.

### Intended Design

Treat `IFrameSink` as an output target for a render invocation or editor session, not as persistent
pipeline state.

Preferred direction:

- Remove durable frame sink ownership from `CPUPipelineExecutor`.
- Pass an optional frame sink through render task options or an editor-preview render context.
- Thumbnail and export tasks should explicitly have no frame sink.
- Editor preview tasks should explicitly provide the current editor sink.
- GPU stage frame-sink attachment should be derived from the current render invocation, not from
  cached executor state.

Short-term hardening if the full decoupling is too large:

- Add a single lock-protected attach/detach path for frame sinks.
- Require all attach, detach, viewer replacement, scope-panel replacement, and executor replacement
  paths to hold `CPUPipelineExecutor::GetRenderLock()`.
- Detach any previous executor before attaching a new one.
- Ensure thumbnail tasks detach or bypass any existing sink before rendering.
- Keep `PipelineMgmtService::SavePipeline()` sink reset as a fallback, not as the primary lifetime
  mechanism.

### Acceptance Criteria

- Thumbnail rendering cannot call into an editor-owned `IFrameSink`.
- Closing the editor while preview work is in flight cannot leave a dangling sink pointer.
- Reopening the editor or importing history cannot mutate execution stages concurrently with render.
- A cached pipeline can be reused for thumbnail/export/editor without carrying stale UI output state.

## Phase 2: Make Thumbnail Lifecycle Request-Key Based

### Problem

The service already models cache entries and pending callbacks with `ThumbnailCacheKey`:

```cpp
struct ThumbnailCacheKey {
  sl_element_id_t element_id;
  ThumbnailResolution resolution;
};
```

However, several lifecycle controls still operate at `element_id` granularity:

- `CancelPending(element_id)` cancels all resolution tiers.
- `ReleaseThumbnail(element_id)` cancels all pending tiers and releases all cached tiers.
- `InvalidateThumbnail(element_id)` removes all tiers.
- `ThumbnailManager::thumbnail_pins_` tracks one pin state per element.
- `thumbnail_active_flags_` tracks one active flag per element.

This is too coarse for zoomable thumbnail grids. When zoom changes, a delegate may release an old
tier and request a new tier for the same element. Element-wide cancellation can invalidate the new
request even though it is the one currently visible.

### Intended Design

Promote the request identity through the whole thumbnail lifecycle.

At minimum, use:

```cpp
using ThumbnailRequestKey = ThumbnailCacheKey;  // element_id + resolution
```

If exact display size matters within a tier, extend the UI-side request identity to include
`image_id` and `max_edge`:

```cpp
struct ThumbnailRequestIdentity {
  sl_element_id_t element_id;
  image_id_t image_id;
  ThumbnailResolution resolution;
  uint32_t requested_max_edge;
};
```

Recommended service API changes:

- Add `CancelPending(ThumbnailCacheKey key)`.
- Add `ReleaseThumbnail(ThumbnailCacheKey key)`.
- Keep `InvalidateThumbnail(element_id)` for content-level invalidation across all tiers.
- Keep an explicit `CancelAllPendingForElement(element_id)` only for delete/import/editor-reset
  scenarios where all tiers are truly stale.
- Track generation tokens per `ThumbnailCacheKey`, not only per `element_id`.
- Track active flags per request identity, not only per `element_id`.
- Track visible pins per request identity, or at least per `(element_id, resolution)`.

### Zoom Behavior

On zoom-level change:

1. Release the old visible request key.
2. Mark only the old request key inactive.
3. Request the new visible request key.
4. Keep UI loading state for the new key until it either succeeds or fails for a real reason.
5. Do not map cancellation to "missing source".

When a delegate is recycled:

1. Release only the request key previously pinned by that delegate.
2. Do not invalidate all tiers for the element.
3. Do not cancel a newer request for the same element and a different tier.

When an edit is committed, the source file changes, or the item is deleted:

1. Invalidate all tiers for that element.
2. Cancel all pending requests for that element.
3. Clear UI thumbnail state for that element.

### Error-State Cleanup

The current UI red exclamation state is driven by `thumbMissingSource`. Cancellation should not
surface as missing source. The thumbnail state should distinguish at least:

- `loading`
- `ready`
- `cancelled/stale`
- `missing_source`
- `render_error`

Until detailed error reporting is available, cancellation should normally return to an idle/loading
state for the current request, not to a missing-source state.

### Acceptance Criteria

- Changing zoom level does not produce red missing-source indicators for still-visible files.
- Releasing an old tier cannot cancel a newer tier for the same element.
- Cache eviction preserves pinned request keys.
- `InvalidateThumbnail(element_id)` remains available for real content invalidation.
- Tests cover rapid zoom changes, delegate recycling, and concurrent old/new tier requests for the
  same element.

## Suggested Implementation Order

1. Harden the current frame sink attach/detach paths under `render_lock_`.
2. Add request-key overloads to thumbnail cancel/release APIs.
3. Move thumbnail active flags and pin tracking from element-level to request-key-level.
4. Update QML/C++ delegate release paths to pass the exact old request key.
5. Split cancellation from missing-source UI state.
6. Remove durable `frame_sink_` state from cached pipelines, or restrict it to an invocation-scoped
   render context.


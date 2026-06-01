# Technical Design Notes

This document explains the implementation shape of `fix/color_adj_overhaul`. It focuses on the
parts most likely to matter during merge review: color adjustment semantics, local tone mapping,
backend parity, preview scheduling, and viewer presentation.

## Color Adjustment Model

The branch moves the HLS and saturation tools toward a perceptual color model rather than the old
RGB/HLS-style manipulation. The practical goal is that a hue-targeted edit should feel local in
perceived hue and should not collapse lightness or chroma in unrelated colors.

Important behavior:

- The default HLS profile set uses eight hue bins: `0, 45, 90, 135, 180, 225, 270, 315`.
- The default hue smoothness/range is widened to `45.0f`, replacing the older narrow `15.0f`
  behavior. This makes profile edits blend more naturally between neighboring bins.
- HLS adjustments operate through OKLab-like lightness/chroma/hue math:
  - source RGB/AP1 is converted to Lab-like coordinates;
  - hue is the polar angle of the opponent axes;
  - chroma is the radius in the opponent plane;
  - hue edits rotate the angle;
  - lightness edits modify the perceptual lightness coordinate;
  - saturation/chroma edits scale chroma with asymmetric positive/negative response.
- Saturation is fused into the HLS/color payload instead of remaining a separate point kernel. In
  fused GPU params, `saturation_offset_` is now a scale-like value with identity at `1.0f`, not
  the older offset identity at `0.0f`.
- The HLS path includes protection gates based on source chroma, shadows, and highlights. Low
  chroma pixels, deep shadows, and extreme highlights receive less hue/chroma movement.

Files to check when resolving merge conflicts:

- `alcedo_studio/src/edit/operators/color/HLS_op.cpp`
- `alcedo_studio/src/include/edit/operators/CPU_kernels/color/HLS_kernel.hpp`
- `alcedo_studio/src/include/edit/operators/GPU_kernels/color.cuh`
- `alcedo_studio/src/edit/pipeline/opencl_shader/color.cl`
- `alcedo_studio/src/edit/operators/GPU_kernels/metal_shader/color.metal`
- `alcedo_studio/src/include/edit/operators/GPU_kernels/fused_param.hpp`
- `alcedo_studio/src/include/edit/operators/GPU_kernels/param.cuh`
- `alcedo_studio/src/include/edit/operators/GPU_kernels/opencl_param.hpp`
- `alcedo_studio/src/include/edit/operators/GPU_kernels/metal_param.hpp`

## Highlight/Shadow Local Tone Stage

The old accelerated highlight and shadow behavior was point-wise. That was fast, but it could not
distinguish broad tonal structure from local texture. This branch introduces a local-tone stage
that treats H/S as a scene-referred base/detail operation.

The intended order is:

1. Convert to working space.
2. Apply exposure, contrast, and tone point operations.
3. Build and apply H/S local tone.
4. Apply curve, vibrance, color wheel, HLS/saturation, LMT, and output transform.
5. Apply detail stages such as sharpen and clarity.

In CUDA this order is visible in `pipeline_gpu_impl.cu`:

- pre-H/S point chain: `to_ws`, `exposure`, `contrast`, `tone`;
- local-tone stage: `GPU_HighlightShadowLocalToneStage`;
- post-H/S point chain: `curve`, `vibrance`, `color_wheel`, `hls`, `lmt`, `to_output`;
- neighbor/detail kernels: sharpen and clarity.

### Local Tone Data Flow

The local-tone stage uses log luminance rather than display-referred values:

1. Read scene-referred AP1-like RGB.
2. Compute AP1 luminance and log2 luminance.
3. Build a blurred base-log buffer with a separable Gaussian kernel.
4. Compute detail as source log luminance minus base log luminance.
5. Evaluate shadow and highlight masks from the base/source tonal reference.
6. Apply base deltas for highlight compression or shadow lift/darken.
7. Reintroduce local detail with protection gates:
   - avoid amplifying noise in very deep shadows;
   - preserve or gently boost useful shadow texture;
   - keep highlight base compression while allowing recoverable highlight detail to remain;
   - avoid treating soft ramps as hard edges;
   - guard chromatic fringe-like regions from excessive local contrast changes.
8. Reconstruct AP1 RGB without arbitrary `0..1` clamping.

### Operator Payload

`ShadowsOp` and `HighlightsOp` now both populate H/S local-tone parameters into
`OperatorParams`:

- `hs_local_tone_enabled_`
- `hs_base_radius_`
- `hs_base_gaussian_tap_count_`
- `hs_base_gaussian_weights_`
- `hs_shadow_log_pivot_`
- `hs_shadow_log_width_`
- `hs_highlight_log_pivot_`
- `hs_highlight_log_width_`

At the time of this handoff, both operators set:

- base radius: `18.0f`
- maximum Gaussian radius used to build taps: `48`
- shadow pivot: `-3.05f`
- shadow width: `0.62f`
- highlight pivot: `-2.80f`
- highlight width: `3.35f`

These numbers should be treated as tuned image-processing constants, not generic defaults.

### Shared Tone Curve

The branch keeps the shared tone curve payload because non-local and compatibility behavior still
matters. Tests were adjusted to reflect the accepted shape:

- shadow controls affect the shadow side and reach into useful upper-shadow/midtone texture;
- highlight controls can affect upper midtones and compress high values;
- shared curve payload upload is verified for both fused and legacy GPU parameter paths.

## CUDA Backend

CUDA remains the source of truth for the branch's H/S local-tone math.

Important concepts:

- `GPU_HighlightShadowLocalToneStage` is inserted as a dedicated stream stage rather than being
  hidden inside the point chain.
- The stage caches a log-base buffer so slider movement can reapply the local-tone function without
  rebuilding the blurred base when the source/reference context has not changed.
- The cache key is derived from luminance-distribution-sensitive inputs and render source context,
  not from every H/S slider movement.
- Scene-referred intermediates may exceed display range. Do not clamp them early.

Primary file:

- `alcedo_studio/src/include/edit/operators/GPU_kernels/color.cuh`

Functions/helpers worth preserving during conflict resolution include:

- H/S mask and base-delta helpers.
- H/S curve slope helpers.
- shadow detail preservation and fill-light weights.
- highlight detail compensation weights.
- chromatic fringe and local-mix guards.
- OKLab/OKLCh conversion helpers.
- `GPU_HighlightShadowLocalToneOpKernel`.
- `GPU_HLSOpKernel`.

## OpenCL Backend

OpenCL is implemented as a staged pipeline so it can run the same H/S local-tone structure as CUDA.
The key design is:

1. Run pre-H/S fused point ops into an intermediate image.
2. Build or reuse the H/S log-base buffer.
3. Apply local tone into a second intermediate image.
4. Run post-H/S fused point ops.
5. Run neighbor/detail stages.

Important implementation details:

- `pipeline_opencl_impl.cpp` owns H/S base-log cache state:
  - `hs_base_log_`
  - cached width/height/pitch
  - host-side H/S base cache key
  - reference-base state for ROI/detail previews
- The OpenCL base buffer is a single-channel float buffer, not a four-channel image.
- The uploaded OpenCL struct intentionally avoids host-only 64-bit cache-key fields where they
  would break ABI layout. Cache keys stay host-side where possible.
- `edit_pipeline_detail.cl` defines:
  - `edit_pipeline_hs_build_log_base_h_rgba32f`
  - `edit_pipeline_hs_build_log_base_v_rgba32f`
  - `edit_pipeline_hs_apply_local_tone_rgba32f`
- `opencl_pipeline_programs.cpp` and `.hpp` register the detail program/kernels through the
  existing program library flow.

OpenCL merge risks:

- Do not reorder `OpenClFusedParams` fields without checking the C++/OpenCL layout validation.
- Do not add 64-bit host cache keys to the uploaded shader struct unless all ABI checks are updated
  intentionally.
- Keep `edit_pipeline_detail.cl` dependent on `fused_params.cl`, `common.cl`, and `color.cl`.

## Metal Backend

Metal was added after CUDA/OpenCL and mirrors the same staged design.

Important implementation details:

- `pipeline_metal_impl.cpp` now owns:
  - `hs_base_log_`
  - `hs_temp_log_`
  - cached H/S dimensions and cache key
  - reference-base state
  - staged fused resources
- Metal uses `ComputePipelineCache` to retrieve immutable pipeline states. It should not recreate
  compute pipelines on every operator invocation.
- Metal execution stats now track H/S encode time separately from fused and neighbor encode time.
- `fused_pipeline.metal` includes:
  - staged fused kernel support;
  - H/S base bilinear read support for reference-base ROI reuse;
  - local-tone apply kernel support.
- `color.metal` mirrors the CUDA/OpenCL H/S local-tone and HLS logic.

Metal merge risks:

- Keep `MetalFusedParams` aligned with `common.metal`.
- Preserve `MetalHsApplyParams` layout.
- Keep local-tone resources reusable across command buffers.
- Preserve the separate H/S encode timing fields; they are useful for performance regressions.

## Preview Scheduling and ROI Semantics

Local tone mapping is neighborhood dependent. A detail ROI is not just a crop: if the base layer is
rebuilt from a cropped patch, the tone response changes at the same pixel. This branch therefore
updates scheduler/viewer behavior so ROI previews carry enough reference metadata to keep local-tone
context stable.

Important behavior:

- `ViewportRenderRegion` now carries:
  - source position;
  - scale;
  - reference width/height;
  - target width/height.
- `PipelineScheduler` can derive a source ROI rectangle from a viewport region.
- detail ROI previews use viewport target pixel size as the render max edge when possible.
- rotation/crop preview cases that need full-frame context avoid invalid ROI assumptions.
- frame sink ownership is detached for tasks that should not call back into an editor-owned viewer
  sink, such as non-editor thumbnail/export paths.

Files to check:

- `alcedo_studio/src/renderer/pipeline_scheduler.cpp`
- `alcedo_studio/src/include/ui/edit_viewer/frame_sink.hpp`
- `alcedo_studio/src/ui/edit_viewer/viewport_mapper.cpp`
- `alcedo_studio/src/ui/edit_viewer/edit_viewer.cpp`

## Viewer Presentation and Detail Patch

The viewer now supports a layered presentation model for direct GPU frames:

- full-frame / quality-base frames provide the stable base image;
- detail-patch frames provide a higher-resolution patch for the current viewport;
- detail patches are accepted only if their preview generation, detail serial, ROI, and aspect are
  compatible with the current view state.

Important parts:

- `DirectPresentFrameQueue` keeps pending direct-present slots ordered while de-duplicating slots.
- `IsRenderReferenceFrame` distinguishes frames that can become a reference from ROI/detail patch
  frames.
- `RhiEditViewerSurface` can bind both primary and detail textures and composite a detail patch in
  the fragment shader.
- stale detail patches are dropped when preview generation changes.
- `QtEditViewer::SyncPendingFrameStateForScheduling()` exposes a scheduling synchronization point.

This solves two user-visible problems:

- zoomed preview can request a sharper patch without replacing the full-frame reference with an ROI
  frame;
- local-tone and geometry edits do not get inconsistent viewport coordinates from stale or cropped
  metadata.

## Packaging/CMake Change

The root `CMakeLists.txt` adds `alcedo_install_packaged_luts(config_dir, destination)`. It removes
the destination LUT directory before installing the curated LUT set, then installs only the curated
Agfa/Fuji/Kodak `.cube` files gathered by `alcedo_collect_packaged_luts`.

This helper is used for both macOS app bundle installation and non-bundle runtime installation.

The practical intent is to avoid stale LUTs surviving in the install tree after the curated LUT
list changes.

## Test Coverage Added or Updated

Relevant tests:

- `SharedToneCurveTest`
  - shared curve anchor shape;
  - shadow/highlight side behavior;
  - GPU upload payload parity.
- `PipelineFrameSinkTest`
  - detached sink no-op behavior;
  - render region crop behavior;
  - detail ROI preview target sizing;
  - scheduler/frame-sink safety.
- `EditViewerLogicTests`
  - viewport reference dimensions;
  - direct present queue ordering;
  - render reference frame classification;
  - point-to-point zoom anchor behavior;
  - crop/pan clamp behavior.
- `CropRotateOpTest`
  - crop/rotate and ROI behavior relevant to preview scheduling.

## Known Non-Goals

- This branch does not redesign the CPU tone pipeline to match the GPU LLF implementation exactly.
  CPU-side HLS/shared-curve behavior remains important for parameter/testing paths, but the H/S LLF
  work is primarily GPU backend work.
- This branch does not add a new chroma recovery model for highlights beyond the current guarded
  highlight-chroma preservation logic. Earlier accepted tuning specifically kept highlight detail
  compensation in luminance/detail space unless chroma is reopened as a separate task.
- This branch does not make arbitrary display-range clamps in scene-referred intermediate math.

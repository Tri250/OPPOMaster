# Tone Mapping Operator Refactor Plan

This handoff describes a proposed refactor for the highlight/shadow local-tone code currently
spread across the color and detail operator files. It is intended for an implementation agent that
will do the actual code changes later.

## Scope

The refactor should turn the current highlight/shadow local-tone implementation into a dedicated
tone-mapping operator that owns both:

- highlight/shadow tonal placement
- large-radius local detail enhancement currently overlapping with clarity

The user-facing controls and project-file behavior should remain compatible. Existing
`Highlights`, `Shadows`, and `Clarity` controls may continue to exist, but internally they should
feed one local tone-mapping stage instead of separate or misleadingly named shader modules.

## Current Problem

The current file boundaries no longer match the actual algorithm.

- `alcedo_studio/src/include/edit/operators/GPU_kernels/color.cuh`
  contains the CUDA source-of-truth for highlight/shadow local tone and LLF-style processing. The
  `GPU_HighlightShadowLocalToneStage` and most `hs_*` helpers are tone-mapping code, not color
  adjustment code.
- `alcedo_studio/src/edit/pipeline/opencl_shader/color.cl`
  mirrors a large part of the same highlight/shadow tone logic even though the file name suggests
  point color operations such as tint or vibrance.
- `alcedo_studio/src/edit/operators/GPU_kernels/metal_shader/color.metal`
  has the same naming mismatch for the Metal backend.
- `alcedo_studio/src/include/edit/operators/GPU_kernels/detail.cuh`,
  `alcedo_studio/src/edit/operators/GPU_kernels/metal_shader/detail.metal`, and
  `alcedo_studio/src/edit/pipeline/opencl_shader/edit_pipeline_detail.cl` contain clarity kernels
  based on Gaussian blur plus residual boosting. This overlaps conceptually with LLF detail
  handling.
- `alcedo_studio/src/edit/operators/detail/clarity_op.cpp` exposes clarity as a separate operator,
  but its actual behavior is large-radius local contrast enhancement, not final-stage sharpening.

The refactor should not treat this as a cosmetic rename only. The end state should have a clear
algorithm boundary:

```text
color operators: tint, vibrance, HSL/color-selection, chroma/lightness point edits
tone mapping: scene-referred local base/detail remap for highlights, shadows, and clarity
detail operators: final sharpen and any small-radius non-tone detail operations
```

## Design Principles

Use these principles when implementing the refactor.

- Keep the local tone stage scene-referred. Do not move the behavior into display/output space.
- Keep the scalar field as AP1 intensity encoded in ACEScc/log-style space unless a later research
  task explicitly changes the math.
- Preserve ratio-based AP1 reconstruction and lower-gamut fitting. Do not add broad `0..1` clamps
  inside ACEScc/AP1/log-intensity paths.
- Treat LLF/local Laplacian as the primary architecture for highlight/shadow and large-radius
  clarity behavior.
- Keep final sharpening separate. Sharpening is an output/detail operation; clarity is a local
  tone/detail remap.
- Keep CUDA, OpenCL, and Metal behavior aligned. Do not let OpenCL or Metal become a simplified
  approximation of CUDA again.
- Keep ROI, reference-cache, and export-preserve-detail semantics intact during the move.

## Target Architecture

Introduce a dedicated operator family:

```text
alcedo_studio/src/edit/operators/tone/tone_mapping_op.cpp
alcedo_studio/src/include/edit/operators/tone/tone_mapping_op.hpp
alcedo_studio/src/include/edit/pipeline/local_tone_mapping.hpp
alcedo_studio/src/include/edit/operators/GPU_kernels/tone_mapping.cuh
alcedo_studio/src/edit/pipeline/opencl_shader/tone_mapping.cl
alcedo_studio/src/edit/operators/GPU_kernels/metal_shader/tone_mapping.metal
```

The exact file names can be adjusted to match local conventions, but the ownership should be clear:
tone mapping code should not live in `color.*`, and LLF kernels should not be hidden in generic
detail shader files.

The shared header should become the single math contract for:

- mask/reference dimensions
- cache-key construction
- shadow/highlight EV profiles
- reference tone curve
- detail alpha/beta functions
- LLF sample generation
- backend amount limits and sampling constants

`alcedo_studio/src/include/edit/pipeline/highlight_shadow_local_tone.hpp` is the closest current
seed for this shared layer. Prefer evolving it instead of creating a parallel implementation.

## Internal Data Flow

The desired tone-mapping stage should follow this model:

```text
input ACEScc RGBA
  -> decode to AP1 for scalar intensity
  -> encode AP1 intensity as log/intensity L
  -> build or reuse local base/reference
  -> build LLF samples from highlight/shadow/clarity params
  -> build remap pyramids
  -> collapse adjusted log-intensity output
  -> reconstruct AP1 by source-intensity ratio
  -> lower-gamut fit against neutral adjusted intensity
  -> encode back to ACEScc RGBA
```

For full-resolution export or other preserve-source-detail paths, keep the current delta-style
meaning:

```text
adjusted_source_l = source_l + (adjusted_reference_l - reference_l)
```

This prevents preview-scale LLF output from replacing full-resolution source detail.

## Parameter Model

Add an internal parameter group, tentatively:

```cpp
struct ToneMappingParams {
  bool shadows_enabled;
  bool highlights_enabled;
  bool clarity_enabled;

  float shadow_amount;
  float highlight_amount;
  float clarity_amount;

  float local_radius;
  float sigma_r;

  bool preserve_source_detail;
  bool roi_enabled;
  int roi_x;
  int roi_y;
  float roi_scale_x;
  float roi_scale_y;
  int roi_reference_width;
  int roi_reference_height;

  uint64_t base_cache_key;
};
```

This is illustrative, not a required ABI. The implementation should map onto the existing
`OperatorParams`, `GPUOperatorParams`, and fused parameter structs carefully.

Compatibility rule:

- Keep saved project semantics stable.
- Existing `HighlightOp`, `ShadowOp`, and `ClarityOp` should initially remain as user-facing
  parameter producers.
- They should write into the tone-mapping parameter payload rather than dispatching separate
  large-radius local contrast code.

## Clarity Migration

The current clarity algorithm is:

```text
Gaussian blur at medium/large radius
detail = original - blur
protect strong edges
apply mostly in midtones
add detail * strength
```

That behavior should migrate into the LLF detail branch instead of staying as a separate
neighbor-blur operator. Recommended mapping:

- positive clarity increases local residual/detail response around midtones and practical shadows
- negative clarity reduces local residual/detail response
- shadow lift may increase detail strength in lifted dark regions, but should remain guarded
  against noise and edge halos
- highlight reduction should avoid default texture loss in bright clouds or near-clip shoulders
- final small-radius sharpening should remain outside this tone-mapping operator

Do not delete the old clarity path in the first implementation step. Keep it available as a
compatibility fallback until the new path has dedicated tests and visual validation.

## Migration Plan

### Phase 1 - Mechanical Move Without Math Changes

Goal: make the ownership correct while preserving behavior.

Tasks:

- Move CUDA `hs_*` helpers, LLF kernels, and `GPU_HighlightShadowLocalToneStage` from
  `color.cuh` into a new tone-mapping CUDA header.
- Move OpenCL highlight/shadow local-tone helpers from `color.cl` into a tone-mapping shader
  include.
- Move Metal highlight/shadow local-tone helpers from `color.metal` into a tone-mapping Metal
  include.
- Keep `color.*` focused on tint, vibrance, HSL, saturation, and color-space helpers.
- Update includes, shader program manifests, CMake wiring, and backend program registration.
- Preserve all public operator names and serialized parameter names.

Acceptance:

- Build succeeds on Windows through the MSVC wrapper.
- Existing highlight/shadow outputs remain unchanged within current backend tolerances.
- OpenCL and Metal still compile their shader libraries.

Implementation status:

- CUDA `hs_*` helpers, LLF kernels, and `GPU_HighlightShadowLocalToneStage` moved from
  `color.cuh` to `tone_mapping.cuh`; `color.cuh` now includes the tone-mapping header after the
  shared ACEScc/AP1 helpers it depends on.
- OpenCL highlight/shadow local-tone helpers moved from `color.cl` to `tone_mapping.cl`; the
  OpenCL pipeline program manifest now concatenates `tone_mapping.cl` before `color.cl` and
  `edit_pipeline_detail.cl`.
- Metal highlight/shadow local-tone helpers and `GPU_HighlightShadowLocalToneOpKernel` moved from
  `color.metal` to `tone_mapping.metal`; the fused Metal shader includes it and CMake tracks it as
  a fused-pipeline dependency.
- Added `ToneMappingOwnershipTest` to guard the Phase 1 source ownership boundary while existing
  highlight/shadow parity tests continue to validate unchanged output behavior.

### Phase 2 - Shared Tone Mapping Contract

Goal: remove duplicated constants and curve logic across CUDA/OpenCL/Metal/host code.

Tasks:

- Promote `highlight_shadow_local_tone.hpp` into a backend-agnostic local tone contract or add a
  new `local_tone_mapping.hpp` that replaces it.
- Centralize constants such as sample count limits, gamma range, sigma values, highlight strength,
  backend amount limits, and cache-key construction.
- Add a generated or explicitly mirrored constants strategy for OpenCL and Metal so backend drift
  is hard to introduce accidentally.
- Ensure CUDA, OpenCL, and Metal sample generation remain mathematically aligned.

Acceptance:

- Unit tests cover sample generation, reference curve shape, detail alpha/beta, and cache-key
  construction.
- Existing shared tone curve tests remain green.

Implementation status:

- Added `alcedo_studio/src/include/edit/pipeline/local_tone_mapping.hpp` as the shared
  backend-agnostic tone-mapping contract. The previous
  `highlight_shadow_local_tone.hpp` remains as a compatibility include for the existing
  OpenCL/Metal stage namespace.
- Centralized host-side constants, reference curve, sample generation, sigma, mask dimensions,
  level-count logic, and adjusted/ROI cache-key construction in the shared contract.
- Updated the CUDA local-tone stage to reuse the shared contract for constants, host-side cache
  keys, dimensions, sigma, level count, and LLF sample payload generation.
- Added explicit OpenCL and Metal mirrored constant blocks in `tone_mapping.cl` and
  `tone_mapping.metal`; these are now guarded by tests so backend drift is easier to catch.
- Added `LocalToneMappingContractTest` for sample generation, reference curve tone direction,
  detail alpha/beta bounds, cache-key participation, compatibility exports, and shader constant
  mirror checks.

### Phase 3 - ToneMappingOp Facade

Goal: introduce the new operator boundary without breaking UI or project files.

Tasks:

- Add `ToneMappingOp` or equivalent internal operator/stage.
- Keep `HighlightOp`, `ShadowOp`, and `ClarityOp` as compatibility facades.
- Route highlight/shadow params into the new tone-mapping payload.
- Leave old clarity dispatch active until Phase 4 unless the implementation can prove equivalence.
- Keep pipeline order equivalent to the current accepted architecture:

```text
pre-H/S point operators
  -> local tone/tone mapping
  -> post-H/S point operators
  -> detail/sharpen stages
  -> output/render transform
```

Acceptance:

- Project load/save behavior does not change.
- UI slider behavior remains stable.
- Existing highlight/shadow visual regressions do not reappear.

### Phase 4 - Integrate Clarity Into LLF Detail

Goal: make clarity a local tone/detail control instead of a separate large-radius blur path.

Tasks:

- Add `clarity_amount` to the local tone sample/detail functions.
- Extend detail alpha/beta or add a dedicated detail gain function that is driven by clarity.
- Keep edge, noise, and bright-highlight guards.
- Add a temporary feature flag if needed, so old clarity and new LLF clarity can be compared.
- Once validated, disable the separate clarity neighbor-blur dispatch for the accelerated path.

Acceptance:

- Clarity-only edits retain the expected local contrast direction.
- Highlight/shadow edits with clarity active do not produce strong edge halos or local reversals.
- `shadow +100 / highlight -100` remains a mandatory regression case.

### Phase 5 - Cleanup And Naming

Goal: remove obsolete names and reduce future confusion.

Tasks:

- Remove highlight/shadow LLF code from `color.*`.
- Remove H/S kernels from generic detail shader files where possible.
- Keep only genuine sharpen/detail kernels in `detail.*`.
- Rename debug labels from `H/S` to `Tone Mapping` or `Local Tone` where appropriate.
- Update nearby comments and docs so future agents do not treat `color.cuh` as the tone-mapping
  source of truth.

Acceptance:

- Search results for `hs_*` are confined to tone-mapping files or compatibility wrappers.
- `color.*` no longer contains LLF or local-tone implementation details.

## Cache And ROI Requirements

Do not simplify the cache model during this refactor. The current behavior exists to avoid real
preview/export bugs.

Preserve:

- canonical full-frame/QualityBase reference ownership
- ROI/detail preview output-size correctness
- temporary thumbnail/export reference exceptions
- preserve-source-detail export semantics
- cache-key participation for any parameter that changes adjusted output meaning
- ROI fields in ROI adjusted-result cache keys

If a later agent sees flicker, stale detail previews, or export halo/upscaling after this refactor,
the first suspects should be cache identity, reference ownership, and ROI geometry, not just shader
curve constants.

## Backend Requirements

CUDA should remain the initial source of truth for implementation order, but the final refactor is
not complete until OpenCL and Metal are aligned.

CUDA:

- Use the new `tone_mapping.cuh` ownership boundary.
- Preserve stream/resource reuse in `GPU_HighlightShadowLocalToneStage` or its renamed equivalent.

OpenCL:

- Keep the staged execution path: pre-local-tone fused ops, local tone base/apply, post-local-tone
  fused ops, then detail/sharpen.
- Keep program-library and manifest registration updated.
- Do not keep a simplified local-tone algorithm in OpenCL.

Metal:

- Keep immutable pipeline state reuse.
- Update `.metal` shader wiring, CMake metallib inputs, and C++ pipeline labels together.
- Keep packed/optimized paths in sync with scalar paths.

## Validation Plan

Windows/MSVC commands should use the repository wrapper:

```bat
cmd /c scripts\msvc_env.cmd --build --preset win_debug --target EditPipeline --parallel 4
cmd /c scripts\msvc_env.cmd --build --preset win_debug --target alcedo_main --parallel 4
ctest --test-dir build/debug -R "SharedToneCurveTest|OpenClFusedEditPipelineTest|PipelineFrameSinkTest|ExportServiceTest" --output-on-failure
```

Suggested focused regression cases:

- `shadow +100 / highlight -100`
- `highlight -100` on bright clouds or near-clip highlights
- `shadow +100` on deep practical shadows
- clarity-only positive adjustment
- clarity-only negative adjustment
- highlight/shadow plus clarity on soft edges
- ROI detail preview with tone mapping active
- full-resolution export after preview cache has been populated

Expected checks:

- no output-size regression in ROI/detail preview
- no preview-scale look in full-resolution export
- no stale cache reuse when clarity changes
- no broad 0..1 clamping in scene-referred intermediate paths
- CUDA/OpenCL/Metal numerical differences remain inside existing tolerances

## Implementation Order For Another Agent

Recommended first pull request:

1. Add the new tone-mapping files.
2. Move code mechanically from `color.*` and detail shader files.
3. Update includes and shader registration.
4. Keep behavior unchanged.
5. Add a short note in this document listing what moved.

Recommended second pull request:

1. Introduce the shared parameter/contract layer.
2. Add tests for sample generation and cache keys.
3. Keep old clarity behavior untouched.

Recommended third pull request:

1. Route clarity into the LLF detail branch behind a feature flag or guarded path.
2. Compare old and new clarity behavior.
3. Remove the old accelerated clarity path only after validation.

This sequence avoids mixing file-boundary cleanup, cache semantics, and quality tuning in one risky
change.

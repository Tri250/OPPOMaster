# CUDA Halation Handoff

This handoff captures the planned CUDA-first halation operator. It is a planning document only.
Do not treat it as an implementation patch.

## Scope

- Add a user-facing `HalationOp` with one visible slider: `strength`.
- Keep hidden algorithm defaults stable until the UI/product decision expands them.
- Backend implementation scope: CUDA first.
- Execute halation as a neighbor-style stage in the CUDA texture/detail tail.
- Place sharpen and clarity before halation and film grain.
- Place halation before film grain.
- Reuse the existing ping-pong scratch buffers in `GPU_StaticKernelStream`; do not allocate a
  dedicated full-size halation image.
- Keep the color-management ODT deterministic. Halation is a post-ODT/output-space texture/glow
  effect, not part of `OutputTransform_fwd`, `OpenDRTTransform_fwd`, or `DisplayEncoding`.

## Source Model

The implementation target follows a bloom-like halation model:

- Bloom reference: LearnOpenGL's bloom chapter describes the classic post-process shape:
  extract bright regions, blur the extracted image, then add the blurred result back to the
  original image.
  - Source: https://learnopengl.com/Advanced-Lighting/Bloom
- Halation reference: Alex Castronovo's film-emulation article adapts that bloom shape for film
  halation:
  - soft threshold with `low = 0.6`, `high = 0.7`
  - blur kernel `exp(-dist / sigma)`
  - default `sigma = 20.0`
  - redshift multiplier `[1.0, 0.05, 0.02]`
  - additive weighted blend back into the source
  - Source: https://articles.alexcastronovo.com/article/2/from-code-to-kodachrome-film-emulation-from-scratch

For Alcedo, use those values as hidden defaults, but expose only a 0-100 `strength` slider at first.

## Algorithm Contract

Work in the display/output buffer after ODT and display encoding. Clamp only the threshold input to
`[0, 1]`; do not clamp scene-referred AP1, ACEScc, or pre-ODT values for this feature.

Suggested scalar mask:

```text
rgb01 = clamp(display_rgb, 0, 1)
brightness = dot(rgb01, vec3(0.2126, 0.7152, 0.0722))
t = saturate((brightness - low) / (high - low))
soft_mask = t
thresholded_rgb = display_rgb * soft_mask
redshifted_rgb = thresholded_rgb * vec3(1.0, 0.05, 0.02)
```

The first implementation can use the Rec.709 luma scalar above because it matches the bloom-style
bright-region extraction and avoids per-channel hue discontinuities on sky ramps. If diagnostics
show that saturated colored highlights need a stronger trigger, compare `brightness = max(rgb01)`
as a tuning follow-up rather than changing the first contract silently.

Blur the redshifted threshold component with an exponential falloff. Since the current CUDA neighbor
tail is built from horizontal/vertical ping-pong stages, start with a separable implementation:

```text
w(i) = exp(-abs(i) / sigma)
normalize(sum(w))
horizontal pass: threshold + redshift + horizontal blur -> scratch
vertical pass: vertical blur(scratch) + additive blend -> original destination
```

This is an L1/separable approximation of the radial `exp(-dist / sigma)` reference, chosen because
it maps cleanly to the existing two-stage neighbor infrastructure and two-scratch-buffer constraint.
If the look is too boxy at high strength, add a later quality phase to test a true radial kernel or a
multi-pass approximation.

Final blend:

```text
out_rgb = original_rgb + strength_scale * blurred_halation_rgb
out_a = original_a
```

Initial strength mapping should be conservative:

```text
strength_scale = (slider / 100.0) * hidden_defaults.additive_scale
```

Start with `additive_scale = 1.0`, then tune only if test images show the default slider is too
aggressive. Do not bake tuning into the threshold or redshift defaults unless the visual failure is
clearly caused there.

## Current Code Anchors

Use the existing output-effect and neighbor-stage conventions as implementation anchors:

- strength-only output-effect operator example:
  - `alcedo_studio/src/include/edit/operators/cst/film_grain_op.hpp`
  - `alcedo_studio/src/edit/operators/cst/film_grain_op.cpp`
- serialized shape:
  - `{"film_grain": {"strength": 0.0}}`
- params:
  - `OperatorParams::FilmGrainParams`
  - `FusedOperatorParams::film_grain_`
  - `GPUOperatorParams::film_grain_`
  - `FusedParamsConverter::ConvertFromCPU(...)`
  - `CudaFusedParamUploader::Upload(...)`
- two-pass neighbor CUDA stage example:
  - `alcedo_studio/src/include/edit/operators/GPU_kernels/film_grain.cuh`
  - `alcedo_studio/src/edit/pipeline/pipeline_gpu_impl.cu`
- UI mapping:
  - `AdjustmentField::FilmGrain`
  - `pipeline_io::FieldSpec(...)`
  - `pipeline_io::ParamsForField(...)`
- `tone_control_panel_widget.cpp`

Halation should use matching conventions with `HalationParams`, `HalationOp`,
`OperatorType::HALATION`, `AdjustmentField::Halation`, and `{"halation": {"strength": 0.0}}`.
This is a separate operator family and handoff from film grain; those files are only nearby examples
of strength-only output-effect plumbing and two-pass neighbor execution.

## Intended CUDA Placement

Current relevant CUDA stream shape:

```text
GPU_PointChain(to_ws, exposure, contrast, tone)
GPU_HighlightShadowLocalToneStage
GPU_PointChain(curve, vibrance, wheel, hls, lmt, to_output)
GPU_FilmGrainBlurHorizontalKernel
GPU_FilmGrainApplyVerticalKernel
GPU_SharpenBlurHorizontalKernel
GPU_SharpenApplyVerticalKernel
GPU_ClarityBlurHorizontalKernel
GPU_ClarityApplyVerticalKernel
```

Target first-pass stream shape:

```text
GPU_PointChain(to_ws, exposure, contrast, tone)
GPU_HighlightShadowLocalToneStage
GPU_PointChain(curve, vibrance, wheel, hls, lmt, to_output)
GPU_SharpenBlurHorizontalKernel
GPU_SharpenApplyVerticalKernel
GPU_ClarityBlurHorizontalKernel
GPU_ClarityApplyVerticalKernel
GPU_HalationBlurHorizontalKernel
GPU_HalationApplyVerticalKernel
GPU_FilmGrainBlurHorizontalKernel
GPU_FilmGrainApplyVerticalKernel
```

Rationale:

- Halation should see the post-ODT/display encoded image because the reference thresholds are
  expressed in display-style `[0, 1]` values.
- Sharpen and clarity should run before halation and film grain so detail adjustment operates on the
  clean output image, not on the red glow or stochastic texture generated later.
- Halation should run before film grain so grain remains visible over the red glow rather than being
  blurred into the glow.

## Scratch Buffer Contract

Use the existing neighbor-stage ping-pong behavior:

1. Horizontal kernel reads the current image and writes the thresholded/redshifted horizontal blur to
   the next scratch buffer.
2. Vertical/apply kernel reads that scratch buffer, reads the original image from the destination
   side of the ping-pong pair, and writes the blended result back to the destination.
3. Disabled or zero-strength state must copy through exactly so it does not perturb later neighbor
   stages.

The implementation must not add a third full-size image just to preserve the original. Follow the
existing two-pass vertical apply pattern, where the vertical kernel reads `src` for the intermediate
blur and `dst` for the original pixel.

## Phased Plan

### Phase 1: Operator and Parameter Plumbing

- Add `OperatorType::HALATION`.
- Add `HalationOp` under the same CST/output-effect family as `FilmGrainOp`.
- Register it in the operator factory and `RegisterAllOperators()`.
- Add `pipeline_defaults::MakeDefaultHalationParams()`.
- Add `OperatorParams::HalationParams`.
- Add fused/GPU param fields and CPU-to-fused-to-CUDA upload wiring.
- Visible serialized shape:

```json
{
  "halation": {
    "strength": 0.0
  }
}
```

- Hidden defaults:

```text
low_threshold      = 0.6
high_threshold     = 0.7
sigma              = 20.0
redshift           = [1.0, 0.05, 0.02]
additive_scale     = 1.0
```

Acceptance tests:

- `HalationOpTest.DefaultParamsExposeOnlyStrength`
- `HalationOpTest.ReadsStrengthAndClampsToUserRange`
- `HalationOpTest.GlobalParamsWritesHiddenDefaultsAndNormalizedStrength`
- `HalationOpTest.EnableGlobalParamsTogglesHalationPayload`
- `HalationOpTest.FusedParamsCarryHalationPayload`
- `HalationOpTest.FactoryCreatesHalationOperator`

### Phase 2: UI and Pipeline Mapping

- Add `AdjustmentField::Halation`.
- Add editor state storage and copy/dirty tracking next to `film_grain_`.
- Add `pipeline_io::FieldSpec(AdjustmentField::Halation)`.
- Add `pipeline_io::ParamsForField(...)` with the strength-only serialized shape.
- Add project-load readback for both object shape and optional legacy numeric shape.
- Add a tone-panel slider in the same output-effect area as Film Grain, while keeping Halation as
  its own operator and handoff.
- Wire `dialog_pipeline.cpp` so HalationOp is installed into the output-effect stage while its CUDA
  kernels execute in the neighbor tail.

Acceptance tests:

- `EditorPipelineIoTest.HalationFieldTargetsOutputTransformOperator`
- `EditorPipelineIoTest.HalationParamsUseStrengthOnlySerializedShape`
- `EditorPipelineIoTest.HalationFieldChangedAndCopyFieldStateUseHalationValue`
- Project/load round-trip for `halation.strength`.

### Phase 3: CUDA Skeleton and Stream Integration

- Add a dedicated CUDA header:

```text
alcedo_studio/src/include/edit/operators/GPU_kernels/halation.cuh
```

- Add two neighbor kernels:

```text
GPU_HalationBlurHorizontalKernel
GPU_HalationApplyVerticalKernel
```

- Include `halation.cuh` from `pipeline_gpu_impl.cu`, before `film_grain.cuh`.
- Reorder the texture/detail tail so sharpen and clarity run before halation and film grain.
- Insert the two halation kernels after clarity and before film grain.
- First skeleton behavior:
  - strength zero: exact pass-through
  - positive strength: simple bright-pixel red additive signal without final tuning
  - no extra scratch allocation beyond the existing stream scratch buffers

Acceptance tests:

- `HalationCudaStageTest.StrengthZeroIsExactPassThroughAfterOdt`
- `HalationCudaStageTest.PositiveStrengthChangesDisplayEncodedOutputAfterOdt`
- `HalationCudaStageTest.DisabledStateDoesNotIncreaseScratchBufferCount`
- `HalationCudaStageTest.RunsAfterDetailAndBeforeFilmGrainInOutputTextureTail`

### Phase 4: Soft Threshold and Exponential Blur

- Implement the soft threshold helper.
- Implement normalized exponential weights derived from `sigma`.
- Clamp only the threshold input to `[0, 1]`.
- Preserve alpha.
- Ensure below-threshold regions produce no halo unless nearby blur bleeds into them.
- Ensure gradual ramps do not create a hard edge at `low` or `high`.

Acceptance tests:

- `HalationCudaStageTest.BelowLowThresholdIsExactPassThrough`
- `HalationCudaStageTest.AboveHighThresholdCreatesRedBiasedHalo`
- `HalationCudaStageTest.SoftThresholdRampIsMonotonic`
- `HalationCudaStageTest.HdrAndNegativeInputsStayFinite`
- `HalationCudaStageTest.AlphaIsPreserved`

### Phase 5: ROI and Preview Semantics

- Match the film-grain/detail-stage precedent: blur footprint is in the current output buffer.
- ROI/detail preview should use the current output buffer dimensions for the blur footprint, not a
  full-frame reference blur.
- Do not add H/S-style canonical reference caches for halation. This effect is a local post-output
  neighbor pass, not a scene-referred local-tone base.

Acceptance tests:

- ROI preview and same-size local render have the same halation footprint.
- Panning/zooming does not reuse a stale full-frame blur radius.
- Full-res export remains finite and uses the same output-space algorithm.

### Phase 6: Visual Tuning and Diagnostics

- Build a small diagnostic image set:
  - dark frame with one white point
  - bridge/window-like high-contrast edge
  - sky ramp around the 0.6-0.7 threshold range
  - saturated red/blue highlight samples
- Emit numeric checks and optional PNG/HTML diagnostics so visual review is not chat-image dependent.
- Compare:
  - Rec.709 luma threshold
  - max-channel threshold
  - separable exponential blur
  - true radial exponential blur on a small CPU/Python diagnostic
- Only after this phase, decide whether `additive_scale` should stay `1.0` or be reduced.

### Phase 7: Backend Parity Follow-Up

Do not mix this into the first CUDA patch unless explicitly requested.

After CUDA is stable:

- Mirror `HalationParams` into OpenCL and Metal parameter structs.
- Add OpenCL/Metal neighbor kernels using their existing detail-stage infrastructure.
- Keep the same hidden defaults and serialized shape.
- Add backend parity tests that compare the same small synthetic images with reasonable tolerances.

## Risks and Guardrails

- The source blur kernel is radial, while the first CUDA plan uses a separable approximation. Track
  this explicitly in tests/diagnostics so it does not become an accidental claim of exactness.
- Large `sigma = 20.0` implies many taps if implemented literally. Cap tap count through the same
  `kDetailMaxGaussianTapCount` infrastructure or add a documented fixed-radius truncation.
- Adding halation after display encoding can brighten/redshift already display-limited regions. This
  is expected for the first model, but regression images should include bright skies to catch
  excessive red wash.
- Do not omit halation inside the thresholded source area in the first implementation. The reference
  notes that this can reduce brightening but introduces edge artifacts.
- Keep `strength = 0` exact. This is the main safety valve for project compatibility.
- Keep serialized params strength-only. Hidden defaults should not leak into project files until the
  UI intentionally exposes them.

## Suggested Build and Test Commands

Use the repository MSVC/CUDA wrapper from the repo root:

```text
cmd /c scripts\msvc_env.cmd --build --preset win_debug --target HalationOpTest --parallel 4
ctest --test-dir build/debug -R HalationOpTest --output-on-failure

cmd /c scripts\msvc_env.cmd --build --preset win_debug --target HalationCudaStageTest --parallel 4
ctest --test-dir build/debug -R HalationCudaStageTest --output-on-failure

cmd /c scripts\msvc_env.cmd --build --preset win_debug --target EditorPipelineIoTest --parallel 4
ctest --test-dir build/debug -R EditorPipelineIoTest --output-on-failure

git diff --check
```

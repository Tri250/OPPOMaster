# CUDA Film Grain Handoff

This handoff captures the current film-grain work and the intended next implementation steps.
It is meant for the next agent that will implement the CUDA rendering stage.

## Scope

- Implement a film grain operator based on the pixel-wise algorithm from
  `D:/Courses/papers/Realistic Film Grain Rendering.pdf`.
- Backend scope for the next implementation step: CUDA first.
- User-facing control for now: `strength` only.
- Hidden algorithm parameters should keep defaults until the UI/product decision expands them.
- Avoid CUDA random-number helper libraries such as cuRAND. This GPLv3 project should keep the
  random generation logic local and auditable.

## Algorithm Summary

The paper models film grain as an inhomogeneous Boolean model.

The continuous image plane is filled with random opaque disk grains. For a point `p`, the Boolean
indicator is:

```text
1Z(p) = 1 if p is covered by at least one grain
1Z(p) = 0 otherwise
```

For an input channel value `u in [0, 1)`, choose a Poisson grain intensity:

```text
lambda = log(1 / (1 - u)) / E[pi * r^2]
```

For radius mean `mu_r` and radius standard deviation `sigma_r`:

```text
E[pi * r^2] = pi * (mu_r^2 + sigma_r^2)
```

This makes the probability of coverage match the input intensity:

```text
P(1Z(p) = 1) = 1 - exp(-lambda * E[pi * r^2]) = u
```

The observed output pixel is not a single Boolean evaluation. The paper applies a Gaussian
low-pass filter, approximated by Monte Carlo samples:

```text
v(y) = (1 / N) * sum_k 1Z((y + xi_k) / s)
xi_k ~ Normal(0, filter_sigma^2)
```

For this application, start with `s = 1` because the effect is applied after the ODT at the current
render resolution. If ROI rendering is active, map the current buffer pixel back to stable full-frame
image coordinates before generating random streams.

## Pixel-Wise Algorithm

The pixel-wise algorithm should not store all grains.

For each output pixel and each RGB channel:

1. Clamp the ODT/display encoded channel only for probability input:

   ```text
   u_prob = clamp(channel_value, 0, 1 - epsilon)
   ```

2. For each Monte Carlo offset `xi_k`, evaluate the Boolean model at the shifted continuous point.
3. To evaluate a point, inspect only neighboring cells within `r_m`, where `r_m` is the maximum
   supported grain radius.
4. For each cell:
   - compute local `lambda` from the source pixel/channel value for that cell
   - sample grain count `Q ~ Poisson(lambda)`
   - generate `Q` deterministic grain centers in the cell
   - generate each grain radius
   - return covered if any disk contains the query point
5. Average the `N` Boolean results.
6. Blend with the original display pixel:

   ```text
   out_rgb = mix(original_display_rgb, grain_rgb, strength / 100)
   ```

This is signal-dependent and spatially correlated. It should not be replaced by additive Gaussian
noise.

For color film grain, apply the same model independently to R, G, and B. Use distinct PRNG streams
per channel.

## Random Number Design

Current utility:

- `alcedo_studio/src/include/cuda/prng.hpp`
- `alcedo_studio/src/cuda/prng.cu`
- `alcedo_studio/tests/cuda/prng_test.cu`

The PRNG is counter-based:

```text
random_bits = CounterHash(seed, stream, counter)
```

It has no mutable state. This is important on GPU because each thread can directly compute its own
random numbers without sharing or storing generator state.

Suggested stream/counter mapping for film grain:

```text
stream  = PixelStream2D(full_frame_x, full_frame_y, channel)
counter = encoded(sample_index, cell_x, cell_y, grain_index, draw_kind)
```

Use `PixelStream2D(...)` with full-frame coordinates, not only current viewport coordinates, so
preview/ROI renders do not make grain swim when the user pans or zooms.

Available helpers:

- `CounterHash(seed, stream, counter)`
- `UniformFloat01(seed, stream, counter)`
- `UniformFloatOpen01(seed, stream, counter)`
- `NormalPair(seed, stream, counter)`
- `SamplePoisson(seed, stream, counter, lambda)`

For exact CPU/GPU comparisons, integer hash and uniform values should match exactly. Normal samples
may differ in the last bits because CPU and GPU math libraries evaluate `log/sqrt/cos/sin` slightly
differently; use a small tolerance.

## Current Code State

Already added:

- `alcedo_studio/src/include/cuda/prng.hpp`
- `alcedo_studio/src/cuda/prng.cu`
- `alcedo_studio/tests/cuda/prng_test.cu`
- `CudaUtils` target in `alcedo_studio/src/CMakeLists.txt`
- `CudaPrngTest` target in `alcedo_studio/tests/CMakeLists.txt`

Already added for the operator parameter entry:

- `alcedo_studio/src/include/edit/operators/cst/film_grain_op.hpp`
- `alcedo_studio/src/edit/operators/cst/film_grain_op.cpp`
- `alcedo_studio/tests/edit/operators/cst/film_grain_op_test.cpp`
- `OperatorType::FILM_GRAIN`
- factory string mapping for `FILM_GRAIN`
- `RegisterAllOperators()` entry for `FilmGrainOp`
- default params via `pipeline_defaults::MakeDefaultFilmGrainParams()`
- clean-baseline and template-pipeline insertion into `Output_Transform`

Current serialized parameter shape:

```json
{
  "film_grain": {
    "strength": 0.0
  }
}
```

Current hidden defaults in `FilmGrainOp::HiddenDefaults`:

```text
monte_carlo_samples = 32
mean_radius         = 0.08
radius_stddev       = 0.04
filter_sigma        = 0.8
seed                = 0x6a09e667f3bcc909
```

Important current limitation:

- `FilmGrainOp::SetGlobalParams(...)` and `EnableGlobalParams(...)` are intentionally no-ops.
- No `OperatorParams` / `GPUOperatorParams` film-grain payload exists yet.
- No CUDA render stage is wired into `pipeline_gpu_impl.cu` yet.

The user's instruction for this checkpoint was: if the global params do not have a field yet, leave
them empty. Do not invent a temporary unrelated global-param path.

## Intended CUDA Integration

Add an explicit film-grain parameter group when implementing the render stage.

Likely additions:

- `OperatorParams::FilmGrainParams`
- matching `FusedOperatorParams` fields
- matching `GPUOperatorParams` fields
- converter wiring in `fused_param.hpp` / `param.cuh`

Minimum payload:

```cpp
struct FilmGrainParams {
  bool enabled = false;
  float strength = 0.0f;          // normalized 0..1 for CUDA
  int samples = 32;
  float mean_radius = 0.08f;
  float radius_stddev = 0.04f;
  float filter_sigma = 0.8f;
  float max_radius = ...;         // quantile or conservative default
  float cell_size = ...;          // paper uses a subdivision tied to mu_r
  std::uint64_t seed = ...;
};
```

Then update:

- `FilmGrainOp::SetGlobalParams(...)`
- `FilmGrainOp::EnableGlobalParams(...)`
- `FusedParamsConverter::ConvertFromCPU(...)`
- `CudaFusedParamUploader` only if device buffers are added
- CUDA kernel param structs

## Where To Put The CUDA Stage

The current CUDA merged stream is assembled in:

- `alcedo_studio/src/edit/pipeline/pipeline_gpu_impl.cu`

Current relevant chain:

```text
GPU_PointChain(curve, vib, wheel, hls, lmt, to_out)
```

`GPU_OUTPUT_Kernel` in:

- `alcedo_studio/src/include/edit/operators/GPU_kernels/cst.cuh`

already does:

```text
ACEScc decode -> ACES/OpenDRT ODT -> DisplayEncoding
```

The film-grain stage should run after this, in display/output space:

```text
GPU_PointChain(curve, vib, wheel, hls, lmt, to_out)
GPU_FilmGrainPixelWiseStage
```

Do not put the film-grain math inside ODT color management functions. Keep ODT deterministic and
make grain a post-ODT output effect.

Because the algorithm needs pixel coordinates, implement it as a custom-dispatch stage rather than
a plain `GPUPointOpTag`. See `GPU_StaticKernelStream::HasCustomDispatch` support in:

- `alcedo_studio/src/include/edit/pipeline/kernel_stream_gpu.cuh`

## Suggested File Placement

Prefer a dedicated CUDA grain header:

- `alcedo_studio/src/include/edit/operators/GPU_kernels/film_grain.cuh`

Then include it from:

- `alcedo_studio/src/edit/pipeline/pipeline_gpu_impl.cu`

This keeps film grain out of `cst.cuh` and avoids making ODT own a stochastic post effect.

## First Implementation Plan

Phase 1: parameter plumbing

- Add film-grain fields to `OperatorParams`, `FusedOperatorParams`, and `GPUOperatorParams`.
- Change `FilmGrainOp::SetGlobalParams(...)` to write normalized strength and hidden defaults.
- Change `EnableGlobalParams(...)` to toggle the enabled bit.
- Add tests that `FilmGrainOp` writes the expected global params.

Phase 2: deterministic CUDA skeleton

- Add `GPU_FilmGrainPixelWiseStage` custom dispatch.
- Initially implement a cheap placeholder using `UniformFloat01` and `strength` only.
- Verify stage ordering by proving `strength = 0` is exact pass-through and `strength > 0` changes
  output after ODT.

Phase 3: paper model

- Replace placeholder with the pixel-wise Boolean model.
- Start with constant radius first.
- Add log-normal radius only after constant-radius behavior is stable.
- Use fixed CPU-generated or param-generated Gaussian offsets if needed, but avoid giant random
  buffers. The PRNG should generate per-cell grain data on demand.

Phase 4: ROI stability

- Use `render_roi_enabled_`, `render_roi_x_`, `render_roi_y_`, `render_roi_scale_x_`,
  `render_roi_scale_y_`, `render_roi_reference_width_`, and `render_roi_reference_height_` to map
  current output coordinates back to full-frame coordinates.
- Add a regression test or diagnostic that renders overlapping ROI/full-frame regions and verifies
  grain anchoring is stable.

## Tests Already Run

PRNG:

```text
cmd /c scripts\msvc_env.cmd --build --preset win_debug --target CudaPrngTest --parallel 4
ctest --test-dir build/debug -R CudaPrngTest --output-on-failure
```

Result:

```text
CudaPrngTest: 5/5 passed
```

FilmGrainOp parameter entry:

```text
cmd /c scripts\msvc_env.cmd --build --preset win_debug --target FilmGrainOpTest --parallel 4
ctest --test-dir build/debug -R FilmGrainOpTest --output-on-failure
```

Result:

```text
FilmGrainOpTest: 5/5 passed
```

Hygiene:

```text
git diff --check
```

Result:

```text
clean
```

## Suggested Future Tests

Unit/contract tests:

- default film-grain params expose only `strength`
- hidden defaults are stable
- global params are populated once `OperatorParams` fields exist
- strength clamp remains `[0, 100]`

CUDA numerical tests:

- `strength = 0` is exact pass-through
- fixed seed is deterministic across repeated launches
- different seed changes output
- constant gray image has average grain close to input gray after enough samples
- R/G/B channels use independent streams
- no NaN/Inf output for black, mid-gray, white, and HDR-ish ODT output

Pipeline tests:

- ODT + film grain runs after display encoding
- output remains finite for both OpenDRT and ACES 2.0 ODT methods
- exported/project params round-trip `film_grain.strength`

ROI tests:

- same full-frame coordinate should generate same grain when rendered through full frame and ROI
- panning/zooming should not reseed the visible grain pattern

## Things To Avoid

- Do not use cuRAND for this work.
- Do not implement this as additive Gaussian noise.
- Do not put random/stochastic behavior inside `OutputTransform_fwd`, `OpenDRTTransform_fwd`, or
  `DisplayEncoding`.
- Do not clamp scene-referred ACEScc/AP1 values for this feature. Only clamp the already ODT/display
  encoded value when converting it to a probability input for the Boolean model.
- Do not make OpenCL/Metal changes in the first CUDA implementation unless explicitly requested.

## Useful Source Anchors

- `D:/Courses/papers/Realistic Film Grain Rendering.pdf`
- `build/diagnostics/realistic_film_grain_rendering.txt` if regenerated with `pdftotext`
- `alcedo_studio/src/include/cuda/prng.hpp`
- `alcedo_studio/src/include/edit/operators/cst/film_grain_op.hpp`
- `alcedo_studio/src/edit/pipeline/pipeline_gpu_impl.cu`
- `alcedo_studio/src/include/edit/operators/GPU_kernels/cst.cuh`
- `alcedo_studio/src/include/edit/pipeline/kernel_stream_gpu.cuh`

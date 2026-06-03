# CUDA Highlight/Shadow LLF Handoff

This note is a work-in-progress handoff for the CUDA-only highlight/shadow local-tone rewrite.
It is not merge-ready documentation. The goal is to let the next agent continue from the current
state without repeating the earlier dead ends.

## Scope And Hard Constraints

- Target file: `alcedo_studio/src/include/edit/operators/GPU_kernels/color.cuh`
- Backend scope: CUDA only for this task
- Do not spend time on OpenCL for this round
- Follow the LLF paper and measured diagnostics, not old heuristics or visual intuition
- Use the provided RAW and PNG references as the primary regression set
- Default research export must not use a CUBE LUT unless explicitly requested

## Current Working Tree State

Modified:

- `alcedo_studio/src/include/edit/operators/GPU_kernels/color.cuh`
- `alcedo_studio/tests/CMakeLists.txt`

Untracked:

- `alcedo_studio/tests/app/hs_research_export_tool.cpp`
- `scripts/diagnose_hs_reference_compare.py`
- `scripts/diagnose_hs_sky_ev_cases.py`
- `scripts/tune_hs_joint_curve.py`

## What Changed

### 1. The active CUDA path was structurally rewritten around LLF-style log intensity

The current experimental path in `color.cuh` no longer follows the earlier
`OKLab L -> modify L -> keep a/b` strategy.

Key direction now in code:

- Scalar field is AP1 intensity encoded in ACEScc-style log space
- Reconstruction is ratio-preserving AP1 rescaling
- A lower-gamut fit is still applied during reconstruction
- LLF detail alpha is fixed at `1.0`
- Gamma interpolation between remap samples is linear, not smoothstep-based
- `sigma_r` is fixed near the paper value for tone mapping: about `log(2.5)` in the log domain
- Highlight amount now maps from `-params.highlights_offset_` directly, without the old `* 0.5`

Useful anchors in the file:

- `hs_apply_reference_curve(...)`
- `hs_llf_detail_alpha(...)`
- `BuildSamples(...)`
- `GPU_HighlightShadowLocalToneStage::Dispatch(...)`

The main code hotspot for the next agent is still `hs_apply_reference_curve(...)`. The current
global target curve is the biggest remaining quality bottleneck.

### 2. The old bilateral-style or mask-heavy intuition should be treated as superseded

Earlier exploration in this area produced a lot of partial heuristics. The user explicitly asked
not to keep steering from those branches. The current branch should stay on:

- log intensity
- LLF-style sampled remap pyramid
- ratio-preserving color rebuild

If a future tweak does not fit that mental model, it should be treated with suspicion.

### 3. A standalone export tool was added for reference-study work

Files:

- `alcedo_studio/tests/app/hs_research_export_tool.cpp`
- `alcedo_studio/tests/CMakeLists.txt`

Purpose:

- import the user-provided RAWs
- apply a controlled baseline pipeline
- force CUDA backend
- export 16-bit PNG outputs for direct comparison

Important behavior:

- default is no LUT
- the old default LUT path is only used when `--default-lut` is passed

This matters because the user explicitly asked to avoid cube-based default exports during research.

### 4. A reference-compare script was added

File:

- `scripts/diagnose_hs_reference_compare.py`

Purpose:

- compare current exports against the supplied reference PNGs
- compute tone, chroma, shadow-noise, halo, and smooth-break metrics
- save summary images and crop sheets

Important implementation detail:

- it resizes current output to match the reference if dimensions differ

This avoids false negatives caused only by size mismatch.

## Paper Takeaways Used In The Current Rewrite

The LLF paper excerpt used during this round is in:

- `build/diagnostics/Paris_11_Local_Laplacian_Filters_lowres.txt`

The key takeaways that directly informed the current rewrite:

- tone mapping should operate on log intensity
- color should be reconstructed from RGB or color ratios relative to intensity
- tone-mapping path can use `alpha = 1`
- `sigma_r` should be treated as a fixed log-domain contrast scale
- the paper cites about `log(2.5)` as a practical tone-mapping value

These points are the main reason the implementation moved away from the old OKLab-lightness-only
editing path.

## Reference Inputs

RAWs:

- `D:\素材\照片\2026.5香港\香港2026-5-12-晚\P2625410.RW2`
- `D:\素材\照片\2026.5.29滇池\P2635785.RW2`
- `D:\素材\照片\2026.5.29滇池\P2635855.RW2`

Reference PNGs:

- `D:\素材\照片\2026-6-3-reference\P2625410_shadow_plus_100_highlight_minus_100.png`
- `D:\素材\照片\2026-6-3-reference\P2635785_shadow_plus_100_highlight_minus_100.png`
- `D:\素材\照片\2026-6-3-reference\P2635855_shadow_plus_100_highlight_minus_100.png`

Mandatory regression case:

- `shadow +100 / highlight -100`

## Current Outputs And Diagnostics

Current no-LUT exports:

- `build/diagnostics/hs_reference_exports/current_cuda_llf_no_lut`

Baseline no-LUT exports:

- `build/diagnostics/hs_reference_exports/baseline_default_no_lut`

Latest compare bundle:

- `build/diagnostics/hs_reference_compare/current_cuda_llf_no_lut`

Latest aggregate metrics from `summary.md`:

- `mean_luma_mae: 0.048006`
- `mean_chroma_mae: 0.010380`
- `mean_highlight_chroma_ratio: 0.668332`
- `mean_shadow_noise_ratio: 0.795799`
- `mean_halo_score: 0.078284`
- `mean_smooth_break_score: 0.009460`

Per-image read:

- `P2625410`: directionally improved and closest to acceptable
- `P2635785`: still not converged
- `P2635855`: still clearly wrong; too compressed, too desaturated, gradients too weak

Important interpretation:

- the current rewrite fixed structural issues
- it did not finish the visual match
- the remaining miss now looks more like a target-curve and reconstruction balance problem than a
  "LLF was the wrong direction" problem

## What Seems Wrong Right Now

### 1. The global target curve is too blunt

The current `hs_apply_reference_curve(...)` is too coarse on hard real scenes. It tends to:

- flatten gradients in difficult sky/cloud cases
- underdeliver highlight reduction where it should still push harder
- suppress highlight chroma after ratio reconstruction plus gamut fit

### 2. The reconstruction is structurally better but still not yet visually matched

The ratio-preserving AP1 rebuild is more faithful to the paper than the old OKLab-lock path, but:

- in some bright cloud and sun-adjacent regions, chroma falls too much
- once the curve pushes harder, lower-gamut fitting may be helping stability while also damping color

This should be investigated only after the target curve is improved, not before.

### 3. Some older scripts are useful as probes, not as truth

- `scripts/diagnose_hs_sky_ev_cases.py`
- `scripts/tune_hs_joint_curve.py`

They are still useful for numeric study, but they contain older highlight/shadow modeling ideas.
They should not be copied back into the CUDA kernel as-is. Use them to inspect slope, reversal, and
EV behavior only.

## Verified Commands

Build:

```bat
cmd /c scripts\msvc_env.cmd --build --preset win_debug --target EditPipeline HsResearchExportTool --parallel 4
```

Current no-LUT export:

```bat
D:\Projects\pu-erh_lab\build\debug\alcedo_studio\tests\HsResearchExportTool.exe --out-dir D:\Projects\pu-erh_lab\build\diagnostics\hs_reference_exports\current_cuda_llf_no_lut "D:\素材\照片\2026.5香港\香港2026-5-12-晚\P2625410.RW2" "D:\素材\照片\2026.5.29滇池\P2635785.RW2" "D:\素材\照片\2026.5.29滇池\P2635855.RW2"
```

Baseline no-LUT export:

```bat
D:\Projects\pu-erh_lab\build\debug\alcedo_studio\tests\HsResearchExportTool.exe --out-dir D:\Projects\pu-erh_lab\build\diagnostics\hs_reference_exports\baseline_default_no_lut --shadow 0 --highlight 0 "D:\素材\照片\2026.5香港\香港2026-5-12-晚\P2625410.RW2" "D:\素材\照片\2026.5.29滇池\P2635785.RW2" "D:\素材\照片\2026.5.29滇池\P2635855.RW2"
```

Reference compare:

```bat
python scripts/diagnose_hs_reference_compare.py --actual-dir D:\Projects\pu-erh_lab\build\diagnostics\hs_reference_exports\current_cuda_llf_no_lut --reference-dir D:\素材\照片\2026-6-3-reference --out-dir D:\Projects\pu-erh_lab\build\diagnostics\hs_reference_compare\current_cuda_llf_no_lut
```

Suggested focused tests after the next real kernel iteration:

```bat
ctest --test-dir build\debug --output-on-failure -R "PipelineFrameSinkTest\.(ActiveCudaHighlightShadowKeepsDetailRoiPreviewAsPatch|ActiveCudaHighlightShadowKeepsFastPreviewAsRoiFrame|RenderSourceCacheKeyUsesStableImageIdentityBeforeBufferPointer)|ODTCudaSmokeTest\."
```

## Best Next Steps

1. Keep the current `log intensity + LLF sample pyramid + ratio-preserving rebuild` architecture.
2. Rework `hs_apply_reference_curve(...)` first. That is the highest-value next change.
3. Re-export the three RAWs with no LUT and rerun `diagnose_hs_reference_compare.py`.
4. Check whether the current gamma sample upper bound `kGammaMaxL = 1.0f` is too low for the
   brightest clouds and clipped highlights.
5. Revisit lower-gamut fitting only if highlight chroma is still too suppressed after the curve is
   improved.
6. Keep the regression priority on:
   - local contrast
   - highlight reduction strength
   - highlight chroma
   - halo score
   - smooth break or banding score
   - shadow noise ratio

## One-Sentence State Summary

The rewrite is on the right architectural track now, but it is still unfinished, and the next
agent should treat `hs_apply_reference_curve(...)` as the main remaining problem rather than
throwing away the LLF-based log-intensity pipeline.

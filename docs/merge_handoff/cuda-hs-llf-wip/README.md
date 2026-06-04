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
- `alcedo_studio/tests/app/hs_research_export_tool.cpp`
- `scripts/diagnose_hs_reference_compare.py`

Useful generated outputs:

- `build/diagnostics/hs_reference_exports/current_cuda_llf_shadow_mid_v10_alpha_only_no_lut`
- `build/diagnostics/hs_reference_compare/current_cuda_llf_shadow_mid_v10_alpha_only_no_lut`
- `build/diagnostics/hs_reference_exports/current_cuda_llf_shadow_mid_v5_sat45_no_lut`
- `build/diagnostics/hs_reference_compare/current_cuda_llf_shadow_mid_v5_sat45_no_lut`

Existing probe scripts:

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
- LLF detail alpha uses guarded deep-shadow noise suppression plus a narrow mid-shadow detail boost
- Gamma interpolation between remap samples is linear, not smoothstep-based
- `sigma_r` is fixed near the paper value for tone mapping: about `log(2.5)` in the log domain
- Highlight amount now maps from `-params.highlights_offset_` directly, without the old `* 0.5`
- Gamma sample coverage now extends to `kGammaMaxL = 1.18f`

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
- `--saturation` can override the research export saturation slider for diagnosis; the default
  remains `pipeline_defaults::kCleanBaselineSaturation`

This matters because the user explicitly asked to avoid cube-based default exports during research.

### 4. A reference-compare script was added

File:

- `scripts/diagnose_hs_reference_compare.py`

Purpose:

- compare current exports against the supplied reference PNGs
- compute tone, chroma, shadow-noise, halo, and smooth-break metrics
- compute luma-normalized chroma ratios so chroma misses can be separated from tone misses
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

- `build/diagnostics/hs_reference_exports/p2625410_bottom_people_v26`
- older retained multi-image baseline:
  `build/diagnostics/hs_reference_exports/current_cuda_llf_shadow_mid_v10_alpha_only_no_lut`

Baseline no-LUT exports:

- `build/diagnostics/hs_reference_exports/baseline_default_no_lut`

Latest compare bundle:

- `build/diagnostics/hs_reference_compare/p2625410_bottom_people_v26`
- older retained multi-image baseline:
  `build/diagnostics/hs_reference_compare/current_cuda_llf_shadow_mid_v10_alpha_only_no_lut`

Latest accepted P2625410 v26 metrics from `summary.md` / `metrics.json`:

- `luma_mae: 0.021562`
- `luma_p95_abs: 0.046727`
- `luma_median_delta: 0.019158`
- `chroma_mae: 0.007737`
- `highlight_chroma_ratio: 0.800326`
- `luma_normalized_chroma_ratio: 0.813414`
- `shadow_noise_ratio: 1.945619` (accepted for this round; noise was explicitly deprioritized)
- `gradient_p90_ratio: 0.701866`
- `halo_score: 0.032054`
- `smooth_break_score: 0.004280`

New bottom-people patch diagnostic for P2625410:

- patch: `bottom_people`
- rect: `26,716,851,1306` in the resized 868 x 1326 reference frame
- output sheet:
  `build/diagnostics/hs_reference_compare/p2625410_bottom_people_v26/P2625410_shadow_plus_100_highlight_minus_100_patches.png`
- `luma_mae: 0.013103`
- `luma_p95_abs: 0.039086`
- `luma_mean_delta: 0.000259`
- `luma_hist_intersection: 0.793512`
- `luma_hist_emd: 0.006858`

Relative to v24 (`p2625410_pivot_dark_v24`) on P2625410:

- whole-image luma MAE improved: `0.021744 -> 0.021562`
- bottom-people patch luma MAE improved: `0.013146 -> 0.013103`
- bottom-people patch luma histogram intersection improved: `0.785461 -> 0.793512`
- bottom-people patch luma histogram EMD improved: `0.007062 -> 0.006858`
- shadow noise ratio regressed: `1.851725 -> 1.945619`; this was accepted because the round
  intentionally prioritized luma similarity over noise

Older v10 aggregate metrics from `summary.md` / `metrics.json`:

- `mean_luma_mae: 0.047959`
- `mean_chroma_mae: 0.010148`
- `mean_highlight_chroma_ratio: 0.668828`
- `mean_luma_normalized_chroma_ratio: 0.793158`
- `mean_shadow_noise_ratio: 0.758361`
- `mean_gradient_p90_ratio: 0.637618`
- `mean_halo_score: 0.078595`
- `mean_smooth_break_score: 0.009481`

Relative to the previous retained v6 (`current_cuda_llf_shadow_mid_v6_alpha_guard_no_lut`):

- luma MAE is effectively flat but slightly worse: `+0.000048`
- chroma MAE is effectively flat but slightly worse: `+0.000024`
- mean highlight chroma ratio is flat: `+0.000013`
- shadow noise improves: `0.784608 -> 0.758361`
- gradient p90 improves: `0.633289 -> 0.637618`
- halo and smooth-break are very slightly worse

Relative to `current_cuda_llf_sampling_v2_no_lut`:

- luma MAE is effectively flat but slightly worse: `+0.000108`
- chroma MAE improves: `-0.000229`
- mean chroma ratio improves: `+0.017477`
- shadow noise improves: `0.789429 -> 0.758361`
- gradient p90 improves: `0.614537 -> 0.637618`
- halo and smooth-break are very slightly worse

Per-image read:

- `P2625410`: still closest; v10 reduces shadow-noise versus v6, with a tiny luma-MAE cost
- `P2635785`: gradient and shadow-noise improve slightly, but tone/chroma are still not converged
- `P2635855`: still too dark/desaturated, but local gradient and shadow-noise move in the right
  direction versus v6

Important interpretation:

- the current rewrite fixed structural issues
- extending gamma coverage and widening `beta` are small but reliable improvements
- stronger all-shadow lift improves `P2635855` but regresses `P2625410` shadow noise; avoid broad
  dark-region lift
- the retained v10 compromise keeps the v6 shadow curve, strengthens deep-shadow `alpha` guarding,
  and adds a narrow mid-shadow detail boost instead of adding more global lift
- v7/v8 curve-lift probes improved `P2635855` more, but were not retained because they raised
  `P2625410` shadow noise and did not improve aggregate luma
- the remaining miss still looks like a target-curve/detail-balance problem rather than "LLF was
  the wrong direction"

## What Seems Wrong Right Now

### 1. The global target curve is too blunt

The current `hs_apply_reference_curve(...)` is too coarse on hard real scenes. It tends to:

- flatten gradients in difficult sky/cloud cases
- underdeliver local separation in difficult mid-shadow/sky-water cases
- need mid-shadow help without increasing true black noise

### 2. Some of the chroma miss is not H/S-specific

The `--saturation 45` diagnostic export improves luma-normalized chroma substantially without
meaningfully moving luma metrics:

- `current_cuda_llf_shadow_mid_v5_sat45_no_lut` raised mean highlight chroma ratio from about
  `0.669` to about `0.749`
- it did not fix tone placement or local contrast
- do not force the H/S curve to solve all reference chroma mismatch; verify the intended color
  baseline before adding H/S-specific chroma compensation

### 3. The reconstruction is structurally better but still not yet visually matched

The ratio-preserving AP1 rebuild is more faithful to the paper than the old OKLab-lock path, but:

- in some bright cloud and sun-adjacent regions, chroma is still low versus reference
- lower-gamut fitting was probed with a softer H/S-only lower bound and did not move the metrics on
  this reference set, so it is not the current primary culprit

### 4. Some older scripts are useful as probes, not as truth

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
D:\Projects\pu-erh_lab\build\debug\alcedo_studio\tests\HsResearchExportTool.exe --out-dir D:\Projects\pu-erh_lab\build\diagnostics\hs_reference_exports\current_cuda_llf_shadow_mid_v10_alpha_only_no_lut "D:\素材\照片\2026.5香港\香港2026-5-12-晚\P2625410.RW2" "D:\素材\照片\2026.5.29滇池\P2635785.RW2" "D:\素材\照片\2026.5.29滇池\P2635855.RW2"
```

Saturation diagnostic export:

```bat
D:\Projects\pu-erh_lab\build\debug\alcedo_studio\tests\HsResearchExportTool.exe --out-dir D:\Projects\pu-erh_lab\build\diagnostics\hs_reference_exports\current_cuda_llf_shadow_mid_v5_sat45_no_lut --saturation 45 "D:\素材\照片\2026.5香港\香港2026-5-12-晚\P2625410.RW2" "D:\素材\照片\2026.5.29滇池\P2635785.RW2" "D:\素材\照片\2026.5.29滇池\P2635855.RW2"
```

Baseline no-LUT export:

```bat
D:\Projects\pu-erh_lab\build\debug\alcedo_studio\tests\HsResearchExportTool.exe --out-dir D:\Projects\pu-erh_lab\build\diagnostics\hs_reference_exports\baseline_default_no_lut --shadow 0 --highlight 0 "D:\素材\照片\2026.5香港\香港2026-5-12-晚\P2625410.RW2" "D:\素材\照片\2026.5.29滇池\P2635785.RW2" "D:\素材\照片\2026.5.29滇池\P2635855.RW2"
```

Reference compare:

```bat
python scripts/diagnose_hs_reference_compare.py --actual-dir D:\Projects\pu-erh_lab\build\diagnostics\hs_reference_exports\current_cuda_llf_shadow_mid_v10_alpha_only_no_lut --reference-dir D:\素材\照片\2026-6-3-reference --out-dir D:\Projects\pu-erh_lab\build\diagnostics\hs_reference_compare\current_cuda_llf_shadow_mid_v10_alpha_only_no_lut --files P2625410_shadow_plus_100_highlight_minus_100.png P2635785_shadow_plus_100_highlight_minus_100.png P2635855_shadow_plus_100_highlight_minus_100.png
```

Suggested focused tests after the next real kernel iteration:

```bat
ctest --test-dir build\debug --output-on-failure -R "PipelineFrameSinkTest\.(ActiveCudaHighlightShadowKeepsDetailRoiPreviewAsPatch|ActiveCudaHighlightShadowKeepsFastPreviewAsRoiFrame|RenderSourceCacheKeyUsesStableImageIdentityBeforeBufferPointer)|ODTCudaSmokeTest\."
```

## Best Next Steps

1. Keep the current `log intensity + LLF sample pyramid + ratio-preserving rebuild` architecture.
2. Continue reworking `hs_apply_reference_curve(...)`, but avoid broad dark-region lift because it
   regresses shadow-noise badly.
3. Re-export the three RAWs with no LUT and rerun `diagnose_hs_reference_compare.py`.
4. Treat `kGammaMaxL = 1.18f` and the wider `beta` clamp as the current small positive baseline.
5. Verify the intended color baseline before adding H/S-specific chroma compensation; saturation
   diagnostics show much of the chroma gap is outside the H/S scalar curve.
6. Revisit lower-gamut fitting only if a future curve change makes it measurable on the reference
   set; the soft lower-bound probe did not move current metrics.
7. Keep the regression priority on:
   - local contrast
   - highlight reduction strength
   - highlight chroma
   - halo score
   - smooth break or banding score
   - shadow noise ratio

## One-Sentence State Summary

The rewrite is still unfinished, but v10 is the best measured CUDA-only compromise so far: it keeps
the LLF log-intensity architecture, preserves the v6 shadow target curve, and improves noise/detail
balance through deep-shadow alpha guarding plus a narrow mid-shadow detail boost.

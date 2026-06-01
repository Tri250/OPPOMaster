# fix/color_adj_overhaul Merge Handoff

This folder records the implementation notes, experiment history, and merge risks for the
`fix/color_adj_overhaul` branch. It is written as a main-branch handoff: reviewers should be able
to understand what changed, why it changed, and which behavior should be preserved during merge
conflict resolution.

Baseline used for this handoff:

- Branch: `fix/color_adj_overhaul`
- Merge base against `main`: `8eed4a04ac3cbc91ab0a65de0e594efe30f9544c`
- Head at time of writing: `839f2e1f`
- Scope: color adjustment overhaul, H/S local tone mapping, GPU backend parity, preview ROI
  behavior, direct viewer presentation, diagnostics, and focused regression coverage.

## Branch Themes

The branch is not a single isolated shader tweak. It is a linked set of changes around interactive
color adjustment quality:

1. The HLS and saturation tools were moved toward an OKLab/OKLCh-style perceptual adjustment
   model so hue, lightness, and chroma edits can be shaped without the old RGB/HLS artifacts.
2. The old highlight and shadow point kernels were replaced in the accelerated path by a
   scene-referred local-tone stage that builds a log-luminance base, applies highlight/shadow
   edits against that base, and preserves or compensates local detail.
3. CUDA was the first source of truth for the new local-tone math; OpenCL and Metal were then
   brought into parity with staged fused pipelines, base-cache reuse, and equivalent shader logic.
4. Preview scheduling and viewer presentation were updated so local-tone neighborhood context is
   stable while zoomed detail previews remain sharp and responsive.
5. Diagnostic Python scripts were added to make the algorithm tuning repeatable instead of relying
   only on screenshot inspection.
6. Regression tests were expanded around shared tone curves, crop/resize ROI behavior, frame sink
   detachment, detail preview sizing, direct presentation queue behavior, and viewport mapping.

## High-Level Change Map

Main implementation surfaces:

- `alcedo_studio/src/include/edit/operators/GPU_kernels/color.cuh`
  - CUDA source of truth for H/S local tone, OKLab/OKLCh helpers, HLS, vibrance, and saturation
    behavior.
- `alcedo_studio/src/edit/pipeline/pipeline_gpu_impl.cu`
  - Splits the CUDA stream into pre-H/S point ops, `GPU_HighlightShadowLocalToneStage`, and
    post-H/S point ops.
- `alcedo_studio/src/edit/pipeline/opencl_shader/color.cl`
  - Mirrors the CUDA H/S and HLS logic for OpenCL.
- `alcedo_studio/src/edit/pipeline/opencl_shader/edit_pipeline_detail.cl`
  - Adds OpenCL kernels for H/S log-base construction and local-tone application.
- `alcedo_studio/src/edit/pipeline/pipeline_opencl_impl.cpp`
  - Adds staged OpenCL execution: pre-H/S fused ops, cached base/apply, post-H/S fused ops, then
    detail stages.
- `alcedo_studio/src/edit/operators/GPU_kernels/metal_shader/color.metal`
  - Mirrors CUDA/OpenCL color and H/S local-tone logic for Metal.
- `alcedo_studio/src/edit/pipeline/metal_shader/fused_pipeline.metal`
  - Adds Metal staged fused kernels and local-tone apply support.
- `alcedo_studio/src/edit/pipeline/pipeline_metal_impl.cpp`
  - Adds Metal base-cache resources, staged dispatch, performance reporting, and resource reuse.
- `alcedo_studio/src/edit/operators/color/HLS_op.cpp`
  - Updates the CPU/operator parameter side for multi-profile OKLab/OKLCh-like HLS behavior.
- `alcedo_studio/src/include/edit/operators/CPU_kernels/color/HLS_kernel.hpp`
  - Mirrors the new HLS point-kernel semantics for CPU/testable paths.
- `alcedo_studio/src/edit/operators/color/saturation_op.cpp`
  - Converts saturation from an independent point kernel into the HLS/fused color payload.
- `alcedo_studio/src/renderer/pipeline_scheduler.cpp`
  - Updates render-region scheduling, detail ROI handling, and frame sink ownership.
- `alcedo_studio/src/ui/edit_viewer/*`
  - Updates direct GPU presentation, detail-patch compositing, viewport mapping, and metadata
    routing.
- `scripts/diagnose_hs_local_tone.py`
- `scripts/diagnose_hs_shadow_detail.py`
- `scripts/diagnose_hs_tone_response.py`
  - Reproducible numerical/visual diagnostics for the H/S local-tone iterations.

## Commit Sequence

The branch contains these commits after the merge base:

- `892e34b3` - migrate to OKLCh-oriented color math.
- `6d91abed` - refactor the HLS operator for enhanced chroma adjustments.
- `4ea033bb` - fuse saturation into the OKLCh/HLS tool path.
- `2b232fc4` - add enhanced shadows/highlights local-tone processing.
- `a38aa24e` - refine CUDA local tone for new shadow/highlight behavior.
- `9a099480` - add point-to-point zoom-in preview scaling.
- `4001c1c9` - fix ROI request failures during H/S and geometry adjustment.
- `ede5318f` - improve shadow adjustment quality.
- `1bf39fd6` - add OpenCL LLF tone mapping.
- `6ed3ca72` - improve tone-mapping regularization.
- `5c9a6926` - further improve shadow contrast.
- `24e8a8a9` - add more detail to shadow regions.
- `66df535b` - mitigate fringe abnormality.
- `ba99cae8` - update macOS install script.
- `02d14710` - add macOS/Metal support for local tone mapping.
- `839f2e1f` - improve performance and accuracy on the Metal path.

## Handoff Documents

- [technical-design.md](technical-design.md): detailed implementation design and backend-specific
  notes.
- [experiments-and-references.md](experiments-and-references.md): tuning history, diagnostic
  script design, reference papers, and validation notes.

## Merge Checklist

Before merging into `main`, preserve the following invariants:

- Keep H/S local tone after exposure/contrast/tone and before curve/vibrance/HLS/LMT/output.
- Keep base-cache keys independent of live H/S slider values where possible, so slider dragging does
  not rebuild the base every frame.
- Keep CUDA, OpenCL, and Metal parameter layouts synchronized with their shader structs.
- Do not add `0..1` clamps inside scene-referred AP1, log-luminance, ACEScc-adjacent, or OKLab
  paths unless the output transform explicitly requires it.
- Preserve viewport reference dimensions and ROI metadata; local-tone base construction depends on
  stable source-neighborhood context.
- Keep detail-patch rendering tokenized by preview generation and detail serial so stale detail
  frames cannot overwrite newer full-frame previews.
- Keep OpenCL detail-program registration in the manifest-driven program library path.
- For Windows validation, use `cmd /c scripts\msvc_env.cmd ...` rather than bare `cmake`.

## Suggested Post-Merge Validation

On Windows/MSVC:

```bat
cmd /c scripts\msvc_env.cmd --build --preset win_debug --target EditPipeline --parallel 4
cmd /c scripts\msvc_env.cmd --build --preset win_debug --target PipelineScheduler --parallel 4
ctest --test-dir build/debug -R "SharedToneCurveTest|PipelineFrameSinkTest|EditViewerLogicTests|CropRotateOpTest" --output-on-failure
ctest --test-dir build/debug -R "OpenCl(ProgramLibrary|Runtime|FusedEditPipeline|CudaPipelineCompare)Test" --output-on-failure
```

On macOS/Metal:

```bash
cmake --build --preset macos_debug --target alcedo_main
```

The macOS command is listed for reviewer convenience. When working from Windows on this repository,
do not substitute it for the MSVC wrapper flow.

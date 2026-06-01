# Experiments, Diagnostics, and References

This document records the experiment trail behind the color adjustment overhaul. It is meant to
explain why the final branch looks the way it does, especially where the implementation differs
from simpler point-wise tone curves.

## Research Direction

The H/S local-tone work was guided by established edge-aware tone-mapping literature rather than
ad hoc masking. The important takeaways were:

- A plain global curve can compress or lift tone but cannot preserve local texture reliably.
- A naive Gaussian base/detail split is fast, but it can create halos or defocused-edge artifacts
  when a blurred tonal ramp is treated like a hard edge.
- Edge-aware decomposition literature repeatedly separates base tone manipulation from detail
  treatment, and then gates detail manipulation to avoid halos, gradient reversal, and noise lift.
- For an interactive editor, a full local Laplacian pyramid is more expensive than necessary for
  this branch. The accepted compromise is a single-scale log-luminance base/detail model inspired
  by local Laplacian filtering, with guards and slope-based detail compensation.

The branch therefore implements a pragmatic LLF-style stage:

- build a blurred log-luminance base;
- calculate base deltas for shadows and highlights;
- estimate local contrast loss with numerical curve slopes;
- reintroduce recoverable texture through gated detail compensation;
- keep scene-referred RGB reconstruction unclamped until the output transform.

## Iteration History

### 1. OKLCh/HLS migration

The initial color work migrated HLS behavior from a conventional hue/saturation/lightness model
toward OKLab/OKLCh-style manipulation. The motivation was to avoid the common failure mode where a
targeted hue edit unexpectedly shifts brightness or destroys chroma.

Design choices:

- use hue as a polar angle in an opponent-color plane;
- use chroma as the radius in that plane;
- blend profile adjustments across eight hue bins;
- widen default hue influence to make edits continuous;
- fold global saturation into the same perceptual color path.

### 2. CUDA local-tone prototype

The first H/S local-tone implementation introduced a dedicated CUDA stage and cached base/detail
structure. It replaced direct highlight/shadow point kernels in the accelerated stream.

Main result:

- H/S behavior became region-aware and could preserve more texture than a global curve.

Observed issue:

- some edge-adjacent regions showed halos or dark circular artifacts after shadow lifting. The
  problem was traced to base/detail geometry and residual handling rather than output color space.

### 3. Paper-driven soft-edge correction

After reviewing edge-aware tone-mapping papers, the tuning shifted from "protect hard edges" toward
"preserve soft ramps as ramps." This was important because the visible artifact was not only
chromatic aberration or hard-edge ringing; it also appeared on defocused transitions.

Accepted direction:

- avoid making soft gradients behave like binary masks;
- damp local detail changes where a low-contrast blurred ramp would otherwise be overinterpreted;
- keep useful texture in real shadow regions.

### 4. Slider-range containment

One mid-branch conclusion was that the algorithm was acceptable inside a narrower shadow range.
The requested behavior then became mapping the useful existing range onto the wider UI slider range
instead of continuing to redesign the algorithm. The documentation keeps this point because future
work should not reopen LLF tuning when the real issue is control mapping.

### 5. OpenCL port

The CUDA LLF shape was ported to OpenCL.

Important result:

- OpenCL now runs a staged flow:
  1. pre-H/S fused ops;
  2. cached base-log build;
  3. H/S local-tone apply;
  4. post-H/S fused ops;
  5. detail stages.

Important failure and fix:

- Adding host cache-key fields directly into the uploaded OpenCL parameter struct broke ABI/layout
  validation. The fix was to keep 64-bit cache keys in host-side fused/cache state and keep shader
  upload layout aligned and compact.

### 6. Highlight detail compensation

Highlight compression initially made highlights darker without recovering enough perceived
texture. The accepted refinement was:

- keep highlight base compression;
- stop default detail suppression in highlight regions;
- add mild positive local detail compensation where highlight slope loss indicates recoverable
  texture;
- gate the compensation by highlight mask, noise/detail magnitude, edge size, and clipping risk;
- do not add a separate chroma compensation model in this pass.

### 7. Shadow contrast/fill-light tuning

Shadow lift went through several numerical experiments to balance:

- perceived texture retention;
- useful fill-light behavior in `-1..-4 EV`-like shadows;
- conservative behavior in very deep shadows where noise should not be promoted;
- avoidance of halo or gradient reversal around large tonal transitions.

### 8. Metal parity and performance

Metal was brought into parity with CUDA/OpenCL:

- staged fused pipeline;
- cached H/S base textures;
- local-tone apply kernel;
- direct compute pipeline cache usage;
- H/S encode timing in performance reports;
- app bundle install updates for curated LUTs and runtime assets.

## Diagnostic Scripts

The branch adds three scripts under `scripts/`. They are intentionally small models of the shader
math, not replacements for image-based QA.

### `scripts/diagnose_hs_local_tone.py`

Purpose:

- reproduce H/S local-tone mask-edge halos on synthetic one-dimensional ramps;
- compare current and fixed local-tone behavior;
- inspect hard edge, soft edge, pivot, and CA-like edge cases.

Inputs:

```bash
python scripts/diagnose_hs_local_tone.py --case pivot
python scripts/diagnose_hs_local_tone.py --case soft --width 768 --shadow 1.0 --highlight 1.0
python scripts/diagnose_hs_local_tone.py --case ca-edge --out build/diagnostics/hs_local_tone_ca
```

Outputs:

- `profile.csv`
- `profile.ppm`
- `summary.txt`

Why it exists:

- The artifact was easier to reason about on a synthetic ramp than on a full photograph. The script
  mirrors the log-luminance part of the CUDA/OpenCL logic and makes local deltas visible.

### `scripts/diagnose_hs_tone_response.py`

Purpose:

- compare baseline and candidate H/S tone response curves;
- measure output slope for shadow lift and highlight compression;
- estimate OKLab chroma retention for saturated highlight swatches.

Outputs:

- `build/diagnostics/hs_tone_response/tone_response.png`
- `build/diagnostics/hs_tone_response/highlight_chroma_retention.png`
- `build/diagnostics/hs_tone_response/summary.txt`

Why it exists:

- It makes excessive compression visible numerically. If output slope becomes too low, local
  contrast loss is expected even before testing on real images.

### `scripts/diagnose_hs_shadow_detail.py`

Purpose:

- measure perceived shadow contrast and fill-light detail;
- compare previous and candidate shadow-lift curves;
- inspect practical camera-weighted shadow ranges separately from very deep shadow/noise zones.

Outputs:

- `build/diagnostics/hs_shadow_detail/shadow_perceptual_contrast.png`
- `build/diagnostics/hs_shadow_detail/shadow_camera_weighted_contrast.png`
- `build/diagnostics/hs_shadow_detail/shadow_fill_light_contrast.png`
- `build/diagnostics/hs_shadow_detail/shadow_texture_lines.png`
- `build/diagnostics/hs_shadow_detail/summary.txt`

Why it exists:

- A shadow lift that only raises the base can look flat. The script checks whether useful shadow
  texture remains or improves while very deep shadow noise stays conservative.

## Reference Papers and Sources

### Edge-aware tone mapping and local tone

Fredo Durand and Julie Dorsey, "Fast Bilateral Filtering for the Display of High-Dynamic-Range
Images," SIGGRAPH 2002.

- Project page: <https://people.csail.mit.edu/fredo/PUBLI/Siggraph2002/>
- Used for: base/detail separation, bilateral tone-mapping intuition, and halo risk framing.

Zeev Farbman, Raanan Fattal, Dani Lischinski, and Richard Szeliski, "Edge-Preserving
Decompositions for Multi-Scale Tone and Detail Manipulation," ACM Transactions on Graphics,
SIGGRAPH 2008.

- PDF: <https://www.microsoft.com/en-us/research/wp-content/uploads/2008/08/Farbman-EPD-small-SG08.pdf>
- Used for: edge-preserving smoothing, multi-scale tone/detail separation, and the importance of
  avoiding artifacts in smooth gradients.

Sylvain Paris, Samuel W. Hasinoff, and Jan Kautz, "Local Laplacian Filters: Edge-aware Image
Processing with a Laplacian Pyramid," ACM Transactions on Graphics, SIGGRAPH 2011.

- Project page: <https://people.csail.mit.edu/sparis/publi/2011/siggraph/>
- PDF: <https://people.csail.mit.edu/hasinoff/pubs/ParisEtAl11-lapfilters.pdf>
- Used for: preserving edge profiles, avoiding hard-edge overfitting, and the general idea that
  local detail manipulation should be guided by local tonal structure.

### Perceptual color model

Bjorn Ottosson, "A perceptual color space for image processing," original Oklab post, 2020.

- Page: <https://bottosson.github.io/posts/oklab/>
- Used for: OKLab/OKLCh motivation, lightness/chroma/hue separation, and practical conversion
  structure.

### Local repository papers

`docs/paper/110801_1.pdf`

- D. Andrew Rowlands, "Color conversion matrices in digital cameras: a tutorial," Optical
  Engineering, 2020.
- Used for: local context on camera raw spaces, color conversion matrices, white balance, and
  chromatic adaptation. It is relevant to the broader RAW/color pipeline but not the direct source
  of the LLF local-tone algorithm.

`docs/paper/2086696.2086728.pdf`

- Daniel Orozco, Elkin Garcia, Rishi Khan, Kelly Livingston, and Guang R. Gao, "Toward
  High-Throughput Algorithms on Many-Core Architectures," ACM Transactions on Architecture and
  Code Optimization, 2012.
- Used only as broad systems context for throughput-oriented design. It is not a color-science or
  tone-mapping reference.

## Validation Notes

Validation reported during the branch work included:

- `git diff --check`
- `cmd /c scripts\msvc_env.cmd --build --preset win_debug --target EditPipeline --parallel 4`
- `cmd /c scripts\msvc_env.cmd --build --preset win_debug --target PipelineScheduler --parallel 4`
- `cmd /c scripts\msvc_env.cmd --build --preset win_debug --parallel 4`
- `ctest --test-dir build/debug -R "OpenCl(ProgramLibrary|Runtime|FusedEditPipeline|CudaPipelineCompare)Test" --output-on-failure`

The OpenCL targeted test pass was recorded as 27/27 during the OpenCL LLF iteration. Reviewers
should still rerun the relevant build/test commands after merge conflict resolution because shader
ABI and generated program bundles are easy to break with harmless-looking field-order changes.

## Visual QA Suggestions

Use real photographs and synthetic cases. The following visual cases are most likely to expose
regressions:

- backlit subject with deep but textured shadows;
- tree branches or hair against bright sky;
- defocused sky/building or sky/foliage transition;
- bright colored highlights such as saturated sunset clouds or neon signs;
- low-chroma deep shadows where noise should not become colorful texture;
- zoomed editor preview with H/S sliders dragged while a detail ROI is active;
- crop/rotation preview while H/S local tone is active.

Expected behavior:

- shadow lift should behave like fill light in usable shadows, not like a flat gray wash;
- very deep shadow noise should remain conservative;
- highlight reduction should darken base tone while retaining recoverable local texture;
- saturated highlights should not desaturate more than necessary;
- soft tonal ramps should remain soft ramps;
- zoomed detail patches should align with the full-frame reference and not replace it with stale
  ROI content.

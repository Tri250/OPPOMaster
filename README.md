<img src="docs/header.jpg" alt="Alcedo Studio" width="100%"/>

[Project website](https://zidage.github.io/AlcedoStudio) | [项目网页](https://zidage.github.io/AlcedoStudio)

<p align="right"><a href="./README.md"><strong>English</strong></a> | <a href="./README.zh-CN.md">简体中文</a></p>

![License](https://img.shields.io/badge/License-GPLv3-blue)
![CUDA](https://img.shields.io/badge/CUDA-12.8-76B900)
![C++](https://img.shields.io/badge/C++-20-blue)
![AI](https://img.shields.io/badge/AI-CLIP%20%2B%20VLM-ff6f00)

**Alcedo Studio** is a RAW photo editor and digital asset manager built for photographers who want a fast, lightweight, and privacy-respecting workflow. It handles everything from import and culling to grading, versioning, and export — with optional AI assistance that runs locally by default and only reaches the cloud when you explicitly choose to.

> Alcedo Studio is not positioned as a drop-in replacement for any existing commercial or open-source tool. It is an independent workflow with its own design priorities.

## Screenshots and Demo

The screenshots below are taken from the current website image set at `docs/alcedo-website/public/screenshots`, reflecting the v0.2.6-era interface.

<table>
  <colgroup>
    <col style="width: 76%" />
    <col style="width: 24%" />
  </colgroup>
  <tbody>
    <tr>
      <td><img src="docs/alcedo-website/public/screenshots/1-主界面.png" alt="Alcedo Studio library browser" width="100%" /></td>
      <td>Library browser — folder tree, fast thumbnail grid, Library Overview facets, AI labels, ratings, and import/export actions in one workspace</td>
    </tr>
    <tr>
      <td><img src="docs/alcedo-website/public/screenshots/7-高级筛选.png" alt="Advanced filtering and library overview" width="100%" /></td>
      <td>Advanced filtering — exact/fuzzy search, date/camera/lens facets, rating filters, and thumbnail-backed results for large projects</td>
    </tr>
    <tr>
      <td><img src="docs/alcedo-website/public/screenshots/8-AI内容识别.png" alt="AI content recognition settings" width="100%" /></td>
      <td>AI content recognition — local CLIP / SigLIP model management, generation status, and model activation without uploading your photos</td>
    </tr>
    <tr>
      <td><img src="docs/alcedo-website/public/screenshots/9-AI内容过滤.png" alt="AI semantic label filtering" width="100%" /></td>
      <td>AI content filtering — generated semantic labels become first-class library filters that can be combined with EXIF and rating criteria</td>
    </tr>
    <tr>
      <td><img src="docs/alcedo-website/public/screenshots/10-AI自然语言搜索.png" alt="AI natural-language search" width="100%" /></td>
      <td>Natural-language search — describe a scene in plain language, switch on semantic search, and rank matches through the local vector index</td>
    </tr>
    <tr>
      <td><img src="docs/alcedo-website/public/screenshots/2-色彩科学.png" alt="ACES and OpenDRT color science" width="100%" /></td>
      <td>Dual color science — ACES 2.0 and OpenDRT output rendering with display colour space, EOTF, and peak-luminance controls</td>
    </tr>
    <tr>
      <td><img src="docs/alcedo-website/public/screenshots/3-基础调整.png" alt="Basic and local tone adjustments" width="100%" /></td>
      <td>Real-time adjustments — exposure, contrast, white balance, tone curve, local Highlights/Shadows, live histogram, and GPU-backed preview</td>
    </tr>
    <tr>
      <td><img src="docs/alcedo-website/public/screenshots/4-高级色彩.png" alt="Advanced color controls" width="100%" /></td>
      <td>Advanced color — HSL separation, color wheels, channel mixing, and live scopes for precise creative grading</td>
    </tr>
    <tr>
      <td><img src="docs/alcedo-website/public/screenshots/5-几何调整.png" alt="Geometry and crop controls" width="100%" /></td>
      <td>Geometry tools — crop, rotate, lens distortion, perspective repair, and common aspect-ratio presets</td>
    </tr>
    <tr>
      <td><img src="docs/alcedo-website/public/screenshots/Portra 400.png" alt="Kodak Portra 400 film simulation" width="100%" /></td>
      <td>Film simulation — curated Kodak, Fuji, and Agfa CUBE LUTs for one-click film looks, including Portra-style portrait rendering</td>
    </tr>
    <tr>
      <td><img src="docs/alcedo-website/public/screenshots/5-胶片颗粒与Halation模拟.png" alt="Film grain and halation controls" width="100%" /></td>
      <td>Film grain and halation — adjustable grain density/granularity plus red-biased highlight bloom across CPU, CUDA, OpenCL, and Metal paths</td>
    </tr>
    <tr>
      <td><img src="docs/alcedo-website/public/screenshots/6-导出界面.png" alt="Export settings" width="100%" /></td>
      <td>Export workflow — SDR/HDR parameters, ICC profile embedding, metadata handling, and multi-format output</td>
    </tr>
  </tbody>
</table>

> Some demo RAW files used by the project are from [signatureedits](https://www.signatureedits.com/free-raw-photos/) 100% Free Raw Files.

## What Changed Since v0.2.3

v0.2.3 was the Alcedo Studio rebrand and WebGPU/D3D12 experiment. Subsequent releases moved the project from prototype breadth toward a practical editor and DAM.

| Release cycle | Highlights |
| --- | --- |
| **v0.2.4** | Retired the experimental WebGPU path and moved GPU backend work to OpenCL; added OpenCL image containers, program management, RAW/point/linear-reference operators, DRT/LMT, lens calibration, geometry, DNG warp, scope analysis, and runtime backend switching. The editor was split into dedicated tone, RAW decoding, geometry, DRT, color, and versioning panels, while Sleeve gained collection membership, pagination, cache invalidation, star ratings, global search, and disk-backed thumbnail caching. |
| **v0.2.5** | Rebuilt Highlights/Shadows around LLF-style local tone processing, improved OKLCh/HLS color handling, added batch copy/paste adjustment transfer, refreshed geometry/crop interactions, and overhauled HDR export metadata plus UltraHDR writer paths. |
| **v0.2.6** | Landed the AI-native library work: semantic image labels, model profiles, Jina CLIP / SigLIP handling, HNSW vector search, semantic filtering, model download/activation UX, and async model execution. This cycle also added film grain and Halation effects across CPU/CUDA/OpenCL/Metal, Nikon HE/HE* RAW support through patched LibRaw, macOS CoreML model support, LUT favorites, and the current website screenshot refresh. |

## Core Capabilities

### Library management

- One project file keeps all metadata, previews, edit histories, and AI embeddings together.
- Responsive thumbnail grid for large RAW libraries.
- Filter by date, camera, lens, rating, label, or any combination.
- Global search across EXIF, filenames, and semantic tags without leaving the app.

### Semantic search

- Runs entirely on your machine after activating a local vision model.
- Supports MobileCLIP2, SigLIP2, Jina CLIP v2, and macOS CoreML profiles.
- Queries rank by meaning through an on-device HNSW vector index.
- Each model's tags stay isolated, so switching models does not mix incompatible label sets.

### Optional AI captioning and rating

- Connect a remote vision model through OpenAI-compatible, Anthropic, or Volcengine Ark endpoints.
- Returns a caption, searchable tags, scene label, and a 1–5 star rating with rationale.
- Star ratings are written back to EXIF.
- API keys are stored in the OS credential store and never appear in logs or project files.
- Adjustable strictness from generous to exacting.

### Non-destructive, branchable editing

- Every image keeps its own version tree.
- Each Version is a named look with an independent undo/redo timeline.
- Versions replay from the import baseline, so branches never tangle.
- Switch active versions to compare, or clone a recipe across a shoot.

### RAW processing

- 32-bit floating-point pipeline from demosaicing to output transform.
- Demosaic algorithms: AHD, Amaze, RCD.
- Highlight reconstruction and as-shot/custom white balance.
- Tone controls: exposure, contrast, curves, color temperature.
- Color controls: HSL, saturation, vibrance, tint, lift/gamma/gain color wheels.
- Detail controls: local Highlights/Shadows, clarity, sharpening.
- Output transforms: ACES 2.0 or OpenDRT, with display space, EOTF, and peak-luminance controls.
- Film looks: curated CUBE LUTs, plus film grain and halation output effects.

### GPU acceleration

- Windows: CUDA and OpenCL paths.
- macOS: Metal path.
- RAW processing, preview, and many operators can run on the GPU.
- Runtime backend selection for supported operators.
- LRU image pool with pinned handles and disk-backed thumbnail caching.
- CUDA preview path reaches hundreds of frames per second at full preview resolution on modern NVIDIA hardware.

### Export

- Renders through the same pipeline used for previews and thumbnails.
- Output formats: JPEG, PNG, TIFF, WEBP.
- Bit depths: 8/16/32-bit where the format supports it.
- HDR output: Ultra HDR gain-map JPEG with ICC profile embedding.
- Per-batch resize, quality, compression, and metadata handling.

## RAW and Camera Support

Alcedo Studio imports all major RAW formats through a patched fork of [LibRaw](https://github.com/zidage/LibRaw):

- Canon CR2 / CR3
- Nikon NEF
- Sony ARW
- Fujifilm RAF
- Panasonic RW2
- Olympus / OM System ORF
- Leica, Hasselblad, Phase One, Pentax, Sigma, Samsung
- DNG, including smartphone and drone DNGs

See the full format list in [docs/supported_raw_formats.md](docs/supported_raw_formats.md) and the camera list in [docs/supported_cameras.md](docs/supported_cameras.md).

### Exclusive Nikon HE / HE\* support

Nikon High-Efficiency (`HE`) and High-Efficiency★ (`HE*`) NEFs are still unsupported in upstream LibRaw 0.22. Alcedo Studio ships a patched LibRaw fork that decodes these files directly — no conversion to DNG required. Validated cameras include:

- Nikon Z 8
- Nikon Z 9
- Nikon Z 6 III
- Nikon Z 50 II

The decoder lives in the project's LibRaw fork: **https://github.com/zidage/LibRaw**

## System Requirements

- Windows 10/11 x64 for the Qt RHI editor build, with CUDA acceleration on NVIDIA GPUs and OpenCL coverage for supported operators/backends.
- macOS on Apple Silicon for the Metal-backed Qt application build.
- NVIDIA GPU with CUDA support (minimum compute capability 6.0 / 10-series or later, recommended 7.0+ / 20-series or later) for the fastest Windows path; preferably 6GB+ VRAM for smooth work with high-resolution RAW files (40MP+).
- A Metal-capable Apple Silicon Mac for the macOS/Metal build.
- At least 8GB of system RAM (16GB+ recommended for larger libraries and smoother performance).
- 500MB of free disk space for the installation and temporary working files.
- 60+ MB for installation package and partial update support.

## Build from Source

Build instructions are maintained separately in [docs/build_from_source.md](docs/build_from_source.md) (English and Chinese).

## Roadmap

Roadmap and ongoing milestones are tracked in [docs/roadmap/roadmap.md](docs/roadmap/roadmap.md).

## Acknowledgements

Alcedo Studio builds on research, open-source implementations, and community data from the wider imaging ecosystem:

- Distributed film-emulation LUTs are from [JanLohse/spectral_film_lut](https://github.com/JanLohse/spectral_film_lut).
- Some camera color matrices are from [AcademySoftwareFoundation/rawtoaces-data](https://github.com/AcademySoftwareFoundation/rawtoaces-data).
- The highlight reconstruction algorithm is adapted from RawTherapee's [hilite_recon.cc](https://github.com/RawTherapee/RawTherapee/blob/dev/rtengine/hilite_recon.cc).
- The RCD demosaic algorithm is from [LuisSR/RCD-Demosaicing](https://github.com/LuisSR/RCD-Demosaicing).
- OpenDRT is ported from [OpenDRT.dctl](https://github.com/jedypod/open-display-transform/blob/main/display-transforms/opendrt/OpenDRT.dctl).
- ACES 2.0 support is ported from [aces-aswf/aces-core](https://github.com/aces-aswf/aces-core).
- The film grain renderer is based on Alasdair Newson, Noura Faraj, Bruno Galerne, and Julie Delon's
  [Realistic Film Grain Rendering](https://doi.org/10.5201/ipol.2017.192).

## License

The `v0.1.1` tag and earlier releases remain under Apache-2.0.
Development after `v0.1.1` is licensed under `GPL-3.0-only`, with an additional permission under GPLv3 section 7 for combining/distributing required NVIDIA CUDA components.
See [LICENSE](LICENSE) and [NOTICE](NOTICE).

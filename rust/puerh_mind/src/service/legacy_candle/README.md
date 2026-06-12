Legacy Candle CLIP sources live here for reference only.

The active runtime path is `OrtClipEngine` in `src/service/ort_clip.rs`, backed
by ONNX Runtime. This directory is intentionally not exported from
`src/service/mod.rs`, so these files are outside the active build path for the
semantic runtime hardening phase.

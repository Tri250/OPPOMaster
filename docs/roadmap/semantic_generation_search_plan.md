# Semantic Generation and Search Integration Plan

Date: 2026-06-12

Status: Phase 1 complete; Phase 2 complete; Phase 3 complete; Phase 4a
initial scaffold complete; Phase 4c complete; Phase 4d complete

This document proposes how to integrate `rust/puerh_mind` into Alcedo Studio as
project-level semantic image generation and semantic search services.

## Current State

`rust/puerh_mind` is already a local gRPC inference server. Its active engine is
the ONNX Runtime MobileCLIP path, not the older Candle files:

- `proto/semantic.proto` exposes unary `Ping`, `EmbedText`, and `EmbedImage`.
- `SemanticService` already batches image requests internally before calling the
  engine.
- `OrtClipEngine` loads MobileCLIP text and vision ONNX models, produces
  512-dimensional normalized embeddings, and accepts 256-pixel image inputs.
- Python demos start the server through `cargo run --release`, build text-label
  prototypes, classify images by cosine similarity, and write demo JSON output.

The C++ app already has natural integration points:

- `ImportService` and `ExportService` provide job/progress/cancellation patterns.
- `ThumbnailService` can generate and pin `k256`, `k512`, `k1024`, or `k2048`
  thumbnails and release them after use.
- `SleeveFilterService` already has a `SemanticSearchProvider` interface, but no
  concrete implementation.
- `SearchController` and `GlobalSearchDialog.qml` own the current search-preview
  lifecycle and preview-thumbnail paging.
- Project packaging/checksum code enumerates known database tables explicitly, so
  new semantic tables must be added there.

## Target Architecture

Keep the user-facing services aligned with the planned split:

- `SemanticGenerationService`: bulk image embedding generation and label
  assignment for imported files, selected folders, or the full project.
- `SemanticSearchingService`: text-query embedding, tag search, ranking, and
  integration with `SleeveFilterService::SemanticSearchProvider`.

Add one shared internal service:

- `SemanticRuntimeService`: owns the Rust child process, gRPC client, model
  manifest validation, health checks, startup/shutdown, and runtime diagnostics.

This keeps process management out of both generation and search. Both services
should talk to a narrow async C++ interface rather than spawning the Rust process
themselves.

## Rust Runtime Changes

### Configuration

Replace hardcoded runtime defaults with CLI flags and environment fallbacks:

- `--host`
- `--port`
- `--model-root`
- `--model-id`
- `--revision`
- `--device`
- `--no-download`
- `--batch-cap`
- `--batch-wait-ms`
- `--max-message-bytes`

Default behavior for the packaged app should be validate-only. The Rust service
should not download missing models unless an explicit development flag is set.
The Qt app should own download UX, source selection, progress, and integrity
errors.

### RPC Surface

Keep the existing unary RPCs for debugging and smoke tests, then add explicit
batch/status calls:

- `EmbedTextBatch`
- `EmbedImageBatch`
- `GetModelInfo`
- `GetRuntimeStatus`

`GetModelInfo` should return model id, revision, embedding dimension, expected
image size, provider, model root, and prototype/config hashes. C++ should reject
generation if this does not match the local manifest or database model key.

Batch responses should preserve request IDs and include per-item status. A
partial failure should not fail the whole batch unless the runtime itself is
unusable.

### Execution Providers

Windows should continue to use DirectML, with CPU fallback. DirectML should be
run through a single session worker or otherwise protected from concurrent
`Run` calls on the same session.

macOS should add a Core ML execution-provider option and fall back to CPU when
Core ML is unavailable. Device parsing should distinguish:

- `auto`
- `cpu`
- `directml` / `directml:N`
- `coreml`
- `coreml:all`
- `coreml:cpuandgpu`
- `coreml:cpuonly`

The stale Candle source files should either be removed or clearly quarantined
outside the active build path. The integration should standardize on ONNX
Runtime for this phase.

## Model Distribution and Download

The packaged app should not bundle model weights. It should bundle:

- a model manifest JSON
- model id and pinned revision
- required relative files
- expected SHA-256 and byte sizes
- embedding dimension and image input size
- precomputed default text-label prototypes

The Qt app downloads into an application data directory, for example:

- Windows: `%LOCALAPPDATA%/AlcedoStudio/models/...`
- macOS: `~/Library/Application Support/AlcedoStudio/models/...`

Downloader requirements:

- user-configurable endpoint, with a domestic Hugging Face mirror as a preset
- atomic staging directory, then rename on success
- resumable `.part` files where practical
- per-file hash validation before marking the model usable
- clear "missing", "downloading", "ready", and "corrupt" states in settings

Rust should receive the resolved model root from C++ and validate only. For
developer builds, the current `hf-hub` path may remain behind an opt-in flag.

## C++ Service Design

### SemanticRuntimeService

Responsibilities:

- start and stop the Rust child process through `QProcess`
- choose a free localhost port and pass it to Rust
- pass model root, model id, revision, and device flags
- wait for gRPC health readiness
- expose runtime state to QML/settings
- capture stdout/stderr into the app log
- provide async `EmbedText`, `EmbedImage`, and batch methods
- enforce request timeouts and cancellation
- kill the child tree on app exit or explicit user stop

On Windows, use a Job Object or equivalent child-tree cleanup. On macOS, use a
process group. Stopping should first attempt graceful shutdown, then terminate,
then kill after timeout.

Do not spawn one Rust process per image. Start one local service for an explicit
user action or session, reuse it, and let the Rust side batch requests.

### SemanticGenerationService

Responsibilities:

- enumerate target files after import, selected folder, or full project
- skip images that already have valid embeddings for the same model key unless
  forced
- request a thumbnail from `ThumbnailService`
- pin the thumbnail while encoding and sending it to Rust
- release the thumbnail in every success, failure, and cancellation path
- persist embeddings and label decisions transactionally
- publish progress, failures, and cancellation state to UI

Default thumbnail tier should be `k256` for inference cost. Add a setting to use
`k512` or `k1024` when the user wants semantic generation to also warm preview
cache for search results.

Classification should reuse the Python demo semantics:

- text prototypes are normalized
- image embeddings are normalized
- cosine similarity can be computed as dot product
- label decision keeps best score, second score, margin, and confidence

### SemanticSearchingService

Responsibilities:

- implement `SemanticSearchProvider`
- embed free-text queries through `SemanticRuntimeService`
- search stored image embeddings within the current Sleeve scope
- support direct label search without starting Rust when possible
- return stable result pages for `SearchController`
- expose count or total-result metadata for preview pagination

For folder scoping, reuse the same root/folder semantics as
`ElementController::BuildScopedFileQuery`. Avoid duplicating ad hoc folder SQL.

For applying a semantic search to the album grid, avoid a giant `IN (...)` list
for large result sets. Prefer a temporary or project-local result table keyed by
search token, then let the existing thumbnail model page over that scope.

## Database Schema

Add semantic tables rather than storing vectors inside generic image metadata.

Recommended first schema:

```sql
CREATE TABLE SemanticModel (
  model_key VARCHAR PRIMARY KEY,
  model_id VARCHAR NOT NULL,
  revision VARCHAR NOT NULL,
  embedding_dim INTEGER NOT NULL,
  image_size INTEGER NOT NULL,
  prompt_config_hash VARCHAR,
  asset_manifest_json JSON,
  created_at TIMESTAMP DEFAULT current_timestamp
);

CREATE TABLE SemanticImageEmbedding (
  file_id BIGINT NOT NULL,
  image_id BIGINT NOT NULL,
  model_key VARCHAR NOT NULL,
  embedding BLOB NOT NULL,
  embedding_dim INTEGER NOT NULL,
  thumbnail_resolution INTEGER NOT NULL,
  generated_at TIMESTAMP DEFAULT current_timestamp,
  status VARCHAR NOT NULL,
  error VARCHAR,
  PRIMARY KEY (file_id, model_key)
);

CREATE TABLE SemanticImageLabel (
  file_id BIGINT NOT NULL,
  model_key VARCHAR NOT NULL,
  label VARCHAR NOT NULL,
  score DOUBLE NOT NULL,
  second_label VARCHAR,
  second_score DOUBLE,
  margin DOUBLE,
  confident BOOLEAN NOT NULL,
  top_scores JSON,
  updated_at TIMESTAMP DEFAULT current_timestamp,
  PRIMARY KEY (file_id, model_key)
);

CREATE TABLE SemanticLabelPrototype (
  model_key VARCHAR NOT NULL,
  label VARCHAR NOT NULL,
  prompt_config_hash VARCHAR NOT NULL,
  embedding BLOB NOT NULL,
  synonyms_json JSON,
  PRIMARY KEY (model_key, label, prompt_config_hash)
);
```

Store normalized `float32` vectors in DuckDB-native list/array form rather than
opaque BLOBs. Free-text semantic search must not fetch every stored vector into
C++ for cosine comparison. The storage layer owns a ranked query primitive that:

- validates the query vector against the active model dimension
- reuses `ElementController::BuildScopedFileQuery` for root/folder scope
- joins scoped files to `SemanticImageEmbedding`
- ranks with DuckDB VSS/HNSW using `array_distance` over normalized embeddings
- applies `ORDER BY score DESC, file_id` plus `LIMIT/OFFSET` in SQL

This keeps the future `SemanticSearchProvider` thin: it embeds text through the
runtime, then asks storage for a ranked page. The application receives only the
page rows, not the full vector corpus.

DuckDB's `vss` extension is required for semantic search. The packaged app must
ship the matching `vss.duckdb_extension`; local DuckDB may recognize the
extension name but still fail `LOAD vss` if the extension file has not been
installed. Storage should therefore:

- require `LOAD vss` at startup or first semantic search
- create and maintain an HNSW index over the fixed-size embedding column
- enable DuckDB's `hnsw_enable_experimental_persistence` setting for on-disk
  project databases before creating the index
- fail with an actionable storage error when the extension or index is missing
- hide the exact-vs-ANN choice behind the storage controller API

The table shape intentionally uses `FLOAT[512]` for the active MobileCLIP model
because DuckDB's HNSW index requires a fixed-size array column. A future
non-512-dimensional model should use a schema migration or a dimension-specific
embedding table rather than degrading back to C++ vector scans.

Required follow-up updates:

- add table creation to `DBController`
- include semantic tables in project data summaries/checksums
- delete semantic rows when files are removed
- include semantic tables in project save/load/package workflows
- add storage tests for create, update, delete cleanup, and package integrity

## Import and UI Flow

Generation should not be silent.

After import finishes and the project has been synced/saved/reloaded, show a
prompt unless the import is part of a repair/reimport path:

- "Start semantic generation now"
- "Always ask"
- "Always start after import"
- "Skip"
- "Never ask"

Remember the choice in `QSettings`. Settings should expose:

- model source URL
- model directory
- model status and download progress
- runtime device
- start/stop runtime buttons
- auto-start preference after import
- semantic generation thumbnail tier
- clear/regenerate semantic data actions

The runtime state should be visible whenever it consumes GPU/CPU memory. The
user must be able to stop it explicitly.

## Search UI Flow

Extend global search with a mode selection that can be kept compact:

- metadata
- semantic text
- label/tag

Direct label queries can be answered entirely from local database labels. Free
text queries require the runtime to be started, unless a cached query embedding
exists and is still compatible with the active model key.

Existing preview thumbnail lifecycle should be reused. Semantic result pages
should request thumbnails only for visible preview rows and should release them
when rows leave view, matching current `SearchController` behavior.

## Error Handling and Integrity

Reject writes when:

- request ID does not match
- embedding dimension does not match the active model
- vector contains NaN or infinity
- vector norm is zero
- runtime model info does not match the database model key
- database transaction fails

Surface user-actionable states:

- model missing
- model corrupt
- runtime failed to start
- runtime crashed
- device/provider unavailable
- generation cancelled
- partial generation completed with failures

Failed image rows should record enough error text for retry diagnostics, but
large runtime logs should stay in the app log rather than the database.

## Packaging

Add a CMake target that builds the Rust binary:

- Windows: call Cargo through `rust/puerh_mind/script/cargo_msvc.cmd`
- macOS: call Cargo directly from the configured toolchain

Install the binary next to the app executable:

- Windows: install `alcedo_mind.exe` beside `alcedo_main.exe`
- macOS: install inside the `.app` bundle

Also install required ONNX Runtime dynamic libraries. The app package should
contain the runtime binary and small manifests/prototypes, but not model weights.

Packaging smoke tests should verify:

- installed app can find `alcedo_mind`
- runtime can start and answer `Ping`
- `GetModelInfo` reports the expected proto/runtime version
- missing model produces the downloader path instead of a crash

## Phased Rollout

1. Rust runtime hardening - complete
   - CLI config
   - validate-only model assets
   - model/status RPCs
   - batch RPCs
   - Core ML provider option
   - remove or quarantine stale Candle paths

2. C++ runtime client
   - generated C++ gRPC/protobuf stubs
   - `SemanticRuntimeService`
   - start/stop/health/status UI plumbing
   - fake-runtime tests before real model tests

3. Storage foundation
   - semantic tables
   - storage controller or repository wrapper
   - DuckDB VSS/HNSW vector ranking and pagination primitive
   - required DuckDB `vss` extension loading and index creation
   - project checksum/package integration
   - deletion cleanup

4a. Bulk generation request model and thumbnail pipeline - initial scaffold
   complete
   - thumbnail request/pin/release pipeline
   - generation job/progress/cancel/result model
   - batch-shaped image embedding client interface with mock responses
   - real `ThumbnailService::GetThumbnailDetailed` integration
   - thumbnail CPU materialization into RGBA8 request payloads
   - release pinned thumbnail after payload preparation in success, error, and
     cancellation paths
   - tests that import real sample images, request real thumbnails, batch mock
     embeddings, handle thumbnail failures, and cancel during mock embedding

4b. Bulk generation runtime RPC
   - generated C++ gRPC image batch client or equivalent bridge
   - real image embedding request/response waiting and timeout handling
   - request-id matching and per-item partial failure mapping
   - model-info compatibility checks before generation starts
   - decide final image payload contract: raw `rgba8:WxH` or encoded image bytes

4c. Bulk generation persistence and labels - complete
   - persist image embeddings through `SemanticStorageController`
   - seed bundled photography label query rows into new project databases
   - generate label query embeddings once per model/config and cache them in
     `SemanticLabelPrototype`
   - label assignment from cached project-local prototypes
   - persist label decisions transactionally with embeddings
   - reject bad vectors: wrong dimension, NaN/Inf, zero norm, request mismatch

4d. Bulk generation workflow integration
   - retry/force-regenerate rules
   - skip already-valid embeddings for the active model key
   - import-finished prompt
   - UI-facing progress/cancel/failure state

5. Search integration
   - concrete `SemanticSearchProvider`
   - tag search path
   - text search path
   - preview pagination/count support
   - apply-to-album-grid path

6. Download and packaging
   - Qt model downloader
   - source mirror settings
   - remove the temporary development `model-root` fallback that probes
     `rust/puerh_mind/models/mobileclip2-s2-openclip`; release builds must pass
     a downloader/settings-resolved model root explicitly
   - Rust binary install
   - ORT dynamic library deployment
   - DuckDB `vss` extension deployment when ANN search is enabled
   - packaged smoke test

7. Performance path
   - tune DuckDB VSS/HNSW index parameters for large catalogs
   - cached query embedding/result-token tables for repeated UI paging
   - apply-to-album-grid scope tables instead of giant `IN (...)` lists
   - multi-adapter or CPU worker pool only after single-process batching is
     measured

## Verification Plan

Rust tests:

- CLI config parsing
- missing-model validate-only failure
- provider parsing for DirectML/Core ML/CPU
- batch request ordering and per-item errors
- non-finite embedding rejection

C++ tests:

- semantic table create/update/delete cleanup
- project package/checksum includes semantic tables
- fake runtime text/image embedding paths
- generation job progress/cancel/error handling
- thumbnail pin released on success, failure, and cancel
- semantic search ranking within root and folder scopes
- label-only search without runtime

Manual smoke tests:

- download model from default source
- download model from custom mirror source
- start/stop runtime repeatedly
- import images, accept semantic generation, cancel mid-run, retry
- search text query and label query
- package/reopen project and verify semantic rows survive

## Open Decisions

- Whether Phase 1 should add full C++ gRPC dependencies, or use a smaller local
  bridge API while keeping Rust gRPC internal. Direct C++ gRPC is cleaner long
  term if dependency size is acceptable.
- Whether default semantic generation should request `k256` only or warm `k1024`
  previews for newly imported images.
- Whether default labels should stay close to the Python demo list or become a
  user-editable taxonomy with regenerated text prototypes.
- Whether semantic embeddings should be project-local only or share a global
  cross-project cache keyed by image fingerprint and model key.

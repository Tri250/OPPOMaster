# Semantic Generation and Search Integration Plan

Date: 2026-06-12

Status: Phase 1 complete; Phase 2 complete; Phase 3 complete; Phase 4a
initial scaffold complete; Phase 4c complete; Phase 4d complete; Phase 5
deferred; Phase 6a complete; Phase 6 planning updated

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

## Model Identity, Distribution, and Download

The packaged app should not bundle model weights. It should bundle:

- a model manifest JSON
- Hugging Face model id and pinned revision
- required relative files
- expected SHA-256 and byte sizes
- embedding dimension and image input size
- precomputed default text-label prototypes

The active Hugging Face `model_id` is the semantic data compatibility boundary.
Image embeddings, generated labels, and cached label prototypes are valid only
when their stored `model_id` matches the currently selected model. If the user
switches models, old rows must not be read for generation skip logic, ordinary
label search, semantic search, or thumbnail label display. The app should expose
old-model rows as removable semantic data, not silently mix them with active
model results.

The Rust side owns model acquisition and local asset validation. Qt owns only
the settings UX and job orchestration: selected model, endpoint preset/custom
endpoint, optional Hugging Face token, target root, progress display, cancel,
retry, delete, and active-model selection. Qt should send these values to Rust
through explicit model-manager requests instead of implementing Hugging Face
download logic in C++.

Rust downloads into an application data directory resolved by Qt settings, for
example:

- Windows: `%LOCALAPPDATA%/AlcedoStudio/models/...`
- macOS: `~/Library/Application Support/AlcedoStudio/models/...`

Downloader requirements:

- user-configurable endpoint, with a domestic Hugging Face mirror as a preset
- atomic staging directory, then rename on success
- resumable `.part` files where practical
- per-file hash validation before marking the model usable
- clear "missing", "downloading", "ready", and "corrupt" states in settings
- request parameters for endpoint and optional Hugging Face token; tokens should
  not be logged and should not be persisted unless a later credential-store flow
  is deliberately added
- model/profile-driven file manifests instead of MobileCLIP-only hardcoded
  asset paths

The inference runtime should still validate only when loading a model for
generation or search. Downloading should be a separate model-manager job so a
missing model can produce a downloader-ready state instead of preventing the
Rust sidecar from starting.

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

## Current Database Interaction Notes

This section describes the implementation that exists through Phase 4c, plus
the DB-facing hooks that Phase 4d now calls. It is meant to document behavior,
not necessarily endorse every choice.

### Project open and storage lifetime

- `StorageService` owns one `DBController` plus one long-lived
  `SemanticStorageController`; the semantic controller keeps its own DuckDB
  connection guard for the lifetime of the storage service.
- `DBController::InitializeDB()` runs on both new and existing project
  databases. For existing projects it only runs the semantic schema string,
  then seeds default label query rows.
- The semantic schema uses `CREATE TABLE IF NOT EXISTS` and normal secondary
  indexes for model/file and label lookup. It does not create the HNSW index at
  project open.
- Default label queries are seeded on every project open inside one transaction
  using `INSERT OR REPLACE INTO SemanticLabelQuery`. At the current default
  config this rewrites the small bundled photography label set, not the image
  embedding corpus.
- `SemanticLabelQuery` is project-local seed/config data. It is not passed into
  generation jobs as a per-job input.

### Semantic model registration

- Before a generation workflow starts, the album semantic controller asks the
  runtime for model info, builds a model key from that info, then calls
  `SemanticStorageController::UpsertModel(...)`.
- Model registration is an `INSERT OR REPLACE` into `SemanticModel`.
  Re-running generation for the same model key refreshes that row rather than
  creating a new model row.
- The storage layer currently accepts only 512-dimensional embeddings because
  `SemanticImageEmbedding.embedding` and `SemanticLabelPrototype.embedding` are
  `FLOAT[512]` columns for DuckDB HNSW compatibility.

### Label prototype creation and cache behavior

- At the start of a persistent generation job,
  `EnsureCachedLabelPrototypes(...)` counts label query rows for
  `prompt_config_hash`, then counts existing prototype rows for
  `model_key + prompt_config_hash`.
- If `prototype_count >= query_count`, the job does not call `EmbedText` and
  does not rewrite prototype rows. It proceeds without loading prototype vectors
  into the app process.
- If prototypes are missing, the job reads all label query rows ordered by
  label, embeds each query text via the runtime, validates each returned vector,
  and writes all prototypes through `UpsertLabelPrototypes(...)`.
- `UpsertLabelPrototypes(...)` wraps the group write in one transaction, but
  each row still goes through `UpsertLabelPrototype(...)`, which validates the
  registered model and emits an `INSERT OR REPLACE` for that label.
- After the ensure step, the generation job does not load prototype vectors into
  app memory. Label assignment is deferred to the storage transaction that
  writes each image embedding.

### Per-image generation persistence

- Both the UI controller and `SemanticGenerationService::RunJob()` check
  `HasReadyImageEmbedding(file_id, image_id, model_key, require_label=true)`
  when force-regenerate is false. This causes per-candidate DB reads before
  thumbnail and runtime work.
- Image embedding RPCs are batched, but persistence is per item after each
  batch result is mapped back to its request id.
- For each successful image embedding, the service validates the vector and
  calls `UpsertImageEmbeddingAndAssignLabel(...)`; it does not receive or keep
  prototype vectors.
- `UpsertImageEmbeddingAndAssignLabel(...)` validates the model exists,
  validates the vector dimension and finite/non-zero values, then opens a DuckDB
  transaction.
- The per-image transaction deletes the existing
  `SemanticImageEmbedding(file_id, model_key)` row, deletes the matching
  `SemanticImageLabel(file_id, model_key)` row, inserts the new ready embedding,
  ranks `SemanticLabelPrototype` rows for `model_key + prompt_config_hash` with
  DuckDB's exact `array_inner_product`, writes the top/second/top-N label result
  row, and commits.
- Label assignment intentionally does not use HNSW. The prototype table is tiny,
  and exact DB-side dot product keeps classification deterministic while still
  leaving vector storage and page residency to DuckDB.
- There is no explicit "pending" or "failed" row written for failed/canceled
  generation items. Failed/canceled state is reported through the job result,
  not persisted in the semantic tables.

### Search and vector index behavior

- `SemanticStorageController::SearchImageEmbeddings(...)` validates the query
  vector and calls `EnsureVectorSearchIndex(model_key)` before running the
  ranked query.
- `EnsureVectorSearchIndex(...)` loads DuckDB `vss` lazily, first trying the
  `ALCEDO_DUCKDB_VSS_EXTENSION` path, then packaged executable-adjacent paths,
  then `LOAD vss`.
- On each ensure call it sets
  `hnsw_enable_experimental_persistence = true` and runs
  `CREATE INDEX IF NOT EXISTS idx_semantic_image_embedding_hnsw` on
  `SemanticImageEmbedding USING HNSW (embedding)`.
- The search query ranks in SQL using `array_distance` and limits the nearest
  candidate set to `max(offset + limit, 256)` before joining to the folder
  scope from `BuildScopedFileQuery(folder_id)`.
- At this point `SleeveFilterService` only has a provider hook for semantic
  search; no concrete provider is wired in this file set, so the storage search
  primitive can exist without being reachable from normal search UI.

### Cleanup and packaging participation

- Project data summaries include the semantic tables, so semantic row counts
  participate in the lightweight project summary/check path.
- `DeleteImageEmbeddingsForFiles(...)` exists and deletes embedding and label
  rows for a file-id list, but the current production grep only finds the
  method definition and tests. The normal delete/sync path should be audited
  before assuming semantic rows are always cleaned when files are removed.
- Prototype rows are project/model/config cache data. There is no automatic
  pruning of old `SemanticLabelPrototype` rows when a model key or prompt config
  is superseded.

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

Global search should keep the existing metadata-first behavior as the default.
The user-facing control is a visible toggle named `Semantic Search` / `语义搜索`,
not a required mode picker. When the toggle is off, every query uses the
traditional search path:

- file name, element name, and path matching
- EXIF/document matching, including camera, lens, date, ISO, focal length, and
  aperture
- generated semantic labels/tags as ordinary searchable text once labels exist
- recommendation rows and exact-file application

When the toggle is on, query routing is still conservative:

- Empty text shows normal recommendations and does not start the semantic
  runtime.
- A direct label/tag query is resolved locally through the normal search path
  and does not start the semantic runtime. This includes exact label names,
  normalized/case-insensitive label names, and explicit tag syntax such as
  `#portrait` if that syntax is added.
- A query with clear metadata intent, such as a camera model, lens string, date,
  filename fragment, or EXIF-shaped numeric token, uses normal search first.
- Only a non-label free-text query, with the `Semantic Search` toggle enabled,
  starts or acquires `SemanticRuntimeService`, embeds the text query, and asks
  storage for a VSS/HNSW-ranked page.
- If semantic routing fails because the model is missing, runtime startup fails,
  or the VSS extension/index is unavailable, surface that semantic error
  explicitly. Do not silently replace the semantic branch with a full-vector C++
  scan. The user can turn the toggle off to run ordinary matching.

Phase 5 should not make semantic search run on every keystroke by default.
Traditional preview can keep its current short debounce. Semantic preview should
run only when the user presses Enter or clicks the search button. A development
experiment may measure debounced semantic preview with cancellation and a
single-flight request guard, but the shippable default remains explicit-submit
until text embedding plus HNSW paging is proven interactive with MobileCLIP on
real catalogs.

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

5. Search integration and query routing
    - 5a. Traditional search baseline and label participation
      - keep `SleeveFilterService::BuildFuzzySearchWhere(...)` as the default
        path when semantic search is disabled
      - extend the normal search document to include generated
        `SemanticImageLabel.label` values without requiring the runtime
      - preserve filename, element name, image path, and EXIF matching behavior
        exactly for ordinary queries
      - keep recommendation rows local; label/tag recommendations should be
        backed by stored labels, not by runtime text embeddings
      - add focused tests proving that typing a generated label name returns
        results through the ordinary path
    - 5b. Visible semantic-search toggle and persisted UI state
      - add a compact `Semantic Search` / `语义搜索` toggle to
        `GlobalSearchDialog.qml`
      - persist the preference in `QSettings`, but default it off for existing
        users and new projects
      - expose the toggle through `SearchController` so QML does not decide
        runtime behavior by itself
      - keep ordinary preview debounced while the toggle is off
      - add an explicit submit affordance for semantic search: Enter and a
        search button should both call the same controller method
    - 5c. Query intent classifier
      - centralize routing in C++ near `SearchController` or
        `SleeveFilterService`; QML should only pass query text and toggle state
      - normalize query text for direct label matching against
        `SemanticLabelQuery` and assigned `SemanticImageLabel` values
      - treat exact label names, known synonyms, and explicit tag syntax as
        traditional label/tag search even when the toggle is on
      - treat EXIF-shaped tokens, dates, camera/lens strings, filenames, and
        short structured fragments as ordinary search
      - route only non-label natural-language text to semantic search
      - expose the chosen route in testable data, for example
        `traditional`, `label`, `semantic`, or `empty`
    - 5d. Concrete semantic provider
      - implement the concrete `SemanticSearchProvider` and register it from
        `ProjectService` after storage and runtime services exist
      - acquire `SemanticRuntimeService` only for the semantic route, using the
        same ad hoc lifecycle rule as generation
      - fetch/validate model info, derive the active model key, and reject
        search when the model key is not registered in storage
      - call text embedding once per submitted query, validate the returned
        vector, then call `SemanticStorageController::SearchImageEmbeddings`
      - keep DuckDB VSS/HNSW as the only ranked vector path; missing extension
        or index is an actionable semantic-search error, not a C++ scan fallback
      - return rows in the same lightweight shape as `FuzzySearchMatch` so
        preview thumbnail handling stays shared
    - 5e. Preview pagination, counts, and result lifecycle
      - keep existing preview thumbnail pin/release behavior for semantic rows
      - support paged semantic previews with `offset`/`limit`; do not fetch the
        whole vector corpus or materialize giant result lists in QML
      - decide count semantics explicitly: either return an approximate
        `hasMore` response for semantic pages, or add a storage-owned count/page
        token; do not fake a full count by scanning vectors in C++
      - cancel or ignore stale semantic requests when a newer submitted query
        replaces them
      - show loading, empty, and error states in the existing search dialog
        without closing the dialog before progress is visible
    - 5f. Apply-to-album-grid path
      - ordinary searches continue to apply as SQL `WHERE` filters
      - semantic searches should apply through a storage-owned result token or
        scoped temporary result table, not a giant `IN (...)` list in UI code
      - tie the applied result token to folder scope, query text, model key, and
        a generation/version marker so stale result sets can be invalidated
      - make clearing search release the semantic result scope and restore the
        normal album query path
      - keep root and folder scope aligned with
        `ElementController::BuildScopedFileQuery`
    - 5g. Realtime-search experiment
      - measure a debounced semantic preview behind a development flag or local
        instrumentation only
      - record text embedding latency, runtime startup latency, VSS query
        latency, cancellation behavior, and UI frame impact on realistic albums
      - only promote realtime semantic preview if repeated text embeddings are
        compatible with the active MobileCLIP runtime and stay comfortably
        interactive; otherwise keep Enter/button submission as product behavior
      - cache repeated query embeddings by normalized query + model key only
        after correctness and invalidation rules are defined

6. Model identity, Rust download manager, and packaging
   - 6a. Model identity and storage compatibility
     - complete: keep `model_key` as the compatibility boundary for stored
       semantic rows. In the current implementation the key is derived from
       `model_id@revision`, so the embedding, label, prototype, skip-check,
       label-display, ordinary-search, semantic-search, and cleanup paths are
       already scoped without duplicating `model_id` into every row table.
     - complete: add an explicit active-model flag to `SemanticModel`; ordinary
       search and label stats read generated labels only for the active model.
       If there is no active model, generated labels are unavailable rather than
       mixed into ordinary search.
     - complete: extend `SemanticModel` with model/profile metadata needed for
       multilingual CLIP models, including engine/profile id, supported text
       languages JSON, and manifest JSON. UI can later let the user choose the
       language when downloading/selecting a model.
     - complete: old rows for non-active models stay stored but hidden from
       label display, ordinary label search, and label statistics until that
       model is activated again.
     - complete: keep default label queries simple and reusable, while cached
       label prototypes remain scoped by `model_key + prompt_config_hash`.
   - 6b. Embedding dimension policy
     - keep DuckDB VSS/HNSW as a hard requirement for semantic vector ranking;
       do not add a C++ full-scan fallback for unsupported model dimensions
     - support 512-dimensional multilingual CLIP models first, matching the
       existing `FLOAT[512]` storage and HNSW index path
     - reject non-512-dimensional models with an actionable error until
       dimension-specific embedding/prototype tables or a migration strategy
       are implemented
     - validate model-reported embedding dimension before generation, label
       prototype writes, and semantic search
   - 6c. Rust model-manager RPC surface
     - start the Rust sidecar even when no inference model is installed, so the
       app can ask it to validate, download, cancel, or delete models
     - add explicit model-management RPCs such as `ListInstalledModels`,
       `ValidateModel`, `StartModelDownload`, `GetModelDownloadStatus`,
       `CancelModelDownload`, `DeleteModel`, and `LoadModel` or `SelectModel`
     - include `model_id`, `revision`, profile id, endpoint, optional HF token,
       target root, force, and resume flags in download requests
     - keep `hf_token` out of logs and avoid persisting it by default
     - keep `EmbedTextBatch`, `EmbedImageBatch`, and `GetModelInfo` behind a
       successfully loaded model, not as download side effects
   - 6d. Rust download implementation
     - move the current `hf-hub` download path out of inference-engine startup
       and into the model manager
     - replace MobileCLIP-only constants with profile-driven asset manifests for
       text model, vision model, tokenizer, tokenizer config, preprocessing
       config, and model config files
     - download into a staging directory, write `.part` files where practical,
       validate file size and SHA-256, then atomically promote to the ready
       model directory
     - write a local resolved manifest containing `model_id`, revision, profile
       id, embedding dimension, image size, file list, byte sizes, hashes, and
       validation time
     - report `missing`, `downloading`, `ready`, `corrupt`, `canceled`, and
       `failed` states with user-actionable error text
   - 6e. Qt settings and runtime wiring
     - keep download UX in Settings, but call Rust model-manager RPCs for all
       Hugging Face network and file operations
     - expose selected model, endpoint preset/custom endpoint, optional HF
       token, model directory, status, progress, cancel, retry, delete, and
       active-model selection
     - pass the active model's resolved root, `model_id`, revision, and device
       to `SemanticRuntimeService`; release builds should use validate-only
       inference startup
     - remove the temporary development `model-root` fallback that probes
       `rust/puerh_mind/models/mobileclip2-s2-openclip`; release builds must
       pass a settings/model-manager-resolved model root explicitly
     - show old semantic data as generated by another model and offer delete or
       switch actions instead of silently mixing it into active labels/search
   - 6f. Generation and label compatibility gates
     - before thumbnail work starts, verify runtime `GetModelInfo.model_id`,
       revision, image size, and embedding dimension against the active model
       record
     - register the active model before generation writes and reject writes when
       response model identity or vector dimension does not match
     - make `HasReadyImageEmbedding` and force-regenerate logic check the active
       `model_id`, not just file/image id
     - make ordinary text search include generated labels only for the active
       `model_id`
     - make semantic data deletion support active-model-only cleanup and
       old-model cleanup
   - 6g. Packaging and smoke tests
     - install the Rust binary next to the app executable or inside the app
       bundle
     - deploy ONNX Runtime dynamic libraries needed by the selected execution
       providers
     - deploy DuckDB `vss.duckdb_extension` when ANN search is enabled
     - package small manifests/profiles and default label-query config, but not
       model weights
     - smoke-test installed runtime startup without models, model validation
       failure, model-manager status, successful validate/load with local model
       assets, `Ping`, and `GetModelInfo`

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
- model-manager startup without installed model assets
- model-manager download job status, cancellation, corrupt-file reporting, and
  endpoint/token request handling with token redaction
- profile-driven asset manifest validation for MobileCLIP and multilingual CLIP
- provider parsing for DirectML/Core ML/CPU
- batch request ordering and per-item errors
- non-finite embedding rejection

C++ tests:

- semantic table create/update/delete cleanup
- model identity migration/registration keeps old `model_key` rows isolated
  behind the active Hugging Face `model_id`
- project package/checksum includes semantic tables
- fake runtime text/image embedding paths
- generation job progress/cancel/error handling
- thumbnail pin released on success, failure, and cancel
- generation skip logic ignores embeddings generated by a different `model_id`
- label display and ordinary label search ignore labels generated by a different
  `model_id`
- model mismatch states expose delete/switch actions instead of reading stale
  embeddings or labels
- unsupported embedding dimensions fail before storage writes or vector search
- semantic search ranking within root and folder scopes
- label-only search without runtime
- normal search still matches filename, element name, EXIF, camera, lens, date,
  ISO, focal length, and aperture with semantic search disabled
- generated label names participate in ordinary search without starting the
  semantic runtime
- query routing classifies empty, metadata, label/tag, and natural-language
  queries deterministically
- when the semantic-search toggle is enabled, label/tag queries still use the
  ordinary path and natural-language queries use the semantic provider
- semantic provider starts/acquires the runtime only for submitted semantic
  queries and releases it according to the ad hoc lifecycle owner
- semantic provider surfaces missing model, runtime failure, bad embedding, and
  missing VSS extension/index errors without falling back to C++ vector scans
- semantic preview pagination ignores stale results after a newer submitted
  query and keeps thumbnail pin/release behavior intact
- applying a semantic result set to the album grid respects root/folder scope
  and does not build a giant UI-owned `IN (...)` filter

Manual smoke tests:

- download model from default source
- download model from custom mirror source
- download model with a provided HF token and verify the token is not logged
- switch between the default MobileCLIP model and a multilingual CLIP model, then
  verify old embeddings and labels are hidden until switching back
- delete semantic data for an old model without deleting active-model rows
- start/stop runtime repeatedly
- import images, accept semantic generation, cancel mid-run, retry
- with semantic search off, search filename, EXIF strings, dates, generated
  labels, and tag-like text
- with semantic search on, type a generated label name and verify the runtime
  does not start
- with semantic search on, submit a natural-language query with Enter and with
  the search button, then page results
- verify typing alone does not fire semantic requests in the default product
  path
- if the realtime experiment flag is enabled, compare debounce latency and
  cancellation behavior against explicit-submit semantic search
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

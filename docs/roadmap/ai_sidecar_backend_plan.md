# AI Sidecar Backend Integration Plan

Date: 2026-06-25

Status: Planning. SAM and smart mask work are explicitly deferred until the editor pipeline
has first-class mask capability.

## Background

`rust/puerh_mind` is currently a local semantic sidecar. Its gRPC surface is centered on
health checks, model management, CLIP image embeddings, and semantic search generation. That
shape is correct for the first semantic-search milestone, but the next AI features will not all
look like CLIP inference.

Near-term candidates include:

- Image understanding through a multimodal LLM API, producing a score, tags, and a one-line
  caption that can be written into the database search document.
- Future local or remote AI tasks that use very different request payloads, provider settings,
  and timeout behavior.
- A later edit-assistant workflow that proposes non-destructive adjustment recipes for the C++
  editor pipeline.

The exchange format should therefore be unified only at the control plane. Each task still owns
its task-specific protobuf payload, because embeddings, captioning, scoring, and editor recipes
do not share a meaningful request schema. What should be common is the envelope around those
payloads: request identity, task name, timeout, cancellation, priority, trace metadata, capability
description, and credential handles.

## Current C++ Integration Points

- `ProjectService` lazily owns `SemanticRuntimeService`; this is the C++ entry point that starts
  the sidecar process and hands runtime access to album/semantic flows.
- `SemanticRuntimeService` owns `QProcess`, readiness polling, command-line arguments, and the
  gRPC DTO bridge. Its thread-hop behavior in `StartAndWait` must be preserved when the runtime
  becomes more general.
- `GrpcSemanticRuntimeClient` currently creates semantic and model-manager stubs directly and
  sends semantic protobuf requests without a common AI request header.
- Model download and model-profile settings are currently C++-owned through
  `ModelDownloadService` and `ModelDownloadController`, using the local aria2-based download
  path. The AI sidecar plan should not assume Rust owns semantic asset acquisition today.
- Search indexing currently flows through Sleeve storage and `SleeveFilterService` search
  document construction. Captions and AI tags need explicit storage fields or tables before they
  can enter full-text search.
- C++ CMake generation currently targets `rust/puerh_mind/proto/semantic.proto`. A general AI
  sidecar will need multi-proto generation on both Rust and C++ sides.
- The editor pipeline does not yet have a product-level persistent mask/local-adjustment model.
  SAM integration is therefore not part of this plan.

## Design Principles

- Keep execution APIs task-specific. Do not introduce a universal
  `Invoke(task_name, json_payload)` interface.
- Add a small shared protobuf control surface for common fields, then let each task define its
  own typed payload and response.
- Keep host ownership clear. C++ owns project state, persistence, UI policy, model download
  settings, and database writes. The sidecar computes results and reports structured outcomes.
- Treat credentials as short-lived capabilities. Do not pass long-lived API keys through command
  line arguments, persistent logs, or sidecar startup environment by default.
- Preserve semantic compatibility while migrating. Existing semantic generation/search should
  keep working during each phase.
- Keep the binary name `alcedo_mind.exe` initially. Renaming the sidecar can be a later packaging
  cleanup once the API shape is stable.

## Remote LLM Technical Stack And Security Decisions

The first remote provider path should be implemented in Rust inside `alcedo_mind`, not in QML or
direct UI code. The C++ host remains responsible for settings, project state, persistence, and
database writes; Rust owns only the outbound provider call and runtime-only secret handling.

Recommended Rust stack:

- Use `reqwest` on the existing Tokio runtime for HTTPS JSON calls. Keep the provider layer thin
  and task-specific instead of adopting a broad multi-provider LLM SDK in the first iteration.
- Pin `reqwest` with `default-features = false` and an explicit Rustls TLS feature when added to
  `Cargo.toml`. Do not enable invalid certificate acceptance or plaintext HTTP endpoints for
  production providers.
- Build a small `RemoteVisionProvider` / `OpenAiVisionProvider` adapter around the provider REST
  API. It should map provider errors, rate limits, request ids, and usage/cost metadata into the
  typed AI response, not leak provider JSON directly through C++.
- Use provider structured-output support for caption/tag/rating responses when available. The Rust
  service must still validate and normalize the response before returning protobuf fields.
- Do not stream in the MVP. Use non-streaming calls for deterministic timeout, cancellation, and
  schema-validation behavior. Streaming can be added later for assistant-style workflows.
- Keep retries conservative. Retrying a provider call can duplicate cost, so only retry transient
  transport / 429 / 5xx failures under a small, bounded policy and surface the provider request id
  when available.
- Log correlation ids, task ids, provider id, model id, latency, status, and provider request id.
  Never log prompt payloads, image bytes/base64, API keys, credential handles, or raw provider error
  bodies before redaction.

Credential ownership is split deliberately:

- Long-lived user API keys are persisted by the C++/Qt host, not by Rust.
- The persisted store must be an OS credential store: Windows Credential Manager, macOS Keychain,
  and Linux Secret Service / KWallet where supported. `QSettings` may store only non-secret metadata
  such as provider id, selected model, masked key label, and "remember key" preference.
- A practical implementation path is to add a small host `AiCredentialStore` abstraction backed by
  QtKeychain or a minimal platform-native wrapper. QtKeychain is attractive because it already maps
  to Windows Credential Store, macOS Keychain, and Linux desktop keyrings and has vcpkg/Homebrew
  availability; using it should still be gated by a focused dependency/build review.
- When starting a remote task, C++ reads the secret from the OS credential store, calls
  `AiRuntimeService.RegisterCredential`, receives an opaque handle, and passes only that handle in
  task headers. Rust keeps the secret only in memory with TTL/revoke/redaction.
- On sidecar stop, project close, provider logout, or settings deletion, C++ must revoke runtime
  handles and delete persisted credentials when requested.
- Developer override via `OPENAI_API_KEY` or equivalent is allowed for tests/manual smoke, but it is
  not the normal product persistence path and must not be copied into QSettings.

## Rating vs Understanding Boundary

Rating and understanding should be separate task semantics even if a provider can answer both in one
HTTP call.

- `image_understanding.describe`: objective-ish image content. Outputs caption, searchable tags,
  scene/category hints, and optional confidence. These fields can participate in the search document
  when marked active.
- `image_rating.score`: subjective or product-policy scoring. Outputs one or more numeric scores
  such as keeper score, aesthetic score, technical quality, or curation priority, plus short reasons.
  Rating is not full-text search content by default; it should drive sort/filter/recommendation
  workflows only after the product contract is clear.
- Shared plumbing is fine: both tasks may use the same credential handle, HTTP client, provider
  adapter, image rendition selection, timeout, cancellation, and redaction path.
- Storage should keep `task_id` / `prompt_profile_id` / provider / model identity with each result
  so a future rating rubric change does not silently overwrite or reinterpret earlier understanding
  annotations.

## Shared Control Surface

Add protobuf messages similar to:

- `AiRequestHeader`
  - `request_id`
  - `task_id`
  - `deadline_ms` or `timeout_ms`
  - `priority`
  - `trace_id`
  - `credential_ref`
  - `client_capabilities`
- `AiResponseHeader`
  - `request_id`
  - `task_id`
  - `status`
  - `error_code`
  - `error_message`
  - `provider`
  - `model_id`
  - `elapsed_ms`
- `AiCapability`
  - `task_id`
  - `provider_id`
  - `model_id`
  - `input_kinds`
  - `output_kinds`
  - `supports_batch`
  - `supports_cancel`
  - `requires_credential`
  - `max_payload_bytes`

This layer should live beside, not inside, task-specific protobuf files. For example:

- `proto/ai_common.proto` for headers, status, capability descriptors, and credential refs.
- `proto/ai_runtime.proto` for sidecar-level capabilities, credential registration, and
  cancellation.
- `proto/semantic.proto` remains the typed semantic embedding service during migration.
- `proto/image_analysis.proto` or `proto/image_understanding.proto` adds typed image
  understanding and rating task messages; `task_id` distinguishes searchable understanding from
  subjective rating.
- Future task protobufs can be added without changing existing task contracts.

## Mandatory Phase Rule

Every phase must end with both:

- Focused tests for the changed Rust, C++, storage, UI, or packaging surface.
- A self code-review conclusion before moving to the next phase.

The self-review conclusion must be written in the phase handoff or PR notes using this shape:

`Review conclusion: <bugs found or none>; <risk accepted or none>; <missing tests or none>.`

Do not advance to the next phase if phase tests are failing or if the review conclusion contains
an unresolved high-priority correctness, credential-handling, persistence, or compatibility issue.

## Phase 0 - Contract Inventory And Gates

Goal: freeze the compatibility boundary before adding new services.

Deliverables:

- Write the exact shared header fields and status-code mapping.
- Record which existing semantic RPCs stay legacy-compatible during migration.
- List the generated C++ and Rust protobuf targets that must exist after Phase 1.
- Define the minimum fake-runtime behavior required for C++ tests.
- Define where AI annotations will be stored, including model/provider identity and whether the
  result is active for search.

Tests:

- Documentation review only unless files are moved.
- No code phase starts until the acceptance checklist is explicit.

Self-review focus:

- Check that this phase does not mix control-plane design with a universal provider abstraction.
- Check that all existing semantic user flows have a compatibility path.

## Phase 1 - Proto And Runtime Control Plane

Goal: add the general AI sidecar protobuf foundation without changing existing semantic behavior.

Deliverables:

- Add `ai_common.proto` and `ai_runtime.proto`.
- Update `rust/puerh_mind/build.rs` and `src/proto.rs` to compile and expose the new protos.
- Update C++ CMake protobuf generation so C++ stubs can be generated for multiple proto files.
- Add Rust service registration for a sidecar capability/runtime service.
- Keep existing `HealthService`, `ModelManagerService`, and `SemanticService` behavior intact.

Tests:

- `cargo test` in `rust/puerh_mind`.
- C++ configure/build enough to prove generated headers and stubs compile.
- Existing `SemanticRuntimeServiceTest` should still pass with the fake runtime.

Self-review focus:

- Check generated file paths and include names for Windows/MSVC stability.
- Check that old semantic clients and fake runtime fixtures were not broken.

## Phase 2 - C++ Runtime Neutralization

Goal: let the C++ host treat the process as an AI sidecar while preserving semantic entry points.

Deliverables:

- Introduce neutral runtime DTOs for sidecar endpoint, process state, capability status, and
  startup options.
- Either rename `SemanticRuntimeService` carefully or add a small `AiSidecarRuntimeService`
  wrapper around the existing process owner. Keep the public semantic facade stable until
  semantic migration is complete.
- Preserve existing process lifetime behavior, logs, timeouts, readiness polling, and
  `require_model_info=false` model-manager startup mode.
- Extend the fake runtime so tests can expose capability responses as well as semantic responses.

Tests:

- `AiSidecarRuntimeServiceTest` (renamed from `SemanticRuntimeServiceTest` in Phase 2).
- Fake runtime tests for startup args, readiness, crash handling, hung stop, and capability query.
- Manual smoke if the sidecar command-line contract changes.

Self-review focus:

- Check that `QProcess` is only touched on its owning thread.
- Check that model-manager-only startup still works without local model assets.
- Check that ordinary album/search startup does not launch extra AI work.

## Phase 3 - Capability Registry, Credential Handles, And Host Credential Store

Goal: support remote providers without making users re-enter keys every session and without leaking
long-lived secrets into sidecar process launch, QSettings, persistent logs, or crash messages.

Deliverables:

- Add a Rust in-memory credential vault with registration, TTL, revoke, and no-log redaction.
- Add C++ APIs for creating credential handles and passing only the handle in task headers.
- Add capability descriptors for local semantic embedding and remote image understanding.
- Add cancellation by `request_id` or task operation id.
- Add a C++ `AiCredentialStore` interface for long-lived user API keys. Back it with the platform
  secure credential store, preferably through QtKeychain or a narrow native wrapper after dependency
  review.
- Store only non-secret metadata in QSettings: provider id, selected model/profile, masked key label,
  and whether the user enabled persistence.
- Define the task flow from persisted key -> C++ loads secret -> `RegisterCredential` -> runtime
  handle -> task header -> Rust resolves handle for the provider call.
- Keep actual secure credential persistence out of Rust. Rust may keep secrets only in memory for the
  lifetime/TTL of the registered handle.

Tests:

- Rust tests for register, resolve, TTL expiry, revoke, and redaction.
- C++ fake-runtime tests proving API key material is not present in process args or routine logs.
- Cancellation tests with a delayed fake operation.
- C++ credential-store tests with a fake backend for save/read/delete, metadata-only QSettings, and
  "remember key" off/on behavior.
- Manual smoke on Windows Credential Manager and macOS Keychain before treating persisted keys as
  shippable.

Self-review focus:

- Check that keys never appear in command-line arguments, persistent logs, or crash messages.
- Check that credential handles cannot silently outlive their intended session.
- Check that QSettings and project files never contain raw key material.
- Check that deleting or replacing a key revokes the in-memory sidecar handle.

## Phase 4 - Semantic Embedding V2 Migration

Goal: move semantic embedding onto the shared AI control surface while keeping current search
generation stable.

Deliverables:

- Add request/response headers to the semantic embedding path, either in a v2 RPC or compatible
  wrapper messages.
- Update C++ semantic runtime client code to fill request ids, timeout, task id, and trace fields.
- Preserve existing batching, request-id to file-id mapping, model-info validation, and embedding
  persistence behavior.
- Keep a legacy fallback path until the new semantic fake runtime and real runtime tests are
  stable.

Tests:

- Rust semantic batch tests.
- `SemanticGenerationServiceTest`.
- `SemanticStorageControllerTest`.
- `FilterServiceTest` for existing semantic labels/search behavior.
- Environment-gated live runtime smoke when model assets are available.

Self-review focus:

- Check that embedding vector dimensions, model keys, and persistence compatibility are unchanged.
- Check that timeout/cancellation semantics match the old C++ expectations.

## Phase 5 - Remote Image Analysis MVP

Goal: add the first non-CLIP remote AI task over Rust HTTPS: typed image understanding, with rating
kept as a separate task contract even if the first provider request can return both.

Deliverables:

- Add typed protobuf messages for image analysis. At minimum include
  `image_understanding.describe`; add `image_rating.score` either in the same proto file or a sibling
  proto, but keep distinct `task_id`s and result identities.
- Add Rust provider traits for remote vision analysis, starting with a mock provider and one
  OpenAI-compatible provider behind credential handles.
- Add the Rust HTTP stack: `reqwest` client construction, provider config, bearer auth, structured
  output schema, timeout, bounded retry policy, provider request-id capture, and secret redaction.
- Add a C++ `ImageUnderstandingService` that requests thumbnails/previews from existing host
  services, calls the sidecar, and returns structured results.
- Add storage for AI image annotations with file id, provider id, model id, prompt/profile id,
  caption, tags, score, created time, and active-for-search state.
- Extend search document construction so active captions/tags can participate in full-text search.
- Persist ratings separately from searchable understanding fields, or at least gate them with a
  separate `task_id` so rating does not accidentally become full-text search material.

Tests:

- Rust mock-provider and timeout tests.
- Rust HTTP provider tests using a local mock server. Cover authorization header placement, no key in
  logs/errors, schema validation, rate limit mapping, provider request id capture, and cancellation.
- C++ service tests with a fake sidecar client.
- Storage controller tests for insert, replace, active selection, and model/provider identity.
- `FilterServiceTest` or equivalent coverage showing captions/tags are searchable.
- QML/controller smoke tests only if UI is introduced in this phase.

Self-review focus:

- Check that the sidecar does not write directly to the database.
- Check that prompt/profile/model identity is persisted with each annotation.
- Check that failed remote calls do not create partial active search documents.
- Check that understanding and rating outputs cannot overwrite each other across prompt/profile
  changes.
- Check that prompt/image payloads and raw provider response bodies are absent from routine logs.

## Phase 6 - Product Wiring For Credentials, Caption, And Rating

Goal: make remote image analysis usable from the album workflow without disturbing ordinary search or
requiring users to re-enter API keys every session.

Deliverables:

- Add settings/controller flow for remote-provider availability, credential entry, remember/delete
  key behavior, selected provider/model, and connection validation.
- Add an album action for generating or refreshing captions/tags.
- Add a separate rating action or an explicit combined action that writes separate
  `image_understanding.describe` and `image_rating.score` results.
- Add progress, cancellation, retry, and clear error states.
- Add a search-index refresh path after successful annotation persistence.
- Ensure the sidecar starts on demand and is not required for normal album browsing.
- Add provider usage/cost display when the response includes usage metadata. This may start as a
  per-job summary rather than a full billing dashboard.

Tests:

- Controller tests for missing credential, saved credential, delete credential, offline provider,
  cancel, retry, and successful write.
- QML smoke tests for visible states if UI is added.
- Targeted search tests proving new captions appear only after successful persistence.
- Manual smoke with the fake provider and, when configured by a developer, a real provider.

Self-review focus:

- Check user-visible error copy for credential and network failures.
- Check that normal search and browsing remain usable without API keys.
- Check that cancellation does not leave stale progress or half-active annotations.
- Check that no raw API key appears in QML state dumps, settings files, diagnostics logs, or packed
  projects.

## Future Candidate - Edit Assistant Recipes

This is a speculative AI scene that does not require editor mask support.

Goal: let a model propose non-destructive adjustment recipes, while C++ remains the only owner of
the edit graph.

Possible shape:

- C++ sends a low-resolution preview, selected metadata, current pipeline parameters, and a user
  intent such as "make this feel warmer but keep highlights safe".
- The sidecar returns an `AdjustmentRecipe` containing allowed operator names, parameter deltas,
  confidence, and a short explanation.
- C++ validates every operator name and parameter range before creating a candidate edit version.
- The sidecar never mutates `EditHistory`, project files, or pipeline state directly.

Required tests before product use:

- Validator rejects unknown operators and out-of-range values.
- Fake recipe creates a reversible candidate version.
- Existing editor undo/redo and version branching remain unchanged.

## Deferred - SAM And Smart Masks

SAM should not start in the current AI sidecar plan.

Prerequisites before reopening this work:

- The editor pipeline has a persistent mask/local-adjustment model.
- Mask coordinate spaces are defined across thumbnail, preview, full-resolution image, crop,
  rotate, and lens-correction stages.
- The UI has a clear overlay, refine, apply, undo, and versioning lifecycle.
- Storage has a decision for whether masks are project assets, edit-version assets, or derived
  cache artifacts.

Only after those prerequisites land should SAM get its own task-specific protobuf and product
roadmap phase.

## Validation Targets

Use targeted validation per phase rather than one giant test sweep every time.

Rust:

```powershell
cd rust/puerh_mind
cargo test
```

C++ build after generated-code or runtime-service changes:

```powershell
cmd /c scripts\msvc_env.cmd --build --preset win_debug --parallel 4
```

Targeted CTest group after semantic, storage, or search changes:

```powershell
ctest --test-dir build/debug --output-on-failure -R "AiSidecarRuntimeServiceTest|SemanticGenerationServiceTest|SemanticStorageControllerTest|FilterServiceTest|GlobalSearchDialogQmlTest|SearchQueryClassifierTest"
```

Live runtime tests remain environment-gated and should be run when the required model assets and
provider credentials are available.

## Open Decisions

- Whether image understanding should use thumbnails, previews, or caller-selected image renditions.
- Exact first remote provider and model defaults.
- Exact rating rubric: whether score means aesthetic quality, technical quality, keeper priority, or
  a weighted combination.
- Whether the first UI exposes rating separately or runs a combined understanding+rating job that
  stores separate task results.
- When to rename C++ classes and the sidecar executable from semantic-oriented names to AI-sidecar
  names.

## Research References

- OpenAI API docs: official SDKs are available for several languages, and direct HTTP clients are a
  supported path when no official SDK fits. Rust should therefore use direct HTTPS for the first
  provider instead of relying on an unofficial broad SDK.
- OpenAI API docs: API keys are bearer secrets and should be loaded from environment variables or a
  key-management service, not hard-coded or exposed client-side.
- OpenAI API docs: Responses API supports image input and structured outputs, which match the
  caption/tag/rating schema requirement.
- Rust `reqwest` docs: async HTTP client with JSON support, reusable clients, timeout/TLS
  configuration, and explicit TLS backend selection.
- QtKeychain docs: platform-independent Qt secret storage backed by Windows Credential Store, macOS
  Keychain, and Linux desktop keyrings.
- Rust `secrecy` / `zeroize` docs: explicit secret exposure and zeroing help prevent accidental
  logging and reduce in-memory lifetime risk; the current custom `SecretString` can stay if it keeps
  the same audit properties.

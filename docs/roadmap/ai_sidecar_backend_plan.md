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
- `proto/image_understanding.proto` adds caption, scoring, and tag extraction.
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

- `SemanticRuntimeServiceTest`.
- Fake runtime tests for startup args, readiness, crash handling, hung stop, and capability query.
- Manual smoke if the sidecar command-line contract changes.

Self-review focus:

- Check that `QProcess` is only touched on its owning thread.
- Check that model-manager-only startup still works without local model assets.
- Check that ordinary album/search startup does not launch extra AI work.

## Phase 3 - Capability Registry And Credential Handles

Goal: support remote providers without leaking long-lived secrets into sidecar process launch or
logs.

Deliverables:

- Add a Rust in-memory credential vault with registration, TTL, revoke, and no-log redaction.
- Add C++ APIs for creating credential handles and passing only the handle in task headers.
- Add capability descriptors for local semantic embedding and remote image understanding.
- Add cancellation by `request_id` or task operation id.
- Keep actual secure credential persistence out of Rust. C++ settings/UI can decide later whether
  and how to store user credentials.

Tests:

- Rust tests for register, resolve, TTL expiry, revoke, and redaction.
- C++ fake-runtime tests proving API key material is not present in process args or routine logs.
- Cancellation tests with a delayed fake operation.

Self-review focus:

- Check that keys never appear in command-line arguments, persistent logs, or crash messages.
- Check that credential handles cannot silently outlive their intended session.

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

## Phase 5 - Image Understanding MVP

Goal: add the first non-CLIP AI task: score, tags, and one-line caption for an image.

Deliverables:

- Add `image_understanding.proto` with typed request/response messages.
- Add Rust provider traits for image understanding, starting with a mock provider and one optional
  remote provider behind credential handles.
- Add a C++ `ImageUnderstandingService` that requests thumbnails/previews from existing host
  services, calls the sidecar, and returns structured results.
- Add storage for AI image annotations with file id, provider id, model id, prompt/profile id,
  caption, tags, score, created time, and active-for-search state.
- Extend search document construction so active captions/tags can participate in full-text search.

Tests:

- Rust mock-provider and timeout tests.
- C++ service tests with a fake sidecar client.
- Storage controller tests for insert, replace, active selection, and model/provider identity.
- `FilterServiceTest` or equivalent coverage showing captions/tags are searchable.
- QML/controller smoke tests only if UI is introduced in this phase.

Self-review focus:

- Check that the sidecar does not write directly to the database.
- Check that prompt/profile/model identity is persisted with each annotation.
- Check that failed remote calls do not create partial active search documents.

## Phase 6 - Product Wiring For Caption And Score

Goal: make image understanding usable from the album workflow without disturbing ordinary search.

Deliverables:

- Add settings/controller flow for remote-provider availability and credential entry.
- Add an album action for generating or refreshing captions and scores.
- Add progress, cancellation, retry, and clear error states.
- Add a search-index refresh path after successful annotation persistence.
- Ensure the sidecar starts on demand and is not required for normal album browsing.

Tests:

- Controller tests for missing credential, offline provider, cancel, retry, and successful write.
- QML smoke tests for visible states if UI is added.
- Targeted search tests proving new captions appear only after successful persistence.
- Manual smoke with the fake provider and, when configured by a developer, a real provider.

Self-review focus:

- Check user-visible error copy for credential and network failures.
- Check that normal search and browsing remain usable without API keys.
- Check that cancellation does not leave stale progress or half-active annotations.

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
ctest --test-dir build/debug --output-on-failure -R "SemanticRuntimeServiceTest|SemanticGenerationServiceTest|SemanticStorageControllerTest|FilterServiceTest|GlobalSearchDialogQmlTest|SearchQueryClassifierTest"
```

Live runtime tests remain environment-gated and should be run when the required model assets and
provider credentials are available.

## Open Decisions

- Final protobuf package naming, for example `alcedo.ai` vs a shorter `ai` package.
- Exact credential persistence policy on the C++ side.
- Whether image understanding should use thumbnails, previews, or caller-selected image renditions.
- Whether captions/tags are per provider/profile, per active semantic model, or globally active.
- When to rename C++ classes and the sidecar executable from semantic-oriented names to AI-sidecar
  names.

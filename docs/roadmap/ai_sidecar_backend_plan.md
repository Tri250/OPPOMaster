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

## Provider Driver And Config Strategy

Large-model providers do not share a stable request JSON shape. A pure "JSON template per provider"
system would look flexible, but it would push protocol semantics, credential handling, error mapping,
retry policy, and response validation into data files where they are hard to test. The safer pattern
used by LLM gateway projects is:

1. Keep Alcedo task schemas stable and code-owned.
2. Implement a small set of provider drivers for real protocol families.
3. Let provider config files describe endpoints, model defaults, capability flags, schema-injection
   mode, auth slot, limits, and response extraction.

Provider config files are therefore deployment/configuration data, not executable adapters. They
should not contain JavaScript, shell commands, arbitrary eval expressions, or raw API keys.

Initial driver families:

- `openrouter_chat`: OpenRouter's OpenAI Chat Completions-compatible endpoint, with OpenRouter
  routing preferences and metadata handling.
- `volcengine_ark_responses`: Volcengine Ark / 火山方舟 Responses API, used by the built-in
  Doubao multimodal provider config.
- `volcengine_ark_chat`: Volcengine Ark / 火山方舟 Chat API, kept as a fallback/compatibility
  driver if a model or deployment is easier to call through the OpenAI-compatible chat surface.
- `openai_responses`: direct OpenAI Responses API, added after OpenRouter if needed for
  provider-specific capabilities.
- `openai_chat_compatible`: generic OpenAI-compatible chat-completions servers.
- `anthropic_messages`: Anthropic Messages API.
- `gemini_generate_content`: Google Gemini `generateContent`.
- `generic_json_http`: optional experimental fallback for advanced users. It may only use HTTPS
  (except localhost dev), static JSON object templates, a small allowlist of variable substitutions,
  and JSON Pointer response extraction. It must still pass Alcedo schema validation before returning
  results.

Provider config file shape:

```json
{
  "schema_version": 1,
  "provider_id": "openrouter",
  "display_name": "OpenRouter",
  "driver": "openrouter_chat",
  "base_url": "https://openrouter.ai/api/v1",
  "endpoint": "/chat/completions",
  "auth": {
    "type": "bearer",
    "credential_slot": "openrouter_api_key"
  },
  "attribution_headers": {
    "HTTP-Referer": "https://alcedo.studio",
    "X-OpenRouter-Title": "Alcedo Studio"
  },
  "defaults": {
    "model": "qwen/qwen3.7-plus",
    "stream": false,
    "temperature": 0.2
  },
  "structured_output": {
    "mode": "response_format_json_schema",
    "strict": true,
    "provider_require_parameters": true
  },
  "response": {
    "content_json_pointer": "/choices/0/message/content",
    "usage_json_pointer": "/usage",
    "provider_request_id_json_pointer": "/id",
    "provider_request_id_header": null
  },
  "limits": {
    "timeout_ms": 60000,
    "max_image_bytes": 4194304,
    "max_output_tokens": 1200
  }
}
```

Phase 5 should use JSON config first because `rust/puerh_mind` already depends on `serde_json`.
Built-in provider configs can be embedded into the sidecar binary with `include_str!` to avoid
packaging drift. User-added provider configs should live in a user config directory and be loaded
after built-ins, with validation errors surfaced in settings. User configs may override model lists
and endpoints, but they may not override secret storage policy or bypass schema validation.

The Alcedo task schema remains code-owned:

- `image_understanding.describe` always returns Alcedo's caption/tags/scene/confidence shape.
- `image_rating.score` always returns Alcedo's rating/rubric shape.
- Provider configs only describe how that schema is requested from a provider and where to extract
  the provider's response.

Second built-in provider config example:

```json
{
  "schema_version": 1,
  "provider_id": "volcengine_ark",
  "display_name": "Volcengine Ark / 火山方舟",
  "driver": "volcengine_ark_responses",
  "base_url": "https://ark.cn-beijing.volces.com/api/v3",
  "endpoint": "/responses",
  "auth": {
    "type": "bearer",
    "credential_slot": "volcengine_ark_api_key"
  },
  "defaults": {
    "model": "doubao-seed-2-0-lite-260428",
    "stream": false,
    "temperature": 0.2
  },
  "structured_output": {
    "mode": "responses_json_schema",
    "strict": true
  },
  "response": {
    "content_json_pointer": null,
    "usage_json_pointer": "/usage",
    "provider_request_id_json_pointer": "/id",
    "provider_request_id_header": null
  },
  "limits": {
    "timeout_ms": 60000,
    "max_image_bytes": 4194304,
    "max_output_tokens": 1200
  }
}
```

## OpenRouter Implementation Strategy

OpenRouter should be the first remote provider because it gives one OpenAI-compatible endpoint for
many model vendors while still supporting structured outputs for compatible models. The bundled
OpenRouter default is Qwen3.7 Plus: show it to users as `qwen3.7-plus`, but send the canonical
OpenRouter model slug `qwen/qwen3.7-plus` on the wire.

OpenRouter driver behavior:

- Use `POST https://openrouter.ai/api/v1/chat/completions`.
- Authenticate with `Authorization: Bearer <runtime secret>`, where the secret comes from the
  runtime credential vault by `credential_ref`; never from config files.
- Send `Content-Type: application/json`.
- Optionally send `HTTP-Referer` and `X-OpenRouter-Title` attribution headers from config.
- Build OpenAI Chat-compatible `messages` with an Alcedo-owned system prompt and user content that
  contains the selected image rendition plus task instructions.
- Keep the request body compatible with OpenRouter's official Go SDK chat-completion shape. Rust
  still sends direct HTTPS in Phase 5, but the request/response fixtures should be reusable by an
  OpenRouter Go client without changing model slug, structured-output fields, provider routing, or
  attribution-header semantics.
- Inject Alcedo's JSON Schema through `response_format: { "type": "json_schema", ... }` and set
  strict mode when the selected model supports it.
- Include `provider: { "require_parameters": true }` when structured output is required, so
  OpenRouter does not route to a provider that silently ignores `response_format`.
- Keep `stream: false` in Phase 5.
- Parse `choices[0].message.content` as JSON, validate it against the Alcedo task schema, normalize
  strings/tags/scores, and return typed protobuf fields.
- Capture `usage` and provider request id when available; expose them as metadata for UI/cost
  summaries and diagnostics.
- If schema validation fails, return a typed provider/schema error and do not persist an active
  annotation. A future retry may use response healing, but Phase 5 should not silently repair and
  persist ambiguous results.

OpenRouter config should ship with a small curated model list instead of defaulting to arbitrary
router aliases. Phase 5 starts with `qwen/qwen3.7-plus` as the bundled default, because it is the
canonical OpenRouter slug for the user-facing `qwen3.7-plus` model. Each bundled model entry should
declare:

- model slug
- whether vision input is supported
- whether JSON Schema structured output is supported
- max image bytes / recommended image rendition
- approximate cost metadata if available
- whether to request data-collection restrictions such as `provider.data_collection = "deny"` when
  the user enables a privacy-first mode

## Volcengine Ark Implementation Strategy

Volcengine Ark / 火山方舟 should ship as the second built-in remote provider. The default model is
`doubao-seed-2-0-lite-260428`, matching the current multimodal/Responses API plan and keeping a
China-friendly provider available without requiring users to configure an arbitrary custom endpoint.

Volcengine driver behavior:

- Use the Ark data-plane base URL `https://ark.cn-beijing.volces.com/api/v3`.
- Prefer the Responses API driver (`volcengine_ark_responses`) for multimodal understanding because
  the linked 火山方舟 docs place multimodal understanding under Responses API. Keep
  `volcengine_ark_chat` available as a compatibility driver for deployments or models that are
  easier to call through Chat API.
- Authenticate with `Authorization: Bearer <runtime secret>`, where the secret comes from the
  runtime credential vault by `credential_ref`; never from config files.
- Send `Content-Type: application/json`.
- Build a Responses API request from Alcedo-owned task schema, selected image rendition, and prompt
  profile. The driver, not the provider config, owns the exact Responses API field mapping.
- Inject Alcedo's JSON Schema through the Ark structured-output mechanism when supported. If a
  selected model does not support structured output, fail closed for Phase 5 rather than relying on
  best-effort free-form JSON.
- Keep `stream: false` in Phase 5.
- Parse the provider response content as JSON, validate it against the Alcedo task schema, normalize
  values, and return typed protobuf fields.
- Capture response id, model id, usage metadata, and provider error codes when available.
- Map Ark/transport errors into `AiResponseStatus` / `AiErrorCode`, with redacted messages.

Bundled Volcengine config should declare `doubao-seed-2-0-lite-260428` as the default model and mark
it as supporting text generation, multimodal understanding, and structured output only after a live
provider smoke confirms the exact request shape. `content_json_pointer: null` means the
`volcengine_ark_responses` driver owns response-content extraction with a typed parser; it should
only become a static JSON Pointer if the live response shape is stable enough to make that safer than
driver-owned parsing.

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

Goal: add the first non-CLIP remote AI task over Rust HTTPS, with OpenRouter and Volcengine Ark /
火山方舟 as the two built-in remote providers. Image understanding and image rating are separate task
contracts even if a provider request can return both.

### Phase 5a - Provider Config Loader And Registry

Goal: make provider selection data-driven without turning provider JSON files into executable code.

Deliverables:

- Add a Rust `provider_config` module that loads built-in JSON provider configs and optional
  user-provider configs from a configured directory.
- Embed built-in configs with `include_str!` for Phase 5 so Windows packaging cannot omit them.
- Add a validated `ProviderConfig` schema with fields for `provider_id`, `driver`, `base_url`,
  `endpoint`, auth `credential_slot`, attribution headers, structured-output mode, response
  extraction pointers or driver-owned parser mode, model capabilities, and limits.
- Add config validation: HTTPS-only except localhost dev, no raw secrets, known driver id, known
  schema version, allowed header names, bounded timeout/payload limits, and JSON Pointer syntax
  checks.
- Add capability descriptors from loaded provider configs so C++ can display remote-provider
  availability before a task starts.
- Add user-config precedence rules: user configs can add providers or override model defaults, but
  cannot override credential policy, disable schema validation, or enable arbitrary code execution.

Tests:

- Rust config-loader tests for built-in load, user override, duplicate provider id, unknown driver,
  invalid HTTPS policy, raw-secret rejection, invalid JSON Pointer, and schema-version mismatch.
- Capability-registry tests showing OpenRouter and Volcengine Ark model capabilities become
  `image_understanding.describe` / `image_rating.score` descriptors.

Review focus:

- Check that provider configs are data only: no eval, no scripts, no shell, no secrets.
- Check that invalid user configs fail closed and produce actionable diagnostics.

### Phase 5b - Image Analysis Protobuf And Alcedo Task Schemas

Goal: freeze Alcedo's provider-independent result contracts before writing HTTP provider code.

Deliverables:

- Add `image_analysis.proto` (or equivalent) with typed request/response messages for:
  - `image_understanding.describe`
  - `image_rating.score`
- Include `AiRequestHeader` / `AiResponseHeader` in every request/response.
- Define code-owned JSON Schemas for the two task outputs. Provider configs select injection mode;
  they do not define business fields.
- Include provider/model/prompt profile identity, selected rendition metadata, usage metadata, and
  provider request id in the response.
- Add a mock Rust provider that returns valid typed results without HTTP.

Tests:

- Rust proto/service tests for valid understanding, valid rating, missing credential, timeout, and
  schema-validation failure.
- C++ generated-proto build coverage after adding the new proto.

Review focus:

- Check that rating and understanding cannot overwrite each other because they carry distinct
  `task_id`s and result identities.
- Check that provider-specific raw JSON is not exposed as the public task contract.

### Phase 5c - OpenRouter And Volcengine Ark Drivers

Goal: implement the first real remote providers through OpenRouter's Chat Completions-compatible API
and Volcengine Ark's Responses API.

Deliverables:

- Add `reqwest` with explicit Rustls TLS features and no plaintext/invalid-cert production mode.
- Add `OpenRouterChatProvider` behind the `openrouter_chat` driver id.
- Build `POST /api/v1/chat/completions` requests from the loaded OpenRouter config.
- Keep OpenRouter request and response fixtures compatible with OpenRouter Go SDK chat-completion
  types, even though the production Rust implementation uses `reqwest`.
- Add `VolcengineArkResponsesProvider` behind the `volcengine_ark_responses` driver id, with the
  bundled config defaulting to `doubao-seed-2-0-lite-260428`.
- Build `POST /responses` requests from the loaded Volcengine config and Ark data-plane base URL.
- Keep `volcengine_ark_chat` as a reserved compatibility driver unless live provider testing proves
  the default Doubao path needs Chat API instead of Responses API.
- Resolve the OpenRouter or Volcengine API key from `credential_ref` through the Rust credential
  vault and send it only as an `Authorization: Bearer ...` header.
- Send optional `HTTP-Referer` and `X-OpenRouter-Title` attribution headers from config.
- Send non-streaming requests with `response_format.type = "json_schema"` and strict JSON Schema
  when the selected model supports it.
- Send `provider.require_parameters = true` whenever structured output is required.
- Support optional privacy routing knobs from config/user settings, such as
  `provider.data_collection = "deny"` or `provider.zdr = true` when available.
- For Volcengine, construct the Responses API request from the Alcedo task schema, selected image
  rendition, and prompt profile; the typed driver owns the exact request-field mapping.
- Extract `choices[0].message.content`, parse JSON, validate against the Alcedo task schema,
  normalize values, and return typed protobuf fields for OpenRouter.
- Extract Ark Responses output content with a typed parser, parse JSON, validate against the Alcedo
  task schema, normalize values, and return typed protobuf fields for Volcengine.
- Capture response `id`, `model`, `usage`, and any available request-id header for diagnostics and
  usage/cost display.
- Map OpenRouter, Ark, and transport errors into `AiResponseStatus` / `AiErrorCode`, with redacted
  messages.

Tests:

- Rust HTTP tests with a local mock server for auth header placement, attribution headers,
  structured-output request body, `provider.require_parameters`, response parsing, usage capture,
  rate-limit mapping, 5xx retry policy, cancellation, and timeout.
- Rust HTTP tests with a local mock server for Volcengine Ark auth header placement, Responses API
  request body, structured-output request body, typed output extraction, usage capture, provider
  error-code mapping, cancellation, and timeout.
- Negative tests proving API keys, image payloads/base64, prompts, and raw provider bodies are not
  emitted in routine logs or error strings.
- Manual OpenRouter smoke behind an environment-gated test that is skipped without credentials.
- Manual Volcengine Ark smoke behind an environment-gated test that is skipped unless
  `ALCEDO_VOLCENGINE_ARK_API_KEY` or `ALCEDO_ARK_API_KEY` is set.

Review focus:

- Check that OpenRouter and Volcengine compatibility are implemented by drivers, not by arbitrary
  JSON templates.
- Check that a provider schema failure cannot create an active annotation.
- Check that the Volcengine response parser is backed by a live smoke fixture before Phase 5 is
  marked complete.
- Check that retries are bounded and do not retry non-idempotent or paid calls too aggressively.

### Phase 5d - C++ Runtime Client And Host Image Analysis Service

Goal: expose remote image analysis to the host while keeping C++ ownership of image rendition,
project state, and persistence.

Deliverables:

- Extend `IAiSidecarRuntimeClient` / `GrpcAiSidecarRuntimeClient` with typed image-analysis RPCs.
- Add a C++ `ImageAnalysisService` (or narrow `ImageUnderstandingService` plus rating companion)
  that prepares thumbnails/previews via existing host services, registers credentials with the
  sidecar, calls the typed RPC, and returns structured DTOs.
- Keep all database writes in C++; the sidecar returns results only.
- Add cancellation propagation from C++ job id/request id to `AiRuntimeService.CancelTask`.

Tests:

- C++ fake-sidecar tests for success, missing credential, invalid provider config, timeout,
  cancellation, and schema-error propagation.
- Tests proving raw API key material never enters `AiSidecarRuntimeOptions`, process args, QSettings,
  project files, or captured logs.

Review focus:

- Check that the host controls which image rendition is sent and records that rendition in result
  metadata.
- Check that sidecar startup remains on demand and normal browsing/search do not require API keys.

### Phase 5e - Storage And Search Integration

Goal: persist remote analysis results without mixing searchable understanding with subjective rating.

Deliverables:

- Add storage for AI image annotations with file id, task id, provider id, model id, prompt/profile
  id, selected rendition, caption, tags, scene/category hints, confidence, created time, and
  active-for-search state.
- Add rating storage with file id, task id, provider id, model id, prompt/profile id, score fields,
  rubric id/version, reasons, created time, and active-for-rating state.
- Enforce at most one active understanding result per `(file_id, task_id)` for search.
- Keep rating out of full-text search by default; expose it later as sort/filter/recommendation
  data only when a product rubric is approved.
- Extend search document construction so active captions/tags can participate in full-text search.
- Extend delete cleanup so deleting files removes both understanding and rating rows.

Tests:

- Storage controller tests for insert, replace, active selection, provider/model/prompt identity,
  delete cleanup, and rating-vs-understanding isolation.
- `FilterServiceTest` or equivalent coverage showing captions/tags are searchable only after
  successful active persistence, while rating scores do not enter full-text search.

Review focus:

- Check that failed remote calls do not create partial active search documents.
- Check that prompt/profile/rubric changes do not reinterpret old scores as new scores.

### Phase 5f - Developer Smoke And Handoff

Goal: prove the MVP path end to end before product UI wiring in Phase 6.

Deliverables:

- Add CLI/dev smoke paths or environment-gated tests that run one real OpenRouter request and one
  real Volcengine Ark request against a small fixture image when the matching API key env var is set.
- Record required environment variables, skipped-test behavior, and expected output shape.
- Write the Phase 5 self-review conclusion in the required plan format.

Tests:

- Full Rust focused tests for provider config, OpenRouter driver, Volcengine Ark driver, schema
  validation, and mock provider.
- Targeted C++ tests for runtime client, host service, storage, and search.
- Environment-gated real OpenRouter and Volcengine Ark smokes, skipped by default.

Review focus:

- Check that no raw API key appears in diagnostics, logs, screenshots, settings, process args, or
  packed projects.
- Check that OpenRouter and Volcengine model/provider metadata and usage are captured enough for
  Phase 6 UI.

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
- Whether to prefer OpenRouter privacy-first routing knobs by default. OpenRouter's bundled default
  is Qwen3.7 Plus (`qwen/qwen3.7-plus` on the wire; `qwen3.7-plus` in UI copy). Volcengine Ark's
  bundled default is `doubao-seed-2-0-lite-260428`.
- Exact live-verified Volcengine Responses output extraction shape and whether the reserved Chat API
  compatibility driver is needed for any target deployment.
- Exact rating rubric: whether score means aesthetic quality, technical quality, keeper priority, or
  a weighted combination.
- Whether the first UI exposes rating separately or runs a combined understanding+rating job that
  stores separate task results.
- Exact user-provider config directory and whether C++ passes it to Rust or Rust resolves it from an
  app-specific config path.
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
- OpenRouter docs (`https://openrouter.ai/docs/quickstart`,
  `https://openrouter.ai/docs/guides/features/structured-outputs`): the direct API uses
  `POST /api/v1/chat/completions` with Bearer authentication, OpenAI-compatible request/response
  shapes, optional attribution headers, structured outputs via `response_format: json_schema`, and
  provider routing options such as `require_parameters`.
- OpenRouter model docs (`https://openrouter.ai/qwen/qwen3.7-plus`): Qwen3.7 Plus uses the canonical
  model slug `qwen/qwen3.7-plus`, supports text and image input with text output, and is therefore
  the bundled OpenRouter default for Phase 5 image understanding.
- OpenRouter Go SDK docs (`https://openrouter.ai/docs/client-sdks/go/overview`,
  `https://openrouter.ai/docs/sdks/go/api-reference/chat`): the Go SDK is a type-safe client over
  OpenRouter's chat-completion API, so Phase 5 request/response fixtures should stay compatible with
  the SDK's chat send shape even though Rust calls the REST API directly.
- OpenRouter docs: usage metadata is available on non-streaming responses, making it appropriate for
  Phase 6 job-level usage/cost summaries.
- Volcengine Ark docs (`https://www.volcengine.com/docs/82379/1958521?lang=zh`): the linked
  multimodal-understanding guide is under the Responses API path, supporting
  `volcengine_ark_responses` as the preferred built-in driver for Doubao multimodal calls.
- Volcengine Ark model-list docs (`https://www.volcengine.com/docs/82379/1330310`):
  `doubao-seed-2-0-lite-260428` is a current Doubao Seed 2.0 Lite model with text generation,
  multimodal understanding, and tool-call capabilities, making it the built-in default for the
  China-friendly provider path.
- BytePlus/ModelArk docs (`https://docs.byteplus.com/en/docs/ModelArk/Responses_API`,
  `https://docs.byteplus.com/en/docs/ModelArk/1958523`): Responses API and Structured output
  (Responses API) are documented as first-class APIs, so the Volcengine driver should use
  schema-based output when the selected model supports it rather than best-effort prompt-only JSON.
- LiteLLM docs: mature LLM gateway systems use configuration for model aliases, `api_base`,
  provider-specific params, routing, and secret-manager references while still routing through
  provider-aware code paths. This supports Alcedo's driver-plus-config design instead of a pure
  arbitrary-template design.
- Rust `reqwest` docs: async HTTP client with JSON support, reusable clients, timeout/TLS
  configuration, and explicit TLS backend selection.
- QtKeychain docs: platform-independent Qt secret storage backed by Windows Credential Store, macOS
  Keychain, and Linux desktop keyrings.
- Rust `secrecy` / `zeroize` docs: explicit secret exposure and zeroing help prevent accidental
  logging and reduce in-memory lifetime risk; the current custom `SecretString` can stay if it keeps
  the same audit properties.

## Phase 4 - Completion & Self-Review

Status: complete. Semantic embedding now runs over the shared AI control surface
(`alcedo.ai.AiRequestHeader` / `AiResponseHeader`) via additive v2 RPCs, with v1 kept as an automatic
fallback. v1 RPCs, batching, request-id-to-file-id mapping, model-info validation, embedding
dimensions, model keys, and persistence are all unchanged (Phase 0 contract section 2.3 honored: v2 is
added as new methods, v1 is frozen).

Implemented (file-by-file, per the plan):

- `rust/puerh_mind/proto/semantic.proto` - `import "ai_common.proto";`, 4 v2 RPCs on `SemanticService`
  (`EmbedTextV2` / `EmbedImageV2` / `EmbedTextBatchV2` / `EmbedImageBatchV2`), and 9 v2 messages that
  reference `alcedo.ai.*` fully-qualified. v1 is untouched.
- `rust/puerh_mind/src/server/semantic.rs` - a `build_response_header` helper plus 4 v2 trait methods
  that delegate to the frozen v1 methods and wrap the result with an `AiResponseHeader` (single ->
  `header.model_id`; batch -> per-item `request_id` preserved). v1 methods and v1 tests are unedited. 5
  new v2 tests were added. The cross-package `semantic` -> `alcedo.ai` prost reference resolves with no
  `build.rs` / `proto.rs` change (the existing sibling-module layout emits `super::alcedo::ai::*`).
- `alcedo_studio/src/CMakeLists.txt` - the SemanticProto custom-command `DEPENDS` now includes
  `ai_common.proto`; the SemanticProto `add_library` is ordered after AiProto;
  `target_include_directories` adds the AI generated dir; `target_link_libraries` adds `AiProto`. The
  DAG stays clean (AiProto does not depend on SemanticProto).
- `alcedo_studio/src/include/app/ai_sidecar_runtime_service.hpp` - 4 v2 virtuals on
  `IAiSidecarRuntimeClient` (with out-of-line default impls) and 4 `override` declarations on
  `GrpcAiSidecarRuntimeClient`.
- `alcedo_studio/src/app/ai_sidecar_runtime_service.cpp` - `FillAiRequestHeader` gained a
  backward-compatible `trace_id` parameter (the 3 existing AI-runtime call sites are unchanged);
  `MakeBatchRequestId()`; 2 v2 `ToEmbeddingResult` overloads (single reads `header().request_id()`,
  batch item keeps the per-item `request_id`); 4 `GrpcAiSidecarRuntimeClient::Embed*V2` overrides; and 4
  service wrappers that try-v2-then-fallback. `SemanticEmbeddingResult` is unchanged, so storage and
  search are unchanged.
- `alcedo_studio/tests/app/ai_sidecar_runtime_service_test.cpp` - `FakeAiSidecarRuntimeClient` was
  extended with `v2_supported_` / `SetV2Supported`, 4 v2 overrides canned bit-identical to v1, and call
  counters; 5 new tests were added. Existing tests are unedited.

Fallback policy: only `grpc::UNIMPLEMENTED` triggers v1 fallback (`*v2_available = false`, the service
then calls v1). All other grpc codes (including `DEADLINE_EXCEEDED`) keep v2 (`*v2_available = true`)
with synthesized per-input failures and no v1 retry - the server has v2 but the call failed. Single-call
v2 correlation is `header.request_id`; batch correlation is a fresh `MakeBatchRequestId()` with the
per-item `request_id` preserved end to end.

Credential handling (Phase 3 invariant preserved): Phase 4 embedding has no credentials, so
`FillAiRequestHeader` receives an empty `credential_ref`. Secrets still travel only over gRPC loopback
to the Rust vault, never through process args, `AiSidecarRuntimeOptions`, or logs; `FillAiRequestHeader`
never echoes a secret. v2 calls set `trace_id = request_id` (local correlation; no distributed trace
exists yet). `priority` stays at default and `client_capabilities` is left empty (no `cancel-by-request_id`
advertised).

Deferred to Phase 5 (per Phase 0 section 1.5 and the plan design):

- Cancellation wiring for embedding - not a Phase 4 deliverable; the old C++ embedding has no gRPC-level
  cancel (job-level only, unchanged). `supports_cancel: true` remains a forward promise with no regression.
- Full status-to-tonic structured-error-in-body mapping - v2 hard failures still propagate the v1
  `tonic::Status` (`?`); the `AiResponseHeader` travels on success only, matching the existing
  `ok_header` success-only pattern.

Test results:

- Rust - `cargo test` in `rust/puerh_mind`: 88 passed; 0 failed; 0 ignored. Includes the 5 v2 tests
  (`text_batch_v2_preserves_request_order_and_item_errors`,
  `image_batch_v2_preserves_request_order_and_item_errors`,
  `rejects_non_finite_batch_embedding_as_item_error_v2`,
  `embed_image_v2_routes_through_micro_batch_worker`, `embed_text_v2_echoes_header_request_id`).
- C++ MSVC build (run through the PowerShell tool, per project memory): succeeded after regenerating
  `semantic.pb.h` / `ai_common.pb.h`. This resolved all prior clangd "No type named EmbeddingResponseV2"
  and "redefinition" diagnostics, which were stale generated headers, not real errors.
- ctest targeted group (the validation regex in this plan): 87 tests; 100% passed; 0 failed (1 live
  smoke Skipped - environment-gated, `ALCEDO_SEMANTIC_LIVE_RUNTIME_PATH` was not set). The 5 new
  `AiSidecarRuntimeServiceTest` tests pass: `EmbedTextV2ReturnsCannedViaV2Path`,
  `EmbedTextV2FallsBackToV1WhenV2Unsupported`, `EmbedImageBatchV2ReturnsCannedViaV2Path`,
  `EmbedImageBatchFallsBackToV1WhenV2Unsupported`, `EmbedImageBatchV2EchoesRequestIds`. The 15 existing
  `AiSidecarRuntimeServiceTest` tests still pass - now exercising the v2 path (the fake v2 overrides
  with `v2_supported_ = true`), which proves v2 canned is bit-identical to v1.
  `SemanticGenerationServiceTest` / `SemanticStorageControllerTest` / `FilterServiceTest` /
  `SearchQueryClassifierTest` / `GlobalSearchDialogQmlTest` are unchanged and green (they use
  `ISemanticImageEmbeddingClient` fakes / storage / search, not the v2 gRPC path).

Build note for the next handoff: the default `all` MSVC build (`--build --preset win_debug`) did not
rebuild `AiSidecarRuntimeServiceTest` after a test-source-only edit - ninja treated the target as
up-to-date, and deleting the executable confirmed `all` does not own it. After editing test sources,
build the target explicitly before ctest:
`cmd /c scripts\msvc_env.cmd --build --preset win_debug --target AiSidecarRuntimeServiceTest --parallel 4`
(or the affected test targets). The plan's verification step 2 should be augmented with this explicit
test-target build.

Review conclusion: none; risk accepted: v2 hard-failure responses propagate the v1 `tonic::Status` (the
`AiResponseHeader` travels on success only) and embedding cancellation is not wired into the
micro-batch worker - both deferred to Phase 5 per Phase 0 section 1.5, and `supports_cancel` stays a
forward promise with no regression; missing tests: none.

## Phase 5a - Completion & Self-Review

Status: complete. Provider selection is data-driven via validated JSON provider configs (built-in
plus an optional user-config directory), with built-ins embedded through `include_str!` so Windows
packaging cannot omit them. Provider configs are DATA ONLY: the validator scans the raw JSON for
secrets before deserializing, rejects raw secret values (`sk-...`, `Bearer ...`, `AKIA...`) and
secret-named keys, enforces HTTPS-only (except localhost dev), known driver / schema version, valid
JSON Pointers, bounded timeout / payload / output-token limits, `stream = false`, and reserved
attribution-header rejection. Invalid user configs fail closed (skipped with a warning, never
offered); an invalid built-in is a hard error. Capability descriptors are derived from loaded configs
so C++ can display remote-provider availability before a task starts. No HTTP is issued in 5a - only
config load, validation, and descriptor advertisement.

Implemented (file-by-file, per the plan):

- `rust/puerh_mind/configs/providers/openrouter.json` - built-in OpenRouter config: `openrouter_chat`
  driver, `https://openrouter.ai/api/v1` + `/chat/completions`, bearer auth slot `openrouter_api_key`,
  `HTTP-Referer` / `X-OpenRouter-Title` attribution headers, `response_format_json_schema` strict,
  JSON Pointer extraction (`/choices/0/message/content`, `/usage`, `/id`), qwen vision+structured
  model, `data_collection = "deny"`.
- `rust/puerh_mind/configs/providers/volcengine_ark.json` - built-in Volcengine Ark config:
  `volcengine_ark_responses` driver, `https://ark.cn-beijing.volces.com/api/v3` + `/responses`, bearer
  auth slot, `responses_json_schema` mode, driver-owned parser (`content_json_pointer = null`),
  doubao-seed-2-0-lite-260428 vision+structured model.
- `rust/puerh_mind/src/service/provider_config.rs` - `ProviderConfig` schema + validation,
  `ProviderRegistry` (upsert-by-provider_id / get / iter), `BUILTIN_PROVIDER_CONFIGS` via
  `include_str!`, `load_provider_configs(user_dir)` (built-ins hard-error, user configs skip+warn, user
  overrides built-in by provider_id, user-on-user duplicates skipped), `scan_for_secrets` (runs before
  deserialize; rejects secret-named keys and leaked-looking values; a `credential_slot` VALUE like
  `openrouter_api_key` is not treated as a secret because it is a value, not a key, and the slot regex
  `^[a-z0-9_]+$` cannot carry a real key), `build_provider_capability_descriptors` (emits one
  `image_understanding.describe` + one `image_rating.score` descriptor per model with
  `supports_vision && supports_structured_output`; `requires_credential` from auth type). 18 module
  tests.
- `rust/puerh_mind/src/service/capabilities.rs` - rewritten
  `build_capability_descriptors(engine, max_payload_bytes, registry, extra)` returns local-semantic +
  config-derived + extra. The old placeholder `provider_id = "remote"` / `model_id = "unconfigured"`
  descriptor is removed. 3 tests.
- `rust/puerh_mind/src/service/mod.rs` - `pub mod provider_config;` (and `pub mod image_analysis;`
  for 5b).
- `rust/puerh_mind/src/config.rs` - `provider_config_dir: Option<String>` field,
  `--provider-config-dir` CLI flag, `ALCEDO_MIND_PROVIDER_CONFIG_DIR` env. 2 tests.
- `rust/puerh_mind/src/main.rs` - loads the provider registry, constructs the mock image-analysis
  provider + its capability (5b), and passes the registry + extra capability + image providers through
  to `start_server`.
- `rust/puerh_mind/src/server/ai_runtime.rs` - `test_impl` loads the registry; two `ai_runtime` tests
  updated to assert config-derived descriptors (count >= 5, an openrouter understanding descriptor with
  `requires_credential` and `model_id != "unconfigured"`).

Data-only / fail-closed invariants (Phase 5a review focus):

- `scan_for_secrets` runs before deserialize on both built-in and user configs, so a secret-shaped
  value or secret-named key is rejected even if the surrounding JSON would otherwise parse and even
  if the field is unknown to `ProviderConfig` (serde silently drops unknown fields). The
  `credential_slot` value is intentionally not treated as a secret: it names a vault slot, never
  holds key material, and the slot regex `^[a-z0-9_]+$` cannot carry a real key (`sk-`, `Bearer `,
  `AKIA...` all fail it).
- Invalid user configs fail closed (skip + warn, not offered) and produce an actionable `ConfigError`
  diagnostic naming the origin file and reason; an invalid built-in is a hard error (the binary is
  broken).
- User configs can add providers or override model defaults, but cannot override credential policy,
  disable schema validation, or enable arbitrary code execution: those are not user-overridable fields,
  and validation always runs.

Deferred to Phase 5c:

- The real HTTP drivers (`OpenRouterChatProvider`, `VolcengineArkResponsesProvider`) - 5a only loads,
  validates, and advertises; no HTTPS call is made.
- Secret persistence across sidecar restarts - the 5a vault is in-memory only (Phase 3 invariant).

Test results:

- Rust - `cargo test` in `rust/puerh_mind`: 129 passed; 0 failed; 0 ignored; 0 warnings. The 18
  `provider_config` tests cover built-in load, user override of a built-in model default, user adds a
  new provider, duplicate user provider id rejected, unknown driver rejected, invalid HTTPS policy
  rejected, http-localhost allowed for dev, raw secret in an `api_key`-named field rejected, leaked
  `Bearer` value rejected, `credential_slot` value not treated as a secret, invalid JSON Pointer
  rejected, invalid JSON Pointer escape rejected, schema-version mismatch rejected, `stream = true`
  rejected, reserved attribution header rejected, out-of-range timeout rejected, built-ins advertise
  understanding + rating descriptors, non-vision / non-structured models not advertised. The 3
  `capabilities` tests + 2 `ai_runtime` tests cover config-derived remote descriptors appended, the
  local semantic descriptor, extra local providers appended, and `ListCapabilities` with / without a
  request header. The 2 `config` tests cover the `--provider-config-dir` override and the None default.
- C++ - no C++ surface changed in 5a (AiProto / CMake are untouched in 5a; `image_analysis.proto` CMake
  generation is 5b). No ctest required for 5a.

Review conclusion: none at phase close; superseded by the 2026-06-25 follow-up (two bugs found and
fixed, see "Phase 5a - Follow-up Review & Fixes" below); risk accepted: the openrouter and volcengine
capability descriptors advertised by 5a are not backed by a registered provider until the 5c drivers
land, so a `DescribeImage` / `ScoreImage` for those provider_ids returns `UNSUPPORTED_TASK` /
`TASK_UNKNOWN` in 5a/5b (only the mock is registered) - this is the intended phase boundary, not a
regression; missing tests: none after the follow-up added 3 regression tests.

### Phase 5a - Follow-up Review & Fixes (2026-06-25)

A follow-up review of the 5a surface found two correctness gaps in `provider_config.rs`; both are
fixed with regression tests.

- P1 (raw-secret scan not applied to user configs): `load_user_configs` deserialized each user config
  with `serde_json::from_value::<ProviderConfig>` without first calling `scan_for_secrets`. Because
  `ProviderConfig` has no `#[serde(deny_unknown_fields)]`, serde silently drops unknown fields, so a
  user JSON carrying `"api_key": "sk-..."` or `"note": "Bearer ..."` was silently accepted -
  contradicting the 5a "raw JSON before deserialize" / "configs are data only" guarantee, which only
  the built-in `parse_and_validate` path actually enforced. Fixed: `load_user_configs` now scans the
  parsed `Value` before `from_value`, skipping + warning on a secret hit (fail closed, not a hard
  error, matching the existing user-config failure policy).
- P2 (user-on-user duplicate of a built-in override): the duplicate guard was
  `registry.get(&id).is_some() && !is_builtin(&id)`. `is_builtin` checks the static built-in id list,
  so for a built-in id like `openrouter` the second clause is always false, and a second user file
  overriding `openrouter` silently clobbered the first (the guard only caught user-on-user dupes for
  non-builtin ids). The existing `duplicate_user_provider_id_is_rejected` test passed only because it
  used the non-builtin id `"dupe"`. Fixed: `load_user_configs` now tracks a `seen_user_provider_ids`
  `HashSet`; the first user config wins and any later user config sharing the id is skipped + warned,
  regardless of whether the id is also a built-in. The now-dead `is_builtin` helper was removed.

Regression tests added (`provider_config`): `user_config_with_raw_secret_in_unknown_field_is_skipped`,
`user_config_with_leaked_bearer_in_unknown_field_is_skipped`,
`duplicate_user_override_of_builtin_does_not_silently_clobber`. `cargo test` in `rust/puerh_mind`:
133 passed; 0 failed; 0 warnings (was 129; +3 here, +1 in 5b). The `is_builtin` removal introduced no
dead-code warning (its only call site was the old guard).

## Phase 5b - Completion & Self-Review

Status: complete. Alcedo's provider-independent result contracts for `image_understanding.describe`
and `image_rating.score` are frozen as typed proto messages plus code-owned JSON Schemas. The two
tasks are distinct contracts - distinct `task_id`s, distinct result message types
(`ImageUnderstandingResult` vs `ImageRatingResult`), distinct RPCs (`DescribeImage` vs `ScoreImage`) -
so a rating result can never overwrite or be reinterpreted as an understanding result (Phase 5b review
focus). Provider-specific raw JSON is never the public contract: the (5c) driver validates and
normalizes provider output against the code-owned schemas and returns these typed fields. The mock
provider returns valid typed results without HTTP. The service owns the control-plane concerns the
provider should not - credential resolution against the vault, request timeout, cooperative
cancellation via the `CancellationRegistry`, and schema validation of the provider's typed result - and
carries outcomes inside the `AiResponseHeader` (status / error_code / redacted error_message). This is
the Phase 5 structured-error-in-body mapping deferred from Phase 4: image analysis has no legacy v1
caller, so the RPC returns `Ok(Response{ header, ... })` with the header carrying the precise status
rather than a plain `tonic::Status`. A genuinely malformed request (empty `image_bytes`) is still a
transport-level `tonic::Status::invalid_argument`, since there is no provider outcome to report.

Implemented (file-by-file, per the plan):

- `rust/puerh_mind/proto/image_analysis.proto` - package `alcedo.ai` (Phase 0 single-package decision),
  `import "ai_common.proto";`, `ImageAnalysisService { DescribeImage, ScoreImage }`; messages
  `RenditionMetadata`, `UsageMetadata`, `ImageUnderstandingResult { caption, tags, scene, confidence }`,
  `ScoredDimension { name, score }`, `ImageRatingResult { scores, rubric_id, rubric_version, reasons,
  confidence }`, `DescribeImageRequest` / `Response`, `ScoreImageRequest` / `Response`. Every
  request / response carries `AiRequestHeader` / `AiResponseHeader`; the response echoes the selected
  rendition, usage, provider request id, and prompt profile id.
- `rust/puerh_mind/build.rs` - `image_analysis.proto` added to `compile_protos` and
  `cargo:rerun-if-changed`.
- `alcedo_studio/src/CMakeLists.txt` - `ALCEDO_AI_IMAGE_ANALYSIS_PROTO` var; `image_analysis.pb.{cc,h}`
  and `image_analysis.grpc.pb.{cc,h}` added to `ALCEDO_AI_PROTO_SRCS` / `_HDRS`; a new custom command
  mirroring `ai_runtime.proto` (imports `ai_common.proto`, emits `.pb` + `.grpc.pb`, `DEPENDS` the
  `ai_common.proto` source). `AiProto.lib` now includes `image_analysis`.
- `rust/puerh_mind/src/service/image_analysis.rs` - domain types (`Usage`, `DescribeOutcome`,
  `ScoreOutcome`, `ScoreDimension`, `ProviderError`); code-owned `IMAGE_UNDERSTANDING_SCHEMA` and
  `IMAGE_RATING_SCHEMA` (JSON Schema draft 2020-12); `validate_understanding` / `validate_rating`
  (reject empty caption, out-of-range confidence, empty tags / scores, empty rubric_id);
  `ImageAnalysisProvider` trait (`#[tonic::async_trait]`: `provider_id`, `requires_credential`,
  `capability`, `describe_image`, `score_image`); `MockImageAnalysisProvider` with canned valid results
  and `MockFailure { None, InvalidOutput, Slow, Error, Transient }`. 7 schema tests.
- `rust/puerh_mind/src/server/image_analysis.rs` - `ImageAnalysisServiceImpl { providers,
  default_provider_id, vault, cancel_registry }`; `resolve_credential` (missing ->
  `UNAUTHENTICATED` / `MISSING_CREDENTIAL`; `NotFound` / `Revoked` -> `PERMISSION_DENIED` /
  `CREDENTIAL_REVOKED`; `Expired` -> `PERMISSION_DENIED` / `CREDENTIAL_EXPIRED`); `timeout_duration`;
  `failure_header` / `success_header` (return `Option<AiResponseHeader>`, redact via
  `vault.redact_error_message`); `provider_error_to_header` (`SchemaValidation` -> `PROVIDER_ERROR` /
  `PAYLOAD_DECODE`; `Transient` -> `PROVIDER_UNAVAILABLE` / `PROVIDER_5XX`; `Provider` ->
  `PROVIDER_ERROR` / `INTERNAL`, dropping the inner provider string); `describe_image` / `score_image`
  via `tokio::select! { biased; cancel_rx; timeout(provider_fut) }`. Empty `image_bytes` ->
  `tonic::Status::invalid_argument`. 12 service tests (all 5 plan-required plus cancellation,
  valid-credential, unknown-provider, empty-bytes, provider-error, transient-error, mock-capability).
- `rust/puerh_mind/src/server/mod.rs` - `pub mod image_analysis;`.
- `rust/puerh_mind/src/service/registry.rs` - `register_services` takes `image_providers` +
  `default_image_provider_id`; `ImageAnalysisServiceImpl` shares `vault.clone()` /
  `cancel_registry.clone()` with the AI runtime service; `ImageAnalysisServiceServer` added to the
  router.
- `rust/puerh_mind/src/bootstrap.rs` - `start_server` passes through `image_providers` +
  `default_image_provider_id`.
- `rust/puerh_mind/src/main.rs` - the mock image-analysis provider is registered as the default and its
  no-credential capability is passed as `extra` to `build_capability_descriptors`.

Credential handling (Phase 3 invariant preserved): secrets travel only over gRPC loopback to the Rust
vault; the service resolves `credential_ref` against the vault and never sees the secret material.
`error_message` is redacted via `vault.redact_error_message` before placement; the `Provider(String)`
inner text is dropped by `provider_error_to_header` (only a fixed string is placed), so provider text is
not leaked in the header. No image bytes, base64, or prompt payloads are placed in headers. The service
never writes to DuckDB - C++ owns DB writes (relevant to 5e, not 5b).

Distinct-contract / fail-closed invariants (Phase 5b review focus):

- `DescribeImage` returns `ImageUnderstandingResult` (caption / tags / scene); `ScoreImage` returns
  `ImageRatingResult` (scores / rubric_id / rubric_version / reasons). Distinct `task_id`s on the
  request header; the response header echoes the same `task_id`. A rating result cannot overwrite an
  understanding result and vice versa.
- Provider-specific raw JSON is never the public contract: the code-owned JSON Schemas and the proto
  typed fields are the contract; the (5c) driver validates + normalizes provider output against them.
- A schema-validation failure returns a typed error header with `result = None` (no active annotation) -
  `describe_image_schema_validation_failure_returns_provider_error` proves this.

Deferred to Phase 5c:

- The real HTTP drivers (`OpenRouterChatProvider`, `VolcengineArkResponsesProvider`) - 5b ships only the
  mock. The openrouter / volcengine descriptors advertised by 5a are not backed by a registered
  provider in 5b; a request for those `provider_id`s returns `UNSUPPORTED_TASK` / `TASK_UNKNOWN` until
  5c.
- Real 5xx / rate-limit detection - the `Transient` path is exercised by a mock in 5b; real detection
  lives in the 5c HTTP drivers.
- Secret persistence across sidecar restarts (5c / 5d).

Test results:

- Rust - `cargo test` in `rust/puerh_mind`: 129 passed; 0 failed; 0 ignored; 0 warnings. Includes the 5
  plan-required 5b service tests (`describe_image_returns_valid_understanding`,
  `score_image_returns_valid_rating`, `describe_image_missing_credential_returns_unauthenticated`,
  `describe_image_timeout_returns_deadline_exceeded`,
  `describe_image_schema_validation_failure_returns_provider_error`) plus
  `describe_image_cancellation_returns_cancelled`, `describe_image_with_valid_credential_succeeds`,
  `describe_image_unknown_provider_returns_unsupported_task`,
  `describe_image_empty_bytes_is_transport_error`,
  `describe_image_provider_error_returns_provider_error`,
  `describe_image_transient_error_returns_provider_unavailable`,
  `mock_capability_advertises_no_credential_understanding`, and the 7 schema tests. All three
  `provider_error_to_header` arms (SchemaValidation, Provider, Transient) are exercised, so the service
  has no dead error-mapping code.
- C++ MSVC build (run through the PowerShell tool, per project memory): the `AiProto` target built -
  `image_analysis.pb.{cc,h}` and `image_analysis.grpc.pb.{cc,h}` were generated into `generated/ai/` and
  `AiProto.lib` (10 MB) linked. `AiSidecarRuntimeServiceTest` built (exit code 0), confirming the AiProto
  change (adding `image_analysis` to the library) is regression-free downstream - this target
  transitively depends on AiProto via `AppDiagnostics` -> `SemanticProto` -> `AiProto`. The C++
  generated-proto build coverage requirement is met.

Build note for the next handoff: `--target AiProto` regenerates the `image_analysis` stubs and links
`AiProto.lib` but does not rebuild downstream C++ test targets whose own sources are unchanged - after
any 5c change that touches `AiProto`, build a downstream target explicitly (e.g.
`cmd /c scripts\msvc_env.cmd --build --preset win_debug --target AiSidecarRuntimeServiceTest --parallel 4`)
before ctest, mirroring the Phase 4 note. `image_analysis.grpc.pb.h` includes `ai_common.pb.h`, so
AiProto's existing PUBLIC include dir is sufficient for future 5d C++ consumers - no new include dirs.

Review conclusion: none at phase close; superseded by the 2026-06-25 follow-up (one bug found and
fixed, see "Phase 5b - Follow-up Review & Fixes" below); risk accepted: the openrouter / volcengine
capability descriptors advertised by 5a are not backed by a registered provider in 5b (only the mock
is registered), so a `DescribeImage` / `ScoreImage` for those `provider_id`s returns
`UNSUPPORTED_TASK` / `TASK_UNKNOWN` until the 5c drivers land - this is the intended phase boundary,
not a regression; missing tests: none after the follow-up added 1 regression test and renamed 1 for
honesty.

### Phase 5b - Follow-up Review & Fixes (2026-06-25)

A follow-up review of the 5b surface found one correctness gap in `image_analysis.rs`; fixed with a
regression test.

- P2 (empty `tags` list not rejected): `validate_understanding` only checked
  `out.tags.iter().any(|t| t.trim().is_empty())`, which is false for an empty list, so `tags: []`
  slipped through - contradicting the 5b "reject empty tags / scores" promise. `validate_rating`
  already rejected `scores: []` (it checks `out.scores.is_empty()` and `IMAGE_RATING_SCHEMA` has
  `minItems: 1`), so the gap was understanding-only. The existing
  `validator_rejects_empty_tags_or_scores` test only exercised `vec![""]` (a blank-string tag) for
  understanding and `vec![]` for rating, so the empty-tags-list case for understanding was uncovered.
  Fixed: `validate_understanding` now rejects `out.tags.is_empty()`, and `IMAGE_UNDERSTANDING_SCHEMA`
  gained `minItems: 1` on `tags` plus `minLength: 1` on tag items, matching the rating schema's
  `scores` handling.

Regression tests: added `validator_rejects_empty_tags_list`; renamed
`validator_rejects_empty_tags_or_scores` -> `validator_rejects_blank_tag_string_and_empty_scores_list`
so the name describes what it actually covers (a blank-string tag for understanding, an empty scores
list for rating) rather than implying the empty-tags-list case it did not cover. The schema test now
asserts `tags.minItems == 1` and `tags.items.minLength == 1`. `cargo test` in `rust/puerh_mind`:
133 passed; 0 failed; 0 warnings (was 129; +1 here, +3 in 5a).

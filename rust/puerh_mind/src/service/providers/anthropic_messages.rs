//! Anthropic Messages image-analysis driver (Phase 5c follow-up, Phase 6b
//! generalized; `anthropic_messages`).
//!
//! Speaks the Anthropic Messages API (`POST /v1/messages`, or `/messages` on
//! Opencode): an Alcedo-owned `system` prompt, a single user message carrying the
//! image as an Anthropic `image` content block (`source.type = "base64"`, raw
//! base64 + `media_type` — NOT a data URI) plus a `text` task instruction, and
//! **tool-use** for structured output (`tools` + `tool_choice = { type: "tool",
//! name }`) with the code-owned (sanitized to Anthropic-strict-compatible) Alcedo
//! schema as the tool `input_schema`. Tool-use is more reliable than
//! `response_format.json_schema` for forcing a typed result, and does not depend
//! on a proxy honoring `json_schema`. The response's `content[].tool_use.input`
//! object (for the expected tool name) is extracted with a driver-owned typed
//! parser (`content_json_pointer` is null for this driver), validated + normalized
//! against the code-owned understanding / rating contract, and returned as typed
//! fields. Transient / 429 / 5xx failures retry under the same bounded policy as
//! the other drivers; provider and transport errors map to `ProviderError`
//! variants the service translates into `AiResponseStatus` / `AiErrorCode`.
//!
//! Auth is selected by `config.auth.auth_type`:
//! - `bearer` — `Authorization: Bearer <secret>` (Claude-Code-style drop-in; used
//!   by the shipped `volcengine_ark_coding` Coding Plan config and the Opencode
//!   Go Anthropic preset).
//! - `api_key_header` — `x-api-key: <secret>` (the real Anthropic API convention).
//! - `none` — no credential. The `anthropic-version: 2023-06-01` header is always
//!   sent. The secret is resolved from the Rust credential vault per request and
//!   travels only as a header value; `SecretString::expose()` is called solely at
//!   the header-build site and the cloned `String` is dropped right after the call.
//!
//! The driver is generic Anthropic Messages and is NOT coupled to any provider
//! brand. The same code targets the Volcengine Ark **Coding Plan**
//! (`https://ark.cn-beijing.volces.com/api/coding` + `/v1/messages`), the real
//! Anthropic API (`api.anthropic.com`), and Opencode's Anthropic-compatible
//! endpoint (`https://opencode.ai/zen/go/v1` + `/messages`), selected purely by
//! the config's `base_url` / `endpoint` / `credential_slot`. The provider request
//! id is captured from EITHER the config's `provider_request_id_header` (Opencode
//! echoes `request-id`) OR its `provider_request_id_json_pointer` (the Coding Plan
//! embeds `id` in the body), whichever the config sets — both are optional because
//! compatible providers do not all report the id the same way (Phase 6b
//! deliverable).
//!
//! ## Coding Plan usage-policy caveat (accepted risk)
//!
//! Volcengine positions the **Coding Plan** (`/api/coding/*`) for *AI coding
//! assistants* (Claude Code, Cursor, OpenClaw-style tools). Using this driver as
//! a coding-tool backend is therefore the intended usage class. The policy risk is
//! specifically routing **non-coding production image analysis** through
//! `volcengine_ark_coding`; that remains an operator decision the user owns. The
//! standard production Volcengine image-analysis path stays `volcengine_ark`
//! (`/api/v3/responses`). See the Phase 5c follow-up note in
//! `docs/roadmap/ai_sidecar_backend_plan.md`.
//!
//! ## Vision caveat
//!
//! Coding Plan / Opencode models are primarily coding / text models. Image
//! analysis only works if the selected model accepts image input. The live smoke
//! is the ground-truth check; if the configured `slug` rejects the image, adjust
//! `slug` to a confirmed vision-capable model — the driver itself is
//! model-agnostic.

use serde_json::{Value, json};
use std::sync::Mutex;
use tracing::warn;

use crate::service::credential_vault::SecretString;
use crate::service::image_analysis::{
    AnalyzeOutcome, DescribeOutcome, DiscoveredModel, IMAGE_ANALYSIS_BUNDLE_SCHEMA,
    IMAGE_RATING_SCHEMA, IMAGE_UNDERSTANDING_SCHEMA, ImageAnalysisProvider, ProviderError,
    ScoreOutcome, validate_analyze, validate_rating, validate_understanding,
};
use crate::service::provider_config::{ModelConfig, ProviderConfig};
use crate::service::providers::http_util::{
    MAX_TRANSIENT_RETRIES, build_image_base64, build_rustls_client, extract_provider_request_id,
    extract_usage, json_pointer_str, parse_rating_int, read_response, send_get_with_retry,
    send_with_retry, strict_schema_value,
};

const UNDERSTANDING_SCHEMA_NAME: &str = "alcedo_image_understanding";
const RATING_SCHEMA_NAME: &str = "alcedo_image_rating";
const ANALYSIS_BUNDLE_SCHEMA_NAME: &str = "alcedo_image_analysis_bundle";
const ANTHROPIC_VERSION: &str = "2023-06-01";

pub struct AnthropicMessagesProvider {
    config: ProviderConfig,
    http: reqwest::Client,
    /// Models committed from live `list_models` discovery so an explicitly
    /// requested discovered slug passes the Phase 6c `resolve_model` gate. See
    /// `OpenAiChatCompatibleProvider::discovered_models` for the rationale.
    discovered_models: Mutex<Vec<ModelConfig>>,
}

impl AnthropicMessagesProvider {
    pub fn new(config: ProviderConfig) -> Result<Self, ProviderError> {
        let http = build_rustls_client()?;
        Ok(Self {
            config,
            http,
            discovered_models: Mutex::new(Vec::new()),
        })
    }

    #[allow(dead_code)]
    pub fn with_client(config: ProviderConfig, http: reqwest::Client) -> Self {
        Self {
            config,
            http,
            discovered_models: Mutex::new(Vec::new()),
        }
    }

    fn url(&self) -> String {
        format!("{}{}", self.config.base_url, self.config.endpoint)
    }

    /// Phase 6c: the model-listing (discovery) URL. Defaults to
    /// `{base_url}/models` (the Anthropic-compatible default); a config
    /// `models_endpoint` override replaces the path.
    fn models_url(&self) -> String {
        let path = self.config.models_endpoint.as_deref().unwrap_or("/models");
        format!("{}{}", self.config.base_url, path)
    }

    fn resolve_model(
        &self,
        requested: &str,
    ) -> Result<(String, Option<ModelConfig>), ProviderError> {
        if requested.trim().is_empty() {
            let slug = self.config.defaults.model.clone();
            let entry = self.config.models.iter().find(|m| m.slug == slug).cloned();
            return Ok((slug, entry));
        }
        if let Some(m) = self.config.models.iter().find(|m| m.slug == requested) {
            return Ok((m.slug.clone(), Some(m.clone())));
        }
        let committed = self
            .discovered_models
            .lock()
            .expect("discovered_models mutex poisoned");
        if let Some(m) = committed.iter().find(|m| m.slug == requested) {
            return Ok((m.slug.clone(), Some(m.clone())));
        }
        Err(ProviderError::UnknownModel(requested.to_string()))
    }

    /// Commit discovered models so a later explicit `model_id` resolves. See
    /// `OpenAiChatCompatibleProvider::commit_discovered_models` for the rationale
    /// (idempotent, synthesized `supports_structured_output = true`).
    fn commit_discovered_models(&self, models: &[DiscoveredModel]) {
        let mut committed = self
            .discovered_models
            .lock()
            .expect("discovered_models mutex poisoned");
        for m in models {
            let already_known = self.config.models.iter().any(|c| c.slug == m.model_id)
                || committed.iter().any(|c| c.slug == m.model_id);
            if already_known {
                continue;
            }
            committed.push(ModelConfig {
                slug: m.model_id.clone(),
                display_name: m.display_name.clone(),
                supports_vision: true,
                supports_structured_output: true,
                live_confirmed: false,
                max_image_bytes: Some(self.config.limits.max_image_bytes),
                recommended_rendition: None,
                cost_per_million_input_usd: None,
                cost_per_million_output_usd: None,
                data_collection: None,
            });
        }
    }

    fn ensure_structured_output(&self, model: Option<&ModelConfig>) -> Result<(), ProviderError> {
        if self.config.structured_output.mode == "none" {
            return Err(ProviderError::Provider(
                "provider config does not request structured output; failing closed".to_string(),
            ));
        }
        if let Some(m) = model {
            if !m.supports_structured_output {
                return Err(ProviderError::Provider(
                    "selected model does not support structured output; failing closed".to_string(),
                ));
            }
        }
        Ok(())
    }

    /// Resolve the auth material for a request. Returns `(bearer, extra_header)`:
    /// `bearer` is borrowed from the credential for the `Authorization: Bearer`
    /// header; `extra_header` is an owned `(name, value)` pair for the
    /// `api_key_header` mode (`x-api-key`). The `api_key_header` value is a
    /// short-lived `String` clone of the secret, dropped after the request.
    fn build_auth<'a>(
        &'a self,
        credential: Option<&'a SecretString>,
    ) -> Result<(Option<&'a str>, Option<(String, String)>), ProviderError> {
        match (self.config.auth.auth_type.as_str(), credential) {
            ("none", _) => Ok((None, None)),
            ("bearer", Some(s)) => Ok((Some(s.expose()), None)),
            ("bearer", None) => Err(ProviderError::Provider(
                "bearer provider called without a credential".to_string(),
            )),
            ("api_key_header", Some(s)) => Ok((
                None,
                Some(("x-api-key".to_string(), s.expose().to_string())),
            )),
            ("api_key_header", None) => Err(ProviderError::Provider(
                "api_key_header provider called without a credential".to_string(),
            )),
            (other, _) => Err(ProviderError::Provider(format!(
                "unsupported auth type {other}"
            ))),
        }
    }

    fn attribution_headers(&self) -> Vec<(String, String)> {
        self.config
            .attribution_headers
            .iter()
            .map(|(k, v)| (k.clone(), v.clone()))
            .collect()
    }

    fn build_messages_body(
        &self,
        slug: &str,
        media_type: &str,
        image_b64: &str,
        schema: Value,
        schema_name: &str,
        system: &str,
        instruction: &str,
    ) -> Value {
        json!({
            "model": slug,
            "max_tokens": self.config.limits.max_output_tokens,
            "system": system,
            "temperature": self.config.defaults.temperature,
            "stream": false,
            "messages": [
                { "role": "user", "content": [
                    { "type": "image", "source": {
                        "type": "base64", "media_type": media_type, "data": image_b64
                    }},
                    { "type": "text", "text": instruction }
                ]}
            ],
            "tools": [
                { "name": schema_name,
                  "description": "Return the Alcedo image-analysis result as a JSON object matching input_schema.",
                  "input_schema": schema }
            ],
            "tool_choice": { "type": "tool", "name": schema_name }
        })
    }

    /// Driver-owned typed content extraction: walk `content[]` and return the
    /// `input` object of the first `tool_use` item whose `name` matches
    /// `expected_name`. The config sets `content_json_pointer = null` for this
    /// driver, so the parser is code-owned — Anthropic's `content` array may hold
    /// `text` / `thinking` / `tool_use` blocks in any order, so a typed walk by
    /// tool name is more robust than a fixed pointer. A missing or wrong-name
    /// `tool_use` returns `None` (the caller maps that to `SchemaValidation`,
    /// fail-closed — no active annotation is produced).
    fn extract_tool_use_input<'a>(body: &'a Value, expected_name: &str) -> Option<&'a Value> {
        let content = body.get("content")?.as_array()?;
        for item in content {
            if item.get("type").and_then(|t| t.as_str()) == Some("tool_use")
                && item.get("name").and_then(|n| n.as_str()) == Some(expected_name)
            {
                return item.get("input");
            }
        }
        None
    }

    fn parse_describe(
        &self,
        body: &Value,
        model_id: &str,
        header_req_id: &str,
    ) -> Result<DescribeOutcome, ProviderError> {
        let parsed = Self::extract_tool_use_input(body, UNDERSTANDING_SCHEMA_NAME).ok_or_else(|| {
            ProviderError::SchemaValidationMessage(format!(
                "provider response did not contain tool_use input named {UNDERSTANDING_SCHEMA_NAME}; expected Anthropic-compatible structured tool output"
            ))
        })?;
        let out = DescribeOutcome {
            caption: parsed
                .get("caption")
                .and_then(|v| v.as_str())
                .unwrap_or("")
                .trim()
                .to_string(),
            tags: parsed
                .get("tags")
                .and_then(|v| v.as_array())
                .map(|a| {
                    a.iter()
                        .filter_map(|t| t.as_str().map(|s| s.trim().to_string()))
                        .collect()
                })
                .unwrap_or_default(),
            scene: parsed
                .get("scene")
                .and_then(|v| v.as_str())
                .unwrap_or("")
                .to_string(),
            confidence: parsed
                .get("confidence")
                .and_then(|v| v.as_f64())
                .unwrap_or(f64::NAN),
            model_id: model_id.to_string(),
            usage: extract_usage(
                self.config
                    .response
                    .usage_json_pointer
                    .as_deref()
                    .and_then(|p| json_pointer_str(body, p)),
            ),
            // Captured from the response header OR body JSON pointer by the caller
            // (Phase 6b: compatible providers report the id inconsistently).
            provider_request_id: header_req_id.to_string(),
        };
        validate_understanding(&out)?;
        Ok(out)
    }

    fn parse_score(
        &self,
        body: &Value,
        model_id: &str,
        header_req_id: &str,
    ) -> Result<ScoreOutcome, ProviderError> {
        let parsed = Self::extract_tool_use_input(body, RATING_SCHEMA_NAME).ok_or_else(|| {
            ProviderError::SchemaValidationMessage(format!(
                "provider response did not contain tool_use input named {RATING_SCHEMA_NAME}; expected Anthropic-compatible structured tool output"
            ))
        })?;
        // 1..=5 integer star rating. Accept an exact integer or an integer-valued
        // float (e.g. `4.0`); a fractional float (e.g. `4.9`) is NOT truncated —
        // `parse_rating_int` returns None, the rating falls back to 0 (outside the
        // 1..=5 contract), and `validate_rating` rejects it (fail closed).
        let rating = parsed.get("rating").and_then(parse_rating_int).unwrap_or(0);
        let out = ScoreOutcome {
            rating,
            rubric_id: parsed
                .get("rubric_id")
                .and_then(|v| v.as_str())
                .unwrap_or("")
                .to_string(),
            rubric_version: parsed
                .get("rubric_version")
                .and_then(|v| v.as_str())
                .unwrap_or("")
                .to_string(),
            reasons: parsed
                .get("reasons")
                .and_then(|v| v.as_str())
                .unwrap_or("")
                .to_string(),
            model_id: model_id.to_string(),
            usage: extract_usage(
                self.config
                    .response
                    .usage_json_pointer
                    .as_deref()
                    .and_then(|p| json_pointer_str(body, p)),
            ),
            provider_request_id: header_req_id.to_string(),
        };
        validate_rating(&out)?;
        Ok(out)
    }

    fn parse_analyze(
        &self,
        body: &Value,
        model_id: &str,
        header_req_id: &str,
    ) -> Result<AnalyzeOutcome, ProviderError> {
        let parsed = Self::extract_tool_use_input(body, ANALYSIS_BUNDLE_SCHEMA_NAME).ok_or_else(|| {
            ProviderError::SchemaValidationMessage(format!(
                "provider response did not contain tool_use input named {ANALYSIS_BUNDLE_SCHEMA_NAME}; expected Anthropic-compatible structured tool output"
            ))
        })?;
        let understanding = parsed.get("understanding").ok_or_else(|| {
            ProviderError::SchemaValidationMessage(
                "provider response did not contain understanding object".to_string(),
            )
        })?;
        let rating = parsed.get("rating").ok_or_else(|| {
            ProviderError::SchemaValidationMessage(
                "provider response did not contain rating object".to_string(),
            )
        })?;
        let usage = extract_usage(
            self.config
                .response
                .usage_json_pointer
                .as_deref()
                .and_then(|p| json_pointer_str(body, p)),
        );
        let understanding = DescribeOutcome {
            caption: understanding
                .get("caption")
                .and_then(|v| v.as_str())
                .unwrap_or("")
                .trim()
                .to_string(),
            tags: understanding
                .get("tags")
                .and_then(|v| v.as_array())
                .map(|a| {
                    a.iter()
                        .filter_map(|t| t.as_str().map(|s| s.trim().to_string()))
                        .collect()
                })
                .unwrap_or_default(),
            scene: understanding
                .get("scene")
                .and_then(|v| v.as_str())
                .unwrap_or("")
                .to_string(),
            confidence: understanding
                .get("confidence")
                .and_then(|v| v.as_f64())
                .unwrap_or(f64::NAN),
            model_id: model_id.to_string(),
            usage: usage.clone(),
            provider_request_id: header_req_id.to_string(),
        };
        let rating = ScoreOutcome {
            rating: rating.get("rating").and_then(parse_rating_int).unwrap_or(0),
            rubric_id: rating
                .get("rubric_id")
                .and_then(|v| v.as_str())
                .unwrap_or("")
                .to_string(),
            rubric_version: rating
                .get("rubric_version")
                .and_then(|v| v.as_str())
                .unwrap_or("")
                .to_string(),
            reasons: rating
                .get("reasons")
                .and_then(|v| v.as_str())
                .unwrap_or("")
                .to_string(),
            model_id: model_id.to_string(),
            usage: usage.clone(),
            provider_request_id: header_req_id.to_string(),
        };
        let out = AnalyzeOutcome {
            understanding: Some(understanding),
            rating: Some(rating),
            model_id: model_id.to_string(),
            usage,
            provider_request_id: header_req_id.to_string(),
        };
        validate_analyze(&out)?;
        Ok(out)
    }

    /// Build the per-request header set: `anthropic-version` always, then
    /// attribution headers, then the `api_key_header` secret when applicable. The
    /// bearer (when applicable) is returned separately and applied by
    /// `send_with_retry` via `bearer_auth`; it never appears in this vec, so it is
    /// never logged.
    fn request_headers(
        &self,
        extra_auth_header: Option<(String, String)>,
    ) -> Vec<(String, String)> {
        let mut headers = vec![(
            "anthropic-version".to_string(),
            ANTHROPIC_VERSION.to_string(),
        )];
        headers.extend(self.attribution_headers());
        if let Some((k, v)) = extra_auth_header {
            headers.push((k, v));
        }
        headers
    }
}

fn describe_prompt(prompt_profile_id: &str, output_language: &str) -> (String, String) {
    let mut system = "You are an image understanding assistant for Alcedo Studio. Analyze the supplied image and respond with a single JSON object matching the provided schema. The object must contain: \"caption\" (a concise one-line description of the image), \"tags\" (an array of short lowercase searchable tags, with at least one tag), \"scene\" (a short scene or category hint, or an empty string if none), and \"confidence\" (your confidence in the description, a number between 0.0 and 1.0). Output only the JSON object — no prose, no markdown code fences.".to_string();
    system.push_str(&crate::service::image_analysis::language_directive(
        output_language,
    ));
    let mut instruction = "Describe this image for a photo library.".to_string();
    if !prompt_profile_id.trim().is_empty() {
        instruction.push_str(&format!(" Prompt profile: {prompt_profile_id}."));
    }
    instruction.push_str(" Return only the JSON object described above.");
    (system, instruction)
}

fn score_prompt(
    prompt_profile_id: &str,
    rubric_id: &str,
    rating_severity: &str,
    output_language: &str,
) -> (String, String) {
    let system = crate::service::image_analysis::rating_system_prompt(rating_severity, output_language);
    let mut instruction = "Rate this image on a 1–5 star scale.".to_string();
    if !rubric_id.trim().is_empty() {
        instruction.push_str(&format!(" Rubric: {rubric_id}."));
    }
    if !prompt_profile_id.trim().is_empty() {
        instruction.push_str(&format!(" Prompt profile: {prompt_profile_id}."));
    }
    instruction.push_str(
        " Return only the JSON object described above, with an integer \"rating\" between 1 and 5.",
    );
    (system, instruction)
}

fn analyze_prompt(
    prompt_profile_id: &str,
    rubric_id: &str,
    rating_severity: &str,
    output_language: &str,
) -> (String, String) {
    let mut system = "You are an image analysis assistant for Alcedo Studio. Analyze the supplied image and return one tool input object matching the schema. The top-level object must contain \"understanding\" (caption, tags, scene, confidence) and \"rating\" (rating, rubric_id, rubric_version, reasons). The rating is a 1-5 integer star rating; do not include a rating confidence.".to_string();
    match rating_severity.trim().to_ascii_lowercase().as_str() {
        "lite" => system.push_str(" Be generous when rating: ordinary competent photos default to 3-4 stars with mild reasons."),
        "xhigh" | "x_high" | "high" => system.push_str(" Be exacting when rating: judge composition, exposure, focus, lighting, and intent strictly, and write reasons in a harsh critic voice."),
        _ => system.push_str(" Use a balanced rating standard."),
    }
    system.push_str(&crate::service::image_analysis::language_directive(output_language));
    let mut instruction = "Analyze and rate this image in one pass.".to_string();
    if !rubric_id.trim().is_empty() {
        instruction.push_str(&format!(" Rubric: {rubric_id}."));
    }
    if !prompt_profile_id.trim().is_empty() {
        instruction.push_str(&format!(" Prompt profile: {prompt_profile_id}."));
    }
    instruction.push_str(" Return only the requested tool input object.");
    (system, instruction)
}

#[tonic::async_trait]
impl ImageAnalysisProvider for AnthropicMessagesProvider {
    fn provider_id(&self) -> &str {
        &self.config.provider_id
    }

    fn requires_credential(&self) -> bool {
        self.config.auth.auth_type != "none"
    }

    fn capability(&self) -> crate::proto::alcedo::ai::AiCapability {
        // Real providers advertise via the registry (Phase 5a); this trait method
        // is a compliance fallback for the default model and is not used to
        // advertise the provider to the C++ host.
        use crate::proto::alcedo::ai::{AiCapability, AiInputKind, AiOutputKind};
        AiCapability {
            task_id: "image_understanding.describe".to_string(),
            provider_id: self.config.provider_id.clone(),
            model_id: self.config.defaults.model.clone(),
            input_kinds: vec![
                AiInputKind::AiInputThumbnail as i32,
                AiInputKind::AiInputPreview as i32,
                AiInputKind::AiInputImage as i32,
            ],
            output_kinds: vec![
                AiOutputKind::AiOutputCaption as i32,
                AiOutputKind::AiOutputTags as i32,
            ],
            supports_batch: false,
            supports_cancel: true,
            requires_credential: self.requires_credential(),
            max_payload_bytes: self.config.limits.max_image_bytes as i64,
        }
    }

    async fn describe_image(
        &self,
        image_bytes: &[u8],
        model_id: &str,
        prompt_profile_id: &str,
        output_language: &str,
        credential: Option<&SecretString>,
    ) -> Result<DescribeOutcome, ProviderError> {
        let (slug, model) = self.resolve_model(model_id)?;
        self.ensure_structured_output(model.as_ref())?;
        let (bearer, extra_auth_header) = self.build_auth(credential)?;
        let (media_type, image_b64) = build_image_base64(image_bytes)?;
        let schema = strict_schema_value(IMAGE_UNDERSTANDING_SCHEMA)?;
        let (system, instruction) = describe_prompt(prompt_profile_id, output_language);
        let body = self.build_messages_body(
            &slug,
            &media_type,
            &image_b64,
            schema,
            UNDERSTANDING_SCHEMA_NAME,
            &system,
            &instruction,
        );
        let headers = self.request_headers(extra_auth_header);
        let resp = send_with_retry(
            &self.http,
            &self.url(),
            &body,
            &headers,
            bearer,
            MAX_TRANSIENT_RETRIES,
        )
        .await?;
        let (resp_headers, resp_body) = read_response(resp).await?;
        let header_req_id = extract_provider_request_id(
            &resp_headers,
            &resp_body,
            self.config.response.provider_request_id_header.as_deref(),
            self.config
                .response
                .provider_request_id_json_pointer
                .as_deref(),
        );
        let outcome = self.parse_describe(&resp_body, &slug, &header_req_id)?;
        warn!(
            provider = %self.config.provider_id,
            model = %slug,
            provider_request_id = %outcome.provider_request_id,
            "DescribeImage completed"
        );
        Ok(outcome)
    }

    async fn score_image(
        &self,
        image_bytes: &[u8],
        model_id: &str,
        prompt_profile_id: &str,
        rubric_id: &str,
        rating_severity: &str,
        output_language: &str,
        credential: Option<&SecretString>,
    ) -> Result<ScoreOutcome, ProviderError> {
        let (slug, model) = self.resolve_model(model_id)?;
        self.ensure_structured_output(model.as_ref())?;
        let (bearer, extra_auth_header) = self.build_auth(credential)?;
        let (media_type, image_b64) = build_image_base64(image_bytes)?;
        let schema = strict_schema_value(IMAGE_RATING_SCHEMA)?;
        let (system, instruction) = score_prompt(prompt_profile_id, rubric_id, rating_severity, output_language);
        let body = self.build_messages_body(
            &slug,
            &media_type,
            &image_b64,
            schema,
            RATING_SCHEMA_NAME,
            &system,
            &instruction,
        );
        let headers = self.request_headers(extra_auth_header);
        let resp = send_with_retry(
            &self.http,
            &self.url(),
            &body,
            &headers,
            bearer,
            MAX_TRANSIENT_RETRIES,
        )
        .await?;
        let (resp_headers, resp_body) = read_response(resp).await?;
        let header_req_id = extract_provider_request_id(
            &resp_headers,
            &resp_body,
            self.config.response.provider_request_id_header.as_deref(),
            self.config
                .response
                .provider_request_id_json_pointer
                .as_deref(),
        );
        let outcome = self.parse_score(&resp_body, &slug, &header_req_id)?;
        warn!(
            provider = %self.config.provider_id,
            model = %slug,
            provider_request_id = %outcome.provider_request_id,
            "ScoreImage completed"
        );
        Ok(outcome)
    }

    async fn analyze_image(
        &self,
        image_bytes: &[u8],
        model_id: &str,
        prompt_profile_id: &str,
        rubric_id: &str,
        rating_severity: &str,
        output_language: &str,
        credential: Option<&SecretString>,
    ) -> Result<AnalyzeOutcome, ProviderError> {
        let (slug, model) = self.resolve_model(model_id)?;
        self.ensure_structured_output(model.as_ref())?;
        let (bearer, extra_auth_header) = self.build_auth(credential)?;
        let (media_type, image_b64) = build_image_base64(image_bytes)?;
        let schema = strict_schema_value(IMAGE_ANALYSIS_BUNDLE_SCHEMA)?;
        let (system, instruction) =
            analyze_prompt(prompt_profile_id, rubric_id, rating_severity, output_language);
        let body = self.build_messages_body(
            &slug,
            &media_type,
            &image_b64,
            schema,
            ANALYSIS_BUNDLE_SCHEMA_NAME,
            &system,
            &instruction,
        );
        let headers = self.request_headers(extra_auth_header);
        let resp = send_with_retry(
            &self.http,
            &self.url(),
            &body,
            &headers,
            bearer,
            MAX_TRANSIENT_RETRIES,
        )
        .await?;
        let (resp_headers, resp_body) = read_response(resp).await?;
        let header_req_id = extract_provider_request_id(
            &resp_headers,
            &resp_body,
            self.config.response.provider_request_id_header.as_deref(),
            self.config
                .response
                .provider_request_id_json_pointer
                .as_deref(),
        );
        let outcome = self.parse_analyze(&resp_body, &slug, &header_req_id)?;
        warn!(
            provider = %self.config.provider_id,
            model = %slug,
            provider_request_id = %outcome.provider_request_id,
            "AnalyzeImage completed"
        );
        Ok(outcome)
    }

    /// Phase 6c: discover models by listing the configured Anthropic-compatible
    /// `/models` endpoint. Auth follows the config convention (`bearer` ->
    /// `Authorization: Bearer`, `api_key_header` -> `x-api-key`, both with
    /// `anthropic-version`). The Anthropic list shape paginates with
    /// `has_more` + `last_id`; we follow `after_id` pages up to a bounded cap so
    /// a misbehaving server cannot stall discovery. Discovered models are
    /// unverified candidates — no capability flags are inferred.
    async fn list_models(
        &self,
        credential: Option<&SecretString>,
    ) -> Result<Vec<DiscoveredModel>, ProviderError> {
        let (bearer, extra_auth_header) = self.build_auth(credential)?;
        let headers = self.request_headers(extra_auth_header);
        let mut out = Vec::new();
        let mut after_id: Option<String> = None;
        // Bound pagination so a server that always reports has_more=true cannot
        // stall discovery. 50 pages × (typical 100/page) is far beyond any real
        // account's model list.
        for _ in 0..50 {
            let url = match &after_id {
                Some(id) => format!("{}?limit=100&after_id={}", self.models_url(), id),
                None => format!("{}?limit=100", self.models_url()),
            };
            let resp =
                send_get_with_retry(&self.http, &url, &headers, bearer, MAX_TRANSIENT_RETRIES)
                    .await?;
            let (_resp_headers, body) = read_response(resp).await?;
            let data = body
                .get("data")
                .and_then(|d| d.as_array())
                .ok_or(ProviderError::SchemaValidation)?;
            for item in data {
                let Some(id) = item.get("id").and_then(|v| v.as_str()) else {
                    continue;
                };
                let display_name = item
                    .get("display_name")
                    .and_then(|v| v.as_str())
                    .unwrap_or(id)
                    .to_string();
                out.push(DiscoveredModel {
                    model_id: id.to_string(),
                    display_name,
                    source_provider_id: self.config.provider_id.clone(),
                });
            }
            let has_more = body
                .get("has_more")
                .and_then(|v| v.as_bool())
                .unwrap_or(false);
            if !has_more {
                break;
            }
            // Follow the cursor: prefer the response's `last_id` field (Anthropic
            // shape); fall back to the last item's id. If neither is available
            // (server reports has_more with no items and no cursor), stop to
            // avoid an infinite empty-page loop.
            let next_after = body
                .get("last_id")
                .and_then(|v| v.as_str())
                .map(|s| s.to_string())
                .or_else(|| out.last().map(|m| m.model_id.clone()));
            let Some(next_after) = next_after else {
                break;
            };
            after_id = Some(next_after);
        }
        // Commit discovered candidates so a later explicit `model_id` resolves
        // (see OpenAiChatCompatibleProvider::list_models).
        self.commit_discovered_models(&out);
        Ok(out)
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::proto::alcedo::ai::image_analysis_service_server::ImageAnalysisService;
    use crate::service::credential_vault::SecretString;
    use crate::service::provider_config::load_provider_configs;
    use std::collections::HashMap;
    use std::sync::Arc;
    use wiremock::matchers::{header, method, path, query_param};
    use wiremock::{Mock, MockServer, ResponseTemplate};

    const TEST_SECRET: &str = "ark-coding-test-key-DO-NOT-LEAK";
    const TEST_PROMPT: &str = "Describe this image for a photo library.";
    const RAW_BODY_SENTINEL: &str = "RAW_ARK_CODING_BODY_SENTINEL";

    fn test_image_png() -> Vec<u8> {
        let img = image::RgbImage::from_pixel(2, 2, image::Rgb([10, 20, 30]));
        let mut cursor = std::io::Cursor::new(Vec::new());
        image::DynamicImage::ImageRgb8(img)
            .write_to(&mut cursor, image::ImageFormat::Png)
            .expect("encode png");
        cursor.into_inner()
    }

    fn provider_for(server: &MockServer) -> AnthropicMessagesProvider {
        let mut config = load_provider_configs(None)
            .expect("built-ins load")
            .get("volcengine_ark_coding")
            .expect("volcengine_ark_coding built-in")
            .clone();
        config.base_url = server.uri();
        AnthropicMessagesProvider::new(config).expect("provider builds")
    }

    /// Build an Anthropic Messages-shaped success body. `input_json` is the
    /// model's tool arguments, placed in `content[0].tool_use.input` as a parsed
    /// JSON object — the exact envelope the driver-owned parser walks. Mirrors the
    /// real Anthropic response shape (where `input` is an object, not a string).
    fn ok_messages_body(tool_name: &str, input_json: &str) -> serde_json::Value {
        let input: Value = serde_json::from_str(input_json).expect("input json parses");
        json!({
            "id": "ark-coding-resp-789",
            "type": "message",
            "role": "assistant",
            "content": [
                { "type": "tool_use", "id": "toolu_01", "name": tool_name, "input": input }
            ],
            "usage": { "input_tokens": 90, "output_tokens": 35 }
        })
    }

    fn secret() -> SecretString {
        SecretString::new(TEST_SECRET.to_string())
    }

    #[tokio::test]
    async fn sends_authorization_and_anthropic_version_headers() {
        let server = MockServer::start().await;
        Mock::given(method("POST"))
            .and(path("/v1/messages"))
            .and(header("authorization", format!("Bearer {TEST_SECRET}")))
            .and(header("anthropic-version", ANTHROPIC_VERSION))
            .respond_with(ResponseTemplate::new(200).set_body_json(ok_messages_body(
                UNDERSTANDING_SCHEMA_NAME,
                r#"{"caption":"a small image","tags":["test"],"scene":"studio","confidence":0.7}"#,
            )))
            .mount(&server)
            .await;

        let provider = provider_for(&server);
        let out = provider
            .describe_image(&test_image_png(), "", "profile-1", "", Some(&secret()))
            .await
            .expect("describe ok");
        assert_eq!(out.caption, "a small image");

        let reqs = server.received_requests().await.expect("requests captured");
        assert_eq!(reqs.len(), 1);
        // bearer mode: x-api-key must NOT be sent.
        assert!(
            reqs[0].headers.get("x-api-key").is_none(),
            "x-api-key present in bearer mode"
        );
    }

    #[tokio::test]
    async fn request_body_uses_messages_shape_with_tool_use() {
        let server = MockServer::start().await;
        Mock::given(method("POST"))
            .and(path("/v1/messages"))
            .respond_with(ResponseTemplate::new(200).set_body_json(ok_messages_body(
                UNDERSTANDING_SCHEMA_NAME,
                r#"{"caption":"c","tags":["t"],"scene":"","confidence":0.5}"#,
            )))
            .mount(&server)
            .await;

        let provider = provider_for(&server);
        provider
            .describe_image(
                &test_image_png(),
                "doubao-seed-2.0-lite",
                "",
                "",
                Some(&secret()),
            )
            .await
            .expect("describe ok");

        let reqs = server.received_requests().await.expect("requests captured");
        assert_eq!(reqs.len(), 1);
        let body: Value = serde_json::from_slice(&reqs[0].body).expect("body json");
        assert_eq!(body["model"], "doubao-seed-2.0-lite");
        assert_eq!(body["stream"], false);
        // Anthropic Messages requires max_tokens (not max_output_tokens).
        assert_eq!(body["max_tokens"], 1200);
        assert!(body["system"].is_string(), "system must be a string");
        assert_eq!(body["messages"][0]["role"], "user");
        // Anthropic image block: source.type=base64, media_type, raw base64 data.
        let img = &body["messages"][0]["content"][0];
        assert_eq!(img["type"], "image");
        assert_eq!(img["source"]["type"], "base64");
        assert_eq!(img["source"]["media_type"], "image/png");
        let data = img["source"]["data"].as_str().expect("data string");
        assert!(
            !data.starts_with("data:"),
            "image data is a data URI: {data}"
        );
        assert!(!data.is_empty(), "image base64 empty");
        assert_eq!(body["messages"][0]["content"][1]["type"], "text");
        // Structured output via tools + tool_choice, not text.format.json_schema.
        assert_eq!(body["tools"][0]["name"], "alcedo_image_understanding");
        assert_eq!(body["tools"][0]["input_schema"]["type"], "object");
        assert_eq!(
            body["tools"][0]["input_schema"]["additionalProperties"],
            false
        );
        let required = body["tools"][0]["input_schema"]["required"]
            .as_array()
            .expect("required array");
        assert!(required.iter().any(|v| v == "caption"), "caption required");
        assert!(required.iter().any(|v| v == "tags"), "tags required");
        assert_eq!(body["tool_choice"]["type"], "tool");
        assert_eq!(body["tool_choice"]["name"], "alcedo_image_understanding");
        // No Responses-shape fields leak in.
        assert!(
            body.get("input").is_none(),
            "Responses `input` field present"
        );
        assert!(body.get("text").is_none(), "Responses `text` field present");
        assert!(
            body.get("max_output_tokens").is_none(),
            "Responses `max_output_tokens` present"
        );
    }

    #[tokio::test]
    async fn extracts_tool_use_input_from_messages_envelope() {
        let server = MockServer::start().await;
        // The content array may hold a text block before the tool_use block; the
        // walker must skip the text and find the right tool_use input.
        let body = json!({
            "id": "ark-coding-resp-100",
            "type": "message",
            "role": "assistant",
            "content": [
                { "type": "text", "text": "thinking about the image..." },
                { "type": "tool_use", "id": "toolu_02", "name": UNDERSTANDING_SCHEMA_NAME,
                  "input": { "caption": "sunrise", "tags": ["sun", "sky"], "scene": "outdoor", "confidence": 0.8 } }
            ],
            "usage": { "input_tokens": 50, "output_tokens": 20 }
        });
        Mock::given(method("POST"))
            .and(path("/v1/messages"))
            .respond_with(ResponseTemplate::new(200).set_body_json(body))
            .mount(&server)
            .await;
        let provider = provider_for(&server);
        let out = provider
            .describe_image(&test_image_png(), "", "", "", Some(&secret()))
            .await
            .expect("describe ok");
        assert_eq!(out.caption, "sunrise");
        assert_eq!(out.tags, vec!["sun".to_string(), "sky".to_string()]);
        assert_eq!(out.scene, "outdoor");
        assert!((out.confidence - 0.8).abs() < 1e-9);
        assert_eq!(out.usage.input_tokens, 50);
        assert_eq!(out.usage.output_tokens, 20);
        assert_eq!(out.provider_request_id, "ark-coding-resp-100");
        validate_understanding(&out).expect("canned outcome validates");
    }

    #[tokio::test]
    async fn parses_understanding_response_and_captures_usage() {
        let server = MockServer::start().await;
        Mock::given(method("POST"))
            .and(path("/v1/messages"))
            .respond_with(ResponseTemplate::new(200).set_body_json(ok_messages_body(
                UNDERSTANDING_SCHEMA_NAME,
                r#"{"caption":"a small image","tags":["test"],"scene":"studio","confidence":0.7}"#,
            )))
            .mount(&server)
            .await;
        let provider = provider_for(&server);
        let out = provider
            .describe_image(&test_image_png(), "", "profile-1", "", Some(&secret()))
            .await
            .expect("describe ok");
        assert_eq!(out.caption, "a small image");
        assert_eq!(out.tags, vec!["test".to_string()]);
        assert_eq!(out.scene, "studio");
        assert!((out.confidence - 0.7).abs() < 1e-9);
        // Anthropic usage has input/output tokens but no total_tokens.
        assert_eq!(out.usage.input_tokens, 90);
        assert_eq!(out.usage.output_tokens, 35);
        assert_eq!(out.usage.total_tokens, 0);
        assert_eq!(out.provider_request_id, "ark-coding-resp-789");
        validate_understanding(&out).expect("canned outcome validates");
    }

    #[tokio::test]
    async fn parses_rating_response_and_captures_usage() {
        let server = MockServer::start().await;
        Mock::given(method("POST"))
            .and(path("/v1/messages"))
            .respond_with(ResponseTemplate::new(200).set_body_json(ok_messages_body(
                RATING_SCHEMA_NAME,
                r#"{"rating":4,"rubric_id":"alcedo-default-v1","rubric_version":"1","reasons":"r"}"#,
            )))
            .mount(&server)
            .await;
        let provider = provider_for(&server);
        let out = provider
            .score_image(
                &test_image_png(),
                "",
                "",
                "alcedo-default-v1",
                "",
                "",
                Some(&secret()),
            )
            .await
            .expect("score ok");
        // Single 1..=5 integer star rating; no scores array, no confidence.
        assert_eq!(out.rating, 4);
        assert_eq!(out.rubric_id, "alcedo-default-v1");
        assert_eq!(out.rubric_version, "1");
        assert_eq!(out.reasons, "r");
        assert_eq!(out.usage.input_tokens, 90);
        validate_rating(&out).expect("canned rating validates");
    }

    #[tokio::test]
    async fn parses_rating_rejects_fractional_float() {
        // A fractional rating (4.9) is schema-invalid and must NOT be truncated
        // to 4 — fail closed: the parser yields 0 and validate_rating maps it to
        // SchemaValidation (no active annotation).
        let server = MockServer::start().await;
        Mock::given(method("POST"))
            .and(path("/v1/messages"))
            .respond_with(ResponseTemplate::new(200).set_body_json(ok_messages_body(
                RATING_SCHEMA_NAME,
                r#"{"rating":4.9,"rubric_id":"alcedo-default-v1","rubric_version":"1","reasons":"x"}"#,
            )))
            .mount(&server)
            .await;
        let provider = provider_for(&server);
        let err = provider
            .score_image(
                &test_image_png(),
                "",
                "",
                "alcedo-default-v1",
                "",
                "",
                Some(&secret()),
            )
            .await
            .expect_err("fractional rating rejected");
        assert_eq!(err, ProviderError::SchemaValidation);
    }

    #[tokio::test]
    async fn rate_limit_maps_to_transient() {
        let server = MockServer::start().await;
        Mock::given(method("POST"))
            .and(path("/v1/messages"))
            .respond_with(ResponseTemplate::new(429))
            .up_to_n_times(2)
            .mount(&server)
            .await;
        let provider = provider_for(&server);
        let err = provider
            .describe_image(&test_image_png(), "", "", "", Some(&secret()))
            .await
            .expect_err("transient after retries");
        assert_eq!(err, ProviderError::Transient);
        assert_eq!(server.received_requests().await.unwrap().len(), 2);
    }

    #[tokio::test]
    async fn server_500_is_retried_then_succeeds() {
        let server = MockServer::start().await;
        Mock::given(method("POST"))
            .and(path("/v1/messages"))
            .respond_with(ResponseTemplate::new(500))
            .up_to_n_times(1)
            .mount(&server)
            .await;
        Mock::given(method("POST"))
            .and(path("/v1/messages"))
            .respond_with(ResponseTemplate::new(200).set_body_json(ok_messages_body(
                UNDERSTANDING_SCHEMA_NAME,
                r#"{"caption":"c","tags":["t"],"scene":"","confidence":0.5}"#,
            )))
            .mount(&server)
            .await;
        let provider = provider_for(&server);
        let out = provider
            .describe_image(&test_image_png(), "", "", "", Some(&secret()))
            .await
            .expect("succeeds after retry");
        assert_eq!(out.caption, "c");
        assert_eq!(server.received_requests().await.unwrap().len(), 2);
    }

    #[tokio::test]
    async fn client_4xx_is_not_retried_and_maps_to_provider_error() {
        // A 4xx with a structured Anthropic-style error body is not retried and
        // maps to ProviderError::Provider. The raw provider error text must NOT
        // surface in the ProviderError string (the service drops it before
        // placement; here we assert the driver itself does not echo the raw body).
        let server = MockServer::start().await;
        Mock::given(method("POST"))
            .and(path("/v1/messages"))
            .respond_with(ResponseTemplate::new(400).set_body_json(json!({
                "type": "error",
                "error": { "type": "invalid_request_error", "message": RAW_BODY_SENTINEL }
            })))
            .mount(&server)
            .await;
        let provider = provider_for(&server);
        let err = provider
            .describe_image(&test_image_png(), "", "", "", Some(&secret()))
            .await
            .expect_err("400 not retried");
        assert!(matches!(err, ProviderError::Provider(_)), "{err:?}");
        assert_eq!(server.received_requests().await.unwrap().len(), 1);
        assert!(
            !err.to_string().contains(RAW_BODY_SENTINEL),
            "raw body leaked: {err}"
        );
        assert!(
            !err.to_string().contains("invalid_request_error"),
            "error type leaked: {err}"
        );
    }

    #[tokio::test]
    async fn schema_failure_does_not_produce_active_result() {
        let server = MockServer::start().await;
        Mock::given(method("POST"))
            .and(path("/v1/messages"))
            .respond_with(ResponseTemplate::new(200).set_body_json(ok_messages_body(
                UNDERSTANDING_SCHEMA_NAME,
                r#"{"caption":"c","tags":[],"scene":"","confidence":0.5}"#,
            )))
            .mount(&server)
            .await;
        let provider = provider_for(&server);
        let err = provider
            .describe_image(&test_image_png(), "", "", "", Some(&secret()))
            .await
            .expect_err("empty tags rejected");
        assert_eq!(err, ProviderError::SchemaValidation);
    }

    #[tokio::test]
    async fn missing_tool_use_maps_to_schema_validation() {
        let server = MockServer::start().await;
        // 200 but only a text block (no tool_use) -> driver-owned parser returns None.
        Mock::given(method("POST"))
            .and(path("/v1/messages"))
            .respond_with(ResponseTemplate::new(200).set_body_json(json!({
                "id": "ark-coding-resp-x",
                "type": "message",
                "role": "assistant",
                "content": [ { "type": "text", "text": "I cannot use a tool here." } ]
            })))
            .mount(&server)
            .await;
        let provider = provider_for(&server);
        let err = provider
            .describe_image(&test_image_png(), "", "", "", Some(&secret()))
            .await
            .expect_err("no tool_use");
        match err {
            ProviderError::SchemaValidationMessage(message) => {
                assert!(
                    message.contains("tool_use input named alcedo_image_understanding"),
                    "{message}"
                );
            }
            other => panic!("expected detailed schema validation message, got {other:?}"),
        }
    }

    #[tokio::test]
    async fn wrong_tool_name_maps_to_schema_validation() {
        let server = MockServer::start().await;
        // 200 with a tool_use for a DIFFERENT tool name -> parser returns None.
        Mock::given(method("POST"))
            .and(path("/v1/messages"))
            .respond_with(ResponseTemplate::new(200).set_body_json(ok_messages_body(
                "some_other_tool",
                r#"{"caption":"c","tags":["t"],"scene":"","confidence":0.5}"#,
            )))
            .mount(&server)
            .await;
        let provider = provider_for(&server);
        let err = provider
            .describe_image(&test_image_png(), "", "", "", Some(&secret()))
            .await
            .expect_err("wrong tool name");
        assert_eq!(err, ProviderError::SchemaValidation);
    }

    #[tokio::test]
    async fn bearer_required_without_credential_errors() {
        let server = MockServer::start().await;
        Mock::given(method("POST"))
            .and(path("/v1/messages"))
            .respond_with(ResponseTemplate::new(200).set_body_json(ok_messages_body(
                UNDERSTANDING_SCHEMA_NAME,
                r#"{"caption":"c","tags":["t"],"scene":"","confidence":0.5}"#,
            )))
            .mount(&server)
            .await;
        let provider = provider_for(&server);
        let err = provider
            .describe_image(&test_image_png(), "", "", "", None)
            .await
            .expect_err("missing credential");
        assert!(matches!(err, ProviderError::Provider(_)), "{err:?}");
        assert!(server.received_requests().await.unwrap().is_empty());
    }

    #[tokio::test]
    async fn api_key_header_mode_sends_x_api_key() {
        let server = MockServer::start().await;
        Mock::given(method("POST"))
            .and(path("/v1/messages"))
            .and(header("x-api-key", TEST_SECRET))
            .respond_with(ResponseTemplate::new(200).set_body_json(ok_messages_body(
                UNDERSTANDING_SCHEMA_NAME,
                r#"{"caption":"c","tags":["t"],"scene":"","confidence":0.5}"#,
            )))
            .mount(&server)
            .await;
        let mut config = load_provider_configs(None)
            .expect("built-ins load")
            .get("volcengine_ark_coding")
            .expect("coding built-in")
            .clone();
        config.base_url = server.uri();
        config.auth.auth_type = "api_key_header".to_string();
        let provider = AnthropicMessagesProvider::new(config).expect("provider builds");

        let out = provider
            .describe_image(&test_image_png(), "", "", "", Some(&secret()))
            .await
            .expect("describe ok");
        assert_eq!(out.caption, "c");

        let reqs = server.received_requests().await.expect("requests captured");
        assert_eq!(reqs.len(), 1);
        assert_eq!(
            reqs[0].headers.get("x-api-key").unwrap().to_str().unwrap(),
            TEST_SECRET,
            "x-api-key header value"
        );
        assert!(
            reqs[0].headers.get("authorization").is_none(),
            "authorization present in api_key_header mode"
        );
    }

    #[test]
    fn no_secret_image_prompt_or_body_in_logs_or_error_strings() {
        // Capture tracing output emitted during a call that triggers a retry (warn)
        // then a 400 with a raw body sentinel. A current_thread runtime drives the
        // whole call on THIS thread, so the thread-local capturing subscriber set by
        // `with_default` is in scope for every `warn!` the driver emits. A
        // multi_thread runtime could poll the future's continuation on a worker
        // thread where the subscriber is NOT set, leaving `captured` empty and the
        // no-leak assertions passing trivially (a false pass). The first assertion
        // confirms capture actually worked; the rest confirm nothing leaked.
        use std::io::Write;
        use std::sync::Mutex;
        use tracing_subscriber::fmt::MakeWriter;

        #[derive(Clone)]
        struct BufWriter(Arc<Mutex<Vec<u8>>>);
        impl<'a> MakeWriter<'a> for BufWriter {
            type Writer = BufWriteImpl;
            fn make_writer(&'a self) -> Self::Writer {
                BufWriteImpl {
                    inner: self.0.clone(),
                }
            }
        }
        struct BufWriteImpl {
            inner: Arc<Mutex<Vec<u8>>>,
        }
        impl Write for BufWriteImpl {
            fn write(&mut self, b: &[u8]) -> std::io::Result<usize> {
                self.inner.lock().unwrap().extend_from_slice(b);
                Ok(b.len())
            }
            fn flush(&mut self) -> std::io::Result<()> {
                Ok(())
            }
        }

        let buf = Arc::new(Mutex::new(Vec::new()));
        let subscriber = tracing_subscriber::fmt()
            .with_writer(BufWriter(buf.clone()))
            .with_ansi(false)
            .with_env_filter("warn")
            .finish();

        let img = test_image_png();
        let (_, img_b64) = build_image_base64(&img).expect("encode image");

        let rt = tokio::runtime::Builder::new_current_thread()
            .enable_all()
            .build()
            .expect("current_thread runtime");
        let err = tracing::subscriber::with_default(subscriber, || {
            rt.block_on(async {
                let server = MockServer::start().await;
                Mock::given(method("POST"))
                    .and(path("/v1/messages"))
                    .respond_with(ResponseTemplate::new(500))
                    .up_to_n_times(1)
                    .mount(&server)
                    .await;
                Mock::given(method("POST"))
                    .and(path("/v1/messages"))
                    .respond_with(
                        ResponseTemplate::new(400)
                            .set_body_json(json!({ "type": "error", "error": { "type": "x", "message": RAW_BODY_SENTINEL } })),
                    )
                    .mount(&server)
                    .await;
                let provider = provider_for(&server);
                provider
                    .describe_image(&img, "", "profile-1", "", Some(&secret()))
                    .await
            })
        });
        let err = err.expect_err("400 after retry");

        let captured = String::from_utf8_lossy(&buf.lock().unwrap()).to_string();
        // Capture worked: the retry warn was emitted and reached the buffer.
        assert!(
            captured.contains("retrying"),
            "log capture did not work (no retry warn captured): {captured}"
        );
        assert!(
            !captured.contains(TEST_SECRET),
            "secret in logs: {captured}"
        );
        assert!(
            !captured.contains(img_b64.as_str()),
            "image base64 in logs: {captured}"
        );
        assert!(
            !captured.contains(TEST_PROMPT),
            "prompt in logs: {captured}"
        );
        assert!(
            !captured.contains(RAW_BODY_SENTINEL),
            "raw body in logs: {captured}"
        );
        assert!(
            !err.to_string().contains(TEST_SECRET),
            "secret in error: {err}"
        );
        assert!(
            !err.to_string().contains(RAW_BODY_SENTINEL),
            "raw body in error: {err}"
        );
    }

    #[tokio::test]
    async fn cancellation_drops_in_flight_request() {
        use crate::proto::alcedo::ai::{
            AiErrorCode, AiPriority, AiRequestHeader, AiResponseStatus, DescribeImageRequest,
            RenditionMetadata as ProtoRendition,
        };
        use crate::server::image_analysis::ImageAnalysisServiceImpl;
        use crate::service::cancellation_registry::CancellationRegistry;
        use crate::service::credential_vault::CredentialVault;

        let server = MockServer::start().await;
        Mock::given(method("POST"))
            .and(path("/v1/messages"))
            .respond_with(
                ResponseTemplate::new(200)
                    .set_body_json(ok_messages_body(
                        UNDERSTANDING_SCHEMA_NAME,
                        r#"{"caption":"c","tags":["t"],"scene":"","confidence":0.5}"#,
                    ))
                    .set_delay(std::time::Duration::from_secs(2)),
            )
            .mount(&server)
            .await;

        let provider = provider_for(&server);
        let vault = Arc::new(CredentialVault::new(None));
        let cancel_registry = Arc::new(CancellationRegistry::new());
        let mut providers: HashMap<String, Arc<dyn ImageAnalysisProvider>> = HashMap::new();
        let pid = provider.provider_id().to_string();
        providers.insert(pid.clone(), Arc::new(provider));
        let svc =
            ImageAnalysisServiceImpl::new(providers, pid, vault.clone(), cancel_registry.clone());

        let handle = vault.register("volcengine_ark_coding", TEST_SECRET.to_string(), None);
        let request_id = "req-cancel-ark-coding".to_string();
        let req = DescribeImageRequest {
            header: Some(AiRequestHeader {
                request_id: request_id.clone(),
                task_id: "image_understanding.describe".to_string(),
                timeout_ms: 60_000,
                priority: AiPriority::Normal as i32,
                trace_id: String::new(),
                credential_ref: handle,
                client_capabilities: vec![],
            }),
            image_bytes: test_image_png(),
            image_format_hint: "image/png".to_string(),
            rendition: Some(ProtoRendition {
                kind: "preview".to_string(),
                width: 2,
                height: 2,
                bytes: 64,
            }),
            provider_id: "volcengine_ark_coding".to_string(),
            model_id: String::new(),
            prompt_profile_id: String::new(),
            output_language: String::new(),
        };

        let cancel_registry2 = cancel_registry.clone();
        let rid = request_id.clone();
        tokio::spawn(async move {
            tokio::time::sleep(std::time::Duration::from_millis(50)).await;
            cancel_registry2.cancel(&rid);
        });

        let resp = svc
            .describe_image(tonic::Request::new(req))
            .await
            .expect("rpc ok");
        let inner = resp.into_inner();
        let h = inner.header.expect("header present");
        assert_eq!(h.status, AiResponseStatus::AiStatusCancelled as i32);
        assert_eq!(h.error_code, AiErrorCode::AiErrorCancelledByClient as i32);
        assert!(inner.result.is_none());
    }

    #[tokio::test]
    async fn timeout_returns_deadline_exceeded() {
        use crate::proto::alcedo::ai::{
            AiErrorCode, AiPriority, AiRequestHeader, AiResponseStatus, DescribeImageRequest,
            RenditionMetadata as ProtoRendition,
        };
        use crate::server::image_analysis::ImageAnalysisServiceImpl;
        use crate::service::cancellation_registry::CancellationRegistry;
        use crate::service::credential_vault::CredentialVault;

        let server = MockServer::start().await;
        Mock::given(method("POST"))
            .and(path("/v1/messages"))
            .respond_with(
                ResponseTemplate::new(200)
                    .set_body_json(ok_messages_body(
                        UNDERSTANDING_SCHEMA_NAME,
                        r#"{"caption":"c","tags":["t"],"scene":"","confidence":0.5}"#,
                    ))
                    .set_delay(std::time::Duration::from_secs(2)),
            )
            .mount(&server)
            .await;

        let provider = provider_for(&server);
        let vault = Arc::new(CredentialVault::new(None));
        let cancel_registry = Arc::new(CancellationRegistry::new());
        let mut providers: HashMap<String, Arc<dyn ImageAnalysisProvider>> = HashMap::new();
        let pid = provider.provider_id().to_string();
        providers.insert(pid.clone(), Arc::new(provider));
        let svc =
            ImageAnalysisServiceImpl::new(providers, pid, vault.clone(), cancel_registry.clone());

        let handle = vault.register("volcengine_ark_coding", TEST_SECRET.to_string(), None);
        let req = DescribeImageRequest {
            header: Some(AiRequestHeader {
                request_id: "req-timeout-ark-coding".into(),
                task_id: "image_understanding.describe".into(),
                timeout_ms: 80,
                priority: AiPriority::Normal as i32,
                trace_id: String::new(),
                credential_ref: handle,
                client_capabilities: vec![],
            }),
            image_bytes: test_image_png(),
            image_format_hint: "image/png".to_string(),
            rendition: Some(ProtoRendition {
                kind: "preview".to_string(),
                width: 2,
                height: 2,
                bytes: 64,
            }),
            provider_id: "volcengine_ark_coding".to_string(),
            model_id: String::new(),
            prompt_profile_id: String::new(),
            output_language: String::new(),
        };

        let resp = svc
            .describe_image(tonic::Request::new(req))
            .await
            .expect("rpc ok (timeout in header)");
        let inner = resp.into_inner();
        let h = inner.header.expect("header present");
        assert_eq!(h.status, AiResponseStatus::AiStatusDeadlineExceeded as i32);
        assert_eq!(h.error_code, AiErrorCode::AiErrorProviderTimeout as i32);
        assert!(inner.result.is_none());
    }

    // --- Phase 6b: Opencode-compatible base URL / endpoint --------------------
    //
    // The driver is generic Anthropic Messages — these tests prove it accepts an
    // Opencode-compatible base URL + endpoint (`https://opencode.ai/zen/go/v1` +
    // `/messages`) from config, captures the provider request id from the
    // `request-id` response header (Opencode echoes a header, not a body `id`),
    // and stays fail-closed when the endpoint ignores tool-use. The Coding Plan
    // tests above cover the body-`id` path; these cover the header path.

    fn opencode_provider_for(server: &MockServer) -> AnthropicMessagesProvider {
        let mut config = load_provider_configs(None)
            .expect("built-ins load")
            .get("opencode_go_anthropic")
            .expect("opencode_go_anthropic built-in")
            .clone();
        config.base_url = server.uri();
        // OpenCode built-ins are already structured-output capable; these tests
        // exercise the wire shape against a mock endpoint.
        AnthropicMessagesProvider::new(config).expect("provider builds")
    }

    #[tokio::test]
    async fn opencode_base_url_builds_messages_request_with_tool_use() {
        let server = MockServer::start().await;
        Mock::given(method("POST"))
            .and(path("/messages"))
            .and(header("x-api-key", TEST_SECRET))
            .and(header("anthropic-version", ANTHROPIC_VERSION))
            .respond_with(ResponseTemplate::new(200).set_body_json(ok_messages_body(
                UNDERSTANDING_SCHEMA_NAME,
                r#"{"caption":"c","tags":["t"],"scene":"","confidence":0.5}"#,
            )))
            .mount(&server)
            .await;

        let provider = opencode_provider_for(&server);
        provider
            .describe_image(&test_image_png(), "qwen3.7-plus", "", "", Some(&secret()))
            .await
            .expect("describe ok");

        let reqs = server.received_requests().await.expect("requests captured");
        assert_eq!(reqs.len(), 1);
        // The Opencode endpoint is `/messages` (not the Coding Plan `/v1/messages`).
        assert_eq!(reqs[0].url.path(), "/messages");
        let body: Value = serde_json::from_slice(&reqs[0].body).expect("body json");
        assert_eq!(body["model"], "qwen3.7-plus");
        // Anthropic image block: source.type=base64, media_type, raw base64 data.
        let img = &body["messages"][0]["content"][0];
        assert_eq!(img["type"], "image");
        assert_eq!(img["source"]["type"], "base64");
        assert_eq!(img["source"]["media_type"], "image/png");
        let data = img["source"]["data"].as_str().expect("data string");
        assert!(
            !data.starts_with("data:"),
            "image data is a data URI: {data}"
        );
        // Structured output via tools + tool_choice, not response_format.
        assert_eq!(body["tools"][0]["name"], "alcedo_image_understanding");
        assert_eq!(body["tools"][0]["input_schema"]["type"], "object");
        assert_eq!(
            body["tools"][0]["input_schema"]["additionalProperties"],
            false
        );
        assert_eq!(body["tool_choice"]["type"], "tool");
        assert_eq!(body["tool_choice"]["name"], "alcedo_image_understanding");
        // No OpenRouter / OpenAI-Chat fields leak in.
        assert!(
            body.get("provider").is_none(),
            "OpenRouter `provider` object present"
        );
        assert!(
            body.get("response_format").is_none(),
            "OpenAI `response_format` present"
        );
        // OpenCode's Anthropic-compatible /messages endpoint expects Anthropic
        // x-api-key auth; bearer returns a fast 401 "Missing API key".
        assert!(
            reqs[0].headers.get("authorization").is_none(),
            "authorization present in api_key_header mode"
        );
    }

    #[tokio::test]
    async fn opencode_extracts_tool_use_and_captures_request_id_header() {
        // Opencode echoes the provider request id as a `request-id` response header
        // (the config sets `provider_request_id_header: "request-id"`,
        // `provider_request_id_json_pointer: null`). The driver must capture it
        // from the header, not the body.
        let server = MockServer::start().await;
        Mock::given(method("POST"))
            .and(path("/messages"))
            .respond_with(
                ResponseTemplate::new(200)
                    .set_body_json(ok_messages_body(
                        UNDERSTANDING_SCHEMA_NAME,
                        r#"{"caption":"a small image","tags":["test"],"scene":"studio","confidence":0.7}"#,
                    ))
                    .insert_header("request-id", "opencode-hdr-42"),
            )
            .mount(&server)
            .await;
        let provider = opencode_provider_for(&server);
        let out = provider
            .describe_image(&test_image_png(), "", "", "", Some(&secret()))
            .await
            .expect("describe ok");
        assert_eq!(out.caption, "a small image");
        assert_eq!(out.tags, vec!["test".to_string()]);
        // Captured from the `request-id` header, not a body field.
        assert_eq!(out.provider_request_id, "opencode-hdr-42");
        validate_understanding(&out).expect("canned outcome validates");
    }

    #[tokio::test]
    async fn opencode_missing_tool_use_maps_to_schema_validation() {
        // Explicit "unsupported structured output" fail-closed path on the Opencode
        // endpoint: a 200 that returns only a text block (endpoint ignored
        // tool_choice) must not create an active annotation.
        let server = MockServer::start().await;
        Mock::given(method("POST"))
            .and(path("/messages"))
            .respond_with(ResponseTemplate::new(200).set_body_json(json!({
                "id": "opencode-resp-x",
                "type": "message",
                "role": "assistant",
                "content": [ { "type": "text", "text": "I cannot use a tool here." } ]
            })))
            .mount(&server)
            .await;
        let provider = opencode_provider_for(&server);
        let err = provider
            .describe_image(&test_image_png(), "", "", "", Some(&secret()))
            .await
            .expect_err("no tool_use");
        match err {
            ProviderError::SchemaValidationMessage(message) => {
                assert!(
                    message.contains("tool_use input named alcedo_image_understanding"),
                    "{message}"
                );
            }
            other => panic!("expected detailed schema validation message, got {other:?}"),
        }
    }

    #[tokio::test]
    async fn opencode_client_4xx_redacts_raw_body() {
        // A 4xx on the Opencode endpoint is not retried, maps to
        // ProviderError::Provider, and the raw provider error body must NOT surface
        // in the error string (redaction). The full log-capture redaction test
        // (`no_secret_image_prompt_or_body_in_logs_or_error_strings`) covers the
        // shared `send_with_retry` path with the Coding Plan config; this asserts
        // the Opencode path likewise does not leak the raw body in the error.
        let server = MockServer::start().await;
        Mock::given(method("POST"))
            .and(path("/messages"))
            .respond_with(ResponseTemplate::new(400).set_body_json(json!({
                "type": "error",
                "error": { "type": "invalid_request_error", "message": RAW_BODY_SENTINEL }
            })))
            .mount(&server)
            .await;
        let provider = opencode_provider_for(&server);
        let err = provider
            .describe_image(&test_image_png(), "", "", "", Some(&secret()))
            .await
            .expect_err("400 not retried");
        assert!(matches!(err, ProviderError::Provider(_)), "{err:?}");
        assert_eq!(server.received_requests().await.unwrap().len(), 1);
        assert!(
            !err.to_string().contains(RAW_BODY_SENTINEL),
            "raw body leaked: {err}"
        );
        assert!(
            !err.to_string().contains(TEST_SECRET),
            "secret in error: {err}"
        );
    }

    // ----- Phase 6c: model_id tightening + model discovery -----

    #[tokio::test]
    async fn unknown_explicit_model_id_fails_before_http_anthropic() {
        let server = MockServer::start().await;
        let provider = opencode_provider_for(&server);
        let err = provider
            .describe_image(
                &test_image_png(),
                "not-a-real-anthropic-model",
                "",
                "",
                Some(&secret()),
            )
            .await
            .expect_err("unknown model id should fail closed");
        assert_eq!(
            err,
            ProviderError::UnknownModel("not-a-real-anthropic-model".to_string())
        );
        assert_eq!(server.received_requests().await.unwrap().len(), 0);
    }

    /// Phase 6c: Anthropic-compatible discovery follows `has_more` + `last_id`
    /// pagination and merges all pages into unverified candidates.
    #[tokio::test]
    async fn list_models_anthropic_opencode_x_api_key_parses_and_paginates() {
        let server = MockServer::start().await;
        // Page 1 (no after_id): has_more=true, last_id=claude-a.
        Mock::given(method("GET"))
            .and(path("/models"))
            .and(header("x-api-key", TEST_SECRET))
            .respond_with(ResponseTemplate::new(200).set_body_json(json!({
                "data": [ { "id": "claude-a", "display_name": "Claude A" } ],
                "has_more": true,
                "last_id": "claude-a",
                "first_id": "claude-a"
            })))
            .up_to_n_times(1)
            .mount(&server)
            .await;
        // Page 2 (after_id=claude-a): has_more=false.
        Mock::given(method("GET"))
            .and(path("/models"))
            .and(query_param("after_id", "claude-a"))
            .and(header("x-api-key", TEST_SECRET))
            .respond_with(ResponseTemplate::new(200).set_body_json(json!({
                "data": [ { "id": "claude-b" } ],
                "has_more": false,
                "last_id": "claude-b"
            })))
            .mount(&server)
            .await;
        let provider = opencode_provider_for(&server);

        let models = provider
            .list_models(Some(&secret()))
            .await
            .expect("list ok");
        assert_eq!(models.len(), 2);
        assert_eq!(models[0].model_id, "claude-a");
        assert_eq!(models[0].display_name, "Claude A");
        assert_eq!(models[1].model_id, "claude-b");
        assert_eq!(models[1].display_name, "claude-b"); // falls back to id
        assert_eq!(models[0].source_provider_id, "opencode_go_anthropic");
        // Two GETs, second carrying the cursor.
        let reqs = server.received_requests().await.unwrap();
        assert_eq!(reqs.len(), 2);
        assert_eq!(reqs[0].method, "GET");
        assert_eq!(reqs[1].method, "GET");
        assert!(reqs[0].headers.get("authorization").is_none());
        assert!(reqs[1].headers.get("authorization").is_none());
    }

    /// Phase 6c: `api_key_header` mode sends `x-api-key` + `anthropic-version`
    /// and NO `Authorization` for discovery, mirroring the task-call auth path.
    #[tokio::test]
    async fn list_models_anthropic_x_api_key_auth() {
        let server = MockServer::start().await;
        Mock::given(method("GET"))
            .and(path("/v1/models"))
            .and(header("x-api-key", TEST_SECRET))
            .and(header("anthropic-version", "2023-06-01"))
            .respond_with(ResponseTemplate::new(200).set_body_json(json!({
                "data": [ { "id": "claude-a" } ],
                "has_more": false
            })))
            .mount(&server)
            .await;
        let mut config = load_provider_configs(None)
            .expect("built-ins load")
            .get("volcengine_ark_coding")
            .expect("coding built-in")
            .clone();
        config.base_url = server.uri();
        config.endpoint = "/v1/messages".to_string();
        config.models_endpoint = Some("/v1/models".to_string());
        config.auth.auth_type = "api_key_header".to_string();
        let provider = AnthropicMessagesProvider::new(config).expect("provider builds");

        let models = provider
            .list_models(Some(&secret()))
            .await
            .expect("list ok");
        assert_eq!(models.len(), 1);
        let reqs = server.received_requests().await.unwrap();
        assert_eq!(reqs.len(), 1);
        assert!(
            reqs[0].headers.get("authorization").is_none(),
            "Authorization present in x-api-key mode"
        );
    }

    /// Phase 6c: a 401 on the model-list maps to a non-retryable `Provider`
    /// error; the raw body and secret are not leaked.
    #[tokio::test]
    async fn list_models_anthropic_4xx_redacts() {
        let server = MockServer::start().await;
        Mock::given(method("GET"))
            .and(path("/models"))
            .respond_with(ResponseTemplate::new(401).set_body_string(RAW_BODY_SENTINEL))
            .mount(&server)
            .await;
        let provider = opencode_provider_for(&server);

        let err = provider
            .list_models(Some(&secret()))
            .await
            .expect_err("401 fails");
        assert!(matches!(err, ProviderError::Provider(_)), "{err:?}");
        assert_eq!(server.received_requests().await.unwrap().len(), 1);
        assert!(
            !err.to_string().contains(TEST_SECRET),
            "secret in error: {err}"
        );
        assert!(
            !err.to_string().contains(RAW_BODY_SENTINEL),
            "raw body leaked: {err}"
        );
    }
}

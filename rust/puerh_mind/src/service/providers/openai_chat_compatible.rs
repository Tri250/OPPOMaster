//! Generic OpenAI Chat-compatible image-analysis driver (Phase 6b,
//! `openai_chat_compatible`).
//!
//! This is the protocol-family driver for any server that speaks the OpenAI
//! Chat Completions shape (`POST /chat/completions`): Opencode's OpenAI-compatible
//! endpoint (`https://opencode.ai/zen/go/v1/chat/completions`), OpenRouter, a
//! local compatible server, or the real OpenAI API. It builds an OpenAI Chat
//! request from the loaded provider config: an Alcedo-owned system prompt, a user
//! message carrying the selected image rendition as a data URI plus a task
//! instruction, and `response_format: { type: "json_schema", ... }` carrying the
//! code-owned (sanitized to strict-compatible) Alcedo schema. The response's
//! `choices[0].message.content` (or the config's `content_json_pointer`) is parsed
//! as JSON, validated + normalized against the code-owned understanding / rating
//! contract, and returned as typed fields.
//!
//! ## OpenRouter-specific knobs are optional, config-gated, off by default
//!
//! The driver is generic: OpenRouter's routing/attribution knobs are read from
//! the provider config and only emitted when the config sets them. The OpenRouter
//! built-in config sets `attribution_headers`, `structured_output.provider_require_parameters`,
//! and a per-model `data_collection = "deny"`; the Opencode built-in config sets
//! none of those, so for an Opencode preset the `provider` routing object is
//! omitted entirely and no attribution headers are sent — "disabled for Opencode by
//! default" is automatic from the config, not a code branch. Phase 6g may retire
//! the `openrouter_chat` driver id once this generic driver covers the same
//! contract; until then `OpenRouterChatProvider` is a type alias for this type.
//!
//! ## Auth, secrets, structured output
//!
//! The API key is resolved from the Rust credential vault per request and sent
//! only as `Authorization: Bearer <secret>`; `SecretString::expose()` is called at
//! the single header-build site and is never logged. `auth: none` is supported for
//! a local no-key compatible server. Structured output is mandatory: if the config
//! does not request it (`mode = "none"`) or the selected model does not support
//! it, the driver fails closed before any HTTP call. A compatible endpoint that
//! ignores `response_format` and returns non-JSON prose, or returns JSON that
//! violates the code-owned contract, maps to `ProviderError::SchemaValidation` so
//! the service creates no active annotation (Phase 6b "unsupported structured
//! output" fail-closed path). Transport / 429 / 5xx failures retry under the same
//! bounded policy as the other drivers; provider and transport errors map to
//! `ProviderError` variants the service translates into `AiResponseStatus` /
//! `AiErrorCode`.

use serde_json::{Value, json};
use std::sync::Mutex;
use tracing::warn;

use crate::service::credential_vault::SecretString;
use crate::service::image_analysis::{
    AnalyzeOutcome, DescribeOutcome, DiscoveredModel, IMAGE_ANALYSIS_FLAT_SCHEMA,
    IMAGE_RATING_SCHEMA, IMAGE_UNDERSTANDING_SCHEMA, ImageAnalysisProvider, ProviderError,
    ScoreOutcome, validate_analyze, validate_rating, validate_understanding,
};
use crate::service::provider_config::{ModelConfig, ProviderConfig};
use crate::service::providers::http_util::{
    MAX_TRANSIENT_RETRIES, build_image_data_uri, build_rustls_client, compact_json_excerpt,
    compact_text_excerpt, extract_provider_request_id, extract_usage, json_pointer_str,
    parse_content_json, parse_rating_int, read_response, send_get_with_retry, send_with_retry,
    strict_schema_value,
};

/// Schema names injected into `response_format.json_schema.name`. Kept stable and
/// Alcedo-namespaced so a provider's structured-output dashboard identifies the
/// contract unambiguously.
const UNDERSTANDING_SCHEMA_NAME: &str = "alcedo_image_understanding";
const RATING_SCHEMA_NAME: &str = "alcedo_image_rating";
const ANALYSIS_FLAT_SCHEMA_NAME: &str = "alcedo_image_analysis_flat";

pub struct OpenAiChatCompatibleProvider {
    config: ProviderConfig,
    http: reqwest::Client,
    /// Models committed from live `list_models` discovery so an explicitly
    /// requested discovered slug passes the Phase 6c `resolve_model` gate. Each
    /// entry is synthesized with `supports_structured_output = true` (discovery
    /// cannot prove it, but the gate requires it for the call to proceed; a model
    /// that truly lacks support fails at the provider HTTP call as a normal
    /// per-item error). Idempotent: a slug already in `config.models` or already
    /// committed is not duplicated.
    discovered_models: Mutex<Vec<ModelConfig>>,
}

impl OpenAiChatCompatibleProvider {
    /// Construct from a validated provider config, building a rustls-backed
    /// `reqwest::Client`. Used by `main.rs` for the shipped sidecar.
    pub fn new(config: ProviderConfig) -> Result<Self, ProviderError> {
        let http = build_rustls_client()?;
        Ok(Self {
            config,
            http,
            discovered_models: Mutex::new(Vec::new()),
        })
    }

    /// Construct with an injected HTTP client. Used by tests to point the driver
    /// at a local mock server; the rustls client built by `new` already speaks
    /// `http://127.0.0.1` so tests can also use `new` with a localhost base_url.
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
    /// `{base_url}/models` (the OpenAI-compatible default); a config
    /// `models_endpoint` override replaces the path.
    fn models_url(&self) -> String {
        let path = self.config.models_endpoint.as_deref().unwrap_or("/models");
        format!("{}{}", self.config.base_url, path)
    }

    /// Resolve the requested model slug (falling back to the config default) and
    /// the matching model entry (for per-model knobs) if present. Phase 6c: a
    /// non-empty explicit `requested` must resolve to an entry in
    /// `config.models[]` (built-in) or in the committed `discovered_models`
    /// (from a prior `list_models`); an unknown slug fails closed here, BEFORE
    /// any provider HTTP call, closing the Phase 6b review gap where an unlisted
    /// slug bypassed the `supports_structured_output` check. An empty `requested`
    /// uses the config default (config-authored, so it is "known" by virtue of
    /// being the default) and may yield `None` when the default is not itself
    /// listed. Returns an owned `ModelConfig` clone because committed discovered
    /// models live behind a `Mutex` whose guard cannot outlive the call.
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

    /// Commit discovered models so a later explicit `model_id` resolves. Each
    /// discovered id not already present in the static `config.models` or the
    /// committed list is appended as a synthesized `ModelConfig` with
    /// `supports_structured_output = true` (required by `ensure_structured_output`
    /// for the call to proceed). Idempotent. Discovery cannot prove vision or
    /// structured-output support, so a model lacking support fails at the
    /// provider HTTP call as a normal per-item error rather than being silently
    /// dropped.
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

    /// Fail closed if structured output is not requested or the selected model
    /// does not support it (the plan: do not rely on best-effort free-form JSON).
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

    /// Resolve the bearer token from the credential, or `None` for `auth: none`.
    /// The secret is `expose()`d only here, at the single call site that builds the
    /// `Authorization` header, and is never logged.
    fn bearer<'a>(
        &self,
        credential: Option<&'a SecretString>,
    ) -> Result<Option<&'a str>, ProviderError> {
        match (self.config.auth.auth_type.as_str(), credential) {
            ("none", _) => Ok(None),
            ("bearer", Some(s)) => Ok(Some(s.expose())),
            ("bearer", None) => Err(ProviderError::Provider(
                "bearer provider called without a credential".to_string(),
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

    /// Build the optional `provider` routing object: `require_parameters` when the
    /// config asks for it, plus `data_collection = "deny"` when the selected model
    /// declares the privacy-first knob. Both are OpenRouter-style knobs that an
    /// Opencode / plain OpenAI-compatible config does not set, so the object is
    /// empty and omitted for those presets (disabled by default).
    fn provider_knobs(&self, model: Option<&ModelConfig>) -> Value {
        let mut m = serde_json::Map::new();
        if self.config.structured_output.provider_require_parameters {
            m.insert("require_parameters".into(), Value::Bool(true));
        }
        if let Some(md) = model {
            if md.data_collection.as_deref() == Some("deny") {
                m.insert("data_collection".into(), Value::String("deny".into()));
            }
        }
        Value::Object(m)
    }

    fn build_chat_body(
        &self,
        slug: &str,
        model: Option<&ModelConfig>,
        data_uri: &str,
        schema: Value,
        schema_name: &str,
        system: &str,
        instruction: &str,
    ) -> Value {
        let messages = json!([
            { "role": "system", "content": system },
            { "role": "user", "content": [
                { "type": "text", "text": instruction },
                { "type": "image_url", "image_url": { "url": data_uri } }
            ]}
        ]);
        let mut body = json!({
            "model": slug,
            "messages": messages,
            "stream": false,
            "temperature": self.config.defaults.temperature,
            "max_tokens": self.config.limits.max_output_tokens,
            "response_format": {
                "type": "json_schema",
                "json_schema": {
                    "name": schema_name,
                    "strict": self.config.structured_output.strict,
                    "schema": schema
                }
            }
        });
        let provider = self.provider_knobs(model);
        // Omit `provider` entirely when empty so a plain OpenAI-compatible /
        // Opencode body matches the OpenAI Chat shape (no OpenRouter routing
        // object). The OpenRouter config sets the knobs; others do not.
        let is_empty = provider.as_object().map(|o| o.is_empty()).unwrap_or(false);
        if !is_empty {
            body["provider"] = provider;
        }
        body
    }

    fn parse_describe(
        &self,
        body: &Value,
        model_id: &str,
        header_req_id: &str,
    ) -> Result<DescribeOutcome, ProviderError> {
        let content_pointer = self
            .config
            .response
            .content_json_pointer
            .as_deref()
            .unwrap_or("/choices/0/message/content");
        let content = json_pointer_str(body, content_pointer).ok_or_else(|| {
            ProviderError::SchemaValidationMessage(format!(
                "provider response did not contain JSON content at pointer {content_pointer}; expected OpenAI-compatible choices[0].message.content"
            ))
        })?;
        let parsed = match content {
            Value::String(s) => parse_content_json(s)?,
            other => other.clone(),
        };
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
        let content_pointer = self
            .config
            .response
            .content_json_pointer
            .as_deref()
            .unwrap_or("/choices/0/message/content");
        let content = json_pointer_str(body, content_pointer).ok_or_else(|| {
            ProviderError::SchemaValidationMessage(format!(
                "provider response did not contain JSON content at pointer {content_pointer}; expected OpenAI-compatible choices[0].message.content"
            ))
        })?;
        let parsed = match content {
            Value::String(s) => parse_content_json(s)?,
            other => other.clone(),
        };
        // The remote LLM is asked for a 1..=5 integer star rating. Accept an exact
        // integer, or an integer-valued float a model may emit despite the
        // `integer` schema (e.g. `4.0`); a fractional float (e.g. `4.9`) is
        // schema-invalid and is NOT truncated — `parse_rating_int` returns None,
        // the rating falls back to 0 (outside the 1..=5 contract), and
        // `validate_rating` rejects it (fail closed, no active annotation).
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
        let content_pointer = self
            .config
            .response
            .content_json_pointer
            .as_deref()
            .unwrap_or("/choices/0/message/content");
        let content = json_pointer_str(body, content_pointer).ok_or_else(|| {
            ProviderError::SchemaValidationMessage(format!(
                "provider response did not contain JSON content at pointer {content_pointer}; expected OpenAI-compatible choices[0].message.content"
            ))
        })?;
        let parsed = match content {
            Value::String(s) => parse_content_json(s)?,
            other => other.clone(),
        };
        if parsed.get("caption").is_none() {
            return Err(ProviderError::SchemaValidationMessage(
                "provider response did not contain caption".to_string(),
            ));
        }
        if parsed.get("tags").is_none() {
            return Err(ProviderError::SchemaValidationMessage(
                "provider response did not contain tags array".to_string(),
            ));
        }
        if !parsed.get("tags").is_some_and(|v| v.is_array()) {
            return Err(ProviderError::SchemaValidationMessage(
                "provider response tags was not an array; expected tags: [\"...\"]".to_string(),
            ));
        }
        if parsed.get("rating").is_none() {
            return Err(ProviderError::SchemaValidationMessage(
                "provider response did not contain rating".to_string(),
            ));
        }
        if parsed.get("rubric_id").is_none() {
            return Err(ProviderError::SchemaValidationMessage(
                "provider response did not contain rubric_id".to_string(),
            ));
        }
        let usage = extract_usage(
            self.config
                .response
                .usage_json_pointer
                .as_deref()
                .and_then(|p| json_pointer_str(body, p)),
        );
        let understanding = DescribeOutcome {
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
            usage: usage.clone(),
            provider_request_id: header_req_id.to_string(),
        };
        let rating = ScoreOutcome {
            rating: parsed.get("rating").and_then(parse_rating_int).unwrap_or(0),
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

    fn response_content_excerpt(&self, body: &Value) -> String {
        let content_pointer = self
            .config
            .response
            .content_json_pointer
            .as_deref()
            .unwrap_or("/choices/0/message/content");
        match json_pointer_str(body, content_pointer) {
            Some(Value::String(s)) => compact_text_excerpt(s, 1200),
            Some(other) => compact_json_excerpt(other, 1200),
            None => compact_json_excerpt(body, 1200),
        }
    }
}

fn describe_prompt(
    prompt_profile_id: &str,
    output_language: &str,
) -> Result<(String, String), ProviderError> {
    let prompt =
        crate::service::prompt_profiles::describe_prompt(prompt_profile_id, output_language)?;
    Ok((prompt.system, prompt.instruction))
}

fn score_prompt(
    prompt_profile_id: &str,
    rubric_id: &str,
    rating_severity: &str,
    output_language: &str,
    camera_context: &str,
) -> Result<(String, String), ProviderError> {
    let prompt = crate::service::prompt_profiles::score_prompt(
        prompt_profile_id,
        rubric_id,
        rating_severity,
        output_language,
        camera_context,
    )?;
    Ok((prompt.system, prompt.instruction))
}

fn analyze_prompt(
    prompt_profile_id: &str,
    rubric_id: &str,
    rating_severity: &str,
    output_language: &str,
    camera_context: &str,
) -> Result<(String, String), ProviderError> {
    let prompt = crate::service::prompt_profiles::analyze_prompt(
        prompt_profile_id,
        rubric_id,
        rating_severity,
        output_language,
        camera_context,
    )?;
    Ok((prompt.system, prompt.instruction))
}

#[tonic::async_trait]
impl ImageAnalysisProvider for OpenAiChatCompatibleProvider {
    fn provider_id(&self) -> &str {
        &self.config.provider_id
    }

    fn requires_credential(&self) -> bool {
        self.config.auth.auth_type != "none"
    }

    fn capability(&self) -> crate::proto::alcedo::ai::AiCapability {
        // Real providers advertise their capabilities via the provider registry
        // (Phase 5a `build_provider_capability_descriptors`), which emits one
        // descriptor per advertised model. This trait method is a compliance
        // fallback returning the default model's understanding descriptor; it is
        // not used to advertise a compatible provider to the C++ host.
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
        let bearer = self.bearer(credential)?;
        let data_uri = build_image_data_uri(image_bytes)?;
        let schema = strict_schema_value(IMAGE_UNDERSTANDING_SCHEMA)?;
        let (system, instruction) = describe_prompt(prompt_profile_id, output_language)?;
        let body = self.build_chat_body(
            &slug,
            model.as_ref(),
            &data_uri,
            schema,
            UNDERSTANDING_SCHEMA_NAME,
            &system,
            &instruction,
        );
        let resp = send_with_retry(
            &self.http,
            &self.url(),
            &body,
            &self.attribution_headers(),
            bearer,
            MAX_TRANSIENT_RETRIES,
        )
        .await?;
        let (headers, resp_body) = read_response(resp).await?;
        let header_req_id = extract_provider_request_id(
            &headers,
            &resp_body,
            self.config.response.provider_request_id_header.as_deref(),
            self.config
                .response
                .provider_request_id_json_pointer
                .as_deref(),
        );
        let provider_content = self.response_content_excerpt(&resp_body);
        let outcome = match self.parse_describe(&resp_body, &slug, &header_req_id) {
            Ok(outcome) => outcome,
            Err(err) => {
                warn!(
                    provider = %self.config.provider_id,
                    model = %slug,
                    provider_request_id = %header_req_id,
                    provider_content = %provider_content,
                    error = %err,
                    "DescribeImage provider response parse failed"
                );
                return Err(err);
            }
        };
        warn!(
            provider = %self.config.provider_id,
            model = %slug,
            provider_request_id = %outcome.provider_request_id,
            caption = %compact_text_excerpt(&outcome.caption, 240),
            tags = ?outcome.tags,
            provider_content = %provider_content,
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
        camera_context: &str,
        credential: Option<&SecretString>,
    ) -> Result<ScoreOutcome, ProviderError> {
        let (slug, model) = self.resolve_model(model_id)?;
        self.ensure_structured_output(model.as_ref())?;
        let bearer = self.bearer(credential)?;
        let data_uri = build_image_data_uri(image_bytes)?;
        let schema = strict_schema_value(IMAGE_RATING_SCHEMA)?;
        let (system, instruction) = score_prompt(
            prompt_profile_id,
            rubric_id,
            rating_severity,
            output_language,
            camera_context,
        )?;
        let body = self.build_chat_body(
            &slug,
            model.as_ref(),
            &data_uri,
            schema,
            RATING_SCHEMA_NAME,
            &system,
            &instruction,
        );
        let resp = send_with_retry(
            &self.http,
            &self.url(),
            &body,
            &self.attribution_headers(),
            bearer,
            MAX_TRANSIENT_RETRIES,
        )
        .await?;
        let (headers, resp_body) = read_response(resp).await?;
        let header_req_id = extract_provider_request_id(
            &headers,
            &resp_body,
            self.config.response.provider_request_id_header.as_deref(),
            self.config
                .response
                .provider_request_id_json_pointer
                .as_deref(),
        );
        let provider_content = self.response_content_excerpt(&resp_body);
        let outcome = match self.parse_score(&resp_body, &slug, &header_req_id) {
            Ok(outcome) => outcome,
            Err(err) => {
                warn!(
                    provider = %self.config.provider_id,
                    model = %slug,
                    provider_request_id = %header_req_id,
                    provider_content = %provider_content,
                    error = %err,
                    "ScoreImage provider response parse failed"
                );
                return Err(err);
            }
        };
        warn!(
            provider = %self.config.provider_id,
            model = %slug,
            provider_request_id = %outcome.provider_request_id,
            rating = outcome.rating,
            reasons = %compact_text_excerpt(&outcome.reasons, 360),
            provider_content = %provider_content,
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
        camera_context: &str,
        credential: Option<&SecretString>,
    ) -> Result<AnalyzeOutcome, ProviderError> {
        let (slug, model) = self.resolve_model(model_id)?;
        self.ensure_structured_output(model.as_ref())?;
        let bearer = self.bearer(credential)?;
        let data_uri = build_image_data_uri(image_bytes)?;
        let schema = strict_schema_value(IMAGE_ANALYSIS_FLAT_SCHEMA)?;
        let (system, instruction) = analyze_prompt(
            prompt_profile_id,
            rubric_id,
            rating_severity,
            output_language,
            camera_context,
        )?;
        let body = self.build_chat_body(
            &slug,
            model.as_ref(),
            &data_uri,
            schema,
            ANALYSIS_FLAT_SCHEMA_NAME,
            &system,
            &instruction,
        );
        let resp = send_with_retry(
            &self.http,
            &self.url(),
            &body,
            &self.attribution_headers(),
            bearer,
            MAX_TRANSIENT_RETRIES,
        )
        .await?;
        let (headers, resp_body) = read_response(resp).await?;
        let header_req_id = extract_provider_request_id(
            &headers,
            &resp_body,
            self.config.response.provider_request_id_header.as_deref(),
            self.config
                .response
                .provider_request_id_json_pointer
                .as_deref(),
        );
        let provider_content = self.response_content_excerpt(&resp_body);
        let outcome = match self.parse_analyze(&resp_body, &slug, &header_req_id) {
            Ok(outcome) => outcome,
            Err(err) => {
                warn!(
                    provider = %self.config.provider_id,
                    model = %slug,
                    provider_request_id = %header_req_id,
                    provider_content = %provider_content,
                    error = %err,
                    "AnalyzeImage provider response parse failed"
                );
                return Err(err);
            }
        };
        let caption = outcome
            .understanding
            .as_ref()
            .map(|u| compact_text_excerpt(&u.caption, 240))
            .unwrap_or_default();
        let rating = outcome.rating.as_ref().map(|r| r.rating).unwrap_or(0);
        let reasons = outcome
            .rating
            .as_ref()
            .map(|r| compact_text_excerpt(&r.reasons, 360))
            .unwrap_or_default();
        warn!(
            provider = %self.config.provider_id,
            model = %slug,
            provider_request_id = %outcome.provider_request_id,
            caption = %caption,
            rating,
            reasons = %reasons,
            provider_content = %provider_content,
            "AnalyzeImage completed"
        );
        Ok(outcome)
    }

    /// Phase 6c: discover models by listing the configured OpenAI-compatible
    /// `/models` endpoint. The standard OpenAI shape is `{ "data": [{ "id": ...
    /// }] }` with no pagination; a non-`data`-array body fails closed as
    /// `SchemaValidation`. Discovered models are unverified candidates — no
    /// capability flags are inferred (listing proves only that the endpoint can
    /// see the id, not image/structured-output support).
    async fn list_models(
        &self,
        credential: Option<&SecretString>,
    ) -> Result<Vec<DiscoveredModel>, ProviderError> {
        let bearer = self.bearer(credential)?;
        let resp = send_get_with_retry(
            &self.http,
            &self.models_url(),
            &self.attribution_headers(),
            bearer,
            MAX_TRANSIENT_RETRIES,
        )
        .await?;
        let (_headers, body) = read_response(resp).await?;
        let data = body
            .get("data")
            .and_then(|d| d.as_array())
            .ok_or(ProviderError::SchemaValidation)?;
        let mut out = Vec::with_capacity(data.len());
        for item in data {
            let Some(id) = item.get("id").and_then(|v| v.as_str()) else {
                continue;
            };
            let display_name = item
                .get("display_name")
                .or_else(|| item.get("name"))
                .and_then(|v| v.as_str())
                .unwrap_or(id)
                .to_string();
            out.push(DiscoveredModel {
                model_id: id.to_string(),
                display_name,
                source_provider_id: self.config.provider_id.clone(),
            });
        }
        // Phase 6c+: commit the discovered candidates so a later explicit
        // `model_id` selecting one of them passes the `resolve_model` gate and
        // the `supports_structured_output` check. This makes "Test & Refresh
        // Models" immediately usable: a discovered model becomes selectable and
        // functional without a sidecar restart or a hand-authored user config.
        self.commit_discovered_models(&out);
        Ok(out)
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::service::credential_vault::SecretString;
    use crate::service::provider_config::load_provider_configs;
    use serde_json::{Value, json};
    use wiremock::matchers::{header, method, path};
    use wiremock::{Mock, MockServer, ResponseTemplate};

    const TEST_SECRET: &str = "opencode-test-key-DO-NOT-LEAK";
    const TEST_PROMPT: &str = "Describe this image for a photo library.";
    const RAW_BODY_SENTINEL: &str = "RAW_OPENAI_COMPAT_BODY_SENTINEL";

    fn test_image_png() -> Vec<u8> {
        let img = image::RgbImage::from_pixel(2, 2, image::Rgb([10, 20, 30]));
        let mut cursor = std::io::Cursor::new(Vec::new());
        image::DynamicImage::ImageRgb8(img)
            .write_to(&mut cursor, image::ImageFormat::Png)
            .expect("encode png");
        cursor.into_inner()
    }

    /// Load the `opencode_go_openai` built-in (OpenAI-compatible, no attribution
    /// headers, no `provider_require_parameters`, no `data_collection`) and point
    /// it at the mock server. This is the Phase 6b product-facing preset shape.
    fn provider_for(server: &MockServer) -> OpenAiChatCompatibleProvider {
        let mut config = load_provider_configs(None)
            .expect("built-ins load")
            .get("opencode_go_openai")
            .expect("opencode_go_openai built-in")
            .clone();
        config.base_url = server.uri();
        // OpenCode built-ins are already structured-output capable; these tests
        // exercise the wire shape against a mock endpoint.
        OpenAiChatCompatibleProvider::new(config).expect("provider builds")
    }

    fn ok_understanding_body(content_json: &str) -> Value {
        json!({
            "id": "opencode-req-123",
            "choices": [
                { "message": { "content": content_json } }
            ],
            "usage": { "prompt_tokens": 100, "completion_tokens": 40, "total_tokens": 140 }
        })
    }

    fn secret() -> SecretString {
        SecretString::new(TEST_SECRET.to_string())
    }

    #[tokio::test]
    async fn sends_bearer_authorization_without_attribution_or_routing_for_opencode() {
        // Opencode preset: bearer auth, but NO attribution headers and NO `provider`
        // routing object (OpenRouter knobs are off by default for Opencode).
        let server = MockServer::start().await;
        Mock::given(method("POST"))
            .and(path("/chat/completions"))
            .and(header("authorization", format!("Bearer {TEST_SECRET}")))
            .respond_with(ResponseTemplate::new(200).set_body_json(ok_understanding_body(
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
        assert_eq!(out.provider_request_id, "opencode-req-123");
        assert_eq!(out.usage.total_tokens, 140);

        let reqs = server.received_requests().await.expect("requests captured");
        assert_eq!(reqs.len(), 1);
        // No OpenRouter attribution headers for an Opencode preset.
        assert!(
            reqs[0].headers.get("http-referer").is_none(),
            "OpenRouter HTTP-Referer sent for Opencode preset"
        );
        assert!(
            reqs[0].headers.get("x-openrouter-title").is_none(),
            "OpenRouter X-OpenRouter-Title sent for Opencode preset"
        );
        let body: Value = serde_json::from_slice(&reqs[0].body).expect("body json");
        assert!(
            body.get("provider").is_none(),
            "OpenRouter `provider` routing object present for Opencode preset: {body}"
        );
    }

    #[tokio::test]
    async fn request_body_has_json_schema_response_format_and_image_data_uri() {
        let server = MockServer::start().await;
        Mock::given(method("POST"))
            .and(path("/chat/completions"))
            .respond_with(
                ResponseTemplate::new(200).set_body_json(ok_understanding_body(
                    r#"{"caption":"c","tags":["t"],"scene":"","confidence":0.5}"#,
                )),
            )
            .mount(&server)
            .await;

        let provider = provider_for(&server);
        provider
            .describe_image(&test_image_png(), "kimi-k2.7-code", "", "", Some(&secret()))
            .await
            .expect("describe ok");

        let reqs = server.received_requests().await.expect("requests captured");
        assert_eq!(reqs.len(), 1);
        let body: Value = serde_json::from_slice(&reqs[0].body).expect("body json");
        assert_eq!(body["model"], "kimi-k2.7-code");
        assert_eq!(body["stream"], false);
        assert_eq!(body["response_format"]["type"], "json_schema");
        assert_eq!(
            body["response_format"]["json_schema"]["name"],
            "alcedo_image_understanding"
        );
        assert_eq!(body["response_format"]["json_schema"]["strict"], true);
        let schema = &body["response_format"]["json_schema"]["schema"];
        assert_eq!(schema["type"], "object");
        assert_eq!(schema["additionalProperties"], false);
        let required: Vec<&str> = schema["required"]
            .as_array()
            .unwrap()
            .iter()
            .map(|v| v.as_str().unwrap())
            .collect();
        assert!(required.contains(&"caption"));
        assert!(required.contains(&"tags"));
        // The image is carried as a data URI in the user message.
        let img_url = &body["messages"][1]["content"][1]["image_url"]["url"];
        assert!(
            img_url
                .as_str()
                .unwrap()
                .starts_with("data:image/png;base64,")
        );
    }

    #[tokio::test]
    async fn parses_understanding_response_and_captures_usage() {
        let server = MockServer::start().await;
        Mock::given(method("POST"))
            .and(path("/chat/completions"))
            .respond_with(ResponseTemplate::new(200).set_body_json(ok_understanding_body(
                r#"{"caption":"sunrise over mountains","tags":["sunrise","mountain","landscape"],"scene":"outdoor","confidence":0.82}"#,
            )))
            .mount(&server)
            .await;
        let provider = provider_for(&server);
        let out = provider
            .describe_image(&test_image_png(), "", "", "", Some(&secret()))
            .await
            .expect("describe ok");
        assert_eq!(out.caption, "sunrise over mountains");
        assert_eq!(out.tags.len(), 3);
        assert_eq!(out.scene, "outdoor");
        assert!((out.confidence - 0.82).abs() < 1e-9);
        assert_eq!(out.model_id, "kimi-k2.7-code");
        assert_eq!(out.usage.input_tokens, 100);
        assert_eq!(out.usage.output_tokens, 40);
        assert_eq!(out.usage.total_tokens, 140);
        assert_eq!(out.provider_request_id, "opencode-req-123");
        validate_understanding(&out).expect("canned outcome validates");
    }

    #[tokio::test]
    async fn parses_rating_response_and_captures_usage() {
        let server = MockServer::start().await;
        let body = json!({
            "id": "opencode-req-456",
            "choices": [ { "message": { "content":
                r#"{"rating":4,"rubric_id":"alcedo-default-v1","rubric_version":"1","reasons":"good"}"# } } ],
            "usage": { "prompt_tokens": 80, "completion_tokens": 50, "total_tokens": 130 }
        });
        Mock::given(method("POST"))
            .and(path("/chat/completions"))
            .respond_with(ResponseTemplate::new(200).set_body_json(body))
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
                "",
                Some(&secret()),
            )
            .await
            .expect("score ok");
        assert_eq!(out.rating, 4);
        assert_eq!(out.rubric_id, "alcedo-default-v1");
        assert_eq!(out.usage.total_tokens, 130);
        assert_eq!(out.provider_request_id, "opencode-req-456");
        validate_rating(&out).expect("canned rating validates");
    }

    #[tokio::test]
    async fn analyze_image_uses_flat_schema_and_parses_flat_response() {
        let server = MockServer::start().await;
        let body = json!({
            "id": "opencode-req-flat",
            "choices": [ { "message": { "content":
                r#"{"caption":"c","tags":["t"],"scene":"","confidence":0.5,"rating":4,"rubric_id":"general","rubric_version":"","reasons":"solid"}"# } } ],
            "usage": { "prompt_tokens": 120, "completion_tokens": 60, "total_tokens": 180 }
        });
        Mock::given(method("POST"))
            .and(path("/chat/completions"))
            .respond_with(ResponseTemplate::new(200).set_body_json(body))
            .mount(&server)
            .await;
        let provider = provider_for(&server);

        let out = provider
            .analyze_image(
                &test_image_png(),
                "",
                "",
                "general",
                "normal",
                "",
                "",
                Some(&secret()),
            )
            .await
            .expect("flat analyze ok");

        validate_analyze(&out).expect("flat analyze outcome validates");
        assert_eq!(out.understanding.as_ref().unwrap().caption, "c");
        assert_eq!(out.rating.as_ref().unwrap().rating, 4);
        assert_eq!(out.provider_request_id, "opencode-req-flat");

        let reqs = server.received_requests().await.expect("requests captured");
        let req_body: Value = serde_json::from_slice(&reqs[0].body).expect("body json");
        assert_eq!(
            req_body["response_format"]["json_schema"]["name"],
            "alcedo_image_analysis_flat"
        );
        let schema = &req_body["response_format"]["json_schema"]["schema"];
        let required: Vec<&str> = schema["required"]
            .as_array()
            .unwrap()
            .iter()
            .map(|v| v.as_str().unwrap())
            .collect();
        assert!(required.contains(&"caption"));
        assert!(required.contains(&"tags"));
        assert!(required.contains(&"rating"));
        assert!(required.contains(&"rubric_id"));
        assert!(schema["properties"].get("understanding").is_none());
    }

    #[tokio::test]
    async fn rate_limit_maps_to_transient() {
        let server = MockServer::start().await;
        Mock::given(method("POST"))
            .and(path("/chat/completions"))
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
            .and(path("/chat/completions"))
            .respond_with(ResponseTemplate::new(500))
            .up_to_n_times(1)
            .mount(&server)
            .await;
        Mock::given(method("POST"))
            .and(path("/chat/completions"))
            .respond_with(
                ResponseTemplate::new(200).set_body_json(ok_understanding_body(
                    r#"{"caption":"c","tags":["t"],"scene":"","confidence":0.5}"#,
                )),
            )
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
        let server = MockServer::start().await;
        Mock::given(method("POST"))
            .and(path("/chat/completions"))
            .respond_with(
                ResponseTemplate::new(400)
                    .set_body_json(json!({ "error": { "message": RAW_BODY_SENTINEL } })),
            )
            .mount(&server)
            .await;
        let provider = provider_for(&server);
        let err = provider
            .describe_image(&test_image_png(), "", "", "", Some(&secret()))
            .await
            .expect_err("400 not retried");
        assert!(matches!(err, ProviderError::Provider(_)), "{err:?}");
        assert_eq!(server.received_requests().await.unwrap().len(), 1);
        // The raw provider body is NOT surfaced in the error string.
        assert!(
            !err.to_string().contains(RAW_BODY_SENTINEL),
            "raw body leaked: {err}"
        );
    }

    #[tokio::test]
    async fn ignored_response_format_produces_no_active_annotation() {
        // Explicit "unsupported structured output" fail-closed path: a compatible
        // endpoint that IGNORES `response_format` and returns plain prose (not JSON)
        // must not create an active annotation. The content string is non-JSON, so
        // `parse_content_json` maps it to `SchemaValidation`.
        let server = MockServer::start().await;
        Mock::given(method("POST"))
            .and(path("/chat/completions"))
            .respond_with(
                ResponseTemplate::new(200).set_body_json(ok_understanding_body(
                    "Sorry, I can only describe images in plain text.",
                )),
            )
            .mount(&server)
            .await;
        let provider = provider_for(&server);
        let err = provider
            .describe_image(&test_image_png(), "", "", "", Some(&secret()))
            .await
            .expect_err("ignored response_format rejected");
        assert_eq!(err, ProviderError::SchemaValidation);
    }

    #[tokio::test]
    async fn json_violating_contract_maps_to_schema_validation() {
        // Valid JSON but it violates the code-owned contract (empty caption) — the
        // validator rejects it; no active annotation.
        let server = MockServer::start().await;
        Mock::given(method("POST"))
            .and(path("/chat/completions"))
            .respond_with(
                ResponseTemplate::new(200).set_body_json(ok_understanding_body(
                    r#"{"caption":"","tags":["t"],"scene":"","confidence":0.5}"#,
                )),
            )
            .mount(&server)
            .await;
        let provider = provider_for(&server);
        let err = provider
            .describe_image(&test_image_png(), "", "", "", Some(&secret()))
            .await
            .expect_err("empty caption rejected");
        assert_eq!(err, ProviderError::SchemaValidation);
    }

    #[tokio::test]
    async fn bearer_required_without_credential_errors() {
        let server = MockServer::start().await;
        Mock::given(method("POST"))
            .and(path("/chat/completions"))
            .respond_with(
                ResponseTemplate::new(200).set_body_json(ok_understanding_body(
                    r#"{"caption":"c","tags":["t"],"scene":"","confidence":0.5}"#,
                )),
            )
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
    async fn provider_request_id_captured_from_response_header_when_configured() {
        // The Opencode OpenAI built-in uses the `/id` body pointer. Override the
        // config to use a response header instead, proving the header-capture path
        // (Phase 6b deliverable #4) works for the OpenAI-compatible driver.
        let server = MockServer::start().await;
        Mock::given(method("POST"))
            .and(path("/chat/completions"))
            .respond_with(
                ResponseTemplate::new(200)
                    .set_body_json(ok_understanding_body(
                        r#"{"caption":"c","tags":["t"],"scene":"","confidence":0.5}"#,
                    ))
                    .insert_header("request-id", "hdr-opencode-999"),
            )
            .mount(&server)
            .await;
        let mut config = load_provider_configs(None)
            .expect("built-ins load")
            .get("opencode_go_openai")
            .expect("opencode built-in")
            .clone();
        config.base_url = server.uri();
        config.models[0].supports_structured_output = true;
        config.response.provider_request_id_json_pointer = None;
        config.response.provider_request_id_header = Some("request-id".to_string());
        let provider = OpenAiChatCompatibleProvider::new(config).expect("provider builds");

        let out = provider
            .describe_image(&test_image_png(), "", "", "", Some(&secret()))
            .await
            .expect("describe ok");
        assert_eq!(out.provider_request_id, "hdr-opencode-999");
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
        use std::sync::{Arc, Mutex};
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

        let rt = tokio::runtime::Builder::new_current_thread()
            .enable_all()
            .build()
            .expect("current_thread runtime");
        let err = tracing::subscriber::with_default(subscriber, || {
            rt.block_on(async {
                let server = MockServer::start().await;
                Mock::given(method("POST"))
                    .and(path("/chat/completions"))
                    .respond_with(ResponseTemplate::new(500))
                    .up_to_n_times(1)
                    .mount(&server)
                    .await;
                Mock::given(method("POST"))
                    .and(path("/chat/completions"))
                    .respond_with(
                        ResponseTemplate::new(400)
                            .set_body_json(json!({ "error": { "message": RAW_BODY_SENTINEL } })),
                    )
                    .mount(&server)
                    .await;
                let provider = provider_for(&server);
                provider
                    .describe_image(&test_image_png(), "", "profile-1", "", Some(&secret()))
                    .await
            })
        });
        let err = err.expect_err("400 after retry");

        let captured = String::from_utf8_lossy(&buf.lock().unwrap()).to_string();
        assert!(
            captured.contains("retrying"),
            "log capture did not work (no retry warn captured): {captured}"
        );
        assert!(
            !captured.contains(TEST_SECRET),
            "secret in logs: {captured}"
        );
        assert!(
            !captured.contains("data:image/png;base64,"),
            "image in logs: {captured}"
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

    // ----- Phase 6c: model_id tightening + model discovery -----

    /// Phase 6c: a non-empty explicit `model_id` that is not present in
    /// `config.models[]` fails closed with `UnknownModel` BEFORE any provider
    /// HTTP call. Closes the Phase 6b review gap where an unlisted slug bypassed
    /// `supports_structured_output` and was forwarded verbatim.
    #[tokio::test]
    async fn unknown_explicit_model_id_fails_before_http() {
        let server = MockServer::start().await;
        // No mock mounted: any HTTP call would fail the test by being recorded.
        let provider = provider_for(&server);

        let err = provider
            .describe_image(
                &test_image_png(),
                "not-a-real-model-slug",
                "",
                "",
                Some(&secret()),
            )
            .await
            .expect_err("unknown model id should fail closed");
        assert_eq!(
            err,
            ProviderError::UnknownModel("not-a-real-model-slug".to_string())
        );
        // No request should have hit the server.
        assert_eq!(server.received_requests().await.unwrap().len(), 0);
    }

    /// Phase 6c: discovery lists the OpenAI-compatible `/models` shape and
    /// returns unverified candidates with no capability flags.
    #[tokio::test]
    async fn list_models_parses_openai_shape() {
        let server = MockServer::start().await;
        Mock::given(method("GET"))
            .and(path("/models"))
            .respond_with(ResponseTemplate::new(200).set_body_json(json!({
                "object": "list",
                "data": [
                    { "id": "kimi-k2.7-code", "object": "model", "owned_by": "openai" },
                    { "id": "kimi-k2.7-code-mini", "display_name": "Kimi K2.6" }
                ]
            })))
            .mount(&server)
            .await;
        let provider = provider_for(&server);

        let models = provider
            .list_models(Some(&secret()))
            .await
            .expect("list ok");
        assert_eq!(models.len(), 2);
        assert_eq!(models[0].model_id, "kimi-k2.7-code");
        assert_eq!(models[0].display_name, "kimi-k2.7-code"); // falls back to id
        assert_eq!(models[1].model_id, "kimi-k2.7-code-mini");
        assert_eq!(models[1].display_name, "Kimi K2.6"); // uses display_name
        assert_eq!(models[0].source_provider_id, "opencode_go_openai");
    }

    /// Phase 6c: discovery sends `Authorization: Bearer <secret>` and no
    /// attribution/routing for the Opencode preset.
    #[tokio::test]
    async fn list_models_sends_bearer_auth() {
        let server = MockServer::start().await;
        Mock::given(method("GET"))
            .and(path("/models"))
            .and(header("authorization", format!("Bearer {TEST_SECRET}")))
            .respond_with(ResponseTemplate::new(200).set_body_json(json!({ "data": [] })))
            .mount(&server)
            .await;
        let provider = provider_for(&server);

        provider
            .list_models(Some(&secret()))
            .await
            .expect("list ok");
        let reqs = server.received_requests().await.unwrap();
        assert_eq!(reqs.len(), 1);
        assert_eq!(reqs[0].method, "GET");
        // No OpenRouter attribution headers for the Opencode preset.
        let hmap: std::collections::HashMap<&str, &str> = reqs[0]
            .headers
            .iter()
            .map(|(k, v)| (k.as_str(), v.to_str().unwrap_or("")))
            .collect();
        assert!(!hmap.contains_key("http-referer"));
        assert!(!hmap.contains_key("x-openrouter-title"));
    }

    /// Phase 6c: a 401 is a non-retryable 4xx -> `Provider` error (auth failure
    /// surfaced to the host's validate-connection flow).
    #[tokio::test]
    async fn list_models_4xx_maps_to_provider_error() {
        let server = MockServer::start().await;
        Mock::given(method("GET"))
            .and(path("/models"))
            .respond_with(ResponseTemplate::new(401).set_body_string("unauthorized"))
            .mount(&server)
            .await;
        let provider = provider_for(&server);

        let err = provider
            .list_models(Some(&secret()))
            .await
            .expect_err("401 fails");
        match err {
            ProviderError::Provider(_) => {}
            other => panic!("expected Provider for 401, got {other:?}"),
        }
        // 4xx is not retried.
        assert_eq!(server.received_requests().await.unwrap().len(), 1);
    }

    /// Phase 6c: 429 then 200 — the transient status retries once and the
    /// final 200 succeeds (`MAX_TRANSIENT_RETRIES = 1` allows exactly one retry).
    #[tokio::test]
    async fn list_models_429_retries_then_succeeds() {
        let server = MockServer::start().await;
        Mock::given(method("GET"))
            .and(path("/models"))
            .respond_with(ResponseTemplate::new(429))
            .up_to_n_times(1)
            .mount(&server)
            .await;
        Mock::given(method("GET"))
            .and(path("/models"))
            .respond_with(ResponseTemplate::new(200).set_body_json(json!({ "data": [
                { "id": "kimi-k2.7-code" }
            ] })))
            .mount(&server)
            .await;
        let provider = provider_for(&server);

        let models = provider
            .list_models(Some(&secret()))
            .await
            .expect("list ok after retry");
        assert_eq!(models.len(), 1);
        assert_eq!(server.received_requests().await.unwrap().len(), 2);
    }

    /// Phase 6c: exhausted 5xx retries -> `Transient`; the secret never appears
    /// in the error string.
    #[tokio::test]
    async fn list_models_exhausted_5xx_is_transient_and_redacts() {
        let server = MockServer::start().await;
        Mock::given(method("GET"))
            .and(path("/models"))
            .respond_with(ResponseTemplate::new(500).set_body_string(RAW_BODY_SENTINEL))
            .mount(&server)
            .await;
        let provider = provider_for(&server);

        let err = provider
            .list_models(Some(&secret()))
            .await
            .expect_err("500 fails");
        assert_eq!(err, ProviderError::Transient);
        assert!(!err.to_string().contains(TEST_SECRET));
        assert!(!err.to_string().contains(RAW_BODY_SENTINEL));
    }

    /// Phase 6c: a non-`data`-array body fails closed as `SchemaValidation`.
    #[tokio::test]
    async fn list_models_non_data_body_is_schema_validation() {
        let server = MockServer::start().await;
        Mock::given(method("GET"))
            .and(path("/models"))
            .respond_with(ResponseTemplate::new(200).set_body_json(json!({ "models": "oops" })))
            .mount(&server)
            .await;
        let provider = provider_for(&server);

        let err = provider
            .list_models(Some(&secret()))
            .await
            .expect_err("bad shape");
        assert_eq!(err, ProviderError::SchemaValidation);
    }

    /// Phase 6c+: a discovered model is committed during `list_models`, so a
    /// later explicit `model_id` selecting it resolves (passes the
    /// `resolve_model` slug gate and the synthesized
    /// `supports_structured_output = true` check) and the call proceeds to HTTP.
    /// Before discovery, the same slug fails closed as `UnknownModel`.
    #[tokio::test]
    async fn list_models_commits_discovered_model_so_it_becomes_selectable() {
        let server = MockServer::start().await;
        Mock::given(method("GET"))
            .and(path("/models"))
            .respond_with(ResponseTemplate::new(200).set_body_json(json!({ "data": [
                { "id": "discovered-vision-model", "display_name": "Discovered Vision" }
            ] })))
            .mount(&server)
            .await;
        Mock::given(method("POST"))
            .and(path("/chat/completions"))
            .respond_with(
                ResponseTemplate::new(200).set_body_json(ok_understanding_body(
                    r#"{"caption":"c","tags":["t"],"scene":"","confidence":0.5}"#,
                )),
            )
            .mount(&server)
            .await;
        let provider = provider_for(&server);

        // Before discovery, the slug is unknown -> fail closed before HTTP.
        let err = provider
            .describe_image(
                &test_image_png(),
                "discovered-vision-model",
                "",
                "",
                Some(&secret()),
            )
            .await
            .expect_err("unknown slug fails before discovery");
        assert!(matches!(err, ProviderError::UnknownModel(_)));

        // Discovery commits the slug.
        let models = provider
            .list_models(Some(&secret()))
            .await
            .expect("list ok");
        assert_eq!(models.len(), 1);

        // After discovery, the same slug resolves and the call proceeds.
        let out = provider
            .describe_image(
                &test_image_png(),
                "discovered-vision-model",
                "",
                "",
                Some(&secret()),
            )
            .await
            .expect("discovered slug selectable after list_models");
        assert_eq!(out.caption, "c");
        // The committed slug is sent in the request body.
        let reqs = server.received_requests().await.expect("requests captured");
        let post = reqs
            .iter()
            .find(|r| r.method.as_str() == "POST")
            .expect("POST captured");
        let body: Value = serde_json::from_slice(&post.body).expect("body json");
        assert_eq!(body["model"], "discovered-vision-model");
    }

    /// Output language: a `zh` directive is appended to the system prompt and
    /// reaches the provider request body; English/default leaves the prompt as-is.
    #[tokio::test]
    async fn describe_image_zh_output_language_appends_directive() {
        let server = MockServer::start().await;
        Mock::given(method("POST"))
            .and(path("/chat/completions"))
            .respond_with(
                ResponseTemplate::new(200).set_body_json(ok_understanding_body(
                    r#"{"caption":"c","tags":["t"],"scene":"","confidence":0.5}"#,
                )),
            )
            .mount(&server)
            .await;
        let provider = provider_for(&server);
        provider
            .describe_image(&test_image_png(), "", "", "zh", Some(&secret()))
            .await
            .expect("describe ok");
        let reqs = server.received_requests().await.expect("requests captured");
        let body: Value = serde_json::from_slice(&reqs[0].body).expect("body json");
        let system = body["messages"][0]["content"]
            .as_str()
            .expect("system content");
        assert!(
            system.contains("Simplified Chinese"),
            "zh directive missing: {system}"
        );
    }
}

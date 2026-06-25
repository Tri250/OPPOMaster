//! Anthropic Messages image-analysis driver (Phase 5c follow-up,
//! `anthropic_messages`).
//!
//! Speaks the Anthropic Messages API (`POST /v1/messages`): an Alcedo-owned
//! `system` prompt, a single user message carrying the image as an Anthropic
//! `image` content block (`source.type = "base64"`, raw base64 + `media_type` —
//! NOT a data URI) plus a `text` task instruction, and **tool-use** for structured
//! output (`tools` + `tool_choice = { type: "tool", name }`) with the code-owned
//! (sanitized to Anthropic-strict-compatible) Alcedo schema as the tool
//! `input_schema`. Tool-use is more reliable than `response_format.json_schema`
//! for forcing a typed result, and does not depend on a proxy honoring
//! `json_schema`. The response's `content[].tool_use.input` object (for the
//! expected tool name) is extracted with a driver-owned typed parser
//! (`content_json_pointer` is null for this driver), validated + normalized
//! against the code-owned understanding / rating contract, and returned as typed
//! fields. Transient / 429 / 5xx failures retry under the same bounded policy as
//! the other drivers; provider and transport errors map to `ProviderError`
//! variants the service translates into `AiResponseStatus` / `AiErrorCode`.
//!
//! Auth is selected by `config.auth.auth_type`:
//! - `bearer` — `Authorization: Bearer <secret>` (Claude-Code-style drop-in; used
//!   by the shipped `volcengine_ark_coding` Coding Plan config).
//! - `api_key_header` — `x-api-key: <secret>` (the real Anthropic API convention).
//! - `none` — no credential. The `anthropic-version: 2023-06-01` header is always
//!   sent. The secret is resolved from the Rust credential vault per request and
//!   travels only as a header value; `SecretString::expose()` is called solely at
//!   the header-build site and the cloned `String` is dropped right after the call.
//!
//! The driver is generic Anthropic Messages — the same code targets the Volcengine
//! Ark **Coding Plan** (`https://ark.cn-beijing.volces.com/api/coding`), the real
//! Anthropic API (`api.anthropic.com`), and Anthropic models behind OpenRouter, by
//! `base_url`. The Coding Plan is just one config.
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
//! Coding Plan models are primarily coding / text models. Image analysis only
//! works if the selected model accepts image input. The live smoke is the
//! ground-truth check; if the configured `slug` rejects the image on the Coding
//! Plan, adjust `slug` to a confirmed vision-capable model — the driver itself is
//! model-agnostic.

use serde_json::{Value, json};
use tracing::warn;

use crate::service::credential_vault::SecretString;
use crate::service::image_analysis::{
    DescribeOutcome, ImageAnalysisProvider, ProviderError, ScoreDimension, ScoreOutcome,
    IMAGE_RATING_SCHEMA, IMAGE_UNDERSTANDING_SCHEMA, validate_rating, validate_understanding,
};
use crate::service::provider_config::{ModelConfig, ProviderConfig};
use crate::service::providers::http_util::{
    MAX_TRANSIENT_RETRIES, build_image_base64, build_rustls_client, extract_usage,
    json_pointer_str, send_with_retry, strict_schema_value,
};

const UNDERSTANDING_SCHEMA_NAME: &str = "alcedo_image_understanding";
const RATING_SCHEMA_NAME: &str = "alcedo_image_rating";
const ANTHROPIC_VERSION: &str = "2023-06-01";

pub struct AnthropicMessagesProvider {
    config: ProviderConfig,
    http: reqwest::Client,
}

impl AnthropicMessagesProvider {
    pub fn new(config: ProviderConfig) -> Result<Self, ProviderError> {
        let http = build_rustls_client()?;
        Ok(Self { config, http })
    }

    #[allow(dead_code)]
    pub fn with_client(config: ProviderConfig, http: reqwest::Client) -> Self {
        Self { config, http }
    }

    fn url(&self) -> String {
        format!("{}{}", self.config.base_url, self.config.endpoint)
    }

    fn resolve_model<'a>(&'a self, requested: &str) -> (String, Option<&'a ModelConfig>) {
        let slug = if requested.trim().is_empty() {
            self.config.defaults.model.clone()
        } else {
            requested.to_string()
        };
        let entry = self.config.models.iter().find(|m| m.slug == slug);
        (slug, entry)
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
            ("api_key_header", Some(s)) => {
                Ok((None, Some(("x-api-key".to_string(), s.expose().to_string()))))
            }
            ("api_key_header", None) => Err(ProviderError::Provider(
                "api_key_header provider called without a credential".to_string(),
            )),
            (other, _) => Err(ProviderError::Provider(format!("unsupported auth type {other}"))),
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

    fn parse_describe(&self, body: &Value, model_id: &str) -> Result<DescribeOutcome, ProviderError> {
        let parsed = Self::extract_tool_use_input(body, UNDERSTANDING_SCHEMA_NAME)
            .ok_or(ProviderError::SchemaValidation)?;
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
            confidence: parsed.get("confidence").and_then(|v| v.as_f64()).unwrap_or(f64::NAN),
            model_id: model_id.to_string(),
            usage: extract_usage(
                self.config
                    .response
                    .usage_json_pointer
                    .as_deref()
                    .and_then(|p| json_pointer_str(body, p)),
            ),
            provider_request_id: self
                .config
                .response
                .provider_request_id_json_pointer
                .as_deref()
                .and_then(|p| json_pointer_str(body, p))
                .and_then(|v| v.as_str())
                .unwrap_or("")
                .to_string(),
        };
        validate_understanding(&out)?;
        Ok(out)
    }

    fn parse_score(&self, body: &Value, model_id: &str) -> Result<ScoreOutcome, ProviderError> {
        let parsed = Self::extract_tool_use_input(body, RATING_SCHEMA_NAME)
            .ok_or(ProviderError::SchemaValidation)?;
        let scores: Vec<ScoreDimension> = parsed
            .get("scores")
            .and_then(|v| v.as_array())
            .map(|a| {
                a.iter()
                    .filter_map(|s| {
                        let name = s.get("name").and_then(|n| n.as_str()).unwrap_or("").trim().to_string();
                        let score = s.get("score").and_then(|n| n.as_f64()).unwrap_or(f64::NAN);
                        Some(ScoreDimension { name, score })
                    })
                    .collect()
            })
            .unwrap_or_default();
        let out = ScoreOutcome {
            scores,
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
            confidence: parsed.get("confidence").and_then(|v| v.as_f64()).unwrap_or(f64::NAN),
            model_id: model_id.to_string(),
            usage: extract_usage(
                self.config
                    .response
                    .usage_json_pointer
                    .as_deref()
                    .and_then(|p| json_pointer_str(body, p)),
            ),
            provider_request_id: self
                .config
                .response
                .provider_request_id_json_pointer
                .as_deref()
                .and_then(|p| json_pointer_str(body, p))
                .and_then(|v| v.as_str())
                .unwrap_or("")
                .to_string(),
        };
        validate_rating(&out)?;
        Ok(out)
    }

    /// Build the per-request header set: `anthropic-version` always, then
    /// attribution headers, then the `api_key_header` secret when applicable. The
    /// bearer (when applicable) is returned separately and applied by
    /// `send_with_retry` via `bearer_auth`; it never appears in this vec, so it is
    /// never logged.
    fn request_headers(&self, extra_auth_header: Option<(String, String)>) -> Vec<(String, String)> {
        let mut headers = vec![("anthropic-version".to_string(), ANTHROPIC_VERSION.to_string())];
        headers.extend(self.attribution_headers());
        if let Some((k, v)) = extra_auth_header {
            headers.push((k, v));
        }
        headers
    }
}

fn describe_prompt(prompt_profile_id: &str) -> (String, String) {
    let system = "You are an image understanding assistant for Alcedo Studio. Analyze the supplied image and respond with a single JSON object matching the provided schema. The object must contain: \"caption\" (a concise one-line description of the image), \"tags\" (an array of short lowercase searchable tags, with at least one tag), \"scene\" (a short scene or category hint, or an empty string if none), and \"confidence\" (your confidence in the description, a number between 0.0 and 1.0). Output only the JSON object — no prose, no markdown code fences.".to_string();
    let mut instruction = "Describe this image for a photo library.".to_string();
    if !prompt_profile_id.trim().is_empty() {
        instruction.push_str(&format!(" Prompt profile: {prompt_profile_id}."));
    }
    instruction.push_str(" Return only the JSON object described above.");
    (system, instruction)
}

fn score_prompt(prompt_profile_id: &str, rubric_id: &str) -> (String, String) {
    let system = "You are an image rating assistant for Alcedo Studio. Score the supplied image against the given rubric and respond with a single JSON object matching the provided schema. The object must contain: \"scores\" (an array of {\"name\": <dimension>, \"score\": <number>} objects, with at least one), \"rubric_id\" (the rubric you applied), \"rubric_version\" (the rubric version, or an empty string), \"reasons\" (a short rationale), and \"confidence\" (a number between 0.0 and 1.0). Output only the JSON object — no prose, no markdown code fences.".to_string();
    let mut instruction = "Score this image.".to_string();
    if !rubric_id.trim().is_empty() {
        instruction.push_str(&format!(" Rubric: {rubric_id}."));
    }
    if !prompt_profile_id.trim().is_empty() {
        instruction.push_str(&format!(" Prompt profile: {prompt_profile_id}."));
    }
    instruction.push_str(" Return only the JSON object described above.");
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
        credential: Option<&SecretString>,
    ) -> Result<DescribeOutcome, ProviderError> {
        let (slug, model) = self.resolve_model(model_id);
        self.ensure_structured_output(model)?;
        let (bearer, extra_auth_header) = self.build_auth(credential)?;
        let (media_type, image_b64) = build_image_base64(image_bytes)?;
        let schema = strict_schema_value(IMAGE_UNDERSTANDING_SCHEMA)?;
        let (system, instruction) = describe_prompt(prompt_profile_id);
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
        let resp = send_with_retry(&self.http, &self.url(), &body, &headers, bearer, MAX_TRANSIENT_RETRIES)
            .await?;
        let resp_body: Value = resp
            .json()
            .await
            .map_err(|_| ProviderError::SchemaValidation)?;
        let outcome = self.parse_describe(&resp_body, &slug)?;
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
        credential: Option<&SecretString>,
    ) -> Result<ScoreOutcome, ProviderError> {
        let (slug, model) = self.resolve_model(model_id);
        self.ensure_structured_output(model)?;
        let (bearer, extra_auth_header) = self.build_auth(credential)?;
        let (media_type, image_b64) = build_image_base64(image_bytes)?;
        let schema = strict_schema_value(IMAGE_RATING_SCHEMA)?;
        let (system, instruction) = score_prompt(prompt_profile_id, rubric_id);
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
        let resp = send_with_retry(&self.http, &self.url(), &body, &headers, bearer, MAX_TRANSIENT_RETRIES)
            .await?;
        let resp_body: Value = resp
            .json()
            .await
            .map_err(|_| ProviderError::SchemaValidation)?;
        let outcome = self.parse_score(&resp_body, &slug)?;
        warn!(
            provider = %self.config.provider_id,
            model = %slug,
            provider_request_id = %outcome.provider_request_id,
            "ScoreImage completed"
        );
        Ok(outcome)
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
    use wiremock::matchers::{header, method, path};
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
            .describe_image(&test_image_png(), "", "profile-1", Some(&secret()))
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
            .describe_image(&test_image_png(), "doubao-seed-2.0-lite", "", Some(&secret()))
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
        assert!(!data.starts_with("data:"), "image data is a data URI: {data}");
        assert!(!data.is_empty(), "image base64 empty");
        assert_eq!(body["messages"][0]["content"][1]["type"], "text");
        // Structured output via tools + tool_choice, not text.format.json_schema.
        assert_eq!(body["tools"][0]["name"], "alcedo_image_understanding");
        assert_eq!(body["tools"][0]["input_schema"]["type"], "object");
        assert_eq!(body["tools"][0]["input_schema"]["additionalProperties"], false);
        let required = body["tools"][0]["input_schema"]["required"]
            .as_array()
            .expect("required array");
        assert!(required.iter().any(|v| v == "caption"), "caption required");
        assert!(required.iter().any(|v| v == "tags"), "tags required");
        assert_eq!(body["tool_choice"]["type"], "tool");
        assert_eq!(body["tool_choice"]["name"], "alcedo_image_understanding");
        // No Responses-shape fields leak in.
        assert!(body.get("input").is_none(), "Responses `input` field present");
        assert!(body.get("text").is_none(), "Responses `text` field present");
        assert!(body.get("max_output_tokens").is_none(), "Responses `max_output_tokens` present");
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
            .describe_image(&test_image_png(), "", "", Some(&secret()))
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
            .describe_image(&test_image_png(), "", "profile-1", Some(&secret()))
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
                r#"{"scores":[{"name":"aesthetic","score":0.9}],"rubric_id":"alcedo-default-v1","rubric_version":"1","reasons":"r","confidence":0.6}"#,
            )))
            .mount(&server)
            .await;
        let provider = provider_for(&server);
        let out = provider
            .score_image(&test_image_png(), "", "", "alcedo-default-v1", Some(&secret()))
            .await
            .expect("score ok");
        assert_eq!(out.scores.len(), 1);
        assert_eq!(out.scores[0].name, "aesthetic");
        assert!((out.scores[0].score - 0.9).abs() < 1e-9);
        assert_eq!(out.rubric_id, "alcedo-default-v1");
        assert_eq!(out.usage.input_tokens, 90);
        validate_rating(&out).expect("canned rating validates");
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
            .describe_image(&test_image_png(), "", "", Some(&secret()))
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
            .describe_image(&test_image_png(), "", "", Some(&secret()))
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
            .describe_image(&test_image_png(), "", "", Some(&secret()))
            .await
            .expect_err("400 not retried");
        assert!(matches!(err, ProviderError::Provider(_)), "{err:?}");
        assert_eq!(server.received_requests().await.unwrap().len(), 1);
        assert!(!err.to_string().contains(RAW_BODY_SENTINEL), "raw body leaked: {err}");
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
            .describe_image(&test_image_png(), "", "", Some(&secret()))
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
            .describe_image(&test_image_png(), "", "", Some(&secret()))
            .await
            .expect_err("no tool_use");
        assert_eq!(err, ProviderError::SchemaValidation);
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
            .describe_image(&test_image_png(), "", "", Some(&secret()))
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
            .describe_image(&test_image_png(), "", "", None)
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
            .describe_image(&test_image_png(), "", "", Some(&secret()))
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
                BufWriteImpl { inner: self.0.clone() }
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
                    .describe_image(&img, "", "profile-1", Some(&secret()))
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
        assert!(!captured.contains(TEST_SECRET), "secret in logs: {captured}");
        assert!(
            !captured.contains(img_b64.as_str()),
            "image base64 in logs: {captured}"
        );
        assert!(!captured.contains(TEST_PROMPT), "prompt in logs: {captured}");
        assert!(
            !captured.contains(RAW_BODY_SENTINEL),
            "raw body in logs: {captured}"
        );
        assert!(!err.to_string().contains(TEST_SECRET), "secret in error: {err}");
        assert!(
            !err.to_string().contains(RAW_BODY_SENTINEL),
            "raw body in error: {err}"
        );
    }

    #[tokio::test]
    async fn cancellation_drops_in_flight_request() {
        use crate::proto::alcedo::ai::{
            AiErrorCode, AiRequestHeader, AiPriority, AiResponseStatus, DescribeImageRequest,
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
        let svc = ImageAnalysisServiceImpl::new(providers, pid, vault.clone(), cancel_registry.clone());

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
            AiErrorCode, AiRequestHeader, AiPriority, AiResponseStatus, DescribeImageRequest,
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
        let svc = ImageAnalysisServiceImpl::new(providers, pid, vault.clone(), cancel_registry.clone());

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
}

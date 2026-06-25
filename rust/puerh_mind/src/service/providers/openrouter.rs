//! OpenRouter Chat Completions image-analysis driver (Phase 5c, `openrouter_chat`).
//!
//! Builds an OpenAI Chat-compatible `POST /chat/completions` request from the
//! loaded OpenRouter provider config: an Alcedo-owned system prompt, a user
//! message carrying the selected image rendition as a data URI plus a task
//! instruction, `response_format: json_schema` with the code-owned (sanitized to
//! strict-compatible) Alcedo schema, `provider.require_parameters` when the
//! config requests it, and the optional `provider.data_collection = "deny"`
//! privacy knob. The API key is resolved from the Rust credential vault and sent
//! only as `Authorization: Bearer <secret>`; attribution headers come from the
//! config. The response's `choices[0].message.content` is parsed as JSON,
//! validated + normalized against the code-owned understanding / rating contract,
//! and returned as typed fields. Transport / 429 / 5xx failures are retried under
//! a small bounded policy; provider and transport errors map to `ProviderError`
//! variants the service translates into `AiResponseStatus` / `AiErrorCode`.
//!
//! The request/response shape is kept compatible with OpenRouter's official Go
//! SDK chat-completion types (model slug, `response_format.json_schema`,
//! `provider.require_parameters`, attribution headers), even though the
//! production Rust path calls the REST API directly with `reqwest`.

use serde_json::{Value, json};
use tracing::warn;

use crate::service::credential_vault::SecretString;
use crate::service::image_analysis::{
    DescribeOutcome, ImageAnalysisProvider, ProviderError, ScoreDimension, ScoreOutcome,
    IMAGE_RATING_SCHEMA, IMAGE_UNDERSTANDING_SCHEMA, validate_rating, validate_understanding,
};
use crate::service::provider_config::{ModelConfig, ProviderConfig};
use crate::service::providers::http_util::{
    MAX_TRANSIENT_RETRIES, build_image_data_uri, build_rustls_client, extract_usage,
    json_pointer_str, parse_content_json, send_with_retry, strict_schema_value,
};

/// Schema names injected into `response_format.json_schema.name`. Kept stable and
/// Alcedo-namespaced so a provider's structured-output dashboard identifies the
/// contract unambiguously.
const UNDERSTANDING_SCHEMA_NAME: &str = "alcedo_image_understanding";
const RATING_SCHEMA_NAME: &str = "alcedo_image_rating";

pub struct OpenRouterChatProvider {
    config: ProviderConfig,
    http: reqwest::Client,
}

impl OpenRouterChatProvider {
    /// Construct from a validated provider config, building a rustls-backed
    /// `reqwest::Client`. Used by `main.rs` for the shipped sidecar.
    pub fn new(config: ProviderConfig) -> Result<Self, ProviderError> {
        let http = build_rustls_client()?;
        Ok(Self { config, http })
    }

    /// Construct with an injected HTTP client. Used by tests to point the driver
    /// at a local mock server; the rustls client built by `new` already speaks
    /// `http://127.0.0.1` so tests can also use `new` with a localhost base_url.
    #[allow(dead_code)]
    pub fn with_client(config: ProviderConfig, http: reqwest::Client) -> Self {
        Self { config, http }
    }

    fn url(&self) -> String {
        format!("{}{}", self.config.base_url, self.config.endpoint)
    }

    /// Resolve the requested model slug (falling back to the config default) and
    /// the matching model entry (for per-model knobs) if present.
    fn resolve_model<'a>(&'a self, requested: &str) -> (String, Option<&'a ModelConfig>) {
        let slug = if requested.trim().is_empty() {
            self.config.defaults.model.clone()
        } else {
            requested.to_string()
        };
        let entry = self.config.models.iter().find(|m| m.slug == slug);
        (slug, entry)
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
    fn bearer<'a>(&self, credential: Option<&'a SecretString>) -> Result<Option<&'a str>, ProviderError> {
        match (self.config.auth.auth_type.as_str(), credential) {
            ("none", _) => Ok(None),
            ("bearer", Some(s)) => Ok(Some(s.expose())),
            ("bearer", None) => Err(ProviderError::Provider(
                "bearer provider called without a credential".to_string(),
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

    /// Build the `provider` routing object: `require_parameters` when the config
    /// asks for it, plus `data_collection = "deny"` when the selected model
    /// declares the privacy-first knob.
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
        // Omit `provider` entirely when empty so the body matches the Go SDK's
        // omitempty Provider field.
        let is_empty = provider.as_object().map(|o| o.is_empty()).unwrap_or(false);
        if !is_empty {
            body["provider"] = provider;
        }
        body
    }

    fn parse_describe(&self, body: &Value, model_id: &str) -> Result<DescribeOutcome, ProviderError> {
        let content_pointer = self
            .config
            .response
            .content_json_pointer
            .as_deref()
            .unwrap_or("/choices/0/message/content");
        let content = json_pointer_str(body, content_pointer).ok_or(ProviderError::SchemaValidation)?;
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
        let content_pointer = self
            .config
            .response
            .content_json_pointer
            .as_deref()
            .unwrap_or("/choices/0/message/content");
        let content = json_pointer_str(body, content_pointer).ok_or(ProviderError::SchemaValidation)?;
        let parsed = match content {
            Value::String(s) => parse_content_json(s)?,
            other => other.clone(),
        };
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
}

/// Alcedo-owned prompt for `image_understanding.describe`. The prompt profile id is
/// echoed into the instruction for traceability; the JSON contract is enforced by
/// the injected schema + the code-owned validator, not by the prompt text.
fn describe_prompt(prompt_profile_id: &str) -> (String, String) {
    let system = "You are an image understanding assistant for Alcedo Studio. Analyze the supplied image and respond with a single JSON object matching the provided schema. The object must contain: \"caption\" (a concise one-line description of the image), \"tags\" (an array of short lowercase searchable tags, with at least one tag), \"scene\" (a short scene or category hint, or an empty string if none), and \"confidence\" (your confidence in the description, a number between 0.0 and 1.0). Output only the JSON object — no prose, no markdown code fences.".to_string();
    let mut instruction = "Describe this image for a photo library.".to_string();
    if !prompt_profile_id.trim().is_empty() {
        instruction.push_str(&format!(" Prompt profile: {prompt_profile_id}."));
    }
    instruction.push_str(" Return only the JSON object described above.");
    (system, instruction)
}

/// Alcedo-owned prompt for `image_rating.score`.
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
impl ImageAnalysisProvider for OpenRouterChatProvider {
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
        // not used to advertise OpenRouter to the C++ host.
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
        let bearer = self.bearer(credential)?;
        let data_uri = build_image_data_uri(image_bytes)?;
        let schema = strict_schema_value(IMAGE_UNDERSTANDING_SCHEMA)?;
        let (system, instruction) = describe_prompt(prompt_profile_id);
        let body = self.build_chat_body(
            &slug,
            model,
            &data_uri,
            schema,
            UNDERSTANDING_SCHEMA_NAME,
            &system,
            &instruction,
        );
        let resp = send_with_retry(&self.http, &self.url(), &body, &self.attribution_headers(), bearer, MAX_TRANSIENT_RETRIES)
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
        let bearer = self.bearer(credential)?;
        let data_uri = build_image_data_uri(image_bytes)?;
        let schema = strict_schema_value(IMAGE_RATING_SCHEMA)?;
        let (system, instruction) = score_prompt(prompt_profile_id, rubric_id);
        let body = self.build_chat_body(
            &slug,
            model,
            &data_uri,
            schema,
            RATING_SCHEMA_NAME,
            &system,
            &instruction,
        );
        let resp = send_with_retry(&self.http, &self.url(), &body, &self.attribution_headers(), bearer, MAX_TRANSIENT_RETRIES)
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

    const TEST_SECRET: &str = "or-test-key-DO-NOT-LEAK";
    const TEST_PROMPT: &str = "Describe this image for a photo library.";
    const RAW_BODY_SENTINEL: &str = "RAW_PROVIDER_BODY_SENTINEL";

    fn test_image_png() -> Vec<u8> {
        let img = image::RgbImage::from_pixel(2, 2, image::Rgb([10, 20, 30]));
        let mut cursor = std::io::Cursor::new(Vec::new());
        image::DynamicImage::ImageRgb8(img)
            .write_to(&mut cursor, image::ImageFormat::Png)
            .expect("encode png");
        cursor.into_inner()
    }

    fn provider_for(server: &MockServer) -> OpenRouterChatProvider {
        let mut config = load_provider_configs(None)
            .expect("built-ins load")
            .get("openrouter")
            .expect("openrouter built-in")
            .clone();
        config.base_url = server.uri();
        OpenRouterChatProvider::new(config).expect("provider builds")
    }

    fn ok_understanding_body(content_json: &str) -> serde_json::Value {
        json!({
            "id": "or-req-123",
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
    async fn sends_bearer_authorization_and_attribution_headers() {
        let server = MockServer::start().await;
        Mock::given(method("POST"))
            .and(path("/chat/completions"))
            .and(header("authorization", format!("Bearer {TEST_SECRET}")))
            .and(header("http-referer", "https://alcedo.studio"))
            .and(header("x-openrouter-title", "Alcedo Studio"))
            .respond_with(ResponseTemplate::new(200).set_body_json(ok_understanding_body(
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
        assert_eq!(out.provider_request_id, "or-req-123");
        assert_eq!(out.usage.total_tokens, 140);
    }

    #[tokio::test]
    async fn request_body_has_structured_output_and_require_parameters() {
        let server = MockServer::start().await;
        Mock::given(method("POST"))
            .and(path("/chat/completions"))
            .respond_with(ResponseTemplate::new(200).set_body_json(ok_understanding_body(
                r#"{"caption":"c","tags":["t"],"scene":"","confidence":0.5}"#,
            )))
            .mount(&server)
            .await;

        let provider = provider_for(&server);
        provider
            .describe_image(&test_image_png(), "qwen/qwen3.7-plus", "", Some(&secret()))
            .await
            .expect("describe ok");

        let reqs = server.received_requests().await.expect("requests captured");
        assert_eq!(reqs.len(), 1);
        let body: Value = serde_json::from_slice(&reqs[0].body).expect("body json");
        assert_eq!(body["model"], "qwen/qwen3.7-plus");
        assert_eq!(body["stream"], false);
        assert_eq!(body["response_format"]["type"], "json_schema");
        assert_eq!(body["response_format"]["json_schema"]["name"], "alcedo_image_understanding");
        assert_eq!(body["response_format"]["json_schema"]["strict"], true);
        // The code-owned schema is injected (sanitized to strict-compatible: all
        // properties required, additionalProperties false, constraints dropped).
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
        assert!(required.contains(&"confidence"));
        assert!(required.contains(&"scene"));
        assert!(required.contains(&"tags"));
        // provider.require_parameters + data_collection=deny (built-in qwen model).
        assert_eq!(body["provider"]["require_parameters"], true);
        assert_eq!(body["provider"]["data_collection"], "deny");
        // The image is carried as a data URI in the user message.
        let img_url = &body["messages"][1]["content"][1]["image_url"]["url"];
        assert!(img_url.as_str().unwrap().starts_with("data:image/png;base64,"));
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
            .describe_image(&test_image_png(), "", "", Some(&secret()))
            .await
            .expect("describe ok");
        assert_eq!(out.caption, "sunrise over mountains");
        assert_eq!(out.tags.len(), 3);
        assert_eq!(out.scene, "outdoor");
        assert!((out.confidence - 0.82).abs() < 1e-9);
        assert_eq!(out.model_id, "qwen/qwen3.7-plus");
        assert_eq!(out.usage.input_tokens, 100);
        assert_eq!(out.usage.output_tokens, 40);
        assert_eq!(out.usage.total_tokens, 140);
        // validate_understanding passed (returned Ok).
        validate_understanding(&out).expect("canned outcome validates");
    }

    #[tokio::test]
    async fn parses_rating_response_and_captures_usage() {
        let server = MockServer::start().await;
        let body = json!({
            "id": "or-req-456",
            "choices": [ { "message": { "content":
                r#"{"scores":[{"name":"aesthetic","score":0.8},{"name":"technical","score":0.7}],"rubric_id":"alcedo-default-v1","rubric_version":"1","reasons":"good","confidence":0.6}"# } } ],
            "usage": { "prompt_tokens": 80, "completion_tokens": 50, "total_tokens": 130 }
        });
        Mock::given(method("POST"))
            .and(path("/chat/completions"))
            .respond_with(ResponseTemplate::new(200).set_body_json(body))
            .mount(&server)
            .await;
        let provider = provider_for(&server);
        let out = provider
            .score_image(&test_image_png(), "", "", "alcedo-default-v1", Some(&secret()))
            .await
            .expect("score ok");
        assert_eq!(out.scores.len(), 2);
        assert_eq!(out.scores[0].name, "aesthetic");
        assert!((out.scores[0].score - 0.8).abs() < 1e-9);
        assert_eq!(out.rubric_id, "alcedo-default-v1");
        assert_eq!(out.usage.total_tokens, 130);
        assert_eq!(out.provider_request_id, "or-req-456");
        validate_rating(&out).expect("canned rating validates");
    }

    #[tokio::test]
    async fn rate_limit_maps_to_transient() {
        let server = MockServer::start().await;
        // 429 twice (initial + 1 retry), no 200 mock -> Transient after exhaustion.
        Mock::given(method("POST"))
            .and(path("/chat/completions"))
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
        // Two attempts: initial + 1 retry.
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
            .respond_with(ResponseTemplate::new(200).set_body_json(ok_understanding_body(
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
        // Exactly 2 requests: the failed 500 + the successful retry.
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
            .describe_image(&test_image_png(), "", "", Some(&secret()))
            .await
            .expect_err("400 not retried");
        assert!(matches!(err, ProviderError::Provider(_)), "{err:?}");
        // No retry: exactly one request.
        assert_eq!(server.received_requests().await.unwrap().len(), 1);
        // The raw provider body is NOT surfaced in the error string.
        assert!(!err.to_string().contains(RAW_BODY_SENTINEL), "raw body leaked: {err}");
    }

    #[tokio::test]
    async fn schema_failure_does_not_produce_active_result() {
        let server = MockServer::start().await;
        // Valid JSON but violates the understanding contract: empty tags, and a
        // caption that is fine but tags=[] is rejected by validate_understanding.
        Mock::given(method("POST"))
            .and(path("/chat/completions"))
            .respond_with(ResponseTemplate::new(200).set_body_json(ok_understanding_body(
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
    async fn non_json_content_maps_to_schema_validation() {
        let server = MockServer::start().await;
        Mock::given(method("POST"))
            .and(path("/chat/completions"))
            .respond_with(ResponseTemplate::new(200).set_body_json(ok_understanding_body(
                "```json\n{\"caption\":\"c\",\"tags\":[\"t\"]}\n```",
            )))
            .mount(&server)
            .await;
        let provider = provider_for(&server);
        let err = provider
            .describe_image(&test_image_png(), "", "", Some(&secret()))
            .await
            .expect_err("fenced json rejected");
        assert_eq!(err, ProviderError::SchemaValidation);
    }

    #[tokio::test]
    async fn bearer_required_without_credential_errors() {
        let server = MockServer::start().await;
        Mock::given(method("POST"))
            .and(path("/chat/completions"))
            .respond_with(ResponseTemplate::new(200).set_body_json(ok_understanding_body(
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
        // No request was sent (the credential check fails before HTTP).
        assert!(server.received_requests().await.unwrap().is_empty());
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

        let rt = tokio::runtime::Builder::new_current_thread()
            .enable_all()
            .build()
            .expect("current_thread runtime");
        let err = tracing::subscriber::with_default(subscriber, || {
            rt.block_on(async {
                let server = MockServer::start().await;
                // 500 once (triggers a retry warn), then 400 with a raw-body sentinel.
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
                    .describe_image(&test_image_png(), "", "profile-1", Some(&secret()))
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
        // The captured logs must not leak the secret, image, prompt, or raw body.
        assert!(!captured.contains(TEST_SECRET), "secret in logs: {captured}");
        assert!(
            !captured.contains("data:image/png;base64,"),
            "image in logs: {captured}"
        );
        assert!(!captured.contains(TEST_PROMPT), "prompt in logs: {captured}");
        assert!(!captured.contains(RAW_BODY_SENTINEL), "raw body in logs: {captured}");
        // The error string must not leak either.
        assert!(!err.to_string().contains(TEST_SECRET), "secret in error: {err}");
        assert!(!err.to_string().contains(RAW_BODY_SENTINEL), "raw body in error: {err}");
    }

    #[tokio::test]
    async fn cancellation_drops_in_flight_request() {
        use crate::server::image_analysis::ImageAnalysisServiceImpl;
        use crate::proto::alcedo::ai::{
            AiErrorCode, AiRequestHeader, AiPriority, AiResponseStatus, DescribeImageRequest,
            RenditionMetadata as ProtoRendition,
        };
        use crate::service::cancellation_registry::CancellationRegistry;
        use crate::service::credential_vault::CredentialVault;

        let server = MockServer::start().await;
        Mock::given(method("POST"))
            .and(path("/chat/completions"))
            .respond_with(
                ResponseTemplate::new(200)
                    .set_body_json(ok_understanding_body(
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

        let handle = vault.register("openrouter", TEST_SECRET.to_string(), None);
        let request_id = "req-cancel-or".to_string();
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
            provider_id: "openrouter".to_string(),
            model_id: "qwen/qwen3.7-plus".to_string(),
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
        use crate::server::image_analysis::ImageAnalysisServiceImpl;
        use crate::proto::alcedo::ai::{
            AiErrorCode, AiRequestHeader, AiPriority, AiResponseStatus, DescribeImageRequest,
            RenditionMetadata as ProtoRendition,
        };
        use crate::service::cancellation_registry::CancellationRegistry;
        use crate::service::credential_vault::CredentialVault;

        let server = MockServer::start().await;
        Mock::given(method("POST"))
            .and(path("/chat/completions"))
            .respond_with(
                ResponseTemplate::new(200)
                    .set_body_json(ok_understanding_body(
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

        let handle = vault.register("openrouter", TEST_SECRET.to_string(), None);
        let req = DescribeImageRequest {
            header: Some(AiRequestHeader {
                request_id: "req-timeout-or".into(),
                task_id: "image_understanding.describe".into(),
                timeout_ms: 80, // 80ms; the mock delays 2s.
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
            provider_id: "openrouter".to_string(),
            model_id: "qwen/qwen3.7-plus".to_string(),
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
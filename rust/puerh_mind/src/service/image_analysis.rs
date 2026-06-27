//! Image analysis task domain — provider trait, mock provider, code-owned JSON
//! Schemas, and the validator/normalizer (Phase 5b).
//!
//! This is the provider-independent Alcedo result contract for the two Phase 5
//! remote tasks. The proto messages (`proto/image_analysis.proto`) carry these
//! typed fields on the wire; the code-owned JSON Schemas below are what Phase 5c
//! drivers inject as `response_format: json_schema` and validate provider output
//! against. Provider configs select the *injection mode* (response_format_json_schema,
//! responses_json_schema, ...); they do not define the business fields — these
//! schemas and the proto do.
//!
//! `image_understanding.describe` and `image_rating.score` are distinct contracts:
//! distinct `task_id`s, distinct result message types, distinct domain outcomes.
//! A rating result can never overwrite or be reinterpreted as an understanding
//! result (Phase 5b review focus).

use std::time::Duration;

use crate::service::credential_vault::SecretString;

/// Provider-reported usage. Fields are optional in spirit: a provider may report
/// only some; 0 means "not reported". Captured for UI/cost summaries (Phase 6).
#[derive(Debug, Clone, Default, PartialEq, Eq)]
pub struct Usage {
    pub input_tokens: i64,
    pub output_tokens: i64,
    pub total_tokens: i64,
}

/// Phase 6c: a model id discovered by live-listing the configured endpoint's
/// `/models` (or configured override). This is provider-independent: the driver
/// parses the provider's list response into these DTOs and returns them to the
/// host as *unverified candidates*. Discovery only proves the account/endpoint
/// can see a model id; it does NOT prove image input or structured-output
/// support, so a discovered model carries NO capability verdict here — the host
/// merges candidates into preset state but keeps them unadvertised until a
/// validation smoke pins `live_confirmed` (Phase 6f). `source_provider_id`
/// records which configured endpoint produced the candidate so a future
/// prompt/model change does not reinterpret it under a different provider.
#[derive(Debug, Clone, PartialEq, Eq)]
pub struct DiscoveredModel {
    pub model_id: String,
    pub display_name: String,
    pub source_provider_id: String,
}

/// `image_understanding.describe` outcome (provider-neutral).
#[derive(Debug, Clone)]
pub struct DescribeOutcome {
    pub caption: String,
    pub tags: Vec<String>,
    pub scene: String,
    pub confidence: f64,
    /// The model id that actually served (echoed in `AiResponseHeader.model_id`).
    pub model_id: String,
    pub usage: Usage,
    /// The provider's own request id, when available.
    pub provider_request_id: String,
}

/// `image_rating.score` outcome (provider-neutral). A single 1–5 integer star
/// rating aligned with the EXIF-standard Rating the app already stores per file,
/// plus rubric identity and a short rationale. The remote LLM is NOT asked for a
/// confidence (Phase 5f rating-contract change): the rating is a discrete label,
/// not a calibrated probability, and the app's own Rating field has no confidence
/// counterpart to pair it with.
#[derive(Debug, Clone)]
pub struct ScoreOutcome {
    pub rating: i32,
    pub rubric_id: String,
    pub rubric_version: String,
    pub reasons: String,
    pub model_id: String,
    pub usage: Usage,
    pub provider_request_id: String,
}

/// Why a provider call did not produce a usable typed result. The service maps
/// these (plus the credential/timeout/cancel paths it owns) into the
/// `AiResponseHeader` status/error fields. Messages here must stay free of secret
/// material; provider-derived text is redacted by the caller before placement.
#[derive(Debug, Clone, PartialEq, Eq, thiserror::Error)]
pub enum ProviderError {
    #[error("provider response failed schema validation")]
    SchemaValidation,
    #[error("provider returned a transient error")]
    Transient,
    #[error("provider returned an error")]
    Provider(String),
    /// Phase 6c: the request supplied a non-empty `model_id` that does not
    /// resolve to any entry in the provider's `config.models[]` (built-in or
    /// discovered/persisted user config). The call fails closed BEFORE any
    /// provider HTTP request, closing the Phase 6b review gap where an unlisted
    /// model slug could bypass `supports_structured_output` and be forwarded
    /// verbatim to the provider. The inner string is the offending slug; it is
    /// dropped by the service before placement (provider text is never echoed),
    /// so a slug that happens to look like a key cannot leak through this arm.
    #[error("unknown model id; not present in provider config")]
    UnknownModel(String),
}

/// Code-owned JSON Schema for `image_understanding.describe` output. Phase 5c
/// drivers inject this as the provider's structured-output schema and validate
/// parsed provider JSON against it before normalizing into `DescribeOutcome`.
pub const IMAGE_UNDERSTANDING_SCHEMA: &str = r#"{
  "$schema": "https://json-schema.org/draft/2020-12/schema",
  "title": "AlcedoImageUnderstanding",
  "type": "object",
  "additionalProperties": false,
  "required": ["caption", "tags"],
  "properties": {
    "caption": { "type": "string", "minLength": 1 },
    "tags": { "type": "array", "minItems": 1, "items": { "type": "string", "minLength": 1 } },
    "scene": { "type": "string" },
    "confidence": { "type": "number", "minimum": 0.0, "maximum": 1.0 }
  }
}"#;

/// Code-owned JSON Schema for `image_rating.score` output. The remote LLM is
/// asked for a single 1–5 integer star rating plus rubric identity and a short
/// rationale; no `confidence` is requested (Phase 5f rating-contract change). The
/// code-owned validator (`validate_rating`) still enforces `minimum`/`maximum` on
/// the parsed response even though strict-mode injection drops those constraints,
/// so fail-closed behavior is preserved.
pub const IMAGE_RATING_SCHEMA: &str = r#"{
  "$schema": "https://json-schema.org/draft/2020-12/schema",
  "title": "AlcedoImageRating",
  "type": "object",
  "additionalProperties": false,
  "required": ["rating", "rubric_id"],
  "properties": {
    "rating": { "type": "integer", "minimum": 1, "maximum": 5 },
    "rubric_id": { "type": "string", "minLength": 1 },
    "rubric_version": { "type": "string" },
    "reasons": { "type": "string" }
  }
}"#;

/// Validate + normalize a `image_understanding.describe` outcome against the
/// code-owned contract. Returns `ProviderError::SchemaValidation` on any breach
/// so the service maps it to a non-active provider error (Phase 5b: a schema
/// failure must not produce an active annotation).
///
/// Rejects an empty `tags` list as well as any blank tag string, mirroring the
/// `IMAGE_UNDERSTANDING_SCHEMA` `minItems: 1` / `minLength: 1` and matching
/// `validate_rating`'s empty-scores handling.
pub fn validate_understanding(out: &DescribeOutcome) -> Result<(), ProviderError> {
    if out.caption.trim().is_empty() {
        return Err(ProviderError::SchemaValidation);
    }
    if !is_valid_confidence(out.confidence) {
        return Err(ProviderError::SchemaValidation);
    }
    if out.tags.is_empty() || out.tags.iter().any(|t| t.trim().is_empty()) {
        return Err(ProviderError::SchemaValidation);
    }
    Ok(())
}

/// Validate + normalize a `image_rating.score` outcome. The rating is a 1–5
/// integer star rating (the app's own Rating field uses 0–5 with 0=unrated; the
/// remote contract requires 1..=5 so a scored image is never confused with an
/// unrated one). No confidence is checked — the remote LLM is not asked for one.
pub fn validate_rating(out: &ScoreOutcome) -> Result<(), ProviderError> {
    if out.rubric_id.trim().is_empty() {
        return Err(ProviderError::SchemaValidation);
    }
    if !(1..=5).contains(&out.rating) {
        return Err(ProviderError::SchemaValidation);
    }
    Ok(())
}

fn is_valid_confidence(c: f64) -> bool {
    !c.is_nan() && c >= 0.0 && c <= 1.0
}

/// Build the output-language directive appended to a prompt's system message.
/// `output_language` is the host-resolved code ("" or "en" = English, the
/// default prompt language; "zh" = Simplified Chinese). English needs no
/// directive — the prompt is already English — so only a non-English code
/// produces a sentence. Unknown codes produce no directive (fail open to the
/// default English prompt rather than injecting garbage).
pub fn language_directive(output_language: &str) -> String {
    match output_language.trim() {
        "zh" | "zh-CN" | "zh_CN" | "chinese" => {
            " Respond in Simplified Chinese (简体中文).".to_string()
        }
        _ => String::new(),
    }
}

/// A remote/local image-analysis provider. Phase 5b ships one implementation
/// (`MockImageAnalysisProvider`); Phase 5c adds `OpenRouterChatProvider` and
/// `VolcengineArkResponsesProvider` behind the same trait; Phase 6b generalizes the
/// OpenAI Chat-compatible path into `OpenAiChatCompatibleProvider` (of which
/// `OpenRouterChatProvider` is now a type alias) and keeps
/// `AnthropicMessagesProvider` generic over the base URL.
///
/// `credential` carries the resolved secret (from the Rust credential vault) when
/// `requires_credential()` is true; `None` otherwise. The provider must treat the
/// `SecretString` as a secret — only `expose()` it at the single call site that
/// builds the `Authorization` header, and never log it, base64, image bytes, or
/// prompt payloads (Phase 5c negative-test focus). The service resolves the handle
/// against the vault and passes the secret down, so the secret travels only across
/// the gRPC loopback -> vault -> trait call boundary, never through process args,
/// `AiSidecarRuntimeOptions`, or logs (Phase 3 invariant preserved).
#[tonic::async_trait]
pub trait ImageAnalysisProvider: Send + Sync {
    fn provider_id(&self) -> &str;
    /// True when the provider needs a resolved credential handle to call out.
    fn requires_credential(&self) -> bool;
    /// The descriptor this provider advertises via `ListCapabilities` (mock only
    /// in Phase 5b; real providers advertise via provider configs in 5a).
    fn capability(&self) -> crate::proto::alcedo::ai::AiCapability;
    async fn describe_image(
        &self,
        image_bytes: &[u8],
        model_id: &str,
        prompt_profile_id: &str,
        output_language: &str,
        credential: Option<&SecretString>,
    ) -> Result<DescribeOutcome, ProviderError>;
    async fn score_image(
        &self,
        image_bytes: &[u8],
        model_id: &str,
        prompt_profile_id: &str,
        rubric_id: &str,
        output_language: &str,
        credential: Option<&SecretString>,
    ) -> Result<ScoreOutcome, ProviderError>;
    /// Phase 6c: list the model ids the configured endpoint exposes (a dry-run
    /// discovery probe, not an image-analysis call). The credential is the
    /// resolved vault secret when `requires_credential()` is true; it is
    /// `expose()`d only at the auth-header build site, exactly like the task
    /// calls. The default impl fails closed — only the OpenAI- and
    /// Anthropic-compatible drivers override it. Discovery never persists
    /// annotations; the host treats the returned candidates as unverified.
    async fn list_models(
        &self,
        _credential: Option<&SecretString>,
    ) -> Result<Vec<DiscoveredModel>, ProviderError> {
        Err(ProviderError::Provider(
            "model discovery is not supported by this provider".to_string(),
        ))
    }
}

/// How the mock should misbehave, for service-level failure tests.
#[derive(Debug, Clone)]
pub enum MockFailure {
    None,
    /// Return an outcome that fails the service validator (empty caption /
    /// out-of-range confidence) — exercises the schema-validation failure path.
    InvalidOutput,
    /// Sleep past the caller's timeout — exercises the DEADLINE_EXCEEDED path.
    Slow(Duration),
    /// Return `ProviderError::Provider` — exercises the provider-error path
    /// (maps to PROVIDER_ERROR / INTERNAL). The inner string is dropped by the
    /// service before placement, so provider text is not leaked in the header.
    Error,
    /// Return `ProviderError::Transient` — exercises the transient-error path
    /// (maps to PROVIDER_UNAVAILABLE / PROVIDER_5XX). Real 5xx/rate-limit
    /// detection lives in the Phase 5c HTTP drivers; this mock covers the
    /// service-side mapping so all three `provider_error_to_header` arms are
    /// exercised in Phase 5b.
    Transient,
}

/// A provider that returns valid typed results without any HTTP. Used by the
/// service tests and as the bundled dev/mock provider in Phase 5b.
pub struct MockImageAnalysisProvider {
    provider_id: String,
    model_id: String,
    requires_credential: bool,
    failure: MockFailure,
    /// Phase 6c: when set, `list_models` returns these candidates; otherwise it
    /// uses the default unsupported impl. Lets the server test the ListModels
    /// RPC plumbing (credential resolution, success/failure header) without HTTP.
    discovered_models: Vec<DiscoveredModel>,
}

impl MockImageAnalysisProvider {
    pub fn new(provider_id: impl Into<String>, model_id: impl Into<String>) -> Self {
        Self {
            provider_id: provider_id.into(),
            model_id: model_id.into(),
            requires_credential: false,
            failure: MockFailure::None,
            discovered_models: Vec::new(),
        }
    }

    pub fn with_requires_credential(mut self, v: bool) -> Self {
        self.requires_credential = v;
        self
    }

    pub fn with_failure(mut self, f: MockFailure) -> Self {
        self.failure = f;
        self
    }

    pub fn with_discovered_models(mut self, models: Vec<DiscoveredModel>) -> Self {
        self.discovered_models = models;
        self
    }

    fn canned_describe(&self) -> DescribeOutcome {
        DescribeOutcome {
            caption: "A mock caption describing the image.".to_string(),
            tags: vec!["mock".to_string(), "test".to_string()],
            scene: "studio".to_string(),
            confidence: 0.9,
            model_id: self.model_id.clone(),
            usage: Usage {
                input_tokens: 128,
                output_tokens: 32,
                total_tokens: 160,
            },
            provider_request_id: "mock-req-1".to_string(),
        }
    }

    fn canned_score(&self) -> ScoreOutcome {
        ScoreOutcome {
            rating: 4,
            rubric_id: "alcedo-default-v1".to_string(),
            rubric_version: "1".to_string(),
            reasons: "Mock rubric reasons.".to_string(),
            model_id: self.model_id.clone(),
            usage: Usage {
                input_tokens: 128,
                output_tokens: 48,
                total_tokens: 176,
            },
            provider_request_id: "mock-req-2".to_string(),
        }
    }
}

#[tonic::async_trait]
impl ImageAnalysisProvider for MockImageAnalysisProvider {
    fn provider_id(&self) -> &str {
        &self.provider_id
    }

    fn requires_credential(&self) -> bool {
        self.requires_credential
    }

    fn capability(&self) -> crate::proto::alcedo::ai::AiCapability {
        use crate::proto::alcedo::ai::{AiCapability, AiInputKind, AiOutputKind};
        let inputs = vec![
            AiInputKind::AiInputThumbnail as i32,
            AiInputKind::AiInputPreview as i32,
            AiInputKind::AiInputImage as i32,
        ];
        AiCapability {
            task_id: "image_understanding.describe".to_string(),
            provider_id: self.provider_id.clone(),
            model_id: self.model_id.clone(),
            input_kinds: inputs.clone(),
            output_kinds: vec![
                AiOutputKind::AiOutputCaption as i32,
                AiOutputKind::AiOutputTags as i32,
            ],
            supports_batch: false,
            supports_cancel: true,
            requires_credential: self.requires_credential,
            max_payload_bytes: 0,
        }
    }

    async fn describe_image(
        &self,
        _image_bytes: &[u8],
        _model_id: &str,
        _prompt_profile_id: &str,
        _output_language: &str,
        _credential: Option<&SecretString>,
    ) -> Result<DescribeOutcome, ProviderError> {
        if let MockFailure::Slow(d) = self.failure {
            tokio::time::sleep(d).await;
        }
        if matches!(self.failure, MockFailure::Error) {
            return Err(ProviderError::Provider("mock provider failed".to_string()));
        }
        if matches!(self.failure, MockFailure::Transient) {
            return Err(ProviderError::Transient);
        }
        let mut out = self.canned_describe();
        if matches!(self.failure, MockFailure::InvalidOutput) {
            // Corrupt the outcome so the service validator rejects it.
            out.caption.clear();
            out.confidence = 1.5;
        }
        Ok(out)
    }

    async fn score_image(
        &self,
        _image_bytes: &[u8],
        _model_id: &str,
        _prompt_profile_id: &str,
        _rubric_id: &str,
        _output_language: &str,
        _credential: Option<&SecretString>,
    ) -> Result<ScoreOutcome, ProviderError> {
        if let MockFailure::Slow(d) = self.failure {
            tokio::time::sleep(d).await;
        }
        if matches!(self.failure, MockFailure::Error) {
            return Err(ProviderError::Provider("mock provider failed".to_string()));
        }
        if matches!(self.failure, MockFailure::Transient) {
            return Err(ProviderError::Transient);
        }
        let mut out = self.canned_score();
        if matches!(self.failure, MockFailure::InvalidOutput) {
            // Corrupt the outcome so the service validator rejects it: an
            // out-of-range rating (0 is the app's "unrated" sentinel, not a
            // valid scored rating, and outside the 1..=5 contract).
            out.rating = 0;
        }
        Ok(out)
    }

    /// Phase 6c: when the mock was given discovered candidates, return them;
    /// otherwise fail closed with the default unsupported error.
    async fn list_models(
        &self,
        _credential: Option<&SecretString>,
    ) -> Result<Vec<DiscoveredModel>, ProviderError> {
        if self.discovered_models.is_empty() {
            return Err(ProviderError::Provider(
                "model discovery is not supported by this provider".to_string(),
            ));
        }
        Ok(self.discovered_models.clone())
    }
}

#[cfg(test)]
mod schema_tests {
    use super::*;
    use serde_json::Value;

    #[test]
    fn understanding_schema_is_well_formed_json() {
        let v: Value = serde_json::from_str(IMAGE_UNDERSTANDING_SCHEMA).expect("valid json");
        assert_eq!(v["type"], "object");
        assert!(v["required"].is_array());
        let required: Vec<&str> = v["required"].as_array().unwrap().iter().map(|x| x.as_str().unwrap()).collect();
        assert!(required.contains(&"caption"));
        assert!(required.contains(&"tags"));
        assert_eq!(v["properties"]["caption"]["type"], "string");
        assert_eq!(v["properties"]["confidence"]["maximum"], 1.0);
        // tags must be a non-empty array of non-empty strings (matches the
        // validator's empty-list + blank-string rejection and the rating
        // schema's minItems:1 on scores).
        assert_eq!(v["properties"]["tags"]["minItems"], 1);
        assert_eq!(v["properties"]["tags"]["items"]["minLength"], 1);
    }

    #[test]
    fn rating_schema_is_well_formed_json() {
        let v: Value = serde_json::from_str(IMAGE_RATING_SCHEMA).expect("valid json");
        assert_eq!(v["type"], "object");
        let required: Vec<&str> = v["required"].as_array().unwrap().iter().map(|x| x.as_str().unwrap()).collect();
        assert!(required.contains(&"rating"));
        assert!(required.contains(&"rubric_id"));
        // The rating is a 1..=5 integer; no `scores` array and no `confidence`
        // are requested from the remote LLM (Phase 5f rating-contract change).
        assert_eq!(v["properties"]["rating"]["type"], "integer");
        assert_eq!(v["properties"]["rating"]["minimum"], 1);
        assert_eq!(v["properties"]["rating"]["maximum"], 5);
        assert!(v["properties"].get("scores").is_none(), "scores array still present");
        assert!(v["properties"].get("confidence").is_none(), "confidence still present");
    }

    #[test]
    fn validator_accepts_valid_understanding() {
        let out = DescribeOutcome {
            caption: "c".into(),
            tags: vec!["t".into()],
            scene: "s".into(),
            confidence: 0.5,
            model_id: "m".into(),
            usage: Usage::default(),
            provider_request_id: "r".into(),
        };
        validate_understanding(&out).expect("valid");
    }

    #[test]
    fn validator_rejects_empty_caption() {
        let out = DescribeOutcome {
            caption: "  ".into(),
            tags: vec!["t".into()],
            scene: "".into(),
            confidence: 0.5,
            model_id: "m".into(),
            usage: Usage::default(),
            provider_request_id: "r".into(),
        };
        assert_eq!(validate_understanding(&out).unwrap_err(), ProviderError::SchemaValidation);
    }

    #[test]
    fn validator_rejects_out_of_range_confidence() {
        let out = DescribeOutcome {
            caption: "c".into(),
            tags: vec!["t".into()],
            scene: "".into(),
            confidence: 1.5,
            model_id: "m".into(),
            usage: Usage::default(),
            provider_request_id: "r".into(),
        };
        assert_eq!(validate_understanding(&out).unwrap_err(), ProviderError::SchemaValidation);
    }

    #[test]
    fn validator_rejects_blank_tag_string_and_out_of_range_rating() {
        // A tag that is a blank string (vec![""]) is rejected for understanding.
        let bad_tags = DescribeOutcome {
            caption: "c".into(),
            tags: vec!["".into()],
            scene: "".into(),
            confidence: 0.5,
            model_id: "m".into(),
            usage: Usage::default(),
            provider_request_id: "r".into(),
        };
        assert_eq!(validate_understanding(&bad_tags).unwrap_err(), ProviderError::SchemaValidation);

        // An out-of-range rating (0 is the app's "unrated" sentinel and outside
        // the 1..=5 remote contract) is rejected for rating.
        let bad_rating = ScoreOutcome {
            rating: 0,
            rubric_id: "r".into(),
            rubric_version: "1".into(),
            reasons: "".into(),
            model_id: "m".into(),
            usage: Usage::default(),
            provider_request_id: "r".into(),
        };
        assert_eq!(validate_rating(&bad_rating).unwrap_err(), ProviderError::SchemaValidation);

        // 6 is also out of range (the upper bound is inclusive 5).
        let too_high = ScoreOutcome {
            rating: 6,
            rubric_id: "r".into(),
            rubric_version: "1".into(),
            reasons: "".into(),
            model_id: "m".into(),
            usage: Usage::default(),
            provider_request_id: "r".into(),
        };
        assert_eq!(validate_rating(&too_high).unwrap_err(), ProviderError::SchemaValidation);
    }

    #[test]
    fn validator_rejects_empty_tags_list() {
        // An empty tags list (vec![]) is rejected for understanding, not just a
        // blank-string tag. Previously validate_understanding only checked
        // .any(|t| t.trim().is_empty()), which is false for an empty list, so
        // tags: [] slipped through despite the 5b doc promising "reject empty
        // tags / scores". This is the understanding-side mirror of the rating
        // empty-scores-list rejection above.
        let empty_tags = DescribeOutcome {
            caption: "c".into(),
            tags: vec![],
            scene: "".into(),
            confidence: 0.5,
            model_id: "m".into(),
            usage: Usage::default(),
            provider_request_id: "r".into(),
        };
        assert_eq!(validate_understanding(&empty_tags).unwrap_err(), ProviderError::SchemaValidation);
    }

    #[tokio::test]
    async fn mock_returns_canned_valid_results() {
        let mock = MockImageAnalysisProvider::new("mock", "alcedo-mock");
        let d = mock.describe_image(&[], "", "", "", None).await.expect("describe");
        assert_eq!(d.caption, "A mock caption describing the image.");
        assert!(!d.tags.is_empty());
        validate_understanding(&d).expect("canned describe validates");
        let s = mock.score_image(&[], "", "", "", "", None).await.expect("score");
        validate_rating(&s).expect("canned score validates");
    }
}
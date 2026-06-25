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

/// Provider-reported usage. Fields are optional in spirit: a provider may report
/// only some; 0 means "not reported". Captured for UI/cost summaries (Phase 6).
#[derive(Debug, Clone, Default, PartialEq, Eq)]
pub struct Usage {
    pub input_tokens: i64,
    pub output_tokens: i64,
    pub total_tokens: i64,
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

/// One scored dimension of a `image_rating.score` outcome.
#[derive(Debug, Clone)]
pub struct ScoreDimension {
    pub name: String,
    pub score: f64,
}

/// `image_rating.score` outcome (provider-neutral).
#[derive(Debug, Clone)]
pub struct ScoreOutcome {
    pub scores: Vec<ScoreDimension>,
    pub rubric_id: String,
    pub rubric_version: String,
    pub reasons: String,
    pub confidence: f64,
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
    "tags": { "type": "array", "items": { "type": "string" } },
    "scene": { "type": "string" },
    "confidence": { "type": "number", "minimum": 0.0, "maximum": 1.0 }
  }
}"#;

/// Code-owned JSON Schema for `image_rating.score` output.
pub const IMAGE_RATING_SCHEMA: &str = r#"{
  "$schema": "https://json-schema.org/draft/2020-12/schema",
  "title": "AlcedoImageRating",
  "type": "object",
  "additionalProperties": false,
  "required": ["scores", "rubric_id"],
  "properties": {
    "scores": {
      "type": "array",
      "minItems": 1,
      "items": {
        "type": "object",
        "additionalProperties": false,
        "required": ["name", "score"],
        "properties": {
          "name": { "type": "string", "minLength": 1 },
          "score": { "type": "number" }
        }
      }
    },
    "rubric_id": { "type": "string", "minLength": 1 },
    "rubric_version": { "type": "string" },
    "reasons": { "type": "string" },
    "confidence": { "type": "number", "minimum": 0.0, "maximum": 1.0 }
  }
}"#;

/// Validate + normalize a `image_understanding.describe` outcome against the
/// code-owned contract. Returns `ProviderError::SchemaValidation` on any breach
/// so the service maps it to a non-active provider error (Phase 5b: a schema
/// failure must not produce an active annotation).
pub fn validate_understanding(out: &DescribeOutcome) -> Result<(), ProviderError> {
    if out.caption.trim().is_empty() {
        return Err(ProviderError::SchemaValidation);
    }
    if !is_valid_confidence(out.confidence) {
        return Err(ProviderError::SchemaValidation);
    }
    if out.tags.iter().any(|t| t.trim().is_empty()) {
        return Err(ProviderError::SchemaValidation);
    }
    Ok(())
}

/// Validate + normalize a `image_rating.score` outcome.
pub fn validate_rating(out: &ScoreOutcome) -> Result<(), ProviderError> {
    if out.rubric_id.trim().is_empty() {
        return Err(ProviderError::SchemaValidation);
    }
    if out.scores.is_empty() {
        return Err(ProviderError::SchemaValidation);
    }
    if out.scores.iter().any(|s| s.name.trim().is_empty()) {
        return Err(ProviderError::SchemaValidation);
    }
    if !is_valid_confidence(out.confidence) {
        return Err(ProviderError::SchemaValidation);
    }
    Ok(())
}

fn is_valid_confidence(c: f64) -> bool {
    !c.is_nan() && c >= 0.0 && c <= 1.0
}

/// A remote/local image-analysis provider. Phase 5b ships one implementation
/// (`MockImageAnalysisProvider`); Phase 5c adds `OpenRouterChatProvider` and
/// `VolcengineArkResponsesProvider` behind the same trait.
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
    ) -> Result<DescribeOutcome, ProviderError>;
    async fn score_image(
        &self,
        image_bytes: &[u8],
        model_id: &str,
        prompt_profile_id: &str,
        rubric_id: &str,
    ) -> Result<ScoreOutcome, ProviderError>;
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
}

impl MockImageAnalysisProvider {
    pub fn new(provider_id: impl Into<String>, model_id: impl Into<String>) -> Self {
        Self {
            provider_id: provider_id.into(),
            model_id: model_id.into(),
            requires_credential: false,
            failure: MockFailure::None,
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
            scores: vec![
                ScoreDimension {
                    name: "aesthetic".to_string(),
                    score: 0.8,
                },
                ScoreDimension {
                    name: "technical".to_string(),
                    score: 0.7,
                },
            ],
            rubric_id: "alcedo-default-v1".to_string(),
            rubric_version: "1".to_string(),
            reasons: "Mock rubric reasons.".to_string(),
            confidence: 0.85,
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
            out.scores.clear();
        }
        Ok(out)
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
    }

    #[test]
    fn rating_schema_is_well_formed_json() {
        let v: Value = serde_json::from_str(IMAGE_RATING_SCHEMA).expect("valid json");
        assert_eq!(v["type"], "object");
        let required: Vec<&str> = v["required"].as_array().unwrap().iter().map(|x| x.as_str().unwrap()).collect();
        assert!(required.contains(&"scores"));
        assert!(required.contains(&"rubric_id"));
        assert_eq!(v["properties"]["scores"]["minItems"], 1);
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
    fn validator_rejects_empty_tags_or_scores() {
        let bad_tags = DescribeOutcome {
            caption: "c".into(),
            tags: vec!["".into()],
            scene: "".into(),
            confidence: 0.5,
            model_id: "m".into(),
            usage: Usage::default(),
            provider_request_id: "r".into(),
        };
        assert!(validate_understanding(&bad_tags).is_err());

        let bad_scores = ScoreOutcome {
            scores: vec![],
            rubric_id: "r".into(),
            rubric_version: "1".into(),
            reasons: "".into(),
            confidence: 0.5,
            model_id: "m".into(),
            usage: Usage::default(),
            provider_request_id: "r".into(),
        };
        assert!(validate_rating(&bad_scores).is_err());
    }

    #[tokio::test]
    async fn mock_returns_canned_valid_results() {
        let mock = MockImageAnalysisProvider::new("mock", "alcedo-mock");
        let d = mock.describe_image(&[], "", "").await.expect("describe");
        assert_eq!(d.caption, "A mock caption describing the image.");
        assert!(!d.tags.is_empty());
        validate_understanding(&d).expect("canned describe validates");
        let s = mock.score_image(&[], "", "", "").await.expect("score");
        validate_rating(&s).expect("canned score validates");
    }
}
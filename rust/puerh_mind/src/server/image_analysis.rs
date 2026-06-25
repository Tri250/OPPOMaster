//! `ImageAnalysisService` server implementation (Phase 5b).
//!
//! Routes `DescribeImage` / `ScoreImage` to a registered image-analysis provider
//! (the mock in Phase 5b; OpenRouter / Volcengine Ark drivers in Phase 5c). The
//! service owns the control-plane concerns the provider should not: credential
//! resolution against the vault, request timeout, cooperative cancellation via
//! the `CancellationRegistry`, and schema validation of the provider's typed
//! result. Outcomes travel inside the `AiResponseHeader` (status / error_code /
//! redacted error_message) — this is the Phase 5 structured-error-in-body mapping
//! deferred from Phase 4: image analysis has no legacy v1 caller, so the RPC
//! returns `Ok(Response{ header, ... })` with the header carrying the precise
//! status rather than a plain `tonic::Status`. A genuinely malformed request
//! (empty image bytes) is still a transport-level `tonic::Status::invalid_argument`,
//! since there is no provider outcome to report.

use std::collections::HashMap;
use std::sync::Arc;
use std::time::Duration;

use tonic::{Request, Response, Status};
use tracing::info;

use crate::proto::alcedo::ai::{
    AiErrorCode, AiRequestHeader, AiResponseHeader, AiResponseStatus, DescribeImageRequest,
    DescribeImageResponse, ImageUnderstandingResult, ImageRatingResult, ScoreImageRequest,
    ScoreImageResponse, ScoredDimension, UsageMetadata,
    image_analysis_service_server::ImageAnalysisService,
};
use crate::service::credential_vault::{CredentialError, CredentialVault, SecretString};
use crate::service::cancellation_registry::CancellationRegistry;
use crate::service::image_analysis::{
    DescribeOutcome, ImageAnalysisProvider, ProviderError, ScoreOutcome, validate_rating,
    validate_understanding,
};

/// Timeout used when the request header leaves `timeout_ms` at 0 (server default).
const DEFAULT_TIMEOUT_MS: u64 = 60_000;

pub struct ImageAnalysisServiceImpl {
    providers: HashMap<String, Arc<dyn ImageAnalysisProvider>>,
    default_provider_id: String,
    vault: Arc<CredentialVault>,
    cancel_registry: Arc<CancellationRegistry>,
}

impl ImageAnalysisServiceImpl {
    pub fn new(
        providers: HashMap<String, Arc<dyn ImageAnalysisProvider>>,
        default_provider_id: String,
        vault: Arc<CredentialVault>,
        cancel_registry: Arc<CancellationRegistry>,
    ) -> Self {
        Self {
            providers,
            default_provider_id,
            vault,
            cancel_registry,
        }
    }

    fn lookup_provider(&self, provider_id: &str) -> Option<Arc<dyn ImageAnalysisProvider>> {
        let id = if provider_id.trim().is_empty() {
            self.default_provider_id.as_str()
        } else {
            provider_id
        };
        self.providers.get(id).cloned()
    }

    /// Resolve the credential a provider requires to the secret itself. Returns
    /// `Ok(None)` when the provider does not require a credential, `Ok(Some(secret))`
    /// when the handle resolves, or a failure header triple when the credential is
    /// missing or the handle is invalid; the caller turns the triple into an
    /// `Ok(Response)` with that failure header. Provider ids are not secrets and
    /// may appear in error text; the message is still passed through the vault's
    /// redactor before placement. The resolved `SecretString` is handed to the
    /// provider trait method and never logged (Phase 3 invariant preserved).
    fn resolve_credential_secret(
        &self,
        header: &AiRequestHeader,
        provider: &dyn ImageAnalysisProvider,
    ) -> Result<Option<SecretString>, (AiResponseStatus, AiErrorCode, String)> {
        if !provider.requires_credential() {
            return Ok(None);
        }
        if header.credential_ref.trim().is_empty() {
            return Err((
                AiResponseStatus::AiStatusUnauthenticated,
                AiErrorCode::AiErrorMissingCredential,
                "image analysis task requires a credential but none was supplied".to_string(),
            ));
        }
        match self.vault.resolve(&header.credential_ref) {
            Ok(secret) => Ok(Some(secret)),
            Err(CredentialError::NotFound) | Err(CredentialError::Revoked) => Err((
                AiResponseStatus::AiStatusPermissionDenied,
                AiErrorCode::AiErrorCredentialRevoked,
                "credential handle is unknown or revoked".to_string(),
            )),
            Err(CredentialError::Expired) => Err((
                AiResponseStatus::AiStatusPermissionDenied,
                AiErrorCode::AiErrorCredentialExpired,
                "credential handle has expired".to_string(),
            )),
        }
    }

    fn timeout_duration(header: &AiRequestHeader) -> Duration {
        if header.timeout_ms > 0 {
            Duration::from_millis(header.timeout_ms as u64)
        } else {
            Duration::from_millis(DEFAULT_TIMEOUT_MS)
        }
    }

    /// Build an `AiResponseHeader` carrying a failure outcome. The error message
    /// is redacted of any secret the vault currently holds before placement.
    /// Returns `Some(...)` because the proto3 `header` field is `Option<AiResponseHeader>`.
    fn failure_header(
        &self,
        req: &AiRequestHeader,
        status: AiResponseStatus,
        error_code: AiErrorCode,
        message: &str,
        provider: &str,
        model_id: &str,
        elapsed_ms: u64,
    ) -> Option<AiResponseHeader> {
        Some(AiResponseHeader {
            request_id: req.request_id.clone(),
            task_id: req.task_id.clone(),
            status: status as i32,
            error_code: error_code as i32,
            error_message: self.vault.redact_error_message(message),
            provider: provider.to_string(),
            model_id: model_id.to_string(),
            elapsed_ms: elapsed_ms as i64,
        })
    }

    fn success_header(
        &self,
        req: &AiRequestHeader,
        provider: &str,
        model_id: &str,
        elapsed_ms: u64,
    ) -> Option<AiResponseHeader> {
        Some(AiResponseHeader {
            request_id: req.request_id.clone(),
            task_id: req.task_id.clone(),
            status: AiResponseStatus::AiStatusOk as i32,
            error_code: AiErrorCode::AiErrorNone as i32,
            error_message: String::new(),
            provider: provider.to_string(),
            model_id: model_id.to_string(),
            elapsed_ms: elapsed_ms as i64,
        })
    }

    /// Map a `ProviderError` (or a validator rejection) to a header failure triple.
    fn provider_error_to_header(err: &ProviderError) -> (AiResponseStatus, AiErrorCode, String) {
        match err {
            ProviderError::SchemaValidation => (
                AiResponseStatus::AiStatusProviderError,
                AiErrorCode::AiErrorPayloadDecode,
                "provider response failed schema validation".to_string(),
            ),
            ProviderError::Transient => (
                AiResponseStatus::AiStatusProviderUnavailable,
                AiErrorCode::AiErrorProvider5xx,
                "provider returned a transient error".to_string(),
            ),
            ProviderError::Provider(_) => (
                AiResponseStatus::AiStatusProviderError,
                AiErrorCode::AiErrorInternal,
                "provider returned an error".to_string(),
            ),
        }
    }
}

fn to_proto_understanding(out: &DescribeOutcome) -> ImageUnderstandingResult {
    ImageUnderstandingResult {
        caption: out.caption.clone(),
        tags: out.tags.clone(),
        scene: out.scene.clone(),
        confidence: out.confidence,
    }
}

fn to_proto_rating(out: &ScoreOutcome) -> ImageRatingResult {
    let scores = out
        .scores
        .iter()
        .map(|s| ScoredDimension {
            name: s.name.clone(),
            score: s.score,
        })
        .collect();
    ImageRatingResult {
        scores,
        rubric_id: out.rubric_id.clone(),
        rubric_version: out.rubric_version.clone(),
        reasons: out.reasons.clone(),
        confidence: out.confidence,
    }
}

fn to_proto_usage(out: &impl HasUsage) -> Option<UsageMetadata> {
    let u = out.usage();
    if u.input_tokens == 0 && u.output_tokens == 0 && u.total_tokens == 0 {
        return None;
    }
    Some(UsageMetadata {
        input_tokens: u.input_tokens,
        output_tokens: u.output_tokens,
        total_tokens: u.total_tokens,
    })
}

trait HasUsage {
    fn usage(&self) -> &crate::service::image_analysis::Usage;
}
impl HasUsage for DescribeOutcome {
    fn usage(&self) -> &crate::service::image_analysis::Usage {
        &self.usage
    }
}
impl HasUsage for ScoreOutcome {
    fn usage(&self) -> &crate::service::image_analysis::Usage {
        &self.usage
    }
}

#[tonic::async_trait]
impl ImageAnalysisService for ImageAnalysisServiceImpl {
    async fn describe_image(
        &self,
        request: Request<DescribeImageRequest>,
    ) -> Result<Response<DescribeImageResponse>, Status> {
        info!("received DescribeImage request");
        let start = std::time::Instant::now();
        let req = request.into_inner();
        let header = req.header.unwrap_or_default();

        if req.image_bytes.is_empty() {
            return Err(Status::invalid_argument("image_bytes must not be empty"));
        }

        let provider = match self.lookup_provider(&req.provider_id) {
            Some(p) => p,
            None => {
                return Ok(Response::new(DescribeImageResponse {
                    header: self.failure_header(
                        &header,
                        AiResponseStatus::AiStatusUnsupportedTask,
                        AiErrorCode::AiErrorTaskUnknown,
                        "no provider registered for the requested provider_id",
                        &req.provider_id,
                        &req.model_id,
                        start.elapsed().as_millis() as u64,
                    ),
                    result: None,
                    rendition: req.rendition.clone(),
                    usage: None,
                    provider_request_id: String::new(),
                    prompt_profile_id: req.prompt_profile_id.clone(),
                }));
            }
        };

        let credential = match self.resolve_credential_secret(&header, provider.as_ref()) {
            Ok(c) => c,
            Err((status, code, msg)) => {
                return Ok(Response::new(DescribeImageResponse {
                    header: self.failure_header(
                        &header,
                        status,
                        code,
                        &msg,
                        provider.provider_id(),
                        &req.model_id,
                        start.elapsed().as_millis() as u64,
                    ),
                    result: None,
                    rendition: req.rendition.clone(),
                    usage: None,
                    provider_request_id: String::new(),
                    prompt_profile_id: req.prompt_profile_id.clone(),
                }));
            }
        };

        let cancel_rx = self.cancel_registry.register(&header.request_id);
        let provider_fut = provider.describe_image(
            &req.image_bytes,
            &req.model_id,
            &req.prompt_profile_id,
            credential.as_ref(),
        );
        tokio::pin!(provider_fut);
        let timeout_dur = Self::timeout_duration(&header);

        let outcome: Result<Result<DescribeOutcome, ProviderError>, _> = tokio::select! {
            biased;
            _ = cancel_rx => {
                self.cancel_registry.complete(&header.request_id);
                return Ok(Response::new(DescribeImageResponse {
                    header: self.failure_header(
                        &header,
                        AiResponseStatus::AiStatusCancelled,
                        AiErrorCode::AiErrorCancelledByClient,
                        "task cancelled by client",
                        provider.provider_id(),
                        &req.model_id,
                        start.elapsed().as_millis() as u64,
                    ),
                    result: None,
                    rendition: req.rendition.clone(),
                    usage: None,
                    provider_request_id: String::new(),
                    prompt_profile_id: req.prompt_profile_id.clone(),
                }));
            }
            result = tokio::time::timeout(timeout_dur, provider_fut) => result,
        };
        self.cancel_registry.complete(&header.request_id);
        let elapsed = start.elapsed().as_millis() as u64;

        let response = match outcome {
            Ok(Ok(out)) => match validate_understanding(&out) {
                Ok(()) => DescribeImageResponse {
                    header: self.success_header(&header, provider.provider_id(), &out.model_id, elapsed),
                    result: Some(to_proto_understanding(&out)),
                    rendition: req.rendition.clone(),
                    usage: to_proto_usage(&out),
                    provider_request_id: out.provider_request_id.clone(),
                    prompt_profile_id: req.prompt_profile_id.clone(),
                },
                Err(err) => {
                    let (status, code, msg) = Self::provider_error_to_header(&err);
                    DescribeImageResponse {
                        header: self.failure_header(
                            &header, status, code, &msg, provider.provider_id(), &out.model_id, elapsed,
                        ),
                        result: None,
                        rendition: req.rendition.clone(),
                        usage: None,
                        provider_request_id: String::new(),
                        prompt_profile_id: req.prompt_profile_id.clone(),
                    }
                }
            },
            Ok(Err(err)) => {
                let (status, code, msg) = Self::provider_error_to_header(&err);
                DescribeImageResponse {
                    header: self.failure_header(
                        &header, status, code, &msg, provider.provider_id(), &req.model_id, elapsed,
                    ),
                    result: None,
                    rendition: req.rendition.clone(),
                    usage: None,
                    provider_request_id: String::new(),
                    prompt_profile_id: req.prompt_profile_id.clone(),
                }
            }
            Err(_elapsed) => DescribeImageResponse {
                header: self.failure_header(
                    &header,
                    AiResponseStatus::AiStatusDeadlineExceeded,
                    AiErrorCode::AiErrorProviderTimeout,
                    "provider call did not complete within the timeout",
                    provider.provider_id(),
                    &req.model_id,
                    elapsed,
                ),
                result: None,
                rendition: req.rendition.clone(),
                usage: None,
                provider_request_id: String::new(),
                prompt_profile_id: req.prompt_profile_id.clone(),
            },
        };
        Ok(Response::new(response))
    }

    async fn score_image(
        &self,
        request: Request<ScoreImageRequest>,
    ) -> Result<Response<ScoreImageResponse>, Status> {
        info!("received ScoreImage request");
        let start = std::time::Instant::now();
        let req = request.into_inner();
        let header = req.header.unwrap_or_default();

        if req.image_bytes.is_empty() {
            return Err(Status::invalid_argument("image_bytes must not be empty"));
        }

        let provider = match self.lookup_provider(&req.provider_id) {
            Some(p) => p,
            None => {
                return Ok(Response::new(ScoreImageResponse {
                    header: self.failure_header(
                        &header,
                        AiResponseStatus::AiStatusUnsupportedTask,
                        AiErrorCode::AiErrorTaskUnknown,
                        "no provider registered for the requested provider_id",
                        &req.provider_id,
                        &req.model_id,
                        start.elapsed().as_millis() as u64,
                    ),
                    result: None,
                    rendition: req.rendition.clone(),
                    usage: None,
                    provider_request_id: String::new(),
                    prompt_profile_id: req.prompt_profile_id.clone(),
                }));
            }
        };

        let credential = match self.resolve_credential_secret(&header, provider.as_ref()) {
            Ok(c) => c,
            Err((status, code, msg)) => {
                return Ok(Response::new(ScoreImageResponse {
                    header: self.failure_header(
                        &header,
                        status,
                        code,
                        &msg,
                        provider.provider_id(),
                        &req.model_id,
                        start.elapsed().as_millis() as u64,
                    ),
                    result: None,
                    rendition: req.rendition.clone(),
                    usage: None,
                    provider_request_id: String::new(),
                    prompt_profile_id: req.prompt_profile_id.clone(),
                }));
            }
        };

        let cancel_rx = self.cancel_registry.register(&header.request_id);
        let provider_fut = provider.score_image(
            &req.image_bytes,
            &req.model_id,
            &req.prompt_profile_id,
            &req.rubric_id,
            credential.as_ref(),
        );
        tokio::pin!(provider_fut);
        let timeout_dur = Self::timeout_duration(&header);

        let outcome: Result<Result<ScoreOutcome, ProviderError>, _> = tokio::select! {
            biased;
            _ = cancel_rx => {
                self.cancel_registry.complete(&header.request_id);
                return Ok(Response::new(ScoreImageResponse {
                    header: self.failure_header(
                        &header,
                        AiResponseStatus::AiStatusCancelled,
                        AiErrorCode::AiErrorCancelledByClient,
                        "task cancelled by client",
                        provider.provider_id(),
                        &req.model_id,
                        start.elapsed().as_millis() as u64,
                    ),
                    result: None,
                    rendition: req.rendition.clone(),
                    usage: None,
                    provider_request_id: String::new(),
                    prompt_profile_id: req.prompt_profile_id.clone(),
                }));
            }
            result = tokio::time::timeout(timeout_dur, provider_fut) => result,
        };
        self.cancel_registry.complete(&header.request_id);
        let elapsed = start.elapsed().as_millis() as u64;

        let response = match outcome {
            Ok(Ok(out)) => match validate_rating(&out) {
                Ok(()) => ScoreImageResponse {
                    header: self.success_header(&header, provider.provider_id(), &out.model_id, elapsed),
                    result: Some(to_proto_rating(&out)),
                    rendition: req.rendition.clone(),
                    usage: to_proto_usage(&out),
                    provider_request_id: out.provider_request_id.clone(),
                    prompt_profile_id: req.prompt_profile_id.clone(),
                },
                Err(err) => {
                    let (status, code, msg) = Self::provider_error_to_header(&err);
                    ScoreImageResponse {
                        header: self.failure_header(
                            &header, status, code, &msg, provider.provider_id(), &out.model_id, elapsed,
                        ),
                        result: None,
                        rendition: req.rendition.clone(),
                        usage: None,
                        provider_request_id: String::new(),
                        prompt_profile_id: req.prompt_profile_id.clone(),
                    }
                }
            },
            Ok(Err(err)) => {
                let (status, code, msg) = Self::provider_error_to_header(&err);
                ScoreImageResponse {
                    header: self.failure_header(
                        &header, status, code, &msg, provider.provider_id(), &req.model_id, elapsed,
                    ),
                    result: None,
                    rendition: req.rendition.clone(),
                    usage: None,
                    provider_request_id: String::new(),
                    prompt_profile_id: req.prompt_profile_id.clone(),
                }
            }
            Err(_elapsed) => ScoreImageResponse {
                header: self.failure_header(
                    &header,
                    AiResponseStatus::AiStatusDeadlineExceeded,
                    AiErrorCode::AiErrorProviderTimeout,
                    "provider call did not complete within the timeout",
                    provider.provider_id(),
                    &req.model_id,
                    elapsed,
                ),
                result: None,
                rendition: req.rendition.clone(),
                usage: None,
                provider_request_id: String::new(),
                prompt_profile_id: req.prompt_profile_id.clone(),
            },
        };
        Ok(Response::new(response))
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::proto::alcedo::ai::{AiInputKind, AiOutputKind, AiPriority, RenditionMetadata as ProtoRendition};
    use crate::service::image_analysis::{MockFailure, MockImageAnalysisProvider};

    fn svc(mock: MockImageAnalysisProvider) -> ImageAnalysisServiceImpl {
        let vault = Arc::new(CredentialVault::new(None));
        let cancel_registry = Arc::new(CancellationRegistry::new());
        let mut providers: HashMap<String, Arc<dyn ImageAnalysisProvider>> = HashMap::new();
        let pid = mock.provider_id().to_string();
        providers.insert(pid.clone(), Arc::new(mock));
        ImageAnalysisServiceImpl::new(providers, pid, vault, cancel_registry)
    }

    fn header(request_id: &str, timeout_ms: i64, credential_ref: &str) -> AiRequestHeader {
        AiRequestHeader {
            request_id: request_id.to_string(),
            task_id: "image_understanding.describe".to_string(),
            timeout_ms,
            priority: AiPriority::Normal as i32,
            trace_id: String::new(),
            credential_ref: credential_ref.to_string(),
            client_capabilities: vec![],
        }
    }

    fn rendition() -> Option<ProtoRendition> {
        Some(ProtoRendition {
            kind: "preview".to_string(),
            width: 512,
            height: 512,
            bytes: 1024,
        })
    }

    fn image_bytes() -> Vec<u8> {
        vec![1, 2, 3, 4]
    }

    #[tokio::test]
    async fn describe_image_returns_valid_understanding() {
        let svc = svc(MockImageAnalysisProvider::new("mock", "alcedo-mock"));
        let req = Request::new(DescribeImageRequest {
            header: Some(header("req-1", 5000, "")),
            image_bytes: image_bytes(),
            image_format_hint: "rgba8:2x2".to_string(),
            rendition: rendition(),
            provider_id: "mock".to_string(),
            model_id: "alcedo-mock".to_string(),
            prompt_profile_id: "profile-1".to_string(),
        });
        let resp = svc.describe_image(req).await.expect("rpc ok");
        let inner = resp.into_inner();
        assert_eq!(inner.header.as_ref().unwrap().request_id, "req-1");
        assert_eq!(inner.header.as_ref().unwrap().status, AiResponseStatus::AiStatusOk as i32);
        let result = inner.result.expect("understanding result present");
        assert!(!result.caption.is_empty());
        assert!(!result.tags.is_empty());
        assert_eq!(inner.header.as_ref().unwrap().model_id, "alcedo-mock");
        assert_eq!(inner.header.as_ref().unwrap().provider, "mock");
        // Rendition + prompt profile echoed.
        assert_eq!(inner.rendition.as_ref().unwrap().kind, "preview");
        assert_eq!(inner.prompt_profile_id, "profile-1");
        // Usage captured.
        let usage = inner.usage.expect("usage present");
        assert!(usage.total_tokens > 0);
        assert_eq!(inner.provider_request_id, "mock-req-1");
    }

    #[tokio::test]
    async fn score_image_returns_valid_rating() {
        let svc = svc(MockImageAnalysisProvider::new("mock", "alcedo-mock"));
        let req = Request::new(ScoreImageRequest {
            header: Some(AiRequestHeader {
                request_id: "req-2".into(),
                task_id: "image_rating.score".into(),
                ..header("req-2", 5000, "")
            }),
            image_bytes: image_bytes(),
            image_format_hint: "rgba8:2x2".to_string(),
            rendition: rendition(),
            provider_id: "mock".to_string(),
            model_id: "alcedo-mock".to_string(),
            prompt_profile_id: "profile-1".to_string(),
            rubric_id: "alcedo-default-v1".to_string(),
        });
        let resp = svc.score_image(req).await.expect("rpc ok");
        let inner = resp.into_inner();
        assert_eq!(inner.header.as_ref().unwrap().status, AiResponseStatus::AiStatusOk as i32);
        assert_eq!(inner.header.as_ref().unwrap().task_id, "image_rating.score");
        let result = inner.result.expect("rating result present");
        assert!(!result.scores.is_empty());
        // Distinct task_id + result type: rating carries scores, not caption/tags.
        assert_eq!(inner.header.as_ref().unwrap().model_id, "alcedo-mock");
    }

    #[tokio::test]
    async fn describe_image_missing_credential_returns_unauthenticated() {
        let svc = svc(
            MockImageAnalysisProvider::new("mock", "alcedo-mock").with_requires_credential(true),
        );
        let req = Request::new(DescribeImageRequest {
            header: Some(header("req-3", 5000, "")), // empty credential_ref
            image_bytes: image_bytes(),
            image_format_hint: "rgba8:2x2".to_string(),
            rendition: rendition(),
            provider_id: "mock".to_string(),
            model_id: "alcedo-mock".to_string(),
            prompt_profile_id: "".to_string(),
        });
        let resp = svc.describe_image(req).await.expect("rpc ok (failure in header)");
        let inner = resp.into_inner();
        let h = inner.header.expect("header present");
        assert_eq!(h.status, AiResponseStatus::AiStatusUnauthenticated as i32);
        assert_eq!(h.error_code, AiErrorCode::AiErrorMissingCredential as i32);
        assert!(inner.result.is_none(), "no result on credential failure");
        // No secret material in the message.
        assert!(!h.error_message.contains("sk-"));
    }

    #[tokio::test]
    async fn describe_image_with_valid_credential_succeeds() {
        let svc = svc(
            MockImageAnalysisProvider::new("mock", "alcedo-mock").with_requires_credential(true),
        );
        // Register a credential in the vault and pass its handle.
        let handle = svc.vault.register("mock", "sk-test-key".to_string(), None);
        let req = Request::new(DescribeImageRequest {
            header: Some(header("req-cred", 5000, &handle)),
            image_bytes: image_bytes(),
            image_format_hint: "rgba8:2x2".to_string(),
            rendition: rendition(),
            provider_id: "mock".to_string(),
            model_id: "alcedo-mock".to_string(),
            prompt_profile_id: "".to_string(),
        });
        let resp = svc.describe_image(req).await.expect("rpc ok");
        let inner = resp.into_inner();
        assert_eq!(inner.header.as_ref().unwrap().status, AiResponseStatus::AiStatusOk as i32);
        assert!(inner.result.is_some());
        // The secret never appears in the response.
        assert!(!format!("{:?}", inner).contains("sk-test-key"));
    }

    #[tokio::test]
    async fn describe_image_timeout_returns_deadline_exceeded() {
        let svc = svc(
            MockImageAnalysisProvider::new("mock", "alcedo-mock")
                .with_failure(MockFailure::Slow(Duration::from_millis(500))),
        );
        let req = Request::new(DescribeImageRequest {
            header: Some(header("req-4", 50, "")), // 50ms timeout
            image_bytes: image_bytes(),
            image_format_hint: "rgba8:2x2".to_string(),
            rendition: rendition(),
            provider_id: "mock".to_string(),
            model_id: "alcedo-mock".to_string(),
            prompt_profile_id: "".to_string(),
        });
        let resp = svc.describe_image(req).await.expect("rpc ok (timeout in header)");
        let inner = resp.into_inner();
        let h = inner.header.expect("header present");
        assert_eq!(h.status, AiResponseStatus::AiStatusDeadlineExceeded as i32);
        assert_eq!(h.error_code, AiErrorCode::AiErrorProviderTimeout as i32);
        assert!(inner.result.is_none());
    }

    #[tokio::test]
    async fn describe_image_schema_validation_failure_returns_provider_error() {
        let svc = svc(
            MockImageAnalysisProvider::new("mock", "alcedo-mock")
                .with_failure(MockFailure::InvalidOutput),
        );
        let req = Request::new(DescribeImageRequest {
            header: Some(header("req-5", 5000, "")),
            image_bytes: image_bytes(),
            image_format_hint: "rgba8:2x2".to_string(),
            rendition: rendition(),
            provider_id: "mock".to_string(),
            model_id: "alcedo-mock".to_string(),
            prompt_profile_id: "".to_string(),
        });
        let resp = svc.describe_image(req).await.expect("rpc ok (schema failure in header)");
        let inner = resp.into_inner();
        let h = inner.header.expect("header present");
        assert_eq!(h.status, AiResponseStatus::AiStatusProviderError as i32);
        assert_eq!(h.error_code, AiErrorCode::AiErrorPayloadDecode as i32);
        assert!(inner.result.is_none(), "schema failure produces no active result");
    }

    #[tokio::test]
    async fn describe_image_provider_error_returns_provider_error() {
        // Exercises the `ProviderError::Provider(_)` -> PROVIDER_ERROR / INTERNAL
        // arm of `provider_error_to_header`. The inner provider string is dropped
        // before placement, so provider text is not leaked in the header.
        let svc = svc(
            MockImageAnalysisProvider::new("mock", "alcedo-mock")
                .with_failure(MockFailure::Error),
        );
        let req = Request::new(DescribeImageRequest {
            header: Some(header("req-err", 5000, "")),
            image_bytes: image_bytes(),
            image_format_hint: "rgba8:2x2".to_string(),
            rendition: rendition(),
            provider_id: "mock".to_string(),
            model_id: "alcedo-mock".to_string(),
            prompt_profile_id: "".to_string(),
        });
        let resp = svc.describe_image(req).await.expect("rpc ok (provider error in header)");
        let inner = resp.into_inner();
        let h = inner.header.expect("header present");
        assert_eq!(h.status, AiResponseStatus::AiStatusProviderError as i32);
        assert_eq!(h.error_code, AiErrorCode::AiErrorInternal as i32);
        assert!(inner.result.is_none(), "provider error produces no active result");
        // The mock's inner error text is NOT placed in the header.
        assert!(!h.error_message.contains("mock provider failed"));
    }

    #[tokio::test]
    async fn describe_image_transient_error_returns_provider_unavailable() {
        // Exercises the `ProviderError::Transient` -> PROVIDER_UNAVAILABLE /
        // PROVIDER_5XX arm of `provider_error_to_header`. Real 5xx / rate-limit
        // detection is a Phase 5c driver concern; this covers the service mapping.
        let svc = svc(
            MockImageAnalysisProvider::new("mock", "alcedo-mock")
                .with_failure(MockFailure::Transient),
        );
        let req = Request::new(DescribeImageRequest {
            header: Some(header("req-tx", 5000, "")),
            image_bytes: image_bytes(),
            image_format_hint: "rgba8:2x2".to_string(),
            rendition: rendition(),
            provider_id: "mock".to_string(),
            model_id: "alcedo-mock".to_string(),
            prompt_profile_id: "".to_string(),
        });
        let resp = svc.describe_image(req).await.expect("rpc ok (transient in header)");
        let inner = resp.into_inner();
        let h = inner.header.expect("header present");
        assert_eq!(h.status, AiResponseStatus::AiStatusProviderUnavailable as i32);
        assert_eq!(h.error_code, AiErrorCode::AiErrorProvider5xx as i32);
        assert!(inner.result.is_none());
    }

    #[tokio::test]
    async fn describe_image_cancellation_returns_cancelled() {
        let svc = svc(MockImageAnalysisProvider::new("mock", "alcedo-mock").with_failure(
            // Long enough that the cancel branch wins the select! first.
            MockFailure::Slow(Duration::from_secs(30)),
        ));
        let request_id = "req-cancel".to_string();
        let req = Request::new(DescribeImageRequest {
            header: Some(header(&request_id, 60000, "")),
            image_bytes: image_bytes(),
            image_format_hint: "rgba8:2x2".to_string(),
            rendition: rendition(),
            provider_id: "mock".to_string(),
            model_id: "alcedo-mock".to_string(),
            prompt_profile_id: "".to_string(),
        });

        // Cancel before awaiting: register the id, then fire cancel from another task.
        let cancel_registry = svc.cancel_registry.clone();
        let rid = request_id.clone();
        tokio::spawn(async move {
            tokio::time::sleep(Duration::from_millis(20)).await;
            cancel_registry.cancel(&rid);
        });

        let resp = svc.describe_image(req).await.expect("rpc ok (cancel in header)");
        let inner = resp.into_inner();
        let h = inner.header.expect("header present");
        assert_eq!(h.status, AiResponseStatus::AiStatusCancelled as i32);
        assert_eq!(h.error_code, AiErrorCode::AiErrorCancelledByClient as i32);
    }

    #[tokio::test]
    async fn describe_image_unknown_provider_returns_unsupported_task() {
        let svc = svc(MockImageAnalysisProvider::new("mock", "alcedo-mock"));
        let req = Request::new(DescribeImageRequest {
            header: Some(header("req-6", 5000, "")),
            image_bytes: image_bytes(),
            image_format_hint: "rgba8:2x2".to_string(),
            rendition: rendition(),
            provider_id: "openrouter".to_string(), // not registered in 5b
            model_id: "qwen/qwen3.7-plus".to_string(),
            prompt_profile_id: "".to_string(),
        });
        let resp = svc.describe_image(req).await.expect("rpc ok (unsupported in header)");
        let inner = resp.into_inner();
        let h = inner.header.expect("header present");
        assert_eq!(h.status, AiResponseStatus::AiStatusUnsupportedTask as i32);
        assert_eq!(h.error_code, AiErrorCode::AiErrorTaskUnknown as i32);
    }

    #[tokio::test]
    async fn describe_image_empty_bytes_is_transport_error() {
        let svc = svc(MockImageAnalysisProvider::new("mock", "alcedo-mock"));
        let req = Request::new(DescribeImageRequest {
            header: Some(header("req-7", 5000, "")),
            image_bytes: Vec::new(),
            image_format_hint: "rgba8:2x2".to_string(),
            rendition: rendition(),
            provider_id: "mock".to_string(),
            model_id: "alcedo-mock".to_string(),
            prompt_profile_id: "".to_string(),
        });
        let err = svc.describe_image(req).await.expect_err("empty image is a transport error");
        assert_eq!(err.code(), tonic::Code::InvalidArgument);
    }

    #[test]
    fn mock_capability_advertises_no_credential_understanding() {
        let mock = MockImageAnalysisProvider::new("mock", "alcedo-mock");
        let cap = mock.capability();
        assert_eq!(cap.task_id, "image_understanding.describe");
        assert_eq!(cap.provider_id, "mock");
        assert_eq!(cap.model_id, "alcedo-mock");
        assert!(!cap.requires_credential);
        assert!(cap.supports_cancel);
        assert!(!cap.supports_batch);
        assert!(cap.input_kinds.contains(&(AiInputKind::AiInputPreview as i32)));
        assert!(cap.output_kinds.contains(&(AiOutputKind::AiOutputCaption as i32)));
    }
}
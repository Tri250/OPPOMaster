use std::sync::Arc;

use tonic::{Request, Response, Status};
use tracing::info;

use crate::proto::alcedo::ai::{
    AiCapability, AiRequestHeader, AiResponseHeader, AiResponseStatus, CancelTaskRequest,
    CancelTaskResponse, ListCapabilitiesRequest, ListCapabilitiesResponse,
    RegisterCredentialRequest, RegisterCredentialResponse,
    ai_runtime_service_server::AiRuntimeService,
};
use crate::service::cancellation_registry::CancellationRegistry;
use crate::service::credential_vault::CredentialVault;

/// Sidecar-level runtime service.
///
/// Phase 3 makes the service stateful: it owns the in-memory credential vault,
/// the cancellation registry, and the advertised capability descriptors.
/// `ListCapabilities` reports the descriptors; `RegisterCredential` mints an
/// opaque handle for a long-lived provider secret (the secret stays in the
/// vault and is never logged); `CancelTask` cancels an in-flight task by
/// `request_id`.
pub struct AiRuntimeServiceImpl {
    vault: Arc<CredentialVault>,
    cancel_registry: Arc<CancellationRegistry>,
    capabilities: Vec<AiCapability>,
}

impl AiRuntimeServiceImpl {
    pub fn new(
        vault: Arc<CredentialVault>,
        cancel_registry: Arc<CancellationRegistry>,
        capabilities: Vec<AiCapability>,
    ) -> Self {
        Self {
            vault,
            cancel_registry,
            capabilities,
        }
    }
}

/// Build a success `AiResponseHeader` echoing the caller's correlation ids.
fn ok_header(req: &AiRequestHeader) -> AiResponseHeader {
    AiResponseHeader {
        request_id: req.request_id.clone(),
        task_id: req.task_id.clone(),
        status: AiResponseStatus::AiStatusOk as i32,
        error_code: 0,
        error_message: String::new(),
        provider: String::new(),
        model_id: String::new(),
        elapsed_ms: 0,
    }
}

#[tonic::async_trait]
impl AiRuntimeService for AiRuntimeServiceImpl {
    async fn list_capabilities(
        &self,
        request: Request<ListCapabilitiesRequest>,
    ) -> Result<Response<ListCapabilitiesResponse>, Status> {
        info!("received ListCapabilities request");
        // task_id / credential_ref / timeout_ms are documented as ignored here.
        let req_header = request.into_inner().header.unwrap_or_default();
        Ok(Response::new(ListCapabilitiesResponse {
            header: Some(ok_header(&req_header)),
            capabilities: self.capabilities.clone(),
        }))
    }

    async fn register_credential(
        &self,
        request: Request<RegisterCredentialRequest>,
    ) -> Result<Response<RegisterCredentialResponse>, Status> {
        // Log only that the call happened and the provider scope — never the
        // secret. The secret lives only inside the vault's SecretString.
        info!(
            "received RegisterCredential request for provider {:?}",
            request.get_ref().provider_id
        );
        let req = request.into_inner();
        let header = req.header.unwrap_or_default();
        if req.ttl_ms < 0 {
            return Err(Status::invalid_argument("ttl_ms must be >= 0"));
        }
        let ttl = if req.ttl_ms == 0 {
            None
        } else {
            Some(std::time::Duration::from_millis(req.ttl_ms as u64))
        };
        let handle = self.vault.register(&req.provider_id, req.secret, ttl);
        Ok(Response::new(RegisterCredentialResponse {
            header: Some(ok_header(&header)),
            credential_handle: handle,
        }))
    }

    async fn cancel_task(
        &self,
        request: Request<CancelTaskRequest>,
    ) -> Result<Response<CancelTaskResponse>, Status> {
        info!("received CancelTask request");
        let req = request.into_inner();
        let header = req.header.unwrap_or_default();
        let cancelled = self.cancel_registry.cancel(&req.request_id);
        Ok(Response::new(CancelTaskResponse {
            header: Some(ok_header(&header)),
            cancelled,
        }))
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::proto::alcedo::ai::AiRequestHeader;
    use crate::service::capabilities::build_capability_descriptors;
    use crate::service::embedding::MockEmbeddingEngine;
    use tonic::Request;

    fn test_impl() -> AiRuntimeServiceImpl {
        let vault = Arc::new(CredentialVault::new(None));
        let cancel_registry = Arc::new(CancellationRegistry::new());
        let capabilities = build_capability_descriptors(&MockEmbeddingEngine, 4096);
        AiRuntimeServiceImpl::new(vault, cancel_registry, capabilities)
    }

    #[tokio::test]
    async fn list_capabilities_echoes_header_and_returns_descriptors() {
        let svc = test_impl();
        let request = Request::new(ListCapabilitiesRequest {
            header: Some(AiRequestHeader {
                request_id: "req-1".into(),
                task_id: "runtime.list_capabilities".into(),
                ..Default::default()
            }),
        });

        let response = svc.list_capabilities(request).await.expect("rpc succeeds");
        let inner = response.into_inner();
        let header = inner.header.expect("response header present");

        assert_eq!(header.request_id, "req-1");
        assert_eq!(header.task_id, "runtime.list_capabilities");
        // AI_STATUS_OK = 1.
        assert_eq!(header.status, AiResponseStatus::AiStatusOk as i32);
        assert_eq!(header.error_code, 0);

        assert_eq!(inner.capabilities.len(), 2);
        assert_eq!(inner.capabilities[0].task_id, "semantic.embed_*");
        assert!(!inner.capabilities[0].requires_credential);
        assert_eq!(inner.capabilities[1].task_id, "image_understanding.describe");
        assert!(inner.capabilities[1].requires_credential);
        assert_eq!(inner.capabilities[1].model_id, "unconfigured");
    }

    #[tokio::test]
    async fn list_capabilities_succeeds_without_request_header() {
        let svc = test_impl();
        let request = Request::new(ListCapabilitiesRequest { header: None });

        let response = svc.list_capabilities(request).await.expect("rpc succeeds");
        let inner = response.into_inner();
        let header = inner.header.expect("response header present");

        assert_eq!(header.status, AiResponseStatus::AiStatusOk as i32);
        assert!(header.request_id.is_empty());
        assert_eq!(inner.capabilities.len(), 2);
    }

    #[tokio::test]
    async fn register_credential_returns_handle() {
        let svc = test_impl();
        let request = Request::new(RegisterCredentialRequest {
            header: Some(AiRequestHeader {
                request_id: "reg-1".into(),
                task_id: "ai_runtime.register_credential".into(),
                ..Default::default()
            }),
            provider_id: "remote".into(),
            secret: "sk-test".into(),
            ttl_ms: 0,
        });

        let response = svc.register_credential(request).await.expect("rpc succeeds");
        let inner = response.into_inner();
        let header = inner.header.expect("response header present");
        assert_eq!(header.request_id, "reg-1");
        assert_eq!(header.status, AiResponseStatus::AiStatusOk as i32);
        assert!(!inner.credential_handle.is_empty());
    }

    #[tokio::test]
    async fn register_credential_rejects_negative_ttl() {
        let svc = test_impl();
        let request = Request::new(RegisterCredentialRequest {
            header: None,
            provider_id: "remote".into(),
            secret: "sk-test".into(),
            ttl_ms: -1,
        });

        let err = svc
            .register_credential(request)
            .await
            .expect_err("negative ttl rejected");
        assert_eq!(err.code(), tonic::Code::InvalidArgument);
    }

    #[tokio::test]
    async fn cancel_task_unknown_id_returns_not_cancelled() {
        let svc = test_impl();
        let request = Request::new(CancelTaskRequest {
            header: None,
            request_id: "nope".into(),
        });

        let response = svc.cancel_task(request).await.expect("rpc succeeds");
        let inner = response.into_inner();
        assert!(!inner.cancelled);
    }

    #[tokio::test]
    async fn cancel_task_known_id_returns_cancelled() {
        let svc = test_impl();
        // Register an in-flight task directly in the registry; keep the
        // receiver alive so the cancel signal has somewhere to go.
        let _rx = svc.cancel_registry.register("in-flight");

        let request = Request::new(CancelTaskRequest {
            header: None,
            request_id: "in-flight".into(),
        });

        let response = svc.cancel_task(request).await.expect("rpc succeeds");
        let inner = response.into_inner();
        assert!(inner.cancelled);
    }
}
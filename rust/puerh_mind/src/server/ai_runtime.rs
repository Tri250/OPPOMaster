use tonic::{Request, Response, Status};
use tracing::info;

use crate::proto::alcedo::ai::{
    AiResponseHeader, AiResponseStatus, ListCapabilitiesRequest, ListCapabilitiesResponse,
    ai_runtime_service_server::AiRuntimeService,
};

/// Sidecar-level runtime service.
///
/// Phase 1 implements only `ListCapabilities`, and reports an empty capability
/// list: the protobuf foundation and service registration are the deliverable.
/// Concrete capability descriptors (local semantic embedding, remote image
/// understanding) are populated in Phase 3; the C++ host's in-process fake
/// client carries its own canned response for tests (Phase 2).
pub struct AiRuntimeServiceImpl;

impl AiRuntimeServiceImpl {
    pub fn new() -> Self {
        Self
    }
}

impl Default for AiRuntimeServiceImpl {
    fn default() -> Self {
        Self::new()
    }
}

#[tonic::async_trait]
impl AiRuntimeService for AiRuntimeServiceImpl {
    async fn list_capabilities(
        &self,
        request: Request<ListCapabilitiesRequest>,
    ) -> Result<Response<ListCapabilitiesResponse>, Status> {
        info!("received ListCapabilities request");

        // Echo the caller's correlation ids. task_id / credential_ref /
        // timeout_ms are documented as ignored for this RPC.
        let request_header = request.into_inner().header.unwrap_or_default();
        let response_header = AiResponseHeader {
            request_id: request_header.request_id,
            task_id: request_header.task_id,
            status: AiResponseStatus::AiStatusOk as i32,
            error_code: 0,
            error_message: String::new(),
            provider: String::new(),
            model_id: String::new(),
            elapsed_ms: 0,
        };

        Ok(Response::new(ListCapabilitiesResponse {
            header: Some(response_header),
            capabilities: Vec::new(),
        }))
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::proto::alcedo::ai::AiRequestHeader;
    use tonic::Request;

    #[tokio::test]
    async fn list_capabilities_echoes_header_and_returns_ok_with_no_capabilities() {
        let svc = AiRuntimeServiceImpl::new();
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
        assert!(inner.capabilities.is_empty());
    }

    #[tokio::test]
    async fn list_capabilities_succeeds_without_request_header() {
        let svc = AiRuntimeServiceImpl::new();
        let request = Request::new(ListCapabilitiesRequest { header: None });

        let response = svc.list_capabilities(request).await.expect("rpc succeeds");
        let inner = response.into_inner();
        let header = inner.header.expect("response header present");

        assert_eq!(header.status, AiResponseStatus::AiStatusOk as i32);
        assert!(header.request_id.is_empty());
        assert!(inner.capabilities.is_empty());
    }
}

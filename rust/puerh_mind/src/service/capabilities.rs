//! Capability descriptors advertised by `AiRuntimeService::ListCapabilities`.
//!
//! Phase 3 populates two descriptors:
//! - The local semantic-embedding capability (real, backed by the existing
//!   embedding engine; `requires_credential=false`).
//! - A remote image-understanding capability (descriptor-only — the actual
//!   provider RPC and credential-backed execution land in Phase 5). It is
//!   advertised so the C++ host can discover the `requires_credential` path and
//!   wire the credential-handle flow; its `model_id` is a placeholder until
//!   Phase 5 configures a real provider.

use crate::proto::alcedo::ai::{AiCapability, AiInputKind, AiOutputKind};
use crate::service::embedding::EmbeddingEngine;

/// Build the Phase-3 capability descriptor list.
pub fn build_capability_descriptors(
    engine: &dyn EmbeddingEngine,
    max_payload_bytes: usize,
) -> Vec<AiCapability> {
    let info = engine.model_info();

    let local_semantic = AiCapability {
        task_id: "semantic.embed_*".to_string(),
        provider_id: "local".to_string(),
        model_id: info.model_id.clone(),
        input_kinds: vec![
            AiInputKind::AiInputImage as i32,
            AiInputKind::AiInputThumbnail as i32,
        ],
        output_kinds: vec![AiOutputKind::AiOutputEmbedding as i32],
        supports_batch: true,
        supports_cancel: true,
        requires_credential: false,
        max_payload_bytes: max_payload_bytes as i64,
    };

    let remote_image = AiCapability {
        task_id: "image_understanding.describe".to_string(),
        provider_id: "remote".to_string(),
        model_id: "unconfigured".to_string(),
        input_kinds: vec![
            AiInputKind::AiInputThumbnail as i32,
            AiInputKind::AiInputPreview as i32,
            AiInputKind::AiInputImage as i32,
        ],
        output_kinds: vec![
            AiOutputKind::AiOutputCaption as i32,
            AiOutputKind::AiOutputTags as i32,
            AiOutputKind::AiOutputScore as i32,
        ],
        supports_batch: false,
        supports_cancel: true,
        requires_credential: true,
        max_payload_bytes: 0,
    };

    vec![local_semantic, remote_image]
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::service::embedding::MockEmbeddingEngine;

    #[test]
    fn builds_local_and_remote_descriptors() {
        let engine = MockEmbeddingEngine;
        let caps = build_capability_descriptors(&engine, 4096);
        assert_eq!(caps.len(), 2);

        let local = &caps[0];
        assert_eq!(local.task_id, "semantic.embed_*");
        assert_eq!(local.provider_id, "local");
        assert_eq!(local.model_id, "mock-model-v1");
        assert_eq!(
            local.input_kinds,
            vec![
                AiInputKind::AiInputImage as i32,
                AiInputKind::AiInputThumbnail as i32,
            ]
        );
        assert_eq!(local.output_kinds, vec![AiOutputKind::AiOutputEmbedding as i32]);
        assert!(local.supports_batch);
        assert!(local.supports_cancel);
        assert!(!local.requires_credential);
        assert_eq!(local.max_payload_bytes, 4096);

        let remote = &caps[1];
        assert_eq!(remote.task_id, "image_understanding.describe");
        assert_eq!(remote.provider_id, "remote");
        assert_eq!(remote.model_id, "unconfigured");
        assert!(!remote.supports_batch);
        assert!(remote.supports_cancel);
        assert!(remote.requires_credential);
        assert_eq!(remote.max_payload_bytes, 0);
    }
}
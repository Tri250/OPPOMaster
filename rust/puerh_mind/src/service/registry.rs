use std::sync::Arc;

use tonic::transport::Server;

use crate::config::AppConfig;
use crate::proto::common::health_service_server::HealthServiceServer;
use crate::proto::semantic::model_manager_service_server::ModelManagerServiceServer;
use crate::proto::semantic::semantic_service_server::SemanticServiceServer;
use crate::server::health::HealthServiceImpl;
use crate::server::model_manager::ModelManagerServiceImpl;
use crate::server::semantic::SemanticServiceImpl;
use crate::service::embedding::{EmbeddingEngine, EngineModelInfo, UnavailableEmbeddingEngine};
use crate::service::model_assets::REQUIRED_EMBEDDING_DIMENSION;
use crate::service::ort_clip::OrtClipEngine;
use tracing::warn;

const FILE_DESCRIPTOR_SET: &[u8] = tonic::include_file_descriptor_set!("semantic_descriptor");
pub fn register_services(
    mut builder: Server,
    config: &AppConfig,
    semantic_engine: Arc<dyn EmbeddingEngine>,
) -> anyhow::Result<tonic::transport::server::Router> {
    let health_service = HealthServiceImpl;
    let semantic_service = SemanticServiceImpl::new(
        semantic_engine,
        config.semantic.batch_cap,
        std::time::Duration::from_millis(config.semantic.batch_wait_ms),
    );
    let model_manager_service =
        ModelManagerServiceImpl::new(&config.semantic.model_root, &config.semantic.hf_endpoint);

    let reflection_service = tonic_reflection::server::Builder::configure()
        .register_encoded_file_descriptor_set(FILE_DESCRIPTOR_SET)
        .build_v1alpha()
        .expect("failed to build reflection service");

    Ok(builder
        .add_service(reflection_service)
        .add_service(HealthServiceServer::new(health_service))
        .add_service(ModelManagerServiceServer::new(model_manager_service))
        .add_service(
            SemanticServiceServer::new(semantic_service)
                .max_decoding_message_size(config.max_message_bytes)
                .max_encoding_message_size(config.max_message_bytes),
        ))
}

pub fn build_semantic_engine(config: &AppConfig) -> Arc<dyn EmbeddingEngine> {
    let semantic_engine: Arc<dyn EmbeddingEngine> = match OrtClipEngine::new(&config.semantic) {
        Ok(engine) => Arc::new(engine),
        Err(err) => {
            warn!(
                "semantic inference model is unavailable; model-manager RPCs remain available: {err}"
            );
            Arc::new(UnavailableEmbeddingEngine::new(
                EngineModelInfo {
                    profile_id: config.semantic.model_id.clone(),
                    model_id: config.semantic.model_id.clone(),
                    revision: config.semantic.revision.clone(),
                    engine_profile_id: String::new(),
                    language: String::new(),
                    embedding_dim: REQUIRED_EMBEDDING_DIMENSION,
                    native_embedding_dim: REQUIRED_EMBEDDING_DIMENSION,
                    image_size: 0,
                    embedding_transform: String::new(),
                    provider: "unavailable".to_string(),
                    model_root: config.semantic.model_root.clone(),
                    prototype_config_hash: String::new(),
                },
                err.to_string(),
            ))
        }
    };
    semantic_engine
}

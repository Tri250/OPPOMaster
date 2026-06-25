use tonic::transport::Server;
use tracing::info;

use crate::config::AppConfig;
use crate::proto::alcedo::ai::AiCapability;
use crate::service::cancellation_registry::CancellationRegistry;
use crate::service::credential_vault::CredentialVault;
use crate::service::embedding::EmbeddingEngine;
use crate::service::registry::register_services;
use std::sync::Arc;

pub async fn start_server(
    config: AppConfig,
    semantic_engine: Arc<dyn EmbeddingEngine>,
    vault: Arc<CredentialVault>,
    cancel_registry: Arc<CancellationRegistry>,
    capabilities: Vec<AiCapability>,
) -> Result<(), Box<dyn std::error::Error>> {
    let addr = config.listen_addr().parse()?;

    info!("starting alcedo_mind on {}", addr);

    let router = register_services(
        Server::builder(),
        &config,
        semantic_engine,
        vault,
        cancel_registry,
        capabilities,
    )?;

    router.serve(addr).await?;

    Ok(())
}
use tonic::transport::Server;
use tracing::info;

use crate::config::AppConfig;
use crate::service::embedding::EmbeddingEngine;
use crate::service::registry::register_services;
use std::sync::Arc;

pub async fn start_server(
    config: AppConfig,
    semantic_engine: Arc<dyn EmbeddingEngine>,
) -> Result<(), Box<dyn std::error::Error>> {
    let addr = config.listen_addr().parse()?;

    info!("staring alcedo_mind on {}", addr);

    let router = register_services(Server::builder(), &config, semantic_engine)?;

    router.serve(addr).await?;

    Ok(())
}

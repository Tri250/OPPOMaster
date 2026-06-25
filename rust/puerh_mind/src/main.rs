mod bootstrap;
mod config;
mod logging;
mod proto;
mod server;
mod service;

use anyhow::{Context, anyhow};
use std::sync::Arc;
use std::time::Duration;

const SERVER_THREAD_STACK_BYTES: usize = 64 * 1024 * 1024;

fn main() -> anyhow::Result<()> {
    let server_thread = std::thread::Builder::new()
        .name("alcedo-mind-server".to_string())
        .stack_size(SERVER_THREAD_STACK_BYTES)
        .spawn(run_server)
        .context("failed to spawn alcedo_mind server thread")?;

    server_thread
        .join()
        .map_err(|_| anyhow!("alcedo_mind server thread panicked"))?
}

fn run_server() -> anyhow::Result<()> {
    logging::init_logging();
    let config = config::AppConfig::load()?;
    let semantic_engine = service::inference::build_semantic_engine(&config);
    let credential_vault = Arc::new(service::credential_vault::CredentialVault::new(
        if config.credential_ttl_ms == 0 {
            None
        } else {
            Some(Duration::from_millis(config.credential_ttl_ms))
        },
    ));
    let cancel_registry = Arc::new(service::cancellation_registry::CancellationRegistry::new());
    let capabilities = service::capabilities::build_capability_descriptors(
        &*semantic_engine,
        config.max_message_bytes,
    );
    let runtime = tokio::runtime::Builder::new_multi_thread()
        .enable_all()
        .build()
        .context("failed to initialize alcedo_mind tokio runtime")?;
    runtime
        .block_on(bootstrap::start_server(
            config,
            semantic_engine,
            credential_vault,
            cancel_registry,
            capabilities,
        ))
        .map_err(|err| anyhow!("{err}"))
}

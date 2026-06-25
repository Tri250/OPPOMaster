mod bootstrap;
mod config;
mod logging;
mod proto;
mod server;
mod service;

use anyhow::{Context, anyhow};
use std::collections::HashMap;
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
    // Phase 5a: load built-in provider configs plus optional user-provider configs
    // from the configured directory. An invalid built-in is a hard error (the binary
    // is broken); invalid user configs are skipped with a warning by the loader.
    let provider_registry = service::provider_config::load_provider_configs(
        config.provider_config_dir.as_deref().map(std::path::Path::new),
    )
    .context("failed to load provider configs")?;

    // Phase 5b: register the mock image-analysis provider. It returns valid typed
    // results without HTTP and advertises a no-credential capability. Real
    // providers (OpenRouter, Volcengine Ark) are wired in Phase 5c; until then a
    // request for an unregistered provider_id returns UNSUPPORTED_TASK in-header.
    let mock_provider: Arc<dyn service::image_analysis::ImageAnalysisProvider> =
        Arc::new(service::image_analysis::MockImageAnalysisProvider::new(
            "mock",
            "alcedo-mock",
        ));
    let mock_capability = mock_provider.capability();
    let mut image_providers: HashMap<String, Arc<dyn service::image_analysis::ImageAnalysisProvider>> =
        HashMap::new();
    image_providers.insert(mock_provider.provider_id().to_string(), mock_provider.clone());
    let default_image_provider_id = mock_provider.provider_id().to_string();

    let capabilities = service::capabilities::build_capability_descriptors(
        &*semantic_engine,
        config.max_message_bytes,
        &provider_registry,
        // The mock provider advertises its own no-credential image-analysis capability.
        &[mock_capability],
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
            image_providers,
            default_image_provider_id,
        ))
        .map_err(|err| anyhow!("{err}"))
}

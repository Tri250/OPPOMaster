//! Phase 5c remote image-analysis HTTP drivers.
//!
//! Each driver implements [`crate::service::image_analysis::ImageAnalysisProvider`]
//! against a specific provider family's REST API:
//! - [`openrouter::OpenRouterChatProvider`] — `openrouter_chat`, OpenAI
//!   Chat-compatible `/chat/completions`.
//! - [`volcengine_ark::VolcengineArkResponsesProvider`] — `volcengine_ark_responses`,
//!   OpenAI Responses-compatible `/api/v3/responses`.
//!
//! Both reuse the shared rustls client, image->data-URI encoding, bounded retry,
//! strict-schema injection, and redaction discipline from [`http_util`]. The
//! drivers are constructed from validated `ProviderConfig`s at startup
//! ([`build_real_image_providers`]); the secret is resolved from the credential
//! vault per request and never travels through args, options, or logs.

pub mod http_util;
pub mod openrouter;
pub mod volcengine_ark;
pub mod anthropic_messages;

#[cfg(test)]
mod live_smoke;

use std::collections::HashMap;
use std::sync::Arc;

use tracing::warn;

use crate::service::image_analysis::ImageAnalysisProvider;
use crate::service::provider_config::{ProviderConfig, ProviderRegistry};

/// Construct the wired remote image-analysis providers from the loaded registry.
///
/// Only the shipped driver families are constructed here; a config naming a
/// reserved-but-unimplemented driver (e.g. `openai_responses`,
/// `gemini_generate_content`) is skipped with a warning — fail closed, so a request
/// for that `provider_id` returns `UNSUPPORTED_TASK` in-header. A construction
/// failure (e.g. the rustls client could not be built) is likewise skipped with a
/// warning. The returned map is keyed by `provider_id`; `main.rs` merges it with
/// the mock provider and keeps the mock as the default.
///
/// This is the seam for the accepted "advertised-but-unregistered-provider" risk:
/// a capability descriptor (Phase 5a) may advertise a model whose driver is not yet
/// wired. For the shipped built-ins (openrouter, volcengine_ark,
/// volcengine_ark_coding) the driver IS wired, so advertisement and registration
/// align; the risk only surfaces for user-supplied configs naming reserved driver
/// ids.
pub fn build_real_image_providers(
    registry: &ProviderRegistry,
) -> HashMap<String, Arc<dyn ImageAnalysisProvider>> {
    let mut providers: HashMap<String, Arc<dyn ImageAnalysisProvider>> = HashMap::new();
    for config in registry.iter() {
        match build_one(config) {
            Ok(provider) => {
                providers.insert(config.provider_id.clone(), provider);
            }
            Err(err) => {
                warn!(
                    provider = %config.provider_id,
                    driver = %config.driver,
                    error = %err,
                    "skipping image-analysis provider; not registered"
                );
            }
        }
    }
    providers
}

fn build_one(config: &ProviderConfig) -> Result<Arc<dyn ImageAnalysisProvider>, String> {
    match config.driver.as_str() {
        "openrouter_chat" => {
            let p = openrouter::OpenRouterChatProvider::new(config.clone())
                .map_err(|e| format!("failed to build openrouter provider: {e}"))?;
            Ok(Arc::new(p))
        }
        "volcengine_ark_responses" => {
            let p = volcengine_ark::VolcengineArkResponsesProvider::new(config.clone())
                .map_err(|e| format!("failed to build volcengine ark provider: {e}"))?;
            Ok(Arc::new(p))
        }
        "anthropic_messages" => {
            let p = anthropic_messages::AnthropicMessagesProvider::new(config.clone())
                .map_err(|e| format!("failed to build anthropic messages provider: {e}"))?;
            Ok(Arc::new(p))
        }
        other => Err(format!("driver {other:?} is not wired in this build")),
    }
}
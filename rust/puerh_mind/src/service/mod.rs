#[cfg(target_os = "macos")]
pub mod coreml_clip;
pub mod embedding;
pub mod inference;
pub mod model_adapters;
pub mod registry;
pub mod capabilities;
pub mod cancellation_registry;
pub mod credential_vault;

pub mod model_assets;
pub mod ort_clip;
pub mod ort_runtime;
pub mod provider_config;
pub mod image_analysis;

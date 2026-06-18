use std::{
    fs::File,
    io::Read,
    path::{Path, PathBuf},
};

use anyhow::{Context, bail};
use serde::{Deserialize, Serialize};
use sha2::{Digest, Sha256};

pub const MOBILECLIP2_ONNX_REPO: &str = "plhery/mobileclip2-onnx";
pub const MOBILECLIP2_ONNX_REVISION: &str = "ba95759a5bdbaca53e9111e2550a76ec09c8fd9e";
pub const MOBILECLIP2_ONNX_PROFILE: &str = "mobileclip2-s2-en";
pub const MOBILECLIP2_ONNX_MODEL_ID: &str = "plhery/mobileclip2-onnx:s2";
#[allow(dead_code)]
pub const MOBILECLIP2_ONNX_VARIANT: &str = "onnx/s2";
pub const RESOLVED_MANIFEST_FILE: &str = "alcedo_model_manifest.json";
pub const REQUIRED_EMBEDDING_DIMENSION: u32 = 512;

#[derive(Debug, Clone, Copy, PartialEq, Eq, Serialize, Deserialize)]
#[serde(rename_all = "snake_case")]
pub enum AssetRole {
    TextModel,
    VisionModel,
    MultimodalModel,
    OnnxConfig,
    ModelConfig,
    PreprocessConfig,
    Tokenizer,
    TokenizerConfig,
    Vocab,
    SpecialTokens,
}

impl AssetRole {
    pub fn as_str(self) -> &'static str {
        match self {
            AssetRole::TextModel => "text_model",
            AssetRole::VisionModel => "vision_model",
            AssetRole::MultimodalModel => "multimodal_model",
            AssetRole::OnnxConfig => "onnx_config",
            AssetRole::ModelConfig => "model_config",
            AssetRole::PreprocessConfig => "preprocess_config",
            AssetRole::Tokenizer => "tokenizer",
            AssetRole::TokenizerConfig => "tokenizer_config",
            AssetRole::Vocab => "vocab",
            AssetRole::SpecialTokens => "special_tokens",
        }
    }
}

#[derive(Debug, Clone, Copy, PartialEq, Eq, Serialize, Deserialize)]
#[serde(rename_all = "snake_case")]
pub enum ModelLanguage {
    En,
    Zh,
    Multilingual,
}

impl ModelLanguage {
    pub fn as_str(self) -> &'static str {
        match self {
            ModelLanguage::En => "en",
            ModelLanguage::Zh => "zh",
            ModelLanguage::Multilingual => "multilingual",
        }
    }
}

#[derive(Debug, Clone)]
pub struct ModelAssetSpec {
    pub role: AssetRole,
    pub repo_id: &'static str,
    pub revision: &'static str,
    pub remote_path: &'static str,
    pub local_path: &'static str,
    pub size_bytes: u64,
    pub sha256: Option<&'static str>,
}

#[derive(Debug, Clone)]
pub struct ModelProfileSpec {
    pub profile_id: &'static str,
    pub display_name: &'static str,
    pub model_id: &'static str,
    pub revision: &'static str,
    pub engine_profile_id: &'static str,
    pub language: ModelLanguage,
    pub embedding_dimension: u32,
    pub native_embedding_dimension: u32,
    pub image_size: u32,
    pub embedding_transform: &'static str,
    pub assets: &'static [ModelAssetSpec],
}

#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
pub struct ResolvedAssetManifest {
    pub role: String,
    pub repo_id: String,
    pub revision: String,
    pub remote_path: String,
    pub local_path: String,
    pub size_bytes: u64,
    pub sha256: String,
}

#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
pub struct ResolvedModelManifest {
    pub profile_id: String,
    pub model_id: String,
    pub revision: String,
    pub engine_profile_id: String,
    pub language: String,
    pub embedding_dimension: u32,
    pub native_embedding_dimension: u32,
    pub image_size: u32,
    pub embedding_transform: String,
    pub model_root: String,
    pub assets: Vec<ResolvedAssetManifest>,
}

#[derive(Debug, Clone)]
pub struct ModelProfileStatus {
    pub profile: &'static ModelProfileSpec,
    pub model_root: PathBuf,
    pub installed: bool,
    pub status: String,
}

const MOBILECLIP2_ASSETS: &[ModelAssetSpec] = &[
    ModelAssetSpec {
        role: AssetRole::TextModel,
        repo_id: MOBILECLIP2_ONNX_REPO,
        revision: MOBILECLIP2_ONNX_REVISION,
        remote_path: "onnx/s2/text_model.onnx",
        local_path: "onnx/s2/text_model.onnx",
        size_bytes: 254_053_669,
        sha256: Some("622f10372bca71b5017f2efc5f8c2886610a2592b636de8984d717f03213f031"),
    },
    ModelAssetSpec {
        role: AssetRole::VisionModel,
        repo_id: MOBILECLIP2_ONNX_REPO,
        revision: MOBILECLIP2_ONNX_REVISION,
        remote_path: "onnx/s2/vision_model.onnx",
        local_path: "onnx/s2/vision_model.onnx",
        size_bytes: 143_044_797,
        sha256: Some("a841f72c5a5085748bbe271a1d5718aba877822a15cba865bdbd0d37036b849e"),
    },
    ModelAssetSpec {
        role: AssetRole::OnnxConfig,
        repo_id: MOBILECLIP2_ONNX_REPO,
        revision: MOBILECLIP2_ONNX_REVISION,
        remote_path: "onnx/s2/config.json",
        local_path: "onnx/s2/config.json",
        size_bytes: 98,
        sha256: None,
    },
    ModelAssetSpec {
        role: AssetRole::PreprocessConfig,
        repo_id: MOBILECLIP2_ONNX_REPO,
        revision: MOBILECLIP2_ONNX_REVISION,
        remote_path: "onnx/s2/preprocessor_config.json",
        local_path: "onnx/s2/preprocessor_config.json",
        size_bytes: 284,
        sha256: None,
    },
    ModelAssetSpec {
        role: AssetRole::Tokenizer,
        repo_id: MOBILECLIP2_ONNX_REPO,
        revision: MOBILECLIP2_ONNX_REVISION,
        remote_path: "tokenizer.json",
        local_path: "tokenizer.json",
        size_bytes: 2_224_041,
        sha256: None,
    },
    ModelAssetSpec {
        role: AssetRole::TokenizerConfig,
        repo_id: MOBILECLIP2_ONNX_REPO,
        revision: MOBILECLIP2_ONNX_REVISION,
        remote_path: "tokenizer_config.json",
        local_path: "tokenizer_config.json",
        size_bytes: 568,
        sha256: None,
    },
];

const JINA_CLIP_REPO: &str = "jinaai/jina-clip-v2";
const JINA_CLIP_REVISION: &str = "e10d47f5691d0454a0fb5d13f46f2199b74cb436";

// The Jina CLIP v2 export precision is platform-selected. The INT8 (quantized)
// export produces non-finite (NaN) embeddings on CoreML's GPU/ANE path, so
// macOS downloads the FP16 export instead, which is numerically correct on the
// full CoreML compute-unit range. Windows keeps the smaller INT8 export, which
// runs correctly under DirectML. The shared tokenizer/preprocessor assets are
// identical across both exports (same repo revision).
#[cfg(target_os = "macos")]
const JINA_CLIP_MULTIMODAL_ASSET: ModelAssetSpec = ModelAssetSpec {
    role: AssetRole::MultimodalModel,
    repo_id: JINA_CLIP_REPO,
    revision: JINA_CLIP_REVISION,
    remote_path: "onnx/model_fp16.onnx",
    local_path: "onnx/model_fp16.onnx",
    size_bytes: 1_728_814_880,
    sha256: Some("746a78209096d1cd52891b70d752903b8bf86088ba847bd0c56c03fb29256801"),
};
#[cfg(not(target_os = "macos"))]
const JINA_CLIP_MULTIMODAL_ASSET: ModelAssetSpec = ModelAssetSpec {
    role: AssetRole::MultimodalModel,
    repo_id: JINA_CLIP_REPO,
    revision: JINA_CLIP_REVISION,
    remote_path: "onnx/model_int8.onnx",
    local_path: "onnx/model_int8.onnx",
    size_bytes: 874_350_932,
    sha256: Some("21b8b77a009865faecaa29f076ee55d6334ea42699a9efa14d542ce8d3938a3f"),
};

#[cfg(target_os = "macos")]
const JINA_CLIP_ENGINE_PROFILE_ID: &str = "jina-clip-v2-onnx-fp16";
#[cfg(not(target_os = "macos"))]
const JINA_CLIP_ENGINE_PROFILE_ID: &str = "jina-clip-v2-onnx-int8";

#[cfg(target_os = "macos")]
const JINA_CLIP_DISPLAY_NAME: &str = "Jina CLIP v2 FP16 Multilingual";
#[cfg(not(target_os = "macos"))]
const JINA_CLIP_DISPLAY_NAME: &str = "Jina CLIP v2 INT8 Multilingual";

const JINA_CLIP_ASSETS: &[ModelAssetSpec] = &[
    JINA_CLIP_MULTIMODAL_ASSET,
    ModelAssetSpec {
        role: AssetRole::ModelConfig,
        repo_id: JINA_CLIP_REPO,
        revision: JINA_CLIP_REVISION,
        remote_path: "config.json",
        local_path: "config.json",
        size_bytes: 2_152,
        sha256: None,
    },
    ModelAssetSpec {
        role: AssetRole::PreprocessConfig,
        repo_id: JINA_CLIP_REPO,
        revision: JINA_CLIP_REVISION,
        remote_path: "preprocessor_config.json",
        local_path: "preprocessor_config.json",
        size_bytes: 584,
        sha256: None,
    },
    ModelAssetSpec {
        role: AssetRole::Tokenizer,
        repo_id: JINA_CLIP_REPO,
        revision: JINA_CLIP_REVISION,
        remote_path: "tokenizer.json",
        local_path: "tokenizer.json",
        size_bytes: 17_082_997,
        sha256: Some("6601c4120779a1a3863897ba332fe3481d548e363bec2c91eba10ef8640a5e93"),
    },
    ModelAssetSpec {
        role: AssetRole::TokenizerConfig,
        repo_id: JINA_CLIP_REPO,
        revision: JINA_CLIP_REVISION,
        remote_path: "tokenizer_config.json",
        local_path: "tokenizer_config.json",
        size_bytes: 1_148,
        sha256: None,
    },
    ModelAssetSpec {
        role: AssetRole::SpecialTokens,
        repo_id: JINA_CLIP_REPO,
        revision: JINA_CLIP_REVISION,
        remote_path: "special_tokens_map.json",
        local_path: "special_tokens_map.json",
        size_bytes: 964,
        sha256: None,
    },
];

pub const MODEL_PROFILES: &[ModelProfileSpec] = &[
    ModelProfileSpec {
        profile_id: MOBILECLIP2_ONNX_PROFILE,
        display_name: "MobileCLIP2 S2 English",
        model_id: MOBILECLIP2_ONNX_MODEL_ID,
        revision: MOBILECLIP2_ONNX_REVISION,
        engine_profile_id: "mobileclip2-openclip",
        language: ModelLanguage::En,
        embedding_dimension: REQUIRED_EMBEDDING_DIMENSION,
        native_embedding_dimension: REQUIRED_EMBEDDING_DIMENSION,
        image_size: 256,
        embedding_transform: "l2_normalize",
        assets: MOBILECLIP2_ASSETS,
    },
    // `profile_id` is a stable internal key (it persists in user settings and
    // keys batch-size/timeout lookups); the underlying precision is selected
    // per platform via `JINA_CLIP_ENGINE_PROFILE_ID` / `JINA_CLIP_ASSETS`.
    ModelProfileSpec {
        profile_id: "jina-clip-v2-int8-multilingual",
        display_name: JINA_CLIP_DISPLAY_NAME,
        model_id: JINA_CLIP_REPO,
        revision: JINA_CLIP_REVISION,
        engine_profile_id: JINA_CLIP_ENGINE_PROFILE_ID,
        language: ModelLanguage::Multilingual,
        embedding_dimension: REQUIRED_EMBEDDING_DIMENSION,
        native_embedding_dimension: 1024,
        image_size: 512,
        embedding_transform: "matryoshka_truncate_then_l2_normalize",
        assets: JINA_CLIP_ASSETS,
    },
];

#[allow(dead_code)]
pub struct ClipModelPaths {
    pub root: PathBuf,
    pub text_model: PathBuf,
    pub vision_model: PathBuf,
    pub onnx_config: PathBuf,
    pub preprocess_config: PathBuf,
    pub tokenizer_json: PathBuf,
    pub tokenizer_config: PathBuf,
}

impl ClipModelPaths {
    #[allow(dead_code)]
    pub fn from_root(root: impl Into<PathBuf>) -> Self {
        let root = root.into();
        let onnx_root = root.join(MOBILECLIP2_ONNX_VARIANT);
        Self {
            text_model: onnx_root.join("text_model.onnx"),
            vision_model: onnx_root.join("vision_model.onnx"),
            onnx_config: onnx_root.join("config.json"),
            preprocess_config: onnx_root.join("preprocessor_config.json"),
            tokenizer_json: root.join("tokenizer.json"),
            tokenizer_config: root.join("tokenizer_config.json"),
            root,
        }
    }

    /// Validates that the MobileCLIP2 profile is present on disk.
    ///
    /// Asset downloading is handled by the C++ application layer (via aria2);
    /// the `allow_download` and `hf_endpoint` parameters are retained for call
    /// site compatibility but no longer trigger any download.
    #[allow(dead_code)]
    pub fn ensure_present(
        &self,
        _revision: &str,
        _hf_endpoint: &str,
        _allow_download: bool,
    ) -> anyhow::Result<()> {
        self.validate()
    }

    #[allow(dead_code)]
    pub fn validate(&self) -> anyhow::Result<()> {
        validate_model_profile(MOBILECLIP2_ONNX_PROFILE, &self.root).map(|_| ())
    }
}

pub fn default_model_root(profile_id: &str) -> PathBuf {
    std::env::current_dir()
        .unwrap_or_else(|_| PathBuf::from("."))
        .join("models")
        .join(profile_id)
}

pub fn find_profile(profile_id: &str) -> anyhow::Result<&'static ModelProfileSpec> {
    MODEL_PROFILES
        .iter()
        .find(|profile| profile.profile_id == profile_id || profile.model_id == profile_id)
        .ok_or_else(|| anyhow::anyhow!("unknown semantic model profile {profile_id:?}"))
}

pub fn find_profile_asset(
    profile: &ModelProfileSpec,
    role: AssetRole,
) -> anyhow::Result<&'static ModelAssetSpec> {
    profile
        .assets
        .iter()
        .find(|asset| asset.role == role)
        .ok_or_else(|| {
            anyhow::anyhow!(
                "semantic model profile {} does not define a {} asset",
                profile.profile_id,
                role.as_str()
            )
        })
}

pub fn profile_asset_path(
    profile: &ModelProfileSpec,
    root: impl AsRef<Path>,
    role: AssetRole,
) -> anyhow::Result<PathBuf> {
    Ok(root
        .as_ref()
        .join(find_profile_asset(profile, role)?.local_path))
}

pub fn profile_status(
    profile: &'static ModelProfileSpec,
    root: impl AsRef<Path>,
) -> ModelProfileStatus {
    match validate_profile_assets(profile, root.as_ref()) {
        Ok(()) => ModelProfileStatus {
            profile,
            model_root: root.as_ref().to_path_buf(),
            installed: true,
            status: "installed".to_string(),
        },
        Err(err) => ModelProfileStatus {
            profile,
            model_root: root.as_ref().to_path_buf(),
            installed: false,
            status: err.to_string(),
        },
    }
}

pub fn list_profiles(root: Option<&Path>) -> Vec<ModelProfileStatus> {
    MODEL_PROFILES
        .iter()
        .map(|profile| {
            let model_root = root
                .map(|base| base.join(profile.profile_id))
                .unwrap_or_else(|| default_model_root(profile.profile_id));
            profile_status(profile, model_root)
        })
        .collect()
}

pub fn list_installed_profiles(root: Option<&Path>) -> Vec<ModelProfileStatus> {
    list_profiles(root)
        .into_iter()
        .filter(|status| status.installed)
        .collect()
}

pub fn validate_model_profile(
    profile_id: &str,
    root: impl AsRef<Path>,
) -> anyhow::Result<ResolvedModelManifest> {
    let profile = find_profile(profile_id)?;
    validate_profile_assets(profile, root.as_ref())?;
    let manifest = resolved_manifest(profile, root.as_ref());
    let manifest_path = root.as_ref().join(RESOLVED_MANIFEST_FILE);
    if manifest_path.exists() {
        let text = std::fs::read_to_string(&manifest_path)
            .with_context(|| format!("failed to read {}", manifest_path.display()))?;
        let stored: ResolvedModelManifest = serde_json::from_str(&text)
            .with_context(|| format!("failed to parse {}", manifest_path.display()))?;
        if stored.profile_id != manifest.profile_id
            || stored.model_id != manifest.model_id
            || stored.revision != manifest.revision
            || stored.engine_profile_id != manifest.engine_profile_id
            || stored.embedding_dimension != REQUIRED_EMBEDDING_DIMENSION
            || stored.native_embedding_dimension != manifest.native_embedding_dimension
            || stored.image_size != manifest.image_size
            || stored.embedding_transform != manifest.embedding_transform
        {
            bail!(
                "resolved model manifest mismatch for {}",
                manifest_path.display()
            );
        }
    }
    Ok(manifest)
}

pub fn delete_model_profile(profile_id: &str, root: impl AsRef<Path>) -> anyhow::Result<()> {
    let _ = find_profile(profile_id)?;
    let staging = staging_root(root.as_ref());
    if staging.exists() {
        std::fs::remove_dir_all(&staging)
            .with_context(|| format!("failed to remove {}", staging.display()))?;
    }
    if root.as_ref().exists() {
        std::fs::remove_dir_all(root.as_ref())
            .with_context(|| format!("failed to remove {}", root.as_ref().display()))?;
    }
    Ok(())
}

fn validate_profile_dimension(profile: &ModelProfileSpec) -> anyhow::Result<()> {
    if profile.embedding_dimension != REQUIRED_EMBEDDING_DIMENSION {
        bail!(
            "semantic model profile {} reports {} dimensions; Alcedo currently requires {}-dimensional embeddings",
            profile.profile_id,
            profile.embedding_dimension,
            REQUIRED_EMBEDDING_DIMENSION
        );
    }
    Ok(())
}

fn validate_profile_assets(profile: &ModelProfileSpec, root: &Path) -> anyhow::Result<()> {
    validate_profile_dimension(profile)?;
    if !root.exists() {
        bail!("missing model root directory: {}", root.display());
    }

    for asset in profile.assets {
        let local_path = root.join(asset.local_path);
        if !local_path.exists() {
            bail!(
                "missing {} file: {}",
                asset.role.as_str(),
                local_path.display()
            );
        }
        validate_asset_file(asset, &local_path)?;
    }

    Ok(())
}

fn validate_asset_file(asset: &ModelAssetSpec, local_path: &Path) -> anyhow::Result<()> {
    let metadata = std::fs::metadata(local_path)
        .with_context(|| format!("failed to stat {}", local_path.display()))?;
    if metadata.len() != asset.size_bytes {
        bail!(
            "{} size mismatch: expected {} bytes, got {} bytes at {}",
            asset.local_path,
            asset.size_bytes,
            metadata.len(),
            local_path.display()
        );
    }

    if let Some(expected_sha256) = asset.sha256 {
        let actual = sha256_file(local_path)?;
        if !actual.eq_ignore_ascii_case(expected_sha256) {
            bail!(
                "{} sha256 mismatch: expected {}, got {}",
                local_path.display(),
                expected_sha256,
                actual
            );
        }
    }
    Ok(())
}

fn resolved_manifest(profile: &ModelProfileSpec, root: &Path) -> ResolvedModelManifest {
    ResolvedModelManifest {
        profile_id: profile.profile_id.to_string(),
        model_id: profile.model_id.to_string(),
        revision: profile.revision.to_string(),
        engine_profile_id: profile.engine_profile_id.to_string(),
        language: profile.language.as_str().to_string(),
        embedding_dimension: profile.embedding_dimension,
        native_embedding_dimension: profile.native_embedding_dimension,
        image_size: profile.image_size,
        embedding_transform: profile.embedding_transform.to_string(),
        model_root: root.to_string_lossy().into_owned(),
        assets: profile
            .assets
            .iter()
            .map(|asset| ResolvedAssetManifest {
                role: asset.role.as_str().to_string(),
                repo_id: asset.repo_id.to_string(),
                revision: asset.revision.to_string(),
                remote_path: asset.remote_path.to_string(),
                local_path: root.join(asset.local_path).to_string_lossy().into_owned(),
                size_bytes: asset.size_bytes,
                sha256: asset.sha256.unwrap_or_default().to_string(),
            })
            .collect(),
    }
}

#[cfg(test)]
fn profile_total_bytes(profile: &ModelProfileSpec) -> u64 {
    profile.assets.iter().map(|asset| asset.size_bytes).sum()
}

#[cfg(test)]
fn completed_staging_bytes(profile: &ModelProfileSpec, staging: &Path) -> u64 {
    profile
        .assets
        .iter()
        .filter_map(|asset| {
            let local_path = staging.join(asset.local_path);
            if local_path.exists() && validate_asset_file(asset, &local_path).is_ok() {
                Some(asset.size_bytes)
            } else {
                None
            }
        })
        .sum()
}

fn staging_root(root: &Path) -> PathBuf {
    let file_name = root
        .file_name()
        .and_then(|name| name.to_str())
        .unwrap_or("model");
    root.with_file_name(format!(".{file_name}.download"))
}

fn sha256_file(path: &Path) -> anyhow::Result<String> {
    let mut file =
        File::open(path).with_context(|| format!("failed to open {}", path.display()))?;
    let mut hasher = Sha256::new();
    let mut buffer = [0u8; 1024 * 1024];
    loop {
        let read = file
            .read(&mut buffer)
            .with_context(|| format!("failed to read {}", path.display()))?;
        if read == 0 {
            break;
        }
        hasher.update(&buffer[..read]);
    }
    let digest = hasher.finalize();
    Ok(digest.iter().map(|byte| format!("{byte:02x}")).collect())
}

#[cfg(test)]
mod tests {
    use super::*;

    fn unique_temp_root(name: &str) -> PathBuf {
        std::env::temp_dir().join(format!(
            "{name}-{}",
            std::time::SystemTime::now()
                .duration_since(std::time::UNIX_EPOCH)
                .expect("system clock should be valid")
                .as_nanos()
        ))
    }

    #[test]
    fn validate_only_missing_model_fails_without_creating_root() {
        let root = unique_temp_root("alcedo-mind-missing-model");
        let paths = ClipModelPaths::from_root(&root);

        let err = paths
            .ensure_present(MOBILECLIP2_ONNX_REVISION, "https://hf-mirror.com", false)
            .expect_err("validate-only missing model should fail");

        assert!(err.to_string().contains("missing model root directory"));
        assert!(!root.exists());
    }

    #[test]
    fn fixed_profiles_are_512_dimensional() {
        for profile in MODEL_PROFILES {
            validate_profile_dimension(profile).expect("profile should satisfy dimension policy");
            assert_eq!(profile.embedding_dimension, REQUIRED_EMBEDDING_DIMENSION);
        }
        let jina = find_profile("jina-clip-v2-int8-multilingual").expect("jina profile exists");
        assert_eq!(jina.native_embedding_dimension, 1024);
        assert_eq!(jina.embedding_dimension, 512);
        assert_eq!(
            jina.embedding_transform,
            "matryoshka_truncate_then_l2_normalize"
        );
    }

    #[test]
    fn profile_total_bytes_matches_asset_sum() {
        for profile in MODEL_PROFILES {
            let sum: u64 = profile.assets.iter().map(|asset| asset.size_bytes).sum();
            assert_eq!(profile_total_bytes(profile), sum);
            assert!(sum > 0);
        }
    }

    #[test]
    fn profile_listing_reports_missing_models_without_downloading() {
        let root = unique_temp_root("alcedo-mind-profile-list");
        let profiles = list_profiles(Some(&root));
        assert_eq!(profiles.len(), MODEL_PROFILES.len());
        assert!(profiles.iter().all(|profile| !profile.installed));
        assert!(!root.exists());
    }

    #[test]
    fn staging_root_sibling_is_hidden_download_dir() {
        let root = PathBuf::from("/tmp/alcedo/mobileclip2-s2-en");
        assert_eq!(
            staging_root(&root),
            PathBuf::from("/tmp/alcedo/.mobileclip2-s2-en.download")
        );
    }

    #[test]
    fn completed_staging_bytes_zero_for_missing_profile() {
        let root = unique_temp_root("alcedo-mind-staging-bytes");
        let staging = staging_root(&root);
        let profile = find_profile(MOBILECLIP2_ONNX_PROFILE).expect("profile exists");
        assert_eq!(completed_staging_bytes(profile, &staging), 0);
    }
}

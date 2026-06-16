use std::{
    fs::{File, OpenOptions},
    io::{Read, Write},
    path::{Path, PathBuf},
    sync::{
        Arc,
        atomic::{AtomicBool, Ordering},
        mpsc,
    },
    time::Duration,
};

use anyhow::{Context, bail};
use serde::{Deserialize, Serialize};
use sha2::{Digest, Sha256};

const DIRECT_DOWNLOAD_MAX_ATTEMPTS: usize = 5;
const DIRECT_DOWNLOAD_MAX_REDIRECTS: usize = 10;
const DIRECT_DOWNLOAD_DEFAULT_THREADS: usize = 8;
const DIRECT_DOWNLOAD_MAX_THREADS: usize = 16;
const DIRECT_DOWNLOAD_WORKER_STACK_BYTES: usize = 16 * 1024 * 1024;
const DIRECT_DOWNLOAD_MIN_CHUNK_BYTES: u64 = 8 * 1024 * 1024;
const DIRECT_DOWNLOAD_PARALLEL_THRESHOLD_BYTES: u64 = 32 * 1024 * 1024;
const DIRECT_DOWNLOAD_PROGRESS_POLL_MS: u64 = 100;

pub const MOBILECLIP2_ONNX_REPO: &str = "plhery/mobileclip2-onnx";
pub const MOBILECLIP2_ONNX_REVISION: &str = "ba95759a5bdbaca53e9111e2550a76ec09c8fd9e";
pub const MOBILECLIP2_ONNX_PROFILE: &str = "mobileclip2-s2-en";
pub const MOBILECLIP2_ONNX_MODEL_ID: &str = "plhery/mobileclip2-onnx:s2";
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

#[derive(Debug, Clone, Default, PartialEq, Eq)]
pub struct ModelDownloadProgress {
    pub phase: String,
    pub current_file: String,
    pub current_file_bytes_downloaded: u64,
    pub current_file_bytes_total: u64,
    pub bytes_downloaded: u64,
    pub bytes_total: u64,
    pub files_completed: u32,
    pub files_total: u32,
    pub message: String,
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

const CHINESE_CLIP_MODEL_REPO: &str = "felixdu/chinese-clip-vit-base-patch16-onnx";
const CHINESE_CLIP_MODEL_REVISION: &str = "47080d16c631d8416d2e6b155c59f8fd2c322e98";
const CHINESE_CLIP_BASE_REPO: &str = "OFA-Sys/chinese-clip-vit-base-patch16";
const CHINESE_CLIP_BASE_REVISION: &str = "36e679e65c2a2fead755ae21162091293ad37834";
const CHINESE_CLIP_ASSETS: &[ModelAssetSpec] = &[
    ModelAssetSpec {
        role: AssetRole::TextModel,
        repo_id: CHINESE_CLIP_MODEL_REPO,
        revision: CHINESE_CLIP_MODEL_REVISION,
        remote_path: "cn_clip_text.onnx",
        local_path: "cn_clip_text.onnx",
        size_bytes: 409_585_120,
        sha256: Some("5ddc2d8971b09acda8063048003d18cca9b587f089a1c0a4df7d35624bd0fcea"),
    },
    ModelAssetSpec {
        role: AssetRole::VisionModel,
        repo_id: CHINESE_CLIP_MODEL_REPO,
        revision: CHINESE_CLIP_MODEL_REVISION,
        remote_path: "cn_clip_vision.onnx",
        local_path: "cn_clip_vision.onnx",
        size_bytes: 345_839_119,
        sha256: Some("980020bf226e528a202de26c6a6186df125b74c612b09cfcd797a082e046b628"),
    },
    ModelAssetSpec {
        role: AssetRole::ModelConfig,
        repo_id: CHINESE_CLIP_BASE_REPO,
        revision: CHINESE_CLIP_BASE_REVISION,
        remote_path: "config.json",
        local_path: "base/config.json",
        size_bytes: 3_008,
        sha256: None,
    },
    ModelAssetSpec {
        role: AssetRole::PreprocessConfig,
        repo_id: CHINESE_CLIP_BASE_REPO,
        revision: CHINESE_CLIP_BASE_REVISION,
        remote_path: "preprocessor_config.json",
        local_path: "base/preprocessor_config.json",
        size_bytes: 342,
        sha256: None,
    },
    ModelAssetSpec {
        role: AssetRole::Vocab,
        repo_id: CHINESE_CLIP_BASE_REPO,
        revision: CHINESE_CLIP_BASE_REVISION,
        remote_path: "vocab.txt",
        local_path: "base/vocab.txt",
        size_bytes: 109_540,
        sha256: None,
    },
];

const JINA_CLIP_REPO: &str = "jinaai/jina-clip-v2";
const JINA_CLIP_REVISION: &str = "e10d47f5691d0454a0fb5d13f46f2199b74cb436";
const JINA_CLIP_ASSETS: &[ModelAssetSpec] = &[
    ModelAssetSpec {
        role: AssetRole::MultimodalModel,
        repo_id: JINA_CLIP_REPO,
        revision: JINA_CLIP_REVISION,
        remote_path: "onnx/model_int8.onnx",
        local_path: "onnx/model_int8.onnx",
        size_bytes: 874_350_932,
        sha256: Some("21b8b77a009865faecaa29f076ee55d6334ea42699a9efa14d542ce8d3938a3f"),
    },
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
    ModelProfileSpec {
        profile_id: "chinese-clip-vit-base-patch16-zh",
        display_name: "Chinese-CLIP ViT-B/16",
        model_id: CHINESE_CLIP_MODEL_REPO,
        revision: CHINESE_CLIP_MODEL_REVISION,
        engine_profile_id: "chinese-clip-vit-base-patch16",
        language: ModelLanguage::Zh,
        embedding_dimension: REQUIRED_EMBEDDING_DIMENSION,
        native_embedding_dimension: REQUIRED_EMBEDDING_DIMENSION,
        image_size: 224,
        embedding_transform: "l2_normalize",
        assets: CHINESE_CLIP_ASSETS,
    },
    ModelProfileSpec {
        profile_id: "jina-clip-v2-int8-multilingual",
        display_name: "Jina CLIP v2 INT8 Multilingual",
        model_id: JINA_CLIP_REPO,
        revision: JINA_CLIP_REVISION,
        engine_profile_id: "jina-clip-v2-onnx-int8",
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

    pub fn ensure_present(
        &self,
        revision: &str,
        hf_endpoint: &str,
        allow_download: bool,
    ) -> anyhow::Result<()> {
        if allow_download {
            download_model_profile(
                MOBILECLIP2_ONNX_PROFILE,
                &self.root,
                hf_endpoint,
                Some(revision),
            )?;
        }

        self.validate()
    }

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

pub fn download_model_profile(
    profile_id: &str,
    root: impl AsRef<Path>,
    hf_endpoint: &str,
    revision_override: Option<&str>,
) -> anyhow::Result<ResolvedModelManifest> {
    download_model_profile_with_progress(profile_id, root, hf_endpoint, revision_override, |_| {
        Ok(())
    })
}

pub fn download_model_profile_with_progress<F>(
    profile_id: &str,
    root: impl AsRef<Path>,
    hf_endpoint: &str,
    revision_override: Option<&str>,
    mut progress: F,
) -> anyhow::Result<ResolvedModelManifest>
where
    F: FnMut(ModelDownloadProgress) -> anyhow::Result<()>,
{
    let profile = find_profile(profile_id)?;
    validate_profile_dimension(profile)?;
    let root = root.as_ref();
    if validate_profile_assets(profile, root).is_ok() {
        let manifest = resolved_manifest(profile, root);
        progress(ModelDownloadProgress {
            phase: "installed".to_string(),
            bytes_downloaded: profile_total_bytes(profile),
            bytes_total: profile_total_bytes(profile),
            files_completed: profile.assets.len() as u32,
            files_total: profile.assets.len() as u32,
            message: "model profile is already installed".to_string(),
            ..Default::default()
        })?;
        return Ok(manifest);
    }

    let staging = staging_root(root);
    if let Some(parent) = staging.parent() {
        std::fs::create_dir_all(parent).with_context(|| {
            format!(
                "failed to create model staging parent directory {}",
                parent.display()
            )
        })?;
    }
    std::fs::create_dir_all(&staging).with_context(|| {
        format!(
            "failed to create model staging directory {}",
            staging.display()
        )
    })?;

    let bytes_total = profile_total_bytes(profile);
    let files_total = profile.assets.len() as u32;
    let staged_bytes = completed_staging_bytes(profile, &staging);
    let mut bytes_completed = 0u64;
    progress(ModelDownloadProgress {
        phase: "queued".to_string(),
        bytes_downloaded: staged_bytes,
        bytes_total,
        files_total,
        message: format!("preparing model download from {hf_endpoint}"),
        ..Default::default()
    })?;

    for (index, asset) in profile.assets.iter().enumerate() {
        let staging_path = staging.join(asset.local_path);
        if staging_path.exists() && validate_asset_file(asset, &staging_path).is_ok() {
            bytes_completed = bytes_completed.saturating_add(asset.size_bytes);
            progress(ModelDownloadProgress {
                phase: "reused".to_string(),
                current_file: asset.remote_path.to_string(),
                current_file_bytes_downloaded: asset.size_bytes,
                current_file_bytes_total: asset.size_bytes,
                bytes_downloaded: bytes_completed.min(bytes_total),
                bytes_total,
                files_completed: (index + 1) as u32,
                files_total,
                message: format!("reused staged {}", asset.remote_path),
            })?;
            continue;
        }

        let final_path = root.join(asset.local_path);
        if final_path.exists() && validate_asset_file(asset, &final_path).is_ok() {
            copy_asset_atomic(&final_path, &staging_path)?;
            bytes_completed = bytes_completed.saturating_add(asset.size_bytes);
            progress(ModelDownloadProgress {
                phase: "reused".to_string(),
                current_file: asset.remote_path.to_string(),
                current_file_bytes_downloaded: asset.size_bytes,
                current_file_bytes_total: asset.size_bytes,
                bytes_downloaded: bytes_completed.min(bytes_total),
                bytes_total,
                files_completed: (index + 1) as u32,
                files_total,
                message: format!("reused installed {}", asset.remote_path),
            })?;
            continue;
        }

        let baseline_bytes = bytes_completed;
        download_asset_to_path(
            &staging,
            hf_endpoint,
            asset,
            revision_override,
            &mut |phase, file_bytes, message| {
                progress(ModelDownloadProgress {
                    phase: phase.to_string(),
                    current_file: asset.remote_path.to_string(),
                    current_file_bytes_downloaded: file_bytes.min(asset.size_bytes),
                    current_file_bytes_total: asset.size_bytes,
                    bytes_downloaded: baseline_bytes.saturating_add(file_bytes).min(bytes_total),
                    bytes_total,
                    files_completed: index as u32,
                    files_total,
                    message,
                })
            },
        )?;
        validate_asset_file(asset, &staging_path)?;
        bytes_completed = bytes_completed.saturating_add(asset.size_bytes);
        progress(ModelDownloadProgress {
            phase: "validated".to_string(),
            current_file: asset.remote_path.to_string(),
            current_file_bytes_downloaded: asset.size_bytes,
            current_file_bytes_total: asset.size_bytes,
            bytes_downloaded: bytes_completed.min(bytes_total),
            bytes_total,
            files_completed: (index + 1) as u32,
            files_total,
            message: format!("validated {}", asset.remote_path),
        })?;
    }

    validate_profile_assets(profile, &staging)?;
    progress(ModelDownloadProgress {
        phase: "promoting".to_string(),
        bytes_downloaded: bytes_total,
        bytes_total,
        files_completed: files_total,
        files_total,
        message: "promoting staged model profile".to_string(),
        ..Default::default()
    })?;
    cleanup_staging_download_partials(profile, &staging)?;
    promote_staging_root(&staging, root)?;
    validate_profile_assets(profile, root)?;
    let manifest = resolved_manifest(profile, root);
    write_resolved_manifest(&manifest)?;
    progress(ModelDownloadProgress {
        phase: "installed".to_string(),
        bytes_downloaded: bytes_total,
        bytes_total,
        files_completed: files_total,
        files_total,
        message: "model profile installed".to_string(),
        ..Default::default()
    })?;
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

fn download_asset_to_path<F>(
    root: &Path,
    hf_endpoint: &str,
    asset: &ModelAssetSpec,
    revision_override: Option<&str>,
    progress: &mut F,
) -> anyhow::Result<()>
where
    F: FnMut(&str, u64, String) -> anyhow::Result<()>,
{
    let revision = revision_override.unwrap_or(asset.revision);
    let local_path = root.join(asset.local_path);
    if local_path.exists() && validate_asset_file(asset, &local_path).is_ok() {
        progress(
            "reused",
            asset.size_bytes,
            format!("reused {}", asset.remote_path),
        )?;
        return Ok(());
    }

    if let Some(parent) = local_path.parent() {
        std::fs::create_dir_all(parent).with_context(|| {
            format!(
                "failed to create model asset directory {}",
                parent.display()
            )
        })?;
    }

    progress(
        "resolving",
        0,
        format!(
            "resolving {} from configured endpoint {}",
            asset.remote_path, hf_endpoint
        ),
    )?;
    let resolved = resolve_hf_asset_url(hf_endpoint, asset.repo_id, revision, asset.remote_path)?;
    progress(
        "resolving",
        0,
        format!(
            "resolved {}: configured source {}, active source {}",
            asset.remote_path, resolved.configured_source, resolved.active_source
        ),
    )?;
    download_asset_direct_to_path(&resolved, asset, &local_path, progress)?;

    validate_asset_file(asset, &local_path)
}

#[derive(Debug, Clone)]
struct ResolvedAssetUrl {
    url: String,
    configured_source: String,
    active_source: String,
    supports_ranges: bool,
}

fn resolve_hf_asset_url(
    hf_endpoint: &str,
    repo_id: &str,
    revision: &str,
    remote_path: &str,
) -> anyhow::Result<ResolvedAssetUrl> {
    let configured_source = classify_download_source(hf_endpoint);
    let mut active_source = configured_source.clone();
    let mut url = format!(
        "{}/{}/resolve/{}/{}",
        hf_endpoint.trim_end_matches('/'),
        repo_id,
        revision,
        remote_path
    );
    let agent = direct_download_agent(0);

    for _ in 0..=DIRECT_DOWNLOAD_MAX_REDIRECTS {
        let mut response = agent
            .get(&url)
            .header("Range", "bytes=0-0")
            .call()
            .with_context(|| format!("failed to resolve model asset URL {url}"))?;

        if response.status().is_redirection() {
            let location = response
                .headers()
                .get("Location")
                .and_then(|value| value.to_str().ok())
                .with_context(|| format!("redirect from {url} did not include Location"))?;
            url = join_redirect_url(&url, location)?;
            let redirected_source = classify_download_source(&url);
            if redirected_source == "huggingface.co" || active_source != "huggingface.co" {
                active_source = redirected_source;
            }
            continue;
        }

        if !response.status().is_success() {
            bail!(
                "failed to resolve model asset URL {}: HTTP {}",
                url,
                response.status()
            );
        }

        let supports_ranges = response.status().as_u16() == 206
            || response
                .headers()
                .get("Accept-Ranges")
                .and_then(|value| value.to_str().ok())
                .is_some_and(|value| value.eq_ignore_ascii_case("bytes"))
            || response
                .headers()
                .get("Content-Range")
                .and_then(|value| value.to_str().ok())
                .is_some_and(|value| value.to_ascii_lowercase().starts_with("bytes "));
        if active_source != "huggingface.co" {
            active_source = classify_download_source(&url);
        }
        let _ = response.body_mut().as_reader();
        return Ok(ResolvedAssetUrl {
            url,
            configured_source,
            active_source,
            supports_ranges,
        });
    }

    bail!(
        "too many redirects while resolving {} from {}",
        remote_path,
        hf_endpoint
    )
}

fn download_asset_direct_to_path<F>(
    resolved: &ResolvedAssetUrl,
    asset: &ModelAssetSpec,
    local_path: &Path,
    progress: &mut F,
) -> anyhow::Result<()>
where
    F: FnMut(&str, u64, String) -> anyhow::Result<()>,
{
    let parallelism = download_parallelism(asset.size_bytes, resolved.supports_ranges);
    if parallelism <= 1 {
        return download_asset_single_stream(resolved, asset, local_path, progress);
    }

    download_asset_parallel_ranges(resolved, asset, local_path, parallelism, progress)
}

fn download_asset_single_stream<F>(
    resolved: &ResolvedAssetUrl,
    asset: &ModelAssetSpec,
    local_path: &Path,
    progress: &mut F,
) -> anyhow::Result<()>
where
    F: FnMut(&str, u64, String) -> anyhow::Result<()>,
{
    let part_path = local_path.with_extension("part");
    let mut existing = partial_file_size(&part_path);
    if existing > asset.size_bytes || (!resolved.supports_ranges && existing > 0) {
        let _ = std::fs::remove_file(&part_path);
        existing = 0;
    }

    for attempt in 1..=DIRECT_DOWNLOAD_MAX_ATTEMPTS {
        let mut request =
            direct_download_agent(DIRECT_DOWNLOAD_MAX_REDIRECTS as u32).get(&resolved.url);
        if resolved.supports_ranges && existing > 0 {
            request = request.header("Range", format!("bytes={existing}-"));
        }

        match request.call() {
            Ok(mut response) => {
                if existing > 0 && response.status().as_u16() != 206 {
                    let _ = std::fs::remove_file(&part_path);
                    existing = 0;
                    continue;
                }
                if !response.status().is_success() {
                    bail!(
                        "failed to download {} from {}: HTTP {}",
                        asset.remote_path,
                        resolved.active_source,
                        response.status()
                    );
                }

                let mut output = OpenOptions::new()
                    .create(true)
                    .append(existing > 0)
                    .write(true)
                    .truncate(existing == 0)
                    .open(&part_path)
                    .with_context(|| format!("failed to open {}", part_path.display()))?;
                let mut downloaded = existing;
                progress(
                    "downloading",
                    downloaded,
                    format!(
                        "downloading {} from {}",
                        asset.remote_path, resolved.active_source
                    ),
                )?;
                let mut reader = response.body_mut().as_reader();
                let mut buffer = [0u8; 1024 * 1024];
                loop {
                    let read = reader
                        .read(&mut buffer)
                        .with_context(|| format!("failed to read {}", asset.remote_path))?;
                    if read == 0 {
                        break;
                    }
                    output
                        .write_all(&buffer[..read])
                        .with_context(|| format!("failed to write {}", part_path.display()))?;
                    downloaded = downloaded.saturating_add(read as u64);
                    progress(
                        "downloading",
                        downloaded.min(asset.size_bytes),
                        format!(
                            "downloading {} from {}",
                            asset.remote_path, resolved.active_source
                        ),
                    )?;
                }
                output
                    .flush()
                    .with_context(|| format!("failed to flush {}", part_path.display()))?;
                if downloaded != asset.size_bytes {
                    bail!(
                        "{} incomplete: expected {} bytes, got {} bytes",
                        asset.remote_path,
                        asset.size_bytes,
                        downloaded
                    );
                }
                std::fs::rename(&part_path, local_path).with_context(|| {
                    format!(
                        "failed to move {} to {}",
                        part_path.display(),
                        local_path.display()
                    )
                })?;
                return Ok(());
            }
            Err(err) if attempt < DIRECT_DOWNLOAD_MAX_ATTEMPTS => {
                existing = partial_file_size(&part_path);
                progress(
                    "downloading",
                    existing.min(asset.size_bytes),
                    format!(
                        "retrying {} from {} after attempt {} failed: {}",
                        asset.remote_path, resolved.active_source, attempt, err
                    ),
                )?;
            }
            Err(err) => {
                bail!(
                    "failed to download {} from {} after {} attempts: {}",
                    asset.remote_path,
                    resolved.active_source,
                    DIRECT_DOWNLOAD_MAX_ATTEMPTS,
                    err
                );
            }
        }
    }

    unreachable!("download attempts should return or bail")
}

#[derive(Debug, Clone)]
struct DownloadChunk {
    index: usize,
    start: u64,
    end: u64,
    part_path: PathBuf,
}

#[derive(Debug)]
enum ChunkEvent {
    Progress(u64),
    Done { index: usize, error: Option<String> },
}

fn download_asset_parallel_ranges<F>(
    resolved: &ResolvedAssetUrl,
    asset: &ModelAssetSpec,
    local_path: &Path,
    parallelism: usize,
    progress: &mut F,
) -> anyhow::Result<()>
where
    F: FnMut(&str, u64, String) -> anyhow::Result<()>,
{
    let chunks = plan_download_chunks(local_path, asset.size_bytes, parallelism);
    let already_downloaded = chunks
        .iter()
        .map(|chunk| partial_file_size(&chunk.part_path).min(chunk_len(chunk)))
        .sum::<u64>();
    progress(
        "downloading",
        already_downloaded.min(asset.size_bytes),
        format!(
            "downloading {} from {} with {} ranged connections",
            asset.remote_path, resolved.active_source, parallelism
        ),
    )?;

    let (event_tx, event_rx) = mpsc::channel();
    let abort_requested = Arc::new(AtomicBool::new(false));
    let mut handles = Vec::new();
    for chunk in chunks.clone() {
        if partial_file_size(&chunk.part_path) == chunk_len(&chunk) {
            continue;
        }
        let event_tx = event_tx.clone();
        let abort_requested = Arc::clone(&abort_requested);
        let url = resolved.url.clone();
        let remote_path = asset.remote_path.to_string();
        let source = resolved.active_source.clone();
        handles.push(
            std::thread::Builder::new()
                .name(format!("model-download-{}", chunk.index))
                .stack_size(DIRECT_DOWNLOAD_WORKER_STACK_BYTES)
                .spawn(move || {
                    let result = download_chunk_with_retries(
                        &url,
                        &remote_path,
                        &source,
                        &chunk,
                        Arc::clone(&abort_requested),
                        event_tx.clone(),
                    );
                    let _ = event_tx.send(ChunkEvent::Done {
                        index: chunk.index,
                        error: result.err().map(|err| err.to_string()),
                    });
                })
                .context("failed to spawn model download worker")?,
        );
    }
    drop(event_tx);

    let mut downloaded = already_downloaded;
    let mut done = 0usize;
    let mut first_error = None;
    while done < handles.len() {
        match event_rx.recv_timeout(Duration::from_millis(DIRECT_DOWNLOAD_PROGRESS_POLL_MS)) {
            Ok(ChunkEvent::Progress(delta)) => {
                downloaded = downloaded.saturating_add(delta).min(asset.size_bytes);
                if first_error.is_none()
                    && let Err(err) = progress(
                        "downloading",
                        downloaded,
                        format!(
                            "downloading {} from {} with {} ranged connections",
                            asset.remote_path, resolved.active_source, parallelism
                        ),
                    )
                {
                    abort_requested.store(true, Ordering::SeqCst);
                    first_error = Some(err.to_string());
                }
            }
            Ok(ChunkEvent::Done { index, error }) => {
                let _ = index;
                done += 1;
                if let Some(error) = error
                    && first_error.is_none()
                {
                    abort_requested.store(true, Ordering::SeqCst);
                    first_error = Some(error);
                }
            }
            Err(mpsc::RecvTimeoutError::Timeout) => {}
            Err(mpsc::RecvTimeoutError::Disconnected) => break,
        }
    }

    for handle in handles {
        handle
            .join()
            .map_err(|payload| panic_payload_to_anyhow(payload, "download worker panicked"))?;
    }

    if let Some(error) = first_error {
        bail!("{error}");
    }

    combine_download_chunks(&chunks, local_path)?;
    progress(
        "copying",
        asset.size_bytes,
        format!("assembled {}", asset.remote_path),
    )?;
    Ok(())
}

fn download_chunk_with_retries(
    url: &str,
    remote_path: &str,
    active_source: &str,
    chunk: &DownloadChunk,
    abort_requested: Arc<AtomicBool>,
    event_tx: mpsc::Sender<ChunkEvent>,
) -> anyhow::Result<()> {
    for attempt in 1..=DIRECT_DOWNLOAD_MAX_ATTEMPTS {
        if abort_requested.load(Ordering::SeqCst) {
            bail!("download cancelled");
        }
        match download_chunk_once(
            url,
            remote_path,
            chunk,
            Arc::clone(&abort_requested),
            event_tx.clone(),
        ) {
            Ok(()) => return Ok(()),
            Err(err) if attempt < DIRECT_DOWNLOAD_MAX_ATTEMPTS => {
                let _ = event_tx.send(ChunkEvent::Progress(0));
                std::thread::sleep(Duration::from_millis(250 * attempt as u64));
                let _ = err;
            }
            Err(err) => {
                bail!(
                    "failed ranged download for {} chunk {} from {} after {} attempts: {}",
                    remote_path,
                    chunk.index,
                    active_source,
                    DIRECT_DOWNLOAD_MAX_ATTEMPTS,
                    err
                );
            }
        }
    }

    unreachable!("download attempts should return or bail")
}

fn download_chunk_once(
    url: &str,
    remote_path: &str,
    chunk: &DownloadChunk,
    abort_requested: Arc<AtomicBool>,
    event_tx: mpsc::Sender<ChunkEvent>,
) -> anyhow::Result<()> {
    let expected_len = chunk_len(chunk);
    let existing = partial_file_size(&chunk.part_path);
    if existing == expected_len {
        return Ok(());
    }
    if existing > expected_len {
        std::fs::remove_file(&chunk.part_path)
            .with_context(|| format!("failed to remove {}", chunk.part_path.display()))?;
    }
    let existing = partial_file_size(&chunk.part_path);
    let start = chunk.start.saturating_add(existing);
    let range = format!("bytes={}-{}", start, chunk.end);
    let mut response = direct_download_agent(0)
        .get(url)
        .header("Range", range)
        .call()
        .with_context(|| format!("failed to request range for {remote_path}"))?;
    if response.status().as_u16() != 206 {
        bail!(
            "{} range request returned HTTP {}; expected 206",
            remote_path,
            response.status()
        );
    }

    let mut output = OpenOptions::new()
        .create(true)
        .append(existing > 0)
        .write(true)
        .truncate(existing == 0)
        .open(&chunk.part_path)
        .with_context(|| format!("failed to open {}", chunk.part_path.display()))?;
    let mut written = existing;
    let mut reader = response.body_mut().as_reader();
    let mut buffer = [0u8; 1024 * 1024];
    loop {
        if abort_requested.load(Ordering::SeqCst) {
            bail!("download cancelled");
        }
        let read = reader
            .read(&mut buffer)
            .with_context(|| format!("failed to read range for {remote_path}"))?;
        if read == 0 {
            break;
        }
        output
            .write_all(&buffer[..read])
            .with_context(|| format!("failed to write {}", chunk.part_path.display()))?;
        written = written.saturating_add(read as u64);
        let _ = event_tx.send(ChunkEvent::Progress(read as u64));
    }
    output
        .flush()
        .with_context(|| format!("failed to flush {}", chunk.part_path.display()))?;
    if written != expected_len {
        bail!(
            "{} chunk {} incomplete: expected {} bytes, got {} bytes",
            remote_path,
            chunk.index,
            expected_len,
            written
        );
    }
    Ok(())
}

fn combine_download_chunks(chunks: &[DownloadChunk], local_path: &Path) -> anyhow::Result<()> {
    let tmp_path = local_path.with_extension("part");
    let mut output = File::create(&tmp_path)
        .with_context(|| format!("failed to create {}", tmp_path.display()))?;
    let mut buffer = [0u8; 1024 * 1024];
    for chunk in chunks {
        let mut input = File::open(&chunk.part_path)
            .with_context(|| format!("failed to open {}", chunk.part_path.display()))?;
        loop {
            let read = input
                .read(&mut buffer)
                .with_context(|| format!("failed to read {}", chunk.part_path.display()))?;
            if read == 0 {
                break;
            }
            output
                .write_all(&buffer[..read])
                .with_context(|| format!("failed to write {}", tmp_path.display()))?;
        }
    }
    output
        .flush()
        .with_context(|| format!("failed to flush {}", tmp_path.display()))?;
    std::fs::rename(&tmp_path, local_path).with_context(|| {
        format!(
            "failed to move {} to {}",
            tmp_path.display(),
            local_path.display()
        )
    })?;
    for chunk in chunks {
        let _ = std::fs::remove_file(&chunk.part_path);
    }
    Ok(())
}

fn plan_download_chunks(
    local_path: &Path,
    size_bytes: u64,
    parallelism: usize,
) -> Vec<DownloadChunk> {
    let chunk_count = parallelism.max(1).min(DIRECT_DOWNLOAD_MAX_THREADS);
    let base = size_bytes / chunk_count as u64;
    let mut remainder = size_bytes % chunk_count as u64;
    let mut start = 0u64;
    let mut chunks = Vec::with_capacity(chunk_count);
    for index in 0..chunk_count {
        let mut len = base;
        if remainder > 0 {
            len += 1;
            remainder -= 1;
        }
        let end = start + len - 1;
        chunks.push(DownloadChunk {
            index,
            start,
            end,
            part_path: chunk_part_path(local_path, index),
        });
        start = end + 1;
    }
    chunks
}

fn chunk_len(chunk: &DownloadChunk) -> u64 {
    chunk.end - chunk.start + 1
}

fn chunk_part_path(local_path: &Path, index: usize) -> PathBuf {
    let file_name = local_path
        .file_name()
        .and_then(|name| name.to_str())
        .unwrap_or("asset");
    local_path.with_file_name(format!("{file_name}.part.{index}"))
}

fn download_parallelism(size_bytes: u64, supports_ranges: bool) -> usize {
    if !supports_ranges || size_bytes < DIRECT_DOWNLOAD_PARALLEL_THRESHOLD_BYTES {
        return 1;
    }
    let requested = std::env::var("ALCEDO_MIND_DOWNLOAD_THREADS")
        .ok()
        .and_then(|value| value.parse::<usize>().ok())
        .unwrap_or(DIRECT_DOWNLOAD_DEFAULT_THREADS)
        .clamp(1, DIRECT_DOWNLOAD_MAX_THREADS);
    let useful = size_bytes.div_ceil(DIRECT_DOWNLOAD_MIN_CHUNK_BYTES) as usize;
    requested.min(useful.max(1))
}

fn direct_download_agent(max_redirects: u32) -> ureq::Agent {
    ureq::Agent::config_builder()
        .max_redirects(max_redirects)
        .max_redirects_will_error(false)
        .timeout_connect(Some(Duration::from_secs(30)))
        .timeout_recv_response(Some(Duration::from_secs(60)))
        .timeout_recv_body(Some(Duration::from_secs(60)))
        .build()
        .into()
}

fn partial_file_size(path: &Path) -> u64 {
    std::fs::metadata(path)
        .map(|metadata| metadata.len())
        .unwrap_or(0)
}

fn classify_download_source(url_or_endpoint: &str) -> String {
    let value = url_or_endpoint
        .trim()
        .trim_end_matches('/')
        .to_ascii_lowercase();
    if value.starts_with("https://huggingface.co") || value.starts_with("http://huggingface.co") {
        "huggingface.co".to_string()
    } else if value.starts_with("https://hf-mirror.com")
        || value.starts_with("http://hf-mirror.com")
    {
        "hf-mirror.com".to_string()
    } else {
        value
    }
}

fn join_redirect_url(current_url: &str, location: &str) -> anyhow::Result<String> {
    if location.starts_with("http://") || location.starts_with("https://") {
        return Ok(location.to_string());
    }
    if location.starts_with('/') {
        let scheme_end = current_url
            .find("://")
            .with_context(|| format!("cannot parse redirect base URL {current_url}"))?;
        let rest = &current_url[scheme_end + 3..];
        let host_end = rest.find('/').unwrap_or(rest.len());
        let origin = &current_url[..scheme_end + 3 + host_end];
        return Ok(format!("{origin}{location}"));
    }
    let base = current_url
        .rsplit_once('/')
        .map(|(base, _)| base)
        .unwrap_or(current_url);
    Ok(format!("{base}/{location}"))
}

fn panic_payload_to_anyhow(
    payload: Box<dyn std::any::Any + Send>,
    fallback: &str,
) -> anyhow::Error {
    if let Some(message) = payload.downcast_ref::<String>() {
        anyhow::anyhow!("{message}")
    } else if let Some(message) = payload.downcast_ref::<&'static str>() {
        anyhow::anyhow!("{message}")
    } else {
        anyhow::anyhow!("{fallback}")
    }
}

fn copy_asset_atomic(source: &Path, target: &Path) -> anyhow::Result<()> {
    copy_asset_atomic_with_progress(source, target, &mut |_, _, _| Ok(()))
}

fn copy_asset_atomic_with_progress<F>(
    source: &Path,
    target: &Path,
    progress: &mut F,
) -> anyhow::Result<()>
where
    F: FnMut(&str, u64, String) -> anyhow::Result<()>,
{
    if let Some(parent) = target.parent() {
        std::fs::create_dir_all(parent).with_context(|| {
            format!(
                "failed to create model asset directory {}",
                parent.display()
            )
        })?;
    }
    let total = std::fs::metadata(source)
        .with_context(|| format!("failed to stat {}", source.display()))?
        .len();
    let tmp_path = target.with_extension("part");
    let mut input =
        File::open(source).with_context(|| format!("failed to open {}", source.display()))?;
    let mut output = File::create(&tmp_path).with_context(|| {
        format!(
            "failed to create temporary model asset {}",
            tmp_path.display()
        )
    })?;
    let mut copied = 0u64;
    let mut buffer = [0u8; 1024 * 1024];
    loop {
        let read = input
            .read(&mut buffer)
            .with_context(|| format!("failed to read {}", source.display()))?;
        if read == 0 {
            break;
        }
        output
            .write_all(&buffer[..read])
            .with_context(|| format!("failed to write {}", tmp_path.display()))?;
        copied = copied.saturating_add(read as u64);
        progress(
            "copying",
            copied.min(total),
            format!("copying cached {}", source.display()),
        )?;
    }
    output
        .flush()
        .with_context(|| format!("failed to flush {}", tmp_path.display()))?;
    std::fs::rename(&tmp_path, target).with_context(|| {
        format!(
            "failed to move {} to {}",
            tmp_path.display(),
            target.display()
        )
    })?;
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

fn profile_total_bytes(profile: &ModelProfileSpec) -> u64 {
    profile.assets.iter().map(|asset| asset.size_bytes).sum()
}

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

fn cleanup_staging_download_partials(
    profile: &ModelProfileSpec,
    root: &Path,
) -> anyhow::Result<()> {
    for asset in profile.assets {
        let local_path = root.join(asset.local_path);
        let single_part = local_path.with_extension("part");
        if single_part.exists() {
            std::fs::remove_file(&single_part)
                .with_context(|| format!("failed to remove {}", single_part.display()))?;
        }
        for index in 0..DIRECT_DOWNLOAD_MAX_THREADS {
            let chunk_part = chunk_part_path(&local_path, index);
            if chunk_part.exists() {
                std::fs::remove_file(&chunk_part)
                    .with_context(|| format!("failed to remove {}", chunk_part.display()))?;
            }
        }
    }
    Ok(())
}

fn promote_staging_root(staging: &Path, root: &Path) -> anyhow::Result<()> {
    if let Some(parent) = root.parent() {
        std::fs::create_dir_all(parent)
            .with_context(|| format!("failed to create model root parent {}", parent.display()))?;
    }
    if root.exists() {
        std::fs::remove_dir_all(root)
            .with_context(|| format!("failed to replace old model root {}", root.display()))?;
    }
    std::fs::rename(staging, root).with_context(|| {
        format!(
            "failed to promote staged model profile {} to {}",
            staging.display(),
            root.display()
        )
    })?;
    Ok(())
}

fn write_resolved_manifest(manifest: &ResolvedModelManifest) -> anyhow::Result<()> {
    let root = PathBuf::from(&manifest.model_root);
    std::fs::create_dir_all(&root)
        .with_context(|| format!("failed to create model root {}", root.display()))?;
    let path = root.join(RESOLVED_MANIFEST_FILE);
    let text = serde_json::to_string_pretty(manifest)
        .context("failed to serialize resolved model manifest")?;
    std::fs::write(&path, text).with_context(|| format!("failed to write {}", path.display()))?;
    Ok(())
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

    const TEST_DOWNLOAD_THREAD_STACK_BYTES: usize = 64 * 1024 * 1024;

    fn unique_temp_root(name: &str) -> PathBuf {
        std::env::temp_dir().join(format!(
            "{name}-{}",
            std::time::SystemTime::now()
                .duration_since(std::time::UNIX_EPOCH)
                .expect("system clock should be valid")
                .as_nanos()
        ))
    }

    fn run_download_on_large_stack<T, F>(f: F) -> T
    where
        T: Send + 'static,
        F: FnOnce() -> T + Send + 'static,
    {
        std::thread::Builder::new()
            .name("model-assets-real-download-test".to_string())
            .stack_size(TEST_DOWNLOAD_THREAD_STACK_BYTES)
            .spawn(f)
            .expect("real download test thread should spawn")
            .join()
            .expect("real download test thread should not panic")
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
    fn download_source_classifier_reports_mirror_and_official_sources() {
        assert_eq!(
            classify_download_source("https://hf-mirror.com/plhery/mobileclip2-onnx"),
            "hf-mirror.com"
        );
        assert_eq!(
            classify_download_source("https://huggingface.co/plhery/mobileclip2-onnx"),
            "huggingface.co"
        );
        assert_eq!(
            classify_download_source("https://models.example.invalid/cache"),
            "https://models.example.invalid/cache"
        );
    }

    #[test]
    fn redirect_join_handles_absolute_root_relative_and_file_relative_locations() {
        assert_eq!(
            join_redirect_url(
                "https://hf-mirror.com/repo/resolve/rev/model.onnx",
                "https://huggingface.co/repo/resolve/rev/model.onnx"
            )
            .expect("absolute redirect should parse"),
            "https://huggingface.co/repo/resolve/rev/model.onnx"
        );
        assert_eq!(
            join_redirect_url(
                "https://hf-mirror.com/repo/resolve/rev/model.onnx",
                "/repo/resolve/rev/model.onnx"
            )
            .expect("root-relative redirect should parse"),
            "https://hf-mirror.com/repo/resolve/rev/model.onnx"
        );
        assert_eq!(
            join_redirect_url(
                "https://hf-mirror.com/repo/resolve/rev/model.onnx",
                "model-v2.onnx"
            )
            .expect("file-relative redirect should parse"),
            "https://hf-mirror.com/repo/resolve/rev/model-v2.onnx"
        );
    }

    #[test]
    fn large_range_downloads_are_split_into_stable_chunks() {
        let local_path = PathBuf::from("model.onnx");
        let chunks = plan_download_chunks(&local_path, 400 * 1024 * 1024, 8);
        assert_eq!(chunks.len(), 8);
        assert_eq!(chunks.first().expect("first chunk").start, 0);
        assert_eq!(
            chunks.last().expect("last chunk").end,
            400 * 1024 * 1024 - 1
        );
        assert_eq!(chunks.iter().map(chunk_len).sum::<u64>(), 400 * 1024 * 1024);
        assert!(
            chunks
                .iter()
                .all(|chunk| chunk.part_path.to_string_lossy().contains(".part."))
        );
    }

    #[test]
    fn profile_listing_reports_missing_models_without_downloading() {
        let root = unique_temp_root("alcedo-mind-profile-list");
        let profiles = list_profiles(Some(&root));
        assert_eq!(profiles.len(), MODEL_PROFILES.len());
        assert!(profiles.iter().all(|profile| !profile.installed));
        assert!(!root.exists());
    }

    // Disabled by default because it downloads the real MobileCLIP2 profile from
    // the Hugging Face mirror. Run manually after downloader changes to verify
    // mirror URL handling, byte progress, final promotion, and manifest writing.
    #[test]
    #[ignore = "downloads real model assets from hf-mirror.com"]
    fn ignored_downloads_mobileclip_from_mirror_with_progress() {
        let root = unique_temp_root("alcedo-mind-real-mobileclip-download");
        let root_for_thread = root.clone();
        let (manifest, saw_byte_progress, last_progress) = run_download_on_large_stack(move || {
            let mut saw_byte_progress = false;
            let mut last_progress = ModelDownloadProgress::default();
            let manifest = download_model_profile_with_progress(
                MOBILECLIP2_ONNX_PROFILE,
                &root_for_thread,
                "https://hf-mirror.com",
                None,
                |progress| {
                    if progress.phase == "downloading"
                        && progress.bytes_total > 0
                        && progress.bytes_downloaded > 0
                    {
                        saw_byte_progress = true;
                    }
                    last_progress = progress;
                    Ok(())
                },
            )
            .expect("real mirror download should complete");
            (manifest, saw_byte_progress, last_progress)
        });

        assert!(saw_byte_progress);
        assert_eq!(last_progress.phase, "installed");
        assert_eq!(manifest.embedding_dimension, 512);
        assert_eq!(manifest.model_root, root.to_string_lossy());
        let _ = std::fs::remove_dir_all(&root);
        let _ = std::fs::remove_dir_all(staging_root(&root));
    }

    // Disabled by default because it downloads the larger Jina CLIP v2 profile.
    // Run manually to verify the profile exposes Alcedo's 512-dimensional
    // Matryoshka output contract while preserving the model's native 1024 dims.
    #[test]
    #[ignore = "downloads real Jina CLIP v2 assets from hf-mirror.com"]
    fn ignored_downloads_jina_profile_with_512_matryoshka_contract() {
        let root = unique_temp_root("alcedo-mind-real-jina-download");
        let root_for_thread = root.clone();
        let manifest = run_download_on_large_stack(move || {
            download_model_profile_with_progress(
                "jina-clip-v2-int8-multilingual",
                &root_for_thread,
                "https://hf-mirror.com",
                None,
                |_| Ok(()),
            )
            .expect("real Jina mirror download should complete")
        });

        assert_eq!(manifest.embedding_dimension, 512);
        assert_eq!(manifest.native_embedding_dimension, 1024);
        assert_eq!(
            manifest.embedding_transform,
            "matryoshka_truncate_then_l2_normalize"
        );
        let _ = std::fs::remove_dir_all(&root);
        let _ = std::fs::remove_dir_all(staging_root(&root));
    }
}

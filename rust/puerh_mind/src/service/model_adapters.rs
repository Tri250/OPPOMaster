use anyhow::{Result, bail};

use crate::service::model_assets::ModelProfileSpec;

pub(crate) const TEXT_SEQUENCE_LENGTH: usize = 77;

#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub(crate) enum ImageResizeMode {
    ShortestEdgeCenterCrop,
}

#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub(crate) enum EngineProfileAdapter {
    MobileClipOpenClip,
    JinaClipV2OnnxInt8,
}

impl EngineProfileAdapter {
    pub(crate) fn from_profile(profile: &ModelProfileSpec) -> Result<Self> {
        match profile.engine_profile_id {
            "mobileclip2-openclip" => Ok(Self::MobileClipOpenClip),
            "jina-clip-v2-onnx-int8" => Ok(Self::JinaClipV2OnnxInt8),
            other => bail!(
                "semantic model profile {} requests unsupported engine adapter {other:?}",
                profile.profile_id
            ),
        }
    }

    pub(crate) fn supports_current_onnx_loader(self) -> bool {
        matches!(self, Self::MobileClipOpenClip | Self::JinaClipV2OnnxInt8)
    }

    pub(crate) fn text_sequence_length(self) -> usize {
        TEXT_SEQUENCE_LENGTH
    }

    pub(crate) fn requires_attention_mask(self) -> bool {
        false
    }

    pub(crate) fn image_mean_std(self) -> ([f32; 3], [f32; 3]) {
        match self {
            Self::MobileClipOpenClip => ([0.0, 0.0, 0.0], [1.0, 1.0, 1.0]),
            Self::JinaClipV2OnnxInt8 => (
                [0.48145466, 0.4578275, 0.40821073],
                [0.26862954, 0.26130258, 0.27577711],
            ),
        }
    }

    pub(crate) fn image_resize_mode(self) -> ImageResizeMode {
        match self {
            Self::MobileClipOpenClip | Self::JinaClipV2OnnxInt8 => {
                ImageResizeMode::ShortestEdgeCenterCrop
            }
        }
    }

    pub(crate) fn uses_multimodal_session(self) -> bool {
        matches!(self, Self::JinaClipV2OnnxInt8)
    }

    pub(crate) fn preferred_text_output_name(self) -> &'static str {
        match self {
            Self::JinaClipV2OnnxInt8 => "l2norm_text_embeddings",
            Self::MobileClipOpenClip => "",
        }
    }

    pub(crate) fn preferred_image_output_name(self) -> &'static str {
        match self {
            Self::JinaClipV2OnnxInt8 => "l2norm_image_embeddings",
            Self::MobileClipOpenClip => "",
        }
    }
}

pub(crate) fn l2_normalize(mut embedding: Vec<f32>) -> Result<Vec<f32>> {
    if embedding.iter().any(|value| !value.is_finite()) {
        bail!("embedding contains non-finite values");
    }

    let norm = embedding
        .iter()
        .map(|value| (*value as f64) * (*value as f64))
        .sum::<f64>()
        .sqrt();

    if norm == 0.0 {
        bail!("embedding norm is zero");
    }

    for value in &mut embedding {
        *value = (*value as f64 / norm) as f32;
    }
    Ok(embedding)
}

pub(crate) fn apply_embedding_transform(
    profile_id: &str,
    embedding_dim: usize,
    native_embedding_dim: usize,
    transform: &str,
    mut embedding: Vec<f32>,
) -> Result<Vec<f32>> {
    if embedding.len() != native_embedding_dim {
        bail!(
            "unexpected native embedding length {}, expected {} for profile {}",
            embedding.len(),
            native_embedding_dim,
            profile_id
        );
    }

    match transform {
        "l2_normalize" => {
            if embedding_dim != native_embedding_dim {
                bail!(
                    "profile {} uses l2_normalize but output dim {} differs from native dim {}",
                    profile_id,
                    embedding_dim,
                    native_embedding_dim
                );
            }
        }
        "matryoshka_truncate_then_l2_normalize" => {
            if embedding_dim > native_embedding_dim {
                bail!(
                    "profile {} cannot truncate native dim {} to larger output dim {}",
                    profile_id,
                    native_embedding_dim,
                    embedding_dim
                );
            }
            embedding.truncate(embedding_dim);
        }
        other => bail!(
            "profile {} requests unsupported embedding transform {other:?}",
            profile_id
        ),
    }

    if embedding.len() != embedding_dim {
        bail!(
            "transformed embedding length {}, expected {} for profile {}",
            embedding.len(),
            embedding_dim,
            profile_id
        );
    }
    l2_normalize(embedding)
}

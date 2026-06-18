use std::path::PathBuf;
use std::sync::Mutex;

use anyhow::{Result, bail};
use image::RgbImage;
use ort::{
    session::{OutputSelector, RunOptions, Session},
    value::{Tensor, TensorElementType},
};
use tokenizers::Tokenizer;
use tracing::{info, warn};

use crate::config::SemanticConfig;
use crate::service::embedding::{EmbeddingEngine, EngineModelInfo};
use crate::service::model_adapters::{EngineProfileAdapter, ImageResizeMode};
#[cfg(test)]
use crate::service::model_assets::ClipModelPaths;
use crate::service::model_assets::{
    AssetRole, ModelProfileSpec, find_profile, profile_asset_path, validate_model_profile,
};
#[cfg(test)]
use crate::service::ort_runtime::CoreMlMode;
use crate::service::ort_runtime::DeviceRequest;

#[cfg(test)]
const EMBEDDING_DIM: usize = 512;

#[derive(Debug, Clone)]
struct SessionIo {
    input_name: String,
    output_name: String,
}

#[derive(Debug, Clone)]
struct MultimodalSessionIo {
    text_input_name: String,
    image_input_name: String,
    text_output_name: String,
    image_output_name: String,
}

#[derive(Debug, Clone, Copy, PartialEq, Eq)]
enum ImageResizeMode {
    ShortestEdgeCenterCrop,
}

#[derive(Debug, Clone, Copy, PartialEq, Eq)]
enum EngineProfileAdapter {
    MobileClipOpenClip,
    JinaClipV2OnnxInt8,
    JinaClipV2OnnxFp16,
}

impl EngineProfileAdapter {
    fn from_profile(profile: &ModelProfileSpec) -> Result<Self> {
        match profile.engine_profile_id {
            "mobileclip2-openclip" => Ok(Self::MobileClipOpenClip),
            "jina-clip-v2-onnx-int8" => Ok(Self::JinaClipV2OnnxInt8),
            "jina-clip-v2-onnx-fp16" => Ok(Self::JinaClipV2OnnxFp16),
            other => bail!(
                "semantic model profile {} requests unsupported engine adapter {other:?}",
                profile.profile_id
            ),
        }
    }

    fn supports_current_onnx_loader(self) -> bool {
        matches!(
            self,
            Self::MobileClipOpenClip | Self::JinaClipV2OnnxInt8 | Self::JinaClipV2OnnxFp16
        )
    }

    fn text_sequence_length(self) -> usize {
        TEXT_SEQUENCE_LENGTH
    }

    fn requires_attention_mask(self) -> bool {
        false
    }

    fn image_mean_std(self) -> ([f32; 3], [f32; 3]) {
        match self {
            Self::MobileClipOpenClip => ([0.0, 0.0, 0.0], [1.0, 1.0, 1.0]),
            Self::JinaClipV2OnnxInt8 | Self::JinaClipV2OnnxFp16 => (
                [0.48145466, 0.4578275, 0.40821073],
                [0.26862954, 0.26130258, 0.27577711],
            ),
        }
    }

    fn image_resize_mode(self) -> ImageResizeMode {
        match self {
            Self::MobileClipOpenClip
            | Self::JinaClipV2OnnxInt8
            | Self::JinaClipV2OnnxFp16 => ImageResizeMode::ShortestEdgeCenterCrop,
        }
    }

    fn uses_multimodal_session(self) -> bool {
        matches!(
            self,
            Self::JinaClipV2OnnxInt8 | Self::JinaClipV2OnnxFp16
        )
    }

    fn preferred_text_output_name(self) -> &'static str {
        match self {
            Self::JinaClipV2OnnxInt8 | Self::JinaClipV2OnnxFp16 => "l2norm_text_embeddings",
            Self::MobileClipOpenClip => "",
        }
    }

    fn preferred_image_output_name(self) -> &'static str {
        match self {
            Self::JinaClipV2OnnxInt8 | Self::JinaClipV2OnnxFp16 => "l2norm_image_embeddings",
            Self::MobileClipOpenClip => "",
        }
    }

    /// Whether this profile must run CoreML on CPU-only compute units on macOS.
    ///
    /// The Jina CLIP v2 INT8 (quantized) ONNX export produces non-finite
    /// (NaN) embeddings — for both the text and image branches — when CoreML
    /// schedules its subgraphs onto the GPU/ANE (`All` / `CPUAndGPU`); only
    /// the CPU path is numerically correct. The FP16 export is non-quantized
    /// and runs correctly on the full CoreML compute-unit range, so macOS
    /// uses it instead. MobileCLIP2 is likewise unaffected.
    fn requires_coreml_cpu_only(self) -> bool {
        matches!(self, Self::JinaClipV2OnnxInt8)
    }
}

pub struct OrtClipEngine {
    profile_id: String,
    model_id: String,
    revision: String,
    engine_profile_id: String,
    language: String,
    embedding_dim: usize,
    native_embedding_dim: usize,
    image_size: usize,
    embedding_transform: String,
    provider: String,
    model_root: PathBuf,
    tokenizer: Tokenizer,
    adapter: EngineProfileAdapter,
    text_session: Option<Mutex<Session>>,
    vision_session: Option<Mutex<Session>>,
    multimodal_session: Option<Mutex<Session>>,
    text_io: Option<SessionIo>,
    vision_io: Option<SessionIo>,
    multimodal_io: Option<MultimodalSessionIo>,
}

/// Apply a profile's embedding transform (truncate for matryoshka profiles, then
/// L2-normalize) to a single native-dimension embedding row. Extracted as a free
/// function so the math is unit-testable without loading an ONNX model.
fn transform_embedding(
    mut embedding: Vec<f32>,
    native_embedding_dim: usize,
    embedding_dim: usize,
    embedding_transform: &str,
    profile_id: &str,
) -> Result<Vec<f32>> {
    if embedding.len() != native_embedding_dim {
        bail!(
            "unexpected native embedding length {}, expected {} for profile {}",
            embedding.len(),
            native_embedding_dim,
            profile_id
        );
    }

    match embedding_transform {
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
    OrtClipEngine::l2_normalize(embedding)
}

impl OrtClipEngine {
    fn parse_device_request(value: &str) -> Result<DeviceRequest> {
        crate::service::ort_runtime::parse_device_request(value)
    }

    fn describe_device_request(device_request: DeviceRequest) -> String {
        crate::service::ort_runtime::describe_device_request(device_request)
    }

    fn validate_text_session(
        session: &Session,
        seq_len: usize,
        native_embedding_dim: usize,
        requires_attention_mask: bool,
    ) -> Result<SessionIo> {
        let expected_input_count = if requires_attention_mask { 2 } else { 1 };
        if session.inputs().len() != expected_input_count {
            bail!(
                "unexpected text model input count {}, expected {}",
                session.inputs().len(),
                expected_input_count
            );
        }
        if session.outputs().len() != 1 {
            bail!(
                "unexpected text model output count {}, expected 1",
                session.outputs().len()
            );
        }

        let input = session
            .inputs()
            .iter()
            .find(|input| input.name() == "input_ids")
            .unwrap_or(&session.inputs()[0]);
        let input_shape = input
            .dtype()
            .tensor_shape()
            .ok_or_else(|| anyhow::anyhow!("text model input is not a tensor"))?;
        let input_type = input
            .dtype()
            .tensor_type()
            .ok_or_else(|| anyhow::anyhow!("text model input is not a tensor"))?;

        if input_type != TensorElementType::Int64 {
            bail!(
                "unexpected text input tensor type {:?}, expected Int64",
                input_type
            );
        }
        if input_shape.len() != 2 || input_shape[1] != seq_len as i64 {
            bail!(
                "unexpected text input shape {:?}, expected [batch, {}]",
                input_shape,
                seq_len
            );
        }

        if requires_attention_mask {
            let mask = session
                .inputs()
                .iter()
                .find(|input| input.name() == "attention_mask")
                .ok_or_else(|| anyhow::anyhow!("text model is missing attention_mask input"))?;
            let mask_shape = mask
                .dtype()
                .tensor_shape()
                .ok_or_else(|| anyhow::anyhow!("attention_mask input is not a tensor"))?;
            let mask_type = mask
                .dtype()
                .tensor_type()
                .ok_or_else(|| anyhow::anyhow!("attention_mask input is not a tensor"))?;
            if mask_type != TensorElementType::Int64 {
                bail!(
                    "unexpected attention_mask tensor type {:?}, expected Int64",
                    mask_type
                );
            }
            if mask_shape.len() != 2 || mask_shape[1] != seq_len as i64 {
                bail!(
                    "unexpected attention_mask shape {:?}, expected [batch, {}]",
                    mask_shape,
                    seq_len
                );
            }
        }

        let output = &session.outputs()[0];
        let output_shape = output
            .dtype()
            .tensor_shape()
            .ok_or_else(|| anyhow::anyhow!("text model output is not a tensor"))?;
        let output_type = output
            .dtype()
            .tensor_type()
            .ok_or_else(|| anyhow::anyhow!("text model output is not a tensor"))?;

        if output_type != TensorElementType::Float32 {
            bail!(
                "unexpected text output tensor type {:?}, expected Float32",
                output_type
            );
        }
        if output_shape.len() != 2 || output_shape[1] != native_embedding_dim as i64 {
            bail!(
                "unexpected text output shape {:?}, expected [batch, {}]",
                output_shape,
                native_embedding_dim
            );
        }

        Ok(SessionIo {
            input_name: input.name().to_string(),
            output_name: output.name().to_string(),
        })
    }

    fn validate_vision_session(
        session: &Session,
        image_size: usize,
        native_embedding_dim: usize,
    ) -> Result<SessionIo> {
        if session.inputs().len() != 1 {
            bail!(
                "unexpected vision model input count {}, expected 1",
                session.inputs().len()
            );
        }
        if session.outputs().len() != 1 {
            bail!(
                "unexpected vision model output count {}, expected 1",
                session.outputs().len()
            );
        }

        let input = &session.inputs()[0];
        let input_shape = input
            .dtype()
            .tensor_shape()
            .ok_or_else(|| anyhow::anyhow!("vision model input is not a tensor"))?;
        let input_type = input
            .dtype()
            .tensor_type()
            .ok_or_else(|| anyhow::anyhow!("vision model input is not a tensor"))?;

        if input_type != TensorElementType::Float32 {
            bail!(
                "unexpected vision input tensor type {:?}, expected Float32",
                input_type
            );
        }
        if input_shape.len() != 4
            || input_shape[1] != 3
            || input_shape[2] != image_size as i64
            || input_shape[3] != image_size as i64
        {
            bail!(
                "unexpected vision input shape {:?}, expected [batch, 3, {}, {}]",
                input_shape,
                image_size,
                image_size
            );
        }

        let output = &session.outputs()[0];
        let output_shape = output
            .dtype()
            .tensor_shape()
            .ok_or_else(|| anyhow::anyhow!("vision model output is not a tensor"))?;
        let output_type = output
            .dtype()
            .tensor_type()
            .ok_or_else(|| anyhow::anyhow!("vision model output is not a tensor"))?;

        if output_type != TensorElementType::Float32 {
            bail!(
                "unexpected vision output tensor type {:?}, expected Float32",
                output_type
            );
        }
        if output_shape.len() != 2 || output_shape[1] != native_embedding_dim as i64 {
            bail!(
                "unexpected vision output shape {:?}, expected [batch, {}]",
                output_shape,
                native_embedding_dim
            );
        }

        Ok(SessionIo {
            input_name: input.name().to_string(),
            output_name: output.name().to_string(),
        })
    }

    fn validate_multimodal_session(
        session: &Session,
        image_size: usize,
        native_embedding_dim: usize,
        adapter: EngineProfileAdapter,
    ) -> Result<MultimodalSessionIo> {
        let text_input = session
            .inputs()
            .iter()
            .find(|input| input.name() == "input_ids")
            .ok_or_else(|| anyhow::anyhow!("multimodal model is missing input_ids input"))?;
        let image_input = session
            .inputs()
            .iter()
            .find(|input| input.name() == "pixel_values")
            .ok_or_else(|| anyhow::anyhow!("multimodal model is missing pixel_values input"))?;

        let text_input_type = text_input
            .dtype()
            .tensor_type()
            .ok_or_else(|| anyhow::anyhow!("input_ids input is not a tensor"))?;
        if text_input_type != TensorElementType::Int64 {
            bail!(
                "unexpected input_ids tensor type {:?}, expected Int64",
                text_input_type
            );
        }
        let text_shape = text_input
            .dtype()
            .tensor_shape()
            .ok_or_else(|| anyhow::anyhow!("input_ids input is not a tensor"))?;
        if text_shape.len() != 2 {
            bail!(
                "unexpected input_ids shape {:?}, expected [batch, sequence]",
                text_shape
            );
        }

        let image_input_type = image_input
            .dtype()
            .tensor_type()
            .ok_or_else(|| anyhow::anyhow!("pixel_values input is not a tensor"))?;
        if image_input_type != TensorElementType::Float32 {
            bail!(
                "unexpected pixel_values tensor type {:?}, expected Float32",
                image_input_type
            );
        }
        let image_shape = image_input
            .dtype()
            .tensor_shape()
            .ok_or_else(|| anyhow::anyhow!("pixel_values input is not a tensor"))?;
        if image_shape.len() != 4
            || image_shape[1] != 3
            || image_shape[2] != image_size as i64
            || image_shape[3] != image_size as i64
        {
            bail!(
                "unexpected pixel_values shape {:?}, expected [batch, 3, {}, {}]",
                image_shape,
                image_size,
                image_size
            );
        }

        let text_output_name = adapter.preferred_text_output_name();
        let image_output_name = adapter.preferred_image_output_name();
        let text_output = session
            .outputs()
            .iter()
            .find(|output| output.name() == text_output_name)
            .ok_or_else(|| {
                anyhow::anyhow!("multimodal model is missing {text_output_name:?} output")
            })?;
        let image_output = session
            .outputs()
            .iter()
            .find(|output| output.name() == image_output_name)
            .ok_or_else(|| {
                anyhow::anyhow!("multimodal model is missing {image_output_name:?} output")
            })?;

        for output in [text_output, image_output] {
            let output_type = output
                .dtype()
                .tensor_type()
                .ok_or_else(|| anyhow::anyhow!("multimodal output is not a tensor"))?;
            if output_type != TensorElementType::Float32 {
                bail!(
                    "unexpected multimodal output tensor type {:?}, expected Float32",
                    output_type
                );
            }
            let output_shape = output
                .dtype()
                .tensor_shape()
                .ok_or_else(|| anyhow::anyhow!("multimodal output is not a tensor"))?;
            if output_shape.len() != 2 || output_shape[1] != native_embedding_dim as i64 {
                bail!(
                    "unexpected multimodal output shape {:?}, expected [batch, {}]",
                    output_shape,
                    native_embedding_dim
                );
            }
        }

        Ok(MultimodalSessionIo {
            text_input_name: text_input.name().to_string(),
            image_input_name: image_input.name().to_string(),
            text_output_name: text_output.name().to_string(),
            image_output_name: image_output.name().to_string(),
        })
    }

    fn tokenizer_for_profile(profile: &ModelProfileSpec, model_root: &str) -> Result<Tokenizer> {
        let _ = EngineProfileAdapter::from_profile(profile)?;

        let tokenizer_path = profile_asset_path(profile, model_root, AssetRole::Tokenizer)
            .or_else(|_| profile_asset_path(profile, model_root, AssetRole::Vocab))?;
        Tokenizer::from_file(&tokenizer_path)
            .map_err(|e| anyhow::anyhow!("failed to load tokenizer: {e}"))
    }

    fn prepare_text_batch_with_tokenizer(
        tokenizer: &Tokenizer,
        texts: &[&str],
        seq_len: usize,
    ) -> Result<(Vec<i64>, Option<Vec<i64>>, usize)> {
        if texts.is_empty() {
            bail!("text batch must not be empty");
        }

        let mut flattened = Vec::with_capacity(texts.len() * seq_len);
        let mut attention_masks = Vec::with_capacity(texts.len() * seq_len);
        for (index, text) in texts.iter().enumerate() {
            if text.trim().is_empty() {
                bail!("text at batch index {index} must not be empty");
            }

            let encoding = tokenizer
                .encode(*text, true)
                .map_err(|e| anyhow::anyhow!("failed to tokenize text: {e}"))?;

            let mut row = encoding
                .get_ids()
                .iter()
                .copied()
                .map(i64::from)
                .collect::<Vec<_>>();
            row.truncate(seq_len);
            row.resize(seq_len, 0);
            flattened.extend(row);

            let mut mask = encoding
                .get_attention_mask()
                .iter()
                .copied()
                .map(i64::from)
                .collect::<Vec<_>>();
            mask.truncate(seq_len);
            mask.resize(seq_len, 0);
            attention_masks.extend(mask);
        }

        Ok((flattened, Some(attention_masks), texts.len()))
    }

    fn prepare_text_batch(&self, texts: &[&str]) -> Result<(Vec<i64>, Option<Vec<i64>>, usize)> {
        Self::prepare_text_batch_with_tokenizer(
            &self.tokenizer,
            texts,
            self.adapter.text_sequence_length(),
        )
    }

    fn prepare_image_tensor_data_for_size(
        rgb: &RgbImage,
        image_size: usize,
        resize_mode: ImageResizeMode,
        mean: [f32; 3],
        std: [f32; 3],
    ) -> Result<Vec<f32>> {
        let target = image_size as u32;
        let (src_w, src_h) = rgb.dimensions();
        if src_w == 0 || src_h == 0 {
            bail!("image must not be empty");
        }

        let prepared = match resize_mode {
            ImageResizeMode::ShortestEdgeCenterCrop => {
                let scale = if src_w < src_h {
                    target as f32 / src_w as f32
                } else {
                    target as f32 / src_h as f32
                };

                let resized_w = ((src_w as f32) * scale).round().max(target as f32) as u32;
                let resized_h = ((src_h as f32) * scale).round().max(target as f32) as u32;
                let resized = image::imageops::resize(
                    rgb,
                    resized_w,
                    resized_h,
                    image::imageops::FilterType::Triangle,
                );

                let crop_x = (resized_w - target) / 2;
                let crop_y = (resized_h - target) / 2;
                image::imageops::crop_imm(&resized, crop_x, crop_y, target, target).to_image()
            }
        };

        let mut data = Vec::with_capacity((3 * target * target) as usize);
        for channel in 0..3usize {
            for y in 0..target {
                for x in 0..target {
                    let pixel = prepared.get_pixel(x, y);
                    data.push(((pixel[channel] as f32 / 255.0) - mean[channel]) / std[channel]);
                }
            }
        }

        Ok(data)
    }

    fn prepare_image_batch_tensor_data(&self, rgbs: &[RgbImage]) -> Result<(Vec<f32>, usize)> {
        if rgbs.is_empty() {
            bail!("image batch must not be empty");
        }

        let mut batch_data = Vec::with_capacity(rgbs.len() * 3 * self.image_size * self.image_size);
        let (mean, std) = self.adapter.image_mean_std();
        for rgb in rgbs {
            batch_data.extend(Self::prepare_image_tensor_data_for_size(
                rgb,
                self.image_size,
                self.adapter.image_resize_mode(),
                mean,
                std,
            )?);
        }

        Ok((batch_data, rgbs.len()))
    }

    fn extract_embeddings_from_outputs(
        &self,
        outputs: &ort::session::SessionOutputs<'_>,
        output_name: &str,
        batch_size: usize,
        modality: &str,
    ) -> Result<Vec<Vec<f32>>> {
        let output = outputs
            .get(output_name)
            .ok_or_else(|| anyhow::anyhow!("missing {modality} output tensor {output_name:?}"))?;
        let (_shape, values) = output
            .try_extract_tensor::<f32>()
            .map_err(|e| anyhow::anyhow!("failed to extract {modality} output tensor: {e}"))?;

        if values.len() != batch_size * self.native_embedding_dim {
            bail!(
                "unexpected {modality} embedding output length {}, expected {}",
                values.len(),
                batch_size * self.native_embedding_dim
            );
        }

        values
            .chunks(self.native_embedding_dim)
            .map(|row| self.apply_embedding_transform(row.to_vec()))
            .collect()
    }

    fn apply_embedding_transform(&self, embedding: Vec<f32>) -> Result<Vec<f32>> {
        crate::service::model_adapters::apply_embedding_transform(
            &self.profile_id,
            self.embedding_dim,
            self.native_embedding_dim,
            &self.embedding_transform,
            embedding,
        )
    }

    pub fn forward_text_embeddings(&self, texts: &[&str]) -> Result<Vec<Vec<f32>>> {
        let (input_ids, attention_mask, batch_size) = self.prepare_text_batch(texts)?;
        let seq_len = self.adapter.text_sequence_length();
        let input_tensor = Tensor::from_array(([batch_size, seq_len], input_ids))
            .map_err(|e| anyhow::anyhow!("failed to build text input tensor: {e}"))?;

        if self.adapter.uses_multimodal_session() {
            let io = self
                .multimodal_io
                .as_ref()
                .ok_or_else(|| anyhow::anyhow!("multimodal session IO is not initialized"))?;
            let dummy_pixels = vec![0.0f32; batch_size * 3 * self.image_size * self.image_size];
            let pixel_tensor = Tensor::from_array((
                [batch_size, 3, self.image_size, self.image_size],
                dummy_pixels,
            ))
            .map_err(|e| anyhow::anyhow!("failed to build dummy image input tensor: {e}"))?;
            let mut session = self
                .multimodal_session
                .as_ref()
                .ok_or_else(|| anyhow::anyhow!("multimodal session is not initialized"))?
                .lock()
                .map_err(|err| anyhow::anyhow!("multimodal session lock poisoned: {err}"))?;
            let run_options = RunOptions::new()
                .map_err(|e| anyhow::anyhow!("failed to build multimodal run options: {e}"))?
                .with_outputs(OutputSelector::no_default().with(io.text_output_name.clone()));
            let outputs = session
                .run_with_options(
                    ort::inputs! {
                        io.text_input_name.as_str() => input_tensor,
                        io.image_input_name.as_str() => pixel_tensor,
                    },
                    &run_options,
                )
                .map_err(|e| anyhow::anyhow!("failed to run multimodal text ONNX model: {e}"))?;
            return self.extract_embeddings_from_outputs(
                &outputs,
                io.text_output_name.as_str(),
                batch_size,
                "text",
            );
        }

        let attention_mask_tensor = if self.adapter.requires_attention_mask() {
            Some(
                Tensor::from_array((
                    [batch_size, seq_len],
                    attention_mask.ok_or_else(|| anyhow::anyhow!("missing attention mask"))?,
                ))
                .map_err(|e| anyhow::anyhow!("failed to build attention mask tensor: {e}"))?,
            )
        } else {
            None
        };

        let text_io = self
            .text_io
            .as_ref()
            .ok_or_else(|| anyhow::anyhow!("text session IO is not initialized"))?;
        let mut session = self
            .text_session
            .as_ref()
            .ok_or_else(|| anyhow::anyhow!("text session is not initialized"))?
            .lock()
            .map_err(|err| anyhow::anyhow!("text session lock poisoned: {err}"))?;
        let outputs = if let Some(mask_tensor) = attention_mask_tensor {
            session
                .run(ort::inputs! {text_io.input_name.as_str() => input_tensor,
                "attention_mask"                 => mask_tensor})
                .map_err(|e| anyhow::anyhow!("failed to run text ONNX model: {e}"))?
        } else {
            session
                .run(ort::inputs! {text_io.input_name.as_str() => input_tensor})
                .map_err(|e| anyhow::anyhow!("failed to run text ONNX model: {e}"))?
        };

        self.extract_embeddings_from_outputs(
            &outputs,
            text_io.output_name.as_str(),
            batch_size,
            "text",
        )
    }

    pub fn forward_image_embeddings(&self, rgbs: &[RgbImage]) -> Result<Vec<Vec<f32>>> {
        let (pixel_values, batch_size) = self.prepare_image_batch_tensor_data(rgbs)?;
        let input_tensor = Tensor::from_array((
            [batch_size, 3, self.image_size, self.image_size],
            pixel_values,
        ))
        .map_err(|e| anyhow::anyhow!("failed to build image input tensor: {e}"))?;

        if self.adapter.uses_multimodal_session() {
            let io = self
                .multimodal_io
                .as_ref()
                .ok_or_else(|| anyhow::anyhow!("multimodal session IO is not initialized"))?;
            let seq_len = self.adapter.text_sequence_length();
            let input_ids = vec![0i64; batch_size * seq_len];
            let text_tensor = Tensor::from_array(([batch_size, seq_len], input_ids))
                .map_err(|e| anyhow::anyhow!("failed to build dummy text input tensor: {e}"))?;
            let mut session = self
                .multimodal_session
                .as_ref()
                .ok_or_else(|| anyhow::anyhow!("multimodal session is not initialized"))?
                .lock()
                .map_err(|err| anyhow::anyhow!("multimodal session lock poisoned: {err}"))?;
            let run_options = RunOptions::new()
                .map_err(|e| anyhow::anyhow!("failed to build multimodal run options: {e}"))?
                .with_outputs(OutputSelector::no_default().with(io.image_output_name.clone()));
            let outputs = session
                .run_with_options(
                    ort::inputs! {
                        io.text_input_name.as_str() => text_tensor,
                        io.image_input_name.as_str() => input_tensor,
                    },
                    &run_options,
                )
                .map_err(|e| anyhow::anyhow!("failed to run multimodal image ONNX model: {e}"))?;
            return self.extract_embeddings_from_outputs(
                &outputs,
                io.image_output_name.as_str(),
                batch_size,
                "image",
            );
        }

        let vision_io = self
            .vision_io
            .as_ref()
            .ok_or_else(|| anyhow::anyhow!("vision session IO is not initialized"))?;
        let mut session = self
            .vision_session
            .as_ref()
            .ok_or_else(|| anyhow::anyhow!("vision session is not initialized"))?
            .lock()
            .map_err(|err| anyhow::anyhow!("vision session lock poisoned: {err}"))?;
        let outputs = session
            .run(ort::inputs! {vision_io.input_name.as_str() => input_tensor})
            .map_err(|e| anyhow::anyhow!("failed to run image ONNX model: {e}"))?;

        self.extract_embeddings_from_outputs(
            &outputs,
            vision_io.output_name.as_str(),
            batch_size,
            "image",
        )
    }

    pub fn new(config: &SemanticConfig) -> Result<Self> {
        let device_request = Self::parse_device_request(&config.device)?;

        // Model asset downloading is handled by the C++ application layer (via
        // aria2). The Rust runtime only validates that the requested profile is
        // present on disk before loading it for inference.
        let manifest = validate_model_profile(&config.model_id, &config.model_root)?;
        if manifest.revision != config.revision {
            bail!(
                "configured semantic model revision {} does not match resolved profile revision {} for {}",
                config.revision,
                manifest.revision,
                manifest.profile_id
            );
        }
        let profile = find_profile(&manifest.profile_id)?;
        let adapter = EngineProfileAdapter::from_profile(profile)?;
        if !adapter.supports_current_onnx_loader() {
            bail!(
                "semantic model profile {} uses adapter {} which is not yet wired for ONNX inference",
                profile.profile_id,
                profile.engine_profile_id
            );
        }

        crate::service::ort_runtime::initialize_ort_environment()?;

        let effective_device_request =
            Self::resolve_effective_device_request(adapter, device_request);
        if effective_device_request != device_request {
            warn!(
                "CoreML GPU/ANE compute units produce non-finite embeddings for the quantized {} profile; downgrading {} to {} for correctness",
                profile.profile_id,
                Self::describe_device_request(device_request),
                Self::describe_device_request(effective_device_request),
            );
        }

        let device_description = Self::describe_device_request(effective_device_request);
        info!(
            "loading ORT clip profile {} ({}) from {} on {}",
            profile.profile_id, profile.engine_profile_id, config.model_root, device_description,
        );

        let tokenizer = Self::tokenizer_for_profile(profile, &config.model_root)?;

        let (text_session, vision_session, multimodal_session, text_io, vision_io, multimodal_io) =
            if adapter.uses_multimodal_session() {
                let multimodal_model =
                    profile_asset_path(profile, &config.model_root, AssetRole::MultimodalModel)?;
                let session =
                    crate::service::ort_runtime::load_session(&multimodal_model, device_request)?;
                let io = Self::validate_multimodal_session(
                    &session,
                    manifest.image_size as usize,
                    manifest.native_embedding_dimension as usize,
                    adapter,
                )?;
                (None, None, Some(Mutex::new(session)), None, None, Some(io))
            } else {
                let text_model =
                    profile_asset_path(profile, &config.model_root, AssetRole::TextModel)?;
                let vision_model =
                    profile_asset_path(profile, &config.model_root, AssetRole::VisionModel)?;

                let text_session =
                    crate::service::ort_runtime::load_session(&text_model, device_request)?;
                let text_io = Self::validate_text_session(
                    &text_session,
                    adapter.text_sequence_length(),
                    manifest.native_embedding_dimension as usize,
                    adapter.requires_attention_mask(),
                )?;

                let vision_session =
                    crate::service::ort_runtime::load_session(&vision_model, device_request)?;
                let vision_io = Self::validate_vision_session(
                    &vision_session,
                    manifest.image_size as usize,
                    manifest.native_embedding_dimension as usize,
                )?;
                (
                    Some(Mutex::new(text_session)),
                    Some(Mutex::new(vision_session)),
                    None,
                    Some(text_io),
                    Some(vision_io),
                    None,
                )
            };

        Ok(Self {
            profile_id: manifest.profile_id,
            model_id: manifest.model_id,
            revision: manifest.revision,
            engine_profile_id: manifest.engine_profile_id,
            language: manifest.language,
            embedding_dim: manifest.embedding_dimension as usize,
            native_embedding_dim: manifest.native_embedding_dimension as usize,
            image_size: manifest.image_size as usize,
            embedding_transform: manifest.embedding_transform,
            provider: device_description,
            model_root: PathBuf::from(manifest.model_root),
            tokenizer,
            adapter,
            text_session,
            vision_session,
            multimodal_session,
            text_io,
            vision_io,
            multimodal_io,
        })
    }
}

impl EmbeddingEngine for OrtClipEngine {
    fn embed_text(&self, text: &str) -> Result<Vec<f32>> {
        let mut embeddings = self.forward_text_embeddings(&[text])?;
        if embeddings.len() != 1 {
            bail!("expected one text embedding row, got {}", embeddings.len());
        }
        Ok(embeddings.remove(0))
    }

    fn embed_texts(&self, texts: &[&str]) -> Result<Vec<Vec<f32>>> {
        self.forward_text_embeddings(texts)
    }

    fn embed_image(&self, rgb: &RgbImage) -> Result<Vec<f32>> {
        let mut embeddings = self.forward_image_embeddings(std::slice::from_ref(rgb))?;
        if embeddings.len() != 1 {
            bail!("expected one image embedding row, got {}", embeddings.len());
        }
        Ok(embeddings.remove(0))
    }

    fn embed_images(&self, rgbs: &[RgbImage]) -> Result<Vec<Vec<f32>>> {
        self.forward_image_embeddings(rgbs)
    }

    fn default_text_model_name(&self) -> &str {
        &self.model_id
    }

    fn default_image_model_name(&self) -> &str {
        &self.model_id
    }

    fn model_info(&self) -> EngineModelInfo {
        EngineModelInfo {
            profile_id: self.profile_id.clone(),
            model_id: self.model_id.clone(),
            revision: self.revision.clone(),
            engine_profile_id: self.engine_profile_id.clone(),
            language: self.language.clone(),
            embedding_dim: self.embedding_dim as u32,
            native_embedding_dim: self.native_embedding_dim as u32,
            image_size: self.image_size as u32,
            embedding_transform: self.embedding_transform.clone(),
            provider: self.provider.clone(),
            model_root: self.model_root.to_string_lossy().into_owned(),
            prototype_config_hash: String::new(),
        }
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::config::SemanticConfig;
    use std::sync::OnceLock;

    static MODEL_ENGINE_LOAD_LOCK: OnceLock<Mutex<()>> = OnceLock::new();

    fn test_model_root() -> String {
        std::env::var("ALCEDO_MIND_TEST_MODEL_ROOT").unwrap_or_else(|_| {
            std::path::Path::new(env!("CARGO_MANIFEST_DIR"))
                .join("models")
                .join("mobileclip2-s2-openclip")
                .to_string_lossy()
                .into_owned()
        })
    }

    fn test_allow_download() -> bool {
        std::env::var("ALCEDO_MIND_TEST_ALLOW_DOWNLOAD")
            .ok()
            .is_some_and(|value| {
                matches!(
                    value.to_ascii_lowercase().as_str(),
                    "1" | "true" | "yes" | "on"
                )
            })
    }

    fn has_test_model_assets() -> bool {
        ClipModelPaths::from_root(test_model_root())
            .validate()
            .is_ok()
    }

    fn ensure_test_model_assets() -> ClipModelPaths {
        if !test_allow_download() && !has_test_model_assets() {
            eprintln!(
                "skipping ORT model test; set ALCEDO_MIND_TEST_MODEL_ROOT or ALCEDO_MIND_TEST_ALLOW_DOWNLOAD=1"
            );
            return ClipModelPaths::from_root(test_model_root());
        }

        let _guard = MODEL_ENGINE_LOAD_LOCK
            .get_or_init(|| Mutex::new(()))
            .lock()
            .unwrap_or_else(|poisoned| poisoned.into_inner());

        let paths = ClipModelPaths::from_root(test_model_root());
        paths
            .ensure_present(
                crate::service::model_assets::MOBILECLIP2_ONNX_REVISION,
                "https://hf-mirror.com",
                test_allow_download(),
            )
            .expect("test model assets should be present");
        paths
    }

    fn make_test_engine() -> OrtClipEngine {
        make_test_engine_with_device("cpu")
    }

    fn make_test_engine_with_device(device: &str) -> OrtClipEngine {
        if !test_allow_download() && !has_test_model_assets() {
            panic!(
                "skipping ORT model test; set ALCEDO_MIND_TEST_MODEL_ROOT or ALCEDO_MIND_TEST_ALLOW_DOWNLOAD=1"
            );
        }

        let _guard = MODEL_ENGINE_LOAD_LOCK
            .get_or_init(|| Mutex::new(()))
            .lock()
            .unwrap_or_else(|poisoned| poisoned.into_inner());

        let config = SemanticConfig {
            model_id: "plhery/mobileclip2-onnx:s2".to_string(),
            revision: crate::service::model_assets::MOBILECLIP2_ONNX_REVISION.to_string(),
            model_root: test_model_root(),
            hf_endpoint: "https://hf-mirror.com".to_string(),
            device: device.to_string(),
            allow_download: test_allow_download(),
            batch_cap: 512,
            batch_wait_ms: 25,
        };
        OrtClipEngine::new(&config).expect("engine should load")
    }

    fn make_profile_test_engine_with_root(
        profile_id: &str,
        revision: &str,
        model_root: String,
        device: &str,
    ) -> OrtClipEngine {
        let _guard = MODEL_ENGINE_LOAD_LOCK
            .get_or_init(|| Mutex::new(()))
            .lock()
            .unwrap_or_else(|poisoned| poisoned.into_inner());

        let config = SemanticConfig {
            model_id: profile_id.to_string(),
            revision: revision.to_string(),
            model_root,
            hf_endpoint: "https://hf-mirror.com".to_string(),
            device: device.to_string(),
            allow_download: false,
            batch_cap: 512,
            batch_wait_ms: 25,
        };
        OrtClipEngine::new(&config).expect("profile engine should load")
    }

    #[test]
    fn l2_normalize_produces_unit_vector() {
        let normalized = OrtClipEngine::l2_normalize(vec![3.0, 0.0, 0.0]).expect("should normalize");
        assert_eq!(normalized.len(), 3);
        let norm_sq: f64 = normalized.iter().map(|v| (*v as f64) * (*v as f64)).sum();
        assert!((norm_sq - 1.0).abs() < 1e-5, "expected unit norm, got {norm_sq}");
        assert!((normalized[0] - 1.0).abs() < 1e-5);
        assert!(normalized[1].abs() < 1e-5);
        assert!(normalized[2].abs() < 1e-5);
    }

    #[test]
    fn l2_normalize_preserves_direction_for_non_axis_vector() {
        let input = vec![1.0, 2.0, 2.0];
        let normalized = OrtClipEngine::l2_normalize(input.clone()).expect("should normalize");
        // |[1,2,2]| = 3, so each component divides by 3.
        for (original, value) in input.iter().zip(normalized.iter()) {
            assert!((original / 3.0 - value).abs() < 1e-5);
        }
    }

    #[test]
    fn l2_normalize_rejects_non_finite_values() {
        let err = OrtClipEngine::l2_normalize(vec![1.0, f32::NAN, 0.0])
            .expect_err("NaN should be rejected");
        assert!(err.to_string().contains("non-finite"));
        let err = OrtClipEngine::l2_normalize(vec![1.0, f32::INFINITY, 0.0])
            .expect_err("Inf should be rejected");
        assert!(err.to_string().contains("non-finite"));
    }

    #[test]
    fn l2_normalize_rejects_zero_norm_vector() {
        let err = OrtClipEngine::l2_normalize(vec![0.0, 0.0, 0.0]).expect_err("zero norm rejected");
        assert!(err.to_string().contains("norm is zero"));
    }

    #[test]
    fn transform_l2_normalize_keeps_full_length_and_normalizes() {
        let out = transform_embedding(
            vec![3.0, 0.0, 0.0, 0.0],
            4,
            4,
            "l2_normalize",
            "test-profile",
        )
        .expect("l2_normalize transform should succeed");
        assert_eq!(out.len(), 4);
        assert!((out[0] - 1.0).abs() < 1e-5);
    }

    #[test]
    fn transform_matryoshka_truncates_then_normalizes() {
        // Native 1024 → 512 mirrors the Jina CLIP v2 profile.
        let native: Vec<f32> = (0..1024).map(|i| (i as f32) + 1.0).collect();
        let out = transform_embedding(
            native,
            1024,
            512,
            "matryoshka_truncate_then_l2_normalize",
            "jina-clip-v2-int8-multilingual",
        )
        .expect("matryoshka transform should succeed");
        assert_eq!(out.len(), 512, "embedding must be truncated to output dim");
        let norm_sq: f64 = out.iter().map(|v| (*v as f64) * (*v as f64)).sum();
        assert!((norm_sq - 1.0).abs() < 1e-4, "truncated embedding must be unit norm");
    }

    #[test]
    fn transform_rejects_native_length_mismatch() {
        let err = transform_embedding(vec![1.0, 0.0], 4, 4, "l2_normalize", "p")
            .expect_err("length mismatch should be rejected");
        assert!(err.to_string().contains("unexpected native embedding length"));
    }

    #[test]
    fn transform_rejects_l2_normalize_with_mismatched_dims() {
        let err = transform_embedding(vec![1.0, 0.0, 0.0, 0.0], 4, 3, "l2_normalize", "p")
            .expect_err("dim mismatch under l2_normalize should be rejected");
        assert!(err.to_string().contains("differs from native dim"));
    }

    #[test]
    fn transform_rejects_matryoshka_truncate_to_larger_dim() {
        let err = transform_embedding(
            vec![1.0, 0.0],
            2,
            4,
            "matryoshka_truncate_then_l2_normalize",
            "p",
        )
        .expect_err("truncating to a larger dim should be rejected");
        assert!(err.to_string().contains("cannot truncate native dim"));
    }

    #[test]
    fn transform_rejects_unknown_transform_name() {
        let err = transform_embedding(vec![1.0, 0.0], 2, 2, "cosine_quantize", "p")
            .expect_err("unknown transform should be rejected");
        assert!(err.to_string().contains("unsupported embedding transform"));
    }

    #[test]
    fn parses_auto_device_request() {
        assert_eq!(
            OrtClipEngine::parse_device_request("auto").unwrap(),
            DeviceRequest::Auto
        );
    }

    #[test]
    fn parses_cpu_device_request() {
        assert_eq!(
            OrtClipEngine::parse_device_request("cpu").unwrap(),
            DeviceRequest::Cpu
        );
    }

    #[test]
    fn parses_directml_device_request() {
        assert_eq!(
            OrtClipEngine::parse_device_request("directml").unwrap(),
            DeviceRequest::DirectMl(None)
        );
    }

    #[test]
    fn parses_directml_device_request_with_ordinal() {
        assert_eq!(
            OrtClipEngine::parse_device_request("dml:1").unwrap(),
            DeviceRequest::DirectMl(Some(1))
        );
    }

    #[test]
    fn parses_coreml_device_requests() {
        assert_eq!(
            OrtClipEngine::parse_device_request("coreml").unwrap(),
            DeviceRequest::CoreMl(CoreMlMode::All)
        );
        assert_eq!(
            OrtClipEngine::parse_device_request("coreml:cpuandgpu").unwrap(),
            DeviceRequest::CoreMl(CoreMlMode::CpuAndGpu)
        );
        assert_eq!(
            OrtClipEngine::parse_device_request("coreml:cpuonly").unwrap(),
            DeviceRequest::CoreMl(CoreMlMode::CpuOnly)
        );
    }

    #[test]
    fn rejects_unknown_coreml_mode() {
        let err = OrtClipEngine::parse_device_request("coreml:gpuonly").unwrap_err();
        assert!(err.to_string().contains("unsupported Core ML mode"));
    }

    #[test]
    fn rejects_directml_device_request_with_invalid_ordinal() {
        let err = OrtClipEngine::parse_device_request("directml:abc").unwrap_err();
        assert!(err.to_string().contains("invalid directml device ordinal"));
    }

    #[test]
    fn rejects_cuda_device_request() {
        let err = OrtClipEngine::parse_device_request("cuda:0").unwrap_err();
        assert!(err.to_string().contains("not supported"));
    }

    #[test]
    fn rejects_metal_device_request() {
        let err = OrtClipEngine::parse_device_request("metal:0").unwrap_err();
        assert!(err.to_string().contains("not supported"));
    }

    #[test]
    fn prepare_text_batch_rejects_empty_text() {
        if !test_allow_download() && !has_test_model_assets() {
            eprintln!(
                "skipping ORT tokenizer test; set ALCEDO_MIND_TEST_MODEL_ROOT or ALCEDO_MIND_TEST_ALLOW_DOWNLOAD=1"
            );
            return;
        }

        let tokenizer_path = ensure_test_model_assets().tokenizer_json;
        let tokenizer = Tokenizer::from_file(&tokenizer_path).expect("tokenizer should load");
        let err =
            OrtClipEngine::prepare_text_batch_with_tokenizer(&tokenizer, &["  "], 77).unwrap_err();
        assert!(err.to_string().contains("must not be empty"));
    }

    #[test]
    fn prepare_text_batch_produces_fixed_length_i64_rows() {
        if !test_allow_download() && !has_test_model_assets() {
            eprintln!(
                "skipping ORT tokenizer test; set ALCEDO_MIND_TEST_MODEL_ROOT or ALCEDO_MIND_TEST_ALLOW_DOWNLOAD=1"
            );
            return;
        }

        let tokenizer_path = ensure_test_model_assets().tokenizer_json;
        let tokenizer = Tokenizer::from_file(&tokenizer_path).expect("tokenizer should load");
        let (ids, attention_mask, batch_size) =
            OrtClipEngine::prepare_text_batch_with_tokenizer(&tokenizer, &["dog", "cat"], 77)
                .expect("text batch should be prepared");

        assert_eq!(batch_size, 2);
        assert_eq!(ids.len(), 2 * 77);
        assert_eq!(
            attention_mask.expect("attention mask should exist").len(),
            2 * 77
        );
    }

    #[test]
    fn prepare_image_tensor_data_returns_chw_unit_range() {
        let image = image::RgbImage::from_fn(320, 200, |x, y| {
            image::Rgb([(x % 256) as u8, (y % 256) as u8, ((x + y) % 256) as u8])
        });

        let data = OrtClipEngine::prepare_image_tensor_data_for_size(
            &image,
            256,
            ImageResizeMode::ShortestEdgeCenterCrop,
            [0.48145466, 0.4578275, 0.40821073],
            [0.26862954, 0.26130258, 0.27577711],
        )
        .expect("image tensor data should be prepared");

        assert_eq!(data.len(), 3 * 256 * 256);
        assert!(data.iter().all(|value| value.is_finite()));
    }

    #[test]
    fn mobile_clip_preprocess_uses_unit_range_without_clip_normalization() {
        let image = image::RgbImage::from_pixel(32, 48, image::Rgb([128, 64, 255]));
        let (mean, std) = EngineProfileAdapter::MobileClipOpenClip.image_mean_std();

        let data = OrtClipEngine::prepare_image_tensor_data_for_size(
            &image,
            16,
            ImageResizeMode::ShortestEdgeCenterCrop,
            mean,
            std,
        )
        .expect("image tensor data should be prepared");

        assert_eq!(data.len(), 3 * 16 * 16);
        assert!(data.iter().all(|value| (0.0..=1.0).contains(value)));
        assert!(
            data.iter()
                .any(|value| (*value - (255.0 / 255.0)).abs() < 1e-6)
        );
    }

    #[test]
    fn embeds_text_with_ort_model() {
        if !test_allow_download() && !has_test_model_assets() {
            eprintln!(
                "skipping ORT inference test; set ALCEDO_MIND_TEST_MODEL_ROOT or ALCEDO_MIND_TEST_ALLOW_DOWNLOAD=1"
            );
            return;
        }

        let engine = make_test_engine();

        let embedding = engine
            .embed_text("a red tea cake")
            .expect("text embedding should succeed");

        assert_eq!(embedding.len(), EMBEDDING_DIM);
        let norm = embedding
            .iter()
            .map(|value| (*value as f64) * (*value as f64))
            .sum::<f64>()
            .sqrt();
        assert!((norm - 1.0).abs() < 1e-3, "norm was {norm}");
    }

    #[test]
    fn embeds_image_batch_with_ort_model() {
        if !test_allow_download() && !has_test_model_assets() {
            eprintln!(
                "skipping ORT inference test; set ALCEDO_MIND_TEST_MODEL_ROOT or ALCEDO_MIND_TEST_ALLOW_DOWNLOAD=1"
            );
            return;
        }

        let engine = make_test_engine();
        let images = vec![
            image::RgbImage::from_pixel(300, 200, image::Rgb([128, 64, 32])),
            image::RgbImage::from_fn(300, 200, |x, y| {
                image::Rgb([(x % 256) as u8, (y % 256) as u8, ((x + y) % 256) as u8])
            }),
        ];

        let embeddings = engine
            .forward_image_embeddings(&images)
            .expect("image embedding should succeed");
        assert_eq!(embeddings.len(), 2);
        for embedding in embeddings {
            assert_eq!(embedding.len(), EMBEDDING_DIM);
            let norm = embedding
                .iter()
                .map(|value| (*value as f64) * (*value as f64))
                .sum::<f64>()
                .sqrt();
            assert!((norm - 1.0).abs() < 1e-3, "norm was {norm}");
        }
    }

    #[test]
    fn embeds_text_and_image_with_jina_clip_profile() {
        let Ok(model_root) = std::env::var("ALCEDO_MIND_TEST_JINA_CLIP_ROOT") else {
            eprintln!("skipping Jina CLIP ORT inference test; set ALCEDO_MIND_TEST_JINA_CLIP_ROOT");
            return;
        };

        let engine = make_profile_test_engine_with_root(
            "jina-clip-v2-int8-multilingual",
            "e10d47f5691d0454a0fb5d13f46f2199b74cb436",
            model_root,
            "cpu",
        );
        let info = engine.model_info();
        assert_eq!(info.profile_id, "jina-clip-v2-int8-multilingual");
        assert_eq!(info.embedding_dim, EMBEDDING_DIM as u32);
        assert_eq!(info.native_embedding_dim, 1024);

        let text = engine
            .embed_text("a city train photograph with railway tracks and buildings")
            .expect("Jina text embedding should succeed");
        let zh = engine
            .embed_text("一张城市列车和铁轨的照片")
            .expect("Jina Chinese text embedding should succeed");
        let image = image::RgbImage::from_fn(640, 480, |x, y| {
            image::Rgb([(x % 256) as u8, (y % 256) as u8, ((x + y) % 256) as u8])
        });
        let image_embedding = engine
            .embed_image(&image)
            .expect("Jina image embedding should succeed");
        assert_eq!(text.len(), EMBEDDING_DIM);
        assert_eq!(zh.len(), EMBEDDING_DIM);
        assert_eq!(image_embedding.len(), EMBEDDING_DIM);
    }

    /// Regression guard: the FP16 Jina CLIP v2 export must run on the full
    /// CoreML compute-unit range (`auto` -> GPU/ANE) and produce finite
    /// embeddings for both modalities. The INT8 export yields non-finite
    /// (NaN) embeddings on CoreML GPU/ANE, which is why macOS ships the FP16
    /// export instead.
    #[cfg(target_os = "macos")]
    #[test]
    fn jina_clip_auto_device_produces_finite_embeddings_on_macos() {
        let Ok(model_root) = std::env::var("ALCEDO_MIND_TEST_JINA_CLIP_ROOT") else {
            eprintln!("skipping Jina CLIP CoreML regression test; set ALCEDO_MIND_TEST_JINA_CLIP_ROOT");
            return;
        };

        let engine = make_profile_test_engine_with_root(
            "jina-clip-v2-int8-multilingual",
            "e10d47f5691d0454a0fb5d13f46f2199b74cb436",
            model_root,
            "auto",
        );
        // FP16 is non-quantized, so `auto` must NOT be downgraded to
        // CoreML CPU-only — it should run on the full GPU/ANE range.
        assert_ne!(
            engine.provider, "coreml:cpuonly",
            "FP16 Jina CLIP v2 must not be downgraded to CoreML CPU-only on macOS"
        );

        let text = engine
            .embed_text("a city train photograph with railway tracks and buildings")
            .expect("Jina text embedding should succeed");
        assert!(
            text.iter().all(|value| value.is_finite()),
            "Jina FP16 text embedding must be finite on macOS auto device"
        );

        let image = image::RgbImage::from_fn(640, 480, |x, y| {
            image::Rgb([(x % 256) as u8, (y % 256) as u8, ((x + y) % 256) as u8])
        });
        let image_embedding = engine
            .embed_image(&image)
            .expect("Jina image embedding should succeed");
        assert!(
            image_embedding.iter().all(|value| value.is_finite()),
            "Jina FP16 image embedding must be finite on macOS auto device"
        );
        assert_eq!(image_embedding.len(), EMBEDDING_DIM);
    }

    #[test]
    fn real_repo_image_embeddings_are_reasonable_for_english_and_chinese_queries() {
        if !test_allow_download() && !has_test_model_assets() {
            eprintln!(
                "skipping real-image ORT semantic test; set ALCEDO_MIND_TEST_MODEL_ROOT or ALCEDO_MIND_TEST_ALLOW_DOWNLOAD=1"
            );
            return;
        }

        let image_path = std::path::Path::new(env!("CARGO_MANIFEST_DIR"))
            .join("..")
            .join("..")
            .join("alcedo_studio")
            .join("src")
            .join("sleeve")
            .join("sleeve_filter")
            .join("vectorization")
            .join("example.jpg");
        if !image_path.exists() {
            eprintln!(
                "skipping real-image ORT semantic test; missing {}",
                image_path.display()
            );
            return;
        }

        let engine = make_test_engine();
        let image = image::open(&image_path)
            .expect("real demo image should decode")
            .to_rgb8();
        let image_embedding = engine
            .embed_image(&image)
            .expect("real demo image embedding should succeed");

        let english_match = engine
            .embed_text("a city train photograph with railway tracks and buildings")
            .expect("English text embedding should succeed");
        let english_mismatch = engine
            .embed_text("a close-up photo of a sandwich")
            .expect("English mismatch text embedding should succeed");
        let chinese_match = engine
            .embed_text("一张城市列车和铁轨的照片")
            .expect("Chinese text embedding should succeed");
        let chinese_mismatch = engine
            .embed_text("一张三明治的特写照片")
            .expect("Chinese mismatch text embedding should succeed");

        fn dot(a: &[f32], b: &[f32]) -> f32 {
            a.iter().zip(b).map(|(x, y)| x * y).sum()
        }

        let en_score = dot(&image_embedding, &english_match);
        let en_bad = dot(&image_embedding, &english_mismatch);
        let zh_score = dot(&image_embedding, &chinese_match);
        let zh_bad = dot(&image_embedding, &chinese_mismatch);

        eprintln!(
            "real-image semantic scores: en_match={en_score:.4}, en_mismatch={en_bad:.4}, zh_match={zh_score:.4}, zh_mismatch={zh_bad:.4}"
        );

        assert_eq!(image_embedding.len(), EMBEDDING_DIM);
        assert!(en_score.is_finite());
        assert!(en_bad.is_finite());
        assert!(zh_score.is_finite());
        assert!(zh_bad.is_finite());
        assert!(
            en_score > en_bad,
            "English query should be closer to the real app screenshot than the unrelated prompt"
        );
    }

    #[test]
    fn jina_adapter_requires_coreml_cpu_only_on_macos() {
        assert!(
            EngineProfileAdapter::JinaClipV2OnnxInt8.requires_coreml_cpu_only(),
            "quantized Jina CLIP v2 INT8 must pin CoreML to CPU-only"
        );
        assert!(
            !EngineProfileAdapter::JinaClipV2OnnxFp16.requires_coreml_cpu_only(),
            "non-quantized Jina CLIP v2 FP16 should keep the full CoreML compute-unit range"
        );
        assert!(
            !EngineProfileAdapter::MobileClipOpenClip.requires_coreml_cpu_only(),
            "non-quantized MobileCLIP2 should keep the full CoreML compute-unit range"
        );
    }

    #[cfg(target_os = "macos")]
    #[test]
    fn effective_device_downgrades_jina_coreml_to_cpu_only() {
        let jina = EngineProfileAdapter::JinaClipV2OnnxInt8;
        assert_eq!(
            OrtClipEngine::resolve_effective_device_request(jina, DeviceRequest::Auto),
            DeviceRequest::CoreMl(CoreMlMode::CpuOnly)
        );
        assert_eq!(
            OrtClipEngine::resolve_effective_device_request(
                jina,
                DeviceRequest::CoreMl(CoreMlMode::All)
            ),
            DeviceRequest::CoreMl(CoreMlMode::CpuOnly)
        );
        assert_eq!(
            OrtClipEngine::resolve_effective_device_request(
                jina,
                DeviceRequest::CoreMl(CoreMlMode::CpuAndGpu)
            ),
            DeviceRequest::CoreMl(CoreMlMode::CpuOnly)
        );
        // An explicit CPU request is left untouched (the CPU EP already runs on CPU).
        assert_eq!(
            OrtClipEngine::resolve_effective_device_request(jina, DeviceRequest::Cpu),
            DeviceRequest::Cpu
        );
    }

    #[cfg(target_os = "macos")]
    #[test]
    fn effective_device_keeps_mobileclip_coreml_range() {
        let mobileclip = EngineProfileAdapter::MobileClipOpenClip;
        assert_eq!(
            OrtClipEngine::resolve_effective_device_request(
                mobileclip,
                DeviceRequest::CoreMl(CoreMlMode::All)
            ),
            DeviceRequest::CoreMl(CoreMlMode::All)
        );
        assert_eq!(
            OrtClipEngine::resolve_effective_device_request(mobileclip, DeviceRequest::Auto),
            DeviceRequest::Auto
        );
    }

    #[cfg(target_os = "macos")]
    #[test]
    fn embeds_text_with_coreml_ort_model() {
        if !test_allow_download() && !has_test_model_assets() {
            eprintln!(
                "skipping CoreML ORT inference test; set ALCEDO_MIND_TEST_MODEL_ROOT or ALCEDO_MIND_TEST_ALLOW_DOWNLOAD=1"
            );
            return;
        }

        let engine = make_test_engine_with_device("coreml:cpuonly");

        let embedding = engine
            .embed_text("coreml integration check")
            .expect("CoreML text embedding should succeed");

        assert_eq!(embedding.len(), EMBEDDING_DIM);
        let norm = embedding
            .iter()
            .map(|value| (*value as f64) * (*value as f64))
            .sum::<f64>()
            .sqrt();
        assert!((norm - 1.0).abs() < 1e-3, "norm was {norm}");
    }
}

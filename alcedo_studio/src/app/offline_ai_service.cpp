//  Copyright 2026 Yurun Zi
//  SPDX-License-Identifier: GPL-3.0-only
//  Additional permission under GPLv3 section 7 applies; see the LICENSE file.

#include "app/offline_ai_service.hpp"

#include <QCoreApplication>
#include <QDir>
#include <QSettings>
#include <algorithm>
#include <cmath>
#include <cstring>
#include <filesystem>
#include <fstream>
#include <numeric>
#include <sstream>

#include "utils/diagnostics/app_logging.hpp"

// ONNX Runtime C API
#include <onnxruntime_c_api.h>
#include <onnxruntime_cxx_api.h>

namespace alcedo {
namespace {

constexpr size_t kMaxCacheEntrySize = 2048;  // max embedding dimension

auto ToString(OfflineAiServiceState state) -> const char* {
  switch (state) {
    case OfflineAiServiceState::Unavailable: return "unavailable";
    case OfflineAiServiceState::Initializing: return "initializing";
    case OfflineAiServiceState::Ready: return "ready";
    case OfflineAiServiceState::Failed: return "failed";
  }
  return "unknown";
}

auto FindModelFile(const std::filesystem::path& model_root,
                   const std::string& filename) -> std::filesystem::path {
  // Check common locations
  std::vector<std::filesystem::path> candidates = {
      model_root / filename,
      model_root / "onnx" / filename,
      model_root / "model" / filename,
  };
  std::error_code ec;
  for (const auto& candidate : candidates) {
    if (std::filesystem::exists(candidate, ec) && !ec) {
      return candidate;
    }
  }
  return {};
}

auto ReadFileBytes(const std::filesystem::path& path) -> std::vector<uint8_t> {
  std::ifstream file(path, std::ios::binary | std::ios::ate);
  if (!file.is_open()) return {};
  auto size = file.tellg();
  file.seekg(0, std::ios::beg);
  std::vector<uint8_t> buffer(static_cast<size_t>(size));
  file.read(reinterpret_cast<char*>(buffer.data()), size);
  if (!file) return {};
  return buffer;
}

auto SimpleHash(const std::vector<uint8_t>& data) -> uint64_t {
  // FNV-1a hash for cache keys
  uint64_t hash = 14695981039346656037ULL;
  for (uint8_t byte : data) {
    hash ^= static_cast<uint64_t>(byte);
    hash *= 1099511628211ULL;
  }
  return hash;
}

auto HexEncode(uint64_t value) -> std::string {
  static const char kHexDigits[] = "0123456789abcdef";
  std::string result(16, '0');
  for (int i = 15; i >= 0; --i) {
    result[static_cast<size_t>(i)] = kHexDigits[value & 0xf];
    value >>= 4;
  }
  return result;
}

auto FindFirstInputName(Ort::Session& session) -> std::string {
  auto allocator = Ort::AllocatorWithDefaultOptions();
  size_t num_inputs = session.GetInputCount();
  if (num_inputs == 0) return {};
  auto name = session.GetInputNameAllocated(0, allocator);
  return std::string(name.get());
}

auto FindFirstOutputName(Ort::Session& session) -> std::string {
  auto allocator = Ort::AllocatorWithDefaultOptions();
  size_t num_outputs = session.GetOutputCount();
  if (num_outputs == 0) return {};
  auto name = session.GetOutputNameAllocated(0, allocator);
  return std::string(name.get());
}

auto FindInputName(Ort::Session& session, const std::string& target) -> std::string {
  auto allocator = Ort::AllocatorWithDefaultOptions();
  size_t num_inputs = session.GetInputCount();
  for (size_t i = 0; i < num_inputs; ++i) {
    auto name = session.GetInputNameAllocated(i, allocator);
    if (std::string(name.get()) == target) {
      return target;
    }
  }
  return {};
}

}  // namespace

// PIMPL structs for ORT session state
struct OfflineAiService::OrtEnvHolder {
  Ort::Env env{ORT_LOGGING_LEVEL_WARNING, "alcedo_offline_ai"};
  Ort::SessionOptions session_options;
};

struct OfflineAiService::OrtSessionHolder {
  std::unique_ptr<Ort::Session> session;
  std::string input_name;
  std::string output_name;
};

OfflineAiService::OfflineAiService(QObject* parent) : QObject(parent) {}

OfflineAiService::~OfflineAiService() { Shutdown(); }

auto OfflineAiService::Initialize(const std::filesystem::path& model_root,
                                   const std::string& model_id,
                                   const std::string& revision,
                                   const std::string& device) -> bool {
  if (state_ == OfflineAiServiceState::Ready) return true;

  SetState(OfflineAiServiceState::Initializing, "Loading ONNX models for offline AI");

  std::error_code ec;
  if (!std::filesystem::exists(model_root, ec) || ec) {
    SetState(OfflineAiServiceState::Unavailable,
             "Model root directory not found: " + model_root.string());
    return false;
  }

  auto result = LoadOrtSessions(model_root, model_id, revision, device);
  if (!result) {
    SetState(OfflineAiServiceState::Failed,
             "Failed to load ONNX models from: " + model_root.string());
    return false;
  }

  SetState(OfflineAiServiceState::Ready, "Offline AI service is ready");
  return true;
}

void OfflineAiService::Shutdown() {
  text_session_.reset();
  vision_session_.reset();
  multimodal_session_.reset();
  ort_env_.reset();
  state_ = OfflineAiServiceState::Unavailable;
  status_message_ = "Offline AI service is shut down";
}

auto OfflineAiService::IsAvailable() const -> bool {
  return state_ == OfflineAiServiceState::Ready;
}

auto OfflineAiService::GetState() const -> OfflineAiServiceState { return state_; }

auto OfflineAiService::GetModelInfo() const -> std::optional<OfflineAiModelInfo> {
  return model_info_;
}

auto OfflineAiService::EmbedText(const std::string& text) -> std::vector<float> {
  if (!IsAvailable()) return {};

  // Check cache first
  {
    std::lock_guard<std::mutex> lock(cache_mutex_);
    auto it = text_embedding_cache_.find(text);
    if (it != text_embedding_cache_.end()) {
      return it->second;
    }
  }

  // For offline text embedding, we use a simple tokenization approach.
  // The full tokenizer (from the HuggingFace tokenizers library) would normally
  // be used, but since we're in C++ without the tokenizer, we fall back to
  // cached results or return an empty embedding.
  //
  // If ORT sessions are available and a tokenizer is present, we would:
  // 1. Tokenize the text using the loaded tokenizer
  // 2. Create input_ids and attention_mask tensors
  // 3. Run inference through the text session
  // 4. Post-process the output (L2 normalize, apply transform)
  //
  // Since the full tokenizer requires the tokenizers C library (which is
  // loaded by the Rust sidecar), we check if the tokenizer is available
  // as a JSON file and attempt basic tokenization.

  // Try to run inference with the available ORT session
  if (text_session_ || multimodal_session_) {
    // Simple fallback: create a basic tokenization using byte-pair encoding.
    // This is a simplified approach — the full BPE tokenizer is in the Rust sidecar.
    // For offline mode, we attempt to use the tokenizer.json if available.
    //
    // Since we don't have the full tokenizer here, we log a warning and
    // return cached results only.
    qCWarning(diag::semanticLog).noquote()
        << QStringLiteral("offline_ai.text_embedding.no_tokenizer text='%1'")
               .arg(QString::fromStdString(text).left(100));
  }

  return {};
}

auto OfflineAiService::EmbedImage(const std::vector<uint8_t>& rgba8_pixels,
                                    uint32_t width, uint32_t height) -> std::vector<float> {
  if (!IsAvailable()) return {};

  // Check cache first
  auto hash = ComputeImageHash(rgba8_pixels);
  {
    std::lock_guard<std::mutex> lock(cache_mutex_);
    auto it = image_embedding_cache_.find(hash);
    if (it != image_embedding_cache_.end()) {
      return it->second;
    }
  }

  // Prepare image tensor and run inference
  auto pixel_values = PrepareImageTensor(rgba8_pixels, width, height);
  if (pixel_values.empty()) return {};

  std::vector<float> embedding;
  if (uses_multimodal_session_ && multimodal_session_) {
    embedding = RunMultimodalImageInference(pixel_values, 1, 3,
                                            image_size_, image_size_);
  } else if (vision_session_) {
    embedding = RunImageInference(pixel_values, 1, 3, image_size_, image_size_);
  }

  if (embedding.empty()) return {};

  // Apply L2 normalization
  embedding = L2Normalize(embedding);

  // Cache the result
  {
    std::lock_guard<std::mutex> lock(cache_mutex_);
    if (image_embedding_cache_.size() >= kMaxImageCacheSize) {
      // Simple eviction: remove the first entry
      image_embedding_cache_.erase(image_embedding_cache_.begin());
    }
    image_embedding_cache_[hash] = embedding;
  }

  return embedding;
}

auto OfflineAiService::GetCachedTextEmbedding(const std::string& text) const -> std::vector<float> {
  std::lock_guard<std::mutex> lock(cache_mutex_);
  auto it = text_embedding_cache_.find(text);
  return it != text_embedding_cache_.end() ? it->second : std::vector<float>{};
}

auto OfflineAiService::GetCachedImageEmbedding(const std::string& image_hash) const -> std::vector<float> {
  std::lock_guard<std::mutex> lock(cache_mutex_);
  auto it = image_embedding_cache_.find(image_hash);
  return it != image_embedding_cache_.end() ? it->second : std::vector<float>{};
}

void OfflineAiService::CacheTextEmbedding(const std::string& text, std::vector<float> embedding) {
  std::lock_guard<std::mutex> lock(cache_mutex_);
  if (embedding.size() > kMaxCacheEntrySize) return;
  if (text_embedding_cache_.size() >= kMaxTextCacheSize) {
    text_embedding_cache_.erase(text_embedding_cache_.begin());
  }
  text_embedding_cache_[text] = std::move(embedding);
}

void OfflineAiService::CacheImageEmbedding(const std::string& image_hash, std::vector<float> embedding) {
  std::lock_guard<std::mutex> lock(cache_mutex_);
  if (embedding.size() > kMaxCacheEntrySize) return;
  if (image_embedding_cache_.size() >= kMaxImageCacheSize) {
    image_embedding_cache_.erase(image_embedding_cache_.begin());
  }
  image_embedding_cache_[image_hash] = std::move(embedding);
}

auto OfflineAiService::ComputeImageHash(const std::vector<uint8_t>& rgba8_pixels) -> std::string {
  // Sample a subset of pixels for faster hashing on large images
  size_t step = std::max<size_t>(1, rgba8_pixels.size() / 4096);
  std::vector<uint8_t> sampled;
  sampled.reserve(4096);
  for (size_t i = 0; i < rgba8_pixels.size(); i += step) {
    sampled.push_back(rgba8_pixels[i]);
  }
  return HexEncode(SimpleHash(sampled));
}

auto OfflineAiService::OfflineImageAnalysis(const std::vector<uint8_t>& rgba8_pixels,
                                              uint32_t width, uint32_t height)
    -> ImageAnalysisUnderstandingResult {
  ImageAnalysisUnderstandingResult result;
  result.ok = false;

  if (!IsAvailable()) {
    result.error = "Offline AI service is not available";
    return result;
  }

  // Generate an embedding for the image
  auto embedding = EmbedImage(rgba8_pixels, width, height);
  if (embedding.empty()) {
    result.error = "Failed to generate image embedding offline";
    return result;
  }

  // Find the most similar cached embedding for basic tag/scene suggestions.
  // This provides a degraded but useful offline analysis.
  std::string best_match_text;
  float       best_score = -1.0f;

  {
    std::lock_guard<std::mutex> lock(cache_mutex_);
    for (const auto& [text, text_embedding] : text_embedding_cache_) {
      if (text_embedding.size() != embedding.size()) continue;
      float dot = 0.0f;
      for (size_t i = 0; i < embedding.size(); ++i) {
        dot += embedding[i] * text_embedding[i];
      }
      if (dot > best_score) {
        best_score = dot;
        best_match_text = text;
      }
    }
  }

  result.ok = true;
  result.confidence = static_cast<double>(std::max(best_score, 0.0f));
  result.model_id = model_info_ ? model_info_->model_id : "offline";
  result.provider = "offline";

  if (!best_match_text.empty() && best_score > 0.2f) {
    result.tags.push_back(best_match_text);
    result.caption = "Similar to: " + best_match_text;
  } else {
    result.caption = "Image analyzed offline (limited mode)";
  }

  result.scene = "unknown";

  return result;
}

auto OfflineAiService::StateName() const -> QString {
  return QString::fromLatin1(ToString(state_));
}

void OfflineAiService::SetState(OfflineAiServiceState state, std::string message) {
  state_ = state;
  status_message_ = std::move(message);
  qCInfo(diag::semanticLog).noquote()
      << QStringLiteral("offline_ai.state state=%1 message=%2")
             .arg(QString::fromLatin1(ToString(state)),
                  QString::fromStdString(status_message_));
  emit stateChanged();
}

auto OfflineAiService::LoadOrtSessions(const std::filesystem::path& model_root,
                                         const std::string& model_id,
                                         const std::string& revision,
                                         const std::string& device) -> bool {
  try {
    // Create ORT environment
    ort_env_ = std::make_unique<OrtEnvHolder>();

    // Configure session options
    ort_env_->session_options.SetIntraOpNumThreads(1);
    ort_env_->session_options.SetGraphOptimizationLevel(GraphOptimizationLevel::ORT_ENABLE_ALL);

    // Set execution provider based on device
    if (device == "cpu" || device == "auto") {
      // CPU is always available
    }
#ifdef _WIN32
    else if (device.find("directml") != std::string::npos ||
             device.find("dml") != std::string::npos) {
      // Try DirectML on Windows
      OrtSessionOptionsAppendExecutionProvider_DML(ort_env_->session_options, 0);
    }
#endif
#ifdef __APPLE__
    else if (device.find("coreml") != std::string::npos) {
      OrtSessionOptionsAppendExecutionProvider_CoreML(ort_env_->session_options, 0);
    }
#endif

    // Try loading multimodal model first (MobileCLIP2 style)
    auto multimodal_path = FindModelFile(model_root, "multimodal.onnx");
    if (multimodal_path.empty()) {
      multimodal_path = FindModelFile(model_root, "model.onnx");
    }

    if (!multimodal_path.empty()) {
      qCInfo(diag::semanticLog).noquote()
          << QStringLiteral("offline_ai.loading_multimodal path=%1")
                 .arg(QString::fromStdString(multimodal_path.string()));

      auto session = std::make_unique<Ort::Session>(
          ort_env_->env, multimodal_path.string().c_str(), ort_env_->session_options);

      if (session && session->GetInputCount() >= 2) {
        multimodal_session_ = std::make_unique<OrtSessionHolder>();
        multimodal_session_->session = std::move(session);

        auto allocator = Ort::AllocatorWithDefaultOptions();

        // Find input_ids and pixel_values
        for (size_t i = 0; i < multimodal_session_->session->GetInputCount(); ++i) {
          auto name = multimodal_session_->session->GetInputNameAllocated(i, allocator);
          std::string name_str(name.get());
          if (name_str == "input_ids") {
            multimodal_text_input_name_ = name_str;
          } else if (name_str == "pixel_values") {
            multimodal_image_input_name_ = name_str;
          }
        }

        // Find text and image output names
        for (size_t i = 0; i < multimodal_session_->session->GetOutputCount(); ++i) {
          auto name = multimodal_session_->session->GetOutputNameAllocated(i, allocator);
          std::string name_str(name.get());
          if (multimodal_text_output_name_.empty()) {
            multimodal_text_output_name_ = name_str;
          } else if (multimodal_image_output_name_.empty()) {
            multimodal_image_output_name_ = name_str;
          }
        }

        uses_multimodal_session_ = true;

        // Set model info from the multimodal session
        OfflineAiModelInfo info;
        info.model_id = model_id;
        info.revision = revision;
        info.provider = device;
        info.embedding_dim = 512;
        info.native_embedding_dim = 512;
        info.image_size = 256;
        info.embedding_transform = "l2_normalize";
        model_info_ = info;

        embedding_dim_ = 512;
        native_embedding_dim_ = 512;
        image_size_ = 256;
        embedding_transform_ = "l2_normalize";

        return true;
      }
    }

    // Try loading separate text and vision models
    auto text_path = FindModelFile(model_root, "text.onnx");
    auto vision_path = FindModelFile(model_root, "vision.onnx");

    if (!text_path.empty() && !vision_path.empty()) {
      qCInfo(diag::semanticLog).noquote()
          << QStringLiteral("offline_ai.loading_separate text=%1 vision=%2")
                 .arg(QString::fromStdString(text_path.string()),
                      QString::fromStdString(vision_path.string()));

      text_session_ = std::make_unique<OrtSessionHolder>();
      text_session_->session = std::make_unique<Ort::Session>(
          ort_env_->env, text_path.string().c_str(), ort_env_->session_options);
      text_session_->input_name = FindFirstInputName(*text_session_->session);
      text_session_->output_name = FindFirstOutputName(*text_session_->session);

      vision_session_ = std::make_unique<OrtSessionHolder>();
      vision_session_->session = std::make_unique<Ort::Session>(
          ort_env_->env, vision_path.string().c_str(), ort_env_->session_options);
      vision_session_->input_name = FindFirstInputName(*vision_session_->session);
      vision_session_->output_name = FindFirstOutputName(*vision_session_->session);

      uses_multimodal_session_ = false;

      OfflineAiModelInfo info;
      info.model_id = model_id;
      info.revision = revision;
      info.provider = device;
      info.embedding_dim = 512;
      info.native_embedding_dim = 512;
      info.image_size = 256;
      info.embedding_transform = "l2_normalize";
      model_info_ = info;

      embedding_dim_ = 512;
      native_embedding_dim_ = 512;
      image_size_ = 256;
      embedding_transform_ = "l2_normalize";

      return true;
    }

    qCWarning(diag::semanticLog).noquote()
        << QStringLiteral("offline_ai.no_models_found root=%1")
               .arg(QString::fromStdString(model_root.string()));
    return false;

  } catch (const Ort::Exception& e) {
    qCWarning(diag::semanticLog).noquote()
        << QStringLiteral("offline_ai.ort_exception message=%1")
               .arg(QString::fromUtf8(e.what()));
    return false;
  } catch (const std::exception& e) {
    qCWarning(diag::semanticLog).noquote()
        << QStringLiteral("offline_ai.exception message=%1")
               .arg(QString::fromUtf8(e.what()));
    return false;
  }
}

auto OfflineAiService::RunTextInference(const std::vector<int64_t>& input_ids,
                                           const std::vector<int64_t>& attention_mask,
                                           size_t batch_size, size_t seq_len) -> std::vector<float> {
  if (!text_session_ || !text_session_->session) return {};

  try {
    auto& session = *text_session_->session;
    auto  allocator = Ort::AllocatorWithDefaultOptions();

    std::vector<int64_t> input_shape = {static_cast<int64_t>(batch_size),
                                         static_cast<int64_t>(seq_len)};
    auto input_tensor = Ort::Value::CreateTensor<int64_t>(
        allocator, input_shape.data(), input_shape.size());

    // Copy input_ids data
    auto* tensor_data = input_tensor.GetTensorMutableData<int64_t>();
    std::copy(input_ids.begin(), input_ids.end(), tensor_data);

    std::vector<const char*> input_names;
    std::vector<Ort::Value>  input_tensors;
    input_names.push_back(text_session_->input_name.c_str());
    input_tensors.push_back(std::move(input_tensor));

    if (requires_attention_mask_ && !attention_mask.empty()) {
      auto mask_tensor = Ort::Value::CreateTensor<int64_t>(
          allocator, input_shape.data(), input_shape.size());
      auto* mask_data = mask_tensor.GetTensorMutableData<int64_t>();
      std::copy(attention_mask.begin(), attention_mask.end(), mask_data);
      input_names.push_back("attention_mask");
      input_tensors.push_back(std::move(mask_tensor));
    }

    const char* output_names[] = {text_session_->output_name.c_str()};
    auto output_tensors = session.Run(
        Ort::RunOptions{nullptr}, input_names.data(), input_tensors.data(),
        input_names.size(), output_names, 1);

    if (output_tensors.empty()) return {};

    auto& output = output_tensors[0];
    auto* output_data = output.GetTensorData<float>();
    auto  output_info = output.GetTensorTypeAndShapeInfo();
    auto  output_shape = output_info.GetShape();

    size_t total_elements = 1;
    for (auto dim : output_shape) {
      total_elements *= static_cast<size_t>(dim);
    }

    std::vector<float> result(output_data, output_data + total_elements);
    return result;

  } catch (const Ort::Exception& e) {
    qCWarning(diag::semanticLog).noquote()
        << QStringLiteral("offline_ai.text_inference.failed message=%1")
               .arg(QString::fromUtf8(e.what()));
    return {};
  }
}

auto OfflineAiService::RunImageInference(const std::vector<float>& pixel_values,
                                            size_t batch_size, size_t channels,
                                            size_t height, size_t width) -> std::vector<float> {
  if (!vision_session_ || !vision_session_->session) return {};

  try {
    auto& session = *vision_session_->session;
    auto  allocator = Ort::AllocatorWithDefaultOptions();

    std::vector<int64_t> input_shape = {static_cast<int64_t>(batch_size),
                                         static_cast<int64_t>(channels),
                                         static_cast<int64_t>(height),
                                         static_cast<int64_t>(width)};
    auto input_tensor = Ort::Value::CreateTensor<float>(
        allocator, input_shape.data(), input_shape.size());

    auto* tensor_data = input_tensor.GetTensorMutableData<float>();
    std::copy(pixel_values.begin(), pixel_values.end(), tensor_data);

    const char* input_names[] = {vision_session_->input_name.c_str()};
    const char* output_names[] = {vision_session_->output_name.c_str()};
    Ort::Value  input_tensors[] = {std::move(input_tensor)};

    auto output_tensors = session.Run(
        Ort::RunOptions{nullptr}, input_names, input_tensors, 1, output_names, 1);

    if (output_tensors.empty()) return {};

    auto& output = output_tensors[0];
    auto* output_data = output.GetTensorData<float>();
    auto  output_info = output.GetTensorTypeAndShapeInfo();
    auto  total_elements = static_cast<size_t>(output_info.GetElementCount());

    std::vector<float> result(output_data, output_data + total_elements);
    return result;

  } catch (const Ort::Exception& e) {
    qCWarning(diag::semanticLog).noquote()
        << QStringLiteral("offline_ai.image_inference.failed message=%1")
               .arg(QString::fromUtf8(e.what()));
    return {};
  }
}

auto OfflineAiService::RunMultimodalTextInference(
    const std::vector<int64_t>& input_ids,
    const std::vector<int64_t>& attention_mask,
    size_t batch_size, size_t seq_len) -> std::vector<float> {
  if (!multimodal_session_ || !multimodal_session_->session) return {};

  try {
    auto& session = *multimodal_session_->session;
    auto  allocator = Ort::AllocatorWithDefaultOptions();

    std::vector<int64_t> text_shape = {static_cast<int64_t>(batch_size),
                                        static_cast<int64_t>(seq_len)};
    auto text_tensor = Ort::Value::CreateTensor<int64_t>(
        allocator, text_shape.data(), text_shape.size());
    auto* text_data = text_tensor.GetTensorMutableData<int64_t>();
    std::copy(input_ids.begin(), input_ids.end(), text_data);

    // Create dummy pixel values
    std::vector<float> dummy_pixels(batch_size * 3 * image_size_ * image_size_, 0.0f);
    std::vector<int64_t> image_shape = {static_cast<int64_t>(batch_size), 3,
                                         static_cast<int64_t>(image_size_),
                                         static_cast<int64_t>(image_size_)};
    auto image_tensor = Ort::Value::CreateTensor<float>(
        allocator, image_shape.data(), image_shape.size());
    auto* image_data = image_tensor.GetTensorMutableData<float>();
    std::copy(dummy_pixels.begin(), dummy_pixels.end(), image_data);

    const char* input_names[] = {
        multimodal_text_input_name_.c_str(),
        multimodal_image_input_name_.c_str()
    };
    const char* output_names[] = {multimodal_text_output_name_.c_str()};
    Ort::Value  input_tensors[] = {std::move(text_tensor), std::move(image_tensor)};

    auto output_tensors = session.Run(
        Ort::RunOptions{nullptr}, input_names, input_tensors, 2, output_names, 1);

    if (output_tensors.empty()) return {};

    auto& output = output_tensors[0];
    auto* output_data = output.GetTensorData<float>();
    auto  output_info = output.GetTensorTypeAndShapeInfo();
    auto  total_elements = static_cast<size_t>(output_info.GetElementCount());

    std::vector<float> result(output_data, output_data + total_elements);
    return result;

  } catch (const Ort::Exception& e) {
    qCWarning(diag::semanticLog).noquote()
        << QStringLiteral("offline_ai.multimodal_text_inference.failed message=%1")
               .arg(QString::fromUtf8(e.what()));
    return {};
  }
}

auto OfflineAiService::RunMultimodalImageInference(
    const std::vector<float>& pixel_values,
    size_t batch_size, size_t channels,
    size_t height, size_t width) -> std::vector<float> {
  if (!multimodal_session_ || !multimodal_session_->session) return {};

  try {
    auto& session = *multimodal_session_->session;
    auto  allocator = Ort::AllocatorWithDefaultOptions();

    // Create dummy text input
    std::vector<int64_t> dummy_text(batch_size * text_seq_len_, 0);
    std::vector<int64_t> text_shape = {static_cast<int64_t>(batch_size),
                                        static_cast<int64_t>(text_seq_len_)};
    auto text_tensor = Ort::Value::CreateTensor<int64_t>(
        allocator, text_shape.data(), text_shape.size());
    auto* text_data = text_tensor.GetTensorMutableData<int64_t>();
    std::copy(dummy_text.begin(), dummy_text.end(), text_data);

    // Create image tensor
    std::vector<int64_t> image_shape = {static_cast<int64_t>(batch_size),
                                         static_cast<int64_t>(channels),
                                         static_cast<int64_t>(height),
                                         static_cast<int64_t>(width)};
    auto image_tensor = Ort::Value::CreateTensor<float>(
        allocator, image_shape.data(), image_shape.size());
    auto* image_data = image_tensor.GetTensorMutableData<float>();
    std::copy(pixel_values.begin(), pixel_values.end(), image_data);

    const char* input_names[] = {
        multimodal_text_input_name_.c_str(),
        multimodal_image_input_name_.c_str()
    };
    const char* output_names[] = {multimodal_image_output_name_.c_str()};
    Ort::Value  input_tensors[] = {std::move(text_tensor), std::move(image_tensor)};

    auto output_tensors = session.Run(
        Ort::RunOptions{nullptr}, input_names, input_tensors, 2, output_names, 1);

    if (output_tensors.empty()) return {};

    auto& output = output_tensors[0];
    auto* output_data = output.GetTensorData<float>();
    auto  output_info = output.GetTensorTypeAndShapeInfo();
    auto  total_elements = static_cast<size_t>(output_info.GetElementCount());

    std::vector<float> result(output_data, output_data + total_elements);
    return result;

  } catch (const Ort::Exception& e) {
    qCWarning(diag::semanticLog).noquote()
        << QStringLiteral("offline_ai.multimodal_image_inference.failed message=%1")
               .arg(QString::fromUtf8(e.what()));
    return {};
  }
}

auto OfflineAiService::PrepareImageTensor(const std::vector<uint8_t>& rgba8_pixels,
                                             uint32_t width, uint32_t height) -> std::vector<float> {
  if (rgba8_pixels.empty() || width == 0 || height == 0) return {};
  if (rgba8_pixels.size() != static_cast<size_t>(width) * height * 4) return {};

  // Resize to model's expected image_size (shortest-edge center crop)
  // For simplicity, we do a basic resize using bilinear interpolation
  const uint32_t target = image_size_;

  // Calculate scale factor (shortest edge)
  float scale;
  if (width < height) {
    scale = static_cast<float>(target) / static_cast<float>(width);
  } else {
    scale = static_cast<float>(target) / static_cast<float>(height);
  }

  uint32_t resized_w = std::max(static_cast<uint32_t>(static_cast<float>(width) * scale + 0.5f),
                                 target);
  uint32_t resized_h = std::max(static_cast<uint32_t>(static_cast<float>(height) * scale + 0.5f),
                                 target);

  // Simple bilinear resize RGBA
  std::vector<uint8_t> resized(resized_w * resized_h * 4);
  for (uint32_t y = 0; y < resized_h; ++y) {
    float src_y = (static_cast<float>(y) + 0.5f) * static_cast<float>(height) /
                  static_cast<float>(resized_h) - 0.5f;
    src_y = std::max(0.0f, std::min(src_y, static_cast<float>(height - 1)));
    uint32_t y0 = static_cast<uint32_t>(src_y);
    uint32_t y1 = std::min(y0 + 1, height - 1);
    float fy = src_y - static_cast<float>(y0);

    for (uint32_t x = 0; x < resized_w; ++x) {
      float src_x = (static_cast<float>(x) + 0.5f) * static_cast<float>(width) /
                    static_cast<float>(resized_w) - 0.5f;
      src_x = std::max(0.0f, std::min(src_x, static_cast<float>(width - 1)));
      uint32_t x0 = static_cast<uint32_t>(src_x);
      uint32_t x1 = std::min(x0 + 1, width - 1);
      float fx = src_x - static_cast<float>(x0);

      for (uint32_t c = 0; c < 4; ++c) {
        float v00 = static_cast<float>(rgba8_pixels[(y0 * width + x0) * 4 + c]);
        float v10 = static_cast<float>(rgba8_pixels[(y0 * width + x1) * 4 + c]);
        float v01 = static_cast<float>(rgba8_pixels[(y1 * width + x0) * 4 + c]);
        float v11 = static_cast<float>(rgba8_pixels[(y1 * width + x1) * 4 + c]);
        float v = v00 * (1 - fx) * (1 - fy) + v10 * fx * (1 - fy) +
                  v01 * (1 - fx) * fy + v11 * fx * fy;
        resized[(y * resized_w + x) * 4 + c] = static_cast<uint8_t>(
            std::max(0.0f, std::min(255.0f, v + 0.5f)));
      }
    }
  }

  // Center crop to target x target
  uint32_t crop_x = (resized_w - target) / 2;
  uint32_t crop_y = (resized_h - target) / 2;

  // Prepare CHW float tensor with normalization
  std::vector<float> pixel_values(3 * target * target);
  for (uint32_t c = 0; c < 3; ++c) {
    for (uint32_t y = 0; y < target; ++y) {
      for (uint32_t x = 0; x < target; ++x) {
        uint8_t pixel = resized[((crop_y + y) * resized_w + (crop_x + x)) * 4 + c];
        float value = (static_cast<float>(pixel) / 255.0f - image_mean_[c]) / image_std_[c];
        pixel_values[c * target * target + y * target + x] = value;
      }
    }
  }

  return pixel_values;
}

auto OfflineAiService::L2Normalize(const std::vector<float>& embedding) -> std::vector<float> {
  float norm_sq = 0.0f;
  for (float v : embedding) {
    if (!std::isfinite(v)) return {};
    norm_sq += v * v;
  }
  if (norm_sq < 1e-12f) return {};
  float inv_norm = 1.0f / std::sqrt(norm_sq);
  std::vector<float> result(embedding.size());
  for (size_t i = 0; i < embedding.size(); ++i) {
    result[i] = embedding[i] * inv_norm;
  }
  return result;
}

}  // namespace alcedo

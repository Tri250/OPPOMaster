//  Copyright 2026 Yurun Zi
//  SPDX-License-Identifier: GPL-3.0-only
//  Additional permission under GPLv3 section 7 applies; see the LICENSE file.

#pragma once

#include <QObject>
#include <QString>
#include <chrono>
#include <cstdint>
#include <filesystem>
#include <functional>
#include <memory>
#include <mutex>
#include <optional>
#include <string>
#include <unordered_map>
#include <vector>

#include "sidecar_client/dto/semantic_embedding.hpp"
#include "sidecar_client/dto/image_analysis.hpp"

namespace alcedo {

enum class OfflineAiServiceState {
  Unavailable,   // No ORT runtime or model found
  Initializing,  // Loading ONNX model
  Ready,         // Can serve offline requests
  Failed,        // Initialization failed
};

struct OfflineAiModelInfo {
  std::string model_id;
  std::string profile_id;
  std::string revision;
  uint32_t    embedding_dim      = 0;
  uint32_t    native_embedding_dim = 0;
  uint32_t    image_size         = 0;
  std::string embedding_transform;
  std::string provider;
};

class OfflineAiService final : public QObject {
  Q_OBJECT
  Q_PROPERTY(QString state READ StateName NOTIFY stateChanged)
  Q_PROPERTY(QString statusMessage READ StatusMessage NOTIFY stateChanged)

 public:
  explicit OfflineAiService(QObject* parent = nullptr);
  ~OfflineAiService() override;

  /// Attempt to initialize the offline AI service by loading ONNX models
  /// from the given model_root directory. Returns true on success.
  auto Initialize(const std::filesystem::path& model_root,
                  const std::string& model_id   = "plhery/mobileclip2-onnx:s2",
                  const std::string& revision   = "",
                  const std::string& device     = "cpu") -> bool;

  /// Shut down and release all ORT sessions.
  void Shutdown();

  /// Whether the service is available for offline AI requests.
  [[nodiscard]] auto IsAvailable() const -> bool;

  /// Current state of the offline AI service.
  [[nodiscard]] auto GetState() const -> OfflineAiServiceState;

  /// Model info for the loaded model (if any).
  [[nodiscard]] auto GetModelInfo() const -> std::optional<OfflineAiModelInfo>;

  /// Generate a CLIP text embedding locally using ONNX Runtime.
  /// Returns the embedding vector, or an empty vector on failure.
  auto EmbedText(const std::string& text) -> std::vector<float>;

  /// Generate a CLIP image embedding locally using ONNX Runtime.
  /// The image is provided as raw RGBA8 pixels with the given width/height.
  /// Returns the embedding vector, or an empty vector on failure.
  auto EmbedImage(const std::vector<uint8_t>& rgba8_pixels,
                  uint32_t width, uint32_t height) -> std::vector<float>;

  /// Retrieve a cached embedding result. Returns empty if not cached.
  auto GetCachedTextEmbedding(const std::string& text) const -> std::vector<float>;
  auto GetCachedImageEmbedding(const std::string& image_hash) const -> std::vector<float>;

  /// Store an embedding in the offline cache (e.g., from a previous
  /// sidecar response so it's available when offline).
  void CacheTextEmbedding(const std::string& text, std::vector<float> embedding);
  void CacheImageEmbedding(const std::string& image_hash, std::vector<float> embedding);

  /// Compute a simple hash of image pixel data for cache keys.
  static auto ComputeImageHash(const std::vector<uint8_t>& rgba8_pixels) -> std::string;

  /// Generate a basic offline image analysis using heuristics and cached data.
  /// This is a degraded version of the full sidecar analysis — it provides
  /// basic tag suggestions and scene classification based on embedding
  /// similarity to previously cached analyses.
  auto OfflineImageAnalysis(const std::vector<uint8_t>& rgba8_pixels,
                            uint32_t width, uint32_t height)
      -> ImageAnalysisUnderstandingResult;

  /// Maximum number of text embeddings to cache.
  static constexpr size_t kMaxTextCacheSize = 10000;
  /// Maximum number of image embeddings to cache.
  static constexpr size_t kMaxImageCacheSize = 5000;

  auto StateName() const -> QString;
  auto StatusMessage() const -> QString { return QString::fromStdString(status_message_); }

 signals:
  void stateChanged();

 private:
  void SetState(OfflineAiServiceState state, std::string message);

  // Internal ORT session management.
  struct OrtSessionHolder;
  struct OrtEnvHolder;

  auto LoadOrtSessions(const std::filesystem::path& model_root,
                       const std::string& model_id,
                       const std::string& revision,
                       const std::string& device) -> bool;
  auto RunTextInference(const std::vector<int64_t>& input_ids,
                        const std::vector<int64_t>& attention_mask,
                        size_t batch_size, size_t seq_len) -> std::vector<float>;
  auto RunImageInference(const std::vector<float>& pixel_values,
                         size_t batch_size, size_t channels,
                         size_t height, size_t width) -> std::vector<float>;
  auto RunMultimodalTextInference(const std::vector<int64_t>& input_ids,
                                  const std::vector<int64_t>& attention_mask,
                                  size_t batch_size, size_t seq_len) -> std::vector<float>;
  auto RunMultimodalImageInference(const std::vector<float>& pixel_values,
                                   size_t batch_size, size_t channels,
                                   size_t height, size_t width) -> std::vector<float>;

  auto PrepareImageTensor(const std::vector<uint8_t>& rgba8_pixels,
                          uint32_t width, uint32_t height) -> std::vector<float>;

  // L2 normalization for embeddings.
  static auto L2Normalize(const std::vector<float>& embedding) -> std::vector<float>;

  OfflineAiServiceState                     state_ = OfflineAiServiceState::Unavailable;
  std::string                               status_message_ = "Offline AI service is not initialized";
  std::optional<OfflineAiModelInfo>         model_info_;

  // ORT session state (PIMPL to avoid exposing onnxruntime headers).
  std::unique_ptr<OrtEnvHolder>             ort_env_;
  std::unique_ptr<OrtSessionHolder>         text_session_;
  std::unique_ptr<OrtSessionHolder>         vision_session_;
  std::unique_ptr<OrtSessionHolder>         multimodal_session_;

  // Session IO metadata
  std::string                               text_input_name_;
  std::string                               text_output_name_;
  std::string                               vision_input_name_;
  std::string                               vision_output_name_;
  std::string                               multimodal_text_input_name_;
  std::string                               multimodal_image_input_name_;
  std::string                               multimodal_text_output_name_;
  std::string                               multimodal_image_output_name_;
  bool                                      uses_multimodal_session_ = false;
  bool                                      requires_attention_mask_ = true;
  uint32_t                                  text_seq_len_ = 77;
  uint32_t                                  image_size_ = 256;
  uint32_t                                  embedding_dim_ = 0;
  uint32_t                                  native_embedding_dim_ = 0;
  std::string                               embedding_transform_;
  float                                     image_mean_[3] = {0.48145466f, 0.4578275f, 0.40821073f};
  float                                     image_std_[3] = {0.26862954f, 0.26130258f, 0.27577711f};

  // Embedding cache for offline access.
  mutable std::mutex                        cache_mutex_;
  std::unordered_map<std::string, std::vector<float>> text_embedding_cache_;
  std::unordered_map<std::string, std::vector<float>> image_embedding_cache_;
};

}  // namespace alcedo

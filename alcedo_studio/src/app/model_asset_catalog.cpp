//  Copyright 2026 Yurun Zi
//  SPDX-License-Identifier: GPL-3.0-only
//  Additional permission under GPLv3 section 7 applies; see the LICENSE file.

#include "app/model_asset_catalog.hpp"

#include <QCryptographicHash>
#include <QFile>
#include <QJsonArray>
#include <QJsonDocument>
#include <QJsonObject>
#include <algorithm>
#include <system_error>

namespace alcedo {

namespace {

constexpr const char* kMobileClipRepo     = "plhery/mobileclip2-onnx";
constexpr const char* kMobileClipRevision = "ba95759a5bdbaca53e9111e2550a76ec09c8fd9e";
constexpr const char* kMobileClipProfile  = "mobileclip2-s2-en";
constexpr const char* kMobileClipModelId  = "plhery/mobileclip2-onnx:s2";

constexpr const char* kJinaClipRepo     = "jinaai/jina-clip-v2";
constexpr const char* kJinaClipRevision = "e10d47f5691d0454a0fb5d13f46f2199b74cb436";
// `kJinaClipProfile` is a stable internal key (persisted in user settings and
// used for batch-size/timeout lookups); the underlying precision is selected
// per platform below. The INT8 (quantized) export yields non-finite (NaN)
// embeddings on CoreML's GPU/ANE path, so macOS downloads the FP16 export,
// which is numerically correct on the full CoreML compute-unit range.
// Windows keeps the smaller INT8 export, which runs correctly under DirectML.
constexpr const char* kJinaClipProfile = "jina-clip-v2-int8-multilingual";

#if defined(__APPLE__)
constexpr const char* kJinaClipEngineProfileId = "jina-clip-v2-onnx-fp16";
constexpr const char* kJinaClipDisplayName     = "Jina CLIP v2 FP16 Multilingual";
constexpr const char* kJinaClipOnnxRemotePath  = "onnx/model_fp16.onnx";
constexpr const char* kJinaClipOnnxLocalPath   = "onnx/model_fp16.onnx";
constexpr uint64_t    kJinaClipOnnxSize   = 1'728'814'880ULL;
constexpr const char* kJinaClipOnnxSha256 =
    "746a78209096d1cd52891b70d752903b8bf86088ba847bd0c56c03fb29256801";
#else
constexpr const char* kJinaClipEngineProfileId = "jina-clip-v2-onnx-int8";
constexpr const char* kJinaClipDisplayName     = "Jina CLIP v2 INT8 Multilingual";
constexpr const char* kJinaClipOnnxRemotePath  = "onnx/model_int8.onnx";
constexpr const char* kJinaClipOnnxLocalPath   = "onnx/model_int8.onnx";
constexpr uint64_t    kJinaClipOnnxSize   = 874'350'932ULL;
constexpr const char* kJinaClipOnnxSha256 =
    "21b8b77a009865faecaa29f076ee55d6334ea42699a9efa14d542ce8d3938a3f";
#endif

auto BuildProfiles() -> std::vector<ModelProfileSpec> {
  std::vector<ModelProfileSpec> profiles;

  ModelProfileSpec mobileclip{};
  mobileclip.profile_id                 = kMobileClipProfile;
  mobileclip.display_name               = "MobileCLIP2 S2 English";
  mobileclip.model_id                   = kMobileClipModelId;
  mobileclip.revision                   = kMobileClipRevision;
  mobileclip.engine_profile_id          = "mobileclip2-openclip";
  mobileclip.language                   = ModelLanguage::kEn;
  mobileclip.embedding_dimension        = kSemanticRequiredEmbeddingDimension;
  mobileclip.native_embedding_dimension = kSemanticRequiredEmbeddingDimension;
  mobileclip.image_size                 = 256;
  mobileclip.embedding_transform        = "l2_normalize";
  mobileclip.assets = {
      {ModelAssetRole::kTextModel, kMobileClipRepo, kMobileClipRevision, "onnx/s2/text_model.onnx",
       "onnx/s2/text_model.onnx", 254'053'669,
       "622f10372bca71b5017f2efc5f8c2886610a2592b636de8984d717f03213f031"},
      {ModelAssetRole::kVisionModel, kMobileClipRepo, kMobileClipRevision,
       "onnx/s2/vision_model.onnx", "onnx/s2/vision_model.onnx", 143'044'797,
       "a841f72c5a5085748bbe271a1d5718aba877822a15cba865bdbd0d37036b849e"},
      {ModelAssetRole::kOnnxConfig, kMobileClipRepo, kMobileClipRevision, "onnx/s2/config.json",
       "onnx/s2/config.json", 98, nullptr},
      {ModelAssetRole::kPreprocessConfig, kMobileClipRepo, kMobileClipRevision,
       "onnx/s2/preprocessor_config.json", "onnx/s2/preprocessor_config.json", 284, nullptr},
      {ModelAssetRole::kTokenizer, kMobileClipRepo, kMobileClipRevision, "tokenizer.json",
       "tokenizer.json", 2'224'041, nullptr},
      {ModelAssetRole::kTokenizerConfig, kMobileClipRepo, kMobileClipRevision,
       "tokenizer_config.json", "tokenizer_config.json", 568, nullptr},
  };
  profiles.push_back(std::move(mobileclip));

  ModelProfileSpec jina{};
  jina.profile_id                 = kJinaClipProfile;
  jina.display_name               = kJinaClipDisplayName;
  jina.model_id                   = kJinaClipRepo;
  jina.revision                   = kJinaClipRevision;
  jina.engine_profile_id          = kJinaClipEngineProfileId;
  jina.language                   = ModelLanguage::kMultilingual;
  jina.embedding_dimension        = kSemanticRequiredEmbeddingDimension;
  jina.native_embedding_dimension = 1024;
  jina.image_size                 = 512;
  jina.embedding_transform        = "matryoshka_truncate_then_l2_normalize";
  jina.assets = {
      {ModelAssetRole::kMultimodalModel, kJinaClipRepo, kJinaClipRevision, kJinaClipOnnxRemotePath,
       kJinaClipOnnxLocalPath, kJinaClipOnnxSize, kJinaClipOnnxSha256},
      {ModelAssetRole::kModelConfig, kJinaClipRepo, kJinaClipRevision, "config.json",
       "config.json", 2'152, nullptr},
      {ModelAssetRole::kPreprocessConfig, kJinaClipRepo, kJinaClipRevision,
       "preprocessor_config.json", "preprocessor_config.json", 584, nullptr},
      {ModelAssetRole::kTokenizer, kJinaClipRepo, kJinaClipRevision, "tokenizer.json",
       "tokenizer.json", 17'082'997,
       "6601c4120779a1a3863897ba332fe3481d548e363bec2c91eba10ef8640a5e93"},
      {ModelAssetRole::kTokenizerConfig, kJinaClipRepo, kJinaClipRevision, "tokenizer_config.json",
       "tokenizer_config.json", 1'148, nullptr},
      {ModelAssetRole::kSpecialTokens, kJinaClipRepo, kJinaClipRevision, "special_tokens_map.json",
       "special_tokens_map.json", 964, nullptr},
  };
  profiles.push_back(std::move(jina));

  return profiles;
}

}  // namespace

auto ToString(ModelAssetRole role) -> const char* {
  switch (role) {
    case ModelAssetRole::kTextModel:
      return "text_model";
    case ModelAssetRole::kVisionModel:
      return "vision_model";
    case ModelAssetRole::kMultimodalModel:
      return "multimodal_model";
    case ModelAssetRole::kOnnxConfig:
      return "onnx_config";
    case ModelAssetRole::kModelConfig:
      return "model_config";
    case ModelAssetRole::kPreprocessConfig:
      return "preprocess_config";
    case ModelAssetRole::kTokenizer:
      return "tokenizer";
    case ModelAssetRole::kTokenizerConfig:
      return "tokenizer_config";
    case ModelAssetRole::kVocab:
      return "vocab";
    case ModelAssetRole::kSpecialTokens:
      return "special_tokens";
  }
  return "unknown";
}

auto ToString(ModelLanguage language) -> const char* {
  switch (language) {
    case ModelLanguage::kEn:
      return "en";
    case ModelLanguage::kZh:
      return "zh";
    case ModelLanguage::kMultilingual:
      return "multilingual";
  }
  return "multilingual";
}

auto SemanticModelProfiles() -> const std::vector<ModelProfileSpec>& {
  static const std::vector<ModelProfileSpec> kProfiles = BuildProfiles();
  return kProfiles;
}

auto FindSemanticProfile(const std::string& profile_or_model_id) -> const ModelProfileSpec* {
  for (const auto& profile : SemanticModelProfiles()) {
    if (profile_or_model_id == profile.profile_id || profile_or_model_id == profile.model_id) {
      return &profile;
    }
  }
  return nullptr;
}

auto ProfileTotalBytes(const ModelProfileSpec& profile) -> uint64_t {
  uint64_t total = 0;
  for (const auto& asset : profile.assets) {
    total += asset.size_bytes;
  }
  return total;
}

auto StagingRoot(const std::filesystem::path& root) -> std::filesystem::path {
  const auto file_name =
      root.has_filename() ? root.filename().string() : std::string{"model"};
  return root.parent_path() / ("." + file_name + ".download");
}

auto BuildAssetUrl(const std::string& hf_endpoint, const ModelAssetSpec& asset) -> std::string {
  std::string endpoint = hf_endpoint;
  while (!endpoint.empty() && endpoint.back() == '/') {
    endpoint.pop_back();
  }
  return endpoint + "/" + asset.repo_id + "/resolve/" + asset.revision + "/" + asset.remote_path;
}

auto Sha256File(const std::filesystem::path& path) -> std::string {
  QFile file(QString::fromStdString(path.string()));
  if (!file.open(QIODevice::ReadOnly)) {
    return {};
  }
  QCryptographicHash hasher(QCryptographicHash::Sha256);
  constexpr qint64 kChunkSize = 1 * 1024 * 1024;
  while (!file.atEnd()) {
    const QByteArray chunk = file.read(kChunkSize);
    if (chunk.isEmpty()) {
      break;
    }
    hasher.addData(chunk);
  }
  return QString::fromUtf8(hasher.result().toHex()).toStdString();
}

auto ValidateAssetFile(const ModelAssetSpec& asset, const std::filesystem::path& local_path)
    -> std::optional<std::string> {
  std::error_code ec;
  if (!std::filesystem::exists(local_path, ec)) {
    return "missing file: " + local_path.string();
  }
  const auto size = std::filesystem::file_size(local_path, ec);
  if (ec) {
    return "failed to stat " + local_path.string() + ": " + ec.message();
  }
  if (size != asset.size_bytes) {
    return std::string{asset.local_path} + " size mismatch: expected " +
           std::to_string(asset.size_bytes) + " bytes, got " + std::to_string(size) + " bytes";
  }
  if (asset.sha256 != nullptr && asset.sha256[0] != '\0') {
    const auto actual = Sha256File(local_path);
    if (actual.empty()) {
      return "failed to compute sha256 for " + local_path.string();
    }
    std::string expected = asset.sha256;
    std::transform(expected.begin(), expected.end(), expected.begin(),
                   [](unsigned char c) { return std::tolower(c); });
    if (actual != expected) {
      return std::string{asset.local_path} + " sha256 mismatch: expected " + expected +
             ", got " + actual;
    }
  }
  return std::nullopt;
}

auto WriteResolvedManifest(const ModelProfileSpec& profile, const std::filesystem::path& root)
    -> std::optional<std::string> {
  std::error_code ec;
  std::filesystem::create_directories(root, ec);
  if (ec) {
    return "failed to create model root " + root.string() + ": " + ec.message();
  }

  QJsonObject object;
  object.insert("profile_id", QString::fromLatin1(profile.profile_id));
  object.insert("model_id", QString::fromLatin1(profile.model_id));
  object.insert("revision", QString::fromLatin1(profile.revision));
  object.insert("engine_profile_id", QString::fromLatin1(profile.engine_profile_id));
  object.insert("language", QString::fromLatin1(ToString(profile.language)));
  object.insert("embedding_dimension",
                static_cast<int>(profile.embedding_dimension));
  object.insert("native_embedding_dimension",
                static_cast<int>(profile.native_embedding_dimension));
  object.insert("image_size", static_cast<int>(profile.image_size));
  object.insert("embedding_transform", QString::fromLatin1(profile.embedding_transform));
  object.insert("model_root", QString::fromStdString(root.string()));

  QJsonArray assets_array;
  for (const auto& asset : profile.assets) {
    QJsonObject asset_object;
    asset_object.insert("role", QString::fromLatin1(ToString(asset.role)));
    asset_object.insert("repo_id", QString::fromLatin1(asset.repo_id));
    asset_object.insert("revision", QString::fromLatin1(asset.revision));
    asset_object.insert("remote_path", QString::fromLatin1(asset.remote_path));
    const auto local_fs = root / asset.local_path;
    asset_object.insert("local_path", QString::fromStdString(local_fs.string()));
    asset_object.insert("size_bytes", static_cast<qint64>(asset.size_bytes));
    asset_object.insert("sha256",
                        asset.sha256 ? QString::fromLatin1(asset.sha256) : QString{});
    assets_array.append(asset_object);
  }
  object.insert("assets", assets_array);

  const auto manifest_path = root / kSemanticResolvedManifestFile;
  QFile file(QString::fromStdString(manifest_path.string()));
  if (!file.open(QIODevice::WriteOnly | QIODevice::Truncate)) {
    return "failed to write " + manifest_path.string();
  }
  file.write(QJsonDocument(object).toJson(QJsonDocument::Indented));
  return std::nullopt;
}

auto PromoteStagingRoot(const std::filesystem::path& staging, const std::filesystem::path& root)
    -> std::optional<std::string> {
  std::error_code ec;
  if (auto parent = root.parent_path(); !parent.empty()) {
    std::filesystem::create_directories(parent, ec);
    if (ec) {
      return "failed to create model root parent " + parent.string() + ": " + ec.message();
    }
  }
  if (std::filesystem::exists(root, ec)) {
    std::filesystem::remove_all(root, ec);
    if (ec) {
      return "failed to replace old model root " + root.string() + ": " + ec.message();
    }
  }
  std::filesystem::rename(staging, root, ec);
  if (ec) {
    return "failed to promote staged model profile " + staging.string() + " to " + root.string() +
           ": " + ec.message();
  }
  return std::nullopt;
}

}  // namespace alcedo

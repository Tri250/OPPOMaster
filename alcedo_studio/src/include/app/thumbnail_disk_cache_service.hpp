//  Copyright 2026 Yurun Zi
//  SPDX-License-Identifier: GPL-3.0-only
//  Additional permission under GPLv3 section 7 applies; see the LICENSE file.

#pragma once

#include <cstdint>
#include <filesystem>
#include <functional>
#include <memory>
#include <mutex>
#include <string>
#include <unordered_map>
#include <vector>

#include "app/thumbnail_service.hpp"
#include "image/image_buffer.hpp"
#include "type/hash_type.hpp"
#include "type/type.hpp"

namespace alcedo {

enum class ThumbnailCacheFormat : uint8_t {
  kBmp = 0,
  kJpeg = 1,
};

struct ThumbnailDiskCacheKey {
  std::string         project_uuid;
  sl_element_id_t     element_id          = 0;
  ThumbnailResolution resolution          = ThumbnailResolution::k1024;
  std::string         edit_version_hash;
  uint32_t            cache_schema_version = 1;

  bool                operator==(const ThumbnailDiskCacheKey& other) const = default;
};

}  // namespace alcedo

template <>
struct std::hash<alcedo::ThumbnailDiskCacheKey> {
  size_t operator()(const alcedo::ThumbnailDiskCacheKey& key) const noexcept {
    size_t h = std::hash<std::string>{}(key.project_uuid);
    h ^= std::hash<uint32_t>{}(key.element_id) + 0x9e3779b9 + (h << 6) + (h >> 2);
    h ^= std::hash<uint32_t>{}(static_cast<uint32_t>(key.resolution)) + 0x9e3779b9 + (h << 6) +
         (h >> 2);
    h ^= std::hash<std::string>{}(key.edit_version_hash) + 0x9e3779b9 + (h << 6) + (h >> 2);
    h ^= std::hash<uint32_t>{}(key.cache_schema_version) + 0x9e3779b9 + (h << 6) + (h >> 2);
    return h;
  }
};

namespace alcedo {

class ThumbnailDiskCacheService {
 public:
  struct Stats {
    size_t total_entries   = 0;
    size_t total_size_bytes = 0;
    size_t hit_count        = 0;
    size_t miss_count       = 0;
  };

  ThumbnailDiskCacheService();
  explicit ThumbnailDiskCacheService(const std::filesystem::path& cache_root);
  ~ThumbnailDiskCacheService();

  ThumbnailDiskCacheService(const ThumbnailDiskCacheService&)            = delete;
  ThumbnailDiskCacheService& operator=(const ThumbnailDiskCacheService&) = delete;
  ThumbnailDiskCacheService(ThumbnailDiskCacheService&&)                 = delete;
  ThumbnailDiskCacheService& operator=(ThumbnailDiskCacheService&&)      = delete;

  void Initialize(const std::string& project_uuid);
  void Shutdown();

  bool                        Lookup(const ThumbnailDiskCacheKey& key) const;
  std::unique_ptr<ImageBuffer> Read(const ThumbnailDiskCacheKey& key);
  void                        EnqueueWrite(const ThumbnailDiskCacheKey& key,
                                           ImageBuffer&                 buffer,
                                           ThumbnailCacheFormat         format = ThumbnailCacheFormat::kBmp);
  void                        Invalidate(const std::string& project_uuid, sl_element_id_t element_id);

  Stats                       GetStats() const;

 private:
  static constexpr uint32_t kCacheSchemaVersion = 1;
  static constexpr int      kJpegQuality        = 85;

  struct EntryMeta {
    ThumbnailDiskCacheKey  key;
    size_t                 file_size_bytes = 0;
    std::filesystem::path  file_path;
  };

  struct WriteTask {
    ThumbnailDiskCacheKey key;
    std::vector<uint8_t>  encoded_data;
    std::string           key_hash;
    std::string           file_extension;
  };

  struct State;
  std::unique_ptr<State> state_;

  static std::string MakeKeyHashString(const ThumbnailDiskCacheKey& key);
  std::filesystem::path DeriveFilePath(const std::string& key_hash,
                                        ThumbnailCacheFormat format) const;
  void                  WriterThreadLoop();
  void                  FlushMetadata();
  void                  LoadMetadata();
  void                  RemoveEntryFromIndexLocked(const std::string& key_hash);
};

}  // namespace alcedo

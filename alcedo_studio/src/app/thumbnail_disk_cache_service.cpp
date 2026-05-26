//  Copyright 2026 Yurun Zi
//  SPDX-License-Identifier: GPL-3.0-only
//  Additional permission under GPLv3 section 7 applies; see the LICENSE file.

#include "app/thumbnail_disk_cache_service.hpp"

#include <atomic>
#include <cstdint>
#include <filesystem>
#include <fstream>
#include <mutex>
#include <opencv2/opencv.hpp>
#include <sstream>
#include <string>
#include <thread>
#include <unordered_map>
#include <vector>

#include "json.hpp"
#include "type/hash_type.hpp"
#include "utils/queue/queue.hpp"

namespace alcedo {

namespace {
std::filesystem::path GetDefaultCacheRoot() {
#if defined(_WIN32)
  const char* local_app_data = std::getenv("LOCALAPPDATA");
  if (local_app_data) {
    return std::filesystem::path(local_app_data) / "alcedo" / "thumbnails";
  }
  return std::filesystem::temp_directory_path() / "alcedo_thumbnails";
#elif defined(__APPLE__)
  const char* home = std::getenv("HOME");
  if (home) {
    return std::filesystem::path(home) / "Library" / "Caches" / "alcedo" / "thumbnails";
  }
  return std::filesystem::temp_directory_path() / "alcedo_thumbnails";
#else
  const char* xdg_cache = std::getenv("XDG_CACHE_HOME");
  if (xdg_cache) {
    return std::filesystem::path(xdg_cache) / "alcedo" / "thumbnails";
  }
  const char* home = std::getenv("HOME");
  if (home) {
    return std::filesystem::path(home) / ".cache" / "alcedo" / "thumbnails";
  }
  return std::filesystem::temp_directory_path() / "alcedo_thumbnails";
#endif
}

std::string FormatSizeBytes(uint64_t low, uint64_t high) {
  std::ostringstream oss;
  oss << std::hex << std::setfill('0');
  oss << std::setw(16) << high;
  oss << std::setw(16) << low;
  return oss.str();
}

const char* FormatFileExtension(ThumbnailCacheFormat format) {
  switch (format) {
    case ThumbnailCacheFormat::kBmp:
      return ".bmp";
    case ThumbnailCacheFormat::kJpeg:
      return ".jpg";
  }
  return ".bmp";
}
}  // namespace

struct ThumbnailDiskCacheService::State {
  std::filesystem::path                           cache_root_;
  std::string                                     project_uuid_;
  std::filesystem::path                           project_cache_dir_;
  std::filesystem::path                           metadata_file_path_;

  std::unordered_map<std::string, EntryMeta>      index_;
  size_t                                           total_size_bytes_ = 0;

  ConcurrentBlockingQueue<WriteTask>               write_queue_;
  std::thread                                      writer_thread_;
  std::atomic<bool>                                writer_running_{false};

  mutable std::mutex                               metadata_mutex_;
  mutable std::atomic<size_t>                      hit_count_{0};
  mutable std::atomic<size_t>                      miss_count_{0};

  bool                                             initialized_ = false;
};

std::string ThumbnailDiskCacheService::MakeKeyHashString(const ThumbnailDiskCacheKey& key) {
  std::ostringstream oss;
  oss << key.project_uuid << '|' << key.element_id << '|'
      << static_cast<uint32_t>(key.resolution) << '|' << key.edit_version_hash << '|'
      << key.cache_schema_version;
  const auto str    = oss.str();
  const auto hash   = Hash128::Compute(str.data(), str.size());
  return FormatSizeBytes(hash.low64(), hash.high64());
}

std::filesystem::path ThumbnailDiskCacheService::DeriveFilePath(
    const std::string& key_hash, ThumbnailCacheFormat format) const {
  const auto& dir = state_->project_cache_dir_;
  return dir / key_hash.substr(0, 2) / key_hash.substr(2, 2) /
         (key_hash + FormatFileExtension(format));
}

ThumbnailDiskCacheService::ThumbnailDiskCacheService()
    : ThumbnailDiskCacheService(GetDefaultCacheRoot()) {}

ThumbnailDiskCacheService::ThumbnailDiskCacheService(const std::filesystem::path& cache_root)
    : state_(std::make_unique<State>()) {
  state_->cache_root_ = cache_root;
}

ThumbnailDiskCacheService::~ThumbnailDiskCacheService() {
  Shutdown();
}

void ThumbnailDiskCacheService::Initialize(const std::string& project_uuid) {
  if (state_->initialized_) {
    return;
  }

  state_->project_uuid_     = project_uuid;
  state_->project_cache_dir_ = state_->cache_root_ / project_uuid;
  state_->metadata_file_path_ = state_->project_cache_dir_ / "cache_metadata.json";

  {
    std::unique_lock lock(state_->metadata_mutex_);
    state_->index_.clear();
    state_->total_size_bytes_ = 0;
  }

  std::error_code ec;
  std::filesystem::create_directories(state_->project_cache_dir_, ec);

  LoadMetadata();

  state_->writer_running_ = true;
  state_->writer_thread_   = std::thread(&ThumbnailDiskCacheService::WriterThreadLoop, this);

  state_->initialized_ = true;
}

void ThumbnailDiskCacheService::Shutdown() {
  if (!state_->initialized_) {
    return;
  }

  state_->writer_running_ = false;
  state_->write_queue_.push(WriteTask{});

  if (state_->writer_thread_.joinable()) {
    state_->writer_thread_.join();
  }

  FlushMetadata();
  state_->initialized_ = false;
}

bool ThumbnailDiskCacheService::Lookup(const ThumbnailDiskCacheKey& key) const {
  if (!state_->initialized_) {
    return false;
  }

  const auto key_hash = MakeKeyHashString(key);

  std::unique_lock lock(state_->metadata_mutex_);
  auto             it = state_->index_.find(key_hash);
  if (it != state_->index_.end()) {
    state_->hit_count_++;
    return true;
  }
  state_->miss_count_++;
  return false;
}

std::unique_ptr<ImageBuffer> ThumbnailDiskCacheService::Read(const ThumbnailDiskCacheKey& key) {
  if (!state_->initialized_) {
    return nullptr;
  }

  const auto        key_hash = MakeKeyHashString(key);
  std::filesystem::path file_path;

  {
    std::unique_lock lock(state_->metadata_mutex_);
    auto             it = state_->index_.find(key_hash);
    if (it == state_->index_.end()) {
      state_->miss_count_++;
      return nullptr;
    }
    state_->hit_count_++;
    file_path = it->second.file_path;
  }

  std::error_code ec;
  if (!std::filesystem::exists(file_path, ec)) {
    std::unique_lock lock(state_->metadata_mutex_);
    RemoveEntryFromIndexLocked(key_hash);
    return nullptr;
  }

  auto file_size = std::filesystem::file_size(file_path, ec);
  if (ec || file_size == 0) {
    std::unique_lock lock(state_->metadata_mutex_);
    RemoveEntryFromIndexLocked(key_hash);
    return nullptr;
  }

  std::vector<uint8_t> file_data(file_size);
  {
    std::ifstream file(file_path, std::ios::binary);
    if (!file) {
      std::unique_lock lock(state_->metadata_mutex_);
      RemoveEntryFromIndexLocked(key_hash);
      return nullptr;
    }
    file.read(reinterpret_cast<char*>(file_data.data()), static_cast<std::streamsize>(file_size));
    if (file.fail()) {
      std::unique_lock lock(state_->metadata_mutex_);
      RemoveEntryFromIndexLocked(key_hash);
      return nullptr;
    }
  }

  cv::Mat decoded = cv::imdecode(file_data, cv::IMREAD_COLOR);
  if (!decoded.empty()) {
    return std::make_unique<ImageBuffer>(std::move(decoded));
  }

  std::unique_lock lock(state_->metadata_mutex_);
  RemoveEntryFromIndexLocked(key_hash);
  return nullptr;
}

void ThumbnailDiskCacheService::EnqueueWrite(const ThumbnailDiskCacheKey& key,
                                              ImageBuffer&                 buffer,
                                              ThumbnailCacheFormat         format) {
  if (!state_->initialized_) {
    return;
  }

  if (!buffer.cpu_data_valid_) {
    return;
  }

  const auto& mat = buffer.GetCPUData();
  if (mat.empty()) {
    return;
  }

  std::vector<uint8_t> encoded;
  std::vector<int>     params;
  if (format == ThumbnailCacheFormat::kJpeg) {
    params = {cv::IMWRITE_JPEG_QUALITY, kJpegQuality};
  }
  if (!cv::imencode(FormatFileExtension(format), mat, encoded, params)) {
    return;
  }

  const auto key_hash  = MakeKeyHashString(key);
  const auto file_path = DeriveFilePath(key_hash, format);
  const auto extension = std::string(FormatFileExtension(format));

  {
    std::unique_lock lock(state_->metadata_mutex_);
    auto             it = state_->index_.find(key_hash);
    if (it != state_->index_.end()) {
      state_->total_size_bytes_ -= it->second.file_size_bytes;
    }

    EntryMeta meta;
    meta.key            = key;
    meta.file_size_bytes = encoded.size();
    meta.file_path       = file_path;
    state_->index_[key_hash] = meta;
    state_->total_size_bytes_ += encoded.size();
  }

  WriteTask task;
  task.key           = key;
  task.encoded_data  = std::move(encoded);
  task.key_hash      = key_hash;
  task.file_extension = extension;
  state_->write_queue_.push(std::move(task));
}

void ThumbnailDiskCacheService::Invalidate(const std::string&     project_uuid,
                                            sl_element_id_t        element_id) {
  if (!state_->initialized_) {
    return;
  }

  std::unique_lock lock(state_->metadata_mutex_);

  std::vector<std::string> keys_to_remove;
  for (const auto& [hash_str, meta] : state_->index_) {
    if (meta.key.project_uuid == project_uuid && meta.key.element_id == element_id) {
      keys_to_remove.push_back(hash_str);
    }
  }

  for (const auto& hash_str : keys_to_remove) {
    RemoveEntryFromIndexLocked(hash_str);
  }
}

auto ThumbnailDiskCacheService::GetStats() const -> Stats {
  Stats s;
  if (!state_->initialized_) {
    return s;
  }

  std::unique_lock lock(state_->metadata_mutex_);
  s.total_entries   = state_->index_.size();
  s.total_size_bytes = state_->total_size_bytes_;
  s.hit_count        = state_->hit_count_.load();
  s.miss_count       = state_->miss_count_.load();
  return s;
}

void ThumbnailDiskCacheService::WriterThreadLoop() {
  while (state_->writer_running_) {
    auto task = state_->write_queue_.pop();
    if (!state_->writer_running_) {
      break;
    }

    if (task.encoded_data.empty()) {
      continue;
    }

    // Reconstruct file path using the stored extension
    const auto& dir = state_->project_cache_dir_;
    const auto file_path =
        dir / task.key_hash.substr(0, 2) / task.key_hash.substr(2, 2) /
        (task.key_hash + task.file_extension);

    std::error_code ec;
    std::filesystem::create_directories(file_path.parent_path(), ec);
    if (ec) {
      continue;
    }

    {
      std::ofstream file(file_path, std::ios::binary | std::ios::trunc);
      if (!file) {
        continue;
      }
      file.write(reinterpret_cast<const char*>(task.encoded_data.data()),
                 static_cast<std::streamsize>(task.encoded_data.size()));
    }

    FlushMetadata();
  }
}

void ThumbnailDiskCacheService::FlushMetadata() {
  nlohmann::json j;
  j["cache_schema_version"] = kCacheSchemaVersion;
  j["project_uuid"]         = state_->project_uuid_;
  j["total_size_bytes"]     = state_->total_size_bytes_;

  auto& entries_json = j["entries"];
  entries_json       = nlohmann::json::array();

  {
    std::unique_lock lock(state_->metadata_mutex_);
    for (const auto& [hash_str, meta] : state_->index_) {
      nlohmann::json entry;
      entry["key_hash"]            = hash_str;
      entry["project_uuid"]        = meta.key.project_uuid;
      entry["element_id"]          = meta.key.element_id;
      entry["resolution"]          = static_cast<uint32_t>(meta.key.resolution);
      entry["edit_version_hash"]   = meta.key.edit_version_hash;
      entry["cache_schema_version"] = meta.key.cache_schema_version;
      entry["file_size_bytes"]     = meta.file_size_bytes;
      entry["file_path"]           = meta.file_path.string();
      entries_json.push_back(std::move(entry));
    }
  }

  std::error_code ec;
  std::filesystem::create_directories(state_->metadata_file_path_.parent_path(), ec);
  if (ec) {
    return;
  }

  const auto tmp_path = state_->metadata_file_path_.string() + ".tmp";
  {
    std::ofstream file(tmp_path, std::ios::trunc);
    if (!file) {
      return;
    }
    file << j.dump(2);
  }

  std::filesystem::rename(tmp_path, state_->metadata_file_path_, ec);
}

void ThumbnailDiskCacheService::LoadMetadata() {
  std::error_code ec;
  if (!std::filesystem::exists(state_->metadata_file_path_, ec)) {
    return;
  }

  std::ifstream file(state_->metadata_file_path_);
  if (!file) {
    return;
  }

  nlohmann::json j;
  try {
    file >> j;
  } catch (...) {
    return;
  }

  if (!j.contains("entries") || !j["entries"].is_array()) {
    return;
  }

  std::unique_lock lock(state_->metadata_mutex_);

  for (const auto& entry : j["entries"]) {
    try {
      EntryMeta meta;

      const auto file_path_str = entry.value("file_path", std::string{});
      if (file_path_str.empty()) {
        continue;
      }
      meta.file_path       = file_path_str;
      meta.file_size_bytes = entry.value("file_size_bytes", size_t{0});
      meta.key.project_uuid  = entry.value("project_uuid", std::string{});
      meta.key.element_id   = entry.value("element_id", sl_element_id_t{0});
      meta.key.resolution   = static_cast<ThumbnailResolution>(
          entry.value("resolution", static_cast<uint32_t>(ThumbnailResolution::k1024)));
      meta.key.edit_version_hash  = entry.value("edit_version_hash", std::string{});
      meta.key.cache_schema_version = entry.value("cache_schema_version", uint32_t{0});

      const auto key_hash = entry.value("key_hash", std::string{});
      if (key_hash.empty()) {
        continue;
      }

      if (std::filesystem::exists(meta.file_path, ec)) {
        state_->index_[key_hash]  = meta;
        state_->total_size_bytes_ += meta.file_size_bytes;
      }
    } catch (...) {
      continue;
    }
  }
}

void ThumbnailDiskCacheService::RemoveEntryFromIndexLocked(const std::string& key_hash) {
  auto it = state_->index_.find(key_hash);
  if (it == state_->index_.end()) {
    return;
  }

  state_->total_size_bytes_ -= it->second.file_size_bytes;

  std::error_code ec;
  std::filesystem::remove(it->second.file_path, ec);

  state_->index_.erase(it);
}

}  // namespace alcedo

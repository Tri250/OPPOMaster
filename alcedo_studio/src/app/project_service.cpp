//  Copyright 2026 Yurun Zi
//  SPDX-License-Identifier: GPL-3.0-only
//  Additional permission under GPLv3 section 7 applies; see the LICENSE file.

#include "app/project_service.hpp"

#include <array>
#include <fstream>
#include <iostream>
#include <optional>
#include <random>
#include <stdexcept>

#include <json.hpp>

#include "app/project_package_backend.hpp"
#include "app/project_package_service.hpp"
#include "utils/string/convert.hpp"
#include "uuid.h"

namespace alcedo {
namespace {

auto ParseSemVer(std::string_view version, std::array<int, 3>* out) -> bool {
  std::array<int, 3> parts{};
  size_t             begin = 0;
  for (size_t index = 0; index < parts.size(); ++index) {
    const size_t end = version.find('.', begin);
    const auto   token =
        version.substr(begin, end == std::string_view::npos ? version.size() - begin : end - begin);
    if (token.empty()) {
      return false;
    }
    int value = 0;
    for (const char ch : token) {
      if (ch < '0' || ch > '9') {
        return false;
      }
      value = value * 10 + (ch - '0');
    }
    parts[index] = value;
    if (index + 1 < parts.size()) {
      if (end == std::string_view::npos) {
        return false;
      }
      begin = end + 1;
    } else if (end != std::string_view::npos) {
      return false;
    }
  }
  *out = parts;
  return true;
}

auto IsSupportedProjectVersion(std::string_view version) -> bool {
  std::array<int, 3> parsed{};
  std::array<int, 3> min_supported{};
  std::array<int, 3> max_supported{};
  return ParseSemVer(version, &parsed) &&
         ParseSemVer(project_pack::kMinSupportedProjectFileVersion, &min_supported) &&
         ParseSemVer(project_pack::kMaxSupportedProjectFileVersion, &max_supported) &&
         parsed >= min_supported && parsed <= max_supported;
}

auto GenerateProjectUUID() -> std::string {
  std::random_device random_device;
  std::seed_seq      seed{random_device(), random_device(), random_device(), random_device()};
  std::mt19937       generator(seed);
  uuids::uuid_random_generator uuid_gen(generator);
  return uuids::to_string(uuid_gen());
}

// Collects lightweight diagnostic summary of the project database:
// per-table row counts and min/max primary key ranges. This is NOT
// a strong integrity check — mismatches produce a warning, not a
// load failure. Use data_fingerprint (L3) for semantic verification.
auto ComputeProjectDataSummary(StorageService& storage_service) -> nlohmann::json {
  auto guard = storage_service.GetDBController().GetConnectionGuard();

  auto query_int64 = [&](const std::string& sql) -> std::optional<int64_t> {
    duckdb_result result;
    if (duckdb_query(guard.conn_, sql.c_str(), &result) != DuckDBSuccess) {
      duckdb_destroy_result(&result);
      return std::nullopt;
    }
    std::optional<int64_t> value;
    if (duckdb_row_count(&result) > 0 && duckdb_column_count(&result) > 0) {
      if (!duckdb_value_is_null(&result, 0, 0)) {
        value = duckdb_value_int64(&result, 0, 0);
      }
    }
    duckdb_destroy_result(&result);
    return value;
  };

  struct TableInfo {
    const char* name;
    const char* pk_column;  // nullptr if no meaningful single numeric PK
  };
  static constexpr TableInfo kTables[] = {
      {"Sleeve", "id"},         {"Image", "id"},        {"SleeveRoot", "id"},
      {"Element", "id"},        {"FolderContent", nullptr}, {"FileImage", "file_id"},
      {"ComboFolder", "combo_id"}, {"Filter", "combo_id"},  {"EditHistory", "file_id"},
      {"Version", "hash"},      {"PipelineParam", "file_id"},
  };

  nlohmann::json summary;
  summary["version"] = 1;
  nlohmann::json tables = nlohmann::json::object();

  for (const auto& table : kTables) {
    nlohmann::json entry;

    auto count = query_int64(
        std::string("SELECT COUNT(*) FROM \"") + table.name + "\"");
    if (!count.has_value()) continue;
    entry["rows"] = *count;

    if (table.pk_column != nullptr) {
      const std::string pk_str(table.pk_column);
      auto min_val = query_int64(
          std::string("SELECT MIN(\"") + pk_str + "\") FROM \"" + table.name + "\"");
      auto max_val = query_int64(
          std::string("SELECT MAX(\"") + pk_str + "\") FROM \"" + table.name + "\"");
      if (min_val.has_value() && max_val.has_value()) {
        entry["min_id"] = *min_val;
        entry["max_id"] = *max_val;
      }
    }

    tables[table.name] = entry;
  }

  summary["tables"] = tables;
  return summary;
}

}  // namespace

ProjectService::ProjectService(const std::filesystem::path& db_path,
                               const std::filesystem::path& meta_path,
                               ProjectOpenMode              open_mode)
    : db_path_(db_path), meta_path_(meta_path) {
  const auto create_new_project = [this]() {
    storage_service_ = std::make_shared<StorageService>(db_path_);
    RecreateSleeveService(0);
    pool_service_ = std::make_shared<ImagePoolService>(storage_service_, 0);
    filter_service_  = std::make_shared<SleeveFilterService>(storage_service_);
    browse_service_  = std::make_shared<AlbumBrowseService>(sleeve_service_, filter_service_);
    package_service_ = std::make_shared<ProjectPackageService>();
    semantic_runtime_service_ = std::make_shared<SemanticRuntimeService>();

    project_uuid_ = GenerateProjectUUID();
  };

  switch (open_mode) {
    case ProjectOpenMode::kLoadExisting:
      LoadProject(meta_path);
      return;
    case ProjectOpenMode::kCreateNew:
      create_new_project();
      return;
    case ProjectOpenMode::kLoadOrCreate:
      break;
  }

  std::error_code ec;
  const bool meta_exists = std::filesystem::exists(meta_path, ec);
  if (ec) {
    throw std::runtime_error("Failed to inspect project metadata path");
  }
  if (meta_exists) {
    LoadProject(meta_path);
    return;
  }
  create_new_project();
}

ProjectService::~ProjectService() {
  package_service_.reset();
  if (semantic_runtime_service_) {
    semantic_runtime_service_->StopForProjectClose();
  }
  semantic_runtime_service_.reset();
  browse_service_.reset();
  filter_service_.reset();
  pool_service_.reset();
  sleeve_service_.reset();
  storage_service_.reset();
}

void ProjectService::SaveProject(const std::filesystem::path& meta_path) {
  if (!sleeve_service_) {
    throw std::runtime_error("SleeveService is not initialized");
  }

  meta_path_ = meta_path;

  nlohmann::json metadata;
  metadata["db_path"]             = conv::ToBytes(db_path_.wstring());
  metadata["meta_path"]           = conv::ToBytes(meta_path_.wstring());
  metadata["project_uuid"]        = project_uuid_;
  metadata["project_file_version"] = std::string(project_pack::kProjectFileVersion);
  metadata["project_file_min_supported_version"] =
      std::string(project_pack::kMinSupportedProjectFileVersion);
  metadata["project_file_max_supported_version"] =
      std::string(project_pack::kMaxSupportedProjectFileVersion);
  metadata["start_id"]            = sleeve_service_->GetCurrentID();
  metadata["image_pool_start_id"] = pool_service_->GetCurrentID();
  metadata["data_summary"]        = ComputeProjectDataSummary(*storage_service_);

  std::ofstream file(meta_path_);
  if (!file.is_open()) {
    throw std::runtime_error("Failed to open meta file for writing");
  }
  file << metadata.dump(4);
  file.close();
}

void ProjectService::LoadProject(const std::filesystem::path& meta_path) {
  std::ifstream file(meta_path);
  if (!file.is_open()) {
    throw std::runtime_error("Failed to open meta file for reading");
  }

  nlohmann::json metadata;
  file >> metadata;

  if (!metadata.contains("project_file_version") ||
      !metadata.at("project_file_version").is_string()) {
    throw std::runtime_error("Project metadata version is missing");
  }
  if (!project_pack::ProjectVersionIsSupported(
          metadata.at("project_file_version").get<std::string>())) {
    throw std::runtime_error("Project metadata version is not supported");
  }

  if (metadata.contains("project_uuid") && metadata.at("project_uuid").is_string()) {
    project_uuid_ = metadata.at("project_uuid").get<std::string>();
  } else {
    project_uuid_ = GenerateProjectUUID();
  }

  if (!metadata.contains("db_path")) {
    throw std::runtime_error("Project metadata missing db_path");
  }

  db_path_   = std::filesystem::path(conv::FromBytes(metadata.at("db_path")));
  meta_path_ = meta_path;
  if (metadata.contains("meta_path")) {
    const auto stored_meta_path = std::filesystem::path(conv::FromBytes(metadata.at("meta_path")));
    if (!stored_meta_path.empty()) {
      meta_path_ = stored_meta_path;
    }
  }

  if (db_path_.empty()) {
    throw std::runtime_error("Project metadata db_path is empty");
  }
  if (!std::filesystem::exists(db_path_)) {
    throw std::runtime_error("Project database file does not exist");
  }

  // Parse data_summary for diagnostic purposes — mismatch is a
  // warning, not a load failure. Old projects carry db_checksum_xxh3_64
  // instead; that field is silently ignored here.
  std::optional<nlohmann::json> expected_summary;
  if (metadata.contains("data_summary") && metadata.at("data_summary").is_object()) {
    expected_summary = metadata.at("data_summary");
  }

  sl_element_id_t start_id = 0;
  if (metadata.contains("start_id")) {
    start_id = static_cast<sl_element_id_t>(metadata.at("start_id"));
  }

  sl_element_id_t image_pool_start_id =
      metadata.contains("image_pool_start_id")
          ? static_cast<sl_element_id_t>(metadata.at("image_pool_start_id"))
          : 0;

  storage_service_ = std::make_shared<StorageService>(db_path_);

  if (expected_summary.has_value()) {
    try {
      nlohmann::json actual_summary = ComputeProjectDataSummary(*storage_service_);
      if (actual_summary != *expected_summary) {
        std::cerr << "[Alcedo] Project data summary differs from saved metadata. "
                     "This may indicate data changes since the project was saved.\n";
      }
    } catch (const std::exception& e) {
      std::cerr << "[Alcedo] Unable to compute project data summary for comparison: "
                << e.what() << "\n";
    } catch (...) {
      std::cerr << "[Alcedo] Unable to compute project data summary for comparison.\n";
    }
  }

  RecreateSleeveService(start_id);
  pool_service_ = std::make_shared<ImagePoolService>(storage_service_, image_pool_start_id);
  filter_service_  = std::make_shared<SleeveFilterService>(storage_service_);
  browse_service_  = std::make_shared<AlbumBrowseService>(sleeve_service_, filter_service_);
  package_service_ = std::make_shared<ProjectPackageService>();
  semantic_runtime_service_ = std::make_shared<SemanticRuntimeService>();
}

void ProjectService::RecreateSleeveService(sl_element_id_t start_id) {
  if (!storage_service_) {
    throw std::runtime_error("StorageService is not initialized");
  }
  sleeve_service_ = std::make_shared<SleeveServiceImpl>(storage_service_, db_path_, start_id);
}
};  // namespace alcedo

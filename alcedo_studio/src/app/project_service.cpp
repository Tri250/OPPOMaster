//  Copyright 2026 Yurun Zi
//  SPDX-License-Identifier: GPL-3.0-only
//  Additional permission under GPLv3 section 7 applies; see the LICENSE file.

#include "app/project_service.hpp"

#include <fstream>
#include <stdexcept>

#include <json.hpp>

#include "app/project_package_backend.hpp"
#include "app/project_package_service.hpp"
#include "utils/string/convert.hpp"

namespace alcedo {

ProjectService::ProjectService(const std::filesystem::path& db_path,
                               const std::filesystem::path& meta_path,
                               ProjectOpenMode              open_mode)
    : db_path_(db_path), meta_path_(meta_path) {
  const auto create_new_project = [this]() {
    storage_service_ = std::make_shared<StorageService>(db_path_);
    RecreateSleeveService(0);
    pool_service_ = std::make_shared<ImagePoolService>(storage_service_, 0);
    filter_service_  = std::make_shared<SleeveFilterService>(storage_service_);
    browse_service_  = std::make_shared<AlbumBrowseService>(sleeve_service_);
    package_service_ = std::make_shared<ProjectPackageService>();
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
  metadata["project_file_version"] = std::string(project_pack::kProjectFileVersion);
  metadata["project_file_min_supported_version"] =
      std::string(project_pack::kMinSupportedProjectFileVersion);
  metadata["project_file_max_supported_version"] =
      std::string(project_pack::kMaxSupportedProjectFileVersion);
  metadata["start_id"]            = sleeve_service_->GetCurrentID();
  metadata["image_pool_start_id"] = pool_service_->GetCurrentID();

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

  sl_element_id_t start_id = 0;
  if (metadata.contains("start_id")) {
    start_id = static_cast<sl_element_id_t>(metadata.at("start_id"));
  }

  sl_element_id_t image_pool_start_id =
      metadata.contains("image_pool_start_id")
          ? static_cast<sl_element_id_t>(metadata.at("image_pool_start_id"))
          : 0;

  storage_service_ = std::make_shared<StorageService>(db_path_);

  RecreateSleeveService(start_id);
  pool_service_ = std::make_shared<ImagePoolService>(storage_service_, image_pool_start_id);
  filter_service_  = std::make_shared<SleeveFilterService>(storage_service_);
  browse_service_  = std::make_shared<AlbumBrowseService>(sleeve_service_);
  package_service_ = std::make_shared<ProjectPackageService>();
}

void ProjectService::RecreateSleeveService(sl_element_id_t start_id) {
  if (!storage_service_) {
    throw std::runtime_error("StorageService is not initialized");
  }
  sleeve_service_ = std::make_shared<SleeveServiceImpl>(storage_service_, db_path_, start_id);
}
};  // namespace alcedo

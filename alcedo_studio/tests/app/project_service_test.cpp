//  Copyright 2026 Yurun Zi
//  SPDX-License-Identifier: GPL-3.0-only
//  Additional permission under GPLv3 section 7 applies; see the LICENSE file.

#include <gtest/gtest.h>

#include <filesystem>
#include <fstream>

#include <json.hpp>

#include "app/project_service.hpp"

namespace alcedo {
class ProjectServiceUUIDTests : public ::testing::Test {
 protected:
  std::filesystem::path db_path_;
  std::filesystem::path meta_path_;

  void SetUp() override {
    db_path_  = std::filesystem::temp_directory_path() / "project_uuid_test.db";
    meta_path_ = std::filesystem::temp_directory_path() / "project_uuid_test.json";

    if (std::filesystem::exists(db_path_)) {
      std::filesystem::remove(db_path_);
    }
    if (std::filesystem::exists(meta_path_)) {
      std::filesystem::remove(meta_path_);
    }
  }

  void TearDown() override {
    if (std::filesystem::exists(db_path_)) {
      std::filesystem::remove(db_path_);
    }
    if (std::filesystem::exists(meta_path_)) {
      std::filesystem::remove(meta_path_);
    }
  }
};

TEST_F(ProjectServiceUUIDTests, NewProjectHasUUID) {
  ProjectService project(db_path_, meta_path_, ProjectOpenMode::kCreateNew);
  const auto&    uuid = project.GetProjectUUID();
  EXPECT_FALSE(uuid.empty());
  EXPECT_EQ(uuid.length(), 36U);  // Standard UUID format: xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx
}

TEST_F(ProjectServiceUUIDTests, UUIDSurvivesSaveLoad) {
  std::string original_uuid;

  {
    ProjectService project(db_path_, meta_path_, ProjectOpenMode::kCreateNew);
    original_uuid = project.GetProjectUUID();
    EXPECT_FALSE(original_uuid.empty());
    project.SaveProject(meta_path_);
  }

  {
    ProjectService project(db_path_, meta_path_, ProjectOpenMode::kLoadExisting);
    EXPECT_EQ(project.GetProjectUUID(), original_uuid);
  }
}

TEST_F(ProjectServiceUUIDTests, UUIDLoadOrCreateNewProject) {
  // When meta file doesn't exist, kLoadOrCreate creates new project with UUID.
  ProjectService project(db_path_, meta_path_, ProjectOpenMode::kLoadOrCreate);
  const auto&    uuid = project.GetProjectUUID();
  EXPECT_FALSE(uuid.empty());
  EXPECT_EQ(uuid.length(), 36U);
}

TEST_F(ProjectServiceUUIDTests, UUIDWrittenToMetadataJSON) {
  {
    ProjectService project(db_path_, meta_path_, ProjectOpenMode::kCreateNew);
    project.SaveProject(meta_path_);
  }

  std::ifstream file(meta_path_);
  ASSERT_TRUE(file.is_open());
  nlohmann::json metadata;
  file >> metadata;

  ASSERT_TRUE(metadata.contains("project_uuid"));
  EXPECT_TRUE(metadata.at("project_uuid").is_string());
  EXPECT_EQ(metadata.at("project_uuid").get<std::string>().length(), 36U);
}

TEST_F(ProjectServiceUUIDTests, UUIDGeneratedForMetadataWithoutUUID) {
  // Create a project first to get a valid database file.
  {
    ProjectService project(db_path_, meta_path_, ProjectOpenMode::kCreateNew);
    project.SaveProject(meta_path_);
  }

  // Read the metadata, remove project_uuid, and write back.
  {
    std::ifstream in(meta_path_);
    ASSERT_TRUE(in.is_open());
    nlohmann::json metadata;
    in >> metadata;
    in.close();

    ASSERT_TRUE(metadata.contains("project_uuid"));
    metadata.erase("project_uuid");

    std::ofstream out(meta_path_);
    ASSERT_TRUE(out.is_open());
    out << metadata.dump(4);
    out.close();
  }

  // Load again — a new UUID should be generated for metadata without one.
  {
    ProjectService project(db_path_, meta_path_, ProjectOpenMode::kLoadExisting);
    const auto&    uuid = project.GetProjectUUID();
    EXPECT_FALSE(uuid.empty());
    EXPECT_EQ(uuid.length(), 36U);
  }
}

TEST_F(ProjectServiceUUIDTests, CopyPreservesUUID) {
  // A project copy (same db, different meta path) keeps the same UUID.
  std::string original_uuid;
  auto        meta_copy_path = std::filesystem::temp_directory_path() / "project_uuid_copy.json";

  // Clean up copy path
  if (std::filesystem::exists(meta_copy_path)) {
    std::filesystem::remove(meta_copy_path);
  }

  {
    ProjectService project(db_path_, meta_path_, ProjectOpenMode::kCreateNew);
    original_uuid = project.GetProjectUUID();
    project.SaveProject(meta_path_);
  }

  // Simulate a copy: copy the meta file to a new location
  std::filesystem::copy_file(meta_path_, meta_copy_path);

  {
    // Load from the copied meta; it should have the same UUID
    ProjectService project(db_path_, meta_copy_path, ProjectOpenMode::kLoadExisting);
    EXPECT_EQ(project.GetProjectUUID(), original_uuid);
  }

  if (std::filesystem::exists(meta_copy_path)) {
    std::filesystem::remove(meta_copy_path);
  }
}
}  // namespace alcedo

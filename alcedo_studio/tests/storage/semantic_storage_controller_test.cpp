//  Copyright 2026 Yurun Zi
//  SPDX-License-Identifier: GPL-3.0-only
//  Additional permission under GPLv3 section 7 applies; see the LICENSE file.

#include "storage/controller/semantic/semantic_storage_controller.hpp"

#include <gtest/gtest.h>

#include <cmath>
#include <filesystem>
#include <limits>
#include <string>
#include <vector>

#include "app/project_service.hpp"
#include "edit/operators/operator_registeration.hpp"
#include "sleeve/sleeve_element/sleeve_file.hpp"

namespace alcedo {
namespace {
constexpr const char* kModelKey = "mobileclip-test";

auto                  OneHot(size_t index) -> std::vector<float> {
  std::vector<float> embedding(kSemanticEmbeddingDim, 0.0F);
  embedding.at(index) = 1.0F;
  return embedding;
}

auto MixedQuery(size_t primary, size_t secondary) -> std::vector<float> {
  std::vector<float> embedding(kSemanticEmbeddingDim, 0.0F);
  embedding.at(primary)   = 0.95F;
  embedding.at(secondary) = 0.05F;
  return embedding;
}

void RegisterTestModel(SemanticStorageController& semantic) {
  std::string error;
  ASSERT_TRUE(semantic.UpsertModel(SemanticModelRecord{.model_key_     = kModelKey,
                                                       .model_id_      = "mobileclip-test",
                                                       .revision_      = "test-rev",
                                                       .embedding_dim_ = kSemanticEmbeddingDim,
                                                       .image_size_    = 256},
                                   &error))
      << error;
}
}  // namespace

class SemanticStorageControllerTest : public ::testing::Test {
 protected:
  std::filesystem::path db_path_;
  std::filesystem::path meta_path_;

  void                  SetUp() override {
    RegisterAllOperators();
    const auto*       test_info = ::testing::UnitTest::GetInstance()->current_test_info();
    const std::string suffix = std::string(test_info->test_suite_name()) + "_" + test_info->name();
    db_path_   = std::filesystem::temp_directory_path() / (suffix + ".db");
    meta_path_ = std::filesystem::temp_directory_path() / (suffix + ".json");
    std::filesystem::remove(db_path_);
    std::filesystem::remove(meta_path_);
  }

  void TearDown() override {
    std::filesystem::remove(db_path_);
    std::filesystem::remove(meta_path_);
  }

  static auto CreateSyntheticFile(ProjectService& project, const file_name_t& file_name)
      -> sl_element_id_t {
    auto image_pool          = project.GetImagePoolService();
    auto image               = image_pool->CreateAndReturnPinnedEmpty();
    auto image_id            = image.Get()->image_id_;
    image.Get()->image_name_ = file_name;
    image.Get()->image_path_ = std::filesystem::path{file_name};
    image_pool->SyncWithStorage();

    auto sleeve = project.GetSleeveService();
    auto file   = sleeve->Write<std::shared_ptr<SleeveFile>>(
        [file_name, image_id](FileSystem& fs) -> std::shared_ptr<SleeveFile> {
          auto created       = fs.CreateFileInLibrary(file_name);
          created->image_id_ = image_id;
          return created;
        });
    EXPECT_TRUE(file.second.success_);
    EXPECT_NE(file.first, nullptr);
    return file.first ? file.first->element_id_ : 0;
  }

  static void StoreEmbedding(SemanticStorageController& semantic, sl_element_id_t file_id,
                             image_id_t image_id, std::vector<float> embedding) {
    std::string error;
    ASSERT_TRUE(semantic.UpsertImageEmbedding(
        SemanticImageEmbeddingRecord{
            .file_id_   = file_id,
            .image_id_  = image_id,
            .model_key_ = kModelKey,
            .embedding_ = std::move(embedding),
        },
        &error))
        << error;
  }
};

TEST_F(SemanticStorageControllerTest, VssSearchRanksWithinRootAndFolderScope) {
  ProjectService project(db_path_, meta_path_, ProjectOpenMode::kCreateNew);
  auto&          semantic = project.GetStorageService()->GetSemanticStorageController();
  RegisterTestModel(semantic);

  const auto mountain_id = CreateSyntheticFile(project, L"mountain.raf");
  const auto beach_id    = CreateSyntheticFile(project, L"beach.raf");
  const auto portrait_id = CreateSyntheticFile(project, L"portrait.raf");

  const auto rows        = project.GetStorageService()->GetElementController().ListFilesInFolder(0);
  ASSERT_EQ(rows.size(), 3U);
  for (const auto& row : rows) {
    if (row.file_id_ == mountain_id) {
      StoreEmbedding(semantic, row.file_id_, row.image_id_, OneHot(0));
    } else if (row.file_id_ == beach_id) {
      StoreEmbedding(semantic, row.file_id_, row.image_id_, OneHot(1));
    } else if (row.file_id_ == portrait_id) {
      StoreEmbedding(semantic, row.file_id_, row.image_id_, OneHot(2));
    }
  }

  std::string error;
  ASSERT_TRUE(semantic.EnsureVectorSearchIndex(kModelKey, &error)) << error;

  const auto root_results =
      semantic.SearchImageEmbeddings(0, kModelKey, MixedQuery(1, 2), 0, 3, &error);
  ASSERT_GE(root_results.size(), 3U) << error;
  EXPECT_EQ(root_results[0].file_id_, beach_id);
  EXPECT_EQ(root_results[1].file_id_, portrait_id);

  auto sleeve = project.GetSleeveService();
  auto album  = sleeve->CreateFolder(L"/", L"SemanticScope");
  ASSERT_TRUE(album.second.success_);
  ASSERT_NE(album.first, nullptr);
  ASSERT_TRUE(sleeve->LinkFileToFolder(portrait_id, album.first->element_id_).success_);

  const auto scoped_results = semantic.SearchImageEmbeddings(album.first->element_id_, kModelKey,
                                                             MixedQuery(1, 2), 0, 3, &error);
  ASSERT_EQ(scoped_results.size(), 1U) << error;
  EXPECT_EQ(scoped_results[0].file_id_, portrait_id);
}

TEST_F(SemanticStorageControllerTest, RejectsInvalidVectorsBeforeStorageOrSearch) {
  ProjectService project(db_path_, meta_path_, ProjectOpenMode::kCreateNew);
  auto&          semantic = project.GetStorageService()->GetSemanticStorageController();

  std::string    error;
  EXPECT_FALSE(semantic.UpsertModel(SemanticModelRecord{.model_key_     = "wrong-dim",
                                                        .model_id_      = "mobileclip-test",
                                                        .revision_      = "test-rev",
                                                        .embedding_dim_ = 3,
                                                        .image_size_    = 256},
                                    &error));
  EXPECT_FALSE(error.empty());

  RegisterTestModel(semantic);
  const auto file_id = CreateSyntheticFile(project, L"invalid_vectors.raf");
  const auto rows    = project.GetStorageService()->GetElementController().ListFilesInFolder(0);
  ASSERT_EQ(rows.size(), 1U);
  const auto image_id  = rows.front().image_id_;

  auto       wrong_dim = OneHot(0);
  wrong_dim.pop_back();
  EXPECT_FALSE(semantic.UpsertImageEmbedding(SemanticImageEmbeddingRecord{.file_id_   = file_id,
                                                                          .image_id_  = image_id,
                                                                          .model_key_ = kModelKey,
                                                                          .embedding_ = wrong_dim},
                                             &error));

  auto non_finite = OneHot(0);
  non_finite[3]   = std::numeric_limits<float>::quiet_NaN();
  EXPECT_FALSE(semantic.UpsertImageEmbedding(SemanticImageEmbeddingRecord{.file_id_   = file_id,
                                                                          .image_id_  = image_id,
                                                                          .model_key_ = kModelKey,
                                                                          .embedding_ = non_finite},
                                             &error));

  std::vector<float> zero(kSemanticEmbeddingDim, 0.0F);
  EXPECT_FALSE(semantic.UpsertImageEmbedding(
      SemanticImageEmbeddingRecord{
          .file_id_ = file_id, .image_id_ = image_id, .model_key_ = kModelKey, .embedding_ = zero},
      &error));

  StoreEmbedding(semantic, file_id, image_id, OneHot(0));
  EXPECT_EQ(semantic.CountImageEmbeddings(kModelKey), 1U);

  const auto results = semantic.SearchImageEmbeddings(0, kModelKey, wrong_dim, 0, 10, &error);
  EXPECT_TRUE(results.empty());
  EXPECT_FALSE(error.empty());
}

TEST_F(SemanticStorageControllerTest, DeletingFileRemovesSemanticRows) {
  ProjectService project(db_path_, meta_path_, ProjectOpenMode::kCreateNew);
  auto&          semantic = project.GetStorageService()->GetSemanticStorageController();
  RegisterTestModel(semantic);

  const auto delete_id = CreateSyntheticFile(project, L"delete_me.raf");
  const auto keep_id   = CreateSyntheticFile(project, L"keep_me.raf");

  const auto rows      = project.GetStorageService()->GetElementController().ListFilesInFolder(0);
  ASSERT_EQ(rows.size(), 2U);
  for (const auto& row : rows) {
    StoreEmbedding(semantic, row.file_id_, row.image_id_,
                   row.file_id_ == delete_id ? OneHot(0) : OneHot(1));
  }
  EXPECT_EQ(semantic.CountImageEmbeddings(kModelKey), 2U);

  ASSERT_TRUE(project.GetSleeveService()->DeleteFileEverywhere(delete_id).success_);

  EXPECT_EQ(semantic.CountImageEmbeddingsForFile(delete_id, kModelKey), 0U);
  EXPECT_EQ(semantic.CountImageEmbeddingsForFile(keep_id, kModelKey), 1U);

  std::string error;
  const auto  results =
      semantic.SearchImageEmbeddings(0, kModelKey, MixedQuery(0, 1), 0, 10, &error);
  ASSERT_EQ(results.size(), 1U) << error;
  EXPECT_EQ(results.front().file_id_, keep_id);
}
}  // namespace alcedo

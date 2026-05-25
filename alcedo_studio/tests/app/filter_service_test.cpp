//  Copyright 2026 Yurun Zi
//  SPDX-License-Identifier: GPL-3.0-only
//  Additional permission under GPLv3 section 7 applies; see the LICENSE file.

#include <gtest/gtest.h>

#include <exiv2/exiv2.hpp>
#include <filesystem>
#include <future>
#include <memory>
#include <string>
#include <unordered_set>
#include <vector>

#include "app/import_service.hpp"
#include "app/project_service.hpp"
#include "app/sleeve_filter_service.hpp"
#include "edit/operators/operator_registeration.hpp"
#include "sleeve/sleeve_element/sleeve_element.hpp"
#include "sleeve/sleeve_element/sleeve_file.hpp"
#include "sleeve/sleeve_filter/filter_combo.hpp"
#include "type/supported_file_type.hpp"
#include "utils/clock/time_provider.hpp"

namespace alcedo {
class FilterServiceTests : public ::testing::Test {
 protected:
  std::filesystem::path db_path_;
  std::filesystem::path meta_path_;

  void                  SetUp() override {
    TimeProvider::Refresh();
    Exiv2::LogMsg::setLevel(Exiv2::LogMsg::Level::mute);
    RegisterAllOperators();

    db_path_ = std::filesystem::temp_directory_path() / "filter_service_test.db";
    meta_path_ = std::filesystem::temp_directory_path() / "filter_service_test.json";

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

  static auto LoadBatchToRoot(ProjectService& project) -> uint32_t {
    auto                           fs_service       = project.GetSleeveService();
    auto                           img_pool_service = project.GetImagePoolService();

    std::unique_ptr<ImportService> import_service =
        std::make_unique<ImportServiceImpl>(fs_service, img_pool_service);

    const image_path_t        batch_dir = std::string(TEST_IMG_PATH) + "/raw/batch";

    std::vector<image_path_t> paths;
    for (const auto& img : std::filesystem::directory_iterator(batch_dir)) {
      if (!img.is_directory() && is_supported_file(img.path())) {
        paths.push_back(img.path().string());
      }
    }

    std::shared_ptr<ImportJob> import_job = std::make_shared<ImportJob>();

    std::promise<ImportResult> final_result;
    auto                       final_result_future = final_result.get_future();

    import_job->on_finished_                       = [&final_result](const ImportResult& result) {
      final_result.set_value(result);
    };

    import_job = import_service->ImportToFolder(paths, L"", {}, import_job);
    EXPECT_NE(import_job, nullptr);

    final_result_future.wait();
    ImportResult result = final_result_future.get();

    EXPECT_EQ(result.requested_, static_cast<uint32_t>(paths.size()));
    EXPECT_EQ(result.failed_, 0u);

    EXPECT_NE(import_job->import_log_, nullptr);
    auto snapshot = import_job->import_log_->Snapshot();
    import_service->SyncImports(snapshot, L"");

    return result.imported_;
  }

  static auto CreateSyntheticFile(ProjectService& project, const file_name_t& file_name,
                                  const std::string& camera_model) -> sl_element_id_t {
    auto image_pool          = project.GetImagePoolService();
    auto image               = image_pool->CreateAndReturnPinnedEmpty();
    auto image_id            = image.Get()->image_id_;
    image.Get()->image_name_ = file_name;
    image.Get()->image_path_ = std::filesystem::path{file_name};
    image.Get()->image_type_ = ImageType::DNG;

    ExifDisplayMetaData metadata;
    metadata.model_         = camera_model;
    metadata.lens_          = "Synthetic 50mm";
    metadata.date_time_str_ = "2025-01-02 03:04:05";
    metadata.aperture_      = 5.6f;
    metadata.iso_           = 200;
    metadata.focal_         = 50.0f;
    image.Get()->SetExifDisplayMetaData(std::move(metadata));
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
};

TEST_F(FilterServiceTests, ASTCreationTest) {
  try {
    FieldCondition cond{
        .field_ = FilterField::ExifCameraModel,
        .op_    = CompareOp::EQUALS,
        .value_ = std::wstring(L"Canon EOS 5D Mark IV"),
    };
    FilterNode root{FilterNode::Type::Condition, {}, {}, std::move(cond), std::nullopt};
    (void)root;
  } catch (const std::exception& e) {
    FAIL() << "Exception during AST creation: " << e.what();
  }
}

TEST_F(FilterServiceTests, SQLCompilationTest) {
  FieldCondition cond{
      .field_ = FilterField::ExifCameraModel,
      .op_    = CompareOp::EQUALS,
      .value_ = std::wstring(L"Canon EOS 5D Mark IV"),
  };
  FilterNode   root{FilterNode::Type::Condition, {}, {}, std::move(cond), std::nullopt};

  std::wstring sql          = FilterSQLCompiler::Compile(root);
  std::wstring expected_sql = L"(json_extract(metadata, '$.Model') = 'Canon EOS 5D Mark IV')";
  EXPECT_EQ(sql, expected_sql);
}

TEST_F(FilterServiceTests, ComplexFilterSQLTest) {
  FieldCondition cond1{
      .field_ = FilterField::ExifCameraModel,
      .op_    = CompareOp::EQUALS,
      .value_ = std::wstring(L"Nikon D850"),
  };
  FilterNode     node1{FilterNode::Type::Condition, {}, {}, std::move(cond1), std::nullopt};

  FieldCondition cond2{
      .field_ = FilterField::FileExtension,
      .op_    = CompareOp::ENDS_WITH,
      .value_ = std::wstring(L".NEF"),
  };
  FilterNode   node2{FilterNode::Type::Condition, {}, {}, std::move(cond2), std::nullopt};

  FilterNode   root{FilterNode::Type::Logical, FilterOp::AND, {node1, node2}, {}, std::nullopt};

  std::wstring sql = FilterSQLCompiler::Compile(root);
  std::wstring expected_sql =
      L"((json_extract(metadata, '$.Model') = 'Nikon D850') AND (UPPER(file_name) LIKE '%.NEF'))";
  EXPECT_EQ(sql, expected_sql);
}

TEST_F(FilterServiceTests, BetweenConditionSQLTest) {
  FieldCondition cond{
      .field_        = FilterField::ExifISO,
      .op_           = CompareOp::BETWEEN,
      .value_        = int64_t(100),
      .second_value_ = int64_t(800),
  };
  FilterNode   root{FilterNode::Type::Condition, {}, {}, std::move(cond), std::nullopt};

  std::wstring sql          = FilterSQLCompiler::Compile(root);
  std::wstring expected_sql = L"(json_extract(metadata, '$.ISO')::INT BETWEEN 100 AND 800)";
  EXPECT_EQ(sql, expected_sql);
}

TEST_F(FilterServiceTests, FolderIndexTest_Model) {
  ProjectService project(db_path_, meta_path_);
  const uint32_t imported = LoadBatchToRoot(project);
  ASSERT_GT(imported, 0u);

  auto sleeve_service = project.GetSleeveService();
  auto root_folder    = sleeve_service->Read<std::shared_ptr<SleeveElement>>(
      [](FileSystem& fs) { return fs.Get(L"/", false); });
  ASSERT_NE(root_folder, nullptr);

  SleeveFilterService filter_service(project.GetStorageService());

  FieldCondition      cond{
           .field_ = FilterField::ExifCameraModel,
           .op_    = CompareOp::CONTAINS,
           .value_ = std::wstring(L"D850"),
  };
  FilterNode root{FilterNode::Type::Condition, {}, {}, std::move(cond), std::nullopt};

  const auto filter_id  = filter_service.CreateFilterCombo(root);
  auto       result_opt = filter_service.ApplyFilterOn(filter_id, root_folder->element_id_);
  ASSERT_TRUE(result_opt.has_value());

  EXPECT_EQ(result_opt->size(), 5u);
}

TEST_F(FilterServiceTests, AlbumScopeFilterUsesMembershipOnly) {
  ProjectService project(db_path_, meta_path_);
  const auto     d850_file_id  = CreateSyntheticFile(project, L"d850.dng", "Nikon D850");
  const auto     other_file_id = CreateSyntheticFile(project, L"other.dng", "Sony A7");
  ASSERT_NE(d850_file_id, 0u);
  ASSERT_NE(other_file_id, 0u);

  auto sleeve_service = project.GetSleeveService();
  auto root_folder    = sleeve_service->Read<std::shared_ptr<SleeveElement>>(
      [](FileSystem& fs) { return fs.Get(L"/", false); });
  ASSERT_NE(root_folder, nullptr);

  SleeveFilterService filter_service(project.GetStorageService());

  FieldCondition      cond{
           .field_ = FilterField::ExifCameraModel,
           .op_    = CompareOp::CONTAINS,
           .value_ = std::wstring(L"D850"),
  };
  FilterNode root{FilterNode::Type::Condition, {}, {}, std::move(cond), std::nullopt};

  const auto filter_id   = filter_service.CreateFilterCombo(root);
  auto       root_result = filter_service.ApplyFilterOn(filter_id, root_folder->element_id_);
  ASSERT_TRUE(root_result.has_value());
  ASSERT_EQ(root_result->size(), 1u);
  ASSERT_EQ(root_result->front(), d850_file_id);

  auto created_album = sleeve_service->CreateFolder(L"/", L"AlbumScope");
  ASSERT_TRUE(created_album.second.success_);
  ASSERT_NE(created_album.first, nullptr);
  const auto album_id = created_album.first->element_id_;

  ASSERT_TRUE(sleeve_service->LinkFileToFolder(d850_file_id, album_id).success_);

  auto album_result = filter_service.ApplyFilterOn(filter_id, album_id);
  ASSERT_TRUE(album_result.has_value());

  ASSERT_EQ(album_result->size(), 1u);
  EXPECT_EQ(album_result->front(), d850_file_id);

  const auto other_album = sleeve_service->CreateFolder(L"/", L"OtherAlbum");
  ASSERT_TRUE(other_album.second.success_);
  ASSERT_TRUE(
      sleeve_service->LinkFileToFolder(other_file_id, other_album.first->element_id_).success_);
  auto other_album_result = filter_service.ApplyFilterOn(filter_id, other_album.first->element_id_);
  ASSERT_TRUE(other_album_result.has_value());
  EXPECT_TRUE(other_album_result->empty());
}

TEST_F(FilterServiceTests, FolderIndexTest_FileExtension) {
  ProjectService project(db_path_, meta_path_);
  const uint32_t imported = LoadBatchToRoot(project);
  ASSERT_GT(imported, 0u);

  auto sleeve_service = project.GetSleeveService();
  auto root_folder    = sleeve_service->Read<std::shared_ptr<SleeveElement>>(
      [](FileSystem& fs) { return fs.Get(L"/", false); });
  ASSERT_NE(root_folder, nullptr);

  SleeveFilterService filter_service(project.GetStorageService());

  FieldCondition      cond{
           .field_ = FilterField::FileExtension,
           .op_    = CompareOp::ENDS_WITH,
           .value_ = std::wstring(L".NEF"),
  };
  FilterNode root{FilterNode::Type::Condition, {}, {}, std::move(cond), std::nullopt};

  const auto filter_id  = filter_service.CreateFilterCombo(root);
  auto       result_opt = filter_service.ApplyFilterOn(filter_id, root_folder->element_id_);
  ASSERT_TRUE(result_opt.has_value());

  EXPECT_EQ(result_opt->size(), 5u);
}

TEST_F(FilterServiceTests, FolderIndexTest_Aperature) {
  ProjectService project(db_path_, meta_path_);
  const uint32_t imported = LoadBatchToRoot(project);
  ASSERT_GT(imported, 0u);

  auto sleeve_service = project.GetSleeveService();
  auto root_folder    = sleeve_service->Read<std::shared_ptr<SleeveElement>>(
      [](FileSystem& fs) { return fs.Get(L"/", false); });
  ASSERT_NE(root_folder, nullptr);

  SleeveFilterService filter_service(project.GetStorageService());

  FieldCondition      cond{
           .field_ = FilterField::ExifAperture,
           .op_    = CompareOp::GREATER_THAN,
           .value_ = double(5.6),
  };
  FilterNode root{FilterNode::Type::Condition, {}, {}, std::move(cond), std::nullopt};

  const auto filter_id  = filter_service.CreateFilterCombo(root);
  auto       result_opt = filter_service.ApplyFilterOn(filter_id, root_folder->element_id_);
  ASSERT_TRUE(result_opt.has_value());

  // raw/batch also contains JPEG siblings for the Sony ARWs. The import path keeps the RAW
  // identities only, so filter counts must match the 11 imported RAW files rather than all 17
  // files on disk.
  EXPECT_EQ(result_opt->size(), 6u);
}

TEST_F(FilterServiceTests, FolderIndexTest_ISO) {
  ProjectService project(db_path_, meta_path_);
  const uint32_t imported = LoadBatchToRoot(project);
  ASSERT_GT(imported, 0u);

  auto sleeve_service = project.GetSleeveService();
  auto root_folder    = sleeve_service->Read<std::shared_ptr<SleeveElement>>(
      [](FileSystem& fs) { return fs.Get(L"/", false); });
  ASSERT_NE(root_folder, nullptr);

  SleeveFilterService filter_service(project.GetStorageService());

  FieldCondition      cond{
           .field_        = FilterField::ExifISO,
           .op_           = CompareOp::BETWEEN,
           .value_        = int64_t(100),
           .second_value_ = int64_t(400),
  };
  FilterNode root{FilterNode::Type::Condition, {}, {}, std::move(cond), std::nullopt};

  const auto filter_id  = filter_service.CreateFilterCombo(root);
  auto       result_opt = filter_service.ApplyFilterOn(filter_id, root_folder->element_id_);
  ASSERT_TRUE(result_opt.has_value());

  // JPEG siblings are not imported as independent library files in this fixture.
  EXPECT_EQ(result_opt->size(), 6u);
}

TEST_F(FilterServiceTests, FolderIndexTest_FocalLength) {
  ProjectService project(db_path_, meta_path_);
  const uint32_t imported = LoadBatchToRoot(project);
  ASSERT_GT(imported, 0u);

  auto sleeve_service = project.GetSleeveService();
  auto root_folder    = sleeve_service->Read<std::shared_ptr<SleeveElement>>(
      [](FileSystem& fs) { return fs.Get(L"/", false); });
  ASSERT_NE(root_folder, nullptr);

  SleeveFilterService filter_service(project.GetStorageService());

  FieldCondition      cond{
           .field_ = FilterField::ExifFocalLength,
           .op_    = CompareOp::LESS_THAN,
           .value_ = double(150.0),
  };
  FilterNode root{FilterNode::Type::Condition, {}, {}, std::move(cond), std::nullopt};

  const auto filter_id  = filter_service.CreateFilterCombo(root);
  auto       result_opt = filter_service.ApplyFilterOn(filter_id, root_folder->element_id_);
  ASSERT_TRUE(result_opt.has_value());

  // Six Sony ARWs plus one Nikon D850 NEF are under 150mm after import.
  EXPECT_EQ(result_opt->size(), 7u);
}

TEST_F(FilterServiceTests, FolderIndexTest_Combined) {
  ProjectService project(db_path_, meta_path_);
  const uint32_t imported = LoadBatchToRoot(project);
  ASSERT_GT(imported, 0u);

  auto sleeve_service = project.GetSleeveService();
  auto root_folder    = sleeve_service->Read<std::shared_ptr<SleeveElement>>(
      [](FileSystem& fs) { return fs.Get(L"/", false); });
  ASSERT_NE(root_folder, nullptr);

  SleeveFilterService filter_service(project.GetStorageService());

  FieldCondition      cond1{
           .field_ = FilterField::ExifCameraModel,
           .op_    = CompareOp::CONTAINS,
           .value_ = std::wstring(L"D850"),
  };
  FilterNode     node1{FilterNode::Type::Condition, {}, {}, std::move(cond1), std::nullopt};

  FieldCondition cond2{
      .field_ = FilterField::ExifFocalLength,
      .op_    = CompareOp::LESS_THAN,
      .value_ = double(150.0),
  };
  FilterNode node2{FilterNode::Type::Condition, {}, {}, std::move(cond2), std::nullopt};

  FilterNode root{FilterNode::Type::Logical, FilterOp::AND, {node1, node2}, {}, std::nullopt};

  const auto filter_id  = filter_service.CreateFilterCombo(root);
  auto       result_opt = filter_service.ApplyFilterOn(filter_id, root_folder->element_id_);
  ASSERT_TRUE(result_opt.has_value());

  EXPECT_EQ(result_opt->size(), 1u);
}

TEST_F(FilterServiceTests, FolderIndexTest_NoMatch) {
  ProjectService project(db_path_, meta_path_);
  const uint32_t imported = LoadBatchToRoot(project);
  ASSERT_GT(imported, 0u);

  auto sleeve_service = project.GetSleeveService();
  auto root_folder    = sleeve_service->Read<std::shared_ptr<SleeveElement>>(
      [](FileSystem& fs) { return fs.Get(L"/", false); });
  ASSERT_NE(root_folder, nullptr);

  SleeveFilterService filter_service(project.GetStorageService());

  FieldCondition      cond{
           .field_ = FilterField::ExifCameraModel,
           .op_    = CompareOp::CONTAINS,
           .value_ = std::wstring(L"A7"),
  };
  FilterNode root{FilterNode::Type::Condition, {}, {}, std::move(cond), std::nullopt};

  const auto filter_id  = filter_service.CreateFilterCombo(root);
  auto       result_opt = filter_service.ApplyFilterOn(filter_id, root_folder->element_id_);
  ASSERT_TRUE(result_opt.has_value());

  EXPECT_EQ(result_opt->size(), 0u);
}

TEST_F(FilterServiceTests, FolderIndexTest_DateRange) {
  ProjectService project(db_path_, meta_path_);
  const uint32_t imported = LoadBatchToRoot(project);
  ASSERT_GT(imported, 0u);

  auto sleeve_service = project.GetSleeveService();
  auto root_folder    = sleeve_service->Read<std::shared_ptr<SleeveElement>>(
      [](FileSystem& fs) { return fs.Get(L"/", false); });
  ASSERT_NE(root_folder, nullptr);

  SleeveFilterService filter_service(project.GetStorageService());

  FieldCondition      cond{
           .field_        = FilterField::CaptureDate,
           .op_           = CompareOp::BETWEEN,
           .value_        = std::tm{0, 0, 0, 1, 0, 125, 0, 0, -1},    // Jan 1, 2025
           .second_value_ = std::tm{0, 0, 0, 31, 11, 125, 0, 0, -1},  // Dec 31, 2025
  };
  FilterNode root{FilterNode::Type::Condition, {}, {}, std::move(cond), std::nullopt};

  const auto filter_id  = filter_service.CreateFilterCombo(root);
  auto       result_opt = filter_service.ApplyFilterOn(filter_id, root_folder->element_id_);
  ASSERT_TRUE(result_opt.has_value());

  // Six Sony ARWs plus one Nikon D850 NEF fall in the 2025 capture-date range.
  EXPECT_EQ(result_opt->size(), 7u);
}

TEST_F(FilterServiceTests, ListFilesInFolderByIdMatchesPathBasedList) {
  ProjectService project(db_path_, meta_path_);
  const uint32_t imported = LoadBatchToRoot(project);
  ASSERT_GT(imported, 0u);

  auto sleeve_service = project.GetSleeveService();
  auto browse         = project.GetAlbumBrowseService();
  ASSERT_NE(browse, nullptr);

  auto root_folder = sleeve_service->Read<std::shared_ptr<SleeveElement>>(
      [](FileSystem& fs) { return fs.Get(L"/", false); });
  ASSERT_NE(root_folder, nullptr);

  const auto path_based = browse->ListFilesInFolder(std::filesystem::path(L"/"));
  const auto id_based   = browse->ListFilesInFolderById(root_folder->element_id_);
  ASSERT_EQ(path_based.size(), id_based.size());

  std::unordered_set<sl_element_id_t> path_ids;
  for (const auto& f : path_based) {
    path_ids.insert(f.file_id_);
  }
  for (const auto& f : id_based) {
    EXPECT_TRUE(path_ids.contains(f.file_id_))
        << "DB-first result file_id " << f.file_id_ << " not in path-based list";
  }
}

TEST_F(FilterServiceTests, ListCountMatchesStatsCount) {
  ProjectService project(db_path_, meta_path_);
  const uint32_t imported = LoadBatchToRoot(project);
  ASSERT_GT(imported, 0u);

  auto sleeve_service = project.GetSleeveService();
  auto root_folder    = sleeve_service->Read<std::shared_ptr<SleeveElement>>(
      [](FileSystem& fs) { return fs.Get(L"/", false); });
  ASSERT_NE(root_folder, nullptr);

  SleeveFilterService filter_service(project.GetStorageService());
  const auto          stats = filter_service.BuildFolderStats(root_folder->element_id_);
  const auto          list  = project.GetStorageService()->GetElementController().ListFilesInFolder(
      root_folder->element_id_);
  EXPECT_EQ(static_cast<size_t>(stats.total_photo_count_), list.size());
}

TEST_F(FilterServiceTests, RootScopeUsesVirtualFileView) {
  ProjectService project(db_path_, meta_path_);
  const auto     file_id = CreateSyntheticFile(project, L"virtual_root.dng", "Nikon D850");
  ASSERT_NE(file_id, 0u);

  auto storage = project.GetStorageService();
  storage->GetElementController().RemoveFolderContent(0, file_id);

  const auto list = storage->GetElementController().ListFilesInFolder(0);
  ASSERT_EQ(list.size(), 1u);
  EXPECT_EQ(list.front().file_id_, file_id);

  SleeveFilterService filter_service(storage);
  const auto          stats = filter_service.BuildFolderStats(0);
  EXPECT_EQ(stats.total_photo_count_, 1);
}

TEST_F(FilterServiceTests, PagedScopeListUsesStableOrderAndCount) {
  ProjectService project(db_path_, meta_path_);
  const auto     first_id  = CreateSyntheticFile(project, L"page_1.dng", "Nikon D850");
  const auto     second_id = CreateSyntheticFile(project, L"page_2.dng", "Nikon D850");
  const auto     third_id  = CreateSyntheticFile(project, L"page_3.dng", "Nikon D850");
  ASSERT_NE(first_id, 0u);
  ASSERT_NE(second_id, 0u);
  ASSERT_NE(third_id, 0u);

  auto browse = project.GetAlbumBrowseService();
  ASSERT_NE(browse, nullptr);
  EXPECT_EQ(browse->CountFilesInFolderById(0), 3u);

  const auto first_page = browse->ListFilesInFolderById(0, 0, 2);
  const auto next_page  = browse->ListFilesInFolderById(0, 2, 2);

  ASSERT_EQ(first_page.size(), 2u);
  ASSERT_EQ(next_page.size(), 1u);
  EXPECT_EQ(first_page[0].file_id_, first_id);
  EXPECT_EQ(first_page[1].file_id_, second_id);
  EXPECT_EQ(next_page[0].file_id_, third_id);
}

TEST_F(FilterServiceTests, FilterCacheInvalidationAfterLink) {
  ProjectService project(db_path_, meta_path_);
  const auto     file_id = CreateSyntheticFile(project, L"cache_test.dng", "Nikon D850");
  ASSERT_NE(file_id, 0u);

  auto sleeve_service = project.GetSleeveService();
  auto album          = sleeve_service->CreateFolder(L"/", L"CacheTestAlbum");
  ASSERT_TRUE(album.second.success_);
  ASSERT_NE(album.first, nullptr);
  const auto          album_id = album.first->element_id_;

  SleeveFilterService filter_service(project.GetStorageService());
  FieldCondition      cond{
           .field_ = FilterField::ExifCameraModel,
           .op_    = CompareOp::CONTAINS,
           .value_ = std::wstring(L"D850"),
  };
  FilterNode root{FilterNode::Type::Condition, {}, {}, std::move(cond), std::nullopt};
  const auto filter_id     = filter_service.CreateFilterCombo(root);

  // Apply filter on the empty album — should return empty.
  auto       result_before = filter_service.ApplyFilterOn(filter_id, album_id);
  ASSERT_TRUE(result_before.has_value());
  EXPECT_TRUE(result_before->empty());

  // Link file to album, invalidate, then re-apply — should now find the file.
  ASSERT_TRUE(sleeve_service->LinkFileToFolder(file_id, album_id).success_);
  filter_service.InvalidateResultCache(album_id);

  auto result_after = filter_service.ApplyFilterOn(filter_id, album_id);
  ASSERT_TRUE(result_after.has_value());
  ASSERT_EQ(result_after->size(), 1u);
  EXPECT_EQ(result_after->front(), file_id);
}

TEST_F(FilterServiceTests, FilterCacheInvalidationAfterUnlink) {
  ProjectService project(db_path_, meta_path_);
  const auto     file_id = CreateSyntheticFile(project, L"unlink_test.dng", "Nikon D850");
  ASSERT_NE(file_id, 0u);

  auto sleeve_service = project.GetSleeveService();
  auto album          = sleeve_service->CreateFolder(L"/", L"UnlinkTestAlbum");
  ASSERT_TRUE(album.second.success_);
  ASSERT_NE(album.first, nullptr);
  const auto album_id = album.first->element_id_;

  ASSERT_TRUE(sleeve_service->LinkFileToFolder(file_id, album_id).success_);

  SleeveFilterService filter_service(project.GetStorageService());
  FieldCondition      cond{
           .field_ = FilterField::ExifCameraModel,
           .op_    = CompareOp::CONTAINS,
           .value_ = std::wstring(L"D850"),
  };
  FilterNode root{FilterNode::Type::Condition, {}, {}, std::move(cond), std::nullopt};
  const auto filter_id     = filter_service.CreateFilterCombo(root);

  // Apply filter on the album — should find the file.
  auto       result_before = filter_service.ApplyFilterOn(filter_id, album_id);
  ASSERT_TRUE(result_before.has_value());
  ASSERT_EQ(result_before->size(), 1u);

  // Unlink file from album, invalidate, then re-apply — should be empty.
  ASSERT_TRUE(sleeve_service->DeleteFileFromFolder(file_id, album_id).success_);
  filter_service.InvalidateResultCache(album_id);

  auto result_after = filter_service.ApplyFilterOn(filter_id, album_id);
  ASSERT_TRUE(result_after.has_value());
  EXPECT_TRUE(result_after->empty());
}

TEST_F(FilterServiceTests, FilterCacheInvalidationAfterDeleteEverywhere) {
  ProjectService project(db_path_, meta_path_);
  const auto     file_id = CreateSyntheticFile(project, L"del_test.dng", "Nikon D850");
  ASSERT_NE(file_id, 0u);

  auto sleeve_service = project.GetSleeveService();
  auto root_folder    = sleeve_service->Read<std::shared_ptr<SleeveElement>>(
      [](FileSystem& fs) { return fs.Get(L"/", false); });
  ASSERT_NE(root_folder, nullptr);

  SleeveFilterService filter_service(project.GetStorageService());
  FieldCondition      cond{
           .field_ = FilterField::ExifCameraModel,
           .op_    = CompareOp::CONTAINS,
           .value_ = std::wstring(L"D850"),
  };
  FilterNode root{FilterNode::Type::Condition, {}, {}, std::move(cond), std::nullopt};
  const auto filter_id     = filter_service.CreateFilterCombo(root);

  // Apply filter on Root — should find the file.
  auto       result_before = filter_service.ApplyFilterOn(filter_id, root_folder->element_id_);
  ASSERT_TRUE(result_before.has_value());
  ASSERT_EQ(result_before->size(), 1u);

  // Delete everywhere, invalidate entire cache, re-apply — should be empty.
  ASSERT_TRUE(sleeve_service->DeleteFileEverywhere(file_id).success_);
  filter_service.InvalidateResultCache();

  auto result_after = filter_service.ApplyFilterOn(filter_id, root_folder->element_id_);
  ASSERT_TRUE(result_after.has_value());
  EXPECT_TRUE(result_after->empty());
}

TEST_F(FilterServiceTests, AlbumScopeListAndStatsAreConsistent) {
  ProjectService project(db_path_, meta_path_);
  const auto     file1_id = CreateSyntheticFile(project, L"consist1.dng", "Nikon D850");
  const auto     file2_id = CreateSyntheticFile(project, L"consist2.dng", "Sony A7");
  ASSERT_NE(file1_id, 0u);
  ASSERT_NE(file2_id, 0u);

  auto sleeve_service = project.GetSleeveService();
  auto album          = sleeve_service->CreateFolder(L"/", L"ConsistencyAlbum");
  ASSERT_TRUE(album.second.success_);
  ASSERT_NE(album.first, nullptr);
  const auto album_id = album.first->element_id_;

  ASSERT_TRUE(sleeve_service->LinkFileToFolder(file1_id, album_id).success_);
  ASSERT_TRUE(sleeve_service->LinkFileToFolder(file2_id, album_id).success_);

  SleeveFilterService filter_service(project.GetStorageService());
  const auto          stats = filter_service.BuildFolderStats(album_id);
  const auto list = project.GetStorageService()->GetElementController().ListFilesInFolder(album_id);
  EXPECT_EQ(static_cast<size_t>(stats.total_photo_count_), list.size());
  EXPECT_EQ(list.size(), 2u);
}

TEST_F(FilterServiceTests, AutoInvalidationOnLink) {
  ProjectService project(db_path_, meta_path_, ProjectOpenMode::kCreateNew);
  const auto     file_id = CreateSyntheticFile(project, L"auto_inval_test.dng", "Nikon D850");
  ASSERT_NE(file_id, 0u);

  auto sleeve_service = project.GetSleeveService();
  auto album          = sleeve_service->CreateFolder(L"/", L"AutoInvalAlbum");
  ASSERT_TRUE(album.second.success_);
  ASSERT_NE(album.first, nullptr);
  const auto album_id       = album.first->element_id_;

  auto       filter_service = project.GetSleeveFilterService();
  ASSERT_NE(filter_service, nullptr);
  auto browse_service = project.GetAlbumBrowseService();
  ASSERT_NE(browse_service, nullptr);

  FieldCondition cond{
      .field_ = FilterField::ExifCameraModel,
      .op_    = CompareOp::CONTAINS,
      .value_ = std::wstring(L"D850"),
  };
  FilterNode root{FilterNode::Type::Condition, {}, {}, std::move(cond), std::nullopt};
  const auto filter_id     = filter_service->CreateFilterCombo(root);

  // Apply filter on the empty album — should return empty.
  auto       result_before = filter_service->ApplyFilterOn(filter_id, album_id);
  ASSERT_TRUE(result_before.has_value());
  EXPECT_TRUE(result_before->empty());

  // Link via AlbumBrowseService — this MUST auto-invalidate the filter cache.
  const auto link_result = browse_service->LinkFilesToFolder({file_id}, album_id);
  EXPECT_EQ(link_result.deleted_files_.size(), 1u);

  // Re-apply filter WITHOUT manual InvalidateResultCache.
  // The AlbumBrowseService should have auto-invalidated the cache.
  auto result_after = filter_service->ApplyFilterOn(filter_id, album_id);
  ASSERT_TRUE(result_after.has_value());
  ASSERT_EQ(result_after->size(), 1u);
  EXPECT_EQ(result_after->front(), file_id);
}

}  // namespace alcedo

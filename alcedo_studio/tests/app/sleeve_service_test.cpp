//  Copyright 2026 Yurun Zi
//  SPDX-License-Identifier: GPL-3.0-only
//  Additional permission under GPLv3 section 7 applies; see the LICENSE file.

#include <gtest/gtest.h>

#include <algorithm>
#include <filesystem>
#include <memory>
#include <random>
#include <stdexcept>
#include <string>
#include <vector>

#include "app/project_service.hpp"
#include "sleeve/sleeve_element/sleeve_element.hpp"
#include "sleeve/sleeve_element/sleeve_file.hpp"
#include "utils/clock/time_provider.hpp"
#include "utils/string/convert.hpp"

namespace alcedo {
namespace {
auto ContainsId(const std::vector<sl_element_id_t>& ids, sl_element_id_t id) -> bool {
  return std::find(ids.begin(), ids.end(), id) != ids.end();
}
}  // namespace

class SleeveServiceTests : public ::testing::Test {
 protected:
  std::filesystem::path db_path_;
  std::filesystem::path meta_path_;

  void                  SetUp() override {
    TimeProvider::Refresh();
    db_path_ = std::filesystem::temp_directory_path() / "sleeve_service_test.db";
    meta_path_ = std::filesystem::temp_directory_path() / "sleeve_service_test.json";
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

TEST_F(SleeveServiceTests, InitAndCreateTest) {
  ProjectService project(db_path_, meta_path_);
  auto           service      = project.GetSleeveService();

  auto           write_result = service->Write<std::shared_ptr<SleeveElement>>(
      [](FileSystem& fs) { return fs.Create(L"", L"Folder", ElementType::FOLDER); });
  EXPECT_NE(write_result.first, nullptr);
  EXPECT_TRUE(write_result.second.success_);

  service->Write<std::shared_ptr<SleeveElement>>(
      [](FileSystem& fs) { return fs.Create(L"/Folder", L"File", ElementType::FILE); });

  auto file = service->Read<std::shared_ptr<SleeveElement>>(
      [](FileSystem& fs) { return fs.Get(L"/Folder/File", false); });
  ASSERT_NE(file, nullptr);
  EXPECT_EQ(file->element_name_, L"File");
}

TEST_F(SleeveServiceTests, DeleteTest) {
  ProjectService project(db_path_, meta_path_);
  auto           service = project.GetSleeveService();

  service->Write<std::shared_ptr<SleeveElement>>(
      [](FileSystem& fs) { return fs.Create(L"", L"File", ElementType::FILE); });
  service->Write<bool>([](FileSystem& fs) {
    fs.Delete(L"/File");
    return true;
  });

  EXPECT_THROW(service->Read<std::shared_ptr<SleeveElement>>(
                   [](FileSystem& fs) { return fs.Get(L"/File", false); }),
               std::runtime_error);
}

TEST_F(SleeveServiceTests, CopyTest) {
  ProjectService project(db_path_, meta_path_);
  auto           service = project.GetSleeveService();

  service->Write<std::shared_ptr<SleeveElement>>(
      [](FileSystem& fs) { return fs.Create(L"", L"Folder", ElementType::FOLDER); });
  service->Write<std::shared_ptr<SleeveElement>>(
      [](FileSystem& fs) { return fs.Create(L"/Folder", L"Subfolder", ElementType::FOLDER); });
  service->Write<std::shared_ptr<SleeveElement>>(
      [](FileSystem& fs) { return fs.Create(L"/Folder/Subfolder", L"Linux", ElementType::FILE); });

  service->Write<bool>([](FileSystem& fs) {
    fs.Copy(L"/Folder/Subfolder", L"/");
    return true;
  });

  auto tree     = service->Read<std::wstring>([](FileSystem& fs) { return fs.Tree(L"/"); });
  auto tree_str = conv::ToBytes(tree);
  EXPECT_NE(tree_str.find("Subfolder"), std::string::npos);
  EXPECT_NE(tree_str.find("Linux"), std::string::npos);
}

TEST_F(SleeveServiceTests, SaveLoadTest) {
  {
    ProjectService project(db_path_, meta_path_);
    auto           service = project.GetSleeveService();
    service->Write<std::shared_ptr<SleeveElement>>(
        [](FileSystem& fs) { return fs.Create(L"", L"Folder", ElementType::FOLDER); });
    service->Write<std::shared_ptr<SleeveElement>>(
        [](FileSystem& fs) { return fs.Create(L"/Folder", L"File", ElementType::FILE); });

    project.SaveProject(meta_path_);
  }

  ProjectService reloaded_project(db_path_, meta_path_);
  // reloaded_project.LoadProject(meta_path_);
  auto           reloaded_service = reloaded_project.GetSleeveService();
  auto           file             = reloaded_service->Read<std::shared_ptr<SleeveElement>>(
      [](FileSystem& fs) { return fs.Get(L"/Folder/File", false); });
  ASSERT_NE(file, nullptr);
  EXPECT_EQ(file->element_name_, L"File");
}

TEST_F(SleeveServiceTests, ResolveAndListImmediateChildrenByPath) {
  ProjectService project(db_path_, meta_path_);
  auto           service = project.GetSleeveService();

  service->CreateFolder(L"/", L"Folder");
  service->CreateFolder(L"/Folder", L"Nested");
  service->Write<std::shared_ptr<SleeveElement>>(
      [](FileSystem& fs) { return fs.Create(L"/Folder/Nested", L"Image", ElementType::FILE); });

  const auto root_entries = service->ListFolderEntries(L"/");
  ASSERT_EQ(root_entries.size(), 1u);
  EXPECT_EQ(root_entries.front()->element_name_, L"Folder");
  EXPECT_EQ(root_entries.front()->type_, ElementType::FOLDER);

  const auto folder = service->ResolveFolder(L"/Folder");
  ASSERT_NE(folder, nullptr);
  EXPECT_EQ(folder->element_name_, L"Folder");

  const auto nested_entries = service->ListFolderEntries(L"/Folder");
  ASSERT_EQ(nested_entries.size(), 1u);
  EXPECT_EQ(nested_entries.front()->element_name_, L"Nested");
  EXPECT_EQ(nested_entries.front()->type_, ElementType::FOLDER);
}

TEST_F(SleeveServiceTests, CreateAndDeleteFolderByPathApi) {
  ProjectService project(db_path_, meta_path_);
  auto           service = project.GetSleeveService();

  const auto created = service->CreateFolder(L"/", L"ToDelete");
  ASSERT_NE(created.first, nullptr);
  EXPECT_TRUE(created.second.success_);
  EXPECT_EQ(created.first->element_name_, L"ToDelete");

  const auto deleted = service->DeletePath(L"/ToDelete");
  EXPECT_TRUE(deleted.success_);
  EXPECT_THROW(service->ResolveFolder(L"/ToDelete"), std::runtime_error);
}

TEST_F(SleeveServiceTests, ReloadedFolderWriteDoesNotCloneSingleParentFolder) {
  {
    ProjectService project(db_path_, meta_path_);
    auto           service = project.GetSleeveService();

    const auto created = service->CreateFolder(L"/", L"test");
    ASSERT_NE(created.first, nullptr);
    ASSERT_TRUE(created.second.success_);

    const auto sync = service->Sync();
    ASSERT_TRUE(sync.success_);
    project.SaveProject(meta_path_);
  }

  ProjectService reloaded_project(db_path_, meta_path_);
  auto           service = reloaded_project.GetSleeveService();

  const auto root_entries = service->ListFolderEntries(L"/");
  ASSERT_EQ(root_entries.size(), 1u);
  ASSERT_EQ(root_entries.front()->element_name_, L"test");

  const auto folder_before = service->ResolveFolder(L"/test");
  ASSERT_NE(folder_before, nullptr);
  const auto folder_id_before = folder_before->element_id_;
  EXPECT_EQ(folder_before->ref_count_, 1u);

  auto created_file = service->Write_NoSync<std::shared_ptr<SleeveElement>>(
      [](FileSystem& fs) { return fs.Create(L"/test", L"after_reload.arw", ElementType::FILE); });
  ASSERT_NE(created_file, nullptr);

  const auto sync = service->Sync();
  EXPECT_TRUE(sync.success_);

  const auto folder_after = service->ResolveFolder(L"/test");
  ASSERT_NE(folder_after, nullptr);
  EXPECT_EQ(folder_after->element_id_, folder_id_before);

  const auto nested_entries = service->ListFolderEntries(L"/test");
  ASSERT_EQ(nested_entries.size(), 1u);
  EXPECT_EQ(nested_entries.front()->element_name_, L"after_reload.arw");
  EXPECT_EQ(nested_entries.front()->type_, ElementType::FILE);
}

TEST_F(SleeveServiceTests, CreateFileInLibraryAndLinkToAlbumKeepsSingleFileIdentity) {
  ProjectService project(db_path_, meta_path_);
  auto           service = project.GetSleeveService();

  const auto album = service->CreateFolder(L"/", L"Album").first;
  ASSERT_NE(album, nullptr);

  const auto file = service->Write<std::shared_ptr<SleeveFile>>(
      [](FileSystem& fs) { return fs.CreateFileInLibrary(L"Photo.arw"); });
  ASSERT_NE(file.first, nullptr);
  ASSERT_TRUE(file.second.success_);

  const auto linked = service->Write<bool>([&](FileSystem& fs) {
    fs.LinkFileToFolder(file.first->element_id_, album->element_id_);
    fs.LinkFileToFolder(file.first->element_id_, album->element_id_);
    return true;
  });
  ASSERT_TRUE(linked.second.success_);

  const auto root_ids = service->Read<std::vector<sl_element_id_t>>(
      [](FileSystem& fs) { return fs.ListFolderContent(0); });
  const auto album_ids = service->Read<std::vector<sl_element_id_t>>(
      [album](FileSystem& fs) { return fs.ListFolderContent(album->element_id_); });

  EXPECT_TRUE(ContainsId(root_ids, file.first->element_id_));
  ASSERT_EQ(album_ids.size(), 1u);
  EXPECT_EQ(album_ids.front(), file.first->element_id_);
  EXPECT_EQ(file.first->ref_count_, 1u);
}

TEST_F(SleeveServiceTests, AlbumMembershipWriteDoesNotTriggerFileCow) {
  ProjectService project(db_path_, meta_path_);
  auto           service = project.GetSleeveService();

  const auto album_a = service->CreateFolder(L"/", L"AlbumA").first;
  const auto album_b = service->CreateFolder(L"/", L"AlbumB").first;
  ASSERT_NE(album_a, nullptr);
  ASSERT_NE(album_b, nullptr);

  const auto file = service->Write<std::shared_ptr<SleeveFile>>(
      [](FileSystem& fs) { return fs.CreateFileInLibrary(L"Shared.arw"); });
  ASSERT_NE(file.first, nullptr);

  service->Write<bool>([&](FileSystem& fs) {
    fs.LinkFileToFolder(file.first->element_id_, album_a->element_id_);
    fs.LinkFileToFolder(file.first->element_id_, album_b->element_id_);
    return true;
  });

  const auto edited = service->Write_NoSync<std::shared_ptr<SleeveFile>>([](FileSystem& fs) {
    auto file = std::static_pointer_cast<SleeveFile>(fs.Get(L"/AlbumA/Shared.arw", true));
    file->SetLastModifiedTime();
    return file;
  });
  ASSERT_NE(edited, nullptr);
  EXPECT_EQ(edited->element_id_, file.first->element_id_);

  const auto from_root =
      service->Read<std::shared_ptr<SleeveFile>>([](FileSystem& fs) {
        return std::static_pointer_cast<SleeveFile>(fs.Get(L"/Shared.arw", false));
      });
  const auto from_album_b =
      service->Read<std::shared_ptr<SleeveFile>>([](FileSystem& fs) {
        return std::static_pointer_cast<SleeveFile>(fs.Get(L"/AlbumB/Shared.arw", false));
      });

  EXPECT_EQ(from_root->element_id_, file.first->element_id_);
  EXPECT_EQ(from_album_b->element_id_, file.first->element_id_);
  EXPECT_EQ(from_root->last_modified_time_, edited->last_modified_time_);
  EXPECT_EQ(from_album_b->last_modified_time_, edited->last_modified_time_);
}

TEST_F(SleeveServiceTests, DeletingFromAlbumOnlyUnlinksMembership) {
  ProjectService project(db_path_, meta_path_);
  auto           service = project.GetSleeveService();

  const auto album = service->CreateFolder(L"/", L"Album").first;
  ASSERT_NE(album, nullptr);

  const auto file = service->Write<std::shared_ptr<SleeveFile>>(
      [](FileSystem& fs) { return fs.CreateFileInLibrary(L"KeepInRoot.arw"); });
  ASSERT_NE(file.first, nullptr);

  service->Write<bool>([&](FileSystem& fs) {
    fs.LinkFileToFolder(file.first->element_id_, album->element_id_);
    fs.Delete(L"/Album/KeepInRoot.arw");
    return true;
  });

  const auto root_ids = service->Read<std::vector<sl_element_id_t>>(
      [](FileSystem& fs) { return fs.ListFolderContent(0); });
  const auto album_ids = service->Read<std::vector<sl_element_id_t>>(
      [album](FileSystem& fs) { return fs.ListFolderContent(album->element_id_); });

  EXPECT_TRUE(ContainsId(root_ids, file.first->element_id_));
  EXPECT_FALSE(ContainsId(album_ids, file.first->element_id_));
  EXPECT_NE(service->ResolveFile(L"/KeepInRoot.arw"), nullptr);
  EXPECT_THROW(service->ResolveFile(L"/Album/KeepInRoot.arw"), std::runtime_error);
}

TEST_F(SleeveServiceTests, DeletingFromRootDeletesFileEverywhereAndPersists) {
  {
    ProjectService project(db_path_, meta_path_);
    auto           service = project.GetSleeveService();

    const auto album = service->CreateFolder(L"/", L"Album").first;
    ASSERT_NE(album, nullptr);

    const auto file = service->Write<std::shared_ptr<SleeveFile>>(
        [](FileSystem& fs) { return fs.CreateFileInLibrary(L"DeleteEverywhere.arw"); });
    ASSERT_NE(file.first, nullptr);

    const auto deleted = service->Write<bool>([&](FileSystem& fs) {
      fs.LinkFileToFolder(file.first->element_id_, album->element_id_);
      fs.Delete(L"/DeleteEverywhere.arw");
      return true;
    });
    ASSERT_TRUE(deleted.second.success_);

    const auto root_ids = service->Read<std::vector<sl_element_id_t>>(
        [](FileSystem& fs) { return fs.ListFolderContent(0); });
    const auto album_ids = service->Read<std::vector<sl_element_id_t>>(
        [album](FileSystem& fs) { return fs.ListFolderContent(album->element_id_); });
    EXPECT_FALSE(ContainsId(root_ids, file.first->element_id_));
    EXPECT_FALSE(ContainsId(album_ids, file.first->element_id_));

    project.SaveProject(meta_path_);
  }

  ProjectService reloaded_project(db_path_, meta_path_);
  auto           reloaded_service = reloaded_project.GetSleeveService();

  const auto root_entries = reloaded_service->ListFolderEntries(L"/");
  ASSERT_EQ(root_entries.size(), 1u);
  ASSERT_EQ(root_entries.front()->element_name_, L"Album");

  const auto album_entries = reloaded_service->ListFolderEntries(L"/Album");
  EXPECT_TRUE(album_entries.empty());
  EXPECT_THROW(reloaded_service->ResolveFile(L"/DeleteEverywhere.arw"), std::runtime_error);
}

TEST_F(SleeveServiceTests, FuzzyCreateCopyTest) {
  std::wstring first_tree;
  {
    ProjectService            project(db_path_, meta_path_);
    auto                      service = project.GetSleeveService();

    std::mt19937              gen(42);
    std::vector<std::wstring> known_paths;
    std::vector<std::wstring> known_folders;
    known_paths.push_back(L"");
    known_folders.push_back(L"");

    auto generate_name = [&gen](int length = 8) {
      static const std::wstring chars =
          L"abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
      std::uniform_int_distribution<> dist(0, static_cast<int>(chars.size() - 1));
      std::wstring                    result;
      for (int i = 0; i < length; ++i) {
        result += chars[dist(gen)];
      }
      return result;
    };

    constexpr int kOperations = 200;
    for (int i = 0; i < kOperations; ++i) {
      std::uniform_int_distribution<> op_dist(0, 1);
      int                             op = op_dist(gen);

      if (known_paths.size() < 2 && op == 1) {
        op = 0;
      }

      if (op == 0) {
        std::uniform_int_distribution<size_t> parent_dist(0, known_folders.size() - 1);
        std::wstring                          parent = known_folders[parent_dist(gen)];
        std::wstring                          name   = generate_name();
        ElementType type = (gen() % 2 == 0) ? ElementType::FOLDER : ElementType::FILE;

        try {
          service->Write_NoSync<std::shared_ptr<SleeveElement>>(
              [&](FileSystem& fs) { return fs.Create(parent, name, type); });
          std::wstring new_path = parent + L"/" + name;
          known_paths.push_back(new_path);
          if (type == ElementType::FOLDER) {
            known_folders.push_back(new_path);
          }
        } catch (const std::exception&) {
          // Ignore invalid ops to keep fuzz running
        }
      } else {
        std::uniform_int_distribution<size_t> from_dist(0, known_paths.size() - 1);
        std::uniform_int_distribution<size_t> dest_dist(0, known_folders.size() - 1);
        std::wstring                          from_path = known_paths[from_dist(gen)];
        std::wstring                          to_parent = known_folders[dest_dist(gen)];

        try {
          service->Write_NoSync<bool>([&](FileSystem& fs) {
            fs.Copy(from_path, to_parent);
            return true;
          });
        } catch (const std::exception&) {
          // Ignore invalid ops to keep fuzz running
        }
      }
      std::cout << "\r\033[2KCompleted operation " << (i + 1) << " / " << kOperations << std::flush;
    }

    first_tree = service->Read<std::wstring>([](FileSystem& fs) { return fs.Tree(L"/"); });
    service->Sync();
    project.SaveProject(meta_path_);
  }
  std::cout << std::endl;

  ProjectService reloaded_project(db_path_, meta_path_);
  // reloaded_project.LoadProject(meta_path_);
  auto           reloaded_service = reloaded_project.GetSleeveService();
  auto           second_tree =
      reloaded_service->Read<std::wstring>([](FileSystem& fs) { return fs.Tree(L"/"); });

  EXPECT_EQ(conv::ToBytes(first_tree), conv::ToBytes(second_tree));
}

}  // namespace alcedo

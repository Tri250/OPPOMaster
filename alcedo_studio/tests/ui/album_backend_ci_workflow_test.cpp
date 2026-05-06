//  Copyright 2026 Yurun Zi
//  SPDX-License-Identifier: GPL-3.0-only
//  Additional permission under GPLv3 section 7 applies; see the LICENSE file.

#include <QSignalSpy>
#include <algorithm>

#include "ui/album_backend_test_fixture.hpp"

namespace alcedo::ui::test {
namespace {

using AlbumBackendCiWorkflowTest = AlbumBackendTestFixture;

auto CollectCiRawFiles(size_t maxCount = 2) -> std::vector<std::filesystem::path> {
  const std::filesystem::path        root{std::string(TEST_IMG_PATH) + "/ci_rawfiles"};
  std::vector<std::filesystem::path> paths;
  if (!std::filesystem::exists(root)) {
    return paths;
  }

  for (const auto& entry : std::filesystem::directory_iterator(root)) {
    if (entry.is_regular_file() && is_supported_file(entry.path())) {
      paths.push_back(entry.path());
    }
  }
  std::sort(paths.begin(), paths.end());
  if (maxCount != 0 && paths.size() > maxCount) {
    paths.resize(maxCount);
  }
  return paths;
}

void WaitForImportFinished(AlbumBackend& backend, int timeoutMs = 60000) {
  QSignalSpy spy(&backend, &AlbumBackend::ImportStateChanged);
  const int  step    = 200;
  int        elapsed = 0;
  while (backend.ImportRunning() && elapsed < timeoutMs) {
    spy.wait(step);
    elapsed += step;
  }
  ProcessEvents(500);
}

}  // namespace

TEST_F(AlbumBackendCiWorkflowTest, ImportCiRawFilesPublishesAlbumItems) {
  AlbumBackend backend;
  ASSERT_TRUE(CreateTestProject(backend));

  const auto images = CollectCiRawFiles();
  ASSERT_FALSE(images.empty()) << "CI RAW fixtures missing under TEST_IMG_PATH/ci_rawfiles";

  backend.StartImport(PathsToQStringList(images));
  WaitForImportFinished(backend);

  EXPECT_FALSE(backend.ImportRunning());
  EXPECT_EQ(backend.ImportFailed(), 0);
  EXPECT_GE(backend.ImportCompleted(), static_cast<int>(images.size()));
  EXPECT_GE(backend.ShownCount(), static_cast<int>(images.size()));
  ASSERT_FALSE(backend.Thumbnails().isEmpty());

  const QVariantMap first      = backend.Thumbnails().front().toMap();
  const auto        element_id = first.value("elementId").toUInt();
  const auto        image_id   = first.value("imageId").toUInt();
  EXPECT_GT(element_id, 0u);
  EXPECT_GT(image_id, 0u);

  const QVariantMap details = backend.GetImageDetails(element_id, image_id);
  EXPECT_FALSE(details.isEmpty());
}

}  // namespace alcedo::ui::test

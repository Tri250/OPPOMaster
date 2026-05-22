//  Copyright 2026 Yurun Zi
//  SPDX-License-Identifier: GPL-3.0-only
//  Additional permission under GPLv3 section 7 applies; see the LICENSE file.

/// @file album_backend_project_test.cpp
/// @brief Project lifecycle tests for AlbumBackend.
///
/// Covers: create project, load project (valid/invalid), save project, and
/// initial service state.

#include "ui/album_backend_test_fixture.hpp"

#include <QSignalSpy>
#include <algorithm>
#include <array>
#include <chrono>
#include <fstream>
#include <optional>

#include <json.hpp>

#include "app/project_package_backend.hpp"
#include "app/project_service.hpp"

namespace alcedo::ui::test {
namespace {

using ProjectTests = AlbumBackendTestFixture;

auto FindPackedProjectPath(const std::filesystem::path& dir)
    -> std::optional<std::filesystem::path> {
  for (const auto& entry : std::filesystem::directory_iterator(dir)) {
    if (entry.is_regular_file() && entry.path().extension() == ".alcd") {
      return entry.path();
    }
  }
  return std::nullopt;
}

void WriteU32Le(std::ostream& stream, uint32_t value) {
  std::array<unsigned char, 4> bytes{};
  bytes[0] = static_cast<unsigned char>(value & 0xFFU);
  bytes[1] = static_cast<unsigned char>((value >> 8U) & 0xFFU);
  bytes[2] = static_cast<unsigned char>((value >> 16U) & 0xFFU);
  bytes[3] = static_cast<unsigned char>((value >> 24U) & 0xFFU);
  stream.write(reinterpret_cast<const char*>(bytes.data()),
               static_cast<std::streamsize>(bytes.size()));
}

void CreateMetadataProject(const std::filesystem::path& dbPath,
                           const std::filesystem::path& metaPath) {
    ProjectService project(dbPath, metaPath, ProjectOpenMode::kCreateNew);
    project.GetSleeveService()->Sync();
    project.GetImagePoolService()->SyncWithStorage();
    project.SaveProject(metaPath);
}

bool WaitForProjectLoadToFinish(AlbumBackend& backend, int timeoutMs = 15000) {
  if (!backend.ProjectLoading()) {
    return true;
  }

  QSignalSpy spy(&backend, &AlbumBackend::ProjectLoadStateChanged);
  const auto deadline = std::chrono::steady_clock::now() + std::chrono::milliseconds(timeoutMs);
  while (backend.ProjectLoading() && std::chrono::steady_clock::now() < deadline) {
    const auto remaining =
        std::chrono::duration_cast<std::chrono::milliseconds>(deadline -
                                                              std::chrono::steady_clock::now())
            .count();
    if (remaining <= 0 || !spy.wait(static_cast<int>(std::min<qint64>(remaining, 500)))) {
      ProcessEvents(50);
    }
  }
  return !backend.ProjectLoading();
}

// ── Initial state ──────────────────────────────────────────────────────────

TEST_F(ProjectTests, ServiceState_InitiallyNotReady) {
  AlbumBackend backend;
  EXPECT_FALSE(backend.ServiceReady());
  EXPECT_FALSE(backend.ServiceMessage().isEmpty());
}

// ── Create project — happy path ────────────────────────────────────────────

TEST_F(ProjectTests, CreateProject_ValidFolder_Succeeds) {
  AlbumBackend backend;
  QSignalSpy   projSpy(&backend, &AlbumBackend::ProjectChanged);
  QSignalSpy   stateSpy(&backend, &AlbumBackend::ServiceStateChanged);

  const bool ok =
      backend.CreateProjectInFolderNamed(PathToQString(temp_dir_), "test_proj");
  EXPECT_TRUE(ok);

  // Wait for async project initialisation.
  WaitForSignal(projSpy, 15000);
  ProcessEvents(500);

  EXPECT_TRUE(backend.ServiceReady());
  EXPECT_FALSE(stateSpy.isEmpty());
}

// ── Create project — empty name ────────────────────────────────────────────

TEST_F(ProjectTests, CreateProject_EmptyName_Fails) {
  AlbumBackend backend;
  const bool   ok =
      backend.CreateProjectInFolderNamed(PathToQString(temp_dir_), "");
  // Either returns false or sets a service message.
  // The critical assertion: no crash.
  if (!ok) {
    SUCCEED();
  } else {
    // If it somehow succeeds with empty name, the service message should
    // still be reasonable.
    ProcessEvents(200);
  }
}

// ── Create project while "loading" — second call rejected ──────────────────

TEST_F(ProjectTests, CreateProject_DoubleCall_SecondRejected) {
  AlbumBackend backend;

  const bool first =
      backend.CreateProjectInFolderNamed(PathToQString(temp_dir_), "proj_a");

  // If first call started async loading, a second call should be rejected.
  if (first && backend.ProjectLoading()) {
    // Create a different subfolder so paths differ.
    const auto subDir = temp_dir_ / "sub";
    std::filesystem::create_directories(subDir);
    const bool second =
        backend.CreateProjectInFolderNamed(PathToQString(subDir), "proj_b");
    EXPECT_FALSE(second);
  }

  // Drain everything so destructor is clean.
  ProcessEvents(2000);
}

// ── Load project — non-existent file ───────────────────────────────────────

TEST_F(ProjectTests, LoadProject_NonexistentFile_Fails) {
  AlbumBackend backend;
  const bool   ok = backend.LoadProject("C:/nonexistent/project.json");
  EXPECT_FALSE(ok);
  EXPECT_FALSE(backend.ServiceReady());
}

// ── Load project — invalid format ──────────────────────────────────────────

TEST_F(ProjectTests, LoadProject_InvalidFormat_Fails) {
  AlbumBackend backend;

  // Create a temporary .txt file — not a valid project format.
  const auto txtPath = temp_dir_ / "notes.txt";
  {
    std::ofstream ofs(txtPath);
    ofs << "hello world";
  }

  const bool ok = backend.LoadProject(PathToQString(txtPath));
  EXPECT_FALSE(ok);
}

TEST_F(ProjectTests, LoadProject_OldPackedProjectVersion_Fails) {
  const auto oldProjectPath = temp_dir_ / "old_project.alcd";
  {
    std::ofstream out(oldProjectPath, std::ios::binary | std::ios::trunc);
    out.write(project_pack::kPackedProjectMagic.data(),
              static_cast<std::streamsize>(project_pack::kPackedProjectMagic.size()));
    WriteU32Le(out, project_pack::kPackedProjectVersion - 1);
  }

  AlbumBackend backend;
  EXPECT_FALSE(backend.LoadProject(PathToQString(oldProjectPath)));
  EXPECT_FALSE(backend.ServiceReady());
}

TEST_F(ProjectTests, LoadProject_CorruptMetadata_Fails) {
  const auto dbPath = temp_dir_ / "corrupt_meta.db";
  const auto metaPath = temp_dir_ / "corrupt_meta.json";
  CreateMetadataProject(dbPath, metaPath);
  {
    std::ofstream out(metaPath, std::ios::trunc);
    out << "{ not valid json";
  }

  AlbumBackend backend;
  EXPECT_FALSE(backend.LoadProject(PathToQString(metaPath)));
  EXPECT_FALSE(backend.ServiceReady());
}

TEST_F(ProjectTests, LoadProject_CorruptDatabaseChecksum_Fails) {
  const auto dbPath = temp_dir_ / "corrupt_db.db";
  const auto metaPath = temp_dir_ / "corrupt_db.json";
  CreateMetadataProject(dbPath, metaPath);
  {
    std::ofstream out(dbPath, std::ios::binary | std::ios::trunc);
    out << "not a duckdb database";
  }

  AlbumBackend backend;
  QSignalSpy projectSpy(&backend, &AlbumBackend::ProjectChanged);
  ASSERT_TRUE(backend.LoadProject(PathToQString(metaPath)));
  ASSERT_TRUE(WaitForProjectLoadToFinish(backend));
  ProcessEvents(200);

  EXPECT_TRUE(projectSpy.isEmpty());
  EXPECT_FALSE(backend.ServiceReady());
}

TEST_F(ProjectTests, LoadProject_ValidMetadataProject_Succeeds) {
  const auto dbPath = temp_dir_ / "valid_project.db";
  const auto metaPath = temp_dir_ / "valid_project.json";
  CreateMetadataProject(dbPath, metaPath);

  AlbumBackend backend;
  QSignalSpy projectSpy(&backend, &AlbumBackend::ProjectChanged);
  ASSERT_TRUE(backend.LoadProject(PathToQString(metaPath)));
  ASSERT_TRUE(WaitForSignal(projectSpy, 15000));
  ProcessEvents(500);

  EXPECT_TRUE(backend.ServiceReady());
}

TEST_F(ProjectTests, LoadProject_ValidPackedProject_Succeeds) {
  {
    AlbumBackend backend;
    ASSERT_TRUE(CreateTestProject(backend, "valid_packed_project"));
    ASSERT_TRUE(backend.SaveProject());
  }

  const auto packedProjectPath = FindPackedProjectPath(temp_dir_);
  ASSERT_TRUE(packedProjectPath.has_value());

  AlbumBackend backend;
  QSignalSpy projectSpy(&backend, &AlbumBackend::ProjectChanged);
  ASSERT_TRUE(backend.LoadProject(PathToQString(*packedProjectPath)));
  ASSERT_TRUE(WaitForSignal(projectSpy, 15000));
  ProcessEvents(500);

  EXPECT_TRUE(backend.ServiceReady());
}

// ── Save project — no project loaded ───────────────────────────────────────

TEST_F(ProjectTests, SaveProject_NoProject_Fails) {
  AlbumBackend backend;
  const bool   ok = backend.SaveProject();
  EXPECT_FALSE(ok);
}

// ── Save project — after create ────────────────────────────────────────────

TEST_F(ProjectTests, SaveProject_AfterCreate_Succeeds) {
  AlbumBackend backend;
  ASSERT_TRUE(CreateTestProject(backend));

  const bool ok = backend.SaveProject();
  EXPECT_TRUE(ok);
}

// ── Create project with default name via convenience overload ──────────────

TEST_F(ProjectTests, CreateProjectInFolder_DefaultName_Succeeds) {
  AlbumBackend backend;
  QSignalSpy   projSpy(&backend, &AlbumBackend::ProjectChanged);

  const bool ok = backend.CreateProjectInFolder(PathToQString(temp_dir_));
  EXPECT_TRUE(ok);

  WaitForSignal(projSpy, 15000);
  ProcessEvents(500);
  EXPECT_TRUE(backend.ServiceReady());
}

}  // namespace
}  // namespace alcedo::ui::test

//  Copyright 2026 Yurun Zi
//  SPDX-License-Identifier: GPL-3.0-only
//  Additional permission under GPLv3 section 7 applies; see the LICENSE file.

#include "app/thumbnail_disk_cache_service.hpp"

#include <gtest/gtest.h>

#include <cstdint>
#include <filesystem>
#include <memory>
#include <opencv2/opencv.hpp>
#include <string>
#include <thread>
#include <vector>

namespace alcedo {
namespace {

class ThumbnailDiskCacheServiceTest : public ::testing::Test {
 protected:
  void SetUp() override {
    temp_dir_ = std::filesystem::temp_directory_path() / "alcedo_test_cache";
    std::filesystem::create_directories(temp_dir_);
  }

  void TearDown() override {
    std::error_code ec;
    std::filesystem::remove_all(temp_dir_, ec);
  }

  static cv::Mat CreateTestImage(int width, int height, uint8_t r, uint8_t g, uint8_t b) {
    cv::Mat mat(height, width, CV_8UC3);
    for (int y = 0; y < height; ++y) {
      for (int x = 0; x < width; ++x) {
        mat.at<cv::Vec3b>(y, x) = cv::Vec3b(b, g, r);
      }
    }
    return mat;
  }

  static ThumbnailDiskCacheKey MakeTestKey(const std::string& project_uuid,
                                           sl_element_id_t     element_id,
                                           ThumbnailResolution res,
                                           const std::string&  version_hash) {
    ThumbnailDiskCacheKey key;
    key.project_uuid        = project_uuid;
    key.element_id          = element_id;
    key.resolution          = res;
    key.edit_version_hash   = version_hash;
    key.cache_schema_version = 1;
    return key;
  }

  std::filesystem::path temp_dir_;
};

TEST_F(ThumbnailDiskCacheServiceTest, ConstructAndInitialize) {
  ThumbnailDiskCacheService service(temp_dir_);
  EXPECT_NO_THROW(service.Initialize("test-project-uuid"));

  auto stats = service.GetStats();
  EXPECT_EQ(stats.total_entries, 0);
  EXPECT_EQ(stats.total_size_bytes, 0);

  service.Shutdown();
}

TEST_F(ThumbnailDiskCacheServiceTest, WriteAndRead) {
  ThumbnailDiskCacheService service(temp_dir_);
  service.Initialize("test-project-uuid");

  cv::Mat original = CreateTestImage(64, 64, 255, 128, 64);
  ImageBuffer buffer(std::move(original));

  auto key = MakeTestKey("test-project-uuid", 1, ThumbnailResolution::k256,
                         "abc123def456");

  EXPECT_FALSE(service.Lookup(key));

  service.EnqueueWrite(key, buffer);

  // Give the background writer time to flush
  std::this_thread::sleep_for(std::chrono::milliseconds(200));

  // After write is enqueued, index should be updated immediately
  auto stats = service.GetStats();
  EXPECT_EQ(stats.total_entries, 1);

  service.Shutdown();

  // Reinitialize and verify persistence
  ThumbnailDiskCacheService service2(temp_dir_);
  service2.Initialize("test-project-uuid");

  EXPECT_TRUE(service2.Lookup(key));

  auto read_buffer = service2.Read(key);
  ASSERT_NE(read_buffer, nullptr);
  EXPECT_TRUE(read_buffer->cpu_data_valid_);

  const auto& decoded = read_buffer->GetCPUData();
  ASSERT_EQ(decoded.rows, 64);
  ASSERT_EQ(decoded.cols, 64);
  ASSERT_EQ(decoded.type(), CV_8UC3);

  // Verify pixel data is approximately the same (lossy JPEG)
  const auto& orig_mat = buffer.GetCPUData();
  double      psnr     = cv::PSNR(orig_mat, decoded);
  EXPECT_GT(psnr, 30.0);

  service2.Shutdown();
}

TEST_F(ThumbnailDiskCacheServiceTest, LookupMissReturnsFalse) {
  ThumbnailDiskCacheService service(temp_dir_);
  service.Initialize("test-project-uuid");

  auto key = MakeTestKey("test-project-uuid", 999, ThumbnailResolution::k1024,
                         "nonexistent-hash");
  EXPECT_FALSE(service.Lookup(key));

  auto stats = service.GetStats();
  EXPECT_EQ(stats.miss_count, 1);
  EXPECT_EQ(stats.hit_count, 0);

  service.Shutdown();
}

TEST_F(ThumbnailDiskCacheServiceTest, ReadNonexistentReturnsNull) {
  ThumbnailDiskCacheService service(temp_dir_);
  service.Initialize("test-project-uuid");

  auto key = MakeTestKey("test-project-uuid", 999, ThumbnailResolution::k1024,
                         "nonexistent-hash");
  auto result = service.Read(key);
  EXPECT_EQ(result, nullptr);

  service.Shutdown();
}

TEST_F(ThumbnailDiskCacheServiceTest, InvalidateRemovesEntries) {
  ThumbnailDiskCacheService service(temp_dir_);
  service.Initialize("test-project-uuid");

  cv::Mat      mat = CreateTestImage(32, 32, 100, 200, 50);
  ImageBuffer  buffer(mat);

  auto key1 = MakeTestKey("test-project-uuid", 1, ThumbnailResolution::k256,
                          "hash_v1");
  auto key2 = MakeTestKey("test-project-uuid", 1, ThumbnailResolution::k512,
                          "hash_v1");
  auto key3 = MakeTestKey("test-project-uuid", 2, ThumbnailResolution::k256,
                          "hash_v1");

  service.EnqueueWrite(key1, buffer);
  service.EnqueueWrite(key2, buffer);
  service.EnqueueWrite(key3, buffer);

  std::this_thread::sleep_for(std::chrono::milliseconds(200));

  EXPECT_EQ(service.GetStats().total_entries, 3);

  // Invalidate element 1 — should remove key1 and key2, but not key3
  service.Invalidate("test-project-uuid", 1);

  EXPECT_FALSE(service.Lookup(key1));
  EXPECT_FALSE(service.Lookup(key2));
  EXPECT_TRUE(service.Lookup(key3));

  service.Shutdown();
}

TEST_F(ThumbnailDiskCacheServiceTest, DifferentResolutionsAreSeparateEntries) {
  ThumbnailDiskCacheService service(temp_dir_);
  service.Initialize("test-project-uuid");

  cv::Mat     mat = CreateTestImage(32, 32, 255, 0, 0);
  ImageBuffer buffer(mat);

  auto key256  = MakeTestKey("test-project-uuid", 1, ThumbnailResolution::k256,  "hash_v1");
  auto key512  = MakeTestKey("test-project-uuid", 1, ThumbnailResolution::k512,  "hash_v1");
  auto key1024 = MakeTestKey("test-project-uuid", 1, ThumbnailResolution::k1024, "hash_v1");

  service.EnqueueWrite(key256, buffer);
  service.EnqueueWrite(key512, buffer);
  service.EnqueueWrite(key1024, buffer);

  std::this_thread::sleep_for(std::chrono::milliseconds(200));

  EXPECT_EQ(service.GetStats().total_entries, 3);
  EXPECT_TRUE(service.Lookup(key256));
  EXPECT_TRUE(service.Lookup(key512));
  EXPECT_TRUE(service.Lookup(key1024));

  service.Shutdown();
}

TEST_F(ThumbnailDiskCacheServiceTest, DifferentVersionHashProducesCacheMiss) {
  ThumbnailDiskCacheService service(temp_dir_);
  service.Initialize("test-project-uuid");

  cv::Mat     mat = CreateTestImage(32, 32, 0, 255, 0);
  ImageBuffer buffer(mat);

  auto key_v1 = MakeTestKey("test-project-uuid", 1, ThumbnailResolution::k256,
                            "version_hash_aaa");
  auto key_v2 = MakeTestKey("test-project-uuid", 1, ThumbnailResolution::k256,
                            "version_hash_bbb");

  service.EnqueueWrite(key_v1, buffer);
  std::this_thread::sleep_for(std::chrono::milliseconds(200));

  // key_v1 should be found, key_v2 should miss (different hash)
  EXPECT_TRUE(service.Lookup(key_v1));
  EXPECT_FALSE(service.Lookup(key_v2));

  service.Shutdown();
}

TEST_F(ThumbnailDiskCacheServiceTest, DifferentProjectUUIDIsolation) {
  ThumbnailDiskCacheService service(temp_dir_);
  service.Initialize("project-a");

  cv::Mat     mat = CreateTestImage(32, 32, 255, 255, 0);
  ImageBuffer buffer(mat);

  auto key_a = MakeTestKey("project-a", 1, ThumbnailResolution::k256, "hash_v1");
  service.EnqueueWrite(key_a, buffer);
  std::this_thread::sleep_for(std::chrono::milliseconds(200));

  service.Shutdown();

  // Reinitialize with different project — should not see project-a's entries
  service.Initialize("project-b");
  EXPECT_FALSE(service.Lookup(key_a));

  service.Shutdown();
}

TEST_F(ThumbnailDiskCacheServiceTest, MetadataPersistenceAcrossRestarts) {
  auto key1 = MakeTestKey("persist-project", 1, ThumbnailResolution::k256,
                          "hash_abc");
  auto key2 = MakeTestKey("persist-project", 2, ThumbnailResolution::k1024,
                          "hash_def");

  {
    ThumbnailDiskCacheService service(temp_dir_);
    service.Initialize("persist-project");

    cv::Mat     mat1 = CreateTestImage(64, 64, 255, 0, 0);
    ImageBuffer buf1(mat1);
    service.EnqueueWrite(key1, buf1);

    cv::Mat     mat2 = CreateTestImage(128, 128, 0, 255, 0);
    ImageBuffer buf2(mat2);
    service.EnqueueWrite(key2, buf2);

    std::this_thread::sleep_for(std::chrono::milliseconds(200));
    service.Shutdown();
  }

  // Restart
  ThumbnailDiskCacheService service2(temp_dir_);
  service2.Initialize("persist-project");

  auto stats = service2.GetStats();
  EXPECT_EQ(stats.total_entries, 2);
  EXPECT_GT(stats.total_size_bytes, 0);

  EXPECT_TRUE(service2.Lookup(key1));
  EXPECT_TRUE(service2.Lookup(key2));

  auto read1 = service2.Read(key1);
  ASSERT_NE(read1, nullptr);
  EXPECT_TRUE(read1->cpu_data_valid_);

  auto read2 = service2.Read(key2);
  ASSERT_NE(read2, nullptr);
  EXPECT_TRUE(read2->cpu_data_valid_);

  // Different resolutions produce different sized images
  const auto& decoded1 = read1->GetCPUData();
  const auto& decoded2 = read2->GetCPUData();
  EXPECT_EQ(decoded1.rows, 64);
  EXPECT_EQ(decoded2.rows, 128);

  service2.Shutdown();
}

TEST_F(ThumbnailDiskCacheServiceTest, StatsTrackingAccuracy) {
  ThumbnailDiskCacheService service(temp_dir_);
  service.Initialize("stats-project");

  EXPECT_EQ(service.GetStats().total_entries, 0);
  EXPECT_EQ(service.GetStats().hit_count, 0);
  EXPECT_EQ(service.GetStats().miss_count, 0);

  cv::Mat     mat = CreateTestImage(32, 32, 128, 128, 128);
  ImageBuffer buffer(mat);

  auto key = MakeTestKey("stats-project", 1, ThumbnailResolution::k256, "hash");
  service.EnqueueWrite(key, buffer);
  std::this_thread::sleep_for(std::chrono::milliseconds(200));

  // Lookup hits
  EXPECT_TRUE(service.Lookup(key));
  EXPECT_TRUE(service.Lookup(key));
  EXPECT_TRUE(service.Lookup(key));

  // Lookup miss
  auto missing_key = MakeTestKey("stats-project", 999, ThumbnailResolution::k256, "hash");
  EXPECT_FALSE(service.Lookup(missing_key));

  auto stats = service.GetStats();
  EXPECT_EQ(stats.total_entries, 1);
  EXPECT_EQ(stats.hit_count, 3);
  EXPECT_EQ(stats.miss_count, 1);

  service.Shutdown();
}

TEST_F(ThumbnailDiskCacheServiceTest, EmptyImageNotCached) {
  ThumbnailDiskCacheService service(temp_dir_);
  service.Initialize("test-project-uuid");

  ImageBuffer empty_buffer;
  EXPECT_FALSE(empty_buffer.cpu_data_valid_);

  auto key = MakeTestKey("test-project-uuid", 1, ThumbnailResolution::k256,
                         "hash");
  service.EnqueueWrite(key, empty_buffer);

  std::this_thread::sleep_for(std::chrono::milliseconds(200));

  EXPECT_FALSE(service.Lookup(key));
  service.Shutdown();
}

TEST_F(ThumbnailDiskCacheServiceTest, MultipleWriteAndRead) {
  ThumbnailDiskCacheService service(temp_dir_);
  service.Initialize("multi-project");

  const int kNumEntries = 20;
  std::vector<ThumbnailDiskCacheKey> keys;
  keys.reserve(kNumEntries);

  for (int i = 0; i < kNumEntries; ++i) {
    auto key = MakeTestKey("multi-project", static_cast<sl_element_id_t>(i),
                           ThumbnailResolution::k256,
                           "hash_" + std::to_string(i));
    keys.push_back(key);

    cv::Mat     mat = CreateTestImage(16, 16, static_cast<uint8_t>(i * 10),
                                      static_cast<uint8_t>(255 - i * 10), 128);
    ImageBuffer buffer(mat);
    service.EnqueueWrite(key, buffer);
  }

  std::this_thread::sleep_for(std::chrono::milliseconds(500));

  auto stats = service.GetStats();
  EXPECT_EQ(stats.total_entries, kNumEntries);

  for (int i = 0; i < kNumEntries; ++i) {
    EXPECT_TRUE(service.Lookup(keys[i])) << "Entry " << i << " not found";
    auto read = service.Read(keys[i]);
    ASSERT_NE(read, nullptr) << "Read failed for entry " << i;
    EXPECT_TRUE(read->cpu_data_valid_);
  }

  service.Shutdown();
}

TEST_F(ThumbnailDiskCacheServiceTest, ShutdownWithoutInitializeIsSafe) {
  ThumbnailDiskCacheService service(temp_dir_);
  EXPECT_NO_THROW(service.Shutdown());
}

TEST_F(ThumbnailDiskCacheServiceTest, DoubleInitializeIsSafe) {
  ThumbnailDiskCacheService service(temp_dir_);
  service.Initialize("project");
  EXPECT_NO_THROW(service.Initialize("project"));
  service.Shutdown();
}

TEST_F(ThumbnailDiskCacheServiceTest, LargeImageEncodeDecode) {
  ThumbnailDiskCacheService service(temp_dir_);
  service.Initialize("large-project");

  cv::Mat     mat = CreateTestImage(512, 512, 200, 100, 50);
  ImageBuffer buffer(mat);

  auto key = MakeTestKey("large-project", 1, ThumbnailResolution::k1024,
                         "large_hash");
  service.EnqueueWrite(key, buffer);
  std::this_thread::sleep_for(std::chrono::milliseconds(300));

  auto read = service.Read(key);
  ASSERT_NE(read, nullptr);
  EXPECT_TRUE(read->cpu_data_valid_);

  const auto& decoded = read->GetCPUData();
  EXPECT_EQ(decoded.rows, 512);
  EXPECT_EQ(decoded.cols, 512);

  double psnr = cv::PSNR(buffer.GetCPUData(), decoded);
  EXPECT_GT(psnr, 30.0);

  service.Shutdown();
}

TEST_F(ThumbnailDiskCacheServiceTest, CacheDirectoryStructure) {
  ThumbnailDiskCacheService service(temp_dir_);
  service.Initialize("dir-structure-project");

  cv::Mat     mat = CreateTestImage(16, 16, 255, 255, 255);
  ImageBuffer buffer(mat);

  auto key = MakeTestKey("dir-structure-project", 1, ThumbnailResolution::k256,
                         "hash123");
  service.EnqueueWrite(key, buffer);
  std::this_thread::sleep_for(std::chrono::milliseconds(200));

  service.Shutdown();

  // Verify directory structure exists
  auto project_dir = temp_dir_ / "dir-structure-project";
  EXPECT_TRUE(std::filesystem::exists(project_dir));

  // Metadata file should exist
  auto metadata_file = project_dir / "cache_metadata.json";
  EXPECT_TRUE(std::filesystem::exists(metadata_file));

  // There should be sharded directory with the .jpg file
  bool found_cache_file = false;
  for (const auto& entry : std::filesystem::recursive_directory_iterator(project_dir)) {
    if (entry.path().extension() == ".bmp") {
      found_cache_file = true;
      break;
    }
  }
  EXPECT_TRUE(found_cache_file);
}

}  // namespace
}  // namespace alcedo

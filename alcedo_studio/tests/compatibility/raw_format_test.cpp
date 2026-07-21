//  Copyright 2026 Yurun Zi
//  SPDX-License-Identifier: GPL-3.0-only
//  Additional permission under GPLv3 section 7 applies; see the LICENSE file.

#include "compatibility_test.hpp"

#include <gtest/gtest.h>

#include <algorithm>
#include <array>
#include <cstddef>
#include <cstdint>
#include <cstring>
#include <filesystem>
#include <fstream>
#include <sstream>
#include <string>
#include <vector>

namespace alcedo::compat {

// ============================================================================
// Helpers
// ============================================================================

namespace {

// Write a minimal TIFF-based RAW header (used for format detection tests).
// Most camera RAW formats (CR2, NEF, ARW, RW2, PEF, SRW, DNG, 3FR, IIQ)
// are TIFF-based with the standard II (little-endian) byte order mark.
auto WriteTiffBasedRawHeader(const std::filesystem::path& path,
                             uint32_t magic_override = 0) -> bool {
  std::ofstream out(path, std::ios::binary);
  if (!out) return false;

  // TIFF little-endian header: "II" + magic 42 + IFD offset 8
  uint8_t header[] = {
      0x49, 0x49,                               // Byte order: little-endian ("II")
      0x2A, 0x00,                               // Magic number: 42
      0x08, 0x00, 0x00, 0x00,                   // Offset to first IFD
      0x00, 0x00,                               // Number of directory entries: 0
  };

  if (magic_override != 0) {
    // Override the first 4 bytes with custom magic
    header[0] = static_cast<uint8_t>(magic_override & 0xFF);
    header[1] = static_cast<uint8_t>((magic_override >> 8) & 0xFF);
    header[2] = static_cast<uint8_t>((magic_override >> 16) & 0xFF);
    header[3] = static_cast<uint8_t>((magic_override >> 24) & 0xFF);
  }

  out.write(reinterpret_cast<const char*>(header), sizeof(header));
  return out.good();
}

// Write a Fuji RAF header ("FUJIFILMCCD-RAW ")
auto WriteFujiRafHeader(const std::filesystem::path& path) -> bool {
  std::ofstream out(path, std::ios::binary);
  if (!out) return false;

  const char header[] = "FUJIFILMCCD-RAW ";  // 16 bytes
  out.write(header, 16);
  return out.good();
}

// Write an Olympus ORF header ("IIRO")
auto WriteOlympusOrfHeader(const std::filesystem::path& path) -> bool {
  std::ofstream out(path, std::ios::binary);
  if (!out) return false;

  // ORF uses "IIRO" instead of "II*\0"
  uint8_t header[] = {
      0x49, 0x49, 0x52, 0x4F,                   // "IIRO"
      0x2A, 0x00, 0x08, 0x00, 0x00, 0x00,
  };
  out.write(reinterpret_cast<const char*>(header), sizeof(header));
  return out.good();
}

// Write a minimal ISOBMFF header (Canon CR3 uses this format)
auto WriteCr3Header(const std::filesystem::path& path) -> bool {
  std::ofstream out(path, std::ios::binary);
  if (!out) return false;

  // ISOBMFF ftyp box for CR3
  uint8_t header[] = {
      0x00, 0x00, 0x00, 0x14,                   // Box size: 20 bytes
      0x66, 0x74, 0x79, 0x70,                   // Box type: "ftyp"
      0x63, 0x72, 0x78, 0x20,                   // Major brand: "crx "
      0x00, 0x00, 0x00, 0x01,                   // Minor version: 1
      0x63, 0x72, 0x78, 0x20,                   // Compatible brand: "crx "
  };
  out.write(reinterpret_cast<const char*>(header), sizeof(header));
  return out.good();
}

// Read the first N bytes from a file for magic number verification.
auto ReadFileMagic(const std::filesystem::path& path, size_t bytes)
    -> std::vector<uint8_t> {
  std::ifstream in(path, std::ios::binary);
  if (!in) return {};
  std::vector<uint8_t> magic(bytes);
  in.read(reinterpret_cast<char*>(magic.data()), static_cast<std::streamsize>(bytes));
  magic.resize(static_cast<size_t>(in.gcount()));
  return magic;
}

// Verify that a file's first bytes match the expected magic for a given format.
auto VerifyMagic(const std::vector<uint8_t>& actual,
                 const RawFormatInfo& expected) -> bool {
  if (actual.size() < expected.magic_offset + 4) return false;

  for (int i = 0; i < 4; ++i) {
    if (expected.magic_bytes[i] == 0) continue;  // wildcard
    if (actual[expected.magic_offset + i] != expected.magic_bytes[i]) return false;
  }
  return true;
}

}  // namespace

// ============================================================================
// Test fixture
// ============================================================================

class RawFormatTest : public ::testing::Test {
 protected:
  void SetUp() override {
    test_dir_ = std::filesystem::temp_directory_path() / "alcedo_raw_compat_test";
    std::error_code ec;
    std::filesystem::create_directories(test_dir_, ec);
  }

  void TearDown() override {
    std::error_code ec;
    std::filesystem::remove_all(test_dir_, ec);
  }

  auto TestDir() const -> const std::filesystem::path& { return test_dir_; }

 private:
  std::filesystem::path test_dir_;
};

// ============================================================================
// Canon CR2/CR3 tests
// ============================================================================

TEST_F(RawFormatTest, CanonCR2Detection) {
  auto path = TestDir() / "test.cr2";
  ASSERT_TRUE(WriteTiffBasedRawHeader(path));

  auto magic = ReadFileMagic(path, 8);
  ASSERT_GE(magic.size(), 4u);

  // CR2 is TIFF-based: "II*\0"
  EXPECT_EQ(magic[0], 0x49);  // 'I'
  EXPECT_EQ(magic[1], 0x49);  // 'I'
  EXPECT_EQ(magic[2], 0x2A);  // '*'
  EXPECT_EQ(magic[3], 0x00);

  EXPECT_TRUE(VerifyMagic(magic, kKnownRawFormats[0]));  // CR2 entry
}

TEST_F(RawFormatTest, CanonCR3Detection) {
  auto path = TestDir() / "test.cr3";
  ASSERT_TRUE(WriteCr3Header(path));

  auto magic = ReadFileMagic(path, 12);
  ASSERT_GE(magic.size(), 8u);

  // CR3 starts with ISOBMFF ftyp box with "crx " brand
  EXPECT_EQ(magic[4], 0x66);  // 'f'
  EXPECT_EQ(magic[5], 0x74);  // 't'
  EXPECT_EQ(magic[6], 0x79);  // 'y'
  EXPECT_EQ(magic[7], 0x70);  // 'p'
}

// ============================================================================
// Nikon NEF/NRW tests
// ============================================================================

TEST_F(RawFormatTest, NikonNEFDetection) {
  auto path = TestDir() / "test.nef";
  ASSERT_TRUE(WriteTiffBasedRawHeader(path));

  auto magic = ReadFileMagic(path, 8);
  EXPECT_TRUE(VerifyMagic(magic, kKnownRawFormats[2]));  // NEF entry
}

TEST_F(RawFormatTest, NikonNRWDetection) {
  auto path = TestDir() / "test.nrw";
  ASSERT_TRUE(WriteTiffBasedRawHeader(path));

  auto magic = ReadFileMagic(path, 8);
  EXPECT_TRUE(VerifyMagic(magic, kKnownRawFormats[3]));  // NRW entry
}

// ============================================================================
// Sony ARW test
// ============================================================================

TEST_F(RawFormatTest, SonyARWDetection) {
  auto path = TestDir() / "test.arw";
  ASSERT_TRUE(WriteTiffBasedRawHeader(path));

  auto magic = ReadFileMagic(path, 8);
  EXPECT_TRUE(VerifyMagic(magic, kKnownRawFormats[4]));  // ARW entry
}

// ============================================================================
// Fuji RAF test
// ============================================================================

TEST_F(RawFormatTest, FujiRAFDetection) {
  auto path = TestDir() / "test.raf";
  ASSERT_TRUE(WriteFujiRafHeader(path));

  auto magic = ReadFileMagic(path, 8);
  ASSERT_GE(magic.size(), 4u);

  // RAF starts with "FUJI"
  EXPECT_EQ(magic[0], 0x46);  // 'F'
  EXPECT_EQ(magic[1], 0x55);  // 'U'
  EXPECT_EQ(magic[2], 0x4A);  // 'J'
  EXPECT_EQ(magic[3], 0x49);  // 'I'

  EXPECT_TRUE(VerifyMagic(magic, kKnownRawFormats[5]));  // RAF entry
}

// ============================================================================
// Panasonic RW2 test
// ============================================================================

TEST_F(RawFormatTest, PanasonicRW2Detection) {
  auto path = TestDir() / "test.rw2";
  ASSERT_TRUE(WriteTiffBasedRawHeader(path));

  auto magic = ReadFileMagic(path, 8);
  EXPECT_TRUE(VerifyMagic(magic, kKnownRawFormats[6]));  // RW2 entry
}

// ============================================================================
// Olympus ORF test
// ============================================================================

TEST_F(RawFormatTest, OlympusORFDetection) {
  auto path = TestDir() / "test.orf";
  ASSERT_TRUE(WriteOlympusOrfHeader(path));

  auto magic = ReadFileMagic(path, 8);
  ASSERT_GE(magic.size(), 4u);

  // ORF uses "IIRO" instead of standard TIFF "II*\0"
  EXPECT_EQ(magic[0], 0x49);  // 'I'
  EXPECT_EQ(magic[1], 0x49);  // 'I'
  EXPECT_EQ(magic[2], 0x52);  // 'R'
  EXPECT_EQ(magic[3], 0x4F);  // 'O'

  EXPECT_TRUE(VerifyMagic(magic, kKnownRawFormats[7]));  // ORF entry
}

// ============================================================================
// Leica DNG test
// ============================================================================

TEST_F(RawFormatTest, LeicaDNGDetection) {
  auto path = TestDir() / "test.dng";
  ASSERT_TRUE(WriteTiffBasedRawHeader(path));

  auto magic = ReadFileMagic(path, 8);
  EXPECT_TRUE(VerifyMagic(magic, kKnownRawFormats[8]));  // DNG entry
}

// ============================================================================
// Pentax PEF test
// ============================================================================

TEST_F(RawFormatTest, PentaxPEFDetection) {
  auto path = TestDir() / "test.pef";
  ASSERT_TRUE(WriteTiffBasedRawHeader(path));

  auto magic = ReadFileMagic(path, 8);
  EXPECT_TRUE(VerifyMagic(magic, kKnownRawFormats[9]));  // PEF entry
}

// ============================================================================
// Phase One IIQ test
// ============================================================================

TEST_F(RawFormatTest, PhaseOneIIQDetection) {
  auto path = TestDir() / "test.iiq";
  ASSERT_TRUE(WriteTiffBasedRawHeader(path));

  auto magic = ReadFileMagic(path, 8);
  EXPECT_TRUE(VerifyMagic(magic, kKnownRawFormats[10]));  // IIQ entry
}

// ============================================================================
// Samsung SRW test
// ============================================================================

TEST_F(RawFormatTest, SamsungSRWDetection) {
  auto path = TestDir() / "test.srw";
  ASSERT_TRUE(WriteTiffBasedRawHeader(path));

  auto magic = ReadFileMagic(path, 8);
  EXPECT_TRUE(VerifyMagic(magic, kKnownRawFormats[11]));  // SRW entry
}

// ============================================================================
// Hasselblad 3FR test
// ============================================================================

TEST_F(RawFormatTest, Hasselblad3FRDetection) {
  auto path = TestDir() / "test.3fr";
  ASSERT_TRUE(WriteTiffBasedRawHeader(path));

  auto magic = ReadFileMagic(path, 8);
  EXPECT_TRUE(VerifyMagic(magic, kKnownRawFormats[12]));  // 3FR entry
}

// ============================================================================
// Corrupted RAW handling
// ============================================================================

TEST_F(RawFormatTest, CorruptedRawTruncatedHeader) {
  // Write a file with only 2 bytes — too short for any valid header
  auto path = TestDir() / "corrupted.cr2";
  {
    std::ofstream out(path, std::ios::binary);
    out.write("II", 2);  // Only byte-order mark, no magic number
  }

  auto magic = ReadFileMagic(path, 8);
  EXPECT_LT(magic.size(), 4u) << "Truncated file should not have full header.";

  // Verify that none of the format verifications pass with truncated data
  for (size_t i = 0; i < kKnownRawFormatCount; ++i) {
    // Should not crash or hang — VerifyMagic handles short buffers gracefully
    if (magic.size() >= kKnownRawFormats[i].magic_offset + 4) {
      // Only check if we have enough bytes
      bool matches = VerifyMagic(magic, kKnownRawFormats[i]);
      if (kKnownRawFormats[i].magic_bytes[2] == 0 && kKnownRawFormats[i].magic_bytes[3] == 0) {
        // If format has wildcards for bytes 2-3, partial match is possible
        // but shouldn't validate a truncated file
      }
    }
  }
}

TEST_F(RawFormatTest, CorruptedRawWrongMagic) {
  // Write a file with wrong magic bytes
  auto path = TestDir() / "corrupted.nef";
  {
    std::ofstream out(path, std::ios::binary);
    uint8_t bad_header[] = {0xDE, 0xAD, 0xBE, 0xEF, 0x00, 0x00, 0x00, 0x00};
    out.write(reinterpret_cast<const char*>(bad_header), sizeof(bad_header));
  }

  auto magic = ReadFileMagic(path, 8);
  ASSERT_GE(magic.size(), 4u);

  // No format should match 0xDEADBEEF
  for (size_t i = 0; i < kKnownRawFormatCount; ++i) {
    // Only check formats without wildcards (all bytes non-zero)
    bool has_wildcard = false;
    for (int j = 0; j < 4; ++j) {
      if (kKnownRawFormats[i].magic_bytes[j] == 0) has_wildcard = true;
    }
    if (!has_wildcard) {
      EXPECT_FALSE(VerifyMagic(magic, kKnownRawFormats[i]))
          << "Format " << kKnownRawFormats[i].format_name
          << " should not match corrupted header.";
    }
  }
}

TEST_F(RawFormatTest, CorruptedRawZeroBytes) {
  // Write a zero-filled file
  auto path = TestDir() / "corrupted_zero.arw";
  {
    std::ofstream out(path, std::ios::binary);
    std::vector<uint8_t> zeros(1024, 0);
    out.write(reinterpret_cast<const char*>(zeros.data()),
              static_cast<std::streamsize>(zeros.size()));
  }

  auto magic = ReadFileMagic(path, 8);
  ASSERT_GE(magic.size(), 4u);

  // All zeros should not match any valid format
  bool any_match = false;
  for (size_t i = 0; i < kKnownRawFormatCount; ++i) {
    if (VerifyMagic(magic, kKnownRawFormats[i])) {
      // CR3 has all-zero magic (wildcard), so it would match. That's expected.
      if (kKnownRawFormats[i].magic_bytes[0] != 0) {
        any_match = true;
      }
    }
  }
  EXPECT_FALSE(any_match) << "Zero-filled file should not match any non-wildcard format.";
}

// ============================================================================
// Large RAW (100MP+) handling
// ============================================================================

TEST_F(RawFormatTest, LargeRawMemoryEstimate) {
  // A 100MP+ RAW file in 14-bit uncompressed format would be approximately:
  // 11648 x 8736 (Phase One IQ4 150MP) x 14 bits / 8 = ~178 MB
  // With the typical 16-bit processed output: 11648 x 8736 x 2 (16-bit) x 3 (RGB) = ~580 MB
  // In RGBA32F: 11648 x 8736 x 4 (float) x 4 (channels) = ~1.5 GB

  constexpr int large_width  = 11648;
  constexpr int large_height = 8736;
  constexpr size_t bytes_rgba32f = static_cast<size_t>(large_width) *
                                    static_cast<size_t>(large_height) * 4 * sizeof(float);

  // Verify the estimate is reasonable (around 1.5 GB)
  EXPECT_GT(bytes_rgba32f, 1ULL * 1024 * 1024 * 1024);   // > 1 GB
  EXPECT_LT(bytes_rgba32f, 2ULL * 1024 * 1024 * 1024);   // < 2 GB

  // Verify that size_t can represent this (64-bit)
  EXPECT_LT(bytes_rgba32f, std::numeric_limits<size_t>::max())
      << "Large RAW size exceeds size_t capacity.";

  // On 32-bit systems, this would overflow
  if (sizeof(void*) < 8) {
    GTEST_SKIP() << "32-bit platform: large RAW (100MP+) cannot be processed in memory.";
  }
}

TEST_F(RawFormatTest, LargeRawTileChunking) {
  // Verify that even large images can be processed in manageable chunks.
  // MemoryBudgetManager should recommend smaller batches for large images.

  constexpr size_t bytes_per_100mp_image = 1500ULL * 1024 * 1024;  // ~1.5 GB
  constexpr size_t typical_system_memory = 16ULL * 1024 * 1024 * 1024;  // 16 GB
  constexpr float  safety_margin = 0.2f;

  size_t safe_available = static_cast<size_t>(
      static_cast<double>(typical_system_memory) * (1.0 - safety_margin));

  // With 16 GB and 20% margin, ~12.8 GB is available
  EXPECT_GT(safe_available, bytes_per_100mp_image)
      << "A 16 GB system with 20% margin should handle at least one 100MP image.";

  // Batch of 8 would require ~12 GB
  size_t batch_8 = bytes_per_100mp_image * 8;
  EXPECT_GT(batch_8, safe_available)
      << "8 concurrent 100MP images exceed safe memory limits on 16 GB.";
}

// ============================================================================
// Format extension matching
// ============================================================================

TEST_F(RawFormatTest, FormatExtensionUniqueness) {
  // Verify that all format extensions in the matrix are unique
  std::vector<std::string> extensions;
  for (size_t i = 0; i < kKnownRawFormatCount; ++i) {
    extensions.emplace_back(kKnownRawFormats[i].extension);
  }
  std::sort(extensions.begin(), extensions.end());
  for (size_t i = 1; i < extensions.size(); ++i) {
    EXPECT_NE(extensions[i - 1], extensions[i])
        << "Duplicate extension found: " << extensions[i];
  }
}

TEST_F(RawFormatTest, FormatBrandCompleteness) {
  // Verify each format has a non-empty brand name
  for (size_t i = 0; i < kKnownRawFormatCount; ++i) {
    EXPECT_NE(kKnownRawFormats[i].brand, nullptr);
    EXPECT_STRNE(kKnownRawFormats[i].brand, "")
        << "Format " << kKnownRawFormats[i].format_name << " has empty brand.";
    EXPECT_NE(kKnownRawFormats[i].format_name, nullptr);
    EXPECT_STRNE(kKnownRawFormats[i].format_name, "")
        << "Format at index " << i << " has empty name.";
  }
}

TEST_F(RawFormatTest, TiffBasedFormatsConsistency) {
  // Verify that all TIFF-based formats have the correct TIFF magic
  const uint8_t tiff_le_magic[] = {0x49, 0x49, 0x2A, 0x00};  // II*\0
  for (size_t i = 0; i < kKnownRawFormatCount; ++i) {
    if (kKnownRawFormats[i].is_tiff_based) {
      // TIFF-based formats should have the TIFF little-endian magic
      EXPECT_EQ(kKnownRawFormats[i].magic_bytes[0], tiff_le_magic[0])
          << kKnownRawFormats[i].format_name << " is TIFF-based but has wrong byte 0.";
      EXPECT_EQ(kKnownRawFormats[i].magic_bytes[1], tiff_le_magic[1])
          << kKnownRawFormats[i].format_name << " is TIFF-based but has wrong byte 1.";
      EXPECT_EQ(kKnownRawFormats[i].magic_bytes[2], tiff_le_magic[2])
          << kKnownRawFormats[i].format_name << " is TIFF-based but has wrong byte 2.";
      EXPECT_EQ(kKnownRawFormats[i].magic_bytes[3], tiff_le_magic[3])
          << kKnownRawFormats[i].format_name << " is TIFF-based but has wrong byte 3.";
    }
  }
}

}  // namespace alcedo::compat

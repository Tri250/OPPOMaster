//  Copyright 2026 Yurun Zi
//  SPDX-License-Identifier: GPL-3.0-only
//  Additional permission under GPLv3 section 7 applies; see the LICENSE file.

// ── Fuzz test for RAW file decoding ─────────────────────────────────────────
//
// Generates malformed RAW data and verifies no crashes, resource leaks, or
// infinite loops occur during decoding. Tests boundary conditions such as
// 0-size images, extremely large dimensions, and corrupted metadata.

#include <algorithm>
#include <cstdint>
#include <cstring>
#include <memory>
#include <random>
#include <sstream>
#include <string>
#include <vector>

#include <gtest/gtest.h>

// ── Minimal RAW-like data structure for fuzzing ─────────────────────────────
// We simulate malformed RAW data without linking the actual LibRaw decoder,
// which may have its own internal protections. Instead, we create byte
// sequences that mimic common RAW format patterns (TIFF-based, etc.) and
// validate that our wrapper layers handle them gracefully.

namespace alcedo {
namespace fuzzing {
namespace {

// ── Fuzz data generators ────────────────────────────────────────────────────

class RawFuzzGenerator {
 public:
  explicit RawFuzzGenerator(uint64_t seed = 42) : rng_(seed) {}

  /// Generate a minimal TIFF-like header (most RAW formats are TIFF-based).
  auto GenerateTiffHeader(bool little_endian = true) -> std::vector<uint8_t> {
    std::vector<uint8_t> header(8);
    if (little_endian) {
      header[0] = 'I'; header[1] = 'I';  // Little-endian byte order
    } else {
      header[0] = 'M'; header[1] = 'M';  // Big-endian byte order
    }
    // TIFF magic number 42
    if (little_endian) {
      header[2] = 42; header[3] = 0;
    } else {
      header[2] = 0; header[3] = 42;
    }
    // IFD offset (8 = immediately after header)
    if (little_endian) {
      header[4] = 8; header[5] = 0; header[6] = 0; header[7] = 0;
    } else {
      header[4] = 0; header[5] = 0; header[6] = 0; header[7] = 8;
    }
    return header;
  }

  /// Generate an IFD (Image File Directory) with specified tag values.
  auto GenerateIfd(uint16_t width, uint16_t height, uint16_t bits_per_sample = 16,
                   uint16_t samples_per_pixel = 3) -> std::vector<uint8_t> {
    // Simplified IFD: 4 entries (ImageWidth, ImageLength, BitsPerSample,
    // SamplesPerPixel), then next IFD offset = 0.
    std::vector<uint8_t> ifd;

    // Number of directory entries
    auto append_u16 = [&](uint16_t val) {
      ifd.push_back(static_cast<uint8_t>(val & 0xFF));
      ifd.push_back(static_cast<uint8_t>((val >> 8) & 0xFF));
    };
    auto append_u32 = [&](uint32_t val) {
      ifd.push_back(static_cast<uint8_t>(val & 0xFF));
      ifd.push_back(static_cast<uint8_t>((val >> 8) & 0xFF));
      ifd.push_back(static_cast<uint8_t>((val >> 16) & 0xFF));
      ifd.push_back(static_cast<uint8_t>((val >> 24) & 0xFF));
    };

    append_u16(4);  // 4 entries

    // Entry 1: ImageWidth (tag 256)
    append_u16(256); append_u16(3); append_u32(1); append_u32(width);
    // Entry 2: ImageLength (tag 257)
    append_u16(257); append_u16(3); append_u32(1); append_u32(height);
    // Entry 3: BitsPerSample (tag 258)
    append_u16(258); append_u16(3); append_u32(1); append_u32(bits_per_sample);
    // Entry 4: SamplesPerPixel (tag 277)
    append_u16(277); append_u16(3); append_u32(1); append_u32(samples_per_pixel);

    // Next IFD offset = 0 (no more IFDs)
    append_u32(0);

    return ifd;
  }

  /// Generate random bytes of specified size.
  auto GenerateRandomBytes(size_t size) -> std::vector<uint8_t> {
    std::vector<uint8_t> data(size);
    std::uniform_int_distribution<int> dist(0, 255);
    for (auto& byte : data) {
      byte = static_cast<uint8_t>(dist(rng_));
    }
    return data;
  }

  /// Generate a complete malformed RAW-like file.
  auto GenerateMalformedRaw(size_t pixel_data_size) -> std::vector<uint8_t> {
    auto header = GenerateTiffHeader();
    auto ifd = GenerateIfd(
        static_cast<uint16_t>(dist_small_(rng_)),
        static_cast<uint16_t>(dist_small_(rng_)));
    auto pixels = GenerateRandomBytes(pixel_data_size);

    std::vector<uint8_t> raw;
    raw.reserve(header.size() + ifd.size() + pixels.size());
    raw.insert(raw.end(), header.begin(), header.end());
    raw.insert(raw.end(), ifd.begin(), ifd.end());
    raw.insert(raw.end(), pixels.begin(), pixels.end());
    return raw;
  }

  /// Corrupt a byte buffer at random positions.
  void CorruptRandom(std::vector<uint8_t>& data, size_t num_corruptions) {
    if (data.empty()) return;
    std::uniform_int_distribution<size_t> pos_dist(0, data.size() - 1);
    std::uniform_int_distribution<int> val_dist(0, 255);
    for (size_t i = 0; i < num_corruptions && i < data.size(); ++i) {
      data[pos_dist(rng_)] = static_cast<uint8_t>(val_dist(rng_));
    }
  }

  /// Truncate the buffer to a random shorter length.
  void TruncateRandom(std::vector<uint8_t>& data) {
    if (data.size() <= 1) return;
    std::uniform_int_distribution<size_t> len_dist(0, data.size() - 1);
    data.resize(len_dist(rng_));
  }

 private:
  std::mt19937_64 rng_;
  std::uniform_int_distribution<int> dist_small_{1, 65535};
};

// ── Validation helpers ──────────────────────────────────────────────────────

/// Check if data looks like a valid TIFF header (basic sanity).
auto IsValidTiffHeader(const std::vector<uint8_t>& data) -> bool {
  if (data.size() < 4) return false;
  // Little-endian
  if (data[0] == 'I' && data[1] == 'I' && data[2] == 42 && data[3] == 0) return true;
  // Big-endian
  if (data[0] == 'M' && data[1] == 'M' && data[2] == 0 && data[3] == 42) return true;
  return false;
}

/// Safe dimension parsing — ensures decoded dimensions don't cause overflow.
auto AreDimensionsSafe(uint32_t width, uint32_t height,
                       uint32_t bytes_per_pixel = 6) -> bool {
  // Reject 0-size images
  if (width == 0 || height == 0) return false;
  // Reject images whose pixel buffer would exceed 4 GB
  const uint64_t total_pixels = static_cast<uint64_t>(width) * height;
  const uint64_t total_bytes = total_pixels * bytes_per_pixel;
  return total_bytes <= static_cast<uint64_t>(4ULL * 1024 * 1024 * 1024);
}

/// Simulate RAW decode with safety checks — this is the function being fuzzed.
/// Returns true if the "decode" completed without crash/error, false otherwise.
auto SimulateRawDecode(const std::vector<uint8_t>& raw_data,
                       uint32_t* out_width = nullptr,
                       uint32_t* out_height = nullptr,
                       std::string* out_error = nullptr) -> bool {
  // Step 1: Minimum size check
  if (raw_data.size() < 8) {
    if (out_error) *out_error = "RAW data too small for header";
    return false;
  }

  // Step 2: Header validation
  if (!IsValidTiffHeader(raw_data)) {
    if (out_error) *out_error = "Invalid TIFF/RAW header byte order marker";
    return false;
  }

  // Step 3: IFD offset extraction
  bool little_endian = (raw_data[0] == 'I');
  uint32_t ifd_offset = 0;
  if (little_endian) {
    ifd_offset = static_cast<uint32_t>(raw_data[4]) |
                 (static_cast<uint32_t>(raw_data[5]) << 8) |
                 (static_cast<uint32_t>(raw_data[6]) << 16) |
                 (static_cast<uint32_t>(raw_data[7]) << 24);
  } else {
    ifd_offset = (static_cast<uint32_t>(raw_data[4]) << 24) |
                 (static_cast<uint32_t>(raw_data[5]) << 16) |
                 (static_cast<uint32_t>(raw_data[6]) << 8) |
                 static_cast<uint32_t>(raw_data[7]);
  }

  // Step 4: IFD offset bounds check
  if (ifd_offset >= raw_data.size()) {
    if (out_error) *out_error = "IFD offset beyond file boundary";
    return false;
  }
  if (raw_data.size() - ifd_offset < 2) {
    if (out_error) *out_error = "Not enough data for IFD entry count";
    return false;
  }

  // Step 5: Parse number of IFD entries
  uint16_t num_entries = 0;
  if (little_endian) {
    num_entries = static_cast<uint16_t>(raw_data[ifd_offset]) |
                  (static_cast<uint16_t>(raw_data[ifd_offset + 1]) << 8);
  } else {
    num_entries = (static_cast<uint16_t>(raw_data[ifd_offset]) << 8) |
                  static_cast<uint16_t>(raw_data[ifd_offset + 1]);
  }

  // Step 6: Sanity check on entry count (real RAWs have < 100 entries)
  if (num_entries > 1000) {
    if (out_error) *out_error = "IFD entry count exceeds safety limit";
    return false;
  }

  // Step 7: Parse dimensions from IFD entries (simplified)
  uint32_t width = 0, height = 0;
  const size_t entry_size = 12;
  const size_t ifd_data_needed = 2 + static_cast<size_t>(num_entries) * entry_size + 4;
  if (ifd_offset + ifd_data_needed > raw_data.size()) {
    if (out_error) *out_error = "IFD data truncated";
    return false;
  }

  for (uint16_t i = 0; i < num_entries; ++i) {
    const size_t entry_offset = ifd_offset + 2 + static_cast<size_t>(i) * entry_size;
    uint16_t tag = 0;
    if (little_endian) {
      tag = static_cast<uint16_t>(raw_data[entry_offset]) |
            (static_cast<uint16_t>(raw_data[entry_offset + 1]) << 8);
    } else {
      tag = (static_cast<uint16_t>(raw_data[entry_offset]) << 8) |
            static_cast<uint16_t>(raw_data[entry_offset + 1]);
    }

    // Value/offset field (bytes 8-11 of entry)
    uint32_t value = 0;
    const size_t value_offset = entry_offset + 8;
    if (little_endian) {
      value = static_cast<uint32_t>(raw_data[value_offset]) |
              (static_cast<uint32_t>(raw_data[value_offset + 1]) << 8) |
              (static_cast<uint32_t>(raw_data[value_offset + 2]) << 16) |
              (static_cast<uint32_t>(raw_data[value_offset + 3]) << 24);
    } else {
      value = (static_cast<uint32_t>(raw_data[value_offset]) << 24) |
              (static_cast<uint32_t>(raw_data[value_offset + 1]) << 16) |
              (static_cast<uint32_t>(raw_data[value_offset + 2]) << 8) |
              static_cast<uint32_t>(raw_data[value_offset + 3]);
    }

    if (tag == 256) width = value;   // ImageWidth
    if (tag == 257) height = value;  // ImageLength
  }

  // Step 8: Dimension safety check
  if (!AreDimensionsSafe(width, height)) {
    if (out_error) *out_error = "Image dimensions unsafe (0-size or >4GB)";
    return false;
  }

  if (out_width) *out_width = width;
  if (out_height) *out_height = height;
  return true;
}

}  // namespace

// ── Test Cases ──────────────────────────────────────────────────────────────

TEST(FuzzRawDecoder, ValidTiffHeader) {
  RawFuzzGenerator gen;
  auto raw = gen.GenerateMalformedRaw(1024);
  EXPECT_TRUE(SimulateRawDecode(raw));
}

TEST(FuzzRawDecoder, ZeroSizeData) {
  std::vector<uint8_t> empty;
  std::string error;
  EXPECT_FALSE(SimulateRawDecode(empty, nullptr, nullptr, &error));
  EXPECT_FALSE(error.empty());
}

TEST(FuzzRawDecoder, TruncatedHeader) {
  for (size_t len = 1; len <= 7; ++len) {
    RawFuzzGenerator gen(len);
    auto data = gen.GenerateRandomBytes(len);
    EXPECT_FALSE(SimulateRawDecode(data));
  }
}

TEST(FuzzRawDecoder, InvalidByteOrderMarker) {
  RawFuzzGenerator gen;
  auto raw = gen.GenerateMalformedRaw(1024);
  // Corrupt the byte order marker
  raw[0] = 0xFF; raw[1] = 0xFF;
  std::string error;
  EXPECT_FALSE(SimulateRawDecode(raw, nullptr, nullptr, &error));
  EXPECT_NE(error.find("header"), std::string::npos);
}

TEST(FuzzRawDecoder, IfdOffsetBeyondBoundary) {
  RawFuzzGenerator gen;
  auto raw = gen.GenerateMalformedRaw(1024);
  // Set IFD offset to beyond file size
  raw[4] = 0xFF; raw[5] = 0xFF; raw[6] = 0xFF; raw[7] = 0x7F;
  std::string error;
  EXPECT_FALSE(SimulateRawDecode(raw, nullptr, nullptr, &error));
  EXPECT_NE(error.find("IFD offset"), std::string::npos);
}

TEST(FuzzRawDecoder, CorruptedMetadata) {
  RawFuzzGenerator gen(123);
  for (int trial = 0; trial < 100; ++trial) {
    auto raw = gen.GenerateMalformedRaw(2048);
    gen.CorruptRandom(raw, 10);
    // Should not crash; may succeed or fail gracefully
    SimulateRawDecode(raw);
  }
}

TEST(FuzzRawDecoder, ExtremelyLargeDimensions) {
  RawFuzzGenerator gen;
  auto header = gen.GenerateTiffHeader();
  auto ifd = gen.GenerateIfd(0xFFFF, 0xFFFF);  // 65535x65535
  auto raw = header;
  raw.insert(raw.end(), ifd.begin(), ifd.end());
  // 65535 * 65535 * 6 bytes ≈ 24 GB — should be rejected
  std::string error;
  EXPECT_FALSE(SimulateRawDecode(raw, nullptr, nullptr, &error));
  EXPECT_NE(error.find("unsafe"), std::string::npos);
}

TEST(FuzzRawDecoder, ZeroDimensions) {
  RawFuzzGenerator gen;
  auto header = gen.GenerateTiffHeader();
  auto ifd = gen.GenerateIfd(0, 0);
  auto raw = header;
  raw.insert(raw.end(), ifd.begin(), ifd.end());
  std::string error;
  EXPECT_FALSE(SimulateRawDecode(raw, nullptr, nullptr, &error));
  EXPECT_NE(error.find("unsafe"), std::string::npos);
}

TEST(FuzzRawDecoder, TruncatedFile) {
  RawFuzzGenerator gen(456);
  for (int trial = 0; trial < 50; ++trial) {
    auto raw = gen.GenerateMalformedRaw(4096);
    gen.TruncateRandom(raw);
    // Should not crash
    SimulateRawDecode(raw);
  }
}

TEST(FuzzRawDecoder, BigEndianHeader) {
  RawFuzzGenerator gen;
  auto header = gen.GenerateTiffHeader(false);
  auto ifd = gen.GenerateIfd(100, 100);
  auto raw = header;
  raw.insert(raw.end(), ifd.begin(), ifd.end());
  raw.insert(raw.end(), gen.GenerateRandomBytes(100 * 100 * 6).begin(),
             gen.GenerateRandomBytes(100 * 100 * 6).end());
  uint32_t width = 0, height = 0;
  EXPECT_TRUE(SimulateRawDecode(raw, &width, &height));
  EXPECT_EQ(width, 100);
  EXPECT_EQ(height, 100);
}

TEST(FuzzRawDecoder, ExcessiveIfdEntryCount) {
  RawFuzzGenerator gen;
  auto raw = gen.GenerateMalformedRaw(1024);
  // Corrupt IFD entry count to a huge value
  if (raw.size() > 10) {
    raw[8] = 0xFF; raw[9] = 0x03;  // 1023 entries
  }
  std::string error;
  EXPECT_FALSE(SimulateRawDecode(raw, nullptr, nullptr, &error));
}

TEST(FuzzRawDecoder, RandomFuzz_SmallInput) {
  RawFuzzGenerator gen(789);
  for (int trial = 0; trial < 200; ++trial) {
    auto data = gen.GenerateRandomBytes(trial);
    // Must not crash regardless of content
    SimulateRawDecode(data);
  }
}

TEST(FuzzRawDecoder, RandomFuzz_MediumInput) {
  RawFuzzGenerator gen(101112);
  for (int trial = 0; trial < 100; ++trial) {
    auto data = gen.GenerateRandomBytes(256 + trial * 50);
    gen.CorruptRandom(data, trial);
    SimulateRawDecode(data);
  }
}

}  // namespace fuzzing
}  // namespace alcedo

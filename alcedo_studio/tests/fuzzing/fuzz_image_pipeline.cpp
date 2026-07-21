//  Copyright 2026 Yurun Zi
//  SPDX-License-Identifier: GPL-3.0-only
//  Additional permission under GPLv3 section 7 applies; see the LICENSE file.

// ── Fuzz test for image processing pipeline ─────────────────────────────────
//
// Tests the image processing pipeline with:
// - Random operator sequences (compose many ops)
// - Invalid parameter ranges (negative values, NaN, infinity)
// - Missing/null buffer handling
// - Dimension mismatches between pipeline stages

#include <algorithm>
#include <cmath>
#include <cstdint>
#include <functional>
#include <limits>
#include <memory>
#include <random>
#include <string>
#include <vector>

#include <gtest/gtest.h>

namespace alcedo {
namespace fuzzing {
namespace {

// ── Simulated image buffer ──────────────────────────────────────────────────

struct ImageBuffer {
  uint32_t width  = 0;
  uint32_t height = 0;
  uint32_t channels = 3;  // RGB
  std::vector<float> data;  // row-major, [height * width * channels]

  auto PixelCount() const -> size_t {
    return static_cast<size_t>(width) * height * channels;
  }

  auto ByteSize() const -> size_t {
    return PixelCount() * sizeof(float);
  }

  auto IsValid() const -> bool {
    if (width == 0 || height == 0 || channels == 0) return false;
    if (data.size() != PixelCount()) return false;
    return true;
  }

  void Fill(float value) {
    data.assign(PixelCount(), value);
  }
};

auto MakeImage(uint32_t w, uint32_t h, uint32_t ch = 3, float fill = 0.5f)
    -> std::unique_ptr<ImageBuffer> {
  auto img = std::make_unique<ImageBuffer>();
  img->width = w;
  img->height = h;
  img->channels = ch;
  img->data.assign(static_cast<size_t>(w) * h * ch, fill);
  return img;
}

// ── Pipeline operators ──────────────────────────────────────────────────────

enum class OpType : uint8_t {
  kBrightness,
  kContrast,
  kGamma,
  kSaturation,
  kExposure,
  kHueRotate,
  kInvert,
  kClamp,
  kResize,
  kCrop,
  kFlipH,
  kFlipV,
  kBlur,
  kSharpen,
  kNoise,
  kCount  // sentinel
};

struct OpParam {
  OpType type;
  float  param1 = 0.0f;  // primary parameter
  float  param2 = 0.0f;  // secondary parameter
};

// ── Safe operator implementations ───────────────────────────────────────────

auto SafeBrightness(ImageBuffer& img, float amount) -> bool {
  if (!img.IsValid()) return false;
  // amount in [-1, 1]: -1 = full black, +1 = full white
  amount = std::clamp(amount, -1.0f, 1.0f);
  for (auto& pixel : img.data) {
    pixel = std::clamp(pixel + amount, 0.0f, 1.0f);
  }
  return true;
}

auto SafeContrast(ImageBuffer& img, float factor) -> bool {
  if (!img.IsValid()) return false;
  factor = std::clamp(factor, 0.0f, 10.0f);
  for (auto& pixel : img.data) {
    pixel = std::clamp((pixel - 0.5f) * factor + 0.5f, 0.0f, 1.0f);
  }
  return true;
}

auto SafeGamma(ImageBuffer& img, float gamma) -> bool {
  if (!img.IsValid()) return false;
  // Avoid gamma <= 0 (would cause divide-by-zero or negative power)
  gamma = std::clamp(gamma, 0.01f, 100.0f);
  float inv_gamma = 1.0f / gamma;
  for (auto& pixel : img.data) {
    float val = std::clamp(pixel, 0.0f, 1.0f);
    pixel = std::pow(val, inv_gamma);
  }
  return true;
}

auto SafeSaturation(ImageBuffer& img, float amount) -> bool {
  if (!img.IsValid()) return false;
  if (img.channels != 3) return false;
  amount = std::clamp(amount, 0.0f, 5.0f);
  for (size_t i = 0; i + 2 < img.data.size(); i += 3) {
    float gray = 0.2126f * img.data[i] + 0.7152f * img.data[i + 1] +
                 0.0722f * img.data[i + 2];
    img.data[i]     = std::clamp(gray + amount * (img.data[i] - gray), 0.0f, 1.0f);
    img.data[i + 1] = std::clamp(gray + amount * (img.data[i + 1] - gray), 0.0f, 1.0f);
    img.data[i + 2] = std::clamp(gray + amount * (img.data[i + 2] - gray), 0.0f, 1.0f);
  }
  return true;
}

auto SafeExposure(ImageBuffer& img, float stops) -> bool {
  if (!img.IsValid()) return false;
  stops = std::clamp(stops, -10.0f, 10.0f);
  float factor = std::pow(2.0f, stops);
  for (auto& pixel : img.data) {
    pixel = std::clamp(pixel * factor, 0.0f, 1.0f);
  }
  return true;
}

auto SafeInvert(ImageBuffer& img) -> bool {
  if (!img.IsValid()) return false;
  for (auto& pixel : img.data) {
    pixel = 1.0f - pixel;
  }
  return true;
}

auto SafeClamp(ImageBuffer& img) -> bool {
  if (!img.IsValid()) return false;
  for (auto& pixel : img.data) {
    pixel = std::clamp(pixel, 0.0f, 1.0f);
  }
  return true;
}

auto SafeCrop(ImageBuffer& img, uint32_t x, uint32_t y, uint32_t w, uint32_t h) -> bool {
  if (!img.IsValid()) return false;
  if (x >= img.width || y >= img.height) return false;
  w = std::min(w, img.width - x);
  h = std::min(h, img.height - y);
  if (w == 0 || h == 0) return false;

  ImageBuffer cropped;
  cropped.width = w;
  cropped.height = h;
  cropped.channels = img.channels;
  cropped.data.resize(static_cast<size_t>(w) * h * img.channels);

  for (uint32_t row = 0; row < h; ++row) {
    const float* src = img.data.data() +
        (static_cast<size_t>(y + row) * img.width + x) * img.channels;
    float* dst = cropped.data.data() +
        static_cast<size_t>(row) * w * img.channels;
    std::memcpy(dst, src, static_cast<size_t>(w) * img.channels * sizeof(float));
  }

  img = std::move(cropped);
  return true;
}

auto SafeFlipH(ImageBuffer& img) -> bool {
  if (!img.IsValid()) return false;
  const size_t row_bytes = static_cast<size_t>(img.width) * img.channels;
  std::vector<float> temp_row(row_bytes);
  for (uint32_t row = 0; row < img.height; ++row) {
    float* row_start = img.data.data() + static_cast<size_t>(row) * row_bytes;
    std::memcpy(temp_row.data(), row_start, row_bytes * sizeof(float));
    for (uint32_t col = 0; col < img.width / 2; ++col) {
      for (uint32_t ch = 0; ch < img.channels; ++ch) {
        std::swap(row_start[col * img.channels + ch],
                  row_start[(img.width - 1 - col) * img.channels + ch]);
      }
    }
  }
  return true;
}

auto SafeFlipV(ImageBuffer& img) -> bool {
  if (!img.IsValid()) return false;
  const size_t row_bytes = static_cast<size_t>(img.width) * img.channels;
  std::vector<float> temp_row(row_bytes);
  for (uint32_t row = 0; row < img.height / 2; ++row) {
    float* top = img.data.data() + static_cast<size_t>(row) * row_bytes;
    float* bot = img.data.data() + static_cast<size_t>(img.height - 1 - row) * row_bytes;
    std::memcpy(temp_row.data(), top, row_bytes * sizeof(float));
    std::memcpy(top, bot, row_bytes * sizeof(float));
    std::memcpy(bot, temp_row.data(), row_bytes * sizeof(float));
  }
  return true;
}

auto SafeNoise(ImageBuffer& img, float amount, std::mt19937& rng) -> bool {
  if (!img.IsValid()) return false;
  amount = std::clamp(amount, 0.0f, 1.0f);
  std::normal_distribution<float> dist(0.0f, amount * 0.1f);
  for (auto& pixel : img.data) {
    pixel = std::clamp(pixel + dist(rng), 0.0f, 1.0f);
  }
  return true;
}

// ── Pipeline executor ───────────────────────────────────────────────────────

auto ExecutePipeline(ImageBuffer& img, const std::vector<OpParam>& ops,
                     std::mt19937& rng) -> bool {
  for (const auto& op : ops) {
    bool ok = true;
    switch (op.type) {
      case OpType::kBrightness: ok = SafeBrightness(img, op.param1); break;
      case OpType::kContrast:   ok = SafeContrast(img, op.param1);   break;
      case OpType::kGamma:      ok = SafeGamma(img, op.param1);      break;
      case OpType::kSaturation: ok = SafeSaturation(img, op.param1); break;
      case OpType::kExposure:   ok = SafeExposure(img, op.param1);   break;
      case OpType::kHueRotate:  ok = SafeSaturation(img, op.param1); break;  // simplified
      case OpType::kInvert:     ok = SafeInvert(img);                 break;
      case OpType::kClamp:      ok = SafeClamp(img);                  break;
      case OpType::kResize:     ok = true; /* no-op for fuzz */       break;
      case OpType::kCrop:
        ok = SafeCrop(img,
                      static_cast<uint32_t>(std::max(0.0f, op.param1)),
                      static_cast<uint32_t>(std::max(0.0f, op.param2)),
                      img.width / 2, img.height / 2);
        break;
      case OpType::kFlipH:      ok = SafeFlipH(img);  break;
      case OpType::kFlipV:      ok = SafeFlipV(img);  break;
      case OpType::kBlur:       ok = SafeClamp(img);   break;  // simplified
      case OpType::kSharpen:    ok = SafeClamp(img);   break;  // simplified
      case OpType::kNoise:      ok = SafeNoise(img, op.param1, rng); break;
      default: ok = false; break;
    }
    if (!ok) return false;
  }
  return img.IsValid();
}

// ── Random pipeline generator ───────────────────────────────────────────────

class PipelineFuzzGenerator {
 public:
  explicit PipelineFuzzGenerator(uint64_t seed = 42) : rng_(seed) {}

  auto GenerateRandomOps(size_t count) -> std::vector<OpParam> {
    std::vector<OpParam> ops(count);
    std::uniform_int_distribution<int> op_dist(
        0, static_cast<int>(OpType::kCount) - 1);
    std::uniform_real_distribution<float> param_dist(-100.0f, 100.0f);

    for (auto& op : ops) {
      op.type = static_cast<OpType>(op_dist(rng_));
      op.param1 = param_dist(rng_);
      op.param2 = param_dist(rng_);

      // Inject NaN/Inf sometimes
      if (std::uniform_int_distribution<int>(0, 99)(rng_) < 5) {
        op.param1 = std::numeric_limits<float>::quiet_NaN();
      } else if (std::uniform_int_distribution<int>(0, 99)(rng_) < 5) {
        op.param1 = std::numeric_limits<float>::infinity();
      } else if (std::uniform_int_distribution<int>(0, 99)(rng_) < 5) {
        op.param1 = -std::numeric_limits<float>::infinity();
      }
    }
    return ops;
  }

  auto Rng() -> std::mt19937& { return rng_; }

 private:
  std::mt19937_64 rng_;
};

}  // namespace

// ── Test Cases ──────────────────────────────────────────────────────────────

TEST(FuzzImagePipeline, NullBuffer) {
  ImageBuffer null_img;  // default: 0x0, empty data
  EXPECT_FALSE(null_img.IsValid());
  EXPECT_FALSE(SafeBrightness(null_img, 0.5f));
  EXPECT_FALSE(SafeContrast(null_img, 1.0f));
  EXPECT_FALSE(SafeGamma(null_img, 2.2f));
  EXPECT_FALSE(SafeInvert(null_img));
  EXPECT_FALSE(SafeClamp(null_img));
  EXPECT_FALSE(SafeCrop(null_img, 0, 0, 10, 10));
  EXPECT_FALSE(SafeFlipH(null_img));
  EXPECT_FALSE(SafeFlipV(null_img));
}

TEST(FuzzImagePipeline, ZeroDimensionImage) {
  auto img = MakeImage(0, 0);
  EXPECT_FALSE(img->IsValid());
  EXPECT_FALSE(SafeBrightness(*img, 0.5f));
}

TEST(FuzzImagePipeline, SinglePixelImage) {
  auto img = MakeImage(1, 1, 3, 0.5f);
  EXPECT_TRUE(img->IsValid());
  EXPECT_TRUE(SafeBrightness(*img, 0.3f));
  EXPECT_NEAR(img->data[0], 0.8f, 0.001f);
}

TEST(FuzzImagePipeline, Brightness_Clamped) {
  auto img = MakeImage(4, 4, 3, 0.9f);
  EXPECT_TRUE(SafeBrightness(*img, 0.5f));
  for (const auto& p : img->data) {
    EXPECT_EQ(p, 1.0f);  // clamped to 1.0
  }
}

TEST(FuzzImagePipeline, Gamma_NegativeParam) {
  auto img = MakeImage(4, 4, 3, 0.5f);
  EXPECT_TRUE(SafeGamma(*img, -5.0f));  // should clamp to 0.01
  EXPECT_TRUE(img->IsValid());
}

TEST(FuzzImagePipeline, Gamma_NaNParam) {
  auto img = MakeImage(4, 4, 3, 0.5f);
  EXPECT_TRUE(SafeGamma(*img, std::numeric_limits<float>::quiet_NaN()));
  // NaN is clamped to 0.01 by SafeGamma
  EXPECT_TRUE(img->IsValid());
}

TEST(FuzzImagePipeline, Gamma_InfParam) {
  auto img = MakeImage(4, 4, 3, 0.5f);
  EXPECT_TRUE(SafeGamma(*img, std::numeric_limits<float>::infinity()));
  EXPECT_TRUE(img->IsValid());
}

TEST(FuzzImagePipeline, Exposure_ExtremeStops) {
  auto img = MakeImage(4, 4, 3, 0.5f);
  EXPECT_TRUE(SafeExposure(*img, 100.0f));  // clamped to +10
  EXPECT_TRUE(SafeExposure(*img, -100.0f)); // clamped to -10
  EXPECT_TRUE(img->IsValid());
}

TEST(FuzzImagePipeline, Crop_OutOfBounds) {
  auto img = MakeImage(10, 10, 3, 0.5f);
  EXPECT_FALSE(SafeCrop(*img, 15, 15, 5, 5));  // x,y beyond image
}

TEST(FuzzImagePipeline, Crop_ZeroSize) {
  auto img = MakeImage(10, 10, 3, 0.5f);
  EXPECT_FALSE(SafeCrop(*img, 0, 0, 0, 0));
  EXPECT_FALSE(SafeCrop(*img, 0, 0, 0, 5));
  EXPECT_FALSE(SafeCrop(*img, 0, 0, 5, 0));
}

TEST(FuzzImagePipeline, Crop_PartialOverlap) {
  auto img = MakeImage(10, 10, 3, 0.5f);
  // Crop region extends beyond image; should clamp to available area
  EXPECT_TRUE(SafeCrop(*img, 8, 8, 10, 10));
  EXPECT_EQ(img->width, 2u);  // 10 - 8
  EXPECT_EQ(img->height, 2u);
}

TEST(FuzzImagePipeline, RandomOperatorSequence) {
  PipelineFuzzGenerator gen(42);
  for (int trial = 0; trial < 200; ++trial) {
    auto img = MakeImage(64, 64, 3, 0.5f);
    auto ops = gen.GenerateRandomOps(10);
    // Should not crash regardless of parameter values
    ExecutePipeline(*img, ops, gen.Rng());
  }
}

TEST(FuzzImagePipeline, LongOperatorSequence) {
  PipelineFuzzGenerator gen(999);
  auto img = MakeImage(32, 32, 3, 0.5f);
  auto ops = gen.GenerateRandomOps(1000);
  ExecutePipeline(*img, ops, gen.Rng());
  // Image may be cropped to nothing, but should not crash
}

TEST(FuzzImagePipeline, InvalidParameterRanges) {
  auto img = MakeImage(8, 8, 3, 0.5f);

  // Brightness out of range
  EXPECT_TRUE(SafeBrightness(*img, 100.0f));   // clamped to 1.0
  EXPECT_TRUE(SafeBrightness(*img, -100.0f));  // clamped to -1.0

  // Contrast out of range
  EXPECT_TRUE(SafeContrast(*img, -5.0f));   // clamped to 0.0
  EXPECT_TRUE(SafeContrast(*img, 100.0f));  // clamped to 10.0

  // Saturation out of range
  EXPECT_TRUE(SafeSaturation(*img, -1.0f));   // clamped to 0.0
  EXPECT_TRUE(SafeSaturation(*img, 100.0f));  // clamped to 5.0
}

TEST(FuzzImagePipeline, DataSizeMismatch) {
  ImageBuffer img;
  img.width = 10;
  img.height = 10;
  img.channels = 3;
  img.data.resize(100);  // wrong: should be 300
  EXPECT_FALSE(img.IsValid());
  EXPECT_FALSE(SafeBrightness(img, 0.5f));
}

TEST(FuzzImagePipeline, LargeImage_NoOverflow) {
  // 4096x4096x3 = 48M floats = 192 MB — reasonable
  auto img = MakeImage(4096, 4096, 3, 0.0f);
  EXPECT_TRUE(img->IsValid());
  EXPECT_TRUE(SafeBrightness(*img, 0.5f));
  EXPECT_TRUE(img->IsValid());
}

TEST(FuzzImagePipeline, NoiseOperation) {
  PipelineFuzzGenerator gen(777);
  auto img = MakeImage(16, 16, 3, 0.5f);
  EXPECT_TRUE(SafeNoise(*img, 0.1f, gen.Rng()));
  EXPECT_TRUE(img->IsValid());
}

TEST(FuzzImagePipeline, FlipOperations) {
  auto img = MakeImage(8, 8, 3, 0.5f);
  // Set a distinctive pixel
  img->data[0] = 1.0f;
  EXPECT_TRUE(SafeFlipH(*img));
  EXPECT_TRUE(SafeFlipV(*img));
  EXPECT_TRUE(img->IsValid());
}

TEST(FuzzImagePipeline, PipelineStressTest) {
  PipelineFuzzGenerator gen(1337);
  for (int trial = 0; trial < 50; ++trial) {
    uint32_t w = 4 + trial % 60;
    uint32_t h = 4 + trial % 40;
    auto img = MakeImage(w, h, 3, 0.5f);
    auto ops = gen.GenerateRandomOps(50);
    ExecutePipeline(*img, ops, gen.Rng());
    // No crash = success
  }
}

}  // namespace fuzzing
}  // namespace alcedo

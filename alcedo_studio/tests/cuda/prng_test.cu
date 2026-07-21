//  Copyright 2026 Yurun Zi
//  SPDX-License-Identifier: GPL-3.0-only
//  Additional permission under GPLv3 section 7 applies; see the LICENSE file.

#include <gtest/gtest.h>

#include <cmath>
#include <cstdint>
#include <stdexcept>
#include <unordered_set>

#include <cuda_runtime.h>

#include "cuda/prng.hpp"

namespace alcedo {
namespace {

__global__ void GeneratePrngSamples(std::uint64_t seed, std::uint64_t* hashes, float* uniforms,
                                    float* normals, std::uint32_t* poisson, int count) {
  const int idx = blockIdx.x * blockDim.x + threadIdx.x;
  if (idx >= count) {
    return;
  }

  const std::uint64_t stream = cuda::PixelStream2D(idx, idx * 3, idx % 3);
  hashes[idx]                = cuda::CounterHash(seed, stream, static_cast<std::uint64_t>(idx));
  uniforms[idx]              = cuda::UniformFloat01(seed, stream, static_cast<std::uint64_t>(idx));
  normals[idx]               = cuda::NormalPair(seed, stream, static_cast<std::uint64_t>(idx)).x;
  poisson[idx]               = cuda::SamplePoisson(
      seed, stream, static_cast<std::uint64_t>(idx) * 11ULL, 4.0f);
}

auto HasCudaDevice() -> bool {
  int count = 0;
  return ::cudaGetDeviceCount(&count) == cudaSuccess && count > 0;
}

template <typename T>
class ManagedArray {
 public:
  explicit ManagedArray(int count) : count_(count) {
    const auto status = ::cudaMallocManaged(&ptr_, sizeof(T) * static_cast<size_t>(count_));
    if (status != cudaSuccess) {
      throw std::runtime_error(cudaGetErrorString(status));
    }
  }

  ManagedArray(const ManagedArray&)            = delete;
  ManagedArray& operator=(const ManagedArray&) = delete;

  ~ManagedArray() {
    if (ptr_ != nullptr) {
      ::cudaFree(ptr_);
    }
  }

  auto get() -> T* { return ptr_; }
  auto operator[](int index) const -> const T& { return ptr_[index]; }

 private:
  T*  ptr_   = nullptr;
  int count_ = 0;
};

}  // namespace

TEST(CudaPrngTest, CounterHashIsDeterministicAndStateless) {
  constexpr std::uint64_t seed    = 0x123456789abcdef0ULL;
  constexpr std::uint64_t stream  = 0x0fedcba987654321ULL;
  constexpr std::uint64_t counter = 42ULL;

  const std::uint64_t first  = cuda::CounterHash(seed, stream, counter);
  const std::uint64_t second = cuda::CounterHash(seed, stream, counter);

  EXPECT_EQ(first, second);
  EXPECT_EQ(cuda::PrngSelfCheckValue(seed), cuda::CounterHash(seed, 0ULL, 0ULL));
  EXPECT_NE(first, cuda::CounterHash(seed, stream + 1ULL, counter));
  EXPECT_NE(first, cuda::CounterHash(seed, stream, counter + 1ULL));
}

TEST(CudaPrngTest, UniformValuesStayInExpectedRange) {
  constexpr std::uint64_t seed = 17ULL;
  float                   sum  = 0.0f;

  for (std::uint64_t i = 0; i < 4096ULL; ++i) {
    const float closed = cuda::UniformFloat01(
        seed, cuda::PixelStream2D(static_cast<std::int32_t>(i),
                                  static_cast<std::int32_t>(i * 7ULL), 0U),
        i);
    const float open = cuda::UniformFloatOpen01(
        seed, cuda::PixelStream2D(static_cast<std::int32_t>(i),
                                  static_cast<std::int32_t>(i * 7ULL), 1U),
        i);

    EXPECT_GE(closed, 0.0f);
    EXPECT_LT(closed, 1.0f);
    EXPECT_GT(open, 0.0f);
    EXPECT_LT(open, 1.0f);
    sum += closed;
  }

  const float mean = sum / 4096.0f;
  EXPECT_GT(mean, 0.47f);
  EXPECT_LT(mean, 0.53f);
}

TEST(CudaPrngTest, StreamsProduceDistinctShortSequences) {
  constexpr std::uint64_t seed = 91ULL;
  std::unordered_set<std::uint64_t> values;

  for (int y = 0; y < 8; ++y) {
    for (int x = 0; x < 8; ++x) {
      for (std::uint32_t channel = 0; channel < 3; ++channel) {
        const std::uint64_t stream = cuda::PixelStream2D(x, y, channel);
        values.insert(cuda::CounterHash(seed, stream, 0ULL));
      }
    }
  }

  EXPECT_EQ(values.size(), 8U * 8U * 3U);
}

TEST(CudaPrngTest, NormalAndPoissonHelpersAreFiniteAndCentered) {
  constexpr std::uint64_t seed = 1234ULL;
  float                   normal_sum = 0.0f;
  float                   poisson_sum = 0.0f;

  for (std::uint64_t i = 0; i < 16384ULL; ++i) {
    const std::uint64_t stream = cuda::PixelStream2D(
        static_cast<std::int32_t>(i & 255ULL), static_cast<std::int32_t>(i >> 8U), 0U);
    const cuda::Float2 normal = cuda::NormalPair(seed, stream, i);
    ASSERT_TRUE(std::isfinite(normal.x));
    ASSERT_TRUE(std::isfinite(normal.y));
    normal_sum += normal.x;
    poisson_sum += static_cast<float>(cuda::SamplePoisson(seed, stream, i * 5ULL, 4.0f));
  }

  const float normal_mean  = normal_sum / 16384.0f;
  const float poisson_mean = poisson_sum / 16384.0f;
  EXPECT_GT(normal_mean, -0.05f);
  EXPECT_LT(normal_mean, 0.05f);
  EXPECT_GT(poisson_mean, 3.85f);
  EXPECT_LT(poisson_mean, 4.15f);
}

TEST(CudaPrngTest, DeviceResultsMatchHostCounterGeneration) {
  if (!HasCudaDevice()) {
    GTEST_SKIP() << "No CUDA device available.";
  }

  constexpr int           kCount = 128;
  constexpr std::uint64_t seed   = 0xfeed12345678abcdULL;
  ManagedArray<std::uint64_t> hashes(kCount);
  ManagedArray<float>         uniforms(kCount);
  ManagedArray<float>         normals(kCount);
  ManagedArray<std::uint32_t> poisson(kCount);

  GeneratePrngSamples<<<(kCount + 31) / 32, 32>>>(
      seed, hashes.get(), uniforms.get(), normals.get(), poisson.get(), kCount);
  ASSERT_EQ(::cudaGetLastError(), cudaSuccess);
  ASSERT_EQ(::cudaDeviceSynchronize(), cudaSuccess);

  for (int idx = 0; idx < kCount; ++idx) {
    const std::uint64_t stream = cuda::PixelStream2D(idx, idx * 3, idx % 3);
    EXPECT_EQ(hashes[idx], cuda::CounterHash(seed, stream, static_cast<std::uint64_t>(idx)));
    EXPECT_EQ(uniforms[idx], cuda::UniformFloat01(seed, stream, static_cast<std::uint64_t>(idx)));
    EXPECT_NEAR(normals[idx], cuda::NormalPair(seed, stream, static_cast<std::uint64_t>(idx)).x,
                1.0e-5f);
    EXPECT_EQ(poisson[idx],
              cuda::SamplePoisson(seed, stream, static_cast<std::uint64_t>(idx) * 11ULL, 4.0f));
  }
}

}  // namespace alcedo

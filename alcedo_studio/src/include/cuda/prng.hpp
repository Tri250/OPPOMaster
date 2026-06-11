//  Copyright 2026 Yurun Zi
//  SPDX-License-Identifier: GPL-3.0-only
//  Additional permission under GPLv3 section 7 applies; see the LICENSE file.

#pragma once

#if defined(__CUDACC__)
#include <cuda_runtime.h>
#endif

#include <cmath>
#include <cstdint>

#if defined(__CUDACC__)
#define ALCEDO_CUDA_HOST_DEVICE __host__ __device__ inline
#else
#define ALCEDO_CUDA_HOST_DEVICE inline
#endif

namespace alcedo::cuda {

struct Float2 {
  float x = 0.0f;
  float y = 0.0f;
};

ALCEDO_CUDA_HOST_DEVICE auto Mix64(std::uint64_t value) -> std::uint64_t {
  value = (value ^ (value >> 30U)) * 0xbf58476d1ce4e5b9ULL;
  value = (value ^ (value >> 27U)) * 0x94d049bb133111ebULL;
  return value ^ (value >> 31U);
}

ALCEDO_CUDA_HOST_DEVICE auto CounterHash(std::uint64_t seed, std::uint64_t stream,
                                         std::uint64_t counter) -> std::uint64_t {
  std::uint64_t value = seed + 0x9e3779b97f4a7c15ULL;
  value ^= Mix64(stream + 0xd1b54a32d192ed03ULL);
  value += counter * 0x9e3779b97f4a7c15ULL;
  return Mix64(value);
}

ALCEDO_CUDA_HOST_DEVICE auto PixelStream2D(std::int32_t x, std::int32_t y,
                                           std::uint32_t channel) -> std::uint64_t {
  std::uint64_t value = static_cast<std::uint32_t>(x);
  value |= static_cast<std::uint64_t>(static_cast<std::uint32_t>(y)) << 32U;
  value ^= static_cast<std::uint64_t>(channel) * 0x9e3779b97f4a7c15ULL;
  return Mix64(value);
}

ALCEDO_CUDA_HOST_DEVICE auto UniformUint32(std::uint64_t seed, std::uint64_t stream,
                                           std::uint64_t counter) -> std::uint32_t {
  return static_cast<std::uint32_t>(CounterHash(seed, stream, counter) >> 32U);
}

ALCEDO_CUDA_HOST_DEVICE auto UniformFloat01FromBits(std::uint64_t bits) -> float {
  constexpr float kInv24 = 1.0f / 16777216.0f;
  const auto      mantissa =
      static_cast<std::uint32_t>((bits >> 40U) & static_cast<std::uint64_t>(0x00ffffffU));
  return static_cast<float>(mantissa) * kInv24;
}

ALCEDO_CUDA_HOST_DEVICE auto UniformFloatOpen01FromBits(std::uint64_t bits) -> float {
  constexpr float kInv24 = 1.0f / 16777216.0f;
  const auto      mantissa =
      static_cast<std::uint32_t>((bits >> 40U) & static_cast<std::uint64_t>(0x00ffffffU));
  return (static_cast<float>(mantissa) + 0.5f) * kInv24;
}

ALCEDO_CUDA_HOST_DEVICE auto UniformFloat01(std::uint64_t seed, std::uint64_t stream,
                                            std::uint64_t counter) -> float {
  return UniformFloat01FromBits(CounterHash(seed, stream, counter));
}

ALCEDO_CUDA_HOST_DEVICE auto UniformFloatOpen01(std::uint64_t seed, std::uint64_t stream,
                                                std::uint64_t counter) -> float {
  return UniformFloatOpen01FromBits(CounterHash(seed, stream, counter));
}

ALCEDO_CUDA_HOST_DEVICE auto NormalPair(std::uint64_t seed, std::uint64_t stream,
                                        std::uint64_t counter) -> Float2 {
  constexpr float kTwoPi = 6.28318530717958647692f;
  const float     u1     = UniformFloatOpen01(seed, stream, counter * 2ULL);
  const float     u2     = UniformFloatOpen01(seed, stream, counter * 2ULL + 1ULL);
  const float     radius = ::sqrtf(-2.0f * ::logf(u1));
  const float     angle  = kTwoPi * u2;
  return Float2{radius * ::cosf(angle), radius * ::sinf(angle)};
}

ALCEDO_CUDA_HOST_DEVICE auto SamplePoisson(std::uint64_t seed, std::uint64_t stream,
                                           std::uint64_t counter, float lambda) -> std::uint32_t {
  if (!(lambda > 0.0f)) {
    return 0;
  }

  if (lambda < 30.0f) {
    constexpr std::uint32_t kMaxInversionSteps = 256U;
    const float             limit              = ::expf(-lambda);
    float                   product            = 1.0f;
    std::uint32_t           k                  = 0U;
    do {
      ++k;
      product *= UniformFloatOpen01(seed, stream, counter + static_cast<std::uint64_t>(k));
    } while (product > limit && k < kMaxInversionSteps);
    return k - 1U;
  }

  const float gaussian = NormalPair(seed, stream, counter).x;
  const float sample   = lambda + ::sqrtf(lambda) * gaussian + 0.5f;
  if (!(sample > 0.0f)) {
    return 0U;
  }
  return static_cast<std::uint32_t>(sample);
}

auto PrngSelfCheckValue(std::uint64_t seed) -> std::uint64_t;

}  // namespace alcedo::cuda

#undef ALCEDO_CUDA_HOST_DEVICE

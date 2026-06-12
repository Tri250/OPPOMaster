//  Copyright 2026 Yurun Zi
//  SPDX-License-Identifier: GPL-3.0-only
//  Additional permission under GPLv3 section 7 applies; see the LICENSE file.

#ifndef ALCEDO_OPENCL_PRNG_CL
#define ALCEDO_OPENCL_PRNG_CL

typedef struct {
  float x;
  float y;
} OpenClPrngFloat2;

static inline ulong opencl_prng_mix64(ulong value) {
  value = (value ^ (value >> 30u)) * 0xbf58476d1ce4e5b9UL;
  value = (value ^ (value >> 27u)) * 0x94d049bb133111ebUL;
  return value ^ (value >> 31u);
}

static inline ulong opencl_prng_counter_hash(ulong seed, ulong stream, ulong counter) {
  ulong value = seed + 0x9e3779b97f4a7c15UL;
  value ^= opencl_prng_mix64(stream + 0xd1b54a32d192ed03UL);
  value += counter * 0x9e3779b97f4a7c15UL;
  return opencl_prng_mix64(value);
}

static inline ulong opencl_prng_pixel_stream_2d(int x, int y, uint channel) {
  ulong value = (ulong)((uint)x);
  value |= ((ulong)((uint)y)) << 32u;
  value ^= ((ulong)channel) * 0x9e3779b97f4a7c15UL;
  return opencl_prng_mix64(value);
}

static inline uint opencl_prng_uniform_uint32(ulong seed, ulong stream, ulong counter) {
  return (uint)(opencl_prng_counter_hash(seed, stream, counter) >> 32u);
}

static inline float opencl_prng_uniform_float01_from_bits(ulong bits) {
  const uint mantissa = (uint)((bits >> 40u) & (ulong)0x00ffffffu);
  return (float)mantissa * (1.0f / 16777216.0f);
}

static inline float opencl_prng_uniform_float_open01_from_bits(ulong bits) {
  const uint mantissa = (uint)((bits >> 40u) & (ulong)0x00ffffffu);
  return ((float)mantissa + 0.5f) * (1.0f / 16777216.0f);
}

static inline float opencl_prng_uniform_float01(ulong seed, ulong stream, ulong counter) {
  return opencl_prng_uniform_float01_from_bits(opencl_prng_counter_hash(seed, stream, counter));
}

static inline float opencl_prng_uniform_float_open01(ulong seed, ulong stream, ulong counter) {
  return opencl_prng_uniform_float_open01_from_bits(
      opencl_prng_counter_hash(seed, stream, counter));
}

static inline OpenClPrngFloat2 opencl_prng_normal_pair(ulong seed, ulong stream, ulong counter) {
  const float two_pi = 6.28318530717958647692f;
  const float u1 = opencl_prng_uniform_float_open01(seed, stream, counter * 2UL);
  const float u2 = opencl_prng_uniform_float_open01(seed, stream, counter * 2UL + 1UL);
  const float radius = sqrt(-2.0f * log(u1));
  const float angle = two_pi * u2;
  OpenClPrngFloat2 out;
  out.x = radius * cos(angle);
  out.y = radius * sin(angle);
  return out;
}

static inline uint opencl_prng_sample_poisson(ulong seed,
                                              ulong stream,
                                              ulong counter,
                                              float lambda) {
  if (!(lambda > 0.0f)) {
    return 0u;
  }

  if (lambda < 30.0f) {
    const uint max_inversion_steps = 256u;
    const float limit = exp(-lambda);
    float product = 1.0f;
    uint k = 0u;
    do {
      ++k;
      product *= opencl_prng_uniform_float_open01(seed, stream, counter + (ulong)k);
    } while (product > limit && k < max_inversion_steps);
    return k - 1u;
  }

  const float gaussian = opencl_prng_normal_pair(seed, stream, counter).x;
  const float sample = lambda + sqrt(lambda) * gaussian + 0.5f;
  if (!(sample > 0.0f)) {
    return 0u;
  }
  return (uint)sample;
}

#endif  // ALCEDO_OPENCL_PRNG_CL

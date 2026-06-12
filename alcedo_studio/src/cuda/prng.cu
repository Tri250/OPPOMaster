//  Copyright 2026 Yurun Zi
//  SPDX-License-Identifier: GPL-3.0-only
//  Additional permission under GPLv3 section 7 applies; see the LICENSE file.

#include "cuda/prng.hpp"

namespace alcedo::cuda {

auto PrngSelfCheckValue(std::uint64_t seed) -> std::uint64_t {
  return CounterHash(seed, 0ULL, 0ULL);
}

}  // namespace alcedo::cuda

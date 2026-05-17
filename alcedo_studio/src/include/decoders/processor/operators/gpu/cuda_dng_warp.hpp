//  Copyright 2026 Yurun Zi
//  SPDX-License-Identifier: GPL-3.0-only
//  Additional permission under GPLv3 section 7 applies; see the LICENSE file.

#pragma once

#include <opencv2/core/cuda.hpp>

#include "decoders/dng_default_crop.hpp"

namespace alcedo {
namespace CUDA {

void ApplyDngWarpRectilinear(cv::cuda::GpuMat& img, const dng::WarpRectilinear& warp,
                             cv::cuda::Stream* stream = nullptr);

}  // namespace CUDA
}  // namespace alcedo

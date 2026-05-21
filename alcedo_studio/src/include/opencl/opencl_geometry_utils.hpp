//  Copyright 2026 Yurun Zi
//  SPDX-License-Identifier: GPL-3.0-only
//  Additional permission under GPLv3 section 7 applies; see the LICENSE file.

#pragma once

#ifdef HAVE_OPENCL

#include <opencv2/core.hpp>

#include "decoders/dng_default_crop.hpp"
#include "edit/operators/geometry/resize_algorithm.hpp"
#include "image/opencl_image.hpp"

namespace alcedo::OpenCL::Geometry {

void Resize(const opencl::OpenClImage& src, opencl::OpenClImage& dst, cv::Size dst_size,
            ResizeDownsampleAlgorithm downsample_algorithm = ResizeDownsampleAlgorithm::Area);

void CropResize(const opencl::OpenClImage& src, opencl::OpenClImage& dst, const cv::Rect& crop_rect,
                cv::Size                  dst_size,
                ResizeDownsampleAlgorithm downsample_algorithm = ResizeDownsampleAlgorithm::Area);

void ResizeAreaApprox(const opencl::OpenClImage& src, opencl::OpenClImage& dst, cv::Size dst_size);

void ResizeLinear(const opencl::OpenClImage& src, opencl::OpenClImage& dst, cv::Size dst_size);

void WarpAffineLinear(const opencl::OpenClImage& src, opencl::OpenClImage& dst,
                      const cv::Mat& matrix, cv::Size out_size, const cv::Scalar& border_value);

void WarpRectilinear(const opencl::OpenClImage& src, opencl::OpenClImage& dst,
                     const dng::WarpRectilinear& warp);

void Rotate180(opencl::OpenClImage& image);

void Rotate90CW(opencl::OpenClImage& image);

void Rotate90CCW(opencl::OpenClImage& image);

}  // namespace alcedo::OpenCL::Geometry

#endif

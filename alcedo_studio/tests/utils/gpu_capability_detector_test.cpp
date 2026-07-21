//  Copyright 2026 Yurun Zi
//  SPDX-License-Identifier: GPL-3.0-only
//  Additional permission under GPLv3 section 7 applies; see the LICENSE file.

#include <gtest/gtest.h>

#include "utils/gpu/gpu_capability_detector.hpp"

namespace alcedo::gpu {
namespace {

TEST(GpuCapabilityDetectorTest, DetectReturnsValidCapabilityInfo) {
  auto info = GpuCapabilityDetector::Detect();
  // The capability level should be one of the valid enum values
  EXPECT_TRUE(info.IsFull() || info.IsLimited() || info.IsSoftwareOnly());

  // The recommended backend should be consistent with the capability level
  if (info.IsFull()) {
    EXPECT_NE(info.recommended_backend, GpuBackendKind::None);
  }
  if (info.IsSoftwareOnly()) {
    EXPECT_EQ(info.recommended_backend, GpuBackendKind::None);
  }
}

TEST(GpuCapabilityDetectorTest, CapabilityLevelConsistency) {
  auto info = GpuCapabilityDetector::Detect();
  // Exactly one capability flag should be set
  int flags = (info.IsFull() ? 1 : 0) + (info.IsLimited() ? 1 : 0) + (info.IsSoftwareOnly() ? 1 : 0);
  EXPECT_EQ(flags, 1);
}

TEST(GpuCapabilityDetectorTest, BuildWarningMessageNotEmptyForLimited) {
  auto info = GpuCapabilityDetector::Detect();
  if (info.IsLimited() || info.IsSoftwareOnly()) {
    auto msg = GpuCapabilityDetector::BuildDriverWarningMessage(info);
    EXPECT_FALSE(msg.empty());
  }
}

TEST(GpuCapabilityDetectorTest, DriverWarningSuppressionRoundTrip) {
  // Note: This test may affect persistent settings in CI environments.
  // Save and restore the original value.
  bool original = GpuCapabilityDetector::IsDriverWarningSuppressed();

  GpuCapabilityDetector::SetDriverWarningSuppressed(true);
  EXPECT_TRUE(GpuCapabilityDetector::IsDriverWarningSuppressed());

  GpuCapabilityDetector::SetDriverWarningSuppressed(false);
  EXPECT_FALSE(GpuCapabilityDetector::IsDriverWarningSuppressed());

  // Restore original
  GpuCapabilityDetector::SetDriverWarningSuppressed(original);
}

TEST(GpuCapabilityDetectorTest, GpuCapabilityInfoFlags) {
  GpuCapabilityInfo full_info;
  full_info.capability_level = GpuCapabilityLevel::Full;
  EXPECT_TRUE(full_info.IsFull());
  EXPECT_FALSE(full_info.IsLimited());
  EXPECT_FALSE(full_info.IsSoftwareOnly());

  GpuCapabilityInfo limited_info;
  limited_info.capability_level = GpuCapabilityLevel::Limited;
  EXPECT_FALSE(limited_info.IsFull());
  EXPECT_TRUE(limited_info.IsLimited());
  EXPECT_FALSE(limited_info.IsSoftwareOnly());

  GpuCapabilityInfo sw_info;
  sw_info.capability_level = GpuCapabilityLevel::SoftwareOnly;
  EXPECT_FALSE(sw_info.IsFull());
  EXPECT_FALSE(sw_info.IsLimited());
  EXPECT_TRUE(sw_info.IsSoftwareOnly());
}

}  // namespace
}  // namespace alcedo::gpu

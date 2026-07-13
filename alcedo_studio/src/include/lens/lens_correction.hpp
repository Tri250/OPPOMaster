//  Copyright 2026 Yurun Zi
//  SPDX-License-Identifier: GPL-3.0-only
//  Additional permission under GPLv3 section 7 applies; see the LICENSE file.

#pragma once

#include <array>
#include <cmath>
#include <cstdint>
#include <filesystem>
#include <functional>
#include <memory>
#include <optional>
#include <stdexcept>
#include <string>
#include <string_view>
#include <unordered_map>
#include <vector>

namespace alcedo::lens {

// ─────────────────────────────────────────────────────────────────────────────
//  Error types
// ─────────────────────────────────────────────────────────────────────────────

class LensError : public std::runtime_error {
 public:
  explicit LensError(const std::string& msg) : std::runtime_error(msg) {}
};

class DatabaseError : public LensError {
 public:
  explicit DatabaseError(const std::string& msg) : LensError(msg) {}
};

class LensNotFoundError : public LensError {
 public:
  explicit LensNotFoundError(const std::string& msg) : LensError(msg) {}
};

class DimensionMismatchError : public LensError {
 public:
  explicit DimensionMismatchError(const std::string& msg) : LensError(msg) {}
};

// ─────────────────────────────────────────────────────────────────────────────
//  Correction model enums
// ─────────────────────────────────────────────────────────────────────────────

enum class DistortionModel : std::int32_t {
  None   = 0,
  Poly3  = 1,
  Poly5  = 2,
  PtLens = 3,
};

enum class TcaModel : std::int32_t {
  None   = 0,
  Linear = 1,
  Poly3  = 2,
};

enum class VignettingModel : std::int32_t {
  None = 0,
  PA   = 1,
};

enum class ProjectionType : std::int32_t {
  Unknown              = 0,
  Rectilinear          = 1,
  Fisheye              = 2,
  Panoramic            = 3,
  Equirectangular       = 4,
  FisheyeOrthographic  = 5,
  FisheyeStereographic = 6,
  FisheyeEquisolid     = 7,
  FisheyeThoby         = 8,
};

// ─────────────────────────────────────────────────────────────────────────────
//  Calibration data structures
// ─────────────────────────────────────────────────────────────────────────────

struct DistortionCalib {
  DistortionModel model   = DistortionModel::None;
  float           focal   = 0.0f;   // focal length this calibration applies to
  float           real_focal = 0.0f;
  float           k1 = 0.0f, k2 = 0.0f;          // poly3 / poly5
  float           a = 0.0f, b = 0.0f, c = 0.0f;  // ptlens
};

struct TcaCalib {
  TcaModel model = TcaModel::None;
  float    focal = 0.0f;
  // linear
  float vr = 1.0f, vb = 1.0f;  // linear: red/blue scaling
  // poly3
  float cr = 0.0f, cb = 0.0f;  // cubic
  float br = 0.0f, bb = 0.0f;  // quadratic
};

struct VignettingCalib {
  VignettingModel model    = VignettingModel::None;
  float           focal    = 0.0f;
  float           aperture = 0.0f;
  float           distance = 1000.0f;
  float           k1 = 0.0f, k2 = 0.0f, k3 = 0.0f;
};

// ─────────────────────────────────────────────────────────────────────────────
//  Camera entry
// ─────────────────────────────────────────────────────────────────────────────

struct CameraEntry {
  std::string maker;
  std::string model;
  std::string mount;
  float       crop_factor = 1.0f;
};

// ─────────────────────────────────────────────────────────────────────────────
//  Lens entry
// ─────────────────────────────────────────────────────────────────────────────

struct LensEntry {
  std::string                  maker;
  std::string                  model;
  std::string                  mount;
  float                        crop_factor    = 1.0f;
  float                        min_focal      = 0.0f;
  float                        max_focal      = 0.0f;
  float                        min_aperture   = 0.0f;
  float                        max_aperture   = 0.0f;
  ProjectionType               projection     = ProjectionType::Unknown;
  float                        center_x       = 0.0f;
  float                        center_y       = 0.0f;
  std::vector<DistortionCalib> distortions;
  std::vector<TcaCalib>        tca_entries;
  std::vector<VignettingCalib> vignetting_entries;
};

// ─────────────────────────────────────────────────────────────────────────────
//  Lens match result
// ─────────────────────────────────────────────────────────────────────────────

struct LensMatchResult {
  const CameraEntry* camera       = nullptr;
  const LensEntry*   lens         = nullptr;
  float              crop_factor  = 1.0f;
  bool               valid        = false;
};

// ─────────────────────────────────────────────────────────────────────────────
//  Interpolated correction parameters (for a specific focal/aperture/distance)
// ─────────────────────────────────────────────────────────────────────────────

struct CorrectionParams {
  DistortionModel  distortion_model  = DistortionModel::None;
  std::array<float, 5> distortion_terms = {};  // k1,k2 or a,b,c
  float            real_focal_mm     = 0.0f;

  TcaModel         tca_model         = TcaModel::None;
  std::array<float, 6> tca_terms     = {};  // vr,vb or cr,cb,br,bb

  VignettingModel  vignetting_model  = VignettingModel::None;
  std::array<float, 3> vignetting_terms = {};

  float            crop_factor       = 1.0f;
  float            lens_center_x     = 0.0f;
  float            lens_center_y     = 0.0f;
};

// ─────────────────────────────────────────────────────────────────────────────
//  Lens database (Lensfun XML format)
// ─────────────────────────────────────────────────────────────────────────────

class LensDatabase {
 public:
  LensDatabase();
  ~LensDatabase();

  // Non-copyable, movable
  LensDatabase(const LensDatabase&)            = delete;
  LensDatabase& operator=(const LensDatabase&) = delete;
  LensDatabase(LensDatabase&&) noexcept;
  LensDatabase& operator=(LensDatabase&&) noexcept;

  /// Load all XML files from a directory containing Lensfun-format XML files.
  /// @param db_path  Path to the directory.
  /// @throws DatabaseError if the directory is invalid or no files parse.
  void LoadDirectory(const std::filesystem::path& db_path);

  /// Load a single XML file.
  /// @throws DatabaseError if the file cannot be parsed.
  void LoadFile(const std::filesystem::path& file_path);

  /// Find a camera by maker and model name (loose match).
  auto FindCamera(const std::string& maker, const std::string& model) const
      -> const CameraEntry*;

  /// Find lenses matching a maker and model (loose match).
  auto FindLenses(const std::string& maker, const std::string& model) const
      -> std::vector<const LensEntry*>;

  /// Find the best lens for a given camera and input metadata.
  auto MatchLens(const std::string& cam_maker,
                 const std::string& cam_model,
                 const std::string& lens_maker,
                 const std::string& lens_model,
                 float              focal_length_mm,
                 float              aperture_f_number) const -> LensMatchResult;

  /// Get the total number of loaded cameras.
  auto CameraCount() const -> std::size_t;

  /// Get the total number of loaded lenses.
  auto LensCount() const -> std::size_t;

  /// Check if the database has been successfully loaded.
  auto IsValid() const -> bool;

 private:
  struct Impl;
  std::unique_ptr<Impl> impl_;
};

// ─────────────────────────────────────────────────────────────────────────────
//  Lens corrector (CPU-side correction engine)
// ─────────────────────────────────────────────────────────────────────────────

class LensCorrector {
 public:
  /// Configuration flags controlling which corrections are applied.
  struct Config {
    bool apply_distortion = true;
    bool apply_tca        = true;
    bool apply_vignetting = true;
    bool auto_scale       = true;
    float user_scale      = 1.0f;
    int  num_threads      = 0;  // 0 = use hardware concurrency
  };

  LensCorrector();
  ~LensCorrector();

  // Non-copyable, movable
  LensCorrector(const LensCorrector&)            = delete;
  LensCorrector& operator=(const LensCorrector&) = delete;
  LensCorrector(LensCorrector&&) noexcept;
  LensCorrector& operator=(LensCorrector&&) noexcept;

  /// Set the lens database to use for lookups.
  void SetDatabase(std::shared_ptr<const LensDatabase> db);

  /// Set the correction configuration.
  void SetConfig(const Config& config);

  /// Interpolate correction parameters for a specific focal/aperture/distance.
  auto InterpolateParams(const LensEntry& lens,
                         float            focal_mm,
                         float            aperture,
                         float            distance_m) const -> CorrectionParams;

  /// Apply all enabled corrections to an image (float RGB, planar layout).
  /// @param image    Pointer to image data (H*W*3 floats, RGB planar: R then G then B).
  /// @param width    Image width in pixels.
  /// @param height   Image height in pixels.
  /// @param params   Pre-computed correction parameters.
  /// @param config   Correction configuration.
  void ApplyCorrection(float*                 image,
                       int                    width,
                       int                    height,
                       const CorrectionParams& params,
                       const Config&           config) const;

  /// Undistort a single normalized pixel coordinate.
  /// @param xu       Input normalized x (will be replaced with undistorted x).
  /// @param yu       Input normalized y (will be replaced with undistorted y).
  /// @param params   Correction parameters.
  /// @param backward If true, map from distorted→undistorted (for remapping).
  static void UndistortNormalized(float& xu, float& yu,
                                  const CorrectionParams& params,
                                  bool backward = false);

  /// Compute the auto-scale factor for a given lens/params/image combination.
  static auto ComputeAutoScale(const CorrectionParams& params,
                               int width, int height) -> float;

 private:
  struct Impl;
  std::unique_ptr<Impl> impl_;
};

// ─────────────────────────────────────────────────────────────────────────────
//  Convenience free functions
// ─────────────────────────────────────────────────────────────────────────────

/// Load a Lensfun database from a directory.
auto LoadLensDatabase(const std::filesystem::path& db_path) -> std::shared_ptr<LensDatabase>;

/// Apply lens correction to a float RGB image using the given database and metadata.
/// This is a one-shot convenience function.
/// @throws DatabaseError, LensNotFoundError, DimensionMismatchError
void CorrectImage(float*                        image,
                  int                           width,
                  int                           height,
                  const LensDatabase&           db,
                  const std::string&            cam_maker,
                  const std::string&            cam_model,
                  const std::string&            lens_maker,
                  const std::string&            lens_model,
                  float                         focal_length_mm,
                  float                         aperture_f_number,
                  const LensCorrector::Config&  config = {});

}  // namespace alcedo::lens
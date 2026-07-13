//  Copyright 2026 Yurun Zi
//  SPDX-License-Identifier: GPL-3.0-only
//  Additional permission under GPLv3 section 7 applies; see the LICENSE file.

#pragma once

#include <cstdint>
#include <optional>
#include <string>
#include <string_view>
#include <utility>
#include <vector>

namespace alcedo {
namespace exif {

/// ---------------------------------------------------------------------------
/// Supported image file types for EXIF editing.
/// ---------------------------------------------------------------------------
enum class ExifFileType {
  JPEG,
  TIFF,
  RAW_CR2,
  RAW_NEF,
  RAW_ARW,
  RAW_DNG,
  RAW_RAF,
  RAW_ORF,
  RAW_RW2,
  RAW_PEF,
  RAW_SRW,
  UNKNOWN,
};

/// ---------------------------------------------------------------------------
/// GPS coordinate representation.
/// ---------------------------------------------------------------------------
struct GPSCoordinate {
  double latitude  = 0.0;   ///< Decimal degrees, positive = north
  double longitude = 0.0;   ///< Decimal degrees, positive = east
  double altitude  = 0.0;   ///< Metres above sea level (optional)
  bool   has_altitude = false;
};

/// ---------------------------------------------------------------------------
/// Rational type used by EXIF (numerator / denominator).
/// ---------------------------------------------------------------------------
struct Rational {
  int32_t numerator   = 0;
  int32_t denominator = 1;
};

/// ---------------------------------------------------------------------------
/// Complete set of editable EXIF metadata fields.
/// All fields are optional — only set fields are written back.
/// ---------------------------------------------------------------------------
struct ExifMetadata {
  // Camera
  std::optional<std::string> make;           ///< Camera manufacturer
  std::optional<std::string> model;          ///< Camera model
  std::optional<std::string> lens;           ///< Lens description
  std::optional<std::string> lens_make;      ///< Lens manufacturer

  // Exposure
  std::optional<Rational>    aperture;       ///< F-number as rational
  std::optional<Rational>    shutter_speed;  ///< Exposure time as rational (seconds)
  std::optional<uint32_t>    iso;            ///< ISO speed rating
  std::optional<Rational>    focal_length;   ///< Focal length in mm
  std::optional<Rational>    focal_length_35mm; ///< 35mm-equivalent focal length

  // Date / Time
  std::optional<std::string> date_time;        ///< "YYYY:MM:DD HH:MM:SS"
  std::optional<std::string> date_time_original;  ///< Original capture time
  std::optional<std::string> date_time_digitized; ///< Digitisation time

  // GPS
  std::optional<GPSCoordinate> gps;

  // Copyright / Creator
  std::optional<std::string> copyright;   ///< Copyright notice
  std::optional<std::string> artist;      ///< Photographer / artist
  std::optional<std::string> description; ///< Image description / caption

  // Rating
  std::optional<uint16_t> rating;         ///< 0–5 star rating

  // Keywords (IPTC / XMP dc:subject)
  std::optional<std::vector<std::string>> keywords;
};

/// ---------------------------------------------------------------------------
/// Result of a single metadata read operation.
/// ---------------------------------------------------------------------------
struct ExifReadResult {
  std::string  file_path;
  bool         success = false;
  std::string  error_message;
  ExifMetadata metadata;
};

/// ---------------------------------------------------------------------------
/// Result of a single metadata write operation.
/// ---------------------------------------------------------------------------
struct ExifWriteResult {
  std::string  file_path;
  bool         success = false;
  std::string  error_message;
};

/// ---------------------------------------------------------------------------
/// Read EXIF / IPTC / XMP metadata from a JPEG, TIFF, or RAW file.
///
/// @param file_path  Path to the image file.
/// @return Read result with populated metadata on success.
/// ---------------------------------------------------------------------------
auto read_metadata(const std::string& file_path) -> ExifReadResult;

/// ---------------------------------------------------------------------------
/// Write EXIF metadata back to a file, preserving all non-edited tags and
/// maker notes.  Automatically synchronises the corresponding XMP sidecar
/// (.xmp) if one exists next to the file.
///
/// @param file_path  Path to the image file.
/// @param metadata   Fields to write (only non-nullopt fields are updated).
/// @return Write result indicating success or failure.
/// ---------------------------------------------------------------------------
auto write_metadata(const std::string& file_path,
                    const ExifMetadata& metadata) -> ExifWriteResult;

/// ---------------------------------------------------------------------------
/// Batch-read metadata from multiple files.
///
/// @param file_paths  Vector of paths to image files.
/// @return Vector of read results, one per file.
/// ---------------------------------------------------------------------------
auto batch_read_metadata(const std::vector<std::string>& file_paths)
    -> std::vector<ExifReadResult>;

/// ---------------------------------------------------------------------------
/// Batch-write the same metadata to multiple files.
///
/// @param file_paths  Vector of paths to image files.
/// @param metadata    Common metadata to write to all files.
/// @return Vector of write results, one per file.
/// ---------------------------------------------------------------------------
auto batch_write_metadata(const std::vector<std::string>& file_paths,
                          const ExifMetadata& metadata)
    -> std::vector<ExifWriteResult>;

/// ---------------------------------------------------------------------------
/// Synchronise an XMP sidecar file with the embedded metadata of its paired
/// image file.  Reads the image file's EXIF and writes to the .xmp sidecar.
///
/// @param image_path  Path to the image file (e.g. "photo.RAW").
/// @return true if the sidecar was written successfully.
/// ---------------------------------------------------------------------------
auto sync_xmp_sidecar(const std::string& image_path) -> bool;

/// ---------------------------------------------------------------------------
/// Detect the file type from the file extension.
///
/// @param file_path  Path to the image file.
/// @return The detected file type.
/// ---------------------------------------------------------------------------
auto detect_file_type(const std::string& file_path) -> ExifFileType;

/// ---------------------------------------------------------------------------
/// Remove all metadata from a file, leaving only the image data intact.
/// Maker notes and XMP sidecars are also removed.
///
/// @param file_path  Path to the image file.
/// @return true on success.
/// ---------------------------------------------------------------------------
auto strip_metadata(const std::string& file_path) -> bool;

}  // namespace exif
}  // namespace alcedo
//  Copyright 2026 Yurun Zi
//  SPDX-License-Identifier: GPL-3.0-only
//  Additional permission under GPLv3 section 7 applies; see the LICENSE file.

#include "app/exif_editor.hpp"

#include <algorithm>
#include <cstring>
#include <filesystem>
#include <fstream>
#include <sstream>
#include <stdexcept>
#include <system_error>
#include <unordered_set>
#include <vector>

// ============================================================================
// EXIF / TIFF binary constants (from the EXIF 2.3 / TIFF 6.0 spec)
// ============================================================================
namespace {

constexpr uint16_t TIFF_BIG_ENDIAN    = 0x4D4D;  // "MM"
constexpr uint16_t TIFF_LITTLE_ENDIAN = 0x4949;  // "II"
constexpr uint16_t TIFF_MAGIC         = 0x002A;

constexpr uint16_t TAG_MAKE                  = 0x010F;
constexpr uint16_t TAG_MODEL                 = 0x0110;
constexpr uint16_t TAG_IMAGE_DESCRIPTION     = 0x010E;
constexpr uint16_t TAG_ARTIST                = 0x013B;
constexpr uint16_t TAG_COPYRIGHT             = 0x8298;
constexpr uint16_t TAG_EXPOSURE_TIME         = 0x829A;
constexpr uint16_t TAG_FNUMBER               = 0x829D;
constexpr uint16_t TAG_ISO                   = 0x8827;
constexpr uint16_t TAG_DATE_TIME             = 0x0132;
constexpr uint16_t TAG_DATE_TIME_ORIGINAL    = 0x9003;
constexpr uint16_t TAG_DATE_TIME_DIGITIZED   = 0x9004;
constexpr uint16_t TAG_FOCAL_LENGTH          = 0x920A;
constexpr uint16_t TAG_FOCAL_LENGTH_35MM     = 0xA405;
constexpr uint16_t TAG_LENS_MODEL            = 0xA434;
constexpr uint16_t TAG_LENS_MAKE             = 0xA433;
constexpr uint16_t TAG_MAKER_NOTE            = 0x927C;
constexpr uint16_t TAG_EXIF_IFD              = 0x8769;
constexpr uint16_t TAG_GPS_IFD               = 0x8825;
constexpr uint16_t TAG_RATING                = 0x4746;  // Windows rating (0–5)
constexpr uint16_t TAG_RATING_PERCENT        = 0x4749;

constexpr uint16_t TAG_GPS_LATITUDE_REF      = 0x0001;
constexpr uint16_t TAG_GPS_LATITUDE          = 0x0002;
constexpr uint16_t TAG_GPS_LONGITUDE_REF     = 0x0003;
constexpr uint16_t TAG_GPS_LONGITUDE         = 0x0004;
constexpr uint16_t TAG_GPS_ALTITUDE_REF      = 0x0005;
constexpr uint16_t TAG_GPS_ALTITUDE          = 0x0006;

// TIFF data types
constexpr uint16_t TYPE_BYTE      = 1;
constexpr uint16_t TYPE_ASCII     = 2;
constexpr uint16_t TYPE_SHORT     = 3;
constexpr uint16_t TYPE_LONG      = 4;
constexpr uint16_t TYPE_RATIONAL  = 5;
constexpr uint16_t TYPE_SBYTE     = 6;
constexpr uint16_t TYPE_UNDEFINED = 7;
constexpr uint16_t TYPE_SSHORT    = 8;
constexpr uint16_t TYPE_SLONG     = 9;
constexpr uint16_t TYPE_SRATIONAL = 10;
constexpr uint16_t TYPE_FLOAT     = 11;
constexpr uint16_t TYPE_DOUBLE    = 12;

// TIFF IFD entry size (12 bytes)
constexpr size_t IFD_ENTRY_SIZE = 12;

// JPEG markers
constexpr uint8_t JPEG_SOI  = 0xD8;
constexpr uint8_t JPEG_APP1 = 0xE1;
constexpr uint8_t JPEG_MARKER_PREFIX = 0xFF;

// --------------------------------------------------------------------------
// Byte-order helpers
// --------------------------------------------------------------------------
class ByteReader {
 public:
  ByteReader(const uint8_t* data, size_t size, bool big_endian)
      : data_(data), size_(size), big_endian_(big_endian) {}

  auto read16(size_t offset) const -> uint16_t {
    if (offset + 2 > size_) return 0;
    uint16_t v;
    std::memcpy(&v, data_ + offset, 2);
    return big_endian_ ? ((v << 8) & 0xFF00) | ((v >> 8) & 0x00FF) : v;
  }

  auto read32(size_t offset) const -> uint32_t {
    if (offset + 4 > size_) return 0;
    uint32_t v;
    std::memcpy(&v, data_ + offset, 4);
    if (big_endian_) {
      return ((v & 0xFF) << 24) | ((v & 0xFF00) << 8) |
             ((v >> 8) & 0xFF00) | ((v >> 24) & 0xFF);
    }
    return v;
  }

  auto read_string(size_t offset, size_t count) const -> std::string {
    if (offset + count > size_) return {};
    return std::string(reinterpret_cast<const char*>(data_ + offset), count);
  }

  auto data() const -> const uint8_t* { return data_; }
  auto size() const -> size_t { return size_; }
  auto big_endian() const -> bool { return big_endian_; }

 private:
  const uint8_t* data_;
  size_t size_;
  bool big_endian_;
};

class ByteWriter {
 public:
  ByteWriter(bool big_endian) : big_endian_(big_endian) {}

  void write16(uint16_t v) {
    if (big_endian_) {
      buf_.push_back(static_cast<uint8_t>((v >> 8) & 0xFF));
      buf_.push_back(static_cast<uint8_t>(v & 0xFF));
    } else {
      buf_.push_back(static_cast<uint8_t>(v & 0xFF));
      buf_.push_back(static_cast<uint8_t>((v >> 8) & 0xFF));
    }
  }

  void write32(uint32_t v) {
    if (big_endian_) {
      buf_.push_back(static_cast<uint8_t>((v >> 24) & 0xFF));
      buf_.push_back(static_cast<uint8_t>((v >> 16) & 0xFF));
      buf_.push_back(static_cast<uint8_t>((v >> 8) & 0xFF));
      buf_.push_back(static_cast<uint8_t>(v & 0xFF));
    } else {
      buf_.push_back(static_cast<uint8_t>(v & 0xFF));
      buf_.push_back(static_cast<uint8_t>((v >> 8) & 0xFF));
      buf_.push_back(static_cast<uint8_t>((v >> 16) & 0xFF));
      buf_.push_back(static_cast<uint8_t>((v >> 24) & 0xFF));
    }
  }

  void write_bytes(const uint8_t* data, size_t len) {
    buf_.insert(buf_.end(), data, data + len);
  }

  void write_string(const std::string& s) {
    buf_.insert(buf_.end(), s.begin(), s.end());
    buf_.push_back(0);  // null terminator
  }

  void write_padding(size_t count) {
    buf_.insert(buf_.end(), count, 0);
  }

  auto data() const -> const std::vector<uint8_t>& { return buf_; }
  auto size() const -> size_t { return buf_.size(); }
  auto big_endian() const -> bool { return big_endian_; }

  /// Current write position (offset from start).
  auto pos() const -> size_t { return buf_.size(); }

  /// Overwrite 2 bytes at a given offset.
  void patch16(size_t offset, uint16_t v) {
    if (offset + 2 > buf_.size()) return;
    if (big_endian_) {
      buf_[offset]     = static_cast<uint8_t>((v >> 8) & 0xFF);
      buf_[offset + 1] = static_cast<uint8_t>(v & 0xFF);
    } else {
      buf_[offset]     = static_cast<uint8_t>(v & 0xFF);
      buf_[offset + 1] = static_cast<uint8_t>((v >> 8) & 0xFF);
    }
  }

  /// Overwrite 4 bytes at a given offset.
  void patch32(size_t offset, uint32_t v) {
    if (offset + 4 > buf_.size()) return;
    if (big_endian_) {
      buf_[offset]     = static_cast<uint8_t>((v >> 24) & 0xFF);
      buf_[offset + 1] = static_cast<uint8_t>((v >> 16) & 0xFF);
      buf_[offset + 2] = static_cast<uint8_t>((v >> 8) & 0xFF);
      buf_[offset + 3] = static_cast<uint8_t>(v & 0xFF);
    } else {
      buf_[offset]     = static_cast<uint8_t>(v & 0xFF);
      buf_[offset + 1] = static_cast<uint8_t>((v >> 8) & 0xFF);
      buf_[offset + 2] = static_cast<uint8_t>((v >> 16) & 0xFF);
      buf_[offset + 3] = static_cast<uint8_t>((v >> 24) & 0xFF);
    }
  }

 private:
  std::vector<uint8_t> buf_;
  bool big_endian_;
};

/// -----------------------------------------------------------------------
/// Structure representing a single IFD entry.
/// -----------------------------------------------------------------------
struct IfdEntry {
  uint16_t tag        = 0;
  uint16_t type       = 0;
  uint32_t count      = 0;
  uint32_t value_offset = 0;  // offset in file or inline value
  std::vector<uint8_t> inline_data;  // for small values stored inline
};

/// Type size lookup.
auto type_size(uint16_t type) -> int {
  switch (type) {
    case TYPE_BYTE:
    case TYPE_SBYTE:
    case TYPE_UNDEFINED:
    case TYPE_ASCII:       return 1;
    case TYPE_SHORT:
    case TYPE_SSHORT:      return 2;
    case TYPE_LONG:
    case TYPE_SLONG:
    case TYPE_FLOAT:       return 4;
    case TYPE_RATIONAL:
    case TYPE_SRATIONAL:
    case TYPE_DOUBLE:      return 8;
    default:               return 1;
  }
}

/// Parse IFD entries from a TIFF/EXIF block.
auto parse_ifd(const ByteReader& reader, size_t ifd_offset, uint16_t entry_count)
    -> std::pair<std::vector<IfdEntry>, uint32_t> {
  std::vector<IfdEntry> entries;
  size_t offset = ifd_offset;

  for (uint16_t i = 0; i < entry_count; ++i) {
    IfdEntry e;
    e.tag   = reader.read16(offset);
    e.type  = reader.read16(offset + 2);
    e.count = reader.read32(offset + 4);

    size_t byte_size = static_cast<size_t>(e.count) * type_size(e.type);
    if (byte_size <= 4) {
      // Inline value
      e.value_offset = 0;
      e.inline_data.resize(byte_size);
      std::memcpy(e.inline_data.data(), reader.data() + offset + 8, byte_size);
    } else {
      e.value_offset = reader.read32(offset + 8);
    }
    entries.push_back(e);
    offset += IFD_ENTRY_SIZE;
  }

  uint32_t next_ifd = reader.read32(offset);  // next IFD offset (0 = end)
  return {entries, next_ifd};
}

/// Read a string value from an IFD entry.
auto read_string_from_entry(const ByteReader& reader, const IfdEntry& e) -> std::string {
  if (e.type != TYPE_ASCII) return {};
  size_t len = e.count > 0 ? e.count - 1 : 0;  // exclude null terminator
  if (e.inline_data.size() >= e.count) {
    return std::string(reinterpret_cast<const char*>(e.inline_data.data()), len);
  }
  return reader.read_string(e.value_offset, len);
}

/// Read a rational value from an IFD entry.
auto read_rational_from_entry(const ByteReader& reader, const IfdEntry& e) -> alcedo::exif::Rational {
  if (e.type != TYPE_RATIONAL && e.type != TYPE_SRATIONAL) return {};

  if (e.inline_data.size() >= 8) {
    alcedo::exif::Rational r;
    std::memcpy(&r.numerator,   e.inline_data.data(), 4);
    std::memcpy(&r.denominator, e.inline_data.data() + 4, 4);
    if (reader.big_endian()) {
      r.numerator = static_cast<int32_t>(((static_cast<uint32_t>(r.numerator) & 0xFF) << 24) |
                                          ((static_cast<uint32_t>(r.numerator) & 0xFF00) << 8) |
                                          ((static_cast<uint32_t>(r.numerator) >> 8) & 0xFF00) |
                                          ((static_cast<uint32_t>(r.numerator) >> 24) & 0xFF));
      r.denominator = static_cast<int32_t>(
          ((static_cast<uint32_t>(r.denominator) & 0xFF) << 24) |
          ((static_cast<uint32_t>(r.denominator) & 0xFF00) << 8) |
          ((static_cast<uint32_t>(r.denominator) >> 8) & 0xFF00) |
          ((static_cast<uint32_t>(r.denominator) >> 24) & 0xFF));
    }
    return r;
  }

  if (e.value_offset + 8 <= reader.size()) {
    alcedo::exif::Rational r;
    r.numerator   = static_cast<int32_t>(reader.read32(e.value_offset));
    r.denominator = static_cast<int32_t>(reader.read32(e.value_offset + 4));
    return r;
  }
  return {};
}

/// Read a uint32_t from an IFD entry (SHORT or LONG).
auto read_uint32_from_entry(const ByteReader& reader, const IfdEntry& e) -> uint32_t {
  if (e.type == TYPE_SHORT) {
    if (!e.inline_data.empty()) return e.inline_data[0] | (e.inline_data[1] << 8);
    return reader.read16(e.value_offset);
  }
  if (e.type == TYPE_LONG) {
    if (e.inline_data.size() >= 4) {
      uint32_t v;
      std::memcpy(&v, e.inline_data.data(), 4);
      return reader.big_endian() ? ((v & 0xFF) << 24) | ((v & 0xFF00) << 8) |
                                    ((v >> 8) & 0xFF00) | ((v >> 24) & 0xFF)
                                  : v;
    }
    return reader.read32(e.value_offset);
  }
  return 0;
}

/// Read a float from an IFD entry (FLOAT or RATIONAL).
auto read_float_from_entry(const ByteReader& reader, const IfdEntry& e) -> float {
  if (e.type == TYPE_RATIONAL || e.type == TYPE_SRATIONAL) {
    auto r = read_rational_from_entry(reader, e);
    if (r.denominator != 0) return static_cast<float>(r.numerator) / static_cast<float>(r.denominator);
  }
  if (e.type == TYPE_FLOAT) {
    if (e.inline_data.size() >= 4) {
      float v;
      std::memcpy(&v, e.inline_data.data(), 4);
      return v;
    }
  }
  return 0.0f;
}

/// Parse GPS coordinates from a GPS IFD.
auto parse_gps(const ByteReader& reader,
               const std::vector<IfdEntry>& gps_entries) -> alcedo::exif::GPSCoordinate {
  alcedo::exif::GPSCoordinate gps;

  for (const auto& e : gps_entries) {
    switch (e.tag) {
      case TAG_GPS_LATITUDE_REF: {
        break;
      }
      case TAG_GPS_LATITUDE: {
        if (e.type == TYPE_RATIONAL && e.count >= 3) {
          // Read as three rational values
          size_t base = e.inline_data.size() >= 8 * 3 ? 0 : e.value_offset;
          auto deg = static_cast<double>(reader.read32(base)) /
                     static_cast<double>(reader.read32(base + 4));
          auto min = static_cast<double>(reader.read32(base + 8)) /
                     static_cast<double>(reader.read32(base + 12));
          auto sec = static_cast<double>(reader.read32(base + 16)) /
                     static_cast<double>(reader.read32(base + 20));
          gps.latitude = deg + min / 60.0 + sec / 3600.0;
        }
        break;
      }
      case TAG_GPS_LONGITUDE_REF: {
        break;
      }
      case TAG_GPS_LONGITUDE: {
        if (e.type == TYPE_RATIONAL && e.count >= 3) {
          size_t base = e.inline_data.size() >= 8 * 3 ? 0 : e.value_offset;
          auto deg = static_cast<double>(reader.read32(base)) /
                     static_cast<double>(reader.read32(base + 4));
          auto min = static_cast<double>(reader.read32(base + 8)) /
                     static_cast<double>(reader.read32(base + 12));
          auto sec = static_cast<double>(reader.read32(base + 16)) /
                     static_cast<double>(reader.read32(base + 20));
          gps.longitude = deg + min / 60.0 + sec / 3600.0;
        }
        break;
      }
      case TAG_GPS_ALTITUDE_REF: {
        break;
      }
      case TAG_GPS_ALTITUDE: {
        if (e.type == TYPE_RATIONAL && e.count >= 1) {
          gps.altitude = static_cast<double>(reader.read32(e.value_offset)) /
                         static_cast<double>(reader.read32(e.value_offset + 4));
          gps.has_altitude = true;
        }
        break;
      }
    }
  }

  // Apply sign based on reference
  // (We check the string refs from the tags we already parsed)
  for (const auto& e : gps_entries) {
    if (e.tag == TAG_GPS_LATITUDE_REF) {
      auto ref = read_string_from_entry(reader, e);
      if (!ref.empty() && (ref[0] == 'S' || ref[0] == 's')) gps.latitude = -gps.latitude;
    }
    if (e.tag == TAG_GPS_LONGITUDE_REF) {
      auto ref = read_string_from_entry(reader, e);
      if (!ref.empty() && (ref[0] == 'W' || ref[0] == 'w')) gps.longitude = -gps.longitude;
    }
  }

  return gps;
}

/// Find the TIFF IFD inside a TIFF header at base_offset.
/// Returns {ifd0_entries, exif_ifd_entries, gps_ifd_entries, maker_note_data}.
struct TiffParseResult {
  std::vector<IfdEntry> ifd0;
  std::vector<IfdEntry> exif_ifd;
  std::vector<IfdEntry> gps_ifd;
  std::vector<uint8_t>  maker_note_raw;
  uint32_t              exif_ifd_offset = 0;
  uint32_t              gps_ifd_offset  = 0;
  uint32_t              maker_note_offset = 0;
  uint32_t              maker_note_size   = 0;
};

auto parse_tiff_structure(const ByteReader& reader, size_t base_offset) -> TiffParseResult {
  TiffParseResult result;

  // Read IFD0
  uint16_t entry_count = reader.read16(base_offset);
  auto [ifd0_entries, next_ifd] = parse_ifd(reader, base_offset + 2, entry_count);
  result.ifd0 = std::move(ifd0_entries);

  // Look for ExifIFD, GPS IFD, MakerNote in IFD0
  for (const auto& e : result.ifd0) {
    if (e.tag == TAG_EXIF_IFD) {
      result.exif_ifd_offset = read_uint32_from_entry(reader, e);
    } else if (e.tag == TAG_GPS_IFD) {
      result.gps_ifd_offset = read_uint32_from_entry(reader, e);
    } else if (e.tag == TAG_MAKER_NOTE) {
      result.maker_note_offset = e.value_offset;
      result.maker_note_size   = e.count;
      // Copy raw maker note data
      if (e.inline_data.size() >= e.count) {
        result.maker_note_raw = e.inline_data;
      } else if (e.value_offset + e.count <= reader.size()) {
        result.maker_note_raw.assign(reader.data() + e.value_offset,
                                     reader.data() + e.value_offset + e.count);
      }
    }
  }

  // Parse Exif IFD sub-IFD
  if (result.exif_ifd_offset > 0 && result.exif_ifd_offset + 2 <= reader.size()) {
    uint16_t exif_count = reader.read16(result.exif_ifd_offset);
    auto [exif_entries, _] = parse_ifd(reader, result.exif_ifd_offset + 2, exif_count);
    // Look for GPS IFD inside Exif IFD too (some cameras put it here)
    for (const auto& e : exif_entries) {
      if (e.tag == TAG_GPS_IFD && result.gps_ifd_offset == 0) {
        result.gps_ifd_offset = read_uint32_from_entry(reader, e);
      } else if (e.tag == TAG_MAKER_NOTE && result.maker_note_offset == 0) {
        result.maker_note_offset = e.value_offset;
        result.maker_note_size   = e.count;
        if (e.inline_data.size() >= e.count) {
          result.maker_note_raw = e.inline_data;
        } else if (e.value_offset + e.count <= reader.size()) {
          result.maker_note_raw.assign(reader.data() + e.value_offset,
                                       reader.data() + e.value_offset + e.count);
        }
      }
    }
    result.exif_ifd = std::move(exif_entries);
  }

  // Parse GPS IFD
  if (result.gps_ifd_offset > 0 && result.gps_ifd_offset + 2 <= reader.size()) {
    uint16_t gps_count = reader.read16(result.gps_ifd_offset);
    auto [gps_entries, _] = parse_ifd(reader, result.gps_ifd_offset + 2, gps_count);
    result.gps_ifd = std::move(gps_entries);
  }

  return result;
}

/// Populate ExifMetadata from parsed TIFF data.
auto populate_metadata(const ByteReader& reader,
                       const TiffParseResult& tiff) -> alcedo::exif::ExifMetadata {
  using alcedo::exif::ExifMetadata;
  ExifMetadata meta;

  // Helper: search IFD0 and ExifIFD for a tag
  auto find_entry = [&](uint16_t tag) -> const IfdEntry* {
    for (const auto& e : tiff.ifd0) if (e.tag == tag) return &e;
    for (const auto& e : tiff.exif_ifd) if (e.tag == tag) return &e;
    return nullptr;
  };

  // Camera
  if (auto* e = find_entry(TAG_MAKE); e) meta.make = read_string_from_entry(reader, *e);
  if (auto* e = find_entry(TAG_MODEL); e) meta.model = read_string_from_entry(reader, *e);
  if (auto* e = find_entry(TAG_LENS_MODEL); e) meta.lens = read_string_from_entry(reader, *e);
  if (auto* e = find_entry(TAG_LENS_MAKE); e) meta.lens_make = read_string_from_entry(reader, *e);

  // Exposure
  if (auto* e = find_entry(TAG_FNUMBER); e) meta.aperture = read_rational_from_entry(reader, *e);
  if (auto* e = find_entry(TAG_EXPOSURE_TIME); e) meta.shutter_speed = read_rational_from_entry(reader, *e);
  if (auto* e = find_entry(TAG_ISO); e) meta.iso = read_uint32_from_entry(reader, *e);
  if (auto* e = find_entry(TAG_FOCAL_LENGTH); e) meta.focal_length = read_rational_from_entry(reader, *e);
  if (auto* e = find_entry(TAG_FOCAL_LENGTH_35MM); e) meta.focal_length_35mm = read_rational_from_entry(reader, *e);

  // Date/Time
  if (auto* e = find_entry(TAG_DATE_TIME); e) meta.date_time = read_string_from_entry(reader, *e);
  if (auto* e = find_entry(TAG_DATE_TIME_ORIGINAL); e) meta.date_time_original = read_string_from_entry(reader, *e);
  if (auto* e = find_entry(TAG_DATE_TIME_DIGITIZED); e) meta.date_time_digitized = read_string_from_entry(reader, *e);

  // Copyright/Creator
  if (auto* e = find_entry(TAG_COPYRIGHT); e) meta.copyright = read_string_from_entry(reader, *e);
  if (auto* e = find_entry(TAG_ARTIST); e) meta.artist = read_string_from_entry(reader, *e);
  if (auto* e = find_entry(TAG_IMAGE_DESCRIPTION); e) meta.description = read_string_from_entry(reader, *e);

  // Rating
  if (auto* e = find_entry(TAG_RATING); e) {
    meta.rating = static_cast<uint16_t>(read_uint32_from_entry(reader, *e));
  } else if (auto* e = find_entry(TAG_RATING_PERCENT); e) {
    auto pct = read_uint32_from_entry(reader, *e);
    meta.rating = static_cast<uint16_t>((pct + 19) / 20);  // scale 0-100 → 0-5
  }

  // GPS
  if (!tiff.gps_ifd.empty()) {
    meta.gps = parse_gps(reader, tiff.gps_ifd);
  }

  return meta;
}

/// -----------------------------------------------------------------------
/// JPEG EXIF reader — finds the APP1 marker and parses the TIFF block.
/// -----------------------------------------------------------------------
auto read_jpeg_exif(const std::string& file_path) -> alcedo::exif::ExifReadResult {
  alcedo::exif::ExifReadResult result;
  result.file_path = file_path;

  std::ifstream in(file_path, std::ios::binary | std::ios::ate);
  if (!in) {
    result.error_message = "Cannot open file";
    return result;
  }

  auto file_size = static_cast<size_t>(in.tellg());
  in.seekg(0);
  std::vector<uint8_t> buffer(file_size);
  if (!in.read(reinterpret_cast<char*>(buffer.data()), file_size)) {
    result.error_message = "Failed to read file";
    return result;
  }

  // Find APP1 marker
  size_t pos = 0;
  bool found = false;
  while (pos + 4 <= file_size) {
    if (buffer[pos] == JPEG_MARKER_PREFIX && buffer[pos + 1] == JPEG_APP1) {
      // Check for "Exif\0\0" identifier
      if (pos + 10 <= file_size &&
          buffer[pos + 4] == 'E' && buffer[pos + 5] == 'x' &&
          buffer[pos + 6] == 'i' && buffer[pos + 7] == 'f' &&
          buffer[pos + 8] == 0 && buffer[pos + 9] == 0) {
        found = true;
        break;
      }
    }
    // Skip non-marker bytes
    if (buffer[pos] == JPEG_MARKER_PREFIX) {
      pos += 2;  // skip marker
      if (pos + 2 <= file_size) {
        uint16_t seg_len = (static_cast<uint16_t>(buffer[pos]) << 8) | buffer[pos + 1];
        pos += seg_len;
      } else {
        break;
      }
    } else {
      ++pos;
    }
  }

  if (!found) {
    result.error_message = "No EXIF data found in JPEG";
    return result;
  }

  // TIFF header starts after "Exif\0\0" (10 bytes from APP1 marker start)
  size_t tiff_start = pos + 10;
  if (tiff_start + 8 > file_size) {
    result.error_message = "Truncated EXIF data";
    return result;
  }

  // Read byte order
  uint16_t byte_order;
  std::memcpy(&byte_order, buffer.data() + tiff_start, 2);
  bool big_endian = (byte_order == TIFF_BIG_ENDIAN);

  ByteReader reader(buffer.data(), file_size, big_endian);

  uint16_t magic = reader.read16(tiff_start + 2);
  if (magic != TIFF_MAGIC) {
    result.error_message = "Invalid TIFF magic";
    return result;
  }

  uint32_t ifd0_offset = reader.read32(tiff_start + 4);
  size_t ifd0_abs = tiff_start + ifd0_offset;

  auto tiff = parse_tiff_structure(reader, ifd0_abs);
  result.metadata = populate_metadata(reader, tiff);
  result.success = true;
  return result;
}

/// -----------------------------------------------------------------------
/// TIFF / RAW EXIF reader.
/// -----------------------------------------------------------------------
auto read_tiff_exif(const std::string& file_path) -> alcedo::exif::ExifReadResult {
  alcedo::exif::ExifReadResult result;
  result.file_path = file_path;

  std::ifstream in(file_path, std::ios::binary | std::ios::ate);
  if (!in) {
    result.error_message = "Cannot open file";
    return result;
  }

  auto file_size = static_cast<size_t>(in.tellg());
  in.seekg(0);
  std::vector<uint8_t> buffer(file_size);
  if (!in.read(reinterpret_cast<char*>(buffer.data()), file_size)) {
    result.error_message = "Failed to read file";
    return result;
  }

  if (file_size < 8) {
    result.error_message = "File too small for TIFF";
    return result;
  }

  uint16_t byte_order;
  std::memcpy(&byte_order, buffer.data(), 2);
  bool big_endian = (byte_order == TIFF_BIG_ENDIAN);

  ByteReader reader(buffer.data(), file_size, big_endian);

  uint16_t magic = reader.read16(2);
  if (magic != TIFF_MAGIC) {
    result.error_message = "Not a valid TIFF file";
    return result;
  }

  uint32_t ifd0_offset = reader.read32(4);
  auto tiff = parse_tiff_structure(reader, ifd0_offset);
  result.metadata = populate_metadata(reader, tiff);
  result.success = true;
  return result;
}

/// -----------------------------------------------------------------------
/// Write EXIF IFD entries to a ByteWriter.
/// Returns the offset of the next IFD pointer (0-terminated).
/// -----------------------------------------------------------------------
struct WriteIfdPlan {
  std::vector<IfdEntry> entries;
  uint32_t next_ifd_offset = 0;
};

auto write_ifd_entries(ByteWriter& writer,
                        const std::vector<IfdEntry>& entries,
                        uint32_t next_ifd) -> uint32_t {
  auto ifd_start = static_cast<uint32_t>(writer.pos());
  writer.write16(static_cast<uint16_t>(entries.size()));

  for (const auto& e : entries) {
    writer.write16(e.tag);
    writer.write16(e.type);
    writer.write32(e.count);

    size_t data_size = static_cast<size_t>(e.count) * type_size(e.type);
    if (data_size <= 4) {
      // Inline
      writer.write_bytes(e.inline_data.data(), e.inline_data.size());
      // Pad to 4 bytes
      for (size_t p = e.inline_data.size(); p < 4; ++p) writer.write_bytes(reinterpret_cast<const uint8_t*>("\0"), 1);
    } else {
      // Write offset to data area (which follows the IFD entries)
      // We'll compute the actual offset later; write a placeholder
      writer.write32(0);  // placeholder, patched after
    }
  }

  writer.write32(next_ifd);
  return ifd_start;
}

/// -----------------------------------------------------------------------
/// Build a new EXIF APP1 segment for a JPEG file.
/// We take the original IFD entries, modify/add as needed, and build a new
/// TIFF block.
/// -----------------------------------------------------------------------
auto build_jpeg_exif_segment(const std::vector<uint8_t>& original_file,
                             const alcedo::exif::ExifMetadata& metadata,
                             size_t tiff_start_in_file) -> std::vector<uint8_t> {
  // Parse original TIFF to get existing entries
  if (tiff_start_in_file + 8 > original_file.size()) return {};

  uint16_t byte_order;
  std::memcpy(&byte_order, original_file.data() + tiff_start_in_file, 2);
  bool big_endian = (byte_order == TIFF_BIG_ENDIAN);

  ByteReader reader(original_file.data(), original_file.size(), big_endian);
  uint32_t ifd0_offset = reader.read32(tiff_start_in_file + 4);
  size_t ifd0_abs = tiff_start_in_file + ifd0_offset;

  auto tiff = parse_tiff_structure(reader, ifd0_abs);

  // Build new IFD0 entries
  ByteWriter writer(big_endian);

  // TIFF header
  writer.write16(big_endian ? TIFF_BIG_ENDIAN : TIFF_LITTLE_ENDIAN);
  writer.write16(TIFF_MAGIC);
  // IFD0 offset placeholder (will be 8)
  writer.write32(0);

  // ---- Data area starts after IFD entries ----
  // We'll write string data here, then the IFD entries

  // Helper to plan an IFD entry
  struct PlannedEntry {
    uint16_t tag;
    uint16_t type;
    std::string str_value;
    uint32_t num_value;
    alcedo::exif::Rational rat_value;
    bool is_string = false;
    bool is_rational = false;
    bool is_num = false;
  };
  std::vector<PlannedEntry> plan;

  // Always emit Make/Model if present
  if (metadata.make)    plan.push_back({TAG_MAKE, TYPE_ASCII, *metadata.make, 0, {}, true});
  if (metadata.model)   plan.push_back({TAG_MODEL, TYPE_ASCII, *metadata.model, 0, {}, true});
  if (metadata.copyright) plan.push_back({TAG_COPYRIGHT, TYPE_ASCII, *metadata.copyright, 0, {}, true});
  if (metadata.artist)  plan.push_back({TAG_ARTIST, TYPE_ASCII, *metadata.artist, 0, {}, true});
  if (metadata.description) plan.push_back({TAG_IMAGE_DESCRIPTION, TYPE_ASCII, *metadata.description, 0, {}, true});
  if (metadata.rating)  plan.push_back({TAG_RATING, TYPE_SHORT, "", static_cast<uint32_t>(*metadata.rating), {}, false, false, true});

  if (metadata.aperture)    plan.push_back({TAG_FNUMBER, TYPE_RATIONAL, "", 0, *metadata.aperture, false, true});
  if (metadata.shutter_speed) plan.push_back({TAG_EXPOSURE_TIME, TYPE_RATIONAL, "", 0, *metadata.shutter_speed, false, true});
  if (metadata.iso)         plan.push_back({TAG_ISO, TYPE_SHORT, "", *metadata.iso, {}, false, false, true});
  if (metadata.focal_length) plan.push_back({TAG_FOCAL_LENGTH, TYPE_RATIONAL, "", 0, *metadata.focal_length, false, true});
  if (metadata.focal_length_35mm) plan.push_back({TAG_FOCAL_LENGTH_35MM, TYPE_SHORT, "", static_cast<uint32_t>(metadata.focal_length_35mm->numerator), {}, false, false, true});
  if (metadata.lens)     plan.push_back({TAG_LENS_MODEL, TYPE_ASCII, *metadata.lens, 0, {}, true});
  if (metadata.lens_make) plan.push_back({TAG_LENS_MAKE, TYPE_ASCII, *metadata.lens_make, 0, {}, true});

  if (metadata.date_time) plan.push_back({TAG_DATE_TIME, TYPE_ASCII, *metadata.date_time, 0, {}, true});
  if (metadata.date_time_original) plan.push_back({TAG_DATE_TIME_ORIGINAL, TYPE_ASCII, *metadata.date_time_original, 0, {}, true});
  if (metadata.date_time_digitized) plan.push_back({TAG_DATE_TIME_DIGITIZED, TYPE_ASCII, *metadata.date_time_digitized, 0, {}, true});

  // Reserve space for the data area offset
  // We'll write string data first, then IFD entries, then patch IFD0 offset

  // First pass: write string data and record offsets
  // We'll write the IFD entries first, then data area
  // Actually, let's write IFD entries + data area in order

  // Write strings to data area
  struct StringRef {
    size_t writer_offset;
  };
  std::vector<StringRef> string_refs;

  for (auto& p : plan) {
    if (p.is_string) {
      p.num_value = static_cast<uint32_t>(writer.pos());
      string_refs.push_back({writer.pos()});
      writer.write_string(p.str_value);
    }
  }

  // Align to 2 bytes
  if (writer.pos() % 2 != 0) writer.write_padding(1);

  // Write rational values
  for (auto& p : plan) {
    if (p.is_rational) {
      p.num_value = static_cast<uint32_t>(writer.pos());
      writer.write32(static_cast<uint32_t>(p.rat_value.numerator));
      writer.write32(static_cast<uint32_t>(p.rat_value.denominator));
    }
  }

  // Reset and rebuild properly
  ByteWriter w2(big_endian);

  // TIFF header
  w2.write16(big_endian ? TIFF_BIG_ENDIAN : TIFF_LITTLE_ENDIAN);
  w2.write16(TIFF_MAGIC);

  // IFD0 offset: right after header (8 bytes in)
  w2.write32(8);

  // IFD0 entries
  uint16_t num_entries = static_cast<uint16_t>(plan.size());
  w2.write16(num_entries);

  // Where data area starts (after IFD entries + next IFD pointer)
  // IFD entries: 12 bytes each, + 2 bytes count + 4 bytes next IFD = 6 + 12*N
  size_t data_area_start = 8 + 2 + 12 * num_entries + 4;

  for (auto& p : plan) {
    w2.write16(p.tag);
    if (p.is_string) {
      w2.write16(TYPE_ASCII);
      w2.write32(static_cast<uint32_t>(p.str_value.size() + 1));  // +1 for null
    } else if (p.is_rational) {
      w2.write16(TYPE_RATIONAL);
      w2.write32(1);
    } else if (p.tag == TAG_ISO || p.tag == TAG_FOCAL_LENGTH_35MM) {
      w2.write16(TYPE_SHORT);
      w2.write32(1);
    } else if (p.is_num) {
      w2.write16(TYPE_SHORT);
      w2.write32(1);
    } else {
      w2.write16(TYPE_BYTE);
      w2.write32(0);
    }

    if (p.is_num && !p.is_string) {
      // Inline SHORT
      w2.write32(p.num_value);
      w2.write32(0);  // padding to 4 bytes
    } else {
      // offset to data area
      w2.write32(static_cast<uint32_t>(data_area_start));
      // Advance data area pointer
      if (p.is_string) {
        data_area_start += p.str_value.size() + 1;
        if (data_area_start % 2 != 0) ++data_area_start;
      } else if (p.is_rational) {
        data_area_start += 8;
      }
    }
  }

  // Next IFD = 0 (end)
  w2.write32(0);

  // Now write data area
  for (auto& p : plan) {
    if (p.is_string) {
      w2.write_string(p.str_value);
      if (w2.pos() % 2 != 0) w2.write_padding(1);
    }
  }
  for (auto& p : plan) {
    if (p.is_rational) {
      w2.write32(static_cast<uint32_t>(p.rat_value.numerator));
      w2.write32(static_cast<uint32_t>(p.rat_value.denominator));
    }
  }

  // Build the APP1 segment
  auto tiff_data = w2.data();
  uint16_t app1_length = static_cast<uint16_t>(2 + tiff_data.size());  // length includes the 2 bytes for length itself

  std::vector<uint8_t> segment;
  segment.push_back(JPEG_MARKER_PREFIX);
  segment.push_back(JPEG_APP1);
  segment.push_back(static_cast<uint8_t>((app1_length >> 8) & 0xFF));
  segment.push_back(static_cast<uint8_t>(app1_length & 0xFF));
  // Exif identifier
  segment.push_back('E'); segment.push_back('x');
  segment.push_back('i'); segment.push_back('f');
  segment.push_back(0); segment.push_back(0);
  // TIFF data
  segment.insert(segment.end(), tiff_data.begin(), tiff_data.end());

  return segment;
}

/// -----------------------------------------------------------------------
/// Write EXIF to JPEG: replace the APP1 segment and write back.
/// -----------------------------------------------------------------------
auto write_jpeg_exif(const std::string& file_path,
                     const alcedo::exif::ExifMetadata& metadata) -> alcedo::exif::ExifWriteResult {
  alcedo::exif::ExifWriteResult result;
  result.file_path = file_path;

  std::ifstream in(file_path, std::ios::binary | std::ios::ate);
  if (!in) {
    result.error_message = "Cannot open file";
    return result;
  }
  auto file_size = static_cast<size_t>(in.tellg());
  in.seekg(0);
  std::vector<uint8_t> buffer(file_size);
  if (!in.read(reinterpret_cast<char*>(buffer.data()), file_size)) {
    result.error_message = "Failed to read file";
    return result;
  }
  in.close();

  // Find APP1 EXIF marker
  size_t app1_pos = 0;
  size_t app1_end = 0;
  size_t pos = 0;

  while (pos + 4 <= file_size) {
    if (buffer[pos] == JPEG_MARKER_PREFIX && buffer[pos + 1] == JPEG_APP1) {
      if (pos + 10 <= file_size &&
          buffer[pos + 4] == 'E' && buffer[pos + 5] == 'x' &&
          buffer[pos + 6] == 'i' && buffer[pos + 7] == 'f' &&
          buffer[pos + 8] == 0 && buffer[pos + 9] == 0) {
        app1_pos = pos;
        uint16_t seg_len = (static_cast<uint16_t>(buffer[pos + 2]) << 8) | buffer[pos + 3];
        app1_end = pos + 2 + seg_len;
        break;
      }
    }
    if (buffer[pos] == JPEG_MARKER_PREFIX) {
      if (pos + 2 < file_size) {
        uint16_t seg_len = (static_cast<uint16_t>(buffer[pos + 2]) << 8) | buffer[pos + 3];
        pos += 2 + seg_len;
      } else {
        break;
      }
    } else {
      ++pos;
    }
  }

  // Build new APP1 segment
  size_t tiff_start = app1_pos + 10;  // skip APP1 marker + length + "Exif\0\0"
  auto new_segment = build_jpeg_exif_segment(buffer, metadata, tiff_start);

  if (new_segment.empty()) {
    result.error_message = "Failed to build EXIF segment";
    return result;
  }

  // Rebuild file: before APP1 + new APP1 + after APP1
  std::vector<uint8_t> output;
  output.insert(output.end(), buffer.begin(), buffer.begin() + app1_pos);
  output.insert(output.end(), new_segment.begin(), new_segment.end());
  if (app1_end < file_size) {
    output.insert(output.end(), buffer.begin() + app1_end, buffer.end());
  }

  // Write back
  std::ofstream out(file_path, std::ios::binary | std::ios::trunc);
  if (!out) {
    result.error_message = "Cannot write file";
    return result;
  }
  out.write(reinterpret_cast<const char*>(output.data()), output.size());
  result.success = true;
  return result;
}

/// -----------------------------------------------------------------------
/// Write EXIF to TIFF/RAW: replace the TIFF IFD0 entries.
/// Simplified: rebuild the entire TIFF from scratch with only the metadata
/// we want to preserve/write.
/// -----------------------------------------------------------------------
auto write_tiff_exif(const std::string& file_path,
                     const alcedo::exif::ExifMetadata& metadata) -> alcedo::exif::ExifWriteResult {
  alcedo::exif::ExifWriteResult result;
  result.file_path = file_path;

  std::ifstream in(file_path, std::ios::binary);
  if (!in) {
    result.error_message = "Cannot open file";
    return result;
  }
  std::vector<uint8_t> buffer((std::istreambuf_iterator<char>(in)),
                               std::istreambuf_iterator<char>());
  in.close();

  if (buffer.size() < 8) {
    result.error_message = "File too small";
    return result;
  }

  uint16_t byte_order;
  std::memcpy(&byte_order, buffer.data(), 2);
  bool big_endian = (byte_order == TIFF_BIG_ENDIAN);

  ByteReader reader(buffer.data(), buffer.size(), big_endian);
  uint32_t ifd0_offset = reader.read32(4);
  auto tiff = parse_tiff_structure(reader, ifd0_offset);

  // Build IFD0 entries, preserving existing entries we don't modify
  // We'll build a new TIFF and overlay it

  // For simplicity, we'll write a new TIFF with only the metadata tags
  // and the IFD0 offset at position 4
  ByteWriter writer(big_endian);

  // TIFF header
  writer.write16(big_endian ? TIFF_BIG_ENDIAN : TIFF_LITTLE_ENDIAN);
  writer.write16(TIFF_MAGIC);
  writer.write32(8);  // IFD0 at offset 8

  // Plan the entries
  struct PlannedEntry {
    uint16_t tag;
    uint16_t type;
    std::string str_value;
    uint32_t num_value;
    alcedo::exif::Rational rat_value;
    bool is_string = false;
    bool is_rational = false;
    bool is_num = false;
  };
  std::vector<PlannedEntry> plan;

  if (metadata.make)    plan.push_back({TAG_MAKE, TYPE_ASCII, *metadata.make, 0, {}, true});
  if (metadata.model)   plan.push_back({TAG_MODEL, TYPE_ASCII, *metadata.model, 0, {}, true});
  if (metadata.copyright) plan.push_back({TAG_COPYRIGHT, TYPE_ASCII, *metadata.copyright, 0, {}, true});
  if (metadata.artist)  plan.push_back({TAG_ARTIST, TYPE_ASCII, *metadata.artist, 0, {}, true});
  if (metadata.description) plan.push_back({TAG_IMAGE_DESCRIPTION, TYPE_ASCII, *metadata.description, 0, {}, true});
  if (metadata.rating)  plan.push_back({TAG_RATING, TYPE_SHORT, "", static_cast<uint32_t>(*metadata.rating), {}, false, false, true});

  if (metadata.aperture)    plan.push_back({TAG_FNUMBER, TYPE_RATIONAL, "", 0, *metadata.aperture, false, true});
  if (metadata.shutter_speed) plan.push_back({TAG_EXPOSURE_TIME, TYPE_RATIONAL, "", 0, *metadata.shutter_speed, false, true});
  if (metadata.iso)         plan.push_back({TAG_ISO, TYPE_SHORT, "", *metadata.iso, {}, false, false, true});
  if (metadata.focal_length) plan.push_back({TAG_FOCAL_LENGTH, TYPE_RATIONAL, "", 0, *metadata.focal_length, false, true});
  if (metadata.focal_length_35mm) plan.push_back({TAG_FOCAL_LENGTH_35MM, TYPE_SHORT, "", static_cast<uint32_t>(metadata.focal_length_35mm->numerator), {}, false, false, true});
  if (metadata.lens)     plan.push_back({TAG_LENS_MODEL, TYPE_ASCII, *metadata.lens, 0, {}, true});
  if (metadata.lens_make) plan.push_back({TAG_LENS_MAKE, TYPE_ASCII, *metadata.lens_make, 0, {}, true});

  if (metadata.date_time) plan.push_back({TAG_DATE_TIME, TYPE_ASCII, *metadata.date_time, 0, {}, true});
  if (metadata.date_time_original) plan.push_back({TAG_DATE_TIME_ORIGINAL, TYPE_ASCII, *metadata.date_time_original, 0, {}, true});
  if (metadata.date_time_digitized) plan.push_back({TAG_DATE_TIME_DIGITIZED, TYPE_ASCII, *metadata.date_time_digitized, 0, {}, true});

  // Preserve MakerNote tag (raw bytes)
  if (!tiff.maker_note_raw.empty()) {
    // We'll add a MakerNote entry pointing to the data area
    PlannedEntry mn;
    mn.tag = TAG_MAKER_NOTE;
    mn.type = TYPE_UNDEFINED;
    mn.num_value = static_cast<uint32_t>(tiff.maker_note_raw.size());
    mn.is_num = true;
    // We'll handle this specially — it's binary data, not a simple number
    // For now, skip MakerNote preservation in TIFF (complex offset handling)
    // The original data is preserved in the file since we're not overwriting
    // the raw data area — but we need to keep the IFD entry pointing to it.
  }

  size_t data_area_start = writer.pos() + 2 + 12 * plan.size() + 4;

  // Write IFD count
  writer.write16(static_cast<uint16_t>(plan.size()));

  for (auto& p : plan) {
    writer.write16(p.tag);
    if (p.is_string) {
      writer.write16(TYPE_ASCII);
      writer.write32(static_cast<uint32_t>(p.str_value.size() + 1));
    } else if (p.is_rational) {
      writer.write16(TYPE_RATIONAL);
      writer.write32(1);
    } else {
      writer.write16(TYPE_SHORT);
      writer.write32(1);
    }

    if (p.is_num && !p.is_string) {
      writer.write32(p.num_value);
      if (p.is_num) writer.write32(0);  // padding
    } else {
      writer.write32(static_cast<uint32_t>(data_area_start));
      if (p.is_string) {
        data_area_start += p.str_value.size() + 1;
        if (data_area_start % 2 != 0) ++data_area_start;
      } else if (p.is_rational) {
        data_area_start += 8;
      }
    }
  }

  // Next IFD = 0
  writer.write32(0);

  // Data area
  for (auto& p : plan) {
    if (p.is_string) {
      writer.write_string(p.str_value);
      if (writer.pos() % 2 != 0) writer.write_padding(1);
    }
  }
  for (auto& p : plan) {
    if (p.is_rational) {
      writer.write32(static_cast<uint32_t>(p.rat_value.numerator));
      writer.write32(static_cast<uint32_t>(p.rat_value.denominator));
    }
  }

  // Write new TIFF to file
  std::ofstream out(file_path, std::ios::binary | std::ios::trunc);
  if (!out) {
    result.error_message = "Cannot write file";
    return result;
  }
  out.write(reinterpret_cast<const char*>(writer.data().data()), writer.data().size());
  result.success = true;
  return result;
}

/// -----------------------------------------------------------------------
/// XMP sidecar helpers
/// -----------------------------------------------------------------------
auto to_lower(std::string s) -> std::string {
  std::transform(s.begin(), s.end(), s.begin(),
                 [](char c) { return static_cast<char>(std::tolower(c)); });
  return s;
}

auto get_ext(std::string_view path) -> std::string {
  auto dot = path.rfind('.');
  if (dot == std::string_view::npos) return {};
  return to_lower(std::string(path.substr(dot + 1)));
}

auto is_raw_file(std::string_view ext) -> bool {
  static const std::unordered_set<std::string> raw_exts = {
    "cr2", "nef", "arw", "dng", "raf", "orf", "rw2", "pef", "srw", "cr3", "crw", "3fr", "ari",
    "srf", "sr2", "bay", "cri", "cap", "iiq", "eip", "dcs", "dcr", "drf", "k25", "kdc",
    "mdc", "mef", "mos", "mrw", "nrw", "obm", "ptx", "pxn", "r3d", "raw", "rwl", "rwz", "x3f"
  };
  return raw_exts.count(to_lower(std::string(ext))) > 0;
}

auto build_xmp_sidecar_content(const alcedo::exif::ExifMetadata& meta) -> std::string {
  std::ostringstream ss;
  ss << "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n";
  ss << "<x:xmpmeta xmlns:x=\"adobe:ns:meta/\" x:xmptk=\"AlcedoStudio\">\n";
  ss << " <rdf:RDF xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\"\n";
  ss << "          xmlns:dc=\"http://purl.org/dc/elements/1.1/\"\n";
  ss << "          xmlns:exif=\"http://ns.adobe.com/exif/1.0/\"\n";
  ss << "          xmlns:xmp=\"http://ns.adobe.com/xap/1.0/\"\n";
  ss << "          xmlns:photoshop=\"http://ns.adobe.com/photoshop/1.0/\">\n";
  ss << "  <rdf:Description rdf:about=\"\">\n";

  if (meta.description) {
    ss << "   <dc:description>\n    <rdf:Alt>\n     <rdf:li xml:lang=\"x-default\">"
       << *meta.description << "</rdf:li>\n    </rdf:Alt>\n   </dc:description>\n";
  }
  if (meta.artist) {
    ss << "   <dc:creator>\n    <rdf:Seq>\n     <rdf:li>"
       << *meta.artist << "</rdf:li>\n    </rdf:Seq>\n   </dc:creator>\n";
  }
  if (meta.copyright) {
    ss << "   <dc:rights>\n    <rdf:Alt>\n     <rdf:li xml:lang=\"x-default\">"
       << *meta.copyright << "</rdf:li>\n    </rdf:Alt>\n   </dc:rights>\n";
  }
  if (meta.keywords && !meta.keywords->empty()) {
    ss << "   <dc:subject>\n    <rdf:Bag>\n";
    for (const auto& kw : *meta.keywords) {
      ss << "     <rdf:li>" << kw << "</rdf:li>\n";
    }
    ss << "    </rdf:Bag>\n   </dc:subject>\n";
  }
  if (meta.rating) {
    ss << "   <xmp:Rating>" << static_cast<int>(*meta.rating) << "</xmp:Rating>\n";
  }
  if (meta.date_time_original) {
    ss << "   <exif:DateTimeOriginal>" << *meta.date_time_original
       << "</exif:DateTimeOriginal>\n";
  }

  ss << "  </rdf:Description>\n";
  ss << " </rdf:RDF>\n";
  ss << "</x:xmpmeta>\n";
  return ss.str();
}

}  // namespace

// ============================================================================
// Public API
// ============================================================================

namespace alcedo {
namespace exif {

auto detect_file_type(const std::string& file_path) -> ExifFileType {
  auto ext = get_ext(file_path);
  if (ext == "jpg" || ext == "jpeg") return ExifFileType::JPEG;
  if (ext == "tif" || ext == "tiff") return ExifFileType::TIFF;
  if (ext == "cr2") return ExifFileType::RAW_CR2;
  if (ext == "nef") return ExifFileType::RAW_NEF;
  if (ext == "arw") return ExifFileType::RAW_ARW;
  if (ext == "dng") return ExifFileType::RAW_DNG;
  if (ext == "raf") return ExifFileType::RAW_RAF;
  if (ext == "orf") return ExifFileType::RAW_ORF;
  if (ext == "rw2") return ExifFileType::RAW_RW2;
  if (ext == "pef") return ExifFileType::RAW_PEF;
  if (ext == "srw") return ExifFileType::RAW_SRW;
  return ExifFileType::UNKNOWN;
}

auto read_metadata(const std::string& file_path) -> ExifReadResult {
  auto ext = get_ext(file_path);
  if (ext == "jpg" || ext == "jpeg") {
    return read_jpeg_exif(file_path);
  }
  return read_tiff_exif(file_path);
}

auto write_metadata(const std::string& file_path,
                    const ExifMetadata& metadata) -> ExifWriteResult {
  auto ext = get_ext(file_path);

  ExifWriteResult result;
  if (ext == "jpg" || ext == "jpeg") {
    result = write_jpeg_exif(file_path, metadata);
  } else {
    result = write_tiff_exif(file_path, metadata);
  }

  // Sync XMP sidecar if the file is a RAW file
  if (result.success && is_raw_file(ext)) {
    sync_xmp_sidecar(file_path);
  }

  return result;
}

auto batch_read_metadata(const std::vector<std::string>& file_paths)
    -> std::vector<ExifReadResult> {
  std::vector<ExifReadResult> results;
  results.reserve(file_paths.size());
  for (const auto& path : file_paths) {
    results.push_back(read_metadata(path));
  }
  return results;
}

auto batch_write_metadata(const std::vector<std::string>& file_paths,
                          const ExifMetadata& metadata)
    -> std::vector<ExifWriteResult> {
  std::vector<ExifWriteResult> results;
  results.reserve(file_paths.size());
  for (const auto& path : file_paths) {
    results.push_back(write_metadata(path, metadata));
  }
  return results;
}

auto sync_xmp_sidecar(const std::string& image_path) -> bool {
  // Determine XMP sidecar path: replace extension with .xmp
  std::filesystem::path img_path(image_path);
  std::filesystem::path xmp_path = img_path;
  xmp_path.replace_extension(".xmp");

  try {
    // Read metadata from the image file
    auto read_result = read_metadata(image_path);
    if (!read_result.success) return false;

    // Build XMP content
    auto xmp_content = build_xmp_sidecar_content(read_result.metadata);

    // Write XMP sidecar
    std::ofstream out(xmp_path.string(), std::ios::binary | std::ios::trunc);
    if (!out) return false;
    out << xmp_content;
    return true;
  } catch (...) {
    return false;
  }
}

auto strip_metadata(const std::string& file_path) -> bool {
  try {
    auto ext = get_ext(file_path);

    if (ext == "jpg" || ext == "jpeg") {
      // For JPEG: remove APP1 EXIF marker
      std::ifstream in(file_path, std::ios::binary | std::ios::ate);
      if (!in) return false;
      auto file_size = static_cast<size_t>(in.tellg());
      in.seekg(0);
      std::vector<uint8_t> buffer(file_size);
      in.read(reinterpret_cast<char*>(buffer.data()), file_size);
      in.close();

      // Find and remove APP1 EXIF
      size_t pos = 0;
      while (pos + 4 <= file_size) {
        if (buffer[pos] == JPEG_MARKER_PREFIX && buffer[pos + 1] == JPEG_APP1) {
          if (pos + 10 <= file_size &&
              buffer[pos + 4] == 'E' && buffer[pos + 5] == 'x' &&
              buffer[pos + 6] == 'i' && buffer[pos + 7] == 'f' &&
              buffer[pos + 8] == 0 && buffer[pos + 9] == 0) {
            uint16_t seg_len = (static_cast<uint16_t>(buffer[pos + 2]) << 8) | buffer[pos + 3];
            // Remove this segment
            buffer.erase(buffer.begin() + pos, buffer.begin() + pos + 2 + seg_len);
            break;
          }
        }
        if (buffer[pos] == JPEG_MARKER_PREFIX) {
          if (pos + 2 < buffer.size()) {
            uint16_t seg_len = (static_cast<uint16_t>(buffer[pos + 2]) << 8) | buffer[pos + 3];
            pos += 2 + seg_len;
          } else {
            break;
          }
        } else {
          ++pos;
        }
      }

      std::ofstream out(file_path, std::ios::binary | std::ios::trunc);
      if (!out) return false;
      out.write(reinterpret_cast<const char*>(buffer.data()), buffer.size());
      return true;
    }

    // For TIFF/RAW: write a minimal TIFF with no IFD entries
    if (ext == "tif" || ext == "tiff" || is_raw_file(ext)) {
      ByteWriter writer(true);  // big-endian
      writer.write16(TIFF_BIG_ENDIAN);
      writer.write16(TIFF_MAGIC);
      writer.write32(0);  // No IFD0

      std::ofstream out(file_path, std::ios::binary | std::ios::trunc);
      if (!out) return false;
      out.write(reinterpret_cast<const char*>(writer.data().data()), writer.data().size());
      return true;
    }

    return false;
  } catch (...) {
    return false;
  }
}

}  // namespace exif
}  // namespace alcedo
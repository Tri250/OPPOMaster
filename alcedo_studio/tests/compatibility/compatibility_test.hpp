//  Copyright 2026 Yurun Zi
//  SPDX-License-Identifier: GPL-3.0-only
//  Additional permission under GPLv3 section 7 applies; see the LICENSE file.

#pragma once

#include <cstdint>
#include <string>
#include <vector>

namespace alcedo::compat {

// ---------------------------------------------------------------------------
// PlatformFeature — enumerates cross-platform capabilities that the
// compatibility test suite validates. Each value corresponds to a testable
// feature that may or may not be available depending on the runtime
// environment (OS, GPU driver, display, etc.).
// ---------------------------------------------------------------------------
enum class PlatformFeature {
  // GPU backends
  GpuBackendCuda,
  GpuBackendOpenCL,
  GpuBackendMetal,
  GpuBackendOpenGLES,

  // Color management
  ColorManagementICC,          // ICC profile reading & transform
  ColorManagementColorSync,    // macOS ColorSync integration
  ColorManagementWCS,          // Windows Color System (ICM/WCS)

  // HDR display
  HdrDisplayDetection,         // Can detect HDR display capabilities
  HdrDisplayOutput,            // Can output HDR content to display
  HdrToneMapping,              // Software HDR<->SDR tone mapping

  // File system
  FileSystemCaseSensitive,     // File system is case-sensitive
  FileSystemUnicodePaths,      // Full Unicode (CJK, emoji, etc.) path support
  FileSystemLongPaths,         // Paths > 260 chars (Windows)

  // Memory
  MemoryUnified,               // Unified / shared GPU+CPU memory (Apple Silicon)
  MemoryLargeAddress,          // 64-bit address space available

  // Credential store
  CredentialStoreNative,       // OS-native secure credential storage (WinCred/Keychain)
  CredentialStoreFallback,     // In-memory fallback for Linux

  // AI services
  AiServiceSidecar,            // Sidecar runtime available
  AiServiceOffline,            // Offline inference available

  // RAW format support
  RawFormatCanonCR2,
  RawFormatCanonCR3,
  RawFormatNikonNEF,
  RawFormatNikonNRW,
  RawFormatSonyARW,
  RawFormatFujiRAF,
  RawFormatPanasonicRW2,
  RawFormatOlympusORF,
  RawFormatLeicaDNG,
  RawFormatPentaxPEF,
  RawFormatPhaseOneIIQ,
  RawFormatSamsungSRW,
  RawFormatHasselblad3FR,

  // Display
  DisplayDPIAwareness,         // High-DPI / scaling awareness
  DisplayMultiMonitor,         // Multi-monitor ICC profile support

  // Audio/video codec
  CodecHEVC,                   // H.265 / HEVC decode
  CodecAV1,                    // AV1 decode
  CodecProRes,                 // Apple ProRes decode
};

// ---------------------------------------------------------------------------
// CompatibilityTestResult — outcome of a single compatibility test.
// ---------------------------------------------------------------------------
struct CompatibilityTestResult {
  PlatformFeature feature;
  bool            supported     = false;
  bool            passed        = false;  // true if the test ran without error
  std::string     detail;                 // Human-readable explanation
  std::string     platform_note;          // Platform-specific caveat or workaround

  [[nodiscard]] auto Ok() const -> bool { return passed && supported; }
};

// ---------------------------------------------------------------------------
// PlatformCompatTestSuite — runs the full compatibility test battery and
// collects results. Designed for use within GTest fixtures but also usable
// standalone.
// ---------------------------------------------------------------------------
class PlatformCompatTestSuite {
 public:
  PlatformCompatTestSuite() = default;

  // Run all compatibility tests and populate `results_`.
  void RunAll();

  // Run only the tests that apply to the current platform.
  void RunPlatformRelevant();

  // Query results.
  [[nodiscard]] auto Results() const -> const std::vector<CompatibilityTestResult>&;
  [[nodiscard]] auto ResultFor(PlatformFeature feature) const -> const CompatibilityTestResult*;
  [[nodiscard]] auto AllPassed() const -> bool;
  [[nodiscard]] auto FailedCount() const -> size_t;

  // Individual test functions. Each returns a CompatibilityTestResult.
  static auto TestGpuBackendAvailability()      -> CompatibilityTestResult;
  static auto TestColorManagementSupport()      -> CompatibilityTestResult;
  static auto TestHdrDisplaySupport()           -> CompatibilityTestResult;
  static auto TestFileSystemFeatures()          -> CompatibilityTestResult;
  static auto TestMemoryLimits()                -> CompatibilityTestResult;
  static auto TestUnicodeSupport()              -> CompatibilityTestResult;
  static auto TestIccProfileHandling()          -> CompatibilityTestResult;
  static auto TestAiServiceAvailability()       -> CompatibilityTestResult;
  static auto TestRawFormatSupportMatrix()      -> CompatibilityTestResult;
  static auto TestScreenDpiAndScaling()         -> CompatibilityTestResult;
  static auto TestAudioVideoCodecAvailability() -> CompatibilityTestResult;

 private:
  std::vector<CompatibilityTestResult> results_;
};

// ---------------------------------------------------------------------------
// RawFormatInfo — describes a camera RAW format for compatibility testing.
// ---------------------------------------------------------------------------
struct RawFormatInfo {
  const char* format_name;     // e.g. "Canon CR3"
  const char* extension;       // e.g. ".cr3"
  const char* brand;           // e.g. "Canon"
  uint32_t    magic_bytes[4];  // First 4 bytes of file magic (0 = wildcard)
  size_t      magic_offset;    // Byte offset of the magic number
  bool        is_tiff_based;   // Whether the format wraps TIFF
};

// Known RAW format definitions used by the test suite.
const RawFormatInfo kKnownRawFormats[] = {
    {"Canon CR2",          ".cr2",  "Canon",     {0x49, 0x49, 0x2A, 0x00}, 0,  true},
    {"Canon CR3",          ".cr3",  "Canon",     {0x00, 0x00, 0x00, 0x00}, 0,  false},  // ISOBMFF
    {"Nikon NEF",          ".nef",  "Nikon",     {0x49, 0x49, 0x2A, 0x00}, 0,  true},
    {"Nikon NRW",          ".nrw",  "Nikon",     {0x49, 0x49, 0x2A, 0x00}, 0,  true},
    {"Sony ARW",           ".arw",  "Sony",      {0x49, 0x49, 0x2A, 0x00}, 0,  true},
    {"Fuji RAF",           ".raf",  "Fujifilm",  {0x46, 0x55, 0x4A, 0x49}, 0,  false},  // "FUJI"
    {"Panasonic RW2",      ".rw2",  "Panasonic", {0x49, 0x49, 0x2A, 0x00}, 0,  true},
    {"Olympus ORF",        ".orf",  "Olympus",   {0x49, 0x49, 0x52, 0x4F}, 0,  false},  // "IIRO"
    {"Leica DNG",          ".dng",  "Leica",     {0x49, 0x49, 0x2A, 0x00}, 0,  true},
    {"Pentax PEF",         ".pef",  "Pentax",    {0x49, 0x49, 0x2A, 0x00}, 0,  true},
    {"Phase One IIQ",      ".iiq",  "Phase One", {0x49, 0x49, 0x2A, 0x00}, 0,  true},
    {"Samsung SRW",        ".srw",  "Samsung",   {0x49, 0x49, 0x2A, 0x00}, 0,  true},
    {"Hasselblad 3FR",     ".3fr",  "Hasselblad",{0x49, 0x49, 0x2A, 0x00}, 0,  true},
};

constexpr size_t kKnownRawFormatCount = sizeof(kKnownRawFormats) / sizeof(kKnownRawFormats[0]);

}  // namespace alcedo::compat

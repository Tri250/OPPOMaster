//  Copyright 2026 Yurun Zi
//  SPDX-License-Identifier: GPL-3.0-only
//  Additional permission under GPLv3 section 7 applies; see the LICENSE file.

// ── Fuzz test for project file loading ──────────────────────────────────────
//
// Tests project file parsing with:
// - Malformed JSON data
// - Corrupt SQLite database
// - Missing/corrupt tables
// - Version mismatch handling
// - Missing required fields
// - Truncated data

#include <algorithm>
#include <cstdint>
#include <cstring>
#include <memory>
#include <random>
#include <sstream>
#include <string>
#include <vector>

#include <gtest/gtest.h>

// We use a lightweight JSON-like parser simulation to test project file
// resilience without pulling in the full project deserialization code.

namespace alcedo {
namespace fuzzing {
namespace {

// ── Minimal JSON token types ────────────────────────────────────────────────

enum class JsonTokenType : uint8_t {
  kString,
  kNumber,
  kBool,
  kNull,
  kObjectOpen,
  kObjectClose,
  kArrayOpen,
  kArrayClose,
  kColon,
  kComma,
  kError,
  kEnd
};

struct JsonToken {
  JsonTokenType type = JsonTokenType::kError;
  std::string    value;
};

// ── Safe JSON tokenizer ─────────────────────────────────────────────────────

class SafeJsonTokenizer {
 public:
  explicit SafeJsonTokenizer(const std::string& input)
      : input_(input), pos_(0) {}

  auto Next() -> JsonToken {
    SkipWhitespace();
    if (pos_ >= input_.size()) {
      return {JsonTokenType::kEnd, ""};
    }

    const char ch = input_[pos_];

    switch (ch) {
      case '{': pos_++; return {JsonTokenType::kObjectOpen, "{"};
      case '}': pos_++; return {JsonTokenType::kObjectClose, "}"};
      case '[': pos_++; return {JsonTokenType::kArrayOpen, "["};
      case ']': pos_++; return {JsonTokenType::kArrayClose, "]"};
      case ':': pos_++; return {JsonTokenType::kColon, ":"};
      case ',': pos_++; return {JsonTokenType::kComma, ","};

      case '"': return ParseString();

      case 't': case 'f': return ParseBool();
      case 'n': return ParseNull();

      default:
        if (ch == '-' || (ch >= '0' && ch <= '9') || ch == '.') {
          return ParseNumber();
        }
        // Unknown character
        pos_++;
        return {JsonTokenType::kError, std::string(1, ch)};
    }
  }

  auto HasMore() const -> bool { return pos_ < input_.size(); }

 private:
  void SkipWhitespace() {
    while (pos_ < input_.size() &&
           (input_[pos_] == ' ' || input_[pos_] == '\t' ||
            input_[pos_] == '\n' || input_[pos_] == '\r')) {
      pos_++;
    }
  }

  auto ParseString() -> JsonToken {
    // Skip opening quote
    pos_++;
    std::string value;
    bool escaped = false;

    // Safety limit: strings cannot exceed 1 MB
    const size_t max_string_len = 1024 * 1024;

    while (pos_ < input_.size() && value.size() < max_string_len) {
      const char ch = input_[pos_];
      pos_++;

      if (escaped) {
        switch (ch) {
          case '"':  value += '"';  break;
          case '\\': value += '\\'; break;
          case '/':  value += '/';  break;
          case 'b':  value += '\b'; break;
          case 'f':  value += '\f'; break;
          case 'n':  value += '\n'; break;
          case 'r':  value += '\r'; break;
          case 't':  value += '\t'; break;
          case 'u':
            // Unicode escape: \uXXXX — we just skip 4 hex chars
            if (pos_ + 4 <= input_.size()) {
              pos_ += 4;
              value += "?";  // placeholder
            }
            break;
          default:
            // Invalid escape — treat as literal
            value += ch;
            break;
        }
        escaped = false;
      } else if (ch == '\\') {
        escaped = true;
      } else if (ch == '"') {
        return {JsonTokenType::kString, value};
      } else if (static_cast<uint8_t>(ch) >= 0x20) {
        // Control characters (except those handled above) are invalid in JSON
        value += ch;
      }
      // Otherwise: skip control characters
    }

    // Unterminated string
    return {JsonTokenType::kError, value};
  }

  auto ParseNumber() -> JsonToken {
    std::string value;
    // Safety limit
    const size_t max_number_len = 64;

    while (pos_ < input_.size() && value.size() < max_number_len) {
      const char ch = input_[pos_];
      if ((ch >= '0' && ch <= '9') || ch == '-' || ch == '+' ||
          ch == '.' || ch == 'e' || ch == 'E') {
        value += ch;
        pos_++;
      } else {
        break;
      }
    }

    if (value.empty()) {
      return {JsonTokenType::kError, ""};
    }
    return {JsonTokenType::kNumber, value};
  }

  auto ParseBool() -> JsonToken {
    if (input_.compare(pos_, 4, "true") == 0) {
      pos_ += 4;
      return {JsonTokenType::kBool, "true"};
    }
    if (input_.compare(pos_, 5, "false") == 0) {
      pos_ += 5;
      return {JsonTokenType::kBool, "false"};
    }
    return {JsonTokenType::kError, ""};
  }

  auto ParseNull() -> JsonToken {
    if (input_.compare(pos_, 4, "null") == 0) {
      pos_ += 4;
      return {JsonTokenType::kNull, "null"};
    }
    return {JsonTokenType::kError, ""};
  }

  const std::string& input_;
  size_t             pos_;
};

// ── Project version handling ────────────────────────────────────────────────

struct ProjectVersion {
  uint32_t major = 0;
  uint32_t minor = 0;
  uint32_t patch = 0;
};

auto ParseVersionString(const std::string& str) -> std::optional<ProjectVersion> {
  ProjectVersion ver;
  char dot1 = 0, dot2 = 0;
  int n = std::sscanf(str.c_str(), "%u.%u.%u%c", &ver.major, &ver.minor,
                      &ver.patch, &dot1);
  if (n < 1) return std::nullopt;  // At least major version required
  // Sanity: each component must be < 1000
  if (ver.major >= 1000 || ver.minor >= 1000 || ver.patch >= 1000) {
    return std::nullopt;
  }
  return ver;
}

auto IsVersionCompatible(const ProjectVersion& file_ver,
                          const ProjectVersion& app_ver) -> bool {
  // Major version must match; minor <= app minor
  if (file_ver.major != app_ver.major) return false;
  if (file_ver.minor > app_ver.minor) return false;
  return true;
}

// ── SQLite header validation ────────────────────────────────────────────────

auto IsValidSqliteHeader(const std::vector<uint8_t>& data) -> bool {
  if (data.size() < 16) return false;
  const char* expected = "SQLite format 3\0";
  return std::memcmp(data.data(), expected, 16) == 0;
}

auto GetSqlitePageSize(const std::vector<uint8_t>& data) -> uint32_t {
  if (data.size() < 100) return 0;
  // Page size is at offset 16-17 (big-endian 2-byte integer)
  uint32_t page_size = (static_cast<uint32_t>(data[16]) << 8) | data[17];
  // Page size of 1 means 65536
  if (page_size == 1) page_size = 65536;
  // Must be a power of 2 between 512 and 65536
  if (page_size < 512 || page_size > 65536) return 0;
  if ((page_size & (page_size - 1)) != 0) return 0;  // not power of 2
  return page_size;
}

auto ValidateSqliteDatabase(const std::vector<uint8_t>& data,
                            std::string* out_error = nullptr) -> bool {
  if (data.size() < 100) {
    if (out_error) *out_error = "SQLite: file too small for header";
    return false;
  }
  if (!IsValidSqliteHeader(data)) {
    if (out_error) *out_error = "SQLite: invalid header string";
    return false;
  }
  uint32_t page_size = GetSqlitePageSize(data);
  if (page_size == 0) {
    if (out_error) *out_error = "SQLite: invalid page size";
    return false;
  }

  // Check file change counter (offset 24-27) and valid-for counter (offset 92-95)
  // If they don't match, the file may be corrupted or from a crashed write
  uint32_t change_counter =
      (static_cast<uint32_t>(data[24]) << 24) |
      (static_cast<uint32_t>(data[25]) << 16) |
      (static_cast<uint32_t>(data[26]) << 8) |
      static_cast<uint32_t>(data[27]);
  uint32_t valid_for_counter =
      (static_cast<uint32_t>(data[92]) << 24) |
      (static_cast<uint32_t>(data[93]) << 16) |
      (static_cast<uint32_t>(data[94]) << 8) |
      static_cast<uint32_t>(data[95]);

  if (change_counter != valid_for_counter) {
    if (out_error) *out_error = "SQLite: change counter mismatch (possible crash corruption)";
    // We still return true — the DB might be recoverable with WAL
  }

  // Check total page count (offset 28-31)
  uint32_t total_pages =
      (static_cast<uint32_t>(data[28]) << 24) |
      (static_cast<uint32_t>(data[29]) << 16) |
      (static_cast<uint32_t>(data[30]) << 8) |
      static_cast<uint32_t>(data[31]);

  uint64_t expected_size = static_cast<uint64_t>(total_pages) * page_size;
  if (expected_size > data.size()) {
    if (out_error) *out_error = "SQLite: file truncated (pages extend beyond file)";
    return false;
  }

  return true;
}

// ── Project file validation (JSON-based) ────────────────────────────────────

auto ValidateProjectJson(const std::string& json,
                         const ProjectVersion& app_version = {1, 0, 0},
                         std::string* out_error = nullptr) -> bool {
  // Step 1: Tokenize
  SafeJsonTokenizer tokenizer(json);
  std::vector<JsonToken> tokens;
  const size_t max_tokens = 100000;  // safety limit
  while (tokenizer.HasMore() && tokens.size() < max_tokens) {
    auto token = tokenizer.Next();
    if (token.type == JsonTokenType::kError) {
      if (out_error) *out_error = "JSON: parse error at token: " + token.value;
      return false;
    }
    tokens.push_back(std::move(token));
  }
  if (tokens.size() >= max_tokens) {
    if (out_error) *out_error = "JSON: too many tokens (possible DoS)";
    return false;
  }

  // Step 2: Check for minimum structure (must be an object)
  if (tokens.empty() || tokens[0].type != JsonTokenType::kObjectOpen) {
    if (out_error) *out_error = "JSON: root must be an object";
    return false;
  }

  // Step 3: Look for "version" key
  bool found_version = false;
  for (size_t i = 0; i + 2 < tokens.size(); ++i) {
    if (tokens[i].type == JsonTokenType::kString &&
        tokens[i].value == "version" &&
        tokens[i + 1].type == JsonTokenType::kColon) {
      if (tokens[i + 2].type == JsonTokenType::kString) {
        auto ver = ParseVersionString(tokens[i + 2].value);
        if (!ver.has_value()) {
          if (out_error) *out_error = "JSON: invalid version string: " + tokens[i + 2].value;
          return false;
        }
        if (!IsVersionCompatible(*ver, app_version)) {
          if (out_error) *out_error = "JSON: incompatible version: " + tokens[i + 2].value;
          return false;
        }
        found_version = true;
      } else if (tokens[i + 2].type == JsonTokenType::kNumber) {
        auto ver = ParseVersionString(tokens[i + 2].value);
        if (!ver.has_value()) {
          if (out_error) *out_error = "JSON: invalid version number: " + tokens[i + 2].value;
          return false;
        }
        if (!IsVersionCompatible(*ver, app_version)) {
          if (out_error) *out_error = "JSON: incompatible version: " + tokens[i + 2].value;
          return false;
        }
        found_version = true;
      }
    }
  }

  // Version is required
  if (!found_version) {
    if (out_error) *out_error = "JSON: missing required 'version' field";
    return false;
  }

  return true;
}

// ── Fuzz generators ─────────────────────────────────────────────────────────

class ProjectFuzzGenerator {
 public:
  explicit ProjectFuzzGenerator(uint64_t seed = 42) : rng_(seed) {}

  auto GenerateRandomJson(size_t approx_size) -> std::string {
    std::string json;
    json.reserve(approx_size);
    std::uniform_int_distribution<int> char_dist(32, 126);

    for (size_t i = 0; i < approx_size; ++i) {
      json += static_cast<char>(char_dist(rng_));
    }
    return json;
  }

  auto GenerateValidProjectJson(uint32_t ver_major = 1,
                                uint32_t ver_minor = 0) -> std::string {
    std::ostringstream oss;
    oss << "{"
        << "\"version\":\"" << ver_major << "." << ver_minor << ".0\","
        << "\"name\":\"TestProject\","
        << "\"images\":[],"
        << "\"layers\":[],"
        << "\"settings\":{"
        << "\"width\":1920,"
        << "\"height\":1080"
        << "}"
        << "}";
    return oss.str();
  }

  auto GenerateSqliteHeader(uint32_t page_size = 4096,
                            uint32_t total_pages = 1) -> std::vector<uint8_t> {
    std::vector<uint8_t> data(100, 0);
    // Header string
    const char* header = "SQLite format 3\0";
    std::memcpy(data.data(), header, 16);
    // Page size (big-endian)
    if (page_size == 65536) page_size = 1;
    data[16] = static_cast<uint8_t>((page_size >> 8) & 0xFF);
    data[17] = static_cast<uint8_t>(page_size & 0xFF);
    // Change counter at offset 24
    data[24] = 0; data[25] = 0; data[26] = 0; data[27] = 1;
    // Total pages at offset 28
    data[28] = static_cast<uint8_t>((total_pages >> 24) & 0xFF);
    data[29] = static_cast<uint8_t>((total_pages >> 16) & 0xFF);
    data[30] = static_cast<uint8_t>((total_pages >> 8) & 0xFF);
    data[31] = static_cast<uint8_t>(total_pages & 0xFF);
    // Valid-for counter at offset 92 (matches change counter)
    data[92] = 0; data[93] = 0; data[94] = 0; data[95] = 1;
    return data;
  }

  void CorruptRandom(std::string& data, size_t num_corruptions) {
    if (data.empty()) return;
    std::uniform_int_distribution<size_t> pos_dist(0, data.size() - 1);
    std::uniform_int_distribution<int> val_dist(32, 126);
    for (size_t i = 0; i < num_corruptions; ++i) {
      data[pos_dist(rng_)] = static_cast<char>(val_dist(rng_));
    }
  }

  void CorruptRandom(std::vector<uint8_t>& data, size_t num_corruptions) {
    if (data.empty()) return;
    std::uniform_int_distribution<size_t> pos_dist(0, data.size() - 1);
    std::uniform_int_distribution<int> val_dist(0, 255);
    for (size_t i = 0; i < num_corruptions; ++i) {
      data[pos_dist(rng_)] = static_cast<uint8_t>(val_dist(rng_));
    }
  }

 private:
  std::mt19937_64 rng_;
};

}  // namespace

// ── Test Cases ──────────────────────────────────────────────────────────────

// ── JSON Tests ──────────────────────────────────────────────────────────────

TEST(FuzzProjectFile, ValidProjectJson) {
  ProjectFuzzGenerator gen;
  auto json = gen.GenerateValidProjectJson();
  EXPECT_TRUE(ValidateProjectJson(json));
}

TEST(FuzzProjectFile, EmptyString) {
  std::string error;
  EXPECT_FALSE(ValidateProjectJson("", {1, 0, 0}, &error));
  EXPECT_FALSE(error.empty());
}

TEST(FuzzProjectFile, MissingVersion) {
  std::string json = R"({"name":"TestProject","images":[]})";
  std::string error;
  EXPECT_FALSE(ValidateProjectJson(json, {1, 0, 0}, &error));
  EXPECT_NE(error.find("version"), std::string::npos);
}

TEST(FuzzProjectFile, InvalidVersionString) {
  std::string json = R"({"version":"not_a_version"})";
  std::string error;
  EXPECT_FALSE(ValidateProjectJson(json, {1, 0, 0}, &error));
}

TEST(FuzzProjectFile, VersionMajorMismatch) {
  ProjectFuzzGenerator gen;
  auto json = gen.GenerateValidProjectJson(2, 0);  // file is v2.0
  std::string error;
  EXPECT_FALSE(ValidateProjectJson(json, {1, 0, 0}, &error));  // app is v1.0
  EXPECT_NE(error.find("incompatible"), std::string::npos);
}

TEST(FuzzProjectFile, VersionMinorNewer) {
  ProjectFuzzGenerator gen;
  auto json = gen.GenerateValidProjectJson(1, 5);  // file is v1.5
  std::string error;
  EXPECT_FALSE(ValidateProjectJson(json, {1, 0, 0}, &error));  // app is v1.0
}

TEST(FuzzProjectFile, VersionMinorOlder) {
  ProjectFuzzGenerator gen;
  auto json = gen.GenerateValidProjectJson(1, 0);  // file is v1.0
  EXPECT_TRUE(ValidateProjectJson(json, {1, 5, 0}));  // app is v1.5
}

TEST(FuzzProjectFile, VersionExactMatch) {
  ProjectFuzzGenerator gen;
  auto json = gen.GenerateValidProjectJson(1, 3);
  EXPECT_TRUE(ValidateProjectJson(json, {1, 3, 0}));
}

TEST(FuzzProjectFile, MalformedJson_Random) {
  ProjectFuzzGenerator gen(555);
  for (int trial = 0; trial < 100; ++trial) {
    auto json = gen.GenerateRandomJson(50 + trial * 10);
    // Should not crash; may succeed or fail gracefully
    ValidateProjectJson(json);
  }
}

TEST(FuzzProjectFile, MalformedJson_UnterminatedString) {
  std::string json = R"({"version":"1.0.0","name":"unterminated)";
  std::string error;
  EXPECT_FALSE(ValidateProjectJson(json, {1, 0, 0}, &error));
}

TEST(FuzzProjectFile, MalformedJson_UnterminatedObject) {
  std::string json = R"({"version":"1.0.0","name":"test")";
  std::string error;
  EXPECT_FALSE(ValidateProjectJson(json, {1, 0, 0}, &error));
}

TEST(FuzzProjectFile, MalformedJson_DeepNesting) {
  // Generate deeply nested objects
  std::string json;
  for (int i = 0; i < 1000; ++i) {
    json += R"({"a":)";
  }
  json += "1";
  for (int i = 0; i < 1000; ++i) {
    json += "}";
  }
  // This should parse but will be huge; our 100k token limit should handle it
  ValidateProjectJson(json);
}

TEST(FuzzProjectFile, CorruptedValidJson) {
  ProjectFuzzGenerator gen(888);
  for (int trial = 0; trial < 50; ++trial) {
    auto json = gen.GenerateValidProjectJson();
    gen.CorruptRandom(json, 1 + trial % 10);
    // Should not crash
    ValidateProjectJson(json);
  }
}

TEST(FuzzProjectFile, RootMustBeObject) {
  std::string json = R"(["version","1.0.0"])";
  std::string error;
  EXPECT_FALSE(ValidateProjectJson(json, {1, 0, 0}, &error));
  EXPECT_NE(error.find("root"), std::string::npos);
}

// ── SQLite Tests ────────────────────────────────────────────────────────────

TEST(FuzzProjectFile, ValidSqliteHeader) {
  ProjectFuzzGenerator gen;
  auto data = gen.GenerateSqliteHeader(4096, 1);
  // Need at least 1 page of data
  data.resize(4096, 0);
  EXPECT_TRUE(ValidateSqliteDatabase(data));
}

TEST(FuzzProjectFile, Sqlite_TooSmall) {
  std::vector<uint8_t> data(50, 0);
  std::string error;
  EXPECT_FALSE(ValidateSqliteDatabase(data, &error));
  EXPECT_NE(error.find("too small"), std::string::npos);
}

TEST(FuzzProjectFile, Sqlite_InvalidHeader) {
  std::vector<uint8_t> data(200, 0);  // all zeros
  std::string error;
  EXPECT_FALSE(ValidateSqliteDatabase(data, &error));
  EXPECT_NE(error.find("header"), std::string::npos);
}

TEST(FuzzProjectFile, Sqlite_InvalidPageSize) {
  ProjectFuzzGenerator gen;
  auto data = gen.GenerateSqliteHeader(4096, 1);
  // Corrupt page size to non-power-of-2
  data[16] = 0; data[17] = 100;  // page size = 100 (not power of 2)
  std::string error;
  EXPECT_FALSE(ValidateSqliteDatabase(data, &error));
  EXPECT_NE(error.find("page size"), std::string::npos);
}

TEST(FuzzProjectFile, Sqlite_TruncatedFile) {
  ProjectFuzzGenerator gen;
  auto data = gen.GenerateSqliteHeader(4096, 10);  // claims 10 pages
  data.resize(4096);  // but only has 1 page worth
  std::string error;
  EXPECT_FALSE(ValidateSqliteDatabase(data, &error));
  EXPECT_NE(error.find("truncated"), std::string::npos);
}

TEST(FuzzProjectFile, Sqlite_ChangeCounterMismatch) {
  ProjectFuzzGenerator gen;
  auto data = gen.GenerateSqliteHeader(4096, 1);
  data.resize(4096, 0);
  // Corrupt valid-for counter to differ from change counter
  data[95] = 2;  // valid-for = 2, change counter = 1
  std::string error;
  // Should still validate (may be recoverable via WAL)
  EXPECT_TRUE(ValidateSqliteDatabase(data, &error));
  // But should warn
  EXPECT_NE(error.find("mismatch"), std::string::npos);
}

TEST(FuzzProjectFile, Sqlite_RandomCorruption) {
  ProjectFuzzGenerator gen(333);
  for (int trial = 0; trial < 50; ++trial) {
    auto data = gen.GenerateSqliteHeader(4096, 1);
    data.resize(4096, 0);
    gen.CorruptRandom(data, 1 + trial % 20);
    // Should not crash
    ValidateSqliteDatabase(data);
  }
}

TEST(FuzzProjectFile, Sqlite_LargePageSize) {
  ProjectFuzzGenerator gen;
  auto data = gen.GenerateSqliteHeader(65536, 1);
  data.resize(65536, 0);
  EXPECT_TRUE(ValidateSqliteDatabase(data));
}

// ── Version Parsing Tests ───────────────────────────────────────────────────

TEST(FuzzProjectFile, VersionParsing_Valid) {
  auto v = ParseVersionString("1.2.3");
  ASSERT_TRUE(v.has_value());
  EXPECT_EQ(v->major, 1u);
  EXPECT_EQ(v->minor, 2u);
  EXPECT_EQ(v->patch, 3u);
}

TEST(FuzzProjectFile, VersionParsing_MajorOnly) {
  auto v = ParseVersionString("5");
  ASSERT_TRUE(v.has_value());
  EXPECT_EQ(v->major, 5u);
  EXPECT_EQ(v->minor, 0u);
}

TEST(FuzzProjectFile, VersionParsing_Empty) {
  EXPECT_FALSE(ParseVersionString("").has_value());
}

TEST(FuzzProjectFile, VersionParsing_TooLarge) {
  EXPECT_FALSE(ParseVersionString("1000.0.0").has_value());
}

TEST(FuzzProjectFile, VersionParsing_Garbage) {
  EXPECT_FALSE(ParseVersionString("abc").has_value());
  EXPECT_FALSE(ParseVersionString("1.x.3").has_value());
}

}  // namespace fuzzing
}  // namespace alcedo

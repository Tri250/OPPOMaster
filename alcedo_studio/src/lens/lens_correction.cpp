//  Copyright 2026 Yurun Zi
//  SPDX-License-Identifier: GPL-3.0-only
//  Additional permission under GPLv3 section 7 applies; see the LICENSE file.

#include "lens/lens_correction.hpp"

#include <algorithm>
#include <cctype>
#include <charconv>
#include <cmath>
#include <cstring>
#include <fstream>
#include <iostream>
#include <limits>
#include <mutex>
#include <numeric>
#include <sstream>
#include <thread>
#include <unordered_set>

namespace alcedo::lens {

// ═════════════════════════════════════════════════════════════════════════════
//  Internal constants
// ═════════════════════════════════════════════════════════════════════════════

namespace {

constexpr float kEpsilon             = 1e-7f;
constexpr float kFullFrameDiagonalMm = 43.2666153f;
constexpr float kDefaultFarDistanceM = 1000.0f;

// ═════════════════════════════════════════════════════════════════════════════
//  String utilities
// ═════════════════════════════════════════════════════════════════════════════

auto TrimWhitespace(std::string_view text) -> std::string {
  auto start = text.begin();
  auto end   = text.end();
  while (start < end && (std::isspace(static_cast<unsigned char>(*start)) != 0)) {
    ++start;
  }
  while (start < end && (std::isspace(static_cast<unsigned char>(*(end - 1))) != 0)) {
    --end;
  }
  return std::string(start, end);
}

auto ToLower(std::string text) -> std::string {
  std::transform(text.begin(), text.end(), text.begin(),
                 [](unsigned char c) { return static_cast<char>(std::tolower(c)); });
  return text;
}

auto LooseMatch(std::string_view candidate, std::string_view query) -> bool {
  if (query.empty()) {
    return true;
  }
  const auto can_lower = ToLower(std::string(candidate));
  const auto qry_lower = ToLower(std::string(query));
  if (can_lower.find(qry_lower) != std::string::npos) {
    return true;
  }
  // Also try matching just the short form (e.g. "Nikon" vs "Nikon Corporation")
  if (qry_lower.find(can_lower) != std::string::npos) {
    return true;
  }
  // Word-by-word: check if all query words appear in the candidate
  std::istringstream qry_stream(qry_lower);
  std::string        word;
  while (qry_stream >> word) {
    if (can_lower.find(word) == std::string::npos) {
      return false;
    }
  }
  return true;
}

auto ParseFloat(std::string_view text) -> std::optional<float> {
  if (text.empty()) {
    return std::nullopt;
  }
  float value = 0.0f;
  auto  result =
      std::from_chars(text.data(), text.data() + text.size(), value);
  if (result.ec == std::errc()) {
    return value;
  }
  return std::nullopt;
}

auto IsFinitePositive(float value) -> bool {
  return std::isfinite(value) && value > 0.0f;
}

// ═════════════════════════════════════════════════════════════════════════════
//  Minimal XML parser for Lensfun database files
// ═════════════════════════════════════════════════════════════════════════════

class XmlParser {
 public:
  enum class TokenType {
    BeginElement,
    EndElement,
    SelfClosingElement,
    Text,
    EndOfFile,
    Error,
  };

  struct Token {
    TokenType  type       = TokenType::Error;
    std::string name;       // element name
    std::string text;       // text content or attribute value
    std::unordered_map<std::string, std::string> attributes;
  };

  explicit XmlParser(std::string data) : data_(std::move(data)), pos_(0) {}

  auto Next() -> Token {
    SkipWhitespaceAndComments();
    if (pos_ >= data_.size()) {
      return Token{TokenType::EndOfFile};
    }

    if (data_[pos_] == '<') {
      const std::size_t end = data_.find('>', pos_);
      if (end == std::string::npos) {
        return Token{TokenType::Error};
      }
      const std::string_view tag_content(data_.data() + pos_ + 1, end - pos_ - 1);
      pos_ = end + 1;

      if (tag_content.empty()) {
        return Token{TokenType::Error};
      }

      // Closing tag: </name>
      if (tag_content[0] == '/') {
        Token t;
        t.type = TokenType::EndElement;
        t.name = std::string(tag_content.substr(1));
        return t;
      }

      // Self-closing: <name ... />
      bool self_closing = false;
      std::string_view content = tag_content;
      if (content.size() >= 2 && content[content.size() - 1] == '/' &&
          content[content.size() - 2] != ' ') {
        // Check if it ends with />
        std::string_view trimmed = content;
        while (!trimmed.empty() && trimmed.back() == ' ') {
          trimmed.remove_suffix(1);
        }
        if (!trimmed.empty() && trimmed.back() == '/') {
          self_closing = true;
          content      = trimmed.substr(0, trimmed.size() - 1);
          while (!content.empty() && content.back() == ' ') {
            content.remove_suffix(1);
          }
        }
      } else if (tag_content.size() >= 1 && tag_content.back() == '/') {
        self_closing = true;
        content      = tag_content.substr(0, tag_content.size() - 1);
      }

      Token t;
      t.type = self_closing ? TokenType::SelfClosingElement : TokenType::BeginElement;
      auto space_pos = content.find_first_of(" \t\r\n");
      if (space_pos == std::string_view::npos) {
        t.name = std::string(content);
      } else {
        t.name       = std::string(content.substr(0, space_pos));
        std::string_view attrs = content.substr(space_pos + 1);
        ParseAttributes(attrs, t.attributes);
      }
      return t;
    }

    // Text content
    const std::size_t next_tag = data_.find('<', pos_);
    std::string_view  text_content;
    if (next_tag == std::string::npos) {
      text_content = std::string_view(data_.data() + pos_, data_.size() - pos_);
      pos_         = data_.size();
    } else {
      text_content = std::string_view(data_.data() + pos_, next_tag - pos_);
      pos_         = next_tag;
    }
    Token t;
    t.type = TokenType::Text;
    t.text = TrimWhitespace(text_content);
    return t;
  }

  auto PeekNextTag() -> std::string {
    const std::size_t saved = pos_;
    while (pos_ < data_.size() && data_[pos_] != '<') {
      ++pos_;
    }
    if (pos_ >= data_.size()) {
      pos_ = saved;
      return {};
    }
    const std::size_t end = data_.find('>', pos_);
    if (end == std::string::npos) {
      pos_ = saved;
      return {};
    }
    std::string_view tag(data_.data() + pos_ + 1, end - pos_ - 1);
    pos_ = saved;
    if (tag.empty() || tag[0] == '/') {
      return {};
    }
    auto sp = tag.find_first_of(" \t\r\n/");
    if (sp != std::string_view::npos) {
      return std::string(tag.substr(0, sp));
    }
    return std::string(tag);
  }

  void SkipToEndOf(const std::string& element_name) {
    int depth = 1;
    while (depth > 0) {
      auto t = Next();
      if (t.type == TokenType::BeginElement || t.type == TokenType::SelfClosingElement) {
        if (t.name == element_name) {
          ++depth;
        }
      } else if (t.type == TokenType::EndElement) {
        if (t.name == element_name) {
          --depth;
        }
      } else if (t.type == TokenType::EndOfFile || t.type == TokenType::Error) {
        break;
      }
    }
  }

 private:
  void SkipWhitespaceAndComments() {
    while (pos_ < data_.size()) {
      // Skip whitespace
      if (std::isspace(static_cast<unsigned char>(data_[pos_])) != 0) {
        ++pos_;
        continue;
      }
      // Skip XML comments <!-- ... -->
      if (pos_ + 4 <= data_.size() && data_.compare(pos_, 4, "<!--") == 0) {
        const std::size_t end = data_.find("-->", pos_ + 4);
        if (end == std::string::npos) {
          pos_ = data_.size();
          return;
        }
        pos_ = end + 3;
        continue;
      }
      break;
    }
  }

  void ParseAttributes(std::string_view attrs,
                       std::unordered_map<std::string, std::string>& out) {
    while (!attrs.empty()) {
      // Skip whitespace
      while (!attrs.empty() && std::isspace(static_cast<unsigned char>(attrs.front())) != 0) {
        attrs.remove_prefix(1);
      }
      if (attrs.empty()) {
        break;
      }

      auto eq_pos = attrs.find('=');
      if (eq_pos == std::string_view::npos) {
        break;
      }

      std::string key = TrimWhitespace(attrs.substr(0, eq_pos));
      attrs.remove_prefix(eq_pos + 1);

      // Skip whitespace before value
      while (!attrs.empty() && std::isspace(static_cast<unsigned char>(attrs.front())) != 0) {
        attrs.remove_prefix(1);
      }
      if (attrs.empty()) {
        break;
      }

      std::string value;
      if (attrs.front() == '"' || attrs.front() == '\'') {
        const char quote = attrs.front();
        attrs.remove_prefix(1);
        auto close_pos = attrs.find(quote);
        if (close_pos == std::string_view::npos) {
          break;
        }
        value = std::string(attrs.substr(0, close_pos));
        attrs.remove_prefix(close_pos + 1);
      } else {
        auto space_pos = attrs.find_first_of(" \t\r\n");
        if (space_pos == std::string_view::npos) {
          value = std::string(attrs);
          attrs = {};
        } else {
          value = std::string(attrs.substr(0, space_pos));
          attrs.remove_prefix(space_pos);
        }
      }

      if (!key.empty()) {
        out[std::move(key)] = std::move(value);
      }
    }
  }

  std::string data_;
  std::size_t pos_;
};

// ═════════════════════════════════════════════════════════════════════════════
//  XML database loader
// ═════════════════════════════════════════════════════════════════════════════

auto GetTextContent(XmlParser& parser) -> std::string {
  auto t = parser.Next();
  if (t.type == XmlParser::TokenType::Text) {
    return t.text;
  }
  return {};
}

auto ParseProjectionType(const std::string& text) -> ProjectionType {
  const auto lower = ToLower(text);
  if (lower == "rectilinear") {
    return ProjectionType::Rectilinear;
  }
  if (lower == "fisheye") {
    return ProjectionType::Fisheye;
  }
  if (lower == "panoramic") {
    return ProjectionType::Panoramic;
  }
  if (lower == "equirectangular") {
    return ProjectionType::Equirectangular;
  }
  if (lower == "fisheye_orthographic" || lower == "orthographic") {
    return ProjectionType::FisheyeOrthographic;
  }
  if (lower == "fisheye_stereographic" || lower == "stereographic") {
    return ProjectionType::FisheyeStereographic;
  }
  if (lower == "fisheye_equisolid" || lower == "equisolid") {
    return ProjectionType::FisheyeEquisolid;
  }
  if (lower == "fisheye_thoby" || lower == "thoby") {
    return ProjectionType::FisheyeThoby;
  }
  return ProjectionType::Unknown;
}

auto ParseDistortionModel(const std::string& text) -> DistortionModel {
  const auto lower = ToLower(text);
  if (lower == "poly3") {
    return DistortionModel::Poly3;
  }
  if (lower == "poly5") {
    return DistortionModel::Poly5;
  }
  if (lower == "ptlens") {
    return DistortionModel::PtLens;
  }
  return DistortionModel::None;
}

auto ParseTcaModel(const std::string& text) -> TcaModel {
  const auto lower = ToLower(text);
  if (lower == "linear") {
    return TcaModel::Linear;
  }
  if (lower == "poly3") {
    return TcaModel::Poly3;
  }
  return TcaModel::None;
}

void ParseCamera(XmlParser& parser, std::vector<CameraEntry>& cameras) {
  CameraEntry cam;
  std::string current_element;

  for (;;) {
    auto t = parser.Next();
    if (t.type == XmlParser::TokenType::EndElement) {
      if (t.name == "camera") {
        break;
      }
      current_element.clear();
      continue;
    }
    if (t.type == XmlParser::TokenType::BeginElement) {
      current_element = t.name;
      if (t.name == "maker") {
        auto text = GetTextContent(parser);
        if (!text.empty() && cam.maker.empty()) {
          cam.maker = text;
        }
      } else if (t.name == "model") {
        auto text = GetTextContent(parser);
        if (!text.empty() && cam.model.empty()) {
          cam.model = text;
        }
      } else if (t.name == "mount") {
        cam.mount = GetTextContent(parser);
      } else if (t.name == "cropfactor") {
        auto text = GetTextContent(parser);
        if (auto val = ParseFloat(text)) {
          cam.crop_factor = *val;
        }
      }
    } else if (t.type == XmlParser::TokenType::SelfClosingElement) {
      // No self-closing elements expected in camera
    } else if (t.type == XmlParser::TokenType::EndOfFile ||
               t.type == XmlParser::TokenType::Error) {
      break;
    }
  }

  if (!cam.maker.empty() && !cam.model.empty()) {
    cameras.push_back(std::move(cam));
  }
}

void ParseLens(XmlParser& parser, std::vector<LensEntry>& lenses) {
  LensEntry   lens;
  std::string current_element;

  for (;;) {
    auto t = parser.Next();
    if (t.type == XmlParser::TokenType::EndElement) {
      if (t.name == "lens") {
        break;
      }
      current_element.clear();
      continue;
    }
    if (t.type == XmlParser::TokenType::BeginElement) {
      current_element = t.name;
      if (t.name == "maker") {
        auto text = GetTextContent(parser);
        if (!text.empty() && lens.maker.empty()) {
          lens.maker = text;
        }
      } else if (t.name == "model") {
        auto text = GetTextContent(parser);
        if (!text.empty() && lens.model.empty()) {
          lens.model = text;
        }
      } else if (t.name == "mount") {
        lens.mount = GetTextContent(parser);
      } else if (t.name == "cropfactor") {
        auto text = GetTextContent(parser);
        if (auto val = ParseFloat(text)) {
          lens.crop_factor = *val;
        }
      } else if (t.name == "type") {
        auto text = GetTextContent(parser);
        lens.projection = ParseProjectionType(text);
      } else if (t.name == "calibration") {
        // Parse calibration content inline
        for (;;) {
          auto tc = parser.Next();
          if (tc.type == XmlParser::TokenType::EndElement) {
            if (tc.name == "calibration") break;
            continue;
          }
          if (tc.type == XmlParser::TokenType::SelfClosingElement) {
            if (tc.name == "distortion") {
              DistortionCalib d;
              auto it_model = tc.attributes.find("model");
              if (it_model != tc.attributes.end()) d.model = ParseDistortionModel(it_model->second);
              auto it_focal = tc.attributes.find("focal");
              if (it_focal != tc.attributes.end()) { if (auto v = ParseFloat(it_focal->second)) d.focal = *v; }
              auto it_rf = tc.attributes.find("real-focal");
              if (it_rf != tc.attributes.end()) { if (auto v = ParseFloat(it_rf->second)) d.real_focal = *v; }
              auto it_k1 = tc.attributes.find("k1");
              if (it_k1 != tc.attributes.end()) { if (auto v = ParseFloat(it_k1->second)) d.k1 = *v; }
              auto it_k2 = tc.attributes.find("k2");
              if (it_k2 != tc.attributes.end()) { if (auto v = ParseFloat(it_k2->second)) d.k2 = *v; }
              auto it_a = tc.attributes.find("a");
              if (it_a != tc.attributes.end()) { if (auto v = ParseFloat(it_a->second)) d.a = *v; }
              auto it_b = tc.attributes.find("b");
              if (it_b != tc.attributes.end()) { if (auto v = ParseFloat(it_b->second)) d.b = *v; }
              auto it_c = tc.attributes.find("c");
              if (it_c != tc.attributes.end()) { if (auto v = ParseFloat(it_c->second)) d.c = *v; }
              if (d.model != DistortionModel::None) lens.distortions.push_back(d);
            } else if (tc.name == "tca") {
              TcaCalib tca;
              auto it_model = tc.attributes.find("model");
              if (it_model != tc.attributes.end()) tca.model = ParseTcaModel(it_model->second);
              auto it_focal = tc.attributes.find("focal");
              if (it_focal != tc.attributes.end()) { if (auto v = ParseFloat(it_focal->second)) tca.focal = *v; }
              auto it_vr = tc.attributes.find("vr");
              if (it_vr != tc.attributes.end()) { if (auto v = ParseFloat(it_vr->second)) tca.vr = *v; }
              auto it_vb = tc.attributes.find("vb");
              if (it_vb != tc.attributes.end()) { if (auto v = ParseFloat(it_vb->second)) tca.vb = *v; }
              auto it_kr = tc.attributes.find("kr");
              if (it_kr != tc.attributes.end()) { if (auto v = ParseFloat(it_kr->second)) tca.vr = *v; }
              auto it_kb = tc.attributes.find("kb");
              if (it_kb != tc.attributes.end()) { if (auto v = ParseFloat(it_kb->second)) tca.vb = *v; }
              auto it_cr = tc.attributes.find("cr");
              if (it_cr != tc.attributes.end()) { if (auto v = ParseFloat(it_cr->second)) tca.cr = *v; }
              auto it_cb = tc.attributes.find("cb");
              if (it_cb != tc.attributes.end()) { if (auto v = ParseFloat(it_cb->second)) tca.cb = *v; }
              auto it_br = tc.attributes.find("br");
              if (it_br != tc.attributes.end()) { if (auto v = ParseFloat(it_br->second)) tca.br = *v; }
              auto it_bb = tc.attributes.find("bb");
              if (it_bb != tc.attributes.end()) { if (auto v = ParseFloat(it_bb->second)) tca.bb = *v; }
              if (tca.model != TcaModel::None) lens.tca_entries.push_back(tca);
            } else if (tc.name == "vignetting") {
              VignettingCalib v;
              auto it_model = tc.attributes.find("model");
              if (it_model != tc.attributes.end() && ToLower(it_model->second) == "pa") v.model = VignettingModel::PA;
              auto it_focal = tc.attributes.find("focal");
              if (it_focal != tc.attributes.end()) { if (auto val = ParseFloat(it_focal->second)) v.focal = *val; }
              auto it_aperture = tc.attributes.find("aperture");
              if (it_aperture != tc.attributes.end()) { if (auto val = ParseFloat(it_aperture->second)) v.aperture = *val; }
              auto it_dist = tc.attributes.find("distance");
              if (it_dist != tc.attributes.end()) { if (auto val = ParseFloat(it_dist->second)) v.distance = *val; }
              auto it_k1 = tc.attributes.find("k1");
              if (it_k1 != tc.attributes.end()) { if (auto val = ParseFloat(it_k1->second)) v.k1 = *val; }
              auto it_k2 = tc.attributes.find("k2");
              if (it_k2 != tc.attributes.end()) { if (auto val = ParseFloat(it_k2->second)) v.k2 = *val; }
              auto it_k3 = tc.attributes.find("k3");
              if (it_k3 != tc.attributes.end()) { if (auto val = ParseFloat(it_k3->second)) v.k3 = *val; }
              if (v.model != VignettingModel::None) lens.vignetting_entries.push_back(v);
            }
          } else if (tc.type == XmlParser::TokenType::BeginElement) {
            parser.SkipToEndOf(tc.name);
          } else if (tc.type == XmlParser::TokenType::EndOfFile || tc.type == XmlParser::TokenType::Error) {
            break;
          }
        }
      }
    } else if (t.type == XmlParser::TokenType::SelfClosingElement) {
      if (t.name == "focal") {
        auto it_min = t.attributes.find("min");
        auto it_max = t.attributes.find("max");
        auto it_val = t.attributes.find("value");
        if (it_min != t.attributes.end()) {
          if (auto v = ParseFloat(it_min->second)) lens.min_focal = *v;
        }
        if (it_max != t.attributes.end()) {
          if (auto v = ParseFloat(it_max->second)) lens.max_focal = *v;
        }
        if (it_val != t.attributes.end()) {
          if (auto v = ParseFloat(it_val->second)) {
            lens.min_focal = *v;
            lens.max_focal = *v;
          }
        }
      } else if (t.name == "aperture") {
        auto it_min = t.attributes.find("min");
        auto it_max = t.attributes.find("max");
        auto it_val = t.attributes.find("value");
        if (it_min != t.attributes.end()) {
          if (auto v = ParseFloat(it_min->second)) lens.min_aperture = *v;
        }
        if (it_max != t.attributes.end()) {
          if (auto v = ParseFloat(it_max->second)) lens.max_aperture = *v;
        }
        if (it_val != t.attributes.end()) {
          if (auto v = ParseFloat(it_val->second)) {
            lens.min_aperture = *v;
            lens.max_aperture = *v;
          }
        }
      }
    } else if (t.type == XmlParser::TokenType::EndOfFile ||
               t.type == XmlParser::TokenType::Error) {
      break;
    }
  }

  if (!lens.maker.empty() && !lens.model.empty()) {
    lenses.push_back(std::move(lens));
  }
}

void ParseXmlContent(const std::string& xml,
                     std::vector<CameraEntry>& cameras,
                     std::vector<LensEntry>& lenses) {
  XmlParser parser(xml);

  for (;;) {
    auto t = parser.Next();
    if (t.type == XmlParser::TokenType::EndOfFile) {
      break;
    }
    if (t.type == XmlParser::TokenType::Error) {
      break;
    }
    if (t.type == XmlParser::TokenType::BeginElement) {
      if (t.name == "camera") {
        ParseCamera(parser, cameras);
      } else if (t.name == "lens") {
        ParseLens(parser, lenses);
      }
      // Skip <mount>, <lensdatabase>, etc.
    }
  }
}

// ═════════════════════════════════════════════════════════════════════════════
//  Interpolation helpers
// ═════════════════════════════════════════════════════════════════════════════

template <typename T, typename GetKey>
void SortByKey(std::vector<T>& vec, GetKey getter) {
  std::sort(vec.begin(), vec.end(),
            [&](const T& a, const T& b) { return getter(a) < getter(b); });
}

auto InterpolateDistortion(const std::vector<DistortionCalib>& calibs,
                           float focal_mm) -> DistortionCalib {
  if (calibs.empty()) {
    return {};
  }
  if (calibs.size() == 1) {
    return calibs[0];
  }

  // Already sorted by focal during loading
  // Find bracketing interval
  std::size_t lo = 0;
  std::size_t hi = calibs.size() - 1;
  for (std::size_t i = 0; i < calibs.size(); ++i) {
    if (calibs[i].focal <= focal_mm) {
      lo = i;
    }
    if (calibs[i].focal >= focal_mm) {
      hi = i;
      break;
    }
  }

  if (lo == hi || std::fabs(calibs[hi].focal - calibs[lo].focal) < kEpsilon) {
    return calibs[lo];
  }

  const float t = (focal_mm - calibs[lo].focal) /
                  (calibs[hi].focal - calibs[lo].focal);

  DistortionCalib result;
  result.model      = calibs[lo].model;
  result.focal      = focal_mm;
  result.real_focal = calibs[lo].real_focal + t * (calibs[hi].real_focal - calibs[lo].real_focal);
  result.k1         = calibs[lo].k1 + t * (calibs[hi].k1 - calibs[lo].k1);
  result.k2         = calibs[lo].k2 + t * (calibs[hi].k2 - calibs[lo].k2);
  result.a          = calibs[lo].a + t * (calibs[hi].a - calibs[lo].a);
  result.b          = calibs[lo].b + t * (calibs[hi].b - calibs[lo].b);
  result.c          = calibs[lo].c + t * (calibs[hi].c - calibs[lo].c);
  return result;
}

auto InterpolateTca(const std::vector<TcaCalib>& calibs,
                    float focal_mm) -> TcaCalib {
  if (calibs.empty()) {
    return {};
  }
  if (calibs.size() == 1) {
    return calibs[0];
  }

  std::size_t lo = 0;
  std::size_t hi = calibs.size() - 1;
  for (std::size_t i = 0; i < calibs.size(); ++i) {
    if (calibs[i].focal <= focal_mm) {
      lo = i;
    }
    if (calibs[i].focal >= focal_mm) {
      hi = i;
      break;
    }
  }

  if (lo == hi || std::fabs(calibs[hi].focal - calibs[lo].focal) < kEpsilon) {
    return calibs[lo];
  }

  const float t = (focal_mm - calibs[lo].focal) /
                  (calibs[hi].focal - calibs[lo].focal);

  TcaCalib result;
  result.model = calibs[lo].model;
  result.focal = focal_mm;
  result.vr    = calibs[lo].vr + t * (calibs[hi].vr - calibs[lo].vr);
  result.vb    = calibs[lo].vb + t * (calibs[hi].vb - calibs[lo].vb);
  result.cr    = calibs[lo].cr + t * (calibs[hi].cr - calibs[lo].cr);
  result.cb    = calibs[lo].cb + t * (calibs[hi].cb - calibs[lo].cb);
  result.br    = calibs[lo].br + t * (calibs[hi].br - calibs[lo].br);
  result.bb    = calibs[lo].bb + t * (calibs[hi].bb - calibs[lo].bb);
  return result;
}

auto InterpolateVignetting(const std::vector<VignettingCalib>& calibs,
                           float focal_mm, float aperture, float distance) -> VignettingCalib {
  if (calibs.empty()) {
    return {};
  }

  // Find the two closest entries in (focal, aperture, distance) space
  // First filter by aperture and distance, then interpolate by focal
  auto best_aperture = std::numeric_limits<float>::max();
  for (const auto& v : calibs) {
    const float diff = std::fabs(v.aperture - aperture);
    if (diff < best_aperture) {
      best_aperture = diff;
    }
  }

  std::vector<VignettingCalib> filtered;
  for (const auto& v : calibs) {
    if (std::fabs(v.aperture - aperture) <= best_aperture + kEpsilon) {
      filtered.push_back(v);
    }
  }

  if (filtered.empty()) {
    // Fallback: just use the closest by focal
    const VignettingCalib* best = &calibs[0];
    float                  best_diff = std::fabs(calibs[0].focal - focal_mm);
    for (const auto& v : calibs) {
      const float diff = std::fabs(v.focal - focal_mm);
      if (diff < best_diff) {
        best_diff = diff;
        best      = &v;
      }
    }
    return *best;
  }

  // Now find closest by distance among filtered
  const VignettingCalib* best = &filtered[0];
  float                  best_dist_diff = std::fabs(filtered[0].distance - distance);
  for (const auto& v : filtered) {
    const float diff = std::fabs(v.distance - distance);
    if (diff < best_dist_diff) {
      best_dist_diff = diff;
      best           = &v;
    }
  }

  // Interpolate by focal if we have bracket
  SortByKey(filtered, [](const VignettingCalib& v) { return v.focal; });
  std::size_t lo = 0;
  std::size_t hi = filtered.size() - 1;
  for (std::size_t i = 0; i < filtered.size(); ++i) {
    if (filtered[i].focal <= focal_mm) {
      lo = i;
    }
    if (filtered[i].focal >= focal_mm) {
      hi = i;
      break;
    }
  }

  if (lo == hi || std::fabs(filtered[hi].focal - filtered[lo].focal) < kEpsilon) {
    return filtered[lo];
  }

  const float t = (focal_mm - filtered[lo].focal) /
                  (filtered[hi].focal - filtered[lo].focal);

  VignettingCalib result;
  result.model   = VignettingModel::PA;
  result.focal   = focal_mm;
  result.aperture = aperture;
  result.distance = distance;
  result.k1      = filtered[lo].k1 + t * (filtered[hi].k1 - filtered[lo].k1);
  result.k2      = filtered[lo].k2 + t * (filtered[hi].k2 - filtered[lo].k2);
  result.k3      = filtered[lo].k3 + t * (filtered[hi].k3 - filtered[lo].k3);
  return result;
}

// ═════════════════════════════════════════════════════════════════════════════
//  Distortion correction math
// ═════════════════════════════════════════════════════════════════════════════

auto HuginScaleInMm(float crop_factor, float aspect_ratio) -> float {
  if (!IsFinitePositive(crop_factor) || !IsFinitePositive(aspect_ratio)) {
    return 0.0f;
  }
  return kFullFrameDiagonalMm / crop_factor /
         std::hypot(aspect_ratio, 1.0f) * 0.5f;
}

void RescaleDistortion(CorrectionParams& params, float real_focal_mm,
                       float crop_factor, float aspect_ratio) {
  if (params.distortion_model == DistortionModel::None) {
    return;
  }
  if (!IsFinitePositive(real_focal_mm) || !IsFinitePositive(crop_factor) ||
      !IsFinitePositive(aspect_ratio)) {
    return;
  }

  const float hugin_scale = HuginScaleInMm(crop_factor, aspect_ratio);
  if (!IsFinitePositive(hugin_scale)) {
    return;
  }

  const float hugin_scaling = real_focal_mm / hugin_scale;

  switch (params.distortion_model) {
    case DistortionModel::Poly3: {
      const float d = 1.0f - params.distortion_terms[0];
      if (std::fabs(d) <= kEpsilon) {
        return;
      }
      params.distortion_terms[0] *= std::pow(hugin_scaling, 2.0f) / std::pow(d, 3.0f);
      break;
    }
    case DistortionModel::Poly5:
      params.distortion_terms[0] *= std::pow(hugin_scaling, 2.0f);
      params.distortion_terms[1] *= std::pow(hugin_scaling, 4.0f);
      break;
    case DistortionModel::PtLens: {
      float a = params.distortion_terms[0];
      float b = params.distortion_terms[1];
      float c = params.distortion_terms[2];
      const float d = 1.0f - a - b - c;
      if (std::fabs(d) <= kEpsilon) {
        return;
      }
      params.distortion_terms[0] = a * std::pow(hugin_scaling, 3.0f) / std::pow(d, 4.0f);
      params.distortion_terms[1] = b * std::pow(hugin_scaling, 2.0f) / std::pow(d, 3.0f);
      params.distortion_terms[2] = c * hugin_scaling / std::pow(d, 2.0f);
      break;
    }
    default:
      break;
  }
}

void RescaleTca(CorrectionParams& params, float real_focal_mm,
                float crop_factor, float aspect_ratio) {
  if (params.tca_model == TcaModel::None) {
    return;
  }
  if (!IsFinitePositive(real_focal_mm) || !IsFinitePositive(crop_factor) ||
      !IsFinitePositive(aspect_ratio)) {
    return;
  }

  const float hugin_scale = HuginScaleInMm(crop_factor, aspect_ratio);
  if (!IsFinitePositive(hugin_scale)) {
    return;
  }

  const float hugin_scaling = real_focal_mm / hugin_scale;

  if (params.tca_model == TcaModel::Poly3) {
    // Terms layout: [cr, cb, br, bb, vr, vb]
    params.tca_terms[2] *= hugin_scaling;
    params.tca_terms[3] *= hugin_scaling;
    params.tca_terms[4] *= hugin_scaling * hugin_scaling;
    params.tca_terms[5] *= hugin_scaling * hugin_scaling;
  }
}

void RescaleVignetting(CorrectionParams& params, float real_focal_mm,
                       float crop_factor) {
  if (params.vignetting_model == VignettingModel::None) {
    return;
  }
  if (!IsFinitePositive(real_focal_mm) || !IsFinitePositive(crop_factor)) {
    return;
  }

  const float hugin_scale_in_mm = (kFullFrameDiagonalMm / crop_factor) * 0.5f;
  if (!IsFinitePositive(hugin_scale_in_mm)) {
    return;
  }

  const float hugin_scaling = real_focal_mm / hugin_scale_in_mm;
  const float hs2           = hugin_scaling * hugin_scaling;

  params.vignetting_terms[0] *= hs2;
  params.vignetting_terms[1] *= hs2 * hs2;
  params.vignetting_terms[2] *= hs2 * hs2 * hs2;
}

// ═════════════════════════════════════════════════════════════════════════════
//  Undistort coordinate (forward: distorted→ideal, backward: ideal→distorted)
// ═════════════════════════════════════════════════════════════════════════════

void UndistortCoord(float& x, float& y, const CorrectionParams& params, bool backward) {
  if (params.distortion_model == DistortionModel::None) {
    return;
  }

  const float r2 = x * x + y * y;

  switch (params.distortion_model) {
    case DistortionModel::Poly3: {
      // Ru = Rd * (1 - k1 * Rd^2)   for forward (distorted → ideal)
      // Rd = Ru * (1 + k1 * Ru^2)   approximately for backward
      const float k1 = params.distortion_terms[0];
      if (backward) {
        const float scale = 1.0f + k1 * r2;
        x *= scale;
        y *= scale;
      } else {
        const float scale = 1.0f - k1 * r2;
        x *= scale;
        y *= scale;
      }
      break;
    }
    case DistortionModel::Poly5: {
      const float k1 = params.distortion_terms[0];
      const float k2 = params.distortion_terms[1];
      if (backward) {
        const float scale = 1.0f + k1 * r2 + k2 * r2 * r2;
        x *= scale;
        y *= scale;
      } else {
        const float scale = 1.0f - k1 * r2 - k2 * r2 * r2;
        x *= scale;
        y *= scale;
      }
      break;
    }
    case DistortionModel::PtLens: {
      const float a = params.distortion_terms[0];
      const float b = params.distortion_terms[1];
      const float c = params.distortion_terms[2];
      if (backward) {
        // Rd = Ru * (a * Ru^3 + b * Ru^2 + c * Ru + 1 - a - b - c)
        const float r  = std::sqrt(r2);
        const float scale = a * r * r * r + b * r2 + c * r + 1.0f - a - b - c;
        x *= scale;
        y *= scale;
      } else {
        // Ru = Rd * (a * Rd^3 + b * Rd^2 + c * Rd + 1 - a - b - c)
        const float r  = std::sqrt(r2);
        const float scale = a * r * r * r + b * r2 + c * r + 1.0f - a - b - c;
        x *= scale;
        y *= scale;
      }
      break;
    }
    default:
      break;
  }
}

// ═════════════════════════════════════════════════════════════════════════════
//  Bilinear interpolation
// ═════════════════════════════════════════════════════════════════════════════

auto BilinearSample(const float* data, int width, int height,
                    float x, float y, int channel) -> float {
  const int x0 = static_cast<int>(std::floor(x));
  const int y0 = static_cast<int>(std::floor(y));
  const int x1 = x0 + 1;
  const int y1 = y0 + 1;

  const float fx = x - static_cast<float>(x0);
  const float fy = y - static_cast<float>(y0);

  const int cx0 = std::clamp(x0, 0, width - 1);
  const int cx1 = std::clamp(x1, 0, width - 1);
  const int cy0 = std::clamp(y0, 0, height - 1);
  const int cy1 = std::clamp(y1, 0, height - 1);

  const std::size_t stride = static_cast<std::size_t>(width) * static_cast<std::size_t>(height);
  const std::size_t offset = static_cast<std::size_t>(channel) * stride;

  const float v00 = data[offset + static_cast<std::size_t>(cy0) * static_cast<std::size_t>(width) +
                         static_cast<std::size_t>(cx0)];
  const float v10 = data[offset + static_cast<std::size_t>(cy0) * static_cast<std::size_t>(width) +
                         static_cast<std::size_t>(cx1)];
  const float v01 = data[offset + static_cast<std::size_t>(cy1) * static_cast<std::size_t>(width) +
                         static_cast<std::size_t>(cx0)];
  const float v11 = data[offset + static_cast<std::size_t>(cy1) * static_cast<std::size_t>(width) +
                         static_cast<std::size_t>(cx1)];

  return (1.0f - fx) * (1.0f - fy) * v00 + fx * (1.0f - fy) * v10 +
         (1.0f - fx) * fy * v01 + fx * fy * v11;
}

}  // namespace

// ═════════════════════════════════════════════════════════════════════════════
//  LensDatabase::Impl
// ═════════════════════════════════════════════════════════════════════════════

struct LensDatabase::Impl {
  std::vector<CameraEntry> cameras;
  std::vector<LensEntry>   lenses;
  bool                      loaded = false;
};

LensDatabase::LensDatabase() : impl_(std::make_unique<Impl>()) {}

LensDatabase::~LensDatabase() = default;

LensDatabase::LensDatabase(LensDatabase&&) noexcept = default;

LensDatabase& LensDatabase::operator=(LensDatabase&&) noexcept = default;

void LensDatabase::LoadDirectory(const std::filesystem::path& db_path) {
  if (!std::filesystem::exists(db_path) || !std::filesystem::is_directory(db_path)) {
    throw DatabaseError("Database path does not exist or is not a directory: " +
                        db_path.string());
  }

  std::vector<std::filesystem::path> xml_files;
  for (const auto& entry : std::filesystem::directory_iterator(db_path)) {
    if (entry.is_regular_file() &&
        entry.path().extension() == ".xml") {
      xml_files.push_back(entry.path());
    }
  }

  if (xml_files.empty()) {
    throw DatabaseError("No XML files found in database directory: " +
                        db_path.string());
  }

  for (const auto& file : xml_files) {
    try {
      LoadFile(file);
    } catch (const DatabaseError& e) {
      std::cerr << "Warning: Skipping " << file.string() << ": " << e.what() << std::endl;
    }
  }

  if (impl_->cameras.empty() && impl_->lenses.empty()) {
    throw DatabaseError("No camera or lens entries loaded from: " +
                        db_path.string());
  }

  impl_->loaded = true;
}

void LensDatabase::LoadFile(const std::filesystem::path& file_path) {
  std::ifstream file(file_path, std::ios::in | std::ios::binary);
  if (!file.is_open()) {
    throw DatabaseError("Cannot open file: " + file_path.string());
  }

  std::string content((std::istreambuf_iterator<char>(file)),
                       std::istreambuf_iterator<char>());
  file.close();

  if (content.empty()) {
    throw DatabaseError("File is empty: " + file_path.string());
  }

  ParseXmlContent(content, impl_->cameras, impl_->lenses);
  impl_->loaded = true;
}

auto LensDatabase::FindCamera(const std::string& maker,
                               const std::string& model) const -> const CameraEntry* {
  for (const auto& cam : impl_->cameras) {
    if (LooseMatch(cam.maker, maker) && LooseMatch(cam.model, model)) {
      return &cam;
    }
  }
  return nullptr;
}

auto LensDatabase::FindLenses(const std::string& maker,
                               const std::string& model) const
    -> std::vector<const LensEntry*> {
  std::vector<const LensEntry*> results;
  for (const auto& lens : impl_->lenses) {
    if (LooseMatch(lens.maker, maker) && LooseMatch(lens.model, model)) {
      results.push_back(&lens);
    }
  }
  return results;
}

auto LensDatabase::MatchLens(const std::string& cam_maker,
                              const std::string& cam_model,
                              const std::string& lens_maker,
                              const std::string& lens_model,
                              float              focal_length_mm,
                              float              aperture_f_number) const -> LensMatchResult {
  LensMatchResult result;

  if (lens_model.empty()) {
    return result;
  }

  // Find camera
  const CameraEntry* camera = nullptr;
  if (!cam_maker.empty() && !cam_model.empty()) {
    camera = FindCamera(cam_maker, cam_model);
  }

  // Find lenses
  const auto lenses = FindLenses(lens_maker, lens_model);
  if (lenses.empty()) {
    return result;
  }

  // Score candidates
  const LensEntry* best_lens  = nullptr;
  int              best_score = std::numeric_limits<int>::min();

  for (const auto* lens : lenses) {
    int score = 0;

    // Focal range match
    if (IsFinitePositive(focal_length_mm)) {
      if (IsFinitePositive(lens->min_focal) && IsFinitePositive(lens->max_focal)) {
        if (focal_length_mm >= lens->min_focal - 0.2f &&
            focal_length_mm <= lens->max_focal + 0.2f) {
          score += 2000;
        } else {
          score -= 2000;
        }
      }
    }

    // Aperture match
    if (IsFinitePositive(aperture_f_number)) {
      if (IsFinitePositive(lens->min_aperture) && IsFinitePositive(lens->max_aperture)) {
        if (aperture_f_number >= lens->min_aperture - 0.1f &&
            aperture_f_number <= lens->max_aperture + 0.1f) {
          score += 200;
        } else {
          score -= 200;
        }
      }
    }

    // Prefer lenses with calibration data
    if (!lens->distortions.empty()) {
      score += 50;
    }
    if (!lens->tca_entries.empty()) {
      score += 30;
    }
    if (!lens->vignetting_entries.empty()) {
      score += 20;
    }

    if (score > best_score) {
      best_score = score;
      best_lens  = lens;
    }
  }

  if (!best_lens) {
    return result;
  }

  result.lens   = best_lens;
  result.camera = camera;
  result.valid  = true;

  // Determine crop factor
  if (camera && IsFinitePositive(camera->crop_factor)) {
    result.crop_factor = camera->crop_factor;
  } else if (IsFinitePositive(best_lens->crop_factor)) {
    result.crop_factor = best_lens->crop_factor;
  }

  return result;
}

auto LensDatabase::CameraCount() const -> std::size_t {
  return impl_->cameras.size();
}

auto LensDatabase::LensCount() const -> std::size_t {
  return impl_->lenses.size();
}

auto LensDatabase::IsValid() const -> bool {
  return impl_->loaded;
}

// ═════════════════════════════════════════════════════════════════════════════
//  LensCorrector::Impl
// ═════════════════════════════════════════════════════════════════════════════

struct LensCorrector::Impl {
  std::shared_ptr<const LensDatabase> db;
  Config                              config;
};

LensCorrector::LensCorrector() : impl_(std::make_unique<Impl>()) {}

LensCorrector::~LensCorrector() = default;

LensCorrector::LensCorrector(LensCorrector&&) noexcept = default;

LensCorrector& LensCorrector::operator=(LensCorrector&&) noexcept = default;

void LensCorrector::SetDatabase(std::shared_ptr<const LensDatabase> db) {
  impl_->db = std::move(db);
}

void LensCorrector::SetConfig(const Config& config) {
  impl_->config = config;
}

auto LensCorrector::InterpolateParams(const LensEntry& lens,
                                       float            focal_mm,
                                       float            aperture,
                                       float            distance_m) const -> CorrectionParams {
  CorrectionParams params;

  params.crop_factor   = lens.crop_factor;
  params.lens_center_x = lens.center_x;
  params.lens_center_y = lens.center_y;

  // Sort and interpolate distortion
  if (!lens.distortions.empty()) {
    auto sorted = lens.distortions;
    SortByKey(sorted, [](const DistortionCalib& d) { return d.focal; });
    auto interp = InterpolateDistortion(sorted, focal_mm);
    params.distortion_model = interp.model;
    params.real_focal_mm    = interp.real_focal;
    if (interp.model == DistortionModel::Poly3 || interp.model == DistortionModel::Poly5) {
      params.distortion_terms[0] = interp.k1;
      params.distortion_terms[1] = interp.k2;
    } else if (interp.model == DistortionModel::PtLens) {
      params.distortion_terms[0] = interp.a;
      params.distortion_terms[1] = interp.b;
      params.distortion_terms[2] = interp.c;
    }
  }

  // Sort and interpolate TCA
  if (!lens.tca_entries.empty()) {
    auto sorted = lens.tca_entries;
    SortByKey(sorted, [](const TcaCalib& t) { return t.focal; });
    auto interp = InterpolateTca(sorted, focal_mm);
    params.tca_model = interp.model;
    if (interp.model == TcaModel::Linear) {
      params.tca_terms[0] = interp.vr;
      params.tca_terms[1] = interp.vb;
    } else if (interp.model == TcaModel::Poly3) {
      params.tca_terms[0] = interp.cr;
      params.tca_terms[1] = interp.cb;
      params.tca_terms[2] = interp.br;
      params.tca_terms[3] = interp.bb;
      params.tca_terms[4] = interp.vr;
      params.tca_terms[5] = interp.vb;
    }
  }

  // Sort and interpolate vignetting
  if (!lens.vignetting_entries.empty()) {
    auto sorted = lens.vignetting_entries;
    SortByKey(sorted, [](const VignettingCalib& v) { return v.focal; });
    auto interp = InterpolateVignetting(sorted, focal_mm, aperture, distance_m);
    params.vignetting_model = interp.model;
    params.vignetting_terms[0] = interp.k1;
    params.vignetting_terms[1] = interp.k2;
    params.vignetting_terms[2] = interp.k3;
  }

  return params;
}

void LensCorrector::ApplyCorrection(float*                 image,
                                     int                   width,
                                     int                   height,
                                     const CorrectionParams& params,
                                     const Config&           config) const {
  if (!image || width <= 0 || height <= 0) {
    throw DimensionMismatchError("Invalid image dimensions: " +
                                 std::to_string(width) + "x" + std::to_string(height));
  }

  const bool do_distortion = config.apply_distortion &&
                             params.distortion_model != DistortionModel::None;
  const bool do_tca        = config.apply_tca &&
                             params.tca_model != TcaModel::None;
  const bool do_vignetting = config.apply_vignetting &&
                             params.vignetting_model != VignettingModel::None;

  if (!do_distortion && !do_tca && !do_vignetting) {
    return;  // Nothing to do
  }

  const std::size_t total_pixels = static_cast<std::size_t>(width) * static_cast<std::size_t>(height);

  // Compute scale
  float scale = 1.0f;
  if (config.auto_scale && do_distortion) {
    scale = ComputeAutoScale(params, width, height);
  }
  if (config.user_scale > 0.0f && std::isfinite(config.user_scale)) {
    scale = config.user_scale;
  }

  // Normalization: convert pixel coordinates to normalized coordinates
  // normalized = (pixel - center) / norm_scale
  const float cx = static_cast<float>(width) * 0.5f;
  const float cy = static_cast<float>(height) * 0.5f;
  const float min_dim = static_cast<float>(std::min(width, height));
  const float norm_scale = min_dim * 0.5f;

  // Determine thread count
  int num_threads = config.num_threads;
  if (num_threads <= 0) {
    num_threads = static_cast<int>(std::thread::hardware_concurrency());
    if (num_threads <= 0) {
      num_threads = 1;
    }
  }

  // Allocate output buffer
  std::vector<float> output(total_pixels * 3);

  // Process rows in parallel
  const auto process_row_range = [&](int y_start, int y_end) {
    for (int y = y_start; y < y_end; ++y) {
      for (int x = 0; x < width; ++x) {
        const std::size_t idx = static_cast<std::size_t>(y) * static_cast<std::size_t>(width) +
                                static_cast<std::size_t>(x);

        // Normalize coordinates
        float nx = (static_cast<float>(x) - cx) / norm_scale;
        float ny = (static_cast<float>(y) - cy) / norm_scale;

        // Apply TCA (transverse chromatic aberration) - shift red/blue relative to green
        // This is done BEFORE distortion correction
        float nx_r = nx, ny_r = ny;
        float nx_b = nx, ny_b = ny;

        if (do_tca) {
          const float r2 = nx * nx + ny * ny;
          if (params.tca_model == TcaModel::Linear) {
            // vr, vb at [0],[1]
            nx_r = nx * params.tca_terms[0];
            ny_r = ny * params.tca_terms[0];
            nx_b = nx * params.tca_terms[1];
            ny_b = ny * params.tca_terms[1];
          } else if (params.tca_model == TcaModel::Poly3) {
            // cr, cb, br, bb, vr, vb at [0]-[5]
            const float cr = params.tca_terms[0];
            const float cb = params.tca_terms[1];
            const float br = params.tca_terms[2];
            const float bb = params.tca_terms[3];
            const float vr = params.tca_terms[4];
            const float vb = params.tca_terms[5];
            const float scale_r = vr + cr * r2 + br * r2 * r2;
            const float scale_b = vb + cb * r2 + bb * r2 * r2;
            nx_r = nx * scale_r;
            ny_r = ny * scale_r;
            nx_b = nx * scale_b;
            ny_b = ny * scale_b;
          }
        }

        // Apply distortion correction (backward mapping: ideal → distorted)
        if (do_distortion) {
          UndistortCoord(nx, ny, params, true);
          if (do_tca) {
            UndistortCoord(nx_r, ny_r, params, true);
            UndistortCoord(nx_b, ny_b, params, true);
          }
        }

        // Convert back to pixel coordinates
        const float px_g = nx * norm_scale * scale + cx;
        const float py_g = ny * norm_scale * scale + cy;

        // Sample green channel (distortion reference)
        float r = BilinearSample(image, width, height, px_g, py_g, 0);
        float g = BilinearSample(image, width, height, px_g, py_g, 1);
        float b = BilinearSample(image, width, height, px_g, py_g, 2);

        if (do_tca) {
          const float px_r = nx_r * norm_scale * scale + cx;
          const float py_r = ny_r * norm_scale * scale + cy;
          const float px_b = nx_b * norm_scale * scale + cx;
          const float py_b = ny_b * norm_scale * scale + cy;

          r = BilinearSample(image, width, height, px_r, py_r, 0);
          b = BilinearSample(image, width, height, px_b, py_b, 2);
        }

        // Apply vignetting correction
        if (do_vignetting) {
          // PA model: factor = 1 + k1*r^2 + k2*r^4 + k3*r^6
          const float r2 = nx * nx + ny * ny;
          const float r4 = r2 * r2;
          const float r6 = r4 * r2;
          const float vfactor = 1.0f + params.vignetting_terms[0] * r2 +
                                params.vignetting_terms[1] * r4 +
                                params.vignetting_terms[2] * r6;
          if (vfactor > kEpsilon) {
            r /= vfactor;
            g /= vfactor;
            b /= vfactor;
          }
        }

        // Write output (planar layout)
        output[idx]                              = r;
        output[idx + total_pixels]               = g;
        output[idx + total_pixels * 2]           = b;
      }
    }
  };

  // Distribute work across threads
  std::vector<std::thread> threads;
  const int rows_per_thread = (height + num_threads - 1) / num_threads;

  for (int t = 0; t < num_threads; ++t) {
    const int y_start = t * rows_per_thread;
    const int y_end   = std::min(y_start + rows_per_thread, height);
    if (y_start >= y_end) {
      break;
    }
    threads.emplace_back(process_row_range, y_start, y_end);
  }

  for (auto& thread : threads) {
    thread.join();
  }

  // Copy output back to input
  std::memcpy(image, output.data(), total_pixels * 3 * sizeof(float));
}

void LensCorrector::UndistortNormalized(float& xu, float& yu,
                                         const CorrectionParams& params,
                                         bool backward) {
  UndistortCoord(xu, yu, params, backward);
}

auto LensCorrector::ComputeAutoScale(const CorrectionParams& params,
                                      int width, int height) -> float {
  if (params.distortion_model == DistortionModel::None) {
    return 1.0f;
  }

  const float cx = static_cast<float>(width) * 0.5f;
  const float cy = static_cast<float>(height) * 0.5f;
  const float min_dim = static_cast<float>(std::min(width, height));
  const float norm_scale = min_dim * 0.5f;

  // Sample the four corners and find the maximum displacement
  float max_scale = 1.0f;
  const std::pair<float, float> corners[] = {
      {0.0f, 0.0f},
      {static_cast<float>(width - 1), 0.0f},
      {static_cast<float>(width - 1), static_cast<float>(height - 1)},
      {0.0f, static_cast<float>(height - 1)},
  };

  for (const auto& [px, py] : corners) {
    float nx = (px - cx) / norm_scale;
    float ny = (py - cy) / norm_scale;
    UndistortCoord(nx, ny, params, true);  // backward: ideal→distorted

    // Convert back to pixel
    const float mapped_x = nx * norm_scale + cx;
    const float mapped_y = ny * norm_scale + cy;

    // Check if this corner is outside the image bounds
    if (mapped_x < 0.0f || mapped_x >= static_cast<float>(width) ||
        mapped_y < 0.0f || mapped_y >= static_cast<float>(height)) {
      // Compute how much we need to scale to fit
      const float sx = mapped_x < 0.0f ? cx / (cx - mapped_x) :
                       mapped_x >= static_cast<float>(width) ?
                       (static_cast<float>(width - 1) - cx) / (mapped_x - cx) : 1.0f;
      const float sy = mapped_y < 0.0f ? cy / (cy - mapped_y) :
                       mapped_y >= static_cast<float>(height) ?
                       (static_cast<float>(height - 1) - cy) / (mapped_y - cy) : 1.0f;
      max_scale = std::min(max_scale, std::min(sx, sy));
    }
  }

  if (!IsFinitePositive(max_scale) || max_scale > 1.0f) {
    max_scale = 1.0f;
  }

  return max_scale;
}

// ═════════════════════════════════════════════════════════════════════════════
//  Convenience free functions
// ═════════════════════════════════════════════════════════════════════════════

auto LoadLensDatabase(const std::filesystem::path& db_path)
    -> std::shared_ptr<LensDatabase> {
  auto db = std::make_shared<LensDatabase>();
  db->LoadDirectory(db_path);
  return db;
}

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
                  const LensCorrector::Config&  config) {
  if (!image) {
    throw DimensionMismatchError("Image pointer is null");
  }
  if (width <= 0 || height <= 0) {
    throw DimensionMismatchError("Invalid image dimensions: " +
                                 std::to_string(width) + "x" + std::to_string(height));
  }

  auto match = db.MatchLens(cam_maker, cam_model, lens_maker, lens_model,
                             focal_length_mm, aperture_f_number);
  if (!match.valid || !match.lens) {
    throw LensNotFoundError("No matching lens found for: " + lens_maker +
                            " " + lens_model);
  }

  LensCorrector corrector;
  corrector.SetDatabase(std::shared_ptr<const LensDatabase>(&db, [](const LensDatabase*) {}));
  corrector.SetConfig(config);

  const auto params = corrector.InterpolateParams(*match.lens, focal_length_mm,
                                                   aperture_f_number, kDefaultFarDistanceM);

  corrector.ApplyCorrection(image, width, height, params, config);
}

}  // namespace alcedo::lens
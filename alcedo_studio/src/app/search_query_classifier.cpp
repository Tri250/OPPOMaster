//  Copyright 2026 Yurun Zi
//  SPDX-License-Identifier: GPL-3.0-only
//  Additional permission under GPLv3 section 7 applies; see the LICENSE file.

#include "app/search_query_classifier.hpp"

#include <algorithm>
#include <cwctype>
#include <optional>
#include <regex>
#include <string>
#include <unordered_set>
#include <vector>

#include "storage/controller/semantic/semantic_label_config.hpp"
#include "utils/string/convert.hpp"

namespace alcedo {

namespace {

auto TrimWString(std::wstring value) -> std::wstring {
  const auto first = std::find_if_not(value.begin(), value.end(),
                                      [](wchar_t ch) { return std::iswspace(ch) != 0; });
  const auto last  = std::find_if_not(value.rbegin(), value.rend(), [](wchar_t ch) {
                      return std::iswspace(ch) != 0;
                    }).base();
  if (first >= last) {
    return {};
  }
  return std::wstring(first, last);
}

auto LowerCopy(std::wstring value) -> std::wstring {
  std::ranges::transform(value, value.begin(),
                         [](wchar_t ch) { return static_cast<wchar_t>(std::towlower(ch)); });
  return value;
}

auto SplitTokens(const std::wstring& query) -> std::vector<std::wstring> {
  std::vector<std::wstring> tokens;
  std::wstring              current;
  for (const auto ch : query) {
    if (std::iswspace(ch) != 0) {
      if (!current.empty()) {
        tokens.push_back(std::move(current));
        current.clear();
      }
    } else {
      current.push_back(ch);
    }
  }
  if (!current.empty()) {
    tokens.push_back(std::move(current));
  }
  return tokens;
}

// Match a full token against a regex (anchored). Compiled once and reused.
auto FullMatch(const std::wregex& re, const std::wstring& token) -> bool {
  std::wsmatch m;
  return std::regex_match(token, m, re);
}

const std::wregex& DateShortRe() {
  static const std::wregex re(LR"(^\d{4}$|^\d{6}$|^\d{8}$)");
  return re;
}
const std::wregex& DateGroupRe() {
  static const std::wregex re(LR"(^\d{4}[-/]\d{1,2}([-/]\d{1,2})?$)");
  return re;
}
const std::wregex& ApertureRe() {
  static const std::wregex re(LR"(^f/?\d+(\.\d+)?$)", std::regex::icase);
  return re;
}
const std::wregex& FocalLengthRe() {
  static const std::wregex re(LR"(^\d+(\.\d+)?mm$)", std::regex::icase);
  return re;
}
const std::wregex& IsoRe() {
  static const std::wregex re(LR"(^iso\s?\d+$)", std::regex::icase);
  return re;
}
const std::wregex& PureNumberRe() {
  static const std::wregex re(LR"(^\d+$)");
  return re;
}

const std::unordered_set<std::wstring>& CameraLensMakers() {
  static const std::unordered_set<std::wstring> makers = {
      // English
      L"canon",    L"nikon",     L"sony",       L"fuji",     L"fujifilm",
      L"leica",    L"panasonic", L"lumix",      L"olympus",  L"pentax",
      L"ricoh",    L"sigma",     L"tamron",     L"zeiss",    L"hasselblad",
      L"apple",    L"samsung",   L"phase",      L"mamiya",   L"contax",
      L"nikkor",   L"canon rf",  L"sony alpha", L"pentax k", L"pentax fa",
      L"sigma art",
      // Chinese brand names
      L"佳能",     L"尼康",      L"索尼",       L"富士",     L"徕卡",
      L"松下",     L"奥林巴斯",   L"宾得",      L"理光",     L"适马",
      L"腾龙",     L"蔡司",      L"哈苏",       L"三星"};
  return makers;
}

// Chinese photography terms that indicate metadata/structured search.
const std::unordered_set<std::wstring>& ChineseMetadataKeywords() {
  static const std::unordered_set<std::wstring> keywords = {
      // Camera & lens terms
      L"相机",   L"镜头",   L"光圈",   L"快门",   L"焦距",
      L"感光度", L"曝光",   L"对焦",   L"白平衡", L"像素",
      L"分辨率", L"格式",   L"大小",   L"尺寸",   L"文件",
      // Rating & labeling
      L"评分",   L"评星",   L"标签",   L"标记",   L"收藏",
      L"星级",   L"等级",
      // Date & time
      L"日期",   L"时间",   L"年",     L"月",     L"日",
      L"今天",   L"昨天",   L"前天",   L"本周",   L"上周",
      L"本月",   L"上月",   L"今年",   L"去年",
      // Category filters
      L"类型",   L"种类",   L"颜色",   L"色彩",   L"人像",
      L"风景",   L"街拍",   L"建筑",   L"美食",   L"动物",
      L"植物",   L"夜景",   L"微距",   L"航拍"};
  return keywords;
}

// Chinese word segments that strongly suggest natural language (semantic search).
const std::unordered_set<std::wstring>& ChineseSemanticKeywords() {
  static const std::unordered_set<std::wstring> keywords = {
      L"像",     L"类似",   L"感觉",   L"好像",   L"如同",
      L"温暖",   L"冷调",   L"柔和",   L"强烈",   L"梦幻",
      L"复古",   L"清新",   L"暗调",   L"明亮",   L"忧郁",
      L"浪漫",   L"宁静",   L"热闹",   L"孤独",   L"神秘",
      L"有",     L"包含",   L"带有",   L"拥有",   L"带着",
      L"在",     L"里",     L"中",     L"旁边",   L"上面",
      L"的"  // structural particle - usually indicates NL phrase
  };
  return keywords;
}

const std::unordered_set<std::wstring>& ImageExtensions() {
  static const std::unordered_set<std::wstring> exts = {
      L"jpg",  L"jpeg", L"png",  L"tif",  L"tiff", L"arw",  L"cr2",  L"cr3",
      L"nef",  L"raf",  L"dng",  L"orf",  L"rw2",  L"pef",  L"srf",  L"sr2",
      L"heic", L"heif", L"avif", L"webp", L"psd",  L"xcf",  L"mov",  L"mp4",
      L"m4v",  L"avi",  L"raw",  L"3fr",  L"iiq"};
  return exts;
}

auto TokenEndsInKnownExtension(const std::wstring& token_lower) -> bool {
  const auto dot = token_lower.rfind(L'.');
  if (dot == std::wstring::npos || dot == 0 || dot + 1 >= token_lower.size()) {
    return false;
  }
  const auto ext = token_lower.substr(dot + 1);
  return ImageExtensions().contains(ext);
}

auto IsMetadataToken(const std::wstring& raw_token) -> bool {
  const auto token = LowerCopy(raw_token);
  if (token.empty()) {
    return false;
  }
  if (token.find(L'/') != std::wstring::npos || token.find(L'\\') != std::wstring::npos) {
    return true;  // path / folder fragment
  }
  if (FullMatch(DateShortRe(), token) || FullMatch(DateGroupRe(), token)) {
    return true;
  }
  if (FullMatch(ApertureRe(), token) || FullMatch(FocalLengthRe(), token) ||
      FullMatch(IsoRe(), token)) {
    return true;
  }
  if (FullMatch(PureNumberRe(), token)) {
    return true;
  }
  if (TokenEndsInKnownExtension(token)) {
    return true;
  }
  if (CameraLensMakers().contains(token)) {
    return true;
  }
  // Chinese metadata keywords
  if (ChineseMetadataKeywords().contains(raw_token)) {
    return true;
  }
  return false;
}

/// Multi-keyword weighted scoring for mixed language queries.
/// Returns a score in [-1.0, 1.0]: positive => likely Traditional,
/// negative => likely Semantic.  Scores near 0 are ambiguous.
auto ComputeQueryRouteScore(const std::wstring& query) -> double {
  double score = 0.0;

  // Scan for Chinese metadata keywords (high positive weight)
  for (const auto& kw : ChineseMetadataKeywords()) {
    if (query.find(kw) != std::wstring::npos) {
      score += 0.4;
    }
  }

  // Scan for Chinese semantic keywords (negative weight)
  for (const auto& kw : ChineseSemanticKeywords()) {
    if (query.find(kw) != std::wstring::npos) {
      score -= 0.3;
    }
  }

  // Scan for English metadata tokens
  const auto tokens = SplitTokens(query);
  for (const auto& token : tokens) {
    if (IsMetadataToken(token)) {
      score += 0.3;
    }
  }

  // Count CJK vs non-CJK character ratio
  size_t cjk_count = 0;
  size_t total_alpha = 0;
  for (const auto ch : query) {
    if (IsCjk(ch)) {
      ++cjk_count;
    }
    if (std::iswalnum(ch) != 0 || IsCjk(ch)) {
      ++total_alpha;
    }
  }
  // If mostly CJK with no metadata hints, lean semantic
  if (total_alpha > 0 && cjk_count > total_alpha * 0.7 && score <= 0.0) {
    score -= 0.2;
  }

  return score;
}

/// Check if a query contains mixed Chinese and English content.
auto IsMixedLanguageQuery(const std::wstring& query) -> bool {
  bool has_cjk    = false;
  bool has_latin  = false;
  for (const auto ch : query) {
    if (IsCjk(ch)) has_cjk = true;
    if ((ch >= L'a' && ch <= L'z') || (ch >= L'A' && ch <= L'Z')) has_latin = true;
  }
  return has_cjk && has_latin;
}

auto IsCjk(wchar_t ch) -> bool {
  return (ch >= 0x3400 && ch <= 0x9FFF) || (ch >= 0xF900 && ch <= 0xFAFF) ||
         (ch >= 0x3000 && ch <= 0x303F) || (ch >= 0xFF00 && ch <= 0xFFEF);
}

// Resolve a (possibly #-prefixed) whole query to a canonical label id, or
// nullopt if it is not an exact label surface form in any language.
auto ResolveWholeQueryLabel(const std::wstring& query) -> std::optional<std::string> {
  std::wstring body = query;
  if (!body.empty() && body.front() == L'#') {
    body.erase(body.begin());
  }
  body = TrimWString(std::move(body));
  if (body.empty()) {
    return std::nullopt;
  }
  const auto utf8 = conv::ToBytes(body);
  return CanonicalSemanticLabel(utf8);
}

}  // namespace

std::size_t EstimatePromptTokens(const std::wstring& query) {
  std::size_t tokens   = 0;
  std::size_t word_len = 0;
  for (const auto ch : query) {
    if (IsCjk(ch)) {
      if (word_len > 0) {
        ++tokens;
        word_len = 0;
      }
      ++tokens;  // one CJK character ~ one token
    } else if (std::isspace(ch) != 0) {
      if (word_len > 0) {
        ++tokens;
        word_len = 0;
      }
    } else {
      ++word_len;
    }
  }
  if (word_len > 0) {
    ++tokens;
  }
  return tokens;
}

std::string_view SearchQueryRouteName(SearchQueryRoute route) {
  switch (route) {
    case SearchQueryRoute::Empty:
      return "empty";
    case SearchQueryRoute::Traditional:
      return "traditional";
    case SearchQueryRoute::Label:
      return "label";
    case SearchQueryRoute::Semantic:
      return "semantic";
  }
  return "empty";
}

SearchQueryClassification ClassifySearchQuery(const std::wstring& query,
                                              bool                semantic_toggle_enabled,
                                              std::size_t         max_prompt_tokens) {
  SearchQueryClassification result;
  result.normalized_query_ = TrimWString(query);
  if (result.normalized_query_.empty()) {
    result.route_ = SearchQueryRoute::Empty;
    return result;
  }

  // Exact label name, synonym, or #tag -> ordinary label search even with the
  // semantic toggle on.
  if (auto canonical = ResolveWholeQueryLabel(result.normalized_query_); canonical.has_value()) {
    result.route_         = SearchQueryRoute::Label;
    result.matched_label_ = std::move(*canonical);
    return result;
  }

  // Any metadata/EXIF/filename-shaped token -> ordinary search.
  const auto tokens = SplitTokens(result.normalized_query_);
  if (std::ranges::any_of(tokens, [](const std::wstring& token) {
        return IsMetadataToken(token);
      })) {
    result.route_ = SearchQueryRoute::Traditional;
    return result;
  }

  // For mixed-language or CJK-heavy queries, use weighted scoring.
  const bool mixed = IsMixedLanguageQuery(result.normalized_query_);
  bool has_cjk = false;
  for (const auto ch : result.normalized_query_) {
    if (IsCjk(ch)) { has_cjk = true; break; }
  }

  if (mixed || has_cjk) {
    const double route_score = ComputeQueryRouteScore(result.normalized_query_);
    // Score > 0.2: clearly metadata/traditional
    // Score < -0.1: clearly semantic/natural language
    // In between: ambiguous, lean on toggle
    if (route_score > 0.2) {
      result.route_ = SearchQueryRoute::Traditional;
      return result;
    }
    if (route_score < -0.1 && semantic_toggle_enabled) {
      result.route_    = SearchQueryRoute::Semantic;
      result.too_long_ = EstimatePromptTokens(result.normalized_query_) > max_prompt_tokens;
      return result;
    }
  }

  // Natural language.
  if (semantic_toggle_enabled) {
    result.route_    = SearchQueryRoute::Semantic;
    result.too_long_ = EstimatePromptTokens(result.normalized_query_) > max_prompt_tokens;
  } else {
    result.route_ = SearchQueryRoute::Traditional;
  }
  return result;
}

}  // namespace alcedo

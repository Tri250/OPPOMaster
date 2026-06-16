//  Copyright 2026 Yurun Zi
//  SPDX-License-Identifier: GPL-3.0-only
//  Additional permission under GPLv3 section 7 applies; see the LICENSE file.

#pragma once

#include <algorithm>
#include <cctype>
#include <cstddef>
#include <cstdint>
#include <optional>
#include <string>
#include <string_view>
#include <unordered_map>
#include <vector>

namespace alcedo {

inline constexpr const char* kDefaultSemanticPhotographyPromptConfigHash =
    "photography-labels-v4-en";
inline constexpr const char* kDefaultSemanticPhotographyZhPromptConfigHash =
    "photography-labels-v4-zh";
inline constexpr double kDefaultSemanticLabelConfidenceThreshold = 0.20;
inline constexpr double kDefaultSemanticLabelMarginThreshold     = 0.03;
inline constexpr size_t kMaxSemanticImageLabelCount              = 3;
inline constexpr size_t kDefaultSemanticLabelTopScoreCount       = kMaxSemanticImageLabelCount;

enum class SemanticLabelLanguage : uint8_t {
  kEnglish = 0,
  kChinese,
};

struct SemanticPhotographyLabelConfig {
  std::string canonical_label{};
  std::string english_label{};
  std::string chinese_label{};
  std::string english_query{};
  std::string chinese_query{};
};

struct SemanticLabelQueryConfig {
  std::string label{};            // Model-facing label text stored with prototypes.
  std::string query{};            // Model-facing prompt/query text.
  std::string canonical_label{};  // Stable English id used for cross-language mapping.
  std::string english_label{};
  std::string chinese_label{};
};

struct SemanticGenerationLabelPrototype {
  std::string        label{};
  std::vector<float> embedding{};
};

inline auto SemanticLabelLanguageForModel(std::string_view profile_id, std::string_view language)
    -> SemanticLabelLanguage {
  auto lower = [](std::string_view value) {
    std::string out(value);
    std::ranges::transform(out, out.begin(),
                           [](unsigned char ch) { return static_cast<char>(std::tolower(ch)); });
    return out;
  };
  const auto profile = lower(profile_id);
  const auto lang    = lower(language);
  if (lang == "zh" || lang == "zh-cn" || profile.ends_with("-zh")) {
    return SemanticLabelLanguage::kChinese;
  }
  return SemanticLabelLanguage::kEnglish;
}

inline auto SemanticPromptConfigHashForLanguage(SemanticLabelLanguage language) -> const char* {
  return language == SemanticLabelLanguage::kChinese ? kDefaultSemanticPhotographyZhPromptConfigHash
                                                     : kDefaultSemanticPhotographyPromptConfigHash;
}

inline auto SemanticSupportedTextLanguagesJson(SemanticLabelLanguage language) -> const char* {
  return language == SemanticLabelLanguage::kChinese ? R"(["zh"])" : R"(["en","zh"])";
}

inline auto DefaultSemanticPhotographyLabelDefinitions()
    -> const std::vector<SemanticPhotographyLabelConfig>& {
  static const std::vector<SemanticPhotographyLabelConfig> labels{
      {"portrait", "portrait", "人像",
       "a photograph whose main subject is one person or a close portrait",
       "一张以单人或近距离人像为主体的照片"},
      {"group", "group", "合影", "a photograph of several people together",
       "一张多人在一起的合影照片"},
      {"family", "family", "家庭", "a family photograph with relatives or close household members",
       "一张包含亲属或家庭成员的家庭照片"},
      {"children", "children", "儿童", "a photograph where children or babies are the main subject",
       "一张以儿童或婴儿为主体的照片"},
      {"wedding", "wedding", "婚礼",
       "a wedding photograph of a couple, ceremony, reception, or bridal party",
       "一张婚礼照片，包含新人、仪式、宴会或婚礼队伍"},
      {"ceremony", "ceremony", "仪式",
       "a graduation, award, religious, or formal ceremony photograph",
       "一张毕业、颁奖、宗教或正式仪式照片"},
      {"event", "event", "活动", "a social event, party, conference, or gathering photograph",
       "一张社交活动、派对、会议或聚会照片"},
      {"concert", "concert", "演唱会", "a concert or live music photograph",
       "一张演唱会或现场音乐照片"},
      {"performance", "performance", "表演",
       "a stage, theater, dance, or public performance photograph",
       "一张舞台、戏剧、舞蹈或公开表演照片"},
      {"fashion", "fashion", "时尚", "a fashion, model, outfit, or editorial portrait photograph",
       "一张时尚、模特、穿搭或编辑风格人像照片"},
      {"sports", "sports", "运动",
       "a sports photograph with athletes, games, races, or athletic activity",
       "一张包含运动员、比赛、竞速或体育活动的照片"},
      {"street", "street", "街头",
       "a street photography image of candid public life or urban scenes",
       "一张记录公共生活或城市场景的街头摄影照片"},
      {"cityscape", "cityscape", "城市风光", "a skyline or broad city view photograph",
       "一张天际线或宽阔城市景观照片"},
      {"architecture", "architecture", "建筑",
       "a photograph focused on buildings, facades, or structures",
       "一张以建筑、立面或结构为重点的照片"},
      {"interior", "interior", "室内",
       "an indoor room, interior design, or architectural interior photograph",
       "一张室内房间、室内设计或建筑室内照片"},
      {"landscape", "landscape", "风景", "a wide natural landscape photograph",
       "一张宽阔的自然风景照片"},
      {"mountain", "mountain", "山景", "a mountain, cliff, or alpine landscape photograph",
       "一张山脉、悬崖或高山景观照片"},
      {"forest", "forest", "森林", "a forest, woodland, trees, or trail photograph",
       "一张森林、林地、树木或小径照片"},
      {"desert", "desert", "沙漠", "a desert, dunes, arid land, or canyon photograph",
       "一张沙漠、沙丘、干旱地貌或峡谷照片"},
      {"beach", "beach", "海滩", "a beach, shoreline, seaside, or sandy coast photograph",
       "一张海滩、海岸线、海边或沙质海岸照片"},
      {"lake", "lake", "湖泊", "a lake, river, pond, or calm inland water photograph",
       "一张湖泊、河流、池塘或平静内陆水域照片"},
      {"waterfall", "waterfall", "瀑布", "a waterfall, cascade, or rushing water photograph",
       "一张瀑布、跌水或湍流水景照片"},
      {"garden", "garden", "花园",
       "a garden, park, cultivated plants, or landscaped greenery photograph",
       "一张花园、公园、栽培植物或景观绿化照片"},
      {"flower", "flower", "花卉", "a flower, blossom, or botanical close-up photograph",
       "一张花朵、花卉或植物特写照片"},
      {"wildlife", "wildlife", "野生动物",
       "a wild animal, bird, insect, or nature animal photograph",
       "一张野生动物、鸟类、昆虫或自然动物照片"},
      {"pet", "pet", "宠物", "a domestic pet such as a dog, cat, or companion animal photograph",
       "一张狗、猫或伴侣动物等家庭宠物照片"},
      {"food and drink", "food and drink", "餐饮",
       "a photograph of prepared food, drinks, dining, or table service",
       "一张准备好的食物、饮品、用餐或餐桌服务照片"},
      {"product", "product", "产品", "a product, merchandise, packaging, or ecommerce photograph",
       "一张产品、商品、包装或电商照片"},
      {"still life", "still life", "静物", "an arranged still life photograph of objects",
       "一张经过布置的物体静物照片"},
      {"vehicle", "vehicle", "交通工具",
       "a car, motorcycle, bicycle, aircraft, boat, or other vehicle photograph",
       "一张汽车、摩托车、自行车、飞机、船或其他交通工具照片"},
      {"artwork", "artwork", "艺术品",
       "a photograph of artwork, sculpture, mural, craft, or museum objects",
       "一张艺术品、雕塑、壁画、工艺品或博物馆物件照片"},
      {"document", "document", "文档",
       "a document, receipt, sign, whiteboard, page, or printed text photograph",
       "一张文档、票据、标牌、白板、页面或印刷文字照片"},
      {"screenshot", "screenshot", "截图", "a screenshot", "一张屏幕截图"},
      {"macro", "macro", "微距", "a macro or extreme close-up photograph with fine detail",
       "一张包含精细细节的微距或极近距离特写照片"},
      {"night", "night", "夜景", "a night, low-light, or dark scene photograph",
       "一张夜间、弱光或暗场景照片"},
      {"sunrise and sunset", "sunrise and sunset", "日出日落",
       "a sunrise, sunset, golden hour, or colorful sky photograph",
       "一张日出、日落、黄金时刻或彩色天空照片"},
      {"snow", "snow", "雪景", "a snow, ice, frost, or winter photograph",
       "一张雪、冰、霜或冬季照片"},
      {"autumn", "autumn", "秋天", "an autumn, fall foliage, or seasonal leaves photograph",
       "一张秋季、秋叶或季节性树叶照片"},
      {"fog", "fog", "雾", "a fog, mist, haze, or atmospheric weather photograph",
       "一张雾、薄雾、霾或大气天气照片"},
      {"black and white", "black and white", "黑白",
       "a black and white, monochrome, or grayscale photograph", "一张黑白、单色或灰度照片"},
      {"silhouette", "silhouette", "剪影", "a silhouette or strong backlit outline photograph",
       "一张剪影或强逆光轮廓照片"},
      {"aerial", "aerial", "航拍", "an aerial, drone, top-down, or high viewpoint photograph",
       "一张航拍、无人机、俯视或高视角照片"},
      {"panorama", "panorama", "全景", "a panoramic, wide aspect, or stitched landscape photograph",
       "一张全景、宽画幅或拼接风景照片"},
      {"long exposure", "long exposure", "长曝光",
       "a long exposure photograph with motion blur, smooth water, stars, or light trails",
       "一张包含运动模糊、平滑水面、星空或光轨的长曝光照片"},
      {"fireworks", "fireworks", "烟花",
       "a fireworks, sparkler, pyrotechnic, or celebration lights photograph",
       "一张烟花、仙女棒、烟火或庆祝灯光照片"},
      {"studio", "studio", "影棚",
       "a studio-lit photograph with controlled lighting or seamless background",
       "一张使用受控灯光或无缝背景的影棚照片"},
  };
  return labels;
}

inline auto MakeSemanticLabelQueryConfigs(SemanticLabelLanguage language)
    -> std::vector<SemanticLabelQueryConfig> {
  std::vector<SemanticLabelQueryConfig> queries;
  const auto&                           definitions = DefaultSemanticPhotographyLabelDefinitions();
  queries.reserve(definitions.size());
  for (const auto& entry : definitions) {
    queries.push_back(SemanticLabelQueryConfig{
        .label =
            language == SemanticLabelLanguage::kChinese ? entry.chinese_label : entry.english_label,
        .query =
            language == SemanticLabelLanguage::kChinese ? entry.chinese_query : entry.english_query,
        .canonical_label = entry.canonical_label,
        .english_label   = entry.english_label,
        .chinese_label   = entry.chinese_label,
    });
  }
  return queries;
}

inline auto DefaultSemanticPhotographyLabelQueries()
    -> const std::vector<SemanticLabelQueryConfig>& {
  static const std::vector<SemanticLabelQueryConfig> labels =
      MakeSemanticLabelQueryConfigs(SemanticLabelLanguage::kEnglish);
  return labels;
}

inline auto DefaultSemanticPhotographyLabelQueries(SemanticLabelLanguage language)
    -> const std::vector<SemanticLabelQueryConfig>& {
  if (language == SemanticLabelLanguage::kChinese) {
    static const std::vector<SemanticLabelQueryConfig> labels =
        MakeSemanticLabelQueryConfigs(SemanticLabelLanguage::kChinese);
    return labels;
  }
  return DefaultSemanticPhotographyLabelQueries();
}

inline auto NormalizeSemanticLabelKey(std::string value) -> std::string {
  const auto first = std::find_if_not(value.begin(), value.end(),
                                      [](unsigned char ch) { return std::isspace(ch) != 0; });
  const auto last  = std::find_if_not(value.rbegin(), value.rend(), [](unsigned char ch) {
                      return std::isspace(ch) != 0;
                    }).base();
  if (first >= last) {
    return {};
  }
  std::string out(first, last);
  std::ranges::transform(out, out.begin(),
                         [](unsigned char ch) { return static_cast<char>(std::tolower(ch)); });
  return out;
}

inline auto SemanticLabelCanonicalLookup() -> const std::unordered_map<std::string, std::string>& {
  static const std::unordered_map<std::string, std::string> lookup = [] {
    std::unordered_map<std::string, std::string> map;
    for (const auto& entry : DefaultSemanticPhotographyLabelDefinitions()) {
      map.emplace(NormalizeSemanticLabelKey(entry.canonical_label), entry.canonical_label);
      map.emplace(NormalizeSemanticLabelKey(entry.english_label), entry.canonical_label);
      map.emplace(NormalizeSemanticLabelKey(entry.chinese_label), entry.canonical_label);
    }
    return map;
  }();
  return lookup;
}

inline auto SemanticLabelByCanonicalLookup()
    -> const std::unordered_map<std::string, const SemanticPhotographyLabelConfig*>& {
  static const std::unordered_map<std::string, const SemanticPhotographyLabelConfig*> lookup = [] {
    std::unordered_map<std::string, const SemanticPhotographyLabelConfig*> map;
    for (const auto& entry : DefaultSemanticPhotographyLabelDefinitions()) {
      map.emplace(entry.canonical_label, &entry);
    }
    return map;
  }();
  return lookup;
}

inline auto CanonicalSemanticLabel(std::string_view label_text) -> std::optional<std::string> {
  const auto found =
      SemanticLabelCanonicalLookup().find(NormalizeSemanticLabelKey(std::string(label_text)));
  if (found == SemanticLabelCanonicalLookup().end()) {
    return std::nullopt;
  }
  return found->second;
}

inline auto SemanticLabelDisplayText(std::string_view label_text, SemanticLabelLanguage language)
    -> std::string {
  const auto canonical = CanonicalSemanticLabel(label_text);
  if (!canonical.has_value()) {
    return std::string(label_text);
  }
  const auto found = SemanticLabelByCanonicalLookup().find(*canonical);
  if (found == SemanticLabelByCanonicalLookup().end()) {
    return std::string(label_text);
  }
  return language == SemanticLabelLanguage::kChinese ? found->second->chinese_label
                                                     : found->second->english_label;
}

inline auto SemanticLabelAliases(std::string_view label_text) -> std::vector<std::string> {
  const auto canonical = CanonicalSemanticLabel(label_text);
  if (!canonical.has_value()) {
    return {std::string(label_text)};
  }
  const auto found = SemanticLabelByCanonicalLookup().find(*canonical);
  if (found == SemanticLabelByCanonicalLookup().end()) {
    return {std::string(label_text)};
  }
  std::vector<std::string> aliases{found->second->english_label};
  if (found->second->chinese_label != found->second->english_label) {
    aliases.push_back(found->second->chinese_label);
  }
  return aliases;
}

inline auto DefaultSemanticPhotographyLabels(
    SemanticLabelLanguage language = SemanticLabelLanguage::kEnglish) -> std::vector<std::string> {
  std::vector<std::string> labels;
  const auto&              queries = DefaultSemanticPhotographyLabelQueries(language);
  labels.reserve(queries.size());
  for (const auto& query : queries) {
    labels.push_back(query.label);
  }
  return labels;
}

}  // namespace alcedo

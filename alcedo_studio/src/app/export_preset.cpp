//  Copyright 2026 Yurun Zi
//  SPDX-License-Identifier: GPL-3.0-only
//  Additional permission under GPLv3 section 7 applies; see the LICENSE file.

#include "app/export_preset.hpp"

#include <algorithm>
#include <fstream>

#include <QFile>
#include <QJsonArray>
#include <QJsonDocument>
#include <QJsonObject>
#include <QStandardPaths>

namespace alcedo {

ExportPresetManager::ExportPresetManager() {
  const QString data_dir =
      QStandardPaths::writableLocation(QStandardPaths::AppDataLocation);
  storage_path_ = std::filesystem::path(data_dir.toStdString()) / "export_presets.json";

  InitializeBuiltInPresets();
  LoadCustomPresets();
}

void ExportPresetManager::InitializeBuiltInPresets() {
  built_in_presets_ = {
      // ---- Social media presets ----
      ExportPreset{
          .id            = "wechat_moments",
          .display_name  = QStringLiteral("微信朋友圈"),
          .description   = QStringLiteral("微信朋友圈最佳尺寸，JPEG 85%"),
          .width         = 1080,
          .height        = 1080,
          .quality       = 85,
          .format        = "JPEG",
          .color_space   = ExportPresetColorSpace::SRGB,
          .embed_icc     = true,
          .built_in      = true,
          .category      = "social_media",
          .icon_name     = "wechat",
      },
      ExportPreset{
          .id            = "wechat_avatar",
          .display_name  = QStringLiteral("微信头像"),
          .description   = QStringLiteral("微信头像尺寸，JPEG 90%"),
          .width         = 640,
          .height        = 640,
          .quality       = 90,
          .format        = "JPEG",
          .color_space   = ExportPresetColorSpace::SRGB,
          .embed_icc     = true,
          .built_in      = true,
          .category      = "social_media",
          .icon_name     = "wechat",
      },
      ExportPreset{
          .id            = "xiaohongshu",
          .display_name  = QStringLiteral("小红书"),
          .description   = QStringLiteral("小红书竖版最佳尺寸，JPEG 88%"),
          .width         = 1242,
          .height        = 1660,
          .quality       = 88,
          .format        = "JPEG",
          .color_space   = ExportPresetColorSpace::SRGB,
          .embed_icc     = true,
          .built_in      = true,
          .category      = "social_media",
          .icon_name     = "xiaohongshu",
      },
      ExportPreset{
          .id            = "weibo",
          .display_name  = QStringLiteral("微博"),
          .description   = QStringLiteral("微博最佳尺寸，JPEG 85%"),
          .width         = 1080,
          .height        = 1080,
          .quality       = 85,
          .format        = "JPEG",
          .color_space   = ExportPresetColorSpace::SRGB,
          .embed_icc     = true,
          .built_in      = true,
          .category      = "social_media",
          .icon_name     = "weibo",
      },
      ExportPreset{
          .id            = "douyin",
          .display_name  = QStringLiteral("抖音"),
          .description   = QStringLiteral("抖音竖版视频封面尺寸，JPEG 85%"),
          .width         = 1080,
          .height        = 1920,
          .quality       = 85,
          .format        = "JPEG",
          .color_space   = ExportPresetColorSpace::SRGB,
          .embed_icc     = true,
          .built_in      = true,
          .category      = "social_media",
          .icon_name     = "douyin",
      },
      // ---- Print presets ----
      ExportPreset{
          .id            = "a4_print",
          .display_name  = QStringLiteral("A4打印"),
          .description   = QStringLiteral("A4纸300DPI打印尺寸，JPEG 95%，AdobeRGB"),
          .width         = 3508,
          .height        = 2480,
          .quality       = 95,
          .format        = "JPEG",
          .color_space   = ExportPresetColorSpace::AdobeRGB,
          .embed_icc     = true,
          .built_in      = true,
          .category      = "print",
          .icon_name     = "printer",
      },
      ExportPreset{
          .id            = "a4_print_landscape",
          .display_name  = QStringLiteral("A4打印(横版)"),
          .description   = QStringLiteral("A4横版300DPI打印尺寸，JPEG 95%，AdobeRGB"),
          .width         = 2480,
          .height        = 3508,
          .quality       = 95,
          .format        = "JPEG",
          .color_space   = ExportPresetColorSpace::AdobeRGB,
          .embed_icc     = true,
          .built_in      = true,
          .category      = "print",
          .icon_name     = "printer",
      },
      // ---- Web presets ----
      ExportPreset{
          .id            = "web_general",
          .display_name  = QStringLiteral("网页通用"),
          .description   = QStringLiteral("通用网页图片尺寸，JPEG 80%"),
          .width         = 1920,
          .height        = 1080,
          .quality       = 80,
          .format        = "JPEG",
          .color_space   = ExportPresetColorSpace::SRGB,
          .embed_icc     = false,
          .built_in      = true,
          .category      = "web",
          .icon_name     = "web",
      },
      ExportPreset{
          .id            = "web_png",
          .display_name  = QStringLiteral("网页无损"),
          .description   = QStringLiteral("PNG无损网页图片"),
          .width         = 1920,
          .height        = 1080,
          .quality       = 100,
          .format        = "PNG",
          .color_space   = ExportPresetColorSpace::SRGB,
          .embed_icc     = true,
          .built_in      = true,
          .category      = "web",
          .icon_name     = "web",
      },
  };
}

auto ExportPresetManager::BuiltInPresets() const -> const std::vector<ExportPreset>& {
  return built_in_presets_;
}

auto ExportPresetManager::CustomPresets() const -> const std::vector<ExportPreset>& {
  return custom_presets_;
}

auto ExportPresetManager::AllPresets() const -> std::vector<ExportPreset> {
  std::vector<ExportPreset> all = built_in_presets_;
  all.insert(all.end(), custom_presets_.begin(), custom_presets_.end());
  return all;
}

auto ExportPresetManager::FindPreset(const QString& id) const -> std::optional<ExportPreset> {
  for (const auto& p : built_in_presets_) {
    if (p.id == id) return p;
  }
  for (const auto& p : custom_presets_) {
    if (p.id == id) return p;
  }
  return std::nullopt;
}

auto ExportPresetManager::SaveCustomPreset(const ExportPreset& preset) -> bool {
  // Check for existing preset with same id
  for (auto& existing : custom_presets_) {
    if (existing.id == preset.id) {
      existing = preset;
      existing.built_in = false;
      SaveCustomPresets();
      return true;
    }
  }

  ExportPreset new_preset = preset;
  new_preset.built_in = false;
  custom_presets_.push_back(std::move(new_preset));
  SaveCustomPresets();
  return true;
}

auto ExportPresetManager::DeleteCustomPreset(const QString& id) -> bool {
  auto it = std::find_if(custom_presets_.begin(), custom_presets_.end(),
                          [&id](const ExportPreset& p) { return p.id == id; });
  if (it == custom_presets_.end()) {
    return false;
  }
  custom_presets_.erase(it);
  SaveCustomPresets();
  return true;
}

auto ExportPresetManager::PresetToVariantMap(const ExportPreset& preset) -> QVariantMap {
  QVariantMap vm;
  vm["id"]          = preset.id;
  vm["displayName"] = preset.display_name;
  vm["description"] = preset.description;
  vm["width"]       = preset.width;
  vm["height"]      = preset.height;
  vm["quality"]     = preset.quality;
  vm["format"]      = preset.format;
  vm["colorSpace"]  = static_cast<int>(preset.color_space);
  vm["embedICC"]    = preset.embed_icc;
  vm["builtIn"]     = preset.built_in;
  vm["category"]    = preset.category;
  vm["iconName"]    = preset.icon_name;
  return vm;
}

auto ExportPresetManager::AllPresetsAsVariantList() const -> QVariantList {
  QVariantList result;
  for (const auto& p : AllPresets()) {
    result.append(PresetToVariantMap(p));
  }
  return result;
}

void ExportPresetManager::LoadCustomPresets() {
  if (!std::filesystem::exists(storage_path_)) {
    return;
  }

  try {
    QFile file(QString::fromStdString(storage_path_.string()));
    if (!file.open(QIODevice::ReadOnly)) return;

    QJsonDocument doc = QJsonDocument::fromJson(file.readAll());
    if (!doc.isArray()) return;

    custom_presets_.clear();
    const QJsonArray array = doc.array();
    for (const QJsonValue& val : array) {
      if (!val.isObject()) continue;
      QJsonObject obj = val.toObject();

      ExportPreset preset;
      preset.id            = obj.value("id").toString();
      preset.display_name  = obj.value("display_name").toString();
      preset.description   = obj.value("description").toString();
      preset.width         = obj.value("width").toInt();
      preset.height        = obj.value("height").toInt();
      preset.quality       = obj.value("quality").toInt();
      preset.format        = obj.value("format").toString("JPEG");
      preset.color_space   = static_cast<ExportPresetColorSpace>(
          obj.value("color_space").toInt(static_cast<int>(ExportPresetColorSpace::SRGB)));
      preset.embed_icc     = obj.value("embed_icc").toBool(true);
      preset.built_in      = false;
      preset.category      = obj.value("category").toString("custom");
      preset.icon_name     = obj.value("icon_name").toString();

      if (!preset.id.isEmpty()) {
        custom_presets_.push_back(std::move(preset));
      }
    }
  } catch (...) {
    custom_presets_.clear();
  }
}

void ExportPresetManager::SaveCustomPresets() const {
  try {
    std::error_code ec;
    std::filesystem::create_directories(storage_path_.parent_path(), ec);

    QJsonArray array;
    for (const auto& p : custom_presets_) {
      QJsonObject obj;
      obj["id"]           = p.id;
      obj["display_name"] = p.display_name;
      obj["description"]  = p.description;
      obj["width"]        = p.width;
      obj["height"]       = p.height;
      obj["quality"]      = p.quality;
      obj["format"]       = p.format;
      obj["color_space"]  = static_cast<int>(p.color_space);
      obj["embed_icc"]    = p.embed_icc;
      obj["category"]     = p.category;
      obj["icon_name"]    = p.icon_name;
      array.append(obj);
    }

    QFile file(QString::fromStdString(storage_path_.string()));
    if (file.open(QIODevice::WriteOnly | QIODevice::Truncate)) {
      file.write(QJsonDocument(array).toJson());
    }
  } catch (...) {
    // Best-effort save
  }
}

auto ExportPresetManager::CustomPresetFilePath() const -> std::filesystem::path {
  return storage_path_;
}

auto ExportPresetManager::CategoryDisplayName(const QString& category) -> QString {
  if (category == "social_media") return QStringLiteral("社交媒体");
  if (category == "print")        return QStringLiteral("打印输出");
  if (category == "web")          return QStringLiteral("网页");
  if (category == "custom")       return QStringLiteral("自定义");
  return category;
}

}  // namespace alcedo

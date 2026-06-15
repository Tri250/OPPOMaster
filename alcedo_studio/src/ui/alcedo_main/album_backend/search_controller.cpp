//  Copyright 2026 Yurun Zi
//  SPDX-License-Identifier: GPL-3.0-only
//  Additional permission under GPLv3 section 7 applies; see the LICENSE file.

#include "ui/alcedo_main/album_backend/search_controller.hpp"

#include <QCoreApplication>
#include <QDate>
#include <QImage>
#include <QMetaObject>
#include <QPointer>
#include <algorithm>
#include <exception>
#include <limits>
#include <thread>

#include "app/thumbnail_service.hpp"
#include "image/image.hpp"
#include "ui/alcedo_main/album_backend/album_backend.hpp"
#include "ui/alcedo_main/album_backend/path_utils.hpp"
#include "ui/alcedo_main/i18n.hpp"

namespace alcedo::ui {

using namespace album_util;

namespace {

#define SEARCH_TEXT(text, ...)                 \
  i18n::MakeLocalizedText(ALCEDO_I18N_CONTEXT, \
                          QT_TRANSLATE_NOOP(ALCEDO_I18N_CONTEXT, text) __VA_OPT__(, ) __VA_ARGS__)

auto SearchPreviewThumbnailResolution(uint maxEdge) -> ThumbnailResolution {
  return maxEdge <= 256   ? ThumbnailResolution::k256
         : maxEdge <= 512 ? ThumbnailResolution::k512
                          : ThumbnailResolution::k1024;
}

auto CurrentExceptionText(const char* fallback) -> QString {
  try {
    throw;
  } catch (const std::exception& e) {
    return QString::fromUtf8(e.what());
  } catch (...) {
    return QString::fromUtf8(fallback);
  }
}

}  // namespace

SearchController::SearchController(AlbumBackend& backend) : backend_(backend) {}

SearchController::~SearchController() { CancelSearchPreviewThumbnails(); }

bool SearchController::HasActiveSearchFilter() const {
  return active_search_filter_where_.has_value() && !active_search_filter_where_->empty();
}

auto SearchController::ActiveSearchFilterWhere() const -> const std::optional<std::wstring>& {
  return active_search_filter_where_;
}

auto SearchController::SearchRecommendations(int limit) -> QVariantList {
  if (limit <= 0) {
    return {};
  }
  return backend_.stats_.BuildSearchRecommendations(limit);
}

auto SearchController::SearchPreview(const QString& query, int offset, int limit) -> QVariantMap {
  QVariantMap   response{{"rows", QVariantList{}},
                         {"offset", std::max(0, offset)},
                         {"limit", std::max(0, limit)},
                         {"total", 0},
                         {"hasMore", false}};
  QVariantList  rows;
  const QString trimmed = query.trimmed();
  if (trimmed.isEmpty() || limit <= 0) {
    response["rows"] = rows;
    return response;
  }

  auto proj = backend_.project_handler_.project();
  if (!proj) {
    response["rows"] = rows;
    return response;
  }
  auto filter_service = proj->GetSleeveFilterService();
  if (!filter_service) {
    response["rows"] = rows;
    return response;
  }
  const auto folder_id = backend_.folder_ctrl_.CurrentFolderElementId();
  if (!folder_id.has_value()) {
    response["rows"] = rows;
    return response;
  }

  try {
    const auto safe_offset = std::max(0, offset);
    const auto safe_limit  = std::max(0, limit);
    const auto total       = filter_service->CountSearchResults(folder_id.value(),
                                                                trimmed.toStdWString());
    const auto matches     = filter_service->SearchFolder(
        folder_id.value(), trimmed.toStdWString(), static_cast<size_t>(safe_offset),
        static_cast<size_t>(safe_limit));
    response["offset"]  = safe_offset;
    response["limit"]   = safe_limit;
    response["total"]   = static_cast<int>(std::min<size_t>(
        total, static_cast<size_t>(std::numeric_limits<int>::max())));
    response["hasMore"] = static_cast<size_t>(safe_offset) + matches.size() < total;

    rows.reserve(static_cast<qsizetype>(matches.size()));
    for (const auto& match : matches) {
      QVariantMap row{{"elementId", static_cast<uint>(match.file_id_)},
                      {"fileId", static_cast<uint>(match.file_id_)},
                      {"imageId", static_cast<uint>(match.image_id_)},
                      {"fileName", QString::fromUtf8(match.file_name_.c_str())},
                      {"cameraModel", SEARCH_TEXT("Unknown").Render()},
                      {"lens", QString{}},
                      {"captureDate", QStringLiteral("--")},
                      {"rating", 0},
                      {"thumbUrl", QString{}},
                      {"thumbLoading", false},
                      {"thumbMissingSource", false},
                      {"thumbErrorText", QString{}}};

      if (const auto* item = backend_.FindAlbumItem(match.file_id_); item != nullptr) {
        row["thumbUrl"]           = item->thumb_data_url;
        row["thumbLoading"]       = item->thumb_loading;
        row["thumbMissingSource"] = item->thumb_missing_source;
        row["thumbErrorText"]     = item->thumb_error_text;
      }

      try {
        proj->GetImagePoolService()->Read<void>(
            match.image_id_, [&row](std::shared_ptr<Image> image) {
              if (!image) {
                return;
              }
              if (!image->image_name_.empty()) {
                row["fileName"] = album_util::WStringToQString(image->image_name_);
              }
              const auto& exif = image->exif_display_;
              if (!exif.model_.empty()) {
                row["cameraModel"] = QString::fromUtf8(exif.model_.c_str());
              }
              row["lens"]              = QString::fromUtf8(exif.lens_.c_str());
              const QDate capture_date = album_util::DateFromExifString(exif.date_time_str_);
              if (capture_date.isValid()) {
                row["captureDate"] = capture_date.toString(QStringLiteral("yyyy-MM-dd"));
              }
              row["rating"] = exif.rating_;
            });
      } catch (...) {
      }

      rows.push_back(std::move(row));
    }
  } catch (...) {
  }
  response["rows"] = rows;
  return response;
}

void SearchController::ApplyFuzzySearch(const QString& query) {
  const QString trimmed = query.trimmed();
  if (trimmed.isEmpty()) {
    ClearFuzzySearch();
    return;
  }

  auto proj = backend_.project_handler_.project();
  if (!proj) {
    return;
  }
  auto filter_service = proj->GetSleeveFilterService();
  if (!filter_service) {
    return;
  }

  auto where = filter_service->BuildFuzzySearchWhere(trimmed.toStdWString());
  if (!where.has_value()) {
    ClearFuzzySearch();
    return;
  }

  active_search_query_        = trimmed;
  active_search_filter_where_ = std::move(where);
  backend_.stats_.ClearFilters();
  backend_.stats_.RebuildThumbnailView();
  backend_.stats_.RefreshStats();
  emit backend_.StatsFilterChanged();
  emit SearchStateChanged();
}

void SearchController::ApplyExactSearch(uint elementId) {
  if (elementId == 0) {
    return;
  }

  auto proj = backend_.project_handler_.project();
  if (!proj) {
    return;
  }
  auto filter_service = proj->GetSleeveFilterService();
  if (!filter_service) {
    return;
  }

  active_search_query_ =
      SEARCH_TEXT("Image %1", QString::number(static_cast<qulonglong>(elementId))).Render();
  active_search_filter_where_ =
      filter_service->BuildExactFileWhere(static_cast<sl_element_id_t>(elementId));
  backend_.stats_.ClearFilters();
  backend_.stats_.RebuildThumbnailView();
  backend_.stats_.RefreshStats();
  emit backend_.StatsFilterChanged();
  emit SearchStateChanged();
}

void SearchController::ClearFuzzySearch() {
  if (active_search_query_.isEmpty() && !active_search_filter_where_.has_value()) {
    return;
  }
  ClearSearchState(true);
  backend_.stats_.RebuildThumbnailView();
  backend_.stats_.RefreshStats();
}

void SearchController::SetSearchPreviewThumbnailVisible(uint elementId, uint imageId, bool visible,
                                                        uint maxEdge) {
  if (elementId == 0 || imageId == 0) {
    return;
  }

  const ThumbnailCacheKey key{static_cast<sl_element_id_t>(elementId),
                              SearchPreviewThumbnailResolution(maxEdge)};
  if (!visible) {
    search_preview_visible_thumbnails_.erase(key);
    search_preview_thumbnail_requests_.erase(key);

    auto thumb_svc = backend_.project_handler_.thumbnail_service();
    if (!thumb_svc) {
      return;
    }
    try {
      thumb_svc->ReleaseThumbnail(key);
    } catch (...) {
    }
    return;
  }

  search_preview_visible_thumbnails_[key] = static_cast<image_id_t>(imageId);
  RequestSearchPreviewThumbnail(elementId, imageId, maxEdge);
}

void SearchController::RequestSearchPreviewThumbnail(uint elementId, uint imageId, uint maxEdge) {
  if (elementId == 0 || imageId == 0) {
    return;
  }

  auto thumb_svc = backend_.project_handler_.thumbnail_service();
  if (!thumb_svc) {
    return;
  }

  const auto              resolution = SearchPreviewThumbnailResolution(maxEdge);
  const ThumbnailCacheKey key{static_cast<sl_element_id_t>(elementId), resolution};
  const auto              request_generation = search_preview_generation_;
  const auto              expected_image_id  = static_cast<image_id_t>(imageId);

  const auto visible_it = search_preview_visible_thumbnails_.find(key);
  if (visible_it == search_preview_visible_thumbnails_.end() || visible_it->second != expected_image_id) {
    return;
  }

  if (const auto* item = backend_.FindAlbumItem(static_cast<sl_element_id_t>(elementId));
      item != nullptr && !item->thumb_data_url.isEmpty()) {
    emit SearchPreviewThumbnailUpdated(elementId, item->thumb_data_url, false,
                                       item->thumb_missing_source, item->thumb_error_text);
    emit searchPreviewThumbnailUpdated(elementId, item->thumb_data_url, false,
                                       item->thumb_missing_source, item->thumb_error_text);
    return;
  }

  if (search_preview_thumbnail_requests_.find(key) != search_preview_thumbnail_requests_.end()) {
    return;
  }
  const auto request_id = ++search_preview_request_sequence_;
  search_preview_thumbnail_requests_.emplace(key, request_id);

  emit SearchPreviewThumbnailUpdated(elementId, QString{}, true, false, QString{});
  emit searchPreviewThumbnailUpdated(elementId, QString{}, true, false, QString{});

  CallbackDispatcher dispatcher = [](std::function<void()> fn) {
    auto* app = QCoreApplication::instance();
    if (!app) {
      fn();
      return;
    }
    QMetaObject::invokeMethod(app, std::move(fn), Qt::QueuedConnection);
  };

  QPointer<SearchController> self(this);
  try {
    thumb_svc->GetThumbnailDetailed(
        static_cast<sl_element_id_t>(elementId), static_cast<image_id_t>(imageId),
        [self, service = thumb_svc, elementId, imageId, maxEdge, key,
         request_generation, request_id](ThumbnailRequestResult result) {
          auto release_thumbnail = [&]() {
            if (service) {
              try {
                service->ReleaseThumbnail(key);
              } catch (...) {
              }
            }
          };

          if (!self) {
            release_thumbnail();
            return;
          }
          if (self->search_preview_generation_ != request_generation) {
            release_thumbnail();
            return;
          }
          const auto request_it = self->search_preview_thumbnail_requests_.find(key);
          if (request_it == self->search_preview_thumbnail_requests_.end() ||
              request_it->second != request_id) {
            release_thumbnail();
            return;
          }
          const auto visible_it = self->search_preview_visible_thumbnails_.find(key);
          if (visible_it == self->search_preview_visible_thumbnails_.end() ||
              visible_it->second != static_cast<image_id_t>(imageId)) {
            self->search_preview_thumbnail_requests_.erase(key);
            release_thumbnail();
            return;
          }
          if (result.status != ThumbnailRequestStatus::kReady || !result.guard ||
              !result.guard->thumbnail_buffer_) {
            self->search_preview_thumbnail_requests_.erase(key);
            emit self->SearchPreviewThumbnailUpdated(
                elementId, QString{}, false, false,
                result.message.empty() ? QObject::tr("Thumbnail render returned no image.")
                                       : QString::fromUtf8(result.message));
            emit self->searchPreviewThumbnailUpdated(
                elementId, QString{}, false, false,
                result.message.empty() ? QObject::tr("Thumbnail render returned no image.")
                                       : QString::fromUtf8(result.message));
            release_thumbnail();
            return;
          }

          std::thread([self, service, elementId, imageId, maxEdge, key, request_generation,
                       request_id,
                       guard = std::move(result.guard)]() mutable {
            QString data_url;
            QString error_text;
            try {
              auto* buffer = guard->thumbnail_buffer_.get();
              if (buffer && !buffer->cpu_data_valid_ && buffer->gpu_data_valid_) {
                buffer->SyncToCPU();
              }
              if (buffer && buffer->cpu_data_valid_) {
                QImage image = album_util::MatRgba32fToQImageCopy(buffer->GetCPUData());
                if (!image.isNull()) {
                  const int edge = static_cast<int>(std::max<uint>(1, maxEdge));
                  data_url       = album_util::DataUrlFromImage(
                      image.scaled(edge, edge, Qt::KeepAspectRatio, Qt::SmoothTransformation));
                }
              }
              if (data_url.isEmpty()) {
                error_text = QObject::tr("Thumbnail conversion produced no image.");
              }
            } catch (...) {
              error_text = CurrentExceptionText("Unknown thumbnail conversion error.");
            }

            if (self) {
              QMetaObject::invokeMethod(
                  self,
                  [self, service, elementId, imageId, key, request_generation, request_id, data_url,
                   error_text]() {
                    if (!self) {
                      if (service) {
                        try {
                          service->ReleaseThumbnail(key);
                        } catch (...) {
                        }
                      }
                      return;
                    }
                    if (self->search_preview_generation_ != request_generation) {
                      if (service) {
                        try {
                          service->ReleaseThumbnail(key);
                        } catch (...) {
                        }
                      }
                      return;
                    }
                    const auto request_it = self->search_preview_thumbnail_requests_.find(key);
                    if (request_it == self->search_preview_thumbnail_requests_.end() ||
                        request_it->second != request_id) {
                      if (service) {
                        try {
                          service->ReleaseThumbnail(key);
                        } catch (...) {
                        }
                      }
                      return;
                    }
                    const auto visible_it = self->search_preview_visible_thumbnails_.find(key);
                    if (visible_it == self->search_preview_visible_thumbnails_.end() ||
                        visible_it->second != static_cast<image_id_t>(imageId)) {
                      self->search_preview_thumbnail_requests_.erase(key);
                      if (service) {
                        try {
                          service->ReleaseThumbnail(key);
                        } catch (...) {
                        }
                      }
                      return;
                    }
                    self->search_preview_thumbnail_requests_.erase(key);
                    emit self->SearchPreviewThumbnailUpdated(elementId, data_url, false, false,
                                                             error_text);
                    emit self->searchPreviewThumbnailUpdated(elementId, data_url, false, false,
                                                             error_text);
                  },
                  Qt::QueuedConnection);
            } else if (service) {
              try {
                service->ReleaseThumbnail(key);
              } catch (...) {
              }
            }
          }).detach();
        },
        true, dispatcher, resolution);
  } catch (...) {
    search_preview_thumbnail_requests_.erase(key);
    emit SearchPreviewThumbnailUpdated(elementId, QString{}, false, false,
                                       CurrentExceptionText("Unknown thumbnail request error."));
    emit searchPreviewThumbnailUpdated(elementId, QString{}, false, false,
                                       CurrentExceptionText("Unknown thumbnail request error."));
  }
}

void SearchController::CancelSearchPreviewThumbnails() {
  ++search_preview_generation_;
  if (search_preview_thumbnail_requests_.empty() && search_preview_visible_thumbnails_.empty()) {
    return;
  }

  std::unordered_map<ThumbnailCacheKey, bool> keys_to_release;
  for (const auto& [key, image_id] : search_preview_visible_thumbnails_) {
    (void)image_id;
    keys_to_release.emplace(key, true);
  }
  for (const auto& [key, request_id] : search_preview_thumbnail_requests_) {
    (void)request_id;
    keys_to_release.emplace(key, true);
  }

  auto thumb_svc = backend_.project_handler_.thumbnail_service();
  search_preview_visible_thumbnails_.clear();
  search_preview_thumbnail_requests_.clear();
  if (!thumb_svc) {
    return;
  }

  for (const auto& [key, present] : keys_to_release) {
    (void)present;
    try {
      thumb_svc->ReleaseThumbnail(key);
    } catch (...) {
    }
  }
}

void SearchController::ClearSearchState(bool emitSignal) {
  active_search_query_.clear();
  active_search_filter_where_.reset();
  if (emitSignal) {
    emit SearchStateChanged();
  }
}

}  // namespace alcedo::ui

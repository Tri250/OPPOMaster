//  Copyright 2026 Yurun Zi
//  SPDX-License-Identifier: GPL-3.0-only
//  Additional permission under GPLv3 section 7 applies; see the LICENSE file.

#pragma once

#include <QObject>
#include <QString>
#include <QVariantList>
#include <QVariantMap>
#include <cstdint>
#include <optional>
#include <string>
#include <unordered_map>

#include "app/sleeve_filter_service.hpp"
#include "app/thumbnail_types.hpp"

namespace alcedo::ui {

class AlbumBackend;

class SearchController final : public QObject {
  Q_OBJECT
  Q_PROPERTY(QString activeSearchQuery READ active_search_query NOTIFY SearchStateChanged)
  Q_PROPERTY(bool semanticSearchEnabled READ semantic_search_enabled NOTIFY SearchStateChanged)

 public:
  explicit SearchController(AlbumBackend& backend);
  ~SearchController() override;

  [[nodiscard]] const QString& active_search_query() const { return active_search_query_; }
  [[nodiscard]] bool semantic_search_enabled() const { return semantic_search_enabled_; }
  [[nodiscard]] bool HasActiveSearchFilter() const;
  [[nodiscard]] auto ActiveSearchFilterWhere() const -> const std::optional<std::wstring>&;

  Q_INVOKABLE QVariantList SearchRecommendations(int limit = 12);
  Q_INVOKABLE QVariantMap  SearchPreview(const QString& query, int offset = 0, int limit = 24);
  /// Explicit-submit entry used by Enter and the Search button. For a semantic
  /// route this is the only path that may reach the semantic provider; typing
  /// (SearchPreview) never does.
  Q_INVOKABLE QVariantMap  SubmitSearch(const QString& query, int offset = 0, int limit = 24);
  Q_INVOKABLE qulonglong   RequestSubmitSearch(const QString& query, int offset = 0,
                                                int limit = 24,
                                                const QString& mode = QStringLiteral("replace"));
  /// Stable route name for the current toggle: "empty"|"traditional"|"label"|
  /// "semantic". Centralizes routing in C++ so QML never decides runtime behavior.
  Q_INVOKABLE QString      ClassifyQuery(const QString& query) const;
  Q_INVOKABLE void         SetSemanticSearchEnabled(bool enabled);
  Q_INVOKABLE void         ApplyFuzzySearch(const QString& query);
  Q_INVOKABLE void         ApplyExactSearch(uint elementId);
  Q_INVOKABLE void         ClearFuzzySearch();
  Q_INVOKABLE void SetSearchPreviewThumbnailVisible(uint elementId, uint imageId, bool visible,
                                                    uint maxEdge = 192);
  Q_INVOKABLE void CancelSearchPreviewThumbnails();

  void ClearSearchState(bool emitSignal = true);

 signals:
  void SearchStateChanged();
  void SearchPreviewThumbnailUpdated(uint elementId, const QString& dataUrl, bool loading,
                                     bool missingSource, const QString& errorText);
  void searchPreviewThumbnailUpdated(uint elementId, const QString& dataUrl, bool loading,
                                     bool missingSource, const QString& errorText);
  void SearchResponseReady(qulonglong requestId, const QString& mode, const QVariantMap& response);
  void searchResponseReady(qulonglong requestId, const QString& mode, const QVariantMap& response);

 private:
  void RequestSearchPreviewThumbnail(uint elementId, uint imageId, uint maxEdge = 192);
  /// Shared ordinary (traditional/label) preview builder used by SearchPreview
  /// and SubmitSearch for non-semantic routes. `route_name` is stamped on the
  /// response for QML/tests.
  QVariantMap RunTraditionalPreview(const QString& query, int offset, int limit,
                                    const std::string& route_name);
  /// Build enriched preview rows (thumbnails/EXIF) shared by the traditional and
  /// semantic preview paths.
  QVariantList BuildResultRows(const std::vector<alcedo::FuzzySearchMatch>& matches);
  qulonglong   RequestSearch(const QString& query, int offset, int limit, const QString& mode,
                              bool submit);

  AlbumBackend& backend_;
  QString       active_search_query_{};
  std::optional<std::wstring> active_search_filter_where_{};
  bool          semantic_search_enabled_ = false;
  std::uint64_t search_response_request_sequence_ = 0;
  std::uint64_t search_preview_generation_       = 0;
  std::uint64_t search_preview_request_sequence_ = 0;
  std::unordered_map<ThumbnailCacheKey, image_id_t> search_preview_visible_thumbnails_{};
  std::unordered_map<ThumbnailCacheKey, std::uint64_t> search_preview_thumbnail_requests_{};
};

}  // namespace alcedo::ui

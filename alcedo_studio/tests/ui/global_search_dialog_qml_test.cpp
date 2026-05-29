//  Copyright 2026 Yurun Zi
//  SPDX-License-Identifier: GPL-3.0-only
//  Additional permission under GPLv3 section 7 applies; see the LICENSE file.

#include "ui/album_backend_test_fixture.hpp"

#include <QApplication>
#include <QByteArray>
#include <QImage>
#include <QJsonArray>
#include <QJsonDocument>
#include <QJsonObject>
#include <QQmlApplicationEngine>
#include <QQmlContext>
#include <QQuickItem>
#include <QQuickStyle>
#include <QSignalSpy>
#include <QUrl>

#include <algorithm>
#include <chrono>
#include <filesystem>
#include <functional>
#include <iomanip>
#include <optional>
#include <sstream>
#include <string>
#include <vector>

#include "ui/alcedo_main/album_backend/search_controller.hpp"
#include "ui/alcedo_main/app_theme.hpp"

namespace alcedo::ui::test {
namespace {

using GlobalSearchDialogQmlTests = AlbumBackendTestFixture;

constexpr int kPageSize        = 24;
constexpr int kSearchItemCount = 30;

constexpr char kHarnessQml[] = R"(
import QtQuick
import QtQuick.Controls

ApplicationWindow {
    id: root
    width: 1280
    height: 900
    visible: true
    color: "#111214"

    property alias dialog: dialogLoader.item

    Loader {
        id: dialogLoader
        anchors.fill: parent
        asynchronous: false
        source: dialogSourceUrl

        onLoaded: {
            if (!item) {
                return
            }
            item.backend = albumBackend
            item.theme = null
            item.blurSource = null
            item.cornerRadius = 0
        }
    }
}
)";

void WaitForImportFinished(AlbumBackend& backend, int timeoutMs = 180000) {
  QSignalSpy spy(&backend, &AlbumBackend::ImportStateChanged);
  const int  stepMs  = 200;
  int        waited  = 0;

  while (backend.ImportRunning() && waited < timeoutMs) {
    spy.wait(stepMs);
    waited += stepMs;
  }

  ProcessEvents(500);
}

auto WaitUntil(const std::function<bool()>& predicate, int timeoutMs,
               int stepMs = 50) -> bool {
  const auto deadline = std::chrono::steady_clock::now() + std::chrono::milliseconds(timeoutMs);
  while (std::chrono::steady_clock::now() < deadline) {
    if (predicate()) {
      return true;
    }
    ProcessEvents(stepMs);
  }
  return predicate();
}

auto PickScrollableSearchSource() -> std::filesystem::path {
  for (const std::string& subdir :
       {"batch_import", "portrait/dng", "landscape", "airplane", "plant"}) {
    auto images = CollectRawTestImages(subdir, 1);
    if (!images.empty()) {
      return images.front();
    }
  }
  return {};
}

auto MakeSearchDataset(const std::filesystem::path& tempDir, int count)
    -> std::vector<std::filesystem::path> {
  const auto source = PickScrollableSearchSource();
  if (source.empty()) {
    return {};
  }

  const auto datasetDir = tempDir / "global_search_dialog_scroll_dataset";
  std::filesystem::create_directories(datasetDir);

  std::vector<std::filesystem::path> paths;
  paths.reserve(static_cast<size_t>(count));

  for (int i = 0; i < count; ++i) {
    std::ostringstream name;
    name << "scroll_search_" << std::setw(2) << std::setfill('0') << i
         << source.extension().string();
    const auto dst = datasetDir / name.str();
    std::filesystem::copy_file(source, dst, std::filesystem::copy_options::overwrite_existing);
    paths.push_back(dst);
  }

  return paths;
}

auto GlobalSearchDialogFileUrl() -> QUrl {
  const auto qmlPath = std::filesystem::path(ALCEDO_TEST_SRC_DIR) / "ui" / "alcedo_main" / "qml" /
                       "GlobalSearchDialog.qml";
#ifdef _WIN32
  return QUrl::fromLocalFile(QString::fromStdWString(qmlPath.wstring()));
#else
  return QUrl::fromLocalFile(QString::fromStdString(qmlPath.string()));
#endif
}

auto FindSearchField(QObject* root) -> QObject* {
  if (root == nullptr) {
    return nullptr;
  }
  const auto objects = root->findChildren<QObject*>();
  const auto it = std::find_if(objects.begin(), objects.end(), [](QObject* object) {
    if (object == nullptr) {
      return false;
    }
    const QString className = QString::fromLatin1(object->metaObject()->className());
    return className.contains(QStringLiteral("TextInput")) &&
           object->property("text").isValid();
  });
  return it != objects.end() ? *it : nullptr;
}

auto FindVisibleListView(QObject* root) -> QObject* {
  if (root == nullptr) {
    return nullptr;
  }
  const auto objects = root->findChildren<QObject*>();
  const auto it = std::find_if(objects.begin(), objects.end(), [](QObject* object) {
    if (object == nullptr) {
      return false;
    }
    const QString className = QString::fromLatin1(object->metaObject()->className());
    if (!className.contains(QStringLiteral("ListView"))) {
      return false;
    }
    return object->property("visible").toBool() && object->property("count").isValid() &&
           object->property("height").toReal() > 0.0;
  });
  return it != objects.end() ? *it : nullptr;
}

void CollectQuickTreeObjects(QObject* root, std::vector<QObject*>& objects) {
  if (root == nullptr) {
    return;
  }

  objects.push_back(root);

  if (auto* item = qobject_cast<QQuickItem*>(root); item != nullptr) {
    const auto visualChildren = item->childItems();
    for (QQuickItem* child : visualChildren) {
      CollectQuickTreeObjects(child, objects);
    }
    return;
  }

  const auto childObjects = root->children();
  for (QObject* child : childObjects) {
    CollectQuickTreeObjects(child, objects);
  }
}

auto CollectSearchRows(QObject* root) -> std::vector<QObject*> {
  std::vector<QObject*> rows;
  if (root == nullptr) {
    return rows;
  }

  std::vector<QObject*> objects;
  CollectQuickTreeObjects(root, objects);
  rows.reserve(static_cast<size_t>(objects.size()));
  for (QObject* object : objects) {
    if (object == nullptr || !object->property("elementId").isValid() ||
        !object->property("thumbReady").isValid()) {
      continue;
    }
    if (object->property("elementId").toInt() <= 0) {
      continue;
    }
    rows.push_back(object);
  }
  return rows;
}

auto RowOrdinal(QObject* row) -> std::optional<int> {
  if (row == nullptr) {
    return std::nullopt;
  }

  const QString title  = row->property("title").toString();
  const QString prefix = QStringLiteral("scroll_search_");
  const int     start  = title.indexOf(prefix);
  if (start < 0) {
    return std::nullopt;
  }

  QString digits;
  for (int i = start + prefix.size(); i < title.size(); ++i) {
    const QChar ch = title.at(i);
    if (!ch.isDigit()) {
      break;
    }
    digits.append(ch);
  }

  bool ok = false;
  const int ordinal = digits.toInt(&ok);
  return ok ? std::optional<int>{ordinal} : std::nullopt;
}

auto RowMatchesMinOrdinal(QObject* row, int minOrdinalInclusive) -> bool {
  if (minOrdinalInclusive <= 0) {
    return true;
  }

  const auto ordinal = RowOrdinal(row);
  return ordinal.has_value() && ordinal.value() >= minOrdinalInclusive;
}

auto BusyOrReadyRowCount(QObject* root, int minOrdinalInclusive = 0) -> int {
  int count = 0;
  for (QObject* row : CollectSearchRows(root)) {
    if (!RowMatchesMinOrdinal(row, minOrdinalInclusive)) {
      continue;
    }
    if (row->property("liveThumbLoading").toBool() || row->property("thumbReady").toBool()) {
      ++count;
    }
  }
  return count;
}

auto HasVisibleRowAtOrAfter(QObject* root, int minOrdinalInclusive) -> bool {
  for (QObject* row : CollectSearchRows(root)) {
    if (RowMatchesMinOrdinal(row, minOrdinalInclusive)) {
      return true;
    }
  }
  return false;
}

auto ReadyRowCount(QObject* root, int minOrdinalInclusive = 0) -> int {
  int readyCount = 0;
  for (QObject* row : CollectSearchRows(root)) {
    if (!RowMatchesMinOrdinal(row, minOrdinalInclusive)) {
      continue;
    }
    if (row->property("thumbReady").toBool()) {
      ++readyCount;
    }
  }
  return readyCount;
}

auto FirstReadyDataUrl(QObject* root, int minOrdinalInclusive = 0) -> QString {
  for (QObject* row : CollectSearchRows(root)) {
    if (!RowMatchesMinOrdinal(row, minOrdinalInclusive)) {
      continue;
    }
    if (!row->property("thumbReady").toBool()) {
      continue;
    }
    return row->property("liveThumbUrl").toString();
  }
  return {};
}

auto DecodeDataUrlImage(const QString& dataUrl) -> QImage {
  const int comma = dataUrl.indexOf(',');
  if (comma < 0) {
    return {};
  }

  const QByteArray encoded = dataUrl.mid(comma + 1).toLatin1();
  QImage           image;
  image.loadFromData(QByteArray::fromBase64(encoded));
  return image;
}

auto SearchPreviewReadySignalCount(const QSignalSpy& spy) -> int {
  int readySignals = 0;
  for (const auto& call : spy) {
    if (call.size() < 2) {
      continue;
    }
    if (!call.at(1).toString().isEmpty()) {
      ++readySignals;
    }
  }
  return readySignals;
}

auto VariantToJsonString(const QVariant& value) -> std::string {
  const QJsonValue jsonValue = QJsonValue::fromVariant(value);
  if (jsonValue.isObject()) {
    return QJsonDocument(jsonValue.toObject()).toJson(QJsonDocument::Compact).toStdString();
  }
  if (jsonValue.isArray()) {
    return QJsonDocument(jsonValue.toArray()).toJson(QJsonDocument::Compact).toStdString();
  }
  return value.toString().toStdString();
}

auto CurrentRowSummary(QObject* root) -> std::string {
  std::ostringstream summary;
  bool               first = true;
  for (QObject* row : CollectSearchRows(root)) {
    if (!first) {
      summary << " | ";
    }
    first = false;
    const auto ordinal = RowOrdinal(row);
    summary << "{ord=" << (ordinal.has_value() ? std::to_string(ordinal.value()) : "?")
            << ", title=" << row->property("title").toString().toStdString()
            << ", initUrl=" << !row->property("initialThumbUrl").toString().isEmpty()
            << ", initLoading=" << row->property("initialThumbLoading").toBool()
            << ", ready=" << row->property("thumbReady").toBool()
            << ", loading=" << row->property("liveThumbLoading").toBool()
            << ", liveUrl=" << !row->property("liveThumbUrl").toString().isEmpty() << "}";
  }
  return summary.str();
}

}  // namespace

TEST_F(GlobalSearchDialogQmlTests,
       ScrolledSearchResultsRenderPreviewThumbnailsAndReopenStillWorks) {
  auto* app = qobject_cast<QApplication*>(QCoreApplication::instance());
  ASSERT_NE(app, nullptr);

  QQuickStyle::setStyle(QStringLiteral("Material"));
  AppTheme::RegisterFonts();
  AppTheme::ApplyApplicationFont(*app);

  AlbumBackend backend;
  ASSERT_TRUE(CreateTestProject(backend));

  const auto searchDataset = MakeSearchDataset(temp_dir_, kSearchItemCount);
  ASSERT_EQ(searchDataset.size(), static_cast<size_t>(kSearchItemCount))
      << "Need at least 30 searchable RAW files to exercise page 2 thumbnail loading.";

  backend.StartImport(PathsToQStringList(searchDataset));
  WaitForImportFinished(backend);

  ASSERT_FALSE(backend.ImportRunning());
  ASSERT_GE(backend.ImportCompleted(), kSearchItemCount);
  ASSERT_GE(backend.ShownCount(), kSearchItemCount);

  auto* searchController = qobject_cast<SearchController*>(backend.SearchControllerObject());
  ASSERT_NE(searchController, nullptr);
  QSignalSpy previewSpy(searchController, &SearchController::SearchPreviewThumbnailUpdated);

  QQmlApplicationEngine engine;
  engine.addImportPath(QStringLiteral("qrc:/"));
  engine.rootContext()->setContextProperty(QStringLiteral("albumBackend"), &backend);
  engine.rootContext()->setContextProperty(QStringLiteral("appTheme"), &AppTheme::Instance());
  engine.rootContext()->setContextProperty(QStringLiteral("dialogSourceUrl"),
                                           GlobalSearchDialogFileUrl());
  engine.loadData(QByteArray{kHarnessQml},
                  QUrl(QStringLiteral("file:///GlobalSearchDialogTestHarness.qml")));

  ASSERT_FALSE(engine.rootObjects().empty()) << "QML harness failed to load.";

  QObject* windowRoot = engine.rootObjects().front();
  ASSERT_NE(windowRoot, nullptr);

  QObject* dialog = nullptr;
  ASSERT_TRUE(WaitUntil([&]() {
    dialog = qvariant_cast<QObject*>(windowRoot->property("dialog"));
    return dialog != nullptr;
  }, 10000))
      << "GlobalSearchDialog failed to instantiate from "
      << GlobalSearchDialogFileUrl().toString().toStdString();

  ASSERT_TRUE(QMetaObject::invokeMethod(dialog, "openFromCollection"));
  ASSERT_TRUE(WaitUntil([&]() { return dialog->property("visible").toBool(); }, 5000));

  QObject* searchField = nullptr;
  ASSERT_TRUE(WaitUntil([&]() {
    searchField = FindSearchField(windowRoot);
    return searchField != nullptr;
  }, 5000))
      << "Search field not found in GlobalSearchDialog object tree.";

  searchField->setProperty("text", QStringLiteral("scroll_search"));
  ASSERT_TRUE(QMetaObject::invokeMethod(dialog, "refreshPreview"));

  ASSERT_TRUE(WaitUntil([&]() {
    return dialog->property("searchTotal").toInt() >= kSearchItemCount &&
           dialog->property("results").toList().size() == kPageSize;
  }, 20000))
      << "Search preview did not return the expected first page. total="
      << dialog->property("searchTotal").toInt()
      << " rows=" << dialog->property("results").toList().size();

  QObject* resultList = nullptr;
  ASSERT_TRUE(WaitUntil([&]() {
    resultList = FindVisibleListView(windowRoot);
    return resultList != nullptr && resultList->property("contentHeight").toReal() > 0.0;
  }, 10000))
      << "Visible result ListView was not created.";

  QObject* resultContent = qvariant_cast<QObject*>(resultList->property("contentItem"));
  ASSERT_NE(resultContent, nullptr) << "ListView contentItem is missing.";

  ASSERT_TRUE(WaitUntil([&]() { return BusyOrReadyRowCount(resultContent, 0) > 0; }, 10000))
      << "Visible search rows never entered loading/ready state. signals="
      << previewSpy.count() << " readySignals="
      << SearchPreviewReadySignalCount(previewSpy)
      << " previewThumbs=" << VariantToJsonString(dialog->property("previewThumbs"))
      << " rows=" << CurrentRowSummary(resultContent);

  ASSERT_TRUE(WaitUntil([&]() { return ReadyRowCount(resultContent, 0) > 0; }, 60000))
      << "First page never rendered any preview thumbnails. rows="
      << CurrentRowSummary(resultContent);

  const QString firstPageUrl = FirstReadyDataUrl(resultContent, 0);
  ASSERT_FALSE(firstPageUrl.isEmpty());
  EXPECT_FALSE(DecodeDataUrlImage(firstPageUrl).isNull())
      << "First rendered preview did not decode as an image.";

  ASSERT_TRUE(QMetaObject::invokeMethod(dialog, "loadMorePreview"));

  ASSERT_TRUE(WaitUntil([&]() {
    return dialog->property("results").toList().size() >= kSearchItemCount &&
           !dialog->property("searchLoading").toBool();
  }, 30000))
      << "Scrolling near the end did not append the second search page. rows="
      << dialog->property("results").toList().size()
      << " loading=" << dialog->property("searchLoading").toBool();

  ASSERT_TRUE(QMetaObject::invokeMethod(resultList, "positionViewAtEnd"));

  ASSERT_TRUE(WaitUntil([&]() { return HasVisibleRowAtOrAfter(resultContent, kPageSize); }, 10000))
      << "Second-page delegates never entered the visible QML tree. rows="
      << CurrentRowSummary(resultContent);

  ASSERT_TRUE(WaitUntil([&]() { return BusyOrReadyRowCount(resultContent, kPageSize) > 0; },
                        10000))
      << "Second-page rows never entered loading/ready state after scroll. signals="
      << previewSpy.count() << " readySignals="
      << SearchPreviewReadySignalCount(previewSpy)
      << " previewThumbs=" << VariantToJsonString(dialog->property("previewThumbs"))
      << " rows=" << CurrentRowSummary(resultContent);

  ASSERT_TRUE(WaitUntil([&]() { return ReadyRowCount(resultContent, kPageSize) > 0; }, 60000))
      << "Second-page rows never rendered a preview thumbnail after scroll. rows="
      << CurrentRowSummary(resultContent);

  const QString secondPageUrl = FirstReadyDataUrl(resultContent, kPageSize);
  ASSERT_FALSE(secondPageUrl.isEmpty());
  EXPECT_FALSE(DecodeDataUrlImage(secondPageUrl).isNull())
      << "Second-page rendered preview did not decode as an image.";

  ASSERT_TRUE(QMetaObject::invokeMethod(dialog, "close"));
  ASSERT_TRUE(WaitUntil([&]() { return !dialog->property("visible").toBool(); }, 5000));

  const int readySignalCountBeforeReopen = SearchPreviewReadySignalCount(previewSpy);

  ASSERT_TRUE(QMetaObject::invokeMethod(dialog, "openFromCollection"));
  ASSERT_TRUE(WaitUntil([&]() { return dialog->property("visible").toBool(); }, 5000));

  searchField = FindSearchField(windowRoot);
  ASSERT_NE(searchField, nullptr);
  searchField->setProperty("text", QStringLiteral("scroll_search"));
  ASSERT_TRUE(QMetaObject::invokeMethod(dialog, "refreshPreview"));

  ASSERT_TRUE(WaitUntil([&]() {
    return dialog->property("searchTotal").toInt() >= kSearchItemCount &&
           dialog->property("results").toList().size() == kPageSize;
  }, 20000))
      << "Reopened dialog did not rebuild the first search page.";

  resultList = FindVisibleListView(windowRoot);
  ASSERT_NE(resultList, nullptr);
  resultContent = qvariant_cast<QObject*>(resultList->property("contentItem"));
  ASSERT_NE(resultContent, nullptr);

  ASSERT_TRUE(WaitUntil([&]() { return BusyOrReadyRowCount(resultContent, 0) > 0; }, 10000))
      << "Reopened dialog never entered loading/ready state. signals="
      << previewSpy.count() << " readySignals="
      << SearchPreviewReadySignalCount(previewSpy)
      << " previewThumbs=" << VariantToJsonString(dialog->property("previewThumbs"))
      << " rows=" << CurrentRowSummary(resultContent);

  ASSERT_TRUE(WaitUntil([&]() { return ReadyRowCount(resultContent, 0) > 0; }, 60000))
      << "Reopened dialog stayed on placeholder previews. rows="
      << CurrentRowSummary(resultContent);

  const QString reopenedUrl = FirstReadyDataUrl(resultContent, 0);
  ASSERT_FALSE(reopenedUrl.isEmpty());
  EXPECT_FALSE(DecodeDataUrlImage(reopenedUrl).isNull())
      << "Reopened dialog produced a non-decodable preview image.";

  EXPECT_GT(SearchPreviewReadySignalCount(previewSpy), readySignalCountBeforeReopen)
      << "Reopening the dialog did not emit any new ready preview updates.";
}

}  // namespace alcedo::ui::test

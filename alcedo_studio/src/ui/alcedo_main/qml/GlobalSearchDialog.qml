import QtQuick
import QtQuick.Controls
import QtQuick.Controls.Material
import QtQuick.Layouts

Dialog {
    id: dialog

    property var backend
    property var theme
    property var recommendations: []
    property var results: []
    property var previewThumbs: ({})
    property string lastQuery: ""

    modal: true
    focus: true
    width: Math.min(760, Math.max(520, parent ? parent.width * 0.58 : 760))
    height: Math.min(620, Math.max(460, parent ? parent.height * 0.72 : 620))
    x: parent ? Math.round((parent.width - width) / 2) : 0
    y: parent ? Math.round((parent.height - height) / 2) : 0
    padding: 0

    function withAlpha(colorValue, alphaValue) {
        return Qt.rgba(colorValue.r, colorValue.g, colorValue.b, alphaValue)
    }

    function openFromCollection() {
        if (backend) {
            backend.CancelSearchPreviewThumbnails()
        }
        lastQuery = ""
        searchField.text = ""
        results = []
        previewThumbs = ({})
        recommendations = backend ? backend.SearchRecommendations(12) : []
        open()
        Qt.callLater(function() { searchField.forceActiveFocus() })
    }

    function refreshPreview() {
        if (!backend) {
            return
        }
        const query = searchField.text.trim()
        lastQuery = query
        if (query.length === 0) {
            results = []
            previewThumbs = ({})
            recommendations = backend.SearchRecommendations(12)
            return
        }
        const next = backend.SearchPreview(query, 24)
        results = next
    }

    function requestPreviewThumbnails() {
        if (!backend) {
            return
        }
        const query = searchField.text.trim()
        if (query.length === 0 || query !== lastQuery) {
            return
        }
        backend.CancelSearchPreviewThumbnails()
        previewThumbs = ({})
        for (let i = 0; i < results.length; ++i) {
            const row = results[i]
            if (row && Number(row.elementId) > 0 && Number(row.imageId) > 0) {
                backend.RequestSearchPreviewThumbnail(Number(row.elementId),
                                                      Number(row.imageId), 192)
            }
        }
    }

    function applyBroadSearch() {
        if (!backend) {
            return
        }
        backend.ApplyFuzzySearch(searchField.text)
        close()
    }

    function applyRecommendation(row) {
        if (!backend || !row) {
            return
        }
        backend.ApplyFuzzySearch(row.query ? String(row.query) : String(row.label))
        close()
    }

    function applyExact(row) {
        if (!backend || !row) {
            return
        }
        backend.ApplyExactSearch(Number(row.elementId))
        close()
    }

    onOpened: recommendations = backend ? backend.SearchRecommendations(12) : []
    onClosed: {
        previewTimer.stop()
        thumbnailTimer.stop()
        if (backend) {
            backend.CancelSearchPreviewThumbnails()
        }
    }

    Connections {
        target: backend
        ignoreUnknownSignals: true

        function onSearchPreviewThumbnailUpdated(elementId, dataUrl, loading, missingSource, errorText) {
            const next = Object.assign({}, dialog.previewThumbs)
            next[String(Number(elementId))] = {
                url: dataUrl ? String(dataUrl) : "",
                loading: loading === true,
                missingSource: missingSource === true,
                errorText: errorText ? String(errorText) : ""
            }
            dialog.previewThumbs = next
        }
    }

    Timer {
        id: previewTimer
        interval: 140
        repeat: false
        onTriggered: dialog.refreshPreview()
    }

    Timer {
        id: thumbnailTimer
        interval: 500
        repeat: false
        onTriggered: dialog.requestPreviewThumbnails()
    }

    background: Rectangle {
        radius: 12
        color: theme ? theme.colBgPanel : "#202020"
        border.width: 1
        border.color: theme ? dialog.withAlpha(theme.colText, 0.12) : "#3a3a3a"
    }

    contentItem: ColumnLayout {
        anchors.fill: parent
        anchors.margins: 18
        spacing: 14

        RowLayout {
            Layout.fillWidth: true
            Layout.preferredHeight: 44
            spacing: 10

            Rectangle {
                Layout.preferredWidth: 34
                Layout.preferredHeight: 34
                radius: 8
                color: theme ? dialog.withAlpha(theme.colHover, 0.32) : "#303030"

                Image {
                    anchors.centerIn: parent
                    width: 17
                    height: 17
                    source: "qrc:/panel_icons/search.svg"
                    sourceSize.width: 17
                    sourceSize.height: 17
                    fillMode: Image.PreserveAspectFit
                }
            }

            TextField {
                id: searchField
                Layout.fillWidth: true
                Layout.preferredHeight: 44
                placeholderText: qsTr("Search this collection")
                selectByMouse: true
                color: theme ? theme.colText : "#f2f2f2"
                font.family: appTheme.dataFontFamily
                font.pixelSize: 17
                Material.foreground: theme ? theme.colText : "#f2f2f2"
                Material.accent: theme ? theme.colAccentSecondary : "#79a"
                background: Rectangle {
                    radius: 9
                    color: theme ? dialog.withAlpha(theme.colBgBase, 0.72) : "#181818"
                    border.width: 1
                    border.color: searchField.activeFocus && theme
                                  ? dialog.withAlpha(theme.colAccentSecondary, 0.62)
                                  : (theme ? dialog.withAlpha(theme.colText, 0.12) : "#444")
                }
                onTextChanged: {
                    if (backend) {
                        backend.CancelSearchPreviewThumbnails()
                    }
                    previewThumbs = ({})
                    previewTimer.restart()
                    thumbnailTimer.restart()
                }
                onAccepted: dialog.applyBroadSearch()
                Keys.onEscapePressed: dialog.close()
            }

            Button {
                Layout.preferredWidth: 34
                Layout.preferredHeight: 34
                text: "x"
                font.pixelSize: 20
                Material.foreground: theme ? theme.colText : "#f2f2f2"
                onClicked: dialog.close()
                background: Rectangle {
                    radius: 8
                    color: parent.hovered && theme ? dialog.withAlpha(theme.colHover, 0.45)
                                                   : "transparent"
                }
            }
        }

        Label {
            Layout.fillWidth: true
            text: searchField.text.trim().length === 0 ? qsTr("Recommended")
                                                       : qsTr("Results")
            color: theme ? dialog.withAlpha(theme.colText, 0.58) : "#9a9a9a"
            font.pixelSize: 11
            font.letterSpacing: 1.1
            font.weight: 700
        }

        Flow {
            Layout.fillWidth: true
            visible: searchField.text.trim().length === 0
            spacing: 8

            Repeater {
                model: dialog.recommendations

                delegate: Button {
                    required property var modelData
                    text: qsTr("%1  %2").arg(modelData.categoryLabel).arg(modelData.label)
                    height: 34
                    leftPadding: 12
                    rightPadding: 12
                    Material.foreground: theme ? theme.colText : "#f2f2f2"
                    onClicked: dialog.applyRecommendation(modelData)
                    background: Rectangle {
                        radius: 8
                        color: parent.hovered && theme ? dialog.withAlpha(theme.colHover, 0.44)
                                                       : (theme ? dialog.withAlpha(theme.colHover, 0.24)
                                                                : "#303030")
                        border.width: 1
                        border.color: theme ? dialog.withAlpha(theme.colText, 0.08) : "#444"
                    }
                }
            }
        }

        ListView {
            id: resultList
            Layout.fillWidth: true
            Layout.fillHeight: true
            visible: searchField.text.trim().length > 0
            clip: true
            spacing: 8
            model: dialog.results

            ScrollIndicator.vertical: ScrollIndicator {}

            delegate: Rectangle {
                required property var modelData

                width: resultList.width
                height: 74
                radius: 9
                color: rowMouse.containsMouse && theme ? dialog.withAlpha(theme.colHover, 0.28)
                                                       : "transparent"
                border.width: 1
                border.color: theme ? dialog.withAlpha(theme.colText, 0.07) : "#383838"

                readonly property var thumbState: dialog.previewThumbs[String(Number(modelData.elementId))] || ({})
                readonly property bool thumbReady: thumbState.url && thumbState.url.length > 0

                RowLayout {
                    anchors.fill: parent
                    anchors.margins: 8
                    spacing: 12

                    Rectangle {
                        Layout.preferredWidth: 76
                        Layout.preferredHeight: 54
                        radius: 7
                        color: theme ? dialog.withAlpha(theme.colBgBase, 0.74) : "#161616"
                        border.width: 1
                        border.color: theme ? dialog.withAlpha(theme.colText, 0.08) : "#333"
                        clip: true

                        BusyIndicator {
                            anchors.centerIn: parent
                            width: 22
                            height: 22
                            running: visible
                            visible: thumbState.loading === true
                        }

                        Image {
                            anchors.fill: parent
                            anchors.margins: 2
                            source: thumbReady ? thumbState.url : ""
                            visible: thumbReady
                            fillMode: Image.PreserveAspectFit
                            asynchronous: true
                        }
                    }

                    ColumnLayout {
                        Layout.fillWidth: true
                        spacing: 4

                        Label {
                            Layout.fillWidth: true
                            text: modelData.fileName ? modelData.fileName : qsTr("(unnamed)")
                            color: theme ? theme.colText : "#f2f2f2"
                            font.family: appTheme.dataFontFamily
                            font.pixelSize: 14
                            font.weight: 650
                            elide: Text.ElideRight
                        }

                        Label {
                            Layout.fillWidth: true
                            text: qsTr("%1 | %2").arg(modelData.cameraModel).arg(modelData.captureDate)
                            color: theme ? dialog.withAlpha(theme.colText, 0.58) : "#9a9a9a"
                            font.family: appTheme.dataFontFamily
                            font.pixelSize: 11
                            elide: Text.ElideRight
                        }

                        Label {
                            Layout.fillWidth: true
                            text: qsTr("Rating %1/5").arg(Number(modelData.rating))
                            color: theme ? dialog.withAlpha(theme.colText, 0.48) : "#858585"
                            font.family: appTheme.dataFontFamily
                            font.pixelSize: 10
                            elide: Text.ElideRight
                        }
                    }
                }

                MouseArea {
                    id: rowMouse
                    anchors.fill: parent
                    hoverEnabled: true
                    cursorShape: Qt.PointingHandCursor
                    onClicked: dialog.applyExact(modelData)
                }
            }

            Label {
                anchors.centerIn: parent
                visible: resultList.count === 0 && searchField.text.trim().length > 0
                text: qsTr("No matches")
                color: theme ? dialog.withAlpha(theme.colText, 0.52) : "#999"
                font.pixelSize: 13
            }
        }

        RowLayout {
            Layout.fillWidth: true
            Layout.preferredHeight: 38
            spacing: 10

            Label {
                Layout.fillWidth: true
                text: backend && backend.activeSearchQuery.length > 0
                      ? qsTr("Active: %1").arg(backend.activeSearchQuery)
                      : ""
                color: theme ? dialog.withAlpha(theme.colText, 0.48) : "#858585"
                font.pixelSize: 11
                elide: Text.ElideRight
            }

            Button {
                text: qsTr("Clear")
                enabled: backend && backend.activeSearchQuery.length > 0
                Material.foreground: theme ? theme.colText : "#f2f2f2"
                onClicked: {
                    backend.ClearFuzzySearch()
                    dialog.close()
                }
                background: Rectangle {
                    radius: 8
                    color: parent.enabled && parent.hovered && theme
                           ? dialog.withAlpha(theme.colHover, 0.42)
                           : "transparent"
                    border.width: parent.enabled ? 1 : 0
                    border.color: theme ? dialog.withAlpha(theme.colText, 0.10) : "#444"
                }
            }

            Button {
                text: qsTr("Apply")
                enabled: searchField.text.trim().length > 0
                Material.foreground: theme ? theme.colText : "#f2f2f2"
                onClicked: dialog.applyBroadSearch()
                background: Rectangle {
                    radius: 8
                    color: parent.enabled && theme ? dialog.withAlpha(theme.colAccentSecondary, 0.62)
                                                   : dialog.withAlpha(theme.colHover, 0.16)
                }
            }
        }
    }
}

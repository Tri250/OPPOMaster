import QtQuick
import QtQuick.Controls
import QtQuick.Controls.Material
import QtQuick.Layouts
import QtQuick.Effects

Dialog {
    id: dialog
    font.family: appTheme.uiFontFamily

    property var backend
    property var theme
    property var recommendations: []
    property var results: []
    property var previewThumbs: ({})
    property string lastQuery: ""
    property Item blurSource: null
    property real cornerRadius: 0

    property color panelColor: theme ? theme.colBgPanel : "#1C1C1D"
    property color canvasColor: theme ? theme.colBgCanvas : "#111214"
    property color textColor: theme ? theme.colText : "#F5F1EA"
    property color mutedTextColor: theme ? theme.colTextMuted : "#AAA59D"
    property color accentColor: theme ? theme.colAccentSecondary : "#6D93B7"
    property color hoverColor: theme ? theme.colHover : Qt.rgba(1, 1, 1, 0.07)
    property color dividerColor: theme ? theme.colDivider : Qt.rgba(1, 1, 1, 0.08)
    property color overlayColor: theme ? theme.colOverlay : Qt.rgba(11 / 255, 12 / 255, 14 / 255, 0.60)
    property string headlineFontFamily: appTheme.headlineFontFamily
    readonly property string dataFontFamily: appTheme.dataFontFamily

    parent: Overlay.overlay
    modal: true
    focus: visible
    closePolicy: Popup.CloseOnEscape | Popup.CloseOnPressOutside
    width: parent ? parent.width : 0
    height: parent ? parent.height : 0
    x: 0
    y: 0
    padding: 0

    function withAlpha(colorValue, alphaValue) {
        return Qt.rgba(colorValue.r, colorValue.g, colorValue.b, alphaValue)
    }

    function suggestionIconSource(category) {
        const key = String(category || "")
        if (key === "camera") {
            return "qrc:/panel_icons/camera.svg"
        }
        if (key === "date") {
            return "qrc:/panel_icons/calendar.svg"
        }
        if (key === "lens") {
            return "qrc:/panel_icons/aperture.svg"
        }
        return "qrc:/panel_icons/search.svg"
    }

    function resetPreviewState() {
        previewTimer.stop()
        thumbnailTimer.stop()
        if (backend) {
            backend.CancelSearchPreviewThumbnails()
        }
        previewThumbs = ({})
    }

    function openFromCollection() {
        resetPreviewState()
        lastQuery = ""
        searchField.text = ""
        results = []
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
        backend.CancelSearchPreviewThumbnails()
        previewThumbs = ({})
        if (query.length === 0) {
            results = []
            recommendations = backend.SearchRecommendations(12)
            return
        }
        results = backend.SearchPreview(query, 24)
        thumbnailTimer.restart()
    }

    function requestPreviewThumbnails() {
        if (!backend) {
            return
        }
        const query = searchField.text.trim()
        if (query.length === 0 || query !== lastQuery) {
            return
        }
        for (let i = 0; i < results.length; ++i) {
            const row = results[i]
            if (row && Number(row.elementId) > 0 && Number(row.imageId) > 0) {
                backend.RequestSearchPreviewThumbnail(Number(row.elementId),
                                                      Number(row.imageId), 256)
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
    onClosed: resetPreviewState()

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
        interval: 420
        repeat: false
        onTriggered: dialog.requestPreviewThumbnails()
    }

    Overlay.modal: Item {
        anchors.fill: parent

        Rectangle {
            id: backdropMask
            anchors.fill: parent
            radius: dialog.cornerRadius
            color: "white"
            visible: false
            layer.enabled: true
            layer.smooth: true
        }

        Item {
            anchors.fill: parent
            layer.enabled: true
            layer.smooth: true
            layer.effect: MultiEffect {
                maskEnabled: dialog.cornerRadius > 0
                maskSource: backdropMask
            }

            MultiEffect {
                anchors.fill: parent
                source: dialog.blurSource
                blurEnabled: dialog.blurSource !== null
                blur: 0.68
                blurMax: 72
                saturation: -0.22
                brightness: -0.08
            }

            Rectangle {
                anchors.fill: parent
                color: dialog.overlayColor
                opacity: dialog.blurSource !== null ? 0.66 : 0.90
            }
        }

        MouseArea {
            anchors.fill: parent
            hoverEnabled: true
        }
    }

    background: Item {}

    contentItem: Item {
        implicitWidth: dialog.width
        implicitHeight: dialog.height

        Rectangle {
            id: shell
            anchors.horizontalCenter: parent.horizontalCenter
            anchors.top: parent.top
            anchors.topMargin: Math.max(28, Math.round(parent.height * 0.075))
            width: Math.min(parent.width - 56, 1120)
            height: Math.min(parent.height - 72, 710)
            radius: 16
            color: Qt.rgba(dialog.panelColor.r, dialog.panelColor.g, dialog.panelColor.b, 0.96)
            border.width: 1
            border.color: dialog.withAlpha(dialog.textColor, 0.09)
            clip: true

            ColumnLayout {
                anchors.fill: parent
                spacing: 0

                Item {
                    Layout.fillWidth: true
                    Layout.preferredHeight: 108

                    RowLayout {
                        anchors.fill: parent
                        anchors.leftMargin: 28
                        anchors.rightMargin: 24
                        spacing: 18

                        Image {
                            Layout.preferredWidth: 26
                            Layout.preferredHeight: 26
                            source: "qrc:/panel_icons/search.svg"
                            sourceSize.width: 26
                            sourceSize.height: 26
                            fillMode: Image.PreserveAspectFit
                            opacity: 0.76
                        }

                        Rectangle {
                            Layout.fillWidth: true
                            Layout.preferredHeight: 58
                            radius: 10
                            color: dialog.withAlpha(dialog.canvasColor, 0.46)
                            border.width: 1
                            border.color: searchField.activeFocus
                                          ? dialog.withAlpha(dialog.accentColor, 0.48)
                                          : dialog.withAlpha(dialog.textColor, 0.08)

                            Text {
                                anchors.left: parent.left
                                anchors.right: parent.right
                                anchors.verticalCenter: parent.verticalCenter
                                anchors.leftMargin: 16
                                anchors.rightMargin: 16
                                visible: searchField.text.length === 0
                                text: qsTr("Search photos, cameras, lenses, dates...")
                                color: dialog.withAlpha(dialog.textColor, 0.42)
                                font.family: dialog.dataFontFamily
                                font.pixelSize: 17
                                font.weight: 500
                                elide: Text.ElideRight
                            }

                            TextInput {
                                id: searchField
                                anchors.left: parent.left
                                anchors.right: parent.right
                                anchors.verticalCenter: parent.verticalCenter
                                anchors.leftMargin: 16
                                anchors.rightMargin: 16
                                height: Math.max(30, implicitHeight)
                                selectByMouse: true
                                clip: true
                                color: dialog.textColor
                                selectionColor: dialog.withAlpha(dialog.accentColor, 0.36)
                                selectedTextColor: dialog.textColor
                                font.family: dialog.dataFontFamily
                                font.pixelSize: 17
                                font.weight: 600
                                verticalAlignment: TextInput.AlignVCenter
                                onTextChanged: previewTimer.restart()
                                onAccepted: dialog.applyBroadSearch()
                                Keys.onEscapePressed: dialog.close()
                            }
                        }

                        ToolButton {
                            Layout.preferredWidth: 40
                            Layout.preferredHeight: 40
                            text: "\u00d7"
                            font.pixelSize: 28
                            font.weight: 300
                            Material.foreground: dialog.withAlpha(dialog.textColor, 0.78)
                            onClicked: dialog.close()
                            background: Rectangle {
                                radius: 8
                                color: parent.down
                                       ? dialog.withAlpha(dialog.textColor, 0.08)
                                       : (parent.hovered ? dialog.hoverColor : "transparent")
                            }
                        }
                    }
                }

                Rectangle {
                    Layout.fillWidth: true
                    Layout.preferredHeight: 1
                    color: dialog.dividerColor
                }

                ColumnLayout {
                    Layout.fillWidth: true
                    Layout.fillHeight: true
                    Layout.leftMargin: 26
                    Layout.rightMargin: 26
                    Layout.topMargin: 20
                    Layout.bottomMargin: 18
                    spacing: 16

                    RowLayout {
                        Layout.fillWidth: true
                        Layout.preferredHeight: 32
                        spacing: 14

                        Label {
                            text: searchField.text.trim().length === 0 ? qsTr("Suggestion")
                                                                       : qsTr("Results")
                            color: dialog.withAlpha(dialog.textColor, 0.68)
                            font.pixelSize: 14
                            font.weight: 760
                        }

                        Label {
                            visible: searchField.text.trim().length > 0
                            text: qsTr("%1 matches").arg(dialog.results.length)
                            color: dialog.withAlpha(dialog.textColor, 0.42)
                            font.family: dialog.dataFontFamily
                            font.pixelSize: 12
                            font.weight: 600
                        }

                        Item {
                            Layout.fillWidth: true
                        }

                        Label {
                            visible: backend !== null && backend !== undefined
                                     && backend.activeSearchQuery.length > 0
                            text: qsTr("Active: %1").arg(backend ? backend.activeSearchQuery : "")
                            color: dialog.withAlpha(dialog.textColor, 0.46)
                            font.pixelSize: 12
                            elide: Text.ElideRight
                            Layout.maximumWidth: 320
                        }
                    }

                    ListView {
                        id: recommendationList
                        Layout.fillWidth: true
                        Layout.fillHeight: true
                        visible: searchField.text.trim().length === 0
                        clip: true
                        spacing: 0
                        model: dialog.recommendations

                        delegate: SearchRow {
                            required property var modelData
                            width: recommendationList.width
                            title: modelData.label ? String(modelData.label) : ""
                            subtitle: modelData.categoryLabel ? String(modelData.categoryLabel) : ""
                            countText: modelData.count ? String(modelData.count) : ""
                            iconSource: dialog.suggestionIconSource(modelData.category)
                            framedIcon: false
                            rowHeight: 60
                            titlePixelSize: 13
                            titleWeight: 620
                            subtitlePixelSize: 11
                            thumbnailWidth: 42
                            thumbnailHeight: 34
                            iconSize: 17
                            onActivated: dialog.applyRecommendation(modelData)
                        }

                        Label {
                            anchors.centerIn: parent
                            visible: recommendationList.count === 0
                            text: qsTr("No recent suggestions")
                            color: dialog.withAlpha(dialog.textColor, 0.44)
                            font.pixelSize: 13
                        }
                    }

                    ListView {
                        id: resultList
                        Layout.fillWidth: true
                        Layout.fillHeight: true
                        visible: searchField.text.trim().length > 0
                        clip: true
                        spacing: 0
                        model: dialog.results

                        ScrollIndicator.vertical: ScrollIndicator {}

                        delegate: SearchRow {
                            required property var modelData

                            width: resultList.width
                            elementId: Number(modelData.elementId)
                            title: modelData.fileName ? String(modelData.fileName) : qsTr("(unnamed)")
                            subtitle: qsTr("%1  |  %2").arg(modelData.cameraModel).arg(modelData.captureDate)
                            detailText: Number(modelData.rating) > 0
                                        ? qsTr("Rating %1/5").arg(Number(modelData.rating))
                                        : (modelData.lens ? String(modelData.lens) : "")
                            iconSource: "qrc:/panel_icons/image.svg"
                            initialThumbUrl: {
                                const state = dialog.previewThumbs[String(Number(modelData.elementId))] || ({})
                                if (state.url) {
                                    return String(state.url)
                                }
                                return modelData.thumbUrl ? String(modelData.thumbUrl) : ""
                            }
                            initialThumbLoading: {
                                const state = dialog.previewThumbs[String(Number(modelData.elementId))] || ({})
                                return state.loading === true || modelData.thumbLoading === true
                            }
                            initialThumbMissingSource: {
                                const state = dialog.previewThumbs[String(Number(modelData.elementId))] || ({})
                                return state.missingSource === true || modelData.thumbMissingSource === true
                            }
                            initialThumbErrorText: {
                                const state = dialog.previewThumbs[String(Number(modelData.elementId))] || ({})
                                if (state.errorText) {
                                    return String(state.errorText)
                                }
                                return modelData.thumbErrorText ? String(modelData.thumbErrorText) : ""
                            }
                            onActivated: dialog.applyExact(modelData)
                        }

                        Label {
                            anchors.centerIn: parent
                            visible: resultList.count === 0 && searchField.text.trim().length > 0
                            text: qsTr("No matches")
                            color: dialog.withAlpha(dialog.textColor, 0.48)
                            font.pixelSize: 13
                        }
                    }
                }

            }
        }
    }

    component SearchRow: Rectangle {
        id: row

        property string title: ""
        property string subtitle: ""
        property string detailText: ""
        property string countText: ""
        property string iconSource: ""
        property string iconText: ""
        property bool framedIcon: true
        property int rowHeight: 82
        property int titlePixelSize: 17
        property int titleWeight: 690
        property int subtitlePixelSize: 12
        property int thumbnailWidth: 64
        property int thumbnailHeight: 48
        property int iconSize: 21
        property int elementId: 0
        property string initialThumbUrl: ""
        property bool initialThumbLoading: false
        property bool initialThumbMissingSource: false
        property string initialThumbErrorText: ""
        property string liveThumbUrl: initialThumbUrl
        property bool liveThumbLoading: initialThumbLoading
        property bool liveThumbMissingSource: initialThumbMissingSource
        property string liveThumbErrorText: initialThumbErrorText
        readonly property bool thumbReady: liveThumbUrl.length > 0
        readonly property bool thumbProblem: !thumbReady && !liveThumbLoading
                                             && (liveThumbMissingSource || liveThumbErrorText.length > 0)
        readonly property string thumbProblemText: liveThumbErrorText.length > 0
                                                   ? liveThumbErrorText
                                                   : qsTr("Source file is unavailable")

        signal activated()

        onInitialThumbUrlChanged: liveThumbUrl = initialThumbUrl
        onInitialThumbLoadingChanged: liveThumbLoading = initialThumbLoading
        onInitialThumbMissingSourceChanged: liveThumbMissingSource = initialThumbMissingSource
        onInitialThumbErrorTextChanged: liveThumbErrorText = initialThumbErrorText

        height: rowHeight
        radius: 9
        color: rowMouse.pressed
               ? dialog.withAlpha(dialog.textColor, 0.075)
               : (rowMouse.containsMouse ? dialog.hoverColor : "transparent")

        Connections {
            target: dialog.backend
            ignoreUnknownSignals: true

            function onSearchPreviewThumbnailUpdated(updatedElementId, dataUrl, loading, missingSource, errorText) {
                if (Number(updatedElementId) !== row.elementId) {
                    return
                }
                row.liveThumbUrl = dataUrl ? String(dataUrl) : ""
                row.liveThumbLoading = loading === true
                row.liveThumbMissingSource = missingSource === true
                row.liveThumbErrorText = errorText ? String(errorText) : ""
            }
        }

        Rectangle {
            anchors.left: parent.left
            anchors.right: parent.right
            anchors.bottom: parent.bottom
            height: 1
            color: dialog.withAlpha(dialog.textColor, 0.07)
        }

        RowLayout {
            anchors.fill: parent
            anchors.leftMargin: 2
            anchors.rightMargin: 12
            spacing: 14

            Item {
                Layout.preferredWidth: row.thumbnailWidth
                Layout.preferredHeight: row.thumbnailHeight

                Rectangle {
                    anchors.fill: parent
                    visible: row.framedIcon
                    radius: 8
                    color: row.thumbReady ? "transparent" : dialog.withAlpha(dialog.canvasColor, 0.72)
                    border.width: row.thumbReady ? 0 : 1
                    border.color: dialog.withAlpha(dialog.textColor, 0.08)
                }

                BusyIndicator {
                    anchors.centerIn: parent
                    width: 20
                    height: 20
                    running: visible
                    visible: row.framedIcon && row.liveThumbLoading
                }

                Image {
                    id: thumbImage
                    anchors.fill: parent
                    source: row.liveThumbUrl
                    visible: false
                    fillMode: Image.PreserveAspectCrop
                    asynchronous: true

                    onStatusChanged: {
                        if (status === Image.Error && row.liveThumbUrl.length > 0
                                && row.liveThumbErrorText.length === 0) {
                            row.liveThumbErrorText = qsTr("Preview image failed to load")
                        }
                    }
                }

                Rectangle {
                    id: thumbMask
                    anchors.fill: thumbImage
                    radius: 7
                    visible: false
                    layer.enabled: true
                }

                MultiEffect {
                    anchors.fill: thumbImage
                    source: thumbImage
                    maskEnabled: true
                    maskSource: thumbMask
                    visible: row.framedIcon && row.thumbReady
                }

                Image {
                    anchors.centerIn: parent
                    width: row.iconSize
                    height: row.iconSize
                    source: row.iconSource
                    visible: !row.thumbReady && !row.liveThumbLoading && !row.thumbProblem
                             && row.iconSource.length > 0
                    sourceSize.width: row.iconSize
                    sourceSize.height: row.iconSize
                    opacity: row.framedIcon ? 0.58 : 0.72
                    fillMode: Image.PreserveAspectFit
                }

                Label {
                    anchors.centerIn: parent
                    visible: !row.thumbReady && !row.liveThumbLoading && !row.thumbProblem
                             && row.iconSource.length === 0 && row.iconText.length > 0
                    text: row.iconText
                    color: dialog.withAlpha(dialog.textColor, 0.62)
                    font.family: dialog.dataFontFamily
                    font.pixelSize: 11
                    font.weight: 800
                }

                Label {
                    anchors.centerIn: parent
                    visible: row.thumbProblem
                    text: "!"
                    color: dialog.accentColor
                    font.family: dialog.dataFontFamily
                    font.pixelSize: 22
                    font.weight: 800
                }

                HoverHandler {
                    id: thumbHover
                }
                ToolTip.visible: row.thumbProblem && thumbHover.hovered
                ToolTip.text: row.thumbProblemText
                ToolTip.delay: 160
            }

            ColumnLayout {
                Layout.fillWidth: true
                spacing: 5

                Label {
                    Layout.fillWidth: true
                    text: row.title
                    color: dialog.withAlpha(dialog.textColor, 0.88)
                    font.family: dialog.dataFontFamily
                    font.pixelSize: row.titlePixelSize
                    font.weight: row.titleWeight
                    elide: Text.ElideRight
                }

                Label {
                    Layout.fillWidth: true
                    text: row.detailText.length > 0
                          ? qsTr("%1  -  %2").arg(row.subtitle).arg(row.detailText)
                          : row.subtitle
                    visible: text.length > 0
                    color: dialog.withAlpha(dialog.textColor, 0.50)
                    font.family: dialog.dataFontFamily
                    font.pixelSize: row.subtitlePixelSize
                    font.weight: 560
                    elide: Text.ElideRight
                }
            }

            Label {
                visible: row.countText.length > 0
                text: row.countText
                color: dialog.withAlpha(dialog.textColor, 0.42)
                font.family: dialog.dataFontFamily
                font.pixelSize: 12
                font.weight: 700
            }
        }

        MouseArea {
            id: rowMouse
            anchors.fill: parent
            hoverEnabled: true
            cursorShape: Qt.PointingHandCursor
            onClicked: row.activated()
        }
    }
}

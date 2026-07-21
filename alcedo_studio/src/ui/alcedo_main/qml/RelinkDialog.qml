import QtQuick
import QtQuick.Controls
import QtQuick.Layouts
import QtQuick.Dialogs

Dialog {
    id: root
    font.family: appTheme.uiFontFamily
    modal: true
    focus: true
    width: Math.min(parent ? parent.width - 40 : 620, 620)
    height: Math.min(parent ? parent.height - 40 : 520, 520)
    x: parent ? Math.round((parent.width - width) / 2) : 0
    y: parent ? Math.round((parent.height - height) / 2) : 0
    padding: 0
    closePolicy: Popup.CloseOnEscape

    property var relinkService: null

    readonly property color panelColor: appTheme.bgPanelColor
    readonly property color cardColor: appTheme.bgBaseColor
    readonly property color textColor: appTheme.textColor
    readonly property color mutedTextColor: appTheme.textMutedColor
    readonly property color accentColor: appTheme.accentColor
    readonly property color dangerColor: appTheme.dangerColor
    readonly property color successColor: "#2E7D32"
    readonly property string dataFontFamily: appTheme.dataFontFamily
    readonly property string headlineFontFamily: appTheme.headlineFontFamily

    property var missingFiles: []

    // Dialog enter/exit animation
    property real animScale: 1.0
    property real animOpacity: 1.0

    onAboutToShow: {
        animScale = 0.96
        animOpacity = 0
        relinkEnterAnim.start()
    }

    Behavior on animScale { NumberAnimation { duration: 220; easing.type: Easing.OutCubic } }
    Behavior on animOpacity { NumberAnimation { duration: 220; easing.type: Easing.OutCubic } }

    ParallelAnimation {
        id: relinkEnterAnim
        NumberAnimation { target: root; property: "animScale"; to: 1.0; duration: 220; easing.type: Easing.OutCubic }
        NumberAnimation { target: root; property: "animOpacity"; to: 1.0; duration: 220; easing.type: Easing.OutCubic }
    }

    ParallelAnimation {
        id: relinkExitAnim
        NumberAnimation { target: root; property: "animScale"; to: 0.97; duration: 160; easing.type: Easing.InCubic }
        NumberAnimation { target: root; property: "animOpacity"; to: 0; duration: 160; easing.type: Easing.InCubic }
        onFinished: root.close()
    }
    property var relinkStatus: ({})  // path -> "found" | "not_found" | "searching"
    property bool autoSearching: false

    signal allFilesRelinked()

    function refreshMissingFiles() {
        if (relinkService) {
            root.missingFiles = relinkService.GetMissingFiles()
            var statusMap = {}
            for (var i = 0; i < root.missingFiles.length; i++) {
                var f = root.missingFiles[i]
                if (relinkService.IsFileRelinked(f)) {
                    statusMap[f] = "found"
                } else {
                    statusMap[f] = "not_found"
                }
            }
            root.relinkStatus = statusMap
        }
    }

    function checkAllRelinked() {
        for (var i = 0; i < root.missingFiles.length; i++) {
            if (root.relinkStatus[root.missingFiles[i]] !== "found") {
                return false
            }
        }
        return true
    }

    onOpened: refreshMissingFiles()

    background: Rectangle {
        radius: 14
        color: root.panelColor
        border.width: 1
        border.color: Qt.rgba(1, 1, 1, 0.06)
        opacity: root.animOpacity
        transform: Scale {
            origin.x: root.width / 2
            origin.y: root.height / 2
            xScale: root.animScale
            yScale: root.animScale
        }
    }

    contentItem: ColumnLayout {
        anchors.fill: parent
        spacing: 0

        // Header
        RowLayout {
            Layout.fillWidth: true
            Layout.topMargin: 22
            Layout.leftMargin: 24
            Layout.rightMargin: 22
            Layout.bottomMargin: 14
            spacing: 12

            Label {
                text: qsTr("Relink Missing Files")
                font.family: root.headlineFontFamily
                font.pixelSize: 20
                font.weight: Font.Medium
                color: root.textColor
            }

            Item { Layout.fillWidth: true }

            Label {
                visible: root.missingFiles.length > 0
                text: qsTr("%1 file(s) missing").arg(root.missingFiles.length)
                color: root.dangerColor
                font.pixelSize: 12
                font.weight: Font.DemiBold
            }

            Button {
                text: "✕"
                flat: true
                onClicked: root.close()
                implicitWidth: 28
                implicitHeight: 28
                font.pixelSize: 13
                opacity: hovered ? 1.0 : 0.55
                Material.foreground: root.textColor
            }
        }

        Rectangle {
            Layout.fillWidth: true
            Layout.preferredHeight: 1
            color: Qt.rgba(1, 1, 1, 0.08)
        }

        // Body
        ColumnLayout {
            Layout.fillWidth: true
            Layout.fillHeight: true
            Layout.margins: 24
            spacing: 14

            // Toolbar
            RowLayout {
                Layout.fillWidth: true
                spacing: 10

                Button {
                    text: qsTr("Auto Search")
                    highlighted: true
                    onClicked: {
                        if (root.relinkService) {
                            root.autoSearching = true
                            root.relinkService.AutoSearchNearby()
                        }
                    }
                    Material.foreground: root.textColor
                    background: Rectangle {
                        radius: 8
                        color: parent.down ? Qt.darker("#457B9D", 1.18)
                             : parent.hovered ? Qt.lighter("#457B9D", 1.08)
                                              : "#457B9D"
                    }
                }

                Button {
                    text: qsTr("Search Directory")
                    onClicked: batchSearchDialog.open()
                    Material.foreground: root.textColor
                    background: Rectangle {
                        radius: 8
                        color: parent.down ? Qt.rgba(1, 1, 1, 0.06)
                             : parent.hovered ? Qt.rgba(1, 1, 1, 0.12)
                                              : Qt.rgba(1, 1, 1, 0.07)
                        border.width: 1
                        border.color: Qt.rgba(root.textColor.r, root.textColor.g, root.textColor.b, 0.12)
                    }
                }

                Item { Layout.fillWidth: true }

                BusyIndicator {
                    visible: root.autoSearching
                    running: root.autoSearching
                    implicitWidth: 22
                    implicitHeight: 22
                }

                Label {
                    visible: root.autoSearching
                    text: qsTr("Searching...")
                    color: root.accentColor
                    font.pixelSize: 11
                }
            }

            // File list
            Rectangle {
                Layout.fillWidth: true
                Layout.fillHeight: true
                radius: 8
                color: root.cardColor

                ListView {
                    id: fileListView
                    anchors.fill: parent
                    anchors.margins: 8
                    clip: true
                    spacing: 4
                    model: root.missingFiles

                    delegate: Rectangle {
                        width: fileListView.width - 16
                        height: 56
                        radius: 6
                        color: mouseArea.containsMouse ? Qt.rgba(1, 1, 1, 0.03) : "transparent"

                        readonly property string filePath: modelData
                        readonly property string statusKey: root.relinkStatus[modelData] || "not_found"

                        RowLayout {
                            anchors.fill: parent
                            anchors.leftMargin: 12
                            anchors.rightMargin: 12
                            spacing: 10

                            // Status indicator
                            Rectangle {
                                implicitWidth: 8
                                implicitHeight: 8
                                radius: 4
                                color: {
                                    if (statusKey === "found") return root.successColor
                                    if (statusKey === "searching") return root.accentColor
                                    return root.dangerColor
                                }

                                SequentialAnimation on opacity {
                                    running: statusKey === "searching"
                                    loops: Animation.Infinite
                                    NumberAnimation { from: 1.0; to: 0.3; duration: 600 }
                                    NumberAnimation { from: 0.3; to: 1.0; duration: 600 }
                                }
                            }

                            ColumnLayout {
                                Layout.fillWidth: true
                                spacing: 2

                                Label {
                                    Layout.fillWidth: true
                                    text: {
                                        var parts = filePath.split("/")
                                        return parts[parts.length - 1]
                                    }
                                    color: root.textColor
                                    font.pixelSize: 12
                                    font.weight: Font.DemiBold
                                    elide: Text.ElideRight
                                }

                                Label {
                                    Layout.fillWidth: true
                                    text: filePath
                                    color: root.mutedTextColor
                                    font.family: root.dataFontFamily
                                    font.pixelSize: 10
                                    elide: Text.ElideLeft
                                }
                            }

                            Button {
                                text: qsTr("Browse")
                                visible: statusKey !== "found"
                                onClicked: {
                                    browseDialog.currentMissingPath = filePath
                                    browseDialog.open()
                                }
                                implicitHeight: 28
                                font.pixelSize: 11
                                Material.foreground: root.textColor
                                background: Rectangle {
                                    radius: 6
                                    color: parent.down ? Qt.rgba(1, 1, 1, 0.06)
                                         : parent.hovered ? Qt.rgba(1, 1, 1, 0.12)
                                                          : Qt.rgba(1, 1, 1, 0.07)
                                    border.width: 1
                                    border.color: Qt.rgba(root.textColor.r, root.textColor.g, root.textColor.b, 0.12)
                                }
                            }

                            Label {
                                visible: statusKey === "found"
                                text: qsTr("Found")
                                color: root.successColor
                                font.pixelSize: 11
                                font.weight: Font.DemiBold
                            }
                        }

                        MouseArea {
                            id: mouseArea
                            anchors.fill: parent
                            hoverEnabled: true
                            acceptedButtons: Qt.NoButton
                        }
                    }
                }
            }

            Label {
                Layout.fillWidth: true
                visible: root.missingFiles.length === 0
                text: qsTr("No missing files detected. All files are accessible.")
                color: root.mutedTextColor
                font.pixelSize: 13
                horizontalAlignment: Text.AlignHCenter
            }
        }

        // Footer
        Rectangle {
            Layout.fillWidth: true
            Layout.preferredHeight: 1
            color: Qt.rgba(1, 1, 1, 0.08)
        }

        RowLayout {
            Layout.fillWidth: true
            Layout.topMargin: 14
            Layout.bottomMargin: 14
            Layout.leftMargin: 24
            Layout.rightMargin: 22
            spacing: 12

            Button {
                text: qsTr("Cancel")
                onClicked: root.close()
                Material.foreground: root.textColor
                background: Rectangle {
                    radius: 8
                    color: Qt.rgba(1, 1, 1, 0.07)
                    border.width: 1
                    border.color: Qt.rgba(root.textColor.r, root.textColor.g, root.textColor.b, 0.12)
                }
            }

            Item { Layout.fillWidth: true }

            Button {
                highlighted: true
                text: qsTr("Done")
                enabled: root.checkAllRelinked()
                onClicked: {
                    root.allFilesRelinked()
                    root.close()
                }
                Material.foreground: root.textColor
                background: Rectangle {
                    radius: 8
                    color: parent.enabled
                           ? (parent.down ? Qt.darker("#457B9D", 1.18)
                              : parent.hovered ? Qt.lighter("#457B9D", 1.08)
                                               : "#457B9D")
                           : Qt.rgba(0.27, 0.48, 0.62, 0.45)
                }
            }
        }
    }

    // Browse for single file replacement
    FileDialog {
        id: browseDialog
        property string currentMissingPath: ""
        title: qsTr("Select Replacement File")
        onAccepted: {
            if (root.relinkService && currentMissingPath.length > 0) {
                var newPath = selectedFile.toString().replace("file://", "")
                var success = root.relinkService.RelinkFile(currentMissingPath, newPath)
                if (success) {
                    var s = Object.assign({}, root.relinkStatus)
                    s[currentMissingPath] = "found"
                    root.relinkStatus = s
                }
            }
        }
    }

    // Batch search directory
    FolderDialog {
        id: batchSearchDialog
        title: qsTr("Select Directory to Search")
        onAccepted: {
            if (root.relinkService) {
                root.autoSearching = true
                var dir = selectedFolder.toString().replace("file://", "")
                root.relinkService.BatchSearchDirectory(dir)
            }
        }
    }

    Connections {
        target: root.relinkService
        function onFileRelinked(path) {
            var s = Object.assign({}, root.relinkStatus)
            s[path] = "found"
            root.relinkStatus = s
        }
        function onAutoSearchCompleted(results) {
            root.autoSearching = false
            root.refreshMissingFiles()
        }
        function onBatchSearchCompleted(results) {
            root.autoSearching = false
            root.refreshMissingFiles()
        }
    }
}

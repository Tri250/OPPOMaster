import QtQuick
import QtQuick.Controls
import QtQuick.Controls.Material
import QtQuick.Dialogs
import QtQuick.Layouts

ColumnLayout {
    id: panel

    property var semanticController: null
    property string importPreference: "ask"
    property color primaryAccent: "#457B9D"
    property color secondaryAccent: "#9FC7D8"
    property color textColor: "#F5F1EA"
    property color mutedTextColor: "#B6B0A7"
    property color canvasColor: "#111214"
    property color dividerColor: Qt.rgba(1, 1, 1, 0.08)
    property color dangerColor: "#D96C75"
    property string dataFontFamily: appTheme.dataFontFamily

    signal importPreferenceRequested(string preference)
    signal messageRequested(string message)

    readonly property bool hasController: semanticController !== null
    readonly property int albumTotalCount: hasController ? semanticController.albumTotalCount : 0
    readonly property int albumLabeledCount: hasController ? semanticController.albumLabeledCount : 0
    readonly property int albumUnlabeledCount: hasController ? semanticController.albumUnlabeledCount : 0
    readonly property bool generationRunning: hasController ? semanticController.running : false
    readonly property int progressTotal: hasController ? semanticController.total : 0
    readonly property int progressCompleted: hasController
                                           ? semanticController.embedded
                                             + semanticController.skipped
                                             + semanticController.failed
                                             + semanticController.canceled
                                           : 0
    readonly property real progressValue: progressTotal > 0 ? progressCompleted / progressTotal : 0

    width: parent ? parent.width : implicitWidth
    spacing: 20

    function importPreferenceIndex(value) {
        if (value === "always") {
            return 0
        }
        if (value === "never") {
            return 2
        }
        return 1
    }

    function preferenceForIndex(index) {
        if (index === 0) {
            return "always"
        }
        if (index === 2) {
            return "never"
        }
        return "ask"
    }

    function modelProfileIndex(profileId) {
        if (!panel.hasController) {
            return 0
        }
        const options = panel.semanticController.modelProfileOptions
        for (let i = 0; i < options.length; ++i) {
            if (options[i].profileId === profileId) {
                return i
            }
        }
        return 0
    }

    function endpointPresetIndex(preset) {
        if (preset === "huggingface") {
            return 1
        }
        if (preset === "custom") {
            return 2
        }
        return 0
    }

    function endpointPresetForIndex(index) {
        if (index === 1) {
            return "huggingface"
        }
        if (index === 2) {
            return "custom"
        }
        return "mirror"
    }

    FolderDialog {
        id: modelFolderDialog
        title: qsTr("Select Model Download Folder")
        onAccepted: {
            if (panel.hasController) {
                panel.semanticController.SetModelDownloadDirectory(selectedFolder.toString())
            }
        }
    }

    Component.onCompleted: {
        if (panel.hasController) {
            panel.semanticController.RefreshAlbumSummary()
            panel.semanticController.RefreshSelectedModelStatus()
        }
    }

    onSemanticControllerChanged: {
        if (panel.hasController) {
            panel.semanticController.RefreshAlbumSummary()
            panel.semanticController.RefreshSelectedModelStatus()
        }
    }

    SettingsSection {
        Layout.fillWidth: true
        title: qsTr("AI content recognition")
        textColor: panel.textColor
        mutedTextColor: panel.mutedTextColor
        dividerColor: panel.dividerColor

        GridLayout {
            Layout.fillWidth: true
            columns: 3
            columnSpacing: 12
            rowSpacing: 12

            MetricCard {
                Layout.fillWidth: true
                label: qsTr("Images")
                value: String(panel.albumTotalCount)
            }

            MetricCard {
                Layout.fillWidth: true
                label: qsTr("With labels")
                value: String(panel.albumLabeledCount)
            }

            MetricCard {
                Layout.fillWidth: true
                label: qsTr("Need labels")
                value: String(panel.albumUnlabeledCount)
            }
        }

        RowLayout {
            Layout.fillWidth: true
            spacing: 16

            ColumnLayout {
                Layout.fillWidth: true
                spacing: 6

                Label {
                    Layout.fillWidth: true
                    text: panel.generationRunning
                          ? (panel.hasController ? panel.semanticController.statusText : "")
                          : qsTr("Generate labels only for images that do not have AI content labels.")
                    color: panel.mutedTextColor
                    font.pixelSize: 13
                    font.weight: 500
                    wrapMode: Text.WordWrap
                    lineHeight: 1.25
                }

                ProgressBar {
                    visible: panel.generationRunning
                    Layout.fillWidth: true
                    Layout.preferredHeight: 8
                    from: 0
                    to: 1
                    value: panel.progressValue
                    indeterminate: panel.progressTotal <= 0
                }

                Label {
                    visible: panel.generationRunning
                    text: qsTr("%1 / %2").arg(panel.progressCompleted).arg(panel.progressTotal)
                    color: panel.textColor
                    font.family: panel.dataFontFamily
                    font.pixelSize: 13
                    font.weight: 700
                }
            }

            Button {
                id: generateButton
                Layout.preferredWidth: 168
                Layout.preferredHeight: 48
                text: panel.generationRunning ? qsTr("Cancel") : qsTr("Generate")
                enabled: panel.hasController
                         && (panel.generationRunning || panel.albumUnlabeledCount > 0)
                font.pixelSize: 15
                font.weight: 800
                Material.foreground: panel.textColor
                onClicked: {
                    if (!panel.hasController) {
                        return
                    }
                    if (panel.generationRunning) {
                        panel.semanticController.CancelGeneration()
                    } else {
                        panel.semanticController.StartAlbumGeneration(false)
                    }
                }
                background: Rectangle {
                    radius: 10
                    color: generateButton.down
                           ? Qt.darker(panel.primaryAccent, 1.16)
                           : (generateButton.hovered
                              ? Qt.lighter(panel.primaryAccent, 1.06)
                              : panel.primaryAccent)
                    border.width: 1
                    border.color: Qt.rgba(panel.secondaryAccent.r,
                                          panel.secondaryAccent.g,
                                          panel.secondaryAccent.b,
                                          0.18)
                    opacity: generateButton.enabled ? 1.0 : 0.45
                }
            }
        }
    }

    SettingsSection {
        Layout.fillWidth: true
        title: qsTr("Model")
        textColor: panel.textColor
        mutedTextColor: panel.mutedTextColor
        dividerColor: panel.dividerColor

        RowLayout {
            Layout.fillWidth: true
            spacing: 16

            Label {
                Layout.preferredWidth: 180
                text: qsTr("Model")
                color: panel.textColor
                font.pixelSize: 15
                font.weight: 600
            }

            ComboBox {
                id: modelBox
                Layout.fillWidth: true
                Layout.preferredHeight: 44
                enabled: panel.hasController && !panel.semanticController.modelDownloadRunning
                model: panel.hasController ? panel.semanticController.modelProfileOptions : []
                textRole: "label"
                currentIndex: panel.hasController
                              ? panel.modelProfileIndex(panel.semanticController.selectedModelProfileId)
                              : 0
                onActivated: function(index) {
                    const item = model[index]
                    if (item && panel.hasController) {
                        panel.semanticController.SetSelectedModelProfileId(item.profileId)
                        panel.semanticController.RefreshSelectedModelStatus()
                    }
                }
            }
        }

        RowLayout {
            Layout.fillWidth: true
            spacing: 16

            ColumnLayout {
                Layout.fillWidth: true
                spacing: 8

                Label {
                    text: qsTr("Download directory")
                    color: panel.textColor
                    font.pixelSize: 15
                    font.weight: 600
                }

                Rectangle {
                    Layout.fillWidth: true
                    Layout.preferredHeight: 50
                    radius: 8
                    color: Qt.rgba(1, 1, 1, 0.10)
                    border.width: 1
                    border.color: Qt.rgba(panel.textColor.r, panel.textColor.g, panel.textColor.b, 0.12)

                    Label {
                        anchors.fill: parent
                        anchors.leftMargin: 14
                        anchors.rightMargin: 14
                        text: panel.hasController ? panel.semanticController.modelDownloadDirectory : ""
                        elide: Text.ElideMiddle
                        verticalAlignment: Text.AlignVCenter
                        color: panel.textColor
                        font.family: panel.dataFontFamily
                        font.pixelSize: 14
                    }
                }
            }

            Rectangle {
                Layout.preferredWidth: 50
                Layout.preferredHeight: 50
                Layout.alignment: Qt.AlignBottom
                radius: 8
                color: modelBrowseMouse.pressed
                       ? Qt.rgba(1, 1, 1, 0.06)
                       : (modelBrowseMouse.containsMouse
                          ? Qt.rgba(1, 1, 1, 0.12)
                          : Qt.rgba(1, 1, 1, 0.07))
                border.width: 1
                border.color: Qt.rgba(panel.textColor.r, panel.textColor.g, panel.textColor.b, 0.14)
                opacity: panel.hasController && !panel.semanticController.modelDownloadRunning ? 1 : 0.45

                Image {
                    anchors.centerIn: parent
                    width: 23
                    height: 23
                    source: "qrc:/panel_icons/folder-open.svg"
                    sourceSize.width: 23
                    sourceSize.height: 23
                    asynchronous: true
                }

                MouseArea {
                    id: modelBrowseMouse
                    anchors.fill: parent
                    enabled: panel.hasController && !panel.semanticController.modelDownloadRunning
                    hoverEnabled: true
                    cursorShape: Qt.PointingHandCursor
                    onClicked: modelFolderDialog.open()
                }
            }
        }

        RowLayout {
            Layout.fillWidth: true
            spacing: 16

            Label {
                Layout.preferredWidth: 180
                text: qsTr("Source")
                color: panel.textColor
                font.pixelSize: 15
                font.weight: 600
            }

            ComboBox {
                id: endpointBox
                Layout.preferredWidth: 210
                Layout.preferredHeight: 44
                enabled: panel.hasController && !panel.semanticController.modelDownloadRunning
                model: [
                    qsTr("HF Mirror"),
                    qsTr("Hugging Face"),
                    qsTr("Custom")
                ]
                currentIndex: panel.hasController
                              ? panel.endpointPresetIndex(panel.semanticController.modelEndpointPreset)
                              : 0
                onActivated: function(index) {
                    if (panel.hasController) {
                        panel.semanticController.SetModelEndpointPreset(panel.endpointPresetForIndex(index))
                    }
                }
            }

            TextField {
                Layout.fillWidth: true
                Layout.preferredHeight: 44
                visible: panel.hasController && panel.semanticController.modelEndpointPreset === "custom"
                enabled: panel.hasController && !panel.semanticController.modelDownloadRunning
                text: panel.hasController ? panel.semanticController.customModelEndpoint : ""
                placeholderText: qsTr("https://example.com")
                color: panel.textColor
                onEditingFinished: {
                    if (panel.hasController) {
                        panel.semanticController.SetCustomModelEndpoint(text)
                    }
                }
            }
        }

        ColumnLayout {
            Layout.fillWidth: true
            spacing: 8

            Label {
                Layout.fillWidth: true
                text: panel.hasController ? panel.semanticController.modelDownloadStatusText : ""
                color: panel.mutedTextColor
                font.pixelSize: 13
                font.weight: 500
                wrapMode: Text.WordWrap
                lineHeight: 1.25
            }

            ProgressBar {
                visible: panel.hasController && panel.semanticController.modelDownloadRunning
                Layout.fillWidth: true
                Layout.preferredHeight: 8
                from: 0
                to: 100
                value: panel.hasController ? panel.semanticController.modelDownloadProgress : 0
                indeterminate: panel.hasController
                               && panel.semanticController.modelDownloadRunning
                               && panel.semanticController.modelDownloadProgress <= 0
            }

            RowLayout {
                Layout.fillWidth: true
                spacing: 12

                Button {
                    Layout.preferredHeight: 42
                    text: qsTr("Check")
                    enabled: panel.hasController && !panel.semanticController.modelDownloadRunning
                    Material.foreground: panel.textColor
                    onClicked: panel.semanticController.RefreshSelectedModelStatus()
                }

                Button {
                    Layout.preferredHeight: 42
                    text: panel.hasController && panel.semanticController.modelDownloadRunning
                          ? qsTr("Cancel")
                          : qsTr("Download")
                    enabled: panel.hasController
                    Material.foreground: panel.textColor
                    onClicked: {
                        if (panel.semanticController.modelDownloadRunning) {
                            panel.semanticController.CancelSelectedModelDownload()
                        } else {
                            panel.semanticController.StartSelectedModelDownload()
                        }
                    }
                }

                Button {
                    Layout.preferredHeight: 42
                    text: qsTr("Delete")
                    enabled: panel.hasController && !panel.semanticController.modelDownloadRunning
                    Material.foreground: panel.dangerColor
                    onClicked: panel.semanticController.DeleteSelectedModel()
                }

                Button {
                    Layout.preferredHeight: 42
                    text: qsTr("Activate")
                    enabled: panel.hasController
                             && !panel.semanticController.modelDownloadRunning
                             && panel.semanticController.selectedModelProfileId === panel.semanticController.activeModelProfileId
                    Material.foreground: panel.textColor
                    onClicked: panel.semanticController.ActivateSelectedModel()
                }

                Item { Layout.fillWidth: true }
            }
        }
    }

    SettingsSection {
        Layout.fillWidth: true
        title: qsTr("Import")
        textColor: panel.textColor
        mutedTextColor: panel.mutedTextColor
        dividerColor: panel.dividerColor

        RowLayout {
            Layout.fillWidth: true
            spacing: 16

            Label {
                Layout.preferredWidth: 180
                text: qsTr("是否生成标签")
                color: panel.textColor
                font.pixelSize: 15
                font.weight: 600
            }

            ComboBox {
                id: importBehaviorBox
                Layout.fillWidth: true
                Layout.preferredHeight: 44
                model: [
                    qsTr("Always"),
                    qsTr("Always Ask"),
                    qsTr("Always Skip")
                ]
                currentIndex: panel.importPreferenceIndex(panel.importPreference)
                onActivated: function(index) {
                    panel.importPreferenceRequested(panel.preferenceForIndex(index))
                }
            }
        }
    }

    Item { Layout.fillHeight: true }

    component SettingsSection: ColumnLayout {
        property string title: ""
        property color textColor: "white"
        property color mutedTextColor: "#999999"
        property color dividerColor: Qt.rgba(1, 1, 1, 0.08)

        spacing: 14

        Label {
            Layout.fillWidth: true
            text: title
            color: textColor
            font.pixelSize: 18
            font.weight: 800
        }

        Rectangle {
            Layout.fillWidth: true
            Layout.preferredHeight: 1
            color: dividerColor
        }
    }

    component MetricCard: Rectangle {
        property string label: ""
        property string value: ""

        implicitHeight: 84
        radius: 8
        color: Qt.rgba(panel.canvasColor.r, panel.canvasColor.g, panel.canvasColor.b, 0.62)

        ColumnLayout {
            anchors.fill: parent
            anchors.margins: 14
            spacing: 6

            Label {
                Layout.fillWidth: true
                text: label
                color: panel.mutedTextColor
                font.pixelSize: 12
                font.weight: 700
                elide: Text.ElideRight
            }

            Label {
                Layout.fillWidth: true
                text: value
                color: panel.textColor
                font.family: panel.dataFontFamily
                font.pixelSize: 18
                font.weight: 700
                elide: Text.ElideRight
            }
        }
    }
}

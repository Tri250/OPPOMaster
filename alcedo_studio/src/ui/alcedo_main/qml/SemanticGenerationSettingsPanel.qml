import QtQuick
import QtQuick.Controls
import QtQuick.Controls.Material
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

    Component.onCompleted: {
        if (panel.hasController) {
            panel.semanticController.RefreshAlbumSummary()
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

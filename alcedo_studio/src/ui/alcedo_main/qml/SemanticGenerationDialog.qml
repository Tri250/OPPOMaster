import QtQuick
import QtQuick.Controls
import QtQuick.Layouts
import QtQuick.Effects

Popup {
    id: root
    font.family: appTheme.uiFontFamily
    modal: true
    focus: true
    visible: promptVisible || generationRunning
    closePolicy: generationRunning ? Popup.NoAutoClose
                                   : Popup.CloseOnEscape | Popup.CloseOnPressOutside
    anchors.centerIn: parent
    width: Math.min(parent ? parent.width - 64 : 560, 560)
    height: contentColumn.implicitHeight + 44
    padding: 0

    property bool promptVisible: false
    property bool generationRunning: false
    property int pendingCount: 0
    property int total: 0
    property int embedded: 0
    property int skipped: 0
    property int failed: 0
    property int canceled: 0
    property string statusText: ""
    property Item backgroundSource: null

    signal startRequested(bool forceRegenerate)
    signal alwaysStartRequested()
    signal skipRequested()
    signal neverAskRequested()
    signal cancelRequested()

    readonly property int completed: embedded + skipped + failed + canceled
    readonly property real progressValue: total > 0 ? completed / total : 0
    readonly property color modalColor: appTheme.bgPanelColor
    readonly property color raisedColor: appTheme.bgDeepColor
    readonly property color inputColor: appTheme.bgBaseColor
    readonly property color hoverColor: appTheme.hoverColor
    readonly property color accentColor: appTheme.accentColor
    readonly property color textColor: appTheme.textColor
    readonly property color mutedTextColor: appTheme.textMutedColor
    readonly property color dangerColor: appTheme.dangerColor
    readonly property color strokeColor: appTheme.glassStrokeColor

    Overlay.modal: Item {
        anchors.fill: parent

        MultiEffect {
            visible: root.backgroundSource !== null
            anchors.fill: parent
            source: root.backgroundSource
            blurEnabled: true
            blur: 0.62
            blurMax: 64
            saturation: -0.24
        }

        Rectangle {
            anchors.fill: parent
            color: appTheme.overlayColor
        }

        MouseArea { anchors.fill: parent; hoverEnabled: true }
    }

    background: Rectangle {
        radius: appTheme.panelRadius + 2
        color: root.modalColor
        border.width: 1
        border.color: root.strokeColor
    }

    contentItem: ColumnLayout {
        id: contentColumn
        spacing: 0

        ColumnLayout {
            Layout.fillWidth: true
            Layout.margins: 24
            spacing: 18

            RowLayout {
                Layout.fillWidth: true
                spacing: 12

                ColumnLayout {
                    Layout.fillWidth: true
                    spacing: 6

                    Label {
                        Layout.fillWidth: true
                        text: root.generationRunning
                              ? qsTr("Generating semantic labels")
                              : qsTr("Generate semantic labels")
                        color: root.textColor
                        font.family: appTheme.headlineFontFamily
                        font.pixelSize: 24
                        font.weight: 800
                        wrapMode: Text.WordWrap
                    }

                    Label {
                        Layout.fillWidth: true
                        text: root.generationRunning
                              ? (root.statusText.length > 0 ? root.statusText : qsTr("Preparing semantic generation..."))
                              : qsTr("Alcedo can create embeddings and label suggestions for the %1 imported image(s).")
                                    .arg(root.pendingCount)
                        color: root.mutedTextColor
                        font.pixelSize: 13
                        font.weight: 600
                        lineHeight: 1.25
                        wrapMode: Text.WordWrap
                    }
                }

                BusyIndicator {
                    running: root.generationRunning
                    visible: root.generationRunning
                    implicitWidth: 30
                    implicitHeight: 30
                }
            }

            ColumnLayout {
                Layout.fillWidth: true
                visible: root.generationRunning
                spacing: 10

                ImportProgressRing {
                    Layout.alignment: Qt.AlignHCenter
                    Layout.preferredWidth: 150
                    Layout.preferredHeight: 150
                    ringWidth: 13
                    trackColor: root.hoverColor
                    fillColor: root.accentColor
                    progress: root.progressValue
                }

                Label {
                    Layout.alignment: Qt.AlignHCenter
                    text: qsTr("%1 / %2").arg(root.completed).arg(root.total)
                    color: root.textColor
                    font.family: appTheme.dataFontFamily
                    font.pixelSize: 26
                    font.weight: 700
                }

                Rectangle {
                    Layout.fillWidth: true
                    Layout.preferredHeight: 74
                    radius: appTheme.panelRadius
                    color: root.raisedColor
                    border.width: 1
                    border.color: root.strokeColor

                    GridLayout {
                        anchors.fill: parent
                        anchors.margins: 14
                        columns: 4
                        rowSpacing: 3
                        columnSpacing: 10

                        Repeater {
                            model: [
                                { label: qsTr("Generated"), value: root.embedded },
                                { label: qsTr("Skipped"), value: root.skipped },
                                { label: qsTr("Failed"), value: root.failed },
                                { label: qsTr("Canceled"), value: root.canceled }
                            ]

                            ColumnLayout {
                                required property var modelData
                                Layout.fillWidth: true
                                spacing: 3
                                Label {
                                    Layout.alignment: Qt.AlignHCenter
                                    text: String(modelData.value)
                                    color: root.textColor
                                    font.family: appTheme.dataFontFamily
                                    font.pixelSize: 18
                                    font.weight: 800
                                }
                                Label {
                                    Layout.alignment: Qt.AlignHCenter
                                    text: modelData.label
                                    color: root.mutedTextColor
                                    font.pixelSize: 11
                                    font.weight: 700
                                }
                            }
                        }
                    }
                }
            }

            ColumnLayout {
                Layout.fillWidth: true
                visible: !root.generationRunning
                spacing: 10

                Rectangle {
                    Layout.fillWidth: true
                    Layout.preferredHeight: 76
                    radius: appTheme.panelRadius
                    color: root.raisedColor
                    border.width: 1
                    border.color: root.strokeColor

                    Label {
                        anchors.fill: parent
                        anchors.margins: 14
                        verticalAlignment: Text.AlignVCenter
                        text: root.statusText.length > 0
                              ? root.statusText
                              : qsTr("Existing ready embeddings for the active model will be skipped. Failed or incomplete rows can be retried.")
                        color: root.mutedTextColor
                        font.pixelSize: 12
                        font.weight: 600
                        lineHeight: 1.25
                        wrapMode: Text.WordWrap
                    }
                }
            }
        }

        Rectangle {
            Layout.fillWidth: true
            Layout.preferredHeight: root.generationRunning ? 68 : 124
            color: appTheme.bgCanvasColor

            Rectangle {
                anchors.left: parent.left
                anchors.right: parent.right
                anchors.top: parent.top
                height: 1
                color: appTheme.dividerColor
            }

            ColumnLayout {
                anchors.fill: parent
                anchors.margins: 18
                spacing: 10

                RowLayout {
                    Layout.fillWidth: true
                    visible: !root.generationRunning
                    spacing: 10

                    Button {
                        Layout.fillWidth: true
                        text: qsTr("Skip")
                        onClicked: root.skipRequested()
                    }

                    Button {
                        Layout.fillWidth: true
                        text: qsTr("Never ask")
                        onClicked: root.neverAskRequested()
                    }

                    Button {
                        Layout.fillWidth: true
                        text: qsTr("Always start")
                        highlighted: true
                        onClicked: root.alwaysStartRequested()
                    }
                }

                RowLayout {
                    Layout.fillWidth: true
                    spacing: 10

                    Button {
                        Layout.fillWidth: true
                        visible: root.generationRunning
                        text: qsTr("Cancel")
                        onClicked: root.cancelRequested()
                    }

                    Button {
                        Layout.fillWidth: true
                        visible: !root.generationRunning
                        text: qsTr("Force regenerate")
                        onClicked: root.startRequested(true)
                    }

                    Button {
                        Layout.fillWidth: true
                        visible: !root.generationRunning
                        text: qsTr("Start now")
                        highlighted: true
                        onClicked: root.startRequested(false)
                    }
                }
            }
        }
    }
}

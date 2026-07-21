import QtQuick
import QtQuick.Controls
import QtQuick.Layouts
import QtQuick.Dialogs

Dialog {
    id: root
    font.family: appTheme.uiFontFamily
    modal: true
    focus: true
    width: Math.min(parent ? parent.width - 40 : 520, 520)
    height: Math.min(parent ? parent.height - 40 : 460, 460)
    x: parent ? Math.round((parent.width - width) / 2) : 0
    y: parent ? Math.round((parent.height - height) / 2) : 0
    padding: 0
    closePolicy: Popup.NoAutoClose

    property var crashReporter: null

    readonly property color panelColor: appTheme.bgPanelColor
    readonly property color cardColor: appTheme.bgBaseColor
    readonly property color textColor: appTheme.textColor
    readonly property color mutedTextColor: appTheme.textMutedColor
    readonly property color dangerColor: appTheme.dangerColor
    readonly property string dataFontFamily: appTheme.dataFontFamily
    readonly property string headlineFontFamily: appTheme.headlineFontFamily

    signal restartRequested()
    signal closeRequested()

    function showIfPending() {
        if (crashReporter && crashReporter.hasPendingCrashReport) {
            open()
        }
    }

    background: Rectangle {
        radius: 14
        color: root.panelColor
        border.width: 1
        border.color: Qt.rgba(1, 1, 1, 0.06)
    }

    contentItem: ColumnLayout {
        anchors.fill: parent
        spacing: 0

        // Header with warning icon
        RowLayout {
            Layout.fillWidth: true
            Layout.topMargin: 22
            Layout.leftMargin: 24
            Layout.rightMargin: 22
            Layout.bottomMargin: 14
            spacing: 12

            Label {
                text: qsTr("Unexpected Shutdown")
                font.family: root.headlineFontFamily
                font.pixelSize: 20
                font.weight: Font.Medium
                color: root.dangerColor
            }

            Item { Layout.fillWidth: true }
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

            Label {
                Layout.fillWidth: true
                text: qsTr("Alcedo Studio closed unexpectedly in the last session. We apologize for the inconvenience.")
                color: root.textColor
                font.pixelSize: 14
                wrapMode: Text.WordWrap
            }

            // Crash info card
            Rectangle {
                Layout.fillWidth: true
                Layout.preferredHeight: crashInfoCol.implicitHeight + 28
                radius: 8
                color: root.cardColor

                ColumnLayout {
                    id: crashInfoCol
                    y: 14
                    x: 16
                    width: parent.width - 32
                    spacing: 8

                    Label {
                        Layout.fillWidth: true
                        text: qsTr("Crash Information")
                        font.pixelSize: 13
                        font.weight: Font.DemiBold
                        color: root.textColor
                    }

                    RowLayout {
                        Layout.fillWidth: true
                        visible: root.crashReporter && root.crashReporter.pendingCrashInfo.length > 0

                        Label {
                            text: qsTr("Details:")
                            color: root.mutedTextColor
                            font.pixelSize: 12
                        }

                        Label {
                            Layout.fillWidth: true
                            text: root.crashReporter ? root.crashReporter.pendingCrashInfo : ""
                            color: root.textColor
                            font.family: root.dataFontFamily
                            font.pixelSize: 12
                            wrapMode: Text.WordWrap
                        }
                    }
                }
            }

            // Save report option
            RowLayout {
                Layout.fillWidth: true
                spacing: 12

                Button {
                    text: qsTr("Save Crash Report")
                    onClicked: saveReportDialog.open()
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

                Label {
                    Layout.fillWidth: true
                    text: qsTr("Save the crash report to a file for manual inspection or sharing.")
                    color: root.mutedTextColor
                    font.pixelSize: 11
                    wrapMode: Text.WordWrap
                }
            }

            Item { Layout.fillHeight: true }
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
                text: qsTr("Close")
                onClicked: {
                    if (root.crashReporter) {
                        root.crashReporter.DismissPending()
                    }
                    root.closeRequested()
                    root.close()
                }
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

            Button {
                highlighted: true
                text: qsTr("Restart")
                onClicked: {
                    if (root.crashReporter) {
                        root.crashReporter.DismissPending()
                    }
                    root.restartRequested()
                    root.close()
                }
                Material.foreground: root.textColor
                background: Rectangle {
                    radius: 8
                    color: parent.down ? Qt.darker("#457B9D", 1.18)
                         : parent.hovered ? Qt.lighter("#457B9D", 1.08)
                                          : "#457B9D"
                }
            }
        }
    }

    FileDialog {
        id: saveReportDialog
        title: qsTr("Save Crash Report")
        fileMode: FileDialog.SaveFile
        nameFilters: ["Text files (*.txt)", "All files (*)"]
        defaultSuffix: "txt"
        onAccepted: {
            if (root.crashReporter) {
                var report = root.crashReporter.GetPendingCrashReport()
                if (report) {
                    root.crashReporter.ExportCrashReport(report, selectedFile.toString().replace("file://", ""))
                }
            }
        }
    }

    Connections {
        target: root.crashReporter
        function onPendingCrashChanged() {
            if (root.crashReporter && root.crashReporter.hasPendingCrashReport) {
                root.open()
            }
        }
    }
}

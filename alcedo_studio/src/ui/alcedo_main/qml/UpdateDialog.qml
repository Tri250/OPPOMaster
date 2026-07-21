import QtQuick
import QtQuick.Controls
import QtQuick.Layouts

Dialog {
    id: root
    font.family: appTheme.uiFontFamily
    modal: true
    focus: true
    width: Math.min(parent ? parent.width - 40 : 480, 480)
    height: Math.min(parent ? parent.height - 40 : 520, 520)
    x: parent ? Math.round((parent.width - width) / 2) : 0
    y: parent ? Math.round((parent.height - height) / 2) : 0
    padding: 0
    closePolicy: Popup.CloseOnEscape

    property var updateChecker: null
    property var updateInfo: null
    property bool updateAvailable: false

    readonly property color panelColor: appTheme.bgPanelColor
    readonly property color cardColor: appTheme.bgBaseColor
    readonly property color textColor: appTheme.textColor
    readonly property color mutedTextColor: appTheme.textMutedColor
    readonly property color accentColor: appTheme.accentColor
    readonly property string dataFontFamily: appTheme.dataFontFamily
    readonly property string headlineFontFamily: appTheme.headlineFontFamily

    signal downloadRequested()
    signal skipVersionRequested(string version)
    signal remindLaterRequested()

    function showUpdate(info) {
        root.updateInfo = info
        root.updateAvailable = true
        root.open()
    }

    function showNoUpdate() {
        root.updateAvailable = false
        root.updateInfo = null
        noUpdateLabel.visible = true
        root.open()
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

        // Header
        RowLayout {
            Layout.fillWidth: true
            Layout.topMargin: 22
            Layout.leftMargin: 24
            Layout.rightMargin: 22
            Layout.bottomMargin: 14
            spacing: 12

            Label {
                text: qsTr("Software Update")
                font.family: root.headlineFontFamily
                font.pixelSize: 20
                font.weight: Font.Medium
                color: root.textColor
            }

            Item { Layout.fillWidth: true }

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
            spacing: 16

            // Version info
            ColumnLayout {
                Layout.fillWidth: true
                spacing: 8

                RowLayout {
                    Layout.fillWidth: true
                    spacing: 16

                    Label {
                        text: qsTr("Current Version")
                        color: root.mutedTextColor
                        font.pixelSize: 13
                    }

                    Label {
                        text: root.updateChecker ? root.updateChecker.CurrentVersion() : ""
                        color: root.textColor
                        font.family: root.dataFontFamily
                        font.pixelSize: 13
                        font.weight: Font.DemiBold
                    }
                }

                RowLayout {
                    Layout.fillWidth: true
                    spacing: 16
                    visible: root.updateAvailable

                    Label {
                        text: qsTr("Latest Version")
                        color: root.mutedTextColor
                        font.pixelSize: 13
                    }

                    Label {
                        text: root.updateInfo ? root.updateInfo.version : ""
                        color: root.accentColor
                        font.family: root.dataFontFamily
                        font.pixelSize: 13
                        font.weight: Font.DemiBold
                    }

                    Rectangle {
                        visible: root.updateInfo && root.updateInfo.is_prerelease
                        radius: 4
                        color: "#3A3020"
                        border.width: 1
                        border.color: "#D8A93B"
                        implicitWidth: preReleaseTag.implicitWidth + 10
                        implicitHeight: 18

                        Label {
                            id: preReleaseTag
                            anchors.centerIn: parent
                            text: qsTr("Pre-release")
                            color: "#F2C766"
                            font.pixelSize: 10
                            font.weight: Font.DemiBold
                        }
                    }
                }
            }

            // Release notes
            Rectangle {
                Layout.fillWidth: true
                Layout.fillHeight: true
                radius: 8
                color: root.cardColor
                visible: root.updateAvailable

                ScrollView {
                    anchors.fill: parent
                    anchors.margins: 12
                    clip: true

                    Label {
                        text: root.updateInfo ? root.updateInfo.release_notes : ""
                        color: root.textColor
                        font.pixelSize: 12
                        wrapMode: Text.WordWrap
                        onLinkActivated: function(link) {
                            Qt.openUrlExternally(link)
                        }
                    }
                }
            }

            // No update available message
            Label {
                id: noUpdateLabel
                Layout.fillWidth: true
                Layout.fillHeight: true
                visible: !root.updateAvailable
                text: qsTr("You're up to date! Alcedo Studio is running the latest version.")
                color: root.mutedTextColor
                font.pixelSize: 14
                horizontalAlignment: Text.AlignHCenter
                verticalAlignment: Text.AlignVCenter
                wrapMode: Text.WordWrap
            }

            // Auto-update settings
            ColumnLayout {
                Layout.fillWidth: true
                spacing: 8

                CheckBox {
                    id: autoCheckToggle
                    text: qsTr("Automatically check for updates")
                    checked: root.updateChecker ? root.updateChecker.autoCheckEnabled : true
                    onToggled: {
                        if (root.updateChecker) {
                            root.updateChecker.autoCheckEnabled = checked
                        }
                    }
                    font.pixelSize: 12
                }

                RowLayout {
                    Layout.fillWidth: true
                    spacing: 8
                    visible: autoCheckToggle.checked

                    Label {
                        text: qsTr("Check interval")
                        color: root.mutedTextColor
                        font.pixelSize: 11
                    }

                    SpinBox {
                        id: intervalSpinBox
                        from: 1
                        to: 168
                        value: root.updateChecker ? root.updateChecker.checkIntervalHours : 24
                        onValueModified: {
                            if (root.updateChecker) {
                                root.updateChecker.checkIntervalHours = value
                            }
                        }
                        implicitWidth: 110
                    }

                    Label {
                        text: qsTr("hours")
                        color: root.mutedTextColor
                        font.pixelSize: 11
                    }
                }
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
                text: qsTr("Skip This Version")
                visible: root.updateAvailable
                onClicked: {
                    if (root.updateInfo) {
                        root.skipVersionRequested(root.updateInfo.version)
                    }
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

            Button {
                text: qsTr("Remind Me Later")
                visible: root.updateAvailable
                onClicked: {
                    root.remindLaterRequested()
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
                text: qsTr("Close")
                visible: !root.updateAvailable
                onClicked: root.close()
                Material.foreground: root.textColor
                background: Rectangle {
                    radius: 8
                    color: "#457B9D"
                    border.width: 1
                    border.color: Qt.rgba(1, 1, 1, 0.12)
                }
            }

            Button {
                highlighted: true
                text: qsTr("Download Update")
                visible: root.updateAvailable
                onClicked: {
                    root.downloadRequested()
                    if (root.updateInfo && root.updateInfo.release_page_url.length > 0) {
                        Qt.openUrlExternally(root.updateInfo.release_page_url)
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
        }
    }

    Connections {
        target: root.updateChecker
        function onUpdateAvailable(info) {
            root.showUpdate(info)
        }
        function onNoUpdateAvailable() {
            root.showNoUpdate()
        }
        function onCheckError(error) {
            root.updateAvailable = false
            root.updateInfo = null
            root.open()
        }
    }
}

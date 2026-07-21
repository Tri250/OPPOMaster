import QtQuick
import QtQuick.Controls
import QtQuick.Layouts

Item {
    id: root

    property var hdrManager: null

    readonly property color panelColor: appTheme.bgPanelColor
    readonly property color cardColor: appTheme.bgBaseColor
    readonly property color textColor: appTheme.textColor
    readonly property color mutedTextColor: appTheme.textMutedColor
    readonly property color accentColor: appTheme.accentColor
    readonly property color successColor: "#2E7D32"

    property bool hdrPreviewEnabled: root.hdrManager ? root.hdrManager.hdrPreviewEnabled : false
    property bool hdrDisplayAvailable: root.hdrManager ? root.hdrManager.isHdrDisplayAvailable : false
    property string displayInfo: root.hdrManager ? root.hdrManager.currentDisplayInfo : ""

    signal hdrPreviewToggled(bool enabled)

    function toggleHdrPreview() {
        if (root.hdrManager) {
            root.hdrManager.ToggleHdrPreview()
        }
    }

    implicitWidth: hdrRow.implicitWidth + 20
    implicitHeight: 34

    RowLayout {
        id: hdrRow
        anchors.fill: parent
        spacing: 6

        // HDR display status indicator
        Rectangle {
            implicitWidth: 8
            implicitHeight: 8
            radius: 4
            color: root.hdrDisplayAvailable ? root.successColor : root.mutedTextColor

            ToolTip.visible: hdrStatusMouseArea.containsMouse
            ToolTip.text: root.hdrDisplayAvailable
                          ? qsTr("HDR display detected: %1").arg(root.displayInfo)
                          : qsTr("No HDR display detected — HDR preview requires a compatible display")
            ToolTip.delay: 400

            MouseArea {
                id: hdrStatusMouseArea
                anchors.fill: parent
                hoverEnabled: true
                acceptedButtons: Qt.NoButton
            }
        }

        // HDR toggle button
        AbstractButton {
            id: hdrToggle
            Layout.preferredHeight: 28
            implicitWidth: hdrLabel.implicitWidth + 18

            checkable: true
            checked: root.hdrPreviewEnabled
            enabled: root.hdrDisplayAvailable

            onClicked: root.toggleHdrPreview()

            background: Rectangle {
                radius: 6
                color: {
                    if (!hdrToggle.enabled) return Qt.rgba(1, 1, 1, 0.04)
                    if (hdrToggle.checked) return Qt.rgba(root.accentColor.r, root.accentColor.g, root.accentColor.b, 0.18)
                    return Qt.rgba(1, 1, 1, 0.06)
                }
                border.width: 1
                border.color: hdrToggle.checked && hdrToggle.enabled
                              ? Qt.rgba(root.accentColor.r, root.accentColor.g, root.accentColor.b, 0.35)
                              : Qt.rgba(root.textColor.r, root.textColor.g, root.textColor.b, 0.08)
            }

            Label {
                id: hdrLabel
                anchors.centerIn: parent
                text: qsTr("HDR")
                font.pixelSize: 11
                font.weight: Font.DemiBold
                font.family: appTheme.dataFontFamily
                color: {
                    if (!hdrToggle.enabled) return Qt.rgba(root.mutedTextColor.r, root.mutedTextColor.g, root.mutedTextColor.b, 0.45)
                    if (hdrToggle.checked) return root.accentColor
                    return root.mutedTextColor
                }
            }

            ToolTip.visible: hdrToggleHovered.containsMouse
            ToolTip.text: root.hdrDisplayAvailable
                          ? (hdrToggle.checked ? qsTr("HDR preview is ON — click to disable") : qsTr("HDR preview is OFF — click to enable"))
                          : qsTr("HDR preview unavailable — no compatible display detected")
            ToolTip.delay: 500

            MouseArea {
                id: hdrToggleHovered
                anchors.fill: parent
                hoverEnabled: true
                acceptedButtons: Qt.NoButton
            }
        }
    }

    // Notification popup for HDR display change
    Popup {
        id: hdrNotification
        x: Math.round((parent.width - width) / 2)
        y: parent.height + 8
        width: 280
        padding: 10
        modal: false
        closePolicy: Popup.CloseOnEscape | Popup.CloseOnPressOutside

        background: Rectangle {
            radius: 8
            color: root.cardColor
            border.width: 1
            border.color: Qt.rgba(1, 1, 1, 0.08)
        }

        contentItem: Label {
            text: root.hdrDisplayAvailable
                  ? qsTr("HDR display connected — HDR preview is now available")
                  : qsTr("HDR display disconnected — HDR preview disabled")
            font.pixelSize: 12
            color: root.textColor
            wrapMode: Text.WordWrap
        }

        onOpened: hdrNotificationTimer.start()

        Timer {
            id: hdrNotificationTimer
            interval: 4000
            onTriggered: hdrNotification.close()
        }
    }

    Connections {
        target: root.hdrManager
        function onHdrDisplayChanged(available) {
            hdrNotification.open()
        }
        function onHdrPreviewEnabledChanged(enabled) {
            root.hdrPreviewEnabled = enabled
        }
    }
}

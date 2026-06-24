import QtQuick
import QtQuick.Controls
import QtQuick.Layouts
import QtQuick.Effects

// Shown on import when the project is "fresh" — no model has been installed or
// activated in it yet, so label generation is impossible. Instead of the usual
// "Use AI to analyze image content?" prompt, route the user to Settings → AI to
// install and activate a model. The generate/progress dialogs are intentionally
// NOT shown for this batch. Governed by the same ask/always/never import
// preference as SemanticGenerationDialog; "never" suppresses this dialog too.
Popup {
    id: root
    font.family: appTheme.uiFontFamily
    modal: true
    focus: true
    visible: promptVisible
    closePolicy: Popup.CloseOnEscape | Popup.CloseOnPressOutside
    anchors.centerIn: parent
    width: Math.min(parent ? parent.width - 56 : 560, 560)
    height: contentColumn.implicitHeight + 48
    padding: 0

    property bool promptVisible: false
    property Item backgroundSource: null

    signal openSettingsRequested()
    signal dismissed()

    readonly property color panelColor: appTheme.toneGraphite
    readonly property color textColor: appTheme.textColor
    readonly property color mutedTextColor: appTheme.textMutedColor
    readonly property color accentColor: appTheme.accentColor

    onClosed: root.dismissed()

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
        radius: 14
        color: root.panelColor
        border.width: 0
    }

    contentItem: ColumnLayout {
        id: contentColumn
        width: root.width
        spacing: 18

        ColumnLayout {
            Layout.fillWidth: true
            Layout.margins: 24
            spacing: 8

            Label {
                Layout.fillWidth: true
                text: qsTr("Set up an AI model to analyze images")
                color: root.textColor
                font.family: appTheme.headlineFontFamily
                font.pixelSize: 24
                font.weight: 700
                wrapMode: Text.WordWrap
            }

            Label {
                Layout.fillWidth: true
                text: qsTr("This project doesn't have an AI model activated yet, so content labels can't be generated. Install and activate a model in Settings to enable automatic label generation for your images.")
                color: root.mutedTextColor
                font.pixelSize: 13
                font.weight: 500
                lineHeight: 1.3
                wrapMode: Text.WordWrap
            }
        }

        RowLayout {
            Layout.fillWidth: true
            Layout.leftMargin: 24
            Layout.rightMargin: 24
            Layout.bottomMargin: 24
            spacing: 12

            Item { Layout.fillWidth: true }

            AiButton {
                Layout.preferredWidth: 132
                text: qsTr("Not now")
                primary: false
                onClicked: root.dismissed()
            }

            AiButton {
                Layout.preferredWidth: 178
                text: qsTr("Set up model")
                primary: true
                onClicked: root.openSettingsRequested()
            }
        }
    }

    component AiButton: Button {
        property bool primary: false

        Layout.preferredHeight: 48
        font.pixelSize: 15
        font.weight: 800
        hoverEnabled: true
        contentItem: Label {
            text: parent.text
            horizontalAlignment: Text.AlignHCenter
            verticalAlignment: Text.AlignVCenter
            color: root.textColor
            font.pixelSize: parent.font.pixelSize
            font.weight: parent.font.weight
            elide: Text.ElideRight
        }
        background: Rectangle {
            radius: 10
            color: parent.primary
                   ? (parent.down
                      ? Qt.darker(root.accentColor, 1.16)
                      : (parent.hovered ? Qt.lighter(root.accentColor, 1.06)
                                        : root.accentColor))
                   : (parent.down
                      ? Qt.rgba(1, 1, 1, 0.07)
                      : (parent.hovered ? Qt.rgba(1, 1, 1, 0.14)
                                        : Qt.rgba(1, 1, 1, 0.10)))
            border.width: 1
            border.color: parent.primary
                          ? Qt.rgba(root.accentColor.r, root.accentColor.g, root.accentColor.b, 0.20)
                          : Qt.rgba(1, 1, 1, 0.10)
            opacity: parent.enabled ? 1.0 : 0.45
        }
    }
}

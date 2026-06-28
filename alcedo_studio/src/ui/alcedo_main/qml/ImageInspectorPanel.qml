import QtQuick
import QtQuick.Controls
import QtQuick.Layouts

// Image inspector page (Frontend 3 placeholder).
//
// Frontend 3 only ships the shell + this placeholder. The empty state is shown
// until a focused image is available; the focused state previews the six-tile
// dashboard layout (Camera / Lens / Aperture·Shutter / ISO / Description /
// Rating) that Frontend 4 fills with real, editable data.
//
// `focusedImage` is a Frontend 3 stand-in bound from InspectorPanel (which in
// turn is bound to Main.qml's pendingDetailsTarget). Frontend 4 will replace
// this with true thumbnail-focus tracking and the compact focused-image DTO.
Item {
    id: root

    property var focusedImage: ({})

    readonly property color textColor: appTheme.textColor
    readonly property color mutedTextColor: appTheme.textMutedColor
    readonly property color accentColor: appTheme.accentColor

    readonly property bool hasFocus: Number(focusedImage && focusedImage.imageId) > 0

    function withAlpha(color, alpha) {
        return Qt.rgba(color.r, color.g, color.b, alpha)
    }

    // Nested inline component (matches the Main.qml CaptionButton pattern;
    // file-level inline components are rejected by this qmlcachegen).
    // Self-contained placeholder tile. Frontend 4 replaces this with the real
    // editable tiles.
    component ImagePlaceholderTile: Rectangle {
        id: tile
        property string tileLabel: ""
        property bool tall: false
        Layout.fillWidth: true
        Layout.preferredHeight: tile.tall ? 112 : 84
        radius: 8
        color: Qt.rgba(appTheme.bgBaseColor.r, appTheme.bgBaseColor.g, appTheme.bgBaseColor.b, 0.62)
        border.width: 1
        border.color: Qt.rgba(appTheme.glassStrokeColor.r, appTheme.glassStrokeColor.g, appTheme.glassStrokeColor.b, 0.36)

        ColumnLayout {
            anchors.fill: parent
            anchors.margins: 12
            spacing: 8

            Label {
                text: tile.tileLabel
                color: appTheme.textMutedColor
                font.pixelSize: 10
                font.weight: 700
                font.letterSpacing: 1.4
            }

            Label {
                Layout.fillWidth: true
                text: "—"
                color: Qt.rgba(appTheme.textMutedColor.r, appTheme.textMutedColor.g, appTheme.textMutedColor.b, 0.5)
                font.family: appTheme.dataFontFamily
                font.pixelSize: 22
                font.weight: 300
            }

            Item { Layout.fillHeight: true }
        }
    }

    // ── Empty state ──
    ColumnLayout {
        anchors.fill: parent
        visible: !root.hasFocus
        spacing: 12

        Item { Layout.fillHeight: true }

        Button {
            Layout.alignment: Qt.AlignHCenter
            Layout.preferredWidth: 44
            Layout.preferredHeight: 44
            flat: true
            display: AbstractButton.IconOnly
            hoverEnabled: false
            focusPolicy: Qt.NoFocus
            icon.source: "qrc:/panel_icons/image.svg"
            icon.width: 40
            icon.height: 40
            icon.color: root.withAlpha(root.mutedTextColor, 0.45)
            background: Item {}
        }

        Label {
            Layout.alignment: Qt.AlignHCenter
            text: qsTr("No Image Focused")
            color: root.textColor
            font.family: appTheme.headlineFontFamily
            font.pixelSize: 16
            font.weight: 600
        }

        Label {
            Layout.alignment: Qt.AlignHCenter
            Layout.maximumWidth: 240
            horizontalAlignment: Text.AlignHCenter
            wrapMode: Text.WordWrap
            text: qsTr("Focus a photo to inspect its camera, lens, exposure, description, and rating here.")
            color: root.mutedTextColor
            font.family: appTheme.uiFontFamily
            font.pixelSize: 12
        }

        Item { Layout.fillHeight: true }
    }

    // ── Focused state placeholder ──
    ScrollView {
        id: focusedScroll
        anchors.fill: parent
        visible: root.hasFocus
        contentWidth: availableWidth
        clip: true

        ColumnLayout {
            width: focusedScroll.availableWidth
            spacing: 16

            // Title
            ColumnLayout {
                Layout.leftMargin: 16
                Layout.rightMargin: 16
                Layout.topMargin: 18
                spacing: 4

                Label {
                    text: qsTr("IMAGE")
                    color: root.mutedTextColor
                    font.pixelSize: 10
                    font.weight: 700
                    font.letterSpacing: 1.8
                }

                Label {
                    Layout.fillWidth: true
                    text: root.focusedImage && root.focusedImage.fileName
                          ? root.focusedImage.fileName : qsTr("(unnamed)")
                    color: root.textColor
                    font.family: appTheme.uiFontFamily
                    font.pixelSize: 15
                    font.weight: 600
                    elide: Text.ElideRight
                }
            }

            // Responsive tile grid. Wide inspector → 2 columns, narrow → 1.
            // Description and Rating get taller minimum heights than the four
            // metric tiles, matching the Frontend 4 layout spec.
            GridLayout {
                Layout.leftMargin: 16
                Layout.rightMargin: 16
                Layout.bottomMargin: 18
                Layout.fillWidth: true
                columns: focusedScroll.availableWidth > 280 ? 2 : 1
                rowSpacing: 12
                columnSpacing: 12

                ImagePlaceholderTile { tileLabel: qsTr("Camera") }
                ImagePlaceholderTile { tileLabel: qsTr("Lens") }
                ImagePlaceholderTile { tileLabel: qsTr("Aperture / Shutter") }
                ImagePlaceholderTile { tileLabel: qsTr("ISO") }
                ImagePlaceholderTile { tileLabel: qsTr("Description"); tall: true }
                ImagePlaceholderTile { tileLabel: qsTr("Rating"); tall: true }
            }
        }
    }
}

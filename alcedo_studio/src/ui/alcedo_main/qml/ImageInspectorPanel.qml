import QtQuick
import QtQuick.Controls
import QtQuick.Layouts

Item {
    id: root

    property var focusedImage: ({})
    signal ratingRequested(int rating)
    signal descriptionSaveRequested(string caption)
    signal ratingReasonSaveRequested(string reasons)

    readonly property color textColor: appTheme.textColor
    readonly property color mutedTextColor: appTheme.textMutedColor
    readonly property color accentColor: appTheme.accentColor
    readonly property bool hasFocus: focusedImage && focusedImage.success === true
    readonly property string descriptionText: focusedImage && focusedImage.description
                                              ? String(focusedImage.description) : ""
    readonly property string ratingReasonText: focusedImage && focusedImage.ratingReason
                                               ? String(focusedImage.ratingReason) : ""
    readonly property int currentRating: Math.max(0, Math.min(5, Number(
                                      focusedImage && focusedImage.rating ? focusedImage.rating : 0)))

    property bool editingDescription: false
    property bool editingReason: false
    property string draftDescription: ""
    property string draftReason: ""

    function withAlpha(color, alpha) {
        return Qt.rgba(color.r, color.g, color.b, alpha)
    }

    function tileFor(tileId) {
        const tiles = focusedImage && focusedImage.tiles ? focusedImage.tiles : []
        for (let i = 0; i < tiles.length; ++i) {
            if (tiles[i] && String(tiles[i].id) === tileId) {
                return tiles[i]
            }
        }
        return ({ label: "", value: "", detail: "" })
    }

    function resetDrafts() {
        editingDescription = false
        editingReason = false
        draftDescription = descriptionText
        draftReason = ratingReasonText
    }

    function starText(index) {
        return index <= currentRating ? "\u2605" : "\u2606"
    }

    function toggleDescriptionEdit() {
        if (editingDescription) {
            editingDescription = false
            descriptionSaveRequested(draftDescription)
            return
        }
        draftDescription = descriptionText
        editingDescription = true
    }

    function toggleReasonEdit() {
        if (editingReason) {
            editingReason = false
            ratingReasonSaveRequested(draftReason)
            return
        }
        draftReason = ratingReasonText
        editingReason = true
    }

    onFocusedImageChanged: resetDrafts()

    component InspectorTile: Rectangle {
        id: tile
        property string label: ""
        property bool tall: false
        property bool editable: false
        property bool editing: false
        property string editToolTip: ""
        signal editToggled()
        default property alias content: contentColumn.data

        Layout.fillWidth: true
        Layout.preferredHeight: Math.max(tall ? 178 : 104, contentColumn.implicitHeight + 24)
        Layout.minimumHeight: tall ? 160 : 96
        radius: 8
        color: Qt.rgba(appTheme.bgBaseColor.r, appTheme.bgBaseColor.g, appTheme.bgBaseColor.b, 0.62)
        border.width: 0
        clip: true

        ColumnLayout {
            id: contentColumn
            anchors.fill: parent
            anchors.leftMargin: 12
            anchors.topMargin: 12
            anchors.rightMargin: tile.editable ? 42 : 12
            anchors.bottomMargin: 12
            spacing: 8

            Label {
                Layout.fillWidth: true
                text: tile.label
                color: appTheme.textMutedColor
                font.pixelSize: 10
                font.weight: 700
                font.letterSpacing: 1.2
                elide: Text.ElideRight
            }
        }

        Button {
            id: editButton
            visible: tile.editable
            anchors.top: parent.top
            anchors.right: parent.right
            anchors.margins: 8
            width: 28
            height: 28
            padding: 6
            display: AbstractButton.IconOnly
            focusPolicy: Qt.NoFocus
            hoverEnabled: true
            icon.source: "qrc:/panel_icons/edit.svg"
            icon.width: 15
            icon.height: 15
            icon.color: tile.editing ? root.accentColor : root.mutedTextColor
            ToolTip.visible: hovered && tile.editToolTip.length > 0
            ToolTip.text: tile.editToolTip
            background: Rectangle {
                radius: 6
                color: tile.editing
                       ? root.withAlpha(root.accentColor, editButton.down ? 0.24 : 0.14)
                       : root.withAlpha(root.mutedTextColor,
                                        editButton.down ? 0.14 : (editButton.hovered ? 0.08 : 0.0))
            }
            onClicked: tile.editToggled()
        }
    }

    component TileValue: Label {
        Layout.fillWidth: true
        color: appTheme.textColor
        font.family: appTheme.dataFontFamily
        font.pixelSize: 18
        font.weight: 500
        wrapMode: Text.WordWrap
    }

    component TileDetail: Label {
        Layout.fillWidth: true
        color: appTheme.textMutedColor
        font.family: appTheme.uiFontFamily
        font.pixelSize: 11
        wrapMode: Text.WordWrap
    }

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

    ScrollView {
        id: focusedScroll
        anchors.fill: parent
        visible: root.hasFocus
        contentWidth: availableWidth
        clip: true

        ColumnLayout {
            width: focusedScroll.availableWidth
            spacing: 16

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

                RowLayout {
                    Layout.fillWidth: true
                    spacing: 10

                    Label {
                        Layout.fillWidth: true
                        text: focusedImage && focusedImage.title ? focusedImage.title : qsTr("(unnamed)")
                        color: root.textColor
                        font.family: appTheme.uiFontFamily
                        font.pixelSize: 15
                        font.weight: 600
                        elide: Text.ElideMiddle
                    }

                    Label {
                        text: focusedImage && focusedImage.capturedAt ? focusedImage.capturedAt : ""
                        visible: text.length > 0 && text !== "\u2014"
                        color: root.mutedTextColor
                        font.pixelSize: 11
                        horizontalAlignment: Text.AlignRight
                        elide: Text.ElideRight
                    }
                }
            }

            GridLayout {
                Layout.leftMargin: 16
                Layout.rightMargin: 16
                Layout.bottomMargin: 18
                Layout.fillWidth: true
                columns: focusedScroll.availableWidth > 300 ? 2 : 1
                rowSpacing: 12
                columnSpacing: 12

                InspectorTile {
                    label: root.tileFor("camera").label
                    TileValue { text: root.tileFor("camera").value }
                    TileDetail { text: root.tileFor("camera").detail }
                    Item { Layout.fillHeight: true }
                }

                InspectorTile {
                    label: root.tileFor("lens").label
                    TileValue { text: root.tileFor("lens").value }
                    TileDetail { text: root.tileFor("lens").detail }
                    Item { Layout.fillHeight: true }
                }

                InspectorTile {
                    label: root.tileFor("exposure").label
                    TileValue { text: root.tileFor("exposure").value }
                    Item { Layout.fillHeight: true }
                }

                InspectorTile {
                    label: root.tileFor("iso").label
                    TileValue { text: root.tileFor("iso").value }
                    Item { Layout.fillHeight: true }
                }

                InspectorTile {
                    label: root.tileFor("description").label
                    tall: true
                    editable: true
                    editing: root.editingDescription
                    editToolTip: root.editingDescription ? qsTr("Save description") : qsTr("Edit description")
                    onEditToggled: root.toggleDescriptionEdit()

                    TextArea {
                        visible: root.editingDescription
                        Layout.fillWidth: true
                        Layout.fillHeight: true
                        text: root.draftDescription
                        wrapMode: TextEdit.WordWrap
                        selectByMouse: true
                        color: root.textColor
                        font.pixelSize: 12
                        background: Rectangle {
                            radius: 6
                            color: root.withAlpha(root.textColor, 0.05)
                            border.width: 1
                            border.color: root.withAlpha(root.textColor, 0.10)
                        }
                        onTextChanged: root.draftDescription = text
                    }

                    Label {
                        visible: !root.editingDescription
                        Layout.fillWidth: true
                        Layout.fillHeight: true
                        text: root.tileFor("description").value
                        color: root.descriptionText.length > 0 ? root.textColor : root.mutedTextColor
                        font.pixelSize: 12
                        wrapMode: Text.WordWrap
                    }
                }

                InspectorTile {
                    label: root.tileFor("rating").label
                    tall: true
                    editable: true
                    editing: root.editingReason
                    editToolTip: root.editingReason ? qsTr("Save rating reason") : qsTr("Edit rating reason")
                    onEditToggled: root.toggleReasonEdit()

                    RowLayout {
                        Layout.fillWidth: true
                        spacing: 2

                        Repeater {
                            model: 5
                            delegate: Button {
                                id: starButton
                                Layout.preferredWidth: 30
                                Layout.preferredHeight: 30
                                padding: 0
                                text: root.starText(index + 1)
                                font.pixelSize: 18
                                font.family: appTheme.dataFontFamily
                                background: Item {}
                                contentItem: Label {
                                    text: starButton.text
                                    color: index < root.currentRating ? root.accentColor : root.mutedTextColor
                                    horizontalAlignment: Text.AlignHCenter
                                    verticalAlignment: Text.AlignVCenter
                                    font: starButton.font
                                }
                                onClicked: root.ratingRequested(index + 1)
                            }
                        }

                        Button {
                            id: clearRatingButton
                            text: qsTr("Clear")
                            Layout.preferredHeight: 26
                            padding: 8
                            font.pixelSize: 11
                            font.weight: 700
                            background: Rectangle {
                                radius: 6
                                color: root.withAlpha(root.mutedTextColor,
                                                     clearRatingButton.down ? 0.14 : 0.06)
                            }
                            contentItem: Label {
                                text: clearRatingButton.text
                                color: root.mutedTextColor
                                font: clearRatingButton.font
                                horizontalAlignment: Text.AlignHCenter
                                verticalAlignment: Text.AlignVCenter
                            }
                            onClicked: root.ratingRequested(0)
                        }
                    }

                    TextArea {
                        visible: root.editingReason
                        Layout.fillWidth: true
                        Layout.fillHeight: true
                        text: root.draftReason
                        wrapMode: TextEdit.WordWrap
                        selectByMouse: true
                        color: root.textColor
                        font.pixelSize: 12
                        background: Rectangle {
                            radius: 6
                            color: root.withAlpha(root.textColor, 0.05)
                            border.width: 1
                            border.color: root.withAlpha(root.textColor, 0.10)
                        }
                        onTextChanged: root.draftReason = text
                    }

                    Label {
                        visible: !root.editingReason
                        Layout.fillWidth: true
                        Layout.fillHeight: true
                        text: root.tileFor("rating").detail
                        color: root.ratingReasonText.length > 0 ? root.textColor : root.mutedTextColor
                        font.pixelSize: 12
                        wrapMode: Text.WordWrap
                    }
                }
            }
        }
    }
}

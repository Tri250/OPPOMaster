import QtQuick
import QtQuick.Controls
import QtQuick.Layouts

Item {
    id: root

    property var shortcutDefinitions: null
    property var shortcutRegistry: null

    readonly property color panelColor: appTheme.bgPanelColor
    readonly property color cardColor: appTheme.bgBaseColor
    readonly property color textColor: appTheme.textColor
    readonly property color mutedTextColor: appTheme.textMutedColor
    readonly property color accentColor: appTheme.accentColor
    readonly property color dangerColor: appTheme.dangerColor
    readonly property string dataFontFamily: appTheme.dataFontFamily
    readonly property string headlineFontFamily: appTheme.headlineFontFamily

    property var categories: []
    property var shortcutsByCategory: ({})
    property string recordingShortcutId: ""
    property string conflictWarning: ""
    property string searchFilter: ""

    signal shortcutChanged(string shortcutId, string newKey)

    function loadShortcuts() {
        if (!shortcutDefinitions) return
        var cats = shortcutDefinitions.GetCategories()
        var map = {}
        for (var i = 0; i < cats.length; i++) {
            map[cats[i]] = shortcutDefinitions.GetShortcutsByCategory(cats[i])
        }
        root.categories = cats
        root.shortcutsByCategory = map
    }

    function resetToDefaults() {
        if (shortcutDefinitions) {
            shortcutDefinitions.ResetAllToDefaults()
            loadShortcuts()
        }
    }

    function startRecording(shortcutId) {
        root.recordingShortcutId = shortcutId
        root.conflictWarning = ""
        if (shortcutRegistry) {
            shortcutRegistry.StartRecording(shortcutId)
        }
    }

    function stopRecording() {
        root.recordingShortcutId = ""
        if (shortcutRegistry) {
            shortcutRegistry.StopRecording()
        }
    }

    Component.onCompleted: loadShortcuts()

    implicitWidth: 540
    implicitHeight: 520

    ColumnLayout {
        anchors.fill: parent
        spacing: 12

        // Search bar
        RowLayout {
            Layout.fillWidth: true
            spacing: 8

            TextField {
                id: searchField
                Layout.fillWidth: true
                placeholderText: qsTr("Search shortcuts...")
                font.family: root.dataFontFamily
                font.pixelSize: 12
                onTextChanged: root.searchFilter = text.toLowerCase()
            }

            Button {
                text: qsTr("Reset All")
                onClicked: resetToDefaults()
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
        }

        // Conflict warning
        Rectangle {
            Layout.fillWidth: true
            visible: root.conflictWarning.length > 0
            radius: 6
            color: Qt.rgba(root.dangerColor.r, root.dangerColor.g, root.dangerColor.b, 0.12)
            border.width: 1
            border.color: Qt.rgba(root.dangerColor.r, root.dangerColor.g, root.dangerColor.b, 0.3)
            implicitHeight: conflictLabel.implicitHeight + 16

            Label {
                id: conflictLabel
                anchors.fill: parent
                anchors.margins: 8
                text: root.conflictWarning
                color: root.dangerColor
                font.pixelSize: 11
                wrapMode: Text.WordWrap
            }
        }

        // Shortcut list by category
        ScrollView {
            Layout.fillWidth: true
            Layout.fillHeight: true
            clip: true

            ListView {
                id: categoryList
                model: root.categories
                spacing: 6

                delegate: ColumnLayout {
                    width: categoryList.width
                    spacing: 4

                    readonly property string category: modelData

                    Label {
                        Layout.fillWidth: true
                        text: category
                        font.family: root.headlineFontFamily
                        font.pixelSize: 14
                        font.weight: Font.DemiBold
                        color: root.accentColor
                        leftPadding: 4
                    }

                    Repeater {
                        model: {
                            var shortcuts = root.shortcutsByCategory[category] || []
                            if (root.searchFilter.length === 0) return shortcuts
                            return shortcuts.filter(function(s) {
                                return s.label.toLowerCase().indexOf(root.searchFilter) >= 0
                                    || s.keySequence.toLowerCase().indexOf(root.searchFilter) >= 0
                            })
                        }

                        delegate: Rectangle {
                            Layout.fillWidth: true
                            height: 40
                            radius: 6
                            color: shortcutMouseArea.containsMouse ? Qt.rgba(1, 1, 1, 0.03) : "transparent"

                            readonly property var shortcut: modelData
                            readonly property bool isRecording: root.recordingShortcutId === shortcut.id

                            RowLayout {
                                anchors.fill: parent
                                anchors.leftMargin: 12
                                anchors.rightMargin: 12
                                spacing: 10

                                Label {
                                    Layout.fillWidth: true
                                    text: shortcut.label
                                    color: root.textColor
                                    font.pixelSize: 12
                                    elide: Text.ElideRight
                                }

                                // Shortcut key display / recording
                                Rectangle {
                                    implicitWidth: keyLabel.implicitWidth + 20
                                    implicitHeight: 26
                                    radius: 5
                                    color: isRecording
                                           ? Qt.rgba(root.accentColor.r, root.accentColor.g, root.accentColor.b, 0.2)
                                           : root.cardColor
                                    border.width: 1
                                    border.color: isRecording
                                                  ? root.accentColor
                                                  : Qt.rgba(root.textColor.r, root.textColor.g, root.textColor.b, 0.1)

                                    Label {
                                        id: keyLabel
                                        anchors.centerIn: parent
                                        text: isRecording
                                              ? qsTr("Press keys...")
                                              : (shortcut.keySequence.length > 0 ? shortcut.keySequence : qsTr("None"))
                                        font.family: root.dataFontFamily
                                        font.pixelSize: 11
                                        font.weight: Font.DemiBold
                                        color: isRecording ? root.accentColor : root.textColor
                                    }

                                    SequentialAnimation on border.color {
                                        running: isRecording
                                        loops: Animation.Infinite
                                        ColorAnimation {
                                            from: root.accentColor
                                            to: Qt.rgba(root.accentColor.r, root.accentColor.g, root.accentColor.b, 0.3)
                                            duration: 600
                                        }
                                        ColorAnimation {
                                            from: Qt.rgba(root.accentColor.r, root.accentColor.g, root.accentColor.b, 0.3)
                                            to: root.accentColor
                                            duration: 600
                                        }
                                    }
                                }

                                Button {
                                    text: isRecording ? qsTr("Cancel") : qsTr("Change")
                                    onClicked: {
                                        if (isRecording) {
                                            root.stopRecording()
                                        } else {
                                            root.startRecording(shortcut.id)
                                        }
                                    }
                                    implicitHeight: 26
                                    font.pixelSize: 10
                                    Material.foreground: root.textColor
                                    background: Rectangle {
                                        radius: 5
                                        color: parent.down ? Qt.rgba(1, 1, 1, 0.06)
                                             : parent.hovered ? Qt.rgba(1, 1, 1, 0.12)
                                                              : Qt.rgba(1, 1, 1, 0.07)
                                        border.width: 1
                                        border.color: Qt.rgba(root.textColor.r, root.textColor.g, root.textColor.b, 0.1)
                                    }
                                }
                            }

                            MouseArea {
                                id: shortcutMouseArea
                                anchors.fill: parent
                                hoverEnabled: true
                                acceptedButtons: Qt.NoButton
                            }
                        }
                    }

                    // Separator
                    Rectangle {
                        Layout.fillWidth: true
                        Layout.topMargin: 4
                        Layout.preferredHeight: 1
                        color: Qt.rgba(1, 1, 1, 0.06)
                    }
                }
            }
        }
    }

    Connections {
        target: root.shortcutRegistry
        function onShortcutRecorded(shortcutId, keySequence) {
            if (shortcutId === root.recordingShortcutId) {
                // Check for conflicts
                if (root.shortcutDefinitions) {
                    var conflict = root.shortcutDefinitions.CheckConflict(shortcutId, keySequence)
                    if (conflict.length > 0) {
                        root.conflictWarning = qsTr("Shortcut '%1' is already used by '%2'. Click Change again to override.").arg(keySequence).arg(conflict)
                    } else {
                        root.conflictWarning = ""
                    }
                }
                root.shortcutChanged(shortcutId, keySequence)
                root.recordingShortcutId = ""
                root.loadShortcuts()
            }
        }
        function onRecordingCancelled() {
            root.recordingShortcutId = ""
            root.conflictWarning = ""
        }
    }
}

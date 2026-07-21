import QtQuick
import QtQuick.Controls
import QtQuick.Layouts
import QtQuick.Effects

Popup {
    id: root
    font.family: appTheme.uiFontFamily
    modal: true
    focus: true
    closePolicy: running ? Popup.NoAutoClose : (Popup.CloseOnEscape | Popup.CloseOnPressOutside)
    anchors.centerIn: parent
    width: Math.min(parent ? parent.width - 64 : 640, 640)
    height: Math.min(parent ? parent.height - 64 : 520, 520)
    padding: 0

    property var panoramaController: albumBackend.panoramaController
    property bool running: panoramaController ? panoramaController.running : false
    property real progress: panoramaController ? panoramaController.progress : 0
    property string stageText: panoramaController ? panoramaController.stageText : ""
    property bool hasResult: panoramaController ? panoramaController.hasResult : false
    property bool failed: panoramaController ? panoramaController.failed : false
    property string errorMessage: panoramaController ? panoramaController.errorMessage : ""
    property var result: panoramaController ? panoramaController.result : ({})
    property var selectedImageIds: []
    property Item backgroundSource: null

    // Animation state for dialog enter/exit
    property real animScale: 1.0
    property real animOpacity: 1.0
    property real animY: 0

    readonly property color modalColor: appTheme.bgPanelColor
    readonly property color accentColor: appTheme.accentColor
    readonly property color textColor: appTheme.textColor
    readonly property color mutedTextColor: appTheme.textMutedColor
    readonly property color dividerColor: appTheme.dividerColor
    readonly property color dangerColor: "#D96C75"

    signal stitchRequested(var imageIds, var config)
    signal cancelRequested()
    signal closeRequested()
    signal saveToLibraryRequested(string filePath)

    onAboutToShow: {
        animScale = 0.96
        animOpacity = 0
        animY = 18
        dialogEnterAnim.start()
    }

    Behavior on animScale { NumberAnimation { duration: 220; easing.type: Easing.OutCubic } }
    Behavior on animOpacity { NumberAnimation { duration: 220; easing.type: Easing.OutCubic } }
    Behavior on animY { NumberAnimation { duration: 220; easing.type: Easing.OutCubic } }

    ParallelAnimation {
        id: dialogEnterAnim
        NumberAnimation { target: root; property: "animScale"; to: 1.0; duration: 220; easing.type: Easing.OutCubic }
        NumberAnimation { target: root; property: "animOpacity"; to: 1.0; duration: 220; easing.type: Easing.OutCubic }
        NumberAnimation { target: root; property: "animY"; to: 0; duration: 220; easing.type: Easing.OutCubic }
    }

    ParallelAnimation {
        id: dialogExitAnim
        NumberAnimation { target: root; property: "animScale"; to: 0.97; duration: 160; easing.type: Easing.InCubic }
        NumberAnimation { target: root; property: "animOpacity"; to: 0; duration: 160; easing.type: Easing.InCubic }
        NumberAnimation { target: root; property: "animY"; to: 18; duration: 160; easing.type: Easing.InCubic }
        onFinished: root.close()
    }

    onRunningChanged: {
        if (!running && !hasResult && !failed) {
            root.close()
        }
    }

    onHasResultChanged: {
        if (hasResult) {
            root.open()
        }
    }

    onFailedChanged: {
        if (failed) {
            root.open()
        }
    }

    background: Rectangle {
        radius: 14
        color: root.modalColor
        border.width: 0
        opacity: root.animOpacity
        transform: [
            Scale { origin.x: root.width / 2; origin.y: root.height / 2; xScale: root.animScale; yScale: root.animScale },
            Translate { y: root.animY }
        ]

        MultiEffect {
            visible: root.backgroundSource !== null
            anchors.fill: parent
            source: root.backgroundSource
            blurEnabled: true
            blur: 0.6
            blurMax: 64
            saturation: -0.2
            brightness: -0.08
        }

        Rectangle {
            anchors.fill: parent
            radius: 14
            color: root.modalColor
        }
    }

    contentItem: ColumnLayout {
        spacing: 0

        // Header
        Rectangle {
            Layout.fillWidth: true
            Layout.preferredHeight: 60
            radius: 14
            color: "transparent"

            Label {
                anchors.fill: parent
                anchors.leftMargin: 24
                anchors.rightMargin: 24
                text: root.running ? qsTr("Stitching Panorama…")
                                   : (root.hasResult ? qsTr("Panorama Result")
                                                     : qsTr("Stitch to Panorama"))
                color: root.textColor
                font.pixelSize: 20
                font.weight: 700
                verticalAlignment: Text.AlignVCenter
            }
        }

        Rectangle {
            Layout.fillWidth: true
            Layout.preferredHeight: 1
            color: root.dividerColor
        }

        // Content
        ColumnLayout {
            Layout.fillWidth: true
            Layout.fillHeight: true
            Layout.leftMargin: 24
            Layout.rightMargin: 24
            Layout.topMargin: 20
            Layout.bottomMargin: 20
            spacing: 16

            // Stitching progress view
            ColumnLayout {
                Layout.fillWidth: true
                Layout.fillHeight: true
                visible: root.running
                spacing: 16

                Label {
                    Layout.fillWidth: true
                    text: root.stageText
                    color: root.textColor
                    font.pixelSize: 14
                    wrapMode: Text.WordWrap
                }

                ProgressBar {
                    Layout.fillWidth: true
                    Layout.preferredHeight: 8
                    from: 0
                    to: 1
                    value: root.progress

                    Behavior on value { NumberAnimation { duration: 300; easing.type: Easing.OutCubic } }

                    background: Rectangle {
                        radius: 4
                        color: Qt.rgba(1, 1, 1, 0.08)
                    }

                    contentItem: Item {
                        Rectangle {
                            width: root.progress * parent.width
                            height: parent.height
                            radius: 4
                            color: root.accentColor
                        }
                    }
                }

                Label {
                    Layout.fillWidth: true
                    text: qsTr("%1% complete").arg(Math.round(root.progress * 100))
                    color: root.mutedTextColor
                    font.pixelSize: 12
                }

                Item { Layout.fillHeight: true }

                Button {
                    Layout.alignment: Qt.AlignHCenter
                    text: qsTr("Cancel")
                    onClicked: {
                        if (root.panoramaController) {
                            root.panoramaController.cancel()
                        }
                        root.cancelRequested()
                    }
                }
            }

            // Result view
            ColumnLayout {
                Layout.fillWidth: true
                Layout.fillHeight: true
                visible: root.hasResult
                spacing: 12

                Label {
                    Layout.fillWidth: true
                    text: qsTr("Panorama created successfully!")
                    color: root.accentColor
                    font.pixelSize: 16
                    font.weight: 600
                }

                GridLayout {
                    Layout.fillWidth: true
                    columns: 2
                    columnSpacing: 16
                    rowSpacing: 8

                    Label {
                        text: qsTr("Dimensions:")
                        color: root.mutedTextColor
                        font.pixelSize: 13
                    }
                    Label {
                        text: "%1 × %2".arg(root.result.width || 0).arg(root.result.height || 0)
                        color: root.textColor
                        font.pixelSize: 13
                    }

                    Label {
                        text: qsTr("File:")
                        color: root.mutedTextColor
                        font.pixelSize: 13
                    }
                    Label {
                        Layout.fillWidth: true
                        text: root.result.filePath || ""
                        color: root.textColor
                        font.pixelSize: 13
                        elide: Text.ElideMiddle
                    }
                }

                Item { Layout.fillHeight: true }

                RowLayout {
                    Layout.fillWidth: true
                    spacing: 12

                    Item { Layout.fillWidth: true }

                    Button {
                        text: qsTr("Save to Library")
                        highlighted: true
                        visible: root.result && root.result.filePath && root.result.filePath.length > 0
                        onClicked: {
                            root.saveToLibraryRequested(root.result.filePath)
                        }
                        Material.foreground: root.textColor
                        background: Rectangle {
                            radius: 8
                            color: parent.down ? Qt.darker("#457B9D", 1.18)
                                 : parent.hovered ? Qt.lighter("#457B9D", 1.08)
                                                  : "#457B9D"
                        }
                    }

                    Button {
                        text: qsTr("Close")
                        onClicked: {
                            if (root.panoramaController) {
                                root.panoramaController.dismissResult()
                            }
                            dialogExitAnim.start()
                            root.closeRequested()
                        }
                    }
                }
            }

            // Error view
            ColumnLayout {
                Layout.fillWidth: true
                Layout.fillHeight: true
                visible: root.failed
                spacing: 12

                Label {
                    Layout.fillWidth: true
                    text: qsTr("Stitching failed")
                    color: root.dangerColor
                    font.pixelSize: 16
                    font.weight: 600
                }

                Label {
                    Layout.fillWidth: true
                    text: root.errorMessage
                    color: root.mutedTextColor
                    font.pixelSize: 13
                    wrapMode: Text.WordWrap
                }

                Item { Layout.fillHeight: true }

                RowLayout {
                    Layout.fillWidth: true
                    spacing: 12

                    Item { Layout.fillWidth: true }

                    Button {
                        text: qsTr("Close")
                        onClicked: {
                            if (root.panoramaController) {
                                root.panoramaController.dismissResult()
                            }
                            dialogExitAnim.start()
                            root.closeRequested()
                        }
                    }
                }
            }
        }
    }
}

import QtQuick
import QtQuick.Controls
import QtQuick.Layouts

ColumnLayout {
    id: root

    property var options: []
    property string currentCode: "normal"
    property bool useChineseLabels: false
    property color textColor: "#F5F1EA"
    property color mutedTextColor: "#B6B0A7"
    property color accentColor: "#457B9D"
    property color trackColor: Qt.rgba(1, 1, 1, 0.28)
    property color hoverColor: Qt.rgba(1, 1, 1, 0.10)
    property color dividerColor: Qt.rgba(1, 1, 1, 0.10)
    signal selected(string code)

    readonly property int optionCount: options ? options.length : 0
    readonly property real markerProgress: optionCount > 1 ? currentIndex / (optionCount - 1) : 0
    readonly property int currentIndex: {
        for (let i = 0; i < optionCount; ++i) {
            if (options[i].code === currentCode) {
                return i
            }
        }
        return optionCount > 1 ? 1 : 0
    }

    function codeAt(index) {
        if (index < 0 || index >= optionCount) {
            return ""
        }
        return options[index].code
    }

    function indexFromX(localX) {
        if (optionCount <= 0 || sliderSurface.trackWidth <= 0) {
            return -1
        }
        const x = Math.max(sliderSurface.trackLeft,
                           Math.min(sliderSurface.trackLeft + sliderSurface.trackWidth, localX))
        const raw = Math.round((x - sliderSurface.trackLeft) / sliderSurface.trackWidth
                               * Math.max(1, optionCount - 1))
        return Math.max(0, Math.min(optionCount - 1, raw))
    }

    function commitIndex(index) {
        const code = codeAt(index)
        if (code.length > 0 && code !== currentCode) {
            selected(code)
        }
    }

    function selectedColorAt(index) {
        if (index >= 0 && index < optionCount && options[index].selectedColor) {
            return options[index].selectedColor
        }
        return accentColor
    }

    function selectedQtColor(index) {
        return selectedColorAt(index)
    }

    spacing: 0
    enabled: optionCount > 0

    Item {
        id: sliderSurface
        Layout.fillWidth: true
        Layout.preferredHeight: 70
        opacity: root.enabled ? 1.0 : 0.45

        readonly property real edgePadding: Math.max(38, width * 0.06)
        readonly property real trackLeft: edgePadding
        readonly property real trackWidth: Math.max(1, width - edgePadding * 2)
        readonly property real trackY: 22
        readonly property real labelY: 36
        readonly property real markerX: trackLeft + trackWidth * root.markerProgress

        Rectangle {
            x: sliderSurface.trackLeft
            y: sliderSurface.trackY
            width: sliderSurface.trackWidth
            height: 2
            radius: 1
            color: root.trackColor
        }

        Rectangle {
            x: sliderSurface.trackLeft
            y: sliderSurface.trackY
            width: Math.max(0, sliderSurface.markerX - sliderSurface.trackLeft)
            height: 2
            radius: 1
            color: root.selectedQtColor(root.currentIndex)
            Behavior on width { NumberAnimation { duration: 110; easing.type: Easing.OutCubic } }
        }

        Canvas {
            id: marker
            width: 14
            height: 12
            x: sliderSurface.markerX - width / 2
            y: sliderSurface.trackY - height + 1
            opacity: root.enabled ? 1.0 : 0.60

            property color markerColor: root.selectedQtColor(root.currentIndex)

            onMarkerColorChanged: requestPaint()
            onPaint: {
                const ctx = getContext("2d")
                ctx.clearRect(0, 0, width, height)
                ctx.fillStyle = markerColor.toString()
                ctx.beginPath()
                ctx.moveTo(width / 2, 0)
                ctx.lineTo(width, height)
                ctx.lineTo(0, height)
                ctx.closePath()
                ctx.fill()
            }
            Behavior on x { NumberAnimation { duration: 110; easing.type: Easing.OutCubic } }
        }

        Repeater {
            model: root.options
            delegate: Rectangle {
                x: sliderSurface.trackLeft
                   + (root.optionCount > 1 ? index / (root.optionCount - 1) * sliderSurface.trackWidth : 0)
                y: sliderSurface.trackY - 5
                width: 1
                height: 12
                color: index === root.currentIndex ? root.selectedColorAt(index) : root.dividerColor
                opacity: index === root.currentIndex ? 1.0 : 0.55
            }
        }

        Repeater {
            model: root.options
            delegate: Label {
                readonly property real tickX: sliderSurface.trackLeft
                                            + (root.optionCount > 1
                                               ? index / (root.optionCount - 1) * sliderSurface.trackWidth : 0)
                x: Math.max(0, Math.min(sliderSurface.width - width, tickX - width / 2))
                y: sliderSurface.labelY
                width: Math.min(128, Math.max(56, implicitWidth + 10))
                height: 28
                text: root.useChineseLabels ? modelData.zh : modelData.en
                color: root.currentIndex === index ? root.selectedColorAt(index) : root.textColor
                font.pixelSize: 15
                font.weight: root.currentIndex === index ? 900 : 700
                horizontalAlignment: Text.AlignHCenter
                verticalAlignment: Text.AlignVCenter
                elide: Text.ElideRight
                leftPadding: 4
                rightPadding: 4
            }
        }

        MouseArea {
            id: dragArea
            anchors.fill: parent
            enabled: root.enabled
            cursorShape: Qt.PointingHandCursor
            hoverEnabled: true
            preventStealing: true
            onPressed: root.commitIndex(root.indexFromX(mouse.x))
            onPositionChanged: {
                if (pressed) {
                    root.commitIndex(root.indexFromX(mouse.x))
                }
            }
        }
    }
}

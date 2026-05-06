import QtQuick
import QtQuick.Controls
import QtQuick.Effects
import QtQuick.Layouts

Dialog {
    id: root
    font.family: appTheme.uiFontFamily
    modal: true
    focus: true
    width: Math.min(parent ? parent.width - 44 : 760, 760)
    height: Math.min(parent ? parent.height - 48 : 720, 720)
    x: parent ? Math.round((parent.width - width) / 2) : 0
    y: parent ? Math.round((parent.height - height) / 2) : 0
    padding: 0
    closePolicy: Popup.CloseOnEscape | Popup.CloseOnPressOutside

    property string titleText: ""
    property string subtitleText: ""
    property var detailRows: []
    property Item blurSource: null
    property real cornerRadius: 0
    signal rowActionRequested(string actionId, string actionValue)

    readonly property color overlayColor: appTheme.bgDeepColor
    readonly property color panelColor: appTheme.toneGraphite
    readonly property color sectionColor: appTheme.bgBaseColor
    readonly property color summaryCardColor: Qt.rgba(1, 1, 1, 0.04)
    readonly property color summaryCardBorder: Qt.rgba(1, 1, 1, 0.06)
    readonly property color textColor: appTheme.textColor
    readonly property color mutedTextColor: appTheme.textMutedColor
    readonly property color accentColor: appTheme.accentColor
    readonly property color actionBgColor: Qt.rgba(accentColor.r, accentColor.g, accentColor.b, 0.12)
    readonly property color actionHoverBgColor: Qt.rgba(accentColor.r, accentColor.g, accentColor.b, 0.2)
    readonly property color actionBorderColor: Qt.rgba(accentColor.r, accentColor.g, accentColor.b, 0.34)
    readonly property string dataFontFamily: appTheme.dataFontFamily

    readonly property var sectionGroups: {
        const groups = []
        let current = null
        for (let i = 0; i < detailRows.length; ++i) {
            const row = detailRows[i]
            if (!current || current.name !== row.section) {
                current = { name: row.section, rows: [] }
                groups.push(current)
            }
            current.rows.push(row)
        }
        return groups
    }

    readonly property var summaryCards: {
        const cards = []
        const buckets = {}
        for (let i = 0; i < detailRows.length; ++i) {
            const row = detailRows[i]
            if (!buckets[row.section]) buckets[row.section] = []
            buckets[row.section].push(row)
        }
        const orderedKeys = []
        for (let i = 0; i < detailRows.length; ++i) {
            const sec = detailRows[i].section
            if (orderedKeys.indexOf(sec) === -1) orderedKeys.push(sec)
        }
        const capture = orderedKeys[0] ? buckets[orderedKeys[0]] : []
        const gear = orderedKeys[1] ? buckets[orderedKeys[1]] : []
        const exposure = orderedKeys[2] ? buckets[orderedKeys[2]] : []

        function pickEmphasized(rows, fallbackIndex) {
            for (let i = 0; i < rows.length; ++i) {
                if (rows[i].emphasized) return rows[i]
            }
            return rows[fallbackIndex]
        }
        function trimDate(value) {
            if (!value) return value
            const s = String(value).trim()
            const m = s.match(/^(\d{4}[-\/]\d{2}[-\/]\d{2})/)
            return m ? m[1].replace(/\//g, "-") : s
        }

        if (capture[0]) cards.push({ label: capture[0].label, value: capture[0].value, mono: true })
        if (capture[1]) cards.push({ label: capture[1].label, value: capture[1].value, mono: true })
        if (capture[2]) cards.push({ label: capture[2].label, value: trimDate(capture[2].value), mono: true })

        const camera = pickEmphasized(gear, 1)
        if (camera) cards.push({ label: camera.label, value: camera.value, mono: false })

        const focal = exposure[3] || exposure[0]
        if (focal) cards.push({ label: focal.label, value: focal.value, mono: true })

        if (exposure[0] && exposure[1]) {
            cards.push({
                label: exposure[0].label + " · " + exposure[1].label,
                value: exposure[0].value + " · " + exposure[1].value,
                mono: true
            })
        }
        return cards
    }

    Overlay.modal: Item {
        anchors.fill: parent

        Rectangle {
            id: backdropMask
            anchors.fill: parent
            radius: root.cornerRadius
            color: "white"
            visible: false
            layer.enabled: true
            layer.smooth: true
        }

        Item {
            id: maskedBackdrop
            anchors.fill: parent
            layer.enabled: true
            layer.smooth: true
            layer.effect: MultiEffect {
                maskEnabled: root.cornerRadius > 0
                maskSource: backdropMask
            }

            MultiEffect {
                anchors.fill: parent
                source: root.blurSource
                blurEnabled: root.blurSource !== null
                blur: 0.72
                blurMax: 72
                saturation: -0.24
                brightness: -0.08
            }

            Rectangle {
                anchors.fill: parent
                color: root.overlayColor
                opacity: root.blurSource !== null ? 0.55 : 0.9
            }
        }
    }

    background: Rectangle {
        radius: 14
        color: root.panelColor
        border.width: 0
        layer.enabled: true
    }

    contentItem: ColumnLayout {
        anchors.fill: parent
        anchors.margins: 24
        spacing: 18

        ColumnLayout {
            Layout.fillWidth: true
            spacing: 4

            Label {
                Layout.fillWidth: true
                text: root.titleText
                color: root.textColor
                font.pixelSize: 24
                font.weight: 600
                font.letterSpacing: -0.3
                elide: Text.ElideMiddle
            }

            Label {
                Layout.fillWidth: true
                visible: text.length > 0
                text: root.subtitleText
                color: root.mutedTextColor
                font.pixelSize: 12
                wrapMode: Text.WordWrap
            }
        }

        GridLayout {
            id: summaryGrid
            Layout.fillWidth: true
            visible: root.summaryCards.length > 0
            columns: 3
            rowSpacing: 8
            columnSpacing: 8

            Repeater {
                model: root.summaryCards

                delegate: Rectangle {
                    Layout.fillWidth: true
                    Layout.preferredHeight: 72
                    radius: 10
                    color: root.summaryCardColor
                    border.width: 1
                    border.color: root.summaryCardBorder

                    ColumnLayout {
                        anchors.fill: parent
                        anchors.leftMargin: 14
                        anchors.rightMargin: 14
                        anchors.topMargin: 12
                        anchors.bottomMargin: 12
                        spacing: 4

                        Label {
                            Layout.fillWidth: true
                            text: modelData.label
                            color: root.mutedTextColor
                            font.pixelSize: 10
                            font.weight: 500
                            font.letterSpacing: 1.2
                            font.capitalization: Font.AllUppercase
                            elide: Text.ElideRight
                        }

                        Label {
                            Layout.fillWidth: true
                            Layout.alignment: Qt.AlignVCenter
                            text: modelData.value
                            color: root.textColor
                            font.family: modelData.mono ? root.dataFontFamily : root.font.family
                            font.pixelSize: 18
                            font.weight: 600
                            elide: Text.ElideRight
                        }
                    }
                }
            }
        }

        ScrollView {
            id: detailsScroll
            Layout.fillWidth: true
            Layout.fillHeight: true
            clip: true
            contentWidth: availableWidth

            ColumnLayout {
                width: detailsScroll.availableWidth
                spacing: 12

                Repeater {
                    model: root.sectionGroups

                    delegate: Rectangle {
                        Layout.fillWidth: true
                        Layout.preferredHeight: sectionContent.implicitHeight + 32
                        radius: 12
                        color: root.sectionColor
                        border.width: 0

                        ColumnLayout {
                            id: sectionContent
                            anchors.left: parent.left
                            anchors.right: parent.right
                            anchors.top: parent.top
                            anchors.leftMargin: 18
                            anchors.rightMargin: 18
                            anchors.topMargin: 16
                            spacing: 10

                            Label {
                                Layout.fillWidth: true
                                Layout.bottomMargin: 2
                                text: modelData.name
                                color: root.accentColor
                                font.pixelSize: 10
                                font.weight: 700
                                font.letterSpacing: 1.6
                                font.capitalization: Font.AllUppercase
                            }

                            Repeater {
                                model: modelData.rows

                                delegate: RowLayout {
                                    Layout.fillWidth: true
                                    spacing: 16

                                    readonly property bool hasAction:
                                        typeof modelData.actionId === "string"
                                        && modelData.actionId.length > 0
                                        && typeof modelData.actionValue === "string"
                                        && modelData.actionValue.length > 0
                                    readonly property bool isMonoValue: {
                                        const v = String(modelData.value || "")
                                        return /[\d×\/]/.test(v) && !/^—$/.test(v)
                                    }

                                    Label {
                                        Layout.preferredWidth: Math.min(170, detailsScroll.availableWidth * 0.3)
                                        Layout.maximumWidth: 170
                                        Layout.alignment: Qt.AlignTop
                                        text: modelData.label
                                        color: root.mutedTextColor
                                        font.pixelSize: 11
                                        font.weight: 500
                                        wrapMode: Text.WordWrap
                                    }

                                    Label {
                                        Layout.fillWidth: true
                                        Layout.alignment: Qt.AlignTop
                                        text: modelData.value
                                        color: root.textColor
                                        font.family: isMonoValue ? root.dataFontFamily : root.font.family
                                        font.pixelSize: 13
                                        font.weight: 400
                                        wrapMode: Text.WordWrap
                                    }

                                    Button {
                                        visible: hasAction
                                        Layout.alignment: Qt.AlignTop
                                        Layout.preferredWidth: 30
                                        Layout.preferredHeight: 30
                                        padding: 0
                                        hoverEnabled: true
                                        display: AbstractButton.IconOnly
                                        text: ""
                                        background: Rectangle {
                                            radius: 8
                                            color: parent.down ? root.actionHoverBgColor
                                                  : parent.hovered ? root.actionHoverBgColor
                                                  : root.actionBgColor
                                            border.width: 1
                                            border.color: root.actionBorderColor
                                        }
                                        contentItem: Item {
                                            implicitWidth: 30
                                            implicitHeight: 30
                                            Text {
                                                anchors.centerIn: parent
                                                text: "↗"
                                                color: root.accentColor
                                                font.family: root.dataFontFamily
                                                font.pixelSize: 13
                                                font.weight: 700
                                            }
                                        }
                                        ToolTip.visible: hovered
                                        ToolTip.text: typeof modelData.actionTooltip === "string"
                                                      && modelData.actionTooltip.length > 0
                                                      ? modelData.actionTooltip
                                                      : qsTr("Open in file manager")
                                        onClicked: root.rowActionRequested(modelData.actionId,
                                                                           modelData.actionValue)
                                    }
                                }
                            }

                            Item {
                                Layout.fillWidth: true
                                Layout.preferredHeight: 4
                            }
                        }
                    }
                }
            }
        }

        RowLayout {
            Layout.fillWidth: true

            Item { Layout.fillWidth: true }

            Button {
                text: qsTr("Close")
                onClicked: root.close()
            }
        }
    }
}

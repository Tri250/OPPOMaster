//  Copyright 2026 Yurun Zi
//  SPDX-License-Identifier: GPL-3.0-only
//  Additional permission under GPLv3 section 7 applies; see the LICENSE file.

import QtQuick
import QtQuick.Controls
import QtQuick.Layouts

// Onboarding overlay for first-time users (Chinese UX Enhancement)
// Provides step-by-step guidance for new users
Rectangle {
    id: root
    
    property bool active: false
    property int currentStep: 0
    property var steps: [
        {
            title: qsTr("欢迎使用 Alcedo Studio"),
            description: qsTr("Alcedo Studio 是一款专业的 RAW 图像处理软件，为摄影师提供强大的后期处理能力。"),
            highlight: null
        },
        {
            title: qsTr("导入图片"),
            description: qsTr("点击「文件」→「导入」或直接拖拽图片到窗口中开始处理。支持 RAW、JPEG、PNG 等多种格式。"),
            highlight: "fileMenu"
        },
        {
            title: qsTr("浏览与筛选"),
            description: qsTr("使用鼠标滚轮或 Ctrl+滚轮调整缩略图大小。双击打开编辑器，右键查看更多选项。"),
            highlight: "thumbnailGrid"
        },
        {
            title: qsTr("开始编辑"),
            description: qsTr("在编辑器中调整曝光、色彩、对比度等参数。所有操作都是非破坏性的，随时可以撤销。"),
            highlight: null
        },
        {
            title: qsTr("导出作品"),
            description: qsTr("编辑完成后，点击「导出」按钮保存处理结果。支持 JPEG、PNG、TIFF 等多种输出格式。"),
            highlight: "exportButton"
        }
    ]
    
    signal finished()
    signal skipped()
    
    visible: active
    color: Qt.rgba(0, 0, 0, 0.75)
    
    function start() {
        currentStep = 0
        active = true
    }
    
    function nextStep() {
        if (currentStep < steps.length - 1) {
            currentStep++
        } else {
            active = false
            root.finished()
        }
    }
    
    function previousStep() {
        if (currentStep > 0) {
            currentStep--
        }
    }
    
    function skip() {
        active = false
        root.skipped()
    }
    
    Keys.onEscapePressed: skip()
    Keys.onReturnPressed: nextStep()
    Keys.onRightPressed: nextStep()
    Keys.onLeftPressed: previousStep()
    
    // Main content
    Rectangle {
        id: contentCard
        anchors.centerIn: parent
        width: Math.min(parent.width - 48, 520)
        height: Math.max(320, contentColumn.implicitHeight + 80)
        radius: 16
        color: appTheme.bgPanelColor
        border.width: 1
        border.color: appTheme.glassStrokeColor
        
        ColumnLayout {
            id: contentColumn
            anchors.fill: parent
            anchors.margins: 32
            spacing: 20
            
            // Progress dots
            Row {
                Layout.alignment: Qt.AlignHCenter
                spacing: 8
                
                Repeater {
                    model: root.steps.length
                    
                    Rectangle {
                        width: 8
                        height: 8
                        radius: 4
                        color: index === root.currentStep 
                               ? appTheme.accentColor 
                               : (index < root.currentStep ? appTheme.textColor : appTheme.textMutedColor)
                        opacity: index <= root.currentStep ? 1 : 0.5
                        
                        Behavior on color { ColorAnimation { duration: 150 } }
                    }
                }
            }
            
            // Step title
            Label {
                Layout.fillWidth: true
                text: root.steps[root.currentStep].title
                color: appTheme.textColor
                font.family: appTheme.headlineFontFamily
                font.pixelSize: 24
                font.weight: Font.Bold
                horizontalAlignment: Text.AlignHCenter
                wrapMode: Text.WordWrap
            }
            
            // Step description
            Label {
                Layout.fillWidth: true
                Layout.preferredWidth: contentCard.width - 64
                text: root.steps[root.currentStep].description
                color: appTheme.textMutedColor
                font.family: appTheme.uiFontFamily
                font.pixelSize: 14
                horizontalAlignment: Text.AlignHCenter
                wrapMode: Text.WordWrap
                lineHeight: 1.4
            }
            
            // Spacer
            Item {
                Layout.fillHeight: true
            }
            
            // Navigation buttons
            RowLayout {
                Layout.fillWidth: true
                spacing: 12
                
                Button {
                    text: qsTr("跳过教程")
                    flat: true
                    font.pixelSize: 14
                    Material.foreground: appTheme.textMutedColor
                    onClicked: root.skip()
                    visible: root.currentStep < root.steps.length - 1
                }
                
                Item { Layout.fillWidth: true }
                
                Button {
                    text: qsTr("上一步")
                    flat: true
                    font.pixelSize: 14
                    Material.foreground: appTheme.textColor
                    enabled: root.currentStep > 0
                    opacity: enabled ? 1 : 0.5
                    onClicked: root.previousStep()
                    visible: root.currentStep > 0
                }
                
                Button {
                    id: nextButton
                    text: root.currentStep === root.steps.length - 1 ? qsTr("开始使用") : qsTr("下一步")
                    font.pixelSize: 14
                    font.weight: Font.DemiBold
                    Material.foreground: appTheme.bgCanvasColor
                    onClicked: root.nextStep()
                    
                    background: Rectangle {
                        radius: 8
                        color: nextButton.hovered ? Qt.lighter(appTheme.accentColor, 1.1) : appTheme.accentColor
                    }
                }
            }
        }
    }
    
    // Skip all button (top right)
    ToolButton {
        anchors.right: parent.right
        anchors.top: parent.top
        anchors.margins: 24
        text: "\u00d7"
        font.pixelSize: 28
        Material.foreground: Qt.rgba(1, 1, 1, 0.7)
        onClicked: root.skip()
        ToolTip.visible: hovered
        ToolTip.text: qsTr("跳过教程")
        ToolTip.delay: 500
    }
    
    // Keyboard shortcuts hint
    Label {
        anchors.horizontalCenter: parent.horizontalCenter
        anchors.bottom: parent.bottom
        anchors.bottomMargin: 24
        text: qsTr("按 Enter 继续 · Esc 跳过 · 左右方向键导航")
        color: Qt.rgba(1, 1, 1, 0.45)
        font.pixelSize: 12
    }
}
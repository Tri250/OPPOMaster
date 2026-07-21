//  Copyright 2026 Yurun Zi
//  SPDX-License-Identifier: GPL-3.0-only
//  Additional permission under GPLv3 section 7 applies; see the LICENSE file.

import QtQuick

// ── Shared Animation Components ──────────────────────────────────────────
//
//  Reusable animation building blocks for Alcedo Studio's QML layer.
//  Import this file and attach the components to your items.
//
//  Usage:
//    • Dialog enter/exit:  apply DialogPanelAnimation to the shell Rectangle
//    • Thumbnail fade-in:  wrap with ThumbnailFadeIn
//    • List add/remove:    use listAddTransition / listRemoveTransition in
//                           ListView's add / remove Transition
//    • Button hover/press: apply ButtonMicroInteraction to any Button or Item
//    • Toggle switches:    apply ToggleSwitchAnimation to the indicator

// ── 1. Dialog shell enter/exit (scale + opacity + y-shift) ──────────────
//
//  Attach to the "shell" Rectangle inside a full-screen Dialog.
//  Uses attached properties: set `animEnabled: true` on the shell,
//  then bind `animVisible` to whether the dialog is open.
//
//  Example:
//    Rectangle {
//        id: shell
//        property bool animEnabled: true
//        property bool animVisible: dialog.visible
//        Component.onCompleted: AppAnimations.setupDialogShell(shell)
//    }

QtObject {
    id: appAnimations

    // ── Timing constants ──
    readonly property int dialogEnterDuration: 220
    readonly property int dialogExitDuration: 160
    readonly property int fadeInDuration: 200
    readonly property int fadeOutDuration: 140
    readonly property int slideDistance: 18
    readonly property real dialogEnterScale: 0.96
    readonly property real dialogExitScale: 0.97

    // ── Easing curves ──
    readonly property int enterEasing: Easing.OutCubic
    readonly property int exitEasing: Easing.InCubic
    readonly property int springEasing: Easing.OutQuint

    // ── Micro-interaction constants ──
    readonly property real buttonHoverScale: 1.03
    readonly property real buttonPressScale: 0.97
    readonly property int buttonAnimDuration: 100
    readonly property int hoverAnimDuration: 120

    // ── Progress bar ──
    readonly property int progressBarDuration: 300

    // ── Thumbnail ──
    readonly property int thumbnailFadeInDuration: 250
}

// ── 2. Inline component: DialogShellAnimation ────────────────────────────
//
//  A self-contained ParallelAnimation you can trigger imperatively:
//    shellAnim.enter()
//    shellAnim.exit()
//
//  Bind to a target Item that has properties: animScale, animOpacity, animY.
component DialogShellAnimation: ParallelAnimation {
    id: shellAnim

    property Item target: null
    property bool isEnter: true

    function enter() {
        isEnter = true
        if (target) {
            target.animScale = appAnimations.dialogEnterScale
            target.animOpacity = 0
            target.animY = appAnimations.slideDistance
        }
        start()
    }

    function exit() {
        isEnter = false
        start()
    }

    NumberAnimation {
        property: "animScale"
        target: shellAnim.target
        to: shellAnim.isEnter ? 1.0 : appAnimations.dialogExitScale
        duration: shellAnim.isEnter ? appAnimations.dialogEnterDuration
                                    : appAnimations.dialogExitDuration
        easing.type: shellAnim.isEnter ? appAnimations.enterEasing
                                       : appAnimations.exitEasing
    }

    NumberAnimation {
        property: "animOpacity"
        target: shellAnim.target
        to: shellAnim.isEnter ? 1.0 : 0.0
        duration: shellAnim.isEnter ? appAnimations.dialogEnterDuration
                                    : appAnimations.dialogExitDuration
        easing.type: shellAnim.isEnter ? appAnimations.enterEasing
                                       : appAnimations.exitEasing
    }

    NumberAnimation {
        property: "animY"
        target: shellAnim.target
        to: shellAnim.isEnter ? 0 : appAnimations.slideDistance
        duration: shellAnim.isEnter ? appAnimations.dialogEnterDuration
                                    : appAnimations.dialogExitDuration
        easing.type: shellAnim.isEnter ? appAnimations.enterEasing
                                       : appAnimations.exitEasing
    }
}

// ── 3. Inline component: ThumbnailFadeIn ─────────────────────────────────
//
//  Wrap any item to fade it in when it becomes visible or when
//  `active` flips to true.  Typical use: thumbnail image containers.
//
//  Example:
//    ThumbnailFadeIn {
//        active: thumbImage.status === Image.Ready
//        fadesItem: thumbImage
//    }
component ThumbnailFadeIn: Item {
    id: fadeInRoot

    property bool active: false
    property Item fadesItem: null
    readonly property int duration: appAnimations.thumbnailFadeInDuration

    onActiveChanged: {
        if (active && fadesItem) {
            fadesItem.opacity = 0
            fadeInAnim.start()
        }
    }

    NumberAnimation {
        id: fadeInAnim
        target: fadeInRoot.fadesItem
        property: "opacity"
        from: 0
        to: 1
        duration: fadeInRoot.duration
        easing.type: Easing.OutCubic
    }
}

// ── 4. Inline component: ButtonMicroInteraction ──────────────────────────
//
//  Apply hover scale-up and press scale-down to any Item.
//  The host item must expose `hovered` and `pressed` boolean properties.
//
//  Example:
//    ButtonMicroInteraction {
//        target: myButton
//        hovered: myMouseArea.containsMouse
//        pressed: myMouseArea.pressed
//    }
component ButtonMicroInteraction: Item {
    id: microRoot

    property Item target: null
    property bool hovered: false
    property bool pressed: false

    readonly property real targetScale: {
        if (!target || !target.enabled) return 1.0
        if (pressed) return appAnimations.buttonPressScale
        if (hovered) return appAnimations.buttonHoverScale
        return 1.0
    }

    onTargetScaleChanged: {
        if (target) {
            target.scale = targetScale
        }
    }

    Behavior on targetScale {
        enabled: microRoot.target !== null
        NumberAnimation {
            duration: appAnimations.buttonAnimDuration
            easing.type: Easing.OutCubic
        }
    }
}

// ── 5. Transition helpers for ListView add/remove ────────────────────────
//
//  Use as the `add` and `remove` Transitions of a ListView.
//
//  Example:
//    ListView {
//        add: AppAnimations.listAddTransition
//        remove: AppAnimations.listRemoveTransition
//        displaced: AppAnimations.listDisplacedTransition
//    }

component ListAddTransition: Transition {
    NumberAnimation {
        property: "opacity"
        from: 0
        to: 1
        duration: appAnimations.fadeInDuration
        easing.type: Easing.OutCubic
    }
    NumberAnimation {
        property: "y"
        from: ListAddTransition.ViewTransition.item.y + 12
        to: ListAddTransition.ViewTransition.item.y
        duration: appAnimations.dialogEnterDuration
        easing.type: Easing.OutCubic
    }
}

component ListRemoveTransition: Transition {
    NumberAnimation {
        property: "opacity"
        from: 1
        to: 0
        duration: appAnimations.fadeOutDuration
        easing.type: Easing.InCubic
    }
    NumberAnimation {
        property: "scale"
        from: 1.0
        to: 0.92
        duration: appAnimations.fadeOutDuration
        easing.type: Easing.InCubic
    }
}

component ListDisplacedTransition: Transition {
    NumberAnimation {
        property: "y"
        duration: appAnimations.dialogEnterDuration
        easing.type: Easing.OutQuint
    }
    NumberAnimation {
        property: "opacity"
        to: 1
        duration: appAnimations.fadeInDuration
        easing.type: Easing.OutCubic
    }
}

// ── 6. Inline component: ProgressBarAnimation ────────────────────────────
//
//  Apply to a ProgressBar or custom progress bar to smoothly animate
//  its value rather than jumping.
//
//  Example:
//    ProgressBar {
//        id: pb
//        Behavior on value {
//            NumberAnimation {
//                duration: AppAnimations.progressBarDuration
//                easing.type: Easing.OutCubic
//            }
//        }
//    }
//  Or attach imperatively:
//    ProgressBarAnimation { target: pb; value: 0.6 }
component ProgressBarAnimation: QtObject {
    property Item target: null
    property real value: 0

    onValueChanged: {
        if (target && target.hasOwnProperty("value")) {
            target.value = value
        }
    }
}

// ── 7. Inline component: PanelSlideAnimation ────────────────────────────
//
//  For panels that slide in/out (inspector, sidebars).
//  Animates width or x depending on direction.
//
//  Example:
//    PanelSlideAnimation {
//        target: inspectorPanel
//        property: "Layout.preferredWidth"
//        visible: inspectorVisible
//        showValue: 320
//        hideValue: 0
//    }
component PanelSlideAnimation: QtObject {
    property Item target: null
    property string property: "width"
    property bool visible: true
    property real showValue: 320
    property real hideValue: 0
    readonly property real currentValue: visible ? showValue : hideValue

    onCurrentValueChanged: {
        if (target) {
            target[property] = currentValue
        }
    }
}

// ── 8. Inline component: ToggleSwitchAnimation ───────────────────────────
//
//  Smooth toggle switch with position + color animation.
//  Apply to a toggle indicator rectangle.
//
//  Example:
//    ToggleSwitchAnimation {
//        target: switchIndicator
//        checked: mySwitch.checked
//    }
component ToggleSwitchAnimation: QtObject {
    property Item target: null
    property bool checked: false

    readonly property real indicatorX: checked
        ? (target ? target.parent.width - target.width - 4 : 0)
        : 4

    onCheckedChanged: {
        if (target && target.hasOwnProperty("x")) {
            target.x = indicatorX
        }
    }
}

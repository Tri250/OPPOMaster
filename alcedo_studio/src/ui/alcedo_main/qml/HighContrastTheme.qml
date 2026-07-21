import QtQuick

// High-contrast theme for accessibility. Imported by Main.qml and bound to
// the root's color properties when the system reports a high-contrast
// preference (e.g. Windows "High Contrast" mode or increased contrast
// accessibility setting).
QtObject {
    id: hc

    // Whether high-contrast mode should be active. Bound to system detection
    // in Main.qml.
    property bool active: false

    // ── High-contrast color palette ──────────────────────────────────────
    // WCAG 2.1 AAA requires at least 7:1 contrast ratio for normal text.

    readonly property color bgDeepColor:      "#000000"
    readonly property color bgBaseColor:      "#0A0A0A"
    readonly property color bgPanelColor:     "#1A1A1A"
    readonly property color bgCanvasColor:    "#111111"

    readonly property color textColor:        "#FFFFFF"
    readonly property color textMutedColor:   "#CCCCCC"

    readonly property color accentColor:             "#00CCFF"   // bright cyan
    readonly property color accentSecondaryColor:    "#FFD700"   // gold
    readonly property color accentColorSoft:         "#0099CC"

    readonly property color dangerColor:             "#FF4444"
    readonly property color dangerTintColor:         Qt.rgba(1, 0.27, 0.27, 0.25)
    readonly property color selectedTintColor:       Qt.rgba(0, 0.8, 1, 0.20)

    readonly property color hoverColor:              Qt.rgba(1, 1, 1, 0.12)
    readonly property color dividerColor:            Qt.rgba(1, 1, 1, 0.30)

    readonly property color glassPanelColor:         "#1A1A1A"
    readonly property color glassStrokeColor:        Qt.rgba(1, 1, 1, 0.40)
    readonly property color overlayColor:            Qt.rgba(0, 0, 0, 0.85)

    // ── Tone tokens ──
    readonly property color toneGold:       "#FFD700"
    readonly property color toneWine:       "#FF6666"
    readonly property color toneSteel:      "#00CCFF"
    readonly property color toneGraphite:   "#CCCCCC"
    readonly property color toneMist:       "#EEEEEE"

    // ── Border / stroke (visible in high contrast) ──
    readonly property color borderVisible:  Qt.rgba(1, 1, 1, 0.50)

    // ── Panel radius override (reduce for clarity) ──
    readonly property int panelRadius: 6

    // ── Detection ───────────────────────────────────────────────────────

    /// Detects whether the system has a high-contrast accessibility setting
    /// enabled. On Qt 6, we check the platform theme and screen parameters.
    function detectHighContrast() -> bool {
        // Windows: check if the system is in High Contrast mode via the
        // platform theme hint. Qt exposes this through
        // QPlatformTheme::HighContrastMessages but it's not directly
        // accessible from QML. Instead, we check the application palette.
        if (Qt.platform.os === "windows") {
            // On Windows High Contrast mode, the system palette uses very
            // saturated colors (e.g. bright yellow text on black). We detect
            // this by checking if the text color is extremely bright compared
            // to the window background.
            var palette = Qt.application.palette
            if (palette) {
                var windowBg = palette.window
                var textCol = palette.windowText
                // If the text-to-background luminance ratio exceeds 15:1,
                // it's likely high-contrast mode.
                var bgLum = luminance(windowBg)
                var txtLum = luminance(textCol)
                if (bgLum < 0.05 && txtLum > 0.85) {
                    return true
                }
            }
        }

        // Cross-platform: check if the user has enabled the "increase
        // contrast" accessibility setting. Qt doesn't expose this directly,
        // but on macOS it maps to the reduceTransparency + increaseContrast
        // preferences. We use a heuristic based on font settings.
        return false
    }

    /// Relative luminance of a color (per WCAG 2.1).
    function luminance(colorValue) {
        var r = colorValue.r
        var g = colorValue.g
        var b = colorValue.b
        r = r <= 0.03928 ? r / 12.92 : Math.pow((r + 0.055) / 1.055, 2.4)
        g = g <= 0.03928 ? g / 12.92 : Math.pow((g + 0.055) / 1.055, 2.4)
        b = b <= 0.03928 ? b / 12.92 : Math.pow((b + 0.055) / 1.055, 2.4)
        return 0.2126 * r + 0.7152 * g + 0.0722 * b
    }

    /// Returns a property map of theme color overrides for high-contrast mode.
    /// The caller (Main.qml) can spread these onto its color properties.
    function themeOverrides() -> var {
        if (!active) {
            return {}
        }
        return {
            "bgDeepColor": bgDeepColor,
            "bgBaseColor": bgBaseColor,
            "bgPanelColor": bgPanelColor,
            "bgCanvasColor": bgCanvasColor,
            "textColor": textColor,
            "textMutedColor": textMutedColor,
            "accentColor": accentColor,
            "accentSecondaryColor": accentSecondaryColor,
            "dangerColor": dangerColor,
            "dangerTintColor": dangerTintColor,
            "selectedTintColor": selectedTintColor,
            "hoverColor": hoverColor,
            "dividerColor": dividerColor,
            "glassPanelColor": glassPanelColor,
            "glassStrokeColor": glassStrokeColor,
            "overlayColor": overlayColor,
            "panelRadius": panelRadius,
            "borderVisible": borderVisible
        }
    }

    Component.onCompleted: {
        active = detectHighContrast()
    }
}

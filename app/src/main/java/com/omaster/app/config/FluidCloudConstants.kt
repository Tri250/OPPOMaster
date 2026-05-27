package com.omaster.app.config

object FluidCloudConstants {
    const val TEMPLATE_FILE = "fluid_cloud_preset_template.xml"
    const val COMPACT_TEMPLATE_FILE = "fluid_cloud_compact_template.xml"

    const val DEFAULT_GRADIENT_START = "#6366F1"
    const val DEFAULT_GRADIENT_END = "#8B5CF6"
    const val DEFAULT_BORDER_COLOR = "#8B5CF6"

    const val ANIMATION_TYPE_COLOR_FLOW = "colorFlow"
    const val ANIMATION_TYPE_NONE = "none"

    const val UPDATE_TRANSFORM_UP = "up"
    const val UPDATE_TRANSFORM_DOWN = "down"
    const val UPDATE_TRANSFORM_NONE = "none"

    const val ENTRY_TYPE_NOTIFICATION = "notification"
    const val CATEGORY_MODULAR = "modular"
    const val CATEGORY_GENERAL = "general"

    const val LEADING_CATEGORY_MIRROR = "mirror"
    const val LEADING_CATEGORY_SWITCHES = "switches"

    const val CENTER_CATEGORY_COMMON = "common"
    const val CENTER_CATEGORY_MIRROR = "mirror"
    const val CENTER_CATEGORY_GRAPHIC_HIGHLIGHT = "graphic-highlight"
    const val CENTER_CATEGORY_TEXT_HIGHLIGHT = "text-highlight"
    const val CENTER_CATEGORY_SWITCHES = "switches"

    const val TRAILING_CATEGORY_MULTI_TEXTS = "multi-texts"
    const val TRAILING_CATEGORY_MULTI_BUTTONS = "multi-buttons"
    const val TRAILING_CATEGORY_PROGRESS = "progress"
    const val TRAILING_CATEGORY_DIFF_ELEMENT = "diff-element"

    const val BUTTON_STYLE_PRIMARY = "primary"
    const val BUTTON_STYLE_SECONDARY = "secondary"

    const val MIN_API_LEVEL = 26
    const val RECOMMENDED_API_LEVEL = 30

    fun createGradient(angle: Int = 180, vararg colors: String): String {
        val colorList = colors.toList()
        return "linear-gradient(${angle}deg,${colorList.joinToString(",")})"
    }

    fun createDefaultGradient(): String {
        return createGradient(180, DEFAULT_GRADIENT_START, DEFAULT_GRADIENT_END)
    }
}

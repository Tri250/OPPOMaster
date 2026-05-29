package com.omaster.app.model

data class CameraParams(
    // 曝光参数
    var ev: String? = null,
    var iso: String? = null,
    var shutter: String? = null,
    
    // 白平衡参数
    var wb: String? = null,
    var colorTemp: Float? = null,
    var tint: Float? = null,
    
    // HSL参数
    var hue: Float? = null,
    var saturation: Float? = null,
    var luminance: Float? = null,
    
    // 对比度与锐化
    var contrast: Float? = null,
    var sharpness: Float? = null,
    
    // 暗角
    var vignette: Float? = null,
    
    // 滤镜
    var filter: String? = null
)

data class PresetEditorState(
    var name: String = "",
    var description: String = "",
    var coverPath: String = "",
    var tags: List<String> = emptyList(),
    var cameraParams: CameraParams = CameraParams(),
    var isDraft: Boolean = false,
    var draftSavedAt: Long? = null
)

data class EditorParam(
    val key: String,
    val displayName: String,
    val icon: String,
    val min: Float,
    val max: Float,
    val step: Float,
    val defaultValue: Float,
    val unit: String = "",
    val paramType: ParamType = ParamType.SLIDER
)

enum class ParamType {
    SLIDER,
    KNOB,
    SWITCH,
    SELECTOR,
    TEXT
}

object CameraParamConstants {
    val EXPOSURE_PARAMS = listOf(
        EditorParam(
            key = "ev",
            displayName = "曝光补偿",
            icon = "📷",
            min = -3.0f,
            max = 3.0f,
            step = 0.1f,
            defaultValue = 0.0f,
            unit = "EV"
        ),
        EditorParam(
            key = "iso",
            displayName = "ISO",
            icon = "🔆",
            min = 50f,
            max = 102400f,
            step = 1f,
            defaultValue = 100f,
            paramType = ParamType.SELECTOR
        ),
        EditorParam(
            key = "shutter",
            displayName = "快门速度",
            icon = "⏱️",
            min = 0.0001f,
            max = 30f,
            step = 0.001f,
            defaultValue = 0.033f,
            paramType = ParamType.SELECTOR
        )
    )

    val WHITE_BALANCE_PARAMS = listOf(
        EditorParam(
            key = "wb",
            displayName = "白平衡",
            icon = "⚖️",
            min = 0f,
            max = 10f,
            step = 1f,
            defaultValue = 0f,
            paramType = ParamType.SELECTOR
        ),
        EditorParam(
            key = "colorTemp",
            displayName = "色温",
            icon = "🌡️",
            min = 2000f,
            max = 10000f,
            step = 100f,
            defaultValue = 5500f,
            unit = "K"
        ),
        EditorParam(
            key = "tint",
            displayName = "色调",
            icon = "🎨",
            min = -100f,
            max = 100f,
            step = 1f,
            defaultValue = 0f,
            unit = ""
        )
    )

    val HSL_PARAMS = listOf(
        EditorParam(
            key = "hue",
            displayName = "色相",
            icon = "🌈",
            min = 0f,
            max = 360f,
            step = 1f,
            defaultValue = 0f,
            unit = "°"
        ),
        EditorParam(
            key = "saturation",
            displayName = "饱和度",
            icon = "🔸",
            min = -100f,
            max = 100f,
            step = 1f,
            defaultValue = 0f,
            unit = "%"
        ),
        EditorParam(
            key = "luminance",
            displayName = "明度",
            icon = "✨",
            min = -100f,
            max = 100f,
            step = 1f,
            defaultValue = 0f,
            unit = "%"
        )
    )

    val DETAIL_PARAMS = listOf(
        EditorParam(
            key = "contrast",
            displayName = "对比度",
            icon = "⬜",
            min = -50f,
            max = 100f,
            step = 1f,
            defaultValue = 0f,
            unit = "%"
        ),
        EditorParam(
            key = "sharpness",
            displayName = "锐化",
            icon = "✨",
            min = 0f,
            max = 100f,
            step = 1f,
            defaultValue = 50f,
            unit = "%"
        ),
        EditorParam(
            key = "vignette",
            displayName = "暗角",
            icon = "🌑",
            min = 0f,
            max = 100f,
            step = 1f,
            defaultValue = 0f,
            unit = "%"
        )
    )

    val SHUTTER_SPEEDS = listOf(
        "30", "25", "20", "15", "13", "10", "8", "6", "5", "4",
        "3.2", "2.5", "2", "1.6", "1.3", "1", "0.8", "0.6", "0.5",
        "0.4", "0.3", "0.25", "0.2", "0.16", "0.13", "0.1", "0.08",
        "0.06", "0.05", "0.04", "0.03", "0.025", "0.02", "0.016",
        "0.013", "0.01", "1/125", "1/160", "1/200", "1/250",
        "1/320", "1/400", "1/500", "1/640", "1/800", "1/1000",
        "1/1250", "1/1600", "1/2000", "1/2500", "1/3200", "1/4000",
        "1/5000", "1/6400", "1/8000"
    )

    val ISO_VALUES = listOf(
        "50", "100", "125", "160", "200", "250", "320", "400",
        "500", "640", "800", "1000", "1250", "1600", "2000",
        "2500", "3200", "4000", "5000", "6400", "8000", "10000",
        "12800", "16000", "20000", "25600", "32000", "40000",
        "51200", "64000", "80000", "102400"
    )

    val WHITE_BALANCE_PRESETS = listOf(
        "自动", "晴天", "阴天", "荧光灯", "白炽灯", "自定义"
    )
}

package com.omaster.app.model

import com.omaster.app.config.FluidCloudConstants

data class FluidCloudPresetData(
    val presetId: String,
    val title: String,
    val subtitle: String,
    val leading: LeadingData,
    val center: CenterData,
    val trailing: TrailingData?,
    val backgroundColor: String,
    val borderColor: String,
    val animationType: String = FluidCloudConstants.ANIMATION_TYPE_COLOR_FLOW,
    val updateTransform: String = FluidCloudConstants.UPDATE_TRANSFORM_NONE
)

data class LeadingData(
    val category: String = FluidCloudConstants.LEADING_CATEGORY_MIRROR,
    val iconPath: String,
    val titleText: String,
    val subtitleText: String,
    val showIconBg: Boolean = true,
    val smallMargin: Boolean = true
)

data class CenterData(
    val category: String = FluidCloudConstants.CENTER_CATEGORY_COMMON,
    val mainTitle: String,
    val cameraParams: CameraParamsDisplay,
    val coverImagePath: String,
    val buttons: List<ButtonData>,
    val paramsDisplay: List<ParamDisplay>?,
    val showLargeImage: Boolean = false
)

data class TrailingData(
    val category: String = FluidCloudConstants.TRAILING_CATEGORY_MULTI_TEXTS,
    val texts: List<String>,
    val divider: Boolean = true,
    val showIconBg: Boolean = false,
    val smallMargin: Boolean = false
)

data class ButtonData(
    val text: String,
    val action: String,
    val level: String = "E1*"
)

data class ParamDisplay(
    val label: String,
    val value: String,
    val level: String = "C5"
)

data class CameraParamsDisplay(
    val iso: String,
    val shutter: String,
    val ev: String,
    val contrast: String,
    val saturation: String,
    val whiteBalance: String,
    val vignette: String
)

fun Preset.toFluidCloudData(): FluidCloudPresetData {
    val params = this.cameraParams ?: CameraParams()

    return FluidCloudPresetData(
        presetId = this.id,
        title = this.name,
        subtitle = "ISO ${params.iso} | ${params.shutter}",
        leading = LeadingData(
            iconPath = this.coverPath,
            titleText = this.name,
            subtitleText = "ISO ${params.iso} | ${params.shutter}"
        ),
        center = CenterData(
            mainTitle = this.name,
            cameraParams = CameraParamsDisplay(
                iso = params.iso.toString(),
                shutter = params.shutter,
                ev = params.ev,
                contrast = String.format("%.1f", params.contrast),
                saturation = String.format("%.1f", params.saturation),
                whiteBalance = params.wb,
                vignette = String.format("%.2f", params.vignette)
            ),
            coverImagePath = this.coverPath,
            buttons = listOf(
                ButtonData("应用", "apply"),
                ButtonData("详情", "detail")
            ),
            paramsDisplay = listOf(
                ParamDisplay("对比度", String.format("%.1f", params.contrast)),
                ParamDisplay("饱和度", String.format("%.1f", params.saturation)),
                ParamDisplay("白平衡", params.wb),
                ParamDisplay("暗角", String.format("%.2f", params.vignette))
            )
        ),
        trailing = TrailingData(
            texts = listOf(
                String.format("%.1f", this.rating),
                "${this.usageCount}次使用",
                this.deviceModel
            )
        ),
        backgroundColor = FluidCloudConstants.createDefaultGradient(),
        borderColor = FluidCloudConstants.DEFAULT_BORDER_COLOR
    )
}

fun FluidCloudPresetData.toUiDataMap(): Map<String, Any> {
    return mapOf(
        "presetName" to title,
        "isoText" to center.cameraParams.iso,
        "shutterText" to center.cameraParams.shutter,
        "evText" to center.cameraParams.ev,
        "contrastText" to center.cameraParams.contrast,
        "saturationText" to center.cameraParams.saturation,
        "wbText" to center.cameraParams.whiteBalance,
        "vignetteText" to center.cameraParams.vignette,
        "ratingText" to (trailing?.texts?.getOrNull(0) ?: "0.0"),
        "usageText" to (trailing?.texts?.getOrNull(1) ?: "0次使用"),
        "deviceText" to (trailing?.texts?.getOrNull(2) ?: ""),
        "iconPath" to leading.iconPath,
        "coverPath" to center.coverImagePath,
        "bgColor" to backgroundColor,
        "borderColor" to borderColor,
        "applyBtnText" to center.buttons.getOrNull(0)?.text ?: "应用",
        "detailBtnText" to center.buttons.getOrNull(1)?.text ?: "详情",
        "showApplyBtn" to center.buttons.isNotEmpty(),
        "showDetailBtn" to center.buttons.size > 1,
        "showParams" to !center.paramsDisplay.isNullOrEmpty(),
        "showTrailing" to trailing != null
    )
}

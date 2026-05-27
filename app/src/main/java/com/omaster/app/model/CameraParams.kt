package com.omaster.app.model

data class CameraParams(
    val mode: String = "master",
    val filter: String = "",
    val iso: Int = 100,
    val shutter: String = "1/125",
    val ev: String = "0",
    val wb: String = "5500K",
    val hasselblad_hncs: Boolean = false,
    val contrast: Float = 1.0f,
    val saturation: Float = 1.0f,
    val sharpness: Float = 1.0f,
    val vignette: Float = 0.0f,
    val videoLut: String = "",
    val sceneTags: List<String> = emptyList(),
    val colorProfile: ColorProfile? = null
)

data class ColorProfile(
    val dominantColors: List<Int> = emptyList(),
    val toneCurve: List<Float> = emptyList()
)

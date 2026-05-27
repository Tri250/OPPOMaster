package com.omaster.app.model

data class Section(
    val title: String,
    val content: String
)

data class Preset(
    val id: String,
    val name: String,
    val coverPath: String,
    val sections: List<Section> = emptyList(),
    val cameraParams: CameraParams? = null,
    val deviceModel: String = "",
    val source: String = "omaster_cloud",
    val isFavorite: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val usageCount: Int = 0,
    val rating: Float = 0f,
    val author: String = ""
)

data class PresetRecommendation(
    val preset: Preset,
    val score: Float
)

data class ColorExtractionResult(
    val dominantColors: List<Int>,
    val toneCurve: List<Float>,
    val matchedPresets: List<Pair<Preset, Float>>,
    val customPreset: Preset? = null
)

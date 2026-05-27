package com.omaster.app.model

import kotlinx.serialization.Serializable

@Serializable
data class Section(
    val title: String,
    val content: String
)

@Serializable
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
    val usageCount: Int = 0,
    val tags: List<String> = emptyList(),
    val isCustom: Boolean = false
)

package com.omaster.app.model

data class SectionItem(
    val label: String,
    val value: String,
    val span: Int = 1
)

data class Section(
    val title: String,
    val items: List<SectionItem> = emptyList()
)

data class Description(
    val title: String,
    val content: String
)

data class Preset(
    val id: String,
    val name: String,
    val coverPath: String,
    val galleryImages: List<String> = emptyList(),
    val author: String = "",
    val isNew: Boolean = false,
    val sections: List<Section> = emptyList(),
    val tags: List<String> = emptyList(),
    val description: Description? = null,
    val cameraParams: CameraParams? = null,
    val deviceModel: String = "",
    val source: String = "omaster_cloud",
    val isFavorite: Boolean = false,
    val applicableScenes: List<SceneType> = emptyList()
)

package com.omaster.app.model

import com.google.gson.annotations.SerializedName

/**
 * 预设包数据模型
 * 对应远程JSON数据结构
 */
data class PresetBundle(
    @SerializedName("version")
    val version: Int = 1,
    
    @SerializedName("name")
    val name: String = "",
    
    @SerializedName("author")
    val author: String = "",
    
    @SerializedName("build")
    val build: Int = 1,
    
    @SerializedName("presets")
    val presets: List<RemotePreset> = emptyList()
)

/**
 * 远程预设数据模型
 */
data class RemotePreset(
    @SerializedName("name")
    val name: String = "",
    
    @SerializedName("coverPath")
    val coverPath: String = "",
    
    @SerializedName("galleryImages")
    val galleryImages: List<String> = emptyList(),
    
    @SerializedName("author")
    val author: String? = null,
    
    @SerializedName("isNew")
    val isNew: Boolean = false,
    
    @SerializedName("sections")
    val sections: List<PresetSection> = emptyList(),
    
    @SerializedName("tags")
    val tags: List<String> = emptyList(),
    
    @SerializedName("description")
    val description: PresetDescription? = null
)

/**
 * 预设参数分区
 */
data class PresetSection(
    @SerializedName("title")
    val title: String = "",
    
    @SerializedName("items")
    val items: List<PresetItem> = emptyList()
)

/**
 * 预设参数项
 */
data class PresetItem(
    @SerializedName("label")
    val label: String = "",
    
    @SerializedName("value")
    val value: String = "",
    
    @SerializedName("span")
    val span: Int = 1
)

/**
 * 预设描述
 */
data class PresetDescription(
    @SerializedName("title")
    val title: String = "",
    
    @SerializedName("content")
    val content: String = ""
)

/**
 * 将RemotePreset转换为本地Preset模型
 */
fun RemotePreset.toPreset(id: String, brand: String): Preset {
    return Preset(
        id = id,
        name = name,
        description = description?.content ?: "",
        coverUrl = coverPath,
        galleryImages = galleryImages,
        author = author,
        tags = tags + brand,
        supportedDevices = listOf(brand),
        isNew = isNew,
        params = sections.flatMap { section ->
            section.items.map { item ->
                ParamItem(
                    label = item.label,
                    value = item.value,
                    span = item.span
                )
            }
        },
        sections = sections.map { section ->
            PresetSectionModel(
                title = section.title,
                items = section.items.map { item ->
                    ParamItem(
                        label = item.label,
                        value = item.value,
                        span = item.span
                    )
                }
            )
        }
    )
}

/**
 * 参数项模型
 */
data class ParamItem(
    val label: String,
    val value: String,
    val span: Int = 1
)

/**
 * 预设分区模型
 */
data class PresetSectionModel(
    val title: String,
    val items: List<ParamItem>
)

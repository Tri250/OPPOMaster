package com.omaster.app.model

import kotlinx.serialization.Serializable

// 用于解析远程 JSON 数据的数据类
@Serializable
data class RemotePresetResponse(
    val version: Int? = null,
    val name: String? = null,
    val author: String? = null,
    val build: Int? = null,
    val presets: List<RemotePreset>
)

@Serializable
data class RemotePreset(
    val name: String,
    val coverPath: String,
    val galleryImages: List<String>? = null,
    val author: String? = null,
    val isNew: Boolean? = null,
    val sections: List<RemoteSection>? = null,
    val tags: List<String>? = null,
    val description: RemoteDescription? = null
)

@Serializable
data class RemoteSection(
    val title: String,
    val items: List<RemoteSectionItem>? = null
)

@Serializable
data class RemoteSectionItem(
    val label: String,
    val value: String,
    val span: Int? = null
)

@Serializable
data class RemoteDescription(
    val title: String? = null,
    val content: String? = null
)

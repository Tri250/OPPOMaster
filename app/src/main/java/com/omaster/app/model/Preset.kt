package com.omaster.app.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo
import androidx.room.Index
import androidx.room.Fts4

@Entity(
    tableName = "presets",
    indices = [
        Index(value = ["name"], unique = true),
        Index(value = ["brand"]),
        Index(value = ["isFavorite"]),
        Index(value = ["downloadCount"]),
        Index(value = ["usageCount"])
    ]
)
data class Preset(
    @PrimaryKey val id: String,
    val name: String,
    val description: String? = null,
    val coverPath: String,
    val author: String? = null,
    val brand: String? = null,
    val deviceModel: String? = null,
    val isFavorite: Boolean = false,
    val downloadCount: Int = 0,
    val usageCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isNew: Boolean = false,
    val tags: List<String> = emptyList(),
    val styleCategory: String? = null,
    val sceneCategory: String? = null,
    val cameraParams: CameraParams? = null,
    val galleryImages: List<String> = emptyList()
)

@Entity(
    tableName = "preset_categories",
    indices = [
        Index(value = ["categoryType", "name"], unique = true)
    ]
)
data class PresetCategory(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "categoryType") val categoryType: String,
    val name: String,
    val icon: String? = null,
    val order: Int = 0,
    val isActive: Boolean = true
)

@Entity(
    tableName = "preset_category_join",
    primaryKeys = ["presetId", "categoryId"]
)
data class PresetCategoryJoin(
    @ColumnInfo(name = "presetId") val presetId: String,
    @ColumnInfo(name = "categoryId") val categoryId: Long
)

@Fts4(contentEntity = Preset::class)
@Entity(tableName = "presets_fts")
data class PresetsFts(
    @PrimaryKey @ColumnInfo(name = "rowid") val rowId: Long,
    val name: String,
    val description: String?,
    val author: String?,
    val tags: String
)

data class CategoryType(
    val id: String,
    val name: String,
    val icon: String,
    val categories: List<CategoryItem>
)

data class CategoryItem(
    val id: String,
    val name: String,
    val icon: String? = null,
    val count: Int = 0,
    val isSelected: Boolean = false
)

enum class CategoryTypeEnum(val value: String) {
    STYLE("style"),
    SCENE("scene"),
    DEVICE("device")
}

enum class SortType(val value: String, val displayName: String) {
    RELEVANCE("relevance", "匹配度"),
    POPULARITY("popularity", "热度"),
    FAVORITES("favorites", "收藏"),
    NEWEST("newest", "最新"),
    FREQUENCY("frequency", "使用频率")
}

enum class StyleCategory(val value: String, val displayName: String, val icon: String) {
    FILM("film", "胶片", "🎞️"),
    RETRO("retro", "复古", "📻"),
    FRESH("fresh", "清新", "🌿"),
    JAPANESE("japanese", "日系", "🌸"),
    GERMAN("german", "德系", "🇩🇪"),
    BLACK_WHITE("black_white", "黑白", "⬛"),
    CYBER_PUNK("cyber_punk", "赛博朋克", "🖥️"),
    MINIMAL("minimal", "极简", "⬜"),
    VIBRANT("vibrant", "鲜艳", "🌈"),
    MOODY("moody", "情绪", "🌙"),
    PORTRAIT("portrait", "人像", "👤"),
    LANDSCAPE("landscape", "风光", "🏞️")
}

enum class SceneCategory(val value: String, val displayName: String, val icon: String) {
    PORTRAIT("portrait", "人像", "👤"),
    FOOD("food", "美食", "🍔"),
    LANDSCAPE("landscape", "风光", "🏞️"),
    NIGHT("night", "夜景", "🌙"),
    STREET("street", "街拍", "🏙️"),
    STILL_LIFE("still_life", "静物", "🖼️"),
    PET("pet", "宠物", "🐱"),
    ARCHITECTURE("architecture", "建筑", "🏛️")
}

enum class DeviceCategory(val value: String, val displayName: String) {
    FIND_X("find_x", "Find X系列"),
    RENO("reno", "Reno系列"),
    ONEPLUS("oneplus", "一加"),
    REALME("realme", "realme"),
    OTHER("other", "其他机型")
}

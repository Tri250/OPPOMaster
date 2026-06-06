package com.omaster.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo

/**
 * 预设实体 - Room数据库表
 */
@Entity(tableName = "presets")
data class PresetEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val description: String,
    val brand: String,
    val model: String,
    val iso: String,
    val shutterSpeed: String,
    val aperture: String,
    val ev: String,
    val wb: String,
    val focusMode: String,
    val tags: String, // JSON格式存储
    val coverUrl: String?,
    val author: String?,
    val downloadCount: Int = 0,
    val useCount: Int = 0,
    val isFavorite: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val source: String = "remote"
)

/**
 * 相机配置实体
 */
@Entity(tableName = "camera_configs")
data class CameraConfigEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val description: String,
    val brand: String,
    val model: String,
    val iso: String,
    val shutterSpeed: String,
    val aperture: String,
    val ev: String,
    val wb: String,
    val focusMode: String,
    val tags: String, // JSON格式存储
    val isCustom: Boolean = false,
    val isFavorite: Boolean = false,
    val category: String = "默认",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * 用户资料实体
 */
@Entity(tableName = "user_profiles")
data class UserProfileEntity(
    @PrimaryKey
    val id: String,
    val username: String,
    val email: String?,
    val avatar: String?,
    val bio: String?,
    val preferredTags: String, // JSON格式存储
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val lastSyncAt: Long = 0
)

/**
 * 搜索历史实体
 */
@Entity(tableName = "search_history")
data class SearchHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val query: String,
    val resultCount: Int = 0,
    val searchedAt: Long = System.currentTimeMillis()
)

/**
 * 收藏实体
 */
@Entity(tableName = "favorites")
data class FavoriteEntity(
    @PrimaryKey
    val id: String,
    val presetId: String,
    val folderId: String = "default",
    val presetName: String,
    val presetCoverUrl: String?,
    val addedAt: Long = System.currentTimeMillis(),
    val notes: String = ""
)

/**
 * 使用日志实体
 */
@Entity(tableName = "usage_logs")
data class UsageLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val presetId: String,
    val action: String, // view, apply, favorite, share, download
    val metadata: String?, // JSON格式存储
    val createdAt: Long = System.currentTimeMillis()
)

package com.omaster.app.data.db.entity

import androidx.room.Entity
import androidx.room.Fts4
import androidx.room.PrimaryKey

@Entity(tableName = "camera_presets")
data class CameraPresetEntity(
    @PrimaryKey val id: String,
    val name: String,
    val author: String,
    val coverPath: String,
    val tags: String,
    val scene: String,
    val mode: String,
    val filter: String,
    val iso: Int,
    val shutter: String,
    val ev: String,
    val wb: String,
    val deviceModel: String,
    val source: String,
    val isFavorite: Boolean = false,
    val useCount: Int = 0,
    val createTime: Long = System.currentTimeMillis()
)

@Fts4(contentEntity = CameraPresetEntity::class)
@Entity(tableName = "camera_presets_fts")
data class CameraPresetFtsEntity(
    @PrimaryKey val rowid: Long,
    val name: String,
    val tags: String,
    val scene: String,
    val filter: String
)
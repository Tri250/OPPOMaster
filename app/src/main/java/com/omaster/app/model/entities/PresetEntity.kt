package com.omaster.app.model.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.omaster.app.model.CameraParams
import com.omaster.app.model.ColorProfile

@Entity(tableName = "presets")
@TypeConverters(Converters::class)
data class PresetEntity(
    @PrimaryKey val id: String,
    val name: String,
    val coverPath: String,
    val cameraParams: CameraParams?,
    val deviceModel: String,
    val source: String,
    val isFavorite: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
    val usageCount: Int,
    val rating: Float,
    val author: String
)

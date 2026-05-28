package com.omaster.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.omaster.app.model.CameraParams
import com.omaster.app.model.Preset
import com.omaster.app.model.Section

@Entity(tableName = "presets")
data class PresetEntity(
    @PrimaryKey val id: String,
    val name: String,
    val coverPath: String,
    val sectionsJson: String,
    val cameraParamsJson: String?,
    val deviceModel: String,
    val source: String,
    val isFavorite: Boolean,
    val lastAccessed: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis()
)

fun PresetEntity.toPreset(): Preset {
    val sections = try {
        com.google.gson.Gson().fromJson(sectionsJson, Array<Section>::class.java).toList()
    } catch (e: Exception) {
        emptyList()
    }
    val cameraParams = try {
        cameraParamsJson?.let {
            com.google.gson.Gson().fromJson(it, CameraParams::class.java)
        }
    } catch (e: Exception) {
        null
    }
    return Preset(
        id = id,
        name = name,
        coverPath = coverPath,
        sections = sections,
        cameraParams = cameraParams,
        deviceModel = deviceModel,
        source = source,
        isFavorite = isFavorite
    )
}

fun Preset.toPresetEntity(): PresetEntity {
    val sectionsJson = com.google.gson.Gson().toJson(sections)
    val cameraParamsJson = cameraParams?.let { com.google.gson.Gson().toJson(it) }
    return PresetEntity(
        id = id,
        name = name,
        coverPath = coverPath,
        sectionsJson = sectionsJson,
        cameraParamsJson = cameraParamsJson,
        deviceModel = deviceModel,
        source = source,
        isFavorite = isFavorite
    )
}

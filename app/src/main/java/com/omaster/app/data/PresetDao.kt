package com.omaster.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.omaster.app.model.entities.PresetEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PresetDao {
    @Query("SELECT * FROM presets")
    fun getAllPresets(): Flow<List<PresetEntity>>

    @Query("SELECT * FROM presets WHERE id = :id")
    suspend fun getPresetById(id: String): PresetEntity?

    @Query("SELECT * FROM presets WHERE isFavorite = 1")
    fun getFavoritePresets(): Flow<List<PresetEntity>>

    @Insert
    suspend fun insertPreset(preset: PresetEntity)

    @Update
    suspend fun updatePreset(preset: PresetEntity)

    @Query("UPDATE presets SET usageCount = usageCount + 1 WHERE id = :id")
    suspend fun incrementUsageCount(id: String)

    @Query("DELETE FROM presets WHERE id = :id")
    suspend fun deletePreset(id: String)
}

package com.omaster.app.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface PresetDao {
    @Query("SELECT * FROM presets ORDER BY lastAccessed DESC")
    fun getAllPresets(): Flow<List<PresetEntity>>

    @Query("SELECT * FROM presets WHERE isFavorite = 1 ORDER BY lastAccessed DESC")
    fun getFavoritePresets(): Flow<List<PresetEntity>>

    @Query("SELECT * FROM presets WHERE id = :presetId LIMIT 1")
    suspend fun getPresetById(presetId: String): PresetEntity?

    @Query("SELECT * FROM presets WHERE name LIKE '%' || :query || '%' ORDER BY lastAccessed DESC")
    fun searchPresets(query: String): Flow<List<PresetEntity>>

    @Query("SELECT * FROM presets WHERE deviceModel LIKE '%' || :device || '%' ORDER BY lastAccessed DESC")
    fun getPresetsByDevice(device: String): Flow<List<PresetEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPreset(preset: PresetEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPresets(presets: List<PresetEntity>)

    @Update
    suspend fun updatePreset(preset: PresetEntity)

    @Delete
    suspend fun deletePreset(preset: PresetEntity)

    @Query("UPDATE presets SET isFavorite = :isFavorite WHERE id = :presetId")
    suspend fun updateFavoriteStatus(presetId: String, isFavorite: Boolean)

    @Query("UPDATE presets SET lastAccessed = :timestamp WHERE id = :presetId")
    suspend fun updateLastAccessed(presetId: String, timestamp: Long)

    @Query("DELETE FROM presets")
    suspend fun clearAllPresets()
}

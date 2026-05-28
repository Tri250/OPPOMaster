package com.omaster.app.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.omaster.app.data.db.entity.CameraPresetEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PresetDao {
    @Query("SELECT * FROM camera_presets ORDER BY createTime DESC")
    fun getAllPresets(): Flow<List<CameraPresetEntity>>

    @Query("SELECT * FROM camera_presets WHERE id = :presetId")
    suspend fun getPresetById(presetId: String): CameraPresetEntity?

    @Query("""
        SELECT p.* FROM camera_presets p
        JOIN camera_presets_fts fts ON p.rowid = fts.rowid
        WHERE camera_presets_fts MATCH :query
        ORDER BY p.useCount DESC
    """)
    suspend fun searchPresets(query: String): List<CameraPresetEntity>

    @Query("SELECT * FROM camera_presets WHERE scene = :scene ORDER BY createTime DESC")
    suspend fun getPresetsByScene(scene: String): List<CameraPresetEntity>

    @Query("SELECT * FROM camera_presets WHERE filter = :filter ORDER BY createTime DESC")
    suspend fun getPresetsByFilter(filter: String): List<CameraPresetEntity>

    @Query("SELECT * FROM camera_presets WHERE tags LIKE '%' || :tag || '%'")
    suspend fun getPresetsByTag(tag: String): List<CameraPresetEntity>

    @Query("SELECT * FROM camera_presets WHERE isFavorite = 1 ORDER BY createTime DESC")
    fun getFavoritePresets(): Flow<List<CameraPresetEntity>>

    @Query("SELECT DISTINCT scene FROM camera_presets ORDER BY scene")
    suspend fun getAllScenes(): List<String>

    @Query("SELECT DISTINCT filter FROM camera_presets ORDER BY filter")
    suspend fun getAllFilters(): List<String>

    @Query("UPDATE camera_presets SET isFavorite = :isFavorite WHERE id = :presetId")
    suspend fun updateFavorite(presetId: String, isFavorite: Boolean)

    @Query("UPDATE camera_presets SET useCount = useCount + 1 WHERE id = :presetId")
    suspend fun incrementUseCount(presetId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPreset(preset: CameraPresetEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPresets(presets: List<CameraPresetEntity>)

    @Query("DELETE FROM camera_presets WHERE id = :presetId")
    suspend fun deletePreset(presetId: String)

    @Query("DELETE FROM camera_presets")
    suspend fun deleteAllPresets()
}
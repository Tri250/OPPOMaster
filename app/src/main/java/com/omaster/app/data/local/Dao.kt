package com.omaster.app.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * 预设数据访问对象
 */
@Dao
interface PresetDao {
    @Query("SELECT * FROM presets ORDER BY createdAt DESC")
    fun getAllPresets(): Flow<List<PresetEntity>>

    @Query("SELECT * FROM presets WHERE id = :id")
    suspend fun getPresetById(id: String): PresetEntity?

    @Query("SELECT * FROM presets WHERE brand = :brand")
    fun getPresetsByBrand(brand: String): Flow<List<PresetEntity>>

    @Query("SELECT * FROM presets WHERE isFavorite = 1")
    fun getFavoritePresets(): Flow<List<PresetEntity>>

    @Query("SELECT * FROM presets WHERE name LIKE '%' || :query || '%' OR description LIKE '%' || :query || '%'")
    fun searchPresets(query: String): Flow<List<PresetEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPreset(preset: PresetEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPresets(presets: List<PresetEntity>)

    @Update
    suspend fun updatePreset(preset: PresetEntity)

    @Delete
    suspend fun deletePreset(preset: PresetEntity)

    @Query("DELETE FROM presets")
    suspend fun deleteAllPresets()

    @Query("SELECT COUNT(*) FROM presets")
    suspend fun getPresetCount(): Int
}

/**
 * 相机配置数据访问对象
 */
@Dao
interface CameraConfigDao {
    @Query("SELECT * FROM camera_configs ORDER BY createdAt DESC")
    fun getAllConfigs(): Flow<List<CameraConfigEntity>>

    @Query("SELECT * FROM camera_configs WHERE id = :id")
    suspend fun getConfigById(id: String): CameraConfigEntity?

    @Query("SELECT * FROM camera_configs WHERE isFavorite = 1")
    fun getFavoriteConfigs(): Flow<List<CameraConfigEntity>>

    @Query("SELECT * FROM camera_configs WHERE category = :category")
    fun getConfigsByCategory(category: String): Flow<List<CameraConfigEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConfig(config: CameraConfigEntity)

    @Update
    suspend fun updateConfig(config: CameraConfigEntity)

    @Delete
    suspend fun deleteConfig(config: CameraConfigEntity)

    @Query("DELETE FROM camera_configs")
    suspend fun deleteAllConfigs()
}

/**
 * 用户资料数据访问对象
 */
@Dao
interface UserProfileDao {
    @Query("SELECT * FROM user_profiles WHERE id = :id")
    suspend fun getUserProfile(id: String): UserProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserProfile(profile: UserProfileEntity)

    @Update
    suspend fun updateUserProfile(profile: UserProfileEntity)

    @Delete
    suspend fun deleteUserProfile(profile: UserProfileEntity)
}

/**
 * 搜索历史数据访问对象
 */
@Dao
interface SearchHistoryDao {
    @Query("SELECT * FROM search_history ORDER BY searchedAt DESC LIMIT :limit")
    fun getRecentSearches(limit: Int): Flow<List<SearchHistoryEntity>>

    @Insert
    suspend fun insertSearch(search: SearchHistoryEntity)

    @Query("DELETE FROM search_history WHERE searchedAt < :timestamp")
    suspend fun deleteOldSearches(timestamp: Long)

    @Query("DELETE FROM search_history")
    suspend fun clearAllSearches()
}

/**
 * 收藏数据访问对象
 */
@Dao
interface FavoriteDao {
    @Query("SELECT * FROM favorites ORDER BY addedAt DESC")
    fun getAllFavorites(): Flow<List<FavoriteEntity>>

    @Query("SELECT * FROM favorites WHERE folderId = :folderId")
    fun getFavoritesByFolder(folderId: String): Flow<List<FavoriteEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavorite(favorite: FavoriteEntity)

    @Delete
    suspend fun deleteFavorite(favorite: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE presetId = :presetId")
    suspend fun deleteFavoriteByPresetId(presetId: String)
}

/**
 * 使用日志数据访问对象
 */
@Dao
interface UsageLogDao {
    @Query("SELECT * FROM usage_logs WHERE presetId = :presetId ORDER BY createdAt DESC")
    fun getLogsByPreset(presetId: String): Flow<List<UsageLogEntity>>

    @Insert
    suspend fun insertLog(log: UsageLogEntity)

    @Query("DELETE FROM usage_logs WHERE createdAt < :timestamp")
    suspend fun deleteOldLogs(timestamp: Long)

    @Query("SELECT COUNT(*) FROM usage_logs WHERE presetId = :presetId AND action = :action")
    suspend fun getActionCount(presetId: String, action: String): Int
}

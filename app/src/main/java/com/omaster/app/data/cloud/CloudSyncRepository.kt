package com.omaster.app.data.cloud

import com.omaster.app.data.db.DatabaseProvider
import com.omaster.app.data.db.entity.CameraPresetEntity
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CloudSyncRepository @Inject constructor(
    private val supabaseClientProvider: SupabaseClientProvider,
    private val databaseProvider: DatabaseProvider
) {
    private val gson = Gson()

    suspend fun syncUserPresets(userId: String, localPresets: List<CameraPresetEntity>) {
        withContext(Dispatchers.IO) {
            try {
                val client = supabaseClientProvider.client
                val table = client.postgrest[SupabaseConfig.USER_PRESETS_TABLE]

                localPresets.forEach { preset ->
                    val presetJson = gson.toJson(preset)
                    
                    table.upsert(
                        mapOf(
                            "user_id" to userId,
                            "preset_id" to preset.id,
                            "preset_data" to presetJson,
                            "update_time" to System.currentTimeMillis()
                        )
                    )
                }
                
                Timber.d("Successfully synced ${localPresets.size} presets for user $userId")
            } catch (e: Exception) {
                Timber.e(e, "Failed to sync user presets")
            }
        }
    }

    suspend fun restoreUserPresets(userId: String): List<CameraPresetEntity> {
        return withContext(Dispatchers.IO) {
            try {
                val client = supabaseClientProvider.client
                val table = client.postgrest[SupabaseConfig.USER_PRESETS_TABLE]
                
                val result = table.select {
                    filter { "user_id" eq userId }
                }
                
                val presets = mutableListOf<CameraPresetEntity>()
                result.decodeList<Map<String, Any>>().forEach { row ->
                    val presetData = row["preset_data"] as? String
                    presetData?.let {
                        try {
                            val preset = gson.fromJson(it, CameraPresetEntity::class.java)
                            presets.add(preset)
                        } catch (e: Exception) {
                            Timber.e(e, "Failed to parse preset data")
                        }
                    }
                }
                
                Timber.d("Successfully restored ${presets.size} presets for user $userId")
                presets
            } catch (e: Exception) {
                Timber.e(e, "Failed to restore user presets")
                emptyList()
            }
        }
    }

    suspend fun syncUserFavorites(userId: String, favoriteIds: List<String>) {
        withContext(Dispatchers.IO) {
            try {
                val client = supabaseClientProvider.client
                val table = client.postgrest[SupabaseConfig.USER_FAVORITES_TABLE]

                table.delete {
                    filter { "user_id" eq userId }
                }

                favoriteIds.forEach { presetId ->
                    table.insert(
                        mapOf(
                            "user_id" to userId,
                            "preset_id" to presetId,
                            "created_at" to System.currentTimeMillis()
                        )
                    )
                }
                
                Timber.d("Successfully synced ${favoriteIds.size} favorites for user $userId")
            } catch (e: Exception) {
                Timber.e(e, "Failed to sync user favorites")
            }
        }
    }

    suspend fun restoreUserFavorites(userId: String): List<String> {
        return withContext(Dispatchers.IO) {
            try {
                val client = supabaseClientProvider.client
                val table = client.postgrest[SupabaseConfig.USER_FAVORITES_TABLE]
                
                val result = table.select {
                    filter { "user_id" eq userId }
                }
                
                val favoriteIds = mutableListOf<String>()
                result.decodeList<Map<String, Any>>().forEach { row ->
                    val presetId = row["preset_id"] as? String
                    presetId?.let { favoriteIds.add(it) }
                }
                
                Timber.d("Successfully restored ${favoriteIds.size} favorites for user $userId")
                favoriteIds
            } catch (e: Exception) {
                Timber.e(e, "Failed to restore user favorites")
                emptyList()
            }
        }
    }

    suspend fun backupAllData(userId: String) {
        withContext(Dispatchers.IO) {
            try {
                val allPresets = databaseProvider.database.presetDao().getAllPresets().first()
                syncUserPresets(userId, allPresets)
                
                Timber.d("Full data backup completed for user $userId")
            } catch (e: Exception) {
                Timber.e(e, "Failed to backup all data")
            }
        }
    }

    suspend fun restoreAllData(userId: String) {
        withContext(Dispatchers.IO) {
            try {
                val cloudPresets = restoreUserPresets(userId)
                if (cloudPresets.isNotEmpty()) {
                    databaseProvider.database.presetDao().insertPresets(cloudPresets)
                    Timber.d("Successfully restored ${cloudPresets.size} presets from cloud")
                }
                
                val cloudFavorites = restoreUserFavorites(userId)
                Timber.d("Successfully restored ${cloudFavorites.size} favorites from cloud")
            } catch (e: Exception) {
                Timber.e(e, "Failed to restore all data")
            }
        }
    }
}
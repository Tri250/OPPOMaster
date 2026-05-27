package com.omaster.app.service

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.widget.Toast
import com.google.gson.Gson
import com.omaster.app.model.Preset
import com.omaster.app.util.ExportData
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import timber.log.Timber
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CloudSyncService @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val gson = Gson()
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val baseUrl = "https://api.omaster.example.com/v1"
    
    data class SyncResult(
        val success: Boolean,
        val message: String,
        val presets: List<Preset>? = null
    )

    data class SyncStatus(
        val lastSyncTime: Long = 0L,
        val isSyncing: Boolean = false,
        val totalPresets: Int = 0,
        val syncedPresets: Int = 0
    )

    suspend fun syncPresets(localPresets: List<Preset>): SyncResult = withContext(Dispatchers.IO) {
        try {
            if (!isNetworkAvailable()) {
                return@withContext SyncResult(
                    success = false,
                    message = "网络不可用，请检查网络连接"
                )
            }

            val jsonString = gson.toJson(localPresets)
            val requestBody = jsonString.toRequestBody("application/json".toMediaType())
            
            val request = Request.Builder()
                .url("$baseUrl/presets/sync")
                .post(requestBody)
                .addHeader("Content-Type", "application/json")
                .build()

            val response = client.newCall(request).execute()
            
            if (response.isSuccessful) {
                val responseBody = response.body?.string()
                val syncedPresets = if (responseBody != null) {
                    try {
                        gson.fromJson(responseBody, Array<Preset>::class.java).toList()
                    } catch (e: Exception) {
                        localPresets
                    }
                } else {
                    localPresets
                }
                
                saveSyncTimestamp()
                
                SyncResult(
                    success = true,
                    message = "同步成功",
                    presets = syncedPresets
                )
            } else {
                saveLocalBackup(localPresets)
                SyncResult(
                    success = false,
                    message = "同步失败: ${response.code}"
                )
            }
        } catch (e: Exception) {
            Timber.e(e, "同步预设失败")
            saveLocalBackup(localPresets)
            SyncResult(
                success = false,
                message = "同步失败: ${e.message}"
            )
        }
    }

    suspend fun uploadPreset(preset: Preset): SyncResult = withContext(Dispatchers.IO) {
        try {
            if (!isNetworkAvailable()) {
                return@withContext SyncResult(
                    success = false,
                    message = "网络不可用"
                )
            }

            val jsonString = gson.toJson(preset)
            val requestBody = jsonString.toRequestBody("application/json".toMediaType())
            
            val request = Request.Builder()
                .url("$baseUrl/presets")
                .post(requestBody)
                .addHeader("Content-Type", "application/json")
                .build()

            val response = client.newCall(request).execute()
            
            if (response.isSuccessful) {
                SyncResult(
                    success = true,
                    message = "上传成功"
                )
            } else {
                SyncResult(
                    success = false,
                    message = "上传失败: ${response.code}"
                )
            }
        } catch (e: Exception) {
            Timber.e(e, "上传预设失败")
            SyncResult(
                success = false,
                message = "上传失败: ${e.message}"
            )
        }
    }

    suspend fun downloadPresets(): SyncResult = withContext(Dispatchers.IO) {
        try {
            if (!isNetworkAvailable()) {
                return@withContext SyncResult(
                    success = false,
                    message = "网络不可用"
                )
            }

            val request = Request.Builder()
                .url("$baseUrl/presets")
                .get()
                .build()

            val response = client.newCall(request).execute()
            
            if (response.isSuccessful) {
                val responseBody = response.body?.string()
                val presets = if (responseBody != null) {
                    try {
                        val exportData = gson.fromJson(responseBody, ExportData::class.java)
                        exportData.presets
                    } catch (e: Exception) {
                        try {
                            gson.fromJson(responseBody, Array<Preset>::class.java).toList()
                        } catch (e: Exception) {
                            emptyList()
                        }
                    }
                } else {
                    emptyList()
                }
                
                SyncResult(
                    success = true,
                    message = "下载成功",
                    presets = presets
                )
            } else {
                SyncResult(
                    success = false,
                    message = "下载失败: ${response.code}"
                )
            }
        } catch (e: Exception) {
            Timber.e(e, "下载预设失败")
            SyncResult(
                success = false,
                message = "下载失败: ${e.message}"
            )
        }
    }

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private fun saveSyncTimestamp() {
        val sharedPreferences = context.getSharedPreferences("sync_prefs", Context.MODE_PRIVATE)
        sharedPreferences.edit()
            .putLong("last_sync_time", System.currentTimeMillis())
            .apply()
    }

    fun getLastSyncTime(): Long {
        val sharedPreferences = context.getSharedPreferences("sync_prefs", Context.MODE_PRIVATE)
        return sharedPreferences.getLong("last_sync_time", 0L)
    }

    private fun saveLocalBackup(presets: List<Preset>) {
        try {
            val backupDir = File(context.filesDir, "backups")
            if (!backupDir.exists()) {
                backupDir.mkdirs()
            }
            
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val backupFile = File(backupDir, "backup_$timestamp.json")
            
            val exportData = ExportData(
                version = 1,
                exportTime = System.currentTimeMillis(),
                presets = presets
            )
            
            val jsonString = gson.toJson(exportData)
            FileOutputStream(backupFile).use { fos ->
                fos.write(jsonString.toByteArray())
            }
            
            cleanOldBackups()
        } catch (e: Exception) {
            Timber.e(e, "保存本地备份失败")
        }
    }

    private fun cleanOldBackups() {
        try {
            val backupDir = File(context.filesDir, "backups")
            val files = backupDir.listFiles() ?: return
            
            val sortedFiles = files.sortedByDescending { it.lastModified() }
            if (sortedFiles.size > 5) {
                sortedFiles.drop(5).forEach { it.delete() }
            }
        } catch (e: Exception) {
            Timber.e(e, "清理旧备份失败")
        }
    }

    suspend fun restoreFromBackup(): List<Preset>? = withContext(Dispatchers.IO) {
        try {
            val backupDir = File(context.filesDir, "backups")
            val files = backupDir.listFiles() ?: return@withContext null
            
            val latestBackup = files.maxByOrNull { it.lastModified() } ?: return@withContext null
            
            val jsonString = FileInputStream(latestBackup).bufferedReader().readText()
            val exportData = gson.fromJson(jsonString, ExportData::class.java)
            exportData.presets
        } catch (e: Exception) {
            Timber.e(e, "从备份恢复失败")
            null
        }
    }

    fun getBackupFiles(): List<File> {
        val backupDir = File(context.filesDir, "backups")
        return backupDir.listFiles()?.sortedByDescending { it.lastModified() } ?: emptyList()
    }
}

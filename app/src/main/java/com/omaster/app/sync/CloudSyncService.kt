package com.omaster.app.sync

import android.content.Context
import com.omaster.app.data.PreferencesDataStore
import com.omaster.app.model.CameraParams
import com.omaster.app.model.Preset
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import timber.log.Timber
import java.io.Closeable
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 云端同步服务
 * 
 * 负责预设数据的云端同步功能，支持：
 * - 用户登录/注册/注销
 * - 预设数据上传与下载
 * - 变更追踪与冲突检测
 * - 离线模式与同步状态管理
 * 
 * 使用 OkHttp 进行网络请求，StateFlow 管理同步状态。
 */
@Singleton
class CloudSyncService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferencesDataStore: PreferencesDataStore
) : Closeable {
    private val supervisorJob = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + supervisorJob)
    
    private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    private val _userProfile = MutableStateFlow<UserProfile?>(null)
    val userProfile: StateFlow<UserProfile?> = _userProfile.asStateFlow()

    private val _lastSyncTime = MutableStateFlow<Long?>(null)
    val lastSyncTime: StateFlow<Long?> = _lastSyncTime.asStateFlow()

    private val _pendingChanges = MutableStateFlow<List<SyncChange>>(emptyList())
    val pendingChanges: Flow<List<SyncChange>> = _pendingChanges.asStateFlow()

    sealed class SyncState {
        object Idle : SyncState()
        object Connecting : SyncState()
        data class Syncing(val progress: Int) : SyncState()
        data class Success(val message: String) : SyncState()
        data class Error(val message: String) : SyncState()
        object Offline : SyncState()
    }

    data class UserProfile(
        val userId: String,
        val username: String,
        val email: String?,
        val avatarUrl: String?,
        val subscriptionTier: SubscriptionTier,
        val createdAt: Long,
        val lastLoginAt: Long
    )

    enum class SubscriptionTier {
        FREE,
        PREMIUM,
        PRO
    }

    data class SyncChange(
        val id: String,
        val type: ChangeType,
        val presetId: String,
        val data: String,
        val timestamp: Long,
        val deviceId: String,
        val synced: Boolean = false
    )

    enum class ChangeType {
        CREATE,
        UPDATE,
        DELETE
    }

    data class SyncConflict(
        val presetId: String,
        val localVersion: Preset,
        val remoteVersion: Preset,
        val localTimestamp: Long,
        val remoteTimestamp: Long
    )

    private val BASE_URL = "https://api.xiaobangbang.app/v1"
    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    private val deviceId: String by lazy {
        preferencesDataStore.getDeviceId() ?: run {
            val newId = UUID.randomUUID().toString()
            scope.launch {
                preferencesDataStore.saveDeviceId(newId)
            }
            newId
        }
    }

    suspend fun login(email: String, password: String): Result<UserProfile> {
        return withContext(Dispatchers.IO) {
            try {
                _syncState.value = SyncState.Connecting
                
                val json = JSONObject().apply {
                    put("email", email)
                    put("password", password)
                    put("device_id", deviceId)
                }
                
                val requestBody = json.toString().toRequestBody("application/json".toMediaType())
                val request = Request.Builder()
                    .url("$BASE_URL/auth/login")
                    .post(requestBody)
                    .build()
                
                val response = okHttpClient.newCall(request).execute()
                
                try {
                    if (response.isSuccessful) {
                        val responseJson = JSONObject(response.body?.string() ?: "{}")
                        val user = parseUserProfile(responseJson.getJSONObject("user"))
                        val token = responseJson.getString("token")
                        
                        preferencesDataStore.saveAuthToken(token)
                        _userProfile.value = user
                        _syncState.value = SyncState.Success("登录成功")
                        
                        Result.success(user)
                    } else {
                        val error = response.message ?: "登录失败"
                        _syncState.value = SyncState.Error(error)
                        Result.failure(Exception(error))
                    }
                } finally {
                    response.body?.close()
                }
            } catch (e: Exception) {
                Timber.e(e, "Login failed")
                _syncState.value = SyncState.Error(e.message ?: "网络错误")
                Result.failure(e)
            }
        }
    }

    suspend fun register(email: String, password: String, username: String): Result<UserProfile> {
        return withContext(Dispatchers.IO) {
            try {
                _syncState.value = SyncState.Connecting
                
                val json = JSONObject().apply {
                    put("email", email)
                    put("password", password)
                    put("username", username)
                    put("device_id", deviceId)
                }
                
                val requestBody = json.toString().toRequestBody("application/json".toMediaType())
                val request = Request.Builder()
                    .url("$BASE_URL/auth/register")
                    .post(requestBody)
                    .build()
                
                val response = okHttpClient.newCall(request).execute()
                
                try {
                    if (response.isSuccessful) {
                        val responseJson = JSONObject(response.body?.string() ?: "{}")
                        val user = parseUserProfile(responseJson.getJSONObject("user"))
                        val token = responseJson.getString("token")
                        
                        preferencesDataStore.saveAuthToken(token)
                        _userProfile.value = user
                        _syncState.value = SyncState.Success("注册成功")
                        
                        Result.success(user)
                    } else {
                        val error = response.message ?: "注册失败"
                        _syncState.value = SyncState.Error(error)
                        Result.failure(Exception(error))
                    }
                } finally {
                    response.body?.close()
                }
            } catch (e: Exception) {
                Timber.e(e, "Registration failed")
                _syncState.value = SyncState.Error(e.message ?: "网络错误")
                Result.failure(e)
            }
        }
    }

    fun logout() {
        scope.launch {
            preferencesDataStore.clearAuthToken()
            _userProfile.value = null
            _syncState.value = SyncState.Idle
        }
    }

    suspend fun syncAll(): Result<SyncResult> {
        return withContext(Dispatchers.IO) {
            try {
                val token = preferencesDataStore.getAuthToken()
                if (token == null) {
                    return@withContext Result.failure(Exception("未登录"))
                }

                _syncState.value = SyncState.Syncing(0)

                // 1. 获取本地变更
                val pendingChanges = _pendingChanges.value.filter { !it.synced }
                _syncState.value = SyncState.Syncing(20)

                // 2. 上传本地变更
                val uploadResult = uploadChanges(pendingChanges, token)
                _syncState.value = SyncState.Syncing(50)

                // 3. 获取远程变更
                val remoteChanges = fetchRemoteChanges(token)
                _syncState.value = SyncState.Syncing(80)

                // 4. 合并变更
                val mergedResult = mergeChanges(remoteChanges)
                _syncState.value = SyncState.Syncing(100)

                // 5. 更新同步时间
                val syncTime = System.currentTimeMillis()
                _lastSyncTime.value = syncTime
                preferencesDataStore.saveLastSyncTime(syncTime)

                _syncState.value = SyncState.Success("同步完成")

                Result.success(SyncResult(
                    uploaded = uploadResult.size,
                    downloaded = remoteChanges.size,
                    conflicts = mergedResult.conflicts.size,
                    merged = mergedResult.presets.size
                ))
            } catch (e: Exception) {
                Timber.e(e, "Sync failed")
                _syncState.value = SyncState.Error(e.message ?: "同步失败")
                Result.failure(e)
            }
        }
    }

    suspend fun syncPreset(preset: Preset): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val token = preferencesDataStore.getAuthToken() ?: return@withContext Result.failure(Exception("未登录"))

                val json = presetToJson(preset)
                val requestBody = json.toString().toRequestBody("application/json".toMediaType())
                val request = Request.Builder()
                    .url("$BASE_URL/presets/${preset.id}")
                    .put(requestBody)
                    .addHeader("Authorization", "Bearer $token")
                    .build()

                val response = okHttpClient.newCall(request).execute()

                if (response.isSuccessful) {
                    Result.success(Unit)
                } else {
                    Result.failure(Exception("同步失败: ${response.code()}"))
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to sync preset")
                Result.failure(e)
            }
        }
    }

    private suspend fun uploadChanges(changes: List<SyncChange>, token: String): List<SyncChange> = coroutineScope {
        // 使用并发上传提高效率
        val deferredUploads = changes.map { change ->
            async {
                try {
                    val json = JSONObject().apply {
                        put("id", change.id)
                        put("type", change.type.name)
                        put("preset_id", change.presetId)
                        put("data", change.data)
                        put("timestamp", change.timestamp)
                        put("device_id", change.deviceId)
                    }
                    
                    val requestBody = json.toString().toRequestBody("application/json".toMediaType())
                    val request = Request.Builder()
                        .url("$BASE_URL/sync/upload")
                        .post(requestBody)
                        .addHeader("Authorization", "Bearer $token")
                        .build()
                    
                    val response = okHttpClient.newCall(request).execute()
                    
                    try {
                        if (response.isSuccessful) {
                            change.copy(synced = true)
                        } else {
                            null
                        }
                    } finally {
                        response.body?.close()
                    }
                } catch (e: Exception) {
                    Timber.e(e, "Failed to upload change: ${change.id}")
                    null
                }
            }
        }
        
        deferredUploads.awaitAll().filterNotNull()
    }

    private suspend fun fetchRemoteChanges(token: String): List<SyncChange> {
        val changes = mutableListOf<SyncChange>()
        
        try {
            val lastSync = _lastSyncTime.value ?: 0L
            val request = Request.Builder()
                .url("$BASE_URL/sync/changes?since=$lastSync")
                .get()
                .addHeader("Authorization", "Bearer $token")
                .build()
            
            val response = okHttpClient.newCall(request).execute()
            
            if (response.isSuccessful) {
                val jsonArray = JSONArray(response.body?.string() ?: "[]")
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    changes.add(SyncChange(
                        id = obj.getString("id"),
                        type = ChangeType.valueOf(obj.getString("type")),
                        presetId = obj.getString("preset_id"),
                        data = obj.getString("data"),
                        timestamp = obj.getLong("timestamp"),
                        deviceId = obj.getString("device_id")
                    ))
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to fetch remote changes")
        }
        
        return changes
    }

    data class MergeResult(
        val presets: List<Preset>,
        val conflicts: List<SyncConflict>
    )

    private suspend fun mergeChanges(remoteChanges: List<SyncChange>): MergeResult {
        val presets = mutableListOf<Preset>()
        val conflicts = mutableListOf<SyncConflict>()
        
        remoteChanges.forEach { change ->
            try {
                val remotePreset = parsePresetFromJson(JSONObject(change.data))
                val localPreset = loadLocalPreset(change.presetId)
                
                if (localPreset != null) {
                    // 检测冲突 - 使用内容比较而非引用比较
                    val localParams = localPreset.cameraParams
                    val remoteParams = remotePreset.cameraParams
                    val hasConflict = when {
                        localParams == null && remoteParams != null -> true
                        localParams != null && remoteParams == null -> true
                        localParams != null && remoteParams != null -> !cameraParamsContentEquals(localParams, remoteParams)
                        else -> false
                    }
                    
                    if (hasConflict) {
                        conflicts.add(SyncConflict(
                            presetId = change.presetId,
                            localVersion = localPreset,
                            remoteVersion = remotePreset,
                            localTimestamp = localPreset.hashCode().toLong(),
                            remoteTimestamp = change.timestamp
                        ))
                    }
                }
                
                presets.add(remotePreset)
                saveLocalPreset(remotePreset)
            } catch (e: Exception) {
                Timber.e(e, "Failed to merge change: ${change.id}")
            }
        }
        
        return MergeResult(presets, conflicts)
    }
    
    // 内容比较 CameraParams
    private fun cameraParamsContentEquals(local: CameraParams, remote: CameraParams): Boolean {
        return local.mode == remote.mode &&
               local.iso == remote.iso &&
               local.shutter == remote.shutter &&
               local.ev == remote.ev &&
               local.wb == remote.wb &&
               local.focal_length == remote.focal_length &&
               local.aperture == remote.aperture &&
               local.filter == remote.filter &&
               local.hasselblad_hncs == remote.hasselblad_hncs
    }

    private fun parseUserProfile(json: JSONObject): UserProfile {
        return UserProfile(
            userId = json.getString("user_id"),
            username = json.getString("username"),
            email = json.optString("email"),
            avatarUrl = json.optString("avatar_url"),
            subscriptionTier = try {
                SubscriptionTier.valueOf(json.getString("subscription_tier").uppercase())
            } catch (e: Exception) {
                SubscriptionTier.FREE
            },
            createdAt = json.optLong("created_at", System.currentTimeMillis()),
            lastLoginAt = json.optLong("last_login_at", System.currentTimeMillis())
        )
    }

    private fun presetToJson(preset: Preset): JSONObject {
        return JSONObject().apply {
            put("id", preset.id)
            put("name", preset.name)
            put("device_model", preset.deviceModel)
            put("cover_path", preset.coverPath)
            put("source", preset.source)
            preset.cameraParams?.let { params ->
                put("camera_params", JSONObject().apply {
                    put("mode", params.mode)
                    put("iso", params.iso)
                    put("shutter", params.shutter)
                    put("ev", params.ev)
                    put("wb", params.wb)
                    put("focal_length", params.focal_length)
                    put("aperture", params.aperture)
                    put("filter", params.filter)
                    put("hncs", params.hasselblad_hncs)
                })
            } ?: put("camera_params", JSONObject.NULL)
        }
    }

    private fun parsePresetFromJson(json: JSONObject): Preset {
        val paramsJson = json.optJSONObject("camera_params") ?: JSONObject()
        
        return Preset(
            id = json.getString("id"),
            name = json.getString("name"),
            deviceModel = json.optString("device_model"),
            coverPath = json.optString("cover_path"),
            source = json.optString("source", "cloud"),
            cameraParams = CameraParams(
                mode = paramsJson.optString("mode", "哈苏大师"),
                iso = paramsJson.optInt("iso", 100),
                shutter = paramsJson.optString("shutter", "1/125"),
                ev = paramsJson.optString("ev", "0"),
                wb = paramsJson.optString("wb", "5500K"),
                focal_length = paramsJson.optString("focal_length", "24mm"),
                aperture = paramsJson.optString("aperture", "f/1.8"),
                filter = paramsJson.optString("filter"),
                hasselblad_hncs = paramsJson.optBoolean("hncs", false)
            ),
            sections = emptyList()
        )
    }

    private fun loadLocalPreset(presetId: String): Preset? {
        // TODO: 从本地数据库加载预设（待 Room 集成后实现）
        // 当前云端为唯一数据源，本地缓存由 PresetRepository 内存持有
        Timber.w("loadLocalPreset 未实现: presetId=$presetId")
        return null
    }

    private fun saveLocalPreset(preset: Preset) {
        // TODO: 将预设保存到本地数据库（待 Room 集成后实现）
        // 当前云端为唯一数据源，预设修改后由云端同步流程保证一致性
        Timber.w("saveLocalPreset 未实现: presetId=${preset.id}")
    }

    fun addPendingChange(change: SyncChange) {
        _pendingChanges.value = _pendingChanges.value + change
    }

    suspend fun resolveConflict(
        conflict: SyncConflict,
        resolution: ConflictResolution
    ): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val resolvedPreset = when (resolution) {
                    ConflictResolution.KEEP_LOCAL -> conflict.localVersion
                    ConflictResolution.KEEP_REMOTE -> conflict.remoteVersion
                    ConflictResolution.MERGE -> mergePresets(conflict.localVersion, conflict.remoteVersion)
                }
                
                syncPreset(resolvedPreset)
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    private fun mergePresets(local: Preset, remote: Preset): Preset {
        val mergedCameraParams = when {
            local.cameraParams != null && remote.cameraParams != null -> {
                local.cameraParams.copy(
                    mode = if (local.cameraParams.mode != "哈苏大师") local.cameraParams.mode else remote.cameraParams.mode
                )
            }
            local.cameraParams != null -> local.cameraParams
            remote.cameraParams != null -> remote.cameraParams
            else -> null
        }
        return local.copy(cameraParams = mergedCameraParams)
    }

    enum class ConflictResolution {
        KEEP_LOCAL,
        KEEP_REMOTE,
        MERGE
    }

    data class SyncResult(
        val uploaded: Int,
        val downloaded: Int,
        val conflicts: Int,
        val merged: Int
    )

    fun clearPendingChanges() {
        // 使用 update 确保原子性
        _pendingChanges.update { current ->
            current.filter { !it.synced }
        }
    }

    fun cancelSync() {
        scope.coroutineContext.cancelChildren()
        _syncState.value = SyncState.Idle
    }

    fun destroy() {
        close()
    }

    override fun close() {
        try {
            scope.coroutineContext.cancelChildren()
            supervisorJob.cancel()
            okHttpClient.dispatcher.executorService.shutdown()
            okHttpClient.connectionPool.evictAll()
            okHttpClient.cache?.close()
        } catch (e: Exception) {
            Timber.e(e, "Failed to close CloudSyncService")
        }
    }
}
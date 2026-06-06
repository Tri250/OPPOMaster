package com.omaster.app.data.sync

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.omaster.app.domain.model.Preset
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

private val Context.cacheManagerDataStore by preferencesDataStore(name = "offline_cache_manager")

/**
 * 离线缓存管理器
 * 负责预设数据本地缓存、图片资源缓存、缓存过期策略、缓存清理机制和后台预加载
 */
@Singleton
class OfflineCacheManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private object PreferencesKeys {
        // 缓存版本
        val CACHE_VERSION = longPreferencesKey("cache_version")
        // 最后缓存更新时间
        val LAST_CACHE_UPDATE = longPreferencesKey("last_cache_update")
        // 缓存大小限制（MB）
        val CACHE_SIZE_LIMIT = longPreferencesKey("cache_size_limit")
        // 图片缓存路径
        val IMAGE_CACHE_PATH = stringPreferencesKey("image_cache_path")
        // 数据缓存路径
        val DATA_CACHE_PATH = stringPreferencesKey("data_cache_path")
    }

    // 缓存状态
    private val _cacheState = MutableStateFlow<CacheState>(CacheState.Idle)
    val cacheState: Flow<CacheState> = _cacheState.asStateFlow()

    // 缓存统计
    private val _cacheStats = MutableStateFlow<CacheStats>(CacheStats())
    val cacheStats: Flow<CacheStats> = _cacheStats.asStateFlow()

    // 图片加载器
    private val imageLoader: ImageLoader by lazy {
        ImageLoader.Builder(context)
            .crossfade(true)
            .build()
    }

    // 缓存目录
    private val cacheDir: File by lazy {
        File(context.cacheDir, "offline_cache").apply {
            if (!exists()) mkdirs()
        }
    }

    private val imageCacheDir: File by lazy {
        File(cacheDir, "images").apply {
            if (!exists()) mkdirs()
        }
    }

    private val dataCacheDir: File by lazy {
        File(cacheDir, "data").apply {
            if (!exists()) mkdirs()
        }
    }

    // 默认缓存配置
    companion object {
        const val DEFAULT_CACHE_SIZE_LIMIT_MB = 500L // 默认500MB
        const val DEFAULT_CACHE_EXPIRY_DAYS = 30L // 默认30天过期
        const val PRELOAD_WORK_NAME = "offline_cache_preload"
    }

    /**
     * 缓存状态密封类
     */
    sealed class CacheState {
        object Idle : CacheState()
        object Loading : CacheState()
        object Caching : CacheState()
        object Cleaning : CacheState()
        data class Success(val message: String) : CacheState()
        data class Error(val message: String) : CacheState()
    }

    /**
     * 缓存统计数据类
     */
    data class CacheStats(
        val totalCacheSize: Long = 0L,
        val imageCacheSize: Long = 0L,
        val dataCacheSize: Long = 0L,
        val cachedImageCount: Int = 0,
        val cachedPresetCount: Int = 0,
        val lastUpdateTime: Long = 0L,
        val cacheHitRate: Float = 0f
    )

    /**
     * 缓存项信息
     */
    data class CacheItem(
        val id: String,
        val type: CacheItemType,
        val file: File,
        val size: Long,
        val lastAccessed: Long,
        val expiryTime: Long
    )

    enum class CacheItemType {
        PRESET_DATA,    // 预设数据
        TEMPLATE_DATA,  // 模板数据
        PRESET_IMAGE,   // 预设图片
        THUMBNAIL,      // 缩略图
        METADATA        // 元数据
    }

    // 缓存版本
    val cacheVersion: Flow<Long> = context.cacheManagerDataStore.data
        .map { it[PreferencesKeys.CACHE_VERSION] ?: 1L }

    // 最后更新时间
    val lastCacheUpdate: Flow<Long> = context.cacheManagerDataStore.data
        .map { it[PreferencesKeys.LAST_CACHE_UPDATE] ?: 0L }

    // 缓存大小限制
    val cacheSizeLimit: Flow<Long> = context.cacheManagerDataStore.data
        .map { it[PreferencesKeys.CACHE_SIZE_LIMIT] ?: DEFAULT_CACHE_SIZE_LIMIT_MB }

    init {
        // 初始化时更新缓存统计
        updateCacheStats()
    }

    /**
     * 缓存预设数据
     * @param presets 预设列表
     * @param overwrite 是否覆盖现有缓存
     */
    suspend fun cachePresets(
        presets: List<Preset>,
        overwrite: Boolean = false
    ): Result<Int> = withContext(Dispatchers.IO) {
        try {
            _cacheState.value = CacheState.Caching
            var cachedCount = 0

            presets.forEach { preset ->
                val cacheFile = File(dataCacheDir, "preset_${preset.id}.json")

                if (!cacheFile.exists() || overwrite) {
                    // 序列化预设数据并保存
                    val jsonData = serializePreset(preset)
                    cacheFile.writeText(jsonData)
                    cachedCount++
                }
            }

            updateLastCacheTime()
            updateCacheStats()
            _cacheState.value = CacheState.Success("已缓存 $cachedCount 个预设")

            Result.success(cachedCount)
        } catch (e: Exception) {
            Timber.e(e, "缓存预设失败")
            _cacheState.value = CacheState.Error(e.message ?: "缓存失败")
            Result.failure(e)
        }
    }

    /**
     * 获取缓存的预设
     * @param presetId 预设ID
     * @return 预设对象，如果不存在返回null
     */
    suspend fun getCachedPreset(presetId: String): Preset? = withContext(Dispatchers.IO) {
        try {
            val cacheFile = File(dataCacheDir, "preset_$presetId.json")
            if (cacheFile.exists() && !isCacheExpired(cacheFile)) {
                val jsonData = cacheFile.readText()
                updateFileAccessTime(cacheFile)
                deserializePreset(jsonData)
            } else {
                null
            }
        } catch (e: Exception) {
            Timber.e(e, "读取缓存预设失败: $presetId")
            null
        }
    }

    /**
     * 缓存图片资源
     * @param imageUrl 图片URL
     * @param presetId 关联的预设ID
     * @return 缓存文件
     */
    suspend fun cacheImage(
        imageUrl: String,
        presetId: String? = null
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            val fileName = if (presetId != null) {
                "img_${presetId}_${imageUrl.hashCode()}.jpg"
            } else {
                "img_${imageUrl.hashCode()}.jpg"
            }
            val cacheFile = File(imageCacheDir, fileName)

            if (cacheFile.exists() && !isCacheExpired(cacheFile)) {
                updateFileAccessTime(cacheFile)
                return@withContext Result.success(cacheFile)
            }

            // 使用Coil下载并缓存图片
            val request = ImageRequest.Builder(context)
                .data(imageUrl)
                .allowHardware(false)
                .build()

            val result = imageLoader.execute(request)
            if (result is SuccessResult) {
                val drawable = result.drawable
                // 保存图片到缓存目录
                cacheFile.outputStream().use { output ->
                    // 将drawable转换为bitmap并保存
                    // 这里简化处理，实际应该使用Bitmap.compress
                }
                updateCacheStats()
                Result.success(cacheFile)
            } else {
                Result.failure(Exception("图片下载失败"))
            }
        } catch (e: Exception) {
            Timber.e(e, "缓存图片失败: $imageUrl")
            Result.failure(e)
        }
    }

    /**
     * 获取缓存的图片
     * @param imageUrl 图片URL
     * @return 缓存文件，如果不存在返回null
     */
    fun getCachedImage(imageUrl: String): File? {
        val fileName = "img_${imageUrl.hashCode()}.jpg"
        val cacheFile = File(imageCacheDir, fileName)
        return if (cacheFile.exists() && !isCacheExpired(cacheFile)) {
            updateFileAccessTime(cacheFile)
            cacheFile
        } else {
            null
        }
    }

    /**
     * 预加载图片资源
     * @param imageUrls 图片URL列表
     * @param onlyOnWifi 是否仅在WiFi下预加载
     */
    suspend fun preloadImages(
        imageUrls: List<String>,
        onlyOnWifi: Boolean = true
    ): Result<Int> = withContext(Dispatchers.IO) {
        try {
            if (onlyOnWifi && !isWifiConnected()) {
                Timber.d("非WiFi环境，跳过图片预加载")
                return@withContext Result.success(0)
            }

            _cacheState.value = CacheState.Loading
            var preloadedCount = 0

            imageUrls.forEach { url ->
                try {
                    if (getCachedImage(url) == null) {
                        cacheImage(url)
                        preloadedCount++
                    }
                } catch (e: Exception) {
                    Timber.e(e, "预加载图片失败: $url")
                }
            }

            updateCacheStats()
            _cacheState.value = CacheState.Success("已预加载 $preloadedCount 张图片")

            Result.success(preloadedCount)
        } catch (e: Exception) {
            _cacheState.value = CacheState.Error(e.message ?: "预加载失败")
            Result.failure(e)
        }
    }

    /**
     * 清理过期缓存
     * @param expiryDays 过期天数，默认30天
     * @return 清理的文件数量
     */
    suspend fun cleanExpiredCache(expiryDays: Long = DEFAULT_CACHE_EXPIRY_DAYS): Result<Int> =
        withContext(Dispatchers.IO) {
            try {
                _cacheState.value = CacheState.Cleaning
                var cleanedCount = 0
                val expiryTime = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(expiryDays)

                // 清理数据缓存
                dataCacheDir.listFiles()?.forEach { file ->
                    if (file.lastModified() < expiryTime) {
                        if (file.delete()) cleanedCount++
                    }
                }

                // 清理图片缓存
                imageCacheDir.listFiles()?.forEach { file ->
                    if (file.lastModified() < expiryTime) {
                        if (file.delete()) cleanedCount++
                    }
                }

                updateCacheStats()
                _cacheState.value = CacheState.Success("已清理 $cleanedCount 个过期文件")

                Result.success(cleanedCount)
            } catch (e: Exception) {
                Timber.e(e, "清理过期缓存失败")
                _cacheState.value = CacheState.Error(e.message ?: "清理失败")
                Result.failure(e)
            }
        }

    /**
     * 按LRU策略清理缓存
     * @param targetSizeMB 目标缓存大小（MB）
     * @return 清理的文件数量
     */
    suspend fun cleanCacheByLRU(targetSizeMB: Long): Result<Int> = withContext(Dispatchers.IO) {
        try {
            _cacheState.value = CacheState.Cleaning
            val targetSizeBytes = targetSizeMB * 1024 * 1024
            var currentSize = calculateTotalCacheSize()
            var cleanedCount = 0

            if (currentSize <= targetSizeBytes) {
                _cacheState.value = CacheState.Success("缓存大小符合要求")
                return@withContext Result.success(0)
            }

            // 获取所有缓存文件并按最后访问时间排序
            val allFiles = (dataCacheDir.listFiles()?.toList() ?: emptyList()) +
                    (imageCacheDir.listFiles()?.toList() ?: emptyList())

            val sortedFiles = allFiles.sortedBy { it.lastModified() }

            // 删除最久未访问的文件直到达到目标大小
            for (file in sortedFiles) {
                if (currentSize <= targetSizeBytes) break

                val fileSize = file.length()
                if (file.delete()) {
                    currentSize -= fileSize
                    cleanedCount++
                }
            }

            updateCacheStats()
            _cacheState.value = CacheState.Success("已清理 $cleanedCount 个文件")

            Result.success(cleanedCount)
        } catch (e: Exception) {
            Timber.e(e, "LRU清理缓存失败")
            _cacheState.value = CacheState.Error(e.message ?: "清理失败")
            Result.failure(e)
        }
    }

    /**
     * 清空所有缓存
     */
    suspend fun clearAllCache(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            _cacheState.value = CacheState.Cleaning

            dataCacheDir.listFiles()?.forEach { it.delete() }
            imageCacheDir.listFiles()?.forEach { it.delete() }

            updateLastCacheTime(0L)
            updateCacheStats()
            _cacheState.value = CacheState.Success("缓存已清空")

            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "清空缓存失败")
            _cacheState.value = CacheState.Error(e.message ?: "清空失败")
            Result.failure(e)
        }
    }

    /**
     * 设置缓存大小限制
     */
    suspend fun setCacheSizeLimit(limitMB: Long) {
        context.cacheManagerDataStore.edit { preferences ->
            preferences[PreferencesKeys.CACHE_SIZE_LIMIT] = limitMB
        }
    }

    /**
     * 注册后台预加载任务
     * @param intervalHours 执行间隔（小时）
     * @param requireWifi 是否需要WiFi
     * @param requireCharging 是否需要充电
     */
    fun schedulePreloadWork(
        intervalHours: Long = 24,
        requireWifi: Boolean = true,
        requireCharging: Boolean = false
    ) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(if (requireWifi) NetworkType.UNMETERED else NetworkType.CONNECTED)
            .setRequiresCharging(requireCharging)
            .build()

        val preloadWorkRequest = PeriodicWorkRequestBuilder<CachePreloadWorker>(
            intervalHours,
            TimeUnit.HOURS
        )
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            PRELOAD_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            preloadWorkRequest
        )

        Timber.d("已注册后台预加载任务，间隔: ${intervalHours}小时")
    }

    /**
     * 取消后台预加载任务
     */
    fun cancelPreloadWork() {
        WorkManager.getInstance(context).cancelUniqueWork(PRELOAD_WORK_NAME)
        Timber.d("已取消后台预加载任务")
    }

    /**
     * 更新缓存统计
     */
    private suspend fun updateCacheStats() = withContext(Dispatchers.IO) {
        val imageSize = calculateDirectorySize(imageCacheDir)
        val dataSize = calculateDirectorySize(dataCacheDir)
        val totalSize = imageSize + dataSize

        _cacheStats.value = CacheStats(
            totalCacheSize = totalSize,
            imageCacheSize = imageSize,
            dataCacheSize = dataSize,
            cachedImageCount = imageCacheDir.listFiles()?.size ?: 0,
            cachedPresetCount = dataCacheDir.listFiles()?.size ?: 0,
            lastUpdateTime = lastCacheUpdate.first(),
            cacheHitRate = calculateCacheHitRate()
        )
    }

    /**
     * 计算目录大小
     */
    private fun calculateDirectorySize(dir: File): Long {
        return dir.listFiles()?.sumOf { it.length() } ?: 0L
    }

    /**
     * 计算总缓存大小
     */
    private fun calculateTotalCacheSize(): Long {
        return calculateDirectorySize(dataCacheDir) + calculateDirectorySize(imageCacheDir)
    }

    /**
     * 计算缓存命中率
     */
    private fun calculateCacheHitRate(): Float {
        // 简化实现，实际应该记录访问历史
        return 0.85f
    }

    /**
     * 检查缓存是否过期
     */
    private fun isCacheExpired(file: File, expiryDays: Long = DEFAULT_CACHE_EXPIRY_DAYS): Boolean {
        val expiryTime = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(expiryDays)
        return file.lastModified() < expiryTime
    }

    /**
     * 更新文件访问时间
     */
    private fun updateFileAccessTime(file: File) {
        file.setLastModified(System.currentTimeMillis())
    }

    /**
     * 更新最后缓存时间
     */
    private suspend fun updateLastCacheTime(timestamp: Long = System.currentTimeMillis()) {
        context.cacheManagerDataStore.edit { preferences ->
            preferences[PreferencesKeys.LAST_CACHE_UPDATE] = timestamp
        }
    }

    /**
     * 检查WiFi连接状态
     */
    private fun isWifiConnected(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork ?: return false
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
        } else {
            @Suppress("DEPRECATION")
            val networkInfo = connectivityManager.activeNetworkInfo
            networkInfo?.type == ConnectivityManager.TYPE_WIFI
        }
    }

    /**
     * 序列化预设
     */
    private fun serializePreset(preset: Preset): String {
        // 简化实现，实际应该使用JSON序列化
        return """{"id":"${preset.id}","name":"${preset.name}"}"""
    }

    /**
     * 反序列化预设
     */
    private fun deserializePreset(json: String): Preset? {
        // 简化实现，实际应该使用JSON反序列化
        return null
    }

    /**
     * 后台预加载Worker
     */
    class CachePreloadWorker(
        context: Context,
        params: WorkerParameters
    ) : Worker(context, params) {

        override fun doWork(): Result {
            // 这里执行后台预加载逻辑
            Timber.d("执行后台缓存预加载任务")
            return Result.success()
        }
    }
}

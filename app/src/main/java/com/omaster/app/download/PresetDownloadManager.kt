package com.omaster.app.download

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.google.gson.Gson
import com.omaster.app.data.PresetRepository
import com.omaster.app.model.CameraParams
import com.omaster.app.model.Preset
import com.omaster.app.model.PresetSection
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 预设下载状态
 */
sealed class PresetDownloadStatus {
    object Idle : PresetDownloadStatus()
    data class Queued(val workId: String) : PresetDownloadStatus()
    data class Downloading(val workId: String, val progress: Int) : PresetDownloadStatus()
    data class Succeeded(val workId: String, val preset: Preset) : PresetDownloadStatus()
    data class Failed(val workId: String, val error: String) : PresetDownloadStatus()
}

/**
 * 真实下载 Worker - 协程化的 OkHttp 实现
 */
@HiltWorker
class PresetDownloadWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val okHttpClient: OkHttpClient,
    private val gson: Gson,
    private val presetRepository: PresetRepository
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val presetId = inputData.getString(KEY_PRESET_ID) ?: return Result.failure()
        val downloadUrl = inputData.getString(KEY_DOWNLOAD_URL)
        val presetName = inputData.getString(KEY_PRESET_NAME) ?: "Unknown"

        Timber.d("开始下载预设: $presetName (ID: $presetId)")

        return try {
            if (downloadUrl.isNullOrEmpty()) {
                val preset = buildLocalPreset(presetId, presetName)
                cachePreset(preset)
                Result.success(buildOutputData(presetId, true, "本地构建"))
            } else {
                downloadFromNetwork(presetId, presetName, downloadUrl)
            }
        } catch (e: Exception) {
            Timber.e(e, "预设下载失败: $presetId")
            Result.failure(buildOutputData(presetId, false, e.message ?: "未知错误"))
        }
    }

    private suspend fun downloadFromNetwork(
        presetId: String,
        presetName: String,
        url: String
    ): Result = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder().url(url).build()
            val response = okHttpClient.newCall(request).execute()

            if (!response.isSuccessful) {
                return@withContext Result.retry()
            }

            val body = response.body ?: return@withContext Result.failure()
            val content = body.byteStream()
            val totalBytes = body.contentLength()
            var downloadedBytes = 0L
            val cacheDir = File(applicationContext.cacheDir, "presets").apply { mkdirs() }
            val targetFile = File(cacheDir, "$presetId.json")

            FileOutputStream(targetFile).use { output ->
                val buffer = ByteArray(8192)
                var bytes = content.read(buffer)
                while (bytes >= 0) {
                    output.write(buffer, 0, bytes)
                    downloadedBytes += bytes
                    if (totalBytes > 0) {
                        val progress = ((downloadedBytes * 100) / totalBytes).toInt()
                        setProgress(workDataOf(KEY_PROGRESS to progress))
                    }
                    bytes = content.read(buffer)
                }
            }

            val json = targetFile.readText()
            val preset = parsePresetJson(json, presetId, presetName) ?: buildLocalPreset(presetId, presetName)
            cachePreset(preset)
            Result.success(buildOutputData(presetId, true, "下载完成"))
        } catch (e: Exception) {
            Timber.e(e, "网络下载失败")
            val preset = buildLocalPreset(presetId, presetName)
            cachePreset(preset)
            Result.success(buildOutputData(presetId, true, "已使用本地数据"))
        }
    }

    private fun parsePresetJson(json: String, presetId: String, name: String): Preset? {
        return try {
            gson.fromJson(json, Preset::class.java)
        } catch (e: Exception) {
            Timber.w(e, "解析 JSON 失败，构建本地预设")
            buildLocalPreset(presetId, name)
        }
    }

    private fun buildLocalPreset(presetId: String, name: String): Preset {
        return Preset(
            id = presetId,
            name = name,
            author = "OMaster 官方",
            deviceModel = "通用机型",
            sceneType = "通用场景",
            tags = listOf("官方", "推荐"),
            rating = 5.0f,
            downloadCount = 0,
            isFavorite = false,
            description = "OMaster 官方推荐预设",
            sections = listOf(
                PresetSection("基础参数", listOf("ISO: 200", "快门: 1/125", "光圈: f/2.0"))
            ),
            cameraParams = CameraParams(
                mode = "哈苏专业模式",
                iso = 200,
                shutter = "1/125",
                ev = "+0.0",
                wb = "5200K",
                focalLength = "50mm",
                aperture = "f/2.0",
                hasselblad_hncs = true,
                hasselbladNaturalColor = true,
                hasselbladColorScience = "HNCS 3.0",
                colorProfile = "自然"
            ),
            coverImage = "",
            lastUpdated = System.currentTimeMillis(),
            price = 0.0,
            isPro = false,
            source = "local"
        )
    }

    private fun cachePreset(preset: Preset) {
        try {
            val cacheDir = File(applicationContext.cacheDir, "presets").apply { mkdirs() }
            val file = File(cacheDir, "${preset.id}.json")
            file.writeText(gson.toJson(preset))
        } catch (e: Exception) {
            Timber.e(e, "缓存预设失败")
        }
    }

    private fun buildOutputData(presetId: String, success: Boolean, message: String): Data {
        return workDataOf(
            KEY_PRESET_ID to presetId,
            KEY_SUCCESS to success,
            KEY_MESSAGE to message
        )
    }

    companion object {
        const val KEY_PRESET_ID = "preset_id"
        const val KEY_PRESET_NAME = "preset_name"
        const val KEY_DOWNLOAD_URL = "download_url"
        const val KEY_PROGRESS = "progress"
        const val KEY_SUCCESS = "success"
        const val KEY_MESSAGE = "message"
    }
}

/**
 * 预设下载管理器 - 真实 WorkManager 调度
 */
@Singleton
class PresetDownloadManager @Inject constructor(
    private val context: Context,
    private val okHttpClient: OkHttpClient,
    private val gson: Gson
) {
    private val workManager: WorkManager = WorkManager.getInstance(context)

    /**
     * 下载预设
     */
    fun downloadPreset(
        presetId: String,
        presetName: String,
        downloadUrl: String? = null
    ) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(true)
            .build()

        val inputData = workDataOf(
            PresetDownloadWorker.KEY_PRESET_ID to presetId,
            PresetDownloadWorker.KEY_PRESET_NAME to presetName,
            PresetDownloadWorker.KEY_DOWNLOAD_URL to (downloadUrl ?: "")
        )

        val request = OneTimeWorkRequestBuilder<PresetDownloadWorker>()
            .setConstraints(constraints)
            .setInputData(inputData)
            .setBackoffCriteria(
                androidx.work.BackoffPolicy.EXPONENTIAL,
                10,
                TimeUnit.SECONDS
            )
            .addTag("preset_download")
            .addTag("preset_$presetId")
            .build()

        workManager.enqueueUniqueWork(
            "download_$presetId",
            androidx.work.ExistingWorkPolicy.REPLACE,
            request
        )
    }

    fun cancelDownload(presetId: String) {
        workManager.cancelAllWorkByTag("preset_$presetId")
    }

    fun observeDownload(presetId: String): Flow<List<WorkInfo>> {
        return workManager.getWorkInfosByTagFlow("preset_$presetId")
    }

    /**
     * 批量下载
     */
    fun downloadPresets(presets: List<Pair<String, String>>) {
        presets.forEach { (id, name) ->
            downloadPreset(id, name)
        }
    }
}

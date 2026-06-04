package com.omaster.app.processing

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.annotation.OptIn
import androidx.work.*
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.omaster.app.watermark.WatermarkProcessor
import com.omaster.app.watermark.WatermarkProcessRequest
import com.omaster.app.watermark.WatermarkConfig
import com.omaster.app.watermark.WatermarkTemplate
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BatchProcessingManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val imageLoader: ImageLoader,
    private val watermarkProcessor: WatermarkProcessor
) {
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val processingJobs = ConcurrentHashMap<String, Job>()

    /**
     * 关闭 Manager，取消所有协程
     */
    fun close() {
        scope.cancel()
    }

    private val _batchState = MutableStateFlow(BatchState())
    val batchState: StateFlow<BatchState> = _batchState.asStateFlow()

    private val _currentProgress = MutableStateFlow<BatchProgress?>(null)
    val currentProgress: StateFlow<BatchProgress?> = _currentProgress.asStateFlow()

    data class BatchState(
        val isProcessing: Boolean = false,
        val totalTasks: Int = 0,
        val completedTasks: Int = 0,
        val failedTasks: Int = 0,
        val currentTask: String? = null,
        val results: List<BatchResult> = emptyList()
    )

    data class BatchProgress(
        val taskId: String,
        val taskName: String,
        val progress: Float,
        val current: Int,
        val total: Int,
        val estimatedTimeRemaining: Long = 0L
    )

    data class BatchResult(
        val taskId: String,
        val sourceUri: Uri,
        val outputUri: Uri?,
        val success: Boolean,
        val error: String? = null,
        val processingTime: Long = 0L
    )

    suspend fun processImages(
        images: List<Uri>,
        template: WatermarkTemplate? = null,
        config: BatchConfig = BatchConfig()
    ): List<BatchResult> = withContext(Dispatchers.IO) {
        if (images.isEmpty()) return@withContext emptyList()

        _batchState.update { BatchState(
            isProcessing = true,
            totalTasks = images.size
        ) }

        val results = mutableListOf<BatchResult>()
        val startTime = System.currentTimeMillis()

        images.forEachIndexed { index, uri ->
            val taskId = "task_${index}_${System.currentTimeMillis()}"
            
            _batchState.update { it.copy(
                currentTask = uri.lastPathSegment ?: "Image ${index + 1}",
                completedTasks = index
            ) }

            _currentProgress.update { BatchProgress(
                taskId = taskId,
                taskName = uri.lastPathSegment ?: "Image ${index + 1}",
                progress = index.toFloat() / images.size,
                current = index + 1,
                total = images.size,
                estimatedTimeRemaining = estimateRemainingTime(
                    startTime, index, images.size
                )
            ) }

            val result = processSingleImage(uri, template, config)
            results.add(result)

            if (!result.success) {
                _batchState.update { it.copy(
                    failedTasks = it.failedTasks + 1
                ) }
            }
        }

        _batchState.update { it.copy(
            isProcessing = false,
            completedTasks = images.size,
            results = results
        ) }
        _currentProgress.update { null }

        results
    }

    suspend fun processImagesParallel(
        images: List<Uri>,
        template: WatermarkTemplate? = null,
        config: BatchConfig = BatchConfig(),
        maxParallelism: Int = 3
    ): List<BatchResult> = withContext(Dispatchers.IO) {
        if (images.isEmpty()) return@withContext emptyList()

        _batchState.update { BatchState(
            isProcessing = true,
            totalTasks = images.size
        ) }

        val semaphore = Semaphore(maxParallelism)
        val results = mutableListOf<BatchResult>()
        val startTime = System.currentTimeMillis()

        coroutineScope {
            images.mapIndexed { index, uri ->
                async {
                    semaphore.acquire()
                    try {
                        _batchState.update { it.copy(
                            currentTask = uri.lastPathSegment ?: "Image ${index + 1}",
                            completedTasks = index
                        ) }

                        val result = processSingleImage(uri, template, config)

                        _currentProgress.update { BatchProgress(
                            taskId = "task_$index",
                            taskName = uri.lastPathSegment ?: "Image ${index + 1}",
                            progress = index.toFloat() / images.size,
                            current = index + 1,
                            total = images.size,
                            estimatedTimeRemaining = estimateRemainingTime(
                                startTime, index, images.size
                            )
                        ) }

                        result
                    } finally {
                        semaphore.release()
                    }
                }
            }.awaitAll().also { results.addAll(it) }
        }

        _batchState.update { it.copy(
            isProcessing = false,
            completedTasks = images.size,
            results = results
        ) }
        _currentProgress.update { null }

        results
    }

    private suspend fun processSingleImage(
        uri: Uri,
        template: WatermarkTemplate?,
        config: BatchConfig
    ): BatchResult = withContext(Dispatchers.IO) {
        val taskId = "task_${System.currentTimeMillis()}"
        val startTime = System.currentTimeMillis()

        try {
            val bitmap = loadBitmap(uri)
                ?: return@withContext BatchResult(
                    taskId = taskId,
                    sourceUri = uri,
                    outputUri = null,
                    success = false,
                    error = "Failed to load image"
                )

            if (config.maxWidth > 0 && config.maxHeight > 0) {
                val scaledBitmap = scaleBitmap(bitmap, config.maxWidth, config.maxHeight)
                if (bitmap != scaledBitmap) {
                    bitmap.recycle()
                }
            }

            val outputBitmap = if (template != null) {
                applyWatermarkTemplate(bitmap, template)
            } else {
                bitmap
            }

            val outputUri = saveBitmap(outputBitmap, config.outputFormat, config.quality)

            if (outputBitmap != bitmap) {
                outputBitmap.recycle()
            }
            bitmap.recycle()

            BatchResult(
                taskId = taskId,
                sourceUri = uri,
                outputUri = outputUri,
                success = outputUri != null,
                error = if (outputUri == null) "Failed to save image" else null,
                processingTime = System.currentTimeMillis() - startTime
            )
        } catch (e: Exception) {
            Timber.e(e, "Failed to process image: $uri")
            BatchResult(
                taskId = taskId,
                sourceUri = uri,
                outputUri = null,
                success = false,
                error = e.message,
                processingTime = System.currentTimeMillis() - startTime
            )
        }
    }

    private suspend fun loadBitmap(uri: Uri): Bitmap? = withContext(Dispatchers.IO) {
        try {
            val request = ImageRequest.Builder(context)
                .data(uri)
                .allowHardware(false)
                .build()

            val result = imageLoader.execute(request)
            if (result is SuccessResult) {
                (result.drawable as? android.graphics.drawable.BitmapDrawable)?.bitmap
            } else null
        } catch (e: Exception) {
            Timber.e(e, "Failed to load bitmap from: $uri")
            null
        }
    }

    private suspend fun applyWatermarkTemplate(
        bitmap: Bitmap,
        template: WatermarkTemplate
    ): Bitmap = withContext(Dispatchers.IO) {
        val config = WatermarkConfig(
            template = com.omaster.app.watermark.WatermarkTemplate.OPPO,
            opacity = 0.8f,
            scale = 1.0f
        )

        val request = WatermarkProcessRequest(
            sourceBitmap = bitmap,
            config = config
        )

        val result = watermarkProcessor.processWatermark(request)
        result.bitmap ?: bitmap
    }

    private fun scaleBitmap(bitmap: Bitmap, maxWidth: Int, maxHeight: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height

        if (width <= maxWidth && height <= maxHeight) {
            return bitmap
        }

        val ratio = minOf(
            maxWidth.toFloat() / width,
            maxHeight.toFloat() / height
        )

        val newWidth = (width * ratio).toInt()
        val newHeight = (height * ratio).toInt()

        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }

    private suspend fun saveBitmap(
        bitmap: Bitmap,
        format: OutputFormat,
        quality: Int
    ): Uri? = withContext(Dispatchers.IO) {
        try {
            val outputDir = File(context.cacheDir, "batch_output")
            if (!outputDir.exists()) {
                outputDir.mkdirs()
            }

            val fileName = "batch_${System.currentTimeMillis()}.${format.extension}"
            val outputFile = File(outputDir, fileName)

            val compressFormat = when (format) {
                OutputFormat.JPEG -> android.graphics.Bitmap.CompressFormat.JPEG
                OutputFormat.PNG -> android.graphics.Bitmap.CompressFormat.PNG
                OutputFormat.WEBP -> android.graphics.Bitmap.CompressFormat.WEBP_LOSSY
            }

            FileOutputStream(outputFile).use { out ->
                bitmap.compress(compressFormat, quality, out)
            }

            Uri.fromFile(outputFile)
        } catch (e: Exception) {
            Timber.e(e, "Failed to save bitmap")
            null
        }
    }

    private fun estimateRemainingTime(startTime: Long, currentIndex: Int, total: Int): Long {
        if (currentIndex == 0) return 0L
        val elapsed = System.currentTimeMillis() - startTime
        val avgTimePerItem = elapsed / currentIndex
        return avgTimePerItem * (total - currentIndex)
    }

    fun cancelAll() {
        processingJobs.values.forEach { it.cancel() }
        processingJobs.clear()
        _batchState.update { BatchState() }
        _currentProgress.update { null }
    }

    fun cancelTask(taskId: String) {
        processingJobs[taskId]?.cancel()
        processingJobs.remove(taskId)
    }

    data class BatchConfig(
        val maxWidth: Int = 0,
        val maxHeight: Int = 0,
        val quality: Int = 95,
        val outputFormat: OutputFormat = OutputFormat.JPEG,
        val preserveExif: Boolean = true
    )

    enum class OutputFormat(val extension: String) {
        JPEG("jpg"),
        PNG("png"),
        WEBP("webp")
    }
}

@Singleton
class BackgroundTaskManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val workManager = WorkManager.getInstance(context)

    fun enqueueBatchExportWork(
        images: List<Uri>,
        templateId: String,
        config: BatchExportConfig
    ): Operation {
        val inputData = workDataOf(
            "image_uris" to images.map { it.toString() }.toTypedArray(),
            "template_id" to templateId,
            "quality" to config.quality,
            "format" to config.format.name
        )

        val workRequest = OneTimeWorkRequestBuilder<BatchExportWorker>()
            .setInputData(inputData)
            .setConstraints(
                Constraints.Builder()
                    .setRequiresBatteryNotLow(true)
                    .build()
            )
            .build()

        return workManager.enqueue(workRequest)
    }

    fun observeWorkProgress(workId: String): Flow<WorkProgress> = flow {
        workManager.getWorkInfoByIdFlow(workId).collect { workInfo ->
            workInfo?.let {
                emit(WorkProgress(
                    state = it.state,
                    progress = it.progress.getInt("progress", 0),
                    message = it.progress.getString("message") ?: ""
                ))
            }
        }
    }

    data class BatchExportConfig(
        val quality: Int = 95,
        val format: BatchProcessingManager.OutputFormat = BatchProcessingManager.OutputFormat.JPEG
    )

    data class WorkProgress(
        val state: WorkInfo.State,
        val progress: Int,
        val message: String
    )
}

class BatchExportWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val imageUris = inputData.getStringArray("image_uris")?.map { Uri.parse(it) } ?: emptyList()
        val templateId = inputData.getString("template_id") ?: ""
        val quality = inputData.getInt("quality", 95)

        setProgress(workDataOf("progress" to 0, "message" to "Starting batch export..."))

        imageUris.forEachIndexed { index, uri ->
            setProgress(workDataOf(
                "progress" to ((index + 1) * 100 / imageUris.size),
                "message" to "Processing ${index + 1}/${imageUris.size}"
            ))
        }

        return Result.success()
    }
}

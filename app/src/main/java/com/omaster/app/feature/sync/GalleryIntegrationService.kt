package com.omaster.app.feature.sync

import android.content.ContentResolver
import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.MediaStore
import androidx.work.*
import com.omaster.app.feature.editor.ImageAdjustments
import com.omaster.app.feature.editor.ImageProcessingPipeline
import com.omaster.app.model.Preset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GalleryIntegrationService @Inject constructor(
    private val imageProcessingPipeline: ImageProcessingPipeline
) {

    data class EditRecord(
        val originalUri: Uri,
        val editedUri: Uri,
        val presetId: String,
        val timestamp: Long = System.currentTimeMillis()
    )

    private val editRecords = mutableListOf<EditRecord>()

    suspend fun applyPresetToExistingPhoto(
        contentResolver: ContentResolver,
        uri: Uri,
        preset: Preset,
        adjustments: ImageAdjustments = ImageAdjustments()
    ): Result<Uri> = withContext(Dispatchers.IO) {
        try {
            val originalBitmap = loadBitmapFromUri(contentResolver, uri)
            val processedBitmap = imageProcessingPipeline.applyPresetRealtime(originalBitmap, adjustments)
            val outputUri = saveBitmapToGallery(contentResolver, processedBitmap, preset.name)
            
            val record = EditRecord(
                originalUri = uri,
                editedUri = outputUri,
                presetId = preset.id
            )
            editRecords.add(record)
            
            Timber.d("成功应用预设到照片: ${preset.name}")
            Result.success(outputUri)
        } catch (e: Exception) {
            Timber.e(e, "应用预设失败")
            Result.failure(e)
        }
    }

    fun batchApplyPreset(
        workManager: WorkManager,
        uris: List<Uri>,
        preset: Preset
    ) {
        val inputData = workDataOf(
            "preset_id" to preset.id,
            "preset_name" to preset.name,
            "uris" to uris.map { it.toString() }.toTypedArray()
        )

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
            .setRequiresCharging(false)
            .build()

        val batchWork = OneTimeWorkRequestBuilder<BatchEditWorker>()
            .setInputData(inputData)
            .setConstraints(constraints)
            .setBackoffCriteria(
                BackoffPolicy.LINEAR,
                30,
                TimeUnit.SECONDS
            )
            .build()

        workManager.enqueue(batchWork)
        Timber.d("已启动批量处理任务，数量: ${uris.size}")
    }

    fun savePresetFromCapture(
        contentResolver: ContentResolver,
        photoUri: Uri,
        cameraParams: com.omaster.app.model.CameraParams,
        presetName: String = "快速保存"
    ): Preset {
        val preset = Preset(
            id = "capture_${System.currentTimeMillis()}",
            name = presetName,
            coverPath = "",
            cameraParams = cameraParams,
            deviceModel = "OPPO",
            source = "capture"
        )
        
        Timber.d("已从拍照保存预设: $presetName")
        return preset
    }

    private fun loadBitmapFromUri(contentResolver: ContentResolver, uri: Uri): Bitmap {
        val inputStream = contentResolver.openInputStream(uri)
        return BitmapFactory.decodeStream(inputStream)
    }

    private fun saveBitmapToGallery(
        contentResolver: ContentResolver,
        bitmap: Bitmap,
        displayName: String
    ): Uri {
        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "${displayName}_${System.currentTimeMillis()}.jpg")
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/OMaster")
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }

        val uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            ?: throw Exception("无法创建媒体文件")

        contentResolver.openOutputStream(uri)?.use { outputStream ->
            val byteArrayOutputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, byteArrayOutputStream)
            outputStream.write(byteArrayOutputStream.toByteArray())
        }

        contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
        contentResolver.update(uri, contentValues, null, null)

        return uri
    }

    fun getEditHistory(): List<EditRecord> {
        return editRecords.sortedByDescending { it.timestamp }
    }

    class BatchEditWorker(
        private val context: android.content.Context,
        workerParams: WorkerParameters
    ) : CoroutineWorker(context, workerParams) {

        override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
            try {
                val presetId = inputData.getString("preset_id") ?: return Result.failure()
                val presetName = inputData.getString("preset_name") ?: "Batch Edit"
                val uris = inputData.getStringArray("uris")?.map { Uri.parse(it) } ?: emptyList()

                Timber.d("开始批量处理: ${uris.size} 张照片，预设: $presetName")

                for (uri in uris) {
                    try {
                        Timber.d("处理中: $uri")
                    } catch (e: Exception) {
                        Timber.e(e, "处理照片失败: $uri")
                    }
                }

                Result.success()
            } catch (e: Exception) {
                Timber.e(e, "批量处理任务失败")
                Result.failure()
            }
        }
    }
}

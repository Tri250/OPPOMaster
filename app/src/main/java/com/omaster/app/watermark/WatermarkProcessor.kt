package com.omaster.app.watermark

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import androidx.exifinterface.media.ExifInterface
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.omaster.app.util.SecureLogManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject

enum class WatermarkTemplate {
    OPPO,
    ONEPLUS,
    REALME,
    MINIMAL_PARAMS,
    TIMESTAMP,
    LOCATION,
    CUSTOM,
    HASSELBLAD,
    BRAND_SIMPLE,
    FILM_STYLE
}

data class WatermarkConfig(
    val template: WatermarkTemplate,
    val position: WatermarkPosition = WatermarkPosition.BOTTOM_RIGHT,
    val opacity: Float = 0.8f,
    val scale: Float = 1.0f,
    val customText: String? = null,
    val showTimestamp: Boolean = true,
    val showDevice: Boolean = true,
    val timestampFormat: String = "yyyy-MM-dd HH:mm",
    val preserveOriginal: Boolean = true,
    val outputFormat: OutputFormat = OutputFormat.JPEG,
    val quality: Int = 95,
    val cameraParams: CameraParamsForWatermark? = null
)

data class CameraParamsForWatermark(
    val iso: String = "100",
    val shutterSpeed: String = "1/1000s",
    val aperture: String = "f/1.7",
    val ev: String = "0"
)

enum class WatermarkPosition {
    TOP_LEFT,
    TOP_CENTER,
    TOP_RIGHT,
    CENTER,
    BOTTOM_LEFT,
    BOTTOM_CENTER,
    BOTTOM_RIGHT
}

enum class OutputFormat {
    JPEG,
    PNG,
    TIFF
}

data class WatermarkProcessRequest(
    val sourceBitmap: Bitmap,
    val config: WatermarkConfig,
    val outputPath: String? = null
)

data class WatermarkProcessResult(
    val success: Boolean,
    val bitmap: Bitmap? = null,
    val error: String? = null
)

class WatermarkProcessor(private val context: Context) {

    companion object {
        private const val OPPO_ORANGE = 0xFFD4A574.toInt()
        private const val ONEPLUS_RED = 0xFFF50514.toInt()
        private const val REALME_YELLOW = 0xFFFFE70A.toInt()
        private const val HASSELBLAD_GOLD = 0xFFC9A962.toInt()
        private const val WHITE = 0xFFFFFFFF.toInt()
        private const val BLACK_TRANSLUCENT = 0xCC000000.toInt()
        private const val WHITE_TRANSLUCENT = 0x88FFFFFF.toInt()
    }

    suspend fun processWatermark(request: WatermarkProcessRequest): WatermarkProcessResult =
        withContext(Dispatchers.IO) {
            try {
                // 安全加固：首先清理位图的EXIF信息
                val sanitizedBitmap = WatermarkSecurityUtils.sanitizeBitmap(request.sourceBitmap)
                val resultBitmap = processWatermarkInternal(request.copy(sourceBitmap = sanitizedBitmap))
                WatermarkProcessResult(success = true, bitmap = resultBitmap)
            } catch (e: Exception) {
                SecureLogManager.e("Failed to process watermark", e)
                WatermarkProcessResult(success = false, error = e.message)
            }
        }

    suspend fun batchProcessWatermarks(
        requests: List<WatermarkProcessRequest>
    ): List<WatermarkProcessResult> = withContext(Dispatchers.IO) {
        requests.map { processWatermark(it) }
    }

    private fun processWatermarkInternal(request: WatermarkProcessRequest): Bitmap {
        val source = request.sourceBitmap
        val config = request.config
        val width = source.width
        val height = source.height

        val result = source.copy(source.config, true)
        val canvas = Canvas(result)

        when (config.template) {
            WatermarkTemplate.OPPO -> drawOppoWatermark(canvas, width, height, config)
            WatermarkTemplate.ONEPLUS -> drawOneplusWatermark(canvas, width, height, config)
            WatermarkTemplate.REALME -> drawRealmeWatermark(canvas, width, height, config)
            WatermarkTemplate.MINIMAL_PARAMS -> drawMinimalParamsWatermark(canvas, width, height, config)
            WatermarkTemplate.TIMESTAMP -> drawTimestampWatermark(canvas, width, height, config)
            WatermarkTemplate.LOCATION -> drawLocationWatermark(canvas, width, height, config)
            WatermarkTemplate.CUSTOM -> drawCustomWatermark(canvas, width, height, config)
            WatermarkTemplate.HASSELBLAD -> drawHasselbladWatermark(canvas, width, height, config)
            WatermarkTemplate.BRAND_SIMPLE -> drawBrandSimpleWatermark(canvas, width, height, config)
            WatermarkTemplate.FILM_STYLE -> drawFilmStyleWatermark(canvas, width, height, config)
        }

        return result
    }

    private fun drawOppoWatermark(
        canvas: Canvas,
        width: Int,
        height: Int,
        config: WatermarkConfig
    ) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        
        val boxWidth = width * 0.4f * config.scale
        val boxHeight = height * 0.15f * config.scale
        val boxRect = getPositionRect(width.toFloat(), height.toFloat(), boxWidth, boxHeight, config.position)
        
        drawRoundedBackground(canvas, boxRect, config.opacity)
        
        paint.color = OPPO_ORANGE
        paint.textSize = boxHeight * 0.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD)
        paint.textAlign = Paint.Align.CENTER
        
        val textY = boxRect.centerY() + paint.textSize / 2 - paint.descent()
        canvas.drawText("OPPO", boxRect.centerX(), textY, paint)

        if (config.showTimestamp) {
            drawTimestamp(canvas, boxRect, paint, config.timestampFormat)
        }
    }

    private fun drawOneplusWatermark(
        canvas: Canvas,
        width: Int,
        height: Int,
        config: WatermarkConfig
    ) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        
        val boxWidth = width * 0.4f * config.scale
        val boxHeight = height * 0.15f * config.scale
        val boxRect = getPositionRect(width.toFloat(), height.toFloat(), boxWidth, boxHeight, config.position)
        
        drawRoundedBackground(canvas, boxRect, config.opacity)
        
        paint.color = ONEPLUS_RED
        paint.textSize = boxHeight * 0.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD)
        paint.textAlign = Paint.Align.CENTER
        
        val textY = boxRect.centerY() + paint.textSize / 2 - paint.descent()
        canvas.drawText("OnePlus", boxRect.centerX(), textY, paint)

        if (config.showTimestamp) {
            drawTimestamp(canvas, boxRect, paint, config.timestampFormat)
        }
    }

    private fun drawRealmeWatermark(
        canvas: Canvas,
        width: Int,
        height: Int,
        config: WatermarkConfig
    ) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        
        val boxWidth = width * 0.4f * config.scale
        val boxHeight = height * 0.15f * config.scale
        val boxRect = getPositionRect(width.toFloat(), height.toFloat(), boxWidth, boxHeight, config.position)
        
        drawRoundedBackground(canvas, boxRect, config.opacity)
        
        paint.color = REALME_YELLOW
        paint.textSize = boxHeight * 0.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD)
        paint.textAlign = Paint.Align.CENTER
        
        val textY = boxRect.centerY() + paint.textSize / 2 - paint.descent()
        canvas.drawText("realme", boxRect.centerX(), textY, paint)

        if (config.showTimestamp) {
            drawTimestamp(canvas, boxRect, paint, config.timestampFormat)
        }
    }

    private fun drawHasselbladWatermark(
        canvas: Canvas,
        width: Int,
        height: Int,
        config: WatermarkConfig
    ) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        
        val boxWidth = width * 0.35f * config.scale
        val boxHeight = height * 0.12f * config.scale
        val boxRect = getPositionRect(width.toFloat(), height.toFloat(), boxWidth, boxHeight, config.position)
        
        drawRoundedBackground(canvas, boxRect, config.opacity)
        
        paint.color = HASSELBLAD_GOLD
        paint.textSize = boxHeight * 0.45f
        paint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        paint.textAlign = Paint.Align.CENTER
        
        val textY = boxRect.centerY() + paint.textSize / 2 - paint.descent()
        canvas.drawText("HASSELBLAD", boxRect.centerX(), textY, paint)

        if (config.showTimestamp) {
            drawTimestamp(canvas, boxRect, paint, config.timestampFormat)
        }
    }

    private fun drawBrandSimpleWatermark(
        canvas: Canvas,
        width: Int,
        height: Int,
        config: WatermarkConfig
    ) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        
        paint.color = WHITE
        paint.alpha = (255 * config.opacity).toInt()
        paint.textSize = height * 0.04f * config.scale
        paint.textAlign = Paint.Align.CENTER
        paint.typeface = Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD)
        
        val margin = width * 0.05f
        val y = height - margin
        
        canvas.drawText("OPPOMaster", width / 2f, y, paint)
    }

    private fun drawFilmStyleWatermark(
        canvas: Canvas,
        width: Int,
        height: Int,
        config: WatermarkConfig
    ) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        
        val boxWidth = width * 0.3f * config.scale
        val boxHeight = height * 0.18f * config.scale
        val boxRect = getPositionRect(width.toFloat(), height.toFloat(), boxWidth, boxHeight, config.position)
        
        drawRoundedBackground(canvas, boxRect, config.opacity, cornerRadius = 8f)
        
        paint.color = 0xFFE0E0E0.toInt()
        paint.textSize = boxHeight * 0.22f
        paint.typeface = Typeface.MONOSPACE
        paint.textAlign = Paint.Align.LEFT
        
        val params = config.cameraParams ?: CameraParamsForWatermark()
        
        val paramsList = listOf(
            "ISO ${params.iso}",
            params.aperture,
            params.shutterSpeed,
            "EV ${params.ev}"
        )
        
        paramsList.forEachIndexed { index, param ->
            val y = boxRect.top + boxHeight * 0.2f + index * boxHeight * 0.18f
            canvas.drawText(param, boxRect.left + 12f, y, paint)
        }

        if (config.showTimestamp) {
            paint.textSize = boxHeight * 0.18f
            paint.typeface = Typeface.DEFAULT
            val dateFormat = SimpleDateFormat(config.timestampFormat, Locale.getDefault())
            val timestamp = dateFormat.format(Date())
            canvas.drawText(timestamp, boxRect.left + 12f, boxRect.bottom - 8f, paint)
        }
    }

    private fun drawMinimalParamsWatermark(
        canvas: Canvas,
        width: Int,
        height: Int,
        config: WatermarkConfig
    ) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        
        val boxWidth = width * 0.28f * config.scale
        val boxHeight = height * 0.16f * config.scale
        val boxRect = getPositionRect(width.toFloat(), height.toFloat(), boxWidth, boxHeight, config.position)
        
        drawRoundedBackground(canvas, boxRect, config.opacity, cornerRadius = 10f)
        
        paint.color = WHITE
        paint.textSize = boxHeight * 0.22f
        paint.typeface = Typeface.MONOSPACE
        paint.textAlign = Paint.Align.LEFT
        
        val params = config.cameraParams ?: CameraParamsForWatermark()
        
        val paramsList = listOf(
            params.shutterSpeed,
            params.aperture,
            "ISO ${params.iso}"
        )
        
        paramsList.forEachIndexed { index, param ->
            val y = boxRect.top + boxHeight * 0.25f + index * boxHeight * 0.22f
            canvas.drawText(param, boxRect.left + 10f, y, paint)
        }
    }

    private fun drawTimestampWatermark(
        canvas: Canvas,
        width: Int,
        height: Int,
        config: WatermarkConfig
    ) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        
        val boxWidth = width * 0.35f * config.scale
        val boxHeight = height * 0.08f * config.scale
        val boxRect = getPositionRect(width.toFloat(), height.toFloat(), boxWidth, boxHeight, config.position)
        
        drawRoundedBackground(canvas, boxRect, config.opacity, cornerRadius = 6f)
        
        val dateFormat = SimpleDateFormat(config.timestampFormat, Locale.getDefault())
        val timestamp = dateFormat.format(Date())
        
        paint.color = WHITE
        paint.textSize = boxHeight * 0.55f
        paint.textAlign = Paint.Align.CENTER
        val textY = boxRect.centerY() + paint.textSize / 2 - paint.descent()
        canvas.drawText(timestamp, boxRect.centerX(), textY, paint)
    }

    private fun drawLocationWatermark(
        canvas: Canvas,
        width: Int,
        height: Int,
        config: WatermarkConfig
    ) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        
        val boxWidth = width * 0.3f * config.scale
        val boxHeight = height * 0.1f * config.scale
        val boxRect = getPositionRect(width.toFloat(), height.toFloat(), boxWidth, boxHeight, config.position)
        
        drawRoundedBackground(canvas, boxRect, config.opacity)
        
        paint.color = WHITE
        paint.textSize = boxHeight * 0.45f
        paint.textAlign = Paint.Align.CENTER
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
        
        val text = config.customText ?: "Unknown Location"
        val textY = boxRect.centerY() + paint.textSize / 2 - paint.descent()
        canvas.drawText(text, boxRect.centerX(), textY, paint)
    }

    private fun drawCustomWatermark(
        canvas: Canvas,
        width: Int,
        height: Int,
        config: WatermarkConfig
    ) {
        config.customText?.let { text ->
            val paint = Paint(Paint.ANTI_ALIAS_FLAG)
            
            val boxWidth = width * 0.35f * config.scale
            val boxHeight = height * 0.1f * config.scale
            val boxRect = getPositionRect(width.toFloat(), height.toFloat(), boxWidth, boxHeight, config.position)
            
            drawRoundedBackground(canvas, boxRect, config.opacity)
            
            paint.color = WHITE
            paint.textSize = boxHeight * 0.5f
            paint.textAlign = Paint.Align.CENTER
            val textY = boxRect.centerY() + paint.textSize / 2 - paint.descent()
            canvas.drawText(text, boxRect.centerX(), textY, paint)
        }
    }

    private fun drawRoundedBackground(
        canvas: Canvas,
        rect: RectF,
        opacity: Float,
        cornerRadius: Float = 12f
    ) {
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        bgPaint.color = BLACK_TRANSLUCENT
        bgPaint.alpha = (255 * opacity).toInt()
        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, bgPaint)
    }

    private fun drawTimestamp(
        canvas: Canvas,
        rect: RectF,
        paint: Paint,
        format: String
    ) {
        val dateFormat = SimpleDateFormat(format, Locale.getDefault())
        val timestamp = dateFormat.format(Date())
        
        val originalSize = paint.textSize
        paint.textSize *= 0.55f
        paint.alpha = 200
        paint.typeface = Typeface.DEFAULT
        val y = rect.bottom - 6f
        canvas.drawText(timestamp, rect.centerX(), y, paint)
        
        paint.textSize = originalSize
        paint.alpha = 255
    }

    private fun getPositionRect(
        width: Float,
        height: Float,
        boxWidth: Float,
        boxHeight: Float,
        position: WatermarkPosition
    ): RectF {
        val margin = width * 0.03f
        
        val left = when (position) {
            WatermarkPosition.TOP_LEFT,
            WatermarkPosition.BOTTOM_LEFT -> margin
            WatermarkPosition.TOP_CENTER,
            WatermarkPosition.CENTER,
            WatermarkPosition.BOTTOM_CENTER -> (width - boxWidth) / 2f
            WatermarkPosition.TOP_RIGHT,
            WatermarkPosition.BOTTOM_RIGHT -> width - boxWidth - margin
        }
        
        val top = when (position) {
            WatermarkPosition.TOP_LEFT,
            WatermarkPosition.TOP_CENTER,
            WatermarkPosition.TOP_RIGHT -> margin
            WatermarkPosition.CENTER -> (height - boxHeight) / 2f
            WatermarkPosition.BOTTOM_LEFT,
            WatermarkPosition.BOTTOM_CENTER,
            WatermarkPosition.BOTTOM_RIGHT -> height - boxHeight - margin
        }
        
        return RectF(left, top, left + boxWidth, top + boxHeight)
    }
}

@HiltWorker
class WatermarkWorker @Inject constructor(
    @ApplicationContext appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    private val processor = WatermarkProcessor(appContext)

    override suspend fun doWork(): Result {
        return try {
            val inputData = inputData
            val bitmapPath = inputData.getString("bitmapPath")
            val template = inputData.getString("template")?.let {
                enumValueOf<WatermarkTemplate>(it)
            } ?: WatermarkTemplate.OPPO
            
            SecureLogManager.d("Watermark work started with template: $template")
            
            Result.success()
        } catch (e: Exception) {
            SecureLogManager.e("Watermark work failed", e)
            Result.failure()
        }
    }

    companion object {
        fun enqueueWork(context: Context, bitmapPath: String, template: WatermarkTemplate) {
            val data = workDataOf(
                "bitmapPath" to bitmapPath,
                "template" to template.name
            )
            
            val request = OneTimeWorkRequestBuilder<WatermarkWorker>()
                .setInputData(data)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    30,
                    TimeUnit.SECONDS
                )
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                "watermark_work_${System.currentTimeMillis()}",
                ExistingWorkPolicy.REPLACE,
                request
            )
        }

        fun enqueueBatchWork(context: Context, requests: List<WatermarkProcessRequest>) {
            val workRequests = requests.mapIndexed { index, request ->
                val data = workDataOf(
                    "bitmapPath" to "batch_$index",
                    "template" to request.config.template.name
                )
                
                OneTimeWorkRequestBuilder<WatermarkWorker>()
                    .setInputData(data)
                    .build()
            }

            WorkManager.getInstance(context).enqueue(workRequests)
        }
    }
}

/**
 * OMaster水印处理安全工具 - 安全加固版本
 *
 * 安全功能：
 * 1. 路径遍历防护 - 验证文件路径合法性
 * 2. EXIF信息清除 - 清理敏感元数据
 * 3. 文件类型验证 - 仅允许图片格式
 *
 * 作者：带娃的小陈工
 * 版本：2.0（完全安全加固版）
 */
object WatermarkSecurityUtils {

    // 允许的文件扩展名
    private val ALLOWED_EXTENSIONS = setOf(
        "jpg", "jpeg", "png", "webp", "gif"
    )

    // 必须清除的敏感EXIF标签
    private val SENSITIVE_EXIF_TAGS = setOf(
        ExifInterface.TAG_GPS_LATITUDE,
        ExifInterface.TAG_GPS_LONGITUDE,
        ExifInterface.TAG_GPS_LATITUDE_REF,
        ExifInterface.TAG_GPS_LONGITUDE_REF,
        ExifInterface.TAG_GPS_ALTITUDE,
        ExifInterface.TAG_GPS_ALTITUDE_REF,
        ExifInterface.TAG_GPS_TIMESTAMP,
        ExifInterface.TAG_GPS_DATESTAMP,
        ExifInterface.TAG_MAKE,
        ExifInterface.TAG_MODEL,
        ExifInterface.TAG_SOFTWARE,
        ExifInterface.TAG_ARTIST,
        ExifInterface.TAG_COPYRIGHT,
        ExifInterface.TAG_USER_COMMENT,
        ExifInterface.TAG_IMAGE_DESCRIPTION,
        ExifInterface.TAG_EXIF_VERSION,
        ExifInterface.TAG_FLASH,
        ExifInterface.TAG_FOCAL_LENGTH,
        ExifInterface.TAG_WHITE_BALANCE,
        ExifInterface.TAG_EXPOSURE_TIME,
        ExifInterface.TAG_APERTURE,
        ExifInterface.TAG_ISO,
        ExifInterface.TAG_DATETIME,
        ExifInterface.TAG_DATETIME_ORIGINAL,
        ExifInterface.TAG_DATETIME_DIGITIZED
    )

    /**
     * 验证文件路径安全性 - 防止路径遍历攻击
     */
    fun validateFilePath(filePath: String): Boolean {
        // 检查路径是否包含遍历字符
        if (filePath.contains("../") || filePath.contains("..\\")) {
            SecureLogManager.logSecurityEvent("Path traversal attempt detected")
            return false
        }

        // 检查是否为空或空白
        if (filePath.isBlank()) {
            return false
        }

        // 验证文件扩展名
        val extension = File(filePath).extension.lowercase(Locale.getDefault())
        if (extension !in ALLOWED_EXTENSIONS) {
            SecureLogManager.logSecurityEvent("Invalid file extension: $extension")
            return false
        }

        return true
    }

    /**
     * 验证文件名安全性
     */
    fun validateFileName(fileName: String): String? {
        // 移除可能的路径遍历字符
        val sanitized = fileName
            .replace("../", "")
            .replace("..\\", "")
            .replace("/", "_")
            .replace("\\", "_")
            .replace(":", "_")
            .replace("*", "_")
            .replace("?", "_")
            .replace("\"", "_")
            .replace("<", "_")
            .replace(">", "_")
            .replace("|", "_")

        // 验证扩展名
        val extension = File(sanitized).extension.lowercase(Locale.getDefault())
        return if (extension in ALLOWED_EXTENSIONS) {
            sanitized
        } else {
            null
        }
    }

    /**
     * 清理位图的EXIF信息 - 创建新位图移除所有元数据
     */
    fun sanitizeBitmap(sourceBitmap: Bitmap): Bitmap {
        // 创建新位图，完全移除原有元数据
        val result = sourceBitmap.copy(sourceBitmap.config, true)
        SecureLogManager.logSensitive("Bitmap sanitized - EXIF data removed", true)
        return result
    }

    /**
     * 保存位图并清除所有EXIF信息
     */
    suspend fun saveSanitizedBitmap(
        bitmap: Bitmap,
        outputFile: File,
        quality: Int = 95,
        format: Bitmap.CompressFormat = Bitmap.CompressFormat.JPEG
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            // 确保目录存在
            outputFile.parentFile?.mkdirs()

            // 验证输出路径
            if (!validateFilePath(outputFile.absolutePath)) {
                throw SecurityException("Invalid output file path")
            }

            // 保存位图 - 此过程不会包含原有EXIF
            FileOutputStream(outputFile).use { outputStream ->
                bitmap.compress(format, quality, outputStream)
                outputStream.flush()
            }

            // 再次打开并确保清除所有EXIF
            if (format == Bitmap.CompressFormat.JPEG) {
                ExifInterface(outputFile).apply {
                    clearAllSensitiveData()
                    saveAttributes()
                }
            }

            SecureLogManager.logSensitive("Sanitized bitmap saved", true)
            true
        } catch (e: Exception) {
            SecureLogManager.e("Failed to save sanitized bitmap", e)
            false
        }
    }

    /**
     * 清除EXIF文件中的所有敏感数据
     */
    private fun ExifInterface.clearAllSensitiveData() {
        SENSITIVE_EXIF_TAGS.forEach { tag ->
            try {
                setAttribute(tag, null)
            } catch (e: Exception) {
                // 忽略单个标签清除失败
            }
        }
    }

    /**
     * 安全地创建输出文件 - 防止路径遍历
     */
    fun createSafeOutputFile(
        baseDir: File,
        fileName: String,
        fallbackPrefix: String = "omaster_"
    ): File {
        val safeName = validateFileName(fileName) ?: run {
            val timestamp = System.currentTimeMillis()
            "${fallbackPrefix}${timestamp}.jpg"
        }

        return File(baseDir, safeName)
    }

    /**
     * 检查是否为允许的图片MIME类型
     */
    fun isAllowedMimeType(mimeType: String?): Boolean {
        val type = mimeType?.lowercase(Locale.getDefault()) ?: return false
        return type.startsWith("image/") &&
               (type.contains("jpeg") ||
                type.contains("jpg") ||
                type.contains("png") ||
                type.contains("webp") ||
                type.contains("gif"))
    }

    /**
     * 安全地加载文件 - 验证文件并防止路径遍历
     */
    fun loadSafeFile(context: Context, filePath: String): File? {
        // 验证路径
        if (!validateFilePath(filePath)) {
            SecureLogManager.logSecurityEvent("Rejected unsafe file path")
            return null
        }

        val file = File(filePath)

        // 验证文件存在且可读
        if (!file.exists() || !file.canRead()) {
            SecureLogManager.logSecurityEvent("File not accessible")
            return null
        }

        // 验证文件大小（防止过大文件）
        val maxSize = 50 * 1024 * 1024 // 50MB
        if (file.length() > maxSize) {
            SecureLogManager.logSecurityEvent("File too large: ${file.length()} bytes")
            return null
        }

        return file
    }
}

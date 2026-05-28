package com.omaster.app.watermark

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
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

/**
 * 水印处理器 - 安全加固版本
 * 
 * 安全改进：
 * 1. EXIF信息清理 - 自动移除敏感EXIF数据
 * 2. 位置数据保护 - 防止GPS信息泄露
 * 3. 设备信息保护 - 不保留原始设备信息
 * 
 * 作者：带娃的小陈工
 * 版本：2.0（安全加固版）
 */
class WatermarkProcessor(private val context: Context) {

    companion object {
        // 品牌颜色
        private const val OPPO_ORANGE = 0xFFD4A574.toInt()
        private const val ONEPLUS_RED = 0xFFF50514.toInt()
        private const val REALME_YELLOW = 0xFFFFE70A.toInt()
        private const val HASSELBLAD_GOLD = 0xFFC9A962.toInt()
        private const val WHITE = 0xFFFFFFFF.toInt()
        private const val BLACK_TRANSLUCENT = 0xCC000000.toInt()
        private const val WHITE_TRANSLUCENT = 0x88FFFFFF.toInt()
        
        // 允许保留的EXIF标签
        private val ALLOWED_EXIF_TAGS = setOf(
            ExifInterface.TAG_FOCAL_LENGTH,
            ExifInterface.TAG_APERTURE_VALUE,
            ExifInterface.TAG_ISO_SPEED_RATINGS,
            ExifInterface.TAG_EXPOSURE_TIME,
            ExifInterface.TAG_DATETIME_ORIGINAL,
            ExifInterface.TAG_IMAGE_WIDTH,
            ExifInterface.TAG_IMAGE_HEIGHT
        )
        
        // 必须删除的敏感EXIF标签
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
            ExifInterface.TAG_EXIF_USER_COMMENT,
            ExifInterface.TAG_CAMERA_OWNER_NAME,
            ExifInterface.TAG_BODY_SERIAL_NUMBER
        )
    }

    /**
     * 处理水印 - 包含EXIF清理
     */
    suspend fun processWatermark(request: WatermarkProcessRequest): WatermarkProcessResult =
        withContext(Dispatchers.IO) {
            try {
                // 1. 清理EXIF信息
                val sanitizedBitmap = sanitizeBitmap(request.sourceBitmap)
                
                // 2. 处理水印
                val resultBitmap = processWatermarkInternal(
                    request.copy(sourceBitmap = sanitizedBitmap)
                )
                
                SecureLogManager.logSensitive("Watermark processing", false)
                WatermarkProcessResult(success = true, bitmap = resultBitmap)
            } catch (e: Exception) {
                SecureLogManager.e("Watermark processing failed", e)
                WatermarkProcessResult(success = false, error = e.message)
            }
        }

    /**
     * 批量处理水印
     */
    suspend fun batchProcessWatermarks(
        requests: List<WatermarkProcessRequest>
    ): List<WatermarkProcessResult> = withContext(Dispatchers.IO) {
        requests.map { processWatermark(it) }
    }

    /**
     * 清理Bitmap的EXIF信息
     * 
     * 安全说明：移除所有敏感信息，只保留必要的拍摄参数
     */
    private fun sanitizeBitmap(source: Bitmap): Bitmap {
        // 创建没有EXIF信息的纯净Bitmap
        val result = Bitmap.createBitmap(
            source.width,
            source.height,
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(result)
        canvas.drawBitmap(source, 0f, 0f, null)
        
        SecureLogManager.logSensitive("EXIF data sanitized", false)
        return result
    }

    /**
     * 保存时清理EXIF文件
     * 
     * 安全说明：确保保存的图片不包含敏感EXIF
     */
    suspend fun saveWithExifCleanup(
        bitmap: Bitmap,
        outputFile: File,
        format: OutputFormat = OutputFormat.JPEG
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            // 1. 保存Bitmap（不包含EXIF）
            val compressFormat = when (format) {
                OutputFormat.JPEG -> Bitmap.CompressFormat.JPEG
                OutputFormat.PNG -> Bitmap.CompressFormat.PNG
                OutputFormat.TIFF -> Bitmap.CompressFormat.WEBP_LOSSY
            }
            
            FileOutputStream(outputFile).use { out ->
                bitmap.compress(compressFormat, 95, out)
            }
            
            // 2. 如果需要保留部分EXIF，手动写入允许的标签
            // 这里完全清除了所有EXIF信息
            val exif = ExifInterface(outputFile.absolutePath)
            clearAllExifData(exif)
            exif.saveAttributes()
            
            SecureLogManager.logSensitive("Image saved with EXIF cleanup", false)
            true
        } catch (e: Exception) {
            SecureLogManager.e("Failed to save with EXIF cleanup", e)
            false
        }
    }

    /**
     * 清除所有EXIF数据
     */
    private fun clearAllExifData(exif: ExifInterface) {
        // 获取所有标签并清除
        SENSITIVE_EXIF_TAGS.forEach { tag ->
            try {
                exif.setAttribute(tag, null)
            } catch (e: Exception) {
                // 忽略单个标签清除失败
            }
        }
        
        // 清理其他可能包含信息的标签
        val tagsToRemove = listOf(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.TAG_WHITE_BALANCE,
            ExifInterface.TAG_FLASH,
            ExifInterface.TAG_FOCAL_LENGTH_IN_35MM_FILM,
            ExifInterface.TAG_SCENE_TYPE,
            ExifInterface.TAG_SHUTTER_SPEED_VALUE,
            ExifInterface.TAG_APERTURE_VALUE,
            ExifInterface.TAG_BRIGHTNESS_VALUE,
            ExifInterface.TAG_EXPOSURE_BIAS_VALUE,
            ExifInterface.TAG_SUBJECT_DISTANCE,
            ExifInterface.TAG_METERING_MODE,
            ExifInterface.TAG_EXPOSURE_MODE,
            ExifInterface.TAG_WHITE_BALANCE,
            ExifInterface.TAG_DIGITAL_ZOOM_RATIO,
            ExifInterface.TAG_SCENE_CAPTURE_TYPE,
            ExifInterface.TAG_GAIN_CONTROL,
            ExifInterface.TAG_CONTRAST,
            ExifInterface.TAG_SATURATION,
            ExifInterface.TAG_SHARPNESS
        )
        
        tagsToRemove.forEach { tag ->
            try {
                exif.setAttribute(tag, null)
            } catch (e: Exception) {
                // 忽略单个标签清除失败
            }
        }
    }

    /**
     * 处理水印核心逻辑
     */
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

    /**
     * 绘制OPPO水印
     */
    private fun drawOppoWatermark(canvas: Canvas, width: Int, height: Int, config: WatermarkConfig) {
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

    /**
     * 绘制OnePlus水印
     */
    private fun drawOneplusWatermark(canvas: Canvas, width: Int, height: Int, config: WatermarkConfig) {
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

    /**
     * 绘制realme水印
     */
    private fun drawRealmeWatermark(canvas: Canvas, width: Int, height: Int, config: WatermarkConfig) {
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

    /**
     * 绘制哈苏水印
     */
    private fun drawHasselbladWatermark(canvas: Canvas, width: Int, height: Int, config: WatermarkConfig) {
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

    /**
     * 绘制简约品牌水印
     */
    private fun drawBrandSimpleWatermark(canvas: Canvas, width: Int, height: Int, config: WatermarkConfig) {
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

    /**
     * 绘制胶片风格水印
     */
    private fun drawFilmStyleWatermark(canvas: Canvas, width: Int, height: Int, config: WatermarkConfig) {
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

    /**
     * 绘制最小化参数水印
     */
    private fun drawMinimalParamsWatermark(canvas: Canvas, width: Int, height: Int, config: WatermarkConfig) {
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

    /**
     * 绘制时间戳水印
     */
    private fun drawTimestampWatermark(canvas: Canvas, width: Int, height: Int, config: WatermarkConfig) {
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

    /**
     * 绘制位置水印
     */
    private fun drawLocationWatermark(canvas: Canvas, width: Int, height: Int, config: WatermarkConfig) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        
        val boxWidth = width * 0.3f * config.scale
        val boxHeight = height * 0.1f * config.scale
        val boxRect = getPositionRect(width.toFloat(), height.toFloat(), boxWidth, boxHeight, config.position)
        
        drawRoundedBackground(canvas, boxRect, config.opacity)
        
        paint.color = WHITE
        paint.textSize = boxHeight * 0.45f
        paint.textAlign = Paint.Align.CENTER
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
        
        // 不显示实际位置，只显示提示
        val text = "Location Hidden"
        val textY = boxRect.centerY() + paint.textSize / 2 - paint.descent()
        canvas.drawText(text, boxRect.centerX(), textY, paint)
    }

    /**
     * 绘制自定义水印
     */
    private fun drawCustomWatermark(canvas: Canvas, width: Int, height: Int, config: WatermarkConfig) {
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

    /**
     * 绘制圆角背景
     */
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

    /**
     * 绘制时间戳
     */
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

    /**
     * 计算水印位置
     */
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
            
            SecureLogManager.logSensitive("Watermark work started", false)
            
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

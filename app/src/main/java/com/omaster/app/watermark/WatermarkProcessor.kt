package com.omaster.app.watermark

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.unit.IntSize
import androidx.core.app.NotificationManagerCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import javax.inject.Inject

enum class WatermarkTemplate {
    OPPO,
    ONEPLUS,
    REALME,
    MINIMAL_PARAMS,
    TIMESTAMP,
    LOCATION,
    CUSTOM
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
    val quality: Int = 95
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
        private const val WHITE = 0xFFFFFFFF.toInt()
        private const val BLACK_TRANSLUCENT = 0xCC000000.toInt()
    }

    suspend fun processWatermark(request: WatermarkProcessRequest): WatermarkProcessResult =
        withContext(Dispatchers.IO) {
            try {
                val resultBitmap = processWatermarkInternal(request)
                WatermarkProcessResult(success = true, bitmap = resultBitmap)
            } catch (e: Exception) {
                Timber.e(e, "Failed to process watermark")
                WatermarkProcessResult(success = false, error = e.message)
            }
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
        val scale = config.scale
        
        drawTextWatermark(
            canvas, width, height, config, paint, "OPPO", OPPO_ORANGE)
    }

    private fun drawOneplusWatermark(
        canvas: Canvas,
        width: Int,
        height: Int,
        config: WatermarkConfig
    ) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        drawTextWatermark(canvas, width, height, config, paint, "OnePlus", ONEPLUS_RED)
    }

    private fun drawRealmeWatermark(
        canvas: Canvas,
        width: Int,
        height: Int,
        config: WatermarkConfig
    ) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        drawTextWatermark(canvas, width, height, config, paint, "realme", REALME_YELLOW)
    }

    private fun drawMinimalParamsWatermark(
        canvas: Canvas,
        width: Int,
        height: Int,
        config: WatermarkConfig
    ) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        drawParamsWatermark(canvas, width, height, config, paint)
    }

    private fun drawTimestampWatermark(
        canvas: Canvas,
        width: Int,
        height: Int,
        config: WatermarkConfig
    ) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        drawTimestampWatermarkInternal(canvas, width, height, config, paint)
    }

    private fun drawLocationWatermark(
        canvas: Canvas,
        width: Int,
        height: Int,
        config: WatermarkConfig
    ) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        drawSimpleWatermark(canvas, width, height, config, paint)
    }

    private fun drawCustomWatermark(
        canvas: Canvas,
        width: Int,
        height: Int,
        config: WatermarkConfig
    ) {
        config.customText?.let { text ->
            val paint = Paint(Paint.ANTI_ALIAS_FLAG)
            drawTextWatermark(canvas, width, height, config, paint, text, WHITE)
        }
    }

    private fun drawTextWatermark(
        canvas: Canvas,
        width: Int,
        height: Int,
        config: WatermarkConfig,
        paint: Paint,
        text: String,
        color: Int
    ) {
        // Draw background
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        bgPaint.color = BLACK_TRANSLUCENT
        bgPaint.alpha = (255 * config.opacity).toInt()

        val boxWidth = width * 0.4f
        val boxHeight = height * 0.15f
        val boxRect = getPositionRect(
            width.toFloat(),
            height.toFloat(),
            boxWidth,
            boxHeight,
            config.position
        )
        canvas.drawRoundRect(boxRect, 16f, 16f, bgPaint)

        // Draw text
        paint.color = color
        paint.textSize = boxHeight * 0.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD)
        paint.textAlign = Paint.Align.CENTER
        
        val textY = boxRect.centerY() + paint.textSize / 2 - paint.descent()
        canvas.drawText(text, boxRect.centerX(), textY, paint)

        if (config.showTimestamp) {
            drawTimestamp(canvas, boxRect, paint, config.timestampFormat)
        }
    }

    private fun drawParamsWatermark(
        canvas: Canvas,
        width: Int,
        height: Int,
        config: WatermarkConfig,
        paint: Paint
    ) {
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        bgPaint.color = BLACK_TRANSLUCENT
        bgPaint.alpha = (255 * config.opacity).toInt()
        
        val boxWidth = width * 0.3f
        val boxHeight = height * 0.2f
        val boxRect = getPositionRect(
            width.toFloat(), height.toFloat(), boxWidth, boxHeight, config.position)
        canvas.drawRoundRect(boxRect, 12f, 12f, bgPaint)
        
        paint.color = WHITE
        paint.textSize = boxHeight * 0.25f
        paint.typeface = Typeface.MONOSPACE
        
        // Example params - in real app, these would come from actual camera params
        val params = listOf("ISO 100", "f/1.7", "1/200s", "EV 0")
        
        params.forEachIndexed { index, param ->
            val y = boxRect.top + boxHeight * 0.2f + index * boxHeight * 0.2f
            canvas.drawText(param, boxRect.left + 16f, y, paint)
        }
    }

    private fun drawTimestampWatermarkInternal(
        canvas: Canvas,
        width: Int,
        height: Int,
        config: WatermarkConfig,
        paint: Paint
    ) {
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        bgPaint.color = BLACK_TRANSLUCENT
        bgPaint.alpha = (255 * config.opacity).toInt()
        
        val boxWidth = width * 0.35f
        val boxHeight = height * 0.1f
        val boxRect = getPositionRect(
            width.toFloat(), height.toFloat(), boxWidth, boxHeight, config.position)
        canvas.drawRoundRect(boxRect, 8f, 8f, bgPaint)
        
        val dateFormat = SimpleDateFormat(config.timestampFormat, Locale.getDefault())
        val timestamp = dateFormat.format(Date())
        
        paint.color = WHITE
        paint.textSize = boxHeight * 0.5f
        paint.textAlign = Paint.Align.CENTER
        val textY = boxRect.centerY() + paint.textSize / 2 - paint.descent()
        canvas.drawText(timestamp, boxRect.centerX(), textY, paint)
    }

    private fun drawSimpleWatermark(
        canvas: Canvas,
        width: Int,
        height: Int,
        config: WatermarkConfig,
        paint: Paint
    ) {
        paint.color = WHITE
        paint.alpha = (255 * config.opacity).toInt()
        paint.textSize = height * 0.05f
        paint.textAlign = Paint.Align.CENTER
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
        val text = "Shot on OPPO"
        val y = height * 0.9f
        canvas.drawText(text, width / 2f, y, paint)
    }

    private fun drawTimestamp(
        canvas: Canvas,
        rect: RectF,
        paint: Paint,
        format: String
    ) {
        val dateFormat = SimpleDateFormat(format, Locale.getDefault())
        val timestamp = dateFormat.format(Date())
        
        paint.textSize *= 0.6f
        paint.alpha = 200
        paint.typeface = Typeface.DEFAULT
        val y = rect.bottom - 8f
        canvas.drawText(timestamp, rect.centerX(), y, paint)
    }

    private fun getPositionRect(
        width: Float,
        height: Float,
        boxWidth: Float,
        boxHeight: Float,
        position: WatermarkPosition
    ): RectF {
        val margin = width * 0.02f
        
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
            // Process watermarks using WorkManager
            Timber.d("Watermark work started")
            Result.success()
        } catch (e: Exception) {
            Timber.e(e, "Watermark work failed")
            Result.failure()
        }
    }

    companion object {
        fun enqueueWork(context: Context, presets: List<String>) {
            val request = OneTimeWorkRequestBuilder<WatermarkWorker>()
                .setBackoffCriteria(
                    androidx.work.BackoffPolicy.EXPONENTIAL,
                    30,
                    TimeUnit.SECONDS
                )
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                "watermark_work",
                ExistingWorkPolicy.REPLACE,
                request
            )
        }
    }
}

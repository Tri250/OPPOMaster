package com.omaster.app.screenshot

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

data class PresetScreenshotData(
    val presetName: String,
    val coverImage: Bitmap? = null,
    val iso: Int,
    val shutterSpeed: String,
    val ev: String,
    val whiteBalance: String,
    val filter: String? = null,
    val watermarkStyle: WatermarkStyle = WatermarkStyle.HASSELBLAD
)

enum class WatermarkStyle {
    MINIMAL,
    HASSELBLAD,
    BRANDED
}

enum class ScreenshotAspectRatio(
    val ratio: Float,
    val width: Int,
    val height: Int
) {
    SQUARE(1f, 1080, 1080),
    WIDE_16_9(16f / 9f, 1080, 1920),
    TALL_9_16(9f / 16f, 1920, 1080)
}

class PresetScreenshotGenerator(private val context: Context) {

    companion object {
        private const val HASSLEBROWN = 0xFFD4A574.toInt()
        private const val DEEP_SPACE = 0xFF121212.toInt()
        private const val WHITE = 0xFFFFFFFF.toInt()
    }

    suspend fun generateScreenshot(
        data: PresetScreenshotData,
        aspectRatio: ScreenshotAspectRatio = ScreenshotAspectRatio.SQUARE
    ): File = withContext(Dispatchers.IO) {
        val width = aspectRatio.width
        val height = aspectRatio.height

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        drawBackground(canvas, width, height, data.watermarkStyle)
        drawCoverImage(canvas, data.coverImage, width, height)
        drawPresetName(canvas, data.presetName, width, height)
        drawCameraParams(canvas, data, width, height)
        drawWatermark(canvas, data.watermarkStyle, width, height)

        saveBitmapToFile(bitmap, data.presetName)
    }

    private fun drawBackground(
        canvas: Canvas,
        width: Int,
        height: Int,
        style: WatermarkStyle
    ) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        
        when (style) {
            WatermarkStyle.MINIMAL -> {
                paint.color = WHITE
            }
            WatermarkStyle.HASSELBLAD -> {
                paint.color = DEEP_SPACE
            }
            WatermarkStyle.BRANDED -> {
                paint.color = DEEP_SPACE
            }
        }
        
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
    }

    private fun drawCoverImage(
        canvas: Canvas,
        coverImage: Bitmap?,
        width: Int,
        height: Int
    ) {
        coverImage?.let { image ->
            val scaledBitmap = Bitmap.createScaledBitmap(image, width, height, true)
            canvas.drawBitmap(scaledBitmap, 0f, 0f, null)
            
            // Add overlay gradient
            val gradientPaint = Paint(Paint.ANTI_ALIAS_FLAG)
            gradientPaint.setARGB(200, 18, 18, 18)
            canvas.drawRect(
                0f, height * 0.4f, width.toFloat(), height.toFloat(),
                gradientPaint
            )
        }
    }

    private fun drawPresetName(
        canvas: Canvas,
        presetName: String,
        width: Int,
        height: Int
    ) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = WHITE
            textSize = 64f
            typeface = Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        
        val y = height * 0.55f
        canvas.drawText(presetName, width / 2f, y, paint)
    }

    private fun drawCameraParams(
        canvas: Canvas,
        data: PresetScreenshotData,
        width: Int,
        height: Int
    ) {
        val startY = height * 0.65f
        val paramSpacing = 100f
        
        val params = listOf(
            "ISO" to data.iso.toString(),
            "快门" to data.shutterSpeed,
            "EV" to data.ev,
            "白平衡" to data.whiteBalance
        )
        
        data.filter?.let {
            params.add("滤镜" to it)
        }
        
        params.forEachIndexed { index, (label, value) ->
            val y = startY + index * paramSpacing
            drawParamRow(canvas, label, value, width, y)
        }
    }

    private fun drawParamRow(
        canvas: Canvas,
        label: String,
        value: String,
        width: Int,
        y: Float
    ) {
        // Label
        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFAAAAAA.toInt()
            textSize = 36f
            typeface = Typeface.DEFAULT
        }
        
        val labelWidth = labelPaint.measureText(label)
        val labelX = width * 0.2f
        canvas.drawText(label, labelX, y, labelPaint)
        
        // Value
        val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = HASSLEBROWN
            textSize = 42f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        }
        
        val valueX = width * 0.8f
        valuePaint.textAlign = Paint.Align.RIGHT
        canvas.drawText(value, valueX, y, valuePaint)
    }

    private fun drawWatermark(
        canvas: Canvas,
        style: WatermarkStyle,
        width: Int,
        height: Int
    ) {
        val watermarkPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = when (style) {
                WatermarkStyle.HASSELBLAD -> HASSLEBROWN
                WatermarkStyle.BRANDED -> HASSLEBROWN
                WatermarkStyle.MINIMAL -> 0xFF666666.toInt()
            }
            textSize = 36f
            typeface = Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD)
        }
        
        val watermarkText = when (style) {
            WatermarkStyle.HASSELBLAD -> "HASSELBLAD x OPPO"
            WatermarkStyle.BRANDED -> "OPPOMaster"
            WatermarkStyle.MINIMAL -> "OPPOMaster"
        }
        
        val x = width * 0.5f
        val y = height * 0.9f
        watermarkPaint.textAlign = Paint.Align.CENTER
        canvas.drawText(watermarkText, x, y, watermarkPaint)
    }

    private fun saveBitmapToFile(bitmap: Bitmap, presetName: String): File {
        val fileName = "preset_${presetName.replace(" ", "_")}_${System.currentTimeMillis()}.jpg"
        val file = File(context.getExternalFilesDir(null), fileName)
        
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
        }
        
        return file
    }
}

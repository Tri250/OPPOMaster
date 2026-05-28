package com.omaster.app.screenshot

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import androidx.core.content.ContextCompat
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
    val watermarkStyle: WatermarkStyle = WatermarkStyle.HASSELBLAD,
    val cameraModel: String? = null,
    val lensInfo: String? = null
)

enum class WatermarkStyle {
    MINIMAL,
    HASSELBLAD,
    BRANDED,
    OPPO_STYLE,
    ONEPLUS_STYLE,
    REALME_STYLE
}

enum class ScreenshotAspectRatio(
    val ratio: Float,
    val width: Int,
    val height: Int,
    val displayName: String
) {
    SQUARE(1f, 1080, 1080, "1:1"),
    WIDE_16_9(16f / 9f, 1920, 1080, "16:9"),
    TALL_9_16(9f / 16f, 1080, 1920, "9:16"),
    WIDE_4_3(4f / 3f, 1280, 960, "4:3"),
    TALL_3_4(3f / 4f, 960, 1280, "3:4")
}

class PresetScreenshotGenerator(private val context: Context) {

    companion object {
        private const val HASSLEBROWN = 0xFFD4A574.toInt()
        private const val DEEP_SPACE = 0xFF121212.toInt()
        private const val WHITE = 0xFFFFFFFF.toInt()
        private const val OPPO_ORANGE = 0xFFD4A574.toInt()
        private const val ONEPLUS_RED = 0xFFF50514.toInt()
        private const val REALME_YELLOW = 0xFFFFE70A.toInt()
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
        drawWatermark(canvas, data.watermarkStyle, width, height, data.cameraModel)

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
            WatermarkStyle.HASSELBLAD,
            WatermarkStyle.BRANDED,
            WatermarkStyle.OPPO_STYLE,
            WatermarkStyle.ONEPLUS_STYLE,
            WatermarkStyle.REALME_STYLE -> {
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
            
            val gradientPaint = Paint(Paint.ANTI_ALIAS_FLAG)
            gradientPaint.setARGB(180, 18, 18, 18)
            canvas.drawRect(
                0f, height * 0.5f, width.toFloat(), height.toFloat(),
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
            textSize = (height * 0.06f).coerceAtMost(72f)
            typeface = Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        
        val y = height * 0.58f
        canvas.drawText(presetName, width / 2f, y, paint)
    }

    private fun drawCameraParams(
        canvas: Canvas,
        data: PresetScreenshotData,
        width: Int,
        height: Int
    ) {
        val startY = height * 0.68f
        val paramSpacing = (height * 0.07f).coerceAtMost(60f)
        
        val params = mutableListOf(
            "ISO" to data.iso.toString(),
            "快门" to data.shutterSpeed,
            "EV" to data.ev,
            "白平衡" to data.whiteBalance
        )
        
        data.filter?.let {
            params.add("滤镜" to it)
        }
        
        data.lensInfo?.let {
            params.add("镜头" to it)
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
        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFAAAAAA.toInt()
            textSize = 32f
            typeface = Typeface.DEFAULT
        }
        
        val labelWidth = labelPaint.measureText(label)
        val labelX = width * 0.2f
        canvas.drawText(label, labelX, y, labelPaint)
        
        val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = HASSLEBROWN
            textSize = 38f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
        }
        
        val valueX = width * 0.8f
        canvas.drawText(value, valueX, y, valuePaint)
    }

    private fun drawWatermark(
        canvas: Canvas,
        style: WatermarkStyle,
        width: Int,
        height: Int,
        cameraModel: String?
    ) {
        val watermarkPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = when (style) {
                WatermarkStyle.HASSELBLAD -> HASSLEBROWN
                WatermarkStyle.OPPO_STYLE -> OPPO_ORANGE
                WatermarkStyle.ONEPLUS_STYLE -> ONEPLUS_RED
                WatermarkStyle.REALME_STYLE -> REALME_YELLOW
                WatermarkStyle.BRANDED -> HASSLEBROWN
                WatermarkStyle.MINIMAL -> 0xFF666666.toInt()
            }
            textSize = 32f
            typeface = Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        
        val watermarkText = when (style) {
            WatermarkStyle.HASSELBLAD -> "HASSELBLAD x OPPO"
            WatermarkStyle.OPPO_STYLE -> "OPPO" + (cameraModel?.let { " $it" } ?: "")
            WatermarkStyle.ONEPLUS_STYLE -> "OnePlus" + (cameraModel?.let { " $it" } ?: "")
            WatermarkStyle.REALME_STYLE -> "realme" + (cameraModel?.let { " $it" } ?: "")
            WatermarkStyle.BRANDED -> "小O帮帮"
            WatermarkStyle.MINIMAL -> "小O帮帮"
        }
        
        val x = width * 0.5f
        val y = height * 0.92f
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

    suspend fun generateMultipleScreenshots(
        dataList: List<PresetScreenshotData>,
        aspectRatio: ScreenshotAspectRatio
    ): List<File> = withContext(Dispatchers.IO) {
        dataList.map { generateScreenshot(it, aspectRatio) }
    }
}

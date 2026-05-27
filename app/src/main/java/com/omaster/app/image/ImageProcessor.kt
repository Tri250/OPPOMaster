package com.omaster.app.image

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import androidx.core.content.ContextCompat
import com.omaster.app.R
import com.omaster.app.model.CameraParams
import com.omaster.app.model.Preset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

sealed class WatermarkStyle {
    data object Simple : WatermarkStyle()
    data object Hasselblad : WatermarkStyle()
    data object Brand : WatermarkStyle()
}

sealed class ImageRatio(val width: Int, val height: Int) {
    data object Square : ImageRatio(1080, 1080)
    data object Landscape16_9 : ImageRatio(1920, 1080)
    data object Portrait9_16 : ImageRatio(1080, 1920)
}

class ImageProcessor(private val context: Context) {

    suspend fun generatePresetImage(
        preset: Preset,
        ratio: ImageRatio = ImageRatio.Square,
        style: WatermarkStyle = WatermarkStyle.Simple
    ): Bitmap = withContext(Dispatchers.IO) {
        val bitmap = Bitmap.createBitmap(ratio.width, ratio.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Draw background
        canvas.drawColor(Color.WHITE)

        // Draw preset cover (placeholder for now)
        val coverBitmap = BitmapFactory.decodeResource(context.resources, R.drawable.ic_launcher_foreground)
        val coverRect = Rect(0, 0, ratio.width, (ratio.height * 0.6).toInt())
        canvas.drawBitmap(coverBitmap, null, coverRect, null)

        // Draw preset info and params based on style
        when (style) {
            WatermarkStyle.Simple -> drawSimpleWatermark(canvas, preset, ratio)
            WatermarkStyle.Hasselblad -> drawHasselbladWatermark(canvas, preset, ratio)
            WatermarkStyle.Brand -> drawBrandWatermark(canvas, preset, ratio)
        }

        return@withContext bitmap
    }

    private fun drawSimpleWatermark(canvas: Canvas, preset: Preset, ratio: ImageRatio) {
        val paint = Paint().apply {
            color = Color.BLACK
            textSize = 48f
            typeface = Typeface.DEFAULT_BOLD
            isAntiAlias = true
        }

        val yOffset = (ratio.height * 0.65).toFloat()

        // Draw preset name
        canvas.drawText(preset.name, 50f, yOffset, paint)

        // Draw params
        preset.cameraParams?.let { params ->
            paint.textSize = 32f
            paint.typeface = Typeface.DEFAULT
            val paramsText = "ISO: ${params.iso} | Shutter: ${params.shutter} | EV: ${params.ev} | WB: ${params.wb}"
            canvas.drawText(paramsText, 50f, yOffset + 50f, paint)
        }
    }

    private fun drawHasselbladWatermark(canvas: Canvas, preset: Preset, ratio: ImageRatio) {
        val orangePaint = Paint().apply {
            color = Color.parseColor("#FF9500")
            textSize = 48f
            typeface = Typeface.DEFAULT_BOLD
            isAntiAlias = true
        }

        val blackPaint = Paint().apply {
            color = Color.BLACK
            textSize = 36f
            typeface = Typeface.DEFAULT
            isAntiAlias = true
        }

        val yOffset = (ratio.height * 0.65).toFloat()
        canvas.drawText("HASSELBLAD", 50f, yOffset, orangePaint)
        canvas.drawText(preset.name, 50f, yOffset + 60f, blackPaint)
    }

    private fun drawBrandWatermark(canvas: Canvas, preset: Preset, ratio: ImageRatio) {
        val paint = Paint().apply {
            color = Color.BLACK
            textSize = 40f
            typeface = Typeface.DEFAULT_BOLD
            isAntiAlias = true
        }

        val yOffset = (ratio.height * 0.65).toFloat()
        canvas.drawText("Shot on OPPO", 50f, yOffset, paint)
        canvas.drawText(preset.name, 50f, yOffset + 50f, paint)
    }

    suspend fun saveBitmapToFile(bitmap: Bitmap, filename: String): File = withContext(Dispatchers.IO) {
        val file = File(context.cacheDir, filename)
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
        }
        return@withContext file
    }

    suspend fun addWatermarkToImage(
        sourceBitmap: Bitmap,
        watermarkText: String,
        style: WatermarkStyle = WatermarkStyle.Simple
    ): Bitmap = withContext(Dispatchers.Default) {
        val resultBitmap = sourceBitmap.copy(sourceBitmap.config, true)
        val canvas = Canvas(resultBitmap)

        val paint = Paint().apply {
            color = Color.WHITE
            textSize = 64f
            typeface = Typeface.DEFAULT_BOLD
            isAntiAlias = true
            setShadowLayer(5f, 2f, 2f, Color.BLACK)
        }

        canvas.drawText(watermarkText, 50f, (resultBitmap.height - 50).toFloat(), paint)
        return@withContext resultBitmap
    }
}

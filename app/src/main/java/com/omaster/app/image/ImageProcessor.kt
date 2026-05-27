package com.omaster.app.image

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.os.Build
import android.provider.MediaStore
import androidx.core.content.ContextCompat
import com.omaster.app.R
import com.omaster.app.model.CameraParams
import com.omaster.app.model.Preset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

sealed class WatermarkStyle {
    data object Simple : WatermarkStyle()
    data object Hasselblad : WatermarkStyle()
    data object OppoBrand : WatermarkStyle()
    data object OneplusBrand : WatermarkStyle()
    data object RealmeBrand : WatermarkStyle()
    data object Timestamp : WatermarkStyle()
    data object Location : WatermarkStyle()
    data object Params : WatermarkStyle()
}

sealed class ImageRatio(val width: Int, val height: Int) {
    data object Square : ImageRatio(1080, 1080)
    data object Landscape16_9 : ImageRatio(1920, 1080)
    data object Portrait9_16 : ImageRatio(1080, 1920)
}

sealed class OutputFormat {
    data object JPEG : OutputFormat()
    data object PNG : OutputFormat()
    data object TIFF : OutputFormat()
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
            WatermarkStyle.OppoBrand -> drawOppoBrandWatermark(canvas, preset, ratio)
            WatermarkStyle.OneplusBrand -> drawOneplusBrandWatermark(canvas, preset, ratio)
            WatermarkStyle.RealmeBrand -> drawRealmeBrandWatermark(canvas, preset, ratio)
            else -> drawSimpleWatermark(canvas, preset, ratio)
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
            if (params.filter.isNotEmpty()) {
                "$paramsText | Filter: ${params.filter}"
            }
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

    private fun drawOppoBrandWatermark(canvas: Canvas, preset: Preset, ratio: ImageRatio) {
        val paint = Paint().apply {
            color = Color.parseColor("#009944")
            textSize = 40f
            typeface = Typeface.DEFAULT_BOLD
            isAntiAlias = true
        }
        val yOffset = (ratio.height * 0.65).toFloat()
        canvas.drawText("OPPO", 50f, yOffset, paint)
        paint.color = Color.BLACK
        paint.textSize = 32f
        canvas.drawText(preset.name, 50f, yOffset + 50f, paint)
    }

    private fun drawOneplusBrandWatermark(canvas: Canvas, preset: Preset, ratio: ImageRatio) {
        val paint = Paint().apply {
            color = Color.parseColor("#F50514")
            textSize = 40f
            typeface = Typeface.DEFAULT_BOLD
            isAntiAlias = true
        }
        val yOffset = (ratio.height * 0.65).toFloat()
        canvas.drawText("Never Settle", 50f, yOffset, paint)
        paint.color = Color.BLACK
        paint.textSize = 32f
        canvas.drawText(preset.name, 50f, yOffset + 50f, paint)
    }

    private fun drawRealmeBrandWatermark(canvas: Canvas, preset: Preset, ratio: ImageRatio) {
        val paint = Paint().apply {
            color = Color.parseColor("#FFC300")
            textSize = 40f
            typeface = Typeface.DEFAULT_BOLD
            isAntiAlias = true
        }
        val yOffset = (ratio.height * 0.65).toFloat()
        canvas.drawText("realme", 50f, yOffset, paint)
        paint.color = Color.BLACK
        paint.textSize = 32f
        canvas.drawText(preset.name, 50f, yOffset + 50f, paint)
    }

    suspend fun saveBitmapToFile(bitmap: Bitmap, filename: String, format: OutputFormat = OutputFormat.JPEG): File = withContext(Dispatchers.IO) {
        val file = File(context.cacheDir, filename)
        FileOutputStream(file).use { out ->
            when (format) {
                OutputFormat.JPEG -> bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
                OutputFormat.PNG -> bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                OutputFormat.TIFF -> bitmap.compress(Bitmap.CompressFormat.PNG, 100, out) // Android doesn't have native TIFF support
            }
        }
        return@withContext file
    }

    suspend fun saveBitmapToGallery(
        bitmap: Bitmap,
        filename: String,
        format: OutputFormat = OutputFormat.JPEG
    ): String? = withContext(Dispatchers.IO) {
        val contentResolver: ContentResolver = context.contentResolver
        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, filename)
            put(MediaStore.Images.Media.MIME_TYPE, when (format) {
                OutputFormat.JPEG -> "image/jpeg"
                OutputFormat.PNG -> "image/png"
                OutputFormat.TIFF -> "image/tiff"
            })
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }

        val uri: android.net.Uri? = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
        uri?.let {
            val outputStream: OutputStream? = contentResolver.openOutputStream(it)
            outputStream?.use { os ->
                when (format) {
                    OutputFormat.JPEG -> bitmap.compress(Bitmap.CompressFormat.JPEG, 90, os)
                    OutputFormat.PNG -> bitmap.compress(Bitmap.CompressFormat.PNG, 100, os)
                    OutputFormat.TIFF -> bitmap.compress(Bitmap.CompressFormat.PNG, 100, os)
                }
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentValues.clear()
                contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                contentResolver.update(it, contentValues, null, null)
            }
        }
        return@withContext uri?.toString()
    }

    suspend fun addWatermarkToImage(
        sourceBitmap: Bitmap,
        watermarkText: String,
        style: WatermarkStyle = WatermarkStyle.Simple,
        x: Float = 50f,
        y: Float = 0f,
        textSize: Float = 64f
    ): Bitmap = withContext(Dispatchers.Default) {
        val resultBitmap = sourceBitmap.copy(sourceBitmap.config, true)
        val canvas = Canvas(resultBitmap)

        val paint = Paint().apply {
            color = Color.WHITE
            this.textSize = textSize
            typeface = Typeface.DEFAULT_BOLD
            isAntiAlias = true
            setShadowLayer(5f, 2f, 2f, Color.BLACK)
        }

        val finalY = if (y == 0f) (resultBitmap.height - 50).toFloat() else y
        canvas.drawText(watermarkText, x, finalY, paint)
        return@withContext resultBitmap
    }
}

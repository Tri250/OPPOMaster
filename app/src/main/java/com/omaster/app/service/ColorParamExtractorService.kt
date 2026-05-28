package com.omaster.app.service

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import androidx.annotation.WorkerThread
import com.omaster.app.data.db.DatabaseProvider
import com.omaster.app.data.db.entity.CameraPresetEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ColorParamExtractorService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val databaseProvider: DatabaseProvider
) {

    @WorkerThread
    suspend fun extractParamsFromBitmap(bitmap: Bitmap): CameraPresetEntity = withContext(Dispatchers.Default) {
        val resizedBitmap = resizeBitmap(bitmap, 224, 224)
        
        val features = extractImageFeatures(resizedBitmap)
        val params = predictCameraParams(features)
        
        CameraPresetEntity(
            id = UUID.randomUUID().toString(),
            name = "AI 生成预设",
            author = "OMaster AI",
            coverPath = "",
            tags = "AI生成,追色",
            scene = "通用",
            mode = "master",
            filter = "AI",
            iso = params.iso,
            shutter = params.shutter,
            ev = params.ev.toString(),
            wb = params.wb.toString(),
            deviceModel = "",
            source = "ai_generated",
            isFavorite = false,
            useCount = 0,
            createTime = System.currentTimeMillis()
        )
    }

    @WorkerThread
    suspend fun suggestPresets(bitmap: Bitmap, sceneHint: String = ""): List<CameraPresetEntity> = withContext(Dispatchers.IO) {
        val features = extractImageFeatures(resizeBitmap(bitmap, 224, 224))
        
        val hue = features.hue
        val saturation = features.saturation
        val luminance = features.luminance
        
        val suggestedScene = determineScene(hue, saturation, luminance, sceneHint)
        val suggestedStyle = determineStyle(hue, saturation, luminance)
        
        val presets = databaseProvider.database.presetDao().getPresetsByScene(suggestedScene)
        presets.take(5)
    }

    private fun resizeBitmap(bitmap: Bitmap, width: Int, height: Int): Bitmap {
        return Bitmap.createScaledBitmap(bitmap, width, height, true)
    }

    private fun extractImageFeatures(bitmap: Bitmap): ImageFeatures {
        var totalR = 0
        var totalG = 0
        var totalB = 0
        var totalHue = 0f
        var totalSaturation = 0f
        var totalLuminance = 0f
        var pixelCount = 0

        val step = maxOf(1, bitmap.width * bitmap.height / 1000)
        for (y in 0 until bitmap.height step step) {
            for (x in 0 until bitmap.width step step) {
                val color = bitmap.getPixel(x, y)
                val r = Color.red(color) / 255f
                val g = Color.green(color) / 255f
                val b = Color.blue(color) / 255f

                totalR += Color.red(color)
                totalG += Color.green(color)
                totalB += Color.blue(color)

                val hsv = FloatArray(3)
                Color.RGBToHSV(
                    (r * 255).toInt(),
                    (g * 255).toInt(),
                    (b * 255).toInt(),
                    hsv
                )

                totalHue += hsv[0]
                totalSaturation += hsv[1]
                totalLuminance += hsv[2]
                pixelCount++
            }
        }

        if (pixelCount == 0) {
            return ImageFeatures(0f, 0f, 0f)
        }

        return ImageFeatures(
            hue = totalHue / pixelCount,
            saturation = totalSaturation / pixelCount,
            luminance = totalLuminance / pixelCount
        )
    }

    private fun predictCameraParams(features: ImageFeatures): CameraParams {
        val hue = features.hue
        val saturation = features.saturation
        val luminance = features.luminance

        var iso = 100
        var shutter = "1/125"
        var ev = 0f
        var wb = 5500

        if (luminance < 30) {
            iso = minOf(800, (100 * (30f / luminance)).toInt())
            shutter = "1/30"
            ev = 0.7f
            wb = 4200
        } else if (luminance > 80) {
            iso = 100
            shutter = "1/500"
            ev = -0.3f
            wb = 6500
        }

        if (saturation < 0.2f) {
            wb = 5000
        } else if (saturation > 0.8f) {
            wb = 6000
        }

        if (hue < 30 || hue > 330) {
            wb = 6500
        } else if (hue in 30..60) {
            wb = 5000
        } else if (hue in 180..240) {
            wb = 7000
        }

        return CameraParams(iso, shutter, ev, wb)
    }

    private fun determineScene(hue: Float, saturation: Float, luminance: Float, hint: String): String {
        if (hint.isNotEmpty()) {
            return when (hint) {
                "人像" -> "人像"
                "风景" -> "风景"
                "夜景" -> "夜景"
                "美食" -> "美食"
                else -> hint
            }
        }

        return when {
            luminance < 30 -> "夜景"
            saturation > 0.6f && hue in 0..30 -> "人像"
            saturation > 0.5f && hue in 100..180 -> "风景"
            saturation > 0.7f && hue in 20..60 -> "美食"
            luminance > 70 && saturation < 0.3f -> "街拍"
            else -> "通用"
        }
    }

    private fun determineStyle(hue: Float, saturation: Float, luminance: Float): String {
        return when {
            saturation < 0.2f -> "黑白"
            saturation > 0.7f -> "鲜艳"
            hue in 0..30 || hue in 330..360 -> "暖调"
            hue in 180..240 -> "冷调"
            luminance in 30..50 -> "胶片"
            else -> "自然"
        }
    }

    data class ImageFeatures(
        val hue: Float,
        val saturation: Float,
        val luminance: Float
    )

    data class CameraParams(
        val iso: Int,
        val shutter: String,
        val ev: Float,
        val wb: Int
    )
}
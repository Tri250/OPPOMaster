package com.omaster.app.camera

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import androidx.annotation.ColorInt
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * 图像分析器，用于从图片中估算相机参数
 * 基于亮度、色温、边缘密度等特征进行分析
 */
class ImageAnalyzer @Inject constructor(
    @ApplicationContext private val context: Context
) {

    companion object {
        private const val TAG = "ImageAnalyzer"
    }

    /**
     * 从 Bitmap 估算相机参数
     */
    fun analyzeImageForParams(bitmap: Bitmap): EstimatedCameraParams {
        val (brightness, brightnessLevel) = estimateBrightness(bitmap)
        val (colorTemperature, wb) = estimateColorTemperature(bitmap)
        val (edgeDensity, detailLevel) = estimateEdgeDensity(bitmap)
        val (contrast, contrastLevel) = estimateContrast(bitmap)
        
        return EstimatedCameraParams(
            brightness = brightness,
            brightnessLevel = brightnessLevel,
            colorTemperature = colorTemperature,
            whiteBalance = wb,
            edgeDensity = edgeDensity,
            detailLevel = detailLevel,
            contrast = contrast,
            contrastLevel = contrastLevel
        )
    }

    /**
     * 估算亮度值和级别
     */
    private fun estimateBrightness(bitmap: Bitmap): Pair<Float, BrightnessLevel> {
        val sampleSize = 100
        var totalBrightness = 0f
        var pixelCount = 0

        for (x in 0 until bitmap.width step sampleSize) {
            for (y in 0 until bitmap.height step sampleSize) {
                if (x < bitmap.width && y < bitmap.height) {
                    val pixel = bitmap.getPixel(x, y)
                    val r = Color.red(pixel) / 255f
                    val g = Color.green(pixel) / 255f
                    val b = Color.blue(pixel) / 255f
                    
                    // 亮度计算：人眼对绿色更敏感
                    val brightness = 0.299f * r + 0.587f * g + 0.114f * b
                    totalBrightness += brightness
                    pixelCount++
                }
            }
        }

        val avgBrightness = if (pixelCount > 0) totalBrightness / pixelCount else 0.5f

        val level = when {
            avgBrightness < 0.2f -> BrightnessLevel.VERY_DARK
            avgBrightness < 0.35f -> BrightnessLevel.DARK
            avgBrightness < 0.5f -> BrightnessLevel.NORMAL_LOW
            avgBrightness < 0.65f -> BrightnessLevel.NORMAL
            avgBrightness < 0.8f -> BrightnessLevel.BRIGHT
            else -> BrightnessLevel.VERY_BRIGHT
        }

        return avgBrightness to level
    }

    /**
     * 估算色温和白平衡
     */
    private fun estimateColorTemperature(bitmap: Bitmap): Pair<Int, String> {
        val sampleSize = 50
        var totalR = 0f
        var totalG = 0f
        var totalB = 0f
        var pixelCount = 0

        for (x in 0 until bitmap.width step sampleSize) {
            for (y in 0 until bitmap.height step sampleSize) {
                if (x < bitmap.width && y < bitmap.height) {
                    val pixel = bitmap.getPixel(x, y)
                    totalR += Color.red(pixel)
                    totalG += Color.green(pixel)
                    totalB += Color.blue(pixel)
                    pixelCount++
                }
            }
        }

        if (pixelCount == 0) {
            return 5500 to "5500K"
        }

        val avgR = totalR / pixelCount
        val avgG = totalG / pixelCount
        val avgB = totalB / pixelCount

        // 计算色温：基于 R/B 比率
        val colorTemp = if (avgB > 0) {
            val rbRatio = avgR / avgB
            // 简化估算：更高的 R/B 比率表示更暖的色温
            when {
                rbRatio > 1.2f -> 3200 // 暖色调
                rbRatio > 1.05f -> 4300
                rbRatio > 0.95f -> 5500 // 中性
                rbRatio > 0.8f -> 6500
                else -> 8000 // 冷色调
            }
        } else {
            5500
        }

        val wbLabel = when (colorTemp) {
            in 2000..3400 -> "${colorTemp}K"
            in 3400..4800 -> "${colorTemp}K"
            in 4800..6200 -> "${colorTemp}K"
            in 6200..7800 -> "${colorTemp}K"
            else -> "Auto"
        }

        return colorTemp to wbLabel
    }

    /**
     * 估算边缘密度（用于判断细节和运动模糊）
     */
    private fun estimateEdgeDensity(bitmap: Bitmap): Pair<Float, DetailLevel> {
        var edgeCount = 0
        val sampleStep = 2

        for (x in sampleStep until bitmap.width - sampleStep step sampleStep) {
            for (y in sampleStep until bitmap.height - sampleStep step sampleStep) {
                val current = bitmap.getPixel(x, y)
                val right = bitmap.getPixel(x + sampleStep, y)
                val below = bitmap.getPixel(x, y + sampleStep)

                if (isEdge(current, right) || isEdge(current, below)) {
                    edgeCount++
                }
            }
        }

        val totalPixels = (bitmap.width / sampleStep) * (bitmap.height / sampleStep)
        val edgeDensity = if (totalPixels > 0) edgeCount.toFloat() / totalPixels else 0f

        val level = when {
            edgeDensity < 0.02f -> DetailLevel.VERY_LOW
            edgeDensity < 0.05f -> DetailLevel.LOW
            edgeDensity < 0.1f -> DetailLevel.NORMAL
            edgeDensity < 0.15f -> DetailLevel.HIGH
            else -> DetailLevel.VERY_HIGH
        }

        return edgeDensity to level
    }

    private fun isEdge(@ColorInt color1: Int, @ColorInt color2: Int): Boolean {
        val r1 = Color.red(color1)
        val g1 = Color.green(color1)
        val b1 = Color.blue(color1)

        val r2 = Color.red(color2)
        val g2 = Color.green(color2)
        val b2 = Color.blue(color2)

        val diff = sqrt(
            (r1 - r2).toFloat().pow(2) +
            (g1 - g2).toFloat().pow(2) +
            (b1 - b2).toFloat().pow(2)
        )

        return diff > 30f // 阈值，可调整
    }

    /**
     * 估算对比度
     */
    private fun estimateContrast(bitmap: Bitmap): Pair<Float, ContrastLevel> {
        val sampleSize = 30
        val brightnessValues = mutableListOf<Float>()

        for (x in 0 until bitmap.width step sampleSize) {
            for (y in 0 until bitmap.height step sampleSize) {
                if (x < bitmap.width && y < bitmap.height) {
                    val pixel = bitmap.getPixel(x, y)
                    val r = Color.red(pixel) / 255f
                    val g = Color.green(pixel) / 255f
                    val b = Color.blue(pixel) / 255f
                    val brightness = 0.299f * r + 0.587f * g + 0.114f * b
                    brightnessValues.add(brightness)
                }
            }
        }

        if (brightnessValues.isEmpty()) {
            return 0.5f to ContrastLevel.NORMAL
        }

        val avgBrightness = brightnessValues.average().toFloat()
        val variance = brightnessValues.map { (it - avgBrightness).pow(2) }.average().toFloat()
        val standardDeviation = sqrt(variance)

        val contrast = standardDeviation * 2.5f

        val level = when {
            contrast < 0.15f -> ContrastLevel.VERY_LOW
            contrast < 0.25f -> ContrastLevel.LOW
            contrast < 0.4f -> ContrastLevel.NORMAL
            contrast < 0.55f -> ContrastLevel.HIGH
            else -> ContrastLevel.VERY_HIGH
        }

        return contrast to level
    }

    /**
     * 基于图像分析结果建议相机参数
     */
    fun suggestCameraParams(analysis: EstimatedCameraParams): CameraParams {
        // 基于亮度估算 ISO
        val iso = when (analysis.brightnessLevel) {
            BrightnessLevel.VERY_DARK -> 800
            BrightnessLevel.DARK -> 400
            BrightnessLevel.NORMAL_LOW -> 200
            BrightnessLevel.NORMAL -> 100
            BrightnessLevel.BRIGHT -> 64
            BrightnessLevel.VERY_BRIGHT -> 50
        }

        // 基于亮度估算快门
        val shutter = when (analysis.brightnessLevel) {
            BrightnessLevel.VERY_DARK -> "1/30"
            BrightnessLevel.DARK -> "1/60"
            BrightnessLevel.NORMAL_LOW -> "1/125"
            BrightnessLevel.NORMAL -> "1/200"
            BrightnessLevel.BRIGHT -> "1/400"
            BrightnessLevel.VERY_BRIGHT -> "1/800"
        }

        // 基于对比度估算 EV
        val ev = when (analysis.contrastLevel) {
            ContrastLevel.VERY_LOW -> "+0.7"
            ContrastLevel.LOW -> "+0.3"
            ContrastLevel.NORMAL -> "0"
            ContrastLevel.HIGH -> "-0.3"
            ContrastLevel.VERY_HIGH -> "-0.7"
        }

        // 基于色温确定白平衡
        val wb = analysis.whiteBalance

        return CameraParams(
            mode = "哈苏大师",
            iso = iso,
            shutter = shutter,
            ev = ev,
            wb = wb,
            focal_length = "24mm",
            aperture = "f/1.8",
            ai_optimization = true,
            hasselblad_hncs = true,
            hasselblad_natural_color = true,
            color_profile = determineColorProfile(analysis),
            sharpness = determineSharpness(analysis),
            contrast = determineContrastValue(analysis),
            saturation = determineSaturation(analysis)
        )
    }

    private fun determineColorProfile(analysis: EstimatedCameraParams): String {
        return when (analysis.brightnessLevel) {
            BrightnessLevel.VERY_DARK, BrightnessLevel.DARK -> "Night"
            BrightnessLevel.BRIGHT, BrightnessLevel.VERY_BRIGHT -> "Natural"
            else -> "Natural"
        }
    }

    private fun determineSharpness(analysis: EstimatedCameraParams): Int {
        return when (analysis.detailLevel) {
            DetailLevel.VERY_LOW, DetailLevel.LOW -> 65
            DetailLevel.NORMAL -> 50
            DetailLevel.HIGH -> 45
            DetailLevel.VERY_HIGH -> 40
        }
    }

    private fun determineContrastValue(analysis: EstimatedCameraParams): Int {
        return when (analysis.contrastLevel) {
            ContrastLevel.VERY_LOW -> 60
            ContrastLevel.LOW -> 55
            ContrastLevel.NORMAL -> 50
            ContrastLevel.HIGH -> 45
            ContrastLevel.VERY_HIGH -> 40
        }
    }

    private fun determineSaturation(analysis: EstimatedCameraParams): Int {
        return when {
            analysis.colorTemperature > 6500 -> 45
            analysis.colorTemperature < 4000 -> 55
            else -> 50
        }
    }
}

/**
 * 图像分析结果
 */
data class EstimatedCameraParams(
    val brightness: Float,
    val brightnessLevel: BrightnessLevel,
    val colorTemperature: Int,
    val whiteBalance: String,
    val edgeDensity: Float,
    val detailLevel: DetailLevel,
    val contrast: Float,
    val contrastLevel: ContrastLevel
)

enum class BrightnessLevel {
    VERY_DARK, DARK, NORMAL_LOW, NORMAL, BRIGHT, VERY_BRIGHT
}

enum class DetailLevel {
    VERY_LOW, LOW, NORMAL, HIGH, VERY_HIGH
}

enum class ContrastLevel {
    VERY_LOW, LOW, NORMAL, HIGH, VERY_HIGH
}

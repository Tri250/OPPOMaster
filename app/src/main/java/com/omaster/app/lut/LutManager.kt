package com.omaster.app.lut

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import com.omaster.app.model.CameraParams
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import java.io.*
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToInt

@Singleton
class LutManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    data class LutFile(
        val name: String,
        val path: String,
        val size: Int,
        val format: LutFormat,
        val type: LutType
    )

    enum class LutFormat(val extension: String, val mimeType: String) {
        CUBE(".cube", "application/octet-stream"),
        DL3(".3dl", "application/octet-stream"),
        PNG(".png", "image/png")
    }

    enum class LutType {
        IMPORT,
        EXPORT
    }

    typealias IntArray3D = Array<Array<IntArray>>

    data class Lut3D(
        val size: Int,
        val data: IntArray3D
    )

    fun importCubeLut(file: File): Result<Lut3D> {
        return try {
            val lines = file.readLines()
            var size = 0
            val dataList = mutableListOf<MutableList<FloatArray>>()
            var currentSize = mutableListOf<FloatArray>()

            lines.forEach { line ->
                val trimmed = line.trim()
                
                when {
                    trimmed.startsWith("LUT_3D_SIZE") -> {
                        size = trimmed.substringAfter("LUT_3D_SIZE").trim().toInt()
                    }
                    trimmed.startsWith("#") || trimmed.isEmpty() -> {
                        // 跳过注释和空行
                    }
                    trimmed == "TITLE" || trimmed.startsWith("TITLE") -> {
                        // 跳过标题
                    }
                    else -> {
                        // 解析 RGB 值
                        val parts = trimmed.split("\\s+".toRegex())
                        if (parts.size >= 3) {
                            val r = parts[0].toFloatOrNull() ?: 0f
                            val g = parts[1].toFloatOrNull() ?: 0f
                            val b = parts[2].toFloatOrNull() ?: 0f
                            currentSize.add(floatArrayOf(r, g, b))
                        }
                    }
                }
            }

            if (size == 0 || currentSize.isEmpty()) {
                return Result.failure(Exception("Invalid CUBE file format"))
            }

            // 转换为3D数组
            val data = Array(size) { x ->
                Array(size) { y ->
                    IntArray(size) { z ->
                        val index = x * size * size + y * size + z
                        if (index < currentSize.size) {
                            val rgb = currentSize[index]
                            Color.rgb(
                                (rgb[0].coerceIn(0f, 1f) * 255).roundToInt(),
                                (rgb[1].coerceIn(0f, 1f) * 255).roundToInt(),
                                (rgb[2].coerceIn(0f, 1f) * 255).roundToInt()
                            )
                        } else {
                            Color.rgb(128, 128, 128)
                        }
                    }
                }
            }

            Result.success(Lut3D(size, data))
        } catch (e: Exception) {
            Timber.e(e, "Failed to import CUBE LUT")
            Result.failure(e)
        }
    }

    fun exportCubeLut(params: CameraParams, outputFile: File): Result<File> {
        return try {
            val lut = cameraParamsToLut(params)
            val cubeContent = buildString {
                appendLine("TITLE \"${params.hasselblad_master_style ?: "Custom LUT"}\"")
                appendLine("LUT_3D_SIZE ${lut.size}")
                appendLine()
                
                for (b in 0 until lut.size) {
                    for (g in 0 until lut.size) {
                        for (r in 0 until lut.size) {
                            val color = lut.data[r][g][b]
                            val red = Color.red(color) / 255f
                            val green = Color.green(color) / 255f
                            val blue = Color.blue(color) / 255f
                            appendLine("%.6f %.6f %.6f".format(red, green, blue))
                        }
                    }
                }
            }
            
            FileWriter(outputFile).use { writer ->
                writer.write(cubeContent)
            }
            
            Result.success(outputFile)
        } catch (e: Exception) {
            Timber.e(e, "Failed to export CUBE LUT")
            Result.failure(e)
        }
    }

    private fun cameraParamsToLut(params: CameraParams): Lut3D {
        val size = 33 // 标准 LUT 大小
        
        val data = Array(size) { x ->
            Array(size) { y ->
                IntArray(size) { z ->
                    val r = x * 255 / (size - 1)
                    val g = y * 255 / (size - 1)
                    val b = z * 255 / (size - 1)
                    
                    // 应用相机参数调整
                    val adjusted = applyColorAdjustments(r, g, b, params)
                    Color.rgb(adjusted[0], adjusted[1], adjusted[2])
                }
            }
        }
        
        return Lut3D(size, data)
    }

    private fun applyColorAdjustments(r: Int, g: Int, b: Int, params: CameraParams): IntArray {
        var red = r.toFloat()
        var green = g.toFloat()
        var blue = b.toFloat()
        
        // 应用饱和度调整
        val saturation = params.saturation / 50f
        if (saturation != 1f) {
            val gray = (red + green + blue) / 3f
            red = gray + (red - gray) * saturation
            green = gray + (green - gray) * saturation
            blue = gray + (blue - gray) * saturation
        }
        
        // 应用对比度调整
        val contrast = params.contrast / 50f
        if (contrast != 1f) {
            val factor = (259f * (contrast * 255f + 255f)) / (255f * (259f - contrast * 255f))
            red = factor * (red - 128f) + 128f
            green = factor * (green - 128f) + 128f
            blue = factor * (blue - 128f) + 128f
        }
        
        // 应用色调调整 (基于白平衡)
        val wbValue = params.wb.replace("K", "").toIntOrNull() ?: 5500
        val wbFactor = wbValue / 5500f
        red *= wbFactor.coerceIn(0.7f, 1.3f)
        
        // 钳制值范围
        return intArrayOf(
            red.toInt().coerceIn(0, 255),
            green.toInt().coerceIn(0, 255),
            blue.toInt().coerceIn(0, 255)
        )
    }

    fun applyLutToBitmap(bitmap: Bitmap, lut: Lut3D): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        
        for (i in pixels.indices) {
            val color = pixels[i]
            val r = Color.red(color)
            val g = Color.green(color)
            val b = Color.blue(color)
            
            // 在 LUT 中查找颜色
            val lutR = (r * (lut.size - 1) / 255f).roundToInt().coerceIn(0, lut.size - 1)
            val lutG = (g * (lut.size - 1) / 255f).roundToInt().coerceIn(0, lut.size - 1)
            val lutB = (b * (lut.size - 1) / 255f).roundToInt().coerceIn(0, lut.size - 1)
            
            val newColor = lut.data[lutR][lutG][lutB]
            pixels[i] = (Color.alpha(color) shl 24) or newColor
        }
        
        result.setPixels(pixels, 0, width, 0, 0, width, height)
        return result
    }

    fun convertPresetToLut(params: CameraParams, outputDir: File): Result<File> {
        val fileName = "${params.hasselblad_master_style ?: "custom_preset"}.cube"
        val outputFile = File(outputDir, fileName)
        return exportCubeLut(params, outputFile)
    }

    fun getLutFiles(directory: File, type: LutType): List<LutFile> {
        val extension = when (type) {
            LutType.IMPORT -> listOf(".cube", ".3dl", ".png")
            LutType.EXPORT -> listOf(".cube")
        }
        
        return directory.listFiles()?.filter { file ->
            extension.any { file.extension.equals(it.removePrefix("."), ignoreCase = true) }
        }?.map { file ->
            LutFile(
                name = file.nameWithoutExtension,
                path = file.absolutePath,
                size = file.length().toInt(),
                format = when (file.extension.lowercase()) {
                    "cube" -> LutFormat.CUBE
                    "3dl" -> LutFormat.DL3
                    "png" -> LutFormat.PNG
                    else -> LutFormat.CUBE
                },
                type = type
            )
        } ?: emptyList()
    }
}
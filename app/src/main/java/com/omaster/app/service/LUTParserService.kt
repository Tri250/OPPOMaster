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
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LUTParserService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val databaseProvider: DatabaseProvider
) {

    @WorkerThread
    suspend fun parseLutFile(lutFile: File): CameraPresetEntity = withContext(Dispatchers.IO) {
        val lutData = parseCubeFile(lutFile)
        val avgWb = calculateAverageWhiteBalance(lutData)
        val saturationOffset = calculateSaturationOffset(lutData)
        val contrastOffset = calculateContrastOffset(lutData)

        CameraPresetEntity(
            id = UUID.randomUUID().toString(),
            name = lutFile.nameWithoutExtension,
            author = "LUT Import",
            coverPath = "",
            tags = "导入,LUT,色彩风格",
            scene = "通用",
            mode = "master",
            filter = "LUT",
            iso = 100,
            shutter = "1/125",
            ev = "0",
            wb = avgWb.toString(),
            deviceModel = "",
            source = "lut_import",
            isFavorite = false,
            useCount = 0,
            createTime = System.currentTimeMillis()
        )
    }

    @WorkerThread
    suspend fun parseLutToPreset(bitmap: Bitmap): CameraPresetEntity = withContext(Dispatchers.IO) {
        val avgWb = calculateWBFromBitmap(bitmap)
        val saturationOffset = calculateSaturationFromBitmap(bitmap)
        val contrastOffset = calculateContrastFromBitmap(bitmap)

        CameraPresetEntity(
            id = UUID.randomUUID().toString(),
            name = "AI 生成预设",
            author = "OMaster AI",
            coverPath = "",
            tags = "AI生成,追色",
            scene = "通用",
            mode = "master",
            filter = "AI",
            iso = 100,
            shutter = "1/125",
            ev = "0",
            wb = avgWb.toString(),
            deviceModel = "",
            source = "ai_generated",
            isFavorite = false,
            useCount = 0,
            createTime = System.currentTimeMillis()
        )
    }

    private fun parseCubeFile(file: File): LUTData {
        val data = mutableListOf<FloatArray>()
        var size = 32
        var lineIndex = 0

        BufferedReader(FileReader(file)).use { reader ->
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                val trimmedLine = line?.trim() ?: continue
                
                if (trimmedLine.startsWith("LUT_3D_SIZE")) {
                    size = trimmedLine.split("=")[1].trim().toInt()
                } else if (!trimmedLine.startsWith("#") && trimmedLine.isNotEmpty()) {
                    val values = trimmedLine.split("\\s+".toRegex())
                        .filter { it.isNotEmpty() }
                        .map { it.toFloat() }
                    if (values.size == 3) {
                        data.add(floatArrayOf(values[0], values[1], values[2]))
                    }
                    lineIndex++
                }
            }
        }

        return LUTData(size, data.toList())
    }

    private fun calculateAverageWhiteBalance(lutData: LUTData): Int {
        val neutralIndex = getNeutralIndex(lutData.size)
        val neutralColor = lutData.data.getOrNull(neutralIndex) ?: floatArrayOf(0.5f, 0.5f, 0.5f)
        
        val r = neutralColor[0]
        val g = neutralColor[1]
        val b = neutralColor[2]
        
        val avg = (r + g + b) / 3f
        val ratio = g / avg
        
        var temp = 5500f
        if (ratio > 1.02f) {
            temp = (5500f / ratio).toInt().toFloat()
        } else if (ratio < 0.98f) {
            temp = (5500f * (2f - ratio)).toInt().toFloat()
        }
        
        return temp.toInt().coerceIn(2000, 10000)
    }

    private fun calculateSaturationOffset(lutData: LUTData): Float {
        var totalSaturation = 0f
        val sampleCount = minOf(lutData.data.size, 100)
        
        for (i in 0 until sampleCount step lutData.data.size / sampleCount) {
            val color = lutData.data[i]
            val saturation = calculateSaturation(color[0], color[1], color[2])
            totalSaturation += saturation
        }
        
        val avgSaturation = totalSaturation / sampleCount
        return (avgSaturation - 0.5f) * 2f
    }

    private fun calculateContrastOffset(lutData: LUTData): Float {
        var minLuminance = Float.MAX_VALUE
        var maxLuminance = Float.MIN_VALUE
        
        for (color in lutData.data) {
            val luminance = 0.299f * color[0] + 0.587f * color[1] + 0.114f * color[2]
            minLuminance = minOf(minLuminance, luminance)
            maxLuminance = maxOf(maxLuminance, luminance)
        }
        
        val contrast = maxLuminance - minLuminance
        return contrast - 0.5f
    }

    private fun getNeutralIndex(size: Int): Int {
        val mid = size / 2
        return mid * size * size + mid * size + mid
    }

    private fun calculateSaturation(r: Float, g: Float, b: Float): Float {
        val max = maxOf(r, g, b)
        val min = minOf(r, g, b)
        
        if (max == min) return 0f
        
        val luminance = (max + min) / 2f
        val delta = max - min
        
        return if (luminance > 0.5f) {
            delta / (2f - max - min)
        } else {
            delta / (max + min)
        }
    }

    private fun calculateWBFromBitmap(bitmap: Bitmap): Int {
        var totalR = 0
        var totalG = 0
        var totalB = 0
        var pixelCount = 0
        
        val step = maxOf(1, bitmap.width * bitmap.height / 1000)
        for (y in 0 until bitmap.height step step) {
            for (x in 0 until bitmap.width step step) {
                val color = bitmap.getPixel(x, y)
                totalR += Color.red(color)
                totalG += Color.green(color)
                totalB += Color.blue(color)
                pixelCount++
            }
        }
        
        if (pixelCount == 0) return 5500
        
        val avgR = totalR / pixelCount / 255f
        val avgG = totalG / pixelCount / 255f
        val avgB = totalB / pixelCount / 255f
        
        val ratio = avgG / ((avgR + avgG + avgB) / 3f)
        var temp = 5500f
        if (ratio > 1.02f) {
            temp = 5500f / ratio
        } else if (ratio < 0.98f) {
            temp = 5500f * (2f - ratio)
        }
        
        return temp.toInt().coerceIn(2000, 10000)
    }

    private fun calculateSaturationFromBitmap(bitmap: Bitmap): Float {
        var totalSaturation = 0f
        var pixelCount = 0
        
        val step = maxOf(1, bitmap.width * bitmap.height / 500)
        for (y in 0 until bitmap.height step step) {
            for (x in 0 until bitmap.width step step) {
                val color = bitmap.getPixel(x, y)
                val r = Color.red(color) / 255f
                val g = Color.green(color) / 255f
                val b = Color.blue(color) / 255f
                
                val max = maxOf(r, g, b)
                val min = minOf(r, g, b)
                
                var saturation = 0f
                if (max != min) {
                    val luminance = (max + min) / 2f
                    val delta = max - min
                    saturation = if (luminance > 0.5f) {
                        delta / (2f - max - min)
                    } else {
                        delta / (max + min)
                    }
                }
                
                totalSaturation += saturation
                pixelCount++
            }
        }
        
        if (pixelCount == 0) return 0f
        return (totalSaturation / pixelCount - 0.5f) * 2f
    }

    private fun calculateContrastFromBitmap(bitmap: Bitmap): Float {
        var minLuminance = Float.MAX_VALUE
        var maxLuminance = Float.MIN_VALUE
        
        val step = maxOf(1, bitmap.width * bitmap.height / 500)
        for (y in 0 until bitmap.height step step) {
            for (x in 0 until bitmap.width step step) {
                val color = bitmap.getPixel(x, y)
                val luminance = (Color.red(color) * 0.299f + 
                               Color.green(color) * 0.587f + 
                               Color.blue(color) * 0.114f) / 255f
                
                minLuminance = minOf(minLuminance, luminance)
                maxLuminance = maxOf(maxLuminance, luminance)
            }
        }
        
        val contrast = maxLuminance - minLuminance
        return contrast - 0.5f
    }

    data class LUTData(
        val size: Int,
        val data: List<FloatArray>
    )
}
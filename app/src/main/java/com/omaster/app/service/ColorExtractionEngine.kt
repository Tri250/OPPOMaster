package com.omaster.app.service

import android.graphics.Bitmap
import com.omaster.app.data.PresetRepository
import com.omaster.app.model.CameraParams
import com.omaster.app.model.ColorExtractionResult
import com.omaster.app.model.ColorProfile
import com.omaster.app.model.Preset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.opencv.android.Utils
import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.MatOfFloat
import org.opencv.core.MatOfPoint
import org.opencv.imgproc.Imgproc
import kotlin.math.sqrt

class ColorExtractionEngine(
    private val presetRepository: PresetRepository
) {

    suspend fun extract(image: Bitmap): ColorExtractionResult {
        return withContext(Dispatchers.Default) {
            val mat = Mat()
            Utils.bitmapToMat(image, mat)

            val lab = Mat()
            Imgproc.cvtColor(mat, lab, Imgproc.COLOR_RGB2Lab)

            val dominantColors = kMeansCluster(lab, k = 5)

            val toneCurve = calculateToneCurve(mat)

            val presets = presetRepository.getAllPresets()
            val matchedPresets = presets.map { preset ->
                preset to cosineSimilarity(dominantColors, preset.cameraParams?.colorProfile)
            }.sortedByDescending { it.second }

            val customPreset = if (matchedPresets.firstOrNull()?.second ?: 0f < 0.7f) {
                generatePresetFromColors(dominantColors, toneCurve)
            } else {
                null
            }

            ColorExtractionResult(
                dominantColors = dominantColors,
                toneCurve = toneCurve,
                matchedPresets = matchedPresets.take(3),
                customPreset = customPreset
            )
        }
    }

    private fun kMeansCluster(lab: Mat, k: Int): List<Int> {
        val samples = Mat()
        lab.reshape(1, lab.rows() * lab.cols()).convertTo(samples, CvType.CV_32F)

        val labels = Mat()
        val centers = Mat()
        val criteria = org.opencv.core.TermCriteria(
            org.opencv.core.TermCriteria.EPS + org.opencv.core.TermCriteria.MAX_ITER,
            10,
            1.0
        )

        Core.kmeans(samples, k, labels, criteria, 3, Core.KMEANS_RANDOM_CENTERS, centers)

        val colors = mutableListOf<Int>()
        for (i in 0 until centers.rows()) {
            val labColor = centers.row(i)
            val rgbColor = Mat()
            Imgproc.cvtColor(labColor.reshape(3, 1), rgbColor, Imgproc.COLOR_Lab2RGB)
            val r = rgbColor.get(0, 0)[0].toInt()
            val g = rgbColor.get(0, 1)[0].toInt()
            val b = rgbColor.get(0, 2)[0].toInt()
            colors.add((0xFF shl 24) or (r shl 16) or (g shl 8) or b)
        }

        return colors
    }

    private fun calculateToneCurve(mat: Mat): List<Float> {
        val gray = Mat()
        Imgproc.cvtColor(mat, gray, Imgproc.COLOR_RGB2GRAY)

        val hist = Mat()
        val histSize = MatOfInt(256)
        val ranges = MatOfFloat(0f, 256f)
        Imgproc.calcHist(listOf(gray), MatOfInt(0), Mat(), hist, histSize, ranges)

        val toneCurve = mutableListOf<Float>()
        for (i in 0..255 step 32) {
            toneCurve.add(hist.get(i, 0)[0].toFloat())
        }

        return toneCurve
    }

    private fun cosineSimilarity(
        colors1: List<Int>,
        colorProfile: ColorProfile?
    ): Float {
        val colors2 = colorProfile?.dominantColors ?: return 0f
        if (colors1.size != colors2.size) return 0f

        var dotProduct = 0.0
        var norm1 = 0.0
        var norm2 = 0.0

        for (i in colors1.indices) {
            val c1 = colors1[i]
            val c2 = colors2[i]

            val r1 = (c1 shr 16) and 0xFF
            val g1 = (c1 shr 8) and 0xFF
            val b1 = c1 and 0xFF

            val r2 = (c2 shr 16) and 0xFF
            val g2 = (c2 shr 8) and 0xFF
            val b2 = c2 and 0xFF

            dotProduct += (r1 * r2 + g1 * g2 + b1 * b2).toDouble()
            norm1 += (r1 * r1 + g1 * g1 + b1 * b1).toDouble()
            norm2 += (r2 * r2 + g2 * g2 + b2 * b2).toDouble()
        }

        return if (norm1 == 0.0 || norm2 == 0.0) {
            0f
        } else {
            (dotProduct / (sqrt(norm1) * sqrt(norm2))).toFloat()
        }
    }

    private fun generatePresetFromColors(
        colors: List<Int>,
        toneCurve: List<Float>
    ): Preset {
        val avgColor = colors.firstOrNull() ?: 0xFF6200EE.toInt()
        val r = (avgColor shr 16) and 0xFF
        val g = (avgColor shr 8) and 0xFF
        val b = avgColor and 0xFF

        val saturation = ((r + g + b) / 3.0 / 255.0).toFloat().coerceIn(0.8f, 1.5f)
        val temperature = if (r > b) 0.5f else -0.5f

        return Preset(
            id = "custom_${System.currentTimeMillis()}",
            name = "Custom Filter",
            coverPath = "",
            cameraParams = CameraParams(
                saturation = saturation,
                temperature = temperature,
                contrast = 1.1f,
                vignette = 0.2f,
                colorProfile = ColorProfile(
                    dominantColors = colors,
                    toneCurve = toneCurve
                )
            )
        )
    }
}

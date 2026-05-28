package com.omaster.app.feature.aifeature

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions
import com.omaster.app.feature.aifeature.model.ColorTemperature
import com.omaster.app.feature.aifeature.model.LightingCondition
import com.omaster.app.feature.aifeature.model.SceneAnalysis
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import kotlin.math.sqrt

class SceneRecognitionEngine(private val context: Context) {

    private val labeler = ImageLabeling.getClient(
        ImageLabelerOptions.Builder()
            .setConfidenceThreshold(0.5f)
            .build()
    )

    private val objectDetector = ObjectDetection.getClient(
        ObjectDetectorOptions.Builder()
            .setDetectorMode(ObjectDetectorOptions.SINGLE_IMAGE_MODE)
            .enableMultipleObjects()
            .enableClassification()
            .build()
    )

    suspend fun analyzeScene(bitmap: Bitmap): SceneAnalysis {
        val inputImage = InputImage.fromBitmap(bitmap, 0)
        
        val labels = try {
            labeler.process(inputImage).await()
                .map { it.text to it.confidence }
                .sortedByDescending { it.second }
        } catch (e: Exception) {
            Timber.e(e, "图像标签识别失败")
            emptyList()
        }

        val objects = try {
            objectDetector.process(inputImage).await()
                .mapNotNull { it.labels.firstOrNull()?.text }
        } catch (e: Exception) {
            Timber.e(e, "物体检测失败")
            emptyList()
        }

        val lightingCondition = analyzeLighting(bitmap)
        val colorTemperature = analyzeColorTemperature(bitmap)
        val recommendedPresets = matchPresets(labels, lightingCondition, colorTemperature)

        return SceneAnalysis(
            primaryLabels = labels,
            detectedObjects = objects,
            lightingCondition = lightingCondition,
            colorTemperature = colorTemperature,
            recommendedPresetIds = recommendedPresets
        )
    }

    private fun analyzeLighting(bitmap: Bitmap): LightingCondition {
        var totalBrightness = 0.0
        val sampleSize = minOf(100, bitmap.width * bitmap.height / 10)
        
        for (i in 0 until sampleSize) {
            val x = (Math.random() * bitmap.width).toInt()
            val y = (Math.random() * bitmap.height).toInt()
            val pixel = bitmap.getPixel(x, y)
            val r = Color.red(pixel)
            val g = Color.green(pixel)
            val b = Color.blue(pixel)
            val brightness = (0.299 * r + 0.587 * g + 0.114 * b)
            totalBrightness += brightness
        }
        
        val avgBrightness = totalBrightness / sampleSize
        
        return when {
            avgBrightness > 200 -> LightingCondition.BRIGHT_SUNLIGHT
            avgBrightness > 150 -> LightingCondition.SOFT_LIGHT
            avgBrightness > 80 -> LightingCondition.LOW_LIGHT
            avgBrightness > 30 -> LightingCondition.INDOOR
            else -> LightingCondition.NIGHT
        }
    }

    private fun analyzeColorTemperature(bitmap: Bitmap): ColorTemperature {
        var totalR = 0.0
        var totalB = 0.0
        val sampleSize = minOf(100, bitmap.width * bitmap.height / 10)
        
        for (i in 0 until sampleSize) {
            val x = (Math.random() * bitmap.width).toInt()
            val y = (Math.random() * bitmap.height).toInt()
            val pixel = bitmap.getPixel(x, y)
            totalR += Color.red(pixel)
            totalB += Color.blue(pixel)
        }
        
        val avgR = totalR / sampleSize
        val avgB = totalB / sampleSize
        
        return when {
            avgR > avgB * 1.2 -> ColorTemperature.WARM
            avgB > avgR * 1.2 -> ColorTemperature.COOL
            else -> ColorTemperature.NEUTRAL
        }
    }

    private fun matchPresets(
        labels: List<Pair<String, Float>>,
        lighting: LightingCondition,
        colorTemp: ColorTemperature
    ): List<String> {
        val recommendations = mutableListOf<String>()
        
        val labelPresetMap = mapOf(
            "Portrait" to "1",
            "Person" to "1",
            "Face" to "1",
            "Sunset" to "4",
            "Sunrise" to "4",
            "Sky" to "3",
            "Nature" to "3",
            "Tree" to "3",
            "Forest" to "3",
            "Night" to "2",
            "Dark" to "2",
            "City" to "5",
            "Building" to "5",
            "Street" to "5",
            "Flower" to "6",
            "Plant" to "6",
            "Cherry" to "6",
            "Sakura" to "6"
        )
        
        labels.forEach { (label, confidence) ->
            labelPresetMap[label]?.let { presetId ->
                if (!recommendations.contains(presetId)) {
                    recommendations.add(presetId)
                }
            }
        }
        
        when (lighting) {
            LightingCondition.NIGHT -> if (!recommendations.contains("2")) recommendations.add("2")
            LightingCondition.BRIGHT_SUNLIGHT -> if (!recommendations.contains("4")) recommendations.add("4")
            LightingCondition.LOW_LIGHT -> if (!recommendations.contains("2")) recommendations.add("2")
            else -> {}
        }
        
        when (colorTemp) {
            ColorTemperature.WARM -> if (!recommendations.contains("4")) recommendations.add("4")
            ColorTemperature.COOL -> if (!recommendations.contains("3")) recommendations.add("3")
            ColorTemperature.NEUTRAL -> if (!recommendations.contains("1")) recommendations.add("1")
        }
        
        return recommendations.take(3)
    }

    fun close() {
        labeler.close()
        objectDetector.close()
    }
}

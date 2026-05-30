package com.omaster.app.ml

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.common.model.CustomRemoteModel
import com.google.mlkit.linkfirebase.FirebaseModelManager
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import com.omaster.app.model.SceneType
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@Singleton
class LocalSceneClassifier @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "LocalSceneClassifier"
        private const val CONFIDENCE_THRESHOLD = 0.65f
    }

    private val labeler = ImageLabeling.getClient(
        ImageLabelerOptions.Builder()
            .setConfidenceThreshold(CONFIDENCE_THRESHOLD)
            .build()
    )

    private val sceneKeywordMap = mapOf(
        SceneType.LANDSCAPE to setOf(
            "sky", "mountain", "sea", "ocean", "lake", "landscape", "nature",
            "outdoor", "scenery", "horizon", "cloud", "sun", "beach", "river",
            "forest", "tree", "grass", "field", "valley", "hill", "water"
        ),
        SceneType.PORTRAIT to setOf(
            "person", "people", "face", "portrait", "selfie", "human",
            "man", "woman", "boy", "girl", "child", "adult", "crowd",
            "wedding", "portrait photography"
        ),
        SceneType.NIGHT to setOf(
            "night", "dark", "evening", "cityscape at night", "neon",
            "streetlight", "lamp", "light trail", "star", "firework",
            "night photography", "city lights"
        ),
        SceneType.FOOD to setOf(
            "food", "meal", "dish", "restaurant", "cake", "dessert",
            "fruit", "vegetable", "coffee", "drink", "pizza", "sushi",
            "bakery", "gourmet", "culinary"
        ),
        SceneType.STREET to setOf(
            "street", "urban", "city", "building", "road", "sidewalk",
            "traffic", "car", "storefront", "sign", "graffiti", "alley",
            "downtown", "metropolitan"
        ),
        SceneType.ARCHITECTURE to setOf(
            "architecture", "building", "skyscraper", "tower", "bridge",
            "house", "church", "temple", "monument", "palace", "castle",
            "structure", "construction"
        ),
        SceneType.NATURE to setOf(
            "nature", "plant", "flower", "animal", "dog", "cat", "bird",
            "insect", "butterfly", "bee", "garden", "leaf", "waterfall",
            "wildlife", "pet", "botanical"
        ),
        SceneType.SUNSET to setOf(
            "sunset", "sunrise", "dusk", "dawn", "golden hour", "twilight",
            "sun", "silhouette", "warm", "orange sky", "evening"
        ),
        SceneType.MACRO to setOf(
            "macro", "close-up", "detail", "texture", "pattern", "jewelry",
            "watch", "insect close-up", "flower close-up", "product"
        ),
        SceneType.SPORTS to setOf(
            "sports", "football", "basketball", "soccer", "tennis", "running",
            "swimming", "cycling", "skiing", "baseball", "hockey", "golf",
            "athlete", "fitness", "workout"
        ),
        SceneType.NIGHT_PORTRAIT to setOf(
            "night portrait", "flash photography", "low light portrait",
            "evening portrait", "nightlife", "party", "concert"
        ),
        SceneType.VINTAGE to setOf(
            "vintage", "retro", "old", "antique", "classic", "nostalgic",
            "film", "analog", "1970s", "1980s", "1990s"
        ),
        SceneType.CINEMATIC to setOf(
            "cinematic", "movie", "film still", "drama", "action", "scene",
            "widescreen", "movie set", "Hollywood"
        ),
        SceneType.BLACK_WHITE to setOf(
            "black and white", "monochrome", "grayscale", "bw", "b&w",
            "contrast", "shadows"
        )
    )

    private val edgeCaseKeywords = mapOf(
        SceneType.BLACK to setOf("black", "darkness", "shadow"),
        SceneType.WHITE to setOf("white", "snow", "overexposed", "bright"),
        SceneType.BLURRY to setOf("blur", "motion blur", "out of focus")
    )

    suspend fun classify(bitmap: Bitmap): SceneClassification = withContext(Dispatchers.IO) {
        try {
            val inputImage = InputImage.fromBitmap(bitmap, 0)
            val labels = awaitImageLabels(labeler.process(inputImage))

            if (labels.isEmpty()) {
                Log.d(TAG, "No labels detected, returning UNKNOWN")
                return@withContext SceneClassification(SceneType.UNKNOWN, 0f)
            }

            val primaryLabel = labels.first()
            val labelText = primaryLabel.text.lowercase()
            val confidence = primaryLabel.confidence

            Log.d(TAG, "Primary label: $labelText (confidence: $confidence)")

            // 检测边界情况
            val edgeCase = detectEdgeCase(labels)
            if (edgeCase != null) {
                Log.d(TAG, "Edge case detected: ${edgeCase.sceneType}")
                return@withContext edgeCase
            }

            // 映射到场景类型
            val sceneType = mapLabelsToSceneType(labels)

            Log.d(TAG, "Classified as: $sceneType")
            SceneClassification(sceneType, confidence)

        } catch (e: Exception) {
            Log.e(TAG, "Classification failed: ${e.message}", e)
            SceneClassification(SceneType.UNKNOWN, 0f)
        }
    }

    private fun detectEdgeCase(labels: List<com.google.mlkit.vision.label.ImageLabel>): SceneClassification? {
        val labelTexts = labels.map { it.text.lowercase() }

        // 检测过暗
        if (labelTexts.any { it in edgeCaseKeywords[SceneType.BLACK]!! }) {
            val darkLabel = labels.find { it.text.lowercase() in edgeCaseKeywords[SceneType.BLACK]!! }
            if (darkLabel != null && darkLabel.confidence > 0.8f) {
                return SceneClassification(
                    SceneType.BLACK,
                    darkLabel.confidence,
                    isEdgeCase = true,
                    edgeCaseMessage = "光线太暗，建议增加曝光或使用闪光灯"
                )
            }
        }

        // 检测过亮/过曝
        if (labelTexts.any { it in edgeCaseKeywords[SceneType.WHITE]!! }) {
            val whiteLabel = labels.find { it.text.lowercase() in edgeCaseKeywords[SceneType.WHITE]!! }
            if (whiteLabel != null && whiteLabel.confidence > 0.8f) {
                return SceneClassification(
                    SceneType.WHITE,
                    whiteLabel.confidence,
                    isEdgeCase = true,
                    edgeCaseMessage = "画面过亮，可能过曝"
                )
            }
        }

        return null
    }

    private fun mapLabelsToSceneType(labels: List<com.google.mlkit.vision.label.ImageLabel>): SceneType {
        val labelScores = mutableMapOf<SceneType, Float>()

        for ((sceneType, keywords) in sceneKeywordMap) {
            var score = 0f
            for (label in labels) {
                val labelText = label.text.lowercase()
                if (keywords.any { keyword -> labelText.contains(keyword) || keyword.contains(labelText) }) {
                    score += label.confidence
                }
            }
            if (score > 0) {
                labelScores[sceneType] = score
            }
        }

        return labelScores.maxByOrNull { it.value }?.key ?: SceneType.UNKNOWN
    }

    private suspend fun <T> awaitImageLabels(task: com.google.android.gms.tasks.Task<T>): T {
        return suspendCancellableCoroutine { continuation ->
            task.addOnSuccessListener { result ->
                if (continuation.isActive) {
                    continuation.resume(result)
                }
            }.addOnFailureListener { exception ->
                if (continuation.isActive) {
                    continuation.resumeWithException(exception)
                }
            }
        }
    }

    fun close() {
        labeler.close()
    }
}

data class SceneClassification(
    val sceneType: SceneType,
    val confidence: Float,
    val isEdgeCase: Boolean = false,
    val edgeCaseMessage: String? = null
)

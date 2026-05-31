package com.omaster.app.ai

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.FileInputStream
import kotlin.math.roundToInt

/**
 * AI 运行时管理器
 * 统一管理 AI 模型加载、推理、异常处理
 */
class AiRuntimeManager(private val context: Context) {

    companion object {
        private const val MAX_BITMAP_MAX_WIDTH = 1080
        private const val BITMAP_QUALITY = 80
        private const val INFERENCE_TIMEOUT_MS = 3000L
        private const val MODEL_LOADING_TIMEOUT_MS = 10000L
    }

    private val _state = MutableStateFlow<AiState>(AiState.Idle)
    val state: StateFlow<AiState> = _state

    private var isModelLoaded = false

    sealed interface AiState {
        object Idle : AiState
        object LoadingModel : AiState
        object Analyzing : AiState
        data class Success(val result: AiSceneResult) : AiState
        data class Error(val message: String) : AiState
    }

    data class AiSceneResult(
        val sceneType: String,
        val confidence: Float,
        val recommendedPresets: List<String>
    )

    /**
     * 初始化 AI 模型（异步）
     */
    suspend fun initializeModel(): Boolean {
        if (isModelLoaded) return

        try {
            _state.value = AiState.LoadingModel
            Timber.d("Starting AI model initialization")

            // 模拟模型加载
            withContext(Dispatchers.Default) {
                // 实际项目中应该加载实际的 TFLite 或 ML Kit 模型
                delay(500)
                isModelLoaded = true
                _state.value = AiState.Idle
                Timber.d("AI model loaded successfully")
                true
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to load AI model")
            _state.value = AiState.Error("AI模型加载失败，请检查应用版本")
            false
        }
    }

    /**
     * 分析图片场景（带超时和异常处理）
     */
    suspend fun analyzeScene(imagePath: String): AiState {
        return withContext(Dispatchers.Default) {
            try {
                _state.value = AiState.Analyzing

                // 1. 检查模型是否已加载
                if (!isModelLoaded) {
                    val loaded = initializeModel()
                    if (!loaded) {
                        return@withContext AiState.Error("AI模型未加载失败")
                    }
                }

                // 2. 加载并下采样图片（防 OOM）
                val bitmap = loadAndDownscaleBitmap(imagePath)

                // 3. 执行推理（带超时）
                val result = kotlinx.coroutines.withTimeoutOrNull(INFERENCE_TIMEOUT_MS) {
                    performInference(bitmap)
                }

                if (result != null) {
                    _state.value = AiState.Success(result)
                    AiState.Success(result)
                } else {
                    _state.value = AiState.Error("识别超时，请尝试更小的图片")
                    AiState.Error("识别超时，请尝试更小的图片")
                }
            } catch (e: Exception) {
                Timber.e(e, "AI scene analysis failed")
                val errorMessage = when (e) {
                    is OutOfMemoryError -> "图片太大，请选择更小的图片"
                    else -> "识别失败：${e.message}"
                }
                _state.value = AiState.Error(errorMessage)
                AiState.Error(errorMessage)
            }
        }
    }

    /**
     * 批量分析（带并发控制
     */
    suspend fun analyzeScenes(imagePaths: List<String>, maxConcurrency: Int = 2): List<AiState> {
        val results = mutableListOf<AiState>()

        for (i in imagePaths.indices step maxConcurrency) {
            val batch = imagePaths.subList(i, (i + maxConcurrency).coerceAtMost(imagePaths.size))
            val batchResults = batch.map { path ->
            analyzeScene(path)
        }
            results.addAll(batchResults)
        }

        return results
    }

    /**
     * 加载并下采样图片，防止 OOM
     */
    private fun loadAndDownscaleBitmap(imagePath: String): Bitmap {
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        BitmapFactory.decodeFile(imagePath, options)

        val width = options.outWidth
        val height = options.outHeight

        val sampleSize = calculateInSampleSize(width, height, MAX_BITMAP_MAX_WIDTH)

        options.apply {
            inJustDecodeBounds = false
            inSampleSize = sampleSize
            inPreferredConfig = Bitmap.Config.RGB_565
        }

        return BitmapFactory.decodeFile(imagePath, options)
    }

    private fun calculateInSampleSize(width: Int, height: Int, maxWidth: Int): Int {
        var inSampleSize = 1

        if (width > maxWidth) {
            val halfWidth = width / 2
            val halfHeight = height / 2

            while ((halfWidth / inSampleSize) >= maxWidth) {
                inSampleSize *= 2
            }
        }

        return inSampleSize
    }

    /**
     * 执行推理（模拟实现）
     */
    private suspend fun performInference(bitmap: Bitmap): AiSceneResult {
        // 实际项目中应该替换为真实的 TFLite 或 ML Kit 推理
        delay(300)

        return AiSceneResult(
            sceneType = "风景",
            confidence = 0.85f,
            recommendedPresets = listOf("哈苏风景大师", "电影感风景")
        )
    }

    /**
     * 重置状态
     */
    fun reset() {
        _state.value = AiState.Idle
    }

    /**
     * 检查设备是否支持 AI 功能
     */
    fun isAiSupported(): Boolean {
        // 实际项目中可以检查是否有 NDK 支持、是否有 GPU 等
        return true
    }
}

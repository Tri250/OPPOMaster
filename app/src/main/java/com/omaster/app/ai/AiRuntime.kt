package com.omaster.app.ai

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/**
 * AI运行时管理器 - 单例模式
 * 负责模型加载、降级策略、可用性管理
 */
object AiRuntime {

    @Volatile
    private var isModelLoaded: Boolean = false

    @Volatile
    private var isAvailable: Boolean = true

    private val lock = Any()

    /**
     * 确保模型已加载（线程安全）
     */
    suspend fun ensureModelLoaded(context: Context): Boolean = withContext(Dispatchers.IO) {
        if (isModelLoaded) return@withContext true

        synchronized(lock) {
            if (isModelLoaded) return@withContext true
        }

        try {
            // 模拟模型加载 - 实际项目中加载TFLite/NCNN模型
            loadModelInternal(context)

            synchronized(lock) {
                isModelLoaded = true
                isAvailable = true
            }
            true
        } catch (e: Exception) {
            synchronized(lock) {
                isAvailable = false
            }
            false
        }
    }

    /**
     * 内部模型加载
     */
    private suspend fun loadModelInternal(context: Context) {
        // 实际项目中：
        // 1. 从assets或内部存储加载模型文件
        // 2. 初始化TFLite/NCNN解释器
        // 3. 验证模型完整性

        delay(500) // 模拟加载时间
    }

    /**
     * 检查AI是否可用
     */
    fun isAiAvailable(): Boolean = isAvailable && isModelLoaded

    /**
     * 获取AI可用性状态
     */
    fun getAvailabilityStatus(): AiAvailabilityStatus {
        return when {
            !isAvailable -> AiAvailabilityStatus.ERROR
            !isModelLoaded -> AiAvailabilityStatus.LOADING
            else -> AiAvailabilityStatus.READY
        }
    }

    /**
     * 标记AI不可用（降级策略）
     */
    fun markAsUnavailable() {
        synchronized(lock) {
            isAvailable = false
        }
    }

    /**
     * 重置AI状态
     */
    fun reset() {
        synchronized(lock) {
            isModelLoaded = false
            isAvailable = true
        }
    }
}

/**
 * AI可用性状态
 */
enum class AiAvailabilityStatus {
    READY,      // 就绪
    LOADING,    // 加载中
    ERROR       // 错误
}

/**
 * 功能开关管理
 */
object FeatureFlags {
    var isAiSceneDetectionEnabled: Boolean = true
        private set

    var isAiFineTuningEnabled: Boolean = true
        private set

    fun updateAiSceneDetectionEnabled(enabled: Boolean) {
        isAiSceneDetectionEnabled = enabled
    }

    fun updateAiFineTuningEnabled(enabled: Boolean) {
        isAiFineTuningEnabled = enabled
    }

    fun disableAllAiFeatures() {
        isAiSceneDetectionEnabled = false
        isAiFineTuningEnabled = false
    }
}


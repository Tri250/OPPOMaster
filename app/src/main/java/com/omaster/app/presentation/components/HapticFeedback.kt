package com.omaster.app.presentation.components

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.content.ContextCompat

/**
 * 震动反馈强度枚举
 */
enum class HapticIntensity {
    LIGHT,      // 轻触反馈
    MEDIUM,     // 中触反馈
    HEAVY       // 重触反馈
}

/**
 * 震动反馈工具类
 * 封装 HapticFeedbackType 的使用，提供统一的震动反馈接口
 *
 * @param context 应用上下文
 * @param view 当前视图（用于系统级触觉反馈）
 */
class HapticFeedbackHelper(
    private val context: Context,
    private val view: View
) {
    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        vibratorManager.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    /**
     * 检查设备是否支持震动
     */
    fun hasVibrator(): Boolean {
        return vibrator?.hasVibrator() ?: false
    }

    /**
     * 检查设备是否支持高级触觉反馈 (Android 8.0+)
     */
    @RequiresApi(Build.VERSION_CODES.O)
    fun hasAmplitudeControl(): Boolean {
        return vibrator?.hasAmplitudeControl() ?: false
    }

    /**
     * 执行震动反馈
     *
     * @param intensity 震动强度
     */
    fun performHapticFeedback(intensity: HapticIntensity) {
        when (intensity) {
            HapticIntensity.LIGHT -> performLightHaptic()
            HapticIntensity.MEDIUM -> performMediumHaptic()
            HapticIntensity.HEAVY -> performHeavyHaptic()
        }
    }

    /**
     * 轻触反馈 - 适用于按钮点击、轻触等场景
     */
    fun performLightHaptic() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android 10+ 使用预定义效果
            vibrator?.vibrate(
                VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK)
            )
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Android 8.0+ 使用自定义效果
            vibrator?.vibrate(
                VibrationEffect.createOneShot(10, VibrationEffect.DEFAULT_AMPLITUDE)
            )
        } else {
            // 低版本使用系统触觉反馈
            @Suppress("DEPRECATION")
            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
        }
    }

    /**
     * 中触反馈 - 适用于确认操作、切换开关等场景
     */
    fun performMediumHaptic() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            vibrator?.vibrate(
                VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK)
            )
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator?.vibrate(
                VibrationEffect.createOneShot(20, VibrationEffect.DEFAULT_AMPLITUDE)
            )
        } else {
            @Suppress("DEPRECATION")
            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        }
    }

    /**
     * 重触反馈 - 适用于重要操作、错误提示等场景
     */
    fun performHeavyHaptic() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            vibrator?.vibrate(
                VibrationEffect.createPredefined(VibrationEffect.EFFECT_DOUBLE_CLICK)
            )
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // 创建双击效果
            val timings = longArrayOf(0, 30, 50, 30)
            val amplitudes = intArrayOf(0, 255, 0, 255)
            vibrator?.vibrate(
                VibrationEffect.createWaveform(timings, amplitudes, -1)
            )
        } else {
            @Suppress("DEPRECATION")
            view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
        }
    }

    /**
     * 成功反馈 - 适用于操作成功场景
     */
    fun performSuccessHaptic() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            vibrator?.vibrate(
                VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK)
            )
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator?.vibrate(
                VibrationEffect.createOneShot(15, 128)
            )
        } else {
            @Suppress("DEPRECATION")
            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
        }
    }

    /**
     * 错误反馈 - 适用于操作失败场景
     */
    fun performErrorHaptic() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // 错误反馈使用短促的两次震动
            val timings = longArrayOf(0, 40, 80, 40)
            val amplitudes = intArrayOf(0, 200, 0, 200)
            vibrator?.vibrate(
                VibrationEffect.createWaveform(timings, amplitudes, -1)
            )
        } else {
            @Suppress("DEPRECATION")
            view.performHapticFeedback(HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING)
        }
    }

    /**
     * 自定义震动模式
     *
     * @param timings 震动时长数组（毫秒）
     * @param amplitudes 震动强度数组 (0-255)
     * @param repeat 重复次数，-1表示不重复
     */
    @RequiresApi(Build.VERSION_CODES.O)
    fun performCustomHaptic(timings: LongArray, amplitudes: IntArray, repeat: Int = -1) {
        vibrator?.vibrate(
            VibrationEffect.createWaveform(timings, amplitudes, repeat)
        )
    }
}

/**
 * 创建并记住 HapticFeedbackHelper 实例
 * 在 Composable 中使用
 */
@Composable
fun rememberHapticFeedbackHelper(): HapticFeedbackHelper {
    val context = LocalContext.current
    val view = LocalView.current
    return remember {
        HapticFeedbackHelper(context, view)
    }
}

/**
 * 便捷的 Composable 扩展函数
 * 用于在点击事件中快速添加触觉反馈
 *
 * 使用示例：
 * ```
 * Button(
 *     onClick = {
 *         haptic.performClickHaptic(HapticIntensity.LIGHT)
 *         // 执行点击逻辑
 *     }
 * ) { Text("点击我") }
 * ```
 */
@Composable
fun HapticFeedbackHelper.performClickHaptic(intensity: HapticIntensity = HapticIntensity.LIGHT) {
    performHapticFeedback(intensity)
}

/**
 * 触觉反馈配置对象
 * 用于统一管理应用内的触觉反馈行为
 */
object HapticConfig {
    /**
     * 是否启用触觉反馈
     */
    var enabled: Boolean = true

    /**
     * 全局震动强度倍率 (0.0 - 2.0)
     */
    var intensityMultiplier: Float = 1.0f

    /**
     * 检查并执行触觉反馈
     */
    fun HapticFeedbackHelper.performIfEnabled(intensity: HapticIntensity) {
        if (enabled) {
            performHapticFeedback(intensity)
        }
    }
}

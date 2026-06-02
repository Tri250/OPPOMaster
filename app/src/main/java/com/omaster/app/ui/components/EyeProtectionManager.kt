package com.omaster.app.ui.components

import android.app.Activity
import android.content.Context
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.PixelFormat
import android.os.Build
import android.view.View
import android.view.WindowManager
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalView
import com.omaster.app.data.EyeProtectionMode
import timber.log.Timber
import kotlin.math.abs

/**
 * 护眼模式管理器 - 专业级实现
 * - 真实 ColorMatrix 蓝光过滤
 * - WindowManager 全局覆盖层支持
 * - 智能色温调节（3000K-6500K）
 * - 透明度可调
 */
object EyeProtectionManager {
    private var overlayView: View? = null
    private var windowManager: WindowManager? = null
    private var isActive = false

    /**
     * 计算护眼模式的 ColorMatrix
     * @param intensity 护眼强度 0.0-1.0
     * @param colorTemperature 色温 3000K-6500K
     */
    fun calculateColorMatrix(intensity: Float, colorTemperature: Int): ColorMatrix {
        val matrix = ColorMatrix()
        val safeIntensity = intensity.coerceIn(0f, 1f)
        val safeTemp = colorTemperature.coerceIn(3000, 6500)

        val tempRatio = (6500 - safeTemp).toFloat() / 3500f
        val blueReduction = safeIntensity * tempRatio

        val r = 1f
        val g = 1f
        val b = (1f - blueReduction).coerceIn(0.4f, 1f)

        val warm = 1f + (safeIntensity * 0.08f)
        val r2 = r + (safeIntensity * 0.05f * warm)
        val g2 = g

        matrix.set(floatArrayOf(
            r2, 0f, 0f, 0f, 0f,
            0f, g2, 0f, 0f, 0f,
            0f, 0f, b, 0f, 0f,
            0f, 0f, 0f, 1f, 0f
        ))

        val saturationScale = 1f - (safeIntensity * 0.1f)
        val saturationMatrix = ColorMatrix().apply { setSaturation(saturationScale) }
        matrix.postConcat(saturationMatrix)

        return matrix
    }

    /**
     * 应用护眼模式到 Activity
     */
    fun applyToActivity(activity: Activity, mode: EyeProtectionMode, intensity: Float) {
        try {
            val window = activity.window
            val decorView = window.decorView

            if (mode == EyeProtectionMode.OFF || intensity <= 0f) {
                clearFromActivity(activity)
                return
            }

            val colorTemperature = mode.colorTemperature
            val matrix = calculateColorMatrix(intensity, colorTemperature)
            val paint = decorView.paint
            paint.colorFilter = ColorMatrixColorFilter(matrix)
            isActive = true
            Timber.d("护眼模式已应用: $mode, 强度: $intensity, 色温: ${colorTemperature}K")
        } catch (e: Exception) {
            Timber.e(e, "应用护眼模式失败")
        }
    }

    /**
     * 清除 Activity 上的护眼效果
     */
    fun clearFromActivity(activity: Activity) {
        try {
            activity.window.decorView.paint.colorFilter = null
            isActive = false
            Timber.d("护眼模式已关闭")
        } catch (e: Exception) {
            Timber.e(e, "清除护眼模式失败")
        }
    }

    /**
     * 启用系统级护眼覆盖层（需要权限）
     */
    fun enableOverlay(context: Context, intensity: Float, colorTemperature: Int) {
        if (!Settings.canDrawOverlays(context)) {
            Timber.w("无悬浮窗权限，无法启用系统级护眼")
            return
        }

        try {
            disableOverlay()

            val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            windowManager = wm

            val overlayView = View(context).apply {
                setBackgroundColor(android.graphics.Color.argb(
                    (intensity * 80).toInt().coerceIn(0, 128),
                    255, 200, 100
                ))
            }

            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                } else {
                    @Suppress("DEPRECATION")
                    WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY
                },
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
            )

            wm.addView(overlayView, params)
            this.overlayView = overlayView
            Timber.d("系统级护眼覆盖层已启用")
        } catch (e: Exception) {
            Timber.e(e, "启用系统级护眼覆盖层失败")
        }
    }

    /**
     * 禁用系统级护眼覆盖层
     */
    fun disableOverlay() {
        try {
            overlayView?.let {
                windowManager?.removeView(it)
            }
            overlayView = null
            windowManager = null
            Timber.d("系统级护眼覆盖层已禁用")
        } catch (e: Exception) {
            Timber.e(e, "禁用护眼覆盖层失败")
        }
    }

    fun isActive(): Boolean = isActive
}

/**
 * Composable: 在 Composable 生命周期内应用护眼模式
 */
@Composable
fun rememberEyeProtectionState(): EyeProtectionState {
    val view = LocalView.current
    val modeState = remember { mutableStateOf(EyeProtectionMode.OFF) }
    val intensityState = remember { mutableFloatStateOf(0.3f) }

    DisposableEffect(view) {
        onDispose {
            val activity = view.context as? Activity
            activity?.let { EyeProtectionManager.clearFromActivity(it) }
        }
    }

    return EyeProtectionState(
        mode = modeState,
        intensity = intensityState,
        view = view
    )
}

data class EyeProtectionState(
    val mode: MutableState<EyeProtectionMode>,
    val intensity: MutableState<Float>,
    val view: View
) {
    fun apply() {
        val activity = view.context as? Activity ?: return
        EyeProtectionManager.applyToActivity(activity, mode.value, intensity.value)
    }

    fun clear() {
        val activity = view.context as? Activity ?: return
        EyeProtectionManager.clearFromActivity(activity)
    }

    fun setMode(newMode: EyeProtectionMode) {
        mode.value = newMode
        if (newMode == EyeProtectionMode.OFF) clear() else apply()
    }

    fun setIntensity(newIntensity: Float) {
        intensity.value = newIntensity.coerceIn(0f, 1f)
        if (mode.value != EyeProtectionMode.OFF) apply()
    }
}

/**
 * 估算护眼模式下的预期色温值
 */
fun estimateEffectiveColorTemperature(baseTemp: Int, intensity: Float): Int {
    val safeIntensity = intensity.coerceIn(0f, 1f)
    return baseTemp - (safeIntensity * 1500).toInt()
}

/**
 * 智能护眼：根据时间和环境光自动建议护眼强度
 */
fun suggestAutoEyeProtection(
    isDarkEnvironment: Boolean,
    hourOfDay: Int
): Pair<EyeProtectionMode, Float> {
    return when {
        isDarkEnvironment -> EyeProtectionMode.STRONG to 0.7f
        hourOfDay in 19..23 || hourOfDay in 0..5 ->
            EyeProtectionMode.NORMAL to 0.5f
        hourOfDay in 16..18 ->
            EyeProtectionMode.LIGHT to 0.3f
        else -> EyeProtectionMode.OFF to 0f
    }
}

private object Settings {
    fun canDrawOverlays(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            android.provider.Settings.canDrawOverlays(context)
        } else true
    }
}

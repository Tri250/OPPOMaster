package com.omaster.app.ui.animation

import androidx.compose.animation.core.*

/**
 * 动画配置常量 - 简化版
 * 兼容 Compose Material3 最新版本
 */
object AnimationConfig {

    // ==================== 页面转场动画时长 ====================
    const val PAGE_TRANSITION_DURATION = 350
    const val PAGE_TRANSITION_DURATION_SHORT = 280

    // ==================== 微交互动画时长 ====================
    const val MICRO_INTERACTION_DURATION = 120
    const val MICRO_INTERACTION_DURATION_LONG = 180

    // ==================== 状态切换动画时长 ====================
    const val STATE_TRANSITION_DURATION = 220
    const val STATE_TRANSITION_DURATION_FAST = 160

    // ==================== 悬浮窗动画时长 ====================
    const val FLOATING_WINDOW_DURATION = 280

    // ==================== 骨架屏动画时长 ====================
    const val SKELETON_SWEEP_DURATION = 1500
    const val SKELETON_FADE_DURATION = 300

    // ==================== 新标签呼吸动画时长 ====================
    const val NEW_TAG_BREATHING_DURATION = 1800

    // ==================== 提示信息动画时长 ====================
    const val SNACKBAR_DURATION = 250
    const val TOAST_DURATION = 200

    // ==================== 卡片缩放参数 ====================
    const val CARD_PRESS_SCALE = 0.96f
    const val CARD_PRESS_ALPHA = 0.92f
}

/**
 * ColorOS 动画时长常量
 */
object ColorOSAnimationDuration {
    const val FAST = 150
    const val MEDIUM = 250
    const val SLOW = 350
    const val VERY_SLOW = 500
}

/**
 * ColorOS 缓动曲线
 */
object ColorOSEasing {
    val Standard = FastOutSlowInEasing
    val Decelerate = FastOutLinearInEasing
    val Accelerate = LinearOutSlowInEasing
    val Linear = LinearEasing
}

// 兼容旧引用
val AnimationConfig.ColorOSDefaultEasing get() = ColorOSEasing.Standard
val AnimationConfig.ColorOSDecelerateEasing get() = ColorOSEasing.Decelerate
val AnimationConfig.FastOutSlowInEasing get() = FastOutSlowInEasing
val AnimationConfig.LinearOutSlowInEasing get() = LinearOutSlowInEasing
val AnimationConfig.FastOutLinearInEasing get() = FastOutLinearInEasing
val AnimationConfig.SoftSpringSpec get() = spring<Float>(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)

/**
 * ColorOS 缩放常量
 */
object ColorOSScale {
    const val Pressed = 0.95f
    const val Hover = 1.02f
    const val Active = 1.0f
}

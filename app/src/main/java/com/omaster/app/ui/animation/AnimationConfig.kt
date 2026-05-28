package com.omaster.app.ui.animation

import androidx.compose.animation.core.*
import androidx.compose.ui.unit.Dp

// ==================== ColorOS 16 动画系统 ====================
// ColorOS 16 强调流畅、自然的动效，符合人体工学的视觉反馈
object AnimationConfig {
    
    // 页面转场动画 - ColorOS 16 轻盈流畅
    const val PAGE_TRANSITION_DURATION = 350
    const val PAGE_TRANSITION_DURATION_SHORT = 280
    
    // 微交互动画 - ColorOS 16 精致细腻
    const val MICRO_INTERACTION_DURATION = 120
    const val MICRO_INTERACTION_DURATION_LONG = 180
    
    // 状态切换动画 - ColorOS 16 自然过渡
    const val STATE_TRANSITION_DURATION = 220
    const val STATE_TRANSITION_DURATION_FAST = 160
    
    // 悬浮窗动画 - ColorOS 16 优雅流畅
    const val FLOATING_WINDOW_DURATION = 280
    
    // 骨架屏动画 - ColorOS 16 柔和渐变
    const val SKELETON_SWEEP_DURATION = 1500
    const val SKELETON_FADE_DURATION = 300
    
    // 新标签呼吸动画 - ColorOS 16 脉动效果
    const val NEW_TAG_BREATHING_DURATION = 1800
    
    // 提示信息动画 - ColorOS 16 温和出现
    const val SNACKBAR_DURATION = 250
    const val TOAST_DURATION = 200
    
    // 卡片缩放动画 - ColorOS 16 触压反馈
    const val CARD_PRESS_SCALE = 0.96f
    const val CARD_PRESS_ALPHA = 0.92f
    
    // ==================== ColorOS 16 缓动曲线 ====================
    // ColorOS 16 专属缓动 - 自然流畅，符合物理规律
    val ColorOSDefaultEasing = CubicBezierEasing(0.33f, 0.0f, 0.67f, 1.0f)
    val ColorOSDecelerateEasing = CubicBezierEasing(0.0f, 0.0f, 0.33f, 1.0f)
    val ColorOSAccelerateEasing = CubicBezierEasing(0.67f, 0.0f, 1.0f, 1.0f)
    
    // 标准缓动 - 保留兼容性
    val FastOutSlowInEasing = CubicBezierEasing(0.4f, 0.0f, 0.2f, 1.0f)
    val LinearOutSlowInEasing = CubicBezierEasing(0.0f, 0.0f, 0.2f, 1.0f)
    val FastOutLinearInEasing = CubicBezierEasing(0.4f, 0.0f, 1.0f, 1.0f)
    val MicroInteractionEasing = CubicBezierEasing(0.4f, 0.0f, 0.2f, 1.0f)
    
    // ==================== ColorOS 16 弹簧效果 ====================
    // 标准弹簧 - 适中回弹
    val SpringSpec = SpringSpec(
        dampingRatio = 0.85f,
        stiffness = 300f,
        visibilityThreshold = Dp(0.5f).value
    )
    
    // 轻柔弹簧 - 微妙回弹
    val SoftSpringSpec = SpringSpec(
        dampingRatio = 0.92f,
        stiffness = 250f,
        visibilityThreshold = Dp(0.3f).value
    )
    
    // 活泼弹簧 - 明显回弹
    val BouncySpringSpec = SpringSpec(
        dampingRatio = 0.75f,
        stiffness = 400f,
        visibilityThreshold = Dp(0.8f).value
    )
    
    // 衰减动画 - ColorOS 16 平滑滚动
    val SmoothDecay = DecayAnimationSpec(
        frictionMultiplier = 0.99f,
        absVelocityThreshold = 0.1f
    )
}

// 便捷访问
val Float.Companion.DefaultEasing: CubicBezierEasing
    get() = AnimationConfig.ColorOSDefaultEasing

val Float.Companion.DecelerateEasing: CubicBezierEasing
    get() = AnimationConfig.ColorOSDecelerateEasing

val Float.Companion.AccelerateEasing: CubicBezierEasing
    get() = AnimationConfig.ColorOSAccelerateEasing

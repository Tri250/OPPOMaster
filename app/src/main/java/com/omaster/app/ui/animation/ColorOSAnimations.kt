package com.omaster.app.ui.animation

import androidx.compose.animation.core.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.scale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * ==================== ColorOS 16 动画时长规范 ====================
 */
object ColorOSAnimationDuration {
    // 微交互 - 极快
    const val MICRO = 80
    
    // 快速交互
    const val FAST = 150
    
    // 标准动画
    const val MEDIUM = 250
    
    // 流畅过渡
    const val SLOW = 350
    
    // 复杂动画
    const val LONG = 500
}

/**
 * ==================== ColorOS 16 缓动曲线规范 ====================
 */
object ColorOSEasing {
    // 标准缓动 - 自然的进入和退出
    val Standard = CubicBezierEasing(0.4f, 0.0f, 0.2f, 1.0f)
    
    // 加速缓动 - 元素离开屏幕
    val Accelerate = CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f)
    
    // 减速缓动 - 元素进入屏幕
    val Decelerate = CubicBezierEasing(0.0f, 0.0f, 0.2f, 1.0f)
    
    // 弹性缓动 - 有弹性的效果
    val Springy = CubicBezierEasing(0.34f, 1.56f, 0.64f, 1.0f)
}

/**
 * ==================== ColorOS 16 缩放规范 ====================
 */
object ColorOSScale {
    // 按压时的缩放
    const val Pressed = 0.96f
    
    // 悬停时的缩放
    const val Hovered = 0.98f
    
    // 强调时的缩放
    const val Emphasize = 1.02f
}

/**
 * ==================== ColorOS 16 阴影层级规范 ====================
 */
object ColorOSElevation {
    // 无阴影
    val None = 0.dp
    
    // 扁平卡片
    val Flat = 1.dp
    
    // 标准卡片
    val Card = 2.dp
    
    // 提升卡片
    val Elevated = 4.dp
    
    // 聚焦状态
    val Focus = 6.dp
    
    // 模态/对话框
    val Modal = 8.dp
}

/**
 * ==================== ColorOS 16 按压反馈 Modifier ====================
 */
fun Modifier.clickableWithColorOSFeedback(
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() }
): Modifier = composed {
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed) ColorOSScale.Pressed else 1f,
        animationSpec = spring(
            dampingRatio = 0.85f,
            stiffness = 300f
        ),
        label = "colorosScale"
    )
    
    this.scale(scale)
}

/**
 * ==================== ColorOS 16 进入动画规范 ====================
 */
@Composable
fun rememberEnterTransition(
    delay: Int = 0
): Float {
    val animation = remember { Animatable(0f) }
    
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(delay.toLong())
        animation.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = ColorOSAnimationDuration.MEDIUM,
                easing = ColorOSEasing.Decelerate
            )
        )
    }
    
    return animation.value
}

/**
 * ==================== ColorOS 16 呼吸动画 ====================
 */
@Composable
fun rememberBreathingAnimation(
    minScale: Float = 0.98f,
    maxScale: Float = 1.02f,
    duration: Int = 2000
): Float {
    var currentScale by remember { mutableStateOf(minScale) }
    var isGrowing by remember { mutableStateOf(true) }
    
    LaunchedEffect(Unit) {
        while (true) {
            val targetScale = if (isGrowing) maxScale else minScale
            val current = currentScale
            
            animate(
                initialValue = current,
                targetValue = targetScale,
                animationSpec = tween(
                    durationMillis = duration / 2,
                    easing = ColorOSEasing.Standard
                )
            ) { value, _ ->
                currentScale = value
            }
            
            isGrowing = !isGrowing
        }
    }
    
    return currentScale
}

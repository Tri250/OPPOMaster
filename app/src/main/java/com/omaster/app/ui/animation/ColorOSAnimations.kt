package com.omaster.app.ui.animation

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
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
    
    // 超长篇动画
    const val EXTRA_LONG = 700
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
    
    // 平滑缓动 - 玻璃态效果
    val Smooth = CubicBezierEasing(0.4f, 0.0f, 0.4f, 1.0f)
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
    
    // 入场时的缩放
    const val Enter = 0.9f
    
    // 强调时的放大
    const val Pop = 1.08f
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
    
    // 顶层悬浮
    val Top = 12.dp
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
 * ==================== ColorOS 16 按压反馈 Modifier (带动画效果) ====================
 */
fun Modifier.clickableWithColorOSAnimation(
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    onPressedChanged: ((Boolean) -> Unit)? = null
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
    
    val alpha by animateFloatAsState(
        targetValue = if (isPressed) 0.7f else 1f,
        animationSpec = spring(
            dampingRatio = 0.85f,
            stiffness = 400f
        ),
        label = "colorosAlpha"
    )
    
    LaunchedEffect(isPressed) {
        onPressedChanged?.invoke(isPressed)
    }
    
    this
        .graphicsLayer {
            this.scaleX = scale
            this.scaleY = scale
            this.alpha = alpha
        }
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
            
            animate(
                initialValue = currentScale,
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

/**
 * ==================== ColorOS 16 脉冲动画 ====================
 */
@Composable
fun rememberPulseAnimation(
    duration: Int = 1500
): Float {
    var progress by remember { mutableStateOf(0f) }
    
    LaunchedEffect(Unit) {
        while (true) {
            animate(
                initialValue = progress,
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = duration,
                    easing = ColorOSEasing.Standard
                )
            ) { value, _ ->
                progress = value
            }
            progress = 0f
        }
    }
    
    return progress
}

/**
 * ==================== ColorOS 16 弹跳动画 ====================
 */
@Composable
fun rememberBounceAnimation(
    initialScale: Float = 0.95f,
    bounceCount: Int = 2,
    duration: Int = 400
): Float {
    var scale by remember { mutableStateOf(initialScale) }
    var bouncePhase by remember { mutableStateOf(0) }
    
    LaunchedEffect(Unit) {
        animate(
            initialValue = initialScale,
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = 0.6f,
                stiffness = 400f
            )
        ) { value, _ ->
            scale = value
        }
    }
    
    return scale
}

/**
 * ==================== ColorOS 16 晃动动画 ====================
 */
@Composable
fun rememberShakeAnimation(
    shakeOffset: Float = 8f,
    shakeCount: Int = 3
): Float {
    var offset by remember { mutableStateOf(0f) }
    var isShaking by remember { mutableStateOf(false) }
    
    LaunchedEffect(isShaking) {
        if (isShaking) {
            for (i in 0 until shakeCount * 2) {
                offset = if (i % 2 == 0) shakeOffset else -shakeOffset
                kotlinx.coroutines.delay(50)
            }
            offset = 0f
            isShaking = false
        }
    }
    
    return offset
}

/**
 * ==================== ColorOS 16 淡入淡出动画 Modifier ====================
 */
fun Modifier.fadeInOut(
    visible: Boolean,
    duration: Int = ColorOSAnimationDuration.MEDIUM
): Modifier = composed {
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(
            durationMillis = duration,
            easing = if (visible) ColorOSEasing.Decelerate else ColorOSEasing.Accelerate
        ),
        label = "fadeAnimation"
    )
    
    this.alpha(alpha)
}

/**
 * ==================== ColorOS 16 缩放进入退出 Modifier ====================
 */
fun Modifier.scaleInOut(
    visible: Boolean,
    duration: Int = ColorOSAnimationDuration.MEDIUM
): Modifier = composed {
    val scale by animateFloatAsState(
        targetValue = if (visible) 1f else ColorOSScale.Enter,
        animationSpec = tween(
            durationMillis = duration,
            easing = if (visible) ColorOSEasing.Decelerate else ColorOSEasing.Accelerate
        ),
        label = "scaleAnimation"
    )
    
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(
            durationMillis = duration,
            easing = if (visible) ColorOSEasing.Decelerate else ColorOSEasing.Accelerate
        ),
        label = "alphaAnimation"
    )
    
    this
        .graphicsLayer {
            this.scaleX = scale
            this.scaleY = scale
            this.alpha = alpha
        }
}

/**
 * ==================== ColorOS 16 强调动画 Modifier ====================
 */
fun Modifier.emphasize(
    emphasize: Boolean,
    duration: Int = ColorOSAnimationDuration.FAST
): Modifier = composed {
    val scale by animateFloatAsState(
        targetValue = if (emphasize) ColorOSScale.Pop else 1f,
        animationSpec = spring(
            dampingRatio = 0.75f,
            stiffness = 350f
        ),
        label = "emphasizeAnimation"
    )
    
    this.scale(scale)
}

/**
 * ==================== ColorOS 16 进入动画效果集 ====================
 */
object ColorOSEnterTransition {
    val FadeIn: EnterTransition = fadeIn(
        animationSpec = tween(
            durationMillis = ColorOSAnimationDuration.MEDIUM,
            easing = ColorOSEasing.Decelerate
        )
    )
    
    val ScaleIn: EnterTransition = scaleIn(
        initialScale = ColorOSScale.Enter,
        animationSpec = tween(
            durationMillis = ColorOSAnimationDuration.MEDIUM,
            easing = ColorOSEasing.Decelerate
        )
    ) + fadeIn(
        animationSpec = tween(
            durationMillis = ColorOSAnimationDuration.MEDIUM,
            easing = ColorOSEasing.Decelerate
        )
    )
    
    val SlideUp: EnterTransition = slideInVertically(
        initialOffsetY = { it / 2 },
        animationSpec = tween(
            durationMillis = ColorOSAnimationDuration.MEDIUM,
            easing = ColorOSEasing.Decelerate
        )
    ) + fadeIn(
        animationSpec = tween(
            durationMillis = ColorOSAnimationDuration.MEDIUM,
            easing = ColorOSEasing.Decelerate
        )
    )
    
    val SpringIn: EnterTransition = scaleIn(
        initialScale = 0.85f,
        animationSpec = spring(
            dampingRatio = 0.7f,
            stiffness = 250f
        )
    ) + fadeIn(
        animationSpec = tween(
            durationMillis = ColorOSAnimationDuration.FAST,
            easing = ColorOSEasing.Decelerate
        )
    )
}

/**
 * ==================== ColorOS 16 退出动画效果集 ====================
 */
object ColorOSExitTransition {
    val FadeOut: ExitTransition = fadeOut(
        animationSpec = tween(
            durationMillis = ColorOSAnimationDuration.MEDIUM,
            easing = ColorOSEasing.Accelerate
        )
    )
    
    val ScaleOut: ExitTransition = scaleOut(
        targetScale = ColorOSScale.Enter,
        animationSpec = tween(
            durationMillis = ColorOSAnimationDuration.MEDIUM,
            easing = ColorOSEasing.Accelerate
        )
    ) + fadeOut(
        animationSpec = tween(
            durationMillis = ColorOSAnimationDuration.MEDIUM,
            easing = ColorOSEasing.Accelerate
        )
    )
    
    val SlideDown: ExitTransition = slideOutVertically(
        targetOffsetY = { it / 2 },
        animationSpec = tween(
            durationMillis = ColorOSAnimationDuration.MEDIUM,
            easing = ColorOSEasing.Accelerate
        )
    ) + fadeOut(
        animationSpec = tween(
            durationMillis = ColorOSAnimationDuration.MEDIUM,
            easing = ColorOSEasing.Accelerate
        )
    )
}


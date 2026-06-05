package com.omaster.app.ui.animation

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Indication
import androidx.compose.foundation.IndicationInstance
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.math.abs

// ==================== OPPO 2026 ColorOS 16 动画系统 ====================
// 基于Aquatic Design流畅自然、顺滑细腻的设计理念

// ========== 动画时长规范 - OPPO 2026 ==========
object OppoAnimationDuration {
    // 快速动画 - 微交互
    const val Micro = 80  // 触觉反馈
    const val Fast = 150  // 即时响应
    const val Medium = 250 // 标准动画
    const val Slow = 350    // 流畅过渡
    const val Long = 500  // 复杂动画
}

// ========== 缓动曲线规范 - OPPO 2026 ==========
object OppoEasing {
    // 标准 - 自然的缓入缓出
    val Standard = CubicBezierEasing(0.4f, 0.0f, 0.2f, 1.0f)
    
    // 加速 - 内容弹出
    val Accelerate = CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f)
    
    // 减速 - 内容进入
    val Decelerate = CubicBezierEasing(0.0f, 0.0f, 0.2f, 1.0f)
    
    // 弹性 - 年轻活力
    val Bounce = CubicBezierEasing(0.34f, 1.56f, 0.64f, 1.0f)
    
    // 回弹 - 释放反馈
    val Spring = SpringSpec<Float>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessLow
    )
}

// ========== 缩放动画规范 - OPPO 2026 ==========
object OppoScale {
    // 点击缩放
    const val Pressed = 0.95f
    // 悬停缩放
    const val Hovered = 0.98f
    // 放大提示
    const val Emphasize = 1.02f
}

// ========== 阴影层级 - OPPO 2026 ==========
object OppoElevation {
    val None = 0.dp
    val Flat = 1.dp
    val Card = 2.dp
    val Elevated = 4.dp
    val Focus = 6.dp
    val Modal = 8.dp
}

// ========== ColorOS 16 流体按压涟漪效果 ==========
@Stable
class OppoRipple(
    private val color: Color
) : Indication {
    @Composable
    override fun rememberUpdatedInstance(interactionSource: InteractionSource): IndicationInstance {
        val isPressed by interactionSource.collectIsPressedAsState()
        
        val animatedAlpha by animateFloatAsState(
            targetValue = if (isPressed) 0.2f else 0f,
            animationSpec = tween(
                durationMillis = OppoAnimationDuration.Fast,
                easing = OppoEasing.Standard
            )
        )
        
        return remember(interactionSource) {
            object : IndicationInstance {
                override fun ContentDrawScope.drawIndication() {
                    drawContent()
                    if (animatedAlpha > 0f) {
                        drawCircle(
                            color = color,
                            radius = size.maxDimension * 0.5f,
                            alpha = animatedAlpha
                        )
                    }
                }
            }
        }
    }
}

// ========== ColorOS 16 按下效果 Modifier ==========
fun Modifier.oppoClickable(
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    onClick: () -> Unit
) = composed {
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed) OppoScale.Pressed else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "oppo_scale"
    )
    
    Modifier
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .then(
            if (enabled) {
                Modifier.clickable(
                    interactionSource = interactionSource,
                    indication = OppoRipple(
                        color = Color.White.copy(alpha = 0.15f)
                    ),
                    onClick = onClick
                )
            } else {
                Modifier
            }
        )
}

// ========== ColorOS 16 卡片按压动画 Modifier ==========
fun Modifier.oppoCardAnimation(
    enabled: Boolean = true,
    scaleOnPress: Float = OppoScale.Pressed,
    scaleOnHover: Float = OppoScale.Hovered
) = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val animatedScale by animateFloatAsState(
        targetValue = if (isPressed) scaleOnPress else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "card_scale"
    )
    
    Modifier
        .graphicsLayer {
            scaleX = animatedScale
            scaleY = animatedScale
            shadowElevation = if (isPressed) OppoElevation.Card.value else OppoElevation.Focus.value
        }
}

// ========== ColorOS 16 淡入动画 ==========
@Composable
fun Modifier.animateFadeIn(
    delayMillis: Int = 0
): Modifier {
    val visible = remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(delayMillis.toLong())
        visible.value = true
    }
    
    val alpha by animateFloatAsState(
        targetValue = if (visible.value) 1f else 0f,
        animationSpec = tween(
            durationMillis = OppoAnimationDuration.Medium,
            easing = OppoEasing.Decelerate
        ),
        label = "fade_in"
    )
    
    return this.graphicsLayer { this.alpha = alpha }
}

// ========== ColorOS 16 滑动动画 ==========
@Composable
fun Modifier.animateSlideIn(
    direction: SlideDirection = SlideDirection.FromBottom,
    delayMillis: Int = 0
): Modifier {
    val visible = remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(delayMillis.toLong())
        visible.value = true
    }
    
    val offset by animateFloatAsState(
        targetValue = if (visible.value) 0f else direction.offset,
        animationSpec = tween(
            durationMillis = OppoAnimationDuration.Medium,
            easing = OppoEasing.Decelerate
        ),
        label = "slide_in"
    )
    
    return this.graphicsLayer {
        when (direction) {
            SlideDirection.FromTop -> translationY = -offset
            SlideDirection.FromBottom -> translationY = offset
            SlideDirection.FromStart -> translationX = -offset
            SlideDirection.FromEnd -> translationX = offset
        }
    }
}

enum class SlideDirection(val offset: Float) {
    FromTop(200f),
    FromBottom(200f),
    FromStart(100f),
    FromEnd(100f)
}

// ========== ColorOS 16 缩放进入动画 ==========
@Composable
fun Modifier.animateScaleIn(
    initialScale: Float = 0.85f,
    delayMillis: Int = 0
): Modifier {
    val visible = remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(delayMillis.toLong())
        visible.value = true
    }
    
    val scale by animateFloatAsState(
        targetValue = if (visible.value) 1f else initialScale,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "scale_in"
    )
    
    val alpha by animateFloatAsState(
        targetValue = if (visible.value) 1f else 0f,
        animationSpec = tween(
            durationMillis = OppoAnimationDuration.Fast,
            easing = OppoEasing.Standard
        ),
        label = "scale_alpha"
    )
    
    return this.graphicsLayer {
        this.alpha = alpha
        scaleX = scale
        scaleY = scale
    }
}

// ========== ColorOS 16 组合进入动画 ==========
@Composable
fun Modifier.animateEnter(
    delayMillis: Int = 0
): Modifier {
    return this
        .animateFadeIn(delayMillis)
        .animateSlideIn(SlideDirection.FromBottom, delayMillis)
}

// ========== ColorOS 16 列表项动画 ==========
@Composable
fun Modifier.animateListItem(
    index: Int,
    baseDelay: Int = 50
): Modifier {
    val delay = baseDelay * (index % 8)  // 控制延迟
    return this.animateEnter(delay)
}

// ========== ColorOS 16 卡片悬停/高亮动画 ==========
fun Modifier.oppoHover(
    interactionSource: MutableInteractionSource
) = composed {
    val isHovered by interactionSource.collectIsPressedAsState()
    
    val scale by animateFloatAsState(
        targetValue = if (isHovered) OppoScale.Hovered else 1f,
        animationSpec = tween(
            durationMillis = OppoAnimationDuration.Micro,
            easing = OppoEasing.Standard
        ),
        label = "hover_scale"
    )
    
    Modifier
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
}

// ========== ColorOS 16 震动/呼吸动画 ==========
@Composable
fun rememberBreathingAnimation(
    minAlpha: Float = 0.9f,
    maxAlpha: Float = 1.0f,
    cycleDuration: Int = 2000
): State<Float> {
    val transition = rememberInfiniteTransition(label = "breathing")
    val alpha by transition.animateFloat(
        initialValue = minAlpha,
        targetValue = maxAlpha,
        animationSpec = infiniteRepeatable(
            animation = tween(
            durationMillis = cycleDuration,
            easing = OppoEasing.Standard
        ),
        repeatMode = RepeatMode.Reverse
    ),
        label = "breathing_alpha"
    )
    return rememberUpdatedState(alpha)
}

// ========== ColorOS 16 脉冲动画 ==========
@Composable
fun rememberPulseAnimation(
    minScale: Float = 1.0f,
    maxScale: Float = 1.05f,
    cycleDuration: Int = 1500
): State<Float> {
    val transition = rememberInfiniteTransition(label = "pulse")
    val scale by transition.animateFloat(
        initialValue = minScale,
        targetValue = maxScale,
        animationSpec = infiniteRepeatable(
            animation = tween(
            durationMillis = cycleDuration,
            easing = OppoEasing.Standard
        ),
        repeatMode = RepeatMode.Reverse
    ),
        label = "pulse_scale"
    )
    return rememberUpdatedState(scale)
}

// ========== ColorOS 16 过渡动画包 ==========
fun EnterTransition.oppoFadeIn(): EnterTransition {
    return fadeIn(
        animationSpec = tween(
            durationMillis = OppoAnimationDuration.Medium,
            easing = OppoEasing.Decelerate
        )
    )
}

fun ExitTransition.oppoFadeOut(): ExitTransition {
    return fadeOut(
        animationSpec = tween(
            durationMillis = OppoAnimationDuration.Fast,
            easing = OppoEasing.Accelerate
        )
    )
}

fun EnterTransition.oppoSlideInVertically(): EnterTransition {
    return slideInVertically(
        initialOffsetY = { it / 3 },
        animationSpec = tween(
            durationMillis = OppoAnimationDuration.Medium,
            easing = OppoEasing.Decelerate
        )
    ) + oppoFadeIn()
}

fun ExitTransition.oppoSlideOutVertically(): ExitTransition {
    return slideOutVertically(
        targetOffsetY = { it / 3 },
        animationSpec = tween(
            durationMillis = OppoAnimationDuration.Fast,
            easing = OppoEasing.Accelerate
        )
    ) + oppoFadeOut()
}

fun EnterTransition.oppoScaleIn(): EnterTransition {
    return scaleIn(
        initialScale = 0.92f,
        animationSpec = tween(
            durationMillis = OppoAnimationDuration.Medium,
            easing = OppoEasing.Decelerate
        )
    ) + oppoFadeIn()
}

fun ExitTransition.oppoScaleOut(): ExitTransition {
    return scaleOut(
        targetScale = 0.92f,
        animationSpec = tween(
            durationMillis = OppoAnimationDuration.Fast,
            easing = OppoEasing.Accelerate
        )
    ) + oppoFadeOut()
}

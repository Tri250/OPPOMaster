package com.omaster.app.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 淡入动画包装器
 * 为内容添加淡入动画效果
 *
 * @param visible 是否可见
 * @param durationMillis 动画持续时间
 * @param delayMillis 动画延迟时间
 * @param content 动画内容
 */
@Composable
fun FadeInAnimation(
    visible: Boolean,
    modifier: Modifier = Modifier,
    durationMillis: Int = 300,
    delayMillis: Int = 0,
    content: @Composable () -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = fadeIn(
            animationSpec = tween(
                durationMillis = durationMillis,
                delayMillis = delayMillis,
                easing = FastOutSlowInEasing
            )
        ),
        exit = fadeOut(
            animationSpec = tween(
                durationMillis = durationMillis / 2,
                easing = FastOutSlowInEasing
            )
        )
    ) {
        content()
    }
}

/**
 * 滑入动画包装器
 * 支持水平和垂直方向的滑入效果
 *
 * @param visible 是否可见
 * @param direction 滑动方向
 * @param durationMillis 动画持续时间
 * @param delayMillis 动画延迟时间
 * @param content 动画内容
 */
@Composable
fun SlideInAnimation(
    visible: Boolean,
    modifier: Modifier = Modifier,
    direction: SlideDirection = SlideDirection.FROM_BOTTOM,
    durationMillis: Int = 400,
    delayMillis: Int = 0,
    content: @Composable () -> Unit
) {
    val enterTransition = when (direction) {
        SlideDirection.FROM_LEFT -> slideInHorizontally(
            initialOffsetX = { -it },
            animationSpec = tween(
                durationMillis = durationMillis,
                delayMillis = delayMillis,
                easing = FastOutSlowInEasing
            )
        ) + fadeIn()

        SlideDirection.FROM_RIGHT -> slideInHorizontally(
            initialOffsetX = { it },
            animationSpec = tween(
                durationMillis = durationMillis,
                delayMillis = delayMillis,
                easing = FastOutSlowInEasing
            )
        ) + fadeIn()

        SlideDirection.FROM_TOP -> slideInVertically(
            initialOffsetY = { -it },
            animationSpec = tween(
                durationMillis = durationMillis,
                delayMillis = delayMillis,
                easing = FastOutSlowInEasing
            )
        ) + fadeIn()

        SlideDirection.FROM_BOTTOM -> slideInVertically(
            initialOffsetY = { it },
            animationSpec = tween(
                durationMillis = durationMillis,
                delayMillis = delayMillis,
                easing = FastOutSlowInEasing
            )
        ) + fadeIn()
    }

    val exitTransition = when (direction) {
        SlideDirection.FROM_LEFT -> slideOutHorizontally(
            targetOffsetX = { -it },
            animationSpec = tween(durationMillis = durationMillis / 2)
        ) + fadeOut()

        SlideDirection.FROM_RIGHT -> slideOutHorizontally(
            targetOffsetX = { it },
            animationSpec = tween(durationMillis = durationMillis / 2)
        ) + fadeOut()

        SlideDirection.FROM_TOP -> slideOutVertically(
            targetOffsetY = { -it },
            animationSpec = tween(durationMillis = durationMillis / 2)
        ) + fadeOut()

        SlideDirection.FROM_BOTTOM -> slideOutVertically(
            targetOffsetY = { it },
            animationSpec = tween(durationMillis = durationMillis / 2)
        ) + fadeOut()
    }

    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = enterTransition,
        exit = exitTransition
    ) {
        content()
    }
}

/**
 * 滑动方向枚举
 */
enum class SlideDirection {
    FROM_LEFT,
    FROM_RIGHT,
    FROM_TOP,
    FROM_BOTTOM
}

/**
 * 缩放动画包装器
 * 为内容添加缩放进入/退出动画
 *
 * @param visible 是否可见
 * @param initialScale 初始缩放比例
 * @param durationMillis 动画持续时间
 * @param delayMillis 动画延迟时间
 * @param content 动画内容
 */
@Composable
fun ScaleAnimation(
    visible: Boolean,
    modifier: Modifier = Modifier,
    initialScale: Float = 0.8f,
    durationMillis: Int = 300,
    delayMillis: Int = 0,
    content: @Composable () -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = scaleIn(
            initialScale = initialScale,
            animationSpec = tween(
                durationMillis = durationMillis,
                delayMillis = delayMillis,
                easing = FastOutSlowInEasing
            )
        ) + fadeIn(),
        exit = scaleOut(
            targetScale = initialScale,
            animationSpec = tween(durationMillis = durationMillis / 2)
        ) + fadeOut()
    ) {
        content()
    }
}

/**
 * 组合动画包装器
 * 同时包含淡入、滑动和缩放效果
 *
 * @param visible 是否可见
 * @param direction 滑动方向
 * @param durationMillis 动画持续时间
 * @param content 动画内容
 */
@Composable
fun CombinedAnimation(
    visible: Boolean,
    modifier: Modifier = Modifier,
    direction: SlideDirection = SlideDirection.FROM_BOTTOM,
    durationMillis: Int = 400,
    content: @Composable () -> Unit
) {
    val enterTransition = when (direction) {
        SlideDirection.FROM_LEFT -> slideInHorizontally(
            initialOffsetX = { -it / 2 }
        ) + fadeIn() + scaleIn(initialScale = 0.9f)

        SlideDirection.FROM_RIGHT -> slideInHorizontally(
            initialOffsetX = { it / 2 }
        ) + fadeIn() + scaleIn(initialScale = 0.9f)

        SlideDirection.FROM_TOP -> slideInVertically(
            initialOffsetY = { -it / 2 }
        ) + fadeIn() + scaleIn(initialScale = 0.9f)

        SlideDirection.FROM_BOTTOM -> slideInVertically(
            initialOffsetY = { it / 2 }
        ) + fadeIn() + scaleIn(initialScale = 0.9f)
    }

    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = enterTransition,
        exit = fadeOut() + scaleOut(targetScale = 0.9f)
    ) {
        content()
    }
}

/**
 * 骨架屏闪光效果
 * 用于加载状态的占位符动画
 *
 * @param modifier 修饰符
 * @param shape 形状
 * @param shimmerColors 闪光颜色渐变
 * @param content 内容区域
 */
@Composable
fun ShimmerEffect(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(8.dp),
    shimmerColors: List<Color>? = null,
    content: @Composable () -> Unit = {}
) {
    val transition = rememberInfiniteTransition(label = "shimmer")

    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 1200,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_translate"
    )

    val defaultColors = listOf(
        MaterialTheme.colorScheme.surface.copy(alpha = 0.3f),
        MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
        MaterialTheme.colorScheme.surface.copy(alpha = 0.3f)
    )

    val colors = shimmerColors ?: defaultColors

    Box(
        modifier = modifier
            .clip(shape)
            .background(
                brush = Brush.linearGradient(
                    colors = colors,
                    start = Offset(translateAnim - 200f, 0f),
                    end = Offset(translateAnim, 0f)
                )
            )
    ) {
        content()
    }
}

/**
 * 骨架屏占位符组件
 * 用于列表或卡片加载状态
 *
 * @param modifier 修饰符
 * @param height 高度
 * @param shape 形状
 */
@Composable
fun ShimmerPlaceholder(
    modifier: Modifier = Modifier,
    height: Dp = 100.dp,
    shape: Shape = RoundedCornerShape(8.dp)
) {
    ShimmerEffect(
        modifier = modifier
            .fillMaxWidth()
            .height(height),
        shape = shape
    )
}

/**
 * 骨架屏列表占位符
 * 用于列表加载状态
 *
 * @param itemCount 占位符数量
 * @param itemHeight 每个占位符高度
 * @param spacing 间距
 */
@Composable
fun ShimmerListPlaceholder(
    itemCount: Int = 5,
    itemHeight: Dp = 80.dp,
    spacing: Dp = 8.dp
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        repeat(itemCount) { index ->
            ShimmerPlaceholder(
                height = itemHeight,
                modifier = Modifier.padding(
                    top = if (index == 0) 0.dp else spacing
                )
            )
        }
    }
}

/**
 * 脉冲动画修饰符
 * 为组件添加脉冲缩放效果
 *
 * @param minScale 最小缩放比例
 * @param maxScale 最大缩放比例
 * @param durationMillis 动画周期
 */
fun Modifier.pulseAnimation(
    minScale: Float = 1f,
    maxScale: Float = 1.05f,
    durationMillis: Int = 1000
): Modifier = composed {
    val transition = rememberInfiniteTransition(label = "pulse")

    val scale by transition.animateFloat(
        initialValue = minScale,
        targetValue = maxScale,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = durationMillis,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

/**
 * 呼吸动画修饰符
 * 为组件添加透明度呼吸效果
 *
 * @param minAlpha 最小透明度
 * @param maxAlpha 最大透明度
 * @param durationMillis 动画周期
 */
fun Modifier.breathingAnimation(
    minAlpha: Float = 0.4f,
    maxAlpha: Float = 1f,
    durationMillis: Int = 1500
): Modifier = composed {
    val transition = rememberInfiniteTransition(label = "breathing")

    val alpha by transition.animateFloat(
        initialValue = minAlpha,
        targetValue = maxAlpha,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = durationMillis,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathing_alpha"
    )

    graphicsLayer {
        this.alpha = alpha
    }
}

/**
 * 旋转动画修饰符
 * 为组件添加持续旋转效果
 *
 * @param durationMillis 旋转一周的时间
 * @param clockwise 是否顺时针旋转
 */
fun Modifier.rotationAnimation(
    durationMillis: Int = 2000,
    clockwise: Boolean = true
): Modifier = composed {
    val transition = rememberInfiniteTransition(label = "rotation")

    val rotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = if (clockwise) 360f else -360f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = durationMillis,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation_angle"
    )

    graphicsLayer {
        rotationZ = rotation
    }
}

/**
 * 交错动画列表
 * 为列表项添加交错进入动画
 *
 * @param items 列表数据
 * @param staggerDelay 每项动画延迟
 * @param itemContent 列表项内容
 */
@Composable
fun <T> StaggeredAnimationList(
    items: List<T>,
    modifier: Modifier = Modifier,
    staggerDelay: Int = 50,
    itemContent: @Composable (T, Int) -> Unit
) {
    Column(modifier = modifier) {
        items.forEachIndexed { index, item ->
            SlideInAnimation(
                visible = true,
                direction = SlideDirection.FROM_BOTTOM,
                delayMillis = index * staggerDelay,
                durationMillis = 300
            ) {
                itemContent(item, index)
            }
        }
    }
}

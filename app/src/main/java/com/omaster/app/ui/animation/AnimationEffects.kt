package com.omaster.app.ui.animation

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun FadeInUpAnimation(
    targetState: Boolean,
    delay: Int = 0,
    duration: Int = AnimationConfig.STATE_TRANSITION_DURATION,
    offsetY: Dp = 20.dp,
    content: @Composable () -> Unit
) {
    AnimatedVisibility(
        visible = targetState,
        enter = fadeIn(
            animationSpec = tween(
                durationMillis = duration,
                delayMillis = delay,
                easing = AnimationConfig.LinearOutSlowInEasing
            )
        ) + slideInVertically(
            initialOffsetY = { offsetY.toPx().toInt() },
            animationSpec = tween(
                durationMillis = duration,
                delayMillis = delay,
                easing = AnimationConfig.LinearOutSlowInEasing
            )
        ),
        exit = fadeOut(
            animationSpec = tween(
                durationMillis = duration / 2,
                easing = AnimationConfig.FastOutLinearInEasing
            )
        ) + slideOutVertically(
            targetOffsetY = { offsetY.toPx().toInt() },
            animationSpec = tween(
                durationMillis = duration / 2,
                easing = AnimationConfig.FastOutLinearInEasing
            )
        )
    ) {
        content()
    }
}

@Composable
fun ScaleInAnimation(
    targetState: Boolean,
    delay: Int = 0,
    duration: Int = AnimationConfig.MICRO_INTERACTION_DURATION,
    initialScale: Float = 0.9f,
    content: @Composable () -> Unit
) {
    AnimatedVisibility(
        visible = targetState,
        enter = scaleIn(
            initialScale = initialScale,
            animationSpec = tween(
                durationMillis = duration,
                delayMillis = delay,
                easing = AnimationConfig.FastOutSlowInEasing
            )
        ),
        exit = scaleOut(
            targetScale = initialScale,
            animationSpec = tween(
                durationMillis = duration / 2,
                easing = AnimationConfig.FastOutLinearInEasing
            )
        )
    ) {
        content()
    }
}

@Composable
fun BounceScaleAnimation(
    targetState: Boolean,
    delay: Int = 0,
    content: @Composable () -> Unit
) {
    AnimatedVisibility(
        visible = targetState,
        enter = scaleIn(
            animationSpec = spring(
                dampingRatio = 0.6f,
                stiffness = 500f
            )
        ),
        exit = scaleOut(
            animationSpec = spring(
                dampingRatio = 0.6f,
                stiffness = 500f
            )
        )
    ) {
        content()
    }
}

@Composable
fun SlideInFromRightAnimation(
    targetState: Boolean,
    delay: Int = 0,
    duration: Int = AnimationConfig.PAGE_TRANSITION_DURATION,
    content: @Composable () -> Unit
) {
    AnimatedVisibility(
        visible = targetState,
        enter = slideInHorizontally(
            initialOffsetX = { it },
            animationSpec = tween(
                durationMillis = duration,
                delayMillis = delay,
                easing = AnimationConfig.FastOutSlowInEasing
            )
        ) + fadeIn(
            animationSpec = tween(
                durationMillis = duration,
                delayMillis = delay
            )
        ),
        exit = slideOutHorizontally(
            targetOffsetX = { it },
            animationSpec = tween(
                durationMillis = duration,
                easing = AnimationConfig.FastOutLinearInEasing
            )
        ) + fadeOut(
            animationSpec = tween(
                durationMillis = duration
            )
        )
    ) {
        content()
    }
}

@Composable
fun SkeletonShimmer(
    modifier: Modifier = Modifier
) {
    val shimmerGradient = Brush.linearGradient(
        colors = listOf(
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        start = Offset(0f, 0f),
        end = Offset(Float.POSITIVE_INFINITY, 0f)
    )
    
    val infiniteTransition = rememberInfiniteTransition(label = "skeleton")
    val offsetX by infiniteTransition.animateFloat(
        initialValue = -1000f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = AnimationConfig.SKELETON_SWEEP_DURATION,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "skeletonOffset"
    )
    
    Box(
        modifier = modifier
            .drawBehind {
                drawRect(shimmerGradient, topLeft = Offset(offsetX, 0f))
            }
    )
}

@Composable
fun BreathingAnimation(
    targetState: Boolean,
    duration: Int = AnimationConfig.NEW_TAG_BREATHING_DURATION,
    content: @Composable () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "breathing")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = duration,
                easing = CubicBezierEasing(0.5f, 0.0f, 0.5f, 1.0f)
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathingAlpha"
    )
    
    AnimatedVisibility(visible = targetState) {
        Box(modifier = Modifier.alpha(alpha)) {
            content()
        }
    }
}

@Composable
fun FloatingAnimation(
    targetState: Boolean,
    duration: Int = 2000,
    offsetY: Dp = 8.dp,
    content: @Composable () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "floating")
    val yOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = offsetY.toPx(),
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = duration,
                easing = CubicBezierEasing(0.5f, 0.0f, 0.5f, 1.0f)
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "floatingOffset"
    )
    
    AnimatedVisibility(visible = targetState) {
        Box(modifier = Modifier.offset(y = yOffset)) {
            content()
        }
    }
}

@Composable
fun ShakeAnimation(
    trigger: Boolean,
    duration: Int = 500,
    content: @Composable () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "shake")
    val shakeOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 0f,
        animationSpec = if (trigger) {
            keyframes {
                durationMillis = duration
                -5f at 0
                5f at 50
                -5f at 100
                5f at 150
                -5f at 200
                5f at 250
                -5f at 300
                5f at 350
                0f at 400
            }
        } else {
            tween(durationMillis = 0)
        },
        label = "shakeOffset"
    )
    
    Box(modifier = Modifier.offset(x = shakeOffset.dp)) {
        content()
    }
}

@Composable
fun ProgressRingAnimation(
    progress: Float,
    duration: Int = 300,
    content: @Composable () -> Unit
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(
            durationMillis = duration,
            easing = AnimationConfig.FastOutSlowInEasing
        ),
        label = "progress"
    )
    
    Box {
        content()
    }
}

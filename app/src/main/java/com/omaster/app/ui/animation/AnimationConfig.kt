package com.omaster.app.ui.animation

import androidx.compose.animation.core.*
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

object AnimationConfig {
    
    const val PAGE_TRANSITION_DURATION = 300
    const val STATE_TRANSITION_DURATION = 200
    
    const val MICRO_INTERACTION_DURATION = 100
    const val SMALL_TRANSITION_DURATION = 200
    const val MEDIUM_TRANSITION_DURATION = 300
    const val LARGE_TRANSITION_DURATION = 400
    
    const val FLOATING_WINDOW_DURATION = 300
    const val SKELETON_SWEEP_DURATION = 1200
    const val NEW_TAG_BREATHING_DURATION = 1500
    const val SNACKBAR_DURATION = 200
    const val HEARTBEAT_DURATION = 600
    const val FLIP_DURATION = 300
    
    val StandardEasing = CubicBezierEasing(0.4f, 0.0f, 0.2f, 1.0f)
    val EmphasisEasing = CubicBezierEasing(0.0f, 0.0f, 0.2f, 1.0f)
    val DecelerateEasing = CubicBezierEasing(0.0f, 0.0f, 0.2f, 1.0f)
    val AccelerateEasing = CubicBezierEasing(0.4f, 0.0f, 1.0f, 1.0f)
    val MicroInteractionEasing = CubicBezierEasing(0.4f, 0.0f, 0.2f, 1.0f)
    
    val ColorOSSpringSpec = SpringSpec(
        dampingRatio = 0.7f,
        stiffness = 300f,
        visibilityThreshold = Dp(0.5f).value
    )
    
    val BouncySpringSpec = SpringSpec(
        dampingRatio = 0.6f,
        stiffness = 400f,
        visibilityThreshold = Dp(0.5f).value
    )
    
    val GentleSpringSpec = SpringSpec(
        dampingRatio = 0.85f,
        stiffness = 200f,
        visibilityThreshold = Dp(0.5f).value
    )
    
    val SmoothDecay = DecayAnimationSpec(
        frictionMultiplier = 0.99f,
        absVelocityThreshold = 0.1f
    )
    
    val FastOutSlowInEasing = StandardEasing
    val LinearOutSlowInEasing = DecelerateEasing
    val FastOutLinearInEasing = AccelerateEasing
    
    fun microInteractionSpec() = tween<Float>(
        durationMillis = MICRO_INTERACTION_DURATION,
        easing = MicroInteractionEasing
    )
    
    fun smallTransitionSpec() = tween<Float>(
        durationMillis = SMALL_TRANSITION_DURATION,
        easing = StandardEasing
    )
    
    fun mediumTransitionSpec() = tween<Float>(
        durationMillis = MEDIUM_TRANSITION_DURATION,
        easing = DecelerateEasing
    )
    
    fun pageTransitionSpec() = tween<Float>(
        durationMillis = PAGE_TRANSITION_DURATION,
        easing = DecelerateEasing
    )
    
    fun springSpec(dampingRatio: Float = 0.7f, stiffness: Float = 300f) = SpringSpec(
        dampingRatio = dampingRatio,
        stiffness = stiffness,
        visibilityThreshold = Dp(0.5f).value
    )
}

val Float.Companion.DefaultEasing: CubicBezierEasing
    get() = CubicBezierEasing(0.4f, 0.0f, 0.2f, 1.0f)

@Composable
fun rememberColorOSSpringSpec(
    dampingRatio: Float = 0.7f,
    stiffness: Float = 300f
): SpringSpec<Float> {
    return remember(dampingRatio, stiffness) {
        SpringSpec(
            dampingRatio = dampingRatio,
            stiffness = stiffness,
            visibilityThreshold = 0.5f
        )
    }
}

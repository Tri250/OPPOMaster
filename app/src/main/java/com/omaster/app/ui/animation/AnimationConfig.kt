package com.omaster.app.ui.animation

import androidx.compose.animation.core.*
import androidx.compose.ui.unit.Dp

object AnimationConfig {
    
    const val PAGE_TRANSITION_DURATION = 300
    const val MICRO_INTERACTION_DURATION = 150
    const val STATE_TRANSITION_DURATION = 200
    const val FLOATING_WINDOW_DURATION = 250
    const val SKELETON_SWEEP_DURATION = 1200
    const val NEW_TAG_BREATHING_DURATION = 1500
    const val SNACKBAR_DURATION = 200
    
    val FastOutSlowInEasing = CubicBezierEasing(0.4f, 0.0f, 0.2f, 1.0f)
    val LinearOutSlowInEasing = CubicBezierEasing(0.0f, 0.0f, 0.2f, 1.0f)
    val FastOutLinearInEasing = CubicBezierEasing(0.4f, 0.0f, 1.0f, 1.0f)
    val MicroInteractionEasing = CubicBezierEasing(0.4f, 0.0f, 0.2f, 1.0f)
    
    val SpringSpec = SpringSpec(
        dampingRatio = 0.85f,
        stiffness = 300f,
        visibilityThreshold = Dp(0.5f).value
    )
    
    val SmoothDecay = DecayAnimationSpec(
        frictionMultiplier = 0.99f,
        absVelocityThreshold = 0.1f
    )
}

val Float.Companion.DefaultEasing: CubicBezierEasing
    get() = CubicBezierEasing(0.4f, 0.0f, 0.2f, 1.0f)

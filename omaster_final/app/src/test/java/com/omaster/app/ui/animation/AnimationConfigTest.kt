package com.omaster.app.ui.animation

import androidx.compose.animation.core.*
import androidx.compose.ui.unit.Dp
import org.junit.Assert.*
import org.junit.Test

class AnimationConfigTest {
    
    @Test
    fun testDurationConstants() {
        assertEquals(300, AnimationConfig.PAGE_TRANSITION_DURATION)
        assertEquals(150, AnimationConfig.MICRO_INTERACTION_DURATION)
        assertEquals(200, AnimationConfig.STATE_TRANSITION_DURATION)
        assertEquals(250, AnimationConfig.FLOATING_WINDOW_DURATION)
        assertEquals(1200, AnimationConfig.SKELETON_SWEEP_DURATION)
        assertEquals(1500, AnimationConfig.NEW_TAG_BREATHING_DURATION)
        assertEquals(200, AnimationConfig.SNACKBAR_DURATION)
    }
    
    @Test
    fun testEasingCurves() {
        val fastOutSlowIn = AnimationConfig.FastOutSlowInEasing
        val linearOutSlowIn = AnimationConfig.LinearOutSlowInEasing
        val fastOutLinearIn = AnimationConfig.FastOutLinearInEasing
        val microInteraction = AnimationConfig.MicroInteractionEasing
        
        assertNotNull(fastOutSlowIn)
        assertNotNull(linearOutSlowIn)
        assertNotNull(fastOutLinearIn)
        assertNotNull(microInteraction)
        
        assertEquals(0.4f, fastOutSlowIn.m, 0.01f)
        assertEquals(0.0f, fastOutSlowIn.n, 0.01f)
        assertEquals(0.2f, fastOutSlowIn.p, 0.01f)
        assertEquals(1.0f, fastOutSlowIn.q, 0.01f)
    }
    
    @Test
    fun testSpringSpec() {
        val springSpec = AnimationConfig.SpringSpec
        
        assertEquals(0.85f, springSpec.dampingRatio, 0.01f)
        assertEquals(300f, springSpec.stiffness, 0.01f)
        assertEquals(Dp(0.5f).value, springSpec.visibilityThreshold, 0.01f)
    }
    
    @Test
    fun testEasingValues() {
        val easing = AnimationConfig.FastOutSlowInEasing
        
        val startValue = easing.transform(0f)
        val midValue = easing.transform(0.5f)
        val endValue = easing.transform(1f)
        
        assertEquals(0f, startValue, 0.01f)
        assertTrue(midValue > 0.3f && midValue < 0.7f)
        assertEquals(1f, endValue, 0.01f)
    }
    
    @Test
    fun testDurationConstraints() {
        assertTrue(AnimationConfig.PAGE_TRANSITION_DURATION in 250..400)
        assertTrue(AnimationConfig.MICRO_INTERACTION_DURATION in 100..200)
        assertTrue(AnimationConfig.STATE_TRANSITION_DURATION in 150..250)
        assertTrue(AnimationConfig.FLOATING_WINDOW_DURATION in 200..300)
    }
}

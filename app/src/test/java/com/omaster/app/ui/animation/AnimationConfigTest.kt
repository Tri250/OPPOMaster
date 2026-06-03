package com.omaster.app.ui.animation

import org.junit.Assert.*
import org.junit.Test

/**
 * AnimationConfig 单元测试
 * 覆盖：动画时长常量和缩放参数
 *
 * 注意：缓动曲线和弹簧常量涉及 Compose API，不在此测试中验证
 */
class AnimationConfigTest {

    // ==================== 页面转场动画时长 ====================

    @Test
    fun `PAGE_TRANSITION_DURATION should be 350ms`() {
        assertEquals(350, AnimationConfig.PAGE_TRANSITION_DURATION)
    }

    @Test
    fun `PAGE_TRANSITION_DURATION_SHORT should be 280ms`() {
        assertEquals(280, AnimationConfig.PAGE_TRANSITION_DURATION_SHORT)
    }

    @Test
    fun `PAGE_TRANSITION_DURATION should be greater than SHORT`() {
        assertTrue(
            "Page transition should be longer than short",
            AnimationConfig.PAGE_TRANSITION_DURATION > AnimationConfig.PAGE_TRANSITION_DURATION_SHORT
        )
    }

    // ==================== 微交互动画时长 ====================

    @Test
    fun `MICRO_INTERACTION_DURATION should be 120ms`() {
        assertEquals(120, AnimationConfig.MICRO_INTERACTION_DURATION)
    }

    @Test
    fun `MICRO_INTERACTION_DURATION_LONG should be 180ms`() {
        assertEquals(180, AnimationConfig.MICRO_INTERACTION_DURATION_LONG)
    }

    @Test
    fun `MICRO_INTERACTION_DURATION_LONG should be longer than MICRO_INTERACTION_DURATION`() {
        assertTrue(
            AnimationConfig.MICRO_INTERACTION_DURATION_LONG > AnimationConfig.MICRO_INTERACTION_DURATION
        )
    }

    // ==================== 状态切换动画时长 ====================

    @Test
    fun `STATE_TRANSITION_DURATION should be 220ms`() {
        assertEquals(220, AnimationConfig.STATE_TRANSITION_DURATION)
    }

    @Test
    fun `STATE_TRANSITION_DURATION_FAST should be 160ms`() {
        assertEquals(160, AnimationConfig.STATE_TRANSITION_DURATION_FAST)
    }

    @Test
    fun `STATE_TRANSITION_DURATION should be longer than FAST`() {
        assertTrue(
            AnimationConfig.STATE_TRANSITION_DURATION > AnimationConfig.STATE_TRANSITION_DURATION_FAST
        )
    }

    // ==================== 悬浮窗动画时长 ====================

    @Test
    fun `FLOATING_WINDOW_DURATION should be 280ms`() {
        assertEquals(280, AnimationConfig.FLOATING_WINDOW_DURATION)
    }

    // ==================== 骨架屏动画时长 ====================

    @Test
    fun `SKELETON_SWEEP_DURATION should be 1500ms`() {
        assertEquals(1500, AnimationConfig.SKELETON_SWEEP_DURATION)
    }

    @Test
    fun `SKELETON_FADE_DURATION should be 300ms`() {
        assertEquals(300, AnimationConfig.SKELETON_FADE_DURATION)
    }

    // ==================== 新标签呼吸动画时长 ====================

    @Test
    fun `NEW_TAG_BREATHING_DURATION should be 1800ms`() {
        assertEquals(1800, AnimationConfig.NEW_TAG_BREATHING_DURATION)
    }

    // ==================== 提示信息动画时长 ====================

    @Test
    fun `SNACKBAR_DURATION should be 250ms`() {
        assertEquals(250, AnimationConfig.SNACKBAR_DURATION)
    }

    @Test
    fun `TOAST_DURATION should be 200ms`() {
        assertEquals(200, AnimationConfig.TOAST_DURATION)
    }

    // ==================== 卡片缩放参数 ====================

    @Test
    fun `CARD_PRESS_SCALE should be 0_96`() {
        assertEquals(0.96f, AnimationConfig.CARD_PRESS_SCALE, 0.001f)
    }

    @Test
    fun `CARD_PRESS_ALPHA should be 0_92`() {
        assertEquals(0.92f, AnimationConfig.CARD_PRESS_ALPHA, 0.001f)
    }

    @Test
    fun `CARD_PRESS_SCALE should be less than 1`() {
        assertTrue("Card press scale should be < 1 (compression effect)", AnimationConfig.CARD_PRESS_SCALE < 1f)
    }

    @Test
    fun `CARD_PRESS_ALPHA should be less than 1`() {
        assertTrue("Card press alpha should be < 1 (fade effect)", AnimationConfig.CARD_PRESS_ALPHA < 1f)
    }

    @Test
    fun `CARD_PRESS_SCALE should be greater than 0`() {
        assertTrue("Card press scale should be > 0", AnimationConfig.CARD_PRESS_SCALE > 0f)
    }

    // ==================== 动画时长合理性 ====================

    @Test
    fun `all durations should be positive`() {
        assertTrue(AnimationConfig.PAGE_TRANSITION_DURATION > 0)
        assertTrue(AnimationConfig.PAGE_TRANSITION_DURATION_SHORT > 0)
        assertTrue(AnimationConfig.MICRO_INTERACTION_DURATION > 0)
        assertTrue(AnimationConfig.MICRO_INTERACTION_DURATION_LONG > 0)
        assertTrue(AnimationConfig.STATE_TRANSITION_DURATION > 0)
        assertTrue(AnimationConfig.STATE_TRANSITION_DURATION_FAST > 0)
        assertTrue(AnimationConfig.FLOATING_WINDOW_DURATION > 0)
        assertTrue(AnimationConfig.SKELETON_SWEEP_DURATION > 0)
        assertTrue(AnimationConfig.SKELETON_FADE_DURATION > 0)
        assertTrue(AnimationConfig.NEW_TAG_BREATHING_DURATION > 0)
        assertTrue(AnimationConfig.SNACKBAR_DURATION > 0)
        assertTrue(AnimationConfig.TOAST_DURATION > 0)
    }

    @Test
    fun `all durations should be reasonable upper bound`() {
        // 动画时长应该合理(小于5秒)
        val allDurations = listOf(
            AnimationConfig.PAGE_TRANSITION_DURATION,
            AnimationConfig.PAGE_TRANSITION_DURATION_SHORT,
            AnimationConfig.MICRO_INTERACTION_DURATION,
            AnimationConfig.MICRO_INTERACTION_DURATION_LONG,
            AnimationConfig.STATE_TRANSITION_DURATION,
            AnimationConfig.STATE_TRANSITION_DURATION_FAST,
            AnimationConfig.FLOATING_WINDOW_DURATION,
            AnimationConfig.SKELETON_FADE_DURATION,
            AnimationConfig.SNACKBAR_DURATION,
            AnimationConfig.TOAST_DURATION
        )
        allDurations.forEach { duration ->
            assertTrue("Duration $duration should be < 5000ms", duration < 5000)
        }
    }
}

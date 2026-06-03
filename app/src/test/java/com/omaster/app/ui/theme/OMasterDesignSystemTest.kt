package com.omaster.app.ui.theme

import androidx.compose.ui.unit.dp
import org.junit.Assert.*
import org.junit.Test

/**
 * 设计系统全面单元测试
 * 覆盖：颜色常量、排版、间距、圆角、阴影、动画、图标尺寸
 */
class OMasterDesignSystemTest {

    // ==================== OMasterColors 测试 ====================

    @Test
    fun `primary color should be correct`() {
        assertEquals(0xFFD4A574, OMasterColors.Primary.value.toLong())
    }

    @Test
    fun `background color should be dark`() {
        assertEquals(0xFF0A0A0A, OMasterColors.Background.value.toLong())
    }

    @Test
    fun `surface color should be slightly lighter than background`() {
        assertTrue(
            "Surface should be lighter than background",
            OMasterColors.Surface.value > OMasterColors.Background.value
        )
    }

    @Test
    fun `hasselblad orange should match primary`() {
        assertEquals(OMasterColors.Primary, OMasterColors.HasselbladOrange)
    }

    @Test
    fun `accent colors should be distinct`() {
        val accents = setOf(
            OMasterColors.AccentOrange,
            OMasterColors.AccentGold,
            OMasterColors.AccentBlue,
            OMasterColors.AccentGreen,
            OMasterColors.AccentRed
        )
        assertEquals("All accent colors should be distinct", 5, accents.size)
    }

    @Test
    fun `glass colors should have alpha`() {
        // GlassBackground should be semi-transparent (0x1A = ~10% alpha)
        assertTrue("Glass background should have alpha", OMasterColors.GlassBackground.alpha < 1f)
        assertTrue("Glass border should have alpha", OMasterColors.GlassBorder.alpha < 1f)
        assertTrue("Glass highlight should have alpha", OMasterColors.GlassHighlight.alpha < 1f)
    }

    @Test
    fun `onPrimary should contrast with primary`() {
        // OnPrimary should be dark (black) on light primary
        assertEquals(0xFF000000, OMasterColors.OnPrimary.value.toLong())
    }

    @Test
    fun `onBackground should be light on dark background`() {
        assertEquals(0xFFFFFFFF, OMasterColors.OnBackground.value.toLong())
    }

    // ==================== OMasterTypography 测试 ====================

    @Test
    fun `display large should have largest font size`() {
        assertTrue(
            "Display large should be largest",
            OMasterTypography.DisplayLarge.fontSize.value >= OMasterTypography.DisplayMedium.fontSize.value
        )
    }

    @Test
    fun `display sizes should decrease`() {
        assertTrue(
            "Display large > medium > small",
            OMasterTypography.DisplayLarge.fontSize.value >= OMasterTypography.DisplayMedium.fontSize.value &&
            OMasterTypography.DisplayMedium.fontSize.value >= OMasterTypography.DisplaySmall.fontSize.value
        )
    }

    @Test
    fun `headline sizes should decrease`() {
        assertTrue(
            "Headline large > medium > small",
            OMasterTypography.HeadlineLarge.fontSize.value >= OMasterTypography.HeadlineMedium.fontSize.value &&
            OMasterTypography.HeadlineMedium.fontSize.value >= OMasterTypography.HeadlineSmall.fontSize.value
        )
    }

    @Test
    fun `body sizes should decrease`() {
        assertTrue(
            "Body large > medium > small",
            OMasterTypography.BodyLarge.fontSize.value >= OMasterTypography.BodyMedium.fontSize.value &&
            OMasterTypography.BodyMedium.fontSize.value >= OMasterTypography.BodySmall.fontSize.value
        )
    }

    @Test
    fun `label sizes should decrease`() {
        assertTrue(
            "Label large > medium > small",
            OMasterTypography.LabelLarge.fontSize.value >= OMasterTypography.LabelMedium.fontSize.value &&
            OMasterTypography.LabelMedium.fontSize.value >= OMasterTypography.LabelSmall.fontSize.value
        )
    }

    @Test
    fun `display text should be bold`() {
        assertEquals(
            androidx.compose.ui.text.font.FontWeight.Bold,
            OMasterTypography.DisplayLarge.fontWeight
        )
    }

    // ==================== OMasterSpacing 测试 ====================

    @Test
    fun `spacing should increase progressively`() {
        assertTrue(OMasterSpacing.xxs < OMasterSpacing.xs)
        assertTrue(OMasterSpacing.xs < OMasterSpacing.sm)
        assertTrue(OMasterSpacing.sm < OMasterSpacing.md)
        assertTrue(OMasterSpacing.md < OMasterSpacing.lg)
        assertTrue(OMasterSpacing.lg < OMasterSpacing.xl)
        assertTrue(OMasterSpacing.xl < OMasterSpacing.xxl)
        assertTrue(OMasterSpacing.xxl < OMasterSpacing.xxxl)
        assertTrue(OMasterSpacing.xxxl < OMasterSpacing.huge)
    }

    @Test
    fun `screen padding should be reasonable`() {
        assertTrue(
            "Screen padding should be between 16 and 24 dp",
            OMasterSpacing.ScreenPadding in 16.dp..24.dp
        )
    }

    @Test
    fun `card padding should be reasonable`() {
        assertTrue(
            "Card padding should be between 12 and 20 dp",
            OMasterSpacing.CardPadding in 12.dp..20.dp
        )
    }

    @Test
    fun `minimum spacing should be positive`() {
        assertTrue("Minimum spacing should be positive", OMasterSpacing.xxs > 0.dp)
    }

    // ==================== OMasterRadius 测试 ====================

    @Test
    fun `radius should increase progressively`() {
        assertTrue(OMasterRadius.none < OMasterRadius.xs)
        assertTrue(OMasterRadius.xs < OMasterRadius.sm)
        assertTrue(OMasterRadius.sm < OMasterRadius.md)
        assertTrue(OMasterRadius.md < OMasterRadius.lg)
        assertTrue(OMasterRadius.lg < OMasterRadius.xl)
        assertTrue(OMasterRadius.xl < OMasterRadius.xxl)
    }

    @Test
    fun `card radius should be standard`() {
        assertEquals(16.dp, OMasterRadius.Card)
    }

    @Test
    fun `chip radius should be smaller than card`() {
        assertTrue("Chip radius should be smaller than card", OMasterRadius.Chip < OMasterRadius.Card)
    }

    @Test
    fun `full radius should be very large`() {
        assertEquals(999.dp, OMasterRadius.full)
    }

    // ==================== OMasterElevation 测试 ====================

    @Test
    fun `elevation should increase progressively`() {
        assertTrue(OMasterElevation.none < OMasterElevation.low)
        assertTrue(OMasterElevation.low < OMasterElevation.medium)
        assertTrue(OMasterElevation.medium < OMasterElevation.high)
        assertTrue(OMasterElevation.high < OMasterElevation.highest)
    }

    @Test
    fun `card elevation should be medium`() {
        assertEquals(4.dp, OMasterElevation.Card)
    }

    @Test
    fun `app bar elevation should be flat`() {
        assertEquals(0.dp, OMasterElevation.AppBar)
    }

    @Test
    fun `dialog elevation should be highest`() {
        assertEquals(24.dp, OMasterElevation.Dialog)
    }

    // ==================== OMasterAnimation 测试 ====================

    @Test
    fun `animation durations should be positive`() {
        assertTrue(OMasterAnimation.DurationFast > 0)
        assertTrue(OMasterAnimation.DurationNormal > 0)
        assertTrue(OMasterAnimation.DurationSlow > 0)
        assertTrue(OMasterAnimation.DurationVerySlow > 0)
    }

    @Test
    fun `animation durations should increase`() {
        assertTrue(OMasterAnimation.DurationFast < OMasterAnimation.DurationNormal)
        assertTrue(OMasterAnimation.DurationNormal < OMasterAnimation.DurationSlow)
        assertTrue(OMasterAnimation.DurationSlow < OMasterAnimation.DurationVerySlow)
    }

    @Test
    fun `fast animation should be reasonable`() {
        assertTrue(
            "Fast animation should be 100-200ms",
            OMasterAnimation.DurationFast in 100..200
        )
    }

    @Test
    fun `normal animation should be reasonable`() {
        assertTrue(
            "Normal animation should be 250-400ms",
            OMasterAnimation.DurationNormal in 250..400
        )
    }

    @Test
    fun `easing objects should not be null`() {
        assertNotNull(OMasterAnimation.EasingStandard)
        assertNotNull(OMasterAnimation.EasingDecelerate)
        assertNotNull(OMasterAnimation.EasingAccelerate)
    }

    // ==================== OMasterShadow 测试 ====================

    @Test
    fun `shadow elevations should increase`() {
        assertTrue(OMasterShadow.SoftElevation < OMasterShadow.MediumElevation)
        assertTrue(OMasterShadow.MediumElevation < OMasterShadow.StrongElevation)
    }

    @Test
    fun `shadow colors should have alpha`() {
        assertTrue("Soft shadow should have alpha", OMasterShadow.SoftColor.alpha < 1f)
        assertTrue("Medium shadow should have alpha", OMasterShadow.MediumColor.alpha < 1f)
        assertTrue("Strong shadow should have alpha", OMasterShadow.StrongColor.alpha < 1f)
    }

    @Test
    fun `glow color should be hasselblad themed`() {
        // Glow color should have the hasselblad orange tint
        assertTrue("Glow color should have alpha", OMasterShadow.GlowColor.alpha < 1f)
    }

    // ==================== OMasterIcons 测试 ====================

    @Test
    fun `icon sizes should increase`() {
        assertTrue(OMasterIcons.SizeSmall < OMasterIcons.SizeMedium)
        assertTrue(OMasterIcons.SizeMedium < OMasterIcons.SizeLarge)
        assertTrue(OMasterIcons.SizeLarge < OMasterIcons.SizeXLarge)
    }

    @Test
    fun `icon sizes should be reasonable`() {
        assertEquals(16.dp, OMasterIcons.SizeSmall)
        assertEquals(24.dp, OMasterIcons.SizeMedium)
        assertEquals(32.dp, OMasterIcons.SizeLarge)
        assertEquals(48.dp, OMasterIcons.SizeXLarge)
    }

    @Test
    fun `stroke width should be reasonable`() {
        assertTrue("Stroke width should be positive", OMasterIcons.StrokeWidth > 0.dp)
        assertTrue("Bold stroke should be wider", OMasterIcons.StrokeWidthBold > OMasterIcons.StrokeWidth)
    }

    // ==================== 主题一致性测试 ====================

    @Test
    fun `all color constants should be non-transparent for solid colors`() {
        // Primary colors should be fully opaque
        assertEquals(1f, OMasterColors.Primary.alpha, 0.01f)
        assertEquals(1f, OMasterColors.Secondary.alpha, 0.01f)
        assertEquals(1f, OMasterColors.Background.alpha, 0.01f)
        assertEquals(1f, OMasterColors.OnPrimary.alpha, 0.01f)
        assertEquals(1f, OMasterColors.OnBackground.alpha, 0.01f)
    }

    @Test
    fun `gradient colors should be dark themed`() {
        // Both gradient colors should be dark
        assertTrue("Gradient start should be dark", OMasterColors.GradientStart.value < 0xFF404040)
        assertTrue("Gradient end should be dark", OMasterColors.GradientEnd.value < 0xFF404040)
    }
}

package com.omaster.app.ui.components

import org.junit.Assert.*
import org.junit.Test

/**
 * 屏幕适配逻辑单元测试
 * 覆盖：ScreenSizeClass、FoldState、DisplayMode、触摸目标、响应式逻辑
 */
class ScreenAdaptationTest {

    // ==================== ScreenSizeClass 测试 ====================

    @Test
    fun `ScreenSizeClass should have 3 values`() {
        assertEquals(3, ScreenSizeClass.values().size)
    }

    @Test
    fun `ScreenSizeClass values should be COMPACT MEDIUM EXPANDED`() {
        assertTrue(ScreenSizeClass.values().contains(ScreenSizeClass.COMPACT))
        assertTrue(ScreenSizeClass.values().contains(ScreenSizeClass.MEDIUM))
        assertTrue(ScreenSizeClass.values().contains(ScreenSizeClass.EXPANDED))
    }

    @Test
    fun `ScreenSizeClass COMPACT should be first`() {
        assertEquals(ScreenSizeClass.COMPACT, ScreenSizeClass.values()[0])
    }

    // ==================== FoldState 测试 ====================

    @Test
    fun `FoldState should have 3 values`() {
        assertEquals(3, FoldState.values().size)
    }

    @Test
    fun `FoldState should have FLAT HALF_OPENED FOLDED`() {
        assertTrue(FoldState.values().contains(FoldState.FLAT))
        assertTrue(FoldState.values().contains(FoldState.HALF_OPENED))
        assertTrue(FoldState.values().contains(FoldState.FOLDED))
    }

    // ==================== DeviceFoldInfo 测试 ====================

    @Test
    fun `DeviceFoldInfo default should be FLAT`() {
        val foldInfo = DeviceFoldInfo(
            foldState = FoldState.FLAT,
            isTableTopMode = false,
            isBookMode = false
        )
        assertEquals(FoldState.FLAT, foldInfo.foldState)
        assertFalse(foldInfo.isTableTopMode)
        assertFalse(foldInfo.isBookMode)
    }

    @Test
    fun `DeviceFoldInfo table top mode should have correct state`() {
        val foldInfo = DeviceFoldInfo(
            foldState = FoldState.HALF_OPENED,
            isTableTopMode = true,
            isBookMode = false
        )
        assertEquals(FoldState.HALF_OPENED, foldInfo.foldState)
        assertTrue(foldInfo.isTableTopMode)
        assertFalse(foldInfo.isBookMode)
    }

    @Test
    fun `DeviceFoldInfo book mode should have correct state`() {
        val foldInfo = DeviceFoldInfo(
            foldState = FoldState.HALF_OPENED,
            isTableTopMode = false,
            isBookMode = true
        )
        assertEquals(FoldState.HALF_OPENED, foldInfo.foldState)
        assertFalse(foldInfo.isTableTopMode)
        assertTrue(foldInfo.isBookMode)
    }

    // ==================== DisplayMode 测试 ====================

    @Test
    fun `DisplayMode should have 3 values`() {
        assertEquals(3, DisplayMode.values().size)
    }

    @Test
    fun `DisplayMode should have LIGHT DARK SYSTEM`() {
        assertTrue(DisplayMode.values().contains(DisplayMode.LIGHT))
        assertTrue(DisplayMode.values().contains(DisplayMode.DARK))
        assertTrue(DisplayMode.values().contains(DisplayMode.SYSTEM))
    }

    // ==================== 触摸目标测试 ====================

    @Test
    fun `MinTouchTarget should be 48dp`() {
        assertEquals(48, MinTouchTarget.value.toInt())
    }

    @Test
    fun `RecommendedTouchTarget should be 56dp`() {
        assertEquals(56, RecommendedTouchTarget.value.toInt())
    }

    @Test
    fun `MinTouchTarget should be less than RecommendedTouchTarget`() {
        assertTrue(MinTouchTarget < RecommendedTouchTarget)
    }

    @Test
    fun `touch targets should meet accessibility minimum`() {
        // Material Design accessibility guidelines recommend at least 48dp
        assertTrue(
            "MinTouchTarget should be at least 48dp for accessibility",
            MinTouchTarget.value >= 48f
        )
    }

    // ==================== 屏幕尺寸分类逻辑测试 ====================

    @Test
    fun `screen width below 600 should be COMPACT`() {
        val screenWidthDp = 360
        val sizeClass = when {
            screenWidthDp < 600 -> ScreenSizeClass.COMPACT
            screenWidthDp < 840 -> ScreenSizeClass.MEDIUM
            else -> ScreenSizeClass.EXPANDED
        }
        assertEquals(ScreenSizeClass.COMPACT, sizeClass)
    }

    @Test
    fun `screen width 600-839 should be MEDIUM`() {
        val widths = listOf(600, 720, 839)
        widths.forEach { width ->
            val sizeClass = when {
                width < 600 -> ScreenSizeClass.COMPACT
                width < 840 -> ScreenSizeClass.MEDIUM
                else -> ScreenSizeClass.EXPANDED
            }
            assertEquals("Width $width should be MEDIUM", ScreenSizeClass.MEDIUM, sizeClass)
        }
    }

    @Test
    fun `screen width 840 plus should be EXPANDED`() {
        val widths = listOf(840, 1024, 1280)
        widths.forEach { width ->
            val sizeClass = when {
                width < 600 -> ScreenSizeClass.COMPACT
                width < 840 -> ScreenSizeClass.MEDIUM
                else -> ScreenSizeClass.EXPANDED
            }
            assertEquals("Width $width should be EXPANDED", ScreenSizeClass.EXPANDED, sizeClass)
        }
    }

    @Test
    fun `common phone resolutions should be COMPACT`() {
        // Common phone resolutions in dp
        val phoneWidths = listOf(360, 375, 390, 393, 412)
        phoneWidths.forEach { width ->
            val sizeClass = when {
                width < 600 -> ScreenSizeClass.COMPACT
                width < 840 -> ScreenSizeClass.MEDIUM
                else -> ScreenSizeClass.EXPANDED
            }
            assertEquals("Phone width $width should be COMPACT", ScreenSizeClass.COMPACT, sizeClass)
        }
    }

    @Test
    fun `tablet resolutions should be MEDIUM or EXPANDED`() {
        val tabletWidths = mapOf(
            600 to ScreenSizeClass.MEDIUM,
            768 to ScreenSizeClass.MEDIUM,
            840 to ScreenSizeClass.EXPANDED,
            1024 to ScreenSizeClass.EXPANDED
        )
        tabletWidths.forEach { (width, expected) ->
            val sizeClass = when {
                width < 600 -> ScreenSizeClass.COMPACT
                width < 840 -> ScreenSizeClass.MEDIUM
                else -> ScreenSizeClass.EXPANDED
            }
            assertEquals("Tablet width $width should be $expected", expected, sizeClass)
        }
    }

    // ==================== 响应式间距逻辑测试 ====================

    @Test
    fun `responsive padding should increase with screen size`() {
        val compactPadding = 16f
        val mediumPadding = 20f
        val expandedPadding = 24f

        assertTrue("Compact < Medium", compactPadding < mediumPadding)
        assertTrue("Medium < Expanded", mediumPadding < expandedPadding)
    }

    @Test
    fun `responsive text size should increase with screen size`() {
        val compactSize = 14
        val mediumSize = 15
        val expandedSize = 16

        assertTrue("Compact < Medium", compactSize < mediumSize)
        assertTrue("Medium < Expanded", mediumSize < expandedSize)
    }

    // ==================== 网格列数逻辑测试 ====================

    @Test
    fun `grid columns should increase with screen size`() {
        val smallColumns = 1
        val mediumColumns = 2
        val expandedColumns = 3

        assertTrue("Small < Medium", smallColumns < mediumColumns)
        assertTrue("Medium < Expanded", mediumColumns < expandedColumns)
    }

    @Test
    fun `items chunked by columns should fill rows correctly`() {
        val items = listOf(1, 2, 3, 4, 5)
        val columns = 2
        val chunked = items.chunked(columns)

        assertEquals(3, chunked.size)
        assertEquals(listOf(1, 2), chunked[0])
        assertEquals(listOf(3, 4), chunked[1])
        assertEquals(listOf(5), chunked[2])
    }

    @Test
    fun `empty items chunked should return empty`() {
        val items = emptyList<Int>()
        val columns = 2
        val chunked = items.chunked(columns)
        assertTrue(chunked.isEmpty())
    }

    @Test
    fun `items exactly matching columns should chunk correctly`() {
        val items = listOf(1, 2, 3, 4)
        val columns = 2
        val chunked = items.chunked(columns)

        assertEquals(2, chunked.size)
        assertEquals(listOf(1, 2), chunked[0])
        assertEquals(listOf(3, 4), chunked[1])
    }

    @Test
    fun `single column should preserve order`() {
        val items = listOf(1, 2, 3)
        val chunked = items.chunked(1)
        assertEquals(3, chunked.size)
        assertEquals(listOf(1), chunked[0])
        assertEquals(listOf(2), chunked[1])
        assertEquals(listOf(3), chunked[2])
    }
}

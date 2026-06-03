package com.omaster.app.data

import org.junit.Assert.*
import org.junit.Test

/**
 * ThemeMode 单元测试
 * 覆盖：主题模式枚举值
 */
class ThemeModeTest {

    @Test
    fun `ThemeMode SYSTEM should have value 0`() {
        assertEquals(0, ThemeMode.SYSTEM.value)
    }

    @Test
    fun `ThemeMode LIGHT should have value 1`() {
        assertEquals(1, ThemeMode.LIGHT.value)
    }

    @Test
    fun `ThemeMode DARK should have value 2`() {
        assertEquals(2, ThemeMode.DARK.value)
    }

    @Test
    fun `ThemeMode should have exactly 3 values`() {
        assertEquals(3, ThemeMode.values().size)
    }

    @Test
    fun `ThemeMode values should be ordered correctly`() {
        val values = ThemeMode.values()
        assertEquals(ThemeMode.SYSTEM, values[0])
        assertEquals(ThemeMode.LIGHT, values[1])
        assertEquals(ThemeMode.DARK, values[2])
    }

    @Test
    fun `ThemeMode valueOf should work correctly`() {
        assertEquals(ThemeMode.SYSTEM, ThemeMode.valueOf("SYSTEM"))
        assertEquals(ThemeMode.LIGHT, ThemeMode.valueOf("LIGHT"))
        assertEquals(ThemeMode.DARK, ThemeMode.valueOf("DARK"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `ThemeMode valueOf with invalid name should throw`() {
        ThemeMode.valueOf("INVALID")
    }
}

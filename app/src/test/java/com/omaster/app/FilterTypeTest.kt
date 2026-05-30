package com.omaster.app

import com.omaster.app.viewmodel.FilterType
import org.junit.Test
import org.junit.Assert.*

class FilterTypeTest {
    @Test
    fun `all filter types should be available`() {
        val filters = FilterType.entries.toTypedArray()
        assertEquals(7, filters.size)
        assertTrue(filters.contains(FilterType.ALL))
        assertTrue(filters.contains(FilterType.FAVORITES))
        assertTrue(filters.contains(FilterType.HNCS))
        assertTrue(filters.contains(FilterType.FIND_X))
        assertTrue(filters.contains(FilterType.RENO))
        assertTrue(filters.contains(FilterType.NEW))
        assertTrue(filters.contains(FilterType.TRENDING))
    }

    @Test
    fun `filter type values should be correct`() {
        assertEquals("ALL", FilterType.ALL.name)
        assertEquals("FAVORITES", FilterType.FAVORITES.name)
        assertEquals("HNCS", FilterType.HNCS.name)
        assertEquals("FIND_X", FilterType.FIND_X.name)
        assertEquals("RENO", FilterType.RENO.name)
        assertEquals("NEW", FilterType.NEW.name)
        assertEquals("TRENDING", FilterType.TRENDING.name)
    }

    @Test
    fun `filter type ordinal should be correct`() {
        assertEquals(0, FilterType.ALL.ordinal)
        assertEquals(1, FilterType.FAVORITES.ordinal)
        assertEquals(2, FilterType.HNCS.ordinal)
        assertEquals(3, FilterType.FIND_X.ordinal)
        assertEquals(4, FilterType.RENO.ordinal)
        assertEquals(5, FilterType.NEW.ordinal)
        assertEquals(6, FilterType.TRENDING.ordinal)
    }
}

package com.omaster.app

import com.omaster.app.viewmodel.FilterType
import org.junit.Test
import org.junit.Assert.*

class FilterTypeTest {
    @Test
    fun `all filter types should be available`() {
        val filters = FilterType.entries
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
    fun `filter type order should be correct`() {
        val filters = FilterType.entries
        assertEquals(FilterType.ALL, filters[0])
        assertEquals(FilterType.FAVORITES, filters[1])
        assertEquals(FilterType.HNCS, filters[2])
        assertEquals(FilterType.FIND_X, filters[3])
        assertEquals(FilterType.RENO, filters[4])
        assertEquals(FilterType.NEW, filters[5])
        assertEquals(FilterType.TRENDING, filters[6])
    }
}
package com.omaster.app.floating

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FloatingWindowManagerTest {
    
    private lateinit var floatingWindowManager: FloatingWindowManager
    private lateinit var permissionHelper: PermissionHelper
    private lateinit var context: Context
    
    @Before
    fun setup() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        permissionHelper = PermissionHelper(context)
        floatingWindowManager = FloatingWindowManager(context, permissionHelper)
    }
    
    @Test
    fun testInitialState() {
        assertFalse(
            "Window should not be showing initially",
            floatingWindowManager.isWindowShowing.value
        )
        
        assertTrue(
            "Window should be expanded initially",
            floatingWindowManager.isExpanded.value
        )
        
        assertEquals(
            "Opacity should be 1.0 initially",
            1f,
            floatingWindowManager.windowOpacity.value,
            0.01f
        )
        
        assertEquals(
            "Current preset index should be 0 initially",
            0,
            floatingWindowManager.currentPresetIndex.value
        )
    }
    
    @Test
    fun testSetOpacity() {
        floatingWindowManager.setOpacity(0.5f)
        assertEquals(
            "Opacity should be set to 0.5",
            0.5f,
            floatingWindowManager.windowOpacity.value,
            0.01f
        )
        
        floatingWindowManager.setOpacity(0.2f)
        assertEquals(
            "Opacity should be clamped to 0.3 minimum",
            0.3f,
            floatingWindowManager.windowOpacity.value,
            0.01f
        )
        
        floatingWindowManager.setOpacity(1.5f)
        assertEquals(
            "Opacity should be clamped to 1.0 maximum",
            1f,
            floatingWindowManager.windowOpacity.value,
            0.01f
        )
    }
    
    @Test
    fun testToggleExpand() {
        val initialState = floatingWindowManager.isExpanded.value
        
        floatingWindowManager.toggleExpand()
        assertEquals(
            "Expanded state should be toggled",
            !initialState,
            floatingWindowManager.isExpanded.value
        )
        
        floatingWindowManager.toggleExpand()
        assertEquals(
            "Expanded state should be toggled back",
            initialState,
            floatingWindowManager.isExpanded.value
        )
    }
    
    @Test
    fun testSelectNextPreset() {
        floatingWindowManager.selectNextPreset(10)
        assertEquals(
            "Preset index should be incremented",
            1,
            floatingWindowManager.currentPresetIndex.value
        )
        
        floatingWindowManager.selectNextPreset(10)
        assertEquals(
            "Preset index should continue incrementing",
            2,
            floatingWindowManager.currentPresetIndex.value
        )
    }
    
    @Test
    fun testSelectNextPresetWrapping() {
        floatingWindowManager.selectPreset(9, 10)
        floatingWindowManager.selectNextPreset(10)
        assertEquals(
            "Preset index should wrap to 0",
            0,
            floatingWindowManager.currentPresetIndex.value
        )
    }
    
    @Test
    fun testSelectPreviousPreset() {
        floatingWindowManager.selectPreset(5, 10)
        floatingWindowManager.selectPreviousPreset(10)
        assertEquals(
            "Preset index should be decremented",
            4,
            floatingWindowManager.currentPresetIndex.value
        )
    }
    
    @Test
    fun testSelectPreviousPresetWrapping() {
        floatingWindowManager.selectPreset(0, 10)
        floatingWindowManager.selectPreviousPreset(10)
        assertEquals(
            "Preset index should wrap to 9",
            9,
            floatingWindowManager.currentPresetIndex.value
        )
    }
    
    @Test
    fun testSelectPresetInvalidIndex() {
        floatingWindowManager.selectPreset(15, 10)
        assertEquals(
            "Invalid index should not change preset",
            0,
            floatingWindowManager.currentPresetIndex.value
        )
        
        floatingWindowManager.selectPreset(-1, 10)
        assertEquals(
            "Negative index should not change preset",
            0,
            floatingWindowManager.currentPresetIndex.value
        )
    }
    
    @Test
    fun testSelectPresetZeroTotal() {
        floatingWindowManager.selectPreset(0, 0)
        assertEquals(
            "Zero total presets should not change preset",
            0,
            floatingWindowManager.currentPresetIndex.value
        )
    }
    
    @Test
    fun testUpdatePosition() {
        floatingWindowManager.updatePosition(100, 200)
    }
    
    @Test
    fun testDestroy() {
        floatingWindowManager.showWindow()
        floatingWindowManager.destroy()
        assertFalse(
            "Window should be hidden after destroy",
            floatingWindowManager.isWindowShowing.value
        )
    }
}

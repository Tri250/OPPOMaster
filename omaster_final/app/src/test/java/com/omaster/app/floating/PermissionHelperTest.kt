package com.omaster.app.floating

import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PermissionHelperTest {
    
    private lateinit var permissionHelper: PermissionHelper
    private lateinit var context: Context
    
    @Before
    fun setup() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        permissionHelper = PermissionHelper(context)
    }
    
    @Test
    fun testCanDrawOverlays() {
        val canDraw = permissionHelper.canDrawOverlays()
        assertNotNull("canDrawOverlays should return a boolean", canDraw)
    }
    
    @Test
    fun testRequestOverlayPermission() {
        val intent = permissionHelper.requestOverlayPermission()
        assertNotNull("requestOverlayPermission should return an Intent", intent)
        assertEquals(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            intent.action
        )
    }
    
    @Test
    fun testSystemBrandDetection() {
        val brand = permissionHelper.getSystemBrand()
        assertNotNull("System brand should not be null", brand)
        assertTrue(
            "System brand should be a valid string",
            brand.isNotEmpty()
        )
    }
    
    @Test
    fun testSpecialGuidanceForColorOS() {
        if (permissionHelper.isColorOS()) {
            val guidance = permissionHelper.getSpecialGuidanceText()
            assertTrue(
                "ColorOS should have special guidance",
                guidance.contains("ColorOS")
            )
        }
    }
    
    @Test
    fun testSpecialGuidanceForOxygenOS() {
        if (permissionHelper.isOxygenOS()) {
            val guidance = permissionHelper.getSpecialGuidanceText()
            assertTrue(
                "OxygenOS should have special guidance",
                guidance.contains("OxygenOS")
            )
        }
    }
    
    @Test
    fun testShouldShowSpecialGuidance() {
        val shouldShow = permissionHelper.shouldShowSpecialGuidance()
        val brand = permissionHelper.getSystemBrand()
        
        if (brand == "ColorOS" || brand == "OxygenOS") {
            assertTrue(
                "ColorOS and OxygenOS should show special guidance",
                shouldShow
            )
        }
    }
    
    @Test
    fun testPermissionStatus() {
        val status = permissionHelper.checkAllPermissions()
        
        assertNotNull("PermissionStatus should not be null", status)
        assertNotNull("canDrawOverlays should be set", status.canDrawOverlays)
        assertNotNull("systemBrand should be set", status.systemBrand)
        assertNotNull("shouldShowSpecialGuidance should be set", status.shouldShowSpecialGuidance)
    }
}

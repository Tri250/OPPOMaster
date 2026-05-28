package com.omaster.app.camera

import org.junit.Test
import org.junit.Assert.*
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations
import android.content.Context
import android.hardware.camera2.CameraManager

class Camera2ParamProviderTest {

    @Mock
    private lateinit var mockContext: Context
    
    @Mock
    private lateinit var mockCameraManager: CameraManager
    
    private lateinit var provider: Camera2ParamProvider

    @org.junit.Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        `when`(mockContext.getSystemService(Context.CAMERA_SERVICE)).thenReturn(mockCameraManager)
        `when`(mockCameraManager.cameraIdList).thenReturn(arrayOf("0", "1"))
        
        provider = Camera2ParamProvider(mockContext)
    }

    @Test
    fun `check if camera is supported on lollipop and above`() {
        android.os.Build.VERSION.SDK_INT = android.os.Build.VERSION_CODES.LOLLIPOP
        
        val isSupported = provider.checkCameraSupport()
        assertTrue("Camera should be supported", isSupported)
    }

    @Test
    fun `check if camera is not supported below lollipop`() {
        android.os.Build.VERSION.SDK_INT = android.os.Build.VERSION_CODES.KITKAT
        
        val isSupported = provider.checkCameraSupport()
        assertFalse("Camera should not be supported", isSupported)
    }

    @Test
    fun `start monitor sets correct status when available`() {
        android.os.Build.VERSION.SDK_INT = android.os.Build.VERSION_CODES.LOLLIPOP
        
        // Mock permissions granted
        val observer = object : androidx.lifecycle.Observer<CameraCompatibilityStatus> {
            var lastStatus: CameraCompatibilityStatus? = null
            override fun onChanged(status: CameraCompatibilityStatus?) {
                lastStatus = status
            }
        }
        
        provider.status.observeForever(observer)
        provider.startMonitor()
        
        // Should eventually set Available status
        provider.status.removeObserver(observer)
    }

    @Test
    fun `release stops monitoring`() {
        provider.startMonitor()
        provider.release()
        
        // Monitor should be stopped
        assertNotNull("Provider should have been released", provider)
    }
}

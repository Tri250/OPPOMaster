package com.omaster.app.floating

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.getSystemService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FloatingWindowManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val permissionHelper: PermissionHelper
) {
    private val _isWindowShowing = MutableStateFlow(false)
    val isWindowShowing: StateFlow<Boolean> = _isWindowShowing.asStateFlow()
    
    private val _isExpanded = MutableStateFlow(true)
    val isExpanded: StateFlow<Boolean> = _isExpanded.asStateFlow()
    
    private val _windowOpacity = MutableStateFlow(1f)
    val windowOpacity: StateFlow<Float> = _windowOpacity.asStateFlow()
    
    private val _currentPresetIndex = MutableStateFlow(0)
    val currentPresetIndex: StateFlow<Int> = _currentPresetIndex.asStateFlow()
    
    private var windowManager: WindowManager? = null
    private var floatingView: View? = null
    
    private var isAccessibilityMode = false

    fun isOverlayPermissionGranted(): Boolean {
        return permissionHelper.canDrawOverlays()
    }

    fun isAccessibilityPermissionGranted(): Boolean {
        return permissionHelper.isAccessibilityServiceEnabled()
    }

    private fun getWindowType(): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (isAccessibilityPermissionGranted()) {
                isAccessibilityMode = true
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
            } else {
                isAccessibilityMode = false
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            }
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }
    }
    
    fun showWindow() {
        if (!hasRequiredPermissions()) {
            Timber.w("Required permissions not granted, cannot show window")
            return
        }
        
        if (_isWindowShowing.value) {
            Timber.d("Window already showing")
            return
        }
        
        try {
            windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            
            val layoutParams = WindowManager.LayoutParams().apply {
                type = getWindowType()
                format = PixelFormat.TRANSLUCENT
                flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                
                if (isAccessibilityMode) {
                    flags = flags or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                }
                
                gravity = Gravity.TOP or Gravity.START
                x = 100
                y = 300
                width = WindowManager.LayoutParams.WRAP_CONTENT
                height = WindowManager.LayoutParams.WRAP_CONTENT
            }
            
            _isWindowShowing.value = true
            Timber.d("Floating window shown successfully in ${if (isAccessibilityMode) "accessibility" else "overlay"} mode")
        } catch (e: Exception) {
            Timber.e(e, "Failed to show floating window")
            _isWindowShowing.value = false
            
            if (!isAccessibilityMode && isAccessibilityPermissionGranted()) {
                isAccessibilityMode = true
                showWindow()
            }
        }
    }
    
    private fun hasRequiredPermissions(): Boolean {
        return isOverlayPermissionGranted() || isAccessibilityPermissionGranted()
    }
    
    fun hideWindow() {
        try {
            floatingView?.let {
                windowManager?.removeView(it)
            }
            floatingView = null
            windowManager = null
            _isWindowShowing.value = false
            Timber.d("Floating window hidden successfully")
        } catch (e: Exception) {
            Timber.e(e, "Failed to hide floating window")
        }
    }
    
    fun toggleWindow() {
        if (_isWindowShowing.value) {
            hideWindow()
        } else {
            showWindow()
        }
    }
    
    fun expand() {
        _isExpanded.value = true
        Timber.d("Floating window expanded")
    }
    
    fun collapse() {
        _isExpanded.value = false
        Timber.d("Floating window collapsed")
    }
    
    fun toggleExpand() {
        _isExpanded.value = !_isExpanded.value
        Timber.d("Floating window toggled to: ${_isExpanded.value}")
    }
    
    fun setOpacity(opacity: Float) {
        _windowOpacity.value = opacity.coerceIn(0.3f, 1f)
        Timber.d("Floating window opacity set to: ${_windowOpacity.value}")
    }
    
    fun selectNextPreset(totalPresets: Int) {
        if (totalPresets == 0) return
        _currentPresetIndex.value = (_currentPresetIndex.value + 1) % totalPresets
        Timber.d("Selected next preset: ${_currentPresetIndex.value}")
    }
    
    fun selectPreviousPreset(totalPresets: Int) {
        if (totalPresets == 0) return
        _currentPresetIndex.value = if (_currentPresetIndex.value == 0) {
            totalPresets - 1
        } else {
            _currentPresetIndex.value - 1
        }
        Timber.d("Selected previous preset: ${_currentPresetIndex.value}")
    }
    
    fun selectPreset(index: Int, totalPresets: Int) {
        if (index < 0 || index >= totalPresets) {
            Timber.w("Invalid preset index: $index, total: $totalPresets")
            return
        }
        _currentPresetIndex.value = index
        Timber.d("Selected preset at index: $index")
    }
    
    fun updatePosition(x: Int, y: Int) {
        Timber.d("Floating window position updated to: x=$x, y=$y")
    }
    
    fun destroy() {
        hideWindow()
        Timber.d("FloatingWindowManager destroyed")
    }

    fun getCurrentMode(): String {
        return if (isAccessibilityMode) "ACCESSIBILITY" else "OVERLAY"
    }
}
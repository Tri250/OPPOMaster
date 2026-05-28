package com.omaster.app.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.os.Build
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.omaster.app.model.Preset
import timber.log.Timber

class CameraAutoFillService : AccessibilityService() {

    private val cameraParamMap = mapOf(
        "OPPO" to mapOf(
            "iso" to "com.oplus.camera:id/iso_value",
            "shutter" to "com.oplus.camera:id/shutter_speed_value",
            "wb" to "com.oplus.camera:id/wb_value",
            "ev" to "com.oplus.camera:id/ev_value"
        ),
        "ONEPLUS" to mapOf(
            "iso" to "com.oneplus.camera:id/iso_value",
            "shutter" to "com.oneplus.camera:id/shutter_speed_value",
            "wb" to "com.oneplus.camera:id/wb_value",
            "ev" to "com.oneplus.camera:id/ev_value"
        ),
        "XIAOMI" to mapOf(
            "iso" to "com.miui.camera:id/iso_selector",
            "shutter" to "com.miui.camera:id/shutter_selector",
            "wb" to "com.miui.camera:id/wb_selector",
            "ev" to "com.miui.camera:id/ev_selector"
        ),
        "VIVO" to mapOf(
            "iso" to "com.vivo.camera:id/iso_value",
            "shutter" to "com.vivo.camera:id/shutter_speed_value",
            "wb" to "com.vivo.camera:id/wb_value",
            "ev" to "com.vivo.camera:id/ev_value"
        ),
        "HUAWEI" to mapOf(
            "iso" to "com.huawei.camera:id/iso_value",
            "shutter" to "com.huawei.camera:id/shutter_speed_value",
            "wb" to "com.huawei.camera:id/wb_value",
            "ev" to "com.huawei.camera:id/ev_value"
        ),
        "SAMSUNG" to mapOf(
            "iso" to "com.samsung.android.camera:id/iso_value",
            "shutter" to "com.samsung.android.camera:id/shutter_speed_value",
            "wb" to "com.samsung.android.camera:id/wb_value",
            "ev" to "com.samsung.android.camera:id/ev_value"
        )
    )

    private var currentPreset: Preset? = null
    private var onCameraDetectedCallback: (() -> Unit)? = null

    fun setPreset(preset: Preset) {
        currentPreset = preset
        Timber.d("Preset set for autofill: ${preset.name}")
    }

    fun fillPresetParams() {
        val preset = currentPreset ?: return
        val brand = getDeviceBrand()
        val paramMap = cameraParamMap[brand] ?: return
        val rootNode = rootInActiveWindow ?: return

        Timber.d("Starting autofill for brand: $brand")

        paramMap.forEach { (paramKey, viewId) ->
            val targetNode = rootNode.findAccessibilityNodeInfosByViewId(viewId).firstOrNull()
            if (targetNode != null) {
                val paramValue = getParamValue(preset, paramKey)
                if (paramValue.isNotEmpty()) {
                    performInput(targetNode, paramValue)
                    Timber.d("Filled $paramKey with value: $paramValue")
                }
            } else {
                Timber.w("View not found for $paramKey (viewId: $viewId)")
            }
        }
    }

    fun setOnCameraDetectedCallback(callback: () -> Unit) {
        this.onCameraDetectedCallback = callback
    }

    private fun getDeviceBrand(): String {
        val brand = Build.BRAND.uppercase()
        return when {
            brand.contains("OPPO") -> "OPPO"
            brand.contains("ONEPLUS") -> "ONEPLUS"
            brand.contains("REALME") -> "OPPO"
            brand.contains("XIAOMI") -> "XIAOMI"
            brand.contains("REDMI") -> "XIAOMI"
            brand.contains("VIVO") -> "VIVO"
            brand.contains("HUAWEI") -> "HUAWEI"
            brand.contains("HONOR") -> "HUAWEI"
            brand.contains("SAMSUNG") -> "SAMSUNG"
            else -> brand
        }
    }

    private fun getParamValue(preset: Preset, paramKey: String): String {
        val params = preset.cameraParams ?: return ""
        return when (paramKey) {
            "iso" -> params.iso.toString()
            "shutter" -> params.shutter
            "wb" -> params.wb
            "ev" -> params.ev
            else -> ""
        }
    }

    private fun performInput(node: AccessibilityNodeInfo, value: String) {
        node.performAction(AccessibilityNodeInfo.ACTION_FOCUS)
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val arguments = android.os.Bundle()
            arguments.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, value)
            node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
        } else {
            node.text = value
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val packageName = event.packageName?.toString() ?: return
            val isCameraApp = cameraParamMap.keys.any { brand ->
                packageName.contains(brand.lowercase())
            }
            
            if (isCameraApp) {
                Timber.d("Camera app detected: $packageName")
                onCameraDetectedCallback?.invoke()
            }
        }
    }

    override fun onInterrupt() {
        Timber.d("Accessibility service interrupted")
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        Timber.d("CameraAutoFillService connected")
    }

    override fun onUnbind(intent: Intent?): Boolean {
        Timber.d("CameraAutoFillService unbound")
        return super.onUnbind(intent)
    }

    companion object {
        private var instance: CameraAutoFillService? = null

        fun getInstance(): CameraAutoFillService? = instance
    }
}
package com.omaster.app.accessibility

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.content.Context
import android.content.Intent
import android.provider.Settings
import timber.log.Timber

class AutoFillAccessibilityService : AccessibilityService() {

    companion object {
        private var currentParams: Map<String, String>? = null

        fun setParams(params: Map<String, String>) {
            currentParams = params
        }

        fun isServiceEnabled(context: Context): Boolean {
            val pref = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            )
            return pref?.contains("${context.packageName}/.accessibility.AutoFillAccessibilityService") ?: false
        }

        fun openAccessibilitySettings(context: Context) {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            context.startActivity(intent)
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event ?: return

        val rootNode = rootInActiveWindow ?: return

        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                tryAutoFillParams(rootNode)
            }
        }
    }

    private fun tryAutoFillParams(rootNode: AccessibilityNodeInfo) {
        currentParams ?: return

        Timber.d("Trying to auto-fill params: $currentParams")

        val brandCameraMap = mapOf(
            "com.oppo.camera" to OPPOCameraHelper,
            "com.oneplus.camera" to OnePlusCameraHelper,
            "com.realme.camera" to RealmeCameraHelper,
            "com.android.camera" to GenericCameraHelper
        )

        val packageName = rootNode.packageName?.toString()
        val helper = brandCameraMap.entries.find { packageName?.contains(it.key) == true }?.value

        helper?.autoFillParams(rootNode, currentParams!!)
    }

    override fun onInterrupt() {
        Timber.d("Accessibility service interrupted")
    }
}

interface CameraAutoFillHelper {
    fun autoFillParams(rootNode: AccessibilityNodeInfo, params: Map<String, String>)
}

object OPPOCameraHelper : CameraAutoFillHelper {
    override fun autoFillParams(rootNode: AccessibilityNodeInfo, params: Map<String, String>) {
        Timber.d("OPPO camera auto-fill")
        fillParam(rootNode, "iso", "com.oppo.camera:id/iso_value", params["iso"])
        fillParam(rootNode, "shutter", "com.oppo.camera:id/shutter_value", params["shutter"])
        fillParam(rootNode, "ev", "com.oppo.camera:id/ev_value", params["ev"])
        fillParam(rootNode, "wb", "com.oppo.camera:id/wb_value", params["wb"])
    }

    private fun fillParam(rootNode: AccessibilityNodeInfo, paramName: String, viewId: String, value: String?) {
        value ?: return
        val nodes = rootNode.findAccessibilityNodeInfosByViewId(viewId)
        nodes.firstOrNull()?.apply {
            if (isClickable || isFocusable) {
                performAction(AccessibilityNodeInfo.ACTION_CLICK)
            }
        }
    }
}

object OnePlusCameraHelper : CameraAutoFillHelper {
    override fun autoFillParams(rootNode: AccessibilityNodeInfo, params: Map<String, String>) {
        Timber.d("OnePlus camera auto-fill")
        OPPOCameraHelper.autoFillParams(rootNode, params)
    }
}

object RealmeCameraHelper : CameraAutoFillHelper {
    override fun autoFillParams(rootNode: AccessibilityNodeInfo, params: Map<String, String>) {
        Timber.d("Realme camera auto-fill")
        OPPOCameraHelper.autoFillParams(rootNode, params)
    }
}

object GenericCameraHelper : CameraAutoFillHelper {
    override fun autoFillParams(rootNode: AccessibilityNodeInfo, params: Map<String, String>) {
        Timber.d("Generic camera auto-fill")
    }
}

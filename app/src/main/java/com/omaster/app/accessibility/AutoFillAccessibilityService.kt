package com.omaster.app.accessibility

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.graphics.Rect
import android.view.inputmethod.InputMethodManager
import com.omaster.app.notification.NotificationHelper
import timber.log.Timber
import kotlinx.coroutines.*

class AutoFillAccessibilityService : AccessibilityService() {

    companion object {
        private val ALLOWED_PACKAGES = setOf(
            "com.oppo.camera",
            "com.oneplus.camera",
            "com.realme.camera",
            "com.android.camera",
            "com.miui.camera",
            "com.vivo.camera",
            "com.iqoo.camera",
            "com.huawei.camera",
            "com.huawei.systemcamera",
            "com.hihonor.camera",
            "com.meizu.camera",
            "com.zte.camera",
            "com.nubia.camera",
            "com.lenovo.camera",
            "com.motorola.camera",
            "com.samsung.android.camera",
            "com.tcl.camera",
            "com.hisense.camera",
            "com.coolpad.camera",
            "com.smartisan.camera"
        )
        
        private var pendingParams: CameraParams? = null
        private var pendingCallback: (() -> Unit)? = null
        private var isUserConfirmed = false
        private var serviceInstance: AutoFillAccessibilityService? = null
        
        private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

        fun setPendingParams(params: CameraParams, callback: () -> Unit) {
            pendingParams = params
            pendingCallback = callback
            isUserConfirmed = false
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
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }

        fun hasPendingConfirmation(): Boolean {
            return pendingParams != null && !isUserConfirmed
        }

        fun confirmAutoFill() {
            isUserConfirmed = true
            pendingCallback?.invoke()
            pendingParams = null
            pendingCallback = null
        }

        fun cancelAutoFill() {
            pendingParams = null
            pendingCallback = null
            isUserConfirmed = false
        }

        fun getServiceInstance(): AutoFillAccessibilityService? = serviceInstance

        private fun clearPendingData() {
            pendingParams = null
            pendingCallback = null
            isUserConfirmed = false
        }
    }

    private var deviceHelper: DeviceCameraHelper? = null
    private val paramMapper = ParamMapper()
    private var isAutoFilling = false

    override fun onServiceConnected() {
        super.onServiceConnected()
        serviceInstance = this
        
        val info = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or 
                         AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED or
                         AccessibilityEvent.TYPE_VIEW_CLICKED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            notificationTimeout = 50
            packageNames = ALLOWED_PACKAGES.toTypedArray()
            canRetrieveWindowContent = true
            flags = AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS or
                    AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or
                    AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
        }
        
        serviceInfo = info
        
        NotificationHelper.showServiceRunningNotification(this)
        
        Timber.d("AutoFillAccessibilityService connected - Enhanced mode v3.0")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event ?: return

        val packageName = event.packageName?.toString()
        if (packageName !in ALLOWED_PACKAGES) {
            return
        }

        if (pendingParams == null || !isUserConfirmed) {
            return
        }

        if (isAutoFilling) {
            return
        }

        val rootNode = rootInActiveWindow ?: return

        try {
            val deviceType = detectDeviceType(packageName)
            deviceHelper = DeviceCameraHelperRegistry.getHelper(deviceType)

            if (deviceHelper != null) {
                isAutoFilling = true
                NotificationHelper.showAutoFillingNotification(this)
                
                scope.launch {
                    try {
                        deviceHelper!!.autoFillParams(rootNode, pendingParams!!)
                        delay(500)
                        NotificationHelper.showAutoFillCompleteNotification(this@AutoFillAccessibilityService)
                    } catch (e: Exception) {
                        Timber.e(e, "Auto-fill failed")
                        NotificationHelper.showAutoFillFailedNotification(this@AutoFillAccessibilityService, e.message)
                    } finally {
                        isAutoFilling = false
                        rootNode.recycle()
                        clearPendingData()
                        deviceHelper = null
                    }
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Error in accessibility event")
            isAutoFilling = false
        } finally {
            if (!isAutoFilling) {
                rootNode.recycle()
            }
        }
    }

    private fun detectDeviceType(packageName: String): DeviceType {
        return when {
            packageName.contains("oppo") -> DeviceType.OPPO
            packageName.contains("oneplus") -> DeviceType.ONEPLUS
            packageName.contains("realme") -> DeviceType.REALME
            packageName.contains("android.camera") || packageName.contains("miui.camera") -> DeviceType.XIAOMI
            packageName.contains("vivo") && !packageName.contains("iqoo") -> DeviceType.VIVO
            packageName.contains("iqoo") -> DeviceType.IQOO
            packageName.contains("huawei.systemcamera") -> DeviceType.HUAWEI_SYSTEM
            packageName.contains("huawei") && !packageName.contains("honor") -> DeviceType.HUAWEI
            packageName.contains("honor") || packageName.contains("hihonor") -> DeviceType.HONOR
            packageName.contains("meizu") -> DeviceType.MEIZU
            packageName.contains("zte") || packageName.contains("nubia") -> DeviceType.ZTE
            packageName.contains("lenovo") || packageName.contains("motorola") -> DeviceType.LENOVO
            packageName.contains("samsung") -> DeviceType.SAMSUNG
            packageName.contains("tcl") || packageName.contains("hisense") -> DeviceType.OTHER
            packageName.contains("coolpad") || packageName.contains("smartisan") -> DeviceType.OTHER
            else -> DeviceType.GENERIC
        }
    }

    override fun onInterrupt() {
        Timber.d("Accessibility service interrupted")
        isAutoFilling = false
        clearPendingData()
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceInstance = null
        scope.cancel()
        clearPendingData()
        NotificationHelper.hideNotification(this)
        Timber.d("AutoFillAccessibilityService destroyed")
    }
}

enum class DeviceType {
    OPPO, ONEPLUS, REALME, XIAOMI, VIVO, IQOO, HUAWEI, HUAWEI_SYSTEM, HONOR, MEIZU, ZTE, LENOVO, SAMSUNG, OTHER, GENERIC
}

data class CameraParams(
    val iso: String? = null,
    val shutter: String? = null,
    val ev: String? = null,
    val wb: String? = null,
    val contrast: Int? = null,
    val sharpness: Int? = null,
    val saturation: Int? = null,
    val vignette: Boolean? = null
)

interface DeviceCameraHelper {
    suspend fun autoFillParams(rootNode: AccessibilityNodeInfo, params: CameraParams)
}

object DeviceCameraHelperRegistry {
    private val helpers = mapOf(
        DeviceType.OPPO to OppoCameraHelper(),
        DeviceType.ONEPLUS to OnePlusCameraHelper(),
        DeviceType.REALME to RealmeCameraHelper(),
        DeviceType.XIAOMI to XiaomiCameraHelper(),
        DeviceType.VIVO to VivoCameraHelper(),
        DeviceType.HUAWEI to HuaweiCameraHelper(),
        DeviceType.SAMSUNG to SamsungCameraHelper(),
        DeviceType.GENERIC to GenericCameraHelper()
    )

    fun getHelper(deviceType: DeviceType): DeviceCameraHelper {
        return helpers[deviceType] ?: helpers[DeviceType.GENERIC]!!
    }
}

class OppoCameraHelper : DeviceCameraHelper {
    private val nodeFinder = NodeFinder("com.oppo.camera")
    
    override suspend fun autoFillParams(rootNode: AccessibilityNodeInfo, params: CameraParams) {
        Timber.d("OPPO camera auto-fill initiated")
        
        withContext(Dispatchers.Main) {
            params.iso?.let { iso ->
                nodeFinder.findAndFill(rootNode, listOf("iso_value", "iso", "ISO"), iso)
            }
            params.shutter?.let { shutter ->
                nodeFinder.findAndFill(rootNode, listOf("shutter_value", "shutter", "快门"), shutter)
            }
            params.ev?.let { ev ->
                nodeFinder.findAndFill(rootNode, listOf("ev_value", "ev", "曝光补偿"), ev)
            }
            params.wb?.let { wb ->
                nodeFinder.findAndFill(rootNode, listOf("wb_value", "wb", "白平衡"), wb)
            }
        }
    }
}

class OnePlusCameraHelper : DeviceCameraHelper {
    private val nodeFinder = NodeFinder("com.oneplus.camera")
    
    override suspend fun autoFillParams(rootNode: AccessibilityNodeInfo, params: CameraParams) {
        Timber.d("OnePlus camera auto-fill initiated")
        
        withContext(Dispatchers.Main) {
            params.iso?.let { iso ->
                nodeFinder.findAndFill(rootNode, listOf("iso_value", "iso", "ISO"), iso)
            }
            params.shutter?.let { shutter ->
                nodeFinder.findAndFill(rootNode, listOf("shutter_value", "shutter", "快门"), shutter)
            }
        }
    }
}

class RealmeCameraHelper : DeviceCameraHelper {
    private val nodeFinder = NodeFinder("com.realme.camera")
    
    override suspend fun autoFillParams(rootNode: AccessibilityNodeInfo, params: CameraParams) {
        Timber.d("Realme camera auto-fill initiated")
        
        withContext(Dispatchers.Main) {
            params.iso?.let { iso ->
                nodeFinder.findAndFill(rootNode, listOf("iso_value", "iso", "ISO"), iso)
            }
            params.shutter?.let { shutter ->
                nodeFinder.findAndFill(rootNode, listOf("shutter_value", "shutter", "快门"), shutter)
            }
        }
    }
}

class XiaomiCameraHelper : DeviceCameraHelper {
    private val nodeFinder = NodeFinder("com.xiaomi.camera")
    
    override suspend fun autoFillParams(rootNode: AccessibilityNodeInfo, params: CameraParams) {
        Timber.d("Xiaomi camera auto-fill initiated")
        
        withContext(Dispatchers.Main) {
            params.iso?.let { iso ->
                nodeFinder.findAndFill(rootNode, listOf("iso", "ISO值", "感光度"), iso)
            }
            params.shutter?.let { shutter ->
                nodeFinder.findAndFill(rootNode, listOf("shutter", "快门速度", "S"), shutter)
            }
        }
    }
}

class VivoCameraHelper : DeviceCameraHelper {
    private val nodeFinder = NodeFinder("com.vivo.camera")
    
    override suspend fun autoFillParams(rootNode: AccessibilityNodeInfo, params: CameraParams) {
        Timber.d("Vivo camera auto-fill initiated")
        
        withContext(Dispatchers.Main) {
            params.iso?.let { iso ->
                nodeFinder.findAndFill(rootNode, listOf("iso", "ISO", "感光度"), iso)
            }
            params.shutter?.let { shutter ->
                nodeFinder.findAndFill(rootNode, listOf("shutter", "快门", "S"), shutter)
            }
        }
    }
}

class HuaweiCameraHelper : DeviceCameraHelper {
    private val nodeFinder = NodeFinder("com.huawei.camera")
    
    override suspend fun autoFillParams(rootNode: AccessibilityNodeInfo, params: CameraParams) {
        Timber.d("Huawei camera auto-fill initiated")
        
        withContext(Dispatchers.Main) {
            params.iso?.let { iso ->
                nodeFinder.findAndFill(rootNode, listOf("iso", "ISO", "感光度"), iso)
            }
            params.shutter?.let { shutter ->
                nodeFinder.findAndFill(rootNode, listOf("shutter", "快门速度", "S"), shutter)
            }
        }
    }
}

class SamsungCameraHelper : DeviceCameraHelper {
    private val nodeFinder = NodeFinder("com.samsung.android.camera")
    
    override suspend fun autoFillParams(rootNode: AccessibilityNodeInfo, params: CameraParams) {
        Timber.d("Samsung camera auto-fill initiated")
        
        withContext(Dispatchers.Main) {
            params.iso?.let { iso ->
                nodeFinder.findAndFill(rootNode, listOf("iso", "ISO", "ISO值"), iso)
            }
            params.shutter?.let { shutter ->
                nodeFinder.findAndFill(rootNode, listOf("shutter", "shutter_speed", "S"), shutter)
            }
        }
    }
}

class GenericCameraHelper : DeviceCameraHelper {
    override suspend fun autoFillParams(rootNode: AccessibilityNodeInfo, params: CameraParams) {
        Timber.d("Generic camera auto-fill - using fuzzy matching")
        
        withContext(Dispatchers.Main) {
            val nodeFinder = NodeFinder("generic")
            
            params.iso?.let { iso ->
                nodeFinder.findByContentDescription(rootNode, listOf("ISO", "iso", "感光度", "ISO值"))?.let { node ->
                    node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                    node.recycle()
                }
            }
        }
    }
}

class NodeFinder(private val packageName: String) {
    
    fun findAndFill(rootNode: AccessibilityNodeInfo, identifiers: List<String>, value: String) {
        identifiers.forEach { identifier ->
            findByViewId(rootNode, identifier)?.let { node ->
                try {
                    if (node.isClickable || node.isFocusable) {
                        Timber.d("Found node: $identifier, performing click")
                        node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                    }
                } finally {
                    node.recycle()
                }
                return
            }
        }
        
        identifiers.forEach { identifier ->
            findByContentDescription(rootNode, listOf(identifier))?.let { node ->
                try {
                    if (node.isClickable || node.isFocusable) {
                        Timber.d("Found node by content desc: $identifier, performing click")
                        node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                    }
                } finally {
                    node.recycle()
                }
                return
            }
        }
        
        Timber.w("Could not find node for: ${identifiers.first()}")
    }

    fun findByViewId(rootNode: AccessibilityNodeInfo, viewId: String): AccessibilityNodeInfo? {
        val fullViewId = if (viewId.contains(packageName)) viewId else "$packageName:id/$viewId"
        return rootNode.findAccessibilityNodeInfosByViewId(fullViewId)?.firstOrNull()
    }

    fun findByContentDescription(rootNode: AccessibilityNodeInfo, descriptions: List<String>): AccessibilityNodeInfo? {
        descriptions.forEach { desc ->
            val nodes = rootNode.findAccessibilityNodeInfosByText(desc)
            nodes.firstOrNull { it.isClickable || it.isFocusable }?.let { return it }
        }
        return null
    }

    fun findByCoordinates(rootNode: AccessibilityNodeInfo, bounds: Rect): AccessibilityNodeInfo? {
        val children = mutableListOf<AccessibilityNodeInfo>()
        collectLeaves(rootNode, children)
        
        return children.firstOrNull { node ->
            val nodeBounds = Rect()
            node.getBoundsInScreen(nodeBounds)
            nodeBounds.intersect(bounds)
        }
    }

    private fun collectLeaves(node: AccessibilityNodeInfo, leaves: MutableList<AccessibilityNodeInfo>) {
        if (node.childCount == 0) {
            leaves.add(node)
        } else {
            for (i in 0 until node.childCount) {
                node.getChild(i)?.let { child ->
                    collectLeaves(child, leaves)
                }
            }
        }
    }
}

class ParamMapper {
    private val paramMapping = mapOf(
        "iso" to mapOf(
            DeviceType.OPPO to listOf("iso_value", "iso", "ISO", "感光度"),
            DeviceType.ONEPLUS to listOf("iso_value", "iso", "ISO", "感光度"),
            DeviceType.REALME to listOf("iso_value", "iso", "ISO", "感光度"),
            DeviceType.XIAOMI to listOf("iso", "ISO值", "感光度"),
            DeviceType.VIVO to listOf("iso", "ISO", "感光度"),
            DeviceType.HUAWEI to listOf("iso", "ISO", "感光度"),
            DeviceType.SAMSUNG to listOf("iso", "ISO", "ISO值")
        ),
        "shutter" to mapOf(
            DeviceType.OPPO to listOf("shutter_value", "shutter", "快门", "S"),
            DeviceType.ONEPLUS to listOf("shutter_value", "shutter", "快门", "S"),
            DeviceType.REALME to listOf("shutter_value", "shutter", "快门", "S"),
            DeviceType.XIAOMI to listOf("shutter", "快门速度", "S"),
            DeviceType.VIVO to listOf("shutter", "快门", "S"),
            DeviceType.HUAWEI to listOf("shutter", "快门速度", "S"),
            DeviceType.SAMSUNG to listOf("shutter", "shutter_speed", "S")
        ),
        "ev" to mapOf(
            DeviceType.OPPO to listOf("ev_value", "ev", "曝光补偿", "EV"),
            DeviceType.ONEPLUS to listOf("ev_value", "ev", "曝光补偿", "EV"),
            DeviceType.REALME to listOf("ev_value", "ev", "曝光补偿", "EV")
        ),
        "wb" to mapOf(
            DeviceType.OPPO to listOf("wb_value", "wb", "白平衡", "WB"),
            DeviceType.ONEPLUS to listOf("wb_value", "wb", "白平衡", "WB"),
            DeviceType.REALME to listOf("wb_value", "wb", "白平衡", "WB")
        )
    )

    fun getMapping(paramName: String, deviceType: DeviceType): List<String> {
        return paramMapping[paramName]?.get(deviceType) ?: emptyList()
    }
}

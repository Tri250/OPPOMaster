package com.omaster.app.accessibility

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Context
import android.content.Intent
import android.graphics.Path
import android.graphics.Rect
import android.os.Build
import android.provider.Settings
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import androidx.annotation.RequiresApi
import com.omaster.app.R
import com.omaster.app.model.CameraParams
import kotlinx.coroutines.*
import timber.log.Timber
import kotlin.math.max
import kotlin.math.min

/**
 * 相机参数自动填入无障碍服务
 * 支持六大品牌相机：OPPO、一加、Realme、小米、华为、vivo
 */
class AutoFillAccessibilityService : AccessibilityService() {

    companion object {
        // 服务状态
        private var instance: AutoFillAccessibilityService? = null
        private var currentParams: Map<String, String>? = null
        private var isFillingInProgress = false
        private var fillResultListener: ((FillResult) -> Unit)? = null
        
        // 品牌相机包名
        private val BRAND_CAMERA_PACKAGES = mapOf(
            "oppo" to listOf("com.oppo.camera", "com.coloros.camera"),
            "oneplus" to listOf("com.oneplus.camera"),
            "realme" to listOf("com.realme.camera"),
            "xiaomi" to listOf("com.miui.camera", "com.android.camera"),
            "huawei" to listOf("com.huawei.camera", "com.hihonor.camera"),
            "vivo" to listOf("com.vivo.camera", "com.android.camera")
        )

        /**
         * 设置要填入的参数
         */
        fun setParams(params: CameraParams) {
            currentParams = mapOf(
                "iso" to params.iso.toString(),
                "shutter" to params.shutter,
                "ev" to params.ev,
                "wb" to (params.wb ?: "auto"),
                "filter" to params.filter
            )
        }

        /**
         * 设置结果监听器
         */
        fun setFillResultListener(listener: ((FillResult) -> Unit)?) {
            fillResultListener = listener
        }

        /**
         * 检查服务是否启用
         */
        fun isServiceEnabled(context: Context): Boolean {
            val pref = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            )
            return pref?.contains("${context.packageName}/.accessibility.AutoFillAccessibilityService") ?: false
        }

        /**
         * 打开无障碍服务设置页面
         */
        fun openAccessibilitySettings(context: Context) {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(intent)
        }

        /**
         * 尝试触发自动填入
         */
        fun triggerAutoFill() {
            instance?.let { service ->
                if (!isFillingInProgress && currentParams != null) {
                    service.startFillProcess()
                }
            }
        }
    }

    // 协程作用域
    private val serviceScope = MainScope()
    
    // 当前品牌助手
    private var currentBrandHelper: CameraAutoFillHelper? = null

    override fun onCreate() {
        super.onCreate()
        instance = this
        Timber.d("无障碍服务已创建")
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
        serviceScope.cancel()
        Timber.d("无障碍服务已销毁")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event ?: return
        
        // 仅在窗口状态变化时处理
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val packageName = event.packageName?.toString() ?: return
            Timber.d("窗口变化: $packageName")
            
            // 检测到相机应用时自动触发填入
            if (isCameraApp(packageName) && currentParams != null && !isFillingInProgress) {
                startFillProcess()
            }
        }
    }

    override fun onInterrupt() {
        Timber.d("无障碍服务被中断")
        if (isFillingInProgress) {
            notifyFillResult(FillResult.INTERRUPTED)
        }
        isFillingInProgress = false
    }

    /**
     * 开始参数填入流程
     */
    private fun startFillProcess() {
        val rootNode = rootInActiveWindow ?: return
        val params = currentParams ?: return
        
        isFillingInProgress = true
        Timber.d("开始自动填入参数: $params")
        
        // 识别品牌
        val packageName = rootNode.packageName?.toString() ?: ""
        val brand = detectBrand(packageName)
        Timber.d("检测到相机品牌: $brand")
        
        // 获取对应的助手
        currentBrandHelper = getBrandHelper(brand)
        
        serviceScope.launch {
            try {
                val result = currentBrandHelper?.autoFillParams(rootNode, params) ?: FillResult.NO_HELPER
                
                // 处理结果
                when (result) {
                    FillResult.SUCCESS -> {
                        Timber.d("参数填入成功")
                        notifyFillResult(FillResult.SUCCESS)
                    }
                    FillResult.PARTIAL_SUCCESS -> {
                        Timber.d("部分参数填入成功")
                        notifyFillResult(FillResult.PARTIAL_SUCCESS)
                    }
                    else -> {
                        Timber.d("参数填入失败: $result")
                        notifyFillResult(result)
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "自动填入异常")
                notifyFillResult(FillResult.ERROR)
            } finally {
                isFillingInProgress = false
            }
        }
    }

    /**
     * 识别相机品牌
     */
    private fun detectBrand(packageName: String): String {
        return BRAND_CAMERA_PACKAGES.entries.find { (_, packages) ->
            packages.any { packageName.contains(it) }
        }?.key ?: "generic"
    }

    /**
     * 获取品牌助手
     */
    private fun getBrandHelper(brand: String): CameraAutoFillHelper {
        return when (brand) {
            "oppo" -> OppoCameraHelper(this)
            "oneplus" -> OnePlusCameraHelper(this)
            "realme" -> RealmeCameraHelper(this)
            "xiaomi" -> XiaomiCameraHelper(this)
            "huawei" -> HuaweiCameraHelper(this)
            "vivo" -> VivoCameraHelper(this)
            else -> GenericCameraHelper(this)
        }
    }

    /**
     * 通知填入结果
     */
    private fun notifyFillResult(result: FillResult) {
        fillResultListener?.invoke(result)
    }

    /**
     * 检查是否是相机应用
     */
    private fun isCameraApp(packageName: String): Boolean {
        return BRAND_CAMERA_PACKAGES.values.flatten().any { 
            packageName.contains(it) 
        }
    }

    /**
     * 执行点击操作
     */
    fun performClick(node: AccessibilityNodeInfo): Boolean {
        return if (node.isClickable) {
            node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
        } else {
            // 尝试点击父节点
            node.parent?.let { performClick(it) } ?: false
        }
    }

    /**
     * 执行长按操作
     */
    fun performLongClick(node: AccessibilityNodeInfo): Boolean {
        return node.performAction(AccessibilityNodeInfo.ACTION_LONG_CLICK)
    }

    /**
     * 滚动查找元素
     */
    fun scrollToFindNode(rootNode: AccessibilityNodeInfo, predicate: (AccessibilityNodeInfo) -> Boolean): AccessibilityNodeInfo? {
        // 先尝试直接查找
        findNodeByPredicate(rootNode, predicate)?.let { return it }
        
        // 尝试滚动后查找
        performScroll(rootNode)
        
        // 延迟后再次查找
        Thread.sleep(300)
        
        return findNodeByPredicate(rootNode, predicate)
    }

    /**
     * 查找节点
     */
    fun findNodeByPredicate(
        rootNode: AccessibilityNodeInfo,
        predicate: (AccessibilityNodeInfo) -> Boolean
    ): AccessibilityNodeInfo? {
        val nodes = mutableListOf<AccessibilityNodeInfo>()
        collectAllNodes(rootNode, nodes)
        return nodes.find(predicate)
    }

    /**
     * 递归收集所有节点
     */
    private fun collectAllNodes(node: AccessibilityNodeInfo, result: MutableList<AccessibilityNodeInfo>) {
        result.add(node)
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            collectAllNodes(child, result)
        }
    }

    /**
     * 执行滚动
     */
    private fun performScroll(node: AccessibilityNodeInfo): Boolean {
        return node.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD) ||
               node.performAction(AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD)
    }

    /**
     * 执行手势点击（Android 7.0+）
     */
    @RequiresApi(Build.VERSION_CODES.N)
    fun performGestureClick(x: Float, y: Float): Boolean {
        val path = Path()
        path.moveTo(x, y)
        
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 100))
            .build()
            
        return dispatchGesture(gesture, null, null)
    }

    /**
     * 设置文本
     */
    fun setText(node: AccessibilityNodeInfo, text: String): Boolean {
        val arguments = AccessibilityNodeInfo.AccessibilityActionArguments
            .forSetText(text)
        return node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
    }
}

/**
 * 填入结果枚举
 */
enum class FillResult {
    SUCCESS,              // 完全成功
    PARTIAL_SUCCESS,      // 部分成功
    NO_HELPER,            // 无对应助手
    CAMERA_NOT_FOUND,     // 未找到相机
    PARAMS_NOT_SET,       // 参数未设置
    INTERRUPTED,          // 被中断
    ERROR                 // 错误
}

/**
 * 相机自动填入助手接口
 */
abstract class CameraAutoFillHelper(protected val service: AutoFillAccessibilityService) {
    
    // 填入统计
    protected var successCount = 0
    protected var failCount = 0
    
    /**
     * 自动填入参数
     */
    suspend fun autoFillParams(rootNode: AccessibilityNodeInfo, params: Map<String, String>): FillResult {
        successCount = 0
        failCount = 0
        
        Timber.d("开始填入参数: $params")
        
        // 1. 切换到专业/大师模式
        if (!switchToProMode(rootNode)) {
            Timber.w("无法切换到专业模式，尝试继续...")
        }
        
        delay(200)
        
        // 2. 填入各个参数
        params["iso"]?.let { fillISO(rootNode, it) }
        delay(150)
        
        params["shutter"]?.let { fillShutter(rootNode, it) }
        delay(150)
        
        params["ev"]?.let { fillEV(rootNode, it) }
        delay(150)
        
        params["wb"]?.let { fillWB(rootNode, it) }
        delay(150)
        
        params["filter"]?.takeIf { it.isNotEmpty() }?.let { fillFilter(rootNode, it) }
        
        // 3. 统计结果
        return when {
            successCount >= 4 -> FillResult.SUCCESS
            successCount >= 1 -> FillResult.PARTIAL_SUCCESS
            else -> FillResult.ERROR
        }
    }
    
    /**
     * 切换到专业/大师模式
     */
    protected abstract suspend fun switchToProMode(rootNode: AccessibilityNodeInfo): Boolean
    
    /**
     * 填入ISO
     */
    protected abstract suspend fun fillISO(rootNode: AccessibilityNodeInfo, value: String): Boolean
    
    /**
     * 填入快门速度
     */
    protected abstract suspend fun fillShutter(rootNode: AccessibilityNodeInfo, value: String): Boolean
    
    /**
     * 填入曝光补偿
     */
    protected abstract suspend fun fillEV(rootNode: AccessibilityNodeInfo, value: String): Boolean
    
    /**
     * 填入白平衡
     */
    protected abstract suspend fun fillWB(rootNode: AccessibilityNodeInfo, value: String): Boolean
    
    /**
     * 填入滤镜
     */
    protected abstract suspend fun fillFilter(rootNode: AccessibilityNodeInfo, value: String): Boolean
    
    /**
     * 通过文本查找节点
     */
    protected fun findNodeByText(rootNode: AccessibilityNodeInfo, text: String): AccessibilityNodeInfo? {
        return service.findNodeByPredicate(rootNode) { node ->
            node.text?.contains(text, ignoreCase = true) == true ||
            node.contentDescription?.contains(text, ignoreCase = true) == true
        }
    }
    
    /**
     * 通过ID查找节点
     */
    protected fun findNodeById(rootNode: AccessibilityNodeInfo, id: String): AccessibilityNodeInfo? {
        return try {
            rootNode.findAccessibilityNodeInfosByViewId(id).firstOrNull()
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * 标记成功
     */
    protected fun markSuccess() {
        successCount++
    }
    
    /**
     * 标记失败
     */
    protected fun markFail() {
        failCount++
    }
}

/**
 * OPPO相机助手
 */
class OppoCameraHelper(service: AutoFillAccessibilityService) : CameraAutoFillHelper(service) {
    
    companion object {
        // OPPO相机常用ID
        private const val ID_PRO_MODE = "com.oppo.camera:id/pro_mode"
        private const val ID_ISO = "com.oppo.camera:id/iso_value"
        private const val ID_SHUTTER = "com.oppo.camera:id/shutter_value"
        private const val ID_EV = "com.oppo.camera:id/ev_value"
        private const val ID_WB = "com.oppo.camera:id/wb_value"
        private const val ID_FILTER = "com.oppo.camera:id/filter_value"
    }
    
    override suspend fun switchToProMode(rootNode: AccessibilityNodeInfo): Boolean {
        val proTextList = listOf("专业", "大师", "Pro", "Expert")
        
        for (text in proTextList) {
            val node = findNodeByText(rootNode, text)
            if (node != null) {
                if (service.performClick(node)) {
                    Timber.d("已切换到专业模式: $text")
                    return true
                }
            }
        }
        
        // 尝试通过ID查找
        findNodeById(rootNode, ID_PRO_MODE)?.let {
            if (service.performClick(it)) {
                Timber.d("已通过ID切换到专业模式")
                return true
            }
        }
        
        return false
    }
    
    override suspend fun fillISO(rootNode: AccessibilityNodeInfo, value: String): Boolean {
        Timber.d("尝试填入ISO: $value")
        
        // 尝试通过文本查找
        val isoNode = findNodeByText(rootNode, "ISO") ?: findNodeById(rootNode, ID_ISO)
        
        return if (isoNode != null) {
            if (service.performClick(isoNode)) {
                delay(200)
                // 尝试选择对应值
                selectValueFromOptions(rootNode, value)
                markSuccess()
                true
            } else {
                markFail()
                false
            }
        } else {
            markFail()
            false
        }
    }
    
    override suspend fun fillShutter(rootNode: AccessibilityNodeInfo, value: String): Boolean {
        Timber.d("尝试填入快门: $value")
        
        val shutterTexts = listOf("快门", "S", "Shutter")
        var shutterNode: AccessibilityNodeInfo? = null
        
        for (text in shutterTexts) {
            shutterNode = findNodeByText(rootNode, text)
            if (shutterNode != null) break
        }
        
        shutterNode = shutterNode ?: findNodeById(rootNode, ID_SHUTTER)
        
        return if (shutterNode != null) {
            if (service.performClick(shutterNode)) {
                delay(200)
                selectValueFromOptions(rootNode, value)
                markSuccess()
                true
            } else {
                markFail()
                false
            }
        } else {
            markFail()
            false
        }
    }
    
    override suspend fun fillEV(rootNode: AccessibilityNodeInfo, value: String): Boolean {
        Timber.d("尝试填入EV: $value")
        
        val evTexts = listOf("曝光", "EV", "曝光补偿")
        var evNode: AccessibilityNodeInfo? = null
        
        for (text in evTexts) {
            evNode = findNodeByText(rootNode, text)
            if (evNode != null) break
        }
        
        evNode = evNode ?: findNodeById(rootNode, ID_EV)
        
        return if (evNode != null) {
            if (service.performClick(evNode)) {
                delay(200)
                selectValueFromOptions(rootNode, value)
                markSuccess()
                true
            } else {
                markFail()
                false
            }
        } else {
            markFail()
            false
        }
    }
    
    override suspend fun fillWB(rootNode: AccessibilityNodeInfo, value: String): Boolean {
        Timber.d("尝试填入WB: $value")
        
        val wbTexts = listOf("白平衡", "WB", "White Balance")
        var wbNode: AccessibilityNodeInfo? = null
        
        for (text in wbTexts) {
            wbNode = findNodeByText(rootNode, text)
            if (wbNode != null) break
        }
        
        wbNode = wbNode ?: findNodeById(rootNode, ID_WB)
        
        return if (wbNode != null) {
            if (service.performClick(wbNode)) {
                delay(200)
                selectValueFromOptions(rootNode, value)
                markSuccess()
                true
            } else {
                markFail()
                false
            }
        } else {
            markFail()
            false
        }
    }
    
    override suspend fun fillFilter(rootNode: AccessibilityNodeInfo, value: String): Boolean {
        Timber.d("尝试填入滤镜: $value")
        
        val filterTexts = listOf("滤镜", "Filter")
        var filterNode: AccessibilityNodeInfo? = null
        
        for (text in filterTexts) {
            filterNode = findNodeByText(rootNode, text)
            if (filterNode != null) break
        }
        
        filterNode = filterNode ?: findNodeById(rootNode, ID_FILTER)
        
        return if (filterNode != null) {
            if (service.performClick(filterNode)) {
                delay(200)
                selectValueFromOptions(rootNode, value)
                markSuccess()
                true
            } else {
                markFail()
                false
            }
        } else {
            markFail()
            false
        }
    }
    
    /**
     * 从选项中选择值
     */
    private fun selectValueFromOptions(rootNode: AccessibilityNodeInfo, value: String): Boolean {
        // 查找包含目标值的节点
        val optionNode = findNodeByText(rootNode, value)
        if (optionNode != null) {
            return service.performClick(optionNode)
        }
        
        // 尝试模糊匹配
        val normalizedValue = value.lowercase()
        val fuzzyNode = service.findNodeByPredicate(rootNode) { node ->
            val nodeText = (node.text ?: node.contentDescription ?: "").toString().lowercase()
            nodeText.contains(normalizedValue) ||
            normalizedValue.contains(nodeText)
        }
        
        if (fuzzyNode != null) {
            return service.performClick(fuzzyNode)
        }
        
        return false
    }
}

/**
 * 一加相机助手（复用OPPO逻辑）
 */
class OnePlusCameraHelper(service: AutoFillAccessibilityService) : OppoCameraHelper(service) {
    init {
        Timber.d("使用一加相机助手")
    }
}

/**
 * Realme相机助手（复用OPPO逻辑）
 */
class RealmeCameraHelper(service: AutoFillAccessibilityService) : OppoCameraHelper(service) {
    init {
        Timber.d("使用Realme相机助手")
    }
}

/**
 * 小米相机助手
 */
class XiaomiCameraHelper(service: AutoFillAccessibilityService) : OppoCameraHelper(service) {
    
    companion object {
        private const val ID_MI_PRO_MODE = "com.miui.camera:id/pro_mode_button"
        private const val ID_MI_ISO = "com.miui.camera:id/iso"
        private const val ID_MI_SHUTTER = "com.miui.camera:id/shutter"
        private const val ID_MI_EV = "com.miui.camera:id/exposure"
        private const val ID_MI_WB = "com.miui.camera:id/wb"
    }
    
    init {
        Timber.d("使用小米相机助手")
    }
    
    override suspend fun switchToProMode(rootNode: AccessibilityNodeInfo): Boolean {
        // 小米相机特殊处理
        val proTexts = listOf("专业", "Pro", "手动")
        for (text in proTexts) {
            findNodeByText(rootNode, text)?.let {
                if (service.performClick(it)) {
                    Timber.d("小米相机已切换到专业模式")
                    return true
                }
            }
        }
        return false
    }
}

/**
 * 华为相机助手
 */
class HuaweiCameraHelper(service: AutoFillAccessibilityService) : OppoCameraHelper(service) {
    
    companion object {
        private const val ID_HW_PRO_MODE = "com.huawei.camera:id/pro_mode"
    }
    
    init {
        Timber.d("使用华为相机助手")
    }
    
    override suspend fun switchToProMode(rootNode: AccessibilityNodeInfo): Boolean {
        val proTexts = listOf("专业", "Pro", "专业模式")
        for (text in proTexts) {
            findNodeByText(rootNode, text)?.let {
                if (service.performClick(it)) {
                    Timber.d("华为相机已切换到专业模式")
                    return true
                }
            }
        }
        return false
    }
}

/**
 * vivo相机助手
 */
class VivoCameraHelper(service: AutoFillAccessibilityService) : OppoCameraHelper(service) {
    
    init {
        Timber.d("使用vivo相机助手")
    }
    
    override suspend fun switchToProMode(rootNode: AccessibilityNodeInfo): Boolean {
        val proTexts = listOf("专业", "Pro", "专业模式", "手动")
        for (text in proTexts) {
            findNodeByText(rootNode, text)?.let {
                if (service.performClick(it)) {
                    Timber.d("vivo相机已切换到专业模式")
                    return true
                }
            }
        }
        return false
    }
}

/**
 * 通用相机助手
 */
class GenericCameraHelper(service: AutoFillAccessibilityService) : OppoCameraHelper(service) {
    
    init {
        Timber.d("使用通用相机助手")
    }
}

/**
 * 延迟函数
 */
suspend fun delay(timeMillis: Long) {
    withContext(Dispatchers.IO) {
        Thread.sleep(timeMillis)
    }
}

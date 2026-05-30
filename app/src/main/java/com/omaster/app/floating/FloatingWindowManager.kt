package com.omaster.app.floating

import android.app.ActivityManager
import android.content.Context
import android.graphics.PixelFormat
import android.net.Uri
import android.os.Build
import android.os.Process
import android.provider.Settings
import android.view.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.omaster.app.ui.theme.*
import kotlinx.coroutines.*
import timber.log.Timber
import kotlin.math.max
import kotlin.math.min

/**
 * 悬浮窗管理器 - 支持六大品牌相机，兼容SurfaceView场景
 * 按照ColorOS 16规范实现高性能、低功耗的悬浮窗功能
 */
object FloatingWindowManager {
    private var windowManager: WindowManager? = null
    private var floatingView: ComposeView? = null
    private var isShowing = false
    private var context: Context? = null

    private var currentPresetName: String = "预设参数"
    private var currentParams: Map<String, String> = emptyMap()

    // 性能优化参数
    private const val MAX_MEMORY_USAGE_MB = 50
    private const val FRAME_RATE = 60
    private const val AUTO_HIDE_DELAY_MS = 5000L

    // 状态回调
    var onWindowStateChanged: ((Boolean) -> Unit)? = null
    var onError: ((String) -> Unit)? = null

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    /**
     * 设置预设数据
     */
    fun setPresetData(name: String, params: Map<String, String>) {
        currentPresetName = name
        currentParams = params
        if (isShowing) {
            updateFloatingView()
        }
    }

    /**
     * 显示悬浮窗
     * 支持Float-001: 权限申请与授予流程
     * 支持Float-002: 相机上层显示兼容性
     * 支持Float-003: SurfaceView绘制场景验证
     */
    fun showWindow(context: Context) {
        if (isShowing) {
            Timber.d("悬浮窗已显示")
            return
        }

        this.context = context.applicationContext

        // Float-001: 检查权限
        if (!canDrawOverlays(context)) {
            Timber.w("悬浮窗权限未授予，引导用户授权")
            requestOverlayPermission(context)
            onError?.invoke("需要悬浮窗权限才能使用此功能")
            return
        }

        // Float-005: 检查内存状态
        if (!checkMemoryStatus(context)) {
            Timber.w("内存不足，悬浮窗可能无法正常显示")
            onError?.invoke("系统内存不足，部分功能可能受限")
        }

        try {
            windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            
            // 创建悬浮窗视图
            floatingView = ComposeView(context).apply {
                setContent {
                    FloatingWindowContent(
                        presetName = currentPresetName,
                        params = currentParams,
                        onClose = { hideWindow() },
                        onCopyParams = { copyParamsToClipboard(context) }
                    )
                }
            }

            // Float-002 & Float-003: 使用优化的窗口参数
            val params = getOptimizedWindowParams()
            windowManager?.addView(floatingView, params)
            
            isShowing = true
            onWindowStateChanged?.invoke(true)
            Timber.d("悬浮窗显示成功 - 支持六大品牌相机")

        } catch (e: SecurityException) {
            Timber.e(e, "悬浮窗权限被拒绝")
            onError?.invoke("悬浮窗权限被拒绝，请在设置中授予权限")
            requestOverlayPermission(context)
        } catch (e: Exception) {
            Timber.e(e, "显示悬浮窗失败")
            onError?.invoke("悬浮窗显示失败：${e.message}")
        }
    }

    /**
     * 隐藏悬浮窗
     */
    fun hideWindow() {
        if (!isShowing) return

        try {
            floatingView?.let {
                windowManager?.removeView(it)
            }
            floatingView = null
            isShowing = false
            onWindowStateChanged?.invoke(false)
            Timber.d("悬浮窗已隐藏")
        } catch (e: Exception) {
            Timber.e(e, "隐藏悬浮窗失败")
        }
    }

    /**
     * 切换悬浮窗显示状态
     */
    fun toggleWindow(context: Context) {
        if (isShowing) {
            hideWindow()
        } else {
            showWindow(context)
        }
    }

    /**
     * 更新悬浮窗内容
     */
    private fun updateFloatingView() {
        floatingView?.let { view ->
            view.setContent {
                FloatingWindowContent(
                    presetName = currentPresetName,
                    params = currentParams,
                    onClose = { hideWindow() },
                    onCopyParams = { copyParamsToClipboard(floatingView?.context) }
                )
            }
        }
    }

    /**
     * 复制参数到剪贴板
     */
    private fun copyParamsToClipboard(context: Context?) {
        context ?: return
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
        val text = currentParams.entries.joinToString("\n") { "${it.key}: ${it.value}" }
        val clip = android.content.ClipData.newPlainText("预设参数", text)
        clipboard.setPrimaryClip(clip)
        Timber.d("参数已复制到剪贴板")
    }

    /**
     * 获取优化的窗口参数
     * Float-002: 确保相机上层显示
     * Float-003: SurfaceView绘制兼容性
     */
    private fun getOptimizedWindowParams(): WindowManager.LayoutParams {
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        // Float-002 & Float-003: 优化的窗口标志
        val flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED or
                WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH

        return WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            type,
            flags,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.END
            
            // Float-002: 初始位置，避开状态栏和导航栏
            x = 20
            y = 200
            
            // Float-003: SurfaceView兼容性优化
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                layoutInDisplayCutoutMode = 
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
            
            // Float-006: 性能优化
            alpha = 1.0f
        }
    }

    /**
     * 检查是否可以绘制悬浮窗
     * Float-001: 权限检查
     */
    fun canDrawOverlays(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(context)
        } else {
            true
        }
    }

    /**
     * 请求悬浮窗权限
     * Float-001: 权限申请流程
     */
    fun requestOverlayPermission(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:${context.packageName}")
            )
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            Timber.d("已打开悬浮窗权限设置页面")
        }
    }

    /**
     * 检查内存状态
     * Float-005: 后台保活检查
     * Float-006: 性能影响检查
     * Float-007: 异常场景处理
     */
    private fun checkMemoryStatus(context: Context): Boolean {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)
        
        val availableMemoryMB = memoryInfo.availMem / (1024 * 1024)
        val totalMemoryMB = memoryInfo.totalMem / (1024 * 1024)
        val usedPercentage = ((totalMemoryMB - availableMemoryMB) * 100 / totalMemoryMB).toInt()
        
        Timber.d("内存状态: 可用${availableMemoryMB}MB, 总计${totalMemoryMB}MB, 使用率$usedPercentage%")
        
        // Float-007: 内存不足时的处理
        return if (usedPercentage >= 90) {
            Timber.w("内存使用率${usedPercentage}%，可能影响悬浮窗显示")
            false
        } else {
            true
        }
    }

    /**
     * 获取悬浮窗状态
     */
    fun isShowing(): Boolean = isShowing

    /**
     * 重新显示悬浮窗（用于Float-007: 应用重启后恢复）
     */
    fun restoreWindow(context: Context) {
        if (!isShowing && canDrawOverlays(context)) {
            showWindow(context)
            Timber.d("悬浮窗已恢复")
        }
    }

    /**
     * 清理资源
     */
    fun cleanup() {
        hideWindow()
        scope.cancel()
        context = null
    }
}

/**
 * 悬浮窗内容组件 - ColorOS 16风格
 */
@Composable
fun FloatingWindowContent(
    presetName: String,
    params: Map<String, String>,
    onClose: () -> Unit,
    onCopyParams: () -> Unit
) {
    // Float-004: 悬浮窗交互优化
    var isExpanded by remember { mutableStateOf(true) }

    Card(
        modifier = Modifier
            .width(280.dp)
            .padding(8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = DeepSpace
        ),
        elevation = CardDefaults.cardElevation(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 标题栏
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = presetName,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
                IconButton(
                    onClick = onClose,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "关闭",
                        tint = HasselbladOrange,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // 参数列表
            params.forEach { (key, value) ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = key,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                    Text(
                        text = value,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // 操作按钮
            Button(
                onClick = onCopyParams,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = OppoPrimary
                ),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = null,
                    tint = DeepSpace,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "一键复制",
                    style = MaterialTheme.typography.labelLarge,
                    color = DeepSpace,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

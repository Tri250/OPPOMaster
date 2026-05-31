package com.omaster.app.floating

import android.content.Context
import android.graphics.PixelFormat
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.view.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.omaster.app.service.FloatingWindowForegroundService
import com.omaster.app.ui.theme.AccentPrimary
import com.omaster.app.ui.theme.DeepSpace
import com.omaster.app.ui.theme.HasselbladOrange
import com.omaster.app.utils.RomUtils
import timber.log.Timber

object FloatingWindowManager {
    private var windowManager: WindowManager? = null
    private var floatingView: ComposeView? = null
    private var isShowing = false
    private var serviceStarted = false

    private var presetList: List<Pair<String, Map<String, String>>> = listOf(
        "人像模式" to mapOf("ISO" to "100", "快门" to "1/125s", "光圈" to "f/1.8"),
        "夜景模式" to mapOf("ISO" to "3200", "快门" to "1/8s", "光圈" to "f/1.8"),
        "风光模式" to mapOf("ISO" to "100", "快门" to "1/250s", "光圈" to "f/8.0"),
        "微距模式" to mapOf("ISO" to "200", "快门" to "1/200s", "光圈" to "f/2.4"),
        "运动模式" to mapOf("ISO" to "800", "快门" to "1/2000s", "光圈" to "f/2.8")
    )
    private var currentPresetIndex = 0

    private val currentPresetName: String
        get() = presetList[currentPresetIndex].first
    private val currentParams: Map<String, String>
        get() = presetList[currentPresetIndex].second

    fun setPresetList(presets: List<Pair<String, Map<String, String>>>) {
        presetList = presets
        if (currentPresetIndex >= presetList.size) {
            currentPresetIndex = presetList.size - 1
        }
        if (isShowing) {
            updateFloatingView()
        }
    }

    fun showWindow(context: Context) {
        if (isShowing) return

        if (!canDrawOverlays(context)) {
            requestOverlayPermission(context)
            return
        }

        try {
            // 启动前台服务保活
            if (!serviceStarted) {
                FloatingWindowForegroundService.startService(context)
                serviceStarted = true
            }
            
            windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            floatingView = ComposeView(context).apply {
                setContent {
                    FloatingWindowContent(
                        presetName = currentPresetName,
                        params = currentParams,
                        presetIndex = currentPresetIndex,
                        totalPresets = presetList.size,
                        onSwipeLeft = { nextPreset() },
                        onSwipeRight = { prevPreset() },
                        onClose = { hideWindow() },
                        onCopyParams = { copyParamsToClipboard(context) },
                        onOpenPermissionSettings = { RomUtils.openBatteryOptimizationSettings(context) }
                    )
                }
            }

            val params = getWindowParams()
            windowManager?.addView(floatingView, params)
            isShowing = true
            Timber.d("Floating window shown")
        } catch (e: Exception) {
            Timber.e(e, "Failed to show floating window")
        }
    }

    fun hideWindow() {
        if (!isShowing) return

        try {
            floatingView?.let {
                windowManager?.removeView(it)
            }
            floatingView = null
            isShowing = false
            
            // 隐藏窗口时不停止服务，保持后台保活
            Timber.d("Floating window hidden")
        } catch (e: Exception) {
            Timber.e(e, "Failed to hide floating window")
        }
    }
    
    fun destroy(context: Context) {
        hideWindow()
        if (serviceStarted) {
            FloatingWindowForegroundService.stopService(context)
            serviceStarted = false
        }
    }

    fun toggleWindow(context: Context) {
        if (isShowing) {
            hideWindow()
        } else {
            showWindow(context)
        }
    }

    private fun nextPreset() {
        if (presetList.isEmpty()) return
        currentPresetIndex = (currentPresetIndex + 1) % presetList.size
        updateFloatingView()
    }

    private fun prevPreset() {
        if (presetList.isEmpty()) return
        currentPresetIndex = if (currentPresetIndex > 0) currentPresetIndex - 1 else presetList.size - 1
        updateFloatingView()
    }

    private fun updateFloatingView() {
        floatingView?.setContent {
            FloatingWindowContent(
                presetName = currentPresetName,
                params = currentParams,
                presetIndex = currentPresetIndex,
                totalPresets = presetList.size,
                onSwipeLeft = { nextPreset() },
                onSwipeRight = { prevPreset() },
                onClose = { hideWindow() },
                onCopyParams = { copyParamsToClipboard(floatingView?.context) },
                onOpenPermissionSettings = { RomUtils.openBatteryOptimizationSettings(floatingView?.context) }
            )
        }
    }

    private fun copyParamsToClipboard(context: Context?) {
        context ?: return
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
        val text = currentParams.entries.joinToString("\n") { "${it.key}: ${it.value}" }
        val clip = android.content.ClipData.newPlainText("参数", text)
        clipboard.setPrimaryClip(clip)
    }

    private fun getWindowParams(): WindowManager.LayoutParams {
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            WindowManager.LayoutParams.TYPE_PHONE
        }

        return WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.END
            x = 20
            y = 200
        }
    }

    fun canDrawOverlays(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(context)
        } else {
            true
        }
    }

    fun requestOverlayPermission(context: Context) {
        RomUtils.openOverlayPermissionSettings(context)
    }
}

@Composable
fun FloatingWindowContent(
    presetName: String,
    params: Map<String, String>,
    presetIndex: Int,
    totalPresets: Int,
    onSwipeLeft: () -> Unit,
    onSwipeRight: () -> Unit,
    onClose: () -> Unit,
    onCopyParams: () -> Unit,
    onOpenPermissionSettings: () -> Unit
) {
    var dragOffset by remember { mutableStateOf(0f) }
    
    Card(
        modifier = Modifier
            .width(280.dp)
            .padding(8.dp)
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragEnd = {
                        if (dragOffset > 50f) {
                            onSwipeRight()
                        } else if (dragOffset < -50f) {
                            onSwipeLeft()
                        }
                        dragOffset = 0f
                    },
                    onHorizontalDrag = { change, dragAmount ->
                        change.consume()
                        dragOffset += dragAmount
                    }
                )
            },
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 左箭头
                IconButton(
                    onClick = onSwipeRight,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowLeft,
                        contentDescription = "上一个",
                        tint = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(24.dp)
                    )
                }
                
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = presetName,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        textAlign = TextAlign.Center
                    )
                    // 页码指示器
                    Text(
                        text = "${presetIndex + 1}/$totalPresets",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.5f)
                    )
                }
                
                // 右箭头
                IconButton(
                    onClick = onSwipeLeft,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowRight,
                        contentDescription = "下一个",
                        tint = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(24.dp)
                    )
                }
                
                // 设置按钮
                IconButton(
                    onClick = onOpenPermissionSettings,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "权限设置",
                        tint = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(18.dp)
                    )
                }
                
                // 关闭按钮
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

            // 滑动提示
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.SwipeLeft,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.3f),
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = " 左右滑动切换预设 ",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.3f)
                )
                Icon(
                    imageVector = Icons.Default.SwipeRight,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.3f),
                    modifier = Modifier.size(16.dp)
                )
            }

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

            Button(
                onClick = onCopyParams,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AccentPrimary
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

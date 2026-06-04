package com.omaster.app.floating

import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.view.*
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
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.ViewTreeSavedStateRegistryOwner
import com.omaster.app.ui.theme.AccentPrimary
import com.omaster.app.ui.theme.DeepSpace
import com.omaster.app.ui.theme.HasselbladOrange
import timber.log.Timber
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

/**
 * 自定义 LifecycleOwner 和 SavedStateRegistryOwner 用于悬浮窗
 */
private class FloatingLifecycleOwner : LifecycleOwner, androidx.savedstate.SavedStateRegistryOwner {
    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    
    override val lifecycle: Lifecycle
        get() = lifecycleRegistry
    
    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryController.savedStateRegistry
    
    fun onCreate() {
        savedStateRegistryController.performAttach()
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.currentState = Lifecycle.State.CREATED
    }
    
    fun onStart() {
        lifecycleRegistry.currentState = Lifecycle.State.STARTED
    }
    
    fun onResume() {
        lifecycleRegistry.currentState = Lifecycle.State.RESUMED
    }
    
    fun onPause() {
        lifecycleRegistry.currentState = Lifecycle.State.STARTED
    }
    
    fun onStop() {
        lifecycleRegistry.currentState = Lifecycle.State.CREATED
    }
    
    fun onDestroy() {
        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
    }
}

object FloatingWindowManager {
    private val windowManagerRef = AtomicReference<WindowManager?>(null)
    private val floatingViewRef = AtomicReference<ComposeView?>(null)
    private val isShowing = AtomicBoolean(false)
    private val lifecycleOwnerRef = AtomicReference<FloatingLifecycleOwner?>(null)

    private val stateLock = Any()
    
    // 使用 AtomicReference 保证线程安全
    private val currentPresetNameRef = AtomicReference("预设参数")
    private val currentParamsRef = AtomicReference<Map<String, String>>(emptyMap())

    fun setPresetData(name: String, params: Map<String, String>) {
        synchronized(stateLock) {
            currentPresetNameRef.set(name)
            currentParamsRef.set(params.toMap()) // 创建不可变副本
        }
        if (isShowing.get()) {
            updateFloatingView()
        }
    }

    fun showWindow(context: Context) {
        if (!isShowing.compareAndSet(false, true)) return

        if (!canDrawOverlays(context)) {
            isShowing.set(false)
            requestOverlayPermission(context)
            return
        }

        try {
            val appContext = context.applicationContext
            windowManagerRef.set(appContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager)
            
            // 创建并初始化 LifecycleOwner
            lifecycleOwnerRef.set(FloatingLifecycleOwner().apply {
                onCreate()
                onStart()
                onResume()
            })
            
            floatingViewRef.set(ComposeView(appContext).apply {
                // 设置 LifecycleOwner
                ViewTreeLifecycleOwner.set(this, lifecycleOwnerRef.get())
                
                // 设置 SavedStateRegistryOwner (lifecycleOwner 已实现该接口)
                ViewTreeSavedStateRegistryOwner.set(this, lifecycleOwnerRef.get())
                
                setContent {
                    FloatingWindowContent(
                        presetName = currentPresetNameRef.get(),
                        params = currentParamsRef.get(),
                        onClose = { hideWindow() },
                        onCopyParams = { copyParamsToClipboard(appContext) }
                    )
                }
            })

            val params = getWindowParams()
            windowManagerRef.get()?.addView(floatingViewRef.get(), params)
            Timber.d("Floating window shown")
        } catch (e: Exception) {
            Timber.e(e, "Failed to show floating window")
            isShowing.set(false)
        }
    }

    fun hideWindow() {
        if (!isShowing.compareAndSet(true, false)) return

        try {
            floatingViewRef.get()?.let {
                windowManagerRef.get()?.removeView(it)
            }
            // 清理 LifecycleOwner
            lifecycleOwnerRef.get()?.apply {
                onPause()
                onStop()
                onDestroy()
            }
            lifecycleOwnerRef.set(null)
            floatingViewRef.set(null)
            Timber.d("Floating window hidden")
        } catch (e: Exception) {
            Timber.e(e, "Failed to hide floating window")
            isShowing.set(true)
        }
    }

    fun toggleWindow(context: Context) {
        if (isShowing.get()) {
            hideWindow()
        } else {
            showWindow(context)
        }
    }

    private fun updateFloatingView() {
        floatingViewRef.get()?.setContent {
            FloatingWindowContent(
                presetName = currentPresetNameRef.get(),
                params = currentParamsRef.get(),
                onClose = { hideWindow() },
                onCopyParams = { copyParamsToClipboard(floatingViewRef.get()?.context) }
            )
        }
    }

    private fun copyParamsToClipboard(context: Context?) {
        context ?: return
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
        val text = currentParamsRef.get().entries.joinToString("\n") { "${it.key}: ${it.value}" }
        val clip = android.content.ClipData.newPlainText("参数", text)
        clipboard.setPrimaryClip(clip)
    }

    private fun getWindowParams(): WindowManager.LayoutParams {
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}"))
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }
    }

    fun clearPresetData() {
        synchronized(stateLock) {
            currentPresetNameRef.set("预设参数")
            currentParamsRef.set(emptyMap())
        }
    }

    /**
     * 清理所有引用，防止内存泄漏
     */
    fun cleanup() {
        hideWindow()
        windowManagerRef.set(null)
        synchronized(stateLock) {
            currentPresetNameRef.set("预设参数")
            currentParamsRef.set(emptyMap())
        }
        Timber.d("FloatingWindowManager cleaned up")
    }
}

@Composable
fun FloatingWindowContent(
    presetName: String,
    params: Map<String, String>,
    onClose: () -> Unit,
    onCopyParams: () -> Unit
) {
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
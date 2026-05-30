package com.omaster.app.floating

import android.content.Context
import android.graphics.PixelFormat
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.*
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.omaster.app.ui.theme.AccentPrimary
import com.omaster.app.ui.theme.DeepSpace
import com.omaster.app.ui.theme.HasselbladOrange
import timber.log.Timber
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

object FloatingWindowManager {
    private val mainHandler = Handler(Looper.getMainLooper())
    private var windowManager: WindowManager? = null
    private var floatingView: ComposeView? = null
    private val isShowing = AtomicBoolean(false)

    private val currentPresetName = AtomicReference<String>("预设参数")
    private val currentParams = AtomicReference<Map<String, String>>(emptyMap())

    fun setPresetData(name: String, params: Map<String, String>) {
        currentPresetName.set(name)
        currentParams.set(HashMap(params))
        if (isShowing.get()) {
            updateFloatingView()
        }
    }

    fun showWindow(context: Context) {
        if (isShowing.get()) {
            return
        }

        if (!canDrawOverlays(context)) {
            requestOverlayPermission(context)
            return
        }

        mainHandler.post {
            try {
                if (isShowing.get()) {
                    return@post
                }

                windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
                floatingView = ComposeView(context).apply {
                    setupLifecycle(this)
                    setContent {
                        FloatingWindowContent(
                            presetName = currentPresetName.get(),
                            params = currentParams.get(),
                            onClose = { hideWindow() },
                            onCopyParams = { copyParamsToClipboard(context) }
                        )
                    }
                }

                val params = getWindowParams()
                windowManager?.addView(floatingView, params)
                isShowing.set(true)
                Timber.d("Floating window shown")
            } catch (e: Exception) {
                Timber.e(e, "Failed to show floating window")
                isShowing.set(false)
            }
        }
    }

    fun hideWindow() {
        if (!isShowing.compareAndSet(true, false)) {
            return
        }

        mainHandler.post {
            try {
                floatingView?.let {
                    windowManager?.removeView(it)
                }
                floatingView = null
                Timber.d("Floating window hidden")
            } catch (e: Exception) {
                Timber.e(e, "Failed to hide floating window")
            }
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
        mainHandler.post {
            floatingView?.setContent {
                FloatingWindowContent(
                    presetName = currentPresetName.get(),
                    params = currentParams.get(),
                    onClose = { hideWindow() },
                    onCopyParams = { copyParamsToClipboard(floatingView?.context) }
                )
            }
        }
    }

    private fun setupLifecycle(composeView: ComposeView) {
        val lifecycleOwner = FloatingWindowLifecycleOwner()
        composeView.setViewTreeLifecycleOwner(lifecycleOwner)
        composeView.setViewTreeSavedStateRegistryOwner(lifecycleOwner)
        lifecycleOwner.performCreate()
        lifecycleOwner.performStart()
    }

    private fun copyParamsToClipboard(context: Context?) {
        context ?: return
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
        val params = currentParams.get()
        val text = params.entries.joinToString("\n") { "${it.key}: ${it.value}" }
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
}

class FloatingWindowLifecycleOwner : LifecycleOwner, SavedStateRegistryOwner {
    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)

    init {
        savedStateRegistryController.performRestore(null)
    }

    override val lifecycle: Lifecycle = lifecycleRegistry
    override val savedStateRegistry: SavedStateRegistry = savedStateRegistryController.savedStateRegistry

    fun performCreate() {
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.currentState = Lifecycle.State.CREATED
    }

    fun performStart() {
        lifecycleRegistry.currentState = Lifecycle.State.STARTED
    }

    fun performResume() {
        lifecycleRegistry.currentState = Lifecycle.State.RESUMED
    }

    fun performDestroy() {
        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
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

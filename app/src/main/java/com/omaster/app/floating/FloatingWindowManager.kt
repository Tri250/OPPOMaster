package com.omaster.app.floating

import android.content.Context
import android.graphics.PixelFormat
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.view.*
import android.widget.LinearLayout
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
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.omaster.app.ui.theme.AccentPrimary
import com.omaster.app.ui.theme.DeepSpace
import com.omaster.app.ui.theme.HasselbladOrange
import timber.log.Timber

object FloatingWindowManager {
    private var windowManager: WindowManager? = null
    private var floatingView: ComposeView? = null
    private var isShowing = false

    private var currentPresetName: String = "预设参数"
    private var currentParams: Map<String, String> = emptyMap()

    fun setPresetData(name: String, params: Map<String, String>) {
        currentPresetName = name
        currentParams = params
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
            windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
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
            Timber.d("Floating window hidden")
        } catch (e: Exception) {
            Timber.e(e, "Failed to hide floating window")
        }
    }

    fun toggleWindow(context: Context) {
        if (isShowing) {
            hideWindow()
        } else {
            showWindow(context)
        }
    }

    private fun updateFloatingView() {
        floatingView?.setContent {
            FloatingWindowContent(
                presetName = currentPresetName,
                params = currentParams,
                onClose = { hideWindow() },
                onCopyParams = { copyParamsToClipboard(floatingView?.context) }
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}"))
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }
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

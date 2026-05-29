package com.omaster.app.accessibility

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import com.omaster.app.R
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ClipboardFallbackManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

    fun copyParamsToClipboard(params: CameraParams): Boolean {
        return try {
            val paramList = mutableListOf<String>()
            
            params.iso?.let { iso ->
                paramList.add("ISO: $iso")
            }
            params.shutter?.let { shutter ->
                paramList.add("快门: $shutter")
            }
            params.ev?.let { ev ->
                paramList.add("曝光: $ev")
            }
            params.wb?.let { wb ->
                paramList.add("白平衡: $wb")
            }
            
            if (paramList.isEmpty()) {
                Timber.w("No params to copy")
                return false
            }
            
            val clipboardText = paramList.joinToString("\n")
            val clip = ClipData.newPlainText("camera_params", clipboardText)
            clipboardManager.setPrimaryClip(clip)
            
            Toast.makeText(
                context,
                "参数已复制到剪贴板，请手动粘贴到相机应用",
                Toast.LENGTH_LONG
            ).show()
            
            Timber.d("Params copied to clipboard: $clipboardText")
            true
        } catch (e: Exception) {
            Timber.e(e, "Failed to copy params to clipboard")
            Toast.makeText(
                context,
                "复制失败，请手动输入参数",
                Toast.LENGTH_SHORT
            ).show()
            false
        }
    }

    fun copySingleParam(paramName: String, value: String): Boolean {
        return try {
            val displayText = "$paramName: $value"
            val clip = ClipData.newPlainText("camera_param", displayText)
            clipboardManager.setPrimaryClip(clip)
            
            Toast.makeText(
                context,
                "已复制: $displayText",
                Toast.LENGTH_SHORT
            ).show()
            
            Timber.d("Single param copied: $displayText")
            true
        } catch (e: Exception) {
            Timber.e(e, "Failed to copy single param")
            false
        }
    }

    fun clearClipboard() {
        try {
            val clip = ClipData.newPlainText("", "")
            clipboardManager.setPrimaryClip(clip)
            Timber.d("Clipboard cleared")
        } catch (e: Exception) {
            Timber.e(e, "Failed to clear clipboard")
        }
    }
}

class FallbackStrategyManager(
    private val context: Context,
    private val clipboardManager: ClipboardFallbackManager
) {
    enum class FallbackLevel {
        LEVEL_1_AUTO_FILL,
        LEVEL_2_COORDINATE_CLICK,
        LEVEL_3_CLIPBOARD
    }

    private var currentLevel = FallbackLevel.LEVEL_1_AUTO_FILL

    fun setFallbackLevel(level: FallbackLevel) {
        currentLevel = level
        Timber.d("Fallback level set to: $level")
    }

    fun getCurrentLevel(): FallbackLevel = currentLevel

    fun executeFallback(params: CameraParams): Boolean {
        return when (currentLevel) {
            FallbackLevel.LEVEL_1_AUTO_FILL -> {
                Timber.d("Attempting Level 1: Auto-fill")
                false
            }
            FallbackLevel.LEVEL_2_COORDINATE_CLICK -> {
                Timber.d("Attempting Level 2: Coordinate click")
                executeCoordinateClick(params)
            }
            FallbackLevel.LEVEL_3_CLIPBOARD -> {
                Timber.d("Attempting Level 3: Clipboard")
                clipboardManager.copyParamsToClipboard(params)
            }
        }
    }

    private fun executeCoordinateClick(params: CameraParams): Boolean {
        try {
            Toast.makeText(
                context,
                "正在尝试坐标点击填充...",
                Toast.LENGTH_SHORT
            ).show()
            return true
        } catch (e: Exception) {
            Timber.e(e, "Coordinate click failed")
            return clipboardManager.copyParamsToClipboard(params)
        }
    }

    fun degradeFallback(): FallbackLevel {
        currentLevel = when (currentLevel) {
            FallbackLevel.LEVEL_1_AUTO_FILL -> FallbackLevel.LEVEL_2_COORDINATE_CLICK
            FallbackLevel.LEVEL_2_COORDINATE_CLICK -> FallbackLevel.LEVEL_3_CLIPBOARD
            FallbackLevel.LEVEL_3_CLIPBOARD -> FallbackLevel.LEVEL_3_CLIPBOARD
        }
        Timber.d("Fallback degraded to: $currentLevel")
        return currentLevel
    }

    fun getUserSteps(): Int {
        return when (currentLevel) {
            FallbackLevel.LEVEL_1_AUTO_FILL -> 1
            FallbackLevel.LEVEL_2_COORDINATE_CLICK -> 2
            FallbackLevel.LEVEL_3_CLIPBOARD -> 3
        }
    }
}

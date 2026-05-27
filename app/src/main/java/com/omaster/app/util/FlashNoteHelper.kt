package com.omaster.app.util

import android.content.Context
import android.view.View
import android.widget.Toast
import com.google.android.material.snackbar.Snackbar
import com.omaster.app.model.FlashNoteResult
import com.omaster.app.model.Preset
import com.omaster.app.service.OneTapFlashNoteService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

object FlashNoteHelper {

    private var flashNoteService: OneTapFlashNoteService? = null

    fun init(context: Context) {
        if (flashNoteService == null) {
            flashNoteService = OneTapFlashNoteService(context)
        }
    }

    fun quickSavePreset(
        context: Context,
        preset: Preset,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        init(context)

        CoroutineScope(Dispatchers.Main).launch {
            val result = flashNoteService?.quickSavePreset(preset)

            result?.let {
                if (it.success) {
                    showSuccessToast(context, preset.name)
                    onSuccess()
                } else {
                    showErrorSnackbar(context, View.VISIBLE, it.errorMessage ?: "保存失败")
                    onError(it.errorMessage ?: "未知错误")
                }
            } ?: run {
                showErrorSnackbar(context, View.VISIBLE, "服务未初始化")
                onError("服务未初始化")
            }
        }
    }

    fun savePresetWithDialog(
        context: Context,
        preset: Preset,
        includeCover: Boolean = true,
        includeParams: Boolean = true,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        init(context)

        CoroutineScope(Dispatchers.Main).launch {
            try {
                val service = flashNoteService ?: throw IllegalStateException("Service not initialized")

                val flashNoteData = preset.toFlashNoteData(
                    includeCover = includeCover,
                    includeParams = includeParams
                )

                val result = service.saveToFlashNote(flashNoteData)

                if (result.success) {
                    showSuccessToast(context, preset.name)
                    onSuccess()
                } else {
                    showErrorSnackbar(context, View.VISIBLE, result.errorMessage ?: "保存失败")
                    onError(result.errorMessage ?: "未知错误")
                }
            } catch (e: Exception) {
                showErrorSnackbar(context, View.VISIBLE, e.message ?: "保存失败")
                onError(e.message ?: "未知错误")
            }
        }
    }

    fun batchSavePresets(
        context: Context,
        presets: List<Preset>,
        onComplete: (List<FlashNoteResult>) -> Unit = {}
    ) {
        init(context)

        CoroutineScope(Dispatchers.Main).launch {
            try {
                val service = flashNoteService ?: throw IllegalStateException("Service not initialized")
                val results = service.batchSavePresets(presets)

                val successCount = results.count { it.success }
                val failCount = results.size - successCount

                if (failCount == 0) {
                    showSuccessToast(context, "已保存 ${successCount} 个预设到闪记")
                } else {
                    showWarningSnackbar(
                        context,
                        View.VISIBLE,
                        "已保存 ${successCount} 个，失败 ${failCount} 个"
                    )
                }

                onComplete(results)
            } catch (e: Exception) {
                showErrorSnackbar(context, View.VISIBLE, e.message ?: "批量保存失败")
                onComplete(emptyList())
            }
        }
    }

    fun isServiceAvailable(): Boolean {
        return flashNoteService?.isFlashNoteSupported() ?: false
    }

    fun getSupportedFeatures(): List<String> {
        return flashNoteService?.getSupportedFeatures() ?: emptyList()
    }

    private fun showSuccessToast(context: Context, presetName: String) {
        Toast.makeText(
            context,
            "✅ 已保存到闪记: $presetName",
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun showErrorSnackbar(view: View, visibility: Int, message: String) {
        if (visibility == View.VISIBLE) {
            Snackbar.make(view, "❌ $message", Snackbar.LENGTH_LONG)
                .setAction("重试") {
                    view.performClick()
                }
                .show()
        }
    }

    private fun showWarningSnackbar(view: View, visibility: Int, message: String) {
        if (visibility == View.VISIBLE) {
            Snackbar.make(view, "⚠️ $message", Snackbar.LENGTH_LONG).show()
        }
    }
}

fun Preset.toFlashNoteContent(includeCover: Boolean = true, includeParams: Boolean = true): String {
    return toFlashNoteData(includeCover, includeParams).content
}

fun Preset.quickSaveToFlashNote(context: Context, onSuccess: () -> Unit = {}) {
    FlashNoteHelper.quickSavePreset(context, this, onSuccess)
}

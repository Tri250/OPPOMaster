package com.omaster.app.service

import android.content.ComponentName
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.omaster.app.config.FlashNoteConstants
import com.omaster.app.model.FlashNoteData
import com.omaster.app.model.FlashNoteResult
import com.omaster.app.model.Preset
import com.omaster.app.model.QuickNoteRequest
import com.omaster.app.model.toFlashNoteData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

class OneTapFlashNoteService(private val context: Context) {

    private val flashNoteComponent: ComponentName? by lazy {
        try {
            ComponentName(
                "com.coloros.flashnote",
                "com.coloros.flashnote.ui.AddNoteActivity"
            )
        } catch (e: Exception) {
            Timber.e(e, "Failed to get FlashNote component")
            null
        }
    }

    suspend fun savePreset(preset: Preset, includeCover: Boolean = true): FlashNoteResult {
        return withContext(Dispatchers.IO) {
            try {
                val flashNoteData = preset.toFlashNoteData(
                    includeCover = includeCover,
                    includeParams = true
                )
                saveToFlashNote(flashNoteData)
            } catch (e: Exception) {
                Timber.e(e, "Failed to save preset to flash note")
                FlashNoteResult(
                    success = false,
                    errorMessage = "保存失败: ${e.message}"
                )
            }
        }
    }

    suspend fun savePresetWithCustomTags(
        preset: Preset,
        tags: List<String>,
        includeCover: Boolean = true
    ): FlashNoteResult {
        return withContext(Dispatchers.IO) {
            try {
                val flashNoteData = preset.toFlashNoteData(
                    includeCover = includeCover,
                    customTags = tags
                )
                saveToFlashNote(flashNoteData)
            } catch (e: Exception) {
                Timber.e(e, "Failed to save preset with custom tags")
                FlashNoteResult(
                    success = false,
                    errorMessage = "保存失败: ${e.message}"
                )
            }
        }
    }

    suspend fun quickSavePreset(preset: Preset): FlashNoteResult {
        return withContext(Dispatchers.IO) {
            try {
                if (!FlashNoteConstants.isFlashNoteAvailable()) {
                    return@withContext FlashNoteResult(
                        success = false,
                        errorMessage = "ColorOS版本不支持此功能"
                    )
                }

                val intent = createQuickSaveIntent(preset)
                context.startActivity(intent)

                FlashNoteResult(
                    success = true,
                    noteId = preset.id,
                    timestamp = System.currentTimeMillis()
                )
            } catch (e: Exception) {
                Timber.e(e, "Failed to quick save preset")
                FlashNoteResult(
                    success = false,
                    errorMessage = "快速保存失败: ${e.message}"
                )
            }
        }
    }

    suspend fun saveCameraParams(params: com.omaster.app.model.CameraParams): FlashNoteResult {
        return withContext(Dispatchers.IO) {
            try {
                val content = params.toFlashNoteContent()
                val flashNoteData = FlashNoteData(
                    title = "📸 相机参数配置",
                    content = content,
                    category = FlashNoteConstants.CATEGORY_CAMERA_PARAMS,
                    tags = listOf(
                        FlashNoteConstants.TAG_CAMERA,
                        FlashNoteConstants.TAG_PRESET
                    ),
                    source = FlashNoteConstants.SOURCE_OPPO_MASTER
                )
                saveToFlashNote(flashNoteData)
            } catch (e: Exception) {
                Timber.e(e, "Failed to save camera params")
                FlashNoteResult(
                    success = false,
                    errorMessage = "保存失败: ${e.message}"
                )
            }
        }
    }

    suspend fun saveToFlashNote(data: FlashNoteData): FlashNoteResult {
        return withContext(Dispatchers.IO) {
            try {
                val intent = createFlashNoteIntent(data)
                context.startActivity(intent)

                FlashNoteResult(
                    success = true,
                    noteId = data.metadata?.presetId,
                    timestamp = data.timestamp
                )
            } catch (e: Exception) {
                Timber.e(e, "Failed to save to flash note")
                saveViaBroadcast(data)
            }
        }
    }

    private fun createFlashNoteIntent(data: FlashNoteData): Intent {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, data.title)
            putExtra(Intent.EXTRA_TEXT, data.content)

            data.tags.forEachIndexed { index, tag ->
                putExtra("${Intent.EXTRA_TAGS}_$index", tag)
            }

            data.attachmentUri?.let { uri ->
                putExtra(Intent.EXTRA_STREAM, uri)
                type = "image/*"
            }

            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }

        flashNoteComponent?.let { component ->
            intent.component = component
        }

        return intent
    }

    private fun createQuickSaveIntent(preset: Preset): Intent {
        return Intent().apply {
            action = FlashNoteConstants.ACTION_FLASH_NOTE_V2
            putExtra(FlashNoteConstants.EXTRA_TITLE, "🎨 ${preset.name}")
            putExtra(
                FlashNoteConstants.EXTRA_CONTENT,
                buildPresetContent(preset)
            )
            putExtra(
                FlashNoteConstants.EXTRA_TAGS,
                arrayOf(
                    FlashNoteConstants.TAG_PRESET,
                    FlashNoteConstants.TAG_CAMERA,
                    FlashNoteConstants.TAG_PHOTO
                ) + preset.cameraParams?.sceneTags?.toTypedArray().orEmpty()
            )
            putExtra(FlashNoteConstants.EXTRA_CATEGORY, FlashNoteConstants.CATEGORY_PRESET)
            putExtra(FlashNoteConstants.EXTRA_SOURCE, FlashNoteConstants.SOURCE_OPPO_MASTER)
            putExtra(FlashNoteConstants.EXTRA_TIMESTAMP, System.currentTimeMillis())

            if (preset.coverPath.isNotEmpty()) {
                putExtra(
                    FlashNoteConstants.EXTRA_ATTACHMENT_URI,
                    Uri.parse(preset.coverPath)
                )
                putExtra(
                    FlashNoteConstants.EXTRA_ATTACHMENT_TYPE,
                    FlashNoteConstants.ATTACHMENT_TYPE_IMAGE
                )
            }

            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
    }

    private fun buildPresetContent(preset: Preset): String {
        val params = preset.cameraParams ?: return ""

        return buildString {
            append("📷 ${preset.name}\n\n")
            append("📸 相机参数\n")
            append("━━━━━━━━━━━━━━━━\n")
            append("• ISO: ${params.iso}\n")
            append("• 快门: ${params.shutter}\n")
            append("• 曝光补偿: ${params.ev}\n")
            append("• 白平衡: ${params.wb}\n")
            append("• 对比度: ${params.contrast}\n")
            append("• 饱和度: ${params.saturation}\n")
            append("• 暗角: ${params.vignette}\n\n")

            if (params.sceneTags.isNotEmpty()) {
                append("🏷️ 场景: ${params.sceneTags.joinToString(", ")}\n\n")
            }

            append("━━━━━━━━━━━━━━━━\n")
            append("设备: ${preset.deviceModel}\n")
            append("评分: ⭐ ${preset.rating}\n")
            append("使用次数: ${preset.usageCount}次\n")
        }
    }

    private suspend fun saveViaBroadcast(data: FlashNoteData): FlashNoteResult {
        return withContext(Dispatchers.IO) {
            try {
                val values = ContentValues().apply {
                    put("title", data.title)
                    put("content", data.content)
                    put("category", data.category)
                    put("source", data.source)
                    put("create_time", data.timestamp)
                    put("modify_time", data.timestamp)

                    data.attachmentUri?.let { uri ->
                        put("attachment_uri", uri.toString())
                        put("attachment_type", data.attachmentType)
                    }
                }

                context.contentResolver.insert(
                    Uri.parse("content://com.coloros.flashnote.provider/notes"),
                    values
                )

                FlashNoteResult(
                    success = true,
                    timestamp = System.currentTimeMillis()
                )
            } catch (e: Exception) {
                Timber.e(e, "Failed to save via broadcast")
                FlashNoteResult(
                    success = false,
                    errorMessage = "保存失败: ${e.message}"
                )
            }
        }
    }

    fun isFlashNoteSupported(): Boolean {
        return FlashNoteConstants.isFlashNoteAvailable() && flashNoteComponent != null
    }

    fun getSupportedFeatures(): List<String> {
        return if (FlashNoteConstants.isFullFeatureAvailable()) {
            FlashNoteConstants.ENABLED_FEATURES
        } else {
            listOf(
                FlashNoteConstants.FEATURE_PRESET_SAVE,
                FlashNoteConstants.FEATURE_CAMERA_PARAMS_SAVE
            )
        }
    }

    suspend fun batchSavePresets(presets: List<Preset>): List<FlashNoteResult> {
        return withContext(Dispatchers.IO) {
            presets.map { preset ->
                try {
                    quickSavePreset(preset)
                } catch (e: Exception) {
                    FlashNoteResult(
                        success = false,
                        errorMessage = "批量保存失败: ${e.message}"
                    )
                }
            }
        }
    }
}

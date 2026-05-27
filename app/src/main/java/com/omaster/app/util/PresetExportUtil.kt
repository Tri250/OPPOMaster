package com.omaster.app.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import com.omaster.app.model.CameraParams
import com.omaster.app.model.Preset
import com.omaster.app.model.Section
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PresetExportUtil {
    
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
    
    fun exportPreset(context: Context, preset: Preset): Intent? {
        return try {
            val jsonString = json.encodeToString(preset)
            val fileName = generateFileName(preset.name)
            val file = createTempFile(context, fileName, jsonString)
            
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            
            createShareIntent(uri, fileName)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "导出失败: ${e.message}", Toast.LENGTH_SHORT).show()
            null
        }
    }
    
    fun exportAllPresets(context: Context, presets: List<Preset>): Intent? {
        return try {
            val exportData = ExportData(
                version = 1,
                exportTime = System.currentTimeMillis(),
                presets = presets
            )
            val jsonString = json.encodeToString(exportData)
            val fileName = "omaster_backup_${getTimestamp()}.json"
            val file = createTempFile(context, fileName, jsonString)
            
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            
            createShareIntent(uri, fileName)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "导出失败: ${e.message}", Toast.LENGTH_SHORT).show()
            null
        }
    }
    
    fun importPreset(context: Context, uri: Uri): Preset? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val jsonString = inputStream?.bufferedReader()?.readText() ?: return null
            inputStream.close()
            
            json.decodeFromString<Preset>(jsonString)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "导入失败: ${e.message}", Toast.LENGTH_SHORT).show()
            null
        }
    }
    
    fun importPresets(context: Context, uri: Uri): List<Preset>? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val jsonString = inputStream?.bufferedReader()?.readText() ?: return null
            inputStream.close()
            
            try {
                val exportData = json.decodeFromString<ExportData>(jsonString)
                exportData.presets
            } catch (e: Exception) {
                val singlePreset = json.decodeFromString<Preset>(jsonString)
                listOf(singlePreset)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "导入失败: ${e.message}", Toast.LENGTH_SHORT).show()
            null
        }
    }
    
    fun validatePresetJson(jsonString: String): Boolean {
        return try {
            json.decodeFromString<Preset>(jsonString)
            true
        } catch (e: Exception) {
            false
        }
    }
    
    private fun generateFileName(presetName: String): String {
        val sanitizedName = presetName
            .replace(Regex("[^a-zA-Z0-9\\u4e00-\\u9fa5]"), "_")
            .take(50)
        val timestamp = getTimestamp()
        return "omaster_preset_${sanitizedName}_$timestamp.omaster"
    }
    
    private fun getTimestamp(): String {
        val sdf = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
        return sdf.format(Date())
    }
    
    private fun createTempFile(context: Context, fileName: String, content: String): File {
        val cacheDir = File(context.cacheDir, "exports")
        if (!cacheDir.exists()) {
            cacheDir.mkdirs()
        }
        val file = File(cacheDir, fileName)
        file.writeText(content)
        return file
    }
    
    private fun createShareIntent(uri: Uri, fileName: String): Intent {
        return Intent(Intent.ACTION_SEND).apply {
            type = "application/json"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "OMaster 预设分享")
            putExtra(Intent.EXTRA_TEXT, "分享来自 OMaster 的摄影预设")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
}

@kotlinx.serialization.Serializable
data class ExportData(
    val version: Int,
    val exportTime: Long,
    val presets: List<Preset>
)

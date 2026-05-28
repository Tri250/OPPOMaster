package com.omaster.app.utils

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import com.omaster.app.model.CameraParams
import com.omaster.app.model.Preset
import timber.log.Timber

object PresetUtils {
    
    fun copyPresetParamsToClipboard(context: Context, preset: Preset) {
        val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        
        val paramsText = buildString {
            appendLine("📷 ${preset.name}")
            appendLine()
            preset.cameraParams?.let { params ->
                appendLine("相机参数：")
                appendLine("• 模式: ${params.mode}")
                appendLine("• 滤镜: ${params.filter ?: "标准"}")
                params.iso?.let { appendLine("• ISO: $it") }
                params.shutter?.let { appendLine("• 快门: $it") }
                params.ev?.let { appendLine("• 曝光补偿: $it") }
                params.wb?.let { appendLine("• 白平衡: $it") }
                if (params.hasselblad_hncs) {
                    appendLine("• 哈苏 HNCS: ✓")
                }
            }
            appendLine()
            appendLine("——来自 OMaster")
        }
        
        val clip = ClipData.newPlainText("OMaster 预设参数", paramsText)
        clipboardManager.setPrimaryClip(clip)
        
        Timber.d("Preset params copied to clipboard: ${preset.name}")
    }
    
    fun sharePreset(context: Context, preset: Preset) {
        val shareText = buildString {
            appendLine("📷 ${preset.name}")
            appendLine()
            preset.sections.forEach { section ->
                appendLine("${section.title}:")
                appendLine("${section.content}")
                appendLine()
            }
            preset.cameraParams?.let { params ->
                appendLine("相机参数：")
                params.iso?.let { appendLine("ISO: $it") }
                params.shutter?.let { appendLine("快门: $it") }
                params.ev?.let { appendLine("曝光: $it") }
                params.wb?.let { appendLine("白平衡: $it") }
                appendLine()
            }
            appendLine("适配机型: ${preset.deviceModel ?: "通用"}")
            appendLine()
            appendLine("使用 OMaster 一键套用预设参数 🎨")
        }
        
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, shareText)
            putExtra(Intent.EXTRA_SUBJECT, "分享预设: ${preset.name}")
        }
        
        val chooser = Intent.createChooser(shareIntent, "分享到")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
        
        Timber.d("Sharing preset: ${preset.name}")
    }
    
    fun generatePresetShareImage(context: Context, preset: Preset): Uri? {
        return null
    }
    
    fun formatCameraParams(params: CameraParams): String {
        return buildString {
            params.iso?.let { append("ISO $it ") }
            params.shutter?.let { append("Shutter $it ") }
            params.ev?.let { append("EV $it ") }
            params.wb?.let { append("WB $it ") }
            if (params.hasselblad_hncs) {
                append("HNCS")
            }
        }.trim()
    }
    
    fun getPresetCategories(): List<String> {
        return listOf(
            "全部",
            "收藏",
            "复古",
            "清新",
            "夜景",
            "自然",
            "人像",
            "暖调",
            "黑白"
        )
    }
    
    fun filterPresetsByCategory(
        presets: List<Preset>,
        category: String
    ): List<Preset> {
        return when (category) {
            "全部" -> presets
            "收藏" -> presets.filter { it.isFavorite }
            else -> presets.filter { 
                it.cameraParams?.filter == category 
            }
        }
    }
}

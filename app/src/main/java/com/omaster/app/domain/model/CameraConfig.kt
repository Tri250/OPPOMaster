package com.omaster.app.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 相机配置文件模型
 * 支持创建、保存、应用、分享相机参数配置
 */
@Parcelize
data class CameraConfig(
    val id: String = generateId(),
    val name: String = "",
    val description: String = "",
    val params: CameraParams = CameraParams(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isFavorite: Boolean = false,
    val category: String = "默认",
    val tags: List<String> = emptyList()
) : Parcelable {

    companion object {
        private fun generateId(): String {
            val sdf = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
            return "config_${sdf.format(Date())}_${(0..9999).random().toString().padStart(4, '0')}"
        }

        /**
         * 创建示例配置
         */
        fun sampleConfigs(): List<CameraConfig> {
            return listOf(
                CameraConfig(
                    name = "哈苏大师人像",
                    description = "适合人像拍摄的哈苏自然色彩方案",
                    params = CameraParams(
                        iso = 100,
                        shutter = "1/200",
                        ev = "+0.0",
                        wb = "5200K",
                        portraitMode = true,
                        colorStyle = ColorStyle.Portrait.name
                    ),
                    category = "人像",
                    tags = listOf("人像", "哈苏", "自然色彩")
                ),
                CameraConfig(
                    name = "夜景大师",
                    description = "低光环境下的哈苏夜景方案",
                    params = CameraParams(
                        iso = 800,
                        shutter = "1/30",
                        ev = "+0.3",
                        wb = "Auto",
                        nightMode = true,
                        colorStyle = ColorStyle.Natural.name
                    ),
                    category = "夜景",
                    tags = listOf("夜景", "低光", "哈苏")
                ),
                CameraConfig(
                    name = "街拍模式",
                    description = "快速抓拍的街拍配置",
                    params = CameraParams(
                        iso = 200,
                        shutter = "1/500",
                        ev = "0",
                        wb = "Auto",
                        colorStyle = ColorStyle.Cinematic.name
                    ),
                    category = "街拍",
                    tags = listOf("街拍", "快速抓拍")
                ),
                CameraConfig(
                    name = "风景大片",
                    description = "风景摄影的哈苏配置",
                    params = CameraParams(
                        iso = 100,
                        shutter = "1/125",
                        ev = "-0.3",
                        wb = "5600K",
                        focalLength = "24mm",
                        colorStyle = ColorStyle.Natural.name
                    ),
                    category = "风景",
                    tags = listOf("风景", "广角")
                )
            )
        }

        /**
         * 从现有参数创建配置
         */
        fun fromParams(name: String, params: CameraParams, description: String = ""): CameraConfig {
            return CameraConfig(
                name = name,
                description = description,
                params = params
            )
        }
    }

    /**
     * 格式化创建时间
     */
    fun getFormattedDate(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        return sdf.format(Date(createdAt))
    }

    /**
     * 获取简要信息
     */
    fun getSummary(): String {
        return buildString {
            append("ISO ${params.iso}")
            append(" · ${params.shutter}")
            if (params.ev != "+0.0" && params.ev != "0") append(" · EV ${params.ev}")
        }
    }
}

/**
 * 配置文件导入导出格式
 */
data class CameraConfigExport(
    val version: String = "1.0",
    val appVersion: String = "OPPO Master 3.0",
    val exportTime: Long = System.currentTimeMillis(),
    val configs: List<CameraConfig>
) {
    companion object {
        const val FILE_EXTENSION = ".oppocam"
        const val MIME_TYPE = "application/octet-stream"
    }
}

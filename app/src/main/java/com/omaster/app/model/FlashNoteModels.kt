package com.omaster.app.model

import android.net.Uri
import com.omaster.app.config.FlashNoteConstants

data class FlashNoteData(
    val title: String,
    val content: String,
    val category: String = FlashNoteConstants.CATEGORY_PRESET,
    val tags: List<String> = emptyList(),
    val source: String = FlashNoteConstants.SOURCE_OPPO_MASTER,
    val attachmentUri: Uri? = null,
    val attachmentType: String = FlashNoteConstants.ATTACHMENT_TYPE_TEXT,
    val timestamp: Long = System.currentTimeMillis(),
    val priority: Int = FlashNoteConstants.DEFAULT_PRIORITY,
    val metadata: FlashNoteMetadata? = null
)

data class FlashNoteMetadata(
    val presetId: String? = null,
    val cameraParams: CameraParams? = null,
    val deviceModel: String? = null,
    val author: String? = null,
    val rating: Float? = null,
    val usageCount: Int? = null,
    val deviceInfo: DeviceInfo? = null
)

data class DeviceInfo(
    val brand: String = "OPPO",
    val model: String,
    val colorosVersion: String? = null,
    val cameraApp: String? = null
)

data class FlashNoteResult(
    val success: Boolean,
    val noteId: String? = null,
    val errorMessage: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

data class QuickNoteRequest(
    val preset: Preset,
    val includeCover: Boolean = true,
    val includeParams: Boolean = true,
    val customTags: List<String> = emptyList(),
    val priority: Int = FlashNoteConstants.DEFAULT_PRIORITY
)

fun Preset.toFlashNoteData(
    includeCover: Boolean = true,
    includeParams: Boolean = true,
    customTags: List<String> = emptyList()
): FlashNoteData {
    val params = this.cameraParams

    val contentBuilder = StringBuilder()

    contentBuilder.append("📷 ${this.name}\n\n")

    if (includeParams && params != null) {
        contentBuilder.append("📸 相机参数\n")
        contentBuilder.append("━━━━━━━━━━━━━━━━\n")
        contentBuilder.append("• ISO: ${params.iso}\n")
        contentBuilder.append("• 快门: ${params.shutter}\n")
        contentBuilder.append("• 曝光补偿: ${params.ev}\n")
        contentBuilder.append("• 白平衡: ${params.wb}\n")

        if (params.contrast != 1.0f) {
            contentBuilder.append("• 对比度: ${params.contrast}\n")
        }
        if (params.saturation != 1.0f) {
            contentBuilder.append("• 饱和度: ${params.saturation}\n")
        }
        if (params.vignette != 0.0f) {
            contentBuilder.append("• 暗角: ${params.vignette}\n")
        }

        if (params.sceneTags.isNotEmpty()) {
            contentBuilder.append("\n🏷️ 场景标签: ${params.sceneTags.joinToString(", ")}\n")
        }

        contentBuilder.append("\n")
    }

    contentBuilder.append("━━━━━━━━━━━━━━━━\n")
    contentBuilder.append("设备: ${this.deviceModel}\n")
    contentBuilder.append("评分: ⭐ ${this.rating}\n")
    contentBuilder.append("使用次数: ${this.usageCount}次\n")

    if (this.author.isNotEmpty()) {
        contentBuilder.append("作者: ${this.author}\n")
    }

    val tags = mutableListOf(
        FlashNoteConstants.TAG_PRESET,
        FlashNoteConstants.TAG_CAMERA,
        FlashNoteConstants.TAG_PHOTO
    ).apply {
        if (params != null) {
            addAll(params.sceneTags)
        }
        addAll(customTags)
    }.distinct()

    val metadata = FlashNoteMetadata(
        presetId = this.id,
        cameraParams = params,
        deviceModel = this.deviceModel,
        author = this.author,
        rating = this.rating,
        usageCount = this.usageCount,
        deviceInfo = DeviceInfo(model = this.deviceModel)
    )

    return FlashNoteData(
        title = "🎨 ${this.name}",
        content = contentBuilder.toString(),
        category = FlashNoteConstants.CATEGORY_PRESET,
        tags = tags,
        source = FlashNoteConstants.SOURCE_OPPO_MASTER,
        attachmentUri = if (includeCover && this.coverPath.isNotEmpty()) {
            Uri.parse(this.coverPath)
        } else null,
        attachmentType = if (includeCover) FlashNoteConstants.ATTACHMENT_TYPE_IMAGE
                        else FlashNoteConstants.ATTACHMENT_TYPE_TEXT,
        metadata = metadata
    )
}

fun CameraParams.toFlashNoteContent(): String {
    return buildString {
        append("📸 相机参数详情\n")
        append("━━━━━━━━━━━━━━━━\n")
        append("• ISO: $iso\n")
        append("• 快门: $shutter\n")
        append("• 曝光补偿: $ev\n")
        append("• 白平衡: $wb\n")
        append("• 对比度: $contrast\n")
        append("• 饱和度: $saturation\n")
        append("• 清晰度: $sharpness\n")
        append("• 暗角: $vignette\n")

        if (sceneTags.isNotEmpty()) {
            append("\n🏷️ 场景标签:\n")
            sceneTags.forEach { tag ->
                append("  • $tag\n")
            }
        }

        if (hasselblad_hncs) {
            append("\n📷 哈苏HNCS色彩优化已启用\n")
        }

        append("\n━━━━━━━━━━━━━━━━\n")
    }
}

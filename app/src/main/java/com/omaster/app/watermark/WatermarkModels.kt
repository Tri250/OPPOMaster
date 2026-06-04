package com.omaster.app.watermark

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import java.util.UUID

enum class WatermarkType {
    TEXT,
    IMAGE,
    TEMPLATE
}

data class Watermark(
    val id: String = UUID.randomUUID().toString(),
    val type: WatermarkType,
    val text: String = "",
    val imageUri: Uri? = null,
    val position: Offset = Offset(0.5f, 0.5f),
    val size: Size = Size(100f, 100f),
    val scale: Float = 1f,
    val rotation: Float = 0f,
    val opacity: Float = 1f,
    val zIndex: Int = 0,
    val textConfig: TextWatermarkConfig = TextWatermarkConfig(),
    val imageConfig: ImageWatermarkConfig = ImageWatermarkConfig(),
    val mixMode: BlendMode = BlendMode.NORMAL,
    val isSelected: Boolean = false
)

data class TextWatermarkConfig(
    val fontSize: Float = 24f,
    val fontColor: Color = Color.White,
    val fontWeight: FontWeight = FontWeight.Normal,
    val isBold: Boolean = false,
    val isItalic: Boolean = false,
    val isUnderline: Boolean = false,
    val isStrikethrough: Boolean = false,
    val hasStroke: Boolean = false,
    val strokeColor: Color = Color.Black,
    val strokeWidth: Float = 2f,
    val hasShadow: Boolean = false,
    val shadowColor: Color = Color.Black,
    val shadowBlurRadius: Float = 4f,
    val shadowOffset: Offset = Offset(2f, 2f),
    val alignment: TextAlignment = TextAlignment.CENTER,
    val lineSpacing: Float = 1.2f
)

data class ImageWatermarkConfig(
    val bitmap: Bitmap? = null,
    val preserveAspectRatio: Boolean = true,
    val cropRect: Rect? = null,
    val flipHorizontal: Boolean = false,
    val flipVertical: Boolean = false
)

enum class TextAlignment {
    LEFT,
    CENTER,
    RIGHT
}

enum class BlendMode {
    NORMAL,
    MULTIPLY,
    SCREEN,
    OVERLAY,
    DARKEN,
    LIGHTEN
}

data class WatermarkEditorState(
    val imageUri: Uri? = null,
    val watermarks: List<Watermark> = emptyList(),
    val selectedWatermarkId: String? = null,
    val isProcessing: Boolean = false,
    val history: List<WatermarkEditorState> = emptyList(),
    val historyIndex: Int = -1,
    val maxHistorySize: Int = 20
) {
    val selectedWatermark: Watermark?
        get() = watermarks.find { it.id == selectedWatermarkId }
}

data class WatermarkTemplateData(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val description: String = "",
    val watermarks: List<Watermark>,
    val thumbnail: Uri? = null,
    val isSystem: Boolean = false,
    val isCustom: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

data class ExportConfig(
    val format: ExportFormat = ExportFormat.JPEG,
    val quality: Int = 95,
    val resolution: ExportResolution = ExportResolution.ORIGINAL,
    val outputUri: Uri? = null
)

enum class ExportFormat {
    JPEG,
    PNG,
    WEBP
}

enum class ExportResolution(val width: Int? = null, val height: Int? = null) {
    ORIGINAL,
    HD_720(1280, 720),
    FHD_1080(1920, 1080),
    QHD_1440(2560, 1440),
    UHD_4K(3840, 2160)
}

data class BatchExportRequest(
    val sourceUris: List<Uri>,
    val template: WatermarkTemplateData,
    val config: ExportConfig
)

data class ExportProgress(
    val current: Int = 0,
    val total: Int = 1,
    val isProcessing: Boolean = false,
    val isCompleted: Boolean = false,
    val error: String? = null
) {
    val progress: Float
        get() = if (total > 0) current.toFloat() / total else 0f
}

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
    TEMPLATE,
    HASSELBLAD
}

data class Watermark(
    val id: String = UUID.randomUUID().toString(),
    val type: WatermarkType,
    var text: String = "",
    var imageUri: Uri? = null,
    var position: Offset = Offset(0.5f, 0.5f),
    var size: Size = Size(100f, 100f),
    var scale: Float = 1f,
    var rotation: Float = 0f,
    var opacity: Float = 1f,
    var zIndex: Int = 0,
    var textConfig: TextWatermarkConfig = TextWatermarkConfig(),
    var imageConfig: ImageWatermarkConfig = ImageWatermarkConfig(),
    var mixMode: BlendMode = BlendMode.NORMAL,
    var isSelected: Boolean = false
)

data class TextWatermarkConfig(
    var fontSize: Float = 24f,
    var fontColor: Color = Color.White,
    var fontWeight: FontWeight = FontWeight.Normal,
    var isBold: Boolean = false,
    var isItalic: Boolean = false,
    var isUnderline: Boolean = false,
    var isStrikethrough: Boolean = false,
    var hasStroke: Boolean = false,
    var strokeColor: Color = Color.Black,
    var strokeWidth: Float = 2f,
    var hasShadow: Boolean = false,
    var shadowColor: Color = Color.Black,
    var shadowBlurRadius: Float = 4f,
    var shadowOffset: Offset = Offset(2f, 2f),
    var alignment: TextAlignment = TextAlignment.CENTER,
    var lineSpacing: Float = 1.2f
)

data class ImageWatermarkConfig(
    var bitmap: Bitmap? = null,
    var preserveAspectRatio: Boolean = true,
    var cropRect: Rect? = null,
    var flipHorizontal: Boolean = false,
    var flipVertical: Boolean = false
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

data class WatermarkTemplate(
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
    val template: WatermarkTemplate,
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

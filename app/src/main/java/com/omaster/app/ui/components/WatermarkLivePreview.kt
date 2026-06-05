package com.omaster.app.ui.components

import android.graphics.Bitmap
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.omaster.app.ui.animation.ColorOSAnimationDuration
import com.omaster.app.ui.animation.ColorOSScale
import com.omaster.app.ui.theme.*
import com.omaster.app.watermark.*

/**
 * 水印实时预览增强组件 - P0-3 功能改善
 * 所见即所得 + 拖拽定位 + 模板缩略图预览
 */

/**
 * 水印预览模板数据 - 带视觉缩略图
 */
data class WatermarkPreviewTemplate(
    val id: String,
    val name: String,
    val description: String,
    val previewIcon: androidx.compose.ui.graphics.vector.ImageVector,
    val previewColor: Color,
    val watermarks: List<Watermark>
)

/**
 * 水印实时预览画布 - 增强版
 */
@Composable
fun WatermarkLivePreviewCanvas(
    imageBitmap: Bitmap?,
    watermarks: List<Watermark>,
    selectedWatermarkId: String?,
    onWatermarkSelect: (String?) -> Unit,
    onWatermarkMove: (String, Offset) -> Unit,
    onWatermarkTransform: (String, Float, Float) -> Unit,
    modifier: Modifier = Modifier
) {
    var canvasSize by remember { mutableStateOf(Size.Zero) }
    var canvasOffset by remember { mutableStateOf(Offset.Zero) }
    val textMeasurer = rememberTextMeasurer()
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Colors.Background)
    ) {
        if (imageBitmap == null) {
            // 空状态
            EmptyPreviewPlaceholder(
                modifier = Modifier.fillMaxSize()
            )
        } else {
            val bmpImage = remember(imageBitmap) { imageBitmap.asImageBitmap() }
            
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                val hit = findWatermarkAt(
                                    watermarks, offset, canvasSize, canvasOffset
                                )
                                onWatermarkSelect(hit?.id)
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                selectedWatermarkId?.let { id ->
                                    onWatermarkMove(id, Offset(
                                        dragAmount.x / canvasSize.width,
                                        dragAmount.y / canvasSize.height
                                    ))
                                }
                            },
                            onDragEnd = { onWatermarkSelect(selectedWatermarkId) }
                        )
                    }
                    .pointerInput(Unit) {
                        // 双指缩放/旋转
                        detectTransformGestures { _, pan, zoom, rotation ->
                            selectedWatermarkId?.let { id ->
                                onWatermarkTransform(id, zoom, rotation)
                            }
                        }
                    }
            ) {
                // 计算缩放比例
                val scaleFactor = minOf(
                    size.width / imageBitmap.width,
                    size.height / imageBitmap.height
                )
                val drawSize = Size(
                    imageBitmap.width * scaleFactor,
                    imageBitmap.height * scaleFactor
                )
                val drawOffset = Offset(
                    (size.width - drawSize.width) / 2f,
                    (size.height - drawSize.height) / 2f
                )
                
                canvasSize = drawSize
                canvasOffset = drawOffset
                
                // 绘制背景图片
                drawImage(
                    image = bmpImage,
                    dstSize = IntSize(drawSize.width.toInt(), drawSize.height.toInt()),
                    dstOffset = IntOffset(drawOffset.x.toInt(), drawOffset.y.toInt())
                )
                
                // 绘制所有水印
                watermarks.sortedBy { it.zIndex }.forEach { watermark ->
                    drawLivePreviewWatermark(
                        watermark = watermark,
                        canvasSize = drawSize,
                        canvasOffset = drawOffset,
                        isSelected = watermark.id == selectedWatermarkId,
                        textMeasurer = textMeasurer
                    )
                }
            }
            
            // 实时参数调整提示
            if (selectedWatermarkId != null) {
                LiveAdjustHint(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(Spacing.md)
                )
            }
        }
    }
}

private fun DrawScope.drawLivePreviewWatermark(
    watermark: Watermark,
    canvasSize: Size,
    canvasOffset: Offset,
    isSelected: Boolean,
    textMeasurer: TextMeasurer
) {
    val position = Offset(
        canvasOffset.x + watermark.position.x * canvasSize.width,
        canvasOffset.y + watermark.position.y * canvasSize.height
    )
    
    withTransform({
        translate(left = position.x, top = position.y)
        scale(watermark.scale, watermark.scale)
        rotate(watermark.rotation, pivot = Offset.Zero)
    }) {
        // 绘制水印内容
        when (watermark.type) {
            WatermarkType.TEXT -> {
                val config = watermark.textConfig
                val textStyle = TextStyle(
                    fontSize = config.fontSize.sp,
                    color = config.fontColor.copy(alpha = watermark.opacity),
                    fontWeight = if (config.isBold) FontWeight.Bold else FontWeight.Normal
                )
                val measured = textMeasurer.measure(watermark.text, style = textStyle)
                drawText(measured, topLeft = Offset(-measured.size.width / 2f, -measured.size.height / 2f))
            }
            WatermarkType.IMAGE -> {
                watermark.imageConfig.bitmap?.let { bmp ->
                    val img = bmp.asImageBitmap()
                    val sz = watermark.size
                    drawImage(
                        image = img,
                        dstSize = IntSize(sz.width.toInt(), sz.height.toInt()),
                        dstOffset = IntOffset(-sz.width.toInt() / 2, -sz.height.toInt() / 2),
                        alpha = watermark.opacity
                    )
                }
            }
            WatermarkType.TEMPLATE -> {}
        }
        
        // 选中状态框
        if (isSelected) {
            drawSelectionFrame(watermark.size)
        }
    }
}

private fun DrawScope.drawSelectionFrame(size: Size) {
    val padding = 20f
    val rect = androidx.compose.ui.geometry.Rect(
        left = -size.width / 2 - padding,
        top = -size.height / 2 - padding,
        right = size.width / 2 + padding,
        bottom = size.height / 2 + padding
    )
    
    // 边框
    drawRect(
        color = Colors.HasselbladOrange,
        style = Stroke(width = 3f),
        topLeft = rect.topLeft,
        size = rect.size
    )
    
    // 角标
    val handleSize = 16f
    listOf(
        Offset(rect.left, rect.top),
        Offset(rect.right, rect.top),
        Offset(rect.left, rect.bottom),
        Offset(rect.right, rect.bottom)
    ).forEach { handle ->
        drawRect(
            color = Colors.HasselbladOrange,
            topLeft = Offset(handle.x - handleSize / 2, handle.y - handleSize / 2),
            size = Size(handleSize, handleSize)
        )
    }
}

private fun findWatermarkAt(
    watermarks: List<Watermark>,
    touch: Offset,
    canvasSize: Size,
    canvasOffset: Offset
): Watermark? {
    if (canvasSize.width <= 0) return null
    for (wm in watermarks.sortedByDescending { it.zIndex }) {
        val pos = Offset(
            canvasOffset.x + wm.position.x * canvasSize.width,
            canvasOffset.y + wm.position.y * canvasSize.height
        )
        val halfW = wm.size.width / 2 * wm.scale
        val halfH = wm.size.height / 2 * wm.scale
        val bounds = androidx.compose.ui.geometry.Rect(
            left = pos.x - halfW - 20,
            top = pos.y - halfH - 20,
            right = pos.x + halfW + 20,
            bottom = pos.y + halfH + 20
        )
        if (bounds.contains(touch)) return wm
    }
    return null
}

@Composable
private fun EmptyPreviewPlaceholder(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
                .background(Colors.GlassBackground),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.AddPhotoAlternate,
                contentDescription = "添加图片",
                tint = Colors.HasselbladOrange,
                modifier = Modifier.size(48.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(Spacing.md))
        
        Text(
            text = "选择图片开始预览",
            style = Typography.TitleMedium,
            color = Colors.OnSurface
        )
    }
}

@Composable
private fun LiveAdjustHint(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(Radius.md),
        color = Colors.GlassBackground.copy(alpha = 0.9f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.TouchApp,
                contentDescription = null,
                tint = Colors.HasselbladOrange,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = "拖拽移动 · 双指缩放旋转",
                style = Typography.LabelSmall,
                color = Colors.OnSurface
            )
        }
    }
}

/**
 * 水印模板选择器 - 带视觉缩略图
 */
@Composable
fun WatermarkTemplateSelector(
    templates: List<WatermarkPreviewTemplate>,
    selectedTemplateId: String?,
    onTemplateSelect: (WatermarkPreviewTemplate) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = "水印模板",
            style = Typography.TitleSmall,
            fontWeight = FontWeight.Bold,
            color = Colors.OnSurface,
            modifier = Modifier.padding(horizontal = Spacing.md)
        )
        
        Spacer(modifier = Modifier.height(Spacing.sm))
        
        LazyRow(
            contentPadding = PaddingValues(horizontal = Spacing.md),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
        ) {
            items(templates) { template ->
                WatermarkTemplateCard(
                    template = template,
                    isSelected = template.id == selectedTemplateId,
                    onClick = { onTemplateSelect(template) }
                )
            }
        }
    }
}

@Composable
private fun WatermarkTemplateCard(
    template: WatermarkPreviewTemplate,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed) ColorOSScale.Pressed else if (isSelected) 1.05f else 1f,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 400f),
        label = "templateCardScale"
    )
    
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) Colors.HasselbladOrange else Colors.GlassBackground,
        animationSpec = tween(ColorOSAnimationDuration.FAST),
        label = "borderColor"
    )
    
    Surface(
        modifier = Modifier
            .width(120.dp)
            .height(140.dp)
            .scale(scale),
        shape = RoundedCornerShape(Radius.md),
        color = Colors.Surface,
        border = BorderStroke(2.dp, borderColor),
        onClick = {
            isPressed = true
            onClick()
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(Spacing.sm),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // 缩略图预览
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(template.previewColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = template.previewIcon,
                    contentDescription = null,
                    tint = template.previewColor,
                    modifier = Modifier.size(28.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(Spacing.sm))
            
            Text(
                text = template.name,
                style = Typography.LabelMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (isSelected) Colors.HasselbladOrange else Colors.OnSurface,
                textAlign = TextAlign.Center
            )
            
            if (template.description.isNotEmpty()) {
                Text(
                    text = template.description,
                    style = Typography.LabelSmall,
                    color = Colors.OnSurfaceVariant,
                    textAlign = TextAlign.Center,
                    maxLines = 2
                )
            }
        }
    }
}

/**
 * 水印实时属性调整面板
 */
@Composable
fun WatermarkLivePropertyPanel(
    watermark: Watermark?,
    onOpacityChange: (Float) -> Unit,
    onFontSizeChange: (Float) -> Unit,
    onRotationChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = watermark != null,
        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
        modifier = modifier
    ) {
        watermark?.let { wm ->
            GlassCard(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(Spacing.md)
                ) {
                    Text(
                        text = when (wm.type) {
                            WatermarkType.TEXT -> "文字水印"
                            WatermarkType.IMAGE -> "图片水印"
                            WatermarkType.TEMPLATE -> "模板水印"
                        },
                        style = Typography.TitleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Colors.HasselbladOrange
                    )
                    
                    Spacer(modifier = Modifier.height(Spacing.md))
                    
                    // 透明度
                    PropertySlider(
                        label = "透明度",
                        value = wm.opacity,
                        valueRange = 0.1f..1f,
                        valueText = "${(wm.opacity * 100).toInt()}%",
                        onValueChange = onOpacityChange
                    )
                    
                    // 字号（仅文字水印）
                    if (wm.type == WatermarkType.TEXT) {
                        Spacer(modifier = Modifier.height(Spacing.sm))
                        PropertySlider(
                            label = "字号",
                            value = wm.textConfig.fontSize,
                            valueRange = 8f..120f,
                            valueText = "${wm.textConfig.fontSize.toInt()}sp",
                            onValueChange = onFontSizeChange
                        )
                    }
                    
                    // 旋转
                    Spacer(modifier = Modifier.height(Spacing.sm))
                    PropertySlider(
                        label = "旋转",
                        value = wm.rotation,
                        valueRange = 0f..360f,
                        valueText = "${wm.rotation.toInt()}°",
                        onValueChange = onRotationChange
                    )
                }
            }
        }
    }
}

@Composable
private fun PropertySlider(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    valueText: String,
    onValueChange: (Float) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = Typography.BodySmall,
            color = Colors.OnSurfaceVariant
        )
        
        Text(
            text = valueText,
            style = Typography.LabelMedium,
            fontWeight = FontWeight.Medium,
            color = Colors.HasselbladOrange
        )
    }
    
    Slider(
        value = value,
        onValueChange = onValueChange,
        valueRange = valueRange,
        colors = SliderDefaults.colors(
            thumbColor = Colors.HasselbladOrange,
            activeTrackColor = Colors.HasselbladOrange
        ),
        modifier = Modifier.fillMaxWidth()
    )
}

/**
 * 默认水印模板列表
 */
fun getDefaultPreviewTemplates(): List<WatermarkPreviewTemplate> = listOf(
    WatermarkPreviewTemplate(
        id = "hasselblad_classic",
        name = "哈苏经典",
        description = "HNCS认证水印",
        previewIcon = Icons.Default.CameraAlt,
        previewColor = Colors.HasselbladOrange,
        watermarks = listOf(
            Watermark(
                type = WatermarkType.TEXT,
                text = "HASSELBLAD",
                position = Offset(0.5f, 0.92f),
                textConfig = TextWatermarkConfig(
                    fontSize = 28f,
                    fontColor = Color.White,
                    isBold = true
                ),
                opacity = 0.85f
            )
        )
    ),
    WatermarkPreviewTemplate(
        id = "camera_params",
        name = "参数水印",
        description = "相机参数显示",
        previewIcon = Icons.Default.Settings,
        previewColor = Colors.AccentBlue,
        watermarks = listOf(
            Watermark(
                type = WatermarkType.TEXT,
                text = "ISO 100 · f/1.8 · 1/200s",
                position = Offset(0.5f, 0.95f),
                textConfig = TextWatermarkConfig(
                    fontSize = 18f,
                    fontColor = Color.White,
                    isBold = false
                ),
                opacity = 0.7f
            )
        )
    ),
    WatermarkPreviewTemplate(
        id = "film_style",
        name = "胶片风格",
        description = "复古胶片水印",
        previewIcon = Icons.Default.Movie,
        previewColor = Colors.AccentOrange,
        watermarks = listOf(
            Watermark(
                type = WatermarkType.TEXT,
                text = "© 2026 FILM",
                position = Offset(0.9f, 0.95f),
                textConfig = TextWatermarkConfig(
                    fontSize = 16f,
                    fontColor = Color.White,
                    isBold = false
                ),
                opacity = 0.6f
            )
        )
    ),
    WatermarkPreviewTemplate(
        id = "minimal",
        name = "极简水印",
        description = "简洁角落水印",
        previewIcon = Icons.Default.Remove,
        previewColor = Colors.OnSurfaceVariant,
        watermarks = listOf(
            Watermark(
                type = WatermarkType.TEXT,
                text = "©",
                position = Offset(0.95f, 0.95f),
                textConfig = TextWatermarkConfig(
                    fontSize = 14f,
                    fontColor = Color.White,
                    isBold = true
                ),
                opacity = 0.5f
            )
        )
    )
)
package com.omaster.app.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.omaster.app.ui.animation.ColorOSAnimationDuration
import com.omaster.app.ui.animation.ColorOSEasing
import com.omaster.app.ui.animation.ColorOSScale
import com.omaster.app.ui.theme.*
import com.omaster.app.watermark.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdvancedWatermarkEditorV2(
    onDismiss: () -> Unit,
    onExport: (Uri) -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    
    var state by remember { mutableStateOf(WatermarkEditorState()) }
    var showImagePicker by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var showTemplatePicker by remember { mutableStateOf(false) }
    
    val pickImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            scope.launch {
                val bitmap = loadBitmapFromUri(context, it)
                bitmap?.let { bmp ->
                    state = state.copy(
                        imageUri = it,
                        watermarks = emptyList()
                    )
                }
            }
        }
    }
    
    val pickWatermarkImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            scope.launch {
                val bitmap = loadBitmapFromUri(context, it)
                bitmap?.let { bmp ->
                    val newWatermark = Watermark(
                        type = WatermarkType.IMAGE,
                        imageUri = it,
                        imageConfig = ImageWatermarkConfig(bitmap = bmp),
                        zIndex = state.watermarks.size
                    )
                    state = addToHistoryV2(
                        state,
                        state.copy(
                            watermarks = state.watermarks + newWatermark,
                            selectedWatermarkId = newWatermark.id
                        )
                    )
                }
            }
        }
    }
    
    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        containerColor = Colors.Background,
        topBar = {
            GlassTopAppBarV2(
                title = "水印编辑器",
                onBackClick = onDismiss,
                onUndo = { state = undoV2(state) },
                onRedo = { state = redoV2(state) },
                canUndo = state.historyIndex > 0,
                canRedo = state.historyIndex < state.history.size - 1,
                onExport = { showExportDialog = true }
            )
        },
        bottomBar = {
            WatermarkEditorBottomBarV2(
                onAddText = {
                    val newWatermark = Watermark(
                        type = WatermarkType.TEXT,
                        text = "水印文字",
                        textConfig = TextWatermarkConfig(
                            fontSize = 24f,
                            fontColor = Color.White
                        ),
                        zIndex = state.watermarks.size
                    )
                    state = addToHistoryV2(
                        state,
                        state.copy(
                            watermarks = state.watermarks + newWatermark,
                            selectedWatermarkId = newWatermark.id
                        )
                    )
                },
                onAddImage = { pickWatermarkImageLauncher.launch("image/*") },
                onTemplateClick = { showTemplatePicker = true },
                onDeleteSelected = {
                    state.selectedWatermarkId?.let { id ->
                        state = addToHistoryV2(
                            state,
                            state.copy(
                                watermarks = state.watermarks.filterNot { it.id == id },
                                selectedWatermarkId = null
                            )
                        )
                    }
                },
                hasSelectedWatermark = state.selectedWatermark != null
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                if (state.imageUri == null) {
                    EmptyImportAreaV2(
                        onImportClick = { showImagePicker = true },
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    WatermarkCanvasV2(
                        state = state,
                        onStateChange = { newState ->
                            state = newState
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
            
            state.selectedWatermark?.let { watermark ->
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(Spacing.md)
                ) {
                    WatermarkPropertiesPanelV2(
                        watermark = watermark,
                        onWatermarkChange = { updatedWatermark ->
                            state = addToHistoryV2(
                                state,
                                state.copy(
                                    watermarks = state.watermarks.map {
                                        if (it.id == updatedWatermark.id) updatedWatermark else it
                                    }
                                )
                            )
                        }
                    )
                }
            }
        }
    }
    
    if (showImagePicker) {
        GlassDialog(
            onDismiss = { showImagePicker = false },
            title = "选择图片",
            text = "请选择要添加水印的图片",
            confirmButton = {
                GlassButton(
                    text = "选择图片",
                    onClick = {
                        showImagePicker = false
                        pickImageLauncher.launch("image/*")
                    }
                )
            },
            dismissButton = {
                TextButton(onClick = { showImagePicker = false }) {
                    Text("取消")
                }
            }
        )
    }
    
    if (showExportDialog && state.imageUri != null) {
        ExportDialogV2(
            state = state,
            onExport = { config ->
                scope.launch {
                    snackbarHostState.showSnackbar("导出功能待完善")
                }
            },
            onDismiss = { showExportDialog = false }
        )
    }
    
    if (showTemplatePicker) {
        TemplatePickerDialogV2(
            onTemplateSelected = { template ->
                state = addToHistoryV2(
                    state,
                    state.copy(
                        watermarks = template.watermarks.mapIndexed { index, wm ->
                            wm.copy(zIndex = index)
                        },
                        selectedWatermarkId = null
                    )
                )
                showTemplatePicker = false
            },
            onDismiss = { showTemplatePicker = false }
        )
    }
}

@Composable
internal fun GlassTopAppBarV2(
    title: String,
    onBackClick: () -> Unit,
    onUndo: (() -> Unit)? = null,
    onRedo: (() -> Unit)? = null,
    canUndo: Boolean = false,
    canRedo: Boolean = false,
    onExport: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.ScreenPadding, vertical = Spacing.md),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
        ) {
            GlassIconButton(
                icon = Icons.Default.Close,
                onClick = onBackClick,
                contentDescription = "关闭",
                size = 44.dp
            )

            Text(
                text = title,
                style = Typography.HeadlineMedium,
                fontWeight = FontWeight.Bold,
                color = Colors.OnBackground
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
        ) {
            if (onUndo != null) {
                GlassIconButton(
                    icon = Icons.Default.Undo,
                    onClick = onUndo,
                    contentDescription = "撤销",
                    size = 40.dp,
                    enabled = canUndo
                )
            }

            if (onRedo != null) {
                GlassIconButton(
                    icon = Icons.Default.Redo,
                    onClick = onRedo,
                    contentDescription = "重做",
                    size = 40.dp,
                    enabled = canRedo
                )
            }

            if (onExport != null) {
                GlassIconButton(
                    icon = Icons.Default.Save,
                    onClick = onExport,
                    contentDescription = "导出",
                    size = 40.dp
                )
            }
        }
    }
}

@Composable
private fun EmptyImportAreaV2(
    onImportClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isPressed by remember { mutableStateOf(false) }
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed) ColorOSScale.Pressed else 1f,
        animationSpec = spring(
            dampingRatio = 0.8f,
            stiffness = 400f
        ),
        label = "importScale"
    )
    
    Column(
        modifier = modifier
            .scale(scale)
            .clickable { isPressed = true }
            .clickable { onImportClick() },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(Colors.GlassBackground),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.AddPhotoAlternate,
                contentDescription = "添加图片",
                modifier = Modifier.size(60.dp),
                tint = Colors.HasselbladOrange
            )
        }
        
        Spacer(modifier = Modifier.height(Spacing.lg))
        
        Text(
            text = "点击选择图片",
            style = Typography.TitleLarge,
            fontWeight = FontWeight.SemiBold,
            color = Colors.OnSurface
        )
        
        Spacer(modifier = Modifier.height(Spacing.sm))
        
        Text(
            text = "支持 JPG, PNG, WEBP 格式",
            style = Typography.BodyMedium,
            color = Colors.OnSurfaceVariant
        )
    }
}

@Composable
private fun WatermarkCanvasV2(
    state: WatermarkEditorState,
    onStateChange: (WatermarkEditorState) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    var canvasDrawSize by remember { mutableStateOf(Size.Zero) }
    var canvasDrawOffset by remember { mutableStateOf(Offset.Zero) }
    
    LaunchedEffect(state.imageUri) {
        state.imageUri?.let { uri ->
            bitmap = loadBitmapFromUri(context, uri)
        }
    }
    
    var dragStartPosition by remember { mutableStateOf(Offset.Zero) }
    var draggedWatermarkId by remember { mutableStateOf<String?>(null) }
    
    val interactionSource = remember { MutableInteractionSource() }
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        val hitWatermark = findWatermarkAtPosition(
                            state.watermarks,
                            offset,
                            canvasDrawSize,
                            canvasDrawOffset
                        )
                        if (hitWatermark != null) {
                            draggedWatermarkId = hitWatermark.id
                            dragStartPosition = offset
                            onStateChange(state.copy(selectedWatermarkId = hitWatermark.id))
                        }
                    },
                    onDragEnd = {
                        draggedWatermarkId = null
                        dragStartPosition = Offset.Zero
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        draggedWatermarkId?.let { id ->
                            val watermark = state.watermarks.find { it.id == id }
                            if (watermark != null && canvasDrawSize.width > 0) {
                                val newPosition = Offset(
                                    x = (watermark.position.x + dragAmount.x / canvasDrawSize.width).coerceIn(0f, 1f),
                                    y = (watermark.position.y + dragAmount.y / canvasDrawSize.height).coerceIn(0f, 1f)
                                )
                                val updatedWatermark = watermark.copy(position = newPosition)
                                onStateChange(
                                    state.copy(
                                        watermarks = state.watermarks.map { 
                                            if (it.id == id) updatedWatermark else it 
                                        }
                                    )
                                )
                            }
                        }
                    }
                )
            }
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, rotation ->
                    state.selectedWatermarkId?.let { id ->
                        val watermark = state.watermarks.find { it.id == id }
                        if (watermark != null && canvasDrawSize.width > 0) {
                            val newPosition = Offset(
                                x = (watermark.position.x + pan.x / canvasDrawSize.width).coerceIn(0f, 1f),
                                y = (watermark.position.y + pan.y / canvasDrawSize.height).coerceIn(0f, 1f)
                            )
                            val newScale = (watermark.scale * zoom).coerceIn(0.2f, 3f)
                            val newRotation = (watermark.rotation + rotation).coerceIn(0f, 360f)
                            val updatedWatermark = watermark.copy(
                                position = newPosition,
                                scale = newScale,
                                rotation = newRotation
                            )
                            onStateChange(
                                state.copy(
                                    watermarks = state.watermarks.map { 
                                        if (it.id == id) updatedWatermark else it 
                                    }
                                )
                            )
                        }
                    }
                }
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) {
                onStateChange(state.copy(selectedWatermarkId = null))
            }
    ) {
        bitmap?.let { bmp ->
            val bmpImage = remember(bmp) { bmp.asImageBitmap() }
            
            Canvas(
                modifier = Modifier.fillMaxSize()
            ) {
                val scaleFactor = minOf(
                    size.width / bmp.width,
                    size.height / bmp.height
                )
                val drawSize = Size(
                    bmp.width * scaleFactor,
                    bmp.height * scaleFactor
                )
                val drawOffset = Offset(
                    (size.width - drawSize.width) / 2f,
                    (size.height - drawSize.height) / 2f
                )
                
                canvasDrawSize = drawSize
                canvasDrawOffset = drawOffset
                
                drawImage(
                    image = bmpImage,
                    dstSize = IntSize(drawSize.width.toInt(), drawSize.height.toInt()),
                    dstOffset = IntOffset(drawOffset.x.toInt(), drawOffset.y.toInt())
                )
                
                state.watermarks.sortedBy { it.zIndex }.forEach { watermark ->
                    drawWatermarkV2(
                        watermark = watermark,
                        canvasSize = drawSize,
                        canvasOffset = drawOffset,
                        isSelected = watermark.id == state.selectedWatermarkId
                    )
                }
            }
        }
    }
}

private fun findWatermarkAtPosition(
    watermarks: List<Watermark>,
    touchPosition: Offset,
    canvasSize: Size,
    canvasOffset: Offset
): Watermark? {
    if (canvasSize.width <= 0 || canvasSize.height <= 0) return null
    
    for (watermark in watermarks.sortedByDescending { it.zIndex }) {
        val watermarkPosition = Offset(
            canvasOffset.x + watermark.position.x * canvasSize.width,
            canvasOffset.y + watermark.position.y * canvasSize.height
        )
        
        val halfWidth = watermark.size.width / 2 * watermark.scale
        val halfHeight = watermark.size.height / 2 * watermark.scale
        
        val bounds = androidx.compose.ui.geometry.Rect(
            left = watermarkPosition.x - halfWidth,
            top = watermarkPosition.y - halfHeight,
            right = watermarkPosition.x + halfWidth,
            bottom = watermarkPosition.y + halfHeight
        )
        
        if (bounds.contains(touchPosition)) {
            return watermark
        }
    }
    return null
}

private fun DrawScope.drawWatermarkV2(
    watermark: Watermark,
    canvasSize: Size,
    canvasOffset: Offset,
    isSelected: Boolean
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
        when (watermark.type) {
            WatermarkType.TEXT -> drawTextWatermarkV2(watermark, watermark.opacity)
            WatermarkType.IMAGE -> drawImageWatermarkV2(watermark, watermark.opacity)
            WatermarkType.TEMPLATE -> {}
        }
        
        if (isSelected) {
            drawSelectionFrameV2(watermark.size)
        }
    }
}

private fun DrawScope.drawTextWatermarkV2(watermark: Watermark, opacity: Float) {
    val config = watermark.textConfig
    
    val textStyle = TextStyle(
        fontSize = config.fontSize.sp,
        color = config.fontColor.copy(alpha = opacity),
        fontWeight = if (config.isBold) FontWeight.Bold else FontWeight.Normal,
        fontStyle = if (config.isItalic) FontStyle.Italic else FontStyle.Normal
    )
    
    val textMeasurer = rememberTextMeasurer()
    val measuredText = textMeasurer.measure(watermark.text, style = textStyle)
    
    val textOffset = when (config.alignment) {
        TextAlign.Left -> Offset.Zero
        TextAlign.Center -> Offset(-measuredText.size.width / 2f, 0f)
        TextAlign.Right -> Offset(-measuredText.size.width.toFloat(), 0f)
        else -> Offset.Zero
    }
    
    drawText(
        textLayoutResult = measuredText,
        topLeft = textOffset
    )
}

private fun DrawScope.drawImageWatermarkV2(watermark: Watermark, opacity: Float) {
    watermark.imageConfig.bitmap?.let { bitmap ->
        val image = bitmap.asImageBitmap()
        val scaledSize = watermark.size
        
        drawImage(
            image = image,
            dstSize = IntSize(scaledSize.width.toInt(), scaledSize.height.toInt()),
            dstOffset = IntOffset(-scaledSize.width.toInt() / 2, -scaledSize.height.toInt() / 2),
            alpha = opacity
        )
    }
}

private fun DrawScope.drawSelectionFrameV2(size: Size) {
    val padding = 16f
    val frameRect = androidx.compose.ui.geometry.Rect(
        left = -size.width / 2 - padding,
        top = -size.height / 2 - padding,
        right = size.width / 2 + padding,
        bottom = size.height / 2 + padding
    )
    
    drawRect(
        color = Colors.HasselbladOrange,
        style = Stroke(width = 2f),
        topLeft = frameRect.topLeft,
        size = frameRect.size
    )
    
    val handleSize = 12f
    val handles = listOf(
        Offset(frameRect.left, frameRect.top),
        Offset(frameRect.right, frameRect.top),
        Offset(frameRect.left, frameRect.bottom),
        Offset(frameRect.right, frameRect.bottom)
    )
    
    handles.forEach { handle ->
        drawRect(
            color = Colors.HasselbladOrange,
            topLeft = Offset(handle.x - handleSize / 2, handle.y - handleSize / 2),
            size = Size(handleSize, handleSize)
        )
    }
}

@Composable
private fun WatermarkEditorBottomBarV2(
    onAddText: () -> Unit,
    onAddImage: () -> Unit,
    onTemplateClick: () -> Unit,
    onDeleteSelected: () -> Unit,
    hasSelectedWatermark: Boolean
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.md),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            BottomBarButtonV2(
                icon = Icons.Default.Title,
                label = "文字",
                onClick = onAddText
            )
            
            BottomBarButtonV2(
                icon = Icons.Default.Image,
                label = "图片",
                onClick = onAddImage
            )
            
            BottomBarButtonV2(
                icon = Icons.Default.Style,
                label = "模板",
                onClick = onTemplateClick
            )
            
            BottomBarButtonV2(
                icon = Icons.Default.Delete,
                label = "删除",
                onClick = onDeleteSelected,
                enabled = hasSelectedWatermark
            )
        }
    }
}

@Composable
private fun BottomBarButtonV2(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    var isPressed by remember { mutableStateOf(false) }
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed) ColorOSScale.Pressed else 1f,
        animationSpec = spring(
            dampingRatio = 0.8f,
            stiffness = 400f
        ),
        label = "bottomBarScale"
    )
    
    Column(
        modifier = Modifier
            .scale(scale)
            .clip(RoundedCornerShape(Radius.md))
            .clickable(enabled = enabled) {
                isPressed = true
                onClick()
            }
            .padding(Spacing.sm),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (enabled) Colors.HasselbladOrange else Colors.Disabled,
            modifier = Modifier.size(28.dp)
        )
        
        Spacer(modifier = Modifier.height(Spacing.xs))
        
        Text(
            text = label,
            style = Typography.LabelMedium,
            color = if (enabled) Colors.OnSurface else Colors.Disabled
        )
    }
}

@Composable
private fun WatermarkPropertiesPanelV2(
    watermark: Watermark,
    onWatermarkChange: (Watermark) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(Spacing.md),
        verticalArrangement = Arrangement.spacedBy(Spacing.md)
    ) {
        Text(
            text = when (watermark.type) {
                WatermarkType.TEXT -> "文字水印"
                WatermarkType.IMAGE -> "图片水印"
                WatermarkType.TEMPLATE -> "模板水印"
            },
            style = Typography.TitleMedium,
            fontWeight = FontWeight.Bold,
            color = Colors.HasselbladOrange
        )
        
        if (watermark.type == WatermarkType.TEXT) {
            TextWatermarkPropertiesV2(
                watermark = watermark,
                onWatermarkChange = onWatermarkChange
            )
        } else if (watermark.type == WatermarkType.IMAGE) {
            ImageWatermarkPropertiesV2(
                watermark = watermark,
                onWatermarkChange = onWatermarkChange
            )
        }
        
        CommonWatermarkPropertiesV2(
            watermark = watermark,
            onWatermarkChange = onWatermarkChange
        )
    }
}

@Composable
private fun TextWatermarkPropertiesV2(
    watermark: Watermark,
    onWatermarkChange: (Watermark) -> Unit
) {
    var text by remember { mutableStateOf(watermark.text) }
    var fontSize by remember { mutableFloatStateOf(watermark.textConfig.fontSize) }
    var isBold by remember { mutableStateOf(watermark.textConfig.isBold) }
    var isItalic by remember { mutableStateOf(watermark.textConfig.isItalic) }
    
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        OutlinedTextField(
            value = text,
            onValueChange = {
                text = it
                onWatermarkChange(watermark.copy(text = it))
            },
            label = { Text("文字内容") },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Colors.HasselbladOrange,
                focusedLabelColor = Colors.HasselbladOrange
            )
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "字号: ${fontSize.toInt()}",
                style = Typography.BodyMedium,
                color = Colors.OnSurface
            )
            
            Slider(
                value = fontSize,
                onValueChange = {
                    fontSize = it
                    onWatermarkChange(
                        watermark.copy(
                            textConfig = watermark.textConfig.copy(fontSize = it)
                        )
                    )
                },
                valueRange = 8f..120f,
                colors = SliderDefaults.colors(
                    thumbColor = Colors.HasselbladOrange,
                    activeTrackColor = Colors.HasselbladOrange
                ),
                modifier = Modifier.weight(1f)
            )
        }
        
        Row(
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
        ) {
            GlassChip(
                text = "粗体",
                selected = isBold,
                onClick = {
                    isBold = !isBold
                    onWatermarkChange(
                        watermark.copy(
                            textConfig = watermark.textConfig.copy(isBold = isBold)
                        )
                    )
                }
            )
            
            GlassChip(
                text = "斜体",
                selected = isItalic,
                onClick = {
                    isItalic = !isItalic
                    onWatermarkChange(
                        watermark.copy(
                            textConfig = watermark.textConfig.copy(isItalic = isItalic)
                        )
                    )
                }
            )
        }
    }
}

@Composable
private fun ImageWatermarkPropertiesV2(
    watermark: Watermark,
    onWatermarkChange: (Watermark) -> Unit
) {
    var preserveAspectRatio by remember { mutableStateOf(watermark.imageConfig.preserveAspectRatio) }
    
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        watermark.imageUri?.let { uri ->
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(uri)
                    .build(),
                contentDescription = "水印图片",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .height(120.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(Radius.md))
                    .background(Colors.GlassBackground)
            )
        }
        
        GlassChip(
            text = "保持比例",
            selected = preserveAspectRatio,
            onClick = {
                preserveAspectRatio = !preserveAspectRatio
                onWatermarkChange(
                    watermark.copy(
                        imageConfig = watermark.imageConfig.copy(preserveAspectRatio = preserveAspectRatio)
                    )
                )
            }
        )
    }
}

@Composable
private fun CommonWatermarkPropertiesV2(
    watermark: Watermark,
    onWatermarkChange: (Watermark) -> Unit
) {
    var opacity by remember { mutableFloatStateOf(watermark.opacity) }
    var rotation by remember { mutableFloatStateOf(watermark.rotation) }
    
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "透明度",
                style = Typography.BodyMedium,
                color = Colors.OnSurface
            )
            Text(
                text = "${(opacity * 100).toInt()}%",
                style = Typography.BodyMedium,
                color = Colors.HasselbladOrange
            )
        }
        
        Slider(
            value = opacity,
            onValueChange = {
                opacity = it
                onWatermarkChange(watermark.copy(opacity = it))
            },
            valueRange = 0.1f..1f,
            colors = SliderDefaults.colors(
                thumbColor = Colors.HasselbladOrange,
                activeTrackColor = Colors.HasselbladOrange
            )
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "旋转",
                style = Typography.BodyMedium,
                color = Colors.OnSurface
            )
            Text(
                text = "${rotation.toInt()}°",
                style = Typography.BodyMedium,
                color = Colors.HasselbladOrange
            )
        }
        
        Slider(
            value = rotation,
            onValueChange = {
                rotation = it
                onWatermarkChange(watermark.copy(rotation = it))
            },
            valueRange = 0f..360f,
            colors = SliderDefaults.colors(
                thumbColor = Colors.HasselbladOrange,
                activeTrackColor = Colors.HasselbladOrange
            )
        )
    }
}

@Composable
private fun ExportDialogV2(
    state: WatermarkEditorState,
    onExport: (ExportConfig) -> Unit,
    onDismiss: () -> Unit
) {
    var format by remember { mutableStateOf(ExportFormat.JPEG) }
    var quality by remember { mutableIntStateOf(95) }
    var resolution by remember { mutableStateOf(ExportResolution.ORIGINAL) }
    
    GlassDialog(
        onDismiss = onDismiss,
        title = "导出图片",
        text = "选择导出格式和质量",
        confirmButton = {
            GlassButton(
                text = "导出",
                onClick = {
                    onExport(ExportConfig(format, quality, resolution))
                    onDismiss()
                }
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

@Composable
private fun TemplatePickerDialogV2(
    onTemplateSelected: (WatermarkTemplateData) -> Unit,
    onDismiss: () -> Unit
) {
    val templates = getDefaultTemplatesV2()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "选择模板",
                style = Typography.HeadlineSmall,
                color = Colors.OnSurface
            )
        },
        text = {
            Column {
                Text(
                    text = "选择一个预设水印模板",
                    style = Typography.BodyMedium,
                    color = Colors.OnSurfaceVariant
                )
                Spacer(modifier = Modifier.height(Spacing.md))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                ) {
                    items(templates) { template ->
                        TemplateItemV2(
                            template = template,
                            onClick = { onTemplateSelected(template) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        },
        containerColor = Colors.Surface,
        titleContentColor = Colors.OnSurface,
        textContentColor = Colors.OnSurfaceVariant
    )
}

@Composable
private fun TemplateItemV2(
    template: WatermarkTemplateData,
    onClick: () -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed) ColorOSScale.Pressed else 1f,
        animationSpec = spring(
            dampingRatio = 0.8f,
            stiffness = 400f
        ),
        label = "templateScale"
    )
    
    GlassCard(
        modifier = Modifier
            .width(150.dp)
            .scale(scale),
        onClick = {
            isPressed = true
            onClick()
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.md),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Colors.HasselbladOrange.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Style,
                    contentDescription = null,
                    tint = Colors.HasselbladOrange,
                    modifier = Modifier.size(24.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(Spacing.sm))
            
            Text(
                text = template.name,
                style = Typography.LabelLarge,
                fontWeight = FontWeight.SemiBold,
                color = Colors.OnSurface,
                textAlign = TextAlign.Center
            )
            
            if (template.description.isNotEmpty()) {
                Text(
                    text = template.description,
                    style = Typography.LabelSmall,
                    color = Colors.OnSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

private fun getDefaultTemplatesV2(): List<WatermarkTemplateData> = listOf(
    WatermarkTemplateData(
        id = "simple_text",
        name = "简单文字",
        description = "底部居中文字水印",
        isSystem = true,
        watermarks = listOf(
            Watermark(
                type = WatermarkType.TEXT,
                text = "© OPPO",
                position = Offset(0.5f, 0.95f),
                textConfig = TextWatermarkConfig(
                    fontSize = 32f,
                    fontColor = Color.White,
                    isBold = true
                ),
                opacity = 0.8f
            )
        )
    ),
    WatermarkTemplateData(
        id = "corner_text",
        name = "角落文字",
        description = "右下角文字水印",
        isSystem = true,
        watermarks = listOf(
            Watermark(
                type = WatermarkType.TEXT,
                text = "OPPO",
                position = Offset(0.9f, 0.95f),
                textConfig = TextWatermarkConfig(
                    fontSize = 24f,
                    fontColor = Color.White,
                    isBold = true
                ),
                opacity = 0.7f
            )
        )
    )
)

private fun addToHistoryV2(state: WatermarkEditorState, newState: WatermarkEditorState): WatermarkEditorState {
    val newHistory = state.history.take(state.historyIndex + 1) + newState
    val trimmedHistory = if (newHistory.size > state.maxHistorySize) {
        newHistory.drop(newHistory.size - state.maxHistorySize)
    } else {
        newHistory
    }
    return newState.copy(
        history = trimmedHistory,
        historyIndex = trimmedHistory.size - 1
    )
}

private fun undoV2(state: WatermarkEditorState): WatermarkEditorState {
    if (state.historyIndex <= 0) return state
    val newIndex = state.historyIndex - 1
    return state.history[newIndex].copy(
        history = state.history,
        historyIndex = newIndex
    )
}

private fun redoV2(state: WatermarkEditorState): WatermarkEditorState {
    if (state.historyIndex >= state.history.size - 1) return state
    val newIndex = state.historyIndex + 1
    return state.history[newIndex].copy(
        history = state.history,
        historyIndex = newIndex
    )
}

private suspend fun loadBitmapFromUri(context: Context, uri: Uri): Bitmap? = withContext(Dispatchers.IO) {
    try {
        context.contentResolver.openInputStream(uri)?.use { input ->
            BitmapFactory.decodeStream(input)
        }
    } catch (e: Exception) {
        null
    }
}

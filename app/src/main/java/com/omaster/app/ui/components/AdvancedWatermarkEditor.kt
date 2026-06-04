package com.omaster.app.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.omaster.app.ui.theme.*
import com.omaster.app.watermark.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.cos
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdvancedWatermarkEditor(
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
                        imageConfig = ImageWatermarkConfig(bitmap = bmp)
                    ).apply {
                        zIndex = state.watermarks.size
                    }
                    state = addToHistory(
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
        topBar = {
            TopAppBar(
                title = { Text("水印编辑器", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "关闭")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { 
                            state = undo(state) 
                        },
                        enabled = state.historyIndex > 0
                    ) {
                        Icon(Icons.Default.Undo, contentDescription = "撤销")
                    }
                    IconButton(
                        onClick = { 
                            state = redo(state) 
                        },
                        enabled = state.historyIndex < state.history.size - 1
                    ) {
                        Icon(Icons.Default.Redo, contentDescription = "重做")
                    }
                    IconButton(onClick = { showExportDialog = true }) {
                        Icon(Icons.Default.Save, contentDescription = "保存")
                    }
                }
            )
        },
        bottomBar = {
            WatermarkEditorBottomBar(
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
                    state = addToHistory(
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
                        state = addToHistory(
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
            // Main editor area
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(Color.Black)
            ) {
                if (state.imageUri == null) {
                    EmptyImportArea(
                        onImportClick = { showImagePicker = true },
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    WatermarkCanvas(
                        state = state,
                        onStateChange = { newState -> 
                            state = newState 
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
            
            // Properties panel
            state.selectedWatermark?.let { watermark ->
                WatermarkPropertiesPanel(
                    watermark = watermark,
                    onWatermarkChange = { updatedWatermark ->
                        state = addToHistory(
                            state,
                            state.copy(
                                watermarks = state.watermarks.map { 
                                    if (it.id == updatedWatermark.id) updatedWatermark else it 
                                }
                            )
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                        .verticalScroll(rememberScrollState())
                )
            }
        }
    }
    
    if (showImagePicker) {
        AlertDialog(
            onDismissRequest = { showImagePicker = false },
            title = { Text("选择图片") },
            text = { Text("请选择要添加水印的图片") },
            confirmButton = {
                TextButton(onClick = {
                    showImagePicker = false
                    pickImageLauncher.launch("image/*")
                }) {
                    Text("选择")
                }
            },
            dismissButton = {
                TextButton(onClick = { showImagePicker = false }) {
                    Text("取消")
                }
            }
        )
    }
    
    if (showExportDialog && state.imageUri != null) {
        ExportDialog(
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
        TemplatePickerDialog(
            onTemplateSelected = { template ->
                state = addToHistory(
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
fun EmptyImportArea(
    onImportClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clickable(onClick = onImportClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.AddPhotoAlternate,
            contentDescription = "添加图片",
            modifier = Modifier.size(64.dp),
            tint = Color.Gray
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "点击选择图片",
            color = Color.Gray,
            style = MaterialTheme.typography.titleMedium
        )
        Text(
            text = "支持 JPG, PNG, WEBP 格式",
            color = Color.Gray.copy(alpha = 0.7f),
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
fun WatermarkCanvas(
    state: WatermarkEditorState,
    onStateChange: (WatermarkEditorState) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    
    LaunchedEffect(state.imageUri) {
        state.imageUri?.let { uri ->
            bitmap = loadBitmapFromUri(context, uri)
        }
    }
    
    val scale = remember { mutableFloatStateOf(1f) }
    val offset = remember { mutableStateOf(Offset.Zero) }
    
    var dragOffset by remember { mutableStateOf(Offset.Zero) }
    var isDragging by remember { mutableStateOf(false) }
    
    val interactionSource = remember { MutableInteractionSource() }
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) {
                onStateChange(state.copy(selectedWatermarkId = null))
            }
    ) {
        bitmap?.let { bmp ->
            val bmpImage = remember(bmp) { bmp.asImageBitmap() }
            val canvasSize = remember { mutableStateOf(Size.Zero) }
            
            Canvas(
                modifier = Modifier.fillMaxSize()
            ) {
                canvasSize.value = size
                
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
                
                drawImage(
                    image = bmpImage,
                    dstSize = IntSize(drawSize.width.toInt(), drawSize.height.toInt()),
                    dstOffset = IntOffset(drawOffset.x.toInt(), drawOffset.y.toInt())
                )
                
                state.watermarks.sortedBy { it.zIndex }.forEach { watermark ->
                    drawWatermark(
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

fun DrawScope.drawWatermark(
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
        rotate(watermark.rotation, pivot = Offset.Zero)
    }) {
        when (watermark.type) {
            WatermarkType.TEXT -> drawTextWatermark(watermark, watermark.opacity)
            WatermarkType.IMAGE -> drawImageWatermark(watermark, watermark.opacity)
            WatermarkType.TEMPLATE -> {}
        }
        
        if (isSelected) {
            drawSelectionFrame(watermark.size)
        }
    }
}

fun DrawScope.drawTextWatermark(watermark: Watermark, opacity: Float) {
    val config = watermark.textConfig
    
    val textStyle = TextStyle(
        fontSize = config.fontSize.sp,
        color = config.fontColor.copy(alpha = opacity),
        fontWeight = if (config.isBold) FontWeight.Bold else FontWeight.Normal,
        fontStyle = if (config.isItalic) FontStyle.Italic else FontStyle.Normal,
        textDecoration = when {
            config.isUnderline && config.isStrikethrough ->
                TextDecoration.combine(listOf(TextDecoration.Underline, TextDecoration.LineThrough))
            config.isUnderline -> TextDecoration.Underline
            config.isStrikethrough -> TextDecoration.LineThrough
            else -> TextDecoration.None
        }
    )
    
    val textMeasurer = rememberTextMeasurer()
    val measuredText = textMeasurer.measure(watermark.text, style = textStyle)
    
    val textOffset = when (config.alignment) {
        TextAlignment.LEFT -> Offset.Zero
        TextAlignment.CENTER -> Offset(-measuredText.size.width / 2f, 0f)
        TextAlignment.RIGHT -> Offset(-measuredText.size.width.toFloat(), 0f)
    }
    
    if (config.hasStroke) {
        // Draw stroke
    }
    
    if (config.hasShadow) {
        // Draw shadow
    }
    
    drawText(
        textLayoutResult = measuredText,
        topLeft = textOffset
    )
}

fun DrawScope.drawImageWatermark(watermark: Watermark, opacity: Float) {
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

fun DrawScope.drawSelectionFrame(size: Size) {
    val padding = 16f
    val frameRect = Rect(
        left = -size.width / 2 - padding,
        top = -size.height / 2 - padding,
        right = size.width / 2 + padding,
        bottom = size.height / 2 + padding
    )
    
    drawRect(
        color = HasselbladOrange,
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
            color = HasselbladOrange,
            topLeft = Offset(handle.x - handleSize / 2, handle.y - handleSize / 2),
            size = Size(handleSize, handleSize)
        )
    }
}

@Composable
fun WatermarkEditorBottomBar(
    onAddText: () -> Unit,
    onAddImage: () -> Unit,
    onTemplateClick: () -> Unit,
    onDeleteSelected: () -> Unit,
    hasSelectedWatermark: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            BottomBarButton(
                icon = Icons.Default.Title,
                label = "文字",
                onClick = onAddText
            )
            BottomBarButton(
                icon = Icons.Default.Image,
                label = "图片",
                onClick = onAddImage
            )
            BottomBarButton(
                icon = Icons.Default.Style,
                label = "模板",
                onClick = onTemplateClick
            )
            BottomBarButton(
                icon = Icons.Default.Delete,
                label = "删除",
                onClick = onDeleteSelected,
                enabled = hasSelectedWatermark
            )
        }
    }
}

@Composable
fun BottomBarButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Column(
        modifier = modifier
            .clickable(enabled = enabled, onClick = onClick)
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            icon,
            contentDescription = label,
            tint = if (enabled) HasselbladOrange else Color.Gray,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (enabled) MaterialTheme.colorScheme.onSurface else Color.Gray
        )
    }
}

@Composable
fun WatermarkPropertiesPanel(
    watermark: Watermark,
    onWatermarkChange: (Watermark) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = when (watermark.type) {
                WatermarkType.TEXT -> "文字水印"
                WatermarkType.IMAGE -> "图片水印"
                WatermarkType.TEMPLATE -> "模板水印"
            },
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        
        if (watermark.type == WatermarkType.TEXT) {
            TextWatermarkProperties(
                watermark = watermark,
                onWatermarkChange = onWatermarkChange
            )
        } else if (watermark.type == WatermarkType.IMAGE) {
            ImageWatermarkProperties(
                watermark = watermark,
                onWatermarkChange = onWatermarkChange
            )
        }
        
        CommonWatermarkProperties(
            watermark = watermark,
            onWatermarkChange = onWatermarkChange
        )
    }
}

@Composable
fun TextWatermarkProperties(
    watermark: Watermark,
    onWatermarkChange: (Watermark) -> Unit
) {
    var text by remember { mutableStateOf(watermark.text) }
    var fontSize by remember { mutableFloatStateOf(watermark.textConfig.fontSize) }
    var isBold by remember { mutableStateOf(watermark.textConfig.isBold) }
    var isItalic by remember { mutableStateOf(watermark.textConfig.isItalic) }
    
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
            value = text,
            onValueChange = { 
                text = it
                onWatermarkChange(watermark.copy(text = it))
            },
            label = { Text("文字内容") },
            modifier = Modifier.fillMaxWidth()
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("字号: ${fontSize.toInt()}")
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
                modifier = Modifier.weight(1f)
            )
        }
        
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = isBold,
                onClick = {
                    isBold = !isBold
                    onWatermarkChange(
                        watermark.copy(
                            textConfig = watermark.textConfig.copy(isBold = isBold)
                        )
                    )
                },
                label = { Text("粗体") }
            )
            FilterChip(
                selected = isItalic,
                onClick = {
                    isItalic = !isItalic
                    onWatermarkChange(
                        watermark.copy(
                            textConfig = watermark.textConfig.copy(isItalic = isItalic)
                        )
                    )
                },
                label = { Text("斜体") }
            )
        }
    }
}

@Composable
fun ImageWatermarkProperties(
    watermark: Watermark,
    onWatermarkChange: (Watermark) -> Unit
) {
    var preserveAspectRatio by remember { mutableStateOf(watermark.imageConfig.preserveAspectRatio) }
    
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            )
        }
        
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = preserveAspectRatio,
                onClick = {
                    preserveAspectRatio = !preserveAspectRatio
                    onWatermarkChange(
                        watermark.copy(
                            imageConfig = watermark.imageConfig.copy(preserveAspectRatio = preserveAspectRatio)
                        )
                    )
                },
                label = { Text("保持比例") }
            )
        }
    }
}

@Composable
fun CommonWatermarkProperties(
    watermark: Watermark,
    onWatermarkChange: (Watermark) -> Unit
) {
    var opacity by remember { mutableFloatStateOf(watermark.opacity) }
    var rotation by remember { mutableFloatStateOf(watermark.rotation) }
    
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("透明度", style = MaterialTheme.typography.bodyMedium)
            Text("${(opacity * 100).toInt()}%", style = MaterialTheme.typography.bodyMedium)
        }
        Slider(
            value = opacity,
            onValueChange = { 
                opacity = it
                onWatermarkChange(watermark.copy(opacity = it))
            },
            valueRange = 0.1f..1f
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("旋转", style = MaterialTheme.typography.bodyMedium)
            Text("${rotation.toInt()}°", style = MaterialTheme.typography.bodyMedium)
        }
        Slider(
            value = rotation,
            onValueChange = { 
                rotation = it
                onWatermarkChange(watermark.copy(rotation = it))
            },
            valueRange = 0f..360f
        )
    }
}

@Composable
fun ExportDialog(
    state: WatermarkEditorState,
    onExport: (ExportConfig) -> Unit,
    onDismiss: () -> Unit
) {
    var format by remember { mutableStateOf(ExportFormat.JPEG) }
    var quality by remember { mutableIntStateOf(95) }
    var resolution by remember { mutableStateOf(ExportResolution.ORIGINAL) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("导出图片") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("格式", style = MaterialTheme.typography.titleSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ExportFormat.entries.forEach { f ->
                        FilterChip(
                            selected = format == f,
                            onClick = { format = f },
                            label = { Text(f.name) }
                        )
                    }
                }
                
                Text("质量: $quality%", style = MaterialTheme.typography.titleSmall)
                Slider(
                    value = quality.toFloat(),
                    onValueChange = { quality = it.toInt() },
                    valueRange = 50f..100f
                )
                
                Text("分辨率", style = MaterialTheme.typography.titleSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ExportResolution.entries.forEach { r ->
                        FilterChip(
                            selected = resolution == r,
                            onClick = { resolution = r },
                            label = { Text(if (r == ExportResolution.ORIGINAL) "原始" else r.name) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                onExport(ExportConfig(format, quality, resolution))
                onDismiss()
            }) {
                Text("导出")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

@Composable
fun TemplatePickerDialog(
    onTemplateSelected: (WatermarkTemplateData) -> Unit,
    onDismiss: () -> Unit
) {
    val templates = getDefaultTemplates()
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择模板") },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(templates) { template ->
                    TemplateItem(
                        template = template,
                        onClick = { onTemplateSelected(template) }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

@Composable
fun TemplateItem(
    template: WatermarkTemplateData,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Style,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = HasselbladOrange
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(template.name, fontWeight = FontWeight.Bold)
                if (template.description.isNotEmpty()) {
                    Text(
                        template.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

fun getDefaultTemplates(): List<WatermarkTemplateData> = listOf(
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

fun addToHistory(state: WatermarkEditorState, newState: WatermarkEditorState): WatermarkEditorState {
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

fun undo(state: WatermarkEditorState): WatermarkEditorState {
    if (state.historyIndex <= 0) return state
    val newIndex = state.historyIndex - 1
    return state.history[newIndex].copy(
        history = state.history,
        historyIndex = newIndex
    )
}

fun redo(state: WatermarkEditorState): WatermarkEditorState {
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

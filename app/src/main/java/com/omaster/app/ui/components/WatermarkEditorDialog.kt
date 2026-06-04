package com.omaster.app.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.omaster.app.ui.theme.*
import com.omaster.app.watermark.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File

@Composable
fun WatermarkEditorDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var selectedTemplate by remember { mutableStateOf(WatermarkTemplate.OPPO) }
    var selectedPosition by remember { mutableStateOf(WatermarkPosition.BOTTOM_RIGHT) }
    var opacity by remember { mutableFloatStateOf(0.8f) }
    var scale by remember { mutableFloatStateOf(1.0f) }
    var previewBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isProcessing by remember { mutableStateOf(false) }
    var batchMode by remember { mutableStateOf(false) }

    val processor = remember { WatermarkProcessor(context) }

    val pickImageLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            selectedImageUri = it
            scope.launch {
                previewBitmap = loadBitmapFromUri(context, it)
            }
        }
    }

    val pickMultipleImagesLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (uris.isNotEmpty()) {
            selectedImageUri = uris.first()
            scope.launch {
                previewBitmap = loadBitmapFromUri(context, uris.first())
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "水印编辑器",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "关闭")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Batch mode toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "批量模式", fontWeight = FontWeight.Medium)
                Switch(
                    checked = batchMode,
                    onCheckedChange = { batchMode = it }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Image selection area
            if (selectedImageUri == null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clickable {
                            if (batchMode) {
                                pickMultipleImagesLauncher.launch("image/*")
                            } else {
                                pickImageLauncher.launch("image/*")
                            }
                        },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.AddPhotoAlternate,
                            contentDescription = "添加图片",
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (batchMode) "选择多张图片" else "选择图片",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                // Preview area
                previewBitmap?.let { bitmap ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = "预览",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Template selection
            Text(
                text = "水印模板",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(WatermarkTemplate.entries) { template ->
                    TemplateChip(
                        template = template,
                        selected = selectedTemplate == template,
                        onClick = { selectedTemplate = template }
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Position selection
            Text(
                text = "水印位置",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))

            PositionSelector(
                selectedPosition = selectedPosition,
                onPositionChange = { selectedPosition = it }
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Opacity slider
            Text(
                text = "透明度",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Slider(
                value = opacity,
                onValueChange = { opacity = it },
                valueRange = 0.1f..1.0f,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Text(
                text = "${(opacity * 100).toInt()}%",
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Scale slider
            Text(
                text = "大小",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Slider(
                value = scale,
                onValueChange = { scale = it },
                valueRange = 0.5f..2.0f,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Text(
                text = "${(scale * 100).toInt()}%",
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        previewBitmap?.let { bitmap ->
                            scope.launch {
                                isProcessing = true
                                val config = WatermarkConfig(
                                    template = selectedTemplate,
                                    position = selectedPosition,
                                    opacity = opacity,
                                    scale = scale
                                )
                                val result = processor.processWatermark(
                                    WatermarkProcessRequest(
                                        sourceBitmap = bitmap,
                                        config = config
                                    )
                                )
                                if (result.success && result.bitmap != null) {
                                    previewBitmap = result.bitmap
                                    snackbarHostState.showSnackbar("预览已更新")
                                } else {
                                    snackbarHostState.showSnackbar(
                                        result.error ?: "处理失败"
                                    )
                                }
                                isProcessing = false
                            }
                        }
                    },
                    modifier = Modifier.weight(1f),
                    enabled = previewBitmap != null && !isProcessing
                ) {
                    if (isProcessing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(Icons.Default.Visibility, contentDescription = "预览")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("预览")
                }

                Button(
                    onClick = {
                        previewBitmap?.let {
                            scope.launch {
                                snackbarHostState.showSnackbar("保存功能待实现")
                            }
                        }
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = HasselbladOrange),
                    enabled = previewBitmap != null
                ) {
                    Icon(Icons.Default.Save, contentDescription = "保存")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("保存")
                }
            }
        }
    }
}

@Composable
fun TemplateChip(
    template: WatermarkTemplate,
    selected: Boolean,
    onClick: () -> Unit
) {
    val displayName = when (template) {
        WatermarkTemplate.OPPO -> "OPPO"
        WatermarkTemplate.ONEPLUS -> "OnePlus"
        WatermarkTemplate.REALME -> "realme"
        WatermarkTemplate.MINIMAL_PARAMS -> "参数"
        WatermarkTemplate.TIMESTAMP -> "时间"
        WatermarkTemplate.LOCATION -> "位置"
        WatermarkTemplate.CUSTOM -> "自定义"
    }

    val accentColor = when (template) {
        WatermarkTemplate.OPPO -> HasselbladOrange
        WatermarkTemplate.ONEPLUS -> AccentPrimary
        WatermarkTemplate.REALME -> Color(0xFFFFE70A).copy()
        else -> MaterialTheme.colorScheme.primary
    }

    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(displayName) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = accentColor.copy(alpha = 0.2f),
            selectedLabelColor = accentColor
        )
    )
}

@Composable
fun PositionSelector(
    selectedPosition: WatermarkPosition,
    onPositionChange: (WatermarkPosition) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top row
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PositionItem(
                position = WatermarkPosition.TOP_LEFT,
                selected = selectedPosition == WatermarkPosition.TOP_LEFT,
                onClick = { onPositionChange(WatermarkPosition.TOP_LEFT) }
            )
            PositionItem(
                position = WatermarkPosition.TOP_CENTER,
                selected = selectedPosition == WatermarkPosition.TOP_CENTER,
                onClick = { onPositionChange(WatermarkPosition.TOP_CENTER) }
            )
            PositionItem(
                position = WatermarkPosition.TOP_RIGHT,
                selected = selectedPosition == WatermarkPosition.TOP_RIGHT,
                onClick = { onPositionChange(WatermarkPosition.TOP_RIGHT) }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Middle row
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PositionItem(
                position = WatermarkPosition.CENTER,
                selected = selectedPosition == WatermarkPosition.CENTER,
                onClick = { onPositionChange(WatermarkPosition.CENTER) }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Bottom row
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PositionItem(
                position = WatermarkPosition.BOTTOM_LEFT,
                selected = selectedPosition == WatermarkPosition.BOTTOM_LEFT,
                onClick = { onPositionChange(WatermarkPosition.BOTTOM_LEFT) }
            )
            PositionItem(
                position = WatermarkPosition.BOTTOM_CENTER,
                selected = selectedPosition == WatermarkPosition.BOTTOM_CENTER,
                onClick = { onPositionChange(WatermarkPosition.BOTTOM_CENTER) }
            )
            PositionItem(
                position = WatermarkPosition.BOTTOM_RIGHT,
                selected = selectedPosition == WatermarkPosition.BOTTOM_RIGHT,
                onClick = { onPositionChange(WatermarkPosition.BOTTOM_RIGHT) }
            )
        }
    }
}

@Composable
fun PositionItem(
    position: WatermarkPosition,
    selected: Boolean,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        shape = CircleShape,
        colors = CardDefaults.cardColors(
            containerColor = if (selected) HasselbladOrange.copy(alpha = 0.3f)
            else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Icon(
            imageVector = Icons.Default.Place,
            contentDescription = null,
            modifier = Modifier.padding(12.dp),
            tint = if (selected) HasselbladOrange
            else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private suspend fun loadBitmapFromUri(context: Context, uri: Uri): Bitmap? = withContext(Dispatchers.IO) {
    try {
        context.contentResolver.openInputStream(uri)?.use { input ->
            BitmapFactory.decodeStream(input)
        }
    } catch (e: Exception) {
        Timber.e(e, "Failed to load bitmap")
        null
    }
}

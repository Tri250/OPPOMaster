package com.omaster.app.ui.screens

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.omaster.app.ui.theme.HasselbladOrange
import com.omaster.app.ui.theme.OMasterSpacing
import com.omaster.app.watermark.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WatermarkEditorScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var selectedTemplate by remember { mutableStateOf(WatermarkTemplate.HASSELBLAD) }
    var customText by remember { mutableStateOf("") }
    var showTimestamp by remember { mutableStateOf(true) }
    var showDevice by remember { mutableStateOf(true) }
    var opacity by remember { mutableStateOf(0.8f) }
    var isProcessing by remember { mutableStateOf(false) }
    var processedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var showPreview by remember { mutableStateOf(false) }
    
    val watermarkProcessor = remember { WatermarkProcessor(context) }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        selectedImageUri = uri
        processedBitmap = null
        showPreview = false
    }
    
    val saveLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("image/jpeg")
    ) { uri ->
        uri?.let { outputUri ->
            processedBitmap?.let { bitmap ->
                scope.launch {
                    saveBitmapToUri(context, bitmap, outputUri)
                    snackbarHostState.showSnackbar("水印图片已保存")
                }
            }
        }
    }
    
    fun applyWatermark() {
        if (selectedImageUri == null) {
            scope.launch {
                snackbarHostState.showSnackbar("请先选择图片")
            }
            return
        }
        
        scope.launch {
            isProcessing = true
            try {
                val sourceBitmap = loadBitmapFromUri(context, selectedImageUri!!)
                
                val config = WatermarkConfig(
                    template = selectedTemplate,
                    opacity = opacity,
                    customText = if (selectedTemplate == WatermarkTemplate.CUSTOM) customText else null,
                    showTimestamp = showTimestamp,
                    showDevice = showDevice
                )
                
                val request = WatermarkProcessRequest(
                    sourceBitmap = sourceBitmap,
                    config = config
                )
                
                val result = watermarkProcessor.processWatermark(request)
                
                if (result.success && result.bitmap != null) {
                    processedBitmap = result.bitmap
                    showPreview = true
                    snackbarHostState.showSnackbar("水印应用成功")
                } else {
                    snackbarHostState.showSnackbar("水印应用失败: ${result.error}")
                }
            } catch (e: Exception) {
                snackbarHostState.showSnackbar("处理失败: ${e.message}")
            } finally {
                isProcessing = false
            }
        }
    }
    
    fun saveWatermarkedImage() {
        if (processedBitmap != null) {
            val fileName = "watermark_${System.currentTimeMillis()}.jpg"
            saveLauncher.launch(fileName)
        } else {
            scope.launch {
                snackbarHostState.showSnackbar("请先应用水印")
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "水印编辑",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(OMasterSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(OMasterSpacing.lg)
        ) {
            ImagePreviewCard(
                imageUri = selectedImageUri?.toString(),
                processedBitmap = processedBitmap,
                showPreview = showPreview,
                onPickImage = { imagePicker.launch("image/*") }
            )

            TemplateSelector(
                selectedTemplate = selectedTemplate,
                onTemplateSelected = { selectedTemplate = it }
            )

            WatermarkOptions(
                customText = customText,
                onCustomTextChange = { customText = it },
                showTimestamp = showTimestamp,
                onShowTimestampChange = { showTimestamp = it },
                showDevice = showDevice,
                onShowDeviceChange = { showDevice = it },
                opacity = opacity,
                onOpacityChange = { opacity = it }
            )

            Button(
                onClick = { applyWatermark() },
                enabled = selectedImageUri != null && !isProcessing,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = HasselbladOrange
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                if (isProcessing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.Black,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "处理中...",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "应用水印",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            if (processedBitmap != null) {
                OutlinedButton(
                    onClick = { saveWatermarkedImage() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Save,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "保存图片",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun ImagePreviewCard(
    imageUri: String?,
    processedBitmap: Bitmap?,
    showPreview: Boolean,
    onPickImage: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(4f / 3f),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(onClick = onPickImage),
            contentAlignment = Alignment.Center
        ) {
            if (showPreview && processedBitmap != null) {
                Image(
                    bitmap = processedBitmap.asImageBitmap(),
                    contentDescription = "Watermarked image",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "点击更换图片",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White
                    )
                }
            } else if (imageUri != null) {
                AsyncImage(
                    model = imageUri,
                    contentDescription = "Selected image",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(8.dp)
                        .background(
                            HasselbladOrange.copy(alpha = 0.8f),
                            RoundedCornerShape(4.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "原图",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Black,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AddPhotoAlternate,
                        contentDescription = "Add photo",
                        modifier = Modifier.size(48.dp),
                        tint = HasselbladOrange.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "点击添加照片",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun TemplateSelector(
    selectedTemplate: WatermarkTemplate,
    onTemplateSelected: (WatermarkTemplate) -> Unit
) {
    val templates = listOf(
        WatermarkTemplate.HASSELBLAD to "哈苏",
        WatermarkTemplate.OPPO to "OPPO",
        WatermarkTemplate.ONEPLUS to "一加",
        WatermarkTemplate.REALME to "真我",
        WatermarkTemplate.MINIMAL_PARAMS to "参数",
        WatermarkTemplate.TIMESTAMP to "时间",
        WatermarkTemplate.FILM_STYLE to "胶片",
        WatermarkTemplate.CUSTOM to "自定义"
    )

    Column {
        Text(
            text = "水印模板",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(12.dp))
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(templates) { (template, name) ->
                TemplateChip(
                    name = name,
                    isSelected = selectedTemplate == template,
                    onClick = { onTemplateSelected(template) }
                )
            }
        }
    }
}

@Composable
fun TemplateChip(
    name: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .then(
                if (isSelected) Modifier.border(
                    2.dp,
                    HasselbladOrange,
                    RoundedCornerShape(12.dp)
                ) else Modifier
            ),
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) {
            HasselbladOrange.copy(alpha = 0.15f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        }
    ) {
        Text(
            text = name,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) HasselbladOrange else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun WatermarkOptions(
    customText: String,
    onCustomTextChange: (String) -> Unit,
    showTimestamp: Boolean,
    onShowTimestampChange: (Boolean) -> Unit,
    showDevice: Boolean,
    onShowDeviceChange: (Boolean) -> Unit,
    opacity: Float,
    onOpacityChange: (Float) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(OMasterSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(OMasterSpacing.md)
        ) {
            Text(
                text = "水印选项",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("显示时间戳", style = MaterialTheme.typography.bodyMedium)
                Switch(
                    checked = showTimestamp,
                    onCheckedChange = onShowTimestampChange,
                    colors = SwitchDefaults.colors(
                        checkedTrackColor = HasselbladOrange
                    )
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("显示设备信息", style = MaterialTheme.typography.bodyMedium)
                Switch(
                    checked = showDevice,
                    onCheckedChange = onShowDeviceChange,
                    colors = SwitchDefaults.colors(
                        checkedTrackColor = HasselbladOrange
                    )
                )
            }

            Text(
                text = "透明度: ${(opacity * 100).toInt()}%",
                style = MaterialTheme.typography.bodyMedium
            )
            Slider(
                value = opacity,
                onValueChange = onOpacityChange,
                colors = SliderDefaults.colors(
                    thumbColor = HasselbladOrange,
                    activeTrackColor = HasselbladOrange
                )
            )

            OutlinedTextField(
                value = customText,
                onValueChange = onCustomTextChange,
                label = { Text("自定义文字") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )
        }
    }
}

private suspend fun loadBitmapFromUri(context: android.content.Context, uri: Uri): Bitmap =
    withContext(Dispatchers.IO) {
        val inputStream = context.contentResolver.openInputStream(uri)
        BitmapFactory.decodeStream(inputStream)
    }

private suspend fun saveBitmapToUri(context: android.content.Context, bitmap: Bitmap, uri: Uri) =
    withContext(Dispatchers.IO) {
        val outputStream = context.contentResolver.openOutputStream(uri)
        bitmap.compress(Bitmap.CompressFormat.JPEG, 95, outputStream)
        outputStream?.flush()
        outputStream?.close()
    }

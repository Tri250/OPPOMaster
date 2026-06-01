package com.omaster.app.ui.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.omaster.app.model.Preset
import com.omaster.app.screenshot.*
import com.omaster.app.ui.theme.*
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun ScreenshotShareDialog(
    preset: Preset,
    onDismiss: () -> Unit,
    onScreenshotSaved: (Uri) -> Unit = {},
    onScreenshotShared: (Intent) -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    
    var selectedAspectRatio by remember { mutableStateOf(ScreenshotAspectRatio.SQUARE) }
    var selectedWatermarkStyle by remember { mutableStateOf(WatermarkStyle.HASSELBLAD) }
    var previewBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isGenerating by remember { mutableStateOf(false) }
    var showStoragePermissionRequest by remember { mutableStateOf(false) }
    
    val screenshotGenerator = remember { PresetScreenshotGenerator(context) }
    val screenshotService = remember { ScreenshotService(context) }

    val storagePermissionLauncher = rememberStoragePermissionLauncher(
        onPermissionGranted = {
            scope.launch {
                generateAndSaveScreenshot(
                    preset,
                    selectedAspectRatio,
                    selectedWatermarkStyle,
                    screenshotGenerator,
                    screenshotService,
                    snackbarHostState,
                    onScreenshotSaved
                )
            }
        },
        onPermissionDenied = {
            scope.launch {
                snackbarHostState.showSnackbar("需要存储权限")
            }
        }
    )

    LaunchedEffect(selectedAspectRatio, selectedWatermarkStyle) {
        // Generate preview
        previewBitmap = generatePreviewBitmap(preset, selectedWatermarkStyle, selectedAspectRatio)
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
                    text = "生成预设截图",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "关闭")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Preview
            previewBitmap?.let { bitmap ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "预览",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(selectedAspectRatio.ratio)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Aspect Ratio Selection
            Text(
                text = "选择比例",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AspectRatioOption(
                    text = "1:1",
                    selected = selectedAspectRatio == ScreenshotAspectRatio.SQUARE,
                    onClick = { selectedAspectRatio = ScreenshotAspectRatio.SQUARE }
                )
                AspectRatioOption(
                    text = "16:9",
                    selected = selectedAspectRatio == ScreenshotAspectRatio.WIDE_16_9,
                    onClick = { selectedAspectRatio = ScreenshotAspectRatio.WIDE_16_9 }
                )
                AspectRatioOption(
                    text = "9:16",
                    selected = selectedAspectRatio == ScreenshotAspectRatio.TALL_9_16,
                    onClick = { selectedAspectRatio = ScreenshotAspectRatio.TALL_9_16 }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Watermark Style Selection
            Text(
                text = "水印样式",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                WatermarkStyleOption(
                    style = WatermarkStyle.MINIMAL,
                    text = "简约",
                    selected = selectedWatermarkStyle == WatermarkStyle.MINIMAL,
                    onClick = { selectedWatermarkStyle = it }
                )
                WatermarkStyleOption(
                    style = WatermarkStyle.HASSELBLAD,
                    text = "哈苏",
                    selected = selectedWatermarkStyle == WatermarkStyle.HASSELBLAD,
                    onClick = { selectedWatermarkStyle = it }
                )
                WatermarkStyleOption(
                    style = WatermarkStyle.BRANDED,
                    text = "品牌",
                    selected = selectedWatermarkStyle == WatermarkStyle.BRANDED,
                    onClick = { selectedWatermarkStyle = it }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        if (!hasStoragePermission(context)) {
                            showStoragePermissionRequest = true
                        } else {
                            scope.launch {
                                generateAndSaveScreenshot(
                                    preset,
                                    selectedAspectRatio,
                                    selectedWatermarkStyle,
                                    screenshotGenerator,
                                    screenshotService,
                                    snackbarHostState,
                                    onScreenshotSaved
                                )
                            }
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Download, contentDescription = "保存")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("保存到相册")
                }

                Button(
                    onClick = {
                        scope.launch {
                            val screenshotFile = generateScreenshot(
                                preset,
                                selectedAspectRatio,
                                selectedWatermarkStyle,
                                screenshotGenerator,
                                snackbarHostState
                            )
                            screenshotFile?.let {
                                val shareIntent = screenshotService.getShareIntent(it)
                                onScreenshotShared(shareIntent)
                            }
                        }
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = HasselbladOrange)
                ) {
                    Icon(Icons.Default.Share, contentDescription = "分享")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("分享")
                }
            }
        }
    }
}

@Composable
fun AspectRatioOption(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = if (selected) HasselbladOrange.copy(alpha = 0.2f)
            else MaterialTheme.colorScheme.surfaceVariant,
            contentColor = if (selected) HasselbladOrange
            else MaterialTheme.colorScheme.onSurfaceVariant
        )
    ) {
        Text(text)
    }
}

@Composable
fun WatermarkStyleOption(
    style: WatermarkStyle,
    text: String,
    selected: Boolean,
    onClick: (WatermarkStyle) -> Unit
) {
    Card(
        onClick = { onClick(style) },
        colors = CardDefaults.cardColors(
            containerColor = if (selected) HasselbladOrange.copy(alpha = 0.2f)
            else MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = selected,
                onClick = { onClick(style) },
                colors = RadioButtonDefaults.colors(selectedColor = HasselbladOrange)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

private fun generatePreviewBitmap(
    preset: Preset,
    style: WatermarkStyle,
    ratio: ScreenshotAspectRatio
): Bitmap {
    val config = Bitmap.Config.ARGB_8888
    val preview = Bitmap.createBitmap(ratio.width / 4, ratio.height / 4, config)
    val canvas = Canvas(preview)
    canvas.drawARGB(255, 18, 18, 18)
    return preview
}

private suspend fun generateScreenshot(
    preset: Preset,
    ratio: ScreenshotAspectRatio,
    style: WatermarkStyle,
    generator: PresetScreenshotGenerator,
    snackbarHostState: SnackbarHostState
): File? {
    return try {
        val data = PresetScreenshotData(
            presetName = preset.name,
            coverImage = null, // We'd load the actual image in a real app
            iso = preset.cameraParams?.iso ?: 0,
            shutterSpeed = preset.cameraParams?.shutter ?: "auto",
            ev = preset.cameraParams?.ev ?: "0",
            whiteBalance = preset.cameraParams?.wb ?: "auto",
            filter = preset.cameraParams?.filter,
            watermarkStyle = style
        )
        generator.generateScreenshot(data, ratio)
    } catch (e: Exception) {
        Timber.e(e, "Failed to generate screenshot")
        null
    }
}

private suspend fun generateAndSaveScreenshot(
    preset: Preset,
    ratio: ScreenshotAspectRatio,
    style: WatermarkStyle,
    generator: PresetScreenshotGenerator,
    service: ScreenshotService,
    snackbarHostState: SnackbarHostState,
    onSaved: (Uri) -> Unit
) {
    try {
        val screenshotFile = generateScreenshot(preset, ratio, style, generator, snackbarHostState)
        screenshotFile?.let {
            val uri = service.saveScreenshotToGallery(it, preset.name)
            uri?.let { savedUri ->
                snackbarHostState.showSnackbar("已保存到相册")
                onSaved(savedUri)
            }
        }
    } catch (e: Exception) {
        Timber.e(e, "Failed to save screenshot")
        snackbarHostState.showSnackbar("保存失败")
    }
}

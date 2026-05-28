package com.omaster.app.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.omaster.app.camera.CameraCompatibilityStatus
import com.omaster.app.camera.RealTimeCameraParams
import com.omaster.app.model.Preset
import com.omaster.app.ui.components.CameraPermissionRequester
import com.omaster.app.ui.components.ParamComparisonDisplay
import com.omaster.app.ui.components.RealTimeCameraParamsDisplay
import com.omaster.app.ui.components.ScreenshotShareDialog
import com.omaster.app.ui.theme.*
import com.omaster.app.viewmodel.MainViewModel
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DetailScreen(
    preset: Preset,
    onBack: () -> Unit,
    onFavoriteToggle: () -> Unit,
    onApplyPreset: (Preset) -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: MainViewModel = androidx.hilt.navigation.compose.hiltViewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val cameraStatus by viewModel.cameraStatus.observeAsState(
        initial = CameraCompatibilityStatus.NotSupported
    )
    val cameraParams by viewModel.cameraParams.observeAsState(
        initial = RealTimeCameraParams()
    )
    var showCameraParams by remember { mutableStateOf(false) }
    var showScreenshotDialog by remember { mutableStateOf(false) }
    var showApplyGuideDialog by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.stopCameraMonitor()
        }
    }

    fun copyAllParameters(preset: Preset): String {
        val params = preset.cameraParams ?: return ""
        return buildString {
            appendLine("📷 ${preset.name}")
            appendLine("适配设备: ${preset.deviceModel ?: "通用"}")
            appendLine()
            appendLine("相机参数:")
            appendLine("• ISO: ${params.iso}")
            appendLine("• 快门: ${params.shutter}")
            appendLine("• 曝光补偿: ${params.ev}")
            appendLine("• 白平衡: ${params.wb}")
            if (params.filter.isNotEmpty()) {
                appendLine("• 滤镜: ${params.filter}")
            }
            if (params.hasselblad_hncs) {
                appendLine("• HNCS: ✓ 哈苏自然色彩解决方案")
            }
            appendLine()
            preset.sections.firstOrNull()?.let {
                appendLine("说明: ${it.content}")
            }
        }
    }

    fun copyToClipboard(text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("OMaster预设参数", text)
        clipboard.setPrimaryClip(clip)
    }

    fun sharePreset(preset: Preset) {
        val shareText = copyAllParameters(preset)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, shareText)
            putExtra(Intent.EXTRA_SUBJECT, "分享OMaster预设: ${preset.name}")
        }
        context.startActivity(Intent.createChooser(intent, "分享预设"))
    }

    fun openCameraSettings() {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_SETTINGS)
            context.startActivity(intent)
        } catch (e: Exception) {
            val intent = Intent(Settings.ACTION_SETTINGS)
            context.startActivity(intent)
        }
    }

    fun openSystemCamera() {
        try {
            val intent = Intent(Settings.ACTION_CAMERA_SETTINGS)
            context.startActivity(intent)
        } catch (e: Exception) {
            try {
                val intent = Intent(Settings.ACTION_APPLICATION_SETTINGS)
                context.startActivity(intent)
            } catch (e2: Exception) {
                scope.launch {
                    snackbarHostState.showSnackbar("无法打开相机设置，请手动设置")
                }
            }
        }
    }

    fun saveParameterCard(preset: Preset) {
        scope.launch {
            try {
                val bitmap = Bitmap.createBitmap(1080, 1920, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bitmap)
                
                canvas.drawColor(android.graphics.Color.parseColor("#1A1A1A"))
                
                val titlePaint = Paint().apply {
                    color = android.graphics.Color.WHITE
                    textSize = 60f
                    typeface = Typeface.DEFAULT_BOLD
                    isAntiAlias = true
                }
                
                val textPaint = Paint().apply {
                    color = android.graphics.Color.parseColor("#CCCCCC")
                    textSize = 40f
                    isAntiAlias = true
                }
                
                val accentPaint = Paint().apply {
                    color = android.graphics.Color.parseColor("#FF6B35")
                    textSize = 45f
                    typeface = Typeface.DEFAULT_BOLD
                    isAntiAlias = true
                }
                
                var y = 200f
                canvas.drawText(preset.name, 60f, y, titlePaint)
                y += 100f
                
                preset.deviceModel?.let {
                    canvas.drawText("适配: $it", 60f, y, textPaint)
                    y += 80f
                }
                
                y += 60f
                canvas.drawText("相机参数", 60f, y, accentPaint)
                y += 80f
                
                preset.cameraParams?.let { params ->
                    canvas.drawText("ISO: ${params.iso}", 80f, y, textPaint)
                    y += 60f
                    canvas.drawText("快门: ${params.shutter}", 80f, y, textPaint)
                    y += 60f
                    canvas.drawText("曝光补偿: ${params.ev}", 80f, y, textPaint)
                    y += 60f
                    canvas.drawText("白平衡: ${params.wb}", 80f, y, textPaint)
                    y += 60f
                    if (params.filter.isNotEmpty()) {
                        canvas.drawText("滤镜: ${params.filter}", 80f, y, textPaint)
                        y += 60f
                    }
                }
                
                y += 60f
                preset.sections.firstOrNull()?.let {
                    canvas.drawText(it.content.take(50), 60f, y, textPaint)
                }
                
                y = 1800f
                val footerPaint = Paint().apply {
                    color = android.graphics.Color.parseColor("#666666")
                    textSize = 30f
                    isAntiAlias = true
                }
                canvas.drawText("OMaster - 哈苏影像参数专家", 60f, y, footerPaint)
                
                val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                val filename = "OMaster_${preset.name}_$timestamp.jpg"
                val file = File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), filename)
                
                FileOutputStream(file).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
                }
                
                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )
                
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "image/jpeg"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    putExtra(Intent.EXTRA_TEXT, "OMaster预设参数卡片: ${preset.name}")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(shareIntent, "保存并分享"))
                
                snackbarHostState.showSnackbar("参数卡片已保存")
            } catch (e: Exception) {
                snackbarHostState.showSnackbar("保存失败: ${e.message}")
            }
        }
    }

    if (showApplyGuideDialog) {
        AlertDialog(
            onDismissRequest = { showApplyGuideDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = AccentPrimary,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = "应用影像预设",
                    style = MaterialTheme.typography.headlineSmall
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "将预设参数应用到相机需要以下步骤：",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "📱 手动操作方式：",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            
                            StepItem(number = 1, text = "打开手机相机应用")
                            StepItem(number = 2, text = "进入专业/手动模式 (M)")
                            StepItem(number = 3, text = "根据预设参数手动调整")
                        }
                    }
                    
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = AccentPrimary.copy(alpha = 0.1f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Tips,
                                contentDescription = null,
                                tint = AccentPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                text = "提示：OPPO Find系列支持专业模式参数预设导入",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showApplyGuideDialog = false
                        openSystemCamera()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AccentPrimary
                    )
                ) {
                    Text("打开相机", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showApplyGuideDialog = false }) {
                    Text("知道了")
                }
            }
        )
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                ),
                actions = {
                    IconButton(
                        onClick = {
                            copyToClipboard(copyAllParameters(preset))
                            scope.launch {
                                snackbarHostState.showSnackbar("参数已复制到剪贴板")
                            }
                        },
                        modifier = Modifier.semantics {
                            contentDescription = "复制参数"
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "复制参数",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    
                    IconButton(
                        onClick = { saveParameterCard(preset) },
                        modifier = Modifier.semantics {
                            contentDescription = "保存参数卡片"
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.SaveAlt,
                            contentDescription = "保存",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    
                    IconButton(
                        onClick = onFavoriteToggle,
                        modifier = Modifier.semantics {
                            contentDescription = if (preset.isFavorite) "取消收藏" else "收藏"
                        }
                    ) {
                        Icon(
                            imageVector = if (preset.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = if (preset.isFavorite) "取消收藏" else "收藏",
                            tint = if (preset.isFavorite) AccentPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(
                        onClick = { sharePreset(preset) },
                        modifier = Modifier.semantics {
                            contentDescription = "分享预设"
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "分享",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
            ) {
                AsyncImage(
                    model = "https://picsum.photos/seed/${preset.coverPath}/800/600",
                    contentDescription = preset.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    MaterialTheme.colorScheme.background
                                ),
                                startY = 200f
                            )
                        )
                )

                if (preset.cameraParams?.hasselblad_hncs == true) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(16.dp),
                        color = HasselbladOrange,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "HNCS",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = DeepSpace,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = preset.name,
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Spacer(modifier = Modifier.height(8.dp))

                preset.deviceModel?.let { deviceModel ->
                    if (deviceModel.isNotEmpty()) {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = "适配: $deviceModel",
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                preset.cameraParams?.let { params ->
                    Text(
                        text = "相机参数",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    GridParamsGrid(params)

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = { showCameraParams = !showCameraParams },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = if (showCameraParams) "隐藏实时参数" else "查看实时参数",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    if (showCameraParams) {
                        when (cameraStatus) {
                            CameraCompatibilityStatus.PermissionRequired -> {
                                CameraPermissionRequester(
                                    onPermissionGranted = {
                                        viewModel.startCameraMonitor()
                                    },
                                    onPermissionDenied = {
                                        scope.launch {
                                            snackbarHostState.showSnackbar("相机权限被拒绝")
                                        }
                                    }
                                )
                            }
                            else -> {
                                RealTimeCameraParamsDisplay(
                                    status = cameraStatus,
                                    params = cameraParams,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }

                if (preset.sections.isNotEmpty()) {
                    Text(
                        text = "详细说明",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    preset.sections.forEach { section ->
                        SectionItem(section)
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = { showScreenshotDialog = true },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.PhotoCamera, contentDescription = "生成截图")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("生成截图")
                    }
                    
                    Button(
                        onClick = {
                            showApplyGuideDialog = true
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AccentPrimary
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = null,
                            tint = DeepSpace,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "应用影像",
                            style = MaterialTheme.typography.titleLarge,
                            color = DeepSpace,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
    
    if (showScreenshotDialog) {
        AlertDialog(
            onDismissRequest = { showScreenshotDialog = false },
            title = { },
            text = {
                ScreenshotShareDialog(
                    preset = preset,
                    onDismiss = { showScreenshotDialog = false }
                )
            },
            confirmButton = { },
            dismissButton = { },
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
private fun StepItem(number: Int, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(50),
            color = AccentPrimary,
            modifier = Modifier.size(24.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = number.toString(),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
fun GridParamsGrid(params: com.omaster.app.model.CameraParams) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ParamItem("ISO", params.iso.toString())
        ParamItem("快门", params.shutter)
        ParamItem("曝光补偿", params.ev)
        ParamItem("白平衡", params.wb)
        if (params.filter.isNotEmpty()) {
            ParamItem("滤镜", params.filter)
        }
    }
}

@Composable
fun ParamItem(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            color = AccentPrimary,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun SectionItem(section: com.omaster.app.model.Section) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = section.title,
                style = MaterialTheme.typography.titleLarge,
                color = AccentPrimary,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = section.content,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

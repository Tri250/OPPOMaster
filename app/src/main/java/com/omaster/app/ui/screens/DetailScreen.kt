package com.omaster.app.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.omaster.app.R
import com.omaster.app.accessibility.AutoFillAccessibilityService
import com.omaster.app.accessibility.FillResult
import com.omaster.app.floating.FloatingWindowManager
import com.omaster.app.model.CameraParams
import com.omaster.app.model.Preset
import com.omaster.app.ui.theme.*
import com.omaster.app.viewmodel.MainViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun DetailScreen(
    preset: Preset,
    onBack: () -> Unit,
    onFavoriteToggle: () -> Unit,
    onApplyPreset: (Preset) -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: MainViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // 状态管理
    var showApplyGuideDialog by remember { mutableStateOf(false) }
    var showAccessibilityGuideDialog by remember { mutableStateOf(false) }
    var showScreenshotDialog by remember { mutableStateOf(false) }
    var showPresetImportExportDialog by remember { mutableStateOf(false) }
    var isFilling by remember { mutableStateOf(false) }
    var fillResult by remember { mutableStateOf<FillResult?>(null) }
    var showResultToast by remember { mutableStateOf(false) }
    
    // 检查无障碍服务是否启用
    val isAccessibilityEnabled = remember {
        mutableStateOf(AutoFillAccessibilityService.isServiceEnabled(context))
    }

    // 设置填入结果监听器
    DisposableEffect(Unit) {
        AutoFillAccessibilityService.setFillResultListener { result ->
            fillResult = result
            isFilling = false
            showResultToast = true
        }
        onDispose {
            AutoFillAccessibilityService.setFillResultListener(null)
        }
    }

    // 显示结果提示
    LaunchedEffect(showResultToast) {
        if (showResultToast) {
            delay(3000)
            showResultToast = false
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            DetailTopAppBar(
                onBack = onBack,
                onFavoriteToggle = onFavoriteToggle,
                onCopy = { copyAllParamsToClipboard(context, preset) },
                onShare = { sharePreset(context, preset) },
                isFavorite = preset.isFavorite
            )
        },
        containerColor = DeepSpace
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                // 封面图片区域
                PresetCoverImage(
                    coverPath = preset.coverPath,
                    isHNCS = preset.cameraParams?.hasselblad_hncs == true
                )

                // 内容区域
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    // 标题和设备适配
                    PresetHeader(
                        title = preset.name,
                        deviceModel = preset.deviceModel
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // 相机参数网格
                    preset.cameraParams?.let { params ->
                        CameraParamsGrid(params)
                    }

                    // 详细说明
                    if (preset.sections.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(24.dp))
                        DetailedSections(preset.sections)
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    // 主要操作按钮
                    PrimaryActionButtons(
                        isAccessibilityEnabled = isAccessibilityEnabled.value,
                        isFilling = isFilling,
                        onOneClickFill = {
                            preset.cameraParams?.let { params ->
                                handleOneClickFill(
                                    context = context,
                                    params = params,
                                    isAccessibilityEnabled = isAccessibilityEnabled.value,
                                    showAccessibilityGuide = { showAccessibilityGuideDialog = true },
                                    showApplyGuide = { showApplyGuideDialog = true },
                                    startFilling = { isFilling = true }
                                )
                            }
                        },
                        onOpenFloatingWindow = {
                            preset.cameraParams?.let { params ->
                                AutoFillAccessibilityService.setParams(params)
                            }
                            FloatingWindowManager.showWindow(context)
                        }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // 次要操作按钮
                    SecondaryActionButtons(
                        onImportExport = { showPresetImportExportDialog = true },
                        onGenerateScreenshot = { showScreenshotDialog = true }
                    )

                    Spacer(modifier = Modifier.height(32.dp))
                }
            }

            // 结果提示条
            AnimatedVisibility(
                visible = showResultToast && fillResult != null,
                enter = slideInVertically() + fadeIn(),
                exit = slideOutVertically() + fadeOut()
            ) {
                FillResultToast(
                    result = fillResult!!,
                    onDismiss = { showResultToast = false }
                )
            }
        }
    }

    // 对话框
    if (showApplyGuideDialog) {
        ApplyGuideDialog(
            preset = preset,
            onOpenCamera = {
                openSystemCamera(context)
                showApplyGuideDialog = false
            },
            onDismiss = { showApplyGuideDialog = false }
        )
    }

    if (showAccessibilityGuideDialog) {
        AccessibilityGuideDialog(
            onOpenSettings = {
                AutoFillAccessibilityService.openAccessibilitySettings(context)
                showAccessibilityGuideDialog = false
            },
            onDismiss = { showAccessibilityGuideDialog = false }
        )
    }

    if (showPresetImportExportDialog) {
        PresetImportExportDialog(
            preset = preset,
            onExport = { exportPreset(context, preset) },
            onImport = { /* TODO */ },
            onDismiss = { showPresetImportExportDialog = false }
        )
    }

    if (showScreenshotDialog) {
        ScreenshotDialog(
            onDismiss = { showScreenshotDialog = false }
        )
    }
}

/**
 * 顶部应用栏
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DetailTopAppBar(
    onBack: () -> Unit,
    onFavoriteToggle: () -> Unit,
    onCopy: () -> Unit,
    onShare: () -> Unit,
    isFavorite: Boolean
) {
    TopAppBar(
        title = { },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "返回",
                    tint = TextPrimary
                )
            }
        },
        actions = {
            IconButton(onClick = onCopy) {
                Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = "复制参数",
                    tint = TextPrimary
                )
            }
            IconButton(onClick = onFavoriteToggle) {
                Icon(
                    imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = if (isFavorite) "取消收藏" else "收藏",
                    tint = if (isFavorite) OppoPrimary else TextSecondary
                )
            }
            IconButton(onClick = onShare) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = "分享",
                    tint = TextPrimary
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = DeepSpace
        )
    )
}

/**
 * 预设封面图片
 */
@Composable
private fun PresetCoverImage(
    coverPath: String,
    isHNCS: Boolean
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(280.dp)
    ) {
        AsyncImage(
            model = "https://picsum.photos/seed/$coverPath/800/600",
            contentDescription = "预设封面",
            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // 渐变遮罩
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            DeepSpace.copy(alpha = 0.3f),
                            DeepSpace.copy(alpha = 0.95f)
                        ),
                        startY = 100f,
                        endY = 400f
                    )
                )
        )

        // HNCS标签
        if (isHNCS) {
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
}

/**
 * 预设标题区域
 */
@Composable
private fun PresetHeader(
    title: String,
    deviceModel: String?
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            color = TextPrimary,
            fontWeight = FontWeight.Bold
        )

        deviceModel?.let { model ->
            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                color = CardBackground,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "适配: $model",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary
                )
            }
        }
    }
}

/**
 * 相机参数网格
 */
@Composable
private fun CameraParamsGrid(params: CameraParams) {
    Column {
        Text(
            text = "相机参数",
            style = MaterialTheme.typography.titleMedium,
            color = TextPrimary,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(16.dp))

        // ISO
        ParamItem(
            label = "ISO",
            value = params.iso.toString(),
            icon = Icons.Default.Exposure
        )

        // 快门
        ParamItem(
            label = "快门",
            value = params.shutter,
            icon = Icons.Default.Timer
        )

        // 曝光补偿
        ParamItem(
            label = "曝光补偿",
            value = params.ev,
            icon = Icons.Default.ExposurePlus1
        )

        // 白平衡
        ParamItem(
            label = "白平衡",
            value = params.wb ?: "自动",
            icon = Icons.Default.BrightnessMedium
        )

        // 滤镜
        if (params.filter.isNotEmpty()) {
            ParamItem(
                label = "滤镜",
                value = params.filter,
                icon = Icons.Default.Brush
            )
        }
    }
}

/**
 * 单个参数项
 */
@Composable
private fun ParamItem(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        color = CardBackground,
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    color = OppoPrimary.copy(alpha = 0.15f),
                    shape = CircleShape
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = OppoPrimary,
                        modifier = Modifier.padding(8.dp)
                    )
                }
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary
                )
            }
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                color = OppoPrimary,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/**
 * 详细说明区域
 */
@Composable
private fun DetailedSections(sections: List<com.omaster.app.model.Section>) {
    Column {
        Text(
            text = "详细说明",
            style = MaterialTheme.typography.titleMedium,
            color = TextPrimary,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(16.dp))

        sections.forEach { section ->
            SectionItem(section)
        }
    }
}

/**
 * 单个说明项
 */
@Composable
private fun SectionItem(section: com.omaster.app.model.Section) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        color = CardBackground,
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = section.title,
                style = MaterialTheme.typography.titleMedium,
                color = HasselbladOrange,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = section.content,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
        }
    }
}

/**
 * 主要操作按钮
 */
@Composable
private fun PrimaryActionButtons(
    isAccessibilityEnabled: Boolean,
    isFilling: Boolean,
    onOneClickFill: () -> Unit,
    onOpenFloatingWindow: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 一键自动填入按钮（主按钮）
        Button(
            onClick = onOneClickFill,
            modifier = Modifier.weight(1f),
            enabled = !isFilling,
            colors = ButtonDefaults.buttonColors(
                containerColor = OppoPrimary,
                disabledContainerColor = OppoPrimary.copy(alpha = 0.5f)
            ),
            shape = RoundedCornerShape(16.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            if (isFilling) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = DeepSpace,
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "填入中...",
                    style = MaterialTheme.typography.titleLarge,
                    color = DeepSpace,
                    fontWeight = FontWeight.Bold
                )
            } else {
                Icon(
                    imageVector = Icons.Default.AutoFixHigh,
                    contentDescription = null,
                    tint = DeepSpace,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isAccessibilityEnabled) "一键自动填入" else "启用服务后填入",
                    style = MaterialTheme.typography.titleLarge,
                    color = DeepSpace,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // 悬浮窗按钮
        OutlinedButton(
            onClick = onOpenFloatingWindow,
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(16.dp),
            contentPadding = PaddingValues(vertical = 16.dp),
            border = androidx.compose.foundation.BorderStroke(
                width = 1.dp,
                color = OppoPrimary.copy(alpha = 0.5f)
            )
        ) {
            Icon(
                imageVector = Icons.Default.PictureInPicture,
                contentDescription = null,
                tint = OppoPrimary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "悬浮窗",
                style = MaterialTheme.typography.titleMedium,
                color = OppoPrimary
            )
        }
    }
}

/**
 * 次要操作按钮
 */
@Composable
private fun SecondaryActionButtons(
    onImportExport: () -> Unit,
    onGenerateScreenshot: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedButton(
            onClick = onImportExport,
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(16.dp),
            contentPadding = PaddingValues(vertical = 14.dp)
        ) {
            Icon(
                imageVector = Icons.Default.FileDownload,
                contentDescription = null,
                tint = TextSecondary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "导入/导出",
                style = MaterialTheme.typography.titleSmall,
                color = TextSecondary
            )
        }

        OutlinedButton(
            onClick = onGenerateScreenshot,
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(16.dp),
            contentPadding = PaddingValues(vertical = 14.dp)
        ) {
            Icon(
                imageVector = Icons.Default.PhotoCamera,
                contentDescription = null,
                tint = TextSecondary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "生成截图",
                style = MaterialTheme.typography.titleSmall,
                color = TextSecondary
            )
        }
    }
}

/**
 * 填入结果提示条
 */
@Composable
private fun FillResultToast(
    result: FillResult,
    onDismiss: () -> Unit
) {
    val (backgroundColor, text, icon) = when (result) {
        FillResult.SUCCESS -> Triple(
            OppoGreen,
            "参数填入成功！",
            Icons.Default.CheckCircle
        )
        FillResult.PARTIAL_SUCCESS -> Triple(
            HasselbladOrange,
            "部分参数填入成功",
            Icons.Default.Warning
        )
        else -> Triple(
            ErrorRed,
            "参数填入失败，请重试",
            Icons.Default.Error
        )
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        color = backgroundColor,
        shape = RoundedCornerShape(16.dp),
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = DeepSpace,
                modifier = Modifier.size(24.dp)
            )
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium,
                color = DeepSpace,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "关闭",
                    tint = DeepSpace.copy(alpha = 0.7f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

/**
 * 处理一键填入操作
 */
private fun handleOneClickFill(
    context: Context,
    params: CameraParams,
    isAccessibilityEnabled: Boolean,
    showAccessibilityGuide: () -> Unit,
    showApplyGuide: () -> Unit,
    startFilling: () -> Unit
) {
    if (!isAccessibilityEnabled) {
        showAccessibilityGuide()
    } else {
        // 设置参数
        AutoFillAccessibilityService.setParams(params)
        
        // 尝试打开相机
        val intent = context.packageManager
            .getLaunchIntentForPackage("com.oppo.camera")
            ?: context.packageManager.getLaunchIntentForPackage("com.coloros.camera")
            ?: Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE)
        
        try {
            context.startActivity(intent)
            startFilling()
        } catch (e: Exception) {
            showApplyGuide()
        }
    }
}

/**
 * 应用预设引导对话框
 */
@Composable
private fun ApplyGuideDialog(
    preset: Preset,
    onOpenCamera: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "一键自动填入",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "点击下方按钮打开相机，将自动填入预设参数",
                    style = MaterialTheme.typography.bodyMedium
                )
                
                Surface(
                    color = CardBackground,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "操作步骤（仅需2步）",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "1. 点击「打开相机」按钮\n2. 参数将自动填入",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onOpenCamera,
                colors = ButtonDefaults.buttonColors(
                    containerColor = OppoPrimary
                )
            ) {
                Text("打开相机")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        },
        containerColor = CardBackground
    )
}

/**
 * 无障碍服务引导对话框
 */
@Composable
private fun AccessibilityGuideDialog(
    onOpenSettings: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "需要启用无障碍服务",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "为了实现相机参数自动填入，需要启用小O帮帮的无障碍服务",
                    style = MaterialTheme.typography.bodyMedium
                )
                
                Surface(
                    color = OppoPrimary.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "安全说明",
                            style = MaterialTheme.typography.titleMedium,
                            color = OppoPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "• 无障碍服务仅用于识别相机界面\n• 不会收集您的任何隐私数据\n• 可以随时在系统设置中关闭",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onOpenSettings,
                colors = ButtonDefaults.buttonColors(
                    containerColor = OppoPrimary
                )
            ) {
                Text("去开启服务")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("稍后再说")
            }
        },
        containerColor = CardBackground
    )
}

/**
 * 预设导入导出对话框
 */
@Composable
private fun PresetImportExportDialog(
    preset: Preset,
    onExport: () -> Unit,
    onImport: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "导入/导出预设",
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Surface(
                    color = CardBackground,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "支持格式",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "• LUT文件（.cube）\n• 泼辣修图预设\n• Lightroom手机版预设\n• JSON格式",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onExport) {
                Text("导出预设")
            }
        },
        dismissButton = {
            TextButton(onClick = onImport) {
                Text("导入预设")
            }
        },
        containerColor = CardBackground
    )
}

/**
 * 截图对话框
 */
@Composable
private fun ScreenshotDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("生成参数截图") },
        text = { Text("正在生成截图...") },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("确定")
            }
        },
        containerColor = CardBackground
    )
}

/**
 * 复制所有参数到剪贴板
 */
private fun copyAllParamsToClipboard(context: Context, preset: Preset) {
    val params = preset.cameraParams ?: return
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val text = buildString {
        appendLine("📷 ${preset.name}")
        appendLine()
        appendLine("ISO: ${params.iso}")
        appendLine("快门: ${params.shutter}")
        appendLine("曝光补偿: ${params.ev}")
        appendLine("白平衡: ${params.wb}")
        if (params.filter.isNotEmpty()) {
            appendLine("滤镜: ${params.filter}")
        }
    }
    val clip = ClipData.newPlainText("预设参数", text)
    clipboard.setPrimaryClip(clip)
}

/**
 * 分享预设
 */
private fun sharePreset(context: Context, preset: Preset) {
    val params = preset.cameraParams ?: return
    val shareText = buildString {
        appendLine("📷 ${preset.name}")
        appendLine()
        appendLine("ISO: ${params.iso}")
        appendLine("快门: ${params.shutter}")
        appendLine("曝光补偿: ${params.ev}")
        appendLine("白平衡: ${params.wb}")
        if (params.filter.isNotEmpty()) {
            appendLine("滤镜: ${params.filter}")
        }
    }
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, shareText)
        putExtra(Intent.EXTRA_SUBJECT, "分享预设：${preset.name}")
    }
    context.startActivity(Intent.createChooser(intent, "分享预设"))
}

/**
 * 导出预设
 */
private fun exportPreset(context: Context, preset: Preset) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, "预设导出功能开发中...")
    }
    context.startActivity(Intent.createChooser(intent, "导出预设"))
}

/**
 * 打开系统相机
 */
private fun openSystemCamera(context: Context) {
    val intent = context.packageManager
        .getLaunchIntentForPackage("com.oppo.camera")
        ?: context.packageManager.getLaunchIntentForPackage("com.coloros.camera")
        ?: Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE)
    
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    
    try {
        context.startActivity(intent)
    } catch (e: Exception) {
        val fallbackIntent = Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE)
        fallbackIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(fallbackIntent)
    }
}

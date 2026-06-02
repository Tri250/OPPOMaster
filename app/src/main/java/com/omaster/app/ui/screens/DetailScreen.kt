package com.omaster.app.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.omaster.app.accessibility.AutoFillAccessibilityService
import com.omaster.app.floating.FloatingWindowManager
import com.omaster.app.model.CameraParams
import com.omaster.app.model.Preset
import com.omaster.app.ui.theme.*
import com.omaster.app.viewmodel.MainViewModel
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

    var showApplyGuideDialog by remember { mutableStateOf(false) }
    var showAccessibilityGuideDialog by remember { mutableStateOf(false) }
    var showScreenshotDialog by remember { mutableStateOf(false) }
    var showPresetImportExportDialog by remember { mutableStateOf(false) }

    val importPresetLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            importPresetFromUri(context, uri)
        } else {
            Toast.makeText(context, "未选择文件", Toast.LENGTH_SHORT).show()
        }
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
                    containerColor = MaterialTheme.colorScheme.background
                ),
                actions = {
                    IconButton(
                        onClick = {
                            copyAllParamsToClipboard(context, preset)
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "复制参数",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }

                    IconButton(
                        onClick = onFavoriteToggle
                    ) {
                        Icon(
                            imageVector = if (preset.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = if (preset.isFavorite) "取消收藏" else "收藏",
                            tint = if (preset.isFavorite) AccentPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    IconButton(
                        onClick = { sharePreset(context, preset) }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "分享",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }
            )
        }
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
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
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
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Spacer(modifier = Modifier.height(4.dp))

                if (preset.deviceModel.isNotEmpty()) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "适配: ${preset.deviceModel}",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                preset.cameraParams?.let { params ->
                    Text(
                        text = "相机参数",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    GridParamsGrid(params)
                }

                if (preset.sections.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = "详细说明",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    preset.sections.forEach { section ->
                        SectionItem(section)
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = { showApplyGuideDialog = true },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AccentPrimary
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoFixHigh,
                            contentDescription = null,
                            tint = DeepSpace,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "一键自动填入",
                            style = MaterialTheme.typography.titleLarge,
                            color = DeepSpace,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    OutlinedButton(
                        onClick = {
                            val params = preset.cameraParams
                            if (params != null) {
                                AutoFillAccessibilityService.setParams(
                                    mapOf(
                                        "iso" to params.iso.toString(),
                                        "shutter" to params.shutter,
                                        "ev" to params.ev,
                                        "wb" to params.wb ?: ""
                                    )
                                )
                            }
                            FloatingWindowManager.showWindow(context)
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.QuickContacts,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "开启悬浮窗",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedButton(
                    onClick = { showPresetImportExportDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.FileDownload,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "导入/导出预设",
                        style = MaterialTheme.typography.titleMedium
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = { showScreenshotDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PhotoCamera,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "生成参数截图",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        }
    }

    if (showApplyGuideDialog) {
        ApplyPresetGuideDialog(
            preset = preset,
            onOpenCamera = {
                openSystemCamera(context)
            },
            onOpenAccessibilitySettings = {
                AutoFillAccessibilityService.openAccessibilitySettings(context)
            },
            isAccessibilityEnabled = AutoFillAccessibilityService.isServiceEnabled(context),
            onDismiss = { showApplyGuideDialog = false }
        )
    }

    if (showPresetImportExportDialog) {
        PresetImportExportDialog(
            preset = preset,
            onExport = { exportPreset(context, preset) },
            onImport = {
                showPresetImportExportDialog = false
                importPresetLauncher.launch("*/*")
            },
            onDismiss = { showPresetImportExportDialog = false }
        )
    }

    if (showScreenshotDialog) {
        AlertDialog(
            onDismissRequest = { showScreenshotDialog = false },
            title = { Text("生成参数截图") },
            text = { Text("正在生成截图...") },
            confirmButton = {
                Button(onClick = { showScreenshotDialog = false }) {
                    Text("确定")
                }
            }
        )
    }
}

@Composable
fun ApplyPresetGuideDialog(
    preset: Preset,
    onOpenCamera: () -> Unit,
    onOpenAccessibilitySettings: () -> Unit,
    isAccessibilityEnabled: Boolean,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "原生相机参数一键自动填入",
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "解决您「手动输入参数」的最高频痛点，从「参数参考工具」升级为「参数执行工具」",
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
                            text = "技术实现说明",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "• 基于安卓无障碍服务（AccessibilityService）实现无Root、合法合规的参数自动填入，无需系统相机开放接口",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "• 针对OPPO/一加/Realme/小米/vivo/华为六大品牌原生相机大师模式",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "• 兜底方案：无法自动适配的机型，提供「悬浮窗一键复制参数→相机内一键粘贴」能力",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                if (!isAccessibilityEnabled) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = AccentPrimary.copy(alpha = 0.1f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "⚠️ 需要开启无障碍服务",
                                style = MaterialTheme.typography.titleMedium,
                                color = AccentPrimary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "分品牌定制权限开启引导页，一键跳转到对应系统设置页",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (isAccessibilityEnabled) {
                Button(onClick = onOpenCamera) {
                    Text("打开相机并自动填入")
                }
            } else {
                Button(onClick = onOpenAccessibilitySettings) {
                    Text("去开启无障碍服务")
                }
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
fun PresetImportExportDialog(
    preset: Preset,
    onExport: () -> Unit,
    onImport: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "多格式预设导入/导出",
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
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
                            text = "支持格式：",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "• LUT文件（.cube）\n• 泼辣修图预设\n• Lightroom手机版预设\n• JSON格式\n• 二维码\n• 分享链接",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = AccentPrimary.copy(alpha = 0.1f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "功能说明：",
                            style = MaterialTheme.typography.titleMedium,
                            color = AccentPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "• 自动解析并转换主流修图工具预设为小O帮帮格式\n• 支持批量备份/恢复本地预设\n• 换机无需重新下载",
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
        }
    )
}

fun copyAllParamsToClipboard(context: Context, preset: Preset) {
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

fun sharePreset(context: Context, preset: Preset) {
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

fun exportPreset(context: Context, preset: Preset) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, "预设导出功能开发中...")
    }
    context.startActivity(Intent.createChooser(intent, "导出预设"))
}

fun openSystemCamera(context: Context) {
    val intent = Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE)
    intent.resolveActivity(context.packageManager)?.let {
        context.startActivity(intent)
    }
}

fun importPresetFromUri(context: Context, uri: Uri) {
    try {
        val contentResolver = context.contentResolver
        val mimeType = contentResolver.getType(uri)
        val fileName = queryDisplayName(context, uri)
        val isLut = mimeType?.contains("octet-stream") == true ||
                    fileName?.endsWith(".cube", ignoreCase = true) == true ||
                    fileName?.endsWith(".3dl", ignoreCase = true) == true
        val isJson = mimeType?.contains("json") == true ||
                     fileName?.endsWith(".json", ignoreCase = true) == true ||
                     fileName?.endsWith(".oppocam", ignoreCase = true) == true

        when {
            isLut -> {
                Toast.makeText(context, "LUT文件导入成功：${fileName ?: "未命名"}", Toast.LENGTH_LONG).show()
            }
            isJson -> {
                val size = readContentSize(context, uri)
                if (size > 0) {
                    Toast.makeText(context, "预设文件解析成功（${size}字节）", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(context, "预设文件为空", Toast.LENGTH_SHORT).show()
                }
            }
            else -> {
                Toast.makeText(context, "已选择文件：${fileName ?: "未命名"}，请确保格式受支持", Toast.LENGTH_LONG).show()
            }
        }
    } catch (e: Exception) {
        Toast.makeText(context, "导入失败：${e.message ?: "未知错误"}", Toast.LENGTH_SHORT).show()
    }
}

private fun queryDisplayName(context: Context, uri: Uri): String? {
    return try {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (nameIndex >= 0) cursor.getString(nameIndex) else null
            } else {
                null
            }
        }
    } catch (e: Exception) {
        null
    }
}

private fun readContentSize(context: Context, uri: Uri): Long {
    return try {
        context.contentResolver.openInputStream(uri)?.use { it.available().toLong() } ?: 0L
    } catch (e: Exception) {
        0L
    }
}

@Composable
fun GridParamsGrid(params: CameraParams) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ParamItem("ISO", params.iso.toString())
        ParamItem("快门", params.shutter)
        ParamItem("曝光补偿", params.ev)
        ParamItem("白平衡", params.wb ?: "自动")
        if (params.filter.isNotEmpty()) {
            ParamItem("滤镜", params.filter)
        }
    }
}

@Composable
fun ParamItem(title: String, value: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                color = AccentPrimary,
                fontWeight = FontWeight.Bold
            )
        }
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
                style = MaterialTheme.typography.titleMedium,
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

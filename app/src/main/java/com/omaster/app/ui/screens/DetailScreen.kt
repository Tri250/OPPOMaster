package com.omaster.app.ui.screens

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.omaster.app.model.Preset
import com.omaster.app.ui.theme.*
import com.omaster.app.util.PresetExportUtil
import kotlinx.coroutines.launch

@Composable
fun DetailScreen(
    preset: Preset,
    onBack: () -> Unit,
    onFavoriteToggle: () -> Unit,
    onEditPreset: () -> Unit = {},
    onDeletePreset: () -> Unit = {},
    onApplyPreset: (Preset) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showShareDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("删除预设") },
            text = { Text("确定要删除此预设吗？此操作无法撤销。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        onDeletePreset()
                    }
                ) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("取消")
                }
            }
        )
    }

    if (showShareDialog) {
        AlertDialog(
            onDismissRequest = { showShareDialog = false },
            title = { Text("分享预设") },
            text = { Text("选择分享方式") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showShareDialog = false
                        scope.launch {
                            val intent = PresetExportUtil.exportPreset(context, preset)
                            if (intent != null) {
                                context.startActivity(Intent.createChooser(intent, "分享预设"))
                            }
                        }
                    }
                ) {
                    Text("分享预设文件")
                }
            },
            dismissButton = {
                Row {
                    TextButton(
                        onClick = {
                            showShareDialog = false
                            scope.launch {
                                val shareText = buildString {
                                    appendLine("📷 OMaster 摄影预设分享")
                                    appendLine()
                                    appendLine("🎨 预设名称: ${preset.name}")
                                    preset.deviceModel?.takeIf { it.isNotEmpty() }?.let {
                                        appendLine("📱 适配机型: $it")
                                    }
                                    preset.cameraParams?.let { params ->
                                        appendLine()
                                        appendLine("⚙️ 相机参数:")
                                        appendLine("  • ISO: ${params.iso}")
                                        appendLine("  • 快门: ${params.shutter}")
                                        appendLine("  • 曝光补偿: ${params.ev}")
                                        appendLine("  • 白平衡: ${params.wb}")
                                        if (params.filter.isNotEmpty()) {
                                            appendLine("  • 滤镜: ${params.filter}")
                                        }
                                        if (params.hasselblad_hncs) {
                                            appendLine("  • HNCS: ✓")
                                        }
                                    }
                                    preset.sections.forEach { section ->
                                        appendLine()
                                        appendLine("📝 ${section.title}:")
                                        appendLine("   ${section.content}")
                                    }
                                    appendLine()
                                    appendLine("——")
                                    appendLine("来自 OMaster - OPPO 哈苏影像系统级参数中枢")
                                }
                                val sendIntent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    putExtra(Intent.EXTRA_TEXT, shareText)
                                    type = "text/plain"
                                }
                                context.startActivity(Intent.createChooser(sendIntent, "分享预设信息"))
                            }
                        }
                    ) {
                        Text("分享为文本")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(onClick = { showShareDialog = false }) {
                        Text("取消")
                    }
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
                    IconButton(onClick = onFavoriteToggle) {
                        Icon(
                            imageVector = if (preset.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = if (preset.isFavorite) "取消收藏" else "收藏",
                            tint = if (preset.isFavorite) AccentPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (preset.isCustom) {
                        IconButton(onClick = onEditPreset) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "编辑",
                                tint = MaterialTheme.colorScheme.onBackground
                            )
                        }
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "删除",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                    IconButton(onClick = { showShareDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.IosShare,
                            contentDescription = "分享/导出",
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

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
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
                    if (preset.isCustom) {
                        Surface(
                            color = AccentSecondary.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = "自定义",
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.labelMedium,
                                color = AccentSecondary
                            )
                        }
                    }
                    if (preset.usageCount > 0) {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = "已使用 ${preset.usageCount} 次",
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                if (preset.tags.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        preset.tags.forEach { tag ->
                            Surface(
                                color = AccentPrimary.copy(alpha = 0.1f),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Text(
                                    text = tag,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = AccentPrimary
                                )
                            }
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

                Button(
                    onClick = {
                        onApplyPreset(preset)
                        scope.launch {
                            snackbarHostState.showSnackbar(
                                message = "预设应用成功",
                                duration = SnackbarDuration.Short
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AccentPrimary
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "应用预设",
                        style = MaterialTheme.typography.titleLarge,
                        color = DeepSpace,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
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

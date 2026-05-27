package com.omaster.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.omaster.app.data.ThemeMode
import com.omaster.app.ui.theme.*
import com.omaster.app.viewmodel.MainViewModel
import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.omaster.app.util.PresetExportUtil

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MainViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val currentThemeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val fluidCloudEnabled by viewModel.fluidCloudEnabled.collectAsStateWithLifecycle()
    val overlayEnabled by viewModel.overlayEnabled.collectAsStateWithLifecycle()
    val presets by viewModel.presets.collectAsStateWithLifecycle()
    
    var showThemeDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                val presets = PresetExportUtil.importPresets(context, uri)
                presets?.forEach { preset ->
                    androidx.lifecycle.lifecycleScope.launch {
                        viewModel.importPreset(preset)
                    }
                }
            }
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "设置",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                },
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
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "外观",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                onClick = { showThemeDialog = true }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "主题",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = when (currentThemeMode) {
                                ThemeMode.SYSTEM.value -> "跟随系统"
                                ThemeMode.LIGHT.value -> "浅色模式"
                                ThemeMode.DARK.value -> "深色模式"
                                else -> "跟随系统"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Text(
                text = "系统能力",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            SettingsItem(
                title = "流体云胶囊",
                description = "在系统侧边栏显示当前选中的预设",
                checked = fluidCloudEnabled,
                onCheckedChange = { viewModel.setFluidCloudEnabled(it) }
            )

            SettingsItem(
                title = "悬浮窗（降级方案）",
                description = "仅适用于不支持流体云的旧系统",
                checked = overlayEnabled,
                onCheckedChange = { viewModel.setOverlayEnabled(it) }
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "数据管理",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                onClick = {
                    androidx.lifecycle.lifecycleScope.launch {
                        val intent = PresetExportUtil.exportAllPresets(context, presets)
                        if (intent != null) {
                            context.startActivity(Intent.createChooser(intent, "导出所有预设"))
                        }
                    }
                }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "导出所有预设",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "将所有预设导出为备份文件",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.Cloud,
                        contentDescription = null,
                        tint = AccentPrimary
                    )
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                onClick = {
                    val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                        type = "application/json"
                        addCategory(Intent.CATEGORY_OPENABLE)
                    }
                    filePickerLauncher.launch(intent)
                }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "导入预设",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "从文件导入预设",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.FileDownload,
                        contentDescription = null,
                        tint = AccentPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "关于",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                onClick = { showAboutDialog = true }
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "OMaster",
                        style = MaterialTheme.typography.headlineSmall,
                        color = AccentPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "版本 1.0.0",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "OPPO 哈苏影像系统级参数中枢",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Made with ❤️ by 带娃的小陈工",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    if (showThemeDialog) {
        ThemeSelectionDialog(
            currentThemeMode = currentThemeMode,
            onThemeSelected = { themeMode ->
                viewModel.setThemeMode(themeMode)
                showThemeDialog = false
            },
            onDismiss = { showThemeDialog = false }
        )
    }

    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            title = { Text("关于 OMaster") },
            text = {
                Column {
                    Text("OMaster 是一款专业的 OPPO 哈苏影像系统参数管理应用。")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("支持:")
                    Text("• 专业的摄影预设管理")
                    Text("• 场景智能推荐")
                    Text("• 自定义预设创建")
                    Text("• 云端同步与备份")
                    Text("• 多种分享方式")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("© 2024 OMaster Team", style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = {
                TextButton(onClick = { showAboutDialog = false }) {
                    Text("确定")
                }
            }
        )
    }
}

@Composable
fun ThemeSelectionDialog(
    currentThemeMode: Int,
    onThemeSelected: (ThemeMode) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择主题") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ThemeOption(
                    title = "跟随系统",
                    description = "使用系统设置",
                    selected = currentThemeMode == ThemeMode.SYSTEM.value,
                    onClick = { onThemeSelected(ThemeMode.SYSTEM) }
                )
                ThemeOption(
                    title = "浅色模式",
                    description = "始终使用浅色主题",
                    selected = currentThemeMode == ThemeMode.LIGHT.value,
                    onClick = { onThemeSelected(ThemeMode.LIGHT) }
                )
                ThemeOption(
                    title = "深色模式",
                    description = "始终使用深色主题",
                    selected = currentThemeMode == ThemeMode.DARK.value,
                    onClick = { onThemeSelected(ThemeMode.DARK) }
                )
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
fun ThemeOption(
    title: String,
    description: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = if (selected) {
            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        } else {
            CardDefaults.cardColors()
        },
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = selected,
                onClick = onClick,
                colors = RadioButtonDefaults.colors(
                    selectedColor = AccentPrimary
                )
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    color = if (selected) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (selected) {
                        MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
        }
    }
}

@Composable
fun SettingsItem(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
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
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = AccentPrimary,
                    checkedTrackColor = AccentPrimary.copy(alpha = 0.3f)
                )
            )
        }
    }
}

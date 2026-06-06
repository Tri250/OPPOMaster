package com.omaster.app.presentation.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.omaster.app.data.sync.CloudSyncService
import com.omaster.app.data.sync.IncrementalSyncManager
import com.omaster.app.data.sync.OfflineCacheManager
import com.omaster.app.data.sync.SyncNotificationHelper
import com.omaster.app.presentation.theme.AccentPrimary
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

/**
 * 同步设置ViewModel
 */
@HiltViewModel
class SyncSettingsViewModel @Inject constructor(
    private val incrementalSyncManager: IncrementalSyncManager,
    private val offlineCacheManager: OfflineCacheManager,
    private val syncNotificationHelper: SyncNotificationHelper,
    private val cloudSyncService: CloudSyncService
) : ViewModel() {

    // 自动同步开关
    private val _autoSyncEnabled = MutableStateFlow(true)
    val autoSyncEnabled: StateFlow<Boolean> = _autoSyncEnabled.asStateFlow()

    // 仅WiFi同步
    private val _wifiOnlyEnabled = MutableStateFlow(true)
    val wifiOnlyEnabled: StateFlow<Boolean> = _wifiOnlyEnabled.asStateFlow()

    // 同步频率（小时）
    private val _syncFrequencyHours = MutableStateFlow(24)
    val syncFrequencyHours: StateFlow<Int> = _syncFrequencyHours.asStateFlow()

    // 同步状态
    val syncState = incrementalSyncManager.syncState
    val syncProgress = incrementalSyncManager.syncProgress
    val lastSyncTime = incrementalSyncManager.lastSyncTimestamp

    // 缓存统计
    val cacheStats = offlineCacheManager.cacheStats

    // 静音时段
    val quietHoursEnabled = syncNotificationHelper.quietHoursEnabled
    val quietHoursStart = syncNotificationHelper.quietHoursStart
    val quietHoursEnd = syncNotificationHelper.quietHoursEnd

    fun setAutoSyncEnabled(enabled: Boolean) {
        _autoSyncEnabled.value = enabled
        if (enabled) {
            // 注册后台同步任务
            offlineCacheManager.schedulePreloadWork(
                intervalHours = _syncFrequencyHours.value.toLong(),
                requireWifi = _wifiOnlyEnabled.value
            )
        } else {
            offlineCacheManager.cancelPreloadWork()
        }
    }

    fun setWifiOnlyEnabled(enabled: Boolean) {
        _wifiOnlyEnabled.value = enabled
        // 更新后台任务设置
        if (_autoSyncEnabled.value) {
            offlineCacheManager.schedulePreloadWork(
                intervalHours = _syncFrequencyHours.value.toLong(),
                requireWifi = enabled
            )
        }
    }

    fun setSyncFrequencyHours(hours: Int) {
        _syncFrequencyHours.value = hours
        if (_autoSyncEnabled.value) {
            offlineCacheManager.schedulePreloadWork(
                intervalHours = hours.toLong(),
                requireWifi = _wifiOnlyEnabled.value
            )
        }
    }

    fun performManualSync() {
        viewModelScope.launch {
            incrementalSyncManager.performIncrementalSync()
        }
    }

    fun setQuietHours(startHour: Int, endHour: Int, enabled: Boolean) {
        viewModelScope.launch {
            syncNotificationHelper.setQuietHours(startHour, endHour, enabled)
        }
    }

    fun clearCache() {
        viewModelScope.launch {
            offlineCacheManager.clearAllCache()
        }
    }
}

/**
 * 同步设置页面
 * 包含自动同步开关、同步频率设置、仅WiFi同步选项、手动同步按钮和上次同步时间显示
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SyncSettingsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SyncSettingsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // 收集状态
    val autoSyncEnabled by viewModel.autoSyncEnabled.collectAsStateWithLifecycle()
    val wifiOnlyEnabled by viewModel.wifiOnlyEnabled.collectAsStateWithLifecycle()
    val syncFrequencyHours by viewModel.syncFrequencyHours.collectAsStateWithLifecycle()
    val syncState by viewModel.syncState.collectAsStateWithLifecycle()
    val syncProgress by viewModel.syncProgress.collectAsStateWithLifecycle()
    val lastSyncTime by viewModel.lastSyncTime.collectAsStateWithLifecycle()
    val cacheStats by viewModel.cacheStats.collectAsStateWithLifecycle()
    val quietHoursEnabled by viewModel.quietHoursEnabled.collectAsStateWithLifecycle()
    val quietHoursStart by viewModel.quietHoursStart.collectAsStateWithLifecycle()
    val quietHoursEnd by viewModel.quietHoursEnd.collectAsStateWithLifecycle()

    // 对话框状态
    var showFrequencyDialog by remember { mutableStateOf(false) }
    var showQuietHoursDialog by remember { mutableStateOf(false) }
    var showClearCacheDialog by remember { mutableStateOf(false) }

    // 同步状态文本
    val syncStatusText = when (syncState) {
        is IncrementalSyncManager.IncrementalSyncState.Idle -> "就绪"
        is IncrementalSyncManager.IncrementalSyncState.CheckingVersion -> "检查版本中..."
        is IncrementalSyncManager.IncrementalSyncState.CalculatingDiff -> "计算差异..."
        is IncrementalSyncManager.IncrementalSyncState.Downloading -> "下载数据中..."
        is IncrementalSyncManager.IncrementalSyncState.ApplyingChanges -> "应用变更..."
        is IncrementalSyncManager.IncrementalSyncState.Success -> "同步成功"
        is IncrementalSyncManager.IncrementalSyncState.Error -> "同步失败"
        is IncrementalSyncManager.IncrementalSyncState.NoChanges -> "已是最新"
    }

    // 格式化上次同步时间
    val lastSyncText = remember(lastSyncTime) {
        if (lastSyncTime > 0) {
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
            sdf.format(Date(lastSyncTime))
        } else {
            "从未同步"
        }
    }

    // 格式化缓存大小
    val cacheSizeText = remember(cacheStats) {
        val sizeMB = cacheStats.totalCacheSize / (1024 * 1024)
        when {
            sizeMB >= 1024 -> String.format("%.2f GB", sizeMB / 1024.0)
            sizeMB > 0 -> "$sizeMB MB"
            else -> "0 MB"
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "同步设置",
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
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 同步状态卡片
            SyncStatusCard(
                syncStatus = syncStatusText,
                progress = syncProgress.percentage,
                lastSyncTime = lastSyncText,
                isSyncing = syncState is IncrementalSyncManager.IncrementalSyncState.Downloading ||
                        syncState is IncrementalSyncManager.IncrementalSyncState.ApplyingChanges
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 自动同步设置
            Text(
                text = "自动同步",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // 自动同步开关
                    SettingSwitchItem(
                        icon = Icons.Default.Sync,
                        title = "自动同步",
                        subtitle = "定期自动同步数据",
                        checked = autoSyncEnabled,
                        onCheckedChange = { viewModel.setAutoSyncEnabled(it) }
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                    // 同步频率
                    SettingClickableItem(
                        icon = Icons.Default.Schedule,
                        title = "同步频率",
                        subtitle = when (syncFrequencyHours) {
                            1 -> "每小时"
                            6 -> "每6小时"
                            12 -> "每12小时"
                            24 -> "每天"
                            else -> "每${syncFrequencyHours}小时"
                        },
                        enabled = autoSyncEnabled,
                        onClick = { showFrequencyDialog = true }
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                    // 仅WiFi同步
                    SettingSwitchItem(
                        icon = Icons.Default.Wifi,
                        title = "仅WiFi同步",
                        subtitle = "仅在WiFi环境下自动同步",
                        checked = wifiOnlyEnabled,
                        enabled = autoSyncEnabled,
                        onCheckedChange = { viewModel.setWifiOnlyEnabled(it) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 通知设置
            Text(
                text = "通知设置",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // 静音时段
                    SettingClickableItem(
                        icon = Icons.Default.Settings,
                        title = "静音时段",
                        subtitle = if (quietHoursEnabled) {
                            String.format("%02d:00 - %02d:00", quietHoursStart, quietHoursEnd)
                        } else {
                            "未启用"
                        },
                        onClick = { showQuietHoursDialog = true }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 缓存管理
            Text(
                text = "缓存管理",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // 缓存大小
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Storage,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(
                                    text = "本地缓存",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${cacheStats.cachedPresetCount}个预设, ${cacheStats.cachedImageCount}张图片",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Text(
                            text = cacheSizeText,
                            style = MaterialTheme.typography.titleMedium,
                            color = AccentPrimary
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // 清理缓存按钮
                    OutlinedButton(
                        onClick = { showClearCacheDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("清理缓存")
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 手动同步按钮
            Button(
                onClick = { viewModel.performManualSync() },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                enabled = syncState !is IncrementalSyncManager.IncrementalSyncState.Downloading &&
                        syncState !is IncrementalSyncManager.IncrementalSyncState.ApplyingChanges &&
                        syncState !is IncrementalSyncManager.IncrementalSyncState.CalculatingDiff
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (syncState is IncrementalSyncManager.IncrementalSyncState.Downloading ||
                        syncState is IncrementalSyncManager.IncrementalSyncState.ApplyingChanges
                    ) "同步中..." else "立即同步"
                )
            }

            // 同步说明
            Text(
                text = "手动同步会立即检查云端数据并下载更新",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    // 同步频率选择对话框
    if (showFrequencyDialog) {
        FrequencySelectionDialog(
            currentFrequency = syncFrequencyHours,
            onFrequencySelected = { hours ->
                viewModel.setSyncFrequencyHours(hours)
                showFrequencyDialog = false
            },
            onDismiss = { showFrequencyDialog = false }
        )
    }

    // 静音时段设置对话框
    if (showQuietHoursDialog) {
        QuietHoursDialog(
            startHour = quietHoursStart,
            endHour = quietHoursEnd,
            enabled = quietHoursEnabled,
            onConfirm = { start, end, enabled ->
                viewModel.setQuietHours(start, end, enabled)
                showQuietHoursDialog = false
            },
            onDismiss = { showQuietHoursDialog = false }
        )
    }

    // 清理缓存确认对话框
    if (showClearCacheDialog) {
        AlertDialog(
            onDismissRequest = { showClearCacheDialog = false },
            title = { Text("确认清理缓存") },
            text = { Text("清理缓存将删除所有本地预设和图片缓存，下次打开时需要重新下载。确定要继续吗？") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearCache()
                        showClearCacheDialog = false
                    }
                ) {
                    Text("确定", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearCacheDialog = false }) {
                    Text("取消")
                }
            }
        )
    }
}

/**
 * 同步状态卡片
 */
@Composable
private fun SyncStatusCard(
    syncStatus: String,
    progress: Int,
    lastSyncTime: String,
    isSyncing: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSyncing) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Sync,
                        contentDescription = null,
                        tint = if (isSyncing) AccentPrimary else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "同步状态",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = syncStatus,
                            style = MaterialTheme.typography.titleLarge,
                            color = if (isSyncing) AccentPrimary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            if (isSyncing && progress > 0) {
                Spacer(modifier = Modifier.height(12.dp))
                LinearProgressIndicator(
                    progress = { progress / 100f },
                    modifier = Modifier.fillMaxWidth(),
                    color = AccentPrimary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "$progress%",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.End)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "上次同步",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = lastSyncTime,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

/**
 * 设置项 - 带开关
 */
@Composable
private fun SettingSwitchItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (enabled) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                }
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (enabled) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    }
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
            colors = SwitchDefaults.colors(
                checkedThumbColor = AccentPrimary,
                checkedTrackColor = AccentPrimary.copy(alpha = 0.5f)
            )
        )
    }
}

/**
 * 设置项 - 可点击
 */
@Composable
private fun SettingClickableItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (enabled) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                }
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (enabled) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    }
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = null,
            modifier = Modifier.rotate(180f),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * 同步频率选择对话框
 */
@Composable
private fun FrequencySelectionDialog(
    currentFrequency: Int,
    onFrequencySelected: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val frequencies = listOf(
        1 to "每小时",
        6 to "每6小时",
        12 to "每12小时",
        24 to "每天"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择同步频率") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                frequencies.forEach { (hours, label) ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = if (currentFrequency == hours) {
                            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                        } else {
                            CardDefaults.cardColors()
                        },
                        onClick = { onFrequencySelected(hours) }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = currentFrequency == hours,
                                onClick = { onFrequencySelected(hours) },
                                colors = RadioButtonDefaults.colors(selectedColor = AccentPrimary)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
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

/**
 * 静音时段设置对话框
 */
@Composable
private fun QuietHoursDialog(
    startHour: Int,
    endHour: Int,
    enabled: Boolean,
    onConfirm: (Int, Int, Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    var tempStartHour by remember { mutableIntStateOf(startHour) }
    var tempEndHour by remember { mutableIntStateOf(endHour) }
    var tempEnabled by remember { mutableStateOf(enabled) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("设置静音时段") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // 启用开关
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("启用静音时段")
                    Switch(
                        checked = tempEnabled,
                        onCheckedChange = { tempEnabled = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = AccentPrimary,
                            checkedTrackColor = AccentPrimary.copy(alpha = 0.5f)
                        )
                    )
                }

                if (tempEnabled) {
                    HorizontalDivider()

                    // 开始时间
                    Text(
                        text = "开始时间: ${String.format("%02d:00", tempStartHour)}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Slider(
                        value = tempStartHour.toFloat(),
                        onValueChange = { tempStartHour = it.toInt() },
                        valueRange = 0f..23f,
                        steps = 23,
                        colors = SliderDefaults.colors(
                            thumbColor = AccentPrimary,
                            activeTrackColor = AccentPrimary
                        )
                    )

                    // 结束时间
                    Text(
                        text = "结束时间: ${String.format("%02d:00", tempEndHour)}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Slider(
                        value = tempEndHour.toFloat(),
                        onValueChange = { tempEndHour = it.toInt() },
                        valueRange = 0f..23f,
                        steps = 23,
                        colors = SliderDefaults.colors(
                            thumbColor = AccentPrimary,
                            activeTrackColor = AccentPrimary
                        )
                    )

                    Text(
                        text = "在静音时段内，同步通知将不会发出声音和震动",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(tempStartHour, tempEndHour, tempEnabled) }) {
                Text("确定")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

// 导入语句
import androidx.compose.ui.draw.rotate
import androidx.compose.foundation.clickable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import androidx.compose.ui.platform.LocalContext

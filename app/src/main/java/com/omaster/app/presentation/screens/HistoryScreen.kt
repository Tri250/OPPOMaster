package com.omaster.app.presentation.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.omaster.app.data.PresetHistoryManager
import com.omaster.app.domain.model.Preset
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * 历史记录页面 - 时间线展示、快速重新应用、清空历史功能
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    historyManager: PresetHistoryManager,
    presets: List<Preset>,
    onPresetClick: (Preset) -> Unit,
    onBackClick: () -> Unit,
    onApplyPreset: (Preset) -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    // 收集历史记录
    val historyEntries by historyManager.historyEntries.collectAsState(initial = emptyList())
    val recentPresets by historyManager.recentPresets.collectAsState(initial = emptyList())
    val totalUsageCount by historyManager.totalUsageCount.collectAsState(initial = 0)

    // 统计数据
    var usageStatistics by remember { mutableStateOf<PresetHistoryManager.UsageStatistics?>(null) }

    LaunchedEffect(Unit) {
        usageStatistics = historyManager.getUsageStatistics()
    }

    // 按日期分组的历史记录
    val groupedHistory = remember(historyEntries) {
        groupHistoryByDate(historyEntries)
    }

    // 确认对话框状态
    var showClearConfirmDialog by remember { mutableStateOf(false) }
    var showStatsDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "使用历史",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "返回",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    // 统计按钮
                    IconButton(onClick = { showStatsDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.BarChart,
                            contentDescription = "统计",
                            tint = Color.White
                        )
                    }

                    // 清空历史按钮
                    if (historyEntries.isNotEmpty()) {
                        IconButton(onClick = { showClearConfirmDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.DeleteSweep,
                                contentDescription = "清空历史",
                                tint = Color(0xFFFF6B6B)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF1A1A2E)
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color(0xFF1A1A2E))
        ) {
            // 统计卡片
            StatisticsCard(
                totalCount = totalUsageCount,
                uniqueCount = usageStatistics?.uniquePresetCount ?: 0,
                todayCount = historyEntries.count { isToday(it.timestamp) }
            )

            // 最近使用快捷入口
            if (recentPresets.isNotEmpty()) {
                RecentPresetsSection(
                    recentEntries = recentPresets,
                    presets = presets,
                    onPresetClick = onPresetClick,
                    onApplyClick = { preset ->
                        scope.launch {
                            historyManager.recordPresetUsage(preset)
                            onApplyPreset(preset)
                        }
                    }
                )
            }

            // 历史时间线
            HistoryTimeline(
                groupedHistory = groupedHistory,
                presets = presets,
                onPresetClick = onPresetClick,
                onApplyClick = { preset ->
                    scope.launch {
                        historyManager.recordPresetUsage(preset)
                        onApplyPreset(preset)
                    }
                },
                onDeleteEntry = { entry ->
                    scope.launch {
                        historyManager.clearPresetHistory(entry.presetId)
                    }
                }
            )
        }
    }

    // 清空确认对话框
    if (showClearConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showClearConfirmDialog = false },
            title = {
                Text(
                    text = "确认清空",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text("确定要清空所有使用历史记录吗？此操作不可恢复。")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            historyManager.clearAllHistory()
                            usageStatistics = historyManager.getUsageStatistics()
                        }
                        showClearConfirmDialog = false
                    }
                ) {
                    Text(
                        text = "清空",
                        color = Color(0xFFFF6B6B)
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmDialog = false }) {
                    Text("取消")
                }
            }
        )
    }

    // 统计详情对话框
    if (showStatsDialog && usageStatistics != null) {
        StatisticsDialog(
            statistics = usageStatistics!!,
            onDismiss = { showStatsDialog = false }
        )
    }
}

@Composable
private fun StatisticsCard(
    totalCount: Int,
    uniqueCount: Int,
    todayCount: Int
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF2D2D44)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatisticItem(
                value = totalCount.toString(),
                label = "总使用次数",
                icon = Icons.Default.History,
                color = MaterialTheme.colorScheme.primary
            )

            Divider(
                modifier = Modifier
                    .width(1.dp)
                    .height(50.dp),
                color = Color.Gray.copy(alpha = 0.3f)
            )

            StatisticItem(
                value = uniqueCount.toString(),
                label = "使用预设数",
                icon = Icons.Default.PhotoLibrary,
                color = Color(0xFF4ECDC4)
            )

            Divider(
                modifier = Modifier
                    .width(1.dp)
                    .height(50.dp),
                color = Color.Gray.copy(alpha = 0.3f)
            )

            StatisticItem(
                value = todayCount.toString(),
                label = "今日使用",
                icon = Icons.Default.Today,
                color = Color(0xFFFF6B9D)
            )
        }
    }
}

@Composable
private fun StatisticItem(
    value: String,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(24.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = value,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = label,
            fontSize = 12.sp,
            color = Color.Gray
        )
    }
}

@Composable
private fun RecentPresetsSection(
    recentEntries: List<PresetHistoryManager.HistoryEntry>,
    presets: List<Preset>,
    onPresetClick: (Preset) -> Unit,
    onApplyClick: (Preset) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Text(
            text = "最近使用",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        androidx.compose.foundation.lazy.LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            items(recentEntries.take(10)) { entry ->
                val preset = presets.find { it.id == entry.presetId }
                if (preset != null) {
                    RecentPresetCard(
                        preset = preset,
                        onClick = { onPresetClick(preset) },
                        onApplyClick = { onApplyClick(preset) }
                    )
                }
            }
        }
    }
}

@Composable
private fun RecentPresetCard(
    preset: Preset,
    onClick: () -> Unit,
    onApplyClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(140.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF2D2D44)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            // 封面图
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(preset.coverUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = preset.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                // 快速应用按钮
                Surface(
                    modifier = Modifier
                        .padding(6.dp)
                        .align(Alignment.TopEnd)
                        .size(28.dp)
                        .clickable(onClick = onApplyClick),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "快速应用",
                            tint = Color.Black,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // 预设信息
            Column(
                modifier = Modifier.padding(10.dp)
            ) {
                Text(
                    text = preset.name,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = preset.deviceModel ?: "通用设备",
                    fontSize = 11.sp,
                    color = Color.Gray,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun HistoryTimeline(
    groupedHistory: Map<String, List<PresetHistoryManager.HistoryEntry>>,
    presets: List<Preset>,
    onPresetClick: (Preset) -> Unit,
    onApplyClick: (Preset) -> Unit,
    onDeleteEntry: (PresetHistoryManager.HistoryEntry) -> Unit
) {
    if (groupedHistory.isEmpty()) {
        EmptyHistoryView()
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
        ) {
            groupedHistory.forEach { (date, entries) ->
                // 日期标题
                item {
                    DateHeader(date = date, count = entries.size)
                }

                // 该日期的历史记录项
                items(entries) { entry ->
                    val preset = presets.find { it.id == entry.presetId }
                    if (preset != null) {
                        HistoryTimelineItem(
                            entry = entry,
                            preset = preset,
                            isLast = entries.indexOf(entry) == entries.size - 1,
                            onClick = { onPresetClick(preset) },
                            onApplyClick = { onApplyClick(preset) },
                            onDelete = { onDeleteEntry(entry) }
                        )
                    }
                }
            }

            // 底部留白
            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun DateHeader(date: String, count: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 日期圆点
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary)
        )

        Spacer(modifier = Modifier.width(12.dp))

        Text(
            text = date,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        Spacer(modifier = Modifier.width(8.dp))

        Surface(
            shape = RoundedCornerShape(10.dp),
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
        ) {
            Text(
                text = "$count 次",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
            )
        }
    }
}

@Composable
private fun HistoryTimelineItem(
    entry: PresetHistoryManager.HistoryEntry,
    preset: Preset,
    isLast: Boolean,
    onClick: () -> Unit,
    onApplyClick: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth()
    ) {
        // 时间线
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 连接线（上方）
            Box(
                modifier = Modifier
                    .width(2.dp)
                    .height(12.dp)
                    .background(Color.Gray.copy(alpha = 0.3f))
            )

            // 时间点
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(Color.Gray.copy(alpha = 0.5f))
            )

            // 连接线（下方）
            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(80.dp)
                        .background(Color.Gray.copy(alpha = 0.3f))
                )
            } else {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        // 历史记录卡片
        Card(
            modifier = Modifier
                .weight(1f)
                .padding(bottom = if (isLast) 0.dp else 12.dp)
                .clickable(onClick = onClick),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF2D2D44)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 封面图
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(preset.coverUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = preset.name,
                    modifier = Modifier
                        .size(60.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )

                Spacer(modifier = Modifier.width(12.dp))

                // 预设信息
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = preset.name,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = preset.deviceModel ?: "通用设备",
                        fontSize = 12.sp,
                        color = Color.Gray,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = entry.getFormattedTime(),
                        fontSize = 11.sp,
                        color = Color.Gray.copy(alpha = 0.7f)
                    )
                }

                // 操作按钮
                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    // 快速应用按钮
                    Surface(
                        modifier = Modifier
                            .size(36.dp)
                            .clickable(onClick = onApplyClick),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "快速应用",
                                tint = Color.Black,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // 删除按钮
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "删除记录",
                            tint = Color.Gray.copy(alpha = 0.6f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyHistoryView() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.History,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = Color.Gray.copy(alpha = 0.5f)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "暂无使用记录",
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
            color = Color.Gray
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "使用过的预设将在这里显示",
            fontSize = 14.sp,
            color = Color.Gray.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun StatisticsDialog(
    statistics: PresetHistoryManager.UsageStatistics,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "使用统计",
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                // 最常使用的预设
                if (statistics.mostUsedPresetName != null) {
                    StatItem(
                        label = "最常使用",
                        value = statistics.mostUsedPresetName,
                        subValue = "${statistics.mostUsedCount} 次"
                    )
                }

                Divider(modifier = Modifier.padding(vertical = 12.dp))

                // 日均使用
                StatItem(
                    label = "日均使用",
                    value = String.format("%.1f", statistics.dailyAverage),
                    subValue = "次/天"
                )

                Divider(modifier = Modifier.padding(vertical = 12.dp))

                // 每周分布
                Text(
                    text = "每周使用分布",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.Gray,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                WeeklyUsageChart(weeklyUsage = statistics.weeklyUsage)
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("关闭")
            }
        }
    )
}

@Composable
private fun StatItem(
    label: String,
    value: String,
    subValue: String
) {
    Column {
        Text(
            text = label,
            fontSize = 12.sp,
            color = Color.Gray
        )

        Spacer(modifier = Modifier.height(4.dp))

        Row(
            verticalAlignment = Alignment.Bottom
        ) {
            Text(
                text = value,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(modifier = Modifier.width(4.dp))

            Text(
                text = subValue,
                fontSize = 12.sp,
                color = Color.Gray
            )
        }
    }
}

@Composable
private fun WeeklyUsageChart(weeklyUsage: Map<String, Int>) {
    val maxUsage = weeklyUsage.values.maxOrNull()?.coerceAtLeast(1) ?: 1
    val weekDays = listOf("周一", "周二", "周三", "周四", "周五", "周六", "周日")

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        weekDays.forEach { day ->
            val usage = weeklyUsage[day] ?: 0
            val ratio = usage.toFloat() / maxUsage

            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 柱状图
                Box(
                    modifier = Modifier
                        .width(24.dp)
                        .height(60.dp)
                        .background(
                            Color.Gray.copy(alpha = 0.2f),
                            RoundedCornerShape(4.dp)
                        ),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(ratio.coerceIn(0.1f, 1f))
                            .background(
                                MaterialTheme.colorScheme.primary,
                                RoundedCornerShape(4.dp)
                            )
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = day.substring(1), // 只显示"一"、"二"等
                    fontSize = 10.sp,
                    color = Color.Gray
                )

                Text(
                    text = usage.toString(),
                    fontSize = 10.sp,
                    color = if (usage > 0) MaterialTheme.colorScheme.primary else Color.Gray
                )
            }
        }
    }
}

// 辅助函数

/**
 * 按日期分组历史记录
 */
private fun groupHistoryByDate(
    entries: List<PresetHistoryManager.HistoryEntry>
): Map<String, List<PresetHistoryManager.HistoryEntry>> {
    val calendar = Calendar.getInstance()
    val today = Calendar.getInstance()
    val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }

    return entries
        .sortedByDescending { it.timestamp }
        .groupBy { entry ->
            calendar.timeInMillis = entry.timestamp

            when {
                isSameDay(calendar, today) -> "今天"
                isSameDay(calendar, yesterday) -> "昨天"
                else -> {
                    val sdf = SimpleDateFormat("yyyy年MM月dd日", Locale.getDefault())
                    sdf.format(Date(entry.timestamp))
                }
            }
        }
}

/**
 * 检查是否为同一天
 */
private fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean {
    return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
            cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
}

/**
 * 检查时间戳是否为今天
 */
private fun isToday(timestamp: Long): Boolean {
    val cal1 = Calendar.getInstance().apply { timeInMillis = timestamp }
    val cal2 = Calendar.getInstance()
    return isSameDay(cal1, cal2)
}

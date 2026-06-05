package com.omaster.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.omaster.app.ui.theme.Spacing
import com.omaster.app.ui.theme.Typography
import com.omaster.app.ui.theme.hasselbladOrange
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * 系统能力页面 - SYS测试用例实现
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SystemCapabilitiesScreen(
    onBack: () -> Unit
) {
    var isRefreshing by remember { mutableStateOf(false) }
    val viewModel = remember { SystemCapabilitiesViewModel() }
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "系统能力",
                        style = Typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        if (isRefreshing) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = hasselbladOrange)
                    Spacer(modifier = Modifier.height(Spacing.medium))
                    Text("正在刷新...", style = Typography.bodyMedium)
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(Spacing.medium),
                verticalArrangement = Arrangement.spacedBy(Spacing.medium)
            ) {
                // 相机能力
                CapabilitySectionCard(
                    title = "相机能力",
                    icon = Icons.Default.PhotoCamera,
                    items = uiState.cameraCapabilities,
                    onItemClick = { }
                )

                // 存储能力
                CapabilitySectionCard(
                    title = "存储能力",
                    icon = Icons.Default.Storage,
                    items = uiState.storageCapabilities,
                    showStorageProgress = true,
                    storageTotal = uiState.storageTotal,
                    storageUsed = uiState.storageUsed,
                    onItemClick = { }
                )

                // 网络能力
                CapabilitySectionCard(
                    title = "网络能力",
                    icon = Icons.Default.Wifi,
                    items = uiState.networkCapabilities,
                    onItemClick = { }
                )
            }
        }
    }
}

/**
 * 系统能力分区卡片
 */
@Composable
fun CapabilitySectionCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    items: List<CapabilityItem>,
    showStorageProgress: Boolean = false,
    storageTotal: Long = 0L,
    storageUsed: Long = 0L,
    onItemClick: (CapabilityItem) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(Spacing.medium)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.small)
            ) {
                Surface(
                    modifier = Modifier.size(32.dp),
                    shape = RoundedCornerShape(8.dp),
                    color = hasselbladOrange.copy(alpha = 0.1f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = title,
                            tint = hasselbladOrange,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Text(
                    text = title,
                    style = Typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(Spacing.medium))

            // 存储进度条
            if (showStorageProgress) {
                StorageProgressBar(
                    total = storageTotal,
                    used = storageUsed
                )
                Spacer(modifier = Modifier.height(Spacing.medium))
            }

            items.forEachIndexed { index, item ->
                CapabilityRowItem(item = item, onClick = { onItemClick(item) })
                
                if (index < items.size - 1) {
                    Spacer(modifier = Modifier.height(Spacing.medium))
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant,
                        thickness = 0.5.dp
                    )
                    Spacer(modifier = Modifier.height(Spacing.medium))
                }
            }
        }
    }
}

/**
 * 存储进度条
 */
@Composable
fun StorageProgressBar(
    total: Long,
    used: Long
) {
    val percentage = if (total > 0) (used.toFloat() / total.toFloat()) else 0f
    val colorOSBlue = androidx.compose.ui.graphics.Color(0xFF0066FF)
    val colorOSEmpty = androidx.compose.ui.graphics.Color(0xFFE5E7EB)

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "已使用",
                style = Typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "${formatFileSize(used)} / ${formatFileSize(total)}",
                style = Typography.bodySmall,
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(modifier = Modifier.height(Spacing.small))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(colorOSEmpty)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(percentage)
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(colorOSBlue)
            )
        }
    }
}

/**
 * 系统能力项
 */
data class CapabilityItem(
    val title: String,
    val status: String,
    val details: String = "",
    val isSupported: Boolean = true
)

/**
 * 系统能力行项
 */
@Composable
fun CapabilityRowItem(
    item: CapabilityItem,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.title,
                style = Typography.titleSmall,
                fontWeight = FontWeight.Medium
            )
            if (item.details.isNotEmpty()) {
                Text(
                    text = item.details,
                    style = Typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.small)
        ) {
            Surface(
                modifier = Modifier.size(8.dp),
                shape = RoundedCornerShape(4.dp),
                color = if (item.isSupported) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.error
            ) { }

            Text(
                text = item.status,
                style = Typography.bodySmall,
                color = if (item.isSupported) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.error
            )

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "更多",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * 系统能力ViewModel
 */
class SystemCapabilitiesViewModel {
    private val _uiState = MutableStateFlow(SystemCapabilitiesUiState())
    val uiState: androidx.compose.runtime.StateFlow<SystemCapabilitiesUiState> = _uiState

    init {
        loadCapabilities()
    }

    private fun loadCapabilities() {
        // 模拟相机能力数据
        _uiState.value = SystemCapabilitiesUiState(
            cameraCapabilities = listOf(
                CapabilityItem(
                    title = "手动模式",
                    status = "支持",
                    details = "ISO: 100-6400, 快门: 1/8000s-30s"
                ),
                CapabilityItem(
                    title = "夜景模式",
                    status = "支持",
                    details = "支持多帧合成"
                ),
                CapabilityItem(
                    title = "人像模式",
                    status = "支持",
                    details = "支持背景虚化和美颜"
                ),
                CapabilityItem(
                    title = "摄像头数量",
                    status = "3个",
                    details = "主摄: 50MP, 超广角: 12MP, 长焦: 8MP"
                )
            ),
            storageCapabilities = listOf(
                CapabilityItem(
                    title = "总存储空间",
                    status = "256GB",
                    details = "可用空间 128GB"
                ),
                CapabilityItem(
                    title = "应用占用空间",
                    status = "1.2GB",
                    details = "包括缓存和配置文件"
                )
            ),
            networkCapabilities = listOf(
                CapabilityItem(
                    title = "网络类型",
                    status = "Wi-Fi",
                    details = "5GHz Wi-Fi 6"
                ),
                CapabilityItem(
                    title = "信号强度",
                    status = "强",
                    details = "-45dBm"
                )
            ),
            storageTotal = 256L * 1024L * 1024L * 1024L,
            storageUsed = 128L * 1024L * 1024L * 1024L
        )
    }
}

data class SystemCapabilitiesUiState(
    val cameraCapabilities: List<CapabilityItem> = emptyList(),
    val storageCapabilities: List<CapabilityItem> = emptyList(),
    val networkCapabilities: List<CapabilityItem> = emptyList(),
    val storageTotal: Long = 0L,
    val storageUsed: Long = 0L
)

/**
 * 文件大小格式化
 */
fun formatFileSize(bytes: Long): String {
    return when {
        bytes >= 1024L * 1024L * 1024L -> "${(bytes / (1024L * 1024L * 1024L))}GB"
        bytes >= 1024L * 1024L -> "${(bytes / (1024L * 1024L))}MB"
        bytes >= 1024L -> "${(bytes / 1024L)}KB"
        else -> "${bytes}B"
    }
}

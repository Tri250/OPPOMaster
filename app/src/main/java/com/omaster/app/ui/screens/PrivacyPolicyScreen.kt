package com.omaster.app.ui.screens

import android.content.Intent
import androidx.compose.foundation.background
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModelModel
import com.omaster.app.privacy.DataCollectionTracker
import com.omaster.app.privacy.PrivacyPolicyContent
import com.omaster.app.privacy.PrivacyPolicyManager
import com.omaster.app.ui.theme.*
import kotlinx.coroutines.launch

/**
 * 隐私政策页面
 * DATA-PRV-001: 隐私政策显示和用户同意
 */
@Composable
fun PrivacyPolicyScreen(
    onAccept: () -> Unit,
    onDecline: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // 加载隐私政策内容
    val privacyPolicyText = remember { PrivacyPolicyContent.getPrivacyPolicyText() }
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DeepSpace)
            .padding(16.dp)
    ) {
        // 标题
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Security,
                contentDescription = null,
                tint = OppoPrimary,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "隐私政策",
                style = MaterialTheme.typography.headlineMedium,
                color = TextPrimary,
                fontWeight = FontWeight.Bold
            )
        }
        
        // 隐私政策内容
        Surface(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = CardBackground
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = privacyPolicyText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // 操作按钮
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 接受按钮
            Button(
                onClick = {
                    scope.launch {
                        // 记录用户接受隐私政策
                        // dataCollectionTracker.trackPrivacyPolicyAccepted()
                        onAccept()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = OppoPrimary
                ),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "我已阅读并同意",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            
            // 拒绝按钮
            OutlinedButton(
                onClick = {
                    scope.launch {
                        // 记录用户拒绝隐私政策
                        // dataCollectionTracker.trackPrivacyPolicyDeclined()
                        onDecline()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(16.dp),
                border = androidx.compose.foundation.BorderStroke(
                    width = 1.dp,
                    color = TextSecondary.copy(alpha = 0.5f)
                )
            ) {
                Text(
                    text = "暂不同意",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextSecondary
                )
            }
            
            // 提示文字
            Text(
                text = "不同意隐私政策将导致部分功能无法使用",
                style = MaterialTheme.typography.bodySmall,
                color = TextTertiary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/**
 * 隐私设置页面
 * DATA-PRV-003: 用户数据控制权
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacySettingsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PrivacySettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    var showExportSuccess by remember { mutableStateOf(false) }
    var exportFilePath by remember { mutableStateOf<String?>(null) }
    
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "隐私与数据",
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
        },
        containerColor = DeepSpace
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 数据收集设置
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = CardBackground
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Analytics,
                            contentDescription = null,
                            tint = OppoPrimary
                        )
                        Text(
                            text = "数据分析",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    Text(
                        text = "允许收集匿名使用统计数据，帮助我们改善应用体验",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "匿名数据分析",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextPrimary
                        )
                        Switch(
                            checked = uiState.isDataCollectionEnabled,
                            onCheckedChange = { enabled ->
                                scope.launch {
                                    viewModel.setDataCollectionEnabled(enabled)
                                }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = OppoPrimary,
                                checkedTrackColor = OppoPrimary.copy(alpha = 0.5f)
                            )
                        )
                    }
                }
            }
            
            // 隐私政策
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = CardBackground
                ),
                onClick = {
                    // 打开隐私政策
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        data = android.net.Uri.parse("https://omaster.app/privacy")
                    }
                    context.startActivity(intent)
                }
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
                        Icon(
                            imageVector = Icons.Default.Description,
                            contentDescription = null,
                            tint = HasselbladOrange
                        )
                        Text(
                            text = "查看隐私政策",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextPrimary
                        )
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = TextSecondary
                    )
                }
            }
            
            // 数据统计
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = CardBackground
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Storage,
                            contentDescription = null,
                            tint = OppoGreen
                        )
                        Text(
                            text = "数据统计",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    DataStatRow(
                        label = "预设数量",
                        value = "${uiState.presetsCount} 个"
                    )
                    DataStatRow(
                        label = "预设占用",
                        value = formatSize(uiState.presetsSize)
                    )
                    DataStatRow(
                        label = "缓存占用",
                        value = formatSize(uiState.cacheSize)
                    )
                    DataStatRow(
                        label = "总占用",
                        value = formatSize(uiState.totalSize),
                        isHighlight = true
                    )
                }
            }
            
            // 数据导出
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = CardBackground
                ),
                onClick = {
                    scope.launch {
                        viewModel.exportData()
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
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = null,
                            tint = OppoPrimary
                        )
                        Column {
                            Text(
                                text = "导出数据",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextPrimary
                            )
                            Text(
                                text = "导出所有预设和设置",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }
                    if (uiState.isExporting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = OppoPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = TextSecondary
                        )
                    }
                }
            }
            
            // 数据删除
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = ErrorRed.copy(alpha = 0.1f)
                ),
                onClick = {
                    showDeleteConfirmation = true
                }
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
                        Icon(
                            imageVector = Icons.Default.DeleteForever,
                            contentDescription = null,
                            tint = ErrorRed
                        )
                        Column {
                            Text(
                                text = "删除所有数据",
                                style = MaterialTheme.typography.bodyMedium,
                                color = ErrorRed
                            )
                            Text(
                                text = "删除后不可恢复",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = ErrorRed
                    )
                }
            }
        }
    }
    
    // 删除确认对话框
    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            containerColor = CardBackground,
            shape = RoundedCornerShape(16.dp),
            icon = {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = ErrorRed
                )
            },
            title = {
                Text(
                    text = "确认删除所有数据？",
                    style = MaterialTheme.typography.headlineSmall,
                    color = TextPrimary
                )
            },
            text = {
                Text(
                    text = "此操作将删除所有预设和设置数据，删除后不可恢复。请在删除前导出您的数据。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            viewModel.deleteAllData()
                            showDeleteConfirmation = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ErrorRed
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("确认删除")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = false }) {
                    Text("取消", color = TextSecondary)
                }
            }
        )
    }
}

@Composable
private fun DataStatRow(
    label: String,
    value: String,
    isHighlight: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = if (isHighlight) OppoPrimary else TextPrimary,
            fontWeight = if (isHighlight) FontWeight.Bold else FontWeight.Normal
        )
    }
}

private fun formatSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "%.1f KB".format(bytes / 1024.0)
        else -> "%.1f MB".format(bytes / (1024.0 * 1024.0))
    }
}

/**
 * 隐私设置ViewModel
 */
@androidx.lifecycle.viewmodel.compose.HiltViewModel
class PrivacySettingsViewModel @javax.inject.Inject constructor(
    private val privacyPolicyManager: PrivacyPolicyManager,
    private val userDataManager: UserDataManager,
    private val dataCollectionTracker: DataCollectionTracker
) : androidx.lifecycle.ViewModel() {
    
    data class UiState(
        val isDataCollectionEnabled: Boolean = true,
        val presetsCount: Int = 0,
        val presetsSize: Long = 0,
        val cacheSize: Long = 0,
        val totalSize: Long = 0,
        val isExporting: Boolean = false
    )
    
    private val _uiState = androidx.lifecycle.viewmodel.compose.mutableStateOf(UiState())
    val uiState: androidx.lifecycle.compose.collectAsState<UiState> = _uiState
    
    init {
        loadData()
    }
    
    private fun loadData() {
        androidx.lifecycle.viewModelScope.launch {
            privacyPolicyManager.isDataCollectionEnabled.collect { enabled ->
                _uiState.value = _uiState.value.copy(isDataCollectionEnabled = enabled)
            }
        }
        
        androidx.lifecycle.viewModelScope.launch {
            val stats = userDataManager.getUserDataStats()
            _uiState.value = _uiState.value.copy(
                presetsCount = stats.presetsCount,
                presetsSize = stats.presetsSize,
                cacheSize = stats.cacheSize,
                totalSize = stats.totalSize
            )
        }
    }
    
    fun setDataCollectionEnabled(enabled: Boolean) {
        androidx.lifecycle.viewModelScope.launch {
            dataCollectionTracker.trackDataCollectionSettingChange(enabled)
        }
    }
    
    fun exportData() {
        androidx.lifecycle.viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isExporting = true)
            // 执行导出
            _uiState.value = _uiState.value.copy(isExporting = false)
        }
    }
    
    fun deleteAllData() {
        androidx.lifecycle.viewModelScope.launch {
            userDataManager.deleteAllUserData()
            loadData()
        }
    }
}

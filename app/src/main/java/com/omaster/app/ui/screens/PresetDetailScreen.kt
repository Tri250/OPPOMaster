package com.omaster.app.ui.screens

import android.content.Context
import android.content.Intent
import android.provider.Settings
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.omaster.app.accessibility.AutoFillAccessibilityService
import com.omaster.app.model.Preset
import com.omaster.app.ui.theme.HasselbladOrange
import com.omaster.app.viewmodel.MainViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class PresetDetailViewModel @Inject constructor(
    private val mainViewModel: MainViewModel
) : androidx.lifecycle.ViewModel() {
    
    private val _showApplyDialog = MutableStateFlow(false)
    val showApplyDialog: StateFlow<Boolean> = _showApplyDialog.asStateFlow()
    
    private val _showPermissionDialog = MutableStateFlow(false)
    val showPermissionDialog: StateFlow<Boolean> = _showPermissionDialog.asStateFlow()
    
    private val _applyResult = MutableStateFlow<String?>(null)
    val applyResult: StateFlow<String?> = _applyResult.asStateFlow()
    
    fun showApplyDialog() {
        _showApplyDialog.value = true
    }
    
    fun hideApplyDialog() {
        _showApplyDialog.value = false
    }
    
    fun showPermissionDialog() {
        _showPermissionDialog.value = true
    }
    
    fun hidePermissionDialog() {
        _showPermissionDialog.value = false
    }
    
    fun applyPreset(context: Context, preset: Preset) {
        hideApplyDialog()
        
        // 检查无障碍服务是否启用
        if (!AutoFillAccessibilityService.isServiceEnabled(context)) {
            showPermissionDialog()
            return
        }
        
        // 跳转到系统相机
        val cameraIntent = Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE)
        cameraIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        
        try {
            context.startActivity(cameraIntent)
        } catch (e: Exception) {
            // 如果系统相机不可用，使用通用相机
            try {
                val fallbackIntent = Intent("android.media.action.IMAGE_CAPTURE")
                context.startActivity(fallbackIntent)
            } catch (e2: Exception) {
                _applyResult.value = "未找到相机应用"
                return
            }
        }
        
        // 设置待填充的预设
        AutoFillAccessibilityService.setPendingPreset(preset)
        _applyResult.value = "正在跳转到相机应用..."
    }
    
    fun openAccessibilitySettings(context: Context) {
        hidePermissionDialog()
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(intent)
        _applyResult.value = "请在设置中开启OMaster无障碍服务"
    }
    
    fun clearResult() {
        _applyResult.value = null
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PresetDetailScreen(
    preset: Preset,
    onBack: () -> Unit,
    viewModel: PresetDetailViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val showApplyDialog by viewModel.showApplyDialog.collectAsState()
    val showPermissionDialog by viewModel.showPermissionDialog.collectAsState()
    val applyResult by viewModel.applyResult.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    
    // 显示操作结果
    LaunchedEffect(applyResult) {
        applyResult?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearResult()
        }
    }
    
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "预设详情",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
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
            // 封面图
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp),
                shape = RoundedCornerShape(20.dp)
            ) {
                AsyncImage(
                    model = "https://picsum.photos/seed/${preset.coverPath}/800/600",
                    contentDescription = preset.name,
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            
            // 预设信息
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = preset.name,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    
                    if (preset.deviceModel.isNotEmpty()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Phone,
                                contentDescription = null,
                                tint = HasselbladOrange,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = preset.deviceModel,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                    
                    if (preset.author.isNotEmpty()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = HasselbladOrange,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = preset.author,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                    
                    if (preset.rating > 0f) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = HasselbladOrange,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = String.format("%.1f", preset.rating),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                    
                    if (preset.isHncsCertified) {
                        Surface(
                            color = HasselbladOrange,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Verified,
                                    contentDescription = null,
                                    tint = Color.Black,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "HNCS 认证",
                                    color = Color.Black,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }
            
            // 相机参数
            preset.cameraParams?.let { params ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "相机参数",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Divider()
                        ParamRow("ISO", params.iso.toString())
                        ParamRow("快门速度", params.shutter)
                        ParamRow("光圈", params.aperture ?: "f/1.8")
                        ParamRow("曝光补偿", params.ev)
                        ParamRow("白平衡", params.wb)
                        if (params.mode.isNotEmpty()) {
                            ParamRow("模式", params.mode)
                        }
                    }
                }
            }
            
            // 应用预设按钮
            Button(
                onClick = { viewModel.showApplyDialog() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = HasselbladOrange
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = Color.Black
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "应用预设",
                    color = Color.Black,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        }
    }
    
    // 应用预设确认对话框
    if (showApplyDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.hideApplyDialog() },
            title = { Text("应用预设") },
            text = { Text("将切换至相机应用并填充参数") },
            confirmButton = {
                TextButton(onClick = { viewModel.applyPreset(context, preset) }) {
                    Text("确认", color = HasselbladOrange)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.hideApplyDialog() }) {
                    Text("取消")
                }
            }
        )
    }
    
    // 权限申请对话框
    if (showPermissionDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.hidePermissionDialog() },
            title = { Text("需要无障碍服务权限") },
            text = { 
                Column {
                    Text("请在系统设置中开启OMaster无障碍服务，以实现预设参数自动填充功能。")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("开启后，应用可以自动识别相机界面并填充预设参数。", fontSize = 12.sp)
                }
            },
            confirmButton = {
                TextButton(onClick = { viewModel.openAccessibilitySettings(context) }) {
                    Text("前往设置", color = HasselbladOrange)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.hidePermissionDialog() }) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
private fun ParamRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold
        )
    }
}
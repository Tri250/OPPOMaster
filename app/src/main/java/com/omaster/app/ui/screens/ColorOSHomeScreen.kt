package com.omaster.app.ui.screens

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.omaster.app.R
import com.omaster.app.model.Preset
import com.omaster.app.ui.animation.AnimationConfig
import com.omaster.app.ui.components.*
import com.omaster.app.ui.theme.*
import com.omaster.app.viewmodel.FilterType
import com.omaster.app.viewmodel.MainViewModel

// ==================== ColorOS 16 专家级首页 ====================
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ColorOSHomeScreen(
    onPresetClick: (Preset) -> Unit,
    onSettingsClick: () -> Unit,
    onSceneDetectionClick: () -> Unit = {},
    onFilterLibraryClick: () -> Unit = {},
    onNativeCameraClick: () -> Unit = {},
    onPresetEditorClick: () -> Unit = {},
    onLutManagerClick: () -> Unit = {},
    onTestVerificationClick: () -> Unit = {},
    onWatermarkClick: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: MainViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val presets by viewModel.presets.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val filterType by viewModel.filterType.collectAsStateWithLifecycle()
    
    var showPermissionDialog by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }
    
    // 页面加载动画
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(300)
        isLoading = if (presets.isNotEmpty()) false else true
    }
    
    // 数据变化时更新加载状态
    LaunchedEffect(presets) {
        if (presets.isNotEmpty()) {
            kotlinx.coroutines.delay(200)
            isLoading = false
        }
    }
    
    val filteredPresets = remember(presets, searchQuery, filterType) {
        presets.filter { preset ->
            val matchesQuery = searchQuery.isEmpty() ||
                preset.name.contains(searchQuery, ignoreCase = true) ||
                preset.deviceModel?.contains(searchQuery, ignoreCase = true) == true
            val matchesFilter = when (filterType) {
                FilterType.ALL -> true
                FilterType.FAVORITES -> preset.isFavorite
                FilterType.HNCS -> preset.cameraParams?.hasselblad_hncs == true
                FilterType.FIND_X -> preset.deviceModel?.contains("Find X", ignoreCase = true) == true
                FilterType.RENO -> preset.deviceModel?.contains("Reno", ignoreCase = true) == true
                FilterType.NEW -> true
                FilterType.TRENDING -> true
            }
            matchesQuery && matchesFilter
        }
    }
    
    fun canDrawOverlays(): Boolean {
        return Settings.canDrawOverlays(context)
    }
    
    fun toggleFloatingWindow() {
        if (!canDrawOverlays()) {
            showPermissionDialog = true
            return
        }
        viewModel.setOverlayEnabled(true)
    }
    
    fun openOverlayPermissionSettings() {
        try {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:${context.packageName}")
            )
            context.startActivity(intent)
        } catch (e: Exception) {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:${context.packageName}")
            }
            context.startActivity(intent)
        }
    }
    
    Scaffold(
        modifier = modifier,
        topBar = {
            ColorOSTopBar(
                onSettingsClick = onSettingsClick,
                onFloatingWindowClick = { toggleFloatingWindow() }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(top = 4.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // 搜索栏
            item {
                ColorOSSearchBar(
                    query = searchQuery,
                    onQueryChange = { viewModel.onSearchQueryChanged(it) },
                    onClearQuery = { viewModel.onSearchQueryChanged("") }
                )
            }
            
            // 功能入口卡片
            item {
                ColorOSFeatureGrid(
                    onSceneDetectionClick = onSceneDetectionClick,
                    onFilterLibraryClick = onFilterLibraryClick,
                    onNativeCameraClick = onNativeCameraClick,
                    onPresetEditorClick = onPresetEditorClick,
                    onLutManagerClick = onLutManagerClick,
                    onWatermarkClick = onWatermarkClick,
                    onTestVerificationClick = onTestVerificationClick
                )
            }
            
            // 筛选栏
            item {
                ColorOSFilterBar(
                    selectedFilter = filterType,
                    onFilterSelected = { viewModel.onFilterTypeChanged(it) }
                )
            }
            
            // 内容区域
            if (isLoading) {
                items(3) { index ->
                    AnimatedVisibility(
                        visible = true,
                        enter = fadeIn(
                            animationSpec = tween(
                                durationMillis = AnimationConfig.STATE_TRANSITION_DURATION,
                                delayMillis = index * 80,
                                easing = AnimationConfig.ColorOSDecelerateEasing
                            )
                        ) + slideInVertically(
                            initialOffsetY = { 30 },
                            animationSpec = tween(
                                durationMillis = AnimationConfig.STATE_TRANSITION_DURATION,
                                delayMillis = index * 80,
                                easing = AnimationConfig.ColorOSDecelerateEasing
                            )
                        )
                    ) {
                        ColorOSSkeletonCard()
                    }
                }
            } else if (filteredPresets.isEmpty()) {
                item {
                    ColorOSEmptyState(
                        message = if (searchQuery.isNotEmpty() || filterType != FilterType.ALL)
                            "没有找到匹配的预设，试试其他关键词"
                        else "暂无预设，看看热门推荐",
                        isSearchEmpty = searchQuery.isEmpty() && filterType == FilterType.ALL
                    )
                }
            } else {
                itemsIndexed(
                    items = filteredPresets,
                    key = { _, preset -> preset.id }
                ) { index, preset ->
                    ColorOSAnimatedPresetCard(
                        preset = preset,
                        index = index,
                        onClick = { onPresetClick(preset) },
                        onFavoriteToggle = { viewModel.toggleFavorite(preset) }
                    )
                }
            }
        }
    }
    
    // 权限引导对话框
    if (showPermissionDialog) {
        ColorOSPermissionDialog(
            onDismiss = { showPermissionDialog = false },
            onAuthorize = {
                showPermissionDialog = false
                openOverlayPermissionSettings()
            }
        )
    }
}

// ==================== ColorOS 16 顶部栏 ====================
@Composable
private fun ColorOSTopBar(
    onSettingsClick: () -> Unit,
    onFloatingWindowClick: () -> Unit
) {
    LargeTopAppBar(
        title = {
            Column(
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = "小O帮帮",
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "OPPO 哈苏影像系统级参数中枢",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        colors = TopAppBarDefaults.largeTopAppBarColors(
            containerColor = MaterialTheme.colorScheme.background,
            scrolledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        actions = {
            // 悬浮窗按钮
            IconButton(onClick = onFloatingWindowClick) {
                Surface(
                    shape = androidx.compose.foundation.shape.CircleShape,
                    color = AccentPrimary.copy(alpha = 0.1f),
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Widgets,
                            contentDescription = "悬浮窗",
                            tint = AccentPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.width(8.dp))
            
            // 设置按钮
            IconButton(onClick = onSettingsClick) {
                Surface(
                    shape = androidx.compose.foundation.shape.CircleShape,
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "设置",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.width(8.dp))
        }
    )
}

// ==================== ColorOS 16 功能入口网格 ====================
@Composable
private fun ColorOSFeatureGrid(
    onSceneDetectionClick: () -> Unit,
    onFilterLibraryClick: () -> Unit,
    onNativeCameraClick: () -> Unit,
    onPresetEditorClick: () -> Unit,
    onLutManagerClick: () -> Unit,
    onWatermarkClick: () -> Unit,
    onTestVerificationClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = ColorOSShapes.large,
        tonalElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Text(
                text = "快捷功能",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                FeatureButton(
                    icon = Icons.Default.AutoAwesome,
                    label = "AI识别",
                    onClick = onSceneDetectionClick
                )
                FeatureButton(
                    icon = Icons.Default.FilterAlt,
                    label = "预设库",
                    onClick = onFilterLibraryClick
                )
                FeatureButton(
                    icon = Icons.Default.CameraAlt,
                    label = "相机参数",
                    onClick = onNativeCameraClick
                )
                FeatureButton(
                    icon = Icons.Default.Edit,
                    label = "编辑器",
                    onClick = onPresetEditorClick
                )
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                FeatureButton(
                    icon = Icons.Default.Storage,
                    label = "预设管理",
                    onClick = onLutManagerClick
                )
                FeatureButton(
                    icon = Icons.Default.WaterDrop,
                    label = "水印",
                    onClick = onWatermarkClick
                )
                FeatureButton(
                    icon = Icons.Default.VerifiedUser,
                    label = "测试",
                    onClick = onTestVerificationClick
                )
                Spacer(modifier = Modifier.width(56.dp))
            }
        }
    }
}

// ==================== 功能按钮 ====================
@Composable
private fun FeatureButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
            color = AccentPrimary.copy(alpha = 0.1f),
            modifier = Modifier.size(48.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = AccentPrimary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

// ==================== ColorOS 16 动画卡片 ====================
@Composable
private fun ColorOSAnimatedPresetCard(
    preset: Preset,
    index: Int,
    onClick: () -> Unit,
    onFavoriteToggle: () -> Unit
) {
    var visible by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        visible = true
    }
    
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(
            animationSpec = tween(
                durationMillis = AnimationConfig.STATE_TRANSITION_DURATION,
                delayMillis = index * 60,
                easing = AnimationConfig.ColorOSDecelerateEasing
            )
        ) + slideInVertically(
            initialOffsetY = { 40 },
            animationSpec = tween(
                durationMillis = AnimationConfig.STATE_TRANSITION_DURATION,
                delayMillis = index * 60,
                easing = AnimationConfig.ColorOSDecelerateEasing
            )
        )
    ) {
        ColorOSPresetCard(
            preset = preset,
            onClick = onClick,
            onFavoriteToggle = onFavoriteToggle,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            isNew = index < 3
        )
    }
}

// ==================== ColorOS 16 权限对话框 ====================
@Composable
private fun ColorOSPermissionDialog(
    onDismiss: () -> Unit,
    onAuthorize: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Surface(
                shape = androidx.compose.foundation.shape.CircleShape,
                color = AccentPrimary.copy(alpha = 0.1f),
                modifier = Modifier.size(64.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.PermDeviceInformation,
                        contentDescription = null,
                        tint = AccentPrimary,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        },
        title = {
            Text(
                text = "悬浮窗权限",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            Text(
                text = "需要授予悬浮窗权限才能使用实时参数显示功能",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        confirmButton = {
            FilledTonalButton(
                onClick = onAuthorize,
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = AccentPrimary,
                    contentColor = Color.White
                )
            ) {
                Text("去授权")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        },
        shape = ColorOSShapes.large,
        tonalElevation = 4.dp
    )
}

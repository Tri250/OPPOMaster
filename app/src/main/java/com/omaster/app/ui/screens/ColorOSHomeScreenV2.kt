package com.omaster.app.ui.screens

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.omaster.app.domain.model.Preset
import com.omaster.app.ui.animation.ColorOSAnimationDuration
import com.omaster.app.ui.animation.ColorOSEasing
import com.omaster.app.ui.animation.ColorOSScale
import com.omaster.app.ui.components.*
import com.omaster.app.ui.theme.*
import com.omaster.app.viewmodel.FilterType
import com.omaster.app.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ColorOSHomeScreenV2(
    onPresetClick: (Preset) -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MainViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val presets by viewModel.presets.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val filterType by viewModel.filterType.collectAsStateWithLifecycle()
    
    var showPermissionDialog by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }
    
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
    
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(300)
        isLoading = if (presets.isNotEmpty()) false else true
    }
    
    LaunchedEffect(presets) {
        if (presets.isNotEmpty()) {
            kotlinx.coroutines.delay(200)
            isLoading = false
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
        containerColor = Colors.Background
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            item {
                ColorOSTopBarV2(
                    onSettingsClick = onSettingsClick,
                    onFloatingWindowClick = { toggleFloatingWindow() }
                )
            }
            
            item {
                GlassAnimatedSearchBar(
                    query = searchQuery,
                    onQueryChange = { viewModel.onSearchQueryChanged(it) },
                    onClearQuery = { viewModel.onSearchQueryChanged("") },
                    placeholder = "搜索哈苏预设...",
                    delayMillis = 100
                )
            }
            
            item {
                GlassFilterChips(
                    selectedFilter = filterType,
                    onFilterSelected = { viewModel.onFilterTypeChanged(it) },
                    filters = colorOSFilters
                )
            }
            
            if (isLoading) {
                items(3) { index ->
                    GlassPresetCardShimmer(
                        modifier = Modifier.padding(horizontal = Spacing.ScreenPadding, vertical = Spacing.sm)
                    )
                }
            } else if (filteredPresets.isEmpty()) {
                item {
                    ColorOSEmptyStateV2(
                        message = if (searchQuery.isNotEmpty() || filterType != FilterType.ALL)
                            "没有找到匹配的预设，试试其他关键词"
                        else "暂无预设，看看热门推荐"
                    )
                }
            } else {
                itemsIndexed(
                    items = filteredPresets,
                    key = { _, preset -> preset.id }
                ) { index, preset ->
                    GlassPresetCard(
                        preset = preset,
                        index = index,
                        onClick = { onPresetClick(preset) },
                        onFavoriteToggle = { viewModel.toggleFavorite(preset) },
                        modifier = Modifier.padding(horizontal = Spacing.ScreenPadding, vertical = Spacing.sm),
                        isNew = index < 3
                    )
                }
            }
        }
    }
    
    if (showPermissionDialog) {
        GlassDialog(
            onDismiss = { showPermissionDialog = false },
            title = "悬浮窗权限",
            text = "需要授予悬浮窗权限才能使用实时参数显示功能",
            confirmButton = {
                GlassButton(
                    text = "去授权",
                    onClick = {
                        showPermissionDialog = false
                        openOverlayPermissionSettings()
                    }
                )
            },
            dismissButton = {
                TextButton(onClick = { showPermissionDialog = false }) {
                    Text("取消")
                }
            }
        )
    }
}

private val colorOSFilters = listOf(
    "all" to "全部",
    "favorite" to "收藏",
    "hncs" to "HNCS",
    "find_x" to "Find X",
    "reno" to "Reno"
)

@Composable
private fun ColorOSTopBarV2(
    onSettingsClick: () -> Unit,
    onFloatingWindowClick: () -> Unit
) {
    var isVisible by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(50)
        isVisible = true
    }
    
    val animatedAlpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(
            durationMillis = ColorOSAnimationDuration.MEDIUM,
            easing = ColorOSEasing.Decelerate
        ),
        label = "topBarAlpha"
    )
    
    val animatedOffsetY by animateFloatAsState(
        targetValue = if (isVisible) 0f else (-20).dp.value,
        animationSpec = tween(
            durationMillis = ColorOSAnimationDuration.MEDIUM,
            easing = ColorOSEasing.Decelerate
        ),
        label = "topBarOffsetY"
    )
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                alpha = animatedAlpha
                translationY = animatedOffsetY
            }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.ScreenPadding, vertical = Spacing.md),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = "OMaster",
                    style = Typography.DisplayMedium,
                    fontWeight = FontWeight.Bold,
                    color = Colors.HasselbladOrange
                )
                Text(
                    text = "OPPO 哈苏影像系统级参数中枢",
                    style = Typography.BodyMedium,
                    color = Colors.OnSurfaceVariant
                )
            }
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                GlassIconButton(
                    icon = Icons.Default.Widgets,
                    onClick = onFloatingWindowClick,
                    contentDescription = "悬浮窗",
                    size = 48.dp
                )
                
                GlassIconButton(
                    icon = Icons.Default.Settings,
                    onClick = onSettingsClick,
                    contentDescription = "设置",
                    size = 48.dp
                )
            }
        }
    }
}

@Composable
private fun ColorOSEmptyStateV2(
    message: String
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.huge),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(Colors.GlassBackground),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.SearchOff,
                    contentDescription = null,
                    tint = Colors.OnSurfaceVariant,
                    modifier = Modifier.size(48.dp)
                )
            }
            
            Text(
                text = message,
                style = Typography.BodyLarge,
                color = Colors.OnSurfaceVariant
            )
        }
    }
}

@Composable
fun ColorOSAnimatedPresetCardV2(
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
                durationMillis = ColorOSAnimationDuration.MEDIUM,
                delayMillis = index * 60,
                easing = ColorOSEasing.Decelerate
            )
        ) + slideInVertically(
            initialOffsetY = { 40 },
            animationSpec = tween(
                durationMillis = ColorOSAnimationDuration.MEDIUM,
                delayMillis = index * 60,
                easing = ColorOSEasing.Decelerate
            )
        )
    ) {
        GlassPresetCard(
            preset = preset,
            onClick = onClick,
            onFavoriteToggle = onFavoriteToggle,
            modifier = Modifier.padding(horizontal = Spacing.ScreenPadding, vertical = Spacing.sm),
            isNew = index < 3
        )
    }
}

@Composable
private fun GlassIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    contentDescription: String?,
    size: Dp
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed) ColorOSScale.Pressed else 1f,
        animationSpec = spring(
            dampingRatio = 0.8f,
            stiffness = 400f
        ),
        label = "iconButtonScale"
    )
    
    Box(
        modifier = Modifier
            .size(size)
            .scale(scale)
            .clip(CircleShape)
            .background(Colors.GlassBackground)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = Colors.OnSurfaceVariant,
            modifier = Modifier.size(size * 0.5f)
        )
    }
}

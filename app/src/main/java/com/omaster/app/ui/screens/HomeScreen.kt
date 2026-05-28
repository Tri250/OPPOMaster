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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.omaster.app.R
import com.omaster.app.floating.FloatingWindowToggleButton
import com.omaster.app.floating.PermissionGuidanceDialog
import com.omaster.app.model.Preset
import com.omaster.app.ui.animation.AnimationConfig
import com.omaster.app.ui.components.EnhancedFilterChips
import com.omaster.app.ui.components.EnhancedPresetCard
import com.omaster.app.ui.components.EnhancedSearchBar
import com.omaster.app.ui.components.PresetCardSkeleton
import com.omaster.app.ui.theme.*
import com.omaster.app.viewmodel.FilterType
import com.omaster.app.viewmodel.MainViewModel
import com.omaster.app.viewmodel.SceneType
import com.omaster.app.viewmodel.SortType
import com.omaster.app.viewmodel.StyleType

@Composable
fun HomeScreen(
    onPresetClick: (Preset) -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MainViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val presets by viewModel.presets.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val filterType by viewModel.filterType.collectAsStateWithLifecycle()
    val sceneType by viewModel.sceneType.collectAsStateWithLifecycle()
    val styleType by viewModel.styleType.collectAsStateWithLifecycle()
    val sortType by viewModel.sortType.collectAsStateWithLifecycle()
    
    var showPermissionDialog by remember { mutableStateOf(false) }
    var showSpecialGuidance by remember { mutableStateOf(false) }
    var isFloatingWindowEnabled by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }
    
    LaunchedEffect(presets) {
        if (presets.isNotEmpty()) {
            delay(300)
            isLoading = false
        }
    }
    
    val filteredPresets = remember(presets, searchQuery, filterType, sceneType, styleType, sortType) {
        presets.filter { preset ->
            val matchesQuery = searchQuery.isEmpty() ||
                    preset.name.contains(searchQuery, ignoreCase = true) ||
                    preset.deviceModel?.contains(searchQuery, ignoreCase = true) == true ||
                    preset.sections.any { it.title.contains(searchQuery, ignoreCase = true) }
            
            val matchesFilter = when (filterType) {
                FilterType.ALL -> true
                FilterType.FAVORITES -> preset.isFavorite
                FilterType.HNCS -> preset.cameraParams?.hasselblad_hncs == true
                FilterType.FIND_X -> preset.deviceModel?.contains("Find X", ignoreCase = true) == true
                FilterType.RENO -> preset.deviceModel?.contains("Reno", ignoreCase = true) == true
                FilterType.NEW -> true
                FilterType.TRENDING -> true
            }
            
            val matchesScene = when (sceneType) {
                SceneType.ALL -> true
                SceneType.PORTRAIT -> preset.name.contains("人像", ignoreCase = true) || preset.sections.any { it.title.contains("人像", ignoreCase = true) }
                SceneType.LANDSCAPE -> preset.name.contains("风景", ignoreCase = true) || preset.name.contains("风光", ignoreCase = true) || preset.sections.any { it.title.contains("风景", ignoreCase = true) }
                SceneType.NIGHT -> preset.name.contains("夜景", ignoreCase = true) || preset.sections.any { it.title.contains("夜景", ignoreCase = true) }
                SceneType.FOOD -> preset.name.contains("美食", ignoreCase = true) || preset.sections.any { it.title.contains("美食", ignoreCase = true) }
                SceneType.STREET -> preset.name.contains("街拍", ignoreCase = true) || preset.name.contains("街头", ignoreCase = true)
                SceneType.MACRO -> preset.name.contains("微距", ignoreCase = true) || preset.sections.any { it.title.contains("微距", ignoreCase = true) }
                SceneType.ARCHITECTURE -> preset.name.contains("建筑", ignoreCase = true) || preset.sections.any { it.title.contains("建筑", ignoreCase = true) }
            }
            
            val matchesStyle = when (styleType) {
                StyleType.ALL -> true
                StyleType.FILM -> preset.name.contains("胶片", ignoreCase = true) || preset.sections.any { it.content.contains("胶片", ignoreCase = true) }
                StyleType.RETRO -> preset.name.contains("复古", ignoreCase = true) || preset.sections.any { it.content.contains("复古", ignoreCase = true) }
                StyleType.FRESH -> preset.name.contains("清新", ignoreCase = true) || preset.sections.any { it.content.contains("清新", ignoreCase = true) }
                StyleType.VIBRANT -> preset.name.contains("鲜艳", ignoreCase = true) || preset.sections.any { it.content.contains("鲜艳", ignoreCase = true) }
                StyleType.BLACK_WHITE -> preset.name.contains("黑白", ignoreCase = true) || preset.sections.any { it.title.contains("黑白", ignoreCase = true) }
                StyleType.NATURAL -> preset.name.contains("自然", ignoreCase = true) || preset.sections.any { it.content.contains("自然", ignoreCase = true) }
                StyleType.WARM -> preset.name.contains("暖", ignoreCase = true) || preset.sections.any { it.content.contains("暖", ignoreCase = true) }
            }
            
            matchesQuery && matchesFilter && matchesScene && matchesStyle
        }.sortedWith { a, b ->
            when (sortType) {
                SortType.HOT -> b.sections.size.compareTo(a.sections.size)
                SortType.FAVORITE -> b.isFavorite.compareTo(a.isFavorite)
                SortType.NEWEST -> b.id.toIntOrNull()?.compareTo(a.id.toIntOrNull() ?: 0) ?: 0
            }
        }
    }
    
    val systemBrand = remember {
        when {
            Build.MANUFACTURER.lowercase().contains("oppo") ||
            Build.MANUFACTURER.lowercase().contains("realme") -> "ColorOS"
            Build.MANUFACTURER.lowercase().contains("oneplus") -> "OxygenOS"
            Build.MANUFACTURER.lowercase().contains("xiaomi") ||
            Build.MANUFACTURER.lowercase().contains("redmi") -> "MIUI"
            Build.MANUFACTURER.lowercase().contains("vivo") -> "OriginOS"
            else -> "原生Android"
        }
    }
    
    val specialGuidance = remember(systemBrand) {
        when {
            systemBrand == "ColorOS" -> """
                请按以下步骤解除ColorOS授权限制：
                
                1. 点击「去授权」按钮
                2. 在「权限与隐私」→「自启动管理」中找到OMaster
                3. 开启「允许自启动」和「允许后台活动」
                4. 返回后点击「允许」授予悬浮窗权限
            """.trimIndent()
            systemBrand == "OxygenOS" -> """
                请按以下步骤解除OxygenOS授权限制：
                
                1. 点击「去授权」按钮
                2. 在「电池」→「电池优化」中找到OMaster
                3. 选择「不允许」以防止后台被清理
                4. 返回后授予悬浮窗权限
            """.trimIndent()
            else -> ""
        }
    }
    
    val shouldShowSpecialGuidance = systemBrand == "ColorOS" || systemBrand == "OxygenOS"
    
    fun canDrawOverlays(): Boolean {
        return Settings.canDrawOverlays(context)
    }
    
    fun toggleFloatingWindow() {
        if (!canDrawOverlays()) {
            showPermissionDialog = true
            return
        }
        
        isFloatingWindowEnabled = !isFloatingWindowEnabled
        if (isFloatingWindowEnabled) {
            viewModel.setOverlayEnabled(true)
        } else {
            viewModel.setOverlayEnabled(false)
        }
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
            TopAppBar(
                title = {
                    Text(
                        text = "OMaster",
                        style = MaterialTheme.typography.displaySmall,
                        color = AccentPrimary
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                ),
                actions = {
                    FloatingWindowToggleButton(
                        isEnabled = isFloatingWindowEnabled,
                        onToggle = { toggleFloatingWindow() }
                    )
                    
                    IconButton(
                        onClick = onSettingsClick,
                        modifier = Modifier.semantics {
                            contentDescription = "设置"
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "设置",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                EnhancedSearchBar(
                    query = searchQuery,
                    onQueryChange = { viewModel.onSearchQueryChanged(it) },
                    onClearQuery = { viewModel.onSearchQueryChanged("") }
                )
            }
            item {
                EnhancedFilterChips(
                    selectedFilter = filterType,
                    onFilterSelected = { viewModel.onFilterTypeChanged(it) }
                )
            }
            item {
                SceneFilterChips(
                    selectedScene = sceneType,
                    onSceneSelected = { viewModel.onSceneTypeChanged(it) }
                )
            }
            item {
                StyleFilterChips(
                    selectedStyle = styleType,
                    onStyleSelected = { viewModel.onStyleTypeChanged(it) }
                )
            }
            item {
                SortSelector(
                    selectedSort = sortType,
                    onSortSelected = { viewModel.onSortTypeChanged(it) }
                )
            }
            
            if (isLoading) {
                items(3) { index ->
                    AnimatedVisibility(
                        visible = true,
                        enter = fadeIn(
                            animationSpec = tween(
                                durationMillis = 200,
                                delayMillis = index * 50
                            )
                        ) + slideInVertically(
                            initialOffsetY = { 20.dp.toPx().toInt() },
                            animationSpec = tween(
                                durationMillis = 200,
                                delayMillis = index * 50,
                                easing = AnimationConfig.LinearOutSlowInEasing
                            )
                        )
                    ) {
                        PresetCardSkeleton(
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }
            } else if (filteredPresets.isEmpty()) {
                item {
                    EmptyState(
                        message = if (searchQuery.isNotEmpty() || filterType != FilterType.ALL)
                            "没有找到匹配的预设，试试其他关键词" 
                        else "暂无预设，看看热门推荐",
                        isSearchEmpty = searchQuery.isEmpty() && filterType == FilterType.ALL,
                        modifier = Modifier.fillParentMaxWidth()
                    )
                }
            } else {
                itemsIndexed(filteredPresets, key = { _, preset -> preset.id }) { index, preset ->
                    AnimatedPresetCard(
                        preset = preset,
                        index = index,
                        onClick = { onPresetClick(preset) },
                        onFavoriteToggle = { viewModel.toggleFavorite(preset) }
                    )
                }
            }
        }
    }
    
    if (showPermissionDialog) {
        PermissionGuidanceDialog(
            systemBrand = systemBrand,
            specialGuidance = specialGuidance,
            onDismiss = { showPermissionDialog = false },
            onAuthorize = {
                showPermissionDialog = false
                openOverlayPermissionSettings()
            }
        )
    }
}

@Composable
fun AnimatedPresetCard(
    preset: Preset,
    index: Int,
    onClick: () -> Unit,
    onFavoriteToggle: () -> Unit
) {
    val delay = index * 50L
    
    AnimatedVisibility(
        visible = true,
        enter = fadeIn(
            animationSpec = tween(
                durationMillis = AnimationConfig.STATE_TRANSITION_DURATION,
                delayMillis = delay.toInt(),
                easing = AnimationConfig.LinearOutSlowInEasing
            )
        ) + slideInVertically(
            initialOffsetY = { 20.dp.toPx().toInt() },
            animationSpec = tween(
                durationMillis = AnimationConfig.STATE_TRANSITION_DURATION,
                delayMillis = delay.toInt(),
                easing = AnimationConfig.LinearOutSlowInEasing
            )
        )
    ) {
        EnhancedPresetCard(
            preset = preset,
            onClick = onClick,
            onFavoriteToggle = onFavoriteToggle,
            modifier = Modifier.padding(horizontal = 16.dp),
            isNew = index < 3
        )
    }
}

@Composable
fun EmptyState(
    message: String,
    isSearchEmpty: Boolean = false,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .padding(32.dp)
            .semantics { contentDescription = message },
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AnimatedVisibility(
            visible = true,
            enter = fadeIn(
                animationSpec = tween(500)
            ) + slideInVertically(
                initialOffsetY = { 20.dp.toPx().toInt() },
                animationSpec = tween(500, easing = AnimationConfig.LinearOutSlowInEasing)
            )
        ) {
            if (isSearchEmpty) {
                Image(
                    painter = painterResource(id = R.drawable.empty_presets),
                    contentDescription = "暂无预设",
                    modifier = Modifier.size(120.dp)
                )
            } else {
                Image(
                    painter = painterResource(id = R.drawable.empty_search),
                    contentDescription = "搜索无结果",
                    modifier = Modifier.size(120.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        AnimatedVisibility(
            visible = true,
            enter = fadeIn(
                animationSpec = tween(300, delayMillis = 200)
            )
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        AnimatedVisibility(
            visible = true,
            enter = fadeIn(
                animationSpec = tween(300, delayMillis = 300)
            )
        ) {
            Text(
                text = if (isSearchEmpty) "期待更多精彩预设" else "换个关键词试试",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
        }
    }
}

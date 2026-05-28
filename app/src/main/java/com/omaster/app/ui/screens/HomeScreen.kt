package com.omaster.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.omaster.app.R
import com.omaster.app.model.Preset
import com.omaster.app.service.FloatingWindowService
import com.omaster.app.ui.components.EnhancedFilterChips
import com.omaster.app.ui.components.EnhancedPresetCard
import com.omaster.app.ui.components.EnhancedSearchBar
import com.omaster.app.ui.theme.*
import com.omaster.app.utils.OverlayPermissionHelper
import com.omaster.app.viewmodel.FilterType
import com.omaster.app.viewmodel.MainViewModel
import kotlinx.coroutines.launch
import timber.log.Timber

@Composable
fun HomeScreen(
    onPresetClick: (Preset) -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MainViewModel = hiltViewModel()
) {
    val presets by viewModel.presets.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val filterType by viewModel.filterType.collectAsStateWithLifecycle()
    val overlayEnabled by viewModel.overlayEnabled.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    val filteredPresets = remember(presets, searchQuery, filterType) {
        presets.filter { preset ->
            val matchesQuery = searchQuery.isEmpty() ||
                    preset.name.contains(searchQuery, ignoreCase = true) ||
                    preset.deviceModel?.contains(searchQuery, ignoreCase = true) == true ||
                    preset.cameraParams?.filter?.contains(searchQuery, ignoreCase = true) == true
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
    
    fun toggleOverlayWithPermission() {
        if (OverlayPermissionHelper.canDrawOverlays(context)) {
            viewModel.setOverlayEnabled(!overlayEnabled)
            if (!overlayEnabled) {
                FloatingWindowService.updatePresets(presets)
                FloatingWindowService.showOverlay(context)
            } else {
                FloatingWindowService.hideOverlay(context)
            }
        } else {
            OverlayPermissionHelper.requestOverlayPermission(
                context = context,
                onGranted = {
                    viewModel.setOverlayEnabled(true)
                    FloatingWindowService.updatePresets(presets)
                    FloatingWindowService.showOverlay(context)
                },
                onDenied = {
                    Timber.tag("OMaster").d("Overlay permission denied")
                }
            )
        }
    }
    
    LaunchedEffect(presets) {
        if (overlayEnabled) {
            FloatingWindowService.updatePresets(presets)
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
                    IconButton(
                        onClick = { toggleOverlayWithPermission() },
                        modifier = Modifier.semantics {
                            contentDescription = if (overlayEnabled) "关闭悬浮窗" else "开启悬浮窗"
                        }
                    ) {
                        Icon(
                            imageVector = if (overlayEnabled) Icons.Default.Layers else Icons.Default.LayersClear,
                            contentDescription = if (overlayEnabled) "悬浮窗已开启" else "悬浮窗已关闭",
                            tint = if (overlayEnabled) AccentPrimary else MaterialTheme.colorScheme.onBackground
                        )
                    }
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
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
            if (filteredPresets.isEmpty()) {
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
                items(filteredPresets, key = { it.id }) { preset ->
                    EnhancedPresetCard(
                        preset = preset,
                        onClick = { 
                            FloatingWindowService.setCurrentPreset(preset)
                            onPresetClick(preset) 
                        },
                        onFavoriteToggle = { viewModel.toggleFavorite(preset) },
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }
        }
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
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = if (isSearchEmpty) "期待更多精彩预设" else "换个关键词试试",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )
    }
}

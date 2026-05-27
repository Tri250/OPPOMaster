package com.omaster.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.omaster.app.model.Preset
import com.omaster.app.ui.components.FilterChips
import com.omaster.app.ui.components.PresetCard
import com.omaster.app.ui.components.SearchBar
import com.omaster.app.ui.theme.*
import com.omaster.app.viewmodel.FilterType
import com.omaster.app.viewmodel.MainViewModel

@Composable
fun HomeScreen(
    onPresetClick: (Preset) -> Unit,
    onSettingsClick: () -> Unit,
    onWatermarkClick: () -> Unit,
    onCameraParamsClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MainViewModel = hiltViewModel()
) {
    val presets by viewModel.presets.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val filterType by viewModel.filterType.collectAsStateWithLifecycle()

    val filteredPresets = remember(presets, searchQuery, filterType) {
        presets.filter { preset ->
            val matchesQuery = searchQuery.isEmpty() ||
                    preset.name.contains(searchQuery, ignoreCase = true)
            val matchesFilter = when (filterType) {
                FilterType.ALL -> true
                FilterType.FAVORITES -> preset.isFavorite
                FilterType.HNCS -> preset.cameraParams?.hasselblad_hncs == true
                FilterType.FIND_X -> preset.deviceModel?.contains("Find X", ignoreCase = true) == true
                FilterType.RENO -> preset.deviceModel?.contains("Reno", ignoreCase = true) == true
            }
            matchesQuery && matchesFilter
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
                    IconButton(onClick = onSettingsClick) {
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
                SearchBar(
                    query = searchQuery,
                    onQueryChange = { viewModel.onSearchQueryChanged(it) },
                    onClearQuery = { viewModel.onSearchQueryChanged("") }
                )
            }
            item {
                FilterChips(
                    selectedFilter = filterType,
                    onFilterSelected = { viewModel.onFilterTypeChanged(it) }
                )
            }
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onWatermarkClick,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("添加水印")
                        }
                        Button(
                            onClick = onCameraParamsClick,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("相机参数")
                        }
                    }
                }
            }
            if (filteredPresets.isEmpty()) {
                item {
                    EmptyState(
                        message = if (searchQuery.isNotEmpty() || filterType != FilterType.ALL)
                            "没有找到匹配的预设" else "暂无预设",
                        modifier = Modifier.fillParentMaxWidth()
                    )
                }
            } else {
                items(filteredPresets, key = { it.id }) { preset ->
                    PresetCard(
                        preset = preset,
                        onClick = { onPresetClick(preset) },
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
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

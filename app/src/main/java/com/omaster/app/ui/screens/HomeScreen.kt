package com.omaster.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.omaster.app.model.Preset
import com.omaster.app.ui.theme.*
import com.omaster.app.viewmodel.FilterType
import com.omaster.app.viewmodel.MainViewModel

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
    
    Scaffold(
        modifier = modifier,
        topBar = {
            HomeTopBar(
                onSettingsClick = onSettingsClick,
                searchQuery = searchQuery,
                onSearchQueryChange = { viewModel.onSearchQueryChanged(it) },
                onClearSearch = { viewModel.onSearchQueryChanged("") }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            FilterChipsRow(
                selectedFilter = filterType,
                onFilterSelected = { viewModel.onFilterTypeChanged(it) }
            )
            
            if (filteredPresets.isEmpty()) {
                EmptyState(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp)
                )
            } else {
                PresetGrid(
                    presets = filteredPresets,
                    onPresetClick = onPresetClick,
                    onFavoriteToggle = { viewModel.toggleFavorite(it) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeTopBar(
    onSettingsClick: () -> Unit,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onClearSearch: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "OMaster",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "OPPO 哈苏影像专家",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(
                    onClick = { },
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Icon(
                        imageVector = Icons.Default.QrCodeScanner,
                        contentDescription = "扫码",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                IconButton(
                    onClick = onSettingsClick,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "设置",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        SearchBar(
            query = searchQuery,
            onQueryChange = onSearchQueryChange,
            onClearQuery = onClearSearch,
            onSearch = { }
        )
    }
}

@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClearQuery: () -> Unit,
    onSearch: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "搜索",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
            
            TextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier.weight(1f),
                placeholder = {
                    Text(
                        text = "搜索影像预设...",
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                singleLine = true
            )
            
            if (query.isNotEmpty()) {
                IconButton(
                    onClick = onClearQuery,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "清除",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun FilterChipsRow(
    selectedFilter: FilterType,
    onFilterSelected: (FilterType) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterChip(
            selected = selectedFilter == FilterType.ALL,
            onClick = { onFilterSelected(FilterType.ALL) },
            label = { Text("全部") },
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = AccentPrimary,
                selectedLabelColor = Color.White
            )
        )
        
        FilterChip(
            selected = selectedFilter == FilterType.HNCS,
            onClick = { onFilterSelected(FilterType.HNCS) },
            label = { Text("哈苏 HNCS") },
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = HasselbladOrange,
                selectedLabelColor = DeepSpace
            )
        )
        
        FilterChip(
            selected = selectedFilter == FilterType.FAVORITES,
            onClick = { onFilterSelected(FilterType.FAVORITES) },
            label = { Text("我的收藏") },
            leadingIcon = if (selectedFilter == FilterType.FAVORITES) {
                {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
            } else null,
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = AccentPrimary,
                selectedLabelColor = Color.White
            )
        )
    }
}

@Composable
private fun PresetGrid(
    presets: List<Preset>,
    onPresetClick: (Preset) -> Unit,
    onFavoriteToggle: (Preset) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = modifier.padding(horizontal = 12.dp),
        contentPadding = PaddingValues(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(presets, key = { it.id }) { preset ->
            PresetCard(
                preset = preset,
                onClick = { onPresetClick(preset) },
                onFavoriteToggle = { onFavoriteToggle(preset) }
            )
        }
    }
}

@Composable
private fun PresetCard(
    preset: Preset,
    onClick: () -> Unit,
    onFavoriteToggle: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.75f)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = "https://picsum.photos/seed/${preset.coverPath}/400/533",
                contentDescription = preset.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Transparent,
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                            )
                        )
                    )
            )
            
            if (preset.cameraParams?.hasselblad_hncs == true) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp),
                    color = HasselbladOrange,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "HNCS",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = DeepSpace,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            IconButton(
                onClick = onFavoriteToggle,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.3f))
            ) {
                Icon(
                    imageVector = if (preset.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = if (preset.isFavorite) "取消收藏" else "收藏",
                    tint = if (preset.isFavorite) AccentPrimary else Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
            
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(12.dp)
            ) {
                Text(
                    text = preset.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = FontWeight.SemiBold
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                preset.deviceModel?.let { device ->
                    Text(
                        text = device,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                
                preset.cameraParams?.let { params ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        if (params.iso > 0) {
                            Text(
                                text = "ISO ${params.iso}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        params.wb?.let { wb ->
                            Text(
                                text = wb,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.PhotoCamera,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "暂无影像推荐",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "换个筛选条件试试",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        )
    }
}

package com.omaster.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.omaster.app.model.Preset
import com.omaster.app.ui.theme.*
import com.omaster.app.viewmodel.FilterType
import com.omaster.app.viewmodel.MainViewModel

enum class PresetCategory(val displayName: String) {
    ALL("全部"),
    HASSELBLAD("哈苏"),
    PORTRAIT("人像"),
    LANDSCAPE("风景"),
    STREET("街拍"),
    FOOD("美食"),
    NIGHT("夜景")
}

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
    var selectedCategory by remember { mutableStateOf(PresetCategory.ALL) }

    val filteredPresets = remember(presets, searchQuery, filterType, selectedCategory) {
        presets.filter { preset ->
            val matchesSearch = searchQuery.isEmpty() ||
                preset.name.contains(searchQuery, ignoreCase = true) ||
                preset.deviceModel?.contains(searchQuery, ignoreCase = true) == true

            val matchesFilter = when (filterType) {
                FilterType.ALL -> true
                FilterType.FAVORITES -> preset.isFavorite
                FilterType.HASSELBLAD -> preset.cameraParams?.hasselblad_hncs == true
                FilterType.FIND_X -> preset.deviceModel?.contains("Find X", ignoreCase = true) == true
                FilterType.RENO -> preset.deviceModel?.contains("Reno", ignoreCase = true) == true
                else -> true
            }

            val matchesCategory = when (selectedCategory) {
                PresetCategory.ALL -> true
                PresetCategory.HASSELBLAD -> preset.cameraParams?.hasselblad_hncs == true
                PresetCategory.PORTRAIT -> preset.name.contains("人像", ignoreCase = true) || preset.name.contains("portrait", ignoreCase = true)
                PresetCategory.LANDSCAPE -> preset.name.contains("风景", ignoreCase = true) || preset.name.contains("landscape", ignoreCase = true)
                PresetCategory.STREET -> preset.name.contains("街拍", ignoreCase = true) || preset.name.contains("street", ignoreCase = true)
                PresetCategory.FOOD -> preset.name.contains("美食", ignoreCase = true) || preset.name.contains("food", ignoreCase = true)
                PresetCategory.NIGHT -> preset.name.contains("夜景", ignoreCase = true) || preset.name.contains("night", ignoreCase = true)
            }

            matchesSearch && matchesFilter && matchesCategory
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
            CategoryTabs(
                selectedCategory = selectedCategory,
                onCategorySelected = { selectedCategory = it }
            )

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
fun HomeTopBar(
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
                    text = "小O帮帮",
                    style = MaterialTheme.typography.headlineMedium,
                    color = AccentPrimary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "精选影像推荐",
                    style = MaterialTheme.typography.bodyMedium,
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
fun SearchBar(
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

            androidx.compose.foundation.text.BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier.weight(1f),
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurface
                ),
                decorationBox = { innerTextField ->
                    if (query.isEmpty()) {
                        Text(
                            text = "搜索预设名称、机型...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                    innerTextField()
                }
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
fun CategoryTabs(
    selectedCategory: PresetCategory,
    onCategorySelected: (PresetCategory) -> Unit
) {
    var scrollState = rememberScrollState()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .horizontalScroll(scrollState),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        PresetCategory.values().forEach { category ->
            FilterChip(
                selected = selectedCategory == category,
                onClick = { onCategorySelected(category) },
                label = {
                    Text(
                        text = category.displayName,
                        style = MaterialTheme.typography.labelLarge
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = if (category == PresetCategory.HASSELBLAD) HasselbladOrange else AccentPrimary,
                    selectedLabelColor = if (category == PresetCategory.HASSELBLAD) DeepSpace else Color.White
                ),
                shape = RoundedCornerShape(24.dp)
            )
        }
    }
}

@Composable
fun FilterChipsRow(
    selectedFilter: FilterType,
    onFilterSelected: (FilterType) -> Unit
) {
    val filters = listOf(
        FilterType.ALL to "全部",
        FilterType.FAVORITES to "我的收藏",
        FilterType.HASSELBLAD to "HNCS"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        filters.forEach { (filter, label) ->
            FilterChip(
                selected = selectedFilter == filter,
                onClick = { onFilterSelected(filter) },
                label = {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelMedium
                    )
                },
                leadingIcon = if (filter == FilterType.FAVORITES) {
                    {
                        Icon(
                            imageVector = if (selectedFilter == FilterType.FAVORITES) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                } else null,
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = if (filter == FilterType.HASSELBLAD) HasselbladOrange else AccentPrimary,
                    selectedLabelColor = if (filter == FilterType.HASSELBLAD) DeepSpace else Color.White
                ),
                shape = RoundedCornerShape(20.dp)
            )
        }
    }
}

@Composable
fun PresetGrid(
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
fun PresetCard(
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
                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        androidx.compose.ui.graphics.Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
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
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.4f))
            ) {
                Icon(
                    imageVector = if (preset.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = if (preset.isFavorite) "取消收藏" else "收藏",
                    tint = if (preset.isFavorite) AccentPrimary else Color.White,
                    modifier = Modifier.size(18.dp)
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
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(4.dp))

                preset.deviceModel?.let { device ->
                    Text(
                        text = device,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }

                preset.cameraParams?.let { params ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Text(
                            text = "ISO${params.iso}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        params.wb?.let {
                            Text(
                                text = it,
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
fun EmptyState(modifier: Modifier = Modifier) {
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
            text = "暂无预设",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "尝试其他搜索或筛选条件",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        )
    }
}

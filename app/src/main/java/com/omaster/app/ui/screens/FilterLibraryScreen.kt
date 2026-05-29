package com.omaster.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.omaster.app.model.Preset
import com.omaster.app.ui.theme.*
import com.omaster.app.ui.animation.*

enum class StyleCategory(val displayName: String, val count: Int) {
    ALL("全部", 0),
    FILM("胶片", 12),
    VINTAGE("复古", 15),
    FRESH("清新", 8),
    PORTRAIT("人像", 12),
    LANDSCAPE("风光", 15),
    FOOD("美食", 8),
    NIGHT("夜景", 7)
}

enum class DeviceCategory(val displayName: String, val count: Int) {
    ALL("全部机型", 0),
    FIND_X7("OPPO Find X7", 25),
    FIND_X6("OPPO Find X6", 20),
    ONEPLUS_12("一加 12", 18),
    REALME_GT5_PRO("realme GT5 Pro", 15)
}

enum class SortOption(val displayName: String) {
    TRENDING("热度"),
    FAVORITES("收藏"),
    NEWEST("最新")
}

enum class ViewMode {
    GRID,
    LIST
}

@Composable
fun FilterLibraryScreen(
    onBack: () -> Unit,
    onPresetClick: (Preset) -> Unit,
    modifier: Modifier = Modifier,
    presets: List<Preset> = remember { generateMockPresets() }
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedStyleCategory by remember { mutableStateOf(StyleCategory.ALL) }
    var selectedDeviceCategory by remember { mutableStateOf(DeviceCategory.ALL) }
    var selectedSortOption by remember { mutableStateOf(SortOption.TRENDING) }
    var viewMode by remember { mutableStateOf(ViewMode.GRID) }
    var selectedPresets by remember { mutableStateOf(setOf<Int>()) }
    var isSelectionMode by remember { mutableStateOf(false) }

    val filteredPresets = remember(presets, searchQuery, selectedStyleCategory, selectedDeviceCategory, selectedSortOption) {
        presets.filter { preset ->
            val matchesSearch = searchQuery.isEmpty() ||
                preset.name.contains(searchQuery, ignoreCase = true) ||
                preset.author?.contains(searchQuery, ignoreCase = true) == true ||
                preset.style?.contains(searchQuery, ignoreCase = true) == true ||
                preset.scene?.contains(searchQuery, ignoreCase = true) == true

            val matchesStyle = when (selectedStyleCategory) {
                StyleCategory.ALL -> true
                else -> preset.style?.equals(selectedStyleCategory.displayName, ignoreCase = true) == true
            }

            val matchesDevice = when (selectedDeviceCategory) {
                DeviceCategory.ALL -> true
                else -> preset.deviceModel?.contains(selectedDeviceCategory.displayName.split(" ")[0], ignoreCase = true) == true
            }

            matchesSearch && matchesStyle && matchesDevice
        }
    }

    val sortedPresets = remember(filteredPresets, selectedSortOption) {
        when (selectedSortOption) {
            SortOption.TRENDING -> filteredPresets.sortedByDescending { it.popularity }
            SortOption.FAVORITES -> filteredPresets.sortedByDescending { it.favoriteCount }
            SortOption.NEWEST -> filteredPresets.sortedByDescending { it.createdAt }
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            FilterLibraryTopBar(
                onBack = onBack,
                searchQuery = searchQuery,
                onSearchQueryChange = { searchQuery = it },
                onClearSearch = { searchQuery = "" }
            )
        },
        bottomBar = {
            AnimatedVisibility(
                visible = isSelectionMode && selectedPresets.isNotEmpty(),
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
            ) {
                BatchOperationBar(
                    selectedCount = selectedPresets.size,
                    onApply = { /* Batch apply logic */ },
                    onCancel = {
                        selectedPresets.clear()
                        isSelectionMode = false
                    }
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            StyleCategoryChips(
                selectedCategory = selectedStyleCategory,
                onCategorySelected = { selectedStyleCategory = it }
            )

            DeviceCategoryChips(
                selectedCategory = selectedDeviceCategory,
                onCategorySelected = { selectedDeviceCategory = it }
            )

            SortAndViewOptions(
                selectedSort = selectedSortOption,
                onSortSelected = { selectedSortOption = it },
                viewMode = viewMode,
                onViewModeChange = { viewMode = it }
            )

            if (sortedPresets.isEmpty()) {
                EmptyState()
            } else {
                when (viewMode) {
                    ViewMode.GRID -> PresetGridView(
                        presets = sortedPresets,
                        selectedPresets = selectedPresets,
                        isSelectionMode = isSelectionMode,
                        onPresetClick = { preset ->
                            if (isSelectionMode) {
                                selectedPresets = if (selectedPresets.contains(preset.id)) {
                                    selectedPresets - preset.id
                                } else {
                                    selectedPresets + preset.id
                                }
                            } else {
                                onPresetClick(preset)
                            }
                        },
                        onLongPress = { preset ->
                            isSelectionMode = true
                            selectedPresets = selectedPresets + preset.id
                        },
                        modifier = Modifier.weight(1f)
                    )
                    ViewMode.LIST -> PresetListView(
                        presets = sortedPresets,
                        selectedPresets = selectedPresets,
                        isSelectionMode = isSelectionMode,
                        onPresetClick = { preset ->
                            if (isSelectionMode) {
                                selectedPresets = if (selectedPresets.contains(preset.id)) {
                                    selectedPresets - preset.id
                                } else {
                                    selectedPresets + preset.id
                                }
                            } else {
                                onPresetClick(preset)
                            }
                        },
                        onLongPress = { preset ->
                            isSelectionMode = true
                            selectedPresets = selectedPresets + preset.id
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterLibraryTopBar(
    onBack: () -> Unit,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onClearSearch: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.background,
        shadowElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "返回",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Column {
                        Text(
                            text = "预设库",
                            style = MaterialTheme.typography.headlineSmall,
                            color = OppoSunriseGold,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "按风格、场景、适配机型分类，支持全文搜索和多种排序方式",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            LibrarySearchBar(
                query = searchQuery,
                onQueryChange = onSearchQueryChange,
                onClearQuery = onClearSearch
            )
        }
    }
}

@Composable
fun LibrarySearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClearQuery: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = if (query.isNotEmpty()) {
            BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
        } else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "搜索",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
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
                            text = "搜索预设名称、作者、风格、场景...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                    innerTextField()
                }
            )

            AnimatedVisibility(
                visible = query.isNotEmpty(),
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Surface(
                    onClick = onClearQuery,
                    shape = CircleShape,
                    color = Color.Transparent
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "清除",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun StyleCategoryChips(
    selectedCategory: StyleCategory,
    onCategorySelected: (StyleCategory) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        StyleCategory.values().forEach { category ->
            LibraryFilterChip(
                selected = selectedCategory == category,
                onClick = { onCategorySelected(category) },
                label = if (category.count > 0) "${category.displayName} (${category.count})" else category.displayName
            )
        }
    }
}

@Composable
fun DeviceCategoryChips(
    selectedCategory: DeviceCategory,
    onCategorySelected: (DeviceCategory) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        DeviceCategory.values().forEach { category ->
            LibraryFilterChip(
                selected = selectedCategory == category,
                onClick = { onCategorySelected(category) },
                label = if (category.count > 0) "${category.displayName} (${category.count})" else category.displayName,
                isPrimary = category != DeviceCategory.ALL
            )
        }
    }
}

@Composable
fun LibraryFilterChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: String,
    isPrimary: Boolean = false
) {
    val selectedColor = if (isPrimary) AccentPrimary else OppoSunriseGold

    val backgroundColor by animateColorAsState(
        targetValue = if (selected) {
            selectedColor.copy(alpha = 0.12f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        },
        animationSpec = tween(
            durationMillis = OppoAnimationDuration.Fast,
            easing = OppoEasing.Standard
        ),
        label = "chip_bg"
    )

    val contentColor by animateColorAsState(
        targetValue = if (selected) {
            selectedColor
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        animationSpec = tween(
            durationMillis = OppoAnimationDuration.Fast,
            easing = OppoEasing.Standard
        ),
        label = "chip_content"
    )

    val borderColor by animateColorAsState(
        targetValue = if (selected) {
            selectedColor.copy(alpha = 0.5f)
        } else {
            Color.Transparent
        },
        animationSpec = tween(
            durationMillis = OppoAnimationDuration.Fast,
            easing = OppoEasing.Standard
        ),
        label = "chip_border"
    )

    Surface(
        onClick = onClick,
        color = backgroundColor,
        shape = RoundedCornerShape(100.dp),
        border = if (selected) BorderStroke(1.dp, borderColor) else null
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            style = MaterialTheme.typography.labelMedium,
            color = contentColor,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
        )
    }
}

@Composable
fun SortAndViewOptions(
    selectedSort: SortOption,
    onSortSelected: (SortOption) -> Unit,
    viewMode: ViewMode,
    onViewModeChange: (ViewMode) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            SortOption.values().forEach { option ->
                SortTab(
                    selected = selectedSort == option,
                    onClick = { onSortSelected(option) },
                    icon = when (option) {
                        SortOption.TRENDING -> Icons.Default.TrendingUp
                        SortOption.FAVORITES -> Icons.Default.Favorite
                        SortOption.NEWEST -> Icons.Default.Schedule
                    },
                    label = option.displayName
                )
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            ViewModeButton(
                selected = viewMode == ViewMode.GRID,
                onClick = { onViewModeChange(ViewMode.GRID) },
                icon = Icons.Default.GridView
            )
            ViewModeButton(
                selected = viewMode == ViewMode.LIST,
                onClick = { onViewModeChange(ViewMode.LIST) },
                icon = Icons.Default.ViewList
            )
        }
    }
}

@Composable
fun SortTab(
    selected: Boolean,
    onClick: () -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (selected) {
            AccentPrimary.copy(alpha = 0.12f)
        } else {
            Color.Transparent
        },
        animationSpec = tween(
            durationMillis = OppoAnimationDuration.Fast,
            easing = OppoEasing.Standard
        ),
        label = "sort_bg"
    )

    val contentColor by animateColorAsState(
        targetValue = if (selected) {
            AccentPrimary
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        animationSpec = tween(
            durationMillis = OppoAnimationDuration.Fast,
            easing = OppoEasing.Standard
        ),
        label = "sort_content"
    )

    Surface(
        onClick = onClick,
        color = backgroundColor,
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = contentColor
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = contentColor,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
            )
        }
    }
}

@Composable
fun ViewModeButton(
    selected: Boolean,
    onClick: () -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (selected) {
            AccentPrimary.copy(alpha = 0.12f)
        } else {
            Color.Transparent
        },
        animationSpec = tween(
            durationMillis = OppoAnimationDuration.Fast,
            easing = OppoEasing.Standard
        ),
        label = "view_bg"
    )

    val iconColor by animateColorAsState(
        targetValue = if (selected) {
            AccentPrimary
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        animationSpec = tween(
            durationMillis = OppoAnimationDuration.Fast,
            easing = OppoEasing.Standard
        ),
        label = "view_icon"
    )

    Surface(
        onClick = onClick,
        color = backgroundColor,
        shape = RoundedCornerShape(8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier
                .size(36.dp)
                .padding(8.dp),
            tint = iconColor
        )
    }
}

@Composable
fun PresetGridView(
    presets: List<Preset>,
    selectedPresets: Set<Int>,
    isSelectionMode: Boolean,
    onPresetClick: (Preset) -> Unit,
    onLongPress: (Preset) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = modifier.padding(horizontal = 10.dp),
        contentPadding = PaddingValues(vertical = 8.dp, horizontal = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(presets, key = { it.id }) { preset ->
            LibraryPresetCard(
                preset = preset,
                onClick = { onPresetClick(preset) },
                onLongClick = { onLongPress(preset) },
                isSelected = selectedPresets.contains(preset.id),
                isSelectionMode = isSelectionMode
            )
        }
    }
}

@Composable
fun PresetListView(
    presets: List<Preset>,
    selectedPresets: Set<Int>,
    isSelectionMode: Boolean,
    onPresetClick: (Preset) -> Unit,
    onLongPress: (Preset) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.padding(horizontal = 10.dp),
        contentPadding = PaddingValues(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(presets, key = { it.id }) { preset ->
            LibraryPresetListItem(
                preset = preset,
                onClick = { onPresetClick(preset) },
                onLongClick = { onLongPress(preset) },
                isSelected = selectedPresets.contains(preset.id),
                isSelectionMode = isSelectionMode
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LibraryPresetCard(
    preset: Preset,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    isSelected: Boolean,
    isSelectionMode: Boolean
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "card_scale"
    )

    val elevation by animateDpAsState(
        targetValue = if (isPressed) 1.dp else 3.dp,
        animationSpec = tween(
            durationMillis = OppoAnimationDuration.Fast,
            easing = OppoEasing.Standard
        ),
        label = "card_elevation"
    )

    val borderColor by animateColorAsState(
        targetValue = if (isSelected) AccentPrimary else Color.Transparent,
        animationSpec = tween(
            durationMillis = OppoAnimationDuration.Fast,
            easing = OppoEasing.Standard
        ),
        label = "card_border"
    )

    Card(
        onClick = onClick,
        interactionSource = interactionSource,
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(0.78f)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = elevation),
        border = if (isSelected) BorderStroke(2.dp, borderColor) else null
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
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.98f)
                            ),
                            startY = 0f,
                            endY = 350f
                        )
                    )
            )

            if (preset.hasNewTag) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(10.dp),
                    color = AccentPrimary,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "NEW",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (preset.cameraParams?.hasselblad_hncs == true) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(if (preset.hasNewTag) 48.dp else 10.dp, 10.dp),
                    color = HasselbladOrangePro,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "HNCS",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = OppoDeepSpace,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (isSelectionMode) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp),
                    shape = CircleShape,
                    color = if (isSelected) AccentPrimary else Color.Black.copy(alpha = 0.35f)
                ) {
                    Box(
                        modifier = Modifier.size(36.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "已选择",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            } else {
                Surface(
                    onClick = { },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp),
                    shape = CircleShape,
                    color = Color.Black.copy(alpha = 0.35f)
                ) {
                    Box(
                        modifier = Modifier.size(36.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (preset.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "收藏",
                            tint = if (preset.isFavorite) OppoSunriseGold else Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
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

                preset.author?.let { author ->
                    Text(
                        text = "by $author",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    preset.style?.let { style ->
                        Text(
                            text = style,
                            style = MaterialTheme.typography.labelSmall,
                            color = AccentPrimary
                        )
                    }
                    preset.scene?.let { scene ->
                        Text(
                            text = scene,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.TrendingUp,
                            contentDescription = "热度",
                            modifier = Modifier.size(12.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${preset.popularity}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = "收藏",
                            modifier = Modifier.size(12.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${preset.favoriteCount}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LibraryPresetListItem(
    preset: Preset,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    isSelected: Boolean,
    isSelectionMode: Boolean
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "item_scale"
    )

    val borderColor by animateColorAsState(
        targetValue = if (isSelected) AccentPrimary else Color.Transparent,
        animationSpec = tween(
            durationMillis = OppoAnimationDuration.Fast,
            easing = OppoEasing.Standard
        ),
        label = "item_border"
    )

    Card(
        onClick = onClick,
        interactionSource = interactionSource,
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = if (isSelected) BorderStroke(2.dp, borderColor) else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = "https://picsum.photos/seed/${preset.coverPath}/200/200",
                contentDescription = preset.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(8.dp))
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (preset.hasNewTag) {
                        Surface(
                            color = AccentPrimary,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "NEW",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    if (preset.cameraParams?.hasselblad_hncs == true) {
                        Surface(
                            color = HasselbladOrangePro,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "HNCS",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = OppoDeepSpace,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Text(
                    text = preset.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = FontWeight.SemiBold
                )

                preset.author?.let { author ->
                    Text(
                        text = "by $author",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    preset.style?.let { style ->
                        Text(
                            text = style,
                            style = MaterialTheme.typography.labelSmall,
                            color = AccentPrimary
                        )
                    }
                    preset.scene?.let { scene ->
                        Text(
                            text = scene,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.TrendingUp,
                            contentDescription = "热度",
                            modifier = Modifier.size(12.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${preset.popularity}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = "收藏",
                            modifier = Modifier.size(12.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${preset.favoriteCount}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (isSelectionMode) {
                Surface(
                    shape = CircleShape,
                    color = if (isSelected) AccentPrimary else MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Box(
                        modifier = Modifier.size(36.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "已选择",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            } else {
                IconButton(onClick = { }) {
                    Icon(
                        imageVector = if (preset.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "收藏",
                        tint = if (preset.isFavorite) OppoSunriseGold else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun BatchOperationBar(
    selectedCount: Int,
    onApply: () -> Unit,
    onCancel: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "已选择 $selectedCount 个预设",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "点击批量应用将这些预设应用到您的照片",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onCancel,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) {
                    Text("取消")
                }

                Button(
                    onClick = onApply,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AccentPrimary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("批量应用")
                }
            }
        }
    }
}

@Composable
fun EmptyState() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ) {
            Icon(
                imageVector = Icons.Default.SearchOff,
                contentDescription = null,
                modifier = Modifier
                    .size(80.dp)
                    .padding(16.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "未找到匹配的预设",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "尝试调整搜索条件或筛选选项",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            textAlign = TextAlign.Center
        )
    }
}

private fun generateMockPresets(): List<Preset> {
    val styles = listOf("胶片", "复古", "清新", "人像", "风光", "美食", "夜景")
    val scenes = listOf("室内", "户外", "旅行", "日常", "聚会")
    val authors = listOf("摄影师小王", "影像达人", "OPPO官方", "摄影爱好者", "专业摄影师")
    val devices = listOf("OPPO Find X7", "OPPO Find X6", "一加 12", "realme GT5 Pro")

    return (1..50).map { index ->
        Preset(
            id = index,
            name = "预设 ${index}",
            author = authors.random(),
            style = styles.random(),
            scene = scenes.random(),
            deviceModel = devices.random(),
            coverPath = "preset$index",
            popularity = (100..5000).random(),
            favoriteCount = (10..500).random(),
            hasNewTag = index <= 5,
            cameraParams = if ((0..1).random() == 1) {
                Preset.CameraParams(
                    iso = (100..3200).random(),
                    wb = listOf("5000K", "5500K", "6000K", "6500K").random(),
                    hasselblad_hncs = (0..10).random() <= 3
                )
            } else null,
            isFavorite = (0..10).random() <= 3,
            createdAt = System.currentTimeMillis() - (index * 86400000L)
        )
    }
}

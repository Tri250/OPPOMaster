package com.omaster.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.*
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.omaster.app.model.Preset
import com.omaster.app.ui.animation.*
import com.omaster.app.ui.theme.*
import com.omaster.app.viewmodel.FilterType
import com.omaster.app.viewmodel.MainViewModel

// ==================== 预设分类枚举 ====================
enum class PresetCategory(val displayName: String) {
    ALL("全部"),
    HASSELBLAD("哈苏"),
    PORTRAIT("人像"),
    LANDSCAPE("风景"),
    STREET("街拍"),
    FOOD("美食"),
    NIGHT("夜景")
}

@OptIn(ExperimentalAnimationApi::class)
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
    var isScrolled by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()
    
    // 检测滚动位置
    LaunchedEffect(scrollState.value) {
        isScrolled = scrollState.value > 20
    }

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
            OppoHomeTopBar(
                isScrolled = isScrolled,
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
            // 分类标签
            CategoryTabs(
                selectedCategory = selectedCategory,
                onCategorySelected = { selectedCategory = it }
            )
            
            // 筛选标签
            FilterChipsRow(
                selectedFilter = filterType,
                onFilterSelected = { viewModel.onFilterTypeChanged(it) }
            )
            
            if (filteredPresets.isEmpty()) {
                OppoEmptyState(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                )
            } else {
                OppoPresetGrid(
                    presets = filteredPresets,
                    onPresetClick = onPresetClick,
                    onFavoriteToggle = { viewModel.toggleFavorite(it) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

// ==================== OPPO 2026 首页顶部栏 ====================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OppoHomeTopBar(
    isScrolled: Boolean,
    onSettingsClick: () -> Unit,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onClearSearch: () -> Unit
) {
    val elevation by animateDpAsState(
        targetValue = if (isScrolled) 2.dp else 0.dp,
        animationSpec = tween(
            durationMillis = OppoAnimationDuration.Fast,
            easing = OppoEasing.Standard
        ),
        label = "topbar_elevation"
    )
    
    val containerColor by animateColorAsState(
        targetValue = if (isScrolled) {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f)
        } else {
            MaterialTheme.colorScheme.background
        },
        animationSpec = tween(
            durationMillis = OppoAnimationDuration.Fast,
            easing = OppoEasing.Standard
        ),
        label = "topbar_color"
    )
    
    Surface(
        color = containerColor,
        tonalElevation = elevation,
        shadowElevation = if (isScrolled) 1.dp else 0.dp
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
                Column {
                    Text(
                        text = "小O帮帮",
                        style = MaterialTheme.typography.headlineMedium,
                        color = OppoSunriseGold,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "哈苏影像系统级标定基座",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OppoIconButton(
                        onClick = { },
                        icon = Icons.Default.QrCodeScanner
                    )

                    OppoIconButton(
                        onClick = onSettingsClick,
                        icon = Icons.Default.Settings
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            OppoSearchBar(
                query = searchQuery,
                onQueryChange = onSearchQueryChange,
                onClearQuery = onClearSearch,
                onSearch = { }
            )
        }
    }
}

// ==================== OPPO 2026 图标按钮 ====================
@Composable
fun OppoIconButton(
    onClick: () -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.9f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "icon_btn_scale"
    )
    
    val background by animateColorAsState(
        targetValue = if (isPressed) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        },
        animationSpec = tween(
            durationMillis = OppoAnimationDuration.Micro,
            easing = OppoEasing.Standard
        ),
        label = "icon_btn_bg"
    )
    
    Surface(
        onClick = onClick,
        modifier = modifier
            .size(44.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        shape = CircleShape,
        color = background
    ) {
        Box(
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

// ==================== OPPO 2026 搜索栏 ====================
@Composable
fun OppoSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClearQuery: () -> Unit,
    onSearch: () -> Unit
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
                            text = "搜索预设名称、机型...",
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

// ==================== OPPO 2026 分类标签 ====================
@Composable
fun CategoryTabs(
    selectedCategory: PresetCategory,
    onCategorySelected: (PresetCategory) -> Unit
) {
    var scrollState = rememberScrollState()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 10.dp)
            .horizontalScroll(scrollState),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        PresetCategory.values().forEach { category ->
            OppoFilterChip(
                selected = selectedCategory == category,
                onClick = { onCategorySelected(category) },
                label = category.displayName,
                isPrimary = category == PresetCategory.HASSELBLAD
            )
        }
    }
}

// ==================== OPPO 2026 筛选标签 ====================
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
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        filters.forEach { (filter, label) ->
            OppoFilterChip(
                selected = selectedFilter == filter,
                onClick = { onFilterSelected(filter) },
                label = label,
                isPrimary = filter == FilterType.HASSELBLAD,
                leadingIcon = if (filter == FilterType.FAVORITES) {
                    {
                        Icon(
                            imageVector = if (selectedFilter == FilterType.FAVORITES) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                } else null
            )
        }
    }
}

// ==================== OPPO 2026 筛选Chip ====================
@Composable
fun OppoFilterChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: String,
    isPrimary: Boolean = false,
    leadingIcon: @Composable (() -> Unit)? = null
) {
    val selectedColor = if (isPrimary) HasselbladOrangePro else OppoSunriseGold
    
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
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            leadingIcon?.let {
                CompositionLocalProvider(
                    androidx.compose.ui.LocalContentColor provides contentColor
                ) {
                    it()
                }
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = contentColor,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
            )
        }
    }
}

// ==================== OPPO 2026 预设网格 ====================
@Composable
fun OppoPresetGrid(
    presets: List<Preset>,
    onPresetClick: (Preset) -> Unit,
    onFavoriteToggle: (Preset) -> Unit,
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
            OppoPresetCard(
                preset = preset,
                onClick = { onPresetClick(preset) },
                onFavoriteToggle = { onFavoriteToggle(preset) }
            )
        }
    }
}

// ==================== OPPO 2026 预设卡片 ====================
@Composable
fun OppoPresetCard(
    preset: Preset,
    onClick: () -> Unit,
    onFavoriteToggle: () -> Unit,
    modifier: Modifier = Modifier
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

    Card(
        onClick = onClick,
        interactionSource = interactionSource,
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(0.78f)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = elevation)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // 图片
            AsyncImage(
                model = "https://picsum.photos/seed/${preset.coverPath}/400/533",
                contentDescription = preset.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // 渐变遮罩
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

            // HNCS标签
            if (preset.cameraParams?.hasselblad_hncs == true) {
                OppoHncsBadge(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(10.dp)
                )
            }

            // 收藏按钮
            OppoFavoriteButton(
                isFavorite = preset.isFavorite,
                onToggle = onFavoriteToggle,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
            )

            // 信息区域
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

// ==================== OPPO 2026 HNCS徽章 ====================
@Composable
fun OppoHncsBadge(
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
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

// ==================== OPPO 2026 收藏按钮 ====================
@Composable
fun OppoFavoriteButton(
    isFavorite: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scale by animateFloatAsState(
        targetValue = if (isFavorite) 1.15f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "favorite_scale"
    )
    
    val iconTint by animateColorAsState(
        targetValue = if (isFavorite) {
            OppoSunriseGold
        } else {
            Color.White
        },
        animationSpec = tween(
            durationMillis = OppoAnimationDuration.Fast,
            easing = OppoEasing.Standard
        ),
        label = "favorite_tint"
    )
    
    val background by animateColorAsState(
        targetValue = if (isFavorite) {
            Color.White.copy(alpha = 0.95f)
        } else {
            Color.Black.copy(alpha = 0.35f)
        },
        animationSpec = tween(
            durationMillis = OppoAnimationDuration.Fast,
            easing = OppoEasing.Standard
        ),
        label = "favorite_bg"
    )
    
    Surface(
        onClick = onToggle,
        modifier = modifier
            .size(36.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        shape = CircleShape,
        color = background
    ) {
        Box(
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                contentDescription = if (isFavorite) "取消收藏" else "收藏",
                tint = iconTint,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

// ==================== OPPO 2026 空状态 ====================
@Composable
fun OppoEmptyState(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ) {
            Icon(
                imageVector = Icons.Default.PhotoCamera,
                contentDescription = null,
                modifier = Modifier
                    .size(80.dp)
                    .padding(16.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "暂无预设",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "尝试其他搜索或筛选条件",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            textAlign = TextAlign.Center
        )
    }
}

package com.omaster.app.presentation.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.omaster.app.data.PresetTagSystem
import com.omaster.app.domain.model.Preset
import kotlinx.coroutines.launch

/**
 * 标签筛选页面 - 支持标签云展示、多选标签筛选、筛选结果展示
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TagFilterScreen(
    presets: List<Preset>,
    tagSystem: PresetTagSystem,
    onPresetClick: (Preset) -> Unit,
    onBackClick: () -> Unit,
    onClearFilters: () -> Unit = {}
) {
    var selectedTags by remember { mutableStateOf<Set<String>>(emptySet()) }
    var selectedCategory by remember { mutableStateOf<PresetTagSystem.TagCategory?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var filteredPresets by remember { mutableStateOf(presets) }
    var isFilterActive by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    // 执行筛选
    LaunchedEffect(selectedTags, selectedCategory) {
        if (selectedTags.isEmpty() && selectedCategory == null) {
            filteredPresets = presets
            isFilterActive = false
        } else {
            isFilterActive = true
            val result = tagSystem.filterPresetsByTags(
                presets = presets,
                selectedTags = selectedTags.toList(),
                matchMode = PresetTagSystem.TagMatchMode.ANY
            )
            filteredPresets = result.presets
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "标签筛选",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "返回",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    if (isFilterActive) {
                        TextButton(
                            onClick = {
                                selectedTags = emptySet()
                                selectedCategory = null
                                onClearFilters()
                            }
                        ) {
                            Text(
                                text = "清除筛选",
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF1A1A2E)
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color(0xFF1A1A2E))
        ) {
            // 搜索栏
            SearchBar(
                query = searchQuery,
                onQueryChange = { searchQuery = it },
                onSearch = {
                    scope.launch {
                        val searchResults = tagSystem.searchTags(searchQuery)
                        if (searchResults.isNotEmpty()) {
                            selectedTags = selectedTags + searchResults.map { it.id }.toSet()
                        }
                    }
                }
            )

            // 已选标签展示
            if (selectedTags.isNotEmpty()) {
                SelectedTagsRow(
                    selectedTags = selectedTags,
                    tagSystem = tagSystem,
                    onTagRemove = { tagId ->
                        selectedTags = selectedTags - tagId
                    }
                )
            }

            // 分类标签页
            CategoryTabs(
                selectedCategory = selectedCategory,
                onCategorySelected = { category ->
                    selectedCategory = if (selectedCategory == category) null else category
                },
                tagSystem = tagSystem
            )

            // 标签云
            TagCloudSection(
                tagSystem = tagSystem,
                selectedCategory = selectedCategory,
                selectedTags = selectedTags,
                onTagClick = { tagId ->
                    selectedTags = if (tagId in selectedTags) {
                        selectedTags - tagId
                    } else {
                        selectedTags + tagId
                    }
                }
            )

            // 筛选结果统计
            FilterResultHeader(
                resultCount = filteredPresets.size,
                isFilterActive = isFilterActive
            )

            // 筛选结果列表
            FilteredPresetsGrid(
                presets = filteredPresets,
                onPresetClick = onPresetClick
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        placeholder = {
            Text(
                text = "搜索标签...",
                color = Color.Gray
            )
        },
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "搜索",
                tint = Color.Gray
            )
        },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = "清除",
                        tint = Color.Gray
                    )
                }
            }
        },
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        colors = TextFieldDefaults.outlinedTextFieldColors(
            containerColor = Color(0xFF2D2D44),
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = Color.Transparent,
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White
        ),
        keyboardActions = androidx.compose.foundation.text.KeyboardActions(
            onSearch = { onSearch() }
        )
    )
}

@Composable
private fun SelectedTagsRow(
    selectedTags: Set<String>,
    tagSystem: PresetTagSystem,
    onTagRemove: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Text(
            text = "已选标签 (${selectedTags.size})",
            fontSize = 14.sp,
            color = Color.Gray,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 4.dp)
        ) {
            items(selectedTags.toList()) { tagId ->
                val tag = tagSystem.getTagById(tagId)
                if (tag != null) {
                    SelectedTagChip(
                        tag = tag,
                        onRemove = { onTagRemove(tagId) }
                    )
                }
            }
        }
    }
}

@Composable
private fun SelectedTagChip(
    tag: PresetTagSystem.Tag,
    onRemove: () -> Unit
) {
    val tagColor = Color(android.graphics.Color.parseColor(tag.color))

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = tagColor.copy(alpha = 0.2f),
        border = androidx.compose.foundation.BorderStroke(1.dp, tagColor)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = tag.icon ?: "",
                fontSize = 14.sp,
                modifier = Modifier.padding(end = 4.dp)
            )
            Text(
                text = tag.name,
                fontSize = 14.sp,
                color = Color.White,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "移除",
                modifier = Modifier
                    .size(16.dp)
                    .clickable(onClick = onRemove),
                tint = Color.White.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun CategoryTabs(
    selectedCategory: PresetTagSystem.TagCategory?,
    onCategorySelected: (PresetTagSystem.TagCategory) -> Unit,
    tagSystem: PresetTagSystem
) {
    val categories = tagSystem.getAllCategories()

    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(categories) { category ->
            val isSelected = selectedCategory == category
            val displayName = tagSystem.getCategoryDisplayName(category)

            CategoryTab(
                name = displayName,
                isSelected = isSelected,
                onClick = { onCategorySelected(category) }
            )
        }
    }
}

@Composable
private fun CategoryTab(
    name: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = if (isSelected) {
        MaterialTheme.colorScheme.primary
    } else {
        Color(0xFF2D2D44)
    }

    val textColor = if (isSelected) {
        Color.Black
    } else {
        Color.White
    }

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = backgroundColor,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Text(
            text = name,
            fontSize = 14.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = textColor,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
    }
}

@Composable
private fun TagCloudSection(
    tagSystem: PresetTagSystem,
    selectedCategory: PresetTagSystem.TagCategory?,
    selectedTags: Set<String>,
    onTagClick: (String) -> Unit
) {
    val tags = if (selectedCategory != null) {
        tagSystem.getTagsByCategory(selectedCategory)
    } else {
        tagSystem.getAllTags()
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(
            text = if (selectedCategory != null) {
                "${tagSystem.getCategoryDisplayName(selectedCategory)}标签"
            } else {
                "全部标签"
            },
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // 标签云布局
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            tags.forEach { tag ->
                val isSelected = tag.id in selectedTags
                TagCloudItem(
                    tag = tag,
                    isSelected = isSelected,
                    onClick = { onTagClick(tag.id) }
                )
            }
        }
    }
}

@Composable
private fun TagCloudItem(
    tag: PresetTagSystem.Tag,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val tagColor = Color(android.graphics.Color.parseColor(tag.color))

    val backgroundColor = if (isSelected) {
        tagColor
    } else {
        tagColor.copy(alpha = 0.15f)
    }

    val textColor = if (isSelected) {
        Color.Black
    } else {
        Color.White
    }

    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.05f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "tag_scale"
    )

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = backgroundColor,
        modifier = Modifier
            .scale(scale)
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (tag.icon != null) {
                Text(
                    text = tag.icon,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(end = 4.dp)
                )
            }
            Text(
                text = tag.name,
                fontSize = 14.sp,
                color = textColor,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}

@Composable
private fun FilterResultHeader(
    resultCount: Int,
    isFilterActive: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = if (isFilterActive) {
                "筛选结果: $resultCount 个预设"
            } else {
                "全部预设: $resultCount 个"
            },
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = Color.White
        )

        if (isFilterActive) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
            ) {
                Text(
                    text = "已筛选",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun FilteredPresetsGrid(
    presets: List<Preset>,
    onPresetClick: (Preset) -> Unit
) {
    if (presets.isEmpty()) {
        EmptyFilterResult()
    } else {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            items(presets) { preset ->
                FilteredPresetCard(
                    preset = preset,
                    onClick = { onPresetClick(preset) }
                )
            }
        }
    }
}

@Composable
private fun FilteredPresetCard(
    preset: Preset,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF2D2D44)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            // 封面图
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(preset.coverUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = preset.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                // 标签数量标识
                if (preset.tags.isNotEmpty()) {
                    Surface(
                        modifier = Modifier
                            .padding(8.dp)
                            .align(Alignment.TopEnd),
                        shape = RoundedCornerShape(8.dp),
                        color = Color.Black.copy(alpha = 0.6f)
                    ) {
                        Text(
                            text = "${preset.tags.size} 标签",
                            fontSize = 10.sp,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            // 预设信息
            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                Text(
                    text = preset.name,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = preset.deviceModel ?: "通用设备",
                    fontSize = 12.sp,
                    color = Color.Gray,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // 显示部分标签
                if (preset.tags.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(preset.tags.take(3)) { tagName ->
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                            ) {
                                Text(
                                    text = tagName,
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyFilterResult() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.SearchOff,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = Color.Gray
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "没有找到匹配的预设",
            fontSize = 16.sp,
            color = Color.Gray,
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "尝试调整筛选条件或选择其他标签",
            fontSize = 14.sp,
            color = Color.Gray.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )
    }
}

/**
 * 流式布局实现
 */
@Composable
private fun FlowRow(
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    content: @Composable () -> Unit
) {
    Layout(
        content = content,
        modifier = modifier
    ) { measurables, constraints ->
        val hGapPx = 8.dp.roundToPx()
        val vGapPx = 8.dp.roundToPx()

        val rows = mutableListOf<List<androidx.compose.ui.layout.Placeable>>()
        val rowWidths = mutableListOf<Int>()
        val rowHeights = mutableListOf<Int>()

        var currentRow = mutableListOf<androidx.compose.ui.layout.Placeable>()
        var currentRowWidth = 0
        var currentRowHeight = 0

        measurables.forEach { measurable ->
            val placeable = measurable.measure(constraints)

            if (currentRow.isNotEmpty() &&
                currentRowWidth + hGapPx + placeable.width > constraints.maxWidth
            ) {
                rows.add(currentRow)
                rowWidths.add(currentRowWidth)
                rowHeights.add(currentRowHeight)
                currentRow = mutableListOf()
                currentRowWidth = 0
                currentRowHeight = 0
            }

            currentRow.add(placeable)
            currentRowWidth += if (currentRow.size == 1) placeable.width else hGapPx + placeable.width
            currentRowHeight = maxOf(currentRowHeight, placeable.height)
        }

        if (currentRow.isNotEmpty()) {
            rows.add(currentRow)
            rowWidths.add(currentRowWidth)
            rowHeights.add(currentRowHeight)
        }

        val width = rowWidths.maxOrNull()?.coerceIn(constraints.minWidth, constraints.maxWidth)
            ?: constraints.minWidth
        val height = rowHeights.sum() + (rowHeights.size - 1).coerceAtLeast(0) * vGapPx

        layout(width, height) {
            var y = 0
            rows.forEachIndexed { rowIndex, row ->
                var x = when (horizontalArrangement) {
                    Arrangement.Start -> 0
                    Arrangement.End -> width - rowWidths[rowIndex]
                    Arrangement.Center -> (width - rowWidths[rowIndex]) / 2
                    else -> 0
                }

                row.forEach { placeable ->
                    placeable.placeRelative(x, y)
                    x += placeable.width + hGapPx
                }

                y += rowHeights[rowIndex] + vGapPx
            }
        }
    }
}

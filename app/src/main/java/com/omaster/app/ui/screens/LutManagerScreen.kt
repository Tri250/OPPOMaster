package com.omaster.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.omaster.app.ui.theme.*

enum class PresetFormat(val extension: String, val displayName: String, val color: Color, val description: String, val compatibleApps: List<String>) {
    CUBE(".cube", "3D LUT 标准格式", HasselbladOrangePro, "业界通用的3D LUT文件格式", listOf("DaVinci Resolve", "Adobe Lightroom", "Capture One", "Final Cut Pro")),
    JSON(".json", "小O帮帮自定义格式", OppoSunriseGold, "小O帮帮自研的高动态预设格式", listOf("小O帮帮", "ColorOS 系统相机")),
    XMP(".xmp", "Adobe XMP 预设", Color(0xFF31A8FF), "Adobe Lightroom/Camera RAW预设格式", listOf("Adobe Lightroom", "Adobe Photoshop", "Adobe Camera Raw")),
    LRTEMPLATE(".lrtemplate", "Lightroom 旧版预设", Color(0xFF9999FF), "Lightroom Classic 经典预设格式", listOf("Lightroom Classic", "Lightroom CC")),
    DNG(".dng", "DNG 配置文件", Color(0xFF00D4AA), "Adobe DNG处理配置文件", listOf("Adobe DNG Converter", "Lightroom", "Photoshop"))
}

data class LutPreset(
    val id: String,
    val name: String,
    val category: String,
    val format: PresetFormat,
    val author: String,
    val size: String,
    val date: String,
    val isSelected: Boolean = false
)

enum class ViewMode {
    GRID, LIST
}

@Composable
fun LutManagerScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var viewMode by remember { mutableStateOf(ViewMode.GRID) }
    var selectedPresets by remember { mutableStateOf(setOf<String>()) }
    var isSelectionMode by remember { mutableStateOf(false) }

    val presets = remember {
        listOf(
            LutPreset("1", "Fuji_Pro_400H", "胶片风格", PresetFormat.CUBE, "Hasselblad Team", "2.3 MB", "2024-01-15"),
            LutPreset("2", "Leica_M_Mono", "黑白风格", PresetFormat.CUBE, "Leica Official", "1.8 MB", "2024-02-20"),
            LutPreset("3", "Cinematic_Tone", "电影感", PresetFormat.JSON, "小O帮帮", "856 KB", "2024-03-10"),
            LutPreset("4", "Vintage_Film", "复古风格", PresetFormat.XMP, "Adobe", "1.2 MB", "2024-04-05"),
            LutPreset("5", "Portraits_Soft", "人像风格", PresetFormat.LRTEMPLATE, "Lightroom", "925 KB", "2024-05-18"),
            LutPreset("6", "Night_City", "夜景风格", PresetFormat.CUBE, "Night Lab", "2.8 MB", "2024-06-22")
        )
    }

    val filteredPresets = remember(presets, searchQuery) {
        if (searchQuery.isEmpty()) presets
        else presets.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
            it.category.contains(searchQuery, ignoreCase = true) ||
            it.author.contains(searchQuery, ignoreCase = true)
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            LutManagerTopBar(onBack = onBack)
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            StatsCardsRow()
            SearchAndActionsRow(
                searchQuery = searchQuery,
                onSearchQueryChange = { searchQuery = it },
                viewMode = viewMode,
                onViewModeChange = { viewMode = it },
                onImportClick = { }
            )

            AnimatedVisibility(
                visible = isSelectionMode && selectedPresets.isNotEmpty(),
                enter = slideInVertically() + fadeIn(),
                exit = slideOutVertically() + fadeOut()
            ) {
                BatchOperationsBar(
                    selectedCount = selectedPresets.size,
                    onExportClick = { },
                    onDeleteClick = { },
                    onCancelClick = {
                        selectedPresets = emptySet()
                        isSelectionMode = false
                    }
                )
            }

            if (viewMode == ViewMode.GRID) {
                PresetGridView(
                    presets = filteredPresets,
                    selectedPresets = selectedPresets,
                    isSelectionMode = isSelectionMode,
                    onPresetSelect = { preset ->
                        if (isSelectionMode) {
                            selectedPresets = if (selectedPresets.contains(preset.id)) {
                                selectedPresets - preset.id
                            } else {
                                selectedPresets + preset.id
                            }
                            if (selectedPresets.isEmpty()) {
                                isSelectionMode = false
                            }
                        }
                    },
                    onPresetLongClick = { preset ->
                        isSelectionMode = true
                        selectedPresets = selectedPresets + preset.id
                    },
                    modifier = Modifier.weight(1f)
                )
            } else {
                PresetListView(
                    presets = filteredPresets,
                    selectedPresets = selectedPresets,
                    isSelectionMode = isSelectionMode,
                    onPresetSelect = { preset ->
                        if (isSelectionMode) {
                            selectedPresets = if (selectedPresets.contains(preset.id)) {
                                selectedPresets - preset.id
                            } else {
                                selectedPresets + preset.id
                            }
                            if (selectedPresets.isEmpty()) {
                                isSelectionMode = false
                            }
                        }
                    },
                    onPresetLongClick = { preset ->
                        isSelectionMode = true
                        selectedPresets = selectedPresets + preset.id
                    },
                    modifier = Modifier.weight(1f)
                )
            }

            SupportedFormatsCard()
            UsageTipsCard()
        }
    }
}

@Composable
fun LutManagerTopBar(onBack: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OppoIconButton(
                    onClick = onBack,
                    icon = Icons.Default.ArrowBack
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Storage,
                            contentDescription = null,
                            tint = HasselbladOrangePro,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "预设管理",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "支持多种格式导入导出，打通主流修图工具的预设生态",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun StatsCardsRow() {
    val stats = listOf(
        Triple("6", "预设总数", Icons.Default.Folder),
        Triple("4", "CUBE 格式", Icons.Default.ViewModule),
        Triple("1", "XMP 格式", Icons.Default.Description),
        Triple("5", "支持小O帮帮", Icons.Default.CheckCircle),
        Triple("12.5 MB", "总大小", Icons.Default.Storage)
    )

    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 4.dp)
    ) {
        items(stats) { (value, label, icon) ->
            StatCard(value = value, label = label, icon = icon)
        }
    }
}

@Composable
fun StatCard(value: String, label: String, icon: ImageVector) {
    val backgroundColor = MaterialTheme.colorScheme.surfaceVariant

    Surface(
        modifier = Modifier.width(100.dp),
        shape = RoundedCornerShape(12.dp),
        color = backgroundColor
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = HasselbladOrangePro,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun SearchAndActionsRow(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    viewMode: ViewMode,
    onViewModeChange: (ViewMode) -> Unit,
    onImportClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        LutSearchBar(
            query = searchQuery,
            onQueryChange = onSearchQueryChange,
            modifier = Modifier.weight(1f)
        )
        Spacer(modifier = Modifier.width(8.dp))
        ViewModeToggle(
            currentMode = viewMode,
            onModeChange = onViewModeChange
        )
        Spacer(modifier = Modifier.width(8.dp))
        ImportButton(onClick = onImportClick)
    }
}

@Composable
fun LutSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier.weight(1f),
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurface
                ),
                decorationBox = { innerTextField ->
                    if (query.isEmpty()) {
                        Text(
                            text = "搜索名称、分类、作者...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                    innerTextField()
                }
            )
        }
    }
}

@Composable
fun ViewModeToggle(
    currentMode: ViewMode,
    onModeChange: (ViewMode) -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Surface(
            onClick = { onModeChange(ViewMode.GRID) },
            shape = RoundedCornerShape(8.dp),
            color = if (currentMode == ViewMode.GRID) {
                HasselbladOrangePro.copy(alpha = 0.15f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        ) {
            Icon(
                imageVector = Icons.Default.GridView,
                contentDescription = "网格视图",
                modifier = Modifier.padding(8.dp),
                tint = if (currentMode == ViewMode.GRID) {
                    HasselbladOrangePro
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }
        Surface(
            onClick = { onModeChange(ViewMode.LIST) },
            shape = RoundedCornerShape(8.dp),
            color = if (currentMode == ViewMode.LIST) {
                HasselbladOrangePro.copy(alpha = 0.15f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        ) {
            Icon(
                imageVector = Icons.Default.ViewList,
                contentDescription = "列表视图",
                modifier = Modifier.padding(8.dp),
                tint = if (currentMode == ViewMode.LIST) {
                    HasselbladOrangePro
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }
    }
}

@Composable
fun ImportButton(onClick: () -> Unit) {
    val isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "import_scale"
    )

    Surface(
        onClick = onClick,
        modifier = Modifier.graphicsLayer {
            scaleX = scale
            scaleY = scale
        },
        shape = RoundedCornerShape(12.dp),
        color = HasselbladOrangePro
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.FileUpload,
                contentDescription = null,
                tint = OppoDeepSpace,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "导入",
                style = MaterialTheme.typography.labelLarge,
                color = OppoDeepSpace,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun BatchOperationsBar(
    selectedCount: Int,
    onExportClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onCancelClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = HasselbladOrangePro.copy(alpha = 0.1f),
        border = BorderStroke(1.dp, HasselbladOrangePro.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "已选择 $selectedCount 项",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onExportClick,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = HasselbladOrangePro
                    )
                ) {
                    Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("导出")
                }
                OutlinedButton(
                    onClick = onDeleteClick,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFFFF6B6B)
                    )
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("删除")
                }
                TextButton(onClick = onCancelClick) {
                    Text("取消")
                }
            }
        }
    }
}

@Composable
fun PresetGridView(
    presets: List<LutPreset>,
    selectedPresets: Set<String>,
    isSelectionMode: Boolean,
    onPresetSelect: (LutPreset) -> Unit,
    onPresetLongClick: (LutPreset) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(presets, key = { it.id }) { preset ->
            PresetGridCard(
                preset = preset,
                isSelected = selectedPresets.contains(preset.id),
                isSelectionMode = isSelectionMode,
                onClick = { onPresetSelect(preset) },
                onLongClick = { onPresetLongClick(preset) }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PresetGridCard(
    preset: LutPreset,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 0.97f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "grid_card_scale"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.85f)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = preset.format.color.copy(alpha = 0.15f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = preset.format.color,
                            modifier = Modifier.padding(8.dp)
                        )
                    }

                    if (isSelectionMode) {
                        Surface(
                            shape = CircleShape,
                            color = if (isSelected) {
                                HasselbladOrangePro
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            }
                        ) {
                            Icon(
                                imageVector = if (isSelected) Icons.Default.Check else Icons.Default.Circle,
                                contentDescription = null,
                                tint = if (isSelected) OppoDeepSpace else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .size(24.dp)
                                    .padding(2.dp)
                            )
                        }
                    }
                }

                Column {
                    Text(
                        text = preset.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FormatChip(format = preset.format)
                        CategoryChip(category = preset.category)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = preset.author,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = preset.size,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = preset.date,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PresetListView(
    presets: List<LutPreset>,
    selectedPresets: Set<String>,
    isSelectionMode: Boolean,
    onPresetSelect: (LutPreset) -> Unit,
    onPresetLongClick: (LutPreset) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(presets, key = { it.id }) { preset ->
            PresetListCard(
                preset = preset,
                isSelected = selectedPresets.contains(preset.id),
                isSelectionMode = isSelectionMode,
                onClick = { onPresetSelect(preset) },
                onLongClick = { onPresetLongClick(preset) }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PresetListCard(
    preset: LutPreset,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) {
            HasselbladOrangePro.copy(alpha = 0.08f)
        } else {
            MaterialTheme.colorScheme.surface
        },
        animationSpec = tween(durationMillis = 200),
        label = "list_card_bg"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isSelectionMode) {
                Surface(
                    shape = CircleShape,
                    color = if (isSelected) {
                        HasselbladOrangePro
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    }
                ) {
                    Icon(
                        imageVector = if (isSelected) Icons.Default.Check else Icons.Default.Circle,
                        contentDescription = null,
                        tint = if (isSelected) OppoDeepSpace else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .size(22.dp)
                            .padding(2.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
            }

            Surface(
                shape = RoundedCornerShape(10.dp),
                color = preset.format.color.copy(alpha = 0.15f)
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = preset.format.color,
                    modifier = Modifier.padding(10.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = preset.name,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FormatChip(format = preset.format, small = true)
                    CategoryChip(category = preset.category, small = true)
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = preset.author,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${preset.size} • ${preset.date}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun FormatChip(format: PresetFormat, small: Boolean = false) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = format.color.copy(alpha = 0.15f)
    ) {
        Text(
            text = format.extension,
            modifier = Modifier.padding(
                horizontal = if (small) 6.dp else 8.dp,
                vertical = if (small) 2.dp else 4.dp
            ),
            style = if (small) MaterialTheme.typography.labelSmall else MaterialTheme.typography.labelMedium,
            color = format.color,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun CategoryChip(category: String, small: Boolean = false) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Text(
            text = category,
            modifier = Modifier.padding(
                horizontal = if (small) 6.dp else 8.dp,
                vertical = if (small) 2.dp else 4.dp
            ),
            style = if (small) MaterialTheme.typography.labelSmall else MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun SupportedFormatsCard() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = HasselbladOrangePro,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "支持的格式",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            PresetFormat.values().forEach { format ->
                FormatInfoItem(format = format)
                if (format != PresetFormat.values().last()) {
                    Spacer(modifier = Modifier.height(10.dp))
                }
            }
        }
    }
}

@Composable
fun FormatInfoItem(format: PresetFormat) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = format.color.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = format.extension,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = format.color,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = format.displayName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Medium
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = format.description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            format.compatibleApps.take(3).forEach { app ->
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Text(
                        text = app,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun UsageTipsCard() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = HasselbladOrangePro.copy(alpha = 0.08f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Lightbulb,
                    contentDescription = null,
                    tint = HasselbladOrangePro,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "使用提示",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            val tips = listOf(
                "支持批量导入多个预设文件",
                "小O帮帮会自动识别并转换不同格式",
                "可以通过二维码或链接快速分享",
                "支持批量导出为多种格式",
                "建议定期备份预设"
            )
            tips.forEach { tip ->
                Row(
                    modifier = Modifier.padding(vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = OppoSunriseGold,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = tip,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

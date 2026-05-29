package com.omaster.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Sort
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.omaster.app.model.Preset
import com.omaster.app.ui.theme.*
import com.omaster.app.ui.animation.*
import android.widget.Toast
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context

enum class FilterStyle(val displayName: String, val color: Color) {
    STANDARD("标准", Color(0xFF8E8E93)),
    VIVID("明艳", Color(0xFFFF6B35)),
    VINTAGE("复古", Color(0xFFD4A574)),
    FILM("胶片", Color(0xFFB89A5C)),
    FRESH("清新", Color(0xFF90EE90)),
    TRANSPARENT("通透", Color(0xFF87CEEB)),
    BLACK_WHITE("黑白", Color(0xFF404040)),
    FAIRY("童话", Color(0xFFFFB6C1)),
    DREAM("梦幻", Color(0xFFDDA0DD)),
    COOL("冷调", Color(0xFF6B98D4)),
    WARM("暖调", Color(0xFFFFB347))
}

data class SavedPreset(
    val id: String,
    val name: String,
    val filterStyle: FilterStyle,
    val createdAt: Long
)

data class RankedPreset(
    val rank: Int,
    val name: String,
    val author: String,
    val downloads: Int,
    val rating: Float,
    val tags: List<String>,
    val filterStyle: FilterStyle
)

enum class SortField {
    NAME, CREATED_AT
}

enum class SortOrder {
    ASCENDING, DESCENDING
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PresetEditorScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedFilterStyle by remember { mutableStateOf(FilterStyle.STANDARD) }
    var filterIntensity by remember { mutableFloatStateOf(70f) }
    var saturation by remember { mutableFloatStateOf(0f) }
    var contrast by remember { mutableFloatStateOf(0f) }
    var brightness by remember { mutableFloatStateOf(0f) }
    var colorTemperature by remember { mutableFloatStateOf(0f) }
    var vignetteEnabled by remember { mutableStateOf(false) }
    var presetName by remember { mutableStateOf("") }
    var showSaveDialog by remember { mutableStateOf(false) }
    var showContributeDialog by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showSortDialog by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }
    var editingPreset by remember { mutableStateOf<SavedPreset?>(null) }
    var newPresetName by remember { mutableStateOf("") }
    var sortField by remember { mutableStateOf(SortField.CREATED_AT) }
    var sortOrder by remember { mutableStateOf(SortOrder.DESCENDING) }
    var snackbarMessage by remember { mutableStateOf<String?>(null) }
    
    val savedPresets = remember {
        mutableStateListOf(
            SavedPreset("1", "我的复古预设", FilterStyle.VINTAGE, System.currentTimeMillis() - 86400000),
            SavedPreset("2", "风景大片", FilterStyle.FRESH, System.currentTimeMillis() - 172800000),
            SavedPreset("3", "人像美颜", FilterStyle.FAIRY, System.currentTimeMillis() - 259200000)
        )
    }

    val rankedPresets = remember {
        listOf(
            RankedPreset(1, "哈苏自然", "影像达人", 12890, 4.9f, listOf("风景", "人像"), FilterStyle.STANDARD),
            RankedPreset(2, "胶片时光", "胶片爱好者", 9834, 4.8f, listOf("复古", "街拍"), FilterStyle.FILM),
            RankedPreset(3, "梦幻人像", "人像摄影师", 8765, 4.7f, listOf("人像", "童话"), FilterStyle.FAIRY),
            RankedPreset(4, "冷峻都市", "街头摄影师", 7654, 4.6f, listOf("街拍", "冷调"), FilterStyle.COOL),
            RankedPreset(5, "暖阳午后", "生活方式博主", 6543, 4.5f, listOf("暖调", "清新"), FilterStyle.WARM)
        )
    }

    val tips = remember {
        listOf(
            "调整参数时观察左侧预览效果",
            "上传自己的作品作为预览底图",
            "为预设添加标签，方便分类管理",
            "好的预设可以一键贡献社区",
            "支持JSON格式导入导出"
        )
    }

    val sortedPresets = remember(savedPresets, sortField, sortOrder) {
        savedPresets.sortedWith(
            when (sortField) {
                SortField.NAME -> if (sortOrder == SortOrder.ASCENDING) 
                    compareBy { it.name } 
                else 
                    compareByDescending { it.name }
                SortField.CREATED_AT -> if (sortOrder == SortOrder.ASCENDING) 
                    compareBy { it.createdAt } 
                else 
                    compareByDescending { it.createdAt }
            }
        )
    }

    fun showSnackbar(message: String) {
        snackbarMessage = message
    }

    LaunchedEffect(snackbarMessage) {
        if (snackbarMessage != null) {
            kotlinx.coroutines.delay(3000)
            snackbarMessage = null
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                PresetEditorTopBar(
                    onBack = onBack,
                    presetName = presetName,
                    onPresetNameChange = { presetName = it }
                )
            },
            containerColor = MaterialTheme.colorScheme.background
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                PreviewPanel(
                    filterStyle = selectedFilterStyle,
                    filterIntensity = filterIntensity,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                )

                ParameterPanel(
                    selectedFilterStyle = selectedFilterStyle,
                    onFilterStyleSelected = { selectedFilterStyle = it },
                    filterIntensity = filterIntensity,
                    onFilterIntensityChange = { filterIntensity = it },
                    saturation = saturation,
                    onSaturationChange = { saturation = it },
                    contrast = contrast,
                    onContrastChange = { contrast = it },
                    brightness = brightness,
                    onBrightnessChange = { brightness = it },
                    colorTemperature = colorTemperature,
                    onColorTemperatureChange = { colorTemperature = it },
                    vignetteEnabled = vignetteEnabled,
                    onVignetteToggle = { vignetteEnabled = it },
                    onSavePreset = { showSaveDialog = true },
                    onContribute = { showContributeDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1.5f)
                )
            }
        }

        AnimatedVisibility(
            visible = snackbarMessage != null,
            enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 100.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = AccentPrimary.copy(alpha = 0.95f),
                shadowElevation = 8.dp
            ) {
                Text(
                    text = snackbarMessage ?: "",
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                    color = OppoDeepSpace,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }

    if (showSaveDialog) {
        SavePresetDialog(
            presetName = presetName,
            onPresetNameChange = { presetName = it },
            onConfirm = {
                if (presetName.isNotBlank()) {
                    savedPresets.add(
                        0,
                        SavedPreset(
                            id = System.currentTimeMillis().toString(),
                            name = presetName,
                            filterStyle = selectedFilterStyle,
                            createdAt = System.currentTimeMillis()
                        )
                    )
                    showSaveDialog = false
                    showSnackbar("预设保存成功")
                } else {
                    showSnackbar("请输入预设名称")
                }
            },
            onDismiss = { showSaveDialog = false }
        )
    }

    if (showContributeDialog) {
        ContributeDialog(
            onConfirm = { showContributeDialog = false },
            onDismiss = { showContributeDialog = false }
        )
    }

    if (showRenameDialog && editingPreset != null) {
        RenamePresetDialog(
            currentName = editingPreset!!.name,
            newName = newPresetName,
            onNewNameChange = { newPresetName = it },
            onConfirm = {
                if (newPresetName.isNotBlank()) {
                    val index = savedPresets.indexOfFirst { it.id == editingPreset!!.id }
                    if (index != -1) {
                        savedPresets[index] = editingPreset!!.copy(name = newPresetName)
                        showSnackbar("预设已重命名")
                    }
                    showRenameDialog = false
                    editingPreset = null
                    newPresetName = ""
                } else {
                    showSnackbar("请输入有效的名称")
                }
            },
            onDismiss = {
                showRenameDialog = false
                editingPreset = null
                newPresetName = ""
            }
        )
    }

    if (showSortDialog) {
        SortPresetDialog(
            currentSortField = sortField,
            currentSortOrder = sortOrder,
            onSortChange = { field, order ->
                sortField = field
                sortOrder = order
                showSnackbar("排序已更新")
            },
            onDismiss = { showSortDialog = false }
        )
    }

    if (showImportDialog) {
        ImportPresetDialog(
            onImport = { jsonContent ->
                try {
                    val imported = parsePresetsFromJson(jsonContent)
                    savedPresets.addAll(imported.mapIndexed { index, preset ->
                        preset.copy(id = (System.currentTimeMillis() + index).toString())
                    })
                    showSnackbar("成功导入 ${imported.size} 个预设")
                } catch (e: Exception) {
                    showSnackbar("导入失败：${e.message}")
                }
                showImportDialog = false
            },
            onDismiss = { showImportDialog = false }
        )
    }
}

private fun parsePresetsFromJson(json: String): List<SavedPreset> {
    return emptyList()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PresetEditorTopBar(
    onBack: () -> Unit,
    presetName: String,
    onPresetNameChange: (String) -> Unit
) {
    TopAppBar(
        title = {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Sliders,
                        contentDescription = null,
                        tint = AccentPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "预设编辑器",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = "1:1复刻原生相机大师模式参数，创建专属预设，一键贡献社区",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "返回",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background
        )
    )
}

@Composable
fun PreviewPanel(
    filterStyle: FilterStyle,
    filterIntensity: Float,
    modifier: Modifier = Modifier
) {
    var isClicked by remember { mutableStateOf(false) }

    Column(
        modifier = modifier.padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable { isClicked = !isClicked }
            ) {
                AsyncImage(
                    model = "https://picsum.photos/seed/preset_preview/600/800",
                    contentDescription = "预览图片",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    filterStyle.color.copy(alpha = filterIntensity / 200f),
                                    filterStyle.color.copy(alpha = filterIntensity / 300f)
                                )
                            )
                        )
                )

                if (vignetteEnabled) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        Color.Black.copy(alpha = 0.4f)
                                    )
                                )
                            )
                    )
                }

                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.AddPhotoAlternate,
                        contentDescription = "上传图片",
                        tint = Color.White,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "点击上传自定义图片",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White
                    )
                }

                Surface(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp),
                    shape = RoundedCornerShape(8.dp),
                    color = Color.Black.copy(alpha = 0.5f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(filterStyle.color)
                        )
                        Text(
                            text = filterStyle.displayName,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = { },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("重置")
            }

            Button(
                onClick = { },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AccentPrimary,
                    contentColor = OppoDeepSpace
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Download,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("导出")
            }
        }
    }
}

@Composable
fun ParameterPanel(
    selectedFilterStyle: FilterStyle,
    onFilterStyleSelected: (FilterStyle) -> Unit,
    filterIntensity: Float,
    onFilterIntensityChange: (Float) -> Unit,
    saturation: Float,
    onSaturationChange: (Float) -> Unit,
    contrast: Float,
    onContrastChange: (Float) -> Unit,
    brightness: Float,
    onBrightnessChange: (Float) -> Unit,
    colorTemperature: Float,
    onColorTemperatureChange: (Float) -> Unit,
    vignetteEnabled: Boolean,
    onVignetteToggle: (Boolean) -> Unit,
    onSavePreset: () -> Unit,
    onContribute: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            FilterStyleSection(
                selectedStyle = selectedFilterStyle,
                onStyleSelected = onFilterStyleSelected
            )
        }

        item {
            SliderSection(
                label = "滤镜强度",
                value = filterIntensity,
                onValueChange = onFilterIntensityChange,
                valueRange = 0f..100f,
                valueLabel = "${filterIntensity.toInt()}%",
                valueColor = AccentPrimary
            )
        }

        item {
            SliderSection(
                label = "饱和度",
                value = saturation,
                onValueChange = onSaturationChange,
                valueRange = -100f..100f,
                valueLabel = if (saturation >= 0) "+${saturation.toInt()}" else "${saturation.toInt()}"
            )
        }

        item {
            SliderSection(
                label = "对比度",
                value = contrast,
                onValueChange = onContrastChange,
                valueRange = -100f..100f,
                valueLabel = if (contrast >= 0) "+${contrast.toInt()}" else "${contrast.toInt()}"
            )
        }

        item {
            SliderSection(
                label = "亮度",
                value = brightness,
                onValueChange = onBrightnessChange,
                valueRange = -100f..100f,
                valueLabel = if (brightness >= 0) "+${brightness.toInt()}" else "${brightness.toInt()}"
            )
        }

        item {
            ColorTemperatureSection(
                value = colorTemperature,
                onValueChange = onColorTemperatureChange
            )
        }

        item {
            VignetteToggle(
                enabled = vignetteEnabled,
                onToggle = onVignetteToggle
            )
        }

        item {
            ActionButtons(
                onSavePreset = onSavePreset,
                onContribute = onContribute
            )
        }
    }
}

@Composable
fun FilterStyleSection(
    selectedStyle: FilterStyle,
    onStyleSelected: (FilterStyle) -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "滤镜风格",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(12.dp))

            FilterStyleChips(
                selectedStyle = selectedStyle,
                onStyleSelected = onStyleSelected
            )
        }
    }
}

@Composable
fun FilterStyleChips(
    selectedStyle: FilterStyle,
    onStyleSelected: (FilterStyle) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterStyle.entries.take(4).forEach { style ->
                FilterStyleChip(
                    style = style,
                    selected = selectedStyle == style,
                    onClick = { onStyleSelected(style) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterStyle.entries.drop(4).take(4).forEach { style ->
                FilterStyleChip(
                    style = style,
                    selected = selectedStyle == style,
                    onClick = { onStyleSelected(style) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterStyle.entries.drop(8).forEach { style ->
                FilterStyleChip(
                    style = style,
                    selected = selectedStyle == style,
                    onClick = { onStyleSelected(style) },
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

@Composable
fun FilterStyleChip(
    style: FilterStyle,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (selected) style.color.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant,
        animationSpec = tween(durationMillis = OppoAnimationDuration.Fast),
        label = "chip_bg"
    )

    val contentColor by animateColorAsState(
        targetValue = if (selected) style.color else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(durationMillis = OppoAnimationDuration.Fast),
        label = "chip_content"
    )

    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = backgroundColor,
        border = if (selected) BorderStroke(1.5.dp, style.color.copy(alpha = 0.5f)) else null
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(if (selected) style.color else style.color.copy(alpha = 0.5f))
            )
            Text(
                text = style.displayName,
                style = MaterialTheme.typography.labelMedium,
                color = contentColor,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
            )
        }
    }
}

@Composable
fun SliderSection(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    valueLabel: String,
    valueColor: Color = AccentPrimary
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = valueLabel,
                    style = MaterialTheme.typography.labelLarge,
                    color = valueColor,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Slider(
                value = value,
                onValueChange = onValueChange,
                valueRange = valueRange,
                colors = SliderDefaults.colors(
                    thumbColor = valueColor,
                    activeTrackColor = valueColor,
                    inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )
        }
    }
}

@Composable
fun ColorTemperatureSection(
    value: Float,
    onValueChange: (Float) -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "冷暖调",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Medium
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "冷",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF6B98D4)
                    )
                    Text(
                        text = if (value >= 0) "+${value.toInt()}" else "${value.toInt()}",
                        style = MaterialTheme.typography.labelLarge,
                        color = when {
                            value < -30 -> Color(0xFF6B98D4)
                            value > 30 -> Color(0xFFFFB347)
                            else -> AccentPrimary
                        },
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "暖",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFFFB347)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(32.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color(0xFF6B98D4),
                                Color.White,
                                Color(0xFFFFB347)
                            )
                        )
                    )
            ) {
                Slider(
                    value = value,
                    onValueChange = onValueChange,
                    valueRange = -100f..100f,
                    colors = SliderDefaults.colors(
                        thumbColor = Color.White,
                        activeTrackColor = Color.Transparent,
                        inactiveTrackColor = Color.Transparent
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
fun VignetteToggle(
    enabled: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "暗角效果",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "添加边缘暗角，增强视觉焦点",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Switch(
                checked = enabled,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = AccentPrimary,
                    checkedTrackColor = AccentPrimary.copy(alpha = 0.3f)
                )
            )
        }
    }
}

@Composable
fun ActionButtons(
    onSavePreset: () -> Unit,
    onContribute: () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Button(
            onClick = onSavePreset,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = AccentPrimary,
                contentColor = OppoDeepSpace
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Save,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "保存预设",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }

        OutlinedButton(
            onClick = onContribute,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = AccentPrimary
            ),
            border = BorderStroke(1.5.dp, AccentPrimary),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.CloudUpload,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "贡献社区",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun SavePresetDialog(
    presetName: String,
    onPresetNameChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "保存预设",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                Text(
                    text = "为您的预设起个名字",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = presetName,
                    onValueChange = onPresetNameChange,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("输入预设名称") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AccentPrimary,
                        cursorColor = AccentPrimary
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = presetName.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AccentPrimary,
                    contentColor = OppoDeepSpace
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(20.dp)
    )
}

@Composable
fun RenamePresetDialog(
    currentName: String,
    newName: String,
    onNewNameChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "重命名预设",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                Text(
                    text = "当前名称: $currentName",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = newName,
                    onValueChange = onNewNameChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("新名称") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AccentPrimary,
                        cursorColor = AccentPrimary
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = newName.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AccentPrimary,
                    contentColor = OppoDeepSpace
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("确定")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(20.dp)
    )
}

@Composable
fun SortPresetDialog(
    currentSortField: SortField,
    currentSortOrder: SortOrder,
    onSortChange: (SortField, SortOrder) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "排序方式",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SortOptionItem(
                    label = "按名称排序",
                    isSelected = currentSortField == SortField.NAME,
                    sortOrder = if (currentSortField == SortField.NAME) currentSortOrder else null,
                    onClick = {
                        if (currentSortField == SortField.NAME) {
                            onSortChange(SortField.NAME, 
                                if (currentSortOrder == SortOrder.ASCENDING) SortOrder.DESCENDING else SortOrder.ASCENDING
                            )
                        } else {
                            onSortChange(SortField.NAME, SortOrder.ASCENDING)
                        }
                        onDismiss()
                    }
                )
                SortOptionItem(
                    label = "按时间排序",
                    isSelected = currentSortField == SortField.CREATED_AT,
                    sortOrder = if (currentSortField == SortField.CREATED_AT) currentSortOrder else null,
                    onClick = {
                        if (currentSortField == SortField.CREATED_AT) {
                            onSortChange(SortField.CREATED_AT,
                                if (currentSortOrder == SortOrder.ASCENDING) SortOrder.DESCENDING else SortOrder.ASCENDING
                            )
                        } else {
                            onSortChange(SortField.CREATED_AT, SortOrder.DESCENDING)
                        }
                        onDismiss()
                    }
                )
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("关闭")
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(20.dp)
    )
}

@Composable
fun SortOptionItem(
    label: String,
    isSelected: Boolean,
    sortOrder: SortOrder?,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) AccentPrimary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant,
        border = if (isSelected) BorderStroke(1.dp, AccentPrimary.copy(alpha = 0.3f)) else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                color = if (isSelected) AccentPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
            )
            if (isSelected && sortOrder != null) {
                Icon(
                    imageVector = if (sortOrder == SortOrder.ASCENDING) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                    contentDescription = null,
                    tint = AccentPrimary
                )
            }
        }
    }
}

@Composable
fun ImportPresetDialog(
    onImport: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var importText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "导入预设",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                Text(
                    text = "粘贴JSON格式的预设数据",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = importText,
                    onValueChange = { importText = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp),
                    placeholder = { Text("在此粘贴JSON数据...") },
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AccentPrimary,
                        cursorColor = AccentPrimary
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onImport(importText) },
                enabled = importText.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AccentPrimary,
                    contentColor = OppoDeepSpace
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("导入")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(20.dp)
    )
}

@Composable
fun ContributeDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.CloudUpload,
                contentDescription = null,
                tint = AccentPrimary,
                modifier = Modifier.size(48.dp)
            )
        },
        title = {
            Text(
                text = "贡献到社区",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        },
        text = {
            Column {
                Text(
                    text = "您的预设将被分享给其他用户使用，同时有机会获得官方推荐和奖励。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = AccentPrimary.copy(alpha = 0.1f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = AccentPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "贡献默认公开，可随时删除",
                            style = MaterialTheme.typography.bodySmall,
                            color = AccentPrimary
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = AccentPrimary,
                    contentColor = OppoDeepSpace
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("确认贡献")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(20.dp)
    )
}

@Composable
fun PresetRankingSection(
    rankedPresets: List<RankedPreset>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Leaderboard,
                    contentDescription = null,
                    tint = AccentPrimary,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "预设排行榜",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            rankedPresets.forEach { preset ->
                RankedPresetItem(preset = preset)
                if (preset.rank < rankedPresets.size) {
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
fun RankedPresetItem(
    preset: RankedPreset
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(
                        when (preset.rank) {
                            1 -> Color(0xFFFFD700)
                            2 -> Color(0xFFC0C0C0)
                            3 -> Color(0xFFCD7F32)
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${preset.rank}",
                    style = MaterialTheme.typography.labelLarge,
                    color = if (preset.rank <= 3) Color(0xFF333333) else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = preset.name,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = preset.author,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(preset.filterStyle.color)
                    )
                }
            }
        }

        Column(
            horizontalAlignment = Alignment.End
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Download,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${preset.downloads / 1000}K",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = Color(0xFFFFB347)
                )
                Text(
                    text = "${preset.rating}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            preset.tags.take(2).forEach { tag ->
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        text = tag,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun SavedPresetsSection(
    presets: List<SavedPreset>,
    onLoadPreset: (SavedPreset) -> Unit,
    onDeletePreset: (SavedPreset) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Bookmark,
                    contentDescription = null,
                    tint = AccentPrimary,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "已保存的预设",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (presets.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.BookmarkBorder,
                            contentDescription = null,
                            modifier = Modifier.size(40.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "暂无保存的预设",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            } else {
                presets.take(5).forEach { preset ->
                    SavedPresetItem(
                        preset = preset,
                        onLoad = { onLoadPreset(preset) },
                        onDelete = { onDeletePreset(preset) }
                    )
                    if (preset != presets.last() && presets.indexOf(preset) < 4) {
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 8.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SavedPresetItem(
    preset: SavedPreset,
    onLoad: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(preset.filterStyle.color.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Tune,
                    contentDescription = null,
                    tint = preset.filterStyle.color,
                    modifier = Modifier.size(20.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = preset.name,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = preset.filterStyle.displayName,
                    style = MaterialTheme.typography.bodySmall,
                    color = preset.filterStyle.color
                )
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            IconButton(
                onClick = onLoad,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.FileOpen,
                    contentDescription = "加载",
                    tint = AccentPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "删除",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun TipsSection(
    tips: List<String>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = AccentPrimary.copy(alpha = 0.08f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Lightbulb,
                    contentDescription = null,
                    tint = AccentPrimary,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "使用技巧",
                    style = MaterialTheme.typography.titleMedium,
                    color = AccentPrimary,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            tips.forEach { tip ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = AccentPrimary,
                        modifier = Modifier
                            .size(16.dp)
                            .padding(top = 2.dp)
                    )
                    Text(
                        text = tip,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
            }
        }
    }
}

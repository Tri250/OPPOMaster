package com.omaster.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.omaster.app.ui.theme.*

enum class FloatingWindowType(val title: String, val description: String, val icon: ImageVector) {
    STANDARD("标准悬浮窗", "适用于Android 8.0+，兼容性好", Icons.Default.Window),
    ACCESSIBILITY("无障碍悬浮窗", "针对ColorOS/小米等定制系统", Icons.Default.Accessibility),
    NOTIFICATION("通知栏模式", "极端限制场景下的兜底方案", Icons.Default.Notifications)
}

enum class BrandGuide(val brandName: String, val steps: List<String>) {
    OPPO("OPPO/一加/realme", listOf(
        "打开设置 > 应用管理",
        "找到小O帮帮 > 悬浮窗",
        "选择\"允许创建悬浮窗\""
    )),
    XIAOMI("小米", listOf(
        "打开设置 > 应用设置",
        "找到小O帮帮 > 权限管理",
        "开启\"显示悬浮窗\"权限"
    )),
    VIVO("vivo", listOf(
        "打开设置 > 应用与权限",
        "找到小O帮帮 > 权限",
        "开启\"悬浮窗\"开关"
    )),
    HUAWEI("华为", listOf(
        "打开设置 > 应用 > 应用管理",
        "找到小O帮帮 > 权限",
        "开启\"悬浮窗\"权限"
    ))
}

data class FilterPreset(val name: String, val color: Color)

data class AddedWindow(
    val id: Int,
    val filterName: String,
    val filterColor: Color,
    val intensity: Float = 50f,
    val isVisible: Boolean = true,
    val isLocked: Boolean = false
)

@Composable
fun FloatingWindowScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedWindowType by remember { mutableStateOf(FloatingWindowType.STANDARD) }
    var opacity by remember { mutableFloatStateOf(70f) }
    var addedWindows by remember { mutableStateOf(listOf<AddedWindow>()) }
    var showPermissionGuide by remember { mutableStateOf(false) }
    var selectedBrand by remember { mutableStateOf(BrandGuide.OPPO) }

    val filterPresets = remember {
        listOf(
            FilterPreset("富士胶片", Color(0xFFE8A87C)),
            FilterPreset("徕卡经典", Color(0xFF8B4513)),
            FilterPreset("哈苏自然", Color(0xFFD4A574)),
            FilterPreset("赛博朋克", Color(0xFF00D4FF)),
            FilterPreset("人像暖色", Color(0xFFFFB347)),
            FilterPreset("夜景大师", Color(0xFF4169E1))
        )
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "智能悬浮窗",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onBackground
                    )
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
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                PageHeader()
            }

            item {
                WindowTypeSection(
                    selectedType = selectedWindowType,
                    onTypeSelected = { selectedWindowType = it }
                )
            }

            item {
                PermissionGuideSection(
                    showGuide = showPermissionGuide,
                    selectedBrand = selectedBrand,
                    onToggleGuide = { showPermissionGuide = !showPermissionGuide },
                    onBrandSelected = { selectedBrand = it }
                )
            }

            item {
                GlobalSettingsSection(opacity = opacity, onOpacityChange = { opacity = it })
            }

            item {
                FilterPresetsSection(
                    presets = filterPresets,
                    onAddWindow = { preset ->
                        val newId = (addedWindows.maxOfOrNull { it.id } ?: 0) + 1
                        addedWindows = addedWindows + AddedWindow(
                            id = newId,
                            filterName = preset.name,
                            filterColor = preset.color
                        )
                    }
                )
            }

            if (addedWindows.isNotEmpty()) {
                item {
                    AddedWindowsSection(
                        windows = addedWindows,
                        onIntensityChange = { id, intensity ->
                            addedWindows = addedWindows.map {
                                if (it.id == id) it.copy(intensity = intensity) else it
                            }
                        },
                        onVisibilityToggle = { id ->
                            addedWindows = addedWindows.map {
                                if (it.id == id) it.copy(isVisible = !it.isVisible) else it
                            }
                        },
                        onLockToggle = { id ->
                            addedWindows = addedWindows.map {
                                if (it.id == id) it.copy(isLocked = !it.isLocked) else it
                            }
                        },
                        onDelete = { id ->
                            addedWindows = addedWindows.filter { it.id != id }
                        }
                    )
                }
            }

            item {
                UsageInstructionsSection()
            }

            item {
                CompatibilityStatsSection()
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun PageHeader() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = AccentPrimary.copy(alpha = 0.12f),
            modifier = Modifier.size(72.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.Layers,
                    contentDescription = null,
                    tint = AccentPrimary,
                    modifier = Modifier.size(36.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "智能悬浮窗",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "多悬浮窗类型兼容方案，兼容率达95%+",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun WindowTypeSection(
    selectedType: FloatingWindowType,
    onTypeSelected: (FloatingWindowType) -> Unit
) {
    Column {
        SectionTitle(title = "悬浮窗类型选择")

        Spacer(modifier = Modifier.height(12.dp))

        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            FloatingWindowType.values().forEach { type ->
                WindowTypeCard(
                    type = type,
                    isSelected = selectedType == type,
                    onClick = { onTypeSelected(type) }
                )
            }
        }
    }
}

@Composable
private fun WindowTypeCard(
    type: FloatingWindowType,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) {
            AccentPrimary.copy(alpha = 0.12f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        },
        animationSpec = tween(durationMillis = 200),
        label = "card_bg"
    )

    val borderColor by animateColorAsState(
        targetValue = if (isSelected) AccentPrimary else Color.Transparent,
        animationSpec = tween(durationMillis = 200),
        label = "card_border"
    )

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        border = BorderStroke(1.5.dp, borderColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = if (isSelected) AccentPrimary else MaterialTheme.colorScheme.surfaceVariant
            ) {
                Icon(
                    imageVector = type.icon,
                    contentDescription = null,
                    tint = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .size(48.dp)
                        .padding(12.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = type.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = type.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "已选择",
                    tint = AccentPrimary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
private fun PermissionGuideSection(
    showGuide: Boolean,
    selectedBrand: BrandGuide,
    onToggleGuide: () -> Unit,
    onBrandSelected: (BrandGuide) -> Unit
) {
    Column {
        SectionTitle(title = "分品牌权限引导")

        Spacer(modifier = Modifier.height(12.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            onClick = onToggleGuide
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Help,
                        contentDescription = null,
                        tint = AccentPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "查看各品牌开启步骤",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Icon(
                    imageVector = if (showGuide) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        AnimatedVisibility(
            visible = showGuide,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column(modifier = Modifier.padding(top = 12.dp)) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(BrandGuide.values().toList()) { brand ->
                        FilterChip(
                            selected = selectedBrand == brand,
                            onClick = { onBrandSelected(brand) },
                            label = { Text(brand.brandName) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = AccentPrimary.copy(alpha = 0.12f),
                                selectedLabelColor = AccentPrimary
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = selectedBrand.brandName,
                            style = MaterialTheme.typography.titleMedium,
                            color = AccentPrimary,
                            fontWeight = FontWeight.SemiBold
                        )

                        selectedBrand.steps.forEachIndexed { index, step ->
                            Row(verticalAlignment = Alignment.Top) {
                                Surface(
                                    shape = CircleShape,
                                    color = AccentPrimary.copy(alpha = 0.2f),
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = "${index + 1}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = AccentPrimary,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = step,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1f)
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
private fun GlobalSettingsSection(
    opacity: Float,
    onOpacityChange: (Float) -> Unit
) {
    Column {
        SectionTitle(title = "全局设置")

        Spacer(modifier = Modifier.height(12.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "悬浮窗透明度",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${opacity.toInt()}%",
                        style = MaterialTheme.typography.titleMedium,
                        color = AccentPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Slider(
                    value = opacity,
                    onValueChange = onOpacityChange,
                    valueRange = 0f..100f,
                    steps = 99,
                    colors = SliderDefaults.colors(
                        thumbColor = AccentPrimary,
                        activeTrackColor = AccentPrimary,
                        inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                )
            }
        }
    }
}

@Composable
private fun FilterPresetsSection(
    presets: List<FilterPreset>,
    onAddWindow: (FilterPreset) -> Unit
) {
    Column {
        SectionTitle(title = "添加滤镜悬浮窗")

        Spacer(modifier = Modifier.height(12.dp))

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(presets) { preset ->
                FilterPresetCard(preset = preset, onClick = { onAddWindow(preset) })
            }
        }
    }
}

@Composable
private fun FilterPresetCard(
    preset: FilterPreset,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                shape = CircleShape,
                color = preset.color,
                modifier = Modifier.size(48.dp)
            ) {}

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = preset.name,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(4.dp))

            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "添加",
                tint = AccentPrimary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun AddedWindowsSection(
    windows: List<AddedWindow>,
    onIntensityChange: (Int, Float) -> Unit,
    onVisibilityToggle: (Int) -> Unit,
    onLockToggle: (Int) -> Unit,
    onDelete: (Int) -> Unit
) {
    Column {
        SectionTitle(title = "已添加的悬浮窗")

        Spacer(modifier = Modifier.height(12.dp))

        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            windows.forEach { window ->
                AddedWindowCard(
                    window = window,
                    onIntensityChange = { onIntensityChange(window.id, it) },
                    onVisibilityToggle = { onVisibilityToggle(window.id) },
                    onLockToggle = { onLockToggle(window.id) },
                    onDelete = { onDelete(window.id) }
                )
            }
        }
    }
}

@Composable
private fun AddedWindowCard(
    window: AddedWindow,
    onIntensityChange: (Float) -> Unit,
    onVisibilityToggle: () -> Unit,
    onLockToggle: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = window.filterColor,
                        modifier = Modifier.size(32.dp)
                    ) {}

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = window.filterName,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (window.isVisible) "显示中" else "已隐藏",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(onClick = onVisibilityToggle, modifier = Modifier.size(36.dp)) {
                        Icon(
                            imageVector = if (window.isVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = if (window.isVisible) "隐藏" else "显示",
                            tint = if (window.isVisible) AccentPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    IconButton(onClick = onLockToggle, modifier = Modifier.size(36.dp)) {
                        Icon(
                            imageVector = if (window.isLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                            contentDescription = if (window.isLocked) "解锁" else "锁定",
                            tint = if (window.isLocked) AccentPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "删除",
                            tint = Error,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "强度",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${window.intensity.toInt()}%",
                    style = MaterialTheme.typography.labelMedium,
                    color = AccentPrimary
                )
            }

            Slider(
                value = window.intensity,
                onValueChange = onIntensityChange,
                valueRange = 0f..100f,
                colors = SliderDefaults.colors(
                    thumbColor = AccentPrimary,
                    activeTrackColor = AccentPrimary,
                    inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )
        }
    }
}

@Composable
private fun UsageInstructionsSection() {
    Column {
        SectionTitle(title = "使用说明")

        Spacer(modifier = Modifier.height(12.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                UsageItem(
                    icon = Icons.Default.TouchApp,
                    title = "快速显示/隐藏",
                    description = "双击悬浮窗即可快速切换显示状态"
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                UsageItem(
                    icon = Icons.Default.Lock,
                    title = "锁定位置",
                    description = "锁定后可防止误触移动悬浮窗"
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                UsageItem(
                    icon = Icons.Default.TouchApp,
                    title = "拖动调整",
                    description = "长按悬浮窗边缘即可拖动调整位置和大小"
                )
            }
        }
    }
}

@Composable
private fun UsageItem(
    icon: ImageVector,
    title: String,
    description: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = AccentPrimary.copy(alpha = 0.12f),
            modifier = Modifier.size(40.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = AccentPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun CompatibilityStatsSection() {
    Column {
        SectionTitle(title = "兼容性统计")

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatCard(
                modifier = Modifier.weight(1f),
                value = "95%+",
                label = "机型兼容率"
            )
            StatCard(
                modifier = Modifier.weight(1f),
                value = "<200ms",
                label = "交互响应"
            )
            StatCard(
                modifier = Modifier.weight(1f),
                value = "3种",
                label = "悬浮窗类型"
            )
        }
    }
}

@Composable
private fun StatCard(
    modifier: Modifier = Modifier,
    value: String,
    label: String
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = AccentPrimary.copy(alpha = 0.08f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                color = AccentPrimary,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontWeight = FontWeight.SemiBold
    )
}

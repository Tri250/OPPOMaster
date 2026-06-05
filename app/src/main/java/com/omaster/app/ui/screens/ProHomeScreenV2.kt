package com.omaster.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.omaster.app.data.ThemeMode
import com.omaster.app.model.Preset
import com.omaster.app.ui.animation.ColorOSAnimationDuration
import com.omaster.app.ui.animation.ColorOSEasing
import com.omaster.app.ui.animation.ColorOSScale
import com.omaster.app.ui.components.*
import com.omaster.app.ui.theme.*
import com.omaster.app.viewmodel.MainViewModel
import com.omaster.app.viewmodel.FilterType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProHomeScreenV2(
    onPresetClick: (Preset) -> Unit,
    onSettingsClick: () -> Unit,
    onSceneDetectionClick: () -> Unit,
    onAiFineTuneClick: () -> Unit,
    onWatermarkClick: () -> Unit,
    onColorOSHomeClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MainViewModel = hiltViewModel()
) {
    val presets by viewModel.presets.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val filterType by viewModel.filterType.collectAsState()
    val themeMode by viewModel.themeMode.collectAsState()
    
    val filteredPresets = remember(presets, searchQuery, filterType) {
        presets.filter { preset ->
            val matchesQuery = searchQuery.isEmpty() ||
                preset.name.contains(searchQuery, ignoreCase = true) ||
                preset.deviceModel.contains(searchQuery, ignoreCase = true) ||
                preset.tags.any { it.contains(searchQuery, ignoreCase = true) }
            
            val matchesFilter = when (filterType) {
                FilterType.ALL -> true
                FilterType.FAVORITES -> preset.isFavorite
                FilterType.HNCS -> preset.isHncsCertified
                FilterType.FIND_X -> preset.deviceModel.contains("Find X", ignoreCase = true)
                FilterType.RENO -> preset.deviceModel.contains("Reno", ignoreCase = true)
                FilterType.NEW -> preset.version.contains("3.0") || preset.downloadCount < 5000
                FilterType.TRENDING -> preset.downloadCount > 10000
            }
            
            matchesQuery && matchesFilter
        }
    }
    
    var isLoading by remember { mutableStateOf(true) }
    
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(300)
        isLoading = false
    }
    
    Scaffold(
        containerColor = Colors.Background,
        modifier = modifier
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(bottom = Spacing.xxl)
        ) {
            item {
                ProTopAppBarV2(
                    onSettingsClick = onSettingsClick,
                    onColorOSHomeClick = onColorOSHomeClick
                )
            }
            
            item {
                ProHeroSectionV2()
            }
            
            item {
                ProFeatureGridV2(
                    onSceneDetectionClick = onSceneDetectionClick,
                    onAiFineTuneClick = onAiFineTuneClick,
                    onWatermarkClick = onWatermarkClick
                )
            }
            
            item {
                GlassAnimatedSearchBar(
                    query = searchQuery,
                    onQueryChange = { viewModel.onSearchQueryChanged(it) },
                    onClearQuery = { viewModel.onSearchQueryChanged("") },
                    delayMillis = 200
                )
            }
            
            item {
                GlassFilterChips(
                    selectedFilter = filterType,
                    onFilterSelected = { viewModel.onFilterTypeChanged(it) }
                )
            }
            
            item {
                SectionHeaderV2(
                    title = when (filterType) {
                        FilterType.FAVORITES -> "我的收藏"
                        FilterType.HNCS -> "HNCS认证"
                        FilterType.FIND_X -> "Find X 系列"
                        FilterType.RENO -> "Reno 系列"
                        FilterType.NEW -> "最新预设"
                        FilterType.TRENDING -> "热门趋势"
                        else -> "哈苏大师预设"
                    }
                )
            }
            
            if (isLoading) {
                items(3) { index ->
                    GlassPresetCardShimmer(
                        modifier = Modifier.padding(horizontal = Spacing.ScreenPadding, vertical = Spacing.sm)
                    )
                }
            } else if (filteredPresets.isEmpty()) {
                item {
                    ProEmptyState(
                        message = if (searchQuery.isNotEmpty()) "没有找到匹配的预设" 
                                  else if (filterType == FilterType.FAVORITES) "暂无收藏的预设"
                                  else "暂无预设",
                        modifier = Modifier.padding(horizontal = Spacing.ScreenPadding)
                    )
                }
            } else {
                itemsIndexed(
                    items = filteredPresets,
                    key = { _, preset -> preset.id }
                ) { index, preset ->
                    GlassPresetCard(
                        preset = preset,
                        onClick = { onPresetClick(preset) },
                        onFavoriteToggle = { viewModel.toggleFavorite(preset) },
                        isNew = index < 3,
                        index = index,
                        modifier = Modifier.padding(horizontal = Spacing.ScreenPadding, vertical = Spacing.sm)
                    )
                }
            }
        }
    }
}

@Composable
private fun ProTopAppBarV2(
    onSettingsClick: () -> Unit,
    onColorOSHomeClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.ScreenPadding, vertical = Spacing.md),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
        ) {
            Surface(
                modifier = Modifier.size(48.dp),
                shape = CircleShape,
                color = Colors.HasselbladOrange
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "H",
                        color = Colors.OnPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                }
            }
            
            Column {
                Text(
                    text = "哈苏影像",
                    style = Typography.HeadlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Colors.OnBackground
                )
                Text(
                    text = "OPPO Find X8 Ultra",
                    style = Typography.LabelSmall,
                    color = Colors.OnSurfaceVariant
                )
            }
        }
        
        Row(
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
        ) {
            GlassIconButton(
                icon = Icons.Default.ViewModule,
                onClick = onColorOSHomeClick,
                contentDescription = "ColorOS视图",
                size = 44.dp
            )
            
            GlassIconButton(
                icon = Icons.Default.Settings,
                onClick = onSettingsClick,
                contentDescription = "设置",
                size = 44.dp
            )
        }
    }
}

@Composable
private fun ProHeroSectionV2() {
    var isVisible by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(100)
        isVisible = true
    }
    
    val animatedAlpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(
            durationMillis = ColorOSAnimationDuration.SLOW,
            easing = ColorOSEasing.Decelerate
        ),
        label = "heroAlpha"
    )
    
    val animatedOffsetY by animateFloatAsState(
        targetValue = if (isVisible) 0f else 50f,
        animationSpec = tween(
            durationMillis = ColorOSAnimationDuration.SLOW,
            easing = ColorOSEasing.Decelerate
        ),
        label = "heroOffsetY"
    )
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.ScreenPadding)
            .graphicsLayer {
                alpha = animatedAlpha
                translationY = animatedOffsetY
            }
    ) {
        GlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp),
            onClick = {}
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Colors.HasselbladOrange.copy(alpha = 0.3f),
                                Colors.HasselbladBrown.copy(alpha = 0.2f),
                                Colors.Background
                            )
                        )
                    )
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(180.dp)
                        .offset(x = 60.dp, y = (-40).dp)
                        .background(
                            color = Colors.HasselbladOrange.copy(alpha = 0.1f),
                            shape = CircleShape
                        )
                )
                
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(Spacing.xl),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "大师影像",
                        style = Typography.DisplaySmall,
                        fontWeight = FontWeight.Bold,
                        color = Colors.HasselbladOrange
                    )
                    
                    Spacer(modifier = Modifier.height(Spacing.sm))
                    
                    Text(
                        text = "探索哈苏认证的专业摄影预设",
                        style = Typography.BodyLarge,
                        color = Colors.OnSurfaceVariant
                    )
                    
                    Spacer(modifier = Modifier.height(Spacing.lg))
                    
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(Spacing.xl)
                    ) {
                        ProMetric(value = "11+", label = "摄影预设")
                        ProMetric(value = "7+", label = "场景模式")
                        ProMetric(value = "哈苏", label = "HNCS认证")
                    }
                }
            }
        }
    }
}

@Composable
private fun ProMetric(
    value: String,
    label: String
) {
    Column {
        Text(
            text = value,
            style = Typography.TitleLarge,
            fontWeight = FontWeight.Bold,
            color = Colors.HasselbladOrange
        )
        Text(
            text = label,
            style = Typography.LabelSmall,
            color = Colors.OnSurfaceVariant
        )
    }
}

@Composable
private fun ProFeatureGridV2(
    onSceneDetectionClick: () -> Unit,
    onAiFineTuneClick: () -> Unit,
    onWatermarkClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.ScreenPadding, vertical = Spacing.md),
        horizontalArrangement = Arrangement.spacedBy(Spacing.md)
    ) {
        ProFeatureCardV2(
            icon = Icons.Default.Visibility,
            title = "场景检测",
            subtitle = "AI智能识别",
            gradientColors = listOf(Colors.AccentBlue.copy(alpha = 0.2f), Colors.AccentBlue.copy(alpha = 0.1f)),
            onClick = onSceneDetectionClick,
            modifier = Modifier.weight(1f)
        )
        
        ProFeatureCardV2(
            icon = Icons.Default.AutoFixHigh,
            title = "AI微调",
            subtitle = "专业调色",
            gradientColors = listOf(Colors.AccentGreen.copy(alpha = 0.2f), Colors.AccentGreen.copy(alpha = 0.1f)),
            onClick = onAiFineTuneClick,
            modifier = Modifier.weight(1f)
        )
        
        ProFeatureCardV2(
            icon = Icons.Default.Brush,
            title = "水印编辑",
            subtitle = "哈苏水印",
            gradientColors = listOf(Colors.HasselbladOrange.copy(alpha = 0.2f), Colors.HasselbladOrange.copy(alpha = 0.1f)),
            onClick = onWatermarkClick,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun ProFeatureCardV2(
    icon: ImageVector,
    title: String,
    subtitle: String,
    gradientColors: List<Color>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed) ColorOSScale.Pressed else 1f,
        animationSpec = spring(
            dampingRatio = 0.8f,
            stiffness = 400f
        ),
        label = "featureScale"
    )
    
    GlassCard(
        modifier = modifier.scale(scale),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(
                        brush = Brush.linearGradient(colors = gradientColors)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = gradientColors.first(),
                    modifier = Modifier.size(28.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(Spacing.sm))
            
            Text(
                text = title,
                style = Typography.LabelLarge,
                fontWeight = FontWeight.SemiBold,
                color = Colors.OnSurface
            )
            
            Text(
                text = subtitle,
                style = Typography.LabelSmall,
                color = Colors.OnSurfaceVariant
            )
        }
    }
}

@Composable
private fun SectionHeaderV2(
    title: String
) {
    Text(
        text = title,
        style = Typography.HeadlineMedium,
        fontWeight = FontWeight.Bold,
        color = Colors.OnBackground,
        modifier = Modifier.padding(horizontal = Spacing.ScreenPadding, vertical = Spacing.md)
    )
}

@Composable
private fun ProEmptyState(
    message: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.xxl),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.ImageNotSupported,
                contentDescription = null,
                tint = Colors.OnSurfaceVariant,
                modifier = Modifier.size(64.dp)
            )
            
            Spacer(modifier = Modifier.height(Spacing.md))
            
            Text(
                text = message,
                style = Typography.BodyLarge,
                color = Colors.OnSurfaceVariant
            )
        }
    }
}

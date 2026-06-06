package com.omaster.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ripple.rememberRipple
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
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.omaster.app.data.ThemeMode
import com.omaster.app.domain.model.Preset
import com.omaster.app.ui.animation.ColorOSElevation
import com.omaster.app.ui.animation.ColorOSScale
import com.omaster.app.ui.animation.clickableWithColorOSFeedback
import com.omaster.app.ui.components.ProFeatureCard
import com.omaster.app.ui.components.ProPresetCard
import com.omaster.app.ui.components.ProSearchBar
import com.omaster.app.ui.theme.*
import com.omaster.app.viewmodel.MainViewModel

/**
 * ==================== ProHomeScreen - ColorOS 16 专业摄影首页 ====================
 * OPPO Find X8 Pro 哈苏影像专业体验
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ProHomeScreen(
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
    val isDark = themeMode == ThemeMode.DARK.value
    
    var isSearching by remember { mutableStateOf(false) }
    
    Scaffold(
        containerColor = if (isDark) ColorOSBlack else ColorOSLightBackground,
        topBar = {
            ProTopAppBar(
                onSettingsClick = onSettingsClick,
                isDark = isDark
            )
        },
        modifier = modifier
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Hero 区域
            item {
                ProHeroSection(isDark = isDark)
            }
            
            // 功能卡片网格
            item {
                ProFeatureGrid(
                    onSceneDetectionClick = onSceneDetectionClick,
                    onAiFineTuneClick = onAiFineTuneClick,
                    onWatermarkClick = onWatermarkClick,
                    isDark = isDark
                )
            }
            
            // 搜索栏
            item {
                ProSearchBar(
                    query = searchQuery,
                    onQueryChange = {
                        viewModel.onSearchQueryChanged(it)
                        isSearching = it.isNotEmpty()
                    },
                    isDark = isDark
                )
            }
            
            // 分类标签
            item {
                ProFilterChips(
                    selectedFilter = filterType,
                    onFilterSelected = { viewModel.onFilterTypeChanged(it) },
                    isDark = isDark
                )
            }
            
            // 预设列表标题
            item {
                SectionHeader(
                    title = if (filterType == "favorite") "我的收藏" else "哈苏大师预设",
                    isDark = isDark
                )
            }
            
            // 预设卡片列表
            items(
                items = presets,
                key = { it.id }
            ) { preset ->
                ProPresetCard(
                    preset = preset,
                    onClick = { onPresetClick(preset) },
                    onFavoriteToggle = { viewModel.toggleFavorite(preset) },
                    isDark = isDark,
                    modifier = Modifier
                        .animateItemPlacement(
                            animationSpec = tween(
                                durationMillis = 300,
                                easing = androidx.compose.animation.core.EaseInOutCubic
                            )
                        )
                )
            }
            
            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

/**
 * ==================== ProTopAppBar - 专业顶部栏 ====================
 */
@Composable
fun ProTopAppBar(
    onSettingsClick: () -> Unit,
    isDark: Boolean
) {
    TopAppBar(
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 哈苏标志
                Surface(
                    modifier = Modifier.size(32.dp),
                    shape = CircleShape,
                    color = HasselbladOrange
                ) {
                    Box(
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "H",
                            color = Color.Black,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }
                
                // 标题
                Column {
                    Text(
                        text = "哈苏影像",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) ColorOSTextPrimary else ColorOSLightTextPrimary
                    )
                    Text(
                        text = "OPPO Find X8 Pro",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isDark) ColorOSTextTertiary else ColorOSLightTextTertiary
                    )
                }
            }
        },
        actions = {
            IconButton(
                onClick = onSettingsClick,
                modifier = Modifier.clickableWithColorOSFeedback()
            ) {
                Icon(
                    imageVector = androidx.compose.material.icons.Icons.Default.Settings,
                    contentDescription = "设置",
                    tint = if (isDark) ColorOSTextSecondary else ColorOSLightTextSecondary
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = if (isDark) ColorOSBlack else ColorOSLightBackground
        )
    )
}

/**
 * ==================== ProHeroSection - 专业 Hero 区域 ====================
 */
@Composable
fun ProHeroSection(
    isDark: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.linearGradient(
                        colors = GradientHasselbladMaster
                    )
                )
        ) {
            // 装饰性元素
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(160.dp)
                    .offset(x = 40.dp, y = (-40).dp)
                    .background(
                        color = Color.White.copy(alpha = 0.1f),
                        shape = CircleShape
                    )
            )
            
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .size(100.dp)
                    .offset(x = (-20).dp, y = 20.dp)
                    .background(
                        color = Color.Black.copy(alpha = 0.1f),
                        shape = CircleShape
                    )
            )
            
            // 内容区域
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "大师影像",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = "探索哈苏认证的专业摄影预设",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.9f)
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // 快速指标
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    HeroMetric("120+", "摄影预设", isDark = true)
                    HeroMetric("5", "场景模式", isDark = true)
                    HeroMetric("哈苏", "HNCS认证", isDark = true)
                }
            }
        }
    }
}

/**
 * ==================== HeroMetric - Hero 指标 ====================
 */
@Composable
fun HeroMetric(
    value: String,
    label: String,
    isDark: Boolean
) {
    Column {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.8f)
        )
    }
}

/**
 * ==================== ProFeatureGrid - 专业功能网格 ====================
 */
@Composable
fun ProFeatureGrid(
    onSceneDetectionClick: () -> Unit,
    onAiFineTuneClick: () -> Unit,
    onWatermarkClick: () -> Unit,
    isDark: Boolean
) {
    val features = listOf(
        ProFeature(
            icon = androidx.compose.material.icons.Icons.Default.Visibility,
            title = "场景检测",
            subtitle = "AI智能识别",
            gradient = GradientAuroraMaster,
            onClick = onSceneDetectionClick
        ),
        ProFeature(
            icon = androidx.compose.material.icons.Icons.Default.AutoFixHigh,
            title = "AI微调",
            subtitle = "专业调色",
            gradient = GradientDeepOcean,
            onClick = onAiFineTuneClick
        ),
        ProFeature(
            icon = androidx.compose.material.icons.Icons.Default.Brush,
            title = "水印编辑",
            subtitle = "哈苏水印",
            gradient = GradientCosmicArt,
            onClick = onWatermarkClick
        )
    )
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        features.forEach { feature ->
            ProFeatureCard(
                feature = feature,
                isDark = isDark,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/**
 * ==================== ProFeature - 专业功能数据类 ====================
 */
data class ProFeature(
    val icon: ImageVector,
    val title: String,
    val subtitle: String,
    val gradient: List<Color>,
    val onClick: () -> Unit
)

/**
 * ==================== ProFilterChips - 专业筛选标签 ====================
 */
@Composable
fun ProFilterChips(
    selectedFilter: String,
    onFilterSelected: (String) -> Unit,
    isDark: Boolean
) {
    val filters = listOf(
        "all" to "全部",
        "favorite" to "收藏",
        "hncs" to "HNCS",
        "portrait" to "人像",
        "landscape" to "风景",
        "night" to "夜景"
    )
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        filters.forEach { (key, label) ->
            val isSelected = selectedFilter == key
            FilterChip(
                selected = isSelected,
                onClick = { onFilterSelected(key) },
                label = {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = if (isDark) HasselbladOrangeDark else HasselbladOrangeLight,
                    selectedLabelColor = if (isDark) Color.White else Color.Black,
                    containerColor = if (isDark) ColorOSCard else ColorOSLightCard,
                    labelColor = if (isDark) ColorOSTextSecondary else ColorOSLightTextSecondary
                ),
                border = FilterChipDefaults.filterChipBorder(
                    borderColor = Color.Transparent
                ),
                shape = RoundedCornerShape(12.dp)
            )
        }
    }
}

/**
 * ==================== SectionHeader - 分区标题 ====================
 */
@Composable
fun SectionHeader(
    title: String,
    isDark: Boolean
) {
    Text(
        text = title,
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold,
        color = if (isDark) ColorOSTextPrimary else ColorOSLightTextPrimary
    )
}

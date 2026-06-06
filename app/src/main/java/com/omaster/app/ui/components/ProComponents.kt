package com.omaster.app.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
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
import coil.compose.AsyncImage
import com.omaster.app.model.Preset
import com.omaster.app.ui.theme.*

/**
 * ==================== ProPresetCard - 专业预设卡片 ====================
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProPresetCard(
    preset: Preset,
    onClick: () -> Unit,
    onFavoriteToggle: () -> Unit,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = spring(
            dampingRatio = 0.85f,
            stiffness = 300f
        ),
        label = "cardScale"
    )
    
    val elevation by animateDpAsState(
        targetValue = if (isPressed) 2.dp else 4.dp,
        animationSpec = spring(
            dampingRatio = 0.85f,
            stiffness = 300f
        ),
        label = "cardElevation"
    )
    
    Card(
        onClick = onClick,
        interactionSource = interactionSource,
        modifier = modifier
            .fillMaxWidth()
            .scale(scale),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDark) ColorOSCard else ColorOSLightCard
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = elevation)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // 图片区域
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
            ) {
                AsyncImage(
                    model = preset.coverUrl,
                    contentDescription = preset.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                
                // 渐变遮罩
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.4f)
                                )
                            )
                        )
                )
                
                // 右上角标签
                Row(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (preset.hasselbladHncs) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = HasselbladOrange.copy(alpha = 0.9f)
                        ) {
                            Text(
                                text = "HNCS",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
                
                // 收藏按钮
                IconButton(
                    onClick = onFavoriteToggle,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(12.dp)
                        .size(36.dp)
                        .background(
                            color = Color.Black.copy(alpha = 0.5f),
                            shape = CircleShape
                        )
                ) {
                    Icon(
                        imageVector = if (preset.isFavorite) {
                            androidx.compose.material.icons.Icons.Default.Favorite
                        } else {
                            androidx.compose.material.icons.Icons.Default.FavoriteBorder
                        },
                        contentDescription = if (preset.isFavorite) "取消收藏" else "收藏",
                        tint = if (preset.isFavorite) HasselbladOrange else Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            
            // 内容区域
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
                    Column {
                        Text(
                            text = preset.name,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) ColorOSTextPrimary else ColorOSLightTextPrimary
                        )
                        
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        Text(
                            text = "${preset.deviceModel} · ${preset.author}",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isDark) ColorOSTextTertiary else ColorOSLightTextTertiary
                        )
                    }
                    
                    // 场景标签
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = when (preset.sceneType) {
                            "portrait" -> SunsetRed.copy(alpha = 0.15f)
                            "landscape" -> AuroraGreen.copy(alpha = 0.15f)
                            "night" -> DeepOceanBlue.copy(alpha = 0.15f)
                            "sunset" -> HasselbladOrange.copy(alpha = 0.15f)
                            "food" -> CosmicPurple.copy(alpha = 0.15f)
                            else -> ColorOSGrey400.copy(alpha = 0.15f)
                        }
                    ) {
                        Text(
                            text = when (preset.sceneType) {
                                "portrait" -> "人像"
                                "landscape" -> "风景"
                                "night" -> "夜景"
                                "sunset" -> "日落"
                                "food" -> "美食"
                                else -> "通用"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                            color = when (preset.sceneType) {
                                "portrait" -> SunsetRed
                                "landscape" -> AuroraGreen
                                "night" -> DeepOceanBlue
                                "sunset" -> HasselbladOrange
                                "food" -> CosmicPurple
                                else -> if (isDark) ColorOSTextTertiary else ColorOSLightTextTertiary
                            },
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // 参数预览
                preset.cameraParams?.let { params ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        ParamPreviewItem(
                            label = "ISO",
                            value = params.iso.toString(),
                            color = ColorISO,
                            isDark = isDark
                        )
                        ParamPreviewItem(
                            label = "快门",
                            value = params.shutter,
                            color = ColorShutter,
                            isDark = isDark
                        )
                        ParamPreviewItem(
                            label = "曝光",
                            value = params.ev,
                            color = ColorEV,
                            isDark = isDark
                        )
                        ParamPreviewItem(
                            label = "白平衡",
                            value = params.wb,
                            color = ColorWB,
                            isDark = isDark
                        )
                    }
                }
            }
        }
    }
}

/**
 * ==================== ParamPreviewItem - 参数预览项 ====================
 */
@Composable
fun ParamPreviewItem(
    label: String,
    value: String,
    color: Color,
    isDark: Boolean
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (isDark) ColorOSTextTertiary else ColorOSLightTextTertiary
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

/**
 * ==================== ProFeatureCard - 专业功能卡片 ====================
 */
@Composable
fun ProFeatureCard(
    feature: ProFeature,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = spring(
            dampingRatio = 0.85f,
            stiffness = 300f
        ),
        label = "featureScale"
    )
    
    Card(
        onClick = feature.onClick,
        interactionSource = interactionSource,
        modifier = modifier
            .aspectRatio(1f)
            .scale(scale),
        shape = RoundedCornerShape(16.dp),
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
                        colors = feature.gradient
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color.White.copy(alpha = 0.2f)
                ) {
                    Icon(
                        imageVector = feature.icon,
                        contentDescription = feature.title,
                        tint = Color.White,
                        modifier = Modifier
                            .size(48.dp)
                            .padding(12.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Text(
                    text = feature.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                
                Text(
                    text = feature.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.8f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    }
}

/**
 * ==================== ProSearchBar - 专业搜索栏 ====================
 */
@Composable
fun ProSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = if (isDark) ColorOSCard else ColorOSLightCard,
        shadowElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = androidx.compose.material.icons.Icons.Default.Search,
                contentDescription = "搜索",
                tint = if (isDark) ColorOSTextTertiary else ColorOSLightTextTertiary
            )
            
            TextField(
                value = query,
                onValueChange = onQueryChange,
                placeholder = {
                    Text(
                        text = "搜索预设、设备、场景...",
                        color = if (isDark) ColorOSTextTertiary else ColorOSLightTextTertiary
                    )
                },
                modifier = Modifier.weight(1f),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent
                ),
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    color = if (isDark) ColorOSTextPrimary else ColorOSLightTextPrimary
                ),
                singleLine = true
            )
            
            if (query.isNotEmpty()) {
                IconButton(
                    onClick = { onQueryChange("") }
                ) {
                    Icon(
                        imageVector = androidx.compose.material.icons.Icons.Default.Clear,
                        contentDescription = "清除",
                        tint = if (isDark) ColorOSTextSecondary else ColorOSLightTextSecondary
                    )
                }
            }
        }
    }
}

/**
 * ==================== OMasterTopBar - 专业顶部栏 ====================
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OMasterTopBar(
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    TopAppBar(
        title = {
            Text(
                text = "哈苏大师",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        },
        actions = {
            IconButton(onClick = onSettingsClick) {
                Icon(
                    imageVector = androidx.compose.material.icons.Icons.Default.Settings,
                    contentDescription = "设置"
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Transparent
        ),
        modifier = modifier
    )
}

/**
 * ==================== ProFeature - 专业功能数据类 ====================
 */
data class ProFeature(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val gradient: List<Color>,
    val onClick: () -> Unit
)

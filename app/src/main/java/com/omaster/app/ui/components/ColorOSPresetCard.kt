package com.omaster.app.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.omaster.app.domain.model.Preset
import com.omaster.app.ui.animation.AnimationConfig
import com.omaster.app.ui.theme.*

// ==================== ColorOS 16 专家级预设卡片 ====================
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ColorOSPresetCard(
    preset: Preset,
    onClick: () -> Unit,
    onFavoriteToggle: () -> Unit,
    modifier: Modifier = Modifier,
    showPreview: Boolean = true,
    isNew: Boolean = false
) {
    val hapticFeedback = LocalHapticFeedback.current
    var isPressed by remember { mutableStateOf(false) }
    var showQuickMenu by remember { mutableStateOf(false) }
    
    // ColorOS 16 缩放动画
    val scale by animateFloatAsState(
        targetValue = if (isPressed) AnimationConfig.CARD_PRESS_SCALE else 1f,
        animationSpec = spring(
            dampingRatio = 0.9f,
            stiffness = 400f
        ),
        label = "cardScale"
    )
    
    val alpha by animateFloatAsState(
        targetValue = if (isPressed) AnimationConfig.CARD_PRESS_ALPHA else 1f,
        animationSpec = tween(
            durationMillis = AnimationConfig.MICRO_INTERACTION_DURATION,
            easing = AnimationConfig.ColorOSDefaultEasing
        ),
        label = "cardAlpha"
    )
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .scale(scale)
            .alpha(alpha)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = { 
                        isPressed = true
                        tryAwaitRelease()
                        isPressed = false
                    },
                    onTap = {
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        onClick()
                    },
                    onLongPress = {
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        showQuickMenu = true
                    }
                )
            }
            .semantics {
                contentDescription = "预设：${preset.name}，适合${preset.deviceModel}设备"
                stateDescription = if (preset.isFavorite) "已收藏" else "未收藏"
            },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp,
            pressedElevation = 0.dp
        ),
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
        )
    ) {
        Column {
            // 图片区域 - ColorOS 16 风格
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            ) {
                if (showPreview) {
                    AsyncImage(
                        model = "https://picsum.photos/seed/${preset.coverPath}/600/400",
                        contentDescription = "${preset.name}预览图",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    
                    // ColorOS 16 渐变遮罩
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        Color.Transparent,
                                        Color.Black.copy(alpha = 0.1f),
                                        Color.Black.copy(alpha = 0.6f)
                                    ),
                                    startY = 0f,
                                    endY = Float.POSITIVE_INFINITY
                                )
                            )
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Photo,
                            contentDescription = null,
                            modifier = Modifier.size(72.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
                        )
                    }
                }
                
                // 标签区域 - ColorOS 16 风格
                Row(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (isNew) {
                        ColorOSBreathingTag()
                    }
                    
                    if (preset.cameraParams?.hasselblad_hncs == true) {
                        ColorOSHasselbladTag()
                    }
                    
                    if (preset.source == "community") {
                        ColorOSCommunityTag()
                    }
                }
                
                // 收藏按钮 - ColorOS 16 悬浮风格
                ColorOSFavoriteButton(
                    isFavorite = preset.isFavorite,
                    onToggle = {
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        onFavoriteToggle()
                    },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(14.dp)
                )
            }
            
            // 内容区域 - ColorOS 16 布局
            Column(
                modifier = Modifier
                    .padding(18.dp)
            ) {
                // 标题
                Text(
                    text = preset.name,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = FontWeight.SemiBold
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // 元信息行
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 设备标签
                    preset.deviceModel?.let { deviceModel ->
                        if (deviceModel.isNotEmpty()) {
                            ColorOSChip(
                                text = deviceModel,
                                icon = Icons.Default.PhoneAndroid
                            )
                        }
                    }
                    
                    // 参数速览
                    preset.cameraParams?.let { params ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (params.iso > 0) {
                                ColorOSMiniTag("ISO ${params.iso}")
                            }
                            params.wb?.let { wb ->
                                ColorOSMiniTag(wb)
                            }
                        }
                    }
                }
                
                // 描述摘要
                preset.sections.firstOrNull()?.let { section ->
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = section.content,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
    
    // ColorOS 16 快捷菜单
    if (showQuickMenu) {
        ColorOSQuickMenu(
            preset = preset,
            onDismiss = { showQuickMenu = false },
            onFavoriteToggle = {
                showQuickMenu = false
                onFavoriteToggle()
            },
            onShare = { showQuickMenu = false },
            onViewDetails = {
                showQuickMenu = false
                onClick()
            }
        )
    }
}

// ==================== ColorOS 16 呼吸新标签 ====================
@Composable
private fun ColorOSBreathingTag() {
    val infiniteTransition = rememberInfiniteTransition(label = "breathing")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = AnimationConfig.NEW_TAG_BREATHING_DURATION,
                easing = CubicBezierEasing(0.4f, 0.0f, 0.6f, 1.0f)
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathingAlpha"
    )
    
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = AnimationConfig.NEW_TAG_BREATHING_DURATION,
                easing = CubicBezierEasing(0.4f, 0.0f, 0.6f, 1.0f)
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathingScale"
    )
    
    Surface(
        modifier = Modifier.alpha(alpha).scale(scale),
        color = AccentPrimary,
        shape = RoundedCornerShape(100.dp),
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = Icons.Default.NewReleases,
                contentDescription = null,
                modifier = Modifier.size(13.dp),
                tint = Color.White
            )
            Text(
                text = "NEW",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// ==================== ColorOS 16 哈苏标签 ====================
@Composable
private fun ColorOSHasselbladTag() {
    Surface(
        color = HasselbladOrange,
        shape = RoundedCornerShape(100.dp),
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = null,
                modifier = Modifier.size(13.dp),
                tint = DeepSpace
            )
            Text(
                text = "HNCS",
                style = MaterialTheme.typography.labelSmall,
                color = DeepSpace,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// ==================== ColorOS 16 社区标签 ====================
@Composable
private fun ColorOSCommunityTag() {
    Surface(
        color = AccentSecondary,
        shape = RoundedCornerShape(100.dp),
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = Icons.Default.People,
                contentDescription = null,
                modifier = Modifier.size(13.dp),
                tint = Color.White
            )
            Text(
                text = "社区",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// ==================== ColorOS 16 收藏按钮 ====================
@Composable
private fun ColorOSFavoriteButton(
    isFavorite: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isAnimating by remember { mutableStateOf(false) }
    
    val scale by animateFloatAsState(
        targetValue = if (isAnimating) 1.3f else 1f,
        animationSpec = spring(
            dampingRatio = 0.7f,
            stiffness = 450f
        ),
        label = "favoriteScale"
    )
    
    Surface(
        modifier = modifier
            .scale(scale)
            .size(44.dp),
        shape = CircleShape,
        color = Color.Black.copy(alpha = 0.35f),
        border = BorderStroke(
            1.dp,
            Color.White.copy(alpha = 0.15f)
        )
    ) {
        IconButton(
            onClick = {
                isAnimating = true
                onToggle()
                kotlinx.coroutines.delay(180) {
                    isAnimating = false
                }
            },
            modifier = Modifier.semantics {
                if (isFavorite) {
                    onClick(label = "取消收藏") { onToggle() }
                } else {
                    onClick(label = "添加收藏") { onToggle() }
                }
            }
        ) {
            Icon(
                imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                contentDescription = if (isFavorite) "取消收藏" else "收藏",
                tint = if (isFavorite) AccentPrimary else Color.White,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

// ==================== ColorOS 16 Chip组件 ====================
@Composable
private fun ColorOSChip(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(100.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            icon?.let {
                Icon(
                    imageVector = it,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ==================== ColorOS 16 微型标签 ====================
@Composable
private fun ColorOSMiniTag(text: String) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            fontWeight = FontWeight.Medium
        )
    }
}

// ==================== ColorOS 16 快捷菜单 ====================
@Composable
private fun ColorOSQuickMenu(
    preset: Preset,
    onDismiss: () -> Unit,
    onFavoriteToggle: () -> Unit,
    onShare: () -> Unit,
    onViewDetails: () -> Unit
) {
    var scale by remember { mutableStateOf(0.85f) }
    var alpha by remember { mutableStateOf(0f) }
    
    LaunchedEffect(Unit) {
        scale = 1f
        alpha = 1f
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
            .clickable(onClick = onDismiss)
            .graphicsLayer {
                this.alpha = alpha
            },
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .graphicsLayer {
                    this.scaleX = scale
                    this.scaleY = scale
                }
                .animateContentSize(
                    animationSpec = AnimationConfig.SoftSpringSpec
                ),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
        ) {
            Column {
                // 标题区域
                Column(
                    modifier = Modifier.padding(24.dp)
                ) {
                    Text(
                        text = preset.name,
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "快速操作",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                
                // 操作列表
                ColorOSMenuItem(
                    icon = if (preset.isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                    title = if (preset.isFavorite) "取消收藏" else "快速收藏",
                    tint = if (preset.isFavorite) AccentPrimary else MaterialTheme.colorScheme.onSurface,
                    onClick = onFavoriteToggle
                )
                
                ColorOSMenuItem(
                    icon = Icons.Default.Share,
                    title = "分享预设",
                    onClick = onShare
                )
                
                ColorOSMenuItem(
                    icon = Icons.Default.Visibility,
                    title = "查看详细参数",
                    onClick = onViewDetails
                )
                
                if (preset.source == "official") {
                    Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    ColorOSMenuItem(
                        icon = Icons.Default.Verified,
                        title = "官方认证",
                        tint = AccentPrimary,
                        onClick = {},
                        isEnabled = false
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

// ==================== ColorOS 16 菜单项 ====================
@Composable
private fun ColorOSMenuItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    tint: Color? = null,
    onClick: () -> Unit,
    isEnabled: Boolean = true
) {
    val interactionSource = remember { MutableInteractionSource() }
    var isPressed by remember { mutableStateOf(false) }
    
    val contentColor = when {
        !isEnabled -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        tint != null -> tint
        else -> MaterialTheme.colorScheme.onSurface
    }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = isEnabled,
                onClick = onClick
            )
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        tryAwaitRelease()
                        isPressed = false
                    }
                )
            }
            .padding(horizontal = 24.dp, vertical = 16.dp)
            .background(
                if (isPressed) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                else Color.Transparent,
                RoundedCornerShape(12.dp)
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            modifier = Modifier.size(24.dp),
            tint = contentColor
        )
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = contentColor
        )
    }
}

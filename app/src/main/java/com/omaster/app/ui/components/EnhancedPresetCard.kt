package com.omaster.app.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
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

@OptIn(ExperimentalComposeUiApi::class, ExperimentalFoundationApi::class)
@Composable
fun EnhancedPresetCard(
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
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = tween(
            durationMillis = AnimationConfig.MICRO_INTERACTION_DURATION,
            easing = AnimationConfig.FastOutSlowInEasing
        ),
        label = "cardScale"
    )
    
    val alpha by animateFloatAsState(
        targetValue = if (isPressed) 0.9f else 1f,
        animationSpec = tween(
            durationMillis = AnimationConfig.MICRO_INTERACTION_DURATION,
            easing = AnimationConfig.FastOutSlowInEasing
        ),
        label = "cardAlpha"
    )
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .scale(scale)
            .alpha(alpha)
            .combinedClickable(
                onClick = {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                    onClick()
                },
                onLongClick = {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                    showQuickMenu = true
                },
                onChange = { pressed ->
                    isPressed = pressed
                },
                onClickLabel = "查看预设详情",
                onLongClickLabel = "打开快捷菜单"
            )
            .semantics {
                contentDescription = "预设：${preset.name}，适合${preset.deviceModel}设备"
                stateDescription = if (preset.isFavorite) "已收藏" else "未收藏"
            },
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isPressed) 2.dp else 4.dp
        ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Box {
            Column {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                ) {
                    if (showPreview) {
                        AsyncImage(
                            model = "https://picsum.photos/seed/${preset.coverPath}/600/400",
                            contentDescription = "${preset.name}预览图",
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
                                            Color.Black.copy(alpha = 0.7f)
                                        )
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
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                            )
                        }
                    }
                    
                    if (preset.cameraParams?.hasselblad_hncs == true) {
                        Surface(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(12.dp),
                            color = HasselbladOrange,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = null,
                                    modifier = Modifier.size(12.dp),
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
                    
                    if (isNew) {
                        BreathingNewTag(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(12.dp)
                        )
                    } else if (preset.source == "community") {
                        Surface(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(12.dp),
                            color = AccentSecondary,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "社区",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    
                    FavoriteButton(
                        isFavorite = preset.isFavorite,
                        onToggle = {
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                            onFavoriteToggle()
                        },
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(8.dp)
                    )
                }
                
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = preset.name,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        fontWeight = FontWeight.SemiBold
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        preset.deviceModel?.let { deviceModel ->
                            if (deviceModel.isNotEmpty()) {
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(
                                        text = deviceModel,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                        
                        preset.cameraParams?.let { params ->
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (params.iso > 0) {
                                    Text(
                                        text = "ISO ${params.iso}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                params.wb?.let { wb ->
                                    Text(
                                        text = wb,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                    
                    preset.sections.firstOrNull()?.let { section ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = section.content,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
            
            AnimatedVisibility(
                visible = showQuickMenu,
                enter = scaleIn(
                    animationSpec = spring(
                        dampingRatio = 0.8f,
                        stiffness = 500f
                    )
                ),
                exit = scaleOut(
                    animationSpec = spring(
                        dampingRatio = 0.8f,
                        stiffness = 500f
                    )
                )
            ) {
                DropdownMenu(
                    expanded = showQuickMenu,
                    onDismissRequest = { showQuickMenu = false }
                ) {
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (preset.isFavorite) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(if (preset.isFavorite) "取消收藏" else "快速收藏")
                            }
                        },
                        onClick = {
                            showQuickMenu = false
                            onFavoriteToggle()
                        },
                        modifier = Modifier.semantics {
                            contentDescription = if (preset.isFavorite) "快速取消收藏" else "快速收藏"
                        }
                    )
                    
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text("分享预设")
                            }
                        },
                        onClick = {
                            showQuickMenu = false
                        },
                        modifier = Modifier.semantics {
                            contentDescription = "分享预设"
                        }
                    )
                    
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Visibility,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text("查看参数")
                            }
                        },
                        onClick = {
                            showQuickMenu = false
                            onClick()
                        },
                        modifier = Modifier.semantics {
                            contentDescription = "查看详细参数"
                        }
                    )
                    
                    if (preset.source == "official") {
                        HorizontalDivider()
                        
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.NewReleases,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp),
                                        tint = AccentPrimary
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text("官方认证", color = AccentPrimary)
                                }
                            },
                            onClick = {
                                showQuickMenu = false
                            },
                            enabled = false
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FavoriteButton(
    isFavorite: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isAnimating by remember { mutableStateOf(false) }
    
    val scale by animateFloatAsState(
        targetValue = if (isAnimating) 1.3f else 1f,
        animationSpec = spring(
            dampingRatio = 0.6f,
            stiffness = 500f
        ),
        label = "favoriteScale"
    )
    
    IconButton(
        onClick = {
            isAnimating = true
            onToggle()
            kotlinx.coroutines.delay(200) {
                isAnimating = false
            }
        },
        modifier = modifier
            .scale(scale)
            .semantics {
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
            tint = if (isFavorite) AccentPrimary else Color.White
        )
    }
}

@Composable
private fun BreathingNewTag(
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "newTag")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = AnimationConfig.NEW_TAG_BREATHING_DURATION,
                easing = CubicBezierEasing(0.5f, 0.0f, 0.5f, 1.0f)
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "newTagAlpha"
    )
    
    Surface(
        modifier = modifier.alpha(alpha),
        color = AccentPrimary,
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = Icons.Default.NewReleases,
                contentDescription = null,
                modifier = Modifier.size(12.dp),
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

private suspend fun delay(timeMillis: Long, block: () -> Unit) {
    kotlinx.coroutines.delay(timeMillis)
    block()
}

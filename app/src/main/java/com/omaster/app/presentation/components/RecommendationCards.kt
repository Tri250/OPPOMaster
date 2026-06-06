package com.omaster.app.presentation.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.omaster.app.domain.model.Recommendation
import com.omaster.app.domain.model.RecommendationType
import com.omaster.app.domain.model.TrendingItem
import com.omaster.app.presentation.theme.*

/**
 * 推荐卡片组件
 * 包含带推荐理由的预设卡片、相似度百分比显示、趋势标识等
 */

/**
 * 推荐预设卡片
 * 带推荐理由和相似度显示的增强版预设卡片
 *
 * @param recommendation 推荐项
 * @param onClick 点击回调
 * @param onFavoriteToggle 收藏切换回调
 * @param modifier 修饰符
 * @param showReason 是否显示推荐理由
 * @param showSimilarity 是否显示相似度
 */
@Composable
fun RecommendationCard(
    recommendation: Recommendation,
    onClick: () -> Unit,
    onFavoriteToggle: () -> Unit,
    modifier: Modifier = Modifier,
    showReason: Boolean = true,
    showSimilarity: Boolean = true
) {
    val hapticFeedback = LocalHapticFeedback.current
    var isPressed by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = tween(durationMillis = 150),
        label = "cardScale"
    )

    Card(
        modifier = modifier
            .width(280.dp)
            .scale(scale)
            .clickable {
                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                onClick()
            },
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isPressed) 2.dp else 6.dp
        ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column {
            // 封面图区域
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
            ) {
                AsyncImage(
                    model = "https://picsum.photos/seed/${recommendation.preset.coverPath}/600/400",
                    contentDescription = recommendation.preset.name,
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
                                    Color.Black.copy(alpha = 0.6f)
                                ),
                                startY = 80f
                            )
                        )
                )

                // 推荐类型标识
                RecommendationTypeBadge(
                    type = recommendation.recommendationType,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(12.dp)
                )

                // 相似度显示
                if (showSimilarity && recommendation.similarityScore > 0.3f) {
                    SimilarityBadge(
                        similarity = recommendation.similarityScore,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(12.dp)
                    )
                }

                // 趋势排名
                if (recommendation.trendingRank in 1..3) {
                    TrendingRankBadge(
                        rank = recommendation.trendingRank,
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 12.dp)
                    )
                }

                // 收藏按钮
                FavoriteButton(
                    isFavorite = recommendation.preset.isFavorite,
                    onToggle = {
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        onFavoriteToggle()
                    },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp)
                )

                // HNCS 认证标识
                if (recommendation.preset.isHncsCertified) {
                    HncsBadge(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(12.dp)
                    )
                }
            }

            // 内容区域
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                // 预设名称
                Text(
                    text = recommendation.preset.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(4.dp))

                // 设备型号
                Text(
                    text = recommendation.preset.getDeviceDisplay(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // 推荐理由
                if (showReason) {
                    Spacer(modifier = Modifier.height(8.dp))

                    RecommendationReason(
                        reason = recommendation.getFormattedReason(),
                        icon = recommendation.getTypeIcon()
                    )
                }

                // 标签
                if (recommendation.preset.tags.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        recommendation.preset.tags.take(3).forEach { tag ->
                            TagChip(tag = tag)
                        }
                    }
                }
            }
        }
    }
}

/**
 * 紧凑版推荐卡片
 * 用于横向滚动列表
 */
@Composable
fun CompactRecommendationCard(
    recommendation: Recommendation,
    onClick: () -> Unit,
    onFavoriteToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val hapticFeedback = LocalHapticFeedback.current

    Card(
        modifier = modifier
            .width(200.dp)
            .clickable {
                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                onClick()
            },
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
            ) {
                AsyncImage(
                    model = "https://picsum.photos/seed/${recommendation.preset.coverPath}/400/300",
                    contentDescription = recommendation.preset.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                // 相似度指示器
                if (recommendation.similarityScore > 0.5f) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .background(
                                color = HasselbladOrange.copy(alpha = 0.9f),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = recommendation.getSimilarityPercentage(),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.Black,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // 趋势标识
                if (recommendation.recommendationType == RecommendationType.TRENDING) {
                    TrendingIndicator(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(8.dp)
                    )
                }
            }

            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                Text(
                    text = recommendation.preset.name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = recommendation.reason,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * 趋势卡片
 * 专门用于展示热门趋势预设
 */
@Composable
fun TrendingCard(
    trendingItem: TrendingItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val hapticFeedback = LocalHapticFeedback.current

    Card(
        modifier = modifier
            .width(160.dp)
            .clickable {
                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                onClick()
            },
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
            ) {
                AsyncImage(
                    model = "https://picsum.photos/seed/${trendingItem.presetId}/400/300",
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                // 排名标识
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                        .background(
                            color = when (trendingItem.rank) {
                                1 -> Color(0xFFFFD700) // 金色
                                2 -> Color(0xFFC0C0C0) // 银色
                                3 -> Color(0xFFCD7F32) // 铜色
                                else -> Color.Black.copy(alpha = 0.6f)
                            },
                            shape = CircleShape
                        )
                        .size(28.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${trendingItem.rank}",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (trendingItem.rank <= 3) Color.Black else Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }

                // 趋势图标
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                ) {
                    Text(
                        text = trendingItem.getTrendIcon(),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                trendingItem.preset?.let { preset ->
                    Text(
                        text = preset.name,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Visibility,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${trendingItem.viewCount}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (trendingItem.growthRate != 0f) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = trendingItem.getGrowthRateDisplay(),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (trendingItem.isRising()) SuccessPro else ErrorPro,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

/**
 * 推荐理由组件
 */
@Composable
private fun RecommendationReason(
    reason: String,
    icon: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(8.dp)
            )
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = icon,
            style = MaterialTheme.typography.bodySmall
        )
        Text(
            text = reason,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * 推荐类型徽章
 */
@Composable
private fun RecommendationTypeBadge(
    type: RecommendationType,
    modifier: Modifier = Modifier
) {
    val (backgroundColor, textColor, label) = when (type) {
        RecommendationType.COLLABORATIVE_FILTERING ->
            Triple(CosmicPurple.copy(alpha = 0.9f), Color.White, "相似推荐")
        RecommendationType.CONTENT_BASED ->
            Triple(AuroraGreen.copy(alpha = 0.9f), Color.White, "个性推荐")
        RecommendationType.TRENDING ->
            Triple(SunsetRed.copy(alpha = 0.9f), Color.White, "热门")
        RecommendationType.NEW_USER ->
            Triple(InfoPro.copy(alpha = 0.9f), Color.White, "精选")
        RecommendationType.SEASONAL ->
            Triple(WarningPro.copy(alpha = 0.9f), Color.Black, "时令")
        RecommendationType.POPULAR ->
            Triple(HasselbladOrange.copy(alpha = 0.9f), Color.Black, "人气")
    }

    Surface(
        modifier = modifier,
        color = backgroundColor,
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = textColor,
            fontWeight = FontWeight.Bold
        )
    }
}

/**
 * 相似度徽章
 */
@Composable
private fun SimilarityBadge(
    similarity: Float,
    modifier: Modifier = Modifier
) {
    val percentage = (similarity * 100).toInt()
    val color = when {
        percentage >= 80 -> SuccessPro
        percentage >= 60 -> WarningPro
        else -> InfoPro
    }

    Surface(
        modifier = modifier,
        color = color.copy(alpha = 0.9f),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = Icons.Default.ThumbUp,
                contentDescription = null,
                modifier = Modifier.size(12.dp),
                tint = Color.White
            )
            Text(
                text = "$percentage%",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/**
 * 趋势排名徽章
 */
@Composable
private fun TrendingRankBadge(
    rank: Int,
    modifier: Modifier = Modifier
) {
    val (icon, color) = when (rank) {
        1 -> Triple("🥇", Color(0xFFFFD700))
        2 -> Triple("🥈", Color(0xFFC0C0C0))
        3 -> Triple("🥉", Color(0xFFCD7F32))
        else -> Triple("", Color.Gray)
    }

    if (rank <= 3) {
        Surface(
            modifier = modifier,
            color = color.copy(alpha = 0.95f),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(
                text = icon,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

/**
 * 趋势指示器
 */
@Composable
private fun TrendingIndicator(
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "trending")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "trendingScale"
    )

    Box(
        modifier = modifier
            .background(
                color = SunsetRed.copy(alpha = 0.9f),
                shape = RoundedCornerShape(8.dp)
            )
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = "🔥",
            modifier = Modifier.scale(scale),
            style = MaterialTheme.typography.bodySmall
        )
    }
}

/**
 * HNCS 认证徽章
 */
@Composable
private fun HncsBadge(
    modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
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
                tint = Color.Black
            )
            Text(
                text = "HNCS",
                style = MaterialTheme.typography.labelSmall,
                color = Color.Black,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/**
 * 收藏按钮
 */
@Composable
private fun FavoriteButton(
    isFavorite: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isAnimating by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (isAnimating) 1.3f else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 500f),
        label = "favoriteScale"
    )

    IconButton(
        onClick = {
            isAnimating = true
            onToggle()
        },
        modifier = modifier.scale(scale)
    ) {
        Icon(
            imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
            contentDescription = if (isFavorite) "取消收藏" else "收藏",
            tint = if (isFavorite) SunsetRed else Color.White
        )
    }

    LaunchedEffect(isAnimating) {
        if (isAnimating) {
            kotlinx.coroutines.delay(200)
            isAnimating = false
        }
    }
}

/**
 * 标签芯片
 */
@Composable
private fun TagChip(
    tag: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(6.dp)
    ) {
        Text(
            text = tag,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * 推荐板块标题
 */
@Composable
fun RecommendationSectionHeader(
    title: String,
    subtitle: String,
    icon: String,
    modifier: Modifier = Modifier,
    onMoreClick: (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = icon,
                style = MaterialTheme.typography.titleMedium
            )
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        onMoreClick?.let {
            TextButton(onClick = it) {
                Text("查看更多")
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

/**
 * 横向推荐列表
 */
@Composable
fun HorizontalRecommendationList(
    recommendations: List<Recommendation>,
    onItemClick: (Recommendation) -> Unit,
    onFavoriteToggle: (Recommendation) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp)
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(androidx.compose.foundation.rememberScrollState())
            .padding(contentPadding),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        recommendations.forEach { recommendation ->
            RecommendationCard(
                recommendation = recommendation,
                onClick = { onItemClick(recommendation) },
                onFavoriteToggle = { onFavoriteToggle(recommendation) },
                showReason = false,
                showSimilarity = true
            )
        }
    }
}

/**
 * 紧凑横向推荐列表
 */
@Composable
fun CompactHorizontalRecommendationList(
    recommendations: List<Recommendation>,
    onItemClick: (Recommendation) -> Unit,
    onFavoriteToggle: (Recommendation) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp)
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(androidx.compose.foundation.rememberScrollState())
            .padding(contentPadding),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        recommendations.forEach { recommendation ->
            CompactRecommendationCard(
                recommendation = recommendation,
                onClick = { onItemClick(recommendation) },
                onFavoriteToggle = { onFavoriteToggle(recommendation) }
            )
        }
    }
}

/**
 * 趋势横向列表
 */
@Composable
fun TrendingHorizontalList(
    trendingItems: List<TrendingItem>,
    onItemClick: (TrendingItem) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp)
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(androidx.compose.foundation.rememberScrollState())
            .padding(contentPadding),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        trendingItems.forEach { item ->
            TrendingCard(
                trendingItem = item,
                onClick = { onItemClick(item) }
            )
        }
    }
}

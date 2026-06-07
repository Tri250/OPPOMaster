package com.omaster.app.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.omaster.app.ui.theme.WebAnimations
import com.omaster.app.ui.theme.WebColors
import com.omaster.app.ui.theme.WebRadius
import com.omaster.app.ui.theme.WebSpacing
import com.omaster.app.ui.theme.WebTypography

// ==================== Web风格卡片组件 ====================

@Composable
fun WebCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = when {
            isPressed -> 0.98f
            isHovered -> 1.02f
            else -> 1f
        },
        animationSpec = tween(WebAnimations.DurationNormal)
    )

    val borderColor by animateColorAsState(
        targetValue = if (isHovered) WebColors.CardBorderHover else WebColors.CardBorder,
        animationSpec = tween(WebAnimations.DurationNormal)
    )

    Column(
        modifier = modifier
            .scale(scale)
            .clip(RoundedCornerShape(WebRadius.xl2))
            .background(WebColors.CardBackground)
            .border(1.dp, borderColor, RoundedCornerShape(WebRadius.xl2))
            .then(if (onClick != null) {
                Modifier.clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick
                )
            } else Modifier)
            .padding(WebSpacing.CardPadding),
        content = content
    )
}

@Composable
fun WebFeatureCard(
    icon: ImageVector,
    title: String,
    description: String,
    features: List<String>,
    modifier: Modifier = Modifier,
    index: Int = 0
) {
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(index * WebAnimations.StaggerDelay.toLong())
        visible = true
    }

    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(WebAnimations.DurationNormal, easing = WebAnimations.EaseOut)
    )

    val offsetY by animateFloatAsState(
        targetValue = if (visible) 0f else 30f,
        animationSpec = tween(WebAnimations.DurationNormal, easing = WebAnimations.EaseOut)
    )

    WebCard(modifier = modifier.alpha(alpha).offset(y = offsetY.dp)) {
        // 图标
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(WebRadius.xl))
                .background(
                    Brush.linearGradient(WebColors.GradientOrange)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(modifier = Modifier.height(WebSpacing.base))

        // 标题
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold,
                color = WebColors.TextPrimary
            )
        )

        Spacer(modifier = Modifier.height(WebSpacing.xs))

        // 描述
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium.copy(
                color = WebColors.TextSecondary,
                lineHeight = WebTypography.LeadingRelaxed
            )
        )

        Spacer(modifier = Modifier.height(WebSpacing.base))

        // 功能标签
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(WebSpacing.sm),
            verticalArrangement = Arrangement.spacedBy(WebSpacing.sm)
        ) {
            features.forEach { feature ->
                WebTag(text = feature)
            }
        }
    }
}

@Composable
fun WebPresetCard(
    name: String,
    author: String,
    deviceModel: String,
    description: String,
    rating: Float,
    downloadCount: String,
    tags: List<String>,
    iso: String,
    shutter: String,
    aperture: String,
    isHncsCertified: Boolean = false,
    modifier: Modifier = Modifier,
    index: Int = 0,
    onClick: () -> Unit = {}
) {
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(index * WebAnimations.StaggerDelay.toLong())
        visible = true
    }

    val scale by animateFloatAsState(
        targetValue = if (visible) 1f else 0.9f,
        animationSpec = tween(WebAnimations.DurationNormal, easing = WebAnimations.EaseOut)
    )

    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(WebAnimations.DurationNormal, easing = WebAnimations.EaseOut)
    )

    WebCard(
        modifier = modifier
            .alpha(alpha)
            .scale(scale),
        onClick = onClick
    ) {
        // 封面区域
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .clip(RoundedCornerShape(WebRadius.xl))
                .background(
                    Brush.verticalGradient(
                        listOf(WebColors.Zinc700, WebColors.Zinc800)
                    )
                )
        ) {
            // 渐变遮罩
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                WebColors.Zinc900.copy(alpha = 0.8f)
                            )
                        )
                    )
            )

            // HNCS徽章
            if (isHncsCertified) {
                WebBadge(
                    text = "HNCS",
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(WebSpacing.sm),
                    color = WebColors.AccentPrimary
                )
            }

            // 评分
            Row(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(WebSpacing.sm),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    tint = Color(0xFFFFD700),
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = rating.toString(),
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = Color.White,
                        fontWeight = FontWeight.Medium
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(WebSpacing.base))

        // 内容
        Text(
            text = name,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                color = WebColors.TextPrimary
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Text(
            text = "$author · $deviceModel",
            style = MaterialTheme.typography.bodySmall.copy(
                color = WebColors.TextTertiary
            )
        )

        Spacer(modifier = Modifier.height(WebSpacing.xs))

        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium.copy(
                color = WebColors.TextSecondary
            ),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(WebSpacing.base))

        // 标签
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(WebSpacing.xs),
            verticalArrangement = Arrangement.spacedBy(WebSpacing.xs)
        ) {
            tags.take(3).forEach { tag ->
                WebTag(text = tag, small = true)
            }
        }

        Spacer(modifier = Modifier.height(WebSpacing.base))

        // 参数预览
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "ISO $iso",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = WebColors.TextTertiary
                )
            )
            Text(
                text = shutter,
                style = MaterialTheme.typography.labelSmall.copy(
                    color = WebColors.TextTertiary
                )
            )
            Text(
                text = aperture,
                style = MaterialTheme.typography.labelSmall.copy(
                    color = WebColors.TextTertiary
                )
            )
        }

        Spacer(modifier = Modifier.height(WebSpacing.sm))

        // 下载量
        Text(
            text = "$downloadCount 次下载",
            style = MaterialTheme.typography.labelSmall.copy(
                color = WebColors.TextTertiary
            )
        )
    }
}

// ==================== Web风格基础组件 ====================

@Composable
fun WebTag(
    text: String,
    small: Boolean = false,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(WebRadius.md))
            .background(WebColors.TagBackground)
            .padding(
                horizontal = if (small) WebSpacing.sm else WebSpacing.base,
                vertical = if (small) 4.dp else WebSpacing.xs
            )
    ) {
        Text(
            text = text,
            style = if (small) {
                MaterialTheme.typography.labelSmall.copy(color = WebColors.TagText)
            } else {
                MaterialTheme.typography.bodySmall.copy(color = WebColors.TagText)
            }
        )
    }
}

@Composable
fun WebBadge(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = WebColors.AccentPrimary
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(WebRadius.md))
            .background(color.copy(alpha = 0.9f))
            .padding(horizontal = WebSpacing.sm, vertical = 4.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(
                color = Color.White,
                fontWeight = FontWeight.Medium
            )
        )
    }
}

@Composable
fun WebButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    variant: WebButtonVariant = WebButtonVariant.Primary,
    enabled: Boolean = true
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val isHovered by interactionSource.collectIsHoveredAsState()

    val scale by animateFloatAsState(
        targetValue = when {
            isPressed -> 0.95f
            isHovered -> 1.05f
            else -> 1f
        },
        animationSpec = tween(WebAnimations.DurationFast)
    )

    val backgroundColor = when (variant) {
        WebButtonVariant.Primary -> Brush.linearGradient(WebColors.GradientOrange)
        WebButtonVariant.Secondary -> Brush.linearGradient(
            listOf(WebColors.Zinc800, WebColors.Zinc700)
        )
        WebButtonVariant.Ghost -> Brush.linearGradient(listOf(Color.Transparent, Color.Transparent))
    }

    val contentColor = when (variant) {
        WebButtonVariant.Primary -> Color.White
        WebButtonVariant.Secondary -> WebColors.TextPrimary
        WebButtonVariant.Ghost -> WebColors.TextPrimary
    }

    Box(
        modifier = modifier
            .scale(scale)
            .clip(RoundedCornerShape(WebRadius.xl))
            .background(backgroundColor)
            .then(
                if (variant == WebButtonVariant.Ghost) {
                    Modifier.border(1.dp, WebColors.Zinc700, RoundedCornerShape(WebRadius.xl))
                } else Modifier
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick
            )
            .padding(horizontal = WebSpacing.xl, vertical = WebSpacing.base),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(WebSpacing.sm)
        ) {
            icon?.let {
                Icon(
                    imageVector = it,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(20.dp)
                )
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge.copy(
                    color = contentColor,
                    fontWeight = FontWeight.SemiBold
                )
            )
        }
    }
}

enum class WebButtonVariant {
    Primary, Secondary, Ghost
}

@Composable
fun WebSectionTitle(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold,
                color = WebColors.TextPrimary
            )
        )
        subtitle?.let {
            Spacer(modifier = Modifier.height(WebSpacing.sm))
            Text(
                text = it,
                style = MaterialTheme.typography.bodyLarge.copy(
                    color = WebColors.TextSecondary
                )
            )
        }
    }
}

// ==================== FlowRow 实现 ====================

@Composable
fun FlowRow(
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    content: @Composable () -> Unit
) {
    Layout(
        content = content,
        modifier = modifier
    ) { measurables, constraints ->
        val hGapPx = horizontalArrangement.spacing.roundToPx()
        val vGapPx = verticalArrangement.spacing.roundToPx()

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

        val width = constraints.maxWidth
        val height = rowHeights.sum() + (rows.size - 1).coerceAtLeast(0) * vGapPx

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

package com.omaster.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.omaster.app.ui.animation.ColorOSAnimationDuration
import com.omaster.app.ui.animation.ColorOSEasing
import com.omaster.app.ui.animation.ColorOSScale
import com.omaster.app.ui.theme.*
import com.omaster.app.viewmodel.FilterType

@Composable
fun GlassFilterChips(
    selectedFilter: FilterType,
    onFilterSelected: (FilterType) -> Unit,
    modifier: Modifier = Modifier,
    filters: List<Pair<FilterType, String>> = defaultFilters
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = Spacing.ScreenPadding, vertical = Spacing.sm),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
    ) {
        filters.forEachIndexed { index, (key, label) ->
            GlassFilterChip(
                text = label,
                selected = selectedFilter == key,
                onClick = { onFilterSelected(key) },
                index = index
            )
        }
    }
}

private val defaultFilters = listOf(
    FilterType.ALL to "全部",
    FilterType.FAVORITES to "收藏",
    FilterType.HNCS to "HNCS",
    FilterType.FIND_X to "Find X",
    FilterType.RENO to "Reno",
    FilterType.NEW to "最新",
    FilterType.TRENDING to "热门"
)

@Composable
fun GlassFilterChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    index: Int = 0,
    icon: @Composable (() -> Unit)? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    var isVisible by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(index * 50L)
        isVisible = true
    }
    
    val animatedAlpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(
            durationMillis = ColorOSAnimationDuration.FAST,
            easing = ColorOSEasing.Decelerate
        ),
        label = "alpha"
    )
    
    val animatedOffsetX by animateFloatAsState(
        targetValue = if (isVisible) 0f else (-20).dp.value,
        animationSpec = tween(
            durationMillis = ColorOSAnimationDuration.FAST,
            easing = ColorOSEasing.Decelerate
        ),
        label = "offsetX"
    )
    
    val scale by animateFloatAsState(
        targetValue = when {
            isPressed -> ColorOSScale.Pressed
            selected -> 1.05f
            else -> 1f
        },
        animationSpec = spring(
            dampingRatio = 0.8f,
            stiffness = 400f
        ),
        label = "scale"
    )
    
    val backgroundColor by animateColorAsState(
        targetValue = if (selected) {
            Colors.HasselbladOrange.copy(alpha = 0.25f)
        } else {
            Colors.GlassBackground.copy(alpha = 0.2f)
        },
        animationSpec = tween(200),
        label = "backgroundColor"
    )
    
    val borderColor by animateColorAsState(
        targetValue = if (selected) {
            Colors.HasselbladOrange.copy(alpha = 0.5f)
        } else {
            Colors.GlassBorder.copy(alpha = 0.3f)
        },
        animationSpec = tween(200),
        label = "borderColor"
    )
    
    val textColor by animateColorAsState(
        targetValue = if (selected) {
            Colors.HasselbladOrange
        } else {
            Colors.OnSurfaceVariant
        },
        animationSpec = tween(200),
        label = "textColor"
    )
    
    Box(
        modifier = modifier
            .scale(scale)
            .graphicsLayer {
                alpha = animatedAlpha
                translationX = animatedOffsetX
            }
            .clip(RoundedCornerShape(Radius.Chip))
            .background(backgroundColor)
            .border(
                width = 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(Radius.Chip)
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = Spacing.md, vertical = Spacing.sm)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
        ) {
            icon?.invoke()
            Text(
                text = text,
                style = Typography.LabelMedium,
                color = textColor,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
            )
        }
    }
}

@Composable
fun GlassFilterChipGroup(
    filters: List<Pair<String, String>>,
    selectedFilter: String,
    onFilterSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
        ) {
            filters.forEach { (key, label) ->
                GlassFilterChip(
                    text = label,
                    selected = selectedFilter == key,
                    onClick = { onFilterSelected(key) }
                )
            }
        }
    }
}

@Composable
fun AnimatedFilterChipRow(
    filters: List<Pair<String, String>>,
    selectedFilter: String,
    onFilterSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = Spacing.ScreenPadding, vertical = Spacing.sm),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
    ) {
        filters.forEachIndexed { index, (key, label) ->
            val isSelected = selectedFilter == key
            
            AnimatedFilterChip(
                text = label,
                selected = isSelected,
                onClick = { onFilterSelected(key) },
                index = index
            )
        }
    }
}

@Composable
private fun AnimatedFilterChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    index: Int
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(index * 40L)
    }
    
    val scale by animateFloatAsState(
        targetValue = when {
            isPressed -> ColorOSScale.Pressed
            selected -> 1.05f
            else -> 1f
        },
        animationSpec = spring(
            dampingRatio = 0.8f,
            stiffness = 400f
        ),
        label = "scale"
    )
    
    Box(
        modifier = Modifier
            .scale(scale)
            .then(
                GlassChip(
                    text = text,
                    selected = selected,
                    onClick = onClick
                )
            )
    )
}

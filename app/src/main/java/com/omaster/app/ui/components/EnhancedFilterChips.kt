package com.omaster.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.omaster.app.ui.animation.AnimationConfig
import com.omaster.app.viewmodel.FilterType
import com.omaster.app.viewmodel.SceneType
import com.omaster.app.viewmodel.SortType
import com.omaster.app.viewmodel.StyleType

@Composable
fun EnhancedFilterChips(
    selectedFilter: FilterType,
    onFilterSelected: (FilterType) -> Unit,
    modifier: Modifier = Modifier
) {
    val filters = remember {
        listOf(
            FilterInfo(FilterType.ALL, "全部", Icons.Default.Apps),
            FilterInfo(FilterType.FAVORITES, "收藏", Icons.Default.Favorite),
            FilterInfo(FilterType.HNCS, "HNCS", Icons.Default.Star),
            FilterInfo(FilterType.FIND_X, "Find X", Icons.Default.PhoneAndroid),
            FilterInfo(FilterType.RENO, "Reno", Icons.Default.PhoneIphone),
            FilterInfo(FilterType.NEW, "最新", Icons.Default.NewReleases),
            FilterInfo(FilterType.TRENDING, "热门", Icons.Default.TrendingUp)
        )
    }
    
    Row(
        modifier = modifier
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        filters.forEach { filterInfo ->
            val isSelected = selectedFilter == filterInfo.type
            
            ColorOSFilterChip(
                selected = isSelected,
                onClick = { onFilterSelected(filterInfo.type) },
                label = filterInfo.label,
                icon = filterInfo.icon
            )
        }
    }
}

@Composable
private fun ColorOSFilterChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val scale by animateFloatAsState(
        targetValue = when {
            isPressed -> 0.92f
            selected -> 1.02f
            else -> 1f
        },
        animationSpec = spring(
            dampingRatio = 0.7f,
            stiffness = 400f
        ),
        label = "chipScale"
    )
    
    val animatedContainerColor by animateColorAsState(
        targetValue = when {
            selected -> MaterialTheme.colorScheme.primary
            else -> MaterialTheme.colorScheme.surfaceVariant
        },
        animationSpec = tween(
            durationMillis = AnimationConfig.SMALL_TRANSITION_DURATION,
            easing = AnimationConfig.DecelerateEasing
        ),
        label = "chipContainerColor"
    )
    
    val animatedContentColor by animateColorAsState(
        targetValue = when {
            selected -> Color.White
            else -> MaterialTheme.colorScheme.onSurfaceVariant
        },
        animationSpec = tween(
            durationMillis = AnimationConfig.SMALL_TRANSITION_DURATION,
            easing = AnimationConfig.DecelerateEasing
        ),
        label = "chipContentColor"
    )
    
    Surface(
        modifier = Modifier
            .scale(scale)
            .height(36.dp),
        shape = RoundedCornerShape(18.dp),
        color = animatedContainerColor,
        onClick = onClick,
        interactionSource = interactionSource
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = animatedContentColor
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = animatedContentColor
            )
        }
    }
}

@Composable
fun SceneFilterChips(
    selectedScene: SceneType,
    onSceneSelected: (SceneType) -> Unit,
    modifier: Modifier = Modifier
) {
    val scenes = remember {
        listOf(
            SceneInfo(SceneType.ALL, "全部", Icons.Default.Grid3x3),
            SceneInfo(SceneType.PORTRAIT, "人像", Icons.Default.Person),
            SceneInfo(SceneType.LANDSCAPE, "风景", Icons.Default.Mountain),
            SceneInfo(SceneType.NIGHT, "夜景", Icons.Default.Nightlight),
            SceneInfo(SceneType.FOOD, "美食", Icons.Default.Restaurant),
            SceneInfo(SceneType.STREET, "街拍", Icons.Default.City),
            SceneInfo(SceneType.MACRO, "微距", Icons.Default.Search),
            SceneInfo(SceneType.ARCHITECTURE, "建筑", Icons.Default.Building)
        )
    }
    
    Row(
        modifier = modifier
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        scenes.forEach { sceneInfo ->
            val isSelected = selectedScene == sceneInfo.type
            
            ColorOSAssistChip(
                selected = isSelected,
                onClick = { onSceneSelected(sceneInfo.type) },
                label = sceneInfo.label,
                icon = sceneInfo.icon
            )
        }
    }
}

@Composable
private fun ColorOSAssistChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val scale by animateFloatAsState(
        targetValue = when {
            isPressed -> 0.95f
            selected -> 1.02f
            else -> 1f
        },
        animationSpec = spring(
            dampingRatio = 0.7f,
            stiffness = 400f
        ),
        label = "sceneChipScale"
    )
    
    val animatedColor by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.secondaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        },
        animationSpec = tween(
            durationMillis = AnimationConfig.SMALL_TRANSITION_DURATION,
            easing = AnimationConfig.DecelerateEasing
        ),
        label = "sceneChipColor"
    )
    
    val animatedContentColor by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.onSecondaryContainer
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        animationSpec = tween(
            durationMillis = AnimationConfig.SMALL_TRANSITION_DURATION,
            easing = AnimationConfig.DecelerateEasing
        ),
        label = "sceneChipContentColor"
    )
    
    Surface(
        modifier = Modifier
            .scale(scale)
            .height(32.dp),
        shape = RoundedCornerShape(16.dp),
        color = animatedColor,
        onClick = onClick,
        interactionSource = interactionSource
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = animatedContentColor
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = animatedContentColor
            )
        }
    }
}

@Composable
fun StyleFilterChips(
    selectedStyle: StyleType,
    onStyleSelected: (StyleType) -> Unit,
    modifier: Modifier = Modifier
) {
    val styles = remember {
        listOf(
            StyleInfo(StyleType.ALL, "全部", Icons.Default.Palette),
            StyleInfo(StyleType.FILM, "胶片", Icons.Default.Photo),
            StyleInfo(StyleType.RETRO, "复古", Icons.Default.History),
            StyleInfo(StyleType.FRESH, "清新", Icons.Default.Sprout),
            StyleInfo(StyleType.VIBRANT, "鲜艳", Icons.Default.BrightnessHigh),
            StyleInfo(StyleType.BLACK_WHITE, "黑白", Icons.Default.BlackWhite),
            StyleInfo(StyleType.NATURAL, "自然", Icons.Default.Leaf),
            StyleInfo(StyleType.WARM, "暖调", Icons.Default.Sunny)
        )
    }
    
    Row(
        modifier = modifier
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        styles.forEach { styleInfo ->
            val isSelected = selectedStyle == styleInfo.type
            
            ColorOSAssistChip(
                selected = isSelected,
                onClick = { onStyleSelected(styleInfo.type) },
                label = styleInfo.label,
                icon = styleInfo.icon
            )
        }
    }
}

@Composable
fun SortSelector(
    selectedSort: SortType,
    onSortSelected: (SortType) -> Unit,
    modifier: Modifier = Modifier
) {
    val sorts = remember {
        listOf(
            SortInfo(SortType.HOT, "热门", Icons.Default.TrendingUp),
            SortInfo(SortType.FAVORITE, "收藏", Icons.Default.Favorite),
            SortInfo(SortType.NEWEST, "最新", Icons.Default.NewReleases)
        )
    }
    
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "排序:",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(end = 8.dp)
        )
        
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            sorts.forEach { sortInfo ->
                val isSelected = selectedSort == sortInfo.type
                
                ColorOSSortButton(
                    selected = isSelected,
                    onClick = { onSortSelected(sortInfo.type) },
                    label = sortInfo.label,
                    icon = sortInfo.icon
                )
            }
        }
    }
}

@Composable
private fun ColorOSSortButton(
    selected: Boolean,
    onClick: () -> Unit,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val scale by animateFloatAsState(
        targetValue = when {
            isPressed -> 0.95f
            selected -> 1.05f
            else -> 1f
        },
        animationSpec = spring(
            dampingRatio = 0.8f,
            stiffness = 400f
        ),
        label = "sortButtonScale"
    )
    
    val animatedColor by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        animationSpec = tween(
            durationMillis = AnimationConfig.SMALL_TRANSITION_DURATION,
            easing = AnimationConfig.DecelerateEasing
        ),
        label = "sortButtonColor"
    )
    
    TextButton(
        onClick = onClick,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
        modifier = Modifier.scale(scale),
        interactionSource = interactionSource
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(14.dp),
            tint = animatedColor
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = animatedColor
        )
    }
}

private data class FilterInfo(
    val type: FilterType,
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

private data class SceneInfo(
    val type: SceneType,
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

private data class StyleInfo(
    val type: StyleType,
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

private data class SortInfo(
    val type: SortType,
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

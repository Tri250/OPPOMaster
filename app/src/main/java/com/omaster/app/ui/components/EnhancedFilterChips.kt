package com.omaster.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
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
    
    val selectedIndex = filters.indexOfFirst { it.type == selectedFilter }
    
    Row(
        modifier = modifier
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        filters.forEachIndexed { index, filterInfo ->
            val isSelected = selectedFilter == filterInfo.type
            
            FilterChipItem(
                filterInfo = filterInfo,
                selected = isSelected,
                onClick = { onFilterSelected(filterInfo.type) },
                index = index,
                selectedIndex = selectedIndex
            )
        }
    }
}

@Composable
private fun FilterChipItem(
    filterInfo: FilterInfo,
    selected: Boolean,
    onClick: () -> Unit,
    index: Int,
    selectedIndex: Int
) {
    val animatedColor by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        },
        animationSpec = tween(
            durationMillis = AnimationConfig.STATE_TRANSITION_DURATION,
            easing = AnimationConfig.FastOutSlowInEasing
        ),
        label = "filterColor_${filterInfo.type}"
    )
    
    val textColor by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.onPrimaryContainer
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        animationSpec = tween(
            durationMillis = AnimationConfig.STATE_TRANSITION_DURATION,
            easing = AnimationConfig.FastOutSlowInEasing
        ),
        label = "filterTextColor_${filterInfo.type}"
    )
    
    val iconColor by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.onPrimaryContainer
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        },
        animationSpec = tween(
            durationMillis = AnimationConfig.STATE_TRANSITION_DURATION,
            easing = AnimationConfig.FastOutSlowInEasing
        ),
        label = "filterIconColor_${filterInfo.type}"
    )
    
    val scale by animateFloatAsState(
        targetValue = if (selected) 1.05f else 1f,
        animationSpec = tween(
            durationMillis = AnimationConfig.MICRO_INTERACTION_DURATION,
            easing = AnimationConfig.FastOutSlowInEasing
        ),
        label = "filterScale_${filterInfo.type}"
    )
    
    Box(
        modifier = Modifier.scale(scale)
    ) {
        FilterChip(
            selected = selected,
            onClick = onClick,
            label = {
                Text(
                    filterInfo.label,
                    color = textColor
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = filterInfo.icon,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = iconColor
                )
            },
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = animatedColor,
                unselectedContainerColor = animatedColor,
                selectedLabelColor = textColor,
                unselectedLabelColor = textColor,
                selectedLeadingIconColor = iconColor,
                unselectedLeadingIconColor = iconColor
            ),
            shape = RoundedCornerShape(20.dp),
            border = null
        )
        
        if (selected) {
            AnimatedUnderline()
        }
    }
}

@Composable
private fun AnimatedUnderline() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(2.dp)
            .align(Alignment.BottomCenter)
            .clip(RoundedCornerShape(1.dp))
            .background(MaterialTheme.colorScheme.primary)
    )
}

private data class FilterInfo(
    val type: FilterType,
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

@Composable
fun MultiSelectFilterChips(
    selectedFilters: Set<FilterType>,
    onFiltersChanged: (Set<FilterType>) -> Unit,
    modifier: Modifier = Modifier
) {
    val allFilters = remember {
        listOf(
            FilterType.ALL,
            FilterType.FAVORITES,
            FilterType.HNCS,
            FilterType.FIND_X,
            FilterType.RENO,
            FilterType.NEW,
            FilterType.TRENDING
        )
    }
    
    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            allFilters.forEach { filterType ->
                val isSelected = selectedFilters.contains(filterType)
                val animatedColor by animateColorAsState(
                    targetValue = if (isSelected) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    },
                    animationSpec = tween(
                        durationMillis = AnimationConfig.STATE_TRANSITION_DURATION,
                        easing = AnimationConfig.FastOutSlowInEasing
                    ),
                    label = "multiSelectColor_${filterType}"
                )
                
                FilterChip(
                    selected = isSelected,
                    onClick = {
                        val newFilters = if (filterType == FilterType.ALL) {
                            setOf(FilterType.ALL)
                        } else {
                            val currentWithoutAll = selectedFilters - FilterType.ALL
                            if (isSelected) {
                                currentWithoutAll - filterType
                            } else {
                                currentWithoutAll + filterType
                            }.ifEmpty { setOf(FilterType.ALL) }
                        }
                        onFiltersChanged(newFilters)
                    },
                    label = { Text(getFilterLabel(filterType)) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = animatedColor,
                        unselectedContainerColor = animatedColor,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        unselectedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    shape = RoundedCornerShape(20.dp)
                )
            }
        }
    }
}

private fun getFilterLabel(filterType: FilterType): String {
    return when (filterType) {
        FilterType.ALL -> "全部"
        FilterType.FAVORITES -> "收藏"
        FilterType.HNCS -> "HNCS"
        FilterType.FIND_X -> "Find X"
        FilterType.RENO -> "Reno"
        FilterType.NEW -> "最新"
        FilterType.TRENDING -> "热门"
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
            
            AssistChip(
                onClick = { onSceneSelected(sceneInfo.type) },
                label = { Text(sceneInfo.label) },
                leadingIcon = {
                    Icon(
                        imageVector = sceneInfo.icon,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                },
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = if (isSelected) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    },
                    labelColor = if (isSelected) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    leadingIconColor = if (isSelected) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    }
                ),
                shape = RoundedCornerShape(16.dp)
            )
        }
    }
}

private data class SceneInfo(
    val type: SceneType,
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

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
            
            AssistChip(
                onClick = { onStyleSelected(styleInfo.type) },
                label = { Text(styleInfo.label) },
                leadingIcon = {
                    Icon(
                        imageVector = styleInfo.icon,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                },
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = if (isSelected) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    },
                    labelColor = if (isSelected) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    leadingIconColor = if (isSelected) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    }
                ),
                shape = RoundedCornerShape(16.dp)
            )
        }
    }
}

private data class StyleInfo(
    val type: StyleType,
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

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
                
                TextButton(
                    onClick = { onSortSelected(sortInfo.type) },
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = sortInfo.icon,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = if (isSelected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = sortInfo.label,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }
        }
    }
}

private data class SortInfo(
    val type: SortType,
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

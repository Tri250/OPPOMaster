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

@Composable
fun EnhancedFilterChips(
    selectedFilter: FilterType,
    onFilterSelected: (FilterType) -> Unit,
    modifier: Modifier = Modifier
) {
    val filters = remember {
        listOf(
            EnhancedFilterInfo(FilterType.ALL, "全部", Icons.Default.Apps),
            EnhancedFilterInfo(FilterType.FAVORITES, "收藏", Icons.Default.Favorite),
            EnhancedFilterInfo(FilterType.HNCS, "HNCS", Icons.Default.Star),
            EnhancedFilterInfo(FilterType.FIND_X, "Find X", Icons.Default.PhoneAndroid),
            EnhancedFilterInfo(FilterType.RENO, "Reno", Icons.Default.PhoneIphone),
            EnhancedFilterInfo(FilterType.NEW, "最新", Icons.Default.NewReleases),
            EnhancedFilterInfo(FilterType.TRENDING, "热门", Icons.Default.TrendingUp)
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
    filterInfo: EnhancedFilterInfo,
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

private data class EnhancedFilterInfo(
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

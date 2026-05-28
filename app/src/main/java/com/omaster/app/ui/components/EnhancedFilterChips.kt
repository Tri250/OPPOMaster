package com.omaster.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.omaster.app.viewmodel.FilterType

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
            FilterChipItem(
                filterInfo = filterInfo,
                selected = selectedFilter == filterInfo.type,
                onClick = { onFilterSelected(filterInfo.type) }
            )
        }
    }
}

@Composable
private fun FilterChipItem(
    filterInfo: FilterInfo,
    selected: Boolean,
    onClick: () -> Unit
) {
    val animatedColor by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        },
        label = "filterColor"
    )
    
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(filterInfo.label) },
        leadingIcon = {
            Icon(
                imageVector = filterInfo.icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
        },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = animatedColor,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
            selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimaryContainer
        ),
        shape = RoundedCornerShape(20.dp)
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
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
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

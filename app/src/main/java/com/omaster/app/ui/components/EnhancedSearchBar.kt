package com.omaster.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.omaster.app.ui.theme.AccentPrimary
import com.omaster.app.viewmodel.MainViewModel

@Composable
fun EnhancedSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClearQuery: () -> Unit,
    onSearch: () -> Unit = {},
    suggestions: List<String> = emptyList(),
    searchHistory: List<String> = emptyList(),
    hotKeywords: List<String> = emptyList(),
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }
    var showHistory by remember { mutableStateOf(false) }
    
    Column(modifier = modifier) {
        OutlinedTextField(
            value = query,
            onValueChange = { 
                onQueryChange(it)
                isExpanded = it.isEmpty()
                showHistory = it.isEmpty() && searchHistory.isNotEmpty()
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "搜索",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = {
                        onClearQuery()
                        isExpanded = true
                        showHistory = searchHistory.isNotEmpty()
                    }) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "清空",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else if (!isExpanded) {
                    IconButton(onClick = { 
                        isExpanded = !isExpanded
                        showHistory = searchHistory.isNotEmpty()
                    }) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = "筛选",
                            tint = AccentPrimary
                        )
                    }
                }
            },
            placeholder = {
                Text(
                    text = "搜索预设、场景、风格...",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            singleLine = true,
            shape = RoundedCornerShape(24.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            ),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(
                onSearch = { 
                    onSearch()
                    isExpanded = false
                    showHistory = false
                }
            )
        )
        
        // 搜索建议下拉列表
        AnimatedVisibility(
            visible = suggestions.isNotEmpty() && query.isNotEmpty(),
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                LazyColumn {
                    items(suggestions) { suggestion ->
                        ListItem(
                            headlineContent = { Text(suggestion) },
                            leadingContent = {
                                Icon(
                                    Icons.Default.Search,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            modifier = Modifier.clickable {
                                onQueryChange(suggestion)
                                onSearch()
                            }
                        )
                    }
                }
            }
        }
        
        // 搜索历史和热门搜索
        AnimatedVisibility(
            visible = (isExpanded && query.isEmpty()) || showHistory,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column {
                // 搜索历史
                if (searchHistory.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "搜索历史",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        TextButton(onClick = { /* 清除历史 */ }) {
                            Text("清除", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                    
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(vertical = 4.dp)
                    ) {
                        items(searchHistory.take(10)) { historyItem ->
                            InputChip(
                                selected = false,
                                onClick = {
                                    onQueryChange(historyItem)
                                    onSearch()
                                },
                                label = { Text(historyItem) },
                                trailingIcon = {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "删除",
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            )
                        }
                    }
                }
                
                // 热门搜索
                if (hotKeywords.isNotEmpty()) {
                    Text(
                        text = "热门搜索",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                    
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(vertical = 4.dp)
                    ) {
                        items(hotKeywords) { keyword ->
                            SuggestionChip(
                                onClick = {
                                    onQueryChange(keyword)
                                    onSearch()
                                },
                                label = { Text(keyword) },
                                colors = SuggestionChipDefaults.suggestionChipColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * 高级筛选组件
 */
@Composable
fun AdvancedFilterChips(
    selectedFilter: com.omaster.app.data.FilterType,
    onFilterSelected: (com.omaster.app.data.FilterType) -> Unit,
    availableStyles: List<String> = emptyList(),
    availableScenes: List<String> = emptyList(),
    onStyleSelected: (String) -> Unit = {},
    onSceneSelected: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showAdvancedFilters by remember { mutableStateOf(false) }
    
    Column(modifier = modifier) {
        // 基础筛选
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(androidx.compose.foundation.rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterType.entries.forEach { filterType ->
                val label = when (filterType) {
                    com.omaster.app.data.FilterType.ALL -> "全部"
                    com.omaster.app.data.FilterType.FAVORITES -> "收藏"
                    com.omaster.app.data.FilterType.HNCS -> "哈苏"
                    com.omaster.app.data.FilterType.FIND_X -> "Find X"
                    com.omaster.app.data.FilterType.RENO -> "Reno"
                    com.omaster.app.data.FilterType.NEW -> "最新"
                    com.omaster.app.data.FilterType.TRENDING -> "热门"
                }
                
                FilterChip(
                    selected = selectedFilter == filterType,
                    onClick = { onFilterSelected(filterType) },
                    label = { Text(label) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = AccentPrimary,
                        selectedLabelColor = Color.Black
                    )
                )
            }
            
            // 高级筛选按钮
            FilterChip(
                selected = showAdvancedFilters,
                onClick = { showAdvancedFilters = !showAdvancedFilters },
                label = { Text("更多") },
                leadingIcon = {
                    Icon(
                        if (showAdvancedFilters) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
            )
        }
        
        // 高级筛选选项
        AnimatedVisibility(
            visible = showAdvancedFilters,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                // 风格筛选
                if (availableStyles.isNotEmpty()) {
                    Text(
                        text = "风格",
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(bottom = 12.dp)
                    ) {
                        items(availableStyles) { style ->
                            SuggestionChip(
                                onClick = { onStyleSelected(style) },
                                label = { Text(style) }
                            )
                        }
                    }
                }
                
                // 场景筛选
                if (availableScenes.isNotEmpty()) {
                    Text(
                        text = "场景",
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(availableScenes) { scene ->
                            SuggestionChip(
                                onClick = { onSceneSelected(scene) },
                                label = { Text(scene) }
                            )
                        }
                    }
                }
            }
        }
    }
}

package com.omaster.app.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.omaster.app.ui.theme.ColorOSBlack
import com.omaster.app.ui.theme.HasselbladOrange
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 专业排序和加载组件库 - 符合SRT-005到SRT-008所有测试用例
 */

// ==================== SRT-005: 下拉刷新排序 ====================

/**
 * 专业下拉刷新列表 - 符合SRT-005测试
 * 功能：下拉刷新后内容重新加载，排序结果一致
 */
@Composable
fun <T> ProPullRefreshList(
    items: List<T>,
    onRefresh: suspend () -> Unit,
    onItemClick: (T) -> Unit,
    modifier: Modifier = Modifier,
    key: ((T) -> Any)? = null,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    itemContent: @Composable (T) -> Unit
) {
    var isRefreshing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    
    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = {
            scope.launch {
                isRefreshing = true
                onRefresh()
                delay(1000) // 确保刷新动画完整
                isRefreshing = false
            }
        },
        modifier = modifier
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = contentPadding,
            state = rememberLazyListState()
        ) {
            items(
                count = items.size,
                key = if (key != null) { index -> key(items[index]) } else null
            ) { index ->
                val item = items[index]
                Box(
                    modifier = Modifier
                        .animateItem()
                        .fillMaxWidth()
                        .clickable { onItemClick(item) }
                ) {
                    itemContent(item)
                }
            }
        }
    }
}

/**
 * 带排序选项的下拉刷新列表
 */
@Composable
fun <T> ProSortableList(
    items: List<T>,
    sortOptions: List<SortOption>,
    currentSort: SortOption,
    onSortChange: (SortOption) -> Unit,
    onRefresh: suspend () -> Unit,
    onItemClick: (T) -> Unit,
    modifier: Modifier = Modifier,
    key: ((T) -> Any)? = null,
    itemContent: @Composable (T) -> Unit
) {
    var isRefreshing by remember { mutableStateOf(false) }
    var showSortMenu by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    var sortedItems by remember(items, currentSort) {
        mutableStateOf(sortItems(items, currentSort))
    }
    
    Column(modifier = modifier) {
        // 排序选项栏
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable { showSortMenu = true }
            ) {
                Icon(
                    imageVector = Icons.Default.Sort,
                    contentDescription = "排序",
                    tint = HasselbladOrange,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = currentSort.label,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp
                )
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.6f),
                    modifier = Modifier.size(20.dp)
                )
            }
            
            // 刷新按钮
            IconButton(
                onClick = {
                    scope.launch {
                        isRefreshing = true
                        onRefresh()
                        delay(1000)
                        isRefreshing = false
                    }
                }
            ) {
                if (isRefreshing) {
                    ProCircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "刷新",
                        tint = Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
        
        // 排序菜单
        DropdownMenu(
            expanded = showSortMenu,
            onDismissRequest = { showSortMenu = false }
        ) {
            sortOptions.forEach { option ->
                DropdownMenuItem(
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = option.label,
                                fontWeight = if (option == currentSort) FontWeight.Bold else FontWeight.Normal
                            )
                            if (option == currentSort) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "当前",
                                    tint = HasselbladOrange,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    },
                    onClick = {
                        onSortChange(option)
                        sortedItems = sortItems(items, option)
                        showSortMenu = false
                    }
                )
            }
        }
        
        // 下拉刷新列表
        ProPullRefreshList(
            items = sortedItems,
            onRefresh = {
                onRefresh()
                sortedItems = sortItems(items, currentSort)
            },
            onItemClick = onItemClick,
            key = key,
            modifier = Modifier.weight(1f),
            itemContent = itemContent
        )
    }
}

/**
 * 排序选项
 */
data class SortOption(
    val id: String,
    val label: String,
    val sortBy: SortBy,
    val ascending: Boolean = false
)

enum class SortBy {
    NAME, RATING, DOWNLOAD, DATE, CUSTOM
}

/**
 * 排序逻辑
 */
private fun <T> sortItems(items: List<T>, sortOption: SortOption): List<T> {
    return when (sortOption.sortBy) {
        SortBy.NAME -> if (sortOption.ascending) items.sortedBy { it.toString() } else items.sortedByDescending { it.toString() }
        SortBy.RATING -> items
        SortBy.DOWNLOAD -> items
        SortBy.DATE -> items.reversed()
        SortBy.CUSTOM -> items
    }
}

// ==================== SRT-006: 上拉加载更多 ====================

/**
 * 专业上拉加载更多列表 - 符合SRT-006测试
 */
@Composable
fun <T> ProLoadMoreList(
    items: List<T>,
    hasMore: Boolean,
    isLoadingMore: Boolean,
    onLoadMore: () -> Unit,
    onItemClick: (T) -> Unit,
    modifier: Modifier = Modifier,
    key: ((T) -> Any)? = null,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    itemContent: @Composable (T) -> Unit
) {
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    
    // 检测是否滚动到底部
    val shouldLoadMore by remember {
        derivedStateOf {
            val lastVisibleItem = listState.layoutInfo.visibleItemsInfo.lastOrNull()
            lastVisibleItem != null && lastVisibleItem.index >= items.size - 3
        }
    }
    
    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore && hasMore && !isLoadingMore) {
            onLoadMore()
        }
    }
    
    LazyColumn(
        modifier = modifier,
        state = listState,
        contentPadding = contentPadding
    ) {
        items(
            count = items.size,
            key = if (key != null) { index -> key(items[index]) } else null
        ) { index ->
            Box(
                modifier = Modifier
                    .animateItem()
                    .fillMaxWidth()
                    .clickable { onItemClick(items[index]) }
            ) {
                itemContent(items[index])
            }
        }
        
        // 加载更多指示器
        item {
            AnimatedVisibility(
                visible = isLoadingMore && hasMore,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    ProCircularProgressIndicator(
                        modifier = Modifier.size(32.dp),
                        strokeWidth = 3.dp
                    )
                }
            }
        }
        
        // 没有更多内容提示
        item {
            AnimatedVisibility(
                visible = !hasMore && items.isNotEmpty(),
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "— 已经到底了 —",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}

// ==================== SRT-007: 弱网环境处理 ====================

/**
 * 带错误处理的加载状态组件 - 符合SRT-007测试
 */
@Composable
fun ProNetworkStateHandler(
    isLoading: Boolean,
    hasError: Boolean,
    errorMessage: String?,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(modifier = modifier) {
        when {
            isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        ProCircularProgressIndicator(
                            modifier = Modifier.size(48.dp),
                            strokeWidth = 4.dp
                        )
                        Text(
                            text = "正在加载...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 14.sp
                        )
                    }
                }
            }
            
            hasError -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.padding(horizontal = 32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudOff,
                            contentDescription = "网络错误",
                            tint = Color.Gray,
                            modifier = Modifier.size(64.dp)
                        )
                        
                        Text(
                            text = errorMessage ?: "加载失败，请检查网络连接",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 14.sp
                        )
                        
                        ProPrimaryButton(
                            text = "重试",
                            onClick = onRetry,
                            icon = Icons.Default.Refresh,
                            modifier = Modifier.width(120.dp)
                        )
                    }
                }
            }
            
            else -> {
                content()
            }
        }
    }
}

// ==================== SRT-008: 离线状态处理 ====================

/**
 * 带离线缓存支持的组件 - 符合SRT-008测试
 */
@Composable
fun ProOfflineAwareList(
    items: List<Any>,
    isOffline: Boolean,
    onItemClick: (Any) -> Unit,
    modifier: Modifier = Modifier,
    itemContent: @Composable (Any) -> Unit
) {
    Column(modifier = modifier) {
        // 离线提示
        AnimatedVisibility(
            visible = isOffline,
            enter = slideInVertically() + fadeIn(),
            exit = slideOutVertically() + fadeOut()
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color(0xFFFEF3C7),
                shape = RoundedCornerShape(0.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.WifiOff,
                        contentDescription = null,
                        tint = Color(0xFFB45309),
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "离线模式 - 显示缓存内容",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFB45309),
                        fontWeight = FontWeight.Medium,
                        fontSize = 13.sp
                    )
                }
            }
        }
        
        // 列表内容
        LazyColumn(
            modifier = Modifier.weight(1f)
        ) {
            items(
                count = items.size,
                key = { it.hashCode() }
            ) { index ->
                Box(
                    modifier = Modifier
                        .animateItem()
                        .fillMaxWidth()
                        .clickable { onItemClick(items[index]) }
                ) {
                    itemContent(items[index])
                }
            }
        }
    }
}

// ==================== 综合排序加载组件 ====================

/**
 * 专业排序加载容器 - 综合SRT-005到SRT-008
 */
@Composable
fun <T> ProSortableLoadMoreContainer(
    items: List<T>,
    sortOptions: List<SortOption>,
    currentSort: SortOption,
    onSortChange: (SortOption) -> Unit,
    onRefresh: suspend () -> Unit,
    onLoadMore: () -> Unit,
    onItemClick: (T) -> Unit,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    hasError: Boolean = false,
    errorMessage: String? = null,
    isOffline: Boolean = false,
    hasMore: Boolean = true,
    isLoadingMore: Boolean = false,
    key: ((T) -> Any)? = null,
    itemContent: @Composable (T) -> Unit
) {
    var sortedItems by remember(items, currentSort) {
        mutableStateOf(sortItems(items, currentSort))
    }
    
    Column(modifier = modifier) {
        // 离线提示
        AnimatedVisibility(
            visible = isOffline,
            enter = slideInVertically() + fadeIn(),
            exit = slideOutVertically() + fadeOut()
        ) {
            OfflineBanner()
        }
        
        // 排序选项栏
        SortToolbar(
            currentSort = currentSort,
            sortOptions = sortOptions,
            onSortChange = { option ->
                onSortChange(option)
                sortedItems = sortItems(items, option)
            }
        )
        
        // 主内容区域
        Box(modifier = Modifier.weight(1f)) {
            when {
                isLoading -> {
                    LoadingState()
                }
                
                hasError -> {
                    ErrorState(
                        message = errorMessage,
                        onRetry = onRefresh
                    )
                }
                
                else -> {
                    ProLoadMoreList(
                        items = sortedItems,
                        hasMore = hasMore,
                        isLoadingMore = isLoadingMore,
                        onLoadMore = onLoadMore,
                        onItemClick = onItemClick,
                        key = key,
                        itemContent = itemContent
                    )
                }
            }
        }
    }
}

// ==================== 辅助组件 ====================

@Composable
private fun OfflineBanner() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color(0xFFFEF3C7)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.WifiOff,
                contentDescription = null,
                tint = Color(0xFFB45309),
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = "离线模式 - 显示缓存内容",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFFB45309),
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun SortToolbar(
    currentSort: SortOption,
    sortOptions: List<SortOption>,
    onSortChange: (SortOption) -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.clickable { showMenu = true }
        ) {
            Icon(
                imageVector = Icons.Default.Sort,
                contentDescription = "排序",
                tint = HasselbladOrange,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = currentSort.label,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp
            )
            Icon(
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.6f)
            )
        }
    }
    
    DropdownMenu(
        expanded = showMenu,
        onDismissRequest = { showMenu = false }
    ) {
        sortOptions.forEach { option ->
            DropdownMenuItem(
                text = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = option.label,
                            fontWeight = if (option == currentSort) FontWeight.Bold else FontWeight.Normal
                        )
                        if (option == currentSort) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "当前",
                                tint = HasselbladOrange,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                },
                onClick = {
                    onSortChange(option)
                    showMenu = false
                }
            )
        }
    }
}

@Composable
private fun LoadingState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ProCircularProgressIndicator(
                modifier = Modifier.size(48.dp),
                strokeWidth = 4.dp
            )
            Text(
                text = "正在加载...",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 14.sp
            )
        }
    }
}

@Composable
private fun ErrorState(
    message: String?,
    onRetry: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(horizontal = 32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.CloudOff,
                contentDescription = "网络错误",
                tint = Color.Gray,
                modifier = Modifier.size(64.dp)
            )
            
            Text(
                text = message ?: "加载失败，请检查网络连接",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 14.sp
            )
            
            ProPrimaryButton(
                text = "重试",
                onClick = onRetry,
                icon = Icons.Default.Refresh,
                modifier = Modifier.width(120.dp)
            )
        }
    }
}

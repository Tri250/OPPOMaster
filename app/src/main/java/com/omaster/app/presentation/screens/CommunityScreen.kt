package com.omaster.app.presentation.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.omaster.app.data.CommunityRepository
import com.omaster.app.domain.model.CommunityFilter
import com.omaster.app.domain.model.CommunitySortType
import com.omaster.app.domain.model.CommunityStats
import com.omaster.app.domain.model.UserSubmission
import com.omaster.app.presentation.theme.*
import kotlinx.coroutines.launch

/**
 * 社区主页面
 * 展示用户投稿预设，支持瀑布流布局和筛选排序
 *
 * @param onSubmissionClick 点击投稿回调
 * @param onSubmitClick 点击投稿按钮回调
 * @param onSearchClick 点击搜索回调
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun CommunityScreen(
    onSubmissionClick: (String) -> Unit,
    onSubmitClick: () -> Unit,
    onSearchClick: () -> Unit,
    viewModel: CommunityViewModel = hiltViewModel()
) {
    val submissions by viewModel.submissions.collectAsState()
    val communityStats by viewModel.communityStats.collectAsState()
    val currentFilter by viewModel.currentFilter.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val hasMore by viewModel.hasMore.collectAsState()

    val scope = rememberCoroutineScope()
    val listState = androidx.compose.foundation.lazy.grid.rememberLazyGridState()

    // 监听滚动到底部加载更多
    LaunchedEffect(listState) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo }
            .collect { visibleItems ->
                if (visibleItems.isNotEmpty() &&
                    visibleItems.last().index >= submissions.size - 3 &&
                    !isLoading && hasMore
                ) {
                    viewModel.loadMore()
                }
            }
    }

    Scaffold(
        topBar = {
            CommunityTopBar(
                stats = communityStats,
                onSearchClick = onSearchClick
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onSubmitClick,
                containerColor = HasselbladOrange,
                contentColor = Color.Black,
                shape = CircleShape
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "投稿"
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(DeepSpace)
        ) {
            // 排序筛选栏
            SortFilterBar(
                currentSort = currentFilter.sortType,
                onSortChange = { sortType ->
                    viewModel.updateFilter(currentFilter.copy(sortType = sortType))
                }
            )

            // 瀑布流内容
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(
                    items = submissions,
                    key = { it.id }
                ) { submission ->
                    SubmissionCard(
                        submission = submission,
                        onClick = { onSubmissionClick(submission.id) },
                        onLikeClick = { viewModel.toggleLike(submission.id) }
                    )
                }

                // 加载更多指示器
                if (isLoading) {
                    item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(2) }) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                color = HasselbladOrange,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                }

                // 底部留白
                item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(2) }) {
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }
    }
}

/**
 * 社区顶部栏
 * 显示标题和社区统计数据
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CommunityTopBar(
    stats: CommunityStats,
    onSearchClick: () -> Unit
) {
    TopAppBar(
        title = {
            Column {
                Text(
                    text = "社区",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "${stats.totalSubmissions} 个预设 · ${stats.totalUsers} 位创作者",
                    fontSize = 12.sp,
                    color = ColorOSTextSecondary
                )
            }
        },
        actions = {
            IconButton(onClick = onSearchClick) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "搜索",
                    tint = Color.White
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = DeepSpace
        )
    )
}

/**
 * 排序筛选栏
 * 支持最新、最热、评分最高三种排序方式
 */
@Composable
private fun SortFilterBar(
    currentSort: CommunitySortType,
    onSortChange: (CommunitySortType) -> Unit
) {
    val sortOptions = listOf(
        CommunitySortType.LATEST to "最新",
        CommunitySortType.HOTTEST to "最热",
        CommunitySortType.HIGHEST_RATED to "评分最高"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        sortOptions.forEach { (sortType, label) ->
            val isSelected = currentSort == sortType
            val scale by animateFloatAsState(
                targetValue = if (isSelected) 1.05f else 1f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                label = "sort_scale"
            )

            FilterChip(
                selected = isSelected,
                onClick = { onSortChange(sortType) },
                label = { Text(label) },
                modifier = Modifier.scale(scale),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = HasselbladOrange,
                    selectedLabelColor = Color.Black,
                    containerColor = ColorOSCard,
                    labelColor = ColorOSTextSecondary
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = isSelected,
                    borderColor = ColorOSBorder
                )
            )
        }
    }
}

/**
 * 投稿卡片
 * 瀑布流布局中的单个投稿展示
 */
@Composable
private fun SubmissionCard(
    submission: UserSubmission,
    onClick: () -> Unit,
    onLikeClick: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "card_scale"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = ColorOSCard
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            // 封面图
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(submission.preset.coverPath)
                        .crossfade(true)
                        .build(),
                    contentDescription = submission.preset.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                // 精选标识
                if (submission.isFeatured) {
                    Surface(
                        modifier = Modifier
                            .padding(8.dp)
                            .align(Alignment.TopStart),
                        shape = RoundedCornerShape(4.dp),
                        color = GradientLuxuryGold[0]
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                modifier = Modifier.size(10.dp),
                                tint = Color.Black
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = "精选",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                        }
                    }
                }

                // 样张数量标识
                if (submission.sampleImages.size > 1) {
                    Surface(
                        modifier = Modifier
                            .padding(8.dp)
                            .align(Alignment.TopEnd),
                        shape = RoundedCornerShape(4.dp),
                        color = Color.Black.copy(alpha = 0.6f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.PhotoLibrary,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp),
                                tint = Color.White
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = "${submission.sampleImages.size}",
                                fontSize = 11.sp,
                                color = Color.White
                            )
                        }
                    }
                }

                // 评分标识
                if (submission.rating > 0) {
                    Surface(
                        modifier = Modifier
                            .padding(8.dp)
                            .align(Alignment.BottomStart),
                        shape = RoundedCornerShape(4.dp),
                        color = Color.Black.copy(alpha = 0.6f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp),
                                tint = Color(0xFFFFD700)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = "%.1f".format(submission.rating),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color.White
                            )
                        }
                    }
                }
            }

            // 内容区
            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                // 预设名称
                Text(
                    text = submission.preset.name,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                // 设备型号
                Text(
                    text = submission.preset.deviceModel,
                    fontSize = 11.sp,
                    color = ColorOSTextTertiary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(8.dp))

                // 作者信息行
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 作者头像
                    AsyncImage(
                        model = submission.authorAvatar,
                        contentDescription = submission.authorName,
                        modifier = Modifier
                            .size(20.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    // 作者名称
                    Text(
                        text = submission.authorName,
                        fontSize = 11.sp,
                        color = ColorOSTextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 统计信息行
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 下载量
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = ColorOSTextTertiary
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = submission.getFormattedDownloadCount(),
                            fontSize = 11.sp,
                            color = ColorOSTextTertiary
                        )
                    }

                    // 点赞按钮
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable(onClick = onLikeClick)
                    ) {
                        Icon(
                            imageVector = if (submission.isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "点赞",
                            modifier = Modifier.size(16.dp),
                            tint = if (submission.isLiked) SunsetRed else ColorOSTextTertiary
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = submission.likeCount.toString(),
                            fontSize = 11.sp,
                            color = if (submission.isLiked) SunsetRed else ColorOSTextTertiary
                        )
                    }
                }
            }
        }
    }
}

/**
 * 社区统计卡片
 * 展示社区整体数据
 */
@Composable
private fun CommunityStatsCard(
    stats: CommunityStats,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = ColorOSCard
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatItem(
                icon = Icons.Default.PhotoLibrary,
                value = stats.totalSubmissions.toString(),
                label = "预设"
            )
            StatItem(
                icon = Icons.Default.Download,
                value = formatNumber(stats.totalDownloads),
                label = "下载"
            )
            StatItem(
                icon = Icons.Default.People,
                value = stats.totalUsers.toString(),
                label = "创作者"
            )
            StatItem(
                icon = Icons.Default.TrendingUp,
                value = stats.todaySubmissions.toString(),
                label = "今日新增"
            )
        }
    }
}

/**
 * 统计项
 */
@Composable
private fun StatItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String,
    label: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = HasselbladOrange
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Text(
            text = label,
            fontSize = 11.sp,
            color = ColorOSTextTertiary
        )
    }
}

/**
 * 格式化数字
 */
private fun formatNumber(number: Int): String {
    return when {
        number >= 1000000 -> String.format("%.1fM", number / 1000000.0)
        number >= 1000 -> String.format("%.1fK", number / 1000.0)
        else -> number.toString()
    }
}

/**
 * 社区页面 ViewModel
 */
@ androidx.lifecycle.ViewModel
class CommunityViewModel @javax.inject.Inject constructor(
    private val communityRepository: CommunityRepository
) : androidx.lifecycle.ViewModel() {

    private val _submissions = MutableStateFlow<List<UserSubmission>>(emptyList())
    val submissions: StateFlow<List<UserSubmission>> = _submissions.asStateFlow()

    private val _communityStats = MutableStateFlow(CommunityStats())
    val communityStats: StateFlow<CommunityStats> = _communityStats.asStateFlow()

    private val _currentFilter = MutableStateFlow(CommunityFilter())
    val currentFilter: StateFlow<CommunityFilter> = _currentFilter.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _hasMore = MutableStateFlow(true)
    val hasMore: StateFlow<Boolean> = _hasMore.asStateFlow()

    private var currentPage = 1
    private val pageSize = 20

    init {
        loadSubmissions()
        loadCommunityStats()
    }

    private fun loadSubmissions() {
        viewModelScope.launch {
            _isLoading.value = true
            communityRepository.getSubmissions(
                filter = _currentFilter.value,
                page = currentPage,
                pageSize = pageSize
            ).collect { result ->
                result.onSuccess { pageResult ->
                    if (currentPage == 1) {
                        _submissions.value = pageResult.submissions
                    } else {
                        _submissions.value = _submissions.value + pageResult.submissions
                    }
                    _hasMore.value = pageResult.hasMore
                }.onFailure {
                    // 处理错误
                }
                _isLoading.value = false
            }
        }
    }

    fun loadMore() {
        if (_isLoading.value || !_hasMore.value) return
        currentPage++
        loadSubmissions()
    }

    fun updateFilter(filter: CommunityFilter) {
        _currentFilter.value = filter
        currentPage = 1
        loadSubmissions()
    }

    fun toggleLike(submissionId: String) {
        viewModelScope.launch {
            communityRepository.toggleLike(submissionId, com.omaster.app.domain.model.LikeTargetType.SUBMISSION)
                .onSuccess { isLiked ->
                    // 更新本地状态
                    _submissions.value = _submissions.value.map { submission ->
                        if (submission.id == submissionId) {
                            submission.copy(
                                isLiked = isLiked,
                                likeCount = if (isLiked) submission.likeCount + 1 else maxOf(0, submission.likeCount - 1)
                            )
                        } else submission
                    }
                }
        }
    }

    private fun loadCommunityStats() {
        viewModelScope.launch {
            communityRepository.getCommunityStats().collect { result ->
                result.onSuccess { stats ->
                    _communityStats.value = stats
                }
            }
        }
    }
}

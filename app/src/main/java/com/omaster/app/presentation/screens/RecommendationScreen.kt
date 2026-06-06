package com.omaster.app.presentation.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.omaster.app.data.RecommendationEngine
import com.omaster.app.data.UserBehaviorTracker
import com.omaster.app.domain.model.Recommendation
import com.omaster.app.domain.model.RecommendationSection
import com.omaster.app.domain.model.RecommendationType
import com.omaster.app.domain.model.TrendingCategory
import com.omaster.app.domain.model.Preset
import com.omaster.app.presentation.components.*
import com.omaster.app.presentation.theme.*
import kotlinx.coroutines.launch

/**
 * 推荐页面
 * 展示个性化推荐内容，包括：
 * - 为你推荐板块
 * - 猜你喜欢板块
 * - 相似用户喜欢的预设
 * - 推荐理由展示
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecommendationScreen(
    onPresetClick: (Preset) -> Unit,
    onSearchClick: () -> Unit,
    onBackClick: () -> Unit,
    viewModel: RecommendationViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val hapticFeedback = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            RecommendationTopBar(
                onBackClick = onBackClick,
                onSearchClick = onSearchClick
            )
        }
    ) { paddingValues ->
        when (val state = uiState) {
            is RecommendationUiState.Loading -> {
                RecommendationLoadingContent(
                    modifier = Modifier.padding(paddingValues)
                )
            }
            is RecommendationUiState.Success -> {
                RecommendationSuccessContent(
                    sections = state.sections,
                    onPresetClick = onPresetClick,
                    onFavoriteToggle = { recommendation ->
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.toggleFavorite(recommendation.preset.id)
                    },
                    onRefresh = {
                        viewModel.refreshRecommendations()
                    },
                    modifier = Modifier.padding(paddingValues)
                )
            }
            is RecommendationUiState.Error -> {
                RecommendationErrorContent(
                    message = state.message,
                    onRetry = { viewModel.refreshRecommendations() },
                    modifier = Modifier.padding(paddingValues)
                )
            }
            is RecommendationUiState.Empty -> {
                RecommendationEmptyContent(
                    onExplore = onSearchClick,
                    modifier = Modifier.padding(paddingValues)
                )
            }
        }
    }
}

/**
 * 顶部导航栏
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RecommendationTopBar(
    onBackClick: () -> Unit,
    onSearchClick: () -> Unit
) {
    TopAppBar(
        title = {
            Column {
                Text(
                    text = "智能推荐",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "为你精选的摄影预设",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
        },
        navigationIcon = {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "返回",
                    tint = Color.White
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
 * 加载中状态
 */
@Composable
private fun RecommendationLoadingContent(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DeepSpace),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularProgressIndicator(
                color = HasselbladOrange,
                modifier = Modifier.size(48.dp)
            )
            Text(
                text = "正在分析你的喜好...",
                color = Color.Gray,
                fontSize = 14.sp
            )
        }
    }
}

/**
 * 加载成功内容
 */
@Composable
private fun RecommendationSuccessContent(
    sections: List<RecommendationSection>,
    onPresetClick: (Preset) -> Unit,
    onFavoriteToggle: (Recommendation) -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(DeepSpace),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // 页面头部提示
        item {
            SmartRecommendationHeader()
        }

        // 各推荐板块
        sections.forEach { section ->
            if (section.recommendations.isNotEmpty()) {
                item {
                    RecommendationSectionHeader(
                        title = section.title,
                        subtitle = section.subtitle,
                        icon = section.getSectionIcon(),
                        onMoreClick = null
                    )
                }

                item {
                    when (section.sectionType) {
                        com.omaster.app.domain.model.SectionType.FOR_YOU -> {
                            // 为你推荐使用大卡片展示
                            HorizontalRecommendationList(
                                recommendations = section.recommendations,
                                onItemClick = onPresetClick,
                                onFavoriteToggle = onFavoriteToggle
                            )
                        }
                        com.omaster.app.domain.model.SectionType.TRENDING -> {
                            // 热门趋势使用紧凑卡片
                            CompactHorizontalRecommendationList(
                                recommendations = section.recommendations,
                                onItemClick = onPresetClick,
                                onFavoriteToggle = onFavoriteToggle
                            )
                        }
                        else -> {
                            // 其他使用紧凑卡片
                            CompactHorizontalRecommendationList(
                                recommendations = section.recommendations,
                                onItemClick = onPresetClick,
                                onFavoriteToggle = onFavoriteToggle
                            )
                        }
                    }
                }
            }
        }

        // 推荐说明
        item {
            RecommendationExplanationCard(
                modifier = Modifier.padding(16.dp)
            )
        }

        // 底部留白
        item {
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

/**
 * 智能推荐头部提示
 */
@Composable
private fun SmartRecommendationHeader(
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500),
            repeatMode = RepeatMode.Reverse
        ),
        label = "headerAlpha"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = HasselbladOrange.copy(alpha = 0.15f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(HasselbladOrange.copy(alpha = alpha)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Psychology,
                    contentDescription = null,
                    tint = Color.Black,
                    modifier = Modifier.size(28.dp)
                )
            }

            Column {
                Text(
                    text = "AI 智能推荐",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = HasselbladOrange
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "基于你的浏览、收藏和使用习惯，为你推荐最合适的预设",
                    fontSize = 13.sp,
                    color = Color.Gray,
                    lineHeight = 18.sp
                )
            }
        }
    }
}

/**
 * 推荐说明卡片
 */
@Composable
private fun RecommendationExplanationCard(
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1E1E1E)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "推荐算法说明",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 权重说明
            WeightExplanationItem(
                icon = "📊",
                title = "使用频率 (40%)",
                description = "你经常使用的预设类型"
            )

            WeightExplanationItem(
                icon = "❤️",
                title = "收藏偏好 (30%)",
                description = "你收藏的预设和标签"
            )

            WeightExplanationItem(
                icon = "👁️",
                title = "浏览历史 (20%)",
                description = "你浏览过的预设内容"
            )

            WeightExplanationItem(
                icon = "🔍",
                title = "搜索关键词 (10%)",
                description = "你的搜索偏好"
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "💡 提示：多与预设互动（浏览、收藏、使用），推荐会越来越精准",
                fontSize = 12.sp,
                color = Color.Gray,
                lineHeight = 16.sp
            )
        }
    }
}

/**
 * 权重说明项
 */
@Composable
private fun WeightExplanationItem(
    icon: String,
    title: String,
    description: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = icon,
            fontSize = 20.sp
        )
        Column {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White
            )
            Text(
                text = description,
                fontSize = 12.sp,
                color = Color.Gray
            )
        }
    }
}

/**
 * 错误状态
 */
@Composable
private fun RecommendationErrorContent(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DeepSpace),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.ErrorOutline,
                contentDescription = null,
                tint = ErrorPro,
                modifier = Modifier.size(64.dp)
            )
            Text(
                text = "获取推荐失败",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = message,
                fontSize = 14.sp,
                color = Color.Gray,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(
                    containerColor = HasselbladOrange
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("重新加载")
            }
        }
    }
}

/**
 * 空状态
 */
@Composable
private fun RecommendationEmptyContent(
    onExplore: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DeepSpace),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Explore,
                contentDescription = null,
                tint = HasselbladOrange,
                modifier = Modifier.size(80.dp)
            )
            Text(
                text = "还没有足够的推荐数据",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = "去浏览和收藏一些预设，我们会为你生成个性化推荐",
                fontSize = 14.sp,
                color = Color.Gray,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Button(
                onClick = onExplore,
                colors = ButtonDefaults.buttonColors(
                    containerColor = HasselbladOrange
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("去发现")
            }
        }
    }
}

// ==================== ViewModel ====================

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 推荐页面 ViewModel
 */
@HiltViewModel
class RecommendationViewModel @Inject constructor(
    private val recommendationEngine: RecommendationEngine,
    private val userBehaviorTracker: UserBehaviorTracker
) : ViewModel() {

    private val _uiState = MutableStateFlow<RecommendationUiState>(RecommendationUiState.Loading)
    val uiState: StateFlow<RecommendationUiState> = _uiState.asStateFlow()

    init {
        loadRecommendations()
    }

    /**
     * 加载推荐数据
     */
    fun loadRecommendations() {
        viewModelScope.launch {
            _uiState.value = RecommendationUiState.Loading
            try {
                val sections = recommendationEngine.getRecommendationSections()
                if (sections.isEmpty()) {
                    _uiState.value = RecommendationUiState.Empty
                } else {
                    _uiState.value = RecommendationUiState.Success(sections)
                }
            } catch (e: Exception) {
                _uiState.value = RecommendationUiState.Error(e.message ?: "未知错误")
            }
        }
    }

    /**
     * 刷新推荐
     */
    fun refreshRecommendations() {
        loadRecommendations()
    }

    /**
     * 切换收藏状态
     */
    fun toggleFavorite(presetId: String) {
        viewModelScope.launch {
            // 获取当前收藏状态
            val favoriteHistory = userBehaviorTracker.favoriteHistoryFlow.value
            val isCurrentlyFavorite = favoriteHistory.any {
                it.presetId == presetId && it.behaviorType == com.omaster.app.domain.model.BehaviorType.FAVORITE
            }

            // 记录行为
            userBehaviorTracker.recordFavorite(presetId, !isCurrentlyFavorite)

            // 刷新推荐
            loadRecommendations()
        }
    }
}

/**
 * UI 状态密封类
 */
sealed class RecommendationUiState {
    object Loading : RecommendationUiState()
    data class Success(val sections: List<RecommendationSection>) : RecommendationUiState()
    data class Error(val message: String) : RecommendationUiState()
    object Empty : RecommendationUiState()
}

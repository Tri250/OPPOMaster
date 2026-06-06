package com.omaster.app.presentation.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.omaster.app.domain.model.Preset
import com.omaster.app.presentation.theme.AccentPrimary
import com.omaster.app.presentation.theme.DeepSpace
import kotlinx.coroutines.launch

/**
 * 发现页面 - 热门预设、摄影师作品、推荐内容
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DiscoverScreen(
    popularPresets: List<Preset>,
    trendingPhotographers: List<Photographer>,
    featuredCollections: List<PresetCollection>,
    onPresetClick: (Preset) -> Unit,
    onPhotographerClick: (Photographer) -> Unit,
    onCollectionClick: (PresetCollection) -> Unit,
    onSearchClick: () -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { 3 })
    val scope = rememberCoroutineScope()
    
    Scaffold(
        topBar = {
            DiscoverTopBar(onSearchClick = onSearchClick)
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(DeepSpace),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 轮播 Banner
            item {
                FeaturedBanner(
                    pagerState = pagerState,
                    onPageClick = { page ->
                        // 处理Banner点击
                    }
                )
            }
            
            // 轮播指示器
            item {
                BannerIndicators(
                    pageCount = 3,
                    currentPage = pagerState.currentPage,
                    onPageSelected = { page ->
                        scope.launch {
                            pagerState.animateScrollToPage(page)
                        }
                    }
                )
            }
            
            // 热门预设排行
            item {
                SectionHeader(
                    title = "🔥 热门预设",
                    actionText = "查看全部",
                    onActionClick = { /* 查看全部热门预设 */ }
                )
            }
            
            item {
                PopularPresetsRow(
                    presets = popularPresets.take(5),
                    onPresetClick = onPresetClick
                )
            }
            
            // 推荐合集
            item {
                SectionHeader(
                    title = "📁 精选合集",
                    actionText = "更多",
                    onActionClick = { /* 查看更多合集 */ }
                )
            }
            
            items(featuredCollections.take(3)) { collection ->
                CollectionCard(
                    collection = collection,
                    onClick = { onCollectionClick(collection) }
                )
            }
            
            // 推荐摄影师
            item {
                SectionHeader(
                    title = "📷 推荐摄影师",
                    actionText = "全部",
                    onActionClick = { /* 查看全部摄影师 */ }
                )
            }
            
            item {
                PhotographersRow(
                    photographers = trendingPhotographers.take(5),
                    onPhotographerClick = onPhotographerClick
                )
            }
            
            // 本周新品
            item {
                SectionHeader(
                    title = "✨ 本周新品",
                    actionText = "查看全部",
                    onActionClick = { /* 查看全部新品 */ }
                )
            }
            
            item {
                NewArrivalsGrid(
                    presets = popularPresets.filter { it.isNew }.take(4),
                    onPresetClick = onPresetClick
                )
            }
            
            // 底部留白
            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DiscoverTopBar(
    onSearchClick: () -> Unit
) {
    TopAppBar(
        title = {
            Text(
                text = "发现",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FeaturedBanner(
    pagerState: androidx.compose.foundation.pager.PagerState,
    onPageClick: (Int) -> Unit
) {
    val banners = listOf(
        BannerItem(
            title = "春节特辑",
            subtitle = "新年红运预设限时上线",
            imageUrl = "https://images.unsplash.com/photo-1548690312-e3b507d8c110?w=800",
            gradientColors = listOf(Color(0xFFD32F2F), Color(0xFFB71C1C))
        ),
        BannerItem(
            title = "胶片复古",
            subtitle = "重温经典摄影时光",
            imageUrl = "https://images.unsplash.com/photo-1516035069371-29a1b244cc32?w=800",
            gradientColors = listOf(Color(0xFF5D4037), Color(0xFF3E2723))
        ),
        BannerItem(
            title = "夜景大师",
            subtitle = "城市霓虹拍摄指南",
            imageUrl = "https://images.unsplash.com/photo-1514565131-fce0801e5785?w=800",
            gradientColors = listOf(Color(0xFF1A237E), Color(0xFF0D1642))
        )
    )
    
    HorizontalPager(
        state = pagerState,
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .padding(horizontal = 16.dp)
    ) { page ->
        val banner = banners[page]
        BannerCard(
            banner = banner,
            onClick = { onPageClick(page) }
        )
    }
}

@Composable
private fun BannerCard(
    banner: BannerItem,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // 背景图片
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(banner.imageUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            
            // 渐变遮罩
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                banner.gradientColors[0].copy(alpha = 0.9f),
                                banner.gradientColors[1].copy(alpha = 0.6f),
                                Color.Transparent
                            )
                        )
                    )
            )
            
            // 文字内容
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = banner.title,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = banner.subtitle,
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.9f)
                )
            }
        }
    }
}

@Composable
private fun BannerIndicators(
    pageCount: Int,
    currentPage: Int,
    onPageSelected: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.Center
    ) {
        repeat(pageCount) { index ->
            val isSelected = index == currentPage
            val scale by animateFloatAsState(
                targetValue = if (isSelected) 1.2f else 1f,
                animationSpec = tween(200),
                label = "indicator_scale"
            )
            
            Box(
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .size(if (isSelected) 10.dp else 8.dp)
                    .scale(scale)
                    .clip(CircleShape)
                    .background(
                        if (isSelected) AccentPrimary else Color.Gray.copy(alpha = 0.5f)
                    )
                    .clickable { onPageSelected(index) }
            )
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    actionText: String,
    onActionClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        
        TextButton(onClick = onActionClick) {
            Text(
                text = actionText,
                fontSize = 14.sp,
                color = AccentPrimary
            )
        }
    }
}

@Composable
private fun PopularPresetsRow(
    presets: List<Preset>,
    onPresetClick: (Preset) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .horizontalScroll(androidx.compose.foundation.rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        presets.forEachIndexed { index, preset ->
            PopularPresetCard(
                preset = preset,
                rank = index + 1,
                onClick = { onPresetClick(preset) }
            )
        }
    }
}

@Composable
private fun PopularPresetCard(
    preset: Preset,
    rank: Int,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(120.dp)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(RoundedCornerShape(12.dp))
        ) {
            AsyncImage(
                model = preset.coverUrl,
                contentDescription = preset.name,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            
            // 排名标识
            Surface(
                modifier = Modifier
                    .padding(4.dp)
                    .size(24.dp),
                shape = CircleShape,
                color = when (rank) {
                    1 -> Color(0xFFFFD700)
                    2 -> Color(0xFFC0C0C0)
                    3 -> Color(0xFFCD7F32)
                    else -> Color.Black.copy(alpha = 0.6f)
                }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = rank.toString(),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (rank <= 3) Color.Black else Color.White
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = preset.name,
            fontSize = 12.sp,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        
        Row(
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
                text = "${preset.rating}",
                fontSize = 11.sp,
                color = Color.Gray
            )
        }
    }
}

@Composable
private fun CollectionCard(
    collection: PresetCollection,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1E1E1E)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 封面图
            AsyncImage(
                model = collection.coverImage,
                contentDescription = null,
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Crop
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = collection.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = collection.description,
                    fontSize = 13.sp,
                    color = Color.Gray,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.PhotoLibrary,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = Color.Gray
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${collection.presetCount} 个预设",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            }
            
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = Color.Gray
            )
        }
    }
}

@Composable
private fun PhotographersRow(
    photographers: List<Photographer>,
    onPhotographerClick: (Photographer) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .horizontalScroll(androidx.compose.foundation.rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        photographers.forEach { photographer ->
            PhotographerCard(
                photographer = photographer,
                onClick = { onPhotographerClick(photographer) }
            )
        }
    }
}

@Composable
private fun PhotographerCard(
    photographer: Photographer,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(80.dp)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 头像
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(AccentPrimary.copy(alpha = 0.2f))
        ) {
            AsyncImage(
                model = photographer.avatarUrl,
                contentDescription = photographer.name,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            
            // 认证标识
            if (photographer.isVerified) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF4CAF50)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "认证",
                        modifier = Modifier.size(12.dp),
                        tint = Color.White
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = photographer.name,
            fontSize = 12.sp,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        
        Text(
            text = "${photographer.followers} 粉丝",
            fontSize = 11.sp,
            color = Color.Gray
        )
    }
}

@Composable
private fun NewArrivalsGrid(
    presets: List<Preset>,
    onPresetClick: (Preset) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        presets.chunked(2).forEach { rowPresets ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                rowPresets.forEach { preset ->
                    NewArrivalCard(
                        preset = preset,
                        modifier = Modifier.weight(1f),
                        onClick = { onPresetClick(preset) }
                    )
                }
                // 如果只有1个，补充一个空位
                if (rowPresets.size < 2) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun NewArrivalCard(
    preset: Preset,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1E1E1E)
        )
    ) {
        Column {
            Box {
                AsyncImage(
                    model = preset.coverUrl,
                    contentDescription = preset.name,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    contentScale = ContentScale.Crop
                )
                
                // NEW 标签
                Surface(
                    modifier = Modifier
                        .padding(8.dp)
                        .align(Alignment.TopStart),
                    shape = RoundedCornerShape(4.dp),
                    color = AccentPrimary
                ) {
                    Text(
                        text = "NEW",
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                }
            }
            
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = preset.name,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = preset.author ?: "未知作者",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
        }
    }
}

// 数据模型
data class BannerItem(
    val title: String,
    val subtitle: String,
    val imageUrl: String,
    val gradientColors: List<Color>
)

data class Photographer(
    val id: String,
    val name: String,
    val avatarUrl: String,
    val followers: Int,
    val isVerified: Boolean,
    val bio: String = ""
)

data class PresetCollection(
    val id: String,
    val name: String,
    val description: String,
    val coverImage: String,
    val presetCount: Int,
    val author: String
)
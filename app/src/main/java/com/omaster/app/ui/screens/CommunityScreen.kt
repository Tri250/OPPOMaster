package com.omaster.app.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.omaster.app.model.Work
import com.omaster.app.model.UserProfile
import com.omaster.app.ui.theme.*
import java.util.*

// ============================================
// 社区首页 - 社交核心
// ============================================

@Composable
fun CommunityScreen(
    onWorkClick: (Work) -> Unit = {},
    onProfileClick: (UserProfile) -> Unit = {},
    onCreateWorkClick: () -> Unit = {},
    onMyWorksClick: () -> Unit = {},
    onMyPresetsClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "community_glow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.05f,
        targetValue = 0.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(5000, easing = EaseInOut),
        label = "glow"
    )

    var activeTab by remember { mutableStateOf(0) }
    val tabs = listOf("推荐", "关注", "最新", "挑战")

    Scaffold(
        modifier = modifier,
        topBar = {
            CommunityTopBar(
                onCreateClick = onCreateWorkClick
            )
        },
        floatingActionButton = {
            CreateWorkFab(onClick = onCreateWorkClick)
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            TabRow(
                selectedTabIndex = activeTab,
                containerColor = Color.Transparent,
                divider = {},
                indicator = {}
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = activeTab == index,
                        onClick = { activeTab = index },
                        text = {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = if (activeTab == index) FontWeight.Bold else FontWeight.Medium,
                                color = if (activeTab == index) LightFieldPrimary else LightFieldOnSurfaceVariantDark
                            )
                        }
                    )
                }
            }

            when (activeTab) {
                0 -> FeedSection(onWorkClick, onProfileClick)
                1 -> FollowingSection()
                2 -> LatestSection()
                3 -> ChallengesSection()
            }
        }
    }
}

@Composable
fun CommunityTopBar(
    onCreateClick: () -> Unit
) {
    TopAppBar(
        title = {
            Text(
                text = "社区",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.ExtraBold,
                color = LightFieldPrimary
            )
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Transparent
        ),
        actions = {
            IconButton(onClick = onCreateClick) {
                Icon(
                    imageVector = Icons.Default.AddCircle,
                    contentDescription = "发布",
                    tint = LightFieldPrimary
                )
            }
            Spacer(modifier = Modifier.width(4.dp)
        }
    )
}

@Composable
fun CreateWorkFab(onClick: () -> Unit) {
    FloatingActionButton(
        onClick = onClick,
        containerColor = LightFieldPrimary,
        contentColor = Color.Black,
        shape = RoundedCornerShape(16.dp)
    ) {
        Icon(
            imageVector = Icons.Default.AddAPhoto,
            contentDescription = "发布作品"
        )
    }
}

// ============================================
// 推荐流
// ============================================
@Composable
fun FeedSection(
    onWorkClick: (Work) -> Unit,
    onProfileClick: (UserProfile) -> Unit
) {
    val works = remember { getMockWorks() }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(24.dp),
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        item {
            FeaturedCreators()
        }

        items(works) { work ->
            WorkCard(
                work = work,
                onClick = { onWorkClick(work)
            )
        }
    }
}

@Composable
fun FeaturedCreators() {
    val creators = remember { getMockCreators() }

    Column(
        modifier = Modifier.padding(bottom = 16.dp)
    ) {
        Text(
            text = "推荐创作者",
            style = MaterialTheme.typography.titleLarge,
            color = LightFieldOnSurfaceDark,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp, bottom = 12.dp)
        )

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(creators) { creator ->
            CreatorCard(creator)
        }
    }
}

@Composable
fun CreatorCard(creator: UserProfile) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = GlassMediumDark,
        modifier = Modifier.width(140.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(16.dp)
        ) {
            Surface(
            shape = CircleShape,
            color = LightFieldPrimary.copy(alpha = 0.2f),
            modifier = Modifier.size(64.dp)
        ) {
                if (creator.avatar?.let { 
                    AsyncImage(
                        model = it,
                        contentDescription = creator.name,
                        contentScale = ContentScale.Crop
                    )
                } ?: run {
                    Box(
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = creator.name.first().toString(),
                            style = MaterialTheme.typography.headlineMedium,
                            color = LightFieldPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = creator.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = LightFieldOnSurfaceDark,
                maxLines = 1
            )
            Text(
                text = "${creator.followers} 粉丝",
                style = MaterialTheme.typography.bodySmall,
                color = LightFieldOnSurfaceVariantDark
            )
        }
    }
}

// ============================================
// 作品卡片
// ============================================
@Composable
fun WorkCard(
    work: Work,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = GlassMediumDark
        ),
        onClick = onClick
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(320.dp)
            ) {
                AsyncImage(
                    model = work.photoUrl,
                    contentDescription = work.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                Surface(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp),
                    shape = RoundedCornerShape(100),
                    color = Color.Black.copy(alpha = 0.5f)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        if (work.presetId != null) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "使用预设",
                                tint = LightFieldPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = work.presetName ?: "预设",
                                style = MaterialTheme.typography.labelSmall,
                                color = LightFieldPrimary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = LightFieldPrimary.copy(alpha = 0.2f),
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = work.userName.first().toString(),
                                style = MaterialTheme.typography.titleMedium,
                                color = LightFieldPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = work.userName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = LightFieldOnSurfaceDark
                        )
                        Text(
                            text = work.deviceModel,
                            style = MaterialTheme.typography.bodySmall,
                            color = LightFieldOnSurfaceVariantDark
                        )
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    if (work.isVerified) {
                        Surface(
                            shape = RoundedCornerShape(100),
                            color = OppoGreen.copy(alpha = 0.15f),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "认证",
                                style = MaterialTheme.typography.labelSmall,
                                color = OppoGreen,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                if (work.title.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = work.title,
                        style = MaterialTheme.typography.bodyLarge,
                        color = LightFieldOnSurfaceDark,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (work.isLikedByMe) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "点赞",
                            tint = if (work.isLikedByMe) ErrorPrimary else LightFieldOnSurfaceVariantDark,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = work.likeCount.toString(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = LightFieldOnSurfaceVariantDark
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.ChatBubbleOutline,
                            contentDescription = "评论",
                            tint = LightFieldOnSurfaceVariantDark,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = work.commentCount.toString(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = LightFieldOnSurfaceVariantDark
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "分享",
                            tint = LightFieldOnSurfaceVariantDark,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = work.shareCount.toString(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = LightFieldOnSurfaceVariantDark
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FollowingSection() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.PersonAdd,
                contentDescription = null,
                tint = LightFieldOnSurfaceVariantDark,
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "关注更多创作者",
                style = MaterialTheme.typography.titleMedium,
                color = LightFieldOnSurfaceDark
            )
        }
    }
}

@Composable
fun LatestSection() {
    val works = remember { getMockWorks() }

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(works) { work ->
            WorkThumbnail(work)
        }
    }
}

@Composable
fun WorkThumbnail(work: Work) {
    Card(
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.aspectRatio(1f)
    ) {
        Box {
            AsyncImage(
                model = work.photoUrl,
                contentDescription = work.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp),
                shape = RoundedCornerShape(100),
                color = Color.Black.copy(alpha = 0.5f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = work.likeCount.toString(),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
fun ChallengesSection() {
    Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        ChallengeCard(
            title = "哈苏色彩挑战",
            description = "用HNCS预设拍一张风光照",
            prize = "Find X8 Pro"
        )
        ChallengeCard(
            title = "夜景大师赛",
            description = "展示你的夜景作品",
            prize = "哈苏周边"
        )
    }
}

@Composable
fun ChallengeCard(title: String, description: String, prize: String) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = GlassMediumDark
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = LightFieldPrimary.copy(alpha = 0.15f)
            ) {
                Text(
                    text = "进行中",
                    style = MaterialTheme.typography.labelSmall,
                    color = LightFieldPrimary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = LightFieldOnSurfaceDark,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = LightFieldOnSurfaceVariantDark
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.EmojiEvents,
                    contentDescription = "奖品",
                    tint = OppoGreen
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "奖品: $prize",
                    style = MaterialTheme.typography.bodyMedium,
                    color = OppoGreen,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

// Mock数据
private fun getMockWorks(): List<Work> = listOf(
    Work(
        id = "1",
        userId = "u1",
        userName = "摄影大师",
        photoUrl = "https://images.unsplash.com/photo-1506905925346-21bda4d32df4?w=800",
        title = "日落时分的城市",
        description = "使用哈苏自然色彩预设",
        presetId = "hncs_natural",
        presetName = "HNCS自然",
        deviceModel = "Find X8 Ultra",
        likeCount = 1284,
        commentCount = 89,
        shareCount = 234,
        isLikedByMe = true,
        isVerified = true
    ),
    Work(
        id = "2",
        userId = "u2",
        userName = "色彩猎人",
        photoUrl = "https://images.unsplash.com/photo-1493246507139-91e8fad9978e?w=800",
        title = "街头随拍",
        description = "胶片质感预设",
        presetId = "film_kodachrome",
        presetName = "Kodachrome",
        deviceModel = "Reno12 Pro",
        likeCount = 856,
        commentCount = 45,
        shareCount = 123
    )
)

private fun getMockCreators(): List<UserProfile> = listOf(
    UserProfile(
        id = "c1",
        name = "哈苏大师",
        bio = "哈苏认证摄影师",
        followers = 52800,
        worksCount = 320,
        presetsCount = 12,
        isVerified = true,
        isHncsCreator = true
    ),
    UserProfile(
        id = "c2",
        name = "色彩探索者",
        bio = "Find X用户",
        followers = 12500,
        worksCount = 156,
        presetsCount = 8
    )
)

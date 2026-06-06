package com.omaster.app.presentation.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.omaster.app.data.CommunityRepository
import com.omaster.app.domain.model.Comment
import com.omaster.app.domain.model.CommentPageResult
import com.omaster.app.domain.model.LikeTargetType
import com.omaster.app.domain.model.Rating
import com.omaster.app.domain.model.RatingDistribution
import com.omaster.app.domain.model.UserSubmission
import com.omaster.app.presentation.theme.*
import kotlinx.coroutines.launch

/**
 * 预设详情社区部分
 * 展示投稿详情、评分组件、评论列表、点赞功能
 *
 * @param submissionId 投稿ID
 * @param onBackClick 返回按钮回调
 * @param onAuthorClick 点击作者回调
 * @param onDownloadClick 下载预设回调
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun PresetDetailCommunityScreen(
    submissionId: String,
    onBackClick: () -> Unit,
    onAuthorClick: (String) -> Unit,
    onDownloadClick: (String) -> Unit,
    viewModel: PresetDetailViewModel = hiltViewModel()
) {
    val submission by viewModel.submission.collectAsState()
    val comments by viewModel.comments.collectAsState()
    val ratings by viewModel.ratings.collectAsState()
    val ratingDistribution by viewModel.ratingDistribution.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val hasMoreComments by viewModel.hasMoreComments.collectAsState()

    // 加载数据
    LaunchedEffect(submissionId) {
        viewModel.loadSubmissionDetail(submissionId)
        viewModel.loadComments(submissionId)
        viewModel.loadRatings(submissionId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("预设详情", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "返回",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DeepSpace
                )
            )
        },
        bottomBar = {
            submission?.let { sub ->
                DetailBottomBar(
                    submission = sub,
                    onLikeClick = { viewModel.toggleLike(submissionId) },
                    onDownloadClick = { onDownloadClick(submissionId) },
                    onCommentClick = { /* 滚动到评论区 */ }
                )
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(DeepSpace)
        ) {
            submission?.let { sub ->
                // 样张轮播
                item {
                    SampleImagesCarousel(
                        images = sub.sampleImages.map { it.imageUrl },
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                }

                // 预设信息
                item {
                    PresetInfoSection(
                        submission = sub,
                        onAuthorClick = { onAuthorClick(sub.authorId) }
                    )
                }

                // 评分区域
                item {
                    RatingSection(
                        submission = sub,
                        ratingDistribution = ratingDistribution,
                        userRating = ratings.find { it.userId == viewModel.currentUserId }?.rating,
                        onRatingSubmit = { rating, comment ->
                            viewModel.submitRating(submissionId, rating, comment)
                        }
                    )
                }

                // 评论区域标题
                item {
                    CommentSectionHeader(
                        commentCount = sub.ratingCount // 使用评分数作为评论数参考
                    )
                }

                // 评论列表
                items(comments) { comment ->
                    CommentItem(
                        comment = comment,
                        onLikeClick = { viewModel.toggleCommentLike(comment.id) },
                        onReplyClick = { /* 回复评论 */ }
                    )
                }

                // 加载更多评论
                if (hasMoreComments) {
                    item {
                        LoadMoreCommentsButton(
                            onClick = { viewModel.loadMoreComments(submissionId) }
                        )
                    }
                }

                // 发表评论
                item {
                    PostCommentSection(
                        onSubmit = { content ->
                            viewModel.postComment(submissionId, content)
                        }
                    )
                }

                // 底部留白
                item {
                    Spacer(modifier = Modifier.height(100.dp))
                }
            }

            if (isLoading && submission == null) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(400.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = HasselbladOrange)
                    }
                }
            }
        }
    }
}

/**
 * 样张轮播组件
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SampleImagesCarousel(
    images: List<String>,
    modifier: Modifier = Modifier
) {
    if (images.isEmpty()) return

    val pagerState = rememberPagerState(pageCount = { images.size })

    Box(modifier = modifier.fillMaxWidth()) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
        ) { page ->
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(images[page])
                    .crossfade(true)
                    .build(),
                contentDescription = "样张 ${page + 1}",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }

        // 页码指示器
        if (images.size > 1) {
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 16.dp),
                shape = RoundedCornerShape(16.dp),
                color = Color.Black.copy(alpha = 0.6f)
            ) {
                Text(
                    text = "${pagerState.currentPage + 1} / ${images.size}",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    fontSize = 12.sp,
                    color = Color.White
                )
            }
        }
    }
}

/**
 * 预设信息区域
 */
@Composable
private fun PresetInfoSection(
    submission: UserSubmission,
    onAuthorClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // 预设名称
        Text(
            text = submission.preset.name,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        Spacer(modifier = Modifier.height(8.dp))

        // 设备型号和场景类型
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = HasselbladOrange.copy(alpha = 0.2f)
            ) {
                Text(
                    text = submission.preset.deviceModel,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    fontSize = 12.sp,
                    color = HasselbladOrange
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            if (submission.preset.sceneType.isNotEmpty()) {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = ColorOSGrey700
                ) {
                    Text(
                        text = submission.preset.getSceneTypeDisplay(),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        fontSize = 12.sp,
                        color = ColorOSTextSecondary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 投稿描述
        Text(
            text = submission.description,
            fontSize = 14.sp,
            color = ColorOSTextSecondary,
            lineHeight = 22.sp
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 作者信息
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onAuthorClick),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 作者头像
            AsyncImage(
                model = submission.authorAvatar,
                contentDescription = submission.authorName,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = submission.authorName,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White
                )
                Text(
                    text = "发布于 ${submission.getFormattedSubmitTime()}",
                    fontSize = 12.sp,
                    color = ColorOSTextTertiary
                )
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = ColorOSTextTertiary
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 标签
        if (submission.tags.isNotEmpty()) {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                submission.tags.forEach { tag ->
                    TagChip(tag = tag)
                }
            }
        }

        Divider(
            modifier = Modifier.padding(vertical = 16.dp),
            color = ColorOSBorder
        )
    }
}

/**
 * 标签芯片
 */
@Composable
private fun TagChip(tag: String) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = ColorOSGrey800
    ) {
        Text(
            text = "#$tag",
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            fontSize = 12.sp,
            color = ColorOSTextSecondary
        )
    }
}

/**
 * 评分区域
 */
@Composable
private fun RatingSection(
    submission: UserSubmission,
    ratingDistribution: RatingDistribution?,
    userRating: Float?,
    onRatingSubmit: (Float, String) -> Unit
) {
    var showRatingDialog by remember { mutableStateOf(false) }
    var selectedRating by remember { mutableIntStateOf(0) }
    var ratingComment by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 左侧：平均评分
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "%.1f".format(submission.rating),
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Bold,
                    color = HasselbladOrange
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    StarRating(
                        rating = submission.rating,
                        starSize = 16.dp
                    )
                    Text(
                        text = "${submission.ratingCount} 人评分",
                        fontSize = 12.sp,
                        color = ColorOSTextTertiary
                    )
                }
            }

            // 右侧：评分按钮
            Button(
                onClick = { showRatingDialog = true },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (userRating != null) ColorOSGrey700 else HasselbladOrange,
                    contentColor = if (userRating != null) ColorOSTextSecondary else Color.Black
                ),
                shape = RoundedCornerShape(20.dp)
            ) {
                Text(if (userRating != null) "修改评分" else "我要评分")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 评分分布
        ratingDistribution?.let { distribution ->
            if (distribution.getTotalCount() > 0) {
                RatingDistributionBars(distribution = distribution)
            }
        }

        Divider(
            modifier = Modifier.padding(vertical = 16.dp),
            color = ColorOSBorder
        )
    }

    // 评分对话框
    if (showRatingDialog) {
        AlertDialog(
            onDismissRequest = { showRatingDialog = false },
            title = { Text("为预设评分", color = Color.White) },
            text = {
                Column {
                    Text(
                        text = "点击星星进行评分",
                        fontSize = 14.sp,
                        color = ColorOSTextSecondary
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // 五星评分选择
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        for (i in 1..5) {
                            val interactionSource = remember { MutableInteractionSource() }
                            val isPressed by interactionSource.collectIsPressedAsState()
                            val scale by animateFloatAsState(
                                targetValue = if (isPressed) 1.3f else 1f,
                                label = "star_scale"
                            )

                            Icon(
                                imageVector = if (i <= selectedRating) Icons.Default.Star else Icons.Default.StarBorder,
                                contentDescription = "$i 星",
                                modifier = Modifier
                                    .size(40.dp)
                                    .scale(scale)
                                    .clickable(
                                        interactionSource = interactionSource,
                                        indication = null
                                    ) {
                                        selectedRating = i
                                    },
                                tint = if (i <= selectedRating) Color(0xFFFFD700) else ColorOSGrey600
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 评分评论输入
                    OutlinedTextField(
                        value = ratingComment,
                        onValueChange = { ratingComment = it },
                        placeholder = { Text("写下你的评价（可选）", color = ColorOSTextTertiary) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = HasselbladOrange,
                            unfocusedBorderColor = ColorOSBorder,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        maxLines = 3
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (selectedRating > 0) {
                            onRatingSubmit(selectedRating.toFloat(), ratingComment)
                            showRatingDialog = false
                            selectedRating = 0
                            ratingComment = ""
                        }
                    },
                    enabled = selectedRating > 0
                ) {
                    Text("提交", color = if (selectedRating > 0) HasselbladOrange else ColorOSTextTertiary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRatingDialog = false }) {
                    Text("取消", color = ColorOSTextSecondary)
                }
            },
            containerColor = ColorOSCard
        )
    }
}

/**
 * 星级评分显示组件
 */
@Composable
private fun StarRating(
    rating: Float,
    starSize: androidx.compose.ui.unit.Dp = 16.dp,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier) {
        for (i in 1..5) {
            val icon = when {
                i <= rating.toInt() -> Icons.Default.Star
                i - 0.5f <= rating -> Icons.Default.StarHalf
                else -> Icons.Default.StarBorder
            }

            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(starSize),
                tint = Color(0xFFFFD700)
            )
        }
    }
}

/**
 * 评分分布条形图
 */
@Composable
private fun RatingDistributionBars(distribution: RatingDistribution) {
    val total = distribution.getTotalCount()

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        for (star in 5 downTo 1) {
            val percentage = distribution.getPercentage(star)
            val count = when (star) {
                5 -> distribution.fiveStar
                4 -> distribution.fourStar
                3 -> distribution.threeStar
                2 -> distribution.twoStar
                else -> distribution.oneStar
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$star 星",
                    fontSize = 11.sp,
                    color = ColorOSTextTertiary,
                    modifier = Modifier.width(32.dp)
                )

                Spacer(modifier = Modifier.width(8.dp))

                // 进度条
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(ColorOSGrey700)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(percentage / 100f)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(
                                when (star) {
                                    5 -> AuroraGreen
                                    4 -> AuroraGreenLight
                                    3 -> WarningPro
                                    2 -> SunsetRedLight
                                    else -> SunsetRed
                                }
                            )
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = "$count",
                    fontSize = 11.sp,
                    color = ColorOSTextTertiary,
                    modifier = Modifier.width(32.dp),
                    textAlign = TextAlign.End
                )
            }
        }
    }
}

/**
 * 评论区标题
 */
@Composable
private fun CommentSectionHeader(commentCount: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "评论",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = "($commentCount)",
            fontSize = 14.sp,
            color = ColorOSTextTertiary
        )
    }
}

/**
 * 评论项
 */
@Composable
private fun CommentItem(
    comment: Comment,
    onLikeClick: () -> Unit,
    onReplyClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            crossAxisAlignment = androidx.compose.ui.CrossAxisAlignment.Start
        ) {
            // 用户头像
            AsyncImage(
                model = comment.userAvatar,
                contentDescription = comment.userName,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                // 用户名和时间
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = comment.userName,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = comment.getRelativeTime(),
                        fontSize = 12.sp,
                        color = ColorOSTextTertiary
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // 评论内容
                Text(
                    text = comment.getDisplayContent(),
                    fontSize = 14.sp,
                    color = if (comment.isDeleted) ColorOSTextTertiary else ColorOSTextSecondary,
                    fontStyle = if (comment.isDeleted) androidx.compose.ui.text.font.FontStyle.Italic else androidx.compose.ui.text.font.FontStyle.Normal
                )

                Spacer(modifier = Modifier.height(8.dp))

                // 操作按钮
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 点赞
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable(onClick = onLikeClick)
                    ) {
                        Icon(
                            imageVector = if (comment.isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "点赞",
                            modifier = Modifier.size(16.dp),
                            tint = if (comment.isLiked) SunsetRed else ColorOSTextTertiary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (comment.likeCount > 0) comment.likeCount.toString() else "点赞",
                            fontSize = 12.sp,
                            color = if (comment.isLiked) SunsetRed else ColorOSTextTertiary
                        )
                    }

                    Spacer(modifier = Modifier.width(24.dp))

                    // 回复
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable(onClick = onReplyClick)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Reply,
                            contentDescription = "回复",
                            modifier = Modifier.size(16.dp),
                            tint = ColorOSTextTertiary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "回复",
                            fontSize = 12.sp,
                            color = ColorOSTextTertiary
                        )
                    }
                }

                // 子评论（回复）
                if (comment.replies.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = ColorOSGrey800
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp)
                        ) {
                            comment.replies.take(3).forEach { reply ->
                                ReplyItem(reply = reply)
                                if (reply != comment.replies.last()) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                }
                            }

                            if (comment.replies.size > 3) {
                                Text(
                                    text = "查看全部 ${comment.replies.size} 条回复",
                                    fontSize = 12.sp,
                                    color = HasselbladOrange,
                                    modifier = Modifier
                                        .padding(top = 8.dp)
                                        .clickable { /* 展开更多回复 */ }
                                )
                            }
                        }
                    }
                }
            }
        }

        Divider(
            modifier = Modifier.padding(top = 12.dp),
            color = ColorOSBorder
        )
    }
}

/**
 * 回复项
 */
@Composable
private fun ReplyItem(reply: Comment) {
    Row {
        Text(
            text = reply.userName,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = ColorOSTextSecondary
        )

        if (reply.replyToUserName != null) {
            Text(
                text = " 回复 ",
                fontSize = 12.sp,
                color = ColorOSTextTertiary
            )
            Text(
                text = reply.replyToUserName,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = ColorOSTextSecondary
            )
        }

        Text(
            text = ": ${reply.getDisplayContent()}",
            fontSize = 12.sp,
            color = ColorOSTextSecondary
        )
    }
}

/**
 * 加载更多评论按钮
 */
@Composable
private fun LoadMoreCommentsButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        TextButton(onClick = onClick) {
            Text(
                text = "加载更多评论",
                fontSize = 14.sp,
                color = HasselbladOrange
            )
        }
    }
}

/**
 * 发表评论区域
 */
@Composable
private fun PostCommentSection(onSubmit: (String) -> Unit) {
    var commentText by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(
            text = "发表评论",
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = Color.White
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = commentText,
            onValueChange = { commentText = it },
            placeholder = { Text("分享你的想法...", color = ColorOSTextTertiary) },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = HasselbladOrange,
                unfocusedBorderColor = ColorOSBorder,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            ),
            maxLines = 4,
            trailingIcon = {
                if (commentText.isNotEmpty()) {
                    IconButton(
                        onClick = {
                            onSubmit(commentText)
                            commentText = ""
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = "发送",
                            tint = HasselbladOrange
                        )
                    }
                }
            }
        )
    }
}

/**
 * 底部操作栏
 */
@Composable
private fun DetailBottomBar(
    submission: UserSubmission,
    onLikeClick: () -> Unit,
    onDownloadClick: () -> Unit,
    onCommentClick: () -> Unit
) {
    Surface(
        color = ColorOSCard,
        tonalElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 点赞按钮
            IconButton(onClick = onLikeClick) {
                Icon(
                    imageVector = if (submission.isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "点赞",
                    tint = if (submission.isLiked) SunsetRed else ColorOSTextSecondary
                )
            }

            Text(
                text = submission.likeCount.toString(),
                fontSize = 14.sp,
                color = if (submission.isLiked) SunsetRed else ColorOSTextSecondary
            )

            Spacer(modifier = Modifier.width(16.dp))

            // 评论按钮
            IconButton(onClick = onCommentClick) {
                Icon(
                    imageVector = Icons.Default.Comment,
                    contentDescription = "评论",
                    tint = ColorOSTextSecondary
                )
            }

            Text(
                text = submission.ratingCount.toString(),
                fontSize = 14.sp,
                color = ColorOSTextSecondary
            )

            Spacer(modifier = Modifier.weight(1f))

            // 下载按钮
            Button(
                onClick = onDownloadClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = HasselbladOrange,
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(20.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Download,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("下载预设")
            }
        }
    }
}

/**
 * FlowRow 布局组件
 */
@Composable
private fun FlowRow(
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    content: @Composable () -> Unit
) {
    Layout(
        content = content,
        modifier = modifier
    ) { measurables, constraints ->
        val hGapPx = 8.dp.roundToPx()
        val vGapPx = 8.dp.roundToPx()

        val rows = mutableListOf<List<androidx.compose.ui.layout.Placeable>>()
        val rowWidths = mutableListOf<Int>()
        val rowHeights = mutableListOf<Int>()

        var currentRow = mutableListOf<androidx.compose.ui.layout.Placeable>()
        var currentRowWidth = 0
        var currentRowHeight = 0

        measurables.forEach { measurable ->
            val placeable = measurable.measure(constraints)

            if (currentRow.isNotEmpty() &&
                currentRowWidth + hGapPx + placeable.width > constraints.maxWidth
            ) {
                rows.add(currentRow)
                rowWidths.add(currentRowWidth)
                rowHeights.add(currentRowHeight)
                currentRow = mutableListOf()
                currentRowWidth = 0
                currentRowHeight = 0
            }

            currentRow.add(placeable)
            currentRowWidth += if (currentRow.size == 1) placeable.width else hGapPx + placeable.width
            currentRowHeight = maxOf(currentRowHeight, placeable.height)
        }

        if (currentRow.isNotEmpty()) {
            rows.add(currentRow)
            rowWidths.add(currentRowWidth)
            rowHeights.add(currentRowHeight)
        }

        val width = constraints.maxWidth
        val height = rowHeights.sum() + (rowHeights.size - 1).coerceAtLeast(0) * vGapPx

        layout(width, height) {
            var y = 0
            rows.forEachIndexed { rowIndex, row ->
                var x = when (horizontalArrangement) {
                    Arrangement.End -> width - rowWidths[rowIndex]
                    Arrangement.Center -> (width - rowWidths[rowIndex]) / 2
                    else -> 0
                }

                row.forEach { placeable ->
                    placeable.placeRelative(x, y)
                    x += placeable.width + hGapPx
                }

                y += rowHeights[rowIndex] + vGapPx
            }
        }
    }
}

/**
 * 预设详情 ViewModel
 */
@ androidx.lifecycle.ViewModel
class PresetDetailViewModel @javax.inject.Inject constructor(
    private val communityRepository: CommunityRepository
) : androidx.lifecycle.ViewModel() {

    private val _submission = MutableStateFlow<UserSubmission?>(null)
    val submission: StateFlow<UserSubmission?> = _submission.asStateFlow()

    private val _comments = MutableStateFlow<List<Comment>>(emptyList())
    val comments: StateFlow<List<Comment>> = _comments.asStateFlow()

    private val _ratings = MutableStateFlow<List<Rating>>(emptyList())
    val ratings: StateFlow<List<Rating>> = _ratings.asStateFlow()

    private val _ratingDistribution = MutableStateFlow<RatingDistribution?>(null)
    val ratingDistribution: StateFlow<RatingDistribution?> = _ratingDistribution.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _hasMoreComments = MutableStateFlow(true)
    val hasMoreComments: StateFlow<Boolean> = _hasMoreComments.asStateFlow()

    private var commentPage = 1
    private val commentPageSize = 20

    val currentUserId: String
        get() = "current_user" // 实际应从用户管理获取

    fun loadSubmissionDetail(submissionId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            communityRepository.getSubmissionDetail(submissionId).collect { result ->
                result.onSuccess { submission ->
                    _submission.value = submission
                }
                _isLoading.value = false
            }
        }
    }

    fun loadComments(submissionId: String) {
        viewModelScope.launch {
            communityRepository.getComments(submissionId, commentPage, commentPageSize)
                .collect { result ->
                    result.onSuccess { pageResult ->
                        _comments.value = pageResult.comments
                        _hasMoreComments.value = pageResult.hasMore
                    }
                }
        }
    }

    fun loadMoreComments(submissionId: String) {
        if (!_hasMoreComments.value) return
        commentPage++
        loadComments(submissionId)
    }

    fun loadRatings(submissionId: String) {
        viewModelScope.launch {
            communityRepository.getRatings(submissionId).collect { result ->
                result.onSuccess { ratings ->
                    _ratings.value = ratings
                }
            }
        }

        viewModelScope.launch {
            communityRepository.getRatingDistribution(submissionId).collect { result ->
                result.onSuccess { distribution ->
                    _ratingDistribution.value = distribution
                }
            }
        }
    }

    fun toggleLike(submissionId: String) {
        viewModelScope.launch {
            communityRepository.toggleLike(submissionId, LikeTargetType.SUBMISSION)
                .onSuccess { isLiked ->
                    _submission.value = _submission.value?.copy(
                        isLiked = isLiked,
                        likeCount = if (isLiked) (_submission.value?.likeCount ?: 0) + 1
                        else maxOf(0, (_submission.value?.likeCount ?: 0) - 1)
                    )
                }
        }
    }

    fun toggleCommentLike(commentId: String) {
        viewModelScope.launch {
            communityRepository.toggleLike(commentId, LikeTargetType.COMMENT)
                .onSuccess { isLiked ->
                    _comments.value = _comments.value.map { comment ->
                        if (comment.id == commentId) {
                            comment.copy(
                                isLiked = isLiked,
                                likeCount = if (isLiked) comment.likeCount + 1
                                else maxOf(0, comment.likeCount - 1)
                            )
                        } else comment
                    }
                }
        }
    }

    fun submitRating(submissionId: String, rating: Float, comment: String) {
        viewModelScope.launch {
            communityRepository.addRating(submissionId, rating, comment)
                .onSuccess {
                    loadRatings(submissionId)
                    loadSubmissionDetail(submissionId)
                }
        }
    }

    fun postComment(submissionId: String, content: String) {
        viewModelScope.launch {
            communityRepository.postComment(submissionId, content)
                .onSuccess {
                    loadComments(submissionId)
                }
        }
    }
}

package com.omaster.app.data

import com.omaster.app.domain.model.Comment
import com.omaster.app.domain.model.CommentPageResult
import com.omaster.app.domain.model.CommunityFilter
import com.omaster.app.domain.model.CommunitySortType
import com.omaster.app.domain.model.CommunityStats
import com.omaster.app.domain.model.Like
import com.omaster.app.domain.model.LikeTargetType
import com.omaster.app.domain.model.Rating
import com.omaster.app.domain.model.RatingDistribution
import com.omaster.app.domain.model.SubmissionImage
import com.omaster.app.domain.model.SubmissionRequest
import com.omaster.app.domain.model.SubmissionStatus
import com.omaster.app.domain.model.SubmissionPageResult
import com.omaster.app.domain.model.UserSubmission
import com.omaster.app.domain.model.CameraParams
import com.omaster.app.domain.model.ColorStyle
import com.omaster.app.domain.model.Preset
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 社区数据仓库
 * 管理用户投稿预设、评分、评论、点赞等社区功能的数据操作
 */
@Singleton
class CommunityRepository @Inject constructor(
    private val preferencesDataStore: PreferencesDataStore
) {
    // 内存缓存
    private val submissionsCache = mutableMapOf<String, UserSubmission>()
    private val commentsCache = mutableMapOf<String, MutableList<Comment>>()
    private val ratingsCache = mutableMapOf<String, MutableList<Rating>>()
    private val likesCache = mutableSetOf<String>() // 存储 "targetId:userId" 格式的点赞记录

    // 状态流
    private val _submissions = MutableStateFlow<List<UserSubmission>>(emptyList())
    val submissions: StateFlow<List<UserSubmission>> = _submissions.asStateFlow()

    private val _communityStats = MutableStateFlow(CommunityStats())
    val communityStats: StateFlow<CommunityStats> = _communityStats.asStateFlow()

    // 协程作用域
    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    // 当前用户ID（实际项目中应从用户登录状态获取）
    private val currentUserId: String
        get() = preferencesDataStore.getUserId() ?: "guest_${System.currentTimeMillis()}"

    private val currentUserName: String
        get() = preferencesDataStore.getUserName() ?: "摄影爱好者"

    init {
        // 初始化示例数据
        repositoryScope.launch {
            initializeSampleData()
        }
    }

    /**
     * 初始化示例数据
     */
    private suspend fun initializeSampleData() {
        val sampleSubmissions = createSampleSubmissions()
        sampleSubmissions.forEach { submission ->
            submissionsCache[submission.id] = submission
        }
        _submissions.value = sampleSubmissions
        updateCommunityStats()
        Timber.d("社区数据初始化完成，共 ${sampleSubmissions.size} 条投稿")
    }

    /**
     * 获取投稿列表
     * @param filter 筛选条件
     * @param page 页码（从1开始）
     * @param pageSize 每页数量
     */
    fun getSubmissions(
        filter: CommunityFilter = CommunityFilter(),
        page: Int = 1,
        pageSize: Int = 20
    ): Flow<Result<SubmissionPageResult>> = flow {
        try {
            // 模拟网络延迟
            delay(300)

            var result = submissionsCache.values.toList()

            // 筛选已通过审核的投稿
            result = result.filter { it.status == SubmissionStatus.APPROVED }

            // 应用筛选条件
            filter.sceneType?.let { sceneType ->
                result = result.filter { it.preset.sceneType == sceneType }
            }

            filter.deviceModel?.let { deviceModel ->
                result = result.filter { it.preset.deviceModel.contains(deviceModel, ignoreCase = true) }
            }

            filter.tag?.let { tag ->
                result = result.filter { submission ->
                    submission.tags.any { it.contains(tag, ignoreCase = true) } ||
                    submission.preset.tags.any { it.contains(tag, ignoreCase = true) }
                }
            }

            filter.timeRange?.let { days ->
                val cutoffTime = System.currentTimeMillis() - days * 24 * 60 * 60 * 1000
                result = result.filter { it.submitTime >= cutoffTime }
            }

            if (filter.onlyFeatured) {
                result = result.filter { it.isFeatured }
            }

            // 排序
            result = when (filter.sortType) {
                CommunitySortType.LATEST -> result.sortedByDescending { it.submitTime }
                CommunitySortType.HOTTEST -> result.sortedByDescending { it.getHeatScore() }
                CommunitySortType.HIGHEST_RATED -> result.sortedByDescending { it.rating }
            }

            // 分页
            val totalCount = result.size
            val startIndex = (page - 1) * pageSize
            val endIndex = minOf(startIndex + pageSize, totalCount)
            val pagedResult = if (startIndex < totalCount) {
                result.subList(startIndex, endIndex)
            } else {
                emptyList()
            }

            val pageResult = SubmissionPageResult(
                submissions = pagedResult,
                hasMore = endIndex < totalCount,
                totalCount = totalCount
            )

            emit(Result.success(pageResult))
            Timber.d("获取投稿列表成功: 第$page 页, 共 ${pagedResult.size} 条")
        } catch (e: Exception) {
            Timber.e(e, "获取投稿列表失败")
            emit(Result.failure(e))
        }
    }

    /**
     * 获取投稿详情
     */
    fun getSubmissionDetail(submissionId: String): Flow<Result<UserSubmission>> = flow {
        try {
            delay(200)
            val submission = submissionsCache[submissionId]
            if (submission != null) {
                // 增加浏览次数
                val updated = submission.copy(viewCount = submission.viewCount + 1)
                submissionsCache[submissionId] = updated
                emit(Result.success(updated))
            } else {
                emit(Result.failure(Exception("投稿不存在")))
            }
        } catch (e: Exception) {
            Timber.e(e, "获取投稿详情失败: $submissionId")
            emit(Result.failure(e))
        }
    }

    /**
     * 提交新投稿
     */
    suspend fun submitPreset(request: SubmissionRequest): Result<UserSubmission> {
        return try {
            delay(1000) // 模拟上传延迟

            // 创建预设
            val preset = Preset(
                id = "submission_${System.currentTimeMillis()}",
                name = request.presetName,
                coverPath = request.sampleImages.firstOrNull() ?: "",
                deviceModel = request.deviceModel,
                author = currentUserName,
                description = request.description,
                cameraParams = request.cameraParams,
                tags = request.tags,
                source = "community"
            )

            // 创建样张图片列表
            val sampleImages = request.sampleImages.map { path ->
                SubmissionImage(imageUrl = path)
            }

            // 创建投稿
            val submission = UserSubmission(
                preset = preset,
                authorId = currentUserId,
                authorName = currentUserName,
                description = request.description,
                sampleImages = sampleImages,
                status = SubmissionStatus.PENDING,
                tags = request.tags
            )

            // 保存到缓存
            submissionsCache[submission.id] = submission
            _submissions.value = submissionsCache.values.toList()
            updateCommunityStats()

            Timber.d("投稿成功: ${submission.id}")
            Result.success(submission)
        } catch (e: Exception) {
            Timber.e(e, "投稿失败")
            Result.failure(e)
        }
    }

    /**
     * 保存草稿
     */
    suspend fun saveDraft(request: SubmissionRequest): Result<UserSubmission> {
        return try {
            val preset = Preset(
                id = "draft_${System.currentTimeMillis()}",
                name = request.presetName,
                coverPath = request.sampleImages.firstOrNull() ?: "",
                deviceModel = request.deviceModel,
                author = currentUserName,
                description = request.description,
                cameraParams = request.cameraParams,
                tags = request.tags,
                source = "community"
            )

            val sampleImages = request.sampleImages.map { path ->
                SubmissionImage(imageUrl = path)
            }

            val submission = UserSubmission(
                preset = preset,
                authorId = currentUserId,
                authorName = currentUserName,
                description = request.description,
                sampleImages = sampleImages,
                status = SubmissionStatus.DRAFT,
                tags = request.tags
            )

            submissionsCache[submission.id] = submission
            _submissions.value = submissionsCache.values.toList()

            Timber.d("草稿保存成功: ${submission.id}")
            Result.success(submission)
        } catch (e: Exception) {
            Timber.e(e, "保存草稿失败")
            Result.failure(e)
        }
    }

    /**
     * 切换点赞状态
     */
    suspend fun toggleLike(targetId: String, targetType: LikeTargetType): Result<Boolean> {
        return try {
            val likeKey = "$targetId:$currentUserId"
            val isLiked = likesCache.contains(likeKey)

            if (isLiked) {
                // 取消点赞
                likesCache.remove(likeKey)
                updateLikeCount(targetId, targetType, -1)
                Timber.d("取消点赞: $targetId")
            } else {
                // 添加点赞
                likesCache.add(likeKey)
                val like = Like(targetId = targetId, targetType = targetType, userId = currentUserId)
                updateLikeCount(targetId, targetType, 1)
                Timber.d("点赞成功: $targetId")
            }

            Result.success(!isLiked)
        } catch (e: Exception) {
            Timber.e(e, "点赞操作失败")
            Result.failure(e)
        }
    }

    /**
     * 更新点赞数
     */
    private fun updateLikeCount(targetId: String, targetType: LikeTargetType, delta: Int) {
        when (targetType) {
            LikeTargetType.SUBMISSION -> {
                submissionsCache[targetId]?.let { submission ->
                    val updated = submission.copy(likeCount = maxOf(0, submission.likeCount + delta))
                    submissionsCache[targetId] = updated
                    _submissions.value = submissionsCache.values.toList()
                }
            }
            LikeTargetType.COMMENT -> {
                commentsCache.values.forEach { commentList ->
                    commentList.find { it.id == targetId }?.let { comment ->
                        val index = commentList.indexOf(comment)
                        val updated = comment.copy(likeCount = maxOf(0, comment.likeCount + delta))
                        commentList[index] = updated
                    }
                }
            }
        }
    }

    /**
     * 检查是否已点赞
     */
    fun isLiked(targetId: String): Boolean {
        return likesCache.contains("$targetId:$currentUserId")
    }

    /**
     * 添加评分
     */
    suspend fun addRating(submissionId: String, rating: Float, comment: String = ""): Result<Rating> {
        return try {
            require(rating in 1.0f..5.0f) { "评分必须在1-5之间" }

            val ratingObj = Rating(
                submissionId = submissionId,
                userId = currentUserId,
                userName = currentUserName,
                rating = rating,
                comment = comment
            )

            // 保存评分
            val ratings = ratingsCache.getOrPut(submissionId) { mutableListOf() }
            ratings.removeAll { it.userId == currentUserId } // 移除旧评分
            ratings.add(ratingObj)

            // 更新投稿的平均评分
            submissionsCache[submissionId]?.let { submission ->
                val allRatings = ratingsCache[submissionId] ?: emptyList()
                val avgRating = if (allRatings.isNotEmpty()) {
                    allRatings.map { it.rating }.average().toFloat()
                } else 0f

                val updated = submission.copy(
                    rating = avgRating,
                    ratingCount = allRatings.size
                )
                submissionsCache[submissionId] = updated
                _submissions.value = submissionsCache.values.toList()
            }

            Timber.d("评分成功: $submissionId, 评分: $rating")
            Result.success(ratingObj)
        } catch (e: Exception) {
            Timber.e(e, "评分失败")
            Result.failure(e)
        }
    }

    /**
     * 获取评分列表
     */
    fun getRatings(submissionId: String): Flow<Result<List<Rating>>> = flow {
        try {
            val ratings = ratingsCache[submissionId] ?: emptyList()
            emit(Result.success(ratings.sortedByDescending { it.createTime }))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    /**
     * 获取评分分布
     */
    fun getRatingDistribution(submissionId: String): Flow<Result<RatingDistribution>> = flow {
        try {
            val ratings = ratingsCache[submissionId] ?: emptyList()
            val distribution = RatingDistribution(
                fiveStar = ratings.count { it.rating >= 4.5f },
                fourStar = ratings.count { it.rating >= 3.5f && it.rating < 4.5f },
                threeStar = ratings.count { it.rating >= 2.5f && it.rating < 3.5f },
                twoStar = ratings.count { it.rating >= 1.5f && it.rating < 2.5f },
                oneStar = ratings.count { it.rating < 1.5f }
            )
            emit(Result.success(distribution))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    /**
     * 获取评论列表
     */
    fun getComments(
        submissionId: String,
        page: Int = 1,
        pageSize: Int = 20
    ): Flow<Result<CommentPageResult>> = flow {
        try {
            delay(200)
            val allComments = commentsCache[submissionId] ?: emptyList()

            // 只获取顶级评论
            val topLevelComments = allComments.filter { it.parentId == null }
                .sortedByDescending { it.createTime }

            // 分页
            val totalCount = topLevelComments.size
            val startIndex = (page - 1) * pageSize
            val endIndex = minOf(startIndex + pageSize, totalCount)

            val pagedComments = if (startIndex < totalCount) {
                topLevelComments.subList(startIndex, endIndex).map { comment ->
                    // 添加回复
                    val replies = allComments.filter { it.parentId == comment.id }
                        .sortedBy { it.createTime }
                    comment.copy(replies = replies)
                }
            } else {
                emptyList()
            }

            val result = CommentPageResult(
                comments = pagedComments,
                hasMore = endIndex < totalCount,
                totalCount = totalCount
            )

            emit(Result.success(result))
        } catch (e: Exception) {
            Timber.e(e, "获取评论失败")
            emit(Result.failure(e))
        }
    }

    /**
     * 发表评论
     */
    suspend fun postComment(
        submissionId: String,
        content: String,
        parentId: String? = null,
        replyToUserName: String? = null
    ): Result<Comment> {
        return try {
            require(content.isNotBlank()) { "评论内容不能为空" }
            require(content.length <= 500) { "评论内容不能超过500字" }

            val comment = Comment(
                submissionId = submissionId,
                userId = currentUserId,
                userName = currentUserName,
                content = content,
                parentId = parentId,
                replyToUserName = replyToUserName
            )

            val comments = commentsCache.getOrPut(submissionId) { mutableListOf() }
            comments.add(comment)

            Timber.d("评论发表成功: ${comment.id}")
            Result.success(comment)
        } catch (e: Exception) {
            Timber.e(e, "发表评论失败")
            Result.failure(e)
        }
    }

    /**
     * 删除评论
     */
    suspend fun deleteComment(commentId: String): Result<Boolean> {
        return try {
            var deleted = false
            commentsCache.values.forEach { commentList ->
                val index = commentList.indexOfFirst { it.id == commentId && it.userId == currentUserId }
                if (index != -1) {
                    val comment = commentList[index]
                    commentList[index] = comment.copy(isDeleted = true)
                    deleted = true
                }
            }

            if (deleted) {
                Timber.d("评论删除成功: $commentId")
                Result.success(true)
            } else {
                Result.failure(Exception("评论不存在或无权限删除"))
            }
        } catch (e: Exception) {
            Timber.e(e, "删除评论失败")
            Result.failure(e)
        }
    }

    /**
     * 获取用户的投稿列表
     */
    fun getUserSubmissions(userId: String): Flow<Result<List<UserSubmission>>> = flow {
        try {
            val userSubmissions = submissionsCache.values
                .filter { it.authorId == userId }
                .sortedByDescending { it.submitTime }
            emit(Result.success(userSubmissions.toList()))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    /**
     * 获取当前用户的投稿
     */
    fun getMySubmissions(): Flow<Result<List<UserSubmission>>> {
        return getUserSubmissions(currentUserId)
    }

    /**
     * 删除投稿
     */
    suspend fun deleteSubmission(submissionId: String): Result<Boolean> {
        return try {
            val submission = submissionsCache[submissionId]
            if (submission == null) {
                return Result.failure(Exception("投稿不存在"))
            }
            if (submission.authorId != currentUserId) {
                return Result.failure(Exception("无权删除此投稿"))
            }

            submissionsCache.remove(submissionId)
            _submissions.value = submissionsCache.values.toList()
            updateCommunityStats()

            Timber.d("投稿删除成功: $submissionId")
            Result.success(true)
        } catch (e: Exception) {
            Timber.e(e, "删除投稿失败")
            Result.failure(e)
        }
    }

    /**
     * 更新社区统计
     */
    private fun updateCommunityStats() {
        val allSubmissions = submissionsCache.values.toList()
        val approvedSubmissions = allSubmissions.filter { it.status == SubmissionStatus.APPROVED }
        val todayStart = System.currentTimeMillis() - 24 * 60 * 60 * 1000

        _communityStats.value = CommunityStats(
            totalSubmissions = approvedSubmissions.size,
            totalDownloads = approvedSubmissions.sumOf { it.downloadCount },
            totalUsers = allSubmissions.map { it.authorId }.distinct().size,
            todaySubmissions = approvedSubmissions.count { it.submitTime >= todayStart },
            featuredCount = approvedSubmissions.count { it.isFeatured }
        )
    }

    /**
     * 获取社区统计
     */
    fun getCommunityStats(): Flow<Result<CommunityStats>> = flow {
        try {
            emit(Result.success(_communityStats.value))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    /**
     * 搜索投稿
     */
    fun searchSubmissions(query: String): Flow<Result<List<UserSubmission>>> = flow {
        try {
            delay(300)
            val results = submissionsCache.values.filter { submission ->
                submission.status == SubmissionStatus.APPROVED && (
                    submission.preset.name.contains(query, ignoreCase = true) ||
                    submission.preset.description.contains(query, ignoreCase = true) ||
                    submission.authorName.contains(query, ignoreCase = true) ||
                    submission.tags.any { it.contains(query, ignoreCase = true) } ||
                    submission.preset.tags.any { it.contains(query, ignoreCase = true) }
                )
            }.sortedByDescending { it.getHeatScore() }

            emit(Result.success(results.toList()))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    /**
     * 创建示例投稿数据
     */
    private fun createSampleSubmissions(): List<UserSubmission> {
        return listOf(
            UserSubmission(
                id = "sub_001",
                preset = Preset(
                    id = "preset_001",
                    name = "日系清新人像",
                    coverPath = "https://images.unsplash.com/photo-1517841905240-472988babdf9?w=800&h=600&fit=crop",
                    deviceModel = "OPPO Find X8 Ultra",
                    author = "小清新摄影师",
                    description = "适合日系清新风格的人像摄影，色调柔和自然",
                    sceneType = "portrait",
                    tags = listOf("人像", "日系", "清新"),
                    rating = 4.8f,
                    downloadCount = 3420,
                    source = "community",
                    cameraParams = CameraParams(
                        mode = "人像模式",
                        filter = "自然",
                        iso = 200,
                        shutter = "1/200",
                        ev = "+0.3",
                        wb = "5500K",
                        focalLength = "50mm",
                        colorStyle = ColorStyle.Portrait.name,
                        sharpness = 40,
                        contrast = 45,
                        saturation = 55
                    )
                ),
                authorId = "user_001",
                authorName = "小清新摄影师",
                authorAvatar = "https://api.dicebear.com/7.x/avataaars/svg?seed=user1",
                description = "这款预设专为日系清新风格打造，适合户外人像拍摄，色调柔和自然，肤色呈现健康通透。",
                sampleImages = listOf(
                    SubmissionImage(
                        id = "img_001",
                        imageUrl = "https://images.unsplash.com/photo-1517841905240-472988babdf9?w=800&h=600&fit=crop",
                        title = "样张1",
                        description = "户外人像效果"
                    ),
                    SubmissionImage(
                        id = "img_002",
                        imageUrl = "https://images.unsplash.com/photo-1524504388940-b1c1722653e1?w=800&h=600&fit=crop",
                        title = "样张2",
                        description = "逆光人像效果"
                    )
                ),
                status = SubmissionStatus.APPROVED,
                submitTime = System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000,
                downloadCount = 3420,
                viewCount = 12580,
                likeCount = 892,
                rating = 4.8f,
                ratingCount = 156,
                tags = listOf("人像", "日系", "清新", "自然光"),
                isFeatured = true
            ),
            UserSubmission(
                id = "sub_002",
                preset = Preset(
                    id = "preset_002",
                    name = "赛博朋克夜景",
                    coverPath = "https://images.unsplash.com/photo-1519608487953-e999c86e7455?w=800&h=600&fit=crop",
                    deviceModel = "OPPO Find X8 Ultra",
                    author = "夜景猎人",
                    description = "霓虹灯效果的赛博朋克风格夜景预设",
                    sceneType = "night",
                    tags = listOf("夜景", "赛博朋克", "霓虹"),
                    rating = 4.6f,
                    downloadCount = 2850,
                    source = "community",
                    cameraParams = CameraParams(
                        mode = "夜景模式",
                        filter = "赛博朋克",
                        iso = 1600,
                        shutter = "1/30",
                        ev = "+0.0",
                        wb = "4000K",
                        focalLength = "23mm",
                        colorStyle = ColorStyle.Cinematic.name,
                        sharpness = 60,
                        contrast = 65,
                        saturation = 70
                    )
                ),
                authorId = "user_002",
                authorName = "夜景猎人",
                authorAvatar = "https://api.dicebear.com/7.x/avataaars/svg?seed=user2",
                description = "赛博朋克风格夜景预设，增强霓虹灯效果，营造未来感氛围。",
                sampleImages = listOf(
                    SubmissionImage(
                        id = "img_003",
                        imageUrl = "https://images.unsplash.com/photo-1519608487953-e999c86e7455?w=800&h=600&fit=crop",
                        title = "城市夜景",
                        description = "霓虹灯光效果"
                    )
                ),
                status = SubmissionStatus.APPROVED,
                submitTime = System.currentTimeMillis() - 3 * 24 * 60 * 60 * 1000,
                downloadCount = 2850,
                viewCount = 8920,
                likeCount = 654,
                rating = 4.6f,
                ratingCount = 98,
                tags = listOf("夜景", "赛博朋克", "城市", "霓虹")
            ),
            UserSubmission(
                id = "sub_003",
                preset = Preset(
                    id = "preset_003",
                    name = "胶片复古风",
                    coverPath = "https://images.unsplash.com/photo-1493863641943-9b68992a8d07?w=800&h=600&fit=crop",
                    deviceModel = "OPPO Find X8",
                    author = "胶片爱好者",
                    description = "模拟胶片质感的复古风格预设",
                    sceneType = "street",
                    tags = listOf("胶片", "复古", "街拍"),
                    rating = 4.9f,
                    downloadCount = 5680,
                    source = "community",
                    cameraParams = CameraParams(
                        mode = "专业模式",
                        filter = "胶片",
                        iso = 400,
                        shutter = "1/125",
                        ev = "-0.3",
                        wb = "6000K",
                        focalLength = "35mm",
                        colorStyle = ColorStyle.Classic.name,
                        sharpness = 35,
                        contrast = 55,
                        saturation = 45
                    )
                ),
                authorId = "user_003",
                authorName = "胶片爱好者",
                authorAvatar = "https://api.dicebear.com/7.x/avataaars/svg?seed=user3",
                description = "精心调校的胶片模拟预设，还原经典胶片色彩，带有轻微的颗粒感。",
                sampleImages = listOf(
                    SubmissionImage(
                        id = "img_004",
                        imageUrl = "https://images.unsplash.com/photo-1493863641943-9b68992a8d07?w=800&h=600&fit=crop",
                        title = "街景",
                        description = "胶片质感呈现"
                    ),
                    SubmissionImage(
                        id = "img_005",
                        imageUrl = "https://images.unsplash.com/photo-1449824913935-59a10b8d2000?w=800&h=600&fit=crop",
                        title = "街拍",
                        description = "复古色调"
                    ),
                    SubmissionImage(
                        id = "img_006",
                        imageUrl = "https://images.unsplash.com/photo-1517732306149-e8f829eb588a?w=800&h=600&fit=crop",
                        title = "人像",
                        description = "胶片人像效果"
                    )
                ),
                status = SubmissionStatus.APPROVED,
                submitTime = System.currentTimeMillis() - 14 * 24 * 60 * 60 * 1000,
                downloadCount = 5680,
                viewCount = 18250,
                likeCount = 1456,
                rating = 4.9f,
                ratingCount = 234,
                tags = listOf("胶片", "复古", "街拍", "人像"),
                isFeatured = true
            ),
            UserSubmission(
                id = "sub_004",
                preset = Preset(
                    id = "preset_004",
                    name = "美食探店专用",
                    coverPath = "https://images.unsplash.com/photo-1504674900247-0877df9cc836?w=800&h=600&fit=crop",
                    deviceModel = "OPPO Reno12 Pro",
                    author = "美食博主",
                    description = "提升美食色彩饱和度，让食物更诱人",
                    sceneType = "food",
                    tags = listOf("美食", "探店", "色彩"),
                    rating = 4.5f,
                    downloadCount = 1890,
                    source = "community",
                    cameraParams = CameraParams(
                        mode = "美食模式",
                        filter = "美食",
                        iso = 100,
                        shutter = "1/100",
                        ev = "+0.3",
                        wb = "5000K",
                        focalLength = "50mm",
                        colorStyle = ColorStyle.Food.name,
                        sharpness = 50,
                        contrast = 50,
                        saturation = 70
                    )
                ),
                authorId = "user_004",
                authorName = "美食博主",
                authorAvatar = "https://api.dicebear.com/7.x/avataaars/svg?seed=user4",
                description = "专为美食探店设计的预设，提升色彩饱和度和食欲感，适合餐厅环境拍摄。",
                sampleImages = listOf(
                    SubmissionImage(
                        id = "img_007",
                        imageUrl = "https://images.unsplash.com/photo-1504674900247-0877df9cc836?w=800&h=600&fit=crop",
                        title = "美食",
                        description = "色彩鲜艳"
                    )
                ),
                status = SubmissionStatus.APPROVED,
                submitTime = System.currentTimeMillis() - 1 * 24 * 60 * 60 * 1000,
                downloadCount = 1890,
                viewCount = 4560,
                likeCount = 328,
                rating = 4.5f,
                ratingCount = 67,
                tags = listOf("美食", "探店", "色彩", "餐厅")
            ),
            UserSubmission(
                id = "sub_005",
                preset = Preset(
                    id = "preset_005",
                    name = "风光大片",
                    coverPath = "https://images.unsplash.com/photo-1464822759023-fed622ff2c3b?w=800&h=600&fit=crop",
                    deviceModel = "OPPO Find X8 Ultra",
                    author = "风光摄影师",
                    description = "风景摄影专用预设，色彩鲜明层次丰富",
                    sceneType = "landscape",
                    tags = listOf("风景", "风光", "自然"),
                    rating = 4.7f,
                    downloadCount = 4250,
                    source = "community",
                    cameraParams = CameraParams(
                        mode = "风景模式",
                        filter = "风景",
                        iso = 100,
                        shutter = "1/250",
                        ev = "+0.0",
                        wb = "6500K",
                        focalLength = "16mm",
                        colorStyle = ColorStyle.Vivid.name,
                        sharpness = 55,
                        contrast = 55,
                        saturation = 60
                    )
                ),
                authorId = "user_005",
                authorName = "风光摄影师",
                authorAvatar = "https://api.dicebear.com/7.x/avataaars/svg?seed=user5",
                description = "专为风景摄影设计的预设，增强色彩层次和细节表现，适合山川湖海等自然风光。",
                sampleImages = listOf(
                    SubmissionImage(
                        id = "img_008",
                        imageUrl = "https://images.unsplash.com/photo-1464822759023-fed622ff2c3b?w=800&h=600&fit=crop",
                        title = "山景",
                        description = "层次丰富"
                    ),
                    SubmissionImage(
                        id = "img_009",
                        imageUrl = "https://images.unsplash.com/photo-1433086966358-54859d0ed716?w=800&h=600&fit=crop",
                        title = "瀑布",
                        description = "流水效果"
                    )
                ),
                status = SubmissionStatus.APPROVED,
                submitTime = System.currentTimeMillis() - 5 * 24 * 60 * 60 * 1000,
                downloadCount = 4250,
                viewCount = 11230,
                likeCount = 756,
                rating = 4.7f,
                ratingCount = 145,
                tags = listOf("风景", "风光", "自然", "山川")
            ),
            UserSubmission(
                id = "sub_006",
                preset = Preset(
                    id = "preset_006",
                    name = "黑白纪实",
                    coverPath = "https://images.unsplash.com/photo-1494521695290-e1b495b63894?w=800&h=600&fit=crop",
                    deviceModel = "OnePlus 13 Pro",
                    author = "纪实摄影师",
                    description = "黑白风格纪实摄影预设，强调对比度",
                    sceneType = "street",
                    tags = listOf("黑白", "纪实", "街拍"),
                    rating = 4.8f,
                    downloadCount = 2150,
                    source = "community",
                    cameraParams = CameraParams(
                        mode = "专业模式",
                        filter = "黑白",
                        iso = 400,
                        shutter = "1/250",
                        ev = "+0.0",
                        wb = "Auto",
                        focalLength = "35mm",
                        colorStyle = ColorStyle.Classic.name,
                        sharpness = 60,
                        contrast = 70,
                        saturation = 0
                    )
                ),
                authorId = "user_006",
                authorName = "纪实摄影师",
                authorAvatar = "https://api.dicebear.com/7.x/avataaars/svg?seed=user6",
                description = "黑白纪实风格预设，强调对比度和层次感，适合街头摄影和纪实题材。",
                sampleImages = listOf(
                    SubmissionImage(
                        id = "img_010",
                        imageUrl = "https://images.unsplash.com/photo-1494521695290-e1b495b63894?w=800&h=600&fit=crop",
                        title = "街拍",
                        description = "黑白对比"
                    )
                ),
                status = SubmissionStatus.APPROVED,
                submitTime = System.currentTimeMillis() - 10 * 24 * 60 * 60 * 1000,
                downloadCount = 2150,
                viewCount = 6780,
                likeCount = 489,
                rating = 4.8f,
                ratingCount = 89,
                tags = listOf("黑白", "纪实", "街拍", "对比")
            )
        )
    }
}
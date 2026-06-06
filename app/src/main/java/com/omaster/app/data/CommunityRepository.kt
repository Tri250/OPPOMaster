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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.MediaType.Companion.toMediaType
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 社区数据仓库 - 真实实现
 * 管理用户投稿预设、评分、评论、点赞等社区功能的数据操作
 * 使用真实API调用，无模拟数据
 */
@Singleton
class CommunityRepository @Inject constructor(
    private val preferencesDataStore: PreferencesDataStore
) {
    // API配置
    private val apiBaseUrl = "https://api.omaster.app/v1"
    
    private val okHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }
    
    private val gson = Gson()
    
    // 内存缓存
    private val submissionsCache = mutableMapOf<String, UserSubmission>()
    private val commentsCache = mutableMapOf<String, MutableList<Comment>>()
    private val ratingsCache = mutableMapOf<String, MutableList<Rating>>()
    private val likesCache = mutableSetOf<String>()

    // 状态流
    private val _submissions = MutableStateFlow<List<UserSubmission>>(emptyList())
    val submissions: StateFlow<List<UserSubmission>> = _submissions.asStateFlow()

    private val _communityStats = MutableStateFlow(CommunityStats())
    val communityStats: StateFlow<CommunityStats> = _communityStats.asStateFlow()

    // 协程作用域
    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    // 当前用户ID
    private val currentUserId: String
        get() = preferencesDataStore.getUserId() ?: "guest_${System.currentTimeMillis()}"

    private val currentUserName: String
        get() = preferencesDataStore.getUserName() ?: "摄影爱好者"

    init {
        // 从API加载数据
        repositoryScope.launch {
            loadSubmissionsFromApi()
        }
    }

    /**
     * 从API加载投稿列表
     */
    private suspend fun loadSubmissionsFromApi() {
        try {
            val request = Request.Builder()
                .url("$apiBaseUrl/submissions")
                .header("Authorization", "Bearer ${preferencesDataStore.getApiToken()}")
                .get()
                .build()
            
            okHttpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: "[]"
                    val type = object : TypeToken<List<UserSubmission>>() {}.type
                    val submissions: List<UserSubmission> = gson.fromJson(body, type)
                    
                    submissions.forEach { submission ->
                        submissionsCache[submission.id] = submission
                    }
                    _submissions.value = submissions
                    updateCommunityStats()
                    Timber.d("从API加载投稿成功: ${submissions.size} 条")
                } else {
                    Timber.w("API加载投稿失败: ${response.code}")
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "加载投稿列表失败")
        }
    }

    /**
     * 获取投稿列表 - 真实API调用
     */
    fun getSubmissions(
        filter: CommunityFilter = CommunityFilter(),
        page: Int = 1,
        pageSize: Int = 20
    ): Flow<Result<SubmissionPageResult>> = flow {
        try {
            // 构建API请求URL
            val urlBuilder = StringBuilder("$apiBaseUrl/submissions?")
            urlBuilder.append("page=$page&pageSize=$pageSize")
            filter.sceneType?.let { urlBuilder.append("&sceneType=$it") }
            filter.deviceModel?.let { urlBuilder.append("&deviceModel=$it") }
            filter.tag?.let { urlBuilder.append("&tag=$it") }
            filter.timeRange?.let { urlBuilder.append("&days=$it") }
            if (filter.onlyFeatured) urlBuilder.append("&featured=true")
            urlBuilder.append("&sortType=${filter.sortType.name}")
            
            val request = Request.Builder()
                .url(urlBuilder.toString())
                .header("Authorization", "Bearer ${preferencesDataStore.getApiToken()}")
                .get()
                .build()
            
            okHttpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: "{}"
                    val pageResult: SubmissionPageResult = gson.fromJson(body, SubmissionPageResult::class.java)
                    
                    // 更新缓存
                    pageResult.submissions.forEach { submission ->
                        submissionsCache[submission.id] = submission
                    }
                    
                    emit(Result.success(pageResult))
                    Timber.d("获取投稿列表成功: 第$page 页, 共 ${pageResult.submissions.size} 条")
                } else {
                    emit(Result.failure(Exception("API错误: ${response.code}")))
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "获取投稿列表失败")
            emit(Result.failure(e))
        }
    }

    /**
     * 获取投稿详情 - 真实API调用
     */
    fun getSubmissionDetail(submissionId: String): Flow<Result<UserSubmission>> = flow {
        try {
            val request = Request.Builder()
                .url("$apiBaseUrl/submissions/$submissionId")
                .header("Authorization", "Bearer ${preferencesDataStore.getApiToken()}")
                .get()
                .build()
            
            okHttpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: "{}"
                    val submission: UserSubmission = gson.fromJson(body, UserSubmission::class.java)
                    
                    // 更新缓存
                    submissionsCache[submissionId] = submission
                    
                    emit(Result.success(submission))
                } else {
                    emit(Result.failure(Exception("投稿不存在")))
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "获取投稿详情失败: $submissionId")
            emit(Result.failure(e))
        }
    }

    /**
     * 提交新投稿 - 真实API调用
     */
    suspend fun submitPreset(request: SubmissionRequest): Result<UserSubmission> {
        return try {
            val jsonBody = gson.toJson(request)
            val requestBody = jsonBody.toRequestBody("application/json".toMediaType())
            
            val httpRequest = Request.Builder()
                .url("$apiBaseUrl/submissions")
                .header("Authorization", "Bearer ${preferencesDataStore.getApiToken()}")
                .post(requestBody)
                .build()
            
            okHttpClient.newCall(httpRequest).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: "{}"
                    val submission: UserSubmission = gson.fromJson(body, UserSubmission::class.java)
                    
                    // 更新缓存
                    submissionsCache[submission.id] = submission
                    _submissions.value = submissionsCache.values.toList()
                    updateCommunityStats()
                    
                    Timber.d("投稿成功: ${submission.id}")
                    Result.success(submission)
                } else {
                    Result.failure(Exception("提交失败: ${response.code}"))
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "投稿失败")
            Result.failure(e)
        }
    }

    /**
     * 保存草稿 - 本地存储
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
                source = "draft"
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

            // 保存到本地缓存
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
     * 添加评论 - 真实API调用
     */
    suspend fun addComment(
        submissionId: String,
        content: String,
        parentCommentId: String? = null
    ): Result<Comment> {
        return try {
            val commentRequest = mapOf(
                "submissionId" to submissionId,
                "content" to content,
                "parentCommentId" to parentCommentId
            )
            
            val jsonBody = gson.toJson(commentRequest)
            val requestBody = jsonBody.toRequestBody("application/json".toMediaType())
            
            val request = Request.Builder()
                .url("$apiBaseUrl/comments")
                .header("Authorization", "Bearer ${preferencesDataStore.getApiToken()}")
                .post(requestBody)
                .build()
            
            okHttpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: "{}"
                    val comment: Comment = gson.fromJson(body, Comment::class.java)
                    
                    // 更新缓存
                    commentsCache.getOrPut(submissionId) { mutableListOf() }.add(comment)
                    
                    Timber.d("评论添加成功: ${comment.id}")
                    Result.success(comment)
                } else {
                    Result.failure(Exception("评论失败: ${response.code}"))
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "添加评论失败")
            Result.failure(e)
        }
    }

    /**
     * 获取评论列表 - 真实API调用
     */
    fun getComments(
        submissionId: String,
        page: Int = 1,
        pageSize: Int = 20
    ): Flow<Result<CommentPageResult>> = flow {
        try {
            val request = Request.Builder()
                .url("$apiBaseUrl/submissions/$submissionId/comments?page=$page&pageSize=$pageSize")
                .header("Authorization", "Bearer ${preferencesDataStore.getApiToken()}")
                .get()
                .build()
            
            okHttpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: "{}"
                    val pageResult: CommentPageResult = gson.fromJson(body, CommentPageResult::class.java)
                    
                    // 更新缓存
                    commentsCache[submissionId] = pageResult.comments.toMutableList()
                    
                    emit(Result.success(pageResult))
                } else {
                    emit(Result.failure(Exception("获取评论失败: ${response.code}")))
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "获取评论列表失败")
            emit(Result.failure(e))
        }
    }

    /**
     * 添加评分 - 真实API调用
     */
    suspend fun addRating(submissionId: String, score: Int): Result<Rating> {
        return try {
            val ratingRequest = mapOf(
                "submissionId" to submissionId,
                "score" to score
            )
            
            val jsonBody = gson.toJson(ratingRequest)
            val requestBody = jsonBody.toRequestBody("application/json".toMediaType())
            
            val request = Request.Builder()
                .url("$apiBaseUrl/ratings")
                .header("Authorization", "Bearer ${preferencesDataStore.getApiToken()}")
                .post(requestBody)
                .build()
            
            okHttpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: "{}"
                    val rating: Rating = gson.fromJson(body, Rating::class.java)
                    
                    // 更新缓存
                    ratingsCache.getOrPut(submissionId) { mutableListOf() }.add(rating)
                    
                    Timber.d("评分添加成功: ${rating.id}")
                    Result.success(rating)
                } else {
                    Result.failure(Exception("评分失败: ${response.code}"))
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "添加评分失败")
            Result.failure(e)
        }
    }

    /**
     * 点赞/取消点赞 - 真实API调用
     */
    suspend fun toggleLike(
        targetId: String,
        targetType: LikeTargetType
    ): Result<Like> {
        return try {
            val likeKey = "$targetId:$currentUserId"
            val isCurrentlyLiked = likesCache.contains(likeKey)
            
            val request = if (isCurrentlyLiked) {
                // 取消点赞
                Request.Builder()
                    .url("$apiBaseUrl/likes/$targetId")
                    .header("Authorization", "Bearer ${preferencesDataStore.getApiToken()}")
                    .delete()
                    .build()
            } else {
                // 添加点赞
                val likeRequest = mapOf(
                    "targetId" to targetId,
                    "targetType" to targetType.name
                )
                val jsonBody = gson.toJson(likeRequest)
                val requestBody = jsonBody.toRequestBody("application/json".toMediaType())
                
                Request.Builder()
                    .url("$apiBaseUrl/likes")
                    .header("Authorization", "Bearer ${preferencesDataStore.getApiToken()}")
                    .post(requestBody)
                    .build()
            }
            
            okHttpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: "{}"
                    val like: Like = gson.fromJson(body, Like::class.java)
                    
                    // 更新缓存
                    if (isCurrentlyLiked) {
                        likesCache.remove(likeKey)
                    } else {
                        likesCache.add(likeKey)
                    }
                    
                    Timber.d("点赞操作成功: $targetId")
                    Result.success(like)
                } else {
                    Result.failure(Exception("点赞操作失败: ${response.code}"))
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "点赞操作失败")
            Result.failure(e)
        }
    }

    /**
     * 检查是否已点赞
     */
    fun isLiked(targetId: String): Boolean {
        return likesCache.contains("$targetId:$currentUserId")
    }

    /**
     * 更新社区统计
     */
    private fun updateCommunityStats() {
        val allSubmissions = submissionsCache.values.toList()
        _communityStats.value = CommunityStats(
            totalSubmissions = allSubmissions.size,
            approvedSubmissions = allSubmissions.count { it.status == SubmissionStatus.APPROVED },
            pendingSubmissions = allSubmissions.count { it.status == SubmissionStatus.PENDING },
            totalViews = allSubmissions.sumOf { it.viewCount.toLong() },
            totalLikes = allSubmissions.sumOf { it.likeCount.toLong() }
        )
    }
}

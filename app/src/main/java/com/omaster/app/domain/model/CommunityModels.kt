package com.omaster.app.domain.model

import java.util.UUID

/**
 * 社区数据模型定义
 * 包含用户投稿、评分、评论、点赞等社区功能相关数据模型
 */

/**
 * 用户投稿状态枚举
 */
enum class SubmissionStatus {
    PENDING,    // 待审核
    APPROVED,   // 已通过
    REJECTED,   // 已拒绝
    DRAFT       // 草稿
}

/**
 * 社区排序方式枚举
 */
enum class CommunitySortType {
    LATEST,     // 最新发布
    HOTTEST,    // 最热（下载量+点赞数综合）
    HIGHEST_RATED  // 评分最高
}

/**
 * 用户投稿数据模型
 * 表示用户投稿的预设信息
 *
 * @property id 投稿唯一标识
 * @property preset 关联的预设信息
 * @property authorId 作者用户ID
 * @property authorName 作者昵称
 * @property authorAvatar 作者头像URL
 * @property description 投稿描述/说明
 * @property sampleImages 样张图片列表
 * @property status 审核状态
 * @property submitTime 提交时间戳
 * @property reviewTime 审核时间戳
 * @property reviewComment 审核备注
 * @property downloadCount 下载次数
 * @property viewCount 浏览次数
 * @property likeCount 点赞数
 * @property rating 平均评分（1-5星）
 * @property ratingCount 评分人数
 * @property tags 投稿标签
 * @property isFeatured 是否精选
 * @property isLiked 当前用户是否已点赞
 */
data class UserSubmission(
    val id: String = UUID.randomUUID().toString(),
    val preset: Preset,
    val authorId: String,
    val authorName: String,
    val authorAvatar: String = "",
    val description: String = "",
    val sampleImages: List<SubmissionImage> = emptyList(),
    val status: SubmissionStatus = SubmissionStatus.PENDING,
    val submitTime: Long = System.currentTimeMillis(),
    val reviewTime: Long? = null,
    val reviewComment: String? = null,
    val downloadCount: Int = 0,
    val viewCount: Int = 0,
    val likeCount: Int = 0,
    val rating: Float = 0f,
    val ratingCount: Int = 0,
    val tags: List<String> = emptyList(),
    val isFeatured: Boolean = false,
    val isLiked: Boolean = false
) {
    /**
     * 获取格式化提交时间
     */
    fun getFormattedSubmitTime(): String {
        val date = java.util.Date(submitTime)
        val format = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault())
        return format.format(date)
    }

    /**
     * 获取格式化下载量
     */
    fun getFormattedDownloadCount(): String {
        return when {
            downloadCount >= 1000000 -> String.format("%.1fM", downloadCount / 1000000.0)
            downloadCount >= 1000 -> String.format("%.1fK", downloadCount / 1000.0)
            else -> downloadCount.toString()
        }
    }

    /**
     * 获取热度值（用于排序）
     */
    fun getHeatScore(): Int {
        return downloadCount * 2 + likeCount * 3 + viewCount / 10
    }
}

/**
 * 投稿样张图片数据模型
 *
 * @property id 图片唯一标识
 * @property imageUrl 图片URL
 * @property thumbnailUrl 缩略图URL
 * @property title 图片标题
 * @property description 图片描述
 * @property uploadTime 上传时间戳
 * @property width 图片宽度
 * @property height 图片高度
 */
data class SubmissionImage(
    val id: String = UUID.randomUUID().toString(),
    val imageUrl: String,
    val thumbnailUrl: String = imageUrl,
    val title: String = "",
    val description: String = "",
    val uploadTime: Long = System.currentTimeMillis(),
    val width: Int = 0,
    val height: Int = 0
)

/**
 * 评分数据模型
 * 用户对投稿的评分记录
 *
 * @property id 评分唯一标识
 * @property submissionId 关联投稿ID
 * @property userId 评分用户ID
 * @property userName 评分用户昵称
 * @property rating 评分值（1-5星）
 * @property comment 评分评论
 * @property createTime 创建时间戳
 * @property updateTime 更新时间戳
 */
data class Rating(
    val id: String = UUID.randomUUID().toString(),
    val submissionId: String,
    val userId: String,
    val userName: String,
    val rating: Float,  // 1.0 - 5.0
    val comment: String = "",
    val createTime: Long = System.currentTimeMillis(),
    val updateTime: Long = System.currentTimeMillis()
) {
    init {
        require(rating in 1.0f..5.0f) { "评分必须在1-5之间" }
    }

    /**
     * 获取星级显示文本
     */
    fun getStarDisplay(): String {
        val fullStars = rating.toInt()
        val hasHalfStar = rating - fullStars >= 0.5f
        val emptyStars = 5 - fullStars - if (hasHalfStar) 1 else 0

        return "★".repeat(fullStars) +
               (if (hasHalfStar) "☆" else "") +
               "☆".repeat(emptyStars)
    }
}

/**
 * 评论数据模型
 * 用户对投稿的评论
 *
 * @property id 评论唯一标识
 * @property submissionId 关联投稿ID
 * @property userId 评论用户ID
 * @property userName 评论用户昵称
 * @property userAvatar 评论用户头像
 * @property content 评论内容
 * @property parentId 父评论ID（用于回复）
 * @property replyToUserName 回复对象用户名
 * @property likeCount 点赞数
 * @property isLiked 当前用户是否已点赞
 * @property createTime 创建时间戳
 * @property isDeleted 是否已删除
 * @property replies 子评论列表（仅用于展示）
 */
data class Comment(
    val id: String = UUID.randomUUID().toString(),
    val submissionId: String,
    val userId: String,
    val userName: String,
    val userAvatar: String = "",
    val content: String,
    val parentId: String? = null,
    val replyToUserName: String? = null,
    val likeCount: Int = 0,
    val isLiked: Boolean = false,
    val createTime: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false,
    val replies: List<Comment> = emptyList()
) {
    /**
     * 获取格式化时间（相对时间）
     */
    fun getRelativeTime(): String {
        val now = System.currentTimeMillis()
        val diff = now - createTime

        return when {
            diff < 60000 -> "刚刚"
            diff < 3600000 -> "${diff / 60000}分钟前"
            diff < 86400000 -> "${diff / 3600000}小时前"
            diff < 604800000 -> "${diff / 86400000}天前"
            else -> {
                val date = java.util.Date(createTime)
                val format = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                format.format(date)
            }
        }
    }

    /**
     * 判断是否为回复评论
     */
    fun isReply(): Boolean = parentId != null

    /**
     * 获取显示内容（已删除评论显示提示）
     */
    fun getDisplayContent(): String {
        return if (isDeleted) "该评论已删除" else content
    }
}

/**
 * 点赞数据模型
 * 用户对投稿或评论的点赞记录
 *
 * @property id 点赞唯一标识
 * @property targetId 目标ID（投稿ID或评论ID）
 * @property targetType 目标类型（submission/comment）
 * @property userId 点赞用户ID
 * @property createTime 创建时间戳
 */
data class Like(
    val id: String = UUID.randomUUID().toString(),
    val targetId: String,
    val targetType: LikeTargetType,
    val userId: String,
    val createTime: Long = System.currentTimeMillis()
)

/**
 * 点赞目标类型枚举
 */
enum class LikeTargetType {
    SUBMISSION,  // 投稿
    COMMENT      // 评论
}

/**
 * 投稿请求数据模型
 * 用于提交新投稿
 *
 * @property presetName 预设名称
 * @property description 投稿描述
 * @property cameraParams 相机参数
 * @property sampleImages 样张图片列表（本地路径）
 * @property tags 标签列表
 * @property deviceModel 设备型号
 */
data class SubmissionRequest(
    val presetName: String,
    val description: String,
    val cameraParams: CameraParams? = null,
    val sampleImages: List<String> = emptyList(),
    val tags: List<String> = emptyList(),
    val deviceModel: String = ""
)

/**
 * 社区筛选条件数据模型
 *
 * @property sortType 排序方式
 * @property sceneType 场景类型筛选
 * @property deviceModel 设备型号筛选
 * @property tag 标签筛选
 * @property timeRange 时间范围（天）
 * @property onlyFeatured 仅显示精选
 */
data class CommunityFilter(
    val sortType: CommunitySortType = CommunitySortType.LATEST,
    val sceneType: String? = null,
    val deviceModel: String? = null,
    val tag: String? = null,
    val timeRange: Int? = null,
    val onlyFeatured: Boolean = false
)

/**
 * 社区统计数据模型
 *
 * @property totalSubmissions 总投稿数
 * @property totalDownloads 总下载量
 * @property totalUsers 总用户数
 * @property todaySubmissions 今日投稿数
 * @property featuredCount 精选投稿数
 */
data class CommunityStats(
    val totalSubmissions: Int = 0,
    val totalDownloads: Int = 0,
    val totalUsers: Int = 0,
    val todaySubmissions: Int = 0,
    val featuredCount: Int = 0
)

/**
 * 社区用户数据模型
 *
 * @property id 用户ID
 * @property nickname 昵称
 * @property avatar 头像URL
 * @property bio 个人简介
 * @property submissionCount 投稿数量
 * @property followerCount 粉丝数量
 * @property followingCount 关注数量
 * @property totalDownloads 总下载量
 * @property isFollowing 当前用户是否已关注
 */
data class CommunityUser(
    val id: String,
    val nickname: String,
    val avatar: String = "",
    val bio: String = "",
    val submissionCount: Int = 0,
    val followerCount: Int = 0,
    val followingCount: Int = 0,
    val totalDownloads: Int = 0,
    val isFollowing: Boolean = false
)

/**
 * 评分分布数据模型
 * 用于展示评分的分布情况
 *
 * @property fiveStar 五星数量
 * @property fourStar 四星数量
 * @property threeStar 三星数量
 * @property twoStar 二星数量
 * @property oneStar 一星数量
 */
data class RatingDistribution(
    val fiveStar: Int = 0,
    val fourStar: Int = 0,
    val threeStar: Int = 0,
    val twoStar: Int = 0,
    val oneStar: Int = 0
) {
    /**
     * 获取总评分数量
     */
    fun getTotalCount(): Int = fiveStar + fourStar + threeStar + twoStar + oneStar

    /**
     * 获取各星级百分比
     */
    fun getPercentage(star: Int): Float {
        val total = getTotalCount()
        if (total == 0) return 0f

        val count = when (star) {
            5 -> fiveStar
            4 -> fourStar
            3 -> threeStar
            2 -> twoStar
            1 -> oneStar
            else -> 0
        }
        return (count * 100f) / total
    }

    /**
     * 获取平均分
     */
    fun getAverageRating(): Float {
        val total = getTotalCount()
        if (total == 0) return 0f

        val sum = fiveStar * 5 + fourStar * 4 + threeStar * 3 + twoStar * 2 + oneStar
        return sum.toFloat() / total
    }
}

/**
 * 评论分页结果数据模型
 *
 * @property comments 评论列表
 * @property hasMore 是否还有更多
 * @property totalCount 总评论数
 */
data class CommentPageResult(
    val comments: List<Comment>,
    val hasMore: Boolean,
    val totalCount: Int
)

/**
 * 投稿分页结果数据模型
 *
 * @property submissions 投稿列表
 * @property hasMore 是否还有更多
 * @property totalCount 总投稿数
 */
data class SubmissionPageResult(
    val submissions: List<UserSubmission>,
    val hasMore: Boolean,
    val totalCount: Int
)

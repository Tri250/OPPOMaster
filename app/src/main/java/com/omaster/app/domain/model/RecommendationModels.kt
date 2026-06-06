package com.omaster.app.domain.model

/**
 * 推荐数据模型
 * 智能推荐系统的核心数据类
 */

/**
 * 推荐项
 * 包含推荐预设、推荐理由、相似度分数等信息
 *
 * @property preset 推荐预设
 * @property reason 推荐理由
 * @property similarityScore 相似度分数 (0.0 - 1.0)
 * @property recommendationType 推荐类型
 * @property confidence 推荐置信度 (0.0 - 1.0)
 * @property timestamp 推荐生成时间戳
 * @property isNew 是否为新增推荐
 * @property trendingRank 趋势排名 (仅用于热门推荐)
 */
data class Recommendation(
    val preset: Preset,
    val reason: String,
    val similarityScore: Float,
    val recommendationType: RecommendationType,
    val confidence: Float = 0.8f,
    val timestamp: Long = System.currentTimeMillis(),
    val isNew: Boolean = false,
    val trendingRank: Int = 0
) {
    /**
     * 获取相似度百分比显示
     */
    fun getSimilarityPercentage(): String {
        return "${(similarityScore * 100).toInt()}%"
    }

    /**
     * 获取格式化的推荐理由
     */
    fun getFormattedReason(): String {
        return when (recommendationType) {
            RecommendationType.COLLABORATIVE_FILTERING -> "相似用户也喜欢 · $reason"
            RecommendationType.CONTENT_BASED -> "基于你的喜好 · $reason"
            RecommendationType.TRENDING -> "热门趋势 · $reason"
            RecommendationType.NEW_USER -> "新用户推荐 · $reason"
            RecommendationType.SEASONAL -> "时令推荐 · $reason"
            RecommendationType.POPULAR -> "人气精选 · $reason"
        }
    }

    /**
     * 获取推荐类型图标
     */
    fun getTypeIcon(): String {
        return when (recommendationType) {
            RecommendationType.COLLABORATIVE_FILTERING -> "👥"
            RecommendationType.CONTENT_BASED -> "🎯"
            RecommendationType.TRENDING -> "🔥"
            RecommendationType.NEW_USER -> "✨"
            RecommendationType.SEASONAL -> "🌸"
            RecommendationType.POPULAR -> "⭐"
        }
    }
}

/**
 * 推荐类型枚举
 */
enum class RecommendationType {
    COLLABORATIVE_FILTERING,  // 协同过滤推荐
    CONTENT_BASED,            // 基于内容的推荐
    TRENDING,                 // 热门趋势推荐
    NEW_USER,                 // 新用户冷启动推荐
    SEASONAL,                 // 时令/节日推荐
    POPULAR                   // 人气推荐
}

/**
 * 用户偏好
 * 记录用户的偏好设置和行为权重
 *
 * @property userId 用户ID
 * @property favoriteTags 用户偏好的标签列表
 * @property preferredSceneTypes 偏好的场景类型
 * @property preferredAuthors 偏好的作者
 * @property preferredDeviceModels 偏好的设备型号
 * @property behaviorWeights 行为权重配置
 * @property lastUpdated 最后更新时间
 */
data class UserPreference(
    val userId: String = "default_user",
    val favoriteTags: List<String> = emptyList(),
    val preferredSceneTypes: List<String> = emptyList(),
    val preferredAuthors: List<String> = emptyList(),
    val preferredDeviceModels: List<String> = emptyList(),
    val behaviorWeights: BehaviorWeights = BehaviorWeights(),
    val lastUpdated: Long = System.currentTimeMillis()
) {
    /**
     * 计算标签匹配分数
     */
    fun calculateTagMatchScore(tags: List<String>): Float {
        if (favoriteTags.isEmpty() || tags.isEmpty()) return 0f
        val matchingTags = tags.intersect(favoriteTags.toSet())
        return matchingTags.size.toFloat() / tags.size.coerceAtLeast(1)
    }

    /**
     * 检查场景类型偏好
     */
    fun isPreferredSceneType(sceneType: String): Boolean {
        return preferredSceneTypes.contains(sceneType)
    }

    /**
     * 更新偏好标签
     */
    fun updateFavoriteTags(newTags: List<String>): UserPreference {
        return copy(
            favoriteTags = (favoriteTags + newTags).distinct().take(20),
            lastUpdated = System.currentTimeMillis()
        )
    }
}

/**
 * 行为权重配置
 * 用于推荐算法中的权重计算
 *
 * @property usageWeight 使用频率权重 (默认40%)
 * @property favoriteWeight 收藏权重 (默认30%)
 * @property browseWeight 浏览权重 (默认20%)
 * @property searchWeight 搜索权重 (默认10%)
 */
data class BehaviorWeights(
    val usageWeight: Float = 0.40f,
    val favoriteWeight: Float = 0.30f,
    val browseWeight: Float = 0.20f,
    val searchWeight: Float = 0.10f
) {
    init {
        // 确保权重总和为1.0
        val total = usageWeight + favoriteWeight + browseWeight + searchWeight
        require(total in 0.99f..1.01f) { "行为权重总和必须等于1.0，当前为$total" }
    }

    /**
     * 获取权重列表（用于计算）
     */
    fun toWeightList(): List<Float> = listOf(
        usageWeight,
        favoriteWeight,
        browseWeight,
        searchWeight
    )
}

/**
 * 趋势项
 * 记录热门趋势信息
 *
 * @property presetId 预设ID
 * @property preset 预设对象（可选）
 * @property trendScore 趋势分数
 * @property rank 排名
 * @property growthRate 增长率
 * @property category 趋势分类
 * @property timestamp 数据时间戳
 * @property viewCount 浏览次数
 * @property favoriteCount 收藏次数
 * @property downloadCount 下载次数
 */
data class TrendingItem(
    val presetId: String,
    val preset: Preset? = null,
    val trendScore: Float,
    val rank: Int,
    val growthRate: Float = 0f,
    val category: TrendingCategory,
    val timestamp: Long = System.currentTimeMillis(),
    val viewCount: Int = 0,
    val favoriteCount: Int = 0,
    val downloadCount: Int = 0
) {
    /**
     * 获取增长率显示
     */
    fun getGrowthRateDisplay(): String {
        val sign = if (growthRate >= 0) "+" else ""
        return "$sign${String.format("%.1f", growthRate * 100)}%"
    }

    /**
     * 是否为上升趋势
     */
    fun isRising(): Boolean = growthRate > 0

    /**
     * 获取趋势图标
     */
    fun getTrendIcon(): String {
        return when {
            growthRate > 0.3f -> "🔥🔥🔥"
            growthRate > 0.1f -> "🔥🔥"
            growthRate > 0f -> "🔥"
            growthRate == 0f -> "➡️"
            else -> "📉"
        }
    }
}

/**
 * 趋势分类
 */
enum class TrendingCategory {
    DAILY,      // 日榜
    WEEKLY,     // 周榜
    MONTHLY,    // 月榜
    EMERGING,   // 新锐榜
    ALL_TIME    // 总榜
}

/**
 * 用户行为记录
 * 用于追踪用户与预设的交互行为
 *
 * @property presetId 预设ID
 * @property behaviorType 行为类型
 * @property timestamp 行为发生时间
 * @property duration 行为持续时间（毫秒，用于浏览）
 * @property metadata 额外元数据
 */
data class UserBehavior(
    val presetId: String,
    val behaviorType: BehaviorType,
    val timestamp: Long = System.currentTimeMillis(),
    val duration: Long = 0,
    val metadata: Map<String, String> = emptyMap()
) {
    /**
     * 计算时间衰减因子
     * 近期行为权重更高
     */
    fun calculateTimeDecayFactor(halfLifeDays: Int = 7): Float {
        val daysSinceEvent = (System.currentTimeMillis() - timestamp) / (1000 * 60 * 60 * 24f)
        val halfLife = halfLifeDays.toFloat()
        return kotlin.math.exp(-0.693f * daysSinceEvent / halfLife)
    }
}

/**
 * 行为类型
 */
enum class BehaviorType {
    VIEW,       // 浏览
    FAVORITE,   // 收藏
    UNFAVORITE, // 取消收藏
    APPLY,      // 应用预设
    DOWNLOAD,   // 下载
    SEARCH,     // 搜索
    SHARE       // 分享
}

/**
 * 搜索关键词记录
 *
 * @property keyword 搜索关键词
 * @property timestamp 搜索时间
 * @property resultCount 返回结果数
 * @property clickedPresetId 用户点击的预设ID
 */
data class SearchKeyword(
    val keyword: String,
    val timestamp: Long = System.currentTimeMillis(),
    val resultCount: Int = 0,
    val clickedPresetId: String? = null
)

/**
 * 相似用户
 * 用于协同过滤推荐
 *
 * @property userId 用户ID
 * @property similarityScore 相似度分数
 * @property commonPresets 共同喜欢的预设
 */
data class SimilarUser(
    val userId: String,
    val similarityScore: Float,
    val commonPresets: List<String> = emptyList()
)

/**
 * 推荐结果包装类
 *
 * @property recommendations 推荐列表
 * @property hasMore 是否有更多推荐
 * @property nextPageToken 下一页令牌
 * @property generatedAt 生成时间
 */
data class RecommendationResult(
    val recommendations: List<Recommendation>,
    val hasMore: Boolean = false,
    val nextPageToken: String? = null,
    val generatedAt: Long = System.currentTimeMillis()
)

/**
 * 推荐板块
 * 用于UI展示的分组推荐
 *
 * @property title 板块标题
 * @property subtitle 板块副标题
 * @property recommendations 推荐列表
 * @property sectionType 板块类型
 */
data class RecommendationSection(
    val title: String,
    val subtitle: String,
    val recommendations: List<Recommendation>,
    val sectionType: SectionType
) {
    /**
     * 是否为空板块
     */
    fun isEmpty(): Boolean = recommendations.isEmpty()

    /**
     * 获取板块图标
     */
    fun getSectionIcon(): String {
        return when (sectionType) {
            SectionType.FOR_YOU -> "💝"
            SectionType.GUESS_YOU_LIKE -> "🤔"
            SectionType.SIMILAR_USERS -> "👥"
            SectionType.TRENDING -> "🔥"
            SectionType.NEW_ARRIVALS -> "✨"
            SectionType.SEASONAL -> "🌸"
            SectionType.BECAUSE_YOU_LIKED -> "🔗"
        }
    }
}

/**
 * 板块类型
 */
enum class SectionType {
    FOR_YOU,           // 为你推荐
    GUESS_YOU_LIKE,    // 猜你喜欢
    SIMILAR_USERS,     // 相似用户喜欢
    TRENDING,          // 热门趋势
    NEW_ARRIVALS,      // 新品推荐
    SEASONAL,          // 时令推荐
    BECAUSE_YOU_LIKED  // 因为你喜欢
}

/**
 * 冷启动推荐配置
 * 用于新用户的初始推荐策略
 */
object ColdStartConfig {
    // 默认热门预设ID列表
    val DEFAULT_TRENDING_PRESETS = listOf(
        "hncs_portrait_master",
        "hncs_landscape_master",
        "film_portrait_01",
        "night_urban_01"
    )

    // 默认场景类型推荐
    val DEFAULT_SCENE_RECOMMENDATIONS = mapOf(
        "portrait" to listOf("hncs_portrait_master", "film_portrait_01"),
        "landscape" to listOf("hncs_landscape_master"),
        "night" to listOf("night_urban_01"),
        "food" to listOf("food_style_01"),
        "street" to listOf("street_documentary")
    )

    // 默认标签权重
    val DEFAULT_TAG_WEIGHTS = mapOf(
        "人像" to 1.0f,
        "风景" to 0.9f,
        "夜景" to 0.85f,
        "胶片" to 0.8f,
        "HNCS" to 1.0f,
        "美食" to 0.75f,
        "街拍" to 0.7f
    )
}

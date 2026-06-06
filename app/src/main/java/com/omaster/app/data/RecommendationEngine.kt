package com.omaster.app.data

import com.omaster.app.domain.model.BehaviorType
import com.omaster.app.domain.model.BehaviorWeights
import com.omaster.app.domain.model.ColdStartConfig
import com.omaster.app.domain.model.Recommendation
import com.omaster.app.domain.model.RecommendationResult
import com.omaster.app.domain.model.RecommendationSection
import com.omaster.app.domain.model.RecommendationType
import com.omaster.app.domain.model.SectionType
import com.omaster.app.domain.model.SimilarUser
import com.omaster.app.domain.model.TrendingCategory
import com.omaster.app.domain.model.TrendingItem
import com.omaster.app.domain.model.UserBehavior
import com.omaster.app.domain.model.UserPreference
import com.omaster.app.domain.model.Preset
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.exp
import kotlin.math.sqrt

/**
 * 推荐引擎
 * 核心推荐算法实现，支持协同过滤、内容推荐、热门趋势等多种推荐策略
 *
 * 算法要点：
 * - 权重计算：使用频率40%、收藏30%、浏览20%、搜索10%
 * - 相似度计算：标签匹配度 + 使用模式相似度
 * - 时间衰减：近期行为权重更高
 */
@Singleton
class RecommendationEngine @Inject constructor(
    private val behaviorTracker: UserBehaviorTracker,
    private val presetRepository: PresetRepository
) {
    companion object {
        // 推荐数量配置
        private const val DEFAULT_RECOMMENDATION_LIMIT = 20
        private const val SECTION_RECOMMENDATION_LIMIT = 10

        // 时间衰减半衰期（天）
        private const val TIME_DECAY_HALF_LIFE = 7

        // 相似度阈值
        private const val SIMILARITY_THRESHOLD = 0.3f

        // 最小行为数据量（用于判断是否为冷启动）
        private const val MIN_BEHAVIOR_FOR_PERSONALIZATION = 5
    }

    // ==================== 核心推荐方法 ====================

    /**
     * 获取个性化推荐
     * 根据用户行为历史生成综合推荐
     *
     * @param limit 推荐数量限制
     * @return 推荐结果
     */
    suspend fun getPersonalizedRecommendations(
        limit: Int = DEFAULT_RECOMMENDATION_LIMIT
    ): RecommendationResult {
        val allPresets = presetRepository.getAllPresets()
        val userBehaviors = behaviorTracker.getAllBehaviors()

        // 判断是否为冷启动用户
        if (userBehaviors.size < MIN_BEHAVIOR_FOR_PERSONALIZATION) {
            return getColdStartRecommendations(limit)
        }

        // 计算每个预设的推荐分数
        val scoredPresets = allPresets.map { preset ->
            val score = calculateRecommendationScore(preset, userBehaviors)
            val recommendation = createRecommendation(preset, score, userBehaviors)
            recommendation
        }

        // 按分数排序并去重
        val sortedRecommendations = scoredPresets
            .filter { it.similarityScore > 0.1f }
            .sortedByDescending { it.similarityScore * it.confidence }
            .distinctBy { it.preset.id }
            .take(limit)

        return RecommendationResult(
            recommendations = sortedRecommendations,
            hasMore = sortedRecommendations.size >= limit
        )
    }

    /**
     * 获取冷启动推荐
     * 针对新用户的推荐策略
     *
     * @param limit 推荐数量限制
     * @return 推荐结果
     */
    suspend fun getColdStartRecommendations(
        limit: Int = DEFAULT_RECOMMENDATION_LIMIT
    ): RecommendationResult {
        val allPresets = presetRepository.getAllPresets()

        // 获取热门预设
        val trendingPresets = getTrendingPresets(TrendingCategory.WEEKLY, limit)
            .mapNotNull { item ->
                allPresets.find { it.id == item.presetId }?.let { preset ->
                    Recommendation(
                        preset = preset,
                        reason = "本周热门，${item.viewCount}人正在使用",
                        similarityScore = item.trendScore,
                        recommendationType = RecommendationType.NEW_USER,
                        confidence = 0.7f,
                        trendingRank = item.rank
                    )
                }
            }

        // 补充默认推荐
        val defaultPresetIds = ColdStartConfig.DEFAULT_TRENDING_PRESETS
        val defaultRecommendations = defaultPresetIds.mapNotNull { presetId ->
            allPresets.find { it.id == presetId }?.let { preset ->
                Recommendation(
                    preset = preset,
                    reason = "新用户精选推荐",
                    similarityScore = 0.8f,
                    recommendationType = RecommendationType.NEW_USER,
                    confidence = 0.75f
                )
            }
        }

        // 合并推荐列表
        val combinedRecommendations = (trendingPresets + defaultRecommendations)
            .distinctBy { it.preset.id }
            .take(limit)

        return RecommendationResult(
            recommendations = combinedRecommendations,
            hasMore = false
        )
    }

    /**
     * 获取协同过滤推荐
     * 基于相似用户的偏好进行推荐
     *
     * @param limit 推荐数量限制
     * @return 推荐列表
     */
    suspend fun getCollaborativeFilteringRecommendations(
        limit: Int = SECTION_RECOMMENDATION_LIMIT
    ): List<Recommendation> {
        val allPresets = presetRepository.getAllPresets()
        val userBehaviors = behaviorTracker.getAllBehaviors()

        // 获取当前用户的偏好预设
        val userFavoritePresets = userBehaviors
            .filter { it.behaviorType == BehaviorType.FAVORITE }
            .map { it.presetId }
            .distinct()

        if (userFavoritePresets.isEmpty()) {
            return emptyList()
        }

        // 模拟相似用户（实际应用中应从服务器获取）
        val similarUsers = findSimilarUsers(userFavoritePresets)

        // 获取相似用户喜欢但当前用户未收藏的预设
        val recommendations = mutableListOf<Recommendation>()

        similarUsers.forEach { similarUser ->
            // 模拟相似用户喜欢的预设
            val similarUserPresets = simulateSimilarUserPreferences(similarUser.userId)

            similarUserPresets.forEach { presetId ->
                if (presetId !in userFavoritePresets) {
                    allPresets.find { it.id == presetId }?.let { preset ->
                        val commonTags = calculateCommonTags(
                            userFavoritePresets.mapNotNull { id ->
                                allPresets.find { it.id == id }
                            },
                            preset
                        )

                        recommendations.add(
                            Recommendation(
                                preset = preset,
                                reason = "与你兴趣相似的用户也喜欢${if (commonTags.isNotEmpty()) "，共同关注：$commonTags" else ""}",
                                similarityScore = similarUser.similarityScore,
                                recommendationType = RecommendationType.COLLABORATIVE_FILTERING,
                                confidence = similarUser.similarityScore
                            )
                        )
                    }
                }
            }
        }

        return recommendations
            .distinctBy { it.preset.id }
            .sortedByDescending { it.similarityScore }
            .take(limit)
    }

    /**
     * 获取基于内容的推荐
     * 根据用户偏好标签和场景类型推荐相似预设
     *
     * @param limit 推荐数量限制
     * @return 推荐列表
     */
    suspend fun getContentBasedRecommendations(
        limit: Int = SECTION_RECOMMENDATION_LIMIT
    ): List<Recommendation> {
        val allPresets = presetRepository.getAllPresets()
        val userBehaviors = behaviorTracker.getAllBehaviors()

        // 提取用户偏好标签（带时间衰减）
        val userTagPreferences = extractUserTagPreferences(userBehaviors)

        // 提取用户偏好的场景类型
        val userScenePreferences = extractUserScenePreferences(userBehaviors)

        if (userTagPreferences.isEmpty() && userScenePreferences.isEmpty()) {
            return emptyList()
        }

        // 计算每个预设的内容相似度
        val recommendations = allPresets.mapNotNull { preset ->
            val tagScore = calculateTagSimilarity(preset.tags, userTagPreferences)
            val sceneScore = if (preset.sceneType in userScenePreferences) 1.0f else 0f

            // 综合内容相似度
            val contentScore = (tagScore * 0.7f) + (sceneScore * 0.3f)

            if (contentScore > SIMILARITY_THRESHOLD) {
                val matchedTags = preset.tags.filter { it in userTagPreferences.keys }
                val reason = when {
                    matchedTags.isNotEmpty() -> "因为你喜欢${matchedTags.take(2).joinToString("、")}风格",
                    sceneScore > 0 -> "因为你喜欢${preset.getSceneTypeDisplay()}",
                    else -> "根据你的浏览历史推荐"
                }

                Recommendation(
                    preset = preset,
                    reason = reason,
                    similarityScore = contentScore,
                    recommendationType = RecommendationType.CONTENT_BASED,
                    confidence = contentScore
                )
            } else null
        }

        return recommendations
            .sortedByDescending { it.similarityScore }
            .take(limit)
    }

    /**
     * 获取热门趋势推荐
     *
     * @param category 趋势分类
     * @param limit 推荐数量限制
     * @return 推荐列表
     */
    suspend fun getTrendingRecommendations(
        category: TrendingCategory = TrendingCategory.WEEKLY,
        limit: Int = SECTION_RECOMMENDATION_LIMIT
    ): List<Recommendation> {
        val allPresets = presetRepository.getAllPresets()
        val trendingItems = getTrendingPresets(category, limit)

        return trendingItems.mapNotNull { item ->
            allPresets.find { it.id == item.presetId }?.let { preset ->
                val trendReason = when {
                    item.growthRate > 0.3f -> "热度飙升${(item.growthRate * 100).toInt()}%"
                    item.rank <= 3 -> "本周TOP${item.rank}热门"
                    else -> "${item.viewCount}人正在使用"
                }

                Recommendation(
                    preset = preset,
                    reason = trendReason,
                    similarityScore = item.trendScore,
                    recommendationType = RecommendationType.TRENDING,
                    confidence = 0.85f,
                    trendingRank = item.rank
                )
            }
        }
    }

    /**
     * 获取"猜你喜欢"推荐
     * 综合多种推荐策略的结果
     *
     * @param limit 推荐数量限制
     * @return 推荐列表
     */
    suspend fun getGuessYouLikeRecommendations(
        limit: Int = SECTION_RECOMMENDATION_LIMIT
    ): List<Recommendation> {
        val contentBased = getContentBasedRecommendations(limit / 2)
        val collaborative = getCollaborativeFilteringRecommendations(limit / 2)

        // 合并并重新排序
        return (contentBased + collaborative)
            .distinctBy { it.preset.id }
            .shuffled() // 增加多样性
            .take(limit)
    }

    /**
     * 获取推荐板块数据
     * 用于推荐页面的分组展示
     *
     * @return 推荐板块列表
     */
    suspend fun getRecommendationSections(): List<RecommendationSection> {
        val sections = mutableListOf<RecommendationSection>()

        // 1. 为你推荐（个性化综合推荐）
        val forYouRecommendations = getPersonalizedRecommendations(10).recommendations
        if (forYouRecommendations.isNotEmpty()) {
            sections.add(
                RecommendationSection(
                    title = "为你推荐",
                    subtitle = "根据你的喜好精心挑选",
                    recommendations = forYouRecommendations,
                    sectionType = SectionType.FOR_YOU
                )
            )
        }

        // 2. 猜你喜欢
        val guessYouLike = getGuessYouLikeRecommendations(10)
        if (guessYouLike.isNotEmpty()) {
            sections.add(
                RecommendationSection(
                    title = "猜你喜欢",
                    subtitle = "发现更多符合你口味的预设",
                    recommendations = guessYouLike,
                    sectionType = SectionType.GUESS_YOU_LIKE
                )
            )
        }

        // 3. 相似用户喜欢
        val similarUsers = getCollaborativeFilteringRecommendations(10)
        if (similarUsers.isNotEmpty()) {
            sections.add(
                RecommendationSection(
                    title = "相似用户也喜欢",
                    subtitle = "看看和你兴趣相似的用户在用什么",
                    recommendations = similarUsers,
                    sectionType = SectionType.SIMILAR_USERS
                )
            )
        }

        // 4. 热门趋势
        val trending = getTrendingRecommendations(TrendingCategory.WEEKLY, 10)
        if (trending.isNotEmpty()) {
            sections.add(
                RecommendationSection(
                    title = "热门趋势",
                    subtitle = "本周最受欢迎的预设",
                    recommendations = trending,
                    sectionType = SectionType.TRENDING
                )
            )
        }

        return sections
    }

    /**
     * 获取推荐流（实时更新）
     */
    fun getRecommendationsFlow(): Flow<RecommendationResult> = flow {
        val result = getPersonalizedRecommendations()
        emit(result)
    }

    /**
     * 获取组合推荐流
     * 结合用户行为实时变化
     */
    fun getCombinedRecommendationsFlow(): Flow<List<RecommendationSection>> = flow {
        val sections = getRecommendationSections()
        emit(sections)
    }

    // ==================== 推荐算法核心方法 ====================

    /**
     * 计算推荐分数
     * 综合多种因素计算预设的推荐分数
     *
     * @param preset 预设
     * @param userBehaviors 用户行为历史
     * @return 推荐分数 (0.0 - 1.0)
     */
    private fun calculateRecommendationScore(
        preset: Preset,
        userBehaviors: List<UserBehavior>
    ): Float {
        val weights = BehaviorWeights()

        // 1. 使用频率分数（40%）
        val usageScore = calculateUsageScore(preset.id, userBehaviors) * weights.usageWeight

        // 2. 收藏分数（30%）
        val favoriteScore = calculateFavoriteScore(preset.id, userBehaviors) * weights.favoriteWeight

        // 3. 浏览分数（20%）
        val browseScore = calculateBrowseScore(preset.id, userBehaviors) * weights.browseWeight

        // 4. 搜索相关分数（10%）
        val searchScore = calculateSearchScore(preset, userBehaviors) * weights.searchWeight

        // 5. 内容相似度加成
        val contentBonus = calculateContentBonus(preset, userBehaviors)

        // 6. 时间衰减因子
        val timeDecay = calculateAverageTimeDecay(userBehaviors)

        // 综合分数
        val baseScore = usageScore + favoriteScore + browseScore + searchScore
        val finalScore = (baseScore + contentBonus) * timeDecay

        return finalScore.coerceIn(0f, 1f)
    }

    /**
     * 计算使用频率分数
     */
    private fun calculateUsageScore(
        presetId: String,
        behaviors: List<UserBehavior>
    ): Float {
        val usageBehaviors = behaviors.filter {
            it.presetId == presetId && it.behaviorType == BehaviorType.APPLY
        }

        if (usageBehaviors.isEmpty()) return 0f

        // 考虑时间衰减的使用次数
        val weightedCount = usageBehaviors.sumOf { behavior ->
            (behavior.calculateTimeDecayFactor(TIME_DECAY_HALF_LIFE) * 10).toDouble()
        }

        // 归一化到 0-1
        return (weightedCount / 50.0).coerceAtMost(1.0).toFloat()
    }

    /**
     * 计算收藏分数
     */
    private fun calculateFavoriteScore(
        presetId: String,
        behaviors: List<UserBehavior>
    ): Float {
        val favoriteBehaviors = behaviors.filter {
            it.presetId == presetId && it.behaviorType == BehaviorType.FAVORITE
        }
        val unfavoriteBehaviors = behaviors.filter {
            it.presetId == presetId && it.behaviorType == BehaviorType.UNFAVORITE
        }

        // 如果取消收藏过，降低分数
        if (unfavoriteBehaviors.isNotEmpty()) return -0.3f

        if (favoriteBehaviors.isEmpty()) return 0f

        return 1.0f
    }

    /**
     * 计算浏览分数
     */
    private fun calculateBrowseScore(
        presetId: String,
        behaviors: List<UserBehavior>
    ): Float {
        val viewBehaviors = behaviors.filter {
            it.presetId == presetId && it.behaviorType == BehaviorType.VIEW
        }

        if (viewBehaviors.isEmpty()) return 0f

        // 考虑浏览时长和次数
        val weightedScore = viewBehaviors.sumOf { behavior ->
            val durationBonus = (behavior.duration / 5000.0).coerceAtMost(1.0) // 最多5秒算满分
            val timeDecay = behavior.calculateTimeDecayFactor(TIME_DECAY_HALF_LIFE)
            (durationBonus + 0.5) * timeDecay
        }

        return (weightedScore / 10.0).coerceAtMost(1.0).toFloat()
    }

    /**
     * 计算搜索相关分数
     */
    private fun calculateSearchScore(
        preset: Preset,
        behaviors: List<UserBehavior>
    ): Float {
        val searchBehaviors = behaviors.filter {
            it.behaviorType == BehaviorType.SEARCH
        }

        if (searchBehaviors.isEmpty()) return 0f

        // 检查预设标签是否与搜索关键词匹配
        var matchCount = 0
        searchBehaviors.forEach { behavior ->
            val searchKeywords = behavior.metadata["keywords"]?.split(",") ?: emptyList()
            val matchedTags = preset.tags.intersect(searchKeywords.toSet())
            if (matchedTags.isNotEmpty()) {
                matchCount++
            }
        }

        return (matchCount / 5.0f).coerceAtMost(1.0f)
    }

    /**
     * 计算内容相似度加成
     */
    private fun calculateContentBonus(
        preset: Preset,
        behaviors: List<UserBehavior>
    ): Float {
        // 获取用户喜欢的预设的标签
        val favoritePresetIds = behaviors
            .filter { it.behaviorType == BehaviorType.FAVORITE }
            .map { it.presetId }

        if (favoritePresetIds.isEmpty()) return 0f

        // 这里简化处理，实际应从repository获取完整预设信息
        // 返回一个基于标签匹配的小加成
        return 0.1f
    }

    /**
     * 计算平均时间衰减因子
     */
    private fun calculateAverageTimeDecay(behaviors: List<UserBehavior>): Float {
        if (behaviors.isEmpty()) return 1f

        val decaySum = behaviors.sumOf {
            it.calculateTimeDecayFactor(TIME_DECAY_HALF_LIFE).toDouble()
        }

        return (decaySum / behaviors.size).toFloat().coerceIn(0.5f, 1.0f)
    }

    // ==================== 辅助方法 ====================

    /**
     * 创建推荐对象
     */
    private fun createRecommendation(
        preset: Preset,
        score: Float,
        userBehaviors: List<UserBehavior>
    ): Recommendation {
        // 确定推荐类型
        val recommendationType = when {
            score > 0.8f -> RecommendationType.CONTENT_BASED
            score > 0.5f -> RecommendationType.COLLABORATIVE_FILTERING
            else -> RecommendationType.POPULAR
        }

        // 生成推荐理由
        val reason = generateRecommendationReason(preset, userBehaviors, recommendationType)

        return Recommendation(
            preset = preset,
            reason = reason,
            similarityScore = score,
            recommendationType = recommendationType,
            confidence = score.coerceIn(0.5f, 0.95f)
        )
    }

    /**
     * 生成推荐理由
     */
    private fun generateRecommendationReason(
        preset: Preset,
        behaviors: List<UserBehavior>,
        recommendationType: RecommendationType
    ): String {
        return when (recommendationType) {
            RecommendationType.CONTENT_BASED -> {
                val topTag = preset.tags.firstOrNull() ?: "摄影"
                "因为你喜欢${topTag}风格"
            }
            RecommendationType.COLLABORATIVE_FILTERING -> {
                "与你兴趣相似的用户也在使用"
            }
            RecommendationType.TRENDING -> {
                "本周热门预设"
            }
            RecommendationType.NEW_USER -> {
                "新用户精选推荐"
            }
            else -> {
                "根据你的浏览历史推荐"
            }
        }
    }

    /**
     * 提取用户标签偏好（带权重）
     */
    private fun extractUserTagPreferences(
        behaviors: List<UserBehavior>
    ): Map<String, Float> {
        val tagWeights = mutableMapOf<String, Float>()

        behaviors.forEach { behavior ->
            val weight = when (behavior.behaviorType) {
                BehaviorType.FAVORITE -> 1.0f
                BehaviorType.APPLY -> 0.8f
                BehaviorType.VIEW -> 0.4f
                else -> 0.2f
            }

            val timeDecay = behavior.calculateTimeDecayFactor(TIME_DECAY_HALF_LIFE)
            val finalWeight = weight * timeDecay

            // 从metadata中提取标签信息（简化处理）
            val tags = behavior.metadata["tags"]?.split(",") ?: emptyList()
            tags.forEach { tag ->
                tagWeights[tag] = (tagWeights[tag] ?: 0f) + finalWeight
            }
        }

        return tagWeights
    }

    /**
     * 提取用户场景偏好
     */
    private fun extractUserScenePreferences(
        behaviors: List<UserBehavior>
    ): Set<String> {
        return behaviors
            .filter { it.behaviorType in listOf(BehaviorType.FAVORITE, BehaviorType.APPLY) }
            .mapNotNull { it.metadata["sceneType"] }
            .toSet()
    }

    /**
     * 计算标签相似度
     */
    private fun calculateTagSimilarity(
        presetTags: List<String>,
        userTagPreferences: Map<String, Float>
    ): Float {
        if (presetTags.isEmpty() || userTagPreferences.isEmpty()) return 0f

        val matchedWeight = presetTags.sumOf { tag ->
            (userTagPreferences[tag] ?: 0f).toDouble()
        }

        val maxPossibleWeight = userTagPreferences.values.sum()
        if (maxPossibleWeight == 0f) return 0f

        return (matchedWeight / maxPossibleWeight).toFloat().coerceIn(0f, 1f)
    }

    /**
     * 查找相似用户（模拟实现）
     */
    private fun findSimilarUsers(userFavoritePresets: List<String>): List<SimilarUser> {
        // 模拟相似用户数据
        // 实际应用中应从服务器获取真实的相似用户数据
        return listOf(
            SimilarUser(
                userId = "similar_user_1",
                similarityScore = 0.85f,
                commonPresets = userFavoritePresets.take(2)
            ),
            SimilarUser(
                userId = "similar_user_2",
                similarityScore = 0.72f,
                commonPresets = userFavoritePresets.take(1)
            ),
            SimilarUser(
                userId = "similar_user_3",
                similarityScore = 0.68f,
                commonPresets = emptyList()
            )
        ).filter { it.similarityScore >= SIMILARITY_THRESHOLD }
    }

    /**
     * 模拟相似用户的偏好（用于演示）
     */
    private fun simulateSimilarUserPreferences(userId: String): List<String> {
        return when (userId) {
            "similar_user_1" -> listOf("film_portrait_01", "night_urban_01", "food_style_01")
            "similar_user_2" -> listOf("hncs_landscape_master", "street_documentary")
            "similar_user_3" -> listOf("film_portrait_01", "hncs_portrait_master")
            else -> emptyList()
        }
    }

    /**
     * 计算共同标签
     */
    private fun calculateCommonTags(
        userPresets: List<Preset>,
        targetPreset: Preset
    ): String {
        val userTags = userPresets.flatMap { it.tags }.toSet()
        val commonTags = targetPreset.tags.intersect(userTags)
        return commonTags.take(2).joinToString("、")
    }

    /**
     * 获取热门预设列表
     */
    private suspend fun getTrendingPresets(
        category: TrendingCategory,
        limit: Int
    ): List<TrendingItem> {
        // 从行为追踪器获取统计数据
        val viewCounts = behaviorTracker.presetViewCountsFlow.first()
        val usageCounts = behaviorTracker.presetUsageCountsFlow.first()

        // 计算趋势分数
        val allPresets = presetRepository.getAllPresets()

        return allPresets.mapIndexed { index, preset ->
            val views = viewCounts[preset.id] ?: 0
            val usages = usageCounts[preset.id] ?: 0

            // 趋势分数 = 浏览量 * 0.4 + 使用量 * 0.6
            val trendScore = (views * 0.4f + usages * 0.6f) / 100f

            // 模拟增长率
            val growthRate = kotlin.random.Random.nextFloat() * 0.5f

            TrendingItem(
                presetId = preset.id,
                preset = preset,
                trendScore = trendScore.coerceIn(0f, 1f),
                rank = index + 1,
                growthRate = growthRate,
                category = category,
                viewCount = views,
                downloadCount = usages
            )
        }
            .sortedByDescending { it.trendScore }
            .take(limit)
            .mapIndexed { index, item -> item.copy(rank = index + 1) }
    }

    /**
     * 计算余弦相似度
     */
    private fun calculateCosineSimilarity(
        vector1: Map<String, Float>,
        vector2: Map<String, Float>
    ): Float {
        val allKeys = vector1.keys.union(vector2.keys)

        var dotProduct = 0f
        var norm1 = 0f
        var norm2 = 0f

        allKeys.forEach { key ->
            val v1 = vector1[key] ?: 0f
            val v2 = vector2[key] ?: 0f

            dotProduct += v1 * v2
            norm1 += v1 * v1
            norm2 += v2 * v2
        }

        if (norm1 == 0f || norm2 == 0f) return 0f

        return dotProduct / (sqrt(norm1) * sqrt(norm2))
    }

    // ==================== 用户偏好管理 ====================

    /**
     * 获取用户偏好
     */
    suspend fun getUserPreference(): UserPreference {
        val tags = behaviorTracker.userPreferenceTagsFlow.first().toList()
        val scenes = behaviorTracker.userPreferredScenesFlow.first().toList()

        return UserPreference(
            favoriteTags = tags,
            preferredSceneTypes = scenes
        )
    }

    /**
     * 更新用户偏好
     */
    suspend fun updateUserPreference(preference: UserPreference) {
        behaviorTracker.updatePreferenceTags(preference.favoriteTags)
        behaviorTracker.updatePreferredScenes(preference.preferredSceneTypes)
    }
}

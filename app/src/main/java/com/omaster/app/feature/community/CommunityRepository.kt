package com.omaster.app.feature.community

import com.omaster.app.feature.community.model.CommunityPreset
import com.omaster.app.feature.community.model.QualityScore
import com.omaster.app.model.Preset
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import timber.log.Timber
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CommunityRepository @Inject constructor() {

    private val _communityPresets = MutableStateFlow<List<CommunityPreset>>(emptyList())
    val communityPresets: StateFlow<List<CommunityPreset>> = _communityPresets.asStateFlow()

    private val sampleCommunityPresets = listOf(
        CommunityPreset(
            id = "c1",
            name = "哈苏大师人像",
            creatorId = "user1",
            creatorName = "摄影师小王",
            creatorAvatar = null,
            presetData = Preset(
                id = "p1",
                name = "哈苏大师人像",
                coverPath = "portrait",
                deviceModel = "Find X8 Pro",
                source = "community"
            ),
            sampleImages = listOf("sample1", "sample2"),
            likes = 234,
            downloads = 1500,
            rating = 4.8f,
            ratingCount = 128,
            tags = listOf("人像", "哈苏", "大师"),
            description = "适合户外人像拍摄，肤色自然，背景虚化优美",
            creationDate = Instant.now(),
            isVerified = true,
            isFeatured = true
        ),
        CommunityPreset(
            id = "c2",
            name = "夜景霓虹",
            creatorId = "user2",
            creatorName = "城市猎人",
            presetData = Preset(
                id = "p2",
                name = "夜景霓虹",
                coverPath = "night",
                deviceModel = "Find X8 Ultra",
                source = "community"
            ),
            sampleImages = listOf("night1", "night2"),
            likes = 189,
            downloads = 980,
            rating = 4.6f,
            ratingCount = 87,
            tags = listOf("夜景", "城市", "霓虹"),
            description = "城市夜景必备，提升霓虹色彩饱和度",
            creationDate = Instant.now().minusSeconds(86400),
            isVerified = false,
            isFeatured = true
        )
    )

    init {
        _communityPresets.value = sampleCommunityPresets
    }

    fun getFeaturedPresets(): Flow<List<CommunityPreset>> {
        return _communityPresets.map { presets ->
            presets.filter { it.isFeatured }
        }
    }

    fun getTrendingPresets(): Flow<List<CommunityPreset>> {
        return _communityPresets.map { presets ->
            presets.sortedByDescending { it.likes + it.downloads }
        }
    }

    suspend fun likePreset(presetId: String) {
        _communityPresets.update { presets ->
            presets.map { preset ->
                if (preset.id == presetId) {
                    preset.copy(likes = preset.likes + 1)
                } else {
                    preset
                }
            }
        }
        Timber.d("点赞预设: $presetId")
    }

    suspend fun downloadPreset(presetId: String) {
        _communityPresets.update { presets ->
            presets.map { preset ->
                if (preset.id == presetId) {
                    preset.copy(downloads = preset.downloads + 1)
                } else {
                    preset
                }
            }
        }
        Timber.d("下载预设: $presetId")
    }

    suspend fun uploadPreset(preset: CommunityPreset) {
        _communityPresets.update { presets ->
            presets + preset
        }
        Timber.d("上传预设: ${preset.name}")
    }

    fun evaluatePresetQuality(preset: Preset): QualityScore {
        val technicalScore = if (preset.cameraParams?.hasselblad_hncs == true) 0.9f else 0.7f
        val aestheticScore = 0.8f
        val usabilityScore = 0.85f
        val communityScore = 0.75f
        val overallScore = (technicalScore + aestheticScore + usabilityScore + communityScore) / 4f

        return QualityScore(
            technicalScore = technicalScore,
            aestheticScore = aestheticScore,
            usabilityScore = usabilityScore,
            communityScore = communityScore,
            overallScore = overallScore
        )
    }
}

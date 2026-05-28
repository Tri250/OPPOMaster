package com.omaster.app.feature.community.model

import java.time.Instant

data class CommunityPreset(
    val id: String,
    val name: String,
    val creatorId: String,
    val creatorName: String,
    val creatorAvatar: String? = null,
    val presetData: com.omaster.app.model.Preset,
    val sampleImages: List<String>,
    val likes: Int = 0,
    val downloads: Int = 0,
    val rating: Float = 0f,
    val ratingCount: Int = 0,
    val tags: List<String>,
    val description: String = "",
    val creationDate: Instant,
    val isVerified: Boolean = false,
    val isFeatured: Boolean = false
)

data class Comment(
    val id: String,
    val presetId: String,
    val userId: String,
    val userName: String,
    val userAvatar: String? = null,
    val content: String,
    val timestamp: Instant,
    val likes: Int = 0
)

data class QualityScore(
    val technicalScore: Float,
    val aestheticScore: Float,
    val usabilityScore: Float,
    val communityScore: Float,
    val overallScore: Float
)

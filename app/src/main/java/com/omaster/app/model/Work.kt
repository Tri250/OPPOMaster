package com.omaster.app.model

import java.util.Date

// ============================================
// OPPO OMaster 作品模型 - UGC创作核心
// ============================================

data class Work(
    val id: String,
    val userId: String,
    val userName: String,
    val userAvatar: String? = null,
    val photoUrl: String,
    val title: String = "",
    val description: String = "",
    val presetId: String? = null,
    val presetName: String? = null,
    val styleType: StyleType = StyleType.NATURAL,
    val sceneTags: List<SceneTag> = emptyList(),
    val deviceModel: String = "",
    val location: String? = null,
    val takenAt: Date = Date(),
    
    // 社交数据
    val likeCount: Int = 0,
    val commentCount: Int = 0,
    val shareCount: Int = 0,
    val saveCount: Int = 0,
    val isLikedByMe: Boolean = false,
    val isSavedByMe: Boolean = false,
    
    // 专业数据
    val cameraParams: CameraParams? = null,
    val exifData: ExifData? = null,
    val hasLocation: Boolean = false,
    
    // 编辑信息
    val editedAt: Date? = null,
    val isVerified: Boolean = false
) {
    val isPresetUsed: Boolean
        get() = presetId != null
    
    val isPro: Boolean
        get() = cameraParams != null || exifData != null
}

data class ExifData(
    val iso: Int? = null,
    val shutterSpeed: String? = null,
    val aperture: Float? = null,
    val focalLength: Float? = null,
    val whiteBalance: Int? = null,
    val exposureCompensation: Float? = null,
    val flash: Boolean? = null,
    val lensModel: String? = null
)

data class Comment(
    val id: String,
    val userId: String,
    val userName: String,
    val userAvatar: String? = null,
    val content: String,
    val createdAt: Date = Date(),
    val likes: Int = 0,
    val replyTo: String? = null
)

data class UserProfile(
    val id: String,
    val name: String,
    val avatar: String? = null,
    val bio: String = "",
    val followers: Int = 0,
    val following: Int = 0,
    val worksCount: Int = 0,
    val presetsCount: Int = 0,
    val isVerified: Boolean = false,
    val isHncsCreator: Boolean = false,
    val joinDate: Date = Date()
)

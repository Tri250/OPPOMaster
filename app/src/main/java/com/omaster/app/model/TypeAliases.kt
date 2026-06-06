/**
 * 模型类型别名
 * 将 domain.model 包中的类暴露为 model 包的类，保持向后兼容
 */
package com.omaster.app.model

// 相机参数相关
typealias CameraParams = com.omaster.app.domain.model.CameraParams
typealias CameraConfig = com.omaster.app.domain.model.CameraConfig
typealias CameraConfigExport = com.omaster.app.domain.model.CameraConfigExport
typealias CameraMode = com.omaster.app.domain.model.CameraMode
typealias ColorStyle = com.omaster.app.domain.model.ColorStyle
typealias FocalLengthMode = com.omaster.app.domain.model.FocalLengthMode

// 预设相关
typealias Preset = com.omaster.app.domain.model.Preset
typealias PresetSection = com.omaster.app.domain.model.PresetSection
typealias AiAdjustmentParams = com.omaster.app.domain.model.AiAdjustmentParams

// 场景相关
typealias SceneType = com.omaster.app.domain.model.SceneType

// 水印相关
typealias WatermarkConfig = com.omaster.app.domain.model.WatermarkConfig
typealias TextWatermark = com.omaster.app.domain.model.TextWatermark
typealias ImageWatermark = com.omaster.app.domain.model.ImageWatermark
typealias WatermarkPosition = com.omaster.app.domain.model.WatermarkPosition
typealias WatermarkStyle = com.omaster.app.domain.model.WatermarkStyle

// 社区相关
typealias UserSubmission = com.omaster.app.domain.model.UserSubmission
typealias Comment = com.omaster.app.domain.model.Comment
typealias Rating = com.omaster.app.domain.model.Rating
typealias Like = com.omaster.app.domain.model.Like
typealias CommunityStats = com.omaster.app.domain.model.CommunityStats
typealias CommunityFilter = com.omaster.app.domain.model.CommunityFilter
typealias CommunitySortType = com.omaster.app.domain.model.CommunitySortType
typealias SubmissionStatus = com.omaster.app.domain.model.SubmissionStatus
typealias LikeTargetType = com.omaster.app.domain.model.LikeTargetType
typealias SubmissionRequest = com.omaster.app.domain.model.SubmissionRequest
typealias SubmissionImage = com.omaster.app.domain.model.SubmissionImage
typealias SubmissionPageResult = com.omaster.app.domain.model.SubmissionPageResult
typealias CommentPageResult = com.omaster.app.domain.model.CommentPageResult
typealias RatingDistribution = com.omaster.app.domain.model.RatingDistribution

// 推荐相关
typealias SceneRecommendation = com.omaster.app.domain.model.SceneRecommendation
typealias RecommendationResult = com.omaster.app.domain.model.RecommendationResult
typealias PresetRecommendation = com.omaster.app.domain.model.PresetRecommendation

// 节日相关
typealias Holiday = com.omaster.app.domain.model.Holiday

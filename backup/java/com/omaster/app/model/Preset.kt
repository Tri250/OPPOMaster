package com.omaster.app.model

// ============================================
// OPPO 哈苏预设模型 - 专业级完整定义
// ============================================

data class Section(
    val title: String,
    val content: String
)

data class Preset(
    val id: String,
    val name: String,
    val coverPath: String,
    val sections: List<Section> = emptyList(),
    val cameraParams: CameraParams? = null,
    val fineTuneParams: FineTuneParams = FineTuneParams.HNCS_DEFAULT,
    val deviceModel: String = "",
    val compatibleDevices: List<String> = emptyList(),
    val source: String = "omaster_cloud",
    val isFavorite: Boolean = false,
    val isCloudSynced: Boolean = false,
    val isLocalOnly: Boolean = false,
    
    // 新增字段 - 专业级扩展
    val styleType: StyleType = StyleType.NATURAL,
    val sceneTags: List<SceneTag> = emptyList(),
    val creator: Creator? = null,
    val stats: PresetStats = PresetStats(),
    val description: String = "",
    val usageTips: List<String> = emptyList(),
    val examplePhotos: List<String> = emptyList(),
    
    // 版本控制
    val version: Int = 1,
    val hash: String = ""
) {
    // 便捷属性
    val isHncsCertified: Boolean
        get() = cameraParams?.hasselblad_hncs == true
    
    val hasCreator: Boolean
        get() = creator != null
    
    val isPopular: Boolean
        get() = stats.usageCount > 1000
    
    val isHighlyRated: Boolean
        get() = stats.rating >= 4.5f
    
    companion object {
        // 创建哈苏HNCS认证预设
        fun createHncsPreset(
            id: String,
            name: String,
            coverPath: String,
            deviceModel: String,
            cameraParams: CameraParams,
            fineTuneParams: FineTuneParams = FineTuneParams.HNCS_DEFAULT,
            styleType: StyleType = StyleType.NATURAL,
            sceneTags: List<SceneTag> = emptyList(),
            description: String = "",
            sections: List<Section> = emptyList()
        ): Preset {
            return Preset(
                id = id,
                name = name,
                coverPath = coverPath,
                sections = sections,
                cameraParams = cameraParams.copy(hasselblad_hncs = true),
                fineTuneParams = fineTuneParams,
                deviceModel = deviceModel,
                styleType = styleType,
                sceneTags = sceneTags,
                description = description,
                source = "hncs_certified",
                version = 1
            )
        }
        
        // 创建社区预设
        fun createCommunityPreset(
            id: String,
            name: String,
            coverPath: String,
            creator: Creator,
            cameraParams: CameraParams,
            fineTuneParams: FineTuneParams,
            deviceModel: String,
            styleType: StyleType,
            sceneTags: List<SceneTag> = emptyList(),
            description: String = ""
        ): Preset {
            return Preset(
                id = id,
                name = name,
                coverPath = coverPath,
                cameraParams = cameraParams,
                fineTuneParams = fineTuneParams,
                deviceModel = deviceModel,
                creator = creator,
                styleType = styleType,
                sceneTags = sceneTags,
                description = description,
                source = "community",
                stats = PresetStats(),
                version = 1
            )
        }
    }
}

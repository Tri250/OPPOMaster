package com.omaster.app.model

/**
 * Preset - 预设数据模型
 * 支持2026年 OPPO Find X8 Ultra 哈苏大师模式
 */

data class Section(
    val title: String,
    val content: String
)

/**
 * 样张展示数据模型
 */
data class SampleImage(
    val id: String,
    val imagePath: String,
    val title: String,
    val description: String,
    val isBeforeImage: Boolean = false,
    val isAfterImage: Boolean = false
)

/**
 * 预设数据模型
 */
data class Preset(
    val id: String,
    val name: String,
    val coverPath: String,
    val coverUrl: String = "",
    val sections: List<Section> = emptyList(),
    val cameraParams: CameraParams? = null,
    val deviceModel: String = "",
    val source: String = "omaster_cloud",
    val isFavorite: Boolean = false,
    
    // 扩展元数据
    val author: String = "哈苏影像实验室",
    val description: String = "",
    val sceneType: String = "",
    val tags: List<String> = emptyList(),
    val rating: Float = 5.0f,
    val downloadCount: Int = 0,
    val favoriteCount: Int = 0,
    val version: String = "3.0",
    val lastUpdated: Long = System.currentTimeMillis(),
    val publishDate: Long = System.currentTimeMillis(),
    val isHncsCertified: Boolean = cameraParams?.hasselblad_hncs == true,
    
    // 样张展示
    val sampleImages: List<SampleImage> = emptyList(),
    
    // 兼容性别名
    val hasselbladHncs: Boolean = cameraParams?.hasselblad_hncs ?: false,
    val isOfficialSource: Boolean = source == "hasselblad_official" || author.contains("哈苏官方"),
    val canModify: Boolean = !isHncsCertified || !isOfficialSource
) {
    /**
     * 获取设备显示名称
     */
    fun getDeviceDisplay(): String {
        return when {
            deviceModel.isNotEmpty() -> deviceModel
            deviceModel.contains("Find X", ignoreCase = true) -> "OPPO Find X 系列"
            deviceModel.contains("Reno", ignoreCase = true) -> "OPPO Reno 系列"
            deviceModel.contains("Find N", ignoreCase = true) -> "OPPO Find N 系列"
            else -> "通用设备"
        }
    }
    
    /**
     * 获取场景类型中文显示
     */
    fun getSceneTypeDisplay(): String {
        return when (sceneType.lowercase()) {
            "portrait", "人像" -> "人像摄影"
            "landscape", "风景" -> "风景摄影"
            "night", "夜景" -> "夜景摄影"
            "sunset", "日落" -> "日落摄影"
            "food", "美食" -> "美食摄影"
            "street", "街拍" -> "街拍摄影"
            "macro", "微距" -> "微距摄影"
            "still_life", "静物" -> "静物摄影"
            else -> "通用摄影"
        }
    }
    
    /**
     * 获取完整的营销参数展示
     */
    fun getMarketingParams(): Map<String, String> {
        return cameraParams?.formatFullParams() ?: emptyMap()
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
     * 获取格式化收藏量
     */
    fun getFormattedFavoriteCount(): String {
        return when {
            favoriteCount >= 1000000 -> String.format("%.1fM", favoriteCount / 1000000.0)
            favoriteCount >= 1000 -> String.format("%.1fK", favoriteCount / 1000.0)
            else -> favoriteCount.toString()
        }
    }
    
    /**
     * 获取发布日期格式化
     */
    fun getFormattedPublishDate(): String {
        val date = java.util.Date(publishDate)
        val format = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
        return format.format(date)
    }
    
    /**
     * 获取HNCS认证说明
     */
    fun getHncsCertificationText(): String {
        return if (isHncsCertified) {
            "本预设已通过哈苏自然色彩解决方案 (HNCS) 认证，还原真实自然色彩"
        } else {
            ""
        }
    }
    
    /**
     * 获取版本信息
     */
    fun getVersionInfo(): String {
        return "v$version"
    }
    
    /**
     * 转换为JSON格式用于同步
     */
    fun toJson(): Map<String, Any?> {
        return mapOf(
            "id" to id,
            "name" to name,
            "coverUrl" to coverUrl,
            "deviceModel" to deviceModel,
            "source" to source,
            "author" to author,
            "description" to description,
            "sceneType" to sceneType,
            "tags" to tags,
            "rating" to rating,
            "downloadCount" to downloadCount,
            "favoriteCount" to favoriteCount,
            "version" to version,
            "lastUpdated" to lastUpdated,
            "publishDate" to publishDate,
            "isHncsCertified" to isHncsCertified,
            "cameraParams" to cameraParams?.toJsonMap()
        )
    }
    
    companion object {
        /**
         * 从 JSON 创建 Preset
         */
        fun fromJson(json: Map<String, Any?>): Preset {
            val paramsJson = json["cameraParams"] as? Map<String, Any?>
            val cameraParams = paramsJson?.let { CameraParams.fromJsonMap(it) }
            
            return Preset(
                id = json["id"] as? String ?: "",
                name = json["name"] as? String ?: "",
                coverPath = json["coverPath"] as? String ?: "",
                coverUrl = json["coverUrl"] as? String ?: "",
                cameraParams = cameraParams,
                deviceModel = json["deviceModel"] as? String ?: "",
                source = json["source"] as? String ?: "omaster_cloud",
                author = json["author"] as? String ?: "哈苏影像实验室",
                description = json["description"] as? String ?: "",
                sceneType = json["sceneType"] as? String ?: "",
                tags = (json["tags"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
                rating = (json["rating"] as? Number)?.toFloat() ?: 5.0f,
                downloadCount = (json["downloadCount"] as? Number)?.toInt() ?: 0,
                favoriteCount = (json["favoriteCount"] as? Number)?.toInt() ?: 0,
                version = json["version"] as? String ?: "3.0",
                lastUpdated = (json["lastUpdated"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                publishDate = (json["publishDate"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                isHncsCertified = json["isHncsCertified"] as? Boolean ?: (cameraParams?.hasselblad_hncs == true)
            )
        }
        
        /**
         * 创建示例预设
         */
        fun createSamplePresets(): List<Preset> {
            return listOf(
                Preset(
                    id = "hncs_portrait_master",
                    name = "哈苏人像大师",
                    coverPath = "hncs_portrait",
                    deviceModel = "OPPO Find X8 Pro",
                    author = "哈苏官方",
                    description = "专为OPPO Find X8 Pro打造的人像摄影预设，采用哈苏自然色彩解决方案，还原真实肤色。",
                    sceneType = "portrait",
                    tags = listOf("人像", "HNCS", "肤色优化"),
                    downloadCount = 158642,
                    favoriteCount = 28453,
                    version = "3.0",
                    source = "hasselblad_official",
                    isHncsCertified = true,
                    cameraParams = CameraParams.createPortraitPreset()
                ),
                Preset(
                    id = "hncs_landscape_master",
                    name = "哈苏风景大师",
                    coverPath = "hncs_landscape",
                    deviceModel = "OPPO Find X8 Ultra",
                    author = "哈苏官方",
                    description = "风景摄影专用预设，精准还原天空、草地、树木等自然色彩。",
                    sceneType = "landscape",
                    tags = listOf("风景", "HNCS", "自然色彩"),
                    downloadCount = 126847,
                    favoriteCount = 19872,
                    version = "3.0",
                    source = "hasselblad_official",
                    isHncsCertified = true,
                    cameraParams = CameraParams.createLandscapePreset()
                ),
                Preset(
                    id = "film_portrait_01",
                    name = "胶片人像 · 暖调",
                    coverPath = "film_portrait_warm",
                    deviceModel = "OPPO Find X8",
                    author = "小O帮帮",
                    description = "复古胶片风格人像预设，温暖的色调营造怀旧氛围。",
                    sceneType = "portrait",
                    tags = listOf("人像", "胶片", "复古"),
                    downloadCount = 45231,
                    favoriteCount = 8921,
                    version = "2.5",
                    source = "omaster_cloud"
                ),
                Preset(
                    id = "night_urban_01",
                    name = "城市夜景 · 霓虹",
                    coverPath = "night_urban",
                    deviceModel = "OPPO Find N3",
                    author = "城市摄影师",
                    description = "城市夜景专用预设，增强霓虹灯光效果，降低噪点。",
                    sceneType = "night",
                    tags = listOf("夜景", "城市", "霓虹"),
                    downloadCount = 32156,
                    favoriteCount = 5623,
                    version = "2.0",
                    source = "omaster_cloud"
                ),
                Preset(
                    id = "food_style_01",
                    name = "美食摄影 · 鲜亮",
                    coverPath = "food_vibrant",
                    deviceModel = "OPPO Reno12 Pro",
                    author = "美食达人",
                    description = "提升美食色彩饱和度和鲜亮度，让食物更加诱人。",
                    sceneType = "food",
                    tags = listOf("美食", "鲜亮", "食欲"),
                    downloadCount = 28547,
                    favoriteCount = 4532,
                    version = "1.8",
                    source = "omaster_cloud"
                ),
                Preset(
                    id = "street_documentary",
                    name = "街头纪实 · 黑白",
                    coverPath = "street_bw",
                    deviceModel = "通用",
                    author = "纪实摄影师",
                    description = "黑白街头摄影预设，强调对比度和层次感。",
                    sceneType = "street",
                    tags = listOf("街拍", "黑白", "纪实"),
                    downloadCount = 19234,
                    favoriteCount = 3421,
                    version = "1.5",
                    source = "omaster_cloud"
                )
            )
        }
    }
}

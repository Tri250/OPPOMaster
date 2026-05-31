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
    val version: String = "3.0",
    val lastUpdated: Long = System.currentTimeMillis(),
    
    // 兼容性别名
    val hasselbladHncs: Boolean = cameraParams?.hasselbladHncs ?: false
) {
    /**
     * 获取完整的营销参数展示
     */
    fun getMarketingParams(): Map<String, String> {
        return cameraParams?.formatFullParams() ?: emptyMap()
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
            "version" to version,
            "lastUpdated" to lastUpdated,
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
                version = json["version"] as? String ?: "3.0",
                lastUpdated = (json["lastUpdated"] as? Number)?.toLong() ?: System.currentTimeMillis()
            )
        }
    }
}

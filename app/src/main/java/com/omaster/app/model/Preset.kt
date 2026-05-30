package com.omaster.app.model

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
    val deviceModel: String = "",
    val author: String? = null,
    val source: String = "omaster_cloud",
    val isFavorite: Boolean = false,
    val isNew: Boolean = false,
    val category: String? = null,
    val difficulty: String? = null,
    val tags: List<String> = emptyList(),
    val description: String? = null,
    val style: String? = null,
    val scene: String? = null
)

// 筛选条件
data class FilterConfig(
    val selectedStyle: String? = null,
    val selectedScene: String? = null,
    val searchQuery: String = "",
    val isFavoriteOnly: Boolean = false,
    val isNewOnly: Boolean = false
)

// 风格分类
object PresetStyles {
    const val ALL = "全部"
    const val FILM = "胶片"
    const val VINTAGE = "复古"
    const val FRESH = "清新"
    const val BLUE = "蓝调"
    const val DOCUMENTARY = "纪实"
    const val PORTRAIT = "人像"
    const val LANDSCAPE = "风景"
    const val CINEMA = "电影"
    const val FOOD = "美食"
    const val LIFE = "生活"
    const val STREET = "街拍"
    const val ARCHITECTURE = "建筑"
    const val B_W = "黑白"
    const val NEON = "霓虹"
    const val DREAM = "梦幻"
    
    val ALL_STYLES = listOf(
        ALL, FILM, VINTAGE, FRESH, BLUE, DOCUMENTARY, PORTRAIT,
        LANDSCAPE, CINEMA, FOOD, LIFE, STREET, ARCHITECTURE, B_W, NEON, DREAM
    )
}

// 场景分类
object PresetScenes {
    const val ALL = "全部"
    const val PORTRAIT = "人像"
    const val NIGHT = "夜景"
    const val LANDSCAPE = "风景"
    const val SPORTS = "运动"
    const val FOOD = "美食"
    const val STREET = "街拍"
    const val ARCHITECTURE = "建筑"
    const val NATURE = "自然"
    const val CINEMA = "电影"
    const val TRAVEL = "旅行"
    const val PARTY = "派对"
    const val FAMILY = "家庭"
    const val SELFIE = "自拍"
    
    val ALL_SCENES = listOf(
        ALL, PORTRAIT, NIGHT, LANDSCAPE, SPORTS, FOOD, STREET,
        ARCHITECTURE, NATURE, CINEMA, TRAVEL, PARTY, FAMILY, SELFIE
    )
}

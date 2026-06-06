package com.omaster.app.data

import com.omaster.app.model.CameraParams
import com.omaster.app.model.Preset
import com.omaster.app.model.Section

object PresetDataExpander {

    fun expandPresets(originalPresets: List<Preset>): List<Preset> {
        val expandedPresets = mutableListOf<Preset>()
        expandedPresets.addAll(originalPresets)
        
        expandedPresets.addAll(generatePortraitPresets())
        expandedPresets.addAll(generateLandscapePresets())
        expandedPresets.addAll(generateNightPresets())
        expandedPresets.addAll(generateFoodPresets())
        expandedPresets.addAll(generateArchitecturePresets())
        expandedPresets.addAll(generateStreetPresets())
        expandedPresets.addAll(generateFilmPresets())
        
        return expandedPresets.distinctBy { it.id }
    }

    private fun generatePortraitPresets(): List<Preset> {
        val portraits = listOf(
            "自然人像", "柔和人像", "电影人像", "复古人像", "时尚人像",
            "日光人像", "黄金时刻", "蓝调时刻", "低光人像", "逆光人像",
            "逆光人像", "高对比度", "低饱和度", "肤色优化", "艺术人像",
            "黑白人像", "电影感", "环境人像", "特写人像", "街头人像"
        )
        
        return portraits.mapIndexed { index, name ->
            Preset(
                id = "portrait_${index + 1}",
                name = name,
                coverPath = "portrait_${index + 1}",
                sections = listOf(
                    Section("人像优化", "保留肤色自然质感，适度磨皮"),
                    Section("色彩", "柔和肤色，暖色调")
                ),
                cameraParams = CameraParams(
                    mode = "人像模式",
                    filter = "人像",
                    iso = 100,
                    shutter = "1/125s",
                    ev = "0",
                    wb = "5200K",
                    hasselblad_hncs = index < 5
                ),
                deviceModel = "Find X8 Ultra",
                source = "omaster_cloud",
                isFavorite = index < 3
            )
        }
    }

    private fun generateLandscapePresets(): List<Preset> {
        val landscapes = listOf(
            "自然风光", "山川湖海", "森林秘境", "草原风光", "沙漠日落",
            "海岸风光", "山峰云海", "森林晨雾", "夕阳西下", "日出时分",
            "城市天际线", "湖光山色", "田园风光", "黄金日落", "蓝色时刻",
            "阴天风景", "彩虹时刻", "极光时刻", "星空夜景", "秋色风光",
            "雪景风光", "绿意盎然", "夏日风情", "秋日暖阳", "春日花海"
        )
        
        return landscapes.mapIndexed { index, name ->
            Preset(
                id = "landscape_${index + 1}",
                name = name,
                coverPath = "landscape_${index + 1}",
                sections = listOf(
                    Section("动态范围", "保留高光和阴影细节"),
                    Section("色彩", "增强自然色彩饱和度")
                ),
                cameraParams = CameraParams(
                    mode = "专业模式",
                    filter = "风光",
                    iso = 100,
                    shutter = "1/500s",
                    ev = "-0.3",
                    wb = "5600K",
                    hasselblad_hncs = index < 5
                ),
                deviceModel = "Find X8 Pro",
                source = "omaster_cloud",
                isFavorite = index < 3
            )
        }
    }

    private fun generateNightPresets(): List<Preset> {
        val nights = listOf(
            "城市夜景", "霓虹灯下", "星空夜景", "光绘摄影", "夜景人像",
            "烟花时刻", "街灯夜景", "车流光轨", "建筑夜景", "夜景风光",
            "蓝色夜景", "黑金城市", "赛博朋克", "复古夜景", "月光下",
            "车灯轨迹", "城市霓虹", "星空银河", "夜景人像", "暗夜人像",
            "手持夜景", "脚架夜景", "夜景色彩", "夜景黑白", "夜景电影"
        )
        
        return nights.mapIndexed { index, name ->
            Preset(
                id = "night_${index + 1}",
                name = name,
                coverPath = "night_${index + 1}",
                sections = listOf(
                    Section("降噪优化", "保留细节，抑制噪点"),
                    Section("曝光", "手持稳定拍摄")
                ),
                cameraParams = CameraParams(
                    mode = "夜景模式",
                    filter = "夜景",
                    iso = 800,
                    shutter = "1/30s",
                    ev = "0",
                    wb = "4500K",
                    hasselblad_hncs = index < 3
                ),
                deviceModel = "Find X8 Ultra",
                source = "omaster_cloud",
                isFavorite = index < 3
            )
        }
    }

    private fun generateFoodPresets(): List<Preset> {
        val foods = listOf(
            "美食美味", "新鲜色彩", "温暖烘焙", "甜品时间", "饮品时光",
            "美食暖色", "美食冷调", "新鲜水果", "咖啡时光", "下午茶点",
            "西餐风格", "日料风格", "中餐风格", "甜点物语", "美食家",
            "食物特写", "温暖食物", "清凉夏日", "美食家选", "厨房时光"
        )
        
        return foods.mapIndexed { index, name ->
            Preset(
                id = "food_${index + 1}",
                name = name,
                coverPath = "food_${index + 1}",
                sections = listOf(
                    Section("色彩", "增强食物的新鲜色彩"),
                    Section("细节", "保留食物质感")
                ),
                cameraParams = CameraParams(
                    mode = "专业模式",
                    filter = "美食",
                    iso = 200,
                    shutter = "1/200s",
                    ev = "+0.3",
                    wb = "5200K",
                    hasselblad_hncs = index < 3
                ),
                deviceModel = "Reno 12 Pro",
                source = "omaster_cloud",
                isFavorite = index < 2
            )
        }
    }

    private fun generateArchitecturePresets(): List<Preset> {
        val architectures = listOf(
            "建筑美学", "城市建筑", "现代建筑", "古典建筑", "建筑线条",
            "建筑光影", "建筑对称", "建筑色彩", "建筑细节", "建筑全景",
            "摩天大楼", "历史建筑", "教堂建筑", "建筑风光", "街景建筑",
            "建筑黑白", "建筑色彩", "建筑透视", "建筑倒影", "建筑人物"
        )
        
        return architectures.mapIndexed { index, name ->
            Preset(
                id = "architecture_${index + 1}",
                name = name,
                coverPath = "architecture_${index + 1}",
                sections = listOf(
                    Section("几何优化", "强化线条和结构"),
                    Section("透视", "建筑透视增强")
                ),
                cameraParams = CameraParams(
                    mode = "专业模式",
                    filter = "建筑",
                    iso = 100,
                    shutter = "1/250s",
                    ev = "0",
                    wb = "5600K",
                    hasselblad_hncs = index < 3
                ),
                deviceModel = "Find X8 Pro",
                source = "omaster_cloud",
                isFavorite = index < 2
            )
        }
    }

    private fun generateStreetPresets(): List<Preset> {
        val streets = listOf(
            "街头纪实", "街头色彩", "街头黑白", "街头电影", "街头风光",
            "街头人像", "街头光影", "街头瞬间", "街头抓拍", "街景故事",
            "街头人文", "街头生活", "街头艺术", "街头建筑", "街头夜景",
            "街头雨天", "街头晴天", "街头黄昏", "街头清晨", "街头日常"
        )
        
        return streets.mapIndexed { index, name ->
            Preset(
                id = "street_${index + 1}",
                name = name,
                coverPath = "street_${index + 1}",
                sections = listOf(
                    Section("纪实风格", "真实的街头记录"),
                    Section("抓怕", "高速快门捕捉瞬间")
                ),
                cameraParams = CameraParams(
                    mode = "专业模式",
                    filter = "黑白",
                    iso = 400,
                    shutter = "1/1000s",
                    ev = "0",
                    wb = "自动",
                    hasselblad_hncs = index < 3
                ),
                deviceModel = "Find X8",
                source = "omaster_cloud",
                isFavorite = index < 2
            )
        }
    }

    private fun generateFilmPresets(): List<Preset> {
        val films = listOf(
            "柯达Portra", "富士Velvia", "柯达Ektar", "富士Provia",
            "柯达Tri-X", "伊尔福HP5", "富士C200", "柯达Gold",
            "电影感", "复古胶片", "宝丽来风格", "富士Classic",
            "电影色彩", "柯达电影", "富士电影", "复古色调",
            "暖调胶片", "冷调胶片", "高饱和度", "低饱和度",
            "电影黑白", "彩色负片", "反转片", "怀旧复古",
            "宝丽来", "一次成像", "胶片颗粒", "怀旧色彩",
            "港风复古", "日式胶片", "欧式胶片", "美式复古"
        )
        
        return films.mapIndexed { index, name ->
            Preset(
                id = "film_${index + 1}",
                name = name,
                coverPath = "film_${index + 1}",
                sections = listOf(
                    Section("胶片质感", "模拟胶片颗粒和色彩"),
                    Section("风格", "经典胶片色彩表现")
                ),
                cameraParams = CameraParams(
                    mode = "专业模式",
                    filter = "复古",
                    iso = 200,
                    shutter = "1/125s",
                    ev = "0",
                    wb = "5400K",
                    hasselblad_hncs = index < 5
                ),
                deviceModel = "Find X8 Pro",
                source = "omaster_cloud",
                isFavorite = index < 3
            )
        }
    }
}

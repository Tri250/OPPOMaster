package com.omaster.app.data

import com.omaster.app.model.CameraParams
import com.omaster.app.model.Preset
import com.omaster.app.model.Section
import com.omaster.app.network.PresetApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PresetRepository @Inject constructor(
    private val preferencesDataStore: PreferencesDataStore,
    private val presetApi: PresetApi
) {
    private val samplePresets = listOf(
        Preset(
            id = "1",
            name = "哈苏 X2D | 慵懒午后的佛罗伦萨",
            coverPath = "hasselblad_florence_01",
            sections = listOf(
                Section("光感设置", "降低对比度，提高高光保留"),
                Section("色彩调校", "暖色调偏移，饱和度适中")
            ),
            cameraParams = CameraParams(
                mode = "master",
                filter = "复古",
                iso = 200,
                shutter = "1/250",
                ev = "+0.3",
                wb = "5600K",
                hasselblad_hncs = true
            ),
            deviceModel = "Find X8 Pro",
            source = "omaster_cloud"
        ),
        Preset(
            id = "2",
            name = "京都夜色 | 霓虹光斑",
            coverPath = "kyoto_night_01",
            sections = listOf(
                Section("夜景优化", "高ISO降噪，长曝光"),
                Section("色彩强化", "霓虹色饱和度提升")
            ),
            cameraParams = CameraParams(
                mode = "master",
                filter = "夜景",
                iso = 800,
                shutter = "1/30",
                ev = "-0.7",
                wb = "4200K",
                hasselblad_hncs = false
            ),
            deviceModel = "Find X8 Ultra",
            source = "omaster_cloud"
        ),
        Preset(
            id = "3",
            name = "北欧森林 | 自然清新",
            coverPath = "nordic_forest_01",
            sections = listOf(
                Section("绿色优化", "树叶色彩还原"),
                Section("动态范围", "高对比度保留细节")
            ),
            cameraParams = CameraParams(
                mode = "master",
                filter = "自然",
                iso = 100,
                shutter = "1/500",
                ev = "0",
                wb = "5200K",
                hasselblad_hncs = true
            ),
            deviceModel = "Reno 12 Pro",
            source = "omaster_cloud"
        ),
        Preset(
            id = "4",
            name = "海边日落 | 温暖橙调",
            coverPath = "sunset_beach_01",
            sections = listOf(
                Section("金色时刻", "暖色调强化"),
                Section("天空细节", "渐变层次保留")
            ),
            cameraParams = CameraParams(
                mode = "master",
                filter = "暖调",
                iso = 100,
                shutter = "1/200",
                ev = "+0.7",
                wb = "6000K",
                hasselblad_hncs = true
            ),
            deviceModel = "Find X7 Ultra",
            source = "omaster_cloud"
        ),
        Preset(
            id = "5",
            name = "城市街头 | 黑白纪实",
            coverPath = "city_street_01",
            sections = listOf(
                Section("黑白模式", "高对比度黑白"),
                Section("颗粒感", "胶片颗粒模拟")
            ),
            cameraParams = CameraParams(
                mode = "master",
                filter = "黑白",
                iso = 400,
                shutter = "1/1000",
                ev = "0",
                wb = "自动",
                hasselblad_hncs = false
            ),
            deviceModel = "Find X8",
            source = "omaster_cloud"
        ),
        Preset(
            id = "6",
            name = "春日樱花 | 粉调柔焦",
            coverPath = "sakura_spring_01",
            sections = listOf(
                Section("粉色优化", "樱花色彩还原"),
                Section("柔焦效果", "轻微虚化处理")
            ),
            cameraParams = CameraParams(
                mode = "master",
                filter = "人像",
                iso = 200,
                shutter = "1/320",
                ev = "+0.3",
                wb = "5800K",
                hasselblad_hncs = true
            ),
            deviceModel = "Reno 12",
            source = "omaster_cloud"
        ),
        Preset(
            id = "7",
            name = "哈苏自然色彩 | 人像写真",
            coverPath = "hasselblad_portrait_01",
            sections = listOf(
                Section("肤色优化", "自然肤色还原，红润度适中"),
                Section("背景虚化", "大光圈景深效果")
            ),
            cameraParams = CameraParams(
                mode = "portrait",
                filter = "人像",
                iso = 100,
                shutter = "1/500",
                ev = "0",
                wb = "5400K",
                hasselblad_hncs = true
            ),
            deviceModel = "Find X8 Ultra",
            source = "omaster_cloud"
        ),
        Preset(
            id = "8",
            name = "胶片模拟 | Kodak Portra 400",
            coverPath = "film_portra_400",
            sections = listOf(
                Section("胶片色调", "柔和过渡，低饱和度"),
                Section("颗粒感", "轻微胶片颗粒模拟")
            ),
            cameraParams = CameraParams(
                mode = "master",
                filter = "胶片",
                iso = 400,
                shutter = "1/250",
                ev = "+0.3",
                wb = "5200K",
                hasselblad_hncs = false
            ),
            deviceModel = "Find X7 Pro",
            source = "omaster_cloud"
        ),
        Preset(
            id = "9",
            name = "城市夜景 | 星空车流",
            coverPath = "city_night_traffic",
            sections = listOf(
                Section("长曝光", "15秒曝光捕捉车流轨迹"),
                Section("降噪处理", "多帧降噪优化")
            ),
            cameraParams = CameraParams(
                mode = "pro",
                filter = "夜景",
                iso = 100,
                shutter = "15s",
                ev = "-1.0",
                wb = "3800K",
                hasselblad_hncs = false
            ),
            deviceModel = "Find X8 Pro",
            source = "omaster_cloud"
        ),
        Preset(
            id = "10",
            name = "美食摄影 | 精致摆盘",
            coverPath = "food_photography",
            sections = listOf(
                Section("色彩鲜艳", "食物色彩饱和度提升"),
                Section("锐度优化", "细节纹理清晰")
            ),
            cameraParams = CameraParams(
                mode = "master",
                filter = "鲜艳",
                iso = 100,
                shutter = "1/200",
                ev = "+0.7",
                wb = "5000K",
                hasselblad_hncs = true
            ),
            deviceModel = "Reno 11 Pro",
            source = "omaster_cloud"
        ),
        Preset(
            id = "11",
            name = "风景摄影 | 山川湖海",
            coverPath = "landscape_mountains",
            sections = listOf(
                Section("广角优化", "大场景透视效果"),
                Section("动态范围", "HDR高动态范围")
            ),
            cameraParams = CameraParams(
                mode = "pro",
                filter = "风景",
                iso = 100,
                shutter = "1/1000",
                ev = "0",
                wb = "5600K",
                hasselblad_hncs = true
            ),
            deviceModel = "Find X8 Ultra",
            source = "omaster_cloud"
        ),
        Preset(
            id = "12",
            name = "复古胶片 | 80年代风格",
            coverPath = "retro_80s_style",
            sections = listOf(
                Section("色调偏移", "偏黄的复古色调"),
                Section("对比度", "高对比度，颗粒感强")
            ),
            cameraParams = CameraParams(
                mode = "master",
                filter = "复古",
                iso = 200,
                shutter = "1/500",
                ev = "0",
                wb = "4800K",
                hasselblad_hncs = false
            ),
            deviceModel = "Reno 10 Pro+",
            source = "omaster_cloud"
        ),
        Preset(
            id = "13",
            name = "街头纪实 | 人文摄影",
            coverPath = "street_documentary",
            sections = listOf(
                Section("快速抓拍", "高速快门捕捉瞬间"),
                Section("自然色彩", "真实记录场景")
            ),
            cameraParams = CameraParams(
                mode = "pro",
                filter = "自然",
                iso = 400,
                shutter = "1/1000",
                ev = "0",
                wb = "自动",
                hasselblad_hncs = false
            ),
            deviceModel = "Find X7",
            source = "omaster_cloud"
        ),
        Preset(
            id = "14",
            name = "阴天氛围 | 柔和光线",
            coverPath = "cloudy_day_soft",
            sections = listOf(
                Section("曝光补偿", "增加曝光补偿提亮"),
                Section("色温调整", "稍微偏暖调")
            ),
            cameraParams = CameraParams(
                mode = "master",
                filter = "柔和",
                iso = 200,
                shutter = "1/125",
                ev = "+1.0",
                wb = "6000K",
                hasselblad_hncs = false
            ),
            deviceModel = "Reno 12",
            source = "omaster_cloud"
        ),
        Preset(
            id = "15",
            name = "微距摄影 | 花卉细节",
            coverPath = "macro_flower_detail",
            sections = listOf(
                Section("微距模式", "近距离对焦"),
                Section("细节锐化", "纹理清晰呈现")
            ),
            cameraParams = CameraParams(
                mode = "macro",
                filter = "自然",
                iso = 100,
                shutter = "1/200",
                ev = "0",
                wb = "5200K",
                hasselblad_hncs = true
            ),
            deviceModel = "Find X8 Pro",
            source = "omaster_cloud"
        ),
        Preset(
            id = "16",
            name = "赛博朋克 | 霓虹未来",
            coverPath = "cyberpunk_neon",
            sections = listOf(
                Section("高饱和度", "鲜艳的霓虹色彩"),
                Section("对比度", "高对比度，暗部深邃")
            ),
            cameraParams = CameraParams(
                mode = "master",
                filter = "赛博",
                iso = 800,
                shutter = "1/60",
                ev = "-0.5",
                wb = "3600K",
                hasselblad_hncs = false
            ),
            deviceModel = "Find X8 Ultra",
            source = "omaster_cloud"
        ),
        Preset(
            id = "17",
            name = "秋日落叶 | 金黄色调",
            coverPath = "autumn_leaves_gold",
            sections = listOf(
                Section("暖色调", "强化黄色和橙色"),
                Section("对比度", "中等对比度，层次分明")
            ),
            cameraParams = CameraParams(
                mode = "master",
                filter = "暖调",
                iso = 100,
                shutter = "1/320",
                ev = "+0.3",
                wb = "6200K",
                hasselblad_hncs = true
            ),
            deviceModel = "Find X7 Pro",
            source = "omaster_cloud"
        ),
        Preset(
            id = "18",
            name = "极简主义 | 黑白简约",
            coverPath = "minimalist_bw",
            sections = listOf(
                Section("极简构图", "大面积留白"),
                Section("高对比度", "黑白分明")
            ),
            cameraParams = CameraParams(
                mode = "master",
                filter = "黑白",
                iso = 100,
                shutter = "1/500",
                ev = "0",
                wb = "自动",
                hasselblad_hncs = false
            ),
            deviceModel = "Reno 11",
            source = "omaster_cloud"
        ),
        Preset(
            id = "19",
            name = "夜景人像 | 都市光影",
            coverPath = "night_portrait_city",
            sections = listOf(
                Section("人像补光", "闪光灯柔和补光"),
                Section("背景虚化", "霓虹灯散景效果")
            ),
            cameraParams = CameraParams(
                mode = "portrait",
                filter = "夜景",
                iso = 400,
                shutter = "1/60",
                ev = "-0.3",
                wb = "4000K",
                hasselblad_hncs = false
            ),
            deviceModel = "Find X8 Pro",
            source = "omaster_cloud"
        ),
        Preset(
            id = "20",
            name = "富士胶片 | Classic Chrome",
            coverPath = "fujifilm_classic_chrome",
            sections = listOf(
                Section("复古色调", "低饱和度，柔和过渡"),
                Section("动态范围", "保留高光细节")
            ),
            cameraParams = CameraParams(
                mode = "master",
                filter = "胶片",
                iso = 200,
                shutter = "1/250",
                ev = "0",
                wb = "5200K",
                hasselblad_hncs = false
            ),
            deviceModel = "Find X7 Ultra",
            source = "omaster_cloud"
        ),
        Preset(
            id = "21",
            name = "冬日雪景 | 纯净白色",
            coverPath = "winter_snow_white",
            sections = listOf(
                Section("曝光补偿", "增加曝光防止欠曝"),
                Section("白平衡", "偏冷色调表现雪景")
            ),
            cameraParams = CameraParams(
                mode = "master",
                filter = "自然",
                iso = 100,
                shutter = "1/500",
                ev = "+1.3",
                wb = "4800K",
                hasselblad_hncs = true
            ),
            deviceModel = "Reno 12 Pro",
            source = "omaster_cloud"
        ),
        Preset(
            id = "22",
            name = "宠物摄影 | 萌宠瞬间",
            coverPath = "pet_photography_cute",
            sections = listOf(
                Section("快速对焦", "连续追焦模式"),
                Section("眼神光", "突出眼睛细节")
            ),
            cameraParams = CameraParams(
                mode = "portrait",
                filter = "自然",
                iso = 400,
                shutter = "1/1000",
                ev = "+0.3",
                wb = "5400K",
                hasselblad_hncs = false
            ),
            deviceModel = "Find X8",
            source = "omaster_cloud"
        ),
        Preset(
            id = "23",
            name = "产品摄影 | 商业质感",
            coverPath = "product_photography",
            sections = listOf(
                Section("光线均匀", "柔和布光效果"),
                Section("色彩准确", "产品色彩真实还原")
            ),
            cameraParams = CameraParams(
                mode = "pro",
                filter = "标准",
                iso = 100,
                shutter = "1/125",
                ev = "0",
                wb = "5200K",
                hasselblad_hncs = true
            ),
            deviceModel = "Find X8 Ultra",
            source = "omaster_cloud"
        ),
        Preset(
            id = "24",
            name = "星空银河 | 暗夜奇观",
            coverPath = "starry_night_milkyway",
            sections = listOf(
                Section("长曝光", "30秒曝光捕捉银河"),
                Section("高ISO", "ISO 3200感光度")
            ),
            cameraParams = CameraParams(
                mode = "pro",
                filter = "夜景",
                iso = 3200,
                shutter = "30s",
                ev = "0",
                wb = "3600K",
                hasselblad_hncs = false
            ),
            deviceModel = "Find X8 Ultra",
            source = "omaster_cloud"
        ),
        Preset(
            id = "25",
            name = "旅行街拍 | 异域风情",
            coverPath = "travel_street_shot",
            sections = listOf(
                Section("色彩丰富", "异域色彩表现"),
                Section("快速反应", "抓拍精彩瞬间")
            ),
            cameraParams = CameraParams(
                mode = "master",
                filter = "鲜艳",
                iso = 200,
                shutter = "1/500",
                ev = "0",
                wb = "自动",
                hasselblad_hncs = true
            ),
            deviceModel = "Reno 12",
            source = "omaster_cloud"
        )
    )

    val presets: Flow<List<Preset>> = preferencesDataStore.favoritePresets
        .map { favoriteIds ->
            samplePresets.map { preset ->
                preset.copy(isFavorite = favoriteIds.contains(preset.id))
            }
        }

    suspend fun toggleFavorite(presetId: String) {
        preferencesDataStore.toggleFavorite(presetId)
    }

    fun getPresetById(id: String): Preset? {
        return samplePresets.find { it.id == id }
    }

    suspend fun fetchPresetsFromNetwork(): List<Preset>? {
        return try {
            val response = presetApi.getPresets()
            if (response.isSuccessful) {
                Timber.d("成功从网络获取预设")
                response.body()
            } else {
                Timber.e("网络请求失败: ${response.code()}")
                null
            }
        } catch (e: Exception) {
            Timber.e(e, "获取预设时发生错误")
            null
        }
    }
}

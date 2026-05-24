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
    private val presetApi: PresetApi?
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
            presetApi?.let {
                val response = it.getPresets()
                if (response.isSuccessful) {
                    Timber.d("成功从网络获取预设")
                    response.body()
                } else {
                    Timber.e("网络请求失败: ${response.code()}")
                    null
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "获取预设时发生错误")
            null
        }
    }
}
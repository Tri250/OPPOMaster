package com.omaster.app.data

import android.content.Context
import com.omaster.app.model.CameraParams
import com.omaster.app.model.Preset
import com.omaster.app.model.Section
import com.omaster.app.network.PresetApi
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import timber.log.Timber
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PresetRepository @Inject constructor(
    private val preferencesDataStore: PreferencesDataStore,
    private val presetApi: PresetApi,
    @ApplicationContext private val context: Context
) {
    private val _customPresets = MutableStateFlow<List<Preset>>(emptyList())
    val customPresets: Flow<List<Preset>> = _customPresets

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
            source = "omaster_cloud",
            tags = listOf("风景", "哈苏", "暖色")
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
            source = "omaster_cloud",
            tags = listOf("夜景", "城市", "霓虹")
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
            source = "omaster_cloud",
            tags = listOf("风景", "自然", "绿色")
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
            source = "omaster_cloud",
            tags = listOf("风景", "日落", "海边")
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
            source = "omaster_cloud",
            tags = listOf("黑白", "街拍", "纪实")
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
            source = "omaster_cloud",
            tags = listOf("人像", "樱花", "春天")
        ),
        Preset(
            id = "7",
            name = "人像大师 | 柔肤磨皮",
            coverPath = "portrait_master_01",
            sections = listOf(
                Section("人像优化", "自然磨皮，保留细节"),
                Section("肤色调整", "温暖肤色，红润气色")
            ),
            cameraParams = CameraParams(
                mode = "portrait",
                filter = "人像",
                iso = 100,
                shutter = "1/200",
                ev = "+0.5",
                wb = "5200K",
                hasselblad_hncs = true
            ),
            deviceModel = "Find X8 Ultra",
            source = "omaster_cloud",
            tags = listOf("人像", "哈苏")
        ),
        Preset(
            id = "8",
            name = "美食诱惑 | 色彩饱和",
            coverPath = "food_delicious_01",
            sections = listOf(
                Section("色彩增强", "提高食物色彩饱和度"),
                Section("锐化", "增强食物质感表现")
            ),
            cameraParams = CameraParams(
                mode = "master",
                filter = "鲜艳",
                iso = 200,
                shutter = "1/100",
                ev = "0",
                wb = "5000K",
                hasselblad_hncs = false
            ),
            deviceModel = "Find X8",
            source = "omaster_cloud",
            tags = listOf("美食", "静物")
        ),
        Preset(
            id = "9",
            name = "星空银河 | 长曝光",
            coverPath = "starry_night_01",
            sections = listOf(
                Section("长曝光", "捕捉星空轨迹"),
                Section("降噪", "高ISO降噪处理")
            ),
            cameraParams = CameraParams(
                mode = "night",
                filter = "夜景",
                iso = 3200,
                shutter = "30",
                ev = "0",
                wb = "4000K",
                hasselblad_hncs = false
            ),
            deviceModel = "Find X7 Ultra",
            source = "omaster_cloud",
            tags = listOf("夜景", "星空", "长曝光")
        ),
        Preset(
            id = "10",
            name = "建筑美学 | 几何构图",
            coverPath = "architecture_01",
            sections = listOf(
                Section("透视修正", "保持建筑线条笔直"),
                Section("色彩调整", "冷色调增强现代感")
            ),
            cameraParams = CameraParams(
                mode = "master",
                filter = "标准",
                iso = 100,
                shutter = "1/250",
                ev = "-0.3",
                wb = "5000K",
                hasselblad_hncs = true
            ),
            deviceModel = "Find X8 Pro",
            source = "omaster_cloud",
            tags = listOf("建筑", "哈苏", "城市")
        )
    )

    val presets: Flow<List<Preset>> = combine(
        preferencesDataStore.favoritePresets,
        _customPresets
    ) { favoriteIds, customPresets ->
        val allPresets = samplePresets + customPresets
        allPresets.map { preset ->
            preset.copy(isFavorite = favoriteIds.contains(preset.id))
        }
    }

    suspend fun toggleFavorite(presetId: String) {
        preferencesDataStore.toggleFavorite(presetId)
    }

    fun getPresetById(id: String): Preset? {
        return (samplePresets + _customPresets.value).find { it.id == id }
    }

    suspend fun createCustomPreset(preset: Preset) {
        val newPreset = preset.copy(
            id = UUID.randomUUID().toString(),
            isCustom = true,
            createdAt = System.currentTimeMillis()
        )
        _customPresets.value = _customPresets.value + newPreset
        saveCustomPresets()
    }

    suspend fun updateCustomPreset(preset: Preset) {
        _customPresets.value = _customPresets.value.map { 
            if (it.id == preset.id) preset else it 
        }
        saveCustomPresets()
    }

    suspend fun deleteCustomPreset(presetId: String) {
        _customPresets.value = _customPresets.value.filterNot { it.id == presetId }
        saveCustomPresets()
    }

    fun getAllTags(): List<String> {
        val allPresets = samplePresets + _customPresets.value
        return allPresets.flatMap { it.tags }.distinct().sorted()
    }

    suspend fun incrementUsageCount(presetId: String) {
        val preset = getPresetById(presetId)
        preset?.let {
            if (it.isCustom) {
                val updated = it.copy(usageCount = it.usageCount + 1)
                updateCustomPreset(updated)
            }
        }
    }

    suspend fun exportPreset(preset: Preset): String {
        return Json.encodeToString(preset)
    }

    suspend fun importPreset(jsonString: String): Preset? {
        return try {
            val preset = Json.decodeFromString<Preset>(jsonString)
            val newPreset = preset.copy(
                id = UUID.randomUUID().toString(),
                isCustom = true,
                createdAt = System.currentTimeMillis(),
                isFavorite = false
            )
            createCustomPreset(newPreset)
            newPreset
        } catch (e: Exception) {
            Timber.e(e, "导入预设失败")
            null
        }
    }

    private suspend fun saveCustomPresets() {
        try {
            val file = File(context.filesDir, "custom_presets.json")
            val json = Json.encodeToString(_customPresets.value)
            file.writeText(json)
        } catch (e: Exception) {
            Timber.e(e, "保存自定义预设失败")
        }
    }

    suspend fun loadCustomPresets() {
        try {
            val file = File(context.filesDir, "custom_presets.json")
            if (file.exists()) {
                val json = file.readText()
                _customPresets.value = Json.decodeFromString(json)
            }
        } catch (e: Exception) {
            Timber.e(e, "加载自定义预设失败")
        }
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

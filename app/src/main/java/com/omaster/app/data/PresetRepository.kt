package com.omaster.app.data

import com.omaster.app.model.CameraParams
import com.omaster.app.model.Preset
import com.omaster.app.model.Section
import com.omaster.app.model.entities.PresetEntity
import com.omaster.app.network.PresetApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PresetRepository @Inject constructor(
    private val preferencesDataStore: PreferencesDataStore,
    private val presetApi: PresetApi,
    private val presetDao: PresetDao
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
                hasselblad_hncs = true,
                contrast = 1.1f,
                saturation = 1.0f,
                vignette = 0.1f,
                sceneTags = listOf("landscape", "portrait")
            ),
            deviceModel = "Find X8 Pro",
            source = "omaster_cloud",
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            usageCount = 0,
            rating = 4.5f,
            author = "OPPO"
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
                hasselblad_hncs = false,
                contrast = 1.3f,
                saturation = 1.2f,
                vignette = 0.3f,
                sceneTags = listOf("night", "city")
            ),
            deviceModel = "Find X8 Ultra",
            source = "omaster_cloud",
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            usageCount = 0,
            rating = 4.8f,
            author = "OPPO"
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
                hasselblad_hncs = true,
                contrast = 1.0f,
                saturation = 1.1f,
                vignette = 0.0f,
                sceneTags = listOf("landscape", "nature")
            ),
            deviceModel = "Reno 12 Pro",
            source = "omaster_cloud",
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            usageCount = 0,
            rating = 4.3f,
            author = "OPPO"
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
                hasselblad_hncs = true,
                contrast = 1.2f,
                saturation = 1.3f,
                vignette = 0.2f,
                sceneTags = listOf("sunset", "landscape")
            ),
            deviceModel = "Find X7 Ultra",
            source = "omaster_cloud",
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            usageCount = 0,
            rating = 4.9f,
            author = "OPPO"
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
                hasselblad_hncs = false,
                contrast = 1.4f,
                saturation = 0.0f,
                vignette = 0.4f,
                sceneTags = listOf("street", "blackwhite")
            ),
            deviceModel = "Find X8",
            source = "omaster_cloud",
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            usageCount = 0,
            rating = 4.6f,
            author = "OPPO"
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
                hasselblad_hncs = true,
                contrast = 0.9f,
                saturation = 1.2f,
                vignette = 0.15f,
                sceneTags = listOf("portrait", "flower")
            ),
            deviceModel = "Reno 12",
            source = "omaster_cloud",
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            usageCount = 0,
            rating = 4.7f,
            author = "OPPO"
        )
    )

    val presets: Flow<List<Preset>> = presetDao.getAllPresets()
        .map { entities ->
            entities.map { entity ->
                Preset(
                    id = entity.id,
                    name = entity.name,
                    coverPath = entity.coverPath,
                    cameraParams = entity.cameraParams,
                    deviceModel = entity.deviceModel,
                    source = entity.source,
                    isFavorite = entity.isFavorite,
                    createdAt = entity.createdAt,
                    updatedAt = entity.updatedAt,
                    usageCount = entity.usageCount,
                    rating = entity.rating,
                    author = entity.author
                )
            }.ifEmpty {
                samplePresets
            }
        }

    val favoritePresets: Flow<List<Preset>> = presetDao.getFavoritePresets()
        .map { entities ->
            entities.map { entity ->
                Preset(
                    id = entity.id,
                    name = entity.name,
                    coverPath = entity.coverPath,
                    cameraParams = entity.cameraParams,
                    deviceModel = entity.deviceModel,
                    source = entity.source,
                    isFavorite = entity.isFavorite,
                    createdAt = entity.createdAt,
                    updatedAt = entity.updatedAt,
                    usageCount = entity.usageCount,
                    rating = entity.rating,
                    author = entity.author
                )
            }
        }

    suspend fun initializeDatabase() = withContext(Dispatchers.IO) {
        samplePresets.forEach { preset ->
            val existing = presetDao.getPresetById(preset.id)
            if (existing == null) {
                presetDao.insertPreset(preset.toEntity())
            }
        }
    }

    suspend fun toggleFavorite(presetId: String) = withContext(Dispatchers.IO) {
        val preset = presetDao.getPresetById(presetId) ?: return@withContext
        presetDao.updatePreset(
            preset.copy(
                isFavorite = !preset.isFavorite,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    fun getAllPresets(): List<Preset> = samplePresets

    suspend fun getPresetById(id: String): Preset? = withContext(Dispatchers.IO) {
        val entity = presetDao.getPresetById(id)
        if (entity != null) {
            Preset(
                id = entity.id,
                name = entity.name,
                coverPath = entity.coverPath,
                cameraParams = entity.cameraParams,
                deviceModel = entity.deviceModel,
                source = entity.source,
                isFavorite = entity.isFavorite,
                createdAt = entity.createdAt,
                updatedAt = entity.updatedAt,
                usageCount = entity.usageCount,
                rating = entity.rating,
                author = entity.author
            )
        } else {
            samplePresets.find { it.id == id }
        }
    }

    suspend fun incrementUsageCount(presetId: String) = withContext(Dispatchers.IO) {
        presetDao.incrementUsageCount(presetId)
    }

    suspend fun insertPreset(preset: Preset) = withContext(Dispatchers.IO) {
        presetDao.insertPreset(preset.toEntity())
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

    private fun Preset.toEntity(): PresetEntity = PresetEntity(
        id = id,
        name = name,
        coverPath = coverPath,
        cameraParams = cameraParams,
        deviceModel = deviceModel,
        source = source,
        isFavorite = isFavorite,
        createdAt = createdAt,
        updatedAt = updatedAt,
        usageCount = usageCount,
        rating = rating,
        author = author
    )
}

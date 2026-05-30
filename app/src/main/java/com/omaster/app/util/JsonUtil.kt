package com.omaster.app.util

import android.content.Context
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.omaster.app.model.CameraParams
import com.omaster.app.model.Preset
import com.omaster.app.model.Section
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class JsonUtil @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val gson: Gson = GsonBuilder()
        .setLenient()
        .create()

    private val mutex = Mutex()
    private var cachedPresets: List<Preset>? = null
    private var localCacheFile: File? = null

    private val localCacheDir: File
        get() = File(context.filesDir, "presets_cache").also {
            if (!it.exists()) it.mkdirs()
        }

    private val localCache: File
        get() {
            if (localCacheFile == null) {
                localCacheFile = File(localCacheDir, "local_presets.json")
            }
            return localCacheFile!!
        }

    suspend fun loadPresets(): List<Preset> = withContext(Dispatchers.IO) {
        mutex.withLock {
            cachedPresets?.let { return@withContext it }
        }

        try {
            val presets = loadFromAssets()
            mutex.withLock {
                cachedPresets = presets
            }
            return@withContext presets
        } catch (e: Exception) {
            Timber.e(e, "Failed to load presets from assets")
            return@withContext loadFromLocalCache().ifEmpty {
                emptyList()
            }
        }
    }

    private fun loadFromAssets(): List<Preset> {
        return try {
            val json = context.assets.open("presets.json").bufferedReader().use { it.readText() }
            parsePresetsJson(json)
        } catch (e: IOException) {
            Timber.e(e, "Failed to read presets.json from assets")
            emptyList()
        }
    }

    private fun parsePresetsJson(json: String): List<Preset> {
        return try {
            val type = object : TypeToken<PresetListContainer>() {}.type
            val container: PresetListContainer = gson.fromJson(json, type)
            container.presets.map { it.toPreset() }
        } catch (e: Exception) {
            Timber.e(e, "Failed to parse presets JSON")
            emptyList()
        }
    }

    suspend fun saveToLocalCache(presets: List<Preset>) = withContext(Dispatchers.IO) {
        mutex.withLock {
            try {
                val container = PresetListContainer(
                    version = "2.0.0",
                    lastUpdated = java.text.SimpleDateFormat(
                        "yyyy-MM-dd",
                        java.util.Locale.getDefault()
                    ).format(java.util.Date()),
                    presets = presets.map { PresetDto.fromPreset(it) }
                )
                val json = gson.toJson(container)
                localCache.writeText(json)
                Timber.d("Presets saved to local cache")
            } catch (e: Exception) {
                Timber.e(e, "Failed to save presets to local cache")
            }
        }
    }

    private fun loadFromLocalCache(): List<Preset> {
        return try {
            if (!localCache.exists()) return emptyList()
            val json = localCache.readText()
            parsePresetsJson(json)
        } catch (e: Exception) {
            Timber.e(e, "Failed to load presets from local cache")
            emptyList()
        }
    }

    fun hasLocalCache(): Boolean = localCache.exists()

    fun getCacheVersion(): String? {
        return try {
            if (!localCache.exists()) return null
            val json = localCache.readText()
            val type = object : TypeToken<PresetListContainer>() {}.type
            val container: PresetListContainer = gson.fromJson(json, type)
            container.version
        } catch (e: Exception) {
            null
        }
    }

    fun clearCache() {
        mutex.withLock {
            cachedPresets = null
            localCache.delete()
        }
    }

    fun clearAllCaches() {
        clearCache()
        localCacheDir.deleteRecursively()
    }

    private data class PresetListContainer(
        val version: String,
        val lastUpdated: String,
        val presets: List<PresetDto>
    )

    private data class PresetDto(
        val id: String,
        val name: String,
        val deviceModel: String,
        val coverPath: String,
        val source: String,
        val cameraParams: CameraParamsDto?,
        val sections: List<SectionDto>?
    ) {
        fun toPreset(): Preset = Preset(
            id = id,
            name = name,
            coverPath = coverPath,
            sections = sections?.map { it.toSection() } ?: emptyList(),
            cameraParams = cameraParams?.toCameraParams(),
            deviceModel = deviceModel,
            source = source
        )

        companion object {
            fun fromPreset(preset: Preset): PresetDto = PresetDto(
                id = preset.id,
                name = preset.name,
                deviceModel = preset.deviceModel,
                coverPath = preset.coverPath,
                source = preset.source,
                cameraParams = preset.cameraParams?.let { CameraParamsDto.fromCameraParams(it) },
                sections = preset.sections.map { SectionDto.fromSection(it) }
            )
        }
    }

    private data class CameraParamsDto(
        val mode: String?,
        val filter: String?,
        val iso: Int?,
        val shutter: String?,
        val ev: String?,
        val wb: String?,
        val focal_length: String?,
        val aperture: String?,
        val hdr: Boolean?,
        val night_mode: Boolean?,
        val portrait_mode: Boolean?,
        val macro_mode: Boolean?,
        val sports_mode: Boolean?,
        val ai_optimization: Boolean?,
        val hasselblad_hncs: Boolean?,
        val hasselblad_natural_color: Boolean?,
        val hasselblad_master_style: String?,
        val color_profile: String?,
        val sharpness: Int?,
        val contrast: Int?,
        val saturation: Int?,
        val master_tonemap: Boolean?,
        val vignette: Float?,
        val softness: Int?,
        val film_simulation: String?,
        val exposure_compensation: Float?,
        val shutter_priority: Boolean?,
        val aperture_priority: Boolean?
    ) {
        fun toCameraParams(): CameraParams = CameraParams(
            mode = mode ?: "哈苏大师",
            filter = filter ?: "",
            iso = iso ?: 100,
            shutter = shutter ?: "1/200",
            ev = ev ?: "0",
            wb = wb ?: "5500K",
            focal_length = focal_length ?: "24mm",
            aperture = aperture ?: "f/1.8",
            hdr = hdr ?: false,
            night_mode = night_mode ?: false,
            portrait_mode = portrait_mode ?: false,
            macro_mode = macro_mode ?: false,
            sports_mode = sports_mode ?: false,
            ai_optimization = ai_optimization ?: true,
            hasselblad_hncs = hasselblad_hncs ?: true,
            hasselblad_natural_color = hasselblad_natural_color ?: true,
            hasselblad_master_style = hasselblad_master_style ?: "",
            color_profile = color_profile ?: "Natural",
            sharpness = sharpness ?: 50,
            contrast = contrast ?: 50,
            saturation = saturation ?: 50,
            master_tonemap = master_tonemap ?: true,
            vignette = vignette ?: 0.15f,
            softness = softness ?: 0,
            film_simulation = com.omaster.app.model.FilmSimulation.entries.find {
                it.name.equals(film_simulation, ignoreCase = true)
            } ?: com.omaster.app.model.FilmSimulation.NONE,
            exposure_compensation = exposure_compensation ?: 0.0f,
            shutter_priority = shutter_priority ?: false,
            aperture_priority = aperture_priority ?: false
        )

        companion object {
            fun fromCameraParams(params: CameraParams): CameraParamsDto = CameraParamsDto(
                mode = params.mode,
                filter = params.filter,
                iso = params.iso,
                shutter = params.shutter,
                ev = params.ev,
                wb = params.wb,
                focal_length = params.focal_length,
                aperture = params.aperture,
                hdr = params.hdr,
                night_mode = params.night_mode,
                portrait_mode = params.portrait_mode,
                macro_mode = params.macro_mode,
                sports_mode = params.sports_mode,
                ai_optimization = params.ai_optimization,
                hasselblad_hncs = params.hasselblad_hncs,
                hasselblad_natural_color = params.hasselblad_natural_color,
                hasselblad_master_style = params.hasselblad_master_style,
                color_profile = params.color_profile,
                sharpness = params.sharpness,
                contrast = params.contrast,
                saturation = params.saturation,
                master_tonemap = params.master_tonemap,
                vignette = params.vignette,
                softness = params.softness,
                film_simulation = params.film_simulation.name,
                exposure_compensation = params.exposure_compensation,
                shutter_priority = params.shutter_priority,
                aperture_priority = params.aperture_priority
            )
        }
    }

    private data class SectionDto(
        val title: String,
        val content: String
    ) {
        fun toSection(): Section = Section(title = title, content = content)

        companion object {
            fun fromSection(section: Section): SectionDto = SectionDto(
                title = section.title,
                content = section.content
            )
        }
    }
}

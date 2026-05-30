package com.omaster.app.model

import android.content.Context
import androidx.annotation.StringRes
import com.omaster.app.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PresetI18n @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val stringResources = mapOf(
        "app_name" to R.string.app_name,
        "home_title" to R.string.home_title,
        "settings_title" to R.string.settings_title,
        "scene_detection_title" to R.string.scene_detection_title,
        "detail_title" to R.string.detail_title,
        "search_hint" to R.string.search_hint,
        "filter_all" to R.string.filter_all,
        "filter_portrait" to R.string.filter_portrait,
        "filter_landscape" to R.string.filter_landscape,
        "filter_night" to R.string.filter_night,
        "filter_food" to R.string.filter_food,
        "filter_street" to R.string.filter_street,
        "apply_preset" to R.string.apply_preset,
        "add_to_favorites" to R.string.add_to_favorites,
        "remove_from_favorites" to R.string.remove_from_favorites,
        "no_presets" to R.string.no_presets,
        "no_search_results" to R.string.no_search_results,
        "loading" to R.string.loading,
        "error_loading_presets" to R.string.error_loading_presets,
        "retry" to R.string.retry,
        "camera_permission_required" to R.string.camera_permission_required,
        "share_preset" to R.string.share_preset,
        "preset_source_official" to R.string.preset_source_official,
        "preset_source_community" to R.string.preset_source_community,
        "scene_type_portrait" to R.string.scene_type_portrait,
        "scene_type_landscape" to R.string.scene_type_landscape,
        "scene_type_night" to R.string.scene_type_night,
        "scene_type_food" to R.string.scene_type_food,
        "scene_type_street" to R.string.scene_type_street,
        "scene_type_architecture" to R.string.scene_type_architecture,
        "scene_type_nature" to R.string.scene_type_nature,
        "scene_type_sunset" to R.string.scene_type_sunset,
        "scene_type_macro" to R.string.scene_type_macro,
        "scene_type_sports" to R.string.scene_type_sports,
        "scene_type_night_portrait" to R.string.scene_type_night_portrait,
        "scene_type_vintage" to R.string.scene_type_vintage,
        "scene_type_cinematic" to R.string.scene_type_cinematic,
        "scene_type_black_white" to R.string.scene_type_black_white,
        "scene_type_unknown" to R.string.scene_type_unknown,
        "scene_confidence" to R.string.scene_confidence,
        "ai_analyzing" to R.string.ai_analyzing,
        "ai_recommendation" to R.string.ai_recommendation,
        "ai_no_recommendation" to R.string.ai_no_recommendation
    )

    fun getString(key: String): String {
        return getString(key, context)
    }

    fun getString(key: String, vararg formatArgs: Any): String {
        val resId = stringResources[key] ?: return key
        return context.getString(resId, *formatArgs)
    }

    fun getCategoryDisplayName(category: PresetCategory): String {
        return context.getString(getCategoryStringRes(category))
    }

    fun getSceneTypeDisplayName(sceneType: SceneType): String {
        return context.getString(getSceneTypeStringRes(sceneType))
    }

    fun getSourceDisplayName(source: String): String {
        return when (source.lowercase()) {
            "official" -> getString("preset_source_official")
            "community" -> getString("preset_source_community")
            else -> source
        }
    }

    @StringRes
    private fun getCategoryStringRes(category: PresetCategory): Int {
        return when (category) {
            PresetCategory.PORTRAIT -> R.string.filter_portrait
            PresetCategory.LANDSCAPE -> R.string.filter_landscape
            PresetCategory.NIGHT -> R.string.filter_night
            PresetCategory.FOOD -> R.string.filter_food
            PresetCategory.STREET -> R.string.scene_type_street
            PresetCategory.ARCHITECTURE -> R.string.scene_type_architecture
            PresetCategory.NATURE -> R.string.scene_type_nature
            PresetCategory.SUNSET -> R.string.scene_type_sunset
            PresetCategory.MACRO -> R.string.scene_type_macro
            PresetCategory.SPORTS -> R.string.scene_type_sports
            PresetCategory.NIGHT_PORTRAIT -> R.string.scene_type_night_portrait
            PresetCategory.VINTAGE -> R.string.scene_type_vintage
            PresetCategory.CINEMATIC -> R.string.scene_type_cinematic
            PresetCategory.BLACK_WHITE -> R.string.scene_type_black_white
        }
    }

    @StringRes
    private fun getSceneTypeStringRes(sceneType: SceneType): Int {
        return when (sceneType) {
            SceneType.PORTRAIT -> R.string.scene_type_portrait
            SceneType.LANDSCAPE -> R.string.scene_type_landscape
            SceneType.NIGHT -> R.string.scene_type_night
            SceneType.FOOD -> R.string.scene_type_food
            SceneType.STREET -> R.string.scene_type_street
            SceneType.ARCHITECTURE -> R.string.scene_type_architecture
            SceneType.NATURE -> R.string.scene_type_nature
            SceneType.SUNSET -> R.string.scene_type_sunset
            SceneType.MACRO -> R.string.scene_type_macro
            SceneType.SPORTS -> R.string.scene_type_sports
            SceneType.NIGHT_PORTRAIT -> R.string.scene_type_night_portrait
            SceneType.VINTAGE -> R.string.scene_type_vintage
            SceneType.CINEMATIC -> R.string.scene_type_cinematic
            SceneType.BLACK_WHITE -> R.string.scene_type_black_white
            SceneType.UNKNOWN -> R.string.scene_type_unknown
            SceneType.BLACK -> R.string.scene_type_unknown
            SceneType.WHITE -> R.string.scene_type_unknown
            SceneType.BLURRY -> R.string.scene_type_unknown
        }
    }

    companion object {
        fun getPresetName(preset: Preset, i18n: PresetI18n? = null): String {
            return i18n?.getString(preset.name) ?: preset.name
        }

        fun getPresetDescription(preset: Preset): String {
            return preset.sections.firstOrNull()?.content ?: ""
        }

        fun getCategoryLabel(category: PresetCategory, i18n: PresetI18n?): String {
            return i18n?.getCategoryDisplayName(category) ?: category.displayName
        }
    }
}

package com.omaster.app.model

import kotlinx.serialization.Serializable

@Serializable
data class CloudPresetResponse(
    val version: Int,
    val name: String,
    val author: String,
    val build: Int,
    val presets: List<CloudPreset>
)

@Serializable
data class CloudPreset(
    val name: String,
    val coverPath: String,
    val galleryImages: List<String> = emptyList(),
    val author: String,
    val isNew: Boolean = false,
    val sections: List<CloudSection>,
    val tags: List<String> = emptyList(),
    val description: CloudDescription? = null,
    val id: String = ""
)

@Serializable
data class CloudSection(
    val title: String,
    val items: List<CloudParamItem>
)

@Serializable
data class CloudParamItem(
    val label: String,
    val value: String,
    val span: Int = 1
)

@Serializable
data class CloudDescription(
    val title: String,
    val content: String
)

fun CloudPreset.toPreset(): Preset {
    return Preset(
        id = if (id.isNotEmpty()) id else "${author}_${name.hashCode()}",
        name = name,
        coverPath = coverPath,
        sections = sections.map { section ->
            Section(
                title = section.title.replace("@string/", ""),
                content = section.items.joinToString("\n") { "${it.label}: ${it.value}" }
            )
        },
        cameraParams = parseCameraParams(sections),
        deviceModel = "Community",
        source = "omaster_community",
        isFavorite = false,
        createdAt = System.currentTimeMillis(),
        updatedAt = System.currentTimeMillis(),
        usageCount = 0,
        rating = 4.5f,
        author = author
    )
}

private fun parseCameraParams(sections: List<CloudSection>): CameraParams {
    val params = mutableMapOf<String, String>()
    
    sections.forEach { section ->
        section.items.forEach { item ->
            val label = item.label.replace("@string/param_", "")
            params[label] = item.value
        }
    }
    
    return CameraParams(
        mode = "master",
        filter = params["filter"] ?: "",
        iso = params["iso"]?.toIntOrNull() ?: 100,
        shutter = params["shutter"] ?: "1/125",
        ev = params["ev"] ?: "0",
        wb = params["wb"] ?: "5500K",
        hasselblad_hncs = false,
        contrast = parseFloatParam(params["contrast"]) ?: 1.0f,
        saturation = parseFloatParam(params["saturation"]) ?: 1.0f,
        sharpness = parseFloatParam(params["sharpness"]) ?: 1.0f,
        vignette = if (params["vignette"] == "开") 0.2f else 0.0f,
        videoLut = "",
        sceneTags = emptyList()
    )
}

private fun parseFloatParam(value: String?): Float? {
    if (value == null) return null
    
    val cleanValue = value.replace("+", "").replace("%", "")
    return try {
        cleanValue.toFloat() / 100.0f
    } catch (e: Exception) {
        null
    }
}

object CloudPresetConstants {
    const val OPPO_PRESETS_URL = "https://cdn.jsdelivr.net/gh/fengyec2/OMaster-Community@main/presets/v2/oppo.json"
    const val REALME_PRESETS_URL = "https://cdn.jsdelivr.net/gh/fengyec2/OMaster-Community@main/presets/v2/realme.json"
    const val CACHE_DURATION_MS = 24 * 60 * 60 * 1000L
}

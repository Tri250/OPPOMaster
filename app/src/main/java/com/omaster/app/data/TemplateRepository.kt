package com.omaster.app.data

import android.content.Context
import com.omaster.app.watermark.*
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import timber.log.Timber
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TemplateRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val templatesDir = File(context.filesDir, "templates")
    private val systemTemplates = mutableListOf<WatermarkTemplateData>()
    private val customTemplates = mutableListOf<WatermarkTemplateData>()

    private val _templates = MutableStateFlow<List<WatermarkTemplateData>>(emptyList())
    val templates: Flow<List<WatermarkTemplateData>> = _templates.asStateFlow()

    private val _systemTemplatesFlow = MutableStateFlow<List<WatermarkTemplateData>>(emptyList())
    val systemTemplatesFlow: Flow<List<WatermarkTemplateData>> = _systemTemplatesFlow.asStateFlow()

    private val _customTemplatesFlow = MutableStateFlow<List<WatermarkTemplateData>>(emptyList())
    val customTemplatesFlow: Flow<List<WatermarkTemplateData>> = _customTemplatesFlow.asStateFlow()

    init {
        if (!templatesDir.exists()) {
            templatesDir.mkdirs()
        }
        initializeSystemTemplates()
        loadCustomTemplates()
    }

    private fun initializeSystemTemplates() {
        systemTemplates.clear()
        systemTemplates.addAll(listOf(
            createSystemTemplate(
                id = "oppo_brand",
                name = "OPPO品牌",
                description = "OPPO官方水印模板",
                watermarks = listOf(
                    Watermark(
                        type = WatermarkType.TEXT,
                        text = "OPPO",
                        position = Offset(0.9f, 0.95f),
                        textConfig = TextWatermarkConfig(
                            fontSize = 32f,
                            fontColor = androidx.compose.ui.graphics.Color.White,
                            isBold = true
                        ),
                        opacity = 0.8f
                    )
                )
            ),
            createSystemTemplate(
                id = "hasselblad_brand",
                name = "哈苏大师",
                description = "哈苏专业水印",
                watermarks = listOf(
                    Watermark(
                        type = WatermarkType.TEXT,
                        text = "HASSELBLAD",
                        position = Offset(0.5f, 0.9f),
                        textConfig = TextWatermarkConfig(
                            fontSize = 28f,
                            fontColor = androidx.compose.ui.graphics.Color(0xFFC9A962),
                            isBold = true
                        ),
                        opacity = 0.9f
                    )
                )
            ),
            createSystemTemplate(
                id = "minimal_style",
                name = "简约风格",
                description = "简洁的文字水印",
                watermarks = listOf(
                    Watermark(
                        type = WatermarkType.TEXT,
                        text = "Shot on OPPO",
                        position = Offset(0.5f, 0.95f),
                        textConfig = TextWatermarkConfig(
                            fontSize = 18f,
                            fontColor = androidx.compose.ui.graphics.Color.White,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Normal
                        ),
                        opacity = 0.7f
                    )
                )
            ),
            createSystemTemplate(
                id = "timestamp_style",
                name = "时间戳",
                description = "显示拍摄时间",
                watermarks = listOf(
                    Watermark(
                        type = WatermarkType.TEXT,
                        text = "2026-01-01",
                        position = Offset(0.95f, 0.95f),
                        textConfig = TextWatermarkConfig(
                            fontSize = 16f,
                            fontColor = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.8f)
                        ),
                        opacity = 0.8f
                    )
                )
            ),
            createSystemTemplate(
                id = "location_style",
                name = "位置水印",
                description = "显示拍摄位置",
                watermarks = listOf(
                    Watermark(
                        type = WatermarkType.TEXT,
                        text = "杭州西湖",
                        position = Offset(0.95f, 0.9f),
                        textConfig = TextWatermarkConfig(
                            fontSize = 20f,
                            fontColor = androidx.compose.ui.graphics.Color.White,
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                        ),
                        opacity = 0.75f
                    )
                )
            ),
            createSystemTemplate(
                id = "camera_params",
                name = "相机参数",
                description = "显示完整相机参数",
                watermarks = listOf(
                    Watermark(
                        type = WatermarkType.TEXT,
                        text = "ISO 100 | f/1.7 | 1/1000s",
                        position = Offset(0.95f, 0.95f),
                        textConfig = TextWatermarkConfig(
                            fontSize = 14f,
                            fontColor = androidx.compose.ui.graphics.Color.White
                        ),
                        opacity = 0.85f
                    )
                )
            ),
            createSystemTemplate(
                id = "diy_template",
                name = "自定义水印",
                description = "可自定义文字和位置",
                watermarks = listOf(
                    Watermark(
                        type = WatermarkType.TEXT,
                        text = "自定义文字",
                        position = Offset(0.5f, 0.5f),
                        textConfig = TextWatermarkConfig(
                            fontSize = 24f,
                            fontColor = androidx.compose.ui.graphics.Color.White
                        ),
                        opacity = 1.0f
                    )
                )
            ),
            createSystemTemplate(
                id = "copyright_style",
                name = "版权保护",
                description = "显示版权信息",
                watermarks = listOf(
                    Watermark(
                        type = WatermarkType.TEXT,
                        text = "© 2026 All Rights Reserved",
                        position = Offset(0.5f, 0.02f),
                        textConfig = TextWatermarkConfig(
                            fontSize = 12f,
                            fontColor = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.6f)
                        ),
                        opacity = 0.6f
                    )
                )
            )
        ))

        _systemTemplatesFlow.value = systemTemplates.toList()
        updateCombinedTemplates()
    }

    private fun createSystemTemplate(
        id: String,
        name: String,
        description: String,
        watermarks: List<Watermark>
    ): WatermarkTemplateData {
        return WatermarkTemplateData(
            id = id,
            name = name,
            description = description,
            watermarks = watermarks,
            isSystem = true,
            isCustom = false
        )
    }

    suspend fun loadCustomTemplates() = withContext(Dispatchers.IO) {
        customTemplates.clear()

        templatesDir.listFiles()?.filter { it.extension == "json" }?.forEach { file ->
            try {
                val json = file.readText()
                val template = parseTemplateFromJson(json)
                customTemplates.add(template)
            } catch (e: Exception) {
                Timber.e(e, "Failed to load template from: ${file.absolutePath}")
            }
        }

        _customTemplatesFlow.value = customTemplates.toList()
        updateCombinedTemplates()
    }

    suspend fun saveTemplate(template: WatermarkTemplateData): Result<WatermarkTemplateData> = withContext(Dispatchers.IO) {
        try {
            val customTemplate = template.copy(
                isCustom = true,
                isSystem = false
            )

            val file = File(templatesDir, "${customTemplate.id}.json")
            val json = templateToJson(customTemplate)
            file.writeText(json)

            val existingIndex = customTemplates.indexOfFirst { it.id == customTemplate.id }
            if (existingIndex >= 0) {
                customTemplates[existingIndex] = customTemplate
            } else {
                customTemplates.add(customTemplate)
            }

            _customTemplatesFlow.value = customTemplates.toList()
            updateCombinedTemplates()

            Result.success(customTemplate)
        } catch (e: Exception) {
            Timber.e(e, "Failed to save template")
            Result.failure(e)
        }
    }

    suspend fun deleteTemplate(templateId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val file = File(templatesDir, "$templateId.json")
            if (file.exists()) {
                file.delete()
            }

            customTemplates.removeAll { it.id == templateId }
            _customTemplatesFlow.value = customTemplates.toList()
            updateCombinedTemplates()

            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to delete template")
            Result.failure(e)
        }
    }

    suspend fun updateTemplate(template: WatermarkTemplateData): Result<WatermarkTemplateData> = withContext(Dispatchers.IO) {
        if (template.isSystem) {
            return@withContext Result.failure(IllegalArgumentException("Cannot update system templates"))
        }
        saveTemplate(template)
    }

    fun getTemplateById(id: String): WatermarkTemplateData? {
        return (systemTemplates + customTemplates).find { it.id == id }
    }

    fun searchTemplates(query: String): List<WatermarkTemplateData> {
        val lowerQuery = query.lowercase()
        return (systemTemplates + customTemplates).filter {
            it.name.lowercase().contains(lowerQuery) ||
            it.description.lowercase().contains(lowerQuery)
        }
    }

    private fun updateCombinedTemplates() {
        _templates.value = systemTemplates + customTemplates
    }

    private fun parseTemplateFromJson(json: String): WatermarkTemplateData {
        val jsonObject = JSONObject(json)

        val watermarksArray = jsonObject.getJSONArray("watermarks")
        val watermarks = mutableListOf<Watermark>()

        for (i in 0 until watermarksArray.length()) {
            val wmObject = watermarksArray.getJSONObject(i)
            watermarks.add(parseWatermarkFromJson(wmObject))
        }

        return WatermarkTemplateData(
            id = jsonObject.getString("id"),
            name = jsonObject.getString("name"),
            description = jsonObject.optString("description", ""),
            watermarks = watermarks,
            isSystem = false,
            isCustom = true,
            createdAt = jsonObject.optLong("createdAt", System.currentTimeMillis())
        )
    }

    private fun parseWatermarkFromJson(json: JSONObject): Watermark {
        val type = WatermarkType.valueOf(json.optString("type", "TEXT"))

        return Watermark(
            id = json.optString("id", ""),
            type = type,
            text = json.optString("text", ""),
            position = Offset(
                json.optDouble("positionX", 0.5).toFloat(),
                json.optDouble("positionY", 0.5).toFloat()
            ),
            size = androidx.compose.ui.geometry.Size(
                json.optDouble("sizeWidth", 100.0).toFloat(),
                json.optDouble("sizeHeight", 100.0).toFloat()
            ),
            rotation = json.optDouble("rotation", 0.0).toFloat(),
            opacity = json.optDouble("opacity", 1.0).toFloat(),
            zIndex = json.optInt("zIndex", 0)
        )
    }

    private fun templateToJson(template: WatermarkTemplateData): String {
        val jsonObject = JSONObject()

        jsonObject.put("id", template.id)
        jsonObject.put("name", template.name)
        jsonObject.put("description", template.description)
        jsonObject.put("createdAt", template.createdAt)
        jsonObject.put("isSystem", template.isSystem)
        jsonObject.put("isCustom", template.isCustom)

        val watermarksArray = JSONArray()
        template.watermarks.forEach { watermark ->
            val wmObject = JSONObject()
            wmObject.put("id", watermark.id)
            wmObject.put("type", watermark.type.name)
            wmObject.put("text", watermark.text)
            wmObject.put("positionX", watermark.position.x)
            wmObject.put("positionY", watermark.position.y)
            wmObject.put("sizeWidth", watermark.size.width)
            wmObject.put("sizeHeight", watermark.size.height)
            wmObject.put("rotation", watermark.rotation)
            wmObject.put("opacity", watermark.opacity)
            wmObject.put("zIndex", watermark.zIndex)
            watermarksArray.put(wmObject)
        }

        jsonObject.put("watermarks", watermarksArray)

        return jsonObject.toString(2)
    }

    suspend fun exportTemplate(templateId: String, exportPath: File): Result<File> = withContext(Dispatchers.IO) {
        try {
            val template = getTemplateById(templateId)
                ?: return@withContext Result.failure(IllegalArgumentException("Template not found"))

            val json = templateToJson(template)
            exportPath.writeText(json)

            Result.success(exportPath)
        } catch (e: Exception) {
            Timber.e(e, "Failed to export template")
            Result.failure(e)
        }
    }

    suspend fun importTemplate(importFile: File): Result<WatermarkTemplateData> = withContext(Dispatchers.IO) {
        try {
            val json = importFile.readText()
            var template = parseTemplateFromJson(json)

            val newId = "custom_${System.currentTimeMillis()}"
            template = template.copy(id = newId)

            val result = saveTemplate(template)
            if (result.isSuccess) {
                Result.success(result.getOrThrow())
            } else {
                Result.failure(result.exceptionOrNull() ?: Exception("Import failed"))
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to import template")
            Result.failure(e)
        }
    }
}

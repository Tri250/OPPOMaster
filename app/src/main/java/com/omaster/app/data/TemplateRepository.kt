package com.omaster.app.data

import android.content.Context
import androidx.compose.ui.geometry.Offset
import com.omaster.app.watermark.*
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import timber.log.Timber
import java.io.Closeable
import java.io.File
import java.util.concurrent.CopyOnWriteArrayList
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TemplateRepository @Inject constructor(
    @ApplicationContext private val context: Context
) : Closeable {
    private val templatesDir = File(context.filesDir, "templates")
    // 使用 CopyOnWriteArrayList 保证线程安全
    private val systemTemplates = CopyOnWriteArrayList<WatermarkTemplateData>()
    private val customTemplates = CopyOnWriteArrayList<WatermarkTemplateData>()

    private val _templates = MutableStateFlow<List<WatermarkTemplateData>>(emptyList())
    val templates: StateFlow<List<WatermarkTemplateData>> = _templates.asStateFlow()

    private val _systemTemplatesFlow = MutableStateFlow<List<WatermarkTemplateData>>(emptyList())
    val systemTemplatesFlow: StateFlow<List<WatermarkTemplateData>> = _systemTemplatesFlow.asStateFlow()

    private val _customTemplatesFlow = MutableStateFlow<List<WatermarkTemplateData>>(emptyList())
    val customTemplatesFlow: StateFlow<List<WatermarkTemplateData>> = _customTemplatesFlow.asStateFlow()

    // 使用 SupervisorJob 避免协程取消影响整个作用域
    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    init {
        if (!templatesDir.exists()) {
            templatesDir.mkdirs()
        }
        // 移除 init 中的 suspend 调用，改为异步执行
        initializeSystemTemplates()
        // 异步加载自定义模板
        repositoryScope.launch {
            loadCustomTemplatesAsync()
        }
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

    /**
     * 异步加载自定义模板
     */
    private suspend fun loadCustomTemplatesAsync() = withContext(Dispatchers.IO) {
        customTemplates.clear()

        // 添加空检查，避免 NullPointerException
        val files = templatesDir.listFiles()
        if (files != null) {
            files.filter { it.extension == "json" }.forEach { file ->
                try {
                    val json = file.readText()
                    val template = parseTemplateFromJson(json)
                    customTemplates.add(template)
                } catch (e: Exception) {
                    Timber.e(e, "Failed to load template from: ${file.absolutePath}")
                }
            }
        }

        _customTemplatesFlow.value = customTemplates.toList()
        updateCombinedTemplates()
    }

    suspend fun loadCustomTemplates() = withContext(Dispatchers.IO) {
        loadCustomTemplatesAsync()
    }

    suspend fun saveTemplate(template: WatermarkTemplateData): Result<WatermarkTemplateData> = withContext(Dispatchers.IO) {
        try {
            // 安全检查: 验证模板ID，防止路径遍历攻击
            val sanitizedId = sanitizeTemplateId(template.id)
            if (sanitizedId == null) {
                return Result.failure(IllegalArgumentException("Invalid template ID"))
            }
            
            val customTemplate = template.copy(
                id = sanitizedId,
                isCustom = true,
                isSystem = false
            )

            val file = File(templatesDir, "${customTemplate.id}.json")
            // 再次验证文件路径是否在预期目录内
            if (!isPathInAllowedDirectory(file, templatesDir)) {
                Timber.w("Path traversal attempt detected for template: ${template.id}")
                return Result.failure(IllegalArgumentException("Invalid template path"))
            }
            
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
            // 安全检查: 验证模板ID，防止路径遍历攻击
            val sanitizedId = sanitizeTemplateId(templateId)
            if (sanitizedId == null) {
                return Result.failure(IllegalArgumentException("Invalid template ID"))
            }
            
            val file = File(templatesDir, "$sanitizedId.json")
            // 再次验证文件路径是否在预期目录内
            if (!isPathInAllowedDirectory(file, templatesDir)) {
                Timber.w("Path traversal attempt detected for template: $templateId")
                return Result.failure(IllegalArgumentException("Invalid template path"))
            }
            
            if (file.exists()) {
                file.delete()
            }

            customTemplates.removeAll { it.id == sanitizedId }
            _customTemplatesFlow.value = customTemplates.toList()
            updateCombinedTemplates()

            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to delete template")
            Result.failure(e)
        }
    }
    
    /**
     * 验证并清理模板ID，防止路径遍历攻击
     * - 只允许字母、数字、下划线和连字符
     * - 限制长度
     * - 返回安全ID或null（如果无效）
     */
    private fun sanitizeTemplateId(id: String?): String? {
        if (id == null || id.isEmpty()) return null
        
        // 限制最大长度
        val maxLength = 64
        if (id.length > maxLength) {
            Timber.w("Template ID too long, truncating: $id")
            return null
        }
        
        // 只允许安全字符：字母、数字、下划线、连字符
        val sanitized = id.filter { char ->
            char.isLetterOrDigit() || char == '_' || char == '-'
        }
        
        // 检查是否包含路径遍历字符
        if (sanitized.contains("..") || sanitized.contains("/") || sanitized.contains("\\")) {
            Timber.w("Path traversal attempt detected in template ID: $id")
            return null
        }
        
        return sanitized.ifEmpty { null }
    }
    
    /**
     * 验证文件路径是否在允许的目录内
     * 防止路径遍历攻击
     */
    private fun isPathInAllowedDirectory(file: File, allowedDir: File): Boolean {
        try {
            val canonicalPath = file.canonicalPath
            val canonicalAllowedDir = allowedDir.canonicalPath
            return canonicalPath.startsWith(canonicalAllowedDir)
        } catch (e: Exception) {
            Timber.e(e, "Failed to verify file path")
            return false
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
        // 安全检查: 限制JSON字符串长度
        val maxJsonLength = 1024 * 1024 // 1MB
        if (json.length > maxJsonLength) {
            throw IllegalArgumentException("JSON data too large")
        }
        
        val jsonObject = JSONObject(json)

        // 安全检查: 验证必需字段是否存在且类型正确
        if (!jsonObject.has("id") || !jsonObject.has("name")) {
            throw IllegalArgumentException("Missing required fields: id or name")
        }
        
        // 验证并清理ID和名称
        val id = jsonObject.getString("id")
        val sanitizedId = sanitizeTemplateId(id) ?: throw IllegalArgumentException("Invalid template ID in JSON")
        
        val name = jsonObject.getString("name")
        if (name.isEmpty() || name.length > 100) {
            throw IllegalArgumentException("Invalid template name length")
        }

        val watermarksArray = jsonObject.optJSONArray("watermarks")
        val watermarks = mutableListOf<Watermark>()
        
        // 安全检查: 限制水印数量
        val maxWatermarks = 50
        if (watermarksArray != null && watermarksArray.length() > maxWatermarks) {
            throw IllegalArgumentException("Too many watermarks, max is $maxWatermarks")
        }

        if (watermarksArray != null) {
            for (i in 0 until watermarksArray.length()) {
                val wmObject = watermarksArray.getJSONObject(i)
                watermarks.add(parseWatermarkFromJson(wmObject))
            }
        }

        return WatermarkTemplateData(
            id = sanitizedId,
            name = name,
            description = jsonObject.optString("description", "").take(500), // 限制描述长度
            watermarks = watermarks,
            isSystem = false,
            isCustom = true,
            createdAt = jsonObject.optLong("createdAt", System.currentTimeMillis())
        )
    }

    private fun parseWatermarkFromJson(json: JSONObject): Watermark {
        // 安全检查: 验证水印类型
        val typeString = json.optString("type", "TEXT")
        val type = try {
            WatermarkType.valueOf(typeString)
        } catch (e: IllegalArgumentException) {
            Timber.w("Invalid watermark type: $typeString, using TEXT as default")
            WatermarkType.TEXT
        }
        
        // 安全检查: 验证并限制数值范围
        val positionX = json.optDouble("positionX", 0.5).toFloat().coerceIn(0f, 1f)
        val positionY = json.optDouble("positionY", 0.5).toFloat().coerceIn(0f, 1f)
        val opacity = json.optDouble("opacity", 1.0).toFloat().coerceIn(0f, 1f)
        val rotation = json.optDouble("rotation", 0.0).toFloat().coerceIn(-360f, 360f)
        val sizeWidth = json.optDouble("sizeWidth", 100.0).toFloat().coerceIn(1f, 10000f)
        val sizeHeight = json.optDouble("sizeHeight", 100.0).toFloat().coerceIn(1f, 10000f)
        val zIndex = json.optInt("zIndex", 0).coerceIn(-100, 100)
        
        // 安全检查: 限制文本长度
        val text = json.optString("text", "").take(200)

        return Watermark(
            id = json.optString("id", "").take(64),
            type = type,
            text = text,
            position = Offset(positionX, positionY),
            size = androidx.compose.ui.geometry.Size(sizeWidth, sizeHeight),
            rotation = rotation,
            opacity = opacity,
            zIndex = zIndex
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
            // 安全检查: 验证导入文件路径
            if (!importFile.exists()) {
                return Result.failure(IllegalArgumentException("Import file does not exist"))
            }
            
            // 限制文件大小，防止内存攻击
            val maxFileSize = 1024 * 1024 // 1MB
            if (importFile.length() > maxFileSize) {
                Timber.w("Import file too large: ${importFile.length()} bytes")
                return Result.failure(IllegalArgumentException("File too large, max size is 1MB"))
            }
            
            val json = importFile.readText()
            var template = parseTemplateFromJson(json)

            // 生成新的安全ID
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

    override fun close() {
        // 清理资源，取消所有协程
        repositoryScope.coroutineContext.cancel()
        Timber.d("TemplateRepository closed, resources cleaned up")
    }
}
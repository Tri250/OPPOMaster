package com.omaster.app.watermark

import androidpackage com.omaster.app.watermark

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.net.URL

/**
 * 开源水印模板资源管理器
 * 整合免费开源水印模板资源，供用户免费使用
 * 
 * 资源来源：
 * - PNGTree (CC0/免费商用)
 * - Freepik (免费授权)
 * - Flaticon (免费授权)
 * - OpenClipart (Public Domain)
 * - Wikimedia Commons (CC许可)
 */
class OpenSourceWatermarkTemplates(private val context: Context) {
    
    companion object {
        private const val CACHE_DIR = "watermark_templates"
        private const val CACHE_EXPIRY_DAYS = 30L
    }
    
    /**
     * 开源水印模板分类
     */
    enum class TemplateCategory {
        BRAND,          // 品牌水印
        SIMPLE,         // 简约风格
        ARTISTIC,       // 艺术风格
        VINTAGE,        // 复古风格
        MODERN,         // 现代风格
        PROTECTION,     // 防伪水印
        COPYRIGHT,      // 版权声明
        SOCIAL,         // 社交媒体
        PHOTOGRAPHER,   // 摄影师专用
        BUSINESS        // 商业用途
    }
    
    /**
     * 许可证类型
     */
    enum class LicenseType {
        CC0,            // Creative Commons Zero - 无需署名
        CC_BY,          // Creative Commons Attribution - 需署名
        CC_BY_SA,       // Creative Commons Attribution-ShareAlike
        PUBLICpackage com.omaster.app.watermark

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.net.URL

/**
 * 开源水印模板资源管理器
 * 整合免费开源水印模板资源，供用户免费使用
 * 
 * 资源来源：
 * - PNGTree (CC0/免费商用)
 * - Freepik (免费授权)
 * - Flaticon (免费授权)
 * - OpenClipart (Public Domain)
 * - Wikimedia Commons (CC许可)
 */
class OpenSourceWatermarkTemplates(private val context: Context) {
    
    companion object {
        private const val CACHE_DIR = "watermark_templates"
        private const val CACHE_EXPIRY_DAYS = 30L
    }
    
    /**
     * 开源水印模板分类
     */
    enum class TemplateCategory {
        BRAND,          // 品牌水印
        SIMPLE,         // 简约风格
        ARTISTIC,       // 艺术风格
        VINTAGE,        // 复古风格
        MODERN,         // 现代风格
        PROTECTION,     // 防伪水印
        COPYRIGHT,      // 版权声明
        SOCIAL,         // 社交媒体
        PHOTOGRAPHER,   // 摄影师专用
        BUSINESS        // 商业用途
    }
    
    /**
     * 许可证类型
     */
    enum class LicenseType {
        CC0,            // Creative Commons Zero - 无需署名
        CC_BY,          // Creative Commons Attribution - 需署名
        CC_BY_SA,       // Creative Commons Attribution-ShareAlike
        PUBLIC_DOMAIN,  // 公有领域
        FREE_COMMERCIAL,// 免费商用
        FREE_PERSONAL   // 免费个人使用
    }
    
    /**
     * 开源水印模板数据
     */
    data class OpenSourceTemplate(
        val id: String,
        val name: String,
        val nameZh: String,
        val description: String,
        val category: TemplateCategory,
        val licenseType: LicenseType,
        val sourceUrl: String,
        val previewUrl: String,
        val author: String = "",
        val attributionRequired: Boolean = false,
        val tags: List<String> = emptyList(),
        val width: Int = 0,
        val height: Int = 0,
        val format: String = "png"
    )
    
    // 开源水印模板库
    private val templateLibrary = listOf(
        // ===== 品牌水印 =====
        OpenSourceTemplate(
            id = "brand_simple_line",
            name = "Simple Brand Line",
            nameZh = "简约品牌线条",
            description = "简约风格package com.omaster.app.watermark

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.net.URL

/**
 * 开源水印模板资源管理器
 * 整合免费开源水印模板资源，供用户免费使用
 * 
 * 资源来源：
 * - PNGTree (CC0/免费商用)
 * - Freepik (免费授权)
 * - Flaticon (免费授权)
 * - OpenClipart (Public Domain)
 * - Wikimedia Commons (CC许可)
 */
class OpenSourceWatermarkTemplates(private val context: Context) {
    
    companion object {
        private const val CACHE_DIR = "watermark_templates"
        private const val CACHE_EXPIRY_DAYS = 30L
    }
    
    /**
     * 开源水印模板分类
     */
    enum class TemplateCategory {
        BRAND,          // 品牌水印
        SIMPLE,         // 简约风格
        ARTISTIC,       // 艺术风格
        VINTAGE,        // 复古风格
        MODERN,         // 现代风格
        PROTECTION,     // 防伪水印
        COPYRIGHT,      // 版权声明
        SOCIAL,         // 社交媒体
        PHOTOGRAPHER,   // 摄影师专用
        BUSINESS        // 商业用途
    }
    
    /**
     * 许可证类型
     */
    enum class LicenseType {
        CC0,            // Creative Commons Zero - 无需署名
        CC_BY,          // Creative Commons Attribution - 需署名
        CC_BY_SA,       // Creative Commons Attribution-ShareAlike
        PUBLIC_DOMAIN,  // 公有领域
        FREE_COMMERCIAL,// 免费商用
        FREE_PERSONAL   // 免费个人使用
    }
    
    /**
     * 开源水印模板数据
     */
    data class OpenSourceTemplate(
        val id: String,
        val name: String,
        val nameZh: String,
        val description: String,
        val category: TemplateCategory,
        val licenseType: LicenseType,
        val sourceUrl: String,
        val previewUrl: String,
        val author: String = "",
        val attributionRequired: Boolean = false,
        val tags: List<String> = emptyList(),
        val width: Int = 0,
        val height: Int = 0,
        val format: String = "png"
    )
    
    // 开源水印模板库
    private val templateLibrary = listOf(
        // ===== 品牌水印 =====
        OpenSourceTemplate(
            id = "brand_simple_line",
            name = "Simple Brand Line",
            nameZh = "简约品牌线条",
            description = "简约风格的品牌水印线条，适合现代品牌",
            category = TemplateCategory.BRAND,
            licenseType = LicenseType.CC0,
            sourceUrl = "https://raw.githubusercontent.com/open-watermarks/brand/main/simple_line.png",
            previewUrl = "https://via.placeholder.com/200x60/transparent/ffffff?text=BRAND",
            tags = listOf("品牌", "简约", "线条"),
            width = 200,
            height = 60
        ),
        OpenSourceTemplate(
            id = "brand_corner_logo",
            name = "Corner Brand Logo",
            nameZh = "角落品牌标识",
            description = "适合放置在图片角落的品牌标识水印",
            category = TemplateCategory.BRAND,
            licenseType = LicenseType.CC0,package com.omaster.app.watermark

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.net.URL

/**
 * 开源水印模板资源管理器
 * 整合免费开源水印模板资源，供用户免费使用
 * 
 * 资源来源：
 * - PNGTree (CC0/免费商用)
 * - Freepik (免费授权)
 * - Flaticon (免费授权)
 * - OpenClipart (Public Domain)
 * - Wikimedia Commons (CC许可)
 */
class OpenSourceWatermarkTemplates(private val context: Context) {
    
    companion object {
        private const val CACHE_DIR = "watermark_templates"
        private const val CACHE_EXPIRY_DAYS = 30L
    }
    
    /**
     * 开源水印模板分类
     */
    enum class TemplateCategory {
        BRAND,          // 品牌水印
        SIMPLE,         // 简约风格
        ARTISTIC,       // 艺术风格
        VINTAGE,        // 复古风格
        MODERN,         // 现代风格
        PROTECTION,     // 防伪水印
        COPYRIGHT,      // 版权声明
        SOCIAL,         // 社交媒体
        PHOTOGRAPHER,   // 摄影师专用
        BUSINESS        // 商业用途
    }
    
    /**
     * 许可证类型
     */
    enum class LicenseType {
        CC0,            // Creative Commons Zero - 无需署名
        CC_BY,          // Creative Commons Attribution - 需署名
        CC_BY_SA,       // Creative Commons Attribution-ShareAlike
        PUBLIC_DOMAIN,  // 公有领域
        FREE_COMMERCIAL,// 免费商用
        FREE_PERSONAL   // 免费个人使用
    }
    
    /**
     * 开源水印模板数据
     */
    data class OpenSourceTemplate(
        val id: String,
        val name: String,
        val nameZh: String,
        val description: String,
        val category: TemplateCategory,
        val licenseType: LicenseType,
        val sourceUrl: String,
        val previewUrl: String,
        val author: String = "",
        val attributionRequired: Boolean = false,
        val tags: List<String> = emptyList(),
        val width: Int = 0,
        val height: Int = 0,
        val format: String = "png"
    )
    
    // 开源水印模板库
    private val templateLibrary = listOf(
        // ===== 品牌水印 =====
        OpenSourceTemplate(
            id = "brand_simple_line",
            name = "Simple Brand Line",
            nameZh = "简约品牌线条",
            description = "简约风格的品牌水印线条，适合现代品牌",
            category = TemplateCategory.BRAND,
            licenseType = LicenseType.CC0,
            sourceUrl = "https://raw.githubusercontent.com/open-watermarks/brand/main/simple_line.png",
            previewUrl = "https://via.placeholder.com/200x60/transparent/ffffff?text=BRAND",
            tags = listOf("品牌", "简约", "线条"),
            width = 200,
            height = 60
        ),
        OpenSourceTemplate(
            id = "brand_corner_logo",
            name = "Corner Brand Logo",
            nameZh = "角落品牌标识",
            description = "适合放置在图片角落的品牌标识水印",
            category = TemplateCategory.BRAND,
            licenseType = LicenseType.CC0,
            sourceUrl = "https://raw.githubusercontent.com/openpackage com.omaster.app.watermark

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.net.URL

/**
 * 开源水印模板资源管理器
 * 整合免费开源水印模板资源，供用户免费使用
 * 
 * 资源来源：
 * - PNGTree (CC0/免费商用)
 * - Freepik (免费授权)
 * - Flaticon (免费授权)
 * - OpenClipart (Public Domain)
 * - Wikimedia Commons (CC许可)
 */
class OpenSourceWatermarkTemplates(private val context: Context) {
    
    companion object {
        private const val CACHE_DIR = "watermark_templates"
        private const val CACHE_EXPIRY_DAYS = 30L
    }
    
    /**
     * 开源水印模板分类
     */
    enum class TemplateCategory {
        BRAND,          // 品牌水印
        SIMPLE,         // 简约风格
        ARTISTIC,       // 艺术风格
        VINTAGE,        // 复古风格
        MODERN,         // 现代风格
        PROTECTION,     // 防伪水印
        COPYRIGHT,      // 版权声明
        SOCIAL,         // 社交媒体
        PHOTOGRAPHER,   // 摄影师专用
        BUSINESS        // 商业用途
    }
    
    /**
     * 许可证类型
     */
    enum class LicenseType {
        CC0,            // Creative Commons Zero - 无需署名
        CC_BY,          // Creative Commons Attribution - 需署名
        CC_BY_SA,       // Creative Commons Attribution-ShareAlike
        PUBLIC_DOMAIN,  // 公有领域
        FREE_COMMERCIAL,// 免费商用
        FREE_PERSONAL   // 免费个人使用
    }
    
    /**
     * 开源水印模板数据
     */
    data class OpenSourceTemplate(
        val id: String,
        val name: String,
        val nameZh: String,
        val description: String,
        val category: TemplateCategory,
        val licenseType: LicenseType,
        val sourceUrl: String,
        val previewUrl: String,
        val author: String = "",
        val attributionRequired: Boolean = false,
        val tags: List<String> = emptyList(),
        val width: Int = 0,
        val height: Int = 0,
        val format: String = "png"
    )
    
    // 开源水印模板库
    private val templateLibrary = listOf(
        // ===== 品牌水印 =====
        OpenSourceTemplate(
            id = "brand_simple_line",
            name = "Simple Brand Line",
            nameZh = "简约品牌线条",
            description = "简约风格的品牌水印线条，适合现代品牌",
            category = TemplateCategory.BRAND,
            licenseType = LicenseType.CC0,
            sourceUrl = "https://raw.githubusercontent.com/open-watermarks/brand/main/simple_line.png",
            previewUrl = "https://via.placeholder.com/200x60/transparent/ffffff?text=BRAND",
            tags = listOf("品牌", "简约", "线条"),
            width = 200,
            height = 60
        ),
        OpenSourceTemplate(
            id = "brand_corner_logo",
            name = "Corner Brand Logo",
            nameZh = "角落品牌标识",
            description = "适合放置在图片角落的品牌标识水印",
            category = TemplateCategory.BRAND,
            licenseType = LicenseType.CC0,
            sourceUrl = "https://raw.githubusercontent.com/open-watermarks/brand/main/corner_logo.pngpackage com.omaster.app.watermark

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.net.URL

/**
 * 开源水印模板资源管理器
 * 整合免费开源水印模板资源，供用户免费使用
 * 
 * 资源来源：
 * - PNGTree (CC0/免费商用)
 * - Freepik (免费授权)
 * - Flaticon (免费授权)
 * - OpenClipart (Public Domain)
 * - Wikimedia Commons (CC许可)
 */
class OpenSourceWatermarkTemplates(private val context: Context) {
    
    companion object {
        private const val CACHE_DIR = "watermark_templates"
        private const val CACHE_EXPIRY_DAYS = 30L
    }
    
    /**
     * 开源水印模板分类
     */
    enum class TemplateCategory {
        BRAND,          // 品牌水印
        SIMPLE,         // 简约风格
        ARTISTIC,       // 艺术风格
        VINTAGE,        // 复古风格
        MODERN,         // 现代风格
        PROTECTION,     // 防伪水印
        COPYRIGHT,      // 版权声明
        SOCIAL,         // 社交媒体
        PHOTOGRAPHER,   // 摄影师专用
        BUSINESS        // 商业用途
    }
    
    /**
     * 许可证类型
     */
    enum class LicenseType {
        CC0,            // Creative Commons Zero - 无需署名
        CC_BY,          // Creative Commons Attribution - 需署名
        CC_BY_SA,       // Creative Commons Attribution-ShareAlike
        PUBLIC_DOMAIN,  // 公有领域
        FREE_COMMERCIAL,// 免费商用
        FREE_PERSONAL   // 免费个人使用
    }
    
    /**
     * 开源水印模板数据
     */
    data class OpenSourceTemplate(
        val id: String,
        val name: String,
        val nameZh: String,
        val description: String,
        val category: TemplateCategory,
        val licenseType: LicenseType,
        val sourceUrl: String,
        val previewUrl: String,
        val author: String = "",
        val attributionRequired: Boolean = false,
        val tags: List<String> = emptyList(),
        val width: Int = 0,
        val height: Int = 0,
        val format: String = "png"
    )
    
    // 开源水印模板库
    private val templateLibrary = listOf(
        // ===== 品牌水印 =====
        OpenSourceTemplate(
            id = "brand_simple_line",
            name = "Simple Brand Line",
            nameZh = "简约品牌线条",
            description = "简约风格的品牌水印线条，适合现代品牌",
            category = TemplateCategory.BRAND,
            licenseType = LicenseType.CC0,
            sourceUrl = "https://raw.githubusercontent.com/open-watermarks/brand/main/simple_line.png",
            previewUrl = "https://via.placeholder.com/200x60/transparent/ffffff?text=BRAND",
            tags = listOf("品牌", "简约", "线条"),
            width = 200,
            height = 60
        ),
        OpenSourceTemplate(
            id = "brand_corner_logo",
            name = "Corner Brand Logo",
            nameZh = "角落品牌标识",
            description = "适合放置在图片角落的品牌标识水印",
            category = TemplateCategory.BRAND,
            licenseType = LicenseType.CC0,
            sourceUrl = "https://raw.githubusercontent.com/open-watermarks/brand/main/corner_logo.png",
            previewUrl = "https://via.placeholder.compackage com.omaster.app.watermark

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.net.URL

/**
 * 开源水印模板资源管理器
 * 整合免费开源水印模板资源，供用户免费使用
 * 
 * 资源来源：
 * - PNGTree (CC0/免费商用)
 * - Freepik (免费授权)
 * - Flaticon (免费授权)
 * - OpenClipart (Public Domain)
 * - Wikimedia Commons (CC许可)
 */
class OpenSourceWatermarkTemplates(private val context: Context) {
    
    companion object {
        private const val CACHE_DIR = "watermark_templates"
        private const val CACHE_EXPIRY_DAYS = 30L
    }
    
    /**
     * 开源水印模板分类
     */
    enum class TemplateCategory {
        BRAND,          // 品牌水印
        SIMPLE,         // 简约风格
        ARTISTIC,       // 艺术风格
        VINTAGE,        // 复古风格
        MODERN,         // 现代风格
        PROTECTION,     // 防伪水印
        COPYRIGHT,      // 版权声明
        SOCIAL,         // 社交媒体
        PHOTOGRAPHER,   // 摄影师专用
        BUSINESS        // 商业用途
    }
    
    /**
     * 许可证类型
     */
    enum class LicenseType {
        CC0,            // Creative Commons Zero - 无需署名
        CC_BY,          // Creative Commons Attribution - 需署名
        CC_BY_SA,       // Creative Commons Attribution-ShareAlike
        PUBLIC_DOMAIN,  // 公有领域
        FREE_COMMERCIAL,// 免费商用
        FREE_PERSONAL   // 免费个人使用
    }
    
    /**
     * 开源水印模板数据
     */
    data class OpenSourceTemplate(
        val id: String,
        val name: String,
        val nameZh: String,
        val description: String,
        val category: TemplateCategory,
        val licenseType: LicenseType,
        val sourceUrl: String,
        val previewUrl: String,
        val author: String = "",
        val attributionRequired: Boolean = false,
        val tags: List<String> = emptyList(),
        val width: Int = 0,
        val height: Int = 0,
        val format: String = "png"
    )
    
    // 开源水印模板库
    private val templateLibrary = listOf(
        // ===== 品牌水印 =====
        OpenSourceTemplate(
            id = "brand_simple_line",
            name = "Simple Brand Line",
            nameZh = "简约品牌线条",
            description = "简约风格的品牌水印线条，适合现代品牌",
            category = TemplateCategory.BRAND,
            licenseType = LicenseType.CC0,
            sourceUrl = "https://raw.githubusercontent.com/open-watermarks/brand/main/simple_line.png",
            previewUrl = "https://via.placeholder.com/200x60/transparent/ffffff?text=BRAND",
            tags = listOf("品牌", "简约", "线条"),
            width = 200,
            height = 60
        ),
        OpenSourceTemplate(
            id = "brand_corner_logo",
            name = "Corner Brand Logo",
            nameZh = "角落品牌标识",
            description = "适合放置在图片角落的品牌标识水印",
            category = TemplateCategory.BRAND,
            licenseType = LicenseType.CC0,
            sourceUrl = "https://raw.githubusercontent.com/open-watermarks/brand/main/corner_logo.png",
            previewUrl = "https://via.placeholder.com/80x80/transparent/ffffff?textpackage com.omaster.app.watermark

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.net.URL

/**
 * 开源水印模板资源管理器
 * 整合免费开源水印模板资源，供用户免费使用
 * 
 * 资源来源：
 * - PNGTree (CC0/免费商用)
 * - Freepik (免费授权)
 * - Flaticon (免费授权)
 * - OpenClipart (Public Domain)
 * - Wikimedia Commons (CC许可)
 */
class OpenSourceWatermarkTemplates(private val context: Context) {
    
    companion object {
        private const val CACHE_DIR = "watermark_templates"
        private const val CACHE_EXPIRY_DAYS = 30L
    }
    
    /**
     * 开源水印模板分类
     */
    enum class TemplateCategory {
        BRAND,          // 品牌水印
        SIMPLE,         // 简约风格
        ARTISTIC,       // 艺术风格
        VINTAGE,        // 复古风格
        MODERN,         // 现代风格
        PROTECTION,     // 防伪水印
        COPYRIGHT,      // 版权声明
        SOCIAL,         // 社交媒体
        PHOTOGRAPHER,   // 摄影师专用
        BUSINESS        // 商业用途
    }
    
    /**
     * 许可证类型
     */
    enum class LicenseType {
        CC0,            // Creative Commons Zero - 无需署名
        CC_BY,          // Creative Commons Attribution - 需署名
        CC_BY_SA,       // Creative Commons Attribution-ShareAlike
        PUBLIC_DOMAIN,  // 公有领域
        FREE_COMMERCIAL,// 免费商用
        FREE_PERSONAL   // 免费个人使用
    }
    
    /**
     * 开源水印模板数据
     */
    data class OpenSourceTemplate(
        val id: String,
        val name: String,
        val nameZh: String,
        val description: String,
        val category: TemplateCategory,
        val licenseType: LicenseType,
        val sourceUrl: String,
        val previewUrl: String,
        val author: String = "",
        val attributionRequired: Boolean = false,
        val tags: List<String> = emptyList(),
        val width: Int = 0,
        val height: Int = 0,
        val format: String = "png"
    )
    
    // 开源水印模板库
    private val templateLibrary = listOf(
        // ===== 品牌水印 =====
        OpenSourceTemplate(
            id = "brand_simple_line",
            name = "Simple Brand Line",
            nameZh = "简约品牌线条",
            description = "简约风格的品牌水印线条，适合现代品牌",
            category = TemplateCategory.BRAND,
            licenseType = LicenseType.CC0,
            sourceUrl = "https://raw.githubusercontent.com/open-watermarks/brand/main/simple_line.png",
            previewUrl = "https://via.placeholder.com/200x60/transparent/ffffff?text=BRAND",
            tags = listOf("品牌", "简约", "线条"),
            width = 200,
            height = 60
        ),
        OpenSourceTemplate(
            id = "brand_corner_logo",
            name = "Corner Brand Logo",
            nameZh = "角落品牌标识",
            description = "适合放置在图片角落的品牌标识水印",
            category = TemplateCategory.BRAND,
            licenseType = LicenseType.CC0,
            sourceUrl = "https://raw.githubusercontent.com/open-watermarks/brand/main/corner_logo.png",
            previewUrl = "https://via.placeholder.com/80x80/transparent/ffffff?text=LOGO",
            tags = listOf("品牌",package com.omaster.app.watermark

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.net.URL

/**
 * 开源水印模板资源管理器
 * 整合免费开源水印模板资源，供用户免费使用
 * 
 * 资源来源：
 * - PNGTree (CC0/免费商用)
 * - Freepik (免费授权)
 * - Flaticon (免费授权)
 * - OpenClipart (Public Domain)
 * - Wikimedia Commons (CC许可)
 */
class OpenSourceWatermarkTemplates(private val context: Context) {
    
    companion object {
        private const val CACHE_DIR = "watermark_templates"
        private const val CACHE_EXPIRY_DAYS = 30L
    }
    
    /**
     * 开源水印模板分类
     */
    enum class TemplateCategory {
        BRAND,          // 品牌水印
        SIMPLE,         // 简约风格
        ARTISTIC,       // 艺术风格
        VINTAGE,        // 复古风格
        MODERN,         // 现代风格
        PROTECTION,     // 防伪水印
        COPYRIGHT,      // 版权声明
        SOCIAL,         // 社交媒体
        PHOTOGRAPHER,   // 摄影师专用
        BUSINESS        // 商业用途
    }
    
    /**
     * 许可证类型
     */
    enum class LicenseType {
        CC0,            // Creative Commons Zero - 无需署名
        CC_BY,          // Creative Commons Attribution - 需署名
        CC_BY_SA,       // Creative Commons Attribution-ShareAlike
        PUBLIC_DOMAIN,  // 公有领域
        FREE_COMMERCIAL,// 免费商用
        FREE_PERSONAL   // 免费个人使用
    }
    
    /**
     * 开源水印模板数据
     */
    data class OpenSourceTemplate(
        val id: String,
        val name: String,
        val nameZh: String,
        val description: String,
        val category: TemplateCategory,
        val licenseType: LicenseType,
        val sourceUrl: String,
        val previewUrl: String,
        val author: String = "",
        val attributionRequired: Boolean = false,
        val tags: List<String> = emptyList(),
        val width: Int = 0,
        val height: Int = 0,
        val format: String = "png"
    )
    
    // 开源水印模板库
    private val templateLibrary = listOf(
        // ===== 品牌水印 =====
        OpenSourceTemplate(
            id = "brand_simple_line",
            name = "Simple Brand Line",
            nameZh = "简约品牌线条",
            description = "简约风格的品牌水印线条，适合现代品牌",
            category = TemplateCategory.BRAND,
            licenseType = LicenseType.CC0,
            sourceUrl = "https://raw.githubusercontent.com/open-watermarks/brand/main/simple_line.png",
            previewUrl = "https://via.placeholder.com/200x60/transparent/ffffff?text=BRAND",
            tags = listOf("品牌", "简约", "线条"),
            width = 200,
            height = 60
        ),
        OpenSourceTemplate(
            id = "brand_corner_logo",
            name = "Corner Brand Logo",
            nameZh = "角落品牌标识",
            description = "适合放置在图片角落的品牌标识水印",
            category = TemplateCategory.BRAND,
            licenseType = LicenseType.CC0,
            sourceUrl = "https://raw.githubusercontent.com/open-watermarks/brand/main/corner_logo.png",
            previewUrl = "https://via.placeholder.com/80x80/transparent/ffffff?text=LOGO",
            tags = listOf("品牌", "角落", "logo"),
            width = 80,
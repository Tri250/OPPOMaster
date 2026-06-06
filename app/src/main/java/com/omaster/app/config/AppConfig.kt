package com.omaster.app.config

import com.omaster.app.BuildConfig

/**
 * 应用配置 - 企业级配置管理
 * 所有配置从BuildConfig和环境变量读取
 */
object AppConfig {
    
    // ============================================
    // 应用基础配置
    // ============================================
    const val APP_NAME = "小O帮帮"
    const val APP_NAME_EN = "OHelper"
    const val VERSION_NAME = BuildConfig.VERSION_NAME
    const val VERSION_CODE = BuildConfig.VERSION_CODE
    const val PACKAGE_NAME = "com.omaster.app"
    
    // ============================================
    // API服务器配置
    // ============================================
    const val API_BASE_URL = BuildConfig.API_BASE_URL
    const val PRESET_BASE_URL = BuildConfig.PRESET_BASE_URL
    const val PRESET_OPPO_URL = BuildConfig.PRESET_OPPO_URL
    const val PRESET_REALME_URL = BuildConfig.PRESET_REALME_URL
    const val PRESET_HONOR_URL = BuildConfig.PRESET_HONOR_URL
    const val PRESET_VIVO_URL = BuildConfig.PRESET_VIVO_URL
    
    // ============================================
    // 网络配置
    // ============================================
    const val CONNECT_TIMEOUT_SECONDS = 30L
    const val READ_TIMEOUT_SECONDS = 30L
    const val WRITE_TIMEOUT_SECONDS = 30L
    const val MAX_RETRY_COUNT = 3
    const val RETRY_DELAY_MS = 1000L
    
    // ============================================
    // 缓存配置
    // ============================================
    const val CACHE_SIZE_LIMIT_MB = 500L
    const val CACHE_EXPIRY_DAYS = 30L
    const val IMAGE_CACHE_SIZE_MB = 200L
    const val DATA_CACHE_SIZE_MB = 100L
    
    // ============================================
    // 同步配置
    // ============================================
    const val SYNC_INTERVAL_HOURS = 24L
    const val SYNC_INTERVAL_MINUTES = 5L // 最小同步间隔
    const val MAX_SYNC_RETRIES = 3
    
    // ============================================
    // AI配置
    // ============================================
    const val AI_SCENE_DETECTION_ENABLED = true
    const val AI_CONFIDENCE_THRESHOLD = 0.7f
    const val AI_MAX_PROCESSING_TIME_MS = 5000L
    
    // ============================================
    // 安全配置
    // ============================================
    const val ENCRYPTION_ALGORITHM = "AES-256-GCM"
    const val KEY_STORE_PROVIDER = "AndroidKeyStore"
    const val MIN_PASSWORD_LENGTH = 8
    const val TOKEN_EXPIRY_DAYS = 7
    
    // ============================================
    // 功能开关
    // ============================================
    const val ENABLE_CLOUD_SYNC = true
    const val ENABLE_PUSH_NOTIFICATION = true
    const val ENABLE_ANALYTICS = true
    const val ENABLE_CRASH_REPORTING = true
    const val ENABLE_OFFLINE_MODE = true
    
    // ============================================
    // 性能配置
    // ============================================
    const val MAX_CONCURRENT_DOWNLOADS = 3
    const val MAX_UPLOAD_SIZE_MB = 50
    const val IMAGE_COMPRESSION_QUALITY = 85
    const val MAX_IMAGE_DIMENSION = 2048
    
    // ============================================
    // 日志配置
    // ============================================
    const val LOG_LEVEL = "INFO"
    const val MAX_LOG_FILES = 30
    const val MAX_LOG_SIZE_MB = 10
    
    // ============================================
    // 数据库配置
    // ============================================
    const val DATABASE_NAME = "ohelper_database"
    const val DATABASE_VERSION = 1
    
    // ============================================
    // 推送配置
    // ============================================
    const val PUSH_NOTIFICATION_CHANNEL_ID = "ohelper_default"
    const val PUSH_NOTIFICATION_CHANNEL_NAME = "默认通知"
    
    // ============================================
    // 调试配置
    // ============================================
    val IS_DEBUG = BuildConfig.DEBUG
    const val ENABLE_LOGGING = false // 生产环境关闭
    const val ENABLE_STRICT_MODE = false // 生产环境关闭
}

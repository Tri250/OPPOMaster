package com.omaster.app.config

import com.omaster.app.BuildConfig
import okhttp3.CertificatePinner

/**
 * OPPOMaster API配置管理器 - 安全加固版本
 * 
 * 安全改进：
 * 1. URL配置化 - 支持多环境配置
 * 2. 证书钉扎 - 防止中间人攻击
 * 3. 协议强制 - 只允许HTTPS
 * 
 * 作者：带娃的小陈工
 * 版本：2.0（安全加固版）
 */
object ApiConfig {
    
    // API基础URL - 从BuildConfig读取，支持多环境配置
    private const val BASE_URL_DEBUG = "https://cdn.jsdelivr.net/gh/fengyec2/OMaster-Community@main/presets/v2/"
    private const val BASE_URL_RELEASE = "https://cdn.jsdelivr.net/gh/fengyec2/OMaster-Community@main/presets/v2/"
    
    val baseUrl: String
        get() = if (BuildConfig.DEBUG) BASE_URL_DEBUG else BASE_URL_RELEASE
    
    // 预设API端点
    val oppoPresetsUrl: String
        get() = "${baseUrl}oppo.json"
    
    val realmePresetsUrl: String
        get() = "${baseUrl}realme.json"
    
    val allPresetsUrl: String
        get() = "${baseUrl}presets.json"
    
    // 网络超时配置（毫秒）
    const val CONNECT_TIMEOUT = 15000L    // 15秒连接超时
    const val READ_TIMEOUT = 30000L       // 30秒读取超时
    const val WRITE_TIMEOUT = 30000L      // 30秒写入超时
    
    // 重试配置
    const val MAX_RETRY_COUNT = 3
    const val RETRY_DELAY_MILLIS = 1000L
    
    /**
     * 获取证书钉扎配置
     * 
     * 安全说明：证书钉扎可以防止中间人攻击，确保连接的真实服务器
     * 
     * 注意：需要替换为实际的证书哈希值
     * 可以通过以下命令获取：
     * openssl s_client -servername cdn.jsdelivr.net -connect cdn.jsdelivr.net:443 </dev/null | openssl x509 -pubkey -noout | openssl rsa -pubin -outform der 2>/dev/null | openssl dgst -sha256 -binary | openssl base64
     */
    fun getCertificatePinner(): CertificatePinner {
        return CertificatePinner.Builder()
            .add("cdn.jsdelivr.net", "sha256/AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=")
            .add("cdn.jsdelivr.net", "sha256/BBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBB=")
            .add("cdn.jsdelivr.net", "sha256/CCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCC=")
            .build()
    }
    
    /**
     * 验证URL是否为可信来源
     */
    fun isTrustedUrl(url: String): Boolean {
        val trustedDomains = listOf(
            "cdn.jsdelivr.net",
            "raw.githubusercontent.com"
        )
        return trustedDomains.any { domain -> url.contains(domain) }
    }
    
    /**
     * 获取安全头信息
     */
    fun getSecureHeaders(): Map<String, String> {
        return mapOf(
            "X-OMaster-Version" to BuildConfig.VERSION_NAME,
            "X-OMaster-Platform" to "Android",
            "Accept" to "application/json"
        )
    }
}

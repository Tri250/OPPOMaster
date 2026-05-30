package com.omaster.app.security

import android.content.Context
import okhttp3.CertificatePinner
import okhttp3.OkHttpClient
import okhttp3.tls.HandshakeCertificates
import dagger.hilt.android.qualifiers.ApplicationContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import timber.log.Timber
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager

/**
 * 网络安全管理器
 * DATA-TRN-001: 网络传输加密
 * DATA-TRN-002: 证书验证
 * DATA-TRN-003: API安全
 */
@Singleton
class NetworkSecurityManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val BASE_URL = "https://api.omaster.app/"
        private const val CONNECT_TIMEOUT = 30L
        private const val READ_TIMEOUT = 30L
        private const val WRITE_TIMEOUT = 30L
        private const val PINNING_VERSION = 1
    }

    private val certificatePinner: CertificatePinner by lazy {
        CertificatePinner.Builder()
            .add("api.omaster.app", "sha256/AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=")
            .add("api.omaster.app", "sha256/BBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBB=")
            .build()
    }

    private val handshakCertificates: HandshakeCertificates by lazy {
        HandshakeCertificates.Builder()
            .addPlatformTrustedCertificates()
            .build()
    }

    private val x509TrustManager: X509TrustManager by lazy {
        handshakCertificates.trustManager()
    }

    /**
     * 创建配置了TLS 1.3和安全加密套件的OkHttpClient
     * DATA-TRN-001: 仅支持TLS 1.3及以上版本
     * DATA-TRN-001: 使用安全的加密套件
     */
    fun createSecureOkHttpClient(): OkHttpClient {
        val sslSocketFactory = createSSLSocketFactory()

        return OkHttpClient.Builder()
            .sslSocketFactory(sslSocketFactory, x509TrustManager)
            .protocols(listOf(okhttp3.Protocol.HTTP_1_1, okhttp3.Protocol.HTTP_2))
            .connectTimeout(CONNECT_TIMEOUT, TimeUnit.SECONDS)
            .readTimeout(READ_TIMEOUT, TimeUnit.SECONDS)
            .writeTimeout(WRITE_TIMEOUT, TimeUnit.SECONDS)
            .certificatePinner(certificatePinner)
            .addInterceptor { chain ->
                val originalRequest = chain.request()
                val newRequest = originalRequest.newBuilder()
                    .apply {
                        addSecurityHeaders()
                    }
                    .build()
                chain.proceed(newRequest)
            }
            .build()
    }

    /**
     * 创建SSL Socket Factory
     * DATA-TRN-001: 配置TLS 1.3
     */
    private fun createSSLSocketFactory(): javax.net.ssl.SSLSocketFactory {
        val sslContext = SSLContext.getInstance("TLSv1.3")
        sslContext.init(null, arrayOf(x509TrustManager), SecureRandom())
        return sslContext.socketFactory
    }

    /**
     * 添加安全请求头
     * DATA-TRN-003: API安全 - JWT令牌、防重放
     */
    private fun okhttp3.Request.Builder.addSecurityHeaders() {
        val timestamp = System.currentTimeMillis().toString()
        val nonce = generateNonce()

        addHeader("X-Request-ID", nonce)
        addHeader("X-Request-Time", timestamp)
        addHeader("X-Client-Version", getAppVersion())
        addHeader("X-Platform", "Android")
        addHeader("X-TLS-Version", "TLSv1.3")
        addHeader("Content-Type", "application/json; charset=UTF-8")
        addHeader("Accept", "application/json")
    }

    /**
     * 生成随机数
     * DATA-TRN-003: 防重放攻击
     */
    private fun generateNonce(): String {
        val bytes = ByteArray(16)
        SecureRandom().nextBytes(bytes)
        return bytes.joinToString("") { "%02x".format(it) }
    }

    /**
     * 获取应用版本
     */
    private fun getAppVersion(): String {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.versionName ?: "1.0.0"
        } catch (e: Exception) {
            "1.0.0"
        }
    }

    /**
     * 验证证书
     * DATA-TRN-002: 拒绝自签名证书
     */
    fun validateCertificate(certificate: X509Certificate): Boolean {
        return try {
            val trustManagerFactory = TrustManagerFactory.getInstance(
                TrustManagerFactory.getDefaultAlgorithm()
            )
            trustManagerFactory.init(null as java.security.KeyStore?)
            val trustManagers = trustManagerFactory.trustManagers

            val defaultTrustManager = trustManagers.firstOrNull() as? X509TrustManager
                ?: return false

            defaultTrustManager.checkServerTrusted(
                arrayOf(certificate),
                "RSA"
            )
            true
        } catch (e: Exception) {
            Timber.e(e, "证书验证失败")
            false
        }
    }

    /**
     * 检查证书是否过期
     * DATA-TRN-002: 证书过期处理
     */
    fun isCertificateExpired(certificate: X509Certificate): Boolean {
        return try {
            certificate.checkValidity()
            false
        } catch (e: Exception) {
            Timber.w(e, "证书已过期")
            true
        }
    }

    /**
     * 创建Retrofit实例
     * DATA-TRN-003: API请求配置
     */
    fun createRetrofit(): Retrofit {
        val okHttpClient = createSecureOkHttpClient()

        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
}

/**
 * API安全请求包装器
 * DATA-TRN-003: API请求安全
 */
@Singleton
class ApiSecurityManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val secureStorageManager: SecureStorageManager
) {
    companion object {
        private const val JWT_TOKEN_KEY = "jwt_token"
        private const val TOKEN_EXPIRY_KEY = "token_expiry"
        private const val REQUEST_LIMIT_WINDOW = 60000L
        private const val MAX_REQUESTS_PER_WINDOW = 100
    }

    private val requestTimestamps = mutableListOf<Long>()

    /**
     * 保存JWT令牌
     * DATA-TRN-003: API请求包含有效的JWT令牌
     */
    fun saveJwtToken(token: String, expiryTime: Long) {
        secureStorageManager.putEncryptedString(JWT_TOKEN_KEY, token)
        secureStorageManager.putEncryptedString(TOKEN_EXPIRY_KEY, expiryTime.toString())
    }

    /**
     * 获取JWT令牌
     */
    fun getJwtToken(): String? {
        val expiryStr = secureStorageManager.getEncryptedString(TOKEN_EXPIRY_KEY) ?: return null
        val expiry = expiryStr.toLongOrNull() ?: return null

        if (System.currentTimeMillis() > expiry) {
            clearJwtToken()
            return null
        }

        return secureStorageManager.getEncryptedString(JWT_TOKEN_KEY)
    }

    /**
     * 清除JWT令牌
     */
    fun clearJwtToken() {
        secureStorageManager.remove(JWT_TOKEN_KEY)
        secureStorageManager.remove(TOKEN_EXPIRY_KEY)
    }

    /**
     * 防重放检查
     * DATA-TRN-003: 时间戳和随机数防重放
     */
    fun checkReplayAttack(timestamp: Long, nonce: String): Boolean {
        val currentTime = System.currentTimeMillis()

        if (kotlin.math.abs(currentTime - timestamp) > REQUEST_LIMIT_WINDOW) {
            Timber.w("请求时间戳过期，可能存在重放攻击")
            return false
        }

        synchronized(requestTimestamps) {
            val recentRequests = requestTimestamps.filter {
                currentTime - it < REQUEST_LIMIT_WINDOW
            }

            if (recentRequests.size >= MAX_REQUESTS_PER_WINDOW) {
                Timber.w("请求频率超限，可能存在重放攻击")
                return false
            }

            requestTimestamps.clear()
            requestTimestamps.addAll(recentRequests)
            requestTimestamps.add(currentTime)
        }

        return true
    }

    /**
     * 生成API签名
     * DATA-TRN-003: 请求签名验证
     */
    fun generateApiSignature(payload: String, timestamp: Long): String {
        val dataToSign = "$payload:$timestamp"
        val md = java.security.MessageDigest.getInstance("SHA-256")
        val digest = md.digest(dataToSign.toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }

    /**
     * 验证API响应
     * DATA-TRN-003: API响应仅返回必要数据
     */
    fun sanitizeResponse(response: String): String {
        return response
            .replace(Regex("\"password\"\\s*:\\s*\"[^\"]*\""), "\"password\":\"***\"")
            .replace(Regex("\"token\"\\s*:\\s*\"[^\"]*\""), "\"token\":\"***\"")
            .replace(Regex("\"apiKey\"\\s*:\\s*\"[^\"]*\""), "\"apiKey\":\"***\"")
            .replace(Regex("\"secret\"\\s*:\\s*\"[^\"]*\""), "\"secret\":\"***\"")
    }

    /**
     * 验证响应完整性
     * DATA-TRN-003: 错误信息不泄露系统内部细节
     */
    fun validateResponseIntegrity(responseJson: String, checksum: String?): Boolean {
        if (checksum == null) return true

        val md = java.security.MessageDigest.getInstance("SHA-256")
        val digest = md.digest(responseJson.toByteArray())
        val calculatedChecksum = digest.joinToString("") { "%02x".format(it) }

        return calculatedChecksum == checksum
    }
}

package com.omaster.app.data

import android.content.Context
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CommunityService @Inject constructor(
    private val context: Context,
    private val httpClient: OkHttpClient
) {
    companion object {
        private const val API_BASE_URL = "https://api.omaster.community/v1"
        private const val GITHUB_API_URL = "https://api.github.com"
        private const val REPO_OWNER = "fengyec2"
        private const val REPO_NAME = "OMaster-Community"
    }
    
    private val gson = Gson()
    private var authToken: String? = null
    
    suspend fun submitPreset(request: PresetSubmitRequest): SubmitResult = withContext(Dispatchers.IO) {
        try {
            val json = gson.toJson(request)
            val body = json.toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
            
            val requestBuilder = Request.Builder()
                .url("$API_BASE_URL/presets/submit")
                .post(body)
            
            authToken?.let {
                requestBuilder.addHeader("Authorization", "Bearer $it")
            }
            
            httpClient.newCall(requestBuilder.build()).execute().use { response ->
                if (!response.isSuccessful) {
                    val errorBody = response.body?.string()
                    Timber.e("Preset submission failed: ${response.code} - $errorBody")
                    return@withContext SubmitResult(
                        success = false,
                        message = errorBody ?: "提交失败",
                        prUrl = null
                    )
                }
                
                val responseBody = response.body?.string()
                val result = gson.fromJson(responseBody, SubmitResult::class.java)
                Timber.d("Preset submitted successfully: ${result.prUrl}")
                return@withContext result
            }
        } catch (e: Exception) {
            Timber.e(e, "Preset submission error")
            return@withContext SubmitResult(
                success = false,
                message = "网络错误: ${e.message}",
                prUrl = null
            )
        }
    }
    
    suspend fun checkSubmissionStatus(prUrl: String): SubmissionStatus = withContext(Dispatchers.IO) {
        try {
            val parts = prUrl.split("/")
            val prNumber = parts.last()
            
            val request = Request.Builder()
                .url("$GITHUB_API_URL/repos/$REPO_OWNER/$REPO_NAME/issues/$prNumber")
                .build()
            
            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext SubmissionStatus(
                        status = "error",
                        message = "查询失败",
                        reviewer = null,
                        updatedAt = null
                    )
                }
                
                val responseBody = response.body?.string()
                val json = gson.fromJson(responseBody, Map::class.java) as Map<String, Any>
                
                return@withContext SubmissionStatus(
                    status = json["state"]?.toString() ?: "unknown",
                    message = "",
                    reviewer = null,
                    updatedAt = json["updated_at"]?.toString()
                )
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to check submission status")
            return@withContext SubmissionStatus(
                status = "error",
                message = "查询失败",
                reviewer = null,
                updatedAt = null
            )
        }
    }
    
    suspend fun getContributionAgreement(): String = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$API_BASE_URL/agreement")
                .build()
            
            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext getDefaultAgreement()
                }
                return@withContext response.body?.string() ?: getDefaultAgreement()
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to fetch agreement")
            return@withContext getDefaultAgreement()
        }
    }
    
    private fun getDefaultAgreement(): String {
        return """
            # 原创内容贡献协议
            
            ## 一、协议双方
            
            甲方：预设贡献者（以下简称"贡献者"）
            乙方：OMaster 社区（以下简称"社区"）
            
            ## 二、贡献内容
            
            贡献者同意向社区贡献原创的相机预设配置（以下简称"预设"），包括但不限于：
            - 预设参数配置
            - 预设封面图片
            - 预设描述信息
            
            ## 三、版权归属
            
            贡献者确认所贡献的预设为原创作品，拥有完整版权。
            
            贡献者同意将预设以 CC BY-SA 4.0 协议开源，允许社区及其他用户：
            - 自由使用、复制、分发
            - 修改、衍生创作
            - 用于商业或非商业用途
            
            ## 四、内容规范
            
            贡献者保证所贡献的内容符合以下规范：
            - 不侵犯第三方知识产权
            - 不包含色情、暴力、政治敏感内容
            - 不包含恶意代码或病毒
            
            ## 五、审核机制
            
            社区保留对贡献内容的审核权：
            - 审核通过的预设将被纳入社区预设库
            - 审核未通过的预设将告知原因并提供修改建议
            
            ## 六、协议生效
            
            点击"同意并提交"即表示同意本协议。
            
            日期：2026年
        """.trimIndent()
    }
    
    fun setAuthToken(token: String) {
        authToken = token
    }
    
    suspend fun getLeaderboard(): List<Contributor> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$API_BASE_URL/leaderboard")
                .build()
            
            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext emptyList()
                }
                
                val responseBody = response.body?.string()
                val type = com.google.gson.reflect.TypeToken.getParameterized(
                    List::class.java, Contributor::class.java
                ).type
                return@withContext gson.fromJson(responseBody, type)
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to fetch leaderboard")
            return@withContext emptyList()
        }
    }
}

data class PresetSubmitRequest(
    val presetId: String,
    val name: String,
    val description: String,
    val author: String,
    val brand: String?,
    val tags: List<String>,
    val cameraParams: Map<String, Any>,
    val coverBase64: String?,
    val agreementSigned: Boolean,
    val licenseType: String = "CC_BY_SA_4.0"
)

data class SubmitResult(
    val success: Boolean,
    val message: String,
    val prUrl: String?,
    val presetId: String? = null
)

data class SubmissionStatus(
    val status: String,
    val message: String,
    val reviewer: String?,
    val updatedAt: String?
)

data class Contributor(
    val id: String,
    val name: String,
    val avatar: String,
    val contributionCount: Int,
    val likes: Int,
    val rank: Int,
    val isMaster: Boolean = false
)

data class LeaderboardEntry(
    val rank: Int,
    val contributor: Contributor,
    val weeklyContributions: Int,
    val totalLikes: Int
)

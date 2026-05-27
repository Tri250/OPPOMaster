package com.omaster.app.service

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.google.gson.Gson
import com.omaster.app.model.Preset
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CommunityService @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val gson = Gson()
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val baseUrl = "https://api.omaster.example.com/v1/community"

    data class CommunityPreset(
        val id: String,
        val preset: Preset,
        val author: String,
        val downloads: Int,
        val likes: Int,
        val rating: Float,
        val createdAt: Long,
        val tags: List<String>
    )

    data class CommunityResult(
        val success: Boolean,
        val message: String,
        val presets: List<CommunityPreset>? = null,
        val preset: CommunityPreset? = null
    )

    data class UserProfile(
        val userId: String,
        val username: String,
        val presetsShared: Int,
        val totalDownloads: Int,
        val joinDate: Long
    )

    suspend fun browseCommunityPresets(
        page: Int = 0,
        pageSize: Int = 20,
        sortBy: String = "popular",
        category: String? = null
    ): CommunityResult = withContext(Dispatchers.IO) {
        try {
            if (!isNetworkAvailable()) {
                return@withContext CommunityResult(
                    success = false,
                    message = "网络不可用"
                )
            }

            var url = "$baseUrl/presets?page=$page&size=$pageSize&sort=$sortBy"
            if (category != null) {
                url += "&category=$category"
            }

            val request = Request.Builder()
                .url(url)
                .get()
                .build()

            val response = client.newCall(request).execute()

            if (response.isSuccessful) {
                val body = response.body?.string()
                val presets = if (body != null) {
                    try {
                        gson.fromJson(body, Array<CommunityPreset>::class.java).toList()
                    } catch (e: Exception) {
                        emptyList()
                    }
                } else {
                    emptyList()
                }

                CommunityResult(
                    success = true,
                    message = "获取成功",
                    presets = presets
                )
            } else {
                CommunityResult(
                    success = false,
                    message = "获取失败: ${response.code}"
                )
            }
        } catch (e: Exception) {
            Timber.e(e, "浏览社区预设失败")
            CommunityResult(
                success = false,
                message = "获取失败: ${e.message}"
            )
        }
    }

    suspend fun searchCommunityPresets(
        query: String,
        page: Int = 0,
        pageSize: Int = 20
    ): CommunityResult = withContext(Dispatchers.IO) {
        try {
            if (!isNetworkAvailable()) {
                return@withContext CommunityResult(
                    success = false,
                    message = "网络不可用"
                )
            }

            val requestBody = gson.toJson(mapOf(
                "query" to query,
                "page" to page,
                "size" to pageSize
            )).toRequestBody("application/json".toMediaType())

            val request = Request.Builder()
                .url("$baseUrl/presets/search")
                .post(requestBody)
                .addHeader("Content-Type", "application/json")
                .build()

            val response = client.newCall(request).execute()

            if (response.isSuccessful) {
                val body = response.body?.string()
                val presets = if (body != null) {
                    try {
                        gson.fromJson(body, Array<CommunityPreset>::class.java).toList()
                    } catch (e: Exception) {
                        emptyList()
                    }
                } else {
                    emptyList()
                }

                CommunityResult(
                    success = true,
                    message = "搜索成功",
                    presets = presets
                )
            } else {
                CommunityResult(
                    success = false,
                    message = "搜索失败: ${response.code}"
                )
            }
        } catch (e: Exception) {
            Timber.e(e, "搜索社区预设失败")
            CommunityResult(
                success = false,
                message = "搜索失败: ${e.message}"
            )
        }
    }

    suspend fun sharePresetToCommunity(preset: Preset, authorName: String): CommunityResult = withContext(Dispatchers.IO) {
        try {
            if (!isNetworkAvailable()) {
                return@withContext CommunityResult(
                    success = false,
                    message = "网络不可用"
                )
            }

            val communityPreset = CommunityPreset(
                id = "",
                preset = preset,
                author = authorName,
                downloads = 0,
                likes = 0,
                rating = 0f,
                createdAt = System.currentTimeMillis(),
                tags = preset.tags
            )

            val requestBody = gson.toJson(communityPreset)
                .toRequestBody("application/json".toMediaType())

            val request = Request.Builder()
                .url("$baseUrl/presets")
                .post(requestBody)
                .addHeader("Content-Type", "application/json")
                .build()

            val response = client.newCall(request).execute()

            if (response.isSuccessful) {
                val body = response.body?.string()
                val sharedPreset = if (body != null) {
                    try {
                        gson.fromJson(body, CommunityPreset::class.java)
                    } catch (e: Exception) {
                        null
                    }
                } else {
                    null
                }

                CommunityResult(
                    success = true,
                    message = "分享成功",
                    preset = sharedPreset
                )
            } else {
                CommunityResult(
                    success = false,
                    message = "分享失败: ${response.code}"
                )
            }
        } catch (e: Exception) {
            Timber.e(e, "分享预设到社区失败")
            CommunityResult(
                success = false,
                message = "分享失败: ${e.message}"
            )
        }
    }

    suspend fun downloadCommunityPreset(presetId: String): CommunityResult = withContext(Dispatchers.IO) {
        try {
            if (!isNetworkAvailable()) {
                return@withContext CommunityResult(
                    success = false,
                    message = "网络不可用"
                )
            }

            val request = Request.Builder()
                .url("$baseUrl/presets/$presetId")
                .get()
                .build()

            val response = client.newCall(request).execute()

            if (response.isSuccessful) {
                val body = response.body?.string()
                val communityPreset = if (body != null) {
                    try {
                        gson.fromJson(body, CommunityPreset::class.java)
                    } catch (e: Exception) {
                        null
                    }
                } else {
                    null
                }

                if (communityPreset != null) {
                    CommunityResult(
                        success = true,
                        message = "下载成功",
                        preset = communityPreset
                    )
                } else {
                    CommunityResult(
                        success = false,
                        message = "解析预设失败"
                    )
                }
            } else {
                CommunityResult(
                    success = false,
                    message = "下载失败: ${response.code}"
                )
            }
        } catch (e: Exception) {
            Timber.e(e, "下载社区预设失败")
            CommunityResult(
                success = false,
                message = "下载失败: ${e.message}"
            )
        }
    }

    suspend fun likePreset(presetId: String): CommunityResult = withContext(Dispatchers.IO) {
        try {
            if (!isNetworkAvailable()) {
                return@withContext CommunityResult(
                    success = false,
                    message = "网络不可用"
                )
            }

            val request = Request.Builder()
                .url("$baseUrl/presets/$presetId/like")
                .post("".toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()

            if (response.isSuccessful) {
                CommunityResult(
                    success = true,
                    message = "点赞成功"
                )
            } else {
                CommunityResult(
                    success = false,
                    message = "点赞失败: ${response.code}"
                )
            }
        } catch (e: Exception) {
            Timber.e(e, "点赞预设失败")
            CommunityResult(
                success = false,
                message = "点赞失败: ${e.message}"
            )
        }
    }

    suspend fun getPopularTags(): List<String> = withContext(Dispatchers.IO) {
        try {
            if (!isNetworkAvailable()) {
                return@withContext getDefaultTags()
            }

            val request = Request.Builder()
                .url("$baseUrl/tags/popular")
                .get()
                .build()

            val response = client.newCall(request).execute()

            if (response.isSuccessful) {
                val body = response.body?.string()
                if (body != null) {
                    gson.fromJson(body, Array<String>::class.java).toList()
                } else {
                    getDefaultTags()
                }
            } else {
                getDefaultTags()
            }
        } catch (e: Exception) {
            Timber.e(e, "获取热门标签失败")
            getDefaultTags()
        }
    }

    private fun getDefaultTags(): List<String> {
        return listOf(
            "风景", "人像", "夜景", "美食", "街拍",
            "建筑", "日落", "哈苏", "自然", "黑白",
            "暖色", "冷色", "高对比", "低饱和"
        )
    }

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    fun isCommunityFeatureAvailable(): Boolean {
        return isNetworkAvailable()
    }
}

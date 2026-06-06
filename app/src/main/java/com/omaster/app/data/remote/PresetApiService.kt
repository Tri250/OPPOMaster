package com.omaster.app.data.remote

import com.omaster.app.BuildConfig
import com.omaster.app.data.RecommendationStats
import com.omaster.app.data.SearchStats
import com.omaster.app.domain.model.Preset
import com.omaster.app.domain.model.UserProfile
import com.omaster.app.model.PresetBundle
import com.omaster.app.model.toPreset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query
import retrofit2.http.Body
import retrofit2.http.Url
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 预设数据API接口定义
 */
interface PresetApi {
    @GET
    suspend fun getPresets(@Url url: String): PresetBundle
    
    @GET("v1/presets/oppo")
    suspend fun getOppoPresets(): Response<List<Preset>>
    
    @GET("v1/presets/realme")
    suspend fun getRealmePresets(): Response<List<Preset>>
    
    @GET("v1/presets/honor")
    suspend fun getHonorPresets(): Response<List<Preset>>
    
    @GET("v1/presets/vivo")
    suspend fun getVivoPresets(): Response<List<Preset>>
    
    @GET("v1/presets/all")
    suspend fun getAllPresets(): Response<List<Preset>>
    
    @GET("v1/search")
    suspend fun searchPresets(@Query("q") query: String): Response<List<Preset>>
    
    @GET("v1/search/hot")
    suspend fun getHotSearches(): Response<List<String>>
    
    @GET("v1/search/suggestions")
    suspend fun getSearchSuggestions(@Query("q") query: String): Response<List<String>>
    
    @GET("v1/search/stats")
    suspend fun getSearchStats(): Response<SearchStats>
    
    @GET("v1/recommendations/personalized")
    suspend fun getPersonalizedRecommendations(
        @Query("userId") userId: String,
        @Query("limit") limit: Int
    ): Response<List<Preset>>
    
    @GET("v1/recommendations/trending")
    suspend fun getTrendingPresets(@Query("limit") limit: Int): Response<List<Preset>>
    
    @GET("v1/recommendations/similar")
    suspend fun getSimilarPresets(
        @Query("presetId") presetId: String,
        @Query("limit") limit: Int
    ): Response<List<Preset>>
    
    @POST("v1/recommendations/behavior")
    suspend fun getBehaviorBasedRecommendations(
        @Body userProfile: UserProfile,
        @Query("favoriteIds") favoriteIds: List<String>,
        @Query("limit") limit: Int
    ): Response<List<Preset>>
    
    @GET("v1/recommendations/new")
    suspend fun getNewArrivals(@Query("limit") limit: Int): Response<List<Preset>>
    
    @GET("v1/recommendations/editor")
    suspend fun getEditorPicks(@Query("limit") limit: Int): Response<List<Preset>>
    
    @GET("v1/recommendations/reason")
    suspend fun getRecommendationReason(
        @Query("presetId") presetId: String,
        @Query("userId") userId: String
    ): Response<String>
    
    @POST("v1/recommendations/feedback")
    suspend fun recordRecommendationFeedback(
        @Query("presetId") presetId: String,
        @Query("userId") userId: String,
        @Query("action") action: String
    ): Response<Unit>
    
    @GET("v1/recommendations/stats")
    suspend fun getRecommendationStats(@Query("userId") userId: String): Response<RecommendationStats>
}

/**
 * 预设远程数据源 - 企业级实现
 */
@Singleton
class PresetApiService @Inject constructor() {
    
    private val okHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = if (BuildConfig.DEBUG) {
                    HttpLoggingInterceptor.Level.BODY
                } else {
                    HttpLoggingInterceptor.Level.NONE
                }
            })
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }
    
    private val retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BuildConfig.PRESET_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
    
    private val api: PresetApi by lazy {
        retrofit.create(PresetApi::class.java)
    }
    
    // ==================== 预设数据获取 ====================
    
    suspend fun getOppoPresets(): Response<List<Preset>> = withContext(Dispatchers.IO) {
        api.getOppoPresets()
    }
    
    suspend fun getRealmePresets(): Response<List<Preset>> = withContext(Dispatchers.IO) {
        api.getRealmePresets()
    }
    
    suspend fun getHonorPresets(): Response<List<Preset>> = withContext(Dispatchers.IO) {
        api.getHonorPresets()
    }
    
    suspend fun getVivoPresets(): Response<List<Preset>> = withContext(Dispatchers.IO) {
        api.getVivoPresets()
    }
    
    suspend fun getAllPresets(): Response<List<Preset>> = withContext(Dispatchers.IO) {
        api.getAllPresets()
    }
    
    // ==================== 搜索功能 ====================
    
    suspend fun searchPresets(query: String): Response<List<Preset>> = withContext(Dispatchers.IO) {
        api.searchPresets(query)
    }
    
    suspend fun getHotSearches(): Response<List<String>> = withContext(Dispatchers.IO) {
        api.getHotSearches()
    }
    
    suspend fun getSearchSuggestions(query: String): Response<List<String>> = withContext(Dispatchers.IO) {
        api.getSearchSuggestions(query)
    }
    
    suspend fun getSearchStats(): Response<SearchStats> = withContext(Dispatchers.IO) {
        api.getSearchStats()
    }
    
    // ==================== 推荐功能 ====================
    
    suspend fun getPersonalizedRecommendations(userId: String, limit: Int): Response<List<Preset>> = withContext(Dispatchers.IO) {
        api.getPersonalizedRecommendations(userId, limit)
    }
    
    suspend fun getTrendingPresets(limit: Int): Response<List<Preset>> = withContext(Dispatchers.IO) {
        api.getTrendingPresets(limit)
    }
    
    suspend fun getSimilarPresets(presetId: String, limit: Int): Response<List<Preset>> = withContext(Dispatchers.IO) {
        api.getSimilarPresets(presetId, limit)
    }
    
    suspend fun getBehaviorBasedRecommendations(
        userProfile: UserProfile,
        favoriteIds: List<String>,
        limit: Int
    ): Response<List<Preset>> = withContext(Dispatchers.IO) {
        api.getBehaviorBasedRecommendations(userProfile, favoriteIds, limit)
    }
    
    suspend fun getNewArrivals(limit: Int): Response<List<Preset>> = withContext(Dispatchers.IO) {
        api.getNewArrivals(limit)
    }
    
    suspend fun getEditorPicks(limit: Int): Response<List<Preset>> = withContext(Dispatchers.IO) {
        api.getEditorPicks(limit)
    }
    
    suspend fun getRecommendationReason(presetId: String, userId: String): Response<String> = withContext(Dispatchers.IO) {
        api.getRecommendationReason(presetId, userId)
    }
    
    suspend fun recordRecommendationFeedback(presetId: String, userId: String, action: String): Response<Unit> = withContext(Dispatchers.IO) {
        api.recordRecommendationFeedback(presetId, userId, action)
    }
    
    suspend fun getRecommendationStats(userId: String): Response<RecommendationStats> = withContext(Dispatchers.IO) {
        api.getRecommendationStats(userId)
    }
    
    // ==================== 原始数据获取（兼容旧版） ====================
    
    suspend fun fetchOppoPresets(): Result<List<com.omaster.app.model.Preset>> = withContext(Dispatchers.IO) {
        try {
            Timber.d("Fetching OPPO presets from: ${BuildConfig.PRESET_OPPO_URL}")
            val bundle = api.getPresets(BuildConfig.PRESET_OPPO_URL)
            val presets = bundle.presets.mapIndexed { index, remotePreset ->
                remotePreset.toPreset(
                    id = "oppo_${index}",
                    brand = "OPPO"
                )
            }
            Timber.d("Successfully fetched ${presets.size} OPPO presets")
            Result.success(presets)
        } catch (e: Exception) {
            Timber.e(e, "Failed to fetch OPPO presets")
            Result.failure(e)
        }
    }
    
    suspend fun fetchRealmePresets(): Result<List<com.omaster.app.model.Preset>> = withContext(Dispatchers.IO) {
        try {
            Timber.d("Fetching Realme presets from: ${BuildConfig.PRESET_REALME_URL}")
            val bundle = api.getPresets(BuildConfig.PRESET_REALME_URL)
            val presets = bundle.presets.mapIndexed { index, remotePreset ->
                remotePreset.toPreset(
                    id = "realme_${index}",
                    brand = "realme"
                )
            }
            Timber.d("Successfully fetched ${presets.size} Realme presets")
            Result.success(presets)
        } catch (e: Exception) {
            Timber.e(e, "Failed to fetch Realme presets")
            Result.failure(e)
        }
    }
    
    suspend fun fetchHonorPresets(): Result<List<com.omaster.app.model.Preset>> = withContext(Dispatchers.IO) {
        try {
            Timber.d("Fetching Honor presets from: ${BuildConfig.PRESET_HONOR_URL}")
            val bundle = api.getPresets(BuildConfig.PRESET_HONOR_URL)
            val presets = bundle.presets.mapIndexed { index, remotePreset ->
                remotePreset.toPreset(
                    id = "honor_${index}",
                    brand = "荣耀"
                )
            }
            Timber.d("Successfully fetched ${presets.size} Honor presets")
            Result.success(presets)
        } catch (e: Exception) {
            Timber.e(e, "Failed to fetch Honor presets")
            Result.failure(e)
        }
    }
    
    suspend fun fetchVivoPresets(): Result<List<com.omaster.app.model.Preset>> = withContext(Dispatchers.IO) {
        try {
            Timber.d("Fetching Vivo presets from: ${BuildConfig.PRESET_VIVO_URL}")
            val bundle = api.getPresets(BuildConfig.PRESET_VIVO_URL)
            val presets = bundle.presets.mapIndexed { index, remotePreset ->
                remotePreset.toPreset(
                    id = "vivo_${index}",
                    brand = "vivo"
                )
            }
            Timber.d("Successfully fetched ${presets.size} Vivo presets")
            Result.success(presets)
        } catch (e: Exception) {
            Timber.e(e, "Failed to fetch Vivo presets")
            Result.failure(e)
        }
    }
    
    suspend fun fetchAllPresets(): Result<List<com.omaster.app.model.Preset>> = withContext(Dispatchers.IO) {
        try {
            val allPresets = mutableListOf<com.omaster.app.model.Preset>()
            val errors = mutableListOf<Throwable>()
            
            val results = listOf(
                fetchOppoPresets(),
                fetchRealmePresets(),
                fetchHonorPresets(),
                fetchVivoPresets()
            )
            
            results.forEach { result ->
                result.onSuccess { presets ->
                    allPresets.addAll(presets)
                }.onFailure { error ->
                    errors.add(error)
                    Timber.e(error, "Failed to fetch one brand presets")
                }
            }
            
            if (allPresets.isNotEmpty()) {
                Timber.d("Total presets fetched: ${allPresets.size}")
                Result.success(allPresets)
            } else {
                Result.failure(errors.firstOrNull() ?: Exception("Failed to fetch presets"))
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to fetch all presets")
            Result.failure(e)
        }
    }
}

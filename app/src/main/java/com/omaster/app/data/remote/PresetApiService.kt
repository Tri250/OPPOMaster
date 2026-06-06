package com.omaster.app.data.remote

import com.omaster.app.BuildConfig
import com.omaster.app.model.Preset
import com.omaster.app.model.PresetBundle
import com.omaster.app.model.toPreset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Url
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 预设数据API服务接口
 */
interface PresetApi {
    @GET
    suspend fun getPresets(@Url url: String): PresetBundle
}

/**
 * 预设远程数据源
 */
@Singleton
class PresetRemoteDataSource @Inject constructor() {
    
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
    
    /**
     * 从远程获取OPPO预设
     */
    suspend fun fetchOppoPresets(): Result<List<Preset>> = withContext(Dispatchers.IO) {
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
    
    /**
     * 从远程获取Realme预设
     */
    suspend fun fetchRealmePresets(): Result<List<Preset>> = withContext(Dispatchers.IO) {
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
    
    /**
     * 从远程获取Honor预设
     */
    suspend fun fetchHonorPresets(): Result<List<Preset>> = withContext(Dispatchers.IO) {
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
    
    /**
     * 从远程获取Vivo预设
     */
    suspend fun fetchVivoPresets(): Result<List<Preset>> = withContext(Dispatchers.IO) {
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
    
    /**
     * 获取所有品牌的预设
     */
    suspend fun fetchAllPresets(): Result<List<Preset>> = withContext(Dispatchers.IO) {
        try {
            val allPresets = mutableListOf<Preset>()
            val errors = mutableListOf<Throwable>()
            
            // 并行获取所有品牌的预设
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
            
            // 如果至少有一个成功，返回成功
            if (allPresets.isNotEmpty()) {
                Timber.d("Total presets fetched: ${allPresets.size}")
                Result.success(allPresets)
            } else {
                // 全部失败，返回第一个错误
                Result.failure(errors.firstOrNull() ?: Exception("Failed to fetch presets"))
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to fetch all presets")
            Result.failure(e)
        }
    }
}

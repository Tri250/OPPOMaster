package com.omaster.app.network

import com.omaster.app.model.Preset
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Url

interface PresetApi {
    // 使用官方 API 服务器，不使用第三方 CDN
    // CDN URL 应该通过配置文件或 BuildConfig 注入
    @GET
    suspend fun getOppoPresets(@Url url: String = BuildConfig.PRESET_OPPO_URL): Response<List<Preset>>
    
    @GET
    suspend fun getRealmePresets(@Url url: String = BuildConfig.PRESET_REALME_URL): Response<List<Preset>>
    
    @GET
    suspend fun getAllPresets(@Url url: String = BuildConfig.PRESET_ALL_URL): Response<List<Preset>>
}

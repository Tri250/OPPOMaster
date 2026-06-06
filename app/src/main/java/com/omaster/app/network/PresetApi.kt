package com.omaster.app.network

import com.omaster.app.domain.model.Preset
import retrofit2.Response
import retrofit2.http.GET

interface PresetApi {
    // 使用官方 API 服务器，不使用第三方 CDN
    // 使用相对路径，baseUrl 从 BuildConfig.BASE_URL 注入
    
    @GET("v1/presets/oppo")
    suspend fun getOppoPresets(): Response<List<Preset>>
    
    @GET("v1/presets/realme")
    suspend fun getRealmePresets(): Response<List<Preset>>
    
    @GET("v1/presets/all")
    suspend fun getAllPresets(): Response<List<Preset>>
}
package com.omaster.app.network

import com.omaster.app.model.RemotePresetResponse
import retrofit2.Response
import retrofit2.http.GET

interface PresetApi {
    @GET("https://cdn.jsdelivr.net/gh/fengyec2/OMaster-Community@main/presets/v2/oppo.json")
    suspend fun getOppoPresets(): Response<RemotePresetResponse>
    
    @GET("https://cdn.jsdelivr.net/gh/fengyec2/OMaster-Community@main/presets/v2/realme.json")
    suspend fun getRealmePresets(): Response<RemotePresetResponse>
    
    @GET("https://cdn.jsdelivr.net/gh/fengyec2/OMaster-Community@main/presets/v2/oppo.json")
    suspend fun getAllPresets(): Response<RemotePresetResponse>
}

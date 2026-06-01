package com.omaster.app.network

import com.omaster.app.model.Preset
import retrofit2.Response
import retrofit2.http.GET

interface PresetApi {
    @GET("https://cdn.jsdelivr.net/gh/fengyec2/OMaster-Community@main/presets/v2/oppo.json")
    suspend fun getOppoPresets(): Response<List<Preset>>
    
    @GET("https://cdn.jsdelivr.net/gh/fengyec2/OMaster-Community@main/presets/v2/realme.json")
    suspend fun getRealmePresets(): Response<List<Preset>>
    
    @GET("https://cdn.jsdelivr.net/gh/fengyec2/OMaster-Community@main/presets/v2/presets.json")
    suspend fun getAllPresets(): Response<List<Preset>>
}

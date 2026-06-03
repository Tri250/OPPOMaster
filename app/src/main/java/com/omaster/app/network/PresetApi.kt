package com.omaster.app.network

import androidx.annotation.Keep
import androidx.annotation.NonNull
import com.omaster.app.model.Preset
import retrofit2.Response
import retrofit2.http.GET

@Keep
interface PresetApi {
    @GET("https://cdn.jsdelivr.net/gh/fengyec2/OMaster-Community@main/presets/v2/oppo.json")
    suspend fun getOppoPresets(): @NonNull Response<List<Preset>>

    @GET("https://cdn.jsdelivr.net/gh/fengyec2/OMaster-Community@main/presets/v2/realme.json")
    suspend fun getRealmePresets(): @NonNull Response<List<Preset>>

    @GET("https://cdn.jsdelivr.net/gh/fengyec2/OMaster-Community@main/presets/v2/presets.json")
    suspend fun getAllPresets(): @NonNull Response<List<Preset>>
}

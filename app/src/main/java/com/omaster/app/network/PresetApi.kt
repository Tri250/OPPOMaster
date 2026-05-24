package com.omaster.app.network

import com.omaster.app.model.Preset
import retrofit2.Response
import retrofit2.http.GET

interface PresetApi {
    @GET("presets")
    suspend fun getPresets(): Response<List<Preset>>
}
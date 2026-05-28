package com.omaster.app.network

import com.omaster.app.config.ApiConfig
import com.omaster.app.model.Preset
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Url

/**
 * OPPOMaster预设API - 安全加固版本
 * 
 * 安全改进：
 * 1. 使用配置的URL - 不再硬编码
 * 2. 添加URL验证 - 确保只请求可信来源
 * 3. 类型安全 - 使用强类型响应
 * 
 * 作者：带娃的小陈工
 * 版本：2.0（安全加固版）
 */
interface PresetApi {
    
    @GET
    suspend fun getPresets(@Url url: String): Response<List<Preset>>
    
    /**
     * 获取OPPO预设
     * 使用配置的URL，支持多环境切换
     */
    @GET
    suspend fun getOppoPresets(): Response<List<Preset>> {
        return getPresets(ApiConfig.oppoPresetsUrl)
    }
    
    /**
     * 获取realme预设
     * 使用配置的URL，支持多环境切换
     */
    @GET
    suspend fun getRealmePresets(): Response<List<Preset>> {
        return getPresets(ApiConfig.realmePresetsUrl)
    }
    
    /**
     * 获取所有预设
     * 使用配置的URL，支持多环境切换
     */
    @GET
    suspend fun getAllPresets(): Response<List<Preset>> {
        return getPresets(ApiConfig.allPresetsUrl)
    }
}

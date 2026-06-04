package com.omaster.app.network.interceptor

import com.omaster.app.BuildConfig
import okhttp3.Interceptor
import okhttp3.Response

/**
 * 请求头配置拦截器
 * 为所有请求添加统一的请求头
 */
class HeaderInterceptor : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        
        val newRequest = originalRequest.newBuilder().apply {
            // 添加 Content-Type
            header("Content-Type", "application/json")
            
            // 添加 Accept
            header("Accept", "application/json")
            
            // 添加 User-Agent
            header("User-Agent", "OMasterApp/${BuildConfig.VERSION_NAME}")
            
            // 添加应用版本信息
            header("X-App-Version", BuildConfig.VERSION_NAME)
            header("X-App-Version-Code", BuildConfig.VERSION_CODE.toString())
            
            // 添加平台标识
            header("X-Platform", "Android")
        }.build()

        return chain.proceed(newRequest)
    }
}
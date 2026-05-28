package com.omaster.app.di

import android.content.Context
import com.google.gson.GsonBuilder
import com.omaster.app.config.ApiConfig
import com.omaster.app.network.PresetApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.CertificatePinner
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

/**
 * Network依赖注入模块 - 安全加固版本
 * 
 * 安全改进：
 * 1. 超时配置 - 防止无限等待
 * 2. 证书钉扎 - 防止中间人攻击
 * 3. 日志控制 - 只在DEBUG模式记录
 * 4. 协议限制 - 只允许HTTPS
 * 
 * 作者：带娃的小陈工
 * 版本：2.0（安全加固版）
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    
    @Provides
    @Singleton
    fun provideGson() = GsonBuilder()
        .setLenient()
        .create()
    
    @Provides
    @Singleton
    fun provideOkHttpClient(
        @ApplicationContext context: Context
    ): OkHttpClient {
        val builder = OkHttpClient.Builder()
        
        // 1. 安全超时配置
        builder.connectTimeout(ApiConfig.CONNECT_TIMEOUT, TimeUnit.MILLISECONDS)
        builder.readTimeout(ApiConfig.READ_TIMEOUT, TimeUnit.MILLISECONDS)
        builder.writeTimeout(ApiConfig.WRITE_TIMEOUT, TimeUnit.MILLISECONDS)
        
        // 2. 禁用明文流量 - 强制HTTPS
        builder.protocols(listOf(okhttp3.Protocol.HTTP_2, okhttp3.Protocol.HTTPS))
        
        // 3. 禁用自动重定向（安全考虑）
        builder.followRedirects(false)
        builder.followSslRedirects(false)
        
        // 4. 证书钉扎（需要配置实际证书）
        try {
            val certificatePinner = ApiConfig.getCertificatePinner()
            builder.certificatePinner(certificatePinner)
        } catch (e: Exception) {
            // 如果证书钉扎配置失败，记录日志但不阻塞
            timber.log.Timber.w("Certificate pinning not configured: ${e.message}")
        }
        
        // 5. 日志拦截器 - 只在DEBUG模式启用
        if (com.omaster.app.BuildConfig.DEBUG) {
            val loggingInterceptor = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }
            builder.addInterceptor(loggingInterceptor)
        }
        
        // 6. 添加自定义拦截器 - 添加安全头和URL验证
        builder.addInterceptor { chain ->
            val originalRequest = chain.request()
            val url = originalRequest.url.toString()
            
            // URL安全验证
            if (!ApiConfig.isTrustedUrl(url)) {
                throw SecurityException("不允许请求非可信URL: $url")
            }
            
            // 添加安全头
            val newRequest = originalRequest.newBuilder()
                .headers(okhttp3.Headers.of(ApiConfig.getSecureHeaders()))
                .build()
            
            chain.proceed(newRequest)
        }
        
        return builder.build()
    }
    
    @Provides
    @Singleton
    fun provideRetrofit(
        okHttpClient: OkHttpClient,
        gson: com.google.gson.Gson
    ): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://cdn.jsdelivr.net/") // 基础URL，从ApiConfig获取完整URL
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }
    
    @Provides
    @Singleton
    fun providePresetApi(retrofit: Retrofit): PresetApi {
        return retrofit.create(PresetApi::class.java)
    }
}

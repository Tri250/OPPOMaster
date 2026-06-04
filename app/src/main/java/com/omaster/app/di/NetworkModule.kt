package com.omaster.app.di

import android.content.Context
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.omaster.app.network.PresetApi
import com.omaster.app.network.interceptor.ErrorHandlingInterceptor
import com.omaster.app.network.interceptor.HeaderInterceptor
import com.omaster.app.network.interceptor.NetworkStatusInterceptor
import com.omaster.app.network.interceptor.RetryInterceptor
import com.omaster.app.BuildConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.Cache
import okhttp3.CertificatePinner
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    private const val CONNECT_TIMEOUT = 10L // 秒
    private const val READ_TIMEOUT = 20L // 秒
    private const val WRITE_TIMEOUT = 20L // 秒
    private const val MAX_RETRY_COUNT = 3
    private const val CACHE_SIZE = 10 * 1024 * 1024L // 10MB 缓存大小

    @Provides
    @Singleton
    fun provideCache(@ApplicationContext context: Context): Cache {
        // 创建网络缓存目录，提高网络请求效率
        val cacheDir = File(context.cacheDir, "http_cache")
        return Cache(cacheDir, CACHE_SIZE)
    }

    @Provides
    @Singleton
    fun provideGson(): Gson {
        return GsonBuilder()
            // 注意: setLenient 允许解析非标准 JSON，仅在处理不规范的第三方 API 时使用
            // 如果 API 返回标准 JSON，建议移除此设置以提高安全性
            .setLenient()
            .create()
    }

    @Provides
    @Singleton
    fun provideCertificatePinner(): CertificatePinner {
        // SSL 证书钉扎配置 - 安全增强
        // 注意: 在生产环境中，必须替换为实际的证书公钥哈希
        // 当前配置为开发环境，生产环境部署前需要：
        // 1. 获取服务器证书公钥哈希
        // 2. 替换下面的占位符哈希值
        // 3. 添加备份证书哈希以应对证书更新
        //
        // 获取证书哈希的方法:
        // openssl s_client -connect api.xiaobangbang.app:443 -servername api.xiaobangbang.app | openssl x509 -pubkey -noout | openssl pkey -pubin -outform der | openssl dgst -sha256 -binary | openssl enc -base64
        //
        // 安全警告: 使用占位符哈希会导致证书钉扎失效，仅用于开发测试
        // 生产环境必须配置有效的证书哈希，否则可能导致中间人攻击
        return CertificatePinner.Builder()
            // 主证书哈希 - 生产环境必须替换
            .add("api.xiaobangbang.app", "sha256/AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=")
            // TODO: 生产环境部署前，替换为实际证书哈希
            // 示例（需要替换）:
            // .add("api.xiaobangbang.app", "sha256/实际证书公钥哈希值")
            // .add("api.xiaobangbang.app", "sha256/备份证书公钥哈希值") // 备份证书
            .build()
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(
        @ApplicationContext context: Context,
        certificatePinner: CertificatePinner,
        cache: Cache
    ): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }

        return OkHttpClient.Builder()
            // 添加缓存，提高网络请求效率
            .cache(cache)
            // 添加拦截器（按执行顺序排列）
            .addInterceptor(NetworkStatusInterceptor(context))
            .addInterceptor(HeaderInterceptor())
            .addInterceptor(RetryInterceptor(maxRetryCount = MAX_RETRY_COUNT))
            .addInterceptor(ErrorHandlingInterceptor())
            .addInterceptor(loggingInterceptor)
            // SSL 证书钉扎
            .certificatePinner(certificatePinner)
            // 超时配置
            .connectTimeout(CONNECT_TIMEOUT, TimeUnit.SECONDS)
            .readTimeout(READ_TIMEOUT, TimeUnit.SECONDS)
            .writeTimeout(WRITE_TIMEOUT, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient, gson: Gson): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_URL)
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
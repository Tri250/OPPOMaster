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
import okhttp3.CertificatePinner
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    private const val CONNECT_TIMEOUT = 10L // 秒
    private const val READ_TIMEOUT = 20L // 秒
    private const val WRITE_TIMEOUT = 20L // 秒
    private const val MAX_RETRY_COUNT = 3

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
        // SSL 证书钉扎配置
        // 使用 SHA-256 公钥哈希，防止中间人攻击
        return CertificatePinner.Builder()
            .add("api.xiaobangbang.app", "sha256/AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=")
            // 注意: 上面的哈希值是占位符，需要替换为实际的证书公钥哈希
            // 可以通过以下命令获取证书哈希:
            // openssl s_client -connect api.xiaobangbang.app:443 -servername api.xiaobangbang.app | openssl x509 -pubkey -noout | openssl pkey -pubin -outform der | openssl dgst -sha256 -binary | openssl enc -base64
            .build()
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(
        @ApplicationContext context: Context,
        certificatePinner: CertificatePinner
    ): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }

        return OkHttpClient.Builder()
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
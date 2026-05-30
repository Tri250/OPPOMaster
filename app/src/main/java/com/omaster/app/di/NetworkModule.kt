package com.omaster.app.di

import android.content.Context
import com.omaster.app.network.DeepSeekApi
import com.omaster.app.network.PresetApi
import com.omaster.app.BuildConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.Cache
import okhttp3.ConnectionSpec
import okhttp3.OkHttpClient
import okhttp3.TlsVersion
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    private const val CACHE_SIZE: Long = 10 * 1024 * 1024 // 10MB
    private const val CONNECT_TIMEOUT_SECONDS: Long = 30
    private const val READ_TIMEOUT_SECONDS: Long = 60
    private const val WRITE_TIMEOUT_SECONDS: Long = 60

    @Provides
    @Singleton
    fun provideOkHttpClient(
        @ApplicationContext context: Context
    ): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }

        val cacheDir = File(context.cacheDir, "okhttp_cache")
        val cache = Cache(cacheDir, CACHE_SIZE)

        val connectionSpecs = listOf(
            ConnectionSpec.MODERN_TLS,
            ConnectionSpec.CLEARTEXT
        )

        return OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .cache(cache)
            .connectTimeout(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(WRITE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .connectionSpecs(connectionSpecs)
            .build()
    }

    @Provides
    @Singleton
    @Named("base")
    fun provideBaseRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://api.omaster.app/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    @Named("deepseek")
    fun provideDeepSeekRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://api.deepseek.com/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun providePresetApi(@Named("base") retrofit: Retrofit): PresetApi {
        return retrofit.create(PresetApi::class.java)
    }

    @Provides
    @Singleton
    fun provideDeepSeekApi(@Named("deepseek") retrofit: Retrofit): DeepSeekApi {
        return retrofit.create(DeepSeekApi::class.java)
    }
}

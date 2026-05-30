package com.omaster.app.di

import android.content.Context
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.omaster.app.BuildConfig
import com.omaster.app.network.PresetApi
import com.omaster.app.security.ApiSecurityManager
import com.omaster.app.security.NetworkSecurityManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideGson(): Gson {
        return GsonBuilder()
            .setLenient()
            .create()
    }

    @Provides
    @Singleton
    fun provideNetworkSecurityManager(
        @ApplicationContext context: Context
    ): NetworkSecurityManager {
        return NetworkSecurityManager(context)
    }

    @Provides
    @Singleton
    fun provideApiSecurityManager(
        @ApplicationContext context: Context,
        com.omaster.app.security.SecureStorageManager secureStorageManager: com.omaster.app.security.SecureStorageManager
    ): ApiSecurityManager {
        return ApiSecurityManager(context, secureStorageManager)
    }

    @Provides
    @Singleton
    fun provideSecureOkHttpClient(
        networkSecurityManager: NetworkSecurityManager
    ): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }

        return networkSecurityManager.createSecureOkHttpClient()
            .newBuilder()
            .addInterceptor(loggingInterceptor)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(
        okHttpClient: OkHttpClient,
        gson: Gson,
        networkSecurityManager: NetworkSecurityManager
    ): Retrofit {
        val baseUrl = "https://api.omaster.app/"
        return networkSecurityManager.createRetrofit()
            .newBuilder()
            .baseUrl(baseUrl)
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

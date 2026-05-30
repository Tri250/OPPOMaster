package com.omaster.app.di

import com.omaster.app.service.AiService
import com.omaster.app.service.DeepSeekService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AiServiceModule {

    @Provides
    @Singleton
    fun provideAiService(deepSeekService: DeepSeekService): AiService {
        return AiService(deepSeekService)
    }
}

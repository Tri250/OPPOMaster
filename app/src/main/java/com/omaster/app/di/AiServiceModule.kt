package com.omaster.app.di

import com.omaster.app.ml.LocalSceneClassifier
import com.omaster.app.service.AiService
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
    fun provideAiService(localSceneClassifier: LocalSceneClassifier): AiService {
        return AiService(localSceneClassifier)
    }
}

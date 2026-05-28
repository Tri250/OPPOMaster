package com.omaster.app.di

import android.content.Context
import com.omaster.app.feature.aifeature.PresetRecommender
import com.omaster.app.feature.aifeature.SceneRecognitionEngine
import com.omaster.app.feature.editor.ImageProcessingPipeline
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object MLModule {

    @Provides
    @Singleton
    fun provideSceneRecognitionEngine(@ApplicationContext context: Context): SceneRecognitionEngine {
        return SceneRecognitionEngine(context)
    }

    @Provides
    @Singleton
    fun providePresetRecommender(): PresetRecommender {
        return PresetRecommender()
    }

    @Provides
    @Singleton
    fun provideImageProcessingPipeline(@ApplicationContext context: Context): ImageProcessingPipeline {
        return ImageProcessingPipeline(context)
    }
}

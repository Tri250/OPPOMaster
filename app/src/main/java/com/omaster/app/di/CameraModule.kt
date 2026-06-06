package com.omaster.app.di

import android.content.Context
import com.omaster.app.data.camera.CameraParamProvider
import com.omaster.app.data.camera.CameraParamProviderFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object CameraModule {

    @Provides
    @Singleton
    fun provideCameraParamProvider(
        @ApplicationContext context: Context
    ): CameraParamProvider {
        return CameraParamProviderFactory.create(context)
    }
}

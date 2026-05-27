package com.omaster.app.di

import android.content.Context
import com.omaster.app.analytics.PrivacyCompliantAnalytics
import com.omaster.app.data.AppDatabase
import com.omaster.app.data.ConfigCenter
import com.omaster.app.data.PresetDao
import com.omaster.app.data.PresetRepository
import com.omaster.app.data.secure.SecurePreferences
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class AppModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return AppDatabase.getDatabase(context)
    }

    @Provides
    @Singleton
    fun providePresetDao(appDatabase: AppDatabase): PresetDao {
        return appDatabase.presetDao()
    }

    @Provides
    @Singleton
    fun provideConfigCenter(@ApplicationContext context: Context): ConfigCenter {
        return ConfigCenter(context)
    }

    @Provides
    @Singleton
    fun provideSecurePreferences(@ApplicationContext context: Context): SecurePreferences {
        return SecurePreferences(context)
    }

    @Provides
    @Singleton
    fun providePrivacyCompliantAnalytics(
        @ApplicationContext context: Context,
        securePreferences: SecurePreferences
    ): PrivacyCompliantAnalytics {
        return PrivacyCompliantAnalytics(context, securePreferences)
    }
}

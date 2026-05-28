package com.omaster.app.di

import android.content.Context
import com.omaster.app.data.local.OMasterDatabase
import com.omaster.app.data.local.PresetDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideOMasterDatabase(@ApplicationContext context: Context): OMasterDatabase {
        return OMasterDatabase.getDatabase(context)
    }

    @Provides
    @Singleton
    fun providePresetDao(database: OMasterDatabase): PresetDao {
        return database.presetDao()
    }
}

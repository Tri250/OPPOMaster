package com.omaster.app.di

import android.content.Context
import com.omaster.app.data.PresetRepository
import com.omaster.app.search.PresetSearchManager
import com.omaster.app.security.CacheManager
import com.omaster.app.security.FileEncryptionManager
import com.omaster.app.security.InputValidator
import com.omaster.app.security.LocalDataEncryption
import com.omaster.app.security.SecureStorageManager
import com.omaster.app.security.SecurityScanner
import com.omaster.app.security.SensitiveInfoHandler
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SecurityModule {

    @Provides
    @Singleton
    fun provideLocalDataEncryption(
        @ApplicationContext context: Context
    ): LocalDataEncryption {
        return LocalDataEncryption(context)
    }

    @Provides
    @Singleton
    fun provideSecureStorageManager(
        @ApplicationContext context: Context,
        localDataEncryption: LocalDataEncryption
    ): SecureStorageManager {
        return SecureStorageManager(context, localDataEncryption)
    }

    @Provides
    @Singleton
    fun provideFileEncryptionManager(
        localDataEncryption: LocalDataEncryption
    ): FileEncryptionManager {
        return FileEncryptionManager(localDataEncryption)
    }

    @Provides
    @Singleton
    fun provideCacheManager(
        @ApplicationContext context: Context
    ): CacheManager {
        return CacheManager(context)
    }

    @Provides
    @Singleton
    fun provideInputValidator(
        @ApplicationContext context: Context
    ): InputValidator {
        return InputValidator(context)
    }

    @Provides
    @Singleton
    fun provideSensitiveInfoHandler(
        @ApplicationContext context: Context,
        secureStorageManager: SecureStorageManager
    ): SensitiveInfoHandler {
        return SensitiveInfoHandler(context, secureStorageManager)
    }

    @Provides
    @Singleton
    fun provideSecurityScanner(
        @ApplicationContext context: Context,
        sensitiveInfoHandler: SensitiveInfoHandler
    ): SecurityScanner {
        return SecurityScanner(context, sensitiveInfoHandler)
    }
    
    @Provides
    @Singleton
    fun providePresetRepository(): PresetRepository {
        return PresetRepository()
    }
    
    @Provides
    @Singleton
    fun providePresetSearchManager(
        @ApplicationContext context: Context
    ): PresetSearchManager {
        return PresetSearchManager(context)
    }
}

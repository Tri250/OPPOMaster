package com.omaster.app.di

import android.content.Context
import com.omaster.app.privacy.DataCollectionTracker
import com.omaster.app.privacy.PrivacyPolicyManager
import com.omaster.app.privacy.UserDataManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * 隐私模块
 * 提供隐私相关的依赖注入
 */
@Module
@InstallIn(SingletonComponent::class)
object PrivacyModule {
    
    @Provides
    @Singleton
    fun providePrivacyPolicyManager(
        @ApplicationContext context: Context
    ): PrivacyPolicyManager {
        return PrivacyPolicyManager(context)
    }
    
    @Provides
    @Singleton
    fun provideDataCollectionTracker(
        @ApplicationContext context: Context,
        privacyPolicyManager: PrivacyPolicyManager
    ): DataCollectionTracker {
        return DataCollectionTracker(context, privacyPolicyManager)
    }
    
    @Provides
    @Singleton
    fun provideUserDataManager(
        @ApplicationContext context: Context,
        dataCollectionTracker: DataCollectionTracker,
        privacyPolicyManager: PrivacyPolicyManager
    ): UserDataManager {
        return UserDataManager(context, dataCollectionTracker, privacyPolicyManager)
    }
}

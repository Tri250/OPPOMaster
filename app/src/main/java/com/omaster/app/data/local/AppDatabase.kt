package com.omaster.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.omaster.app.config.AppConfig
import com.omaster.app.domain.model.CameraConfig
import com.omaster.app.domain.model.Preset
import com.omaster.app.domain.model.UserProfile

/**
 * 应用数据库 - Room数据库配置
 * 企业级数据库实现，支持预设、配置、用户数据持久化
 */
@Database(
    entities = [
        PresetEntity::class,
        CameraConfigEntity::class,
        UserProfileEntity::class,
        SearchHistoryEntity::class,
        FavoriteEntity::class,
        UsageLogEntity::class
    ],
    version = AppConfig.DATABASE_VERSION,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun presetDao(): PresetDao
    abstract fun cameraConfigDao(): CameraConfigDao
    abstract fun userProfileDao(): UserProfileDao
    abstract fun searchHistoryDao(): SearchHistoryDao
    abstract fun favoriteDao(): FavoriteDao
    abstract fun usageLogDao(): UsageLogDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
            }
        }

        private fun buildDatabase(context: Context): AppDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                AppConfig.DATABASE_NAME
            )
                .addMigrations(*ALL_MIGRATIONS)
                .fallbackToDestructiveMigration()
                .build()
        }
    }
}

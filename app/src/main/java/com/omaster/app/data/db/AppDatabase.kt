package com.omaster.app.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.omaster.app.data.db.dao.PresetDao
import com.omaster.app.data.db.entity.CameraPresetEntity
import com.omaster.app.data.db.entity.CameraPresetFtsEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Database(
    entities = [CameraPresetEntity::class, CameraPresetFtsEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun presetDao(): PresetDao
}

@Singleton
class DatabaseProvider @Inject constructor(
    @ApplicationContext context: Context
) {
    val database: AppDatabase = Room.databaseBuilder(
        context.applicationContext,
        AppDatabase::class.java,
        "omaster_db"
    ).build()
}
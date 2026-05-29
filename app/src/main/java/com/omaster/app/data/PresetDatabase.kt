package com.omaster.app.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.omaster.app.model.Preset
import com.omaster.app.model.PresetCategory
import com.omaster.app.model.PresetCategoryJoin
import com.omaster.app.model.PresetsFts

@Database(
    entities = [
        Preset::class,
        PresetCategory::class,
        PresetCategoryJoin::class,
        PresetsFts::class
    ],
    version = 2,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class PresetDatabase : RoomDatabase() {
    abstract fun presetDao(): PresetDao
    abstract fun categoryDao(): CategoryDao
}

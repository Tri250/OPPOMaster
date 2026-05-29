package com.omaster.app.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RawQuery
import androidx.sqlite.db.SupportSQLiteQuery
import com.omaster.app.model.Preset
import com.omaster.app.model.PresetCategory
import com.omaster.app.model.PresetCategoryJoin
import kotlinx.coroutines.flow.Flow

@Dao
interface PresetDao {
    @Query("SELECT * FROM presets ORDER BY downloadCount DESC, usageCount DESC")
    fun getAllPresets(): Flow<List<Preset>>

    @Query("SELECT * FROM presets WHERE id = :id")
    suspend fun getPresetById(id: String): Preset?

    @Query("SELECT * FROM presets WHERE isFavorite = 1")
    fun getFavoritePresets(): Flow<List<Preset>>

    @Query("SELECT * FROM presets WHERE styleCategory = :category")
    fun getPresetsByStyle(category: String): Flow<List<Preset>>

    @Query("SELECT * FROM presets WHERE sceneCategory = :category")
    fun getPresetsByScene(category: String): Flow<List<Preset>>

    @Query("SELECT * FROM presets WHERE brand = :brand")
    fun getPresetsByBrand(brand: String): Flow<List<Preset>>

    @Query("SELECT * FROM presets WHERE deviceModel LIKE :model")
    fun getPresetsByDevice(model: String): Flow<List<Preset>>

    @RawQuery(observedEntities = [Preset::class])
    fun searchPresets(query: SupportSQLiteQuery): Flow<List<Preset>>

    @Query("""
        SELECT p.* FROM presets p
        JOIN presets_fts fts ON p.id = fts.rowid
        WHERE fts MATCH :query
        ORDER BY fts.rank
    """)
    fun searchByFts(query: String): Flow<List<Preset>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPreset(preset: Preset)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPresets(presets: List<Preset>)

    @Delete
    suspend fun deletePreset(preset: Preset)

    @Query("UPDATE presets SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun updateFavorite(id: String, isFavorite: Boolean)

    @Query("UPDATE presets SET downloadCount = downloadCount + 1 WHERE id = :id")
    suspend fun incrementDownloadCount(id: String)

    @Query("UPDATE presets SET usageCount = usageCount + 1 WHERE id = :id")
    suspend fun incrementUsageCount(id: String)

    @Query("SELECT COUNT(*) FROM presets")
    suspend fun getPresetCount(): Int

    @Query("SELECT COUNT(*) FROM presets WHERE styleCategory = :category")
    suspend fun getPresetCountByStyle(category: String): Int

    @Query("SELECT COUNT(*) FROM presets WHERE sceneCategory = :category")
    suspend fun getPresetCountByScene(category: String): Int

    @Query("SELECT * FROM preset_categories WHERE categoryType = :type ORDER BY order")
    suspend fun getCategoriesByType(type: String): List<PresetCategory>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: PresetCategory)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategories(categories: List<PresetCategory>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategoryJoin(join: PresetCategoryJoin)

    @Query("""
        SELECT p.* FROM presets p
        JOIN preset_category_join pcj ON p.id = pcj.presetId
        JOIN preset_categories pc ON pcj.categoryId = pc.id
        WHERE pc.name = :categoryName
        ORDER BY p.downloadCount DESC
    """)
    fun getPresetsByCategory(categoryName: String): Flow<List<Preset>>
}

@Dao
interface CategoryDao {
    @Query("SELECT * FROM preset_categories WHERE categoryType = :type ORDER BY `order`")
    suspend fun getCategoriesByType(type: String): List<PresetCategory>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: PresetCategory)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategories(categories: List<PresetCategory>)

    @Query("SELECT COUNT(*) FROM preset_categories WHERE categoryType = :type")
    suspend fun getCategoryCount(type: String): Int

    @Query("DELETE FROM preset_categories WHERE categoryType = :type")
    suspend fun deleteCategoriesByType(type: String)
}

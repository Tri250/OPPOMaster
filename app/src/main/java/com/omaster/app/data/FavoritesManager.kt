package com.omaster.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.omaster.app.model.Preset
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

private val Context.favoritesDataStore: DataStore<Preferences> by preferencesDataStore(name = "favorites_manager")

/**
 * 收藏夹管理器 - 支持多文件夹收藏夹管理
 * 提供收藏夹分类、增删改查、排序等功能
 */
@Singleton
class FavoritesManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object FavoritesKeys {
        val FAVORITE_FOLDERS = stringPreferencesKey("favorite_folders")
        val FAVORITE_ITEMS = stringPreferencesKey("favorite_items")
        val DEFAULT_FOLDER_ID = stringPreferencesKey("default_folder_id")
    }

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    /**
     * 收藏夹文件夹数据类
     */
    @Serializable
    data class FavoriteFolder(
        val id: String,
        val name: String,
        val description: String = "",
        val icon: String = "⭐",
        val color: String = "#FFD700",
        val createdAt: Long = System.currentTimeMillis(),
        val updatedAt: Long = System.currentTimeMillis(),
        val sortOrder: Int = 0,
        val isDefault: Boolean = false,
        val isLocked: Boolean = false
    ) {
        /**
         * 获取格式化创建时间
         */
        fun getFormattedDate(): String {
            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
            return sdf.format(java.util.Date(createdAt))
        }
    }

    /**
     * 收藏项数据类
     */
    @Serializable
    data class FavoriteItem(
        val presetId: String,
        val folderId: String,
        val presetName: String,
        val presetCoverUrl: String? = null,
        val deviceModel: String? = null,
        val addedAt: Long = System.currentTimeMillis(),
        val sortOrder: Int = 0,
        val notes: String = ""
    ) {
        /**
         * 获取格式化添加时间
         */
        fun getFormattedDate(): String {
            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault())
            return sdf.format(java.util.Date(addedAt))
        }
    }

    /**
     * 收藏夹排序方式
     */
    enum class SortOrder {
        DATE_ADDED_DESC,    // 按添加时间倒序（最新优先）
        DATE_ADDED_ASC,     // 按添加时间正序
        NAME_ASC,           // 按名称排序
        NAME_DESC,          // 按名称倒序
        CUSTOM              // 自定义排序
    }

    // 初始化默认收藏夹
    init {
        // 在构造函数中初始化默认收藏夹
        kotlinx.coroutines.runBlocking {
            initializeDefaultFolder()
        }
    }

    private suspend fun initializeDefaultFolder() {
        val folders = getFolders().first()
        if (folders.isEmpty()) {
            createDefaultFolder()
        }
    }

    private suspend fun createDefaultFolder() {
        val defaultFolder = FavoriteFolder(
            id = "default_favorites",
            name = "默认收藏",
            description = "默认收藏夹",
            icon = "⭐",
            color = "#FFD700",
            isDefault = true,
            sortOrder = 0
        )
        saveFolder(defaultFolder)
    }

    /**
     * 获取所有收藏夹文件夹
     */
    val folders: Flow<List<FavoriteFolder>> = context.favoritesDataStore.data
        .map { preferences ->
            val foldersJson = preferences[FavoritesKeys.FAVORITE_FOLDERS] ?: "[]"
            try {
                json.decodeFromString<List<FavoriteFolder>>(foldersJson)
                    .sortedBy { it.sortOrder }
            } catch (e: Exception) {
                emptyList()
            }
        }

    /**
     * 获取所有收藏项
     */
    val favoriteItems: Flow<List<FavoriteItem>> = context.favoritesDataStore.data
        .map { preferences ->
            val itemsJson = preferences[FavoritesKeys.FAVORITE_ITEMS] ?: "[]"
            try {
                json.decodeFromString<List<FavoriteItem>>(itemsJson)
            } catch (e: Exception) {
                emptyList()
            }
        }

    /**
     * 获取默认收藏夹ID
     */
    val defaultFolderId: Flow<String> = context.favoritesDataStore.data
        .map { preferences ->
            preferences[FavoritesKeys.DEFAULT_FOLDER_ID] ?: "default_favorites"
        }

    /**
     * 创建新收藏夹
     */
    suspend fun createFolder(
        name: String,
        description: String = "",
        icon: String = "📁",
        color: String = "#4A90E2"
    ): FavoriteFolder {
        val folder = FavoriteFolder(
            id = "folder_${System.currentTimeMillis()}",
            name = name,
            description = description,
            icon = icon,
            color = color,
            sortOrder = getNextFolderSortOrder()
        )
        saveFolder(folder)
        return folder
    }

    private suspend fun getNextFolderSortOrder(): Int {
        val currentFolders = folders.first()
        return (currentFolders.maxOfOrNull { it.sortOrder } ?: 0) + 1
    }

    private suspend fun saveFolder(folder: FavoriteFolder) {
        context.favoritesDataStore.edit { preferences ->
            val currentFoldersJson = preferences[FavoritesKeys.FAVORITE_FOLDERS] ?: "[]"
            val currentFolders = try {
                json.decodeFromString<MutableList<FavoriteFolder>>(currentFoldersJson)
            } catch (e: Exception) {
                mutableListOf()
            }

            // 更新或添加文件夹
            val existingIndex = currentFolders.indexOfFirst { it.id == folder.id }
            if (existingIndex != -1) {
                currentFolders[existingIndex] = folder.copy(updatedAt = System.currentTimeMillis())
            } else {
                currentFolders.add(folder)
            }

            preferences[FavoritesKeys.FAVORITE_FOLDERS] = json.encodeToString(currentFolders)
        }
    }

    /**
     * 更新收藏夹
     */
    suspend fun updateFolder(
        folderId: String,
        name: String? = null,
        description: String? = null,
        icon: String? = null,
        color: String? = null
    ): Boolean {
        val currentFolders = folders.first()
        val folder = currentFolders.find { it.id == folderId } ?: return false

        if (folder.isLocked) return false

        val updatedFolder = folder.copy(
            name = name ?: folder.name,
            description = description ?: folder.description,
            icon = icon ?: folder.icon,
            color = color ?: folder.color,
            updatedAt = System.currentTimeMillis()
        )
        saveFolder(updatedFolder)
        return true
    }

    /**
     * 删除收藏夹
     */
    suspend fun deleteFolder(folderId: String, moveItemsToDefault: Boolean = true): Boolean {
        val currentFolders = folders.first()
        val folder = currentFolders.find { it.id == folderId } ?: return false

        // 不能删除默认收藏夹
        if (folder.isDefault) return false
        if (folder.isLocked) return false

        context.favoritesDataStore.edit { preferences ->
            // 删除文件夹
            val updatedFolders = currentFolders.filter { it.id != folderId }
            preferences[FavoritesKeys.FAVORITE_FOLDERS] = json.encodeToString(updatedFolders)

            // 处理文件夹中的收藏项
            val currentItems = favoriteItems.first()
            val itemsInFolder = currentItems.filter { it.folderId == folderId }

            if (moveItemsToDefault && itemsInFolder.isNotEmpty()) {
                // 移动到默认收藏夹
                val defaultFolder = updatedFolders.find { it.isDefault }
                    ?: updatedFolders.firstOrNull()

                val targetFolderId = defaultFolder?.id ?: "default_favorites"
                val updatedItems = currentItems.map { item ->
                    if (item.folderId == folderId) {
                        item.copy(folderId = targetFolderId)
                    } else {
                        item
                    }
                }
                preferences[FavoritesKeys.FAVORITE_ITEMS] = json.encodeToString(updatedItems)
            } else {
                // 删除文件夹中的所有收藏项
                val updatedItems = currentItems.filter { it.folderId != folderId }
                preferences[FavoritesKeys.FAVORITE_ITEMS] = json.encodeToString(updatedItems)
            }
        }
        return true
    }

    /**
     * 添加预设到收藏夹
     */
    suspend fun addToFavorites(
        preset: Preset,
        folderId: String? = null,
        notes: String = ""
    ): Boolean {
        val targetFolderId = folderId ?: defaultFolderId.first()

        // 检查文件夹是否存在
        val currentFolders = folders.first()
        if (currentFolders.none { it.id == targetFolderId }) {
            return false
        }

        context.favoritesDataStore.edit { preferences ->
            val currentItemsJson = preferences[FavoritesKeys.FAVORITE_ITEMS] ?: "[]"
            val currentItems = try {
                json.decodeFromString<MutableList<FavoriteItem>>(currentItemsJson)
            } catch (e: Exception) {
                mutableListOf()
            }

            // 检查是否已存在
            if (currentItems.any { it.presetId == preset.id && it.folderId == targetFolderId }) {
                return@edit
            }

            val newItem = FavoriteItem(
                presetId = preset.id,
                folderId = targetFolderId,
                presetName = preset.name,
                presetCoverUrl = preset.coverUrl,
                deviceModel = preset.deviceModel,
                sortOrder = getNextItemSortOrder(targetFolderId),
                notes = notes
            )

            currentItems.add(newItem)
            preferences[FavoritesKeys.FAVORITE_ITEMS] = json.encodeToString(currentItems)
        }
        return true
    }

    /**
     * 从收藏夹中移除预设
     */
    suspend fun removeFromFavorites(presetId: String, folderId: String? = null): Boolean {
        context.favoritesDataStore.edit { preferences ->
            val currentItemsJson = preferences[FavoritesKeys.FAVORITE_ITEMS] ?: "[]"
            val currentItems = try {
                json.decodeFromString<MutableList<FavoriteItem>>(currentItemsJson)
            } catch (e: Exception) {
                mutableListOf()
            }

            val updatedItems = if (folderId != null) {
                currentItems.filter { !(it.presetId == presetId && it.folderId == folderId) }
            } else {
                currentItems.filter { it.presetId != presetId }
            }

            preferences[FavoritesKeys.FAVORITE_ITEMS] = json.encodeToString(updatedItems)
        }
        return true
    }

    /**
     * 移动收藏项到另一个文件夹
     */
    suspend fun moveToFolder(presetId: String, fromFolderId: String, toFolderId: String): Boolean {
        context.favoritesDataStore.edit { preferences ->
            val currentItemsJson = preferences[FavoritesKeys.FAVORITE_ITEMS] ?: "[]"
            val currentItems = try {
                json.decodeFromString<MutableList<FavoriteItem>>(currentItemsJson)
            } catch (e: Exception) {
                mutableListOf()
            }

            val itemIndex = currentItems.indexOfFirst {
                it.presetId == presetId && it.folderId == fromFolderId
            }

            if (itemIndex != -1) {
                val item = currentItems[itemIndex]
                currentItems[itemIndex] = item.copy(
                    folderId = toFolderId,
                    sortOrder = getNextItemSortOrder(toFolderId)
                )
                preferences[FavoritesKeys.FAVORITE_ITEMS] = json.encodeToString(currentItems)
            }
        }
        return true
    }

    /**
     * 获取文件夹中的收藏项
     */
    suspend fun getItemsInFolder(folderId: String, sortOrder: SortOrder = SortOrder.DATE_ADDED_DESC): List<FavoriteItem> {
        val items = favoriteItems.first().filter { it.folderId == folderId }
        return sortItems(items, sortOrder)
    }

    /**
     * 获取文件夹中的收藏项流
     */
    fun getItemsInFolderFlow(folderId: String, sortOrder: SortOrder = SortOrder.DATE_ADDED_DESC): Flow<List<FavoriteItem>> {
        return favoriteItems.map { items ->
            val folderItems = items.filter { it.folderId == folderId }
            sortItems(folderItems, sortOrder)
        }
    }

    /**
     * 排序收藏项
     */
    private fun sortItems(items: List<FavoriteItem>, sortOrder: SortOrder): List<FavoriteItem> {
        return when (sortOrder) {
            SortOrder.DATE_ADDED_DESC -> items.sortedByDescending { it.addedAt }
            SortOrder.DATE_ADDED_ASC -> items.sortedBy { it.addedAt }
            SortOrder.NAME_ASC -> items.sortedBy { it.presetName }
            SortOrder.NAME_DESC -> items.sortedByDescending { it.presetName }
            SortOrder.CUSTOM -> items.sortedBy { it.sortOrder }
        }
    }

    private suspend fun getNextItemSortOrder(folderId: String): Int {
        val items = favoriteItems.first().filter { it.folderId == folderId }
        return (items.maxOfOrNull { it.sortOrder } ?: 0) + 1
    }

    /**
     * 检查预设是否在收藏夹中
     */
    suspend fun isFavorite(presetId: String, folderId: String? = null): Boolean {
        val items = favoriteItems.first()
        return if (folderId != null) {
            items.any { it.presetId == presetId && it.folderId == folderId }
        } else {
            items.any { it.presetId == presetId }
        }
    }

    /**
     * 获取预设所在的文件夹列表
     */
    suspend fun getPresetFolders(presetId: String): List<FavoriteFolder> {
        val allFolders = folders.first()
        val items = favoriteItems.first().filter { it.presetId == presetId }
        val folderIds = items.map { it.folderId }.toSet()
        return allFolders.filter { it.id in folderIds }
    }

    /**
     * 更新收藏项排序
     */
    suspend fun reorderItems(folderId: String, orderedPresetIds: List<String>): Boolean {
        context.favoritesDataStore.edit { preferences ->
            val currentItemsJson = preferences[FavoritesKeys.FAVORITE_ITEMS] ?: "[]"
            val currentItems = try {
                json.decodeFromString<MutableList<FavoriteItem>>(currentItemsJson)
            } catch (e: Exception) {
                mutableListOf()
            }

            // 更新排序
            orderedPresetIds.forEachIndexed { index, presetId ->
                val itemIndex = currentItems.indexOfFirst {
                    it.presetId == presetId && it.folderId == folderId
                }
                if (itemIndex != -1) {
                    val item = currentItems[itemIndex]
                    currentItems[itemIndex] = item.copy(sortOrder = index)
                }
            }

            preferences[FavoritesKeys.FAVORITE_ITEMS] = json.encodeToString(currentItems)
        }
        return true
    }

    /**
     * 更新收藏夹排序
     */
    suspend fun reorderFolders(orderedFolderIds: List<String>): Boolean {
        context.favoritesDataStore.edit { preferences ->
            val currentFoldersJson = preferences[FavoritesKeys.FAVORITE_FOLDERS] ?: "[]"
            val currentFolders = try {
                json.decodeFromString<MutableList<FavoriteFolder>>(currentFoldersJson)
            } catch (e: Exception) {
                mutableListOf()
            }

            // 更新排序
            orderedFolderIds.forEachIndexed { index, folderId ->
                val folderIndex = currentFolders.indexOfFirst { it.id == folderId }
                if (folderIndex != -1) {
                    val folder = currentFolders[folderIndex]
                    currentFolders[folderIndex] = folder.copy(sortOrder = index)
                }
            }

            preferences[FavoritesKeys.FAVORITE_FOLDERS] = json.encodeToString(currentFolders)
        }
        return true
    }

    /**
     * 更新收藏项备注
     */
    suspend fun updateItemNotes(presetId: String, folderId: String, notes: String): Boolean {
        context.favoritesDataStore.edit { preferences ->
            val currentItemsJson = preferences[FavoritesKeys.FAVORITE_ITEMS] ?: "[]"
            val currentItems = try {
                json.decodeFromString<MutableList<FavoriteItem>>(currentItemsJson)
            } catch (e: Exception) {
                mutableListOf()
            }

            val itemIndex = currentItems.indexOfFirst {
                it.presetId == presetId && it.folderId == folderId
            }

            if (itemIndex != -1) {
                val item = currentItems[itemIndex]
                currentItems[itemIndex] = item.copy(notes = notes)
                preferences[FavoritesKeys.FAVORITE_ITEMS] = json.encodeToString(currentItems)
            }
        }
        return true
    }

    /**
     * 获取所有收藏的预设ID
     */
    suspend fun getAllFavoritePresetIds(): List<String> {
        return favoriteItems.first().map { it.presetId }.distinct()
    }

    /**
     * 获取收藏统计信息
     */
    suspend fun getFavoritesStatistics(): FavoritesStatistics {
        val allFolders = folders.first()
        val allItems = favoriteItems.first()

        return FavoritesStatistics(
            totalFolders = allFolders.size,
            totalFavorites = allItems.size,
            folderStats = allFolders.map { folder ->
                FolderStat(
                    folder = folder,
                    itemCount = allItems.count { it.folderId == folder.id }
                )
            }.sortedByDescending { it.itemCount }
        )
    }

    /**
     * 收藏统计信息
     */
    data class FavoritesStatistics(
        val totalFolders: Int,
        val totalFavorites: Int,
        val folderStats: List<FolderStat>
    )

    /**
     * 文件夹统计
     */
    data class FolderStat(
        val folder: FavoriteFolder,
        val itemCount: Int
    )

    /**
     * 搜索收藏项
     */
    suspend fun searchFavorites(query: String, folderId: String? = null): List<FavoriteItem> {
        if (query.isBlank()) return emptyList()

        val items = favoriteItems.first()
        val filteredItems = if (folderId != null) {
            items.filter { it.folderId == folderId }
        } else {
            items
        }

        return filteredItems.filter {
            it.presetName.contains(query, ignoreCase = true) ||
            it.deviceModel?.contains(query, ignoreCase = true) == true ||
            it.notes.contains(query, ignoreCase = true)
        }
    }

    /**
     * 清空收藏夹
     */
    suspend fun clearFolder(folderId: String): Boolean {
        val folder = folders.first().find { it.id == folderId } ?: return false
        if (folder.isLocked) return false

        context.favoritesDataStore.edit { preferences ->
            val currentItemsJson = preferences[FavoritesKeys.FAVORITE_ITEMS] ?: "[]"
            val currentItems = try {
                json.decodeFromString<MutableList<FavoriteItem>>(currentItemsJson)
            } catch (e: Exception) {
                mutableListOf()
            }

            val updatedItems = currentItems.filter { it.folderId != folderId }
            preferences[FavoritesKeys.FAVORITE_ITEMS] = json.encodeToString(updatedItems)
        }
        return true
    }

    /**
     * 设置默认收藏夹
     */
    suspend fun setDefaultFolder(folderId: String): Boolean {
        val folders = folders.first()
        if (folders.none { it.id == folderId }) return false

        context.favoritesDataStore.edit { preferences ->
            preferences[FavoritesKeys.DEFAULT_FOLDER_ID] = folderId
        }
        return true
    }
}

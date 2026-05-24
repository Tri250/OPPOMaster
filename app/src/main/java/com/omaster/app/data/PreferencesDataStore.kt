package com.omaster.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "omaster_preferences")

@Singleton
class PreferencesDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object PreferencesKeys {
        val FAVORITE_PRESETS = stringSetPreferencesKey("favorite_presets")
    }

    val favoritePresets: Flow<Set<String>> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.FAVORITE_PRESETS] ?: emptySet()
        }

    suspend fun toggleFavorite(presetId: String) {
        context.dataStore.edit { preferences ->
            val currentFavorites = preferences[PreferencesKeys.FAVORITE_PRESETS]?.toMutableSet() ?: mutableSetOf()
            if (currentFavorites.contains(presetId)) {
                currentFavorites.remove(presetId)
            } else {
                currentFavorites.add(presetId)
            }
            preferences[PreferencesKeys.FAVORITE_PRESETS] = currentFavorites
        }
    }
}
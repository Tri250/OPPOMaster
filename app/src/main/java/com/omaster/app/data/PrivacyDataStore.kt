package com.omaster.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.privacyDataStore: DataStore<Preferences> by preferencesDataStore(name = "omaster_privacy")

@Singleton
class PrivacyDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object PrivacyPreferencesKeys {
        val PRIVACY_ACCEPTED = intPreferencesKey("privacy_accepted")
    }

    private fun Preferences.getBoolean(key: Preferences.Key<Int>, defaultValue: Boolean): Boolean {
        val value = this[key]
        return if (value == null) defaultValue else value == 1
    }

    val privacyAccepted: Flow<Boolean> = context.privacyDataStore.data
        .map { preferences ->
            preferences.getBoolean(PrivacyPreferencesKeys.PRIVACY_ACCEPTED, defaultValue = false)
        }

    suspend fun setPrivacyAccepted(accepted: Boolean) {
        context.privacyDataStore.edit { preferences ->
            preferences[PrivacyPreferencesKeys.PRIVACY_ACCEPTED] = if (accepted) 1 else 0
        }
    }
}

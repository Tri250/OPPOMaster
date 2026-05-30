package com.omaster.app.data

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

@Singleton
class SecurePreferencesManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val KEYSTORE_ALIAS = "omaster_secure_key"
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val PREFS_NAME = "omaster_secure_prefs"
    }

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val encryptedPrefs = EncryptedSharedPreferences.create(
        context,
        PREFS_NAME,
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    private object PrefsKeys {
        const val FAVORITE_PRESETS = "favorite_presets"
        const val THEME_MODE = "theme_mode"
        const val FLUID_CLOUD_ENABLED = "fluid_cloud_enabled"
        const val OVERLAY_ENABLED = "overlay_enabled"
    }

    private val _favoritePresetsState = MutableStateFlow<Set<String>>(emptySet())
    val favoritePresetsState: StateFlow<Set<String>> = _favoritePresetsState.asStateFlow()

    init {
        scope.launch {
            _favoritePresetsState.value = getFavoritePresets()
        }
    }

    fun getFavoritePresets(): Set<String> {
        return try {
            encryptedPrefs.getStringSet(PrefsKeys.FAVORITE_PRESETS, emptySet()) ?: emptySet()
        } catch (e: Exception) {
            Timber.e(e, "Error reading favorites")
            emptySet()
        }
    }

    suspend fun toggleFavorite(presetId: String) {
        scope.launch(Dispatchers.IO) {
            try {
                val current = getFavoritePresets().toMutableSet()
                if (current.contains(presetId)) {
                    current.remove(presetId)
                    Timber.d("Removed from favorites: $presetId")
                } else {
                    current.add(presetId)
                    Timber.d("Added to favorites: $presetId")
                }
                encryptedPrefs.edit().putStringSet(PrefsKeys.FAVORITE_PRESETS, current).apply()
                _favoritePresetsState.value = current
            } catch (e: Exception) {
                Timber.e(e, "Error toggling favorite")
            }
        }
    }

    fun getFavoritePresetsIds(): Set<String> {
        return _favoritePresetsState.value
    }

    fun getInt(key: String, defaultValue: Int): Int {
        return try {
            encryptedPrefs.getInt(key, defaultValue)
        } catch (e: Exception) {
            defaultValue
        }
    }

    fun putInt(key: String, value: Int) {
        scope.launch(Dispatchers.IO) {
            try {
                encryptedPrefs.edit().putInt(key, value).apply()
            } catch (e: Exception) {
                Timber.e(e, "Error saving int: $key")
            }
        }
    }

    fun getBoolean(key: String, defaultValue: Boolean): Boolean {
        return try {
            encryptedPrefs.getBoolean(key, defaultValue)
        } catch (e: Exception) {
            defaultValue
        }
    }

    fun putBoolean(key: String, value: Boolean) {
        scope.launch(Dispatchers.IO) {
            try {
                encryptedPrefs.edit().putBoolean(key, value).apply()
            } catch (e: Exception) {
                Timber.e(e, "Error saving boolean: $key")
            }
        }
    }

    fun getString(key: String, defaultValue: String?): String? {
        return try {
            encryptedPrefs.getString(key, defaultValue)
        } catch (e: Exception) {
            defaultValue
        }
    }

    fun putString(key: String, value: String?) {
        scope.launch(Dispatchers.IO) {
            try {
                encryptedPrefs.edit().putString(key, value).apply()
            } catch (e: Exception) {
                Timber.e(e, "Error saving string: $key")
            }
        }
    }

    fun getLong(key: String, defaultValue: Long): Long {
        return try {
            encryptedPrefs.getLong(key, defaultValue)
        } catch (e: Exception) {
            defaultValue
        }
    }

    fun putLong(key: String, value: Long) {
        scope.launch(Dispatchers.IO) {
            try {
                encryptedPrefs.edit().putLong(key, value).apply()
            } catch (e: Exception) {
                Timber.e(e, "Error saving long: $key")
            }
        }
    }

    fun clear() {
        scope.launch(Dispatchers.IO) {
            try {
                encryptedPrefs.edit().clear().apply()
                _favoritePresetsState.value = emptySet()
            } catch (e: Exception) {
                Timber.e(e, "Error clearing prefs")
            }
        }
    }
}

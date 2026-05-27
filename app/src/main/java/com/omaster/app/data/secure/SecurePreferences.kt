package com.omaster.app.data.secure

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SecurePreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val masterKey: MasterKey by lazy {
        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
    }

    private val encryptedPrefs: SharedPreferences by lazy {
        try {
            EncryptedSharedPreferences.create(
                context,
                SECURE_PREFS_FILE_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            Timber.e(e, "Failed to create encrypted SharedPreferences, falling back to standard")
            context.getSharedPreferences(SECURE_PREFS_FILE_NAME, Context.MODE_PRIVATE)
        }
    }

    private val standardPrefs: SharedPreferences by lazy {
        context.getSharedPreferences(STANDARD_PREFS_FILE_NAME, Context.MODE_PRIVATE)
    }

    fun saveFavorite(presetId: String, isFavorite: Boolean) {
        try {
            encryptedPrefs.edit()
                .putBoolean("${KEY_FAVORITE_PREFIX}$presetId", isFavorite)
                .apply()
        } catch (e: Exception) {
            Timber.e(e, "Failed to save favorite for preset: $presetId")
        }
    }

    fun isFavorite(presetId: String): Boolean {
        return try {
            encryptedPrefs.getBoolean("${KEY_FAVORITE_PREFIX}$presetId", false)
        } catch (e: Exception) {
            Timber.e(e, "Failed to get favorite status for preset: $presetId")
            false
        }
    }

    fun getAllFavoriteIds(): Set<String> {
        return try {
            encryptedPrefs.all.keys
                .filter { it.startsWith(KEY_FAVORITE_PREFIX) }
                .map { it.removePrefix(KEY_FAVORITE_PREFIX) }
                .filter { encryptedPrefs.getBoolean("$KEY_FAVORITE_PREFIX$it", false) }
                .toSet()
        } catch (e: Exception) {
            Timber.e(e, "Failed to get all favorite IDs")
            emptySet()
        }
    }

    fun removeFavorite(presetId: String) {
        try {
            encryptedPrefs.edit()
                .remove("${KEY_FAVORITE_PREFIX}$presetId")
                .apply()
        } catch (e: Exception) {
            Timber.e(e, "Failed to remove favorite for preset: $presetId")
        }
    }

    fun saveUserAgreedPrivacy(agreed: Boolean) {
        try {
            encryptedPrefs.edit()
                .putBoolean(KEY_USER_AGREED_PRIVACY, agreed)
                .apply()
        } catch (e: Exception) {
            Timber.e(e, "Failed to save privacy agreement status")
        }
    }

    fun hasUserAgreedPrivacy(): Boolean {
        return try {
            encryptedPrefs.getBoolean(KEY_USER_AGREED_PRIVACY, false)
        } catch (e: Exception) {
            Timber.e(e, "Failed to get privacy agreement status")
            false
        }
    }

    fun saveAnalyticsEnabled(enabled: Boolean) {
        try {
            encryptedPrefs.edit()
                .putBoolean(KEY_ANALYTICS_ENABLED, enabled)
                .apply()
        } catch (e: Exception) {
            Timber.e(e, "Failed to save analytics enabled status")
        }
    }

    fun isAnalyticsEnabled(): Boolean {
        return try {
            encryptedPrefs.getBoolean(KEY_ANALYTICS_ENABLED, true)
        } catch (e: Exception) {
            Timber.e(e, "Failed to get analytics enabled status")
            true
        }
    }

    fun saveMigrationSkipped(skipped: Boolean) {
        try {
            standardPrefs.edit()
                .putBoolean(KEY_MIGRATION_SKIPPED, skipped)
                .apply()
        } catch (e: Exception) {
            Timber.e(e, "Failed to save migration skipped status")
        }
    }

    fun wasMigrationSkipped(): Boolean {
        return try {
            standardPrefs.getBoolean(KEY_MIGRATION_SKIPPED, false)
        } catch (e: Exception) {
            Timber.e(e, "Failed to get migration skipped status")
            false
        }
    }

    fun clearAllSecureData() {
        try {
            encryptedPrefs.edit().clear().apply()
        } catch (e: Exception) {
            Timber.e(e, "Failed to clear secure data")
        }
    }

    fun clearAllStandardData() {
        try {
            standardPrefs.edit().clear().apply()
        } catch (e: Exception) {
            Timber.e(e, "Failed to clear standard data")
        }
    }

    companion object {
        private const val SECURE_PREFS_FILE_NAME = "omaster_secure_prefs"
        private const val STANDARD_PREFS_FILE_NAME = "omaster_standard_prefs"
        private const val KEY_FAVORITE_PREFIX = "favorite_"
        private const val KEY_USER_AGREED_PRIVACY = "user_agreed_privacy"
        private const val KEY_ANALYTICS_ENABLED = "analytics_enabled"
        private const val KEY_MIGRATION_SKIPPED = "migration_skipped"
    }
}

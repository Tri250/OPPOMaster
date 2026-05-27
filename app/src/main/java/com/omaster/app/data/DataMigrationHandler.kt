package com.omaster.app.data

import android.content.Context
import com.omaster.app.data.secure.SecurePreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DataMigrationHandler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val securePreferences: SecurePreferences
) {
    private val currentDataVersion = 2

    sealed class MigrationResult {
        data object Skipped : MigrationResult()
        data object Success : MigrationResult()
        data class Failed(val error: String) : MigrationResult()
    }

    fun needsMigration(): Boolean {
        val savedVersion = getSavedDataVersion()
        return savedVersion < currentDataVersion
    }

    fun shouldShowMigrationDialog(): Boolean {
        return needsMigration() && !securePreferences.wasMigrationSkipped()
    }

    fun markMigrationSkipped() {
        securePreferences.saveMigrationSkipped(true)
        Timber.d("Migration dialog marked as skipped by user")
    }

    fun executeMigration(onComplete: (MigrationResult) -> Unit) {
        Thread {
            try {
                Timber.d("Starting data migration from version ${getSavedDataVersion()} to $currentDataVersion")

                migrateFromV1ToV2()

                saveDataVersion(currentDataVersion)
                securePreferences.saveMigrationSkipped(false)

                Timber.d("Data migration completed successfully")
                onComplete(MigrationResult.Success)
            } catch (e: Exception) {
                Timber.e(e, "Data migration failed")
                onComplete(MigrationResult.Failed(e.message ?: "Unknown error"))
            }
        }.start()
    }

    private fun migrateFromV1ToV2() {
        Timber.d("Executing migration from V1 to V2")
        val prefs = context.getSharedPreferences("omaster_prefs", Context.MODE_PRIVATE)

        prefs.edit().apply {
            putInt("data_version", 2)
            putLong("last_migration_time", System.currentTimeMillis())
            apply()
        }

        Timber.d("V1 to V2 migration logic executed")
    }

    private fun getSavedDataVersion(): Int {
        return try {
            val prefs = context.getSharedPreferences("omaster_prefs", Context.MODE_PRIVATE)
            prefs.getInt("data_version", 1)
        } catch (e: Exception) {
            Timber.e(e, "Failed to get saved data version")
            1
        }
    }

    private fun saveDataVersion(version: Int) {
        try {
            val prefs = context.getSharedPreferences("omaster_prefs", Context.MODE_PRIVATE)
            prefs.edit().putInt("data_version", version).apply()
        } catch (e: Exception) {
            Timber.e(e, "Failed to save data version")
        }
    }

    fun getMigrationInfo(): MigrationInfo {
        return MigrationInfo(
            currentVersion = currentDataVersion,
            savedVersion = getSavedDataVersion(),
            needsMigration = needsMigration(),
            wasSkipped = securePreferences.wasMigrationSkipped()
        )
    }

    data class MigrationInfo(
        val currentVersion: Int,
        val savedVersion: Int,
        val needsMigration: Boolean,
        val wasSkipped: Boolean
    )
}

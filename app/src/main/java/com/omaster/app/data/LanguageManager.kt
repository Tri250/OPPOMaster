package com.omaster.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

private val Context.languageDataStore: DataStore<Preferences> by preferencesDataStore(name = "language_preferences")

/**
 * 支持的语言类型
 */
enum class AppLanguage(
    val code: String,
    val displayName: String,
    val nativeName: String
) {
    SYSTEM("system", "跟随系统", "System"),
    CHINESE("zh", "中文", "中文"),
    ENGLISH("en", "英文", "English"),
    JAPANESE("ja", "日文", "日本語"),
    KOREAN("ko", "韩文", "한국어");

    companion object {
        fun fromCode(code: String): AppLanguage {
            return entries.find { it.code == code } ?: SYSTEM
        }
    }
}

/**
 * 语言管理器
 * 负责应用语言设置的管理和持久化
 */
@Singleton
class LanguageManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object PreferencesKeys {
        val SELECTED_LANGUAGE = stringPreferencesKey("selected_language")
    }

    /**
     * 获取当前设置的语言
     */
    val selectedLanguage: Flow<AppLanguage> = context.languageDataStore.data
        .map { preferences ->
            val code = preferences[PreferencesKeys.SELECTED_LANGUAGE] ?: AppLanguage.SYSTEM.code
            AppLanguage.fromCode(code)
        }

    /**
     * 获取当前语言代码
     */
    val currentLanguageCode: Flow<String> = selectedLanguage
        .map { language ->
            if (language == AppLanguage.SYSTEM) {
                getSystemLanguageCode()
            } else {
                language.code
            }
        }

    /**
     * 获取支持的语言列表
     */
    fun getSupportedLanguages(): List<AppLanguage> {
        return AppLanguage.entries.toList()
    }

    /**
     * 设置应用语言
     * @param language 目标语言
     */
    suspend fun setLanguage(language: AppLanguage) {
        context.languageDataStore.edit { preferences ->
            preferences[PreferencesKeys.SELECTED_LANGUAGE] = language.code
        }
    }

    /**
     * 获取系统语言代码
     */
    fun getSystemLanguageCode(): String {
        val locale = Locale.getDefault()
        return when (locale.language) {
            "zh" -> "zh"
            "en" -> "en"
            "ja" -> "ja"
            "ko" -> "ko"
            else -> "zh" // 默认中文
        }
    }

    /**
     * 检查是否需要重启应用
     * @param newLanguage 新选择的语言
     * @return 是否需要重启
     */
    suspend fun isRestartRequired(newLanguage: AppLanguage): Boolean {
        val current = selectedLanguage.map { it }.toString()
        return current != newLanguage.code
    }

    /**
     * 获取语言对应的Locale
     */
    fun getLocale(language: AppLanguage): Locale {
        return when (language) {
            AppLanguage.SYSTEM -> Locale.getDefault()
            AppLanguage.CHINESE -> Locale.CHINESE
            AppLanguage.ENGLISH -> Locale.ENGLISH
            AppLanguage.JAPANESE -> Locale.JAPANESE
            AppLanguage.KOREAN -> Locale.KOREAN
        }
    }

    /**
     * 获取当前有效的Locale
     */
    suspend fun getCurrentLocale(): Locale {
        val lang = selectedLanguage.map { it }.toString()
        return when (AppLanguage.fromCode(lang)) {
            AppLanguage.SYSTEM -> Locale.getDefault()
            AppLanguage.CHINESE -> Locale.CHINESE
            AppLanguage.ENGLISH -> Locale.ENGLISH
            AppLanguage.JAPANESE -> Locale.JAPANESE
            AppLanguage.KOREAN -> Locale.KOREAN
        }
    }
}

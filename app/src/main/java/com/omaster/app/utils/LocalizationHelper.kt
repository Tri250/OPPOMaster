package com.omaster.app.utils

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Build
import android.os.LocaleList
import androidx.core.os.ConfigurationCompat
import com.omaster.app.data.AppLanguage
import dagger.hilt.android.qualifiers.ApplicationContext
import java.text.DateFormat
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 本地化辅助工具
 * 提供动态语言切换、字符串资源获取、日期时间本地化等功能
 */
@Singleton
class LocalizationHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {

    /**
     * 获取指定语言的 Context
     * @param context 原始 Context
     * @param languageCode 语言代码
     * @return 配置后的 Context
     */
    fun getLocalizedContext(context: Context, languageCode: String): Context {
        val locale = when (languageCode) {
            "zh" -> Locale.CHINESE
            "en" -> Locale.ENGLISH
            "ja" -> Locale.JAPANESE
            "ko" -> Locale.KOREAN
            else -> Locale.getDefault()
        }
        return updateLocale(context, locale)
    }

    /**
     * 更新 Context 的 Locale 配置
     */
    private fun updateLocale(context: Context, locale: Locale): Context {
        Locale.setDefault(locale)

        val resources = context.resources
        val configuration = Configuration(resources.configuration)

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            // Android N (API 24) 及以上使用 LocaleList
            val localeList = LocaleList(locale)
            LocaleList.setDefault(localeList)
            configuration.setLocales(localeList)
            context.createConfigurationContext(configuration)
        } else {
            // 兼容旧版本
            configuration.setLocale(locale)
            @Suppress("DEPRECATION")
            resources.updateConfiguration(configuration, resources.displayMetrics)
            context
        }
    }

    /**
     * 获取字符串资源
     * @param context Context
     * @param resId 字符串资源 ID
     * @param languageCode 语言代码（可选）
     * @return 本地化后的字符串
     */
    fun getString(
        context: Context,
        resId: Int,
        languageCode: String? = null
    ): String {
        return if (languageCode != null) {
            val localizedContext = getLocalizedContext(context, languageCode)
            localizedContext.getString(resId)
        } else {
            context.getString(resId)
        }
    }

    /**
     * 获取带格式参数的字符串资源
     * @param context Context
     * @param resId 字符串资源 ID
     * @param formatArgs 格式参数
     * @param languageCode 语言代码（可选）
     * @return 格式化后的字符串
     */
    fun getString(
        context: Context,
        resId: Int,
        vararg formatArgs: Any,
        languageCode: String? = null
    ): String {
        return if (languageCode != null) {
            val localizedContext = getLocalizedContext(context, languageCode)
            localizedContext.getString(resId, *formatArgs)
        } else {
            context.getString(resId, *formatArgs)
        }
    }

    /**
     * 获取本地化的日期格式器
     * @param locale Locale（可选，默认使用系统）
     * @param style 日期格式样式
     * @return DateFormat 实例
     */
    fun getDateFormat(
        locale: Locale? = null,
        style: Int = DateFormat.MEDIUM
    ): DateFormat {
        return if (locale != null) {
            DateFormat.getDateInstance(style, locale)
        } else {
            DateFormat.getDateInstance(style)
        }
    }

    /**
     * 获取本地化的时间格式器
     * @param locale Locale（可选，默认使用系统）
     * @param style 时间格式样式
     * @return DateFormat 实例
     */
    fun getTimeFormat(
        locale: Locale? = null,
        style: Int = DateFormat.SHORT
    ): DateFormat {
        return if (locale != null) {
            DateFormat.getTimeInstance(style, locale)
        } else {
            DateFormat.getTimeInstance(style)
        }
    }

    /**
     * 获取本地化的日期时间格式器
     * @param locale Locale（可选，默认使用系统）
     * @param dateStyle 日期格式样式
     * @param timeStyle 时间格式样式
     * @return DateFormat 实例
     */
    fun getDateTimeFormat(
        locale: Locale? = null,
        dateStyle: Int = DateFormat.MEDIUM,
        timeStyle: Int = DateFormat.SHORT
    ): DateFormat {
        return if (locale != null) {
            DateFormat.getDateTimeInstance(dateStyle, timeStyle, locale)
        } else {
            DateFormat.getDateTimeInstance(dateStyle, timeStyle)
        }
    }

    /**
     * 获取自定义格式的日期格式器
     * @param pattern 日期格式模式
     * @param locale Locale（可选，默认使用系统）
     * @return SimpleDateFormat 实例
     */
    fun getCustomDateFormat(
        pattern: String,
        locale: Locale? = null
    ): SimpleDateFormat {
        return if (locale != null) {
            SimpleDateFormat(pattern, locale)
        } else {
            SimpleDateFormat(pattern, Locale.getDefault())
        }
    }

    /**
     * 格式化日期
     * @param date 日期
     * @param locale Locale（可选）
     * @param style 格式样式
     * @return 格式化后的日期字符串
     */
    fun formatDate(
        date: Date,
        locale: Locale? = null,
        style: Int = DateFormat.MEDIUM
    ): String {
        return getDateFormat(locale, style).format(date)
    }

    /**
     * 格式化时间
     * @param date 日期时间
     * @param locale Locale（可选）
     * @param style 格式样式
     * @return 格式化后的时间字符串
     */
    fun formatTime(
        date: Date,
        locale: Locale? = null,
        style: Int = DateFormat.SHORT
    ): String {
        return getTimeFormat(locale, style).format(date)
    }

    /**
     * 格式化日期时间
     * @param date 日期时间
     * @param locale Locale（可选）
     * @param dateStyle 日期格式样式
     * @param timeStyle 时间格式样式
     * @return 格式化后的日期时间字符串
     */
    fun formatDateTime(
        date: Date,
        locale: Locale? = null,
        dateStyle: Int = DateFormat.MEDIUM,
        timeStyle: Int = DateFormat.SHORT
    ): String {
        return getDateTimeFormat(locale, dateStyle, timeStyle).format(date)
    }

    /**
     * 获取本地化的数字格式器
     * @param locale Locale（可选，默认使用系统）
     * @return NumberFormat 实例
     */
    fun getNumberFormat(locale: Locale? = null): NumberFormat {
        return if (locale != null) {
            NumberFormat.getNumberInstance(locale)
        } else {
            NumberFormat.getNumberInstance()
        }
    }

    /**
     * 获取本地化的货币格式器
     * @param locale Locale（可选，默认使用系统）
     * @return NumberFormat 实例
     */
    fun getCurrencyFormat(locale: Locale? = null): NumberFormat {
        return if (locale != null) {
            NumberFormat.getCurrencyInstance(locale)
        } else {
            NumberFormat.getCurrencyInstance()
        }
    }

    /**
     * 获取本地化的百分比格式器
     * @param locale Locale（可选，默认使用系统）
     * @return NumberFormat 实例
     */
    fun getPercentFormat(locale: Locale? = null): NumberFormat {
        return if (locale != null) {
            NumberFormat.getPercentInstance(locale)
        } else {
            NumberFormat.getPercentInstance()
        }
    }

    /**
     * 格式化数字
     * @param number 数字
     * @param locale Locale（可选）
     * @return 格式化后的数字字符串
     */
    fun formatNumber(number: Number, locale: Locale? = null): String {
        return getNumberFormat(locale).format(number)
    }

    /**
     * 格式化货币
     * @param amount 金额
     * @param locale Locale（可选）
     * @return 格式化后的货币字符串
     */
    fun formatCurrency(amount: Number, locale: Locale? = null): String {
        return getCurrencyFormat(locale).format(amount)
    }

    /**
     * 格式化百分比
     * @param value 数值（0.0 - 1.0）
     * @param locale Locale（可选）
     * @return 格式化后的百分比字符串
     */
    fun formatPercent(value: Number, locale: Locale? = null): String {
        return getPercentFormat(locale).format(value)
    }

    /**
     * 根据语言代码获取对应的 Locale
     * @param languageCode 语言代码
     * @return Locale 实例
     */
    fun getLocaleFromCode(languageCode: String): Locale {
        return when (languageCode) {
            "zh" -> Locale.CHINESE
            "en" -> Locale.ENGLISH
            "ja" -> Locale.JAPANESE
            "ko" -> Locale.KOREAN
            else -> Locale.getDefault()
        }
    }

    /**
     * 根据 AppLanguage 获取对应的 Locale
     * @param language AppLanguage 枚举
     * @return Locale 实例
     */
    fun getLocaleFromAppLanguage(language: AppLanguage): Locale {
        return when (language) {
            AppLanguage.SYSTEM -> Locale.getDefault()
            AppLanguage.CHINESE -> Locale.CHINESE
            AppLanguage.ENGLISH -> Locale.ENGLISH
            AppLanguage.JAPANESE -> Locale.JAPANESE
            AppLanguage.KOREAN -> Locale.KOREAN
        }
    }

    /**
     * 获取系统当前 Locale
     * @return 系统 Locale
     */
    fun getSystemLocale(): Locale {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            LocaleList.getDefault().get(0)
        } else {
            Locale.getDefault()
        }
    }

    /**
     * 获取当前 Context 的 Locale
     * @param context Context
     * @return Locale 实例
     */
    fun getCurrentLocale(context: Context): Locale {
        return ConfigurationCompat.getLocales(context.resources.configuration)[0]
            ?: Locale.getDefault()
    }

    /**
     * 检查是否需要 RTL 布局
     * @param locale Locale（可选，默认使用当前）
     * @return 是否需要 RTL 布局
     */
    fun isRtlLayout(locale: Locale? = null): Boolean {
        val targetLocale = locale ?: Locale.getDefault()
        val directionality = Character.getDirectionality(targetLocale.displayName[0])
        return directionality == Character.DIRECTIONALITY_RIGHT_TO_LEFT ||
                directionality == Character.DIRECTIONALITY_RIGHT_TO_LEFT_ARABIC
    }

    /**
     * 获取语言显示名称
     * @param languageCode 语言代码
     * @param inLocale 显示名称的目标语言 Locale
     * @return 语言显示名称
     */
    fun getLanguageDisplayName(
        languageCode: String,
        inLocale: Locale? = null
    ): String {
        val locale = getLocaleFromCode(languageCode)
        return if (inLocale != null) {
            locale.getDisplayLanguage(inLocale)
        } else {
            locale.getDisplayLanguage(locale)
        }
    }

    /**
     * 获取国家/地区显示名称
     * @param countryCode 国家代码
     * @param inLocale 显示名称的目标语言 Locale
     * @return 国家/地区显示名称
     */
    fun getCountryDisplayName(
        countryCode: String,
        inLocale: Locale? = null
    ): String {
        val locale = Locale("", countryCode)
        return if (inLocale != null) {
            locale.getDisplayCountry(inLocale)
        } else {
            locale.getDisplayCountry(locale)
        }
    }

    companion object {
        /**
         * 标准日期格式模式
         */
        const val DATE_PATTERN_DEFAULT = "yyyy-MM-dd"
        const val DATE_PATTERN_SHORT = "MM/dd"
        const val DATE_PATTERN_LONG = "yyyy年MM月dd日"
        const val TIME_PATTERN_DEFAULT = "HH:mm"
        const val TIME_PATTERN_WITH_SECONDS = "HH:mm:ss"
        const val DATETIME_PATTERN_DEFAULT = "yyyy-MM-dd HH:mm"
        const val DATETIME_PATTERN_LONG = "yyyy年MM月dd日 HH:mm:ss"

        /**
         * 获取本地化 Context（静态方法）
         */
        @JvmStatic
        fun createLocalizedContext(context: Context, locale: Locale): Context {
            Locale.setDefault(locale)
            val resources = context.resources
            val configuration = Configuration(resources.configuration)

            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                val localeList = LocaleList(locale)
                LocaleList.setDefault(localeList)
                configuration.setLocales(localeList)
                context.createConfigurationContext(configuration)
            } else {
                configuration.setLocale(locale)
                @Suppress("DEPRECATION")
                resources.updateConfiguration(configuration, resources.displayMetrics)
                context
            }
        }

        /**
         * 应用语言设置到 Activity
         */
        @JvmStatic
        fun applyLanguageToActivity(context: Context, languageCode: String) {
            val locale = when (languageCode) {
                "zh" -> Locale.CHINESE
                "en" -> Locale.ENGLISH
                "ja" -> Locale.JAPANESE
                "ko" -> Locale.KOREAN
                else -> Locale.getDefault()
            }

            val resources = context.resources
            val configuration = resources.configuration

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                val localeList = LocaleList(locale)
                configuration.setLocales(localeList)
            } else {
                configuration.setLocale(locale)
            }

            resources.updateConfiguration(configuration, resources.displayMetrics)
        }
    }
}

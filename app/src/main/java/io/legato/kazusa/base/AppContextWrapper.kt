package io.legato.kazusa.base

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Build
import android.os.LocaleList
import io.legato.kazusa.constant.PreferKey
import io.legato.kazusa.utils.getPrefInt
import io.legato.kazusa.utils.getPrefString
import io.legato.kazusa.utils.sysConfiguration
import java.util.*


@Suppress("unused")
object AppContextWrapper {

    fun wrap(context: Context): Context {
        val newConfig = Configuration(context.resources.configuration)
        val targetLocale = getSetLocale(context)
        newConfig.setLocale(targetLocale)
        newConfig.setLocales(LocaleList(targetLocale))
        newConfig.fontScale = getFontScale(context)
        return context.createConfigurationContext(newConfig)
    }

    fun applyLocaleAndFont(activity: Activity) {
        val config = activity.resources.configuration
        val locale = getSetLocale(activity)
        val fontScale = getFontScale(activity)

        val newConfig = Configuration(config)
        newConfig.setLocale(locale)
        newConfig.setLocales(LocaleList(locale))
        newConfig.fontScale = fontScale

        @Suppress("DEPRECATION")
        activity.resources.updateConfiguration(newConfig, activity.resources.displayMetrics)
    }



    fun getFontScale(context: Context): Float {
        var fontScale = context.getPrefInt(PreferKey.fontScale) / 10f
        if (fontScale !in 0.8f..1.6f) {
            fontScale = sysConfiguration.fontScale
        }
        return fontScale
    }

    /**
     * 当前系统语言
     */
    @SuppressLint("ObsoleteSdkInt")
    private fun getSystemLocale(): Locale {
        val locale: Locale
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) { //7.0有多语言设置获取顶部的语言
            locale = sysConfiguration.locales.get(0)
        } else {
            @Suppress("DEPRECATION")
            locale = sysConfiguration.locale
        }
        return locale
    }

    /**
     * 当前App语言
     */
    @SuppressLint("ObsoleteSdkInt")
    private fun getAppLocale(context: Context): Locale {
        val locale: Locale
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            locale = context.resources.configuration.locales[0]
        } else {
            @Suppress("DEPRECATION")
            locale = context.resources.configuration.locale
        }
        return locale

    }

    /**
     * 当前设置语言
     */
    private fun getSetLocale(context: Context): Locale {
        return when (context.getPrefString(PreferKey.language)) {
            "zh" -> Locale.SIMPLIFIED_CHINESE
            "tw" -> Locale.TRADITIONAL_CHINESE
            "en" -> Locale.ENGLISH
            else -> getSystemLocale()
        }
    }

    /**
     * 判断App语言和设置语言是否相同
     */
    fun isSameWithSetting(context: Context): Boolean {
        val locale = getAppLocale(context)
        val language = locale.language
        val country = locale.country
        val pfLocale = getSetLocale(context)
        val pfLanguage = pfLocale.language
        val pfCountry = pfLocale.country
        return language == pfLanguage && country == pfCountry
    }

}
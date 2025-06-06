package io.legato.kazusa.help.config

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.util.DisplayMetrics
import androidx.annotation.Keep
import androidx.appcompat.app.AppCompatDelegate
import io.legato.kazusa.R
import io.legato.kazusa.constant.EventBus
import io.legato.kazusa.constant.PreferKey
import io.legato.kazusa.constant.Theme
import io.legato.kazusa.help.DefaultData
import io.legato.kazusa.lib.theme.ThemeStore
import io.legato.kazusa.model.BookCover
import io.legato.kazusa.utils.BitmapUtils
import io.legato.kazusa.utils.ColorUtils
import io.legato.kazusa.utils.FileUtils
import io.legato.kazusa.utils.GSON
import io.legato.kazusa.utils.fromJsonArray
import io.legato.kazusa.utils.fromJsonObject
import io.legato.kazusa.utils.getCompatColor
import io.legato.kazusa.utils.getPrefInt
import io.legato.kazusa.utils.getPrefString
import io.legato.kazusa.utils.hexString
import io.legato.kazusa.utils.postEvent
import io.legato.kazusa.utils.printOnDebug
import io.legato.kazusa.utils.putPrefInt
import io.legato.kazusa.utils.stackBlur
import splitties.init.appCtx
import java.io.File
import androidx.core.graphics.toColorInt

@Keep
object ThemeConfig {
    const val configFileName = "themeConfig.json"
    val configFilePath = FileUtils.getPath(appCtx.filesDir, configFileName)

    val configList: ArrayList<Config> by lazy {
        val cList = getConfigs() ?: DefaultData.themeConfigs
        ArrayList(cList)
    }

    fun getTheme() = when {
        AppConfig.isNightTheme -> Theme.Dark
        else -> Theme.Light
    }

    fun isDarkTheme(): Boolean {
        return getTheme() == Theme.Dark
    }

    fun applyDayNight(context: Context) {
        applyTheme(context)
        initNightMode()
        BookCover.upDefaultCover()
        postEvent(EventBus.RECREATE, "")
    }

    fun applyDayNightInit(context: Context) {
        applyTheme(context)
        initNightMode()
    }

    private fun initNightMode() {
        val targetMode =
            if (AppConfig.isNightTheme) {
                AppCompatDelegate.MODE_NIGHT_YES
            } else {
                AppCompatDelegate.MODE_NIGHT_NO
            }
        AppCompatDelegate.setDefaultNightMode(targetMode)
    }

    fun getBgImage(context: Context, metrics: DisplayMetrics): Bitmap? {
        val bgCfg = when (getTheme()) {
            Theme.Light -> Pair(
                context.getPrefString(PreferKey.bgImage),
                context.getPrefInt(PreferKey.bgImageBlurring, 0)
            )

            Theme.Dark -> Pair(
                context.getPrefString(PreferKey.bgImageN),
                context.getPrefInt(PreferKey.bgImageNBlurring, 0)
            )

            else -> null
        } ?: return null
        if (bgCfg.first.isNullOrBlank()) return null
        val bgImage = BitmapUtils
            .decodeBitmap(bgCfg.first!!, metrics.widthPixels, metrics.heightPixels)
        if (bgCfg.second == 0) {
            return bgImage
        }
        return bgImage?.stackBlur(bgCfg.second)
    }

    fun upConfig() {
        getConfigs()?.forEach { config ->
            addConfig(config)
        }
    }

    fun save() {
        val json = GSON.toJson(configList)
        FileUtils.delete(configFilePath)
        FileUtils.createFileIfNotExist(configFilePath).writeText(json)
    }

    fun delConfig(index: Int) {
        configList.removeAt(index)
        save()
    }

    fun addConfig(json: String): Boolean {
        GSON.fromJsonObject<Config>(json.trim { it < ' ' }).getOrNull()
            ?.let {
                addConfig(it)
                return true
            }
        return false
    }

    fun addConfig(newConfig: Config) {
        configList.forEachIndexed { index, config ->
            if (newConfig.themeName == config.themeName) {
                configList[index] = newConfig
                return
            }
        }
        configList.add(newConfig)
        save()
    }

    private fun getConfigs(): List<Config>? {
        val configFile = File(configFilePath)
        if (configFile.exists()) {
            kotlin.runCatching {
                val json = configFile.readText()
                return GSON.fromJsonArray<Config>(json).getOrThrow()
            }.onFailure {
                it.printOnDebug()
            }
        }
        return null
    }

    fun applyConfig(context: Context, config: Config) {
        val primary = config.primaryColor.toColorInt()
        val accent = config.accentColor.toColorInt()
        val background = config.backgroundColor.toColorInt()
        val bBackground = config.bottomBackground.toColorInt()
        if (config.isNightTheme) {
            context.putPrefInt(PreferKey.cNPrimary, primary)
            context.putPrefInt(PreferKey.cNAccent, accent)
            context.putPrefInt(PreferKey.cNBackground, background)
            context.putPrefInt(PreferKey.cNBBackground, bBackground)
        } else {
            context.putPrefInt(PreferKey.cPrimary, primary)
            context.putPrefInt(PreferKey.cAccent, accent)
            context.putPrefInt(PreferKey.cBackground, background)
            context.putPrefInt(PreferKey.cBBackground, bBackground)
        }
        AppConfig.isNightTheme = config.isNightTheme
        applyDayNight(context)
    }

    fun saveDayTheme(context: Context, name: String) {
        val primary =
            context.getPrefInt(PreferKey.cPrimary, context.getCompatColor(R.color.md_brown_500))
        val accent =
            context.getPrefInt(PreferKey.cAccent, context.getCompatColor(R.color.md_red_600))
        val background =
            context.getPrefInt(PreferKey.cBackground, context.getCompatColor(R.color.md_grey_100))
        val bBackground =
            context.getPrefInt(PreferKey.cBBackground, context.getCompatColor(R.color.md_grey_200))
        val config = Config(
            themeName = name,
            isNightTheme = false,
            primaryColor = "#${primary.hexString}",
            accentColor = "#${accent.hexString}",
            backgroundColor = "#${background.hexString}",
            bottomBackground = "#${bBackground.hexString}"
        )
        addConfig(config)
    }

    fun saveNightTheme(context: Context, name: String) {
        val primary =
            context.getPrefInt(
                PreferKey.cNPrimary,
                context.getCompatColor(R.color.md_blue_grey_600)
            )
        val accent =
            context.getPrefInt(
                PreferKey.cNAccent,
                context.getCompatColor(R.color.md_deep_orange_800)
            )
        val background =
            context.getPrefInt(PreferKey.cNBackground, context.getCompatColor(R.color.md_grey_900))
        val bBackground =
            context.getPrefInt(PreferKey.cNBBackground, context.getCompatColor(R.color.md_grey_850))
        val config = Config(
            themeName = name,
            isNightTheme = true,
            primaryColor = "#${primary.hexString}",
            accentColor = "#${accent.hexString}",
            backgroundColor = "#${background.hexString}",
            bottomBackground = "#${bBackground.hexString}"
        )
        addConfig(config)
    }

    /**
     * 更新主题
     */
    fun applyTheme(context: Context) = with(context) {

    }

    @Keep
    data class Config(
        var themeName: String,
        var isNightTheme: Boolean,
        var primaryColor: String,
        var accentColor: String,
        var backgroundColor: String,
        var bottomBackground: String
    ) {

        override fun hashCode(): Int {
            return GSON.toJson(this).hashCode()
        }

        override fun equals(other: Any?): Boolean {
            other ?: return false
            if (other is Config) {
                return other.themeName == themeName
                        && other.isNightTheme == isNightTheme
                        && other.primaryColor == primaryColor
                        && other.accentColor == accentColor
                        && other.backgroundColor == backgroundColor
                        && other.bottomBackground == bottomBackground
            }
            return false
        }

    }

}
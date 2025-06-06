package io.legato.kazusa.ui.welcome

import android.content.Intent
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import androidx.core.view.postDelayed
import io.legato.kazusa.base.BaseActivity
import io.legato.kazusa.constant.PreferKey
import io.legato.kazusa.constant.Theme
import io.legato.kazusa.databinding.ActivityWelcomeBinding
import io.legato.kazusa.help.config.ThemeConfig
//import io.legado.app.lib.theme.accentColor
//import io.legado.app.lib.theme.backgroundColor
import io.legato.kazusa.ui.book.read.ReadBookActivity
import io.legato.kazusa.ui.main.MainActivity
import io.legato.kazusa.utils.*
import io.legato.kazusa.utils.viewbindingdelegate.viewBinding

open class WelcomeActivity : BaseActivity<ActivityWelcomeBinding>() {

    override val binding by viewBinding(ActivityWelcomeBinding::inflate)

    override fun onActivityCreated(savedInstanceState: Bundle?) {
//        binding.ivBook.setColorFilter(accentColor)
//        binding.vwTitleLine.setBackgroundColor(accentColor)
        // 避免从桌面启动程序后，会重新实例化入口类的activity
        if (intent.flags and Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT != 0) {
            finish()
        } else {
            binding.root.postDelayed(100) { startMainActivity() }
        }
    }

//    override fun setupSystemBar() {
//        fullScreen()
//        //setStatusBarColorAuto(backgroundColor, true, fullScreen)
//        upNavigationBarColor()
//    }

    override fun upBackgroundImage() {
        if (getPrefBoolean(PreferKey.customWelcome)) {
            kotlin.runCatching {
                when (ThemeConfig.getTheme()) {
                    Theme.Dark -> getPrefString(PreferKey.welcomeImageDark)?.let { path ->
                        val size = windowManager.windowSize
                        BitmapUtils.decodeBitmap(path, size.widthPixels, size.heightPixels).let {
                            binding.tvLegado.visible(getPrefBoolean(PreferKey.welcomeShowTextDark))
                            binding.ivBook.visible(getPrefBoolean(PreferKey.welcomeShowIconDark))
                            binding.tvGzh.visible(getPrefBoolean(PreferKey.welcomeShowTextDark))
                            window.decorView.background = BitmapDrawable(resources, it)
                            return
                        }
                    }
                    else -> getPrefString(PreferKey.welcomeImage)?.let { path ->
                        val size = windowManager.windowSize
                        BitmapUtils.decodeBitmap(path, size.widthPixels, size.heightPixels).let {
                            binding.tvLegado.visible(getPrefBoolean(PreferKey.welcomeShowText))
                            binding.ivBook.visible(getPrefBoolean(PreferKey.welcomeShowIcon))
                            binding.tvGzh.visible(getPrefBoolean(PreferKey.welcomeShowText))
                            window.decorView.background = BitmapDrawable(resources, it)
                            return
                        }
                    }
                }
            }
        }
        super.upBackgroundImage()
    }

    private fun startMainActivity() {
        startActivity<MainActivity>()
        if (getPrefBoolean(PreferKey.defaultToRead)) {
            startActivity<ReadBookActivity>()
        }
        finish()
    }

}

class Launcher1 : WelcomeActivity()
class Launcher2 : WelcomeActivity()
class Launcher3 : WelcomeActivity()
class Launcher4 : WelcomeActivity()
class Launcher5 : WelcomeActivity()
class Launcher6 : WelcomeActivity()
package io.legato.kazusa.lib.theme.view

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import com.google.android.material.bottomnavigation.BottomNavigationView
import io.legato.kazusa.databinding.ViewNavigationBadgeBinding
import io.legato.kazusa.help.config.AppConfig
//import io.legado.app.lib.theme.bottomBackground
//import io.legado.app.lib.theme.getSecondaryTextColor
import io.legato.kazusa.ui.widget.text.BadgeView
import androidx.core.graphics.drawable.toDrawable

class ThemeBottomNavigationVIew(context: Context, attrs: AttributeSet) :
    BottomNavigationView(context, attrs) {

    init {
//        val bgColor = context.bottomBackground
//        setBackgroundColor(bgColor)
//        val textIsDark = ColorUtils.isColorLight(bgColor)
//        val textColor = context.getSecondaryTextColor(textIsDark)
//
//        val colorStateList = Selector.colorBuild()
//            .setDefaultColor(textColor)
//            .setSelectedColor(ThemeStore.accentColor(context)).create()
//        itemIconTintList = colorStateList
//        itemTextColor = colorStateList

        if (AppConfig.isEInkMode) {
            isItemHorizontalTranslationEnabled = false
            itemBackground = Color.TRANSPARENT.toDrawable()
        }

        ViewCompat.setOnApplyWindowInsetsListener(this, null)
    }

    fun addBadgeView(index: Int): BadgeView {
        //获取底部菜单view
        val menuView = getChildAt(0) as ViewGroup
        //获取第index个itemView
        val itemView = menuView.getChildAt(index) as ViewGroup
        val badgeBinding = ViewNavigationBadgeBinding.inflate(LayoutInflater.from(context))
        itemView.addView(badgeBinding.root)
        return badgeBinding.viewBadge
    }

}
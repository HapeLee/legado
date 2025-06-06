package io.legato.kazusa.lib.theme.view

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.TooltipCompat
import com.google.android.material.radiobutton.MaterialRadioButton
import io.legato.kazusa.R
//import io.legado.app.lib.theme.accentColor
//import io.legado.app.lib.theme.bottomBackground
//import io.legado.app.lib.theme.getPrimaryTextColor

class ThemeRadioNoButton(context: Context, attrs: AttributeSet) :
    MaterialRadioButton(context, attrs) {

    private val isBottomBackground: Boolean

    init {
        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.ThemeRadioNoButton)
        isBottomBackground =
            typedArray.getBoolean(R.styleable.ThemeRadioNoButton_isBottomBackground, false)
        typedArray.recycle()
        initTheme()
        TooltipCompat.setTooltipText(this, text)
    }

    private fun initTheme() {
//        when {
//            isInEditMode -> Unit
//            isBottomBackground -> {
//                val accentColor = context.accentColor
//                val isLight = ColorUtils.isColorLight(context.bottomBackground)
//                val textColor = context.getPrimaryTextColor(isLight)
//                val checkedTextColor = if (ColorUtils.isColorLight(accentColor)) {
//                    Color.BLACK
//                } else {
//                    Color.WHITE
//                }
//                background = Selector.shapeBuild()
//                    .setCornerRadius(2.dpToPx())
//                    .setStrokeWidth(2.dpToPx())
//                    .setCheckedBgColor(accentColor)
//                    .setCheckedStrokeColor(accentColor)
//                    .setDefaultStrokeColor(textColor)
//                    .create()
//                setTextColor(
//                    Selector.colorBuild()
//                        .setDefaultColor(textColor)
//                        .setCheckedColor(checkedTextColor)
//                        .create()
//                )
//            }
//            else -> {
//                val accentColor = context.accentColor
//                val defaultTextColor = context.getCompatColor(R.color.primaryText)
//                val checkedTextColor = if (ColorUtils.isColorLight(accentColor)) {
//                    Color.BLACK
//                } else {
//                    Color.WHITE
//                }
//                background = Selector.shapeBuild()
//                    .setCornerRadius(2.dpToPx())
//                    .setStrokeWidth(2.dpToPx())
//                    .setCheckedBgColor(accentColor)
//                    .setCheckedStrokeColor(accentColor)
//                    .setDefaultStrokeColor(defaultTextColor)
//                    .create()
//                setTextColor(
//                    Selector.colorBuild()
//                        .setDefaultColor(defaultTextColor)
//                        .setCheckedColor(checkedTextColor)
//                        .create()
//                )
//            }
//        }

    }

}

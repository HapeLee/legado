package io.legato.kazusa.ui.widget.button

import android.content.Context
import android.util.AttributeSet
import com.google.android.material.button.MaterialButton
//import io.legado.app.lib.theme.bottomBackground

class AccentStrokeButton(context: Context, attrs: AttributeSet) :
    MaterialButton(context, attrs) {

//    private val isBottomBackground: Boolean

//    init {
//        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.AccentStrokeTextView)
//        isBottomBackground =
//            typedArray.getBoolean(R.styleable.StrokeTextView_isBottomBackground, false)
//        typedArray.recycle()
//
//        // MaterialButton specific attributes
//        insetTop = 0
//        insetBottom = 0
//        upStyle()
//    }

//    private fun upStyle() {
//        val isLight = ColorUtils.isColorLight(context.bottomBackground)
//        val disableColor = if (isBottomBackground) {
//            if (isLight) {
//                context.getCompatColor(R.color.md_light_disabled)
//            } else {
//                context.getCompatColor(R.color.md_dark_disabled)
//            }
//        } else {
//            context.getCompatColor(R.color.disabled)
//        }
//        val accentColor = if (isInEditMode) {
//            context.getCompatColor(R.color.accent)
//        } else {
//            ThemeStore.accentColor(context)
//        }
//
//        // Set MaterialButton properties
//        strokeWidth = 1.dpToPx()
//        strokeColor = ColorStateList.valueOf(accentColor)
//
//        // For disabled state
//        val states = arrayOf(
//            intArrayOf(-android.R.attr.state_enabled), // disabled
//            intArrayOf(android.R.attr.state_enabled)  // enabled
//        )
//        val colors = intArrayOf(disableColor, accentColor)
//
//        // Set stroke color state list
//        strokeColor = ColorStateList(states, colors)
//
//        // Set text color state list
//        setTextColor(ColorStateList(states, colors))
//
//        // Set ripple color
//        rippleColor = ColorStateList.valueOf(context.getCompatColor(R.color.transparent30))
//
//        // Remove default background and set transparent background
//        setBackgroundColor(Color.TRANSPARENT)
//    }

}
package io.legato.kazusa.ui.widget.text

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView
import io.legato.kazusa.lib.theme.ThemeStore

/**
 * @author Aidan Follestad (afollestad)
 */
@Suppress("unused")
class PrimaryTextView(context: Context, attrs: AttributeSet) :
    AppCompatTextView(context, attrs) {

    init {
        setTextColor(ThemeStore.textColorPrimary(context))
    }
}

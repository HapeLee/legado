package io.legato.kazusa.ui.widget.text

import android.content.Context
import android.util.AttributeSet
import com.google.android.material.textfield.TextInputLayout
import io.legato.kazusa.lib.theme.Selector
import io.legato.kazusa.lib.theme.ThemeStore

class TextInputLayout(context: Context, attrs: AttributeSet?) : TextInputLayout(context, attrs) {

    init {
        if (!isInEditMode) {
            defaultHintTextColor =
                Selector.colorBuild().setDefaultColor(ThemeStore.accentColor(context)).create()
        }
    }
}

package io.legato.kazusa.ui.book.read.config

import android.content.Context
import android.text.SpannableString
import android.util.AttributeSet
import com.google.android.material.button.MaterialButton
import io.legato.kazusa.R
import io.legato.kazusa.help.config.ReadBookConfig
import io.legato.kazusa.lib.dialogs.alert
//import io.legado.app.lib.theme.accentColor
import io.legato.kazusa.ui.widget.text.StrokeTextView


class TextFontWeightConverter(context: Context, attrs: AttributeSet?) :
    MaterialButton(context, attrs) {

    private val spannableString = SpannableString(context.getString(R.string.font_weight_text))
    private var onChanged: (() -> Unit)? = null

    init {
        text = spannableString
        if (!isInEditMode) {
            upUi(ReadBookConfig.textBold)
        }
        setOnClickListener {
            selectType()
        }
    }

    @Suppress("MemberVisibilityCanBePrivate")
    fun upUi(type: Int) {
        val weightOptions = context.resources.getStringArray(R.array.text_font_weight)
        text = weightOptions.getOrNull(type) ?: ""
    }

    private fun selectType() {
        context.alert(titleResource = R.string.text_font_weight_converter) {
            items(context.resources.getStringArray(R.array.text_font_weight).toList()) { _, i ->
                ReadBookConfig.textBold = i
                upUi(i)
                onChanged?.invoke()
            }
        }
    }

    fun onChanged(unit: () -> Unit) {
        onChanged = unit
    }
}
package io.legato.kazusa.ui.book.read.config

import android.content.Context
import android.text.SpannableString
import android.util.AttributeSet
import com.google.android.material.button.MaterialButton
import io.legato.kazusa.R
import io.legato.kazusa.help.config.AppConfig
import io.legato.kazusa.lib.dialogs.alert
//import io.legado.app.lib.theme.accentColor
import io.legato.kazusa.ui.widget.text.StrokeTextView


class ChineseConverter(context: Context, attrs: AttributeSet?) : MaterialButton(context, attrs) {

    private val spannableString = SpannableString("简/繁")
    private var onChanged: (() -> Unit)? = null

    init {
        text = spannableString
        if (!isInEditMode) {
            upUi(AppConfig.chineseConverterType)
        }
        setOnClickListener {
            selectType()
        }
    }

    private fun upUi(type: Int) {
        val showOptions = context.resources.getStringArray(R.array.chinese_mode)
        text = showOptions.getOrNull(type) ?: ""
    }

    private fun selectType() {
        context.alert(titleResource = R.string.chinese_converter) {
            items(context.resources.getStringArray(R.array.chinese_mode).toList()) { _, i ->
                AppConfig.chineseConverterType = i
                upUi(i)
                onChanged?.invoke()
            }
        }
    }

    fun onChanged(unit: () -> Unit) {
        onChanged = unit
    }
}
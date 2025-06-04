package io.legato.kazusa.ui.widget.text

import android.content.Context
import android.util.AttributeSet
import com.google.android.material.color.MaterialColors
import com.google.android.material.textview.MaterialTextView
import io.legato.kazusa.R
import io.legato.kazusa.lib.theme.Selector
import io.legato.kazusa.utils.ColorUtils
import io.legato.kazusa.utils.dpToPx
import androidx.core.content.withStyledAttributes

class AccentBgTextView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : MaterialTextView(context, attrs) {

    private var radiusPx = 0

    init {
        context.withStyledAttributes(attrs, R.styleable.AccentBgTextView) {
            val radiusDp = getDimensionPixelOffset(R.styleable.AccentBgTextView_radius, 0)
            radiusPx = radiusDp
        }
        updateBackground()
    }

    fun setRadius(dp: Int) {
        radiusPx = dp.dpToPx()
        updateBackground()
    }

    private fun updateBackground() {
        val backgroundColor = MaterialColors.getColor(
            this,
            com.google.android.material.R.attr.colorPrimaryContainer,
            //context.getColorCompat(R.color.fallback_bg)
        )
        val textColor = MaterialColors.getColor(
            this,
            com.google.android.material.R.attr.colorOnPrimaryContainer,
            //context.getColorCompat(R.color.fallback_text)
        )

        background = Selector.shapeBuild()
            .setCornerRadius(radiusPx)
            .setDefaultBgColor(backgroundColor)
            .setPressedBgColor(ColorUtils.darkenColor(backgroundColor))
            .create()

        setTextColor(textColor)
    }
}


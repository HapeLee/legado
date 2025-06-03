package io.legado.app.ui.widget.text

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView
import com.google.android.material.color.MaterialColors
import com.google.android.material.textview.MaterialTextView
import io.legado.app.R
import io.legado.app.lib.theme.Selector
import io.legado.app.lib.theme.ThemeStore
import io.legado.app.utils.ColorUtils
import io.legado.app.utils.dpToPx
import io.legado.app.utils.getCompatColor
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


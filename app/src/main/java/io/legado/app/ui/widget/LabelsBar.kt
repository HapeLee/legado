package io.legado.app.ui.widget

import android.content.Context
import android.os.Build
import android.util.AttributeSet
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.RequiresApi
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import io.legado.app.ui.widget.text.AccentBgTextView
import io.legado.app.utils.dpToPx

@Suppress("unused", "MemberVisibilityCanBePrivate")
class LabelsBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : ChipGroup(context, attrs) {

    private val unUsedChips = arrayListOf<Chip>()
    private val usedChips = arrayListOf<Chip>()
    //var textSizeSp = 12f

    init {
//        isSingleLine = false
//        chipSpacingHorizontal = 8.dpToPx()
//        chipSpacingVertical = 4.dpToPx()
    }

    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    fun setLabels(labels: List<String>) {
        clear()
        labels.forEach {
            addLabel(it)
        }
    }

    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    fun setLabels(labels: Array<String>) {
        setLabels(labels.toList())
    }

    fun clear() {
        unUsedChips.addAll(usedChips)
        usedChips.clear()
        removeAllViews()
    }

    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    fun addLabel(label: String) {
        val chip = if (unUsedChips.isEmpty()) {
            Chip(context).apply {
                isClickable = false
                isCheckable = false
                chipStartPadding = 8f
                chipEndPadding = 8f
                chipStrokeWidth = 0f
                usedChips.add(this)
            }
        } else {
            unUsedChips.removeLast().also { usedChips.add(it) }
        }
        chip.text = label
        addView(chip)
    }
}

package io.legato.kazusa.ui.widget

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Build
import android.util.AttributeSet
import androidx.annotation.RequiresApi
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.color.MaterialColors

@Suppress("unused", "MemberVisibilityCanBePrivate")
class LabelsBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : ChipGroup(context, attrs) {

    private val unUsedChips = arrayListOf<Chip>()
    private val usedChips = arrayListOf<Chip>()


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

    fun addLabel(label: String) {
        val chip = if (unUsedChips.isEmpty()) {
            Chip(context).apply {
                isClickable = false
                isCheckable = false
                chipStartPadding = 8f
                chipEndPadding = 8f
                chipStrokeWidth = 0f

                chipBackgroundColor = ColorStateList.valueOf(
                    MaterialColors.getColor(this, com.google.android.material.R.attr.colorPrimaryContainer, Color.LTGRAY)
                )


                usedChips.add(this)
            }
        } else {
            unUsedChips.removeAt(unUsedChips.lastIndex).also {
                usedChips.add(it)
            }
        }
        chip.text = label
        addView(chip)
    }
}

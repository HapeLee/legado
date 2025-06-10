package io.legato.kazusa.lib.prefs

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.util.AttributeSet
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.preference.PreferenceViewHolder
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import io.legato.kazusa.R
import androidx.core.graphics.toColorInt
import io.legato.kazusa.constant.EventBus
import io.legato.kazusa.utils.postEvent

@SuppressLint("ResourceType")
class ThemeCardPreference(context: Context, attrs: AttributeSet) : Preference(context, attrs) {

    private var entries: Array<CharSequence> = context.resources.getTextArray(R.array.themes_item)
    private var entryValues: Array<CharSequence> = context.resources.getTextArray(R.array.themes_value).takeIf { it.isNotEmpty() }
        ?: arrayOf("0")
    private var currentValue: String? = null

    init {
        layoutResource = R.layout.preference_theme_card
        widgetLayoutResource = R.layout.item_theme_card
    }

    override fun onSetInitialValue(defaultValue: Any?) {
        currentValue = getPersistedString(defaultValue as? String ?: entryValues.getOrNull(0)?.toString())
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)

        val recyclerView = holder.findViewById(R.id.recyclerView) as RecyclerView

        recyclerView.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        recyclerView.adapter = ThemeAdapter()
    }

    private inner class ThemeAdapter : RecyclerView.Adapter<ThemeViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ThemeViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_theme_card, parent, false)
            return ThemeViewHolder(view)
        }

        @SuppressLint("NotifyDataSetChanged")
        override fun onBindViewHolder(holder: ThemeViewHolder, position: Int) {
            val label = entries.getOrNull(position)?.toString() ?: return
            val value = entryValues.getOrNull(position)?.toString() ?: return

            holder.label.text = label
            holder.card.isChecked = (value == currentValue)

            val colors = getThemeColors(value)
            holder.colorTop.setCardBackgroundColor(colors[0])
            holder.colorBook.setCardBackgroundColor(colors[1])
            holder.colorPin.setCardBackgroundColor(colors[2])
            holder.colorBottom.setCardBackgroundColor(colors[3])
            holder.colorBottomLeft.setCardBackgroundColor(colors[4])
            holder.colorBottomRight.setCardBackgroundColor(colors[5])
            holder.background.setCardBackgroundColor(colors[6])
            val isSelected = (value == currentValue)
            holder.background.strokeColor = if (isSelected) colors[4] else colors[7]
            holder.card.checkedIconTint = ColorStateList.valueOf(colors[2])


            holder.card.setOnClickListener {
                if (value != currentValue) {
                    currentValue = value
                    persistString(value)
                    callChangeListener(value)
                    notifyDataSetChanged()
                    postEvent(EventBus.RECREATE, "")
                }
            }
        }


        override fun getItemCount(): Int = entries.size
    }

    private val themeResIdMap = mapOf(
        "0" to R.style.Theme_Base_Dycolors,
        "1" to R.style.Theme_Base_GR,
        "2" to R.style.Theme_Base_Lemon,
        "3" to R.style.Theme_Base_WH,
        )

    private fun getThemeColors(value: String): List<Int> {
        val themeResId = themeResIdMap[value] ?: return listOf(Color.GRAY, Color.GRAY, Color.GRAY, Color.GRAY)

        val themedContext = ContextThemeWrapper(context, themeResId)
        val attrs = intArrayOf(
            com.google.android.material.R.attr.colorOnSurface,
            com.google.android.material.R.attr.colorSecondaryContainer,
            com.google.android.material.R.attr.colorSecondaryVariant,
            com.google.android.material.R.attr.colorSurfaceContainer,
            com.google.android.material.R.attr.colorPrimary,
            com.google.android.material.R.attr.colorOnSurfaceVariant,
            com.google.android.material.R.attr.colorSurface,
            com.google.android.material.R.attr.colorSecondaryContainer,
        )

        val ta = themedContext.obtainStyledAttributes(attrs)
        return List(attrs.size) { ta.getColor(it, Color.GRAY) }.also { ta.recycle() }
    }



    class ThemeViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val card: MaterialCardView = view.findViewById(R.id.cardView)
        val label: TextView = view.findViewById(R.id.themeLabel)
        val colorTop: MaterialCardView = view.findViewById(R.id.cv_title)
        val colorBook: MaterialCardView = view.findViewById(R.id.cv_book)
        val colorPin: MaterialCardView = view.findViewById(R.id.cv_pin)
        val colorBottom: MaterialCardView = view.findViewById(R.id.cv_bottom)
        val colorBottomRight: MaterialCardView = view.findViewById(R.id.right_rect)
        val colorBottomLeft : MaterialCardView = view.findViewById(R.id.left_circle)
        val background : MaterialCardView = view.findViewById(R.id.cardView)
    }
}



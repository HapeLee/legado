package io.legato.kazusa.lib.prefs

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.preference.PreferenceViewHolder
import com.google.android.material.button.MaterialButtonToggleGroup
import io.legato.kazusa.R


class ThemeModePreference(context: Context, attrs: AttributeSet) : Preference(context, attrs) {

    private var currentValue: String? = null

    init {
        layoutResource = R.layout.view_pref
        widgetLayoutResource = R.layout.view_theme_mode
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)

        val toggleGroup = holder.itemView.findViewById<View>(R.id.theme_toggle_group)
        if (toggleGroup == null) {
            val rootView = holder.itemView as ViewGroup
            val inflater = LayoutInflater.from(context)
            val themeToggleView = inflater.inflate(R.layout.view_theme_mode, rootView, false)
            rootView.addView(themeToggleView)

            setupToggleGroup(themeToggleView.findViewById(R.id.theme_toggle_group))
        }
    }

    private fun setupToggleGroup(toggleGroup: MaterialButtonToggleGroup) {

        when (currentValue) {
            "0" -> toggleGroup.check(R.id.btn_system)
            "1" -> toggleGroup.check(R.id.btn_light)
            "2" -> toggleGroup.check(R.id.btn_dark)
        }

        // 监听选择变化
        toggleGroup.addOnButtonCheckedListener { group, checkedId, isChecked ->
            if (isChecked) {
                val newValue = when (checkedId) {
                    R.id.btn_system -> "0"
                    R.id.btn_light -> "1"
                    R.id.btn_dark -> "2"
                    else -> null
                }

                if (newValue != null && callChangeListener(newValue)) {
                    currentValue = newValue
                    persistString(newValue)
                }
            }
        }
    }

    override fun onSetInitialValue(defaultValue: Any?) {
        currentValue = getPersistedString(defaultValue as? String ?: "0")
    }

    override fun shouldDisableDependents(): Boolean {
        return currentValue == null || super.shouldDisableDependents()
    }
}
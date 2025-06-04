package io.legato.kazusa.ui.book.audio

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupWindow
import io.legato.kazusa.R
import io.legato.kazusa.databinding.PopupSeekBarBinding
import io.legato.kazusa.model.AudioPlay
import io.legato.kazusa.service.AudioPlayService

class TimerSliderPopup(private val context: Context) :
    PopupWindow(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT) {

    private val binding = PopupSeekBarBinding.inflate(LayoutInflater.from(context))

    init {
        contentView = binding.root

        isTouchable = true
        isOutsideTouchable = false
        isFocusable = true

        // 初始化 slider 值
        binding.slider.valueFrom = 1f
        binding.slider.valueTo = 180f
        binding.slider.stepSize = 1f
        binding.slider.value = AudioPlayService.timeMinute.toFloat()
        setSliderTextValue(AudioPlayService.timeMinute)

        binding.slider.addOnChangeListener { _, value, fromUser ->
            setSliderTextValue(value.toInt())
            if (fromUser) {
                AudioPlay.setTimer(value.toInt())
            }
        }
    }

    override fun showAsDropDown(anchor: View?, xoff: Int, yoff: Int, gravity: Int) {
        super.showAsDropDown(anchor, xoff, yoff, gravity)
        binding.slider.value = AudioPlayService.timeMinute.toFloat()
    }

    override fun showAtLocation(parent: View?, gravity: Int, x: Int, y: Int) {
        super.showAtLocation(parent, gravity, x, y)
        binding.slider.value = AudioPlayService.timeMinute.toFloat()
    }

    private fun setSliderTextValue(value: Int) {
        binding.tvSeekValue.text = context.getString(R.string.timer_m, value)
    }
}

package io.legato.kazusa.ui.widget

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import android.widget.SeekBar
import androidx.appcompat.widget.TooltipCompat
import com.google.android.material.slider.Slider
import io.legato.kazusa.R
import io.legato.kazusa.databinding.ViewDetailSeekBarBinding
//import io.legado.app.lib.theme.bottomBackground
//import io.legado.app.lib.theme.getPrimaryTextColor
import io.legato.kazusa.ui.widget.seekbar.SeekBarChangeListener


class DetailSeekBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : FrameLayout(context, attrs),
    SeekBarChangeListener {
    private var binding: ViewDetailSeekBarBinding =
        ViewDetailSeekBarBinding.inflate(LayoutInflater.from(context), this, true)
    private val isBottomBackground: Boolean

    var valueFormat: ((progress: Int) -> String)? = null
    var onChanged: ((progress: Int) -> Unit)? = null

    var progress: Int
        get() = binding.slider.value.toInt()
        set(value) {
            binding.slider.value = value.toFloat()
            upValue()
        }

    var max: Int
        get() = binding.slider.valueTo.toInt()
        set(value) {
            binding.slider.valueTo = value.toFloat()
        }

    init {
        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.DetailSeekBar)
        isBottomBackground =
            typedArray.getBoolean(R.styleable.DetailSeekBar_isBottomBackground, false)
        val title = typedArray.getText(R.styleable.DetailSeekBar_title)
        binding.tvSeekTitle.apply {
            text = title
            TooltipCompat.setTooltipText(this, title)
        }
        binding.slider.valueTo = typedArray.getInteger(R.styleable.DetailSeekBar_max, 0).toFloat()
        typedArray.recycle()
        if (isBottomBackground && !isInEditMode) {
//            val isLight = ColorUtils.isColorLight(context.bottomBackground)
//            val textColor = context.getPrimaryTextColor(isLight)
//            binding.tvSeekTitle.setTextColor(textColor)
//            binding.ivSeekPlus.setColorFilter(textColor, PorterDuff.Mode.SRC_IN)
//            binding.ivSeekReduce.setColorFilter(textColor, PorterDuff.Mode.SRC_IN)
//            binding.tvSeekValue.setTextColor(textColor)
        }
        binding.ivSeekPlus.setOnClickListener {
            binding.slider.value = binding.slider.value + 1
            onChanged?.invoke(binding.slider.value.toInt())
        }

        binding.ivSeekReduce.setOnClickListener {
            binding.slider.value = binding.slider.value - 1
            onChanged?.invoke(binding.slider.value.toInt())
        }

        binding.slider.addOnChangeListener { _, value, fromUser ->
            upValue(value.toInt())
        }

        binding.slider.addOnSliderTouchListener(object : Slider.OnSliderTouchListener {
            override fun onStartTrackingTouch(slider: Slider) {
                // 可以添加开始拖动时的逻辑
            }

            override fun onStopTrackingTouch(slider: Slider) {
                onChanged?.invoke(slider.value.toInt())
            }
        })
    }

    private fun upValue(progress: Int = binding.slider.value.toInt()) {
        valueFormat?.let {
            binding.tvSeekValue.text = it.invoke(progress)
        } ?: let {
            binding.tvSeekValue.text = progress.toString()
        }
    }

    override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
        upValue(progress)
    }

//    override fun onStartTrackingTouch(seekBar: SeekBar) {
//
//    }
//
//    override fun onStopTrackingTouch(seekBar: SeekBar) {
//        onChanged?.invoke(binding.slider.progress)
//    }
}
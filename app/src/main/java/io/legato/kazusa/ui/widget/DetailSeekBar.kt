package io.legato.kazusa.ui.widget

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.MotionEvent
import android.widget.FrameLayout
import android.widget.SeekBar
import androidx.annotation.RequiresApi
import androidx.appcompat.widget.TooltipCompat
import com.google.android.material.slider.Slider
import io.legato.kazusa.R
import io.legato.kazusa.databinding.ViewDetailSeekBarBinding
//import io.legado.app.lib.theme.bottomBackground
//import io.legado.app.lib.theme.getPrimaryTextColor
import io.legato.kazusa.ui.widget.seekbar.SeekBarChangeListener
import kotlin.math.abs


@RequiresApi(Build.VERSION_CODES.O)
@SuppressLint("ClickableViewAccessibility")
class DetailSeekBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : FrameLayout(context, attrs),
    SeekBarChangeListener {
    private var binding: ViewDetailSeekBarBinding =
        ViewDetailSeekBarBinding.inflate(LayoutInflater.from(context), this, true)
    private val isBottomBackground: Boolean

    var valueFormat: ((progress: Int) -> String)? = null
    var onStartTracking: (() -> Unit)? = null
    var onChanged: ((progress: Int) -> Unit)? = null
    var onStopTracking: (() -> Unit)? = null

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
        binding.ivSeekPlus.setOnClickListener {
            val newValue = (binding.slider.value + 1).coerceAtMost(binding.slider.valueTo)
            binding.slider.value = newValue
            onChanged?.invoke(newValue.toInt())
        }

        binding.ivSeekReduce.setOnClickListener {
            val newValue = (binding.slider.value - 1).coerceAtLeast(binding.slider.valueFrom)
            binding.slider.value = newValue
            onChanged?.invoke(newValue.toInt())
        }

        binding.slider.addOnChangeListener { _, value, fromUser ->
            upValue(value.toInt())

            if (fromUser) {
                onChanged?.invoke(value.toInt())
            }
        }

        binding.slider.addOnSliderTouchListener(object : Slider.OnSliderTouchListener {
            override fun onStartTrackingTouch(slider: Slider) {
                onStartTracking?.invoke()
            }

            override fun onStopTrackingTouch(slider: Slider) {
                onStopTracking?.invoke()
                onChanged?.invoke(slider.value.toInt())
            }
        })

        binding.slider.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                val slider = binding.slider
                val sliderWidth = slider.width - slider.paddingStart - slider.paddingEnd
                val touchX = event.x - slider.paddingStart

                val proportion = (slider.value - slider.valueFrom) / (slider.valueTo - slider.valueFrom)
                val thumbCenterX = proportion * sliderWidth

                val thumbRadiusPx = slider.thumbRadius * 2f  // 可适当扩大滑块判断区域

                val isTouchNearThumb = kotlin.math.abs(touchX - thumbCenterX) <= thumbRadiusPx
                return@setOnTouchListener !isTouchNearThumb // 拦截不是点在滑块上的点击
            }
            return@setOnTouchListener false
        }

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

}
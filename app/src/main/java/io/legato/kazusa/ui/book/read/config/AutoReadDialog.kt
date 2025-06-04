package io.legato.kazusa.ui.book.read.config

import android.content.DialogInterface
import android.os.Bundle
import android.view.View
import com.google.android.material.slider.Slider
import io.legato.kazusa.R
import io.legato.kazusa.base.BaseBottomSheetDialogFragment
import io.legato.kazusa.databinding.DialogAutoReadBinding
import io.legato.kazusa.help.config.ReadBookConfig
//import io.legado.app.lib.theme.bottomBackground
//import io.legado.app.lib.theme.getPrimaryTextColor
import io.legato.kazusa.model.ReadAloud
import io.legato.kazusa.model.ReadBook
import io.legato.kazusa.service.BaseReadAloudService
import io.legato.kazusa.ui.book.read.BaseReadBookActivity
import io.legato.kazusa.ui.book.read.ReadBookActivity
import io.legato.kazusa.utils.viewbindingdelegate.viewBinding
import java.util.Locale


class AutoReadDialog : BaseBottomSheetDialogFragment(R.layout.dialog_auto_read) {

    private val binding by viewBinding(DialogAutoReadBinding::bind)
    private val callBack: CallBack? get() = activity as? CallBack

    override fun onStart() {
        super.onStart()
//        dialog?.window?.run {
//            clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
//            setBackgroundDrawableResource(R.color.background)
//            decorView.setPadding(0, 0, 0, 0)
//            val attr = attributes
//            attr.dimAmount = 0.0f
//            attr.gravity = Gravity.BOTTOM
//            attributes = attr
//            setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
//        }
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        (activity as ReadBookActivity).bottomDialog--
    }

    override fun onFragmentCreated(view: View, savedInstanceState: Bundle?) = binding.run {
        val bottomDialog = (activity as ReadBookActivity).bottomDialog++
        if (bottomDialog > 0) {
            dismiss()
            return
        }
        //val bg = requireContext().bottomBackground
        //val isLight = ColorUtils.isColorLight(bg)
        //val textColor = requireContext().getPrimaryTextColor(isLight)
        //root.setBackgroundColor(bg)
//        tvReadSpeedTitle.setTextColor(textColor)
//        tvReadSpeed.setTextColor(textColor)
//        ivCatalog.setColorFilter(textColor, PorterDuff.Mode.SRC_IN)
//        tvCatalog.setTextColor(textColor)
//        ivMainMenu.setColorFilter(textColor, PorterDuff.Mode.SRC_IN)
//        tvMainMenu.setTextColor(textColor)
//        ivAutoPageStop.setColorFilter(textColor, PorterDuff.Mode.SRC_IN)
//        tvAutoPageStop.setTextColor(textColor)
//        ivSetting.setColorFilter(textColor, PorterDuff.Mode.SRC_IN)
//        tvSetting.setTextColor(textColor)
        initOnChange()
        initData()
        initEvent()
    }

    private fun initData() {
        val speed = if (ReadBookConfig.autoReadSpeed < 1) 1 else ReadBookConfig.autoReadSpeed
        binding.tvReadSpeed.text = String.format(Locale.ROOT, "%ds", speed)
        binding.seekAutoRead.value = speed.toFloat()
    }

    private fun initOnChange() {
        binding.seekAutoRead.addOnChangeListener { slider, value, fromUser ->
            val speed = if (value < 1) 1 else value.toInt()
            binding.tvReadSpeed.text = String.format(Locale.ROOT, "%ds", speed)
        }

        binding.seekAutoRead.addOnSliderTouchListener(object : Slider.OnSliderTouchListener {
            override fun onStartTrackingTouch(slider: Slider) {

            }

            override fun onStopTrackingTouch(slider: Slider) {
                ReadBookConfig.autoReadSpeed = if (slider.value < 1) 1 else slider.value.toInt()
                upTtsSpeechRate()
            }
        })
    }

    private fun initEvent() {
        binding.btnMainMenu.setOnClickListener {
            callBack?.showMenuBar()
            dismissAllowingStateLoss()
        }
        binding.btnSetting.setOnClickListener {
            (activity as BaseReadBookActivity).showPageAnimConfig {
                (activity as ReadBookActivity).upPageAnim()
                ReadBook.loadContent(false)
            }
        }
        binding.btnCatalog.setOnClickListener { callBack?.openChapterList() }
        binding.btnAutoPageStop.setOnClickListener {
            callBack?.autoPageStop()
            binding.btnAutoPageStop.post {
                dismissAllowingStateLoss()
            }
        }
    }

    private fun upTtsSpeechRate() {
        ReadAloud.upTtsSpeechRate(requireContext())
        if (!BaseReadAloudService.pause) {
            ReadAloud.pause(requireContext())
            ReadAloud.resume(requireContext())
        }
    }

    interface CallBack {
        fun showMenuBar()
        fun openChapterList()
        fun autoPageStop()
    }
}
package io.legato.kazusa.ui.book.manga.config

import android.content.DialogInterface
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import io.legato.kazusa.R
import io.legato.kazusa.base.BaseDialogFragment
import io.legato.kazusa.databinding.DialogMangaColorFilterBinding
import io.legato.kazusa.help.config.AppConfig
import io.legato.kazusa.utils.GSON
import io.legato.kazusa.utils.fromJsonObject
import io.legato.kazusa.utils.setLayout
import io.legato.kazusa.utils.viewbindingdelegate.viewBinding

class MangaColorFilterDialog : BaseDialogFragment(R.layout.dialog_manga_color_filter) {
    private val binding by viewBinding(DialogMangaColorFilterBinding::bind)
    private val mConfig =
        GSON.fromJsonObject<MangaColorFilterConfig>(AppConfig.mangaColorFilter).getOrNull()
            ?: MangaColorFilterConfig()
    private val callback get() = activity as? Callback

    override fun onStart() {
        super.onStart()
        dialog?.window?.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    override fun onFragmentCreated(view: View, savedInstanceState: Bundle?) {
        initData()
        initView()
    }

    private fun initData() {
        binding.run {
            dsbBrightness.progress = mConfig.l
            dsbR.progress = mConfig.r
            dsbG.progress = mConfig.g
            dsbB.progress = mConfig.b
            dsbA.progress = mConfig.a
        }
    }

    private fun initView() {
        binding.run {
            dsbBrightness.onChanged = {
                mConfig.l = it
                callback?.updateColorFilter(mConfig)
            }
            dsbR.onChanged = {
                mConfig.r = it
                callback?.updateColorFilter(mConfig)
            }
            dsbG.onChanged = {
                mConfig.g = it
                callback?.updateColorFilter(mConfig)
            }
            dsbB.onChanged = {
                mConfig.b = it
                callback?.updateColorFilter(mConfig)
            }
            dsbA.onChanged = {
                mConfig.a = it
                callback?.updateColorFilter(mConfig)
            }
        }
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        AppConfig.mangaColorFilter = mConfig.toJson()
    }

    interface Callback {
        fun updateColorFilter(config: MangaColorFilterConfig)
    }

}
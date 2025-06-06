package io.legato.kazusa.ui.config

import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import io.legato.kazusa.R
import io.legato.kazusa.base.BaseBottomSheetDialogFragment
import io.legato.kazusa.base.BaseDialogFragment
import io.legato.kazusa.databinding.DialogCoverRuleConfigBinding
//import io.legado.app.lib.theme.primaryColor
import io.legato.kazusa.model.BookCover
import io.legato.kazusa.utils.GSON
import io.legato.kazusa.utils.setLayout
import io.legato.kazusa.utils.toastOnUi
import io.legato.kazusa.utils.viewbindingdelegate.viewBinding
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import splitties.views.onClick

class CoverRuleConfigDialog : BaseBottomSheetDialogFragment(R.layout.dialog_cover_rule_config) {

    val binding by viewBinding(DialogCoverRuleConfigBinding::bind)

    override fun onStart() {
        super.onStart()

    }

    override fun onFragmentCreated(view: View, savedInstanceState: Bundle?) {
        //binding.toolBar.setBackgroundColor(primaryColor)
        initData()
        binding.tvCancel.onClick {
            dismissAllowingStateLoss()
        }
        binding.tvOk.onClick {
            val enable = binding.cbEnable.isChecked
            val searchUrl = binding.editSearchUrl.text?.toString()
            val coverRule = binding.editCoverUrlRule.text?.toString()
            if (searchUrl.isNullOrBlank() || coverRule.isNullOrBlank()) {
                toastOnUi("搜索url和cover规则不能为空")
            } else {
                BookCover.CoverRule(enable, searchUrl, coverRule).let { config ->
                    BookCover.saveCoverRule(config)
                }
                dismissAllowingStateLoss()
            }
        }
        binding.tvFooterLeft.onClick {
            BookCover.delCoverRule()
            dismissAllowingStateLoss()
        }
    }

    private fun initData() {
        lifecycleScope.launch {
            val rule = withContext(IO) {
                BookCover.getCoverRule()
            }
            Log.e("coverRule", GSON.toJson(rule))
            binding.cbEnable.isChecked = rule.enable
            binding.editSearchUrl.setText(rule.searchUrl)
            binding.editCoverUrlRule.setText(rule.coverRule)
        }
    }

}
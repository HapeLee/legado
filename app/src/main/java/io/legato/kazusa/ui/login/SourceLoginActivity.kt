package io.legato.kazusa.ui.login

import android.os.Bundle
import androidx.activity.viewModels
import io.legato.kazusa.R
import io.legato.kazusa.base.VMBaseActivity
import io.legato.kazusa.data.entities.BaseSource
import io.legato.kazusa.databinding.ActivitySourceLoginBinding
import io.legato.kazusa.utils.showDialogFragment
import io.legato.kazusa.utils.viewbindingdelegate.viewBinding


class SourceLoginActivity : VMBaseActivity<ActivitySourceLoginBinding, SourceLoginViewModel>() {

    override val binding by viewBinding(ActivitySourceLoginBinding::inflate)
    override val viewModel by viewModels<SourceLoginViewModel>()

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        viewModel.initData(intent) { source ->
            initView(source)
        }
    }

    private fun initView(source: BaseSource) {
        if (source.loginUi.isNullOrEmpty()) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fl_fragment, WebViewLoginFragment(), "webViewLogin")
                .commit()
        } else {
            showDialogFragment<SourceLoginDialog>()
        }
    }

}
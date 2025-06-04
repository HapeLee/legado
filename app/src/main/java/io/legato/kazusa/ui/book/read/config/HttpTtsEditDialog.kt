package io.legato.kazusa.ui.book.read.config

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.viewModels
import io.legato.kazusa.R
import io.legato.kazusa.base.BaseDialogFragment
import io.legato.kazusa.data.entities.HttpTTS
import io.legato.kazusa.databinding.DialogHttpTtsEditBinding
import io.legato.kazusa.lib.dialogs.alert
//import io.legado.app.lib.theme.primaryColor
import io.legato.kazusa.ui.about.AppLogDialog
import io.legato.kazusa.ui.login.SourceLoginActivity
import io.legato.kazusa.ui.widget.code.addJsPattern
import io.legato.kazusa.ui.widget.code.addJsonPattern
import io.legato.kazusa.ui.widget.code.addLegadoPattern
import io.legato.kazusa.utils.GSON
import io.legato.kazusa.utils.applyTint
import io.legato.kazusa.utils.sendToClip
import io.legato.kazusa.utils.setLayout
import io.legato.kazusa.utils.showDialogFragment
import io.legato.kazusa.utils.showHelp
import io.legato.kazusa.utils.startActivity
import io.legato.kazusa.utils.toastOnUi
import io.legato.kazusa.utils.viewbindingdelegate.viewBinding

class HttpTtsEditDialog() : BaseDialogFragment(R.layout.dialog_http_tts_edit, true),
    Toolbar.OnMenuItemClickListener {

    constructor(id: Long) : this() {
        arguments = Bundle().apply {
            putLong("id", id)
        }
    }

    private val binding by viewBinding(DialogHttpTtsEditBinding::bind)
    private val viewModel by viewModels<HttpTtsEditViewModel>()

    override fun onStart() {
        super.onStart()
        setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
    }

    override fun onFragmentCreated(view: View, savedInstanceState: Bundle?) {
        //binding.toolBar.setBackgroundColor(primaryColor)
        binding.tvUrl.run {
            addLegadoPattern()
            addJsonPattern()
            addJsPattern()
        }
        binding.tvLoginUrl.run {
            addLegadoPattern()
            addJsonPattern()
            addJsPattern()
        }
        binding.tvLoginUi.addJsonPattern()
        binding.tvLoginCheckJs.addJsPattern()
        binding.tvHeaders.run {
            addLegadoPattern()
            addJsonPattern()
            addJsPattern()
        }
        viewModel.initData(arguments) {
            initView(httpTTS = it)
        }
        initMenu()
    }

    fun initMenu() {
        binding.toolBar.inflateMenu(R.menu.speak_engine_edit)
        binding.toolBar.menu.applyTint(requireContext())
        binding.toolBar.setOnMenuItemClickListener(this)
    }

    fun initView(httpTTS: HttpTTS) {
        binding.tvName.setText(httpTTS.name)
        binding.tvUrl.setText(httpTTS.url)
        binding.tvContentType.setText(httpTTS.contentType)
        binding.tvConcurrentRate.setText(httpTTS.concurrentRate)
        binding.tvLoginUrl.setText(httpTTS.loginUrl)
        binding.tvLoginUi.setText(httpTTS.loginUi)
        binding.tvLoginCheckJs.setText(httpTTS.loginCheckJs)
        binding.tvHeaders.setText(httpTTS.header)
    }

    override fun onMenuItemClick(item: MenuItem?): Boolean {
        when (item?.itemId) {
            R.id.menu_save -> viewModel.save(dataFromView()) {
                toastOnUi("保存成功")
            }
            R.id.menu_login -> dataFromView().let { httpTts ->
                if (httpTts.loginUrl.isNullOrBlank()) {
                    toastOnUi("登录url不能为空")
                } else {
                    viewModel.save(httpTts) {
                        startActivity<SourceLoginActivity> {
                            putExtra("type", "httpTts")
                            putExtra("key", httpTts.id.toString())
                        }
                    }
                }
            }
            R.id.menu_show_login_header -> alert {
                setTitle(R.string.login_header)
                dataFromView().getLoginHeader()?.let { loginHeader ->
                    setMessage(loginHeader)
                }
            }
            R.id.menu_del_login_header -> dataFromView().removeLoginHeader()
            R.id.menu_copy_source -> dataFromView().let {
                context?.sendToClip(GSON.toJson(it))
            }
            R.id.menu_paste_source -> viewModel.importFromClip {
                initView(it)
            }
            R.id.menu_log -> showDialogFragment<AppLogDialog>()
            R.id.menu_help -> showHelp("httpTTSHelp")
        }
        return true
    }

    private fun dataFromView(): HttpTTS {
        return HttpTTS(
            id = viewModel.id ?: System.currentTimeMillis(),
            name = binding.tvName.text.toString(),
            url = binding.tvUrl.text.toString(),
            contentType = binding.tvContentType.text?.toString(),
            concurrentRate = binding.tvConcurrentRate.text?.toString(),
            loginUrl = binding.tvLoginUrl.text?.toString(),
            loginUi = binding.tvLoginUi.text?.toString(),
            loginCheckJs = binding.tvLoginCheckJs.text?.toString(),
            header = binding.tvHeaders.text?.toString()
        )
    }

}
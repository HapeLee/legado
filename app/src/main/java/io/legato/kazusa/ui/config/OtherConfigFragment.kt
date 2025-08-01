package io.legato.kazusa.ui.config

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import androidx.core.view.postDelayed
import androidx.fragment.app.activityViewModels
import androidx.preference.ListPreference
import androidx.preference.Preference
import com.jeremyliao.liveeventbus.LiveEventBus
import io.legato.kazusa.R
import io.legato.kazusa.constant.EventBus
import io.legato.kazusa.constant.PreferKey
import io.legato.kazusa.databinding.DialogEditTextBinding
import io.legato.kazusa.help.AppFreezeMonitor
import io.legato.kazusa.help.config.AppConfig
import io.legato.kazusa.help.config.LocalConfig
import io.legato.kazusa.lib.dialogs.alert
import io.legato.kazusa.lib.prefs.fragment.PreferenceFragment
//import io.legado.app.lib.theme.primaryColor
import io.legato.kazusa.model.CheckSource
import io.legato.kazusa.model.ImageProvider
import io.legato.kazusa.receiver.SharedReceiverActivity
import io.legato.kazusa.service.WebService
import io.legato.kazusa.ui.file.HandleFileContract
import io.legato.kazusa.ui.widget.number.NumberPickerDialog
import io.legato.kazusa.utils.LogUtils
import io.legato.kazusa.utils.getPrefBoolean
import io.legato.kazusa.utils.postEvent
import io.legato.kazusa.utils.putPrefBoolean
import io.legato.kazusa.utils.putPrefString
import io.legato.kazusa.utils.removePref
import io.legato.kazusa.utils.restart
import io.legato.kazusa.utils.showDialogFragment
import splitties.init.appCtx

/**
 * 其它设置
 */
class OtherConfigFragment : PreferenceFragment(),
    SharedPreferences.OnSharedPreferenceChangeListener {

    private val viewModel by activityViewModels<ConfigViewModel>()
    private val packageManager = appCtx.packageManager
    private val componentName = ComponentName(
        appCtx,
        SharedReceiverActivity::class.java.name
    )
    private val localBookTreeSelect = registerForActivityResult(HandleFileContract()) {
        it.uri?.let { treeUri ->
            AppConfig.defaultBookTreeUri = treeUri.toString()
        }
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        putPrefBoolean(PreferKey.processText, isProcessTextEnabled())
        addPreferencesFromResource(R.xml.pref_config_other)
        upPreferenceSummary(PreferKey.userAgent, AppConfig.userAgent)
        upPreferenceSummary(PreferKey.preDownloadNum, AppConfig.preDownloadNum.toString())
        upPreferenceSummary(PreferKey.threadCount, AppConfig.threadCount.toString())
        upPreferenceSummary(PreferKey.webPort, AppConfig.webPort.toString())
        AppConfig.defaultBookTreeUri?.let {
            upPreferenceSummary(PreferKey.defaultBookTreeUri, it)
        }
        upPreferenceSummary(PreferKey.checkSource, CheckSource.summary)
        upPreferenceSummary(PreferKey.bitmapCacheSize, AppConfig.bitmapCacheSize.toString())
        upPreferenceSummary(PreferKey.imageRetainNum, AppConfig.imageRetainNum.toString())
        upPreferenceSummary(PreferKey.sourceEditMaxLine, AppConfig.sourceEditMaxLine.toString())
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        activity?.setTitle(R.string.other_setting)
        preferenceManager.sharedPreferences?.registerOnSharedPreferenceChangeListener(this)
        //listView.setEdgeEffectColor(primaryColor)
    }

    override fun onDestroy() {
        super.onDestroy()
        preferenceManager.sharedPreferences?.unregisterOnSharedPreferenceChangeListener(this)
    }

    override fun onPreferenceTreeClick(preference: Preference): Boolean {
        when (preference.key) {
            PreferKey.userAgent -> showUserAgentDialog()
            PreferKey.defaultBookTreeUri -> localBookTreeSelect.launch {
                title = getString(R.string.select_book_folder)
                mode = HandleFileContract.DIR_SYS
            }

            PreferKey.preDownloadNum -> NumberPickerDialog(requireContext())
                .setTitle(getString(R.string.pre_download))
                .setMaxValue(9999)
                .setMinValue(0)
                .setValue(AppConfig.preDownloadNum)
                .show {
                    AppConfig.preDownloadNum = it
                }

            PreferKey.threadCount -> NumberPickerDialog(requireContext())
                .setTitle(getString(R.string.threads_num_title))
                .setMaxValue(999)
                .setMinValue(1)
                .setValue(AppConfig.threadCount)
                .show {
                    AppConfig.threadCount = it
                }

            PreferKey.webPort -> NumberPickerDialog(requireContext())
                .setTitle(getString(R.string.web_port_title))
                .setMaxValue(60000)
                .setMinValue(1024)
                .setValue(AppConfig.webPort)
                .show {
                    AppConfig.webPort = it
                }

            PreferKey.cleanCache -> clearCache()
            PreferKey.uploadRule -> showDialogFragment<DirectLinkUploadConfig>()
            PreferKey.checkSource -> showDialogFragment<CheckSourceConfig>()
            PreferKey.bitmapCacheSize -> {
                NumberPickerDialog(requireContext())
                    .setTitle(getString(R.string.bitmap_cache_size))
                    .setMaxValue(2047)
                    .setMinValue(1)
                    .setValue(AppConfig.bitmapCacheSize)
                    .show {
                        AppConfig.bitmapCacheSize = it
                        ImageProvider.bitmapLruCache.resize(ImageProvider.cacheSize)
                    }
            }
            PreferKey.imageRetainNum -> NumberPickerDialog(requireContext())
                .setTitle(getString(R.string.image_retain_number))
                .setMaxValue(999)
                .setMinValue(0)
                .setValue(AppConfig.imageRetainNum)
                .show {
                    AppConfig.imageRetainNum = it
                }

            PreferKey.sourceEditMaxLine -> {
                NumberPickerDialog(requireContext())
                    .setTitle(getString(R.string.source_edit_text_max_line))
                    .setMaxValue(Int.MAX_VALUE)
                    .setMinValue(10)
                    .setValue(AppConfig.sourceEditMaxLine)
                    .show {
                        AppConfig.sourceEditMaxLine = it
                    }
            }

            PreferKey.clearWebViewData -> clearWebViewData()
            "localPassword" -> alertLocalPassword()
            PreferKey.shrinkDatabase -> shrinkDatabase()
        }
        return super.onPreferenceTreeClick(preference)
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        when (key) {
            PreferKey.preDownloadNum -> {
                upPreferenceSummary(key, AppConfig.preDownloadNum.toString())
            }

            PreferKey.threadCount -> {
                upPreferenceSummary(key, AppConfig.threadCount.toString())
                postEvent(PreferKey.threadCount, "")
            }

            PreferKey.webPort -> {
                upPreferenceSummary(key, AppConfig.webPort.toString())
                if (WebService.isRun) {
                    WebService.stop(requireContext())
                    WebService.start(requireContext())
                }
            }

            PreferKey.defaultBookTreeUri -> {
                upPreferenceSummary(key, AppConfig.defaultBookTreeUri)
            }

            PreferKey.recordLog -> {
                AppConfig.recordLog = appCtx.getPrefBoolean(PreferKey.recordLog)
                LogUtils.upLevel()
                LogUtils.logDeviceInfo()
                LiveEventBus.config().enableLogger(AppConfig.recordLog)
                AppFreezeMonitor.init(appCtx)
            }

            PreferKey.processText -> sharedPreferences?.let {
                setProcessTextEnable(it.getBoolean(key, true))
            }

            PreferKey.showDiscovery, PreferKey.showRss, PreferKey.showBottomView -> postEvent(EventBus.NOTIFY_MAIN, true)
            PreferKey.language -> listView.postDelayed(1000) {
                appCtx.restart()
            }

            PreferKey.userAgent -> listView.post {
                upPreferenceSummary(PreferKey.userAgent, AppConfig.userAgent)
            }

            PreferKey.checkSource -> listView.post {
                upPreferenceSummary(PreferKey.checkSource, CheckSource.summary)
            }

            PreferKey.bitmapCacheSize -> {
                upPreferenceSummary(key, AppConfig.bitmapCacheSize.toString())
            }

            PreferKey.imageRetainNum -> {
                upPreferenceSummary(key, AppConfig.imageRetainNum.toString())
            }

            PreferKey.sourceEditMaxLine -> {
                upPreferenceSummary(key, AppConfig.sourceEditMaxLine.toString())
            }
        }
    }

    private fun upPreferenceSummary(preferenceKey: String, value: String?) {
        val preference = findPreference<Preference>(preferenceKey) ?: return
        when (preferenceKey) {
            PreferKey.preDownloadNum -> preference.summary =
                getString(R.string.pre_download_s, value)

            PreferKey.threadCount -> preference.summary = getString(R.string.threads_num, value)
            PreferKey.webPort -> preference.summary = getString(R.string.web_port_summary, value)
            PreferKey.bitmapCacheSize -> preference.summary =
                getString(R.string.bitmap_cache_size_summary, value)
            PreferKey.imageRetainNum -> preference.summary =
                getString(R.string.image_retain_number_summary, value)

            PreferKey.sourceEditMaxLine -> preference.summary =
                getString(R.string.source_edit_max_line_summary, value)

            else -> if (preference is ListPreference) {
                val index = preference.findIndexOfValue(value)
                // Set the summary to reflect the new value.
                preference.summary = if (index >= 0) preference.entries[index] else null
            } else {
                preference.summary = value
            }
        }
    }

    @SuppressLint("InflateParams")
    private fun showUserAgentDialog() {
        alert(getString(R.string.user_agent)) {
            val alertBinding = DialogEditTextBinding.inflate(layoutInflater).apply {
                editView.hint = getString(R.string.user_agent)
                editView.setText(AppConfig.userAgent)
            }
            customView { alertBinding.root }
            okButton {
                val userAgent = alertBinding.editView.text?.toString()
                if (userAgent.isNullOrBlank()) {
                    removePref(PreferKey.userAgent)
                } else {
                    putPrefString(PreferKey.userAgent, userAgent)
                }
            }
            cancelButton()
        }
    }

    private fun clearCache() {
        requireContext().alert(
            titleResource = R.string.clear_cache,
            messageResource = R.string.sure_del
        ) {
            okButton {
                viewModel.clearCache()
            }
            noButton()
        }
    }

    private fun shrinkDatabase() {
        alert(R.string.sure, R.string.shrink_database) {
            okButton {
                viewModel.shrinkDatabase()
            }
            noButton()
        }
    }

    private fun clearWebViewData() {
        alert(R.string.clear_webview_data, R.string.sure_del) {
            okButton {
                viewModel.clearWebViewData()
            }
            noButton()
        }
    }

    private fun isProcessTextEnabled(): Boolean {
        return packageManager.getComponentEnabledSetting(componentName) != PackageManager.COMPONENT_ENABLED_STATE_DISABLED
    }

    private fun setProcessTextEnable(enable: Boolean) {
        if (enable) {
            packageManager.setComponentEnabledSetting(
                componentName,
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP
            )
        } else {
            packageManager.setComponentEnabledSetting(
                componentName,
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP
            )
        }
    }

    private fun alertLocalPassword() {
        context?.alert(R.string.set_local_password, R.string.set_local_password_summary) {
            val editTextBinding = DialogEditTextBinding.inflate(layoutInflater).apply {
                editView.hint = "password"
            }
            customView {
                editTextBinding.root
            }
            okButton {
                LocalConfig.password = editTextBinding.editView.text.toString()
            }
            cancelButton()
        }
    }

}
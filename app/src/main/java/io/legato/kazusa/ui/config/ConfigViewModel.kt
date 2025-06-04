package io.legato.kazusa.ui.config

import android.app.Application
import android.content.Context
import io.legato.kazusa.R
import io.legato.kazusa.base.BaseViewModel
import io.legato.kazusa.data.appDb
import io.legato.kazusa.help.AppWebDav
import io.legato.kazusa.help.book.BookHelp
import io.legato.kazusa.utils.FileUtils
import io.legato.kazusa.utils.restart
import io.legato.kazusa.utils.toastOnUi
import kotlinx.coroutines.delay
import splitties.init.appCtx

class ConfigViewModel(application: Application) : BaseViewModel(application) {

    fun upWebDavConfig() {
        execute {
            AppWebDav.upConfig()
        }
    }

    fun clearCache() {
        execute {
            BookHelp.clearCache()
            FileUtils.delete(context.cacheDir.absolutePath)
        }.onSuccess {
            context.toastOnUi(R.string.clear_cache_success)
        }
    }

    fun clearWebViewData() {
        execute {
            FileUtils.delete(context.getDir("webview", Context.MODE_PRIVATE))
            FileUtils.delete(context.getDir("hws_webview", Context.MODE_PRIVATE), true)
            context.toastOnUi(R.string.clear_webview_data_success)
            delay(3000)
            appCtx.restart()
        }
    }

    fun shrinkDatabase() {
        execute {
            appDb.openHelper.writableDatabase.execSQL("VACUUM")
        }.onSuccess {
            context.toastOnUi(R.string.success)
        }
    }

}

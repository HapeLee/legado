package io.legato.kazusa.model

import android.content.Context
import io.legato.kazusa.constant.IntentAction
import io.legato.kazusa.service.DownloadService
import io.legato.kazusa.utils.startService

object Download {


    fun start(context: Context, url: String, fileName: String) {
        context.startService<DownloadService> {
            action = IntentAction.start
            putExtra("url", url)
            putExtra("fileName", fileName)
        }
    }

}
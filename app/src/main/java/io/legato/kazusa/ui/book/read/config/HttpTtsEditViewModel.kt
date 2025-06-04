package io.legato.kazusa.ui.book.read.config

import android.app.Application
import android.os.Bundle
import io.legato.kazusa.base.BaseViewModel
import io.legato.kazusa.data.appDb
import io.legato.kazusa.data.entities.HttpTTS
import io.legato.kazusa.exception.NoStackTraceException
import io.legato.kazusa.model.ReadAloud
import io.legato.kazusa.utils.getClipText
import io.legato.kazusa.utils.isJsonArray
import io.legato.kazusa.utils.isJsonObject
import io.legato.kazusa.utils.toastOnUi

class HttpTtsEditViewModel(app: Application) : BaseViewModel(app) {

    var id: Long? = null

    fun initData(arguments: Bundle?, success: (httpTTS: HttpTTS) -> Unit) {
        execute {
            if (id == null) {
                val argumentId = arguments?.getLong("id")
                if (argumentId != null && argumentId != 0L) {
                    id = argumentId
                    return@execute appDb.httpTTSDao.get(argumentId)
                }
            }
            return@execute null
        }.onSuccess {
            it?.let {
                success.invoke(it)
            }
        }
    }

    fun save(httpTTS: HttpTTS, success: (() -> Unit)? = null) {
        id = httpTTS.id
        execute {
            appDb.httpTTSDao.insert(httpTTS)
            if (ReadAloud.ttsEngine == httpTTS.id.toString()) ReadAloud.upReadAloudClass()
        }.onSuccess {
            success?.invoke()
        }
    }

    fun importFromClip(onSuccess: (httpTTS: HttpTTS) -> Unit) {
        val text = context.getClipText()
        if (text.isNullOrBlank()) {
            context.toastOnUi("剪贴板为空")
        } else {
            importSource(text, onSuccess)
        }
    }

    fun importSource(text: String, onSuccess: (httpTTS: HttpTTS) -> Unit) {
        val text1 = text.trim()
        execute {
            when {
                text1.isJsonObject() -> {
                    HttpTTS.fromJson(text1).getOrThrow()
                }
                text1.isJsonArray() -> {
                    HttpTTS.fromJsonArray(text1).getOrThrow().first()
                }
                else -> {
                    throw NoStackTraceException("格式不对")
                }
            }
        }.onSuccess {
            onSuccess.invoke(it)
        }.onError {
            context.toastOnUi(it.localizedMessage)
        }
    }

}
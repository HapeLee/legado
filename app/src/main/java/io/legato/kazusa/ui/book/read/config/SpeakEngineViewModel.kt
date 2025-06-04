package io.legato.kazusa.ui.book.read.config

import android.app.Application
import android.net.Uri
import android.speech.tts.TextToSpeech
import io.legato.kazusa.base.BaseViewModel
import io.legato.kazusa.data.appDb
import io.legato.kazusa.data.entities.HttpTTS
import io.legato.kazusa.exception.NoStackTraceException
import io.legato.kazusa.help.DefaultData
import io.legato.kazusa.utils.isJsonArray
import io.legato.kazusa.utils.isJsonObject
import io.legato.kazusa.utils.readText
import io.legato.kazusa.utils.toastOnUi

class SpeakEngineViewModel(application: Application) : BaseViewModel(application) {

    val sysEngines: List<TextToSpeech.EngineInfo> by lazy {
        val tts = TextToSpeech(context, null)
        val engines = tts.engines
        tts.shutdown()
        engines
    }

    fun importDefault() {
        execute {
            DefaultData.importDefaultHttpTTS()
        }
    }

    fun importLocal(uri: Uri) {
        execute {
            import(uri.readText(context))
        }.onSuccess {
            context.toastOnUi("导入成功")
        }.onError {
            context.toastOnUi("导入失败\n${it.localizedMessage}")
        }
    }

    fun import(text: String) {
        when {
            text.isJsonArray() -> {
                HttpTTS.fromJsonArray(text).getOrThrow().let {
                    appDb.httpTTSDao.insert(*it.toTypedArray())
                }
            }

            text.isJsonObject() -> {
                HttpTTS.fromJson(text).getOrThrow().let {
                    appDb.httpTTSDao.insert(it)
                }
            }

            else -> {
                throw NoStackTraceException("格式不对")
            }
        }
    }

}
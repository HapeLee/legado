package io.legato.kazusa.ui.association

import android.app.Application
import androidx.lifecycle.MutableLiveData
import io.legato.kazusa.R
import io.legato.kazusa.base.BaseViewModel
import io.legato.kazusa.constant.AppConst
import io.legato.kazusa.constant.AppLog
import io.legato.kazusa.data.appDb
import io.legato.kazusa.data.entities.DictRule
import io.legato.kazusa.exception.NoStackTraceException
import io.legato.kazusa.help.http.decompressed
import io.legato.kazusa.help.http.newCallResponseBody
import io.legato.kazusa.help.http.okHttpClient
import io.legato.kazusa.help.http.text
import io.legato.kazusa.utils.GSON
import io.legato.kazusa.utils.fromJsonArray
import io.legato.kazusa.utils.fromJsonObject
import io.legato.kazusa.utils.isAbsUrl
import io.legato.kazusa.utils.isJsonArray
import io.legato.kazusa.utils.isJsonObject

class ImportDictRuleViewModel(app: Application) : BaseViewModel(app) {

    val errorLiveData = MutableLiveData<String>()
    val successLiveData = MutableLiveData<Int>()

    val allSources = arrayListOf<DictRule>()
    val checkSources = arrayListOf<DictRule?>()
    val selectStatus = arrayListOf<Boolean>()

    val isSelectAll: Boolean
        get() {
            selectStatus.forEach {
                if (!it) {
                    return false
                }
            }
            return true
        }

    val selectCount: Int
        get() {
            var count = 0
            selectStatus.forEach {
                if (it) {
                    count++
                }
            }
            return count
        }

    fun importSelect(finally: () -> Unit) {
        execute {
            val selectSource = arrayListOf<DictRule>()
            selectStatus.forEachIndexed { index, b ->
                if (b) {
                    selectSource.add(allSources[index])
                }
            }
            appDb.dictRuleDao.insert(*selectSource.toTypedArray())
        }.onFinally {
            finally.invoke()
        }
    }

    fun importSource(text: String) {
        execute {
            importSourceAwait(text.trim())
        }.onError {
            errorLiveData.postValue("ImportError:${it.localizedMessage}")
            AppLog.put("ImportError:${it.localizedMessage}", it)
        }.onSuccess {
            comparisonSource()
        }
    }

    private suspend fun importSourceAwait(text: String) {
        when {
            text.isJsonObject() -> {
                GSON.fromJsonObject<DictRule>(text).getOrThrow().let {
                    allSources.add(it)
                }
            }
            text.isJsonArray() -> GSON.fromJsonArray<DictRule>(text).getOrThrow().let { items ->
                allSources.addAll(items)
            }
            text.isAbsUrl() -> {
                importSourceUrl(text)
            }
            else -> throw NoStackTraceException(context.getString(R.string.wrong_format))
        }
    }

    private suspend fun importSourceUrl(url: String) {
        okHttpClient.newCallResponseBody {
            if (url.endsWith("#requestWithoutUA")) {
                url(url.substringBeforeLast("#requestWithoutUA"))
                header(AppConst.UA_NAME, "null")
            } else {
                url(url)
            }
        }.decompressed().text().let {
            importSourceAwait(it)
        }
    }

    private fun comparisonSource() {
        execute {
            allSources.forEach {
                val source = appDb.dictRuleDao.getByName(it.name)
                checkSources.add(source)
                selectStatus.add(source == null)
            }
            successLiveData.postValue(allSources.size)
        }
    }

}
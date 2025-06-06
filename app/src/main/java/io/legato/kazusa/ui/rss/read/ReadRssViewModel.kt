package io.legato.kazusa.ui.rss.read

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.util.Base64
import android.webkit.URLUtil
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.script.rhino.runScriptWithContext
import io.legato.kazusa.base.BaseViewModel
import io.legato.kazusa.constant.AppConst
import io.legato.kazusa.data.appDb
import io.legato.kazusa.data.entities.RssArticle
import io.legato.kazusa.data.entities.RssSource
import io.legato.kazusa.data.entities.RssStar
import io.legato.kazusa.exception.NoStackTraceException
import io.legato.kazusa.help.TTS
import io.legato.kazusa.help.http.newCallResponseBody
import io.legato.kazusa.help.http.okHttpClient
import io.legato.kazusa.model.analyzeRule.AnalyzeUrl
import io.legato.kazusa.model.rss.Rss
import io.legato.kazusa.utils.toastOnUi
import io.legato.kazusa.utils.writeBytes
import kotlinx.coroutines.Dispatchers.IO
import splitties.init.appCtx
import java.util.Date
import kotlin.coroutines.coroutineContext


class ReadRssViewModel(application: Application) : BaseViewModel(application) {
    var rssSource: RssSource? = null
    var rssArticle: RssArticle? = null
    var tts: TTS? = null
    val contentLiveData = MutableLiveData<String>()
    val urlLiveData = MutableLiveData<AnalyzeUrl>()
    var rssStar: RssStar? = null
    val upTtsMenuData = MutableLiveData<Boolean>()
    val upStarMenuData = MutableLiveData<Boolean>()
    var headerMap: Map<String, String> = emptyMap()

    fun initData(intent: Intent) {
        execute {
            val origin = intent.getStringExtra("origin") ?: return@execute
            val link = intent.getStringExtra("link")
            rssSource = appDb.rssSourceDao.getByKey(origin)
            headerMap = runScriptWithContext {
                rssSource?.getHeaderMap() ?: emptyMap()
            }
            if (link != null) {
                rssStar = appDb.rssStarDao.get(origin, link)
                rssArticle = rssStar?.toRssArticle() ?: appDb.rssArticleDao.get(origin, link)
                val rssArticle = rssArticle ?: return@execute
                if (!rssArticle.description.isNullOrBlank()) {
                    contentLiveData.postValue(rssArticle.description!!)
                } else {
                    rssSource?.let {
                        val ruleContent = it.ruleContent
                        if (!ruleContent.isNullOrBlank()) {
                            loadContent(rssArticle, ruleContent)
                        } else {
                            loadUrl(rssArticle.link, rssArticle.origin)
                        }
                    } ?: loadUrl(rssArticle.link, rssArticle.origin)
                }
            } else {
                val ruleContent = rssSource?.ruleContent
                if (ruleContent.isNullOrBlank()) {
                    loadUrl(origin, origin)
                } else {
                    val rssArticle = RssArticle()
                    rssArticle.origin = origin
                    rssArticle.link = origin
                    rssArticle.title = rssSource!!.sourceName
                    loadContent(rssArticle, ruleContent)
                }
            }
        }.onFinally {
            upStarMenuData.postValue(true)
        }
    }

    private suspend fun loadUrl(url: String, baseUrl: String) {
        val analyzeUrl = AnalyzeUrl(
            mUrl = url,
            baseUrl = baseUrl,
            source = rssSource,
            coroutineContext = coroutineContext,
            hasLoginHeader = false
        )
        urlLiveData.postValue(analyzeUrl)
    }

    private fun loadContent(rssArticle: RssArticle, ruleContent: String) {
        val source = rssSource ?: return
        Rss.getContent(viewModelScope, rssArticle, ruleContent, source)
            .onSuccess(IO) { body ->
                rssArticle.description = body
                appDb.rssArticleDao.insert(rssArticle)
                rssStar?.let {
                    it.description = body
                    appDb.rssStarDao.insert(it)
                }
                contentLiveData.postValue(body)
            }.onError {
                contentLiveData.postValue("加载正文失败\n${it.stackTraceToString()}")
            }
    }

    fun refresh(finish: () -> Unit) {
        rssArticle?.let { rssArticle ->
            rssSource?.let {
                val ruleContent = it.ruleContent
                if (!ruleContent.isNullOrBlank()) {
                    loadContent(rssArticle, ruleContent)
                } else {
                    finish.invoke()
                }
            } ?: let {
                appCtx.toastOnUi("订阅源不存在")
                finish.invoke()
            }
        } ?: finish.invoke()
    }

    fun favorite() {
        execute {
            rssStar?.let {
                appDb.rssStarDao.delete(it.origin, it.link)
                rssStar = null
            } ?: rssArticle?.toStar()?.let {
                appDb.rssStarDao.insert(it)
                rssStar = it
            }
        }.onSuccess {
            upStarMenuData.postValue(true)
        }
    }

    fun addFavorite() {
        execute {
            rssStar ?: rssArticle?.toStar()?.let {
                appDb.rssStarDao.insert(it)
                rssStar = it
            }
        }.onSuccess {
            upStarMenuData.postValue(true)
        }
    }

    fun updateFavorite() {
        execute {
            rssArticle?.toStar()?.let {
                appDb.rssStarDao.update(it)
                rssStar = it
            }
        }.onSuccess {
            upStarMenuData.postValue(true)
        }
    }

    fun delFavorite() {
        execute {
            rssStar?.let {
                appDb.rssStarDao.delete(it.origin, it.link)
                rssStar = null
            }
        }.onSuccess {
            upStarMenuData.postValue(true)
        }
    }

    fun saveImage(webPic: String?, uri: Uri) {
        webPic ?: return
        execute {
            val fileName = "${AppConst.fileNameFormat.format(Date(System.currentTimeMillis()))}.jpg"
            val byteArray = webData2bitmap(webPic) ?: throw NoStackTraceException("NULL")
            uri.writeBytes(context, fileName, byteArray)
        }.onError {
            context.toastOnUi("保存图片失败:${it.localizedMessage}")
        }.onSuccess {
            context.toastOnUi("保存成功")
        }
    }

    private suspend fun webData2bitmap(data: String): ByteArray? {
        return if (URLUtil.isValidUrl(data)) {
            okHttpClient.newCallResponseBody {
                url(data)
            }.bytes()
        } else {
            Base64.decode(data.split(",").toTypedArray()[1], Base64.DEFAULT)
        }
    }

    fun clHtml(content: String): String {
        return when {
            !rssSource?.style.isNullOrEmpty() -> {
                """
                    <style>
                        ${rssSource?.style}
                    </style>
                    $content
                """.trimIndent()
            }

            content.contains("<style>".toRegex()) -> {
                content
            }

            else -> {
                """
                    <style>
                        img{max-width:100% !important; width:auto; height:auto;}
                        video{object-fit:fill; max-width:100% !important; width:auto; height:auto;}
                        body{word-wrap:break-word; height:auto;max-width: 100%; width:auto;}
                    </style>
                    $content
                """.trimIndent()
            }
        }
    }

    @Synchronized
    fun readAloud(text: String) {
        if (tts == null) {
            tts = TTS().apply {
                setSpeakStateListener(object : TTS.SpeakStateListener {
                    override fun onStart() {
                        upTtsMenuData.postValue(true)
                    }

                    override fun onDone() {
                        upTtsMenuData.postValue(false)
                    }
                })
            }
        }
        tts?.speak(text)
    }

    override fun onCleared() {
        super.onCleared()
        tts?.clearTts()
    }

}
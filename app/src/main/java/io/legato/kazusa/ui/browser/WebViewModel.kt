package io.legato.kazusa.ui.browser

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.util.Base64
import android.webkit.URLUtil
import android.webkit.WebView
import androidx.documentfile.provider.DocumentFile
import io.legato.kazusa.base.BaseViewModel
import io.legato.kazusa.constant.AppConst
import io.legato.kazusa.constant.SourceType
import io.legato.kazusa.data.appDb
import io.legato.kazusa.exception.NoStackTraceException
import io.legato.kazusa.help.http.newCallResponseBody
import io.legato.kazusa.help.http.okHttpClient
import io.legato.kazusa.help.source.SourceHelp
import io.legato.kazusa.help.source.SourceVerificationHelp
import io.legato.kazusa.model.analyzeRule.AnalyzeUrl
import io.legato.kazusa.utils.DocumentUtils
import io.legato.kazusa.utils.FileUtils
import io.legato.kazusa.utils.isContentScheme
import io.legato.kazusa.utils.printOnDebug
import io.legato.kazusa.utils.toastOnUi
import io.legato.kazusa.utils.writeBytes
import org.apache.commons.text.StringEscapeUtils
import java.io.File
import java.util.Date

class WebViewModel(application: Application) : BaseViewModel(application) {
    var intent: Intent? = null
    var baseUrl: String = ""
    var html: String? = null
    val headerMap: HashMap<String, String> = hashMapOf()
    var sourceVerificationEnable: Boolean = false
    var refetchAfterSuccess: Boolean = true
    var sourceName: String = ""
    var sourceOrigin: String = ""
    var sourceType = SourceType.book

    fun initData(
        intent: Intent,
        success: () -> Unit
    ) {
        execute {
            this@WebViewModel.intent = intent
            val url = intent.getStringExtra("url")
                ?: throw NoStackTraceException("url不能为空")
            sourceName = intent.getStringExtra("sourceName") ?: ""
            sourceOrigin = intent.getStringExtra("sourceOrigin") ?: ""
            sourceType = intent.getIntExtra("sourceType", SourceType.book)
            sourceVerificationEnable = intent.getBooleanExtra("sourceVerificationEnable", false)
            refetchAfterSuccess = intent.getBooleanExtra("refetchAfterSuccess", true)
            val source = SourceHelp.getSource(sourceOrigin, sourceType)
            val analyzeUrl = AnalyzeUrl(url, source = source, coroutineContext = coroutineContext)
            baseUrl = analyzeUrl.url
            headerMap.putAll(analyzeUrl.headerMap)
            if (analyzeUrl.isPost()) {
                html = analyzeUrl.getStrResponseAwait(useWebView = false).body
            }
        }.onSuccess {
            success.invoke()
        }.onError {
            context.toastOnUi("error\n${it.localizedMessage}")
            it.printOnDebug()
        }
    }

    fun saveImage(webPic: String?, path: String) {
        webPic ?: return
        execute {
            val fileName = "${AppConst.fileNameFormat.format(Date(System.currentTimeMillis()))}.jpg"
            webData2bitmap(webPic)?.let { biteArray ->
                if (path.isContentScheme()) {
                    val uri = Uri.parse(path)
                    DocumentFile.fromTreeUri(context, uri)?.let { doc ->
                        DocumentUtils.createFileIfNotExist(doc, fileName)
                            ?.writeBytes(context, biteArray)
                    }
                } else {
                    val file = FileUtils.createFileIfNotExist(File(path), fileName)
                    file.writeBytes(biteArray)
                }
            } ?: throw Throwable("NULL")
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

    fun saveVerificationResult(webView: WebView, success: () -> Unit) {
        if (!sourceVerificationEnable) {
            return success.invoke()
        }
        if (refetchAfterSuccess) {
            execute {
                val url = intent!!.getStringExtra("url")!!
                val source = appDb.bookSourceDao.getBookSource(sourceOrigin)
                html = AnalyzeUrl(
                    url,
                    headerMapF = headerMap,
                    source = source,
                    coroutineContext = coroutineContext
                ).getStrResponseAwait(useWebView = false).body
                SourceVerificationHelp.setResult(sourceOrigin, html ?: "")
            }.onSuccess {
                success.invoke()
            }
        } else {
            webView.evaluateJavascript("document.documentElement.outerHTML") {
                execute {
                    html = StringEscapeUtils.unescapeJson(it).trim('"')
                    SourceVerificationHelp.setResult(sourceOrigin, html ?: "")
                }.onSuccess {
                    success.invoke()
                }
            }
        }
    }

    fun disableSource(block: () -> Unit) {
        execute {
            SourceHelp.enableSource(sourceOrigin, sourceType, false)
        }.onSuccess {
            block.invoke()
        }
    }

    fun deleteSource(block: () -> Unit) {
        execute {
            SourceHelp.deleteSource(sourceOrigin, sourceType)
        }.onSuccess {
            block.invoke()
        }
    }

}
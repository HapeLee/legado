package io.legado.app.ui.association

import android.app.Application
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.MutableLiveData
import io.legado.app.R
import io.legado.app.constant.AppPattern.bookFileRegex
import io.legado.app.exception.NoStackTraceException
import io.legado.app.lib.dialogs.alert
import io.legado.app.model.localBook.LocalBook
import io.legado.app.utils.isJson
import io.legado.app.utils.printOnDebug
import io.legado.app.utils.readText
import java.io.File
import splitties.init.appCtx

class FileAssociationViewModel(application: Application) : BaseAssociationViewModel(application) {
    val importBookLiveData = MutableLiveData<Uri>()
    val onLineImportLive = MutableLiveData<Uri>()
    val importBookSourceLive = MutableLiveData<String>()
    val importRssSourceLive = MutableLiveData<String>()
    val importReplaceRuleLive = MutableLiveData<String>()
    val openBookLiveData = MutableLiveData<String>()
    val errorLiveData = MutableLiveData<String>()

    @Suppress("BlockingMethodInNonBlockingContext")
    fun dispatchIndent(uri: Uri, finally: (title: String, msg: String) -> Unit) {
        execute {
            lateinit var fileName: String
            lateinit var content: String
            //如果是普通的url，需要根据返回的内容判断是什么
            if (uri.scheme == "file" || uri.scheme == "content") {
                if (uri.scheme == "file") {
                    val file = File(uri.path.toString())
                    content = file.readText()
                    fileName = file.name
                } else {
                    val file = DocumentFile.fromSingleUri(context, uri)
                    content = file?.readText(context) ?: throw NoStackTraceException("文件不存在")
                    fileName = file.name ?: ""
                }
                when {
                    content.isJson() -> {
                        //暂时根据文件内容判断属于什么
                        when {
                            content.contains("bookSourceUrl") ->
                                importBookSourceLive.postValue(content)
                            content.contains("sourceUrl") ->
                                importRssSourceLive.postValue(content)
                            content.contains("pattern") ->
                                importReplaceRuleLive.postValue(content)
                            content.contains("themeName") ->
                                importTheme(content, finally)
                            content.contains("name") && content.contains("rule") ->
                                importTextTocRule(content, finally)
                            content.contains("name") && content.contains("url") ->
                                importHttpTTS(content, finally)
                            else -> errorLiveData.postValue(appCtx.getString(R.string.wrong_format))
                        }
                    }
                    !fileName.matches(bookFileRegex) -> {
                        appCtx.alert(title = appCtx.getString(R.string.draw), message = appCtx.getString(R.string.file_not_supported, fileName)) {
                            okButton {
                                importBookLiveData.postValue(uri)
                            }
                            cancelButton()
                        }
                    }
                    else -> {
                        importBookLiveData.postValue(uri)
                    }
                }
            } else {
                onLineImportLive.postValue(uri)
            }
        }.onError {
            it.printOnDebug()
            errorLiveData.postValue(it.localizedMessage)
        }
    }

    fun importBook(uri: Uri) {
        val book = LocalBook.importFile(uri)
        openBookLiveData.postValue(book.bookUrl)
    }
}
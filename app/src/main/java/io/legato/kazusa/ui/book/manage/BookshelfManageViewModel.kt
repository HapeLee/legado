package io.legato.kazusa.ui.book.manage

import android.app.Application
import androidx.lifecycle.MutableLiveData
import io.legato.kazusa.R
import io.legato.kazusa.base.BaseViewModel
import io.legato.kazusa.constant.AppLog
import io.legato.kazusa.constant.BookType
import io.legato.kazusa.data.appDb
import io.legato.kazusa.data.entities.Book
import io.legato.kazusa.data.entities.BookSource
import io.legato.kazusa.help.book.BookHelp
import io.legato.kazusa.help.book.isLocal
import io.legato.kazusa.help.book.removeType
import io.legato.kazusa.help.config.AppConfig
import io.legato.kazusa.help.coroutine.Coroutine
import io.legato.kazusa.model.localBook.LocalBook
import io.legato.kazusa.model.webBook.WebBook
import io.legato.kazusa.utils.FileUtils
import io.legato.kazusa.utils.GSON
import io.legato.kazusa.utils.stackTraceStr
import io.legato.kazusa.utils.toastOnUi
import io.legato.kazusa.utils.writeToOutputStream
import kotlinx.coroutines.delay
import java.io.File


class BookshelfManageViewModel(application: Application) : BaseViewModel(application) {
    var groupId: Long = -1L
    var groupName: String? = null
    val batchChangeSourceState = MutableLiveData<Boolean>()
    val batchChangeSourceProcessLiveData = MutableLiveData<String>()
    var batchChangeSourceCoroutine: Coroutine<Unit>? = null

    fun upCanUpdate(books: List<Book>, canUpdate: Boolean) {
        execute {
            val array = Array(books.size) {
                books[it].copy(canUpdate = canUpdate).apply {
                    if (!canUpdate) {
                        removeType(BookType.updateError)
                    }
                }
            }
            appDb.bookDao.update(*array)
        }
    }

    fun updateBook(vararg book: Book) {
        execute {
            appDb.bookDao.update(*book)
        }
    }

    fun deleteBook(books: List<Book>, deleteOriginal: Boolean = false) {
        execute {
            appDb.bookDao.delete(*books.toTypedArray())
            books.forEach {
                if (it.isLocal) {
                    LocalBook.deleteBook(it, deleteOriginal)
                }
            }
        }
    }

    fun saveAllUseBookSourceToFile(success: (file: File) -> Unit) {
        execute {
            val path = "${context.filesDir}/shareBookSource.json"
            FileUtils.delete(path)
            val file = FileUtils.createFileWithReplace(path)
            val sources = appDb.bookDao.getAllUseBookSource()
            file.outputStream().buffered().use {
                GSON.writeToOutputStream(it, sources)
            }
            file
        }.onSuccess {
            success.invoke(it)
        }.onError {
            context.toastOnUi(it.stackTraceStr)
        }
    }

    fun changeSource(books: List<Book>, source: BookSource) {
        batchChangeSourceCoroutine?.cancel()
        batchChangeSourceCoroutine = execute {
            val changeSourceDelay = AppConfig.batchChangeSourceDelay * 1000L
            books.forEachIndexed { index, book ->
                batchChangeSourceProcessLiveData.postValue("${index + 1} / ${books.size}")
                if (book.isLocal) return@forEachIndexed
                if (book.origin == source.bookSourceUrl) return@forEachIndexed
                val newBook = WebBook.preciseSearchAwait(source, book.name, book.author)
                    .onFailure {
                        AppLog.put("搜索书籍出错\n${it.localizedMessage}", it, true)
                    }.getOrNull() ?: return@forEachIndexed
                kotlin.runCatching {
                    if (newBook.tocUrl.isEmpty()) {
                        WebBook.getBookInfoAwait(source, newBook)
                    }
                }.onFailure {
                    AppLog.put("获取书籍详情出错\n${it.localizedMessage}", it, true)
                    return@forEachIndexed
                }
                WebBook.getChapterListAwait(source, newBook)
                    .onFailure {
                        AppLog.put("获取目录出错\n${it.localizedMessage}", it, true)
                    }.getOrNull()?.let { toc ->
                        book.migrateTo(newBook, toc)
                        book.removeType(BookType.updateError)
                        appDb.bookDao.insert(newBook)
                        appDb.bookChapterDao.insert(*toc.toTypedArray())
                    }
                delay(changeSourceDelay)
            }
        }.onStart {
            batchChangeSourceState.postValue(true)
        }.onFinally {
            batchChangeSourceState.postValue(false)
        }
    }

    fun clearCache(books: List<Book>) {
        execute {
            books.forEach {
                BookHelp.clearCache(it)
            }
        }.onSuccess {
            context.toastOnUi(R.string.clear_cache_success)
        }
    }

}
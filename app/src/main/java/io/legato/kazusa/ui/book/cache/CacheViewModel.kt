package io.legato.kazusa.ui.book.cache

import android.app.Application
import androidx.lifecycle.MutableLiveData
import io.legato.kazusa.base.BaseViewModel
import io.legato.kazusa.data.appDb
import io.legato.kazusa.data.entities.Book
import io.legato.kazusa.help.book.BookHelp
import io.legato.kazusa.help.book.isLocal
import io.legato.kazusa.help.coroutine.Coroutine
import io.legato.kazusa.utils.sendValue
import kotlinx.coroutines.ensureActive
import kotlin.collections.set


class CacheViewModel(application: Application) : BaseViewModel(application) {
    val upAdapterLiveData = MutableLiveData<String>()

    private var loadChapterCoroutine: Coroutine<Unit>? = null
    val cacheChapters = hashMapOf<String, HashSet<String>>()

    fun loadCacheFiles(books: List<Book>) {
        loadChapterCoroutine?.cancel()
        loadChapterCoroutine = execute {
            books.forEach { book ->
                if (!book.isLocal && !cacheChapters.contains(book.bookUrl)) {
                    val chapterCaches = hashSetOf<String>()
                    val cacheNames = BookHelp.getChapterFiles(book)
                    if (cacheNames.isNotEmpty()) {
                        appDb.bookChapterDao.getChapterList(book.bookUrl).also {
                            book.totalChapterNum = it.size
                        }.forEach { chapter ->
                            if (cacheNames.contains(chapter.getFileName()) || chapter.isVolume) {
                                chapterCaches.add(chapter.url)
                            }
                        }
                    }
                    cacheChapters[book.bookUrl] = chapterCaches
                    upAdapterLiveData.sendValue(book.bookUrl)
                }
                ensureActive()
            }
        }
    }

}
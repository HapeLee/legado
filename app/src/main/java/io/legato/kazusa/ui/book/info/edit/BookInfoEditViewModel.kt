package io.legato.kazusa.ui.book.info.edit

import android.app.Application
import androidx.lifecycle.MutableLiveData
import io.legato.kazusa.base.BaseViewModel
import io.legato.kazusa.data.appDb
import io.legato.kazusa.data.entities.Book
import io.legato.kazusa.model.ReadBook

class BookInfoEditViewModel(application: Application) : BaseViewModel(application) {
    var book: Book? = null
    val bookData = MutableLiveData<Book>()

    fun loadBook(bookUrl: String) {
        execute {
            book = appDb.bookDao.getBook(bookUrl)
            book?.let {
                bookData.postValue(it)
            }
        }
    }

    fun saveBook(book: Book, success: (() -> Unit)?) {
        execute {
            if (ReadBook.book?.bookUrl == book.bookUrl) {
                ReadBook.book = book
            }
            appDb.bookDao.update(book)
        }.onSuccess {
            success?.invoke()
        }
    }
}
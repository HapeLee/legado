package io.legato.kazusa.ui.rss.read

import io.legato.kazusa.data.entities.BaseSource
import io.legato.kazusa.help.JsExtensions
import io.legato.kazusa.ui.association.AddToBookshelfDialog
import io.legato.kazusa.ui.book.search.SearchActivity
import io.legato.kazusa.utils.showDialogFragment

@Suppress("unused")
class RssJsExtensions(private val activity: ReadRssActivity) : JsExtensions {

    override fun getSource(): BaseSource? {
        return activity.getSource()
    }

    fun searchBook(key: String) {
        SearchActivity.start(activity, key)
    }

    fun addBook(bookUrl: String) {
        activity.showDialogFragment(AddToBookshelfDialog(bookUrl))
    }

}

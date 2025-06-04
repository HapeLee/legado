package io.legato.kazusa.ui.main.explore

import android.app.Application
import io.legato.kazusa.base.BaseViewModel
import io.legato.kazusa.data.appDb
import io.legato.kazusa.data.entities.BookSourcePart
import io.legato.kazusa.help.source.SourceHelp

class ExploreViewModel(application: Application) : BaseViewModel(application) {

    fun topSource(bookSource: BookSourcePart) {
        execute {
            val minXh = appDb.bookSourceDao.minOrder
            bookSource.customOrder = minXh - 1
            appDb.bookSourceDao.upOrder(bookSource)
        }
    }

    fun deleteSource(source: BookSourcePart) {
        execute {
            SourceHelp.deleteBookSource(source.bookSourceUrl)
        }
    }

}
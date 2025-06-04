package io.legato.kazusa.ui.rss.article

import android.app.Application
import android.content.Intent
import androidx.lifecycle.MutableLiveData
import io.legato.kazusa.base.BaseViewModel
import io.legato.kazusa.data.appDb
import io.legato.kazusa.data.entities.RssArticle
import io.legato.kazusa.data.entities.RssReadRecord
import io.legato.kazusa.data.entities.RssSource
import io.legato.kazusa.help.source.removeSortCache


class RssSortViewModel(application: Application) : BaseViewModel(application) {
    var url: String? = null
    var rssSource: RssSource? = null
    val titleLiveData = MutableLiveData<String>()
    var order = System.currentTimeMillis()
    val isGridLayout get() = rssSource?.articleStyle == 2

    fun initData(intent: Intent, finally: () -> Unit) {
        execute {
            url = intent.getStringExtra("url")
            url?.let { url ->
                rssSource = appDb.rssSourceDao.getByKey(url)
                rssSource?.let {
                    titleLiveData.postValue(it.sourceName)
                } ?: let {
                    rssSource = RssSource(sourceUrl = url)
                }
            }
        }.onFinally {
            finally()
        }
    }

    fun switchLayout() {
        rssSource?.let {
            if (it.articleStyle < 2) {
                it.articleStyle += 1
            } else {
                it.articleStyle = 0
            }
            execute {
                appDb.rssSourceDao.update(it)
            }
        }
    }

    fun read(rssArticle: RssArticle) {
        execute {
            val rssReadRecord = RssReadRecord(
                record = rssArticle.link,
                title = rssArticle.title,
                readTime = System.currentTimeMillis()
            )
            appDb.rssReadRecordDao.insertRecord(rssReadRecord)
        }
    }

    fun clearArticles() {
        execute {
            url?.let {
                appDb.rssArticleDao.delete(it)
            }
            order = System.currentTimeMillis()
        }.onSuccess {

        }
    }

    fun clearSortCache(onFinally: () -> Unit) {
        execute {
            rssSource?.removeSortCache()
        }.onFinally {
            onFinally.invoke()
        }
    }

    fun getRecords(): List<RssReadRecord> {
        return appDb.rssReadRecordDao.getRecords()
    }

    fun countRecords() : Int {
        return appDb.rssReadRecordDao.countRecords
    }

    fun deleteAllRecord() {
        execute {
            appDb.rssReadRecordDao.deleteAllRecord()
        }
    }

}
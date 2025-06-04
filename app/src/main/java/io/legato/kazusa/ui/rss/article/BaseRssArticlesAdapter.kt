package io.legato.kazusa.ui.rss.article

import android.content.Context
import androidx.viewbinding.ViewBinding
import io.legato.kazusa.base.adapter.RecyclerAdapter
import io.legato.kazusa.data.entities.RssArticle


abstract class BaseRssArticlesAdapter<VB : ViewBinding>(context: Context, val callBack: CallBack) :
    RecyclerAdapter<RssArticle, VB>(context) {

    interface CallBack {
        val isGridLayout: Boolean
        fun readRss(rssArticle: RssArticle)
    }
}
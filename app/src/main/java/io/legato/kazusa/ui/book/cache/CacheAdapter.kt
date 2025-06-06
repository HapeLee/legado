package io.legato.kazusa.ui.book.cache

import android.content.Context
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import com.google.android.material.button.MaterialButton
import io.legato.kazusa.R
import io.legato.kazusa.base.adapter.DiffRecyclerAdapter
import io.legato.kazusa.base.adapter.ItemViewHolder
import io.legato.kazusa.data.entities.Book
import io.legato.kazusa.databinding.ItemDownloadBinding
import io.legato.kazusa.help.book.isLocal
import io.legato.kazusa.model.CacheBook
import io.legato.kazusa.utils.gone
import io.legato.kazusa.utils.visible

class CacheAdapter(context: Context, private val callBack: CallBack) :
    DiffRecyclerAdapter<Book, ItemDownloadBinding>(context) {

    override val diffItemCallback: DiffUtil.ItemCallback<Book>
        get() = object : DiffUtil.ItemCallback<Book>() {
            override fun areItemsTheSame(oldItem: Book, newItem: Book): Boolean {
                return oldItem.bookUrl == newItem.bookUrl
            }

            override fun areContentsTheSame(oldItem: Book, newItem: Book): Boolean {
                return oldItem.name == newItem.name
                        && oldItem.author == newItem.author
            }

        }

    override fun getViewBinding(parent: ViewGroup): ItemDownloadBinding {
        return ItemDownloadBinding.inflate(inflater, parent, false)
    }

    override fun convert(
        holder: ItemViewHolder,
        binding: ItemDownloadBinding,
        item: Book,
        payloads: MutableList<Any>
    ) {
        binding.run {
            if (payloads.isEmpty()) {
                tvName.text = item.name
                tvAuthor.text = context.getString(R.string.author_show, item.getRealAuthor())
                if (item.isLocal) {
                    tvDownload.setText(R.string.local_book)
                } else {
                    val cs = callBack.cacheChapters[item.bookUrl]
                    if (cs == null) {
                        tvDownload.setText(R.string.loading)
                    } else {
                        tvDownload.text =
                            context.getString(
                                R.string.download_count,
                                cs.size,
                                item.totalChapterNum
                            )
                    }
                }
            } else {
                if (item.isLocal) {
                    tvDownload.setText(R.string.local_book)
                } else {
                    val cacheSize = callBack.cacheChapters[item.bookUrl]?.size ?: 0
                    tvDownload.text =
                        context.getString(R.string.download_count, cacheSize, item.totalChapterNum)
                }
            }
            upDownloadIv(ivDownload, item)
            upExportInfo(tvMsg, progressExport, item)
        }
    }

    override fun registerListener(holder: ItemViewHolder, binding: ItemDownloadBinding) {
        binding.run {
            ivDownload.setOnClickListener {
                getItem(holder.layoutPosition)?.let { book ->
                    CacheBook.cacheBookMap[book.bookUrl]?.let {
                        if (!it.isStop()) {
                            CacheBook.remove(context, book.bookUrl)
                        } else {
                            CacheBook.start(context, book, 0, book.lastChapterIndex)
                        }
                    } ?: let {
                        CacheBook.start(context, book, 0, book.lastChapterIndex)
                    }
                }
            }
            tvExport.setOnClickListener {
                callBack.export(holder.layoutPosition)
            }
        }
    }

    private fun upDownloadIv(button: MaterialButton, book: Book) {
        if (book.isLocal) {
            button.gone()
        } else {
            button.visible()
            CacheBook.cacheBookMap[book.bookUrl]?.let {
                if (!it.isStop()) {
                    button.icon = ContextCompat.getDrawable(context, R.drawable.ic_stop_black_24dp)
                } else {
                    button.icon = ContextCompat.getDrawable(context, R.drawable.ic_play_24dp)
                }
            } ?: run {
                button.icon = ContextCompat.getDrawable(context, R.drawable.ic_play_24dp)
            }
        }
    }

    private fun upExportInfo(msgView: TextView, progressView: ProgressBar, book: Book) {
        val msg = callBack.exportMsg(book.bookUrl)
        if (msg != null) {
            msgView.text = msg
            msgView.visible()
            progressView.gone()
            return
        }
        msgView.gone()
        val progress = callBack.exportProgress(book.bookUrl)
        if (progress != null) {
            progressView.max = book.totalChapterNum
            progressView.progress = progress
            progressView.visible()
            return
        }
        progressView.gone()
    }

    interface CallBack {
        val cacheChapters: HashMap<String, HashSet<String>>
        fun export(position: Int)
        fun exportProgress(bookUrl: String): Int?
        fun exportMsg(bookUrl: String): String?
    }
}
package io.legato.kazusa.ui.book.toc

//import io.legado.app.lib.theme.accentColor
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import io.legato.kazusa.R
import io.legato.kazusa.base.adapter.DiffRecyclerAdapter
import io.legato.kazusa.base.adapter.ItemViewHolder
import io.legato.kazusa.data.entities.Book
import io.legato.kazusa.data.entities.BookChapter
import io.legato.kazusa.databinding.ItemChapterListBinding
import io.legato.kazusa.help.book.ContentProcessor
import io.legato.kazusa.help.config.AppConfig
import io.legato.kazusa.help.coroutine.Coroutine
import io.legato.kazusa.lib.theme.ThemeUtils
import io.legato.kazusa.utils.gone
import io.legato.kazusa.utils.longToastOnUi
import io.legato.kazusa.utils.themeColor
import io.legato.kazusa.utils.visible
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

class ChapterListAdapter(context: Context, val callback: Callback) :
    DiffRecyclerAdapter<BookChapter, ItemChapterListBinding>(context) {

    val cacheFileNames = hashSetOf<String>()
    private val displayTitleMap = ConcurrentHashMap<String, String>()
    private val handler = Handler(Looper.getMainLooper())

    override val diffItemCallback: DiffUtil.ItemCallback<BookChapter>
        get() = object : DiffUtil.ItemCallback<BookChapter>() {

            override fun areItemsTheSame(
                oldItem: BookChapter,
                newItem: BookChapter
            ): Boolean {
                return oldItem.index == newItem.index
            }

            override fun areContentsTheSame(
                oldItem: BookChapter,
                newItem: BookChapter
            ): Boolean {
                return oldItem.bookUrl == newItem.bookUrl
                        && oldItem.url == newItem.url
                        && oldItem.isVip == newItem.isVip
                        && oldItem.isPay == newItem.isPay
                        && oldItem.title == newItem.title
                        && oldItem.tag == newItem.tag
                        && oldItem.wordCount == newItem.wordCount
                        && oldItem.isVolume == newItem.isVolume
            }

        }

    private var upDisplayTileJob: Coroutine<*>? = null

    override fun onCurrentListChanged() {
        super.onCurrentListChanged()
        callback.onListChanged()
    }

    fun clearDisplayTitle() {
        upDisplayTileJob?.cancel()
        displayTitleMap.clear()
    }

    fun upDisplayTitles(startIndex: Int) {
        upDisplayTileJob?.cancel()
        upDisplayTileJob = Coroutine.async(callback.scope) {
            val book = callback.book ?: return@async
            val replaceRules = ContentProcessor.get(book.name, book.origin).getTitleReplaceRules()
            val useReplace = AppConfig.tocUiUseReplace && book.getUseReplaceRule()
            val items = getItems()
            launch {
                for (i in startIndex until items.size) {
                    val item = items[i]
                    if (displayTitleMap[item.title] == null) {
                        ensureActive()
                        val displayTitle = item.getDisplayTitle(replaceRules, useReplace)
                        ensureActive()
                        displayTitleMap[item.title] = displayTitle
                        handler.post {
                            notifyItemChanged(i, true)
                        }
                    }
                }
            }
            launch {
                for (i in startIndex downTo 0) {
                    val item = items[i]
                    if (displayTitleMap[item.title] == null) {
                        ensureActive()
                        val displayTitle = item.getDisplayTitle(replaceRules, useReplace)
                        ensureActive()
                        displayTitleMap[item.title] = displayTitle
                        handler.post {
                            notifyItemChanged(i, true)
                        }
                    }
                }
            }
        }
    }

    private fun getDisplayTitle(chapter: BookChapter): String {
        return displayTitleMap[chapter.title] ?: chapter.title
    }

    override fun getViewBinding(parent: ViewGroup): ItemChapterListBinding {
        return ItemChapterListBinding.inflate(inflater, parent, false)
    }

    override fun convert(
        holder: ItemViewHolder,
        binding: ItemChapterListBinding,
        item: BookChapter,
        payloads: MutableList<Any>
    ) {
        binding.run {
            val isDur = callback.durChapterIndex() == item.index
            val cached = callback.isLocalBook
                    || item.isVolume
                    || cacheFileNames.contains(item.getFileName())
            if (payloads.isEmpty()) {
                if (isDur) {
                    tvChapterName.setTextColor(context.themeColor(androidx.appcompat.R.attr.colorPrimary))
                    tvChapterItem.setBackgroundColor(context.themeColor(com.google.android.material.R.attr.colorSurfaceContainer))
                } else {
                    tvChapterName.setTextColor(context.themeColor(com.google.android.material.R.attr.colorOnSurface))
                    tvChapterItem.setBackgroundColor(context.themeColor(com.google.android.material.R.attr.colorSurface))
                }
                tvChapterName.text = getDisplayTitle(item)
                if (item.isVolume) {
                    tvChapterItem.setBackgroundColor(context.themeColor(com.google.android.material.R.attr.colorSecondaryContainer))
                } else {
                    tvChapterItem.foreground =
                        ThemeUtils.resolveDrawable(context, android.R.attr.selectableItemBackground)
                }

                //卷名不显示
                if (!item.tag.isNullOrEmpty() && !item.isVolume) {
                    //更新时间规则
                    tvTag.text = item.tag
                    tvTag.visible()
                } else {
                    tvTag.gone()
                }
                if (AppConfig.tocCountWords && !item.wordCount.isNullOrEmpty() && !item.isVolume) {
                    //章节字数
                    tvWordCount.text = item.wordCount
                    tvWordCount.visible()
                } else {
                    tvWordCount.gone()
                }

                if (item.isVip && !item.isPay) {
                    ivLocked.visible()
                } else {
                    ivLocked.gone()
                }

                upHasCache(binding, isDur, cached)
            } else {
                tvChapterName.text = getDisplayTitle(item)
                upHasCache(binding, isDur, cached)
            }
        }
    }

    override fun registerListener(holder: ItemViewHolder, binding: ItemChapterListBinding) {
        holder.itemView.setOnClickListener {
            getItem(holder.layoutPosition)?.let {
                callback.openChapter(it)
            }
        }
        holder.itemView.setOnLongClickListener {
            getItem(holder.layoutPosition)?.let { item ->
                context.longToastOnUi(getDisplayTitle(item))
            }
            true
        }
    }

    private fun upHasCache(binding: ItemChapterListBinding, isDur: Boolean, cached: Boolean) =
        binding.apply {
            when {
                isDur -> ivChecked.gone()
                cached -> {
                    ivChecked.setImageResource(R.drawable.ic_check)
                    ivChecked.visible()
                }

                else -> {
                    ivChecked.setImageResource(R.drawable.ic_outline_cloud_24)
                    ivChecked.visible()
                }
            }
        }


    interface Callback {
        val scope: CoroutineScope
        val book: Book?
        val isLocalBook: Boolean
        fun openChapter(bookChapter: BookChapter)
        fun durChapterIndex(): Int
        fun onListChanged()
    }

}
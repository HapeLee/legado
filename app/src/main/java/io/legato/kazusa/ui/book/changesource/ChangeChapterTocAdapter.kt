package io.legato.kazusa.ui.book.changesource

import android.content.Context
import android.view.ViewGroup
import io.legato.kazusa.R
import io.legato.kazusa.base.adapter.ItemViewHolder
import io.legato.kazusa.base.adapter.RecyclerAdapter
import io.legato.kazusa.data.entities.BookChapter
import io.legato.kazusa.databinding.ItemChapterListBinding
import io.legato.kazusa.lib.theme.ThemeUtils
//import io.legado.app.lib.theme.accentColor
import io.legato.kazusa.utils.getCompatColor
import io.legato.kazusa.utils.gone
import io.legato.kazusa.utils.themeColor
import io.legato.kazusa.utils.visible

class ChangeChapterTocAdapter(context: Context, val callback: Callback) :
    RecyclerAdapter<BookChapter, ItemChapterListBinding>(context) {

    var durChapterIndex = 0

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
            val isDur = durChapterIndex == item.index

            if (isDur) {
                tvChapterName.setTextColor(context.themeColor(com.google.android.material.R.attr.colorPrimary))
            } else {
                tvChapterName.setTextColor(context.themeColor(com.google.android.material.R.attr.colorOnSurface))
            }

            tvChapterName.text = item.title
            if (item.isVolume) {
                //卷名，如第一卷 突出显示
                tvChapterItem.setBackgroundColor(context.getCompatColor(R.color.btn_bg_press))
            } else {
                //普通章节 保持不变
                tvChapterItem.background =
                    ThemeUtils.resolveDrawable(context, android.R.attr.selectableItemBackground)
            }
            if (!item.tag.isNullOrEmpty() && !item.isVolume) {
                //卷名不显示tag(更新时间规则)
                tvTag.text = item.tag
                tvTag.visible()
            } else {
                tvTag.gone()
            }
            ivChecked.setImageResource(R.drawable.ic_check)
            ivChecked.visible(isDur)
        }
    }

    override fun registerListener(holder: ItemViewHolder, binding: ItemChapterListBinding) {
        holder.itemView.setOnClickListener {
            getItem(holder.layoutPosition)?.let {
                callback.clickChapter(it, getItem(holder.layoutPosition + 1)?.url)
            }
        }
    }

    interface Callback {
        fun clickChapter(bookChapter: BookChapter, nextChapterUrl: String?)
    }
}
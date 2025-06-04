package io.legato.kazusa.ui.main.bookshelf.style1.books

import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import io.legato.kazusa.base.adapter.ItemViewHolder
import io.legato.kazusa.data.entities.Book
import io.legato.kazusa.databinding.ItemBookshelfGridBinding
import io.legato.kazusa.help.book.isLocal
import io.legato.kazusa.help.config.AppConfig

class BooksAdapterGrid(context: Context, private val callBack: CallBack) :
    BaseBooksAdapter<ItemBookshelfGridBinding>(context) {

    override fun getViewBinding(parent: ViewGroup): ItemBookshelfGridBinding {
        return ItemBookshelfGridBinding.inflate(inflater, parent, false)
    }

    override fun convert(
        holder: ItemViewHolder,
        binding: ItemBookshelfGridBinding,
        item: Book,
        payloads: MutableList<Any>
    ) = binding.run {
        if (payloads.isEmpty()) {
            tvName.text = item.name
            ivCover.load(item.getDisplayCover(), item.name, item.author, false, item.origin)
            upRefresh(binding, item)
        } else {
            for (i in payloads.indices) {
                val bundle = payloads[i] as Bundle
                bundle.keySet().forEach {
                    when (it) {
                        "name" -> tvName.text = item.name
                        "cover" -> ivCover.load(item.getDisplayCover(), item.name, item.author, false, item.origin)
                        "refresh" -> upRefresh(binding, item)
                    }
                }
            }
        }
    }

    private fun upRefresh(binding: ItemBookshelfGridBinding, item: Book) {
        if (!item.isLocal && callBack.isUpdate(item.bookUrl)) {
            binding.cdUnread.visibility = View.GONE
            binding.rlLoading.visibility = View.VISIBLE
        } else {
            binding.rlLoading.visibility = View.GONE
            if (AppConfig.showUnread) {
                val unreadCount = item.getUnreadChapterNum()
                if (unreadCount > 0) {
                    binding.cdUnread.visibility = View.VISIBLE
                    binding.tvUnread.text = unreadCount.toString()
                } else {
                    binding.cdUnread.visibility = View.GONE
                }
            } else {
                binding.cdUnread.visibility = View.GONE
            }
        }
    }

    override fun registerListener(holder: ItemViewHolder, binding: ItemBookshelfGridBinding) {
        binding.cvContent.setOnClickListener {
            getItem(holder.layoutPosition)?.let {
                callBack.open(it)
            }
        }

        binding.cvContent.setOnLongClickListener {
            getItem(holder.layoutPosition)?.let {
                callBack.openBookInfo(it)
            }
            true
        }
    }
}
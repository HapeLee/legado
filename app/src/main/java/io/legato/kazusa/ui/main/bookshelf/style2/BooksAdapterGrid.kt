package io.legato.kazusa.ui.main.bookshelf.style2

import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import io.legato.kazusa.data.entities.Book
import io.legato.kazusa.data.entities.BookGroup
import io.legato.kazusa.databinding.ItemBookshelfGridBinding
import io.legato.kazusa.databinding.ItemBookshelfGridGroupBinding
import io.legato.kazusa.help.book.isLocal
import io.legato.kazusa.help.config.AppConfig
import splitties.views.onLongClick

@Suppress("UNUSED_PARAMETER")
class BooksAdapterGrid(context: Context, callBack: CallBack) :
    BaseBooksAdapter<RecyclerView.ViewHolder>(context, callBack) {

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): RecyclerView.ViewHolder {
        return when (viewType) {
            1 -> GroupViewHolder(ItemBookshelfGridGroupBinding.inflate(inflater, parent, false))
            else -> BookViewHolder(ItemBookshelfGridBinding.inflate(inflater, parent, false))
        }
    }

    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        position: Int,
        payloads: MutableList<Any>
    ) {
        when (holder) {
            is BookViewHolder -> (getItem(position) as? Book)?.let {
                holder.registerListener(it)
                holder.onBind(it, position, payloads)
            }

            is GroupViewHolder -> (getItem(position) as? BookGroup)?.let {
                holder.registerListener(it)
                holder.onBind(it, position, payloads)
            }
        }
    }

    inner class BookViewHolder(val binding: ItemBookshelfGridBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun onBind(item: Book, position: Int) = binding.run {
            tvName.text = item.name
            ivCover.load(item.getDisplayCover(), item.name, item.author, false, item.origin)
            upRefresh(this, item)
        }

        fun onBind(item: Book, position: Int, payloads: MutableList<Any>) = binding.run {
            if (payloads.isEmpty()) {
                onBind(item, position)
            } else {
                for (i in payloads.indices) {
                    val bundle = payloads[i] as Bundle
                    bundle.keySet().forEach {
                        when (it) {
                            "name" -> tvName.text = item.name
                            "cover" -> ivCover.load(
                                item.getDisplayCover(),
                                item.name,
                                item.author,
                                false,
                                item.origin
                            )

                            "refresh" -> upRefresh(this, item)
                        }
                    }
                }
            }
        }

        fun registerListener(item: Any) {
            binding.root.setOnClickListener {
                callBack.onItemClick(item)
            }
            binding.root.onLongClick {
                callBack.onItemLongClick(item)
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


    }

    inner class GroupViewHolder(val binding: ItemBookshelfGridGroupBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun onBind(item: BookGroup, position: Int) = binding.run {
            tvName.text = item.groupName
            ivCover.load(item.cover)
        }

        fun onBind(item: BookGroup, position: Int, payloads: MutableList<Any>) = binding.run {
            if (payloads.isEmpty()) {
                onBind(item, position)
            } else {
                for (i in payloads.indices) {
                    val bundle = payloads[i] as Bundle
                    bundle.keySet().forEach {
                        when (it) {
                            "groupName" -> tvName.text = item.groupName
                            "cover" -> ivCover.load(item.cover)
                        }
                    }
                }
            }
        }

        fun registerListener(item: Any) {
            binding.cvContent.setOnClickListener {
                callBack.onItemClick(item)
            }
            binding.cvContent.onLongClick {
                callBack.onItemLongClick(item)
            }
        }

    }

}
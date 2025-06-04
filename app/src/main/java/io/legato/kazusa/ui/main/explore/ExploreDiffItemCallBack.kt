package io.legato.kazusa.ui.main.explore

import androidx.recyclerview.widget.DiffUtil
import io.legato.kazusa.data.entities.BookSourcePart


class ExploreDiffItemCallBack : DiffUtil.ItemCallback<BookSourcePart>() {

    override fun areItemsTheSame(oldItem: BookSourcePart, newItem: BookSourcePart): Boolean {
        return oldItem == newItem
    }

    override fun areContentsTheSame(oldItem: BookSourcePart, newItem: BookSourcePart): Boolean {
        return oldItem.bookSourceName == newItem.bookSourceName
    }

}
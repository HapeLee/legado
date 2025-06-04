package io.legato.kazusa.ui.book.read.config

import android.content.Context
import android.view.ViewGroup
import io.legato.kazusa.base.adapter.ItemViewHolder
import io.legato.kazusa.base.adapter.RecyclerAdapter
import io.legato.kazusa.constant.EventBus
import io.legato.kazusa.databinding.ItemBgImageBinding
import io.legato.kazusa.help.config.ReadBookConfig
import io.legato.kazusa.help.glide.ImageLoader
import io.legato.kazusa.utils.postEvent
import java.io.File

class BgAdapter(context: Context, val textColor: Int) :
    RecyclerAdapter<String, ItemBgImageBinding>(context) {

    override fun getViewBinding(parent: ViewGroup): ItemBgImageBinding {
        return ItemBgImageBinding.inflate(inflater, parent, false)
    }

    override fun convert(
        holder: ItemViewHolder,
        binding: ItemBgImageBinding,
        item: String,
        payloads: MutableList<Any>
    ) {
        binding.run {
            ImageLoader.load(
                context,
                context.assets.open("bg${File.separator}$item").readBytes()
            )
                .centerCrop()
                .into(ivBg)
            tvName.setTextColor(textColor)
            tvName.text = item.substringBeforeLast(".")
        }
    }

    override fun registerListener(holder: ItemViewHolder, binding: ItemBgImageBinding) {
        holder.itemView.apply {
            this.setOnClickListener {
                getItemByLayoutPosition(holder.layoutPosition)?.let {
                    ReadBookConfig.durConfig.setCurBg(1, it)
                    postEvent(EventBus.UP_CONFIG, arrayListOf(1))
                }
            }
        }
    }
}
package io.legato.kazusa.ui.rss.favorites

import android.content.Context
import android.graphics.drawable.Drawable
import android.view.ViewGroup
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.Target
import io.legato.kazusa.base.adapter.ItemViewHolder
import io.legato.kazusa.base.adapter.RecyclerAdapter
import io.legato.kazusa.data.entities.RssStar
import io.legato.kazusa.databinding.ItemRssArticleBinding
import io.legato.kazusa.help.glide.ImageLoader
import io.legato.kazusa.help.glide.OkHttpModelLoader
import io.legato.kazusa.utils.gone
import io.legato.kazusa.utils.visible


class RssFavoritesAdapter(context: Context, val callBack: CallBack) :
    RecyclerAdapter<RssStar, ItemRssArticleBinding>(context) {

    override fun getViewBinding(parent: ViewGroup): ItemRssArticleBinding {
        return ItemRssArticleBinding.inflate(inflater, parent, false)
    }

    override fun convert(
        holder: ItemViewHolder,
        binding: ItemRssArticleBinding,
        item: RssStar,
        payloads: MutableList<Any>
    ) {
        binding.run {
            tvTitle.text = item.title
            tvPubDate.text = item.pubDate
            if (item.image.isNullOrBlank()) {
                imageView.gone()
            } else {
                val options =
                    RequestOptions().set(OkHttpModelLoader.sourceOriginOption, item.origin)
                ImageLoader.load(context, item.image)
                    .apply(options)
                    .addListener(object : RequestListener<Drawable> {
                        override fun onLoadFailed(
                            e: GlideException?,
                            model: Any?,
                            target: Target<Drawable>,
                            isFirstResource: Boolean
                        ): Boolean {
                            imageView.gone()
                            return false
                        }

                        override fun onResourceReady(
                            resource: Drawable,
                            model: Any,
                            target: Target<Drawable>?,
                            dataSource: DataSource,
                            isFirstResource: Boolean
                        ): Boolean {
                            imageView.visible()
                            return false
                        }

                    })
                    .into(imageView)
            }
        }
    }

    override fun registerListener(holder: ItemViewHolder, binding: ItemRssArticleBinding) {
        holder.itemView.setOnClickListener {
            getItem(holder.layoutPosition)?.let {
                callBack.readRss(it)
            }
        }
        holder.itemView.setOnLongClickListener {
            getItem(holder.layoutPosition)?.let {
                callBack.delStar(it)
            }
            true
        }
    }

    interface CallBack {
        fun readRss(rssStar: RssStar)

        fun delStar(rssStar: RssStar)
    }
}
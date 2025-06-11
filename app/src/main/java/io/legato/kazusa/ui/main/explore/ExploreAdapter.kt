package io.legato.kazusa.ui.main.explore

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.children
import com.google.android.flexbox.FlexboxLayout
import io.legato.kazusa.R
import io.legato.kazusa.base.adapter.ItemViewHolder
import io.legato.kazusa.base.adapter.RecyclerAdapter
import io.legato.kazusa.data.entities.BookSourcePart
import io.legato.kazusa.data.entities.rule.ExploreKind
import io.legato.kazusa.databinding.ItemFilletTextBinding
import io.legato.kazusa.databinding.ItemFindBookBinding
import io.legato.kazusa.help.coroutine.Coroutine
import io.legato.kazusa.help.source.clearExploreKindsCache
import io.legato.kazusa.help.source.exploreKinds
//import io.legado.app.lib.theme.accentColor
import io.legato.kazusa.ui.login.SourceLoginActivity
import io.legato.kazusa.ui.widget.dialog.TextDialog
import io.legato.kazusa.utils.activity
import io.legato.kazusa.utils.gone
import io.legato.kazusa.utils.removeLastElement
import io.legato.kazusa.utils.showDialogFragment
import io.legato.kazusa.utils.startActivity
import io.legato.kazusa.utils.visible
import kotlinx.coroutines.CoroutineScope
import splitties.views.onLongClick

class ExploreAdapter(context: Context, val callBack: CallBack) :
    RecyclerAdapter<BookSourcePart, ItemFindBookBinding>(context) {

    private val recycler = arrayListOf<View>()
    private var exIndex = -1
    private var scrollTo = -1

    override fun getViewBinding(parent: ViewGroup): ItemFindBookBinding {
        return ItemFindBookBinding.inflate(inflater, parent, false)
    }

    override fun convert(
        holder: ItemViewHolder,
        binding: ItemFindBookBinding,
        item: BookSourcePart,
        payloads: MutableList<Any>
    ) {
        binding.run {
            if (payloads.isEmpty()) {
                tvName.text = item.bookSourceName
            }

            if (exIndex == holder.layoutPosition) {
                ivStatus.icon = ContextCompat.getDrawable(context, R.drawable.ic_arrow_right)
                ivStatus.rotation = 90f

                if (scrollTo >= 0) {
                    callBack.scrollTo(scrollTo)
                }

                Coroutine.async(callBack.scope) {
                    item.exploreKinds()
                }.onSuccess { kindList ->
                    upKindList(flexbox, item.bookSourceUrl, kindList)
                }.onFinally {

                    if (scrollTo >= 0) {
                        callBack.scrollTo(scrollTo)
                        scrollTo = -1
                    }
                }
            } else {
                ivStatus.icon = ContextCompat.getDrawable(context, R.drawable.ic_arrow_right)
                ivStatus.rotation = 0f
                recyclerFlexbox(flexbox)
                flexbox.gone()
            }
        }
    }

    private fun upKindList(flexbox: FlexboxLayout, sourceUrl: String, kinds: List<ExploreKind>) {
        if (kinds.isNotEmpty()) kotlin.runCatching {
            recyclerFlexbox(flexbox)
            flexbox.visible()
            kinds.forEach { kind ->
                val tv = getFlexboxChild(flexbox)
                flexbox.addView(tv)
                tv.text = kind.title
                kind.style().apply(tv)
                if (kind.url.isNullOrBlank()) {
                    tv.setOnClickListener(null)
                } else {
                    tv.setOnClickListener {
                        if (kind.title.startsWith("ERROR:")) {
                            it.activity?.showDialogFragment(TextDialog("ERROR", kind.url))
                        } else {
                            callBack.openExplore(sourceUrl, kind.title, kind.url)
                        }
                    }
                }
            }
        }
    }

    @Synchronized
    private fun getFlexboxChild(flexbox: FlexboxLayout): TextView {
        return if (recycler.isEmpty()) {
            ItemFilletTextBinding.inflate(inflater, flexbox, false).root
        } else {
            recycler.removeLastElement() as TextView
        }
    }

    @Synchronized
    private fun recyclerFlexbox(flexbox: FlexboxLayout) {
        recycler.addAll(flexbox.children)
        flexbox.removeAllViews()
    }

    override fun registerListener(holder: ItemViewHolder, binding: ItemFindBookBinding) {
        binding.apply {
            llTitle.setOnClickListener {
                val position = holder.layoutPosition
                val oldEx = exIndex
                exIndex = if (exIndex == position) -1 else position
                notifyItemChanged(oldEx, false)
                if (exIndex != -1) {
                    scrollTo = position
                    callBack.scrollTo(position)
                    notifyItemChanged(position, false)
                }
            }
            llTitle.onLongClick {
                showMenu(llTitle, holder.layoutPosition)
            }
        }
    }

    fun compressExplore(): Boolean {
        return if (exIndex < 0) {
            false
        } else {
            val oldExIndex = exIndex
            exIndex = -1
            notifyItemChanged(oldExIndex)
            true
        }
    }

    private fun showMenu(view: View, position: Int): Boolean {
        val source = getItem(position) ?: return true
        val popupMenu = PopupMenu(context, view)
        popupMenu.inflate(R.menu.explore_item)
        popupMenu.menu.findItem(R.id.menu_login).isVisible = source.hasLoginUrl
        popupMenu.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.menu_edit -> callBack.editSource(source.bookSourceUrl)
                R.id.menu_top -> callBack.toTop(source)
                R.id.menu_search -> callBack.searchBook(source)
                R.id.menu_login -> context.startActivity<SourceLoginActivity> {
                    putExtra("type", "bookSource")
                    putExtra("key", source.bookSourceUrl)
                }

                R.id.menu_refresh -> Coroutine.async(callBack.scope) {
                    source.clearExploreKindsCache()
                }.onSuccess {
                    notifyItemChanged(position)
                }

                R.id.menu_del -> callBack.deleteSource(source)
            }
            true
        }
        popupMenu.show()
        return true
    }

    interface CallBack {
        val scope: CoroutineScope
        fun scrollTo(pos: Int)
        fun openExplore(sourceUrl: String, title: String, exploreUrl: String?)
        fun editSource(sourceUrl: String)
        fun toTop(source: BookSourcePart)
        fun deleteSource(source: BookSourcePart)
        fun searchBook(bookSource: BookSourcePart)
    }
}

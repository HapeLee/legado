package io.legato.kazusa.ui.book.source.manage

//import io.legado.app.lib.theme.backgroundColor
import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.core.os.bundleOf
import androidx.core.view.doOnLayout
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import io.legato.kazusa.R
import io.legato.kazusa.base.adapter.ItemViewHolder
import io.legato.kazusa.base.adapter.RecyclerAdapter
import io.legato.kazusa.data.entities.BookSourcePart
import io.legato.kazusa.databinding.ItemBookSourceBinding
import io.legato.kazusa.model.Debug
import io.legato.kazusa.ui.login.SourceLoginActivity
import io.legato.kazusa.ui.widget.recycler.DragSelectTouchHelper
import io.legato.kazusa.ui.widget.recycler.ItemTouchCallback
import io.legato.kazusa.utils.buildMainHandler
import io.legato.kazusa.utils.gone
import io.legato.kazusa.utils.invisible
import io.legato.kazusa.utils.startActivity
import io.legato.kazusa.utils.themeColor
import io.legato.kazusa.utils.visible
import java.util.Collections


class BookSourceAdapter(
    context: Context,
    private val callBack: CallBack,
    private val recyclerView: RecyclerView
) : RecyclerAdapter<BookSourcePart, ItemBookSourceBinding>(context),
    ItemTouchCallback.Callback {

    private val selected = linkedSetOf<BookSourcePart>()
    private val finalMessageRegex = Regex("成功|失败")
    private val handler = buildMainHandler()
    var showSourceHost = false

    val selection: List<BookSourcePart>
        get() {
            return getItems().filter {
                selected.contains(it)
            }
        }

    val diffItemCallback = object : DiffUtil.ItemCallback<BookSourcePart>() {

        override fun areItemsTheSame(oldItem: BookSourcePart, newItem: BookSourcePart): Boolean {
            return oldItem.bookSourceUrl == newItem.bookSourceUrl
        }

        override fun areContentsTheSame(oldItem: BookSourcePart, newItem: BookSourcePart): Boolean {
            return oldItem.bookSourceName == newItem.bookSourceName
                    && oldItem.bookSourceGroup == newItem.bookSourceGroup
                    && oldItem.enabled == newItem.enabled
                    && oldItem.enabledExplore == newItem.enabledExplore
                    && oldItem.hasExploreUrl == newItem.hasExploreUrl
        }

        override fun getChangePayload(oldItem: BookSourcePart, newItem: BookSourcePart): Any? {
            val payload = Bundle()
            if (oldItem.bookSourceName != newItem.bookSourceName
                || oldItem.bookSourceGroup != newItem.bookSourceGroup
            ) {
                payload.putBoolean("upName", true)
            }
            if (oldItem.enabled != newItem.enabled) {
                payload.putBoolean("enabled", newItem.enabled)
            }
            if (oldItem.enabledExplore != newItem.enabledExplore ||
                oldItem.hasExploreUrl != newItem.hasExploreUrl
            ) {
                payload.putBoolean("upExplore", true)
            }
            if (payload.isEmpty) {
                return null
            }
            return payload
        }

    }

    override fun getViewBinding(parent: ViewGroup): ItemBookSourceBinding {
        return ItemBookSourceBinding.inflate(inflater, parent, false)
    }

    override fun convert(
        holder: ItemViewHolder,
        binding: ItemBookSourceBinding,
        item: BookSourcePart,
        payloads: MutableList<Any>
    ) {
        binding.run {
            if (payloads.isEmpty()) {
                //root.setBackgroundColor(ColorUtils.withAlpha(context.backgroundColor, 0.5f))
                cbBookSource.text = item.getDisPlayNameGroup()
                swtEnabled.isChecked = item.enabled
                cbBookSource.isChecked = selected.contains(item)
                upCheckSourceMessage(binding, item)
                upShowExplore(ivExplore, item)
                upSourceHost(binding, holder.layoutPosition)
            } else {
                for (i in payloads.indices) {
                    val bundle = payloads[i] as Bundle
                    bundle.keySet().forEach {
                        when (it) {
                            "enabled" -> swtEnabled.isChecked = bundle.getBoolean("enabled")
                            "upName" -> cbBookSource.text = item.getDisPlayNameGroup()
                            "upExplore" -> upShowExplore(ivExplore, item)
                            "selected" -> cbBookSource.isChecked = selected.contains(item)
                            "checkSourceMessage" -> upCheckSourceMessage(binding, item)
                            "upSourceHost" -> upSourceHost(binding, holder.layoutPosition)
                        }
                    }
                }
            }
        }
    }

    override fun registerListener(holder: ItemViewHolder, binding: ItemBookSourceBinding) {
        binding.apply {
            swtEnabled.setOnCheckedChangeListener { view, checked ->
                getItem(holder.layoutPosition)?.let {
                    if (view.isPressed) {
                        it.enabled = checked
                        callBack.enable(checked, it)
                    }
                }
            }
            cbBookSource.setOnCheckedChangeListener { view, checked ->
                getItem(holder.layoutPosition)?.let {
                    if (view.isPressed) {
                        if (checked) {
                            selected.add(it)
                        } else {
                            selected.remove(it)
                        }
                        callBack.upCountView()
                    }
                }
            }
            ivEdit.setOnClickListener {
                getItem(holder.layoutPosition)?.let {
                    callBack.edit(it)
                }
            }
            ivMenuMore.setOnClickListener {
                showMenu(ivMenuMore, holder.layoutPosition)
            }
        }
    }

    override fun onCurrentListChanged() {
        callBack.upCountView()
        recyclerView.doOnLayout {
            handler.post {
                notifyItemRangeChanged(0, itemCount, bundleOf("upSourceHost" to null))
            }
        }
    }

    private fun showMenu(view: View, position: Int) {
        val source = getItem(position) ?: return
        val popupMenu = PopupMenu(context, view)
        popupMenu.inflate(R.menu.book_source_item)
        popupMenu.menu.findItem(R.id.menu_top).isVisible = callBack.sort == BookSourceSort.Default
        popupMenu.menu.findItem(R.id.menu_bottom).isVisible =
            callBack.sort == BookSourceSort.Default
        val qyMenu = popupMenu.menu.findItem(R.id.menu_enable_explore)
        if (!source.hasExploreUrl) {
            qyMenu.isVisible = false
        } else {
            if (source.enabledExplore) {
                qyMenu.setTitle(R.string.disable_explore)
            } else {
                qyMenu.setTitle(R.string.enable_explore)
            }
        }
        val loginMenu = popupMenu.menu.findItem(R.id.menu_login)
        loginMenu.isVisible = source.hasLoginUrl
        popupMenu.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.menu_top -> callBack.toTop(source)
                R.id.menu_bottom -> callBack.toBottom(source)
                R.id.menu_login -> context.startActivity<SourceLoginActivity> {
                    putExtra("type", "bookSource")
                    putExtra("key", source.bookSourceUrl)
                }

                R.id.menu_search -> callBack.searchBook(source)
                R.id.menu_debug_source -> callBack.debug(source)
                R.id.menu_del -> {
                    callBack.del(source)
                    selected.remove(source)
                }

                R.id.menu_enable_explore -> {
                    callBack.enableExplore(!source.enabledExplore, source)
                }
            }
            true
        }
        popupMenu.show()
    }

    private fun upShowExplore(iv: MaterialCardView, source: BookSourcePart) {
        when {
            !source.hasExploreUrl -> {
                iv.invisible()
            }

            source.enabledExplore -> {
                iv.setCardBackgroundColor(context.themeColor(androidx.appcompat.R.attr.colorPrimary))
                iv.visible()
                iv.contentDescription = context.getString(R.string.tag_explore_enabled)
            }

            else -> {
                iv.setCardBackgroundColor(context.themeColor(com.google.android.material.R.attr.colorOnError))
                iv.visible()
                iv.contentDescription = context.getString(R.string.tag_explore_disabled)
            }
        }
    }

    private fun upCheckSourceMessage(
        binding: ItemBookSourceBinding,
        item: BookSourcePart
    ) = binding.run {
        val msg = Debug.debugMessageMap[item.bookSourceUrl] ?: ""
        ivDebugText.text = msg
        val isEmpty = msg.isEmpty()
        var isFinalMessage = msg.contains(finalMessageRegex)
        if (!Debug.isChecking && !isFinalMessage) {
            Debug.updateFinalMessage(item.bookSourceUrl, "校验失败")
            ivDebugText.text = Debug.debugMessageMap[item.bookSourceUrl] ?: ""
            isFinalMessage = true
        }
        ivDebugText.visibility =
            if (!isEmpty) View.VISIBLE else View.GONE
        ivProgressBar.visibility =
            if (isFinalMessage || isEmpty || !Debug.isChecking) View.GONE else View.VISIBLE
    }

    private fun upSourceHost(binding: ItemBookSourceBinding, position: Int) = binding.run {
        if (showSourceHost && isItemHeader(position)) {
            tvHostText.text = getHeaderText(position)
            tvHostText.visible()
        } else {
            tvHostText.gone()
        }
    }

    fun selectAll() {
        getItems().forEach {
            selected.add(it)
        }
        notifyItemRangeChanged(0, itemCount, bundleOf(Pair("selected", null)))
        callBack.upCountView()
    }

    fun revertSelection() {
        getItems().forEach {
            if (selected.contains(it)) {
                selected.remove(it)
            } else {
                selected.add(it)
            }
        }
        notifyItemRangeChanged(0, itemCount, bundleOf(Pair("selected", null)))
        callBack.upCountView()
    }

    fun checkSelectedInterval() {
        val selectedPosition = linkedSetOf<Int>()
        getItems().forEachIndexed { index, it ->
            if (selected.contains(it)) {
                selectedPosition.add(index)
            }
        }
        val minPosition = Collections.min(selectedPosition)
        val maxPosition = Collections.max(selectedPosition)
        val itemCount = maxPosition - minPosition + 1
        for (i in minPosition..maxPosition) {
            getItem(i)?.let {
                selected.add(it)
            }
        }
        notifyItemRangeChanged(minPosition, itemCount, bundleOf(Pair("selected", null)))
        callBack.upCountView()
    }

    fun getHeaderText(position: Int): String {
        val source = getItem(position)!!
        return callBack.getSourceHost(source.bookSourceUrl)
    }

    fun isItemHeader(position: Int): Boolean {
        if (position == 0) return true
        val lastHost = getHeaderText(position - 1)
        val curHost = getHeaderText(position)
        return lastHost != curHost
    }

    override fun swap(srcPosition: Int, targetPosition: Int): Boolean {
        val srcItem = getItem(srcPosition)
        val targetItem = getItem(targetPosition)
        if (srcItem != null && targetItem != null) {
            val srcOrder = srcItem.customOrder
            srcItem.customOrder = targetItem.customOrder
            targetItem.customOrder = srcOrder
            movedItems.add(srcItem)
            movedItems.add(targetItem)
        }
        swapItem(srcPosition, targetPosition)
        return true
    }

    private val movedItems = hashSetOf<BookSourcePart>()

    override fun onClearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
        if (movedItems.isNotEmpty()) {
            val sortNumberSet = hashSetOf<Int>()
            movedItems.forEach {
                sortNumberSet.add(it.customOrder)
            }
            if (movedItems.size > sortNumberSet.size) {
                callBack.upOrder(getItems().mapIndexed { index, bookSourcePart ->
                    bookSourcePart.customOrder = if (callBack.sortAscending) index else -index
                    bookSourcePart
                })
            } else {
                callBack.upOrder(movedItems.toList())
            }
            movedItems.clear()
        }
    }

    val dragSelectCallback: DragSelectTouchHelper.Callback =
        object : DragSelectTouchHelper.AdvanceCallback<BookSourcePart>(Mode.ToggleAndReverse) {
            override fun currentSelectedId(): MutableSet<BookSourcePart> {
                return selected
            }

            override fun getItemId(position: Int): BookSourcePart {
                return getItem(position)!!
            }

            override fun updateSelectState(position: Int, isSelected: Boolean): Boolean {
                getItem(position)?.let {
                    if (isSelected) {
                        selected.add(it)
                    } else {
                        selected.remove(it)
                    }
                    notifyItemChanged(position, bundleOf(Pair("selected", null)))
                    callBack.upCountView()
                    return true
                }
                return false
            }
        }

    interface CallBack {
        val sort: BookSourceSort
        val sortAscending: Boolean
        fun del(bookSource: BookSourcePart)
        fun edit(bookSource: BookSourcePart)
        fun toTop(bookSource: BookSourcePart)
        fun toBottom(bookSource: BookSourcePart)
        fun searchBook(bookSource: BookSourcePart)
        fun debug(bookSource: BookSourcePart)
        fun upOrder(items: List<BookSourcePart>)
        fun enable(enable: Boolean, bookSource: BookSourcePart)
        fun enableExplore(enable: Boolean, bookSource: BookSourcePart)
        fun upCountView()
        fun getSourceHost(origin: String): String
    }
}

package io.legato.kazusa.ui.book.search

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.SearchView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import io.legato.kazusa.R
import io.legato.kazusa.base.BaseDialogFragment
import io.legato.kazusa.base.adapter.ItemViewHolder
import io.legato.kazusa.constant.AppLog
import io.legato.kazusa.data.AppDatabase
import io.legato.kazusa.data.appDb
import io.legato.kazusa.data.entities.BookSourcePart
import io.legato.kazusa.databinding.DialogSearchScopeBinding
import io.legato.kazusa.databinding.ItemCheckBoxBinding
import io.legato.kazusa.databinding.ItemRadioButtonBinding
//import io.legado.app.lib.theme.primaryColor
import io.legato.kazusa.utils.applyTint
import io.legato.kazusa.utils.flowWithLifecycleAndDatabaseChange
import io.legato.kazusa.utils.setLayout
import io.legato.kazusa.utils.viewbindingdelegate.viewBinding
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SearchScopeDialog : BaseDialogFragment(R.layout.dialog_search_scope) {

    private val binding by viewBinding(DialogSearchScopeBinding::bind)
    private var sourceFlowJob: Job? = null
    val callback: Callback get() = parentFragment as? Callback ?: activity as Callback
    var groups: List<String> = emptyList()
    val screenSources = arrayListOf<BookSourcePart>()
    var screenText: String? = null

    val adapter by lazy {
        RecyclerAdapter()
    }

    override fun onStart() {
        super.onStart()
        setLayout(0.9f, 0.8f)
    }

    override fun onFragmentCreated(view: View, savedInstanceState: Bundle?) {
        //binding.toolBar.setBackgroundColor(primaryColor)
        binding.recyclerView.adapter = adapter
        initMenu()
        initSearchView()
        initOtherView()
        initData()
    }

    private fun initMenu() {
        binding.toolBar.inflateMenu(R.menu.book_search_scope)
        //binding.toolBar.menu.applyTint(requireContext())
    }

    private fun initSearchView() {
        val searchView = binding.toolBar.menu.findItem(R.id.menu_screen).actionView as SearchView
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                screenText = newText
                upData()
                return false
            }

        })
    }

    private fun initOtherView() {
        binding.rgScope.setOnCheckedChangeListener { _, checkedId ->
            binding.toolBar.menu.findItem(R.id.menu_screen)?.isVisible = checkedId == R.id.rb_source
            upData()
        }
        binding.tvCancel.setOnClickListener {
            dismiss()
        }
        binding.tvAllSource.setOnClickListener {
            callback.onSearchScopeOk(SearchScope(""))
            dismiss()
        }
        binding.tvOk.setOnClickListener {
            if (binding.rbGroup.isChecked) {
                callback.onSearchScopeOk(SearchScope(adapter.selectGroups))
            } else {
                val selectSource = adapter.selectSource
                if (selectSource != null) {
                    callback.onSearchScopeOk(SearchScope(selectSource))
                } else {
                    callback.onSearchScopeOk(SearchScope(""))
                }
            }
            dismiss()
        }
    }

    private fun initData() {
        lifecycleScope.launch {
            groups = withContext(IO) {
                appDb.bookSourceDao.allEnabledGroups()
            }
            upData()
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun upData() {
        if (binding.rbSource.isChecked) {
            upBookSource(screenText)
        } else {
            adapter.notifyDataSetChanged()
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun upBookSource(searchKey: String? = null) {
        sourceFlowJob?.cancel()
        sourceFlowJob = lifecycleScope.launch {
            when {
                searchKey.isNullOrEmpty() -> {
                    appDb.bookSourceDao.flowAll()
                }

                else -> {
                    appDb.bookSourceDao.flowSearch(searchKey)
                }
            }.flowWithLifecycleAndDatabaseChange(
                lifecycle,
                table = AppDatabase.BOOK_SOURCE_TABLE_NAME
            ).catch {
                AppLog.put("多分组/书源界面更新书源出错", it)
            }.flowOn(IO).conflate().collect { data ->
                screenSources.clear()
                screenSources.addAll(data)
                adapter.notifyDataSetChanged()
                delay(500)
            }
        }
    }

    inner class RecyclerAdapter : RecyclerView.Adapter<ItemViewHolder>() {

        val selectGroups = arrayListOf<String>()
        var selectSource: BookSourcePart? = null

        override fun getItemViewType(position: Int): Int {
            return if (binding.rbSource.isChecked) {
                1
            } else {
                0
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
            return if (viewType == 1) {
                ItemViewHolder(ItemRadioButtonBinding.inflate(layoutInflater, parent, false))
            } else {
                ItemViewHolder(ItemCheckBoxBinding.inflate(layoutInflater, parent, false))
            }
        }

        override fun onBindViewHolder(
            holder: ItemViewHolder,
            position: Int,
            payloads: MutableList<Any>
        ) {
            if (payloads.isEmpty()) {
                super.onBindViewHolder(holder, position, payloads)
            } else {
                when (holder.binding) {
                    is ItemCheckBoxBinding -> {
                        groups.getOrNull(position)?.let {
                            holder.binding.checkBox.isChecked = selectGroups.contains(it)
                            holder.binding.checkBox.text = it
                        }
                    }

                    is ItemRadioButtonBinding -> {
                        screenSources.getOrNull(position)?.let {
                            holder.binding.radioButton.isChecked = selectSource == it
                            holder.binding.radioButton.text = it.bookSourceName
                        }
                    }
                }
            }
        }

        override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
            when (holder.binding) {
                is ItemCheckBoxBinding -> {
                    groups.getOrNull(position)?.let {
                        holder.binding.checkBox.isChecked = selectGroups.contains(it)
                        holder.binding.checkBox.text = it
                        holder.binding.checkBox.setOnCheckedChangeListener { buttonView, isChecked ->
                            if (buttonView.isPressed) {
                                if (isChecked) {
                                    selectGroups.add(it)
                                } else {
                                    selectGroups.remove(it)
                                }
                                buttonView.post {
                                    notifyItemRangeChanged(0, itemCount, "up")
                                }
                            }
                        }
                    }
                }

                is ItemRadioButtonBinding -> {
                    screenSources.getOrNull(position)?.let {
                        holder.binding.radioButton.isChecked = selectSource == it
                        holder.binding.radioButton.text = it.bookSourceName
                        holder.binding.radioButton.setOnCheckedChangeListener { buttonView, isChecked ->
                            if (buttonView.isPressed) {
                                if (isChecked) {
                                    selectSource = it
                                }
                                buttonView.post {
                                    notifyItemRangeChanged(0, itemCount, "up")
                                }
                            }
                        }
                    }
                }
            }
        }

        override fun getItemCount(): Int {
            return if (binding.rbSource.isChecked) {
                screenSources.size
            } else {
                groups.size
            }
        }

    }

    interface Callback {

        /**
         * 搜索范围确认
         */
        fun onSearchScopeOk(searchScope: SearchScope)

    }

}
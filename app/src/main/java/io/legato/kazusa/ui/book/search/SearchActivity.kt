package io.legato.kazusa.ui.book.search

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.widget.SearchView
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.flexbox.FlexboxLayoutManager
import io.legato.kazusa.R
import io.legato.kazusa.base.VMBaseActivity
import io.legato.kazusa.constant.AppLog
import io.legato.kazusa.constant.PreferKey
import io.legato.kazusa.data.appDb
import io.legato.kazusa.data.entities.Book
import io.legato.kazusa.data.entities.SearchKeyword
import io.legato.kazusa.databinding.ActivityBookSearchBinding
import io.legato.kazusa.lib.dialogs.alert
//import io.legado.app.lib.theme.accentColor
//import io.legado.app.lib.theme.backgroundColor
//import io.legado.app.lib.theme.primaryColor
//import io.legado.app.lib.theme.primaryTextColor
import io.legato.kazusa.ui.about.AppLogDialog
import io.legato.kazusa.ui.book.info.BookInfoActivity
import io.legato.kazusa.ui.book.source.manage.BookSourceActivity
import io.legato.kazusa.utils.applyNavigationBarMargin
import io.legato.kazusa.utils.applyNavigationBarPadding
import io.legato.kazusa.utils.getPrefBoolean
import io.legato.kazusa.utils.gone
import io.legato.kazusa.utils.invisible
import io.legato.kazusa.utils.putPrefBoolean
import io.legato.kazusa.utils.showDialogFragment
import io.legato.kazusa.utils.startActivity
import io.legato.kazusa.utils.transaction
import io.legato.kazusa.utils.viewbindingdelegate.viewBinding
import io.legato.kazusa.utils.visible
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import splitties.init.appCtx
import kotlin.math.abs

class SearchActivity : VMBaseActivity<ActivityBookSearchBinding, SearchViewModel>(),
    BookAdapter.CallBack,
    HistoryKeyAdapter.CallBack,
    SearchScopeDialog.Callback,
    SearchAdapter.CallBack {

    override val binding by viewBinding(ActivityBookSearchBinding::inflate)
    override val viewModel by viewModels<SearchViewModel>()

    private val adapter by lazy { SearchAdapter(this, this) }
    private val bookAdapter by lazy {
        BookAdapter(this, this).apply {
            setHasStableIds(true)
        }
    }
    private val historyKeyAdapter by lazy {
        HistoryKeyAdapter(this, this).apply {
            setHasStableIds(true)
        }
    }
    private val searchView: SearchView by lazy {
        binding.titleBar.findViewById(R.id.search_view)
    }
    private var menu: Menu? = null
    private var groups: List<String>? = null
    private var historyFlowJob: Job? = null
    private var booksFlowJob: Job? = null
    private var precisionSearchMenuItem: MenuItem? = null
    private var isManualStopSearch = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //binding.llInputHelp.setBackgroundColor(backgroundColor)
        initRecyclerView()
        initSearchView()
        initOtherView()
        initData()
        receiptIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        receiptIntent(intent)
    }

    override fun onCompatCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.book_search, menu)
        this.menu = menu
        precisionSearchMenuItem = menu.findItem(R.id.menu_precision_search)
        precisionSearchMenuItem?.isChecked = getPrefBoolean(PreferKey.precisionSearch)
        return super.onCompatCreateOptionsMenu(menu)
    }

    override fun onMenuOpened(featureId: Int, menu: Menu): Boolean {
        menu.transaction {
            menu.removeGroup(R.id.menu_group_1)
            menu.removeGroup(R.id.menu_group_2)
            var hasChecked = false
            val searchScopeNames = viewModel.searchScope.displayNames
            if (viewModel.searchScope.isSource()) {
                menu.add(R.id.menu_group_1, Menu.NONE, Menu.NONE, searchScopeNames.first()).apply {
                    isChecked = true
                    hasChecked = true
                }
            }
            val allSourceMenu =
                menu.add(R.id.menu_group_2, R.id.menu_1, Menu.NONE, getString(R.string.all_source))
                    .apply {
                        if (searchScopeNames.isEmpty()) {
                            isChecked = true
                            hasChecked = true
                        }
                    }
            groups?.forEach {
                if (searchScopeNames.contains(it)) {
                    menu.add(R.id.menu_group_1, Menu.NONE, Menu.NONE, it).apply {
                        isChecked = true
                        hasChecked = true
                    }
                } else {
                    menu.add(R.id.menu_group_2, Menu.NONE, Menu.NONE, it)
                }
            }
            if (!hasChecked) {
                viewModel.searchScope.update("")
                allSourceMenu.isChecked = true
            }
            menu.setGroupCheckable(R.id.menu_group_1, true, false)
            menu.setGroupCheckable(R.id.menu_group_2, true, true)
        }
        return super.onMenuOpened(featureId, menu)
    }

    override fun onCompatOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_precision_search -> {
                putPrefBoolean(
                    PreferKey.precisionSearch,
                    !getPrefBoolean(PreferKey.precisionSearch)
                )
                precisionSearchMenuItem?.isChecked = getPrefBoolean(PreferKey.precisionSearch)
                searchView.query?.toString()?.trim()?.let {
                    searchView.setQuery(it, true)
                }
            }

            R.id.menu_search_scope -> alertSearchScope()
            R.id.menu_source_manage -> startActivity<BookSourceActivity>()
            R.id.menu_log -> showDialogFragment(AppLogDialog())
            R.id.menu_1 -> viewModel.searchScope.update("")
            else -> {
                if (item.groupId == R.id.menu_group_1) {
                    viewModel.searchScope.remove(item.title.toString())
                } else if (item.groupId == R.id.menu_group_2) {
                    viewModel.searchScope.update(item.title.toString())
                }
            }
        }
        return super.onCompatOptionsItemSelected(item)
    }

    private fun initSearchView() {
        //searchView.applyTint(primaryTextColor)
        searchView.isSubmitButtonEnabled = true
        searchView.queryHint = getString(R.string.search_book_key)
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String): Boolean {
                searchView.clearFocus()
                query.trim().let { searchKey ->
                    isManualStopSearch = false
                    viewModel.saveSearchKey(searchKey)
                    viewModel.searchKey = ""
                    viewModel.search(searchKey)
                }
                visibleInputHelp(false)
                return true
            }

            override fun onQueryTextChange(newText: String): Boolean {
                viewModel.stop()
                binding.fbStartStop.invisible()
                upHistory(newText.trim())
                return false
            }
        })
//        searchView.setOnQueryTextFocusChangeListener { _, hasFocus ->
//            if (binding.refreshProgressBar.isAutoLoading || (!hasFocus && adapter.isNotEmpty() && searchView.query.isNotBlank())) {
//                visibleInputHelp(false)
//            } else {
//                visibleInputHelp(true)
//            }
//        }
        visibleInputHelp(true)
    }

    private fun initRecyclerView() {
//        binding.recyclerView.setEdgeEffectColor(primaryColor)
//        binding.rvBookshelfSearch.setEdgeEffectColor(primaryColor)
//        binding.rvHistoryKey.setEdgeEffectColor(primaryColor)
        binding.rvBookshelfSearch.layoutManager = FlexboxLayoutManager(this)
        binding.rvBookshelfSearch.adapter = bookAdapter
        binding.rvBookshelfSearch.applyNavigationBarMargin()
        binding.rvHistoryKey.layoutManager = FlexboxLayoutManager(this)
        binding.rvHistoryKey.adapter = historyKeyAdapter
        binding.rvHistoryKey.applyNavigationBarMargin()
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter
        binding.recyclerView.itemAnimator = null
        binding.recyclerView.applyNavigationBarPadding()
        adapter.registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
            override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                super.onItemRangeInserted(positionStart, itemCount)
                if (positionStart == 0) {
                    binding.recyclerView.scrollToPosition(0)
                }
            }

            override fun onItemRangeMoved(fromPosition: Int, toPosition: Int, itemCount: Int) {
                super.onItemRangeMoved(fromPosition, toPosition, itemCount)
                if (toPosition == 0) {
                    binding.recyclerView.scrollToPosition(0)
                }
            }
        })
        binding.recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                if (!recyclerView.canScrollVertically(1)) {
                    val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                    val lastPosition = layoutManager.findLastCompletelyVisibleItemPosition()
                    if (lastPosition == RecyclerView.NO_POSITION) {
                        return
                    }
                    val lastView = layoutManager.findViewByPosition(lastPosition)
                    if (lastView == null) {
                        scrollToBottom()
                        return
                    }
                    val bottom = abs(lastView.bottom - recyclerView.height)
                    if (bottom <= 1) {
                        scrollToBottom()
                    }
                }
            }
        })
    }

    private fun initOtherView() {
//        binding.fbStartStop.backgroundTintList =
//            Selector.colorBuild()
//                .setDefaultColor(accentColor)
//                .setPressedColor(ColorUtils.darkenColor(accentColor))
//                .create()
        binding.fbStartStop.setOnClickListener {
            if (viewModel.isSearchLiveData.value == true) {
                isManualStopSearch = true
                viewModel.stop()
                //binding.refreshProgressBar.visibility = GONE
            } else {
                viewModel.search("")
            }
        }
        binding.tvClearHistory.setOnClickListener { alertClearHistory() }
    }

    private fun initData() {
        viewModel.searchScope.stateLiveData.observe(this) {
            if (!binding.llInputHelp.isVisible) {
                searchView.query?.toString()?.trim()?.let {
                    searchView.setQuery(it, true)
                }
            }
        }
        viewModel.isSearchLiveData.observe(this) {
            if (it) {
                startSearch()
            } else {
                searchFinally()
            }
        }
        viewModel.searchBookLiveData.observe(this) {
            adapter.setItems(it)
        }
        lifecycleScope.launch {
            appDb.bookSourceDao.flowEnabledGroups().collect {
                groups = it
            }
        }
    }

    /**
     * 处理传入数据
     */
    private fun receiptIntent(intent: Intent? = null) {
        val searchScope = intent?.getStringExtra("searchScope")
        searchScope?.let {
            viewModel.searchScope.update(searchScope, false)
        }
        val key = intent?.getStringExtra("key")
        if (key.isNullOrBlank()) {
            searchView.findViewById<TextView>(androidx.appcompat.R.id.search_src_text)
                .requestFocus()
        } else {
            searchView.setQuery(key, true)
        }
    }

    /**
     * 滚动到底部事件
     */
    private fun scrollToBottom() {
        if (isManualStopSearch) {
            return
        }
        if (viewModel.isSearchLiveData.value == false
            && viewModel.searchKey.isNotEmpty()
            && viewModel.hasMore
        ) {
            viewModel.search("")
        }
    }

    /**
     * 打开关闭输入帮助
     */
    private fun visibleInputHelp(visible: Boolean) {
        if (visible) {
            upHistory(searchView.query.toString())
            binding.llInputHelp.visibility = VISIBLE
        } else {
            binding.llInputHelp.visibility = GONE
        }
    }

    /**
     * 更新搜索历史
     */
    private fun upHistory(key: String? = null) {
        booksFlowJob?.cancel()
        booksFlowJob = lifecycleScope.launch {
            if (key.isNullOrBlank()) {
                binding.tvBookShow.gone()
                binding.rvBookshelfSearch.gone()
            } else {
                appDb.bookDao.flowSearch(key).conflate().collect {
                    if (it.isEmpty()) {
                        binding.tvBookShow.gone()
                        binding.rvBookshelfSearch.gone()
                    } else {
                        binding.tvBookShow.visible()
                        binding.rvBookshelfSearch.visible()
                    }
                    bookAdapter.setItems(it)
                }
            }
        }
        historyFlowJob?.cancel()
        historyFlowJob = lifecycleScope.launch {
            when {
                key.isNullOrBlank() -> appDb.searchKeywordDao.flowByTime()
                else -> appDb.searchKeywordDao.flowSearch(key)
            }.catch {
                AppLog.put("搜索界面获取搜索历史数据失败\n${it.localizedMessage}", it)
            }.flowOn(IO).conflate().collect {
                historyKeyAdapter.setItems(it)
                if (it.isEmpty()) {
                    binding.tvClearHistory.invisible()
                } else {
                    binding.tvClearHistory.visible()
                }
            }
        }
    }

    /**
     * 开始搜索
     */
    private fun startSearch() {
        binding.refreshProgressBar.visible()
        //binding.refreshProgressBar.isAutoLoading = true
        binding.fbStartStop.setImageResource(R.drawable.ic_stop_black_24dp)
        binding.fbStartStop.visible()
    }

    /**
     * 搜索结束
     */
    private fun searchFinally() {
        //binding.refreshProgressBar.isAutoLoading = false
        binding.refreshProgressBar.gone()
        if (!isManualStopSearch && viewModel.hasMore) {
            binding.fbStartStop.setImageResource(R.drawable.ic_play_24dp)
        } else {
            binding.fbStartStop.invisible()
        }
    }

    override fun observeLiveBus() {
        viewModel.upAdapterLiveData.observe(this) {
            adapter.notifyItemRangeChanged(0, adapter.itemCount, bundleOf(it to null))
        }
        viewModel.searchFinishLiveData.observe(this) { isEmpty ->
            if (!isEmpty || viewModel.searchScope.isAll()) return@observe
            alert("搜索结果为空") {
                val precisionSearch = appCtx.getPrefBoolean(PreferKey.precisionSearch)
                val displayScope = viewModel.searchScope.display
                if (precisionSearch) {
                    setMessage("${displayScope}分组搜索结果为空，是否关闭精准搜索？")
                    yesButton {
                        appCtx.putPrefBoolean(PreferKey.precisionSearch, false)
                        precisionSearchMenuItem?.isChecked = false
                        viewModel.searchKey = ""
                        viewModel.search(searchView.query.toString())
                    }
                } else {
                    setMessage("${displayScope}分组搜索结果为空，是否切换到全部分组？")
                    yesButton {
                        viewModel.searchScope.update("")
                    }
                }
                noButton()
            }
        }
    }

    /**
     * 显示书籍详情
     */
    override fun showBookInfo(name: String, author: String, bookUrl: String) {
        startActivity<BookInfoActivity> {
            putExtra("name", name)
            putExtra("author", author)
            putExtra("bookUrl", bookUrl)
        }
    }

    /**
     * 是否已经加入书架
     */
    override fun isInBookshelf(name: String, author: String): Boolean {
        return viewModel.isInBookShelf(name, author)
    }

    /**
     * 显示书籍详情
     */
    override fun showBookInfo(book: Book) {
        showBookInfo(book.name, book.author, book.bookUrl)
    }

    /**
     * 点击历史关键字
     */
    override fun searchHistory(key: String) {
        lifecycleScope.launch {
            when {
                searchView.query.toString() == key -> {
                    searchView.setQuery(key, true)
                }

                withContext(IO) { appDb.bookDao.findByName(key).isEmpty() } -> {
                    searchView.setQuery(key, true)
                }

                else -> {
                    searchView.setQuery(key, false)
                }
            }
        }
    }

    /**
     * 删除搜索记录
     */
    override fun deleteHistory(searchKeyword: SearchKeyword) {
        viewModel.deleteHistory(searchKeyword)
    }


    override fun onSearchScopeOk(searchScope: SearchScope) {
        viewModel.searchScope.update(searchScope.toString())
    }

    private fun alertSearchScope() {
        showDialogFragment<SearchScopeDialog>()
    }

    private fun alertClearHistory() {
        alert(R.string.draw) {
            setMessage(R.string.sure_clear_search_history)
            yesButton {
                viewModel.clearHistory()
            }
            noButton()
        }
    }

    override fun finish() {
        if (searchView.hasFocus()) {
            searchView.clearFocus()
        }
        super.finish()
    }

    companion object {

        fun start(context: Context, key: String?) {
            context.startActivity<SearchActivity> {
                putExtra("key", key)
            }
        }

    }
}
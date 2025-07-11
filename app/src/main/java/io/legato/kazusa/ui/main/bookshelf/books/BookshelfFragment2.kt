package io.legato.kazusa.ui.main.bookshelf.books

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import androidx.appcompat.widget.SearchView
import androidx.core.view.isGone
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.legato.kazusa.R
import io.legato.kazusa.constant.AppLog
import io.legato.kazusa.constant.EventBus
import io.legato.kazusa.data.AppDatabase
import io.legato.kazusa.data.appDb
import io.legato.kazusa.data.entities.Book
import io.legato.kazusa.data.entities.BookGroup
import io.legato.kazusa.databinding.FragmentBookshelf2Binding
import io.legato.kazusa.help.config.AppConfig
import io.legato.kazusa.ui.book.group.GroupEditDialog
import io.legato.kazusa.ui.book.info.BookInfoActivity
import io.legato.kazusa.ui.book.search.SearchActivity
import io.legato.kazusa.ui.main.bookshelf.BaseBookshelfFragment
import io.legato.kazusa.ui.main.bookshelf.books.styleFold.BaseBooksAdapter
import io.legato.kazusa.ui.main.bookshelf.books.styleFold.BooksAdapterGrid
import io.legato.kazusa.ui.main.bookshelf.books.styleFold.BooksAdapterList
import io.legato.kazusa.utils.bookshelfLayout
import io.legato.kazusa.utils.cnCompare
import io.legato.kazusa.utils.flowWithLifecycleAndDatabaseChangeFirst
import io.legato.kazusa.utils.observeEvent
import io.legato.kazusa.utils.showDialogFragment
import io.legato.kazusa.utils.startActivity
import io.legato.kazusa.utils.startActivityForBook
import io.legato.kazusa.utils.viewbindingdelegate.viewBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlin.math.max

/**
 * 书架界面
 */
class BookshelfFragment2() : BaseBookshelfFragment(R.layout.fragment_bookshelf2),
    SearchView.OnQueryTextListener,
    BaseBooksAdapter.CallBack {

    constructor(position: Int) : this() {
        val bundle = Bundle()
        bundle.putInt("position", position)
        arguments = bundle
    }

    private val binding by viewBinding(FragmentBookshelf2Binding::bind)

    private val bookshelfLayout by lazy { requireContext().bookshelfLayout }

    private val booksAdapter: BaseBooksAdapter<*> by lazy {
        if (bookshelfLayout == 0) {
            BooksAdapterList(requireContext(), this)
        } else {
            BooksAdapterGrid(requireContext(), this)
        }
    }

    private var bookGroups: List<BookGroup> = emptyList()
    private var booksFlowJob: Job? = null
    override var groupId = BookGroup.Companion.IdRoot
    private var allBooks: List<Book> = emptyList()
    override var books: List<Book> = emptyList()
    private var enableRefresh = true

    override fun onFragmentCreated(view: View, savedInstanceState: Bundle?) {
        setSupportToolbar(binding.titleBar.toolbar)
        initRecyclerView()
        initBookGroupData()
        initAllBooksData()
        initBooksData()
    }

    private fun initRecyclerView() {
        binding.refreshLayout.setOnRefreshListener {
            binding.refreshLayout.isRefreshing = false
            activityViewModel.upToc(books)
        }
        if (bookshelfLayout == 0) {
            binding.rvBookshelf.layoutManager = LinearLayoutManager(context)
        } else {
            binding.rvBookshelf.layoutManager = GridLayoutManager(context, bookshelfLayout)
        }
        binding.rvBookshelf.itemAnimator = null
        binding.rvBookshelf.adapter = booksAdapter
        booksAdapter.registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
            override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                val layoutManager = binding.rvBookshelf.layoutManager
                if (positionStart == 0 && layoutManager is LinearLayoutManager) {
                    val scrollTo = layoutManager.findFirstVisibleItemPosition() - itemCount
                    binding.rvBookshelf.scrollToPosition(max(0, scrollTo))
                }
            }

            override fun onItemRangeMoved(fromPosition: Int, toPosition: Int, itemCount: Int) {
                val layoutManager = binding.rvBookshelf.layoutManager
                if (toPosition == 0 && layoutManager is LinearLayoutManager) {
                    val scrollTo = layoutManager.findFirstVisibleItemPosition() - itemCount
                    binding.rvBookshelf.scrollToPosition(max(0, scrollTo))
                }
            }
        })
        binding.rvBookshelf.itemAnimator = DefaultItemAnimator()
    }

    override fun upGroup(data: List<BookGroup>) {
        if (data != bookGroups) {
            bookGroups = data
            booksAdapter.updateItems()
            binding.tvEmptyMsg.isGone = getItemCount() > 0
            binding.refreshLayout.isEnabled = enableRefresh && getItemCount() > 0
        }
    }

    override fun upSort() {
        initAllBooksData()
        initBooksData()
    }

    private var allBooksFlowJob: Job? = null

    private fun initAllBooksData() {
        allBooksFlowJob?.cancel()
        allBooksFlowJob = viewLifecycleOwner.lifecycleScope.launch {
            appDb.bookDao.flowAll().map { list ->
                when (AppConfig.getBookSortByGroupId(BookGroup.Companion.IdRoot)) {
                    1 -> list.sortedByDescending { it.latestChapterTime }
                    2 -> list.sortedWith { o1, o2 -> o1.name.cnCompare(o2.name) }
                    3 -> list.sortedBy { it.order }
                    4 -> list.sortedByDescending { max(it.latestChapterTime, it.durChapterTime) }
                    else -> list.sortedByDescending { it.durChapterTime }
                }
            }.flowWithLifecycleAndDatabaseChangeFirst(
                viewLifecycleOwner.lifecycle,
                Lifecycle.State.RESUMED,
                AppDatabase.Companion.BOOK_TABLE_NAME
            ).catch {
                AppLog.put("所有书籍更新出错", it)
            }.conflate().flowOn(Dispatchers.Default).collect { list ->
                allBooks = list
                booksAdapter.setAllBooks(allBooks)
                booksAdapter.updateItems()
            }
        }
    }

    private fun initBooksData() {
        if (groupId == BookGroup.Companion.IdRoot) {
            if (isAdded) {
                binding.titleBar.title = getString(R.string.bookshelf)
                binding.refreshLayout.isEnabled = true
                enableRefresh = true
            }
        } else {
            bookGroups.firstOrNull {
                groupId == it.groupId
            }?.let {
                binding.titleBar.title = it.groupName
                binding.refreshLayout.isEnabled = it.enableRefresh
                enableRefresh = it.enableRefresh
            }
        }
        booksFlowJob?.cancel()
        booksFlowJob = viewLifecycleOwner.lifecycleScope.launch {
            appDb.bookDao.flowByGroup(groupId).map { list ->
                //排序
                val isDescending = AppConfig.bookshelfSortOrder == 1

                when (AppConfig.getBookSortByGroupId(groupId)) {
                    1 -> if (isDescending) list.sortedByDescending { it.latestChapterTime }
                    else list.sortedBy { it.latestChapterTime }

                    2 -> if (isDescending)
                        list.sortedWith { o1, o2 -> o2.name.cnCompare(o1.name) }
                    else
                        list.sortedWith { o1, o2 -> o1.name.cnCompare(o2.name) }

                    3 -> if (isDescending) list.sortedByDescending { it.order }
                    else list.sortedBy { it.order }

                    4 -> if (isDescending) list.sortedByDescending {
                        max(
                            it.latestChapterTime,
                            it.durChapterTime
                        )
                    }
                    else list.sortedBy { max(it.latestChapterTime, it.durChapterTime) }

                    5 -> if (isDescending)
                        list.sortedWith { o1, o2 -> o2.author.cnCompare(o1.author) }
                    else
                        list.sortedWith { o1, o2 -> o1.author.cnCompare(o2.author) }

                    else -> if (isDescending) list.sortedByDescending { it.durChapterTime }
                    else list.sortedBy { it.durChapterTime }
                }

            }.flowWithLifecycleAndDatabaseChangeFirst(
                viewLifecycleOwner.lifecycle,
                Lifecycle.State.RESUMED,
                AppDatabase.Companion.BOOK_TABLE_NAME
            ).catch {
                AppLog.put("书架更新出错", it)
            }.conflate().flowOn(Dispatchers.Default).collect { list ->
                books = list
                booksAdapter.updateItems()
                binding.tvEmptyMsg.isGone = getItemCount() > 0
                binding.refreshLayout.isEnabled = enableRefresh && getItemCount() > 0
            }
        }
    }

    fun back(): Boolean {
        if (groupId != BookGroup.Companion.IdRoot) {
            groupId = BookGroup.Companion.IdRoot
            initBooksData()
            return true
        }
        return false
    }

    override fun onQueryTextSubmit(query: String?): Boolean {
        SearchActivity.Companion.start(requireContext(), query)
        return false
    }

    override fun onQueryTextChange(newText: String?): Boolean {
        return false
    }

    override fun gotoTop() {
        if (AppConfig.isEInkMode) {
            binding.rvBookshelf.scrollToPosition(0)
        } else {
            binding.rvBookshelf.smoothScrollToPosition(0)
        }
    }

    override fun onItemClick(item: Any) {
        when (item) {
            is Book -> startActivityForBook(item)

            is BookGroup -> {
                groupId = item.groupId
                initBooksData()
            }
        }
    }

    override fun onItemLongClick(item: Any) {
        when (item) {
            is Book -> startActivity<BookInfoActivity> {
                putExtra("name", item.name)
                putExtra("author", item.author)
            }

            is BookGroup -> showDialogFragment(GroupEditDialog(item))
        }
    }

    override fun isUpdate(bookUrl: String): Boolean {
        return activityViewModel.isUpdate(bookUrl)
    }

    fun getItemCount(): Int {
        return if (groupId == BookGroup.Companion.IdRoot) {
            bookGroups.size + books.size
        } else {
            books.size
        }
    }

    override fun getItems(): List<Any> {
        return if (groupId == BookGroup.Companion.IdRoot) {
            bookGroups + books
        } else {
            books
        }
    }


    @SuppressLint("NotifyDataSetChanged")
    override fun observeLiveBus() {
        super.observeLiveBus()
        observeEvent<String>(EventBus.UP_BOOKSHELF) {
            booksAdapter.notification(it)
        }
        observeEvent<String>(EventBus.BOOKSHELF_REFRESH) {
            booksAdapter.notifyDataSetChanged()
        }
    }
}
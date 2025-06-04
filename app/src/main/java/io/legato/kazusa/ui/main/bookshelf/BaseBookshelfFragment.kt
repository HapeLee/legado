package io.legato.kazusa.ui.main.bookshelf

import android.annotation.SuppressLint
import android.view.Menu
import android.view.MenuItem
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.LiveData
import io.legato.kazusa.R
import io.legato.kazusa.base.VMBaseFragment
import io.legato.kazusa.constant.EventBus
import io.legato.kazusa.data.appDb
import io.legato.kazusa.data.entities.Book
import io.legato.kazusa.data.entities.BookGroup
import io.legato.kazusa.databinding.DialogBookshelfConfigBinding
import io.legato.kazusa.databinding.DialogEditTextBinding
import io.legato.kazusa.help.DirectLinkUpload
import io.legato.kazusa.help.config.AppConfig
import io.legato.kazusa.lib.dialogs.alert
import io.legato.kazusa.ui.about.AppLogDialog
import io.legato.kazusa.ui.book.cache.CacheActivity
import io.legato.kazusa.ui.book.group.GroupManageDialog
import io.legato.kazusa.ui.book.import.local.ImportBookActivity
import io.legato.kazusa.ui.book.import.remote.RemoteBookActivity
import io.legato.kazusa.ui.book.manage.BookshelfManageActivity
import io.legato.kazusa.ui.book.search.SearchActivity
import io.legato.kazusa.ui.file.HandleFileContract
import io.legato.kazusa.ui.main.MainFragmentInterface
import io.legato.kazusa.ui.main.MainViewModel
import io.legato.kazusa.ui.widget.dialog.WaitDialog
import io.legato.kazusa.utils.checkByIndex
import io.legato.kazusa.utils.getCheckedIndex
import io.legato.kazusa.utils.isAbsUrl
import io.legato.kazusa.utils.postEvent
import io.legato.kazusa.utils.readText
import io.legato.kazusa.utils.sendToClip
import io.legato.kazusa.utils.showDialogFragment
import io.legato.kazusa.utils.startActivity
import io.legato.kazusa.utils.toastOnUi

abstract class BaseBookshelfFragment(layoutId: Int) : VMBaseFragment<BookshelfViewModel>(layoutId),
    MainFragmentInterface {

    override val position: Int? get() = arguments?.getInt("position")

    val activityViewModel by activityViewModels<MainViewModel>()
    override val viewModel by viewModels<BookshelfViewModel>()

    private val importBookshelf = registerForActivityResult(HandleFileContract()) {
        kotlin.runCatching {
            it.uri?.readText(requireContext())?.let { text ->
                viewModel.importBookshelf(text, groupId)
            }
        }.onFailure {
            toastOnUi(it.localizedMessage ?: "ERROR")
        }
    }
    private val exportResult = registerForActivityResult(HandleFileContract()) {
        it.uri?.let { uri ->
            alert(R.string.export_success) {
                if (uri.toString().isAbsUrl()) {
                    setMessage(DirectLinkUpload.getSummary())
                }
                val alertBinding = DialogEditTextBinding.inflate(layoutInflater).apply {
                    editView.hint = getString(R.string.path)
                    editView.setText(uri.toString())
                }
                customView { alertBinding.root }
                okButton {
                    requireContext().sendToClip(uri.toString())
                }
            }
        }
    }
    abstract val groupId: Long
    abstract val books: List<Book>
    private var groupsLiveData: LiveData<List<BookGroup>>? = null
    private val waitDialog by lazy {
        WaitDialog(requireContext()).apply {
            setOnCancelListener {
                viewModel.addBookJob?.cancel()
            }
        }
    }

    abstract fun gotoTop()

    override fun onCompatCreateOptionsMenu(menu: Menu) {
        menuInflater.inflate(R.menu.main_bookshelf, menu)
    }

    override fun onCompatOptionsItemSelected(item: MenuItem) {
        super.onCompatOptionsItemSelected(item)
        when (item.itemId) {
            R.id.menu_remote -> startActivity<RemoteBookActivity>()
            R.id.menu_search -> startActivity<SearchActivity>()
            R.id.menu_update_toc -> activityViewModel.upToc(books)
            R.id.menu_bookshelf_layout -> configBookshelf()
            R.id.menu_group_manage -> showDialogFragment<GroupManageDialog>()
            R.id.menu_add_local -> startActivity<ImportBookActivity>()
            R.id.menu_add_url -> showAddBookByUrlAlert()
            R.id.menu_bookshelf_manage -> startActivity<BookshelfManageActivity> {
                putExtra("groupId", groupId)
            }

            R.id.menu_download -> startActivity<CacheActivity> {
                putExtra("groupId", groupId)
            }

            R.id.menu_export_bookshelf -> viewModel.exportBookshelf(books) { file ->
                exportResult.launch {
                    mode = HandleFileContract.EXPORT
                    fileData =
                        HandleFileContract.FileData("bookshelf.json", file, "application/json")
                }
            }

            R.id.menu_import_bookshelf -> importBookshelfAlert(groupId)
            R.id.menu_log -> showDialogFragment<AppLogDialog>()
        }
    }

    protected fun initBookGroupData() {
        groupsLiveData?.removeObservers(viewLifecycleOwner)
        groupsLiveData = appDb.bookGroupDao.show.apply {
            observe(viewLifecycleOwner) {
                upGroup(it)
            }
        }
    }

    abstract fun upGroup(data: List<BookGroup>)

    abstract fun upSort()

    override fun observeLiveBus() {
        viewModel.addBookProgressLiveData.observe(this) { count ->
            if (count < 0) {
                waitDialog.dismiss()
            } else {
                waitDialog.setText("添加中... ($count)")
            }
        }
    }

    @SuppressLint("InflateParams")
    fun showAddBookByUrlAlert() {
        alert(titleResource = R.string.add_book_url) {
            val alertBinding = DialogEditTextBinding.inflate(layoutInflater).apply {
                editView.hint = "url"
            }
            customView { alertBinding.root }
            okButton {
                alertBinding.editView.text?.toString()?.let {
                    waitDialog.setText("添加中...")
                    waitDialog.show()
                    viewModel.addBookByUrl(it)
                }
            }
            cancelButton()
        }
    }

    @SuppressLint("InflateParams")
    fun configBookshelf() {
        alert(titleResource = R.string.bookshelf_layout) {
            val bookshelfLayout = AppConfig.bookshelfLayout
            val bookshelfSort = AppConfig.bookshelfSort
            val alertBinding =
                DialogBookshelfConfigBinding.inflate(layoutInflater)
                    .apply {
                        spGroupStyle.setSelection(AppConfig.bookGroupStyle)
                        swShowUnread.isChecked = AppConfig.showUnread
                        swShowLastUpdateTime.isChecked = AppConfig.showLastUpdateTime
                        swShowWaitUpBooks.isChecked = AppConfig.showWaitUpCount
                        swShowBookshelfFastScroller.isChecked = AppConfig.showBookshelfFastScroller
                        rgLayout.checkByIndex(bookshelfLayout)
                        rgSort.checkByIndex(bookshelfSort)
                    }
            customView { alertBinding.root }
            okButton {
                alertBinding.apply {
                    var notifyMain = false
                    var recreate = false
                    if (AppConfig.bookGroupStyle != spGroupStyle.selectedItemPosition) {
                        AppConfig.bookGroupStyle = spGroupStyle.selectedItemPosition
                        notifyMain = true
                    }
                    if (AppConfig.showUnread != swShowUnread.isChecked) {
                        AppConfig.showUnread = swShowUnread.isChecked
                        postEvent(EventBus.BOOKSHELF_REFRESH, "")
                    }
                    if (AppConfig.showLastUpdateTime != swShowLastUpdateTime.isChecked) {
                        AppConfig.showLastUpdateTime = swShowLastUpdateTime.isChecked
                        postEvent(EventBus.BOOKSHELF_REFRESH, "")
                    }
                    if (AppConfig.showWaitUpCount != swShowWaitUpBooks.isChecked) {
                        AppConfig.showWaitUpCount = swShowWaitUpBooks.isChecked
                        activityViewModel.postUpBooksLiveData(true)
                    }
                    if (AppConfig.showBookshelfFastScroller != swShowBookshelfFastScroller.isChecked) {
                        AppConfig.showBookshelfFastScroller = swShowBookshelfFastScroller.isChecked
                        postEvent(EventBus.BOOKSHELF_REFRESH, "")
                    }
                    if (bookshelfSort != rgSort.getCheckedIndex()) {
                        AppConfig.bookshelfSort = rgSort.getCheckedIndex()
                        upSort()
                    }
                    if (bookshelfLayout != rgLayout.getCheckedIndex()) {
                        AppConfig.bookshelfLayout = rgLayout.getCheckedIndex()
                        if (AppConfig.bookshelfLayout == 0) {
                            activityViewModel.booksGridRecycledViewPool.clear()
                        } else {
                            activityViewModel.booksListRecycledViewPool.clear()
                        }
                        recreate = true
                    }
                    if (recreate) {
                        postEvent(EventBus.RECREATE, "")
                    } else if (notifyMain) {
                        postEvent(EventBus.NOTIFY_MAIN, false)
                    }
                }
            }
            cancelButton()
        }
    }


    private fun importBookshelfAlert(groupId: Long) {
        alert(titleResource = R.string.import_bookshelf) {
            val alertBinding = DialogEditTextBinding.inflate(layoutInflater).apply {
                editView.hint = "url/json"
            }
            customView { alertBinding.root }
            okButton {
                alertBinding.editView.text?.toString()?.let {
                    viewModel.importBookshelf(it, groupId)
                }
            }
            cancelButton()
            neutralButton(R.string.select_file) {
                importBookshelf.launch {
                    mode = HandleFileContract.FILE
                    allowExtensions = arrayOf("txt", "json")
                }
            }
        }
    }

}
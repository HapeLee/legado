package io.legato.kazusa.ui.book.source.debug

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.widget.SearchView
import androidx.lifecycle.lifecycleScope
import io.legato.kazusa.R
import io.legato.kazusa.base.VMBaseActivity
import io.legato.kazusa.databinding.ActivitySourceDebugBinding
import io.legato.kazusa.help.source.clearExploreKindsCache
import io.legato.kazusa.help.source.exploreKinds
import io.legato.kazusa.lib.dialogs.selector
//import io.legado.app.lib.theme.accentColor
//import io.legado.app.lib.theme.primaryColor
import io.legato.kazusa.ui.qrcode.QrCodeResult
import io.legato.kazusa.ui.widget.dialog.TextDialog
import io.legato.kazusa.utils.applyNavigationBarPadding
import io.legato.kazusa.utils.launch
import io.legato.kazusa.utils.showDialogFragment
import io.legato.kazusa.utils.showHelp
import io.legato.kazusa.utils.toastOnUi
import io.legato.kazusa.utils.viewbindingdelegate.viewBinding
import kotlinx.coroutines.launch
import splitties.views.onClick
import splitties.views.onLongClick

class BookSourceDebugActivity : VMBaseActivity<ActivitySourceDebugBinding, BookSourceDebugModel>() {

    override val binding by viewBinding(ActivitySourceDebugBinding::inflate)
    override val viewModel by viewModels<BookSourceDebugModel>()

    private val adapter by lazy { BookSourceDebugAdapter(this) }
    private val searchView: SearchView by lazy {
        binding.titleBar.findViewById(R.id.search_view)
    }
    private val qrCodeResult = registerForActivityResult(QrCodeResult()) {
        it?.let {
            startSearch(it)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initRecyclerView()
        initSearchView()
        viewModel.init(intent.getStringExtra("key")) {
            initHelpView()
        }
        viewModel.observe { state, msg ->
            lifecycleScope.launch {
                adapter.addItem(msg)
                if (state == -1 || state == 1000) {
                    binding.rotateLoading.gone()
                }
            }
        }
    }

    private fun initRecyclerView() {
        //binding.recyclerView.setEdgeEffectColor(primaryColor)
        binding.recyclerView.adapter = adapter
        binding.recyclerView.applyNavigationBarPadding()
        //binding.rotateLoading.loadingColor = accentColor
    }

    private fun initSearchView() {
        searchView.onActionViewExpanded()
        searchView.isSubmitButtonEnabled = true
        searchView.queryHint = getString(R.string.search_book_key)
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                searchView.clearFocus()
                openOrCloseHelp(false)
                startSearch(query ?: "我的")
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                return false
            }
        })
        searchView.setOnQueryTextFocusChangeListener { _, hasFocus ->
            openOrCloseHelp(hasFocus)
        }
        openOrCloseHelp(true)
    }

    @SuppressLint("SetTextI18n")
    private fun initHelpView() {
        viewModel.bookSource?.ruleSearch?.checkKeyWord?.let {
            if (it.isNotBlank()) {
                binding.textMy.text = it
            }
        }
        binding.textMy.onClick {
            searchView.setQuery(binding.textMy.text, true)
        }
        binding.textXt.onClick {
            searchView.setQuery(binding.textXt.text, true)
        }
        binding.textFx.onClick {
            if (!binding.textFx.text.startsWith("ERROR:")) {
                searchView.setQuery(binding.textFx.text, true)
            }
        }
        binding.textInfo.onClick {
            if (!searchView.query.isNullOrBlank()) {
                searchView.setQuery(searchView.query, true)
            }
        }
        binding.textToc.onClick {
            prefixAutoComplete("++")
        }
        binding.textContent.onClick {
            prefixAutoComplete("--")
        }
        initExploreKinds()
    }

    private fun initExploreKinds() {
        lifecycleScope.launch {
            val exploreKinds = viewModel.bookSource?.exploreKinds()?.filter {
                !it.url.isNullOrBlank()
            }
            exploreKinds?.firstOrNull()?.let {
                binding.textFx.text = "${it.title}::${it.url}"
                if (it.title.startsWith("ERROR:")) {
                    adapter.addItem("获取发现出错\n${it.url}")
                    openOrCloseHelp(false)
                    searchView.clearFocus()
                    return@launch
                }
            }
            @Suppress("USELESS_ELVIS")
            exploreKinds?.map { it.title ?: "" }?.let { exploreKindTitles ->
                binding.textFx.onLongClick {
                    selector("选择发现", exploreKindTitles) { _, index ->
                        val explore = exploreKinds[index]
                        binding.textFx.text = "${explore.title}::${explore.url}"
                        searchView.setQuery(binding.textFx.text, true)
                    }
                }
            }
        }
    }

    private fun prefixAutoComplete(prefix: String) {
        val query = searchView.query
        if (query.isNullOrBlank() || query.length <= 2) {
            searchView.setQuery(prefix, false)
        } else {
            if (!query.startsWith(prefix)) {
                searchView.setQuery("$prefix$query", true)
            } else {
                searchView.setQuery(query, true)
            }
        }
    }

    /**
     * 打开关闭历史界面
     */
    private fun openOrCloseHelp(open: Boolean) {
        if (open) {
            binding.help.visibility = View.VISIBLE
        } else {
            binding.help.visibility = View.GONE
        }
    }

    private fun startSearch(key: String) {
        adapter.clearItems()
        viewModel.startDebug(key, {
            binding.rotateLoading.visible()
        }, {
            toastOnUi("未获取到书源")
        })
    }

    override fun onCompatCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.book_source_debug, menu)
        return super.onCompatCreateOptionsMenu(menu)
    }

    override fun onCompatOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_scan -> qrCodeResult.launch()
            R.id.menu_search_src -> showDialogFragment(TextDialog("html", viewModel.searchSrc))
            R.id.menu_book_src -> showDialogFragment(TextDialog("html", viewModel.bookSrc))
            R.id.menu_toc_src -> showDialogFragment(TextDialog("html", viewModel.tocSrc))
            R.id.menu_content_src -> showDialogFragment(TextDialog("html", viewModel.contentSrc))
            R.id.menu_refresh_explore -> lifecycleScope.launch {
                viewModel.bookSource?.clearExploreKindsCache()
                adapter.clearItems()
                openOrCloseHelp(true)
                initExploreKinds()
            }

            R.id.menu_help -> showHelp("debugHelp")
        }
        return super.onCompatOptionsItemSelected(item)
    }

}
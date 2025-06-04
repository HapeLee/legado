package io.legato.kazusa.ui.main.rss

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.SubMenu
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import com.google.android.material.search.SearchBar
import com.google.android.material.search.SearchView
import io.legato.kazusa.R
import io.legato.kazusa.base.VMBaseFragment
import io.legato.kazusa.constant.AppLog
import io.legato.kazusa.data.AppDatabase
import io.legato.kazusa.data.appDb
import io.legato.kazusa.data.entities.RssSource
import io.legato.kazusa.databinding.FragmentRssBinding
import io.legato.kazusa.databinding.ItemRssBinding
import io.legato.kazusa.lib.dialogs.alert
//import io.legado.app.lib.theme.primaryColor
//import io.legado.app.lib.theme.primaryTextColor
import io.legato.kazusa.ui.main.MainFragmentInterface
import io.legato.kazusa.ui.rss.article.RssSortActivity
import io.legato.kazusa.ui.rss.favorites.RssFavoritesActivity
import io.legato.kazusa.ui.rss.read.ReadRssActivity
import io.legato.kazusa.ui.rss.source.edit.RssSourceEditActivity
import io.legato.kazusa.ui.rss.source.manage.RssSourceActivity
import io.legato.kazusa.ui.rss.subscription.RuleSubActivity
import io.legato.kazusa.utils.flowWithLifecycleAndDatabaseChange
import io.legato.kazusa.utils.openUrl
import io.legato.kazusa.utils.startActivity
import io.legato.kazusa.utils.transaction
import io.legato.kazusa.utils.viewbindingdelegate.viewBinding
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch


/**
 * 订阅界面
 */
class RssFragment() : VMBaseFragment<RssViewModel>(R.layout.fragment_rss),
    MainFragmentInterface,
    RssAdapter.CallBack {

    constructor(position: Int) : this() {
        val bundle = Bundle()
        bundle.putInt("position", position)
        arguments = bundle
    }

    override val position: Int? get() = arguments?.getInt("position")

    private val binding by viewBinding(FragmentRssBinding::bind)
    override val viewModel by viewModels<RssViewModel>()
    private val adapter by lazy {
        RssAdapter(requireContext(), this, this, viewLifecycleOwner.lifecycle)
    }
    private val searchBar: SearchBar by lazy { binding.searchBar }
    private val searchView: SearchView by lazy { binding.searchView }
    private var groupsFlowJob: Job? = null
    private var rssFlowJob: Job? = null
    private val groups = linkedSetOf<String>()
    private var groupsMenu: SubMenu? = null

    override fun onFragmentCreated(view: View, savedInstanceState: Bundle?) {
        setSupportToolbar(binding.topBar)
        initSearchView()
        initRecyclerView()
        initGroupData()
        upRssFlowJob()
    }

    override fun onCompatCreateOptionsMenu(menu: Menu) {
        menuInflater.inflate(R.menu.main_rss, menu)
        groupsMenu = menu.findItem(R.id.menu_group)?.subMenu
        upGroupsMenu()
    }

    override fun onCompatOptionsItemSelected(item: MenuItem) {
        super.onCompatOptionsItemSelected(item)
        when (item.itemId) {
            R.id.menu_rss_config -> startActivity<RssSourceActivity>()
            R.id.menu_rss_star -> startActivity<RssFavoritesActivity>()
            else -> if (item.groupId == R.id.menu_group_text) {
                searchView.setText("group:${item.title}")
                upRssFlowJob("group:${item.title}")
            }
        }
    }

    override fun onPause() {
        super.onPause()
        searchView.clearFocus()
    }

    private fun upGroupsMenu() = groupsMenu?.transaction { subMenu ->
        subMenu.removeGroup(R.id.menu_group_text)
        groups.forEach {
            subMenu.add(R.id.menu_group_text, Menu.NONE, Menu.NONE, it)
        }
    }

    private fun initSearchView() {
        searchView.hint = getString(R.string.search_rss_source)

        searchBar.setOnClickListener {
            searchView.show()
            if (searchView.text.isEmpty()) {
                searchView.hint = getString(R.string.search_rss_source)
            }
        }

        searchView.editText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                val query = searchView.text.toString()
                upRssFlowJob(query)
                searchBar.hint = if (query.isEmpty()) {
                    getString(R.string.search_rss_source)
                } else {
                    query
                }
                searchView.hide()
                return@setOnEditorActionListener true
            }
            false
        }

        searchView.editText.doAfterTextChanged { editable ->
            editable?.let {
                upRssFlowJob(it.toString())

                if (it.isEmpty()) {
                    searchBar.hint = getString(R.string.search_rss_source)
                }
            }
        }

        searchView.setupWithSearchBar(searchBar)
    }

    private fun initRecyclerView() {
        //binding.recyclerView.setEdgeEffectColor(primaryColor)
        binding.recyclerView.adapter = adapter
        adapter.addHeaderView {
            ItemRssBinding.inflate(layoutInflater, it, false).apply {
                tvName.setText(R.string.rule_subscription)
                ivIcon.setImageResource(R.drawable.image_legado)
                root.setOnClickListener {
                    startActivity<RuleSubActivity>()
                }
            }
        }
    }

    private fun initGroupData() {
        groupsFlowJob?.cancel()
        groupsFlowJob = viewLifecycleOwner.lifecycleScope.launch {
            appDb.rssSourceDao.flowEnabledGroups().catch {
                AppLog.put("订阅界面获取分组数据失败\n${it.localizedMessage}", it)
            }.flowWithLifecycleAndDatabaseChange(
                viewLifecycleOwner.lifecycle,
                Lifecycle.State.RESUMED,
                AppDatabase.RSS_SOURCE_TABLE_NAME
            ).conflate().collect {
                groups.clear()
                groups.addAll(it)
                upGroupsMenu()
            }
        }
    }

    private fun upRssFlowJob(searchKey: String? = null) {
        rssFlowJob?.cancel()
        rssFlowJob = viewLifecycleOwner.lifecycleScope.launch {
            when {
                searchKey.isNullOrEmpty() -> appDb.rssSourceDao.flowEnabled()
                searchKey.startsWith("group:") -> {
                    val key = searchKey.substringAfter("group:")
                    appDb.rssSourceDao.flowEnabledByGroup(key)
                }

                else -> appDb.rssSourceDao.flowEnabled(searchKey)
            }.flowWithLifecycleAndDatabaseChange(
                viewLifecycleOwner.lifecycle,
                Lifecycle.State.RESUMED,
                AppDatabase.RSS_SOURCE_TABLE_NAME
            ).catch {
                AppLog.put("订阅界面更新数据出错", it)
            }.flowOn(IO).collect {
                adapter.setItems(it)
            }
        }
    }

    override fun openRss(rssSource: RssSource) {
        if (rssSource.singleUrl) {
            viewModel.getSingleUrl(rssSource) { url ->
                if (url.startsWith("http", true)) {
                    startActivity<ReadRssActivity> {
                        putExtra("title", rssSource.sourceName)
                        putExtra("origin", url)
                    }
                } else {
                    context?.openUrl(url)
                }
            }
        } else {
            startActivity<RssSortActivity> {
                putExtra("url", rssSource.sourceUrl)
            }
        }
    }

    override fun toTop(rssSource: RssSource) {
        viewModel.topSource(rssSource)
    }

    override fun edit(rssSource: RssSource) {
        startActivity<RssSourceEditActivity> {
            putExtra("sourceUrl", rssSource.sourceUrl)
        }
    }

    override fun del(rssSource: RssSource) {
        alert(R.string.draw) {
            setMessage(getString(R.string.sure_del) + "\n" + rssSource.sourceName)
            noButton()
            yesButton {
                viewModel.del(rssSource)
            }
        }
    }

    override fun disable(rssSource: RssSource) {
        viewModel.disable(rssSource)
    }
}
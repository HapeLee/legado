@file:Suppress("DEPRECATION")

package io.legato.kazusa.ui.main.bookshelf.style1

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import io.legato.kazusa.R
import io.legato.kazusa.data.appDb
import io.legato.kazusa.data.entities.Book
import io.legato.kazusa.data.entities.BookGroup
import io.legato.kazusa.databinding.FragmentBookshelf1Binding
import io.legato.kazusa.help.config.AppConfig
//import io.legado.app.lib.theme.accentColor
//import io.legado.app.lib.theme.primaryColor
import io.legato.kazusa.ui.book.group.GroupEditDialog
import io.legato.kazusa.ui.book.search.SearchActivity
import io.legato.kazusa.ui.main.bookshelf.BaseBookshelfFragment
import io.legato.kazusa.ui.main.bookshelf.style1.books.BooksFragment
import io.legato.kazusa.utils.isCreated
import io.legato.kazusa.utils.showDialogFragment
import io.legato.kazusa.utils.toastOnUi
import io.legato.kazusa.utils.viewbindingdelegate.viewBinding
import kotlin.collections.set

/**
 * 书架界面
 */
class BookshelfFragment1() : BaseBookshelfFragment(R.layout.fragment_bookshelf1),
    TabLayout.OnTabSelectedListener,
    SearchView.OnQueryTextListener {

    constructor(position: Int) : this() {
        val bundle = Bundle()
        bundle.putInt("position", position)
        arguments = bundle
    }

    private val binding by viewBinding(FragmentBookshelf1Binding::bind)
    private val tabLayout: TabLayout by lazy {
        binding.tabLayout.findViewById(R.id.tab_layout)
    }
    private val bookGroups = mutableListOf<BookGroup>()
    private val fragmentMap = hashMapOf<Long, BooksFragment>()
    override val groupId: Long get() = selectedGroup?.groupId ?: 0

    private lateinit var adapter: TabFragmentPageAdapter

    override val books: List<Book>
        get() {
            val fragment = fragmentMap[groupId]
            return fragment?.getBooks() ?: emptyList()
        }

    override fun onFragmentCreated(view: View, savedInstanceState: Bundle?) {
        setSupportToolbar(binding.topBar)
        initView()
        initBookGroupData()
    }

    private val selectedGroup: BookGroup?
        get() = bookGroups.getOrNull(binding.viewPagerBookshelf.currentItem)

    private fun initView() {
        adapter = TabFragmentPageAdapter(this)
        binding.viewPagerBookshelf.adapter = adapter
        binding.viewPagerBookshelf.offscreenPageLimit = 1

        TabLayoutMediator(binding.tabLayout, binding.viewPagerBookshelf) { tab, position ->
            tab.text = bookGroups[position].groupName
        }.attach()
    }

    override fun onQueryTextSubmit(query: String?): Boolean {
        SearchActivity.start(requireContext(), query)
        return false
    }

    override fun onQueryTextChange(newText: String?): Boolean {
        return false
    }

    @SuppressLint("NotifyDataSetChanged")
    @Synchronized
    override fun upGroup(data: List<BookGroup>) {
        if (data.isEmpty()) {
            appDb.bookGroupDao.enableGroup(BookGroup.IdAll)
        } else {
            if (data != bookGroups) {
                bookGroups.clear()
                bookGroups.addAll(data)
                adapter.notifyDataSetChanged()
                adapter = TabFragmentPageAdapter(this)
                binding.viewPagerBookshelf.adapter = adapter

                TabLayoutMediator(binding.tabLayout, binding.viewPagerBookshelf) { tab, position ->
                    tab.text = bookGroups[position].groupName
                }.attach()

                selectLastTab()

                binding.tabLayout.post {
                    for (i in 0 until bookGroups.size) {
                        binding.tabLayout.getTabAt(i)?.view?.setOnLongClickListener {
                            showDialogFragment(GroupEditDialog(bookGroups[i]))
                            true
                        }
                    }
                }
            }
        }
    }

    override fun upSort() {
        childFragmentManager.fragments.forEach {
            if (it is BooksFragment) {
                val position = it.position
                val group = bookGroups.getOrNull(position) ?: return@forEach
                val newSort = group.getRealBookSort()
                it.setEnableRefresh(group.enableRefresh)
                if (it.bookSort != newSort) {
                    it.upBookSort(newSort)
                }
            }
        }
    }

    private fun selectLastTab() {
        binding.tabLayout.post {
            binding.tabLayout.removeOnTabSelectedListener(this)
            binding.tabLayout.getTabAt(AppConfig.saveTabPosition)?.select()
            binding.tabLayout.addOnTabSelectedListener(this)
        }
    }

    override fun onTabReselected(tab: TabLayout.Tab) {
        selectedGroup?.let { group ->
            fragmentMap[group.groupId]?.let {
                toastOnUi("${group.groupName}(${it.getBooksCount()})")
            }
        }
    }

    override fun onTabUnselected(tab: TabLayout.Tab) = Unit

    override fun onTabSelected(tab: TabLayout.Tab) {
        AppConfig.saveTabPosition = tab.position
    }

    override fun gotoTop() {
        fragmentMap[groupId]?.gotoTop()
    }

    private inner class TabFragmentPageAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {
        override fun getItemCount(): Int = bookGroups.size

        override fun createFragment(position: Int): Fragment {
            val group = bookGroups[position]
            val fragment = BooksFragment(position, group)
            fragmentMap[group.groupId] = fragment
            return fragment
        }
    }
}